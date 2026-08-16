# CrucibleCraft 阶段档案 · T21

> **工作副本。** 权威关闭档案为仓库根目录
> 《[../CrucibleCraft-阶段档案-T21.md](../CrucibleCraft-阶段档案-T21.md)》（`T21_READY`）。

> 阶段：T21 · Mixer 模板分母与普通化学规则轴  
> 状态：🔶 进行中（草稿档案；`T21_READY` 未绑定完整验证，不作关闭证据）  
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`  
> 权威产物：`tools/t21_readiness.json`

## 1. 开工纠偏

第四阶段规划中的“234-material”是 stale prose。权威
`tools/t5_chemical_readiness.json` 明确记录：

```text
145 terminal-dust + 110 crusher - 31 overlap = 224 unique candidates
```

另有 110 条 byproduct-only debt，其中 60 条与 224 candidates 重叠、50 条在
其外；这些行单独引用，不相加到候选分母。T21 因此以 224 为唯一材料候选分母，
而不是为了匹配旧文案复制或补造 10 条 identity。

## 2. 材料候选校准（不是 source-row 完成分母）

224 条材料候选已完成 shape 校准：

| 分类 | 数量 | 判据 |
|---|---:|---|
| `composition_generated` | 84 | 固定 Loader conjunction 具备 components、DECOMPOSABLE、divider ≤ 64 与 centrifuge/electrolyzer route tag |
| `prefix_matrix` | 0 | T5 chemical ledger 没有 source-backed prefix eligibility matrix，显式记零 |
| `named_reaction` | 70 | 当前有 pinned T5 concrete source row，但 Loader composition conjunction 不定义该行为 |
| `blocked_by_new_subsystem` | 70 | 既无 Loader composition route，也无当前 pinned concrete runtime row |

9 个当前被引用 RecipeMap 全部分类，material-candidate `unclassified = 0`。70 条 blocked row 各自记录
reason、dependency、owner、replacement condition 与 recheck point；不存在用
相邻材料猜反应或把未知行为塞进 `named_reaction` 的行。

`no_decompose` 仍是 CrucibleCraft quarantine gate，不是 source route selector。
因此 84 条 composition-shaped candidate 中，已发布、quarantined 与尚未发布状态
分别保留，shape 分类不伪装成全部实现。

## 3. Mixer 模板重基线

45,353 / 45,044 行账保留为 input-touch mapping/rejection 诊断，不再作为关闭
分母。schema-first 分析修正了 GT meta 材料轴识别和 leave-one-out 重复消费后，
Mixer 固定 source 得到：

- logical source rows：64,245；
- authored units：3,414；
- material-matrix：2,628 templates / 58,009 rows；
- enumerated：103 templates / 4,858 rows；
- opaque：683 units / 1,378 rows；
- membership unassigned / duplicate：0 / 0；
- canonical multiset missing / extra：0 / 0。

旧 451 个探索模板只保留为分组提示；两个 27,984-row construction-foam
探索 family 被完整 replay 后拆成显式 material/enumerated support，不再用
Cartesian 猜测扩展。

Mixer template 与其余九张 map 的 exact singleton/family unit 合并后共有
93,133 denominator units，全部唯一分类。Beta seed 反向闭包只选出一个
v1-required unit；T5 partial coverage 均先拆分，`unclassified = 0`。

## 4. 首个 Carbon electrolysis 校准纵切

首个 selected family 为 `carbon_electrolysis`：

- authored rule：1；
- source facts：2；
- members：`charcoal / coal`；
- 输入：1 dust；
- 输出：1 carbon dust；
- map / machine / energy：Electrolyzer / electrolyzer / EU；
- duration / EU/t：292 / 16；
- pinned source rows：electrolyzer recipes 72 / 275。

生产 expansion 从两个材料的 composition、normalized component、divider 与
family data 推导；独立 expected 从 pinned source rows 与 T5 source-I/O manifest
推导。两路不共享 composition selector，且：

```text
independent expected == composition expansion == current runtime recipe
```

现有 runtime id 与文件字节保持不变，因此没有 duplicate signature 或 publication
增量。新增 GameTest 在两个真实 Electrolyzer 上分别执行 coal / charcoal，
验证 live matcher、EU 消耗、输入提交与 carbon dust 输出。换两个 family member
只消费数据，不增加材料专用 Java。

输入经 T20 worldgen 与现有 ore chain 可取得；carbon dust 当前被 5 条 carbon
fluid-pipe acquisition recipe 真实消费，纵切不是无用途陈列。

## 5. Template 发布与三轴账

唯一 v1-required unit 为 4-row `mixer/gunpowder` material-matrix family：

- members：`carbon / charcoal / coal / coal_coke`；
- shared inputs：niter dust + blaze powder；
- output：4 gunpowder；
- independent expected == template projection == runtime；
- GameTest 在四台真实 Mixer 上执行全部 members；
- consumer：vanilla TNT / gunpowder 路线；
- v1-required published / remaining：1 / 0。

**Closure**

- material candidates：224；
- Mixer source/template：64,245 / 3,414；
- template denominator units：93,133；
- v1-required units：1，remaining 0；
- unclassified / in-scope runtime blocker：0 / 0；
- input-touch row diagnostic 不参与 closure numerator。

**Fidelity**

- Carbon source rows 72 / 275 与 gunpowder source rows
  5,319 / 5,788 / 31,445 / 49,274 均有独立 SHA-256；
- Mixer 64,245 expected/replay canonical multiset 全等；
- expected / template expansion / runtime 全字段相等；
- amount、chance、duration、EU/t、specialValue、buffering 与 source provenance
  全部保留；
- support、membership 或 expected 变异会使门禁变红。

**Load**

| 计数 | 值 |
|---|---:|
| source facts | 4 |
| authored rules | 1 |
| datapack files | 4 |
| logical rows | 4 |
| eager rows | 4 |
| lazy rows | 0 |

- family load projection：PASS；
- logical / eager / lazy delta = 4 / 4 / 0；
- 全局转为 32 RecipeMap、18,879 logical、16,654 eager、2,225 lazy；
- construction-foam 为非 v1，不进入 eager publication。

## 6. 验证与交接

关闭绑定：

```text
python tools/build_t21_chemical_axis.py --check --full-replay
python tools/gt6_mixer_templates.py --check --full-replay
python tools/build_t21_template_denominator.py --check --full-replay
python tools/build_t21_mixer_gunpowder.py --check --full-replay
python tools/run_python_tests.py --suite closure
.\gradlew.bat test
.\gradlew.bat runGameTestServer
python tools/run_full_verification.py --record --new-session
```

T21 关闭的是 template necessity 与 v1 publication，不宣称 64,245 Mixer rows
全部进入 runtime；`ordinary_optional` 保持可追踪但不阻断 Beta。当前无 active T，
下一张卡为 T22 石油化工全量与纵深。
