# GT6 管道与线缆语义重基线详细计划

> 计划 slug：`content/gt6-pipe-cable-baseline`
> 状态：**prep 审计已关闭**（2026-09-13）。本文件位于 `card-plans/closed/`。
> 不创建 `capability.json`，不占 unique-active，不改 `src/main`，不发布配方。
> 性质：跨域连接件基础设施审计卡，不新增 GT6 MTE 1,817 行分母。
> 机器可读交付：`tools/waves/content/gt6-pipe-cable-baseline/**`。
> GT6 Java/tick revision：
> `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 关闭只表示 prep 合同完成，不是 Fluid / Item / EU / Redstone 的
> `runtime_ready` 或 `player_complete`。

```text
lane                         = closed
capability_slug              = content/gt6-pipe-cable-baseline
unique_active_wave           = null
catalog_family               = connector-runtime-baseline
catalog_family_count         = overlaps connector 663 + redstone 3; no new denominator
prep_row_count               = 0; cross-cutting audit, not R0 row ownership
dispositions                 = source_rebase + identity_reaudit + art_migration
owns_families                = 0
new_processing_machine       = 0
prep_owned_paths             = docs/history/card-plans/closed/GT6管道与线缆语义重基线详细计划.md
                               tools/gt6_pipe_cable_baseline.py
                               tools/build_gt6_pipe_cable_baseline.py
                               tools/waves/content/gt6-pipe-cable-baseline/**
                               tools/tests/test_gt6_pipe_cable_baseline.py
landing_owned_paths          = none; future child cards only
landing_depends_on           = registry/catalog-modern-ids; exact GT6 source replay
runtime_status               = prep audit complete; runtime not executed
```

## 0. 结论与开卡理由

早期管道、物品管和线缆借鉴 GTCEu/GTM 的代码组织、模型尺寸和连接交互，
本身不构成错误。错误发生在把参考实现的对象身份或运行时语义也当成 GT6
合同时。

本卡冻结以下判断：

1. GT6 本地 `gt6_code/gregtech6` 是 Java、tick、规格、注册身份和配方语义
   的唯一权威；`gt6_referencable_port_code/gregtech6_w` 是贴图和模型来源。
2. 本地 `gtceu_code` 只保留为命名、几何和现代连接 UX 的参考，不得用来
   覆盖 GT6 的容量、损耗、路由、覆盖层、红石或电能合同。
3. 当前 CC 的流体管、物品管、EU 线缆和 GT6 红石线不能因为共享
   `Gt6StyleConnections`、材料目录或模型生成器而共享网络语义。
4. `red_alloy/wire`、`signalum/wire`、`lumium/wire` 属于 GT6
   `OP.wireGt01` 红石线；GTCEu 中 Red Alloy 的 ULV EU cable 定义不能
   反向决定 CC 身份。绝缘红石 `27006/27056/27506` 另行处理。
5. “配方已经映射”不等于“可放置 MTE 已实现”。每个 meta 必须重新核对
   live item、live BlockItem、方块实体、连接规则、贴图层和存档处置。

本卡的目的不是立即修复所有差异，而是先建立一张能阻止继续沿用 GTCEu
假设的 GT6 对照卡。未完成本卡的 source/identity/behavior gate 之前，
不得把相关管线或红石线继续晋级为新的 `player_complete`。

---

## 1. 权威来源与禁止误用

### 1.1 来源优先级

- Java/tick：`gt6_code/gregtech6`
  - `MultiTileEntityPipeFluid`
  - `MultiTileEntityPipeItem`
  - `MultiTileEntityWireElectric`
  - `MultiTileEntityWireRedstone`
  - `MultiTileEntityWireRedstoneInsulated`
  - `Loader_MultiTileEntities.addFluidPipes`
  - `Loader_MultiTileEntities.addItemPipes`
  - `Loader_MultiTileEntities.addElectricWires`
- 美术：`gt6_referencable_port_code/gregtech6_w`
  - `materialicons/<set>/pipe*.png`
  - `materialicons/<set>/wire*.png`
  - overlay、insulation、restrictor 和连接件相关层
- 当前 CC 合同与实现：
  - `PipeCatalog`
  - `FluidPipeBlock` / `FluidPipeBlockEntity`
  - `ItemPipeBlock` / `ItemPipeBlockEntity`
  - `ElectricalConductorCatalog`
  - `CableBlock` / `CableBlockEntity` / `CableNetworkTraversal`
  - `Gt6StyleConnections`
  - 材料生成、注册、datagen、recipe operand 和 GameTest
- GTCEu/GTM：`gtceu_code`
  - 只作为“早期实现从何处借鉴”的 provenance 证据；
  - 不得作为 GT6 行为或缺失身份的替代来源。

上述本地 reference tree 只读，不复制、删除、链接或从网络重新获取。

### 1.2 禁止的结论

- 看到 GTCEu 有一个同名 `wire` / `cable`，不能推导它就是 GT6
  `wireGt01` / `cableGt01`。
- 看到材料有 `generates_wire` / `generates_cable`，不能推导该形式已经
  有可放置 BlockItem、EU 网络或红石网络。
- 看到当前 CC 有相同尺寸的 Block，不能推导 capacity、loss、tick cadence、
  side I/O 或 cover 语义相同。
- 缺少真实 GT6 形态时不得用错误材料、原版物品、`programmed_circuit`、
  单一 generic pipe/cable 或“能放下就算完成”的 stand-in。
- 不能为了消除 JEI 双物品而直接删除 dummy；必须先完成逐 meta
  identity-resolution、存档和贴图合同。

---

## 2. 范围与不范围

### 2.1 本卡审计范围

#### A. 流体管

核对材料资格、`pipeTiny` / `pipeSmall` / `pipeMedium` /
`pipeLarge` / `pipeHuge` / `pipeQuadruple` / `pipeNonuple` 的注册、
直径、容量、tank count、最大温度、blocking、gas/acid/plasma/magic/
contact/flammability 以及取得配方。

核对 CC 当前的：

- `PipeCatalog` 规格映射和缺失 gauge；
- 每段 tank、transfer cadence、源优先/目标优先和分配顺序；
- 流体温度、气体、酸、等离子、魔法流体的失败/破坏/伤害语义；
- machine/container/cauldron 等端点和跨区块加载；
- 连接面、盖板、wrench 九宫格和邻接更新。

#### B. 物品管

核对普通 medium/large/huge 与 restrictive medium/large/huge 的
`stepSize`、`invSize`、带宽、10 tick 搬运、管内库存、输入输出禁用位、
过滤和路由优先级。

核对 CC 当前的：

- `ItemPipeCatalog` / `PipeCatalog` 的规格与 live BlockItem；
- `ItemPipeBlockEntity` 的无库存 handler、cover pump、route cache、
  recovery buffer 和 topology invalidation；
- restrictive 只存在 dummy 还是已经存在真实管段；
- item pipe 与 hopper、普通 inventory、cover network 的边界。

#### C. EU 线缆

核对 `addElectricWires` 的材料资格、`wireGt01..wireGt16`、
`cableGt01/02/04/08/12`、电压、单线安培、线规带宽、wire/cable loss、
绝缘、接触伤害、过载和烧毁行为。

核对 CC 当前的：

- `ElectricalConductorCatalog` 与 material `electrical_by_specification`；
- `CableBlock` / `CableBlockEntity` 的 packet、DFS、逐段损耗、端点插入、
  overload、burn counter、保存和 chunk unload；
- 多线规是否只是普通 item 而不是可放置 MTE；
- cable width、insulation、overlay、inventory/world render 和 tint；
- EU、LU、红石与物流管网络是否错误混连。

#### D. 红石线边界

不把红石线并入 EU 重构，但必须把它作为本卡的边界回归：

- `27000/27050/27500` 保持独立于 `CableBlock`、`ENERGY` capability 和
  `CableNetworkTraversal`；
- `Gt6StyleConnections` 只能复用 GT6-compatible 的连接 UX；
- 复核当前 `RedstoneWireNetwork` 的原版输入方向、混材 hop 的发送端损耗、
  `REDSTONE_SINKS`、每 tick vanilla cache、material kind 绑定和传播收敛；
- 复核 `27006/27056/27506` 是未来绝缘红石线，不是 GTCEu EU cable；
- 本卡不直接修改已关闭红石线卡；发现行为 blocker 时，另开 correction child
  或在执行前重新签发其 runtime 合同。

#### E. 身份、重复物品与 GT6 贴图迁移

对管、物品管、EU 线、红石线涉及的每个 Loader meta 建立 ledger，至少包含：

`meta`、Loader 注册点、GT6 class/tag、材料、`OP.*` 目标、规格/尺寸、
当前 registry path/kind、live item、live BlockItem、live block、
处置、存档策略、贴图 selector、source hash 和后续 child owner。

物品处置只允许：

- `already_shared`：同一 meta、材料、class/spec，且 runtime registry 已有
  正确 live item / BlockItem；
- `fold_live_block`：dummy 与已有精确 live BlockItem 是同一对象，折回活主机；
- `upgrade_live_item`：canonical 材料 item 保留同一 id，原地升级为真实
  BlockItem；
- `keep_distinct` / `in_place`：对象不同或无 live host，在 canonical modern
  id 上实现真实对象并迁图。

贴图迁移必须覆盖 blockstate、block model、BlockItem model、底图、overlay、
insulation/restrictor 层、tint index、材料 tint 以及 inventory/world render。
目标路径使用 CC registry family，不保留 `gt.meta.*` 或数字 multiitem 名称。
每一份复制都要记录 `source -> gt6_source -> destination`，禁止用
`multiblock_casing`、`pipe_filter_cover`、`conveyor_cover` 或原版方块充当
连接件贴图。

### 2.2 本卡不做的事情

- 不修改 `src/main`、`src/generated`、`src/recipe_generated` 或资源；
- 不创建 capability、不创建 unique-active、不生成 production lock；
- 不改变 R0 `1,817` 行 disposition，不把本卡伪装成新的 MTE family；
- 不直接实现 quadruple/nonuple、restrictive、未注册 wire gauge 或绝缘红石；
- 不补生存配方，不用 stand-in 把 `explicitly_blocked` 改绿；
- 不做全量历史 machine/multiitem 美术债；
- 不把 GTCEu 的 EnergyNet、fluid net 或 item net 直接移植为 GT6 合同。

---

## 3. 已确认的重基线风险

以下不是本卡的“猜测项”，而是开卡时必须保留的已知风险；最终处置仍
要由 source replay 和 runtime evidence 决定。

### 3.1 Red Alloy 的身份冲突

GTCEu 的 Red Alloy 使用 `cableProperties(GTValues.V[0], 1, 0)`，是 ULV
EU cable。GT6 Loader 则把 Red Alloy、Signalum、Lumium 的 `OP.wireGt01`
绑定到 `MultiTileEntityWireRedstone` 的 27000、27050、27500；同一 Loader
还把 `OP.cableGt01` 绑定到绝缘红石 27006、27056、27506。

因此：

- `*/wire` 不能进入 `ElectricalConductorCatalog`；
- `*/cable` 不能被解释为 GTCEu EU cable；
- `redstone_wire/*` dummy 与 `*/wire` 的重复物品必须逐 meta 收口；
- `lumium/wirelamp` 的显示名与 `lumium/wire` 的 canonical registry path
  必须分别记录，不能用同名猜测覆盖身份。

### 3.2 流体管不是只差名字

GT6 有 7 种 fluid pipe gauge，且 quadruple/nonuple 是多 tank 规格；
CC 当前 PipeCatalog 只接入 5 种普通 gauge。CC 还采用 GTM 风格的
5-tick 分段分配，而 GT6 的 source/tank 更新、容量提示和危险流体处理
不能由同名 `FluidPipeBlockEntity` 自动证明等价。

特别要防止把以下材料 metadata 当成已经实现的行为：
`plasmaProof`、`magicProof`、`contactDamage`、`flammable`。每一项都必须
在 runtime 读路径和失败测试中找到证据，否则记录为 gap。

### 3.3 物品管缺少多个 GT6 语义面

GT6 item pipe 有管内库存、`mDisabledInputs` /
`mDisabledOutputs`、10 tick 发送以及 restrictive 规格。CC 当前 route cache
和 cover pump 是可复用的架构素材，却不能自动替代这些语义。尤其不能把
`item_pipe_tile/*` dummy 机械删掉，或把 restrictive item 当普通 item pipe。

### 3.4 EU 线缆需要“规格已对齐”而不是“代码像”

当前 CC 的 packet/DFS/逐段 loss/overload 方向主要按 GT6 electrical
contract 组织，但仍需逐 gauge、逐 material、逐 BlockItem 检查。GTM/GTCEu
的几何宽度和 EnergyNet 路径实现不能证明 GT6 的规格完整。`wireGt03`、
`wireGt05` 等普通材料 item 也不能在没有 live conductor host 时宣称
可放置。

### 3.5 连接 UX 与网络语义必须解耦

`Gt6StyleConnections` 的放置、九宫格和 cutter/wrench 交互可以作为共享
helper 候选；但 `sameNetwork`、`canConnect`、side I/O、covers、capability
和 topology invalidation 必须按连接件家族分开验证。共享 helper 不是共享
Fluid/EU/Item/Redstone network 的授权。

---

## 4. 后续拆卡与交付物

本卡已完成 R0/R1 审计。R2–R6 只冻结 envelope，不得把四个网络一次性塞进
一张 runtime 实现卡，也不得借本卡改 `src/main`。

机器可读权威：`tools/waves/content/gt6-pipe-cable-baseline/`。

### R0：source authority 与 provenance — 已完成

- GT6 revision `3703e40308c8c030763fd6297dea8b210d2a77b1`，blob hash 见
  `source_authority.json`（`git show` 钉死，不读可能 CRLF 漂移的工作树）。
- 行号：`MultiTileEntityPipeFluid` 83/91/235；`PipeItem` 76/173/194；
  `WireElectric` 71/145/170；`WireRedstone` 35/51/79；
  `WireRedstoneInsulated` 108/121/168；`ITileEntityRedstoneWire` 32/43；
  Loader `addFluidPipes` / `addItemPipes` / 红石 1893–1902 /
  `addElectricWires` 1914+。
- 贴图 selector 见 `art_selector_contract.json`：`pipetiny.png` 等，
  **没有**独立 `cable.png`；电缆是 `wire.png` + `iconsets/insulation_*.png`。
- GTM/GTCEu 只作 geometry/UX 与 Red Alloy 误读 provenance；不得覆盖 GT6。

### R1：identity / duplicate / migration ledger — 已完成

- catalog 666 行（connector 663 + redstone 3）全部进入 ledger；
  GT6 展开另有 366 行 catalog 外（restrictive、未进 catalog 的 gauge、
  绝缘红石 27006/27056/27506）。
- 当前 catalog 处置：`already_shared` 15、`fold_live_block` 223、
  `upgrade_live_item` 184、`keep_distinct` 244。
- 红石 27000/27050/27500 已是活 `*/wire` BlockItem（`already_shared`）。
- `content/electric-wire-cable-mte-fold` 的 259 mapped 只记 `recipe_mapped`；
  例如 `tin/wire` 仍要 `fold_live_block` 撤 `electric_wire/1x_tin_wire`，
  `lead/double_wire` 是 `upgrade_live_item`。
- 存档：无 NeoForge alias；折回 dummy 变空气或由 child 写一次替换。

### R2：Fluid child envelope — `content/gt6-fluid-pipe-runtime`

- 五种已接入 gauge 的 tick/容量/失败语义修正；
- quadruple/nonuple、plasma/magic/flammable/contact 保持 blocked；
- 依赖本卡 + `registry/catalog-modern-ids`；无 stand-in。

### R3：Item child envelope — `content/gt6-item-pipe-runtime`

- 管内库存、`mDisabledInputs/Outputs`、10 tick、restrictive 三规格；
- cover pump 不能替代 GT6 库存合同；无 stand-in。

### R4：EU child envelope — `content/gt6-eu-wire-cable-runtime`

- 逐 `wireGt01..16` 与 `cableGt01/02/04/08/12` 的 live BlockItem；
- 红石材料不得进入；缺前缀 `wireGt07/09/10/14` 保持 blocked。

### R5：Redstone correction — `content/gt6-redstone-wire-correction`

- 身份已由 `content/mte-redstone-wire` 关闭；本 child 只补
  **发送端**混材损耗、vanilla 每 tick cache、sink；
- 不得接入 `CableBlock` / ENERGY / `CableNetworkTraversal` / PipeTopology。

### R6：art / migration closeout — `content/gt6-connector-art`

- 仅复制本地 `gregtech6_w`；fold 行新增 png 数为 0；
- `player_complete` 前必须有 manifest。绝缘红石另开
  `content/gt6-redstone-insulated`。

---

## 5. Prep 验收门

- [x] GT6 source authority 已固定，GTCEu 参考范围已写死；
- [x] Fluid / Item / EU / Redstone 四个 domain 的 behavior-gap matrix 已逐项列出；
- [x] 管、物品管、EU 线缆、红石线的网络隔离和 capability 边界有证据；
- [x] connector 相关 identity-resolution ledger 覆盖 live item、BlockItem、
      dummy、存档和迁图处置；
- [x] `generates_*`、recipe mapped、realized_natively 均未被误读为 runtime；
- [x] GT6 贴图 selector、manifest 字段和禁止 placeholder 规则已冻结；
- [x] R2–R6 的 child ownership、依赖、blocked/no-stand-in 边界明确；
- [x] 预期 GameTest、registry census、art manifest 和 source currentness
      入口已列出；
- [x] 未创建 capability、未修改 runtime、未发布配方。

本卡完成只表示“GT6 管道与线缆重基线的 prep 合同完成”。它不表示
Fluid Pipe、Item Pipe、EU Wire/Cable 或 Redstone Wire 已达到
`runtime_ready`，更不表示 `player_complete`。

重放：`python tools/build_gt6_pipe_cable_baseline.py --check`。

---

## 6. 相关计划与依赖

- [MTE 全量 Prep 总索引](../prep/MTE全量Prep总索引.md)：提供 1,817 行身份、
  物品重复和迁图总合同；本卡不新增其分母。
- [MTE 连接件详细计划](../prep/MTE连接件详细计划.md)：提供 connector family 的
  GT6 identity prep；本卡负责把“已有 CC 管线实现”重新接到行为和身份审计。
- [导线电缆 MTE 折回详细计划](导线电缆MTE折回详细计划.md)：
  只提供 recipe operand fold 背景，不能代替本卡的 live BlockItem 审计。
- [MTE 红石线详细计划](MTE红石线详细计划.md)：当前只作为
  `wireGt01` identity fold 的已关闭记录；本卡发现的行为问题必须另行签发
  correction child。
- [目录身份现代 id 详细计划](目录身份现代id详细计划.md)：提供
  改名不合并活主机、无 alias 的迁移前提。
- [GT6 贴图纪律](../../../current/gt6-art-policy.md)：提供本地源图、manifest
  和禁止 placeholder 的美术合同。

---

## 7. 审计结论（2026-09-13）

### 7.1 不得再沿用的 GTCEu 假设

1. 同名 `wire` / `cable` 不是 GT6 `wireGt01` / `cableGt01`。Red Alloy 在
   GTCEu 是 ULV EU cable，在 GT6 是红石线。
2. `generates_wire` / `generates_cable` 只说明材料生成形态，不是可放置 MTE。
3. 配方 mapped 259 不是 dummy 删除集合；`lead/double_wire` 仍要
   `upgrade_live_item`。
4. `realized_natively` 不是 fold 授权。钢/镀锌五种活管是 `already_shared`；
   同材料 quadruple 仍是 `keep_distinct`。
5. `Gt6StyleConnections` 只共享放置/九宫格/扳手钳 UX，不共享四套网络。

### 7.2 行为缺口（详见 `behavior_gap_matrix.json`）

| Domain | 已对齐或可复用 | 缺口 / blocked |
| --- | --- | --- |
| Fluid | 五规格容量写入材料 metadata；gas/acid/温度有失败计数 | GT6 每 tick even/odd PRE/PR2，CC 是 5 tick；单 tank；plasma/magic/flammable/contact 未读路径；quad/nonuple 未注册 |
| Item | cover pump / route cache 可作架构素材 | 无管内库存；无 `mDisabledInputs/Outputs`；5 tick 而非 10 tick；restrictive 三种 blocked |
| EU | packet/DFS/逐段 loss/overload 方向按 GT6 组织；29+115 可放置 host | `wireGt02..16` 多数只是 item；奇数规缺前缀；须逐规格重放 |
| Redstone | 三身份已是 `*/wire` BlockItem；无 ENERGY / CableNetwork | 混材 hop 用接收端 loss，GT6 用发送端 `getRedstoneMinusLoss`；无 vanilla 每面 cache |

### 7.3 网络隔离证据（`network_isolation.json`）

- `CableBlock` 有 `ModCapabilities.ENERGY` 和 `CableNetworkTraversal`。
- `RedstoneWireBlock` / `AbstractPipeBlock` 没有 ENERGY，也不走电缆 DFS。
- `PipeTopology` 只在流体/物品管上失效；电缆和红石不得加入。
- `sameNetwork`：EU/LU 按 `EnergyType`、红石只连红石、管按 `PipeCatalog.Kind`。

### 7.4 贴图

`gregtech6_w` 有 41 个 materialicons 套。管道文件是 `pipetiny.png`…
不是 `pipe.png`。电缆没有 `cable.png`。红石已迁 copper `wire.png` /
`wire_overlay.png`。本卡不复制 png。

### 7.5 未做

未创建 capability、未改 Java/资源/配方、未签发 R2–R6 unique-active。
未完成本卡列出的 source/identity/behavior gate 之前，不得把相关管线或
红石线继续晋级为新的 `player_complete`。
