# 导线电缆 MTE 折回详细计划
> 计划 slug：`content/electric-wire-cable-mte-fold`
> 状态：accepted / runtime_ready（2026-09-10）。本文件位于
> `card-plans/closed/`。
> 正式名称：导线电缆 MTE 折回
> 性质：把 GT6 `addElectricWires` 的线/缆 MTE id 折到已经注册的 CC
> conductor。不实现龙头，不注册新材料，不把 `gt_mte` 散落物当配方对象。
> 机器可读 `unique_active_wave = null`（已闭卡）。
> 关闭目标：`runtime_ready`，不是 `player_complete`。
> 读法修订（2026-09-12）：只折了配方操作数；散落身份仍须逐 meta
> 区分 live BlockItem 与普通 item。
>
> Java/tick 源：`gt6_code/gregtech6` @ `3703e40308c8c030763fd6297dea8b210d2a77b1`。

```text
lane                         = accepted
capability_slug              = content/electric-wire-cable-mte-fold
unique_active_wave           = null
prep_owned_paths             = tools/electric_wire_cable_mte_fold.py
                               tools/build_electric_wire_cable_mte_fold.py
                               tools/waves/content/electric-wire-cable-mte-fold/**
landing_owned_paths           = tools/recipe_bulk/ordinary_source.py
                               tools/waves/machines/laminator/**
                               tools/waves/machines/loom/**
                               tools/waves/machines/melter/**
                               tools/waves/machines/nanofab/**
                               src/recipe_generated/resources/data/cruciblecraft/recipe/laminator/**
                               src/recipe_generated/resources/data/cruciblecraft/recipe/loom/**
                               src/recipe_generated/resources/data/cruciblecraft/recipe/melter/**
                               src/recipe_generated/resources/data/cruciblecraft/recipe/nanofab/**
landing_depends_on           = MTE 身份分母处置 R0 已关；unique-active 空窗
```

## 读法修订（2026-09-12）

本卡关闭时只折了配方操作数，明确不折 `identity_only` 散落物。那是当时分母，
不是产品许可「配方走活线、JEI 再留一根铁锭 dummy」。但 mapped 259 也不是
dummy 删除清单：它含 catalog 外 id，且 `double_wire` 等可能只有普通材料
item、没有可放置 BlockItem。连接件 runtime 必须按
[MTE 全量 Prep 总索引 §0.1](../prep/MTE全量Prep总索引.md) 逐 meta 判定：
同一 live BlockItem 才撤 dummy；只有 item 时解决 canonical item 的 BlockItem
升级；未注册规格继续在 dummy id 上实现并迁 `gregtech6_w`。折回行不迁第二套图。

---

## 0. 决策

MTE R0 分类的是 catalog 1,817 个散落物。层压机 / 织机 / 纳米加工机 overflow
里的 `gt.multitileentity` 几乎全是 **catalog 没收进来的电缆档**
（`aID+16` 起的 `cableGt01/02/04/08/12`）。

CC 已经有对应 conductor（`tin/cable`、`osmium_elemental/octuple_cable` 等）。
本卡只把 Loader id 折到 **census 已注册** 的同一对象。缺规格、超导、未注册
石墨烯高规格继续 `blocked`。禁止用别的线规、盖板或 `programmed_circuit` 顶。

---

## 1. 分母

- 30 次 `addElectricWires`（含 Graphene / Superconductor）
- 线规映射：`wireGt01/02/04/08/12/16` → `wire` / `double_wire` /
  `quadruple_wire` / `octuple_wire` / `dodecuple_wire` / `hexadecuple_wire`
- 电缆映射（`aCable=true`）：`cableGt01/02/04/08/12` → `cable` /
  `double_cable` / `quadruple_cable` / `octuple_cable` / `dodecuple_cable`
- 只写 `runtime_registry_gate` 已有的 item/block id
- 3/5/6/7/9/10/11/13/14/15 线规没有 CC 前缀，保持 unmapped

---

## 2. 明确不接管

- 33 个流体附件行为
- 红石线 27000/27050/27500
- 流体管 / 物品管
- 轴 / 齿轮箱
- 新材料或新前缀
- 印刷机染料流体、传送带模块、活塞、电机
- 把 `identity_only` 散落物折进配方

---

## 3. 验收

- `28050` → `cruciblecraft:tin/wire`，`28066` → `cruciblecraft:tin/cable`
- overlay 不含 `gt_mte`
- 回编层压机 / 织机 / 纳米加工机，overflow 只剩真正缺注册的行
  （层压机 226/272 → 438/60；织机 453/881 → 465/869；纳米加工机 7/57 不变）
- 熔融机 selected 里的电缆 MTE 从 `gt_mte` 散落物折到已注册 conductor
- 关闭时 `unique_active_wave = null`，maturity 仍是 `runtime_ready`
- 61 个 Loader id 保持 unmapped：未注册规格/材料（HSLA、BlueAlloy、
  ElectrotineAlloy、Nq、YttriumBariumCuprate、Graphene 高规格）以及
  Superconductor `29950+`。不得用别的线规顶。
