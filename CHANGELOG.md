# CrucibleCraft Changelog

## 2026-09-23 至 2026-09-28

自 `0.1.0-test.20260922.1` 之后。下面的新增和修复都在 `0.1.0-test.20260927.1` 里。09-28 还有两条没打进那个 jar：坩埚外壳朝向，以及浇铸盆和坩埚熔毁对齐。

### 新加

- 大型机器：洗矿槽、内爆压缩机、榨汁机。焦炉补上煤、褐煤、油页岩等原料；各地石砖和苔石砖可以做出来。
- 基础加工机：燃烧搅拌机、催化裂化机、结晶坩埚、蒸汽裂解机，以及它们的配方。搅拌器和洗矿槽补上钢、钛、钨钢合成。
- 配方批量：前缀常规加工、化学与杂项、蒸汽裂解。挤压机按配方发布，并打开第二输出槽。加工机的流体产出做到罐子真正满为止。
- 多方块：两种空心立方储罐（含 5×5×5 金属壳）、大型高压釜。大型热交换器底环作为输入舱。蒸汽轮机把蒸汽转成 RU。聚变结构搭失败时会解开舱口。
- 流体容器：桶、鼓、罐和物流罐。金属鼓可以合成，也能和坩埚右键转移液体。
- 七种流体：塑料、冰、石化流体、充能物质、炽热之血、炽热之泪、蓝莓汁。生产仍停在对应机器或外置物品还没接上的地方。
- 材料：长尾形态、仓储粉块。曲板用自己的图标。
- 能源：太阳能板、电池盒、水晶充电器、魔法吸收器。激光、磁铁、零点模块转换器进目录。电激光用罐装二氧化碳；两个零点放电器可以抽出零点模块；选择器盖板限制激光输出。
- 世界：地表木棍。
- 去掉 kTFRU 小燃气轮机和旧的共享砧。

### 修复

- 大型坩埚：按金属着色，去掉重复锅壁。炼钢时把动能当成空气，并在装料时清掉空气，避免动能越积越多。外壳按半格锅壁取样，准星描边贴在锅壁上。09-28：外壳面朝外，外表面不再被剔掉。
- 09-28：流体储罐、鼓、罐和物流罐放下后能看见材料纹理。拿容器右键坩埚可以舀出最轻的熔融金属。浇铸盆的中文名改成浇铸盆；里面的熔融金属填满内腔，凝固后是一块金属块。坩埚在离熔毁还差 100 度时壳体变黄并发光。小坩埚、大坩埚、浇铸盆、模具和水龙头熔毁后变成会往下流、然后消失的流动岩浆。
- 蒸汽机：转速每秒采样一次，缓冲满了不再被当成已经过载。活塞离开前面板。
- 实心燃烧箱按熔炉燃烧刻给热，焦炭也算在内。热量不再被一块固定大小的缓冲区卡住。
- 加工机重新按配方要电。蒸汽机和流体接口对齐 GT6。
- EMI：同类堆叠折在一起，组装机界面摆正，配方页打开时才建。重载过程中创造模式物品栏可以打开。
- 金属流体罐的中文名改成储罐。GT6 砧的中文名恢复。
- 方块信息改走 Jade。紧凑配方线可以写方块状态。

安装文件：`cruciblecraft-0.1.0-test.20260927.1.jar`（Minecraft 1.21.1、NeoForge 21.1.243、Java 21）。仍须新开世界。玩家说明：[docs/current/player-guide.md](docs/current/player-guide.md)。

## 0.1.0-test.20260922.1 (2026-09-22)

六个运行时回归已修复并完成 Java 验证。

- 安装文件：`cruciblecraft-0.1.0-test.20260922.1.jar`（Minecraft 1.21.1、NeoForge 21.1.243、Java 21）。
- 修复大型坩埚同步、铸造模具入口、GT6 木材/面板模型、矿石熔炼资源、加工机获得配方和仓储获得资源。
- Melter 的 GT6 `ANY.Iron` 中型流体管使用已注册的 steel 同族管道；`runtime-java` 全部 1,239 个测试通过。
- 仍须新开世界。玩家说明：[docs/current/player-guide.md](docs/current/player-guide.md)。

## 0.1.0-test.20260919.2 (2026-09-19)

小群私测第二包。含朋友反馈修复，以及小燃气轮机、矿石宿主、石层和小砖等工作树。

- 安装文件：`cruciblecraft-0.1.0-test.20260919.2.jar`（Minecraft 1.21.1、NeoForge 21.1.243、Java 21）。
- 仍须新开世界。玩家说明：[docs/current/player-guide.md](docs/current/player-guide.md)。

## 0.1.0-test.20260919 (2026-09-19)

小群私测快照，不是公开测试、不是 RC、不是 GA。不要上传 CurseForge / Modrinth / GitHub Release。

