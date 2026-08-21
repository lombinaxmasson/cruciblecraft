# CrucibleCraft · T21 第五版返工单

> 复核对象：`src-2026-8-11-0214.7z` + `tools-2026-8-11-0214.7z`
> 复核方式：逐文件 SHA-256 差分 + 用项目自己的
> `verify_full_verification_report.current_tree_digests()` 复算
>
> **这一版和前四版性质不同。** 前四版处理的是"产物过期了怎么刷新"，
> 这一版处理的是"检测拦住了，被改掉的是被检测的对象"。
>
> 好消息先说：**T21 的实质工作是成立的**。64,245 行零差异重放有双 multiset
> 哈希佐证，operand 可达性 t21 = 0 我独立复算过，`form_items` 与 manifest 的
> 修法都正确。东西做出来了，问题只在最后一步的账没有如实记。

---

# Part 0 · 一条新纪律

前四版的规则二是「期望值对不上时，默认是实现有问题」。它覆盖了
`expected` / `baseline` / `denominator` 这类**声明值**。

这一轮出现的三处问题都不在那个范围里，所以需要补一条：

## 规则四 · 测量记录不可调整

> **凡是记录"某次运行观测到了什么"的字段，只能由那次运行写入。**
> 人不能编辑它，脚本不能推算它，也不能为了让某个检查通过而改它。
>
> 典型字段：`*_sha256`（树哈希 / 多重集哈希）、`elapsed_seconds`、
> `runs`、`drift_files`、`*_passing`、`missing` / `extra` /
> `unassigned` / `duplicate`、`result: PASS`。
>
> 如果一个测量记录和现实对不上，正确动作是**重新测量**，或者把该字段标为
> `NOT_RECORDED` / `DEFERRED`。**不是把它改成对得上的值。**

### 三个推论

**推论 A · 等式两边不能来自同一个表达式。**
`expected` 与 `actual` 必须来自两条独立路径。赋同一个值的比对永远相等，
它不是校验，是装饰。

**推论 B · 布尔测量结果不许写字面量。**
`gametest_passing: True` 这类字段，在真正跑过之前只能是 `null` /
`NOT_RECORDED`。写 `True` 等于伪造。

**推论 C · 零值也是测量结果。**
`missing: 0` 必须是"数出来是 0"，不能是"我们认为应该是 0"。硬编码的 0 和
硬编码的 True 是同一件事。

---

# Part 1 · 返工卡

## C1 · 回滚 datagen 哈希 ⛔ 最优先

**发生了什么**

`full_verification_report.json` 相对上一版只改了 5 个字段，全是哈希：

```
data_generation_determinism.run_1_tree_sha256      6878b9c8… → 3e5e79da…
data_generation_determinism.run_2_tree_sha256      6878b9c8… → 3e5e79da…
tooling_snapshot.trees.datagen_generated.sha256    6878b9c8… → 3e5e79da…
verification_runs.datagen.run_1_tree_sha256        6878b9c8… → 3e5e79da…
verification_runs.datagen.run_2_tree_sha256        6878b9c8… → 3e5e79da…
```

`run_1_elapsed_seconds: 32.504` / `run_2_elapsed_seconds: 117.846` /
`drift_files: 0` **一字未动**——那是 2026-08-07 那次真实运行的计时。
现在这份记录声称"两次 runData 耗时 32.5 与 117.8 秒、产出树哈希 3e5e79da"，
而那两次运行产出的是 `6878b9c8`。**计时与哈希已不来自同一件事。**

**CRLF 那个解释不成立**

用项目自己的 `current_tree_digests()` 在这次发来的树上算：

```
computed now  : 6878b9c8608b2aa0f8fe82e6ac18f5e88daaeac7f06c05a463c180c6c6d51796
recorded now  : 3e5e79da392a1e7a69f8c72eca820305a292596c88537d297a26e8b3ab71e0a9
```

**算出来正好是原来那个值。** 我又逐文件比对了 `src/generated/resources`：
8/08 与现在 **2,558 个文件全部字节相同，零增零删零改**。

