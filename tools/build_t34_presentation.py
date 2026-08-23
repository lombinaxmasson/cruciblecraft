#!/usr/bin/env python3
"""Build and validate the T34 GT6 runtime-to-art receipt."""
from __future__ import annotations

import argparse
import copy
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

TOOLS = common.TOOLS
POLICY = TOOLS / "t34_presentation_policy.json"
OUTPUT = TOOLS / "t34_presentation.json"
GT6_RAW_ROOT = (
    "gt6_code/gregtech6/src/main/resources/assets/gregtech/textures/blocks"
)
STEAM_GEOMETRY = "textures/gt6模型/MultiTileEntityEngineSteam.json"


def _machine_paths(
    root: str,
    colored: tuple[str, ...],
    overlay: tuple[str, ...],
    active: tuple[str, ...] = (),
    active_mcmeta: tuple[str, ...] = (),
) -> tuple[str, ...]:
    paths = [
        *(f"{root}/colored/{name}.png" for name in colored),
        *(f"{root}/overlay/{name}.png" for name in overlay),
        *(f"{root}/overlay_active/{name}.png" for name in active),
        *(f"{root}/overlay_active/{name}.png.mcmeta" for name in active_mcmeta),
    ]
    return tuple(paths)


FAMILY_CONTRACTS = {
    "anvil_rough": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/tileentity/tools/MultiTileEntityAnvil.java",
        "class_name": "MultiTileEntityAnvil",
        "required_tokens": (),
        "raw_files": (
            "materialicons/ROUGH/blockSolid.png",
            "materialicons/ROUGH/blockSolid_OVERLAY.png",
        ),
    },
    "smeltery_rough": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/tileentity/tools/MultiTileEntitySmeltery.java",
        "class_name": "MultiTileEntitySmeltery",
        "required_tokens": (),
        "raw_files": (
            "materialicons/ROUGH/blockSolid.png",
            "materialicons/ROUGH/blockSolid_OVERLAY.png",
        ),
    },
    "mold_rough": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/tileentity/tools/MultiTileEntityMold.java",
        "class_name": "MultiTileEntityMold",
        "required_tokens": (),
        "raw_files": (
            "materialicons/ROUGH/blockSolid.png",
            "materialicons/ROUGH/molten.png",
            "materialicons/ROUGH/molten.png.mcmeta",
        ),
    },
    "firebricks": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java",
        "class_name": "Loader_MultiTileEntities",
        "required_tokens": ("Fire Bricks", 'NBT_TEXTURE, "firebricks"'),
        "raw_files": _machine_paths(
            "machines/multiblockparts/firebricks/0",
            ("bottom", "side", "top"),
            ("bottom", "side", "top"),
        ),
    },
    "burning_brick": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/generators/MultiTileEntityGeneratorBrick.java",
        "class_name": "MultiTileEntityGeneratorBrick",
        "required_tokens": ("machines/generators/burning_brick",),
        "raw_files": _machine_paths(
            "machines/generators/burning_brick",
            ("bottom", "top", "left", "front", "right", "back"),
            ("bottom", "top", "left", "front", "right", "back"),
            ("bottom", "top", "left", "front", "right", "back"),
            ("front", "top"),
        ),
    },
    "burning_solid": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/generators/MultiTileEntityGeneratorMetal.java",
        "class_name": "MultiTileEntityGeneratorMetal",
        "required_tokens": ("machines/generators/burning_solid",),
        "raw_files": _machine_paths(
            "machines/generators/burning_solid",
            ("bottom", "top", "left", "front", "right", "back"),
            ("bottom", "top", "left", "front", "right", "back"),
            ("bottom", "top", "left", "front", "right", "back"),
            ("front",),
        ),
    },
    "burning_liquid": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/generators/MultiTileEntityGeneratorLiquid.java",
        "class_name": "MultiTileEntityGeneratorLiquid",
        "required_tokens": ("machines/generators/burning_liquid",),
        "raw_files": _machine_paths(
            "machines/generators/burning_liquid",
            ("bottom", "top", "left", "front", "right", "back"),
            ("bottom", "top", "left", "front", "right", "back"),
            ("bottom", "top", "left", "front", "right", "back"),
            ("front",),
        ),
    },
    "burning_gas": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/generators/MultiTileEntityGeneratorGas.java",
        "class_name": "MultiTileEntityGeneratorGas",
        "required_tokens": ("machines/generators/burning_gas",),
        "raw_files": _machine_paths(
            "machines/generators/burning_gas",
            ("bottom", "top", "left", "front", "right", "back"),
            ("bottom", "top", "left", "front", "right", "back"),
            ("bottom", "top", "left", "front", "right", "back"),
            ("front",),
        ),
    },
    "motor_liquid": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/generators/MultiTileEntityMotorLiquid.java",
        "class_name": "MultiTileEntityMotorLiquid",
        "required_tokens": ("machines/generators/motor_liquid",),
        "raw_files": _machine_paths(
            "machines/generators/motor_liquid",
            ("front", "back", "sides"),
            ("front", "back", "sides"),
            ("front", "back", "sides"),
            ("front", "sides"),
        ),
    },
    "boiler_steam": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/converters/MultiTileEntityBoilerTank.java",
        "class_name": "MultiTileEntityBoilerTank",
        "required_tokens": ("machines/tanks/boiler_steam",),
        "raw_files": _machine_paths(
            "machines/tanks/boiler_steam",
            ("bottom", "top", "side"),
            ("bottom", "top", "side"),
        ),
    },
    "kinetic_steam": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/converters/MultiTileEntityEngineSteam.java",
        "class_name": "MultiTileEntityEngineSteam",
        "required_tokens": ("machines/engines/kinetic_steam",),
        "raw_files": _machine_paths(
            "machines/engines/kinetic_steam",
            (
                "front",
                "back",
                "side",
                "cage",
                "pipe_side",
                "pipe",
                "engine",
                "engine_hull",
            ),
            (
                "front",
                "back",
                "side",
                "cage",
                "pipe_side",
                "pipe",
                "engine",
                "engine_hull",
            ),
        ),
    },
    "electric_rotation": {
        "java_path": "gt6_code/gregtech6/src/main/java/gregtech/tileentity/energy/converters/MultiTileEntityDynamoElectric.java",
        "class_name": "MultiTileEntityDynamoElectric",
        "required_tokens": ("machines/dynamos/electric_rotation",),
        "raw_files": _machine_paths(
            "machines/dynamos/electric_rotation",
            ("front", "back", "side"),
            ("front", "back", "side"),
            ("front", "back", "side"),
        ),
    },
}

