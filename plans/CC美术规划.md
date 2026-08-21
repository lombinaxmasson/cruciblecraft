# CC美术规划（材质/纹理债清单）

> 建立于 2026-08-15，来源：全仓库纹理/模型/注册盘点。
> 背景债务：总体规划 O-24 ——「16,048 注册形态的完整视觉表现是独立美术工程」（`CrucibleCraft-阶段档案-T10-T12.md:175`，历史债务独立记账，T24 骨架里的「O-24 纹理债盘点」门槛未执行）。规则 5.8：**不新增 placeholder texture**。
> 可用参考：本仓库 `gt6_dump/gt6_recipe_dump/textures/`（gitignored）存有完整 GT6 纹理集元数据——41 物品集 + 41 方块集（`sets.json`）、每材质 textureSet（`materials.json`）、468 前缀的路径模板 `gregtech:materialicons/{set}/{filename}`（`prefix_icons.json`），已部分导入 `tools/gt6_l1b_selected.json`（texture_set_item 分布：SHINY 354 / RAD 328 / METALLIC 138 / DULL 96 / FLUID 44 / FINE 29 / STONE 27 / CUBE 24），但 mod 数据层尚未消费。
> 视觉改动验收方式：玩家进游戏目视验收，不走自动化测试。

## 1. MC 原版物品/方块占位

### 1.1 方块模型直接引用 vanilla 纹理（src/main/resources）

- [x] `models/block/anvil.json` / `crucible.json` → `cruciblecraft:block/metal_surface`（由根目录 `textures/gold-texture.png` 转灰度，保留 GT6 金块表面噪点；仍走材质 tint）。**砧保持玩家 TFC 几何**（`textures/anvil.json` 同源），不使用 GT6 `MultiTileEntityAnvil`。坩埚仍是单块中空，**不用** GT6 `MultiTileEntityCrucible`（那是 3×3）。
- [ ] **坩埚表面未完成**：几何已是中空单块，贴图仍是灰度金噪点 `metal_surface`，看起来像平涂金属壳，不是成品坩埚材质（见 §9）
- [x] `models/block/bronze_boiler.json` → GT6 `largeboiler` 双层贴图 + `machine_cube_2_layer` + bronze tint
- [x] `models/block/bronze_crusher.json` → GT6 `crusher` 双层贴图 + tint
- [x] `models/block/bronze_dynamo.json` → GT6 `electric_rotation` 双层贴图 + tint
- [x] `models/block/bronze_steam_engine.json` → GT6 `MultiTileEntityEngineSteam` 体素 + `metal_surface` tint（不再用 `kinetic_steam` 方块叠层）
- [x] `mortar` / `sifter`（含 steel/titanium）/ `bath` / `smelter`（含 invar/titanium）→ 对应 GT6 研钵/筛台/浸洗盆/小冶炼炉体素 + tint；模具 `ceramic_mold(_filled)` → `MultiTileEntityMold` + firebrick / molten 顶面
- 未接：搅拌碗（CC mixer 仍是工业方块）、动力仓（保留 overlay 方块）、GT6 电池/漏斗/砂轮/浇铸盆（CC 无对应单块）
- 附带：`machine_cube_2_layer.json` 加 `render_type: cutout`（overlay 二值透明正确绘制）；`MachineBlockColor` 纳入四台青铜机。**待玩家目视验收。**

### 1.2 方块模型引用 vanilla 纹理（src/generated/resources，datagen 产出）

- [x] `ceramic_mold.json` / `ceramic_mold_filled.json` → 手写 GT6 模具体素（firebrick + `ceramic_mold_filled_top`）；datagen 只出 blockstate / item
- [ ] **陶瓷模具表面未完成**：GT6 体素已接，贴图仍是平铺 `firebrick`，缺少模具本身的槽/边细节（见 §9）
- [x] `bellows.json` / `bellows_active.json` → CC `bellows_side` / `bellows_front(_active)`（GT6 oak beam + axle）
- [x] `coke_oven.json` → GT6 cokeoven 双层；`firebrick.json` / `firebox.json` → `cruciblecraft:block/firebrick`（升级为 GT6 firebricks）
- [x] `large_boiler.json` → `machine/boiler`；`tank_3x3x3.json` → `machine/tank_3x3x3`；`mortar.json` 改为 MAIN 体素（不再生成 `machine/mortar` 方块）
- [x] `multiblock_casing.json` / `multiblock_item_fluid_port.json` / `multiblock_energy_input_port.json` → CC 自有方块纹理
- [x] `rotational_axle.json` → `cruciblecraft:block/rotational_axle`
- [x] `fluid_deposit_extractor.json` → CC 纹理；`gas_cloud.json` → 半透明玻璃风 + `render_type: translucent`
- [ ] `blockstates/subsurface_fluid_deposit.json` → 仍映射 vanilla stone/deepslate（地下流体标记块；刻意保持宿主石外观，暂不单独纹理）
- [x] 线缆/管道方块模型 → GTM `pipe_side` / `pipe_*_in` / `wire_side` / `wire_end` / `insulation_*`（side vs end，绝缘 overlay）+ tint。连接改为 GTM 默认 GT6：点哪连哪，扳手切面，不自动六向。**待玩家目视验收。**
- datagen 源：`ModBlockStateProvider` + `MachineBlockColor`；已 `runData`。**待玩家目视验收。**

