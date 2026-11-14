package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.common.lib.HexItems;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.blocks.AnalyticalEngineBlock;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

public class AnalyticalEngineControllerBlock extends AnalyticalEngineBlock
    implements EntityBlock, IWrenchable {
    public static final EnumProperty<EngineState.Status> STATUS =
        EnumProperty.create("status", EngineState.Status.class);

    public AnalyticalEngineControllerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(STATUS, EngineState.Status.IDLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(STATUS);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AnalyticalEngineControllerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != HexCreatingBlockEntities.ANALYTICAL_ENGINE_CONTROLLER) {
            return null;
        }
        return (BlockEntityTicker<T>) (BlockEntityTicker<AnalyticalEngineControllerBlockEntity>)
            (lvl, pos, blockState, be) -> be.serverTick();
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                            BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof AnalyticalEngineControllerBlockEntity controller)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            controller.toggleRun();
        }
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(
                Component.translatable("hexcreating.analytical_engine.status",
                    Component.translatable("hexcreating.analytical_engine.state."
                        + controller.getState().getSerializedName())));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide()) {
            return mediaValue(stack) > 0 || stack.getItem() instanceof IotaHolderItem
                ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof AnalyticalEngineControllerBlockEntity controller)) {
            return InteractionResult.PASS;
        }

        long media = mediaValue(stack);
        if (media > 0) {
            controller.setMediaBuffer(controller.getMediaBuffer() + media);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }

        if (stack.getItem() instanceof IotaHolderItem && level instanceof ServerLevel serverLevel) {
            java.util.List<Iota> program = AnalyticalEngineRuntime.readProgram(stack, serverLevel);
            if (!program.isEmpty()) {
                AnalyticalEngineRuntime.installProgram(controller, program);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private static long mediaValue(ItemStack stack) {
        if (stack.is(HexItems.AMETHYST_DUST)) {
            return HexConfig.common().dustMediaAmount();
        }
        if (stack.is(HexItems.CHARGED_AMETHYST)) {
            return HexConfig.common().chargedCrystalMediaAmount();
        }
        return 0L;
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof AnalyticalEngineControllerBlockEntity controller)) {
            return InteractionResult.PASS;
        }
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            controller.resetError();
        } else {
            controller.cyclePort(context.getClickedFace());
        }
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!level.isClientSide()
            && level.getBlockEntity(pos) instanceof AnalyticalEngineControllerBlockEntity controller) {
            controller.requestClusterRefresh();
        }
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof AnalyticalEngineControllerBlockEntity controller)) {
            return 0;
        }
        if (controller.getError().isError()) {
            return 15;
        }
        return Math.min(15, controller.getStack().size());
    }
}
