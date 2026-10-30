package at.petrak.hexcasting.common.blocks.circles;

import at.petrak.hexcasting.api.block.HexBlockEntity;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.common.lib.HexBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class BlockEntitySlate extends HexBlockEntity {
    public static final String TAG_PATTERN = "pattern";

    @Nullable
    public HexPattern pattern;

    public BlockEntitySlate(BlockPos pos, BlockState state) {
        super(HexBlockEntities.SLATE_TILE, pos, state);
    }

    @Override
    protected void saveModData(CompoundTag tag) {
        if (this.pattern != null) {
            tag.put(TAG_PATTERN, this.pattern.serializeToNBT());
        }
    }

    @Override
    protected void loadModData(CompoundTag tag) {
        var patternTag = tag.getCompound(TAG_PATTERN);
        if (patternTag.isPresent() && HexPattern.isPattern(patternTag.get())) {
            this.pattern = HexPattern.fromNBT(patternTag.get());
        } else {
            this.pattern = null;
        }
    }

    // 26.1's copy_nbt loot function is gone; drops transfer block entity state through item components instead,
    // so we expose our pattern as minecraft:block_entity_data (the same channel BlockItem uses to restore it on
    // placement).
    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder builder) {
        super.collectImplicitComponents(builder);
        if (this.pattern != null) {
            builder.set(DataComponents.BLOCK_ENTITY_DATA,
                TypedEntityData.<BlockEntityType<?>>of(HexBlockEntities.SLATE_TILE, this.serializeWithModData()));
        }
    }

}
