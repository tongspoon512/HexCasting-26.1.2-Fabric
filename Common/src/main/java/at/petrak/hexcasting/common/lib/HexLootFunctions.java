package at.petrak.hexcasting.common.lib;

import at.petrak.hexcasting.common.loot.AddPerWorldPatternToScrollFunc;
import at.petrak.hexcasting.common.loot.AddHexToAncientCypherFunc;
import at.petrak.hexcasting.common.loot.AmethystReducerFunc;
import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class HexLootFunctions {
    public static void registerSerializers(BiConsumer<MapCodec<? extends LootItemFunction>, Identifier> r) {
        for (var e : LOOT_FUNCS.entrySet()) {
            r.accept(e.getValue(), e.getKey());
        }
    }

    private static final Map<Identifier, MapCodec<? extends LootItemFunction>> LOOT_FUNCS = new LinkedHashMap<>();

    public static final MapCodec<AddPerWorldPatternToScrollFunc> PATTERN_SCROLL = register("pattern_scroll",
        AddPerWorldPatternToScrollFunc.CODEC);
    public static final MapCodec<AddHexToAncientCypherFunc> HEX_CYPHER = register("hex_cypher",
        AddHexToAncientCypherFunc.CODEC);
    public static final MapCodec<AmethystReducerFunc> AMETHYST_SHARD_REDUCER = register("amethyst_shard_reducer",
        AmethystReducerFunc.CODEC);

    private static <T extends LootItemFunction> MapCodec<T> register(String id, MapCodec<T> lift) {
        var old = LOOT_FUNCS.put(modLoc(id), lift);
        if (old != null) {
            throw new IllegalArgumentException("Typo? Duplicate id " + id);
        }
        return lift;
    }
}
