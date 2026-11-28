package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import at.petrak.hexcasting.api.casting.PatternShapeMatch;
import at.petrak.hexcasting.api.casting.eval.ExecutionClientView;
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage;
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.misc.MediaConstants;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.common.casting.PatternRegistryManifest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaNetProcessor;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaPack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Executes programs for a controller. Kept separate from the block entity so the scheduler can be
 * tested and extended without touching persistence code.
 */
public final class AnalyticalEngineRuntime {
    private static final int MAX_STEPS_PER_TICK = 32;
    private static final int MAX_PACKS_PER_TICK = 8;
    private static final int MAX_WAIT_TICKS = 12000;
    private static final long PATTERN_SURCHARGE = Math.max(1L, MediaConstants.DUST_UNIT / 100);

    private AnalyticalEngineRuntime() {
    }

    public static void tick(AnalyticalEngineControllerBlockEntity controller) {
        if (!(controller.getLevel() instanceof ServerLevel level) || !controller.isRunning()) {
            return;
        }
        List<Iota> program = controller.getProgram();
        if (program.isEmpty()) {
            controller.setRunning(false);
            return;
        }

        if (controller.getWaitTicks() > 0) {
            controller.setWaitTicks(controller.getWaitTicks() - 1);
            controller.setState(EngineState.State.PAUSED);
            controller.setPauseReason(EngineState.PauseReason.WAIT);
            return;
        }

        AnalyticalEngineCluster cluster = controller.cluster();
        if (cluster.hasConflict()) {
            fail(controller, EngineState.ErrorCode.CORE_CONFLICT);
            return;
        }

        float rpm = Math.abs(cluster.rpm());
        if (rpm <= 0.001f) {
            controller.setState(EngineState.State.PAUSED);
            controller.setPauseReason(EngineState.PauseReason.NO_POWER);
            return;
        }

        double rate = (1 + cluster.clampedAluCount()) * Math.min(8.0, rpm / 32.0);
        double accumulator = controller.getOpAccumulator() + rate;
        int steps = (int) Math.min(MAX_STEPS_PER_TICK, Math.floor(accumulator));
        controller.setOpAccumulator(accumulator - steps);

        controller.setState(EngineState.State.RUNNING);
        controller.setPauseReason(EngineState.PauseReason.NONE);
        for (int i = 0; i < steps; i++) {
            if (!controller.isRunning() || controller.getState() != EngineState.State.RUNNING) {
                break;
            }
            if (!step(controller, level, cluster)) {
                break;
            }
        }
        controller.setChanged();
    }

    private static boolean step(AnalyticalEngineControllerBlockEntity controller, ServerLevel level,
                                AnalyticalEngineCluster cluster) {
        int pc = controller.getPc();
        List<Iota> program = controller.getProgram();
        int limit = Math.min(program.size(), cluster.programLength());
        if (pc < 0 || pc >= limit) {
            finish(controller);
            return false;
        }

        Iota iota = program.get(pc);
        if (iota instanceof InstructionIota instruction) {
            return applyInstruction(controller, cluster, instruction);
        }
        if (iota instanceof PatternIota pattern) {
            return applyPattern(controller, level, cluster, pattern);
        }
        if (controller.getStack().size() >= cluster.stackDepth()) {
            overflow(controller);
            return false;
        }
        controller.getStack().add(iota);
        controller.setPc(pc + 1);
        return true;
    }

