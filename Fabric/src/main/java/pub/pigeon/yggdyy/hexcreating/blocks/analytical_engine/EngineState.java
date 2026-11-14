package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

/**
 * Runtime state machine of an analytical engine controller, as specified in
 * {@code doc/analytical-engine-gameplay-design.md} §5.
 */
public final class EngineState {
    private EngineState() {
    }

    public enum State implements StringRepresentable {
        IDLE,
        READY,
        RUNNING,
        PAUSED,
        ERROR;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public Status toStatus() {
            return switch (this) {
                case IDLE -> Status.IDLE;
                case READY, PAUSED -> Status.READY;
                case RUNNING -> Status.RUNNING;
                case ERROR -> Status.ERROR;
            };
        }
    }

    /**
     * The four lamp colours exposed as a block state, matching §4.7 of the design doc.
     */
    public enum Status implements StringRepresentable {
        IDLE,
        READY,
        RUNNING,
        ERROR;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public enum PauseReason implements StringRepresentable {
        NONE,
        NO_POWER,
        NO_MEDIA,
        OUT_BLOCKED,
        WAIT,
        REDSTONE,
        MANUAL,
        PROGRAM_WRITE,
        PORT_CONFLICT,
        LOCKED_PATTERN;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public enum ErrorCode implements StringRepresentable {
        NONE,
        NO_PROGRAM,
        STACK_OVERFLOW,
        STACK_UNDERFLOW,
        OUT_OF_BOUNDS,
        OUT_BLOCKED,
        NO_MEDIA,
        NO_POWER,
        LOCKED_PATTERN,
        MISHAP,
        UNDERSTRESS,
        OVERSPEED,
        CORE_CONFLICT,
        INVALID_PROGRAM;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public boolean isError() {
            return this != NONE;
        }
    }
}
