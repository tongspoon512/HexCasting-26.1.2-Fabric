package at.petrak.hexcasting.client.guidebook

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style

/**
 * Patchouli 的轻量标记解析器。支持书里实际用到的：
 * `$(br)`、`$(br2)`、`$(li)`、`$(li2)`、`$(#rrggbb)`、`$(l:...)`、`$()`、`/$`、
 * `$(item)`、`$(thing)`、`$(action)`、`$(media)`、`$(hex)`、`$(italics)`、`$(k:key)`、
 * 以及 book.json 里定义的 `_Hex` 一类别名宏。
 */
object GuideMarkup {
    data class Span(
        val text: String,
        val color: Int?,
        val italic: Boolean,
        val bold: Boolean,
        val underlined: Boolean,
        val link: String?
    )

    private data class State(
        val color: Int?,
        val italic: Boolean,
        val bold: Boolean,
        val underlined: Boolean,
        val link: String?
    )

    private val BASE = State(null, false, false, false, null)

    private const val ITEM_COLOR = 0xBB00BB
    private const val THING_COLOR = 0x8D6ACC
    private const val ACTION_COLOR = 0xFC77BE
    private const val MEDIA_COLOR = 0x74B3F2
    private const val HEX_COLOR = 0xB38EF3
    private const val KEY_COLOR = 0x555555

    fun parse(raw: String, macros: Map<String, String>): List<Span> {
        val text = expandMacros(raw, macros)
            .replace("\$(2br)", "\$(br2)")
            .replace("\$(reset)", "$()")
            .replace("\$(clear)", "$()")
            .replace("\$(nocolor)", "\$(0)")
            .replace("\$(obf)", "\$(k)")
            .replace("\$(strike)", "\$(m)")
            .replace("\$(list", "\$(li")
        val out = ArrayList<Span>()
        val sb = StringBuilder()
        var state = BASE
        val stack = ArrayDeque<State>()

        fun flush() {
            if (sb.isNotEmpty()) {
                out += Span(sb.toString(), state.color, state.italic, state.bold, state.underlined, state.link)
                sb.setLength(0)
            }
        }

        var i = 0
        while (i < text.length) {
            when {
                text.startsWith("$/", i) -> {
                    flush()
                    state = if (stack.isEmpty()) BASE else stack.removeLast()
                    i += 2
                }
                text.startsWith("$(", i) -> {
                    val close = text.indexOf(')', i + 2)
                    if (close < 0) {
                        sb.append(text, i, text.length)
                        i = text.length
                    } else {
                        val code = text.substring(i + 2, close)
                        flush()
                        when {
                            code == "" -> {
                                stack.clear()
                                state = BASE
                            }
                            code == "br" -> sb.append('\n')
                            code == "br2" || code == "p" -> sb.append("\n\n")
                            code == "li" -> sb.append("\n\u2022 ")
                            code == "li2" -> sb.append("\n  - ")
                            code == "italic" || code == "italics" || code == "o" -> {
                                stack.addLast(state)
                                state = state.copy(italic = true)
                            }
                            code == "bold" || code == "l" -> {
                                stack.addLast(state)
                                state = state.copy(bold = true)
                            }
                            code == "underline" || code == "n" -> {
                                stack.addLast(state)
                                state = state.copy(underlined = true)
                            }
                            code.startsWith("l:") -> {
                                stack.addLast(state)
                                state = state.copy(link = code.substring(2).trim())
                            }
                            code.startsWith("#") -> {
                                stack.addLast(state)
                                state = state.copy(color = parseColor(code.substring(1)))
                            }
                            code.length == 1 && code[0].lowercaseChar() in '0'..'9' -> {
                                stack.addLast(state)
                                state = state.copy(color = LEGACY_COLORS[code[0] - '0'])
                            }
                            code.length == 1 && code[0].lowercaseChar() in 'a'..'f' -> {
                                stack.addLast(state)
                                state = state.copy(color = LEGACY_COLORS[10 + (code[0].lowercaseChar() - 'a')])
                            }
                            code == "k" || code == "m" -> {
                                // 书中未实际使用混淆/删除线，保持原样即可
                            }
                            code == "item" -> {
                                stack.addLast(state)
                                state = state.copy(color = ITEM_COLOR)
                            }
                            code == "thing" -> {
                                stack.addLast(state)
                                state = state.copy(color = THING_COLOR)
                            }
                            code == "action" -> {
                                stack.addLast(state)
                                state = state.copy(color = ACTION_COLOR)
                            }
                            code == "media" -> {
                                stack.addLast(state)
                                state = state.copy(color = MEDIA_COLOR)
                            }
                            code == "hex" -> {
                                stack.addLast(state)
                                state = state.copy(color = HEX_COLOR)
                            }
                            code.startsWith("k:") -> {
                                val key = "key." + code.substring(2)
                                sb.append(if (I18n.exists(key)) I18n.get(key) else code.substring(2))
                            }
                            else -> sb.append("\$(").append(code).append(')')
                        }
                        i = close + 1
                    }
                }
                else -> {
                    sb.append(text[i])
                    i++
                }
            }
        }
        flush()
        return out
    }

