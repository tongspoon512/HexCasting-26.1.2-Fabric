# HexCasting 图案 id 速查表

本表覆盖当前移植版内置的全部图案注册：`hexcasting`、`moreiotas`、`hexcassettes`、`hexdebug`、`hexcreating`。模块与注册表位置见 [项目架构表](项目架构表.md)。

## 书写约定

- `hexparse` 里通常只写 **代码名**（短名）。短名优先解析 `hexcasting:` 命名空间；其它模组的短名在本命名空间内唯一时也可直接用（如 `string/iota`、`zone_entity/type`）。
- 写 **完整 id**（如 `hexcasting:sentinel/create/great`、`hexcassettes:enqueue`）同样有效，适合避免重名歧义。
- 数据字面量（不是图案，可直接写在代码里）：
  - `num_42`、`num_-1`：数字
  - `vec_1_2_3`、`vec_0_-1_0`：向量（缺省分量自动补 0）
  - `str_文本`、`"文本"`：字符串
  - `type/entity_experience_orb`：实体类型 iota（默认命名空间 `minecraft:`）
  - `type/item_stone`、`type/block_stone`：物品/方块类型 iota
  - `type_hexcasting:list`：iota 类型 iota
  - `matrix_2_2_...` / `mat_...`：矩阵
  - `entity_<UUID>`：实体引用（不能引用其他玩家）
  - `_wedsaq...`：直接以笔画角度写图案
  - `(` / `)`：开/闭括号（数据列表）
- **卓越法术的笔顺不固定**：`lightning`、`flight`、`three_suns_adrift`、`infinite_void` 等 `per_world_pattern` 图案，每个世界会随机生成一套笔顺；不要用 `_...` 写死它们，也不要指望图案在不同世界间通用。
- 表格的“输入”列按**压栈顺序**排列：从左到右依次压栈，最右边的是栈顶；图案从栈顶取参，因此源码中的 `args[0]` 对应最左边的参数。“无”表示不需要输入。

## hexcasting：实体与位置

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `get_caster` | `hexcasting:get_caster` | 无 | 获取施法者实体 |
| `entity_pos/eye` | `hexcasting:entity_pos/eye` | entity | 实体眼睛位置 |
| `entity_pos/foot` | `hexcasting:entity_pos/foot` | entity | 实体脚底位置 |
| `get_entity_look` | `hexcasting:get_entity_look` | entity | 实体视线方向向量 |
| `get_entity_height` | `hexcasting:get_entity_height` | entity | 实体高度 |
| `get_entity_velocity` | `hexcasting:get_entity_velocity` | entity | 实体速度向量 |

## hexcasting：射线、媒体、施法圈

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `raycast` | `hexcasting:raycast` | vec 起点, vec 方向 | 方块射线，命中返回方块坐标，否则 null |
| `raycast/axis` | `hexcasting:raycast/axis` | vec 起点, vec 方向 | 方块射线，附带命中面法线 |
| `raycast/entity` | `hexcasting:raycast/entity` | vec 起点, vec 方向 | 实体射线 |
| `get_media` | `hexcasting:get_media` | 无 | 施法者剩余媒体 |
| `circle/impetus_pos` | `hexcasting:circle/impetus_pos` | 无 | 施法圈促动点位置 |
| `circle/impetus_dir` | `hexcasting:circle/impetus_dir` | 无 | 施法圈促动方向 |
| `circle/bounds/min` | `hexcasting:circle/bounds/min` | 无 | 施法圈包围盒最小角点 |
| `circle/bounds/max` | `hexcasting:circle/bounds/max` | 无 | 施法圈包围盒最大角点 |

## hexcasting：环境

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `boolean_plane` | `hexcasting:boolean_plane` | 无 | 施法者所在维度是否为“非下界”：主世界/末地为 true，下界为 false |

## hexcasting：栈操作

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `swap` | `hexcasting:swap` | a, b | 交换栈顶两项 |
| `rotate` | `hexcasting:rotate` | a, b, c | 栈顶三项轮转 |
| `rotate_reverse` | `hexcasting:rotate_reverse` | a, b, c | 栈顶三项反向轮转 |
| `duplicate` | `hexcasting:duplicate` | a | 复制栈顶 |
| `over` | `hexcasting:over` | a, b | 复制第二项到栈顶 |
| `tuck` | `hexcasting:tuck` | a, b | 把栈顶插入第二项下方 |
| `2dup` | `hexcasting:2dup` | a, b | 复制栈顶两项 |
| `stack_len` | `hexcasting:stack_len` | 无 | 当前栈长度 |
| `duplicate_n` | `hexcasting:duplicate_n` | 任意 iota, num 份数 | 复制栈顶 n 项 |
| `fisherman` | `hexcasting:fisherman` | num 深度 | 按深度取出并移除一项 |
| `fisherman/copy` | `hexcasting:fisherman/copy` | num 深度 | 按深度复制一项（不移除） |
| `swizzle` | `hexcasting:swizzle` | 若干 iota, num 编码（栈顶） | 按掩码重排栈顶若干项 |

