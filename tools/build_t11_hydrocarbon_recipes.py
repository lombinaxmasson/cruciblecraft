#!/usr/bin/env python3
"""Generate the four fixed-row T11 hydrocarbon recipes."""

from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import build_t11_preflight_projection as preflight


OUTPUT_ROOT = ROOT / "src/t11_hydrocarbon_generated/resources"
MANIFEST = ROOT / "tools/t11_hydrocarbon_recipe_manifest.json"
ROUTES = {
    "crude_oil_distillation": (
        "distillery",
        "t11/distillery/crude_oil_to_fuel_and_lubricant",
    ),
    "natural_gas_to_methane": (
        "generifier",
        "t11/generifier/natural_gas_to_methane",
    ),
    "fuel_oil_engine": (
        "fuels_engine",
        "t11/fuels_engine/fuel_oil",
    ),
    "methane_gas_fuel": (
        "fuels_gas",
        "t11/fuels_gas/methane",
    ),
}


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def digest_text(value: str) -> str:
    return hashlib.sha256(value.encode("utf-8")).hexdigest()


def digest_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def recipe(route: dict[str, Any], map_id: str) -> dict[str, Any]:
    projection = route["runtime_projection"]
    return {
        "can_be_buffered": True,
        "duration": route["source_recipe"]["duration"],
        "eut": route["source_recipe"]["eut"],
        "fluid_inputs": [
            {"amount": stack["amount"], "id": stack["id"]}
            for stack in projection["fluid_inputs"]
        ],
        "fluid_outputs": [
            {"amount": stack["amount"], "id": stack["id"]}
            for stack in projection["fluid_outputs"]
        ],
        "map": f"cruciblecraft:{map_id}",
        "provenance": {
            "evidence_hashes": [
                preflight.load(preflight.POLICY)["source_revision"],
                route["source"]["row_sha256"],
            ],
            "selected_source_recipe": route["source"]["path"],
            "source_kind": "gt6_pinned_dump_projection",
        },
        "type": "cruciblecraft:gt_recipe",
    }


def planned_documents() -> tuple[dict[str, str], str]:
    projection = preflight.build()
    if projection["status"] not in {
        "T11B_READY",
        "T11C_READY",
        "T11D_READY",
        "T11_READY",
    }:
        raise ValueError("T11 preflight is not ready for recipe publication")
    selected = projection["selected_routes"]
    if set(selected) != set(ROUTES):
        raise ValueError("T11 recipe route set drifted from independent expectation")
    files: dict[str, str] = {}
    manifest_rows: list[dict[str, Any]] = []
    for route_id, (map_id, recipe_id) in sorted(ROUTES.items()):
        document = recipe(selected[route_id], map_id)
        relative = f"data/cruciblecraft/recipe/{recipe_id}.json"
        content = stable(document)
        files[relative] = content
        manifest_rows.append({
            "id": f"cruciblecraft:{recipe_id}",
            "map": f"cruciblecraft:{map_id}",
            "route": route_id,
            "sha256": digest_text(content),
            "source": selected[route_id]["source"],
        })
    output_hashes = {
        relative: digest_text(content)
        for relative, content in sorted(files.items())
    }
    manifest = {
        "schema_version": 2,
        "status": "T11_FIXED_ROWS_PUBLISHED",
        "counts": {
            "chemical_processing": 2,
            "fuel_generation": 2,
            "material_rules": 0,
            "published": len(files),
        },
        "recipes": manifest_rows,
        "route_set_sha256": projection["independent_expectation"][
            "route_set_sha256"
        ],
        "output_hashes": output_hashes,
        "proof": {
            "proof_tier": "full_replay",
            "builder_sha256": digest_file(Path(__file__).resolve()),
            "preflight_builder_sha256": digest_file(
                Path(preflight.__file__).resolve()
            ),
            "preflight_artifact_sha256": digest_file(preflight.OUTPUT),
            "output_tree_sha256": digest_text(stable(output_hashes)),
        },
    }
    return files, stable(manifest)


def actual_files() -> dict[str, Path]:
    if not OUTPUT_ROOT.is_dir():
        return {}
    return {
        path.relative_to(OUTPUT_ROOT).as_posix(): path
        for path in OUTPUT_ROOT.rglob("*.json")
    }


