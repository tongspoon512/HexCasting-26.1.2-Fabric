# 新增法术与 TODO 规划

> 状态：规划中，尚未实现。
> 范围：`todo.txt` 中的“繁殖动物 / 加速幼崽 / 让悦灵兴奋”和“睡到下一次日出或月出 / 临时清除怪物 AI”，以及新增的“生成粒子”“生成贝塞尔粒子”“方块下落”。
> 目标：先确定玩法、输入、媒质、失误、实现位置和验收标准，再分阶段写代码，避免边写边改。

## 1. 设计原则

沿用本模组“让一切成为可能，而不是让其更简单”的理念。新法术只提供**能力**，不替玩家跳过过程：

- 粒子法术是表达工具：用于信号、标记、轨迹预演和装置艺术，本身不造成伤害、不改变方块。
- 贝塞尔粒子只负责“把点连成可预期的曲线”，不自动索敌、不自动触发效果。
- 方块下落是物理搬运：遵守重力、能被水/爆炸/活塞影响，不是凭空生成方块。
- 生命法术只调节既有生物的状态，不召唤生物、不直接产出掉落物。
- 长眠是“暂时让渡时间与 AI”，不是永久奴役；退出世界前必须恢复所有被修改的状态。

## 2. 总览

| 法术 | 注册 id | 类别 | 输入（压栈顺序，右为栈顶） | 媒质 | 分析机 |
| --- | --- | --- | --- | --- | --- |
| 生成粒子 | `hexcasting:spawn_particles` | 普通法术 | `vec 位置, vec 散布, num 数量, str 粒子` | 1 粉 + 每 8 个粒子 1 粉 | 可用 |
| 生成贝塞尔粒子 | `hexcasting:spawn_bezier_particles` | 普通法术 | `vec P0, vec P1, vec P2, vec P3, num 采样数, str 粒子` | 1 粉 + 每 8 个采样点 1 粉 | 可用 |
| 方块下落 | `hexcasting:fall_block` | 普通法术 | `vec 位置` | 1 粉 | 可用 |
| 生命的节拍·繁殖 | `hexcasting:life/breed` | 普通法术 | `entity 动物` | 2 粉 | 可用 |
| 生命的节拍·成长 | `hexcasting:life/grow` | 普通法术 | `entity 幼崽, num 秒` | 1 粉 + 每 10 秒 1 粉 | 可用 |
| 生命的节拍·兴奋 | `hexcasting:life/excite` | 普通法术 | `entity 悦灵, num 秒` | 1 粉 + 每 10 秒 1 粉 | 可用 |
| 长眠 | `hexcasting:sleep` | 伟大法术 | `player 目标, vec 床, num 模式` | 2 充能紫水晶 | 不可用 |

“分析机可用”指法术不依赖 `env.castingEntity`，可以由分析机或法术环施放；需要施法者的法术会在 `execute` 开头抛 `MishapBadCaster`。

## 3. 图案候选

以下签名已按 `HexPattern.fromAngles` 的“线段不重叠、不反向折返”规则验证，并与当前源码中 241 个 `fromAngles` 签名逐一比对，未发现重复。起始方向只是建议，实际形状由起始方向决定；实现前仍应看一次服务器启动日志，因为 `PatternRegistryManifest` 遇到相同角度序列时只会警告并覆盖，不会抛错。

| 法术 | 角度签名 | 起始方向 |
| --- | --- | --- |
| 生成粒子 | `wdwaeqawqeeedaqae` | `EAST` |
| 生成贝塞尔粒子 | `aewqqddewdaqadqadqq` | `EAST` |
| 方块下落 | `dewqaedwawadadawa` | `EAST` |
| 生命的节拍·繁殖 | `qaqewawawdadweeqq` | `EAST` |
| 生命的节拍·成长 | `dedawewaawwqaaqad` | `EAST` |
| 生命的节拍·兴奋 | `wqedwwdwewddeadeaaqwqaw` | `EAST` |
| 长眠 | `eaqwawqdawwdwdqwe` | `EAST` |

## 4. 通用基础设施

### 4.1 粒子描述 `ParticleSpec`

iota 体系中没有“粒子类型”，因此用 MoreIotas 的 `StringIota` 描述粒子。新增一个内部工具类 `ParticleSpec`，把字符串解析为 `ParticleOptions`：

