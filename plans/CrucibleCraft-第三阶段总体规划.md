# CrucibleCraft 第三阶段总体规划

> **工作副本，已停止维护进度。** 2026-08-14 起以仓库根目录
> 《[../CrucibleCraft-第三阶段总体规划.md](../CrucibleCraft-第三阶段总体规划.md)》为权威。

> GregTech 6 → Minecraft 1.21.1 NeoForge 移植 · 第三阶段 T13–T19
> 状态：✅ 已归档；T13–T19 已关闭，当前入口为第四阶段 T20–T26
> 基线：2026-08-06，第二阶段 `T12_READY`
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 固定 dump：95 RecipeMap / 720,841 source rows
> 预发布清理：已完成；不改变 T19 或第三阶段状态

---

## 0. 这一阶段是什么

第一阶段搬事实，第二阶段搭通用架构，第三阶段开始在已关闭的架构上**按分母批量填充工业阶梯**。

“填分母”不是把所有 GT6 类文件一次实现完，而是：

1. 先把 source 总量规范化成机器可读的 canonical kind；
2. 把每项分成 `in_scope` / `deferred_with_reason` / `out_of_scope`，做到 `unclassified = 0`；
3. 只对本阶段 `in_scope` 的工业机器、能源链和物流面实施；
4. 每批同时交 closure / fidelity / load 三份账。

因此本阶段不使用以下伪完成度：

- `18,875 / 720,841 = 2.6%`：live publication 与 source dump row 不是同一语义单位，只能说明规模差距。
- “约 65 cover / 44 multiblock”：类文件数混有 abstract、helper 和 variant，T13 规范化前不能当实现分母。
- “25 个 processing kind 全部分档”：T12 ledger 已证明其中有 fixed utility 和 deferred 项，不能强行乘 tier。



### 文档层级

- 规则与不变量：《[CrucibleCraft-总体规划.md](CrucibleCraft-总体规划.md)》
- 第二阶段关闭摘要：《[CrucibleCraft-第二阶段总体规划.md](CrucibleCraft-第二阶段总体规划.md)》
- 历史完整卡：《[CrucibleCraft-阶段档案-T7-T9.md](CrucibleCraft-阶段档案-T7-T9.md)》、《[CrucibleCraft-阶段档案-T10-T12.md](CrucibleCraft-阶段档案-T10-T12.md)》、《[CrucibleCraft-阶段档案-T13-T16.md](CrucibleCraft-阶段档案-T13-T16.md)》、《[CrucibleCraft-阶段档案-T17.md](CrucibleCraft-阶段档案-T17.md)》、《[CrucibleCraft-阶段档案-T18.md](CrucibleCraft-阶段档案-T18.md)》与《[CrucibleCraft-阶段档案-T19.md](CrucibleCraft-阶段档案-T19.md)》
- 当前规划与执行入口：《[CrucibleCraft-第四阶段总体规划.md](CrucibleCraft-第四阶段总体规划.md)》；T20 已关闭，T21 进行中（早前写入的 T21_READY 为草稿状态）

**唯一硬规则不变：任何时刻最多一个 T 阶段进行中。**

---



## 1. 阶段结构

```text
T13 可比较分母
  │
  ▼
T14 载荷架构与压缩证明
  │
  ▼
T15 T12 残余与获取证据
  │
  ▼
T16 RU/KU 机器档位
  │
  ▼
T17 HU/EU 机器档位
  │
  ▼
T18 蒸汽、燃油与能量转换
  │
  ▼
T19 Cover 与管道获取闭包
```

T13–T19 全部关闭，本文件只保存第三阶段关闭语义。T20 后续已独立关闭；
其实现与证据不回写为第三阶段交付。

### 开工基线


| 账目               | T12 关闭值                                | 第三阶段规则                                                             |
| ---------------- | -------------------------------------- | ------------------------------------------------------------------ |
| datapack entries | 3,223；soft 6,000 / hard 6,600          | T14 Extruder compact 减少 2,762 条 authored entry                     |
| publication      | 18,875 logical；16,650 eager；2,225 lazy | logical hard 21,000；eager soft 18,000；lazy soft/hard 16,000/56,000 |
| compression      | 5.856 logical/authored                 | 只证明当前总账；Extruder 压缩比不得外推其他 family                                  |
| reload / index   | 1,397 ms / 54 ms                       | server soft/hard 5/10 s、500/1,000 ms；client 与内存另有实测门禁              |
| machine ledger   | 25 processing + 12 non-spec            | T13 扩成 GT6 canonical 分母                                            |
| machine variants | 3 kind × 3 tier                        | T16/T17 只扩 source-tiered 项                                         |
| multiblock       | 2 JSON structures                      | 第三阶段只清分母，不做全量结构                                                    |
| cover            | 3 behavior types                       | T19 做高价值批次，不承诺全量                                                   |