- 安装文件：`cruciblecraft-0.1.0-test.20260919.jar`（Minecraft 1.21.1、NeoForge 21.1.243、Java 21）。
- 内容是当前工作树：已超过历史 `0.1.0-rc.1` 冻结（混合材料身份、MTE runtime、裂变、仓储贴图、铸造 hosted 改动等）。
- **必须新开世界。** 未做 DataFixer；旧 beta/rc 存档会丢公共 16 前缀物品或对不上坩埚方块。
- 玩家说明：[docs/current/player-guide.md](docs/current/player-guide.md)。

## Source archive maintenance (2026-08-21)

- License changed from All Rights Reserved to LGPL-3.0-or-later; see `LICENSE`,
  `NOTICE`, and `CREDITS.md` for third-party attribution.
- This maintenance entry does not create a player release or change the
  `0.1.0-rc.1` mod version.

## 0.1.0-rc.1 (2026-08-21)

Pre-RC board freeze. This is an RC package, not GA and not 1.0.0.

### RC1 范围

- 冻结 T30 Hopper / Queue Hopper / Dust Funnel 完成树；本卡无新机器、方块、物品、材料或 RecipeMap。
- 关闭 T24-F003 / T24-F005：在 ≥16 GiB 物理内存上实测 target（250 管）与 stress（1000 管）。T24 历史 SKIP 仍保留在 `tools/t24_scale_evidence.json`。
- source-derived GT 配方与 bounded dust 保真度不变；publication delta 0/0/0（18766 logical / 16541 eager / 2225 lazy）。
- Handshake `NETWORK_VERSION` 仍为 `"1"`；Hopper 菜单不把协议错误伪装成 GUI 超时。
- 安装文件：`cruciblecraft-0.1.0-rc.1.jar`（Minecraft 1.21.1、NeoForge 21.1.243、Java 21）。
- 可选依赖按现有合同：缺 EMI/Jade/KubeJS 可启动；存在时客户端加载 EMI/Jade。

### 存档

复制的 beta.1 世界可在本候选上加载。不要用唯一玩家存档做升级试验。

### 已知问题

无 RC 阻断项。T14 hard ceiling 全绿。4.5 体验项仍是 `post_beta_polish`，不阻断 RC。

## 0.1.0-beta.1 (2026-08-14)

首个公开 Beta 候选。工业主链内容按第四阶段规划 T20-T25 全部关闭。

### 包含内容

- **世界资源（T20）**：129 条大型矿脉目录（40 large / 75 explicit-small / 1 dynamic，固定 GT6 来源），全部 profile v2 可从运行时 registry 放置；原油与天然气地下流体矿藏。
- **材料与铸造（T2-T7）**：材料目录（1,774 种 GT6 来源材质）、坩埚合金、陶瓷模具铸造、矿物学处理链（研钵/磨粉/粉碎/筛分）。
- **机器（T10-T17）**：青铜/钢/不锈钢/钛多级加工机器（破碎机、压机、离心机、电解机、蒸馏机等 29 种），能量审计与获取路径闭合。
- **能源（T18）**：三域能源（HU 热 / KU 动能 / RU 旋转 / EU 电），火箱→锅炉→蒸汽机、燃油引擎→发电机、天然气发电的完整守恒链（SOURCE_BACKED 转换率）。
- **物流（T19）**：5-tick 位置相位调度的流体/物品管道，6 种 cover（泵/过滤/阀/选择器等），逐块精确损耗的电缆网络。
- **石油化工（T21-T22.5）**：原油蒸馏→燃料/润滑油→燃油引擎，天然气→甲烷→燃气发电的完整下游链。
- **多方块（T23）**：蒸馏塔 / 大型锅炉 / 3×3×3 储罐（含 port 供应与 quarantine 生命周期）。
- **可复现规模基线（T24）**：small/target/stress 三场景声明、workload 指纹、有界计数 CI 硬门禁。
- **实测阻断修复（T25）**：T24 findings 全量落 disposition（5 条 non_blocking，selected=0，blocking=0）。

### 本地化

- en_us 3,173 键（v1 玩家可见域完整）；zh_cn 873 键：v1 关键路径材料（208 种，含全部主链金属/合金/燃料/矿石/化工）+ 机器/GUI/消息域完整；材料长尾（1,566 种）显式 post_1_0（回退英文显示）。

### 安装要求

- Minecraft 1.21.1 + NeoForge ≥ 21.1.243（jar 自包含，无捆绑库）
- 可选：EMI（配方查看）、Jade（方块信息）、KubeJS（启动期材料注册）

### 已知问题（详见《玩家指南》）

全部来自 T25 disposition 台账（5 条 non_blocking，无一阻断 Beta）：

| id | 概述 | 状态 |
|---|---|---|
| T24-F003 | route 发现上限（32,768 根管）声明但未规模实测 | non_blocking，T27 RC 复测 |
| T24-F005 | target/stress 墙钟/内存/网络未实测（无 ≥16 GB 声明环境） | non_blocking，T27 RC 复测 |
| T24-F001/F002/F004 | 管道相位/多方块校验/cover 负载边界已验证 | 无动作 |