| 写法 | 解析结果 |
| --- | --- |
| `minecraft:flame` | `BuiltInRegistries.PARTICLE_TYPE` 中的简单粒子，转成 `SimpleParticleType` |
| `dust:#RRGGBB` | `DustParticleOptions(Vector3f(r,g,b), 1.0f)` |
| `block:minecraft:stone` | `BlockParticleOption(ParticleTypes.BLOCK, 对应 BlockState)` |

规则：

- 没有命名空间时补 `minecraft:`。
- 只接受已注册在 `BuiltInRegistries.PARTICLE_TYPE` 的粒子，避免服务端把客户端不认识的粒子发给玩家。
- `dust` 颜色缺省为白色；`block` 缺省为石头。
- 解析失败抛 `MishapInvalidIota(..., "particle")`，并增加对应中英文案。
- `ParticleSpec` 只负责“解析 + 校验 + 生成 `ParticleOptions`”，不负责计费和范围检查，供普通粒子与贝塞尔粒子复用。

### 4.2 临时状态场

生命的节拍和长眠需要跨 tick 维持状态。沿用 `ThreeSunsAdriftField` 的模式，但只改实体内存状态，不写入任何方块或存档：

- `END_LEVEL_TICK` 推进到期时间；
- `SERVER_STOPPING` 强制恢复所有被修改的生物状态；
- 所有记录以实体 UUID 为键，实体卸载或死亡时直接丢弃；
- 不持久化到世界存档，避免“退出后怪物永久不出 AI”。

### 4.3 注册与资源

- 图案动作统一在 `Common/.../common/lib/hex/HexActions.java` 用 `make(id, new ActionRegistryEntry(HexPattern.fromAngles(...), Op...))` 注册。
- 普通法术教程书条目放在 `assets/hexcasting/patchouli_books/thehexbook/en_us/entries/patterns/spells/`，长眠放在 `patterns/great_spells/`。
- 每个图案需要 `hexcasting.action.hexcasting:<id>` 语言键；每个条目需要 `hexcasting.entry.<entry>` 与 `hexcasting.page.<entry>.N`。
- 失误文案按现有拼接规则补充：`hexcasting.mishap.invalid_value.<reason>`、`hexcasting.mishap.bad_block.<stub>`、`hexcasting.mishap.bad_location.<reason>`。

## 5. 生成粒子

| 项目 | 内容 |
| --- | --- |
| 注册 id | `hexcasting:spawn_particles` |
| 图案 | `wdwaeqawqeeedaqae`，起始 `EAST` |
| 输入 | `vec 位置`、`vec 散布`、`num 数量`、`str 粒子` |
| 输出 | 无 |
| 类别 | 普通法术，`patterns/spells` |
| 分析机 | 可用 |

玩法：

1. 在 `位置` 周围按 `散布` 各轴的最大偏移随机生成 `数量` 个粒子。
2. `散布` 为零时粒子集中在一点，适合做标记；数值越大越像云团。
3. 数量 1–256；散布每轴 0–16 格；位置必须通过 `assertVecInRange` 且在世界边界内。
4. 不检查 `canEditBlockAt`，因为粒子不改变世界。

媒质：

```text
1 粉 + ceil(数量 / 8) 粉
```

失误：

- 未知粒子 id 或格式错误：`MishapInvalidIota(粒子 iota, ..., "particle")`。
- 数量小于 1、大于 256，或散布超过 16：`MishapInvalidIota(..., "particle_count")` / `MishapBadLocation(..., "too_far")`。
- 位置超出施法范围：现有 `assertVecInRange` 的 `MishapBadLocation`。

实现位置：

- `Common/.../actions/spells/OpSpawnParticles.kt`
- `Common/.../actions/spells/ParticleSpec.kt`
- 教程书 `patterns/spells/particles.json`

限制与说明：

- 只发送服务端粒子，不使用客户端本地粒子，保证多人可见。
- 不提供“粒子颜色”作为独立参数；颜色通过 `dust:#RRGGBB` 字符串表达，避免增加 `argc`。
- 不发送 `explosion_emitter` 等会明显干扰客户端性能的粒子；如需要，后续单独加黑名单配置。

## 6. 生成贝塞尔粒子

