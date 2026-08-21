# CrucibleCraft 阶段档案 · T31

> 阶段：T31 · pre-RC 封板 / `0.1.0-rc.1`
> 状态：● 已关闭（`T31_READY`，2026-08-21 `--check-ready`）
> 固定 GT6 revision：`3703e40308c8c030763fd6297dea8b210d2a77b1`
> 权威产物：`tools/t31_readiness.json` · G0：`tools/t31_g0.json` · 规模：`tools/t31_scale_recheck.json` · 兼容：`tools/t31_compatibility_report.json` · 载荷：`tools/t31_load_report.json` · 发行清单：`tools/t31_release_manifest.json`

## 1. 关闭判据

T31 在 T30 hopper-complete 树上封板，把版本改为 `0.1.0-rc.1`，并在 ≥16 GiB 物理内存上实测关闭 T24-F003 / T24-F005。本卡 **零内容增量**。完成 T31 **不等于** GA，也不等于 `1.0.0`。

T24 历史 SKIP 仍在 `tools/t24_scale_evidence.json`；当前测量在 `tools/t31_scale_recheck.json`。

## 2. 三轴

**Closure**

- `T30_READY` 仍当前；T31 content delta 0；work set 0；
- G0 `T31_G0_PASSED`；release blockers 0；
- F003 / F005 当前状态 `CLOSED`（target 500 管、stress 2000 管，各 12,000 sampling ticks）；
- GameTest **137/137**；JUnit **652**；Python **826**；
- 发行 jar/zip 可安装；embedded version `0.1.0-rc.1`；无 datagen `.cache`。

**Fidelity**

- Handshake `NETWORK_VERSION` 仍为 `"1"`；Hopper 菜单不伪装协议错误；
- T24 历史 SKIP 未改写成 measured；
- source revision 钉死 `3703e40308c8c030763fd6297dea8b210d2a77b1`。

**Load**

GT publication delta **0/0/0**（opening 18766 / 16541 / 2225）。eager 硬顶 21000、datapack 硬顶 6600 未破。target/stress 已实测，pending 轴未填 0。

## 3. 验证与交接

```text
PYTHONUTF8=1 python tools/run_full_verification.py --record --new-session --source-replay
PYTHONUTF8=1 python tools/run_full_verification.py --check-ready
.\gradlew.bat build distBeta
PYTHONUTF8=1 python tools/build_t31_release_manifest.py --check
```

**2026-08-21 绑定事实（`--check-ready` 退出 0）：**

| 项 | 状态 |
|---|---|
| 报告 | `status = READY`；绑定会话 `20260820T215754.605495Z-96adb37f577c-8cdce70b` |
| SHA-256 | `a01ae5eb4cba5a76af4ff37b9591979f3652cd33ed7698f728e8c50d3f48a8d5` |
| T31 | `t31_readiness_acceptance.status = T31_READY` |
| T30 | 仍 `T30_READY` |
| JUnit / GameTest / Python | **652** / **137/137** / **826** |
| publication | GT delta **0/0/0** |
| 版本 | `mod_version = 0.1.0-rc.1`；`rc_number = null` |
| jar | 7666238 B · `6800a13b33ab98db6898ef0e5fb42691be346255f3a60eca0f68b5f192ff5324` |
| zip | 5158724 B · `6f026e5280140ae4e8c8ffe81c7fa7ad4d9ba2c948de565bbbc72f62697d929a` |
| 兼容 | client/dedicated CC-only、save-reload、EMI/Jade 矩阵实测通过；KubeJS 轴按合同记账 |

**T31 之后（soak / GA 另开卡）：**

1. **RC soak**：只接 crash、存档、复制、协议、主链、hard-load blocker；见 `T31-RC-soak.md`；
2. soak 通过后另开 GA 卡；禁止把 RC1 写成已发布 / GA / `1.0.0`；
3. 禁止 `--update-baseline` / `--write-tooling-snapshot`。

下一张工作 = **RC soak**。GA 不是本卡。
