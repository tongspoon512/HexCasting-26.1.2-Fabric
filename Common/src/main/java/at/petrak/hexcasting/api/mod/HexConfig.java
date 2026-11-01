package at.petrak.hexcasting.api.mod;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.misc.MediaConstants;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.Level;

import java.util.List;

public class HexConfig {
    public interface CommonConfigAccess {

        long dustMediaAmount();

        long shardMediaAmount();

        long chargedCrystalMediaAmount();

        double mediaToHealthRate();

        int cypherCooldown();

        int trinketCooldown();

        int artifactCooldown();

        long DEFAULT_DUST_MEDIA_AMOUNT = MediaConstants.DUST_UNIT;
        long DEFAULT_SHARD_MEDIA_AMOUNT = MediaConstants.SHARD_UNIT;
        long DEFAULT_CHARGED_MEDIA_AMOUNT = MediaConstants.CRYSTAL_UNIT;
        double DEFAULT_MEDIA_TO_HEALTH_RATE = 2 * MediaConstants.CRYSTAL_UNIT / 20.0;

        int DEFAULT_CYPHER_COOLDOWN = 8;
        int DEFAULT_TRINKET_COOLDOWN = 5;
        int DEFAULT_ARTIFACT_COOLDOWN = 3;

    }

    public interface ClientConfigAccess {
        boolean ctrlTogglesOffStrokeOrder();

        boolean disableInworldScrolling();

        boolean invertSpellbookScrollDirection();

        boolean invertAbacusScrollDirection();

        double gridSnapThreshold();

        boolean clickingTogglesDrawing();

        boolean alwaysShowListCommas();

        boolean advancedTooltipsShowsIotaNBT();
      
        boolean staticActiveSlates();

        /**
         * Multiplier on the distance between adjacent hex grid dots in the spellcasting screen.
         * 1.0 is the vanilla sizing; larger values spread the grid out, smaller values tighten it.
         */
        default double gridSpacing() {
            return DEFAULT_GRID_SPACING;
        }

        /**
         * Updates the grid spacing multiplier. Implementations that own a writable config file
         * should persist the value; read-only implementations may ignore it.
         */
        default void setGridSpacing(double spacing) {
        }

        boolean DEFAULT_CTRL_TOGGLES_OFF_STROKE_ORDER = false;
        boolean DEFAULT_DISABLE_INWORLD_SCROLLING = false;
        boolean DEFAULT_INVERT_SPELLBOOK_SCROLL = false;
        boolean DEFAULT_INVERT_ABACUS_SCROLL = false;
        double DEFAULT_GRID_SNAP_THRESHOLD = 0.5;
        double DEFAULT_GRID_SPACING = 1.0;
        boolean DEFAULT_CLICKING_TOGGLES_DRAWING = false;
        boolean DEFAULT_ALWAYS_SHOW_LIST_COMMAS = false;
        boolean DEFAULT_ADVANCED_TOOLTIPS_SHOWS_IOTA_NBT = false;
        boolean DEFAULT_STATIC_ACTIVE_SLATES = false;
    }

    public interface ServerConfigAccess {
        int opBreakHarvestLevelBecauseForgeThoughtItWasAGoodIdeaToImplementHarvestTiersUsingAnHonestToGodTopoSort();

        int maxOpCount();

        int maxSpellCircleLength();

        boolean isActionAllowed(Identifier actionID);

        boolean isActionAllowedInCircles(Identifier actionID);

        double getActionCostScaling(Identifier actionID);

        double globalCostScaling();

        boolean doesGreaterTeleportSplatItems();

        boolean doVillagersTakeOffenseAtMindMurder();

        // fun fact, although dimension keys are a RegistryHolder, they aren't a registry, so i can't do tags
        boolean canTeleportInThisDimension(ResourceKey<Level> dimension);

        boolean trueNameHasAmbit();

        double traderScrollChance();

        int DEFAULT_MAX_OP_COUNT = 100_000;
        int DEFAULT_MAX_SPELL_CIRCLE_LENGTH = 1024;
        int DEFAULT_OP_BREAK_HARVEST_LEVEL = 3;

        double DEFAULT_GLOBAL_COST_SCALING = 1.0;
        double DEFAULT_TRADER_SCROLL_CHANCE = 0.2;
        boolean DEFAULT_GREATER_TELEPORT_SPLATS_ITEMS = true;

        boolean DEFAULT_VILLAGERS_DISLIKE_MIND_MURDER = true;

        List<String> DEFAULT_DIM_TP_DENYLIST = List.of("twilightforest:twilight_forest");

        boolean DEFAULT_TRUE_NAME_HAS_AMBIT = true;

