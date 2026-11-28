package at.petrak.hexcasting.neoforge

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.api.HexAPI.modLoc
import at.petrak.hexcasting.api.advancements.HexAdvancementTriggers
import at.petrak.hexcasting.api.mod.HexStatistics
import at.petrak.hexcasting.common.blocks.behavior.HexComposting
import at.petrak.hexcasting.common.blocks.behavior.HexStrippables
import at.petrak.hexcasting.common.casting.PatternRegistryManifest
import at.petrak.hexcasting.common.command.PatternResLocArgument
import at.petrak.hexcasting.neoforge.attachment.NeoForgeHexAttachments
import at.petrak.hexcasting.common.casting.actions.spells.LifeEffectField
import at.petrak.hexcasting.common.casting.actions.spells.OpFlight
import at.petrak.hexcasting.common.casting.actions.spells.ThreeSunsAdriftField
import at.petrak.hexcasting.common.casting.actions.spells.domain.AreaField
import at.petrak.hexcasting.common.casting.actions.spells.domain.InfinityVoidField
import at.petrak.hexcasting.common.casting.actions.spells.great.OpAltiora
import at.petrak.hexcasting.common.entities.HexEntities
import at.petrak.hexcasting.common.items.ItemJewelerHammer
import at.petrak.hexcasting.common.lib.HexAttributes
import at.petrak.hexcasting.common.lib.HexBlockEntities
import at.petrak.hexcasting.common.lib.HexBlocks
import at.petrak.hexcasting.common.lib.HexBlockSetTypes
import at.petrak.hexcasting.common.lib.HexCreativeTabs
import at.petrak.hexcasting.common.lib.HexItemHolderHandlers
import at.petrak.hexcasting.common.lib.HexItems
import at.petrak.hexcasting.common.lib.HexLootFunctions
import at.petrak.hexcasting.common.lib.HexMobEffects
import at.petrak.hexcasting.common.lib.HexParticles
import at.petrak.hexcasting.common.lib.HexPotions
import at.petrak.hexcasting.common.lib.HexRegistries
import at.petrak.hexcasting.common.lib.HexSounds
import at.petrak.hexcasting.common.lib.hex.HexActions
import at.petrak.hexcasting.common.lib.hex.HexArithmetics
import at.petrak.hexcasting.common.lib.hex.HexContinuationTypes
import at.petrak.hexcasting.common.lib.hex.HexEvalSounds
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import at.petrak.hexcasting.common.lib.hex.HexSpecialHandlers
import at.petrak.hexcasting.common.misc.AkashicTreeGrower
import at.petrak.hexcasting.common.misc.BrainsweepingEvents
import at.petrak.hexcasting.common.misc.PlayerPositionRecorder
import at.petrak.hexcasting.common.misc.RegisterMisc
import at.petrak.hexcasting.common.recipe.HexRecipeStuffRegistry
import at.petrak.hexcasting.interop.HexInterop
import at.petrak.hexcasting.neoforge.network.NeoForgePacketHandler
import at.petrak.hexcasting.neoforge.registry.NeoForgeHexRegistries
import at.petrak.hexcasting.xplat.IXplatAbstractions
import gay.`object`.hexdebug.actions.HexDebugActions
import gay.`object`.hexdebug.actions.OpScheduleCall
import gay.`object`.hexdebug.media.HexDebugAttributes
import gay.`object`.hexdebug.HexDebugRegistry
import gay.`object`.hexdebug.media.PersonalManaHolder
import gay.`object`.hexdebug.media.PersonalMediaRegen
import miyucomics.hexcassettes.HexCassetteItems
import miyucomics.hexcassettes.HexCassetteSounds
import miyucomics.hexcassettes.actions.HexCassetteActions
import miyucomics.hexcassettes.advancements.CassetteTrigger
import miyucomics.hexcassettes.cassetteState
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat
import pub.pigeon.yggdyy.hexcreating.HexCreatingRegistration
import pub.pigeon.yggdyy.hexcreating.blocks.HexCreatingBlockEntities
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
import ram.talia.moreiotas.common.lib.hex.MoreIotasActions
import ram.talia.moreiotas.common.lib.hex.MoreIotasArithmetics
import ram.talia.moreiotas.common.lib.hex.MoreIotasIotaTypes
import ram.talia.moreiotas.neoforge.NeoForgeMoreIotasXplat
import net.neoforged.neoforge.event.ServerChatEvent
import net.minecraft.commands.synchronization.ArgumentTypeInfos
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.ModContainer
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.neoforge.common.NeoForge
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.event.RegisterCommandsEvent
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent
import net.neoforged.neoforge.event.server.ServerStartedEvent
import net.neoforged.neoforge.event.server.ServerStartingEvent
import net.neoforged.neoforge.event.server.ServerStoppingEvent
import net.neoforged.neoforge.event.tick.LevelTickEvent
import net.neoforged.neoforge.event.tick.ServerTickEvent
import net.neoforged.neoforge.registries.NeoForgeRegistries
import net.neoforged.neoforge.registries.RegisterEvent
import java.util.function.BiConsumer

