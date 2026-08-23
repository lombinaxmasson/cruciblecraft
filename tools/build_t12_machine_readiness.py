#!/usr/bin/env python3
"""Build the fixed-source T12a machine/scaling/boundary readiness gate."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
from collections import Counter
from pathlib import Path
from typing import Any
from urllib.request import urlopen


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t12_machine_policy.json"
OUTPUT = TOOLS / "t12a_machine_readiness.json"
BENCHMARK = TOOLS / "t12_capacity_matcher_benchmark.json"
GT6_METADATA = TOOLS / "gt6_reference_metadata.json"
PROCESSING_MACHINES = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry"
    / "ModProcessingMachines.java"
)
MATERIAL_GATE = (
    ROOT
    / "src/main/resources/data/cruciblecraft"
    / "material_registration_gate.json"
)
MATERIAL_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/materials"
)
MACHINE_CRAFTING_READINESS = TOOLS / "machine_crafting_readiness.json"
T11_PREFLIGHT = TOOLS / "t11_preflight_projection.json"
CAPACITY_MATCHER = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/recipe/gt"
    / "CapacityMatcher.java"
)
BENCHMARK_HARNESS = (
    ROOT
    / "src/test/java/com/masson/cruciblecraft/recipe/gt"
    / "CapacityMatcherBenchmarkHarness.java"
)
COKE_OVEN_STRUCTURE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/multiblock"
    / "CokeOvenStructure.java"
)
COKE_OVEN_LAYOUT = COKE_OVEN_STRUCTURE.with_name(
    "CokeOvenStructureLayout.java"
)
COKE_OVEN_JSON = (
    ROOT
    / "src/main/resources/data/cruciblecraft"
    / "multiblock_structures/coke_oven.json"
)

PROCESSING_CLASSES = {
    "source_tiered",
    "fixed_utility",
    "deferred_with_reason",
}
NON_SPEC_CLASSES = {
    "tier_profile_participant",
    "material_capability",
    "fixed_infrastructure",
    "deferred",
}
ENERGY_IDENTITIES = {
    "RU",
    "KU",
    "EU",
    "HU",
    "TU",
    "LU",
    "AU",
    "RU_TO_EU",
    "NONE",
    "UNVERIFIED",
}
SELECTED_FORM_REQUIREMENTS = {
    "centrifuge": {"gear", "long_rod"},
    "sifter": {"spring", "fine_wire", "rod"},
}
PINNED_VERTICAL_FACTS = {
    "centrifuge": {
        "energy_identity": "RU",
        "cheap_overclock": False,
        "parallel_duration": True,
        "efficiency": 10_000,
        "tiers": [
            (1, "bronze", 32, 1, None),
            (2, "steel", 128, 2, None),
            (3, "titanium", 512, 4, None),
        ],
    },
    "sifter": {
        "energy_identity": "KU",
        "cheap_overclock": False,
        "parallel_duration": True,
        "efficiency": 10_000,
        "tiers": [
            (1, "bronze", 32, 4, None),
            (2, "steel", 128, 8, None),
            (3, "titanium", 512, 16, None),
        ],
    },
    "electrolyzer": {
        "energy_identity": "EU",
        "cheap_overclock": False,
        "parallel_duration": True,
        "efficiency": 10_000,
        "tiers": [
            (1, "steel_galvanized", 32, 1, "tin"),
            (2, "aluminium", 128, 2, "copper"),
            (3, "stainless_steel", 512, 4, "gold"),
        ],
    },
}


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


def canonical_hash(value: Any) -> str:
    payload = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def git_blob_sha1(data: bytes) -> str:
    normalized = data.replace(b"\r\n", b"\n")
    header = f"blob {len(normalized)}\0".encode("ascii")
    return hashlib.sha1(header + normalized).hexdigest()


def _source_excerpt(lines: list[str], line_spec: str) -> str:
    selected: list[str] = []
    for part in line_spec.split(","):
        bounds = part.strip().split("-", maxsplit=1)
        first = int(bounds[0])
        last = int(bounds[-1])
        if first <= 0 or last < first or last > len(lines):
            raise ValueError(f"invalid pinned source lines: {part}")
        selected.extend(lines[first - 1:last])
    return "\n".join(selected)


def validate_source_tree(
    source_root: Path,
    policy: dict[str, Any],
) -> dict[str, str]:
    records = policy["gt6_source"]["source_files"]
    texts: dict[str, str] = {}
    hashes: dict[str, str] = {}
    for key, record in records.items():
        path = source_root / record["path"]
        if not path.is_file():
            raise ValueError(f"missing fetched GT6 source file {path}")
        data = path.read_bytes()
        actual = git_blob_sha1(data)
        if actual != record["git_blob_sha1"]:
            raise ValueError(
                f"{record['path']}: expected git blob "
                f"{record['git_blob_sha1']}, got {actual}"
            )
        texts[key] = data.decode("utf-8")
        hashes[key] = actual

    errors: list[str] = []
    for name, anchor in policy["source_anchors"].items():
        text = texts[anchor["file"]]
        excerpt = _source_excerpt(text.splitlines(), anchor["lines"])
        normalized_excerpt = re.sub(r"\s+", " ", excerpt)
        for token in anchor.get("required_tokens", []):
            normalized_token = re.sub(r"\s+", " ", token)
            if normalized_token not in normalized_excerpt:
                errors.append(f"{name}: missing token {token!r}")
        for pattern in anchor.get("required_absent_patterns", []):
            if pattern in text:
                errors.append(
                    f"{name}: forbidden source pattern is present: {pattern}"
                )
    if errors:
        raise ValueError(
            "pinned T12a source verification failed:\n"
            + "\n".join(errors)
        )
    return hashes


def fetch_source(destination: Path, policy: dict[str, Any]) -> None:
    for record in policy["gt6_source"]["source_files"].values():
        with urlopen(record["raw_url"]) as response:
            data = response.read()
        actual = git_blob_sha1(data)
        if actual != record["git_blob_sha1"]:
            raise ValueError(
                f"{record['path']}: fetched git blob {actual} does not "
                f"match {record['git_blob_sha1']}"
            )
        target = destination / record["path"]
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    validate_source_tree(destination, policy)


def validate_revision(policy: dict[str, Any]) -> None:
    source = policy["gt6_source"]
    metadata = load(GT6_METADATA)
    expected_version = "6.17.06-22-g3703e4030"
    if metadata.get("gt6_version") != expected_version:
        raise ValueError("GT6 dump version drifted from the T12a source pin")
    dump_revision = expected_version.rsplit("-g", maxsplit=1)[-1]
    if not source["revision"].startswith(dump_revision):
        raise ValueError("T12a source revision does not match the GT6 dump")
    legacy = load(MACHINE_CRAFTING_READINESS)
    if legacy["gt6_source"]["revision"] != source["revision"]:
        raise ValueError("T5.5 and T12a GT6 revisions diverged")


def current_processing_machine_ids() -> list[str]:
    source = PROCESSING_MACHINES.read_text(encoding="utf-8")
    pattern = re.compile(
        r"public\s+static\s+final\s+ProcessingMachineSpec\s+"
        r"([A-Z0-9_]+)\s*=\s*(.*?);",
        flags=re.DOTALL,
    )
    t36_owned = {"ROASTER", "COAGULATOR"}
    result: list[str] = []
    for constant, expression in pattern.findall(source):
        if constant in t36_owned:
            continue
        helper = re.search(
            r"(?:mechanical|reusedT5|t3|t5)\s*\(\s*\"([^\"]+)\"",
            expression,
        )
        direct = re.search(r"\bid\(\s*\"([^\"]+)\"\s*\)", expression)
        match = helper or direct
        if match is None:
            raise ValueError(
                f"could not derive machine id for {constant}"
            )
        result.append(match.group(1))
    if len(result) != 25 or len(result) != len(set(result)):
        raise ValueError(
            f"expected 25 unique processing specs, found {len(result)}"
        )
    return result


def validate_ledger_row(
    row: dict[str, Any],
    allowed: set[str],
    vocabulary: dict[str, str],
) -> None:
    identity = row.get("id")
    if not isinstance(identity, str) or not identity:
        raise ValueError("ledger row has no id")
    classification = row.get("classification")
    if classification not in allowed or classification not in vocabulary:
        raise ValueError(
            f"{identity}: invalid or undocumented classification "
            f"{classification!r}"
        )
    if row.get("energy_identity") not in ENERGY_IDENTITIES:
        raise ValueError(
            f"{identity}: invalid energy identity "
            f"{row.get('energy_identity')!r}"
        )
    for field in ("source_lines", "reason", "revisit_point"):
        if not isinstance(row.get(field), str) or not row[field].strip():
            raise ValueError(f"{identity}: missing non-empty {field}")


def build_ledger(
    rows: list[dict[str, Any]],
    allowed: set[str],
    vocabulary: dict[str, str],
) -> dict[str, Any]:
    ids: list[str] = []
    for row in rows:
        validate_ledger_row(row, allowed, vocabulary)
        ids.append(row["id"])
    if len(ids) != len(set(ids)):
        raise ValueError("ledger ids are not unique")
    categories = Counter(row["classification"] for row in rows)
    energies = Counter(row["energy_identity"] for row in rows)
    return {
        "classification_vocabulary": vocabulary,
        "counts": {
            "total": len(rows),
            "classified": len(rows),
            "unclassified": 0,
            "classifications": {
                name: categories.get(name, 0)
                for name in sorted(allowed)
            },
            "energy_identities": {
                name: energies[name]
                for name in sorted(energies)
            },
        },
        "rows": rows,
    }


def build_ledgers(policy: dict[str, Any]) -> tuple[dict[str, Any], dict[str, Any]]:
    processing = build_ledger(
        policy["processing_kinds"],
        PROCESSING_CLASSES,
        policy["classification_vocabulary"]["processing_kind"],
    )
    current_ids = set(current_processing_machine_ids())
    policy_ids = {row["id"] for row in processing["rows"]}
    if current_ids != policy_ids:
        raise ValueError(
            "processing-kind coverage drifted: "
            f"missing={sorted(current_ids - policy_ids)}, "
            f"extra={sorted(policy_ids - current_ids)}"
        )
    if processing["counts"]["total"] != 25:
        raise ValueError("T12a processing ledger must cover 25 kinds")
    processing["counts"]["configured"] = 24
    processing["counts"]["bronze_crusher"] = 1

    non_spec = build_ledger(
        policy["non_spec_kinds"],
        NON_SPEC_CLASSES,
        policy["classification_vocabulary"]["non_spec_kind"],
    )
    if non_spec["counts"]["total"] != 12:
        raise ValueError("T12a non-spec ledger must cover 12 kinds")
    return processing, non_spec


def representative_recipes(
    policy: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    expected = policy["vertical_slice"]["representative_recipes"]
    for machine_id in policy["vertical_slice"]["machine_order"]:
        record = expected[machine_id]
        path = ROOT / record["path"]
        document = load(path)
        recipes = document["recipes"]
        if (
            document.get("nameInternal") != record["expected_map"]
            or document.get("recipeCount")
            != record["expected_recipe_count"]
            or len(recipes) != record["expected_recipe_count"]
        ):
            raise ValueError(
                f"{machine_id}: representative source map drifted"
            )
        recipe = recipes[record["recipe_index"]]
        if (
            recipe.get("euPerTick") != record["eut"]
            or recipe.get("duration") != record["duration"]
            or recipe.get("enabled") is False
            or recipe.get("hidden") is True
            or recipe.get("fake") is True
        ):
            raise ValueError(
                f"{machine_id}: representative source row drifted"
            )
        result[machine_id] = {
            "map": record["expected_map"],
            "path": (
                f"{record['path']}#recipes[{record['recipe_index']}]"
            ),
            "recipe_index": record["recipe_index"],
            "row_sha256": canonical_hash(recipe),
            "eut": recipe["euPerTick"],
            "duration": recipe["duration"],
        }
    return result


def validate_vertical_facts(policy: dict[str, Any]) -> None:
    selected = policy["vertical_slice"]
    if selected["machine_order"] != list(PINNED_VERTICAL_FACTS):
        raise ValueError("T12a selected machine order drifted")
    for machine_id, expected in PINNED_VERTICAL_FACTS.items():
        machine = selected["machines"][machine_id]
        for field in (
            "energy_identity",
            "cheap_overclock",
            "parallel_duration",
            "efficiency",
        ):
            if machine[field] != expected[field]:
                raise ValueError(
                    f"{machine_id}: pinned {field} drifted"
                )
        actual_tiers = [
            (
                tier["source_tier"],
                tier["material"],
                tier["nominal_input"],
                tier["parallel"],
                tier.get("cable_material"),
            )
            for tier in machine["tiers"]
        ]
        if actual_tiers != expected["tiers"]:
            raise ValueError(
                f"{machine_id}: pinned tier rows drifted"
            )


def _overclock(
    recipe_power: int,
    base_progress: int,
    input_min: int,
    input_max: int,
    cheap: bool,
) -> tuple[int, int, int]:
    power = recipe_power
    progress = base_progress
    steps = 0
    if not cheap:
        while power < input_min and power * 4 <= input_max:
            power *= 4
            progress *= 2
            steps += 1
    return power, progress, steps


def _power_point(
    actual_input: int,
    progress: int,
    parallel: int,
    minimum_power: int,
) -> dict[str, Any]:
    if actual_input < minimum_power:
        return {
            "actual_input": actual_input,
            "result": "UNDERPOWERED",
            "effective_duration_ticks": None,
            "throughput_recipes_per_tick": 0.0,
        }
    duration = math.ceil(progress / actual_input)
    return {
        "actual_input": actual_input,
        "result": "RUNNABLE",
        "effective_duration_ticks": duration,
        "throughput": {
            "recipes": parallel,
            "ticks": duration,
        },
        "throughput_recipes_per_tick": round(
            parallel / duration, 9
        ),
    }


def scaling_matrix(
    policy: dict[str, Any],
    recipes: dict[str, dict[str, Any]],
) -> dict[str, Any]:
    rows: list[dict[str, Any]] = []
    machines = policy["vertical_slice"]["machines"]
    for machine_id in policy["vertical_slice"]["machine_order"]:
        machine = machines[machine_id]
        recipe = recipes[machine_id]
        if machine["efficiency"] != 10_000:
            raise ValueError(
                f"{machine_id}: selected scaling matrix expects source "
                "default efficiency 10000"
            )
        for tier in machine["tiers"]:
            nominal = tier["nominal_input"]
            input_min = nominal // 2
            input_max = nominal * 2
            base_progress = (
                recipe["eut"]
                * recipe["duration"]
                * tier["parallel"]
            )
            scenarios: dict[str, Any] = {}
            for scenario_name, cheap in (
                ("standard", False),
                ("cheap", True),
            ):
                minimum_power, progress, steps = _overclock(
                    recipe["eut"],
                    base_progress,
                    input_min,
                    input_max,
                    cheap,
                )
                scenarios[scenario_name] = {
                    "selected_by_machine": (
                        machine["cheap_overclock"] == cheap
                    ),
                    "overclock_steps": steps,
                    "minimum_recipe_power": minimum_power,
                    "progress_requirement": progress,
                    "at_input_min": _power_point(
                        input_min,
                        progress,
                        tier["parallel"],
                        minimum_power,
                    ),
                    "at_nominal": _power_point(
                        nominal,
                        progress,
                        tier["parallel"],
                        minimum_power,
                    ),
                    "at_input_max": _power_point(
                        input_max,
                        progress,
                        tier["parallel"],
                        minimum_power,
                    ),
                }
            rows.append({
                "machine": machine_id,
                "energy_identity": machine["energy_identity"],
                "source_tier": tier["source_tier"],
                "material": tier["material"],
                "input_min": input_min,
                "nominal_input": nominal,
                "input_max": input_max,
                "recipe": recipe,
                "parallel": tier["parallel"],
                "parallel_duration": machine["parallel_duration"],
                "efficiency": machine["efficiency"],
                "cheap_overclock_selected": machine[
                    "cheap_overclock"
                ],
                "scenarios": scenarios,
            })
    if len(rows) != 9:
        raise ValueError("T12a scaling matrix must contain nine tier rows")
    return {
        "independent_expected_selector": True,
        "production_selector_reused": False,
        "contract": policy["scaling_contract"],
        "counts": {
            "machines": 3,
            "tiers_per_machine": 3,
            "rows": len(rows),
            "overclock_scenarios_per_row": 2,
        },
        "rows": rows,
        "failure_matrix": policy["scaling_contract"][
            "failure_outcomes"
        ],
    }


def component_projection(policy: dict[str, Any]) -> dict[str, Any]:
    gate = load(MATERIAL_GATE)["materials"]
    machines = policy["vertical_slice"]["machines"]
    variants: list[dict[str, Any]] = []
    for machine_id in policy["vertical_slice"]["machine_order"]:
        machine = machines[machine_id]
        for tier in machine["tiers"]:
            material = tier["material"]
            material_path = MATERIAL_ROOT / f"{material}.json"
            if not material_path.is_file() or material not in gate:
                raise ValueError(
                    f"{machine_id} tier material is not registered: {material}"
                )
            required_forms = SELECTED_FORM_REQUIREMENTS.get(
                machine_id, set()
            )
            missing_forms = sorted(
                required_forms - set(gate[material])
            )
            if missing_forms:
                raise ValueError(
                    f"{machine_id}/{material} missing selected forms: "
                    f"{missing_forms}"
                )
            blockers = [
                (
                    "OP.casingMachineDouble"
                    if machine_id in {"centrifuge", "sifter"}
                    else "OP.casingMachine"
                )
            ]
            if machine_id == "electrolyzer":
                cable_material = tier["cable_material"]
                if "cable" not in gate.get(cable_material, []):
                    raise ValueError(
                        f"electrolyzer tier cable is unavailable: "
                        f"{cable_material}"
                    )
                if "wire" not in gate.get("platinum", []):
                    raise ValueError("platinum wire is unavailable")
            variants.append({
                "id": f"{machine_id}/t{tier['source_tier']}",
                "machine": machine_id,
                "source_tier": tier["source_tier"],
                "material": material,
                "registered_material": True,
                "non_casing_forms": sorted(required_forms),
                "non_casing_forms_reachable": True,
                "cable_material": tier.get("cable_material"),
                "blocking_components": blockers,
                "craft_status": (
                    "FAIL_CLOSED_MISSING_MACHINE_CASING"
                ),
            })

    if (
        (
            ROOT
            / "src/main/resources/data/cruciblecraft"
            / "material_prefixes/machine_casing.json"
        ).exists()
        or (
            ROOT
            / "src/main/resources/data/cruciblecraft"
            / "material_prefixes/double_machine_casing.json"
        ).exists()
    ):
        raise ValueError(
            "machine casing prefixes appeared; T12a component policy "
            "must be re-reviewed"
        )
    projection = policy["component_projection"]
    if len(variants) != projection["variant_count"]:
        raise ValueError("T12a variant projection count drifted")
    return {
        **projection,
        "counts": {
            "variants": len(variants),
            "crafts": len(variants),
            "crafts_ready": 0,
            "crafts_fail_closed": len(variants),
            "machine_casing_components_missing": 2,
        },
        "variants": variants,
    }


def structure_projection(policy: dict[str, Any]) -> dict[str, Any]:
    if COKE_OVEN_LAYOUT.is_file() and COKE_OVEN_STRUCTURE.is_file():
        layout = COKE_OVEN_LAYOUT.read_text(encoding="utf-8")
        structure = COKE_OVEN_STRUCTURE.read_text(encoding="utf-8")
        for token in (
            "new ArrayList<>(25)",
            "if (x == 0 && y == 0 && z == 0)",
            "center.x(), -2, center.z()",
        ):
            if token not in layout:
                raise ValueError("Coke Oven layout baseline drifted")
        for token in (
            "ModBlocks.COKE_OVEN",
            "ModBlocks.FIREBRICK",
            "isAir()",
            "hasChunkAt",
        ):
            if token not in structure:
                raise ValueError("Coke Oven structure baseline drifted")
    elif COKE_OVEN_JSON.is_file():
        document = load(COKE_OVEN_JSON)
        predicates = Counter(
            row["predicate"] for row in document["structure"]
        )
        if (
            document.get("schema_version") != 1
            or predicates != {"F": 25, "A": 1, "C": 1}
            or document["anchors"].get("center") != [0, 0, 1]
            or document["anchors"].get("heat_source") != [0, -2, 1]
        ):
            raise ValueError("Coke Oven JSON baseline drifted")
    else:
        raise ValueError("Coke Oven baseline source is missing")
    result = policy["structure_projection"]
    coke = result["coke_oven"]
    large = result["large_centrifuge"]
    if (
        sum(coke["predicate_counts"].values()) != coke["scan_volume"]
        or math.prod(large["scan_dimensions"]) != large["scan_volume"]
        or (
            sum(large["port_counts"].values())
            + large["controller_count"]
            != large["scan_volume"]
        )
        or large["port_counts"]
        != {"item_fluid": 15, "energy_input": 2}
        or large["controller_count"] != 1
        or large["port_count_correction_audit"].get(
            "historical_expected_item_fluid_ports"
        )
        != 16
        or large["port_count_correction_audit"].get("historical_status")
        != "SUPERSEDED_INCORRECT_EXPECTATION"
        or large["port_count_correction_audit"].get("owner") != "T15e"
        or large["requested_symbol_status"] != "ABSENT_IN_PINNED_TREE"
    ):
        raise ValueError("T12a structure projection is internally inconsistent")
    return result


def benchmark_acceptance(policy: dict[str, Any]) -> dict[str, Any]:
    if not BENCHMARK.is_file():
        raise ValueError(
            "missing T12a matcher benchmark; run "
            "./gradlew t12CapacityMatcherBenchmark"
        )
    document = load(BENCHMARK)
    scenarios = document.get("scenarios") or []
    dense_supplies = {
        row["supplies"]
        for row in scenarios
        if row["id"].startswith("dense_consuming_")
    }
    rejected_supplies = {
        row["supplies"]
        for row in scenarios
        if row["id"].startswith("presence_cap_rejection_")
    }
    expected_supplies = set(
        policy["matcher_benchmark"]["required_supply_counts"]
    )
    if (
        document.get("status") != "T12A_BENCHMARK_READY"
        or dense_supplies != expected_supplies
        or rejected_supplies != {16, 32, 64}
        or not all(row.get("within_budget") for row in scenarios)
        or document["decision"]["current_presence_supply_cap"] != 12
        or document["decision"]["matcher_rewritten"]
        or "PRESENCE_RESERVATION_SUPPLY_CAP = 12"
        not in CAPACITY_MATCHER.read_text(encoding="utf-8")
    ):
        raise ValueError("T12a CapacityMatcher benchmark gate failed")
    return {
        "artifact": BENCHMARK.relative_to(ROOT).as_posix(),
        "artifact_sha256": sha256(BENCHMARK),
        "harness": BENCHMARK_HARNESS.relative_to(ROOT).as_posix(),
        "harness_sha256": sha256(BENCHMARK_HARNESS),
        "matcher_sha256": sha256(CAPACITY_MATCHER),
        "decision": document["decision"],
        "method": document["method"],
        "runtime": document["runtime"],
        "scenarios": scenarios,
    }


def load_gate(policy: dict[str, Any]) -> dict[str, Any]:
    java = PROCESSING_MACHINES.read_text(encoding="utf-8")
    if (
        "T12_AUTHORED_MATERIAL_RULE_BUDGET = 0" not in java
        or "12, T12_AUTHORED_MATERIAL_RULE_BUDGET" not in java
    ):
        raise ValueError("T12 authored MaterialRule zero budget is missing")
    t11 = load(T11_PREFLIGHT)["load_gate"]
    current_entries = t11["projected"]["datapack_recipe_entries"]
    current_publication = t11["projected"]["published_recipes"]
    expected = policy["load_gate"]
    if (
        expected["t12_authored_material_rule_budget"] != 0
        or expected["recipe_publication_additions"] != 0
        or expected["machine_variants_projected"] != 9
        or expected["machine_crafts_projected"] != 9
    ):
        raise ValueError("T12a zero-publication projection drifted")
    return {
        "status": "T12A_PREPROJECTION_READY",
        "current": {
            "datapack_recipe_entries": current_entries,
            "published_recipes": current_publication,
        },
        "projected_after_tiers_only": {
            "datapack_recipe_entries": current_entries,
            "published_recipes": current_publication,
            "concrete_recipe_id_set_change": 0,
        },
        "budgets": {
            "t12_authored_material_rules": 0,
            "all_published_recipes": t11["budgets"][
                "published_recipes"
            ],
            "datapack_recipe_entries": t11["budgets"][
                "datapack_recipe_entries"
            ],
        },
        "runtime_contract": {
            "t12_budget_constant_present": True,
            "tier_profiles_do_not_publish_recipes": True,
        },
    }


def source_records(
    policy: dict[str, Any],
    source_root: Path | None,
) -> dict[str, Any]:
    records = policy["gt6_source"]["source_files"]
    verified = (
        validate_source_tree(source_root, policy)
        if source_root is not None
        else {
            key: record["git_blob_sha1"]
            for key, record in records.items()
        }
    )
    return {
        "mode": "committed_blob_pins_with_optional_local_replay",
        "verified_git_blobs": verified,
        "anchors": {
            name: {
                "path": records[anchor["file"]]["path"],
                "git_blob_sha1": records[anchor["file"]][
                    "git_blob_sha1"
                ],
                "lines": anchor["lines"],
                "reason": anchor["reason"],
            }
            for name, anchor in policy["source_anchors"].items()
        },
    }


def build(source_root: Path | None = None) -> dict[str, Any]:
    policy = load(POLICY)
    validate_revision(policy)
    validate_vertical_facts(policy)
    processing, non_spec = build_ledgers(policy)
    recipes = representative_recipes(policy)
    scaling = scaling_matrix(policy, recipes)
    components = component_projection(policy)
    structures = structure_projection(policy)
    benchmark = benchmark_acceptance(policy)
    load_acceptance = load_gate(policy)
    sources = source_records(policy, source_root)

    blockers = [
        {
            "owner": "T12b",
            "blocker": "MachineKind/TierProfile schema, one-map-to-many lookup and current-state persistence are not implemented.",
            "replacement_condition": "Complete T12b codec/runtime persistence tests."
        },
        {
            "owner": "T12c",
            "blocker": "Nine variants/crafts are fail-closed on machine casing components and RU/KU runtime identity split.",
            "replacement_condition": "Close all source operands and implement the three energy-specific vertical slices."
        },
        {
            "owner": "T12d",
            "blocker": "Coke Oven remains Java-defined and Large Centrifuge has no shared JSON validator/runtime.",
            "replacement_condition": "Move both exact predicate/port projections to the generic schema."
        },
        {
            "owner": "T12e",
            "blocker": "RU producer/Axle/GearBox runtime is absent and 16 Large Centrifuge item/fluid ports exceed the 12-supply presence-reservation cap.",
            "replacement_condition": "Implement the pinned RU network and either constrain matcher supply aggregation to 12 or replace the witness search."
        },
    ]
    return {
        "schema_version": 2,
        "proof_tier": "full_replay",
        "status": "T12A_READY",
        "full_t12_status": "BLOCKED_ON_T12B_T12C_T12D_T12E",
        "delivery_boundary": policy["delivery_boundary"],
        "full_t12_closure_claimed": False,
        "gt6_source": policy["gt6_source"],
        "source_verification": sources,
        "processing_kind_ledger": processing,
        "non_spec_kind_ledger": non_spec,
        "scaling_expected_matrix": scaling,
        "component_and_variant_projection": components,
        "network_projection": policy["network_projection"],
        "structure_projection": structures,
        "capacity_matcher_benchmark": benchmark,
        "supersession": policy["supersession"],
        "load_gate": load_acceptance,
        "t12b_blockers": blockers,
        "source_hashes": {
            "builder": sha256(Path(__file__).resolve()),
            "policy": sha256(POLICY),
            "gt6_reference_metadata": sha256(GT6_METADATA),
            "mod_processing_machines": sha256(PROCESSING_MACHINES),
            "material_registration_gate": sha256(MATERIAL_GATE),
            "machine_crafting_readiness": sha256(
                MACHINE_CRAFTING_READINESS
            ),
            "t11_preflight_projection": sha256(T11_PREFLIGHT),
            "capacity_matcher": sha256(CAPACITY_MATCHER),
            "capacity_matcher_benchmark": sha256(BENCHMARK),
            "capacity_matcher_benchmark_harness": sha256(
                BENCHMARK_HARNESS
            ),
            "representative_recipe_rows": {
                machine: row["row_sha256"]
                for machine, row in recipes.items()
            },
        },
    }


def check(source_root: Path | None = None) -> list[str]:
    encoded = stable_json(build(source_root))
    if not OUTPUT.is_file():
        return [
            f"missing generated file: "
            f"{OUTPUT.relative_to(ROOT).as_posix()}"
        ]
    if OUTPUT.read_text(encoding="utf-8") != encoded:
        return [
            f"stale generated file: "
            f"{OUTPUT.relative_to(ROOT).as_posix()}"
        ]
    return []


def reference_only_check() -> list[str]:
    if not OUTPUT.is_file():
        return [
            f"missing generated file: "
            f"{OUTPUT.relative_to(ROOT).as_posix()}"
        ]
    try:
        document = load(OUTPUT)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        return [f"invalid T12a compact artifact: {exc}"]
    errors: list[str] = []
    if document.get("schema_version") != 2:
        errors.append("T12a compact schema_version must be 2")
    if (
        document.get("status") != "T12A_READY"
        or document.get("proof_tier") != "full_replay"
    ):
        errors.append("T12a compact status/proof tier drifted")
    source_hashes = document.get("source_hashes") or {}
    expected_hashes = {
        "builder": sha256(Path(__file__).resolve()),
        "policy": sha256(POLICY),
        "gt6_reference_metadata": sha256(GT6_METADATA),
        "mod_processing_machines": sha256(PROCESSING_MACHINES),
        "material_registration_gate": sha256(MATERIAL_GATE),
        "machine_crafting_readiness": sha256(
            MACHINE_CRAFTING_READINESS
        ),
        "t11_preflight_projection": sha256(T11_PREFLIGHT),
        "capacity_matcher": sha256(CAPACITY_MATCHER),
        "capacity_matcher_benchmark": sha256(BENCHMARK),
        "capacity_matcher_benchmark_harness": sha256(
            BENCHMARK_HARNESS
        ),
    }
    for key, expected in expected_hashes.items():
        if source_hashes.get(key) != expected:
            errors.append(f"T12a compact source hash drifted: {key}")
    representative = source_hashes.get("representative_recipe_rows") or {}
    if set(representative) != {"centrifuge", "electrolyzer", "sifter"}:
        errors.append("T12a representative recipe row set drifted")
    elif any(
        not isinstance(value, str) or len(value) != 64
        for value in representative.values()
    ):
        errors.append("T12a representative recipe row hash is invalid")
    processing_counts = (
        document.get("processing_kind_ledger", {}).get("counts", {})
    )
    non_spec_counts = (
        document.get("non_spec_kind_ledger", {}).get("counts", {})
    )
    if processing_counts.get("classified") != 25:
        errors.append("T12a processing kind ledger drifted")
    if non_spec_counts.get("classified") != 12:
        errors.append("T12a non-spec kind ledger drifted")
    return errors


def write(source_root: Path | None = None) -> dict[str, Any]:
    document = build(source_root)
    OUTPUT.write_text(
        stable_json(document),
        encoding="utf-8",
        newline="\n",
    )
    return document


def refresh_declared_source_hashes() -> dict[str, Any]:
    """Refresh only the T12a hashes whose upstream inputs changed."""
    current = load(OUTPUT)
    rebuilt = build()
    current_without_hashes = dict(current)
    rebuilt_without_hashes = dict(rebuilt)
    current_without_hashes.pop("source_hashes", None)
    rebuilt_without_hashes.pop("source_hashes", None)
    if current_without_hashes != rebuilt_without_hashes:
        raise ValueError(
            "T12a non-hash projection drifted; full rebuild is not permitted"
        )
    source_hashes = dict(current["source_hashes"])
    rebuilt_hashes = rebuilt["source_hashes"]
    for key in (
        "builder",
        "material_registration_gate",
        "machine_crafting_readiness",
    ):
        source_hashes[key] = rebuilt_hashes[key]
    current["source_hashes"] = source_hashes
    OUTPUT.write_bytes(stable_json(current).encode("utf-8"))
    return current


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed T12a readiness artifact is stale",
    )
    check_mode = parser.add_mutually_exclusive_group()
    check_mode.add_argument("--reference-only", action="store_true")
    check_mode.add_argument("--full-replay", action="store_true")
    parser.add_argument(
        "--verify-source",
        type=Path,
        help="verify a local fixed-revision GT6 source directory",
    )
    parser.add_argument(
        "--fetch-source",
        type=Path,
        help="fetch and verify only the pinned T12a GT6 source files",
    )
    parser.add_argument(
        "--refresh-declared-source-hashes",
        action="store_true",
        help="refresh only the declared upstream source hashes",
    )
    args = parser.parse_args()
    if args.refresh_declared_source_hashes and (
        args.check
        or args.verify_source is not None
        or args.fetch_source is not None
    ):
        parser.error(
            "--refresh-declared-source-hashes is an exclusive write command"
        )
    if (
        args.reference_only or args.full_replay
    ) and not args.check:
        parser.error("--reference-only and --full-replay require --check")
    policy = load(POLICY)
    if args.fetch_source is not None:
        if args.check or args.verify_source is not None:
            parser.error("--fetch-source is an exclusive evidence command")
        fetch_source(args.fetch_source, policy)
        print(
            "Fetched and verified pinned T12a GT6 source under "
            f"{args.fetch_source}"
        )
        return 0

    if args.refresh_declared_source_hashes:
        refresh_declared_source_hashes()
        print("Refreshed declared T12a source hashes")
        return 0

    if args.check:
        errors = (
            reference_only_check()
            if args.reference_only
            else check(args.verify_source)
        )
        if errors:
            print("T12a machine readiness is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        tier = "compact" if args.reference_only else "full replay"
        print(
            "T12a machine readiness matches committed "
            f"{tier} artifacts."
        )
        return 0
    document = write(args.verify_source)
    print(
        "T12a readiness: "
        f"{document['processing_kind_ledger']['counts']['classified']}/25 "
        "processing, "
        f"{document['non_spec_kind_ledger']['counts']['classified']}/12 "
        "non-spec, "
        f"{document['scaling_expected_matrix']['counts']['rows']} "
        "scaling rows"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
