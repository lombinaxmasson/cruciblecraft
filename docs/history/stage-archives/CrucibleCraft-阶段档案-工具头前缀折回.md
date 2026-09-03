# CrucibleCraft 阶段档案 · 工具头前缀折回

> 状态：`TOOL_HEAD_PREFIX_READY`（2026-09-02）
> 计划 slug：`registry/tool-head-prefix`
> 固定 source revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> Opening：`COMPACT_RECIPE_AUTHORED_MATRIX_READY`；核能 Track C `started = false`
> Closing：bath ∪ semantic 的 `gt_tool_head/*` unique item 折回
> `材料 × 前缀`；一份 remap；mapped `7990` / remainder `0` / 前缀 `36`；
> bath identity `71`；semantic `244`；T48 **145 / 34091** 不变；
> `#0025` 仍 `matrix_v1` 1116 行；B 项留下；`nuclear_started = false`；
> `unique_active_wave = null`；`next_unassigned = true`；
> `production_lock = null`；`leftover_later_count = 39`

## 关闭结果

- 零 GT family 内容卡。`owns_families = 0`。`completion_delta = 0`。
  未签新 production lock，未开放 tag，未拆 Holder，未把 `#0025`
  收成 `MaterialRule`。不保留旧 `gt_tool_head/*` 别名。
- 注册：36 个 `tool_head_*` 前缀走 `MaterialItem`；创造栏 MAIN 不再
  倾倒 `kind=tool_head` identity。每种新前缀一张 SOURCE_BACKED
  GT6 METALLIC 底板，禁止 `minecraft:item/iron_ingot`。
- emit：`runtime_id_for` / ledger / catalogs / compile 硬门禁止再写
  mapped unique tool head。live bath/identity 与七个 ordinary-closure
  generated 正文已换前缀 id。
- B：钥匙 9、电路线 5、Low Heat Extruder Shape 32 对不上
  `material_id_to_cc` 或已有 catalog，identity 留下。画布 0；铠甲 12
  未折。
- reseal：T48 与
  `smelter|mixer|drying|electrolyzer|centrifuge|autoclave|compressor/ordinary-closure`。
  `repair_wave=registry/tool-head-prefix`。不改 family / relation 计数。
  `archive/sealed/T48` 不回写。
- 账本内容下一张仍是 Item Network Core，未签发。不预分配物流网 /
  核电 child。
- READY **只**证明：mapped tool head 不再是 unique item；配方换 id
  后 family 合同没改。不证明钥匙 / 电路线 / 挤出模折回，也不证明
  Item Network 已开工。

```text
TOOL_HEAD_PREFIX_READY
```

`partial_family_count=0`。不签发里程碑编号。不把它写成配方内容完成。

## 权威 artifacts

- `tools/tool_head_prefix.py`
- `tools/build_tool_head_prefix_remap.py`
- `tools/tool_head_prefix_remap.json`
- `src/main/resources/data/cruciblecraft/tool_head_prefix_remap.json`
- `src/main/resources/data/cruciblecraft/material_prefixes/tool_head_*.json`
- `src/main/resources/assets/cruciblecraft/textures/item/material/tool_head_*.png`
- `src/main/resources/data/cruciblecraft/bath_identity_catalog.json`（71）
- `src/main/resources/data/cruciblecraft/semantic_object_catalog.json`（244）
- `tools/tests/test_tool_head_prefix.py`
- `python tools/verify.py integration --profile materials`
- `python tools/verify.py integration --profile recipes --report-all`
- `python tools/verify.py dev --path docs/`

无新 wave JSON。不跑 `card-closeout` / census。
