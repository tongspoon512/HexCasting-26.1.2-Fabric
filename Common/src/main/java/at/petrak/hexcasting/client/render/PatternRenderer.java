package at.petrak.hexcasting.client.render;


import at.petrak.hexcasting.api.casting.math.HexPattern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec2;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class PatternRenderer {

    public static boolean shouldDoStrokeGradient() {
        return true;
    }

    private static final float OUTER_Z = 0.0005f;
    private static final float INNER_Z = 0.001f;

    public static void renderPattern(HexPattern pattern, PoseStack ps, PatternSettings patSets, PatternColors patColors, double seed, int resPerUnit) {
        renderPattern(pattern, ps, null, patSets, patColors, seed, resPerUnit);
    }

    public static void renderPattern(HexPattern pattern, PoseStack ps, @Nullable WorldlyBits worldlyBits, PatternSettings patSets, PatternColors patColors, double seed, int resPerUnit) {
        renderPattern(HexPatternLike.of(pattern), ps, worldlyBits, patSets, patColors, seed, resPerUnit);
    }

    /**
     * Renders a pattern (or rather a pattern-like) according to the given settings.
     * @param patternlike the pattern (or more generally the lines) to render.
     * @param ps pose/matrix stack to render based on. (0,0) is treated as the top left corner. The size of the render is determined by patSets.
     * @param worldlyBits where the resulting geometry goes. Null means "throw it away".
     * @param patSets settings that control how the pattern is drawn.
     * @param patColors colors to use for drawing the pattern and dots.
     * @param seed seed to use for zappy wobbles.
     * @param resPerUnit the texture resolution per pose unit space to be used *if* the texture renderer is used.
     */
    public static void renderPattern(HexPatternLike patternlike, PoseStack ps, @Nullable WorldlyBits worldlyBits, PatternSettings patSets, PatternColors patColors, double seed, int resPerUnit){
        HexPatternPoints staticPoints = HexPatternPoints.getStaticPoints(patternlike, patSets, seed);

        // 26.1 has no immediate-mode custom geometry, so everything below accumulates into this and is
        // replayed into the caller's sink once it's complete.
        MeshCollector mesh = new MeshCollector();

        boolean shouldRenderDynamic = true;

        // only do texture rendering if it's static and has solid colors
        if(patSets.getSpeed() == 0 && PatternTextureManager.useTextures && patColors.innerStartColor() == patColors.innerEndColor()
        && patColors.outerStartColor() == patColors.outerEndColor()){
            boolean didRender = renderPatternTexture(patternlike, ps, mesh, patSets, patColors, seed, resPerUnit);
            if(didRender) shouldRenderDynamic = false;
        }
        if(shouldRenderDynamic){
            List<Vec2> zappyPattern;

            if(patSets.getSpeed() == 0) {
                // re-use our static points if we're rendering a static pattern anyway
                zappyPattern = staticPoints.zappyPoints;
            } else {
                List<Vec2> nonzappyLines = patternlike.getNonZappyPoints();
                Set<Integer> dupIndices = RenderLib.findDupIndices(nonzappyLines);
                zappyPattern = RenderLib.makeZappy(nonzappyLines, dupIndices,
                        patSets.getHops(), patSets.getVariance(), patSets.getSpeed(), patSets.getFlowIrregular(),
                        patSets.getReadabilityOffset(), patSets.getLastSegmentProp(), seed);
            }

            List<Vec2> zappyRenderSpace = staticPoints.scaleVecs(zappyPattern);

            if(ARGB.alpha(patColors.outerEndColor()) != 0 && ARGB.alpha(patColors.outerStartColor()) != 0){
                RenderLib.drawLineSeq(ps.last().pose(), zappyRenderSpace, (float)patSets.getOuterWidth(staticPoints.finalScale),
                        OUTER_Z, patColors.outerStartColor(), patColors.outerEndColor(), mesh);
            }
            if(ARGB.alpha(patColors.innerEndColor()) != 0 && ARGB.alpha(patColors.innerStartColor()) != 0) {
                RenderLib.drawLineSeq(ps.last().pose(), zappyRenderSpace, (float)patSets.getInnerWidth(staticPoints.finalScale),
                        INNER_Z, patColors.innerStartColor(), patColors.innerEndColor(), mesh);
            }
        }

        // render dots and grid dynamically

        float dotZ = 0.0011f;

        if(ARGB.alpha(patColors.startingDotColor()) != 0) {
            RenderLib.drawSpot(ps.last().pose(), staticPoints.dotsScaled.get(0), (float)patSets.getStartDotRadius(staticPoints.finalScale),
                    patColors.startingDotColor(), mesh, dotZ);
        }

        if(ARGB.alpha(patColors.gridDotsColor()) != 0) {
            for(int i = 1; i < staticPoints.dotsScaled.size(); i++){
                Vec2 gridDot = staticPoints.dotsScaled.get(i);
                RenderLib.drawSpot(ps.last().pose(), gridDot, (float)patSets.getGridDotsRadius(staticPoints.finalScale),
                        patColors.gridDotsColor(), mesh, dotZ);
            }
        }

        if (worldlyBits != null) {
            mesh.submitTo(worldlyBits.sink());
        }
    }

    private static boolean renderPatternTexture(HexPatternLike patternlike, PoseStack ps, MeshCollector mesh, PatternSettings patSets, PatternColors patColors, double seed, int resPerUnit){
        Optional<Map<String, Identifier>> maybeTextures = PatternTextureManager.getTextures(patternlike, patSets, seed, resPerUnit);
        if(maybeTextures.isEmpty()){
            return false;
        }

        Map<String, Identifier> textures = maybeTextures.get();
        HexPatternPoints staticPoints = HexPatternPoints.getStaticPoints(patternlike, patSets, seed);

        if(ARGB.alpha(patColors.outerStartColor()) != 0) {
            drawPatternQuad(ps, mesh, textures.get("outer"), staticPoints, patColors.outerStartColor(), OUTER_Z);
        }

        if(ARGB.alpha(patColors.innerStartColor()) != 0) {
            drawPatternQuad(ps, mesh, textures.get("inner"), staticPoints, patColors.innerStartColor(), INNER_Z);
        }

        return true;
    }

    private static void drawPatternQuad(PoseStack ps, MeshCollector mesh, Identifier texture, HexPatternPoints staticPoints,
                                        int color, float z) {
        float w = (float) staticPoints.fullWidth;
        float h = (float) staticPoints.fullHeight;
        mesh.begin(VertexFormat.Mode.QUADS, ps.last().pose(), texture);
        mesh.vertex(0f, 0f, z, color, 0f, 0f);
        mesh.vertex(0f, h, z, color, 0f, 1f);
        mesh.vertex(w, h, z, color, 1f, 1f);
        mesh.vertex(w, 0f, z, color, 1f, 0f);
        mesh.end();
    }

    /**
     * Where a pattern's geometry should end up: the world's submit queue, or the GUI's render state list.
     */
    public record WorldlyBits(MeshSink sink) {}
}
