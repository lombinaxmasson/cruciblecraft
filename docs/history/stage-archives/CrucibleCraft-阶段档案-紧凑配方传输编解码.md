# CrucibleCraft 阶段档案 · 紧凑配方传输编解码

> 状态：`COMPACT_RECIPE_WIRE_CODEC_READY`（2026-09-02）
> 计划 slug：`runtime/compact-recipe-wire-codec`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`VANILLA_REPLACE_MVP_READY`；核能 Track C `started = false`
> Closing：compact family 网络 `StreamCodec` 从 NBT
> `fromCodecWithRegistries` 换成有界字典二进制；5651 条 live entry
> encode ≤ 512 KiB；dedicated 进世界不再撞 `NbtAccounter`；整包
> Varint21 由 GenericPacketSplitter 2 MiB 门切开；T48 seal 未改；
> `nuclear_started = false`；`unique_active_wave = null`；
> `next_unassigned = true`；`production_lock = null`；
> `leftover_later_count = 39`

## 关闭结果

- 零 GT family 内容卡。`owns_families = 0`。`completion_delta = 0`。
  `generated_recipe_count = 0`。未签 production lock，未改
  `src/recipe_generated/**`，未走 §3.2 family fragment。
- 网络路径：`CompactGTRecipeFamilyStreamCodec`（version 1，字典
  intern，count 先读再分配）。datapack `MapCodec` 保持可读。
- 产品门：`MAX_RECIPE_ENTRY_WIRE_BYTES = 524288`；
  `DECODE_RELATIONS_CEILING = 4096`；每 relation IO ≤ 16。
- 整包：换 NBT 后 dedicated 仍会在 ~4.09 MiB 上撞
  `Varint21LengthFieldPrepender`。Mixin 把 NeoForge splitter 的
  uncompressed 8 MiB 判断收到 2 MiB compressed 帧上限，让
  `update_recipes` 切开而不是抬协议上限。
- `payloadBytes` 仍是 stub（现名 `estimateStubPayloadBytes`），不是
  per-holder 证明。证明是 JUnit `streamCodec().encode()`。
- 账本内容下一张仍是 Item Network Core，未签发。不预分配物流网 /
  核电 child。
- READY **只**证明：独立客户端进得去；family 合同没改。不证明改成
  GTM「一条小配方 + tag 输入」，也不证明整包不再依赖 splitter。

### 与 GTM 差在哪

物品已经和 GTM 一边：`TagPrefix × Material` 各一个 Item，栈上通常
不挂材料 NBT，注册表膨胀的税付过了。配方也进原版 RecipeManager，
`matches()` 永远 false，加载后再填自己的 RecipeDB——和 dedicated 从
manager 重建是同一合同。

差在一个 Holder 里装什么，以及因此线上一次 encode 有多大：

| | GTM | 本卡改前 | 本卡现在 |
| --- | --- | --- | --- |
| Holder | 一条小机器配方 | 整个 semantic family | 仍是整个 family |
| 输入 | 优先 tag（`ingots/copper`） | exact item id | 仍是 exact |
| 线上 | 手写二进制 `toNetwork` | 整表 NBT | family 内字典二进制 |
| 会炸的门 | 几乎不碰单 compound 2 MiB | `#0025` 一次 `readNbt` | 单条过了；整包 ~4 MiB 撞 Varint21，靠 splitter |

GTM 成千上万条进 `update_recipes` 也没问题：每条只有几个 Ingredient /
流体 / duration。NeoForge 拆的是整包，`NbtAccounter` 卡的是一次
`readNbt()`。改前是「一个大 family + NBT」，一条 `#0025` 就把
compound 撑过 2 MiB。现在单条不再是 NBT、也压进 512 KiB，但 5651
个 family 加其他 eager 配方仍约 4 MiB；下一扇门是 3 字节长度前缀。

第二刀（GTM 粒度或 §3.2 fragment）不在本卡。打开 parameterized / tag
输入会动 exact 合同和 T48 分母。传输再切碎且仍带同一个 `family_id`
本卡测过单条已经够小，没走。

```text
COMPACT_RECIPE_WIRE_CODEC_READY
```

`partial_family_count=0`。不签发里程碑编号。不把它写成配方内容完成，
也不把它写成 GTM 对齐完成。

## 权威 artifacts

- `src/main/java/com/masson/cruciblecraft/recipe/gt/CompactRecipeWireLimits.java`
- `src/main/java/com/masson/cruciblecraft/recipe/gt/CompactGTRecipeFamilyStreamCodec.java`
- `src/main/java/com/masson/cruciblecraft/recipe/gt/CompactRecipeWireValues.java`
- `src/main/java/com/masson/cruciblecraft/mixin/GenericPacketSplitterSizeLimitsMixin.java`
- `src/test/java/com/masson/cruciblecraft/recipe/gt/CompactGTRecipeFamilySyncSizeTest.java`
- `.\gradlew.bat test --tests com.masson.cruciblecraft.recipe.gt.CompactGTRecipeFamilySyncSizeTest`
  （全量 recipes profile Gradle 为 817 tests / 20 scope-external failures /
  本卡 7/7 通过）
- `.\gradlew.bat runServer -PwireCodecSmoke=dedicated` 与
  `.\gradlew.bat runClient -PwireCodecSmoke=dedicated`

不重写已 sealed 的 T48。无 T38 式 GameTest receipt。无 wave JSON。
