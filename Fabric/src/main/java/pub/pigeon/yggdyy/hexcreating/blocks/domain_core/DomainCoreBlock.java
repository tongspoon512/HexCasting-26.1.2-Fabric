package pub.pigeon.yggdyy.hexcreating.blocks.domain_core;

import com.zurrtum.create.foundation.block.IBE;
import com.zurrtum.create.infrastructure.fluids.FluidInventory;
import com.zurrtum.create.infrastructure.fluids.FluidInventoryProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities;

/**
 * 领域咒核。放入写着领域 iota 的物品后可绑定领域，并用媒质代替玩家维持它。
 */
public class DomainCoreBlock extends Block
    implements IBE<DomainCoreBlockEntity>, FluidInventoryProvider<DomainCoreBlockEntity> {
    public DomainCoreBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Class<DomainCoreBlockEntity> getBlockEntityClass() {
        return DomainCoreBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends DomainCoreBlockEntity> getBlockEntityType() {
        return HexCreatingBlockEntities.DOMAIN_CORE;
    }

    @Override
    public FluidInventory getFluidInventory(LevelAccessor level, BlockPos pos, BlockState state,
                                            DomainCoreBlockEntity blockEntity, Direction side) {
        return blockEntity.getTank();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return this.onBlockEntityUse(level, pos, blockEntity -> {
            ItemStack stored = blockEntity.removeItem();
            if (stored.isEmpty()) {
                return InteractionResult.PASS;
            }
            player.getInventory().placeItemBackInInventory(stored);
            return InteractionResult.SUCCESS;
        });
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, net.minecraft.world.InteractionHand hand,
                                           BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return this.onBlockEntityUse(level, pos, blockEntity -> {
            if (stack.isEmpty()) {
                ItemStack stored = blockEntity.removeItem();
                if (stored.isEmpty()) {
                    return InteractionResult.PASS;
                }
                player.getInventory().placeItemBackInInventory(stored);
                return InteractionResult.SUCCESS;
            }
            return blockEntity.insertItem(stack, player)
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
        });
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              @Nullable BlockEntity blockEntity, ItemStack tool) {
        if (blockEntity instanceof DomainCoreBlockEntity core) {
            ItemStack stored = core.removeItem();
            if (!stored.isEmpty()) {
                Block.popResource(level, pos, stored);
            }
        }
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
    }
}
