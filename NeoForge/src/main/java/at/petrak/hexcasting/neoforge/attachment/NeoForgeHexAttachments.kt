package at.petrak.hexcasting.neoforge.attachment

import at.petrak.hexcasting.api.casting.eval.ResolvedPattern
import at.petrak.hexcasting.api.casting.eval.vm.CastingImage
import at.petrak.hexcasting.api.pigment.FrozenPigment
import at.petrak.hexcasting.api.player.AltioraAbility
import at.petrak.hexcasting.api.player.FlightAbility
import at.petrak.hexcasting.api.player.Sentinel
import com.mojang.serialization.Codec
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.world.phys.Vec3
import net.neoforged.neoforge.attachment.AttachmentType
import net.neoforged.neoforge.common.util.ValueIOSerializable
import miyucomics.hexcassettes.data.CassetteState
import java.util.function.Supplier

/**
 * CCA 组件在 NeoForge 的替代品：用 data attachment 保存玩家/生物状态。
 *
 * 存档格式与 Fabric 侧 CCFlight/CCSentinel 等保持一致，便于两平台共享存档语义。
 * 同步（sync）暂未接入，客户端视觉相关状态会在 P7 补齐。
 */
object NeoForgeHexAttachments {
    private val DIMENSION_CODEC: Codec<ResourceKey<Level>> = ResourceKey.codec(Registries.DIMENSION)

    class FlightData : ValueIOSerializable {
        var flight: FlightAbility? = null

        override fun serialize(output: ValueOutput) {
            output.putBoolean(TAG_ALLOWED, flight != null)
            flight?.let {
                output.putInt(TAG_TIME_LEFT, it.timeLeft())
                output.store(TAG_DIMENSION, DIMENSION_CODEC, it.dimension())
                output.store(TAG_ORIGIN, Vec3.CODEC, it.origin())
                output.putDouble(TAG_RADIUS, it.radius())
            }
        }

        override fun deserialize(input: ValueInput) {
            if (!input.getBooleanOr(TAG_ALLOWED, false)) {
                flight = null
                return
            }
            flight = FlightAbility(
                input.getIntOr(TAG_TIME_LEFT, 0),
                input.read(TAG_DIMENSION, DIMENSION_CODEC).orElse(null),
                input.read(TAG_ORIGIN, Vec3.CODEC).orElse(Vec3.ZERO),
                input.getDoubleOr(TAG_RADIUS, 0.0)
            )
        }
    }

    class SentinelData : ValueIOSerializable {
        var sentinel: Sentinel? = null

        override fun serialize(output: ValueOutput) {
            output.putBoolean(TAG_HAS_SENTINEL, sentinel != null)
            sentinel?.let {
                output.putBoolean(TAG_EXTENDS_RANGE, it.extendsRange())
                output.store(TAG_POSITION, Vec3.CODEC, it.position())
                output.store(TAG_DIMENSION, DIMENSION_CODEC, it.dimension())
            }
        }

        override fun deserialize(input: ValueInput) {
            if (!input.getBooleanOr(TAG_HAS_SENTINEL, false)) {
                sentinel = null
                return
            }
            sentinel = Sentinel(
                input.getBooleanOr(TAG_EXTENDS_RANGE, false),
                input.read(TAG_POSITION, Vec3.CODEC).orElse(Vec3.ZERO),
                input.read(TAG_DIMENSION, DIMENSION_CODEC).orElse(null)
            )
        }
    }

    class AltioraData : ValueIOSerializable {
        var altiora: AltioraAbility? = null

        override fun serialize(output: ValueOutput) {
            output.putBoolean(TAG_ALLOWED, altiora != null)
            altiora?.let { output.putInt(TAG_GRACE, it.gracePeriod()) }
        }

        override fun deserialize(input: ValueInput) {
            altiora = if (input.getBooleanOr(TAG_ALLOWED, false)) {
                AltioraAbility(input.getIntOr(TAG_GRACE, 0))
            } else {
                null
            }
        }
    }

    class PigmentData : ValueIOSerializable {
        var pigment: FrozenPigment = FrozenPigment.DEFAULT.get()

        override fun serialize(output: ValueOutput) {
            output.store(TAG_PIGMENT, CompoundTag.CODEC, pigment.serializeToNBT())
        }

        override fun deserialize(input: ValueInput) {
            pigment = input.read(TAG_PIGMENT, CompoundTag.CODEC)
                .map(FrozenPigment::fromNBT)
                .orElseGet(FrozenPigment.DEFAULT)
        }
    }

