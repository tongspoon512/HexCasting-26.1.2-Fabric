package pub.pigeon.yggdyy.hexcreating.blocks.iotawriter;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.casting.iota.NullIota;
import at.petrak.hexcasting.api.item.HexHolderItem;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import com.simibubi.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.simibubi.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * 传送带处理行为：写入器收到 iota 后，把经过的物品按 iota 大小累积“写入进度”，
 * 完成后写入物品（支持 iota 物品与法术卷轴类物品）。
 */
public class IotaWriteBehaviour extends BeltProcessingBehaviour {
    private static final String TAG_KEY = "hexcreating_write_progress";
    private static final String NEED_WRITE_TIME_KEY = "need_write_time";
    private static final String HAVE_WRITE_TIME_KEY = "have_write_time";
    private static final String TARGET_IOTA_KEY = "target_iota";
    /** 每个 iota 的基础工作量：32 RPM 下约 1 秒完成；转速越高越快。 */
    private static final long BASE_WORK_TICKS = 32L * 20L;

    private final IotaWriteBlockEntity blockEntity;
    private long needWriteTime;
    private long haveWriteTime;
    private Iota targetIota = new NullIota();

    public IotaWriteBehaviour(IotaWriteBlockEntity be) {
        super(be);
        this.blockEntity = be;
        whenItemEnters(this::itemEnter);
        whileItemHeld(this::itemHeld);
    }

    public long getNeedWriteTime() {
        return this.needWriteTime;
    }

    public long getHaveWriteTime() {
        return this.haveWriteTime;
    }

    private ProcessingResult itemEnter(TransportedItemStack transported,
        TransportedItemStackHandlerBehaviour handler) {
        Level level = this.blockEntity.getLevel();
        if (level == null) {
            return ProcessingResult.PASS;
        }
        ItemStack stack = transported.stack;
        Iota stored = this.blockEntity.getIota();
        // 没有接到任何 iota 时不要写入空值，否则会白白消耗结念绳。
        if (stored instanceof NullIota) {
            return ProcessingResult.PASS;
        }
        if (stack.getItem() instanceof IotaHolderItem item) {
            if (!item.writeable(stack) || !item.canWrite(stack, stored)) {
                return ProcessingResult.PASS;
            }
            this.beginWrite(stored, stack.getCount());
            return ProcessingResult.HOLD;
        }
        if (stack.getItem() instanceof HexHolderItem item) {
            if (stored.executable() || stored instanceof ListIota listIota && isListIotaExecutable(listIota)) {
                this.beginWrite(stored, stack.getCount());
                return ProcessingResult.HOLD;
            }
        }
        return ProcessingResult.PASS;
    }

    private void beginWrite(Iota iota, int count) {
        this.targetIota = iota;
        this.haveWriteTime = 0L;
        this.needWriteTime = BASE_WORK_TICKS * iota.size() * count;
        this.blockEntity.sync();
    }

    private ProcessingResult itemHeld(TransportedItemStack transported,
        TransportedItemStackHandlerBehaviour handler) {
        if (this.blockEntity.getLevel() == null) {
            return ProcessingResult.PASS;
        }
        double speed = Math.abs(this.blockEntity.getSpeed());
        if (speed <= 0.0) {
            // 没有旋转动力时不工作。
            return ProcessingResult.HOLD;
        }
        // 进度随转速线性增长：转速越高，写入越快。
        this.haveWriteTime += Math.max(1L, Math.round(speed));
        if (this.haveWriteTime < this.needWriteTime) {
            this.spawnWorkingParticles();
            this.blockEntity.sync();
            return ProcessingResult.HOLD;
        }
        if (this.targetIota instanceof NullIota) {
            this.haveWriteTime = 0L;
            this.needWriteTime = 0L;
            return ProcessingResult.PASS;
        }

        ItemStack stack = transported.stack;
        if (stack.getItem() instanceof IotaHolderItem item) {
            item.writeDatum(stack, this.targetIota);
        } else if (stack.getItem() instanceof HexHolderItem item) {
            if (this.targetIota.executable()) {
                item.writeHex(stack, List.of(this.targetIota), item.getPigment(stack), item.getMedia(stack));
            } else if (this.targetIota instanceof ListIota listIota) {
                List<Iota> program = new ArrayList<>();
                for (Iota sub : listIota.getList()) {
                    program.add(sub);
                }
                item.writeHex(stack, program, item.getPigment(stack), item.getMedia(stack));
            }
        }
        this.haveWriteTime = 0L;
        this.needWriteTime = 0L;
        this.targetIota = new NullIota();
        this.blockEntity.sync();
        return ProcessingResult.PASS;
    }

    private boolean isListIotaExecutable(ListIota iota) {
        for (Iota sub : iota.getList()) {
            if (!sub.executable()) {
                return false;
            }
        }
        return true;
    }

    private void spawnWorkingParticles() {
        if (!(this.blockEntity.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getGameTime() % 4L != 0L) {
            return;
        }
        BlockPos pos = this.blockEntity.getBlockPos();
        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
            pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
            2, 0.25, 0.1, 0.25, 0.01);
    }

    @Override
    public void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        CompoundTag data = tag.getCompoundOrEmpty(TAG_KEY);
        this.haveWriteTime = data.getLongOr(HAVE_WRITE_TIME_KEY, 0L);
        this.needWriteTime = data.getLongOr(NEED_WRITE_TIME_KEY, 0L);
        this.targetIota = new NullIota();
        if (!clientPacket && this.blockEntity.getLevel() instanceof ServerLevel serverLevel) {
            data.getCompound(TARGET_IOTA_KEY).ifPresent(saved -> {
                Iota read = IotaType.deserialize(saved, serverLevel);
                if (read != null) {
                    this.targetIota = read;
                }
            });
        }
    }

    @Override
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        CompoundTag data = new CompoundTag();
        data.putLong(HAVE_WRITE_TIME_KEY, this.haveWriteTime);
        data.putLong(NEED_WRITE_TIME_KEY, this.needWriteTime);
        if (!clientPacket) {
            data.put(TARGET_IOTA_KEY, IotaType.serialize(this.targetIota));
        }
        tag.put(TAG_KEY, data);
    }
}
