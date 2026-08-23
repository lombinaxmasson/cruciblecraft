# CrucibleCraft 阶段档案 · T34

> 阶段：T34 · 开局与 T18 青铜能源链表现层
> 状态：● 已关闭（`T34_GT6_ART_READY`，2026-08-22）
> 权威产物：[`tools/t34_gt6_art_manifest.json`](../../../tools/t34_gt6_art_manifest.json)
> 闭卡 profile：`python tools/verify.py integration --profile presentation`

## 1. GT6 重开关闭判据

- Receipt 固定 19 个实际 CC runtime targets，并为每个导入资源验证 GT6 原始文件与目的文件
  的 SHA-256、模型解析、纹理解析、active `.mcmeta`、tint identity 和 blockstate contract。
- Firebox 映射 GT6 Brick Burning Box (1199)，Diesel Engine 映射 `motor_liquid` (9147)，
  `burning_gas_generator` 映射 GT6 Gas Burning Box (1602)，而非独立小型 Gas Generator。
- Steam Boiler Tank、Steam Engine 和 Electric Dynamo 分别绑定 1202、1302、10111；
  Dynamo 固定 TinAlloy 着色。包含 `overlay_active` 的 fuel engine、gas box 和 dynamo 有
  `LIT` runtime projection。
- CC 未登记的 Solid/Liquid Burning Box 与独立小型 Gas Generator 固定为 `not_registered`，
  严禁用现有方块、模型或贴图伪装其已实现。
- `presentation` integration 通过；两次 `runData` 成功后 receipt 仍 current。`T32-VD-001`
  保持 open；没有 recipes profile 通过声明、玩家发行、RC soak 或 GA 声明。

## 2. 首次闭卡撤回记录

> 原状态：○ 已撤回（`T34_PRESENTATION_READY`，2026-08-22）
> 原产物：[`tools/t34_presentation.json`](../../../tools/t34_presentation.json)
>
> 撤回原因：原范围仅覆盖 three-member 自制表现层，未核对 GT6 原始资源，且未覆盖已登记的
> 起步物件和 T18 青铜能源链。以下内容只保留原错误闭卡的可追溯记录。

## 3. 首次闭卡判据（已撤回）

T34 将 T33 S0 evidence 收敛为可审计的原版手工开局路线：stone anvil 为 S1，ceramic
crucible 和 firebox 为 S2。三个成员都有明确表现分类，不能再使用未声明的共享占位外观。
本卡不将保守的全局 operand closure 误称为完整玩家时间线。

## 4. 首次闭卡三轴（已撤回）

**Closure**

- receipt 固定三个 members、六个 T33-reachable 原版种子和五条手工路线；
- anvil/crucible 的模型必须绑定设备专属 mask，全部可着色面保留 `tintindex: 0`；
- firebox 的 LIT blockstate 必须投影 authored unlit/lit 模型，item 使用 unlit model；
- 输入模型、PNG、datagen 源、生成资源和路线全部受 hash currentness 保护；
- T18/T19 的既有 firebox/语言 resource receipt 已与本卡资源身份一致。

**Fidelity**

- anvil/crucible 为 `ART_DERIVED`，使用设备专属灰度 mask 和既有 material tint；
- firebox 为 `ART_AUTHORED`，使用原创炉膛、炉排和 lit/unlit 纹理；
- 不主张这些视觉对象与任何外部模组模型或纹理等价。

**Load**

- block/item 注册和 recipe publication delta 均为 **0**；
- 增量限定为 **2** 个模型、**9** 个 16×16 texture 文件和同一 firebox 的 state projection。

## 5. 首次闭卡验证（已撤回）

- scoped T34 `verify.py dev`：退出 0；
- `verify.py integration --profile presentation`：退出 0；
- `gradlew.bat test --no-daemon`：退出 0；
- 默认 datagen 两次成功运行：排除 `.cache` 后 **3,068** 个 generated resources 的
  SHA-256 一致，为 `1712c76423373c510077d8cdabfe0dc33d140b15eb2b641085a7a98a6ef6c610`；
- `t34_presentation.json`：`status = T34_PRESENTATION_READY`。

NeoForge Maven 在其中一次尝试中重置 `asm-util-9.5` 下载连接；后续默认重试成功，该瞬态
网络错误不表示资源生成或验证通过。

`T32-VD-001`（T5 chemical readiness source-hash drift）仍保持 open。T34 没有将 recipes
profile 标记为 passing、waived 或“带债通过”。

## 6. 首次闭卡交接（已撤回）

下一张唯一 active 内容卡是 **T35 · 1.x 全域 census 与预算基线**。它以 T34 的
first-hour presentation receipt 作为输入，并在消费 recipes profile 前处理
`T32-VD-001`；不在 T35 前重开 T34 的 wider texture refresh。
