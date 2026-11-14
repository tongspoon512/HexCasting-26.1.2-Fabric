package at.petrak.hexcasting.client;

import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.client.guidebook.GuideBookScreen;
import at.petrak.hexcasting.client.gui.GridSpacingScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Client-only hooks for common item code.
 * <p>
 * Dedicated servers refuse to load {@code net.minecraft.client.*}, so nothing in {@code common} or
 * {@code api} may reference client classes directly. Each method here is only invoked from a
 * client-side branch, which keeps this class merely <i>loaded</i> (never verified/initialized) on a
 * server.
 */
public final class ClientItemHooks {
    private ClientItemHooks() {
    }

    public static void openGuideBook() {
        Minecraft.getInstance().setScreen(new GuideBookScreen());
    }

    public static void openGridSpacing() {
        Minecraft.getInstance().setScreen(new GridSpacingScreen());
    }

    @Nullable
    public static Level currentLevel() {
        return Minecraft.getInstance().level;
    }

    /**
     * Item tint for {@code VisualOverride} items, cycled with the client tick counter.
     */
    public static int rainbowOverrideColor() {
        return 0xFF000000 | Mth.hsvToRgb(ClientTickCounter.getTotal() * 2 % 360 / 360F, 0.75F, 1F);
    }

    /**
     * Body of {@link IotaHolderItem#appendHoverText}; needs the client font to wrap the iota display.
     */
    public static void appendIotaHoverText(IotaHolderItem self, ItemStack stack, List<Component> components,
        TooltipFlag flag) {
        var datumTag = self.readIotaTag(stack);
        if (datumTag != null) {
            var mc = Minecraft.getInstance();
            var font = mc.font;
            var prefix = Component.translatable("hexcasting.spelldata.onitem", Component.empty());
            // wrap to the width of the screen, leaving room for the margins and the first-line prefix
            var maxWidth = Math.max(80, mc.getWindow().getGuiScaledWidth()
                - IotaHolderItem.TOOLTIP_SCREEN_MARGIN - font.width(prefix));
            var lines = IotaType.getDisplayLines(datumTag, font, maxWidth);
            components.add(Component.translatable("hexcasting.spelldata.onitem", lines.get(0)));
            for (int i = 1; i < lines.size(); i++) {
                components.add(Component.literal("    ").append(lines.get(i)));
            }

            if (flag.isAdvanced() && (HexConfig.client() == null || HexConfig.client().advancedTooltipsShowsIotaNBT())) {
                components.add(Component.literal("").append(NbtUtils.toPrettyComponent(datumTag)));
            }
        } else if (NBTHelper.hasString(stack, IotaHolderItem.TAG_OVERRIDE_VISUALLY)) {
            components.add(Component.translatable("hexcasting.spelldata.onitem",
                Component.translatable("hexcasting.spelldata.anything").withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
    }
}