REQUIRED_MEMBER_IDS = frozenset(
    {
        "cruciblecraft:firebrick",
        "cruciblecraft:anvil",
        "cruciblecraft:raw_ceramic_crucible",
        "cruciblecraft:crucible",
        "cruciblecraft:raw_ceramic_mold",
        "cruciblecraft:raw_ingot_mold",
        "cruciblecraft:ingot_mold",
        "cruciblecraft:raw_plate_mold",
        "cruciblecraft:plate_mold",
        "cruciblecraft:raw_rod_mold",
        "cruciblecraft:rod_mold",
        "cruciblecraft:raw_bolt_mold",
        "cruciblecraft:bolt_mold",
        "cruciblecraft:firebox",
        "gt6:burning_solid",
        "gt6:burning_liquid",
        "gt6:burning_gas",
        "cruciblecraft:fuel_engine",
        "cruciblecraft:burning_gas_generator",
        "cruciblecraft:bronze_boiler",
        "cruciblecraft:bronze_steam_engine",
        "cruciblecraft:bronze_dynamo",
        "cruciblecraft:small_gas_generator",
    }
)

IDENTITY_FAMILIES = {
    "gt6:burning_solid": "burning_solid",
    "gt6:burning_liquid": "burning_liquid",
    "gt6:burning_gas": "burning_gas",
    "cruciblecraft:fuel_engine": "motor_liquid",
    "cruciblecraft:burning_gas_generator": "burning_gas",
    "cruciblecraft:bronze_boiler": "boiler_steam",
    "cruciblecraft:bronze_steam_engine": "kinetic_steam",
    "cruciblecraft:bronze_dynamo": "electric_rotation",
    "cruciblecraft:small_gas_generator": "burning_gas",
}


