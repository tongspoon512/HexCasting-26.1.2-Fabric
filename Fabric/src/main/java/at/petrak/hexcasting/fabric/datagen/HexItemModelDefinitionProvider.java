package at.petrak.hexcasting.fabric.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntFunction;

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
    private static final Set<String> PACKAGED_ITEMS = Set.of("artifact", "trinket", "cypher", "ancient_cypher");
    private static final int VARIANTS = 8;

    static {
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

            JsonObject model;
            var path = id.getPath();
            if ("focus".equals(path)) {
                model = sealableVariantModel(path);
            } else if ("thought_knot".equals(path)) {
                model = thoughtKnotModel();
            } else if ("slate".equals(path)) {
                model = slateModel();
            } else if (PACKAGED_ITEMS.contains(path)) {
                model = packagedModel(path);
            } else {
                model = simpleModel(STATIC_MODELS.getOrDefault(path, "item/" + path));
            }

            var root = new JsonObject();
            root.add("model", model);

            futures.add(DataProvider.saveStable(cache, root, pathProvider.json(id)));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private static JsonObject simpleModel(String path) {
        var model = new JsonObject();
        model.addProperty("type", "minecraft:model");
        model.addProperty("model", NAMESPACE + ":" + path);
        return model;
    }

    private static JsonObject componentCondition(JsonObject nbt, JsonObject onTrue, JsonObject onFalse) {
        // minecraft:component 的 Codec 是 dispatchMap("predicate", ...)，字段在条件对象上平铺：
        // predicate 是谓词类型 id（字符串），value 是该谓词的值（这里是部分匹配的 NBT）。
        var condition = new JsonObject();
        condition.addProperty("type", "minecraft:condition");
        condition.addProperty("property", "minecraft:component");
        condition.addProperty("predicate", "minecraft:custom_data");
        condition.add("value", nbt);
        condition.add("on_true", onTrue);
        condition.add("on_false", onFalse);
        return condition;
    }

    private static JsonObject hasComponent(String componentId, JsonObject onTrue, JsonObject onFalse) {
        var condition = new JsonObject();
        condition.addProperty("type", "minecraft:condition");
        condition.addProperty("property", "minecraft:has_component");
        condition.addProperty("component", componentId);
        condition.add("on_true", onTrue);
        condition.add("on_false", onFalse);
        return condition;
    }

    private static JsonObject variantChain(IntFunction<JsonObject> statesForVariant) {
        var root = statesForVariant.apply(0);
        for (var variant = 1; variant < VARIANTS; variant++) {
            var nbt = new JsonObject();
            nbt.addProperty("variant", variant);
            root = componentCondition(nbt, statesForVariant.apply(variant), root);
        }
        return root;
    }

    private static JsonObject sealableVariantModel(String path) {
        return variantChain(variant -> {
            var sealed = simpleModel("item/" + path + "_" + variant + "_sealed");
            var filled = simpleModel("item/" + path + "_" + variant + "_filled");
            var plain = simpleModel(variant == 0 ? "item/" + path : "item/" + path + "_" + variant);

            var sealedTag = new JsonObject();
            sealedTag.addProperty("sealed", true);
            var dataTag = new JsonObject();
            dataTag.add("data", new JsonObject());

            return componentCondition(sealedTag, sealed, componentCondition(dataTag, filled, plain));
        });
    }

    private static JsonObject thoughtKnotModel() {
        var dataTag = new JsonObject();
        dataTag.add("data", new JsonObject());
        return componentCondition(dataTag,
            simpleModel("item/thought_knot_written"), simpleModel("item/thought_knot"));
    }

    private static JsonObject slateModel() {
        return hasComponent("minecraft:block_entity_data",
            simpleModel("item/slate_written"), simpleModel("item/slate_blank"));
    }

    private static JsonObject packagedModel(String path) {
        return variantChain(variant -> {
            var filled = simpleModel("item/" + path + "_" + variant + "_filled");
            var plain = simpleModel(variant == 0 ? "item/" + path : "item/" + path + "_" + variant);

            var patternsTag = new JsonObject();
            var patterns = new JsonArray();
            patterns.add(new JsonObject());
            patternsTag.add("patterns", patterns);

            return componentCondition(patternsTag, filled, plain);
        });
    }

    @Override
    public String getName() {
        return "HexCasting item model definitions";
    }
}
