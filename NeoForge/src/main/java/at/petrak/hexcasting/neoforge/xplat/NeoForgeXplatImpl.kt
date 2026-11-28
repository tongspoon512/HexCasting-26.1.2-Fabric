package at.petrak.hexcasting.neoforge.xplat

import at.petrak.hexcasting.api.addldata.ADHexHolder
import at.petrak.hexcasting.api.addldata.ADIotaHolder
import at.petrak.hexcasting.api.addldata.ADMediaHolder
import at.petrak.hexcasting.api.addldata.ADVariantItem
import at.petrak.hexcasting.api.addldata.ItemDelegatingEntityIotaHolder
import at.petrak.hexcasting.api.casting.ActionRegistryEntry
import at.petrak.hexcasting.api.casting.arithmetic.Arithmetic
import at.petrak.hexcasting.api.casting.castables.SpecialHandler
import at.petrak.hexcasting.api.casting.eval.ResolvedPattern
import at.petrak.hexcasting.api.casting.eval.env.StaffCastEnv
import at.petrak.hexcasting.api.casting.eval.sideeffects.EvalSound
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.eval.vm.ContinuationFrame
import at.petrak.hexcasting.api.casting.iota.DoubleIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.api.item.HexHolderItem
import at.petrak.hexcasting.api.item.IotaHolderItem
import at.petrak.hexcasting.api.item.MediaHolderItem
import at.petrak.hexcasting.api.item.PigmentItem
import at.petrak.hexcasting.api.item.VariantItem
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.mod.HexConfig
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.api.pigment.ColorProvider
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.api.player.AltioraAbility
import at.petrak.hexcasting.api.player.FlightAbility
import at.petrak.hexcasting.api.player.Sentinel
import at.petrak.hexcasting.common.entities.EntityWallScroll
import at.petrak.hexcasting.common.lib.HexBlocks
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.hexcasting.common.msgs.IMessage
import at.petrak.hexcasting.interop.pehkui.PehkuiInterop
import at.petrak.hexcasting.neoforge.addldata.NeoHexHolder
import at.petrak.hexcasting.neoforge.addldata.NeoItemIotaHolder
import at.petrak.hexcasting.neoforge.addldata.NeoItemMediaHolder
import at.petrak.hexcasting.neoforge.addldata.NeoStaticIotaHolder
import at.petrak.hexcasting.neoforge.addldata.NeoStaticMediaHolder
import at.petrak.hexcasting.neoforge.addldata.NeoVariantItem
import at.petrak.hexcasting.neoforge.attachment.NeoForgeHexAttachments
import at.petrak.hexcasting.neoforge.network.NeoForgePacketHandler
import at.petrak.hexcasting.neoforge.registry.NeoForgeHexRegistries
import at.petrak.hexcasting.xplat.DummyXplatAbstractions
import at.petrak.hexcasting.xplat.IXplatAbstractions
import at.petrak.hexcasting.xplat.IXplatTags
import at.petrak.hexcasting.xplat.Platform
import net.minecraft.advancements.criterion.ItemPredicate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.TagKey
import net.minecraft.util.TriState
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.decoration.ItemFrame
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.ToolMaterial
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition
import net.minecraft.world.level.storage.loot.predicates.MatchTool
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.fml.ModList
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.common.util.FakePlayerFactory
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.level.block.BreakBlockEvent
import net.neoforged.neoforge.transfer.fluid.FluidResource
import net.neoforged.neoforge.transfer.transaction.Transaction
import ram.talia.moreiotas.api.casting.iota.StringIota
import java.util.ArrayList
import java.util.function.BiFunction
import java.util.function.Supplier

// 注意：IXplatAbstractions 通过 ServiceLoader 实例化，实现类必须是带 public 无参构造器的普通类，
// 不能用 Kotlin object（其构造器是私有的）。
class NeoForgeXplatImpl : IXplatAbstractions by DummyXplatAbstractions() {
    private val BUCKET = 1000

    override fun platform(): Platform = Platform.NEOFORGE

    override fun isModPresent(id: String): Boolean = ModList.get().isLoaded(id)

    override fun isPhysicalClient(): Boolean = FMLEnvironment.getDist() == Dist.CLIENT

    override fun initPlatformSpecific() {
        // P3：附件/事件等 NeoForge 专属初始化在后续阶段接入。
    }

    override fun setBrainsweepAddlData(mob: Mob) {
        mob.getData(NeoForgeHexAttachments.BRAINSWEPT).brainswept = true
    }

