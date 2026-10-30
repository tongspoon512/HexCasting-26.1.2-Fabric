@file:JvmName("NBTHelper")

package at.petrak.hexcasting.api.utils

import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.*
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.CustomData
import java.util.*

private inline fun <T : Any, K, E> T?.getIf(key: K, predicate: T?.(K) -> Boolean, get: T.(K) -> E): E? =
    getIf(key, predicate, get, null)

private inline fun <T : Any, K, E> T?.getIf(key: K, predicate: T?.(K) -> Boolean, get: T.(K) -> E, default: E): E {
    if (this != null && predicate(key))
        return get(key)
    return default
}

// ======================================================================================================== CompoundTag

// 26.1 removed `CompoundTag.contains(key, type)` and `Tag.TAG_ANY_NUMERIC`,
// so types are identified by the tag's id instead.

private fun Tag?.hasId(id: Byte) = this != null && this.getId() == id

// TAG_BYTE(1) .. TAG_DOUBLE(6) are all numeric
private fun Tag?.isNumeric() = this != null && this.getId() in Tag.TAG_BYTE..Tag.TAG_DOUBLE

private fun Tag?.isUUIDArray(): Boolean {
    val t = this as? IntArrayTag ?: return false
    return t.getAsIntArray().size == 4
}

// Checks for containment

fun CompoundTag?.hasNumber(key: String) = get(key).isNumeric()
fun CompoundTag?.hasByte(key: String) = get(key).hasId(Tag.TAG_BYTE)
fun CompoundTag?.hasShort(key: String) = get(key).hasId(Tag.TAG_SHORT)
fun CompoundTag?.hasInt(key: String) = get(key).hasId(Tag.TAG_INT)
fun CompoundTag?.hasLong(key: String) = get(key).hasId(Tag.TAG_LONG)
fun CompoundTag?.hasFloat(key: String) = get(key).hasId(Tag.TAG_FLOAT)
fun CompoundTag?.hasDouble(key: String) = get(key).hasId(Tag.TAG_DOUBLE)
fun CompoundTag?.hasLongArray(key: String) = get(key).hasId(Tag.TAG_LONG_ARRAY)
fun CompoundTag?.hasIntArray(key: String) = get(key).hasId(Tag.TAG_INT_ARRAY)
fun CompoundTag?.hasByteArray(key: String) = get(key).hasId(Tag.TAG_BYTE_ARRAY)
fun CompoundTag?.hasCompound(key: String) = get(key).hasId(Tag.TAG_COMPOUND)
fun CompoundTag?.hasString(key: String) = get(key).hasId(Tag.TAG_STRING)
fun CompoundTag?.hasList(key: String) = get(key).hasId(Tag.TAG_LIST)

fun CompoundTag?.hasList(key: String, objType: Int) = hasList(key, objType.toByte())
fun CompoundTag?.hasList(key: String, objType: Byte): Boolean {
    val lt = this?.get(key) as? ListTag ?: return false
    // 26.1's ListTag no longer exposes the element type; infer it from the first element.
    return lt.isEmpty() || lt[0]?.getId() == objType
}

fun CompoundTag?.hasUUID(key: String) = get(key).isUUIDArray()

fun CompoundTag?.contains(key: String, id: Byte) = get(key).hasId(id)
fun CompoundTag?.contains(key: String, id: Int) = contains(key, id.toByte())
fun CompoundTag?.contains(key: String) = this != null && this.contains(key)

// Puts

fun CompoundTag?.putBoolean(key: String, value: Boolean) = this?.putBoolean(key, value)
fun CompoundTag?.putByte(key: String, value: Byte) = this?.putByte(key, value)
fun CompoundTag?.putShort(key: String, value: Short) = this?.putShort(key, value)
fun CompoundTag?.putInt(key: String, value: Int) = this?.putInt(key, value)
fun CompoundTag?.putLong(key: String, value: Long) = this?.putLong(key, value)
fun CompoundTag?.putFloat(key: String, value: Float) = this?.putFloat(key, value)
fun CompoundTag?.putDouble(key: String, value: Double) = this?.putDouble(key, value)
fun CompoundTag?.putLongArray(key: String, value: LongArray) = this?.putLongArray(key, value)
fun CompoundTag?.putIntArray(key: String, value: IntArray) = this?.putIntArray(key, value)
fun CompoundTag?.putByteArray(key: String, value: ByteArray) = this?.putByteArray(key, value)
fun CompoundTag?.putCompound(key: String, value: CompoundTag) = this?.put(key, value)
fun CompoundTag?.putString(key: String, value: String) = this?.putString(key, value)
fun CompoundTag?.putList(key: String, value: ListTag) = this?.put(key, value)
fun CompoundTag?.putUUID(key: String, value: UUID) = this?.put(key, uuidTag(value))
fun CompoundTag?.put(key: String, value: Tag) = this?.put(key, value)

