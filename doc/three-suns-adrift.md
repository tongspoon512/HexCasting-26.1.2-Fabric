# 三日凌空（Three Suns Adrift）设计文档

> 状态：已实现
> 归属：Hex Casting 核心（非 hexcreating）
> 定位：三点成面的范围打击法术，可被法杖、法术环与分析机施放

## 1. 设计目标与简化原则

1. **不新增 buff / 状态效果**：不使用 MobEffect，不做持续光照、温度场或标记。
2. **三个向量 Iota**：三个坐标围成三角形，三角形内部的生物承受打击。
3. **分析机可用**：法术注册为普通 Action，不进入 `REQUIRES_ENLIGHTENMENT` / `PER_WORLD_PATTERN` 标签，分析机与法术环都能施放。
4. **一次结算**：粒子表现与伤害在施法当 tick 完成，不生成实体、不延时、不追踪。
5. **不破坏方块**：只造成伤害与点燃。

## 2. 法术标识

| 项目 | 值 |
| --- | --- |
| 注册 ID | `hexcasting:three_suns_adrift` |
| 中文名 | 三日凌空 |
| 英文名 | Three Suns Adrift |
| 类型 | `SpellAction`（普通法术，非伟大之作） |
| 图案 | `edaawaqdw`，起始方向 `WEST`（三连形/三角力量） |
| 教程书条目 | `entries/patterns/spells/three_suns.json`，分类 `hexcasting:patterns/spells` |
| 名称键 | `hexcasting.action.hexcasting:three_suns_adrift` |
| 页面文本键 | `hexcasting.page.three_suns.0` |

图案为九段一笔画的三连形：外三角 + 内倒三角，与仓库内 243 个已注册签名均不重复，且满足 `fromAngles` 的无重叠校验。

## 3. 施法参数

| 项目 | 值 |
| --- | --- |
| 参数个数 `argc` | 3 |
| 输入 | `vector, vector, vector`（三角形三个顶点） |
| 输出 | 无 |
| 媒质消耗 | `MediaConstants.DUST_UNIT * 24`（24 紫水晶粉） |
| 范围检查 | 三个顶点逐一 `env.assertVecInRange` |
| 方块编辑 | 不需要 |

## 4. 效果流程

1. **三日凌空**：在每个顶点上方 10 格生成一颗太阳（纯火焰粒子），并向顶点垂直落下三道火线；三角形的三条边用火焰粒子描边，明确影响范围。
2. **灼烧打击**：同一 tick 内，结算三角形内部的所有生物：
   - 受到 8 点魔法伤害（4 颗心）；
   - 点燃 8 秒；
   - 不破坏方块、不留火、不施加状态效果。
3. **判定方式**：
   - 水平方向用 XZ 平面的叉积同号法判断点是否在三角形内；
   - 垂直方向取三个顶点最小 y - 2 到最大 y + 8 的区间，兼顾地面目标与飞行生物；
   - 面积小于 `1e-4` 的退化三角形不造成伤害，但仍播放粒子。

> 伤害合并为一次 `hurtServer` 调用，避免无敌帧吞伤害。

## 5. 分析机与法术环兼容性

- 本术未加入伟大之作标签，`HexConfig.isActionAllowedInCircles` 默认放行。
- `AnalyticalEngineCastEnv.getCastingEntity()` 返回 `null`，本术不读取施法者，不会 mishap。
- 粒子直接通过 `ServerLevel.sendParticles` 发送，因此分析机施放时同样可见。
- 分析机范围 32 格，三个顶点中任意一个超出范围都会走现有范围 mishap。

## 6. 边界与失败情况

| 情况 | 行为 |
| --- | --- |
| 任一顶点超出施法范围 | 现有范围 mishap |
| 媒质不足 | 现有媒质不足流程；分析机暂停等待补媒 |
| 三点共线 / 重合 | 无伤害，仅粒子 |
| 三角形跨越墙体 | 仍按三角形判定，不做视线与遮挡检查 |
| 生物处于三角形外 | 不受伤 |
| 方块保护 / 领地 | 不破坏方块，无需额外权限 |

## 7. 教程书内容

条目 `Common/src/main/resources/assets/hexcasting/patchouli_books/thehexbook/en_us/entries/patterns/spells/three_suns.json`，分两页：图案页 + 文本页。页面文本（`zh_cn.json`）：

```json
"hexcasting.entry.three_suns": "三日凌空",
"hexcasting.page.three_suns.0": "三个坐标，三颗太阳。用它们围出一个三角形，其中的每一个生物都会承受一次灼烧打击：8 点伤害与 8 秒燃烧。",
"hexcasting.page.three_suns.1": "它不需要施法者，也不需要视线——只要栈上有三个向量，法术环与分析机都能施放。$(br2)$(o)世界刚刚毁灭！！世界刚刚毁灭！！脱水！！脱水！！/$"
```

英文文本（`en_us.json`）：

```json
"hexcasting.entry.three_suns": "Three Suns Adrift",
"hexcasting.page.three_suns.0": "Three coordinates, three suns. Draw a triangle with them and every creature inside is scorched by a single strike: eight points of damage and eight seconds of fire.",
"hexcasting.page.three_suns.1": "It needs no caster and no line of sight — a circle or an Analytical Engine can cast it from three vectors alone.$(br2)$(o)The world has just been destroyed!! The world has just been destroyed!! Dehydrate!! Dehydrate!!/$"
```

斜体使用本项目的 `$(o) ... /$` 标记（见 `GuideMarkup.kt`）。

## 8. 实现位置

| 文件 | 改动 |
| --- | --- |
| `Common/src/main/java/at/petrak/hexcasting/common/casting/actions/spells/OpThreeSunsAdrift.kt` | 新增法术实现 |
| `Common/src/main/java/at/petrak/hexcasting/common/lib/hex/HexActions.java` | 注册 `THREE_SUNS_ADRIFT` |
| `Common/src/main/resources/assets/hexcasting/lang/en_us.json` | 名称、条目名、页面文本 |
| `Common/src/main/resources/assets/hexcasting/lang/zh_cn.json` | 同上（中文） |
| `Common/src/main/resources/.../entries/patterns/spells/three_suns.json` | 教程书条目 |

## 9. 验收清单

- [ ] 法杖施放：栈上三个向量按正确顺序消耗。
- [ ] 分析机程序：三个向量入栈后能施放并扣除控制器媒质。
- [ ] 法术环：能正常施放。
- [ ] 三角形内生物受到 8 点伤害并着火 8 秒，三角形外不受影响。
- [ ] 三点共线时不造成伤害。
- [ ] 方块、地形、天气、光照均无变化。
- [ ] 教程书条目显示图案、名称与文本，结尾为斜体。
- [ ] 教程书搜索可通过“三日凌空 / Three Suns”命中。
