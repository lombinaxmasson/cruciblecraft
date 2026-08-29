# T39-Repair 详细计划

> 阶段：T39 内部 repair gate，不占用 T40
> 性质：修复配方生产线分母、资源隔离、玩家路径与验证合同
> 状态：✅ 已完成（2026-08-26）；`T39_REPAIR_READY`，`failed_gates=[]`
> owns_families：0
> gap_delta：0
> 前置：T38 READY；T39 原 157/250 签发已撤回为 catalog fixture

## 1. 事故边界

T39 最初把 157 families / 250 relations 同时当作 source catalog、生产集合与 load
fixture。玩家路径复核发现 hosted/state alias、循环 support、旧 T21 闭包和核阶段对象后，
这三个分母不能继续混用。

Repair 固定以下三集合：

1. **catalog**：157 / 250，hash
   `3e125fd647b88b0b93e2840f1cf0681c7196a5e91a8b16a321fa9e0b1b4f2c19`；
   只用于 source replay、codec、router 与 fixture load；
2. **candidate**：动态重算的 22 / 32，hash
   `9aee1c503956a88778e1075e03d8c281d2cea450f12d74b9273b2bb4aefda6e3`；
   只用于诊断，不具有生产权威；
3. **production lock**：人工复核后冻结的 22 / 32；绑定 family/template/stable IDs、
   source revision、support route keys/hash、publication groups 和 phase owner。

任何下游 builder 缺少 lock、hash 不符、把 candidate 当作 signed 或读取 catalog fixture
作为生产证据时必须 fail closed。

## 2. Fuel rod 结论

普通 `material/rod` 只表达材料形态，不表达 active/depleted/product reactor state。
GT6 `gt.multitileentity@9xxx` fuel rods 是带行为和生命周期的 reactor parts，T35 已记录为
`out_of_scope`、`not_registered`，future owner 为 post-1.x nuclear。

已知七个受影响 families：

- `#0169`：`MultiTileEntityReactorRodProduct@9441`；
- `#0176/#0180/#0185/#0188/#0190/#0191`：depleted rods。

它们不得再映射成普通 `/rod` 或 `/ingot`，不得生成生产 recipe 或 support。Repair 只记录
source object、meta、behavior class、canonical family 与 `future_owner=post_1x:nuclear`；
真正的 fuel rod item、反应堆放置/燃烧/耗尽生命周期和核废物链留给核阶段。

## 3. R0–R6

### R0 · 冻结事故与生产锁

- 保留原 157/250 issuance hash 和撤回原因；
- `t39_candidate_selection.json` 由 source/player-path 动态重算；
- `t39_production_lock.json` 已存在时，普通 `--write` 不得覆盖，重签必须显式审批；
- lock schema 验证 counts、selection hash、stable IDs、support routes 与 phase-deferred ledger。

### R1 · Operand fidelity 与阶段归属

- 对 source-derived aliases 逐项分类为 `proven_equivalent`、
  `needs_current_expression`、`phase_deferred` 或 `unsupported`；
- namespace 或可注册 ID 不构成等价证明；
- multi-relation family 任一 relation 不合格，整 family 不得进入生产 lock。

### R2 · 资源隔离

- `src/t39_recipe_generated` 只包含 lock 的 22 compact families；
- 157-family catalog 移到 `src/test/resources/t39_catalog_fixture`；
- locked support 移到 `src/t39_support_generated`；
- 旧 `t39_player_path_recovery` tree 从 main resources 排除，不能进入 main JAR。

### R3 · 分层玩家路径

固定三层：

```text
B0 = T21 without T39 production/support
B1 = B0 + locked support
B2 = B1 + locked T39 production
```

Support 必须 source-backed、按发现顺序从前一层可达、属于 lock 的 family-atomic
反向依赖最小切片。生产 relation 的输入必须在 B1 可达，输出必须注册并进入 B2；target
recipe 不得自证输入。

### R4 · Runtime、测试与 load

- registry-touching 静态常量只能在幂等 Minecraft test bootstrap 后解析；
- production suite 验证 22/32 的 exact IDs、group、matcher 与拒绝语义；
- fixture suite 只验证 157/250 codec/router capacity；
- materialization measurement 只读 production root，group 与 card workload 分开实测；
- fixture 测量不能成为 production winner、census 或 READY 证据。

### R5 · 单一分母

Publication、load、census、topology、readiness 与 GameTest 必须报告同一 production lock：

```text
production families = 22
production relations = 32
singleton = 19 / 19
multi = 3 / 13
support routes = 34
```

Closing census 分列：

```text
closed_by_t39 = 22
reclassified_to_nuclear = 7
remaining_ordinary = 5639 - 22 - 7 = 5610
```

第二个减项是 denominator reclassification，不得写成配方完成量。

### R6 · 文档与恢复

- current workflow 增加 catalog/candidate/production-lock、phase-deferred、fixture 非生产
  证据和 repair withdrawal；
- T39 计划标记原 host-complete issuance 已撤回；
- Repair READY 后 T39 才能恢复 closing；Repair 自身不拥有 family，也不扣 gap。

## 4. 退出门

- `T39_REPAIR_READY`，`failed_gates=[]`；
- production 中 `unknown/unproven_lossy_alias=0`；
- main runtime 精确包含 lock + 34 条 support，fixture 不进入 main JAR；
- B0/B1/B2 无循环自证；
- Java 单测单独与全套顺序都无 registry bootstrap failure；
- publication/load/census/topology/readiness/GameTest 绑定同一 lock hash；
- 两次生成零漂移，T35–T38 历史 artifacts 未改写。

## 5. 非目标

- 不在 Repair 中实现 fuel rod 或核物理；
- 不强行让撤回 catalog 达到 250/250；
- 不用 placeholder、宽泛 support、提高 shard ceiling 或改写旧 READY 制造通过。
