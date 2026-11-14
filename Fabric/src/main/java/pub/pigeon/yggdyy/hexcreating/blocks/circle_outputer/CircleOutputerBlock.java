package pub.pigeon.yggdyy.hexcreating.blocks.circle_outputer;

import at.petrak.hexcasting.api.block.circle.BlockCircleComponent;
import at.petrak.hexcasting.api.casting.circles.ICircleComponent;
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.ListIota;
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
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaNetProcessor;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaPack;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaTerminal;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;

public class CircleOutputerBlock extends BlockCircleComponent implements EntityBlock, IWrenchable {
    public static final EnumProperty<OutputType> OUTPUT_TYPE =
        EnumProperty.create("output_type", OutputType.class);

    public CircleOutputerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(ENERGIZED, false)
            .setValue(OUTPUT_TYPE, OutputType.SILENCE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OUTPUT_TYPE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CircleOutputerBlockEntity(pos, state);
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

        List<Iota> stack = imageIn.getStack();
        if (stack.isEmpty() || !(level.getBlockEntity(pos) instanceof CircleOutputerBlockEntity be)) {
            return new ICircleComponent.ControlFlow.Continue(imageIn, exitPairs);
        }

        OutputType type = state.getValue(OUTPUT_TYPE);
        IotaTerminal terminal = new IotaTerminal(be, 0);
        if (type == OutputType.SINGLE) {
            send(IotaPack.createNew(stack.get(stack.size() - 1)), terminal, level);
        } else if (type == OutputType.MULTIPLE) {
            send(IotaPack.createNew(new ListIota(stack)), terminal, level);
        }
        return new ICircleComponent.ControlFlow.Continue(imageIn, exitPairs);
    }

    private static void send(IotaPack pack, IotaTerminal terminal, ServerLevel level) {
        BlockPos origin = terminal.pos();
        for (Direction direction : Direction.values()) {
            IotaNetProcessor.trySendIotaPack(pack.copyWithSameId(), terminal, origin.relative(direction), level);
        }
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
        level.setBlock(pos, state.cycle(OUTPUT_TYPE), 3);
        IWrenchable.playRotateSound(level, pos);
        return InteractionResult.SUCCESS;
    }

    public enum OutputType implements StringRepresentable {
        SILENCE,
        SINGLE,
        MULTIPLE;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public OutputType next() {
            return switch (this) {
                case SILENCE -> SINGLE;
                case SINGLE -> MULTIPLE;
                case MULTIPLE -> SILENCE;
            };
        }
    }
}
