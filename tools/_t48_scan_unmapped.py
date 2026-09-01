#!/usr/bin/env python3
"""One-shot diagnostic: T48 work-set unmapped operands in the T47 source."""
from __future__ import annotations

from collections import Counter, defaultdict

from tools import t48_common as c
from tools import t48_identities as ids


def main() -> None:
    ws = c.load_json(c.WORK_SET)
    wanted = set(ws["family_ids"])
    print("loading t47 source...")
    src = c.load_json(c.t47.SOURCE)
    print("relations", len(src.get("relations") or []))
    kind_items: dict[str, Counter[str]] = defaultdict(Counter)
    unmapped: Counter[str] = Counter()
    forms: Counter[str] = Counter()
    fluids: Counter[str] = Counter()
    kind_fams: dict[str, set[str]] = defaultdict(set)
    display: dict[str, str] = {}
    matched = 0
    for rel in src.get("relations") or []:
        fid = str(rel.get("family_id") or "")
        if fid not in wanted:
            continue
        matched += 1
        ops = (
            list(rel.get("item_inputs") or [])
            + list(rel.get("item_outputs") or [])
            + list(rel.get("fluid_inputs") or [])
            + list(rel.get("fluid_outputs") or [])
        )
        for op in ops:
            src_row = op.get("source") or {}
            runtime = op.get("runtime_id")
            mapping = str(op.get("mapping") or "")
            item = str(src_row.get("item") or "")
            fluid = str(src_row.get("fluid") or "")
            meta = src_row.get("meta")
            if fluid and (mapping == "blocked_unmapped" or not runtime):
                fluids[fluid] += 1
            if not item:
                continue
            if mapping != "blocked_unmapped" and runtime:
                continue
            kind = ids.classify_source_item(item)
            key = f"{item}@{meta}"
            unmapped[key] += 1
            kind_items[kind][key] += 1
            kind_fams[kind].add(fid)
            display[key] = str(src_row.get("displayName") or "")
            if op.get("material") or op.get("form"):
                forms[f"{op.get('material')}:{op.get('form')}"] += 1
    print("matched relations", matched)
    print("unmapped kinds", {k: (len(v), sum(v.values())) for k, v in kind_items.items()})
    print("kind families", {k: len(v) for k, v in kind_fams.items()})
    print("unmapped item types", len(unmapped))
    print("unmapped fluids", dict(fluids))
    print("unregistered forms", len(forms))
    print("top unmapped")
    for key, count in unmapped.most_common(30):
        print(f"  {count:6d} {key} {display.get(key, '')}")
    print("top forms")
    for key, count in forms.most_common(30):
        print(f"  {count:6d} {key}")
    overlay_hits = sum(
        1 for key in unmapped if ids.overlay_prefix_form(key.split("@")[0])
    )
    print("prefix overlay type hits", overlay_hits)
    print("source items")
    items = Counter(key.split("@")[0] for key in unmapped)
    for item, count in items.most_common():
        print(f"  {count:6d} {item} kind={ids.classify_source_item(item)} overlay={ids.overlay_prefix_form(item)}")


if __name__ == "__main__":
    main()
