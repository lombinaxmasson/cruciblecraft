#!/usr/bin/env python3
"""portfolio/logistics-cover-net-r0: freeze seven T13 logistics cover kinds."""
from __future__ import annotations

import argparse
import json
import re
import sys
from typing import Any

from tools import closeout_seal
from tools import portfolio_one_x as one_x
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for
from tools.wave_closeout import wave_dir

SLUG = "portfolio/logistics-cover-net-r0"
PREDECESSOR = "portfolio/generic-recipe-generator"
PREDECESSOR_STATUS = "GENERIC_RECIPE_IMPORT_READY"
STATUS = "LOGISTICS_COVER_NET_R0_READY"
SOURCE_REVISION = census.SOURCE_REVISION
GENERATED_BY = "python tools/build_logistics_cover_net_r0.py"
NOTE = (
    "LOGISTICS_COVER_NET_R0_READY. Seven T13 logistics cover kinds inherited "
    "byte-identical. The T19 9/8 cover stack cannot express network-aware "
    "storage, transfer, or dump. Feasibility is requires_new_runtime. No core "
    "child is assigned. unique_active_wave is null."
)
PINNED_KINDS = (
    "logistics_fluid_storage",
    "logistics_fluid_transfer",
    "logistics_generic_dump",
    "logistics_generic_storage",
    "logistics_generic_transfer",
    "logistics_item_storage",
    "logistics_item_transfer",
)
DISPLAY_CPU_KINDS = (
    "logistics_display_cpu_control",
    "logistics_display_cpu_conversion",
    "logistics_display_cpu_logic",
    "logistics_display_cpu_storage",
)
FEASIBILITY_VALUES = (
    "bounded_extension",
    "requires_new_runtime",
    "blocked",
)
FORBIDDEN_SUCCESSORS = (
    "logistics-cover-net-core",
    "t13c",
    "combinatorial",
    "nuclear",
    "count-ceiling-kind-envelope",
)
STORAGE_LOCK_ENDPOINT = "cruciblecraft:mass_storage_logistics_6200"
EXPECTED_DEFINITION_COUNT = 9
EXPECTED_BEHAVIOR_COUNT = 8
BUILTIN_BEHAVIOR_RE = re.compile(r'registerBuiltin\("([a-z0-9_]+)"')
T13_COVER_KINDS = census.TOOLS / "machine_tree_denominators" / "cover_kinds.json"
T13_POLICY = census.TOOLS / "machine_tree_cover_multiblock_policy.json"
T27_COVER_KINDS = census.TOOLS / "ledger_portfolio" / "cover_kinds.json"
GROWTH_ORDER = (
    census.TOOLS / "waves" / "portfolio" / "source-capability-map" / "growth_order.json"
)
COVER_DEFINITIONS = (
    census.ROOT / "src/main/resources/data/cruciblecraft/cover_definitions.json"
)
COVER_BEHAVIOR_REGISTRY = (
    census.ROOT
    / "src/main/java/com/masson/cruciblecraft/logistics/pipe/cover"
    / "CoverBehaviorRegistry.java"
)
LEFTOVER_LATER = (
    census.TOOLS / "waves" / "portfolio" / "source-capability-map-r0" / "leftover_later.json"
)
RECIPE_GENERATED = census.ROOT / "src" / "recipe_generated"
NETWORK_QUESTIONS = {
    "dump_policy": (
        "How dump differs from transfer and storage, and what a dump cover "
        "flushes or discards on the network."
    ),
    "fail_closed": (
        "Unknown kind or unknown direction must not silent no-op."
    ),
    "identity": (
        "How a cover joins and leaves a logistics identity."
    ),
    "load": (
        "A later implementation card measures player acquisition, network/save "
        "and server behavior separately; it cannot share a machine or energy owner."
    ),
    "routing": (
        "How transfer direction variants address non-adjacent endpoints."
    ),
    "save_sync": (
        "How the network and cover configuration enter the save."
    ),
    "storage_policy": (
        "How a storage kind declares inventory that the network may query."
    ),
}


def generated_by(_slug: str = SLUG) -> str:
    return GENERATED_BY


def nuclear_started() -> bool:
    return one_x.nuclear_started()


def leftover_later_count() -> int:
    leftover = census.load_json(LEFTOVER_LATER)
    total = int(leftover["counts"]["total"])
    if total != 39:
        raise ValueError(f"leftover_later_count {total} != 39")
    return total


