package at.petrak.hexcasting.common.msgs;

import at.petrak.hexcasting.api.casting.ParticleSpray;
import at.petrak.hexcasting.api.pigment.FrozenPigment;
import at.petrak.hexcasting.client.ClientMessageHandlers;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

/**
 * Sent server->client to spray particles everywhere.
 */
public record MsgCastParticleS2C(ParticleSpray spray, FrozenPigment colorizer) implements IMessage {
    public static final Identifier ID = modLoc("cprtcl");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    public static MsgCastParticleS2C deserialize(ByteBuf buffer) {
        var buf = new FriendlyByteBuf(buffer);
        var posX = buf.readDouble();
        var posY = buf.readDouble();
        var posZ = buf.readDouble();
        var velX = buf.readDouble();
        var velY = buf.readDouble();
        var velZ = buf.readDouble();
        var fuzziness = buf.readDouble();
        var spread = buf.readDouble();
        var count = buf.readInt();
        var tag = buf.readNbt();
        var colorizer = FrozenPigment.fromNBT(tag);
        return new MsgCastParticleS2C(
            new ParticleSpray(new Vec3(posX, posY, posZ), new Vec3(velX, velY, velZ), fuzziness, spread, count),
            colorizer);
    }

    @Override
    public void serialize(FriendlyByteBuf buf) {
        buf.writeDouble(this.spray.getPos().x);
        buf.writeDouble(this.spray.getPos().y);
        buf.writeDouble(this.spray.getPos().z);
        buf.writeDouble(this.spray.getVel().x);
        buf.writeDouble(this.spray.getVel().y);
        buf.writeDouble(this.spray.getVel().z);
        buf.writeDouble(this.spray.getFuzziness());
        buf.writeDouble(this.spray.getSpread());
        buf.writeInt(this.spray.getCount());
        buf.writeNbt(this.colorizer.serializeToNBT());
    }


    public static void handle(MsgCastParticleS2C msg) {
        ClientMessageHandlers.castParticle(msg);
    }
}
