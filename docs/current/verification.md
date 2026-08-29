# 开发与验证指南

日常入口是 `tools/verify.py`。不要把历史 `full_verification_report.json` 或
Markdown 叙述当作当前代码必须匹配的证明。

T40 闭卡期间门闸 overlay、跨卡哈希链、Gradle UP-TO-DATE 假绿灯与超大 JSON
写失败的现场记录见 [T40 闭卡拖延回顾](../history/work-logs/t40-closeout-delay-review.md)
与 [current 入口](t40-closeout-delay-review.md)。T40-VR 已把这些缺口收成
`material_form_authority`、atomic writer、versioned semantic projection、
currentness sidecar、verification DAG 与 `--report-all`/`--json` runner。
compact `--check` 绑 semantic root，不因 envelope/builder 整文件 SHA 要求重写巨型正文。

T39 起的 bounded recipe waves 还必须遵守
[ordinary recipe wave 流程与规范](recipe-wave-workflow.md)：每波先区分 catalog、
动态 candidate 与不可自动漂移的 production lock，并完成 operand disposition / phase-owner
审计；随后生成 publication-group / query-addressable-shard manifests，提交规范化 GameTest
日志与 aggregate-root/dependency-bound receipt，同时测 per-shard、card-only / integrated
load，并在 clean checkout 复核 closing artifacts。Test-only catalog fixture 不能成为
production winner、player-path、census 或 READY 证据。

## 改了什么，跑什么

| 改动 | 命令 | 何时升级 |
| --- | --- | --- |
| Markdown / `docs/` / 历史档案 | `python tools/verify.py dev` | 不升级 |
| `tools/` 验证入口、profile、policy | `python tools/verify.py dev` | 关闭验证卡时再跑 integration `--profile verification` |
| 材料 / 前缀 / oredict | `python tools/verify.py integration --profile materials` | 内容卡闭合 |
| 配方 / 化学 / 石油 | `python tools/verify.py integration --profile recipes` | 内容卡闭合 |
| 世界生成 / 矿脉 / 石子 | `python tools/verify.py integration --profile worldgen` | 内容卡闭合 |
| 机器 / 能量 / 容器 | `python tools/verify.py integration --profile machines` | 内容卡闭合 |
| 管道 / 覆盖板 / hopper | `python tools/verify.py integration --profile logistics` | 内容卡闭合 |
| 贴图 / 语言 / 客户端外观 | `python tools/verify.py integration --profile presentation` | 内容卡闭合 |
| T35 census currentness / topology | `python tools/verify.py integration --profile census` | T35 范围内闭卡；历史分母只读 |
| T36 census overlay / T37–T44 overlay | `python tools/verify.py integration --profile census-replay` | T36 机器闭合后、T37 校准闭合后、T38 Roaster / T39 Centrifuge / T40 Electrolyzer / T41 Assembler / T42 partition / T42-Repair / T42-Owner / T43 Smelter stone 闭合后、T36-Repair 扩展闭环后、T44 Storage bundle 闭合后；delta/readiness/load decision / `t42_repair_readiness` / `t42_owner_readiness` / `t43_readiness` / `t36_repair_readiness` / `t44_readiness`，不重跑 GT6 inventory |
| T42 remaining-family partition / internal repairs | `python tools/verify.py integration --profile recipe-partition` | T42 freeze / snapshot / inventory / B0 / overlay / lock / gap / T42-Repair pre-freeze，以及 T42-Owner pre-freeze / owner tracks / recovery evidence / owner lock / effective gap / readiness；不跑 datagen、GameTest 或 T14 load benchmark |
| T40-VR 验证基础设施 | `python tools/verify.py integration --profile recipes --report-all --json build/verification/t40-vr-recipes.json` | `T40_VR_READY`；不占用 T41。hash-only 用 `python tools/verify.py currentness --mode plan --scope card-closeout` |
| T40-VR 卡级诊断 | `python tools/verify.py integration --profile card-diagnostic-T40 --json build/verification/t40-vr-diagnostic.json` | 报告上游 stale/debt 与 sidecar hash-only；状态 `DIAGNOSTIC`，不是 closeout PASS |
| T40-VR 闭卡 | `python tools/verify.py integration --profile card-closeout --report-all --json build/verification/t40-vr-closeout.json` | recipes + census + census-replay；顶层 JSON 保留 recipes 的 Gradle XML 与 GameTest receipt，不被最后一个 profile 覆盖 |
| T44 Storage runtime 隔离 | `.\gradlew.bat runGameTestServer -Pt44Storage --no-daemon` | T44 storage suite 验证 624+1 catalog、bookshelf/crate/locker/charging/mass/inserter/logistics 与 T36 85 catalog 行未扩张。Receipt 使用 committed UTF-8 log，并绑定 production lock 与 bundled catalog；被忽略的 `run-t44-storage/` 不是证据 |
| T43 Smelter stone runtime 隔离 | `.\gradlew.bat runGameTestServer -Pt43Recipes --no-daemon` | T43 production suite 验证 407 authored Smelter stone families。Catalog fixture 只进 JUnit。Receipt 使用 committed UTF-8 log，并绑定 production lock、worldgen support tree、publication-group/shard aggregate root 与 runtime dependency manifest；被忽略的 `run-t43-recipes/` 不是证据 |
| T39 Centrifuge runtime 隔离 | `.\gradlew.bat runGameTestServer -Pt39Recipes --no-daemon` | T39 production suite 精确验证 production lock 22/32 与 34 locked support；157/250 catalog 只进 JUnit test fixture。Receipt 使用 committed UTF-8 log，并绑定 production lock、minimal support、publication-group/shard aggregate root 与 runtime dependency manifest；被忽略的 `run-t39-recipes/` 不是证据 |
| T38 配方 runtime 隔离 | `.\gradlew.bat runGameTestServer -Pt38Recipes --no-daemon` | R3 起；namespace `cruciblecraft_t38`，不进日常网格。`--write --from-log` 把运行日志规范成 UTF-8 证据 `tools/t38_gametest.log`；收据指向该文件并绑定 `MaterialRegistrationGate` 与 T38 资源树。`--check` 不得依赖被忽略的 `run-t38-recipes/`，也不得只对照 receipt 自保存指纹 |
| T37 配方 runtime 隔离 | `.\gradlew.bat runGameTestServer -Pt37Recipes --no-daemon` | R5 起；namespace `cruciblecraft_t37`，不进日常网格 |
| T36 机器 runtime 隔离 | `.\gradlew.bat runGameTestServer -Pt36Machines --no-daemon` | R7 起；namespace `cruciblecraft_t36`，不进日常 137 网格 |
| 历史 READY 收据 | `python tools/verify.py archive-inspect` | 不改写、不按未来全局 recipe tree 重算；核心 runtime ABI 变化由 compatibility/migration profile 统一回归 |
| 玩家发行 | `python tools/verify.py release` | 仅未来发行卡 |

