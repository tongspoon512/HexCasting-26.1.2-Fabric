package at.petrak.hexcasting.interop.utils;

import at.petrak.hexcasting.api.casting.math.HexCoord;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.client.render.HexGuiRender;
import at.petrak.hexcasting.client.render.MeshCollector;
import at.petrak.hexcasting.client.render.MeshVertex;
import at.petrak.hexcasting.client.render.PatternColors;
import at.petrak.hexcasting.client.render.PatternRenderer;
import at.petrak.hexcasting.client.render.PatternSettings;
import at.petrak.hexcasting.client.render.RenderLib;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class PatternDrawingUtil {
    public static void drawPattern(GuiGraphicsExtractor graphics, int x, int y, List<PatternEntry> patterns, List<Vec2> dots,
                   boolean strokeOrder, int outer, int innerLight, int innerDark,
                   int dotColor) {
        // 26.1's GUI draws through recorded render states, so the local translation lives in the mesh transform
        // rather than in the 2D pose stack.
        var mat = new Matrix4f().translate(x, y, 0);
        var mesh = new MeshCollector();

        // mark center
//        RenderLib.drawSpot(mat, Vec2.ZERO, 0f, 0f, 0f, 1f);

        for (var pat : patterns) {
            RenderLib.drawLineSeq(mat, pat.zappyPoints(), 5f, 0, outer, outer, mesh);
            RenderLib.drawLineSeq(mat, pat.zappyPoints(), 2f, 0,
                strokeOrder ? innerDark : innerLight, innerLight, mesh);

            if (strokeOrder) {
                RenderLib.drawSpot(mat, pat.zappyPoints().get(0), 2.5f, 1f, 0.1f, 0.15f, 0.6f, mesh);
            }
        }

        float dotR = ARGB.red(dotColor) / 255f;
        float dotG = ARGB.green(dotColor) / 255f;
        float dotB = ARGB.blue(dotColor) / 255f;
        float dotA = ARGB.alpha(dotColor) / 255f;

        for (var dot : dots) {
            RenderLib.drawSpot(mat, dot, 1.5f, dotR, dotG, dotB, dotA, mesh);
        }

        HexGuiRender.submitMesh(graphics, mesh);
    }

    /**
     * Renders a single pattern through {@link PatternRenderer} into the GUI's render-state queue.
     *
     * <p>{@code PatternRenderer} still works in a {@link PoseStack} and emits into a {@code MeshSink},
     * while the GUI only has a 2D pose stack and records meshes, so we render into a throwaway pose
     * stack and replay the captured meshes into a {@link MeshCollector}.</p>
     */
    public static void renderPattern(GuiGraphicsExtractor graphics, HexPattern pattern,
                                     PatternSettings patSets, PatternColors patColors,
                                     double seed, int resPerUnit, float x, float y) {
        var ps = new PoseStack();
        ps.translate(x, y, 0);

        var mesh = new MeshCollector();
        PatternRenderer.renderPattern(pattern, ps, new PatternRenderer.WorldlyBits(captured -> {
            mesh.begin(captured.mode, captured.transform, captured.texture);
            for (MeshVertex v : captured.vertices) {
                mesh.vertex(v.x, v.y, v.z, v.color, v.u, v.v);
            }
            mesh.end();
        }), patSets, patColors, seed, resPerUnit);

        HexGuiRender.submitMesh(graphics, mesh);
    }

    public static PatternRenderingData loadPatterns(List<Pair<HexPattern, HexCoord>> patterns,
        float readabilityOffset, float lastLineLenProp) {
        var patternEntries = new ArrayList<PatternEntry>(patterns.size());

        var fakeScale = 1;
        var seenFakePoints = new ArrayList<Vec2>();
        var seenCoords = new HashSet<HexCoord>();
        for (var pair : patterns) {
            var pattern = pair.getFirst();
            var origin = pair.getSecond();
            for (var pos : pattern.positions(origin)) {
                var px = HexUtils.coordToPx(pos, fakeScale, Vec2.ZERO);
                seenFakePoints.add(px);
            }

            // And while we're looping add the (COORD ONLY) things internally
            patternEntries.add(new PatternEntry(pattern, origin, new ArrayList<>()));
            seenCoords.addAll(pattern.positions(origin));
        }
        var fakeCom = HexUtils.findCenter(seenFakePoints);

        var maxDx = -1f;
        var maxDy = -1f;
        for (var dot : seenFakePoints) {
            var dx = Mth.abs(dot.x - fakeCom.x);
            if (dx > maxDx) {
                maxDx = dx;
            }
            var dy = Mth.abs(dot.y - fakeCom.y);
            if (dy > maxDy) {
                maxDy = dy;
            }
        }
        var hexSize = Math.min(12, Math.min(120 / 2.5f / maxDx, 70 / 2.5f / maxDy));

        var seenRealPoints = new ArrayList<Vec2>();
        for (var pat : patternEntries) {
            for (var pos : pat.pattern().positions(pat.origin())) {
                var px = HexUtils.coordToPx(pos, hexSize, Vec2.ZERO);
                seenRealPoints.add(px);
            }
        }
        var realCom = HexUtils.findCenter(seenRealPoints);

        // and NOW for real!
        for (int i = 0; i < patternEntries.size(); i++) {
            PatternEntry pat = patternEntries.get(i);
            var localOrigin = HexUtils.coordToPx(pat.origin(), hexSize, realCom.negated());
            var points = pat.pattern().toLines(hexSize, localOrigin);
            pat.zappyPoints()
                .addAll(RenderLib.makeZappy(points, RenderLib.findDupIndices(pat.pattern().positions()), 10, 0.8f, 0f,
                    0f, readabilityOffset, lastLineLenProp, i));
        }

        var pathfinderDots = seenCoords.stream()
            .map(coord -> HexUtils.coordToPx(coord, hexSize, realCom.negated())).toList();

        return new PatternRenderingData(patternEntries, pathfinderDots, hexSize);
    }

    public record PatternRenderingData(List<PatternEntry> patterns, List<Vec2> pathfinderDots, float hexSize) {
        // NO-OP
    }
}
