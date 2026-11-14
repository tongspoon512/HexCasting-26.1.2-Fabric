package pub.pigeon.yggdyy.hexcreating.client;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.NullIota;
import at.petrak.hexcasting.api.casting.math.HexAngle;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.blocks.akashic.BlockEntityAkashicBookshelf;
import at.petrak.hexcasting.common.lib.HexBlocks;
import com.zurrtum.create.client.ponder.api.PonderPalette;
import com.zurrtum.create.client.ponder.api.scene.SceneBuilder;
import com.zurrtum.create.client.ponder.api.scene.SceneBuildingUtil;
import com.zurrtum.create.client.ponder.foundation.element.TextWindowElement;
import com.zurrtum.create.client.ponder.foundation.instruction.TextInstruction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Port of the upstream HexPonderUtils to the Create 6 ponder API.
 */
public final class HexPonderUtils {
    public static final int A_LONG_TIME = 20 * 60 * 10;

    private HexPonderUtils() {
    }

    private static double[] hexOffset(int index) {
        return switch (index) {
            case 0 -> new double[]{0.5, Math.sqrt(3.0) / 2.0};
            case 1 -> new double[]{1.0, 0.0};
            case 2 -> new double[]{0.5, -Math.sqrt(3.0) / 2.0};
            case 3 -> new double[]{-0.5, -Math.sqrt(3.0) / 2.0};
            case 4 -> new double[]{-1.0, 0.0};
            default -> new double[]{-0.5, Math.sqrt(3.0) / 2.0};
        };
    }

    private static int dirIndex(HexDir dir) {
        return switch (dir) {
            case NORTH_EAST -> 0;
            case EAST -> 1;
            case SOUTH_EAST -> 2;
            case SOUTH_WEST -> 3;
            case WEST -> 4;
            case NORTH_WEST -> 5;
        };
    }

    private static int angleIndex(HexAngle angle) {
        return switch (angle) {
            case FORWARD -> 0;
            case RIGHT -> 1;
            case RIGHT_BACK -> 2;
            case BACK -> 3;
            case LEFT_BACK -> 4;
            case LEFT -> 5;
        };
    }

    /**
     * Adds a text window whose content is computed at render time without consuming the scene's
     * sequential text localization index (the upstream implementation did the same through an accessor).
     */
    @SuppressWarnings("unchecked")
    private static void addDynamicText(SceneBuilder scene, TextWindowElement element, Supplier<String> text, int duration) {
        try {
            Field field = TextWindowElement.class.getDeclaredField("textGetter");
            field.setAccessible(true);
            field.set(element, text);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to set ponder text getter", e);
        }
        scene.addInstruction(new TextInstruction(element, duration));
    }

    public static class IotaDisplay {
        private final SceneBuilder scene;
        private final SceneBuildingUtil util;
        private final Iota iota;
        private BlockPos nowPos;

        public IotaDisplay(SceneBuilder scene, SceneBuildingUtil util, Iota iota) {
            this.scene = scene;
            this.util = util;
            this.iota = iota;
        }

        public void setPos(BlockPos pos) {
            this.nowPos = pos;
        }

        public Iota iota() {
            return iota;
        }

        public void showIota(int predictedDuration) {
            if (nowPos == null) {
                return;
            }
            scene.world().setBlock(nowPos, getBookshelfState(), false);
            scene.world().modifyBlockEntity(nowPos, BlockEntityAkashicBookshelf.class, be ->
                be.setNewMapping(getPattern(), iota));
            scene.world().showSection(util.select().position(nowPos), Direction.DOWN);

            TextWindowElement iotaText = new TextWindowElement();
            iotaText.builder(scene.getScene())
                .pointAt(util.vector().blockSurface(nowPos, Direction.NORTH).add(0, -0.25, 0))
                .placeNearTarget()
                .colored(getColor());
            addDynamicText(scene, iotaText, () -> iota.display().getString(), predictedDuration);
        }

        public void hideIota() {
            if (nowPos == null) {
                return;
            }
            scene.world().hideSection(util.select().position(nowPos), Direction.UP);
        }

        private BlockState getBookshelfState() {
            return HexBlocks.AKASHIC_BOOKSHELF.defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        }

        private HexPattern getPattern() {
            return HexPattern.fromAngles("w", HexDir.EAST);
        }

        private PonderPalette getColor() {
            return PonderPalette.WHITE;
        }
    }

    public static class StackDisplay {
        private final SceneBuilder scene;
        private final SceneBuildingUtil util;
        private final ArrayList<IotaDisplay> iotas = new ArrayList<>();
        private final BlockPos basePos;

        public StackDisplay(SceneBuilder scene, SceneBuildingUtil util, BlockPos basePos) {
            this.scene = scene;
            this.util = util;
            this.basePos = basePos;
        }

        public void push(Iota iota, int textDuration) {
            BlockPos pos = basePos.above(iotas.size() + 1);
            IotaDisplay display = new IotaDisplay(scene, util, iota);
            display.setPos(pos);
            display.showIota(textDuration);
            iotas.add(display);
        }

