package at.petrak.hexcasting.fabric.network;

import at.petrak.hexcasting.common.msgs.*;
import gay.object.hexdebug.networking.MsgSplicingTableActionC2S;
import gay.object.hexdebug.networking.MsgSplicingTableDrawPatternC2S;
import gay.object.hexdebug.networking.MsgSplicingTableGetDataC2S;
import gay.object.hexdebug.networking.MsgSplicingTableNewDataS2C;
import gay.object.hexdebug.networking.MsgSplicingTableSelectC2S;
import gay.object.hexdebug.networking.MsgSplicingTableViewC2S;
import miyucomics.hexcassettes.network.MsgCassetteRemoveC2S;
import miyucomics.hexcassettes.network.MsgCassetteSyncRequestC2S;
import miyucomics.hexcassettes.network.MsgCassetteSyncS2C;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 26.1 uses the payload system instead of the old channel/id + raw-buffer API.
 * Every {@link IMessage} is wrapped in a {@link HexPayload} whose type id is the message id.
 */
public class FabricPacketHandler {
    private static final Map<Identifier, CustomPacketPayload.Type<HexPayload<?>>> TYPES = new HashMap<>();

    public record HexPayload<T extends IMessage>(CustomPacketPayload.Type<HexPayload<T>> type, T msg)
        implements CustomPacketPayload {
    }

    public static void init() {
        // Payload types have to be known on both physical sides so packets can be encoded/decoded.
        registerType(MsgNewSpellPatternC2S.ID, MsgNewSpellPatternC2S::deserialize);
        registerType(MsgShiftScrollC2S.ID, MsgShiftScrollC2S::deserialize);

        var splicingGetData = registerType(MsgSplicingTableGetDataC2S.ID, MsgSplicingTableGetDataC2S::deserialize);
        var splicingSelect = registerType(MsgSplicingTableSelectC2S.ID, MsgSplicingTableSelectC2S::deserialize);
        var splicingView = registerType(MsgSplicingTableViewC2S.ID, MsgSplicingTableViewC2S::deserialize);
        var splicingAction = registerType(MsgSplicingTableActionC2S.ID, MsgSplicingTableActionC2S::deserialize);
        var splicingDrawPattern = registerType(MsgSplicingTableDrawPatternC2S.ID, MsgSplicingTableDrawPatternC2S::deserialize);
        var splicingNewData = registerType(MsgSplicingTableNewDataS2C.ID, MsgSplicingTableNewDataS2C::deserialize);

        var newPattern = registerType(MsgNewSpellPatternS2C.ID, MsgNewSpellPatternS2C::deserialize);
        var castParticle = registerType(MsgCastParticleS2C.ID, MsgCastParticleS2C::deserialize);
        var openGui = registerType(MsgOpenSpellGuiS2C.ID, MsgOpenSpellGuiS2C::deserialize);
        var beep = registerType(MsgBeepS2C.ID, MsgBeepS2C::deserialize);
        var wallScroll = registerType(MsgNewWallScrollS2C.ID, MsgNewWallScrollS2C::deserialize);
        var recalcScroll = registerType(MsgRecalcWallScrollDisplayS2C.ID,
            MsgRecalcWallScrollDisplayS2C::deserialize);
        var newPatterns = registerType(MsgNewSpiralPatternsS2C.ID, MsgNewSpiralPatternsS2C::deserialize);
        var clearPatterns = registerType(MsgClearSpiralPatternsS2C.ID, MsgClearSpiralPatternsS2C::deserialize);

        var pullClipboard = registerType(MsgPullClipboardS2C.ID, MsgPullClipboardS2C::deserialize);
        var pushClipboard = registerType(MsgPushClipboardC2S.ID, MsgPushClipboardC2S::deserialize);

        var cassetteSync = registerType(MsgCassetteSyncS2C.ID, MsgCassetteSyncS2C::deserialize);
        registerType(MsgCassetteRemoveC2S.ID, MsgCassetteRemoveC2S::deserialize);
        registerType(MsgCassetteSyncRequestC2S.ID, MsgCassetteSyncRequestC2S::deserialize);

        registerServerReceiver(MsgNewSpellPatternC2S.ID,
            (msg, server, player) -> ((MsgNewSpellPatternC2S) msg).handle(server, player));
        registerServerReceiver(MsgShiftScrollC2S.ID,
            (msg, server, player) -> ((MsgShiftScrollC2S) msg).handle(server, player));
        registerServerReceiver(MsgSplicingTableGetDataC2S.ID,
            (msg, server, player) -> MsgSplicingTableGetDataC2S.handle(player));
        registerServerReceiver(MsgSplicingTableSelectC2S.ID,
            (msg, server, player) -> MsgSplicingTableSelectC2S.handle(player, (MsgSplicingTableSelectC2S) msg));
        registerServerReceiver(MsgSplicingTableViewC2S.ID,
            (msg, server, player) -> MsgSplicingTableViewC2S.handle(player, (MsgSplicingTableViewC2S) msg));
        registerServerReceiver(MsgSplicingTableActionC2S.ID,
            (msg, server, player) -> MsgSplicingTableActionC2S.handle(player, (MsgSplicingTableActionC2S) msg));
        registerServerReceiver(MsgSplicingTableDrawPatternC2S.ID,
            (msg, server, player) -> MsgSplicingTableDrawPatternC2S.handle(player, (MsgSplicingTableDrawPatternC2S) msg));
        registerServerReceiver(MsgPushClipboardC2S.ID,
            (msg, server, player) -> MsgPushClipboardC2S.handle(player, (MsgPushClipboardC2S) msg));
        registerServerReceiver(MsgCassetteRemoveC2S.ID,
            (msg, server, player) -> MsgCassetteRemoveC2S.handle(player, (MsgCassetteRemoveC2S) msg));
        registerServerReceiver(MsgCassetteSyncRequestC2S.ID,
            (msg, server, player) -> MsgCassetteSyncRequestC2S.handle(player));

        RECEIVERS.put(newPattern.id(), (msg, ctx) -> MsgNewSpellPatternS2C.handle((MsgNewSpellPatternS2C) msg));
        RECEIVERS.put(castParticle.id(), (msg, ctx) -> MsgCastParticleS2C.handle((MsgCastParticleS2C) msg));
        RECEIVERS.put(openGui.id(), (msg, ctx) -> MsgOpenSpellGuiS2C.handle((MsgOpenSpellGuiS2C) msg));
        RECEIVERS.put(beep.id(), (msg, ctx) -> MsgBeepS2C.handle((MsgBeepS2C) msg));
        RECEIVERS.put(wallScroll.id(), (msg, ctx) -> MsgNewWallScrollS2C.handle((MsgNewWallScrollS2C) msg));
        RECEIVERS.put(recalcScroll.id(),
            (msg, ctx) -> MsgRecalcWallScrollDisplayS2C.handle((MsgRecalcWallScrollDisplayS2C) msg));
        RECEIVERS.put(newPatterns.id(),
            (msg, ctx) -> MsgNewSpiralPatternsS2C.handle((MsgNewSpiralPatternsS2C) msg));
        RECEIVERS.put(clearPatterns.id(),
            (msg, ctx) -> MsgClearSpiralPatternsS2C.handle((MsgClearSpiralPatternsS2C) msg));
        RECEIVERS.put(splicingNewData.id(),
            (msg, ctx) -> MsgSplicingTableNewDataS2C.handle((MsgSplicingTableNewDataS2C) msg));
        RECEIVERS.put(pullClipboard.id(),
            (msg, ctx) -> MsgPullClipboardS2C.handle((MsgPullClipboardS2C) msg));
        RECEIVERS.put(cassetteSync.id(),
            (msg, ctx) -> MsgCassetteSyncS2C.handle((MsgCassetteSyncS2C) msg));
    }

