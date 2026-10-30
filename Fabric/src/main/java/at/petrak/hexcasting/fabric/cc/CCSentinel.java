package at.petrak.hexcasting.fabric.cc;

import at.petrak.hexcasting.api.player.Sentinel;
import org.ladysnake.cca.api.v3.component.Component;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public class CCSentinel implements Component, AutoSyncedComponent {
    public static final String
        TAG_HAS_SENTINEL = "has_sentinel",
        TAG_EXTENDS_RANGE = "extends_range",
        TAG_POSITION = "position",
        TAG_DIMENSION = "dimension";

    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);

    private final Player owner;
    private @Nullable Sentinel sentinel = null;

    public CCSentinel(Player owner) {
        this.owner = owner;
    }

    public @Nullable Sentinel getSentinel() {
        return sentinel;
    }

    public void setSentinel(Sentinel sentinel) {
        this.sentinel = sentinel;
        HexCardinalComponents.SENTINEL.sync(this.owner);
    }

    @Override
    public void readData(ValueInput input) {
        var hasSentinel = input.getBooleanOr(TAG_HAS_SENTINEL, false);
        if (hasSentinel) {
            var extendsRange = input.getBooleanOr(TAG_EXTENDS_RANGE, false);
            var position = input.read(TAG_POSITION, Vec3.CODEC).orElse(Vec3.ZERO);
            var dim = input.read(TAG_DIMENSION, DIMENSION_CODEC).orElse(null);
            this.sentinel = new Sentinel(extendsRange, position, dim);
        } else {
            this.sentinel = null;
        }
    }

    @Override
    public void writeData(ValueOutput output) {
        output.putBoolean(TAG_HAS_SENTINEL, this.sentinel != null);
        if (this.sentinel != null) {
            output.putBoolean(TAG_EXTENDS_RANGE, this.sentinel.extendsRange());
            output.store(TAG_POSITION, Vec3.CODEC, this.sentinel.position());
            output.store(TAG_DIMENSION, DIMENSION_CODEC, this.sentinel.dimension());
        }
    }
}
