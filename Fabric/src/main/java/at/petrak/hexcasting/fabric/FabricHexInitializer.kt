package at.petrak.hexcasting.fabric

import at.petrak.hexcasting.api.HexAPI.modLoc
import at.petrak.hexcasting.api.advancements.HexAdvancementTriggers
import at.petrak.hexcasting.api.mod.HexStatistics
import at.petrak.hexcasting.common.blocks.behavior.HexComposting
import at.petrak.hexcasting.common.blocks.behavior.HexStrippables
import at.petrak.hexcasting.common.casting.PatternRegistryManifest
import at.petrak.hexcasting.common.casting.actions.spells.OpFlight
import at.petrak.hexcasting.common.casting.actions.spells.great.OpAltiora
import at.petrak.hexcasting.common.command.PatternResLocArgument
import at.petrak.hexcasting.common.entities.HexEntities
import at.petrak.hexcasting.common.items.ItemJewelerHammer
import at.petrak.hexcasting.common.lib.*
import at.petrak.hexcasting.common.lib.hex.*
import at.petrak.hexcasting.common.misc.AkashicTreeGrower
import at.petrak.hexcasting.common.misc.BrainsweepingEvents
import at.petrak.hexcasting.common.misc.PlayerPositionRecorder
import at.petrak.hexcasting.common.misc.RegisterMisc
import at.petrak.hexcasting.common.recipe.HexRecipeStuffRegistry
import at.petrak.hexcasting.fabric.event.VillagerConversionCallback
import at.petrak.hexcasting.fabric.loot.FabricHexLootModJankery
import at.petrak.hexcasting.fabric.network.FabricPacketHandler
import at.petrak.hexcasting.fabric.storage.FabricImpetusStorage
import gay.`object`.hexdebug.HexDebugRegistry
import gay.`object`.hexdebug.actions.HexDebugActions
import gay.`object`.hexdebug.actions.OpScheduleCall
import gay.`object`.hexdebug.media.HexDebugAttributes
import gay.`object`.hexdebug.media.PersonalManaHolder
import gay.`object`.hexdebug.media.PersonalMediaRegen
import at.petrak.hexcasting.interop.HexInterop
import at.petrak.hexcasting.xplat.IXplatAbstractions
import miyucomics.hexcassettes.HexCassetteItems
import miyucomics.hexcassettes.HexCassetteSounds
import miyucomics.hexcassettes.actions.HexCassetteActions
import miyucomics.hexcassettes.advancements.CassetteTrigger
import miyucomics.hexcassettes.cassetteState
import ram.talia.moreiotas.fabric.FabricMoreIotasInitializer
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.event.player.AttackBlockCallback
import net.fabricmc.fabric.api.event.player.UseEntityCallback
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.fabric.api.loot.v3.LootTableEvents
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry
import net.fabricmc.fabric.api.`object`.builder.v1.entity.FabricDefaultAttributeRegistry
import net.minecraft.commands.synchronization.SingletonArgumentInfo
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.block.state.properties.BlockSetType
import java.util.function.BiConsumer

object FabricHexInitializer : ModInitializer {
    lateinit var CONFIG: FabricHexConfig

    override fun onInitialize() {
        this.CONFIG = FabricHexConfig.setup()
        FabricPacketHandler.init()

        initListeners()

        initRegistries()

        ArgumentTypeRegistry.registerArgumentType(
            modLoc("pattern"),
            PatternResLocArgument::class.java,
            SingletonArgumentInfo.contextFree { PatternResLocArgument.id() }
        )
        HexAdvancementTriggers.registerTriggers()
        CassetteTrigger.register()
        HexComposting.setup()
        HexStrippables.init()
        FabricImpetusStorage.registerStorage()

        HexInterop.init()
        RegisterMisc.register()
    }

