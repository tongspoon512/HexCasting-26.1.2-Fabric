package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapImmuneEntity
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.mod.HexConfig
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import ram.talia.moreiotas.api.getEntityType

/**
 * 无为转变：把一个实体转化成指定实体类型。
 * 保留位置、朝向与自定义名称，血量按比例折算；对 Boss 与配置中的免疫生物无效。
 */
object OpIdleTransfiguration : SpellAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val target = args.getEntity(0, argc)
        env.assertEntityInRange(target)

        val targetType = args.getEntityType(1, argc)
        if (targetType == EntityType.PLAYER) {
            throw MishapInvalidIota.ofType(args[1], 1, "type.entity")
        }
        if (target is Player || !isAllowed(target.type, targetType) || !isWhitelisted(target.type)) {
            throw MishapImmuneEntity(target)
        }

        return SpellAction.Result(
            Spell(target, target.type, targetType),
            MediaConstants.DUST_UNIT * 10,
            listOf(ParticleSpray.burst(target.position(), 1.0, 30))
        )
    }

    private data class Spell(
        private val target: Entity,
        private val sourceType: EntityType<*>,
        private val destinationType: EntityType<*>,
    ) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            // 施法到实际生效之间目标可能已死亡、被转化或换类型
            if (!target.isAlive || target.type != sourceType) {
                return
            }
            if (!isAllowed(sourceType, destinationType) || !isWhitelisted(sourceType)) {
                return
            }

            val newEntity = destinationType.create(env.world, EntitySpawnReason.CONVERSION) ?: return

            target.ejectPassengers()
            if (target.isPassenger) {
                target.stopRiding()
            }

            newEntity.snapTo(target.x, target.y, target.z, target.yRot, target.xRot)
            newEntity.yHeadRot = target.yHeadRot
            newEntity.customName = target.customName
            newEntity.isCustomNameVisible = target.isCustomNameVisible

            if (target is LivingEntity && newEntity is LivingEntity && target.maxHealth > 0.0f) {
                newEntity.health = (newEntity.maxHealth * target.health / target.maxHealth).coerceAtLeast(1.0f)
            }

            env.world.addFreshEntity(newEntity)
            target.discard()
        }
    }
}

/** Boss 硬编码免疫，其余按配置黑名单处理。 */
private fun isAllowed(source: EntityType<*>, destination: EntityType<*>): Boolean {
    return !isForbidden(source) && !isForbidden(destination)
}

/** Boss 硬编码免疫，其余按配置黑名单处理。 */
private fun isForbidden(type: EntityType<*>): Boolean {
    if (type == EntityType.PLAYER || type == EntityType.WITHER || type == EntityType.ENDER_DRAGON) {
        return true
    }
    val id = BuiltInRegistries.ENTITY_TYPE.getKey(type)
    return HexConfig.anyMatch(HexConfig.server().idleTransfigurationBlacklist(), id)
}

/** 白名单为空表示所有非免疫生物都适用。 */
private fun isWhitelisted(type: EntityType<*>): Boolean {
    val whitelist = HexConfig.server().idleTransfigurationWhitelist()
    return whitelist.isEmpty() || HexConfig.anyMatch(whitelist, BuiltInRegistries.ENTITY_TYPE.getKey(type))
}
