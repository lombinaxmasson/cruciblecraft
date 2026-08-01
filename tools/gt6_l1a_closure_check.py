#!/usr/bin/env python3
"""L1a — closure check only (no keep/drop filtering).

Feeds all dump materials into four graphs (components, targets, byProducts,
recipe references) and reports dangling refs + composition cycles. This is the
dump-integrity gate before L1b selection rules.
"""
from __future__ import annotations

import json
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
OD = ROOT / "gt6_dump" / "gt6_recipe_dump" / "oredict"
MAPS = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps"
OUT = ROOT / "tools" / "gt6_l1a_closure_report.json"
TOOLS = ROOT / "tools"

sys.path.insert(0, str(TOOLS))
from gt6_structural_class import (  # noqa: E402
    STRUCTURAL_CLASSES,
    atomic_number,
    element_completeness,
    is_element_like,
    structural_class,
)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def mat_key(material_name: str, material_id: int | None) -> str:
    if isinstance(material_id, int) and material_id >= 0:
        return f"id:{material_id}"
    return f"name:{material_name}"


def build_universe(materials: list[dict[str, Any]]) -> tuple[dict[str, dict], dict[str, str]]:
    by_key: dict[str, dict] = {}
    name_to_key: dict[str, str] = {}
    for m in materials:
        mid = m.get("id")
        name = m.get("nameInternal") or ""
        key = mat_key(name, mid if isinstance(mid, int) else -1)
        by_key[key] = m
        if name:
            # Prefer positive-id binding when names collide with stubs.
            if name not in name_to_key or (isinstance(mid, int) and mid >= 0):
                name_to_key[name] = key
    return by_key, name_to_key


def resolve(name: str, material_id: int | None, name_to_key: dict[str, str]) -> str | None:
    if isinstance(material_id, int) and material_id >= 0:
        return f"id:{material_id}"
    if name in name_to_key:
        return name_to_key[name]
    if name:
        return f"name:{name}"
    return None


def collect_component_edges(
    materials: list[dict[str, Any]], name_to_key: dict[str, str]
) -> dict[str, list[dict[str, Any]]]:
    graph: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for m in materials:
        src = mat_key(m.get("nameInternal") or "", m.get("id") if isinstance(m.get("id"), int) else -1)
        comps = m.get("components")
        entries = []
        if isinstance(comps, dict):
            entries = comps.get("components") or []
        elif isinstance(comps, list):
            entries = comps
        for entry in entries:
            if not isinstance(entry, dict):
                continue
            dst = resolve(entry.get("material") or "", entry.get("materialId"), name_to_key)
            if dst:
                graph[src].append({
                    "to": dst,
                    "amount": entry.get("amount"),
                    "material": entry.get("material"),
                    "materialId": entry.get("materialId"),
                })
    return graph


def collect_target_edges(
    materials: list[dict[str, Any]], name_to_key: dict[str, str]
) -> dict[str, list[dict[str, Any]]]:
    graph: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for m in materials:
        src = mat_key(m.get("nameInternal") or "", m.get("id") if isinstance(m.get("id"), int) else -1)
        targets = m.get("targets") or {}
        if not isinstance(targets, dict):
            continue
        for tname, entry in targets.items():
            if not isinstance(entry, dict):
                continue
            dst = resolve(entry.get("material") or "", entry.get("materialId"), name_to_key)
            if dst:
                graph[src].append({
                    "to": dst,
                    "target": tname,
                    "amount": entry.get("amount"),
                    "material": entry.get("material"),
                    "materialId": entry.get("materialId"),
                })
    return graph


def collect_byproduct_edges(
    materials: list[dict[str, Any]], name_to_key: dict[str, str]
) -> dict[str, list[dict[str, Any]]]:
    graph: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for m in materials:
        src = mat_key(m.get("nameInternal") or "", m.get("id") if isinstance(m.get("id"), int) else -1)
        byproducts = m.get("byProducts")
        if byproducts is None:
            continue
        entries = byproducts if isinstance(byproducts, list) else []
        if isinstance(byproducts, dict):
            entries = list(byproducts.values())
        for entry in entries:
            if isinstance(entry, str):
                dst = resolve(entry, None, name_to_key)
                if dst:
                    graph[src].append({"to": dst, "material": entry})
            elif isinstance(entry, dict):
                dst = resolve(entry.get("material") or "", entry.get("materialId"), name_to_key)
                if dst:
                    graph[src].append({
                        "to": dst,
                        "material": entry.get("material"),
                        "materialId": entry.get("materialId"),
                    })
    return graph


