package pub.pigeon.yggdyy.hexcreating;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks;
import pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * L1 registry holder. Only touched after {@link HexCreatingCompat#isCreateLoaded()} returned true.
 */
public final class HexCreatingRegistration {
    private static final Map<Identifier, Item> BLOCK_ITEMS = new LinkedHashMap<>();

    private HexCreatingRegistration() {
    }

    public static void registerFluids(BiConsumer<net.minecraft.world.level.material.Fluid, Identifier> r) {
        pub.pigeon.yggdyy.hexcreating.fluids.HexCreatingFluids.registerFluids(r);
    }

    public static void registerBlocks(BiConsumer<Block, Identifier> r) {
        HexCreatingBlocks.registerBlocks(r);
        HexCreatingBlocks.all().forEach((id, block) -> {
            if (block != HexCreatingBlocks.MEDIA) {
                BLOCK_ITEMS.put(id, new BlockItem(block,
                    new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
            }
        });
    }

    public static void registerBlockItems(BiConsumer<Item, Identifier> r) {
        BLOCK_ITEMS.forEach((id, item) -> r.accept(item, id));
    }

    public static void registerItems(BiConsumer<Item, Identifier> r) {
        HexCreatingItems.registerItems(r);
    }

    public static void registerBlockEntities(BiConsumer<BlockEntityType<?>, Identifier> r) {
        HexCreatingBlockEntities.registerBlockEntities(r);
    }

    public static void registerActions(
        BiConsumer<at.petrak.hexcasting.api.casting.ActionRegistryEntry, Identifier> r) {
        pub.pigeon.yggdyy.hexcreating.cast.HexCreatingActions.INSTANCE.register(r);
    }

    public static void addCreativeTab(net.minecraft.world.item.CreativeModeTab.Output output) {
        BLOCK_ITEMS.values().forEach(output::accept);
        HexCreatingItems.all().values().forEach(output::accept);
    }

    public static void init() {
        // Create-specific wiring is installed here; this method runs only with Create present.
        HexCreatingCreateIntegration.init();
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, HexCreatingCompat.id(name));
    }
}
