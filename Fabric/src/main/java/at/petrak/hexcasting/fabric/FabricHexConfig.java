package at.petrak.hexcasting.fabric;

import at.petrak.hexcasting.api.HexAPI;
import at.petrak.hexcasting.api.mod.HexConfig;
import at.petrak.hexcasting.common.loot.HexLootHandler;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static at.petrak.hexcasting.api.mod.HexConfig.anyMatchResLoc;
import static at.petrak.hexcasting.api.mod.HexConfig.noneMatch;

@Config(name = HexAPI.MOD_ID)
@Config.Gui.Background("minecraft:textures/block/calcite.png")
@SuppressWarnings({"FieldMayBeFinal", "FieldCanBeLocal"})

public class FabricHexConfig extends PartitioningSerializer.GlobalData {
    @ConfigEntry.Category("common")
    @ConfigEntry.Gui.TransitiveObject
    public final Common common = new Common();
    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.TransitiveObject
    public final Client client = new Client();
    @ConfigEntry.Category("server")
    @ConfigEntry.Gui.TransitiveObject
    public final Server server = new Server();

    public static FabricHexConfig setup() {
        var gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Identifier.class, new TypeAdapter<Identifier>() {
                @Override
                public void write(JsonWriter out, Identifier value) throws IOException {
                    out.value(value.toString());
                }

                @Override
                public Identifier read(JsonReader in) throws IOException {
                    return Identifier.parse(in.nextString());
                }
            })
            .create();
        AutoConfig.register(FabricHexConfig.class, PartitioningSerializer.wrap((cfg, clazz) ->
            new GsonConfigSerializer<>(cfg, clazz, gson)));
        var instance = AutoConfig.getConfigHolder(FabricHexConfig.class).getConfig();

        HexConfig.setCommon(instance.common);
        // We care about the client only on the *physical* client ...
        if (IXplatAbstractions.INSTANCE.isPhysicalClient()) {
            HexConfig.setClient(instance.client);
        }
        // but we care about the server on the *logical* server
        // i believe this should Just Work without a guard? assuming we don't access it from the client ever
        HexConfig.setServer(instance.server);

