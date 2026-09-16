# CrucibleCraft 玩家指南（0.1.0-rc.1）

> 本指南对应 RC 候选 `0.1.0-rc.1`。这是预发布候选，不是正式 GA，也不是 `1.0.0`。
> 安装、首玩路径、已知问题与支持范围。

## 1. 安装

1. 安装 Minecraft 1.21.1 与 **NeoForge 21.1.243 或更高**（Java 21）；
2. 把 `cruciblecraft-0.1.0-rc.1.jar` 放入 `.minecraft/mods/`；
3. （可选）EMI `1.1.24+` 查看配方、Jade `15+` 查看方块信息；缺 EMI / Jade / KubeJS 仍可启动；
4. 启动游戏，确认模组列表中出现 CrucibleCraft 0.1.0-rc.1。

**多人**：服务端安装同一 jar；客户端与服务端的材质定义必须一致
（不一致会在连接阶段被明确拒绝并给出差异列表）。Handshake `NETWORK_VERSION` 仍为 `"1"`。

## 2. 首玩路径（五节点）

按顺序走通以下五步即完成本指南的首玩验收。全部步骤在生存模式
（无创造/指令捷径）下可完成。

### 节点 1 · 获取并处理世界资源

- 在主世界探索并寻找大型矿脉（地表与地下均有大型岩层状矿体）；
- 原油与天然气是地下流体矿藏，用管道 + 泵提取；
- 用研钵/粉碎/筛分链把矿石处理成粉末，进坩埚熔炼。
- 原版纸已改成 GT6 口径：3 甘蔗合成 **1** 张纸（不再是原版 3→3）。
- 原版熔炉仍是 8 圆石（尚未换成带点火物的配方）。

### 节点 2 · 从热力/蒸汽进入能源与机器阶梯

- 固体燃料燃烧室已删除（余量卡）；GT6 Burning Box 分档已进游戏。热力加工用
  燃气燃烧室出 HU（天然气 → 甲烷 → `bronze_burning_box_gas`），固体燃烧室
  可用煤 / 焦炭宝石出 HU。
- 锅炉（80 HU + 1 mB 水 → 160 mB 蒸汽；蒸汽过半罐才从顶面喷出，过热或满汽会炸）→ 蒸汽机（200 mB 蒸汽 → 50 KU，
  固定 12 KU/t）驱动 KU 机器（压机等）仍可用，但需要上述 HU 源；
- 中期：燃油引擎（1 单位燃油 → 512 RU）经轴/齿轮箱 → 发电机
  （32 RU → 22 EU + 10 损耗）→ 电缆（逐块精确损耗）驱动 EU 机器
  （电解机等）。电加热器把 EU 转 HU，电引擎把 EU 转 KU。
- 后期切片（工作树，非 RC1 发行承诺）：LU 光纤只传 LU。裂变堆芯 / 棒 /
  LV Canner 已关 `runtime_ready`。大型热交换器、蒸汽涡轮与聚变 18 行已在
  工作树作为 CC 扩展落地（`frozen`，不是生存终局）。冷却器仍未开。

### 节点 3 · 三种下游产品

- 蒸馏机处理原油 → 燃油 + 润滑油；燃油驱动引擎闭环；
- 天然气 → 甲烷 → 燃气燃烧室 `bronze_burning_box_gas`（1,536 单位 → 1,152 HU + 9 mB 废气）；
- 以任意一种下游产品完成一个"开采→加工→供能"闭环即达标。

### 节点 4 · 至少一个自动化循环

- 例：泵 cover 从流体矿藏抽油 → 管道 → 蒸馏机 → 燃油管道 →
  引擎 → 发电 → 机器加工，全程无人工搬运。

### 节点 5 · 形成并运行一座多方块

- 蒸馏塔 / 大型锅炉 / 3×3×3 储罐任选一座；
- 按结构摆放控制器与 port（错放会给出诊断且不破坏已放方块）；
- 运行并确认产物/流体正常进出 port。

## 3. 旧存档支持

- **复制的 `0.1.0-beta.1` 世界可在本候选上加载**；不要用唯一玩家存档做升级试验；
- **processing_version ≤ 3 的存档可加载**（含开发期存档）；
- 旧 `tier_profile` 字段会被隔离（机器暂停、给出诊断、数据不丢）；
- 未来更高版本写入的存档：加载、隔离、数据保留，升级回支持版本后
  自动解除隔离；
