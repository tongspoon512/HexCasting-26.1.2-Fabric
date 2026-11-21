package at.petrak.hexcasting.common.casting.actions.spells.domain

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.Mob
import java.util.UUID

/**
 * Runtime state for 「无量空处」: players are sealed for 30 seconds (darkness, blindness, slowness V
 * and no spellcasting), mobs lose their AI for 60 seconds.
 */
object InfinityVoidField {
    private const val PLAYER_LOCK_TICKS = 30 * 20
    private const val MOB_LOCK_TICKS = 60 * 20

    private class MobLock(
        val level: ServerLevel,
        val id: UUID,
        val endTick: Long,
        val previousNoAi: Boolean,
    )

    private val playerLocks = HashMap<UUID, Long>()
    private val mobLocks = HashMap<UUID, MobLock>()

    private fun now(level: ServerLevel): Long = level.server.overworld().gameTime

    fun apply(level: ServerLevel, entity: LivingEntity) {
        val now = now(level)
        if (entity is ServerPlayer) {
            this.playerLocks[entity.uuid] = now + PLAYER_LOCK_TICKS
            entity.addEffect(MobEffectInstance(MobEffects.DARKNESS, PLAYER_LOCK_TICKS))
            entity.addEffect(MobEffectInstance(MobEffects.BLINDNESS, PLAYER_LOCK_TICKS))
            entity.addEffect(MobEffectInstance(MobEffects.SLOWNESS, PLAYER_LOCK_TICKS, 4))
        } else if (entity is Mob) {
            this.mobLocks[entity.uuid] = MobLock(level, entity.uuid, now + MOB_LOCK_TICKS, entity.isNoAi)
            entity.isNoAi = true
        }
    }

    @JvmStatic
    fun isLocked(player: ServerPlayer): Boolean {
        val end = this.playerLocks[player.uuid] ?: return false
        return end > now(player.level() as ServerLevel)
    }

    fun onPlayerDeath(player: ServerPlayer) {
        this.playerLocks.remove(player.uuid)
    }

    fun tick(level: ServerLevel) {
        val now = now(level)
        this.playerLocks.entries.removeIf { it.value <= now }

        val iter = this.mobLocks.values.iterator()
        while (iter.hasNext()) {
            val lock = iter.next()
            if (lock.level !== level) {
                continue
            }
            if (now < lock.endTick) {
                continue
            }
            (lock.level.getEntity(lock.id) as? Mob)?.isNoAi = lock.previousNoAi
            iter.remove()
        }
    }

    fun clear() {
        for (lock in this.mobLocks.values) {
            (lock.level.getEntity(lock.id) as? Mob)?.isNoAi = lock.previousNoAi
        }
        this.mobLocks.clear()
        this.playerLocks.clear()
    }
}