    override fun isBrainswept(mob: Mob): Boolean =
        mob.getData(NeoForgeHexAttachments.BRAINSWEPT).brainswept

    override fun setPigment(target: Player, colorizer: FrozenPigment?): FrozenPigment? {
        val data = target.getData(NeoForgeHexAttachments.PIGMENT)
        val old = data.pigment
        data.pigment = colorizer ?: FrozenPigment.DEFAULT.get()
        return old
    }

    override fun getPigment(player: Player): FrozenPigment =
        player.getData(NeoForgeHexAttachments.PIGMENT).pigment

    override fun setSentinel(target: Player, sentinel: Sentinel?) {
        target.getData(NeoForgeHexAttachments.SENTINEL).sentinel = sentinel
    }

    override fun getSentinel(player: Player): Sentinel? =
        player.getData(NeoForgeHexAttachments.SENTINEL).sentinel

    override fun setFlight(target: ServerPlayer, flight: FlightAbility?) {
        target.getData(NeoForgeHexAttachments.FLIGHT).flight = flight
    }

    override fun getFlight(player: ServerPlayer): FlightAbility? =
        player.getData(NeoForgeHexAttachments.FLIGHT).flight

    override fun setAltiora(target: Player, altiora: AltioraAbility?) {
        target.getData(NeoForgeHexAttachments.ALTIORA).altiora = altiora
    }

    override fun getAltiora(player: Player): AltioraAbility? =
        player.getData(NeoForgeHexAttachments.ALTIORA).altiora

    override fun setStaffcastImage(target: ServerPlayer, image: CastingImage?) {
        target.getData(NeoForgeHexAttachments.STAFFCAST_IMAGE).image =
            image?.serializeToNbt() ?: CompoundTag()
    }

    override fun getStaffcastVM(player: ServerPlayer, hand: InteractionHand): CastingVM =
        CastingVM(
            player.getData(NeoForgeHexAttachments.STAFFCAST_IMAGE).toImage(player.level()),
            StaffCastEnv(player, hand)
        )

    override fun setPatterns(target: ServerPlayer, patterns: List<ResolvedPattern>) {
        target.getData(NeoForgeHexAttachments.PATTERNS).patterns = patterns
    }

    override fun getPatternsSavedInUi(player: ServerPlayer): List<ResolvedPattern> =
        player.getData(NeoForgeHexAttachments.PATTERNS).patterns

    override fun clearCastingData(player: ServerPlayer) {
        player.getData(NeoForgeHexAttachments.STAFFCAST_IMAGE).image = CompoundTag()
        player.getData(NeoForgeHexAttachments.PATTERNS).patterns = emptyList()
    }

    override fun getPehkuiApi(): PehkuiInterop.ApiAbstraction =
        throw UnsupportedOperationException("Pehkui interop not yet ported to NeoForge")

    override fun findMediaHolder(stack: ItemStack): ADMediaHolder? {
        val item = stack.item
        return when {
            item is MediaHolderItem -> NeoItemMediaHolder(stack)
            item === HexItems.AMETHYST_DUST -> NeoStaticMediaHolder(
                Supplier { HexConfig.common().dustMediaAmount() },
                ADMediaHolder.AMETHYST_DUST_PRIORITY, stack
            )
            item === Items.AMETHYST_SHARD -> NeoStaticMediaHolder(
                Supplier { HexConfig.common().shardMediaAmount() },
                ADMediaHolder.AMETHYST_SHARD_PRIORITY, stack
            )
            item === HexItems.CHARGED_AMETHYST -> NeoStaticMediaHolder(
                Supplier { HexConfig.common().chargedCrystalMediaAmount() },
                ADMediaHolder.CHARGED_AMETHYST_PRIORITY, stack
            )
            item === HexItems.QUENCHED_SHARD -> NeoStaticMediaHolder(
                Supplier { MediaConstants.QUENCHED_SHARD_UNIT },
                ADMediaHolder.QUENCHED_SHARD_PRIORITY, stack
            )
            item === HexBlocks.QUENCHED_ALLAY.asItem() -> NeoStaticMediaHolder(
                Supplier { MediaConstants.QUENCHED_BLOCK_UNIT },
                ADMediaHolder.QUENCHED_ALLAY_PRIORITY, stack
            )
            else -> null
        }
    }

    override fun findMediaHolder(player: ServerPlayer): ADMediaHolder? = null

