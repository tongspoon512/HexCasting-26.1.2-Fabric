package at.petrak.hexcasting.datagen;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.advancements.FailToCastGreatSpellTrigger;
import at.petrak.hexcasting.api.advancements.HexAdvancementTriggers;
import at.petrak.hexcasting.api.advancements.OvercastTrigger;
import at.petrak.hexcasting.api.advancements.SpendMediaTrigger;
import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.common.items.ItemLoreFragment;
import at.petrak.hexcasting.common.lib.HexBlocks;
import at.petrak.hexcasting.common.lib.HexItems;
import at.petrak.paucal.api.datagen.PaucalAdvancementSubProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.criterion.*;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Consumer;

public class HexAdvancements extends PaucalAdvancementSubProvider {
    public static final Criterion<OvercastTrigger.Instance> ENLIGHTEN =
        new Criterion<>(HexAdvancementTriggers.OVERCAST_TRIGGER,
            new OvercastTrigger.Instance(ContextAwarePredicate.create(),
                MinMaxBounds.Ints.ANY,
                // add a little bit of slop here. use 80% or more health ...
                MinMaxBounds.Doubles.atLeast(0.8),
                // and be left with under 1 healthpoint (half a heart)
                // TODO this means if 80% of your health is less than half a heart, so if you have 2.5 hearts or
                //  less, you can't become enlightened.
                MinMaxBounds.Doubles.between(Double.MIN_NORMAL, 1.0)));

    public HexAdvancements() {
        super(HexAPI.MOD_ID);
    }

    @Override
    public void generate(HolderLookup.Provider provider, Consumer<AdvancementHolder> consumer) {
        HolderGetter<Item> items = provider.lookupOrThrow(Registries.ITEM);

        var root = Advancement.Builder.advancement()
            // what an ergonomic design decision
            // i am so happy that data generators are the future
            .display(Items.BUDDING_AMETHYST,
                Component.translatable("advancement.hexcasting:root"),
                Component.translatable("advancement.hexcasting:root.desc"),
                Identifier.fromNamespaceAndPath("minecraft", "textures/block/calcite.png"),
                AdvancementType.TASK, true, true, true)
            // the only thing making this vaguely tolerable is the knowledge the json files are worse somehow
            .addCriterion("has_charged_amethyst", InventoryChangeTrigger.TriggerInstance.hasItems(
                ItemPredicate.Builder.item().of(items, HexTags.Items.GRANTS_ROOT_ADVANCEMENT).build()))
            .save(consumer, prefix("root")); // how the hell does one even read this

        //Creative Debug Unlocker
        Advancement.Builder.advancement()
            .display(HexItems.CREATIVE_UNLOCKER,
                Component.translatable("advancement.hexcasting:creative_unlocker"),
                Component.translatable("advancement.hexcasting:creative_unlocker.desc"),
                Identifier.fromNamespaceAndPath("minecraft", "textures/block/calcite.png"),
                AdvancementType.TASK, true, false, true)
            .parent(root)
            .addCriterion("has_creative_unlocker", InventoryChangeTrigger.TriggerInstance.hasItems(
                HexItems.CREATIVE_UNLOCKER))
            .save(consumer, prefix("creative_unlocker"));

        // weird names so we have alphabetical parity
        Advancement.Builder.advancement()
            .display(simpleDisplay(Items.GLISTERING_MELON_SLICE, "wasteful_cast", AdvancementType.TASK))
            .parent(root)
            .addCriterion("waste_amt", new Criterion<>(HexAdvancementTriggers.SPEND_MEDIA_TRIGGER,
                new SpendMediaTrigger.Instance(ContextAwarePredicate.create(),
                    MinMaxBounds.Doubles.ANY,
                    MinMaxBounds.Doubles.atLeast(89 * MediaConstants.DUST_UNIT / 10))))
            .save(consumer, prefix("aaa_wasteful_cast"));
        Advancement.Builder.advancement()
            .display(simpleDisplay(HexItems.CHARGED_AMETHYST, "big_cast", AdvancementType.TASK))
            .parent(root)
            .addCriterion("cast_amt", new Criterion<>(HexAdvancementTriggers.SPEND_MEDIA_TRIGGER,
                new SpendMediaTrigger.Instance(ContextAwarePredicate.create(),
                    MinMaxBounds.Doubles.atLeast(64 * MediaConstants.CRYSTAL_UNIT),
                    MinMaxBounds.Doubles.ANY)))
            .save(consumer, prefix("aab_big_cast"));

        var impotence = Advancement.Builder.advancement()
            .display(simpleDisplay(Items.BLAZE_POWDER, "y_u_no_cast_angy", AdvancementType.TASK))
            .parent(root)
            .addCriterion("did_the_thing", new Criterion<>(HexAdvancementTriggers.FAIL_GREAT_SPELL_TRIGGER,
                new FailToCastGreatSpellTrigger.Instance(ContextAwarePredicate.create())))
            .save(consumer, prefix("y_u_no_cast_angy"));

        var opened_eyes = Advancement.Builder.advancement()
            .display(simpleDisplay(Items.ENDER_EYE, "opened_eyes", AdvancementType.TASK))
            .parent(impotence)
            .addCriterion("health_used", new Criterion<>(HexAdvancementTriggers.OVERCAST_TRIGGER,
                new OvercastTrigger.Instance(ContextAwarePredicate.create(),
                    MinMaxBounds.Ints.ANY,
                    MinMaxBounds.Doubles.ANY,
                    // you can't just kill yourself
                    MinMaxBounds.Doubles.atLeast(0.0))))
            .save(consumer, prefix("opened_eyes"));

        Advancement.Builder.advancement()
            .display(Items.MUSIC_DISC_11,
                Component.translatable("advancement.hexcasting:enlightenment"),
                Component.translatable("advancement.hexcasting:enlightenment.desc"),
                null,
                AdvancementType.CHALLENGE, true, true, true)
            .parent(opened_eyes)
            .addCriterion("health_used", ENLIGHTEN)
            .save(consumer, prefix("enlightenment"));

        var loreRoot = Advancement.Builder.advancement()
            .display(simpleDisplayWithBackground(HexBlocks.AKASHIC_LIGATURE, "lore", AdvancementType.GOAL,
                modLoc("textures/block/slate.png")))
            .addCriterion("used_item", ConsumeItemTrigger.TriggerInstance.usedItem(items, HexItems.LORE_FRAGMENT))
            .save(consumer, prefix("lore"));

        for (var advId : ItemLoreFragment.NAMES) {
            Advancement.Builder.advancement()
                .display(HexItems.LORE_FRAGMENT,
                    Component.translatable("advancement." + advId), Component.empty(),
                    null, AdvancementType.TASK, true, true, true)
                .parent(loreRoot)
                .addCriterion(ItemLoreFragment.CRITEREON_KEY,
                    new Criterion<>(CriteriaTriggers.IMPOSSIBLE, new ImpossibleTrigger.TriggerInstance()))
                .save(consumer, advId.toString());
        }

//        super.registerAdvancements(consumer, fileHelper);
    }
}
