package pub.pigeon.yggdyy.hexcreating.blocks;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaNetProcessor;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaPack;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaTerminal;
import pub.pigeon.yggdyy.hexcreating.api.iotawire.IIotaWireConnectorHost;
import pub.pigeon.yggdyy.hexcreating.api.iotawire.IotaWireConnector;
import pub.pigeon.yggdyy.hexcreating.api.iotawire.IotaWireConnectorPropertyHelper;
import pub.pigeon.yggdyy.hexcreating.api.iotawire.IotaWireProcessor;
import pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems;

import java.util.ArrayList;
import java.util.List;

/**
 * A tiny, invisible wire hub. Every face is an independent socket: right-clicking a face with a
 * connector item adds a socket there (up to six per block), and wire reels link two sockets.
 * Packs received by one socket are relayed to every socket it is wired to, plus the block touching
 * the socket's own face.
 */
public class IotaWireConnectorBlock extends Block implements EntityBlock, IWrenchable {
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");

    public static final double H_WIDTH = 0.1;
    public static final double HEIGHT = 0.15;

    public static final AABB UP_BOX = new AABB(0.5 - H_WIDTH, 1.0 - HEIGHT, 0.5 - H_WIDTH,
        0.5 + H_WIDTH, 1.0, 0.5 + H_WIDTH);
    public static final AABB DOWN_BOX = new AABB(0.5 - H_WIDTH, 0.0, 0.5 - H_WIDTH,
        0.5 + H_WIDTH, HEIGHT, 0.5 + H_WIDTH);
    public static final AABB NORTH_BOX = new AABB(0.5 - H_WIDTH, 0.5 - H_WIDTH, 0.0,
        0.5 + H_WIDTH, 0.5 + H_WIDTH, HEIGHT);
    public static final AABB SOUTH_BOX = new AABB(0.5 - H_WIDTH, 0.5 - H_WIDTH, 1.0 - HEIGHT,
        0.5 + H_WIDTH, 0.5 + H_WIDTH, 1.0);
    public static final AABB WEST_BOX = new AABB(0.0, 0.5 - H_WIDTH, 0.5 - H_WIDTH,
        HEIGHT, 0.5 + H_WIDTH, 0.5 + H_WIDTH);
    public static final AABB EAST_BOX = new AABB(1.0 - HEIGHT, 0.5 - H_WIDTH, 0.5 - H_WIDTH,
        1.0, 0.5 + H_WIDTH, 0.5 + H_WIDTH);

