"""Validated access to the pinned GT6 pipe source extraction."""
from __future__ import annotations

import hashlib
import json
import re
import subprocess
from pathlib import Path
from typing import Any


GT6_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
GTM_REVISION = "de5d2c4a4c863b94a10bfb5d0839df2de8246628"
GT6_SOURCE_FILE_HASHES = {
    "src/main/java/gregapi/tileentity/connectors/"
    "MultiTileEntityPipeFluid.java":
        "3a6850055375e7975145b03abab44c5922746a6f4053dd8d3163de989f37b110",
    "src/main/java/gregapi/tileentity/connectors/"
    "MultiTileEntityPipeItem.java":
        "0e9aa956b807d8390b20dd70f224b2b8d0a7be8df25fe66660abffda8b88069b",
    "src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java":
        "96f579dd5c0a26a759ddee34c312dadbc5d52123e2b85e82da6a40f68b494393",
}
GTM_SOURCE_FILE_HASHES = {
    "src/main/java/com/gregtechceu/gtceu/common/pipelike/fluidpipe/"
    "FluidPipeType.java":
        "f55ab727dc883552ffb73a0f6e4ed2fdf185fd3ee400de9a23f435d9b5492f59",
    "src/main/java/com/gregtechceu/gtceu/common/pipelike/item/"
    "ItemPipeType.java":
        "3c47111960837adb492af7438bb236142326b939f73ec76a8898c64866e0d056",
}
EXTRACTION_SHA256 = (
    "71bb8e36c2956cbcbfe2db6f09a100fba3c38f8dd9a72f37868c10f108b5f89b"
)
FLUID_DIRECT_COUNT = 40
ITEM_DIRECT_COUNT = 21


class PipeSourceError(ValueError):
    """The reviewed GT6 pipe extraction is invalid."""


def _stable_hash(value: Any) -> str:
    encoded = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def _source_hashes(source: dict[str, Any]) -> dict[str, str | None]:
    return {
        path: row.get("sha256") if isinstance(row, dict) else None
        for path, row in (source.get("files") or {}).items()
    }


def _validate_rows(
    rows: list[dict[str, Any]],
    *,
    kind: str,
    expected_count: int,
    required: set[str],
) -> None:
    if len(rows) != expected_count:
        raise PipeSourceError(
            f"GT6 {kind} direct-call count drifted: "
            f"{len(rows)} != {expected_count}"
        )
    names = [row.get("source_name") for row in rows]
    symbols = [row.get("source_symbol") for row in rows]
    ids = [row.get("base_id") for row in rows]
    if (
        len(set(names)) != len(names)
        or len(set(symbols)) != len(symbols)
        or len(set(ids)) != len(ids)
        or any(not isinstance(value, str) or not value for value in names)
        or any(not isinstance(value, str) or not value for value in symbols)
    ):
        raise PipeSourceError(f"invalid or duplicate GT6 {kind} identity")
    for row in rows:
        name = str(row.get("source_name") or "<unknown>")
        if set(row) != required:
            raise PipeSourceError(f"{name}: GT6 {kind} call fields drifted")
        for field in ("base_id", "creative_tab_id"):
            value = row[field]
            if not isinstance(value, int) or isinstance(value, bool) or value <= 0:
                raise PipeSourceError(f"{name}: invalid {field}")
        if not isinstance(row["block_symbol"], str) or not row["block_symbol"]:
            raise PipeSourceError(f"{name}: invalid block_symbol")


