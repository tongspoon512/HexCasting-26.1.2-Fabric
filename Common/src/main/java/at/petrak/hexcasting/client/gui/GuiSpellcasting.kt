package at.petrak.hexcasting.client.gui

import at.petrak.hexcasting.api.casting.eval.ExecutionClientView
import at.petrak.hexcasting.api.casting.eval.ResolvedPattern
import at.petrak.hexcasting.api.casting.eval.ResolvedPatternType
import at.petrak.hexcasting.api.casting.iota.IotaType
import at.petrak.hexcasting.api.casting.math.HexAngle
import at.petrak.hexcasting.api.casting.math.HexCoord
import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.api.mod.HexConfig
import at.petrak.hexcasting.api.mod.HexTags
import at.petrak.hexcasting.api.utils.asTranslatedComponent
import at.petrak.hexcasting.client.ClientTickCounter
import at.petrak.hexcasting.client.Keybinds
import at.petrak.hexcasting.client.ShiftScrollListener
import at.petrak.hexcasting.client.ktxt.accumulatedScroll
import at.petrak.hexcasting.client.render.*
import at.petrak.hexcasting.client.sound.GridSoundInstance
import at.petrak.hexcasting.common.lib.HexAttributes
import at.petrak.hexcasting.common.lib.HexSounds
import at.petrak.hexcasting.common.lib.hex.HexActions
import at.petrak.hexcasting.common.msgs.MsgNewSpellPatternC2S
import at.petrak.hexcasting.xplat.IClientXplatAbstractions

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.nbt.CompoundTag
import net.minecraft.sounds.SoundSource
import net.minecraft.util.FormattedCharSequence
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec2
import org.joml.Matrix4f
import kotlin.math.*

