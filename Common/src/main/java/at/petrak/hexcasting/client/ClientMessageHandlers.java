package at.petrak.hexcasting.client;

import at.petrak.hexcasting.client.gui.GuiSpellcasting;
import at.petrak.hexcasting.common.entities.EntityWallScroll;
import at.petrak.hexcasting.common.entities.HexEntities;
import at.petrak.hexcasting.common.lib.HexSounds;
import at.petrak.hexcasting.common.msgs.MsgBeepS2C;
import at.petrak.hexcasting.common.msgs.MsgCastParticleS2C;
import at.petrak.hexcasting.common.msgs.MsgClearSpiralPatternsS2C;
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternS2C;
import at.petrak.hexcasting.common.msgs.MsgNewSpiralPatternsS2C;
import at.petrak.hexcasting.common.msgs.MsgNewWallScrollS2C;
import at.petrak.hexcasting.common.msgs.MsgOpenSpellGuiS2C;
import at.petrak.hexcasting.common.msgs.MsgPullClipboardS2C;
import at.petrak.hexcasting.common.msgs.MsgPushClipboardC2S;
import at.petrak.hexcasting.common.msgs.MsgRecalcWallScrollDisplayS2C;
import at.petrak.hexcasting.common.particles.ConjureParticleOptions;
import at.petrak.hexcasting.xplat.IClientXplatAbstractions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Pattern;

/**
 * Client-only handlers for S2C packets.
 * <p>
 * The message classes themselves live in {@code common} and are initialized on both sides when
 * packets are registered, so their {@code handle} methods must not reference client classes.
 */
public final class ClientMessageHandlers {
    private ClientMessageHandlers() {
    }

    private static final Random RANDOM = new Random();

    /** Mirrors {@link MsgPullClipboardS2C#MODE_ANGLES}. */
    private static final int PULL_MODE_ANGLES = 1;
    private static final Pattern ANGLES = Pattern.compile("(?<=\")[wedsaq]*(?=\")");

    public static void openSpellGui(MsgOpenSpellGuiS2C msg) {
        Minecraft.getInstance().execute(() -> {
            var mc = Minecraft.getInstance();
            mc.setScreen(new GuiSpellcasting(msg.hand(), msg.patterns(), msg.stack(), msg.ravenmind(),
                msg.parenCount()));
        });
    }

    public static void newSpellPattern(MsgNewSpellPatternS2C self) {
        Minecraft.getInstance().execute(() -> {
            var mc = Minecraft.getInstance();
            if (self.info().isStackClear()) {
                // don't pay attention to the screen, so it also stops when we die
                mc.getSoundManager().stop(HexSounds.CASTING_AMBIANCE.location(), null);
            }
            var screen = mc.screen;
            if (screen instanceof GuiSpellcasting spellGui) {
                spellGui.recvServerUpdate(self.info(), self.index());
            }
        });
    }

    public static void newSpiralPatterns(MsgNewSpiralPatternsS2C self) {
        Minecraft.getInstance().execute(() -> {
            var mc = Minecraft.getInstance();
            assert mc.level != null;
            var player = mc.level.getPlayerByUUID(self.playerUUID());
            var stack = IClientXplatAbstractions.INSTANCE.getClientCastingStack(player);

            for (var pattern : self.patterns()) {
                stack.addPattern(pattern, self.lifetime());
            }
        });
    }

    public static void clearSpiralPatterns(MsgClearSpiralPatternsS2C self) {
        Minecraft.getInstance().execute(() -> {
            var mc = Minecraft.getInstance();
            assert mc.level != null;
            var player = mc.level.getPlayerByUUID(self.playerUUID());
            var stack = IClientXplatAbstractions.INSTANCE.getClientCastingStack(player);
            stack.slowClear();
        });
    }

