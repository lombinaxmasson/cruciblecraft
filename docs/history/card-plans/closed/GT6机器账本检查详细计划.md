# GT6 机器账本检查详细计划

> 计划 slug：`portfolio/gt6-machine-ledger-audit`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。本文件位于 `card-plans/closed/`。
> 性质：机器与多方块总计划的第 0 张。只对账，不新做主机。
> 总计划：[GT6 机器与多方块总计划](../prep/GT6机器与多方块总计划.md)。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = portfolio/gt6-machine-ledger-audit
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
prep_owned_paths             = 本文件
landing_owned_paths          = 本文件分母里点名的 capability.json、
                               这些已关/prep 计划末尾的读法修订、
                               tools/capabilities/ledger 由生成器重写
landing_depends_on           = content/gt6-fluid-barrel 已关闭
close_target                 = runtime_ready
```

`content/gt6-fluid-barrel` 已关闭。本卡占这一次 unique-active。

---

## 0. 为什么先做

总计划后面的锅炉、储罐、发酵机都假设「已关卡的合同等于现在的代码」。
这个假设已经破了。

蒸汽机是例子，不是蒸汽涡轮。涡轮是暂停的 `energy/steam-turbine`（STEAM→RU），不在本卡关闭名单里。

`energy/converter-catalog` 于 2026-09-06 关闭，正文写明不做蒸汽机动力学，青铜仍固定 12 KU/t，要另开动力学卡才能换。
现在的运行时已经换过：

- `SteamEngineKuCurve`：GT6 `(mOutput*(mState+1))/16`。青铜名义包仍是 12，发出去的是 6–24，随储 KU 变化。
- `SteamEngineBlockEntity`：正负行程、邻居没收下也扣 KU、`mState > 30` 锁停并排汽、排气是蒸馏水。
- `SteamEngineKuCurveTest` 已锁「名义 12，含 6 与 24」。
- 裂变 Jade 卡写过「不得拆掉蒸汽机 KU/行程字段」，说明行程已经是活行为。
- `energy/converter-catalog` 的 `required_test_ids` 仍只有燃烧室四条，note 不提曲线。

已关计划保持当时的关闭段不动。本卡在文末加读法修订，并把活行为写进 capability 的 note 和测试 id。不把 2026-09-06 改写成「当时就做了动力学」。

## 1. 分母

只查下表。要加行，先改本文件。

### 1.1 该关：计划已在 closed，capability 仍 paused

| slug | 现况 | 本卡允许的出口 |
| --- | --- | --- |
| `machines/large-electrolyzer` | 计划已关，`frozen` / `paused`。结构与 GameTest 已在 | 现有 `required_test_ids` 本次 fresh PASS → `workflow=accepted`。失败则留在总计划第 3 张，本卡不改电解行为 |
| `machines/large-autoclave` | 同上。note 写明曾让出 unique-active | 同电解。失败则留总计划第 4 张 |
| `machines/large-fermenter` | 同上。背面偏移输出还没有对应用例 | 缺输出坐标测试就**不能**关。记入总计划第 5 张。本卡不补那条测试 |

### 1.2 过期：accepted 或 prep 的文书落后于后来的代码

| 文书 | 过期句 | 代码 |
| --- | --- | --- |
| `docs/history/card-plans/closed/能量转换机目录详细计划.md` 与 `energy/converter-catalog` | 动力学未做；青铜固定 12 KU/t | `SteamEngineKuCurve`、行程、排汽、蒸馏水排气 |
| `docs/history/card-plans/closed/能量系统余量详细计划.md` | 「另开动力学卡才能换」 | 动力学已经在 `SteamEngineBlockEntity`，没有对应 capability 句 |
| `docs/history/card-plans/prep/大型洗矿机详细计划.md` | 还在等电解机让出锁 | `machines/large-bathing-vat` 已 `runtime_ready` / `accepted` |

覆盖表把物质制造机写成 `identity_only`、把旋转引擎写成 `runtime_code_uncarded`。代码分别在已关的 `machines/gt6-coil-hosts` 和 `content/gt6-mte-drive-runtime`。本卡只把这个差异写进读法修订。改覆盖扫描器另说，不在本卡改 `build_reconciliation.py`。

## 2. 每条只许三种出口

1. **accepted**：现有关闭合同和现有测试都对得上这次 fresh profile。只改 `workflow`。
2. **amend**：行为已经在 `src/main`，文书还是否认它。补 capability `note` 与 `required_test_ids`，在已关计划**文末**加一节读法修订。历史关闭段、关闭日期、当时的「不做」清单保持原样。
3. **leave**：测试缺、合同和代码冲突、或还要改结构。本卡写一句缺口，实施回到总计划里的原序号。

禁止在本卡里改锅炉容量、储罐阀门、发酵配方、聚变或 `EnergyType`。

## 3. 蒸汽机修订要写明的活合同

读法修订用这些事实，不把名义 12 和发出去的 6–24 写成互相推翻：

- 青铜 `outputPacket` 名义大小仍是 12。这是曲线的 `mOutput`，不是每 tick 固定输出。
- 实际 KU 包是 `SteamEngineKuCurve.outputKu`：状态 7 → 6，状态 15 → 12，状态 31 → 24。
- 蒸汽消耗仍是 200 mB 一批，KU 入账按效率 basis points，见 `SteamConversion.engineKuPerBatch`。
- 发出去的包带行程符号；抽空条件是 `tOutput * 2 > nominal` 且库存大于 `tOutput`。
- 本卡不新开 `energy/steam-engine-kinetics`。余量卡说的「另开动力学卡」已经由这段运行时兑现，挂回 `energy/converter-catalog` 的 note 即可。

`required_test_ids` 增补现有用例名，不新写一套行为。至少包括 `SteamEngineKuCurveTest` 里名义 12 覆盖 6 与 24 的那条。GameTest 只在现有 `steamEnginePowersSifterThroughKu` 一类已经断言行程或曲线时才挂上；没有断言就不把旧的「通电就能转」算成动力学证明。

## 4. 关闭

- 分母每一行都有 accepted、amend 或 leave 三选一，写在本文件文末。
- `python tools/build_project_status.py --write` 之后，大型洗矿机 prep 不再读得像一张还要落地的卡。
- 总计划第 3–5 张里被本卡 accepted 的，不再占下一轮落地锁。
- 试玩 cycle 不因纯账本修订作废。本卡不改获得格、不改世界生成。

## 5. 分母出口（2026-09-27）

多方块网格 `runGameTestServer -PgameTestGrid=multiblock`：48 条，7 条失败。失败名单没有大型电解机的 `required_test_ids`。

| 行 | 出口 | 说明 |
| --- | --- | --- |
| `machines/large-electrolyzer` | accepted | `controllerIsLive`、`bareControllerIsNotFormed`、`formedStructureBindsBottomInAndTopOut`、`survivalRecipeIsPresent` 都不在失败名单。失败的那条 `survivalRecipeIsPresent` 报的是碳化硅线圈配方缺失。该卡自己的 python 合同要求 accepted 同时是 `runtime_ready`，所以 maturity 一并改了。没有改结构、配方或测试。总计划第 3 张不再占落地锁。 |
| `machines/large-autoclave` | leave | `controllerHullAcceptsAnyFaceIo` 失败：17112 壳体朝下拒收物品。`autoOutputsDownWithoutAutoInput` 失败：没有自动输出到下方箱子。留总计划第 4 张。本卡不改高压釜行为。 |
| `machines/large-fermenter` | leave | 偏移 5 的输出坐标、堵塞、重载没有用例。`biomassRunUsesHeatAndKeepsCircuit` 只断言了朝向的 `destinationSide`，而且这次没有产出甲烷。留总计划第 5 张。本卡不补那条测试。 |
| `energy/converter-catalog` 与已关的能量转换机目录 | amend | capability note 与 `required_test_ids` 补上 `bronzeNominalTwelveSpansInclusiveSixToTwentyFour`。已关计划文末有读法修订。2026-09-06 的关闭段未改。不新开 `energy/steam-engine-kinetics`。`steamEnginePowersSifterThroughKu` 没有断言曲线，没有挂上。 |
| 已关的能量系统余量 | amend | 文末读法修订。「另开动力学卡」已由 `SteamEngineBlockEntity` 兑现，记在 `energy/converter-catalog`。 |
| prep 大型洗矿机 | amend | 文末读法修订。那份文件对的是 17104 大型浸洗器，主机是已 accepted 的 `machines/large-bathing-vat`。不要再落地 `machines/large-bath`。洗矿机是 17107 sluice。 |

覆盖表差异当时只写读法，本卡不改 `build_reconciliation.py`：

- 物质制造机当时仍可能被扫成 `identity_only`。活主机在 `machines/gt6-coil-hosts`（没有单独的已关计划文件），note 已写明 `MatterFabricatorStructure` 与 `matterFabricatorOsmiumCoilsArePorts`。
- 旋转引擎当时仍可能被扫成 `runtime_code_uncarded`。读法修订在已关的 `content/gt6-mte-drive-runtime`。`rotationEngineConvertsRuToKu` 与 `rotationEngineSoftHammerStopsInput` 不加进 `required_test_ids`。

同日后续（本卡已关，用户要求更新覆盖表）：`matter_fabricator` 别名到 `machines/gt6-coil-hosts`，`MultiTileEntityEngineRotation` 别名到 `content/gt6-mte-drive-runtime`。冻结分母的 disposition 未改。旋转引擎那两条 GameTest 仍不进 `required_test_ids`。

关闭用 `--change-class none`。试玩 cycle 不延长。
