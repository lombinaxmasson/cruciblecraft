#!/usr/bin/env python3
"""
GT6 RecipeMap 形状分析器

回答一个问题：这个 map 的 N 条 logical_row，值多少条 authored_rule？

用法
    python3 analyze_map_shape.py <dump.json>              # 完整分析
    python3 analyze_map_shape.py <dump.json> --schema     # 只打印 schema，先看清结构
    python3 analyze_map_shape.py <dump.json> --dump-groups out.json
    python3 analyze_map_shape.py <dump.json> --min-group 3

不假设 dump 的字段名：先自动探测，探测不准时用 --schema 看一眼再用
--inputs/--outputs/--scalars 手工指定。

输出的三个数就是决策依据：
    logical_row      dump 的行数
    authored_rule    归并后估计需要人工声明的规则数
    压缩比           前者除以后者
"""

import argparse
import collections
import json
import re
import sys
from pathlib import Path


# ---------------------------------------------------------------- schema 探测

def find_recipe_list(doc):
    """dump 顶层可能是 list，也可能是 {"recipes": [...]} 之类。"""
    if isinstance(doc, list):
        return doc, "<root>"
    if isinstance(doc, dict):
        for key in ("recipes", "entries", "rows", "list", "data"):
            if isinstance(doc.get(key), list):
                return doc[key], key
        # 退路：取最长的 list 值
        best, best_key = None, None
        for key, value in doc.items():
            if isinstance(value, list) and (best is None or len(value) > len(best)):
                best, best_key = value, key
        if best is not None:
            return best, best_key
    raise SystemExit("找不到配方数组；用 --schema 看一眼顶层结构")


def probe_schema(rows, sample=400):
    """统计字段出现频率与类型，猜哪些是 input/output/scalar。"""
    freq = collections.Counter()
    kinds = collections.defaultdict(collections.Counter)
    for row in rows[:sample]:
        if not isinstance(row, dict):
            continue
        for key, value in row.items():
            freq[key] += 1
            kinds[key][type(value).__name__] += 1

    inputs, outputs, scalars = [], [], []
    for key in freq:
        low = key.lower()
        top = kinds[key].most_common(1)[0][0]
        is_list = top == "list"
        numericish = any(is_numeric_list(r.get(key)) for r in rows[:sample]
                         if isinstance(r, dict))
        if key in META_KEYS or top == "dict":
            continue                       # 元数据不参与归并
        if is_list and numericish:
            continue                       # 并行数字数组由 pair_parallel 处理
        if is_list and "input" in low:
            inputs.append(key)
        elif is_list and "output" in low:
            outputs.append(key)
        elif not is_list:
            scalars.append(key)
    return freq, kinds, sorted(inputs), sorted(outputs), sorted(scalars)


def print_schema(rows, freq, kinds, inputs, outputs, scalars):
    print(f"行数 {len(rows)}\n")
    print("字段            出现率     类型")
    for key, count in freq.most_common():
        share = count / min(len(rows), 400)
        types = "/".join(k for k, _ in kinds[key].most_common())
        print(f"  {key:<24}{share:>6.0%}   {types}")
    print(f"\n猜测 inputs  = {inputs}")
    print(f"猜测 outputs = {outputs}")
    print(f"猜测 scalars = {scalars}")
    print("\n首行样例:")
    print(json.dumps(rows[0], ensure_ascii=False, indent=1)[:1200])


# ---------------------------------------------------------------- 归一化

ITEM_KEYS = ("tag", "id", "item", "name", "ingredient", "material", "fluid")

# 每行唯一或与事实无关的字段，参与归并会把簇全部打散
META_KEYS = {"provenance", "source", "map", "type", "id", "name", "comment",
             "_comment", "stable_id", "shadow_order", "evidence_hashes"}


def is_numeric_list(value):
    return isinstance(value, list) and value and all(
        isinstance(v, (int, float)) and not isinstance(v, bool) for v in value)


def pair_parallel(row, list_key):
    """把 item_input_counts / output_chances 这类并行数字数组配回它的成分表。"""
    stem = list_key.replace("item_", "").replace("fluid_", "")
    stem = stem.replace("s", "")  # inputs -> input
    out = []
    for key, value in row.items():
        if key == list_key or not is_numeric_list(value):
            continue
        low = key.lower()
        if stem in low:
            out.append((key, value))
    return out


def material_slug(value):
    value = re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", str(value))
    return re.sub(r"[^a-z0-9_]+", "_", value.lower()).strip("_")