---



## T13 · GT6 可比较分母清点 ●

**判据**

> 从固定 revision 与 dump 生成七类机器可读分母表：RecipeMap、OP/prefix、ITEMGENERATOR domain、machine kind/tier、cover kind、multiblock kind、energy identity。每表先规范化 canonical kind，再逐项分类并做到 `unclassified = 0`；后续阶段只能引用本表，不从 Wiki、CC 当前数量或类文件个数反推。

**依赖**：T12 已关闭；固定 revision、两个 dump blob 与当前 source-replay 可复核。

**来源身份规则**

- 事实身份由 revision / blob hash、文件路径、symbol 或 extraction key、规范化 row key 共同组成。
- 行号只用于人工定位；源码移动不能改变事实身份。
- 720,841 行不要求每行手写 Java 行号；dump row 使用稳定 map id + source row key / digest。
- canonical kind 必须去除 abstract base、helper、compat wrapper 与仅数值 variant。

**开工门禁**

1. 固定 revision 和 dump blob 与 T10–T12 使用的摘要一致；不一致时先更新证据，不清点。
2. 每张表有 schema version、source digest、生成命令与 currentness check。
3. 分类词表先定：至少 `in_scope` / `deferred_with_reason` / `out_of_scope` / `unclassified`。
4. `deferred_with_reason` 必须包含原因、owner、replacement condition 与 recheck point。
5. 分母表的 expected set 与生产 normalizer 分开构造，避免同错同对。

**先算的数字**

- 95 / 95 RecipeMap 的 row count、p50 / p95 / max 与 720,841 总数复核。
- `OP.java` canonical prefix 数与 CC 56 的差集。
- 25 个 ITEMGENERATOR domain 的 source denominator 与 CC 16-domain 状态。
- GT6 Basic Machine canonical kind、逐 kind tier array 和 energy identity；与本地 25 processing / 12 non-spec 只做映射，不互相代替。
- cover 与 multiblock 的 canonical kind 数；同时记录原始类文件数和排除理由。
- `TD.Energy` 全 identity 与本地 enum / topology 映射。

**子判据**

- **T13a · RecipeMap 分母**：95 / 95 分类，row 总和与 dump 一致。
- **T13b · 形态分母**：prefix 与 25 domain 全量分类。
- **T13c · 机器与能源分母**：kind / tier / energy 映射 `unclassified = 0`。
- **T13d · Cover / 多方块分母**：canonical 化规则与结果可复算。
- **T13e · CI / provenance**：七表 currentness、双向集合与变异检查进入验证。

**范围之外**

- 不实现新机器、cover、多方块或 recipe。
- 不在清点阶段设计各系统最终 runtime schema。
- 不把 `in_scope` 自动解释成“第三阶段必须全部实现”。
- 不把 source row 数转换成 publication 预测；这是 T14。

**完成信号**

1. ✅ 七类表机器可读、可复算、`unclassified = 0`。
2. ✅ O-33 关闭。
3. ✅ 本项目“GT6 完全搬完”的口径写成 canonical denominator + 明确范围，而不是模糊百分比。
4. ✅ T14–T19 的所有分母、投影和 deferred 都只引用 T13 artifact。

**关闭快照**

- RecipeMap：95 / 95，720,841 rows；p50 / p95 / max = 307 / 27,454 / 325,595。
- Prefix / domain：452 OP canonical、468 raw、56 CC mapping；25 ITEMGENERATOR domain 全分类。
- Machine / energy：2,328 expanded registrations → 96 canonical kind；20 TD.Energy identity。
- Cover / multiblock：65 raw → 47 canonical Cover；44 raw → 30 canonical multiblock。
- 七表 `unclassified = 0`、`UNIFORM_*` blocker = 0；datapack/publication 增量均为 0，状态 `T13_READY`。

---



## T14 · 载荷架构与压缩证明 ●

**判据**

> 用 T13 的 in-scope 投影和实际测量决定 recipe publication 的物化策略；以 O-26 extruder 为首个配方族完成全字段双向等价压缩证明，并重设或确认 datapack / publication / reload / index 预算。未经证明的单族压缩比不得外推到其他 RecipeMap。