| 项目 | 内容 |
| --- | --- |
| 注册 id | `hexcasting:spawn_bezier_particles` |
| 图案 | `aewqqddewdaqadqadqq`，起始 `EAST` |
| 输入 | `vec P0`、`vec P1`、`vec P2`、`vec P3`、`num 采样数`、`str 粒子` |
| 输出 | 无 |
| 类别 | 普通法术，`patterns/spells` |
| 分析机 | 可用 |

玩法：

1. 以 `P0`、`P1`、`P2`、`P3` 为控制点，计算三次贝塞尔曲线：

   ```text
   B(t) = (1-t)^3·P0 + 3(1-t)^2·t·P1 + 3(1-t)·t^2·P2 + t^3·P3
   ```

2. 在 `t = 0 … 1` 上等距取 `采样数` 个点，每个点发送 1 个粒子。
3. 采样数 2–128；控制多边形总长（`|P0P1| + |P1P2| + |P2P3|`）不超过 64 格，否则触发失误。
4. 所有控制点都要通过 `assertVecInRange`，且曲线不能跨出世界边界。

媒质：

```text
1 粉 + ceil(采样数 / 8) 粉
```

失误：

- 采样数越界：`MishapInvalidIota(..., "particle_count")`。
- 曲线过长：`MishapBadLocation(P0, "curve_too_long")`。
- 粒子 id 非法：同普通粒子。

实现位置：

- `Common/.../actions/spells/OpSpawnBezierParticles.kt`（复用 `ParticleSpec`）
- 教程书与普通粒子共用 `patterns/spells/particles.json` 的第二页

说明：

- 该法术只画曲线，不沿曲线施加力、不生成实体；要产生实际效果，需要与哨卫、实体操作或磁带组合。
- 采样点之间不插值、不绘制连线；需要更密的线就提高采样数，媒质随之增加。
- 速度参数固定为 0，避免粒子飞出预期轨迹；需要“喷射”效果时使用普通粒子法术并配合散布。

## 7. 方块下落

| 项目 | 内容 |
| --- | --- |
| 注册 id | `hexcasting:fall_block` |
| 图案 | `dewqaedwawadadawa`，起始 `EAST` |
| 输入 | `vec 位置` |
| 输出 | 无 |
| 类别 | 普通法术，`patterns/spells` |
| 分析机 | 可用 |

玩法：

1. 读取 `位置` 的方块状态。
2. 满足“可下落”条件时，先用 `level.removeBlock(pos, false)` 移除原方块，再调用 `FallingBlockEntity.fall(level, pos, state)` 生成下落方块实体。
3. 下落方块按原版重力落地；落地后变回方块，可被水、爆炸、活塞和其他实体影响。
4. 不额外掉落物品，避免复制；如果下落途中被破坏，方块直接消失，这是玩法代价。

“可下落”条件：

- 不是空气、不是流体；
- 没有方块实体（容器、箱子、熔炉等一律拒绝，避免内容丢失或复制）；
- `state.getDestroySpeed(level, pos) >= 0`（排除基岩、屏障、命令方块等）；
- 不是 `FallingBlock` 本身；
- 通过 `assertPosInRangeForEditing`，即施法范围内且有权编辑。

媒质：`1 粉`。

失误：

- 方块不可下落：`MishapBadBlock(pos, "fallable")`。
- 位置超出范围或无权编辑：现有 `MishapBadLocation` / `MishapBadBlock`。

实现位置：

- `Common/.../actions/spells/OpFallBlock.kt`
- 教程书 `patterns/spells/blockworks.json` 新增一页

说明：

- `FallingBlockEntity` 落地时可能造成原版级别的伤害或方块替换，这属于物理效果，不做额外抑制。
- 对分析机施法时 `env.castingEntity` 为 `null`，`isPlacingAllowed`/`isBreakingAllowed` 按现有 `OpBreakBlock` 的方式传入可空玩家；实现时确认平台层允许无玩家来源的方块变更。

## 8. 生命的节拍

`todo.txt` 的三项合成一组“生命的节拍”，但拆成三个图案，避免一个图案因实体类型不同而产生不可预测行为。

### 8.1 繁殖

| 项目 | 内容 |
| --- | --- |
| 注册 id | `hexcasting:life/breed` |
| 图案 | `qaqewawawdadweeqq`，起始 `EAST` |
| 输入 | `entity 动物` |
| 媒质 | `2 粉` |