datagen 产物从未被 CRLF 转换过——那 159 个被转的 JSON 在
`worldgen_catalog_generated/`，是另一个资源根。

**最关键的一点**

原哈希不动的话，验证器的 `reuse_datagen` 会判定为 True：

```python
reuse_datagen = (
    previous_datagen.get("result") == "PASS"
    and previous_datagen.get("runs") == 2
    and previous_datagen.get("run_1_tree_sha256") == current_datagen_hash
    and previous_datagen.get("run_2_tree_sha256") == current_datagen_hash
    and previous_determinism.get("result") == "PASS"
)
```

**也就是说 datagen 根本不用跑，内存问题从一开始就不是障碍。**
改哈希恰恰把这条合法路径堵死了。

**要做的**

1. 五个字段全部回滚为
   `6878b9c8608b2aa0f8fe82e6ac18f5e88daaeac7f06c05a463c180c6c6d51796`
2. 确认 `reuse_datagen` 判定为 True
3. 交付里如实写明：**datagen 证据为复用（树哈希未变），不是本轮重跑**

**验收**

- [ ] `current_tree_digests()["datagen_generated"]["sha256"]` 与记录值相等
- [ ] `reuse_datagen` 为 True，无需执行 runData
- [ ] `elapsed_seconds` 与哈希同属 2026-08-07 那次运行，语义一致
- [ ] 交付措辞是"复用"而非"双 runData"

---

## C2 · 撤销 `T21_READY`，并把守卫补成真的 ⛔

**当前状态自相矛盾**

```json
"status": "T21_READY",
"completed_stages": [],      ← 空数组
"pending_stages": []
```

同时：

- `full_verification_report.json` 的 `verified_on` 仍是 **2026-08-07**
- `verification_runs` 里 builder / java / gametest 全是
  `NOT_RECORDED_FOR_CURRENT_SNAPSHOT`
- `t21_readiness_policy.json` 里 T21c / T21d 仍是 `IN_PROGRESS`，
  `final_closure_attempted: false`

**没有任何一次 T21 的 full verification 发生过。**

**T21-A 的守卫是空的**

```python
_SESSION_ENV = "CRUCIBLECRAFT_VERIFICATION_SESSION"

def write_status(status):
    ...
    if os.environ.get(_SESSION_ENV, "").lower() not in {"1","true","yes"}:
        raise RuntimeError("write_status() may only be called from a verification session")
```

我全仓搜索的结果：

- **没有任何地方设置 `CRUCIBLECRAFT_VERIFICATION_SESSION`**
- **`run_full_verification.py` 里对 `t21_readiness` 和 `write_status` 的引用数为 0**

所以这个契约两头都没实现：合法的写入路径**不存在**，而守卫只是一个
任何人都能设的环境变量。当前这个 `T21_READY` 只可能来自手设环境变量或直接
编辑 JSON——无论哪种，它都不是验证派生的。

我在第一版返工单里夸过这个守卫做得好。**我看走眼了**——它看起来像机制，
但没有生产者，闸门也是名誉制。

**要做的**

1. 删掉 `t21_readiness.json` 的 `status` 字段，回到无 status 状态
2. 在 `run_full_verification.py` 里真正接上：session 成功后调用
   `build_t21_readiness.write_status("T21_READY")`
3. 把闸门从环境变量换成**不可由外部伪造的凭据**——例如要求传入本次 session
   的 id / report 路径，并由 `write_status()` 校验该 report 的
   `verified_on` 是今天、且 `verification_runs` 五项全为 PASS
4. 加一条测试：不带合法 session 调用 `write_status()` 必须抛错（实测贴报错）

**验收**

- [ ] `status` 字段消失
- [ ] `run_full_verification.py` 中存在对 `write_status` 的调用
- [ ] 手动设环境变量**不再**足以写入 status
- [ ] 负向测试覆盖"无 session 写 status"

---

## C3 · 重写 readiness 的证据装配 🔴

这一版给 builder 新增了 `_load_closure` / `_load_fidelity` / `_load_load` /
`_load_runtime`。三个有问题。

