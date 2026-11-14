package pub.pigeon.yggdyy.hexcreating.blocks;

import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import pub.pigeon.yggdyy.hexcreating.blocks.board.BoardBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.circle_amplifier.CircleAmplifierBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.circle_inputer.CircleInputerBlockEntity;
import pub.pigeon.yggdyy.hexcreating.blocks.circle_outputer.CircleOutputerBlockEntity;
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

    public static final BlockEntityType<SimpleIotaBlock.BufferBlockEntity> SIMPLE_IOTA_BUFFER =
        register("simple_iota_buffer", (pos, state) -> new SimpleIotaBlock.BufferBlockEntity(pos, state, false),
            HexCreatingBlocks.IOTA_READER, HexCreatingBlocks.IOTA_WRITER,
            HexCreatingBlocks.IOTA_WIRE_CONNECTOR, HexCreatingBlocks.IOTA_PACK_OBSERVER);

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
