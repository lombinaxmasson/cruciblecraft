#!/usr/bin/env python3
"""Read-only census: known demand ∩ GT6-generated ∩ ungated material forms.

Prep only. Does not write the registration gate or occupy unique-active.
The openable list is the freeze nucleus for a later bounded form-open card.
"""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[4]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import atomic_io
from tools import io_common as io

WAVE = Path(__file__).resolve().parent
OUT = WAVE / "census.json"
GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
PREFIX_DIR = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_prefixes"
)
MATERIAL_DIR = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
)
CATALOG = ROOT / "tools" / "blockers" / "catalog.json"
FORM_DEMAND = WAVE / "form_demand_pairs.json"
COPPER_FAMILY_FORMS = ("curved_plate", "double_plate")
COPPER_FAMILY_BLOCKER = "material-form/copper-family-curved-plate"
SLUG = "registry/material-form-demand-census"


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def parse_cc_form(value: str | None) -> tuple[str, str] | None:
    if not isinstance(value, str) or not value:
        return None
    item = value.strip()
    if item.startswith("cruciblecraft:"):
        item = item.split(":", 1)[1]
    if ":" in item:
        return None
    if "/" not in item or item.count("/") != 1:
        return None
    material, form = item.split("/", 1)
    if not material or not form:
        return None
    return material, form


def load_prefixes() -> dict[str, dict[str, Any]]:
    index = load_json(PREFIX_DIR / "index.json")
    prefixes: dict[str, dict[str, Any]] = {}
    for filename in index:
        document = load_json(PREFIX_DIR / filename)
        path = str(document["serialized_path"])
        prefixes[path] = document
    return prefixes


def load_material_flags() -> dict[str, set[str]]:
    flags: dict[str, set[str]] = {}
    for path in sorted(MATERIAL_DIR.glob("*.json")):
        if path.name == "index.json":
            continue
        document = load_json(path)
        material_id = str(document.get("id") or path.stem)
        flags[material_id] = {
            str(flag) for flag in (document.get("generation_flags") or [])
        }
    return flags


def load_gated_forms(gate: dict[str, Any]) -> dict[str, set[str]]:
    gated: dict[str, set[str]] = {}
    for material_id, forms in (gate.get("materials") or {}).items():
        gated[str(material_id)] = {str(form) for form in forms}
    return gated


def classify_pair(
    material: str,
    form: str,
    *,
    gated: dict[str, set[str]],
    prefixes: dict[str, dict[str, Any]],
    materials: dict[str, set[str]],
) -> str:
    if form not in prefixes:
        return "no_prefix_json"
    if material not in materials:
        return "no_material_json"
    if form in gated.get(material, set()):
        return "already_gated"
    flag = prefixes[form].get("generation_flag")
    if flag and flag in materials[material]:
        return "openable_flagged"
    return "openable_dump_proven"


def current_gap_files() -> list[Path]:
    files: list[Path] = []
    for root in (
        ROOT / "tools" / "waves" / "prep",
        ROOT / "tools" / "waves" / "content",
    ):
        files.extend(sorted(root.rglob("current_gap.json")))
    return files


def decision_for_dump_prefix(
    gt_prefixes: Iterable[str],
    form: str,
) -> str | None:
    values = {str(value).lower() for value in gt_prefixes if value}
    values.add(str(form).lower())
    if any("crate" in value for value in values):
        return "crate_packaging"
    if any(value.startswith("bulletgt") or value.startswith("bullet_") for value in values):
        return "bullet_forms"
    storage_variants = {
        "storage.raw",
        "storage.gem",
        "storage.plategem",
        "blockraw",
        "blockgem",
        "blockplategem",
        "block_raw",
        "block_gem",
        "block_plate_gem",
        "raw_storage_block",
        "gem_storage_block",
        "plate_gem_storage_block",
    }
    if values & storage_variants:
        return "storage_variant"
    return None


def add_demand(
    demand: dict[tuple[str, str], dict[str, Any]],
    material: str,
    form: str,
    *,
    source: str,
    gt: str | None = None,
    kind: str | None = None,
    reason: str | None = None,
    rows: int = 0,
    maps: dict[str, int] | None = None,
    decision: str | None = None,
) -> None:
    key = (material, form)
    row = demand.setdefault(
        key,
        {
            "material": material,
            "form": form,
            "item": f"cruciblecraft:{material}/{form}",
            "sources": [],
            "gt": [],
            "kinds": [],
            "reasons": [],
            "rows": 0,
            "maps": {},
            "decision_reasons": [],
        },
    )
    if source not in row["sources"]:
        row["sources"].append(source)
    if gt and gt not in row["gt"]:
        row["gt"].append(gt)
    if kind and kind not in row["kinds"]:
        row["kinds"].append(kind)
    if reason and reason not in row["reasons"]:
        row["reasons"].append(reason)
    row["rows"] += int(rows or 0)
    for map_name, count in (maps or {}).items():
        row["maps"][str(map_name)] = (
            int(row["maps"].get(str(map_name), 0)) + int(count or 0)
        )
    if decision and decision not in row["decision_reasons"]:
        row["decision_reasons"].append(decision)


