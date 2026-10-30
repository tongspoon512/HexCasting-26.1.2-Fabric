package dev.emi.trinkets.api.client;

import dev.emi.trinkets.api.SlotReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Stub of trinkets' TrinketRenderer.
 */
public interface TrinketRenderer {
    default void render(ItemStack stack, SlotReference slotReference, LivingEntity entity,
                        com.mojang.blaze3d.vertex.PoseStack matrices,
                        net.minecraft.client.renderer.MultiBufferSource vertexConsumers,
                        int light, float limbAngle, float limbDistance, float tickDelta, float animationProgress,
                        float headYaw, float headPitch) {
    }
}
