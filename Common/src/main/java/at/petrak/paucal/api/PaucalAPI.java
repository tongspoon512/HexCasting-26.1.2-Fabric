package at.petrak.paucal.api;

import at.petrak.paucal.api.contrib.Contributor;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Vendored stub of PaucalAPI for the 26.1.2 port.
 * paucal is not published for Minecraft 26.1, so the minimal API surface used by
 * Hex Casting is reimplemented here. Contributor lookup always returns null.
 */
public interface PaucalAPI {
    String MOD_ID = "paucal";
    Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    String CONTRIBUTOR_URL = "https://raw.githubusercontent.com/gamma-delta/HexMod/main/contributors.json";
    String HEADPAT_AUDIO_URL_STUB = "https://cdn.discordapp.com/attachments/";

    static PaucalAPI instance() {
        return StubPaucalAPI.INSTANCE;
    }

    static Identifier modLoc(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    default Contributor getContributor(UUID uuid) {
        return null;
    }
}
