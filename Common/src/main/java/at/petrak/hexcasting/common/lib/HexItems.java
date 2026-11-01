package at.petrak.hexcasting.common.lib;

import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.common.items.ItemGuideBook;
import at.petrak.hexcasting.common.items.ItemJewelerHammer;
import at.petrak.hexcasting.common.items.ItemLens;
import at.petrak.hexcasting.common.items.ItemLoreFragment;
import at.petrak.hexcasting.common.items.ItemScryingLensAssembly;
import at.petrak.hexcasting.common.items.ItemStaff;
import at.petrak.hexcasting.common.items.magic.*;
import at.petrak.hexcasting.common.items.pigment.*;
import at.petrak.hexcasting.common.items.storage.*;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import com.google.common.base.Suppliers;
import net.minecraft.util.Util;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

// https://github.com/VazkiiMods/Botania/blob/2c4f7fdf9ebf0c0afa1406dfe1322841133d75fa/Common/src/main/java/vazkii/botania/common/item/ModItems.java
public class HexItems {
    public static void registerItems(BiConsumer<Item, Identifier> r) {
        for (var e : ITEMS.entrySet()) {
            r.accept(e.getValue(), e.getKey());
        }
    }

    public static void registerItemCreativeTab(Object r, CreativeModeTab tab) {
        if (tab == HexCreativeTabs.SCROLLS) generateScrollEntries();
        for (var item : ITEM_TABS.getOrDefault(tab, List.of())) {
            item.register(r);
        }
    }

    private static final Map<Identifier, Item> ITEMS = new LinkedHashMap<>(); // preserve insertion order
    private static final Map<CreativeModeTab, List<TabEntry>> ITEM_TABS = new LinkedHashMap<>();


    public static final Item AMETHYST_DUST = make("amethyst_dust", new Item(props().setId(itemKey("amethyst_dust"))));
    public static final Item CHARGED_AMETHYST = make("charged_amethyst", new Item(props().setId(itemKey("charged_amethyst"))));

    public static final ItemGuideBook GUIDE_BOOK = make("guide_book",
        new ItemGuideBook(unstackable().setId(itemKey("guide_book"))));

    public static final Item QUENCHED_SHARD = make("quenched_allay_shard", new Item(props().rarity(Rarity.UNCOMMON).setId(itemKey("quenched_allay_shard"))));

    public static final ItemStaff STAFF_OAK = make("staff/oak", new ItemStaff(unstackable().setId(itemKey("staff/oak"))));
    public static final ItemStaff STAFF_SPRUCE = make("staff/spruce", new ItemStaff(unstackable().setId(itemKey("staff/spruce"))));
    public static final ItemStaff STAFF_BIRCH = make("staff/birch", new ItemStaff(unstackable().setId(itemKey("staff/birch"))));
    public static final ItemStaff STAFF_JUNGLE = make("staff/jungle", new ItemStaff(unstackable().setId(itemKey("staff/jungle"))));
    public static final ItemStaff STAFF_ACACIA = make("staff/acacia", new ItemStaff(unstackable().setId(itemKey("staff/acacia"))));
    public static final ItemStaff STAFF_DARK_OAK = make("staff/dark_oak", new ItemStaff(unstackable().setId(itemKey("staff/dark_oak"))));
    public static final ItemStaff STAFF_CRIMSON = make("staff/crimson", new ItemStaff(unstackable().setId(itemKey("staff/crimson"))));
    public static final ItemStaff STAFF_WARPED = make("staff/warped", new ItemStaff(unstackable().setId(itemKey("staff/warped"))));
    public static final ItemStaff STAFF_MANGROVE = make("staff/mangrove", new ItemStaff(unstackable().setId(itemKey("staff/mangrove"))));
    public static final ItemStaff STAFF_CHERRY = make("staff/cherry", new ItemStaff(unstackable().setId(itemKey("staff/cherry"))));
    public static final ItemStaff STAFF_BAMBOO = make("staff/bamboo", new ItemStaff(unstackable().setId(itemKey("staff/bamboo"))));
    public static final ItemStaff STAFF_EDIFIED = make("staff/edified", new ItemStaff(unstackable().setId(itemKey("staff/edified"))));
    public static final ItemStaff STAFF_QUENCHED = make("staff/quenched", new ItemStaff(unstackable().rarity(Rarity.UNCOMMON).setId(itemKey("staff/quenched"))));
    // mindsplice staffaratus
    public static final ItemStaff STAFF_MINDSPLICE = make("staff/mindsplice", new ItemStaff(unstackable().rarity(Rarity.UNCOMMON).setId(itemKey("staff/mindsplice"))));

    public static final ItemLens SCRYING_LENS = make("lens", new ItemLens(
        IXplatAbstractions.INSTANCE.addEquipSlotFabric(EquipmentSlot.HEAD)
            .stacksTo(1).setId(itemKey("lens"))));

