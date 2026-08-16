# T10 阻塞根因：`apply_t8_pipe_metadata` 缺 `newline="\n"`（Windows-only）

分析基线：`tools/src 2026-08-13 02:54`。结论已数值证明，非推测。

---

## 1. 一句话

`tools/apply_t8_pipe_metadata.py:170` 的 `path.write_text(content, encoding="utf-8")`
**没有传 `newline="\n"`**。在 Windows 上 Python 文本模式按 `os.linesep` 翻译，
于是它把**全部 1,774 个材料文件写成 CRLF**；`build_t10_preflight_projection`（#17）
用 `read_bytes()` 算 `material_tree_sha256`，记下的正是这个 CRLF 状态；
`apply_t10_form_flags`（#19，**有** `newline="\n"`）随后把它们全部写回 LF。

## 2. 数值证明

```
materials 目录全部 LF   -> 482eca8464f1f56f0c5d870cd55058ac92b6c714de374c658314faad68a52b48
materials 目录全部 CRLF -> 69b10dacd998d7a9b748468236bd4b3cdf28300198a54d2ecae95a760592dacb

t10_preflight_projection.json 提交值 = 69b10dac…   ← 全 CRLF
磁盘重算值                      = 482eca84…   ← 全 LF
```

**逐位相等。** 提交的 T10 哈希描述的是一个只在 Windows 上、
只在 #14 与 #19 之间存在的瞬时目录状态。

## 3. 为什么二分查不到

三层遮蔽同时生效：

1. **所有 `--check` 都瞎。** `apply_t8` / `apply_t10` / import 的比较全部走
   `Path.read_text()`，通用换行在**读取时**把 CRLF 折成 LF。写 CRLF 的脚本
   自己 check 自己，永远 `CURRENT`。
2. **只有字节级消费者看得见。** `material_tree_sha256` 用 `read_bytes()`。
   全仓 66 个文件使用字节级哈希，这次是 T10 先撞上。
3. **净变化为零，定点检测被骗。** 一轮内 LF → CRLF → LF，
   轮首 == 轮尾，churn = 0，上一版补丁据此判定"已收敛"，
   而 #17 记下的是中间值。

外加：`git status` 干净（最终态是 LF），所以从版本控制侧也看不出来。

`apply_t8_pipe_metadata --write` 还有一个放大因素：它**无条件重写全部 1,774 个
文档**，不是只写 `stale` 的那些——所以整棵树一次性变成 CRLF。

## 4. 时序表（Windows sweep）

| 步 | builder | materials 状态 | tree hash |
|---:|---|---|---|
| #0 | `import_gt6_oredict --write`（有 `newline="\n"`） | LF | `482eca84` |
| #14 | `apply_t8_pipe_metadata --write`（**缺**） | **CRLF** | **`69b10dac`** |
| #17 | `build_t10_preflight_projection` | 读字节 → **记下 `69b10dac`** | |
| #19 | `apply_t10_form_flags --write`（有） | LF | `482eca84` |
| 末 | | LF | `482eca84` |

## 5. 修复（一行）

```diff
--- a/tools/apply_t8_pipe_metadata.py
+++ b/tools/apply_t8_pipe_metadata.py
@@ -168,7 +168,7 @@
     if args.write:
         for path, content in documents.items():
-            path.write_text(content, encoding="utf-8")
+            path.write_text(content, encoding="utf-8", newline="\n")
```

沙箱以 Windows 等价写入验证：修复前 `0 file(s) changed, 1773 oscillating`；
修复后 2 轮到达定点，`0 oscillating`，T10 记录 `482eca84`，
`build_t10_preflight_projection --check` → **current**。

## 6. 同类缺陷面（需一并处理）

AST 扫描 `tools/*.py`：**42 处 `write_text` 缺 `newline=`，分布在 22 个文件**。
70 处是对的。前几名：

| 处数 | 文件 |
|---:|---|
| 9 | `compare_gt6_recipes.py` |
| 7 | `gt6_l1b_layer_select.py` |
| 4 | `run_material_registry_stress.py` |
| 3 | `gt6_recipe_templates.py` |
| 2 | `build_t13_cover_multiblock_denominators.py` |
| 1 | `apply_t8_pipe_metadata.py` ← 本次触发 |
| 1 × 16 | `build_gt6_generation_bits` / `build_gt6_ore_chain_closure` / `build_machine_crafting_readiness` / `build_t4_tool_readiness` / `build_t5_chemical_readiness` / `build_t13_prefix_domain_denominators` / `build_t19_*`(4) / `import_gt6_oredict` / `gt6_config_overlay` / `gt6_extruder_templates` / `gt6_l1a_closure_check` / `gt6_tag_domain_analysis` / `build_source_mod_inventory` |

其中多数当前无害（消费方用 `read_text` 或 `json.loads`），但只要哪天有人
给它加一个字节级哈希消费者，就会复现同一类事故。建议全部补齐，
而不是只修 `apply_t8`。

## 7. 交付物

- `apply_t8_pipe_metadata.patch` —— 一行修复。
- `check_text_write_newline.py` —— AST 扫描器，缺 `newline=` 即硬失败，
  带 `ALLOWLIST`（须写理由）。建议进 `run_python_tests --suite closure`。
- `rebuild_artifacts.py` —— 在上一版定点迭代基础上增加**逐步探针**：
  每个 builder 之后采样一次 `src/main/resources/data/cruciblecraft` 与 `tools`，
  报告"写入后又被写回"的文件。净变化为零也照样硬失败，并直接点名
  `apply_t8_pipe_metadata -> apply_t10_form_flags` 这类配对。

## 8. 建议动作顺序

1. 打上一行修复。
2. `python tools/rebuild_artifacts.py --only apply_t8,build_t10_preflight,apply_t10`
   —— 应看到 `0 oscillating`，T10 落到 `482eca84`。
3. 跑 `python tools/check_text_write_newline.py`，把其余 41 处补齐
   （纯机械改动，无语义风险）。
4. 全量 `python tools/rebuild_artifacts.py --keep-going`，确认
   `0 oscillating` 且到达定点。
5. 再跑 `run_full_verification.py --record`。

## 9. 顺带确认

修复后 T10 记录的 `482eca84` 与 08-08 存档的 `4f4a32fc` 不同，属正常：
两次快照之间材料内容确有变化（T21/T22 的 `form_items` 等）。
08-08 那份是 MATCH 状态，说明该快照是在 Linux 或未跑 `apply_t8 --write`
的情况下产生的——这也解释了为什么这个 bug 直到现在才浮出来。
