package at.petrak.hexcasting.common.entities;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class HexEntities {
    public static void registerEntities(BiConsumer<EntityType<?>, Identifier> r) {
        for (var e : ENTITIES.entrySet()) {
            r.accept(e.getValue(), e.getKey());
        }
    }

    private static final Map<Identifier, EntityType<?>> ENTITIES = new LinkedHashMap<>();

    public static final EntityType<EntityWallScroll> WALL_SCROLL = register("wall_scroll",
        EntityType.Builder.<EntityWallScroll>of(EntityWallScroll::new, MobCategory.MISC)
            .sized(0.5f, 0.5f).clientTrackingRange(10).updateInterval(Integer.MAX_VALUE)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, modLoc("wall_scroll"))));

    private static <T extends Entity> EntityType<T> register(String id, EntityType<T> type) {
        var old = ENTITIES.put(modLoc(id), type);
        if (old != null) {
            throw new IllegalArgumentException("Typo? Duplicate id " + id);
        }
        return type;
    }
}