## hexcasting：基础数学

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `add` | `hexcasting:add` | a, b | 加法 / 向量相加 / 列表拼接 |
| `sub` | `hexcasting:sub` | a, b | 减法 / 向量相减 |
| `mul` | `hexcasting:mul` | a, b | 乘法 / 点积 |
| `div` | `hexcasting:div` | a, b | 除法 / 叉积 |
| `abs` | `hexcasting:abs` | a | 绝对值 / 向量长度 / 列表长度 |
| `pow` | `hexcasting:pow` | num/vec a, num/vec b | 幂 / 向量投影 |
| `floor` | `hexcasting:floor` | num/vec | 向下取整 |
| `ceil` | `hexcasting:ceil` | num/vec | 向上取整 |
| `construct_vec` | `hexcasting:construct_vec` | num x, num y, num z | 三个数构造向量 |
| `deconstruct_vec` | `hexcasting:deconstruct_vec` | vec | 向量拆成三个数 |
| `coerce_axial` | `hexcasting:coerce_axial` | num/vec | 向量转轴对齐向量 |

## hexcasting：逻辑与比较

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `and` | `hexcasting:and` | a, b | 逻辑与 |
| `or` | `hexcasting:or` | a, b | 逻辑或 |
| `not` | `hexcasting:not` | a | 逻辑非 |
| `xor` | `hexcasting:xor` | a, b | 逻辑异或 |
| `greater` | `hexcasting:greater` | a, b | 大于 |
| `less` | `hexcasting:less` | a, b | 小于 |
| `greater_eq` | `hexcasting:greater_eq` | a, b | 大于等于 |
| `less_eq` | `hexcasting:less_eq` | a, b | 小于等于 |
| `equals` | `hexcasting:equals` | a, b | 任意 iota 相等 |
| `not_equals` | `hexcasting:not_equals` | a, b | 任意 iota 不相等 |
| `type_equals` | `hexcasting:type_equals` | a, b | 两个 iota 类型相同 |
| `type_not_equals` | `hexcasting:type_not_equals` | a, b | 类型不同 |
| `bool_coerce` | `hexcasting:bool_coerce` | a | 转布尔（空列表/0/null 为假） |
| `if` | `hexcasting:if` | bool 条件, 真值, 假值 | 条件分支（条件、真分支、假分支） |
| `random` | `hexcasting:random` | 无 | 随机数 |

## hexcasting：高级数学

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `sin` | `hexcasting:sin` | num | 正弦 |
| `cos` | `hexcasting:cos` | num | 余弦 |
| `tan` | `hexcasting:tan` | num | 正切 |
| `arcsin` | `hexcasting:arcsin` | num | 反正弦 |
| `arccos` | `hexcasting:arccos` | num | 反余弦 |
| `arctan` | `hexcasting:arctan` | num | 反正切 |
| `arctan2` | `hexcasting:arctan2` | num y, num x | 双参数反正切 |
| `logarithm` | `hexcasting:logarithm` | num 值, num 底 | 对数 |
| `modulo` | `hexcasting:modulo` | num a, num b | 取模 |
| `unique` | `hexcasting:unique` | list | 列表去重（集合） |

