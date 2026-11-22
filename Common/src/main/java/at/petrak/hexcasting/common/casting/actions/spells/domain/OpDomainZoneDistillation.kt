package at.petrak.hexcasting.common.casting.actions.spells.domain

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getArea
import at.petrak.hexcasting.api.casting.getBool
import at.petrak.hexcasting.api.casting.iota.EntityIota
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.phys.AABB

/**
 * 区域之馏化：领域 → 领域内符合要求的实体列表。
 *
 * 布尔为假时返回除掉落物以外的所有实体；
 * 布尔为真时再排除召唤该领域的玩家。
 */
object OpDomainZoneDistillation : ConstMediaAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val area = args.getArea(0, this.argc)
        val excludeOwner = args.getBool(1, this.argc)
        val domain = AreaField.get(area.domainId)
            ?: throw MishapBadLocation(area.shape.center, "domain_expired")

        val shape = domain.shape
        val bounds = shape.bounds()
        val box = AABB(bounds[0], bounds[1], bounds[2], bounds[3], bounds[4], bounds[5])
        val level = domain.level
        val entities = level.getEntities(null, box) { entity ->
            entity.isAlive &&
                !entity.isSpectator &&
                entity !is ItemEntity &&
                (!excludeOwner || entity.uuid != domain.ownerId) &&
                shape.contains(entity.position())
        }.sortedBy { it.distanceToSqr(shape.center) }
        return entities.map(::EntityIota).asActionResult
    }
}
