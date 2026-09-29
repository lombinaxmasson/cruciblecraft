# 开发与验证指南

现行入口是 `tools/verify.py`。验证的权威是当前 Git revision 上刚执行的命令结果，
不是提交库中的 currentness sidecar、receipt、seal、snapshot 或历史 READY 报告。
Git 负责保存和审查变更；内容摘要不作为开发流程的防篡改机制。

全项目进度按
[能力交付流程](capability-delivery-workflow.md) 晋级。编号卡、历史波次和
编号卡历史收据都已退出工作树；不要再把它们当现行门。

## 日常入口

```powershell
python tools/verify.py dev
python tools/verify.py integration --profile verification
python tools/verify.py integration --profile runtime-java
python tools/verify.py integration --profile semantic-generators
python tools/verify.py integration --profile recipe-generators
python tools/verify.py integration --profile recipes
python tools/verify.py integration --profile capability-runtime --if-changed
python tools/verify.py integration --profile game-tests
python tools/verify.py release
```

- `dev` 根据显式 `--path` 或 Git dirty paths 选择 active profile。
- `runtime-java` 跑全量 `gradle test`，不跑 datagen。普通 runtime Java / 测试 /
  `src/main/resources` 只命中这个 profile。
- `semantic-generators` 跑 generated-art / resource-gate builders、相关 Python
  tests 和两次 `runData`。命中面是 datagen provider、`src/generated` 和贴图
  工具；不跑 JUnit，也不重编已封板的 semantic recipe wave。
- `recipe-generators` 跑 material-form `--check` 和相关 Python 合同。
  `build_semantic_recipes.py --check` 是手工重建，不再当日常门；它会把
  预存在的 generated JSON 漂移当成无关改动的失败。
- `recipes` 是 fresh 配方接入门：机器 delivery sidecar、`import-source --check`、
  临时目录小图/大图 compile。不跑 GameTestServer，不把旧 receipt 当 PASS。
  它是 active-only，不进 `release`。
- 改 datagen provider 会同时命中 `runtime-java` 与 `semantic-generators`：
  JUnit 加上双 datagen。
- `integration --profile` 默认 fresh 执行该 profile 的 builders、Python tests、
  Gradle tasks 和 datagen；不复用旧 PASS。CI 对 `runtime-java`、
  `recipe-generators`、`recipes`、`semantic-generators` 与 `capability-runtime`
  加 `--if-changed`：有 diff base 且本 profile 未命中则 SKIP；没有 diff base（无 `GITHUB_BASE_REF`，且
  `GITHUB_EVENT_BEFORE` 为空或全零）时仍执行，避免覆盖收缩。`release` 始终跑全部
  release profiles。
- 新增方块或物品但只改 registry、不改 `datagen/` 时，这次 PR 不会跑 `runData`。
  `*ResourceTest` 与 `release` 仍覆盖生成树。
- `capability-runtime` 只跑横切 lint、`required_test_ids` 通用检查，以及
  jade / catalog id / registry identity 三个 builder。CI 加 `--if-changed`。
  已关卡的账本测试在 `manual_replay`：`verify.py release` 会跑并写入
  `build/verification/latest.json`，失败会打印，但不改变 release 退出码。
  关卡时不要把这类测试加回常驻 profile。持续有效的证明写 GameTest 或 JUnit。
  拆分前这个 profile 有 69 个 Python 模块、30 个 builder；现在是 5 个模块、
  3 个 builder。56 个模块降级前先跑过，243 个测试、190 秒、全部通过。
  旧的整份 capability-runtime 墙钟没有测过。横切 lint 大约 14 秒。
  改 `HopperBlock.java` 只选中 `runtime-java` 和 `game-tests`，不再带上那
  56 个账本模块。改一份已关内容卡的 `capability.json` 只选中
  `player-complete`，以及 `test_capability_ledger`、`test_required_test_ids`、
  `test_player_complete`。共享账本哈希针已经删掉，改无关字段不会再因哈希变红。
  生成类 builder 的 `--check` 只核对它写出的资源；不扫 Java 源码文本，也不读
  别的卡的计划。常驻检查在没有 `gt6_code` 时不去读参考树：catalog id builder
  会打印 `SKIP catalog_modern_ids GT6 name source` 并继续做结构核对。
  石头层、连接件获得和对应测试在 `manual_replay`。材料形态需求普查用已翻译的
  铜族名单，不再为了常驻测试打开 `gt6_code`。
- `release` 对当前 checkout fresh 执行 release profiles。它不读取历史报告来代替运行。
- Registry census 不是 active profile。日常由材料闸门与手写配方形态测试覆盖
  缺失形态；完整 registry 冻结子集探针用
  `.\gradlew.bat runGameTestServer -PrecipeCensus`，在 release 或改注册表后跑。
- 裸 `.\gradlew.bat runGameTestServer` 只跑 `cruciblecraft` 命名空间里的占位测试
  （NeoForge 在已启用命名空间为零测试时会崩溃）。领域网格用
  `-PgameTestGrid=<id>`，名单在 `tools/gametest_grids.json`。
  卡进行中的临时命名空间仍用 `-PwaveRecipes=<slug>`，提交的 holder 不能留下
  `cruciblecraft_wave_`。排错时加
  `-PgameTestFilter=方法名片段`（逗号分隔，大小写不敏感，按测试全名包含匹配；
  PowerShell 里给整个 `-PgameTestFilter=...` 加引号）。
  过滤只用于定位，不能代替网格关卡。GameTest 服务器不写关服区块存档。