    override fun findDataHolder(stack: ItemStack): ADIotaHolder? {
        val item = stack.item
        return when {
            item is IotaHolderItem -> NeoItemIotaHolder(stack)
            item === Items.PUMPKIN_PIE -> NeoStaticIotaHolder(stack) { s ->
                DoubleIota(Math.PI * s.count)
            }
            item === Items.WRITABLE_BOOK || item === Items.WRITTEN_BOOK ->
                NeoStaticIotaHolder(stack) { s -> bookPages(s) }
            else -> null
        }
    }

    override fun findDataHolder(entity: Entity): ADIotaHolder? = when (entity) {
        is ItemEntity -> ItemDelegatingEntityIotaHolder.ToItemEntity(entity)
        is ItemFrame -> ItemDelegatingEntityIotaHolder.ToItemFrame(entity)
        is EntityWallScroll -> ItemDelegatingEntityIotaHolder.ToWallScroll(entity)
        else -> null
    }

    override fun findHexHolder(stack: ItemStack): ADHexHolder? =
        if (stack.item is HexHolderItem) NeoHexHolder(stack) else null

    override fun findVariantHolder(stack: ItemStack): ADVariantItem? =
        if (stack.item is VariantItem) NeoVariantItem(stack) else null

    /** MoreIotas：把成书的每一页读成一个字符串 iota，整体作为字符串列表。 */
    private fun bookPages(stack: ItemStack): Iota? {
        stack.get(DataComponents.WRITABLE_BOOK_CONTENT)?.let { writable ->
            val list = ArrayList<Iota>()
            writable.getPages(false).forEach { list.add(StringIota.makeUnchecked(it)) }
            return ListIota(list)
        }
        stack.get(DataComponents.WRITTEN_BOOK_CONTENT)?.let { written ->
            val list = ArrayList<Iota>()
            written.getPages(false).forEach { list.add(StringIota.makeUnchecked(it.getString())) }
            return ListIota(list)
        }
        return null
    }

    override fun sendPacketToPlayer(target: net.minecraft.server.level.ServerPlayer, packet: IMessage) =
        NeoForgePacketHandler.sendToPlayer(target, packet)

    override fun sendPacketNear(pos: Vec3, radius: Double, dimension: ServerLevel, packet: IMessage) =
        NeoForgePacketHandler.sendPacketNear(pos, radius, dimension, packet)

    override fun sendPacketTracking(entity: net.minecraft.world.entity.Entity, packet: IMessage) =
        NeoForgePacketHandler.sendPacketTracking(entity, packet)

    override fun toVanillaClientboundPacket(message: IMessage): Packet<ClientGamePacketListener> =
        NeoForgePacketHandler.toVanillaClientboundPacket(message)

    override fun isPigment(stack: ItemStack): Boolean = stack.item is PigmentItem

    override fun getColorProvider(pigment: FrozenPigment): ColorProvider {
        val stack = pigment.item()
        val item = stack.item
        return if (item is PigmentItem) item.provideColor(stack, pigment.owner()) else ColorProvider.MISSING
    }

    override fun addEquipSlotFabric(slot: EquipmentSlot): Item.Properties =
        Item.Properties().equippable(slot)

    override fun <T : BlockEntity> createBlockEntityType(
        func: BiFunction<BlockPos, BlockState, T>,
        vararg blocks: Block
    ): BlockEntityType<T> =
        BlockEntityType(
            BlockEntityType.BlockEntitySupplier { pos, state -> func.apply(pos, state) },
            *blocks
        )

