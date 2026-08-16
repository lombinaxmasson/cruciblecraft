# CrucibleCraft 阶段档案 · T20

> **工作副本。** 权威关闭档案为仓库根目录
> 《[../CrucibleCraft-阶段档案-T20.md](../CrucibleCraft-阶段档案-T20.md)》。

> 阶段：T20 · O-29 世界生成保真  
> 状态：✅ `T20_READY`  
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`  
> 权威产物：`tools/t20_readiness.json`

## 1. 关闭判据

T20 没有把 129 条 CrucibleCraft catalog identity 伪称为 129 条 canonical
GT6 large vein。固定源码 replay 实际观察到 40 条 large、75 条显式 small 与
1 条动态 `RANDOM_SMALL_GEM_ORE` 规则；129 条 CC identity 全部进入唯一分类，
有 GT6 字段的值保留 source fact 与 transformation id，无唯一 GT6 worldgen
事实的值显式标为 `DESIGN_POLICY` 并排除在 GT6 parity 声明之外。

独立 expected、authored schema v2、configured/placed feature 和运行时 profile
集合双向闭合；`PLACEHOLDER / UNVERIFIED / unclassified = 0`。全部 129 条
profile v2 可从运行时 registry 解码并真实放置，feature id 与 salt 保持不变，
RecipeMap publication 增量为零。

## 2. T20a · 固定来源

- `GT6WorldGenerator.java`：固定 large-vein 3×3 chunk 选择与 weight 语义；
- `WorldgenOresLarge.java`：固定 height、weight、density、size 与四材料角色；
- `WorldgenOresSmall.java`：固定 height、amount 与逐 chunk 尝试语义；
- `Loader_Worldgen.java`：固定 40 large、75 explicit-small 与 dynamic-small
  声明；
- `MT.java`：固定 source material id/name/token 映射。

五个 blob、tree、SHA-256、人工锚点和提取命令进入
`tools/t20_worldgen_source_policy.json`；规范化全集进入
`tools/t20_gt6_worldgen_source.json`。完整 replay 与 compact check 分离，缺少
raw/cache 时不得冒充 full replay。

## 3. T20b · 129-entry 分类

| 分类 | 数量 | 语义 |
|---|---:|---|
| `EXPLICIT_SMALL_SOURCE` | 39 | 直接消费固定 small row；height/material 为 source-backed，NeoForge deposit projection 为 source-derived |
| `RANDOM_SMALL_GEM_SOURCE` | 28 | 消费固定动态规则 `minY=5 / maxY=250 / amount=1` |
| `UNIQUE_LARGE_ROLE_SOURCE` | 6 | 消费唯一 large-role fact；无法注册的 source role 有逐角色替换记录 |
| `DESIGN_POLICY_NO_GT6_WORLDGEN_FACT` | 56 | 无唯一可适用 GT6 worldgen fact；使用显式确定性 CC 分布，不计 GT6 parity |

状态汇总为 73 `SOURCE_DERIVED` + 56 `DESIGN_POLICY`。所有截断、角色替换、
small→deposit 投影、overworld acquisition 与 identity-preserving profile
都有 transformation id 和可复算公式。当前有 91 种不同几何签名；28 条随机
宝石共享同一 source 规则属于已声明分布，不是未声明压平。

## 4. T20c · Authored 与生成树

- `worldgen_ore_veins.schema.json` 固定 129 条 profile v2 的字段和上界；
- `t20_worldgen_expected.json` 从 T2c ledger 与固定 source evidence 独立生成；
- 生产 builder 只消费显式 authored rows，不复用 expected selector/default；
- 129 configured + 129 placed + 1 biome modifier = 259 个 T20 ore 文件；
- 加 2 个 fluid deposit 的 configured/placed 后，catalog root 保持 263 文件；
- 连同 T2 的 11 个文件，全部 worldgen 资源保持 274 文件；
- 少一条、多一条、stale 文件或任一字段变异均使 currentness/双向门禁失败。

## 5. T20d · 运行时与存档边界

运行时共有 134 条 large-vein configured feature：5 条历史 profile v1 与
129 条 T20 profile v2。GameTest 从 registry 枚举、解码并真实放置全部集合，
同时检查 `profile_id == registry id`。

旧世界边界采用身份保持策略：

- configured/placed feature id 与 salt 不变；
- 已生成区块不追溯重写；
- 普通矿石方块不持久化生成器几何，因此每块新增 profile 存档字节为 0；
- 未探索区块使用当前 profile v2；
- codec 仅接受 profile 1/2，未知未来版本 fail closed；
- T11 的 `crude_oil` / `natural_gas` deposit identity 与行为不变。

## 6. 三轴账

**Closure**

- source facts：40 large / 75 explicit-small / 1 dynamic；
- catalog：129 / configured：129 / placed：129；
- registered ore material：137；runtime large vein：134；
- `unclassified = 0`，pending = 0。

**Fidelity**

- 129 expected == authored == generated；
- 73 `SOURCE_DERIVED` + 56 `DESIGN_POLICY`；
- `PLACEHOLDER = 0`，`UNVERIFIED = 0`；
- mutation test 会捕获任一 authored 字段变化；
- 不宣称 DESIGN_POLICY 行或 NeoForge projection 与 GT6 1.7.10 placement
  algorithm 行为全等。

**Load**

- catalog generated：263 文件 / 139,051 bytes；全部 worldgen：274 文件；
- codec 静态上界：129 profile decode、516 weighted-state decode、每 role 最大 1；
- catalog 期望命中 `0.075837/chunk`，含历史 T2 后
  `0.12336712640525413/chunk < 0.15`；
- persisted profile bytes per ore block = 0；
- RecipeMap 32，publication 保持 18,875 logical / 16,650 eager / 2,225 lazy；
- RecipeMap/logical/eager/lazy delta 全为 0。

## 7. 验证与交接

关闭绑定：

```text
python tools/build_t20_worldgen_source.py --check --full-replay
python tools/run_python_tests.py --suite closure
.\gradlew.bat test
.\gradlew.bat runGameTestServer
python tools/run_full_verification.py --record --new-session --source-replay
```

`T20_READY` 只关闭 O-29 的当前产品契约：固定来源被如实分类、全部 129 条
资源有非占位 profile、运行时与身份边界可执行。它不关闭第三方原生 worldgen
复刻，也不产生新的 RecipeMap 内容。T21 仍在进行中（早前写入的 T21_READY
为草稿，不作关闭证据），之后才是 T22 石油化工全量与纵深。