def require_registered(slug: str) -> list[str]:
    errors: list[str] = []
    if slug not in KNOWN_SEMANTIC_SLUGS:
        errors.append(f"{slug} missing from KNOWN_SEMANTIC_SLUGS")
    if slug not in known_slugs():
        errors.append(f"{slug} missing from wave_closeout")
    return errors


def require_predecessor() -> list[str]:
    errors = closeout_seal.check_wave_seal(PREDECESSOR)
    readiness = census.load_json(wave_dir(PREDECESSOR) / "readiness.json")
    if readiness.get("status") != PREDECESSOR_STATUS:
        errors.append(
            f"{PREDECESSOR} status {readiness.get('status')} != {PREDECESSOR_STATUS}"
        )
    return errors


def load_t13_kinds() -> dict[str, dict[str, Any]]:
    document = census.load_json(T13_COVER_KINDS)
    rows = {
        str(row["canonical_id"]): dict(row)
        for row in document["canonical_kinds"]
    }
    return rows


def load_t13_transforms() -> dict[str, dict[str, Any]]:
    document = census.load_json(T13_COVER_KINDS)
    return {
        str(row["canonical_id"]): dict(row)
        for row in document["declared_transformations"]
        if str(row["canonical_id"]).startswith("logistics_")
    }


def load_t27_rows() -> dict[str, dict[str, Any]]:
    document = census.load_json(T27_COVER_KINDS)
    return {
        str(row["canonical_id"]): dict(row)
        for row in document["records"]
    }


def authority_hashes() -> dict[str, str]:
    return {
        "growth_order": census.sha256_file(GROWTH_ORDER),
        "machine_tree_cover_kinds": census.sha256_file(T13_COVER_KINDS),
        "machine_tree_cover_multiblock_policy": census.sha256_file(T13_POLICY),
        "ledger_cover_kinds": census.sha256_file(T27_COVER_KINDS),
    }


def pinned_kind_rows() -> list[dict[str, Any]]:
    rows = load_t13_kinds()
    missing = [kind for kind in PINNED_KINDS if kind not in rows]
    if missing:
        raise ValueError(f"T13 missing logistics kinds: {missing}")
    return [dict(rows[kind]) for kind in PINNED_KINDS]


def display_cpu_rows() -> list[dict[str, Any]]:
    rows = load_t13_kinds()
    missing = [kind for kind in DISPLAY_CPU_KINDS if kind not in rows]
    if missing:
        raise ValueError(f"T13 missing display CPU kinds: {missing}")
    out: list[dict[str, Any]] = []
    for kind in DISPLAY_CPU_KINDS:
        row = rows[kind]
        if row.get("disposition") != "out_of_scope":
            raise ValueError(f"{kind} disposition is {row.get('disposition')}")
        out.append(
            {
                "canonical_id": kind,
                "disposition": row["disposition"],
                "implementation_status": row["implementation_status"],
                "roadmap_bucket": row["roadmap_bucket"],
            }
        )
    return out


def logistics_core_row() -> dict[str, Any]:
    policy = census.load_json(T13_POLICY)
    kinds = load_t13_kinds()
    if "logistics_core" in kinds:
        raise ValueError("logistics_core must not be a cover kind")
    domain = policy["domains"]["multiblock_kinds"]
    canonical = next(
        row
        for row in domain["canonical_rules"]
        if row["canonical_id"] == "logistics_core"
    )
    classification = next(
        row
        for row in domain["classification_assignments"]
        if row["canonical_id"] == "logistics_core"
    )
    return {
        "canonical_id": "logistics_core",
        "canonical_rule_id": canonical["id"],
        "classification_rule_id": classification["id"],
        "disposition": classification["disposition"],
        "domain": "multiblock_kinds",
        "implementation_status": classification["implementation_status"],
        "reason": classification["reason"],
        "roadmap_bucket": classification["roadmap_bucket"],
        "source_symbols": list(canonical["source_symbols"]),
    }


