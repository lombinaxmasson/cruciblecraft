#!/usr/bin/env python3
"""Build the T35 census input currentness snapshot.

Records authoritative path/hash/count metadata for T13, T22.5, T14, T30/T31,
worldgen, reachability, acquisition, presentation, and verification debt.
Fails closed on missing or malformed required inputs.
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
POLICY = TOOLS / "t35_census_policy.json"
OUTPUT = TOOLS / "t35_census_inputs.json"
BUILDER = Path(__file__).resolve()
LEGAL_PORTFOLIO_SCOPES = (
    "in_scope_1x",
    "candidate_1x",
    "post_1x",
    "out_of_scope",
)
STORAGE_SCOPE_INPUT = "tools/t35_storage_scope.json"
MACHINE_TRACK_INPUT = "tools/t35_machine_track.json"
STORAGE_SCOPE_FAMILIES = frozenset(
    {
        "bookshelf",
        "bottle_crate",
        "drawer",
        "locker",
        "mass_storage_barrel",
        "mass_storage_box",
        "mass_storage_standard",
        "storage_inserter",
        "mass_storage_logistics",
        "hopper",
        "chest",
        "safe",
        "tank",
        "fluid_container",
        "pump",
        "sorting",
    }
)
STORAGE_SCOPE_CHOICES = {
    "chest": "post_1x",
    "safe": "post_1x",
    "tank": "post_1x",
    "fluid_container": "candidate_1x",
    "pump": "post_1x",
    "sorting": "post_1x",
    "hopper": "in_scope_1x",
}
LOCKER_SOURCE_BEHAVIORS = {
    "base": {
        "behavior_class": "MultiTileEntityLocker",
        "source_id_expression": "7300+aID",
        "display_expression": '"Locker (" +aMat.getLocal()+")"',
    },
    "charging": {
        "behavior_class": "MultiTileEntityLockerCharging",
        "source_id_expression": "7500+aID",
        "display_expression": '"Charging Locker (" +aMat.getLocal()+")"',
        "behavior_diff": (
            "Adds item charging to locker inventory; retains locker storage lineage."
        ),
    },
}


def _require_file(path: Path, label: str) -> None:
    if not path.is_file():
        raise FileNotFoundError(f"missing required input {label}: {common.relative(path)}")


def _input_record(path: Path) -> dict[str, Any]:
    _require_file(path, "input")
    return {
        "path": common.relative(path),
        "sha256": common.sha256_file(path),
    }


def _narrative_input_record(path: Path) -> dict[str, Any]:
    _require_file(path, "narrative input")
    return {
        "path": common.relative(path),
        "currentness": "narrative",
    }


def _optional_input_record(path: Path) -> dict[str, Any]:
    if path.exists() and not path.is_file():
        raise ValueError(
            f"optional input must be a file when present: {common.relative(path)}"
        )
    record = {"path": common.relative(path)}
    if path.is_file():
        record.update({"status": "available", "sha256": common.sha256_file(path)})
    else:
        record["status"] = "bootstrap_pending"
    return record


def _require_mapping(value: Any, label: str) -> dict[str, Any]:
    if not isinstance(value, dict):
        raise ValueError(f"{label} must be an object")
    return value


def _require_exact_keys(
    value: dict[str, Any],
    expected: set[str],
    label: str,
) -> None:
    actual = set(value)
    missing = sorted(expected - actual)
    extra = sorted(actual - expected)
    if missing or extra:
        raise ValueError(
            f"{label} keys must exactly equal {sorted(expected)}; "
            f"missing={missing} extra={extra}"
        )


def _require_exact_int(
    value: dict[str, Any],
    key: str,
    expected: int,
    label: str,
) -> None:
    actual = value.get(key)
    if type(actual) is not int or actual != expected:
        raise ValueError(
            f"{label}.{key} must be integer {expected}, got {actual!r}"
        )


def _require_nonempty_string(
    value: dict[str, Any],
    key: str,
    label: str,
) -> str:
    actual = value.get(key)
    if not isinstance(actual, str) or not actual.strip():
        raise ValueError(f"{label}.{key} must be a non-empty string")
    return actual


def _validate_policy(policy: dict[str, Any]) -> None:
    if policy.get("source_revision") != common.SOURCE_REVISION:
        raise ValueError("T35 census policy revision drifted")
    expected_scopes = list(LEGAL_PORTFOLIO_SCOPES)
    if policy.get("portfolio_scopes") != expected_scopes:
        raise ValueError(
            "T35 policy portfolio_scopes must exactly equal "
            f"{expected_scopes}, got {policy.get('portfolio_scopes')!r}"
        )
    domains = _require_mapping(policy.get("input_domains"), "T35 policy input_domains")
    for key, expected_path in {
        "t35_storage_scope": STORAGE_SCOPE_INPUT,
        "t35_machine_track": MACHINE_TRACK_INPUT,
    }.items():
        actual_path = domains.get(key)
        if actual_path != expected_path:
            raise ValueError(
                f"T35 policy input_domains.{key} must equal {expected_path!r}, "
                f"got {actual_path!r}"
            )


def _validate_storage_scope_document(
    document: dict[str, Any],
    policy: dict[str, Any],
) -> None:
    _require_exact_keys(
        document,
        {"schema_version", "status", "source_revision", "denominators", "families"},
        "T35 storage scope",
    )
    if document.get("schema_version") != 1:
        raise ValueError(
            "T35 storage scope schema_version must be 1, "
            f"got {document.get('schema_version')!r}"
        )
    if document.get("status") != "T35_STORAGE_SCOPE":
        raise ValueError(
            "T35 storage scope status must be 'T35_STORAGE_SCOPE', "
            f"got {document.get('status')!r}"
        )
    if document.get("source_revision") != common.SOURCE_REVISION:
        raise ValueError("T35 storage scope revision is not the fixed GT6 revision")

    denominators = _require_mapping(
        document.get("denominators"),
        "T35 storage scope denominators",
    )
    _require_exact_keys(
        denominators,
        {"storage", "mass_storage_logistics"},
        "T35 storage scope denominators",
    )
    expected_denominators = {
        "storage": {
            "source_sites": 28,
            "expanded_rows": 624,
            "counts_toward_storage_624": True,
        },
        "mass_storage_logistics": {
            "source_sites": 1,
            "expanded_rows": 1,
            "counts_toward_storage_624": False,
        },
    }
    for name, expected in expected_denominators.items():
        denominator = _require_mapping(
            denominators.get(name),
            f"T35 storage scope denominators.{name}",
        )
        _require_exact_keys(
            denominator,
            set(expected),
            f"T35 storage scope denominators.{name}",
        )
        _require_exact_int(
            denominator,
            "source_sites",
            expected["source_sites"],
            f"T35 storage scope denominators.{name}",
        )
        _require_exact_int(
            denominator,
            "expanded_rows",
            expected["expanded_rows"],
            f"T35 storage scope denominators.{name}",
        )
        if (
            denominator.get("counts_toward_storage_624")
            is not expected["counts_toward_storage_624"]
        ):
            raise ValueError(
                "T35 storage scope denominators."
                f"{name}.counts_toward_storage_624 must be "
                f"{expected['counts_toward_storage_624']!r}, got "
                f"{denominator.get('counts_toward_storage_624')!r}"
            )

    storage_policy = _require_mapping(policy.get("storage_counts"), "T35 policy storage_counts")
    if storage_policy.get("expected_source_sites") != 28:
        raise ValueError(
            "T35 policy storage_counts.expected_source_sites must be 28"
        )
    if storage_policy.get("expected_expanded_registrations") != 624:
        raise ValueError(
            "T35 policy storage_counts.expected_expanded_registrations must be 624"
        )

    families = _require_mapping(document.get("families"), "T35 storage scope families")
    _require_exact_keys(families, set(STORAGE_SCOPE_FAMILIES), "T35 storage scope families")
    dispositions = set(policy.get("dispositions") or [])
    priorities = set(policy.get("portfolio_priorities") or [])
    for name in sorted(STORAGE_SCOPE_FAMILIES):
        family = _require_mapping(families.get(name), f"T35 storage scope families.{name}")
        scope = family.get("portfolio_scope")
        if scope not in LEGAL_PORTFOLIO_SCOPES:
            raise ValueError(
                f"T35 storage scope families.{name}.portfolio_scope must be one of "
                f"{list(LEGAL_PORTFOLIO_SCOPES)}, got {scope!r}"
            )
        disposition = family.get("disposition")
        if disposition not in dispositions:
            raise ValueError(
                f"T35 storage scope families.{name}.disposition must be a policy "
                f"disposition, got {disposition!r}"
            )
        priority = family.get("portfolio_priority")
        if priority not in priorities:
            raise ValueError(
                f"T35 storage scope families.{name}.portfolio_priority must be a "
                f"policy priority, got {priority!r}"
            )
        _require_nonempty_string(family, "owner", f"T35 storage scope families.{name}")

    for name, expected_scope in STORAGE_SCOPE_CHOICES.items():
        actual_scope = families[name].get("portfolio_scope")
        if actual_scope != expected_scope:
            raise ValueError(
                f"T35 storage scope families.{name}.portfolio_scope must be "
                f"{expected_scope!r}, got {actual_scope!r}"
            )

    for name in ("chest", "safe", "tank", "pump", "sorting"):
        track = _require_nonempty_string(
            families[name],
            "subsequent_track",
            f"T35 storage scope families.{name}",
        )
        if not track.startswith("post_1x/"):
            raise ValueError(
                f"T35 storage scope families.{name}.subsequent_track must start "
                f"with 'post_1x/', got {track!r}"
            )
    for key in ("admission_criterion", "measurement_owner", "recheck_epoch"):
        _require_nonempty_string(
            families["fluid_container"],
            key,
            "T35 storage scope families.fluid_container",
        )

    locker = _require_mapping(families.get("locker"), "T35 storage scope families.locker")
    folded = locker.get("folded_source_behaviors")
    if not isinstance(folded, list):
        raise ValueError(
            "T35 storage scope families.locker.folded_source_behaviors must be a list"
        )
    by_role: dict[str, dict[str, Any]] = {}
    for item in folded:
        if not isinstance(item, dict):
            raise ValueError(
                "T35 storage scope locker folded_source_behaviors entries must be objects"
            )
        role = item.get("role")
        if role not in LOCKER_SOURCE_BEHAVIORS or role in by_role:
            raise ValueError(
                "T35 storage scope locker folded_source_behaviors must contain one "
                "base and one charging behavior"
            )
        by_role[role] = item
    if set(by_role) != set(LOCKER_SOURCE_BEHAVIORS):
        raise ValueError(
            "T35 storage scope locker folded_source_behaviors must contain exactly "
            "base and charging behaviors"
        )
    for role, expected in LOCKER_SOURCE_BEHAVIORS.items():
        behavior = by_role[role]
        for key, expected_value in expected.items():
            if behavior.get(key) != expected_value:
                raise ValueError(
                    "T35 storage scope locker "
                    f"{role}.{key} must be {expected_value!r}, got "
                    f"{behavior.get(key)!r}"
                )


def _storage_scope(policy: dict[str, Any]) -> dict[str, Any]:
    scope_path = ROOT / policy["input_domains"]["t35_storage_scope"]
    _require_file(scope_path, "T35 storage scope")
    document = common.load_json(scope_path)
    _validate_storage_scope_document(document, policy)

    source_path = ROOT / policy["storage_counts"]["artifact"]
    source = common.load_json(source_path)
    exclusions = source.get("exclusions") or []
    storage_rows = [
        row
        for row in exclusions
        if isinstance(row, dict) and row.get("category") == "Storage"
    ]
    logistics_rows = [
        row
        for row in exclusions
        if isinstance(row, dict)
        and row.get("behavior_class") == "MultiTileEntityMassStorageLogistics"
        and row.get("category") == "Logistics"
    ]
    actual_counts = {
        "storage": {
            "source_sites": len(storage_rows),
            "expanded_rows": sum(int(row.get("multiplicity") or 0) for row in storage_rows),
        },
        "mass_storage_logistics": {
            "source_sites": len(logistics_rows),
            "expanded_rows": sum(
                int(row.get("multiplicity") or 0) for row in logistics_rows
            ),
        },
    }
    for name, counts in actual_counts.items():
        expected = document["denominators"][name]
        for key, actual in counts.items():
            if actual != expected[key]:
                raise ValueError(
                    f"T35 storage scope {name} {key} {actual} != "
                    f"denominator {expected[key]}"
                )

    return {
        "artifact": policy["input_domains"]["t35_storage_scope"],
        "artifact_sha256": common.sha256_file(scope_path),
        "schema_version": document["schema_version"],
        "denominators": document["denominators"],
        "family_scopes": {
            name: document["families"][name]["portfolio_scope"]
            for name in sorted(STORAGE_SCOPE_FAMILIES)
        },
        "locker_charging_lineage": {
            role: {
                key: behavior[key]
                for key in (
                    "behavior_class",
                    "source_id_expression",
                    "display_expression",
                    *(("behavior_diff",) if role == "charging" else ()),
                )
            }
            for role, behavior in LOCKER_SOURCE_BEHAVIORS.items()
        },
    }


def _t13_tables(policy: dict[str, Any]) -> dict[str, Any]:
    manifest_path = ROOT / policy["input_domains"]["t13_manifest"]
    _require_file(manifest_path, "T13 manifest")
    manifest = common.load_json(manifest_path)
    if (manifest.get("source") or {}).get("revision") != common.SOURCE_REVISION:
        raise ValueError("T13 manifest revision is not the fixed GT6 revision")
    tables_policy = policy["t13_seven_tables"]
    tables: dict[str, Any] = {}
    total = 0
    for name, spec in common.TABLE_SPECS.items():
        expected = tables_policy[name]["expected_canonical_count"]
        artifact = ROOT / spec["artifact"]
        document = common.load_json(artifact)
        ids = common.t13_canonical_ids(name, document)
        actual = len(ids)
        if actual != expected:
            raise ValueError(
                f"T13 {name} canonical count {actual} != expected {expected}"
            )
        tables[name] = {
            "artifact": spec["artifact"],
            "artifact_sha256": common.sha256_file(artifact),
            "canonical_count": actual,
            "id_field": spec["id_field"],
            "unclassified": sum(
                1
                for row in common.t13_rows(name, document)
                if str(row.get(spec["classification_field"]) or "")
                == "unclassified"
            ),
        }
        total += actual
    expected_total = int(tables_policy["expected_canonical_identities_total"])
    if total != expected_total:
        raise ValueError(
            f"T13 seven-table total {total} != expected {expected_total}"
        )
    tables["canonical_identities_total"] = total
    tables["manifest"] = {
        "path": policy["input_domains"]["t13_manifest"],
        "sha256": common.sha256_file(manifest_path),
        "status": manifest.get("status"),
    }
    return tables


def _exclusion_counts(policy: dict[str, Any]) -> dict[str, Any]:
    spec = policy["exclusion_counts"]
    path = ROOT / spec["artifact"]
    document = common.load_json(path)
    counts = document.get("counts") or {}
    sites = int(counts.get("excluded_call_sites") or 0)
    expanded = int(counts.get("excluded_expanded_registrations") or 0)
    if sites != int(spec["excluded_call_sites"]):
        raise ValueError(
            f"exclusion source sites {sites} != {spec['excluded_call_sites']}"
        )
    if expanded != int(spec["excluded_expanded_registrations"]):
        raise ValueError(
            "exclusion expanded registrations "
            f"{expanded} != {spec['excluded_expanded_registrations']}"
        )
    exclusions = document.get(spec["exclusions_key"]) or []
    if not isinstance(exclusions, list):
        raise ValueError("machine_kinds exclusions must be a list")
    if len(exclusions) != sites:
        raise ValueError(
            f"exclusion row count {len(exclusions)} != source sites {sites}"
        )
    multiplicity = sum(int(row.get("multiplicity") or 0) for row in exclusions)
    if multiplicity != expanded:
        raise ValueError(
            f"exclusion multiplicity sum {multiplicity} != expanded {expanded}"
        )
    return {
        "artifact": spec["artifact"],
        "artifact_sha256": common.sha256_file(path),
        "excluded_call_sites": sites,
        "excluded_expanded_registrations": expanded,
        "exclusion_rows": len(exclusions),
    }


def _storage_counts(policy: dict[str, Any]) -> dict[str, Any]:
    spec = policy["storage_counts"]
    path = ROOT / spec["artifact"]
    document = common.load_json(path)
    exclusions = document.get("exclusions") or []
    storage_rows = [
        row
        for row in exclusions
        if str(row.get("category") or "") == spec["category"]
    ]
    sites = len(storage_rows)
    expanded = sum(int(row.get("multiplicity") or 0) for row in storage_rows)
    if sites != int(spec["expected_source_sites"]):
        raise ValueError(
            f"Storage source sites {sites} != {spec['expected_source_sites']}"
        )
    if expanded != int(spec["expected_expanded_registrations"]):
        raise ValueError(
            f"Storage expanded {expanded} != "
            f"{spec['expected_expanded_registrations']}"
        )
    families = sorted(
        {
            str(row.get("behavior_class") or row.get("display_expression") or "")
            for row in storage_rows
        }
    )
    return {
        "artifact": spec["artifact"],
        "artifact_sha256": common.sha256_file(path),
        "category": spec["category"],
        "source_sites": sites,
        "expanded_registrations": expanded,
        "behavior_classes": families,
        "note": spec["note"],
    }


def _t22_5_counts(policy: dict[str, Any]) -> dict[str, Any]:
    spec = policy["t22_5_counts"]
    path = ROOT / spec["classification_artifact"]
    document = common.load_json(path)
    counts = document.get("counts") or {}
    by_class = counts.get("by_class") or {}
    total = int(counts.get("total") or 0)
    ordinary = int(by_class.get("ordinary_optional") or 0)
    unclassified = int(counts.get("unclassified") or 0)
    if total != int(spec["row_universe"]):
        raise ValueError(f"T22.5 row universe {total} != {spec['row_universe']}")
    if ordinary != int(spec["ordinary_optional"]):
        raise ValueError(
            f"T22.5 ordinary_optional {ordinary} != {spec['ordinary_optional']}"
        )
    if unclassified != 0:
        raise ValueError(f"T22.5 unclassified must be 0, got {unclassified}")
    return {
        "classification_artifact": spec["classification_artifact"],
        "classification_sha256": common.sha256_file(path),
        "row_universe": total,
        "ordinary_optional": ordinary,
        "unclassified": unclassified,
    }


def _publication_opening(policy: dict[str, Any]) -> dict[str, Any]:
    spec = policy["publication_opening"]
    projection_path = ROOT / spec["projection_artifact"]
    publication_path = ROOT / policy["input_domains"]["t30_publication"]
    projection = common.load_json(projection_path)
    publication = common.load_json(publication_path)
    opening = projection.get("opening_publication") or {}
    logical = int(opening.get("logical") or 0)
    eager = int(opening.get("eager") or 0)
    lazy = int(opening.get("lazy") or 0)
    if logical != int(spec["expected_logical"]):
        raise ValueError(
            f"publication logical {logical} != {spec['expected_logical']}"
        )
    if eager != int(spec["expected_eager"]):
        raise ValueError(
            f"publication eager {eager} != {spec['expected_eager']}"
        )
    if lazy != int(spec["expected_lazy"]):
        raise ValueError(f"publication lazy {lazy} != {spec['expected_lazy']}")
    projected = publication.get("projected") or {}
    if projected != policy["publication_delta"]:
        raise ValueError("T30 projected publication delta must remain 0/0/0")
    return {
        "authority_chain": spec["authority_chain"],
        "supersedes": spec["supersedes"],
        "logical": logical,
        "eager": eager,
        "lazy": lazy,
        "projection_artifact": spec["projection_artifact"],
        "projection_sha256": common.sha256_file(projection_path),
        "publication_artifact": policy["input_domains"]["t30_publication"],
        "publication_sha256": common.sha256_file(publication_path),
        "publication_delta": policy["publication_delta"],
    }


def _t14_authority(policy: dict[str, Any]) -> dict[str, Any]:
    spec = policy["t14_authority"]
    path = ROOT / spec["policy"]
    document = common.load_json(path)
    budgets = document.get("budgets") or {}
    axis_ids = list(spec["axis_ids"])
    missing = sorted(set(axis_ids) - set(budgets))
    if missing:
        raise ValueError(f"T14 policy missing axes: {missing}")
    return {
        "policy": spec["policy"],
        "policy_sha256": common.sha256_file(path),
        "axis_count": len(axis_ids),
        "axis_ids": axis_ids,
        "hard_ceiling_mutation": spec["hard_ceiling_mutation"],
        "status": document.get("status"),
    }


def _collect_inputs(policy: dict[str, Any]) -> dict[str, Any]:
    domains = policy["input_domains"]
    collected: dict[str, Any] = {}
    collected["roadmap"] = _narrative_input_record(ROOT / domains["roadmap"])
    collected["verification_debt"] = _input_record(
        ROOT / domains["verification_debt"]
    )
    collected["t22_5"] = {
        path: _input_record(ROOT / path) for path in domains["t22_5"]
    }
    collected["t31_load"] = {
        path: _input_record(ROOT / path) for path in domains["t31_load"]
    }
    collected["reachability"] = _input_record(ROOT / domains["reachability"])
    collected["worldgen"] = _input_record(ROOT / domains["worldgen"])
    collected["presentation"] = {
        path: _input_record(ROOT / path) for path in domains["presentation"]
    }
    collected["acquisition"] = {
        path: _input_record(ROOT / path) for path in domains["acquisition"]
    }
    collected["t34_art_manifest"] = _input_record(
        ROOT / domains["t34_art_manifest"]
    )
    collected["t35_storage_scope"] = _input_record(
        ROOT / domains["t35_storage_scope"]
    )
    collected["t35_machine_track"] = _optional_input_record(
        ROOT / domains["t35_machine_track"]
    )
    return collected


def build() -> dict[str, Any]:
    policy = common.load_json(POLICY)
    _validate_policy(policy)
    if policy.get("publication_delta") != {
        "eager": 0,
        "lazy": 0,
        "logical": 0,
    }:
        raise ValueError("T35 publication delta must remain 0/0/0")
    inputs = _collect_inputs(policy)
    storage_scope = _storage_scope(policy)
    currentness_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(POLICY): common.sha256_file(POLICY),
        inputs["t35_storage_scope"]["path"]: inputs["t35_storage_scope"]["sha256"],
    }
    machine_track = inputs["t35_machine_track"]
    if machine_track.get("status") == "available":
        currentness_inputs[machine_track["path"]] = machine_track["sha256"]
    return {
        "schema_version": 1,
        "status": "T35_CENSUS_INPUTS",
        "source_revision": common.SOURCE_REVISION,
        "generated_by": "python tools/build_t35_census_inputs.py",
        "policy": common.relative(POLICY),
        "policy_sha256": common.sha256_file(POLICY),
        "currentness": {
            "owned_inputs": currentness_inputs
        },
        "publication_delta": policy["publication_delta"],
        "fixed_card_nodes": policy["fixed_card_nodes"],
        "generated_topology_card_count_before_census": policy[
            "generated_topology_card_count_before_census"
        ],
        "inputs": inputs,
        "t13_seven_tables": _t13_tables(policy),
        "exclusion_counts": _exclusion_counts(policy),
        "storage_counts": _storage_counts(policy),
        "storage_scope": storage_scope,
        "t22_5_counts": _t22_5_counts(policy),
        "publication_opening": _publication_opening(policy),
        "t14_authority": _t14_authority(policy),
        "vocabularies": {
            "dispositions": policy["dispositions"],
            "portfolio_priorities": policy["portfolio_priorities"],
            "portfolio_scopes": policy["portfolio_scopes"],
            "closure_statuses": policy["axes"]["closure_statuses"],
            "fidelity_statuses": policy["axes"]["fidelity_statuses"],
            "load_statuses": policy["axes"]["load_statuses"],
            "pending_load_verdict": policy["axes"]["pending_load_verdict"],
        },
    }


def check() -> list[str]:
    errors: list[str] = []
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = common.stable_json(build())
    actual = OUTPUT.read_text(encoding="utf-8")
    if actual != expected:
        errors.append(f"{common.relative(OUTPUT)} is stale")
    return errors


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args(argv)
    if args.check == args.write:
        parser.error("choose exactly one of --check or --write")
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = common.load_json(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T35 census inputs failed: {error}", file=sys.stderr)
        return 1
    summary = {
        "status": document.get("status"),
        "canonical_identities": (document.get("t13_seven_tables") or {}).get(
            "canonical_identities_total"
        ),
        "exclusion_sites": (document.get("exclusion_counts") or {}).get(
            "excluded_call_sites"
        ),
        "storage_sites": (document.get("storage_counts") or {}).get("source_sites"),
        "ordinary_optional": (document.get("t22_5_counts") or {}).get(
            "ordinary_optional"
        ),
    }
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
