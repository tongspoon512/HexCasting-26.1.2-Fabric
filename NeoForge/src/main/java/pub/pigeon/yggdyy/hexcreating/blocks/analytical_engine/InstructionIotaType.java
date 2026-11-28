package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import at.petrak.hexcasting.api.casting.iota.IotaType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;

import java.util.function.BiConsumer;

public class InstructionIotaType extends IotaType<InstructionIota> {
    public static final InstructionIotaType INSTANCE = new InstructionIotaType();

    private InstructionIotaType() {
    }

    public static void register(BiConsumer<IotaType<?>, Identifier> registrar) {
        registrar.accept(INSTANCE, HexCreatingCompat.id("instruction"));
    }

    @Override
    public InstructionIota deserialize(Tag tag, ServerLevel world) {
        if (!(tag instanceof CompoundTag compound)) {
            return null;
        }
        InstructionIota.Op op = InstructionIota.Op.byName(compound.getStringOr("op", ""));
        if (op == null) {
            return null;
        }
        int arg = compound.getIntOr("arg", InstructionIota.NO_ARG);
        return new InstructionIota(new InstructionIota.Instruction(op, arg));
    }

    @Override
    public Component display(Tag tag) {
        if (!(tag instanceof CompoundTag compound)) {
            return Component.literal("?");
        }
        InstructionIota.Op op = InstructionIota.Op.byName(compound.getStringOr("op", ""));
        if (op == null) {
            return Component.literal("?");
        }
        Component name = Component.translatable("hexcreating.instruction." + op.getSerializedName());
        int arg = compound.getIntOr("arg", InstructionIota.NO_ARG);
        return arg == InstructionIota.NO_ARG ? name
            : Component.translatable("hexcreating.instruction.with_arg", name, arg);
    }

    @Override
    public int color() {
        return 0x7B68EE;
    }
}