def t27_overlays() -> list[dict[str, Any]]:
    rows = load_t27_rows()
    out: list[dict[str, Any]] = []
    for kind in PINNED_KINDS:
        row = rows[kind]
        load_axis = row["axes"]["load"]
        if load_axis.get("status") != "pending":
            raise ValueError(f"{kind} T27 load status is {load_axis.get('status')}")
        if load_axis.get("verdict") != "BLOCKED_PENDING_MEASUREMENT":
            raise ValueError(
                f"{kind} T27 load verdict is {load_axis.get('verdict')}"
            )
        if row.get("disposition") in {"complete", "v1_required"}:
            raise ValueError(f"{kind} T27 disposition is {row.get('disposition')}")
        replacement = str(row.get("replacement_condition") or "")
        if "separately" not in replacement:
            raise ValueError(f"{kind} T27 replacement_condition lost separate axes")
        out.append(
            {
                "axes_load": {
                    "status": load_axis["status"],
                    "verdict": load_axis["verdict"],
                },
                "canonical_id": kind,
                "cc_implementation": row["cc_implementation"],
                "disposition": row["disposition"],
                "owner": row["owner"],
                "replacement_condition": row["replacement_condition"],
                "machine_tree_classification": row["machine_tree_classification"],
            }
        )
    return out


def inherited_denominator_document() -> dict[str, Any]:
    kinds = pinned_kind_rows()
    for row in kinds:
        if row.get("cc_behavior") is not None:
            raise ValueError(f"{row['canonical_id']} cc_behavior is not null")
        if row.get("disposition") != "deferred_with_reason":
            raise ValueError(
                f"{row['canonical_id']} disposition is {row.get('disposition')}"
            )
        if row.get("implementation_status") != "deferred_with_reason":
            raise ValueError(
                f"{row['canonical_id']} implementation_status is "
                f"{row.get('implementation_status')}"
            )
    return {
        "authority_hashes": authority_hashes(),
        "display_cpu_out_of_scope": display_cpu_rows(),
        "generated_by": GENERATED_BY,
        "kind_count": len(kinds),
        "kinds": kinds,
        "logistics_core": logistics_core_row(),
        "schema_version": 1,
        "source_artifact": census.relative(T13_COVER_KINDS),
        "source_revision": SOURCE_REVISION,
        "status": "INHERITED_DENOMINATOR_READY",
        "ledger_overlays": t27_overlays(),
        "wave_slug": SLUG,
    }


def kind_role(canonical_id: str) -> str:
    if canonical_id.endswith("_transfer"):
        return "transfer"
    if canonical_id.endswith("_storage"):
        return "storage"
    if canonical_id.endswith("_dump"):
        return "dump"
    raise ValueError(f"{canonical_id} has no transfer/storage/dump role")


def kind_medium(canonical_id: str) -> str:
    if "_fluid_" in canonical_id:
        return "fluid"
    if "_item_" in canonical_id:
        return "item"
    if "_generic_" in canonical_id:
        return "generic"
    raise ValueError(f"{canonical_id} has no item/fluid/generic medium")


def source_semantics_document() -> dict[str, Any]:
    kinds = pinned_kind_rows()
    transforms = load_t13_transforms()
    rows: list[dict[str, Any]] = []
    for kind in kinds:
        canonical_id = str(kind["canonical_id"])
        role = kind_role(canonical_id)
        transform = transforms.get(canonical_id)
        collapse = None
        if transform is not None:
            collapse = {
                "id": transform["id"],
                "raw_members": list(transform["raw_members"]),
                "source_evidence": transform["source_evidence"],
                "statement": transform["statement"],
                "variant_dimensions": list(transform["variant_dimensions"]),
            }
            if "direction" not in transform["variant_dimensions"]:
                raise ValueError(f"{canonical_id} collapse is not direction")
        elif role == "transfer":
            raise ValueError(f"{canonical_id} transfer is missing T13 collapse")
        rows.append(
            {
                "canonical_id": canonical_id,
                "direction_collapse": collapse,
                "evidence_class": "SOURCE_BACKED",
                "medium": kind_medium(canonical_id),
                "normalization_reason": kind["normalization_reason"],
                "raw_members": list(kind["raw_members"]),
                "role": role,
                "scope_reason": kind["scope_reason"],
            }
        )
    roles = {row["role"] for row in rows}
    if roles != {"dump", "storage", "transfer"}:
        raise ValueError(f"source semantics roles drifted: {sorted(roles)}")
    return {
        "generated_by": GENERATED_BY,
        "kinds": rows,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "SOURCE_SEMANTICS_READY",
        "wave_slug": SLUG,
    }


def cover_definition_ids() -> list[str]:
    document = census.load_json(COVER_DEFINITIONS)
    ids = [str(row["id"]) for row in document["definitions"]]
    if len(ids) != EXPECTED_DEFINITION_COUNT:
        raise ValueError(f"cover definition count {len(ids)} != 9")
    logistics = [item for item in ids if "logistics_" in item]
    if logistics:
        raise ValueError(f"cover_definitions grew logistics ids: {logistics}")
    return ids


