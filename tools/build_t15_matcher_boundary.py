#!/usr/bin/env python3
"""Build the source-derived T15e port/matcher boundary artifact."""
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
OUTPUT = TOOLS / "t15_matcher_boundary.json"
T12_POLICY = TOOLS / "t12_machine_policy.json"
BENCHMARK = TOOLS / "t12_capacity_matcher_benchmark.json"
STRUCTURE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/multiblock_structures"
    / "large_centrifuge.json"
)
PROCESSING_SPECS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry"
    / "ModProcessingMachines.java"
)
PROCESSING_HOST = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/blockentity"
    / "ProcessingMachineBlockEntity.java"
)
PORT_BRIDGE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/blockentity"
    / "MultiblockPortBlockEntity.java"
)
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


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def parse_structure(document: dict[str, Any]) -> dict[str, int]:
    if document.get("schema_version") != 1:
        raise ValueError("Large Centrifuge structure schema must be 1")
    palette = document.get("palette") or {}
    rows = document.get("structure") or []
    offsets = [tuple(row.get("offset") or []) for row in rows]
    if len(offsets) != len(set(offsets)) or any(
        len(offset) != 3 for offset in offsets
    ):
        raise ValueError("Large Centrifuge structure offsets are invalid")

    counts: Counter[str] = Counter()
    for row in rows:
        predicate = palette.get(row.get("predicate"))
        if not isinstance(predicate, dict):
            raise ValueError("Large Centrifuge structure references an unknown predicate")
        kind = predicate.get("type")
        if kind == "port":
            port = predicate.get("port")
            if port not in {"item_fluid", "energy_input"}:
                raise ValueError(f"unknown Large Centrifuge port type {port!r}")
            counts[port] += 1
        elif kind == "controller":
            counts["controller"] += 1
        else:
            counts[kind or "missing_type"] += 1

    result = {
        "scan_volume": len(rows),
        "item_fluid_ports": counts["item_fluid"],
        "energy_input_ports": counts["energy_input"],
        "controllers": counts["controller"],
    }
    if result != {
        "scan_volume": 18,
        "item_fluid_ports": 15,
        "energy_input_ports": 2,
        "controllers": 1,
    } or sum(counts.values()) != len(rows):
        raise ValueError(
            "Large Centrifuge live structure must be "
            "15 item/fluid ports + 2 energy ports + 1 controller"
        )
    return result


def _java_int(value: str) -> int:
    return int(value.replace("_", ""))


def parse_centrifuge_spec(source: str) -> dict[str, int]:
    number = r"([0-9][0-9_]*)"
    pattern = re.compile(
        r"public\s+static\s+final\s+ProcessingMachineSpec\s+CENTRIFUGE"
        r"\s*=\s*reusedT5\s*\(\s*\"centrifuge\"\s*,"
        r"\s*\(\)\s*->\s*ModRecipeMaps\.CENTRIFUGE\s*,\s*"
        + r"\s*,\s*".join([number] * 6)
        + r"\s*,",
        flags=re.DOTALL,
    )
    matches = pattern.findall(source)
    if len(matches) != 1:
        raise ValueError(
            "could not derive exactly one live ModProcessingMachines.CENTRIFUGE layout"
        )
    (
        item_inputs,
        item_outputs,
        fluid_inputs,
        fluid_outputs,
        fluid_input_capacity,
        fluid_output_capacity,
    ) = map(_java_int, matches[0])
    result = {
        "item_inputs": item_inputs,
        "item_outputs": item_outputs,
        "item_slots": item_inputs + item_outputs,
        "fluid_inputs": fluid_inputs,
        "fluid_outputs": fluid_outputs,
        "fluid_tanks": fluid_inputs + fluid_outputs,
        "fluid_input_capacity": fluid_input_capacity,
        "fluid_output_capacity": fluid_output_capacity,
    }
    if result != {
        "item_inputs": 1,
        "item_outputs": 6,
        "item_slots": 7,
        "fluid_inputs": 1,
        "fluid_outputs": 2,
        "fluid_tanks": 3,
        "fluid_input_capacity": 4_000,
        "fluid_output_capacity": 8_000,
    }:
        raise ValueError(f"Large Centrifuge live host layout drifted: {result}")
    return result


