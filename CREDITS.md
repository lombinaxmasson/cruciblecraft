# CrucibleCraft — Credits & Third-Party Attribution

## 项目

- **作者**：lombinaxmasson
- **许可证**：LGPL-3.0-or-later（见 `LICENSE`）
- **仓库**：https://github.com/lombinaxmasson/cruciblecraft
- **Issue Tracker**：https://github.com/lombinaxmasson/cruciblecraft/issues

## 固定来源（GT6 / GTM）

CrucibleCraft 的数值、配方与结构数据来源于以下固定 revision 的源代码
replay，逐文件来源指针与 sha 钉见 `tools/` 下的来源政策产物。

### GregTech 6

- **仓库**：https://github.com/GregTech6/gregtech6
- **固定 revision**：`3703e40308c8c030763fd6297dea8b210d2a77b1`
- **许可证**：LGPL-3.0-or-later
- **来源证据**：`tools/worldgen_worldgen_source_policy.json`（worldgen 5 文件）、
  `tools/machine_tree_denominator_manifest.json`（1229 selected Java blobs / 1546
  symbol declarations）、`tools/gt6_reference_metadata.json`（运行时
  6.17.06-22-g3703e4030）
- **使用方式**：世界生成（GT6WorldGenerator / WorldgenOresLarge /
  WorldgenOresSmall / Loader_Worldgen / MT）、材料与配方分母、机器与
  前缀身份。派生数据按 SOURCE_BACKED / SOURCE_DERIVED 分类；平衡性
  调整均为显式 `DESIGN_POLICY`，不冒充来源事实。
- **资产说明**：GT6 默认资产按上游 `LICENSE.assets` 为 CC0 1.0；GregTech
  logo 与其衍生资产按上游 `LICENSE.logos` 为 CC-BY-NC-4.0。本项目不使用
  GregTech logo。
- **砧几何**：`assets/cruciblecraft/models/block/anvil.json` 来自 GT6
  MultiTileEntityAnvil 空态 ISBRH（ID 32025），不是 TerraFirmaCraft。

### GregTech Modern

- **仓库**：https://github.com/GregTechCEu/GregTech-Modern
- **固定 revision**：`de5d2c4a4c863b94a10bfb5d0839df2de8246628`
- **许可证**：LGPL-3.0
- **使用方式**：**仅命名与几何参考**（FluidPipeType.java /
  ItemPipeType.java 逐文件 sha256 见 `tools/gt6_pipe_source.json`）。
  材质资格与管道统计以 GT6 直接注册为准。

## 模板与第三方依赖

- **NeoForged MDK 模板**：MIT（NeoForged 项目，仅适用于模板文件；全文见
  `NOTICE`）
- **NeoForged 参考副本**：`net/neoforged/` 下的参考/补丁文件保留其
  LGPL-2.1-only SPDX 文件头，且不是模组运行时源码集。
- **Jade**（可选，方块信息）：https://modrinth.com/mod/jade
- **EMI**（可选，配方查看）：https://modrinth.com/mod/emi
- **Reliable EMI / REMI**（可选，EMI 物品列表按材料前缀、工具种类、加工机 / 转换器 kind，以及玻璃 / 木板 / 台阶等建筑方块、书架、抽屉、保险箱、箱子、料斗、坩埚、模具折叠；原 EMI++）：https://modrinth.com/mod/reliable-emi
- **YetAnotherConfigLib**（REMI 依赖，不由本模组直接调用）：https://modrinth.com/mod/yacl
- **KubeJS**（可选，启动期材质注册）：https://modrinth.com/mod/kubejs

以上可选兼容模组均未捆绑；运行时按能力门控启用集成。

## 其它对照源

贴图、GT6U、kTFRUAddon、GT6-TFRU 和配方 dump 不随本仓库发布。检出位置见
[代码树 · 参考源](docs/current/code-tree.md#参考源)。

- **贴图**：[wolfram0108/gregtech6_w](https://github.com/wolfram0108/gregtech6_w)
  @ `936083c247a70b1bbc5f19996a83d75c27d196e2`。默认资产 CC0 1.0，logo 例外为
  CC-BY-NC-4.0。
- **GT6U**：[GregTech6-Unofficial/GregTech6-Unofficial](https://github.com/GregTech6-Unofficial/GregTech6-Unofficial)
  @ `4972d0468ee2ea0e896af1e4afe4018d4e2294e6`。LGPL-3.0-or-later，默认资产
  CC0 1.0，logo 例外为 CC-BY-NC-4.0。不是运行时输入。
- **kTFRUAddon**：[kuzuanpa/kTFRUAddon](https://github.com/kuzuanpa/kTFRUAddon)
  @ `75cebb71abb9b5ac5ceb447e3324bd161b6fa7ab`。不是 GT6 权威。
- **GT6-TFRU**：[kuzuanpa/gregtech6-TFRU](https://github.com/kuzuanpa/gregtech6-TFRU)
  @ `35402b05b35d4c7e666233cb37f99d305eeffc8b`。TFRU 的 Waila 表面，不是 GT6 权威。

## 免责声明

- 本模组按上面钉住的 revision 移植 GT6，目标是完整覆盖。当前是开发快照，不是完整移植；覆盖进度见 [GT6 全量覆盖重评估](docs/current/gt6-full-coverage.md)。
- G10 与 GT6U 不在当前范围。
