#!/usr/bin/env python3
"""Build the fixed-source T18 machine and energy denominator."""
from __future__ import annotations

import argparse
import hashlib
import json
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "t18_machine_energy_denominator_policy.json"
OUTPUT = TOOLS / "t18_machine_energy_denominator.json"
MACHINE_DENOMINATOR = TOOLS / "t13_denominators/machine_kinds.json"
ENERGY_DENOMINATOR = TOOLS / "t13_denominators/energy_identities.json"
CONVERTER_CATALOG = (
    ROOT / "src/main/resources/data/cruciblecraft/energy_converters.json"
)
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
DISPOSITIONS = {
    "SELECTED_T18",
    "PREIMPLEMENTED_REFERENCE",
    "DEFERRED_WITH_REASON",
}
DEFERRED_FIELDS = ("reason", "replacement_condition", "recheck_point")


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def require_deferred_contract(row: dict[str, Any], owner: str) -> None:
    for field in DEFERRED_FIELDS:
        if not str(row.get(field) or "").strip():
            raise ValueError(f"{owner}: deferred row lacks {field}")


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status")
        != "T18_MACHINE_ENERGY_DENOMINATOR_POLICY"
        or policy.get("source_revision") != SOURCE_REVISION
        or policy.get("owner") != "T18"
        or policy.get("energy_identities")
        != {"AU": "AIR", "STEAM": "STEAM"}
    ):
        raise ValueError("T18 denominator policy header drifted")
    if set(policy.get("dispositions") or []) != DISPOSITIONS:
        raise ValueError("T18 disposition vocabulary drifted")
    kinds = policy.get("kinds")
    if not isinstance(kinds, dict) or len(kinds) != 29:
        raise ValueError("T18 policy must classify exactly 29 machine kinds")
    selected_source_ids: set[int] = set()
    for behavior, row in kinds.items():
        disposition = row.get("disposition")
        if disposition not in DISPOSITIONS:
            raise ValueError(f"{behavior}: invalid T18 disposition")
        if disposition == "SELECTED_T18":
            source_id = row.get("selected_source_id")
            profile = row.get("converter_profile")
            if (
                not isinstance(source_id, int)
                or source_id <= 0
                or not str(profile or "").startswith("cruciblecraft:")
            ):
                raise ValueError(
                    f"{behavior}: selected kind lacks exact source/profile"
                )
            if source_id in selected_source_ids:
                raise ValueError(
                    f"{behavior}: selected source id is duplicated"
                )
            selected_source_ids.add(source_id)
        elif disposition == "PREIMPLEMENTED_REFERENCE":
            reference = row.get("local_reference")
            if not str(reference or "").strip():
                raise ValueError(
                    f"{behavior}: preimplemented row lacks local reference"
                )
        else:
            require_deferred_contract(row, behavior)


def t18_machine_rows(
    document: dict[str, Any], policy: dict[str, Any]
) -> dict[str, dict[str, Any]]:
    if document.get("source", {}).get("revision") != SOURCE_REVISION:
        raise ValueError("T13 machine denominator revision drifted")
    rows = {
        row["behavior_class"]: row
        for row in document.get("canonical_kinds", [])
        if row.get("owner") == "T18"
    }
    if len(rows) != 29:
        raise ValueError(
            f"T13 exposes {len(rows)} T18 machine kinds, expected 29"
        )
    if len(rows) != len({
        row["canonical_key"] for row in rows.values()
    }):
        raise ValueError("T18 behavior classes are not canonical-key unique")
    policy_kinds = set(policy["kinds"])
    if set(rows) != policy_kinds:
        raise ValueError(
            "T18 policy and T13 machine kinds are not bidirectional: "
            f"missing={sorted(set(rows) - policy_kinds)} "
            f"extra={sorted(policy_kinds - set(rows))}"
        )
    return rows


def t18_energy_rows(
    document: dict[str, Any], policy: dict[str, Any]
) -> dict[str, dict[str, Any]]:
    if document.get("source", {}).get("revision") != SOURCE_REVISION:
        raise ValueError("T13 energy denominator revision drifted")
    owned = [
        row for row in document.get("rows", [])
        if row.get("owner") == "T18"
    ]
    result: dict[str, dict[str, Any]] = {}
    for row in owned:
        identity = (
            "STEAM" if row.get("symbol") == "STEAM"
            else "AU" if "AU" in (row.get("aliases") or [])
            else None
        )
        if identity is None or identity in result:
            raise ValueError("T18 energy identity classification drifted")
        result[identity] = row
    if set(result) != set(policy["energy_identities"]):
        raise ValueError(
            "T13 energy denominator does not bidirectionally cover STEAM/AU"
        )
    expected = {
        "STEAM": (None, "FLUID_STEAM_TRANSPORT"),
        "AU": ("AIR", "AIR_PRESSURE"),
    }
    for identity, row in result.items():
        local, topology = expected[identity]
        if (
            row.get("classification") != "in_scope"
            or row.get("local_energy_type") != local
            or row.get("topology") != topology
        ):
            raise ValueError(f"{identity}: T13 energy mapping drifted")
    return result


