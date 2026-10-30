package at.petrak.paucal.api.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public abstract class PaucalItemTagProvider extends IntrinsicHolderTagsProvider<Item> {
    protected final PaucalBlockTagProvider blockTagsProvider;

    protected PaucalItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                    String modid, PaucalBlockTagProvider blockTagsProvider) {
        super(output, Registries.ITEM, lookupProvider,
            item -> BuiltInRegistries.ITEM.getResourceKey(item).orElseThrow());
        this.blockTagsProvider = blockTagsProvider;
    }

    protected void copy(TagKey<Block> blockTag, TagKey<Item> itemTag) {
        var itemBuilder = this.getOrCreateRawBuilder(itemTag);
        var blockBuilder = this.blockTagsProvider.rawBuilder(blockTag);
        for (TagEntry entry : blockBuilder.build()) {
            itemBuilder.add(entry);
        }
    }
}
