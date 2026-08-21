#!/usr/bin/env python3
"""Build the aggregate T13 canonical-denominator manifest and readiness."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
DENOMINATORS = TOOLS / "t13_denominators"
POLICY = TOOLS / "t13_denominator_policy.json"
TREE = TOOLS / "t13_gt6_tree_manifest.json"
SYMBOLS = TOOLS / "t13_source_symbol_inventory.json"
T12 = TOOLS / "t12_closure_readiness.json"
MANIFEST = TOOLS / "t13_denominator_manifest.json"
READINESS = TOOLS / "t13_denominator_readiness.json"

TABLES = {
    "recipe_maps": {
        "artifact": DENOMINATORS / "recipe_maps.json",
        "builder": TOOLS / "build_t13_recipe_map_denominator.py",
        "policy": TOOLS / "t13_recipe_map_policy.json",
        "test": TOOLS / "tests/test_build_t13_recipe_map_denominator.py",
    },
    "prefixes": {
        "artifact": DENOMINATORS / "prefixes.json",
        "builder": TOOLS / "build_t13_prefix_domain_denominators.py",
        "policy": TOOLS / "t13_prefix_domain_policy.json",
        "test": TOOLS / "tests/test_build_t13_prefix_domain_denominators.py",
    },
    "itemgenerator_domains": {
        "artifact": DENOMINATORS / "itemgenerator_domains.json",
        "builder": TOOLS / "build_t13_prefix_domain_denominators.py",
        "policy": TOOLS / "t13_prefix_domain_policy.json",
        "test": TOOLS / "tests/test_build_t13_prefix_domain_denominators.py",
    },
    "machine_kinds": {
        "artifact": DENOMINATORS / "machine_kinds.json",
        "builder": TOOLS / "build_t13_machine_energy_denominators.py",
        "policy": TOOLS / "t13_machine_energy_policy.json",
        "test": TOOLS / "tests/test_build_t13_machine_energy_denominators.py",
    },
    "energy_identities": {
        "artifact": DENOMINATORS / "energy_identities.json",
        "builder": TOOLS / "build_t13_machine_energy_denominators.py",
        "policy": TOOLS / "t13_machine_energy_policy.json",
        "test": TOOLS / "tests/test_build_t13_machine_energy_denominators.py",
    },
    "cover_kinds": {
        "artifact": DENOMINATORS / "cover_kinds.json",
        "builder": TOOLS / "build_t13_cover_multiblock_denominators.py",
        "policy": TOOLS / "t13_cover_multiblock_policy.json",
        "test": TOOLS / "tests/test_build_t13_cover_multiblock_denominators.py",
    },
    "multiblock_kinds": {
        "artifact": DENOMINATORS / "multiblock_kinds.json",
        "builder": TOOLS / "build_t13_cover_multiblock_denominators.py",
        "policy": TOOLS / "t13_cover_multiblock_policy.json",
        "test": TOOLS / "tests/test_build_t13_cover_multiblock_denominators.py",
    },
}


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def stable(document: Any) -> str:
    return json.dumps(
        document, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def classification_counts(
    table: str, document: dict[str, Any]
) -> tuple[dict[str, int], int]:
    if table == "recipe_maps":
        counts = document["counts"]
        return counts["classifications"], counts["unclassified"]
    if table in {"prefixes", "itemgenerator_domains"}:
        summary = document["summary"]
        return (
            summary["classification_counts"],
            summary["unclassified_count"],
        )
    counts = document["counts"]
    return counts["classifications"], counts["unclassified"]


def canonical_count(table: str, document: dict[str, Any]) -> int:
    if table == "recipe_maps":
        return document["counts"]["rows"]
    if table == "prefixes":
        return document["summary"]["op_canonical_count"]
    if table == "itemgenerator_domains":
        return document["summary"]["observed_domain_count"]
    if table == "machine_kinds":
        return document["counts"]["canonical_kinds"]
    if table == "energy_identities":
        return document["counts"]["identities"]
    return document["counts"]["canonical"]


def raw_count(table: str, document: dict[str, Any]) -> int:
    if table == "recipe_maps":
        return document["counts"]["actual_map_files"]
    if table == "prefixes":
        return document["summary"]["raw_dump_count"]
    if table == "itemgenerator_domains":
        return document["summary"]["observed_domain_count"]
    if table == "machine_kinds":
        return document["counts"]["raw_expanded_registrations"]
    if table == "energy_identities":
        return document["counts"]["identities"]
    return document["counts"]["raw_classes"]


def uniform_status(
    table: str, document: dict[str, Any]
) -> tuple[str, list[Any]]:
    if table == "recipe_maps":
        audit = document["normalization_audit"]
        blockers = list(audit["unexplained_field_cardinality_reductions"])
        blockers.extend(audit["undeclared_many_to_one"])
        return audit["status"], blockers
    if table in {"prefixes", "itemgenerator_domains", "cover_kinds", "multiblock_kinds"}:
        audit = document["uniform_audit"]
        blockers = list(audit.get("blockers", []))
        blockers.extend(audit.get("undeclared_many_to_one", []))
        blockers.extend(
            audit.get("unexplained_field_cardinality_reductions", [])
        )
        return audit["status"], blockers
    audit = document["field_cardinality_audit"]
    return audit["status"], list(audit.get("uniform_findings", []))


def build_manifest() -> dict[str, Any]:
    policy = load(POLICY)
    tree = load(TREE)
    symbols = load(SYMBOLS)
    if policy["source"]["revision"] != tree["revision"]:
        raise ValueError("T13 policy/tree revision mismatch")
    if policy["source"]["tree_sha1"] != tree["tree_sha1"]:
        raise ValueError("T13 policy/tree sha mismatch")
    if symbols["source_revision"] != tree["revision"]:
        raise ValueError("T13 symbol inventory revision mismatch")
    if symbols["source_tree_sha1"] != tree["tree_sha1"]:
        raise ValueError("T13 symbol inventory tree mismatch")
    if set(policy["tables"]) != set(TABLES):
        raise ValueError("T13 policy table set drifted")

    rows: dict[str, Any] = {}
    total_unclassified = 0
    uniform_blockers: dict[str, Any] = {}
    for table, paths in TABLES.items():
        for path in paths.values():
            if not path.is_file():
                raise ValueError(f"missing T13 {table} input: {path}")
        document = load(paths["artifact"])
        classes, unclassified = classification_counts(table, document)
        if "unclassified" in classes and classes["unclassified"] != 0:
            unclassified += classes["unclassified"]
        status, blockers = uniform_status(table, document)
        if status not in {"PASS", "UNIFORM_PASS"} or blockers:
            uniform_blockers[table] = {
                "status": status,
                "blockers": blockers,
            }
        total_unclassified += unclassified
        row = {
            "artifact": paths["artifact"].relative_to(ROOT).as_posix(),
            "artifact_sha256": sha256(paths["artifact"]),
            "builder": paths["builder"].relative_to(ROOT).as_posix(),
            "builder_sha256": sha256(paths["builder"]),
            "policy": paths["policy"].relative_to(ROOT).as_posix(),
            "policy_sha256": sha256(paths["policy"]),
            "test": paths["test"].relative_to(ROOT).as_posix(),
            "test_sha256": sha256(paths["test"]),
            "raw_count": raw_count(table, document),
            "canonical_count": canonical_count(table, document),
            "classification_counts": classes,
            "unclassified": unclassified,
            "uniform_status": status,
        }
        if table == "recipe_maps":
            receipt = document.get("full_replay_receipt")
            if not isinstance(receipt, dict) or receipt.get(
                "proof_tier"
            ) != "full_replay":
                raise ValueError("T13 recipe-map full-replay receipt is missing")
            row["full_replay_receipt"] = receipt
        rows[table] = row
    if total_unclassified != 0:
        raise ValueError(
            f"T13 denominator tables leave {total_unclassified} unclassified"
        )
    if uniform_blockers:
        raise ValueError(
            f"T13 denominator normalization blockers: {uniform_blockers}"
        )
    recipe = load(TABLES["recipe_maps"]["artifact"])
    if recipe["counts"]["recipes"] != 720_841:
        raise ValueError("T13 RecipeMap row total drifted")
    if recipe["counts"]["rows"] != 95:
        raise ValueError("T13 RecipeMap denominator drifted")
    return {
        "schema_version": 2,
        "status": "T13_DENOMINATOR_MANIFEST_READY",
        "generated_by": "python tools/build_t13_denominator_readiness.py",
        "source": {
            "repository": policy["source"]["repository"],
            "revision": tree["revision"],
            "tree_sha1": tree["tree_sha1"],
            "global_policy": POLICY.relative_to(ROOT).as_posix(),
            "global_policy_sha256": sha256(POLICY),
            "tree_manifest": TREE.relative_to(ROOT).as_posix(),
            "tree_manifest_sha256": sha256(TREE),
            "source_symbol_inventory": SYMBOLS.relative_to(ROOT).as_posix(),
            "source_symbol_inventory_sha256": sha256(SYMBOLS),
            "selected_java_blobs": tree["integrity"]["selected_tree"]["entries"],
            "symbol_declarations": symbols["counts"]["declarations"],
        },
        "classification_vocabulary": policy[
            "classification_vocabulary"
        ],
        "tables": rows,
        "totals": {
            "tables": len(rows),
            "unclassified": total_unclassified,
            "normalization_blockers": 0,
            "recipe_maps": recipe["counts"]["rows"],
            "recipe_rows": recipe["counts"]["recipes"],
        },
    }


def build_readiness(manifest: dict[str, Any]) -> dict[str, Any]:
    t12 = load(T12)
    policy = load(POLICY)
    if t12["status"] != "T12_READY":
        raise ValueError("T13 requires current T12_READY baseline")
    energy_audit = (
        t12.get("energy", {}).get("processing_machine_audit") or {}
    )
    if (
        energy_audit.get("status")
        != "PROCESSING_MACHINE_ENERGY_AUDIT_READY"
        or energy_audit.get("machine_specs") != 25
        or energy_audit.get("implicit_energy_arguments") != 0
        or energy_audit.get("new_legacy_kinetic") != 0
    ):
        raise ValueError("T13 requires the current 25-machine energy audit")
    load_gate = t12["load_gate"]
    expected_zero_content = policy["zero_content_delta"]
    if (
        load_gate["datapack_recipe_entries"]
        != expected_zero_content["datapack_recipe_entries"]
        or load_gate["published_recipes"] != 18_875
    ):
        raise ValueError("T13 zero-content baseline drifted")
    table_counts = {
        name: {
            "raw": row["raw_count"],
            "canonical": row["canonical_count"],
            "classifications": row["classification_counts"],
        }
        for name, row in manifest["tables"].items()
    }
    return {
        "schema_version": 2,
        "status": "T13_READY",
        "delivery_boundary": "T13_CANONICAL_DENOMINATORS",
        "source_revision": manifest["source"]["revision"],
        "manifest": {
            "path": MANIFEST.relative_to(ROOT).as_posix(),
            "sha256": hashlib.sha256(
                stable(manifest).encode("utf-8")
            ).hexdigest(),
        },
        "acceptance": {
            "tables": 7,
            "unclassified": manifest["totals"]["unclassified"],
            "normalization_blockers": manifest["totals"][
                "normalization_blockers"
            ],
            "recipe_maps": manifest["totals"]["recipe_maps"],
            "recipe_rows": manifest["totals"]["recipe_rows"],
            "o_33": "CLOSED",
            "canonical_completion_metric": (
                "canonical denominator plus explicit classification; "
                "live publication/source rows is not a completion ratio"
            ),
        },
        "denominators": table_counts,
        "zero_content_delta": {
            "datapack_recipe_entries": load_gate[
                "datapack_recipe_entries"
            ],
            "published_recipes": load_gate["published_recipes"],
            "datapack_delta": 0,
            "publication_delta": 0,
        },
        "source_replay": {
            "ordinary_check": (
                "python tools/build_t13_recipe_map_denominator.py --check "
                "--reference-only"
            ),
            "hash_fast_check": (
                "python tools/build_t13_recipe_map_denominator.py --check "
                "--hash-fast"
            ),
            "full_replay_check": (
                "python tools/build_t13_recipe_map_denominator.py --check "
                "--full-replay"
            ),
            "fetch_command": (
                "python tools/build_t13_recipe_map_denominator.py "
                "--fetch-source build/t13-gt6-source "
                "--write-source-inventory"
            ),
            "verify_command": (
                "python tools/build_t13_recipe_map_denominator.py --check "
                "--full-replay --verify-source build/t13-gt6-source"
            ),
            "missing_full_source_policy": "explicit SKIP, never PASS",
        },
        "downstream_contract": {
            "T14": ["recipe_maps", "prefixes", "itemgenerator_domains"],
            "T15": ["machine_kinds", "prefixes"],
            "T16": ["machine_kinds", "energy_identities"],
            "T17": ["machine_kinds", "energy_identities", "recipe_maps"],
            "T18": ["machine_kinds", "energy_identities", "recipe_maps"],
            "T19": ["cover_kinds", "prefixes"],
            "post_T19": ["multiblock_kinds", "itemgenerator_domains"],
        },
        "currentness": {
            "owned_inputs": {
                BUILDER.relative_to(ROOT).as_posix(): sha256(BUILDER),
                POLICY.relative_to(ROOT).as_posix(): sha256(POLICY),
            },
            "dependencies": {
                T12.relative_to(ROOT).as_posix(): sha256(T12),
                MANIFEST.relative_to(ROOT).as_posix(): hashlib.sha256(
                    stable(manifest).encode("utf-8")
                ).hexdigest(),
            },
            "processing_machine_energy_audit": energy_audit,
            "pending_report": {
                "status": "BOUND_TO_FULL_VERIFICATION_REPORT",
                "this_refresh_final_closure_attempted": True,
                "pending": [],
                "evidence": "tools/full_verification_report.json",
            },
        },
    }


def write_or_check(path: Path, encoded: str, check: bool) -> None:
    if check:
        if not path.is_file() or path.read_text(
            encoding="utf-8"
        ) != encoded:
            raise ValueError(f"stale generated file: {path}")
    else:
        path.write_bytes(encoded.encode("utf-8"))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    manifest = build_manifest()
    readiness = build_readiness(manifest)
    try:
        write_or_check(MANIFEST, stable(manifest), args.check)
        write_or_check(READINESS, stable(readiness), args.check)
    except (OSError, ValueError) as error:
        print(f"T13 denominator readiness failed: {error}")
        return 1
    print(
        json.dumps(
            {
                "status": readiness["status"],
                "tables": readiness["acceptance"]["tables"],
                "recipe_maps": readiness["acceptance"]["recipe_maps"],
                "recipe_rows": readiness["acceptance"]["recipe_rows"],
                "unclassified": readiness["acceptance"]["unclassified"],
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
