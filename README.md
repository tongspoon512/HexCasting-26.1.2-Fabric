# Hex Casting — Minecraft 26.1.2 / Fabric

Hex Casting 的 **Minecraft 26.1.2（Fabric）** 移植分支。原模组由 petrak@（gamma-delta）开发，是一个「在空中绘制图案来施法」的魔法模组，灵感来自 PSI。

本仓库基于上游 0.11.4 代码，把 1.20.1 时代的实现迁移到 26.1 的新 API 体系：官方名直出（无混淆、无 remap）、Java 25、渲染走 render state / submit 管线、网络走 payload 体系。

在此基础上，本分支还移植了 [HexDebug](https://github.com/object-Object/HexDebug) 的**剪接台（Splicing Table）**，用于在游戏内直接编辑 iota 列表。

> **个人移植项目声明**
>
> 本仓库是个人学习与存档用途的**非官方移植分支**，与 FallingColors、petrak@（gamma-delta）、object-Object 及 YukkuriC 均无隶属或合作关系，也不代表上述项目的质量与立场。
>
> - 这里的改动**不会提交给上游仓库**，也不建议把本仓库的问题反馈到上游 issue 区。
> - 上游项目本身请以各自官方仓库与文档为准；本仓库只保证「能在 26.1.2 Fabric 上跑起来」。
> - 仅供个人存档与继续移植使用，并非官方发布版本。遇到问题时请附上完整日志。
> - 上游的 GitHub Actions 工作流（`build_docs.yml`、`pr.yml`）已从本仓库移除，本分支不使用上游 CI。

## 状态一览

| 项 | 状态 |
| --- | --- |
| Minecraft | 26.1.2（`fabric.loom.disableObfuscation=true`，源码名 = 官方名 = 线上名） |
| 加载器 | Fabric Loader ≥ 0.20.1 |
| Java | 25 |
| 构建 | `:Fabric:build` / `:Fabric:jar` 均通过，产出可安装 jar |
| 运行验证 | 客户端已在真实实例中启动、进入世界；施法、内联图案、指南书、剪接台均已实机验证 |
| Forge | **未移植**，未在 `settings.gradle` 中 include，不参与构建 |

Common 与 Fabric 是合并编译的：`Fabric/build.gradle` 会把 `Common` 的主源码并入 Fabric 的 `compileJava`，因此 Common 并不是独立的编译产物。

## 特色改动

### 1. 剪接台（移植自 HexDebug）

新增方块与物品 `hexdebug:splicing_table`，代码位于 `gay.object.hexdebug` 包，资源命名空间为 `hexdebug`，但**不新增 mod id**，仍随主 jar 一起加载。

- **方块实体**：4 个槽位——列表槽、剪贴板槽、介质槽、法杖槽；列表/剪贴板只接受持有 iota 数据的物品，法杖槽只接受 `hexcasting:staves` 标签物品。
- **列表视图**：一次显示 9 个 iota，左右翻页按钮与滚轮/方向键/PageUp/PageDown/Home/End 均可移动视图。
- **选择**：点击 iota 选中单元格，点击格子之间的窄条选中「边缘」；支持 Shift 扩展选择、Ctrl+Shift 移动选择。
- **编辑动作**：左移/右移、删除、退格、复制、剪切、粘贴（`Ctrl+V` 普通粘贴、`Ctrl+Shift+V` 原样粘贴）、复制条目、撤销/重做，全部经服务端执行并回推数据。
- **迷你法杖网格**：把法杖放进法杖槽后，界面左侧出现 160×160 的绘咒网格。在网格内绘制完成的图案会写入当前选区，并把选区右移到图案之后；网格只提供绘制与音效，不施法、不消耗介质。
- **其他**：网格下方有清空按钮（仅清空客户端绘制的视觉内容，不改列表数据）；绘制过程中按 ESC 会取消当前图案而不是关闭界面；点击法杖槽区域不会误触绘制。

实现位置：

- [SplicingTableBlock.kt](Fabric/src/main/java/gay/object/hexdebug/blocks/SplicingTableBlock.kt)
- [SplicingTableBlockEntity.kt](Fabric/src/main/java/gay/object/hexdebug/blocks/splicing/SplicingTableBlockEntity.kt)
- [SplicingTableMenu.kt](Fabric/src/main/java/gay/object/hexdebug/gui/splicing/SplicingTableMenu.kt) / [SplicingTableScreen.kt](Fabric/src/main/java/gay/object/hexdebug/gui/splicing/SplicingTableScreen.kt)
- [splicing 数据模型](Fabric/src/main/java/gay/object/hexdebug/splicing/)
- [HexDebugMessages.kt](Fabric/src/main/java/gay/object/hexdebug/networking/HexDebugMessages.kt)

合成配方（对应上游）：

```
P C P        P = 磨制木板（hexcasting:edified_planks）
A F A        C = 充能紫水晶（hexcasting:charged_amethyst）
S G S        A = 紫水晶碎片      F = 焦点（hexcasting:focus）
             S = 板岩块          G = 金锭
```

配方、战利品表与配方解锁进度分别位于 `data/hexdebug/recipe/`、`data/hexdebug/loot_table/`、`data/hexdebug/advancement/`。

### 2. 内置指南书（不依赖 Patchouli）

26.1 没有可用的 Patchouli，本分支用原生 GUI 重做了一本指南书，**内容直接复用上游书的 JSON 与语言文件**，不另外维护一套文案。

- 入口物品：`hexcasting:guide_book`，配方为「书 + 紫水晶粉」（无序合成），也已加入创造模式物品栏。
- 打开方式：手持右键。尺寸为 512×256 贴图里的 271×180 双页书。
- 导航：目录 → 分类 → 条目 → 双页翻页；支持鼠标滚轮滚动长页、`<` `>` 翻页、`目录/返回`回到上一级。
- 内容规模：9 个分类、82 个条目、528 页，全部来自 `assets/hexcasting/patchouli_books/thehexbook/en_us/`。
- 页型覆盖：`patchouli:text`、`link`、`image`、`spotlight`、`empty`、`crafting`，以及 Hex Casting 自定义的 `pattern`、`manual_pattern`、`manual_pattern_nosig`、`brainsweep`、`crafting_multi`。
- 文本渲染：支持 `$(br)`/`$(br2)`/`$(p)`/`$(li)`/`$(#rgb)`/`$(0-f)`/`$(o)`/`$(n)`/`$(l:...)`/`$(k:...)`、book.json 自定义宏；**按页面宽度自动换行，超长单词按字符折行**，不会戳出书页。
- 配方页直接从 jar 内的配方 JSON 读取并画 3×3 网格（26.1 客户端没有完整的 `RecipeManager`，因此不走运行时查询）。
- 图案页复用现有 `PatternDrawingUtil`，按官方 Patchouli 组件的同一套参数（单元格、描边、点阵）绘制。

实现位置：

- [GuideBook.kt](Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideBook.kt)：书数据加载（分类、条目、宏、图标解析）
- [GuideBookScreen.kt](Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideBookScreen.kt)：双页 GUI、导航、各页型渲染
- [GuideMarkup.kt](Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideMarkup.kt)：文本标记解析 + 自动换行 + 链接命中
- [GuideRecipes.kt](Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideRecipes.kt)：配方 JSON 读取
- [GuideTextures.kt](Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideTextures.kt)：按原始 PNG 尺寸做 UV 缩放
- [ItemGuideBook.java](Common/src/main/java/at/petrak/hexcasting/common/items/ItemGuideBook.java)：入口物品

### 3. 内联图案渲染

26.1 没有可用的 Inline 版本，本仓库自行实现了一个最小内核，让 HexCasting 的图案语法可以直接显示在 GUI 文本里（聊天栏、物品提示框、物品名等）。

支持的写法（与上游 Inline 的图案匹配器一致）：

```
{west aqw}          {east qqqqq}
[west aqw]          <west aqw>
{w aqw}             {north_east aqwqew}
```

- 匹配到的整段文本被折叠成一个图案字形，宽度按图案实际宽度推进，后续文字正常接排；
- 图案颜色取自文本颜色；
- 图案字形通过 `Font.getGlyphSource` 提供（同时覆盖渲染与量宽），因此排版/换行不会错位。

实现位置：

- [InlinePatternFont.java](Common/src/main/java/at/petrak/hexcasting/interop/inline/InlinePatternFont.java)
- [InlineTextProcessor.java](Common/src/main/java/at/petrak/hexcasting/interop/inline/InlineTextProcessor.java)
- [InlinePatternTextRenderable.java](Common/src/main/java/at/petrak/hexcasting/interop/inline/InlinePatternTextRenderable.java)
- [MixinFontInline.java](Common/src/main/java/at/petrak/hexcasting/mixin/client/MixinFontInline.java)、[MixinGuiTextRenderStateInline.java](Common/src/main/java/at/petrak/hexcasting/mixin/client/MixinGuiTextRenderStateInline.java)

### 4. 施法界面透明背景与网格间距

- 施法 GUI（`GuiSpellcasting`）会清空 `extractBackground`，只绘制图案与 UI 元素，背景保持透明，便于叠加在任意场景上。
- 新增客户端配置项「咒术网格间距」（`gridSpacing`），缩放施法界面中网格相邻两点的距离，取值范围 0.5–2.0，**默认 1.0**。可通过 ModMenu 或 `config/hexcasting/client.json` 调整。

### 5. 个人媒质池与拖延（移植自 HexOverpowered）

从 [YukkuriC/HexOverpowered](https://github.com/YukkuriC/HexOverpowered) 只移植了「个人媒质条」与「拖延」两部分，未引入心灵堆栈、Factor Cut 等内容。

**个人媒质池**

- 玩家新增 3 个可同步属性：`hexdebug:personal_media`、`hexdebug:personal_media_max`、`hexdebug:personal_media_regen`。
- 施法取介质时个人池**优先**被消耗（优先级 `Int.MAX_VALUE`），但不参与「造电池」。
- 每隔 `personalMediaRegenInterval` tick 自动回充 `personalMediaRegenStep`，默认 20 tick / 1 紫水晶碎片，上限默认 10 充能紫水晶（1,000,000 粉）。
- 默认需完成「启蒙」进度才会启用；可在服务端配置中关闭该限制。
- 血条上方显示粉紫色媒质条（带缓慢呼吸透明度），值为 0 或池未启用时隐藏。

相关配置（`config/hexcasting/server.json`）：`personalMediaPool`、`personalMediaMax`、`personalMediaRegenStep`、`personalMediaRegenInterval`、`personalMediaAfterEnlightened`。

**拖延图案**

| 图案 | 签名 / 起始方向 | 参数 | 说明 |
| --- | --- | --- | --- |
| `hexdebug:get_personal_media` | `qqaqqea` / east | 0 | 返回当前个人媒质 |
| `hexdebug:get_personal_media_max` | `qqaqqqd` / east | 0 | 返回个人媒质上限 |
| `hexdebug:mind_env/schedule` | `waawedaqqqqdeaqq` / south_west | 2（代码、延迟 tick） | 延迟执行代码；延迟 ≤0 时立刻执行 |
| `hexdebug:mind_env/running_code` | `wqaqwweeeeeqdeaqq` / south_west | 0 | 返回当前待执行代码，无则返回 Null |

- 同一施法者再次排程会取消旧任务；延迟执行使用静默施法环境，避免每 tick 刷屏。
- 保留上游的同 tick 递归保护（同一 tick 最多 10 次「无用」调用、100000 次普通调用）。
- 新增服务端配置：`enablesMindEnvActions`（默认开）、`bypassesSilencedEnvRestrictions`（默认关，开启后延迟法术使用原始环境并正常输出消息）。

实现位置：

- [HexDebugAttributes.java](Common/src/main/java/gay/object/hexdebug/media/HexDebugAttributes.java) / [PersonalManaHolder.kt](Common/src/main/java/gay/object/hexdebug/media/PersonalManaHolder.kt) / [PersonalMediaRegen.kt](Common/src/main/java/gay/object/hexdebug/media/PersonalMediaRegen.kt)
- [OpGetPersonalMedia.kt](Common/src/main/java/gay/object/hexdebug/actions/OpGetPersonalMedia.kt) / [OpScheduleCall.kt](Common/src/main/java/gay/object/hexdebug/actions/OpScheduleCall.kt) / [OpScheduledCode.kt](Common/src/main/java/gay/object/hexdebug/actions/OpScheduledCode.kt)
- [SilencedCastingEnv.kt](Common/src/main/java/gay/object/hexdebug/ext/SilencedCastingEnv.kt)
- [PersonalMediaHud.kt](Fabric/src/main/java/gay/object/hexdebug/client/PersonalMediaHud.kt)（贴图 `assets/hexdebug/textures/gui/media_bar.png`）
- [HexDebugActions.kt](Common/src/main/java/gay/object/hexdebug/actions/HexDebugActions.kt)：动作注册

> 延迟任务仅存在于内存中，服务器重启（`SERVER_STARTING`）会清空队列；玩家退出时未执行的任务也不会保留。

### 6. MoreIotas（移植自 FallingColors/MoreIotas）

上游 [MoreIotas](https://github.com/FallingColors/MoreIotas)（作者 Talia，MIT）是一套 iota 扩展库，本分支把它的全部玩法并入单 jar，并保持注册命名空间 `moreiotas:*` 以兼容旧存档。

- **6 种新 iota**：`moreiotas:string`、`moreiotas:matrix`、`moreiotas:iota_type`、`moreiotas:entity_type`、`moreiotas:item_type`、`moreiotas:stack`（物品组，用 26.1 数据组件序列化）。
- **45 个图案**：17 个字符串图案、12 个矩阵图案（含 4 个元素级运算符）、6 个类型/实体图案、4 个物品图案，另含 6 个算子改写的图案（`moreiotas:altadd` 等）。
- **三套算术**：`moreiotas:string`（10 个）、`moreiotas:matrix`（12 个，含矩阵指数）、`moreiotas:type`（3 个），注册 id 与上游一致。
- **聊天前缀**：`moreiotas:chat_prefix_holder` 组件 + 聊天事件处理；`Sifter's Gambit` 设置前缀后，带前缀的消息只对「耳语之精思」可见。
- **服务端配置**：`maxMatrixSize`（默认 144）、`maxStringLength`（默认 1728）、`setBlockStringCost` / `nameCost`（默认各 0.01 粉），随 hexcasting 的 Cloth 配置界面展示。
- **指南书条目**：字符串、矩阵、类型、物品、物品 Iota 共 5 个条目已并入内置指南书。

相关代码位于 `Common/src/main/java/ram/talia/moreiotas/`，Fabric 侧组件与事件在 `Fabric/src/main/java/ram/talia/moreiotas/fabric/`。

### 7. HexParse 文本咒术（`/hexParse`）

26.1 没有可用的 HexParseMod，本分支按它的命令格式自行实现了一套「文本 ↔ iota」的双向转换，方便把长法术写成文本保存、分享，再用命令写回物品。

- **导入**：`/hexParse "<代码>" [改名]` 把文本解析成 iota，写入主手可写容器（Focus、法术书等），可同时改物品名。
- **导出**：`/hexParse read` 读取手持容器并输出可点击复制的文本；`/hexParse read_signatures` 只输出 `<起始方向,角度串>` 形式的签名。
- **传播**：`/hexParse share` 向全服广播手持咒术文本。
- **剪贴板**：`/hexParse clipboard [改名]` 用剪贴板原文导入，`clipboard_angles [改名]` 只导入引号内的角度串；客户端与服务端往返走新增的 `MsgPullClipboardS2C` / `MsgPushClipboardC2S`。
- **语法要点**：顶层本身即列表；`(` `)` 是 Introspection/Retrospection，在运行期构造列表；裸数字是 `DoubleIota` **字面量**，要被 `eval` 执行必须写成 `num_<n>` 图案；`vec_x_y_z` 与 `mask_` 有专用写法；图案可用名称、别名或方向签名书写；实体、字符串、矩阵、类型 iota 均支持。`read` 输出的文本可再次 `/hexParse` 回写，嵌套列表、图案方向与掩码都会保留。

实现位置：

- [HexParseCommand.java](Common/src/main/java/at/petrak/hexcasting/common/lib/hexparse/HexParseCommand.java)：命令树、读写目标与聊天输出
- [IotaCode.kt](Common/src/main/java/at/petrak/hexcasting/common/lib/hexparse/IotaCode.kt)：分词、解析、反向输出、数字/掩码图案角度算法
- [MsgPullClipboardS2C.java](Common/src/main/java/at/petrak/hexcasting/common/msgs/MsgPullClipboardS2C.java) / [MsgPushClipboardC2S.java](Common/src/main/java/at/petrak/hexcasting/common/msgs/MsgPushClipboardC2S.java)：剪贴板往返

示例法术文档见 [hexparse/连锁挖掘.md](hexparse/连锁挖掘.md)：3×3 平面、3×3×3 立方、直线钻探，以及「同种矿物 + 自引用（quine）」的连锁挖掘。

### 8. 探知透镜组（`hexcasting:lens_assembly`）

- 新增物品 `hexcasting:lens_assembly`，手持右键打开网格间距调节界面（`GridSpacingScreen`）。
- 界面中央实时预览当前网格密度，滑块范围 0.5–2.0；关闭界面时把结果写回客户端配置 `gridSpacing`，与 ModMenu、`config/hexcasting/client.json` 完全等价。
- 该间距同时作用于施法界面与剪接台的迷你法杖网格。
- 附带自制贴图、配方（金锭 + 充能紫水晶 + 探知透镜）与指南书条目。

实现位置：

- [ItemScryingLensAssembly.java](Common/src/main/java/at/petrak/hexcasting/common/items/ItemScryingLensAssembly.java)
- [GridSpacingScreen.kt](Common/src/main/java/at/petrak/hexcasting/client/gui/GridSpacingScreen.kt)
- [HexConfig.java](Common/src/main/java/at/petrak/hexcasting/api/mod/HexConfig.java) 中的 `gridSpacing()`

### 9. 物品 iota 提示自适应换行

聚焦器、法术书、思想结、算盘等物品的物品提示里，列表 iota 不再挤成一条长行：

- 按 `font.width` 实测每个 iota（含逗号）的渲染像素宽度，超出预算才换行；图案 iota 也按真实字宽计入。
- 预算取 GUI 缩放宽度减去 32 像素边距，并扣掉首行 `Contains:` 前缀宽度；后续行四空格缩进。
- 收窄窗口或调大 GUI 缩放后，下一次悬停即按新宽度重排；单行 `display()`（法术书界面内等场景）行为不变。

实现位置：[IotaType.java](Common/src/main/java/at/petrak/hexcasting/api/casting/iota/IotaType.java) 的 `displayLines`、[ListIota.java](Common/src/main/java/at/petrak/hexcasting/api/casting/iota/ListIota.java)、[IotaHolderItem.java](Common/src/main/java/at/petrak/hexcasting/api/item/IotaHolderItem.java)。

### 10. 键盘施法、图案补全与别名（移植自 Hexcessible）

参考 [tizu69/Hexcessible](https://github.com/tizu69/Hexcessible) 的纯客户端无障碍层，在施法界面内实现了一套不改变原有鼠标绘制的键盘操作：

- **空闲悬停提示**：鼠标停在已绘制图案上 0.5 秒，显示 `<起始方向, 角度串>` 与该图案的本地化名称；数字之精思与簿记员之策略会显示计算出的具体值（如「数字之精思：4.00」「簿记员之策略：-v--v」）。
- **右键图案列表**：空闲时在绘图区右键，打开可滚动的全部已注册图案列表，直接输入即可搜索，`↑/↓` 选择、`Enter`/`Tab` 施放；列表只显示已解锁图案并标注锁定数量。
- **键盘绘制**：空闲时按 `q/w/e/a/d` 直接开始输入相对角度，`Backspace`/`s` 撤销，`Enter`/`Tab`/空格 施放，`H/J/K/L` 与方向键移动落点，`R`/`Shift+R` 或滚轮旋转；绘制中显示灰色幽灵图案与蓝色实际落点，并在每个可延伸方向标注下一个按键，空间不足时红字提示。
- **绘制中实时预览**：鼠标拖着画图案时，在落点旁实时显示「假如现在就松手会画出什么」——当前角度串与图案名称（数字之精思/簿记员之策略显示计算出的具体值）。
- **模糊补全**（`Ctrl+Space`）：按名称、别名与动作 id 做逐字符模糊匹配，`↑/↓` 或滚轮选择，`Enter`/`Tab` 直接进入键盘绘制，`Esc` 退出；空查询时鼠标移出 1.75 格半径自动收起。
- **JustEnoughCharacters 兼容**：安装 JEC 后，补全与右键图案列表可以直接用拼音搜索中文图案名（如 `shuzi`、`zhisi` 匹配「数字之精思」）；兼容层通过反射调用 JEC 的 `HexcessibleHooks.mergeScore`，未安装 JEC 时自动退回原有模糊匹配。
- **指南书说明**：补全列表会展示选中图案的输入/输出与指南书说明文本，`←/→` 在同一图案的多条实现说明之间切换；尚未解锁的图案不会出现在列表中，只显示「另有 N 个图案尚未解锁」。
- **别名**（`Ctrl+E`）：在悬停图案或补全列表上改名，`Enter`/`Tab` 保存、留空清除、`Esc` 取消；别名保存在 `config/hexcasting_aliases.json`，并立即用于悬停提示与模糊搜索。
- **客户端配置**：`config/hexcasting/client.json` 新增「图案补全条数」（`patternAutocompleteCount`，默认 5，取值 3–20）与「图案补全显示指南书说明」（`showPatternDocsInAutocomplete`，默认开），也可在 ModMenu 中调整。

实现位置：[GuiSpellcasting.kt](Common/src/main/java/at/petrak/hexcasting/client/gui/GuiSpellcasting.kt)（状态机与绘制）、[PatternKeyboard.kt](Common/src/main/java/at/petrak/hexcasting/client/gui/PatternKeyboard.kt)（键位/签名/模糊搜索）、[PatternDocs.kt](Common/src/main/java/at/petrak/hexcasting/client/gui/PatternDocs.kt)（指南书说明与解锁过滤）、[PatternAliases.kt](Common/src/main/java/at/petrak/hexcasting/client/gui/PatternAliases.kt)（别名持久化）。

文字输入走 26.1 的 `charTyped` 与 `preeditUpdated`：支持输入法直接输入中文，组字过程中提示框会显示「输入中：…」，非美式键盘布局也按实际字符接收；拼音搜索需要安装 JustEnoughCharacters。

## 安装

1. 安装 Fabric Loader ≥ 0.20.1（对应 26.1.2）。
2. 把构建产物放入实例的 `mods/`：

   ```
   hexcasting-fabric-26.1.2-0.11.4.jar
   ```

3. 同时安装以下前置模组（缺失会直接报硬依赖错误）：

   | 模组 | 最低版本 | 说明 |
   | --- | --- | --- |
   | Fabric API | ≥ 0.155.3+26.1.2 | 26.1 起 mod id 为 `fabric-api`，旧 `fabric` 别名已不存在 |
   | Fabric Language Kotlin | ≥ 1.14.2+kotlin.2.4.21 | 运行时必需 |
   | Cardinal Components API | ≥ 8.0.1 | 需要 base / entity / item / block 等子模块 |
   | Cloth Config | ≥ 26.1.154 | 配置界面 |

   可选：ModMenu（提供配置入口）。

> 仓库内以源码形式内嵌了 Patchouli、Inline、Trinkets、Paucal 等依赖的桩件，**不要**再安装真实的同名模组，否则会出现同名类冲突。指南书已由模组自带 GUI 实现，不需要安装 Patchouli。剪接台已并入本 jar，也不需要单独安装 HexDebug。

## 构建

需要 JDK 25（`build.gradle` 中 `release = 25`、`jvmTarget = 25`）。

```bash
./gradlew :Fabric:build     # 完整构建，含 sources / javadoc jar
./gradlew :Fabric:jar       # 只出可安装 jar，速度更快
./gradlew :Fabric:clean     # 清理构建产物
```

产物：

```
Fabric/build/libs/hexcasting-fabric-26.1.2-0.11.4.jar
Fabric/build/libs/hexcasting-fabric-26.1.2-0.11.4-sources.jar
Fabric/build/libs/hexcasting-fabric-26.1.2-0.11.4-javadoc.jar
```

开发期运行（需要图形环境）：

```bash
./gradlew :Fabric:runClient
./gradlew :Fabric:runServer
./gradlew :Fabric:runDatagen   # 输出到 Fabric/src/generated/resources
```

## 项目结构

```
Common/   平台无关代码：API、施法 VM、方块/物品/实体、数据生成、mixin、客户端 GUI
          └─ gay/object/hexdebug/  HexDebug 移植：media（个人媒质）、actions（图案）、ext（静默环境）
Fabric/   平台实现：入口、网络、CCA 组件、事件、interop、datagen 接线
          └─ gay/object/hexdebug/  剪接台（方块、菜单、GUI、网络、数据模型）与 HUD
Forge/    上游 1.20.1 遗留，未纳入构建，请勿修改
doc/      原版文档源
hexparse/  HexParse 文本法术文档（连锁挖掘等）
```

## 26.1 移植要点

迁移过程中主要对齐了以下断裂点，方便后续继续维护：

- **网络**：`registerGlobalReceiver(Identifier, …)` 已移除，全部消息改走 `CustomPacketPayload` + `PayloadTypeRegistry.serverboundPlay()/clientboundPlay()`，由 `FabricPacketHandler` 统一注册 codec 与收发。
- **数据包目录**：26.1 起数据包目录改为单数（`recipe/`、`loot_table/`、`advancement/`），复数旧目录不会被加载。
- **属性**：所有属性查询/注入改用 `Holder<Attribute>`，`HexAttributes.holder(...)` 负责把裸 `Attribute` 桥接回注册表。
- **装备**：`Item.Properties.equipmentSlot(...)` 与 `ArmorMaterial` 旧体系被移除，改用 `equippable(EquipmentSlot)` 与 `net.minecraft.world.item.equipment.*`。
- **战利品**：`LootTableReference` → `NestedLootTable.lootTableReference`；`LootTable.functions` 由数组变为 `List`；服务端重载资源改用 `ReloadableServerResources.fullRegistries()`。
- **渲染**：`BakedModel`、`BlockRenderDispatcher`、`ItemColor`、`ItemProperties`、`LightTexture`、`PlayerRenderer` 等均已移除或改名，改为 render state + `SubmitNodeCollector` 管线（`AvatarRenderer`、`AltioraLayer` 等已重写）。GUI 元素必须提交 4 顶点一组的三角形并归一化绕序，否则会被固定 QUADS 索引缓冲与背面剔除吞掉。
- **GUI**：`Screen` 由 `render` 改为 `extractRenderState(GuiGraphicsExtractor, …)`；`GuiGraphicsExtractor` 提供 `fill`、`text`、`centeredText`、`blit`、`item`、`pose()` 等绘制入口。
- **输入**：`Screen` 的鼠标回调改为 `MouseButtonEvent` / `KeyEvent`，`GuiSpellcasting` 可嵌入其他界面（剪接台迷你网格即复用此能力）。
- **CCA v8**：item component 形式的附加数据被删除，物品附加数据改为直接构造 adapter；`CardinalComponent` 读写走 `ValueInput` / `ValueOutput`。
- **Datagen**：配方/战利品构建器 codec 化，`RecipeBuilder` 不再有 `getResult()`，`RecipeProvider.buildRecipes()` 的可见性也变了。

## 已知限制

- **剪接台**：仅移植到 P2 阶段——列表编辑与迷你法杖网格可用，但**还不能在剪接台直接施法**（P3 未实现），因此介质槽目前不参与任何逻辑，也不会消耗介质。未移植 Enlightened 剪接台、调试杖、Focus Holder 与调试器（DAP）等功能。
- **拖延**：延迟任务只存在于内存，不随存档持久化；服务器重启或重置时队列被清空，玩家离线时待执行任务也不会保留。
- **指南书**：未做搜索、书签、已读进度与进度（advancement）锁定；`spotlight` 页不显示合成来源；若某条配方在 26.1 移植中缺失（如 `amethyst_dust_packing`），对应页只显示文字不显示网格。
- **Patchouli**：没有 26.1 版本，仓库内以源码形式内嵌 API 桩件；书内容改由内置 GUI 读取，不再需要 Patchouli 运行时。
- **Inline**：仅实现了图案内联，其余 Inline 功能（物品/实体/头像匹配、尺寸修饰、叠加渲染）未实现；世界空间文本（告示牌、名字标签等）会显示占位符。
- **Trinkets**：未移植，`dev/emi/trinkets` 为桩件，饰品渲染器是空实现（功能可用，视觉不显示）。
- **Pehkui**：其可用版本链接的是旧 intermediary 名，无法在 26.1 上运行，`getPehkuiApi()` 直接抛 `UnsupportedOperationException`。**不要安装 Pehkui**。
- **EMI / JEI**：保留编译期集成与插件入口；EMI 相关代码同样基于内嵌桩件。
- **渲染保真度**：26.1 渲染管线重写后，部分特效（画笔、光效、饰品/鞘翅覆盖层等）是按新管线近似实现的，与原版观感可能有差异。
- **MoreIotas**：仅移植 Common/Fabric 玩法，未引入上游的 Forge 端、Paucal/Inline/Fiber 软依赖、JBLAS 与 Mixin/AW；`moreiotas:stack` 已改为 26.1 数据组件序列化，书与笔/成书的页 iota holder 以 `CCItemIotaHolder.Static` 形式恢复。
- **验证程度**：构建与基础实机流程已通过，尚未做完整玩法回归；启动阶段的问题请附日志。

## 玩法与文档

- 原版在线手册：https://hexcasting.hexxy.media/
- HexDebug 在线手册：https://hexdebug.hexxy.media/
- HexParse 法术示例：[hexparse/连锁挖掘.md](hexparse/连锁挖掘.md)
- 更新日志：见 [CHANGELOG.md](./CHANGELOG.md)
- 贡献指南：见 [CONTRIBUTING.md](./CONTRIBUTING.md)

## 致谢与许可

- 原项目：[FallingColors/HexMod](https://github.com/FallingColors/HexMod)，作者 petrak@（gamma-delta）
- 剪接台来源：[object-Object/HexDebug](https://github.com/object-Object/HexDebug)
- 个人媒质池与拖延来源：[YukkuriC/HexOverpowered](https://github.com/YukkuriC/HexOverpowered)（MIT）
- 字符串/矩阵/类型/物品 iota 与相关图案来源：[FallingColors/MoreIotas](https://github.com/FallingColors/MoreIotas)，作者 Talia（MIT）
- 本移植分支遵循原项目许可：[MIT](./LICENSE.txt)