def builtin_behavior_ids() -> list[str]:
    text = COVER_BEHAVIOR_REGISTRY.read_text(encoding="utf-8")
    ids = [f"cruciblecraft:{name}" for name in BUILTIN_BEHAVIOR_RE.findall(text)]
    if len(ids) != EXPECTED_BEHAVIOR_COUNT:
        raise ValueError(f"builtin behavior count {len(ids)} != 8")
    return ids


def existing_mechanism_document() -> dict[str, Any]:
    definitions = cover_definition_ids()
    behaviors = builtin_behavior_ids()
    kinds = pinned_kind_rows()
    mapped: list[dict[str, Any]] = []
    for kind in kinds:
        canonical_id = str(kind["canonical_id"])
        role = kind_role(canonical_id)
        analog: str | None
        if role == "transfer":
            analog = "T19 adjacent activeTransfer (pump/conveyor/retriever/robot_arm)"
        elif role == "storage":
            analog = STORAGE_LOCK_ENDPOINT
        else:
            analog = None
        mapped.append(
            {
                "adjacent_analog": analog,
                "canonical_id": canonical_id,
                "cc_mechanism": "none",
                "gap": "mechanism",
                "role": role,
            }
        )
    return {
        "behaviors": behaviors,
        "behavior_count": len(behaviors),
        "cover_definitions_sha256": census.sha256_file(COVER_DEFINITIONS),
        "definition_count": len(definitions),
        "definitions": definitions,
        "generated_by": GENERATED_BY,
        "kinds": mapped,
        "note": (
            "The current 9/8 cover stack cannot express network-aware storage, "
            "transfer, or dump. The gap is a missing mechanism, not a missing "
            "cover_definitions row."
        ),
        "pipe_cover_runtime": "src/main/java/com/masson/cruciblecraft/logistics/pipe/cover",
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "EXISTING_MECHANISM_READY",
        "storage_endpoint": {
            "id": STORAGE_LOCK_ENDPOINT,
            "role": "endpoint_not_cover",
            "source": "storage/lock mass_storage_logistics 1/1",
        },
        "wave_slug": SLUG,
    }


def network_contract_document() -> dict[str, Any]:
    return {
        "generated_by": GENERATED_BY,
        "implemented": False,
        "questions": dict(NETWORK_QUESTIONS),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "NETWORK_CONTRACT_READY",
        "wave_slug": SLUG,
    }


def derive_feasibility(
    inherited: dict[str, Any],
    semantics: dict[str, Any],
    mechanism: dict[str, Any],
    contract: dict[str, Any],
) -> dict[str, Any]:
    missing: list[str] = []
    roles = {row["role"] for row in semantics["kinds"]}
    if "dump" not in roles:
        missing.append("dump")
    dump_rows = [row for row in semantics["kinds"] if row["role"] == "dump"]
    if dump_rows and "distinct from import/export and storage" not in dump_rows[0][
        "normalization_reason"
    ]:
        missing.append("dump")
    if set(contract["questions"]) != set(NETWORK_QUESTIONS):
        missing.append("schema")
    overlays = inherited["ledger_overlays"]
    if len(overlays) != 7:
        missing.append("measurement")
    for row in overlays:
        if "separately" not in str(row["replacement_condition"]):
            missing.append("measurement")
            break
    if inherited["logistics_core"]["domain"] != "multiblock_kinds":
        missing.append("schema")
    network_capable = False
    if any(row["cc_mechanism"] != "none" for row in mechanism["kinds"]):
        network_capable = True
    if "logistics_" in " ".join(mechanism["definitions"]):
        network_capable = True
    reasons = [
        "T13 scope_reason on all seven kinds names a later network runtime or "
        "exceeds adjacent T19 transfer.",
        "Cover definitions stay at 9 adjacent T19 rows with no logistics_* id.",
        "CoverBehaviorRegistry stays at 8 adjacent builtins with no network identity.",
        "storage/lock mass_storage_logistics_6200 is an ILogisticsStorage endpoint, not a cover.",
        "logistics_generic_dump is already distinct from transfer and storage.",
        "T27 replacement_condition already requires separate player-acquisition, "
        "network/save, and server-behavior measurement.",
    ]
    if missing:
        verdict = "blocked"
        allows_core_child = False
        status = "BLOCKED"
    elif network_capable:
        verdict = "bounded_extension"
        allows_core_child = True
        status = "FEASIBILITY_READY"
    else:
        verdict = "requires_new_runtime"
        allows_core_child = False
        status = "FEASIBILITY_READY"
    if verdict not in FEASIBILITY_VALUES:
        raise ValueError(f"illegal feasibility {verdict}")
    return {
        "allows_core_child": allows_core_child,
        "generated_by": GENERATED_BY,
        "missing_evidence": sorted(set(missing)),
        "reasons": reasons,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": status,
        "verdict": verdict,
        "wave_slug": SLUG,
    }