@Mod(HexAPI.MOD_ID)
class NeoForgeHexInitializer(modBus: IEventBus, modContainer: ModContainer) {
    init {
        CONFIG = NeoForgeHexConfig.setup()

        // 自定义注册表必须先建立，后续 Common 的类初始化才能拿到它们。
        NeoForgeHexRegistries.attach(modBus)
        NeoForgePacketHandler.init(modBus)

        modBus.addListener(RegisterEvent::class.java, ::onRegister)
        modBus.addListener(FMLCommonSetupEvent::class.java) { commonSetup() }
        modBus.addListener(EntityAttributeModificationEvent::class.java, ::onAttributeModification)
        modBus.addListener(BuildCreativeModeTabContentsEvent::class.java, ::onBuildCreativeTab)

        modBus.addListener(RegisterCapabilitiesEvent::class.java) { event ->
            if (HexCreatingCompat.isCreateLoaded()) {
                // 领域咒核的媒质罐暴露为流体能力，供 Create 管道/注液器访问。
                event.registerBlockEntity(Capabilities.Fluid.BLOCK, HexCreatingBlockEntities.DOMAIN_CORE) { be, _ ->
                    be.tankHandler
                }
            }
        }

        if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient) {
            at.petrak.hexcasting.neoforge.client.NeoForgeClientSetup.register(modBus)
        }

