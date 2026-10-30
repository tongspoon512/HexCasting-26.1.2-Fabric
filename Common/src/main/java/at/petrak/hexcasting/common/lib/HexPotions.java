package at.petrak.hexcasting.common.lib;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.Items;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class HexPotions {
    public static void register(BiConsumer<Potion, Identifier> r) {
        for (var e : POTIONS.entrySet()) {
            r.accept(e.getValue(), e.getKey());
        }
    }

    private static final Map<Identifier, Potion> POTIONS = new LinkedHashMap<>();

    public static final Potion ENLARGE_GRID = make("enlarge_grid",
        new Potion("enlarge_grid", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HexMobEffects.ENLARGE_GRID), 3600)));
    public static final Potion ENLARGE_GRID_LONG = make("enlarge_grid_long",
        new Potion("enlarge_grid_long", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HexMobEffects.ENLARGE_GRID), 9600)));
    public static final Potion ENLARGE_GRID_STRONG = make("enlarge_grid_strong",
        new Potion("enlarge_grid_strong", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HexMobEffects.ENLARGE_GRID), 1800, 1)));

    public static final Potion SHRINK_GRID = make("shrink_grid",
        new Potion("shrink_grid", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HexMobEffects.SHRINK_GRID), 3600)));
    public static final Potion SHRINK_GRID_LONG = make("shrink_grid_long",
        new Potion("shrink_grid_long", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HexMobEffects.SHRINK_GRID), 9600)));
    public static final Potion SHRINK_GRID_STRONG = make("shrink_grid_strong",
        new Potion("shrink_grid_strong", new MobEffectInstance(BuiltInRegistries.MOB_EFFECT.wrapAsHolder(HexMobEffects.SHRINK_GRID), 1800, 1)));

    /**
     * Called from {@code MixinPotionBrewing} while 26.1 assembles the vanilla brewing list.
     */
    public static void addRecipes(PotionBrewing.Builder builder) {
        builder.addMix(Potions.AWKWARD, HexItems.AMETHYST_DUST, holder(ENLARGE_GRID));
        builder.addMix(holder(ENLARGE_GRID), Items.REDSTONE, holder(ENLARGE_GRID_LONG));
        builder.addMix(holder(ENLARGE_GRID), Items.GLOWSTONE_DUST, holder(ENLARGE_GRID_STRONG));

        builder.addMix(holder(ENLARGE_GRID), Items.FERMENTED_SPIDER_EYE, holder(SHRINK_GRID));
        builder.addMix(holder(ENLARGE_GRID_LONG), Items.FERMENTED_SPIDER_EYE, holder(SHRINK_GRID_LONG));
        builder.addMix(holder(ENLARGE_GRID_STRONG), Items.FERMENTED_SPIDER_EYE, holder(SHRINK_GRID_STRONG));

        builder.addMix(holder(SHRINK_GRID), Items.REDSTONE, holder(SHRINK_GRID_LONG));
        builder.addMix(holder(SHRINK_GRID), Items.GLOWSTONE_DUST, holder(SHRINK_GRID_STRONG));
    }

    private static Holder<Potion> holder(Potion potion) {
        return BuiltInRegistries.POTION.wrapAsHolder(potion);
    }

    private static <T extends Potion> T make(String id, T potion) {
        var old = POTIONS.put(modLoc(id), potion);
        if (old != null) {
            throw new IllegalArgumentException("Typo? Duplicate id " + id);
        }
        return potion;
    }
}
