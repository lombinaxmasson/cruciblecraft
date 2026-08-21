# CrucibleCraft 阶段档案 · T30

> 阶段：T30 · Hopper / Queue Hopper / steel Dust Funnel
> 状态：● 已关闭（`T30_READY`，2026-08-20 `--check-ready`）
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 权威产物：`tools/t30_readiness.json` · 取证：`tools/t30_hopper_source_evidence.json` · 载荷：`tools/t30_load_projection.json` · 账本：`tools/t30_publication_delta.json` · 合同：`tools/phase5_1_pre_rc_logistics_contract.json`

## 1. 关闭判据

T30 用固定 revision 把 GT6 Hopper 的 60 个 SOURCE_BACKED 材质行落到 CC：每个材质注册 `<material>_hopper` 与 `<material>_queue_hopper`（120），外加 `steel_dust_funnel`（合计 121 block/item）。flat catalog、共享 tinted 美术、vanilla 合成/loot/lang、JUnit 与 6 个生命周期 GameTest 一次闭合。完成 T30 **不等于** 启动 RC，也不等于 1.0。

T27–T29 正式结果里 “Hopper = post_1_0” 仍是历史快照，本卡不回写成“当时就计划了 T30”。

## 2. 三轴

**Closure**

- 60 source rows；120 hopper-family IDs；`steel_dust_funnel` 1；
- 无裸 `hopper` / `queue_hopper` 材质 ID；
- vanilla 合成 121；loot/blockstate/item model 各 121；共享模型，无 per-material 贴图；
- 6 个 Hopper-family GameTest 在源码中存在且全量套件 **137/137** 通过；
- Phase 5.1 合同 `total_new_blocks = 121`；replacement 已满足。

**Fidelity**

- Hopper 行 SOURCE_BACKED；槽位数来自 GT6 取证；
- Hopper 合成省略 tool catalyst / `plateCurved`，记为 SOURCE_DERIVED；
- Dust 运行时只含 `dust` / `small_dust` / `tiny_dust`；`blockDust` / `dustDiv72` / `plateCurved` 不进 runtime；
- 美术 ART_DERIVED；`rc_number` 仍 null。

**Load**

| 轴 | T30 opening（T29 live） | T30 delta | measured |
|---|---:|---:|---:|
| logical | 18,766 | 0 | 0 |
| eager | 16,541 | 0 | 0 |
| lazy | 2,225 | 0 | 0 |

vanilla 121 是非 GT 轴，已记账。eager 仍低于 21,000。T27 opening 冻结 **19087 / 16862 / 2225** 未改。

## 3. 验证与交接

```text
PYTHONUTF8=1 python tools/run_full_verification.py --record --new-session --source-replay
PYTHONUTF8=1 python tools/run_full_verification.py --check-ready
```

**2026-08-20 绑定事实（`--check-ready` 退出 0）：**

| 项 | 状态 |
|---|---|
| 报告 | `status = READY`；滞后会话 `20260820T084919.174882Z-df269c2f538b-5bd24a80`；绑定会话 `20260820T092034.350228Z-df269c2f538b-08cee179` |
| SHA-256 | `fa3e32483f42a1f106fcccbe27ba95f3022ee3aa332e103f265727e68f477fee` |
| T30 | `t30_readiness_acceptance.status = T30_READY` |
| T29 / T28 / T27 | 仍 `T29_READY` / `T28_READY` / `T27_READY`；历史 Hopper=`post_1_0` 未改 |
| JUnit / GameTest | **644** 全绿 / **137/137** |
| Python | **807**（optional raw/cache 仍显式 skip） |
| publication | opening **18766 / 16541 / 2225**；GT delta **0/0/0** |
| RC | `rc_number = null`；`mod_version = 0.1.0-beta.1`；F003/F005 仍留给 T31 |

**T30 之后（不得跳）：**

1. **T31**：封板 + T24-F003 / T24-F005 + 打包门；门禁通过后才把版本改为 `0.1.0-rc.1`；
2. barrels / mass storage / drawers / fluid funnel / canner 仍 post-1.0；
3. 禁止 `--update-baseline` / `--write-tooling-snapshot`。

下一张唯一 active T = **T31**。T31 开工前仍须 `--check-ready`。
