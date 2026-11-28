package pub.pigeon.yggdyy.hexcreating.client;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;
import pub.pigeon.yggdyy.hexcreating.client.render.BoardRenderer;
import pub.pigeon.yggdyy.hexcreating.fluids.HexCreatingFluids;

/**
 * L1 client wiring. Only loaded after the Create presence gate in {@link HexCreatingClientCompat}.
 */
public final class HexCreatingClientRegistration {
    private HexCreatingClientRegistration() {
    }

    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        registerFluid(event, HexCreatingFluids.MEDIA, HexCreatingFluids.FLOWING_MEDIA,
            "block/media_still", "block/media_flowing");
        registerFluid(event, HexCreatingFluids.SOUL, HexCreatingFluids.FLOWING_SOUL,
            "block/soul_still", "block/soul_flowing");
    }

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(HexCreatingBlockEntities.BOARD, BoardRenderer::new);
        event.registerBlockEntityRenderer(HexCreatingBlockEntities.IOTA_WIRE_CONNECTOR,
            pub.pigeon.yggdyy.hexcreating.client.render.IotaWireConnectorRenderer::new);
    }

    /**
     * Scrying lens readouts for the hexcreating machines. Upstream used a Patchouli page for the
     * board; Patchouli is not available here, so the stored iotas are shown directly instead.
     */
    public static void registerScryingLens() {        at.petrak.hexcasting.api.client.ScryingLensOverlayRegistry.addDisplayer(
            pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks.IOTA_READER,
            (lines, state, pos, observer, world, direction) -> {
                if (world.getBlockEntity(pos) instanceof pub.pigeon.yggdyy.hexcreating.blocks.iotareader.IotaReaderBlockEntity be
                    && be.getReadBehaviour() != null && be.getReadBehaviour().getTargetIota() != null) {
                    lines.add(new com.mojang.datafixers.util.Pair<>(
                        new net.minecraft.world.item.ItemStack(
                            pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks.IOTA_READER),
                        at.petrak.hexcasting.api.casting.iota.IotaType.getDisplay(
                            (net.minecraft.nbt.CompoundTag) at.petrak.hexcasting.api.casting.iota.IotaType.serialize(
                                be.getReadBehaviour().getTargetIota()))));
                }
            });
        at.petrak.hexcasting.api.client.ScryingLensOverlayRegistry.addDisplayer(
            pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks.IOTA_WRITER,
            (lines, state, pos, observer, world, direction) -> {
                if (world.getBlockEntity(pos) instanceof pub.pigeon.yggdyy.hexcreating.blocks.iotawriter.IotaWriteBlockEntity be) {
                    lines.add(new com.mojang.datafixers.util.Pair<>(
                        new net.minecraft.world.item.ItemStack(
                            pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks.IOTA_WRITER),
                        at.petrak.hexcasting.api.casting.iota.IotaType.getDisplay(
                            (net.minecraft.nbt.CompoundTag) at.petrak.hexcasting.api.casting.iota.IotaType.serialize(
                                be.getIota()))));
                }
            });
        at.petrak.hexcasting.api.client.ScryingLensOverlayRegistry.addDisplayer(
            pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks.IOTA_WIRE_CONNECTOR,
            (lines, state, pos, observer, world, direction) -> {
                if (world.getBlockEntity(pos) instanceof pub.pigeon.yggdyy.hexcreating.blocks.IotaWireConnectorBlock.ConnectorBlockEntity be) {
                    int sockets = be.getIotaWireConnectors().size();
                    int wires = 0;
                    for (var connector : be.getIotaWireConnectors()) {
                        wires += connector.others.size();
                    }
                    lines.add(new com.mojang.datafixers.util.Pair<>(
                        new net.minecraft.world.item.ItemStack(
                            pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems.IOTA_WIRE),
                        net.minecraft.network.chat.Component.translatable(
                            "hexcreating.scrying.iota_wire_connector", sockets, wires / 2)));
                }
            });
        at.petrak.hexcasting.api.client.ScryingLensOverlayRegistry.addDisplayer(
            pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks.BOARD,
            (lines, state, pos, observer, world, direction) -> {
                if (!(world.getBlockEntity(pos) instanceof pub.pigeon.yggdyy.hexcreating.blocks.board.BoardBlockEntity be)) {
                    return;
                }
                for (int i = 0; i < pub.pigeon.yggdyy.hexcreating.blocks.board.BoardBlockEntity.SLOTS; i++) {
                    var stack = be.getSquare(i);
                    if (stack.isEmpty()
                        || !(stack.getItem() instanceof at.petrak.hexcasting.api.item.IotaHolderItem holder)) {
                        continue;
                    }
                    var tag = holder.readIotaTag(stack);
                    if (tag != null) {
                        lines.add(new com.mojang.datafixers.util.Pair<>(stack.copy(),
                            at.petrak.hexcasting.api.casting.iota.IotaType.getDisplay(tag)));
                    }
                }
            });
    }

    private static void registerFluid(RegisterFluidModelsEvent event, Fluid source, Fluid flowing,
                                      String still, String flowingTexture) {
        event.register(new FluidModel.Unbaked(
            new Material(HexCreatingCompat.id(still), true),
            new Material(HexCreatingCompat.id(flowingTexture), true),
            null, null), source, flowing);
    }
}
