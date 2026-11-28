package miyucomics.hexcassettes

import at.petrak.hexcasting.neoforge.attachment.NeoForgeHexAttachments
import miyucomics.hexcassettes.data.CassetteState
import net.minecraft.server.level.ServerPlayer

/**
 * 读取玩家身上的卡带状态附件（NeoForge data attachment）。
 */
val ServerPlayer.cassetteState: CassetteState
    get() = getData(NeoForgeHexAttachments.CASSETTES).state