def collect_recipe_refs(name_to_key: dict[str, str], by_key: dict[str, dict]) -> dict[str, int]:
    """Count recipe item.meta hits that resolve (or fail) against material ids."""
    known_ids = {int(k[3:]) for k in by_key if k.startswith("id:")}
    hits: dict[str, int] = defaultdict(int)
    dangling_meta: dict[int, int] = defaultdict(int)
    scanned = 0
    for path in sorted(MAPS.glob("*.json")):
        data = load(path)
        recipes = data.get("recipes") if isinstance(data, dict) else None
        if not isinstance(recipes, list):
            continue
        for recipe in recipes:
            if not isinstance(recipe, dict) or recipe.get("enabled") is False:
                continue
            scanned += 1
            for side in ("inputs", "outputs"):
                for item in recipe.get(side) or []:
                    if not isinstance(item, dict):
                        continue
                    meta = item.get("meta")
                    item_id = str(item.get("item") or "")
                    if not isinstance(meta, int):
                        continue
                    if not item_id.startswith("gregtech:gt.meta."):
                        continue
                    if meta < 0:
                        raise ValueError(
                            "recipe material reference cannot use negative "
                            f"meta {meta}: {path.name} {side}"
                        )
                    if meta in known_ids:
                        hits[f"id:{meta}"] += 1
                    else:
                        dangling_meta[meta] += 1
    return {
        "recipes_scanned": scanned,
        "resolved_meta_refs": sum(hits.values()),
        "distinct_materials_touched": len(hits),
        "dangling_meta_count": sum(dangling_meta.values()),
        "dangling_meta_distinct": len(dangling_meta),
        "dangling_meta_sample": sorted(dangling_meta.items(), key=lambda kv: -kv[1])[:30],
    }


def dangling_edges(
    graph: dict[str, list[dict[str, Any]]], universe: set[str]
) -> list[dict[str, Any]]:
    bad = []
    for src, edges in graph.items():
        if src not in universe:
            continue
        for edge in edges:
            dst = edge["to"]
            if dst not in universe:
                bad.append({"from": src, **edge, "reason": "target_not_in_universe"})
            elif edge.get("materialId") == -1 and dst.startswith("name:"):
                # name-only stub targets are in universe but flagged for review
                pass
    return bad


def edge_identity_stats(
    graph: dict[str, list[dict[str, Any]]],
) -> dict[str, Any]:
    """Separate stable-id closure from name-only compatibility records."""
    counts: Counter[str] = Counter()
    stable_to_name_only = []
    for source, edges in graph.items():
        source_kind = "stable_id" if source.startswith("id:") else "name_only"
        for edge in edges:
            target = edge["to"]
            target_kind = (
                "stable_id" if target.startswith("id:") else "name_only"
            )
            counts[f"{source_kind}_to_{target_kind}"] += 1
            if source_kind == "stable_id" and target_kind == "name_only":
                stable_to_name_only.append({
                    "from": source,
                    **edge,
                })
    return {
        "edge_source_x_target_identity": {
            key: counts[key]
            for key in (
                "stable_id_to_stable_id",
                "stable_id_to_name_only",
                "name_only_to_stable_id",
                "name_only_to_name_only",
            )
        },
        "stable_source_to_name_only_target_count": len(
            stable_to_name_only
        ),
        "stable_source_to_name_only_target_sample": (
            stable_to_name_only[:50]
        ),
    }


def find_cyclic_materials(component_graph: dict[str, list[dict[str, Any]]]) -> list[list[str]]:
    """Port of MaterialLoader.findCyclicMaterials / collectCycles (composition only)."""
    adj = {src: [e["to"] for e in edges] for src, edges in component_graph.items()}
    states: dict[str, int] = {}
    path: list[str] = []
    cycles: list[list[str]] = []
    seen_cycle_keys: set[tuple[str, ...]] = set()

    def collect(node: str) -> None:
        states[node] = 1
        path.append(node)
        for nxt in adj.get(node, []):
            if nxt not in adj and nxt not in states:
                continue
            state = states.get(nxt, 0)
            if state == 0:
                collect(nxt)
            elif state == 1:
                start = path.index(nxt)
                cycle = path[start:]
                key = tuple(sorted(cycle))
                if key not in seen_cycle_keys:
                    seen_cycle_keys.add(key)
                    cycles.append(cycle[:])
        path.pop()
        states[node] = 2

    for node in sorted(adj):
        if states.get(node, 0) == 0:
            collect(node)
    return cycles


