# T23 导致 `run_full_verification.py --record` 反复失败的修复方案

> 结论基于对上传的 `src` / `tools` 快照的实测（重跑 77 个构建器、端到端模拟
> `record_ready()`、模拟下一轮 record）。三处改动互相依赖，建议一次性全部应用。

---

## 问题概述

`build_t23_readiness.py` 的 `runtime` 取自**已提交的 `full_verification_report.json`**，
而这个字段同时被写进 `check()` 的过期比对里。结果是 T23 与报告互相咬死：

```
report.required_tests = 89
        │
        ├─► build_t23_readiness._load_runtime() → gametest_total = 89
        │        └─► 闸门 `> 89` 不成立 → status 缺失 → T23 永远不 READY
        │
        └─► 一旦 --record 成功写入 114
                 └─► 重建结果 ≠ 磁盘上的 t23_readiness.json
                          └─► 下一轮 builder 阶段 #76 报 "stale generated file"
```

对照 `build_t22_readiness.py`：它的 `_load_runtime()` 读**自己的产物**
（`_load(OUTPUT)`），且闸门只判 `gametest_passing is True`、不比数量，
所以不存在这个循环。T23 的注释写着 "See the T22 pattern"，但实现没有跟上。

---

## 补丁 1 — 去掉写死的阈值（`tools/build_t23_readiness.py`，约 326 行）

当前 `gametest_total` 恰好等于 89，`> 89` 永远为假。

```diff
         and lifecycle_complete
         and port_supply_not_per_block
         and runtime["gametest_passing"] is True
-        and (runtime["gametest_total"] or 0) > 89
+        and (runtime["gametest_total"] or 0) > 0
     )
```

阈值本身没有意义：数量正确性已由 `verify_full_verification_report.py` 的
`record_gametest()`（要求日志里出现 `All <current_game_test_count()> required
tests passed`）和 READY 分支的
`game.required_tests != current_game_test_count()` 两处把关。这里只需要确认
"跑过且通过"。

---

## 补丁 2 — 让报告绑定字段退出过期比对（`tools/build_t23_readiness.py`，`check()`）

`status_owner` 已经声明这些字段归 `run_full_verification` 所有，
`check()` 却仍在比它们，这是 ping-pong 的直接来源。

```diff
+# run_full_verification 拥有的字段：由报告推导，不参与产物过期比对
+REPORT_OWNED = ("currentness", "runtime", "status",
+                "completed_stages", "pending_stages")
+
+
 def check() -> list[str]:
```

```diff
     disk_stripped = {
-        k: v for k, v in on_disk.items() if k != "currentness"
+        k: v for k, v in on_disk.items() if k not in REPORT_OWNED
     }
     expected_stripped = {
-        k: v for k, v in expected.items() if k != "currentness"
+        k: v for k, v in expected.items() if k not in REPORT_OWNED
     }
```

同一份剥离规则必须同步到
`verify_full_verification_report.py` 的 `derived_t23_readiness_acceptance()`，
否则 `readiness_current` 仍会在下一轮变成 `False`：

```diff
     document = builder.build()
     committed = json.loads(
         builder.OUTPUT.read_text(encoding="utf-8")
     ) if builder.OUTPUT.is_file() else {}
+    strip = lambda d: {k: v for k, v in d.items()
+                       if k not in builder.REPORT_OWNED}
     return {
         "status": document.get("status"),
-        "readiness_current": committed == document,
+        "readiness_current": strip(committed) == strip(document),
```

---

## 补丁 3 — 恢复被注释掉的状态断言（`verify_full_verification_report.py`，3016 行起）

补丁 1、2 落地后，这三行不再需要"放宽"，且不恢复的话 T23 闸门对 status
形同虚设。

```diff
     if not errors and (
-        # expected_t23_acceptance["status"] != "T23_READY"
-        # or expected_t23_acceptance["completed_stages"]
-        # != ["T23a", "T23b", "T23c", "T23d"]
-        # or expected_t23_acceptance["pending_stages"]
-        not expected_t23_acceptance["readiness_current"]
+        expected_t23_acceptance["status"] != "T23_READY"
+        or expected_t23_acceptance["completed_stages"]
+        != ["T23a", "T23b", "T23c", "T23d"]
+        or expected_t23_acceptance["pending_stages"]
+        or not expected_t23_acceptance["readiness_current"]
         or t23_closure["behavior_unclassified"] != 0
```

同时把上方那段 "second --record run derives T23_READY" 的 NOTE 删掉——
它描述的自愈行为在补丁 1 存在时不成立。

---

## 应用后的一次性操作

```bash
# 1) 重建 T23 产物，让 status 真正落盘
python tools/build_t23_readiness.py
python -c "import json;print(json.load(open('tools/t23_readiness.json'))['status'])"
#   期望输出: T23_READY

# 2) 确认 builder 阶段 76/77 恢复
python tools/build_t23_readiness.py --check

# 3) 正式录制
python tools/run_full_verification.py --record
```

---

## 顺带需要确认的两件事

**a) `plans/` 路径不一致。** 仓库里只有 `build_t22_5_readiness.py:41` 和
`t22_5_readiness_policy.json:19` 把第四阶段总体规划文档放在
`plans/CrucibleCraft-第四阶段总体规划.md`；`build_t20_readiness.py:57`、
`python_test_policy.json`、`tooling_paths()` 与报告的 `artifact_sha256`
全部用仓库根目录。一旦文档被归位或移动，`_load_if_exists` 的兜底会静默返回
`{"wording_updated": False}`，T22.5 立刻退化成"无 status → 产物过期 →
builder #74 失败"——和 T23 完全同一类故障。建议统一到一个位置，并把兜底
改成显式报错而不是静默返回 False。

**b) `*_result` 键是恒真断言。** `record_builder()` 把
`t4_readiness_result` … `t23_readiness_result` 共 23 个键全部硬编码为
`"PASS"`，READY 分支再逐个断言它们等于 `"PASS"`。这 21 处检查永远不可能失败。
另外 `t8_readiness_result` 与 `worldgen_catalog_result` 写入后从未被检查，
而 T22 / T22.5 根本没有对应的键。
