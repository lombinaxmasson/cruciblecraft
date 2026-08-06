# CrucibleCraft 阶段档案 · T13–T16

> 状态：历史档案；T13–T16 均已关闭。
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 归档基线：2026-08-06；最终状态 `T16_READY`
> 当前执行入口：《[CrucibleCraft-第三阶段总体规划.md](CrucibleCraft-第三阶段总体规划.md)》T18

本文保存第三阶段前四张已关闭判据卡的边界与关闭快照。这里的分母是分类范围，不是“全部实现”声明；当前导航仍以总体规划和第三阶段总体规划为准。

---

## T13 · GT6 可比较分母清点 ✅

- 七类 canonical denominator 全部就绪：95 RecipeMap / 720,841 rows、452 prefix、25 ITEMGENERATOR domain、96 machine kind、20 energy identity、47 cover kind、30 multiblock kind。
- 七表 `unclassified = 0`，规范化 blocker = 0；T13 本身 datapack / publication delta 均为 0。
- `T13_READY` 只证明分母、分类和 provenance 可复算，不表示所有 source row 或 canonical kind 已实现。

## T14 · 载荷架构与 Extruder 压缩 ✅

- Extruder 由 2,782 条 flattened authored row 收敛为 20 条 compact rule，完整 logical stable-id 集合与全字段双向等价。
- 20 ↔ 2,782 是 built-in 完整基线，不是 datapack 子集的装载下限；运行时允许 0–20 个 compact source、0–2,782 条 relation，保留原 `shadow_order`，按排序后严格递增校验并允许 gap。
- server 与 dedicated client 对实际装载子集分别枚举并计算诊断 fingerprint；fingerprint 不再要求子集匹配 built-in baseline。
- Hybrid 为 557 eager + 2,225 lazy，per-epoch cache ceiling 512；全局账目为 3,223 authored、18,875 logical、16,650 eager、2,225 lazy。
- server/client reload/index、retained、allocation、lookup 与 sync 预算完成实测并进入 hard-fail evaluator，状态 `T14_READY`。

## T15 · T12 残余与现有档位获取 ✅

- Large Centrifuge 独立 profile、6 casing + 9 machine recipe、四类 identity quarantine 与精确 legacy migration 均已关闭。
- live 结构为 15 item/fluid port、2 energy port、1 controller；物理端口共享一个 host，item/fluid matcher 各只有 1 份 input supply。
- T15 新增 GT 配方与 publication 均为 0，状态 `T15_READY`。

## T16 · RU / KU 机器档位批次 ✅

### 关闭边界

- T13 owner 分母为 20 kind，`unclassified = 0`。
- 本批 selected 5：Lathe、Rolling Mill、Wiremill、Shredder、Press；每种只实现 bronze / steel / titanium 三档，共 15 variants。
- preimplemented 2：Centrifuge、Sifter。
- deferred 13；另有全部 20 kind 的 tier 4 deferred 记录，每项都有 reason、replacement condition 与 recheck point。
- 这不是“20 kind 全实现”：只有 selected 5 × 3 与既有 preimplemented 2 进入当前 runtime，其余边界保持显式 deferred。

### 运行时与获取

- RU 使用 `KINETIC_ROTATION` axle / gearbox；KU 使用 `KINETIC_PUSH` adjacent push，没有退回 deprecated `KINETIC`。
- 15 个 selected variant 都有模型、en_us / zh_cn、loot、tag 和 survival crafting；获取闭包 `unreachable = 0`。
- 五个 tier-1 legacy identity 精确迁移；near miss 继续 quarantine，库存、流体、进度与原始坏 identity 可恢复。

### T16d publication / load

- 正式 `t16_load_projection_input.json` / `t16_load_projection.json` 为 delivery T16、`PASS`；authored / logical / eager / lazy / cache / sync 与全部 runtime interval 增量均为 0。
- pre-T16 RecipeMap stable-id 集合保持 32 个；logical / eager / lazy 保持 18,875 / 16,650 / 2,225。
- EMI 的 24 个 configured RecipeMap 与完整 recipe enumeration 保持双向相等。
- 只新增 15 条 vanilla shaped crafting machine-acquisition recipe；GT recipe row 新增 0，`publication delta = 0`。
- 历史关闭验证规模：475 Java tests、392 Python tests、62 GameTests；最终均由统一 verifier 记录。P0 currentness 修订新增的定向测试不回写这组历史 full-closure 计数。
- 最终 readiness 为 `T16_READY`，T16a–d complete、pending 为空。

