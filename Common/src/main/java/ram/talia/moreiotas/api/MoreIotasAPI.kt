package ram.talia.moreiotas.api

import net.minecraft.resources.Identifier
import org.slf4j.LoggerFactory

/**
 * MoreIotas 移植入口：id 命名空间保持上游的 "moreiotas"，
 * 以保证旧存档中的 iota / 图案 id 仍能解析。
 */
object MoreIotasAPI {
    const val MOD_ID = "moreiotas"

    val LOGGER = LoggerFactory.getLogger("MoreIotas")

    fun modLoc(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
