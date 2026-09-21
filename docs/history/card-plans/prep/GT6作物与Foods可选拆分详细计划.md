# GT6 作物与 Foods 可选拆分详细计划

> 计划 slug：`content/gt6-crop-food-split`
> 状态：prep。不占 unique-active。落地锁已空。
> 本文件位于 `card-plans/prep/`。
> 正式名称：GT6 作物与 Foods 可选拆分
> 性质：把作物杂交、named plant-form overlay 和 Foods 物品拆成可选附属，
> 同时保留现行材料身份契约。不是 form-open 全量。不是 Juicer/Fermenter 主机卡。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 食物贴图：本地 `gt6_referencable_port_code/gregtech6_w` `gt.multiitem.food`。
> 不创建 `capability.json`。

```text
lane                         = prep
capability_slug              = content/gt6-crop-food-split
unique_active_wave           = null
source                       = Compat_Recipes_IndustrialCraft GT_BaseCrop 59 + Ferru/Aurelia
prep_owned_paths             = tools/waves/prep/gt6-crop-food-split/**
landing_owned_paths          = src/addons/crops/**；src/addons/foods/**；
                               MaterialCatalog.addStartupForm；Core plant-form gate 清理
```

## 0. 开场判断

GT6 作物卡是 IC2 CropCard 注册，不是 `OP.plantGtBerry` 前缀族。Core 已有
Glowtus / bush 世界生成，没有杂交 runtime。Squeezer dump 的 5215 条
`plant_gt_*` ignore 行是配方规模，不是 5215 个作物。

## 1. 分母

| 集合 | 数 | 用法 |
| --- | --- | --- |
| `GT_BaseCrop` | 59 | Crop addon 卡 |
| Ferru / Aurelia | 2 | IC2 掉落改写，不是新卡类 |
| food 属性卡 | 35 | 缺 Foods 则不注册 |
| named plant-form overlay | 13 | 有界 `(material, plant_gt_*)` |
| ignored squeezer `plant_gt_*` | 5215 | 排除出作物分母 |

## 2. 明确不接管

- unique-active 落地锁（作物拆分保持 prep）
- 公共 16 增减、长尾一人一 id、管/缆切片 C
- Juicer / Fermenter dump 全量
- ItemEntity 世界生成当获得
- stand-in 配料或把 vanilla 甜浆果改成 GT 浆果替换原版生成

## 3. 关闭清单

- [x] source-backed ledger：61 卡、35 food、13 overlay、5215 排除
- [x] Core `addStartupForm` + gate / leftover unique plant 清理
- [x] Crop addon runtime、作物架、named overlay
- [x] Foods addon 物品、FoodStat、`c:` / Farmer's Delight 可选标签
- [ ] 后继 unique-active 才关 runtime_ready；本 prep 不建 capability