- 本模组从未公开发布过 0.1 之前的版本，**不承诺任何外部旧档的兼容**
  （O-39）。

## 4. 已知问题（release disposition）

无 RC 阻断项。小负载管网条目已在 ≥16 GiB 环境实测关闭（target 250 管、
stress 1000 管）；当时的历史 SKIP 仍保留，不伪写成当时已测。4.5 体验项
仍是 `post_beta_polish`，不阻断 RC。

| 严重度 | 问题 | 现状 / 规避 | 责任方 | disposition |
|---|---|---|---|---|
| medium | 第一次进世界可能卡住几分钟（配方展开已排除） | 等首次加载结束；已排除配方 epoch 主因 | 4.5 P0 | post_beta_polish |
| low | 扳手/剪线钳九宫格 | 管道用扳手点九宫格；电缆/裸线用剪线钳 | 4.5 P1/P2 | post_beta_polish |
| low | 创造栏线/缆/管分页、工具与高档机器展示 | 可放置线与缆同页，管单独成页；有配方的工具变体与已注册档位会进创造栏/EMI | 4.5 P3/P5/P6/P7 | post_beta_polish |
| low | 创造栏灌装流体 | 创造栏 cell 按已注册流体预填 | 4.5 P9 | post_beta_polish |
| info | 地表石子 | 新生成的主世界地表按 GT6 `WorldgenRocks` 放 32757 石子（空石头 / 燧石 / 陨铁），不是整表 `c:rocks` | worldgen | closed |
| info | 石层石子 | 主世界按 GT6 `WorldgenStoneLayers` 把原版石头/圆石/深板岩/花岗岩族/凝灰岩换成层立方体（黑花岗岩等），并在石面 1/128 放 32757；不写层矿、不吃原版矿格 | worldgen | active |
| low | 形态标签命名空间 | 粉族/缆/管等走 `c:` 标签，便于 EMI 折叠 | 4.5 P8 | post_beta_polish |

## 5. RC1 范围与支持范围

本候选冻结 Hopper / Queue Hopper / Dust Funnel 完成树：60 个材质 Hopper、
60 个 Queue Hopper、一个 `steel_dust_funnel`（dust / small_dust / tiny_dust
的 1 / 4 / 9 有界换算）。无新机器、方块、物品、材料或 RecipeMap。source-derived
GT 配方与 bounded dust 保真度不变。
工作树（2026-09-14）已超出该 RC 冻结（电转换、LU、裂变、电池芯、连接件 /
MTE 家族 runtime、PUV/OMEGA CC 扩展），见
[冻结与未实现账本](unimplemented-gap.md)；RC 发行范围仍以上面为准。

- 支持：1.21.1 + NeoForge ≥ 21.1.243 + Java 21；单机与 dedicated server；
- 可选依赖：缺 EMI / Jade / KubeJS 可启动；存在时客户端加载 EMI / Jade；
- 不支持：G10、GT6U 内容、GT6 全量 720,841 行配方移植（v1 只承诺工业主链）；
- 裂变：46 棒 / 2 堆芯 / LV Canner 已关 `runtime_ready`（U-238 蒸馏水蒸汽可喂现有蒸汽机）。
  8 个独立热流体与热量合同已关 `runtime_ready`（蒸馏水仍出蒸汽；`Coolant_IC2` /
  `Thorium_Salt` 仍 blocked）。堆芯 HU Jade、辐射/烫伤、8 件防护服、温度计与
  盖革行为，以及 37 电池 / 179 转换机 Jade 已关
  `runtime_ready`（[裂变观测安全与能源 Jade](../history/card-plans/closed/裂变观测安全与能源Jade详细计划.md)）。
  空盖革可用铝胶囊单元容器 + 氦/氖/氩灌装获得。8 个单体热交换器已关
  `runtime_ready`（`FM.Hot` → HU；钨钢效率 90%）。大型热交换器 17197、
  蒸汽涡轮与聚变 18 行已在工作树落地，但 capability 停在 `frozen`+`paused`
  （CC 扩展，不是生存完成档）。冷却器仍未开。不当成可玩终局；
- 本包是 RC，不是正式 GA，也不是 `1.0.0`；soak 只接 release blocker；
- 反馈：https://github.com/icodestuljh/cruciblecraft/issues（附上
  版本号 + 复现步骤 + 存档/日志）。