    // this global is for both server and client because PatternRegistryManifest.processRegistry
    // doesn't have separate processing for each
    internal var patternRegistryIsProcessed: Boolean = false
    fun initListeners() {
        UseEntityCallback.EVENT.register(BrainsweepingEvents::interactWithBrainswept)
        VillagerConversionCallback.EVENT.register(BrainsweepingEvents::copyBrainsweepPostTransformation)
        AttackBlockCallback.EVENT.register { player, world, _, pos, _ ->
            // SUCCESS cancels further processing and, on the client, sends a packet to the server.
            // PASS falls back to further processing.
            // FAIL cancels further processing and does not send a packet to the server.
            if (ItemJewelerHammer.shouldFailToBreak(player, world.getBlockState(pos), pos)) {
                InteractionResult.SUCCESS // "success"
            } else {
                InteractionResult.PASS
            }
        }


        ServerLifecycleEvents.SERVER_STARTED.register { server ->
            if (!patternRegistryIsProcessed) {
                PatternRegistryManifest.processRegistry(server.overworld())
                patternRegistryIsProcessed = true
            }
        }

        ServerTickEvents.START_SERVER_TICK.register(OpScheduleCall::ProcessQueue)
        ServerLifecycleEvents.SERVER_STARTING.register(OpScheduleCall::ResetQueue)
        ServerTickEvents.END_LEVEL_TICK.register(PlayerPositionRecorder::updateAllPlayers)
        ServerTickEvents.END_LEVEL_TICK.register(OpFlight::tickAllPlayers)
        ServerTickEvents.END_LEVEL_TICK.register(OpAltiora::checkAllPlayers)
        ServerTickEvents.END_LEVEL_TICK.register { level -> PersonalMediaRegen.tick(level) }
        ServerPlayConnectionEvents.DISCONNECT.register { handler, _ -> PersonalManaHolder.forget(handler.player) }
        ServerLifecycleEvents.SERVER_STARTED.register { PersonalManaHolder.clearCaches() }
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register { _, _, _ -> PersonalManaHolder.clearCaches() }

        // hexcassettes: 每 tick 推进所有玩家的卡带队列；死亡后清空队列但保留槽位
        ServerTickEvents.END_SERVER_TICK.register { server ->
            for (player in server.playerList.players) {
                player.cassetteState.tick(player)
            }
        }
        ServerPlayerEvents.AFTER_RESPAWN.register { oldPlayer, newPlayer, _ ->
            newPlayer.cassetteState.owned = oldPlayer.cassetteState.owned
            newPlayer.cassetteState.sync(newPlayer)
        }

        CommandRegistrationCallback.EVENT.register { dp, _, _ -> HexCommands.register(dp) }

        LootTableEvents.MODIFY.register { id, builder, _, _ ->
            FabricHexLootModJankery.lootLoad(id.identifier()) { pool -> builder.withPool(pool) }
        }

        EntityElytraEvents.CUSTOM.register { target, _ ->
            if (target is Player) {
                val altiora = IXplatAbstractions.INSTANCE.getAltiora(target)
                altiora != null
            } else {
                false
            }
        }

        CreativeModeTabEvents.MODIFY_OUTPUT_ALL.register { tab, output ->
            HexBlocks.registerBlockCreativeTab({ block: net.minecraft.world.level.block.Block -> output.accept(block) }, tab)
            HexItems.registerItemCreativeTab(output, tab)
            if (tab == HexCreativeTabs.HEX) {
                output.accept(HexDebugRegistry.SPLICING_TABLE_ITEM)
                output.accept(HexCassetteItems.CASSETTE)
                if (pub.pigeon.yggdyy.hexcreating.HexCreatingCompat.isCreateLoaded()) {
                    pub.pigeon.yggdyy.hexcreating.HexCreatingRegistration.addCreativeTab(output)
                }
            }
        }
    }

