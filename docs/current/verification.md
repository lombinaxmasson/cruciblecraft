# 开发与验证指南

日常入口是 `tools/verify.py`。不要把历史 `full_verification_report.json` 或
Markdown 叙述当作当前代码必须匹配的证明。

早期 Electrolyzer 闭卡期间的门闸 overlay、跨卡哈希链、Gradle UP-TO-DATE 假绿灯与
超大 JSON 写失败记录在 `docs/history/work-logs/`。后续验证修复把这些缺口收成
`material_form_authority`、atomic writer、versioned semantic projection、
currentness sidecar、verification DAG 与 `--report-all`/`--json` runner。
compact `--check` 绑 semantic root，不因 envelope/builder 整文件 SHA 要求重写巨型正文。

现行 bounded recipe waves 还必须遵守
[ordinary recipe wave 流程与规范](recipe-wave-workflow.md)：每波先区分 catalog、
动态 candidate 与不可自动漂移的 production lock，并完成 operand disposition / phase-owner
审计；随后生成 publication-group / query-addressable-shard manifests，提交规范化 GameTest
日志与 aggregate-root/dependency-bound receipt，同时测 per-shard、card-only / integrated
load，并在 clean checkout 复核 closing artifacts。Test-only catalog fixture 不能成为
production winner、player-path、census 或 READY 证据。

`recycling/deferred-ordinary-runtime` 已 `DEFERRED_ORDINARY_RUNTIME_READY`。
`portfolio/one-x-joint-exit` 已 `ONE_X_JOINT_EXIT_READY`。
`portfolio/source-capability-map` 已 `SOURCE_CAPABILITY_MAP_READY`。
growth-order 建议的 `next_major = portfolio/generic-recipe-generator` 已按
[通用 Source Pack 导入器详细计划](../history/card-plans/active/通用Source-Pack导入器详细计划.md)
签发为零 family 机制 program，尚未实现，没有 production lock 或 nuclear census。
R0 artifact 生成前，上一 closing 的 `unique_active_wave = null` /
`next_unassigned = true` 仍是机器可读事实。pending / missing-event /
zero-filled load 仍不得 `WAVE_READY`。count telemetry 超旧参考只报
`UNVERIFIED_SCALE` / `REPORT_ONLY`，不是 1.x exit 硬顶。

## 改了什么，跑什么

| 改动 | 命令 | 何时升级 |
| --- | --- | --- |
| Markdown / `docs/` / 历史档案 | `python tools/verify.py dev` | 不升级 |
| `tools/` 验证入口、profile、policy | `python tools/verify.py dev` | 关闭验证工作时再跑 integration `--profile verification` |
| 材料 / 前缀 / oredict | `python tools/verify.py integration --profile materials` | 内容卡闭合 |
| 配方 / 化学 / 石油 | `python tools/verify.py integration --profile recipes` | 内容卡闭合 |
| 世界生成 / 矿脉 / 石子 | `python tools/verify.py integration --profile worldgen` | 内容卡闭合 |
| 机器 / 能量 / 容器 | `python tools/verify.py integration --profile machines` | 内容卡闭合 |
| 管道 / 覆盖板 / hopper | `python tools/verify.py integration --profile logistics` | 内容卡闭合 |
| 贴图 / 语言 / 客户端外观 | `python tools/verify.py integration --profile presentation` | 内容卡闭合 |
| census currentness / topology | `python tools/verify.py integration --profile census` | 历史分母只读 |
| census overlay / 已关闭波 overlay | `python tools/verify.py integration --profile census-replay` | hash/seal `--check` 与当前波 load decision；历史 load 重跑用 `--full-replay` 或 `release`。不重跑 GT6 inventory，也不付 Gradle |
| remaining-family partition / internal repairs | `python tools/verify.py integration --profile recipe-partition` | freeze / snapshot / inventory / B0 / overlay / lock / gap / owner tracks；不跑 datagen、GameTest 或 compact-load benchmark |
| 现行闭卡 | `python tools/verify.py integration --profile card-closeout --report-all` | 现行支付：recipes（唯一 Gradle/`test` + 本波 GameTest）→ census（无 Gradle）→ closeout-seals（无 Gradle）。已关闭卡 `--check` 只比 seal，不因 composed v2 / profiles 把 `complete_family_count` 打成 0 |
| Bath identity 尾账复核 | `python tools/verify.py integration --profile card-closeout`；isolated `.\gradlew.bat runGameTestServer -PwaveRecipes=bath/identity --no-daemon` | 145/34091/3532；remaining 1354；receipt 走 seal 绑定面，不 live-pin composed v2 |
| Bath identity runtime 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=bath/identity --no-daemon` | 验证 145/34091/3532 与 2 条真实 B1 support。Receipt 使用 committed UTF-8 log；被忽略的 `run-wave-bath-identity/` 不是证据 |
| Bath tiny-purified 复核 | `python tools/verify.py integration --profile card-closeout`；isolated `.\gradlew.bat runGameTestServer -PwaveRecipes=bath/tiny-purified --no-daemon` | 5/95；remaining 1349；Bath ordinary 0；receipt 走 seal 绑定面 |
| Bath tiny-purified runtime 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=bath/tiny-purified --no-daemon` | 验证 5/95 与 tiny-washed B1 scatter。被忽略的 `run-wave-bath-tiny-purified/` 不是证据 |
| Storage runtime 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=storage --no-daemon` | Storage suite 验证 624+1 catalog 与 85 台机器 catalog 行未扩张。被忽略的 `run-wave-storage/` 不是证据 |
| Smelter stone runtime 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=smelter/stone --no-daemon` | 验证 407 authored families。Catalog fixture 只进 JUnit |
| Smelter ordinary-closure 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=smelter/ordinary-closure --no-daemon` | 已关闭 ordinary-closure 波 |
| Mixer ordinary-closure 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=mixer/ordinary-closure --no-daemon` | 已关闭 ordinary-closure 波 |
| Drying ordinary-closure 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=drying/ordinary-closure --no-daemon` | 已关闭 ordinary-closure 波；44/85 |
| Electrolyzer ordinary-closure 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=electrolyzer/ordinary-closure --no-daemon` | 已关闭 ordinary-closure 波；46/94 |
| Centrifuge ordinary-closure 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=centrifuge/ordinary-closure --no-daemon` | 已关闭 ordinary-closure 波；126 complete + 2 reclass |
| Autoclave ordinary-closure 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=autoclave/ordinary-closure --no-daemon` | 已关闭 ordinary-closure 波；36 complete + 24 later:recycling |
| Compressor ordinary-closure 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=compressor/ordinary-closure --no-daemon` | 已关闭 ordinary-closure 波；56/1284；remaining gap 0 |
| Centrifuge compact runtime 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=centrifuge/compact --no-daemon` | 精确验证 production lock 22/32 与 34 locked support；157/250 catalog 只进 JUnit |
| Roaster compact runtime 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=roaster/compact --no-daemon` | namespace `cruciblecraft_wave_roaster_compact`，不进日常网格 |
| Assembler compact runtime 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=assembler/compact --no-daemon` | namespace `cruciblecraft_wave_assembler_compact`，不进日常网格 |
| 机器 runtime 隔离 | `.\gradlew.bat runGameTestServer -PwaveRecipes=machines --no-daemon` | namespace `cruciblecraft_wave_machines`，不进日常网格 |
| 历史 READY 收据 | `python tools/verify.py archive-inspect` | 不改写、不按未来全局 recipe tree 重算；核心 runtime ABI 变化由 compatibility/migration profile 统一回归 |
| 玩家发行 | `python tools/verify.py release` | 仅未来发行卡 |