    public static final ItemScryingLensAssembly LENS_ASSEMBLY = make("lens_assembly",
        new ItemScryingLensAssembly(unstackable().rarity(Rarity.UNCOMMON)
            .setId(itemKey("lens_assembly"))));

    public static final ItemAbacus ABACUS = make("abacus", new ItemAbacus(unstackable().setId(itemKey("abacus"))));
    public static final ItemThoughtKnot THOUGHT_KNOT = make("thought_knot", new ItemThoughtKnot(unstackable().setId(itemKey("thought_knot"))));
    public static final ItemFocus FOCUS = make("focus", new ItemFocus(unstackable().setId(itemKey("focus"))));
    public static final ItemSpellbook SPELLBOOK = make("spellbook", new ItemSpellbook(unstackable().setId(itemKey("spellbook"))));

    public static final ItemCypher ANCIENT_CYPHER = make("ancient_cypher", new ItemAncientCypher(unstackable().setId(itemKey("ancient_cypher"))));
    public static final ItemCypher CYPHER = make("cypher", new ItemCypher(unstackable().setId(itemKey("cypher"))));
    public static final ItemTrinket TRINKET = make("trinket", new ItemTrinket(unstackable().rarity(Rarity.UNCOMMON).setId(itemKey("trinket"))));
    public static final ItemArtifact ARTIFACT = make("artifact", new ItemArtifact(unstackable().rarity(Rarity.RARE).setId(itemKey("artifact"))));

    public static final ItemJewelerHammer JEWELER_HAMMER = make("jeweler_hammer",
        new ItemJewelerHammer(ToolMaterial.IRON, 0, -2.8F, props().stacksTo(1).durability(ToolMaterial.DIAMOND.durability()).setId(itemKey("jeweler_hammer"))));

    public static final ItemScroll SCROLL_SMOL = make("scroll_small", new ItemScroll(props().setId(itemKey("scroll_small")), 1));
    public static final ItemScroll SCROLL_MEDIUM = make("scroll_medium", new ItemScroll(props().setId(itemKey("scroll_medium")), 2));
    public static final ItemScroll SCROLL_LARGE = make("scroll", new ItemScroll(props().setId(itemKey("scroll")), 3));

    public static final ItemSlate SLATE = make("slate", new ItemSlate(HexBlocks.SLATE, props().setId(itemKey("slate"))));

    public static final ItemMediaBattery BATTERY = make("battery",
        new ItemMediaBattery(unstackable().setId(itemKey("battery"))), null);

    public static final Supplier<ItemStack> BATTERY_DUST_STACK = addToTab(() -> ItemMediaBattery.withMedia(
            new ItemStack(HexItems.BATTERY),
            MediaConstants.DUST_UNIT * 64,
            MediaConstants.DUST_UNIT * 64), HexCreativeTabs.HEX);
    public static final Supplier<ItemStack> BATTERY_SHARD_STACK = addToTab(() -> ItemMediaBattery.withMedia(
            new ItemStack(HexItems.BATTERY),
            MediaConstants.SHARD_UNIT * 64,
            MediaConstants.SHARD_UNIT * 64), HexCreativeTabs.HEX);
    public static final Supplier<ItemStack> BATTERY_CRYSTAL_STACK = addToTab(() -> ItemMediaBattery.withMedia(
            new ItemStack(HexItems.BATTERY),
            MediaConstants.CRYSTAL_UNIT * 64,
            MediaConstants.CRYSTAL_UNIT * 64), HexCreativeTabs.HEX);
    public static final Supplier<ItemStack> BATTERY_QUENCHED_SHARD_STACK = addToTab(() -> ItemMediaBattery.withMedia(
            new ItemStack(HexItems.BATTERY),
            MediaConstants.QUENCHED_SHARD_UNIT * 64,
            MediaConstants.QUENCHED_SHARD_UNIT * 64), HexCreativeTabs.HEX);
    public static final Supplier<ItemStack> BATTERY_QUENCHED_BLOCK_STACK = addToTab(() -> ItemMediaBattery.withMedia(
            new ItemStack(HexItems.BATTERY),
            MediaConstants.QUENCHED_BLOCK_UNIT * 64,
            MediaConstants.QUENCHED_BLOCK_UNIT * 64), HexCreativeTabs.HEX);

    public static final EnumMap<DyeColor, ItemDyePigment> DYE_PIGMENTS = Util.make(() -> {
        var out = new EnumMap<DyeColor, ItemDyePigment>(DyeColor.class);
        for (var dye : DyeColor.values()) {
            out.put(dye, make("dye_colorizer_" + dye.getName(), new ItemDyePigment(dye, unstackable().setId(itemKey("dye_colorizer_" + dye.getName())))));
        }
        return out;
    });
    public static final EnumMap<ItemPridePigment.Type, ItemPridePigment> PRIDE_PIGMENTS = Util.make(() -> {
        var out = new EnumMap<ItemPridePigment.Type, ItemPridePigment>(ItemPridePigment.Type.class);
        for (var politicsInMyVidya : ItemPridePigment.Type.values()) {
            out.put(politicsInMyVidya, make("pride_colorizer_" + politicsInMyVidya.getName(),
                new ItemPridePigment(politicsInMyVidya, unstackable().setId(itemKey("pride_colorizer_" + politicsInMyVidya.getName())))));
        }
        return out;
    });

