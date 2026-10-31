package ram.talia.moreiotas.fabric.cc

import org.ladysnake.cca.api.v3.component.ComponentKey
import org.ladysnake.cca.api.v3.component.ComponentRegistry
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy
import ram.talia.moreiotas.api.MoreIotasAPI

class MoreIotasCardinalComponents : EntityComponentInitializer {
    override fun registerEntityComponentFactories(registry: EntityComponentFactoryRegistry) {
        registry.registerForPlayers(CHAT_PREFIX_HOLDER, ::CCChatPrefixHolder, RespawnCopyStrategy.ALWAYS_COPY)
    }

    companion object {
        @JvmField
        val CHAT_PREFIX_HOLDER: ComponentKey<CCChatPrefixHolder> = ComponentRegistry.getOrCreate(
            MoreIotasAPI.modLoc("chat_prefix_holder"),
            CCChatPrefixHolder::class.java
        )
    }
}
