package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import at.petrak.hexcasting.api.casting.iota.Iota;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * A machine instruction stored inside a program reel. Instructions are data to the normal cast
 * engine and only the analytical engine interprets them.
 */
public class InstructionIota extends Iota {
    public static final int NO_ARG = -1;

    public enum Op implements StringRepresentable {
        JMP,
        JZ,
        WAIT,
        READ,
        EMIT,
        HALT;

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }

        public static Op byName(String name) {
            for (Op op : values()) {
                if (op.getSerializedName().equals(name)) {
                    return op;
                }
            }
            return null;
        }
    }

    public record Instruction(Op op, int arg) {
    }

    public InstructionIota(Instruction instruction) {
        super(InstructionIotaType.INSTANCE, instruction);
    }

    public Instruction instruction() {
        return (Instruction) this.payload;
    }

    @Override
    public boolean isTruthy() {
        return true;
    }

    @Override
    protected boolean toleratesOther(Iota that) {
        return that instanceof InstructionIota other && this.instruction().equals(other.instruction());
    }

    @Override
    public @NotNull Tag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putString("op", this.instruction().op().getSerializedName());
        tag.putInt("arg", this.instruction().arg());
        return tag;
    }
}
