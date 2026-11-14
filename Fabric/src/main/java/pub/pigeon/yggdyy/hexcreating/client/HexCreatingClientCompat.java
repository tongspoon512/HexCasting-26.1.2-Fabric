package pub.pigeon.yggdyy.hexcreating.client;

import net.fabricmc.loader.api.FabricLoader;

/**
 * L0 client gate. Must not import any Create class.
 */
public final class HexCreatingClientCompat {
    private static boolean initialized = false;

    private HexCreatingClientCompat() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        if (!FabricLoader.getInstance().isModLoaded("create")) {
            return;
        }
        HexCreatingPonder.register();
    }
}