    private static boolean applyInstruction(AnalyticalEngineControllerBlockEntity controller,
                                            AnalyticalEngineCluster cluster, InstructionIota iota) {
        InstructionIota.Instruction instruction = iota.instruction();
        int pc = controller.getPc();
        switch (instruction.op()) {
            case JMP -> {
                return jump(controller, cluster, instruction.arg());
            }
            case JZ -> {
                if (controller.getStack().isEmpty()) {
                    fail(controller, EngineState.ErrorCode.STACK_UNDERFLOW);
                    return false;
                }
                Iota condition = controller.getStack().remove(controller.getStack().size() - 1);
                if (!condition.isTruthy()) {
                    return jump(controller, cluster, instruction.arg());
                }
                controller.setPc(pc + 1);
                return true;
            }
            case WAIT -> {
                int ticks = Math.max(1, Math.min(MAX_WAIT_TICKS, instruction.arg()));
                controller.setWaitTicks(ticks);
                controller.setPc(pc + 1);
                controller.setState(EngineState.State.PAUSED);
                controller.setPauseReason(EngineState.PauseReason.WAIT);
                return false;
            }
            case READ -> {
                IotaPack pack = controller.getInputQueue().pollFirst();
                if (pack == null) {
                    // Block at the read until a matching pack arrives.
                    return false;
                }
                if (controller.getStack().size() >= cluster.stackDepth()) {
                    overflow(controller);
                    return false;
                }
                controller.getStack().add(pack.iota);
                controller.setPc(pc + 1);
                return true;
            }
            case EMIT -> {
                if (controller.getStack().isEmpty()) {
                    fail(controller, EngineState.ErrorCode.STACK_UNDERFLOW);
                    return false;
                }
                int outputs = 0;
                for (Direction direction : Direction.values()) {
                    if (controller.getPortMode(direction) == PortMode.OUTPUT) {
                        outputs++;
                    }
                }
                if (outputs == 0) {
                    controller.setPauseReason(EngineState.PauseReason.OUT_BLOCKED);
                    return false;
                }
                if (controller.getPacketsThisTick() + outputs > MAX_PACKS_PER_TICK) {
                    // Out of bandwidth this tick; retry the same instruction next tick.
                    return false;
                }
                Iota value = controller.getStack().get(controller.getStack().size() - 1);
                if (!emit(controller, value)) {
                    if (controller.getPauseReason() != EngineState.PauseReason.PORT_CONFLICT) {
                        controller.setPauseReason(EngineState.PauseReason.OUT_BLOCKED);
                    }
                    return false;
                }
                controller.getStack().remove(controller.getStack().size() - 1);
                controller.setPc(pc + 1);
                return true;
            }
            case HALT -> {
                finish(controller);
                return false;
            }
            default -> {
                fail(controller, EngineState.ErrorCode.INVALID_PROGRAM);
                return false;
            }
        }
    }