        // --- HexDebug personal media pool ---
        boolean DEFAULT_PERSONAL_MEDIA_POOL = true;
        int DEFAULT_PERSONAL_MEDIA_MAX = (int) (10 * MediaConstants.CRYSTAL_UNIT);
        int DEFAULT_PERSONAL_MEDIA_REGEN_STEP = (int) MediaConstants.SHARD_UNIT;
        int DEFAULT_PERSONAL_MEDIA_REGEN_INTERVAL = 20;
        boolean DEFAULT_PERSONAL_MEDIA_AFTER_ENLIGHTENED = true;
        boolean DEFAULT_ENABLES_MIND_ENV_ACTIONS = true;
        boolean DEFAULT_BYPASSES_SILENCED_ENV_RESTRICTIONS = false;

        // --- MoreIotas ---
        int DEFAULT_MAX_MATRIX_SIZE = 144;
        int MIN_MAX_MATRIX_SIZE = 3;
        int MAX_MAX_MATRIX_SIZE = 512;
        int DEFAULT_MAX_STRING_LENGTH = 1728;
        int MIN_MAX_STRING_LENGTH = 1;
        int MAX_MAX_STRING_LENGTH = 32768;
        double DEFAULT_SET_BLOCK_STRING_COST = 0.01;
        double DEFAULT_NAME_COST = 0.01;

        default int maxMatrixSize() {
            return DEFAULT_MAX_MATRIX_SIZE;
        }

        default int maxStringLength() {
            return DEFAULT_MAX_STRING_LENGTH;
        }

        default double setBlockStringCost() {
            return DEFAULT_SET_BLOCK_STRING_COST;
        }

        default double nameCost() {
            return DEFAULT_NAME_COST;
        }

        default ToolMaterial opBreakHarvestLevel() {
            return switch (this.opBreakHarvestLevelBecauseForgeThoughtItWasAGoodIdeaToImplementHarvestTiersUsingAnHonestToGodTopoSort()) {
                case 0 -> ToolMaterial.WOOD;
                case 1 -> ToolMaterial.STONE;
                case 2 -> ToolMaterial.IRON;
                case 3 -> ToolMaterial.DIAMOND;
                case 4 -> ToolMaterial.NETHERITE;
                default -> throw new RuntimeException("please only return a value in 0<=x<=4");
            };
        }

        default boolean personalMediaPool() {
            return DEFAULT_PERSONAL_MEDIA_POOL;
        }

        default int personalMediaMax() {
            return DEFAULT_PERSONAL_MEDIA_MAX;
        }

        default int personalMediaRegenStep() {
            return DEFAULT_PERSONAL_MEDIA_REGEN_STEP;
        }

        default int personalMediaRegenInterval() {
            return DEFAULT_PERSONAL_MEDIA_REGEN_INTERVAL;
        }

        default boolean personalMediaAfterEnlightened() {
            return DEFAULT_PERSONAL_MEDIA_AFTER_ENLIGHTENED;
        }

        default boolean enablesMindEnvActions() {
            return DEFAULT_ENABLES_MIND_ENV_ACTIONS;
        }

        default boolean bypassesSilencedEnvRestrictions() {
            return DEFAULT_BYPASSES_SILENCED_ENV_RESTRICTIONS;
        }
    }

    // Simple extensions for resource location configs
    public static boolean anyMatch(List<? extends String> keys, Identifier key) {
        for (String s : keys) {
            var rl = Identifier.tryParse(s);
            if (rl != null) {
                if (rl.equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean noneMatch(List<? extends String> keys, Identifier key) {
        return !anyMatch(keys, key);
    }

    public static boolean anyMatchResLoc(List<? extends Identifier> keys, Identifier key) {
        return keys.stream().anyMatch(key::equals);
    }

    // oh man this is aesthetically pleasing
    private static CommonConfigAccess common = null;
    private static ClientConfigAccess client = null;
    private static ServerConfigAccess server = null;

    public static CommonConfigAccess common() {
        return common;
    }

    public static ClientConfigAccess client() {
        return client;
    }

    public static ServerConfigAccess server() {
        return server;
    }

    public static void setCommon(CommonConfigAccess access) {
        if (common != null) {
            HexAPI.LOGGER.warn("CommonConfigAccess was replaced! Old {} New {}",
                common.getClass().getName(), access.getClass().getName());
        }
        common = access;
    }

    public static void setClient(ClientConfigAccess access) {
        if (client != null) {
            HexAPI.LOGGER.warn("ClientConfigAccess was replaced! Old {} New {}",
                client.getClass().getName(), access.getClass().getName());
        }
        client = access;
    }

    public static void setServer(ServerConfigAccess access) {
        if (server != null) {
            HexAPI.LOGGER.warn("ServerConfigAccess was replaced! Old {} New {}",
                server.getClass().getName(), access.getClass().getName());
        }
        server = access;
    }
}
