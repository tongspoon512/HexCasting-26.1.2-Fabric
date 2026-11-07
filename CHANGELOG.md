# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## `0.11.4-fabric-build_18` - 2026-11-07

### Added

- 新增图案「高阶拾取」（`hexcasting:advanced_pickup`）：输入一个掉落物实体与一个容器坐标，按原版漏斗的规则（`HopperBlockEntity.addItem`）把物品送入该容器，放不下的部分留在掉落物实体上。
- 指南书新增「高阶拾取」与「获得经验」两篇条目，各自附带图案页。

### Fixed

- 修复哨卫无法渲染：26.1 的线段管线依赖顶点属性 `LineWidth` 在屏幕空间展开四边形，原代码未设置该属性（默认为 0，四边形面积为零），现补上 `setLineWidth(5f)`，与上游线宽一致。

## `0.11.4-fabric-build_17` - 2026-11-07

### Added

- 新增图案「获得经验」（`hexcasting:gain_experience`）：输入一个经验球实体，把该球的经验值直接授予施法者并移除经验球，效果等同于玩家拾取该经验球。
- 新增 `hexparse/` 文档：卓越哨卫 8×1×8 矿石挖掘的磁带 quine 咒术，以及哨卫同层的施法范围向量表。

### Fixed

- 挖掘咒术的拾取不再使用 `get_entity/item`（每格只返回最近一个，且查询框以哨卫脚底为中心、与方块中心差 0.5 格时会因 AABB 边界严格相交而漏查），改为 `zone_entity/item`（半径 2）取出该处全部掉落物并递归遍历逐个 `pickup_item`。

## `0.11.4-fabric-build_16` - 2026-11-06

### Added

