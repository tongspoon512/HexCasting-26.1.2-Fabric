package at.petrak.hexcasting.api.advancements;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.advancements.criterion.MinMaxBounds;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

/**
 * Long variant of {@link MinMaxBounds}. Vanilla no longer provides one in 26.1, so this is a standalone
 * implementation that keeps the old public surface.
 */
public class MinMaxLongs implements MinMaxBounds<Long> {
    public static final MinMaxLongs ANY = new MinMaxLongs(null, null);
    private final MinMaxBounds.Bounds<Long> bounds;
    @Nullable
    private final Long minSq;
    @Nullable
    private final Long maxSq;

    @Nullable
    private static Long squareOpt(@Nullable Long l) {
        return l == null ? null : l * l;
    }

    private MinMaxLongs(@Nullable Long min, @Nullable Long max) {
        this.bounds = min == null && max == null
            ? MinMaxBounds.Bounds.any()
            : new MinMaxBounds.Bounds<>(Optional.ofNullable(min), Optional.ofNullable(max));
        this.minSq = squareOpt(min);
        this.maxSq = squareOpt(max);
    }

    public static MinMaxLongs exactly(long l) {
        return new MinMaxLongs(l, l);
    }

    public static MinMaxLongs between(long min, long max) {
        return new MinMaxLongs(min, max);
    }

    public static MinMaxLongs atLeast(long min) {
        return new MinMaxLongs(min, null);
    }

    public static MinMaxLongs atMost(long max) {
        return new MinMaxLongs(null, max);
    }

    @Override
    public MinMaxBounds.Bounds<Long> bounds() {
        return this.bounds;
    }

    public boolean matches(long l) {
        if (this.bounds.min().isPresent() && this.bounds.min().get() > l) {
            return false;
        } else {
            return this.bounds.max().isEmpty() || this.bounds.max().get() >= l;
        }
    }

    public boolean matchesSqr(long l) {
        if (this.minSq != null && this.minSq > l) {
            return false;
        } else {
            return this.maxSq == null || this.maxSq >= l;
        }
    }

    public static MinMaxLongs fromJson(@Nullable JsonElement json) {
        if (json == null || json.isJsonNull()) {
            return ANY;
        }
        if (json.isJsonPrimitive()) {
            return exactly(GsonHelper.convertToLong(json, "value"));
        }
        JsonObject obj = json.getAsJsonObject();
        Long min = obj.has("min") ? GsonHelper.convertToLong(obj.get("min"), "min") : null;
        Long max = obj.has("max") ? GsonHelper.convertToLong(obj.get("max"), "max") : null;
        return new MinMaxLongs(min, max);
    }

    public static MinMaxLongs fromReader(StringReader reader) throws CommandSyntaxException {
        return fromReader(reader, (l) -> l);
    }

    public static MinMaxLongs fromReader(StringReader reader, Function<Long, Long> map) throws CommandSyntaxException {
        StringBuilder sb = new StringBuilder();
        while (reader.canRead() && isAllowedInputChar(reader.peek())) {
            sb.append(reader.read());
        }
        String s = sb.toString();
        if (s.isEmpty() || s.equals("*") || s.equals("any")) {
            return ANY;
        }
        int split = s.indexOf("..");
        try {
            if (split < 0) {
                return exactly(map.apply(Long.parseLong(s)));
            }
            Long min = split == 0 ? null : map.apply(Long.parseLong(s.substring(0, split)));
            Long max = split == s.length() - 2 ? null : map.apply(Long.parseLong(s.substring(split + 2)));
            return new MinMaxLongs(min, max);
        } catch (NumberFormatException exn) {
            throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerInvalidInt().createWithContext(reader, s);
        }
    }

    private static boolean isAllowedInputChar(char c) {
        return c == '-' || c == '+' || c == '.' || c == '*' || (c >= '0' && c <= '9');
    }
}