        registerGameEvents()
    }

    private fun onRegister(event: RegisterEvent) {
        when (event.registryKey) {
            Registries.SOUND_EVENT -> {
                HexSounds.registerSounds(bind(event, Registries.SOUND_EVENT))
                HexCassetteSounds.registerSounds(bind(event, Registries.SOUND_EVENT))
            }
            Registries.BLOCK -> {
                HexBlocks.registerBlocks(bind(event, Registries.BLOCK))
                HexDebugRegistry.registerBlocks(bind(event, Registries.BLOCK))
                if (HexCreatingCompat.isCreateLoaded()) {
                    HexCreatingRegistration.registerBlocks(bind(event, Registries.BLOCK))
                }
            }
            Registries.ITEM -> {
                HexBlocks.registerBlockItems(bind(event, Registries.ITEM))
                HexItems.registerItems(bind(event, Registries.ITEM))
                HexCassetteItems.registerItems(bind(event, Registries.ITEM))
                HexDebugRegistry.registerBlockItems(bind(event, Registries.ITEM))
                if (HexCreatingCompat.isCreateLoaded()) {
                    HexCreatingRegistration.registerBlockItems(bind(event, Registries.ITEM))
                    HexCreatingRegistration.registerItems(bind(event, Registries.ITEM))
                }
            }
            Registries.FLUID -> {
                if (HexCreatingCompat.isCreateLoaded()) {
                    HexCreatingRegistration.registerFluids(bind(event, Registries.FLUID))
                }
            }
            NeoForgeRegistries.Keys.FLUID_TYPES -> {
                if (HexCreatingCompat.isCreateLoaded()) {
                    HexCreatingRegistration.registerFluidTypes(bind(event, NeoForgeRegistries.Keys.FLUID_TYPES))
                }
            }
            Registries.BLOCK_ENTITY_TYPE -> {
                HexBlockEntities.registerTiles(bind(event, Registries.BLOCK_ENTITY_TYPE))
                HexDebugRegistry.registerBlockEntities(bind(event, Registries.BLOCK_ENTITY_TYPE))
                if (HexCreatingCompat.isCreateLoaded()) {
                    HexCreatingRegistration.registerBlockEntities(bind(event, Registries.BLOCK_ENTITY_TYPE))
                }
            }
            Registries.MENU -> HexDebugRegistry.registerMenus(bind(event, Registries.MENU))
            Registries.CREATIVE_MODE_TAB ->
                HexCreativeTabs.registerCreativeTabs(bind(event, Registries.CREATIVE_MODE_TAB))
            Registries.ENTITY_TYPE -> HexEntities.registerEntities(bind(event, Registries.ENTITY_TYPE))
            Registries.ATTRIBUTE -> {
                HexAttributes.register(bind(event, Registries.ATTRIBUTE))
                HexDebugAttributes.register(bind(event, Registries.ATTRIBUTE))
            }
            Registries.MOB_EFFECT -> HexMobEffects.register(bind(event, Registries.MOB_EFFECT))
            Registries.POTION -> HexPotions.register(bind(event, Registries.POTION))
            Registries.PARTICLE_TYPE -> HexParticles.registerParticles(bind(event, Registries.PARTICLE_TYPE))
            Registries.RECIPE_SERIALIZER ->
                HexRecipeStuffRegistry.registerSerializers(bind(event, Registries.RECIPE_SERIALIZER))
            Registries.RECIPE_TYPE -> HexRecipeStuffRegistry.registerTypes(bind(event, Registries.RECIPE_TYPE))
            Registries.COMMAND_ARGUMENT_TYPE ->
                event.register(Registries.COMMAND_ARGUMENT_TYPE, modLoc("pattern_res_loc")) { PATTERN_ARG_INFO }
            NeoForgeRegistries.Keys.ATTACHMENT_TYPES -> {
                event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, modLoc("flight")) {
                    NeoForgeHexAttachments.FLIGHT
                }
                event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, modLoc("sentinel")) {
                    NeoForgeHexAttachments.SENTINEL
                }
                event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, modLoc("altiora")) {
                    NeoForgeHexAttachments.ALTIORA
                }
                event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, modLoc("pigment")) {
                    NeoForgeHexAttachments.PIGMENT
                }
                event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, modLoc("staffcast_image")) {
                    NeoForgeHexAttachments.STAFFCAST_IMAGE
                }
                event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, modLoc("patterns")) {
                    NeoForgeHexAttachments.PATTERNS
                }
                event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, modLoc("brainswept")) {
                    NeoForgeHexAttachments.BRAINSWEPT
                }
                event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, modLoc("cassettes")) {
                    NeoForgeHexAttachments.CASSETTES
                }
                event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, modLoc("chat_prefix")) {
                    NeoForgeHexAttachments.CHAT_PREFIX
                }
            }
            Registries.LOOT_FUNCTION_TYPE ->
                HexLootFunctions.registerSerializers(bind(event, Registries.LOOT_FUNCTION_TYPE))
            net.minecraft.core.registries.BuiltInRegistries.TRIGGER_TYPES.key() -> {
                HexAdvancementTriggers.registerTriggers()
                CassetteTrigger.register()
            }
            Registries.CUSTOM_STAT -> HexStatistics.register()
            HexRegistries.ACTION -> {
                HexActions.register(bind(event, HexRegistries.ACTION))
                HexDebugActions.register(bind(event, HexRegistries.ACTION))
                MoreIotasActions.register(bind(event, HexRegistries.ACTION))
                HexCassetteActions.register(bind(event, HexRegistries.ACTION))
                if (HexCreatingCompat.isCreateLoaded()) {
                    HexCreatingRegistration.registerActions(bind(event, HexRegistries.ACTION))
                }
            }
            HexRegistries.IOTA_TYPE -> {
                HexIotaTypes.registerTypes(bind(event, HexRegistries.IOTA_TYPE))
                MoreIotasIotaTypes.registerTypes(bind(event, HexRegistries.IOTA_TYPE))
                if (HexCreatingCompat.isCreateLoaded()) {
                    HexCreatingRegistration.registerIotaTypes(bind(event, HexRegistries.IOTA_TYPE))
                }
            }
            HexRegistries.SPECIAL_HANDLER ->
                HexSpecialHandlers.register(bind(event, HexRegistries.SPECIAL_HANDLER))
            HexRegistries.ARITHMETIC -> {
                HexArithmetics.register(bind(event, HexRegistries.ARITHMETIC))
                MoreIotasArithmetics.register(bind(event, HexRegistries.ARITHMETIC))
            }
            HexRegistries.CONTINUATION_TYPE ->
                HexContinuationTypes.registerContinuations(bind(event, HexRegistries.CONTINUATION_TYPE))
            HexRegistries.EVAL_SOUND -> HexEvalSounds.register(bind(event, HexRegistries.EVAL_SOUND))
            // Create 的自定义注册表：只在 Create 存在时处理，且用 id 比较避免提前加载 Create 类。
            else -> if (HexCreatingCompat.isCreateLoaded() &&
                event.registryKey.identifier() == Identifier.fromNamespaceAndPath("create", "display_source")) {
                pub.pigeon.yggdyy.hexcreating.create.HexCreatingDisplaySources.registerDisplaySources(event)
            }
        }
    }

    private fun commonSetup() {
        // 自定义命令参数类型必须登记到 ArgumentTypeInfos，否则玩家进服同步命令树时会抛
        // 「Unrecognized argument type」，表现为「无效的玩家数据」。
        ArgumentTypeInfos.registerByClass(PatternResLocArgument::class.java, PATTERN_ARG_INFO)

        HexBlockSetTypes.registerBlocks { BlockSetType.register(it) }
        HexComposting.setup()
        HexStrippables.init()
        HexItemHolderHandlers.init()
        AkashicTreeGrower.init()

        HexInterop.init()
        RegisterMisc.register()

        // hexcreating：Create 存在时才初始化 Create 侧集成；不存在时仅记录日志。
        HexCreatingCompat.init()

        // MoreIotas 的平台能力（聊天消息缓存、聊天前缀）
        ram.talia.moreiotas.xplat.IXplatAbstractions.INSTANCE = NeoForgeMoreIotasXplat
    }

    private fun onAttributeModification(event: EntityAttributeModificationEvent) {
        for (holder in listOf(
            HexAttributes.holder(HexAttributes.GRID_ZOOM),
            HexAttributes.holder(HexAttributes.SCRY_SIGHT),
            HexAttributes.holder(HexAttributes.FEEBLE_MIND),
            HexAttributes.holder(HexAttributes.MEDIA_CONSUMPTION_MODIFIER),
            HexAttributes.holder(HexAttributes.AMBIT_RADIUS),
            HexAttributes.holder(HexAttributes.SENTINEL_RADIUS),
            HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA),
            HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA_MAX),
            HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA_REGEN),
        )) {
            event.add(EntityType.PLAYER, holder)
        }
    }

    private fun onBuildCreativeTab(event: BuildCreativeModeTabContentsEvent) {
        val tab = event.tab
        HexBlocks.registerBlockCreativeTab({ block -> event.accept(block) }, tab)
        HexItems.registerItemCreativeTab(event, tab)
        if (tab == HexCreativeTabs.HEX) {
            event.accept(HexDebugRegistry.SPLICING_TABLE_ITEM)
            event.accept(HexCassetteItems.CASSETTE)
            if (HexCreatingCompat.isCreateLoaded()) {
                HexCreatingRegistration.addCreativeTab(object : CreativeModeTab.Output {
                    override fun accept(stack: ItemStack, visibility: CreativeModeTab.TabVisibility) {
                        event.accept(stack, visibility)
                    }
                })
            }
        }
    }

    private fun registerGameEvents() {
        val bus = NeoForge.EVENT_BUS

        bus.addListener(RegisterCommandsEvent::class.java) { event ->
            at.petrak.hexcasting.common.lib.HexCommands.register(event.dispatcher)
        }

        bus.addListener(ServerStartingEvent::class.java) { event -> OpScheduleCall.ResetQueue(event.server) }
        bus.addListener(ServerStartedEvent::class.java) { event ->
            if (!patternRegistryIsProcessed) {
                PatternRegistryManifest.processRegistry(event.server.overworld())
                patternRegistryIsProcessed = true
            }
            PersonalManaHolder.clearCaches()
        }
        bus.addListener(ServerStoppingEvent::class.java) {
            ThreeSunsAdriftField.clear()
            LifeEffectField.clear()
            AreaField.clear()
            InfinityVoidField.clear()
        }

        bus.addListener(ServerTickEvent.Pre::class.java) { event -> OpScheduleCall.ProcessQueue(event.server) }
        bus.addListener(ServerTickEvent.Post::class.java) { event ->
            // hexcassettes：每 tick 推进所有玩家的卡带队列
            for (player in event.server.playerList.players) {
                player.cassetteState.tick(player)
            }
        }
        bus.addListener(LevelTickEvent.Post::class.java) { event ->
            val level = event.level as? net.minecraft.server.level.ServerLevel ?: return@addListener
            PlayerPositionRecorder.updateAllPlayers(level)
            OpFlight.tickAllPlayers(level)
            OpAltiora.checkAllPlayers(level)
            ThreeSunsAdriftField.tick(level)
            LifeEffectField.tick(level)
            AreaField.tick(level)
            InfinityVoidField.tick(level)
            PersonalMediaRegen.tick(level)
        }

        bus.addListener(LivingDeathEvent::class.java) { event ->
            val entity = event.entity
            if (entity is net.minecraft.server.level.ServerPlayer) {
                AreaField.onOwnerDeath(entity)
                InfinityVoidField.onPlayerDeath(entity)
                val killer = event.source.entity as? net.minecraft.server.level.ServerPlayer
                if (killer != null && AreaField.consumeGrudge(entity.uuid, killer.uuid)) {
                    val holder = entity.level().server?.advancements?.get(modLoc("shinjuku_showdown"))
                    if (holder != null) {
                        killer.advancements.award(holder, "grant")
                    }
                }
            }
        }

        bus.addListener(PlayerEvent.PlayerLoggedOutEvent::class.java) { event ->
            val player = event.entity as? net.minecraft.server.level.ServerPlayer ?: return@addListener
            PersonalManaHolder.forget(player)
            OpScheduleCall.forgetPlayer(player)
        }

        bus.addListener(PlayerEvent.Clone::class.java) { event ->
            val oldPlayer = event.original as? net.minecraft.server.level.ServerPlayer ?: return@addListener
            val newPlayer = event.entity as? net.minecraft.server.level.ServerPlayer ?: return@addListener
            newPlayer.cassetteState.owned = oldPlayer.cassetteState.owned
            newPlayer.cassetteState.sync(newPlayer)
        }

        bus.addListener(PlayerInteractEvent.EntityInteract::class.java) { event ->
            val result = BrainsweepingEvents.interactWithBrainswept(
                event.entity, event.level, event.hand, event.target, null
            )
            if (result != InteractionResult.PASS) {
                event.isCanceled = true
                event.cancellationResult = result
            }
        }

        bus.addListener(PlayerInteractEvent.LeftClickBlock::class.java) { event ->
            if (ItemJewelerHammer.shouldFailToBreak(event.entity, event.level.getBlockState(event.pos), event.pos)) {
                event.isCanceled = true
            }
        }

        bus.addListener(ServerChatEvent::class.java) { event -> NeoForgeMoreIotasXplat.receiveChat(event) }
    }

    private fun <T : Any> bind(event: RegisterEvent, key: ResourceKey<out Registry<T>>): BiConsumer<T, Identifier> =
        BiConsumer { value, id -> event.register(key, id) { value } }

    companion object {
        /** 与 Fabric 侧 `SingletonArgumentInfo.contextFree` 保持一致，注册与类映射共用同一实例。 */
        private val PATTERN_ARG_INFO: SingletonArgumentInfo<PatternResLocArgument> =
            SingletonArgumentInfo.contextFree { PatternResLocArgument.id() }

        lateinit var CONFIG: NeoForgeHexConfig
            private set

        /** 与 Fabric 侧同名旗标：图案清单只需处理一次。 */
        @JvmStatic
        var patternRegistryIsProcessed: Boolean = false
    }
}
