# CrucibleCraft 阶段档案 · 紧凑配方作者矩阵

> 状态：`COMPACT_RECIPE_AUTHORED_MATRIX_READY`（2026-09-02）
> 计划 slug：`runtime/compact-recipe-authored-matrix`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`COMPACT_RECIPE_WIRE_CODEC_READY`；核能 Track C `started = false`
> Closing：compact family 作者正文改为 `matrix_v1`（共享字段 + IO 字典
> + 允许元组）；Provider / policy / dedup 展开成同一批 exact 关系；
> StreamCodec v2 编矩阵而不是 1116 行；live 5651 文件数不变；JSON >1 MiB
> 从 39 降到 0；dedicated 进世界无 `NbtAccounter` / `Packet too large`；
> T48 seal 未改；语义波只重 seal 哈希；`nuclear_started = false`；
> `unique_active_wave = null`；`next_unassigned = true`；
> `production_lock = null`；`leftover_later_count = 39`

## 关闭结果

- 零 GT family 内容卡。`owns_families = 0`。`completion_delta = 0`。
  live compact 文件数仍 5651（1394 `matrix_v1`，4257 inline）。未签
  production lock，未走 §3.2 family fragment，未开放 tag。
- 作者信封：`authored_form = matrix_v1`；`relations` 缺省或 `[]`；
  XOR 禁止与非空 `relations` 并存。展开点是
  `CompactAuthoredMatrix.expand`，不是 MapCodec / Recipe 构造。
- 产品门沿用传输卡：`MAX_RECIPE_ENTRY_WIRE_BYTES = 524288`；
  `DECODE_RELATIONS_CEILING = 4096`；每 relation IO ≤ 16。
- `#0025`：JSON 254885 字节；Holder `relations().size()==0`；prepare
  仍 1116；wire `< 300 KiB`。
- 整包：dedicated `Dev joined the game`。uncompressed
  `update_recipes` 字节未再打印。splitter Mixin 仍在。不把未测量的
  整包写成新的 MiB 数，也不把 splitter 当完成条件。
- 被改写波次 reseal：`repair_wave=runtime/compact-recipe-authored-matrix`。
  计数与 lock family 集合不变。T48 / T47 归档 **不** 重 seal。
- 账本内容下一张仍是 Item Network Core，未签发。不预分配物流网 /
  核电 child。
- READY **只**证明：作者矩阵与展开等价；独立客户端进得去；family
  合同没改。不证明改成 GTM「一条小配方 + tag 输入」，也不证明整包
  不再依赖 splitter。

### 与 GTM 差在哪

| | GTM | 传输卡之后 | 本卡现在 |
| --- | --- | --- | --- |
| Holder | 一条小机器配方 | 整个 semantic family | 仍是整个 family |
| 输入 | 优先 tag | exact item id | 仍是 exact |
| 作者正文 | 短配方 | 整表 exact relations | shared-shape `N>=2` 为矩阵 |
| 线上 | 手写 `toNetwork` | family 内字典二进制（展开表） | 编矩阵正文 |
| 最终关系 | 小配方集合 | exact | **同一** exact |

```text
COMPACT_RECIPE_AUTHORED_MATRIX_READY
```

`partial_family_count=0`。不签发里程碑编号。不把它写成配方内容完成，
也不把它写成 GTM 对齐完成。

## 权威 artifacts

- `src/main/java/com/masson/cruciblecraft/recipe/gt/CompactGTRecipeFamilyDefinition.java`
  （`AuthoredMatrixV1`）
- `src/main/java/com/masson/cruciblecraft/recipe/gt/CompactAuthoredMatrix.java`
- `src/main/java/com/masson/cruciblecraft/recipe/gt/CompactGTRecipeFamilyStreamCodec.java`
- `tools/recipe_bulk/matrix.py`
- `tools/compact_authored_matrix_v1.schema.json`
- `src/test/java/com/masson/cruciblecraft/recipe/gt/CompactAuthoredMatrixTest.java`
- `src/test/java/com/masson/cruciblecraft/recipe/gt/CompactGTRecipeFamilySyncSizeTest.java`
- `.\gradlew.bat test --tests com.masson.cruciblecraft.recipe.gt.CompactGTRecipeFamilySyncSizeTest`
  （全量 recipes profile Gradle 为 826 tests / 20 scope-external failures /
  本卡 SyncSize 8/8、AuthoredMatrix 7/7 通过）
- `.\gradlew.bat runServer -PwireCodecSmoke=dedicated` 与
  `.\gradlew.bat runClient -PwireCodecSmoke=dedicated`

不重写已 sealed 的 T48。无 T38 式 GameTest receipt。无新 wave JSON。
