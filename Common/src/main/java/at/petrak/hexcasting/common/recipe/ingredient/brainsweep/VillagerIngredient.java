package at.petrak.hexcasting.common.recipe.ingredient.brainsweep;

import com.google.gson.JsonObject;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Special case for villagers so we can have biome/profession/level reqs
 */
public class VillagerIngredient extends BrainsweepeeIngredient {
    public final @Nullable Holder<VillagerProfession> profession;
    public final @Nullable Holder<VillagerType> biome;
    public final int minLevel;

    public VillagerIngredient(
        @Nullable Holder<VillagerProfession> profession,
        @Nullable Holder<VillagerType> biome,
        int minLevel
    ) {
        this.profession = profession;
        this.biome = biome;
        this.minLevel = minLevel;
    }

    @Override
    public boolean test(Entity entity, ServerLevel level) {
        if (!(entity instanceof Villager villager)) return false;

        var data = villager.getVillagerData();

        return (this.profession == null || data.profession().is(this.profession))
            && (this.biome == null || data.type().is(this.biome))
            && this.minLevel <= data.level();
    }

    @Override
    public List<Entity> exampleEntities(Level level) {
        var biome = Objects.requireNonNullElse(this.biome,
            BuiltInRegistries.VILLAGER_TYPE.get(VillagerType.PLAINS).orElseThrow());
        var profession = Objects.requireNonNullElse(this.profession,
            BuiltInRegistries.VILLAGER_PROFESSION.get(VillagerProfession.TOOLSMITH).orElseThrow());
        var tradeLevel = Math.min(this.minLevel, 1);

        var out = new Villager(EntityType.VILLAGER, level);

        var data = out.getVillagerData()
            .withProfession(profession)
            .withType(biome)
            .withLevel(tradeLevel);
        out.setVillagerData(data);

        return List.of(out);
    }