def recipe_generated_mentions_slug() -> bool:
    if not RECIPE_GENERATED.is_dir():
        return False
    token = "logistics-cover-net"
    for path in RECIPE_GENERATED.rglob("*"):
        if token in path.as_posix():
            return True
    return False


def evidence_document(
    inherited: dict[str, Any],
    feasibility: dict[str, Any],
) -> dict[str, Any]:
    leftover = leftover_later_count()
    return {
        "allows_core_child": feasibility["allows_core_child"],
        "completion_delta": 0,
        "display_cpu_out_of_scope": len(inherited["display_cpu_out_of_scope"]),
        "feasibility": feasibility["verdict"],
        "generated_recipe_count": 0,
        "inherited_kind_count": inherited["kind_count"],
        "leftover_later_count": leftover,
        "logistics_core_is_multiblock": True,
        "nuclear_track_c_started": False,
        "owns_families": 0,
        "partial_family_count": 0,
        "production_lock": None,
        "recipe_files_generated": False,
    }


def common_documents(*, evidence: dict[str, Any]) -> dict[str, Any]:
    spec = spec_for(SLUG)
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": GENERATED_BY,
        "leftover_later_count": leftover_later_count(),
        "partial_family_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": SLUG,
        "work_set": {"family_count": 0, "source_rows": 0},
    }
    topology = {
        "append_only": False,
        "complete_family_count": 0,
        "generated_by": GENERATED_BY,
        "next_unassigned": spec.next_unassigned,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": spec.unique_active_wave,
        "wave_slug": SLUG,
    }
    readiness = {
        "evidence": evidence,
        "generated_by": GENERATED_BY,
        "next_unassigned": spec.next_unassigned,
        "note": NOTE,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": STATUS,
        "unique_active_wave": spec.unique_active_wave,
        "wave_complete": True,
        "wave_slug": SLUG,
    }
    wave = {
        "cohort": "logistics-cover-net-r0",
        "depends_on": [PREDECESSOR],
        "generated_by": GENERATED_BY,
        "owns_families": 0,
        "program": SLUG,
        "schema_version": 1,
        "wave_slug": SLUG,
    }
    return {
        "census_delta.json": census,
        "readiness.json": readiness,
        "topology.json": topology,
        "wave.json": wave,
    }


def build_r0_documents() -> dict[str, Any]:
    errors = require_predecessor()
    if errors:
        raise ValueError("; ".join(errors))
    inherited = inherited_denominator_document()
    semantics = source_semantics_document()
    mechanism = existing_mechanism_document()
    contract = network_contract_document()
    feasibility = derive_feasibility(inherited, semantics, mechanism, contract)
    if feasibility["verdict"] == "blocked":
        raise ValueError(
            "feasibility is blocked: " + ", ".join(feasibility["missing_evidence"])
        )
    if recipe_generated_mentions_slug():
        raise ValueError("src/recipe_generated mentions logistics-cover-net")
    evidence = evidence_document(inherited, feasibility)
    documents = common_documents(evidence=evidence)
    documents["inherited_denominator.json"] = inherited
    documents["source_semantics.json"] = semantics
    documents["existing_mechanism.json"] = mechanism
    documents["network_contract.json"] = contract
    documents["feasibility.json"] = feasibility
    return documents


