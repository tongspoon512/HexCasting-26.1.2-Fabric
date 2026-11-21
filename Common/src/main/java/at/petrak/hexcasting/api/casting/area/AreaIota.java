package at.petrak.hexcasting.api.casting.area;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An iota that represents a single cast domain.
 */
public class AreaIota extends Iota {
    public static final int COLOR = 0xFF_FF8080;

    public record Payload(AreaShape shape, UUID domainId) {
    }

    public AreaIota(AreaShape shape, UUID domainId) {
        super(HexIotaTypes.AREA, new Payload(shape, domainId));
    }

    public AreaShape getShape() {
        return ((Payload) this.payload).shape();
    }

    public UUID getDomainId() {
        return ((Payload) this.payload).domainId();
    }

    @Override
    public boolean toleratesOther(Iota that) {
        return typesMatch(this, that)
            && that instanceof AreaIota other
            && this.getShape().equals(other.getShape())
            && this.getDomainId().equals(other.getDomainId());
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    @Override
    public int size() {
        return this.getShape().getPoints().size() + 1;
    }

    @Override
    public @NotNull Tag serialize() {
        CompoundTag tag = this.getShape().serialize();
        tag.store("id", UUIDUtil.CODEC, this.getDomainId());
        return tag;
    }

    @Override
    public Component display() {
        return Component.literal(this.getShape().display())
            .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR)));
    }

    public static final IotaType<AreaIota> TYPE = new IotaType<>() {
        @Override
        public @Nullable AreaIota deserialize(Tag tag, ServerLevel world) throws IllegalArgumentException {
            CompoundTag ctag = HexUtils.downcast(tag, CompoundTag.TYPE);
            AreaShape shape = AreaShape.deserialize(ctag);
            UUID id = ctag.read("id", UUIDUtil.CODEC).orElse(null);
            if (id == null) {
                return null;
            }
            return new AreaIota(shape, id);
        }

        @Override
        public Component display(Tag tag) {
            if (tag instanceof CompoundTag ctag) {
                try {
                    return Component.literal(AreaShape.deserialize(ctag).display())
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(COLOR)));
                } catch (IllegalArgumentException ignored) {
                }
            }
            return Component.translatable("hexcasting.spelldata.unknown");
        }

        @Override
        public int color() {
            return COLOR;
        }
    };
}
