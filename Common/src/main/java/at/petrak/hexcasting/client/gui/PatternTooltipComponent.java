package at.petrak.hexcasting.client.gui;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.HexGuiRender;
import at.petrak.hexcasting.client.render.PatternColors;
import at.petrak.hexcasting.client.render.PatternRenderer;
import at.petrak.hexcasting.client.render.WorldlyPatternRenderHelpers;
import at.petrak.hexcasting.common.misc.PatternTooltip;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.jetbrains.annotations.Nullable;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

// https://github.com/VazkiiMods/Botania/blob/95bd2d3fbc857b7c102687554e1d1b112f8af436/Xplat/src/main/java/vazkii/botania/client/gui/ManaBarTooltipComponent.java
// yoink

/**
 * @see PatternTooltip the associated data for this
 */
public class PatternTooltipComponent implements ClientTooltipComponent {
    public static final Identifier PRISTINE_BG = modLoc("textures/gui/scroll.png");
    public static final Identifier ANCIENT_BG = modLoc("textures/gui/scroll_ancient.png");
    public static final Identifier SLATE_BG = modLoc("textures/gui/slate.png");

    private static final float RENDER_SIZE = 128f;
    private static final int TEXTURE_SIZE = 48;

    private final HexPattern pattern;
    private final Identifier background;

    public PatternTooltipComponent(PatternTooltip tt) {
        this.pattern = tt.pattern();
        this.background = tt.background();
    }

    @Nullable
    public static ClientTooltipComponent tryConvert(TooltipComponent cmp) {
        if (cmp instanceof PatternTooltip ptt) {
            return new PatternTooltipComponent(ptt);
        }
        return null;
    }

    @Override
    public void extractImage(Font font, int mouseX, int mouseY, int width, int height,
        GuiGraphicsExtractor graphics) {
        var guiPose = graphics.pose();
        guiPose.pushMatrix();
        guiPose.translate(mouseX, mouseY);
        renderBG(graphics, this.background);
        guiPose.popMatrix();

        // renderText happens *before* extractImage for some asinine reason
        var ps = new PoseStack();
        ps.translate(mouseX, mouseY, 0);
        ps.scale(RENDER_SIZE, RENDER_SIZE, 1);

        PatternRenderer.renderPattern(pattern, ps, new PatternRenderer.WorldlyBits(HexGuiRender.guiSink(graphics)),
                WorldlyPatternRenderHelpers.READABLE_SCROLL_SETTINGS,
                (PatternRenderer.shouldDoStrokeGradient() ? PatternColors.DEFAULT_GRADIENT_COLOR : PatternColors.DEFAULT_PATTERN_COLOR)
                        .withDots(true, true),
                0, 512);
    }

    private static void renderBG(GuiGraphicsExtractor graphics, Identifier background) {
        graphics.blit(
            background, // texture
            0, (int) RENDER_SIZE, 0, (int) RENDER_SIZE, // x1, x2, y1, y2
            0f, 0f, 1f, 1f); // u0, v0, u1, v1
    }

    @Override
    public int getWidth(Font pFont) {
        return (int) RENDER_SIZE;
    }

    @Override
    public int getHeight(Font pFont) {
        return (int) RENDER_SIZE;
    }
}