---

## 2026-08-06 · P0 currentness 修订

- `CeramicMoldBlock` 的模具与内容物掉落改由真实 block replacement 的 `onRemove` 统一负责；非玩家移除、玩家生存/创造路径一致，同 block state 更新与 block entity 移除不重复掉落。
- T14 built-in 20 ↔ 2,782 全字段双向等价仍由既有 Java/Python gate 保持；新增 19-source、relation gap、全禁用、越界、重复/负值/非法 relation 的定向覆盖。
- `tools/t14_readiness.json`、`tools/t15_readiness.json`、`tools/t16_readiness.json` 已沿 currentness 依赖链刷新；定向 JUnit/Python 与生产 GameTest 63/63 通过。本次未重跑 T14 benchmark，也未重跑 full closure，留待 P1/最终阶段。

## 2026-08-06 · P3 最终收尾

- Java 换行契约固定为 `*.java text eol=lf`；只归一化已确认的四个 CRLF 源文件，并由 Python closure 扫描 `src/**/*.java` 阻止回归。
- `GTRecipeMapLoader` 以显式 `RecipeMap → Prepared` 绑定做 owner / unindexed 校验与日志，不再依赖 `ModRecipeMaps.ALL` 和 prepared list 的隐式同序；publication metrics 在 publish 前构造，只在 maps/material 成功发布后、epoch 提交前替换。
- legacy anvil/crusher RecipeType 保留为 `@Deprecated(forRemoval=false)` addon/datapack 兼容 API；新数据迁移到 `material_rule`，O-39 固定未来删除的审计与兼容窗口条件。
- O-15 由当前生成资源重算为 en_us 3,153、真实 zh_cn 344、缺失 2,809，其中材料名 1,774；不做英文复制式伪翻译。
- T14/T15/T16 readiness 沿 currentness 链刷新；最终 builder、双 runData、全 JUnit、全 GameTest、Python closure 与逐 step wall time 由 `tools/full_verification_report.json` 的单次可恢复 session 统一记录。

## 2026-08-06 · Full Verification 分层修订

- `tools/verification_builder_policy.json` 当时成为 38 个 builder 的唯一有序入口；T17d 接入后当前为 41 个。每项显式声明 ordinary argv、`compact` / `rederived` proof tier 与可选 full-replay 命令。
- T13 RecipeMap 证明拆为 compact、hash-fast、full-replay：ordinary CI 只验证 95 行 canonical artifact、完整输入 closure 与外部 receipt；hash-fast 逐文件 SHA-256；只有 source-replay 解析 720,841 条 recipe row。
- material gate、ore chain、T5 chemical/distillery 与 T11 preflight 的 ordinary check 不再读取 raw dump；删除本地 cache 不降低 PR CI 的 datagen、Java、GameTest、Python closure 强度。
- T5 chemical recipe tree、T11 hydrocarbon fixed rows、历史 T12a 与 T13 OP/ITEMGENERATOR 也改用带 builder/input/output hash 的 compact receipt；全量复算统一进入 source-replay。
- 迁移后的 warm builder 阶段约 23 秒；此前 committed baseline 为 161 秒。性能差异只生成 `WARN`，任何 hash、集合、语义或 currentness 漂移仍 hard-fail。
- T16 分层修订时的 currentness 规模为 495 Java tests、417 Python tests、63 GameTests；T17 最终规模 508 / 441 / 69 记录在 T17 档案。T16 初次关闭时的 475 / 392 / 62 仍仅作为历史快照保留。

T17 HU / EU 机器档位批次的关闭快照已迁入《[CrucibleCraft-阶段档案-T17.md](CrucibleCraft-阶段档案-T17.md)》；当前入口为 T18。