        return instance;
    }

    @Config(name = "common")
    public static final class Common implements HexConfig.CommonConfigAccess, ConfigData {
        @ConfigEntry.Gui.Tooltip
        private long dustMediaAmount = DEFAULT_DUST_MEDIA_AMOUNT;
        @ConfigEntry.Gui.Tooltip
        private long shardMediaAmount = DEFAULT_SHARD_MEDIA_AMOUNT;
        @ConfigEntry.Gui.Tooltip
        private long chargedCrystalMediaAmount = DEFAULT_CHARGED_MEDIA_AMOUNT;
        @ConfigEntry.Gui.Tooltip
        private double mediaToHealthRate = DEFAULT_MEDIA_TO_HEALTH_RATE;

        @ConfigEntry.Gui.Tooltip
        private int cypherCooldown = DEFAULT_CYPHER_COOLDOWN;
        @ConfigEntry.Gui.Tooltip
        private int trinketCooldown = DEFAULT_TRINKET_COOLDOWN;
        @ConfigEntry.Gui.Tooltip
        private int artifactCooldown = DEFAULT_ARTIFACT_COOLDOWN;


        @Override
        public void validatePostLoad() throws ValidationException {
            this.dustMediaAmount = Math.max(this.dustMediaAmount, 0);
            this.shardMediaAmount = Math.max(this.shardMediaAmount, 0);
            this.chargedCrystalMediaAmount = Math.max(this.chargedCrystalMediaAmount, 0);
            this.mediaToHealthRate = Math.max(this.mediaToHealthRate, 0);
        }

        @Override
        public long dustMediaAmount() {
            return dustMediaAmount;
        }

        @Override
        public long shardMediaAmount() {
            return shardMediaAmount;
        }

        @Override
        public long chargedCrystalMediaAmount() {
            return chargedCrystalMediaAmount;
        }

        @Override
        public double mediaToHealthRate() {
            return mediaToHealthRate;
        }

        @Override
        public int cypherCooldown() {
            return cypherCooldown;
        }

        @Override
        public int trinketCooldown() {
            return trinketCooldown;
        }

        @Override
        public int artifactCooldown() {
            return artifactCooldown;
        }
    }

    @Config(name = "client")
    public static final class Client implements HexConfig.ClientConfigAccess, ConfigData {
        @ConfigEntry.Gui.Tooltip
        private boolean ctrlTogglesOffStrokeOrder = DEFAULT_CTRL_TOGGLES_OFF_STROKE_ORDER;
        @ConfigEntry.Gui.Tooltip
        private boolean disableInworldScrolling = DEFAULT_DISABLE_INWORLD_SCROLLING;
        @ConfigEntry.Gui.Tooltip
        private boolean invertSpellbookScrollDirection = DEFAULT_INVERT_SPELLBOOK_SCROLL;
        @ConfigEntry.Gui.Tooltip
        private boolean invertAbacusScrollDirection = DEFAULT_INVERT_ABACUS_SCROLL;
        @ConfigEntry.Gui.Tooltip
        private double gridSnapThreshold = DEFAULT_GRID_SNAP_THRESHOLD;
        @ConfigEntry.Gui.Tooltip
        private double gridSpacing = DEFAULT_GRID_SPACING;
        @ConfigEntry.Gui.Tooltip
        private boolean clickingTogglesDrawing = DEFAULT_CLICKING_TOGGLES_DRAWING;
        @ConfigEntry.Gui.Tooltip
        private boolean alwaysShowListCommas = DEFAULT_ALWAYS_SHOW_LIST_COMMAS;
        @ConfigEntry.Gui.Tooltip
        private boolean advancedTooltipsShowsIotaNBT = DEFAULT_ADVANCED_TOOLTIPS_SHOWS_IOTA_NBT;
        @ConfigEntry.Gui.Tooltip
        private boolean staticActiveSlates = DEFAULT_STATIC_ACTIVE_SLATES;

        @Override
        public void validatePostLoad() throws ValidationException {
            this.gridSnapThreshold = Mth.clamp(this.gridSnapThreshold, 0.5, 1.0);
            this.gridSpacing = Mth.clamp(this.gridSpacing, 0.5, 2.0);
        }

        @Override
        public boolean ctrlTogglesOffStrokeOrder() {
            return ctrlTogglesOffStrokeOrder;
        }

        @Override
        public boolean disableInworldScrolling() {
            return disableInworldScrolling;
        }

        @Override
        public boolean invertSpellbookScrollDirection() {
            return invertSpellbookScrollDirection;
        }

        @Override
        public boolean invertAbacusScrollDirection() {
            return invertAbacusScrollDirection;
        }

        @Override
        public double gridSnapThreshold() {
            return gridSnapThreshold;
        }

        @Override
        public double gridSpacing() {
            return gridSpacing;
        }

        @Override
        public void setGridSpacing(double spacing) {
            this.gridSpacing = Mth.clamp(spacing, 0.5, 2.0);
            AutoConfig.getConfigHolder(FabricHexConfig.class).save();
        }

        @Override
        public boolean clickingTogglesDrawing() {
             return clickingTogglesDrawing;
        }

        @Override
        public boolean alwaysShowListCommas() {
             return alwaysShowListCommas;
        }

        @Override
        public boolean advancedTooltipsShowsIotaNBT() { 
          return advancedTooltipsShowsIotaNBT; 
        }
      
        @Override
        public boolean staticActiveSlates() { 
          return staticActiveSlates; 
        }
    }

    @Config(name = "server")
    public static final class Server implements HexConfig.ServerConfigAccess, ConfigData {
        @ConfigEntry.BoundedDiscrete(min = 0, max = 4)
        @ConfigEntry.Gui.Tooltip
        private int opBreakHarvestLevel = DEFAULT_OP_BREAK_HARVEST_LEVEL;
        @ConfigEntry.Gui.Tooltip
        private int maxOpCount = DEFAULT_MAX_OP_COUNT;
        @ConfigEntry.Gui.Tooltip
        private int maxSpellCircleLength = DEFAULT_MAX_SPELL_CIRCLE_LENGTH;
        @ConfigEntry.Gui.Tooltip
        private List<String> actionDenyList = List.of();
        @ConfigEntry.Gui.Tooltip
        private List<String> circleActionDenyList = List.of();
        @ConfigEntry.Gui.Tooltip
        private List<String> costRescaleListRaw = List.of();
        private transient Object2DoubleMap<Identifier> costRescaleMap;
        @ConfigEntry.Gui.Tooltip
        private double globalCostScaling = DEFAULT_GLOBAL_COST_SCALING;
        @ConfigEntry.Gui.Tooltip
        private boolean greaterTeleportSplatsItems = DEFAULT_GREATER_TELEPORT_SPLATS_ITEMS;
        @ConfigEntry.Gui.Tooltip
        private boolean villagersOffendedByMindMurder = DEFAULT_VILLAGERS_DISLIKE_MIND_MURDER;
        @ConfigEntry.Gui.Tooltip
        private boolean doesTrueNameHaveAmbit = DEFAULT_TRUE_NAME_HAS_AMBIT;
        @ConfigEntry.Gui.Tooltip
        private List<String> tpDimDenylist = DEFAULT_DIM_TP_DENYLIST;
        
        @ConfigEntry.Gui.Tooltip
        private double traderScrollChance = DEFAULT_TRADER_SCROLL_CHANCE;

        // --- HexDebug personal media pool ---
        @ConfigEntry.Gui.Tooltip
        private boolean personalMediaPool = DEFAULT_PERSONAL_MEDIA_POOL;
        @ConfigEntry.Gui.Tooltip
        private int personalMediaMax = DEFAULT_PERSONAL_MEDIA_MAX;
        @ConfigEntry.Gui.Tooltip
        private int personalMediaRegenStep = DEFAULT_PERSONAL_MEDIA_REGEN_STEP;
        @ConfigEntry.Gui.Tooltip
        private int personalMediaRegenInterval = DEFAULT_PERSONAL_MEDIA_REGEN_INTERVAL;
        @ConfigEntry.Gui.Tooltip
        private boolean personalMediaAfterEnlightened = DEFAULT_PERSONAL_MEDIA_AFTER_ENLIGHTENED;
        @ConfigEntry.Gui.Tooltip
        private boolean enablesMindEnvActions = DEFAULT_ENABLES_MIND_ENV_ACTIONS;
        @ConfigEntry.Gui.Tooltip
        private boolean bypassesSilencedEnvRestrictions = DEFAULT_BYPASSES_SILENCED_ENV_RESTRICTIONS;

        // --- MoreIotas ---
        @ConfigEntry.BoundedDiscrete(min = MIN_MAX_MATRIX_SIZE, max = MAX_MAX_MATRIX_SIZE)
        @ConfigEntry.Gui.Tooltip
        private int maxMatrixSize = DEFAULT_MAX_MATRIX_SIZE;
        @ConfigEntry.BoundedDiscrete(min = MIN_MAX_STRING_LENGTH, max = MAX_MAX_STRING_LENGTH)
        @ConfigEntry.Gui.Tooltip
        private int maxStringLength = DEFAULT_MAX_STRING_LENGTH;
        @ConfigEntry.Gui.Tooltip
        private double setBlockStringCost = DEFAULT_SET_BLOCK_STRING_COST;
        @ConfigEntry.Gui.Tooltip
        private double nameCost = DEFAULT_NAME_COST;

        // ModMenu bad and doesn't like java objects in here so we do stupid string parsing
        @ConfigEntry.Gui.Tooltip
        private List<String> scrollInjectionsRaw = HexLootHandler.DEFAULT_SCROLL_INJECTS
            .stream()
            .map(si -> si.injectee() + " " + si.countRange())
            .toList();
        @ConfigEntry.Gui.Excluded
        private transient Object2IntMap<Identifier> scrollInjections;

        // TODO: hook this up to the config, change Jankery, test, also test scroll injects on fabric
        @ConfigEntry.Gui.Tooltip
        private List<String> loreInjectionsRaw = HexLootHandler.DEFAULT_LORE_INJECTS
                .stream()
                .map(Identifier::toString)
                .toList();
        @ConfigEntry.Gui.Excluded
        private transient List<Identifier> loreInjections;
        @ConfigEntry.Gui.Tooltip
        private double loreChance = HexLootHandler.DEFAULT_LORE_CHANCE;

        @ConfigEntry.Gui.Tooltip
        private List<String> cypherInjectionsRaw = HexLootHandler.DEFAULT_CYPHER_INJECTS
                .stream()
                .map(Identifier::toString)
                .toList();
        @ConfigEntry.Gui.Excluded
        private transient List<Identifier> cypherInjections;
        @ConfigEntry.Gui.Tooltip
        private double cypherChance = HexLootHandler.DEFAULT_CYPHER_CHANCE;


        @Override
        public void validatePostLoad() throws ValidationException {
            this.opBreakHarvestLevel = Mth.clamp(this.opBreakHarvestLevel, 0, 4);
            this.maxOpCount = Math.max(this.maxOpCount, 0);
            this.maxSpellCircleLength = Math.max(this.maxSpellCircleLength, 4);
            this.globalCostScaling = Math.max(this.globalCostScaling, 0);
            this.traderScrollChance = Mth.clamp(this.traderScrollChance, 0.0, 1.0);

            this.personalMediaMax = Math.max(this.personalMediaMax, 0);
            this.personalMediaRegenStep = Math.max(this.personalMediaRegenStep, 0);
            this.personalMediaRegenInterval = Math.max(this.personalMediaRegenInterval, 1);

            this.maxMatrixSize = Mth.clamp(this.maxMatrixSize, MIN_MAX_MATRIX_SIZE, MAX_MAX_MATRIX_SIZE);
            this.maxStringLength = Mth.clamp(this.maxStringLength, MIN_MAX_STRING_LENGTH, MAX_MAX_STRING_LENGTH);
            this.setBlockStringCost = Mth.clamp(this.setBlockStringCost, 0.0, Double.MAX_VALUE);
            this.nameCost = Mth.clamp(this.nameCost, 0.0, Double.MAX_VALUE);

            this.costRescaleMap = new Object2DoubleOpenHashMap<>();
            try {
                for (var auugh : this.costRescaleListRaw) {
                    String[] split = auugh.split(" ");
                    Identifier loc = Identifier.parse(split[0]);
                    double scale = Double.parseDouble(split[1]);
                    this.costRescaleMap.put(loc, scale);
                }
            } catch (Exception e) {
                throw new ValidationException("Bad parsing of action cost rescaling", e);
            }

            this.scrollInjections = new Object2IntOpenHashMap<>();
            try {
                for (var auugh : this.scrollInjectionsRaw) {
                    String[] split = auugh.split(" ");
                    Identifier loc = Identifier.parse(split[0]);
                    int count = Integer.parseInt(split[1]);
                    this.scrollInjections.put(loc, count);
                }
            } catch (Exception e) {
                throw new ValidationException("Bad parsing of scroll injects", e);
            }

            this.loreInjections = new ArrayList<>();
            try {
                for (var table : this.loreInjectionsRaw) {
                    Identifier loc = Identifier.parse(table);
                    this.loreInjections.add(loc);
                }
            } catch (Exception e) {
                throw new ValidationException("Bad parsing of lore injects", e);
            }

            this.loreChance = Mth.clamp(this.loreChance, 0.0, 1.0);

            this.cypherInjections = new ArrayList<>();
            try {
                for (var table : this.cypherInjectionsRaw) {
                    Identifier loc = Identifier.parse(table);
                    this.cypherInjections.add(loc);
                }
            } catch (Exception e) {
                throw new ValidationException("Bad parsing of cypher injects", e);
            }

            this.cypherChance = Mth.clamp(this.cypherChance, 0.0, 1.0);
        }

        @Override
        public int opBreakHarvestLevelBecauseForgeThoughtItWasAGoodIdeaToImplementHarvestTiersUsingAnHonestToGodTopoSort() {
            return opBreakHarvestLevel;
        }

        @Override
        public int maxOpCount() {
            return maxOpCount;
        }

        @Override
        public int maxSpellCircleLength() {
            return maxSpellCircleLength;
        }

        @Override
        public boolean isActionAllowed(Identifier actionID) {
            return noneMatch(actionDenyList, actionID);
        }

        @Override
        public boolean isActionAllowedInCircles(Identifier actionID) {
            return noneMatch(circleActionDenyList, actionID);
        }

        @Override
        public double getActionCostScaling(Identifier actionID) {
            return this.costRescaleMap.getOrDefault(actionID, 1.0);
        }

        @Override
        public double globalCostScaling() { return globalCostScaling; }

        @Override
        public boolean doesGreaterTeleportSplatItems() { return greaterTeleportSplatsItems; }

        @Override
        public boolean doVillagersTakeOffenseAtMindMurder() {
            return villagersOffendedByMindMurder;
        }

        @Override
        public boolean canTeleportInThisDimension(ResourceKey<Level> dimension) {
            return noneMatch(tpDimDenylist, dimension.identifier());
        }

        @Override
        public boolean trueNameHasAmbit() {
            return doesTrueNameHaveAmbit;
        }

        @Override
        public boolean personalMediaPool() {
            return personalMediaPool;
        }

        @Override
        public int personalMediaMax() {
            return personalMediaMax;
        }

        @Override
        public int personalMediaRegenStep() {
            return personalMediaRegenStep;
        }

        @Override
        public int personalMediaRegenInterval() {
            return personalMediaRegenInterval;
        }

        @Override
        public boolean personalMediaAfterEnlightened() {
            return personalMediaAfterEnlightened;
        }

        @Override
        public boolean enablesMindEnvActions() {
            return enablesMindEnvActions;
        }

        @Override
        public boolean bypassesSilencedEnvRestrictions() {
            return bypassesSilencedEnvRestrictions;
        }

        @Override
        public int maxMatrixSize() {
            return maxMatrixSize;
        }

        @Override
        public int maxStringLength() {
            return maxStringLength;
        }

        @Override
        public double setBlockStringCost() {
            return setBlockStringCost;
        }

        @Override
        public double nameCost() {
            return nameCost;
        }

        public double traderScrollChance() {
            return traderScrollChance;
        }

        /**
         * Returns -1 if none is found
         */
        public int scrollRangeForLootTable(Identifier lootTable) {
            return this.scrollInjections.getOrDefault(lootTable, -1);
        }

        public boolean shouldInjectLore(Identifier lootTable) {
            return anyMatchResLoc(this.loreInjections, lootTable);
        }

        public double loreChance() {
            return loreChance;
        }

        public boolean shouldInjectCyphers(Identifier lootTable) {
            return anyMatchResLoc(this.cypherInjections, lootTable);
        }

        public double cypherChance() {
            return cypherChance;
        }
    }
}
