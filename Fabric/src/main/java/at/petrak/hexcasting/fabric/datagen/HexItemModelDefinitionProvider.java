package at.petrak.hexcasting.fabric.datagen;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 26.1 resolves item visuals through {@code assets/<namespace>/items/<id>.json} item definitions;
 * the legacy {@code models/item} files alone are no longer enough. Generate one definition per
 * HexCasting item that points at the existing hand-written model.
 */
public class HexItemModelDefinitionProvider implements DataProvider {
    private static final String NAMESPACE = "hexcasting";

    /**
     * These item models only contain the 1.20-style {@code overrides} block and no textures of
     * their own, which 26.1 ignores. Point them at their default (first) variant so they still
     * render a texture instead of the missing model.
     */
    private static final Map<String, String> STATIC_MODELS = new HashMap<>();

    static {
        STATIC_MODELS.put("slate", "item/slate_blank");
        STATIC_MODELS.put("scroll_small", "item/scroll_pristine_small");
        STATIC_MODELS.put("scroll_medium", "item/scroll_pristine_medium");
        STATIC_MODELS.put("scroll", "item/scroll_pristine_large");
        STATIC_MODELS.put("quenched_allay", "block/quenched_allay_0");
        STATIC_MODELS.put("quenched_allay_tiles", "block/deco/quenched_allay_tiles_0");
        STATIC_MODELS.put("quenched_allay_bricks", "block/deco/quenched_allay_bricks_0");
        STATIC_MODELS.put("quenched_allay_bricks_small", "block/deco/quenched_allay_bricks_small_0");
        STATIC_MODELS.put("quenched_allay_shard", "item/quenched_shard_0");
        STATIC_MODELS.put("battery", "item/phial_small_0");
        STATIC_MODELS.put("staff/quenched", "item/staff/quenched_0");
    }

    private final PackOutput output;

    public HexItemModelDefinitionProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        var pathProvider = this.output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (var entry : BuiltInRegistries.ITEM.entrySet()) {
            var id = entry.getKey().identifier();
            if (!NAMESPACE.equals(id.getNamespace())) {
                continue;
            }

            var model = new JsonObject();
            model.addProperty("type", "minecraft:model");
            model.addProperty("model", NAMESPACE + ":"
                + STATIC_MODELS.getOrDefault(id.getPath(), "item/" + id.getPath()));

            var root = new JsonObject();
            root.add("model", model);

            futures.add(DataProvider.saveStable(cache, root, pathProvider.json(id)));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "HexCasting item model definitions";
    }
}
