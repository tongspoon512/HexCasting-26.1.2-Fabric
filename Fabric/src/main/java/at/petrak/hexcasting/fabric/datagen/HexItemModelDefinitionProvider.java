package at.petrak.hexcasting.fabric.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import at.petrak.hexcasting.common.items.storage.ItemScroll;
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
    private static final Map<String, String> SCROLL_SIZES = Map.of(
        "scroll_small", "small",
        "scroll_medium", "medium",
        "scroll", "large");
    private static final Set<String> PACKAGED_ITEMS = Set.of("artifact", "trinket", "cypher", "ancient_cypher");
    private static final int VARIANTS = 8;

    static {
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
            } else if (SCROLL_SIZES.containsKey(path)) {
                model = scrollModel(SCROLL_SIZES.get(path));
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

    /**
     * Item models store the base texture on layer 0 and a grayscale iota indicator on layer 1.
     * Tints are indexed by layer: leave the base white and paint the indicator with the iota color.
     */
    private static JsonObject tintedModel(String path) {
        var model = simpleModel(path);
        var tints = new JsonArray();
        var base = new JsonObject();
        base.addProperty("type", "minecraft:constant");
        base.addProperty("value", -1);
        tints.add(base);
        var iota = new JsonObject();
        iota.addProperty("type", NAMESPACE + ":iota_storage_color");
        tints.add(iota);
        model.add("tints", tints);
        return model;
    }

    private static JsonObject variantCondition(int variant, JsonObject onTrue, JsonObject onFalse) {
        // 内置的 minecraft:component 谓词会用 JsonOps 解析 NBT，小整数会变成 ByteTag，
        // 而 variant 是 putInt 写入的 IntTag，永远匹配不上，所以改用自定义谓词。
        var condition = new JsonObject();
        condition.addProperty("type", "minecraft:condition");
        condition.addProperty("property", NAMESPACE + ":variant");
        condition.addProperty("variant", variant);
        condition.add("on_true", onTrue);
        condition.add("on_false", onFalse);
        return condition;
    }

    private static JsonObject tagCondition(String tag, JsonObject onTrue, JsonObject onFalse) {
        var condition = new JsonObject();
        condition.addProperty("type", "minecraft:condition");
        condition.addProperty("property", NAMESPACE + ":custom_data_tag");
        condition.addProperty("tag", tag);
        condition.add("on_true", onTrue);
        condition.add("on_false", onFalse);
        return condition;
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
            root = variantCondition(variant, statesForVariant.apply(variant), root);
        }
        return root;
    }

    private static JsonObject sealableVariantModel(String path) {
        return variantChain(variant -> {
            var sealed = tintedModel("item/" + path + "_" + variant + "_sealed");
            var filled = tintedModel("item/" + path + "_" + variant + "_filled");
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
            tintedModel("item/thought_knot_written"), simpleModel("item/thought_knot"));
    }

    private static JsonObject scrollModel(String size) {
        // 远古卷轴只是在 custom_data 里存了一个任意 op_id 字符串，内置谓词无法表达"任意字符串"。
        return tagCondition(ItemScroll.TAG_OP_ID,
            simpleModel("item/scroll_ancient_" + size),
            simpleModel("item/scroll_pristine_" + size));
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