def source_variant(
    kind: dict[str, Any], selected_source_id: int
) -> dict[str, Any]:
    variants = kind.get("variants") or []
    matches = [
        row for row in variants
        if str(row.get("source_id_expression")) == str(selected_source_id)
    ]
    if len(matches) != 1:
        raise ValueError(
            f"{kind['behavior_class']}: source id {selected_source_id} "
            f"resolved to {len(matches)} variants"
        )
    return matches[0]


def converter_profiles(document: dict[str, Any]) -> dict[str, dict[str, Any]]:
    if (
        document.get("schemaVersion") != 1
        or document.get("source", {}).get("revision") != SOURCE_REVISION
    ):
        raise ValueError("energy converter catalog header drifted")
    profiles = document.get("profiles")
    if not isinstance(profiles, list):
        raise ValueError("energy converter profiles are missing")
    result = {row.get("id"): row for row in profiles}
    if None in result or len(result) != len(profiles):
        raise ValueError("energy converter profile ids are invalid")
    return result


def validate_steam_engine_semantics(profile: dict[str, Any]) -> None:
    semantics = profile.get("outputSemantics") or {}
    conservation = semantics.get("conservation") or {}
    nominal = semantics.get("sourceNominal") or {}
    fixed = semantics.get("fixedOutput") or {}
    runtime = semantics.get("gt6Runtime") or {}
    if (
        conservation
        != {
            "classification": "SOURCE_BACKED",
            "steamInputMb": 200,
            "kuOutput": 50,
            "steamMbPerKu": 4,
        }
        or conservation["steamInputMb"]
        != conservation["kuOutput"] * conservation["steamMbPerKu"]
        or nominal
        != {
            "classification": "SOURCE_DERIVED_NOMINAL",
            "registeredNumerator": 24,
            "steamPerEu": 2,
            "mOutputKu": 12,
        }
        or nominal["mOutputKu"]
        != nominal["registeredNumerator"] // nominal["steamPerEu"]
        or fixed
        != {
            "classification": "DESIGN_POLICY_FIXED_OUTPUT",
            "kuPerTick": 12,
        }
        or runtime.get("classification") != "DEFERRED_REPLACEMENT"
        or runtime.get("minimumKuPerTick") != 6
        or runtime.get("maximumKuPerTick") != 24
        or not str(runtime.get("replacementCondition") or "").strip()
        or not str(runtime.get("recheckPoint") or "").strip()
        or len(semantics.get("sourceEvidencePaths") or []) != 3
    ):
        raise ValueError("bronze steam-engine output semantics drifted")


