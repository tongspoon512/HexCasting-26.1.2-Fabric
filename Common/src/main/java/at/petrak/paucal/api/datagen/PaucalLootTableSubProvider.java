package at.petrak.paucal.api.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public abstract class PaucalLootTableSubProvider implements net.minecraft.data.loot.LootTableSubProvider {
    protected final String modid;
    protected final HolderLookup.Provider registries;

    protected PaucalLootTableSubProvider(String modid, HolderLookup.Provider registries) {
        this.modid = modid;
        this.registries = registries;
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> writer) {
        Map<Block, LootTable.Builder> blockTables = new HashMap<>();
        Map<Identifier, LootTable.Builder> lootTables = new HashMap<>();
        makeLootTables(blockTables, lootTables);
        for (var entry : blockTables.entrySet()) {
            // 26.1 expects block loot tables under data/<ns>/loot_table/blocks/<id>.json.
            writer.accept(ResourceKey.create(Registries.LOOT_TABLE,
                    BuiltInRegistries.BLOCK.getKey(entry.getKey()).withPrefix("blocks/")),
                entry.getValue());
        }
        for (var entry : lootTables.entrySet()) {
            writer.accept(ResourceKey.create(Registries.LOOT_TABLE, entry.getKey()), entry.getValue());
        }
    }

    protected abstract void makeLootTables(Map<Block, LootTable.Builder> blockTables,
                                           Map<Identifier, LootTable.Builder> lootTables);

    protected LootPool.Builder dropThisPool(ItemLike item, int count) {
        return LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1))
            .add(LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(count))));
    }

    protected final void dropSelf(Map<Block, LootTable.Builder> blockTables, Block... blocks) {
        for (Block block : blocks) {
            blockTables.put(block, LootTable.lootTable().withPool(dropThisPool(block, 1)));
        }
    }
}
