package at.petrak.hexcasting.common.recipe;

import at.petrak.hexcasting.api.HexAPI;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class HexRecipeStuffRegistry {
    public static void registerSerializers(BiConsumer<RecipeSerializer<?>, Identifier> r) {
        for (var e : SERIALIZERS.entrySet()) {
            r.accept(e.getValue(), e.getKey());
        }
    }

    public static void registerTypes(BiConsumer<RecipeType<?>, Identifier> r) {
        for (var e : TYPES.entrySet()) {
            r.accept(e.getValue(), e.getKey());
        }
    }

    private static final Map<Identifier, RecipeSerializer<?>> SERIALIZERS = new LinkedHashMap<>();
    private static final Map<Identifier, RecipeType<?>> TYPES = new LinkedHashMap<>();

    public static final RecipeSerializer<BrainsweepRecipe> BRAINSWEEP = registerSerializer("brainsweep",
        BrainsweepRecipe.SERIALIZER);
    public static final RecipeSerializer<SealThingsRecipe> SEAL_FOCUS = registerSerializer(
        "seal_focus", SealThingsRecipe.FOCUS_SERIALIZER);
    public static final RecipeSerializer<SealThingsRecipe> SEAL_SPELLBOOK = registerSerializer(
        "seal_spellbook", SealThingsRecipe.SPELLBOOK_SERIALIZER);

    public static RecipeType<BrainsweepRecipe> BRAINSWEEP_TYPE = registerType("brainsweep");

    private static <T extends Recipe<?>> RecipeSerializer<T> registerSerializer(String name, RecipeSerializer<T> rs) {
        var old = SERIALIZERS.put(modLoc(name), rs);
        if (old != null) {
            throw new IllegalArgumentException("Typo? Duplicate id " + name);
        }
        return rs;
    }

    private static <T extends Recipe<?>> RecipeType<T> registerType(String name) {
        var type = new RecipeType<T>() {
            @Override
            public String toString() {
                return HexAPI.MOD_ID + ":" + name;
            }
        };
        // never will be a collision because it's a new object
        TYPES.put(modLoc(name), type);
        return type;
    }
}
