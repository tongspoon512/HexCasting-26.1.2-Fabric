package at.petrak.hexcasting.common.casting.actions.spells

import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.mishaps.MishapInvalidIota
import net.minecraft.core.particles.BlockParticleOption
import net.minecraft.core.particles.DustParticleOptions
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Blocks
import ram.talia.moreiotas.api.casting.iota.StringIota

/**
 * 把字符串 iota 解析成粒子选项。语法：
 * - `minecraft:flame` / `flame`：无参数粒子
 * - `dust:#RRGGBB`：有色尘粒子
 * - `block:minecraft:stone`：方块碎屑
 */
object ParticleSpec {
    fun parse(iota: StringIota, reverseIdx: Int): ParticleOptions {
        val text = iota.string.trim().lowercase()
        return when {
            text.startsWith("dust:") -> parseDust(iota, reverseIdx, text.removePrefix("dust:"))
            text.startsWith("block:") -> parseBlock(iota, reverseIdx, text.removePrefix("block:"))
            else -> parseSimple(iota, reverseIdx, text)
        }
    }

    private fun parseSimple(iota: Iota, reverseIdx: Int, text: String): ParticleOptions {
        val id = parseId(text, iota, reverseIdx)
        val type = BuiltInRegistries.PARTICLE_TYPE.getOptional(id).orElse(null)
            ?: throw MishapInvalidIota.of(iota, reverseIdx, "particle")
        return type as? SimpleParticleType
            ?: throw MishapInvalidIota.of(iota, reverseIdx, "particle")
    }

    private fun parseDust(iota: Iota, reverseIdx: Int, colorText: String): ParticleOptions {
        val hex = colorText.removePrefix("#")
        val rgb = if (hex.length == 6) hex.toIntOrNull(16) else null
        if (rgb == null) throw MishapInvalidIota.of(iota, reverseIdx, "particle")
        return DustParticleOptions(0xFF000000.toInt() or rgb, 1.0f)
    }

    private fun parseBlock(iota: Iota, reverseIdx: Int, text: String): ParticleOptions {
        val id = parseId(text, iota, reverseIdx)
        val block = BuiltInRegistries.BLOCK.getOptional(id).orElse(null)
            ?: throw MishapInvalidIota.of(iota, reverseIdx, "particle")
        return BlockParticleOption(ParticleTypes.BLOCK, block.defaultBlockState())
    }

    private fun parseId(text: String, iota: Iota, reverseIdx: Int): Identifier {
        val full = if (':' in text) text else "minecraft:$text"
        return Identifier.tryParse(full) ?: throw MishapInvalidIota.of(iota, reverseIdx, "particle")
    }
}