def remove_invalid_report(
    component_graph: dict[str, list[dict[str, Any]]], universe: set[str]
) -> dict[str, Any]:
    """Simulate MaterialLoader.removeInvalidReferences do/while without mutating dump."""
    alive = set(universe)
    rounds: list[dict[str, Any]] = []
    changed = True
    while changed:
        invalid: list[str] = []
        reasons: dict[str, str] = {}
        for src in sorted(alive):
            for edge in component_graph.get(src, []):
                dst = edge["to"]
                self_ref = dst == src
                missing = dst not in alive
                if self_ref or missing:
                    invalid.append(src)
                    reasons[src] = (
                        "self_component" if self_ref else f"missing_component:{dst}"
                    )
                    break
        if not invalid:
            cycles = find_cyclic_materials(
                {k: v for k, v in component_graph.items() if k in alive}
            )
            cyclic_nodes = sorted({n for cycle in cycles for n in cycle})
            for node in cyclic_nodes:
                reasons[node] = "composition_cycle"
            invalid = cyclic_nodes
            if cycles:
                rounds.append({
                    "removed": cyclic_nodes,
                    "reasons": reasons,
                    "cycles": cycles,
                })
        else:
            rounds.append({"removed": invalid, "reasons": reasons, "cycles": []})
        for node in invalid:
            alive.discard(node)
        changed = bool(invalid)
    return {
        "rounds": len(rounds),
        "removed_total": sum(len(r["removed"]) for r in rounds),
        "remaining": len(alive),
        "detail": rounds[:20],
    }


