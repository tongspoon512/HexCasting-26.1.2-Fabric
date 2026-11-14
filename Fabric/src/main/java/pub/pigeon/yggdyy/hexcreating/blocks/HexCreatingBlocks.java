package pub.pigeon.yggdyy.hexcreating.blocks;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;
import pub.pigeon.yggdyy.hexcreating.blocks.board.BoardBlock;
import pub.pigeon.yggdyy.hexcreating.blocks.circle_amplifier.CircleAmplifierBlock;
import pub.pigeon.yggdyy.hexcreating.blocks.circle_inputer.CircleInputerBlock;
import pub.pigeon.yggdyy.hexcreating.blocks.circle_outputer.CircleOutputerBlock;
import pub.pigeon.yggdyy.hexcreating.blocks.train_gate.TrainGateBlock;
import pub.pigeon.yggdyy.hexcreating.blocks.train_gate.TrainGateCoreBlock;
import pub.pigeon.yggdyy.hexcreating.blocks.train_gate.TrainGateFrameBlock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class HexCreatingBlocks {
    private static final Map<Identifier, Block> BLOCKS = new LinkedHashMap<>();

    private static BlockBehaviour.Properties metalProps(String name) {
        return BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .sound(SoundType.COPPER)
            .strength(3f, 6f)
            .requiresCorrectToolForDrops()
            .setId(ResourceKey.create(Registries.BLOCK, HexCreatingCompat.id(name)));
    }

    public static final Block AMETHYST_LAMP = block("amethyst_lamp", AmethystLampBlock::new,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_PURPLE)
            .sound(SoundType.AMETHYST)
            .strength(1.5f)
            .lightLevel(state -> state.getValue(AmethystLampBlock.POWER)));

    public static final Block BOARD = block("board", BoardBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).sound(SoundType.WOOD).strength(1.5f));

    public static final Block CIRCLE_INPUTER = block("circle_inputer", CircleInputerBlock::new,
        metalProps("circle_inputer"));
    public static final Block CIRCLE_OUTPUTER = block("circle_outputer", CircleOutputerBlock::new,
        metalProps("circle_outputer"));
    public static final Block CIRCLE_AMPLIFIER = block("circle_amplifier", CircleAmplifierBlock::new,
        metalProps("circle_amplifier"));

    public static final Block TRAIN_GATE = block("train_gate", TrainGateBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).sound(SoundType.AMETHYST)
            .strength(2f).noOcclusion().lightLevel(state -> 12));
    public static final Block TRAIN_GATE_FRAME = block("train_gate_frame", TrainGateFrameBlock::new,
        metalProps("train_gate_frame"));
    public static final Block TRAIN_GATE_CORE = block("train_gate_core", TrainGateCoreBlock::new,
        metalProps("train_gate_core"));

    public static final Block IOTA_READER = block("iota_reader",
        p -> new SimpleIotaBlock(p, true), metalProps("iota_reader"));
    public static final Block IOTA_WRITER = block("iota_writer",
        p -> new SimpleIotaBlock(p, false), metalProps("iota_writer"));
    public static final Block IOTA_WIRE_CONNECTOR = block("iota_wire_connector", IotaWireConnectorBlock::new,
        metalProps("iota_wire_connector"));
    public static final Block IOTA_PACK_OBSERVER = block("iota_pack_observer", IotaPackObserverBlock::new,
        metalProps("iota_pack_observer"));

    public static final Block ANALYTICAL_ENGINE_CONTROLLER = block("analytical_engine_controller",
        AnalyticalEngineBlock::new, metalProps("analytical_engine_controller"));
    public static final Block ANALYTICAL_ENGINE_ALU = block("analytical_engine_alu",
        AnalyticalEngineBlock::new, metalProps("analytical_engine_alu"));
    public static final Block ANALYTICAL_ENGINE_KINETIC_INTERFACE =
        block("analytical_engine_kinetic_interface", AnalyticalEngineBlock::new,
            metalProps("analytical_engine_kinetic_interface"));

    public static final Block MEDIA = block("media",
        p -> new pub.pigeon.yggdyy.hexcreating.fluids.HexCreatingFluids.MediaLiquidBlock(p),
        BlockBehaviour.Properties.ofFullCopy(net.minecraft.world.level.block.Blocks.WATER));

    private HexCreatingBlocks() {
    }

    public static Identifier id(String path) {
        return HexCreatingCompat.id(path);
    }

    public static void registerBlocks(BiConsumer<Block, Identifier> r) {
        BLOCKS.forEach((id, block) -> r.accept(block, id));
    }

    public static Map<Identifier, Block> all() {
        return BLOCKS;
    }

    private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory,
        BlockBehaviour.Properties properties) {
        Block block = factory.apply(properties.setId(
            ResourceKey.create(Registries.BLOCK, HexCreatingCompat.id(name))));
        BLOCKS.put(HexCreatingCompat.id(name), block);
        return block;
    }
}
