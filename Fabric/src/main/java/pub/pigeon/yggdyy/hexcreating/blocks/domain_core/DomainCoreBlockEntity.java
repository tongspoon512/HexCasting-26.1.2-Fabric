package pub.pigeon.yggdyy.hexcreating.blocks.domain_core;

import at.petrak.hexcasting.api.casting.area.AreaIota;
import at.petrak.hexcasting.api.casting.area.DomainMediaSource;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.MediaHelper;
import at.petrak.hexcasting.common.casting.actions.spells.domain.AreaField;
import com.zurrtum.create.api.behaviour.BlockEntityBehaviour;
import com.zurrtum.create.foundation.blockEntity.SmartBlockEntity;
import com.zurrtum.create.foundation.fluid.FluidTank;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;
import pub.pigeon.yggdyy.hexcreating.fluids.HexCreatingFluids;

import java.util.List;
import java.util.UUID;

/**
 * 领域咒核：存入一个写着领域 iota 的物品后，可以用液态或固态紫水晶媒质维持领域。
 */
public class DomainCoreBlockEntity extends SmartBlockEntity implements DomainMediaSource {
    /** 1 紫水晶粉 = 81 mB，所以 81000 mB = 1000 粉。 */
    public static final int CAPACITY_MB = 81_000;

    private static final String TAG_ITEM = "Area";
    private static final String TAG_DOMAIN = "Domain";

    private final FluidTank tank = new FluidTank(CAPACITY_MB);
    private ItemStack stored = ItemStack.EMPTY;
    @Nullable
    private UUID domainId;

    public DomainCoreBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.DOMAIN_CORE, pos, state);
    }

    public FluidTank getTank() {
        return this.tank;
    }

    public ItemStack getStored() {
        return this.stored;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour<?>> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        this.bindToDomain();
    }

    @Override
    public void setRemoved() {
        AreaField.INSTANCE.unbind(this.domainId, this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        AreaField.INSTANCE.unbind(this.domainId, this);
        super.onChunkUnloaded();
    }

    /**
     * Tries to put the held item into the core.
     */
    public boolean insertItem(ItemStack stack, net.minecraft.world.entity.player.Player player) {
        if (this.stored.isEmpty() && this.insertDomainItem(stack)) {
            stack.shrink(1);
            this.setChanged();
            return true;
        }
        if (stack.is(pub.pigeon.yggdyy.hexcreating.items.HexCreatingItems.MEDIA_BUCKET)) {
            if (this.fill(1000)) {
                if (!player.hasInfiniteMaterials()) {
                    stack.shrink(1);
                    player.getInventory().placeItemBackInInventory(
                        new ItemStack(net.minecraft.world.item.Items.BUCKET));
                }
                this.setChanged();
                return true;
            }
            return false;
        }
        if (MediaHelper.isMediaItem(stack)) {
            int space = CAPACITY_MB - this.tank.getFluid().getAmount();
            if (space <= 0) {
                return false;
            }
            long affordable = (long) space * 10_000L / 81L;
            long media = Math.min(MediaHelper.extractMedia(stack, -1, false, true), affordable);
            int mB = (int) (media * 81L / 10_000L);
            if (mB <= 0) {
                return false;
            }
            MediaHelper.extractMedia(stack, media, false, false);
            this.fill(mB);
            this.setChanged();
            return true;
        }
        return false;
    }

    private boolean insertDomainItem(ItemStack stack) {
        if (!(stack.getItem() instanceof IotaHolderItem holder)) {
            return false;
        }
        if (this.level == null) {
            return false;
        }
        Iota iota = holder.readIota(stack, (ServerLevel) this.level);
        if (!(iota instanceof AreaIota area)) {
            return false;
        }
        this.stored = stack.copyWithCount(1);
        this.domainId = area.getDomainId();
        this.bindToDomain();
        return true;
    }

    /**
     * Removes the stored item and returns it, closing any binding.
     */
    public ItemStack removeItem() {
        ItemStack result = this.stored;
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        AreaField.INSTANCE.unbind(this.domainId, this);
        this.stored = ItemStack.EMPTY;
        this.domainId = null;
        this.setChanged();
        return result;
    }

    public boolean hasStoredItem() {
        return !this.stored.isEmpty();
    }

    private void bindToDomain() {
        if (this.domainId != null && this.level != null && !this.level.isClientSide()) {
            AreaField.INSTANCE.bind(this.domainId, this);
        }
    }

    private boolean fill(int mB) {
        FluidStack fluid = this.tank.getFluid();
        if (!fluid.isEmpty() && !fluid.isOf(HexCreatingFluids.MEDIA)) {
            return false;
        }
        int space = CAPACITY_MB - fluid.getAmount();
        if (space < mB) {
            return false;
        }
        if (fluid.isEmpty()) {
            this.tank.setFluid(new FluidStack(HexCreatingFluids.MEDIA, mB));
        } else {
            fluid.setAmount(fluid.getAmount() + mB);
        }
        return true;
    }

    @Override
    public long extractMedia(long cost, boolean simulate) {
        FluidStack fluid = this.tank.getFluid();
        if (fluid.isEmpty() || !fluid.isOf(HexCreatingFluids.MEDIA)) {
            return 0;
        }
        long available = fluid.getAmount() * 10_000L / 81L;
        long taken = Math.min(cost, available);
        if (taken <= 0) {
            return 0;
        }
        int mB = (int) Math.ceil(taken * 81.0 / 10_000.0);
        if (simulate) {
            return taken;
        }
        fluid.setAmount(fluid.getAmount() - mB);
        if (fluid.getAmount() <= 0) {
            this.tank.setFluid(FluidStack.EMPTY);
        }
        this.setChanged();
        return taken;
    }

    @Override
    public void onDomainClosed() {
        this.domainId = null;
        this.setChanged();
    }

    @Override
    protected void write(ValueOutput output, boolean client) {
        super.write(output, client);
        if (!this.stored.isEmpty()) {
            output.store(TAG_ITEM, ItemStack.OPTIONAL_CODEC, this.stored);
        }
        if (this.domainId != null) {
            output.putString(TAG_DOMAIN, this.domainId.toString());
        }
        this.tank.write(output);
    }

    @Override
    protected void read(ValueInput input, boolean client) {
        super.read(input, client);
        this.stored = input.read(TAG_ITEM, ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY);
        String raw = input.getStringOr(TAG_DOMAIN, "");
        this.domainId = parseUuid(raw);
        this.tank.read(input);
        this.bindToDomain();
    }

    @Nullable
    private static UUID parseUuid(String raw) {
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
