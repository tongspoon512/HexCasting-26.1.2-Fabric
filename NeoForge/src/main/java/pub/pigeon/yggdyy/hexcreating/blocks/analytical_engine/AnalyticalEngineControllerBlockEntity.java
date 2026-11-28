package pub.pigeon.yggdyy.hexcreating.blocks.analytical_engine;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IIotaTerminalHost;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaPack;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaTerminal;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Program holder, scheduler and iota-network endpoint of an analytical engine cluster.
 * <p>
 * All runtime data lives here so that removing an ALU or kinetic interface never loses the program.
 */
public class AnalyticalEngineControllerBlockEntity extends HexBlockEntity
    implements AnalyticalEnginePart, IIotaTerminalHost, IHaveGoggleInformation {
    public static final int MAX_QUEUE = 64;
    public static final long MAX_MEDIA = 2000L;
    public static final String DEFAULT_CHANNEL = "default";
    public static final String CHANNEL_PREFIX = "channel:";
    public static final String ORIGIN_PREFIX = "origin:";

    private static final int CLUSTER_REFRESH_TICKS = 20;
    private static final int SELF_LOOP_LIMIT = 3;

    private final List<Iota> program = new ArrayList<>();
    private final List<Iota> stack = new ArrayList<>();
    private final Deque<IotaPack> inputQueue = new ArrayDeque<>();
    private final Deque<IotaPack> outputQueue = new ArrayDeque<>();
    private final List<IotaTerminal> terminals = new ArrayList<>(6);
    private final PortMode[] portModes = new PortMode[6];
    private final String[] channels = new String[6];

    private EngineState.State state = EngineState.State.IDLE;
    private EngineState.ErrorCode error = EngineState.ErrorCode.NONE;
    private EngineState.PauseReason pauseReason = EngineState.PauseReason.NONE;
    private int pc;
    private long mediaBuffer;
    private int aluCount;
    private int kineticCount;
    private float rpm;
    private int waitTicks;
    private boolean running;
    private boolean autoRestart;
    private int refreshCooldown;
    private double opAccumulator;
    private int packetsThisTick;
    private int selfLoopTicks;
    private long lastSelfLoopTick = -1L;
    private boolean redstonePowered;

    public AnalyticalEngineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.ANALYTICAL_ENGINE_CONTROLLER, pos, state);
        for (int i = 0; i < 6; i++) {
            this.terminals.add(new IotaTerminal(this, i));
            this.portModes[i] = PortMode.DISABLED;
            this.channels[i] = DEFAULT_CHANNEL;
        }
    }

    // ------------------------------------------------------------------ cluster

    @Override
    public AnalyticalEngineCluster cluster() {
        return AnalyticalEngineClusterScanner.scan(this.level, this.worldPosition);
    }

    public int aluCount() {
        return this.aluCount;
    }

    public float rpm() {
        return this.rpm;
    }

    public int kineticCount() {
        return this.kineticCount;
    }

    public void requestClusterRefresh() {
        this.refreshCooldown = 0;
    }

    private void refreshCluster() {
        AnalyticalEngineCluster cluster = cluster();
        boolean changed = cluster.aluCount() != this.aluCount
            || cluster.kineticCount() != this.kineticCount
            || Math.abs(cluster.rpm() - this.rpm) > 0.01f;
        this.aluCount = cluster.aluCount();
        this.kineticCount = cluster.kineticCount();
        this.rpm = cluster.rpm();

        if (cluster.hasConflict()) {
            this.error = EngineState.ErrorCode.CORE_CONFLICT;
        } else if (this.error == EngineState.ErrorCode.CORE_CONFLICT) {
            this.error = EngineState.ErrorCode.NONE;
        }
        if (changed) {
            setChanged();
        }
    }

    // ------------------------------------------------------------------ goggles

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.translatable("hexcreating.analytical_engine.goggles.header"));
        tooltip.add(Component.translatable("hexcreating.analytical_engine.goggles.state",
            Component.translatable("hexcreating.analytical_engine.state."
                + this.state.getSerializedName())));
        tooltip.add(Component.translatable("hexcreating.analytical_engine.goggles.alu",
            this.aluCount));
        tooltip.add(Component.translatable("hexcreating.analytical_engine.goggles.speed",
            String.format(java.util.Locale.ROOT, "%.1f", Math.abs(this.rpm))));
        tooltip.add(Component.translatable("hexcreating.analytical_engine.goggles.media",
            this.mediaBuffer));
        tooltip.add(Component.translatable("hexcreating.analytical_engine.goggles.pointer",
            this.pc, this.program.size()));
        if (this.error.isError()) {
            tooltip.add(Component.translatable("hexcreating.analytical_engine.goggles.error",
                Component.translatable("hexcreating.analytical_engine.error."
                    + this.error.getSerializedName())));
        }
        return true;
    }

    // ------------------------------------------------------------------ ticking

    public void serverTick() {
        if (!(this.level instanceof ServerLevel)) {
            return;
        }
        this.packetsThisTick = 0;
        if (--this.refreshCooldown <= 0) {
            this.refreshCooldown = CLUSTER_REFRESH_TICKS;
            refreshCluster();
        }
        tickRedstonePorts();

        AnalyticalEngineRuntime.tick(this);
        updateState();
        syncStatus();
    }

    /**
     * A REDSTONE port starts the program on a rising edge, as described in §4.6 of the design doc.
     */
    private void tickRedstonePorts() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        boolean powered = false;
        for (int i = 0; i < 6; i++) {
            if (this.portModes[i] != PortMode.REDSTONE) {
                continue;
            }
            Direction face = Direction.values()[i];
            if (serverLevel.getSignal(this.worldPosition.relative(face), face) > 0) {
                powered = true;
                break;
            }
        }
        if (powered && !this.redstonePowered && !this.program.isEmpty()) {
            if (this.error.isError()) {
                resetError();
            }
            this.running = true;
            this.pauseReason = EngineState.PauseReason.NONE;
            setChanged();
        }
        this.redstonePowered = powered;
    }

    private void updateState() {
        EngineState.State next;
        if (this.error.isError()) {
            next = EngineState.State.ERROR;
        } else if (this.program.isEmpty()) {
            next = EngineState.State.IDLE;
        } else if (this.pauseReason != EngineState.PauseReason.NONE
            && this.pauseReason != EngineState.PauseReason.MANUAL) {
            next = EngineState.State.PAUSED;
        } else if (this.running) {
            next = EngineState.State.RUNNING;
        } else {
            next = EngineState.State.READY;
        }
        if (next != this.state) {
            this.state = next;
            setChanged();
        }
    }

    private void syncStatus() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        BlockState current = getBlockState();
        if (!current.hasProperty(AnalyticalEngineControllerBlock.STATUS)) {
            return;
        }
        EngineState.Status target = this.state.toStatus();
        if (current.getValue(AnalyticalEngineControllerBlock.STATUS) != target) {
            serverLevel.setBlock(this.worldPosition,
                current.setValue(AnalyticalEngineControllerBlock.STATUS, target), 3);
        }
    }

    // ------------------------------------------------------------------ control

    public void toggleRun() {
        if (this.error.isError()) {
            resetError();
        }
        if (this.state == EngineState.State.RUNNING) {
            this.running = false;
            this.pauseReason = EngineState.PauseReason.MANUAL;
        } else if (!this.program.isEmpty()) {
            this.running = true;
            this.pauseReason = EngineState.PauseReason.NONE;
        }
        setChanged();
    }

    public void resetError() {
        this.error = EngineState.ErrorCode.NONE;
        this.pauseReason = EngineState.PauseReason.NONE;
        this.pc = 0;
        this.waitTicks = 0;
        this.running = false;
        setChanged();
    }

    public void cyclePort(Direction face) {
        int index = face.ordinal();
        this.portModes[index] = this.portModes[index].next();
        sync();
    }

    // ------------------------------------------------------------------ ports

    @Override
    public List<IotaTerminal> getTerminals() {
        return this.terminals;
    }

    @Override
    public @Nullable IotaTerminal getDefaultTerminal(IotaTerminal from) {
        Direction face = faceFrom(from.pos());
        return face == null ? null : this.terminals.get(face.ordinal());
    }

    @Override
    public boolean tryReceiveIotaPack(IotaPack pack, IotaTerminal from, IotaTerminal to) {
        if (!(this.level instanceof ServerLevel)) {
            return false;
        }
        if (pack.hasHeadInfo(originKey(this.worldPosition))) {
            noteSelfLoop();
            return false;
        }
        int index = to.terminalId;
        if (index < 0 || index >= 6) {
            return false;
        }
        PortMode mode = this.portModes[index];
        if (mode != PortMode.INPUT && mode != PortMode.PROGRAM) {
            return false;
        }
        if (!channelMatches(pack, this.channels[index])) {
            return false;
        }
        if (mode == PortMode.PROGRAM) {
            return acceptProgram(pack);
        }
        if (this.inputQueue.size() >= MAX_QUEUE) {
            return false;
        }
        this.inputQueue.addLast(pack);
        setChanged();
        return true;
    }

    /**
     * Same-channel output wired straight back into an input is blocked immediately; three
     * consecutive ticks of feedback pause the engine with {@code PORT_CONFLICT} (§7.3).
     */
    private void noteSelfLoop() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        long now = serverLevel.getGameTime();
        if (this.lastSelfLoopTick == now) {
            return;
        }
        if (this.lastSelfLoopTick != now - 1) {
            this.selfLoopTicks = 0;
        }
        this.lastSelfLoopTick = now;
        this.selfLoopTicks++;
        if (this.selfLoopTicks >= SELF_LOOP_LIMIT) {
            this.pauseReason = EngineState.PauseReason.PORT_CONFLICT;
            this.running = false;
            setChanged();
        }
    }

    public static String originKey(BlockPos pos) {
        return ORIGIN_PREFIX + pos.toShortString();
    }

    /**
     * Program port writes are validated by the runtime before replacing the program.
     */
    private boolean acceptProgram(IotaPack pack) {
        return AnalyticalEngineRuntime.acceptProgramWrite(this, pack);
    }

    private boolean channelMatches(IotaPack pack, String channel) {
        String found = null;
        for (String info : pack.headInfo) {
            if (info.startsWith(CHANNEL_PREFIX)) {
                found = info.substring(CHANNEL_PREFIX.length());
                break;
            }
        }
        if (found == null) {
            found = DEFAULT_CHANNEL;
        }
        return found.equals(channel);
    }

    private @Nullable Direction faceFrom(BlockPos other) {
        BlockPos self = getBlockPos();
        int dx = other.getX() - self.getX();
        int dy = other.getY() - self.getY();
        int dz = other.getZ() - self.getZ();
        for (Direction direction : Direction.values()) {
            if (direction.getStepX() == dx && direction.getStepY() == dy
                && direction.getStepZ() == dz) {
                return direction;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ accessors

    public List<Iota> getProgram() {
        return this.program;
    }

    public List<Iota> getStack() {
        return this.stack;
    }

    public Deque<IotaPack> getInputQueue() {
        return this.inputQueue;
    }

    public Deque<IotaPack> getOutputQueue() {
        return this.outputQueue;
    }

    public EngineState.State getState() {
        return this.state;
    }

    public EngineState.ErrorCode getError() {
        return this.error;
    }

    public EngineState.PauseReason getPauseReason() {
        return this.pauseReason;
    }

    public void setState(EngineState.State state) {
        this.state = state;
    }

    public void setError(EngineState.ErrorCode error) {
        this.error = error;
    }

    public void setPauseReason(EngineState.PauseReason pauseReason) {
        this.pauseReason = pauseReason;
    }

    public int getPc() {
        return this.pc;
    }

    public void setPc(int pc) {
        this.pc = pc;
    }

    public long getMediaBuffer() {
        return this.mediaBuffer;
    }

    public void setMediaBuffer(long mediaBuffer) {
        this.mediaBuffer = Math.max(0, Math.min(MAX_MEDIA, mediaBuffer));
    }

    public int getWaitTicks() {
        return this.waitTicks;
    }

    public void setWaitTicks(int waitTicks) {
        this.waitTicks = waitTicks;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public double getOpAccumulator() {
        return this.opAccumulator;
    }

    public void setOpAccumulator(double opAccumulator) {
        this.opAccumulator = opAccumulator;
    }

    public int getPacketsThisTick() {
        return this.packetsThisTick;
    }

    public void addPacketsThisTick(int amount) {
        this.packetsThisTick += amount;
    }

    public boolean isAutoRestart() {
        return this.autoRestart;
    }

    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }

    public PortMode getPortMode(Direction face) {
        return this.portModes[face.ordinal()];
    }

    public String getChannel(Direction face) {
        return this.channels[face.ordinal()];
    }

    public void setChannel(Direction face, String channel) {
        this.channels[face.ordinal()] = channel;
        setChanged();
    }

    // ------------------------------------------------------------------ persistence

    @Override
    protected void saveModData(CompoundTag tag) {
        CompoundTag programTag = new CompoundTag();
        programTag.putInt("length", this.program.size());
        for (int i = 0; i < this.program.size(); i++) {
            programTag.put("iota_" + i, IotaType.serialize(this.program.get(i)));
        }
        tag.put("program", programTag);

        CompoundTag stackTag = new CompoundTag();
        stackTag.putInt("length", this.stack.size());
        for (int i = 0; i < this.stack.size(); i++) {
            stackTag.put("iota_" + i, IotaType.serialize(this.stack.get(i)));
        }
        tag.put("stack", stackTag);

        tag.putInt("pc", this.pc);
        tag.putLong("media", this.mediaBuffer);
        tag.putString("state", this.state.getSerializedName());
        tag.putString("error", this.error.getSerializedName());
        tag.putString("pause_reason", this.pauseReason.getSerializedName());
        tag.putInt("wait_ticks", this.waitTicks);
        tag.putBoolean("running", this.running);
        tag.putBoolean("auto_restart", this.autoRestart);
        tag.putDouble("op_accumulator", this.opAccumulator);
        for (int i = 0; i < 6; i++) {
            tag.putString("port_" + i, this.portModes[i].getSerializedName());
            tag.putString("channel_" + i, this.channels[i]);
        }
        for (IotaTerminal terminal : this.terminals) {
            terminal.writeNbt(tag);
        }
    }

    @Override
    protected void loadModData(CompoundTag tag) {
        ServerLevel serverLevel = this.level instanceof ServerLevel level ? level : null;
        this.program.clear();
        CompoundTag programTag = tag.getCompoundOrEmpty("program");
        int programLength = programTag.getIntOr("length", 0);
        for (int i = 0; i < programLength; i++) {
            var iotaTag = programTag.getCompound("iota_" + i);
            if (iotaTag.isPresent()) {
                Iota iota = IotaType.deserialize(iotaTag.get(), serverLevel);
                if (iota != null) {
                    this.program.add(iota);
                }
            }
        }

        this.stack.clear();
        CompoundTag stackTag = tag.getCompoundOrEmpty("stack");
        int stackLength = stackTag.getIntOr("length", 0);
        for (int i = 0; i < stackLength; i++) {
            var iotaTag = stackTag.getCompound("iota_" + i);
            if (iotaTag.isPresent()) {
                Iota iota = IotaType.deserialize(iotaTag.get(), serverLevel);
                if (iota != null) {
                    this.stack.add(iota);
                }
            }
        }

        this.pc = tag.getIntOr("pc", 0);
        this.mediaBuffer = tag.getLongOr("media", 0L);
        this.state = readEnum(EngineState.State.class, tag.getStringOr("state", "idle"), EngineState.State.IDLE);
        this.error = readEnum(EngineState.ErrorCode.class, tag.getStringOr("error", "none"),
            EngineState.ErrorCode.NONE);
        this.pauseReason = readEnum(EngineState.PauseReason.class,
            tag.getStringOr("pause_reason", "none"), EngineState.PauseReason.NONE);
        this.waitTicks = tag.getIntOr("wait_ticks", 0);
        this.running = tag.getBooleanOr("running", false);
        this.autoRestart = tag.getBooleanOr("auto_restart", false);
        this.opAccumulator = tag.getDoubleOr("op_accumulator", 0.0);
        for (int i = 0; i < 6; i++) {
            this.portModes[i] = readEnum(PortMode.class, tag.getStringOr("port_" + i, "disabled"),
                PortMode.DISABLED);
            this.channels[i] = tag.getStringOr("channel_" + i, DEFAULT_CHANNEL);
        }
        for (IotaTerminal terminal : this.terminals) {
            terminal.readNbt(tag);
        }
        this.refreshCooldown = 0;
    }

    private static <E extends Enum<E>> E readEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
