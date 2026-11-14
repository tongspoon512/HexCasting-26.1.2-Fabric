package pub.pigeon.yggdyy.hexcreating.blocks.circle_amplifier;

import at.petrak.hexcasting.api.casting.circles.ICircleComponent;
import at.petrak.hexcasting.api.casting.eval.env.CircleCastEnv;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import com.zurrtum.create.content.kinetics.base.DirectionalKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class CircleAmplifierBlock extends DirectionalKineticBlock implements ICircleComponent, net.minecraft.world.level.block.EntityBlock {
    public static final BooleanProperty ENERGIZED = BooleanProperty.create("energized");

    public CircleAmplifierBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(ENERGIZED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ENERGIZED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CircleAmplifierBlockEntity(pos, state);
    }

    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public ControlFlow acceptControlFlow(CastingImage imageIn, CircleCastEnv env, Direction enterDir,
        BlockPos pos, BlockState state, ServerLevel level) {
        EnumSet<Direction> exits = this.possibleExitDirections(pos, state, level);
        exits.remove(enterDir.getOpposite());
        List<com.mojang.datafixers.util.Pair<BlockPos, Direction>> exitPairs =
            exits.stream().map(dir -> this.exitPositionFromDirection(pos, dir)).toList();

        var execState = env.circleState();
        int extend = level.getBlockEntity(pos) instanceof CircleAmplifierBlockEntity be
            ? (int) (Math.abs(be.getSpeed()) / 4f) : 0;

        if (extend > 0) {
            AABB old = execState.bounds;
            execState.expandBounds(new AABB(
                Math.min(pos.getX() - extend, old.minX),
                Math.min(pos.getY() - extend, old.minY),
                Math.min(pos.getZ() - extend, old.minZ),
                Math.max(pos.getX() + extend, old.maxX),
                Math.max(pos.getY() + extend, old.maxY),
                Math.max(pos.getZ() + extend, old.maxZ)));
        }
        return new ControlFlow.Continue(imageIn, exitPairs);
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
    public BlockState startEnergized(BlockPos pos, BlockState state, Level level) {
        BlockState energized = state.setValue(ENERGIZED, true);
        level.setBlockAndUpdate(pos, energized);
        return energized;
    }

    @Override
    public boolean isEnergized(BlockPos pos, BlockState state, Level level) {
        return state.getValue(ENERGIZED);
    }

    @Override
    public BlockState endEnergized(BlockPos pos, BlockState state, Level level) {
        BlockState off = state.setValue(ENERGIZED, false);
        level.setBlockAndUpdate(pos, off);
        return off;
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(ENERGIZED) ? 15 : 0;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case DOWN -> Shapes.box(0, 0.75, 0, 1, 1, 1);
            case UP -> Shapes.box(0, 0, 0, 1, 0.25, 1);
            case WEST -> Shapes.box(0.75, 0, 0, 1, 1, 1);
            case EAST -> Shapes.box(0, 0, 0, 0.25, 1, 1);
            case SOUTH -> Shapes.box(0, 0, 0, 1, 1, 0.25);
            case NORTH -> Shapes.box(0, 0, 0.75, 1, 1, 1);
        };
    }
}