    private static final Map<Identifier, ClientReceiver> RECEIVERS = new HashMap<>();

    private interface ClientReceiver {
        void receive(IMessage msg, ClientPlayNetworking.Context ctx);
    }

    public static void initClient() {
        for (var entry : RECEIVERS.entrySet()) {
            var type = TYPES.get(entry.getKey());
            if (type == null) {
                continue;
            }
            registerClientReceiver(type, entry.getValue());
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerClientReceiver(CustomPacketPayload.Type<HexPayload<?>> type,
        ClientReceiver receiver) {
        ClientPlayNetworking.registerGlobalReceiver((CustomPacketPayload.Type) type,
            (payload, ctx) -> receiver.receive(((HexPayload<?>) payload).msg(), ctx));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void registerServerReceiver(Identifier id,
        ServerReceiver receiver) {
        var type = TYPES.get(id);
        if (type == null) {
            return;
        }
        ServerPlayNetworking.registerGlobalReceiver((CustomPacketPayload.Type) type,
            (payload, ctx) -> receiver.receive(((HexPayload<?>) payload).msg(), ctx.server(), ctx.player()));
    }

    private interface ServerReceiver {
        void receive(IMessage msg, MinecraftServer server, ServerPlayer player);
    }

    private static <T extends IMessage> CustomPacketPayload.Type<HexPayload<T>> registerType(Identifier id,
        Function<FriendlyByteBuf, T> decoder) {
        var type = new CustomPacketPayload.Type<HexPayload<T>>(id);
        var codec = codec(type, decoder);
        PayloadTypeRegistry.serverboundPlay().register(type, codec);
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
        TYPES.put(id, (CustomPacketPayload.Type) type);
        return type;
    }

    private static <T extends IMessage> StreamCodec<FriendlyByteBuf, HexPayload<T>> codec(
        CustomPacketPayload.Type<HexPayload<T>> type, Function<FriendlyByteBuf, T> decoder) {
        return new StreamCodec<>() {
            @Override
            public HexPayload<T> decode(FriendlyByteBuf buf) {
                return new HexPayload<>(type, decoder.apply(buf));
            }

            @Override
            public void encode(FriendlyByteBuf buf, HexPayload<T> value) {
                value.msg().serialize(buf);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static HexPayload<?> wrap(IMessage message) {
        var type = TYPES.get(message.getFabricId());
        if (type == null) {
            throw new IllegalArgumentException("Unregistered message id " + message.getFabricId());
        }
        return new HexPayload(type, message);
    }

    public static void sendToServer(IMessage message) {
        ClientPlayNetworking.send(wrap(message));
    }

    public static void sendToPlayer(ServerPlayer player, IMessage message) {
        ServerPlayNetworking.send(player, wrap(message));
    }

    public static void sendToPlayers(Collection<ServerPlayer> players, IMessage message) {
        var packet = toClientboundPacket(message);
        for (var player : players) {
            player.connection.send(packet);
        }
    }

    @SuppressWarnings("unchecked")
    public static Packet<ClientGamePacketListener> toClientboundPacket(IMessage message) {
        // The custom payload packet is a ClientCommonPacketListener packet, which is what the entity
        // spawn path actually accepts at runtime; the generic only needs to line up.
        return (Packet<ClientGamePacketListener>) (Packet<?>) new ClientboundCustomPayloadPacket(wrap(message));
    }
}