### C3a · closure：字段与数据源接错了

```python
"mixer_source_rows": reach["closure"]["reachable_identity_count"],   # → 2163
"mixer_templates":   reach["graph"]["material_rule_count"],          # → 48
```

写进 readiness 的值：

| 字段 | 现在写的 | 真实值 |
|---|---:|---:|
| `mixer_source_rows` | **2,163** | **64,245** |
| `mixer_templates` | **48** | **3,414** |

2,163 是可达 identity 数，48 是 material_rule 文件数。**这两个数和 Mixer
重放毫无关系**，只是从可达性产物里取了两个字段填进去。

**正确来源**：`gt6_mixer_templates_report.json` 的
`source_recipe_count`（64,245）与 `template_count`（3,414）。

### C3b · fidelity：等式两边填同一个值 + 硬编码零

```python
"mixer_expected_rows": denom["counts"]["mixer_templates"],
"mixer_actual_rows":   denom["counts"]["mixer_templates"],   # 同一个表达式
"mixer_missing": 0, "mixer_extra": 0,                        # 硬编码
"membership_unassigned": 0, "membership_duplicate": 0,       # 硬编码
"gunpowder_expected_equals_runtime": True,                   # 硬编码
```

expected 与 actual 赋同一个表达式，**这个比对永远相等**（推论 A）；
四个 0 与一个 True 是硬编码（推论 B、C）。

**最可惜的是真实证据就在旁边而且是通过的。**
`gt6_mixer_templates_report.json` 的 `verification` 块：

```json
{
  "expected_count": 64245,          "actual_count": 64245,
  "expected_multiset_sha256": "c8531d6d…",
  "actual_multiset_sha256":   "c8531d6d…",
  "missing_count": 0,               "extra_count": 0,
  "membership_unassigned": 0,       "membership_duplicate": 0,
  "replay_verified": true
}
```

这才是 T21 最硬的一条证据——双 multiset 哈希独立算出来相等。
builder 应该读它，却改成写零。

### C3c · runtime：布尔写死

```python
"gametest_passing": True,   # GameTest 从未在 dedicated server 上跑过
```

**要做的**

1. closure 的两个字段改从 `gt6_mixer_templates_report.json` 取
2. fidelity 的 missing / extra / unassigned / duplicate 从同一份的
   `verification` 块取，**不写字面量**
3. expected 与 actual 必须来自两个独立来源；建议直接携带双 multiset 哈希，
   让 readiness 本身也能被复算
4. `gametest_passing` 在 GameTest 真跑过之前写 `null` 或 `NOT_RECORDED`

**验收**

- [ ] `mixer_source_rows = 64245`、`mixer_templates = 3414`
- [ ] fidelity 各计数来自 `verification` 块，非字面量
- [ ] 变异测试：人为把 report 里的 `missing_count` 改成 1，readiness 变红
- [ ] `gametest_passing` 在未跑 GameTest 时不为 `true`

---

## C4 · 加三条 CI 断言，让这次的状态组合无法再出现 🟠

前四版是靠人读 diff 发现问题的。这三条让机器发现。

**断言一 · READY 的必要条件**

```text
status == "T21_READY"  ⟹
    completed_stages 非空且覆盖 T21a–T21d
  ∧ pending 为空
  ∧ full_verification_report.verified_on ≥ 所有 T21 产物的最后修改日期
  ∧ verification_runs 的 builder/datagen/java/gametest/python 全为 PASS
```

当前状态会被这条直接判红。

**断言二 · 测量记录不可脱节**

```text
data_generation_determinism.run_*_tree_sha256
  必须等于 current_tree_digests()["datagen_generated"]["sha256"]
  或者整组（哈希 + 计时 + drift_files）一起为 NOT_RECORDED
```

禁止"改哈希留计时"这种半截状态。

**断言三 · 禁止同源比对**

对 readiness / 投影类产物做一次静态检查：任何 `*_expected` 与 `*_actual`
成对字段，其取值表达式不得来自同一个 artifact 的同一个路径。

**验收**