## hexcasting：法术

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `print` | `hexcasting:print` | 任意 iota（保留在栈上） | 在聊天栏打印 iota |
| `explode` | `hexcasting:explode` | vec 位置, num 强度 | 爆炸 |
| `explode/fire` | `hexcasting:explode/fire` | vec 位置, num 强度 | 带火爆炸 |
| `three_suns_adrift` | `hexcasting:three_suns_adrift` | area 火之领域 | 三日凌空（天顶法术，需施法者）：三颗太阳直接落在火之领域的三个角上，作用范围是整个领域，持续 20 分钟，融雪融冰、烘干、催生并灼烧敌对怪物（每秒 6 点）；只接受火之领域，其他领域引发事故；水/细雪可熄灭单颗，雨天削弱；媒质为 1 充能紫水晶 + 每 4 格² 1 粉 |
| `spawn_particles` | `hexcasting:spawn_particles` | vec 位置, vec 散布, num 数量, str 粒子 id | 在位置附近生成粒子（仅绘制） |
| `spawn_bezier_particles` | `hexcasting:spawn_bezier_particles` | vec 控制点1, vec 控制点2, vec 控制点3, vec 控制点4, num 采样数, str 粒子 id | 沿三次贝塞尔曲线生成粒子（仅绘制） |
| `fall_block` | `hexcasting:fall_block` | vec 位置 | 让该处方块以掉落方块的形式下落 |
| `life/breed` | `hexcasting:life/breed` | entity | 让成年动物进入 30 秒恋爱状态，消耗 2 粉 |
| `life/grow` | `hexcasting:life/grow` | entity, num 秒数 | 幼崽成长计时推进 1–60 秒，消耗 1 粉 + 每 10 秒 1 粉 |
| `life/excite` | `hexcasting:life/excite` | entity, num 秒数 | 让悦灵跳舞 1–60 秒，消耗 1 粉 + 每 10 秒 1 粉 |
| `domain/wind` | `hexcasting:domain/wind` | vec ×5 | 生得—风之提整：围出五芒星领域（顶面向下延伸 10 格） |
| `domain/fire` | `hexcasting:domain/fire` | vec ×3 | 生得—火之提整：围出三角形领域（顶面向下延伸 10 格） |
| `domain/water` | `hexcasting:domain/water` | vec 球心, num 半径 | 生得—水之馏化：围出球形领域（半径不超过 32） |
| `domain/earth` | `hexcasting:domain/earth` | vec 对角 A, vec 对角 B | 生得—地之提整：围出立方体领域 |
| `domain/euclid_purification` | `hexcasting:domain/euclid_purification` | area | 欧几里得之纯化：返回领域的重心 |
| `domain/sariel_strategy` | `hexcasting:domain/sariel_strategy` | area, list | 沙利叶之策略：让领域每 3 tick 释放一次列表中的咒术 |
| `domain/hei_wuchang_strategy` | `hexcasting:domain/hei_wuchang_strategy` | area | 黑无常之策略：销毁领域 |
| `domain/bai_wuchang_strategy` | `hexcasting:domain/bai_wuchang_strategy` | area | 白无常之策略：暂停或恢复领域的释放 |
| `domain/mahoraga_purification` | `hexcasting:domain/mahoraga_purification` | vec | 魔虚罗之纯化：返回该向量所在领域的领域 iota |
| `domain/zone_distillation` | `hexcasting:domain/zone_distillation` | area, bool | 区域之馏化：领域：返回领域内的实体列表（假：除掉落物；真：再排除领域召唤者） |
| `add_motion` | `hexcasting:add_motion` | entity, vec | 给实体施加冲量 |
| `blink` | `hexcasting:blink` | entity, num 距离 | 瞬移（短距离） |
| `break_block` | `hexcasting:break_block` | vec 位置 | 破坏方块（正常掉落） |
| `place_block` | `hexcasting:place_block` | blockpos 位置 | 放置方块 |
| `colorize` | `hexcasting:colorize` | 无 | 消耗手持颜料，设置施法颜色 |
| `cycle_variant` | `hexcasting:cycle_variant` | 无 | 循环切换物品变体 |
| `create_water` | `hexcasting:create_water` | blockpos | 造水 |
| `destroy_water` | `hexcasting:destroy_water` | vec | 消水 |
| `ignite` | `hexcasting:ignite` | entity 或 blockpos | 点燃方块/实体 |
| `extinguish` | `hexcasting:extinguish` | vec | 灭火 |
| `conjure_block` | `hexcasting:conjure_block` | vec | 召唤临时方块 |
| `conjure_light` | `hexcasting:conjure_light` | vec | 召唤光源 |
| `bonemeal` | `hexcasting:bonemeal` | blockpos | 骨粉催熟 |
| `recharge` | `hexcasting:recharge` | 掉落物实体 | 用媒体物品给手持可充能物品充能 |
| `erase` | `hexcasting:erase` | 无 | 清空手持物品中的咒术/数据 |
| `pickup_item` | `hexcasting:pickup_item` | 掉落物实体 | 拾取一个掉落物实体 |
| `gain_experience` | `hexcasting:gain_experience` | 经验球实体 | 吸收一个经验球实体 |
| `advanced_pickup` | `hexcasting:advanced_pickup` | 掉落物实体, blockpos | 把掉落物实体送入容器 |
| `edify` | `hexcasting:edify` | vec | 催熟树苗成树 |
| `beep` | `hexcasting:beep` | vec 位置, num 乐器(0–15), num 音高(0–24) | 播放提示音 |
| `craft/cypher` | `hexcasting:craft/cypher` | 掉落物实体, list | 制作符/古符 |
| `craft/trinket` | `hexcasting:craft/trinket` | 掉落物实体, list | 制作饰品 |
| `craft/artifact` | `hexcasting:craft/artifact` | 掉落物实体, list | 制造物 |
| `craft/battery` | `hexcasting:craft/battery` | 掉落物实体（含媒体） | 制作媒体电池 |

