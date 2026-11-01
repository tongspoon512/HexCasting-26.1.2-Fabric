package at.petrak.hexcasting.common.casting.actions.spells.great

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getEntity
import at.petrak.hexcasting.api.casting.getVec3
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapBadLocation
import at.petrak.hexcasting.api.casting.mishaps.MishapImmuneEntity
import at.petrak.hexcasting.api.misc.MediaConstants
import at.petrak.hexcasting.api.mod.HexConfig
import at.petrak.hexcasting.api.mod.HexTags
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.Relative
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.phys.Vec3

// TODO while we're making breaking changes I *really* want to have the vector in the entity's local space
// WRT its look vector
object OpTeleport : SpellAction {
    override val argc = 2
    override fun execute(
            args: List<Iota>,
            env: CastingEnvironment
    ): SpellAction.Result {

        val teleportee = args.getEntity(0, argc)
        val delta = args.getVec3(1, argc)
        env.assertEntityInRange(teleportee)

        if (teleportee.type.builtInRegistryHolder().`is`(HexTags.Entities.CANNOT_TELEPORT))
            throw MishapImmuneEntity(teleportee)

        if (teleportee.type.builtInRegistryHolder().`is`(HexTags.Entities.STICKY_TELEPORTERS)) {
            val immunePassengers = teleportee.passengers.filter { it.type.builtInRegistryHolder().`is`(HexTags.Entities.CANNOT_TELEPORT) }
            if (!immunePassengers.isEmpty()) 
                throw MishapImmuneEntity(immunePassengers.get(0))
        }

        val targetPos = teleportee.position().add(delta)
        if (!HexConfig.server().canTeleportInThisDimension(env.world.dimension()))
            throw MishapBadLocation(targetPos, "bad_dimension")

        env.assertVecInWorld(targetPos)
        if (!env.isVecInWorld(targetPos.subtract(0.0, 1.0, 0.0)))
            throw MishapBadLocation(targetPos, "too_close_to_out")

        val targetMiddlePos = teleportee.position().add(0.0, teleportee.eyeHeight / 2.0, 0.0)

        return SpellAction.Result(
            Spell(teleportee, delta),
            10 * MediaConstants.CRYSTAL_UNIT,
            listOf(ParticleSpray.cloud(targetMiddlePos, 2.0), ParticleSpray.burst(targetMiddlePos.add(delta), 2.0))
        )
    }

    private data class Spell(val teleportee: Entity, val delta: Vec3) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val distance = delta.length()

            teleportRespectSticky(teleportee, delta, env.world)

            if (HexConfig.server().doesGreaterTeleportSplatItems() && teleportee is ServerPlayer && teleportee == env.castingEntity) {
                // Drop items conditionally, based on distance teleported.
                // MOST IMPORTANT: Never drop main hand item, since if it's a trinket, it will get duplicated later.

                val baseDropChance = distance / 10000.0

                // Armor and hotbar items have a further reduced chance to be dropped since it's particularly annoying
                // having to rearrange those. Also it makes sense for LORE REASONS probably, since the caster is more
                // aware of items they use often.
                for (armorItem in EquipmentSlot.values().filter { it.isArmor }.map { teleportee.getItemBySlot(it) }) {
                    if (armorItem.getEnchantments().keySet().any { it.`is`(Enchantments.BINDING_CURSE) })
                        continue

                    if (Math.random() < baseDropChance * 0.25) {
                        teleportee.drop(armorItem.copy(), true, false)
                        armorItem.shrink(armorItem.count)
                    }
                }

                val inv = teleportee.inventory
                for (pos in 0 until inv.getContainerSize()) {
                    val invItem = inv.getItem(pos)
                    if (invItem == teleportee.mainHandItem) continue
                    val dropChance = if (pos < 9) baseDropChance * 0.5 else baseDropChance // hotbar
                    if (Math.random() < dropChance) {
                        teleportee.drop(invItem.copy(), true, false)
                        invItem.shrink(invItem.count)
                    }
                }

                // we also don't drop the offhand just to be nice
            }
        }
    }

    fun teleportRespectSticky(teleportee: Entity, delta: Vec3, world: ServerLevel) {
        if (!HexConfig.server().canTeleportInThisDimension(world.dimension())) {
            return
        }

        // A "sticky" entity teleports itself and its riders
        // This is the default behavior for teleportTo(), so we remove the riders if the teleportee *isn't* sticky
        teleportee.stopRiding()
        if (!teleportee.type.builtInRegistryHolder().`is`(HexTags.Entities.STICKY_TELEPORTERS)) 
            teleportee.passengers.forEach(Entity::stopRiding)

        // 26.1's relative teleports add the entity's current position/rotation to the supplied values
        // (see PositionMoveRotation.calculateAbsolute), so the coordinates here are offsets, not absolute.
        // Hand over the delta itself (and zero rotation deltas) so the entity lands at position + delta,
        // while the DELTA flags keep the current velocity instead of zeroing it.
        teleportee.teleportTo(world, delta.x, delta.y, delta.z,
            setOf(
                Relative.X, Relative.Y, Relative.Z,
                Relative.Y_ROT, Relative.X_ROT,
                Relative.DELTA_X, Relative.DELTA_Y, Relative.DELTA_Z
            ), 0.0f, 0.0f, false)
    }
}
