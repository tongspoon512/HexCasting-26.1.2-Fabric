package at.petrak.hexcasting.client.model;

import net.minecraft.world.entity.player.Player;

/**
 * Duck interface for [net.minecraft.client.renderer.entity.state.AvatarRenderState].
 *
 * 26.1 render states are detached from entities, so the casting-stack overlay needs a way to recover the
 * player it belongs to (and the partial tick it was extracted with) at submit time.
 *
 * <p>This lives outside the mixin package on purpose: mixin packages may not be referenced directly by
 * regular classes (Mixin throws {@code IllegalClassLoadError}).
 */
public interface HexAvatarRenderState {
    Player hex$getPlayer();

    void hex$setPlayer(Player player);

    float hex$getPartialTick();

    void hex$setPartialTick(float partialTick);
}
