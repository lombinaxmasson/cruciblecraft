#!/usr/bin/env python3
"""Build the deterministic T5.5 machine-crafting readiness ledger."""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path
from typing import Any
from urllib.request import urlopen

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "machine_crafting_policy.json"
OUTPUT = TOOLS / "machine_crafting_readiness.json"
RECIPE_PROVIDER = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/datagen/ModRecipeProvider.java"
)
PROCESSING_MACHINES = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/"
    "ModProcessingMachines.java"
)
MOD_ITEMS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModItems.java"
)
MATERIAL_PREFIXES = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/api/material/"
    "MaterialPrefixes.java"
)
GT6_REFERENCE_METADATA = TOOLS / "gt6_reference_metadata.json"
MAIN_RECIPE_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/recipe"
)
GENERATED_MACHINE_RECIPE_ROOT = (
    ROOT / "src/generated/resources/data/cruciblecraft/recipe/machines"
)

ALLOWED_CLASSIFICATIONS = {
    "directly_projectable",
    "tier_collapsed_pending_t6",
    "missing_cc_component",
    "source_identity_unresolved",
    "existing_cc_recipe_retained",
}
CONFIGURED_LIST_NAMES = ("T2_MACHINES", "T3_MACHINES", "T5_MACHINES")
T11_MACHINE_IDS = {
    "generifier",
    "fluid_deposit_extractor",
    "fuel_engine",
    "burning_gas_generator",
}
T12_MACHINE_IDS = {
    "electric_motor",
    "rotational_axle",
    "rotational_gearbox",
}
T12_SOURCE_PROJECTED_CONFIGURED_IDS = {
    "centrifuge",
    "sifter",
    "electrolyzer",
}
T16_SOURCE_PROJECTED_CONFIGURED_IDS = {
    "lathe",
    "rollingmill",
    "wiremill",
    "shredder",
    "press",
}
T17_SOURCE_PROJECTED_CONFIGURED_IDS = {
    "distillery",
    "drying",
    "smelter",
}
SOURCE_PROJECTED_CONFIGURED_IDS = (
    T12_SOURCE_PROJECTED_CONFIGURED_IDS
    | T16_SOURCE_PROJECTED_CONFIGURED_IDS
    | T17_SOURCE_PROJECTED_CONFIGURED_IDS
)


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


