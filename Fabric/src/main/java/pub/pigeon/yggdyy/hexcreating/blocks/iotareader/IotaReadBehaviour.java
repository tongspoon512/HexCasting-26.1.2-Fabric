package pub.pigeon.yggdyy.hexcreating.blocks.iotareader;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.IotaType;
import at.petrak.hexcasting.api.casting.iota.ListIota;
import at.petrak.hexcasting.api.item.HexHolderItem;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.common.lib.HexSounds;
import com.zurrtum.create.content.kinetics.belt.behaviour.BeltProcessingBehaviour;
import com.zurrtum.create.content.kinetics.belt.behaviour.TransportedItemStackHandlerBehaviour;
import com.zurrtum.create.content.kinetics.belt.transport.TransportedItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaNetProcessor;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaPack;
import pub.pigeon.yggdyy.hexcreating.api.iotanet.IotaTerminal;

import java.util.List;

/**
 * 传送带处理行为：物品经过读取器时按其 iota 大小累积“读取进度”，
 * 完成后把 iota 发送给上/左/右三个相邻方块。
 */
public class IotaReadBehaviour extends BeltProcessingBehaviour {
    private static final String TAG_KEY = "hexcreating_read";
    private static final String NEED_READ_TIME_KEY = "need_read_time";
    private static final String HAVE_READ_TIME_KEY = "have_read_time";
    private static final String TARGET_IOTA_KEY = "target_iota";
    /** 每个 iota 的基础工作量：32 RPM 下约 1 秒完成；转速越高越快。 */
    private static final long BASE_WORK_TICKS = 32L * 20L;

    private final IotaReaderBlockEntity reader;
    private long needReadTime;
    private long haveReadTime;
    @Nullable
    private Iota targetIota;

    public IotaReadBehaviour(IotaReaderBlockEntity be) {
        super(be);
        this.reader = be;
        whenItemEnters(this::itemEnter);
        whileItemHeld(this::itemHeld);
    }

    @Nullable
    public Iota getTargetIota() {
        return this.targetIota;
    }

    private ProcessingResult itemEnter(TransportedItemStack transported,
        TransportedItemStackHandlerBehaviour handler) {
        Level level = this.reader.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return ProcessingResult.PASS;
        }
        ItemStack stack = transported.stack;
        if (stack.getItem() instanceof HexHolderItem item) {
            if (!item.hasHex(stack)) {
                return ProcessingResult.PASS;
            }
            List<Iota> list = item.getHex(stack, serverLevel);
            if (list == null || list.isEmpty()) {
                return ProcessingResult.PASS;
            }
            this.targetIota = list.size() == 1 ? list.get(0) : new ListIota(list);
            this.haveReadTime = 0L;
            this.needReadTime = BASE_WORK_TICKS * this.targetIota.size();
            level.playSound(null, this.reader.getBlockPos(), HexSounds.CAST_HERMES, SoundSource.BLOCKS, 1f, 1f);
            this.reader.sync();
            return ProcessingResult.HOLD;
        }
        if (stack.getItem() instanceof IotaHolderItem item) {
            Iota iota = item.readIota(stack, serverLevel);
            if (iota == null) {
                return ProcessingResult.PASS;
            }
            this.targetIota = iota;
            this.haveReadTime = 0L;
            this.needReadTime = BASE_WORK_TICKS * this.targetIota.size();
            level.playSound(null, this.reader.getBlockPos(), HexSounds.CAST_THOTH, SoundSource.BLOCKS, 1f, 1f);
            this.reader.sync();
            return ProcessingResult.HOLD;
        }
        return ProcessingResult.PASS;
    }

    private ProcessingResult itemHeld(TransportedItemStack transported,
        TransportedItemStackHandlerBehaviour handler) {
        Level level = this.reader.getLevel();
        if (level == null) {
            return ProcessingResult.PASS;
        }
        if (this.targetIota == null) {
            return ProcessingResult.PASS;
        }
        double speed = Math.abs(this.reader.getSpeed());
        if (speed <= 0.0) {
            // 没有旋转动力时不工作。
            return ProcessingResult.HOLD;
        }
        // 进度随转速线性增长：转速越高，读取越快。
        this.haveReadTime += Math.max(1L, Math.round(speed));
        if (this.haveReadTime < this.needReadTime) {
            this.spawnWorkingParticles();
            this.reader.sync();
            return ProcessingResult.HOLD;
        }

        if (level instanceof ServerLevel serverLevel) {
            IotaPack pack = IotaPack.createNew(this.targetIota);
            IotaTerminal from = new IotaTerminal(this.reader, 0);
            for (BlockPos target : this.reader.getCanSendIotaPackToPos()) {
                IotaNetProcessor.trySendIotaPack(pack.copyWithSameId(), from, target, serverLevel);
            }
            level.playSound(null, this.reader.getBlockPos(), HexSounds.READ_LORE_FRAGMENT,
                SoundSource.BLOCKS, 1f, 1f);
        }
        this.haveReadTime = 0L;
        this.needReadTime = 0L;
        this.targetIota = null;
        this.reader.sync();
        return ProcessingResult.PASS;
    }

    private void spawnWorkingParticles() {
        if (!(this.reader.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.getGameTime() % 4L != 0L) {
            return;
        }
        BlockPos pos = this.reader.getBlockPos();
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
            pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5,
            2, 0.3, 0.15, 0.3, 0.02);
    }

    @Override
    public void read(ValueInput input, boolean clientPacket) {
        super.read(input, clientPacket);
        CompoundTag tag = input.read(TAG_KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new);
        this.haveReadTime = tag.getLongOr(HAVE_READ_TIME_KEY, 0L);
        this.needReadTime = tag.getLongOr(NEED_READ_TIME_KEY, 0L);
        this.targetIota = null;
        if (!clientPacket && this.reader.getLevel() instanceof ServerLevel serverLevel) {
            tag.getCompound(TARGET_IOTA_KEY)
                .ifPresent(saved -> this.targetIota = IotaType.deserialize(saved, serverLevel));
        }
    }

    @Override
    public void write(ValueOutput output, boolean clientPacket) {
        super.write(output, clientPacket);
        CompoundTag tag = new CompoundTag();
        tag.putLong(HAVE_READ_TIME_KEY, this.haveReadTime);
        tag.putLong(NEED_READ_TIME_KEY, this.needReadTime);
        if (!clientPacket && this.targetIota != null) {
            tag.put(TARGET_IOTA_KEY, IotaType.serialize(this.targetIota));
        }
        output.store(TAG_KEY, CompoundTag.CODEC, tag);
    }
}