效果：

- 目标必须是 `Animal` 且已成年。
- 调用 `setInLoveTime(600)`（30 秒），让附近同类动物进入繁殖状态。
- 不要求施法者；分析机可以用 `null` 玩家来源触发，因此不需要 `setInLove(Player)`。
- 已有恋爱计时（`getInLoveTime() > 0`）时不叠加，直接返回，避免连续施法无限延长。

失误：非动物或幼崽：`MishapInvalidIota(..., "animal")`。

### 8.2 成长

| 项目 | 内容 |
| --- | --- |
| 注册 id | `hexcasting:life/grow` |
| 图案 | `dedawewaawwqaaqad`，起始 `EAST` |
| 输入 | `entity 幼崽`、`num 秒` |
| 媒质 | `1 粉 + ceil(秒 / 10) 粉` |

效果：

- 目标必须是 `AgeableMob` 且 `isBaby()`。
- 调用 `ageUp(秒 × 20, true)`，把成长时间推进指定秒数；到达 0 时正常成年。
- 秒数 1–60；超过 60 秒按 60 秒计费和执行，避免一次施法直接刷出大量成年生物。

失误：非幼崽：`MishapInvalidIota(..., "baby")`；秒数越界：`MishapInvalidIota(..., "seconds")`。

### 8.3 兴奋

| 项目 | 内容 |
| --- | --- |
| 注册 id | `hexcasting:life/excite` |
| 图案 | `wqedwwdwewddeadeaaqwqaw`，起始 `EAST` |
| 输入 | `entity 悦灵`、`num 秒` |
| 媒质 | `1 粉 + ceil(秒 / 10) 粉` |

效果：

- 目标必须是 `Allay`。
- 调用 `setDancing(true)`；由于悦灵每 tick 会检查点唱机并可能自动停止，由 `LifeEffectField` 每 10 tick 重新置为 `true`。
- 到期后调用 `setDancing(false)`；如果悦灵在到期前死亡或卸载，直接丢弃记录。
- 秒数 1–60。

失误：非悦灵：`MishapInvalidIota(..., "allay")`。

### 8.4 状态场

`LifeEffectField` 只保存 `UUID -> 到期 tick`，不写方块、不写存档：

- `END_LEVEL_TICK` 每 10 tick 重新设置跳舞状态；
- 到期或实体不存在时恢复 `setDancing(false)` 并移除记录；
- `SERVER_STOPPING` 遍历剩余记录，全部恢复后清空。

## 9. 长眠

| 项目 | 内容 |
| --- | --- |
| 注册 id | `hexcasting:sleep` |
| 图案 | `eaqwawqdawwdwdqwe`，起始 `EAST` |
| 输入 | `player 目标`、`vec 床位置`、`num 模式` |
| 媒质 | `2 充能紫水晶` |
| 类别 | 伟大法术，`patterns/great_spells` |
| 分析机 | 不可用，需要 `env.castingEntity` |

模式：

- `0`：睡到下一次日出。
- `1`：睡到下一次月出（夜晚）。

实现方案（推荐）：

1. 校验目标玩家在施法范围内，且 `床位置` 是床（`BlockTags.BEDS`）。
2. 模式 0：
   - 调用 `ServerPlayer.startSleepInBed(bedPos)`；
   - 如果返回 `BedSleepingProblem`，转成 `MishapBadLocation(bedPos, "sleep")`；
   - 原版睡眠逻辑会在条件满足时把时间推进到 `WAKE_UP_FROM_SLEEP` 标记。
3. 模式 1：
   - 通过 `ServerLevel.clockManager().moveToTimeMarker(世界时钟 Holder, ClockTimeMarkers.NIGHT)` 把主世界时间直接推到月出；
   - 再调用 `startSleeping` / `stopSleepInBed` 给目标玩家一个睡觉的视觉与统计状态。
4. 只允许主世界；下界/末地触发 `MishapBadLocation(..., "sleep_dimension")`。

临时清除怪物 AI：

- 以床为中心、半径 16 格内的 `Mob`（建议只取实现 `Enemy` 的敌对生物）记录 `UUID + 原 NoAI 值`，然后 `setNoAi(true)`。
- 持续到目标玩家醒来，或最多 30 秒；以先到者为准。
- 恢复时只恢复“原本 NoAI 为 false”的生物，原本就是 NoAI 的不动。
- `SERVER_STOPPING` 强制恢复所有记录，确保存档里不会留下永久 NoAI 的怪物。