def git_blob_sha1(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def source_blob_sha1(data: bytes) -> str:
    """Hash source as stored by Git, independent of Windows checkout EOLs."""
    return git_blob_sha1(data.replace(b"\r\n", b"\n"))


def configured_machine_ids() -> list[str]:
    """Derive CONFIGURED_MACHINES from its constituent Java lists."""
    source = PROCESSING_MACHINES.read_text(encoding="utf-8")
    definitions = dict(re.findall(
        r"public\s+static\s+final\s+ProcessingMachineSpec\s+([A-Z0-9_]+)"
        r"\s*=\s*(?:mechanical|reusedT5|t3|t5)\s*\(\s*\"([^\"]+)\"",
        source,
        flags=re.DOTALL,
    ))
    if not definitions:
        raise ValueError("could not derive processing-machine definitions")

    ordered_constants: list[str] = []
    for list_name in CONFIGURED_LIST_NAMES:
        match = re.search(
            rf"List<ProcessingMachineSpec>\s+{list_name}\s*=\s*List\.of"
            r"\s*\((.*?)\)\s*;",
            source,
            flags=re.DOTALL,
        )
        if match is None:
            raise ValueError(f"could not derive {list_name}")
        for constant in re.findall(r"\b[A-Z][A-Z0-9_]*\b", match.group(1)):
            if constant not in definitions:
                raise ValueError(
                    f"{list_name} references unparsed machine {constant}"
                )
            if constant not in ordered_constants:
                ordered_constants.append(constant)
    result = [definitions[constant] for constant in ordered_constants]
    if len(result) != len(set(result)):
        raise ValueError("derived configured machine ids are not unique")
    return result


def placeholder_machine_ids() -> list[str]:
    return [
        recipe_id
        for _, recipe_id in placeholder_machines()
        if recipe_id not in T11_MACHINE_IDS | T12_MACHINE_IDS
    ]


def placeholder_machines() -> list[tuple[str, str]]:
    source = RECIPE_PROVIDER.read_text(encoding="utf-8")
    return re.findall(
        r"machineCrafting\s*\(\s*output\s*,\s*ModItems\.([A-Z0-9_]+)"
        r"\.get\(\)\s*,\s*\"([^\"]+)\"\s*\)",
        source,
    )


def machine_item_registrations() -> dict[str, str]:
    source = MOD_ITEMS.read_text(encoding="utf-8")
    return dict(re.findall(
        r"DeferredItem<BlockItem>\s+([A-Z0-9_]+)\s*=\s*"
        r"ITEMS\.registerSimpleBlockItem\(\s*\"([^\"]+)\"",
        source,
        flags=re.DOTALL,
    ))


def extract_java_method(source: str, signature: str) -> str:
    start = source.find(signature)
    if start < 0:
        raise ValueError(f"missing Java method signature: {signature}")
    opening = source.find("{", start)
    if opening < 0:
        raise ValueError(f"missing Java method body: {signature}")
    depth = 0
    for index in range(opening, len(source)):
        character = source[index]
        if character == "{":
            depth += 1
        elif character == "}":
            depth -= 1
            if depth == 0:
                return source[start:index + 1]
    raise ValueError(f"unterminated Java method: {signature}")


def placeholder_template() -> dict[str, Any]:
    source = RECIPE_PROVIDER.read_text(encoding="utf-8")
    method = extract_java_method(
        source,
        "private static void machineCrafting(",
    )
    required = (
        '.pattern("CCC")',
        '.pattern("CFC")',
        ".define('C', Items.COPPER_INGOT)",
        ".define('F', Items.FURNACE)",
        '"machines/" + id',
    )
    missing = [token for token in required if token not in method]
    if missing:
        raise ValueError(
            "machineCrafting placeholder template drifted: "
            + ", ".join(missing)
        )
    encoded = method.encode("utf-8")
    return {
        "kind": "generated_copper_furnace_placeholder",
        "pattern": ["CCC", "CFC", "CCC"],
        "ingredients": {
            "C": "minecraft:copper_ingot",
            "F": "minecraft:furnace",
        },
        "source": {
            "path": RECIPE_PROVIDER.relative_to(ROOT).as_posix(),
            "method": "machineCrafting",
            "lines": "168-177",
            "method_sha256": hashlib.sha256(encoded).hexdigest(),
        },
    }


def existing_bronze_recipe_paths() -> list[Path]:
    return sorted(MAIN_RECIPE_ROOT.glob("bronze_*.json"))


def recipe_identity(document: dict[str, Any]) -> dict[str, Any]:
    result = document.get("result")
    if not isinstance(result, dict) or not result.get("id"):
        raise ValueError("bronze recipe has no explicit result.id")
    return {
        "item": str(result["id"]),
        "components": result.get("components", {}),
    }


def resolve_evidence(
    refs: list[str],
    policy: dict[str, Any],
) -> list[dict[str, Any]]:
    anchors = policy["source_anchors"]
    source = policy["gt6_source"]
    result = []
    for ref in refs:
        anchor = anchors.get(ref)
        if not isinstance(anchor, dict):
            raise ValueError(f"unknown source anchor {ref}")
        file_record = source["source_files"].get(anchor["file"])
        if not isinstance(file_record, dict):
            raise ValueError(f"{ref}: unknown source file {anchor['file']}")
        result.append({
            "kind": (
                "gt6_java_negative_search"
                if anchor.get("required_absent_patterns")
                else "gt6_java_anchor"
            ),
            "anchor": ref,
            "repository": source["repository"],
            "revision": source["revision"],
            "path": file_record["path"],
            "git_blob_sha1": file_record["git_blob_sha1"],
            "lines": anchor["lines"],
            "reason": anchor["reason"],
        })
    return result


def validate_policy_row(
    identity: str,
    row: dict[str, Any],
    policy: dict[str, Any],
) -> None:
    classification = row.get("classification")
    if classification not in ALLOWED_CLASSIFICATIONS:
        raise ValueError(f"{identity}: invalid classification {classification}")
    if classification not in policy["classifications"]:
        raise ValueError(f"{identity}: undocumented classification")
    for field in ("reason", "replacement_prerequisites"):
        value = row.get(field)
        if not value:
            raise ValueError(f"{identity}: missing non-empty {field}")
    if not str(row["reason"]).strip():
        raise ValueError(f"{identity}: blank reason")
    for prerequisite in row["replacement_prerequisites"]:
        if not str(prerequisite).strip():
            raise ValueError(f"{identity}: blank replacement prerequisite")
    source_evidence = row.get("source_evidence")
    if not isinstance(source_evidence, list):
        raise ValueError(f"{identity}: source_evidence must be a list")
    resolve_evidence(source_evidence, policy)

    components = row.get("blocking_components")
    if components is None:
        return
    if not isinstance(components, list):
        raise ValueError(f"{identity}: blocking_components must be a list")
    for component in components:
        if component not in policy["component_mappings"]:
            raise ValueError(
                f"{identity}: unknown component mapping {component}"
            )


def validate_source_tree(
    source_root: Path,
    policy: dict[str, Any],
) -> dict[str, str]:
    source = policy["gt6_source"]
    texts: dict[str, str] = {}
    hashes: dict[str, str] = {}
    for key, record in source["source_files"].items():
        path = source_root / record["path"]
        if not path.is_file():
            raise ValueError(f"missing fetched GT6 source file {path}")
        data = path.read_bytes()
        actual = source_blob_sha1(data)
        expected = record["git_blob_sha1"]
        if actual != expected:
            raise ValueError(
                f"{record['path']}: expected git blob {expected}, got {actual}"
            )
        texts[key] = data.decode("utf-8")
        hashes[key] = actual

    anchor_errors: list[str] = []
    for name, anchor in policy["source_anchors"].items():
        text = texts[anchor["file"]]
        lines = text.splitlines()
        selected: list[str] = []
        for part in str(anchor["lines"]).split(","):
            bounds = part.strip().split("-", maxsplit=1)
            first = int(bounds[0])
            last = int(bounds[-1])
            if first <= 0 or last < first or last > len(lines):
                raise ValueError(f"{name}: invalid source lines {part}")
            selected.extend(lines[first - 1:last])
        excerpt = "\n".join(selected)
        for token in anchor.get("required_tokens", []):
            if token not in excerpt:
                anchor_errors.append(
                    f"{name}: source token not found: {token}"
                )
        for pattern in anchor.get("required_absent_patterns", []):
            if pattern in text:
                anchor_errors.append(
                    f"{name}: forbidden source pattern is present: {pattern}"
                )
    if anchor_errors:
        raise ValueError(
            "pinned GT6 source anchor verification failed:\n"
            + "\n".join(anchor_errors)
        )
    return hashes


def fetch_source(destination: Path, policy: dict[str, Any]) -> None:
    source = policy["gt6_source"]
    for record in source["source_files"].values():
        with urlopen(record["raw_url"]) as response:
            data = response.read()
        actual = source_blob_sha1(data)
        if actual != record["git_blob_sha1"]:
            raise ValueError(
                f"{record['path']}: fetched git blob {actual} does not match "
                f"{record['git_blob_sha1']}"
            )
        target = destination / record["path"]
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(data)
    validate_source_tree(destination, policy)


def configured_rows(
    machine_ids: list[str],
    placeholders: list[tuple[str, str]],
    policy: dict[str, Any],
    template: dict[str, Any],
) -> list[dict[str, Any]]:
    item_registrations = machine_item_registrations()
    item_fields = {
        recipe_id: item_field
        for item_field, recipe_id in placeholders
    }
    item_fields.update({
        "centrifuge": "CENTRIFUGE",
        "sifter": "SIFTER",
        "electrolyzer": "ELECTROLYZER",
        **{
            machine_id: machine_id.upper()
            for machine_id in (
                T16_SOURCE_PROJECTED_CONFIGURED_IDS
                | T17_SOURCE_PROJECTED_CONFIGURED_IDS
            )
        },
    })
    result = []
    for machine_id in machine_ids:
        policy_row = policy["configured_machines"][machine_id]
        validate_policy_row(machine_id, policy_row, policy)
        item_field = item_fields[machine_id]
        item_id = item_registrations.get(item_field)
        if item_id is None:
            raise ValueError(
                f"{machine_id}: could not resolve ModItems.{item_field}"
            )
        source_projected = machine_id in SOURCE_PROJECTED_CONFIGURED_IDS
        result.append({
            "scope": "configured_machine_placeholder",
            "machine_id": machine_id,
            "cc_identity": {
                "item": f"cruciblecraft:{item_id}",
                "mod_items_field": item_field,
                "processing_spec": f"cruciblecraft:{machine_id}",
            },
            "current_recipe": {
                "path": (
                    GENERATED_MACHINE_RECIPE_ROOT
                    / f"{machine_id}.json"
                ).relative_to(ROOT).as_posix(),
                "kind": (
                    "t12_source_projected_machine_recipe"
                    if source_projected
                    else template["kind"]
                ),
                "source": (
                    {
                        "path": RECIPE_PROVIDER.relative_to(ROOT).as_posix(),
                        "method": (
                            "t17HeatMachineCrafting"
                            if machine_id
                            in T17_SOURCE_PROJECTED_CONFIGURED_IDS
                            else "t16MachineCrafting"
                            if machine_id
                            in T16_SOURCE_PROJECTED_CONFIGURED_IDS
                            else f"{machine_id}Crafting"
                            if machine_id != "electrolyzer"
                            else "electrolyzerCrafting"
                        ),
                    }
                    if source_projected
                    else template["source"]
                ),
            },
            "classification": policy_row["classification"],
            "reason": policy_row["reason"],
            "source_identity": {
                "status": (
                    "not_found_in_pinned_machine_loader"
                    if policy_row["classification"]
                    == "source_identity_unresolved"
                    else "pinned_gt6_java_identity"
                ),
                "anchor_ids": policy_row["source_evidence"],
                "tier_or_identity_summary": policy_row["reason"],
            },
            "source_evidence": resolve_evidence(
                policy_row["source_evidence"], policy
            ),
            "blocking_components": [
                {
                    "source_identity": component,
                    **policy["component_mappings"][component],
                }
                for component in policy_row["blocking_components"]
            ],
            "replacement_prerequisites": policy_row[
                "replacement_prerequisites"
            ],
        })
    return result


def bronze_rows(
    paths: list[Path],
    policy: dict[str, Any],
) -> list[dict[str, Any]]:
    result = []
    for path in paths:
        recipe_id = path.stem
        policy_row = policy["existing_bronze_resources"][recipe_id]
        validate_policy_row(recipe_id, policy_row, policy)
        document = load(path)
        result.append({
            "scope": "existing_bronze_resource",
            "recipe_id": recipe_id,
            "cc_identity": recipe_identity(document),
            "current_recipe": {
                "path": path.relative_to(ROOT).as_posix(),
                "kind": str(document.get("type") or ""),
                "sha256": sha256(path),
            },
            "classification": policy_row["classification"],
            "reason": policy_row["reason"],
            "source_identity": {
                "status": "existing_cc_recipe_with_pinned_context",
                "anchor_ids": policy_row["source_evidence"],
                "tier_or_identity_summary": policy_row["reason"],
            },
            "source_evidence": [
                {
                    "kind": "cc_recipe_resource",
                    "path": path.relative_to(ROOT).as_posix(),
                    "sha256": sha256(path),
                },
                *resolve_evidence(policy_row["source_evidence"], policy),
            ],
            "blocking_components": [],
            "replacement_prerequisites": policy_row[
                "replacement_prerequisites"
            ],
        })
    return result


def build(source_root: Path | None = None) -> dict[str, Any]:
    policy = load(POLICY)
    source = policy["gt6_source"]
    metadata = load(GT6_REFERENCE_METADATA)
    if metadata["gt6_version"] != source["dump_metadata"]["expected_version"]:
        raise ValueError("GT6 dump/source revision metadata drifted")
    dump_revision = metadata["gt6_version"].rsplit("-g", maxsplit=1)[-1]
    if not source["revision"].startswith(dump_revision):
        raise ValueError("GT6 dump version does not name the pinned commit")

    machine_ids = configured_machine_ids()
    placeholders = placeholder_machines()
    placeholder_ids = [
        recipe_id
        for _, recipe_id in placeholders
        if recipe_id not in T11_MACHINE_IDS | T12_MACHINE_IDS
    ]
    t11_placeholder_ids = {
        recipe_id
        for _, recipe_id in placeholders
        if recipe_id in T11_MACHINE_IDS
    }
    if t11_placeholder_ids != T11_MACHINE_IDS:
        raise ValueError(
            "T11-owned machine placeholder set drifted: "
            f"{sorted(t11_placeholder_ids)}"
        )
    t12_placeholder_ids = {
        recipe_id
        for _, recipe_id in placeholders
        if recipe_id in T12_MACHINE_IDS
    }
    if t12_placeholder_ids != T12_MACHINE_IDS:
        raise ValueError(
            "T12-owned machine placeholder set drifted: "
            f"{sorted(t12_placeholder_ids)}"
        )
    placeholders = [
        row
        for row in placeholders
        if row[1] not in T11_MACHINE_IDS | T12_MACHINE_IDS
    ]
    placeholder_expected = [
        machine_id
        for machine_id in machine_ids
        if machine_id not in SOURCE_PROJECTED_CONFIGURED_IDS
    ]
    if placeholder_expected != placeholder_ids:
        raise ValueError(
            "configured machines and machineCrafting placeholders differ: "
            f"{placeholder_expected!r} != {placeholder_ids!r}"
        )
    configured_policy_ids = list(policy["configured_machines"])
    if set(configured_policy_ids) != set(machine_ids):
        raise ValueError(
            "configured machine policy coverage drifted: "
            f"missing={sorted(set(machine_ids) - set(configured_policy_ids))}, "
            f"extra={sorted(set(configured_policy_ids) - set(machine_ids))}"
        )

    bronze_paths = existing_bronze_recipe_paths()
    bronze_ids = [path.stem for path in bronze_paths]
    bronze_policy_ids = list(policy["existing_bronze_resources"])
    if set(bronze_policy_ids) != set(bronze_ids):
        raise ValueError(
            "bronze resource policy coverage drifted: "
            f"missing={sorted(set(bronze_ids) - set(bronze_policy_ids))}, "
            f"extra={sorted(set(bronze_policy_ids) - set(bronze_ids))}"
        )
    if not bronze_paths:
        raise ValueError("no existing bronze crafting resources discovered")

    template = placeholder_template()
    configured = configured_rows(machine_ids, placeholders, policy, template)
    bronze = bronze_rows(bronze_paths, policy)
    rows = [*configured, *bronze]
    categories = Counter(row["classification"] for row in rows)
    unclassified = sum(
        row["classification"] not in ALLOWED_CLASSIFICATIONS for row in rows
    )
    if unclassified:
        raise ValueError(f"{unclassified} machine-crafting rows unclassified")
    if any(
        row["classification"] == "directly_projectable"
        for row in configured
    ):
        raise ValueError(
            "T5.5 policy unexpectedly declares a placeholder directly projectable"
        )

    verified_source_blobs = (
        validate_source_tree(source_root, policy)
        if source_root is not None
        else {
            key: record["git_blob_sha1"]
            for key, record in source["source_files"].items()
        }
    )
    bronze_hashes = {
        path.relative_to(ROOT).as_posix(): sha256(path)
        for path in bronze_paths
    }
    return {
        "schema_version": 1,
        "status": "BLOCKED_ON_EXPLICIT_PREREQUISITES",
        "delivery_boundary": policy["delivery_boundary"],
        "replacement_authorized": policy["replacement_authorized"],
        "gt6_source": source,
        "source_verification": {
            "mode": "committed_git_blob_pins_with_optional_local_replay",
            "verified_git_blobs": verified_source_blobs,
        },
        "current_placeholder_template": template,
        "component_mappings": policy["component_mappings"],
        "counts": {
            "configured_machine_placeholders": len(configured),
            "existing_bronze_resources": len(bronze),
            "total_rows": len(rows),
            "classifications": {
                name: categories.get(name, 0)
                for name in sorted(ALLOWED_CLASSIFICATIONS)
            },
            "classified": len(rows) - unclassified,
            "unclassified": unclassified,
        },
        "replacement_gate": {
            "ready": False,
            "objective_prerequisites": [
                "Every configured placeholder has an exact approved source or divergence identity.",
                "Every selected GT6 tier is explicit, or an audited T6 tier-collapse rule exists.",
                "Every source casing, motor, cable, circuit, lens, bowl, and tool-head operand has an exact reachable CrucibleCraft mapping.",
                "Focused source-signature and survival-reachability tests pass before any placeholder is replaced."
            ],
            "reason": "Zero configured placeholders are directly projectable; source tier collapse, source identity, or exact component mappings remain unresolved.",
        },
        "source_hashes": {
            "builder": sha256(Path(__file__).resolve()),
            "policy": sha256(POLICY),
            "mod_recipe_provider": sha256(RECIPE_PROVIDER),
            "mod_processing_machines": sha256(PROCESSING_MACHINES),
            "mod_items": sha256(MOD_ITEMS),
            "material_prefixes": sha256(MATERIAL_PREFIXES),
            "gt6_reference_metadata": sha256(GT6_REFERENCE_METADATA),
            "existing_bronze_resources": bronze_hashes,
        },
        "configured_machines": configured,
        "existing_bronze_resources": bronze,
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed machine-crafting readiness ledger is stale",
    )
    parser.add_argument(
        "--verify-source",
        type=Path,
        help="verify fetched official GT6 files and their pinned source anchors",
    )
    parser.add_argument(
        "--fetch-source",
        type=Path,
        help="fetch only the two pinned official GT6 source files, then exit",
    )
    args = parser.parse_args()
    policy = load(POLICY)

    if args.fetch_source is not None:
        if args.check or args.verify_source is not None:
            parser.error("--fetch-source is an exclusive evidence command")
        fetch_source(args.fetch_source, policy)
        print(f"Fetched and verified pinned GT6 source under {args.fetch_source}")
        return 0

    document = build(args.verify_source)
    encoded = stable_json(document)
    if args.check:
        if not OUTPUT.is_file() or OUTPUT.read_text(
            encoding="utf-8"
        ) != encoded:
            raise SystemExit("machine-crafting readiness ledger is stale")
        print("Machine-crafting readiness ledger is current.")
        return 0
    OUTPUT.write_text(encoded, encoding="utf-8", newline="\n")
    print(f"wrote {OUTPUT.relative_to(ROOT)}")
    print(json.dumps(document["counts"], sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