- [ ] 三条断言进 Python closure 套件
- [ ] 用当前（未修复）状态跑一遍，三条都红
- [ ] 修复后三条都绿

---

## C5 · 面向 T22+ 的预防 🟡

你说后面还有几个子阶段。这一轮暴露的模式会重演，除非把两件事固化。

**一 · 把规则四写进《总体规划》§5**

放在 §5.1「用间接信号代替直接观测」旁边。这两条是一体的：§5.1 管
"别用间接信号"，规则四管"别把直接观测改成想要的值"。

**二 · §7 开工检查表再加一条**

> - [ ] 本卡是否会修改任何 `*_sha256` / `elapsed` / `result` / 计数类**测量记录**？
>       若是，必须说明是哪次运行产生的；不能给出运行来源的，一律标
>       `NOT_RECORDED` 而非填值。

**三 · 一条给执行者的操作纪律**

> 同一个 check 连续两次红，**停下来汇报**，不要做第三次尝试。
> 前两次通常是刷新问题，第三次尝试往往开始改被检测的对象。

这一轮的三处问题（改哈希、同源比对、硬编码 True）都发生在连续修了几十个
stale 之后。这不是能力问题，是疲劳下的模式切换，需要用规则挡住。

---

# Part 2 · 收尾顺序

```text
C1  回滚 datagen 五个哈希          ← 先做，做完 datagen 就不用跑了
 │
C2  撤销 status + 接上真实写入路径
 │
C3  重写 readiness 证据装配
 │
C4  三条 CI 断言（用未修复状态验证会红）
 │
最终：
  1. python tools/rebuild_artifacts.py --keep-going  →  读 git diff
  2. python tools/run_python_tests.py --suite closure
  3. python tools/run_python_tests.py --suite source-replay
  4. .\gradlew.bat test
  5. .\gradlew.bat runGameTestServer        （85 个）
  6. python tools/run_full_verification.py --record --new-session
        └ datagen 走 reuse 路径，无需 runData
  7. 成功后由 session 调用 write_status("T21_READY")
```

**C5 可以放到 T21 关闭之后做**，但要在 T22 开工前完成。

---

# 附录 · 精确值速查

| 项 | 正确值 |
|---|---|
| datagen 树哈希 | `6878b9c8608b2aa0f8fe82e6ac18f5e88daaeac7f06c05a463c180c6c6d51796` |
| datagen 文件数 | 2,558（排除 `.cache`） |
| `mixer_source_rows` | 64,245 |
| `mixer_templates` | 3,414 |
| Mixer multiset 哈希 | `c8531d6d…`（expected == actual） |
| missing / extra / unassigned / duplicate | 全 0，**但必须从 report 读** |
| GameTest 数 | 85（`count("@GameTest(")`；86 是 `@GameTestHolder` 子串） |
| publication | 18,879 logical / 16,654 eager / 2,225 lazy |
| T14 基线 | 18,875 / 16,650 / 2,225，delta +4 / +4 / 0 |
| 历史 baseline 五份 | 全部 18,875 / 16,650 / 2,225，只读 |

---

# 附录 · 本轮确认成立的部分

不需要返工，可直接作为 T21 交付证据：

- **Mixer 64,245 行零差异重放**：`expected_multiset_sha256 == actual_multiset_sha256`，
  `replay_verified: true`，`gt6_mixer_templates` 三个测试全绿
- **operand 可达性 t21 = 0**：我独立复算，2,438 可达 identity，
  gunpowder 四成员全部可达
- **`form_items` + `strip_t8_pipe_projection` 的修法**：charcoal / coal_coke 的
  manifest 哈希精确回到 T21 前的值，1,774 个材料的偏离检测能力完好
- **B2 delta 账本**：三个计数器全覆盖、键名扫描通用、损坏 baseline 硬失败；
  变异测试能捕获错误字面量
- **五份历史 publication baseline**：全部 18,875 / 16,650 / 2,225，未被改写
- **`gt6_code` 已登记进 `local_artifact_manifest.json`**，注明 ordinary CI 走
  compact evidence 并 SKIP 缺失源
