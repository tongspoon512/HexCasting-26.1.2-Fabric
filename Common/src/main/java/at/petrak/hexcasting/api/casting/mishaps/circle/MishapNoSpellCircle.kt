package at.petrak.hexcasting.api.casting.mishaps.circle

import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.Mishap
import at.petrak.hexcasting.api.pigment.FrozenPigment
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents
import net.minecraft.world.item.enchantment.EnchantmentHelper

class MishapNoSpellCircle : Mishap() {
    override fun accentColor(env: CastingEnvironment, errorCtx: Context): FrozenPigment =
        dyeColor(DyeColor.LIGHT_BLUE)

    override fun execute(env: CastingEnvironment, errorCtx: Context, stack: MutableList<Iota>) {
        val caster = env.castingEntity as? ServerPlayer
        if (caster != null) {
            // FIXME: handle null caster case
            // 26.1's Inventory no longer exposes its item lists; it is a Container, so go slot by slot.
            val inv = caster.inventory
            for (slot in 0 until inv.containerSize) {
                val item = inv.getItem(slot)
                if (!item.isEmpty && !EnchantmentHelper.has(item, EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) {
                    caster.drop(item, true, false)
                    inv.setItem(slot, ItemStack.EMPTY)
                }
            }
        }
    }

    override fun errorMessage(env: CastingEnvironment, errorCtx: Context) =
        error("no_spell_circle")
}
