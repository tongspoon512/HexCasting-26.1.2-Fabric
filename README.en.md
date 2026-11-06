# Hex Casting — Minecraft 26.1.2 / Fabric

[简体中文](./README.md) | **English**

A port of **Hex Casting** to **Minecraft 26.1.2 (Fabric)**. The original mod by petrak@ (gamma-delta) is a magic mod about drawing patterns in the air to cast spells, inspired by PSI.

This repository starts from upstream 0.11.4 and moves the 1.20.1-era implementation onto the 26.1 API stack: official names directly (no obfuscation, no remapping), Java 25, render state / submit pipeline for rendering, and payload packets for networking.

On top of that, this branch also ports [HexDebug](https://github.com/object-Object/HexDebug)'s **Splicing Table** for editing iota lists in game.

> **Personal port disclaimer**
>
> This is an **unofficial port** kept for personal study and archiving. It is not affiliated with FallingColors, petrak@ (gamma-delta), object-Object or YukkuriC, and does not represent their quality or opinions.
>
> - Changes here are **not submitted upstream**, and issues from this repository should not be reported to upstream trackers.
> - Refer to each upstream project's own repository and documentation as the source of truth; this repository only aims to "run on 26.1.2 Fabric".
> - For personal archiving and further porting only; this is not an official release. Please attach full logs when reporting problems.
> - Upstream GitHub Actions workflows (`build_docs.yml`, `pr.yml`) were removed from this repository; this branch does not use upstream CI.

## Status

| Item | Status |
| --- | --- |
| Minecraft | 26.1.2 (`fabric.loom.disableObfuscation=true`, source names = official names = runtime names) |
| Loader | Fabric Loader ≥ 0.20.1 |
| Java | 25 |
| Build | `:Fabric:build` / `:Fabric:jar` both pass and produce an installable jar |
| Runtime verification | Client has been launched in a real instance and entered a world; casting, inline patterns, the guide book and the splicing table have all been verified in game |
| Forge | **Not ported**, not included in `settings.gradle`, and not built |

Common and Fabric are compiled together: `Fabric/build.gradle` merges `Common`'s main sources into Fabric's `compileJava`, so `Common` is not a standalone artifact.

## Features

### 1. Splicing Table (ported from HexDebug)

Adds the `hexdebug:splicing_table` block and item (code under `gay.object.hexdebug`, resource namespace `hexdebug`) **without a new mod id**; it all loads from the main jar.

- **Block entity**: 4 slots — list, clipboard, media, staff; list/clipboard accept only items holding iota data, the staff slot accepts `hexcasting:staves`.
- **List view**: 9 iotas at a time; page buttons plus scroll wheel / arrow keys / PageUp / PageDown / Home / End.
- **Selection**: click an iota to select a cell, click between cells to select an "edge"; Shift extends, Ctrl+Shift moves the selection.
- **Editing**: move left/right, delete, backspace, copy, cut, paste (`Ctrl+V` normal paste, `Ctrl+Shift+V` raw paste), duplicate entry, undo/redo — all executed server-side and pushed back.
- **Mini staff grid**: with a staff in the staff slot, a 160×160 casting grid appears on the left. Patterns drawn there are written into the current selection, and the selection moves past the pattern. The grid only draws and plays sounds; it does not cast or consume media.
- **Misc**: a clear button under the grid (clears only the client-side drawing, not list data); ESC during drawing cancels the pattern instead of closing the screen; clicking the staff slot area does not start drawing.

Implementation:

- [SplicingTableBlock.kt](Fabric/src/main/java/gay/object/hexdebug/blocks/SplicingTableBlock.kt)
- [SplicingTableBlockEntity.kt](Fabric/src/main/java/gay/object/hexdebug/blocks/splicing/SplicingTableBlockEntity.kt)
- [SplicingTableMenu.kt](Fabric/src/main/java/gay/object/hexdebug/gui/splicing/SplicingTableMenu.kt) / [SplicingTableScreen.kt](Fabric/src/main/java/gay/object/hexdebug/gui/splicing/SplicingTableScreen.kt)
- [splicing data model](Fabric/src/main/java/gay/object/hexdebug/splicing/)
- [HexDebugMessages.kt](Fabric/src/main/java/gay/object/hexdebug/networking/HexDebugMessages.kt)

Recipe (matching upstream):

```
P C P        P = polished planks (hexcasting:edified_planks)
A F A        C = charged amethyst (hexcasting:charged_amethyst)
S G S        A = amethyst shard      F = focus (hexcasting:focus)
             S = slate block        G = gold ingot
```

Recipe, loot table and recipe-unlock advancement live under `data/hexdebug/recipe/`, `data/hexdebug/loot_table/`, `data/hexdebug/advancement/`.

### 2. Built-in guide book (no Patchouli)

Patchouli is not available for 26.1, so this branch reimplements the book as a native GUI that **reads the upstream book's JSON and language files directly**, instead of maintaining a second copy of the text.

- Entry item: `hexcasting:guide_book`, shapeless recipe "book + amethyst dust", also in the creative tab.
- Open with right-click. The 271×180 two-page book is taken from a 512×256 texture.
- Navigation: contents → category → entry → two-page spreads; scroll wheel for long pages, `<` `>` to flip, `Contents/Back` to go up.
- **Bookmarks and search**: the `☆` in an entry's footer bookmarks the current spread; the `Bookmarks` list jumps on click and removes with `×`. `Search` (or press `F`) searches entry names, entry IDs and all page text, with arrow-key selection, Enter to open, and IME composition support; `B` opens the bookmark list directly.
- **Last position memory**: on close the book stores the current entry/category and spread, and returns there next time. Bookmarks and position live in `config/hexcasting_guidebook.json`.
- Content: 9 categories, 82 entries, 528 pages, all from `assets/hexcasting/patchouli_books/thehexbook/en_us/`.
- Page types: `patchouli:text`, `link`, `image`, `spotlight`, `empty`, `crafting`, plus Hex Casting's `pattern`, `manual_pattern`, `manual_pattern_nosig`, `brainsweep`, `crafting_multi`.
- Text rendering: `$(br)`/`$(br2)`/`$(p)`/`$(li)`/`$(#rgb)`/`$(0-f)`/`$(o)`/`$(n)`/`$(l:...)`/`$(k:...)` and book.json macros; **wrapping follows the page width and long words are broken per character**, so nothing spills off the page.
- Recipe pages draw a 3×3 grid straight from the JSON inside the jar (26.1 has no complete client-side `RecipeManager`, so no runtime lookup is used).
- Pattern pages reuse `PatternDrawingUtil` with the same parameters as the official Patchouli component (cells, strokes, dots).

Implementation:

- [GuideBook.kt](Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideBook.kt): book data loading (categories, entries, macros, icon parsing) and the full-text search index
- [GuideBookState.kt](Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideBookState.kt): bookmark and last-position persistence
- [GuideBookScreen.kt](Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideBookScreen.kt): two-page GUI, navigation, per-page-type rendering
- [GuideMarkup.kt](Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideMarkup.kt): markup parsing + wrapping + link hit boxes
- [GuideRecipes.kt](Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideRecipes.kt): recipe JSON reading
- [GuideTextures.kt](Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideTextures.kt): UV scaling from the original PNG size
- [ItemGuideBook.java](Common/src/main/java/at/petrak/hexcasting/common/items/ItemGuideBook.java): entry item

### 3. Inline pattern rendering

There is no usable Inline build for 26.1, so this repository implements a minimal kernel that lets Hex Casting's pattern syntax render directly inside GUI text (chat, item tooltips, item names, etc.).

Supported forms (matching upstream Inline's pattern matcher):

```
{west aqw}          {east qqqqq}
[west aqw]          <west aqw>
{w aqw}             {north_east aqwqew}
```

- The whole matched run is folded into one pattern glyph, advancing by the pattern's real width; following text continues normally.
- Pattern color comes from the text color.
- The glyph is provided through `Font.getGlyphSource` (covering both rendering and width), so layout and wrapping do not shift.

Implementation:

- [InlinePatternFont.java](Common/src/main/java/at/petrak/hexcasting/interop/inline/InlinePatternFont.java)
- [InlineTextProcessor.java](Common/src/main/java/at/petrak/hexcasting/interop/inline/InlineTextProcessor.java)
- [InlinePatternTextRenderable.java](Common/src/main/java/at/petrak/hexcasting/interop/inline/InlinePatternTextRenderable.java)
- [MixinFontInline.java](Common/src/main/java/at/petrak/hexcasting/mixin/client/MixinFontInline.java), [MixinGuiTextRenderStateInline.java](Common/src/main/java/at/petrak/hexcasting/mixin/client/MixinGuiTextRenderStateInline.java)

### 4. Transparent casting GUI and grid spacing

- The casting GUI (`GuiSpellcasting`) clears `extractBackground` and draws only patterns and widgets, keeping the background transparent so it can overlay anything.
- A client option "Hex grid spacing" (`gridSpacing`) scales the distance between grid points from 0.5 to 2.0, **default 1.0**. Change it via ModMenu or `config/hexcasting/client.json`.

### 5. Personal media pool and procrastination (ported from HexOverpowered)

Only the "personal media bar" and "procrastination" parts were ported from [YukkuriC/HexOverpowered](https://github.com/YukkuriC/HexOverpowered); the mind stack, Factor Cut and other content were not.

**Personal media pool**

- The player gains 3 synced attributes: `hexdebug:personal_media`, `hexdebug:personal_media_max`, `hexdebug:personal_media_regen`.
- The personal pool is consumed **first** when casting (priority `Int.MAX_VALUE`), but does not participate in "battery construction".
- Every `personalMediaRegenInterval` ticks it regenerates `personalMediaRegenStep`; defaults are 20 ticks / 1 amethyst shard, capped at 10 charged amethyst (1,000,000 dust).
- Enabled by default only after the "Enlightenment" advancement; that restriction can be disabled in server config.
- A pink/purple media bar (with a slow breathing alpha) is drawn above the health bar; hidden when the pool is empty or disabled.

Related config (`config/hexcasting/server.json`): `personalMediaPool`, `personalMediaMax`, `personalMediaRegenStep`, `personalMediaRegenInterval`, `personalMediaAfterEnlightened`.

**Procrastination patterns**

| Pattern | Signature / start direction | Args | Description |
| --- | --- | --- | --- |
| `hexdebug:get_personal_media` | `qqaqqea` / east | 0 | Current personal media |
| `hexdebug:get_personal_media_max` | `qqaqqqd` / east | 0 | Personal media cap |
| `hexdebug:mind_env/schedule` | `waawedaqqqqdeaqq` / south_west | 2 (code, delay ticks) | Run code after a delay; delay ≤ 0 runs immediately |
| `hexdebug:mind_env/running_code` | `wqaqwweeeeeqdeaqq` / south_west | 0 | Pending code, or Null |

- Scheduling again from the same caster cancels the old task; delayed runs use a silenced casting environment to avoid per-tick chat spam.
- Upstream's same-tick recursion guards are kept (at most 10 "no-op" calls and 100000 normal calls per tick).
- New server options: `enablesMindEnvActions` (default on), `bypassesSilencedEnvRestrictions` (default off; when on, delayed spells use the original environment and print messages normally).

Implementation:

- [HexDebugAttributes.java](Common/src/main/java/gay/object/hexdebug/media/HexDebugAttributes.java) / [PersonalManaHolder.kt](Common/src/main/java/gay/object/hexdebug/media/PersonalManaHolder.kt) / [PersonalMediaRegen.kt](Common/src/main/java/gay/object/hexdebug/media/PersonalMediaRegen.kt)
- [OpGetPersonalMedia.kt](Common/src/main/java/gay/object/hexdebug/actions/OpGetPersonalMedia.kt) / [OpScheduleCall.kt](Common/src/main/java/gay/object/hexdebug/actions/OpScheduleCall.kt) / [OpScheduledCode.kt](Common/src/main/java/gay/object/hexdebug/actions/OpScheduledCode.kt)
- [SilencedCastingEnv.kt](Common/src/main/java/gay/object/hexdebug/ext/SilencedCastingEnv.kt)
- [PersonalMediaHud.kt](Fabric/src/main/java/gay/object/hexdebug/client/PersonalMediaHud.kt) (texture `assets/hexdebug/textures/gui/media_bar.png`)
- [HexDebugActions.kt](Common/src/main/java/gay/object/hexdebug/actions/HexDebugActions.kt): action registration

> Delayed tasks live in memory only; server restarts (`SERVER_STARTING`) clear the queue and pending tasks are not kept when a player logs out.

### 6. MoreIotas (ported from FallingColors/MoreIotas)

Upstream [MoreIotas](https://github.com/FallingColors/MoreIotas) by Talia (MIT) is an iota extension library; this branch merges all of its gameplay into one jar and keeps the `moreiotas:*` namespace for save compatibility.

- **6 new iota types**: `moreiotas:string`, `moreiotas:matrix`, `moreiotas:iota_type`, `moreiotas:entity_type`, `moreiotas:item_type`, `moreiotas:stack` (item stacks, serialized with 26.1 data components).
- **45 patterns**: 17 string patterns, 12 matrix patterns (including 4 element-wise operators), 6 type/entity patterns, 4 item patterns, plus 6 operator rewrites (`moreiotas:altadd` etc.).
- **Three arithmetic sets**: `moreiotas:string` (10), `moreiotas:matrix` (12, including matrix exponentiation), `moreiotas:type` (3), with upstream ids.
- **Chat prefixes**: `moreiotas:chat_prefix_holder` component + chat event handling; after Sifter's Gambit sets a prefix, prefixed messages are only visible to "Whisper's Insight".
- **Server config**: `maxMatrixSize` (144), `maxStringLength` (1728), `setBlockStringCost` / `nameCost` (0.01 dust each), shown in Hex Casting's Cloth config UI.
- **Guide book entries**: 5 entries for strings, matrices, types, items and item iotas are merged into the built-in book.

Code lives under `Common/src/main/java/ram/talia/moreiotas/`; Fabric-side components and events under `Fabric/src/main/java/ram/talia/moreiotas/fabric/`.

### 7. HexParse text spells (`/hexParse`)

There is no usable HexParseMod for 26.1, so this branch implements its own bidirectional "text ↔ iota" conversion, making it easy to save, share and write long spells back with a command.

- **Import**: `/hexParse "<code>" [rename]` parses text into iotas and writes them into a writable container in the main hand (focus, spellbook, etc.), optionally renaming the item.
- **Export**: `/hexParse read` reads the held container and prints clickable, copyable text; `/hexParse read_signatures` prints only `<start direction, angle string>` signatures.
- **Share**: `/hexParse share` broadcasts the held spell text to the whole server.
- **Clipboard**: `/hexParse clipboard [rename]` imports clipboard text, `clipboard_angles [rename]` imports only the quoted angle string; client/server round-trips use `MsgPullClipboardS2C` / `MsgPushClipboardC2S`.
- **Syntax notes**: the top level is itself a list; `(` `)` are Introspection/Retrospection and build lists at runtime; bare numbers are `DoubleIota` **literals** and must be written as `num_<n>` patterns to be executed by `eval`; `vec_x_y_z` and `mask_` have dedicated forms; patterns can be written by name, alias or direction signature; entity, string, matrix and type iotas are supported. `read` output can be fed back into `/hexParse`, preserving nesting, pattern directions and masks.

Implementation:

- [HexParseCommand.java](Common/src/main/java/at/petrak/hexcasting/common/lib/hexparse/HexParseCommand.java): command tree, read/write targets and chat output
- [IotaCode.kt](Common/src/main/java/at/petrak/hexcasting/common/lib/hexparse/IotaCode.kt): tokenizing, parsing, reverse output, number/mask pattern angle algorithms
- [MsgPullClipboardS2C.java](Common/src/main/java/at/petrak/hexcasting/common/msgs/MsgPullClipboardS2C.java) / [MsgPushClipboardC2S.java](Common/src/main/java/at/petrak/hexcasting/common/msgs/MsgPushClipboardC2S.java): clipboard round-trips

Example spell docs: [hexparse/连锁挖掘.md](hexparse/连锁挖掘.md) covers 3×3 planes, 3×3×3 cubes, straight drilling, and "same-ore + self-reference (quine)" chain mining.

### 8. Scrying Lens Assembly (`hexcasting:lens_assembly`)

- Adds `hexcasting:lens_assembly`; right-click opens the grid-spacing screen (`GridSpacingScreen`).
- The center previews the current grid density live; the slider ranges 0.5–2.0. Closing the screen writes the result back to the client option `gridSpacing`, equivalent to ModMenu and `config/hexcasting/client.json`.
- The spacing applies to both the casting GUI and the splicing table's mini staff grid.
- Includes a custom texture, recipe (gold ingot + charged amethyst + scrying lens) and a guide book entry.

Implementation:

- [ItemScryingLensAssembly.java](Common/src/main/java/at/petrak/hexcasting/common/items/ItemScryingLensAssembly.java)
- [GridSpacingScreen.kt](Common/src/main/java/at/petrak/hexcasting/client/gui/GridSpacingScreen.kt)
- `gridSpacing()` in [HexConfig.java](Common/src/main/java/at/petrak/hexcasting/api/mod/HexConfig.java)

### 9. Adaptive wrapping in item iota tooltips

List iotas in tooltips (focus, spellbook, thought knot, abacus, etc.) no longer squeeze into one long line:

- Each iota's rendered pixel width (including commas) is measured with `font.width`; a new line starts only when the budget is exceeded, and pattern iotas count with their real glyph width.
- The budget is the GUI-scaled width minus 32 pixels, minus the first-line `Contains:` prefix; continuation lines are indented by four spaces.
- Narrowing the window or increasing GUI scale re-flows on the next hover. Single-line `display()` (inside the spellbook GUI, etc.) is unchanged.

Implementation: `displayLines` in [IotaType.java](Common/src/main/java/at/petrak/hexcasting/api/casting/iota/IotaType.java), [ListIota.java](Common/src/main/java/at/petrak/hexcasting/api/casting/iota/ListIota.java), [IotaHolderItem.java](Common/src/main/java/at/petrak/hexcasting/api/item/IotaHolderItem.java).

### 10. Keyboard casting, pattern completion and aliases (ported from Hexcessible)

Following the pure-client accessibility layer of [tizu69/Hexcessible](https://github.com/tizu69/Hexcessible), the casting GUI gains a keyboard workflow that does not change mouse drawing:

- **Idle hover hint**: hovering a drawn pattern for 0.5s shows `<start direction, angle string>` and the pattern's localized name; Number Reflection and Bookkeeper's Strategy show computed values (e.g. "Number Reflection: 4.00", "Bookkeeper's Strategy: -v--v").
- **Right-click pattern list**: while idle, right-clicking the drawing area opens a scrollable list of every registered pattern; type to search, `↑/↓` to select, `Enter`/`Tab` to cast. Only unlocked patterns are listed, with a count of locked ones.
- **Keyboard drawing**: while idle, `q/w/e/a/d` start entering relative angles; `Backspace`/`s` undo; `Enter`/`Tab`/space cast; `H/J/K/L` and arrow keys move the point; `R`/`Shift+R` or the scroll wheel rotates. While drawing, a gray ghost pattern and blue actual point are shown, each extendable direction is labeled with its next key, and red text appears when space runs out.
- **Live preview while drawing**: while dragging, the GUI shows what would be drawn if released now — the current angle string and pattern name (with computed values for Number Reflection / Bookkeeper's Strategy); the preview rolls back when undoing.
- **Fuzzy completion** (`Ctrl+Space`): fuzzy match by name, alias and action id; `↑/↓` or scroll wheel to select, `Enter`/`Tab` to enter keyboard drawing, `Esc` to exit; an empty query auto-dismisses when the mouse leaves a 1.75-block radius.
- **JustEnoughCharacters compatibility**: with JEC installed, completion and the right-click list support pinyin search for Chinese pattern names (e.g. `shuzi`, `zhisi` match "数字之精思"); the shim reflectively calls JEC's `HexcessibleHooks.mergeScore` and falls back to the original fuzzy matcher when JEC is absent.
- **Guide book docs**: the completion list shows the selected pattern's input/output and guide book text, `←/→` cycles through multiple docs for the same pattern; locked patterns are filtered out and replaced by "N more patterns are locked".
- **Aliases** (`Ctrl+E`): rename a hovered pattern or a completion entry; `Enter`/`Tab` saves, empty clears, `Esc` cancels. Aliases are stored in `config/hexcasting_aliases.json` and used immediately by hover hints and fuzzy search.
- **Client options**: `config/hexcasting/client.json` gains "pattern completion count" (`patternAutocompleteCount`, default 5, range 3–20) and "show guide book docs in completion" (`showPatternDocsInAutocomplete`, default on), also editable in ModMenu.

Implementation: [GuiSpellcasting.kt](Common/src/main/java/at/petrak/hexcasting/client/gui/GuiSpellcasting.kt) (state machine and drawing), [PatternKeyboard.kt](Common/src/main/java/at/petrak/hexcasting/client/gui/PatternKeyboard.kt) (keys/signatures/fuzzy search), [PatternDocs.kt](Common/src/main/java/at/petrak/hexcasting/client/gui/PatternDocs.kt) (guide docs and unlock filtering), [PatternAliases.kt](Common/src/main/java/at/petrak/hexcasting/client/gui/PatternAliases.kt) (alias persistence).

Text input uses 26.1's `charTyped` and `preeditUpdated`: IMEs can type Chinese directly, the hint box shows "Composing: …" while composing, and non-US keyboard layouts deliver actual characters. Pinyin search requires JustEnoughCharacters.

## Installation

1. Install Fabric Loader ≥ 0.20.1 (for 26.1.2).
2. Put the build artifact into the instance's `mods/`:

   ```
   hexcasting-fabric-26.1.2-0.11.4.jar
   ```

3. Also install these hard dependencies (missing ones cause hard dependency errors):

   | Mod | Minimum | Notes |
   | --- | --- | --- |
   | Fabric API | ≥ 0.155.3+26.1.2 | Since 26.1 the mod id is `fabric-api`; the old `fabric` alias is gone |
   | Fabric Language Kotlin | ≥ 1.14.2+kotlin.2.4.21 | Required at runtime |
   | Cardinal Components API | ≥ 8.0.1 | Needs base / entity / item / block submodules |
   | Cloth Config | ≥ 26.1.154 | Config UI |

   Optional: ModMenu (for the config entry point).

> Patchouli, Inline, Trinkets, Paucal and other dependencies are vendored as stubs in source form — **do not** install real mods with the same names, or you will get duplicate-class conflicts. The guide book is implemented by this mod's own GUI and does not need Patchouli. The splicing table is merged into this jar and does not need HexDebug.

## Building

Requires JDK 25 (`release = 25`, `jvmTarget = 25` in `build.gradle`).

```bash
./gradlew :Fabric:build     # full build, including sources / javadoc jars
./gradlew :Fabric:jar       # installable jar only, faster
./gradlew :Fabric:clean     # clean build outputs
```

Outputs:

```
Fabric/build/libs/hexcasting-fabric-26.1.2-0.11.4.jar
Fabric/build/libs/hexcasting-fabric-26.1.2-0.11.4-sources.jar
Fabric/build/libs/hexcasting-fabric-26.1.2-0.11.4-javadoc.jar
```

Development runs (need a graphical environment):

```bash
./gradlew :Fabric:runClient
./gradlew :Fabric:runServer
./gradlew :Fabric:runDatagen   # writes to Fabric/src/generated/resources
```

## Project layout

```
Common/   Platform-neutral code: API, casting VM, blocks/items/entities, datagen, mixins, client GUI
          └─ gay/object/hexdebug/  HexDebug port: media (personal media), actions (patterns), ext (silenced env)
Fabric/   Platform implementation: entrypoint, networking, CCA components, events, interop, datagen wiring
          └─ gay/object/hexdebug/  Splicing table (block, menu, GUI, networking, data model) and HUD
Forge/    Upstream 1.20.1 leftovers, not in the build; do not modify
doc/      Original docs source
hexparse/ HexParse text spell docs (chain mining, etc.)
```

## 26.1 porting notes

The main gaps that were bridged, for future maintenance:

- **Networking**: `registerGlobalReceiver(Identifier, …)` was removed; all messages go through `CustomPacketPayload` + `PayloadTypeRegistry.serverboundPlay()/clientboundPlay()`, registered by `FabricPacketHandler`.
- **Data pack folders**: since 26.1 the folders are singular (`recipe/`, `loot_table/`, `advancement/`); old plural folders are not loaded.
- **Attributes**: all queries/injections use `Holder<Attribute>`; `HexAttributes.holder(...)` bridges a raw `Attribute` back into the registry.
- **Equipment**: `Item.Properties.equipmentSlot(...)` and the old `ArmorMaterial` system were removed; use `equippable(EquipmentSlot)` and `net.minecraft.world.item.equipment.*`.
- **Loot**: `LootTableReference` → `NestedLootTable.lootTableReference`; `LootTable.functions` is a `List`; server resource reload uses `ReloadableServerResources.fullRegistries()`.
- **Rendering**: `BakedModel`, `BlockRenderDispatcher`, `ItemColor`, `ItemProperties`, `LightTexture`, `PlayerRenderer` etc. were removed or renamed; use the render state + `SubmitNodeCollector` pipeline (`AvatarRenderer`, `AltioraLayer` etc. were rewritten). GUI elements must submit 4-vertex triangles with normalized winding, or the fixed QUADS index buffer and back-face culling will swallow them.
- **GUI**: `Screen` moved from `render` to `extractRenderState(GuiGraphicsExtractor, …)`; `GuiGraphicsExtractor` provides `fill`, `text`, `centeredText`, `blit`, `item`, `pose()`.
- **Input**: mouse callbacks use `MouseButtonEvent` / `KeyEvent`, and `GuiSpellcasting` can be embedded in other screens (the splicing table's mini grid reuses this).
- **CCA v8**: item-component attachment was removed; item attachments are built directly as adapters; `CardinalComponent` reads/writes via `ValueInput` / `ValueOutput`.
- **Datagen**: recipe/loot builders are codec-based, `RecipeBuilder` no longer has `getResult()`, and `RecipeProvider.buildRecipes()` visibility changed.

## Known limitations

- **Splicing table**: ported only to P2 — list editing and the mini staff grid work, but **you cannot cast directly at the table** (P3 is not implemented), so the media slot currently does nothing and consumes no media. Enlightened splicing table, debug wand, Focus Holder and the debugger (DAP) are not ported.
- **Procrastination**: delayed tasks are in-memory only and are cleared on server restart/reset; pending tasks are not kept while a player is offline.
- **Guide book**: no read progress or advancement locking; `spotlight` pages do not show crafting sources; if a recipe is missing in the 26.1 port (e.g. `amethyst_dust_packing`), that page shows text without a grid.
- **Patchouli**: no 26.1 version; the API is vendored as source stubs. Book content is read by the built-in GUI and no Patchouli runtime is needed.
- **Inline**: only pattern inlining is implemented; other Inline features (item/entity/avatar matching, size modifiers, overlay rendering) are missing, and world-space text (signs, name tags, etc.) shows placeholders.
- **Trinkets**: not ported; `dev/emi/trinkets` are stubs and the trinket renderer is an empty implementation (functionality works, visuals do not show).
- **Pehkui**: the available version links against old intermediary names and cannot run on 26.1; `getPehkuiApi()` throws `UnsupportedOperationException`. **Do not install Pehkui.**
- **EMI / JEI**: compile-time integration and plugin entry points are kept; EMI code is also based on vendored stubs.
- **Rendering fidelity**: after the 26.1 render pipeline rewrite, some effects (brush, light, trinket/elytra overlays, etc.) are approximations and may differ visually from the original.
- **MoreIotas**: only Common/Fabric gameplay was ported; upstream's Forge side, Paucal/Inline/Fiber soft dependencies, JBLAS and Mixin/AW were not. `moreiotas:stack` now uses 26.1 data component serialization, and book-and-quill/written-book page iota holders are restored as `CCItemIotaHolder.Static`.
- **Verification**: builds and basic in-game flows pass; no full gameplay regression has been done. Attach logs for startup issues.

## Documentation

- Original online manual: https://hexcasting.hexxy.media/
- HexDebug online manual: https://hexdebug.hexxy.media/
- HexParse spell examples: [hexparse/连锁挖掘.md](hexparse/连锁挖掘.md)
- Changelog: see [CHANGELOG.md](./CHANGELOG.md)
- Contributing: see [CONTRIBUTING.md](./CONTRIBUTING.md)

## Credits and license

- Original project: [FallingColors/HexMod](https://github.com/FallingColors/HexMod) by petrak@ (gamma-delta)
- Splicing table source: [object-Object/HexDebug](https://github.com/object-Object/HexDebug)
- Personal media pool and procrastination source: [YukkuriC/HexOverpowered](https://github.com/YukkuriC/HexOverpowered) (MIT)
- String/matrix/type/item iotas and related patterns: [FallingColors/MoreIotas](https://github.com/FallingColors/MoreIotas) by Talia (MIT)
- This port follows the original project's license: [MIT](./LICENSE.txt)