### 1.3 物品图标引用 vanilla

- [ ] `form_items` 覆盖到原版物品：iron/copper/gold 的锭与 raw ore、charcoal gem → 属材料形态管线，并入 §2 GT6 搬运
- [x] **线缆/管道物品图标**：parent 改为南北贯通杆 `conductor/*_item` / `pipe/*_item`（GTM 物品造型：side + end，电缆带 insulation overlay），不再是 2–12px 核心立方
- [ ] **多股导线物品未完成**：已离开 vanilla string，改为按股数的束状几何（`wire_bundle_2/4/8/12/16`，复用 `material/wire` + tint）。可放置单股改 GTM `wiregt01_item` 贯通杆。**仍待目视**：束状是否够像导线。无电气规格的其余 cable 仍见下条
- [ ] 无电气/管道规格材料的其余 cable/pipe → 前缀 JSON `model_texture` string 回退（并入 §2）
- [ ] `ore.json` 死数据 `iron_ore`（运行时已覆盖，低优先）
- [x] `coal_coke` / `match` / `unknown_material` → CC 物品纹理
- [x] 流体容器：`creosote_bucket` / `steam_bucket` / `portable_fluid_tank` / `fluid_cell` / `gas_cell` → CC 纹理
- [x] 泥模系 6 个 → CC 纹理；烧制模具仍 parent `ceramic_mold`（已 firebrick）
- [x] 机器外壳 6 个 casing → CC 纹理（GT6 casingMachine*）
- [x] 盖板 8 个 → GT6 covers 图标
- [x] `extruder_shape_*` 34 个 / `tool_pattern_*` 11 个 → CC 纹理（共享 GT6 底板，后续可逐个细化）
- `ModItemModelProvider` 改为 `generatedCc`；已 runData。**待玩家目视验收。**

## 2. 未搬运 GT6 材质

- [ ] 数据链路断在 tools 层：`texture_set_item` 已进 `tools/gt6_l1b_selected.json`（1,241 条记录），但 `src/main/resources/data/cruciblecraft/materials/*.json` 无任何 texture 字段（材质 JSON 视觉字段只有 `color` + `tint_style`），Java 无对应类
- [ ] 材质 JSON 缺少 `texture_set` 字段（若要消费 GT6 集需加字段 + 编解码 + 指纹考虑）
- [ ] 方块侧 textureSetBlock（41 集）完全未导入
- [ ] GT6 路径模板 `gregtech:materialicons/{set}/{filename}` 可直接复用（sets.json / prefix_icons.json 为现成映射表）
- [ ] tint_style 与 GT6 set 的对应关系未定义（当前 tint_style 分布：matte 1371 / metallic 401 / shiny 1，与 GT6 set 分布 SHINY 354 / RAD 328 / METALLIC 138 / DULL 96 / FLUID 44 / FINE 29 / STONE 27 / CUBE 24 不对齐）

## 3. CC 简陋材质 / 占位纹理

### 3.1 工具

- [x] 对照 GT6 `materialicons/METALLIC/toolHead*_OVERLAY` 与 `iconsets/HANDLE_*_OVERLAY`：除 `knife_overlay`（已有内容，227B）外，CC 所用镐/斧/铲/锄/剑/锉/凿/锯/起子/扳手/锤 overlay **与 GT6 同源即为 141B 全透明**，不能凭空画高光（规则 5.8）。工具头剪影本身来自 GT6 `toolHead*`。
- [ ] 若以后要做「有高光的工具」，需新画 overlay，不在 GT6 搬运范围内

### 3.2 机器