限制与风险：

- 时间跳跃是**全服**行为，多人服务器上会影响所有玩家；教程书需要明确写出。
- 模式 1 不是原版床行为，属于“把夜晚叫来”，不应用于跳过白天。
- 玩家必须已经在床上或紧邻床；不允许凭空睡在任意坐标。
- 长眠不修复“附近有怪物导致无法入睡”，但 AI 抑制会在入睡后生效。

失误：

- 目标不是玩家：`MishapInvalidIota(..., "player")`。
- 位置不是床、目标不在床附近：`MishapBadLocation(..., "sleep")`。
- 模式不是 0/1：`MishapInvalidIota(..., "mode")`。
- 非主世界：`MishapBadLocation(..., "sleep_dimension")`。

实现位置：

- `Common/.../actions/spells/OpSleep.kt`
- `Common/.../actions/spells/SleepAIField.kt`
- `Fabric/.../FabricHexInitializer.kt` 注册 tick 与停止清理
- 教程书 `patterns/great_spells/sleep.json`

## 10. 教程书与本地化

每个条目使用现有 `hexcasting:pattern` 页面类型：

```json
{
  "type": "hexcasting:pattern",
  "op_id": "hexcasting:spawn_particles",
  "anchor": "hexcasting:spawn_particles",
  "input": "vector, vector, number, string",
  "output": "",
  "text": "hexcasting.page.spawn_particles.0"
}
```

需要新增的教程书条目：

| 文件 | 内容 |
| --- | --- |
| `patterns/spells/particles.json` | 生成粒子、生成贝塞尔粒子（两页） |
| `patterns/spells/blockworks.json` | 方块下落（在原文件追加一页） |
| `patterns/spells/life.json` | 繁殖、成长、兴奋 |
| `patterns/great_spells/sleep.json` | 长眠 |

需要新增的语言键（中英同步）：

- `hexcasting.action.hexcasting:spawn_particles`
- `hexcasting.action.hexcasting:spawn_bezier_particles`
- `hexcasting.action.hexcasting:fall_block`
- `hexcasting.action.hexcasting:life/breed`
- `hexcasting.action.hexcasting:life/grow`
- `hexcasting.action.hexcasting:life/excite`
- `hexcasting.action.hexcasting:sleep`
- 以及对应的 `hexcasting.entry.*`、`hexcasting.page.*.N`

需要新增的失误文案：

| 键 | 含义 |
| --- | --- |
| `hexcasting.mishap.invalid_value.particle` | 粒子 id 或格式非法 |
| `hexcasting.mishap.invalid_value.particle_count` | 粒子数量/采样数越界 |
| `hexcasting.mishap.invalid_value.seconds` | 秒数越界 |
| `hexcasting.mishap.invalid_value.animal` | 目标不是成年动物 |
| `hexcasting.mishap.invalid_value.baby` | 目标不是幼崽 |
| `hexcasting.mishap.invalid_value.allay` | 目标不是悦灵 |
| `hexcasting.mishap.invalid_value.player` | 目标不是玩家 |
| `hexcasting.mishap.invalid_value.mode` | 睡眠模式非法 |
| `hexcasting.mishap.bad_block.fallable` | 方块不可下落 |
| `hexcasting.mishap.bad_block.sleep` | 目标位置不是床 |
| `hexcasting.mishap.bad_location.too_far` | 散布/曲线超出限制 |
| `hexcasting.mishap.bad_location.curve_too_long` | 贝塞尔控制多边形过长 |
| `hexcasting.mishap.bad_location.sleep_dimension` | 非主世界无法长眠 |

## 11. 实施阶段

| 阶段 | 内容 | 完成标志 |
| --- | --- | --- |
| A | `ParticleSpec`、生成粒子、生成贝塞尔粒子、方块下落 | 三个法术能构建、进实例验证；粒子可见、方块能下落且不复制 |
| B | `LifeEffectField`、繁殖、成长、兴奋 | 动物能进入恋爱、幼崽能加速成长、悦灵能跳舞并在到期后停止 |
| C | `SleepAIField`、长眠 | 能在床上睡到日出/月出；半径内怪物 AI 暂停并在醒来后恢复 |
| D | 教程书、中英文案、`hexparse/图案id速查表.md`、`CHANGELOG.md` | 书页能打开、图案名正确、速查表与实现一致 |

