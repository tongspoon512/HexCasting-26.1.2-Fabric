package at.petrak.hexcasting.neoforge.client

import at.petrak.hexcasting.api.HexAPI
import at.petrak.hexcasting.client.ClientTickCounter
import at.petrak.hexcasting.client.Keybinds
import at.petrak.hexcasting.client.RegisterClientStuff
import at.petrak.hexcasting.client.ShiftScrollListener
import at.petrak.hexcasting.client.entity.WallScrollRenderer
import at.petrak.hexcasting.client.color.IotaStorageTintSource
import at.petrak.hexcasting.client.gui.PatternTooltipComponent
import at.petrak.hexcasting.client.item.CustomDataTagModelProperty
import at.petrak.hexcasting.client.item.MediaModelProperty
import at.petrak.hexcasting.client.item.SpellbookStateModelProperty
import at.petrak.hexcasting.client.item.VariantModelProperty
import at.petrak.hexcasting.client.model.HexModelLayers
import at.petrak.hexcasting.client.render.HexAdditionalRenderers
import at.petrak.hexcasting.client.render.be.BlockEntityAkashicBookshelfRenderer
import at.petrak.hexcasting.client.render.be.BlockEntityQuenchedAllayRenderer
import at.petrak.hexcasting.client.render.be.BlockEntitySlateRenderer
import at.petrak.hexcasting.common.casting.PatternRegistryManifest
import at.petrak.hexcasting.common.entities.HexEntities
import at.petrak.hexcasting.common.lib.HexBlockEntities
import at.petrak.hexcasting.common.lib.HexParticles
import at.petrak.hexcasting.common.misc.PatternTooltip
import at.petrak.hexcasting.interop.HexInterop
import at.petrak.hexcasting.neoforge.NeoForgeHexInitializer
import at.petrak.hexcasting.neoforge.network.NeoForgePacketHandler
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.HexDebugRegistry
import gay.`object`.hexdebug.client.PersonalMediaHud
import gay.`object`.hexdebug.gui.splicing.SplicingTableScreen
import miyucomics.hexcassettes.client.CassetteScreen
import miyucomics.hexcassettes.network.MsgCassetteSyncRequestC2S
import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat
import pub.pigeon.yggdyy.hexcreating.client.HexCreatingClientCompat
import pub.pigeon.yggdyy.hexcreating.client.HexCreatingClientRegistration
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.neoforged.bus.api.IEventBus
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent
import net.neoforged.neoforge.client.event.ClientTickEvent
import net.neoforged.neoforge.client.event.EntityRenderersEvent
import net.neoforged.neoforge.client.event.InputEvent
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent
import net.neoforged.neoforge.client.event.RenderFrameEvent
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent
import net.neoforged.neoforge.common.NeoForge
import java.util.function.Function

/**
 * NeoForge 客户端入口。
 *
 * 只在物理客户端加载：注册按键、菜单屏幕、物品模型属性/tint、实体与方块实体渲染器、
 * HUD 覆盖层，以及每帧/每 tick 的客户端逻辑。
 */
object NeoForgeClientSetup {
    private val SCRYING_LENS_OVERLAY = HexAPI.modLoc("scrying_lens_overlay")
    private val PERSONAL_MEDIA_BAR = HexDebug.id("personal_media_bar")

