#!/usr/bin/env python3
from collections import Counter
from tools import t48_common as c

ws = c.load_json(c.WORK_SET)
wanted = set(ws["family_ids"])
src = c.load_json(c.t47.SOURCE)
for rel in src.get("relations") or []:
    if str(rel.get("family_id") or "") not in wanted:
        continue
    for side in ("item_inputs", "item_outputs"):
        for op in rel.get(side) or []:
            item = str((op.get("source") or {}).get("item") or "")
            if item.startswith("minecraft:") or item.startswith("gregapi:"):
                print(rel["family_id"], side, op.get("mapping"), op.get("runtime_id"), op.get("source"))
