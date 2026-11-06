package miyucomics.hexcassettes.client

import at.petrak.hexcasting.fabric.network.FabricPacketHandler
import miyucomics.hexcassettes.HexCassettes
import miyucomics.hexcassettes.HexCassetteSounds
import miyucomics.hexcassettes.network.MsgCassetteRemoveC2S
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.sounds.SoundEvent

/**
 * 磁带总览界面：每盒已拥有的磁带占一个 32×16 的槽位。
 *
 * 点击忙碌的槽位会移除该磁带；点击空闲槽位只会播放失败音效。
 */
class CassetteScreen : Screen(Component.literal("Cassettes")) {
    private val slotX: Int get() = (width - SLOT_W) / 2

    private fun slotY(index: Int): Int = TOP + index * (SLOT_H + GAP)

    override fun isPauseScreen(): Boolean = false

    override fun extractBackground(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        graphics.fill(0, 0, width, height, BACKGROUND)
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)

        graphics.centeredText(font, title, width / 2, 10, TITLE_COLOR)

        val active = CassetteClientStorage.activeCassettes
        for (i in 0 until CassetteClientStorage.ownedCassettes) {
            val y = slotY(i)
            graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, slotX, y, 0f, 0f, SLOT_W, SLOT_H, SLOT_W, SLOT_H)

            val label = if (i < active.size) Component.literal(active[i]) else FREE_TEXT
            graphics.text(font, label, slotX + SLOT_W + 6, y + 4, TEXT_COLOR, false)
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() == 0) {
            val index = indexAt(event.x, event.y)
            if (index >= 0) {
                val active = CassetteClientStorage.activeCassettes
                if (index < active.size) {
                    playSound(HexCassetteSounds.CASSETTE_EJECT, 3.0f)
                    FabricPacketHandler.sendToServer(MsgCassetteRemoveC2S(active[index]))
                } else {
                    playSound(HexCassetteSounds.CASSETTE_FAIL, 3.0f)
                }
                return true
            }
        }
        return super.mouseClicked(event, doubleClick)
    }

    private fun indexAt(mouseX: Double, mouseY: Double): Int {
        if (mouseX < slotX || mouseX >= slotX + SLOT_W) return -1
        for (i in 0 until CassetteClientStorage.ownedCassettes) {
            val y = slotY(i)
            if (mouseY >= y && mouseY < y + SLOT_H) return i
        }
        return -1
    }

    private fun playSound(sound: SoundEvent, pitch: Float) {
        Minecraft.getInstance().soundManager.play(SimpleSoundInstance.forUI(sound, pitch))
    }

    companion object {
        private val TEXTURE: Identifier = HexCassettes.id("textures/cassette.png")
        private val FREE_TEXT: Component = Component.translatable("hexcassettes.free")
            .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)

        private const val SLOT_W = 32
        private const val SLOT_H = 16
        private const val GAP = 9
        private const val TOP = 28

        private const val BACKGROUND = 0xCC101018.toInt()
        private const val TITLE_COLOR = 0xFFEAD9FF.toInt()
        private const val TEXT_COLOR = -1
    }
}
