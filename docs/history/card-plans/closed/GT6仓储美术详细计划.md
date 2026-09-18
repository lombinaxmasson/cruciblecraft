# GT6仓储美术详细计划

> 计划 slug：`content/gt6-storage-art`
> 状态：已关闭 `runtime_ready` / `workflow=accepted`。
> 本文件位于 `card-plans/closed/`。
> 正式名称：GT6仓储美术
> 性质：从本地 `gregtech6_w` 把储物桶 / 箱 / 大容量仓储 / 柜 / 抽屉 / 书架 / 瓶箱 / 投放器的
> kind 级 colored+overlay（及书架/瓶箱空态体素）接到已 live 的 T44 624+1 与
> MTE in-place 储物 BlockItem 上；正面数量字、架上书、瓶内流体用 BER。
>
> 美术源：`gt6_referencable_port_code/gregtech6_w`。
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 不重开 `content/gt6-mte-furniture-storage-runtime` / `content/gt6-mte-furniture-barrel-runtime`
> 的 `required_test_ids`。不改 R0。17 对 T44/MTE 精确 meta 双身份并到 T44 宿主。

```text
lane                         = closed
capability_slug              = content/gt6-storage-art
unique_active_wave           = null
maturity                     = runtime_ready
workflow                     = accepted
depends_on                   = content/gt6-mte-furniture-storage-runtime, content/gt6-mte-furniture-barrel-runtime
close_target                 = runtime_ready
```

关闭目标不是 `player_complete`。不写新获得格，不撒 catalog `ItemEntity`。
禁止继续用原版橡木桶 / 书架 / 橡木板 / 铁块当仓储贴图。

---

## 0. 边界

只动仓储外观与仓储玩家交互：

- GT6 `machines/massstorage/{barrel,box,standard,logistics,inserter}`
- `machines/drawers/quad`、`machines/lockers/{normal,charging}`
- 书架 / 瓶箱空态 ISBRH + `iconsets/planks_wood.png`；金属书架复用 metallic `blocksolid`
- `tintindex` 0 跟 GT6 `mRGBa`；overlay 不染色
- T44 `storage_variants.json` 624+1 按 `model_profile` 换模型
- MTE in-place `BARREL` / `BOOKSHELF` / `BOTTLE_CRATE` / `DRAWER` / `LOCKER` / `MASS_STORAGE`
  共用上述父模型，不再 `cube_all` 指向缺失 png
- 大容量仓储正面 GUI 物品 + 数量字 BER；书架书本与瓶箱流体/玻璃/盖 BER
- 客户端库存同步（`getUpdateTag` / `getUpdatePacket`），否则 BER 看不见内容

- 箱子 / 保险箱 kind 级 GT6 贴图（箱子 TESR，保险箱 overlay）
- 桶 / 塑料箱注册 id 从 `mass_storage_*` 改成 `item_barrel_*` / `plastic_storage_box_*`（旧 id 有 alias）
- 其余 49 金属 × 5 种 in-place 书架 / 瓶箱 / 抽屉 / 柜 / 大容量仓储是 live 储物：STORAGE 创造栏、正确容量、右键 GUI / 柜换甲 / 大容量正面点击。不是 hopper-only dummy。箱子 54 格、保险箱 15 格同样右键开 GUI。

**不在本卡：**

- 世界生成
- 工作台 / 脚手架（家具其它 family）
- `*_real` 身份
- 缺 `small_casing` 的瓶箱生存配方（不写替身配料）
- 金属集里 CC overlay 没有的 10 个 aID，以及 charging-locker / logistics-mass 扩金属

本卡当场并 17 对精确 meta 双身份：folded dummy 撤册，T44 为唯一 live BlockItem。
folded dummy 世界堆变成空气，无 NeoForge alias。

## 1. GameTest 合同

隔离命名空间 `cruciblecraft_wave_content_gt6_storage_art`。

| 测试 id | 必须看见 |
| --- | --- |
| `storageArtManifestResolvesLocalGt6` | manifest 指向本地 `gregtech6_w`，桶 overlay/colored 在 classpath |
| `storageArtBarrelNotVanillaOak` | T44 `item_barrel_6999` 模型不是原版 `minecraft:block/barrel` |
| `storageArtMteBarrelNotMissingCube` | dummy `skyroot/item_barrel` 已撤；T44 `item_barrel_6983` live；父模型不是 `cube_all` |
| `storageArtBookshelfShowsBookDisplay` | 书架插入书后显示 id 非空，书本 png 在 classpath；lead dummy 已撤 |
| `storageArtBottleCrateShowsBottleFluid` | 瓶箱玻璃无流体、蜂蜜有色；瓶 png 在 classpath；lead dummy 已撤 |
| `storageArtMassStorageFrontCountSync` | 大容量仓储 `stored` 进 update tag，`12k` 格式；lead / skyroot dummy 已撤 |
| `storageArtMetalBookshelfIsLive` | 铝书架 28 格、只收书、有 GUI |
| `storageArtMetalBottleCrateIsLive` | 铝瓶箱 9 格、只收瓶、有 GUI |
| `storageArtMetalDrawerIsQuad` | 铝抽屉 144 格、有四分格 GUI |
| `storageArtMetalLockerSwapsArmor` | 铝柜 4 格、正面换甲 |
| `storageArtMetalMassStorageIsLive` | 铝大容量 1e6、正面数量进 update tag |
| `storageArtChestIsFiftyFour` | 铅箱 54 格 GUI |
| `storageArtSafeIsFifteen` | 铅保险箱 15 格 GUI |

上一张家具储物 / 木桶 runtime 的 live-inventory 测试仍必须绿。

## 2. 余量

1. 世界生成不在本卡。
2. 瓶箱生存配方仍缺 `small_casing`，不写替身配方。

## 3. 验收

- [x] 上表 GameTest
- [x] `python tools/build_gt6_storage_art.py --check`
- [x] 关闭目标 `runtime_ready`
