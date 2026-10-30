package at.petrak.hexcasting.interop.patchouli;

import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.client.render.PatternColors;
import at.petrak.hexcasting.client.render.PatternRenderer;
import at.petrak.hexcasting.client.render.PatternSettings;
import at.petrak.hexcasting.interop.utils.PatternDrawingUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import vazkii.patchouli.api.IComponentRenderContext;
import vazkii.patchouli.api.ICustomComponent;
import vazkii.patchouli.api.IVariable;

import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Page that has a hex pattern on it
 */
abstract public class AbstractPatternComponent implements ICustomComponent {
    protected transient int x, y;
    protected transient float hexSize;

    private transient List<HexPattern> patterns;

    /**
     * Pass -1, -1 to center it.
     */
    @Override
    public void build(int x, int y, int pagenum) {
        this.x = x == -1 ? 116 / 2 : x;
        this.y = y == -1 ? 70 : y;
    }

    public abstract List<HexPattern> getPatterns(UnaryOperator<IVariable> lookup);

    public abstract boolean showStrokeOrder();

    @Override
    public void render(GuiGraphicsExtractor graphics, IComponentRenderContext context, float pticks, int mouseX, int mouseY) {
        // want to position x: [0, 116], y: [16, 80]
        int cols = (int)Math.ceil(Math.sqrt(patterns.size()));
        int rows = (int)Math.ceil(patterns.size()/(double)cols);

        double cellW = 116 / (double)cols;
        double cellH = 64 / (double)rows;

        PatternSettings patSets = new PatternSettings("book" + patterns.size() + (showStrokeOrder() ? "" : "r"),
                new PatternSettings.PositionSettings(cellW, cellH, 2, 2,
                        PatternSettings.AxisAlignment.CENTER_FIT, PatternSettings.AxisAlignment.CENTER_FIT, 16, 0, 0),
                PatternSettings.StrokeSettings.fromStroke(4),
                showStrokeOrder() ? PatternSettings.ZappySettings.READABLE : PatternSettings.ZappySettings.STATIC
        );

        PatternColors patCols = PatternColors.DIMMED_COLOR.withDots(false, true);

        if(showStrokeOrder()){
            patCols = PatternRenderer.shouldDoStrokeGradient() ? PatternColors.DEFAULT_GRADIENT_COLOR.withDots(true, true)
                    : PatternColors.READABLE_GRID_SCROLL_COLORS;
        }

        for(int p = 0; p < patterns.size(); p++){

            int r = p / cols;
            int c = p % cols;
            HexPattern pattern = patterns.get(p);

            PatternDrawingUtil.renderPattern(graphics, pattern, patSets, patCols, 0, 4,
                (float) (cellW * c), (float) (cellH * r + 16));
        }
    }

    @Override
    public void onVariablesAvailable(UnaryOperator<IVariable> lookup) {
        this.patterns = this.getPatterns(lookup);
    }

    // used for deserialization from patchi
    protected static class RawPattern {
        protected String startdir;
        protected String signature;
        protected int q, r;
    }
}
