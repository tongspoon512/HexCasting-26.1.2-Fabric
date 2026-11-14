package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Per-face port role of the analytical engine controller.
 */
public enum PortMode implements StringRepresentable {
    DISABLED,
    INPUT,
    OUTPUT,
    PROGRAM,
    REDSTONE;

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public PortMode next() {
        return switch (this) {
            case DISABLED -> INPUT;
            case INPUT -> OUTPUT;
            case OUTPUT -> PROGRAM;
            case PROGRAM -> REDSTONE;
            case REDSTONE -> DISABLED;
        };
    }
}
