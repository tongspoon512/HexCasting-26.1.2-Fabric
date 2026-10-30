package at.petrak.hexcasting.api.casting.eval

import at.petrak.hexcasting.api.casting.math.HexCoord
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.utils.NBTBuilder
import at.petrak.hexcasting.api.utils.getInt
import net.minecraft.nbt.CompoundTag
import java.util.*


data class ResolvedPattern(val pattern: HexPattern, val origin: HexCoord, var type: ResolvedPatternType) {
    fun serializeToNBT() = NBTBuilder {
        "Pattern" %= pattern.serializeToNBT()
        "OriginQ" %= origin.q
        "OriginR" %= origin.r
        "Valid" %= type.name.lowercase(Locale.ROOT)
    }

    companion object {
        @JvmStatic
        fun fromNBT(tag: CompoundTag): ResolvedPattern {
            val pattern = HexPattern.fromNBT(tag.getCompoundOrEmpty("Pattern"))
            val origin = HexCoord(tag.getInt("OriginQ", 0), tag.getInt("OriginR", 0))
            val valid = ResolvedPatternType.fromString(tag.getString("Valid").orElse(""))
            return ResolvedPattern(pattern, origin, valid)
        }
    }
}