**依赖**：T13。

**为什么先做**

当前 `GTRecipeMapLoader.reload()` 将规则展开为 `Prepared` snapshot 并建索引，客户端 `RecipesUpdatedEvent` 也会重展开。T12 的 18,875 publication 在当前门禁内，不证明 5× 或 20× 批量填充仍可承受。若先投影后改物化层，会重写第三阶段全部数据路径。

**开工门禁**

1. **O-26 独立期望集**：2,782 条 extruder row 的 input/output/count/duration/EU/t、fallback、forging target、`plateGem` 与 shadow order 全部进入独立 expected set。
2. **全量而非抽样**：压缩前后全集双向相等；约 27 个 IO variant / 54 条规则只是估算，不是完成证据。
3. **三种候选**：即时物化、按需匹配、混合；分别写命中路径、缓存失效、客户端同步和 fail-closed 边界。
4. **至少三个规模点**：测 server reload/index、client re-expansion、峰值内存与 steady-state lookup。
5. **按族决策**：extruder 结果只证明 extruder family；其他 map 要有自己的 selector 和等价证据。

**先算的数字**

- extruder 压缩前 row、压缩后 authored entries、publication、全量等价 case 数。
- T13 in-scope map 按 family 分组后的投影区间，而不是使用一个全局压缩比。
- server / client 三点斜率、候选上限与安全余量。
- 当前 6,600 / 21,000 是 policy ceiling 还是实际容量；若调整，记录硬上限与软预算。

**子判据**

- **T14a · O-26 压缩证明**：全字段双向相等、独立 expected、变异可报警。
- **T14b · 物化测量**：server/client/内存/lookup 四类数据齐全。
- **T14c · 架构决策**：即时 / 按需 / 混合落定，并写入总体规划 9.5。
- **T14d · 预算重设**：双轴预算和 runtime 门禁进入 CI。
- **T14e · 全量收尾**：`T14_READY`、总体规划与 full verification 绑定同一证据。

**范围之外**

- 不投影新 RecipeMap。
- 不以“预算快满”为理由删除 sparse rows。
- 不重构 EMI 分类或客户端渲染，只测它们的展开成本。
- 不承诺全 720,841 rows 进入 live publication。

**完成信号**

1. ✅ O-26 关闭，20 authored 与 2,782 logical / publication 全字段双向等价。
2. ✅ 总体规划 9.5 采用 Hybrid：557 eager + 2,225 lazy，per-epoch cache ceiling = 512。
3. ✅ datapack / eager / lazy / sync 与 server/client/runtime 预算均区分 soft / hard 并进入 CI。
4. ✅ T15–T19 共用 family load projection schema、零工作量 fixture 与 hard-fail evaluator。

**关闭快照**

- 三候选按预声明算法在 1× / 5× / 20× 真实 compact distribution 上隔离测量；误差带并列时按既定 preference 选 `hybrid`，不存在事后改评分。
- 20× Hybrid：server reload 11.041 ms、dedicated client 11.455 ms、lookup p95 946 ns、retained 21,669,656 B、JFR allocation 1,271,728 B；全部低于 hard ceiling。
- live 总账：3,223 datapack authored → 18,875 logical，其中 16,650 eager + 2,225 lazy；T8 pipe 257 保持独立 eager owner。
- epoch 发布、dedicated/integrated client、完整 EMI enumeration、事务与持久化边界均有回归；状态 `T14_READY`。

---



## T15 · T12 残余与现有档位获取证据 ✅

**判据**

> 不重开 T12 的通用层；把历史 artifact、当前 live 资源与固定来源重新对齐：Large Centrifuge 使用独立 source-backed profile，六种 casing 与九种 variant 的生存获取被运行时证明，saved identity mismatch 的 quarantine 有直接回归，最终只保留一份当前 readiness。

**依赖**：T13、T14；T12 scoped-closure。

**关闭快照**

