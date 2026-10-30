package at.petrak.hexcasting.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Optional;

public abstract class HexBlockEntity extends BlockEntity {
    /**
     * The key our mod data lives under, both on disk and inside the {@code minecraft:block_entity_data} item
     * component. 26.1 replaced raw-{@code CompoundTag} block entity serialization with {@code ValueOutput}/
     * {@code ValueInput}, which has no "write this whole tag at the root" operation, so everything gets nested
     * under one key instead.
     */
    public static final String MOD_DATA_KEY = "hexcasting_data";

    public HexBlockEntity(BlockEntityType<?> pType, BlockPos pWorldPosition, BlockState pBlockState) {
        super(pType, pWorldPosition, pBlockState);
    }

    protected abstract void saveModData(CompoundTag tag);

    protected abstract void loadModData(CompoundTag tag);

    protected CompoundTag serializeModData() {
        CompoundTag tag = new CompoundTag();
        this.saveModData(tag);
        return tag;
    }

    /**
     * The full tag shape used on disk and by {@code minecraft:block_entity_data}, i.e. one level of nesting above
     * whatever {@link #saveModData} produces.
     */
    protected CompoundTag serializeWithModData() {
        CompoundTag root = new CompoundTag();
        root.put(MOD_DATA_KEY, this.serializeModData());
        return root;
    }

    /** Strips the nesting added by {@link #serializeWithModData}. */
    public static Optional<CompoundTag> unwrapModData(CompoundTag root) {
        return root.getCompound(MOD_DATA_KEY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        output.store(MOD_DATA_KEY, CompoundTag.CODEC, this.serializeModData());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        this.loadModData(input.read(MOD_DATA_KEY, CompoundTag.CODEC).orElseGet(CompoundTag::new));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void sync() {
        this.setChanged();
        this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
    }

}
