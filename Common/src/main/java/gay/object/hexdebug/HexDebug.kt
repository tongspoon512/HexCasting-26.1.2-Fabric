package gay.`object`.hexdebug

import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

object HexDebug {
    const val MOD_ID = "hexdebug"

    val LOGGER = LoggerFactory.getLogger("HexDebug")

    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
