package ram.talia.moreiotas.fabric.cc

import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.ladysnake.cca.api.v3.component.Component
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent

class CCChatPrefixHolder(private val player: Player) : Component, AutoSyncedComponent {
    var prefix: String? = null
        private set

    fun setPrefix(prefix: String?) {
        this.prefix = prefix
        MoreIotasCardinalComponents.CHAT_PREFIX_HOLDER.sync(this.player)
    }

    override fun readData(input: ValueInput) {
        this.prefix = input.getString(TAG_CHAT_PREFIX).orElse(null)
    }

    override fun writeData(output: ValueOutput) {
        this.prefix?.let { output.putString(TAG_CHAT_PREFIX, it) }
    }

    companion object {
        const val TAG_CHAT_PREFIX = "moreiotas:prefix"
    }
}
