# GameTest 红灯清理详细计划

> 计划 slug：`tooling/gametest-red-cleanup`
> capability_slug         = tooling/gametest-red-cleanup
> unique_active_wave      = null
> 状态：prep（不占落地锁）
> 性质：`python tools/verify.py release` 的 GameTest 段在仓库瘦身卡收尾时
> 暴露 52 个失败。本计划把它们分桶、提速、限定轮次清掉。
>
> Java/tick 源：`gt6_code/gregtech6`，revision
> `3703e40308c8c030763fd6297dea8b210d2a77b1`。不从 GitHub 补证据。
>
> 本文件随仓库瘦身卡关卡一起提交。未跟踪的 Jade prep 计划仍会让
> `build_project_status.py --check` 判 stale，跑 release 前先把它挪出 `card-plans/`。

---

## 0. 开场判断：为什么磨时间

```text
1. 验证太重     全量 release ≈ 70–80 min；单组 ≈ 1.5–3 min × 8 组；
               multiblock 测完后 Java 不退出，白挂 55 min
2. 改一个跑一次  每修一处就重跑整组
3. 性质混在一起  「数字过期」「身份/资料决策」「真实行为 bug」一起修
4. 关卡被连坐    仓库瘦身卡没有改运行时代码（改写前后 tree SHA 相同
               5d9cba22…），但 B 项要求 release 全绿，于是被卡前遗留红灯卡住
```

已有但没用上的提速开关（`gradle/scripts/runs.gradle`）：

```text
-PgameTestNamespaces=a,b,c   任意组合测试组，目录 run-game-test-filtered/
-PgameTestFilter=x,y,z       测试名小写子串匹配，只保留命中的测试
```

一次开服跑全部红灯：

```text
.\gradlew.bat runGameTestServer --max-workers=1 --rerun --no-daemon ^
  -PgameTestNamespaces=cruciblecraft_default_grid,cruciblecraft_machines,^
cruciblecraft_energy,cruciblecraft_logistics,cruciblecraft_multiblock,^
cruciblecraft_worldgen,cruciblecraft_content ^
  -PgameTestFilter=<本轮红灯测试名，逗号分隔>
```

结果看 `run-game-test-filtered/logs/latest.log` 里的 `failed at` 和
`required tests failed`，不看 Gradle 退出码。

---

## 1. 用户决定（2026-09-29，R0 已完成）

```text
D1  仓库瘦身卡先关。B 项记为「release GameTest 红灯均为卡前遗留，
    已移交本计划」。本计划按开卡流程升为 unique-active 后执行。

D2  iron.json 改回 HEAD（block、nugget 仍排除，不加 chunk）。
    储物测试的粒/块换成 silver（独立 ingot/nugget/block/plate）。
    没有任何材料开放 chunk，普查里也没有 chunk 需求：
    chunk 测试只校验 36×4 = 144，出现 live chunk 后再跑实物换算。
```

---

## 2. 固定分母

上次全量 release（8 组，修复前）：

```text
default 8   machines 6   energy 5   logistics 11（5 个测试名）
multiblock 4   worldgen 10   content 8   measurement 0
合计 52 条失败
```

content 组修复后重跑：8 → 2。其余 6 组还没重跑。

---

## 3. 分桶

### A. 已改、待验证（只跑，不再改）

| 测试 | 已做的改动 |
|---|---|
| `remainingcircuittiersareregistered` | 断言改用 `TechnologicalPartCatalog.PART_COUNT`（164） |
| `eighthazmatpiecesarewearable` `fullradiationsuitblocksradiation` `fullheatsuitblocksheat` | `HazmatArmorItem` 认新路径 `radiation/hazard_suit_*` `heat/protection_suit_*` |
| `playersurfaceisregistered` | 可能随防护服修好；不过就查 `PlayerCompleteSmoke` 哪个字段为假 |
| `stonelayerreplacesvanillastone` `stonelayerdoesnoteatvillagecobble` `fourgraniteblackrockscraftlayercobble` `stonelayercatalogmatchesloaderworldgen` | `layerStone` / `hasLayerStone` 回落到 GT 石头 |
| `livesqueezerhasgt6javalatexrows` | 浆果不存在时按「不可获得行」处理 |
| 储物 nugget / block / chunk 三条 | 按 D2 改用 silver 与单位校验 |

content 组里已确认通过：纸 tiny_plate、机器目录 314/312、前缀样例。

### B. 数字 / 基线漂移（先列 id，再决定改测试还是改生成器）

| 测试 | 现象 |
|---|---|
| `everylivecomponentrecipetracestogeneratedcomponentjson` | 化学配方 168，测试要 164 |
| `sharedmappublishesfourhundredsixtyfiverows` | 织布机 476 / 465 |
| `livemappublishesallselectedrows` | 熔炉 3547 / 3601 |
| `livemappublishesfourhundredninetytworows` | 层压机 486 / 492 |
| `norecoveryrecipespublished` | 身份子表发布了 1817 条回收配方 |
| `recipepublicationbaselineremainsstable` `recipepublicationmatchespriorbaseline` | 逻辑行 +211720（多出约 6 万，主要是 lazy） |
| `fourhousingsarelive` | `vibramantium/gas_turbine_main_housing` source id 漂移 |

做法：写一个 scratch 导出（`tools/scratch/`，gitignored），一次开服把每项的
live id 集合和期望集合 diff 出来。每项三选一：

