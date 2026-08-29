#!/usr/bin/env python3
"""Summarize unreachable T39 centrifuge player-path inputs against T21 closure."""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

_ROOT = Path(__file__).resolve().parents[1]
if str(_ROOT) not in sys.path:
    sys.path.insert(0, str(_ROOT))

from tools import t39_common as t39

ROOT = t39.ROOT
TOOLS = t39.TOOLS
PLAYER_PATH = t39.PLAYER_PATH
T21 = TOOLS / "t21_operand_reachability.json"
REQUIRED_FORMS = t39.REQUIRED_FORMS
MATERIAL_GATE = (
    ROOT / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
OUTPUT = TOOLS / "t39_player_path_gaps.json"

ALLOY_MARKERS = (
    "duranium",
    "naquadah",
    "naquadria",
    "trinium",
    "neutronium",
    "americium",
    "plutonium",
    "uranium235",
    "uranium238",
    "blutonium",
    "yellorium",
    "cyanite",
    "ludicrite",
    "indium",
    "osmiridium",
    "ruridit",
    "tritanium",
    "infused",
    "elemental",
)


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _classify_consume(
    cid: str,
    reachable: set[str],
    gate_materials: dict[str, list[str]],
) -> str:
    if cid.startswith("minecraft:"):
        return "vanilla_survival_missing_from_t21"
    match = re.match(r"cruciblecraft:([^/]+)/(.+)", cid)
    if not match:
        return "other"
    material, form = match.group(1), match.group(2)
    forms = gate_materials.get(material, [])
    if "rock" in forms and f"item:cruciblecraft:{material}/rock" in reachable:
        return "material_with_rock_seed"
    for seed_form in ("raw_ore", "ore", "crushed_ore", "rock"):
        if (
            seed_form in forms
            and f"item:cruciblecraft:{material}/{seed_form}" in reachable
        ):
            return f"material_with_{seed_form}_seed"
    if any(marker in material for marker in ALLOY_MARKERS):
        return "high_tier_alloy_no_seed"
    if form in ("dust", "small_dust", "tiny_dust", "dust_div72"):
        return "material_dust_no_seed"
    if form in ("ingot", "plate", "rod", "nugget"):
        return "material_form_no_seed"
    return "other_cc"


def _fixable_rock_dust(
    cid: str,
    reachable: set[str],
    gate_materials: dict[str, list[str]],
) -> bool:
    match = re.match(r"cruciblecraft:([^/]+)/dust$", cid)
    if not match:
        return False
    material = match.group(1)
    return (
        "rock" in gate_materials.get(material, [])
        and f"item:cruciblecraft:{material}/rock" in reachable
        and f"item:cruciblecraft:{material}/dust" not in reachable
    )


def build() -> dict[str, Any]:
    player_path = _load(PLAYER_PATH)
    t21 = _load(T21)
    reachable = set(t21["closure"]["reachable_identities"])
    gate_materials = (_load(MATERIAL_GATE).get("materials") or {})
    required_forms = (_load(REQUIRED_FORMS).get("required_forms") or {})

    consume_freq: Counter[str] = Counter()
    fluid_freq: Counter[str] = Counter()
    for row in player_path["rows"]:
        if row.get("inputs_reachable", True):
            continue
        consume_freq.update(row.get("consume_ids", []))
        fluid_freq.update(row.get("fluid_input_ids", []))

    taxonomy: dict[str, list[dict[str, object]]] = defaultdict(list)
    for cid, count in consume_freq.items():
        category = _classify_consume(cid, reachable, gate_materials)
        taxonomy[category].append(
            {
                "id": cid,
                "count": count,
                "in_t21_closure": f"item:{cid}" in reachable,
            }
        )
    for category in taxonomy:
        taxonomy[category].sort(key=lambda row: (-int(row["count"]), str(row["id"])))

    taxonomy_summary = {
        category: {
            "unique_ids": len(rows),
            "mention_count": sum(int(row["count"]) for row in rows),
        }
        for category, rows in sorted(taxonomy.items())
    }

    closure_scenarios = {
        "rock_mortar_dust_all": 0,
        "t39_required_forms_only": 0,
        "vanilla_seeds_added": 0,
        "fluid_blocked_relations": 0,
    }
    for row in player_path["rows"]:
        if row.get("inputs_reachable", True):
            continue
        consume = row.get("consume_ids", [])
        fluids = row.get("fluid_input_ids", [])
        if fluids:
            closure_scenarios["fluid_blocked_relations"] += 1
        if consume and not fluids and all(
            _fixable_rock_dust(cid, reachable, gate_materials) for cid in consume
        ):
            closure_scenarios["rock_mortar_dust_all"] += 1
        if consume and not fluids and all(
            (match := re.match(r"cruciblecraft:([^/]+)/(.+)", cid))
            and match.group(1) in required_forms
            and match.group(2) in required_forms[match.group(1)]
            for cid in consume
        ):
            closure_scenarios["t39_required_forms_only"] += 1
        if consume and not fluids and all(
            cid.startswith("minecraft:") for cid in consume
        ):
            closure_scenarios["vanilla_seeds_added"] += 1

    top20 = [
        {"id": cid, "count": count, "in_t21_closure": f"item:{cid}" in reachable}
        for cid, count in consume_freq.most_common(20)
    ]

    return {
        "schema_version": 1,
        "status": "T39_PLAYER_PATH_GAPS",
        "reachability_source": player_path.get("reachability_source"),
        "counts": {
            "relations": player_path["relations"],
            "inputs_reachable": player_path["inputs_reachable"],
            "relations_with_unreachable_inputs": player_path[
                "relations_with_unreachable_inputs"
            ],
            "unique_unreachable_consume_ids": len(consume_freq),
            "unique_unreachable_fluid_input_ids": len(fluid_freq),
            "unreachable_consume_mentions": sum(consume_freq.values()),
            "unreachable_fluid_mentions": sum(fluid_freq.values()),
        },
        "closure_scenarios": closure_scenarios,
        "taxonomy_summary": taxonomy_summary,
        "top_consume_ids": top20,
        "fluid_input_ids": [
            {
                "id": fid,
                "count": count,
                "in_t21_closure": f"fluid:{fid}" in reachable,
            }
            for fid, count in fluid_freq.most_common()
        ],
        "taxonomy": {category: rows for category, rows in sorted(taxonomy.items())},
        "notes": {
            "recovery_builder_decision": (
                "No bounded source-backed recovery set like T38 (5 pinned routes). "
                "Rock mortar projection alone needs 42 materials; 87 relations "
                "require molten-fluid chains."
            ),
            "t21_regeneration": (
                "python tools/t21_operand_reachability.py --json would refresh "
                "closure after T39 required forms and upstream ore-processing "
                "routes exist; it does not seed mob drops or close molten-fluid "
                "gaps by itself."
            ),
            "vanilla_missing_reason": (
                "T21 explicitly excludes mob drops from survival seeds; egg, "
                "honeycomb, magma_cream, feather, etc. are intentional gaps unless "
                "added to _vanilla_survival_seeds()."
            ),
        },
    }


def _stable(document: dict[str, Any]) -> str:
    return json.dumps(document, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(_stable(document), encoding="utf-8")
    return document


def check() -> list[str]:
    document = build()
    if not OUTPUT.is_file() or OUTPUT.read_text(encoding="utf-8") != _stable(document):
        return [f"stale: {OUTPUT.relative_to(ROOT).as_posix()}"]
    return []


def _print_summary(document: dict[str, Any]) -> None:
    print("=== T39 Player Path Gap Summary ===")
    for key, value in document["counts"].items():
        print(f"{key}: {value}")
    print()
    print("Taxonomy:")
    for category, summary in document["taxonomy_summary"].items():
        print(
            f"  {category}: {summary['unique_ids']} ids, "
            f"{summary['mention_count']} mentions"
        )
    print()
    print("Closure scenarios (relations that would become reachable):")
    for key, value in document["closure_scenarios"].items():
        print(f"  {key}: {value}")
    print()
    print("Top 20 unreachable consume_ids:")
    for row in document["top_consume_ids"]:
        print(
            f"  {row['count']:3d}  {row['id']}  "
            f"t21={row['in_t21_closure']}"
        )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    if args.write:
        document = write()
        _print_summary(document)
        print()
        print(f"Wrote {OUTPUT.relative_to(ROOT).as_posix()}")
        return 0
    if args.check:
        errors = check()
        if errors:
            print("\n".join(errors))
            return 1
        return 0
    _print_summary(build())
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
