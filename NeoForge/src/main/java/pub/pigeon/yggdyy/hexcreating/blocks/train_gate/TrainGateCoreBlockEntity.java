package pub.pigeon.yggdyy.hexcreating.blocks.train_gate;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks;

/**
 * Stores the gate shape. Deliberately contains no portal/train teleport logic yet.
 */
public class TrainGateCoreBlockEntity extends HexBlockEntity {
    private static final String TAG_TO_POS = "to_pos";
    private static final String TAG_AXIS = "axis";
    private static final String TAG_WIDTH = "gate_width";
    private static final String TAG_HEIGHT = "gate_height";

    @Nullable
    public BlockPos toPos;
    @Nullable
    public Direction.Axis gateAxis;
    public int gateWidth;
    public int gateHeight;

    public TrainGateCoreBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.TRAIN_GATE_CORE, pos, state);
    }

    @Override
    protected void saveModData(CompoundTag tag) {
        if (this.toPos != null) {
            tag.putLong(TAG_TO_POS, this.toPos.asLong());
        }
        if (this.gateAxis != null) {
            tag.putString(TAG_AXIS, this.gateAxis.getSerializedName());
        }
        tag.putInt(TAG_WIDTH, this.gateWidth);
        tag.putInt(TAG_HEIGHT, this.gateHeight);
    }

    @Override
    protected void loadModData(CompoundTag tag) {
        this.toPos = tag.contains(TAG_TO_POS) ? BlockPos.of(tag.getLongOr(TAG_TO_POS, 0L)) : null;
        this.gateAxis = switch (tag.getStringOr(TAG_AXIS, "")) {
            case "x" -> Direction.Axis.X;
            case "z" -> Direction.Axis.Z;
            default -> null;
        };
        this.gateWidth = tag.getIntOr(TAG_WIDTH, 0);
        this.gateHeight = tag.getIntOr(TAG_HEIGHT, 0);
    }

    public boolean isInGate(BlockPos pos) {
        if (this.gateAxis == null || this.gateWidth <= 0 || this.gateHeight <= 0) {
            return false;
        }
        BlockPos core = this.getBlockPos();
        int half = (this.gateWidth - 1) / 2;
        if (pos.getY() <= core.getY() || pos.getY() > core.getY() + this.gateHeight) {
            return false;
        }
        if (this.gateAxis == Direction.Axis.X) {
            return pos.getZ() == core.getZ() && Math.abs(pos.getX() - core.getX()) <= half;
        }
        return pos.getX() == core.getX() && Math.abs(pos.getZ() - core.getZ()) <= half;
    }

    public boolean tryDestroyGate() {
        if (this.level == null || this.level.isClientSide() || this.gateAxis == null) {
            return false;
        }
        this.clearGateBlocks();
        if (this.toPos != null && this.level.getBlockEntity(this.toPos) instanceof TrainGateCoreBlockEntity other) {
            other.gateAxis = null;
            other.gateWidth = 0;
            other.gateHeight = 0;
            other.toPos = null;
            other.clearGateBlocks();
            other.sync();
        }
        this.toPos = null;
        this.sync();
        return true;
    }

    private void clearGateBlocks() {
        if (this.level == null || this.gateAxis == null) {
            return;
        }
        BlockPos core = this.getBlockPos();
        int half = (this.gateWidth - 1) / 2;
        for (int y = core.getY() + 1; y <= core.getY() + this.gateHeight; y++) {
            for (int offset = -half; offset <= half; offset++) {
                BlockPos gate = this.gateAxis == Direction.Axis.X
                    ? new BlockPos(core.getX() + offset, y, core.getZ())
                    : new BlockPos(core.getX(), y, core.getZ() + offset);
                if (this.level.getBlockState(gate).is(HexCreatingBlocks.TRAIN_GATE)) {
                    this.level.setBlock(gate, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        this.gateAxis = null;
        this.gateWidth = 0;
        this.gateHeight = 0;
    }

    public boolean canSetUpGate() {
        return this.availableAxisAndWidth() != null;
    }

    /**
     * Builds a gate in front of this core. No teleport/portal-track behaviour is installed.
     */
    public void setUpGate(@Nullable BlockPos other) {
        if (this.level == null || this.level.isClientSide() || !this.canSetUpGate()) {
            return;
        }
        this.tryDestroyGate();
        Direction.Axis axisAndWidth = this.availableAxisAndWidth();
        int width = this.availableWidth(axisAndWidth);
        int height = this.availableHeight(axisAndWidth, width);
        if (height <= 0) {
            return;
        }
        this.gateAxis = axisAndWidth;
        this.gateWidth = width;
        this.gateHeight = height;
        this.toPos = other;

        BlockPos core = this.getBlockPos();
        int half = (width - 1) / 2;
        for (int y = core.getY() + 1; y <= core.getY() + height; y++) {
            for (int offset = -half; offset <= half; offset++) {
                BlockPos gate = axisAndWidth == Direction.Axis.X
                    ? new BlockPos(core.getX() + offset, y, core.getZ())
                    : new BlockPos(core.getX(), y, core.getZ() + offset);
                this.level.setBlock(gate, HexCreatingBlocks.TRAIN_GATE.defaultBlockState()
                    .setValue(BlockStateProperties.HORIZONTAL_AXIS, axisAndWidth), 3);
            }
        }
        this.sync();
    }

    @Nullable
    private Direction.Axis availableAxisAndWidth() {
        if (this.level == null) {
            return null;
        }
        int x = this.availableWidth(Direction.Axis.X);
        int z = this.availableWidth(Direction.Axis.Z);
        Direction.Axis axis = x >= z ? Direction.Axis.X : Direction.Axis.Z;
        int width = Math.max(x, z);
        if (width < 3) {
            return null;
        }
        return this.availableHeight(axis, width) > 0 ? axis : null;
    }

    private int availableWidth(Direction.Axis axis) {
        for (int i = 1; i <= 10; i++) {
            BlockPos a = axis == Direction.Axis.X
                ? this.getBlockPos().offset(-i, 0, 0)
                : this.getBlockPos().offset(0, 0, -i);
            BlockPos b = axis == Direction.Axis.X
                ? this.getBlockPos().offset(i, 0, 0)
                : this.getBlockPos().offset(0, 0, i);
            boolean ok = this.level.getBlockState(a).is(HexCreatingBlocks.TRAIN_GATE_FRAME)
                && this.level.getBlockState(a.above()).isAir()
                && this.level.getBlockState(b).is(HexCreatingBlocks.TRAIN_GATE_FRAME)
                && this.level.getBlockState(b.above()).isAir();
            if (!ok) {
                return 2 * (i - 1) + 1;
            }
        }
        return 0;
    }

    private int availableHeight(Direction.Axis axis, int width) {
        int half = (width - 1) / 2;
        for (int y = 1; y <= width + 1; y++) {
            for (int offset = -half; offset <= half; offset++) {
                BlockPos Pos = axis == Direction.Axis.X
                    ? this.getBlockPos().offset(offset, y, 0)
                    : this.getBlockPos().offset(0, y, offset);
                if (!this.level.getBlockState(Pos).isAir()) {
                    return y - 1;
                }
            }
        }
        return width + 1;
    }

    public static void destroyGateAt(Level level, BlockPos gatePos) {
        if (level.isClientSide()) {
            return;
        }
        BlockState state = level.getBlockState(gatePos);
        if (!state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            return;
        }
        Direction.Axis axis = state.getValue(BlockStateProperties.HORIZONTAL_AXIS);
        int bottomY = gatePos.getY();
        while (level.getBlockState(new BlockPos(gatePos.getX(), bottomY, gatePos.getZ()))
            .is(HexCreatingBlocks.TRAIN_GATE)) {
            bottomY--;
        }
        int radius = 10;
        if (axis == Direction.Axis.X) {
            for (int x = gatePos.getX() - radius; x <= gatePos.getX() + radius; x++) {
                checkCore(level, new BlockPos(x, bottomY, gatePos.getZ()), gatePos);
            }
        } else {
            for (int z = gatePos.getZ() - radius; z <= gatePos.getZ() + radius; z++) {
                checkCore(level, new BlockPos(gatePos.getX(), bottomY, z), gatePos);
            }
        }
    }

    private static void checkCore(Level level, BlockPos corePos, BlockPos gatePos) {
        if (level.getBlockEntity(corePos) instanceof TrainGateCoreBlockEntity core && core.isInGate(gatePos)) {
            core.tryDestroyGate();
        }
    }
}
