package pub.pigeon.yggdyy.hexcreating.client;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;
import pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems;

/**
 * Explains soul handling in JEI: how raw souls drop and how liquid soul is used by a spout.
 */
@JeiPlugin
public class HexCreatingJeiPlugin implements IModPlugin {
    private static final Identifier UID = HexCreatingCompat.id("jei");

    @Override
    public @NotNull Identifier getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        if (!HexCreatingCompat.isCreateLoaded()) {
            return;
        }
        registration.addItemStackInfo(new ItemStack(HexCreatingItems.RAW_SOUL),
            Component.translatable("hexcreating.jei.raw_soul.obtain"),
            Component.translatable("hexcreating.jei.raw_soul.mixing"),
            Component.translatable("hexcreating.jei.raw_soul.spout"));
    }
}
