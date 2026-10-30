package at.petrak.hexcasting.client.guidebook

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/** 按原始纹理尺寸做 UV 缩放，避免猜贴图分辨率。 */
object GuideTextures {
    private val sizes = HashMap<Identifier, Pair<Int, Int>>()

    fun size(id: Identifier): Pair<Int, Int> {
        sizes[id]?.let { return it }
        val loaded = try {
            Minecraft.getInstance().resourceManager.getResource(id).map { resource ->
                resource.open().use { stream ->
                    val image: BufferedImage? = ImageIO.read(stream)
                    image?.let { it.width to it.height }
                }
            }.orElse(null)
        } catch (e: Exception) {
            null
        } ?: (16 to 16)
        sizes[id] = loaded
        return loaded
    }

    fun blit(graphics: GuiGraphicsExtractor, id: Identifier, x: Int, y: Int, width: Int, height: Int) {
        val (texW, texH) = size(id)
        graphics.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, width, height, texW, texH)
    }
}