    public static final Item UUID_PIGMENT = make("uuid_colorizer", new ItemUUIDPigment(unstackable().setId(itemKey("uuid_colorizer"))));
    public static final Item DEFAULT_PIGMENT = make("default_colorizer",
        new ItemAmethystPigment(unstackable().setId(itemKey("default_colorizer"))));
    public static final Item ANCIENT_PIGMENT = make("ancient_colorizer",
        new ItemAmethystAndCopperPigment(unstackable().setId(itemKey("ancient_colorizer"))));

    // BUFF SANDVICH
    public static final Item SUBMARINE_SANDWICH = make("sub_sandwich",
        new Item(props().food(new FoodProperties.Builder().nutrition(14).saturationModifier(1.2f).build()).setId(itemKey("sub_sandwich"))));

    public static final ItemLoreFragment LORE_FRAGMENT = make("lore_fragment",
        new ItemLoreFragment(unstackable()
            .rarity(Rarity.RARE).setId(itemKey("lore_fragment"))));

    public static final ItemCreativeUnlocker CREATIVE_UNLOCKER = make("creative_unlocker",
        new ItemCreativeUnlocker(unstackable()
            .rarity(Rarity.EPIC)
            .food(new FoodProperties.Builder().nutrition(20).saturationModifier(1f).alwaysEdible().build()).setId(itemKey("creative_unlocker"))));

    //

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, modLoc(name));
    }

    public static Item.Properties props() {
        return new Item.Properties();
    }

    public static Item.Properties unstackable() {
        return props().stacksTo(1);
    }

    private static void generateScrollEntries() {
        var keyList = new ArrayList<ResourceKey<ActionRegistryEntry>>();
        Registry<ActionRegistryEntry> regi = IXplatAbstractions.INSTANCE.getActionRegistry();
        for (var key : regi.registryKeySet())
            if (HexUtils.isOfTag(regi, key, HexTags.Actions.PER_WORLD_PATTERN))
                keyList.add(key);
        keyList.sort( (a, b) -> a.identifier().compareTo(b.identifier()) );
        for (var key : keyList) {
            addToTab(() -> ItemScroll.withPerWorldPattern(
                new ItemStack(HexItems.SCROLL_LARGE),
                key.identifier().toString()
            ),HexCreativeTabs.SCROLLS);
        }
    }

    private static <T extends Item> T make(Identifier id, T item, @Nullable CreativeModeTab tab) {
        var old = ITEMS.put(id, item);
        if (old != null) {
            throw new IllegalArgumentException("Typo? Duplicate id " + id);
        }
        if (tab != null) {
            ITEM_TABS.computeIfAbsent(tab, t -> new ArrayList<>()).add(new TabEntry.ItemEntry(item));
        }
        return item;
    }

    private static <T extends Item> T make(String id, T item, @Nullable CreativeModeTab tab) {
        return make(modLoc(id), item, tab);
    }

    private static <T extends Item> T make(String id, T item) {
        return make(modLoc(id), item, HexCreativeTabs.HEX);
    }

    private static Supplier<ItemStack> addToTab(Supplier<ItemStack> stack, CreativeModeTab tab) {
        var memoised = Suppliers.memoize(stack::get);
        ITEM_TABS.computeIfAbsent(tab, t -> new ArrayList<>()).add(new TabEntry.StackEntry(memoised));
        return memoised;
    }

    private static void acceptOutput(Object output, Object value) {
        for (var method : output.getClass().getMethods()) {
            if (method.getName().equals("accept") && method.getParameterCount() == 1
                && method.getParameterTypes()[0].isInstance(value)) {
                try {
                    method.setAccessible(true);
                    method.invoke(output, value);
                    return;
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        throw new IllegalArgumentException("No suitable accept method on " + output.getClass());
    }

    private static abstract class TabEntry {
        abstract void register(Object r);

        static class ItemEntry extends TabEntry {
            private final Item item;

            ItemEntry(Item item) {
                this.item = item;
            }

            @Override
            void register(Object r) {
                acceptOutput(r, item);
            }
        }

        static class StackEntry extends TabEntry {
            private final Supplier<ItemStack> stack;

            StackEntry(Supplier<ItemStack> stack) {
                this.stack = stack;
            }

            @Override
            void register(Object r) {
                acceptOutput(r, stack.get());
            }
        }
    }
}
