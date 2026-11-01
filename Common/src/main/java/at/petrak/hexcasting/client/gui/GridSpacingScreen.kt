package at.petrak.hexcasting.client.gui

import at.petrak.hexcasting.api.mod.HexConfig
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 探知透镜组的界面：调节咒术网格的点间距，并把结果写回客户端配置。
 */
class GridSpacingScreen : Screen(Component.translatable("gui.hexcasting.grid_spacing.title")) {

    private var spacing = HexConfig.client()?.gridSpacing() ?: MIN_SPACING
    private var dirty = false
    private var dragging = false

    private var ox = 0
    private var oy = 0

    private val previewX get() = ox + (PANEL_W - PREVIEW_W) / 2
    private val previewY get() = oy + 26
    private val trackX get() = ox + 24
    private val trackY get() = oy + 88
    private val trackW get() = PANEL_W - 48
    private val buttonY get() = oy + 100
    private val resetX get() = ox + 40
    private val doneX get() = ox + 104
    private val buttonW get() = 56

    override fun init() {
        super.init()
        ox = (width - PANEL_W) / 2
        oy = (height - PANEL_H) / 2
        spacing = (HexConfig.client()?.gridSpacing() ?: MIN_SPACING).coerceIn(MIN_SPACING, MAX_SPACING)
        dirty = false
        dragging = false
    }

    // ---------------------------------------------------------------- rendering

    override fun extractBackground(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        graphics.fill(0, 0, width, height, 0x66000000)
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)

        graphics.fill(ox - 1, oy - 1, ox + PANEL_W + 1, oy + PANEL_H + 1, PANEL_BORDER)
        graphics.fill(ox, oy, ox + PANEL_W, oy + PANEL_H, PANEL_BG)

        graphics.centeredText(font, title, ox + PANEL_W / 2, oy + 8, ACCENT)

        // live preview of the dot lattice, scaled by the current multiplier
        graphics.fill(previewX, previewY, previewX + PREVIEW_W, previewY + PREVIEW_H, PREVIEW_BG)
        drawPreview(graphics)

        val label = Component.translatable("gui.hexcasting.grid_spacing.label").string
        graphics.text(font, label, trackX, trackY - 11, TEXT, false)
        val value = String.format(Locale.ROOT, "%.2f", spacing)
        graphics.text(font, value, trackX + trackW - font.width(value), trackY - 11, ACCENT, false)

        // slider track + filled part + knob
        graphics.fill(trackX, trackY, trackX + trackW, trackY + TRACK_H, TRACK_BG)
        val knobX = trackX + ((spacing - MIN_SPACING) / (MAX_SPACING - MIN_SPACING) * trackW).roundToInt()
        graphics.fill(trackX, trackY, knobX, trackY + TRACK_H, TRACK_FILL)
        graphics.fill(knobX - 2, trackY - 2, knobX + 2, trackY + TRACK_H + 2, ACCENT)

        drawButton(graphics, resetX, buttonY, buttonW, mouseX, mouseY, "gui.hexcasting.grid_spacing.reset")
        drawButton(graphics, doneX, buttonY, buttonW, mouseX, mouseY, "gui.hexcasting.grid_spacing.done")
    }

    private fun drawPreview(graphics: GuiGraphicsExtractor) {
        val cx = previewX + PREVIEW_W / 2
        val cy = previewY + PREVIEW_H / 2
        val s = PREVIEW_BASE * spacing
        for (r in -1..1) {
            val qFrom = (-1 - r).coerceAtLeast(-1)
            val qTo = (1 - r).coerceAtMost(1)
            for (q in qFrom..qTo) {
                val dotX = (cx + s * (q + r / 2.0)).roundToInt()
                val dotY = (cy + s * (SQRT3 / 2.0) * r).roundToInt()
                graphics.fill(dotX - 1, dotY - 1, dotX + 1, dotY + 1, DOT)
            }
        }
    }

    private fun drawButton(
        graphics: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        w: Int,
        mouseX: Int,
        mouseY: Int,
        key: String,
    ) {
        val hover = isIn(mouseX.toDouble(), mouseY.toDouble(), x, y, w, BUTTON_H)
        graphics.fill(x, y, x + w, y + BUTTON_H, if (hover) BUTTON_HOVER else BUTTON_BG)
        graphics.fill(x, y, x + w, y + 1, PANEL_BORDER)
        graphics.centeredText(font, Component.translatable(key).string, x + w / 2, y + 4, TEXT)
    }

    // ---------------------------------------------------------------- input

    private fun isIn(mx: Double, my: Double, x: Int, y: Int, w: Int, h: Int) =
        mx >= x && mx < x + w && my >= y && my < y + h

    private fun isOnTrack(mx: Double, my: Double) =
        isIn(mx, my, trackX - 3, trackY - 4, trackW + 6, TRACK_H + 8)

    private fun setFromMouse(mx: Double) {
        val t = ((mx - trackX) / trackW).coerceIn(0.0, 1.0)
        val raw = MIN_SPACING + t * (MAX_SPACING - MIN_SPACING)
        val stepped = (raw / STEP).roundToInt() * STEP
        spacing = stepped.coerceIn(MIN_SPACING, MAX_SPACING)
        dirty = true
    }

    private fun commit() {
        if (!dirty) return
        HexConfig.client()?.setGridSpacing(spacing)
        dirty = false
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (event.button() == 1) {
            onClose()
            return true
        }
        if (event.button() == 0) {
            if (isOnTrack(event.x, event.y)) {
                dragging = true
                setFromMouse(event.x)
                return true
            }
            if (isIn(event.x, event.y, resetX, buttonY, buttonW, BUTTON_H)) {
                spacing = 1.0
                dirty = true
                commit()
                return true
            }
            if (isIn(event.x, event.y, doneX, buttonY, buttonW, BUTTON_H)) {
                onClose()
                return true
            }
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseDragged(event: MouseButtonEvent, dragX: Double, dragY: Double): Boolean {
        if (dragging) {
            setFromMouse(event.x)
            return true
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (dragging) {
            dragging = false
            commit()
            return true
        }
        return super.mouseReleased(event)
    }

    override fun onClose() {
        commit()
        super.onClose()
    }

    companion object {
        private const val PANEL_W = 200
        private const val PANEL_H = 122
        private const val PREVIEW_W = 120
        private const val PREVIEW_H = 44
        private const val PREVIEW_BASE = 11.0
        private const val TRACK_H = 8
        private const val BUTTON_H = 16

        private const val MIN_SPACING = 0.5
        private const val MAX_SPACING = 2.0
        private const val STEP = 0.05

        private val SQRT3 = Math.sqrt(3.0)

        private const val PANEL_BG = 0xF01A1226.toInt()
        private const val PANEL_BORDER = 0xFF8D6ACC.toInt()
        private const val PREVIEW_BG = 0xFF100A1E.toInt()
        private const val TRACK_BG = 0xFF3A2D52.toInt()
        private const val TRACK_FILL = 0xFF8D6ACC.toInt()
        private const val BUTTON_BG = 0xFF2A1E40.toInt()
        private const val BUTTON_HOVER = 0xFF43305F.toInt()
        private const val ACCENT = 0xFFE0B04A.toInt()
        private const val TEXT = 0xFFEAD9FF.toInt()
        private const val DOT = 0xFFCFA0F3.toInt()
    }
}