每阶段沿用现有流程：改代码/资源 → `./gradlew build -x test --offline -q` → 复制 jar 到实例 `mods` → 用户实测 → 提交 → 打 tag → 推送 → 发布 build。

## 12. 验收清单

### 生成粒子

- [ ] 输入合法位置、散布、数量和 `minecraft:flame`，能在指定位置看到火焰粒子。
- [ ] `dust:#FF0000` 显示红色尘粒子；`block:minecraft:stone` 显示石屑。
- [ ] 数量 256 可以执行，257 触发失误；散布 16 可以执行，17 触发失误。
- [ ] 分析机可以施放，不需要玩家。

### 生成贝塞尔粒子

- [ ] 四个控制点形成一条平滑曲线，采样点数量与输入一致。
- [ ] 采样数 128 可以执行，129 触发失误；控制多边形超过 64 格触发失误。
- [ ] 曲线不跨出世界边界；粒子不产生实体、不推动实体。

### 方块下落

- [ ] 对石头施法，石头变成下落方块并落地变回石头。
- [ ] 对箱子、熔炉、基岩、水、空气施法均触发 `fallable` 失误。
- [ ] 落地前破坏下落方块，不会复制出额外方块或掉落物。
- [ ] 分析机可以施放。

### 生命的节拍

- [ ] 对成年牛施放繁殖，牛进入恋爱状态；对幼崽施放触发 `animal` 失误。
- [ ] 对幼崽施放成长 10 秒，成长计时明显减少；成年动物触发 `baby` 失误。
- [ ] 对悦灵施放兴奋 10 秒，悦灵持续跳舞，10 秒后停止；其他生物触发 `allay` 失误。
- [ ] 退出并重进世界后，悦灵不会永久保持跳舞。

### 长眠

- [ ] 在主世界躺在床上施放模式 0，时间推进到日出，玩家醒来。
- [ ] 模式 1 把时间推进到夜晚；教程书明确提示这是全服行为。
- [ ] 床周围 16 格内原本有 AI 的怪物在睡眠期间停止行动，醒来后恢复。
- [ ] 原本就是 NoAI 的怪物在结束后仍然是 NoAI。
- [ ] 退出世界时没有怪物被永久写入 NoAI；保存与退出不卡住。

## 13. 风险与边界

- **粒子性能**：`sendParticles` 的 `count` 过大会造成客户端卡顿；统一上限 256，贝塞尔上限 128 采样点。
- **粒子兼容**：只允许 `BuiltInRegistries.PARTICLE_TYPE` 中已注册的粒子；自定义模组粒子如果没有同步到客户端，会被拒绝。
- **方块复制**：方块下落拒绝方块实体与容器，且不额外掉物品；实现时不要调用 `destroyBlock(pos, true)`。
- **下落伤害**：`FallingBlockEntity` 的原版落地行为可能造成伤害或替换方块，这是物理效果，不额外放大。
- **NoAI 持久化**：必须在 `SERVER_STOPPING` 恢复；只恢复由本法术设置的生物，避免覆盖原本的 NoAI 状态。
- **全服时间**：模式 1 会推进主世界时钟，多人服务器需要教程书明确说明；不要在下界/末地执行。
- **悦灵舞蹈重置**：`Allay` 每 tick 检查点唱机，`setDancing(true)` 可能被自动复位，必须由状态场持续维持。
- **图案冲突**：`PatternRegistryManifest` 对相同角度序列只记录 warn 并覆盖；新增图案后必须看启动日志，并在 `hexparse/图案id速查表.md` 中登记。
- **存档与退出**：所有新状态场只改内存，不写方块、不写持久化数据；退出前恢复，避免重演“释放后无法保存退出”的问题。

## 14. 待确认

1. 生命的节拍是否按本文拆成三个图案，还是合并为一个按实体类型分派的图案。
2. 长眠的模式 1（跳到月出）是否接受“影响全服时间”，还是只做“单人视觉睡眠、不推进时间”。
3. 方块下落是否需要支持容器（默认不支持，避免内容复制/丢失）。
4. 粒子是否需要更多内置简写（如 `flame`、`end_rod` 不带命名空间）。
