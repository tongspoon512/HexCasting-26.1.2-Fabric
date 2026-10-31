package ram.talia.moreiotas.api.mod

import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.mod.HexConfig

/**
 * MoreIotas 的服务器配置访问层。
 * 配置字段已并入 FabricHexConfig.Server，随 hexcasting 的 Cloth 配置界面一并展示。
 */
object MoreIotasConfig {
    val maxMatrixSize: Int
        get() = HexConfig.server()?.maxMatrixSize()
            ?: HexConfig.ServerConfigAccess.DEFAULT_MAX_MATRIX_SIZE

    val maxStringLength: Int
        get() = HexConfig.server()?.maxStringLength()
            ?: HexConfig.ServerConfigAccess.DEFAULT_MAX_STRING_LENGTH

    val setBlockStringCost: Long
        get() = ((HexConfig.server()?.setBlockStringCost()
            ?: HexConfig.ServerConfigAccess.DEFAULT_SET_BLOCK_STRING_COST) * MediaConstants.DUST_UNIT).toLong()

    val nameCost: Long
        get() = ((HexConfig.server()?.nameCost()
            ?: HexConfig.ServerConfigAccess.DEFAULT_NAME_COST) * MediaConstants.DUST_UNIT).toLong()
}