```text
测试过期      → 改数字，commit 里写清多/少的 id
生成器回退    → 修生成器，重新 runData（只收真实 diff，还原换行噪声）
说不清        → 标 known-red，交用户
```

禁止：不列 id 直接改数字；放宽断言去接受 hash id。

### C. 资料 / 身份问题（先查 GT6，再动）

| 测试 | 查什么 |
|---|---|
| `orechain{tin,gold,copper}acrossplacedmachines` `concreteorerecipesarepublishedtolivemaps` | 粉碎机矿石链命中的是 compact `gt6/<hash>`，不是 `ore_chain/shredder/{material}/`；具体配方输入是 tiny 离心粉碎矿标签 ×9。用 `gt6_resolve.py` 对照 GT6 判断哪边错 |
| `stainlessbathingpottablehasacquisitionrecipe` | 配方缺失；GT6 零件都在就补，缺零件记 blocked |
| `displaycoverissurvivalcraftable` | Display CPU logic 配方缺失；同上 |
| 储物测试（D2） | 按 D2 结论执行 |

安全阀不变：缺零件就不出配方，不用替代料。

### D. 真实行为 bug（要改运行时代码）

按可能的共同根因分组，每组一个诊断：

```text
D-log  物流覆盖板       sameidentityconnectedexportsintostorage ×2
                        sameidentityconnectedexportsfluidsintostorage
                        importpullsfromstorage ×3
                        coveridentitysurvivesblockentityreload ×3
                        → 疑似同一个覆盖板 tick / 连接根因
D-mb   多方块           biomassrunusesheatandkeepscircuit（无甲烷）
                        wallportcookscobbletostone（供 EU 后控制器不亮）
                        mixedcoilsdonotform（镍铬 + 碳化硅混装成型了）
D-wg   世界生成掉落     stonelayerplacesnetherquartz / stonelayerplacesdenserockores /
                        surfacerocklootmatchesworldgenrocks → 疑似同一战利品根因
D-imm  浸泡效果         geothermalimmersionregenerates / oilimmersionpoisonsandblinds
D-crop 作物获得         cropobtaindoesnotuseitementityscatter
D-eu   线缆             cablelossandoverloadperspecification（锡 wireGt01 拒收 1A）
D-cnt  content 剩余     moldhopperextractswhencool（冷模具漏斗不抽）
                        steamturbineconsumessteam（涡轮不存 RU）
```

---

## 4. 轮次（每轮最多开服一次）

```text
R0  拍板 D1 / D2                                           ~5 min（用户）
R1  过滤运行：A + B + C + D 全部测试名                     ~3 min
    → 得到当前真实红灯表（A 类修好多少一目了然）
R2  并行诊断：D-log / D-mb / D-wg+D-imm+D-crop / D-eu+D-cnt
    各派一个只读 subagent，回报「根因 + 最小补丁位置」；
    同时我跑 B 的 id diff、C 的 gt6_resolve
    → 统一落补丁 → 一次过滤运行                            ~30–60 min
R3  剩余红灯逐个处理；每次攒 ≥3 个修复再开服              视剩余量
R4  全量 verify.py release 一次（Jade prep 计划先挪出）    ~75 min
    → 通过后关本卡
```

运行保护：

```text
- 一次只跑一个 Gradle（同项目会抢锁）
- 看到 "required tests failed" / "All N required tests passed" 后
  若 3 分钟内 Java 未退出，按 pid 结束该 Java（multiblock 挂死先例）
- 过滤名用完整小写测试名，避免子串误命中
```

止损：

```text
- 单个红灯 2 轮修不动 → 标 known-red，写进 §6，问用户
- 需要 GT6 不存在的零件 → blocked，不做替代
- 发现是 DESIGN_POLICY（公共 16 增减、开新形态）→ 停，问用户
```

---

## 5. 不做

- 用替代料、ItemEntity 撒点、创造栏顶「可获得」
- 把长尾前缀改成一人一 id，或把公共 16 收进组件
- 放宽断言去接受 compact hash id
- 不列 id 改棘轮数字 / 发布基线
- `git checkout` / `restore` / `reset --hard` / `clean` / `stash`
- 碰参考树（`gt6_code` 等）

---

## 6. 记录区

### 当前未提交改动（本计划开始前）

```text
PuvOmegaRecipes.java + 7 个配方 JSON     默认组件输入 → 纯物品（已验证可开服）
test_player_complete.py                  隔离环境变量
HazmatArmorItem.java                     新路径识别
TechnologicalPartCatalog.java / CircuitTierGameTests.java   164
StoneLayerStones.java / ModBlocks.java   layerStone 回落
PaperTinyPlateGameTests.java             不再断言 unique id
HammerSqueezerLaserGameTests.java        浆果缺失分支
StorageGameTests.java                    silver 粒/块、chunk 单位校验、目录 314/312
Gt6PrefixRegularBulkGameTests.java       steel/anvil 补命名空间
```

### 轮次结果

（每轮填：运行命令、通过/失败名单、下一步）

### known-red

（无）

---

## 7. 验收

- [x] D1、D2 已拍板
- [ ] A 桶全部通过
- [ ] B 桶每项有 id diff 与处理结论
- [ ] C 桶每项有 GT6 对照结论（补齐或 blocked）
- [ ] D 桶每组有根因记录并通过
- [ ] `python tools/verify.py release` 通过一次，或剩余项全部在 known-red 且用户同意
