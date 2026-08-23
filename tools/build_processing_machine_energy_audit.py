#!/usr/bin/env python3
"""Build the explicit 25-machine processing-energy audit."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "processing_machine_energy_audit_policy.json"
OUTPUT = TOOLS / "processing_machine_energy_audit.json"
T12_CLOSURE = TOOLS / "t12_closure_readiness.json"
SOURCE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/"
    "ModProcessingMachines.java"
)
ENERGY_TYPES = {
    "HEAT",
    "KINETIC",
    "KINETIC_ROTATION",
    "KINETIC_PUSH",
    "AIR",
    "ELECTRIC",
    "TIME",
}
T36_KIND_BEHAVIORS = {"ROASTER", "COAGULATOR"}


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


def processing_initializers(source: str) -> dict[str, str]:
    declaration = re.compile(
        r"public\s+static\s+final\s+ProcessingMachineSpec\s+"
        r"([A-Z0-9_]+)\s*="
    )
    result: dict[str, str] = {}
    for match in declaration.finditer(source):
        depth = 0
        end = match.end()
        while end < len(source):
            character = source[end]
            if character == "(":
                depth += 1
            elif character == ")":
                depth -= 1
            elif character == ";" and depth == 0:
                break
            end += 1
        if end == len(source):
            raise ValueError(
                f"{match.group(1)}: unterminated processing spec initializer"
            )
        constant = match.group(1)
        if constant in result:
            raise ValueError(f"{constant}: duplicate processing spec")
        result[constant] = source[match.end():end]
    return result


def explicit_energy_values(source: str) -> dict[str, str]:
    result: dict[str, str] = {}
    for constant, initializer in processing_initializers(source).items():
        energies = re.findall(r"EnergyType\.([A-Z_]+)", initializer)
        if len(energies) != 1:
            raise ValueError(
                f"{constant}: expected exactly one explicit EnergyType "
                f"argument, found {energies}"
            )
        result[constant] = energies[0]
    return result


def helper_overload_audit(
    source: str, policy: dict[str, Any]
) -> dict[str, Any]:
    signatures: dict[str, list[str]] = {}
    for helper, parameters in re.findall(
        r"private\s+static\s+ProcessingMachineSpec\s+"
        r"(mechanical|t3|t5)\s*\((.*?)\)\s*\{",
        source,
        flags=re.DOTALL,
    ):
        signatures.setdefault(helper, []).append(parameters)
    required = policy["default_overload_policy"]
    helpers = required["forbidden_helpers"]
    parameter = required["required_parameter"]
    implicit = [
        helper
        for helper in helpers
        for signature in signatures.get(helper, [])
        if parameter not in signature
    ]
    missing = [
        helper for helper in helpers if not signatures.get(helper)
    ]
    duplicate = [
        helper
        for helper in helpers
        if len(signatures.get(helper, [])) != 1
    ]
    if missing or duplicate or implicit:
        raise ValueError(
            "processing helper overload policy failed: "
            f"missing={missing}, duplicate={duplicate}, "
            f"implicit={implicit}"
        )
    return {
        "policy": "DEFAULT_ENERGY_OVERLOAD_TOKEN_FORBIDDEN",
        "helpers": helpers,
        "required_parameter": parameter,
        "helper_declarations": {
            helper: len(signatures[helper]) for helper in helpers
        },
        "forbidden_default_overloads": implicit,
    }


def ledger_row(spec: dict[str, Any]) -> tuple[dict[str, Any], Path]:
    path = ROOT / spec["path"]
    document = load(path)
    section = document.get(spec["section"])
    if isinstance(section, list):
        matches = [
            row for row in section if row.get("id") == spec["key"]
        ]
        if len(matches) != 1:
            raise ValueError(
                f"{spec['key']}: ledger list lookup is not unique"
            )
        row = matches[0]
    elif isinstance(section, dict):
        row = section.get(spec["key"])
        if not isinstance(row, dict):
            raise ValueError(f"{spec['key']}: ledger row is missing")
    else:
        raise ValueError(f"{spec['path']}: ledger section is invalid")
    if row.get(spec["field"]) != spec["value"]:
        raise ValueError(
            f"{spec['key']}: expected ledger {spec['field']}="
            f"{spec['value']!r}, got {row.get(spec['field'])!r}"
        )
    return row, path


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status")
        != "PROCESSING_MACHINE_ENERGY_AUDIT_POLICY"
        or policy.get("source") != relative(SOURCE)
    ):
        raise ValueError("processing-machine energy policy header drifted")
    dispositions = set(policy.get("dispositions") or [])
    expected_dispositions = {
        "SOURCE_ALIGNED",
        "FIXED_UTILITY",
        "MAPPED_DEFERRED",
        "CROSS_OWNER_DEFERRED",
    }
    if dispositions != expected_dispositions:
        raise ValueError("processing-machine disposition vocabulary drifted")
    machines = policy.get("machines")
    if not isinstance(machines, dict) or len(machines) != 25:
        raise ValueError("processing-machine audit must cover exactly 25 specs")
    ids = [row.get("id") for row in machines.values()]
    if len(ids) != len(set(ids)) or any(not value for value in ids):
        raise ValueError("processing-machine audit ids are invalid")
    for constant, row in machines.items():
        if (
            row.get("expected_energy_type") not in ENERGY_TYPES
            or row.get("disposition") not in dispositions
            or not str(row.get("reason") or "").strip()
            or not isinstance(row.get("ledger"), dict)
        ):
            raise ValueError(f"{constant}: invalid audit policy row")


def build(
    policy: dict[str, Any] | None = None,
    source: str | None = None,
) -> dict[str, Any]:
    policy = load(POLICY) if policy is None else policy
    validate_policy(policy)
    source = SOURCE.read_text(encoding="utf-8") if source is None else source
    helper_audit = helper_overload_audit(source, policy)
    actual = explicit_energy_values(source)
    expected_constants = set(policy["machines"])
    extra = set(actual) - expected_constants
    missing = expected_constants - set(actual)
    if missing or extra - T36_KIND_BEHAVIORS:
        raise ValueError(
            "processing-machine spec set differs from policy: "
            f"missing={sorted(missing)}, "
            f"extra={sorted(extra)}"
        )

    rows: list[dict[str, Any]] = []
    ledger_paths: set[Path] = set()
    for constant, expected in policy["machines"].items():
        actual_energy = actual[constant]
        expected_energy = expected["expected_energy_type"]
        if actual_energy != expected_energy:
            raise ValueError(
                f"{constant}: actual energy {actual_energy} differs from "
                f"expected {expected_energy}"
            )
        _, ledger_path = ledger_row(expected["ledger"])
        ledger_paths.add(ledger_path)
        rows.append({
            "java_constant": constant,
            "id": expected["id"],
            "actual": actual_energy,
            "expected": expected_energy,
            "disposition": expected["disposition"],
            "reason": expected["reason"],
            "ledger": expected["ledger"],
        })

    legacy = sorted(
        row["id"] for row in rows if row["actual"] == "KINETIC"
    )
    allowed_legacy = sorted(policy["allowed_legacy_kinetic_ids"])
    if legacy != allowed_legacy:
        raise ValueError(
            "legacy KINETIC set changed: "
            f"actual={legacy}, allowed={allowed_legacy}"
        )
    for row in rows:
        if (
            row["actual"] == "KINETIC"
            and row["disposition"]
            not in {"FIXED_UTILITY", "MAPPED_DEFERRED"}
        ):
            raise ValueError(
                f"{row['id']}: legacy KINETIC lacks fixed/deferred disposition"
            )

    energy_counts = Counter(row["actual"] for row in rows)
    expected_energy_counts = {
        "ELECTRIC": 1,
        "HEAT": 4,
        "KINETIC": 4,
        "KINETIC_PUSH": 4,
        "KINETIC_ROTATION": 9,
        "TIME": 3,
    }
    if dict(sorted(energy_counts.items())) != expected_energy_counts:
        raise ValueError("processing-machine energy counts drifted")
    disposition_counts = Counter(row["disposition"] for row in rows)
    expected_disposition_counts = {
        "CROSS_OWNER_DEFERRED": 1,
        "FIXED_UTILITY": 4,
        "MAPPED_DEFERRED": 7,
        "SOURCE_ALIGNED": 13,
    }
    if dict(sorted(disposition_counts.items())) != expected_disposition_counts:
        raise ValueError("processing-machine disposition counts drifted")

    own_counts = {
        "machine_specs": len(rows),
        "explicit_energy_arguments": len(rows),
        "implicit_energy_arguments": 0,
        "legacy_kinetic": len(legacy),
        "new_legacy_kinetic": 0,
    }
    # Forward edge 23 -> 24: the T12 closure (#23) records this audit's status
    # and counts without pinning its hash; the audit proves the two agree.
    closure = load(T12_CLOSURE)
    recorded = (closure.get("energy") or {}).get("processing_machine_audit")
    if recorded is None:
        raise ValueError("T12 closure readiness lacks the energy audit block")
    if recorded.get("status") != "PROCESSING_MACHINE_ENERGY_AUDIT_READY":
        raise ValueError("T12 closure energy audit status drifted")

    return {
        "schema_version": 1,
        "status": "PROCESSING_MACHINE_ENERGY_AUDIT_READY",
        "counts": {
            **own_counts,
            "energy_types": expected_energy_counts,
            "dispositions": expected_disposition_counts,
        },
        "default_overload_audit": helper_audit,
        "allowed_legacy_kinetic_ids": legacy,
        "rows": rows,
        "currentness": {
            "owned_inputs": {
                relative(BUILDER): sha256(BUILDER),
                relative(POLICY): sha256(POLICY),
                relative(SOURCE): sha256(SOURCE),
            },
            "ledgers": {
                **{
                    relative(path): sha256(path)
                    for path in sorted(ledger_paths)
                },
                relative(T12_CLOSURE): sha256(T12_CLOSURE),
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
    OUTPUT.write_bytes(stable(document).encode("utf-8"))
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
        print(f"processing-machine energy audit failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "machines": document["counts"]["machine_specs"],
        "legacy_kinetic": document["counts"]["legacy_kinetic"],
        "implicit": document["counts"]["implicit_energy_arguments"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