// Remove

fun CompoundTag?.remove(key: String) = this?.remove(key)

// Gets

@JvmOverloads
fun CompoundTag?.getBoolean(key: String, defaultExpected: Boolean = false) =
    this?.getBooleanOr(key, defaultExpected) ?: defaultExpected

@JvmOverloads
fun CompoundTag?.getByte(key: String, defaultExpected: Byte = 0) =
    this?.getByteOr(key, defaultExpected) ?: defaultExpected

@JvmOverloads
fun CompoundTag?.getShort(key: String, defaultExpected: Short = 0) =
    this?.getShortOr(key, defaultExpected) ?: defaultExpected

@JvmOverloads
fun CompoundTag?.getInt(key: String, defaultExpected: Int = 0) =
    this?.getIntOr(key, defaultExpected) ?: defaultExpected

@JvmOverloads
fun CompoundTag?.getLong(key: String, defaultExpected: Long = 0) =
    this?.getLongOr(key, defaultExpected) ?: defaultExpected

@JvmOverloads
fun CompoundTag?.getFloat(key: String, defaultExpected: Float = 0f) =
    this?.getFloatOr(key, defaultExpected) ?: defaultExpected

@JvmOverloads
fun CompoundTag?.getDouble(key: String, defaultExpected: Double = 0.0) =
    this?.getDoubleOr(key, defaultExpected) ?: defaultExpected

fun CompoundTag?.getLongArray(key: String): LongArray? = (this?.get(key) as? LongArrayTag)?.getAsLongArray()
fun CompoundTag?.getIntArray(key: String): IntArray? = (this?.get(key) as? IntArrayTag)?.getAsIntArray()
fun CompoundTag?.getByteArray(key: String): ByteArray? = (this?.get(key) as? ByteArrayTag)?.getAsByteArray()
fun CompoundTag?.getCompound(key: String): CompoundTag? = this?.get(key) as? CompoundTag

fun CompoundTag?.getString(key: String): String? = (this?.get(key) as? StringTag)?.`asString`()?.orElse(null)
fun CompoundTag?.getList(key: String, objType: Byte): ListTag? = getList(key, objType.toInt())
fun CompoundTag?.getList(key: String, objType: Int): ListTag? = this?.get(key) as? ListTag
fun CompoundTag?.getUUID(key: String): UUID? = get(key)?.takeIf { it.isUUIDArray() }?.asUUID
fun CompoundTag?.get(key: String): Tag? = this?.get(key)

@JvmSynthetic
@JvmName("getListByByte")
fun CompoundTag.getList(key: String, objType: Byte): ListTag? = getList(key, objType.toInt())

// Get-or-create

fun CompoundTag.getOrCreateCompound(key: String): CompoundTag =
    // NB: the nullable-receiver extension -- 26.1's CompoundTag.getCompound member returns Optional and would
    // shadow the extension here.
    (this as CompoundTag?).getCompound(key) ?: CompoundTag().also { putCompound(key, it) }

fun CompoundTag.getOrCreateList(key: String, objType: Byte) = getOrCreateList(key, objType.toInt())

fun CompoundTag.getOrCreateList(key: String, objType: Int): ListTag =
    if (hasList(key, objType)) (getList(key, objType) ?: ListTag()) else ListTag().also { putList(key, it) }

// ================================================================================================================ Tag

val Tag.asBoolean get() = (this as? NumericTag)?.byteValue() != 0.toByte()
val Tag.asByte get() = (this as? NumericTag)?.byteValue() ?: 0.toByte()
val Tag.asShort get() = (this as? NumericTag)?.shortValue() ?: 0.toShort()
val Tag.asInt get() = (this as? NumericTag)?.intValue() ?: 0
val Tag.asLong get() = (this as? NumericTag)?.longValue() ?: 0L
val Tag.asFloat get() = (this as? NumericTag)?.floatValue() ?: 0F
val Tag.asDouble get() = (this as? NumericTag)?.doubleValue() ?: 0.0

