package at.petrak.hexcasting.mixin.client;

import at.petrak.hexcasting.client.model.HexAvatarRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AvatarRenderState.class)
public abstract class MixinAvatarRenderState implements HexAvatarRenderState {
    @Unique
    private Player hex$player;
    @Unique
    private float hex$partialTick;

    @Override
    public Player hex$getPlayer() {
        return this.hex$player;
    }

    @Override
    public void hex$setPlayer(Player player) {
        this.hex$player = player;
    }

    @Override
    public float hex$getPartialTick() {
        return this.hex$partialTick;
    }

    @Override
    public void hex$setPartialTick(float partialTick) {
        this.hex$partialTick = partialTick;
    }
}