## hexcasting：药水

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `potion/weakness` | `hexcasting:potion/weakness` | entity, num 时长, num 强度 | 虚弱 |
| `potion/levitation` | `hexcasting:potion/levitation` | entity, num 时长 | 漂浮 |
| `potion/wither` | `hexcasting:potion/wither` | entity, num 时长, num 强度 | 凋零 |
| `potion/poison` | `hexcasting:potion/poison` | entity, num 时长, num 强度 | 中毒 |
| `potion/slowness` | `hexcasting:potion/slowness` | entity, num 时长, num 强度 | 缓慢 |
| `potion/regeneration` | `hexcasting:potion/regeneration` | entity, num 时长, num 强度 | 生命恢复 |
| `potion/night_vision` | `hexcasting:potion/night_vision` | entity, num 时长 | 夜视 |
| `potion/absorption` | `hexcasting:potion/absorption` | entity, num 时长, num 强度 | 伤害吸收 |
| `potion/haste` | `hexcasting:potion/haste` | entity, num 时长, num 强度 | 急迫 |
| `potion/strength` | `hexcasting:potion/strength` | entity, num 时长, num 强度 | 力量 |

## hexcasting：飞行与哨卫

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `flight/range` | `hexcasting:flight/range` | player, num 距离 | 赋予飞行（按距离） |
| `flight/time` | `hexcasting:flight/time` | player, num 秒 | 赋予飞行（按时间） |
| `flight/can_fly` | `hexcasting:flight/can_fly` | player | 查询实体能否飞行 |
| `sentinel/create` | `hexcasting:sentinel/create` | vec | 创建哨卫 |
| `sentinel/destroy` | `hexcasting:sentinel/destroy` | 无 | 销毁哨卫 |
| `sentinel/get_pos` | `hexcasting:sentinel/get_pos` | 无 | 获取哨卫坐标 |
| `sentinel/wayfind` | `hexcasting:sentinel/wayfind` | vec | 从给定坐标指向哨卫的单位向量 |

## hexcasting：伟大法术

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `lightning` | `hexcasting:lightning` | vec | 召唤闪电 |
| `flight` | `hexcasting:flight` | player | 赋予飞行（Altiora） |
| `create_lava` | `hexcasting:create_lava` | blockpos | 造岩浆 |
| `teleport/great` | `hexcasting:teleport/great` | entity, vec | 远距离传送 |
| `sentinel/create/great` | `hexcasting:sentinel/create/great` | vec | 创建卓越哨卫 |
| `dispel_rain` | `hexcasting:dispel_rain` | 无 | 停雨 |
| `summon_rain` | `hexcasting:summon_rain` | 无 | 下雨 |
| `brainsweep` | `hexcasting:brainsweep` | 生物实体, vec | 洗脑/转化生物 |
| `infinite_void` | `hexcasting:infinite_void` | entity | 无量空处（卓越法术，只能由领域释放）：生物停止 AI 60 秒；玩家额外获得黑暗、失明、缓慢 V 并在 30 秒内无法施法；消耗 20 粉 |

## hexcasting：Akashic

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `akashic/read` | `hexcasting:akashic/read` | blockpos, pattern | 读取 Akashic 记录 |
| `akashic/write` | `hexcasting:akashic/write` | blockpos, pattern, 任意 iota | 写入 Akashic 记录 |