- `tools/t12a_machine_readiness.json` 只作为 immutable T12a history；`tools/t15_readiness.json` 是当前单一真相，状态 `T15_READY`，T15a–e 全部完成且无 pending。
- Large Centrifuge 使用独立 `large_centrifuge_profile`：RU、input 512 / 512 / 4,096、capacity 4,096、parallel 16、efficiency 5,000 与 `CHEAP`，不再借单块 titanium profile 数值。
- 六种 casing recipe 与九种 machine variant recipe 共 15 个注册结果全部可达，unreachable = 0；本阶段没有新增 recipe，datapack/logical/eager/lazy/sync 与所有测量区间增量均为 0。
- kind / tier / material / energy 四类 identity mismatch 都会 quarantine，库存与原始坏 identity 可恢复；只有精确 legacy Large Centrifuge tuple 会迁移。
- live `large_centrifuge.json` 可复算为 18 个结构位：15 个 item/fluid port、2 个 energy port、1 个 controller。T12 旧 expected-16 把 controller 所在位误计成 item/fluid port；审计保留旧值、错误原因与修正 authority，不再伪造当前 16。
- live `ModProcessingMachines.CENTRIFUGE` 可复算为 item input 1、fluid input 1、item output 6、fluid output 2。15 个物理 item/fluid port 全部桥接同一 host inventory，不会扩成 15 份 matcher supply；item presence matcher 当前只有 1 份 supply，未触发 cap 12。
- `t12CapacityMatcherBenchmark` 保留 12 / 16 / 32 / 64 consuming 证据以及 16 / 32 / 64 presence-cap rejection 证据，全部在预算内；没有测量证明需要重写 matcher。

**子判据**

- ✅ **T15a · Readiness 单一真相**：历史与当前状态不再混写。
- ✅ **T15b · Large Centrifuge profile**：不借单块 tier 数值，CHEAP/source matrix 全量通过。
- ✅ **T15c · 现有 9 variant 获取**：6 casing + 9 machine recipe 运行时可达。
- ✅ **T15d · Identity quarantine**：四类 mismatch 回归关闭。
- ✅ **T15e · Port / matcher 边界**：15 个物理 item/fluid port → 1 个共享 host → item/fluid matcher 各 1 份 input supply；cap 12 当前不触发。

**范围之外**

- 不新增第四种 processing kind。
- 不重写已通过的 MachineKind / TierProfile 架构。
- 不扩大多方块 schema。
- 不把 casing 重新做成第二套材料前缀，除非固定来源和 T13 prefix ledger 要求。

**完成信号**

1. ✅ T12 scoped-closure 的已登记残余全部有当前 artifact / 测试。
2. ✅ Large Centrifuge 数值与单块 titanium centrifuge 明确分离。
3. ✅ 九种 variant 可以从注册 recipe 和可获取 operand 完成生存合成。
4. ✅ 后续 tier 批次复用同一获取与 identity 检查。

---



## T16 · RU / KU 机器档位批次 ✅

**判据**

> 按 T13 machine denominator，将本阶段选定的 source-tiered RU / KU machine kind 接入既有 Kind/Tier runtime；每个 kind 保留固定来源的 tier array、input window、parallel 与 overclock policy，RU 走 axle/gearbox，KU 走 adjacent push，新增 tier 的 publication delta 恒为 0。

**依赖**：T15。

**开工门禁**

1. 只选择 T13/T12 ledger 中 `source_tiered` 且 source identity 为 RU/KU 的 kind；fixed utility 和 deferred 项不强迁。
2. 每个 kind 的 tier array、source machine id、RecipeMap、input、parallel、parallelDuration、efficiency、overclock policy 与材料形态均已分类。
3. 当前 live `KINETIC` 引用逐项分为 current-only 改造、fixed/deferred 保留或必须删除；“未分档”不能写成“能源折叠”。
4. 新 tier 的 block/item/model/menu/EMI 与 current identity 投影先算；recipe publication 预测必须为 0。

**先算的数字**

- 本批 RU / KU kind 数和逐 kind tier 数。
- 新增 block/item/model/menu 数与生成资源数。
- 每个 variant 的 inputMin / nominal / max、effective duration、parallel throughput 与四类失败结果。
- motor / axle / gearbox 与 KU producer 的端点能力。

**子判据**

- ✅ **T16a · RU batch**：selected kinds 全档位、全来源与 axle/gearbox 纵切。
- ✅ **T16b · KU batch**：selected kinds 全档位与 adjacent-push 纵切。
- ✅ **T16c · 存档与获取**：current identity/quarantine、recipe registry 与 survival operand 关闭。
- ✅ **T16d · 零 publication**：只增加 tier 时 published recipe id 集合不变。

**关闭快照**

