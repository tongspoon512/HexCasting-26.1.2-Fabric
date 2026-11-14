package pub.pigeon.yggdyy.hexcreating;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * L0 entry point for the HexCreating content. This class must never reference a Create class
 * directly (not even in a field type), so it can be loaded safely when Create is absent.
 */
public final class HexCreatingCompat {
    public static final String MOD_ID = "hexcreating";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean initialized = false;

    private HexCreatingCompat() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static boolean isCreateLoaded() {
        return FabricLoader.getInstance().isModLoaded("create");
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        if (!isCreateLoaded()) {
            LOGGER.info("Create is not installed; HexCreating content stays disabled.");
            return;
        }

        HexCreatingRegistration.init();
    }
}
