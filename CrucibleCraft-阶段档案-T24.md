# CrucibleCraft 阶段档案 · T24

> 阶段：T24 · 可复现规模基线
> 状态：✅ `T24_READY`
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 权威产物：`tools/t24_readiness.json` · 场景指纹：见 `tools/t24_workload_manifest.json` 的 `workload_identity`

## 1. 关闭判据

T24 没有新增任何游戏内容（配方 / 机器 / 方块 / 物品 / 多方块均为零增量，
publication delta = 0 / 0 / 0）。它把"模组跑多大规模不会炸"变成可重建的
证据：三个声明规模场景（small / target / stress）由
`tools/t24_workload_policy.json` 数据描述（显式 `DESIGN_POLICY`），
`tools/build_t24_workload_manifest.py` 确定性派生 manifest 与场景身份指纹
（同输入同指纹，可从空目录重建，无黄金存档）。

CI 硬门禁只锁四类：场景身份、守恒、每 tick/每操作有界计数、声明上限。
墙钟时间在任何地方都不做正确性门禁。证据按
`STATIC_INFERENCE` / `SYNTHETIC_BENCHMARK` / `MEASURED_AT_SCALE` 三级分类；
采不到的指标显式 `SKIP` 并携带 reason / blocked_conclusions /
replacement_condition / recheck_point 四字段。

## 2. T24a · 三种 workload 与确定性 manifest

- 三个场景为 JSON 数据描述 + 运行时构建，绝无黄金存档；
- small = 最小完整工业主链（3 机器 RU/KU/EU、converter 对 2、管 20、
  cover 4、tank_3x3x3 一座），在 GameTest 中真实搭建并运行到确定摘要；
- target = Beta 预期持续规模（24 机器、8 转换对、管 500、T23 三种多方块）；
- stress = 同拓扑密度放大（96 机器、32 转换对、管 2,000、每种多方块 4 座）；
- 管道数刻意选 500 / 2,000：5-tick 位置相位调度下每 tick 恰好到期
  100 / 400 根（`PipeTransferPhaseTest` 已证），可直接写成 CI 硬门禁
  （`STATIC_INFERENCE`）；
- `energy_converters` 数 **converter 对**：pair1 = firebox+boiler
  （80 HU + 1 mB → 160 mB steam），pair2 = fuel engine+dynamo
  （32 RU → 22 EU + 10 loss）。交接稿中 "steam engine→dynamo" 草图
  在代码中物理不成立（KU 输出 vs RU 输入），已在 `derivation_notes`
  中记录修正与理由。

## 3. T24b · 正确性与性能证据分离

`src/test/java/com/masson/cruciblecraft/scale/ScaleWorkloadBoundTest.java`
（10 个 JUnit 测试）产出有界计数的确定性证据：

- 管道相位：500→100、2,000→400 每 tick 到期（纯 JVM，`STATIC_INFERENCE`）；
- 多方块校验：真实 tank_3x3x3 定义 + 计数 accessor，27 位置 → 54 操作、
  25 port → 25 次 blockEntity 读（`SYNTHETIC_BENCHMARK`）；
- port 扫描：2 ops/port（tank 50；target/stress 最坏为 tower 160）；
- route 上限 32,768 / cache 256 / cover 负载 13 bytes / menu 3 ints：
  全部与 t19/t23 声明值逐条比对；
- `manifestCountsMatchDeclaredBounds` 把 manifest 与硬门禁绑死。

small 场景的 5 个 GameTest（`t24Small*` / `t24WorkloadMutationFailsStructureGate`）
真实搭建、运行 warmup 200 + sampling 1,200 tick、断言确定摘要与守恒、并证明
少放一台机器时身份门禁**失败**（门禁有效性的反证测试）。

target/stress 的墙钟/内存/网络指标显式 `SKIP`（无声明测量环境；
`t24_scale_evidence.json` 的 `measured_at_scale` 段），操作计数门禁不受影响。

## 4. T24c · 三轴收尾

- `tools/t24_findings.json`：5 条 finding 全部非阻塞（blocking = 0），
  2 条 skipped_measurement，每条含证据种类、证据指针、复现命令与失效边界；
- publication delta 从 `t23_publication_baseline.json` 的 registered_deltas
  求和断言为 0 / 0 / 0；
- `tools/build_t24_readiness.py` 从证据自动派生 `T24_READY`，
  任何 gate 不通过则 fail-closed 不写 status。

## 5. 三轴账

**Closure**

- 场景 3/3 定义，manifest 确定性重建，workload_identity 稳定；
- small 场景 GameTest 5/5 执行；变异门禁测试存在且真实失败；
- finding 5 条，pending = 0。

**Fidelity**

- 场景数值显式 `DESIGN_POLICY`，无冒充 source-backed；
- 没有任何 `SYNTHETIC_BENCHMARK` 被标为 `MEASURED_AT_SCALE`；
- target/stress 的 SKIP 四字段齐全（replacement_condition 指向
  ≥16 GB 内存的声明环境，recheck_point = T26 Beta candidate）；
- 配方/物流/能源分布全部来自 T20–T23 实际运行集合。

**Load**

- publication delta = 0 / 0 / 0（logical / eager / lazy）；
- 操作计数类门禁全部进入 CI 硬门禁；墙钟类全部附带环境声明或 SKIP；
- 无跨机器硬秒数正确性门禁；
- 新增注册对象为 0：本地化/模型债增量为 0。

## 6. 验证与交接

关闭绑定：

```text
python tools/rebuild_artifacts.py --verify
python tools/run_python_tests.py --suite closure
.\gradlew.bat test
.\gradlew.bat runGameTestServer
python tools/run_full_verification.py --record --new-session
python tools/run_full_verification.py --check-ready
```

`T24_READY` 绑定一次完整验证会话：报告 `status = READY` 且
`t24_readiness_acceptance.status = "T24_READY"`。T25 已随后以 selected = 0
关闭；当前唯一 active T 是 T26。

**交给 T25**：唯一输入为 `tools/t24_findings.json`。finding 总数 5、
blocking 0、skipped_measurement 2。blocking = 0，因此 **T25 允许以
`selected = 0` 且全部 disposition 完整关闭，不做预防性重写**（第四阶段
规划原文允许）。每条 SKIP 的 replacement_condition 与 recheck_point
见 `t24_scale_evidence.json` 的 `measured_at_scale` 段，供 T26 Beta
candidate 复跑时补测。
