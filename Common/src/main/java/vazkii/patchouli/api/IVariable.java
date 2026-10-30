package vazkii.patchouli.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.stream.Stream;

/**
 * Stub of patchouli's IVariable for the 26.1.2 port (patchouli not available for 26.1).
 */
public interface IVariable {
    String asString();

    default Stream<IVariable> asStream() {
        return Stream.empty();
    }

    default List<IVariable> asListOrSingleton() {
        return List.of();
    }

    default <T> T as(Class<T> clazz) {
        return null;
    }

    default boolean asBoolean(boolean fallback) {
        return fallback;
    }

    default JsonElement unwrap() {
        return JsonNull.INSTANCE;
    }

    static IVariable wrap(String s) {
        return () -> s == null ? "" : s;
    }

    static IVariable wrap(boolean b) {
        return () -> Boolean.toString(b);
    }

    static IVariable from(Component c) {
        return () -> c == null ? "" : c.getString();
    }

    static IVariable from(ItemStack stack) {
        return () -> "";
    }

    static IVariable from(ItemStack... stacks) {
        return () -> "";
    }

    static IVariable wrapList(List<IVariable> list) {
        return () -> "";
    }

    static IVariable wrapList(List<IVariable> list, boolean b) {
        return () -> "";
    }
}
