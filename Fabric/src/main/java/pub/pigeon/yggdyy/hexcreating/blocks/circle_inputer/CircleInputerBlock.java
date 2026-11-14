package pub.pigeon.yggdyy.hexcreating.blocks.circle_inputer;

import at.petrak.hexcasting.api.block.circle.BlockCircleComponent;
import at.petrak.hexcasting.api.casting.circles.ICircleComponent;
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.iota.Iota;
import com.zurrtum.create.content.equipment.wrench.IWrenchable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public class CircleInputerBlock extends BlockCircleComponent implements EntityBlock, IWrenchable {
    public static final EnumProperty<InputType> INPUT_TYPE =
        EnumProperty.create("input_type", InputType.class);

    public CircleInputerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(ENERGIZED, false)
            .setValue(INPUT_TYPE, InputType.ALWAYS_SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(INPUT_TYPE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CircleInputerBlockEntity(pos, state);
    }

    @Override
    public Direction normalDir(BlockPos pos, BlockState state, Level level, int recursionLeft) {
        return Direction.UP;
    }

    @Override
    public float particleHeight(BlockPos pos, BlockState state, Level level) {
        return 0.6f;
    }

    @Override
    public ICircleComponent.ControlFlow acceptControlFlow(CastingImage imageIn, CircleCastEnv env,
        Direction enterDir, BlockPos pos, BlockState state, ServerLevel level) {
        EnumSet<Direction> exits = this.possibleExitDirections(pos, state, level);
        exits.remove(enterDir.getOpposite());
        List<com.mojang.datafixers.util.Pair<BlockPos, Direction>> exitPairs =
            exits.stream().map(dir -> this.exitPositionFromDirection(pos, dir)).toList();

        if (!(level.getBlockEntity(pos) instanceof CircleInputerBlockEntity be) || be.iotas.isEmpty()) {
            return new ICircleComponent.ControlFlow.Continue(imageIn, exitPairs);
        }

        List<Iota> stack = new ArrayList<>(imageIn.getStack());
        stack.addAll(be.iotas);
        CastingImage result = imageIn.copy(stack, imageIn.getParenCount(), imageIn.getParenthesized(),
            imageIn.getEscapeNext(), imageIn.getOpsConsumed(), imageIn.getUserData());
        return new ICircleComponent.ControlFlow.Continue(result, exitPairs);
    }

    @Override
    public boolean canEnterFromDirection(Direction enterDir, BlockPos pos, BlockState state, ServerLevel level) {
        return true;
    }

    @Override
    public EnumSet<Direction> possibleExitDirections(BlockPos pos, BlockState state, Level level) {
        return EnumSet.allOf(Direction.class);
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        level.setBlock(pos, state.cycle(INPUT_TYPE), 3);
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    public enum InputType implements StringRepresentable {
        REJECT,
        ALWAYS_SINGLE,
        TRY_MULTIPLE;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public InputType next() {
            return switch (this) {
                case REJECT -> ALWAYS_SINGLE;
                case ALWAYS_SINGLE -> TRY_MULTIPLE;
                case TRY_MULTIPLE -> REJECT;
            };
        }
    }
}