val Tag.asLongArray: LongArray
    get() = when (this) {
        is LongArrayTag -> this.getAsLongArray()
        is IntArrayTag -> this.getAsIntArray().map { it.toLong() }.toLongArray()
        is ByteArrayTag -> this.getAsByteArray().map { it.toLong() }.toLongArray()
        else -> LongArray(0)
    }

val Tag.asIntArray: IntArray
    get() = when (this) {
        is IntArrayTag -> this.getAsIntArray()
        is LongArrayTag -> this.getAsLongArray().map { it.toInt() }.toIntArray()
        is ByteArrayTag -> this.getAsByteArray().map { it.toInt() }.toIntArray()
        else -> IntArray(0)
    }

val Tag.asByteArray: ByteArray
    get() = when (this) {
        is ByteArrayTag -> this.getAsByteArray()
        is LongArrayTag -> this.getAsLongArray().map { it.toByte() }.toByteArray()
        is IntArrayTag -> this.getAsIntArray().map { it.toByte() }.toByteArray()
        else -> ByteArray(0)
    }

val Tag.asCompound get() = this as? CompoundTag ?: CompoundTag()

// asString is defined in Tag
val Tag.asList get() = this as? ListTag ?: ListTag()
val Tag.asUUID: UUID get() = uuidFromTag(this)

private fun uuidTag(uuid: UUID) = IntArrayTag(
    intArrayOf(
        (uuid.mostSignificantBits ushr 32).toInt(),
        uuid.mostSignificantBits.toInt(),
        (uuid.leastSignificantBits ushr 32).toInt(),
        uuid.leastSignificantBits.toInt(),
    )
)

private fun uuidFromTag(tag: Tag): UUID {
    val arr = (tag as? IntArrayTag)?.getAsIntArray() ?: return UUID(0, 0)
    if (arr.size != 4) return UUID(0, 0)
    val most = (arr[0].toLong() shl 32) or (arr[1].toLong() and 0xFFFFFFFFL)
    val least = (arr[2].toLong() shl 32) or (arr[3].toLong() and 0xFFFFFFFFL)
    return UUID(most, least)
}

// ========================================================================================================== ItemStack

// 26.1 removed the free-form `ItemStack.tag`. All of this mod's item data lived there, so it now lives in the
// vanilla `minecraft:custom_data` component. Vanilla treats component values as immutable -- copying a stack
// shares them -- so reads copy out and every write goes through `CustomData.update`.

private fun ItemStack.customTag(): CompoundTag? = this.get(DataComponents.CUSTOM_DATA)?.copyTag()

@PublishedApi
internal fun ItemStack.mutateTag(block: (CompoundTag) -> Unit) {
    CustomData.update(DataComponents.CUSTOM_DATA, this) { block(it) }
    if (this.get(DataComponents.CUSTOM_DATA)?.isEmpty() != false) this.remove(DataComponents.CUSTOM_DATA)
}

/**
 * Read-modify-write on a sub-compound of this stack's custom data. Use this instead of mutating the result of
 * [getCompound] in place -- those are detached copies.
 */
inline fun ItemStack.updateCompound(key: String, crossinline block: (CompoundTag) -> Unit) =
    mutateTag { outer ->
        val sub = outer.getOrCreateCompound(key)
        block(sub)
        if (sub.isEmpty()) outer.remove(key)
    }

/**
 * Read-modify-write on a sub-list of this stack's custom data.
 */
inline fun ItemStack.updateList(key: String, objType: Int, crossinline block: (ListTag) -> Unit) =
    mutateTag { outer ->
        val sub = outer.getOrCreateList(key, objType)
        block(sub)
        if (sub.isEmpty()) outer.remove(key)
    }

// Checks for containment