    @Override
    public List<Component> getTooltip(boolean advanced) {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(this.getName());

        if (advanced) {
            if (minLevel >= 5) {
                tooltip.add(Component.translatable("hexcasting.tooltip.brainsweep.level", 5)
                    .withStyle(ChatFormatting.DARK_GRAY));
            } else if (minLevel > 1) {
                tooltip.add(Component.translatable("hexcasting.tooltip.brainsweep.min_level", minLevel)
                    .withStyle(ChatFormatting.DARK_GRAY));
            }

            if (this.biome != null) {
                tooltip.add(Component.literal(this.biome.toString()).withStyle(ChatFormatting.DARK_GRAY));
            }

            if (this.profession != null) {
                tooltip.add(Component.literal(this.profession.toString()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        tooltip.add(BrainsweepeeIngredient.getModNameComponent(
            this.profession == null
                ? "minecraft"
                : this.profession.unwrapKey().orElseThrow().identifier().getNamespace()));

        return tooltip;
    }

    @Override
    public Component getName() {
        MutableComponent component = Component.literal("");

        boolean addedAny = false;

        if (minLevel >= 5) {
            component.append(Component.translatable("merchant.level.5"));
            addedAny = true;
        } else if (minLevel > 1) {
            component.append(Component.translatable("merchant.level." + minLevel));
            addedAny = true;
        } else if (profession != null) {
            component.append(Component.translatable("merchant.level.1"));
            addedAny = true;
        }

        if (biome != null) {
            if (addedAny) {
                component.append(" ");
            }
            var biomeLoc = this.biome.unwrapKey().orElseThrow().identifier();
            component.append(Component.translatable("biome." + biomeLoc.getNamespace() + "." + biomeLoc.getPath()));
            addedAny = true;
        }

        if (profession != null) {
            // We've for sure added something
            component.append(" ");
            var professionLoc = this.profession.unwrapKey().orElseThrow().identifier();
            // TODO: what's the convention used for modded villager types?
            // Villager::getTypeName implies that it there's no namespace information.
            // i hope there is some convention
            component.append(Component.translatable("entity.minecraft.villager." + professionLoc.getPath()));
        } else {
            if (addedAny) {
                component.append(" ");
            }
            component.append(EntityType.VILLAGER.getDescription());
        }

        return component;
    }

    @Override
    public JsonObject serialize() {
        var obj = new JsonObject();
        obj.addProperty("type", Type.VILLAGER.getSerializedName());

        if (this.profession != null) {
            obj.addProperty("profession", this.profession.unwrapKey().orElseThrow().identifier().toString());
        }
        if (this.biome != null) {
            obj.addProperty("biome", this.biome.unwrapKey().orElseThrow().identifier().toString());
        }
        obj.addProperty("minLevel", this.minLevel);
        return obj;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        if (this.profession != null) {
            buf.writeVarInt(1);
            buf.writeVarInt(BuiltInRegistries.VILLAGER_PROFESSION.getId(this.profession.value()));
        } else {
            buf.writeVarInt(0);
        }
        if (this.biome != null) {
            buf.writeVarInt(1);
            buf.writeVarInt(BuiltInRegistries.VILLAGER_TYPE.getId(this.biome.value()));
        } else {
            buf.writeVarInt(0);
        }
        buf.writeInt(this.minLevel);
    }

    public static VillagerIngredient deserialize(JsonObject json) {
        Holder<VillagerProfession> profession = null;
        if (json.has("profession") && !json.get("profession").isJsonNull()) {
            profession = BuiltInRegistries.VILLAGER_PROFESSION.get(Identifier.parse(GsonHelper.getAsString(json,
                "profession"))).orElse(null);
        }
        Holder<VillagerType> biome = null;
        if (json.has("biome") && !json.get("biome").isJsonNull()) {
            biome = BuiltInRegistries.VILLAGER_TYPE.get(Identifier.parse(GsonHelper.getAsString(json, "biome")))
                .orElse(null);
        }
        int minLevel = GsonHelper.getAsInt(json, "minLevel");
        return new VillagerIngredient(profession, biome, minLevel);
    }

    public static VillagerIngredient read(FriendlyByteBuf buf) {
        Holder<VillagerProfession> profession = null;
        var hasProfession = buf.readVarInt();
        if (hasProfession != 0) {
            var raw = BuiltInRegistries.VILLAGER_PROFESSION.byId(buf.readVarInt());
            profession = raw == null ? null : BuiltInRegistries.VILLAGER_PROFESSION.wrapAsHolder(raw);
        }
        Holder<VillagerType> biome = null;
        var hasBiome = buf.readVarInt();
        if (hasBiome != 0) {
            var raw = BuiltInRegistries.VILLAGER_TYPE.byId(buf.readVarInt());
            biome = raw == null ? null : BuiltInRegistries.VILLAGER_TYPE.wrapAsHolder(raw);
        }
        int minLevel = buf.readInt();
        return new VillagerIngredient(profession, biome, minLevel);
    }

    @Override
    public Type ingrType() {
        return Type.VILLAGER;
    }

    @Override
    public String getSomeKindOfReasonableIDForEmi() {
        var bob = new StringBuilder();
        if (this.profession != null) {
            var profLoc = this.profession.unwrapKey().orElseThrow().identifier();
            bob.append(profLoc.getNamespace())
                .append("//")
                .append(profLoc.getPath());
        } else {
            bob.append("null");
        }
        bob.append("_");
        if (this.biome != null) {
            var biomeLoc = this.biome.unwrapKey().orElseThrow().identifier();
            bob.append(biomeLoc.getNamespace())
                .append("//")
                .append(biomeLoc.getPath());
        } else {
            bob.append("null");
        }

        bob.append(this.minLevel);
        return bob.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VillagerIngredient that = (VillagerIngredient) o;
        return minLevel == that.minLevel && Objects.equals(profession, that.profession) && Objects.equals(biome,
            that.biome);
    }

    @Override
    public int hashCode() {
        return Objects.hash(profession, biome, minLevel);
    }
}