    private fun initRegistries() {
        fabricOnlyRegistration()

        HexBlockSetTypes.registerBlocks(BlockSetType::register)

        HexCreativeTabs.registerCreativeTabs(bind(BuiltInRegistries.CREATIVE_MODE_TAB))

        HexSounds.registerSounds(bind(BuiltInRegistries.SOUND_EVENT))
        HexCassetteSounds.registerSounds(bind(BuiltInRegistries.SOUND_EVENT))
        HexBlocks.registerBlocks(bind(BuiltInRegistries.BLOCK))
        HexBlocks.registerBlockItems(bind(BuiltInRegistries.ITEM))
        HexBlockEntities.registerTiles(bind(BuiltInRegistries.BLOCK_ENTITY_TYPE))
        HexItems.registerItems(bind(BuiltInRegistries.ITEM))
        HexCassetteItems.registerItems(bind(BuiltInRegistries.ITEM))

        // HexCreating is a soft integration: nothing below is touched unless Create is installed.
        if (pub.pigeon.yggdyy.hexcreating.HexCreatingCompat.isCreateLoaded()) {
            pub.pigeon.yggdyy.hexcreating.HexCreatingRegistration.registerFluids(bind(BuiltInRegistries.FLUID))
            pub.pigeon.yggdyy.hexcreating.HexCreatingRegistration.registerBlocks(bind(BuiltInRegistries.BLOCK))
            pub.pigeon.yggdyy.hexcreating.HexCreatingRegistration.registerBlockItems(bind(BuiltInRegistries.ITEM))
            pub.pigeon.yggdyy.hexcreating.HexCreatingRegistration.registerItems(bind(BuiltInRegistries.ITEM))
            pub.pigeon.yggdyy.hexcreating.HexCreatingRegistration.registerBlockEntities(bind(BuiltInRegistries.BLOCK_ENTITY_TYPE))
            pub.pigeon.yggdyy.hexcreating.HexCreatingRegistration.registerActions(bind(IXplatAbstractions.INSTANCE.actionRegistry))
            pub.pigeon.yggdyy.hexcreating.HexCreatingRegistration.init()
        }

        HexDebugRegistry.registerBlocks(bind(BuiltInRegistries.BLOCK))
        HexDebugRegistry.registerBlockItems(bind(BuiltInRegistries.ITEM))
        HexDebugRegistry.registerBlockEntities(bind(BuiltInRegistries.BLOCK_ENTITY_TYPE))
        HexDebugRegistry.registerMenus(bind(BuiltInRegistries.MENU))

        HexEntities.registerEntities(bind(BuiltInRegistries.ENTITY_TYPE))
        HexAttributes.register(bind(BuiltInRegistries.ATTRIBUTE))
        HexDebugAttributes.register(bind(BuiltInRegistries.ATTRIBUTE))
        // 26.1 may bake the vanilla default attribute suppliers before mod initializers run, which
        // leaves our attributes as unregistered direct holders. Rebuild the player supplier now that
        // the attributes are really in the registry.
        FabricDefaultAttributeRegistry.register(EntityType.PLAYER, Player.createAttributes())
        HexMobEffects.register(bind(BuiltInRegistries.MOB_EFFECT))
        HexPotions.register(bind(BuiltInRegistries.POTION))

        HexRecipeStuffRegistry.registerSerializers(bind(BuiltInRegistries.RECIPE_SERIALIZER))
        HexRecipeStuffRegistry.registerTypes(bind(BuiltInRegistries.RECIPE_TYPE))

        HexParticles.registerParticles(bind(BuiltInRegistries.PARTICLE_TYPE))

        HexLootFunctions.registerSerializers(bind(BuiltInRegistries.LOOT_FUNCTION_TYPE))

        HexIotaTypes.registerTypes(bind(IXplatAbstractions.INSTANCE.iotaTypeRegistry))
        HexActions.register(bind(IXplatAbstractions.INSTANCE.actionRegistry))
        HexDebugActions.register(bind(IXplatAbstractions.INSTANCE.actionRegistry))
        HexCassetteActions.register(bind(IXplatAbstractions.INSTANCE.actionRegistry))
        HexSpecialHandlers.register(bind(IXplatAbstractions.INSTANCE.specialHandlerRegistry))
        HexArithmetics.register(bind(IXplatAbstractions.INSTANCE.arithmeticRegistry))
        HexContinuationTypes.registerContinuations(bind(IXplatAbstractions.INSTANCE.continuationTypeRegistry))
        HexEvalSounds.register(bind(IXplatAbstractions.INSTANCE.evalSoundRegistry))
        HexItemHolderHandlers.init()

        // MoreIotas
        FabricMoreIotasInitializer.init()
        FabricMoreIotasInitializer.initRegistries()

        // Because of Java's lazy-loading of classes, can't use Kotlin static initialization for
        // any calls that will eventually touch FeatureUtils.register(), as the growers here do,
        // unless the class is called in this initialization step.
        AkashicTreeGrower.init()

        // Done with soft implements in forge
        butYouCouldBeFire()

        HexStatistics.register()
    }

