# CrucibleCraft — Credits & Third-Party Attribution

## 项目

- **作者**：Lorbineitte Masson
- **许可证**：All Rights Reserved（本模组代码与资源）
- **Issue Tracker**：https://github.com/icodestuljh/cruciblecraft/issues

## 固定来源（GT6 / GTM）

CrucibleCraft 的数值、配方与结构数据来源于以下固定 revision 的源代码
replay，逐文件来源指针与 sha 钉见 `tools/` 下的来源政策产物。

### GregTech 6

- **仓库**：https://github.com/GregTech6/gregtech6
- **固定 revision**：`3703e40308c8c030763fd6297dea8b210d2a77b1`
- **许可证**：LGPL-3.0-or-later
- **来源证据**：`tools/t20_worldgen_source_policy.json`（worldgen 5 文件）、
  `tools/t13_denominator_manifest.json`（1229 selected Java blobs / 1546
  symbol declarations）、`tools/gt6_reference_metadata.json`（运行时
  6.17.06-22-g3703e4030）
- **使用方式**：世界生成（GT6WorldGenerator / WorldgenOresLarge /
  WorldgenOresSmall / Loader_Worldgen / MT）、材料与配方分母、机器与
  前缀身份。派生数据按 SOURCE_BACKED / SOURCE_DERIVED 分类；平衡性
  调整均为显式 `DESIGN_POLICY`，不冒充来源事实。

### GregTech Modern

- **仓库**：https://github.com/GregTechCEu/GregTech-Modern
- **固定 revision**：`de5d2c4a4c863b94a10bfb5d0839df2de8246628`
- **许可证**：LGPL-3.0
- **使用方式**：**仅命名与几何参考**（FluidPipeType.java /
  ItemPipeType.java 逐文件 sha256 见 `tools/gt6_pipe_source.json`）。
  T8 材质资格与管道统计以 GT6 直接注册为准。

## 模板与第三方依赖

- **NeoForged MDK 模板**：`TEMPLATE_LICENSE.txt`（MIT，NeoForged 项目，
  仅适用于模板文件）
- **Jade**（可选，方块信息）：https://modrinth.com/mod/jade
- **EMI**（可选，配方查看）：https://modrinth.com/mod/emi
- **KubeJS**（可选，启动期材质注册）：https://modrinth.com/mod/kubejs

以上可选兼容模组均未捆绑；运行时按能力门控启用集成。

## 免责声明

- 本模组不声称是 GT6 或 GT6U 的完整移植；v1 范围为"工业主链"。
- 核裂变/聚变/等离子控制器、G10 及 GT6U 内容不在 v1 范围，亦不在
  v1 阻断清单。
