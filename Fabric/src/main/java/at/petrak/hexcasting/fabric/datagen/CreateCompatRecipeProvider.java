package at.petrak.hexcasting.fabric.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.fabricmc.fabric.impl.datagen.FabricDataGenHelper;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Create's processing recipes are not vanilla {@code Recipe} implementations, so they cannot be
 * written through {@code RecipeOutput}. On 26.1 they use the codec-based recipe format
 * ({@code ingredient}/{@code id}/{@code processing_time}) that Create-Fly shares with Create,
 * so the JSON is emitted directly instead.
 */
public class CreateCompatRecipeProvider implements DataProvider {
    private final PackOutput output;

    public CreateCompatRecipeProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        // PathProvider 会写成 data/<id 的 namespace>/<directory>/<id 的 path>.json
        var createCrushing = this.output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe/crushing");
        var hexCrushing = this.output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe/compat/create/crushing");

        List<CompletableFuture<?>> futures = new ArrayList<>();
        futures.add(DataProvider.saveStable(cache, withCreateCondition(crushing(
                    "minecraft:amethyst_cluster",
                    result("minecraft:amethyst_shard", 7),
                    result("hexcasting:amethyst_dust", 5),
                    result("hexcasting:charged_amethyst", 1, 0.25f))),
                createCrushing.json(Identifier.fromNamespaceAndPath("create", "amethyst_cluster"))));
        futures.add(DataProvider.saveStable(cache, withCreateCondition(crushing(
                    "minecraft:amethyst_block",
                    result("minecraft:amethyst_shard", 3),
                    result("hexcasting:amethyst_dust", 4, 0.5f))),
                createCrushing.json(Identifier.fromNamespaceAndPath("create", "amethyst_block"))));
        futures.add(DataProvider.saveStable(cache, withCreateCondition(crushing(
                    "minecraft:amethyst_shard",
                    result("hexcasting:amethyst_dust", 4),
                    result("hexcasting:amethyst_dust", 1, 0.5f))),
                hexCrushing.json(Identifier.fromNamespaceAndPath("hexcasting", "amethyst_shard"))));

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject crushing(String ingredient, JsonObject... results) {
        var json = new JsonObject();
        json.addProperty("type", "create:crushing");
        json.addProperty("ingredient", ingredient);
        json.addProperty("processing_time", 150);
        var resultArray = new JsonArray();
        for (var result : results) {
            resultArray.add(result);
        }
        json.add("results", resultArray);
        return json;
    }

    private static JsonObject result(String id, int count) {
        var json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("count", count);
        return json;
    }

    private static JsonObject result(String id, int count, float chance) {
        var json = result(id, count);
        json.addProperty("chance", chance);
        return json;
    }

    private static JsonObject withCreateCondition(JsonObject json) {
        FabricDataGenHelper.addConditions(json, ResourceConditions.allModsLoaded("create"));
        return json;
    }

    @Override
    public String getName() {
        return "HexCasting Create compat recipes";
    }
}
