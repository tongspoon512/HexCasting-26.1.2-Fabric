package at.petrak.hexcasting.fabric.mixin.client;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.client.item.MediaModelProperty;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Registers the media battery's numeric properties into the data driven {@code range_dispatch}
 * mapper, alongside the vanilla damage/count/time properties.
 */
@Mixin(RangeSelectItemModelProperties.class)
public class FabricRangeSelectItemModelPropertiesMixin {
    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void hexcasting$registerProperties(CallbackInfo ci) {
        RangeSelectItemModelProperties.ID_MAPPER.put(HexAPI.modLoc("media"), MediaModelProperty.MEDIA_CODEC);
        RangeSelectItemModelProperties.ID_MAPPER.put(HexAPI.modLoc("max_media"), MediaModelProperty.MAX_CODEC);
    }
}
