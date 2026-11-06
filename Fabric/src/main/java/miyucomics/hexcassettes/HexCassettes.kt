package miyucomics.hexcassettes

import net.minecraft.resources.Identifier

/**
 * Hexcassettes 移植版：卡带延迟队列。
 *
 * 上游依赖 Hexpose 的 TextIota，本项目改用 MoreIotas 的 StringIota 作为标签。
 */
object HexCassettes {
    const val MOD_ID: String = "hexcassettes"

    @JvmStatic
    fun id(path: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, path)
}