def collect_dump_demand(
    demand: dict[tuple[str, str], dict[str, Any]],
) -> tuple[int, int]:
    document = load_json(FORM_DEMAND)
    pair_count = 0
    row_count = 0
    source = io.relative(FORM_DEMAND)
    for entry in document.get("pairs") or []:
        material = str(entry.get("cc_material") or "")
        form = str(entry.get("cc_form") or "")
        if not material or not form:
            continue
        gt_prefixes = entry.get("gt_prefix") or []
        if isinstance(gt_prefixes, str):
            gt_prefixes = [gt_prefixes]
        rows = int(entry.get("rows") or 0)
        maps = {
            str(name): int(count)
            for name, count in (entry.get("maps") or {}).items()
        }
        decision = decision_for_dump_prefix(gt_prefixes, form)
        for gt_prefix in gt_prefixes:
            add_demand(
                demand,
                material,
                form,
                source=source,
                gt=str(gt_prefix),
                kind="dump_missing_material_form",
                reason="missing_material_form",
                rows=rows if gt_prefix == gt_prefixes[0] else 0,
                maps=maps if gt_prefix == gt_prefixes[0] else None,
                decision=decision,
            )
        pair_count += 1
        row_count += rows
    return pair_count, row_count


def collect_gap_demand(
    demand: dict[tuple[str, str], dict[str, Any]],
    not_form: Counter[str],
) -> None:
    for path in current_gap_files():
        document = load_json(path)
        source = io.relative(path)
        for row in document.get("rows") or []:
            missing = row.get("missing") or []
            if not isinstance(missing, list):
                continue
            for miss in missing:
                if not isinstance(miss, dict):
                    continue
                reason = str(
                    miss.get("reason") or miss.get("resolve_status") or "unknown"
                )
                if reason != "missing_form":
                    not_form[reason] += 1
                    continue
                parsed = parse_cc_form(miss.get("cc"))
                if parsed is None:
                    not_form["missing_form_unparsed"] += 1
                    continue
                material, form = parsed
                add_demand(
                    demand,
                    material,
                    form,
                    source=source,
                    gt=miss.get("gt"),
                    kind=miss.get("kind"),
                    reason=reason,
                )


def copper_family_members() -> list[str]:
    members = ["copper"]
    try:
        from tools import gt6_resolve
    except Exception:
        return members
    resolved = gt6_resolve.resolve_material("Cu", family=True)
    for member in resolved.get("cc_materials") or []:
        if member not in members:
            members.append(str(member))
    return members


def collect_catalog_demand(
    demand: dict[tuple[str, str], dict[str, Any]],
    catalog_not_prefix: list[str],
) -> None:
    catalog = load_json(CATALOG)
    for item in catalog.get("entries") or []:
        if item.get("root_cause_class") != "missing_form":
            continue
        if item.get("status") not in {"open", "partial"}:
            continue
        blocker_id = str(item.get("id") or "")
        if blocker_id == COPPER_FAMILY_BLOCKER:
            source = f"tools/blockers/catalog.json#{blocker_id}"
            for material in copper_family_members():
                for form in COPPER_FAMILY_FORMS:
                    add_demand(
                        demand,
                        material,
                        form,
                        source=source,
                        gt=f"plateCurved/plateDouble({material})",
                        kind="prefix_material",
                        reason="missing_form",
                    )
            continue
        catalog_not_prefix.append(blocker_id)


def count_ungated_generated(
    gated: dict[str, set[str]],
    prefixes: dict[str, dict[str, Any]],
    materials: dict[str, set[str]],
) -> int:
    count = 0
    for material_id, flags in materials.items():
        live = gated.get(material_id, set())
        for form, prefix in prefixes.items():
            flag = prefix.get("generation_flag")
            if flag and flag in flags and form not in live:
                count += 1
    return count


