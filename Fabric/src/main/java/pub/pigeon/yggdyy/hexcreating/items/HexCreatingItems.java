package pub.pigeon.yggdyy.hexcreating.items;

import com.zurrtum.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class HexCreatingItems {
    private static final Map<Identifier, Item> ITEMS = new LinkedHashMap<>();

    public static final Item IOTA_WIRE_REEL = register("iota_wire_reel", Item::new);
    public static final Item EMPTY_REEL = register("empty_reel", Item::new);
    public static final Item IOTA_WIRE_CONNECTOR_TECH = register("iota_wire_connector_tech", Item::new);
    public static final Item IOTA_WIRE = register("iota_wire", Item::new);
    public static final Item RAW_SOUL = register("raw_soul",
        p -> new Item(p.fireResistant().rarity(Rarity.UNCOMMON)));
    public static final Item UNFINISHED_TRAIN_GATE_FRAME = register("unfinished_train_gate_frame",
        p -> new SequencedAssemblyItem(p.stacksTo(1)));
    public static final Item SQUARE = register("square", SquareItem::new);
    public static final Item PRINTED_PAPER = register("printed_paper", PrintedPaperItem::new);
    public static final Item PAPER_REEL = register("paper_reel", p -> new Item(p.stacksTo(1).durability(1024)));
    public static final Item MEDIA_BUCKET = register("media_bucket",
        p -> new net.minecraft.world.item.BucketItem(
            pub.pigeon.yggdyy.hexcreating.fluids.HexCreatingFluids.MEDIA, p.stacksTo(1)));

    public static final Item THOUGHT_KEY_HEXCASTING_INTRODUCTION = thoughtKey("hexcasting_introduction");
    public static final Item THOUGHT_KEY_BASIC_IOTA = thoughtKey("basic_iota");
    public static final Item THOUGHT_KEY_STACK = thoughtKey("stack");
    public static final Item THOUGHT_KEY_VECTOR = thoughtKey("vector");
    public static final Item THOUGHT_KEY_PATTERN_INTRODUCTION = thoughtKey("pattern_introduction");
    public static final Item THOUGHT_KEY_PATTERN_LOCATE = thoughtKey("pattern_locate");
    public static final Item THOUGHT_KEY_PATTERN_CONSTANTS = thoughtKey("pattern_constants");
    public static final Item THOUGHT_KEY_PATTERN_CALCULATE = thoughtKey("pattern_calculate");
    public static final Item THOUGHT_KEY_PATTERN_SPELL_1 = thoughtKey("pattern_spell_1");

    private HexCreatingItems() {
    }

    public static void registerItems(BiConsumer<Item, Identifier> r) {
        ITEMS.forEach((id, item) -> r.accept(item, id));
    }

    public static Map<Identifier, Item> all() {
        return ITEMS;
    }

    private static Item thoughtKey(String name) {
        return register("thought_key/" + name,
            p -> new Item(p.rarity(Rarity.UNCOMMON)));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory) {
        Item item = factory.apply(new Item.Properties().setId(key(name)));
        ITEMS.put(HexCreatingCompat.id(name), item);
        return item;
    }

    static ResourceKey<Item> key(String name) {
        return ResourceKey.create(Registries.ITEM, HexCreatingCompat.id(name));
    }
}