def parse_presence_supply_cap(source: str) -> int:
    matches = re.findall(
        r"PRESENCE_RESERVATION_SUPPLY_CAP\s*=\s*([0-9][0-9_]*)",
        source,
    )
    if len(matches) != 1:
        raise ValueError("could not derive the CapacityMatcher presence supply cap")
    return _java_int(matches[0])


def validate_runtime_contracts() -> dict[str, dict[str, Any]]:
    contracts = {
        "processing_host": (
            PROCESSING_HOST,
            (
                "spec.items().inputs().stream().map(inventory::getStackInSlot)",
                "spec.fluids().inputs().stream().map(",
                "recipeCache.find(itemInputs, fluidInputs)",
            ),
        ),
        "port_bridge": (
            PORT_BRIDGE,
            (
                "all resource\n * state remains transaction-owned by the shared processing host",
                "host.inventory().getSlots()",
                "host.inventory().getStackInSlot(slot)",
                "host.tanks().size()",
                "host.tanks().get(tank).getFluid().copy()",
            ),
        ),
    }
    result: dict[str, dict[str, Any]] = {}
    for owner, (path, required) in contracts.items():
        source = path.read_text(encoding="utf-8")
        missing = [token for token in required if token not in source]
        if missing:
            raise ValueError(f"T15e runtime contract {owner} is incomplete: {missing}")
        result[owner] = {
            "path": path.relative_to(ROOT).as_posix(),
            "sha256": sha256(path),
            "required_tokens": list(required),
        }
    return result


def correction_audit() -> dict[str, Any]:
    policy = load(T12_POLICY)
    large = policy["structure_projection"]["large_centrifuge"]
    audit = large.get("port_count_correction_audit") or {}
    if (
        large.get("port_counts")
        != {"item_fluid": 15, "energy_input": 2}
        or large.get("controller_count") != 1
        or audit.get("owner") != "T15e"
        or audit.get("historical_expected_item_fluid_ports") != 16
        or audit.get("historical_status")
        != "SUPERSEDED_INCORRECT_EXPECTATION"
        or "controller" not in str(audit.get("error") or "").lower()
        or "15 item/fluid + 2 energy + 1 controller"
        not in str(audit.get("correction") or "")
        or audit.get("corrected_authority")
        != "tools/t15_matcher_boundary.json#physical_structure"
    ):
        raise ValueError(
            "T12 policy must preserve the expected-16 error audit and T15 correction"
        )
    return {
        **audit,
        "policy": T12_POLICY.relative_to(ROOT).as_posix(),
        "policy_sha256": sha256(T12_POLICY),
    }


def benchmark_evidence(cap: int) -> dict[str, Any]:
    document = load(BENCHMARK)
    scenarios = document.get("scenarios") or []
    dense = {
        row.get("supplies")
        for row in scenarios
        if str(row.get("id", "")).startswith("dense_consuming_")
    }
    rejected = {
        row.get("supplies")
        for row in scenarios
        if str(row.get("id", "")).startswith("presence_cap_rejection_")
    }
    harness = BENCHMARK_HARNESS.read_text(encoding="utf-8")
    declared_match = re.search(
        r"SUPPLY_COUNTS\s*=\s*\{([^}]*)\}", harness
    )
    if declared_match is None:
        raise ValueError("benchmark harness supply counts are not derivable")
    declared = {
        int(value)
        for value in re.findall(r"\d+", declared_match.group(1))
    }
    if (
        document.get("status") != "T12A_BENCHMARK_READY"
        or declared != {12, 16, 32, 64}
        or dense != declared
        or rejected != {16, 32, 64}
        or not all(row.get("within_budget") is True for row in scenarios)
        or document.get("decision", {}).get("current_presence_supply_cap")
        != cap
        or document.get("decision", {}).get("matcher_rewritten") is not False
    ):
        raise ValueError("current CapacityMatcher benchmark evidence is incomplete")
    return {
        "path": BENCHMARK.relative_to(ROOT).as_posix(),
        "sha256": sha256(BENCHMARK),
        "status": document["status"],
        "dense_supply_counts": sorted(dense),
        "presence_cap_rejection_supply_counts": sorted(rejected),
        "all_within_budget": True,
        "matcher_rewritten": False,
        "harness": BENCHMARK_HARNESS.relative_to(ROOT).as_posix(),
        "harness_sha256": sha256(BENCHMARK_HARNESS),
        "matcher": CAPACITY_MATCHER.relative_to(ROOT).as_posix(),
        "matcher_sha256": sha256(CAPACITY_MATCHER),
    }