    // sorry lex (not sorry)
    private fun fabricOnlyRegistration() {
//        if (GravityApiInterop.isActive()) {
//            HexActions.make("interop/gravity/get",
//                ActionRegistryEntry(HexPattern.fromAngles("wawawddew", HexDir.NORTH_EAST), OpGetGravity))
//            HexActions.make("interop/gravity/set",
//                ActionRegistryEntry(HexPattern.fromAngles("wdwdwaaqw", HexDir.NORTH_WEST), OpChangeGravity))
//        }
    }

    private fun butYouCouldBeFire() {
        val flameOn = FlammableBlockRegistry.getDefaultInstance()
        for (log in listOf(
            HexBlocks.EDIFIED_LOG,
            HexBlocks.EDIFIED_LOG_AMETHYST,
            HexBlocks.EDIFIED_LOG_AVENTURINE,
            HexBlocks.EDIFIED_LOG_CITRINE,
            HexBlocks.EDIFIED_LOG_PURPLE,
            HexBlocks.STRIPPED_EDIFIED_LOG,
            HexBlocks.EDIFIED_WOOD,
            HexBlocks.STRIPPED_EDIFIED_LOG,
        )) {
            flameOn.add(log, 5, 5)
        }
        for (wood in listOf(
            HexBlocks.EDIFIED_PLANKS,
            HexBlocks.EDIFIED_PANEL,
            HexBlocks.EDIFIED_TILE,
            HexBlocks.EDIFIED_DOOR,
            HexBlocks.EDIFIED_TRAPDOOR,
            HexBlocks.EDIFIED_STAIRS,
            HexBlocks.EDIFIED_SLAB,
            HexBlocks.EDIFIED_FENCE,
            HexBlocks.EDIFIED_FENCE_GATE,
            HexBlocks.EDIFIED_SLAB,
            HexBlocks.EDIFIED_BUTTON,
            HexBlocks.EDIFIED_PRESSURE_PLATE,
        )) {
            flameOn.add(wood, 20, 5)
        }
        for (papery in listOf(
            HexBlocks.SCROLL_PAPER,
            HexBlocks.SCROLL_PAPER_LANTERN,
            HexBlocks.ANCIENT_SCROLL_PAPER,
            HexBlocks.ANCIENT_SCROLL_PAPER_LANTERN,

            )) {
            flameOn.add(papery, 100, 60)
        }
        for (leaves in listOf(
            HexBlocks.AMETHYST_EDIFIED_LEAVES,
            HexBlocks.AVENTURINE_EDIFIED_LEAVES,
            HexBlocks.CITRINE_EDIFIED_LEAVES,
        )) {
            flameOn.add(leaves, 60, 30)
        }
    }

    private fun <T : Any> bind(registry: Registry<T>): BiConsumer<T, Identifier> =
        BiConsumer<T, Identifier> { t, id -> Registry.register(registry, id, t) }
}