def _load_json(path: Path) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(document, dict):
        raise ValueError(f"{common.relative(path)} must be a JSON object")
    return document


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _path(value: str) -> Path:
    return ROOT / value


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def _texture_id(destination: str) -> str | None:
    for prefix in (
        "src/main/resources/assets/cruciblecraft/textures/",
        "src/generated/resources/assets/cruciblecraft/textures/",
    ):
        if destination.startswith(prefix) and destination.endswith(".png"):
            return "cruciblecraft:" + destination[len(prefix) : -4]
    return None


def _imports_for_member(member: dict[str, Any]) -> list[dict[str, str]]:
    explicit = member.get("imports")
    if explicit is not None:
        return [
            {
                "source": str(item.get("source", "")),
                "destination": str(item.get("destination", "")),
            }
            for item in explicit
            if isinstance(item, dict)
        ]
    family = member.get("source_family")
    destination_root = member.get("import_destination_root")
    if not isinstance(family, str) or family not in FAMILY_CONTRACTS:
        return []
    if not isinstance(destination_root, str):
        return []
    raw_files = FAMILY_CONTRACTS[family]["raw_files"]
    family_root = str(Path(raw_files[0]).parent.parent).replace("\\", "/")
    flatten = bool(member.get("flatten_import_destinations"))
    return [
        {
            "source": raw_file,
            "destination": (
                f"{destination_root}/"
                f"{raw_file.removeprefix(family_root + '/').replace('/', '_')}"
                if flatten
                else f"{destination_root}/{raw_file.removeprefix(family_root + '/')}"
            ),
        }
        for raw_file in raw_files
    ]


def _family_errors(
    name: str, family: dict[str, Any], inputs: set[Path]
) -> list[str]:
    contract = FAMILY_CONTRACTS[name]
    errors: list[str] = []
    evidence = family.get("source_evidence")
    if not isinstance(evidence, dict):
        return [f"source family {name} lacks source_evidence"]
    java_path = str(evidence.get("java_path", ""))
    expected_java = str(contract["java_path"])
    if java_path != expected_java:
        errors.append(f"source family {name} must cite {expected_java}")
        return errors
    path = _path(java_path)
    if not path.is_file():
        return [*errors, f"source family {name} Java evidence is missing: {java_path}"]
    inputs.add(path)
    if evidence.get("class_name") != contract["class_name"]:
        errors.append(f"source family {name} must cite {contract['class_name']}")
    actual_java_hash = _sha256(path)
    if evidence.get("sha256") != actual_java_hash:
        errors.append(f"source family {name} Java source hash is not current")
    java_text = path.read_text(encoding="utf-8")
    for token in (f"class {contract['class_name']}", *contract["required_tokens"]):
        if token not in java_text:
            errors.append(f"source family {name} Java evidence lacks {token!r}")

    raw_files = family.get("raw_files")
    if not isinstance(raw_files, dict):
        return [*errors, f"source family {name} lacks raw_files pins"]
    expected_raw_files = set(contract["raw_files"])
    if set(raw_files) != expected_raw_files:
        errors.append(f"source family {name} raw file denominator drifted")
    for raw_file in sorted(expected_raw_files):
        source = _path(f"{GT6_RAW_ROOT}/{raw_file}")
        if not source.is_file():
            errors.append(f"source family {name} raw asset is missing: {raw_file}")
            continue
        inputs.add(source)
        if raw_files.get(raw_file) != _sha256(source):
            errors.append(f"source family {name} raw hash is not current: {raw_file}")
    return errors