    override fun tryPlaceFluid(level: Level, hand: InteractionHand, pos: BlockPos, fluid: Fluid): Boolean {
        val handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP) ?: return false
        Transaction.openRoot().use { transaction ->
            if (handler.insert(FluidResource.of(fluid), BUCKET, transaction) > 0) {
                transaction.commit()
                return true
            }
        }
        return false
    }

    override fun drainAllFluid(level: Level, pos: BlockPos): Boolean {
        val handler = level.getCapability(Capabilities.Fluid.BLOCK, pos, Direction.UP) ?: return false
        Transaction.openRoot().use { transaction ->
            var any = false
            for (slot in 0 until handler.size()) {
                val amount = handler.getAmountAsInt(slot)
                if (amount <= 0) continue
                val resource = handler.getResource(slot)
                if (handler.extract(slot, resource, amount, transaction) > 0) {
                    any = true
                }
            }
            if (any) {
                transaction.commit()
                return true
            }
        }
        return false
    }

    override fun isCorrectTierForDrops(tier: ToolMaterial, bs: BlockState): Boolean {
        if (!bs.requiresCorrectToolForDrops()) return true
        // NeoForge 没有 Fabric 的采集等级约定，沿用配置里的等级做近似判断。
        val level = HexConfig.server()
            .opBreakHarvestLevelBecauseForgeThoughtItWasAGoodIdeaToImplementHarvestTiersUsingAnHonestToGodTopoSort()
        for (tool in HARVEST_TOOLS_BY_LEVEL.get().getOrNull(level) ?: emptyList()) {
            if (tool.isCorrectToolForDrops(bs)) return true
        }
        return false
    }

    override fun getUnsealedIngredient(stack: ItemStack): Ingredient = Ingredient.of(stack.item)

    private val TAGS = object : IXplatTags {
        override fun amethystDust(): TagKey<Item> =
            HexTags.Items.create(Identifier.fromNamespaceAndPath("c", "amethyst_dusts"))

        override fun gems(): TagKey<Item> =
            HexTags.Items.create(Identifier.fromNamespaceAndPath("c", "gems"))
    }

    override fun tags(): IXplatTags = TAGS

    override fun isShearsCondition(): LootItemCondition.Builder =
        MatchTool.toolMatches(ItemPredicate.Builder.item().of(BuiltInRegistries.ITEM, Items.SHEARS))

    override fun getModName(namespace: String): String {
        if (namespace == "c") return "Common"
        return ModList.get().getModContainerById(namespace)
            .map { it.modInfo.displayName }
            .orElse(namespace)
    }

    override fun getActionRegistry(): Registry<ActionRegistryEntry> = NeoForgeHexRegistries.action()

    override fun getSpecialHandlerRegistry(): Registry<SpecialHandler.Factory<*>> =
        NeoForgeHexRegistries.specialHandler()

    override fun getIotaTypeRegistry(): Registry<IotaType<*>> = NeoForgeHexRegistries.iotaType()

    override fun getArithmeticRegistry(): Registry<Arithmetic> = NeoForgeHexRegistries.arithmetic()

    override fun getContinuationTypeRegistry(): Registry<ContinuationFrame.Type<*>> =
        NeoForgeHexRegistries.continuationType()

    override fun getEvalSoundRegistry(): Registry<EvalSound> = NeoForgeHexRegistries.evalSound()

    override fun isBreakingAllowed(
        world: ServerLevel,
        pos: BlockPos,
        state: BlockState,
        player: Player?
    ): Boolean {
        val actor = player ?: FakePlayerFactory.getMinecraft(world)
        val event = BreakBlockEvent(world, pos, state, actor)
        NeoForge.EVENT_BUS.post(event)
        return !event.isCanceled
    }

    override fun isPlacingAllowed(
        world: ServerLevel,
        pos: BlockPos,
        blockStack: ItemStack,
        player: Player?
    ): Boolean {
        val actor = player ?: FakePlayerFactory.getMinecraft(world)
        val hit = BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false)
        val event = PlayerInteractEvent.RightClickBlock(actor, InteractionHand.MAIN_HAND, pos, hit)
        NeoForge.EVENT_BUS.post(event)
        return !event.isCanceled && event.useBlock != TriState.FALSE
    }

    /** 26.1 在物品注册表冻结后才绑定数据组件，这些模板必须延迟构造。 */
    private val HARVEST_TOOLS_BY_LEVEL: com.google.common.base.Supplier<List<List<ItemStack>>> =
        com.google.common.base.Suppliers.memoize {
            listOf(
                stacks(Items.WOODEN_PICKAXE, Items.WOODEN_AXE, Items.WOODEN_HOE, Items.WOODEN_SHOVEL, Items.WOODEN_SWORD),
                stacks(Items.STONE_PICKAXE, Items.STONE_AXE, Items.STONE_HOE, Items.STONE_SHOVEL, Items.STONE_SWORD),
                stacks(Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_HOE, Items.IRON_SHOVEL, Items.IRON_SWORD),
                stacks(Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_HOE, Items.DIAMOND_SHOVEL, Items.DIAMOND_SWORD),
                stacks(Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_HOE, Items.NETHERITE_SHOVEL, Items.NETHERITE_SWORD)
            )
        }

    private fun stacks(vararg items: Item): List<ItemStack> = items.map { ItemStack(it) }
}