## hexcasting：元法术与控制流

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `escape` | `hexcasting:escape` | 无 | 转义：把后续图案当数据 |
| `runtime_escape` | `hexcasting:runtime_escape` | 无 | 运行时转义 |
| `open_paren` | `hexcasting:open_paren` | 无 | 开括号 |
| `close_paren` | `hexcasting:close_paren` | 无 | 闭括号 |
| `open_n_parens` | `hexcasting:open_n_parens` | num n | 连续开 n 个括号 |
| `close_all_parens` | `hexcasting:close_all_parens` | 无 | 关闭所有括号 |
| `read_into_parens` | `hexcasting:read_into_parens` | 无 | 读入并置于括号中 |
| `undo` | `hexcasting:undo` | 无 | 撤销上一个操作 |
| `eval` | `hexcasting:eval` | list | 执行一个咒术列表 |
| `eval/cc` | `hexcasting:eval/cc` | list | 可中断的 eval |
| `halt` | `hexcasting:halt` | 无 | 清空当前咒术的后续执行（可跳出多层 eval） |
| `read` | `hexcasting:read` | 无 | 读取手持物品中的数据 |
| `read/entity` | `hexcasting:read/entity` | entity | 读取实体的数据 |
| `write` | `hexcasting:write` | 任意 iota | 写入手持物品中的数据 |
| `write/entity` | `hexcasting:write/entity` | entity, 任意 iota | 写入实体数据 |
| `readable` | `hexcasting:readable` | 无 | 查询手持物品能否读取 |
| `readable/entity` | `hexcasting:readable/entity` | entity | 查询实体能否读取 |
| `writable` | `hexcasting:writable` | 无 | 查询手持物品能否写入 |
| `writable/entity` | `hexcasting:writable/entity` | entity | 查询实体能否写入 |
| `read/local` | `hexcasting:read/local` | 无 | 读取局部变量 |
| `write/local` | `hexcasting:write/local` | 任意 iota | 写入局部变量 |
| `thanatos` | `hexcasting:thanatos` | 无 | 压入剩余可执行操作数 |

## hexcasting：常量

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `const/null` | `hexcasting:const/null` | 无 | null |
| `const/true` | `hexcasting:const/true` | 无 | 布尔真 |
| `const/false` | `hexcasting:const/false` | 无 | 布尔假 |
| `const/vec/px` | `hexcasting:const/vec/px` | 无 | 单位向量 +X |
| `const/vec/py` | `hexcasting:const/vec/py` | 无 | 单位向量 +Y |
| `const/vec/pz` | `hexcasting:const/vec/pz` | 无 | 单位向量 +Z |
| `const/vec/nx` | `hexcasting:const/vec/nx` | 无 | 单位向量 -X |
| `const/vec/ny` | `hexcasting:const/vec/ny` | 无 | 单位向量 -Y |
| `const/vec/nz` | `hexcasting:const/vec/nz` | 无 | 单位向量 -Z |
| `const/vec/0` | `hexcasting:const/vec/0` | 无 | 零向量 |
| `const/double/pi` | `hexcasting:const/double/pi` | 无 | π |
| `const/double/tau` | `hexcasting:const/double/tau` | 无 | τ = 2π |
| `const/double/e` | `hexcasting:const/double/e` | 无 | e |
| `const/double/phi` | `hexcasting:const/double/phi` | 无 | φ（黄金比） |

## hexcasting：实体查询

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `get_entity` | `hexcasting:get_entity` | vec 中心 | 取查询框内最近的任意实体 |
| `get_entity/animal` | `hexcasting:get_entity/animal` | vec 中心 | 最近的动物 |
| `get_entity/monster` | `hexcasting:get_entity/monster` | vec 中心 | 最近的怪物 |
| `get_entity/item` | `hexcasting:get_entity/item` | vec 中心 | 最近的掉落物 |
| `get_entity/player` | `hexcasting:get_entity/player` | vec 中心 | 最近的玩家 |
| `get_entity/living` | `hexcasting:get_entity/living` | vec 中心 | 最近的生物 |
| `zone_entity` | `hexcasting:zone_entity` | vec 中心, num 半径 | 半径内全部实体列表 |
| `zone_entity/animal` | `hexcasting:zone_entity/animal` | vec 中心, num 半径 | 半径内全部动物 |
| `zone_entity/not_animal` | `hexcasting:zone_entity/not_animal` | vec 中心, num 半径 | 半径内全部非动物 |
| `zone_entity/monster` | `hexcasting:zone_entity/monster` | vec 中心, num 半径 | 半径内全部怪物 |
| `zone_entity/not_monster` | `hexcasting:zone_entity/not_monster` | vec 中心, num 半径 | 半径内全部非怪物 |
| `zone_entity/item` | `hexcasting:zone_entity/item` | vec 中心, num 半径 | 半径内全部掉落物 |
| `zone_entity/not_item` | `hexcasting:zone_entity/not_item` | vec 中心, num 半径 | 半径内全部非掉落物 |
| `zone_entity/player` | `hexcasting:zone_entity/player` | vec 中心, num 半径 | 半径内全部玩家 |
| `zone_entity/not_player` | `hexcasting:zone_entity/not_player` | vec 中心, num 半径 | 半径内全部非玩家 |
| `zone_entity/living` | `hexcasting:zone_entity/living` | vec 中心, num 半径 | 半径内全部生物 |
| `zone_entity/not_living` | `hexcasting:zone_entity/not_living` | vec 中心, num 半径 | 半径内全部非生物 |