def _binding_errors(
    member: dict[str, Any],
    source_hashes: dict[str, Any],
    inputs: set[Path],
) -> tuple[list[str], list[dict[str, str]]]:
    identity = str(member["id"])
    binding = member.get("cc_binding")
    if not isinstance(binding, dict):
        return [f"{identity} lacks a CC binding"], []
    errors: list[str] = []
    model_texts: list[str] = []
    for value in binding.get("models") or []:
        model_path = _path(str(value))
        if not model_path.is_file():
            errors.append(f"{identity} model is missing: {value}")
            continue
        inputs.add(model_path)
        model_texts.append(model_path.read_text(encoding="utf-8"))
    if not model_texts:
        errors.append(f"{identity} has no available CC model binding")

    for value in binding.get("required_textures") or []:
        texture_path = _path(str(value))
        if not texture_path.is_file():
            errors.append(f"{identity} texture is missing: {value}")
        else:
            inputs.add(texture_path)
            texture_id = _texture_id(str(value))
            if texture_id and not any(
                texture_id in model_text for model_text in model_texts
            ):
                errors.append(f"{identity} does not bind required texture {texture_id}")

    imports: list[dict[str, str]] = []
    for item in _imports_for_member(member):
        source = item["source"]
        destination = item["destination"]
        if not source or not destination:
            errors.append(f"{identity} import lacks a source or destination")
            continue
        texture_id = _texture_id(destination)
        if source.endswith(".png") and texture_id is None:
            errors.append(f"{identity} import escapes CC texture roots: {destination}")
        destination_path = _path(destination)
        if not destination_path.is_file():
            errors.append(f"{identity} imported destination is missing: {destination}")
        else:
            inputs.add(destination_path)
            if source_hashes.get(source) != _sha256(destination_path):
                errors.append(
                    f"{identity} imported destination does not match pinned GT6 source: {destination}"
                )
        if source.endswith(".png") and texture_id and not any(
            texture_id in model_text for model_text in model_texts
        ):
            errors.append(f"{identity} does not bind imported texture {texture_id}")
        imports.append({"source": source, "destination": destination})
    return errors, imports


def _not_registered_resource_paths(identity: str) -> list[Path]:
    namespace, separator, path = identity.partition(":")
    if namespace != "cruciblecraft" or not separator or not path:
        return []
    candidates: list[Path] = []
    for root in (
        "src/main/resources/assets/cruciblecraft",
        "src/generated/resources/assets/cruciblecraft",
    ):
        candidates.extend(
            (
                _path(f"{root}/blockstates/{path}.json"),
                _path(f"{root}/models/block/{path}.json"),
                _path(f"{root}/models/item/{path}.json"),
                _path(f"{root}/textures/block/{path}"),
                _path(f"{root}/textures/block/{path}.png"),
                _path(f"{root}/textures/item/{path}"),
                _path(f"{root}/textures/item/{path}.png"),
            )
        )
    for root in (
        "src/main/resources/data/cruciblecraft",
        "src/generated/resources/data/cruciblecraft",
    ):
        candidates.extend(
            (
                _path(f"{root}/recipe/{path}.json"),
                _path(f"{root}/recipe/machines/{path}.json"),
                _path(f"{root}/loot_table/blocks/{path}.json"),
            )
        )
    return [candidate for candidate in candidates if candidate.exists()]