        public void pop() {
            if (iotas.isEmpty()) {
                return;
            }
            iotas.remove(iotas.size() - 1).hideIota();
        }

        public Iota top(int rId) {
            if (0 < rId && rId <= iotas.size()) {
                return iotas.get(iotas.size() - rId).iota();
            }
            return new NullIota();
        }

        public Iota top() {
            return top(1);
        }

        public Vec3 tipPos(int rId) {
            int offset = iotas.size() - rId + 1;
            return util.vector().blockSurface(basePos.above(offset), Direction.WEST).add(0, 0.25, 0);
        }
    }

    public static class ArrowDisplay {
        private final SceneBuilder scene;
        private Vec3 startPos = Vec3.ZERO;
        private Vec3 endPos = new Vec3(0, 1, 0);
        private double headSize = 1;
        private PonderPalette baseColor = PonderPalette.WHITE;
        private PonderPalette headColor = PonderPalette.WHITE;

        public ArrowDisplay(SceneBuilder scene) {
            this.scene = scene;
        }

        public ArrowDisplay start(Vec3 vec) {
            this.startPos = vec;
            return this;
        }

        public ArrowDisplay end(Vec3 vec) {
            this.endPos = vec;
            return this;
        }

        public ArrowDisplay size(double head) {
            this.headSize = head;
            return this;
        }

        public ArrowDisplay color(PonderPalette base, PonderPalette head) {
            this.baseColor = base;
            this.headColor = head;
            return this;
        }

        public void show(int duration) {
            scene.overlay().showLine(baseColor, startPos, endPos, duration);
            if (headSize <= 0) {
                return;
            }
            Vec3 delta = endPos.subtract(startPos);
            Vec3 f = delta.cross(new Vec3(0, 1, 0)).normalize().scale(headSize);
            if (delta.normalize().dot(new Vec3(0, 1, 0)) == 1) {
                f = new Vec3(1, 0, 0).scale(headSize);
            }
            Vec3 preHead = delta.scale(headSize / delta.length() * -1);
            Vec3 head1 = preHead.add(f);
            Vec3 head2 = preHead.add(f.scale(-1));
            scene.overlay().showLine(headColor, endPos.add(head1), endPos, duration);
            scene.overlay().showLine(headColor, endPos, endPos.add(head2), duration);
        }
    }

    public static class PatternDisplay {
        private final SceneBuilder scene;
        private final List<ArrowDisplay> strokes = new ArrayList<>();
        private final Vec3 o;
        private final Vec3 x;
        private final Vec3 y;
        private final Vec3 z;
        private static final int M = 6;

        public PatternDisplay(SceneBuilder scene, SceneBuildingUtil util, Vec3 origin, Vec3 xDelta, Vec3 yDelta, HexPattern pattern) {
            this.o = origin;
            this.x = xDelta;
            this.y = yDelta;
            this.z = xDelta.cross(yDelta);
            this.scene = scene;

            Vec3 nowPos = o;
            int nowIndex = dirIndex(pattern.getStartDir());
            strokes.add(new ArrowDisplay(scene)
                .start(nowPos)
                .end(nowPos.add(makeVec3(hexOffset(nowIndex))).add(z.scale(0.01)))
                .size(x.length() / 16.0));
            nowPos = nowPos.add(makeVec3(hexOffset(nowIndex)));
            for (HexAngle angle : pattern.getAngles()) {
                nowIndex = (nowIndex + angleIndex(angle)) % M;
                strokes.add(new ArrowDisplay(scene)
                    .start(nowPos)
                    .end(nowPos.add(makeVec3(hexOffset(nowIndex))).add(z.scale(0.01)))
                    .size(x.length() / 16.0));
                nowPos = nowPos.add(makeVec3(hexOffset(nowIndex)));
            }
        }

        private Vec3 makeVec3(double px, double py) {
            return x.scale(px).add(y.scale(py));
        }

        private Vec3 makeVec3(double[] pair) {
            return makeVec3(pair[0], pair[1]);
        }

        public void show(int duration, int per, List<PonderPalette> colors) {
            if (strokes.isEmpty()) {
                return;
            }
            if (colors.isEmpty()) {
                show(duration, per, List.of(PonderPalette.FAST));
                return;
            }
            if (per < 0 || per * strokes.size() >= duration) {
                show(duration, duration / strokes.size(), colors);
                return;
            }
            for (int i = 0; i < strokes.size(); ++i) {
                PonderPalette nowColor = i < colors.size() ? colors.get(i) : colors.get(colors.size() - 1);
                strokes.get(i).color(nowColor, nowColor).show(duration - i * per);
                scene.idle(per);
            }
        }

        public void show(int duration, int per, PonderPalette color) {
            show(duration, per, List.of(color));
        }

        public void show(int duration, List<PonderPalette> colors) {
            show(duration, 0, colors);
        }

        public void show(int duration, PonderPalette color) {
            show(duration, 0, List.of(color));
        }

        public void show(int duration, int per) {
            show(duration, per, List.of(PonderPalette.GREEN, PonderPalette.FAST));
        }

        public void show(int duration) {
            show(duration, 0);
        }
    }
}
