package at.petrak.hexcasting.api.casting.area;

import java.util.Locale;

/**
 * The four kinds of domains ("areas") that can be registered by the domain patterns.
 */
public enum AreaKind {
    /** 生得—风之提整：a five-pointed star. */
    PENTAGRAM,
    /** 生得—火之提整：a triangle. */
    TRIANGLE,
    /** 生得—水之馏化：a sphere. */
    SPHERE,
    /** 生得—地之提整：a cuboid. */
    CUBOID;

    public boolean isPolygon() {
        return this == PENTAGRAM || this == TRIANGLE;
    }

    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static AreaKind byName(String name) {
        for (AreaKind kind : values()) {
            if (kind.getSerializedName().equals(name)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("unknown area kind " + name);
    }
}
