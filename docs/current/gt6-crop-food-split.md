# GT6 作物与 Foods 可选拆分

Core 保留材料身份、Glowtus / sweet-berry 灌木世界生成，以及启动期
`MaterialCatalog.addStartupForm` overlay。作物杂交 runtime 和 `plant_gt_*`
live 对不在 Core 里。

## 边界

| 层 | 内容 |
| --- | --- |
| Core | 材料身份、共享长尾 Item、Glowtus / `gt_bush`、overlay API |
| `cruciblecraft_crops` | IC2 风格作物架、59 张 `GT_BaseCrop` + Ferru / Aurelia、13 个 named plant-form overlay |
| `cruciblecraft_foods` | 作物卡食物物品、`FoodStat`、`c:` / Farmer's Delight 可选标签 |
| 另开主机卡 | Juicer / Fermenter / 5215 条 squeezer ignore 行 |

附属必须声明 `ordering="BEFORE"` `cruciblecraft`，在 Core bootstrap 前调用
`addStartupForm`。长尾仍是共享 `PrefixMaterialItem`，禁止 `{material}/plant_gt_*`
双注册或把 plant 前缀收进公共 16。

## 分母

权威：`tools/waves/prep/gt6-crop-food-split/ledger.json`。

- 59 `GT_BaseCrop` + Ferru/Aurelia = 61 张卡
- 35 张 food 属性卡；缺 Foods 时不注册、不 stand-in
- 13 个 named `(material, plant_gt_*)` overlay
- 5215 条 ignored `plant_gt_*` squeezer 行**不是**作物分母

无附属时 Core gate 无 plant form live stack。有 Crop 附属时每个可 overlay 的 named drop
恰有一个共享后端。`oil/plant_gt_berry` 因 `oil` 为 metadata-only 保持 blocked，不 stand-in。
作物获得不走 ItemEntity 世界生成。

重建：`python tools/waves/prep/gt6-crop-food-split/crop_food_split.py --write|--check`。