- [x] 规划里点名的 25 个「纯白 overlay 面」已对 GT6 `basicmachines/{id}/overlay`：这些面在 GT6 源里同样是 141B 空图（autoclave/top、extruder/top、lathe/top、press/bottom、rollbender 顶底、rollingmill 顶底、shredder/back、sifter 左右、wiremill 顶底、compressor/right 等）。**不能发明细节。**
- [x] `electric_motor` / `rotational_gearbox` 空面与 GT6 `motors/rotation_electric`、`engines/kinetic_rotation` 一致（只有 front 或 side 有内容）。`fuel_engine` / `burning_gas_generator` 已是 GT6 `generators/burning_*` 实 overlay。
- [ ] `colored/` 只有 4 个模板文件复用 170 个面；`overlay/` 左右侧模板各被 23 台机器共享 —— 这是 GT6 tint 底板机制，不是缺文件
- [x] `large_boiler` → `machine/boiler`、`tank_3x3x3` → `machine/tank_3x3x3` 目录已存在。GT6 `multiblockmains/largeboiler` overlay 同样为空；储罐已接 `tankmetal/overlay_front`（front/top/bottom 有窗格细节）。**待玩家目视验收储罐控制器。**

### 3.3 材料形态图标

- [x] `textures/block/material/block.png` = 灰方块（148-238 灰阶，≈RGB 169）——已作为可放置存储立方 `models/block/material_storage.json` 的 tint 底（§4）
- [x] 有内容的 5 张 `*_overlay.png`（`crushed_ore` / `fine_wire` / `tiny_crushed_ore` / `plate_gem` / `ring`）已接到对应物品 `layer1`（tintindex≠0 不染色）。其余 overlay 与 GT6 同源即为 141B 空图，**不能发明高光**；`block_overlay.png` 仍未用（存储块走立方模型）
- [ ] 空 overlay PNG（141B）可在确认无引用后清理；不在本批

### 3.4 孤儿/重复/瑕疵清理

- [x] `textures/block/firebrick.png` 有成品砖纹理但零引用 → 已接到 firebrick/firebox/ceramic_mold（§1.2）；coke_oven 改用 GT6 双层机纹理
- [ ] 重复文件：`dust.png` ≡ `purified_dust.png`；`handle_chisel.png` ≡ `handle_file.png` ≡ `handle_screwdriver.png`；机器 `side.png` ×4 孤儿
- [x] ghost pixels（透明区非零 RGB）：`long_rod`、`raw_ore`、`quadruple_plate`、`tiny_centrifuged_crushed_ore`、`wrench`、`knife`、`tiny_crushed_ore_overlay` 已清成 RGB 白 + alpha 0；`crushed_ore_overlay` 本来就干净
- [ ] 根目录 Blockbench 草稿：`textures/anvil.json` 已落地为游戏内砧模型；`textures/2.bbmodel`、`textures/crucible.json` 仍未接入，需要清理或落地

## 4. 金属块（storage block）只有物品、没有方块

- [x] **471 个材料**有 `block` 形式且无 `form_items` 覆盖 → 已注册可放置 `MaterialStorageBlock`（`{material}/block`），共享 `block/material_storage` + tint；iron/copper/gold 等 `exclude_prefixes: block` 仍走原版存储块
- [ ] **金属块表面未完成**：可放置，但所有材料共用一张 GT6 METALLIC `blockIngot` 灰度立方 + tint，看起来像纯色方块。分集（SHINY/DULL/…）见 §2、§9
- [ ] 9 锭 ⇄ 1 块合成/分解仍未做（挤出机已有部分 9×ingot→block）；MapColor 目前统一 `METAL`
- [x] 缺 `item.cruciblecraft.material_form.block` 的 zh_cn 翻译键 → 已在 `material_zh_cn.json` prefixes 加 `block: 块`（「铝块」）。可放置方块另有 `block.cruciblecraft.{id}/block`。

## 5. 已完成

- [x] 矿石方块（137 材料 × 石头/深板岩）—— 2026-08-15：原色石头底 + 矿石粒染色（`ore_flecks.png` + `material_ore.json` + `MaterialOreColor`），与锭同色（含 shiny/matte 风格混合）

## 6. 已知渲染问题（待修）

- [x] **矿石方块渲染成黑色**（2026-08-15 玩家反馈，染色改动引入）：根因确认——`ore_flecks.png` 176 个透明像素 RGB=(0,0,0) + 70 个半透明像素，走 solid/半透明 tint 路径渗黑。已修（2026-08-15）：① 透明像素改为 RGB=(255,255,255) alpha=0；半透明二值化为 opaque/transparent；② `material_ore.json` 加 `render_type: minecraft:cutout`。**玩家目视验收通过（2026-08-15）。**
- [x] **破坏方块粒子未跟材质染色**（2026-08-15）：NeoForge `TerrainParticle` 默认会乘 `BlockColors` tint（`areBreakingParticlesTinted`）。问题主要在粒子贴图选错：双层机器 `particle` 曾指向 overlay（细节层），破坏屑像灰片。已改：① 机器 `particle` → `colored/front`（datagen + 青铜机手写模型 + generated 回写）；② 矿石 `particle` → `#base`（宿主石/深板岩屑）。坩埚/砧/`wire`/`pipe` 本就走 tintable 底。**待玩家破坏验收。**

