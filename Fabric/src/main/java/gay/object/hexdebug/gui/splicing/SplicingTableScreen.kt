package gay.`object`.hexdebug.gui.splicing

import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.client.gui.GuiSpellcasting
import at.petrak.hexcasting.fabric.network.FabricPacketHandler
import com.mojang.blaze3d.platform.InputConstants
import gay.`object`.hexdebug.HexDebug
import gay.`object`.hexdebug.networking.MsgSplicingTableActionC2S
import gay.`object`.hexdebug.networking.MsgSplicingTableDrawPatternC2S
import gay.`object`.hexdebug.networking.MsgSplicingTableNewDataS2C
import gay.`object`.hexdebug.networking.MsgSplicingTableSelectC2S
import gay.`object`.hexdebug.networking.MsgSplicingTableViewC2S
import gay.`object`.hexdebug.splicing.IOTA_BUTTONS
import gay.`object`.hexdebug.splicing.Selection
import gay.`object`.hexdebug.splicing.SplicingTableAction
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.PreeditEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.player.Inventory
import java.awt.Color
import kotlin.math.abs

/**
 * The splicing table GUI: shows the nine visible iotas, supports paging, selection and editing.
 * The mini staff grid and the enlarged features come later.
 */
class SplicingTableScreen(
    menu: SplicingTableMenu,
    inventory: Inventory,
    title: Component,
) : AbstractContainerScreen<SplicingTableMenu>(menu, inventory, title, 192, 193) {

    private val data get() = menu.clientView
    private val selection get() = menu.selection
    private val viewStartIndex get() = menu.viewStartIndex

    private val hasStaffItem get() = menu.staffSlot.hasItem()

    private val guiSpellcasting = GuiSpellcasting(
        InteractionHand.MAIN_HAND, mutableListOf(), listOf(), null, 1
    )

    // staff grid geometry; sizes are multiples of 32 so the frame pieces tile cleanly
    private val staffWidth = 32 * 5
    private val staffHeight = 32 * 5
    private val staffOffsetY = ((32 * 6) - staffHeight) / 2
    private val staffMaxXOffset = -14
    private val staffMinXOffset = staffMaxXOffset - staffWidth
    private val staffMinYOffset = staffOffsetY
    private val staffMaxYOffset = staffMinYOffset + staffHeight
    private val staffSlotWidth = 23
    private val staffSlotHeight = 24
    private val staffSlotMinXOffset = -24
    private val staffSlotMinYOffset = 165

    private val staffMinX get() = leftPos + staffMinXOffset
    private val staffMaxX get() = leftPos + staffMaxXOffset
    private val staffMinY get() = topPos + staffMinYOffset
    private val staffMaxY get() = topPos + staffMaxYOffset
    private val staffSlotMinX get() = leftPos + staffSlotMinXOffset
    private val staffSlotMinY get() = topPos + staffSlotMinYOffset

    private val clearGridButtonX get() = (staffMinX + staffMaxX) / 2 - 19
    private val clearGridButtonY get() = staffMaxY - 10

    private var clearGridButton: ClearGridButton? = null

    override fun init() {
        super.init()
        titleLabelX = (imageWidth - font.width(title)) / 2

        guiSpellcasting.patternSink = { pattern ->
            FabricPacketHandler.sendToServer(MsgSplicingTableDrawPatternC2S(pattern))
        }
        guiSpellcasting.embed(staffWidth, staffHeight)
        // The search/alias overlay is drawn by this screen outside the grid scissor.
        guiSpellcasting.deferOverlay = true
        // ...and may use the whole table area, not just the 160x160 mini grid.
        guiSpellcasting.overlayBoundsWidth = -staffMinXOffset + imageWidth + 46
        guiSpellcasting.overlayBoundsHeight = imageHeight - staffMinYOffset

        clearGridButton = ClearGridButton().also {
            it.visible = hasStaffItem
            addRenderableWidget(it)
        }

        addRenderableWidget(PageButton(leftPos + 4, topPos + 25, -1))
        addRenderableWidget(PageButton(leftPos + 178, topPos + 25, 1))

        addActionButton(67, 57, 18, 16, 256, 48, SplicingTableAction.NUDGE_LEFT)
        addActionButton(107, 57, 18, 16, 276, 48, SplicingTableAction.NUDGE_RIGHT)
        addActionButton(67, 79, 18, 16, 256, 66, SplicingTableAction.DELETE)
        addActionButton(107, 79, 18, 16, 276, 66, SplicingTableAction.DUPLICATE)

        addActionButton(144, 64, 18, 11, 284, 16, SplicingTableAction.SELECT_NONE)
        addActionButton(165, 64, 18, 11, 305, 16, SplicingTableAction.SELECT_ALL)
        addActionButton(144, 77, 18, 11, 284, 29, SplicingTableAction.UNDO)
        addActionButton(165, 77, 18, 11, 305, 29, SplicingTableAction.REDO)

        addActionButton(27, 64, 11, 11, 256, 16, SplicingTableAction.CUT)
        addActionButton(40, 64, 11, 11, 269, 16, SplicingTableAction.COPY)
        addActionButton(27, 77, 24, 11, 256, 29, SplicingTableAction.PASTE_SPLAT, SplicingTableAction.PASTE_VERBATIM)

        // Edge buttons go first so their thin strips win the overlap with the 18px iota
        // buttons; otherwise clicks between two iotas always land on a neighbouring iota.
        for (offset in 0..IOTA_BUTTONS) {
            addRenderableWidget(EdgeButton(offset))
        }
        for (offset in 0 until IOTA_BUTTONS) {
            addRenderableWidget(IotaButton(offset))
            addRenderableOnly(SelectionOverlay(offset))
        }
    }

    fun receiveData(msg: MsgSplicingTableNewDataS2C) {
        menu.receiveData(msg)
    }

    // ---------------------------------------------------------------- rendering

    override fun extractBackground(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick)
        // keep the clear-grid button in sync with the staff slot (widgets that are invisible
        // are not rendered, so this can't be done in the button's own render method)
        clearGridButton?.visible = hasStaffItem

        // main gui + right-hand media slot column
        blit(graphics, leftPos, topPos, 256, 128, imageWidth + 46, imageHeight)

        // staff grid + patterns (only when a staff is in the staff slot)
        if (hasStaffItem) {
            // faint grid background
            blitRepeating(
                graphics, staffMinX, staffMinY, 240, 0, 8, 8,
                staffWidth / 8, staffHeight / 8, 0x40FFFFFF
            )

            // patterns, clipped to the grid
            val pose = graphics.pose()
            pose.pushMatrix()
            pose.translate(staffMinX.toFloat(), staffMinY.toFloat())
            graphics.enableScissor(staffMinX, staffMinY, staffMaxX, staffMaxY)
            // the pattern mesh is rendered from the GuiRenderState, so it does not inherit the
            // immediate scissor above; clip it explicitly as well
            guiSpellcasting.meshScissor = ScreenRectangle(staffMinX, staffMinY, staffWidth, staffHeight)
            guiSpellcasting.extractRenderState(
                graphics, mouseX - staffMinX, mouseY - staffMinY, partialTick
            )
            guiSpellcasting.meshScissor = null
            graphics.disableScissor()
            pose.popMatrix()

            // frame
            blitRepeating(
                graphics, staffMinX, staffMinY - 7, 208, 1, 32, 10,
                staffWidth / 32, 1
            )
            blitRepeating(
                graphics, staffMinX, staffMaxY - 3, 208, 45, 32, 10,
                staffWidth / 32, 1
            )
            blitRepeating(
                graphics, staffMinX - 7, staffMinY, 201, 12, 10, 32,
                1, staffHeight / 32
            )
            blitRepeating(
                graphics, staffMaxX - 3, staffMinY, 237, 12, 10, 32,
                1, staffHeight / 32
            )

            // corners
            blit(graphics, staffMinX - 7, staffMinY - 8, 176, 0, 8, 9)
            blit(graphics, staffMaxX - 1, staffMinY - 7, 184, 1, 9, 8)
            blit(graphics, staffMinX - 8, staffMaxY - 1, 175, 9, 9, 8)
            blit(graphics, staffMaxX - 1, staffMaxY - 1, 184, 9, 8, 9)
        }

        // staff item slot (empty icon when no staff is present)
        if (hasStaffItem) {
            blit(graphics, staffSlotMinX, staffSlotMinY, 232, 293, staffSlotWidth, staffSlotHeight)
        } else {
            blit(graphics, staffSlotMinX, staffSlotMinY, 232, 328, staffSlotWidth, staffSlotHeight)
        }
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTick: Float,
    ) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)

        // The staff's search/alias/hint popup lives in grid-local coordinates, but it must
        // not be clipped to the 160x160 mini grid, otherwise long suggestion lists and
        // pattern docs are cut off. Draw it here, on top of the table, without the scissor.
        if (!hasStaffItem) return
        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(staffMinX.toFloat(), staffMinY.toFloat())
        guiSpellcasting.extractOverlay(graphics, mouseX - staffMinX, mouseY - staffMinY)
        pose.popMatrix()
    }

    private fun isEdgeInRange(index: Int) =
        data.isInRange(index)
            || data.isInRange(index - 1)
            || (index == 0 && data.list?.isEmpty() == true)

    // ---------------------------------------------------------------- actions

    private fun addActionButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        uOffset: Int,
        vOffset: Int,
        action: SplicingTableAction,
        altAction: SplicingTableAction? = null,
    ) {
        addRenderableWidget(
            ActionButton(leftPos + x, topPos + y, width, height, uOffset, vOffset, action, altAction)
        )
    }

    private fun canRun(action: SplicingTableAction) =
        data.isListReadable && action.value.test(data, selection, viewStartIndex)

    private fun runAction(action: SplicingTableAction) {
        FabricPacketHandler.sendToServer(MsgSplicingTableActionC2S(action))
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (hasStaffItem) {
            if (guiSpellcasting.isOverlayOpen()) {
                // The search box, alias editor and keyboard drawing own every key while open;
                // Escape closes just the overlay instead of the table.
                if (event.isEscape()) guiSpellcasting.cancelDrawing() else guiSpellcasting.keyPressed(event)
                return true
            }
            // Ctrl+Space opens the pattern search on the mini staff grid.
            if (event.hasControlDown() && event.key() == InputConstants.KEY_SPACE
                && guiSpellcasting.keyPressed(event)
            ) {
                return true
            }
        }

        if (!event.hasAltDown()) {
            val action = KEYBINDS[KeyCombo(event.key(), event.hasControlDown(), event.hasShiftDown())]
            if (action != null && canRun(action)) {
                runAction(action)
                return true
            }
        }
        return super.keyPressed(event)
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (hasStaffItem && guiSpellcasting.isOverlayOpen() && guiSpellcasting.charTyped(event)) return true
        return super.charTyped(event)
    }

    override fun preeditUpdated(event: PreeditEvent?): Boolean {
        if (hasStaffItem && guiSpellcasting.isOverlayOpen()) return guiSpellcasting.preeditUpdated(event)
        return super.preeditUpdated(event)
    }

    override fun mouseScrolled(
        mouseX: Double,
        mouseY: Double,
        scrollX: Double,
        scrollY: Double,
    ): Boolean {
        // A scrollable search/list or keyboard pattern on the staff grid takes the scroll first.
        if (hasStaffItem && guiSpellcasting.isOverlayOpen() && isInStaffGrid(mouseX, mouseY, null)) {
            guiSpellcasting.mouseScrolled(mouseX - staffMinX, mouseY - staffMinY, scrollX, scrollY)
            return true
        }

        val action = when {
            scrollY > 0 -> when {
                hasControlDown -> SplicingTableAction.VIEW_LEFT_FULL
                hasShiftDown -> SplicingTableAction.VIEW_LEFT_PAGE
                else -> SplicingTableAction.VIEW_LEFT
            }
            scrollY < 0 -> when {
                hasControlDown -> SplicingTableAction.VIEW_RIGHT_FULL
                hasShiftDown -> SplicingTableAction.VIEW_RIGHT_PAGE
                else -> SplicingTableAction.VIEW_RIGHT
            }
            else -> return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
        }

        if (!canRun(action)) return false

        // limit scrolls per event to avoid spamming the server
        repeat(abs(scrollY).toInt().coerceIn(1, 4)) {
            runAction(action)
        }
        return true
    }

    private val hasShiftDown get() = minecraft?.hasShiftDown() == true

    private val hasControlDown get() = minecraft?.hasControlDown() == true

    // ---------------------------------------------------------------- staff grid input

    private fun isOutsideStaffItemSlot(mouseX: Double, mouseY: Double) =
        mouseX < staffSlotMinX
            || mouseY < staffSlotMinY
            || mouseX >= staffSlotMinX + staffSlotWidth
            || mouseY >= staffSlotMinY + staffSlotHeight

    private fun isOverClearGridButton(mouseX: Double, mouseY: Double) =
        mouseX >= clearGridButtonX && mouseX < clearGridButtonX + 38
            && mouseY >= clearGridButtonY && mouseY < clearGridButtonY + 25

    private fun isInStaffGrid(mouseX: Double, mouseY: Double, button: Int?): Boolean =
        hasStaffItem
            && mouseX >= staffMinX && mouseX < staffMaxX
            && mouseY >= staffMinY && mouseY < staffMaxY
            // don't start drawing when inserting the staff item or clicking the clear button
            && (button == null || isOutsideStaffItemSlot(mouseX, mouseY))
            && !isOverClearGridButton(mouseX, mouseY)

    private fun toLocalEvent(event: MouseButtonEvent) =
        MouseButtonEvent(
            event.x - staffMinX,
            event.y - staffMinY,
            event.buttonInfo(),
        )

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (isInStaffGrid(event.x, event.y, event.button())) {
            guiSpellcasting.mouseClicked(toLocalEvent(event), doubleClick)
        }
        return super.mouseClicked(event, doubleClick)
    }

    override fun mouseMoved(mouseX: Double, mouseY: Double) {
        if (isInStaffGrid(mouseX, mouseY, null)) {
            guiSpellcasting.mouseMoved(mouseX - staffMinX, mouseY - staffMinY)
        }
        super.mouseMoved(mouseX, mouseY)
    }

    override fun mouseDragged(
        event: MouseButtonEvent,
        dragX: Double,
        dragY: Double,
    ): Boolean {
        if (isInStaffGrid(event.x, event.y, event.button())) {
            guiSpellcasting.mouseDragged(toLocalEvent(event), dragX, dragY)
        }
        return super.mouseDragged(event, dragX, dragY)
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        // always end a held pattern, even if the cursor left the grid
        if (hasStaffItem) {
            guiSpellcasting.mouseReleased(toLocalEvent(event))
        }
        return super.mouseReleased(event)
    }

    override fun onClose() {
        // pressing escape cancels the pattern being drawn instead of closing the table
        if (hasStaffItem && guiSpellcasting.hasPatternInProgress()) {
            guiSpellcasting.cancelDrawing()
            return
        }
        super.onClose()
    }

    override fun hasClickedOutside(mouseX: Double, mouseY: Double, guiLeft: Int, guiTop: Int): Boolean {
        // the media slot hangs off the right edge of the main gui, so don't treat clicks there as "outside"
        if (
            mouseX >= guiLeft + 192 && mouseX < guiLeft + 238
            && mouseY >= guiTop + 100 && mouseY < guiTop + 193
        ) {
            return false
        }
        // the staff grid and staff slot hang off the left edge
        if (
            mouseX >= staffMinX && mouseX < staffMaxX
            && mouseY >= staffMinY && mouseY < staffMaxY
        ) {
            return false
        }
        if (
            mouseX >= staffSlotMinX && mouseX < staffSlotMinX + staffSlotWidth
            && mouseY >= staffSlotMinY && mouseY < staffSlotMinY + staffSlotHeight
        ) {
            return false
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop)
    }

    // ---------------------------------------------------------------- widgets

    inner class IotaButton(private val offset: Int) : AbstractWidget(
        leftPos + 15 + 18 * offset, topPos + 20, 18, 18, Component.empty()
    ) {
        private val index get() = viewStartIndex + offset
        private val inRange get() = data.isInRange(index)
        private val iotaTag get() = data.list?.getOrNull(index)

        override fun extractWidgetRenderState(
            graphics: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            partialTick: Float,
        ) {
            if (inRange && isHoveredOrFocused) {
                graphics.fill(x, y, x + 18, y + 18, 0x40FFFFFF)
            }

            val tag = iotaTag
            if (tag != null) {
                val display = IotaType.getDisplayWithMaxWidth(tag, 16, font)
                graphics.text(font, display, x + 1, y + 5, IotaType.getColor(tag), false)
            } else if (inRange) {
                graphics.centeredText(font, "?", x + 9, y + 5, 0xFF808080.toInt())
            }
        }

        override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
            if (!inRange) return
            FabricPacketHandler.sendToServer(
                MsgSplicingTableSelectC2S(index, event.hasShiftDown(), true)
            )
        }

        override fun updateWidgetNarration(output: NarrationElementOutput) {}
    }

    inner class EdgeButton(private val offset: Int) : AbstractWidget(
        leftPos + 13 + 18 * offset, topPos + 23, 4, 15, Component.empty()
    ) {
        private val index get() = viewStartIndex + offset

        override fun extractWidgetRenderState(
            graphics: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            partialTick: Float,
        ) {
            if (!isEdgeInRange(index)) return
            val selected = (selection as? Selection.Edge)?.index == index
            if (isHovered || selected) {
                graphics.fill(x + 1, y, x + 3, y + height, 0xA0FFFFFF.toInt())
            }
        }

        override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
            if (!isEdgeInRange(index)) return
            FabricPacketHandler.sendToServer(
                MsgSplicingTableSelectC2S(index, event.hasShiftDown(), false)
            )
        }

        override fun updateWidgetNarration(output: NarrationElementOutput) {}
    }

    inner class SelectionOverlay(private val offset: Int) : Renderable {
        override fun extractRenderState(
            graphics: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            partialTick: Float,
        ) {
            val range = selection as? Selection.Range ?: return
            val index = viewStartIndex + offset
            if (!data.isInRange(index) || index !in range) return

            val x = leftPos + 15 + 18 * offset
            blit(graphics, x, topPos + 18, 352, 24, 18, 25)
            if (index == range.start) {
                blit(graphics, x - 1, topPos + 24, 371, 4, 1, 13)
            }
            if (index == range.end) {
                blit(graphics, x + 18, topPos + 24, 370, 4, 1, 13)
            }
        }
    }

    inner class PageButton(x: Int, y: Int, private val direction: Int) : AbstractWidget(
        x, y, 10, 10, Component.empty()
    ) {
        override fun extractWidgetRenderState(
            graphics: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            partialTick: Float,
        ) {
            val u = if (direction < 0) 256 else 266
            blit(graphics, x, y, u, 0, 10, 10)
            if (isHovered) {
                graphics.fill(x, y, x + 10, y + 10, 0x30FFFFFF)
            }
        }

        override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
            val delta = when {
                event.hasControlDown() -> if (direction < 0) -1_000_000 else 1_000_000
                event.hasShiftDown() -> direction * IOTA_BUTTONS
                else -> direction
            }
            FabricPacketHandler.sendToServer(MsgSplicingTableViewC2S(delta))
        }

        override fun updateWidgetNarration(output: NarrationElementOutput) {}
    }

    inner class ActionButton(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        private val uOffset: Int,
        private val vOffset: Int,
        private val action: SplicingTableAction,
        private val altAction: SplicingTableAction? = null,
    ) : AbstractWidget(x, y, width, height, Component.empty()) {
        override fun extractWidgetRenderState(
            graphics: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            partialTick: Float,
        ) {
            active = canRun(action) || (altAction != null && canRun(altAction))
            val (u, v) = when {
                !active -> (uOffset - 136) to (vOffset + 405)
                isHovered -> uOffset to (vOffset + 405)
                else -> uOffset to vOffset
            }
            blit(graphics, x, y, u, v, width, height)
        }

        override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
            val chosen = if (altAction != null && event.hasShiftDown()) altAction else action
            if (canRun(chosen)) runAction(chosen)
        }

        override fun updateWidgetNarration(output: NarrationElementOutput) {}
    }

    inner class ClearGridButton : AbstractWidget(
        clearGridButtonX, clearGridButtonY, 38, 25, Component.empty()
    ) {
        override fun extractWidgetRenderState(
            graphics: GuiGraphicsExtractor,
            mouseX: Int,
            mouseY: Int,
            partialTick: Float,
        ) {
            val u = if (isHovered) 144 else 192
            blit(graphics, x, y, u, 293, width, height)
        }

        override fun onClick(event: MouseButtonEvent, doubleClick: Boolean) {
            guiSpellcasting.clearGrid()
        }

        override fun updateWidgetNarration(output: NarrationElementOutput) {}
    }

    companion object {
        val TEXTURE = HexDebug.id("textures/gui/splicing_table.png")

        private data class KeyCombo(val key: Int, val ctrl: Boolean, val shift: Boolean)

        private val KEYBINDS: Map<KeyCombo, SplicingTableAction> = mapOf(
            KeyCombo(InputConstants.KEY_UP, false, false) to SplicingTableAction.VIEW_LEFT,
            KeyCombo(InputConstants.KEY_PAGEUP, false, false) to SplicingTableAction.VIEW_LEFT_PAGE,
            KeyCombo(InputConstants.KEY_HOME, false, false) to SplicingTableAction.VIEW_LEFT_FULL,
            KeyCombo(InputConstants.KEY_DOWN, false, false) to SplicingTableAction.VIEW_RIGHT,
            KeyCombo(InputConstants.KEY_PAGEDOWN, false, false) to SplicingTableAction.VIEW_RIGHT_PAGE,
            KeyCombo(InputConstants.KEY_END, false, false) to SplicingTableAction.VIEW_RIGHT_FULL,

            KeyCombo(InputConstants.KEY_LEFT, false, false) to SplicingTableAction.CURSOR_LEFT,
            KeyCombo(InputConstants.KEY_RIGHT, false, false) to SplicingTableAction.CURSOR_RIGHT,

            KeyCombo(InputConstants.KEY_LEFT, false, true) to SplicingTableAction.EXPAND_SELECTION_LEFT,
            KeyCombo(InputConstants.KEY_RIGHT, false, true) to SplicingTableAction.EXPAND_SELECTION_RIGHT,
            KeyCombo(InputConstants.KEY_LEFT, true, true) to SplicingTableAction.MOVE_SELECTION_LEFT,
            KeyCombo(InputConstants.KEY_RIGHT, true, true) to SplicingTableAction.MOVE_SELECTION_RIGHT,

            KeyCombo(InputConstants.KEY_A, true, true) to SplicingTableAction.SELECT_NONE,
            KeyCombo(InputConstants.KEY_A, true, false) to SplicingTableAction.SELECT_ALL,

            KeyCombo(InputConstants.KEY_Z, true, false) to SplicingTableAction.UNDO,
            KeyCombo(InputConstants.KEY_Y, true, false) to SplicingTableAction.REDO,

            KeyCombo(InputConstants.KEY_LEFT, true, false) to SplicingTableAction.NUDGE_LEFT,
            KeyCombo(InputConstants.KEY_RIGHT, true, false) to SplicingTableAction.NUDGE_RIGHT,

            KeyCombo(InputConstants.KEY_D, true, false) to SplicingTableAction.DUPLICATE,
            KeyCombo(InputConstants.KEY_DELETE, false, false) to SplicingTableAction.DELETE,
            KeyCombo(InputConstants.KEY_BACKSPACE, false, false) to SplicingTableAction.BACKSPACE,

            KeyCombo(InputConstants.KEY_X, true, false) to SplicingTableAction.CUT,
            KeyCombo(InputConstants.KEY_C, true, false) to SplicingTableAction.COPY,
            KeyCombo(InputConstants.KEY_V, true, false) to SplicingTableAction.PASTE_SPLAT,
            KeyCombo(InputConstants.KEY_V, true, true) to SplicingTableAction.PASTE_VERBATIM,
        )

        fun blit(
            graphics: GuiGraphicsExtractor,
            x: Int,
            y: Int,
            uOffset: Int,
            vOffset: Int,
            width: Int,
            height: Int,
        ) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE,
                x, y, uOffset.toFloat(), vOffset.toFloat(), width, height, 512, 512
            )
        }

        fun blitTinted(
            graphics: GuiGraphicsExtractor,
            x: Int,
            y: Int,
            uOffset: Int,
            vOffset: Int,
            width: Int,
            height: Int,
            color: Int,
        ) {
            graphics.blit(
                RenderPipelines.GUI_TEXTURED, TEXTURE,
                x, y, uOffset.toFloat(), vOffset.toFloat(), width, height, 512, 512, color
            )
        }

        fun blitRepeating(
            graphics: GuiGraphicsExtractor,
            x: Int,
            y: Int,
            uOffset: Int,
            vOffset: Int,
            width: Int,
            height: Int,
            xTiles: Int,
            yTiles: Int,
            color: Int? = null,
        ) {
            for (yIndex in 0 until yTiles) {
                for (xIndex in 0 until xTiles) {
                    val px = x + width * xIndex
                    val py = y + height * yIndex
                    if (color == null) {
                        blit(graphics, px, py, uOffset, vOffset, width, height)
                    } else {
                        blitTinted(graphics, px, py, uOffset, vOffset, width, height, color)
                    }
                }
            }
        }
    }
}