def build() -> dict[str, Any]:
    physical = parse_structure(load(STRUCTURE))
    host = parse_centrifuge_spec(PROCESSING_SPECS.read_text(encoding="utf-8"))
    cap = parse_presence_supply_cap(
        CAPACITY_MATCHER.read_text(encoding="utf-8")
    )
    contracts = validate_runtime_contracts()
    benchmark = benchmark_evidence(cap)
    audit = correction_audit()
    item_supplies = host["item_inputs"]
    fluid_supplies = host["fluid_inputs"]
    if item_supplies > cap:
        raise ValueError("Large Centrifuge item matcher exceeds the presence cap")
    return {
        "schema_version": 1,
        "status": "T15E_MATCHER_BOUNDARY_READY",
        "physical_structure": {
            "path": STRUCTURE.relative_to(ROOT).as_posix(),
            "sha256": sha256(STRUCTURE),
            **physical,
        },
        "host_layout": {
            "path": PROCESSING_SPECS.relative_to(ROOT).as_posix(),
            "sha256": sha256(PROCESSING_SPECS),
            **host,
        },
        "port_host_boundary": {
            "physical_item_fluid_ports": physical["item_fluid_ports"],
            "shared_processing_hosts": 1,
            "item_matcher_supplies": item_supplies,
            "fluid_matcher_supplies": fluid_supplies,
            "physical_ports_expand_matcher_supplies": False,
            "relationship": (
                "15 physical item/fluid ports bridge one shared host inventory; "
                "matcher supplies are the host's 1 item input and 1 fluid input."
            ),
        },
        "matcher_boundary": {
            "presence_item_supply_cap": cap,
            "current_item_supply_count": item_supplies,
            "presence_cap_triggered": item_supplies > cap,
            "matcher_rewritten": benchmark["matcher_rewritten"],
            "decision": (
                "Keep the current matcher: one source-derived item input supply "
                "does not trigger the presence-only cap of 12."
            ),
        },
        "benchmark": benchmark,
        "historical_correction_audit": audit,
        "source_contracts": contracts,
        "currentness": {
            "owned_inputs": {
                BUILDER.relative_to(ROOT).as_posix(): sha256(BUILDER),
                STRUCTURE.relative_to(ROOT).as_posix(): sha256(STRUCTURE),
                PROCESSING_SPECS.relative_to(ROOT).as_posix(): sha256(
                    PROCESSING_SPECS
                ),
                CAPACITY_MATCHER.relative_to(ROOT).as_posix(): sha256(
                    CAPACITY_MATCHER
                ),
                BENCHMARK.relative_to(ROOT).as_posix(): sha256(BENCHMARK),
                BENCHMARK_HARNESS.relative_to(ROOT).as_posix(): sha256(
                    BENCHMARK_HARNESS
                ),
                T12_POLICY.relative_to(ROOT).as_posix(): sha256(T12_POLICY),
                **{
                    row["path"]: row["sha256"]
                    for row in contracts.values()
                },
            }
        },
    }


def check() -> list[str]:
    encoded = stable(build())
    if not OUTPUT.is_file():
        return [
            f"missing generated file: {OUTPUT.relative_to(ROOT).as_posix()}"
        ]
    if OUTPUT.read_text(encoding="utf-8") != encoded:
        return [
            f"stale generated file: {OUTPUT.relative_to(ROOT).as_posix()}"
        ]
    return []


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
    return document


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed T15e boundary artifact is stale",
    )
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
        print(f"T15e matcher boundary failed: {error}")
        return 1
    print(
        json.dumps(
            {
                "status": document["status"],
                "physical_item_fluid_ports": document[
                    "physical_structure"
                ]["item_fluid_ports"],
                "item_matcher_supplies": document[
                    "port_host_boundary"
                ]["item_matcher_supplies"],
            },
            sort_keys=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
