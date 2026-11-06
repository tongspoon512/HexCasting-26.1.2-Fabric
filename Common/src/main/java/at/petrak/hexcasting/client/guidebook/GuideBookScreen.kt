package at.petrak.hexcasting.client.guidebook

import at.petrak.hexcasting.api.casting.math.HexDir
import at.petrak.hexcasting.api.casting.math.HexPattern
import at.petrak.hexcasting.client.render.PatternColors
import at.petrak.hexcasting.client.render.PatternSettings
import at.petrak.hexcasting.interop.utils.PatternDrawingUtil
import at.petrak.hexcasting.xplat.IXplatAbstractions
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.client.input.PreeditEvent
import net.minecraft.client.resources.language.I18n
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import java.net.URI
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * 不依赖 Patchouli 的内置指南书。数据仍然是官方书的那套 JSON/YAML 翻译键，
 * 这里只负责显示与导航。
 */
class GuideBookScreen(
    private val initialEntry: GuideEntry? = null
) : Screen(Component.translatable("item.hexcasting.book")) {

    private enum class View { HOME, CATEGORY, ENTRY, SEARCH, BOOKMARKS }

    private val data: GuideBookData get() = GuideBook.get()
    private val orderedCategories by lazy { data.orderedCategories() }

    private var view = View.HOME
    private var category: GuideCategory? = null
    private var entry: GuideEntry? = null

    private var spread = 0
    private var scrollLeft = 0
    private var scrollRight = 0
    private var contentLeft = 0
    private var contentRight = 0
    private var bodyHeightLeft = 0
    private var bodyHeightRight = 0

    private var listScroll = 0
    private var listContent = 0

    private var descScroll = 0
    private var descContent = 0
    private var descBodyHeight = 0

    private var searchQuery = ""
    private var searchPreedit = ""
    private var searchResults: List<GuideBookData.SearchResult> = emptyList()
    private var searchSelected = 0
    private var searchScroll = 0
    private var searchContent = 0
    private var searchBody = 0

    private var bookmarkScroll = 0
    private var bookmarkContent = 0
    private var bookmarkBody = 0

    /** 打开搜索/书签列表前所处的视图，Esc 返回时用。 */
    private var overlayReturn = View.HOME

    private val hits = ArrayList<GuideHit>()
    private var originX = 0
    private var originY = 0

    init {
        val start = initialEntry
        if (start != null) {
            openEntry(start, null, resetView = false)
        } else if (!restoreLastPosition()) {
            category = orderedCategories.firstOrNull()?.first
        }
    }

    /** 回到上次退出时的页面；没有记录或数据已失效时返回 false。 */
    private fun restoreLastPosition(): Boolean {
        val pos = GuideBookState.loadPosition() ?: return false
        when (pos.view) {
            "entry" -> {
                val e = pos.entryId?.let { data.entry(it) } ?: return false
                entry = e
                category = data.category(e.categoryId)
                view = View.ENTRY
                val spreads = ((e.pages.size + 1) / 2).coerceAtLeast(1)
                spread = pos.spread.coerceIn(0, spreads - 1)
            }
            "category" -> {
                val cat = pos.categoryId?.let { data.category(it) } ?: return false
                category = cat
                view = View.CATEGORY
            }
            "home" -> {
                view = View.HOME
                category = orderedCategories.firstOrNull()?.first
            }
            else -> return false
        }
        return true
    }

    override fun isPauseScreen(): Boolean = false

    override fun extractBackground(
        graphics: GuiGraphicsExtractor,
        pMouseX: Int,
        pMouseY: Int,
        pPartialTick: Float
    ) {
        graphics.fill(0, 0, this.width, this.height, 0x66000000)
    }

    override fun extractRenderState(
        graphics: GuiGraphicsExtractor,
        pMouseX: Int,
        pMouseY: Int,
        pPartialTick: Float
    ) {
        super.extractRenderState(graphics, pMouseX, pMouseY, pPartialTick)
        hits.clear()
        originX = (this.width - BOOK_W) / 2
        originY = (this.height - BOOK_H) / 2

        val pose = graphics.pose()
        pose.pushMatrix()
        pose.translate(originX.toFloat(), originY.toFloat())
        GuideTextures.blit(graphics, BOOK_TEXTURE, 0, 0, BOOK_W, BOOK_H)

        val mx = pMouseX - originX
        val my = pMouseY - originY
        when (view) {
            View.HOME -> renderHome(graphics, mx, my)
            View.CATEGORY -> renderCategory(graphics, mx, my)
            View.ENTRY -> renderEntry(graphics, mx, my)
            View.SEARCH -> renderSearch(graphics, mx, my)
            View.BOOKMARKS -> renderBookmarks(graphics, mx, my)
        }
        pose.popMatrix()
    }

    // ---------------------------------------------------------------- HOME

    private fun renderHome(graphics: GuiGraphicsExtractor, mx: Int, my: Int) {
        drawHeader(graphics, PAGE_L_X, text("gui.hexcasting.guidebook.categories"))
        listContent = renderCategoryList(graphics, mx, my)
        descContent = drawDescription(graphics, GuideBook.string(data.landingTextKey))
        drawFooter(graphics, mx, my, null)
    }

    private fun renderCategory(graphics: GuiGraphicsExtractor, mx: Int, my: Int) {
        val cat = category ?: run {
            view = View.HOME
            return
        }
        drawHeader(graphics, PAGE_L_X, GuideBook.translate(cat.nameKey).string)
        listContent = renderEntryList(graphics, cat, mx, my)
        descContent = drawDescription(graphics, GuideBook.string(cat.descKey))
        drawFooter(graphics, mx, my, null, showHome = true, homeLabel = text("gui.hexcasting.guidebook.back"))
    }

    private fun renderCategoryList(graphics: GuiGraphicsExtractor, mx: Int, my: Int): Int {
        val x = PAGE_L_X
        val top = PAGE_Y + 17
        val bottom = PAGE_Y + PAGE_H - 10
        val rowH = 18
        val total = orderedCategories.size * rowH
        val maxScroll = (total - (bottom - top)).coerceAtLeast(0)
        listScroll = listScroll.coerceIn(0, maxScroll)

        var y = top - listScroll
        for ((cat, depth) in orderedCategories) {
            if (y + rowH > top - rowH && y < bottom) {
                val hover = mx in x until x + PAGE_W && my in y until y + rowH - 1
                drawListRow(
                    graphics, x, y, PAGE_W, rowH,
                    cat.icon, cat.iconTexture,
                    GuideBook.translate(cat.nameKey).string,
                    depth * 8, hover, "category:" + cat.id
                )
            }
            y += rowH
        }
        return total
    }

    private fun renderEntryList(
        graphics: GuiGraphicsExtractor,
        cat: GuideCategory,
        mx: Int,
        my: Int
    ): Int {
        val entries = data.entriesOf(cat)
        val x = PAGE_L_X
        val top = PAGE_Y + 17
        val bottom = PAGE_Y + PAGE_H - 10
        val rowH = 18
        val total = entries.size * rowH
        val maxScroll = (total - (bottom - top)).coerceAtLeast(0)
        listScroll = listScroll.coerceIn(0, maxScroll)

        var y = top - listScroll
        for (e in entries) {
            if (y + rowH > top - rowH && y < bottom) {
                val hover = mx in x until x + PAGE_W && my in y until y + rowH - 1
                drawListRow(
                    graphics, x, y, PAGE_W, rowH,
                    e.icon, e.iconTexture,
                    GuideBook.translate(e.nameKey).string,
                    0, hover, "entry:" + e.id
                )
            }
            y += rowH
        }
        return total
    }

    private fun drawListRow(
        graphics: GuiGraphicsExtractor,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        icon: ItemStack,
        iconTexture: Identifier?,
        label: String,
        indent: Int,
        hovered: Boolean,
        target: String
    ) {
        if (hovered) {
            graphics.fill(x, y, x + width, y + height - 1, 0x3054398A)
        }
        val iconX = x + 2 + indent
        if (!icon.isEmpty) {
            graphics.item(icon, iconX, y + 1)
        } else if (iconTexture != null) {
            GuideTextures.blit(graphics, iconTexture, iconX, y + 1, 16, 16)
        } else {
            graphics.fill(iconX, y + 1, iconX + 16, y + 17, 0x4054398A)
        }
        val available = width - 22 - indent
        val shown = font.plainSubstrByWidth(label, available)
        val finalText = if (shown != label) shown.dropLast(1) + "\u2026" else shown
        graphics.text(font, finalText, x + 20 + indent, y + 5, if (hovered) LINK_COLOR else BODY_COLOR, false)
        hits += GuideHit(x, y, x + width, y + height, target)
    }

    private fun drawDescription(graphics: GuiGraphicsExtractor, raw: String): Int {
        val x = PAGE_R_X + 4
        val top = PAGE_Y + 4
        val bottom = PAGE_Y + PAGE_H - 10
        descBodyHeight = bottom - top
        val content = GuideText.draw(
            graphics, font, GuideMarkup.parse(raw, data.macros),
            x, top, PAGE_W - 8, bottom, descScroll, BODY_COLOR, hits
        )
        val maxScroll = (content - descBodyHeight).coerceAtLeast(0)
        descScroll = descScroll.coerceIn(0, maxScroll)
        return content
    }

    // --------------------------------------------------------------- ENTRY

    private fun renderEntry(graphics: GuiGraphicsExtractor, mx: Int, my: Int) {
        val e = entry ?: run {
            view = View.HOME
            return
        }
        val pages = e.pages
        val spreads = ((pages.size + 1) / 2).coerceAtLeast(1)
        if (spread >= spreads) spread = spreads - 1
        if (spread < 0) spread = 0

        val leftIndex = spread * 2
        val rightIndex = leftIndex + 1

        val leftHeader = if (leftIndex < pages.size) headerOf(e, pages[leftIndex], leftIndex) else null
        bodyHeightLeft = PAGE_H - if (leftHeader != null) 18 else 5
        contentLeft = if (leftIndex < pages.size) {
            renderPage(graphics, e, leftIndex, PAGE_L_X, leftHeader, mx, my, scrollLeft)
        } else 0

        val rightHeader = if (rightIndex < pages.size) headerOf(e, pages[rightIndex], rightIndex) else null
        bodyHeightRight = PAGE_H - if (rightHeader != null) 18 else 5
        contentRight = if (rightIndex < pages.size) {
            renderPage(graphics, e, rightIndex, PAGE_R_X, rightHeader, mx, my, scrollRight)
        } else 0

        val maxL = (contentLeft - bodyHeightLeft).coerceAtLeast(0)
        val maxR = (contentRight - bodyHeightRight).coerceAtLeast(0)
        scrollLeft = scrollLeft.coerceIn(0, maxL)
        scrollRight = scrollRight.coerceIn(0, maxR)

        val pageLabel = Component.translatable(
            "gui.hexcasting.guidebook.page",
            (leftIndex + 1).coerceAtMost(pages.size),
            pages.size
        ).string
        val bookmarkPage = leftIndex.coerceAtMost(pages.lastIndex.coerceAtLeast(0))
        drawFooter(
            graphics, mx, my, pageLabel,
            showPrev = spread > 0,
            showNext = spread + 1 < spreads,
            showHome = true,
            homeLabel = text("gui.hexcasting.guidebook.home"),
            bookmark = if (pages.isEmpty()) null else e.id to bookmarkPage
        )
    }

    private fun renderPage(
        graphics: GuiGraphicsExtractor,
        entry: GuideEntry,
        index: Int,
        pageX: Int,
        header: Component?,
        mx: Int,
        my: Int,
        scroll: Int
    ): Int {
        val page = entry.pages[index]
        if (header != null) {
            graphics.centeredText(font, header, pageX + PAGE_W / 2, PAGE_Y + 4, TITLE_COLOR)
        }
        val top = PAGE_Y + if (header != null) 18 else 3
        val bottom = PAGE_Y + PAGE_H - 10
        val width = PAGE_W - 8
        val x = pageX + 4

        return when (page.type) {
            "patchouli:text" -> drawBody(graphics, page.string("text"), x, top, width, bottom, scroll)
            "patchouli:link" -> drawLinkPage(graphics, page, x, top, width, bottom, scroll)
            "patchouli:image" -> drawImagePage(graphics, page, pageX, top, width, bottom, scroll)
            "patchouli:spotlight" -> drawSpotlightPage(graphics, page, x, top, width, bottom, scroll)
            "patchouli:empty" -> 0
            "patchouli:crafting" -> {
                val recipe = Identifier.tryParse(page.string("recipe"))?.let { GuideRecipes.get(it) }
                drawCraftingPage(graphics, page, recipe, x, top, width, bottom, scroll)
            }
            "hexcasting:crafting_multi" -> drawMultiCraftingPage(graphics, page, x, top, width, bottom, scroll)
            "hexcasting:pattern" -> drawPatternPage(graphics, page, pageX, top, width, bottom, scroll)
            "hexcasting:manual_pattern" -> drawManualPatternPage(graphics, page, pageX, top, width, bottom, scroll)
            "hexcasting:manual_pattern_nosig" ->
                drawManualPatternPage(graphics, page, pageX, top, width, bottom, scroll)
            "hexcasting:brainsweep" -> drawBrainsweepPage(graphics, page, x, top, width, bottom, scroll)
            else -> drawBody(graphics, page.string("text"), x, top, width, bottom, scroll)
        }
    }

    private fun headerOf(entry: GuideEntry, page: GuidePage, index: Int): Component? {
        return when (page.type) {
            "hexcasting:pattern", "hexcasting:manual_pattern" -> actionTitle(page)
            "hexcasting:manual_pattern_nosig" -> {
                val key = page.string("header")
                if (key.isBlank()) actionTitle(page) else GuideBook.translate(key)
            }
            "hexcasting:crafting_multi" -> {
                val key = page.string("heading")
                if (key.isBlank()) null else GuideBook.translate(key)
            }
            "hexcasting:brainsweep" -> null
            else -> if (index == 0) GuideBook.translate(entry.nameKey) else null
        }
    }

    private fun actionTitle(page: GuidePage): Component {
        val opId = page.string("op_id", page.string("anchor"))
        if (opId.isBlank()) return Component.empty()
        return GuideBook.translate("hexcasting.action.$opId")
    }

    private fun drawBody(
        graphics: GuiGraphicsExtractor,
        raw: String,
        x: Int,
        top: Int,
        width: Int,
        bottom: Int,
        scroll: Int
    ): Int {
        // 上游书的 text 字段全部是语言键（且键里的点被改写过），先解析再排版
        val text = GuideBook.string(raw)
        if (text.isBlank()) return 0
        return GuideText.draw(
            graphics, font, GuideMarkup.parse(text, data.macros),
            x, top, width, bottom, scroll, BODY_COLOR, hits
        )
    }

    private fun drawLinkPage(
        graphics: GuiGraphicsExtractor,
        page: GuidePage,
        x: Int,
        top: Int,
        width: Int,
        bottom: Int,
        scroll: Int
    ): Int {
        val content = drawBody(graphics, page.string("text"), x, top, width, bottom, scroll)
        val linkLabel = GuideBook.string(page.string("link_text"))
        val url = page.string("url")
        if (linkLabel.isNotBlank() && url.isNotBlank()) {
            val y = top + content - scroll
            if (y < bottom) {
                val label = "\u2192 $linkLabel"
                graphics.text(font, label, x, y + 2, LINK_COLOR, false)
                hits += GuideHit(x, y, x + font.width(label), y + font.lineHeight + 2, "url:$url")
            }
            return content + font.lineHeight + 4
        }
        return content
    }

    private fun drawImagePage(
        graphics: GuiGraphicsExtractor,
        page: GuidePage,
        pageX: Int,
        top: Int,
        width: Int,
        bottom: Int,
        scroll: Int
    ): Int {
        val images = page.json.getAsJsonArray("images")
        val id = images?.firstOrNull()?.asString?.let { Identifier.tryParse(it.removePrefix("#")) }
        val size = 96
        val x = pageX + (PAGE_W - size) / 2
        if (id != null) {
            GuideTextures.blit(graphics, id, x, top - scroll, size, size)
        } else {
            graphics.fill(x, top - scroll, x + size, top - scroll + size, 0x3054398A)
        }
        val titleKey = page.string("title")
        var height = size
        if (titleKey.isNotBlank()) {
            graphics.centeredText(
                font, GuideBook.translate(titleKey), pageX + PAGE_W / 2,
                top + size - scroll + 2, MUTED_COLOR
            )
            height += font.lineHeight + 2
        }
        val text = page.string("text")
        if (text.isNotBlank()) {
            height += drawBody(graphics, text, pageX + 4, top + height, width, bottom, scroll)
        }
        return height
    }

    private fun drawSpotlightPage(
        graphics: GuiGraphicsExtractor,
        page: GuidePage,
        x: Int,
        top: Int,
        width: Int,
        bottom: Int,
        scroll: Int
    ): Int {
        val stack = GuideBook.stackOf(page.string("item"))
        var height = 0
        if (!stack.isEmpty) {
            val pose = graphics.pose()
            pose.pushMatrix()
            pose.translate((x + width / 2 - 16).toFloat(), (top - scroll).toFloat())
            pose.scale(2f, 2f)
            graphics.item(stack, 0, 0)
            pose.popMatrix()
            height += 32
            val name = stack.hoverName.string
            graphics.centeredText(font, name, x + width / 2, top + height - scroll + 2, TITLE_COLOR)
            height += font.lineHeight + 4
        }
        height += drawBody(graphics, page.string("text"), x, top + height, width, bottom, scroll)
        return height
    }

    private fun drawCraftingPage(
        graphics: GuiGraphicsExtractor,
        page: GuidePage,
        recipe: GuideRecipes.Recipe?,
        x: Int,
        top: Int,
        width: Int,
        bottom: Int,
        scroll: Int
    ): Int {
        var height = font.lineHeight + 2
        if (recipe != null && recipe.grid.any { it != null }) {
            height = drawRecipeGrid(graphics, recipe, x, top - scroll, width)
        }
        height += drawBody(graphics, page.string("text"), x, top + height, width, bottom, scroll)
        return height
    }

    private fun drawMultiCraftingPage(
        graphics: GuiGraphicsExtractor,
        page: GuidePage,
        x: Int,
        top: Int,
        width: Int,
        bottom: Int,
        scroll: Int
    ): Int {
        var height = drawBody(graphics, page.string("text"), x, top, width, bottom, scroll)
        val recipes = page.json.getAsJsonArray("recipes")
        if (recipes != null) {
            height += 4
            var i = 0
            for (element in recipes) {
                val id = Identifier.tryParse(element.asString) ?: continue
                val recipe = GuideRecipes.get(id) ?: continue
                val col = i % 5
                val row = i / 5
                val ix = x + col * 20
                val iy = top + height - scroll + row * 20
                if (iy < bottom - 18) {
                    graphics.item(recipe.result, ix, iy)
                }
                i++
            }
            height += ((i + 4) / 5) * 20
        }
        return height
    }

    private fun drawRecipeGrid(
        graphics: GuiGraphicsExtractor,
        recipe: GuideRecipes.Recipe,
        x: Int,
        top: Int,
        width: Int
    ): Int {
        val slot = 18
        val gridW = slot * 3
        val gridX = x + 2
        for (r in 0 until 3) {
            for (c in 0 until 3) {
                val sx = gridX + c * slot
                val sy = top + r * slot
                graphics.fill(sx, sy, sx + slot - 1, sy + slot - 1, 0x2054398A)
                val stack = recipe.grid.getOrNull(r * 3 + c)
                if (stack != null && !stack.isEmpty) {
                    graphics.item(stack, sx + 1, sy + 1)
                }
            }
        }
        val arrowX = gridX + gridW + 3
        graphics.text(font, "\u2192", arrowX, top + 20, MUTED_COLOR, false)
        val resultX = arrowX + 12
        graphics.fill(resultX, top + 18, resultX + slot - 1, top + 18 + slot - 1, 0x3054398A)
        if (!recipe.result.isEmpty) {
            graphics.item(recipe.result, resultX + 1, top + 19)
        }
        return slot * 3 + 6
    }

    private fun drawPatternPage(
        graphics: GuiGraphicsExtractor,
        page: GuidePage,
        pageX: Int,
        top: Int,
        width: Int,
        bottom: Int,
        scroll: Int
    ): Int {
        val pattern = lookupPattern(page)
        if (pattern != null) {
            renderPatterns(graphics, listOf(pattern), pageX, top - scroll)
        }
        val signature = signatureText(page)
        var height = 64
        if (signature != null) {
            graphics.text(font, signature, pageX + 4, top + height - scroll, MUTED_COLOR, false)
            height += font.lineHeight + 2
        }
        height += drawBody(graphics, page.string("text"), pageX + 4, top + height, width, bottom, scroll)
        return height
    }

    private fun drawManualPatternPage(
        graphics: GuiGraphicsExtractor,
        page: GuidePage,
        pageX: Int,
        top: Int,
        width: Int,
        bottom: Int,
        scroll: Int
    ): Int {
        val patterns = manualPatterns(page)
        if (patterns.isNotEmpty()) {
            renderPatterns(graphics, patterns, pageX, top - scroll)
        }
        var height = 64
        val signature = signatureText(page)
        if (signature != null) {
            graphics.text(font, signature, pageX + 4, top + height - scroll, MUTED_COLOR, false)
            height += font.lineHeight + 2
        }
        height += drawBody(graphics, page.string("text"), pageX + 4, top + height, width, bottom, scroll)
        return height
    }

    private fun drawBrainsweepPage(
        graphics: GuiGraphicsExtractor,
        page: GuidePage,
        x: Int,
        top: Int,
        width: Int,
        bottom: Int,
        scroll: Int
    ): Int {
        val recipeId = Identifier.tryParse(page.string("recipe"))
        val result = recipeId?.let { GuideRecipes.get(it)?.result }
        var height = 0
        if (result != null && !result.isEmpty) {
            val pose = graphics.pose()
            pose.pushMatrix()
            pose.translate((x + width / 2 - 16).toFloat(), (top - scroll).toFloat())
            pose.scale(2f, 2f)
            graphics.item(result, 0, 0)
            pose.popMatrix()
            height += 34
        }
        height += drawBody(graphics, page.string("text"), x, top + height, width, bottom, scroll)
        return height
    }

    private fun signatureText(page: GuidePage): String? {
        val input = page.string("input")
        val output = page.string("output")
        if (input.isBlank() && output.isBlank()) return null
        return buildString {
            if (input.isNotBlank()) append(input)
            append(" \u2192 ")
            if (output.isNotBlank()) append(output)
        }
    }

    private fun lookupPattern(page: GuidePage): HexPattern? {
        val opId = Identifier.tryParse(page.string("op_id", page.string("anchor"))) ?: return null
        val registry = IXplatAbstractions.INSTANCE.actionRegistry
        val key = ResourceKey.create(registry.key(), opId)
        val holder = registry.getOptional(key).orElse(null) ?: return null
        return holder.prototype()
    }

    private fun manualPatterns(page: GuidePage): List<HexPattern> {
        val array = page.json.getAsJsonArray("patterns")
        if (array != null) {
            return array.mapNotNull { element ->
                if (!element.isJsonObject) return@mapNotNull null
                val obj = element.asJsonObject
                val signature = obj.get("signature")?.asString ?: return@mapNotNull null
                val dirName = obj.get("startdir")?.asString ?: return@mapNotNull null
                val dir = runCatching { HexDir.valueOf(dirName) }.getOrNull() ?: return@mapNotNull null
                HexPattern.fromAngles(signature, dir)
            }
        }
        val obj = page.json.getAsJsonObject("patterns")
        if (obj != null) {
            val signature = obj.get("signature")?.asString
            val dirName = obj.get("startdir")?.asString
            if (signature != null && dirName != null) {
                val dir = runCatching { HexDir.valueOf(dirName) }.getOrNull()
                if (dir != null) return listOf(HexPattern.fromAngles(signature, dir))
            }
        }
        return emptyList()
    }

    private fun renderPatterns(
        graphics: GuiGraphicsExtractor,
        patterns: List<HexPattern>,
        pageX: Int,
        top: Int
    ) {
        if (patterns.isEmpty()) return
        val cols = ceil(sqrt(patterns.size.toDouble())).toInt().coerceAtLeast(1)
        val rows = ceil(patterns.size / cols.toDouble()).toInt().coerceAtLeast(1)
        val cellW = PAGE_W / cols.toDouble()
        val cellH = 64.0 / rows
        val settings = PatternSettings(
            "guide" + patterns.size,
            PatternSettings.PositionSettings(
                cellW, cellH, 2.0, 2.0,
                PatternSettings.AxisAlignment.CENTER_FIT,
                PatternSettings.AxisAlignment.CENTER_FIT,
                16.0, 0.0, 0.0
            ),
            PatternSettings.StrokeSettings.fromStroke(4.0),
            PatternSettings.ZappySettings.READABLE
        )
        val colors = PatternColors.DEFAULT_GRADIENT_COLOR.withDots(true, true)
        for ((index, pattern) in patterns.withIndex()) {
            val cx = pageX + cellW * (index % cols)
            val cy = top + cellH * (index / cols)
            PatternDrawingUtil.renderPattern(
                graphics, pattern, settings, colors, 0.0, 4, cx.toFloat(), cy.toFloat()
            )
        }
    }

    // -------------------------------------------------------------- SEARCH

    private fun renderSearch(graphics: GuiGraphicsExtractor, mx: Int, my: Int) {
        drawHeader(graphics, PAGE_L_X, text("gui.hexcasting.guidebook.search"))

        val fieldX = PAGE_L_X + 4
        val fieldTop = PAGE_Y + 17
        val fieldW = PAGE_W - 8
        val fieldH = 13
        graphics.fill(fieldX, fieldTop, fieldX + fieldW, fieldTop + fieldH, FIELD_BG)
        graphics.fill(fieldX, fieldTop, fieldX + fieldW, fieldTop + 1, FIELD_BORDER)
        graphics.fill(fieldX, fieldTop + fieldH - 1, fieldX + fieldW, fieldTop + fieldH, FIELD_BORDER)
        graphics.fill(fieldX, fieldTop, fieldX + 1, fieldTop + fieldH, FIELD_BORDER)
        graphics.fill(fieldX + fieldW - 1, fieldTop, fieldX + fieldW, fieldTop + fieldH, FIELD_BORDER)

        val shown = searchQuery + searchPreedit
        val available = fieldW - 6
        val textX = fieldX + 3 + minOf(0, available - font.width(shown))
        graphics.text(font, shown, textX, fieldTop + 3, BODY_COLOR, false)
        if (searchPreedit.isEmpty() && System.currentTimeMillis() % 1000 < 500) {
            val caretX = textX + font.width(shown)
            graphics.fill(caretX, fieldTop + 2, caretX + 1, fieldTop + fieldH - 2, BODY_COLOR)
        }

        val x = PAGE_L_X
        val top = fieldTop + fieldH + 5
        val bottom = PAGE_Y + PAGE_H - 10
        val rowH = 18
        searchBody = bottom - top
        searchContent = searchResults.size * rowH
        val maxScroll = (searchContent - searchBody).coerceAtLeast(0)
        searchScroll = searchScroll.coerceIn(0, maxScroll)

        if (searchQuery.isBlank()) {
            graphics.text(font, text("gui.hexcasting.guidebook.searchEmpty"), x + 2, top + 2, MUTED_COLOR, false)
        } else if (searchResults.isEmpty()) {
            graphics.text(font, text("gui.hexcasting.guidebook.searchNoMatch"), x + 2, top + 2, MUTED_COLOR, false)
        } else {
            var y = top - searchScroll
            for ((index, result) in searchResults.withIndex()) {
                if (y + rowH > top - rowH && y < bottom) {
                    val hover = mx in x until x + PAGE_W && my in y until y + rowH - 1
                    val label = buildString {
                        append(GuideBook.translate(result.entry.nameKey).string)
                        if (result.page >= 0) {
                            append("  ")
                            append(pageShort(result.page + 1))
                        }
                    }
                    drawListRow(
                        graphics, x, y, PAGE_W, rowH,
                        result.entry.icon, result.entry.iconTexture,
                        label, 0, hover || index == searchSelected,
                        "search:" + result.entry.id + "#" + result.page
                    )
                }
                y += rowH
            }
        }

        drawWrapped(
            graphics,
            text("gui.hexcasting.guidebook.searchHint"),
            PAGE_R_X + 4, PAGE_Y + 4, PAGE_W - 8, PAGE_Y + PAGE_H - 10, 0, MUTED_COLOR
        )

        drawFooter(graphics, mx, my, null)
    }

    // ------------------------------------------------------------ BOOKMARKS

    private fun renderBookmarks(graphics: GuiGraphicsExtractor, mx: Int, my: Int) {
        drawHeader(graphics, PAGE_L_X, text("gui.hexcasting.guidebook.bookmarks"))

        val valid = GuideBookState.bookmarks().mapNotNull { bookmark ->
            data.entry(bookmark.entryId)?.let { it to bookmark.page }
        }
        val x = PAGE_L_X
        val top = PAGE_Y + 17
        val bottom = PAGE_Y + PAGE_H - 10
        val rowH = 18
        bookmarkBody = bottom - top
        bookmarkContent = valid.size * rowH
        val maxScroll = (bookmarkContent - bookmarkBody).coerceAtLeast(0)
        bookmarkScroll = bookmarkScroll.coerceIn(0, maxScroll)

        if (valid.isEmpty()) {
            graphics.text(font, text("gui.hexcasting.guidebook.bookmarksEmpty"), x + 2, top + 2, MUTED_COLOR, false)
        } else {
            var y = top - bookmarkScroll
            for ((entry, page) in valid) {
                if (y + rowH > top - rowH && y < bottom) {
                    val hover = mx in x until x + PAGE_W && my in y until y + rowH - 1
                    val label = GuideBook.translate(entry.nameKey).string + "  " + pageShort(page + 1)
                    val target = "bookmark:" + entry.id + "#" + page
                    drawListRow(graphics, x, y, PAGE_W, rowH, entry.icon, entry.iconTexture, label, 0, hover, target)

                    val removeX = x + PAGE_W - 10
                    val removeHover = mx in removeX - 2 until x + PAGE_W && my in y until y + rowH - 1
                    graphics.text(
                        font, "\u00d7", removeX, y + 5,
                        if (removeHover) LINK_COLOR else MUTED_COLOR, false
                    )
                    hits += GuideHit(removeX - 2, y, x + PAGE_W, y + rowH, "unbookmark:" + entry.id + "#" + page)
                }
                y += rowH
            }
        }

        drawWrapped(
            graphics,
            text("gui.hexcasting.guidebook.bookmarksHint"),
            PAGE_R_X + 4, PAGE_Y + 4, PAGE_W - 8, PAGE_Y + PAGE_H - 10, 0, MUTED_COLOR
        )

        drawFooter(graphics, mx, my, null)
    }

    /** 画一段自动折行的纯文本（不生成链接命中）。 */
    private fun drawWrapped(
        graphics: GuiGraphicsExtractor,
        raw: String,
        x: Int,
        top: Int,
        width: Int,
        bottom: Int,
        scroll: Int,
        color: Int
    ) {
        if (raw.isBlank()) return
        val spans = listOf(GuideMarkup.Span(raw, null, false, false, false, null))
        GuideText.draw(graphics, font, spans, x, top, width, bottom, scroll, color, hits)
    }

    private fun pageShort(page: Int): String =
        if (I18n.exists("gui.hexcasting.guidebook.pageShort")) {
            I18n.get("gui.hexcasting.guidebook.pageShort", page)
        } else {
            "p.$page"
        }

    // --------------------------------------------------------------- FOOTER

    private fun drawFooter(
        graphics: GuiGraphicsExtractor,
        mx: Int,
        my: Int,
        pageLabel: String?,
        showPrev: Boolean = false,
        showNext: Boolean = false,
        showHome: Boolean = false,
        homeLabel: String = "",
        bookmark: Pair<String, Int>? = null
    ) {
        val y = BOOK_H - 11
        if (showPrev) {
            val hover = mx in 8 until 22 && my in y until y + 11
            graphics.text(font, "<", 10, y, if (hover) LINK_COLOR else MUTED_COLOR, false)
            hits += GuideHit(8, y, 22, y + 11, "nav:prev")
        }
        if (showNext) {
            val hover = mx in 24 until 38 && my in y until y + 11
            graphics.text(font, ">", 26, y, if (hover) LINK_COLOR else MUTED_COLOR, false)
            hits += GuideHit(24, y, 38, y + 11, "nav:next")
        }
        if (bookmark != null) {
            val starred = GuideBookState.isBookmarked(bookmark.first, bookmark.second)
            val hover = mx in 40 until 54 && my in y until y + 11
            graphics.text(
                font, if (starred) "\u2605" else "\u2606", 42, y,
                if (hover || starred) LINK_COLOR else MUTED_COLOR, false
            )
            hits += GuideHit(40, y, 54, y + 11, "nav:toggleBookmark")
        }
        if (pageLabel != null) {
            // 页码放在偏左的位置，给右侧的搜索/书签标签让出空间
            graphics.centeredText(font, pageLabel, BOOK_W / 3, y, MUTED_COLOR)
        }

        // 从右往左排：目录/返回、书签、搜索
        var right = BOOK_W - 8
        if (showHome && homeLabel.isNotBlank()) {
            val width = font.width(homeLabel)
            val x = right - width
            val hover = mx in x - 2 until right + 2 && my in y until y + 11
            graphics.text(font, homeLabel, x, y, if (hover) LINK_COLOR else MUTED_COLOR, false)
            hits += GuideHit(x - 2, y, right + 2, y + 11, "nav:home")
            right = x - 8
        }
        for ((labelKey, target) in listOf(
            "gui.hexcasting.guidebook.bookmarks" to "nav:bookmarks",
            "gui.hexcasting.guidebook.search" to "nav:search"
        )) {
            val label = text(labelKey)
            val width = font.width(label)
            val x = right - width
            val hover = mx in x - 2 until right + 2 && my in y until y + 11
            graphics.text(font, label, x, y, if (hover) LINK_COLOR else MUTED_COLOR, false)
            hits += GuideHit(x - 2, y, right + 2, y + 11, target)
            right = x - 8
        }
    }

    private fun drawHeader(graphics: GuiGraphicsExtractor, pageX: Int, label: String) {
        graphics.centeredText(font, label, pageX + PAGE_W / 2, PAGE_Y + 4, TITLE_COLOR)
    }

    private fun text(key: String): String =
        if (I18n.exists(key)) I18n.get(key) else key

    // --------------------------------------------------------------- INPUT

    override fun mouseClicked(event: MouseButtonEvent, doubleClick: Boolean): Boolean {
        if (super.mouseClicked(event, doubleClick)) return true
        if (event.button() == 1) {
            onClose()
            return true
        }
        val bx = (event.x - originX).toInt()
        val by = (event.y - originY).toInt()
        for (hit in hits.asReversed()) {
            if (bx in hit.x0 until hit.x1 && by in hit.y0 until hit.y1) {
                activate(hit.target)
                return true
            }
        }
        return false
    }

    override fun mouseScrolled(
        pMouseX: Double,
        pMouseY: Double,
        scrollX: Double,
        pDelta: Double
    ): Boolean {
        val bx = (pMouseX - originX).toInt()
        val by = (pMouseY - originY).toInt()
        if (bx !in 0 until BOOK_W || by !in 0 until BOOK_H) return false
        val leftPage = bx < BOOK_W / 2

        when (view) {
            View.ENTRY -> {
                val content = if (leftPage) contentLeft else contentRight
                val body = if (leftPage) bodyHeightLeft else bodyHeightRight
                if (content > body) {
                    if (leftPage) {
                        scrollLeft = (scrollLeft - pDelta * 12).toInt().coerceIn(0, content - body)
                    } else {
                        scrollRight = (scrollRight - pDelta * 12).toInt().coerceIn(0, content - body)
                    }
                } else if (pDelta < 0) {
                    nextSpread()
                } else if (pDelta > 0) {
                    prevSpread()
                }
            }
            View.HOME, View.CATEGORY -> {
                if (leftPage) {
                    val max = (listContent - LIST_BODY_HEIGHT).coerceAtLeast(0)
                    listScroll = (listScroll - pDelta * 12).toInt().coerceIn(0, max)
                } else {
                    val max = (descContent - descBodyHeight).coerceAtLeast(0)
                    descScroll = (descScroll - pDelta * 12).toInt().coerceIn(0, max)
                }
            }
            View.SEARCH -> {
                if (leftPage) {
                    val max = (searchContent - searchBody).coerceAtLeast(0)
                    searchScroll = (searchScroll - pDelta * 12).toInt().coerceIn(0, max)
                }
            }
            View.BOOKMARKS -> {
                if (leftPage) {
                    val max = (bookmarkContent - bookmarkBody).coerceAtLeast(0)
                    bookmarkScroll = (bookmarkScroll - pDelta * 12).toInt().coerceIn(0, max)
                }
            }
        }
        return true
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (view == View.SEARCH && handleSearchKey(event)) return true
        if (view == View.BOOKMARKS && event.isEscape()) {
            closeOverlay()
            return true
        }
        if (super.keyPressed(event)) return true
        return when (event.key()) {
            KEY_LEFT -> {
                if (view == View.ENTRY) {
                    prevSpread()
                    true
                } else false
            }
            KEY_RIGHT -> {
                if (view == View.ENTRY) {
                    nextSpread()
                    true
                } else false
            }
            KEY_BACKSPACE -> when (view) {
                View.ENTRY, View.CATEGORY -> {
                    goHome()
                    true
                }
                View.BOOKMARKS -> {
                    closeOverlay()
                    true
                }
                else -> false
            }
            KEY_F -> {
                if (view != View.SEARCH) {
                    openSearch()
                    true
                } else false
            }
            KEY_B -> {
                if (view != View.SEARCH && view != View.BOOKMARKS) {
                    openBookmarks()
                    true
                } else false
            }
            else -> false
        }
    }

    override fun charTyped(event: CharacterEvent): Boolean {
        if (view != View.SEARCH) return super.charTyped(event)
        val codepoint = event.codepoint()
        if (codepoint < 0x20 || codepoint == 0x7F) return false
        searchQuery += String(Character.toChars(codepoint))
        refreshSearch()
        return true
    }

    override fun preeditUpdated(event: PreeditEvent?): Boolean {
        if (view != View.SEARCH) return super.preeditUpdated(event)
        searchPreedit = event?.fullText() ?: ""
        return true
    }

    /** 搜索结果里的按键处理；返回 false 时交给父类（例如输入法的组字按键）。 */
    private fun handleSearchKey(event: KeyEvent): Boolean {
        if (searchPreedit.isNotEmpty()) {
            if (event.isEscape()) {
                searchPreedit = ""
                return true
            }
            // 组字期间的后退/确认交给 IME，避免把拼音当成搜索词
            return false
        }
        when (event.key()) {
            KEY_ESCAPE -> {
                closeOverlay()
                return true
            }
            KEY_BACKSPACE -> {
                if (searchQuery.isNotEmpty()) {
                    searchQuery = searchQuery.dropLast(1)
                    refreshSearch()
                }
                return true
            }
            KEY_ENTER -> {
                openSelectedResult()
                return true
            }
            KEY_UP -> {
                if (searchResults.isNotEmpty()) {
                    searchSelected = (searchSelected - 1 + searchResults.size) % searchResults.size
                    ensureSearchVisible()
                }
                return true
            }
            KEY_DOWN -> {
                if (searchResults.isNotEmpty()) {
                    searchSelected = (searchSelected + 1) % searchResults.size
                    ensureSearchVisible()
                }
                return true
            }
        }
        return false
    }

    private fun openSearch() {
        if (view != View.SEARCH) overlayReturn = currentSavedView()
        view = View.SEARCH
        searchPreedit = ""
        searchScroll = 0
        refreshSearch()
    }

    private fun openBookmarks() {
        if (view != View.SEARCH && view != View.BOOKMARKS) overlayReturn = currentSavedView()
        view = View.BOOKMARKS
        bookmarkScroll = 0
    }

    private fun closeOverlay() {
        searchPreedit = ""
        view = overlayReturn
    }

    private fun currentSavedView(): View =
        if (view == View.SEARCH || view == View.BOOKMARKS) overlayReturn else view

    private fun refreshSearch() {
        searchResults = data.search(searchQuery)
        searchSelected = 0
        searchScroll = 0
    }

    private fun openSelectedResult() {
        val result = searchResults.getOrNull(searchSelected) ?: return
        openEntry(result.entry, null, resetView = true)
        if (result.page >= 0) spread = result.page / 2
    }

    private fun ensureSearchVisible() {
        val rowH = 18
        val top = PAGE_Y + 17 + 13 + 5
        val body = (PAGE_Y + PAGE_H - 10) - top
        val y = searchSelected * rowH
        if (y < searchScroll) searchScroll = y
        else if (y + rowH > searchScroll + body) searchScroll = y + rowH - body
        searchScroll = searchScroll.coerceIn(0, (searchResults.size * rowH - body).coerceAtLeast(0))
    }

    override fun removed() {
        GuideBookState.savePosition(
            currentSavedView().name.lowercase(),
            entry?.id,
            category?.id,
            spread
        )
        super.removed()
    }

    private fun prevSpread() {
        if (view != View.ENTRY) return
        if (spread > 0) {
            spread--
            scrollLeft = 0
            scrollRight = 0
        }
    }

    private fun nextSpread() {
        if (view != View.ENTRY) return
        val pages = entry?.pages?.size ?: 0
        val spreads = ((pages + 1) / 2).coerceAtLeast(1)
        if (spread + 1 < spreads) {
            spread++
            scrollLeft = 0
            scrollRight = 0
        }
    }

    private fun goHome() {
        view = View.HOME
        entry = null
        spread = 0
        scrollLeft = 0
        scrollRight = 0
        listScroll = 0
        descScroll = 0
    }

    private fun activate(target: String) {
        when {
            target == "nav:home" -> {
                if (view == View.ENTRY && category != null) {
                    view = View.CATEGORY
                    entry = null
                    spread = 0
                    scrollLeft = 0
                    scrollRight = 0
                    listScroll = 0
                    descScroll = 0
                } else {
                    goHome()
                }
            }
            target == "nav:prev" -> prevSpread()
            target == "nav:next" -> nextSpread()
            target == "nav:search" -> openSearch()
            target == "nav:bookmarks" -> openBookmarks()
            target == "nav:toggleBookmark" -> {
                val e = entry ?: return
                val page = (spread * 2).coerceAtMost(e.pages.lastIndex.coerceAtLeast(0))
                if (e.pages.isNotEmpty()) GuideBookState.toggleBookmark(e.id, page)
            }
            target.startsWith("search:") -> openSearchHit(target.removePrefix("search:"))
            target.startsWith("bookmark:") -> openSearchHit(target.removePrefix("bookmark:"))
            target.startsWith("unbookmark:") -> {
                val (entryId, page) = splitPageTarget(target.removePrefix("unbookmark:"))
                if (entryId != null) GuideBookState.removeBookmark(entryId, page)
            }
            target.startsWith("url:") -> {
                val uri = runCatching { URI.create(target.removePrefix("url:")) }.getOrNull()
                if (uri != null) clickUrlAction(minecraft!!, this, uri)
            }
            target.startsWith("category:") -> {
                val id = target.removePrefix("category:")
                val cat = data.category(id) ?: return
                category = cat
                view = View.CATEGORY
                listScroll = 0
                descScroll = 0
            }
            target.startsWith("entry:") -> {
                val path = target.removePrefix("entry:")
                val anchor = path.substringAfter('#', "")
                val entryPath = path.substringBefore('#')
                val e = data.entry(entryPath) ?: return
                openEntry(e, anchor.ifBlank { null }, resetView = true)
            }
        }
    }

    /** 解析 `entryId#page` 形式的命中目标并跳转。 */
    private fun openSearchHit(payload: String) {
        val (entryId, page) = splitPageTarget(payload)
        val e = entryId?.let { data.entry(it) } ?: return
        openEntry(e, null, resetView = true)
        if (page >= 0) spread = page / 2
    }

    private fun splitPageTarget(payload: String): Pair<String?, Int> {
        val entryId = payload.substringBefore('#', "").ifBlank { null }
        val page = payload.substringAfter('#', "-1").toIntOrNull() ?: -1
        return entryId to page
    }

    private fun openEntry(e: GuideEntry, anchor: String?, resetView: Boolean) {
        entry = e
        category = data.category(e.categoryId)
        if (resetView) view = View.ENTRY
        val page = if (anchor != null) e.pageIndexForAnchor(anchor) else -1
        spread = if (page >= 0) page / 2 else 0
        scrollLeft = 0
        scrollRight = 0
        listScroll = 0
        descScroll = 0
        if (resetView) view = View.ENTRY
    }

    companion object {
        const val BOOK_W = 271
        const val BOOK_H = 180
        const val PAGE_L_X = 13
        const val PAGE_R_X = 139
        const val PAGE_W = 118
        const val PAGE_H = 159
        const val PAGE_Y = 9

        private const val LIST_BODY_HEIGHT = 132

        private const val TITLE_COLOR = 0xFF3B2A5A.toInt()
        private const val BODY_COLOR = 0xFF2B2B2B.toInt()
        private const val LINK_COLOR = 0xFF6A3FB5.toInt()
        private const val MUTED_COLOR = 0xFF7A6A55.toInt()

        private const val FIELD_BG = 0xFFF2E8D0.toInt()
        private const val FIELD_BORDER = 0xFF8A7A5F.toInt()

        private const val KEY_ESCAPE = 256
        private const val KEY_ENTER = 257
        private const val KEY_BACKSPACE = 259
        private const val KEY_RIGHT = 262
        private const val KEY_LEFT = 263
        private const val KEY_DOWN = 264
        private const val KEY_UP = 265
        private const val KEY_B = 66
        private const val KEY_F = 70

        val BOOK_TEXTURE: Identifier =
            Identifier.fromNamespaceAndPath("hexcasting", "textures/gui/patchi_book.png")
    }
}
