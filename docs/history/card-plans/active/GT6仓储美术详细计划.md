# GT6仓储美术详细计划

> 计划 slug：`content/gt6-storage-art`
> 状态：unique-active。
> 本文件位于 `card-plans/active/`。
> 正式名称：GT6仓储美术
> 性质：从本地 `gregtech6_w` 把储物桶 / 箱 / 大容量仓储 / 柜 / 抽屉 / 书架 / 瓶箱 / 投放器的
> kind 级 colored+overlay（及书架/瓶箱空态体素）接到已 live 的 T44 624+1 与
> MTE in-place 储物 BlockItem 上；正面数量字、架上书、瓶内流体用 BER。
>
> 美术源：`gt6_referencable_port_code/gregtech6_w`。
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 不重开 `content/gt6-mte-furniture-storage-runtime` / `content/gt6-mte-furniture-barrel-runtime`
> 的 `required_test_ids`。不改 R0。不把 T44 与 dummy in-place 双身份合并。

```text
lane                         = unique-active
capability_slug              = content/gt6-storage-art
unique_active_wave           = content/gt6-storage-art
depends_on                   = content/gt6-mte-furniture-storage-runtime, content/gt6-mte-furniture-barrel-runtime
close_target                 = runtime_ready
```

关闭目标不是 `player_complete`。不写新获得格，不撒 catalog `ItemEntity`。
禁止继续用原版橡木桶 / 书架 / 橡木板 / 铁块当仓储贴图。

---

## 0. 边界

只动仓储外观：

- GT6 `machines/massstorage/{barrel,box,standard,logistics,inserter}`
- `machines/drawers/quad`、`machines/lockers/{normal,charging}`
- 书架 / 瓶箱空态 ISBRH + `iconsets/planks_wood.png`；金属书架复用 metallic `blocksolid`
- `tintindex` 0 跟 GT6 `mRGBa`；overlay 不染色
- T44 `storage_variants.json` 624+1 按 `model_profile` 换模型
- MTE in-place `BARREL` / `BOOKSHELF` / `BOTTLE_CRATE` / `DRAWER` / `LOCKER` / `MASS_STORAGE`
  共用上述父模型，不再 `cube_all` 指向缺失 png
- 大容量仓储正面 GUI 物品 + 数量字 BER；书架书本与瓶箱流体/玻璃/盖 BER
- 客户端库存同步（`getUpdateTag` / `getUpdatePacket`），否则 BER 看不见内容

**不在本卡：**

- 世界生成
- 把 MTE dummy 桶改成 MassStorage 行为，或折回 T44 id
- 箱子 / 保险箱 / 工作台 / 脚手架（家具其它 family）
- 配方、获得格、`*_real` 身份

## 1. GameTest 合同

隔离命名空间 `cruciblecraft_wave_content_gt6_storage_art`。

| 测试 id | 必须看见 |
| --- | --- |
| `storageArtManifestResolvesLocalGt6` | manifest 指向本地 `gregtech6_w`，桶 overlay/colored 在 classpath |
| `storageArtBarrelNotVanillaOak` | T44 `mass_storage_barrel_6999` 模型不是原版 `minecraft:block/barrel` |
| `storageArtMteBarrelNotMissingCube` | `skyroot/item_barrel` 父模型不是 `cube_all` + 缺失 `mte/barrel` |
| `storageArtBookshelfShowsBookDisplay` | 书架插入书后显示 id 非空，书本 png 在 classpath；in-place 书架同样 |
| `storageArtBottleCrateShowsBottleFluid` | 瓶箱玻璃无流体、蜂蜜有色；瓶 png 在 classpath；in-place 瓶箱同样 |
| `storageArtMassStorageFrontCountSync` | 大容量仓储 `stored` 进 update tag，`12k` 格式；in-place MASS_STORAGE 1 槽；dummy 桶仍 27 槽 |

上一张家具储物 / 木桶 runtime 的 live-inventory 测试仍必须绿。

## 2. 余量（本卡不做）

1. T44 与 in-place dummy 双身份并存；本卡只让两边都看起来像 GT6。
2. 世界生成不在本卡。

## 3. 验收

- [ ] 上表 6 个 GameTest
- [ ] `python tools/build_gt6_storage_art.py --check`
- [ ] 关闭目标 `runtime_ready`
