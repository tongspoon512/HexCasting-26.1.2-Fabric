package dev.emi.trinkets.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.Optional;

/**
 * Stub of trinkets' TrinketsApi.
 */
public class TrinketsApi {
    public static void registerTrinket(Item item, Trinket trinket) {
        // no-op
    }

    public static Optional<TrinketComponent> getTrinketComponent(Player player) {
        return Optional.empty();
    }
}
