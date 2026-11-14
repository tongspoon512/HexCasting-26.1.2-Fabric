# 分析机（Analytical Engine）玩法设计

> 文档状态：设计稿 v1.0 · 仅设计，不含实现
> 目标环境：Minecraft 26.1.2 / Fabric / Create-Fly 6.0.9-4（mod id `create`）
> 前置文档：[hexcreating-migration-plan.md](hexcreating-migration-plan.md)
> 适用范围：`hexcreating` 分析机三件套、iota 网络、法术环输入/输出、Create 应力/显示源/Ponder

---

## 0. 摘要

- **定位**：分析机是「咒法自动化的可编程执行核心」——把玩家手里的法术程序搬到工厂里，用**动力、媒质、时间**三种成本持续执行。
- **结构**：控制单元（程序与调度）+ 计算单元（吞吐/栈/程序长度）+ 动力单元（Create 动能接入）组成一个**集群**，相邻即可成形，无固定多方块形状。
- **程序**：程序带就是 `List<Iota>`，复用咒法学「托特之策略」式的逐条执行语义；在图案 iota 与数据 iota 之外，增加 6 条机器指令（跳转/条件跳转/等待/读入/输出/停机）。
- **输入输出**：走现有 iota 网络（`IIotaTerminalHost` / `IotaPack`），用 `IotaPack.headInfo` 承载**频道**，不引入新的网络协议。
- **安全默认**：断料、阻塞、欠应力一律**暂停而不是摧毁**；所有事故可复位；单 tick 指令数硬上限 32，队列上限 64。
- **内容**：新增 3 个思维之匙与 3 个 Ponder 场景、4 个指南书条目、5 个显示源、1 套配方链；全部沿用现有 Create 条件加载机制。

