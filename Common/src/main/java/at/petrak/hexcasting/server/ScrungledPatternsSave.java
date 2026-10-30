package at.petrak.hexcasting.server;

import at.petrak.hexcasting.api.casting.ActionRegistryEntry;
import at.petrak.hexcasting.api.casting.math.EulerPathFinder;
import at.petrak.hexcasting.api.casting.math.HexDir;
import at.petrak.hexcasting.api.mod.HexTags;
import at.petrak.hexcasting.api.utils.HexUtils;
import at.petrak.hexcasting.xplat.IXplatAbstractions;
import com.mojang.datafixers.util.Pair;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps angle sigs to resource locations and their preferred start dir so we can look them up in the main registry
 * Save this on the world in case the random algorithm changes.
 */
public class ScrungledPatternsSave extends SavedData {
    public static final String DATA_VERSION = "0.1.0";
    public static final String TAG_SAVED_DATA = "hexcasting.per-world-patterns." + DATA_VERSION;
    private static final String TAG_DIR = "startDir";
    private static final String TAG_KEY = "key";

    public static final SavedDataType<ScrungledPatternsSave> TYPE = new SavedDataType<>(
        Identifier.parse(TAG_SAVED_DATA),
        () -> new ScrungledPatternsSave(new HashMap<>()),
        CompoundTag.CODEC.xmap(ScrungledPatternsSave::load, ScrungledPatternsSave::saveToTag),
        DataFixTypes.SAVED_DATA_WORLD_CLOCKS);

    /**
     * Maps scrungled signatures to their keys.
     */
    private final Map<String, PerWorldEntry> lookup;

    /**
     * Reverse-maps resource keys to their signature; you can use that in {@code lookup}.
     * <p>
     * This way we can look up things if we know their resource key, for commands and such
     */
    private final Map<ResourceKey<ActionRegistryEntry>, String> reverseLookup;

    private ScrungledPatternsSave(Map<String, PerWorldEntry> lookup) {
        this.lookup = lookup;
        this.reverseLookup = new HashMap<>();
        this.lookup.forEach((sig, entry) -> {
            this.reverseLookup.put(entry.key, sig);
        });
    }

    @Nullable
    public PerWorldEntry lookup(String signature) {
        return this.lookup.get(signature);
    }

    @Nullable
    public Pair<String, PerWorldEntry> lookupReverse(ResourceKey<ActionRegistryEntry> key) {
        var sig = this.reverseLookup.get(key);
        if (sig == null) return null;

        return Pair.of(sig, this.lookup.get(sig));
    }

    public CompoundTag saveToTag() {
        var tag = new CompoundTag();
        // We don't save the reverse lookup cause we can reconstruct it when loading.
        this.lookup.forEach((sig, entry) -> {
            var inner = new CompoundTag();
            inner.putByte(TAG_DIR, (byte) entry.canonicalStartDir.ordinal());
            inner.putString(TAG_KEY, entry.key().identifier().toString());
            tag.put(sig, inner);
        });
        return tag;
    }

    private static ScrungledPatternsSave load(CompoundTag tag) {
        var registryKey = IXplatAbstractions.INSTANCE.getActionRegistry().key();

        var map = new HashMap<String, PerWorldEntry>();
        for (var sig : tag.keySet()) {
            var inner = tag.getCompound(sig).orElse(null);
            if (inner == null) {
                continue;
            }

            var rawDir = inner.getByte(TAG_DIR).orElse((byte) 0);
            var rawKey = inner.getString(TAG_KEY).orElse("");

            var keyId = Identifier.tryParse(rawKey);
            if (keyId == null) {
                continue;
            }

            var dir = HexDir.values()[rawDir];
            var key = ResourceKey.create(registryKey, keyId);

            if (IXplatAbstractions.INSTANCE.getActionRegistry().containsKey(key))
                map.put(sig, new PerWorldEntry(key, dir));
        }

        return new ScrungledPatternsSave(map);
    }

    public static ScrungledPatternsSave createFromScratch(long seed) {
        var map = new HashMap<String, PerWorldEntry>();

        var registry = IXplatAbstractions.INSTANCE.getActionRegistry();

        // TODO: this version of the code doesn't have overlap protection
        // this means if some hilarious funny person makes a great spell that has the same shape as a normal spell
        // there might be overlap.
        // I'm going to file that under "don't do that"
        // (the number literal phial incident won't happen though because we check for special handlers first now)
        for (var key : registry.registryKeySet()) {
            var entry = registry.getOptional(key).orElse(null);
            if (entry != null && HexUtils.isOfTag(registry, key, HexTags.Actions.PER_WORLD_PATTERN)) {
                var scrungledPat = EulerPathFinder.findAltDrawing(entry.prototype(), seed);
                map.put(scrungledPat.anglesSignature(), new PerWorldEntry(key, scrungledPat.getStartDir()));
            }
        }

        var out = new ScrungledPatternsSave(map);
        out.setDirty();
        return out;
    }

    public static ScrungledPatternsSave open(ServerLevel overworld) {
        var storage = overworld.getDataStorage();
        var existing = storage.get(TYPE);
        if (existing != null) {
            return existing;
        }
        var created = createFromScratch(overworld.getSeed());
        storage.set(TYPE, created);
        return created;
    }

    public record PerWorldEntry(ResourceKey<ActionRegistryEntry> key, HexDir canonicalStartDir) {
    }
}