Windows 示例：

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile worldgen
python tools/verify.py integration --profile census
python tools/verify.py integration --profile census-replay
python tools/verify.py integration --profile closeout-seals
python tools/verify.py integration --profile card-closeout
python tools/verify.py integration --profile recipe-partition
python tools/verify.py archive-inspect
```

`dev` 根据 `git diff` 或 `--path` 选择 profile。无法映射的代码路径会列出需要声明的
scope，既不静默通过，也不升级到全部 builder。纯文档改动只跑 Markdown 链接和
profile 静态检查。

`census` 使用 compact/reference currentness；不会在 census、topology 与 readiness 层
重复读取 recipe source dump，也不再调用 `gradlew test`。`census-replay` 默认做 hash/seal
`--check` 与当前波 load decision；全历史 load builder 重跑需要 `--full-replay`。
`closeout-seals` 校验已关闭波的封板。`card-closeout` 串行
`recipes → census → closeout-seals`，顶层 JSON 保留 recipes 的 Gradle XML 与 GameTest；
census-replay 不再是每张卡必付。`integration --profile` 对每个 profile 的 listed
Python modules **只跑一次**
（`run_python_tests.py --suite modules --module …`），不按文件走 `affected` glob 扩族。
`dev` / 无 `--profile` 仍按 git 脏路径走 `affected`。Gradle 任务（profile 全量 `test`
与 `dev` 因 Java 变更触发的 `test`）为 `--rerun-tasks --no-daemon --max-workers=1`。
`recipe-partition` 校验 freeze 到 gap 的诊断链与 owner-track / evidence / lock 链，
不拥有原 census/readiness builders，也不跑 GameTest。
历史隔离 GameTest 用 `-PwaveRecipes=<slug>` 或 `-PrecipeCensus`，都不要混进日常
`cruciblecraft` 网格。

## 硬门与非门

硬门：GT6 compact evidence、JSON schema、生成树一致性、内容卡 load budget。

非门：README、总体规划、阶段档案、工作日志、历史 READY 快照的 live currentness。
census 只把 `docs/current/roadmap.md` 记为 narrative 引用（文件必须存在），
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

历史 recipes-profile ledger 债务是上述规则的实例：worldgen 卡只以 worldgen profile
闭卡；严格 recipes integration 仍是红灯，必须在 census 的 recipe-debt preflight
修复后重跑并关闭该债务。

## 历史全量入口

`python tools/run_full_verification.py --check` 仍可手工运行，但它校验的是历史
单一 READY 证明，不是分层验证之后的日常开发门。CI 的 PR 路径使用 `verify.py dev`。
