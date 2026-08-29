# T42-Repair 详细计划：snapshot / inventory / classifier fidelity

> 阶段：T42-Repair · T42 内部 fidelity repair gate
> 状态：✅ 已关闭（`T42_REPAIR_READY`，2026-08-28）
> 性质：工程修复；`owns_families=0`、`completion_delta=0`、`publication_delta=0`
> 不占用 T43 编号
> 前置：`tools/t42_readiness.json#status = T42_PARTITION_READY`，`failed_gates=[]`
> 后继：T43 在 `T42_REPAIR_READY` 且 `failed_gates=[]` 前保持未签发；T43 R0 必须从
> **repaired overlay** 重新冻结，而不是 2026-08-28 的 T42 lock
> 本文件只存在于 `card-plans/closed/`；不得放回 `active/`。

## 1. 背景、目标与边界

T42 的 family membership 与 dump dictionaries 正确。Partition 仍在 5,305 / 78,206
打开。下列五个缺陷让 `T42_PARTITION_READY` 建立在偏弱的 inventory 数字、consume-vs-PRESERVE
身份、会丢掉 `gt.metaitem.01` 模具的 dump-empty 过滤，以及把 4,970 个 unique family
当成“全部 MTE”的残余桶上。

本卡按 T40-VR 方式做内部 gate。它不撤销 `T42_PARTITION_READY`，不生成配方、form、
GameTest 或 T43 family ID，不把 dump 里的 `minecraft:` ID 当成 1.21.1 已证明，也不把
armor / tool-head 折进 `prefix_item_to_form`。

## 2. 机器可读 freeze 与 readiness

```text
tools/t42_repair_pre_freeze.json
tools/t42_repair_readiness.json
tools/build_t42_repair_pre_freeze.py
tools/build_t42_repair_readiness.py
```

Freeze 只保存巨型 artifact 的 hashes（snapshot、overlay、lock、inventory、allowlist、
gap），不复制 3M 行 snapshot。Lock 重写需要 `--approve-repair-lock`。普通 `--write`
对 **repaired lock** 仍是 append-only。Topology 在 T42 后插入 `partition_repair`；
`next_issue_id` 仍是 `T43`；host/family 不预分配；`unique_active_content_card` 仍为
null。

## 3. Snapshot

- `operand_action` 对齐 T37 `classify_item_action`：`notConsumed` 或 `count==0` →
  preserve；tool-like `damage>0` → wear。
- `normalize_action` / `logical_relation_identity` 发出 `PRESERVE` / `WEAR` /
  `CONSUME`，与 Java `ItemInputAction` 一致，不把 wear/preserve 折进 consume。
- Empty-slot markers 改为 `empty_slot` / `gt.meta.empty` /
  `gregtech:gt.meta.empty`。`gt.metaitem.01` 模具行必须保留。
- Compact operand intern `material` / `form` / `alias`。`--write --full-replay`
  仍需要 dump；`--check` 仍 dump-free。

## 4. Inventory

拆分 molten：

- `generation_tag_molten_material_count` ← `ITEMGENERATOR.MOLTEN`（184）
- `top_level_molten_flag_count`（0，按此标记）
- `remaining_molten_fluids_in_mapping`（`t22_5_fluid_mapping` 中的 `molten.*`，203）

Allowlist 来源 `t37_t41_aliases_plus_explicit`，`not_a_121_registry_scrape=true`。
`item_ids` 是 T37/T41 alias 的 1.21.1 live ID **加上显式** `minecraft:oak_planks`，
不是「只有 alias + 水/熔岩」。不把 remaining dump 的 143 个 `minecraft:*` 刮进
allowlist。anvil / iron_ingot / bucket 必须保持 `minecraft_prefix_not_proof`。Java 用
live `BuiltInRegistries.ITEM` 证明 allowlist 子集，并证明 anvil/ingot/bucket 存在且
不在 allowlist 上。dump 的 `noteblock` 走 vanilla_alias，不靠 allowlist。

`gregapi:gt.integrated_circuit` → `cruciblecraft:programmed_circuit` +
`circuit_config` 是映射，不是证明：`tools/t35_runtime_registry.json` 没有该 item
（`ModItems` 有注册），B0 也没有。Overlay 标 `runtime_id_not_registered` /
`needs_current_expression`。T43 若碰到 autoclave/mixer 的 circuit 槽，不能当成已经可关。

## 5. Overlay classifier

映射顺序：empty / vanilla alias / `minecraft:` → `prefix_item_to_form` →
`prefix_item_to_gt_prefix`（`armor` / `tool_head` / `stone` / `circuit` /
`gt_prefix:<name>`，不是假 CC form）→ `gregapi:gt.integrated_circuit` 作为
programmed-circuit component + preserve → 真 unique object。残余 else 不得无 kind
地变成 unique；发布 `unique_kind_counts`，使 MTE count ≠ unique-bucket family
count。仍然不因 kind tag 自动 `later:object_expression`。

Repaired overlay：unique-bucket 4,980 仍是残差桶；约 3,766 族有 `unique_kinds`，约
1,214 族为空（unmapped / unproven vanilla / B0 / residual_not_ready）。
`unique_kind_counts.mte` 2,646（不是 catch-all，也不是「全部独立物体」）。
`gt.stone.andesite` 与 `gt.armor.hazmat.*` 仍 unmapped。Lock 仍为 already_expressed 1
+ combinatorial 4；closing gap 5,300；`completion_delta=0`。
`t42_gap_partition.json` 的 `partial_family_count: 0` 表示未从 gap 扣除 partial；
overlay 的 `partial_family_count: 1` 仍留在 5,300 里。

## 6. 验证

```powershell
python -m unittest discover -s tools/tests -p "test_build_t42*.py"
python tools/build_t42_family_operand_snapshot.py --write --full-replay
python tools/build_t42_runtime_expression_inventory.py --write
python tools/build_t42_blocker_overlay.py --write
python tools/build_t42_disposition_lock.py --write --approve-repair-lock
.\gradlew.bat test --tests com.masson.cruciblecraft.recipe.gt.T42LogicalRelationIdentityTest --tests com.masson.cruciblecraft.census.T42RuntimeInventoryClassifierTest --no-daemon
python tools/verify.py integration --profile recipe-partition
```

PowerShell 构建命令不得使用 `&&`。`T42_REPAIR_READY` 当且仅当 `failed_gates=[]`。
