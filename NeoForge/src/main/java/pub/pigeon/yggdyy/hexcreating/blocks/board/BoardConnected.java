package pub.pigeon.yggdyy.hexcreating.blocks.board;

import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlocks;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A group of same-facing boards that touch each other. Slots are indexed in reading order over the
 * whole group (rows top to bottom, then columns), which is what the redstone typesetting
 * operations act on.
 */
public final class BoardConnected {
    public record SlotRef(BlockPos pos, int slot) {
    }

    public final List<BlockPos> blockPoses = new ArrayList<>();
    public final List<SlotRef> slots = new ArrayList<>();
    public final int condition;

    private final Level world;
    private final BlockState state;
    private final Vec3i x;
    private final Vec3i y;

    public BoardConnected(Level world, BlockPos pos, BlockState state) {
        this.world = world;
        this.state = state;
        this.x = axisX(state);
        this.y = axisY();
        dfs(pos);

        int power = 0;
        for (BlockPos boardPos : blockPoses) {
            for (int i = 0; i < BoardBlockEntity.SLOTS; i++) {
                slots.add(new SlotRef(boardPos, i));
            }
            power = Math.max(power, world.getBestNeighborSignal(boardPos));
        }
        this.condition = power;
        slots.sort(Comparator.comparingInt(this::row).thenComparingInt(this::column));
    }

    public static Vec3i axisX(BlockState state) {
        return switch (state.getValue(BoardBlock.FACING)) {
            case NORTH -> new Vec3i(-1, 0, 0);
            case SOUTH -> new Vec3i(1, 0, 0);
            case WEST -> new Vec3i(0, 0, 1);
            case EAST -> new Vec3i(0, 0, -1);
            default -> Vec3i.ZERO;
        };
    }

    public static Vec3i axisY() {
        return new Vec3i(0, -1, 0);
    }

    /**
     * Unit vector pointing into the wall the board is attached to.
     */
    public static Vec3i axisBehind(BlockState state) {
        return axisX(state).cross(axisY());
    }

    private void dfs(BlockPos pos) {
        if (blockPoses.contains(pos)) {
            return;
        }
        BlockState here = world.getBlockState(pos);
        if (!here.is(HexCreatingBlocks.BOARD) || here.getValue(BoardBlock.FACING) != state.getValue(BoardBlock.FACING)) {
            return;
        }
        blockPoses.add(pos);
        dfs(pos.offset(x));
        dfs(pos.offset(x.multiply(-1)));
        dfs(pos.offset(y));
        dfs(pos.offset(y.multiply(-1)));
    }

    private static int dot(BlockPos pos, Vec3i v) {
        return pos.getX() * v.getX() + pos.getY() * v.getY() + pos.getZ() * v.getZ();
    }

    private int row(SlotRef ref) {
        return dot(ref.pos(), y) * BoardBlockEntity.SLOTS / 4 + ref.slot() / 4;
    }

    private int column(SlotRef ref) {
        return dot(ref.pos(), x) * 4 + ref.slot() % 4;
    }

    public int getIndex(BlockPos pos, int slot) {
        for (int i = 0; i < slots.size(); i++) {
            SlotRef ref = slots.get(i);
            if (ref.pos().equals(pos) && ref.slot() == slot) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public SlotHandler getSlotHandler(int index) {
        if (index < 0 || index >= slots.size()) {
            return null;
        }
        return new SlotHandler(world, slots.get(index));
    }

    public boolean isEmpty(int index) {
        SlotHandler handler = getSlotHandler(index);
        return handler == null || handler.getStack().isEmpty();
    }

    @Nullable
    public Section getLeftAndRight(int index) {
        if (index < 0 || index >= slots.size()) {
            return null;
        }
        int l = index;
        int r = index;
        while (l >= 0 && !isEmpty(l)) {
            l--;
        }
        l++;
        while (r < slots.size() && !isEmpty(r)) {
            r++;
        }
        r--;
        return l <= r ? new Section(this, l, r) : null;
    }

    @Nullable
    public Section getLeft(int index) {
        if (index < 0 || index >= slots.size()) {
            return null;
        }
        int l = index;
        while (l >= 0 && !isEmpty(l)) {
            l--;
        }
        l++;
        return l <= index ? new Section(this, l, index) : null;
    }

    @Nullable
    public Section getRight(int index) {
        if (index < 0 || index >= slots.size()) {
            return null;
        }
        int r = index;
        while (r < slots.size() && !isEmpty(r)) {
            r++;
        }
        r--;
        return index <= r ? new Section(this, index, r) : null;
    }

    /**
     * Containers directly behind the connected boards, used as a source when pasting squares.
     */
    public List<Container> containersBehind() {
        List<Container> out = new ArrayList<>();
        Vec3i behind = axisBehind(state);
        for (BlockPos boardPos : blockPoses) {
            if (world.getBlockEntity(boardPos.offset(behind)) instanceof Container container) {
                out.add(container);
            }
        }
        return out;
    }

    public static final class Section {
        private final BoardConnected boards;
        public int l;
        public int r;

        private Section(BoardConnected boards, int l, int r) {
            this.boards = boards;
            if (r < l) {
                int tmp = l;
                l = r;
                r = tmp;
            }
            this.l = Math.max(0, l);
            this.r = Math.min(boards.slots.size() - 1, r);
        }

        public boolean canMoveRight() {
            return r + 1 < boards.slots.size() && boards.isEmpty(r + 1);
        }

        public boolean canMoveLeft() {
            return l - 1 >= 0 && boards.isEmpty(l - 1);
        }

        public void moveRight() {
            if (!canMoveRight()) {
                return;
            }
            for (int i = r + 1; i >= l + 1; i--) {
                boards.getSlotHandler(i).setStack(boards.getSlotHandler(i - 1).getStack());
            }
            boards.getSlotHandler(l).setStack(ItemStack.EMPTY);
            l++;
            r++;
        }

        public void moveLeft() {
            if (!canMoveLeft()) {
                return;
            }
            for (int i = l - 1; i <= r - 1; i++) {
                boards.getSlotHandler(i).setStack(boards.getSlotHandler(i + 1).getStack());
            }
            boards.getSlotHandler(r).setStack(ItemStack.EMPTY);
            l--;
            r--;
        }

        public List<Iota> getPatterns() {
            List<Iota> out = new ArrayList<>();
            for (int i = l; i <= r; i++) {
                SlotHandler handler = boards.getSlotHandler(i);
                if (handler == null) {
                    continue;
                }
                ItemStack stack = handler.getStack();
                if (stack.getItem() instanceof IotaHolderItem holder && boards.world instanceof ServerLevel level) {
                    Iota iota = holder.readIota(stack, level);
                    if (iota != null) {
                        out.add(iota);
                    }
                }
            }
            return out;
        }

        public int leftSlotRow() {
            return boards.getSlotHandler(l).slot / 4;
        }

        public int rightSlotRow() {
            return boards.getSlotHandler(r).slot / 4;
        }
    }

    public static final class SlotHandler {
        private final BoardBlockEntity be;
        public final int slot;

        private SlotHandler(Level world, SlotRef ref) {
            this.be = world.getBlockEntity(ref.pos()) instanceof BoardBlockEntity board ? board : null;
            this.slot = ref.slot();
        }

        public ItemStack getStack() {
            return be == null ? ItemStack.EMPTY : be.getSquare(slot);
        }

        public void setStack(ItemStack stack) {
            if (be != null) {
                be.setSquare(slot, stack);
            }
        }
    }
}