    public static void newWallScroll(MsgNewWallScrollS2C self) {
        Minecraft.getInstance().execute(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                var inner = new ClientboundAddEntityPacket(self.entityId(), self.uuid(),
                    self.pos().getX() + 0.5, self.pos().getY() + 0.5, self.pos().getZ() + 0.5,
                    0f, 0f, HexEntities.WALL_SCROLL, 0, Vec3.ZERO, 0);
                player.connection.handleAddEntity(inner);
                var e = player.level().getEntity(self.entityId());
                if (e instanceof EntityWallScroll scroll) {
                    scroll.readSpawnData(self.pos(), self.dir(), self.scrollItem(), self.showsStrokeOrder(),
                        self.blockSize());
                }
            }
        });
    }

    public static void recalcWallScrollDisplay(MsgRecalcWallScrollDisplayS2C msg) {
        Minecraft.getInstance().execute(() -> {
            var mc = Minecraft.getInstance();
            var entity = mc.level.getEntity(msg.entityId());
            if (entity instanceof EntityWallScroll scroll
                && scroll.getShowsStrokeOrder() != msg.showStrokeOrder()) {
                scroll.recalculateDisplay();
            }
        });
    }

    public static void beep(MsgBeepS2C msg) {
        Minecraft.getInstance().execute(() -> {
            var minecraft = Minecraft.getInstance();
            var world = minecraft.level;
            if (world != null) {
                float pitch = (float) Math.pow(2, (msg.note() - 12) / 12.0);
                world.playLocalSound(msg.target().x, msg.target().y, msg.target().z,
                    msg.instrument().getSoundEvent().value(), SoundSource.PLAYERS, 3, pitch, false);
                world.addParticle(ParticleTypes.NOTE, msg.target().x, msg.target().y + 0.2, msg.target().z,
                    msg.note() / 24.0, 0, 0);
            }
        });
    }

    public static void castParticle(MsgCastParticleS2C msg) {
        Minecraft.getInstance().execute(() -> {
            var colProvider = msg.colorizer().getColorProvider();
            for (int i = 0; i < msg.spray().getCount(); i++) {
                // For the colors, pick any random time to get a mix of colors
                var offset = randomInCircle(Mth.TWO_PI).normalize()
                    .scale(RANDOM.nextFloat() * msg.spray().getFuzziness() / 2);
                var pos = msg.spray().getPos().add(offset);

                var phi = Math.acos(1.0 - RANDOM.nextDouble() * (1.0 - Math.cos(msg.spray().getSpread())));
                var theta = Math.PI * 2.0 * RANDOM.nextDouble();
                var v = msg.spray().getVel().normalize();
                // pick any old vector to get a vector normal to v with
                Vec3 k;
                if (v.x == 0.0 && v.y == 0.0) {
                    // oops, pick a *different* normal
                    k = new Vec3(1.0, 0.0, 0.0);
                } else {
                    k = v.cross(new Vec3(0.0, 0.0, 1.0));
                }
                var velUnlen = v.scale(Math.cos(phi))
                    .add(k.scale(Math.sin(phi) * Math.cos(theta)))
                    .add(v.cross(k).scale(Math.sin(phi) * Math.sin(theta)));
                var vel = velUnlen.scale(msg.spray().getVel().length() / 20);

                var color = colProvider.getColor(ClientTickCounter.getTotal(), velUnlen);

                Minecraft.getInstance().level.addParticle(
                    new ConjureParticleOptions(color),
                    pos.x, pos.y, pos.z,
                    vel.x, vel.y, vel.z
                );
            }
        });
    }

    public static void pullClipboard(MsgPullClipboardS2C msg) {
        var mc = Minecraft.getInstance();
        mc.execute(() -> {
            var code = mc.keyboardHandler.getClipboard();
            if (code == null || code.isBlank()) {
                return;
            }

            if (msg.mode() == PULL_MODE_ANGLES) {
                var parts = new ArrayList<String>();
                var matcher = ANGLES.matcher(code);
                while (matcher.find()) {
                    parts.add("_" + matcher.group());
                }
                if (parts.isEmpty()) {
                    return;
                }
                code = String.join(" ", parts);
            }

            var length = code.getBytes(StandardCharsets.UTF_8).length;
            if (length > MsgPullClipboardS2C.MAX_CODE_LENGTH) {
                var player = mc.player;
                if (player != null) {
                    player.sendSystemMessage(Component.literal(
                        "Clipboard code is too long to send (" + length + " bytes, max "
                            + MsgPullClipboardS2C.MAX_CODE_LENGTH + ")"));
                }
                return;
            }

            IClientXplatAbstractions.INSTANCE.sendPacketToServer(
                new MsgPushClipboardC2S(code, msg.rename(), msg.mode()));
        });
    }

    // https://math.stackexchange.com/questions/44689/how-to-find-a-random-axis-or-unit-vector-in-3d
    private static Vec3 randomInCircle(double maxTh) {
        var th = RANDOM.nextDouble(0.0, maxTh + 0.001);
        var z = RANDOM.nextDouble(-1.0, 1.0);
        return new Vec3(Math.sqrt(1.0 - z * z) * Math.cos(th), Math.sqrt(1.0 - z * z) * Math.sin(th), z);
    }
}
