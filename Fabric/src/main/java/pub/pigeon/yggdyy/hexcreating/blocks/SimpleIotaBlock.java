package pub.pigeon.yggdyy.hexcreating.blocks;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

/**
 * A compact iota buffer used by the reader/writer pair. The Create belt/arm integration is
 * intentionally left for a later milestone; the block itself already stores and exposes an iota.
 */
public class SimpleIotaBlock extends Block implements EntityBlock {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING =
        BlockStateProperties.HORIZONTAL_FACING;
    public final boolean readOnly;

    public SimpleIotaBlock(Properties properties, boolean readOnly) {
        super(properties);
        this.readOnly = readOnly;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BufferBlockEntity(pos, state, this.readOnly);
    }

    public static class BufferBlockEntity extends HexBlockEntity {
        private static final String TAG_IOTA = "iota";
        private final boolean readOnly;
        @Nullable
        private Iota iota;

        public BufferBlockEntity(BlockPos pos, BlockState state, boolean readOnly) {
            super(HexCreatingBlockEntities.SIMPLE_IOTA_BUFFER, pos, state);
            this.readOnly = readOnly;
        }

        public @Nullable Iota getIota() {
            return this.iota;
        }

        public void setIota(@Nullable Iota iota) {
            this.iota = iota;
            this.sync();
        }

        public boolean isReadOnly() {
            return this.readOnly;
        }

        @Override
        protected void saveModData(CompoundTag tag) {
            if (this.iota != null) {
                tag.put(TAG_IOTA, IotaType.serialize(this.iota));
            }
        }

        @Override
        protected void loadModData(CompoundTag tag) {
            if (this.level instanceof ServerLevel serverLevel) {
                tag.getCompound(TAG_IOTA).ifPresent(saved -> this.iota = IotaType.deserialize(saved, serverLevel));
            }
        }
    }
}
