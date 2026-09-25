# GT6 缺失流体详细计划

> 计划 slug：`fluid/gt6-missing-fluids`
> 状态：已关。本文件位于 `card-plans/closed/`。
> 正式名称：GT6 缺失流体
> 性质：注册翻译链修复后仍真缺、且 GT6 原生的流体。
> 总计划第 3 张落地卡，见 [GT6 批量移植总计划](../prep/GT6批量移植总计划.md)。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = closed
capability_slug              = fluid/gt6-missing-fluids
unique_active_wave           = null
landing_owned_paths          = ModFluids.java；materials/*.json（molten_fluid）；machine_fluid_mapping.json；
                               流体贴图与语言；src/test/.../gametest/
landing_depends_on           = 翻译链映射修复已完成；当前 unique-active 空窗
partial_close_allowed        = true（逐个流体关，未决策的保持 blocked）
```

---

## 0. 开场判断

首轮逐行分类列了 10 种缺流体。其中 `sluicejuice`、`mercury`、`glass` 在 CC 已有，只是缺别名，
归翻译链修复。本卡处理剩下的。

## 1. 分母

| GT6 流体 | 受影响行 | GT6 定义 | 来源 | CC 现状 | 本卡处置 |
| --- | ---: | --- | --- | --- | --- |
| `plastic` | 356 | `MT.Plastic` | GT6 | 材料在，`molten_fluid: false` | 开熔融流体 |
| `chargedmatter` | 652 | `FL.MatterCharged` | GT6（质量制造） | 只有 `matter_neutral` | 新注册；不拿中性物质顶 |
| `ice` | 145 | `FL` 流体 `ice`（Near Frozen Water，273 K，绑 `MT.Ice`） | GT6 | 材料在，无流体 | 注册 `ice`，不是熔融冰 |
| `blueberryjuice` | 136 | `FL.Juice_Blueberry` | GT6 食物 | 无 | 与 blocker `worldgen/food` 一起看；本卡只在有消费机器时注册 |
| `fieryblood` / `fierytears` | 各 1,095 | `FL.FieryBlood` / `FL.FieryTears` | GT6 定义，主要用于暮色森林兼容 | 无；`machine_fluid_mapping.json` 记 out_of_scope | 移植（负责人 2026-09-24 决定） |
| `petrotheum` | 434 | `MT.Petrotheum` | 热力基础 | 只有粉 | 移植（同上） |

三种兼容流体按 GT6 自己的定义注册（名称、颜色、温度、密度取 GT6 源），不引入暮色森林或热力基础依赖。
它们在 GT6 里的原始生产来源是别的 mod 的物品；CC 没有这些物品时，只注册身份并让消费配方可用，
生产路径记 blocked，不发明来源。`machine_fluid_mapping.json` 的 out_of_scope / no_cc_fluid 同步改为 mapped。

开卡前重跑逐行分类，以当时的流体缺口为准。

## 1.1 获得格（D0）

每种流体至少有一条 live 生产路径（熔融机、质量制造机、榨汁等），否则只注册身份并记 blocked，
不做空获得。

## 2. 实施

1. `ModFluids` 注册；`materials/*.json` 改 `molten_fluid`；`machine_fluid_mapping.json` 改为 mapped。
2. 贴图：GT6 流体图标迁入，写美术清单。
3. 每种流体一个 GameTest：生产一次、进罐、被一条消费配方用掉。

## 3. 验证与试玩

```powershell
python tools/verify.py integration --profile capability-runtime
.\gradlew.bat runGameTestServer -PwaveRecipes=fluid/gt6-missing-fluids
python tools/waves/portfolio/gt6-full-coverage-reassessment/build_semantic_coverage.py --write
```

人工 `runClient`：熔融塑料进罐、倒出、EMI 可见。

## 4. 明确不接管

- 除上表三种以外的其他 mod 流体
- 染料流体、激光气体、`ic2coolant`（各有 blocker）
- 用相近流体顶格

## 5. 关闭清单

- [x] 每种流体已注册；生产路径记在 `tools/waves/fluid/gt6-missing-fluids/obtain.json`，目前全部 blocked
- [x] 三种兼容流体按 GT6 定义注册，生产路径 blocked（来源是别的 mod 的物品）
- [ ] 人工 `runClient` 签收