## hexcasting：类型比较

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `compare_block/lenient` | `hexcasting:compare_block/lenient` | blockpos, blockpos | 宽松比较方块 |
| `compare_block/strict` | `hexcasting:compare_block/strict` | blockpos, blockpos | 严格比较方块（含状态） |
| `compare_entity` | `hexcasting:compare_entity` | entity, entity | 比较实体 |
| `compare_item/lenient` | `hexcasting:compare_item/lenient` | 掉落物实体, 掉落物实体 | 宽松比较物品 |
| `compare_item/strict` | `hexcasting:compare_item/strict` | 掉落物实体, 掉落物实体 | 严格比较物品 |

## hexcasting：列表

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `append` | `hexcasting:append` | list, 元素 | 列表尾部追加 |
| `unappend` | `hexcasting:unappend` | list | 取列表尾部 |
| `index` | `hexcasting:index` | list, num 下标 | 按下标取元素 |
| `for_each` | `hexcasting:for_each` | list 指令, list 数据 | 对每个元素执行一段咒术 |
| `singleton` | `hexcasting:singleton` | 任意 iota | 单元素列表 |
| `empty_list` | `hexcasting:empty_list` | 无 | 空列表 |
| `reverse` | `hexcasting:reverse` | list | 反转列表 |
| `last_n_list` | `hexcasting:last_n_list` | num n | 取最后 n 项 |
| `splat` | `hexcasting:splat` | list | 把列表展开到栈上 |
| `index_of` | `hexcasting:index_of` | list/str, 元素/子串 | 查找元素下标（不存在为 -1） |
| `remove_from` | `hexcasting:remove_from` | list/str, num 下标 | 删除指定下标元素 |
| `slice` | `hexcasting:slice` | list/str, num 起, num 止 | 取子列表 |
| `replace` | `hexcasting:replace` | list/str, num 下标, 元素/字符串 | 替换指定下标元素 |
| `construct` | `hexcasting:construct` | list, 元素 | 由元素构造列表 |
| `deconstruct` | `hexcasting:deconstruct` | list | 拆出首元素与剩余列表 |

## hexcasting：Pehkui 互操作（仅装 Pehkui 时注册）

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `interop/pehkui/get` | `hexcasting:interop/pehkui/get` | entity | 读取实体缩放 |
| `interop/pehkui/set` | `hexcasting:interop/pehkui/set` | entity, num 缩放 | 设置实体缩放 |

## moreiotas：字符串

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `string/empty` | `moreiotas:string/empty` | 无 | 空字符串 |
| `string/space` | `moreiotas:string/space` | 无 | 空格 |
| `string/comma` | `moreiotas:string/comma` | 无 | 逗号 |
| `string/newline` | `moreiotas:string/newline` | 无 | 换行 |
| `string/block/get` | `moreiotas:string/block/get` | blockpos | 读取方块名字符串 |
| `string/block/set` | `moreiotas:string/block/set` | blockpos, str | 按字符串设置方块 |
| `string/chat/caster` | `moreiotas:string/chat/caster` | 无 | 读取施法者最近一条聊天消息 |
| `string/chat/all` | `moreiotas:string/chat/all` | 无 | 读取全服最近一条聊天消息 |
| `string/chat/prefix/get` | `moreiotas:string/chat/prefix/get` | 无 | 读取聊天前缀 |
| `string/chat/prefix/set` | `moreiotas:string/chat/prefix/set` | str 或 null | 设置聊天前缀 |
| `string/iota` | `moreiotas:string/iota` | 任意 iota | iota 的显示文本 |
| `string/action` | `moreiotas:string/action` | pattern | 图案的显示名（可用于按名字查图案） |
| `string/name/get` | `moreiotas:string/name/get` | entity | 读取实体/物品的名字 |
| `string/name/set` | `moreiotas:string/name/set` | str, entity | 设置实体/物品的名字 |
| `string/split` | `moreiotas:string/split` | str, str 分隔符 | 按分隔符切分字符串 |
| `string/parse` | `moreiotas:string/parse` | str | 字符串转数字（失败为 null） |
| `string/case` | `moreiotas:string/case` | str, bool/null（true 大写 / false 小写 / null 反转） | 切换大小写 |

