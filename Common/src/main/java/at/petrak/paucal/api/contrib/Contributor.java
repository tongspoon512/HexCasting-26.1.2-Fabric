package at.petrak.paucal.api.contrib;

import com.google.gson.JsonObject;

import java.util.UUID;

/**
 * Vendored stub of paucal's Contributor. The full feature set (headpat sounds, etc.)
 * is not available in the 26.1 port; only the data-access surface is provided.
 */
public class Contributor {
    private final UUID uuid;
    private final JsonObject otherVals;

    public Contributor(UUID uuid, JsonObject otherVals) {
        this.uuid = uuid;
        this.otherVals = otherVals == null ? new JsonObject() : otherVals;
    }

    public UUID getUuid() {
        return uuid;
    }

    public int getLevel() {
        return 0;
    }

    public boolean isDev() {
        return false;
    }

    public JsonObject otherVals() {
        return otherVals;
    }

    public String getString(String key) {
        return otherVals.has(key) ? otherVals.get(key).getAsString() : null;
    }

    public String getString(String key, String fallback) {
        var s = getString(key);
        return s == null ? fallback : s;
    }

    public Integer getInt(String key) {
        return otherVals.has(key) ? otherVals.get(key).getAsInt() : null;
    }

    public int getInt(String key, int fallback) {
        var i = getInt(key);
        return i == null ? fallback : i;
    }

    public Float getFloat(String key) {
        return otherVals.has(key) ? otherVals.get(key).getAsFloat() : null;
    }

    public float getFloat(String key, float fallback) {
        var f = getFloat(key);
        return f == null ? fallback : f;
    }

    public Boolean getBool(String key) {
        return otherVals.has(key) ? otherVals.get(key).getAsBoolean() : null;
    }

    public boolean getBool(String key, boolean fallback) {
        var b = getBool(key);
        return b == null ? fallback : b;
    }

    public java.util.Set<String> allKeys() {
        return otherVals.keySet();
    }
}