def write_seal() -> dict[str, Any]:
    root = wave_dir(SLUG)
    hashes = {
        "census": census.sha256_file(root / "census_delta.json"),
        "gametest_java": None,
        "gametest_log": None,
        "generated_recipes": None,
        "locked_support": None,
        "production_lock": None,
        "publication_group_manifest": None,
        "readiness": census.sha256_file(root / "readiness.json"),
        "receipt": None,
        "runtime_dependency_manifest": None,
        "shard_manifest": None,
        "topology": census.sha256_file(root / "topology.json"),
    }
    seal = {
        "card_id": SLUG,
        "complete_family_count": 0,
        "composed_identity_ledger_v2_sha256": census.sha256_file(
            closeout_seal.IDENTITY_LEDGER_V2
        ),
        "composed_runtime_manifest_v2_sha256": census.sha256_file(
            closeout_seal.RUNTIME_MANIFEST_V2
        ),
        "gametest_status": "NONE",
        "generated_by": f"{GENERATED_BY} --write",
        "hashes": hashes,
        "note": census.load_json(root / "readiness.json").get("note"),
        "production_lock_sha256": None,
        "receipt_sha256": None,
        "reclassification_delta": 0,
        "relation_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "sealed_at_wave": SLUG,
        "source_revision": SOURCE_REVISION,
        "status": "SEALED",
    }
    census.write_stable(root / "closeout_seal.json", seal)
    return seal


def write_artifacts() -> dict[str, Any]:
    documents = build_r0_documents()
    root = wave_dir(SLUG)
    root.mkdir(parents=True, exist_ok=True)
    for name, document in documents.items():
        census.write_stable(root / name, document)
    write_seal()
    spec = spec_for(SLUG)
    return {
        "status": STATUS,
        "unique_active_wave": spec.unique_active_wave,
        "wave_slug": SLUG,
    }


def check_forbidden_successors(spec_unique: str | None) -> list[str]:
    errors: list[str] = []
    haystack = spec_unique or ""
    for token in FORBIDDEN_SUCCESSORS:
        if token in haystack:
            errors.append(f"topology successor {token} is forbidden")
    return errors


def check_artifacts() -> list[str]:
    errors = require_registered(SLUG)
    if errors:
        return errors
    spec = spec_for(SLUG)
    if spec.owns_families != 0:
        errors.append(f"{SLUG} owns_families must be 0")
    if spec.production_lock is not None:
        errors.append(f"{SLUG} must not carry a production lock")
    if spec.unique_active_wave is not None:
        errors.append("unique_active_wave must be null")
    if not spec.next_unassigned:
        errors.append("next_unassigned must be true")
    errors.extend(check_forbidden_successors(spec.unique_active_wave))
    root = wave_dir(SLUG)
    if not (root / "readiness.json").is_file():
        return errors + [f"{SLUG} artifacts are missing"]
    try:
        live = build_r0_documents()
    except ValueError as error:
        return errors + [str(error)]
    for name, document in live.items():
        committed = census.load_json(root / name)
        drift = census.first_json_diff(document, committed)
        if drift:
            errors.append(f"{name} drifted: {drift}")
    readiness = census.load_json(root / "readiness.json")
    if readiness.get("status") != STATUS:
        errors.append(f"{SLUG} status drifted")
    if readiness.get("unique_active_wave") is not None:
        errors.append(f"{SLUG} unique_active_wave must be null")
    if nuclear_started():
        errors.append("nuclear Track C started must stay false")
    evidence = readiness.get("evidence") or {}
    if int(evidence.get("completion_delta", 1)) != 0:
        errors.append(f"{SLUG} completion_delta must be 0")
    if evidence.get("recipe_files_generated"):
        errors.append(f"{SLUG} must not generate recipes")
    if int(evidence.get("leftover_later_count", 0)) != 39:
        errors.append(f"{SLUG} leftover_later_count must be 39")
    feasibility = census.load_json(root / "feasibility.json")
    if feasibility.get("verdict") not in FEASIBILITY_VALUES:
        errors.append("feasibility.verdict is not an allowed enum")
    if feasibility.get("verdict") != "bounded_extension" and feasibility.get(
        "allows_core_child"
    ):
        errors.append("requires_new_runtime/blocked must not allow a core child")
    inherited = census.load_json(root / "inherited_denominator.json")
    live_kinds = {row["canonical_id"]: row for row in pinned_kind_rows()}
    for row in inherited["kinds"]:
        canonical_id = row["canonical_id"]
        drift = census.first_json_diff(live_kinds[canonical_id], row)
        if drift:
            errors.append(f"{canonical_id} is not byte-identical to T13: {drift}")
    if recipe_generated_mentions_slug():
        errors.append("src/recipe_generated mentions logistics-cover-net")
    errors.extend(closeout_seal.check_wave_seal(SLUG))
    return errors


def main_for(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=f"Write or check {SLUG}.")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose --write or --check")
    try:
        if args.write:
            print(json.dumps(write_artifacts(), sort_keys=True))
            return 0
        errors = check_artifacts()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{SLUG} closeout derivation is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"{SLUG} failed: {error}", file=sys.stderr)
        return 1