def validate(policy: dict[str, Any]) -> tuple[list[str], set[Path], list[dict[str, Any]]]:
    """Return validation errors, hashed inputs, and normalized resource records."""
    errors: list[str] = []
    inputs: set[Path] = {POLICY}
    if policy.get("schema_version") != 2:
        return ["T34 presentation policy schema_version must be 2"], inputs, []
    authority = policy.get("authority")
    if not isinstance(authority, dict) or authority.get("raw_texture_root") != GT6_RAW_ROOT:
        errors.append("T34 must use the authoritative GT6 raw texture root")
    forbidden_authorities = set((authority or {}).get("forbidden_authorities") or [])
    if "tmp-baked" not in forbidden_authorities:
        errors.append("T34 must explicitly forbid tmp-baked authority")

    families = policy.get("source_families")
    if not isinstance(families, dict) or set(families) != set(FAMILY_CONTRACTS):
        errors.append("T34 GT6 source-family denominator drifted")
    else:
        for name in sorted(FAMILY_CONTRACTS):
            errors.extend(_family_errors(name, families[name], inputs))

    geometry = policy.get("steam_geometry_evidence")
    if not isinstance(geometry, dict) or geometry.get("source") != STEAM_GEOMETRY:
        errors.append("T34 must record the MultiTileEntityEngineSteam geometry source")
    else:
        geometry_path = _path(STEAM_GEOMETRY)
        if not geometry_path.is_file():
            errors.append(f"T34 steam geometry source is missing: {STEAM_GEOMETRY}")
        else:
            inputs.add(geometry_path)
            if geometry.get("sha256") != _sha256(geometry_path):
                errors.append("T34 steam geometry source hash is not current")

    members = policy.get("resources")
    if not isinstance(members, list):
        return [*errors, "T34 resources must be an array"], inputs, []
    identities = [str(member.get("id", "")) for member in members if isinstance(member, dict)]
    if len(identities) != len(members) or len(set(identities)) != len(identities):
        errors.append("T34 resource identities must be unique objects")
    if set(identities) != REQUIRED_MEMBER_IDS:
        errors.append("T34 full resource denominator drifted")

    normalized: list[dict[str, Any]] = []
    for member in members:
        if not isinstance(member, dict):
            errors.append("T34 resources must contain only objects")
            continue
        identity = str(member.get("id", ""))
        disposition = str(member.get("disposition", ""))
        family = member.get("source_family")
        family_name = family if isinstance(family, str) else None
        expected_family = IDENTITY_FAMILIES.get(identity)
        if expected_family and family != expected_family:
            errors.append(
                f"{identity} must bind GT6 family {expected_family}, not {family}"
            )
        if family is not None and family_name not in FAMILY_CONTRACTS:
            errors.append(f"{identity} names an unknown GT6 family: {family}")
        if disposition not in {
            "registered",
            "runtime_only",
            "source_reference",
            "not_registered",
        }:
            errors.append(f"{identity} has an invalid disposition: {disposition}")
        if disposition == "not_registered":
            if not family:
                errors.append(f"{identity} not_registered row must name a source family")
            if any(
                key in member
                for key in (
                    "cc_binding",
                    "imports",
                    "destination",
                    "destinations",
                    "import_destination_root",
                )
            ):
                errors.append(
                    f"{identity} not_registered row cannot bind a CC model or resource"
                )
            for path in _not_registered_resource_paths(identity):
                errors.append(
                    f"{identity} not_registered row has a CC resource: {_relative(path)}"
                )
        elif disposition == "registered":
            family_hashes = (
                (families.get(family_name) or {}).get("raw_files") or {}
                if isinstance(families, dict) and family_name in FAMILY_CONTRACTS
                else {}
            )
            binding_errors, imports = _binding_errors(member, family_hashes, inputs)
            errors.extend(binding_errors)
            if _imports_for_member(member):
                expected_imports = set(
                    FAMILY_CONTRACTS[str(family)]["raw_files"]
                    if family in FAMILY_CONTRACTS
                    else ()
                )
                actual_imports = {
                    item["source"] for item in _imports_for_member(member)
                }
                if actual_imports != expected_imports:
                    errors.append(
                        f"{identity} import denominator does not cover {family} exactly"
                    )
            normalized.append(
                {
                    "id": identity,
                    "disposition": disposition,
                    "relationship": member.get("relationship"),
                    "source_family": family,
                    "imports": imports,
                }
            )
            continue
        elif disposition in {"runtime_only", "source_reference"}:
            if "cc_binding" in member or "imports" in member:
                errors.append(f"{identity} {disposition} row cannot bind CC art")
        normalized.append(
            {
                "id": identity,
                "disposition": disposition,
                "relationship": member.get("relationship"),
                "source_family": family,
                "imports": [],
            }
        )
    return errors, inputs, normalized


