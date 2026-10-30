package gay.`object`.hexdebug.client

import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.media.HexDebugAttributes
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.util.ARGB
import kotlin.math.sin

/**
 * Personal media bar drawn just above the hotbar, ported from HexOverpowered's HUD.
 */
object PersonalMediaHud {
    private val MEDIA_BAR = HexDebug.id("textures/gui/media_bar.png")
    private const val TEX_WIDTH = 182
    private const val TEX_HEIGHT = 5

    private val mediaAttribute by lazy { HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA) }
    private val maxMediaAttribute by lazy { HexDebugAttributes.holder(HexDebugAttributes.PERSONAL_MEDIA_MAX) }

    fun render(graphics: GuiGraphicsExtractor) {
        val player = Minecraft.getInstance().player ?: return

        val media = player.getAttributeValue(mediaAttribute).toLong()
        val maxMedia = player.getAttributeValue(maxMediaAttribute).toLong()
        if (media <= 0 || maxMedia <= 0) return

        val x = graphics.guiWidth() / 2 - TEX_WIDTH / 2
        val y = graphics.guiHeight() - 29
        val width = (TEX_WIDTH.toDouble() * media / maxMedia).toInt().coerceIn(1, TEX_WIDTH)

        // Shifted from Botania's mana bar: a slow breathing on the alpha channel.
        val alpha = (sin(System.currentTimeMillis() / 300.0) * 0.5 + 0.5).toFloat()
        val color = ARGB.color((alpha * 255).toInt(), 255, 153, 204)
        graphics.blit(
            RenderPipelines.GUI_TEXTURED, MEDIA_BAR, x, y, 0f, 0f,
            width, TEX_HEIGHT, TEX_WIDTH, TEX_HEIGHT, color
        )
    }
}