    class StaffcastImageData : ValueIOSerializable {
        var image: CompoundTag = CompoundTag()

        fun toImage(level: ServerLevel): CastingImage =
            if (image.isEmpty) CastingImage() else CastingImage.loadFromNbt(image, level)

        override fun serialize(output: ValueOutput) {
            output.store(TAG_VM, CompoundTag.CODEC, image)
        }

        override fun deserialize(input: ValueInput) {
            image = input.read(TAG_VM, CompoundTag.CODEC).orElseGet { CompoundTag() }
        }
    }

    class PatternsData : ValueIOSerializable {
        var patterns: List<ResolvedPattern> = emptyList()

        override fun serialize(output: ValueOutput) {
            val list = output.list(TAG_PATTERNS, CompoundTag.CODEC)
            for (pattern in patterns) {
                list.add(pattern.serializeToNBT())
            }
        }

        override fun deserialize(input: ValueInput) {
            patterns = input.listOrEmpty(TAG_PATTERNS, CompoundTag.CODEC).stream()
                .map(ResolvedPattern::fromNBT)
                .toList()
        }
    }

    class BrainsweptData : ValueIOSerializable {
        var brainswept: Boolean = false

        override fun serialize(output: ValueOutput) {
            output.putBoolean(TAG_BRAINSWEPT, brainswept)
        }

        override fun deserialize(input: ValueInput) {
            brainswept = input.getBooleanOr(TAG_BRAINSWEPT, false)
        }
    }

    val FLIGHT: AttachmentType<FlightData> = AttachmentType.serializable(Supplier { FlightData() }).build()
    val SENTINEL: AttachmentType<SentinelData> = AttachmentType.serializable(Supplier { SentinelData() }).build()
    val ALTIORA: AttachmentType<AltioraData> = AttachmentType.serializable(Supplier { AltioraData() }).build()
    val PIGMENT: AttachmentType<PigmentData> = AttachmentType.serializable(Supplier { PigmentData() }).build()
    val STAFFCAST_IMAGE: AttachmentType<StaffcastImageData> =
        AttachmentType.serializable(Supplier { StaffcastImageData() }).build()
    val PATTERNS: AttachmentType<PatternsData> =
        AttachmentType.serializable(Supplier { PatternsData() }).build()
    val BRAINSWEPT: AttachmentType<BrainsweptData> =
        AttachmentType.serializable(Supplier { BrainsweptData() }).build()

    /** hexcassettes：已拥有的卡带槽位与排队咒术。 */
    class CassetteData : ValueIOSerializable {
        val state: CassetteState = CassetteState()

        override fun serialize(output: ValueOutput) {
            output.store(TAG_CASSETTES, CompoundTag.CODEC, state.serialize())
        }

        override fun deserialize(input: ValueInput) {
            state.load(input.read(TAG_CASSETTES, CompoundTag.CODEC).orElseGet { CompoundTag() })
        }
    }

    val CASSETTES: AttachmentType<CassetteData> =
        AttachmentType.serializable(Supplier { CassetteData() }).build()

    /** MoreIotas：聊天前缀。 */
    class ChatPrefixData : ValueIOSerializable {
        var prefix: String? = null

        override fun serialize(output: ValueOutput) {
            prefix?.let { output.putString(TAG_CHAT_PREFIX, it) }
        }

        override fun deserialize(input: ValueInput) {
            prefix = input.getString(TAG_CHAT_PREFIX).orElse(null)
        }
    }

    val CHAT_PREFIX: AttachmentType<ChatPrefixData> =
        AttachmentType.serializable(Supplier { ChatPrefixData() }).build()

    private const val TAG_ALLOWED = "allowed"
    private const val TAG_TIME_LEFT = "time_left"
    private const val TAG_DIMENSION = "dimension"
    private const val TAG_ORIGIN = "origin"
    private const val TAG_RADIUS = "radius"
    private const val TAG_HAS_SENTINEL = "has_sentinel"
    private const val TAG_EXTENDS_RANGE = "extends_range"
    private const val TAG_POSITION = "position"
    private const val TAG_GRACE = "grace_period"
    private const val TAG_PIGMENT = "pigment"
    private const val TAG_VM = "harness"
    private const val TAG_PATTERNS = "patterns"
    private const val TAG_BRAINSWEPT = "brainswept"
    private const val TAG_CASSETTES = "hexcassettes"
    private const val TAG_CHAT_PREFIX = "moreiotas:prefix"
}
