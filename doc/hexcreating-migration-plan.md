# HexCreating → HexMod 迁移方案

> 目标：把 [yggdyy/hexcreating](https://github.com/yggdyy/hexcreating) 的玩法内容迁移进本项目（MC 26.1.2 / Fabric / HexCasting 0.11.4），
> 适配 Create-Fly（`create`，6.0.9-4），补齐缺失贴图，并且**只有游戏加载 Create 时才激活**这套功能。
>
> 本文只给方案，不含实现代码。

---

## 0. 结论摘要

1. **这不是"改包名就能搬"的移植**。上游基于 MC 1.20.1 + Yarn + Create 0.5.1（`com.simibubi.create`）+ Registrate + PortingLib，
   目标是 MC 26.1.2 + Mojang 官方名 + Create-Fly 6.0.9（`com.zurrtum.create`，无 Registrate）。
   方块实体、流体、传送门、Ponder 四块基本要按新 API 重写，配方/贴图/文本可以复用。
2. **代码整体放进现有 `Fabric` 模块**（与 MoreIotas / HexCassettes / HexDebug 的"内置附属"做法一致），
   `Common` 只做一处小改动（指南书条件显示）。不新增 Gradle 子模块，不产出第二个 jar。
3. **软依赖 + 三层类隔离**：`fabric.mod.json` 不声明 `create` 依赖；入口先用 `isModPresent("create")` 判断，
   再触碰任何 import 了 Create 的类；Mixin 用 `IMixinConfigPlugin` 条件启用；数据包用 `fabric:all_mods_loaded` 条件；
   指南书条目用自定义条件字段过滤。Create 缺席时**零注册、零类加载、零日志报错**。
4. **贴图需自制/补齐约 20–24 个文件**（circle_inputer/outputer 全套、iota_reader/writer 方块贴图、
   media/soul 流体贴图、unfinished_train_gate_frame、media 方块模型等），上游资源本身是不完整的。
5. **建议分 5 个阶段**，先做可玩核心（排版板 / 紫水晶灯 / 液态介质 / 新图案），
   火车传送门（train gate）风险最高，放最后，允许降级或砍掉。
6. **ID 保持 `hexcreating:*`**，方便与上游攻略、视频、旧服务器数据对齐；许可证 MIT，需保留署名。

---

## 1. 上游项目事实清单

### 1.1 构建与依赖

| 项 | 值 |
| --- | --- |
| MC / 映射 | 1.20.1 / Yarn 1.20.1+build.10 |
| 加载器 | Fabric Loader 0.16.7，Fabric API 0.92.2 |
| 语言 | Java 17 + Kotlin 1.9（fabric-language-kotlin 1.10.10） |
| HexCasting | 0.11.2-pre-702（Yarn 名） |
| Create | `create-fabric-1.20.1:0.5.1-f-build.1417`，包名 `com.simibubi.create` |
| 其它 | Paucal 0.6.0-pre-118、Patchouli 84、Registrate、PortingLib（FluidStack）、Flywheel、Ponder |
| 许可 | MIT（`LICENSE`），作者 yggdyy_ |
| 版本 | gradle.properties 0.0.4，fabric.mod.json 仍写 0.0.2（上游自身不一致） |
| 硬依赖 | `fabric.mod.json` 的 `depends` 里写死了 `create`（本次要改成软依赖） |

### 1.2 内容清单

**方块 / 方块实体（14 个方块，13 个 BE）**

| 方块 | 说明 |
| --- | --- |
| `analytical_engine_controller` / `_alu` / `_kinetic_interface` | 分析机（未完成玩法，只有方块与 BE） |
| `iota_reader` / `iota_writer` | 与 Create 传送带/机械臂交互的 iota 读写机 |
| `iota_wire_connector` | iota 线缆连接器 |
| `iota_pack_observer` | iota 包侦测器（红石） |
| `circle_inputer` / `circle_outputer` | 法术环输入/输出（**无模型、无贴图、无配方**） |
| `circle_amplifier` | 法术环增幅器（消耗 SU 扩大作用范围，含未启用的 sender/receiver 方案） |
| `train_gate` / `train_gate_frame` / `train_gate_core` | 列车传送门（依赖 Create 列车与传送门内部实现） |
| `board` | 排版板（放置活字、复制/粘贴/删除图案） |
| `amethyst_lamp` | 0–15 级亮度/红石灯，扳手或 CapsLock 键控制（16 帧贴图） |

**物品**

`iota_wire_reel`、`empty_reel`、`iota_wire_connector`、`iota_wire_connector_tech`、`iota_wire`、`raw_soul`、
`unfinished_train_gate_frame`、`square`、`printed_paper`、`paper_reel`、`media_bucket`、
以及 9 个 `thought_key/*`（Ponder 教学钥匙，由 Registrate 注册）。

**流体**

`media`（still/flowing + 桶 + 流体方块）、`soul`（still/flowing，无桶）。

**图案（10 个已注册 Action）**

`kelvin_heat_gambit`、`kelvin_super_heat_gambit`、`kelvin_time_purification`、`kelvin_type_purification`、
`pascal_time_purification`、`kinetic_rotation_purification`、`kinetic_network_stress_all_purification`、
`kinetic_network_stress_used_purification`（注册名 `kinetic_network_stress_purification`）、
`kinetic_stress_purification`、`train_gate_set_up_gambit`。
另有 mishap 文本（需要 Blaze Burner / Train Gate Core）。

**Create 集成点**

| 功能 | 上游实现 | 26.1 / Create-Fly 对应 |
| --- | --- | --- |
| 应力值 | `BlockStressValues.registerProvider` | `BlockStressValues.IMPACTS / CAPACITIES / RPM`（SimpleRegistry） |
| 显示板数据源 | `ModDisplayBehaviours` + 4 个 DisplaySource | `DisplaySource.BY_BLOCK / BY_BLOCK_ENTITY` |
| 注液器行为 | `FillingBySpoutMixin` 注入 `FillingBySpout` | `BlockSpoutingBehaviour` 注册表（API，无需 mixin） |
| 搅拌/加热配方 | `SoulMixingRecipes` + data JSON | `create:mixing` codec 格式 + `HeatCondition` |
| 序列组装 | `sequenced_assembly/train_gate_frame.json` | `SequencedAssemblyItem` + 新配方格式 |
| 列车传送门 | `ModPortalTracks` + `TrainGateTeleporter` | `com.zurrtum.create.api.contraption.train.PortalTrackProvider` |
| Ponder 教学 | 4 个场景 + `thought_key` 物品 | `com.zurrtum.create.client.foundation.ponder.*`（Ponder 库被重打包） |
| 传送带标签 | `data/create/tags/items/upright_on_belt.json` | `data/create/tags/item/...`（单数路径） |

**客户端**

BoardRenderer、CircleAmplifierRenderer、IotaWireConnectorRenderer、IotaWireRenderer、IotaWriterRenderer、
紫水晶灯控制界面（`ControlAmethystLampScreen` + CapsLock 键位）、ItemRendererMixin、
Patchouli 自定义页 `AllPagesWithPattern`、Patchouli 文本解析 mixin。

**数据与资源**

7 个 Patchouli 条目（amethyst_lamp / board / circle_amplifier / liquid_media / liquid_soul / operations / ponder）、
10 条 mixing、1 条 cutting、1 条 stone_cutting、1 条 sequenced_assembly、1 条 brainsweep、若干 crafting、
3 个 Ponder 场景 NBT、en_us/zh_cn 各约 16–20KB 文本。

### 1.3 上游资源缺口（"自行完善贴图"的依据）

按 `registrations → assets` 对照，上游至少缺：

- `circle_inputer`、`circle_outputer`：无 blockstate、无方块模型、无物品模型、无贴图、无掉落表、无配方；
- `iota_reader`、`iota_writer`：有 blockstate/模型，但**没有方块贴图**（模型引用不存在的 PNG）；
- `media` 流体方块：无 blockstate/模型；`media`、`soul` 均无 still/flow 贴图；
- `unfinished_train_gate_frame`：无物品模型、无贴图；
- `iota_wire_connector` 方块：无 blockstate、无掉落表；
- `analytical_engine_kinetic_interface`：需核对模型引用的贴图是否落在 controller/alu 上；
- 所有 `thought_key` 只有 item 模型与贴图，没有配方（上游未完成）。

---

## 2. 目标环境 / 本项目现状

### 2.1 本项目

| 项 | 值 |
| --- | --- |
| MC | 26.1.2，`fabric.loom.disableObfuscation=true`（官方名，无需 remap） |
| Java / Kotlin | Java 25 / Kotlin 2.4.21 |
| 模块 | `settings.gradle` 仅 `Common` + `Fabric`（Forge 不参与构建） |
| 注册入口 | [FabricHexInitializer.kt](file:///home/tongsun9/HexMod/Fabric/src/main/java/at/petrak/hexcasting/fabric/FabricHexInitializer.kt) 的 `initRegistries()`，通过 `bind(registry)` 写入 `HexIotaTypes / HexActions / HexArithmetics / ...` |
| 内置附属先例 | MoreIotas（Common 放逻辑 + Fabric 放初始化和平台实现）、HexCassettes、HexDebug |
| 指南书 | 自研 Patchouli 替代（[GuideBook.kt](file:///home/tongsun9/HexMod/Common/src/main/java/at/petrak/hexcasting/client/guidebook/GuideBook.kt)），扫描所有命名空间的 `patchouli_books/thehexbook/<lang>/entries`，支持 advancement 解锁 |
| Create 兼容 | [CreateCompatRecipeProvider.java](file:///home/tongsun9/HexMod/Fabric/src/main/java/at/petrak/hexcasting/fabric/datagen/CreateCompatRecipeProvider.java)，手写 26.1 codec JSON + `fabric:all_mods_loaded` 条件 |
| 其它 | Paucal 已 vendored；Patchouli 不可用；`HexItemModelDefinitionProvider` 负责 item model definition |

### 2.2 Create-Fly（本地实例 `create-fly-26.1.2-6.0.9-4.jar`）

- mod id：`create`（名字 "Create Fly"，MC `>=26.1- <26.2-`，Java >=25）；
- 包名：`com.zurrtum.create`（**不是** `com.simibubi.create`）；
- 是 Create 6.x 的 Fabric 移植：有 `com.zurrtum.create.api.*` 稳定 API，**没有 Registrate**；
- 可用的关键 API：
  - `api/registry/SimpleRegistry`（含 `Multi` 变体）；
  - `api/stress/BlockStressValues`（`IMPACTS/CAPACITIES/RPM`）；
  - `api/behaviour/display/DisplaySource`（`BY_BLOCK`、`BY_BLOCK_ENTITY`）；
  - `api/behaviour/spouting/BlockSpoutingBehaviour`；
  - `api/contraption/train/PortalTrackProvider`；
  - `content/processing/{recipe,basin,sequenced}`（`SizedIngredient`、`ProcessingOutput`、`HeatCondition`、`SequencedAssemblyItem`）；
  - `content/kinetics/base/{KineticBlockEntity, SmartBlockEntity, GeneratingKineticBlockEntity, ...}`；
  - `AllBlocks`、`AllBlockEntityTypes`、`AllDisplaySources` 等注册表常量；
  - `client/foundation/ponder/*` 与 `client/ponder/*`（Ponder 库被重打包到这里）；
- **无 jar-in-jar**，`CompatibilityLevel` 为 JAVA_25；
- 上游使用的 PortingLib `FluidStack` 在 Create-Fly 中位置需核对（大概率是 `com.zurrtum.create.infrastructure.fluids.FluidStack`）。

---

## 3. 差异映射表（逐项处理方式）

| 上游 | 目标 | 处理 |
| --- | --- | --- |
| Yarn 名（`net.minecraft.item.ItemStack` 等） | Mojang 官方名（`net.minecraft.world.item.ItemStack`） | 全量改名；建议先整体替换再编译修 |
| `com.simibubi.create.*` | `com.zurrtum.create.*` | 全量替换 + 按 API 重写 |
| `CreateRegistrate` / Registrate `ItemEntry` | 无 | `Registry.register(BuiltInRegistries.ITEM, ...)` + datagen 模型 |
| `FabricItemSettings` / `FabricBlockSettings` | `Item.Properties` / `BlockBehaviour.Properties` | 直接换 |
| `AbstractBlock.Settings.copy(AllBlocks.BRASS_BLOCK.get())` | `BlockBehaviour.Properties.ofFullCopy(AllBlocks.BRASS_BLOCK)` | 注意 `AllBlocks` 是 Create-Fly 的常量 |
| `FluidStack`（PortingLib） | Create-Fly 流体栈 + Fabric Transfer API | 核对类名后替换；配方/注液逻辑重写 |
| `BlockStressValues.registerProvider(...)` | `BlockStressValues.IMPACTS.register(block, supplier)` | 直接注册 |
| `DisplaySource` 注册 | `DisplaySource.BY_BLOCK.register(...)` | 直接注册 |
| `FillingBySpoutMixin` | `BlockSpoutingBehaviour` | **去掉 mixin**，改为 API 注册 |
| `ModPortalTracks` | `PortalTrackProvider` | 重写；风险最高 |
| Ponder `SceneBuilder` / `PonderIndex` | `com.zurrtum.create.client.foundation.ponder.CreateSceneBuilder` | 重写，或降级为指南书文本 |
| Patchouli 书 + 自定义页 | 本项目 GuideBook（JSON 驱动） | 条目转静态 JSON；自定义页改静态文本/复用已有页型 |
| `recipes/`、`tags/items`、`loot_tables/` | `recipe/`、`tags/item`、`loot_table/` | 路径改单数 + codec 格式 |
| 1.20 配方 `ingredients`/`processingTime`/`item` | 26.1 `ingredient`/`processing_time`/`{id,count,chance}` | 参考现有 `CreateCompatRecipeProvider` |
| 创造模式物品栏 `FabricItemGroup` | `CreativeModeTab` + `CreativeModeTabEvents` | 见本项目 `HexCreativeTabs` 写法 |
| `fabric.mod.json` 硬依赖 `create` | 软依赖 | `depends` 不写 create，改 `recommends` |

---

## 4. 架构：软依赖与"只有加载 Create 才激活"

### 4.1 代码放置

```
Fabric/
  src/main/java/pub/pigeon/yggdyy/hexcreating/          ← 上游包名原样保留（便于对照/合并上游）
    HexCreatingCompat.java                              ← L0：唯一可被主入口触碰的类（无 Create import）
    compat/
      HexCreatingRegistration.java                      ← L1：方块/物品/BE/流体/物品栏
      HexCreatingActions.java                           ← L1：图案 / mishap / iota
      HexCreatingBlockEntities.java
      HexCreatingFluids.java
      HexCreatingCreativeTab.java
      HexCreatingNetworking.java
      create/                                           ← L2：Create-Fly 专属（应力/显示源/注液/传送门/Ponder）
    client/
      HexCreatingClientCompat.java                      ← L0：客户端门控入口（无 Create import）
      HexCreatingClientRegistration.java                ← L1：BER / 屏幕 / 键位
    mixin/HexCreatingMixinPlugin.java                   ← 只依赖 FabricLoader，不触碰 Create
    mixin/...                                           ← 条件 mixin（尽量少）
  src/main/resources/hexcreating.mixins.json            ← plugin 指向上面的 MixinPlugin
  src/main/resources/assets/hexcreating/...
  src/main/resources/assets/hexcreating/patchouli_books/...（指南书条目）
  src/main/resources/data/hexcreating/...
Fabric/src/generated/resources/...                      ← datagen 输出（配方/掉落/标签）
```

- **不放 `Common`**：`Common` 不能依赖 Create，否则整个项目被硬绑定。唯一例外见 4.5（GuideBook 增加条件字段，属于纯 JSON 解析，不依赖 Create）。
- 包名保持 `pub.pigeon.yggdyy.hexcreating`，与上游一一对应，后续上游更新可以逐文件 diff。

### 4.2 三层类隔离（关键）

```java
// L0：只有 isModLoaded 判断，不 import 任何 Create 类
public final class HexCreatingCompat {
    private static boolean initialized;
    public static void init() {
        if (initialized) return;
        initialized = true;
        HexCreatingRegistration.init();   // 第一次触碰才加载 L1/L2
    }
}
```

```kotlin
// FabricHexInitializer.onInitialize() / initRegistries() 末尾
if (IXplatAbstractions.INSTANCE.isModPresent("create")) {
    HexCreatingCompat.init()
}
```

- `HexCreatingRegistration` 里的字段用 `init()` 内注册（**不要**用 Java `static final` 字段，否则类加载即注册、且顺序不可控）。
- Kotlin 的 `object` 是懒加载，天然满足；Java 类一律用显式 `init()`。
- 客户端同理：`FabricHexClientInitializer` 里 `if (isModPresent("create")) HexCreatingClientCompat.init()`。
- 服务端/客户端都要判断；不要只在客户端判断（方块/物品是双侧注册）。
- 建议额外校验：`FabricLoader.getInstance().getModContainer("create")` 存在且版本在 `[6.0.0, 7.0.0)`，避免误判同名模组（可选）。

### 4.3 Mixin 条件启用

`create` 缺失时，带 Create 目标的 mixin 会因为类找不到直接崩。必须新增独立配置 + `IMixinConfigPlugin`：

```json
// hexcreating.mixins.json
{
  "required": true,
  "minVersion": "0.8",
  "package": "pub.pigeon.yggdyy.hexcreating.mixin",
  "compatibilityLevel": "JAVA_25",
  "plugin": "pub.pigeon.yggdyy.hexcreating.mixin.HexCreatingMixinPlugin",
  "mixins": [],
  "client": [],
  "injectors": { "defaultRequire": 1 }
}
```

```java
public class HexCreatingMixinPlugin implements IMixinConfigPlugin {
    private static final boolean CREATE = FabricLoader.getInstance().isModLoaded("create");
    public boolean shouldApplyMixin(String target, String mixin) { return CREATE; }
    // 其余方法空实现
}
```

- 目标原则：**能不用 mixin 就不用**。注液器行为改 `BlockSpoutingBehaviour`；应力/显示源都有 API；`CircleExecutionStateInjector` 上游本来就是注释状态，直接丢弃；`BookTextParserAccessor`、`TextWindowElementAccessor`、`SceneBuilderAccessor` 依赖 Patchouli/Ponder 内部，按 Phase 4 处理或删除。
- 若必须保留 `BlazeBurnerBlockEntityAccessor`，它也放进这个条件配置。

### 4.4 数据包条件

所有 `data/` 下引用 Create 或需要 Create 才有意义的文件，必须带：

```json
"fabric:load_conditions": [
  { "condition": "fabric:all_mods_loaded", "values": ["create"] }
]
```

覆盖范围：`recipe/**`（mixing/cutting/sequenced_assembly/brainsweep）、`tags/item|block|fluid` 中引用 Create 的部分。
本项目 `HexFabricConditionsBuilder` / `ResourceConditions.allModsLoaded("create")` 已具备，直接复用。

注意：**datagen 不能 import Create-Fly**（否则 `runDatagen` 在没有 Create 的 dev 环境会崩），继续用现有手写 `JsonObject` 的方式生成。

### 4.5 指南书 / 资源条件

- 条目放在 `assets/hexcreating/patchouli_books/thehexbook/en_us/entries/*.json`；`GuideBook` 会扫描所有命名空间，命名空间不影响分类（分类用 `category` 字段的相对 id）。
- 给条目加自定义字段（本项目自己解析，不需要 Patchouli）：

```json
{ "hexcasting:mod_loaded": "create", ... }
```

- 在 `GuideBook.load()` 的 entry/category 循环里，用 `IXplatAbstractions.INSTANCE.isModPresent(...)` 过滤；同时 `GuideBook.invalidate()` 保持可用。
- 页型：上游 `operations.json` 用的是自定义动态页（`AllPagesWithPattern`）。本项目的 GuideBook 是 JSON 驱动的，**改为静态页面**（每个图案一条 entry，复用本项目已有的图案渲染页型/组件），不要移植 Patchouli 自定义页逻辑。
- Patchouli 不可用，所有 `patchouli:` 类型必须落在 GuideBook 已支持的集合内；`spotlight`、`crafting`、`text` 建议保留，其他类型先降级为 text。

### 4.6 网络与客户端

- 紫水晶灯控制界面、iota 线缆卷轴需要 C2S 包：用 `PayloadTypeRegistry.playC2S()` + `ServerPlayNetworking`，**在 compat init 内注册**；Create 缺席时不注册。
- BER：`BlockEntityRendererRegistry`（Fabric）在客户端门控里注册；若 26.1 API 改为 `BlockRenderLayerMap` / `BlockEntityRendererFactories`，按本项目现有写法对齐。
- 键位（CapsLock）只在 compat 客户端初始化时注册，避免 Create 缺席时出现无主键位。

### 4.7 `fabric.mod.json`

```json
"depends": { "minecraft": "...", "fabricloader": "...", "fabric-api": "*", "fabric-language-kotlin": "...", "hexcasting": "..." },
"recommends": { "create": ">=6.0.0 <7.0.0" }
```

不要新增独立 entrypoint（会绕过 `isModPresent` 判断）；统一挂在现有 `FabricHexInitializer` / `FabricHexClientInitializer` 里。

---

## 5. 构建改动（Phase 0 的最小集合）

1. 把 `create-fly-26.1.2-6.0.9-4.jar` 放进 `libs/`（已被 `settings.gradle` 的 flatDir 引用，但建议用显式路径）。
2. `Fabric/build.gradle`：

```gradle
dependencies {
    modCompileOnly files(rootProject.file("libs/create-fly-26.1.2-6.0.9-4.jar"))
    // 仅开发环境联调；不要打进产物
    modLocalRuntime files(rootProject.file("libs/create-fly-26.1.2-6.0.9-4.jar"))
}
```

- 因为 `fabric.loom.disableObfuscation=true` 且 Create-Fly 以官方名发布，**理论上不需要 remap**；
  若 Loom 仍尝试 remap，改用 `compileOnly files(...)`（不加 `mod` 前缀）并实测。
- 产物 `jar` 不包含 Create-Fly；CI/无 Create 环境照常 `./gradlew build`。
- 冒烟验证：写一个空的 `HexCreatingCompat` + 一个 `import com.zurrtum.create.AllBlocks` 的测试方法，确认编译/运行正常后立刻删除该方法。

---

## 6. 内容迁移映射

### 6.1 方块 / 方块实体 / 物品

| 上游类 | 迁移目标 | 要点 |
| --- | --- | --- |
| `ModBlocks.kt` | `HexCreatingBlocks`（L1） | 普通 `Registry.register`；`withItem=false` 的方块保持无物品 |
| `ModBlockEntities.java` | `HexCreatingBlockEntities`（L1） | `BlockEntityType.Builder.of(...)`（26.1 命名）；BE 构造签名 `(BlockPos, BlockState)` |
| `ModItems.java` | `HexCreatingItems`（L1） | 去掉 `CreateRegistrate`；9 个 thought_key 改为普通 Item + datagen 模型 |
| `ModFluids.java` | `HexCreatingFluids`（L1） | 用 Fabric `Fluid`/`FlowableFluid` 等价 API；media 桶保持 `BucketItem` |
| `HexcreatingMain` | 拆成 `HexCreatingCompat` + `HexCreatingRegistration` | 不再有独立 ModInitializer |

BE 细节：
- `BoardBlockEntity`、`CircleAmplifierBlockEntity`、`IotaReader/WriterBlockEntity` 继承 `BlockEntity`，不继承 Create 的 `SmartBlockEntity`（除分析机/接口类），降低 API 依赖。
- 分析机三件套若要用 Create 的动能接口，需继承 `KineticBlockEntity`；若上游玩法未完成，Phase 2 只保留方块外观，动能逻辑延后。
- `IotaWireConnectorBlockEntity` 连接逻辑纯 HexCasting + 自己维护的图结构，可整体复用。

### 6.2 图案与 HexCasting 侧

- 注册位置：`initRegistries()` 中 `HexActions.register(bind(...))` 之后追加 `HexCreatingActions.register(bind(IXplatAbstractions.INSTANCE.actionRegistry))`。
- 上游用 `HexActions.REGISTRY` 直接 `Registry.register`；本项目在初始化阶段用 `bind` 更安全（避免冻结后写入）。
- 每个 action 的 `HexPattern.fromAngles(...)`、`HexDir` 原样保留，ID 保持 `hexcreating:<name>`。
- lang 键：`hexcasting.action.hexcreating:<name>`，文本从上游 `en_us/zh_cn` 直接迁。
- mishap：`mishaps.need_blaze_burner*`、`mishaps.need_train_gate_core` 需在 `HexCreatingMishaps` 里注册（若本项目 mishap 是枚举/注册表，按注册表方式加）。
- iota 类型：上游 `ModIotaTypes` 基本为空（367B），**无需迁移**；如后续需要 `IotaPack` 作为 iota，再单独评估（`IotaPack` 属于线缆网络传输格式，不一定需要注册成 IotaType）。
- 媒体消耗：图案里 `MediaConstants`/`dust` 换算与上游一致，迁移时逐条核对。

### 6.3 流体

- `MediaFluid`/`SoulFluid`：仍有 still/flowing 两个 `Fluid` 子类；26.1 的 `Fluid`/`FlowingFluid` API 与 1.20 差异主要在 `getBucket`/`getTickDelay`/`beforeDestroyingBlock` 等，按当前 26.1 签名实现。
- media 流体方块注册为 `LiquidBlock`，需要 blockstate + 模型；soul 不注册方块（上游也没有）。
- `data/minecraft/tags/fluid/water.json` 上游把 media 加进 water 标签（会与水互溶/可被海带等处理），建议**不要**迁移此行为，除非确认玩法需要；如果要，加条件。
- 介质容器/注液交互：见 6.4。

### 6.4 Create-Fly 集成

| 项 | 实现方式 |
| --- | --- |
| 应力 | `HexCreatingStress.register()`：对 circle_amplifier / analytical engine 等方块调用 `BlockStressValues.IMPACTS.register(block, () -> x)`、需要发电的用 `CAPACITIES/RPM`（上游 `ModStressValueProvider` 的数值逐条搬） |
| 显示源 | 4 个 source 分别 `DisplaySource.BY_BLOCK.register(block, source)` 或 `BY_BLOCK_ENTITY.register(type, source)`；`provideText/transferData` 签名按 Create 6 重写 |
| 注液器（介质充能、剥离心智） | 不再 mixin：实现 `BlockSpoutingBehaviour` 并注册；内部逻辑复用上游 `ChargingBySpout` / `FlayMindBySpout`；`FluidStack` 换 Create-Fly 类型 |
| 搅拌 | `create:mixing` 配方 10 条 + `HeatCondition`（`HEATED`/`SUPERHEATED`），由 datagen 写 JSON；原料从 amethyst 各种形态 → media，raw_soul + media → soul |
| 切割/石切 | `create:cutting`、`create:stone_cutting` 各 1 条 |
| 序列组装 | `create:sequenced_assembly`：train_gate_frame + 中间物 `unfinished_train_gate_frame`；上游 JSON 按 26.1 codec 重写 |
| 列车传送门 | 实现 `PortalTrackProvider` + `TrainGateTeleporter`；先做"传送门方块能成形、列车能穿过"的最小闭环，再做可视化 |
| Ponder | Phase 4：`thought_key` 物品 + `CreateSceneBuilder` 场景；若 Ponder 重打包 API 不稳定，则**降级**为指南书图文条目，删除 thought_key 或改为纯装饰 |
| 标签 | `data/create/tags/item/upright_on_belt.json` 加入 `hexcreating:square`、`printed_paper` 等 |

### 6.5 指南书

- 7 个条目全部迁到 `assets/hexcreating/patchouli_books/thehexbook/en_us/entries/`，`category` 保持 `hexcasting:items` / `hexcasting:patterns` 等（本项目分类 id 为相对 id）。
- `advancement` 保持 `hexcasting:root`，解锁逻辑复用现有 `isEntryUnlocked`。
- `operations.json` 的动态图案页改为**静态页**：每个新图案一条 entry（名称 + 图案 + 说明），或合并成一条多页 entry（推荐后者：1 个 entry、10 页，减少目录噪音）。
- 中文文本同步迁 `zh_cn`；GuideBook 目前写死 `LANG = "en_us"`，若需要中文书名/文本，需另开任务（不在本方案内，但要注意上游 zh_cn 文本会被忽略）。

### 6.6 客户端

| 上游 | 迁移 |
| --- | --- |
| `HexcreatingClient` | `HexCreatingClientCompat`（L0）+ `HexCreatingClientRegistration`（L1） |
| 5 个 BER | 门控注册；`BoardRenderer`/`IotaWireRenderer` 依赖 Create 的 `SuperByteBuffer` 需换 Create-Fly 的渲染工具或原生 `VertexConsumer` |
| `ModKeyBindings` + `ControlAmethystLampKey` | 26.1 键位 API；只在 compat 客户端初始化时注册 |
| `ControlAmethystLampScreen` | 26.1 Screen + `Menu` 或纯 C2S 包；注意 26.1 GUI 渲染 API 变动 |
| `HexcreatingRenderLayers` | `RenderType`/`ItemBlockRenderTypes` 注册 |
| `ItemRendererMixin` | 若只为 thought_key 的悬停提示，改用 `Item` 的 `appendHoverText`/客户端 tooltip 事件，**删除 mixin** |

---

## 7. 贴图与模型自制清单

### 7.1 缺失文件（必须新做）

| 类别 | 文件 | 说明 |
| --- | --- | --- |
| 方块 | `assets/hexcreating/blockstates/circle_inputer.json`、`models/block/circle_inputer.json`、`models/item/circle_inputer.json`、`textures/block/circle_inputer.png` | 参照 Create 的 andesite/brass 机壳 + 紫水晶观察窗 |
| 方块 | `circle_outputer` 同上 4 件 | 与 inputer 成对，方向/发光可区分 |
| 方块 | `textures/block/iota_reader.png`、`textures/block/iota_writer.png` | 模型已引用，补 PNG 即可 |
| 方块 | `blockstates/media.json`、`models/block/media.json` | 流体方块 |
| 流体 | `textures/block/media_still.png`、`media_flow.png` | 紫色半透明，参照 water |
| 流体 | `textures/block/soul_still.png`、`soul_flow.png` | 青白色灵魂质感 |
| 物品 | `textures/item/unfinished_train_gate_frame.png`、`models/item/unfinished_train_gate_frame.json` | 半成品框架 |
| 方块 | `blockstates/iota_wire_connector.json` | 上游只有 item 模型 |
| 数据 | `loot_table/blocks/{circle_inputer,circle_outputer,iota_wire_connector}.json` | 掉落自身 |
| 配方 | circle_inputer / circle_outputer / thought_key / iota_wire 等 | 上游缺失，建议补最小合成链（创造模式可拿） |

### 7.2 需要核对/可能重绘

- `analytical_engine_kinetic_interface` 的 1/2 号模型引用贴图（可能复用 controller，需确认不出现紫黑格）；
- `amethyst_lamp/0..15` 16 帧与 `train_gate.png.mcmeta` 动画帧数是否匹配；
- `thought_key/*` 9 张现有贴图风格统一性（当前像是临时占位）；
- `slate_casing.png` / `slate_gearbox.png` 仅作为风格参考，不注册。

### 7.3 风格规范（自制贴图统一标准）

- 尺寸：方块/物品 16×16；灯 16 帧；流体 still/flow 各 16×16（可 32×32 以贴近 Create 的流体）。
- 调色板：Create 的黄铜 `#C8A24A`、安山岩 `#6E6E6E`、深板岩 `#3A3A3F` + HexCasting 紫水晶 `#9A5BD6`/`#D9A6FF`；
  能量/介质用高饱和紫，灵魂用青白 `#BFE9E4`。
- 不用抗锯齿、不用渐变透明噪点；机壳类保留 Create 的描边与铆钉节奏。
- 每张图必须被至少一个模型/物品定义引用；提交前用脚本（见 8.3）扫一遍 `models/**` 的 `textures` 引用，确认无缺文件。

---

## 8. 数据生成与格式迁移

### 8.1 路径与格式

| 1.20 | 26.1 |
| --- | --- |
| `data/<ns>/recipes/x.json` | `data/<ns>/recipe/x.json` |
| `data/<ns>/loot_tables/blocks/x.json` | `data/<ns>/loot_table/blocks/x.json` |
| `data/<ns>/tags/items/x.json` | `data/<ns>/tags/item/x.json` |
| `data/<ns>/tags/blocks/x.json` | `data/<ns>/tags/block/x.json` |
| `data/<ns>/tags/fluids/x.json` | `data/<ns>/tags/fluid/x.json` |
| `ingredients: [...]` + `processingTime` | `ingredient`（单个字符串/对象）+ `processing_time` |
| 结果 `{ "item": "..." }` | `{ "id": "...", "count": n, "chance": p }` |

### 8.2 新增 datagen provider（Fabric）

- `HexCreatingRecipeProvider`：mixing / cutting / stone_cutting / sequenced_assembly / brainsweep + 全部 `fabric:all_mods_loaded("create")` 条件；
  直接复用 [CreateCompatRecipeProvider.java](file:///home/tongsun9/HexMod/Fabric/src/main/java/at/petrak/hexcasting/fabric/datagen/CreateCompatRecipeProvider.java) 的 `JsonObject` 手写方式；
  建议把 `withCreateCondition` / `crushing` 这类工具抽成包内静态方法，避免两份。
- `HexCreatingLootProvider`：3 个缺失掉落表（可用现有 `HexLootTables` 风格）。
- `HexCreatingBlockStateProvider`：若手写 blockstate 太零碎，可用 datagen 生成；复杂模型（board、analytical engine）建议保留手写 JSON。
- `HexCreatingItemModelProvider`：9 个 thought_key + 新物品的 item model definition（对齐 `HexItemModelDefinitionProvider`）。
- `HexCreatingTagProvider`：`create:upright_on_belt`、`minecraft:mineable/*`、自己的 `hexcreating` 标签。
- 注册到 [HexFabricDataGenerators.java](file:///home/tongsun9/HexMod/Fabric/src/main/java/at/petrak/hexcasting/fabric/datagen/HexFabricDataGenerators.java)。

### 8.3 自检脚本（建议放 `tools/` 或临时脚本）

- `check_assets.py`：遍历 `assets/hexcreating/models/**/*.json`，抽取 `textures`/`parent`，校验目标 PNG/JSON 存在；
  遍历 blockstates 校验 `model` 存在；遍历 `models/item` 校验物品贴图存在。
- `check_recipes.py`：校验每个 `recipe/*.json` 的 `type` 是否在 `create:*`/`minecraft:*`/`hexcasting:*` 已知列表内，
  且带 `fabric:load_conditions`（仅 Create 类型）。

---

## 9. 分阶段实施计划

### Phase 0：地基与门控（必须先做）

1. `libs/` 放 Create-Fly jar，`Fabric/build.gradle` 加 `compileOnly`/`localRuntime`。
2. 新建 `HexCreatingCompat`（L0）+ `HexCreatingRegistration`（空实现）+ `HexCreatingClientCompat`。
3. 新建 `hexcreating.mixins.json` + `HexCreatingMixinPlugin`，注册到 `fabric.mod.json`。
4. 在 `FabricHexInitializer` / `FabricHexClientInitializer` 加门控调用。
5. 在 `HexFabricDataGenerators` 注册空的 compat provider（后续填充）。
6. **验收**：`./gradlew build` 通过；无 Create 实例启动无异常、无日志、无注册；有 Create 实例启动正常；mixin 配置在两种情况下都加载成功。

### Phase 1：基础内容与贴图（可独立验收）

1. 注册 14 个方块、13 个 BE、全部物品、media/soul 流体、创造模式物品栏。
2. 补齐第 7 节资源；blockstate/model/item definition/loot/lang。
3. 指南书 7 个条目 + 条件字段 + 静态化 operations。
4. **验收**：创造模式能拿到全部物品；每个方块可放置、掉落正确、贴图无紫黑格；指南书条目在加载 Create 时出现、缺席时隐藏；`/give hexcreating:*` 正常。

### Phase 2：HexCasting 玩法

1. 10 个图案 + mishap + lang。
2. iota 线缆网络（connector/reel/wire/pack observer + 渲染 + 卷轴选中逻辑）。
3. 排版板 + 活字 + 纸卷 + 打印纸（board 的交互/红石档位）。
4. iota reader/writer + 分析机方块（先只做外观与基础交互）。
5. 紫水晶灯逻辑 + 控制界面 + 键位 + C2S 包。
6. brainsweep 配方（train_gate_core）。
7. **验收**：单人/专用服务器各跑一遍；每个图案可施放、消耗正确；线缆可连接并传输；排版板可复制/粘贴图案；灯可调光并输出红石。

### Phase 3：Create-Fly 机制

1. 应力值注册。
2. 4 个 DisplaySource。
3. `BlockSpoutingBehaviour`（介质充能 / 剥离心智），删除 `FillingBySpoutMixin`。
4. mixing / cutting / stone_cutting / sequenced_assembly 配方 + 条件。
5. 动能接口类 BE（circle_amplifier 的 SU 消耗、分析机）。
6. 列车传送门（train gate 三件套 + `PortalTrackProvider`）。
7. **验收**：注液器能充能物品；搅拌产出 media/soul；增幅器按 SU 扩大范围；列车可穿过传送门；无 Create 时以上全部不存在且无报错。

### Phase 4：Ponder、收尾与兼容

1. Ponder 场景（4 个）+ thought_key；若 API 不可用则降级为指南书图文。
2. JEI/EMI 信息（可选）。
3. 与现有 `CreateCompatRecipeProvider` 合并/去重（紫水晶粉碎三条保持现状）。
4. 配置项（总开关 / 各子功能开关，默认开）。
5. 文档：README、`doc/` 说明、上游署名与 MIT 声明。
6. 性能检查：Create 缺席时不得注册 ticker/BE；有无空转的 renderer。

---

## 10. 风险与备选

| 风险 | 影响 | 处理 / 备选 |
| --- | --- | --- |
| Create 0.5.1 → Create-Fly 6.0.9 API 大改 | 高：BE/传送门/Ponder 需重写 | 先做 Phase 0 冒烟；逐类重写；train gate 允许延后 |
| 无 Registrate | 中：9 个物品 + 模型 | 原生注册 + datagen，工作量可控 |
| PortingLib `FluidStack` 缺失 | 中：注液/搅拌 | 用 Create-Fly 流体栈；实在不行走 Fabric Transfer API + `FluidVariant` |
| Ponder 库被重打包、API 不稳 | 中：教学场景 | 降级为指南书文本/图片，thought_key 改纯装饰或删除 |
| 列车传送门依赖内部实现 | 高：可能卡很久 | 只做 `PortalTrackProvider` 公开 API；不成就砍掉该子功能，保留方块外观 |
| 上游资源不完整 | 中：视觉缺口 | 第 7 节清单逐项补齐；提交前跑资源自检脚本 |
| 条件加载类泄漏（ClassNotFound） | 高：Create 缺席直接崩 | 三层隔离 + 静态检查（无 Create 环境下启动/跑测试）+ 禁止 static 初始化 |
| GuideBook 不支持自定义页型 | 中：operations 条目 | 静态化；必要时给 GuideBook 加一个图案页型（独立任务） |
| 26.1 客户端渲染/GUI API 变动 | 中：5 个 BER + 1 个 Screen | 用本项目现有渲染工具/先例；BER 简化到原生 `VertexConsumer` |
| 存档/网络兼容 | 低：新内容无旧存档 | 保持 `hexcreating:*` id；不迁移旧 NBT 结构 |
| 许可 | 低 | MIT，保留 `LICENSE` 与作者署名；README 注明来源 |

---

## 11. 待确认问题

1. **Ponder 是否必须**：如果只是为了教学，是否接受"指南书图文 + 删除 thought_key"的降级方案？（影响 Phase 4 工作量）
2. **train gate 是否首版必须**：这是唯一大量依赖 Create 内部列的玩法，建议首版只保留方块/配方，传送逻辑延后。
3. **贴图范围**：只补缺失文件，还是把现有占位贴图（thought_key、灯、分析机）也统一重绘？建议先补缺失，统一风格放 Phase 4。
4. **是否输出独立 jar**：本方案默认并入 `hexcasting` jar；如果希望玩家可单独开关，需要拆子模块并产出 `hexcreating` 附属 jar（工作量和发布流程都会变）。
5. **中文指南书**：本项目 GuideBook 目前固定 `en_us`，上游有完整中文文本；是否需要同时改 GuideBook 支持 `zh_cn`？（独立任务）

---

## 12. 迁移顺序速查（按文件）

| 顺序 | 文件/目录 | 动作 |
| --- | --- | --- |
| 1 | `Fabric/build.gradle`、`libs/` | 加 Create-Fly 编译依赖 |
| 2 | `pub/pigeon/yggdyy/hexcreating/HexCreatingCompat.java` 等 L0 | 门控入口 |
| 3 | `hexcreating.mixins.json`、`HexCreatingMixinPlugin`、`fabric.mod.json` | 条件 mixin |
| 4 | `HexCreatingBlocks/Items/BlockEntities/Fluids/CreativeTab` | 注册 |
| 5 | `assets/hexcreating/**`、`data/hexcreating/**` | 资源与贴图 |
| 6 | `HexCreatingActions` + lang | 图案 |
| 7 | `blocks/board`、`blocks/iotawire`、`blocks/amethyst_lamp` | 玩法核心 |
| 8 | `create/**`（stress/display/spout/recipes/portal/ponder） | Create 集成 |
| 9 | datagen providers + `HexFabricDataGenerators` | 配方/掉落/标签/模型 |
| 10 | `GuideBook.kt`（条件字段）+ 条目 JSON | 指南书 |
| 11 | 测试与文档 | 验收 |
