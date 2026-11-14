package pub.pigeon.yggdyy.hexcreating.blocks;

import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IIotaTerminalHost;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaNetProcessor;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaPack;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaTerminal;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IotaWireConnectorBlock extends Block implements EntityBlock, IWrenchable {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING =
        BlockStateProperties.FACING;

    private static final VoxelShape SHAPE = Shapes.box(0.375, 0.375, 0.375, 0.625, 0.625, 0.625);

    public IotaWireConnectorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConnectorBlockEntity(pos, state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        level.setBlock(pos, state.setValue(FACING, context.getClickedFace()), 3);
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    /**
     * Relays iota packs across a connected subnet of connectors. Every connector in the subnet
     * broadcasts to the hosts touching it; the subnet's positions are stamped into
     * {@code headInfo} so a pack can never traverse the same subnet twice (loop protection).
     */
    public static class ConnectorBlockEntity extends SimpleIotaBlock.BufferBlockEntity
        implements IIotaTerminalHost {
        public static final String PATH_PREFIX = "path:";
        private static final int MAX_SUBNET = 256;

        private final IotaTerminal terminal = new IotaTerminal(this, 0);
        private long cachedTick = -1L;
        private Set<BlockPos> cachedSubnet;

        public ConnectorBlockEntity(BlockPos pos, BlockState state) {
            super(pos, state, false);
        }

        @Override
        public List<IotaTerminal> getTerminals() {
            return List.of(this.terminal);
        }

        @Override
        public @Nullable IotaTerminal getDefaultTerminal(IotaTerminal from) {
            return this.terminal;
        }

        @Override
        public boolean tryReceiveIotaPack(IotaPack pack, IotaTerminal from, IotaTerminal to) {
            if (!(this.level instanceof ServerLevel level)) {
                return false;
            }
            Set<BlockPos> subnet = subnet(level);
            for (BlockPos pos : subnet) {
                if (pack.hasHeadInfo(PATH_PREFIX + pos.toShortString())) {
                    // This subnet already forwarded the pack.
                    return false;
                }
            }

            boolean delivered = false;
            for (BlockPos pos : subnet) {
                if (!(level.getBlockEntity(pos) instanceof ConnectorBlockEntity connector)) {
                    continue;
                }
                for (Direction direction : Direction.values()) {
                    BlockPos target = pos.relative(direction);
                    if (subnet.contains(target)) {
                        continue;
                    }
                    if (!(level.getBlockEntity(target) instanceof IIotaTerminalHost)) {
                        continue;
                    }
                    IotaPack forwarded = pack.copyWithSameId();
                    for (BlockPos member : subnet) {
                        forwarded.addHeadInfo(PATH_PREFIX + member.toShortString());
                    }
                    delivered |= IotaNetProcessor.trySendIotaPack(forwarded, connector.terminal,
                        target, level);
                }
            }
            return delivered;
        }

        /**
         * Breadth-first scan of the connector subnet, cached once per game tick.
         */
        private Set<BlockPos> subnet(ServerLevel level) {
            long now = level.getGameTime();
            if (this.cachedSubnet != null && this.cachedTick == now) {
                return this.cachedSubnet;
            }
            Set<BlockPos> seen = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            BlockPos origin = getBlockPos();
            seen.add(origin);
            queue.add(origin);
            while (!queue.isEmpty() && seen.size() <= MAX_SUBNET) {
                BlockPos pos = queue.removeFirst();
                for (Direction direction : Direction.values()) {
                    BlockPos next = pos.relative(direction);
                    if (seen.contains(next)) {
                        continue;
                    }
                    if (level.getBlockEntity(next) instanceof ConnectorBlockEntity) {
                        seen.add(next);
                        queue.addLast(next);
                    }
                }
            }
            this.cachedSubnet = seen;
            this.cachedTick = now;
            return seen;
        }

        @Override
        protected void saveModData(CompoundTag tag) {
            super.saveModData(tag);
            this.terminal.writeNbt(tag);
        }

        @Override
        protected void loadModData(CompoundTag tag) {
            super.loadModData(tag);
            this.terminal.readNbt(tag);
        }
    }
}
