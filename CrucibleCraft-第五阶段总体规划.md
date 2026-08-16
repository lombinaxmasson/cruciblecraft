# CrucibleCraft 第五阶段总体规划

> GregTech 6 → Minecraft 1.21.1 NeoForge 移植 · 第五阶段  
> 状态：T26_READY；当前唯一 active T 是 `T27`  
> 硬前置：已满足；本文件在 T27 开卡后成为执行入口  
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`

---

## 0. 定位与权威边界

第五阶段把公开 Beta 后的工作分成两部分：

1. **T27 · v1.0 portfolio freeze**：重算并冻结既有 v1.0 缺口，只为既有
  `v1_required` 生成 T28+；
2. **1.x content portfolio**：在 1.0 发布后，依优先级推进五条内容轨 A–E。

本文件是第五阶段的权威规划入口。T26 已关闭；T27 开卡后生成 portfolio 契约与分类产物，不得发布 GT 行、注册对象或配方内容。

### 0.1 v1.0 契约保持不变

继续采用《[CrucibleCraft-第四阶段总体规划.md](CrucibleCraft-第四阶段总体规划.md)》
与 `tools/phase4_v1_planning_contract.json` 的产品契约：

> CrucibleCraft v1.0 表示“GT6 风格工业主链完整且可稳定发布”，不表示 GT6 或
> GT6U 全量移植。

因此：

- T27 后仅为**既有** `v1_required` 生成连续卡；清零后才可创建 RC / GA 卡；
- 核裂变、聚变、等离子、GT6U、全部 95 个 RecipeMap、全部多方块 kind 默认不阻断 RC；
- `crucible` 已是唯一未实现的 `v1_required` 多方块，必须在 T27 中拥有唯一 owner，
并在 T28+ 中作为有界 thermal / steelmaking 行为卡处理；
- 轨 A–E 的新增内容属于 1.x portfolio，不能借规划文字悄然扩张 1.0 阻断范围。

任何想将 A、B 或其他内容提升为 v1.0 要求的提议，必须先显式修改产品契约、重新评估
RC 条件和载荷预算；本规划不作该升级。

### 0.2 执行规则

- 任一时刻最多一张 T 卡处于进行中；
- “五条轨”是 portfolio 分类与排序维度，不是并行开发许可；
- 所有实现卡都须独立交付 closure、fidelity、load 三本账；
- 新 identity、来源冲突或范围发现先回到 portfolio 分类，不插入未登记工作；
- 本文的数字是 2026-08-15 的规划基线；开卡时由对应 artifact 重新派生，不以本文
的历史数字覆盖新测量。

---



## 1. T27 · v1.0 portfolio freeze

T27 是第五阶段唯一入口，也是纯分类/测量卡。其原始接口与退出条件由
《[CrucibleCraft-第四阶段总体规划.md](CrucibleCraft-第四阶段总体规划.md)》定义。

**一句话判据**

> 消费七类 canonical denominator、T20–T26 的 READY 与测量结果、所有 deferred 与
> open item，为每项建立唯一 CC implementation、disposition、owner、replacement
> condition 与 recheck point；令 `unclassified = 0`，并且只为实际剩余的
> `v1_required` 生成连续 T28+。



### 1.1 前置条件

T27 开卡前必须同时满足：

- T26_READY 由完整验证会话绑定；
- T26 已知问题台账包含 severity、workaround、owner 与 release disposition；
- T24-F003 / T24-F005 已在声明的环境复测，或有明确 release disposition；
- anvil_bend_big、anvil_bend_small 的 registered-zero-logical 状态已拍板；
- 未实现的 crucible 多方块已有 owner 与 kind-to-implementation 命名方案；
- O-15 已按玩家可见域关闭；
- 当前工作树与 `--check-ready` 符合 T26 的收尾要求。



### 1.2 T27 的范围与非范围

T27 必须：

- 重放 T13 的七类分母，并消费 T20–T26 的 current readiness / load / release evidence；
- 对每个 identity 写入唯一 disposition、owner、来源类型与重新检查条件；
- 锁定既有 v1_required 集合，确定 T28+ 的顺序；
- 为 A–E 输出 map / identity / 行级范围、依赖和初始载荷投影；
- 记录本规划引用的 legacy logical=21,000 门禁与 T14 policy 的差异，禁止隐式选择其中
一个。

T27 不得：

- 发布 GT logical/eager/lazy 行、新机器、注册对象或内容数据；
- 把 map 级 `PORTED` / `BOUNDED_SUBSET_PORTED` 当成全部 source row 已逐行移植；
- 把历史 T13 `in_scope` 直接等同于 v1_required；
- 提前虚构 T28+ 的张数、工期或 RC 编号。

**完成信号**：`unclassified = 0`；v1_required 的 T28+ 可由 portfolio 确定性生成；
五轨的 owner 与初始投影可复算；T27 的 publication delta 为零。

---



## 2. 计量与载荷边界



### 2.1 数量语义不能混写

所有第五阶段报告必须区分：


| 数量                       | 含义                     |
| ------------------------ | ---------------------- |
| `source_fact`            | 固定 GT6 revision 的规范化事实 |
| `authored_rule`          | 经审查的 CC 规则声明           |
| `datapack_file`          | 具体资源文件                 |
| `logical_row`            | 完整运行时关系                |
| `eager_row` / `lazy_row` | 立即物化 / 按需保留的逻辑关系       |


不得用 source row ÷ logical row 制作“移植百分比”；也不得从 authored-rule 数量推导
logical 或 publication 数量。

### 2.2 当前预算基线

`tools/t14_load_budget_policy.json` 的硬门禁为独立轴，不存在可任意分配的统一 logical
容量池。以当前 `full_verification_report` 规划快照为基线：


| 轴                         | 当前     | hard ceiling | 规划余量   |
| ------------------------- | ------ | ------------ | ------ |
| eager publication rows    | 16,657 | 21,000       | 4,343  |
| lazy logical rows         | 2,225  | 56,000       | 53,775 |
| datapack authored entries | 3,243  | 6,600        | 3,357  |


`4,343 + 53,775 = 58,118` 仅是两个独立余量的算术和，**不是** 58,118 条可随意分配的
logical budget。既有 T22 / T22.5 的 logical=21,000 口径仍在部分测试与文档中存在；
在 T27 明确 policy、verifier 与文档的最终关系前，不得据 58,118 对任何大内容批做
“装得下”的承诺。

每张内容卡还必须投影并验证 datapack、sync（64 MiB hard）、reload/index、
retained memory、allocation、lookup、cache 与操作计数；未测量的维度是
`BLOCKED_PENDING_MEASUREMENT`，不能以猜测值放行。

---



## 3. 1.x portfolio：五条内容轨



### 3.1 轨 A · 既有机器的 ordinary_optional 长尾

**候选分母**：T22.5 已在 10 个 `BOUNDED_SUBSET_PORTED` map 中精分出
78,682 条 `ordinary_optional` 行。它们是已存在机器上的候选内容，不能被描述为
已移植的 source rows。

**首卡 A0**：仅测量，不发布内容。对每个候选 map 计算：

- source row → exact distinct runtime relation 的可投影集合；
- 可翻译性、身份归一化与冲突；
- authored / logical / eager / lazy 的独立预测；
- 所有独立 hard gate 的余量与预测值；
- 可闭合的玩家链与 card-size 边界。

T14 的 Extruder 先例只能证明该 family 的压缩协议：
325,595 source rows 经精确稀疏关系表达为 2,782 logical relation，且其中有 20 条
compact authored rule；它**不能**预测其他 map 的压缩比。

后续按依赖闭合的 recipe family 拆卡，而不是机械地“一 map 一卡”。先用
assembler、autoclave、compressor、centrifuge、drying、electrolyzer 等小范围校准
流水线，再拆 bath、smelter、mixer 等大范围。roaster 当前属于 `DEFERRED` 且无
运行机器，应移交轨 E。

任一卡若预测超出任一硬轴，必须先扩载荷架构、重新测量或缩小里程碑；不得直接发布。

### 3.2 轨 B · 多方块 portfolio

30 个 canonical multiblock kind 已由 T23 分类：6 个 v1_required、19 个 post_1_0、
5 个 out_of_scope。已实现 v1_required 为 centrifuge、coke_oven、
distillation_tower、large_boiler、tank_3x3x3；`crucible` 留给 T27/T28+。

多方块 JSON schema 与 plugin 白名单可复用，但这不等于“新结构不需要 Java”：

- 现有 tower、centrifuge、boiler、tank 都有专用 Block、BlockEntity、controller spec、
注册与逐结构 GameTest；
- 19 个 post_1_0 kind 中 12 个是 processing_host，但只有 7 个已有 CC RecipeMap，
其余 5 个缺失可运行 map；
- conversion ×4、thermal ×1、storage ×1、logistics ×1 的运行时边界不同；
- crusher / shredder / sluice 的 hazard、spin 等特殊行为不能被普通 host 假定覆盖；
- cryo_distillation_tower 依赖 deferred 的 CRYO 身份；不能混入普通数据批。

因此轨 B 的结构为：

1. **v1** `crucible` **行为卡**：固定 canonical kind 与新 controller/block id 的映射
  （例如 `large_crucible`），避免与现有 `cruciblecraft:crucible` 冲突；关闭
   thermal / steelmaking、mold UI、存档和玩家路径的有界要求；
2. **B0 declarative-controller 边界卡**：只评估 post-1.0 processing/storage 能否以
  数据实例复用现有 runtime，输出逐 kind 的 Java、spec、GameTest 与载荷需求；
3. **可证明的数据子批**：仅包含 B0 验证通过且有可运行 host 的 processing/storage
  结构，按矿处、化学、storage 等闭合玩家路径小批交付；
4. **独立行为卡**：conversion、thermal、logistics、cryo 和特殊环境行为各自建立
  物理/运行时门禁，不能伪装成数据补充。

“publication 接近零”仅指多方块自身通常不新增 GT logical/eager/lazy relation。
每个结构仍须独立记账 vanilla 获取配方、注册对象、controller profile、资源/本地化、
菜单/网络/存档，以及 T23 级 GameTest 载荷。

### 3.3 轨 C · 核裂变、聚变与等离子

轨 C 是新物理系统，而非少量 RecipeMap 配方补全：

- `gt.recipe.fusionreactor` 的参考配方为 18 行，`gt.recipe.fuels.plasma` 是空 map；
- 两个 ReactorCore machine kind 与反应堆棒相邻交互、热/中子模拟是不同层次的分母；
- `fusion_reactor` 多方块当前为 out_of_scope；
- NEUTRON identity 是 `deferred_with_reason`，其 topology 为
`DEFERRED_REACTOR_INTERNAL`；它不是八个 out_of_scope energy identity 之一；
- CC 目前没有核、聚变或等离子 controller plugin / 可验证物理模型。

**首卡 C0** 必须是 source / physics 测量与设计卡，产出固定来源锚点、反应堆组件与
状态机分母、热/中子/冷却边界、能源身份、每 tick 操作上限、存档/网络模型和 load
测量方案。C0 关闭前不得估算实现工期或作发布承诺。

轨 C 与轨 E 的 `ENERGY_AND_FUEL_SYSTEM` maps 必须在 portfolio 中有唯一 owner；不得
以“核相关”同时归属两轨。

### 3.4 轨 D · GT6U 来源与 provenance

当前仓库只有固定 GT6 revision 的 tree/blob/symbol artifact 与本地 GT6 dump；未固定
GT6U revision、tree/blob manifest、symbol inventory 或 recipe dump。`GT6ImportUnits`
中的 `U` 是 GT6 材料单位，不能作为 GT6U 来源证据。

轨 D 的首卡是 T13 等价的来源获取卡：

1. 固定可再现的 GT6U revision 与许可证边界；
2. 建立 tree/blob/symbol manifest 和 recipe dump；
3. 派生可复算的 canonical denominator；
4. 对 GT6、GT6U 共名/冲突/新增 identity 建立显式 lineage 与裁决；
5. 扩展 currentness 与 fidelity 词表，防止 GT6 事实与 GT6U 事实混写。

GT6U 引入前，现有单一 GT6 `SOURCE_BACKED` 链保持不变。轨 D 不承诺“GT6U 全量移植”。

### 3.5 轨 E · 64 个 DEFERRED RecipeMap

`tools/gt6_map_roadmap.json` 记录 95 个 map 的 map 级状态：


| status                | map 数 | reference rows |
| --------------------- | ----- | -------------- |
| PORTED                | 16    | 441,899        |
| BOUNDED_SUBSET_PORTED | 13    | 163,141        |
| DEFERRED              | 64    | 106,159        |
| OUT_OF_SCOPE          | 2     | 9,642          |


这些是**map 级规划状态**，不是逐行移植量。64 个 DEFERRED map 也不都等同于“待实现机器”：
其中包括 empty pinned map、reference / analysis map、packaging / automation、chemical /
thermal、energy / fuel、biology 与 specialized-machine 等不同类别。

**首卡 E0** 建立 DEFERRED map 的二级分母与依赖图：对每个 map 写明可运行 host、
玩家循环、source 行级 disposition、依赖能源/物理、owner 与 replacement condition。
E0 后由依赖闭合的内容族生成实现卡；本阶段不承诺完成全部 64 map 或 106,159 行。

---



## 4. 1.x 维护与发布纪律



### 4.1 缺陷分流

公开 Beta 与 1.x 期间按以下规则处理：


| 类型                     | 处理                                              |
| ---------------------- | ----------------------------------------------- |
| 已关闭判据被推翻的回归            | 在当前 active T 的有界修复单元中处理，并补回归测试                  |
| 存档损坏、复制漏洞、安全、启动崩溃、主链阻断 | 可暂停 active T，进入受限 maintenance/release 单元        |
| 一般正确性缺陷                | 进入 known-issue 台账；仅明确 blocking 者进入已登记 portfolio |
| 完备性、体验、性能问题            | 进入台账和 portfolio；性能升级须有 T24 协议证据                 |


不重开已关闭 T；在原阶段档案记录修订与被补的测试边界。

### 4.2 Hotfix

允许在不新开内容 T 的情况下发布 beta / 1.x hotfix，但必须同时满足：

- 变更仅属于已登记 regression 或 maintenance 修复单元；
- 不夹带新 family、新注册对象或未登记内容；
- 所有 publication delta 挂在当前 active T 或 known-issue id 的 ledger；
- 执行一次完整 `run_full_verification.py --record`，不能仅凭 `--check-ready` 发版；
- CHANGELOG、已知问题台账与 release disposition 同步。

具体机器可读门禁在 T27 后的 `tools/phase5_portfolio_contract.json` 中建立；该文件在
T26_READY 前尚不存在，也不得提前成为 READY 输入。

---



## 5. 风险与止损


| 风险                  | 早期信号                            | 止损                          |
| ------------------- | ------------------------------- | --------------------------- |
| 轨 A 的 relation 投影过大 | A0 超任一 hard gate                | 停止发布；调整规则架构、重新测量或缩小范围       |
| 大 family 吞没单卡       | 小范围流程尚未闭合即开始 bath/smelter/mixer | 先完成小 family 校准；按玩家闭环继续细分    |
| 多方块被误判为纯数据          | B0 需要新的 BE/spec/测试或 map 不存在     | 改为独立行为卡，不以 JSON 存在作为完成证据    |
| 核物理无限膨胀             | 未完成 C0 即编写实现                    | 退回来源/物理测量卡，冻结发布承诺           |
| GT6 与 GT6U 事实冲突     | 一个 identity 有两个未裁决来源            | 在 D0 完成 lineage / 裁决前不合并内容  |
| 轨 E 范围失控            | 新 map 未有 host 或行级 disposition   | 回到 E0；按类别和依赖图分批，不承诺全量       |
| 热修复侵蚀主线             | hotfix 夹带内容或未完成 record          | 拒绝发版，改为已登记 T / portfolio 工作 |


---



## 6. 文档与契约关系

- 不可违反的架构不变量与开工检查：
《[CrucibleCraft-总体规划.md](CrucibleCraft-总体规划.md)》
- 第四阶段关闭事实、v1 产品契约与 T27 原始接口：
《[CrucibleCraft-第四阶段总体规划.md](CrucibleCraft-第四阶段总体规划.md)》
- 当前唯一执行入口与 T26 收尾：
《[CrucibleCraft-交接说明.md](CrucibleCraft-交接说明.md)》、
《[CrucibleCraft-阶段档案-T26.md](CrucibleCraft-阶段档案-T26.md)》
- 讨论历史而非权威规划：
《[plans/CrucibleCraft-第五阶段总体规划草稿.md](plans/CrucibleCraft-第五阶段总体规划草稿.md)》

T26_READY 后，T27 首先创建 `tools/phase5_portfolio_contract.json`，将本规划的版本边界、
五轨 identity、owner、load projection、非承诺项和 hotfix gate 固化为机器可读数据。
在此之前，本文件只定义规划边界，不改变当前项目状态。