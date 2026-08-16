#!/usr/bin/env python3
"""Build source_mod_inventory.json skeleton from originalMod grouping.

Decision carrier (not an identification tool): humans fill the three 1.21.1 fields.
Display names need OreDictDumper to also emit ModData.mName (currently id-only).
"""
from __future__ import annotations

import json
import sys
from collections import Counter, defaultdict
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
OUT = TOOLS / "source_mod_inventory.json"

sys.path.insert(0, str(TOOLS))
from gt6_structural_class import NATIVE_ORIGINAL_MODS, structural_class  # noqa: E402


def main() -> int:
    materials = json.loads(
        (TOOLS / "gt6_oredict_materials_normalized.json").read_text(encoding="utf-8")
    )["records"]
    by_mod: dict[str, list] = defaultdict(list)
    for m in materials:
        mod = m.get("original_mod")
        key = mod if mod else "(none)"
        by_mod[key].append(m)

    mods: dict[str, dict] = {}
    for mod_id, rows in sorted(by_mod.items(), key=lambda kv: (-len(kv[1]), kv[0])):
        class_counts: Counter[str] = Counter(
            structural_class(r.get("source_id")) for r in rows
        )
        native = mod_id in NATIVE_ORIGINAL_MODS or mod_id == "(none)"
        mods[mod_id] = {
            "mod_id": None if mod_id == "(none)" else mod_id,
            "display_name": None,
            "material_count": len(rows),
            "by_structural_class": dict(class_counts.most_common()),
            "native_for_ownership": native,
            # Human decisions for 1.21.1 interop (T0b / pending_t0b release):
            "has_121_version": None if not native else True,
            "major_rewrite": None if not native else False,
            "registers_own_materials": None if not native else False,
            "notes": (
                "Native GT/vanilla — ownership gate does not apply."
                if native
                else "Fill the three decision fields; PENDING_T0B alloys/organics wait on this."
            ),
        }

    doc = {
        "schema_version": 1,
        "_notes": [
            "Skeleton generated from materials.original_mod. Not a name-pattern recognizer.",
            "Human fills has_121_version / major_rewrite / registers_own_materials for non-native mods.",
            "display_name is null until dump emits ModData.mName alongside mID.",
        ],
        "mods": mods,
    }
    OUT.write_text(json.dumps(doc, indent=2, ensure_ascii=False) + "\n", encoding="utf-8", newline="\n")
    print(f"Wrote {OUT} ({len(mods)} mods)")
    non_native = [m for m, row in mods.items() if not row["native_for_ownership"]]
    print("non-native mods", len(non_native), "sample", non_native[:15])
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
