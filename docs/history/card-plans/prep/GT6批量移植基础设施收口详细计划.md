# GT6 批量移植基础设施收口

> 计划 slug：`portfolio/gt6-bulk-port-verification`
> 状态：prep
> 性质：批量移植前的验证基础设施收口；不占 unique-active，不实现新 GT6
> 机器、配方或材料公共前缀。

```text
lane                         = prep
capability_slug              = portfolio/gt6-bulk-port-verification
unique_active_wave           = null
landing_owned_paths          = tools/recipe_bulk/source_import.py；
                               tools/material_form_authority.py；
                               tools/python_test_policy.json；
                               tools/verification_profiles.json；
                               tools/tests/**；
                               各 historical receipt 的 currentness 元数据
landing_depends_on           = GT6 长尾形态开门卡完成
```

## 1. 问题边界

当前漂移不是单一哈希算法错误，而是三类状态混在一个 profile：

1. 真实回归：生成物缺失、错误 registry path、源精确 recipe 不匹配；
2. 当前依赖漂移：共享 `material_form_authority.json` 改动导致大量 receipt 失效；
3. 历史快照漂移：已关闭卡仍按当前工作树的数量、路径或全局 authority hash 检查。

本卡必须先把三类状态分开，禁止用全量 `--write` 把历史失败直接抹平。

## 2. 实施

### 2.1 真实回归优先

- 从 `capability-runtime` 的失败清单建立机器可读 disposition；
- 缺失 recipe / identity / generated art 逐项回到其 owner builder 修复；
- 每个修复保留 GT6 source evidence；不使用 stand-in；
- 生成后跑对应 card test，不把失败降级为 historical。

### 2.2 Authority section hash

`material_form_authority.json` 保留整体文件 hash，同时新增稳定的 section slice：

- `gate_section`；
- `required_forms` 文件 hash；
- 该 section 的 `extra_factual_forms` 与 `required_factual_prereqs`；
- source receipt 只记录实际消费 section 的 slice hash。

旧 receipt 不自动重写；生成器以 `authority_scope` 区分 legacy whole-file receipt 与新的 section receipt。

### 2.3 Historical currentness

- `active` / `runtime_ready` 当前卡：依赖 slice 必须 current；
- `closed` 卡：默认验证冻结 source snapshot 与 receipt 内的 `source_revision`，不要求匹配未来 authority；
- historical 测试保留并可手动 replay，但不阻塞当前 capability profile；
- 冻结分母与当前投影分开，禁止用当前计数覆盖历史事实。

### 2.4 Profile 选择

按 changed-path 依赖选择测试：

- shared authority section 变更 → 只选择消费该 section 的 active/current cards；
- registry/runtime source 变更 → 选择对应 registry/runtime tests；
- docs 或历史 receipt 变更 → 不启动全量 capability suite；
- release probe 单独运行，不作为普通卡闭卡门。

## 3. 验证

```powershell
python tools/registry_identity.py --check
python tools/build_gt6_material_form_gate.py --check
python tools/material_form_authority.py --check
python tools/verify.py integration --profile capability-runtime
python -m unittest discover -s tools/tests -p "test_*verification*.py"
```

验收条件：

- 真实回归为零；
- active/current receipt 无未解释漂移；
- historical 漂移有明确 `historical` / `manual_replay` disposition；
- `capability-runtime` 不再因为无关 closed card 的 whole-file hash 变化而失败；
- 不改变公共 16 前缀名单，不引入配方 stand-in，不删除历史测试。

## 4. 明确不接管

- 新 GT6 机器、配方、流体或材料形态；
- `boxinator` / `unboxinator`、`crate.*`、`bulletGt*`；
- 管、缆或 crops addon 的后续功能；
- 通过删除测试、放宽 blocker 或批量覆盖 receipt 来“修绿”。
