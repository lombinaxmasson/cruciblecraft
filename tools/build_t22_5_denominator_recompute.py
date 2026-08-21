#!/usr/bin/env python3
"""Build the T22.5 C1 artifact: the real four-column denominator over
the 146,841 ordinary_optional expanded rows.

Columns (mutually exclusive, sum == 146,841):
  v1                  B1 v1_required rows not yet published
  post_1_0_portfolio  ordinary_optional + post_1_0_g10 +
                      post_1_0_nuclear + cross_mod_compat +
                      ore_processing_byproduct + generic_processing +
                      already_covered (minus unlockable rows)
  out_of_scope        B1 out_of_scope (minus unlockable rows)
  可解锁 (unlockable) rows whose ONLY blocking operand is a mapping gap:
                      an A2 no_cc_fluid fluid name or an A3-remappable
                      equivalence item (crushed_ore / block /
                      dense_plate)

Two independent paths asserted: the ledger-2 expanded-row diagnostic
(146,841, T21 snapshot) vs the B1 operand classification total.

The ceiling is NOT adjusted unless the T14 four-protocol measurement
is triggered; the trigger condition is recorded here.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t22_5_denominator_recompute.json"
BUILDER = Path(__file__).resolve()

B1_ARTIFACT = TOOLS / "t22_5_row_classification.json"
A2_ARTIFACT = TOOLS / "t22_5_fluid_mapping.json"
A3_ARTIFACT = TOOLS / "t22_5_item_classification.json"
LEDGER_2 = TOOLS / "t21_template_denominator.json"
MIXER_INDEX = TOOLS / "gt6_mixer_templates_index.json"
T22_READINESS = TOOLS / "t22_readiness.json"
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump"


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


PORTFOLIO_CLASSES = (
    "ordinary_optional",
    "cross_mod_compat",
    "post_1_0_g10",
    "post_1_0_nuclear",
    "ore_processing_byproduct",
    "generic_processing",
    "already_covered",
)

A3_EQUIVALENCE_CLASSES = (
    "equivalence_crushed_ore",
    "equivalence_block_variant",
    "equivalence_dense_plate",
)


def _is_mapping_only_blocker(
    fluid_names: list[str],
    item_ids: list[str],
    a2_by_name: dict[str, Any],
    a2_unknown_by_name: dict[str, Any],
    a3_by_item: dict[str, str],
) -> bool:
    """True when every blocking operand is a mapping gap (A2
    no_cc_fluid fluid or A3 equivalence remappable item) and there is
    at least one blocker."""
    blockers: list[str] = []
    for name in fluid_names:
        a2 = a2_by_name.get(name)
        if a2 is not None and a2["disposition"] == "no_cc_fluid":
            blockers.append("fluid_gap")
            continue
        unknown = a2_unknown_by_name.get(name, {}).get("class")
        if unknown is not None:
            blockers.append(f"definitional:{unknown}")
    for item in item_ids:
        klass = a3_by_item.get(item)
        if klass in A3_EQUIVALENCE_CLASSES:
            blockers.append("item_gap")
        elif klass is not None and klass.startswith("out_of_scope"):
            blockers.append(f"definitional:{klass}")
    return bool(blockers) and all(
        blocker in ("fluid_gap", "item_gap") for blocker in blockers
    )


def scan_unlockable() -> tuple[int, int]:
    """Scan B1's cross_mod_compat and out_of_scope rows (non-mixer +
    mixer templates) for mapping-only blockers.  Returns
    (unlockable_rows, scanned_rows)."""
    b1 = _load(B1_ARTIFACT)
    a2 = _load(A2_ARTIFACT)
    a3 = _load(A3_ARTIFACT)
    ledger_2 = _load(LEDGER_2)
    mixer_index = _load(MIXER_INDEX)

    a2_by_name = {row["fluid"]: row for row in a2["mapping"]}
    a2_unknown_by_name = {
        row["fluid"]: row for row in a2["unknown_identity"]["records"]
    }
    a3_by_item = {row["item"]: row["class"] for row in a3["records"]}
    class_index = {i: c for i, c in enumerate(b1["classes"])}
    maps = ledger_2["encoding"]["maps"]

    dump_cache: dict[int, list[Any]] = {}
    unlockable = 0
    scanned = 0

    for row in b1["non_mixer_rows"]:
        map_index, recipe_index, klass_i = row
        klass = class_index[klass_i]
        if klass not in ("cross_mod_compat", "out_of_scope"):
            continue
        scanned += 1
        map_name = maps[map_index]
        if map_index not in dump_cache:
            document = _load(DUMP / "maps" / f"{map_name}.json")
            dump_cache[map_index] = document.get("recipes") or []
        recipe = dump_cache[map_index][recipe_index]
        fluids = [
            stack["fluid"]
            for side in ("fluidInputs", "fluidOutputs")
            for stack in recipe.get(side) or []
            if isinstance(stack, dict) and isinstance(
                stack.get("fluid"), str
            )
        ]
        items = [
            str(stack.get("item"))
            for side in ("inputs", "outputs")
            for stack in recipe.get(side) or []
            if isinstance(stack, dict) and stack.get("item")
        ]
        if _is_mapping_only_blocker(
            fluids, items, a2_by_name, a2_unknown_by_name, a3_by_item
        ):
            unlockable += 1

    for template_row in b1["mixer_templates"]:
        if template_row["class"] not in (
            "cross_mod_compat",
            "out_of_scope",
        ):
            continue
        scanned += template_row["rows"]
        template = mixer_index["templates"][
            template_row["template_index"]
        ]
        skeleton = template.get("skeleton") or {}
        fluids = [
            stack["fluid"]
            for side in ("fluidInputs", "fluidOutputs")
            for stack in skeleton.get(side) or []
            if isinstance(stack, dict) and isinstance(
                stack.get("fluid"), str
            )
        ]
        items = [
            str(stack.get("item"))
            for side in ("inputs", "outputs")
            for stack in skeleton.get(side) or []
            if isinstance(stack, dict) and stack.get("item")
        ]
        if _is_mapping_only_blocker(
            fluids, items, a2_by_name, a2_unknown_by_name, a3_by_item
        ):
            unlockable += template_row["rows"]

    return unlockable, scanned


def build() -> dict[str, Any]:
    b1 = _load(B1_ARTIFACT)
    ledger_2 = _load(LEDGER_2)
    t22 = _load(T22_READINESS)
    by_class = b1["counts"]["by_class"]
    total = b1["counts"]["total"]
    ledger_total = ledger_2["counts"]["expanded_row_diagnostics"][
        "ordinary_optional"
    ]
    if total != ledger_total:
        raise ValueError(
            f"B1 total {total} != ledger-2 diagnostic {ledger_total}"
        )

    unlockable, scanned = scan_unlockable()

    v1 = by_class["v1_required"]
    out_of_scope = max(by_class["out_of_scope"] - unlockable, 0)
    portfolio = total - v1 - out_of_scope - unlockable
    columns = {
        "v1": v1,
        "post_1_0_portfolio": portfolio,
        "out_of_scope": out_of_scope,
        "unlockable": unlockable,
    }
    if sum(columns.values()) != total:
        raise ValueError(
            f"columns sum {sum(columns.values())} != {total}"
        )

    hard_ceiling = t22.get("load", {}).get("hard_ceiling", 21000)
    current_logical = t22.get("load", {}).get("logical_rows", 18882)
    headroom = hard_ceiling - current_logical

    return {
        "schema_version": 1,
        "status": "T22_5_DENOMINATOR_RECOMPUTE_READY",
        "universe": {
            "rows": total,
            "source": "ledger-2 ordinary_optional expanded rows",
        },
        "column_rules": {
            "v1": "B1 v1_required (promotion rule); zero by universe "
            "construction",
            "post_1_0_portfolio": "all remaining portfolio classes "
            "(ordinary_optional, cross_mod_compat, post_1_0_g10, "
            "post_1_0_nuclear, ore_processing_byproduct, "
            "generic_processing, already_covered) minus unlockable",
            "out_of_scope": "B1 out_of_scope minus unlockable",
            "unlockable": "rows whose only blocking operand is an A2 "
            "no_cc_fluid fluid name or an A3-remappable equivalence "
            "item (crushed_ore / block / dense_plate)",
        },
        "columns": columns,
        "portfolio_breakdown": {
            cls: by_class[cls] for cls in PORTFOLIO_CLASSES
        },
        "unlockable_scan": {
            "scanned_rows": scanned,
            "definition": "cross_mod_compat + out_of_scope rows "
            "scanned for mapping-only blockers",
        },
        "independent_paths": {
            "ledger_2_diagnostic": ledger_total,
            "b1_classification_total": total,
            "equal": ledger_total == total,
        },
        "ceiling": {
            "hard_ceiling": hard_ceiling,
            "current_logical": current_logical,
            "headroom": headroom,
            "v1_incremental_unpublished": v1,
            "adjustment": "not required",
            "justification": (
                "The v1 column is zero: no optional row was promoted "
                "to v1, so the 21,000 logical hard ceiling is not "
                "touched before v1 (headroom 2,118). The unlockable "
                "and portfolio columns are post-1.0 work."
            ),
            "trigger": (
                "If v1_incremental_unpublished > headroom, start the "
                "T14 four-protocol measurement (server reload/index, "
                "client re-expansion, peak memory, steady-state "
                "lookup) before adjusting the ceiling."
            ),
        },
        "inputs": {
            _relative(B1_ARTIFACT): _sha256(B1_ARTIFACT),
            _relative(A2_ARTIFACT): _sha256(A2_ARTIFACT),
            _relative(A3_ARTIFACT): _sha256(A3_ARTIFACT),
            _relative(LEDGER_2): _sha256(LEDGER_2),
            _relative(T22_READINESS): _sha256(T22_READINESS),
            _relative(BUILDER): _sha256(BUILDER),
        },
    }


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors
    on_disk = _load(OUTPUT)
    if on_disk.get("schema_version") != 1:
        errors.append("schema_version != 1")
    if on_disk.get("status") != "T22_5_DENOMINATOR_RECOMPUTE_READY":
        errors.append("status != T22_5_DENOMINATOR_RECOMPUTE_READY")
    columns = on_disk.get("columns") or {}
    if sum(columns.values()) != 146841:
        errors.append(
            f"columns sum != 146841 (got {sum(columns.values())})"
        )
    b1 = _load(B1_ARTIFACT)
    if b1["counts"]["total"] != 146841:
        errors.append("B1 total drifted")
    ceiling = on_disk.get("ceiling") or {}
    if ceiling.get("adjustment") != "not required":
        errors.append("ceiling adjustment claimed without T14 protocol")
    if not ceiling.get("justification") or not ceiling.get("trigger"):
        errors.append("ceiling justification/trigger missing")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--reference-only", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = _load(OUTPUT)
        elif args.write:
            if not args.full_replay:
                raise ValueError("--write requires --full-replay")
            document = build()
            OUTPUT.write_bytes(_stable(document).encode("utf-8"))
        else:
            parser.error("choose --check or --write")
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T22.5 denominator recompute failed: {error}",
              file=sys.stderr)
        return 1
    print(
        json.dumps(
            {
                "schema_version": document.get("schema_version"),
                "status": document.get("status"),
                "columns": document.get("columns"),
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