// TODO winfy: fix this class to use ExecutionClientView
class GuiSpellcasting constructor(
    private val handOpenedWith: InteractionHand,
    private var patterns: MutableList<ResolvedPattern>,
    private var cachedStack: List<CompoundTag>,
    private var cachedRavenmind: CompoundTag?,
    private var parenCount: Int,
) : Screen("gui.hexcasting.spellcasting".asTranslatedComponent) {
    private var stackDescs: List<FormattedCharSequence> = listOf()
    private var parenDescs: List<FormattedCharSequence> = listOf()
    private var ravenmind: FormattedCharSequence? = null

    private var drawState: PatternDrawState = PatternDrawState.BetweenPatterns
    private val usedSpots: MutableSet<HexCoord> = HashSet()

    private var ambianceSoundInstance: GridSoundInstance? = null

    private val randSrc = SoundInstance.createUnseededRandom()

    /**
     * When set, finished patterns are handed to this callback instead of being sent to the server.
     * Used when this grid is embedded in another screen (e.g. the splicing table).
     */
    var patternSink: ((HexPattern) -> Unit)? = null

    /**
     * When set, the pattern mesh is clipped to this rectangle (in screen coordinates).
     * Embedded grids (e.g. the splicing table) use this so guide dots and strokes cannot
     * bleed outside their visible frame.
     */
    var meshScissor: ScreenRectangle? = null

    init {
        for ((pattern, origin) in patterns) {
            this.usedSpots.addAll(pattern.positions(origin))
        }
        this.calculateIotaDisplays()
    }

    fun recvServerUpdate(info: ExecutionClientView, index: Int) {
        if (info.isStackClear) {
            this.minecraft?.setScreen(null)
            return
        }

        // TODO this is the kinda hacky bit
        if (info.resolutionType == ResolvedPatternType.UNDONE) {
            // find the last undo-able pattern and set its coloring to UNDONE
            this.patterns.reversed().drop(1).firstOrNull { canBeUndone(it) }?.let { it.type = ResolvedPatternType.UNDONE }
            // use the normal EVALUATED coloring for the Evanition that was just drawn
            this.patterns.getOrNull(index)?.let { it.type = ResolvedPatternType.EVALUATED }
        } else this.patterns.getOrNull(index)?.let {
                it.type = info.resolutionType
            }

        this.cachedStack = info.stackDescs
        this.cachedRavenmind = info.ravenmind
        this.calculateIotaDisplays()
    }

    fun canBeUndone(resolvedPat: ResolvedPattern): Boolean {
        return when (resolvedPat.type) {
            // standard escaped patterns can always be undone
            ResolvedPatternType.ESCAPED -> true
            // everything else cannot be undone, with three exceptions:
            // - unescaped Introspection and Meditation can be undone if there's nothing else left to undo
            // - Interjection can always be undone (undoing its inserted iota) despite using the EVALUATED coloring
            ResolvedPatternType.EVALUATED -> {
                resolvedPat.pattern.angles == HexActions.OPEN_PAREN.prototype.angles ||
                resolvedPat.pattern.angles == HexActions.OPEN_N_PARENS.prototype.angles ||
                resolvedPat.pattern.angles == HexActions.READ_INTO_PARENS.prototype.angles
            }
            else -> false
        }
    }

    fun calculateIotaDisplays() {
        val mc = Minecraft.getInstance()
        val width = (this.width * LHS_IOTAS_ALLOCATION).toInt()
        this.stackDescs =
            this.cachedStack.map { IotaType.getDisplayWithMaxWidth(it, width, mc.font) }
                .asReversed()
//        this.parenDescs = if (this.cachedParens.isNotEmpty())
//            this.cachedParens.flatMap { HexIotaTypes.getDisplayWithMaxWidth(it, width, mc.font) }
//        else if (this.parenCount > 0)
//            listOf("...".gold.visualOrderText)
//        else
//            emptyList()
        this.parenDescs = emptyList()
        this.ravenmind =
            this.cachedRavenmind?.let {
                IotaType.getDisplayWithMaxWidth(
                    it,
                    (this.width * RHS_IOTAS_ALLOCATION).toInt(),
                    mc.font
                )
            }
    }

    override fun init() {
        val minecraft = Minecraft.getInstance()
        // the grid always needs a sound instance to source the drawing feedback sounds from;
        // embedded grids (splicing table) just don't play the ambient loop
        val player = minecraft.player
        if (player != null) {
            this.ambianceSoundInstance = GridSoundInstance(player)
            if (patternSink == null) {
                val soundManager = minecraft.soundManager
                soundManager.stop(HexSounds.CASTING_AMBIANCE.location, null)
                soundManager.play(this.ambianceSoundInstance!!)
            }
        }

        this.calculateIotaDisplays()
    }

    /**
     * Fixed distance between hex centers when embedded in another screen. Standalone grids scale with
     * the window, but a small embedded grid would otherwise get a tiny spacing and far too many dots.
     */
    private var embeddedHexSize: Float? = null

    /** Configure this grid to be rendered inside another screen instead of as a standalone screen. */
    fun embed(width: Int, height: Int, hexSize: Float = 20f) {
        this.width = width
        this.height = height
        this.embeddedHexSize = hexSize
        this.init()
    }

    /** Clear the drawn patterns from the visual grid (does not touch the splicer list). */
    fun clearGrid() {
        this.patterns.clear()
        this.usedSpots.clear()
    }

    fun hasPatternInProgress(): Boolean = this.drawState !is PatternDrawState.BetweenPatterns

    fun cancelDrawing() {
        this.drawState = PatternDrawState.BetweenPatterns
    }

    override fun tick() {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player
        if (player != null) {
            val heldItem = player.getItemInHand(handOpenedWith)
            if (heldItem.isEmpty || !heldItem.`is`(HexTags.Items.STAVES) || player.getAttributeValue(HexAttributes.holder(HexAttributes.FEEBLE_MIND)) > 0)
                closeForReal()
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (super.mouseClicked(event, doubleClick)) {
            return true
        }
        if (HexConfig.client().clickingTogglesDrawing()) {
            return if (this.drawState is PatternDrawState.BetweenPatterns)
                drawStart(event.x, event.y)
            else
                drawEnd()
        }
        return drawStart(event.x, event.y)
    }

    private fun drawStart(mxOut: Double, myOut: Double): Boolean {
        val mx = Mth.clamp(mxOut, 0.0, this.width.toDouble())
        val my = Mth.clamp(myOut, 0.0, this.height.toDouble())
        if (this.drawState is PatternDrawState.BetweenPatterns) {
            val coord = this.pxToCoord(Vec2(mx.toFloat(), my.toFloat()))
            if (!this.usedSpots.contains(coord)) {
                this.drawState = PatternDrawState.JustStarted(coord)
                Minecraft.getInstance().soundManager.play(
                    SimpleSoundInstance(
                        HexSounds.START_PATTERN,
                        SoundSource.PLAYERS,
                        0.25f,
                        1f,
                        randSrc,
                        this.ambianceSoundInstance!!.x,
                        this.ambianceSoundInstance!!.y,
                        this.ambianceSoundInstance!!.z,
                    )
                )
            }
        }

        return false
    }

    override fun mouseMoved(mxOut: Double, myOut: Double) {
        super.mouseMoved(mxOut, myOut)

        if (HexConfig.client().clickingTogglesDrawing() && this.drawState !is PatternDrawState.BetweenPatterns)
            drawMove(mxOut, myOut)
    }

    override fun mouseDragged(event: MouseButtonEvent, pDragX: Double, pDragY: Double): Boolean {
        if (super.mouseDragged(event, pDragX, pDragY)) {
            return true
        }
        if (HexConfig.client().clickingTogglesDrawing())
            return false
        return drawMove(event.x, event.y)
    }

    private fun drawMove(mxOut: Double, myOut: Double): Boolean {
        val mx = Mth.clamp(mxOut, 0.0, this.width.toDouble())
        val my = Mth.clamp(myOut, 0.0, this.height.toDouble())

        val anchorCoord = when (this.drawState) {
            PatternDrawState.BetweenPatterns -> null
            is PatternDrawState.JustStarted -> (this.drawState as PatternDrawState.JustStarted).start
            is PatternDrawState.Drawing -> (this.drawState as PatternDrawState.Drawing).current
        }
        if (anchorCoord != null) {
            val anchor = this.coordToPx(anchorCoord)
            val mouse = Vec2(mx.toFloat(), my.toFloat())
            val snapDist =
                this.hexSize() * this.hexSize() * 2.0 * Mth.clamp(HexConfig.client().gridSnapThreshold(), 0.5, 1.0)
            if (anchor.distanceToSqr(mouse) >= snapDist) {
                val delta = mouse.add(anchor.negated())
                val angle = atan2(delta.y, delta.x)
                // 0 is right, increases clockwise(?)
                val snappedAngle = angle.div(Mth.TWO_PI).mod(6.0f)
                val newdir = HexDir.values()[(snappedAngle.times(6).roundToInt() + 1).mod(6)]
                // The player might have a lousy aim, so set the new anchor point to the "ideal"
                // location as if they had hit it exactly on the nose.
                val idealNextLoc = anchorCoord + newdir
                var playSound = false
                if (!this.usedSpots.contains(idealNextLoc)) {
                    if (this.drawState is PatternDrawState.JustStarted) {
                        val pat = HexPattern(newdir)

                        this.drawState = PatternDrawState.Drawing(anchorCoord, idealNextLoc, pat)
                        playSound = true
                    } else if (this.drawState is PatternDrawState.Drawing) {
                        // how anyone gets around without a borrowck is beyond me
                        val ds = (this.drawState as PatternDrawState.Drawing)
                        val lastDir = ds.wipPattern.finalDir()
                        if (newdir == lastDir.rotatedBy(HexAngle.BACK)) {
                            // We're diametrically opposite! Do a backtrack
                            if (ds.wipPattern.angles.isEmpty()) {
                                this.drawState = PatternDrawState.JustStarted(ds.current + newdir)
                            } else {
                                ds.current += newdir
                                ds.wipPattern.angles.removeLast()
                            }
                            playSound = true
                        } else {
                            val success = ds.wipPattern.tryAppendDir(newdir)
                            if (success) {
                                ds.current = idealNextLoc
                            }
                            playSound = success
                        }
                    }
                }

                if (playSound) {
                    Minecraft.getInstance().soundManager.play(
                        SimpleSoundInstance(
                            HexSounds.ADD_TO_PATTERN,
                            SoundSource.PLAYERS,
                            0.25f,
                            1f + (Math.random().toFloat() - 0.5f) * 0.1f,
                            randSrc,
                            this.ambianceSoundInstance!!.x,
                            this.ambianceSoundInstance!!.y,
                            this.ambianceSoundInstance!!.z,
                        )
                    )
                }
            }
        }

        return false
    }

    override fun mouseReleased(event: MouseButtonEvent): Boolean {
        if (super.mouseReleased(event)) {
            return true
        }
        if (HexConfig.client().clickingTogglesDrawing())
            return false
        return drawEnd()
    }

    private fun drawEnd(): Boolean {
        when (this.drawState) {
            PatternDrawState.BetweenPatterns -> {}
            is PatternDrawState.JustStarted -> {
                // Well, we never managed to get anything on the stack this go-around.
                this.drawState = PatternDrawState.BetweenPatterns
            }

            is PatternDrawState.Drawing -> {
                val (start, _, pat) = this.drawState as PatternDrawState.Drawing
                this.drawState = PatternDrawState.BetweenPatterns
                this.patterns.add(ResolvedPattern(pat, start, ResolvedPatternType.UNRESOLVED))

                this.usedSpots.addAll(pat.positions(start))

                val sink = this.patternSink
                if (sink != null) {
                    sink(pat)
                } else {
                    IClientXplatAbstractions.INSTANCE.sendPacketToServer(
                        MsgNewSpellPatternC2S(
                            this.handOpenedWith,
                            pat,
                            this.patterns
                        )
                    )
                }
            }
        }

        return false
    }

    override fun mouseScrolled(pMouseX: Double, pMouseY: Double, scrollX: Double, pDelta: Double): Boolean {
        super.mouseScrolled(pMouseX, pMouseY, scrollX, pDelta)

        val mouseHandler = Minecraft.getInstance().mouseHandler

        if (mouseHandler.accumulatedScroll != 0.0 && sign(pDelta) != sign(mouseHandler.accumulatedScroll)) {
            mouseHandler.accumulatedScroll = 0.0
        }

        mouseHandler.accumulatedScroll += pDelta
        val accumulation: Int = mouseHandler.accumulatedScroll.toInt()
        if (accumulation == 0) {
            return true
        }

        mouseHandler.accumulatedScroll -= accumulation.toDouble()

        ShiftScrollListener.onScroll(pDelta, false)

        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (super.keyPressed(event)) return true

        // because of how mouse scrolling works (scrolling upward moves the page down), a positive
        // delta value makes the book flip backward while a negative one makes it flip forward
        if (Keybinds.spellbookPrev.matches(event)) {
            ShiftScrollListener.onScroll(1.0, false, false)
            return true
        } else if (Keybinds.spellbookNext.matches(event)) {
            ShiftScrollListener.onScroll(-1.0, false, false)
            return true
        }

        return false
    }

    override fun onClose() {
        if (drawState == PatternDrawState.BetweenPatterns)
            closeForReal()
        else
            drawState = PatternDrawState.BetweenPatterns
    }

    fun closeForReal() {
        Minecraft.getInstance().soundManager.stop(HexSounds.CASTING_AMBIANCE.location, null)

        super.onClose()
    }


    override fun extractRenderState(graphics: GuiGraphicsExtractor, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
        super.extractRenderState(graphics, pMouseX, pMouseY, pPartialTick)

        this.ambianceSoundInstance?.mousePosX = pMouseX / this.width.toDouble()
        this.ambianceSoundInstance?.mousePosY = pMouseX / this.width.toDouble()

        // The GUI's pose is a 2D affine transform; pattern geometry is already in screen pixels, so the
        // mesh transform stays identity and the render state picks up the pose itself.
        val mat = Matrix4f()
        val mesh = MeshCollector()

        // Draw guide dots around the cursor
        val mousePos = Vec2(pMouseX.toFloat(), pMouseY.toFloat())
        // snap it to the center
        val mouseCoord = this.pxToCoord(mousePos)
        val radius = 3
        for (dotCoord in mouseCoord.rangeAround(radius)) {
            if (!this.usedSpots.contains(dotCoord)) {
                val dotPx = this.coordToPx(dotCoord)
                val delta = dotPx.add(mousePos.negated()).length()
                // when right on top of the cursor, 1.0
                // when at the full radius, 0! this is so we don't have dots suddenly appear/disappear.
                // we subtract size from delta so there's a little "island" of 100% bright points by the mouse
                val scaledDist = Mth.clamp(
                    1.0f - ((delta - this.hexSize()) / (radius.toFloat() * this.hexSize())),
                    0f,
                    1f
                )
                drawSpot(
                    mat,
                    dotPx,
                    scaledDist * 2f,
                    Mth.lerp(scaledDist, 0.4f, 0.5f),
                    Mth.lerp(scaledDist, 0.8f, 1.0f),
                    Mth.lerp(scaledDist, 0.7f, 0.9f),
                    scaledDist,
                    mesh
                )
            }
        }

        for ((idx, elts) in this.patterns.withIndex()) {
            val (pat, origin, valid) = elts
            drawPatternFromPoints(
                mat,
                pat.toLines(
                    this.hexSize(),
                    this.coordToPx(origin)
                ),
                findDupIndices(pat.positions()),
                true,
                valid.color or (0xC8 shl 24),
                valid.fadeColor or (0xC8 shl 24),
                if (valid.success) 0.2f else 0.9f,
                DEFAULT_READABILITY_OFFSET,
                1f,
                idx.toDouble(),
                mesh
            )
        }

        // Now draw the currently WIP pattern
        if (this.drawState !is PatternDrawState.BetweenPatterns) {
            val points = mutableListOf<Vec2>()
            var dupIndices: Set<Int>? = null

            if (this.drawState is PatternDrawState.JustStarted) {
                val ds = this.drawState as PatternDrawState.JustStarted
                points.add(this.coordToPx(ds.start))
            } else if (this.drawState is PatternDrawState.Drawing) {
                val ds = this.drawState as PatternDrawState.Drawing
                dupIndices = findDupIndices(ds.wipPattern.positions())
                for (pos in ds.wipPattern.positions()) {
                    val pix = this.coordToPx(pos + ds.start)
                    points.add(pix)
                }
            }

            points.add(mousePos)
            // Use the size of the patterns as the seed so that way when this one is added the zappies don't jump
            drawPatternFromPoints(mat,
                points,
                dupIndices,
                false,
                0xff_64c8ff_u.toInt(),
                0xff_fecbe6_u.toInt(),
                0.1f,
                DEFAULT_READABILITY_OFFSET,
                1f,
                this.patterns.size.toDouble(),
                mesh)
        }

        HexGuiRender.submitMesh(graphics, mesh, this.meshScissor)

        val mc = Minecraft.getInstance()
        val font = mc.font
        val matrix = graphics.pose()
        matrix.pushMatrix()
        matrix.translate(10f, 10f)

//        if (this.parenCount > 0) {
//            val boxHeight = (this.parenDescs.size + 1f) * 10f
//            drawBox(graphics, 0f, 0f, (this.width * LHS_IOTAS_ALLOCATION + 5).toFloat(), boxHeight, 7.5f)
//            matrix.translate(0f, 0f)
//
//            val time = ClientTickCounter.getTotal() * 0.16f
//            val opacity = (Mth.map(cos(time), -1f, 1f, 200f, 255f)).toInt()
//            val color = 0x00_ffffff or (opacity shl 24)
//            for (desc in this.parenDescs) {
//                graphics.text(font, desc, 10, 7, color)
//                matrix.translate(0f, 10f)
//            }
//            matrix.translate(0f, 15f)
//        }

        if (this.stackDescs.isNotEmpty()) {
            val boxHeight = (this.stackDescs.size + 1f) * 10f
            drawBox(graphics, 0f, 0f, (this.width * LHS_IOTAS_ALLOCATION + 5).toFloat(), boxHeight)
            matrix.pushMatrix()
            matrix.translate(5f, 7f)
            for (desc in this.stackDescs) {
                graphics.text(font, desc, 0, 0, -1)
                matrix.translate(0f, 10f)
            }
            matrix.popMatrix()
        }

        matrix.popMatrix()
        if (this.ravenmind != null) {
            val kotlinBad = this.ravenmind!!
            matrix.pushMatrix()
            val boxHeight = 15f
            val addlScale = 1.5f
            matrix.translate((this.width * (1.0 - RHS_IOTAS_ALLOCATION * addlScale) - 10f).toFloat(), 10f)
            drawBox(
                graphics, 0f, 0f,
                (this.width * RHS_IOTAS_ALLOCATION * addlScale).toFloat(), boxHeight * addlScale,
            )
            matrix.translate(5f, 5f)
            matrix.scale(addlScale, addlScale)

            val time = ClientTickCounter.getTotal() * 0.2f
            val opacity = (Mth.map(sin(time), -1f, 1f, 150f, 255f)).toInt()
            val color = 0x00_ffffff or (opacity shl 24)

            graphics.text(font, kotlinBad, 0, 0, color)
            matrix.popMatrix()
        }
    }

    // why the hell is this default true
    override fun isPauseScreen(): Boolean = false

    /**
     * 施法界面不绘制任何背景（不压暗、不模糊），让世界画面直接透出来。
     */
    override fun extractBackground(graphics: GuiGraphicsExtractor, pMouseX: Int, pMouseY: Int, pPartialTick: Float) {
    }

    /** Distance between adjacent hex centers */
    fun hexSize(): Float {
        embeddedHexSize?.let { return it }
        val scaleModifier = Minecraft.getInstance().player!!.getAttributeValue(HexAttributes.holder(HexAttributes.GRID_ZOOM))
        val spacing = Mth.clamp(HexConfig.client().gridSpacing(), 0.5, 2.0)

        // Originally, we allowed 32 dots across. Assuming a 1920x1080 screen this allowed like 500-odd area.
        // Let's be generous and give them 512.
        val baseScale = sqrt(this.width.toDouble() * this.height / 512.0)
        return (baseScale * spacing / scaleModifier).toFloat()
    }

    fun coordsOffset(): Vec2 = Vec2(this.width.toFloat() * 0.5f, this.height.toFloat() * 0.5f)

    fun coordToPx(coord: HexCoord) =
        at.petrak.hexcasting.api.utils.coordToPx(coord, this.hexSize(), this.coordsOffset())

    fun pxToCoord(px: Vec2) = at.petrak.hexcasting.api.utils.pxToCoord(px, this.hexSize(), this.coordsOffset())


    private sealed class PatternDrawState {
        /** We're waiting on the player to right-click again */
        object BetweenPatterns : PatternDrawState()

        /** We just started drawing and haven't drawn the first line yet. */
        data class JustStarted(val start: HexCoord) : PatternDrawState()

        /** We've started drawing a pattern for real. */
        data class Drawing(val start: HexCoord, var current: HexCoord, val wipPattern: HexPattern) : PatternDrawState()
    }

    companion object {
        const val LHS_IOTAS_ALLOCATION = 0.7
        const val RHS_IOTAS_ALLOCATION = 0.15

        fun drawBox(graphics: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, leftMargin: Float = 2.5f) {
            graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + h).toInt(), 0x50_303030)
            graphics.fill(
                (x + leftMargin).toInt(), (y + 2.5f).toInt(),
                (x + w - 2.5f).toInt(), (y + h - 5f).toInt(), 0x50_303030
            )
        }
    }
}