Windows 示例：

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile worldgen
python tools/verify.py integration --profile census
python tools/verify.py integration --profile census-replay
python tools/verify.py integration --profile recipe-partition
python tools/verify.py archive-inspect
```

`dev` 根据 `git diff` 或 `--path` 选择 profile。无法映射的代码路径会列出需要声明的
scope，既不静默通过，也不升级到 110 个 builder。纯文档改动只跑 Markdown 链接和
profile 静态检查。

`census` 使用 compact/reference currentness；不会在 census、topology 与 readiness 层
重复读取 recipe source dump。`census-replay` 在 T36 之后校验 `t36_census_delta` / `t36_readiness`，在 T37 之后校验
`t37_census_delta` / `t37_readiness` overlay，在 T38 之后校验 `t38_recipe_load_benchmark` 与 T38 load
decision，在 T41 之后校验 `t41_recipe_load_benchmark` 与 T41 GameTest receipt，在 T42 之后校验
`t42_census_delta` / `t42_card_topology` / `t42_readiness`，在 T42-Repair 之后校验
`t42_repair_readiness`，在 T42-Owner 之后校验 owner gap / `t42_owner_readiness`，在 T43 之后校验
`t43_census_delta` / `t43_card_topology` / `t43_readiness` 与 T43 GameTest receipt / load
decision，在 T44 之后校验 `t44_storage_census_delta` / `t44_card_topology` /
`t44_readiness` 与 T44 GameTest receipt / load projection，不再重跑 T35 machine/recipe full inventory。`recipe-partition` 校验 T42 freeze 到 gap 的诊断链与
T42-Repair pre-freeze 与 T42-Owner owner-track / evidence / lock 链，不拥有原 T42
census/readiness builders（含 repair-readiness），也不跑 GameTest。
T36 机器 runtime 用 `-Pt36Machines`，T37 配方 runtime 用 `-Pt37Recipes`，T38 配方 runtime 用
`-Pt38Recipes`，T41 配方 runtime 用 `-Pt41Recipes`，T43 配方 runtime 用 `-Pt43Recipes`，T44 仓储 runtime 用 `-Pt44Storage`，都不要混进日常 `cruciblecraft` 网格或
`-Pt35Census`。

## 硬门与非门

硬门：GT6 compact evidence、JSON schema、生成树一致性、内容卡 load budget。

非门：README、总体规划、阶段档案、工作日志、历史 READY 快照的 live currentness。
T35 census 只把 `docs/current/roadmap.md` 记为 narrative 引用（文件必须存在），
不把它的内容 hash 纳入 `census` currentness 或 stale 门禁。

已知未修债务见 [`tools/known_issues/verification-debt.json`](../../tools/known_issues/verification-debt.json)。

## Profile 边界与验证债务

内容卡只把其声明且实际改动的 integration profile 作为闭卡门：worldgen 卡运行
`--profile worldgen`，配方卡运行 `--profile recipes`。通过一个 profile 不得推导其他
profile 已通过。

已登记债务导致的 profile 失败不能被忽略、标为 passing，或随不相关内容卡继承为 READY。
如果该失败 profile 不属于当前卡的范围，它不阻断当前卡的**范围内**闭包，但必须同时满足：

1. 当前卡没有改动该 profile 的 owned paths，且其自身 required profile 全部通过；
2. 债务条目保持 `open`，闭卡记录写明失败 profile、债务 id 与其 scope-external 身份；
3. 不增加 bypass / allow-failure 开关，也不把“债务存在”改写成“验证通过”。

`T32-VD-001` 是上述规则的当前实例：T33 只以 worldgen profile 闭卡；严格 recipes
integration 仍是红灯，必须在 T35 的 recipe-debt preflight 修复后重跑并关闭该债务。

## 历史全量入口

`python tools/run_full_verification.py --check` 仍可手工运行，但它校验的是历史
单一 READY 证明，不是 T32 之后的日常开发门。CI 的 PR 路径使用 `verify.py dev`。
