"""Validated access to the pinned GT6 electrical source extraction."""
from __future__ import annotations

import hashlib
import json
import re
import subprocess
from pathlib import Path
from typing import Any


REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
SOURCE_FILE_HASHES = {
    "src/main/java/gregapi/data/ANY.java":
        "6a26fa2293c4a315d2594c5ac80eba689389f9b1922a33547bdd20d2df724f71",
    "src/main/java/gregapi/data/CS.java":
        "407edce9f541bcf34b6573d59e4ae14e477fb9c145439deddb831e2a2e2887a5",
    "src/main/java/gregapi/data/OP.java":
        "4d93260aeec5ce2440b9e2577aa75af04155b4426f934ef51c0b861dfada290e",
    "src/main/java/gregapi/util/UT.java":
        "fd91c2354dfb655f386f65b71c46add1caa067b85958dd3cd61a469b63bc7709",
    "src/main/java/gregapi/tileentity/connectors/"
    "MultiTileEntityWireElectric.java":
        "60dd3bf9147a220ab327a1aed3f0081382678eb0d6190e865e84e1b7dae89724",
    "src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java":
        "96f579dd5c0a26a759ddee34c312dadbc5d52123e2b85e82da6a40f68b494393",
    "src/main/java/gregtech/loaders/b/Loader_OreProcessing.java":
        "bade43317484adffdca7b6bbaf3dc910da424d8ad3bcbe89235979044546c2c3",
}
WIRE_GAUGES = tuple(range(1, 17))
CABLE_GAUGES = (1, 2, 4, 8, 12)
VOLTAGE_TIERS = (
    8,
    32,
    128,
    512,
    2048,
    8192,
    32768,
    131072,
    524288,
    2097152,
    8388608,
    33554432,
    134217728,
    536870912,
    2147483648,
    8589934592,
)


class ElectricalSourceError(ValueError):
    """The reviewed GT6 electrical extraction is invalid."""


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def load(path: Path) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    validate(document)
    return document


def _contains_key(value: Any, key: str) -> bool:
    if isinstance(value, dict):
        return key in value or any(
            _contains_key(child, key) for child in value.values()
        )
    if isinstance(value, list):
        return any(_contains_key(child, key) for child in value)
    return False


def _evaluate_voltage(expression: str, constants: dict[str, int]) -> int:
    match = re.fullmatch(r"(V\[\d+])(?:\*(\d+))?", expression)
    if not match or match.group(1) not in constants:
        raise ElectricalSourceError(
            f"unsupported voltage expression: {expression!r}"
        )
    return constants[match.group(1)] * int(match.group(2) or 1)


