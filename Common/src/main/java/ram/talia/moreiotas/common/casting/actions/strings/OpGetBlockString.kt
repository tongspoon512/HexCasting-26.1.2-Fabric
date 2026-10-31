package ram.talia.moreiotas.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.asActionResult
import at.petrak.hexcasting.api.casting.castables.ConstMediaAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import at.petrak.hexcasting.api.casting.iota.ListIota
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.entity.LecternBlockEntity
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.level.block.entity.SignText
import ram.talia.moreiotas.api.asActionResult
import ram.talia.moreiotas.api.casting.iota.StringIota

/**
 * 读取告示牌或讲台上书页的文本；告示牌返回单个带换行的字符串，书返回字符串列表。
 */
object OpGetBlockString : ConstMediaAction {
    override val argc = 1

    override fun execute(args: List<Iota>, env: CastingEnvironment): List<Iota> {
        val pos = args.getBlockPos(0, argc)
        env.assertVecInRange(pos.center)

        val blockEntity = env.world.getBlockEntity(pos)

        if (blockEntity is SignBlockEntity) {
            val text = blockEntity.getText(true)
            var string = text.getMessage(0, false).string
            for (i in 1 until SignText.LINES) {
                string += "\n" + text.getMessage(i, false).string
            }
            return string.asActionResult
        }

        if (blockEntity is LecternBlockEntity) {
            val book = blockEntity.book

            if (book.`is`(Items.WRITABLE_BOOK)) {
                val content = book.get(DataComponents.WRITABLE_BOOK_CONTENT) ?: return null.asActionResult
                return listOf(ListIota(content.pages().map { StringIota.make(it.raw()) }))
            }
            if (book.`is`(Items.WRITTEN_BOOK)) {
                val content = book.get(DataComponents.WRITTEN_BOOK_CONTENT) ?: return null.asActionResult
                return listOf(ListIota(content.getPages(false).map { StringIota.make(it.string) }))
            }
        }

        return null.asActionResult
    }
}
