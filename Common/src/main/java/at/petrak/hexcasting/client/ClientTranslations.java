package at.petrak.hexcasting.client;

import net.minecraft.client.resources.language.I18n;

/**
 * Client-only translation helpers.
 * <p>
 * Kept out of common code because {@code net.minecraft.client.resources.language.I18n} cannot be
 * loaded on a dedicated server.
 */
public final class ClientTranslations {
    private ClientTranslations() {
    }

    public static boolean exists(String key) {
        return I18n.exists(key);
    }
}
