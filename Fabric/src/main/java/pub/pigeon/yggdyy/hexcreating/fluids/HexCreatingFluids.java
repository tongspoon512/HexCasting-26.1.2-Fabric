package pub.pigeon.yggdyy.hexcreating.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks;
import pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class HexCreatingFluids {
    private static final Map<Identifier, Fluid> FLUIDS = new LinkedHashMap<>();

    public static final MediaFluid.Source MEDIA = new MediaFluid.Source();
    public static final MediaFluid.Flowing FLOWING_MEDIA = new MediaFluid.Flowing();
    public static final SoulFluid.Source SOUL = new SoulFluid.Source();
    public static final SoulFluid.Flowing FLOWING_SOUL = new SoulFluid.Flowing();

    private HexCreatingFluids() {
    }

    public static void registerFluids(BiConsumer<Fluid, Identifier> r) {
        FLUIDS.forEach((id, fluid) -> r.accept(fluid, id));
    }

    public static final class MediaFluid {
        public static class Flowing extends FlowingFluid {
            {
                FLUIDS.put(HexCreatingCompat.id("flowing_media"), this);
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }

            @Override
            public boolean isSource(FluidState state) {
                return false;
            }

            @Override
            public int getAmount(FluidState state) {
                return state.getValue(LEVEL);
            }

            @Override
            public Fluid getFlowing() {
                return this;
            }

            @Override
            public Fluid getSource() {
                return MEDIA;
            }

            @Override
            public Item getBucket() {
                return HexCreatingItems.MEDIA_BUCKET;
            }

            @Override
            protected boolean canConvertToSource(ServerLevel level) {
                return false;
            }

            @Override
            protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            }

            @Override
            protected int getSlopeFindDistance(LevelReader level) {
                return 3;
            }

            @Override
            protected int getDropOff(LevelReader level) {
                return 1;
            }

            @Override
            protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                Fluid fluid, Direction direction) {
                return direction == Direction.DOWN && !fluid.isSame(HexCreatingFluids.MEDIA);
            }

            @Override
            protected float getExplosionResistance() {
                return 100f;
            }

            @Override
            public int getTickDelay(LevelReader level) {
                return 5;
            }

            @Override
            protected BlockState createLegacyBlock(FluidState state) {
                return HexCreatingBlocks.MEDIA.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
            }
        }

        public static class Source extends FlowingFluid {
            {
                FLUIDS.put(HexCreatingCompat.id("media"), this);
            }

            @Override
            public boolean isSource(FluidState state) {
                return true;
            }

            @Override
            public int getAmount(FluidState state) {
                return 8;
            }

            @Override
            public Fluid getFlowing() {
                return FLOWING_MEDIA;
            }

            @Override
            public Fluid getSource() {
                return this;
            }

            @Override
            public Item getBucket() {
                return HexCreatingItems.MEDIA_BUCKET;
            }

            @Override
            protected boolean canConvertToSource(ServerLevel level) {
                return false;
            }

            @Override
            protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            }

            @Override
            protected int getSlopeFindDistance(LevelReader level) {
                return 4;
            }

            @Override
            protected int getDropOff(LevelReader level) {
                return 1;
            }

            @Override
            protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                Fluid fluid, Direction direction) {
                return direction == Direction.DOWN && !fluid.isSame(HexCreatingFluids.MEDIA);
            }

            @Override
            protected float getExplosionResistance() {
                return 100f;
            }

            @Override
            public int getTickDelay(LevelReader level) {
                return 5;
            }

            @Override
            protected BlockState createLegacyBlock(FluidState state) {
                return HexCreatingBlocks.MEDIA.defaultBlockState().setValue(LiquidBlock.LEVEL, 8);
            }
        }
    }

    public static final class SoulFluid {
        public static class Flowing extends FlowingFluid {
            {
                FLUIDS.put(HexCreatingCompat.id("flowing_soul"), this);
            }

            @Override
            protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
                super.createFluidStateDefinition(builder);
                builder.add(LEVEL);
            }

            @Override
            public boolean isSource(FluidState state) {
                return false;
            }

            @Override
            public int getAmount(FluidState state) {
                return state.getValue(LEVEL);
            }

            @Override
            public Fluid getFlowing() {
                return this;
            }

            @Override
            public Fluid getSource() {
                return SOUL;
            }

            @Override
            public Item getBucket() {
                return null;
            }

            @Override
            protected boolean canConvertToSource(ServerLevel level) {
                return false;
            }

            @Override
            protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            }

            @Override
            protected int getSlopeFindDistance(LevelReader level) {
                return 2;
            }

            @Override
            protected int getDropOff(LevelReader level) {
                return 2;
            }

            @Override
            protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                Fluid fluid, Direction direction) {
                return false;
            }

            @Override
            protected float getExplosionResistance() {
                return 100f;
            }

            @Override
            public int getTickDelay(LevelReader level) {
                return 10;
            }

            @Override
            protected BlockState createLegacyBlock(FluidState state) {
                return Blocks.AIR.defaultBlockState();
            }
        }

        public static class Source extends FlowingFluid {
            {
                FLUIDS.put(HexCreatingCompat.id("soul"), this);
            }

            @Override
            public boolean isSource(FluidState state) {
                return true;
            }

            @Override
            public int getAmount(FluidState state) {
                return 8;
            }

            @Override
            public Fluid getFlowing() {
                return FLOWING_SOUL;
            }

            @Override
            public Fluid getSource() {
                return this;
            }

            @Override
            public Item getBucket() {
                return null;
            }

            @Override
            protected boolean canConvertToSource(ServerLevel level) {
                return false;
            }

            @Override
            protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            }

            @Override
            protected int getSlopeFindDistance(LevelReader level) {
                return 2;
            }

            @Override
            protected int getDropOff(LevelReader level) {
                return 2;
            }

            @Override
            protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                Fluid fluid, Direction direction) {
                return false;
            }

            @Override
            protected float getExplosionResistance() {
                return 100f;
            }

            @Override
            public int getTickDelay(LevelReader level) {
                return 10;
            }

            @Override
            protected BlockState createLegacyBlock(FluidState state) {
                return Blocks.AIR.defaultBlockState();
            }
        }
    }

    public static class MediaLiquidBlock extends LiquidBlock {
        public MediaLiquidBlock(BlockBehaviour.Properties properties) {
            super(MEDIA, properties);
        }
    }
}