- `tools/t16_readiness.json` 为 `T16_READY`，T16a–d complete、pending 为空。T13 owner 分母保持 20 kind、`unclassified = 0`；本轮 selected 5 × 3 = 15 variants，preimplemented 2，deferred 13，且 20 个 tier 4 全部显式 deferred。
- selected 5 是 Lathe / Rolling Mill / Wiremill / Shredder / Press；这不表示 20 kind 全实现。其余 13 kind 继续保留 reason、replacement condition 与 recheck point。
- RU / KU 分别保持 `KINETIC_ROTATION` axle/gearbox 与 `KINETIC_PUSH` adjacent-push identity；15 个 selected variant 的资源、生存获取、blank/current 接受和 mismatch quarantine 均闭合。
- `t16_load_projection_input.json` / `t16_load_projection.json` 为 T16 zero-workload `PASS`：authored / logical / eager / lazy / cache / sync 与全部 runtime interval 增量为 0。
- 32 个 RecipeMap stable id、18,875 logical / 16,650 eager / 2,225 lazy 以及 24 个 configured map 的 EMI enumeration 均与 pre-T16 baseline 相等。新增内容只有 15 条 vanilla crafting machine-acquisition recipe，GT row 新增 0，`publication delta = 0`。
- T16/T17 的历史关闭规模只作归档；预发布 final closure 的当前预期为 538 JUnit / 501 Python / 83 GameTest，并由同一 snapshot session 绑定。

**范围之外**

- 不实现 deferred 或没有 exact kind identity 的机器。
- 不把 LU/TU/CU/MU 伪装成 RU/KU。
- 不新增 tier-specific RecipeMap。
- 不做机器升级芯片。

**验收门禁**

1. selected RU/KU denominator 全部 implemented 或显式 deferred。
2. 新机器不使用 deprecated `KINETIC`。
3. 至少一条多档 RU 网络和一条多档 KU adjacent 端到端链运行。
4. datapack/publication/load 与本地化/模型门禁通过。

---



## T17 · HU / EU 机器档位批次 ✅

**判据**

> 按固定 machine ledger 恢复选定机器的 HU / EU 身份和完整 tier array；extruder、distillery、drying、compressor、press 等不能沿用当前错误能源类型。新增档位共享 RecipeMap，bounded recipe replay 只发布 identity / producer / consumer 已闭合的 source rows。

**依赖**：T16。

**关闭快照**

- `tools/t17_readiness.json` 为 `T17_READY`，T17a–d complete、pending 为空。HU 12 + EU 16 = 28 kind denominator 保持 `unclassified = 0`；本轮 selected 3 × 3 = 9 variants，preimplemented Electrolyzer 1 kind / 3 variants，deferred 24。
- selected 3 是 Distillery / Drying / Smelter；这不表示 28 kind 全实现。Heat tier 4 deferred 10，Electric tier 4–5 deferred 32，高档位保持显式 reason、replacement condition 与 recheck point。
- 九个 HU variant 使用 bottom-adjacent firebox，三个 Electrolyzer variant 保持 buffered-EU cable reference；Electric Mixer 新档位为 0。资源、生存获取、current-only identity/quarantine 与 HU/EU 失败状态均闭合。
- `t17_load_projection_input.json` / `t17_load_projection.json` 为 delivery T17 zero-workload `PASS`：authored / logical / eager / lazy / cache / sync 与全部 runtime interval 增量为 0。
- RecipeMap stable id 32 个、18,875 logical / 16,650 eager / 2,225 lazy、24 个 configured map 的 EMI recipe enumeration 均与 T16 baseline 完全相等。新增内容只有 9 条 vanilla crafting machine-acquisition recipe，GT row 新增 0，`publication delta = 0`。
- T17 同时关闭 builder proof policy 门禁：同一 raw corpus 只有一个 canonical full-replay owner；ordinary builder 与下游 readiness 只消费带输入/输出哈希的 compact evidence，完整来源重放由显式 `source-replay` 统一执行。
- denominator、HU/EU topology、tier matrix、acquisition、current identity、load 与 currentness 均无 pending；统一 builder、双 `runData`、全 JUnit、全 GameTest 和 Python closure 绑定最终 READY。

**范围之外**

- 不一次投影全部 chemical maps。
- 不实现无法供能或无产品消费端的 machine kind。
- 不把 source-less balance template 写成 `SOURCE_BACKED`。
- 不将 HU 与环境温度 / item HEAT 混成同一物理量。

**验收门禁**

