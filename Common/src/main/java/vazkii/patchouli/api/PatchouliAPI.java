package vazkii.patchouli.api;

/**
 * Stub of patchouli's PatchouliAPI. Patchouli is not available for 26.1, so this
 * is a no-op stub that allows Hex Casting to compile without the guide book feature.
 */
public interface PatchouliAPI {
    static PatchouliAPI get() {
        return StubPatchouliAPI.INSTANCE;
    }

    void setConfigFlag(String flag, boolean value);

    final class StubPatchouliAPI implements PatchouliAPI {
        static final StubPatchouliAPI INSTANCE = new StubPatchouliAPI();

        @Override
        public void setConfigFlag(String flag, boolean value) {
            // no-op
        }
    }
}