def build_document() -> dict[str, Any]:
    gate = load_json(GATE)
    prefixes = load_prefixes()
    materials = load_material_flags()
    gated = load_gated_forms(gate)
    demand: dict[tuple[str, str], dict[str, Any]] = {}
    not_form: Counter[str] = Counter()
    catalog_not_prefix: list[str] = []
    dump_demand_pairs, dump_demand_rows = collect_dump_demand(demand)
    collect_gap_demand(demand, not_form)
    collect_catalog_demand(demand, catalog_not_prefix)

    from tools import gt6_resolve

    openable: list[dict[str, Any]] = []
    deferred_by_decision: list[dict[str, Any]] = []
    gated_unresolved: list[dict[str, Any]] = []
    already_gated_live = 0
    skipped: Counter[str] = Counter()
    for key in sorted(demand):
        row = demand[key]
        status = classify_pair(
            row["material"],
            row["form"],
            gated=gated,
            prefixes=prefixes,
            materials=materials,
        )
        payload = {
            "form": row["form"],
            "gt": sorted(row["gt"]),
            "item": row["item"],
            "material": row["material"],
            "sources": sorted(row["sources"]),
        }
        if row["rows"]:
            payload["rows"] = row["rows"]
        if row["maps"]:
            payload["maps"] = dict(sorted(row["maps"].items()))
        decisions = sorted(set(row["decision_reasons"]))
        if decisions:
            payload["reason"] = decisions
            deferred_by_decision.append(payload)
        elif status in {"openable_flagged", "openable_dump_proven"}:
            payload["generation"] = (
                "flagged" if status == "openable_flagged" else "dump_proven"
            )
            openable.append(payload)
        elif status == "already_gated":
            if gt6_resolve.form_exists(row["material"], row["form"]):
                already_gated_live += 1
            else:
                gated_unresolved.append(payload)
        else:
            skipped[status] += 1

    form_counts = Counter(row["form"] for row in openable)
    form_rows = Counter(
        row["form"] for row in openable for _ in range(int(row.get("rows") or 0))
    )
    unresolved_counts = Counter(row["form"] for row in gated_unresolved)
    return {
        "already_gated_live": already_gated_live,
        "capability_slug": SLUG,
        "catalog_not_prefix": sorted(set(catalog_not_prefix)),
        "counts": {
            "already_gated_live": already_gated_live,
            "demand_pairs": len(demand),
            "dump_demand_pairs": dump_demand_pairs,
            "dump_demand_rows": dump_demand_rows,
            "deferred_by_decision": len(deferred_by_decision),
            "gated_unresolved": len(gated_unresolved),
            "not_form": int(sum(not_form.values())),
            "openable": len(openable),
            "skipped": int(sum(skipped.values())),
            "ungated_generated_flag_pairs": count_ungated_generated(
                gated, prefixes, materials
            ),
        },
        "deferred_by_decision": deferred_by_decision,
        "gated_unresolved": gated_unresolved,
        "gated_unresolved_by_form": {
            form: unresolved_counts[form] for form in sorted(unresolved_counts)
        },
        "lane": "prep",
        "not_form_by_reason": dict(sorted(not_form.items())),
        "note": (
            "Safety valve unchanged: recipe cards still must not stand in. "
            "openable is ungated demand. gated_unresolved is already in "
            "gate.materials but gt6_resolve.form_exists is false (hybrid "
            "long-tail lookup). Do not re-gate those. Do not open "
            "ungated_generated_flag_pairs."
        ),
        "openable": openable,
        "openable_by_form": {
            form: form_counts[form] for form in sorted(form_counts)
        },
        "openable_rows_by_form": {
            form: form_rows[form] for form in sorted(form_rows)
        },
        "schema_version": 1,
        "skipped_demand": dict(sorted(skipped.items())),
        "status": "MATERIAL_FORM_DEMAND_CENSUS",
    }


def write_census(document: dict[str, Any] | None = None) -> dict[str, Any]:
    payload = document if document is not None else build_document()
    atomic_io.write_json(OUT, stable_json(payload))
    return payload


def check_census() -> dict[str, Any]:
    expected = stable_json(build_document())
    if not OUT.is_file():
        raise SystemExit(f"missing {io.relative(OUT)}; run --write")
    actual = OUT.read_text(encoding="utf-8")
    if actual.replace("\r\n", "\n") != expected:
        raise SystemExit(f"{io.relative(OUT)} drifted; run --write")
    return json.loads(expected)


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--write", action="store_true")
    group.add_argument("--check", action="store_true")
    args = parser.parse_args(list(argv) if argv is not None else None)
    if args.write:
        document = write_census()
    else:
        document = check_census()
    counts = document["counts"]
    print(
        "openable",
        counts["openable"],
        "demand",
        counts["demand_pairs"],
        "not_form",
        counts["not_form"],
        "ungated_generated",
        counts["ungated_generated_flag_pairs"],
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