fun ItemStack.hasNumber(key: String) = customTag().hasNumber(key)
fun ItemStack.hasByte(key: String) = customTag().hasByte(key)
fun ItemStack.hasShort(key: String) = customTag().hasShort(key)
fun ItemStack.hasInt(key: String) = customTag().hasInt(key)
fun ItemStack.hasLong(key: String) = customTag().hasLong(key)
fun ItemStack.hasFloat(key: String) = customTag().hasFloat(key)
fun ItemStack.hasDouble(key: String) = customTag().hasDouble(key)
fun ItemStack.hasLongArray(key: String) = customTag().hasLongArray(key)
fun ItemStack.hasIntArray(key: String) = customTag().hasIntArray(key)
fun ItemStack.hasByteArray(key: String) = customTag().hasByteArray(key)
fun ItemStack.hasCompound(key: String) = customTag().hasCompound(key)
fun ItemStack.hasString(key: String) = customTag().hasString(key)
fun ItemStack.hasList(key: String) = customTag().hasList(key)
fun ItemStack.hasList(key: String, objType: Int) = customTag().hasList(key, objType)
fun ItemStack.hasList(key: String, objType: Byte) = customTag().hasList(key, objType)
fun ItemStack.hasUUID(key: String) = customTag().hasUUID(key)

@JvmName("contains")
fun ItemStack.containsTag(key: String) = customTag().contains(key)

@JvmName("contains")
fun ItemStack.containsTag(key: String, id: Byte) = customTag().contains(key, id)

@JvmName("contains")
fun ItemStack.containsTag(key: String, id: Int) = customTag().contains(key, id)

// Puts

fun ItemStack.putBoolean(key: String, value: Boolean) = mutateTag { it.putBoolean(key, value) }
fun ItemStack.putByte(key: String, value: Byte) = mutateTag { it.putByte(key, value) }
fun ItemStack.putShort(key: String, value: Short) = mutateTag { it.putShort(key, value) }
fun ItemStack.putInt(key: String, value: Int) = mutateTag { it.putInt(key, value) }
fun ItemStack.putLong(key: String, value: Long) = mutateTag { it.putLong(key, value) }
fun ItemStack.putFloat(key: String, value: Float) = mutateTag { it.putFloat(key, value) }
fun ItemStack.putDouble(key: String, value: Double) = mutateTag { it.putDouble(key, value) }

fun ItemStack.putLongArray(key: String, value: LongArray) = mutateTag { it.putLongArray(key, value) }
fun ItemStack.putIntArray(key: String, value: IntArray) = mutateTag { it.putIntArray(key, value) }
fun ItemStack.putByteArray(key: String, value: ByteArray) = mutateTag { it.putByteArray(key, value) }
fun ItemStack.putCompound(key: String, value: CompoundTag) = putTag(key, value)
fun ItemStack.putString(key: String, value: String) = mutateTag { it.putString(key, value) }
fun ItemStack.putList(key: String, value: ListTag) = putTag(key, value)
fun ItemStack.putUUID(key: String, value: UUID) = mutateTag { it.putUUID(key, value) }

@JvmName("put")
fun ItemStack.putTag(key: String, value: Tag) = mutateTag { it.put(key, value) }

// Remove

fun ItemStack.remove(key: String) = mutateTag { it.remove(key) }

// Gets

@JvmOverloads
fun ItemStack.getBoolean(key: String, defaultExpected: Boolean = false) = customTag().getBoolean(key, defaultExpected)

@JvmOverloads
fun ItemStack.getByte(key: String, defaultExpected: Byte = 0) = customTag().getByte(key, defaultExpected)

@JvmOverloads
fun ItemStack.getShort(key: String, defaultExpected: Short = 0) = customTag().getShort(key, defaultExpected)

@JvmOverloads
fun ItemStack.getInt(key: String, defaultExpected: Int = 0) = customTag().getInt(key, defaultExpected)

@JvmOverloads
fun ItemStack.getLong(key: String, defaultExpected: Long = 0) = customTag().getLong(key, defaultExpected)

@JvmOverloads
fun ItemStack.getFloat(key: String, defaultExpected: Float = 0f) = customTag().getFloat(key, defaultExpected)

@JvmOverloads
fun ItemStack.getDouble(key: String, defaultExpected: Double = 0.0) = customTag().getDouble(key, defaultExpected)

fun ItemStack.getLongArray(key: String) = customTag().getLongArray(key)
fun ItemStack.getIntArray(key: String) = customTag().getIntArray(key)
fun ItemStack.getByteArray(key: String) = customTag().getByteArray(key)
fun ItemStack.getCompound(key: String) = customTag().getCompound(key)
fun ItemStack.getString(key: String) = customTag().getString(key)
fun ItemStack.getList(key: String, objType: Int) = customTag().getList(key, objType)
fun ItemStack.getUUID(key: String) = customTag().getUUID(key)

@JvmName("get")
fun ItemStack.getTag(key: String) = customTag().get(key)