## moreiotas：矩阵

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `altadd` | `moreiotas:altadd` | matrix, matrix | 矩阵加法（替代运算符） |
| `altmul` | `moreiotas:altmul` | matrix, matrix | 矩阵乘法（替代运算符） |
| `altdiv` | `moreiotas:altdiv` | matrix, matrix | 矩阵除法（替代运算符） |
| `altpow` | `moreiotas:altpow` | matrix, matrix | 矩阵幂（替代运算符） |
| `matrix/make` | `moreiotas:matrix/make` | num/vec/list | 由数值构造矩阵 |
| `matrix/unmake` | `moreiotas:matrix/unmake` | matrix | 矩阵拆成列表 |
| `matrix/unmake/direct` | `moreiotas:matrix/unmake/direct` | matrix | 矩阵直接拆成栈上数值 |
| `matrix/identity` | `moreiotas:matrix/identity` | num 阶数 | 单位矩阵 |
| `matrix/zero` | `moreiotas:matrix/zero` | num 行, num 列 | 零矩阵 |
| `matrix/rotation` | `moreiotas:matrix/rotation` | vec 轴, num 角度 | 旋转矩阵 |
| `matrix/inverse` | `moreiotas:matrix/inverse` | matrix | 逆矩阵 |
| `matrix/determinant` | `moreiotas:matrix/determinant` | matrix | 行列式 |
| `matrix/concat/vert` | `moreiotas:matrix/concat/vert` | matrix, matrix | 垂直拼接矩阵 |
| `matrix/concat/hori` | `moreiotas:matrix/concat/hori` | matrix, matrix | 水平拼接矩阵 |
| `matrix/split/vert` | `moreiotas:matrix/split/vert` | matrix, num 分割行 | 垂直拆分矩阵 |
| `matrix/split/hori` | `moreiotas:matrix/split/hori` | matrix, num 分割列 | 水平拆分矩阵 |

## moreiotas：类型与实体类型

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `type/to_item` | `moreiotas:type/to_item` | vec/entity/item stack | 把方块/物品类型转成物品 |
| `type/entity` | `moreiotas:type/entity` | entity | 取实体的实体类型 |
| `type/iota` | `moreiotas:type/iota` | 任意 iota | 取 iota 的类型 |
| `type/item_held` | `moreiotas:type/item_held` | 无 | 取手持物品的物品类型 |
| `get_entity/type` | `moreiotas:get_entity/type` | 实体类型, vec 中心 | 取查询框内最近的指定类型实体 |
| `zone_entity/type` | `moreiotas:zone_entity/type` | 实体类型, vec 中心, num 半径 | 取半径内全部指定类型实体 |
| `zone_entity/not_type` | `moreiotas:zone_entity/not_type` | 实体类型, vec 中心, num 半径 | 取半径内全部非指定类型实体 |

> `get_entity/type`、`zone_entity/type` 的类型参数可以是 `type/entity_<id>` 字面量，也可以直接给一个该类型的实体 iota。

## moreiotas：物品

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `item/main_hand` | `moreiotas:item/main_hand` | entity | 读取主手物品 |
| `item/off_hand` | `moreiotas:item/off_hand` | entity | 读取副手物品 |
| `item/inventory/stacks` | `moreiotas:item/inventory/stacks` | blockpos, vec 面方向 | 读取背包物品堆列表 |
| `item/inventory/items` | `moreiotas:item/inventory/items` | blockpos, vec 面方向 | 读取背包物品类型列表 |
| `item/make` | `moreiotas:item/make` | 物品类型/物品堆, num 数量 | 用物品类型造物品堆 |

## hexcassettes：磁带

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `hexcassettes:enqueue` | `hexcassettes:enqueue` | list, num 延迟, [str 标签]（可选，栈顶） | 把一段咒术延迟入队 |
| `hexcassettes:dequeue` | `hexcassettes:dequeue` | str 标签 | 取消指定标签的磁带 |
| `hexcassettes:killall` | `hexcassettes:killall` | 无 | 清空所有磁带 |
| `hexcassettes:specs` | `hexcassettes:specs` | 无 | 已拥有的磁带槽位数 |
| `hexcassettes:busy` | `hexcassettes:busy` | 无 | 正在运行的磁带标签列表 |
| `hexcassettes:inspect` | `hexcassettes:inspect` | player, str 标签 | 查询某玩家是否有指定标签的磁带 |
| `hexcassettes:foretell` | `hexcassettes:foretell` | str 标签 | 查询指定磁带的剩余延迟 |
| `hexcassettes:self` | `hexcassettes:self` | 无 | 当前所在磁带的标签（不在磁带中为 null） |

