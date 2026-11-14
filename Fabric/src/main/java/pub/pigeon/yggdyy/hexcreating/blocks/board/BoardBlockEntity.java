package pub.pigeon.yggdyy.hexcreating.blocks.board;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import com.mojang.serialization.DataResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

public class BoardBlockEntity extends HexBlockEntity implements Container {
    public static final int SLOTS = 16;
    private static final String TAG_SQUARES = "squares";

    private final NonNullList<ItemStack> squares = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public BoardBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.BOARD, pos, state);
    }

    public ItemStack getSquare(int slot) {
        return this.squares.get(slot);
    }

    public void setSquare(int slot, ItemStack stack) {
        this.squares.set(slot, stack);
        this.sync();
    }

    public ItemStack removeSquare(int slot) {
        ItemStack stack = this.squares.get(slot);
        this.squares.set(slot, ItemStack.EMPTY);
        this.sync();
        return stack;
    }

    @Override
    protected void saveModData(CompoundTag tag) {
        CompoundTag list = new CompoundTag();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = this.squares.get(i);
            if (!stack.isEmpty()) {
                int slot = i;
                DataResult<net.minecraft.nbt.Tag> result = ItemStack.CODEC.encodeStart(NbtOps.INSTANCE, stack);
                result.result().ifPresent(saved -> list.put("slot_" + slot, saved));
            }
        }
        tag.put(TAG_SQUARES, list);
    }

    @Override
    protected void loadModData(CompoundTag tag) {
        CompoundTag list = tag.getCompoundOrEmpty(TAG_SQUARES);
        for (int i = 0; i < SLOTS; i++) {
            final int slot = i;
            this.squares.set(slot, ItemStack.EMPTY);
            list.getCompound("slot_" + slot).ifPresent(saved ->
                ItemStack.CODEC.parse(NbtOps.INSTANCE, saved)
                    .result()
                    .ifPresent(stack -> this.squares.set(slot, stack)));
        }
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return this.squares.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return this.squares.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(this.squares, slot, amount);
        if (!result.isEmpty()) {
            this.sync();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(this.squares, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.squares.set(slot, stack);
        this.sync();
    }

    @Override
    public boolean stillValid(Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        this.squares.clear();
        this.sync();
    }
}
