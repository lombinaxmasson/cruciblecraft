# GT6 铸造美术详细计划

> 计划 slug：`content/gt6-foundry-art`
> 本文件位于 `card-plans/active/`。
> 正式名称：GT6 铸造美术
> 性质：从本地 `gregtech6_w` 把材质坩埚 / 模具 / 盆 / 交叉的体素模型与共享 iconset 接到已 live 的 85 个 BlockItem 上。
>
> 美术源：`gt6_referencable_port_code/gregtech6_w`。
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。
> 不重开 `content/gt6-mte-crucible-foundry-runtime` 的 `required_test_ids`。
> 不重开身份卡。不做 `fillMold` tick。

```text
lane                         = active
capability_slug              = content/gt6-foundry-art
unique_active_wave           = content/gt6-foundry-art
depends_on                   = content/gt6-mte-crucible-foundry-runtime, content/gt6-crucible-mold-interaction
close_target                 = runtime_ready
```

关闭目标不是 `player_complete`。不写新获得格，不撒 catalog `ItemEntity`。

---

## 0. 边界

只动 85 个 `CRUCIBLE_FOUNDRY` 的模型 / 贴图 / 染色 / 碰撞外形：

- 四个 GT6 空态体素父模型：熔炼坩埚、模具、盆、交叉
- 共享 `materialicons/*/blocksolid`：金属复用已迁的龙头 metallic PNG；石头单独拷一张
- 材质色 `tintindex` 0；GT6 overlay 为空，省略

**不在本卡：**

- dummy 罐 tick / `ITileEntityMold` / `fillMold`
- 陶瓷小坩埚 / 陶瓷模具（已有体素）
- 大坩埚 3×3 `machines/multiblockmains/crucible/**`
- 每材质一张 PNG、`*_real` 身份、配方、获得格

---

## 1. GameTest 合同

隔离命名空间 `cruciblecraft_wave_content_gt6_foundry_art`。

| 测试 id | 必须看见 |
| --- | --- |
| `foundryArtManifestResolvesLocalGt6` | manifest 指向本地 `gregtech6_w`，石头 PNG 在 classpath |
| `foundryArtNotLargeCrucibleCube` | 殷钢熔炼坩埚 / 石模具模型不是 `cube_all` 大坩埚侧面 |
| `foundryArtSharedParentsNotPerMaterialPng` | 钢模具与石模具共用模具父模型；没有 `mold_steel.png` |

上一张铸造 runtime 的 `foundryCrucibleIsNotCeramicCrucible` 仍必须绿。

---

## 2. 余量（本卡不做）

1. 材质熔炼坩埚 / 模具 / 盆 / 交叉仍是 dummy 罐。
2. 动态熔体液面（陶瓷坩埚 BER）不接到这 85 个方块。

---

## 3. 验收

- [x] 上表 3 个 GameTest
- [x] `python tools/build_gt6_foundry_art.py --check`
- [ ] 关闭目标 `runtime_ready`