def ingredient_key(entry, material_names=None):
    """把一个输入/输出条目压成可比较的字符串。数量与几率并入，因为它们属于事实。"""
    if isinstance(entry, str):
        return entry
    if not isinstance(entry, dict):
        return json.dumps(entry, sort_keys=True, ensure_ascii=False)
    ident = None
    for key in ITEM_KEYS:
        value = entry.get(key)
        if isinstance(value, str):
            ident = value
            break
        if isinstance(value, dict):  # 嵌套 {"item": {"id": ...}}
            for inner in ITEM_KEYS:
                if isinstance(value.get(inner), str):
                    ident = value[inner]
                    break
        if ident:
            break
    if ident is None:
        ident = json.dumps(entry, sort_keys=True, ensure_ascii=False)
    material_names = material_names or {}
    meta = entry.get("meta")
    if (
        ident.startswith("gregtech:gt.meta.")
        and isinstance(meta, int)
        and not isinstance(meta, bool)
    ):
        material = material_names.get(meta)
        if material is not None:
            ident = f"{ident}/{material_slug(material)}"
    extras = []
    for key in ("meta", "count", "amount", "chance", "probability"):
        if key == "meta" and ident.startswith("gregtech:gt.meta.") and "/" in ident:
            continue
        if key in entry:
            extras.append(f"{key}={entry[key]}")
    return ident + ("|" + ",".join(extras) if extras else "")


def collect(row, keys, material_names=None):
    """把成分表压成可比较的元组，并行的 count / chance 数组按位置合并进来。"""
    out = []
    for key in keys:
        value = row.get(key)
        if not isinstance(value, list):
            continue
        parallel = pair_parallel(row, key)
        for i, entry in enumerate(value):
            token = ingredient_key(entry, material_names)
            extras = [f"{pk}={pv[i]}" for pk, pv in parallel if i < len(pv)]
            out.append(token + ("|" + ",".join(extras) if extras else ""))
    return tuple(out)


def scalar_key(row, keys):
    return tuple((k, json.dumps(row.get(k), sort_keys=True, ensure_ascii=False)) for k in keys)


# ---------------------------------------------------------------- 形状分类

MATERIAL_RE = re.compile(r"[:/]([a-z0-9_]+)$")


def varying_axis(values):
    """一组只在某一维不同的输入，判断变化的那一维是什么形状。"""
    stems, prefixes = set(), set()
    for value in values:
        head = value.split("|")[0]
        match = MATERIAL_RE.search(head)
        if match:
            stems.add(match.group(1))
            prefixes.add(head[: match.start()])
        else:
            return "opaque"
    if len(prefixes) == 1 and len(stems) == len(values):
        return "material_matrix"      # c:dusts/<material> 这种，一条规则乘材料
    return "enumerated"               # 变的那一维没有统一形态，只能列举


def analyze(rows, inputs, outputs, scalars, min_group, material_names=None):
    logical = len(rows)
    coarse = collections.Counter()
    normalized = []

    for row_index, row in enumerate(rows):
        if not isinstance(row, dict):
            continue
        ins = collect(row, inputs, material_names)
        outs = collect(row, outputs, material_names)
        sca = scalar_key(row, scalars)
        normalized.append((row_index, ins, outs, sca))
        coarse[(outs, sca)] += 1

    # leave-one-out：找"只有第 i 个输入不同"的簇
    groups = collections.defaultdict(list)
    for row_index, ins, outs, sca in normalized:
        for i in range(len(ins)):
            rest = ins[:i] + ins[i + 1:]
            groups[(outs, sca, rest, i)].append((row_index, ins[i]))

    used, rules, shapes = set(), [], collections.Counter()
    # 大簇优先，避免一行被小簇先吃掉
    for key, members in sorted(groups.items(), key=lambda kv: -len(kv[1])):
        outs, sca, rest, i = key
        fresh = [(row_index, value) for row_index, value in members
                 if row_index not in used]
        if len(fresh) < min_group:
            continue
        for row_index, _value in fresh:
            used.add(row_index)
        fresh_values = [value for _row_index, value in fresh]
        shape = varying_axis(fresh_values)
        shapes[shape] += 1
        rules.append({"size": len(fresh_values), "shape": shape, "outputs": list(outs),
                      "fixed_inputs": list(rest), "varying_slot": i,
                      "varying": sorted(fresh_values)[:12],
                      "varying_total": len(fresh_values),
                      "source_rows": sorted(row_index for row_index, _value in fresh)})

    covered = len(used)
    residual = logical - covered
    authored = len(rules) + residual   # 每个簇一条规则，剩下的逐条
    return {
        "logical_row": logical,
        "matrix_groups": len(rules),
        "rows_in_groups": covered,
        "residual_named_rows": residual,
        "authored_rule_estimate": authored,
        "ratio": round(logical / authored, 2) if authored else None,
        "coarse_groups": len(coarse),
        "shapes": dict(shapes),
        "groups": sorted(rules, key=lambda r: -r["size"]),
    }


