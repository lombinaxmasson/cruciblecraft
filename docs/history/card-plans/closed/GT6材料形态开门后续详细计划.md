# GT6 材料形态开门后续详细计划

> 计划 slug：`registry/gt6-form-open-followup`
> 状态：unique-active。
> 性质：只消费当前材料形态普查的 `openable` 集合；不重开已关闭长尾卡，
> 不处理 `gated_unresolved`、generation flag 全量或公共 16 名单。

```text
lane                         = active
capability_slug              = registry/gt6-form-open-followup
unique_active_wave           = registry/gt6-form-open-followup
landing_depends_on           = registry/material-form-demand-census
```

本卡最终只消费标准 gate 可表达的 `magic/small_casing` 1 对。fresh census
中的 metadata-only 容器形态没有被错误宣称为 live，已冻结到
`tools/waves/prep/gt6-container-chem-tube-forms/required_forms.json`，
等待独立 Java 容器形态注册卡。

验收：

- `build_gt6_material_form_gate.py --check`
- `census.py --check`
- `runGameTestServer -PrecipeCensus`
- `python tools/verify.py integration --profile capability-runtime`
- 形态抽样客户端检查名称、贴图、染色、EMI 和标签

缺失真零件仍保持 `blocked`；本卡不发配方 stand-in，不生成 ItemEntity 世界掉落。
