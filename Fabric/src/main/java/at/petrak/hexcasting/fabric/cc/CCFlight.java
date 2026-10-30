package at.petrak.hexcasting.fabric.cc;

import at.petrak.hexcasting.api.player.FlightAbility;
import org.ladysnake.cca.api.v3.component.Component;
import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class CCFlight implements Component {
    public static final String
        TAG_ALLOWED = "allowed", // Fake: use this as a null sentinel
        TAG_TIME_LEFT = "time_left",
        TAG_DIMENSION = "dimension",
        TAG_ORIGIN = "origin",
        TAG_RADIUS = "radius";

    private static final Codec<ResourceKey<Level>> DIMENSION_CODEC = ResourceKey.codec(Registries.DIMENSION);

    private final ServerPlayer owner;
    @Nullable
    private FlightAbility flight = null;

    public CCFlight(ServerPlayer owner) {
        this.owner = owner;
    }


    @Nullable
    public FlightAbility getFlight() {
        return flight;
    }

    public void setFlight(FlightAbility flight) {
        this.flight = flight;
    }

    @Override
    public void readData(ValueInput input) {
        var allowed = input.getBooleanOr(TAG_ALLOWED, false);
        if (!allowed) {
            this.flight = null;
        } else {
            var timeLeft = input.getIntOr(TAG_TIME_LEFT, 0);
            var dim = input.read(TAG_DIMENSION, DIMENSION_CODEC).orElse(null);
            var origin = input.read(TAG_ORIGIN, Vec3.CODEC).orElse(Vec3.ZERO);
            var radius = input.getDoubleOr(TAG_RADIUS, 0);
            this.flight = new FlightAbility(timeLeft, dim, origin, radius);
        }
    }

    @Override
    public void writeData(ValueOutput output) {
        output.putBoolean(TAG_ALLOWED, this.flight != null);
        if (this.flight != null) {
            output.putInt(TAG_TIME_LEFT, this.flight.timeLeft());
            output.store(TAG_DIMENSION, DIMENSION_CODEC, this.flight.dimension());
            output.store(TAG_ORIGIN, Vec3.CODEC, this.flight.origin());
            output.putDouble(TAG_RADIUS, this.flight.radius());
        }
    }
}