def validate(document: dict[str, Any]) -> None:
    if document.get("schema_version") != 1:
        raise PipeSourceError("pipe source schema_version must be 1")
    gt6 = document.get("gt6_source") or {}
    if (
        gt6.get("repository") != "GregTech6/gregtech6"
        or gt6.get("revision") != GT6_REVISION
        or gt6.get("license") != "LGPL-3.0-or-later"
    ):
        raise PipeSourceError("unexpected GT6 pipe source identity")
    if _source_hashes(gt6) != GT6_SOURCE_FILE_HASHES:
        raise PipeSourceError("GT6 pipe source-file hashes drifted")

    gtm = document.get("gtm_reference") or {}
    if (
        gtm.get("repository") != "GregTechCEu/GregTech-Modern"
        or gtm.get("revision") != GTM_REVISION
        or gtm.get("license") != "LGPL-3.0"
    ):
        raise PipeSourceError("unexpected GTM pipe reference identity")
    if _source_hashes(gtm) != GTM_SOURCE_FILE_HASHES:
        raise PipeSourceError("GTM pipe reference source-file hashes drifted")

    semantics = document.get("semantic_model") or {}
    for field in (
        "fluid_eligibility",
        "item_eligibility",
        "fluid_temperature",
        "fluid_capacity",
        "item_transport",
        "extensions",
    ):
        if not isinstance(semantics.get(field), str) or not semantics[field]:
            raise PipeSourceError(f"missing GT6 pipe semantic: {field}")

    fluid_gauges = document.get("fluid_gauges") or []
    if fluid_gauges != [
        {
            "name": "tiny",
            "source_prefix": "pipeTiny",
            "capacity_multiplier": 1,
            "tank_count": 1,
        },
        {
            "name": "small",
            "source_prefix": "pipeSmall",
            "capacity_multiplier": 2,
            "tank_count": 1,
        },
        {
            "name": "normal",
            "source_prefix": "pipeMedium",
            "capacity_multiplier": 6,
            "tank_count": 1,
        },
        {
            "name": "large",
            "source_prefix": "pipeLarge",
            "capacity_multiplier": 12,
            "tank_count": 1,
        },
        {
            "name": "huge",
            "source_prefix": "pipeHuge",
            "capacity_multiplier": 24,
            "tank_count": 1,
        },
        {
            "name": "quadruple",
            "source_prefix": "pipeQuadruple",
            "capacity_multiplier": 6,
            "tank_count": 4,
        },
        {
            "name": "nonuple",
            "source_prefix": "pipeNonuple",
            "capacity_multiplier": 2,
            "tank_count": 9,
        },
    ]:
        raise PipeSourceError("GT6 fluid gauge table drifted")
    item_gauges = document.get("item_gauges") or []
    if item_gauges != [
        {
            "name": "normal",
            "source_prefix": "pipeMedium",
            "step_size_divisor": 1,
            "inventory_multiplier": 1,
            "restrictive": False,
        },
        {
            "name": "large",
            "source_prefix": "pipeLarge",
            "step_size_divisor": 2,
            "inventory_multiplier": 2,
            "restrictive": False,
        },
        {
            "name": "huge",
            "source_prefix": "pipeHuge",
            "step_size_divisor": 4,
            "inventory_multiplier": 4,
            "restrictive": False,
        },
        {
            "name": "restrictive_normal",
            "source_prefix": "pipeRestrictiveMedium",
            "step_size_multiplier": 100,
            "inventory_multiplier": 1,
            "restrictive": True,
        },
        {
            "name": "restrictive_large",
            "source_prefix": "pipeRestrictiveLarge",
            "step_size_multiplier": 50,
            "inventory_multiplier": 2,
            "restrictive": True,
        },
        {
            "name": "restrictive_huge",
            "source_prefix": "pipeRestrictiveHuge",
            "step_size_multiplier": 25,
            "inventory_multiplier": 4,
            "restrictive": True,
        },
    ]:
        raise PipeSourceError("GT6 item gauge table drifted")

    fluid_rows = document.get("direct_addFluidPipes_calls") or []
    _validate_rows(
        fluid_rows,
        kind="fluid",
        expected_count=FLUID_DIRECT_COUNT,
        required={
            "base_id",
            "creative_tab_id",
            "base_capacity",
            "gas_proof",
            "acid_proof",
            "plasma_proof",
            "magic_proof",
            "contact_damage",
            "flammable",
            "recipe",
            "blocking",
            "block_symbol",
            "max_temperature_override",
            "source_symbol",
            "source_name",
        },
    )
    for row in fluid_rows:
        name = row["source_name"]
        capacity = row["base_capacity"]
        if (
            not isinstance(capacity, int)
            or isinstance(capacity, bool)
            or capacity <= 0
        ):
            raise PipeSourceError(f"{name}: invalid base_capacity")
        for field in (
            "gas_proof",
            "acid_proof",
            "plasma_proof",
            "magic_proof",
            "contact_damage",
            "flammable",
            "recipe",
            "blocking",
        ):
            if not isinstance(row[field], bool):
                raise PipeSourceError(f"{name}: invalid {field}")
        maximum = row["max_temperature_override"]
        if maximum is not None and (
            not isinstance(maximum, int)
            or isinstance(maximum, bool)
            or maximum <= 0
        ):
            raise PipeSourceError(
                f"{name}: invalid max_temperature_override"
            )

    item_rows = document.get("direct_addItemPipes_calls") or []
    _validate_rows(
        item_rows,
        kind="item",
        expected_count=ITEM_DIRECT_COUNT,
        required={
            "base_id",
            "creative_tab_id",
            "base_step_size",
            "base_inventory_size",
            "recipe",
            "blocking",
            "block_symbol",
            "source_symbol",
            "source_name",
        },
    )
    for row in item_rows:
        name = row["source_name"]
        for field in ("base_step_size", "base_inventory_size"):
            value = row[field]
            if not isinstance(value, int) or isinstance(value, bool) or value <= 0:
                raise PipeSourceError(f"{name}: invalid {field}")
        for field in ("recipe", "blocking"):
            if not isinstance(row[field], bool):
                raise PipeSourceError(f"{name}: invalid {field}")

    extraction = {
        "fluid_gauges": fluid_gauges,
        "item_gauges": item_gauges,
        "fluid": fluid_rows,
        "item": item_rows,
    }
    if _stable_hash(extraction) != EXTRACTION_SHA256:
        raise PipeSourceError("reviewed GT6 pipe extraction drifted")


def load(path: Path) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    validate(document)
    return document