# ---------------------------------------------------------------- 输出

def report(result, top=12):
    r = result
    print("=" * 66)
    print(f"  logical_row（dump 行数）        {r['logical_row']:>10,}")
    print(f"  authored_rule（归并后估计）      {r['authored_rule_estimate']:>10,}")
    print(f"  压缩比                          {r['ratio']:>10}  : 1")
    print("=" * 66)
    print(f"\n  矩阵簇            {r['matrix_groups']:>8,} 个，覆盖 {r['rows_in_groups']:,} 行")
    print(f"  归并不掉的具名行   {r['residual_named_rows']:>8,} 行")
    print(f"  粗归并 (产物+标量) {r['coarse_groups']:>8,} 组   ← 上界参考")
    print(f"\n  簇的形状分布: {r['shapes']}")
    print("    material_matrix = 变化维是 <统一前缀>/<材料>，可写成一条规则乘材料")
    print("    enumerated      = 变化维需要列举，仍是一条规则一个文件")
    print("    opaque          = 识别不了，按具名处理")

    print(f"\n  最大的 {top} 个簇:")
    for g in r["groups"][:top]:
        fixed = ", ".join(g["fixed_inputs"]) or "(无固定输入)"
        outs = ", ".join(g["outputs"]) or "(无产物)"
        print(f"    {g['size']:>5} 行  [{g['shape']}]  {fixed}  +  <{g['varying_total']} 种>  ->  {outs}")
        print(f"           变化维样例: {', '.join(g['varying'][:6])}")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("path")
    ap.add_argument("--schema", action="store_true")
    ap.add_argument("--inputs", nargs="*")
    ap.add_argument("--outputs", nargs="*")
    ap.add_argument("--scalars", nargs="*")
    ap.add_argument("--min-group", type=int, default=3,
                    help="小于这个大小的簇不算矩阵，逐条计（默认 3）")
    ap.add_argument("--dump-groups", help="把完整簇列表写成 JSON")
    ap.add_argument("--exclude", nargs="*", default=[],
                    help="额外排除的字段名（每行唯一的元数据）")
    args = ap.parse_args()

    with open(args.path, encoding="utf-8") as fh:
        doc = json.load(fh)
    material_names = {}
    material_path = Path(args.path).resolve().parent.parent / "oredict" / "materials.json"
    if material_path.is_file():
        with material_path.open(encoding="utf-8") as fh:
            material_names = {
                int(row["id"]): row["nameInternal"]
                for row in json.load(fh)
                if isinstance(row, dict)
                and isinstance(row.get("id"), int)
                and row.get("nameInternal")
            }
    rows, where = find_recipe_list(doc)
    freq, kinds, gi, go, gs = probe_schema(rows)
    inputs = args.inputs if args.inputs is not None else gi
    outputs = args.outputs if args.outputs is not None else go
    scalars = args.scalars if args.scalars is not None else gs
    scalars = [k for k in scalars if k not in set(args.exclude)]
    inputs = [k for k in inputs if k not in set(args.exclude)]
    outputs = [k for k in outputs if k not in set(args.exclude)]

    if args.schema:
        print(f"配方数组位置: {where}\n")
        print_schema(rows, freq, kinds, gi, go, gs)
        return

    if not inputs or not outputs:
        print("自动探测失败，先跑 --schema，再用 --inputs/--outputs 指定", file=sys.stderr)
        sys.exit(2)

    print(f"配方数组: {where}    inputs={inputs}  outputs={outputs}")
    print(f"scalars={scalars}\n")
    result = analyze(
        rows, inputs, outputs, scalars, args.min_group, material_names)
    report(result)

    if args.dump_groups:
        with open(args.dump_groups, "w", encoding="utf-8") as fh:
            json.dump(result, fh, ensure_ascii=False, indent=1)
        print(f"\n完整簇列表已写入 {args.dump_groups}")


if __name__ == "__main__":
    main()