1. selected HU/EU machine denominator 关闭。
2. 当前错误能源身份具有明确 disposition；deferred 只代表有退出条件，不计作已实现。
3. HU/EU 端到端供能、输出堵塞、欠功率和 overcharge 分别可观测。
4. 新增内容在 T14 预算内。
5. 新增 builder 必须先登记统一 proof policy；禁止下游 readiness 再次重放同一 raw corpus。

---



## T18 · 蒸汽、燃油与能量转换 ✅

**判据**

> 形成可玩工业能源阶梯：firebox → boiler → steam engine，RU → dynamo → EU，oil product → fuel engine → RU；每个转换器保持来源能源身份、守恒、过载与排气事务。Fuel Engine / Burning Gas Generator 的来源分歧和 O-37 Raw Oil 身份在发布前结账。

**依赖**：T17；T11 油气纵切。

**关闭快照**

- `tools/t18_readiness.json` 为 `T18_READY`，T18a–d complete、pending 为空。29 machine kinds + STEAM/AU 两个 energy identities 均已分类，`unclassified = 0`；本批 selected 6、preimplemented reference 3、deferred 20，不宣称 29 kind 全实现。
- selected 6 为 Bronze Firebox / Boiler / Steam Engine / Dynamo / Fuel Engine / Burning Gas Generator；四条链 Firebox→Boiler→Steam Engine、Fuel→RU、RU→EU、Gas→HU 的来源身份、守恒、堵塞、过载与 exhaust 事务均关闭。
- `t18_converter_acquisition.json` 独立证明 6 个 converter profile 的 recipe/result/operand producer 可达，`unreachable = 0`；profile/block/item/blockstate/block model/item model/en_us/zh_cn/loot/pickaxe tag 双向闭合。
- `t18_load_projection_input.json` / `t18_load_projection.json` 为 delivery T18 zero-workload `PASS`：converter tier/profile/identity 调整及 6 条 vanilla crafting 获取路线不增加 GT row，全部 runtime interval 为 0。
- RecipeMap stable id 32 个、18,875 logical / 16,650 eager / 2,225 lazy、24 个 configured map 的 EMI recipe enumeration 均与 T17 baseline 完全相等，GT publication delta 为 0。
- O-37 由固定 revision 完整 Java tree replay 永久关闭为 `O37_CLOSED_PERMANENT_DESIGN_POLICY`；无 `liquid_medium_oil` / material 9852 direct binding，不改 T9 identity、不新增 runtime registration，独立 publication delta 为 0。

**子判据**

- ✅ **T18a · 蒸汽链**：firebox/boiler/steam engine 可获取、可诊断。
- ✅ **T18b · Fuel/RU/EU**：Fuel Engine 与 Dynamo exact source、identity、守恒和 migration 闭合。
- ✅ **T18c · Gas/HU**：Burning Gas Generator exact source、methane 守恒、exhaust 与 migration 闭合。
- ✅ **T18d · 获取/载荷/O-37**：六机获取与资源双向闭包、T17 零载荷基线、O-37 永久策略和最终 currentness 闭合。

**范围之外**

- 不做核能、聚变、等离子体。
- 不做所有 GT6 generator family。
- 不假 void water / CO₂ / lubricant。
- 不增加 GTM 式矿床递减。

**完成信号**

1. ✅ 四条能源链可从生存原料运行到机器做功。
2. ✅ RU/KU/EU/HU/STEAM 的转换边界不折叠。
3. ✅ O-37 永久关闭为有兼容契约的 `DESIGN_POLICY`，publication delta 为 0。
4. ✅ Fuel Engine 与 Burning Gas Generator 不再处于未选 source family 的模糊状态。

---



## T19 · Cover 与管道获取闭包 ✅

**判据**

> 按 T13 canonical cover kind 选择一批能显著提升自动化表达力的行为，复用 T8 pipe/cover runtime；同时关闭非金属管生存路线 O-27、逐 gauge predicate O-28，并在本轮机器 UI 中处理 O-20。已有 behavior kind 的新实例只改数据，新行为允许小型、登记过的 Java plugin。

**依赖**：T18。

**关闭证据**：`tools/t19_readiness.json` 为 `T19_READY`。47 个 canonical cover 按 4 implemented + 5 selected + 28 deferred + 10 out-of-scope 完整分类，`unclassified = 0`；selected cover 获取 5、非金属管获取 25、`unreachable = 0`。新增 30 条资源只记为 vanilla datapack entries，GT RecipeMap delta 为 0；32 maps、18,875 / 16,650 / 2,225 与 EMI 24 保持 T18 基线。5-tick 调度、32,768 route discovery、256-entry cache、13-byte config payload 与 blocked conservation 均进入 readiness。O-20 / O-27 / O-28 已关闭。

