package pub.pigeon.yggdyy.hexcreating.blocks.board;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.common.lib.HexSounds;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems;
import pub.pigeon.yggdyy.hexcreating.items.SquareItem;
import at.petrak.hexcasting.api.item.IotaHolderItem;

public class BoardBlock extends Block implements EntityBlock, IWrenchable {
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<Direction> FACING =
        BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape NORTH_SHAPE = Shapes.box(0, 0, 0.75, 1, 1, 1);
    private static final VoxelShape SOUTH_SHAPE = Shapes.box(0, 0, 0, 1, 1, 0.25);
    private static final VoxelShape WEST_SHAPE = Shapes.box(0.75, 0, 0, 1, 1, 1);
    private static final VoxelShape EAST_SHAPE = Shapes.box(0, 0, 0, 0.25, 1, 1);

    public BoardBlock(Properties properties) {
        super(properties);
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

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> Shapes.block();
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BoardBlockEntity(pos, state);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BoardBlockEntity board)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                HexSounds.ADD_TO_PATTERN, SoundSource.BLOCKS, 1f, 1f, true);
            return InteractionResult.SUCCESS;
        }

        int slot = slotFromHit(state, pos, player.getEyePosition(), player.getViewVector(1f));
        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.isEmpty() && held.is(HexCreatingItems.SQUARE)) {
            ItemStack existing = board.getSquare(slot);
            if (!existing.isEmpty()) {
                return InteractionResult.PASS;
            }
            board.setSquare(slot, held.copyWithCount(1));
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            return InteractionResult.CONSUME;
        }

        ItemStack removed = board.removeSquare(slot);
        if (!removed.isEmpty()) {
            player.getInventory().placeItemBackInInventory(removed);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    private static int slotFromHit(BlockState state, BlockPos pos, Vec3 eye, Vec3 look) {
        Vec3 origin;
        Vec3 xAxis;
        Vec3 yAxis;
        Vec3 center = pos.getCenter();
        switch (state.getValue(FACING)) {
            case NORTH -> {
                origin = center.add(0.5, 0.5, 0.25);
                xAxis = new Vec3(-1, 0, 0);
                yAxis = new Vec3(0, -1, 0);
            }
            case SOUTH -> {
                origin = center.add(-0.5, 0.5, -0.25);
                xAxis = new Vec3(1, 0, 0);
                yAxis = new Vec3(0, -1, 0);
            }
            case WEST -> {
                origin = center.add(0.25, 0.5, -0.5);
                xAxis = new Vec3(0, 0, 1);
                yAxis = new Vec3(0, -1, 0);
            }
            case EAST -> {
                origin = center.add(-0.25, 0.5, 0.5);
                xAxis = new Vec3(0, 0, -1);
                yAxis = new Vec3(0, -1, 0);
            }
            default -> {
                return 0;
            }
        }
        Vec3 normal = xAxis.cross(yAxis);
        Vec3 rel = eye.subtract(origin);
        double t = -rel.dot(normal) / look.dot(normal);
        Vec3 hitPoint = eye.add(look.scale(t)).subtract(origin);
        double kx = hitPoint.dot(xAxis) / xAxis.lengthSqr() * 4.0;
        double ky = hitPoint.dot(yAxis) / yAxis.lengthSqr() * 4.0;
        int row = Mth.floor(ky);
        int col = Mth.floor(kx);
        return Mth.clamp(row * 4 + col, 0, 15);
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
        @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide() && blockEntity instanceof BoardBlockEntity board) {
            for (int i = 0; i < BoardBlockEntity.SLOTS; i++) {
                ItemStack stack = board.getSquare(i);
                if (!stack.isEmpty()) {
                    Vec3 center = pos.getCenter();
                    level.addFreshEntity(new ItemEntity(level, center.x, center.y, center.z, stack.copy()));
                }
            }
        }
    }

    public static ItemStack iotaToSquare(Iota iota) {
        ItemStack stack = new ItemStack(HexCreatingItems.SQUARE);
        if (stack.getItem() instanceof SquareItem square) {
            square.writeDatum(stack, iota);
        }
        return stack;
    }

    public static @Nullable Iota squareToIota(ItemStack stack, ServerLevel level) {
        if (stack.getItem() instanceof IotaHolderItem holder) {
            return holder.readIota(stack, level);
        }
        return null;
    }
}
