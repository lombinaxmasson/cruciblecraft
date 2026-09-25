#!/usr/bin/env python3
"""Record dump rows that are neither published nor already blocked."""
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(ROOT))
sys.path.insert(1, str(ROOT / "tools"))
from tools import census_common as census
from tools.build_assembler_source import _source_row_hash

WAVE = ROOT / "tools/waves/recipe/gt6-extruder-bulk"
dump = json.loads((WAVE / "source_pack/dump_slice.json").read_text(encoding="utf-8"))
source = json.loads((WAVE / "source.json").read_text(encoding="utf-8"))
blocked = census.load_json(WAVE / "blocked.json")
published = {str(row.get("source_row_sha256") or "") for row in source["relations"]}
rows = list(blocked.get("rows") or [])
known = {str(row.get("source_row_sha256") or "") for row in rows}
added = 0
for index, recipe in enumerate(dump["recipes"]):
    digest = _source_row_hash(recipe)
    if digest in published or digest in known:
        continue
    rows.append(
        {
            "reason": "extruder host rejected recipe shape",
            "source_recipe_index": str(index),
            "source_row_sha256": digest,
        }
    )
    known.add(digest)
    added += 1
blocked["rows"] = rows
census.write_stable(WAVE / "blocked.json", blocked)
proof = census.load_json(WAVE / "coverage_proof.json")
proof["blocked_rows"] = len(rows)
proof["host_rejected_rows"] = added
proof["accounted_rows"] = proof["published_rows"] + len(rows)
census.write_stable(WAVE / "coverage_proof.json", proof)
print(f"added {added} blocked {len(rows)} published {proof['published_rows']} source {len(dump['recipes'])}")
