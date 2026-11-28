package pub.pigeon.yggdyy.hexcreating.api.iotanet;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.utils.NBTHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class IotaPack {
    public final long id;
    public final Iota iota;
    public final Set<String> headInfo;

    public IotaPack(long id, Iota iota, Set<String> headInfo) {
        this.id = id;
        this.iota = iota;
        this.headInfo = headInfo;
    }

    public static IotaPack createNew(Iota iota) {
        return new IotaPack(ThreadLocalRandom.current().nextLong(), iota, new LinkedHashSet<>());
    }

    public static IotaPack createNew(Iota iota, Set<String> headInfo) {
        return new IotaPack(ThreadLocalRandom.current().nextLong(), iota, new LinkedHashSet<>(headInfo));
    }

    public static CompoundTag toNbt(IotaPack pack) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("id", pack.id);
        tag.put("iota", IotaType.serialize(pack.iota));
        tag.putInt("head_info_length", pack.headInfo.size());
        int i = 0;
        for (String s : pack.headInfo) {
            tag.putString("head_info_" + i++, s);
        }
        return tag;
    }

    public static IotaPack fromNbt(CompoundTag tag, ServerLevel level) {
        if (!tag.contains("id") || !tag.contains("iota") || !tag.contains("head_info_length")) {
            return null;
        }
        var iotaTag = tag.getCompound("iota").orElse(null);
        if (iotaTag == null || iotaTag.isEmpty()) {
            return null;
        }
        Iota iota = IotaType.deserialize(iotaTag, level);
        if (iota == null) {
            return null;
        }
        int length = tag.getIntOr("head_info_length", 0);
        Set<String> headInfo = new LinkedHashSet<>();
        for (int i = 0; i < length; i++) {
            headInfo.add(tag.getStringOr("head_info_" + i, ""));
        }
        return new IotaPack(tag.getLongOr("id", 0L), iota, headInfo);
    }

    public boolean addHeadInfo(String value) {
        return this.headInfo.add(value);
    }

    public int addHeadInfo(Iterable<String> values) {
        int added = 0;
        for (String value : values) {
            if (this.headInfo.add(value)) {
                added++;
            }
        }
        return added;
    }

    public boolean hasHeadInfo(String value) {
        return this.headInfo.contains(value);
    }

    public IotaPack copyWithSameId() {
        return new IotaPack(this.id, this.iota, new LinkedHashSet<>(this.headInfo));
    }

    public IotaPack copyWithDifferentId() {
        return createNew(this.iota, this.headInfo);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof IotaPack pack && pack.id == this.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.id);
    }
}
