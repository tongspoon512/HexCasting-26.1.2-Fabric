package at.petrak.hexcasting.interop.emi;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.client.render.PatternColors;
import at.petrak.hexcasting.client.render.PatternRenderer;
import at.petrak.hexcasting.client.render.PatternSettings;
import at.petrak.hexcasting.client.render.PatternSettings.PositionSettings;
import at.petrak.hexcasting.client.render.PatternSettings.StrokeSettings;
import at.petrak.hexcasting.client.render.PatternSettings.ZappySettings;
import at.petrak.hexcasting.interop.utils.PatternDrawingUtil;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import dev.emi.emi.api.render.EmiRenderable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class PatternRendererEMI implements EmiRenderable {

    private final int width;
    private final int height;

    private int xOffset = 0;
    private int yOffset = 0;

    private boolean strokeOrder;

    private final HexPattern pat;
    private PatternSettings patSets;

    public PatternRendererEMI(Identifier pattern, int w, int h) {
        var regi = IXplatAbstractions.INSTANCE.getActionRegistry();
        var entry = regi.get(pattern);
        this.strokeOrder = HexUtils.isOfTag(regi, pattern, HexTags.Actions.PER_WORLD_PATTERN);
        this.pat = regi.getValue(pattern).prototype();
        this.width = w;
        this.height = h;
        this.patSets = new PatternSettings("pattern_drawable_" + w + "_" + h,
                new PositionSettings(width, height, 0, 0,
                        PatternSettings.AxisAlignment.CENTER_FIT, PatternSettings.AxisAlignment.CENTER_FIT, Math.max(width, height), 0, 0),
                StrokeSettings.fromStroke(0.075 * Math.min(width, height)),
                ZappySettings.READABLE);
    }

    public PatternRendererEMI shift(int x, int y) {
        xOffset += x;
        yOffset += y;
        return this;
    }

    public PatternRendererEMI strokeOrder(boolean order) {
        if(order != strokeOrder){
            patSets = new PatternSettings("pattern_drawable_" + width + "_" + height + (order ? "" : "nostroke"),
                    patSets.posSets,
                    patSets.strokeSets,
                    order ? ZappySettings.READABLE : ZappySettings.STATIC
            );
        }
        strokeOrder = order;
        return this;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int x, int y, float delta) {
        PatternDrawingUtil.renderPattern(graphics, pat, patSets,
                new PatternColors(0xc8_0c0a0c, 0xff_333030).withDotColors(0x80_666363, 0),
                0, 10, xOffset + x, yOffset + y + 1f);
    }
}
