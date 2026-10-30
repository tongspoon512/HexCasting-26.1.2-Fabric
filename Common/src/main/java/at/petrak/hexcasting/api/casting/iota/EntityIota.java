package at.petrak.hexcasting.api.casting.iota;

import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import com.google.gson.JsonParser;
import com.samsthenerd.inline.api.InlineAPI;
import com.samsthenerd.inline.api.data.EntityInlineData;
import com.samsthenerd.inline.api.data.PlayerHeadData;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class EntityIota extends Iota {
    public EntityIota(@NotNull Entity e) {
        super(HexIotaTypes.ENTITY, e);
    }

    public Entity getEntity() {
        return (Entity) this.payload;
    }

    @Override
    public boolean toleratesOther(Iota that) {
        return typesMatch(this, that)
            && that instanceof EntityIota dent
            && this.getEntity() == dent.getEntity();
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    @Override
    public @NotNull
    Tag serialize() {
        var out = new CompoundTag();
        out.store("uuid", UUIDUtil.CODEC, this.getEntity().getUUID());
        out.putString("name", ComponentSerialization.CODEC.encodeStart(JsonOps.INSTANCE, getEntityNameWithInline(true))
            .result().map(Object::toString).orElse(""));
        return out;
    }

    @Override
    public Component display() {
        return getEntityNameWithInline(false).copy().withStyle(ChatFormatting.AQUA);
    }

    private Component getEntityNameWithInline(boolean fearSerializer){
        MutableComponent baseName = this.getEntity().getName().copy();
        Component inlineEnt = null;
        if(this.getEntity() instanceof Player){
            inlineEnt = new PlayerHeadData().asText(!fearSerializer);
            inlineEnt = inlineEnt.plainCopy().withStyle(InlineAPI.INSTANCE.withSizeModifier(inlineEnt.getStyle(), 1.5f));
        } else{
            inlineEnt = new EntityInlineData().asText(!fearSerializer);
        }
        return baseName.append(Component.literal(": ")).append(inlineEnt);
    }

    public static IotaType<EntityIota> TYPE = new IotaType<>() {
        @Nullable
        @Override
        public EntityIota deserialize(Tag tag, ServerLevel world) throws IllegalArgumentException {
            var ctag = HexUtils.downcast(tag, CompoundTag.TYPE);
            Tag uuidTag = ctag.get("uuid");
            if (uuidTag == null) {
                return null;
            }
            var uuid = UUIDUtil.CODEC.parse(NbtOps.INSTANCE, uuidTag).result().orElse(null);
            if (uuid == null) {
                return null;
            }
            var entity = world.getEntity(uuid);
            if (entity == null) {
                return null;
            }
            return new EntityIota(entity);
        }

        @Override
        public Component display(Tag tag) {
            if (!(tag instanceof CompoundTag ctag)) {
                return Component.translatable("hexcasting.spelldata.entity.whoknows");
            }
            var nameJson = ctag.getString("name").orElse(null);
            if (nameJson == null) {
                return Component.translatable("hexcasting.spelldata.entity.whoknows");
            }
//            return Component.literal(nameJson);
            return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(nameJson))
                .result().orElse(Component.empty()).copy().withStyle(ChatFormatting.AQUA);
        }

        @Override
        public int color() {
            return 0xff_55ffff;
        }
    };
}
