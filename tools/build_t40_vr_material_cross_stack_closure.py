#!/usr/bin/env python3
"""Prove Python gate, Java overlay contract, and ore-chain crush rows stay closed."""
from __future__ import annotations

import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import material_form_authority as form_authority
from tools import t35_common as t35
from tools import t38_common as t38
from tools import t40_vr_common as vr

OUTPUT = vr.CROSS_STACK_CLOSURE
GATE = t38.MATERIAL_REGISTRATION_GATE_JSON
JAVA = t38.MATERIAL_REGISTRATION_GATE_JAVA


def _registered_ore(gate: dict[str, Any]) -> set[str]:
    return {
        material_id
        for material_id, forms in (gate.get("materials") or {}).items()
        if isinstance(forms, list) and "ore" in forms
    }


def _crush_ore_block_materials(ore_chain: dict[str, Any]) -> set[str]:
    return {
        str(row.get("material") or "")
        for row in ore_chain.get("recipes") or []
        if row.get("family") == "crush_ore_block_to_crushed"
    }


def build() -> dict[str, Any]:
    gate = vr.gate_document()
    ore_chain = vr.ore_chain_document()
    t38_support = t38.support_recipe_ledger()
    vein_ledger = (ore_chain.get("coverage_ledger") or {}).get(
        "worldgen_ore_materials"
    ) or []
    registered_ore = _registered_ore(gate)
    crush_ores = _crush_ore_block_materials(ore_chain)
    java_text = JAVA.read_text(encoding="utf-8")
    overlay_sections = form_authority.java_overlay_sections()
    gates = {
        "authority_current": not form_authority.check(),
        "gate_schema_v2": int(gate.get("schema_version") or 0) == 2,
        "gate_overlay_sections_match_authority": list(
            gate.get("java_overlay_sections") or []
        ) == overlay_sections,
        "java_reads_v2_overlay_sections": "java_overlay_sections" in java_text
        and "SCHEMA_V1_OVERLAY_SECTIONS" in java_text,
        "java_v1_fallback_present": "schemaVersion == 1" in java_text
        or "schemaVersion != 1" in java_text
        or "schema_version" in java_text,
        "registered_ore_matches_crush_rows": registered_ore == crush_ores,
        "t38_support_19_15": (
            int(t38_support["authored"]) == 19 and int(t38_support["eager"]) == 15
        ),
        "t5_semantic_vein_ledger_8": len(vein_ledger) == 8,
        "typed_ore_denominators_hold": vr.live_counts() == vr.FROZEN_COUNTS,
        "card_builders_do_not_write_gate": "GATE_OUT.write_text"
        not in (ROOT / "tools" / "build_t39_centrifuge_recipes.py").read_text(
            encoding="utf-8"
        )
        and "GATE_OUT.write_text"
        not in (ROOT / "tools" / "build_t40_electrolyzer_recipes.py").read_text(
            encoding="utf-8"
        ),
    }
    failed = sorted(name for name, passed in gates.items() if not passed)
    return {
        "schema_version": 1,
        "status": "T40_VR_CROSS_STACK_CLOSED" if not failed else "T40_VR_CROSS_STACK_BLOCKED",
        "generated_by": "python tools/build_t40_vr_material_cross_stack_closure.py",
        "authority_semantic_root_sha256": form_authority.semantic_root_sha256(),
        "typed_ore_denominators": {
            "factual_ore_materials": vr.FROZEN_COUNTS["factual_ore_materials"],
            "registered_ore_materials": vr.FROZEN_COUNTS["registered_ore_materials"],
            "t38_acquisition_ore_delta": vr.FROZEN_COUNTS["t38_acquisition_ore_delta"],
            "t5_semantic_vein_ledger": vr.FROZEN_COUNTS["t5_semantic_vein_ledger"],
        },
        "registered_ore_count": len(registered_ore),
        "crush_ore_block_count": len(crush_ores),
        "t38_support": {
            "authored": int(t38_support["authored"]),
            "eager": int(t38_support["eager"]),
        },
        "java_overlay_sections": overlay_sections,
        "gates": gates,
        "failed_gates": failed,
    }


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    return vr.check_document(OUTPUT, build())


def main(argv: list[str] | None = None) -> int:
    args = vr.parse_write_check(__doc__, argv)
    try:
        if args.write:
            write()
        else:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T40-VR cross-stack closure failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