    public IotaWireConnectorBlock(Properties properties) {
        super(properties);
        BlockState state = this.stateDefinition.any();
        for (BooleanProperty property : IotaWireConnectorPropertyHelper.getAllProperty()) {
            state = state.setValue(property, false);
        }
        this.registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(UP, DOWN, NORTH, SOUTH, WEST, EAST);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        for (BooleanProperty property : IotaWireConnectorPropertyHelper.getAllProperty()) {
            if (state.getValue(property)) {
                shape = Shapes.or(shape, Shapes.create(IotaWireConnectorPropertyHelper.getBox(property)));
            }
        }
        return shape;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConnectorBlockEntity(pos, state);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state,
        net.minecraft.world.entity.player.Player player) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof ConnectorBlockEntity connector) {
            for (IotaWireConnector socket : connector.getIotaWireConnectors()) {
                socket.disConnectAll();
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void playerDestroy(Level level, net.minecraft.world.entity.player.Player player, BlockPos pos,
        BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide() && (player == null || !player.getAbilities().instabuild)) {
            int count = 0;
            for (BooleanProperty property : IotaWireConnectorPropertyHelper.getAllProperty()) {
                if (state.getValue(property)) {
                    count++;
                }
            }
            if (count > 0) {
                ItemStack stack = new ItemStack(HexCreatingItems.IOTA_WIRE_CONNECTOR);
                stack.setCount(count);
                Vec3 center = pos.getCenter();
                level.addFreshEntity(new ItemEntity(level, center.x, center.y, center.z, stack));
            }
        }
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        Vec3 hit = context.getClickLocation().subtract(pos.getX(), pos.getY(), pos.getZ());
        for (BooleanProperty property : IotaWireConnectorPropertyHelper.getAllProperty()) {
            if (!state.getValue(property)) {
                continue;
            }
            if (!IotaWireConnectorPropertyHelper.getBox(property).inflate(0.005).contains(hit)) {
                continue;
            }
            IotaWireConnector socket = IotaWireProcessor.tryGetConnector(level, pos,
                IotaWireConnectorPropertyHelper.getTerminalIdByProperty(property));
            if (socket != null) {
                socket.disConnectAll();
            }
            BlockState newState = state.setValue(property, false);
            boolean anyLeft = false;
            for (BooleanProperty other : IotaWireConnectorPropertyHelper.getAllProperty()) {
                anyLeft |= newState.getValue(other);
            }
            if (anyLeft) {
                level.setBlock(pos, newState, 3);
            } else {
                level.removeBlock(pos, false);
            }
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                ItemStack stack = new ItemStack(HexCreatingItems.IOTA_WIRE_CONNECTOR);
                if (!context.getPlayer().getInventory().add(stack)) {
                    context.getPlayer().drop(stack, false);
                }
            }
            level.playSound(null, pos, SoundEvents.COPPER_BREAK, SoundSource.BLOCKS, 0.8f, 1.0f);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public static class ConnectorBlockEntity extends HexBlockEntity implements IIotaWireConnectorHost {
        public final IotaTerminal up;
        public final IotaTerminal down;
        public final IotaTerminal north;
        public final IotaTerminal south;
        public final IotaTerminal west;
        public final IotaTerminal east;

        public final IotaWireConnector upC;
        public final IotaWireConnector downC;
        public final IotaWireConnector northC;
        public final IotaWireConnector southC;
        public final IotaWireConnector westC;
        public final IotaWireConnector eastC;

        public ConnectorBlockEntity(BlockPos pos, BlockState state) {
            super(HexCreatingBlockEntities.IOTA_WIRE_CONNECTOR, pos, state);
            this.up = new IotaTerminal(this, 0);
            this.down = new IotaTerminal(this, 1);
            this.north = new IotaTerminal(this, 2);
            this.south = new IotaTerminal(this, 3);
            this.west = new IotaTerminal(this, 4);
            this.east = new IotaTerminal(this, 5);
            this.upC = new IotaWireConnector(this.up, UP_BOX);
            this.downC = new IotaWireConnector(this.down, DOWN_BOX);
            this.northC = new IotaWireConnector(this.north, NORTH_BOX);
            this.southC = new IotaWireConnector(this.south, SOUTH_BOX);
            this.westC = new IotaWireConnector(this.west, WEST_BOX);
            this.eastC = new IotaWireConnector(this.east, EAST_BOX);
        }

        private List<IotaWireConnector> allConnectors() {
            return List.of(this.upC, this.downC, this.northC, this.southC, this.westC, this.eastC);
        }

        private List<BooleanProperty> activeProperties() {
            BlockState state = this.getBlockState();
            List<BooleanProperty> out = new ArrayList<>(6);
            for (BooleanProperty property : IotaWireConnectorPropertyHelper.getAllProperty()) {
                if (state.getValue(property)) {
                    out.add(property);
                }
            }
            return out;
        }

        @Override
        public List<IotaTerminal> getTerminals() {
            List<IotaTerminal> out = new ArrayList<>(6);
            for (BooleanProperty property : activeProperties()) {
                out.add(terminalFor(property));
            }
            return out;
        }

        @Override
        public List<IotaWireConnector> getIotaWireConnectors() {
            List<IotaWireConnector> out = new ArrayList<>(6);
            for (BooleanProperty property : activeProperties()) {
                out.add(connectorFor(property));
            }
            return out;
        }

        private IotaTerminal terminalFor(BooleanProperty property) {
            if (property == UP) {
                return this.up;
            }
            if (property == DOWN) {
                return this.down;
            }
            if (property == NORTH) {
                return this.north;
            }
            if (property == SOUTH) {
                return this.south;
            }
            if (property == WEST) {
                return this.west;
            }
            return this.east;
        }

        private IotaWireConnector connectorFor(BooleanProperty property) {
            if (property == UP) {
                return this.upC;
            }
            if (property == DOWN) {
                return this.downC;
            }
            if (property == NORTH) {
                return this.northC;
            }
            if (property == SOUTH) {
                return this.southC;
            }
            if (property == WEST) {
                return this.westC;
            }
            return this.eastC;
        }

        @Override
        public boolean tryReceiveIotaPack(IotaPack pack, IotaTerminal from, IotaTerminal to) {
            if (!(this.level instanceof ServerLevel serverLevel)) {
                return false;
            }
            for (IotaWireConnector connector : this.getIotaWireConnectors()) {
                if (connector.terminal.terminalId != to.terminalId) {
                    continue;
                }
                // 先确认接收，再转发；否则数据包会沿同一根线立刻回到本端子形成无限递归。
                to.confirmAcceptIotaPack(pack);
                // Wired sockets first, then the block touching this socket's face.
                connector.sendIotaPackToAll(pack);
                Direction face = IotaWireConnectorPropertyHelper.getDirByProperty(
                    propertyFor(connector.terminal.terminalId));
                BlockPos neighbour = this.getBlockPos().relative(face);
                IotaNetProcessor.trySendIotaPack(pack.copyWithSameId(), connector.terminal, neighbour,
                    serverLevel);
                return true;
            }
            return false;
        }

        private static BooleanProperty propertyFor(int terminalId) {
            return switch (terminalId) {
                case 0 -> UP;
                case 1 -> DOWN;
                case 2 -> NORTH;
                case 3 -> SOUTH;
                case 4 -> WEST;
                default -> EAST;
            };
        }

        @Override
        public @Nullable IotaTerminal getDefaultTerminal(IotaTerminal from) {
            BlockPos source = from.blockEntity.getBlockPos();
            BlockPos self = this.getBlockPos();
            BlockState state = this.getBlockState();
            for (BooleanProperty property : IotaWireConnectorPropertyHelper.getAllProperty()) {
                if (!state.getValue(property)) {
                    continue;
                }
                if (self.relative(IotaWireConnectorPropertyHelper.getDirByProperty(property)).equals(source)) {
                    return terminalFor(property);
                }
            }
            return null;
        }

        @Override
        protected void saveModData(CompoundTag tag) {
            for (IotaWireConnector connector : allConnectors()) {
                connector.writeNbt(tag);
            }
        }

        @Override
        protected void loadModData(CompoundTag tag) {
            for (IotaWireConnector connector : allConnectors()) {
                connector.readNbt(tag);
            }
        }
    }
}
