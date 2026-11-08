package at.petrak.hexcasting.fabric.mixin.client;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.client.item.CustomDataTagModelProperty;
import at.petrak.hexcasting.client.item.VariantModelProperty;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds the item model predicates HexCasting needs but vanilla cannot express:
 * variant matching (the built-in component predicate mangles int tags) and "tag is present"
 * (ancient scrolls store an arbitrary {@code op_id} string).
 */
@Mixin(ConditionalItemModelProperties.class)
public class FabricConditionalItemModelPropertiesMixin {
    @Shadow
    @Final
    private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ConditionalItemModelProperty>> ID_MAPPER;

    @Inject(method = "bootstrap", at = @At("TAIL"))
    private static void hexcasting$registerProperties(CallbackInfo ci) {
        ID_MAPPER.put(HexAPI.modLoc("variant"), VariantModelProperty.MAP_CODEC);
        ID_MAPPER.put(HexAPI.modLoc("custom_data_tag"), CustomDataTagModelProperty.MAP_CODEC);
    }
}