def build(policy: dict[str, Any] | None = None) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    validate_policy(policy)
    machine_document = load(MACHINE_DENOMINATOR)
    energy_document = load(ENERGY_DENOMINATOR)
    machine_rows = t18_machine_rows(machine_document, policy)
    energy_rows = t18_energy_rows(energy_document, policy)
    profiles = converter_profiles(load(CONVERTER_CATALOG))

    rows: list[dict[str, Any]] = []
    selected_variant_count = 0
    for behavior in sorted(machine_rows):
        source = machine_rows[behavior]
        classification = policy["kinds"][behavior]
        disposition = classification["disposition"]
        row: dict[str, Any] = {
            "behavior_class": behavior,
            "canonical_key": source["canonical_key"],
            "accepted_energy": source["accepted_energy"],
            "emitted_energy": source["emitted_energy"],
            "process_map": source["process_map"],
            "disposition": disposition,
            "source_identity": source["source_identity"],
            "source_variant_count": len(source["variants"]),
        }
        if disposition == "SELECTED_T18":
            variant = source_variant(
                source, classification["selected_source_id"]
            )
            profile_id = classification["converter_profile"]
            profile = profiles.get(profile_id)
            if profile is None:
                raise ValueError(
                    f"{behavior}: missing converter profile {profile_id}"
                )
            profile_source = profile.get("source") or {}
            if (
                profile_source.get("machineKind") != behavior
                or profile_source.get("sourceId")
                != classification["selected_source_id"]
                or profile_source.get("normalizedRowKey")
                != variant["source_identity"]["normalized_row_key"]
            ):
                raise ValueError(
                    f"{behavior}: converter profile differs from source row"
                )
            if behavior == "MultiTileEntityEngineSteam":
                validate_steam_engine_semantics(profile)
            row.update({
                "selected_source": {
                    "source_id": classification["selected_source_id"],
                    "source_line": variant["source_line"],
                    "material_expression":
                        variant["material_expression"],
                    "output_expression": variant["output"],
                    "efficiency_expression": variant["efficiency"],
                    "input_window": variant["input_window"],
                    "source_identity": variant["source_identity"],
                },
                "converter_profile": profile_id,
                "stage": classification["stage"],
            })
            if behavior == "MultiTileEntityEngineSteam":
                row["output_semantics"] = profile["outputSemantics"]
            selected_variant_count += 1
        elif disposition == "PREIMPLEMENTED_REFERENCE":
            row.update({
                "local_reference": classification["local_reference"],
                "implemented_by": classification["implemented_by"],
            })
        else:
            row.update({
                field: classification[field]
                for field in DEFERRED_FIELDS
            })
        rows.append(row)

    dispositions = Counter(row["disposition"] for row in rows)
    selected = dispositions["SELECTED_T18"]
    references = dispositions["PREIMPLEMENTED_REFERENCE"]
    deferred = dispositions["DEFERRED_WITH_REASON"]
    if selected + references + deferred != len(rows):
        raise ValueError("T18 machine disposition count is incomplete")
    selected_profiles = {
        row["converter_profile"] for row in rows
        if row["disposition"] == "SELECTED_T18"
    }
    if selected_profiles != set(profiles):
        raise ValueError(
            "selected T18 kinds and converter profiles are not bidirectional"
        )

    source_keys = {
        row["canonical_key"] for row in machine_rows.values()
    }
    projected_keys = {row["canonical_key"] for row in rows}
    if source_keys != projected_keys or len(rows) != len(projected_keys):
        raise ValueError("T18 canonical assignment is not bidirectional")

    return {
        "schema_version": 1,
        "status": "T18_MACHINE_ENERGY_DENOMINATOR_READY",
        "source_revision": SOURCE_REVISION,
        "counts": {
            "t18_owner_machine_kinds": len(rows),
            "classified": len(rows),
            "unclassified": 0,
            "selected_kinds": selected,
            "selected_source_variants": selected_variant_count,
            "preimplemented_reference_kinds": references,
            "deferred_kinds": deferred,
            "energy_identities": len(energy_rows),
            "source_variants": sum(
                row["source_variant_count"] for row in rows
            ),
            "dispositions": dict(sorted(dispositions.items())),
        },
        "energy_identities": {
            identity: {
                "symbol": row["symbol"],
                "aliases": row["aliases"],
                "local_energy_type": row["local_energy_type"],
                "topology": row["topology"],
                "source_identity": row["source_identity"],
            }
            for identity, row in sorted(energy_rows.items())
        },
        "bidirectional_assignment": {
            "source_canonical_keys": sorted(source_keys),
            "projected_canonical_keys": sorted(projected_keys),
            "complete": True,
            "unique": True,
        },
        "rows": rows,
        "currentness": {
            "owned_inputs": {
                relative(BUILDER): sha256(BUILDER),
                relative(POLICY): sha256(POLICY),
            },
            "t13_denominators": {
                relative(MACHINE_DENOMINATOR):
                    sha256(MACHINE_DENOMINATOR),
                relative(ENERGY_DENOMINATOR):
                    sha256(ENERGY_DENOMINATOR),
            },
            "runtime_projection": {
                relative(CONVERTER_CATALOG): sha256(CONVERTER_CATALOG),
            },
        },
    }


def check() -> list[str]:
    encoded = stable(build())
    if not OUTPUT.is_file():
        return [f"missing generated file: {relative(OUTPUT)}"]
    if OUTPUT.read_text(encoding="utf-8") != encoded:
        return [f"stale generated file: {relative(OUTPUT)}"]
    return []


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
    return document


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = load(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T18 machine/energy denominator failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "machine_kinds":
            document["counts"]["t18_owner_machine_kinds"],
        "selected": document["counts"]["selected_kinds"],
        "references":
            document["counts"]["preimplemented_reference_kinds"],
        "deferred": document["counts"]["deferred_kinds"],
        "unclassified": document["counts"]["unclassified"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
