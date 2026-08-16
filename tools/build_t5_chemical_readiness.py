#!/usr/bin/env python3
"""Build the authoritative T5a chemical-readiness ledger."""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
MATERIAL_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
MATERIAL_INDEX = MATERIAL_ROOT / "index.json"
REGISTRATION_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
)
ORE_CLOSURE = TOOLS / "gt6_ore_chain_closure.json"
NORMALIZED_MATERIALS = TOOLS / "gt6_oredict_materials_normalized.json"
NORMALIZED_FLUIDS = TOOLS / "gt6_oredict_fluids_normalized.json"
MAP_ROADMAP = TOOLS / "gt6_map_roadmap.json"
TEMPLATE_INDEX = TOOLS / "gt6_recipe_templates_index.json"
TEMPLATE_REPORT = TOOLS / "gt6_recipe_templates_report.json"
POLICY = TOOLS / "t5_chemical_policy.json"
GITIGNORE = ROOT / ".gitignore"
DUMP_INDEX = ROOT / "gt6_dump/gt6_recipe_dump/index.json"
T13_RECIPE_MAPS = TOOLS / "t13_denominators/recipe_maps.json"
OUTPUT = TOOLS / "t5_chemical_readiness.json"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def directory_sha256(root: Path) -> str:
    entries = [
        (path.relative_to(root).as_posix(), sha256(path))
        for path in sorted(root.rglob("*.json"))
    ]
    payload = json.dumps(
        entries,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def load_material_documents() -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for filename in load(MATERIAL_INDEX):
        document = load(MATERIAL_ROOT / filename)
        material = str(document["id"])
        if material in result:
            raise ValueError(f"duplicate material id {material}")
        result[material] = document
    return result


def material_path(material: str) -> str:
    return (
        "src/main/resources/data/cruciblecraft/materials/"
        f"{material}.json"
    )


def evidence(path: str, field: str, value: Any) -> dict[str, Any]:
    return {"path": path, "field": field, "value": value}


def normalized_material_indexes(
    records: list[dict[str, Any]],
) -> tuple[dict[int, dict[str, Any]], dict[str, dict[str, Any]]]:
    by_id: dict[int, dict[str, Any]] = {}
    by_name: dict[str, dict[str, Any]] = {}
    for record in records:
        source_id = int(record["source_id"])
        source_name = str(record["source_name"])
        if source_id >= 0:
            if source_id in by_id:
                raise ValueError(f"duplicate stable GT6 material id {source_id}")
            by_id[source_id] = record
        by_name.setdefault(source_name, record)
    return by_id, by_name


def source_key(source_id: int, source_name: str) -> tuple[str, int | str]:
    if source_id >= 0:
        return ("id", source_id)
    return ("name", source_name)


def normalized_for_document(
    document: dict[str, Any],
    by_id: dict[int, dict[str, Any]],
    by_name: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    metadata = document.get("gt6_metadata") or {}
    source_id = int(metadata.get("source_id", -1))
    source_name = str(metadata.get("source_name") or "")
    result = by_id.get(source_id) if source_id >= 0 else None
    result = result or by_name.get(source_name)
    if result is None:
        raise ValueError(
            f"{document['id']}: normalized GT6 material source is missing"
        )
    return result


def chemical_domain(
    closure: dict[str, Any],
    expected: dict[str, int],
) -> tuple[
    list[str],
    list[str],
    list[str],
    dict[str, list[str]],
]:
    terminal = sorted(
        str(row["material"])
        for row in closure["sifter_dust_without_smelter"]
        if row["classification"] == "t5_chemical"
    )
    crusher = sorted(
        str(row["material"])
        for row in closure["crusher_without_worldgen"]
        if row["classification"] == "t5_chemical"
    )
    byproduct = sorted(
        str(row["material"])
        for row in closure["crusher_without_worldgen"]
        if row["classification"] == "byproduct_only"
    )
    terminal_set = set(terminal)
    crusher_set = set(crusher)
    origins = {
        material: [
            origin
            for origin, members in (
                ("terminal_dust", terminal_set),
                ("crusher", crusher_set),
            )
            if material in members
        ]
        for material in sorted(terminal_set | crusher_set)
    }
    actual = {
        "terminal_dust_t5_chemical": len(terminal),
        "crusher_t5_chemical": len(crusher),
        "chemical_overlap": len(terminal_set & crusher_set),
        "unique_chemical_union": len(origins),
        "byproduct_only_debt": len(byproduct),
    }
    for key, value in actual.items():
        if value != int(expected[key]):
            raise ValueError(
                f"T5a {key} drifted: expected {expected[key]}, got {value}"
            )
    return terminal, crusher, byproduct, origins


def route_destinations(
    tags: set[str],
    policy: dict[str, Any],
    roadmap: dict[str, Any],
) -> list[dict[str, Any]]:
    destinations = []
    for tag, route in sorted(
        policy["loader_semantics"]["route_tags"].items()
    ):
        if tag not in tags:
            continue
        map_id = str(route["destination_map"])
        roadmap_row = roadmap["maps"].get(map_id)
        if not isinstance(roadmap_row, dict):
            raise ValueError(f"{map_id}: missing map-roadmap row")
        destinations.append({
            "map": map_id,
            "source_tag": tag,
            "source_condition": route["source_condition"],
            "roadmap_status": roadmap_row["status"],
            "roadmap_phase": roadmap_row["phase"],
            "roadmap_reference_recipe_count": roadmap_row[
                "reference_recipe_count"
            ],
            "roadmap_scope": (
                "Current roadmap status is implementation context only; it "
                "does not prove Loader_Recipes_Decomp replay coverage."
            ),
            "reason": route["reason"],
            "source": {
                "repository": policy["gt6_source"]["repository"],
                "revision": policy["gt6_source"]["revision"],
                **route["source"],
            },
        })
    return destinations


def classify_material(
    material: str,
    origins: list[str],
    document: dict[str, Any],
    normalized: dict[str, Any],
    registered_forms: set[str],
    policy: dict[str, Any],
    roadmap: dict[str, Any],
) -> dict[str, Any]:
    metadata = document.get("gt6_metadata") or {}
    tags = set(str(tag) for tag in metadata.get("material_tags") or [])
    normalized_tags = set(
        str(tag) for tag in normalized.get("material_tags") or []
    )
    if tags != normalized_tags:
        raise ValueError(f"{material}: runtime/normalized GT6 tags drifted")
    composition = document.get("composition") or {}
    components = normalized.get("components") or []
    components_present = bool(components)
    if bool(composition) != components_present:
        raise ValueError(
            f"{material}: runtime composition/normalized components mismatch"
        )
    divider = normalized.get("component_common_divider")
    divider_known = (
        isinstance(divider, int)
        and not isinstance(divider, bool)
    )
    divider_lte_64 = divider_known and int(divider) <= int(
        policy["loader_semantics"]["common_divider_gate"]["maximum"]
    )
    decomposable = (
        policy["loader_semantics"]["decomposable_gate"]["tag"] in tags
    )
    destinations = route_destinations(tags, policy, roadmap)
    route_tagged = bool(destinations)
    conjunction = {
        "components_present": components_present,
        "decomposable_tag": decomposable,
        "component_common_divider_known": divider_known,
        "component_common_divider_lte_64": divider_lte_64,
        "at_least_one_route_tag": route_tagged,
    }
    source_loader_eligible = all(conjunction.values())
    no_decompose = bool(document.get("no_decompose"))

    if source_loader_eligible and no_decompose:
        classification = "route_tagged_but_quarantined"
        reason_code = "CC_NO_DECOMPOSE_QUARANTINE"
        reason = (
            "All pinned Loader_Recipes_Decomp source conditions pass and at "
            "least one source route tag selects a destination map, but the "
            "CrucibleCraft no_decompose gate quarantines runtime projection. "
            "The gate is not counted as a route."
        )
    elif source_loader_eligible:
        classification = "route_ready"
        reason_code = "SOURCE_CONJUNCTION_PASSES"
        reason = (
            "Components, DECOMPOSABLE, the known commonDivider <= 64, and at "
            "least one source route tag satisfy Loader_Recipes_Decomp; the "
            "CrucibleCraft quarantine gate is clear. Authoritative recipe-map "
            "replay is still required before implementation is declared ready."
        )
    elif (
        components_present
        and decomposable
        and divider_known
        and divider_lte_64
        and not route_tagged
    ):
        classification = "decomposable_without_route"
        reason_code = "NO_CENTRIFUGE_OR_ELECTROLYZER_TAG"
        reason = (
            "Factual components, DECOMPOSABLE, and commonDivider <= 64 are "
            "present, but neither source route tag selects centrifuge or "
            "electrolyzer. no_decompose is not used as a route substitute."
        )
    else:
        classification = "unresolved_deferred"
        failed = [key for key, passed in conjunction.items() if not passed]
        reason_code = "LOADER_CONJUNCTION_INCOMPLETE"
        reason = (
            "The pinned Loader_Recipes_Decomp conjunction is incomplete: "
            + ", ".join(failed)
            + ". No route is inferred from no_decompose or unrelated tags."
        )

    source_id = int(metadata.get("source_id", -1))
    source_name = str(metadata.get("source_name") or "")
    source = {
        "repository": policy["gt6_source"]["repository"],
        "revision": policy["gt6_source"]["revision"],
        "loader_path": policy["gt6_source"]["loader_path"],
        "material_path": material_path(material),
        "normalized_material_path": (
            "tools/gt6_oredict_materials_normalized.json"
        ),
        "source_material_id": source_id,
        "source_material_name": source_name,
    }
    return {
        "material": material,
        "origins": origins,
        "classification": classification,
        "reason_code": reason_code,
        "reason": reason,
        "source": source,
        "source_evidence": [
            evidence(
                "tools/gt6_ore_chain_closure.json",
                "classification",
                {"origins": origins, "material": material},
            ),
            evidence(
                material_path(material),
                "composition",
                composition,
            ),
            evidence(
                material_path(material),
                "gt6_metadata.material_tags",
                sorted(tags),
            ),
            evidence(
                material_path(material),
                "no_decompose",
                no_decompose,
            ),
            evidence(
                "tools/gt6_oredict_materials_normalized.json",
                (
                    f"records[source_id={source_id}]"
                    ".component_common_divider"
                ),
                divider,
            ),
            evidence(
                (
                    "src/main/resources/data/cruciblecraft/"
                    "material_registration_gate.json"
                ),
                f"materials.{material}",
                sorted(registered_forms),
            ),
        ],
        "composition": dict(sorted(composition.items())),
        "normalized_components": components,
        "component_common_divider": divider,
        "material_tags": sorted(tags),
        "registered_forms": sorted(registered_forms),
        "no_decompose": no_decompose,
        "loader_conjunction": conjunction,
        "source_loader_eligible": source_loader_eligible,
        "map_destinations": destinations,
        "recipe_evidence": {
            "status": policy["recipe_replay"]["status"],
            "reason": policy["recipe_replay"]["reason"],
            "source": policy["recipe_replay"]["source"],
        },
    }


def byproduct_rows(
    materials: list[str],
    closure: dict[str, Any],
    policy: dict[str, Any],
) -> list[dict[str, Any]]:
    closure_rows = {
        str(row["material"]): row
        for row in closure["crusher_without_worldgen"]
        if row["classification"] == "byproduct_only"
    }
    result = []
    for material in materials:
        row = closure_rows[material]
        result.append({
            "material": material,
            "classification": "byproduct_only_debt",
            "reason": (
                "The ore-chain closure classifies this crusher debt only as a "
                "named processing byproduct. This debt entry is accounted "
                "separately and does not enlarge the 234-material T5 chemical "
                "union; the same material may independently occur in a "
                "terminal-dust chemical debt."
            ),
            "source": {
                "repository": policy["gt6_source"]["repository"],
                "revision": policy["gt6_source"]["revision"],
                "path": "tools/gt6_ore_chain_closure.json",
                "field": "crusher_without_worldgen",
            },
            "source_evidence": row["evidence"],
        })
    return result


def projected_fluid_relations(
    material_rows: list[dict[str, Any]],
    normalized_by_id: dict[int, dict[str, Any]],
    normalized_by_name: dict[str, dict[str, Any]],
    policy: dict[str, Any],
) -> tuple[
    dict[tuple[str, int | str], list[dict[str, Any]]],
    list[dict[str, Any]],
]:
    ambient = float(
        policy["loader_semantics"]["output_phase_rule"]["ambient_kelvin"]
    )
    relations: dict[
        tuple[str, int | str], list[dict[str, Any]]
    ] = defaultdict(list)
    missing_component_sources: list[dict[str, Any]] = []

    for row in material_rows:
        if not row["source_loader_eligible"]:
            continue
        source_id = int(row["source"]["source_material_id"])
        source_name = str(row["source"]["source_material_name"])
        normalized = (
            normalized_by_id.get(source_id)
            if source_id >= 0
            else normalized_by_name.get(source_name)
        )
        if normalized is None:
            raise ValueError(
                f"{row['material']}: source material disappeared"
            )
        if normalized.get("state") in {"liquid", "gas"}:
            relations[source_key(source_id, source_name)].append({
                "kind": "decomposition_input",
                "chemical_material": row["material"],
                "chemical_classification": row["classification"],
                "source_material_id": source_id,
                "source_material_name": source_name,
                "phase": normalized["state"],
                "maps": [
                    route["map"] for route in row["map_destinations"]
                ],
                "reason": (
                    "Loader_Recipes_Decomp tries liquid/gas input forms before "
                    "dust input for a map that accepts fluid input."
                ),
                "source": {
                    "path": policy["gt6_source"]["loader_path"],
                    "lines": "55-79",
                },
            })
        for component in normalized.get("components") or []:
            component_id = int(component["material_id"])
            component_name = str(component["material"])
            target = (
                normalized_by_id.get(component_id)
                if component_id >= 0
                else normalized_by_name.get(component_name)
            )
            if target is None:
                missing_component_sources.append({
                    "chemical_material": row["material"],
                    "component_material_id": component_id,
                    "component_material_name": component_name,
                    "reason": "normalized component material source is missing",
                })
                continue
            melting = target.get("thermal", {}).get(
                "melting_point_kelvin"
            )
            if (
                isinstance(melting, (int, float))
                and not isinstance(melting, bool)
                and math.isfinite(float(melting))
                and float(melting) <= ambient
            ):
                relations[
                    source_key(component_id, component_name)
                ].append({
                    "kind": "decomposition_output",
                    "chemical_material": row["material"],
                    "chemical_classification": row["classification"],
                    "source_material_id": component_id,
                    "source_material_name": component_name,
                    "phase": target["state"],
                    "melting_point_kelvin": melting,
                    "maps": [
                        route["map"] for route in row["map_destinations"]
                    ],
                    "reason": (
                        "Loader_Recipes_Decomp emits a fluid component when "
                        "its melting point is at or below DEF_ENV_TEMP."
                    ),
                    "source": {
                        "path": policy["gt6_source"]["loader_path"],
                        "lines": "47-51",
                    },
                })
    return {
        key: sorted(
            values,
            key=lambda value: (
                value["kind"],
                value["chemical_material"],
                value["source_material_name"],
            ),
        )
        for key, values in relations.items()
    }, missing_component_sources


def fluid_inventory(
    fluid_records: list[dict[str, Any]],
    material_documents: dict[str, dict[str, Any]],
    normalized_by_id: dict[int, dict[str, Any]],
    normalized_by_name: dict[str, dict[str, Any]],
    material_rows: list[dict[str, Any]],
    policy: dict[str, Any],
) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    prefixes = tuple(policy["fluid_inventory"]["molten_id_prefixes"])
    non_molten = sorted(
        (
            record
            for record in fluid_records
            if not str(record["fluid"]).startswith(prefixes)
        ),
        key=lambda record: str(record["fluid"]),
    )
    expected = int(
        policy["ledger_contract"]["expected_counts"][
            "non_molten_fluid_candidates"
        ]
    )
    if len(non_molten) != expected:
        raise ValueError(
            "non-molten fluid candidate count drifted: "
            f"expected {expected}, got {len(non_molten)}"
        )

    cc_by_source_id: dict[int, str] = {}
    cc_by_source_name: dict[str, str] = {}
    for material, document in material_documents.items():
        metadata = document.get("gt6_metadata") or {}
        source_id = int(metadata.get("source_id", -1))
        source_name = str(metadata.get("source_name") or "")
        if source_id >= 0:
            cc_by_source_id[source_id] = material
        cc_by_source_name.setdefault(source_name, material)

    candidates_by_source: dict[
        tuple[str, int | str], list[dict[str, Any]]
    ] = defaultdict(list)
    for record in non_molten:
        candidates_by_source[
            source_key(int(record["material_id"]), str(record["material"]))
        ].append(record)

    relations, missing_component_sources = projected_fluid_relations(
        material_rows,
        normalized_by_id,
        normalized_by_name,
        policy,
    )
    missing_candidates = []
    for key, rows in relations.items():
        if key not in candidates_by_source:
            first = rows[0]
            missing_candidates.append({
                "source_material_id": first["source_material_id"],
                "source_material_name": first["source_material_name"],
                "relations": rows,
                "reason": (
                    "The source material passes the loader's phase test, but "
                    "the normalized artifact has no fluid stack candidate. "
                    "Loader_Recipes_Decomp therefore falls back to dust where "
                    "available; this is not counted as a missing fluid."
                ),
            })

    result: list[dict[str, Any]] = []
    for record in non_molten:
        fluid = str(record["fluid"])
        source_id = int(record["material_id"])
        source_name = str(record["material"])
        key = source_key(source_id, source_name)
        normalized = (
            normalized_by_id.get(source_id)
            if source_id >= 0
            else normalized_by_name.get(source_name)
        )
        cc_material = (
            cc_by_source_id.get(source_id)
            if source_id >= 0
            else cc_by_source_name.get(source_name)
        )
        projected = relations.get(key, [])
        candidate_count = len(candidates_by_source[key])
        if projected and candidate_count == 1:
            classification = "closure_required"
            reason_code = "UNIQUE_SOURCE_FLUID_FOR_LOADER_PROJECTION"
            reason = (
                "Pinned Loader_Recipes_Decomp semantics project this source "
                "material as a non-solid input or output, and the normalized "
                "fluid artifact supplies exactly one non-molten fluid ID for "
                "that source material."
            )
        elif projected:
            classification = "not_required_by_loader_projection"
            reason_code = "LOADER_PROJECTION_ALIAS_AMBIGUITY"
            reason = (
                "Pinned loader semantics project this source material, but "
                f"{candidate_count} non-molten fluid IDs map to it. The "
                "final pinned source selector must name an exact fluid before "
                "it is registered."
            )
        else:
            classification = "not_required_by_loader_projection"
            reason_code = "NO_LOADER_REQUIREMENT_PROVEN"
            reason = (
                "No T5a Loader_Recipes_Decomp input/output relation proves "
                "this candidate is closure-required. It remains classified but "
                "is not registered unless a selected pinned route references it."
            )
        result.append({
            "fluid": fluid,
            "classification": classification,
            "reason_code": reason_code,
            "reason": reason,
            "source": {
                "repository": policy["gt6_source"]["repository"],
                "revision": policy["gt6_source"]["revision"],
                "path": "tools/gt6_oredict_fluids_normalized.json",
                "source_material_id": source_id,
                "source_material_name": source_name,
            },
            "source_evidence": [
                evidence(
                    "tools/gt6_oredict_fluids_normalized.json",
                    f"records[fluid={fluid}]",
                    record,
                ),
            ],
            "phase": (
                normalized.get("state") if normalized is not None else None
            ),
            "cc_material": cc_material,
            "source_material_id": source_id,
            "source_material_name": source_name,
            "fluid_amount": record["fluid_amount"],
            "cc_units": record["cc_units"],
            "integral_cc_units": record["integral_cc_units"],
            "candidate_ids_for_source_material": sorted(
                str(candidate["fluid"])
                for candidate in candidates_by_source[key]
            ),
            "projected_relations": projected,
        })

    classifications = Counter(
        row["classification"] for row in result
    )
    phases = Counter(
        row["phase"] if row["phase"] is not None else "unknown"
        for row in result
    )
    return result, {
        "all_normalized_fluid_records": len(fluid_records),
        "excluded_molten_candidates": len(fluid_records) - len(non_molten),
        "non_molten_candidates": len(non_molten),
        "classifications": dict(sorted(classifications.items())),
        "phases": dict(sorted(phases.items())),
        "projected_source_materials": len(relations),
        "projected_relations": sum(len(rows) for rows in relations.values()),
        "phase_test_without_fluid_candidate_count": len(
            missing_candidates
        ),
        "phase_test_without_fluid_candidates": missing_candidates,
        "missing_component_source_count": len(missing_component_sources),
        "missing_component_sources": missing_component_sources,
    }


def validate_rows(
    rows: list[dict[str, Any]],
    allowed: set[str],
    name: str,
) -> None:
    identities = [
        str(row.get("material", row.get("fluid", ""))) for row in rows
    ]
    if not identities or any(not identity for identity in identities):
        raise ValueError(f"{name}: blank or missing row identity")
    if len(identities) != len(set(identities)):
        raise ValueError(f"{name}: duplicate row identity")
    for identity, row in zip(identities, rows):
        if row.get("classification") not in allowed:
            raise ValueError(
                f"{name}/{identity}: invalid classification"
            )
        for field in ("reason", "source", "source_evidence"):
            value = row.get(field)
            if not value:
                raise ValueError(
                    f"{name}/{identity}: missing non-empty {field}"
                )


def build() -> dict[str, Any]:
    policy = load(POLICY)
    closure = load(ORE_CLOSURE)
    material_documents = load_material_documents()
    gate = load(REGISTRATION_GATE)["materials"]
    normalized_material_document = load(NORMALIZED_MATERIALS)
    normalized_fluid_document = load(NORMALIZED_FLUIDS)
    roadmap = load(MAP_ROADMAP)
    template_index = load(TEMPLATE_INDEX)
    template_report = load(TEMPLATE_REPORT)
    expected = policy["ledger_contract"]["expected_counts"]

    normalized_by_id, normalized_by_name = normalized_material_indexes(
        normalized_material_document["records"]
    )
    terminal, crusher, byproduct, origins = chemical_domain(
        closure, expected
    )
    missing_documents = sorted(set(origins) - set(material_documents))
    if missing_documents:
        raise ValueError(
            "chemical union references missing materials: "
            + ", ".join(missing_documents)
        )
    missing_gate = sorted(set(origins) - set(gate))
    if missing_gate:
        raise ValueError(
            "chemical union references ungated materials: "
            + ", ".join(missing_gate)
        )

    material_rows = []
    for material, material_origins in origins.items():
        document = material_documents[material]
        normalized = normalized_for_document(
            document, normalized_by_id, normalized_by_name
        )
        material_rows.append(classify_material(
            material,
            material_origins,
            document,
            normalized,
            set(gate[material]),
            policy,
            roadmap,
        ))
    byproducts = byproduct_rows(byproduct, closure, policy)
    fluids, fluid_counts = fluid_inventory(
        normalized_fluid_document["records"],
        material_documents,
        normalized_by_id,
        normalized_by_name,
        material_rows,
        policy,
    )

    material_allowed = set(
        policy["ledger_contract"]["material_classifications"]
    )
    fluid_allowed = set(
        policy["ledger_contract"]["fluid_classifications"]
    )
    validate_rows(material_rows, material_allowed, "chemical_materials")
    validate_rows(
        byproducts, {"byproduct_only_debt"}, "byproduct_only_debt"
    )
    validate_rows(fluids, fluid_allowed, "non_molten_fluids")

    material_classifications = Counter(
        row["classification"] for row in material_rows
    )
    origin_partitions = Counter(
        (
            "overlap"
            if len(row["origins"]) == 2
            else f"{row['origins'][0]}_only"
        )
        for row in material_rows
    )
    route_maps = Counter(
        route["map"]
        for row in material_rows
        for route in row["map_destinations"]
    )
    failed_conjunctions = Counter(
        key
        for row in material_rows
        for key, passed in row["loader_conjunction"].items()
        if not passed
    )
    classified = len(material_rows)
    unclassified = len(origins) - classified
    if unclassified:
        raise ValueError(
            f"T5a chemical union has {unclassified} unclassified rows"
        )

    required_maps = policy["recipe_replay"]["required_maps"]
    indexed_template_maps = set(template_index.get("maps") or {})
    reported_template_maps = set(template_report.get("maps") or {})
    template_coverage = {
        map_id: {
            "in_template_index": map_id in indexed_template_maps,
            "in_template_report": map_id in reported_template_maps,
            "pinned_dump_path": source_path,
            "pinned_dump_sha256": sha256(ROOT / source_path),
            "classification": "pinned_source_present",
            "reason": (
                "The fixed-revision recipe-map dump is present and is consumed "
                "by the final source projection."
            ),
            "source": {
                "index": "gt6_dump/gt6_recipe_dump/index.json",
                "selector": (
                    "tools/build_t5_distillery_projection.py"
                    if map_id == "gt.recipe.distillery"
                    else "tools/build_t5_source_projection.py"
                ),
            },
        }
        for map_id, source_path in sorted(required_maps.items())
    }

    blockers: list[dict[str, Any]] = []

    expected_projection = {
        "terminal_dust_t5_chemical": len(terminal),
        "crusher_t5_chemical": len(crusher),
        "chemical_overlap": len(set(terminal) & set(crusher)),
        "unique_chemical_union": len(origins),
        "byproduct_only_debt": len(byproducts),
        "byproduct_materials_also_in_chemical_union": len(
            set(byproduct) & set(origins)
        ),
        "byproduct_materials_outside_chemical_union": len(
            set(byproduct) - set(origins)
        ),
        "origin_partitions": dict(sorted(origin_partitions.items())),
        "material_classifications": {
            classification: material_classifications.get(
                classification, 0
            )
            for classification in sorted(material_allowed)
        },
        "source_loader_eligible": sum(
            row["source_loader_eligible"] for row in material_rows
        ),
        "no_decompose": sum(
            row["no_decompose"] for row in material_rows
        ),
        "route_map_memberships": dict(sorted(route_maps.items())),
        "non_molten_fluid_candidates": fluid_counts[
            "non_molten_candidates"
        ],
        "non_molten_fluid_classifications": fluid_counts[
            "classifications"
        ],
        "non_molten_fluid_phases": fluid_counts["phases"],
    }
    if expected_projection != expected:
        raise ValueError(
            "T5a policy count lock drifted: "
            f"expected {expected!r}, got {expected_projection!r}"
        )

    return {
        "schema_version": 1,
        "status": "SOURCE_REPLAY_VERIFIED",
        "delivery_boundary": policy["delivery_boundary"],
        "gt6_source": policy["gt6_source"],
        "source_hashes": {
            "builder": sha256(Path(__file__).resolve()),
            "gitignore": sha256(GITIGNORE),
            "gt6_recipe_dump_index": sha256(DUMP_INDEX),
            "gt6_map_roadmap": sha256(MAP_ROADMAP),
            "gt6_ore_chain_closure": sha256(ORE_CLOSURE),
            "gt6_oredict_fluids_normalized": sha256(NORMALIZED_FLUIDS),
            "gt6_oredict_materials_normalized": sha256(
                NORMALIZED_MATERIALS
            ),
            "gt6_recipe_templates_index": sha256(TEMPLATE_INDEX),
            "gt6_recipe_templates_report": sha256(TEMPLATE_REPORT),
            "material_catalog": directory_sha256(MATERIAL_ROOT),
            "material_index": sha256(MATERIAL_INDEX),
            "material_registration_gate": sha256(REGISTRATION_GATE),
            "policy": sha256(POLICY),
        },
        "loader_semantics": policy["loader_semantics"],
        "cruciblecraft_gate": policy["cruciblecraft_gate"],
        "counts": {
            "input_ledgers": {
                "terminal_dust_t5_chemical": len(terminal),
                "crusher_t5_chemical": len(crusher),
                "chemical_overlap": len(set(terminal) & set(crusher)),
                "unique_chemical_union": len(origins),
                "byproduct_only_debt": len(byproducts),
                "byproduct_materials_also_in_chemical_union": len(
                    set(byproduct) & set(origins)
                ),
                "byproduct_materials_outside_chemical_union": len(
                    set(byproduct) - set(origins)
                ),
            },
            "origin_partitions": dict(sorted(origin_partitions.items())),
            "material_classifications": {
                classification: material_classifications.get(
                    classification, 0
                )
                for classification in sorted(material_allowed)
            },
            "source_loader_eligible": sum(
                row["source_loader_eligible"] for row in material_rows
            ),
            "no_decompose": sum(
                row["no_decompose"] for row in material_rows
            ),
            "route_map_memberships": dict(sorted(route_maps.items())),
            "failed_loader_conjuncts": dict(
                sorted(failed_conjunctions.items())
            ),
            "classified": classified,
            "unclassified": unclassified,
            "fluids": fluid_counts,
        },
        "template_coverage": template_coverage,
        "recipe_replay": policy["recipe_replay"],
        "blockers": blockers,
        "chemical_materials": material_rows,
        "byproduct_only_debt": byproducts,
        "non_molten_fluid_candidates": fluids,
    }


def reference_only_check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        return [f"missing generated file: {OUTPUT.relative_to(ROOT)}"]
    try:
        document = load(OUTPUT)
        if OUTPUT.read_text(encoding="utf-8") != stable_json(document):
            errors.append("T5 readiness artifact is not canonical JSON")
        t13 = load(T13_RECIPE_MAPS)
        receipt = t13["full_replay_receipt"]
        expected_hashes = {
            "builder": sha256(Path(__file__).resolve()),
            "gitignore": sha256(GITIGNORE),
            "gt6_recipe_dump_index": receipt["dump_index_sha256"],
            "gt6_map_roadmap": sha256(MAP_ROADMAP),
            "gt6_ore_chain_closure": sha256(ORE_CLOSURE),
            "gt6_oredict_fluids_normalized": sha256(NORMALIZED_FLUIDS),
            "gt6_oredict_materials_normalized": sha256(
                NORMALIZED_MATERIALS
            ),
            "gt6_recipe_templates_index": sha256(TEMPLATE_INDEX),
            "gt6_recipe_templates_report": sha256(TEMPLATE_REPORT),
            "material_catalog": directory_sha256(MATERIAL_ROOT),
            "material_index": sha256(MATERIAL_INDEX),
            "material_registration_gate": sha256(REGISTRATION_GATE),
            "policy": sha256(POLICY),
        }
        if document.get("source_hashes") != expected_hashes:
            errors.append("T5 readiness source hashes drifted")
        map_hashes = {
            row["name_internal"]: row["source_blob"]
            for row in t13["rows"]
        }
        for map_name, coverage in (
            document.get("template_coverage") or {}
        ).items():
            if (
                coverage.get("pinned_dump_sha256")
                != map_hashes.get(map_name)
            ):
                errors.append(
                    f"T5 template coverage hash drifted: {map_name}"
                )
        counts = document.get("counts") or {}
        if (
            document.get("status") != "SOURCE_REPLAY_VERIFIED"
            or counts.get("classified")
            != len(document.get("chemical_materials") or [])
            or counts.get("unclassified") != 0
            or len(document.get("blockers") or []) != 0
        ):
            errors.append("T5 readiness compact counts are inconsistent")
    except (KeyError, OSError, TypeError, ValueError, json.JSONDecodeError) as exc:
        errors.append(str(exc))
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed T5a readiness ledger is stale",
    )
    replay_mode = parser.add_mutually_exclusive_group()
    replay_mode.add_argument("--reference-only", action="store_true")
    replay_mode.add_argument("--full-replay", action="store_true")
    args = parser.parse_args()
    if (args.reference_only or args.full_replay) and not args.check:
        parser.error("--reference-only and --full-replay require --check")
    if args.check and args.reference_only:
        errors = reference_only_check()
        if errors:
            raise SystemExit(
                "T5a compact readiness is stale:\n"
                + "\n".join(f"- {error}" for error in errors)
            )
        print("T5a compact chemical readiness ledger is current.")
        return 0
    document = build()
    encoded = stable_json(document)
    if args.check:
        if not OUTPUT.is_file() or OUTPUT.read_text(
            encoding="utf-8"
        ) != encoded:
            raise SystemExit("T5a chemical readiness ledger is stale")
        print("T5a chemical readiness ledger is current.")
        return 0
    OUTPUT.write_text(encoded, encoding="utf-8", newline="\n")
    print(f"wrote {OUTPUT.relative_to(ROOT)}")
    print(json.dumps(document["counts"], sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
