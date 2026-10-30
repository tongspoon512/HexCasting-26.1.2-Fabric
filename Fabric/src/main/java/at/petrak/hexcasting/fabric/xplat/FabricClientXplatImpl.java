package at.petrak.hexcasting.fabric.xplat;

import at.petrak.hexcasting.api.client.ClientCastingStack;
import at.petrak.hexcasting.common.msgs.IMessage;
import at.petrak.hexcasting.fabric.cc.HexCardinalComponents;
import at.petrak.hexcasting.fabric.interop.trinkets.TrinketsApiInterop;
import at.petrak.hexcasting.fabric.network.FabricPacketHandler;
import at.petrak.hexcasting.interop.HexInterop;
import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import at.petrak.hexcasting.xplat.ItemPropertyFunction;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public class FabricClientXplatImpl implements IClientXplatAbstractions {
    @Override
    public void sendPacketToServer(IMessage packet) {
        FabricPacketHandler.sendToServer(packet);
    }

    @Override
    public void setRenderLayer(Block block, RenderType type) {
        // 26.1 selects block render layers through ChunkSectionLayer/material flags in the block model.
    }

    @Override
    public void initPlatformSpecific() {
        if (IXplatAbstractions.INSTANCE.isModPresent(HexInterop.Fabric.TRINKETS_API_ID)) {
            TrinketsApiInterop.clientInit();
        }
    }

    @Override
    public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> type,
        EntityRendererProvider<T> renderer) {
        EntityRendererRegistry.register(type, renderer);
    }

    @Override
    public void registerItemProperty(Item item, Identifier id, ItemPropertyFunction func) {
        // 26.1 removed the old ItemProperties predicate system; item model selection is data driven now.
    }

    @Override
    public ClientCastingStack getClientCastingStack(Player player) {
        return HexCardinalComponents.CLIENT_CASTING_STACK.get(player).getClientCastingStack();
    }

    @Override
    public boolean fabricAdditionalQuenchFrustumCheck(AABB aabb) {
        // 26.1 rebuilt the level renderer around CameraRenderState/ChunkSectionsToRender, so the old
        // frustum snag is gone; assume visible and let vanilla frustum culling do the work.
        return true;
    }
}