def validate(document: dict[str, Any]) -> None:
    if document.get("schema_version") != 1:
        raise ElectricalSourceError("electrical source schema_version must be 1")
    source = document.get("source") or {}
    if source.get("repository") != "GregTech6/gregtech6":
        raise ElectricalSourceError("unexpected GT6 electrical repository")
    if source.get("revision") != REVISION:
        raise ElectricalSourceError("unexpected GT6 electrical revision")
    files = source.get("files") or {}
    actual_hashes = {
        path: value.get("sha256")
        for path, value in files.items()
        if isinstance(value, dict)
    }
    if actual_hashes != SOURCE_FILE_HASHES:
        raise ElectricalSourceError("GT6 electrical source-file hashes drifted")
    if _contains_key(document, "resistance"):
        raise ElectricalSourceError(
            "GT6 electrical source must not invent physical resistance"
        )

    constants = document.get("voltage_constants") or {}
    expected_constants = {
        f"V[{index}]": value
        for index, value in enumerate(VOLTAGE_TIERS)
    }
    if constants != expected_constants:
        raise ElectricalSourceError("GT6 voltage constants drifted")

    semantics = document.get("semantic_model") or {}
    for key in (
        "traversal",
        "overload",
        "burn_state",
        "contact_damage_runtime",
    ):
        if not isinstance(semantics.get(key), str) or not semantics[key]:
            raise ElectricalSourceError(
                f"missing GT6 runtime electrical semantic: {key}"
            )

    specifications = document.get("specifications") or {}
    expected_specs = {
        "wire": [
            {"name": f"wireGt{gauge:02d}", "gauge": gauge}
            for gauge in WIRE_GAUGES
        ],
        "cable": [
            {"name": f"cableGt{gauge:02d}", "gauge": gauge}
            for gauge in CABLE_GAUGES
        ],
    }
    if specifications != expected_specs:
        raise ElectricalSourceError("GT6 wire/cable specification table drifted")

    conductors = document.get("conductors") or []
    names = [row.get("source_name") for row in conductors]
    symbols = [row.get("source_symbol") for row in conductors]
    if (
        not conductors
        or len(set(names)) != len(names)
        or len(set(symbols)) != len(symbols)
        or any(not isinstance(value, str) or not value for value in names + symbols)
    ):
        raise ElectricalSourceError("invalid or duplicate GT6 conductor identity")
    required = {
        "source_symbol",
        "source_name",
        "voltage_expression",
        "max_voltage",
        "base_amperage",
        "wire_loss_per_meter",
        "cable_loss_per_meter",
        "wire_contact_damage",
        "cable_contact_damage",
        "cable_generated",
    }
    for row in conductors:
        if set(row) != required:
            raise ElectricalSourceError(
                f"{row.get('source_name')}: conductor fields drifted"
            )
        for field in (
            "max_voltage",
            "base_amperage",
            "wire_loss_per_meter",
            "cable_loss_per_meter",
        ):
            value = row[field]
            if not isinstance(value, int) or isinstance(value, bool) or value <= 0:
                raise ElectricalSourceError(
                    f"{row['source_name']}: invalid {field}"
                )
        for field in (
            "wire_contact_damage",
            "cable_contact_damage",
            "cable_generated",
        ):
            if not isinstance(row[field], bool):
                raise ElectricalSourceError(
                    f"{row['source_name']}: invalid {field}"
                )
        evaluated = _evaluate_voltage(row["voltage_expression"], constants)
        if evaluated != row["max_voltage"]:
            raise ElectricalSourceError(
                f"{row['source_name']}: voltage expression/value mismatch"
            )

    insulation = document.get("insulation") or {}
    if (
        insulation.get("group_source_name") != "AnyRubber"
        or insulation.get("material_source_names") != ["Rubber"]
    ):
        raise ElectricalSourceError("GT6 insulation extraction drifted")


def specifications_for_conductor(
    document: dict[str, Any],
    conductor: dict[str, Any],
) -> dict[str, dict[str, Any]]:
    result: dict[str, dict[str, Any]] = {}
    for kind in ("wire", "cable"):
        if kind == "cable" and not conductor["cable_generated"]:
            continue
        loss_field = f"{kind}_loss_per_meter"
        contact_field = f"{kind}_contact_damage"
        for specification in document["specifications"][kind]:
            result[specification["name"]] = {
                "max_voltage": conductor["max_voltage"],
                "max_amperage": (
                    conductor["base_amperage"] * specification["gauge"]
                ),
                "loss_per_meter": conductor[loss_field],
                "insulated": kind == "cable",
                "contact_damage": conductor[contact_field],
            }
    return dict(sorted(result.items()))


def specifications_by_source(
    document: dict[str, Any],
) -> dict[str, dict[str, dict[str, Any]]]:
    validate(document)
    return {
        conductor["source_name"]: specifications_for_conductor(
            document, conductor
        )
        for conductor in document["conductors"]
    }


def validate_source_root(document: dict[str, Any], source_root: Path) -> None:
    """Optionally verify an installed source checkout against the extraction."""
    validate(document)
    try:
        head = subprocess.check_output(
            ["git", "-C", str(source_root), "rev-parse", "HEAD"],
            stderr=subprocess.STDOUT,
            text=True,
        ).strip()
        if head != REVISION:
            raise ElectricalSourceError(
                f"installed GT6 checkout is at {head}, expected {REVISION}"
            )
        actual_hashes = {
            relative: hashlib.sha256(subprocess.check_output([
                "git",
                "-C",
                str(source_root),
                "show",
                f"{REVISION}:{relative}",
            ])).hexdigest()
            for relative in SOURCE_FILE_HASHES
        }
    except (OSError, subprocess.CalledProcessError) as error:
        raise ElectricalSourceError(
            f"cannot read installed GT6 git checkout: {source_root}"
        ) from error
    mismatches = [
        relative
        for relative, expected in SOURCE_FILE_HASHES.items()
        if actual_hashes[relative] != expected
    ]
    if mismatches:
        raise ElectricalSourceError(
            "installed GT6 source hash mismatch: " + ", ".join(mismatches)
        )
