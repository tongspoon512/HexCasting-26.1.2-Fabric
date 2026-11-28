package pub.pigeon.yggdyy.hexcreating.blocks;

import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine.AnalyticalEngineAluBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine.AnalyticalEngineControllerBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine.AnalyticalEngineKineticInterfaceBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.board.BoardBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.circle_amplifier.CircleAmplifierBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.circle_inputer.CircleInputerBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.circle_outputer.CircleOutputerBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.domain_core.DomainCoreBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.iotareader.IotaReaderBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.iotawriter.IotaWriteBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.train_gate.TrainGateCoreBlockEntity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class HexCreatingBlockEntities {
    private static final Map<Identifier, BlockEntityType<?>> TYPES = new LinkedHashMap<>();

    public static final BlockEntityType<BoardBlockEntity> BOARD = register("board",
        (pos, state) -> new BoardBlockEntity(pos, state), HexCreatingBlocks.BOARD);

    public static final BlockEntityType<CircleInputerBlockEntity> CIRCLE_INPUTER = register("circle_inputer",
        CircleInputerBlockEntity::new, HexCreatingBlocks.CIRCLE_INPUTER);

    public static final BlockEntityType<CircleOutputerBlockEntity> CIRCLE_OUTPUTER = register("circle_outputer",
        CircleOutputerBlockEntity::new, HexCreatingBlocks.CIRCLE_OUTPUTER);

    public static final BlockEntityType<CircleAmplifierBlockEntity> CIRCLE_AMPLIFIER = register("circle_amplifier",
        CircleAmplifierBlockEntity::new, HexCreatingBlocks.CIRCLE_AMPLIFIER);

    public static final BlockEntityType<TrainGateCoreBlockEntity> TRAIN_GATE_CORE = register("train_gate_core",
        TrainGateCoreBlockEntity::new, HexCreatingBlocks.TRAIN_GATE_CORE);

    // 读取器/写入器在旧存档里保存的是 simple_iota_buffer 类型。
    // 这里按方块类型分派到正确的方块实体，旧存档加载后即可直接变成新的动力方块实体。
    public static final BlockEntityType<BlockEntity> SIMPLE_IOTA_BUFFER =
        register("simple_iota_buffer", (pos, state) -> {
            Block block = state.getBlock();
            if (block == HexCreatingBlocks.IOTA_READER) {
                return new IotaReaderBlockEntity(pos, state);
            }
            if (block == HexCreatingBlocks.IOTA_WRITER) {
                return new IotaWriteBlockEntity(pos, state);
            }
            return new SimpleIotaBlock.BufferBlockEntity(pos, state, false);
        }, HexCreatingBlocks.IOTA_READER, HexCreatingBlocks.IOTA_WRITER,
            HexCreatingBlocks.IOTA_PACK_OBSERVER);

    public static final BlockEntityType<IotaReaderBlockEntity> IOTA_READER =
        register("iota_reader", IotaReaderBlockEntity::new, HexCreatingBlocks.IOTA_READER);

    public static final BlockEntityType<IotaWriteBlockEntity> IOTA_WRITER =
        register("iota_writer", IotaWriteBlockEntity::new, HexCreatingBlocks.IOTA_WRITER);

    public static final BlockEntityType<IotaWireConnectorBlock.ConnectorBlockEntity> IOTA_WIRE_CONNECTOR =
        register("iota_wire_connector", IotaWireConnectorBlock.ConnectorBlockEntity::new,
            HexCreatingBlocks.IOTA_WIRE_CONNECTOR);

    public static final BlockEntityType<AnalyticalEngineControllerBlockEntity> ANALYTICAL_ENGINE_CONTROLLER =
        register("analytical_engine_controller",
            (pos, state) -> new AnalyticalEngineControllerBlockEntity(pos, state),
            HexCreatingBlocks.ANALYTICAL_ENGINE_CONTROLLER);

    public static final BlockEntityType<AnalyticalEngineAluBlockEntity> ANALYTICAL_ENGINE_ALU =
        register("analytical_engine_alu", AnalyticalEngineAluBlockEntity::new,
            HexCreatingBlocks.ANALYTICAL_ENGINE_ALU);

    public static final BlockEntityType<AnalyticalEngineKineticInterfaceBlockEntity>
        ANALYTICAL_ENGINE_KINETIC_INTERFACE = register("analytical_engine_kinetic_interface",
        AnalyticalEngineKineticInterfaceBlockEntity::new,
        HexCreatingBlocks.ANALYTICAL_ENGINE_KINETIC_INTERFACE);

    public static final BlockEntityType<DomainCoreBlockEntity> DOMAIN_CORE = register("domain_core",
        DomainCoreBlockEntity::new, HexCreatingBlocks.DOMAIN_CORE);

    private HexCreatingBlockEntities() {
    }

    public static void registerBlockEntities(BiConsumer<BlockEntityType<?>, Identifier> r) {
        TYPES.forEach((id, type) -> r.accept(type, id));
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> register(
        String name, java.util.function.BiFunction<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState, T> supplier,
        Block... blocks) {
        BlockEntityType<T> type = IXplatAbstractions.INSTANCE.createBlockEntityType(supplier, blocks);
        TYPES.put(HexCreatingBlocks.id(name), type);
        return type;
    }
}