def check() -> list[str]:
    planned, manifest = planned_documents()
    actual = actual_files()
    errors = [
        f"generated file set drift: {path}"
        for path in sorted(set(planned) ^ set(actual))
    ]
    for relative in sorted(set(planned) & set(actual)):
        if actual[relative].read_text(encoding="utf-8") != planned[relative]:
            errors.append(f"stale generated recipe: {relative}")
    if not MANIFEST.is_file() or MANIFEST.read_text(
        encoding="utf-8"
    ) != manifest:
        errors.append("stale T11 hydrocarbon recipe manifest")
    return errors


def reference_only_check() -> list[str]:
    if not MANIFEST.is_file():
        return ["missing T11 hydrocarbon recipe manifest"]
    try:
        manifest = json.loads(MANIFEST.read_text(encoding="utf-8"))
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        return [f"invalid T11 hydrocarbon recipe manifest: {exc}"]
    errors: list[str] = []
    if manifest.get("schema_version") != 2:
        errors.append("T11 hydrocarbon manifest schema_version must be 2")
    if manifest.get("status") != "T11_FIXED_ROWS_PUBLISHED":
        errors.append("T11 hydrocarbon manifest status drifted")
    proof = manifest.get("proof") or {}
    expected_proof = {
        "proof_tier": "full_replay",
        "builder_sha256": digest_file(Path(__file__).resolve()),
        "preflight_builder_sha256": digest_file(
            Path(preflight.__file__).resolve()
        ),
        "preflight_artifact_sha256": digest_file(preflight.OUTPUT),
    }
    for key, expected in expected_proof.items():
        if proof.get(key) != expected:
            errors.append(f"T11 compact proof {key} drifted")

    output_hashes = manifest.get("output_hashes") or {}
    if proof.get("output_tree_sha256") != digest_text(
        stable(output_hashes)
    ):
        errors.append("T11 compact output tree receipt drifted")
    actual = actual_files()
    if set(actual) != set(output_hashes):
        errors.append(
            "generated file set drift: "
            f"missing={sorted(set(output_hashes) - set(actual))}, "
            f"extra={sorted(set(actual) - set(output_hashes))}"
        )
    rows = manifest.get("recipes") or []
    if len(rows) != 4 or set(ROUTES) != {
        row.get("route") for row in rows
    }:
        errors.append("T11 compact route set drifted")
    rows_by_relative = {
        "data/cruciblecraft/recipe/"
        + row["id"].removeprefix("cruciblecraft:")
        + ".json": row
        for row in rows
        if isinstance(row, dict) and isinstance(row.get("id"), str)
    }
    if set(rows_by_relative) != set(output_hashes):
        errors.append("T11 compact manifest recipe identities drifted")
    for relative, path in sorted(actual.items()):
        actual_hash = digest_file(path)
        if output_hashes.get(relative) != actual_hash:
            errors.append(f"generated recipe hash drifted: {relative}")
            continue
        row = rows_by_relative.get(relative)
        if row is None or row.get("sha256") != actual_hash:
            errors.append(f"manifest recipe hash drifted: {relative}")
            continue
        document = json.loads(path.read_text(encoding="utf-8"))
        provenance = document.get("provenance") or {}
        source = row.get("source") or {}
        if (
            provenance.get("selected_source_recipe") != source.get("path")
            or source.get("row_sha256")
            not in provenance.get("evidence_hashes", [])
        ):
            errors.append(f"recipe provenance drifted: {relative}")
    return errors


def write() -> None:
    planned, manifest = planned_documents()
    if OUTPUT_ROOT.exists():
        shutil.rmtree(OUTPUT_ROOT)
    for relative, content in planned.items():
        path = OUTPUT_ROOT / relative
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content, encoding="utf-8", newline="\n")
    MANIFEST.write_text(manifest, encoding="utf-8", newline="\n")


def main() -> int:
    parser = argparse.ArgumentParser()
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--write", action="store_true")
    modes.add_argument("--check", action="store_true")
    check_mode = parser.add_mutually_exclusive_group()
    check_mode.add_argument("--reference-only", action="store_true")
    check_mode.add_argument("--full-replay", action="store_true")
    args = parser.parse_args()
    if (
        args.reference_only or args.full_replay
    ) and not args.check:
        parser.error("--reference-only and --full-replay require --check")
    if args.write:
        write()
        print("Wrote 4 fixed-row T11 hydrocarbon recipes.")
        return 0
    errors = reference_only_check() if args.reference_only else check()
    if errors:
        print("T11 hydrocarbon recipes are stale:")
        for error in errors:
            print(f"- {error}")
        return 1
    tier = "compact" if args.reference_only else "full replay"
    print(
        "T11 hydrocarbon recipes match their pinned "
        f"{tier} source rows."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