## 7. GUI 搬运（GT6 machines）

> 源：`gt6_code/gregtech6/src/main/resources/assets/gregtech/textures/gui/machines`（87 张 256×256；面板约 176–196×166，可用 vanilla `blit` 默认 256 atlas）。

- [x] 全量复制到 `assets/cruciblecraft/textures/gui/machines/*.png`（文件名必须全小写：1.21 ResourceLocation 禁止大写，PascalCase 会 `ExceptionInInitializerError`）
- [x] `MachineGuiTextures`：机器 registry path → GUI stem；`ConfiguredProcessingMachineScreen` / `CrusherScreen` / `CokeOvenScreen` 不再用 vanilla furnace
- [x] 槽位/进度条：`tools/gt6_gui_layout.py` 把 GT6 `ContainerCommonBasicMachine` 槽位表转成 CC `UiLayout`（`tools/gt6_basic_machine_layouts.json`）；运行时 `Gt6BasicMachineGui` 按 RecipeMap 面板尺寸铺格，再截到 CC 实际槽数。进度条改 blit GUI atlas `(176,0)`。**待玩家开机器目视验收。**
- [ ] 无 GT6 对应贴图的机型：`assembler` 暂用 `crafting.png`（源目录无 `Assembler.png`）；`bender` 暂用 `rollbender.png`
- [ ] 蒸汽链 GUI（锅炉/蒸汽机/发电机等）若有专用屏再接线；当前无独立 Screen 的不在本批

## 8. 规划约束

- 新增纹理遵守规则 5.8（不新增 placeholder）；历史债务走 O-24 独立记账
- GT6 搬运可依赖 `gt6_dump/gt6_recipe_dump/textures/`（gitignored 参考，不入库）与 `gt6_code/gregtech6/.../textures/`；`tools/import_gt6_oredict.py` 已有 texture-set 导入逻辑可扩展
- 材质视觉字段目前只有 `color`/`tint_style`；新增字段需同步 MaterialDefinition codec、MaterialFingerprint（结构 vs 调优指纹的归属要先定）
- 视觉改动由玩家目视验收
- **T30 Hopper 族 ART_DERIVED（2026-08-20）**：不按 60 材料复制 PNG。共享 parent 为 `models/block/hopper.json` / `hopper_side.json`、`queue_hopper.json` / `queue_hopper_side.json`、`dust_funnel.json`；几何来自 `textures/gt6模型/MultiTileEntityHopper.json` 与 Queue/DustFunnel 对应模型。染色层复用已有 `block/material/block.png`（`tintindex: 0`）；Queue 与 Dust Funnel 的视觉区别复用 `block/material/block_overlay.png`（无 tintindex）。Dust Funnel 运行时固定 steel tint。机读来源：`tools/t30_art_asset_provenance.json`（121 个投影成员，`per_material_pngs = 0`）。

## 9. 目视未完成（已接线、仍简陋）

> 2026-08-15 玩家反馈：能放/能认出来，但看起来仍是一坨像素或平涂立方。这些**不算完成**。规则 5.8：能用 GT6 现成贴图或几何解决的继续做；不能发明新高光。

- [x] **线缆物品**：可放置线缆/裸线物品改为 GTM 贯通杆（`conductor/{spec}_item`），绝缘侧用 `insulation_5`、端帽用 `wire_end` + `insulation_N`。世界模型同样 side/end，不再是单贴图像素核。**待目视。**
- [ ] **多股导线**：物品栏是 `wire_bundle_{2,4,8,12,16}` 束状（复用 `block/material/wire`）。这些形态不可放置；可放置的单股/电缆已改 GTM 贯通杆（见上条）。待目视束状是否够像导线
- [ ] **金属存储块**：可放置，表面仍是单一 METALLIC `blockIngot` + 材质 tint。要按材质分 SHINY/DULL/METALLIC/… 需 §2 `texture_set`
- [ ] **坩埚**：单块中空几何已有，表面仍是 `metal_surface` 灰度金噪点
- [ ] **陶瓷模具**：GT6 体素已有，表面仍是平铺 `firebrick`
