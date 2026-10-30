package dev.emi.emi;

import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * Stub of emi's EmiPort.
 */
public class EmiPort {
    public static Component literal(String text) {
        return Component.literal(text);
    }

    public static Component translatable(String key, Object... args) {
        return Component.translatable(key, args);
    }

    public static FormattedCharSequence ordered(Component component) {
        return component.getVisualOrderText();
    }
}
