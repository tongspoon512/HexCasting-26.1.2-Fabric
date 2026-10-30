package gay.object.hexdebug.media;

import at.petrak.hexcasting.api.mod.HexConfig;
import gay.object.hexdebug.HexDebug;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.DoubleSupplier;

/**
 * Player attributes backing the HexDebug personal media pool. Registered from
 * {@code FabricHexInitializer.initRegistries()} and added to the player attribute supplier by
 * {@code FabricPlayerMixin}.
 */
public class HexDebugAttributes {

    /**
     * Sentinel base value that marks "this attribute has just been (re)created and still needs to be
     * filled from the session cache".
     */
    public static final double INIT_MEDIA_MARKER = -1919810.0;

    private static final Map<Identifier, Attribute> ATTRIBUTES = new LinkedHashMap<>();

    public static final Attribute PERSONAL_MEDIA = make("personal_media", new DynamicAttr(
        "hexdebug.attributes.personal_media", () -> INIT_MEDIA_MARKER).setSyncable(true));

    public static final Attribute PERSONAL_MEDIA_MAX = make("personal_media_max", new DynamicAttr(
        "hexdebug.attributes.personal_media_max",
        () -> safeConfig(HexConfig.ServerConfigAccess.DEFAULT_PERSONAL_MEDIA_MAX, () -> HexConfig.server().personalMediaMax())
    ).setSyncable(true));

    public static final Attribute PERSONAL_MEDIA_REGEN = make("personal_media_regen", new DynamicAttr(
        "hexdebug.attributes.personal_media_regen",
        () -> safeConfig(HexConfig.ServerConfigAccess.DEFAULT_PERSONAL_MEDIA_REGEN_STEP, () -> HexConfig.server().personalMediaRegenStep())
    ).setSyncable(true));

    private static double safeConfig(double fallback, DoubleSupplier getter) {
        try {
            return getter.getAsDouble();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static <T extends Attribute> T make(String id, T attr) {
        var old = ATTRIBUTES.put(Identifier.fromNamespaceAndPath(HexDebug.MOD_ID, id), attr);
        if (old != null) {
            throw new IllegalArgumentException("Typo? Duplicate id " + id);
        }
        return attr;
    }

    public static void register(BiConsumer<Attribute, Identifier> r) {
        for (var e : ATTRIBUTES.entrySet()) {
            r.accept(e.getValue(), e.getKey());
        }
    }

    /**
     * 26.1 attribute lookups all go through holders, so this bridges the raw attributes above back
     * into the registry that owns them.
     */
    public static Holder<Attribute> holder(Attribute attr) {
        return BuiltInRegistries.ATTRIBUTE.wrapAsHolder(attr);
    }

    /**
     * Attribute whose default value is read lazily so the server config is already available whenever
     * the player attribute supplier is rebuilt.
     */
    public static final class DynamicAttr extends Attribute {
        private final DoubleSupplier defaultSupplier;

        private DynamicAttr(String descriptionId, DoubleSupplier defaultSupplier) {
            super(descriptionId, 0.0);
            this.defaultSupplier = defaultSupplier;
        }

        @Override
        public double getDefaultValue() {
            try {
                return this.defaultSupplier.getAsDouble();
            } catch (Throwable ignored) {
                return super.getDefaultValue();
            }
        }
    }
}