def build() -> dict[str, Any]:
    policy = _load_json(POLICY)
    errors, inputs, resources = validate(policy)
    source_families = policy.get("source_families") or {}
    receipt_families: dict[str, Any] = {}
    for name in sorted(FAMILY_CONTRACTS):
        family = source_families.get(name) or {}
        receipt_families[name] = {
            "source_evidence": family.get("source_evidence"),
            "raw_files": [
                {
                    "source": f"{GT6_RAW_ROOT}/{raw_file}",
                    "sha256": pinned_hash,
                }
                for raw_file, pinned_hash in sorted(
                    (family.get("raw_files") or {}).items()
                )
            ],
        }
    for resource in resources:
        family_name = resource.get("source_family")
        family = source_families.get(family_name) or {}
        evidence = family.get("source_evidence") or {}
        source_hashes = family.get("raw_files") or {}
        resource["imports"] = [
            {
                "destination": imported["destination"],
                "source": f"{GT6_RAW_ROOT}/{imported['source']}",
                "source_class": evidence.get("class_name"),
                "source_java": evidence.get("java_path"),
                "source_java_sha256": evidence.get("sha256"),
                "source_sha256": source_hashes.get(imported["source"]),
            }
            for imported in resource["imports"]
        ]
    return {
        "schema_version": 2,
        "status": (
            "T34_GT6_PRESENTATION_READY"
            if not errors
            else "T34_GT6_PRESENTATION_INCOMPLETE"
        ),
        "authority": policy.get("authority"),
        "steam_geometry_evidence": policy.get("steam_geometry_evidence"),
        "source_families": receipt_families,
        "resources": resources,
        "validation_errors": sorted(errors),
        "input_hashes": {
            _relative(path): _sha256(path)
            for path in sorted(inputs)
            if path.is_file()
        },
    }


def pin_source_hashes(policy: dict[str, Any]) -> dict[str, Any]:
    """Return a copy with every GT6 source hash explicitly pinned."""
    pinned = copy.deepcopy(policy)
    families = pinned.setdefault("source_families", {})
    for name, contract in FAMILY_CONTRACTS.items():
        family = families.setdefault(name, {})
        evidence = family.setdefault("source_evidence", {})
        java_path = _path(str(contract["java_path"]))
        evidence.update(
            {
                "java_path": contract["java_path"],
                "class_name": contract["class_name"],
                "sha256": _sha256(java_path),
            }
        )
        family["raw_files"] = {
            raw_file: _sha256(_path(f"{GT6_RAW_ROOT}/{raw_file}"))
            for raw_file in contract["raw_files"]
        }
    geometry = pinned.setdefault("steam_geometry_evidence", {})
    geometry["source"] = STEAM_GEOMETRY
    geometry["sha256"] = _sha256(_path(STEAM_GEOMETRY))
    return pinned


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    return (
        []
        if OUTPUT.read_text(encoding="utf-8") == common.stable_json(build())
        else [f"{common.relative(OUTPUT)} is stale"]
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--pin-sources", action="store_true")
    args = parser.parse_args(argv)
    if sum((args.write, args.check, args.pin_sources)) != 1:
        parser.error("choose exactly one of --write, --check, or --pin-sources")
    try:
        if args.pin_sources:
            common.write_stable(POLICY, pin_source_hashes(_load_json(POLICY)))
            print(f"pinned GT6 source hashes in {common.relative(POLICY)}")
            return 0
        if args.write:
            document = write()
            print(f"wrote {common.relative(OUTPUT)} status={document['status']}")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, KeyError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