**开工门禁**

1. cover kind 全量分类 `implemented` / `selected` / `deferred_with_reason`，不得用原始 Java 文件数当分母。
2. schema 先区分值与行为：filter、速率、方向、红石参数可数据化；独立 tick、GUI、世界交互属于 behavior plugin。
3. O-27 的五种非金属材料 × 五档真实 GT6 获取路线先定位；找不到时不得发明 source-backed 配方。
4. O-28 的 specification key 与 `PipeCatalog` 共用映射，避免第二套 gauge 名。
5. O-20 的 per-tick progress permille 槽与精确 BE 状态分工先定。

**先算的数字**

- selected cover 按 pure-value / callback / tick / GUI 分级。
- 数据条目、注册对象、网络同步与 tick 成本。
- O-27 的 25 个形态和新增 recipe publication。
- O-28 predicate 影响的现有规则数。

**子判据**

- **T19a · 高价值 Cover 批次**：selected kind 端到端自动化。
- **T19b · 数据 / 行为边界**：同 behavior 新实例只加数据；新 behavior 有受控 plugin。
- **T19c · O-27 / O-28**：获取与 gauge 谓词关闭。
- **T19d · O-20**：短配方进度显示平滑且精确值不回退。

**范围之外**

- 不承诺实现 canonical denominator 的全部 cover。
- 不做 GT6 cover GUI 全套。
- 不扩展 pipe block 形态分母。
- 不把新 behavior 强塞进表达式语言。

**完成信号**

1. ✅ selected cover 批次全部可生存获取、可配置并能端到端搬运。
2. ✅ O-27、O-28、O-20 关闭。
3. ✅ 新增同类 cover 实例只需数据；新增行为的 Java 成本被明确计数而非隐藏。
4. ✅ 第三阶段工业阶梯从资源获取、能源转换、加工档位到自动化形成闭环。

---



## 2. 第三阶段方法论增补



### 2.1 批量导入必须自动检测来源变化被压平

T9 的 129 条 catalog vein 已证明：闭包可以 129 / 129 全绿，同时来源变化被统一模板抹掉。

从第三阶段起，批量 normalizer 比较 source 与 output 的字段基数、分布和声明转换：

- source 字段变化而 output 被未声明转换压成均一值时，生成 `UNIFORM_*` 状态和 open item；
- 该状态在未分类前阻断 fidelity 收尾；
- 不使用固定“80% 同值”代替字段语义；source 本来均一时不误报，合法 many-to-one 转换必须显式声明并可复算。

总体规则见《总体规划》5.7。

### 2.2 每卡新增内容不得扩大本地化与模型债

第三阶段每张卡收尾增加：

- 新增注册对象具有 `en_us`；
- 模型是可接受的生成/继承结果，不新增 placeholder texture；
- `zh_cn` 真实覆盖率不低于本卡开工值；
- 历史 O-15 / O-24 仍单独记账，不因“不倒退”被写成关闭。

第三阶段早期曾以 3,159 / 355 / 2,804 记录 O-15。T19 权威关闭快照已更新为
`en_us` 3,167、真实 `zh_cn` 366、visible debt 2,801，其中材料名 1,774。
历史值不再作为当前 owner 的输入；禁止用英文复制补齐键集。

总体规则见《总体规划》5.8。

### 2.3 “数据化”只适用于值，不抹掉行为边界

- machine tier 只能提供值，behavior 留在 kind；
- cover 同 behavior 的实例只改数据，新行为可用 plugin；
- multiblock structure 零专用 `*Structure` Java，但 controller 事务/UI/交互可用小型 plugin；
- recipe family 只有在 selector 与全量等价证明成立后才能压缩。

---



## 3. 悬案 owner