    private fun expandMacros(raw: String, macros: Map<String, String>): String {
        if (macros.isEmpty()) return raw
        var out = raw
        // 长的先替换，避免 `_Hex` 抢先吃掉 `_Hexcasting`
        for ((from, to) in macros.entries.sortedByDescending { it.key.length }) {
            out = out.replace(from, to)
        }
        return out
    }

    private fun parseColor(raw: String): Int? {
        return try {
            if (raw.length == 3) {
                val r = raw[0].digitToInt(16) * 17
                val g = raw[1].digitToInt(16) * 17
                val b = raw[2].digitToInt(16) * 17
                0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
            } else {
                val value = raw.toInt(16)
                if (raw.length >= 8) value else (0xFF shl 24) or (value and 0xFFFFFF)
            }
        } catch (e: NumberFormatException) {
            null
        }
    }

    /** Patchouli 的 $(0-f) 颜色码，取原版聊天色。 */
    private val LEGACY_COLORS = intArrayOf(
        0xFF000000.toInt(), 0xFF0000AA.toInt(), 0xFF00AA00.toInt(), 0xFF00AAAA.toInt(),
        0xFFAA0000.toInt(), 0xFFAA00AA.toInt(), 0xFFFFAA00.toInt(), 0xFFAAAAAA.toInt(),
        0xFF555555.toInt(), 0xFF5555FF.toInt(), 0xFF55FF55.toInt(), 0xFF55FFFF.toInt(),
        0xFFFF5555.toInt(), 0xFFFF55FF.toInt(), 0xFFFFFF55.toInt(), 0xFFFFFFFF.toInt()
    )
}

data class GuideHit(val x0: Int, val y0: Int, val x1: Int, val y1: Int, val target: String)

/**
 * 把带样式的文本按宽度折行后画到页面上，并把链接的位置收集到 [hits]（书页局部坐标）。
 */
object GuideText {
    private const val SPACE = " "

    data class Word(val text: String, val span: GuideMarkup.Span, val spaceBefore: Boolean)

    private sealed interface Token {
        data class W(val word: Word) : Token
        data object Break : Token
    }

