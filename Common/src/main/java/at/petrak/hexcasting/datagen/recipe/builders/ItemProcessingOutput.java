package at.petrak.hexcasting.datagen.recipe.builders;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record ItemProcessingOutput(ItemStack stack, float chance) implements ProcessingOutput {
    @Override
    public JsonObject serialize() {
        JsonObject json = new JsonObject();
        Identifier resourceLocation = BuiltInRegistries.ITEM.getKey(stack.getItem());
        json.addProperty("item", resourceLocation.toString());
        int count = stack.getCount();
        if (count != 1) {
            json.addProperty("count", count);
        }
        if (chance != 1) {
            json.addProperty("chance", chance);
        }
        return json;
    }
}