## hexdebug：调试

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `get_personal_media` | `hexdebug:get_personal_media` | 无 | 读取个人媒体余量 |
| `get_personal_media_max` | `hexdebug:get_personal_media_max` | 无 | 读取个人媒体上限 |
| `mind_env/schedule` | `hexdebug:mind_env/schedule` | list 咒术, num 延迟 | 延迟调度一段咒术 |
| `mind_env/running_code` | `hexdebug:mind_env/running_code` | 无 | 查询正在运行的调度代码 |

## hexcreating：Create 联动

> 这些图案由 HexCreating 注册，id 前缀为 `hexcreating:`。读取类图案在目标不是对应 Create 方块实体时返回 `null`。

| 代码名 | 完整 id | 输入（按压栈顺序，右为栈顶） | 说明 |
| --- | --- | --- | --- |
| `kelvin_heat_gambit` | `hexcreating:kelvin_heat_gambit` | blockpos 燃烧室, num tick | 开尔文之策略-Δ：把已点燃的烈焰人燃烧室设为普通燃烧并延长 tick；创造模式或已高温时无效。耗媒 tick÷20 粉 |
| `kelvin_super_heat_gambit` | `hexcreating:kelvin_super_heat_gambit` | blockpos 燃烧室, num tick | 开尔文之策略-高温：把已点燃的燃烧室切到高温并延长 tick；创造模式无效。耗媒 tick÷20 粉 |
| `kelvin_time_purification` | `hexcreating:kelvin_time_purification` | blockpos 燃烧室 | 开尔文之纯化-时间：返回剩余燃烧 tick |
| `kelvin_type_purification` | `hexcreating:kelvin_type_purification` | blockpos 燃烧室 | 开尔文之纯化-热量：高温 2、普通 1、熄灭 0 |
| `pascal_time_purification` | `hexcreating:pascal_time_purification` | blockpos 背罐 | 帕斯卡之纯化-时间：返回铜/下界合金背罐的空气量 |
| `kinetic_rotation_purification` | `hexcreating:kinetic_rotation_purification` | blockpos 动力元件 | 动力之纯化-旋转：返回转速绝对值（RPM） |
| `kinetic_network_stress_all_purification` | `hexcreating:kinetic_network_stress_all_purification` | blockpos 动力元件 | 动力之纯化-网络总应力：返回网络容量；孤立网络为 0 |
| `kinetic_network_stress_used_purification` | `hexcreating:kinetic_network_stress_used_purification` | blockpos 动力元件 | 动力之纯化-网络消耗应力：返回网络已用应力；孤立网络为 0 |
| `kinetic_stress_purification` | `hexcreating:kinetic_stress_purification` | blockpos 动力元件 | 动力之纯化-剩余应力：（元件容量 − 已施加应力）× 转速绝对值 |
| `train_gate_set_up_gambit` | `hexcreating:train_gate_set_up_gambit` | blockpos 门核心 A, blockpos 门核心 B | 创建浮辇之门：摧毁两核心原有门并互连；耗媒 曼哈顿距离÷10 粉 |

## 备注

- 卓越法术（`lightning`、`flight`、`create_lava`、`teleport/great`、`sentinel/create/great`、`dispel_rain`、`summon_rain`、`brainsweep`、`craft/battery`、`three_suns_adrift`、`infinite_void`、各类 `potion/*`）的图案随世界随机，**笔顺不固定**。当前世界的实际笔顺用 `/hexcasting perWorldPatterns list` 查看，用 `/hexcasting perWorldPatterns give <id>`（或 `giveAll`）获取对应的远古卷轴。
- 表中的“说明”只写主要行为；同一图案在不同 iota 类型下有不同重载（如 `add` 对数字、向量、列表）。
- 输入列里的 `a, b, c` 只是占位，表示对应顺序上的任意 iota；具体类型看该图案支持的重载。
- `explode`、`get_entity`、`zone_entity` 等带查询框的图案同样按输入列从左到右压栈，例如 `zone_entity` 先压中心向量、再压半径。
- `three_suns_adrift` 的媒质消耗按三角形面积计算：`1 充能紫水晶 + ceil(面积 / 4)` 粉；退化三角形不造成伤害。
- 领域（`area`）iota 在栈中显示为 `delta(x,y,z)(...)`，浅红色；生成消耗 `13 + ceil(面积 / 8)` 粉。玩家死亡时其领域消失；领域咒核可用媒质代替玩家维持领域。
- `hexcreating` 图案依赖 Create：目标不是对应方块实体时，读取类图案返回 `null`，写入类图案触发 `MishapBadBlock`。
- 经验球、掉落物等实体的类型判定可用 `type/entity_<id>` 字面量，例如 `type/entity_experience_orb` 对应 `minecraft:experience_orb`。