| 编号       | 内容                                                  | Owner                                                                    |
| -------- | --------------------------------------------------- | ------------------------------------------------------------------------ |
| **O-15** | T19 快照：3,167 `en_us` / 366 真实 `zh_cn` / visible debt 2,801 / 材料名 1,774；不得伪译 | T26 按 v1 玩家可见域关闭 |
| **O-20** | ✅ bounded per-tick progress permille；精确 BE 状态与长工时不回退       | T19 关闭                                                                  |
| **O-26** | extruder sparse relation 压缩                         | ✅ T14 关闭：20 authored ↔ 2,782 logical 全字段等价，Hybrid 557 eager + 2,225 lazy |
| **O-27** | ✅ 25 个非金属管获取；5 source-backed Wood + 20 `DESIGN_POLICY_NON_GT6` | T19 关闭                                                                  |
| **O-28** | ✅ 逐 gauge pipe predicate 共用 `PipeCatalog` specification key | T19 关闭                                                                  |
| **O-29** | GT6 worldgen 几何导入                                   | T20 唯一 owner                                                            |
| **O-33** | GT6 全量 canonical 分母                                 | ✅ T13 关闭：七表、tree/blob manifest、双向集合与 currentness 已进入 CI                  |
| **O-36** | hot ingot 冷却来源                                      | T27 分类后生成的 v1-required fidelity 卡                                                            |
| **O-37** | ✅ Raw Oil identity：固定 revision 无 direct binding；永久 `DESIGN_POLICY`，material 9852 仅 source layer，T9 migration / publication delta 均为 0 | T18 关闭 |
| **O-39** | legacy anvil/crusher RecipeType addon/datapack 迁移窗口 | addon consumer 审计 + 兼容窗口后再评估删除                                           |


新增 T15 残余不另造 O 编号：它们属于 T12 scoped-closure 的明确交接，T15 关闭后更新档案。

---



## 4. 全阶段范围之外


| 内容                                                             | 推迟理由                                        |
| -------------------------------------------------------------- | ------------------------------------------- |
| 全 95 RecipeMap 批量投影                                            | 不属于第四阶段或 v1 契约；T21/T22 只覆盖 ordinary/petroleum 化学全集，不借机实现无关 map |
| GT6 全多方块实现                                                     | T23 只选 2–3 个服务工业主链的代表结构 |
| O-29 worldgen 导入                                               | 已提升为 T20；仍不得与其他 active T 并行 |
| `ITEMGENERATOR.PLANTS / ARMORS / PROJECTILES / LENSES / RAILS` | G10 外围玩法轴，明确 `post_1_0` |
| UUM / fusion / plasma / nuclear                                | 明确 `post_1_0`；需要独立末期物理与冷却体系 |
| GT6U 全量                                                        | 明确 `post_1_0`，不阻断 v1 |
| 动态合并渲染 / 全结构预览 GUI                                             | 表现层，不阻塞工业运行闭包                               |
| GTM 式矿床衰减                                                      | 默认关闭的未来配置策略，不改 GT6 非衰减默认                    |


---



## 5. 第三阶段统一收尾口径

每张卡除《总体规划》第 8 节外，还必须回答：

- [x] 分母来自 T13 artifact，不是 Wiki、类文件数或 CC 当前数量。
- [x] closure / fidelity / load 三轴分别写状态。
- [x] source 可投影全集与独立期望集双向相等，没有用抽样代替。
- [x] source → normalized output 没有未声明的字段压平。
- [x] datapack、publication、server/client reload/index 与内存分别落账。
- [x] 新增注册对象满足 5.8 的本地化 / 模型不倒退门禁。
- [x] 新增 tier 的 publication delta 为 0。
- [x] deferred 项有原因、replacement condition 和 recheck point。
- [x] 完整 verification 通过，`READY` 证据只绑定一次。

---



## 6. T19 之后

第三阶段已经关闭并归档；下面的交接已经由
《[CrucibleCraft-第四阶段总体规划.md](CrucibleCraft-第四阶段总体规划.md)》
接管：

1. T20：O-29 worldgen fidelity；
2. T21：224-material ledger、Mixer 64,245-row 零差异 template replay 与 Beta 必要性分母；input-touch raw row 只作诊断；
3. T22：石油化工 source-row 全量与至少三种工业下游用途；
4. T23：30 kind 先分类，再实现 2–3 个代表多方块；
5. T24–T25：可复现规模证据与仅限实测 Beta blocker 的修复；
6. T26：公开 Beta 门禁；
7. T27：七类 denominator 的 v1.0 portfolio freeze，之后动态生成 T28+。

T20 已关闭，T21 进行中。固定 source replay 把 129 条
catalog 重分为 73 `SOURCE_DERIVED` + 56 显式 `DESIGN_POLICY`，后者不计入
GT6 parity。G10、
nuclear/fusion/plasma 与 GT6U 已明确转为 `post_1_0`。当前 concrete datapack
recipe file 为 3,240；3,230、3,260 和骨架中的 3,270 只保留历史或非权威语义，
不得再作为当前快照。