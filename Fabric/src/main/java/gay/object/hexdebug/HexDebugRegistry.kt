package gay.`object`.hexdebug

import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlock
import gay.`object`.hexdebug.blocks.splicing.SplicingTableBlockEntity
import gay.`object`.hexdebug.gui.splicing.SplicingTableMenu
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.flag.FeatureFlags
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.MapColor
import java.util.function.BiConsumer

/**
 * Registry holders for the spliced-in HexDebug content. Registered from
 * [at.petrak.hexcasting.fabric.FabricHexInitializer] so everything lands in the big
 * shared registry pass instead of a second mod initializer.
 */
object HexDebugRegistry {
    val SPLICING_TABLE_ID = HexDebug.id("splicing_table")

    lateinit var SPLICING_TABLE: Block
        private set
    lateinit var SPLICING_TABLE_ITEM: Item
        private set
    lateinit var SPLICING_TABLE_TILE: BlockEntityType<SplicingTableBlockEntity>
        private set
    lateinit var SPLICING_TABLE_MENU: MenuType<SplicingTableMenu>
        private set

    fun registerBlocks(reg: BiConsumer<Block, Identifier>) {
        val key = ResourceKey.create(Registries.BLOCK, SPLICING_TABLE_ID)
        SPLICING_TABLE = SplicingTableBlock(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .sound(SoundType.WOOD)
                .strength(2f)
                .setId(key)
        )
        reg.accept(SPLICING_TABLE, SPLICING_TABLE_ID)
    }

    fun registerBlockItems(reg: BiConsumer<Item, Identifier>) {
        val key = ResourceKey.create(Registries.ITEM, SPLICING_TABLE_ID)
        SPLICING_TABLE_ITEM = BlockItem(
            SPLICING_TABLE,
            Item.Properties().setId(key)
        )
        reg.accept(SPLICING_TABLE_ITEM, SPLICING_TABLE_ID)
    }

    fun registerBlockEntities(reg: BiConsumer<BlockEntityType<*>, Identifier>) {
        SPLICING_TABLE_TILE = IXplatAbstractions.INSTANCE.createBlockEntityType(
            { pos, state -> SplicingTableBlockEntity(pos, state) },
            SPLICING_TABLE
        )
        reg.accept(SPLICING_TABLE_TILE, SPLICING_TABLE_ID)
    }

    fun registerMenus(reg: BiConsumer<MenuType<*>, Identifier>) {
        SPLICING_TABLE_MENU = MenuType(
            MenuType.MenuSupplier { containerId, inventory -> SplicingTableMenu(containerId, inventory) },
            FeatureFlags.VANILLA_SET
        )
        reg.accept(SPLICING_TABLE_MENU, SPLICING_TABLE_ID)
    }
}