- `game-tests` 跑领域网格：有路径范围时只跑改动碰到的网格，`release` 或无范围时
  跑全部 8 个。每个网格只认本次 `run-game-test-<id>/logs/latest.log`。
  发现数低于该网格源码 `@GameTest` 计数、失败、崩溃或未执行 required 测试都是
  FAIL。不读取历史 `gametest_receipt.json`。
  `release` 会跑它。改 `src/main/java`、`src/test/java` 或对应资源会选中它。
  配方卡仍用自己的隔离 namespace，不把这次服务器测试塞进 `recipes`。
- 怎么写测试见[测试制作规范](test-authoring.md)。
- 每次结果写到被 Git 忽略的 `build/verification/latest.json`。报告只包含 revision、
  dirty paths、命令、测试计数、环境和 PASS/FAIL，不包含文件摘要。

无法映射的代码路径只报告、不失败。`docs/**`、`*.md`、LICENSE / NOTICE 与
验证隔开：改文档不选任何 profile，fork 也可以不带文档树。工作副本噪音、
生成根冻结和 GameTest 迁移边界见 [代码树与工作副本](code-tree.md)。

Gradle 验证任务使用 `--rerun`，只强制命令行上的目标任务执行；未变化的
`compileJava` / `processResources` 可以 UP-TO-DATE。本地和同一 CI job 复用
Gradle daemon。`release` 通过 `CRUCIBLECRAFT_GRADLE_ISOLATED=1` 隔离 daemon。

`dev` 对未匹配 profile 的路径只报告、不失败。改 `.gitignore` 或未登记的
helper 脚本不再把整次验证打成 exit 2。path-scoped 运行只执行受影响的
Python 模块。没有 `docs` profile，也不跑 markdown 链接检查。

## 即时证据

启动 smoke（可选 `--client`）只证明客户端第一 tick 没炸。
收据只写到 `build/verification/receipts/`。

```powershell
.\gradlew.bat runClient
```

人工 `player_signoff.json` 是受版本控制的评审声明，不是机器执行收据。

## 生成物与 JSON 合同

- datagen 连续 fresh 生成两次，按相对路径清单和逐文件字节比较确认确定性。
- builder 的 `--check` 在临时目录重建输出，并直接比较解析后的 JSON 结构或文件字节。
- production lock 冻结人工审核的语义集合、计数和来源 revision，不绑定内部文件摘要。
- 外部 Source Pack 的文件摘要继续校验；它用于确认外部输入身份。

## 保留的功能性摘要

以下摘要参与运行时协议、缓存或外部分发，不能因本次切换删除：

- `MaterialFingerprint`、`GeneratedMaterialPackCache`、`RegistryCodecHash`
- compact publication/shard membership 与 recipe identity
- 多人握手、生成包缓存
- `tools/recipe_bulk/source_pack.py` 的外部输入完整性
- 发布产物 checksum

不要把 `inputs_sha256`、`builder_sha256`、currentness sidecar 或 closeout seal
重新接回日常开发证明。这些是流程禁令，不是靠全仓库正则扫描来锁的。
`check_no_workflow_hashes.py` 和 `check_zero_milestone_names.py` 仍可手工跑，
但不再是 `verify.py` 的 verification profile 门。能力 JSON 里的电压档
（T2 / T5）和 production lock 的 `selection_sha256` 不是工作流哈希。

## 历史档案

编号 builders、currentness sidecars、verification sessions 与 DAG
列入 `tools/legacy_verification_index.json`。它们不会被 active policy import、
调度、重签或用于路线图进度。numbered closeout seal 与 `archive/sealed` 已从工作树
删除；需要历史调查时从 Git 历史读原字节。不要把它们重新接回 CI。

已知验证债务见
[`tools/known_issues/verification-debt.json`](../../tools/known_issues/verification-debt.json)。
配方域仍适用
[ordinary recipe wave 的语义合同](recipe-wave-workflow.md)，但 Card/Seal 不再是
active verification 单位。

## 还没拆的危险门

这些现在不挡日常 `dev`，但还是脆弱耦合。回头另开验证卡再改，不要在内容卡里顺手修。

1. **VD-2026-09-002 已废的晋级 `runClient`。** `player-complete` profile 的
   `--run --all` 现在是空操作。CI `promotion` 不得再启动客户端。
2. **VD-2026-09-003 Java 投影整数锁。** hopper 60/120、storage 625、EMI 28 台等
   `assertEquals(N, catalog.size())` 仍在。来源分母（核能 11/9/8、电池 37）要留；
   全仓库投影计数不要。
3. **VD-2026-09-005 `build_semantic_recipes.py --check` 预存在红。** smelter
   `gt_recipe_smelter_0099.json` 与重建不一致。已从 `recipe-generators`
   卸下；手工 `--check` 仍会失败。不要为了变绿去重挂回 verify。
4. **VD-2026-09-006 gate overlay 与 authority 不一致。**
   `material_registration_gate.json` 多了 `fission_survival_required_forms`，
   `material_form_authority.json` 没有。日常 `--check` 已不再比 overlay；
   数据漂移还在。
5. **VD-2026-09-007 Gradle 隔离与 card-fast。** 精简卡已关，card-fast / 晋级分层 / 共享
   `build/test-results` 并发保护未实施。