def main() -> int:
    if not (OD / "materials.json").is_file():
        print(f"Missing dump materials at {OD}", file=sys.stderr)
        return 1
    materials = load(OD / "materials.json")
    if not isinstance(materials, list):
        print("materials.json must be a list", file=sys.stderr)
        return 1

    by_key, name_to_key = build_universe(materials)
    universe = set(by_key)
    if "id:-1" in universe:
        raise ValueError("sentinel id=-1 escaped name-only identity normalization")
    components = collect_component_edges(materials, name_to_key)
    targets = collect_target_edges(materials, name_to_key)
    byproducts = collect_byproduct_edges(materials, name_to_key)
    recipe_stats = collect_recipe_refs(name_to_key, by_key)
    component_identity = edge_identity_stats(components)
    target_identity = edge_identity_stats(targets)
    byproduct_identity = edge_identity_stats(byproducts)

    comp_nonempty = sum(1 for edges in components.values() if edges)
    dangling_components = dangling_edges(components, universe)
    dangling_targets = dangling_edges(targets, universe)
    dangling_byproducts = dangling_edges(byproducts, universe)
    cycles = find_cyclic_materials(components)
    invalid_sim = remove_invalid_report(components, universe)

    # id=-1 targets/components that only resolve by name
    stub_component_refs = [
        e for edges in components.values() for e in edges
        if isinstance(e.get("materialId"), int) and e["materialId"] < 0
    ]

    # Derived structural_class (id-space encoding) + element completeness.
    class_counts: Counter[str] = Counter()
    element_like_ids: list[int] = []
    for m in materials:
        mid = m.get("id")
        sc = structural_class(mid if isinstance(mid, int) else -1)
        class_counts[sc] += 1
        if is_element_like(mid):
            element_like_ids.append(int(mid))
    fictional = [
        m for m in materials
        if structural_class(m.get("id")) == "fictional_material"
    ]
    transuranium = [
        m for m in materials
        if isinstance(m.get("id"), int) and 1000 <= m["id"] <= 1180
    ]
    completeness = element_completeness(
        [
            {
                "source_id": m.get("id"),
                "source_name": m.get("nameInternal"),
                "original_mod": m.get("originalMod"),
            }
            for m in materials
        ],
        id_field="source_id",
        z_min=1,
        z_max=118,
    )
    element_nuclear_mismatches = []
    isotope_nuclear_mismatches = []
    nuclear_fields_outside_identity_classes = 0
    for m in materials:
        mid = m.get("id")
        sc = structural_class(mid)
        protons = m.get("protons")
        neutrons = m.get("neutrons")
        electrons = m.get("electrons")
        mass = m.get("mass")
        if sc == "element":
            expected_z = atomic_number(mid)
            if (
                expected_z is None
                or protons != expected_z
                or electrons != protons
                or mass != protons + neutrons
            ):
                element_nuclear_mismatches.append({
                    "id": mid,
                    "name": m.get("nameInternal"),
                    "expected_z": expected_z,
                    "protons": protons,
                    "neutrons": neutrons,
                    "electrons": electrons,
                    "mass": mass,
                })
        elif sc == "isotope" and isinstance(mid, int) and mid >= 10:
            expected_z = mid // 10
            if (
                protons != expected_z
                or electrons != protons
                or mass != protons + neutrons
            ):
                isotope_nuclear_mismatches.append({
                    "id": mid,
                    "name": m.get("nameInternal"),
                    "expected_z": expected_z,
                    "protons": protons,
                    "neutrons": neutrons,
                    "electrons": electrons,
                    "mass": mass,
                })
        elif isinstance(protons, int) and protons > 0:
            # Nuclear totals exist on compounds/fictional materials too; this
            # proves protons>0 is not itself an identity classifier.
            nuclear_fields_outside_identity_classes += 1

    report = {
        "schema_version": 2,
        "universe": {
            "materials": len(materials),
            "keys": len(by_key),
            "positive_id": sum(1 for k in by_key if k.startswith("id:")),
            "name_only_stubs": sum(1 for k in by_key if k.startswith("name:")),
            "sentinel_identity": "name:<nameInternal>",
            "negative_id_lookup_keys_allowed": False,
        },
        "structural_class": {
            "counts": {k: class_counts[k] for k in STRUCTURAL_CLASSES},
            "id_range_semantics": {
                "transuranium_1000_1180_total": len(transuranium),
                "verified_periodic_elements_z_100_118": sum(
                    1 for m in transuranium if is_element_like(m.get("id"))
                ),
                "transuranium_isotopes": sum(
                    1 for m in transuranium if not is_element_like(m.get("id"))
                ),
                "fictional_material_1181_7999": len(fictional),
                "note": (
                    "Raw protons verify ids 1000..1180 as Z=100..118 plus isotope "
                    "Flerovium298(1148). Full-range query classifies 1181..7999 as "
                    "fictional/sci-fi/magic/joke material ids."
                ),
            },
            "nuclear_identity_cross_check": {
                "element_mismatch_count": len(element_nuclear_mismatches),
                "element_mismatches": element_nuclear_mismatches[:20],
                "isotope_mismatch_count": len(isotope_nuclear_mismatches),
                "isotope_mismatches": isotope_nuclear_mismatches[:20],
                "protons_positive_outside_element_or_isotope": (
                    nuclear_fields_outside_identity_classes
                ),
                "ok": not element_nuclear_mismatches and not isotope_nuclear_mismatches,
                "note": (
                    "Nuclear fields cross-validate identity candidates; they cannot "
                    "classify identity alone because compounds and fictional materials "
                    "also carry aggregate proton/neutron/electron/mass values."
                ),
            },
            "element_completeness_z_1_118": completeness,
        },
        "graphs": {
            "components": {
                "nodes_with_edges": comp_nonempty,
                "edge_count": sum(len(v) for v in components.values()),
                "dangling": dangling_components[:50],
                "dangling_count": len(dangling_components),
                "stub_materialId_negative_refs": len(stub_component_refs),
                **component_identity,
            },
            "targets": {
                "nodes_with_edges": sum(1 for v in targets.values() if v),
                "edge_count": sum(len(v) for v in targets.values()),
                "dangling": dangling_targets[:50],
                "dangling_count": len(dangling_targets),
                **target_identity,
            },
            "byProducts": {
                "nodes_with_edges": sum(1 for v in byproducts.values() if v),
                "edge_count": sum(len(v) for v in byproducts.values()),
                "dangling": dangling_byproducts[:50],
                "dangling_count": len(dangling_byproducts),
                **byproduct_identity,
            },
            "recipe_refs": recipe_stats,
        },
        "cycles": {
            "count": len(cycles),
            "samples": cycles[:20],
        },
        "removeInvalidReferences_simulation": invalid_sim,
        "closure_violations": {
            "dangling_components": len(dangling_components),
            "dangling_targets": len(dangling_targets),
            "dangling_byproducts": len(dangling_byproducts),
            "composition_cycles": len(cycles),
            "recipe_dangling_meta_distinct": recipe_stats["dangling_meta_distinct"],
        },
        "verdict": (
            "PASS"
            if (
                comp_nonempty > 0
                and not dangling_components
                and not dangling_targets
                and not dangling_byproducts
                and not cycles
                and completeness["ok"]
                and not element_nuclear_mismatches
                and not isotope_nuclear_mismatches
            )
            else "FAIL_or_REVIEW"
        ),
    }
    OUT.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(f"Wrote {OUT}")
    print("universe", report["universe"])
    print("structural_class", report["structural_class"]["counts"])
    print("id range semantics", report["structural_class"]["id_range_semantics"])
    print(
        "nuclear identity cross-check",
        report["structural_class"]["nuclear_identity_cross_check"],
    )
    print(
        "element_completeness",
        "ok=" + str(completeness["ok"]),
        "present",
        completeness["present_count"],
        "missing",
        completeness["missing_count"],
        completeness["missing_z"][:20],
    )
    print("components nonempty nodes", comp_nonempty, "edges", report["graphs"]["components"]["edge_count"])
    print("closure_violations", report["closure_violations"])
    print("verdict", report["verdict"])
    return 0 if report["verdict"] == "PASS" else 2


if __name__ == "__main__":
    raise SystemExit(main())