def fluid_specifications(
    row: dict[str, Any],
    gauges: list[dict[str, Any]],
    melting_point_kelvin: int,
) -> dict[str, dict[str, Any]]:
    maximum = row.get("max_temperature_override")
    if maximum is None:
        maximum = int(melting_point_kelvin * 1.25)
    return {
        gauge["name"]: {
            "capacity_mb": row["base_capacity"]
            * gauge["capacity_multiplier"],
            "tank_count": gauge["tank_count"],
            "max_temperature_kelvin": maximum,
            "gas_proof": row["gas_proof"],
            "acid_proof": row["acid_proof"],
            "plasma_proof": row["plasma_proof"],
            "magic_proof": row["magic_proof"],
            "contact_damage": row["contact_damage"],
            "flammable": row["flammable"],
            "recipe": row["recipe"],
            "blocking": row["blocking"],
        }
        for gauge in gauges
    }


def item_specifications(
    row: dict[str, Any],
    gauges: list[dict[str, Any]],
) -> dict[str, dict[str, Any]]:
    return {
        gauge["name"]: {
            "step_size": row["base_step_size"]
            // gauge["step_size_divisor"],
            "stacks_per_second": row["base_inventory_size"]
            * gauge["inventory_multiplier"],
            "recipe": row["recipe"],
            "blocking": row["blocking"],
        }
        for gauge in gauges
    }


def _parse_loader_calls(
    loader_text: str,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    boolean = {"T": True, "F": False}
    item_rows = []
    for encoded in re.findall(
        r"MultiTileEntityPipeItem\.addItemPipes\((.*?)\);",
        loader_text,
    ):
        values = [value.strip() for value in encoded.split(",")]
        if len(values) != 10:
            raise PipeSourceError("cannot parse pinned addItemPipes call")
        item_rows.append({
            "base_id": int(values[0]),
            "creative_tab_id": int(values[1]),
            "base_step_size": int(values[2]),
            "base_inventory_size": int(values[3]),
            "recipe": boolean[values[4]],
            "blocking": boolean[values[5]],
            "block_symbol": values[7],
            "source_symbol": values[9].removeprefix("MT."),
        })

    fluid_rows = []
    for encoded in re.findall(
        r"MultiTileEntityPipeFluid\.addFluidPipes\((.*?)\);",
        loader_text,
    ):
        values = [value.strip() for value in encoded.split(",")]
        if len(values) not in (15, 16):
            raise PipeSourceError("cannot parse pinned addFluidPipes call")
        has_override = len(values) == 16
        fluid_rows.append({
            "base_id": int(values[0]),
            "creative_tab_id": int(values[1]),
            "base_capacity": int(values[2]),
            "gas_proof": boolean[values[3]],
            "acid_proof": boolean[values[4]],
            "plasma_proof": boolean[values[5]],
            "magic_proof": boolean[values[6]],
            "contact_damage": boolean[values[7]],
            "flammable": boolean[values[8]],
            "recipe": boolean[values[9]],
            "blocking": boolean[values[10]],
            "block_symbol": values[12],
            "max_temperature_override": (
                int(values[14]) if has_override else None
            ),
            "source_symbol": values[-1].removeprefix("MT."),
        })
    return fluid_rows, item_rows


def validate_source_root(
    document: dict[str, Any],
    source_root: Path,
) -> None:
    """Optionally verify an installed GT6 checkout against the extraction."""
    validate(document)
    try:
        head = subprocess.check_output(
            ["git", "-C", str(source_root), "rev-parse", "HEAD"],
            stderr=subprocess.STDOUT,
            text=True,
        ).strip()
        if head != GT6_REVISION:
            raise PipeSourceError(
                f"installed GT6 checkout is at {head}, "
                f"expected {GT6_REVISION}"
            )
        contents = {
            relative: subprocess.check_output([
                    "git",
                    "-C",
                    str(source_root),
                    "show",
                    f"{GT6_REVISION}:{relative}",
                ])
            for relative in GT6_SOURCE_FILE_HASHES
        }
        actual = {
            relative: hashlib.sha256(content).hexdigest()
            for relative, content in contents.items()
        }
    except (OSError, subprocess.CalledProcessError) as error:
        raise PipeSourceError(
            f"cannot read installed GT6 git checkout: {source_root}"
        ) from error
    mismatches = [
        relative
        for relative, expected in GT6_SOURCE_FILE_HASHES.items()
        if actual[relative] != expected
    ]
    if mismatches:
        raise PipeSourceError(
            "installed GT6 pipe source hash mismatch: "
            + ", ".join(mismatches)
        )
    loader_path = (
        "src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java"
    )
    parsed_fluid, parsed_item = _parse_loader_calls(
        contents[loader_path].decode("utf-8")
    )
    expected_fluid = [
        {
            key: value
            for key, value in row.items()
            if key != "source_name"
        }
        for row in document["direct_addFluidPipes_calls"]
    ]
    expected_item = [
        {
            key: value
            for key, value in row.items()
            if key != "source_name"
        }
        for row in document["direct_addItemPipes_calls"]
    ]
    if parsed_fluid != expected_fluid:
        raise PipeSourceError(
            "reviewed fluid rows do not match pinned addFluidPipes calls"
        )
    if parsed_item != expected_item:
        raise PipeSourceError(
            "reviewed item rows do not match pinned addItemPipes calls"
        )