当前代码里三件套只有方块外观与一个 8.0 SU 的应力占位（[HexCreatingCreateIntegration.java](../Fabric/src/main/java/pub/pigeon/yggdyy/hexcreating/HexCreatingCreateIntegration.java#L15-L22)），没有方块实体。本方案补全的正是这块空白。

---

## 1. 设计目标与边界

### 1.1 设计目标

| 编号 | 目标 | 判定方式 |
| --- | --- | --- |
| G1 | 让咒法成为工厂的一环：可编程、可观测、可组合 | 玩家能搭出「输入信号 → 执行法术 → 输出信号/物品」的闭环 |
| G2 | 保留玩家的不可替代性 | 程序编写、图案解锁、故障复位都必须由玩家完成 |
| G3 | 不新增独立货币/材料体系 | 只消耗媒质、热能/应力、既有零件（黄铜、板岩、紫水晶） |
| G4 | 每个反馈都对应一个既有 Create 概念 | 应力（护目镜）、显示板（Display Link）、红石（比较器/观察者） |
| G5 | 可观测、可中止、可恢复 | 任意时刻能看清状态；拆除/断料不丢存档、不刷资源 |

### 1.2 明确不做的事

1. **不替代玩家施法**：不提供「一键全自动通关」的通用法术执行器，需要玩家上下文的图案（背包、视线、主手、经验）禁止自动化。
2. **不免费**：任何图案执行都必须付动力与媒质，且比玩家手施更贵（见 §8.3）。
3. **不绕过图案解锁**：程序带带作者签名，按作者已解锁图案校验。
4. **不直接生成物品**：产出必须经过 Create 物流或世界掉落，分析机本身没有「凭空出物」的槽位。
5. **不做列车传送**：与既有约定一致，浮辇之门仍只做方块/结构。
6. **不做跨维度/离线执行**：只在已加载区块内 tick，卸载即暂停。

### 1.3 系统关系图

```text
玩家 ──(写程序/解锁/复位)──▶ 纸卷 / 打印纸 ──▶ 分析机集群 ◀── Create 动能网络
                                              │
                    iota 网络 ◀──────────────▶│ (端口 + 频道)
                                              │
                       ┌──────────────────────┼──────────────────────┐
                       ▼                      ▼                      ▼
                 图案世界效果          显示源(翻转显示器)      红石(比较器/脉冲)
```

---

## 2. 核心定位

### 2.1 一句话

> **分析机 = 一座需要供能、供料、供程序的「咒法计算厂」。它不创造法术，它让法术可以被流水线调用。**

### 2.2 三层语义

| 层 | 含义 | 玩家感知 |
| --- | --- | --- |
| 方块层 | 三个功能块拼成的可扩展集群 | 「我该放几个计算单元？」「动力够不够？」 |
| 系统层 | iota 数据流的处理器：输入 → 执行 → 输出 | 「我的工厂在按信号做事」 |
| 世界层 | 咒法程序离开玩家身体的载体 | 「我给这台机器写了一段咒术」 |

### 2.3 作用边界

| 能做 | 不能做 |
| --- | --- |
| 执行以坐标/实体/数值为参数的图案 | 执行需要玩家背包、主手、视线、经验、玩家实体的图案 |
| 读取 iota 网络、把结果发回网络 | 直接改写其他分析机的内存/程序 |
| 消耗媒质与应力持续运行 | 免费待机发电、无限递归、跨维度调用 |
| 通过图案产生世界副作用（放置、伤害、探测） | 绕过图案解锁、绕过事故（mishap）判定 |
| 被红石/显示板/护目镜观测与控制 | 被 Create 移动装置（contraption）整体搬运（与浮辇之门同策略） |

**边界判定规则（可数据驱动）**：新增标签 `hexcreating:unautomatable_patterns`，默认收录所有 `CastingEnvironment` 需要「施法者上下文」的图案；执行到这类图案时进入 `PAUSED(LOCKED_PATTERN)`，并指向指南书说明。第三方整合包可直接改标签，无需改代码。

---

## 3. 系统总览

### 3.1 集群结构

- **成形规则**：控制单元与任意计算单元/动力单元在 **6 邻面**相连即属于同一集群；按 BFS 连通，不要求固定形状。
- **唯一性**：
  - 每个集群**有且仅有一个**控制单元（出现第二个则两者都进入 `ERROR(CORE_CONFLICT)`，拆掉一个即恢复）。
  - 动力单元 **0..1 个**（0 个 = 无动力，只能手动单步调试，用于教学）。
  - 计算单元 **0..7 个**。
- **破坏与重建**：拆掉任一部件不破坏程序与栈数据（数据只存在控制单元 NBT 中），重建后从断点继续。
- **不可被 contraption 包含**：注册时排除，避免移动装置把运行中的集群撕成两半。

### 3.2 部件职责

| 部件 | 职责 | 数据 | 应力影响 |
| --- | --- | --- | --- |
| 控制单元 `analytical_engine_controller` | 程序槽、输入队列、栈、状态机、端口、GUI | 持久化全部运行时状态 | 0 |
| 计算单元 `analytical_engine_alu` | +1 指令/tick、+16 栈深、+128 程序长度 | 无状态（可热插拔） | +4.0 SU / 个 |
| 动力单元 `analytical_engine_kinetic_interface` | 接入 Create 动能，提供转速与应力 | 转速缓存、过热值 | 基准 8.0 SU |

> 实现要点：应力注册表中动力单元默认值为 8.0（已存在），集群总应力由动力单元的 `KineticBlockEntity#calculateStressApplied` 覆写为 `8.0 + 4.0 × aluCount`，避免为 ALU 单独注册应力（ALU 不在动能网络上）。

### 3.3 资源流

| 资源 | 来源 | 消耗点 | 断供表现 |
| --- | --- | --- | --- |
| 旋转动力（RPM） | Create 动能网络 | 时钟倍频 | `PAUSED(NO_POWER)` |
| 应力（SU） | Create 动力源 | 集群负载 | 降频 → `ERROR(UNDERSTRESS)` |
| 媒质（粉 / 液态） | 控制单元 3 个媒质槽；相邻流体容器 | 每条图案指令 | `PAUSED(NO_MEDIA)` |
| 程序 | 纸卷 / 打印纸 / 网络写入 | 执行 | `IDLE(NO_PROGRAM)` |
| iota 数据 | 输入端口 | `read` 指令 | `read` 处阻塞 |
| 输出空间 | 输出端口 / 队列 | `emit` 指令 | `PAUSED(OUT_BLOCKED)` |

---

## 4. 核心规则

### 4.1 程序模型

**程序带 = `List<Iota>`**，与咒法学栈模型完全一致，玩家已有的 iota 认知可直接迁移。

元素分三类：

| 类别 | 判定 | 执行语义 | 成本 |
| --- | --- | --- | --- |
| 图案 iota（`PatternIota`） | 咒法原生类型 | 立即结算该图案，等价于在法术环中被执行 | 图案自身 `mediaCost` + 0.01 粉 |
| 数据 iota（数字/向量/实体/列表/字符串…） | 其他所有 iota | 压入数据栈 | 0 |
| 机器指令 iota（`hexcreating:instruction`） | 新增 iota 类型 | 见下表 | 0 |

**机器指令集（v1 固定 6 条，足够表达分支与循环）**

| 指令 | 参数 | 行为 |
| --- | --- | --- |
| `jmp` | 目标索引 | 无条件跳转 |
| `jz` | 目标索引 | 弹出布尔；为 `false` 时跳转 |
| `wait` | tick 数（1..12000） | 暂停指定 tick 后继续，超上限自动 `PAUSED` |
| `read` | 无 | 从输入队列取一个 `IotaPack` 压栈；空则阻塞（可被新包唤醒） |
| `emit` | 无 | 弹出栈顶打包发往输出端口；无下游则阻塞（背压） |
| `halt` | 无 | 结束本轮；按「自动重跑」开关决定 `IDLE` 或重新开始 |

> 设计取舍：不引入独立的「跳转图案」，避免污染咒法学图案表；指令以 iota 形式存在，天然可被剪接台（HexDebug）、`/hexParse`、iota 网络与纸卷承载，序列化零成本。

**程序来源三条路径**（复用现有物品，不新增载体）：

| 路径 | 载体 | 容量 | 特点 |
| --- | --- | --- | --- |
| P1 排版板打印 | 排版板 4×4 活字 → 打印纸 `printed_paper` | 16 条 | 一次性、只读；适合小宏/教学 |
| P2 纸卷 | `paper_reel`（耐久 1024） | ≤1024 条 | 可反复写入；长程序主力 |
| P3 网络写入 | `iota_writer` → 控制器程序端口 | ≤1024 条 | 由另一台分析机或玩家法术动态生成；高风险高上限 |

**容量与校验**：

- 可执行长度 `L = 128 + 128 × aluCount`（0..7 个 ALU → 128..1024），超出部分忽略并在终端提示。
- 数据栈深 `S = 64 + 16 × aluCount`（64..176）。
- 写入时校验：元素必须是合法 iota；跳转目标必须在 `[0, L)`；`wait` 参数在范围内；作者图案已解锁。任一失败 → 拒绝写入，物品**原样退回**（不吞物品）。
- 作者签名：写入时把玩家 UUID 写入程序带 NBT。运行时按该作者的解锁集合校验；作者离线/不存在时按服务器配置 `analyticalEngine.allowGlobalUnlocks`（默认 `false`）处理，无法校验则 `PAUSED(LOCKED_PATTERN)`。

### 4.2 执行循环

每个服务端 tick：

```text
if state != RUNNING: return
budget = computeBudget(aluCount, rpm)          // 见 §8.1，上限 32
accumulator += budget
while accumulator >= 1 and steps < 32:
    accumulator -= 1
    ins = program[pc]
    switch ins:
      data     -> push(ins); pc++
      pattern  -> checkMedia(); cast(ins); pc++     // 失败 → 状态转移，不吞异常
      opcode   -> applyOp(ins)
    if budget exhausted / state changed: break
flush()     // 仅在状态或可见数据变化时 setChanged + sync
```

约定：

- 所有异常（mishap、栈溢出、媒质不足）都转成**状态 + 错误码**，绝不抛到 tick 循环外。
- 图案执行产生的世界副作用与玩家施法一致（同一套 `SpellAction`/`RangedOperativeAction` 语义），保证「为什么它打怪有效、放方块有效」可被玩家复用既有知识解释。
- 栈溢出等价于咒法学的「栈溢出」事故：`ERROR(STACK_OVERFLOW)`，本轮栈清空但程序保留。
- 每 tick 最多出网 8 个 `IotaPack`，超出进队列；队列 64 满则 `PAUSED(OUT_BLOCKED)`。

### 4.3 端口与 iota 网络

控制单元 6 个面各有一个端口，用**扳手**循环切换（与法术环输入/输出器的交互完全一致，降低学习成本）：

| 模式 | 行为 |
| --- | --- |
| `DISABLED` | 不参与网络（默认） |
| `INPUT` | 接收同频道 `IotaPack` 进输入队列 |
| `OUTPUT` | `emit` 时向该面发送 |
| `PROGRAM` | 接收 `IotaPack` 作为新程序（仅 `iota_writer` 或玩家法术可写） |
| `REDSTONE` | 该面作为启动信号输入（上升沿/电平） |

- **频道**：`IotaPack.headInfo` 中的 `channel:<name>` 字符串；端口可命名（默认 `default`）。发送时写入，接收时过滤，不匹配直接丢弃且不计入去重表。
- **中继**：连接器（`iota_wire_connector`）组成子网，同一子网内广播；转发时把自身路径标记写入 `headInfo` 防环，接受方沿用现有 `IotaTerminal` 的 `lastAcceptTime + receivedId` **每 tick 去重**。
  - 现状提示：当前 `ConnectorBlockEntity` 只是缓冲，没有中继实现；这是本方案在 M2 必须补的缺口（见 §7.4）。
- **背压**：输入队列 64、输出队列 64；两侧任意一侧满即暂停，不在网络里无限堆积。
- **优先级**：同一 tick 内多个包到达按到达顺序入队；`read` 取队首。

### 4.4 动力、时钟与过热

- 转速只决定**倍频**，不决定单条图案的耗时；`rpm < 32` 时用累加器按比例慢跑。
- 转速上限 256；`> 256` 时吞吐仍为 8 倍频，但开始累积**过热值**：每 tick `+ (rpm-256)/256`，过热值 ≥ 20 触发 `ERROR(OVERSPEED)` 并停机；自然冷却每 tick `-0.5`。
- 应力不足：转速瞬间被压低到 32 并给出黄色警示；持续 10 秒（200 tick）仍不足 → `ERROR(UNDERSTRESS)`，需扳手复位（不损毁方块）。
- 动力中断：连续 100 tick 无转速 → `PAUSED(NO_POWER)`；恢复后自动续跑（不重置程序指针）。

### 4.5 媒质与成本

- **成本单位**：沿用咒法学 `MediaConstants.DUST_UNIT`（1 紫水晶粉 = `DUST_UNIT`）。成本表只用「粉」表达，换算交给既有 `MediaConstants`。
- **收费点**：只有真正执行图案 iota 时收费；数据/指令/待机/暂停均不收费。
- **附加机械损耗**：每条图案指令额外 `0.01 粉`，用于表达「机器不如人手精细」，也是唯一可调的平衡旋钮。
- **供料顺序**：内部缓冲（上限 2000 粉）→ 3 个媒质槽的紫水晶粉/充能紫水晶 → 相邻流体容器中的液态媒质（1 mB = 1 粉，沿用指南书既有表述）。每 tick 最多抽取 64 粉，避免瞬间抽干管网。
- **断料**：在收费点暂停，比较器输出 0，显示源显示 `NO_MEDIA`；补料后自动继续，不回滚已执行的指令。

### 4.6 触发、暂停与停机

**启动（可配置，默认「红石上升沿」）**

| 方式 | 操作 | 说明 |
| --- | --- | --- |
| 红石 | `REDSTONE` 端口收到上升沿 | 扳手可切换为电平保持 |
| 手动 | 潜行 + 空手右键 | 启停切换 |
| 单步 | 潜行 + 扳手右键 | 每点一次执行 1 条指令（调试/教学） |
| 事件 | 程序首个可执行指令为 `read` 且收到匹配频道包 | 「事件驱动」模式，无需红石 |

**暂停条件**：无动力、无媒质、输出阻塞、`wait` 未到、红石断开（电平模式）、程序端口正在写入、玩家手动暂停。

**停机条件**：`halt`、程序跑完且「自动重跑」关闭、`ERROR`、控制单元被破坏、动力中断超过 100 tick、区块卸载。

### 4.7 反馈机制

| 通道 | 内容 | 实现载体 |
| --- | --- | --- |
| 方块外观 | `status` 四态（idle/ready/running/error）+ `paused` 细分；控制单元面板灯为青/紫/黄/红 | 新增 blockstate 属性（需同步 blockstate JSON） |
| 护目镜 | 吞吐、应力占用、媒质余量、当前指令 | Create goggles overlay（BE 提供数据） |
| 终端界面 | 程序指针、栈快照（前 8 项）、媒质、吞吐、状态、错误码、端口配置 | 右键控制单元打开；只读为主 + 介质放入/取出 + 启停/单步 |
| 显示源 | 5 个新 `DisplaySource`：状态、程序指针、栈顶、媒质余量、错误码 | `CreateRegistries.DISPLAY_SOURCE`（沿用现有 4 个显示源的注册方式） |
| 红石 | 比较器可扳手切换：栈深 / 媒质百分比 / 程序进度；`halt` 输出 2 tick 的 15 强脉冲；错误持续 15 | `getAnalogOutputSignal` + 邻居更新 |
| 音效 | 启动齿轮声、每条图案的施法音、错误蜂鸣 | `HexSounds` + Create 机械音 |

所有反馈都遵循「**一眼可判断**」：只有四种主色，错误码只在终端/显示源出现，不在世界里刷屏。

### 4.8 产出规则

1. **数据产出**：`emit` 打包为 `IotaPack`，`id` 自动生成，`headInfo` 写入端口频道。
2. **世界产出**：由图案本身决定；若图案产生物品掉落，掉落在控制单元「输出面」上方 0.5 格（避免卡进机器）。
3. **物理产出**：分析机没有物品输出槽，必须由漏斗/传送带/机械臂从掉落点收集——保持 Create 物流的核心地位。
4. **节流**：每 tick ≤8 包、每包 ≤1 个 iota（整栈需用列表 iota 显式打包），防止网络风暴。
5. **去重**：同一 `IotaPack.id` 在同一 tick 只被同一终端接受一次（现有 `IotaTerminal` 机制），跨 tick 允许重放（符合「信号」语义）。

---

## 5. 运行状态机

| 状态 | 含义 | 可见表现 |
| --- | --- | --- |
| `IDLE` | 无程序 | 灯灭；终端提示「需要程序带」 |
| `READY` | 有合法程序 + 有动力 + 有媒质，等待启动 | 青灯慢闪 |
| `RUNNING` | 正在执行 | 紫灯常亮 + 齿轮音 |
| `PAUSED` | 可恢复的等待（细分原因） | 黄灯常亮；比较器输出 0 |
| `ERROR` | 需要玩家介入 | 红灯常亮；护目镜显示错误码 |

**转移表**

| 当前 | 事件 | 下一 | 附加动作 |
| --- | --- | --- | --- |
| IDLE | 装入合法程序 | READY | 校验容量/解锁 |
| READY | 启动信号 | RUNNING | 记录启动 tick |
| READY | 无动力 / 无媒质 | READY | 保持灯色提示 |
| RUNNING | `wait` / 断料 / 阻塞 / 断红石 | PAUSED | 记录暂停原因与 tick |
| PAUSED | 条件恢复且自动续跑开启 | RUNNING | 不清栈、不回滚 |
| RUNNING | `halt` 或跑完 | IDLE | 若「自动重跑」开启则回 READY |
| RUNNING | 栈溢出 / 事故 / 非法跳转 / 锁定图案 | ERROR | 记录错误码；栈清空（非法跳转除外） |
| ERROR | 潜行 + 扳手复位 | READY | 清零过热、指针归零、保留程序 |
| 任意 | 拆掉控制单元 | — | 数据随方块掉落物保存 |
| 任意 | 集群结构变化 | 重新计算 | ALU 变化立即影响预算/栈/长度，不重置程序 |

---

## 6. 用户路径

### 6.1 阶段划分

| 阶段 | 目标 | 关键动作 | 前置 |
| --- | --- | --- | --- |
| T0 认知 | 理解「程序 = 图案序列」 | 读指南书新条目；用排版板把 3 个图案排成一行 | 已接触咒法学与排版板 |
| T1 首台 | 让一个程序跑起来 | 控制单元 + 动力单元 → 插纸卷 → 32 RPM → 红石启动 | 黄铜板、电子管、板岩 |
| T2 联网 | 让机器与工厂对话 | 接 iota 网络、命名频道、接显示源与比较器 | iota 连接器 + 读写器 |
| T3 工厂化 | 多台级联 | 多 ALU 提升吞吐；A 机输出作 B 机输入；液态媒质管线 | 稳定媒质产线 |

### 6.2 首次 10 分钟脚本（验收用）

1. 在排版板 4×4 上放 3 个活字：数字之精思 → 加法 → （留空），打印得到打印纸。
2. 把打印纸插入控制单元程序槽，接动力单元与 32 RPM 手摇/水车。
3. 红石脉冲启动，护目镜看到「1 op/tick、栈 1 项」。
4. 把 `OUTPUT` 端口对准 `iota_writer`，写入石板；用 Create 翻转显示器 + 「石板上的图案」显示源观察结果。
5. 比较器观察栈深度随程序变化——玩家第一次「看见」法术的执行过程。

### 6.3 三个典型场景

- **自动农场**：程序 = `read`（收到成熟信号）→ 一系列破坏/放置图案 → `emit`（把收获数量发回网络）→ `halt`。输入来自侦测器 + iota 写入器，输出驱动传送带。
- **工厂信号**：A 机计算库存/配方比例，输出数字 iota；B 机的 `jz` 据此启停机械臂。全流程无红石，只有频道。
- **级联计算**：A 机把长计算拆成子程序，结果作为 B 机的程序参数；需要两台机器与两个频道，防止自激（同频道自环会被去重表挡住并提示）。

### 6.4 交互约定

| 操作 | 结果 |
| --- | --- |
| 空手右键 | 打开终端 |
| 潜行 + 空手右键 | 启动/暂停 |
| 扳手右键 | 旋转朝向 |
| 潜行 + 扳手右键 | 端口模式循环 / ERROR 复位（按端口是否配置区分） |
| 手持纸卷右键 | 直接插入程序槽 |
| 手持法杖右键 | 不施法，提示「分析机不接受手动施法」 |
| 护目镜 | 数值浮层 |

---

## 7. 流程节点与分支异常

### 7.1 节点清单

| 节点 | 内容 | 失败分支 |
| --- | --- | --- |
| N1 成形 | 扫描 6 邻面组成集群 | 无控制器/双控制器/ALU 超 7 |
| N2 装程序 | 校验类型、容量、解锁、跳转 | `INVALID_PROGRAM` / `LOCKED_PATTERN` |
| N3 配端口 | 6 面模式与频道名 | 同面冲突 → 允许但警告 |
| N4 接动力 | 读转速、算应力 | `NO_POWER` / `UNDERSTRESS` / `OVERSPEED` |
| N5 供料 | 抽粉/液态媒质到缓冲 | `NO_MEDIA` |
| N6 启动 | 红石/手动/事件 | 条件不满足 → 保持 READY |
| N7 执行 | 逐条结算 | 见 §7.2 |
| N8 读入 | `read` 取包 | 队列空 → 阻塞；频道不匹配 → 丢弃 |
| N9 输出 | `emit` 发包 | 对端拒收/队列满 → 背压暂停 |
| N10 副作用 | 图案世界效果 | mishap → `ERROR(MISHAP:<id>)` |
| N11 结束 | `halt`/跑完 | 自动重跑开关 |
| N12 维护 | 复位、过热、拆装 | 数据保留 |

### 7.2 异常与错误码

| 错误码 | 触发点 | 行为 | 恢复 |
| --- | --- | --- | --- |
| `E00 NO_POWER` | N4/N7 | 暂停，保留指针 | 恢复转速自动续跑 |
| `E01 UNDERSTRESS` | N4 | 先降频 10 秒，仍不足则停机 | 扳手复位 |
| `E02 OVERSPEED` | N4 | 过热累积到阈值后停机 | 自然冷却 + 复位 |
| `E03 NO_MEDIA` | N7 | 暂停，比较器 0 | 补料自动续跑 |
| `E04 OUT_BLOCKED` | N9 | 暂停（背压） | 下游腾空自动续跑 |
| `E05 INVALID_PROGRAM` | N2 | 拒收，物品退回 | 修程序 |
| `E06 STACK_OVERFLOW` | N7 | 停机，清栈 | 复位；检查循环是否缺 `halt` |
| `E07 STACK_UNDERFLOW` | N7 | 停机 | 复位；常见于 `emit` 前栈为空 |
| `E08 LOCKED_PATTERN` | N2/N7 | 暂停并指路 | 解锁后复位 |
| `E09 MISHAP:<id>` | N10 | 停机，保留程序与指针 | 复位；按 mishap 提示修正参数 |
| `E10 BAD_JUMP` | N2/N7 | 拒写或停机 | 修正跳转目标 |
| `E11 CORE_CONFLICT` | N1 | 双控制器均停机 | 拆掉一个 |
| `E12 PORT_CONFLICT` | N3 | 允许但警告（同面多用途按优先级） | 改配置 |

### 7.3 特殊分支

- **区块卸载**：指针、栈、队列、缓冲全部持久化；重新加载后进入 `PAUSED(UNLOADED)`，需要启动信号才继续——避免「离线刷资源」。
- **运行中拆 ALU**：立即按新容量裁剪程序长度与预算；若指针越界则 `ERROR(INVALID_PROGRAM)`，不崩档。
- **运行中换程序**：程序端口写入时先暂停，写入成功后指针归零；旧程序退回玩家背包。
- **多人**：终端同一时刻只允许一名玩家编辑；运行不受影响。
- **自环**：同频道输出直接回到输入 → 去重表在一个 tick 内挡住，连续 3 tick 自环则 `PAUSED(PORT_CONFLICT)` 并提示。

---

## 8. 兼容性与数值体系

### 8.1 吞吐与容量

预算公式：

```text
倍频 M = clamp(floor(RPM / 32), 0, 8)
每 tick 指令预算 = min((1 + ALU) × M, 32)
RPM < 32 时：累加器 += (1 + ALU) × RPM / 32，取整执行
```

| ALU | 应力 | 32 RPM | 64 RPM | 128 RPM | 256 RPM | 栈深 | 程序长度 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 0 | 8 SU | 1 | 2 | 4 | 8 | 64 | 128 |
| 1 | 12 SU | 2 | 4 | 8 | 16 | 80 | 256 |
| 2 | 16 SU | 3 | 6 | 12 | 24 | 96 | 384 |
| 3 | 20 SU | 4 | 8 | 16 | 32* | 112 | 512 |
| 5 | 28 SU | 6 | 12 | 24 | 32* | 144 | 768 |
| 7 | 36 SU | 8 | 16 | 32* | 32* | 176 | 1024 |

\* 受 32 指令/tick 硬上限截断。

### 8.2 成本

| 项目 | 数值 | 依据 |
| --- | --- | --- |
| 数据/控制指令 | 0 | 不触碰世界 |
| 图案指令 | 图案注册的媒质成本 + 0.01 粉 | 复用 `ActionRegistryEntry` 的 `mediaCost` |
| 待机/暂停 | 0 | 避免「挂机扣费」挫败感 |
| 单 tick 抽料上限 | 64 粉 | 保护管网 |
| 内部缓冲 | 2000 粉（约 2 桶） | 跨 tick 平滑 |
| 输入/输出队列 | 64 / 64 | 背压阈值 |
| 过热阈值 | 20 点（每点约 1 秒超速） | 可配置 |

### 8.3 与现有系统的平衡对比

| 维度 | 玩家施法 | 法术环 | 分析机 |
| --- | --- | --- | --- |
| 成本 | 仅媒质 | 仅媒质 | 媒质 ×(1+0.01/条) + 应力 |
| 频率 | 手动 | 每次红石/触发 | 持续，受 RPM 限制 |
| 需要玩家 | 是 | 否（但需预先布置） | 否（但需程序与解锁） |
| 精度 | 高（可临场判断） | 中 | 低（只能按程序走） |
| 适用 | 探索/战斗 | 单次批量 | 长期产线 |

结论：分析机的优势是**持续与可组合**，代价是**前期投入、媒质溢价、程序不自适应**；不会让玩家放弃手动施法。

### 8.4 架构约束对照

| 约束 | 方案遵守方式 |
| --- | --- |
| Create 软依赖、零注册 | 全部新类只在 `HexCreatingCompat.isCreateLoaded()` 为真时注册；指令 iota 类型也走同一门控 |
| 26.1 官方名 | 使用 Mojang 映射名（`Identifier`、`setId`、`BlockStateProperties`） |
| 数据目录命名 | `data/hexcreating/recipe/`、`loot_table/`、`tags/item/`、`tags/block/` |
| 物品模型 | 新增物品必须提供 `assets/hexcreating/items/<id>.json` |
| Ponder | 必须实现；3 个思维之匙 + 3 个场景，未装 Create 不注册 |
| 贴图 | 只补缺口，不重绘既有贴图；新贴图沿用 `slate_casing` 风格 |
| 列车门 | 不涉及传送逻辑 |
| 性能 | 单 tick ≤32 指令、≤8 包、网络 BFS 每 tick 缓存一次 |

### 8.5 代码映射

| 设计元素 | 现有支撑 | 需新增 |
| --- | --- | --- |
| 集群成形 | [HexCreatingBlocks.java](../Fabric/src/main/java/pub/pigeon/yggdyy/hexcreating/blocks/HexCreatingBlocks.java#L70-L77)、`AnalyticalEngineBlock` | 控制/ALU/动力三个 BE 与 `AnalyticalEngineCore` |
| 应力 | [HexCreatingCreateIntegration.java](../Fabric/src/main/java/pub/pigeon/yggdyy/hexcreating/HexCreatingCreateIntegration.java#L15-L22) 的 8.0 占位；[CircleAmplifierBlock.java](../Fabric/src/main/java/pub/pigeon/yggdyy/hexcreating/blocks/circle_amplifier/CircleAmplifierBlock.java#L66-L82) 的转速→效果先例 | 动力 BE 继承 `KineticBlockEntity`，覆写 `calculateStressApplied` |
| 程序介质 | `PAPER_REEL` / `PRINTED_PAPER` / `SquareItem` / `BoardBlockEntity` | `IProgramMedium` 实现与作者签名 |
| iota 网络 | [api/iotanet](../Fabric/src/main/java/pub/pigeon/yggdyy/hexcreating/api/iotanet)、`IotaPack.headInfo`、`IotaTerminal` 去重 | 连接器中继 + 频道过滤 |
| 端口 UX | [CircleInputerBlock.java](../Fabric/src/main/java/pub/pigeon/yggdyy/hexcreating/blocks/circle_inputer/CircleInputerBlock.java#L91-L117) 的扳手循环 | `PortMode` 属性/配置与终端页 |
| 显示源 | [HexCreatingDisplaySources.java](../Fabric/src/main/java/pub/pigeon/yggdyy/hexcreating/create/HexCreatingDisplaySources.java) | 5 个新 source |
| 媒介值 | `MediaConstants.DUST_UNIT`；[HexCreatingActions.kt](../Fabric/src/main/java/pub/pigeon/yggdyy/hexcreating/cast/HexCreatingActions.kt#L91-L117) 的按 tick 计费先例 | 成本累加器与缓冲 |
| 持久化 | `HexBlockEntity#saveModData/loadModData` + `sync()` | 运行时快照序列化 |
| Ponder | [HexCreatingPonder.java](../Fabric/src/main/java/pub/pigeon/yggdyy/hexcreating/client/HexCreatingPonder.java) | 3 个 thought_key + 场景 |
| 指南书 | `GuideBook.loadConditionsMet` 条件过滤 | 4 个条目 + lang |

---

## 9. 实现接口预留

以下仅为**包内契约**（不承诺公共 API 稳定性），用于让后续实现与第三方扩展有明确挂点。

```java
// 集群部件：ALU 提供容量，动力单元提供转速与应力
public interface IAnalyticalEnginePart {
    @Nullable AnalyticalEngineCore core();
    int extraOpBudget();
    int extraStackDepth();
    int extraProgramLength();
    double extraStress();
}
```

```java
// 程序介质：纸卷、打印纸、网络写入共用
public interface IProgramMedium {
    List<Iota> program(ServerLevel level);
    int capacity();
    boolean writable();
    Optional<UUID> author();
}
```

```java
// 机器指令：以 iota 形式存在，可被网络/纸卷/剪接台承载
public record InstructionIota(OpCode code, Optional<Iota> arg) implements Iota {
    public enum OpCode { JMP, JZ, WAIT, READ, EMIT, HALT }
}
```

```java
// 端口
public enum PortMode { DISABLED, INPUT, OUTPUT, PROGRAM, REDSTONE }

// 单步结果：运行时只返回这四种，不抛异常到 tick 外
public sealed interface StepResult {
    record Continue() implements StepResult {}
    record Yield(PauseReason reason) implements StepResult {}
    record Halt() implements StepResult {}
    record Error(ErrorCode code) implements StepResult {}
}

// 运行时
public final class AnalyticalEngineRuntime {
    public StepResult tick(ServerLevel level, AnalyticalEngineCore core);
    public void push(Iota iota);
    public Optional<Iota> pop();
    public void reset();
}
```

**扩展点**

1. `IInstructionHandler`：按 `Identifier` 注册自定义指令（第三方可加 `loop`、`call`）。
2. 标签 `hexcreating:unautomatable_patterns`：控制哪些图案禁止自动化。
3. 频道命名规范 `channel:<name>`：任何实现 `IIotaTerminalHost` 的方块都能加入，无需改分析机。
4. `DisplaySource` 既有机制：第三方可为自己方块接显示板。
5. 服务器配置：`allowGlobalUnlocks`、`maxAluPerCluster`、`overheatThreshold`、`autoRestart`。

---

## 10. 分阶段落地

| 阶段 | 范围 | 验收标准 |
| --- | --- | --- |
| M0 骨架 | 三个 BE、集群扫描、应力计算、状态机空转、终端只读页 | 放入/拆掉 ALU 时吞吐与应力数值正确；无 Create 时不注册任何类 |
| M1 闭环 | 程序介质（纸卷/打印纸）、图案执行、媒质消耗、启动/暂停/停机 | 首台分析机能连续执行含 10 个图案的程序；断料/断动力可恢复；重启存档指针保留 |
| M2 联网 | 端口与频道、连接器中继、输入/输出队列、背压、去重 | 两台分析机跨 20 格经连接器通信；同 tick 重复包只收一次；队列满时暂停而非丢数据 |
| M3 内容 | 6 条指令、3 个 Ponder、4 个指南书条目、5 个显示源、配方与掉落表、护目镜浮层 | 新玩家按 §6.2 脚本 10 分钟内跑通；Ponder 可完整播放；`./gradlew build -x test` 通过 |

---

## 11. 风险与待决

| 风险 | 影响 | 缓解 |
| --- | --- | --- |
| 自动施法刷资源 | 经济失衡 | 只允许「世界可寻址」图案；成本溢价；单 tick 硬上限；标签可禁 |
| 图案执行环境仿真复杂 | 实现成本高 | 复用 `CastingEnvironment` 的服务器实现，只替换「施法者」为「机器实体」；M1 先支持无施法者图案 |
| 每 tick BFS 扫描 | 卡顿 | 每 20 tick 或结构变化时重建一次拓扑缓存；发送时只走缓存 |
| 程序解锁语义 | 多人服争议 | 作者签名 + 默认拒绝全局解锁；配置可放宽 |
| 与剪接台/HexParse 的编辑冲突 | 数据损坏 | 程序带写入走同一 `IotaHolderItem` 接口，长度与类型校验前置 |
| 旧存档 | 兼容性 | 新 blockstate 属性有默认值；BE 缺失数据时按 `IDLE` 处理 |

待决问题（需要策划确认）：是否允许分析机执行「伤害实体」类图案（会影响 PVP 服）；自动重跑默认值；频道是否需要注册表（目前用自由字符串）。

---

## 12. 附录

### 12.1 术语

| 术语 | 含义 |
| --- | --- |
| 集群 | 6 邻面连通的控制/计算/动力单元集合 |
| 程序带 | 有序 iota 列表，分析机按指针逐条执行 |
| 频道 | `IotaPack.headInfo` 中的 `channel:<name>` 标签 |
| 倍频 | 由 RPM 决定的每 tick 指令倍率 |
| 机械损耗 | 每条图案指令额外 0.01 粉 |

### 12.2 错误码速查

`E00` 无动力 · `E01` 欠应力 · `E02` 超速过热 · `E03` 无媒质 · `E04` 输出阻塞 · `E05` 非法程序 · `E06` 栈溢出 · `E07` 栈下溢 · `E08` 图案未解锁 · `E09` 图案事故 · `E10` 非法跳转 · `E11` 控制器冲突 · `E12` 端口冲突
