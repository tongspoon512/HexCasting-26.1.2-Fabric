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
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.PreeditEvent
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
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

    /** Pattern currently under the mouse while idling, and when the hover started. */
    private var hoveredPattern: ResolvedPattern? = null
    private var hoveredSince: Long = 0L

    /** Last mouse position in GUI pixels, recorded for keyboard-opened UIs. */
    private var lastMouseX: Int = 0
    private var lastMouseY: Int = 0

    /** Current IME composition shown in the search/alias box; empty when no IME is composing. */
    private var preeditText: String = ""

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

    /**
     * When true, the search/alias/hint overlay is not drawn by [extractRenderState]; the host screen
     * draws it itself via [extractOverlay] so it can escape the embedded grid's scissor rectangle.
     */
    var deferOverlay: Boolean = false

    /** Optional bounds (in grid-local coordinates) used to clamp the overlay popup when embedded. */
    var overlayBoundsWidth: Int? = null
    var overlayBoundsHeight: Int? = null

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
        this.preeditText = ""
    }

    /** True when a search box, alias editor or keyboard-drawn pattern is open above the grid. */
    fun isOverlayOpen(): Boolean =
        this.drawState is PatternDrawState.KeyboardDrawing ||
            this.drawState is PatternDrawState.AutoCompleting ||
            this.drawState is PatternDrawState.AliasEditing

    override fun tick() {
        if (this.drawState !is PatternDrawState.AutoCompleting && this.drawState !is PatternDrawState.AliasEditing) {
            this.preeditText = ""
        }
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player
        if (player != null) {
            val heldItem = player.getItemInHand(handOpenedWith)
            if (heldItem.isEmpty || !heldItem.`is`(HexTags.Items.STAVES) || player.getAttributeValue(HexAttributes.holder(HexAttributes.FEEBLE_MIND)) > 0)
                closeForReal()
        }
    }

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (this.drawState is PatternDrawState.AutoCompleting || this.drawState is PatternDrawState.AliasEditing) {
            this.drawState = PatternDrawState.BetweenPatterns
            this.preeditText = ""
            return true
        }

        val keyboard = this.drawState as? PatternDrawState.KeyboardDrawing
        if (keyboard != null) {
            when (event.button()) {
                0 -> submitKeyboardPattern(keyboard)
                1 -> this.drawState = PatternDrawState.BetweenPatterns
            }
            return true
        }

        // Right-click on the idle grid opens the browsable list of every registered pattern.
        if (this.drawState is PatternDrawState.BetweenPatterns && event.button() == 1) {
            val coord = this.pxToCoord(Vec2(event.x.toFloat(), event.y.toFloat()))
            openAutoComplete(coord, browse = true)
            return true
        }

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

        val keyboard = this.drawState as? PatternDrawState.KeyboardDrawing
        if (keyboard != null) {
            val mx = Mth.clamp(mxOut, 0.0, this.width.toDouble())
            val my = Mth.clamp(myOut, 0.0, this.height.toDouble())
            keyboard.origin = this.pxToCoord(Vec2(mx.toFloat(), my.toFloat()))
            recalculateKeyboardPattern(keyboard)
            return
        }

        val autoComplete = this.drawState as? PatternDrawState.AutoCompleting
        if (autoComplete != null) {
            // Don't leave an empty search box hanging around when the mouse wanders off.
            if (!autoComplete.browse && autoComplete.query.isEmpty()) {
                val anchor = this.coordToPx(autoComplete.origin)
                val dx = mxOut.toFloat() - anchor.x
                val dy = myOut.toFloat() - anchor.y
                val breakout = this.hexSize() * 1.75f
                if (dx * dx + dy * dy > breakout * breakout) {
                    this.drawState = PatternDrawState.BetweenPatterns
                }
            }
            return
        }

        if (this.drawState is PatternDrawState.AliasEditing) return

        if (HexConfig.client().clickingTogglesDrawing() && this.drawState !is PatternDrawState.BetweenPatterns)
            drawMove(mxOut, myOut)
    }

    override fun mouseDragged(event: MouseButtonEvent, pDragX: Double, pDragY: Double): Boolean {
        if (this.drawState is PatternDrawState.KeyboardDrawing ||
            this.drawState is PatternDrawState.AutoCompleting ||
            this.drawState is PatternDrawState.AliasEditing
        ) {
            return true
        }
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
            is PatternDrawState.KeyboardDrawing,
            is PatternDrawState.AutoCompleting,
            is PatternDrawState.AliasEditing -> null
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
        if (this.drawState is PatternDrawState.KeyboardDrawing ||
            this.drawState is PatternDrawState.AutoCompleting ||
            this.drawState is PatternDrawState.AliasEditing
        ) {
            return true
        }
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
                commitPattern(start, pat)
            }

            is PatternDrawState.KeyboardDrawing,
            is PatternDrawState.AutoCompleting,
            is PatternDrawState.AliasEditing -> {}
        }

        return false
    }

    /** Adds a finished pattern to the grid and hands it to the sink or the server. */
    private fun commitPattern(start: HexCoord, pat: HexPattern) {
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

    override fun mouseScrolled(pMouseX: Double, pMouseY: Double, scrollX: Double, pDelta: Double): Boolean {
        val keyboard = this.drawState as? PatternDrawState.KeyboardDrawing
        if (keyboard != null) {
            rotateKeyboardPattern(keyboard, if (pDelta > 0) -1 else 1)
            return true
        }

        val autoComplete = this.drawState as? PatternDrawState.AutoCompleting
        if (autoComplete != null) {
            offsetAutoChosen(autoComplete, if (pDelta > 0) -1 else 1)
            return true
        }

        if (this.drawState is PatternDrawState.AliasEditing) return true

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
        // While an IME is composing, the IME owns every key (Enter/Space only confirm a candidate).
        // Escape drops the composition instead of closing the search, so a stale composition can
        // never swallow input for more than one key.
        if (this.preeditText.isNotEmpty() && isTextState()) {
            if (event.key() == PatternKeyboard.KEY_ESCAPE) this.preeditText = ""
            return true
        }

        // The keyboard-driven states go first so Escape doesn't close the whole screen.
        val autoComplete = this.drawState as? PatternDrawState.AutoCompleting
        if (autoComplete != null && handleAutoCompleteKey(event, autoComplete)) return true

        val aliasEditing = this.drawState as? PatternDrawState.AliasEditing
        if (aliasEditing != null && handleAliasKey(event, aliasEditing)) return true

        val keyboard = this.drawState as? PatternDrawState.KeyboardDrawing
        if (keyboard != null && handleKeyboardKey(event, keyboard)) return true

        // because of how mouse scrolling works (scrolling upward moves the page down), a positive
        // delta value makes the book flip backward while a negative one makes it flip forward.
        // Embedded grids (splicing table) must not flip the held spellbook.
        if (this.patternSink == null) {
            if (Keybinds.spellbookPrev.matches(event)) {
                ShiftScrollListener.onScroll(1.0, false, false)
                return true
            } else if (Keybinds.spellbookNext.matches(event)) {
                ShiftScrollListener.onScroll(-1.0, false, false)
                return true
            }
        }

        // Ctrl combos and typed angles while idling.
        if (this.drawState is PatternDrawState.BetweenPatterns) {
            val ctrl = (event.modifiers() and PatternKeyboard.MOD_CONTROL) != 0
            if (ctrl && event.key() == PatternKeyboard.KEY_SPACE) {
                startAutoComplete()
                return true
            }
            if (ctrl && event.key() == PatternKeyboard.KEY_E) {
                val hovered = this.hoveredPattern
                val entry = hovered?.let { PatternKeyboard.entryOf(it.pattern.angles) }
                if (entry != null) {
                    this.drawState = PatternDrawState.AliasEditing(entry)
                    return true
                }
            }

            if (!ctrl) {
                val angle = PatternKeyboard.angleOfKey(event.key())
                if (angle != null) {
                    startKeyboardPattern(angle)
                    return true
                }
            }
        }

        return super.keyPressed(event)
    }

    /** Text typed into the search box or the alias editor, including IME commits. */
    override fun charTyped(event: CharacterEvent): Boolean {
        val text = PatternKeyboard.charOfTypedEvent(event.codepoint()) ?: return super.charTyped(event)
        val autoComplete = this.drawState as? PatternDrawState.AutoCompleting
        if (autoComplete != null) {
            autoComplete.query += text
            this.preeditText = ""
            refreshAutoComplete(autoComplete)
            return true
        }

        val aliasEditing = this.drawState as? PatternDrawState.AliasEditing
        if (aliasEditing != null) {
            aliasEditing.text += text
            this.preeditText = ""
            return true
        }

        return super.charTyped(event)
    }

    /** IME composition text; the event is null when the composition ends or is committed. */
    override fun preeditUpdated(event: PreeditEvent?): Boolean {
        if (!isTextState()) {
            this.preeditText = ""
            return super.preeditUpdated(event)
        }
        this.preeditText = event?.fullText() ?: ""
        return true
    }

    private fun isTextState(): Boolean =
        this.drawState is PatternDrawState.AutoCompleting || this.drawState is PatternDrawState.AliasEditing

    private fun startKeyboardPattern(firstAngle: HexAngle) {
        val keyboard = PatternDrawState.KeyboardDrawing(HexCoord.Origin, HexDir.EAST)
        keyboard.angles.add(firstAngle)
        this.drawState = keyboard
        recalculateKeyboardPattern(keyboard)
    }

    private fun startKeyboardPattern(entry: PatternEntry, origin: HexCoord) {
        val keyboard = PatternDrawState.KeyboardDrawing(origin, entry.startDir)
        keyboard.angles.addAll(entry.angles)
        this.drawState = keyboard
        recalculateKeyboardPattern(keyboard)
    }

    private fun startAutoComplete() {
        val coord = this.pxToCoord(Vec2(this.lastMouseX.toFloat(), this.lastMouseY.toFloat()))
        openAutoComplete(coord, browse = false)
    }

    private fun openAutoComplete(origin: HexCoord, browse: Boolean) {
        val autoComplete = PatternDrawState.AutoCompleting(origin)
        autoComplete.browse = browse
        this.drawState = autoComplete
        this.preeditText = ""
        refreshAutoComplete(autoComplete)
    }

    private fun refreshAutoComplete(autoComplete: PatternDrawState.AutoCompleting) {
        val result = PatternKeyboard.search(autoComplete.query)
        autoComplete.suggestions = result.suggestions
        autoComplete.lockedCount = result.lockedCount
        autoComplete.chosen = 0
        autoComplete.chosenDoc = 0
    }

    private fun offsetAutoChosen(autoComplete: PatternDrawState.AutoCompleting, by: Int) {
        val size = autoComplete.suggestions.size
        if (size == 0) return
        autoComplete.chosen = ((autoComplete.chosen + by) % size + size) % size
        autoComplete.chosenDoc = 0
    }

    private fun offsetAutoDoc(autoComplete: PatternDrawState.AutoCompleting, by: Int) {
        val entry = autoComplete.suggestions.getOrNull(autoComplete.chosen) ?: return
        val size = PatternDocs.docsOf(entry.id).size
        if (size == 0) return
        autoComplete.chosenDoc = ((autoComplete.chosenDoc + by) % size + size) % size
    }

    private fun handleAutoCompleteKey(event: KeyEvent, autoComplete: PatternDrawState.AutoCompleting): Boolean {
        val ctrl = (event.modifiers() and PatternKeyboard.MOD_CONTROL) != 0
        if (ctrl && event.key() == PatternKeyboard.KEY_E) {
            val entry = autoComplete.suggestions.getOrNull(autoComplete.chosen) ?: return true
            this.drawState = PatternDrawState.AliasEditing(entry)
            return true
        }

        when (event.key()) {
            PatternKeyboard.KEY_ESCAPE -> this.drawState = PatternDrawState.BetweenPatterns
            PatternKeyboard.KEY_BACKSPACE -> {
                autoComplete.query = if (ctrl) {
                    PatternKeyboard.removeLastWord(autoComplete.query)
                } else {
                    autoComplete.query.dropLast(1)
                }
                refreshAutoComplete(autoComplete)
            }

            PatternKeyboard.KEY_ENTER,
            PatternKeyboard.KEY_KP_ENTER,
            PatternKeyboard.KEY_TAB -> {
                val entry = autoComplete.suggestions.getOrNull(autoComplete.chosen) ?: return true
                startKeyboardPattern(entry, autoComplete.origin)
            }

            PatternKeyboard.KEY_UP -> offsetAutoChosen(autoComplete, -1)
            PatternKeyboard.KEY_DOWN -> offsetAutoChosen(autoComplete, 1)
            PatternKeyboard.KEY_LEFT -> offsetAutoDoc(autoComplete, -1)
            PatternKeyboard.KEY_RIGHT -> offsetAutoDoc(autoComplete, 1)

            // Everything else is text, delivered by charTyped so IME commits work too.
            else -> return false
        }
        return true
    }

    private fun handleAliasKey(event: KeyEvent, editing: PatternDrawState.AliasEditing): Boolean {
        val ctrl = (event.modifiers() and PatternKeyboard.MOD_CONTROL) != 0
        when (event.key()) {
            PatternKeyboard.KEY_ESCAPE -> this.drawState = PatternDrawState.BetweenPatterns
            PatternKeyboard.KEY_BACKSPACE -> {
                editing.text = if (ctrl) {
                    PatternKeyboard.removeLastWord(editing.text)
                } else {
                    editing.text.dropLast(1)
                }
            }

            PatternKeyboard.KEY_ENTER, PatternKeyboard.KEY_KP_ENTER, PatternKeyboard.KEY_TAB -> {
                PatternAliases.put(editing.entry.id, editing.text)
                this.drawState = PatternDrawState.BetweenPatterns
            }

            // Everything else is text, delivered by charTyped so IME commits work too.
            else -> return false
        }
        return true
    }

    private fun handleKeyboardKey(event: KeyEvent, keyboard: PatternDrawState.KeyboardDrawing): Boolean {
        when (event.key()) {
            PatternKeyboard.KEY_ESCAPE -> this.drawState = PatternDrawState.BetweenPatterns
            PatternKeyboard.KEY_BACKSPACE, PatternKeyboard.KEY_S -> {
                if (keyboard.angles.isEmpty()) {
                    this.drawState = PatternDrawState.BetweenPatterns
                } else {
                    keyboard.angles.removeLast()
                    if (keyboard.angles.isEmpty()) {
                        this.drawState = PatternDrawState.BetweenPatterns
                    } else {
                        recalculateKeyboardPattern(keyboard)
                    }
                }
            }

            PatternKeyboard.KEY_ENTER,
            PatternKeyboard.KEY_KP_ENTER,
            PatternKeyboard.KEY_TAB,
            PatternKeyboard.KEY_SPACE -> submitKeyboardPattern(keyboard)

            PatternKeyboard.KEY_H, PatternKeyboard.KEY_LEFT -> moveKeyboardOrigin(keyboard, -1, 0)
            PatternKeyboard.KEY_J, PatternKeyboard.KEY_DOWN -> moveKeyboardOrigin(keyboard, 0, 1)
            PatternKeyboard.KEY_K, PatternKeyboard.KEY_UP -> moveKeyboardOrigin(keyboard, 0, -1)
            PatternKeyboard.KEY_L, PatternKeyboard.KEY_RIGHT -> moveKeyboardOrigin(keyboard, 1, 0)

            PatternKeyboard.KEY_R ->
                rotateKeyboardPattern(
                    keyboard,
                    if ((event.modifiers() and PatternKeyboard.MOD_SHIFT) != 0) -1 else 1
                )

            else -> {
                val angle = PatternKeyboard.angleOfKey(event.key()) ?: return false
                if (canAppendKeyboardAngle(keyboard, angle)) {
                    keyboard.angles.add(angle)
                    recalculateKeyboardPattern(keyboard)
                }
            }
        }
        return true
    }

    private fun canAppendKeyboardAngle(keyboard: PatternDrawState.KeyboardDrawing, angle: HexAngle): Boolean {
        val startDir = keyboard.startDir ?: keyboard.originDir
        val pat = HexPattern(startDir, ArrayList(keyboard.angles))
        return pat.tryAppendDir(pat.finalDir().rotatedBy(angle))
    }

    private fun moveKeyboardOrigin(keyboard: PatternDrawState.KeyboardDrawing, x: Int, y: Int) {
        val next = keyboard.origin + HexCoord(x, y)
        if (isVisible(next)) {
            keyboard.origin = next
            recalculateKeyboardPattern(keyboard)
        }
    }

    private fun rotateKeyboardPattern(keyboard: PatternDrawState.KeyboardDrawing, delta: Int) {
        keyboard.originDir = HexDir.values()[(keyboard.originDir.ordinal + delta).mod(HexDir.values().size)]
        recalculateKeyboardPattern(keyboard)
    }

    private fun submitKeyboardPattern(keyboard: PatternDrawState.KeyboardDrawing) {
        if (keyboard.angles.isEmpty()) {
            this.drawState = PatternDrawState.BetweenPatterns
            return
        }
        recalculateKeyboardPattern(keyboard)
        val start = keyboard.start ?: return
        val startDir = keyboard.startDir ?: return
        val pat = HexPattern(startDir, ArrayList(keyboard.angles))
        this.drawState = PatternDrawState.BetweenPatterns
        commitPattern(start, pat)
    }

    /** Find the closest free spot (and the best rotation) for the keyboard pattern being drawn. */
    private fun recalculateKeyboardPattern(keyboard: PatternDrawState.KeyboardDrawing) {
        if (keyboard.angles.isEmpty()) {
            keyboard.start = keyboard.origin
            keyboard.startDir = keyboard.originDir
            keyboard.end = keyboard.origin
            keyboard.endDir = keyboard.originDir
            return
        }

        val proto = HexPattern(keyboard.originDir, ArrayList(keyboard.angles))
        val placement = findClosestAvailable(keyboard.origin, proto)
        if (placement == null) {
            keyboard.start = null
            keyboard.startDir = null
            keyboard.end = keyboard.origin
            keyboard.endDir = keyboard.originDir
            return
        }

        val (start, startDir) = placement
        keyboard.start = start
        keyboard.startDir = startDir
        val placed = HexPattern(startDir, ArrayList(keyboard.angles))
        keyboard.end = placed.positions(start).last()
        keyboard.endDir = placed.finalDir()
    }

    private fun findClosestAvailable(origin: HexCoord, pat: HexPattern): Pair<HexCoord, HexDir>? {
        val dirs = HexDir.values()
        val visited = HashSet<HexCoord>()
        val queue = ArrayDeque<HexCoord>()
        queue.add(origin)
        visited.add(origin)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            for (i in dirs.indices) {
                val startDir = dirs[(pat.startDir.ordinal + i) % dirs.size]
                if (fitsAt(current, pat, startDir)) return current to startDir
            }
            for (dir in dirs) {
                val next = current + dir
                if (visited.add(next)) queue.add(next)
            }
            if (visited.size > 512) return null
        }
        return null
    }

    private fun fitsAt(origin: HexCoord, pat: HexPattern, startDir: HexDir): Boolean {
        if (isUsed(origin)) return false
        var cursor = origin + startDir
        if (isUsed(cursor)) return false
        var dir = startDir
        for (angle in pat.angles) {
            dir = dir.rotatedBy(angle)
            cursor += dir
            if (isUsed(cursor)) return false
        }
        return true
    }

    private fun isUsed(coord: HexCoord): Boolean = this.usedSpots.contains(coord) || !isVisible(coord)

    private fun isVisible(coord: HexCoord): Boolean {
        val px = this.coordToPx(coord)
        return px.x >= 0f && px.x < this.width && px.y >= 0f && px.y < this.height
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

        this.lastMouseX = pMouseX
        this.lastMouseY = pMouseY

        // Now draw the currently WIP pattern
        val keyboard = this.drawState as? PatternDrawState.KeyboardDrawing
        if (keyboard != null) {
            // Grey ghost where the mouse is, then the actual spot the pattern will snap to.
            val ghost = HexPattern(keyboard.originDir, ArrayList(keyboard.angles))
            drawPatternPreview(mat, keyboard.origin, ghost, 0xaa_363a4f_u.toInt(), 0xaa_6e738d_u.toInt(), mesh)

            val start = keyboard.start
            val startDir = keyboard.startDir
            if (start != null && startDir != null) {
                val placed = HexPattern(startDir, ArrayList(keyboard.angles))
                drawPatternPreview(mat, start, placed, 0xff_64c8ff_u.toInt(), 0xff_fecbe6_u.toInt(), mesh)
            }
        } else if (this.drawState is PatternDrawState.JustStarted || this.drawState is PatternDrawState.Drawing) {
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

        if (!deferOverlay) {
            drawCastingOverlay(graphics, font, pMouseX, pMouseY)
        }
    }

    /** Draws only the hint/search overlay in this grid's local coordinate space. */
    fun extractOverlay(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        drawCastingOverlay(graphics, Minecraft.getInstance().font, mouseX, mouseY)
    }

    private fun drawPatternPreview(
        mat: Matrix4f,
        origin: HexCoord,
        pat: HexPattern,
        tail: Int,
        head: Int,
        mesh: MeshCollector
    ) {
        drawPatternFromPoints(
            mat,
            pat.toLines(this.hexSize(), this.coordToPx(origin)),
            findDupIndices(pat.positions()),
            false,
            tail,
            head,
            0.1f,
            DEFAULT_READABILITY_OFFSET,
            1f,
            this.patterns.size.toDouble(),
            mesh
        )
    }

    /** Next-key hints while keyboard drawing, the search/alias boxes, and the hover tooltip while idling. */
    private fun drawCastingOverlay(graphics: GuiGraphicsExtractor, font: Font, mouseX: Int, mouseY: Int) {
        val autoComplete = this.drawState as? PatternDrawState.AutoCompleting
        if (autoComplete != null) {
            drawAutoCompleteOverlay(graphics, font, autoComplete)
            return
        }

        val aliasEditing = this.drawState as? PatternDrawState.AliasEditing
        if (aliasEditing != null) {
            drawAliasOverlay(graphics, font, aliasEditing, mouseX, mouseY)
            return
        }

        val keyboard = this.drawState as? PatternDrawState.KeyboardDrawing
        if (keyboard != null) {
            if (keyboard.start != null) {
                val endPx = this.coordToPx(keyboard.end)
                for (angle in HexAngle.values()) {
                    val nextPos = keyboard.end + keyboard.endDir.rotatedBy(angle)
                    if (isUsed(nextPos) || !canAppendKeyboardAngle(keyboard, angle)) continue
                    val px = this.coordToPx(nextPos)
                    val dx = px.x - endPx.x
                    val dy = px.y - endPx.y
                    val distance = sqrt(dx * dx + dy * dy)
                    if (distance < 0.001f) continue
                    graphics.centeredText(
                        font,
                        PatternKeyboard.keyOfAngle(angle).toString(),
                        (endPx.x + dx / distance * 20f).toInt(),
                        (endPx.y + dy / distance * 20f).toInt() - 4,
                        COLOR_KEY_HINT
                    )
                }
            }

            val lines = mutableListOf<Pair<FormattedCharSequence, Int>>()
            if (keyboard.angles.isNotEmpty()) {
                lines.add(Component.literal(PatternKeyboard.signature(keyboard.angles)).visualOrderText to COLOR_SIGNATURE)
                if (keyboard.start == null) {
                    lines.add(Component.translatable("hexcasting.gui.keyboard.no_space").visualOrderText to COLOR_ERROR)
                }
                val drawn = HexPattern(keyboard.startDir ?: keyboard.originDir, ArrayList(keyboard.angles))
                PatternKeyboard.nameOf(drawn)?.let { lines.add(it.visualOrderText to COLOR_NAME) }
            }
            lines.add(Component.translatable("hexcasting.gui.keyboard.hint").visualOrderText to COLOR_HINT)

            val anchor = if (keyboard.start != null) this.coordToPx(keyboard.end) else this.coordToPx(keyboard.origin)
            drawTooltipBox(graphics, font, anchor.x.toInt() + 20, anchor.y.toInt(), lines)
            return
        }

        // Mouse drawing: show what would be cast if the player let go right now.
        val drawing = this.drawState as? PatternDrawState.Drawing
        if (drawing != null) {
            if (drawing.wipPattern.angles.isNotEmpty()) {
                val lines = mutableListOf<Pair<FormattedCharSequence, Int>>()
                lines.add(
                    Component.literal(PatternKeyboard.signature(drawing.wipPattern.angles))
                        .visualOrderText to COLOR_SIGNATURE
                )
                PatternKeyboard.nameOf(drawing.wipPattern)?.let { lines.add(it.visualOrderText to COLOR_NAME) }
                val anchor = this.coordToPx(drawing.current)
                drawTooltipBox(graphics, font, anchor.x.toInt() + 20, anchor.y.toInt(), lines)
            }
            return
        }

        if (this.drawState !is PatternDrawState.BetweenPatterns) return

        val hovered = patternAt(mouseX, mouseY)
        if (hovered != null && hovered == this.hoveredPattern) {
            if (System.currentTimeMillis() - this.hoveredSince >= HOVER_DELAY_MS) {
                val lines = mutableListOf<Pair<FormattedCharSequence, Int>>()
                lines.add(Component.literal(PatternKeyboard.signature(hovered.pattern.angles)).visualOrderText to COLOR_SIGNATURE)
                PatternKeyboard.nameOf(hovered.pattern)?.let { lines.add(it.visualOrderText to COLOR_NAME) }
                drawTooltipBox(graphics, font, mouseX + 12, mouseY + 12, lines)
            }
        } else {
            this.hoveredPattern = hovered
            this.hoveredSince = System.currentTimeMillis()
        }
    }

    private fun drawAutoCompleteOverlay(
        graphics: GuiGraphicsExtractor,
        font: Font,
        autoComplete: PatternDrawState.AutoCompleting
    ) {
        val lines = mutableListOf<Pair<FormattedCharSequence, Int>>()
        val showList = autoComplete.browse || autoComplete.query.isNotEmpty() || this.preeditText.isNotEmpty()
        val selected = if (showList) autoComplete.suggestions.getOrNull(autoComplete.chosen) else null

        if (!showList) {
            lines.add(Component.translatable("hexcasting.gui.autocomplete.start_typing").visualOrderText to COLOR_HINT)
        } else {
            val header = if (autoComplete.query.isEmpty() && this.preeditText.isEmpty()) {
                Component.translatable("hexcasting.gui.autocomplete.all_patterns")
            } else {
                Component.literal(autoComplete.query)
            }
            lines.add(header.visualOrderText to COLOR_SIGNATURE)
            if (this.preeditText.isNotEmpty()) {
                lines.add(
                    Component.translatable("hexcasting.gui.autocomplete.composing", this.preeditText)
                        .visualOrderText to COLOR_KEY_HINT
                )
            }

            val total = autoComplete.suggestions.size
            if (total == 0) {
                lines.add(Component.translatable("hexcasting.gui.autocomplete.no_match").visualOrderText to COLOR_ERROR)
            } else {
                // Only render a window around the cursor; the whole registry can be hundreds of entries.
                val visible = HexConfig.client().patternAutocompleteCount()
                val from = if (total <= visible) 0
                else (autoComplete.chosen - visible / 2).coerceIn(0, total - visible)
                val to = minOf(total, from + visible)
                for (i in from until to) {
                    val color = if (i == autoComplete.chosen) COLOR_NAME else COLOR_HINT
                    lines.add(autoComplete.suggestions[i].displayName().visualOrderText to color)
                }
                if (total > visible) {
                    lines.add(Component.literal("${from + 1}-$to/$total").visualOrderText to COLOR_KEY_HINT)
                }
                if (autoComplete.lockedCount > 0) {
                    lines.add(
                        Component.translatable("hexcasting.gui.autocomplete.locked", autoComplete.lockedCount)
                            .visualOrderText to COLOR_HINT
                    )
                }
            }
        }

        if (selected != null) {
            lines.add(Component.literal(selected.signatureWithDir).visualOrderText to COLOR_KEY_HINT)
            if (HexConfig.client().showPatternDocsInAutocomplete()) {
                val docs = PatternDocs.docsOf(selected.id)
                if (docs.isNotEmpty()) {
                    val docIndex = autoComplete.chosenDoc.coerceIn(0, docs.size - 1)
                    val doc = docs[docIndex]
                    val header = "[${docIndex + 1}/${docs.size}]" + if (doc.args.isBlank()) "" else " ${doc.args}"
                    lines.add(Component.literal(header).visualOrderText to COLOR_KEY_HINT)
                    if (doc.desc.isNotBlank()) {
                        for (descLine in font.split(Component.literal(doc.desc), DOCS_WRAP_WIDTH)) {
                            lines.add(descLine to COLOR_HINT)
                        }
                    }
                }
            }
        }
        lines.add(Component.translatable("hexcasting.gui.autocomplete.hint").visualOrderText to COLOR_HINT)

        val anchor = this.coordToPx(autoComplete.origin)
        drawTooltipBox(graphics, font, anchor.x.toInt() + 20, anchor.y.toInt(), lines)
    }

    private fun drawAliasOverlay(
        graphics: GuiGraphicsExtractor,
        font: Font,
        editing: PatternDrawState.AliasEditing,
        mouseX: Int,
        mouseY: Int
    ) {
        val lines = mutableListOf<Pair<FormattedCharSequence, Int>>()
        lines.add(editing.entry.rawName().visualOrderText to COLOR_HINT)
        if (editing.text.isEmpty() && this.preeditText.isEmpty()) {
            lines.add(Component.translatable("hexcasting.gui.alias.start_typing").visualOrderText to COLOR_HINT)
        } else {
            lines.add(Component.literal(editing.text).visualOrderText to COLOR_NAME)
        }
        if (this.preeditText.isNotEmpty()) {
            lines.add(
                Component.translatable("hexcasting.gui.autocomplete.composing", this.preeditText)
                    .visualOrderText to COLOR_KEY_HINT
            )
        }
        lines.add(Component.literal(editing.entry.signatureWithDir).visualOrderText to COLOR_KEY_HINT)
        lines.add(Component.translatable("hexcasting.gui.alias.hint").visualOrderText to COLOR_HINT)

        drawTooltipBox(graphics, font, mouseX + 12, mouseY + 12, lines)
    }

    /** Pattern whose origin or any of its points sits on the given grid coordinate. */
    private fun patternAt(mouseX: Int, mouseY: Int): ResolvedPattern? {
        val coord = this.pxToCoord(Vec2(mouseX.toFloat(), mouseY.toFloat()))
        return this.patterns.firstOrNull { entry ->
            val (pat, origin, _) = entry
            origin == coord || pat.positions().any { it + origin == coord }
        }
    }

    private fun drawTooltipBox(
        graphics: GuiGraphicsExtractor,
        font: Font,
        x: Int,
        y: Int,
        lines: List<Pair<FormattedCharSequence, Int>>
    ) {
        if (lines.isEmpty()) return
        val boxWidth = lines.maxOf { font.width(it.first) } + 8
        val boxHeight = lines.size * 10 + 4
        val clampWidth = this.overlayBoundsWidth ?: this.width
        val clampHeight = this.overlayBoundsHeight ?: this.height
        val bx = x.coerceIn(2, (clampWidth - boxWidth - 2).coerceAtLeast(2))
        val by = y.coerceIn(2, (clampHeight - boxHeight - 2).coerceAtLeast(2))
        drawBox(graphics, bx.toFloat(), by.toFloat(), boxWidth.toFloat(), boxHeight.toFloat())

        var textY = by + 3
        for ((text, color) in lines) {
            graphics.text(font, text, bx + 4, textY, color, false)
            textY += 10
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
        val spacing = Mth.clamp(HexConfig.client().gridSpacing(), 0.5, 2.0)
        // Embedded grids keep a fixed base size but still honor the client grid-density setting.
        embeddedHexSize?.let { return it * spacing.toFloat() }
        val scaleModifier = Minecraft.getInstance().player!!.getAttributeValue(HexAttributes.holder(HexAttributes.GRID_ZOOM))

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

        /**
         * Keyboard drawing (ported from Hexcessible): the pattern shape lives in [angles] and is
         * snapped to the closest free spot around [origin], which follows the mouse.
         */
        class KeyboardDrawing(var origin: HexCoord, var originDir: HexDir) : PatternDrawState() {
            val angles: MutableList<HexAngle> = ArrayList()
            var start: HexCoord? = null
            var startDir: HexDir? = null
            var end: HexCoord = origin
            var endDir: HexDir = originDir
        }

        /** Fuzzy pattern search opened with Ctrl+Space. */
        class AutoCompleting(val origin: HexCoord) : PatternDrawState() {
            var query: String = ""
            var chosen: Int = 0
            var chosenDoc: Int = 0
            var suggestions: List<PatternEntry> = emptyList()
            var lockedCount: Int = 0

            /** Opened by right-click: shows the whole list and doesn't close when the mouse wanders. */
            var browse: Boolean = false
        }

        /** Renaming a pattern, opened with Ctrl+E while hovering it. */
        class AliasEditing(val entry: PatternEntry) : PatternDrawState() {
            var text: String = PatternAliases.get(entry.id) ?: ""
        }
    }

    companion object {
        const val LHS_IOTAS_ALLOCATION = 0.7
        const val RHS_IOTAS_ALLOCATION = 0.15

        private const val HOVER_DELAY_MS = 500L
        private const val DOCS_WRAP_WIDTH = 170
        private const val COLOR_SIGNATURE = 0xFF_E6E6E6.toInt()
        private const val COLOR_NAME = 0xFF_5555FF.toInt()
        private const val COLOR_ERROR = 0xFF_FF5555.toInt()
        private const val COLOR_HINT = 0xFF_8A8A8A.toInt()
        private const val COLOR_KEY_HINT = 0xFF_A8A8A8.toInt()

        fun drawBox(graphics: GuiGraphicsExtractor, x: Float, y: Float, w: Float, h: Float, leftMargin: Float = 2.5f) {
            graphics.fill(x.toInt(), y.toInt(), (x + w).toInt(), (y + h).toInt(), 0x50_303030)
            graphics.fill(
                (x + leftMargin).toInt(), (y + 2.5f).toInt(),
                (x + w - 2.5f).toInt(), (y + h - 5f).toInt(), 0x50_303030
            )
        }
    }
}
