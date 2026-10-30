package at.petrak.hexcasting.interop.inline;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.client.render.PatternColors;
import at.petrak.hexcasting.client.render.PatternSettings;
import at.petrak.hexcasting.client.render.RenderLib;
import at.petrak.hexcasting.common.items.storage.ItemScroll;
import at.petrak.hexcasting.common.items.storage.ItemSlate;
import at.petrak.hexcasting.interop.utils.PatternDrawingUtil;
import com.samsthenerd.inline.api.client.extrahooks.ItemOverlayRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

@FunctionalInterface
public interface HexPatternOverlayRenderer extends ItemOverlayRenderer {
    HexPattern getPattern(ItemStack stack);

    HexPatternOverlayRenderer SCROLL_RENDERER = (stack) -> {
        var compound = NBTHelper.getCompound(stack, ItemScroll.TAG_PATTERN);
        if (compound != null && !NBTHelper.getBoolean(stack, ItemScroll.TAG_NEEDS_PURCHASE)) {
            return HexPattern.fromNBT(compound);

        }
        return null;
    };

    HexPatternOverlayRenderer SLATE_RENDERER = (stack) -> ItemSlate.getPattern(stack).orElse(null);

    PatternSettings OVERLAY_RENDER_SETTINGS = new PatternSettings("overlay",
        new PatternSettings.PositionSettings(14, 14, 0.5, 0.5,
            PatternSettings.AxisAlignment.CENTER_FIT, PatternSettings.AxisAlignment.CENTER_FIT, 4.0, 0, 0),
        PatternSettings.StrokeSettings.fromStroke(1.5),
        new PatternSettings.ZappySettings(10, 0, 0, 0,
            PatternSettings.ZappySettings.READABLE_OFFSET, 0.7f)
    );

    // higher contrast purple based on slate wobble color
    PatternColors HC_PURPLE_COLOR =
        new PatternColors(RenderLib.screenCol(0xff_cfa0f3), 0xff_763fa1);

    @Override
    default void render(ItemStack itemStack, GuiGraphicsExtractor guiGraphics) {
        if(!Minecraft.getInstance().hasShiftDown()) return;
        HexPattern pat = getPattern(itemStack);
        if(pat == null) return;
        var pose = guiGraphics.pose();
        PatternDrawingUtil.renderPattern(guiGraphics, pat, OVERLAY_RENDER_SETTINGS, HC_PURPLE_COLOR, 0, 16,
            pose.m20() + 1f, pose.m21() + 1f);
    }
}
