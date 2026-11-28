package pub.pigeon.yggdyy.hexcreating.client;

import pub.pigeon.yggdyy.hexcreating.HexCreatingCompat;

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
        if (!HexCreatingCompat.isCreateLoaded()) {
            return;
        }
        HexCreatingPonder.register();
        HexCreatingClientRegistration.registerScryingLens();
    }
}