- 移植 [hexcassettes](https://github.com/miyucomics/hexcassettes)：新增 8 个图案（排队、取消、清空、槽位数、忙碌列表、查询、剩余延迟、当前标签），把咒术延迟施放到玩家身上的「磁带」槽位，死亡后清空队列但保留槽位。
- 新增磁带物品、4 种音效、合成配方与创造模式物品栏条目；`ServerConfig.maxCassettes` 控制槽位上限（默认 6）。
- 磁带状态通过 Cardinal Components 玩家组件持久化，服务端每 tick 递减延迟并在槽位变化时同步；客户端登录时请求同步，新增 `G` 键打开的磁带总览界面，点击忙碌磁带可将其移除。
- 指南书新增「异步施法」与「守护进程」条目，并新增 `tape_worm`、`full_arsenal`、`quinio` 三项进度。

### Fixed

- `hexcassettes:cassettes` 玩家组件未在 `fabric.mod.json` 的 `custom.cardinal-components` 中声明，导致客户端启动时 CCA 抛错崩溃；已补声明。
- 磁带配方引用了 26.1 已移除的 `minecraft:music_discs` 标签，导致配方整条被丢弃；改为 `hexcassettes:music_discs`（全部 21 张原版唱片）。
- 原版「造物」（`hexcasting:artifact`）配方此前退回 `minecraft:creeper_drop_music_discs`，只接受苦力怕掉落唱片；现与磁带配方共用 `hexcassettes:music_discs`，造物等新唱片也可参与合成。
- 26.1 移除了旧版 `ItemProperties` 与模型 `overrides`，导致核心（存有 iota 时的填充贴图）、结念绳子（写入后的贴图）、石板、造物/饰品/护符/远古护符（变体与已存图案贴图）的状态贴图全部失效。现改用 26.1 的 `minecraft:condition` + `minecraft:component` 物品模型定义，按 `custom_data` 的 `data` / `sealed` / `variant` / `patterns` 与 `minecraft:block_entity_data` 选择对应模型。
- 剪接台的迷你法杖网格中，图案搜索 / 别名 / 按键提示弹窗原先被 160×160 网格的裁剪区域截断，长建议列表与图案文档无法完整显示；现改为在表格层之上、网格裁剪之外绘制，并按整个表格区域限制弹窗位置。

## `0.11.4-fabric-build_15` - 2026-11-06

### Added

- 指南书加入书签：条目页脚部的 `☆` 收藏当前跨页，`书签` 列表支持点击跳转与 `×` 删除；`搜索`（或按 `F`）可对条目名、条目 ID 与所有页面正文做全文搜索，支持方向键选择、回车打开与输入法组字；按 `B` 直接打开书签列表。
- 指南书记住上次退出时的位置：再次打开时自动回到原来的条目/分类与跨页；书签与位置保存在 `config/hexcasting_guidebook.json`。
- 新增英文版说明 [README.en.md](README.en.md)，并在 README 顶部加入中英文互链。

### Fixed

- 剪接台内的迷你法杖网格现在可以正常使用图案搜索：`Ctrl+Space`、字符输入、输入法组字与滚轮都会转发给网格，`Esc` 只关闭搜索浮层而不会关掉剪接台。
- 个人媒质池的五处问题：持有人不再强引用玩家（修复按玩家内存泄漏）；`canConstructBattery()` 恢复为 `true`；HUD 改用与媒质池一致的读数并尊重服务端开关；启蒙进度查询改为缓存，不再每 tick 重复获取；玩家断开时清理按 UUID 的会话缓存与 WeakHashMap 条目。

## `0.11.4-fabric-build_14` - 2026-11-01

### Added

- 图案补全与右键图案列表兼容 JustEnoughCharacters：安装 JEC 后可直接用拼音搜索中文图案名（如 `shuzi`、`zhisi` 匹配「数字之精思」），未安装时自动退回原有模糊匹配。
- 搜索框与别名输入改用 26.1 的 `charTyped` / `preeditUpdated`：支持输入法直接输入中文，组字过程中在提示框显示「输入中：…」，非美式键盘布局也按实际字符接收。

## `0.11.4-fabric-build_13` - 2026-11-01

### Added

- 鼠标绘制图案时实时预览「现在松手会画出什么」：在落点旁显示当前角度串与图案名称，数字之精思/簿记员之策略显示计算出的具体值；反向撤销时预览同步回退。

### Changed

- `hexparse/连锁挖掘.md` 的同种矿物连锁挖掘加入自动拾取：每破坏一格后用 `zone_entity/item` + `for_each` + `pickup_item` 收走掉落物（种子方块也拾取），并补充了栈变化、开关方式与 op 预算说明。

### Fixed

- 闪现/传送落点偏移：`teleportRespectSticky` 把绝对坐标连同 `Relative.X/Y/Z` 一起传给 `teleportTo`，而 26.1 的相对传送会再加上实体当前坐标，导致落点变成 `2 × 当前坐标 + 位移`（离原点越远偏得越多）。现改为传位移本身、旋转增量传 0，并用 `Relative.DELTA_*` 保留原速度，同时修掉朝向被叠加两遍的问题。

## `0.11.4-fabric-build_12` - 2026-11-01

### Added

- 空闲时在绘图区右键可打开全部已注册图案的浏览列表（窗口化滚动，直接输入即可搜索），与 `Ctrl+Space` 补全共用选择与施放逻辑。

### Fixed

- 数字之精思与簿记员之策略的图案名称改为取自各自的特殊处理器，悬停与键盘绘制提示现在会显示具体值，如「数字之精思：4.00」「簿记员之策略：-v--v」，不再显示未翻译的动作键。
- 修复键盘绘制时（尝试撤回图案）客户端崩溃：特殊处理器匹配不再要求非空的施法环境，第三方处理器抛出的异常也不会再让界面崩溃。

## `0.11.4-fabric-build_11` - 2026-11-01

### Added

- 施法界面新增 Hexcessible 风格的键盘操作：空闲悬停图案 0.5 秒显示签名与名称；`q/w/e/a/d` 输入相对角度绘制、`Backspace`/`s` 撤销、`Enter`/`Tab`/空格 施放、`H/J/K/L` 与方向键移动、`R` 或滚轮旋转，带幽灵预览与下一按键提示。
- 新增 `Ctrl+Space` 模糊图案补全：按名称、别名与动作 id 匹配，`↑/↓` 或滚轮选择，`Enter`/`Tab` 直接进入键盘绘制；选中项显示输入/输出与指南书说明，`←/→` 切换同一图案的多条说明；未解锁图案被过滤并显示「另有 N 个图案尚未解锁」。
- 新增 `Ctrl+E` 图案别名：悬停图案或补全列表上改名，保存在 `config/hexcasting_aliases.json`，并立即用于提示与搜索。
- 新增客户端配置「图案补全条数」（`patternAutocompleteCount`，默认 5）与「图案补全显示指南书说明」（`showPatternDocsInAutocomplete`，默认开）。

## `0.11.4-fabric-build_10` - 2026-11-01

### Added

- 物品（聚焦器、法术书、思想结、算盘等）的 iota 列表提示现在**按屏幕宽度自适应换行**：按实际渲染像素宽度切行，图案 iota 同样参与宽度计算，超长法术不会再挤成一条难以阅读的长行。

### Fixed

- 修正 `hexparse/连锁挖掘.md` 中「同种矿物连锁挖掘」法术的裸数字：hexparse 的裸数字是 `DoubleIota` 字面量，作为指令执行会触发 `MishapUnescapedValue`（「本应运行一个图案，而实际运行了 1.00」）。驱动与递归体中的 `0` / `1` / `2` 已改为 `num_0` / `num_1` / `num_2`，并在文档中补充了该规则说明。

## `0.11.4-fabric-build_9` - 2026-11-01

### Added

- 新增 HexParse 文本导入/导出：`/hexParse <code> [rename]`、`/hexParse read`、`/hexParse read_signatures`、`/hexParse share`、`/hexParse clipboard [rename]`、`/hexParse clipboard_angles [rename]`。支持列表、图案名、数字与图案互转、`mask_`/`num_`、向量、实体、字符串、矩阵、类型 iota，以及 HexParseMod 风格的数字/掩码图案语法。
- 新增剪贴板导入用的 `MsgPullClipboardS2C` / `MsgPushClipboardC2S` 往返消息。
- 新增 `hexparse/连锁挖掘.md`：基于现有图案的 3×3 平面、3×3×3 立方与直线钻探三套连锁挖掘法术。

## `0.11.4-fabric-build_8` - 2026-11-01

### Added

- 新增“探知透镜组”物品：右键打开咒术网格间距调节界面，可拖动滑块调整 0.5~2.0 的点间距，实时预览网格密度，关闭时写回客户端配置；附带自制贴图、配方（金锭 / 充能紫水晶 / 探知透镜）与指南书条目。
- 新增界面文本的中英文案。

### Fixed

- 旧版 `data/hexcasting/loot_tables/`（复数目录）中的 `random_scroll`、`random_cypher`、`grant_patchi_book` 已移动到 26.1 使用的 `loot_table/`（单数目录），战利品注入表可正常加载。

## `0.11.4-fabric-build_7` - 2026-10-31

### Fixed

- 修复石板等方块挖掘后不掉落物品：26.1 的方块战利品表需要放在 `data/<ns>/loot_table/blocks/` 下，数据生成器与已生成文件均已同步修正。
- 修复剪接台无法选中两个 iota 之间边界位置的问题：边界按钮现在优先于 iota 按钮响应点击，可正常选中并编辑插入点。

## `0.11.4-fabric-build_6` - 2026-10-31

### Added

- 指南书新增“拾取”条目：说明该图案接受掉落物实体 iota，并与原版拾取行为一致；同时包含图案演示页。

## `0.11.4-fabric-build_5` - 2026-10-31

### Added

- 新增“拾取”图案：接收掉落物实体 iota，让施法者像原版拾取一样把该物品并入背包（含拾取音效与统计）。图案为 7 笔封闭图形，关于中心点 180° 对称。

## `0.11.4-fabric-build_4` - 2026-10-31

### Fixed

- 剪接台嵌入绘咒网格现在会读取客户端 `gridSpacing` 配置：以 20px 为基准按倍率缩放，配置文件中的网格密度修改可正常生效。
- 修复语言键被错误加点的问题（如 `hexcasting.action.hexcasting:.get_caster`），所有图案名称、进度名称与指南书文案恢复正常本地化显示。

## `0.11.4-fabric-build_3` - 2026-10-31

### Added

- 移植 [MoreIotas](https://github.com/FallingColors/MoreIotas)（作者 Talia，MIT）的全部 Common/Fabric 玩法：6 种 iota（字符串、矩阵、iota 类型、实体类型、物品类型、物品组）、45 个图案、三套算术、聊天前缀与 5 个指南书条目。
- 服务端配置新增 `maxMatrixSize`、`maxStringLength`、`setBlockStringCost`、`nameCost`。
- 书与笔/成书可通过 `CCItemIotaHolder.Static` 作为字符串列表 iota 读取。
- 补齐指南书条目：剪接台、个人媒质（含法力/奥术之精思）、拖延图案（含拖延之精思），并提供中英文案。

### Changed

- 物品组 iota 改用 26.1 数据组件序列化，移除对 NBT 与 `Component.Serializer` 的依赖。
- 剪接台迷你绘咒网格改用固定 20px 间距，引导点密度不再随窗口尺寸放大而失控。

## `0.11.5` - [UNRELEASED]

### Added

- Added the `requires_enlightenment_in_parens` tag for patterns that should mishap without enlightenment even while parenthesized ([#1290](https://github.com/FallingColors/HexMod/pull/1290)) @Robotgiggle

### Changed

- Updated the Flay Mind recipe display in EMI and JEI to include the media cost, matching the guidebook ([#1323](https://github.com/FallingColors/HexMod/pull/1323)) @MEEPofFaith
- Updated Patchouli dependency from version 83 to version 85 ([#1319](https://github.com/FallingColors/HexMod/pull/1319)) @Robotgiggle
- Shift-scrolling and the spellbook keybinds now work on any item with the `page_idx` and `page_names` tags ([#1329](https://github.com/FallingColors/HexMod/pull/1329)) @Robotgiggle

### Fixed

- Fixed Great Spells causing a mishap when drawn without enlightenment even while parenthesized ([#1290](https://github.com/FallingColors/HexMod/pull/1290)) @Robotgiggle
- Fixed the bolt from Summon Lightning not being selectable as an entity and not being attributed to the caster ([#1304](https://github.com/FallingColors/HexMod/pull/1304)) @Robotgiggle
- Fixed spells with a variable cost becoming free when cast with an extremely small input value ([#1308](https://github.com/FallingColors/HexMod/pull/1308)) @Robotgiggle
- Fixed Blink and Greater Teleport cancelling all velocity when cast on a player ([#1310](https://github.com/FallingColors/HexMod/pull/1310)) @drtheodor
- Fixed various incorrect version specifications for the Patchouli dependency ([#1319](https://github.com/FallingColors/HexMod/pull/1319)) @Robotgiggle
- Fixed other players not seeing Altiora wings and pigment ([#1331](https://github.com/FallingColors/HexMod/pull/1331)) @R0b0Li0n
- Fixed Altiora wings, pigment and sentinel desyncing after changing dimension ([#1331](https://github.com/FallingColors/HexMod/pull/1331)) @R0b0Li0n
- Fixed an error when trying to cast a Great Spell that no longer exists, such as when an addon has been removed ([#1335](https://github.com/FallingColors/HexMod/pull/1335)) @Robotgiggle
- Fixed a broken translation key when referring to a jump iota in a mishap message ([#1350](https://github.com/FallingColors/HexMod/pull/1350)) @hauveli

## `0.11.4` - 2026-08-26

### Added

- Added the `cannot_modify_cost` tag for patterns that should ignore the `media_consumption` attribute when calculating cost ([#987](https://github.com/FallingColors/HexMod/pull/987)) @Robotgiggle
- Added config options to scale pattern costs per action and globally ([#1041](https://github.com/FallingColors/HexMod/pull/1041)) @Robotgiggle
- Added a config toggle for the pattern wobble animation on active slates to improve performance ([#1017](https://github.com/FallingColors/HexMod/pull/1017)) @aloaloolaola
- Added keybinds to flip forward and backwards in a spellbook ([#1028](https://github.com/FallingColors/HexMod/pull/1028)) @pythonmcpi
- Added a config toggle to disable scrolling through spellbooks outside of the staff grid ([#1028](https://github.com/FallingColors/HexMod/pull/1028)) @pythonmcpi
- Added Harmony Reflection, which pushes the value of the golden ratio ([#900](https://github.com/FallingColors/HexMod/pull/900)) @miyucomics
- Added a config toggle (default false) for the effect that makes iota-holding items display their entire NBT data when Advanced Tooltips is enabled ([#1021](https://github.com/FallingColors/HexMod/pull/1021)) @Robotgiggle
- Added a pattern display overlay for pattern-holding items (ie scrolls or slates) while holding shift in the inventory ([#879](https://github.com/FallingColors/HexMod/pull/879)) @SamsTheNerd
- Added connected textures for Akashic Ligatures when using Continuity or Optifine ([#885](https://github.com/FallingColors/HexMod/pull/885)) @kineticneticat
- Added type comparison patterns ([#1114](https://github.com/FallingColors/HexMod/pull/1114)) @IridescentVoid
  - Similarity Distillation and Dissimilarity Distillation for iota types
  - Surveyor's Distillation and Inspector's Distillation for blocks
  - Taxonomer's Distillation for entities
  - Sorter's Distillation and Stacking Distillation for items
- Added Contemplation for escaping without the exponential growth of Consideration ([#1102](https://github.com/FallingColors/HexMod/pull/1102)) @IridescentVoid
- Added Interjection, Meditation, and Recollection for advanced pattern-list creation ([#1103](https://github.com/FallingColors/HexMod/pull/1103)) @Robotgiggle
- Added Media Reflection for checking media available to the cast ([#1235](https://github.com/FallingColors/HexMod/pull/1235)) @IridescentVoid
- Added ko_kr translations ([#1187](https://github.com/FallingColors/HexMod/pull/1187)) @Bananamilk452
- Added a variety of new translations via Weblate ([#1251](https://github.com/FallingColors/HexMod/pull/1251), [#1261](https://github.com/FallingColors/HexMod/pull/1261), [#1265](https://github.com/FallingColors/HexMod/pull/1265), [#1269](https://github.com/FallingColors/HexMod/pull/1269), [#1272](https://github.com/FallingColors/HexMod/pull/1272), [#1274](https://github.com/FallingColors/HexMod/pull/1274))
  - de_de by @Olfi01 and @miyucomics
  - en_pt by @frostedlyre and @penguinencounter
  - es_es by @Chichi and @ElNico56
  - fi_fi by @hauveli
  - fr_fr by @Spectramaris, @Sixela963, @arsvenutia, @drawnbadden
  - ja_jp by @ViktoriaNtro
  - no_no by @ShyRasan123
  - pl_pl by @ManicTv and @Welesen
  - sv_se by @hauveli

### Changed

- Changed the `media_consumption` attribute to only apply to player-based casting ([#987](https://github.com/FallingColors/HexMod/pull/987)) @Robotgiggle
- Changed Wayfarer's Flight and Anchorite's Flight to both cost 2 dust per unit, and enforced a minimum cost for Anchorite's Flight ([#1040](https://github.com/FallingColors/HexMod/pull/1040)) @Robotgiggle
- Changed the Hasty Retrospection mishap to Absent Introspection as it's now used for anything that only works while parenthesized ([#1047](https://github.com/FallingColors/HexMod/pull/1047)) @Robotgiggle
- Updated and reorganized various entries in the guidebook to improve clarity ([#1199](https://github.com/FallingColors/HexMod/pull/1199)) @Robotgiggle
  - Added a new entry defining what a Hex actually is and how it can be represented on the stack
  - Changed "A Primer on Vectors" to provide its own information rather than linking to Psi's guide
  - Reordered "Escaping Patterns" to account for the newly added Contemplation pattern
  - Removed "Influences" and moved relevant info into "Hex Casting 101"
- Updated the Flay Mind recipe display in EMI and JEI to cycle through all valid entities if the input is an entity tag ([#1023](https://github.com/FallingColors/HexMod/pull/1023)) @YukkuriC
- All custom recipe displays now work with both EMI and JEI on all platforms, rather than only EMI on Fabric and only JEI on Forge ([#1206](https://github.com/FallingColors/HexMod/pull/1206)) @Robotgiggle
- The pattern limit is now checked even when executing non-pattern iotas like jumps ([#1035](https://github.com/FallingColors/HexMod/pull/1035)) @pythonmcpi
- Re-implemented the ability to extract stored media from items in trinket/curio slots ([#996](https://github.com/FallingColors/HexMod/pull/996)) @YukkuriC
- Patterns involving entity look direction now compensate for the vanilla bug that causes projectiles and phantoms to report the wrong direction ([#1025](https://github.com/FallingColors/HexMod/pull/1025)) @Robotgiggle
- Edified Panels are now crafted with 9 planks rather than 8 to avoid conflicting with the vanilla chest recipe ([#1080](https://github.com/FallingColors/HexMod/pull/1080)) @Robotgiggle
- Drawing Evanition with nothing left to do will now undo the opening Introspection ([#1047](https://github.com/FallingColors/HexMod/pull/1047)) @Robotgiggle
- Book pages for patterns without input/output iotas will no longer display the type signature line at all ([#1118](https://github.com/FallingColors/HexMod/pull/1118)) @IridescentVoid
- Consideration, Introspection, Retrospection, and Evanition now count towards the per-hex pattern limit ([#1172](https://github.com/FallingColors/HexMod/pull/1172)) @Robotgiggle
- Updated Inline dependency from 1.0.1 to 1.2.2 ([#1043](https://github.com/FallingColors/HexMod/pull/1043)) @Robotgiggle
- Updated Fabric Language Kotlin dependency from 1.9.4 to 1.13.7 ([#1043](https://github.com/FallingColors/HexMod/pull/1043)) @Robotgiggle
- Updated Kotlin for Forge dependency from 4.3.0 to 4.12.0 ([#1043](https://github.com/FallingColors/HexMod/pull/1043)) @Robotgiggle
- Updated zh_cn translations ([#995](https://github.com/FallingColors/HexMod/pull/995), [#1037](https://github.com/FallingColors/HexMod/pull/1037), [#1045](https://github.com/FallingColors/HexMod/pull/1045)) @ChuijkYahus
- Updated various translations via Weblate ([#1251](https://github.com/FallingColors/HexMod/pull/1251), [#1261](https://github.com/FallingColors/HexMod/pull/1261), [#1265](https://github.com/FallingColors/HexMod/pull/1265), [#1269](https://github.com/FallingColors/HexMod/pull/1269), [#1272](https://github.com/FallingColors/HexMod/pull/1272), [#1274](https://github.com/FallingColors/HexMod/pull/1274))
  - ru_ru by @DarkThunderer and @KOCTEP
  - zh_cn by @ChuijkYahus

### Fixed

- Fixed a crash loop when trying to generate a creative-mode ancient scroll for a Great Spell whose per-world pattern hasn't been calculated yet ([#992](https://github.com/FallingColors/HexMod/pull/992)) @Robotgiggle
- Fixed a crash when trying to generate a loot scroll for a Great Spell whose per-world pattern hasn't been calculated yet ([#1079](https://github.com/FallingColors/HexMod/pull/1079)) @Robotgiggle
- Fixed Create Water and Create Lava causing the "position is forbidden to you" mishap when the position is actually just out of ambit ([#1029](https://github.com/FallingColors/HexMod/pull/1029)) @pythonmcpi
- Fixed a broken translation key on the error message from a Shepherd Directrix ([#1013](https://github.com/FallingColors/HexMod/pull/1013)) @Teal-Wolf-25-v2
- Fixed the log spam caused by double pattern registration when joining a world ([#999](https://github.com/FallingColors/HexMod/pull/999)) @Real-Luxof
- Fixed various incorrect documentation in the Hex Book ([#1010](https://github.com/FallingColors/HexMod/pull/1010)) @Robotgiggle
- Fixed a broken translation key when trying to use Place Block with no placeable items in your hotbar ([#1010](https://github.com/FallingColors/HexMod/pull/1010)) @Robotgiggle
- Fixed a potential chunkban when leaving a looping spell circle running for long enough ([#908](https://github.com/FallingColors/HexMod/pull/908)) @Stick404
- Fixed a potential issue on MacOS when calculating soulglimmer color ([#984](https://github.com/FallingColors/HexMod/pull/984)) @vgskye
- Fixed the book page for the Inert Mindflay mishap incorrectly claiming it would kill unsuitable flay subjects ([#1081](https://github.com/FallingColors/HexMod/pull/1081)) @Robotgiggle
- Fixed Pace Purification returning nonzero velocity on nonmoving items ([#1119](https://github.com/FallingColors/HexMod/pull/1119)) @IridescentVoid
- Fixed Accessor's Purification returning false for scrolls in item frames ([#1115](https://github.com/FallingColors/HexMod/pull/1115)) @IridescentVoid
- Fixed Break Block being unable to break cobwebs on Fabric ([#1218](https://github.com/FallingColors/HexMod/pull/1218)) @Robotgiggle
- Fixed the zero vector not being listed as falsy on the Augur's Purification page ([#1273](https://github.com/FallingColors/HexMod/pull/1273)) @DetermiedMech1
- Fixed the command to list all per-world patterns not working ([#1277](https://github.com/FallingColors/HexMod/pull/1277)) @Olfi01
- Fixed a mixin issue that made gaslighting not work properly on Fabric ([#1281](https://github.com/FallingColors/HexMod/pull/1281)) @Robotgiggle

### Internal

- The mod now uses Fabric Loom 1.9, Gradle 8.11, and Kotlin 2.0.20, by Robotgiggle in [#1043](https://github.com/FallingColors/HexMod/pull/1043).
- Updated Inline dependency from 1.0.1 to 1.2.2, by Robotgiggle in [#1043](https://github.com/FallingColors/HexMod/pull/1043).
- The mod now uses Fabric Loom 1.9, Gradle 8.11, and Kotlin 2.0.20 ([#1043](https://github.com/FallingColors/HexMod/pull/1043)) @Robotgiggle
- Changed the internal implementation of Thoth's Gambit to use a `TreeList` for more efficiency ([#1031](https://github.com/FallingColors/HexMod/pull/1031)) @s5bug
- Deprecated the version of `matchPattern` that takes a boolean argument since it always raises an exception ([#1002](https://github.com/FallingColors/HexMod/pull/1002)) @beholderface
- `Iota` subclasses can now specify whether the iota should be displayed with or without commas when in a list ([#988](https://github.com/FallingColors/HexMod/pull/988)) @TheRobbie73
- Made the `getUsableStacks` and `getPrimaryStacks` methods in `CastingEnvironment` and its subclasses public ([#907](https://github.com/FallingColors/HexMod/pull/907)) @miyucomics
- Improved handling for duplicate pattern signatures in hexdoc ([#1007](https://github.com/FallingColors/HexMod/pull/1007)) @object-Object
- `CircleExecutionState` now stores the shape of the spell circle using two corners rather than an entire list of positions ([#908](https://github.com/FallingColors/HexMod/pull/908)) @Stick404
- Added methods in `Action` and `Iota` to define behavior when inside parentheses, and de-hardcoded the iota-escaping patterns ([#1047](https://github.com/FallingColors/HexMod/pull/1047)) @Robotgiggle
- Added visitor methods to `Iota` to enable walking iotas in an extensible way ([#1191](https://github.com/FallingColors/HexMod/pull/1191)) @poolcritter

## `0.11.3` - 2025-11-22

### Added

- Added several new player attributes (ported from [Hexxy Attributes](https://modrinth.com/mod/hexxyattributes)) ([#823](https://github.com/FallingColors/HexMod/pull/823)) @beholderface
- Added ancient cyphers ([#838](https://github.com/FallingColors/HexMod/pull/838)) @Robotgiggle
- Added the ability for wandering traders to sell ancient scrolls ([#837](https://github.com/FallingColors/HexMod/pull/837)) @Robotgiggle
- Added a creative tab for ancient scrolls ([#837](https://github.com/FallingColors/HexMod/pull/837)) @Robotgiggle
- Added a config option to disable Greater Teleport item splatting ([#841](https://github.com/FallingColors/HexMod/pull/841)) @TheDrawingCoding-Gamer
- Added loot tables to generate exactly one random scroll/cypher ([#844](https://github.com/FallingColors/HexMod/pull/844)) @Robotgiggle
- Added recipes for slate, amethyst, and quenched allay blocks ([#903](https://github.com/FallingColors/HexMod/pull/903)) @KyanBirb
- hexdoc: Added the ability to load patterns from a JSON file (instead of scraping them from source code with regex) ([#911](https://github.com/FallingColors/HexMod/pull/911)) @object-Object
- Added a new Ancient Pigment and changed the appearance of the default pigment ([#912](https://github.com/FallingColors/HexMod/pull/912)) @Robotgiggle

### Changed

- Updated the spell circle documentation to clarify their new behavior in 0.11.x ([#814](https://github.com/FallingColors/HexMod/pull/814)) @Robotgiggle
- Added the recipes for bamboo and cherry staves to the notebook entry ([#814](https://github.com/FallingColors/HexMod/pull/814)) @Robotgiggle
- Renamed the eval limit mishap from "Delve Too Deep" to "Lost in Thought", and updated the description and error message to match ([#814](https://github.com/FallingColors/HexMod/pull/814)) @Robotgiggle
- Updated zh_cn translations ([#799](https://github.com/FallingColors/HexMod/pull/799), [#828](https://github.com/FallingColors/HexMod/pull/828), [#847](https://github.com/FallingColors/HexMod/pull/847), [#913](https://github.com/FallingColors/HexMod/pull/913), [#953](https://github.com/FallingColors/HexMod/pull/953), [#954](https://github.com/FallingColors/HexMod/pull/954), [#971](https://github.com/FallingColors/HexMod/pull/971)) @ChuijkYahus
- Added apostrophes to Compass' Purification in the notebook ([#867](https://github.com/FallingColors/HexMod/pull/867)) @kineticneticat
- Updated the description of Ignite to clarify that it works on all entities ([#844](https://github.com/FallingColors/HexMod/pull/844)) @Robotgiggle
- Improved documentation of iota embedding ([#862](https://github.com/FallingColors/HexMod/pull/862)) @Robotgiggle
- Documented the behaviour of the Shepherd Directrix when no boolean is present on the stack ([#862](https://github.com/FallingColors/HexMod/pull/862)) @Robotgiggle
- Changed vectors in the notebook to use parentheses rather than square brackets ([#862](https://github.com/FallingColors/HexMod/pull/862)) @Robotgiggle
- Changed the rarity of several post-enlightenment items ([#860](https://github.com/FallingColors/HexMod/pull/860)) @Robotgiggle
- API: Changed the base class of `Mishap` from `Throwable` to `RuntimeException` to reduce the likelihood of server crashes ([#933](https://github.com/FallingColors/HexMod/pull/933)) @navarchus
- API: Added `@Throws` annotation to several `Action` methods to allow Java pattern implementations to throw mishaps ([#935](https://github.com/FallingColors/HexMod/pull/935)) @navarchus
- Improved the documentation for the media cube ([#843](https://github.com/FallingColors/HexMod/pull/843)) @object-Object
- Changed Greater Teleport to mishap when a passenger is immune to teleportation ([#916](https://github.com/FallingColors/HexMod/pull/916)) @Robotgiggle
- Re-added the slate limit for spell circles ([#909](https://github.com/FallingColors/HexMod/pull/909)) @Stickia
- Renamed Inverse Tangent Purification II to Inverse Tangent Distillation ([#921](https://github.com/FallingColors/HexMod/pull/921)) @Robotgiggle
- Massively improved ru_ru translations ([#832](https://github.com/FallingColors/HexMod/pull/832)) @JustS-js and LedinecMing
- Changed the invalid-pattern mishap to display the offending pattern ([#951](https://github.com/FallingColors/HexMod/pull/951)) @Robotgiggle
- Changed the invalid-iota mishap to display the type of the offending iota along with the iota itself ([#951](https://github.com/FallingColors/HexMod/pull/951)) @Robotgiggle
- Changed the disallowed-action mishap to properly display the offending action ([#970](https://github.com/FallingColors/HexMod/pull/970)) @Robotgiggle

### Fixed

- Fixed missing dependency metadata on CurseForge/Modrinth.
- Fixed a freeze when rendering patterns in certain cases ([#800](https://github.com/FallingColors/HexMod/pull/800)) @vgskye
- Added a missing translation for `hexcasting.subtitles.casting.cast.fail` ([#814](https://github.com/FallingColors/HexMod/pull/814)) @Robotgiggle
- Fixed incorrect Amethyst Sconce block rotation ([#814](https://github.com/FallingColors/HexMod/pull/814)) @Robotgiggle
- Fixed Place Block not pulling items from the inventory when available ([#812](https://github.com/FallingColors/HexMod/pull/812)) @garyantonyo
- Fixed a broken link in A Primer On Vectors ([#877](https://github.com/FallingColors/HexMod/pull/877)) @bearofbusiness
- Fixed Impulse not increasing its cost for subsequent uses in one cast ([#853](https://github.com/FallingColors/HexMod/pull/853)) @vgskye
- Added a config option to enable commas between patterns in lists ([#844](https://github.com/FallingColors/HexMod/pull/844)) @Robotgiggle
- Fixed incorrect cost for Wayfarer's Flight ([#844](https://github.com/FallingColors/HexMod/pull/844)) @Robotgiggle
- Fixed a bug where some patterns would check for media requirements earlier than expected ([#855](https://github.com/FallingColors/HexMod/pull/855)) @vgskye
- Fixed some modded fake players being able to overcast infinitely for free ([#854](https://github.com/FallingColors/HexMod/pull/854)) @vgskye
- Fixed Negation Purification not working on numbers ([#869](https://github.com/FallingColors/HexMod/pull/869)) @Robotgiggle
- Added a missing translation for the `no_spell_circle` mishap ([#862](https://github.com/FallingColors/HexMod/pull/862)) @Robotgiggle
- Fixed allays sometimes becoming invulnerable when mindflayed ([#928](https://github.com/FallingColors/HexMod/pull/928)) @navarchus
- hexdoc: Improved the error message for missing patterns ([#931](https://github.com/FallingColors/HexMod/pull/931)) @navarchus
- Fixed incorrect rendering for rod variants of cyphers/trinkets/artifacts ([#936](https://github.com/FallingColors/HexMod/pull/936)) @navarchus
- Fixed several issues related to invalid/overlapping patterns ([#938](https://github.com/FallingColors/HexMod/pull/938)) @object-Object
- Fixed sticky teleportation (ie. teleporting passengers) not working (again) ([#916](https://github.com/FallingColors/HexMod/pull/916)) @Robotgiggle
- Fixed Greater Teleport sometimes failing with a "moved wrongly" message ([#916](https://github.com/FallingColors/HexMod/pull/916)) @Robotgiggle
- Fixed incorrect face textures for the Empty and Shepherd Directrix ([#921](https://github.com/FallingColors/HexMod/pull/921)) @Robotgiggle
- Removed unnecessary error logs when activating a bound Cleric Impetus with its owner offline ([#921](https://github.com/FallingColors/HexMod/pull/921)) @Robotgiggle
- Fixed the Cleric Impetus becoming unbound when activated with its owner offline ([#921](https://github.com/FallingColors/HexMod/pull/921)) @Robotgiggle
- Fixed some directrices inadvertently accepting input from their output faces ([#921](https://github.com/FallingColors/HexMod/pull/921)) @Robotgiggle
- Added a missing translation for providing something other than an entity as the first input to Flay Mind ([#921](https://github.com/FallingColors/HexMod/pull/921)) @Robotgiggle
- Added several missing translations for config options ([#921](https://github.com/FallingColors/HexMod/pull/921)) @Robotgiggle
- Clarified the description of Division Distillation ([#832](https://github.com/FallingColors/HexMod/pull/832)) @JustS-js
- Fixed a bug where some patterns inconsistently checked the lower-north-west corner of blocks for ambit instead of the center ([#959](https://github.com/FallingColors/HexMod/pull/959)) @YukkuriC
- Fixed Erase Item cost not scaling with stack size ([#966](https://github.com/FallingColors/HexMod/pull/966)) @PoolloverNathan

## Previous versions

See https://www.curseforge.com/minecraft/mc-mods/hexcasting/files or https://modrinth.com/mod/hex-casting/changelog for changelogs from previous versions.
