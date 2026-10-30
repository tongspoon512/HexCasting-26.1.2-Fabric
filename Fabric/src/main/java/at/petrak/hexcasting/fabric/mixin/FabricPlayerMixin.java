package at.petrak.hexcasting.fabric.mixin;

import at.petrak.hexcasting.common.lib.HexAttributes;
import gay.object.hexdebug.media.HexDebugAttributes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class FabricPlayerMixin extends LivingEntity {
    protected FabricPlayerMixin(EntityType<? extends LivingEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(at = @At("RETURN"), method = "createAttributes")
    private static void hex$addAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
        var out = cir.getReturnValue();
        out.add(HexAttributes.holder(HexAttributes.GRID_ZOOM));
        out.add(HexAttributes.holder(HexAttributes.SCRY_SIGHT));
        out.add(HexAttributes.holder(HexAttributes.FEEBLE_MIND));
        out.add(HexAttributes.holder(HexAttributes.MEDIA_CONSUMPTION_MODIFIER));
        out.add(HexAttributes.holder(HexAttributes.AMBIT_RADIUS));
        out.add(HexAttributes.holder(HexAttributes.SENTINEL_RADIUS));
        out.add(HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA));
        out.add(HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA_MAX));
        out.add(HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA_REGEN));
    }
}