    fun draw(
        graphics: GuiGraphicsExtractor,
        font: Font,
        spans: List<GuideMarkup.Span>,
        x: Int,
        y: Int,
        width: Int,
        bottom: Int,
        scroll: Int,
        defaultColor: Int,
        hits: MutableList<GuideHit>
    ): Int {
        val lineHeight = font.lineHeight + 1
        val tokens = tokenize(spans)
        var cursorX = 0
        var cursorY = 0
        var firstWordOnLine = true
        for (token in tokens) {
            when (token) {
                is Token.Break -> {
                    cursorY += lineHeight
                    cursorX = 0
                    firstWordOnLine = true
                }
                is Token.W -> {
                    val word = token.word
                    val wordWidth = font.width(word.text)
                    if (!firstWordOnLine && word.spaceBefore) {
                        val spaceWidth = font.width(SPACE)
                        if (cursorX + spaceWidth + wordWidth > width) {
                            cursorY += lineHeight
                            cursorX = 0
                            firstWordOnLine = true
                        } else {
                            cursorX += spaceWidth
                        }
                    } else if (!firstWordOnLine && cursorX + wordWidth > width) {
                        cursorY += lineHeight
                        cursorX = 0
                        firstWordOnLine = true
                    }

                    // 整词放不下时按字符拆开换行，避免长单词（如 id、URL）戳出页面
                    var remaining = word.text
                    while (remaining.isNotEmpty()) {
                        val available = width - cursorX
                        if (font.width(remaining) <= available) {
                            val drawY = y + cursorY - scroll
                            if (visible(drawY, y, bottom, font.lineHeight)) {
                                drawPiece(graphics, font, remaining, word.span, x + cursorX, drawY, defaultColor)
                                if (word.span.link != null) {
                                    hits += GuideHit(
                                        x + cursorX, drawY, x + cursorX + font.width(remaining),
                                        drawY + font.lineHeight, wordTarget(word.span.link)
                                    )
                                }
                            }
                            cursorX += font.width(remaining)
                            firstWordOnLine = false
                            break
                        }

                        var cut = 0
                        while (cut < remaining.length &&
                            font.width(remaining.substring(0, cut + 1)) <= available
                        ) {
                            cut++
                        }
                        if (cut == 0) {
                            if (cursorX == 0) {
                                cut = 1 // 页面窄到放不下一个字符时也必须前进，避免死循环
                            } else {
                                cursorY += lineHeight
                                cursorX = 0
                                firstWordOnLine = true
                                continue
                            }
                        }
                        val piece = remaining.substring(0, cut)
                        val pieceWidth = font.width(piece)
                        val drawY = y + cursorY - scroll
                        if (visible(drawY, y, bottom, font.lineHeight)) {
                            drawPiece(graphics, font, piece, word.span, x + cursorX, drawY, defaultColor)
                            if (word.span.link != null) {
                                hits += GuideHit(
                                    x + cursorX, drawY, x + cursorX + pieceWidth,
                                    drawY + font.lineHeight, wordTarget(word.span.link)
                                )
                            }
                        }
                        remaining = remaining.substring(cut)
                        cursorY += lineHeight
                        cursorX = 0
                        firstWordOnLine = true
                    }
                }
            }
        }
        return cursorY + lineHeight
    }

    private fun tokenize(spans: List<GuideMarkup.Span>): List<Token> {
        val tokens = ArrayList<Token>()
        for (span in spans) {
            val parts = span.text.split('\n')
            for ((index, part) in parts.withIndex()) {
                if (index > 0) tokens += Token.Break
                if (part.isEmpty()) continue
                val words = part.split(' ')
                for ((wordIndex, word) in words.withIndex()) {
                    if (word.isEmpty()) continue
                    tokens += Token.W(Word(word, span, wordIndex > 0))
                }
            }
        }
        return tokens
    }

    private fun visible(drawY: Int, top: Int, bottom: Int, lineHeight: Int): Boolean =
        drawY + lineHeight > top - lineHeight && drawY < bottom

    private fun drawPiece(
        graphics: GuiGraphicsExtractor,
        font: Font,
        text: String,
        span: GuideMarkup.Span,
        x: Int,
        y: Int,
        defaultColor: Int
    ) {
        var style = Style.EMPTY
        if (span.italic) style = style.withItalic(true)
        if (span.bold) style = style.withBold(true)
        if (span.underlined || span.link != null) style = style.withUnderlined(true)
        val component = Component.literal(text).withStyle(style)
        graphics.text(font, component, x, y, span.color ?: defaultColor, false)
    }

    private fun wordTarget(link: String): String {
        return if (link.startsWith("http://") || link.startsWith("https://")) "url:$link" else "entry:$link"
    }
}
