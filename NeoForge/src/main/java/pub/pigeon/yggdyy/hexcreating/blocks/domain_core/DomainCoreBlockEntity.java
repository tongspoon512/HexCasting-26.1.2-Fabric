package pub.pigeon.yggdyy.hexcreating.blocks.domain_core;

import at.petrak.hexcasting.api.casting.area.AreaIota;
import at.petrak.hexcasting.api.casting.area.DomainMediaSource;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.MediaHelper;
import at.petrak.hexcasting.common.casting.actions.spells.domain.AreaField;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
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
    private static final String TAG_TANK_FLUID = "TankFluid";
    private static final String TAG_TANK_AMOUNT = "TankAmount";

    private final FluidTank tank = new FluidTank(CAPACITY_MB,
        stack -> stack.isEmpty() || stack.getFluid() == HexCreatingFluids.MEDIA);

    /**
     * 26.1 的流体能力类型是 ResourceHandler；Create 的管道通过该能力读写罐体。
     */
    private final ResourceHandler<FluidResource> tankHandler = new ResourceHandler<>() {
        @Override
        public int size() {
            return 1;
        }

        @Override
        public FluidResource getResource(int index) {
            return FluidResource.of(tank.getFluid());
        }

        @Override
        public long getAmountAsLong(int index) {
            return tank.getFluidAmount();
        }

        @Override
        public long getCapacityAsLong(int index, FluidResource resource) {
            return tank.getCapacity();
        }

        @Override
        public boolean isValid(int index, FluidResource resource) {
            return resource.isEmpty() || resource.getFluid() == HexCreatingFluids.MEDIA;
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || resource.isEmpty()) {
                return 0;
            }
            return tank.fill(new FluidStack(resource.getFluid(), amount), IFluidHandler.FluidAction.EXECUTE);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (amount <= 0 || resource.isEmpty()) {
                return 0;
            }
            FluidStack drained = tank.drain(new FluidStack(resource.getFluid(), amount),
                IFluidHandler.FluidAction.EXECUTE);
            return drained.getAmount();
        }
    };

    private ItemStack stored = ItemStack.EMPTY;
    @Nullable
    private UUID domainId;

    public DomainCoreBlockEntity(BlockPos pos, BlockState state) {
        super(HexCreatingBlockEntities.DOMAIN_CORE, pos, state);
    }

    public FluidTank getTank() {
        return this.tank;
    }

    public ResourceHandler<FluidResource> getTankHandler() {
        return this.tankHandler;
    }

    public ItemStack getStored() {
        return this.stored;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
    }

    @Override
    public void initialize() {
        super.initialize();
        this.bindToDomain();
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
            int space = CAPACITY_MB - this.tank.getFluidAmount();
            if (space <= 0) {
                return false;
            }
            long affordable = (long) space * 10_000L / 81L;
            long media = Math.min(MediaHelper.extractMedia(stack, -1, false, true), affordable);
            int mB = (int) (media * 81L / 10_000L);
            if (mB <= 0) {
                return false;
            }
            // 先确认罐体真的能接收，再扣除物品里的媒质，避免类型不符时媒质凭空消失
            if (!this.fill(mB)) {
                return false;
            }
            MediaHelper.extractMedia(stack, mB * 10_000L / 81L, false, false);
            this.setChanged();
            return true;
        }
        return false;
    }

    private boolean insertDomainItem(ItemStack stack) {
        if (!(stack.getItem() instanceof IotaHolderItem holder)) {
            return false;
        }
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return false;
        }
        Iota iota = holder.readIota(stack, serverLevel);
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
        if (!fluid.isEmpty() && fluid.getFluid() != HexCreatingFluids.MEDIA) {
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
        if (fluid.isEmpty() || fluid.getFluid() != HexCreatingFluids.MEDIA) {
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
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean client) {
        super.write(tag, registries, client);
        if (!this.stored.isEmpty()) {
            ItemStack.OPTIONAL_CODEC.encodeStart(NbtOps.INSTANCE, this.stored)
                .result().ifPresent(saved -> tag.put(TAG_ITEM, saved));
        }
        if (this.domainId != null) {
            tag.putString(TAG_DOMAIN, this.domainId.toString());
        }
        FluidStack fluid = this.tank.getFluid();
        if (!fluid.isEmpty()) {
            Identifier fluidId = BuiltInRegistries.FLUID.getKey(fluid.getFluid());
            tag.putString(TAG_TANK_FLUID, fluidId.toString());
            tag.putInt(TAG_TANK_AMOUNT, fluid.getAmount());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean client) {
        super.read(tag, registries, client);
        this.stored = ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, tag.get(TAG_ITEM))
            .result().orElse(ItemStack.EMPTY);
        this.domainId = parseUuid(tag.getStringOr(TAG_DOMAIN, ""));

        Identifier fluidId = Identifier.tryParse(tag.getStringOr(TAG_TANK_FLUID, ""));
        int amount = tag.getIntOr(TAG_TANK_AMOUNT, 0);
        if (fluidId != null && amount > 0) {
            BuiltInRegistries.FLUID.getOptional(fluidId)
                .ifPresent(fluid -> this.tank.setFluid(new FluidStack(fluid, amount)));
        } else {
            this.tank.setFluid(FluidStack.EMPTY);
        }
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
