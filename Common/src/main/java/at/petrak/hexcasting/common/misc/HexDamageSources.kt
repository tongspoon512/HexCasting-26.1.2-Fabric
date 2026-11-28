package at.petrak.hexcasting.common.misc

import at.petrak.hexcasting.common.lib.HexDamageTypes
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.Level

/**
 * 26.1 中 DamageSources.source(ResourceKey) 是 private：
 * Fabric 靠 access widener 放宽，NeoForge 靠 access transformer。
 * 这里统一通过 RegistryAccess 取 Holder，避免平台各自的访问放宽。
 */
object HexDamageSources {
    @JvmStatic
    fun overcast(entity: Entity): DamageSource = DamageSource(overcastType(entity.level()))

    @JvmStatic
    fun overcast(entity: Entity, cause: Entity?): DamageSource =
        DamageSource(overcastType(entity.level()), cause)

    private fun overcastType(level: Level): Holder<DamageType> =
        level.registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE)
            .getOrThrow(HexDamageTypes.OVERCAST)
}
