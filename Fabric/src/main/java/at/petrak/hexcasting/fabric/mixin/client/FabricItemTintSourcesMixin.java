package at.petrak.hexcasting.fabric.mixin.client;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.client.color.IotaStorageTintSource;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric API exposes no hook for the 26.1 data-driven item tint sources, and vanilla only keeps a
 * private id mapper, so append {@code hexcasting:iota_storage_color} after the vanilla entries.
 */
@Mixin(ItemTintSources.class)
public class FabricItemTintSourcesMixin {
    @Shadow
    @Final
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void hexcasting$registerTintSources(CallbackInfo ci) {
        ID_MAPPER.put(HexAPI.modLoc("iota_storage_color"), IotaStorageTintSource.MAP_CODEC);
    }
}
