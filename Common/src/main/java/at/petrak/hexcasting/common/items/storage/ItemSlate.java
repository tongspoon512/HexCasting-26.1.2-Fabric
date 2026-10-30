package at.petrak.hexcasting.common.items.storage;

import at.petrak.hexcasting.annotations.SoftImplement;
import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.block.HexBlockEntity;
import at.petrak.hexcasting.api.casting.iota.Iota;
import at.petrak.hexcasting.api.casting.iota.PatternIota;
import at.petrak.hexcasting.api.casting.math.HexPattern;
import at.petrak.hexcasting.api.item.IotaHolderItem;
import at.petrak.hexcasting.api.utils.NBTHelper;
import at.petrak.hexcasting.client.gui.PatternTooltipComponent;
import at.petrak.hexcasting.common.blocks.circles.BlockEntitySlate;
import at.petrak.hexcasting.common.lib.HexBlockEntities;
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes;
import at.petrak.hexcasting.common.misc.PatternTooltip;
import at.petrak.hexcasting.interop.inline.InlinePatternData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

import static at.petrak.hexcasting.api.HexAPI.modLoc;

public class ItemSlate extends BlockItem implements IotaHolderItem {
    public static final Identifier WRITTEN_PRED = modLoc("written");

    public ItemSlate(Block pBlock, Properties pProperties) {
        super(pBlock, pProperties);
    }

    @Override
    public Component getName(ItemStack pStack) {
        var key = "block." + HexAPI.MOD_ID + ".slate." + (hasPattern(pStack) ? "written" : "blank");
        Component patternText = getPattern(pStack)
            .map(pat -> Component.literal(": ").append(new InlinePatternData(pat).asText(false)))
            .orElse(Component.literal(""));
        return Component.translatable(key).append(patternText);
    }

    // 26.1 moved the "BlockEntityTag" item NBT channel onto the BLOCK_ENTITY_DATA component, which is what
    // vanilla copies into the block entity when the item is placed.
    private static final DataComponentType<TypedEntityData<BlockEntityType<?>>> BE_DATA =
        DataComponents.BLOCK_ENTITY_DATA;

    private static @Nullable CompoundTag modDataTag(ItemStack stack) {
        var data = stack.get(BE_DATA);
        if (data == null) {
            return null;
        }
        return HexBlockEntity.unwrapModData(data.copyTagWithoutId()).orElse(null);
    }

    private static void setPattern(ItemStack stack, @Nullable HexPattern pattern) {
        if (pattern == null) {
            stack.remove(BE_DATA);
            return;
        }
        var modData = new CompoundTag();
        modData.put(BlockEntitySlate.TAG_PATTERN, pattern.serializeToNBT());
        var root = new CompoundTag();
        root.put(HexBlockEntity.MOD_DATA_KEY, modData);
        stack.set(BE_DATA, TypedEntityData.<BlockEntityType<?>>of(HexBlockEntities.SLATE_TILE, root));
    }

    public static Optional<HexPattern> getPattern(ItemStack stack) {
        var patTag = NBTHelper.getCompound(modDataTag(stack), BlockEntitySlate.TAG_PATTERN);

        if (patTag != null && !patTag.isEmpty()) {
            return Optional.of(HexPattern.fromNBT(patTag));
        }
        return Optional.empty();
    }

    public static boolean hasPattern(ItemStack stack) {
        return getPattern(stack).isPresent();
    }

    @SoftImplement("IForgeItem")
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!hasPattern(stack)) {
            stack.remove(BE_DATA);
        }
        return false;
    }

    @Override
    public void inventoryTick(ItemStack pStack, net.minecraft.server.level.ServerLevel pLevel, Entity pEntity,
        net.minecraft.world.entity.EquipmentSlot pSlot) {
        if (!hasPattern(pStack)) {
            pStack.remove(BE_DATA);
        }
    }

    @Override
    public @Nullable
    CompoundTag readIotaTag(ItemStack stack) {
        var patTag = NBTHelper.getCompound(modDataTag(stack), BlockEntitySlate.TAG_PATTERN);

        if (patTag == null || patTag.isEmpty()) {
            return null;
        }
        var out = new CompoundTag();
        out.putString(HexIotaTypes.KEY_TYPE, "hexcasting:pattern");
        out.put(HexIotaTypes.KEY_DATA, patTag);
        return out;
    }

    @Override
    public boolean writeable(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canWrite(ItemStack stack, Iota datum) {
        return datum instanceof PatternIota || datum == null;
    }

    @Override
    public void writeDatum(ItemStack stack, Iota datum) {
        if (this.canWrite(stack, datum)) {
            if (datum == null) {
                setPattern(stack, null);
            } else if (datum instanceof PatternIota pat) {
                setPattern(stack, pat.getPattern());
            }
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        return getPattern(stack).map(pat -> new PatternTooltip(pat, PatternTooltipComponent.SLATE_BG));
    }
}