    private static boolean emit(AnalyticalEngineControllerBlockEntity controller, Iota value) {
        if (!(controller.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        boolean sent = false;
        for (Direction direction : Direction.values()) {
            if (controller.getPortMode(direction) != PortMode.OUTPUT) {
                continue;
            }
            Set<String> headInfo = new java.util.LinkedHashSet<>();
            headInfo.add(AnalyticalEngineControllerBlockEntity.CHANNEL_PREFIX
                + controller.getChannel(direction));
            // Origin marker lets this controller spot a same-channel feedback loop.
            headInfo.add(AnalyticalEngineControllerBlockEntity.originKey(controller.getBlockPos()));
            IotaPack pack = IotaPack.createNew(value, headInfo);
            controller.addPacketsThisTick(1);
            BlockPos target = controller.getBlockPos().relative(direction);
            sent |= IotaNetProcessor.trySendIotaPack(pack,
                controller.getTerminals().get(direction.ordinal()), target, level);
        }
        return sent;
    }

    private static boolean applyPattern(AnalyticalEngineControllerBlockEntity controller, ServerLevel level,
                                        AnalyticalEngineCluster cluster, PatternIota pattern) {
        List<Iota> seed = new ArrayList<>(controller.getStack());
        CastingImage image = new CastingImage(seed, 0, List.of(), false, 0L, new CompoundTag());
        AnalyticalEngineCastEnv env = new AnalyticalEngineCastEnv(controller, level);

        // Patterns that need a caster context are not circle-safe; refuse them like a circle does.
        PatternShapeMatch match = PatternRegistryManifest.matchPattern(pattern.getPattern(), env);
        Identifier actionId = null;
        if (match instanceof PatternShapeMatch.Normal normal) {
            actionId = normal.key.identifier();
        } else if (match instanceof PatternShapeMatch.PerWorld perWorld) {
            actionId = perWorld.key.identifier();
        }
        if (actionId != null && !HexConfig.server().isActionAllowedInCircles(actionId)) {
            controller.setPauseReason(EngineState.PauseReason.LOCKED_PATTERN);
            return false;
        }

        CastingVM vm = new CastingVM(image, env);
        ExecutionClientView view = vm.queueExecuteAndWrapIota(pattern, level);

        if (!view.getResolutionType().getSuccess()) {
            if (controller.getMediaBuffer() <= 0) {
                // Media starvation pauses instead of erroring so refilling resumes the program.
                controller.setPauseReason(EngineState.PauseReason.NO_MEDIA);
            } else {
                fail(controller, EngineState.ErrorCode.MISHAP);
            }
            return false;
        }

        List<Iota> result = new ArrayList<>(vm.getImage().getStack());
        if (result.size() > cluster.stackDepth()) {
            overflow(controller);
            return false;
        }
        controller.getStack().clear();
        controller.getStack().addAll(result);
        controller.setMediaBuffer(controller.getMediaBuffer() - PATTERN_SURCHARGE);
        controller.setPc(controller.getPc() + 1);
        return true;
    }

    private static boolean jump(AnalyticalEngineControllerBlockEntity controller,
                                AnalyticalEngineCluster cluster, int target) {
        int limit = Math.min(controller.getProgram().size(), cluster.programLength());
        if (target < 0 || target >= limit) {
            fail(controller, EngineState.ErrorCode.OUT_OF_BOUNDS);
            return false;
        }
        controller.setPc(target);
        return true;
    }

    private static void finish(AnalyticalEngineControllerBlockEntity controller) {
        if (controller.isAutoRestart()) {
            controller.setPc(0);
            controller.setRunning(true);
        } else {
            controller.setRunning(false);
            controller.setState(EngineState.State.IDLE);
        }
        controller.setChanged();
    }

    private static void fail(AnalyticalEngineControllerBlockEntity controller, EngineState.ErrorCode code) {
        controller.setError(code);
        controller.setRunning(false);
        controller.setState(EngineState.State.ERROR);
        controller.setChanged();
    }

    /**
     * Stack overflow keeps the program but clears the stack, mirroring the hex casting mishap.
     */
    private static void overflow(AnalyticalEngineControllerBlockEntity controller) {
        controller.getStack().clear();
        fail(controller, EngineState.ErrorCode.STACK_OVERFLOW);
    }

    /**
     * Replaces the controller program from a {@code PROGRAM} port write. The pack is consumed even
     * when validation fails, matching the "reject and return" rule of the design doc.
     */
    public static boolean acceptProgramWrite(AnalyticalEngineControllerBlockEntity controller, IotaPack pack) {
        if (pack.iota instanceof ListIota list) {
            List<Iota> program = new ArrayList<>();
            for (Iota iota : list.getList()) {
                program.add(iota);
            }
            return installProgram(controller, program);
        }
        return false;
    }

    /**
     * Reads a program list from a printed paper or paper reel.
     */
    public static List<Iota> readProgram(ItemStack stack, ServerLevel level) {
        if (!(stack.getItem() instanceof IotaHolderItem holder)) {
            return List.of();
        }
        CompoundTag tag = holder.readIotaTag(stack);
        if (tag == null) {
            return List.of();
        }
        Iota iota = IotaType.deserialize(tag, level);
        if (iota instanceof ListIota list) {
            List<Iota> program = new ArrayList<>();
            for (Iota element : list.getList()) {
                program.add(element);
            }
            return program;
        }
        return List.of();
    }

    public static boolean installProgram(AnalyticalEngineControllerBlockEntity controller, List<Iota> program) {
        AnalyticalEngineCluster cluster = controller.cluster();
        if (program.size() > cluster.programLength()) {
            return false;
        }
        for (Iota iota : program) {
            if (iota instanceof InstructionIota instruction) {
                int arg = instruction.instruction().arg();
                switch (instruction.instruction().op()) {
                    case JMP, JZ -> {
                        if (arg < 0 || arg >= program.size()) {
                            return false;
                        }
                    }
                    case WAIT -> {
                        if (arg < 1 || arg > MAX_WAIT_TICKS) {
                            return false;
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        controller.getProgram().clear();
        controller.getProgram().addAll(program);
        controller.getStack().clear();
        controller.setPc(0);
        controller.setWaitTicks(0);
        controller.setRunning(false);
        controller.setError(EngineState.ErrorCode.NONE);
        controller.setPauseReason(EngineState.PauseReason.NONE);
        controller.setChanged();
        return true;
    }
}
