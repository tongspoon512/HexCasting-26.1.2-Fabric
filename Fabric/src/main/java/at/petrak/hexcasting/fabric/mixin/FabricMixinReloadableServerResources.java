package at.petrak.hexcasting.fabric.mixin;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.fabric.loot.FabricHexLootModJankery;
import at.petrak.hexcasting.mixin.accessor.AccessorLootTable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Mixin(ReloadableServerResources.class)
public class FabricMixinReloadableServerResources {
    // Add the amethyst shard
    @Inject(method = "loadResources", at = @At("RETURN"), cancellable = true)
    private static void onLoadResources(CallbackInfoReturnable<CompletableFuture<ReloadableServerResources>> cir) {
        cir.setReturnValue(cir.getReturnValue().thenApply((rsr) -> {
            var registries = rsr.fullRegistries();
            var tableKey = Blocks.AMETHYST_CLUSTER.getLootTable().orElse(null);
            if (tableKey == null) {
                return rsr;
            }
            var amethystTable = registries.getLootTable(tableKey);
            var theCoolerAmethystTable = (AccessorLootTable) amethystTable;

            HolderLookup.RegistryLookup<LootItemFunction> modifiers =
                registries.lookup().lookupOrThrow(LootDataType.MODIFIER.registryKey());
            var shardReducer = modifiers.get(ResourceKey.create(
                    LootDataType.MODIFIER.registryKey(), FabricHexLootModJankery.FUNC_AMETHYST_SHARD_REDUCER))
                .map(Holder.Reference::value)
                .orElse(null);
            if (shardReducer != null) {
                var newFuncs = new ArrayList<>(theCoolerAmethystTable.hex$getFunctions());
                newFuncs.add(shardReducer);
                theCoolerAmethystTable.hex$setFunctions(newFuncs);
                theCoolerAmethystTable.hex$setCompositeFunction(LootItemFunctions.compose(newFuncs));
            } else {
                HexAPI.LOGGER.warn("{} was not found?", FabricHexLootModJankery.FUNC_AMETHYST_SHARD_REDUCER);
            }
            return rsr;
        }));
    }
}
