package at.petrak.hexcasting.fabric.loot;

import at.petrak.hexcasting.common.lib.HexItems;
import at.petrak.hexcasting.common.loot.AddPerWorldPatternToScrollFunc;
import at.petrak.hexcasting.common.loot.AddHexToAncientCypherFunc;
import at.petrak.hexcasting.fabric.FabricHexInitializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.NestedLootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

import static at.petrak.hexcasting.api.HexAPI.modLoc;
import static at.petrak.hexcasting.common.loot.HexLootHandler.TABLE_INJECT_AMETHYST_CLUSTER;

public class FabricHexLootModJankery {
    public static final Identifier FUNC_AMETHYST_SHARD_REDUCER = modLoc("amethyst_shard_reducer");
    public static final Identifier RANDOM_SCROLL_TABLE = modLoc("random_scroll");
    public static final Identifier RANDOM_CYPHER_TABLE = modLoc("random_cypher");

    public static void lootLoad(Identifier id, Consumer<LootPool.Builder> addPool) {
        var amethystClusterTable = Blocks.AMETHYST_CLUSTER.getLootTable()
            .map(key -> key.identifier()).orElse(null);
        if (id.equals(amethystClusterTable)) {
            addPool.accept(makeAmethystInjectPool());
        } else if (id.equals(RANDOM_SCROLL_TABLE)) {
            // -1 weight = guaranteed spawn
            addPool.accept(makeScrollAddPool(-1));
        } else if (id.equals(RANDOM_CYPHER_TABLE)) {
            // 1 chance = guaranteed spawn
            addPool.accept(makeCypherAddPool(1));
        }

        int countRange = FabricHexInitializer.CONFIG.server.scrollRangeForLootTable(id);
        if (countRange != -1) {
            addPool.accept(makeScrollAddPool(countRange));
        }

        if (FabricHexInitializer.CONFIG.server.shouldInjectLore(id)) {
            addPool.accept(makeLoreAddPool(FabricHexInitializer.CONFIG.server.loreChance()));
        }

        if (FabricHexInitializer.CONFIG.server.shouldInjectCyphers(id)) {
            addPool.accept(makeCypherAddPool(FabricHexInitializer.CONFIG.server.cypherChance()));
        }
    }

    @NotNull
    private static LootPool.Builder makeAmethystInjectPool() {
        return LootPool.lootPool()
            .add(NestedLootTable.lootTableReference(
                net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, TABLE_INJECT_AMETHYST_CLUSTER)));
    }

    private static LootPool.Builder makeScrollAddPool(int range) {
        return LootPool.lootPool()
            .setRolls(range < 0 ? ConstantValue.exactly(1) : UniformGenerator.between(-range, range))
            .add(LootItem.lootTableItem(HexItems.SCROLL_LARGE))
            .apply(() -> new AddPerWorldPatternToScrollFunc(List.of()));
    }

    private static LootPool.Builder makeLoreAddPool(double chance) {
        return LootPool.lootPool()
            .when(LootItemRandomChanceCondition.randomChance((float) chance))
            .setRolls(ConstantValue.exactly(1))
            .add(LootItem.lootTableItem(HexItems.LORE_FRAGMENT));
    }

    private static LootPool.Builder makeCypherAddPool(double chance) {
        return LootPool.lootPool()
            .when(LootItemRandomChanceCondition.randomChance((float) chance))
            .setRolls(ConstantValue.exactly(1))
            .add(LootItem.lootTableItem(HexItems.ANCIENT_CYPHER))
            .apply(() -> new AddHexToAncientCypherFunc(List.of()));
    }
}
