package miyucomics.hexcassettes.client

import net.minecraft.client.Minecraft

/**
 * 客户端缓存的磁带状态，由 [miyucomics.hexcassettes.network.MsgCassetteSyncS2C] 更新。
 */
object CassetteClientStorage {
    var ownedCassettes: Int = 0
        private set

    var activeCassettes: List<String> = emptyList()
        private set

    fun applySync(owned: Int, keys: List<String>) {
        Minecraft.getInstance().execute { update(owned, keys) }
    }

    fun update(owned: Int, keys: List<String>) {
        ownedCassettes = owned
        activeCassettes = keys
    }
}
