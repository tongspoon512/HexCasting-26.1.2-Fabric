package at.petrak.hexcasting.api.casting.mishaps

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.common.lib.HexDamageTypes
import net.minecraft.core.registries.Registries
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Mob
import net.minecraft.world.item.DyeColor

class MishapAlreadyBrainswept(val mob: Mob) : Mishap() {
    override fun accentColor(env: CastingEnvironment, errorCtx: Context): FrozenPigment =
        dyeColor(DyeColor.GREEN)

    override fun execute(env: CastingEnvironment, errorCtx: Context, stack: MutableList<Iota>) {
        val overcast = mob.level().registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE)
            .getOrThrow(HexDamageTypes.OVERCAST)
        mob.hurt(DamageSource(overcast, env.castingEntity), mob.health)
    }

    override fun particleSpray(env: CastingEnvironment) =
        ParticleSpray.burst(mob.eyePosition, 1.0)

    override fun errorMessage(env: CastingEnvironment, errorCtx: Context) =
        error("already_brainswept")

}
