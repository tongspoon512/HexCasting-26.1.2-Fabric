package at.petrak.paucal.api.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import java.util.concurrent.CompletableFuture;

public abstract class PaucalBlockTagProvider extends IntrinsicHolderTagsProvider<Block> {
    protected PaucalBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, Registries.BLOCK, lookupProvider,
            block -> BuiltInRegistries.BLOCK.getResourceKey(block).orElseThrow());
    }

    public TagBuilder rawBuilder(TagKey<Block> tag) {
        return this.getOrCreateRawBuilder(tag);
    }
}
