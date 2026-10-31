package ram.talia.moreiotas.common.casting.actions.strings

import at.petrak.hexcasting.api.casting.ParticleSpray
import at.petrak.hexcasting.api.casting.RenderedSpell
import at.petrak.hexcasting.api.casting.castables.SpellAction
import at.petrak.hexcasting.api.casting.eval.CastingEnvironment
import at.petrak.hexcasting.api.casting.getBlockPos
import at.petrak.hexcasting.api.casting.iota.Iota
import com.mojang.datafixers.util.Either
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.network.Filterable
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.WritableBookContent
import net.minecraft.world.level.block.entity.LecternBlockEntity
import net.minecraft.world.level.block.entity.SignBlockEntity
import net.minecraft.world.level.block.entity.SignText
import net.minecraft.world.phys.Vec3
import ram.talia.moreiotas.api.getStringOrList
import ram.talia.moreiotas.api.mod.MoreIotasConfig

object OpSetBlockString : SpellAction {
    override val argc = 2

    override fun execute(args: List<Iota>, env: CastingEnvironment): SpellAction.Result {
        val pos = args.getBlockPos(0, argc)
        val stringOrList = args.getStringOrList(1, argc)

        env.assertVecInRange(pos.center)

        return SpellAction.Result(
            Spell(pos, stringOrList),
            MoreIotasConfig.setBlockStringCost,
            listOf(ParticleSpray.burst(Vec3.atCenterOf(pos), 1.0))
        )
    }

    private data class Spell(val pos: BlockPos, val stringOrList: Either<String, List<String>>) : RenderedSpell {
        override fun cast(env: CastingEnvironment) {
            val blockEntity = env.world.getBlockEntity(pos)

            when (blockEntity) {
                is SignBlockEntity -> {
                    val editor = blockEntity.getPlayerWhoMayEdit()
                    if (editor != null && editor != env.castingEntity?.uuid) {
                        return
                    }

                    val lines = stringOrList.map(
                        { string -> string.split("\n") },
                        { list -> list.flatMap { it.split("\n") } },
                    )

                    blockEntity.setText(makeSignText(lines), true)
                }

                is LecternBlockEntity -> {
                    val stack = blockEntity.book
                    if (!stack.`is`(Items.WRITABLE_BOOK)) {
                        return
                    }

                    val book = stack.copy()
                    val pages = stringOrList.map(
                        { string -> listOf(string) },
                        { list -> list },
                    )
                    book.set(
                        DataComponents.WRITABLE_BOOK_CONTENT,
                        WritableBookContent(pages.map { Filterable.passThrough(it) })
                    )
                    blockEntity.setBook(book)
                }

                else -> return
            }

            blockEntity.setChanged()
            val blockState = env.world.getBlockState(pos)
            env.world.sendBlockUpdated(pos, blockState, blockState, 3)
        }

        private fun makeSignText(lines: List<String>): SignText {
            val messages = arrayOf(
                Component.literal(lines.getOrElse(0) { "" }),
                lines.getOrNull(1)?.let { Component.literal(it) } ?: Component.empty(),
                lines.getOrNull(2)?.let { Component.literal(it) } ?: Component.empty(),
                lines.getOrNull(3)?.let { Component.literal(it) } ?: Component.empty()
            )
            return SignText(messages, messages.copyOf(), DyeColor.BLACK, false)
        }
    }
}