    fun register(modBus: IEventBus) {
        modBus.addListener(RegisterKeyMappingsEvent::class.java) { event ->
            Keybinds.ALL_BINDS.forEach(event::register)
        }

        modBus.addListener(RegisterMenuScreensEvent::class.java) { event ->
            event.register(HexDebugRegistry.SPLICING_TABLE_MENU) { menu, inv, title ->
                SplicingTableScreen(menu, inv, title)
            }
        }

        // 26.1 的物品模型是数据驱动的；这些属性/tint 的 codec 必须注册，否则
        // assets/hexcasting/items/*.json 会解析失败，表现为物品没有贴图。
        modBus.addListener(RegisterRangeSelectItemModelPropertyEvent::class.java) { event ->
            event.register(HexAPI.modLoc("media"), MediaModelProperty.MEDIA_CODEC)
            event.register(HexAPI.modLoc("max_media"), MediaModelProperty.MAX_CODEC)
        }
        modBus.addListener(RegisterConditionalItemModelPropertyEvent::class.java) { event ->
            event.register(HexAPI.modLoc("variant"), VariantModelProperty.MAP_CODEC)
            event.register(HexAPI.modLoc("custom_data_tag"), CustomDataTagModelProperty.MAP_CODEC)
            event.register(HexAPI.modLoc("spellbook_state"), SpellbookStateModelProperty.MAP_CODEC)
        }
        modBus.addListener(RegisterColorHandlersEvent.ItemTintSources::class.java) { event ->
            event.register(HexAPI.modLoc("iota_storage_color"), IotaStorageTintSource.MAP_CODEC)
        }
        // 方块色调源（神秘书架按 iota 颜色染色）；物品色调已改为数据驱动 codec。
        modBus.addListener(RegisterColorHandlersEvent.BlockTintSources::class.java) { event ->
            RegisterClientStuff.registerColorProviders(
                { _, _ -> },
                { tint, block -> event.register(listOf(tint), block) },
            )
        }

        modBus.addListener(EntityRenderersEvent.RegisterRenderers::class.java) { event ->
            event.registerEntityRenderer(HexEntities.WALL_SCROLL) { WallScrollRenderer(it) }
            event.registerBlockEntityRenderer(HexBlockEntities.SLATE_TILE) { BlockEntitySlateRenderer(it) }
            event.registerBlockEntityRenderer(HexBlockEntities.AKASHIC_BOOKSHELF_TILE) {
                BlockEntityAkashicBookshelfRenderer(it)
            }
            event.registerBlockEntityRenderer(HexBlockEntities.QUENCHED_ALLAY_TILE) {
                BlockEntityQuenchedAllayRenderer(it)
            }
            event.registerBlockEntityRenderer(HexBlockEntities.QUENCHED_ALLAY_TILES_TILE) {
                BlockEntityQuenchedAllayRenderer(it)
            }
            event.registerBlockEntityRenderer(HexBlockEntities.QUENCHED_ALLAY_BRICKS_TILE) {
                BlockEntityQuenchedAllayRenderer(it)
            }
            event.registerBlockEntityRenderer(HexBlockEntities.QUENCHED_ALLAY_BRICKS_SMALL_TILE) {
                BlockEntityQuenchedAllayRenderer(it)
            }
            if (HexCreatingCompat.isCreateLoaded()) {
                HexCreatingClientRegistration.registerRenderers(event)
            }
        }

        modBus.addListener(RegisterFluidModelsEvent::class.java) { event ->
            if (HexCreatingCompat.isCreateLoaded()) {
                HexCreatingClientRegistration.registerFluidModels(event)
            }
        }

        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions::class.java) { event ->
            HexModelLayers.init { loc, defn -> event.registerLayerDefinition(loc, defn) }
        }

        modBus.addListener(RegisterGuiLayersEvent::class.java) { event ->
            event.registerAboveAll(SCRYING_LENS_OVERLAY) { graphics, delta ->
                HexAdditionalRenderers.overlayGui(graphics, delta.getGameTimeDeltaPartialTick(false))
            }
            event.registerAboveAll(PERSONAL_MEDIA_BAR) { graphics, _ ->
                PersonalMediaHud.render(graphics)
            }
        }

        modBus.addListener(RegisterClientTooltipComponentFactoriesEvent::class.java) { event ->
            event.register(PatternTooltip::class.java) { PatternTooltipComponent(it) }
        }

        // 26.1 的粒子 provider 由 ParticleResources 统一注册；NeoForge 对应事件为 RegisterParticleProvidersEvent。
        modBus.addListener(RegisterParticleProvidersEvent::class.java) { event ->
            HexParticles.FactoryHandler.registerFactories(object : HexParticles.FactoryHandler.Consumer {
                override fun <T : ParticleOptions> register(
                    type: ParticleType<T>,
                    constructor: Function<SpriteSet, ParticleProvider<T>>,
                ) {
                    event.registerSpriteSet(type) { sprites -> constructor.apply(sprites) }
                }
            })
        }

        modBus.addListener(FMLClientSetupEvent::class.java) {
            RegisterClientStuff.init()
            HexInterop.clientInit()
            HexCreatingClientCompat.init()
        }

        registerGameEvents()
    }

    private fun registerGameEvents() {
        val bus = NeoForge.EVENT_BUS

        bus.addListener(RenderFrameEvent.Pre::class.java) { event ->
            ClientTickCounter.renderTickStart(event.partialTick.getGameTimeDeltaPartialTick(false))
        }

        bus.addListener(SubmitCustomGeometryEvent::class.java) { event ->
            HexAdditionalRenderers.overlayLevel(event.poseStack, event.submitNodeCollector)
        }

        bus.addListener(ClientTickEvent.Post::class.java) {
            ClientTickCounter.clientTickEnd()
            Keybinds.clientTickEnd()
            ShiftScrollListener.clientTickEnd()

            val minecraft = Minecraft.getInstance()
            while (Keybinds.openCassettes.consumeClick()) {
                if (minecraft.screen == null) {
                    minecraft.setScreen(CassetteScreen())
                }
            }
        }

        bus.addListener(InputEvent.MouseScrollingEvent::class.java) { event ->
            if (ShiftScrollListener.onScrollInGameplay(event.scrollDeltaY)) {
                event.isCanceled = true
            }
        }

        bus.addListener(ClientPlayerNetworkEvent.LoggingIn::class.java) {
            if (!NeoForgeHexInitializer.patternRegistryIsProcessed) {
                PatternRegistryManifest.processRegistry(null)
                NeoForgeHexInitializer.patternRegistryIsProcessed = true
            }
            // 登录后向服务端请求一次磁带状态
            NeoForgePacketHandler.sendToServer(MsgCassetteSyncRequestC2S())
        }
    }
}
