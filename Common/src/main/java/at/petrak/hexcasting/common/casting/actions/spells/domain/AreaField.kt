package at.petrak.hexcasting.common.casting.actions.spells.domain

import at.petrak.hexcasting.api.casting.area.AreaShape
import at.petrak.hexcasting.api.casting.area.DomainMediaSource
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.casting.eval.vm.CastingVM
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import at.petrak.hexcasting.common.lib.hex.HexIotaTypes
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.phys.Vec3
import java.util.IdentityHashMap
import java.util.UUID

/**
 * Server-side registry of all active domains.
 *
 * Domains are mirrored into [SpellFieldSave] so leaving and re-entering a world keeps them alive.
 */
object AreaField {
    const val RELEASE_INTERVAL = 3
    const val MAX_OPS_PER_RELEASE = 1000
    const val MAX_STACK_SIZE = 128

    class DomainInstance(
        val id: UUID,
        val level: ServerLevel,
        val ownerId: UUID,
        val shape: AreaShape,
    ) {
        var spells: List<Iota> = emptyList()
        var active: Boolean = false
        var cooldown: Int = 0
        var mediaSource: DomainMediaSource? = null

        fun getCaster(): ServerPlayer? {
            val player = this.level.server.playerList.getPlayer(this.ownerId) ?: return null
            return if (player.level() === this.level) player else null
        }
    }

    private val domains = LinkedHashMap<UUID, DomainInstance>()

    /** Sources that tried to bind to a domain before it was restored from disk. */
    private val pendingSources = HashMap<UUID, MutableList<DomainMediaSource>>()
    private val levelSaves = IdentityHashMap<ServerLevel, SpellFieldSave>()
    private val loadedLevels = IdentityHashMap<ServerLevel, Boolean>()

    fun create(level: ServerLevel, ownerId: UUID, shape: AreaShape): UUID {
        this.ensureLoaded(level)
        val id = UUID.randomUUID()
        val domain = DomainInstance(id, level, ownerId, shape)
        this.domains[id] = domain
        this.attachPendingSources(domain)
        this.sync(level)
        return id
    }

    fun get(id: UUID): DomainInstance? {
        return this.domains[id]
    }

    /** The first domain on this level that contains the given point. */
    fun findContaining(level: ServerLevel, point: Vec3): DomainInstance? {
        this.ensureLoaded(level)
        return this.domains.values.firstOrNull { it.level === level && it.shape.contains(point) }
    }

    fun remove(id: UUID) {
        val domain = this.domains.remove(id) ?: return
        domain.mediaSource?.onDomainClosed()
        this.sync(domain.level)
    }

    fun setSpells(id: UUID, spells: List<Iota>): Boolean {
        val domain = this.domains[id] ?: return false
        domain.spells = spells.toList()
        this.sync(domain.level)
        return true
    }

    fun setActive(id: UUID, active: Boolean): Boolean {
        val domain = this.domains[id] ?: return false
        domain.active = active
        this.sync(domain.level)
        return true
    }

    fun bind(id: UUID, source: DomainMediaSource) {
        val domain = this.domains[id]
        if (domain == null) {
            val list = this.pendingSources.getOrPut(id) { ArrayList() }
            if (!list.contains(source)) {
                list.add(source)
            }
            return
        }
        domain.mediaSource = source
    }

    fun unbind(id: UUID?, source: DomainMediaSource) {
        if (id == null) {
            return
        }
        this.pendingSources[id]?.remove(source)
        val domain = this.domains[id] ?: return
        if (domain.mediaSource === source) {
            domain.mediaSource = null
        }
    }

    fun onOwnerDeath(player: ServerPlayer) {
        val iter = this.domains.values.iterator()
        while (iter.hasNext()) {
            val domain = iter.next()
            if (domain.ownerId == player.uuid) {
                iter.remove()
                domain.mediaSource?.onDomainClosed()
                this.sync(domain.level)
            }
        }
    }

    fun tick(level: ServerLevel) {
        this.ensureLoaded(level)
        if (this.domains.isEmpty()) {
            return
        }
        for (domain in this.domains.values.toList()) {
            if (domain.level !== level) {
                continue
            }
            // 领域轮廓一直渲染，即使暂停或还没绑定咒术
            if (level.gameTime % 20L == 0L) {
                DomainVisuals.draw(level, domain.shape)
            }
            if (domain.cooldown > 0) {
                domain.cooldown--
                continue
            }
            if (!domain.active || domain.spells.isEmpty()) {
                continue
            }
            if (domain.mediaSource == null && domain.getCaster() == null) {
                continue
            }
            domain.cooldown = RELEASE_INTERVAL - 1
            val vm = CastingVM(CastingImage(), DomainCastEnv(domain))
            vm.queueExecuteAndWrapIotas(domain.spells, level)
        }
    }

    fun clear() {
        for (level in this.domains.values.map { it.level }.distinct()) {
            this.sync(level)
        }
        this.pendingSources.clear()
        for (domain in this.domains.values) {
            domain.mediaSource?.onDomainClosed()
        }
        this.domains.clear()
        this.levelSaves.clear()
        this.loadedLevels.clear()
    }

    private fun attachPendingSources(domain: DomainInstance) {
        val pending = this.pendingSources.remove(domain.id) ?: return
        domain.mediaSource = pending.firstOrNull()
    }

    private fun ensureLoaded(level: ServerLevel) {
        if (this.loadedLevels.containsKey(level)) {
            return
        }
        this.loadedLevels[level] = true
        val data = level.dataStorage.get(SpellFieldSave.TYPE) ?: return
        this.levelSaves[level] = data
        for (raw in data.domains) {
            val tag = raw as? CompoundTag ?: continue
            val domain = this.loadDomain(level, tag) ?: continue
            this.domains[domain.id] = domain
            this.attachPendingSources(domain)
        }
    }

    private fun loadDomain(level: ServerLevel, tag: CompoundTag): DomainInstance? {
        val id = runCatching { UUID.fromString(tag.getStringOr("id", "")) }.getOrNull() ?: return null
        val owner = runCatching { UUID.fromString(tag.getStringOr("owner", "")) }.getOrNull() ?: return null
        val shape = try {
            AreaShape.deserialize(tag.getCompoundOrEmpty("shape"))
        } catch (ignored: IllegalArgumentException) {
            return null
        }
        val domain = DomainInstance(id, level, owner, shape)
        domain.active = tag.getBooleanOr("active", false)
        val spellsTag = tag.getListOrEmpty("spells")
        if (!spellsTag.isEmpty) {
            domain.spells = HexIotaTypes.LIST.deserialize(spellsTag, level)?.list?.toList() ?: emptyList()
        }
        return domain
    }

    /** Writes this level's domains into its saved data; called whenever they change. */
    private fun sync(level: ServerLevel) {
        val data = this.levelSaves[level] ?: run {
            val storage = level.dataStorage
            val existing = storage.get(SpellFieldSave.TYPE) ?: SpellFieldSave()
            storage.set(SpellFieldSave.TYPE, existing)
            this.levelSaves[level] = existing
            existing
        }
        val list = ListTag()
        for (domain in this.domains.values) {
            if (domain.level !== level) {
                continue
            }
            val tag = CompoundTag()
            tag.putString("id", domain.id.toString())
            tag.putString("owner", domain.ownerId.toString())
            tag.put("shape", domain.shape.serialize())
            tag.putBoolean("active", domain.active)
            if (domain.spells.isNotEmpty()) {
                tag.put("spells", ListIota(domain.spells).serialize())
            }
            list.add(tag)
        }
        data.domains = list
        data.setDirty()
    }
}
