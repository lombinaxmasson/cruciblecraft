#!/usr/bin/env python3
"""Build the strict T34 GT6 runtime-to-art receipt."""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402

POLICY = ROOT / "tools" / "t34_gt6_art_policy.json"
OUTPUT = ROOT / "tools" / "t34_gt6_art_manifest.json"

EXPECTED_TARGETS = (
    "cruciblecraft:firebrick",
    "cruciblecraft:raw_ceramic_crucible",
    "cruciblecraft:crucible",
    "cruciblecraft:anvil",
    "cruciblecraft:raw_ceramic_mold",
    "cruciblecraft:raw_ingot_mold",
    "cruciblecraft:raw_plate_mold",
    "cruciblecraft:raw_rod_mold",
    "cruciblecraft:raw_bolt_mold",
    "cruciblecraft:ingot_mold",
    "cruciblecraft:plate_mold",
    "cruciblecraft:rod_mold",
    "cruciblecraft:bolt_mold",
    "cruciblecraft:firebox",
    "cruciblecraft:fuel_engine",
    "cruciblecraft:burning_gas_generator",
    "cruciblecraft:bronze_boiler",
    "cruciblecraft:bronze_steam_engine",
    "cruciblecraft:bronze_dynamo",
)
VALID_TINT_IDENTITIES = {"ceramic", "stone", "brick", "bronze", "tin_alloy"}


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _load_json(path: Path) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(document, dict):
        raise ValueError(f"{path.as_posix()} must be a JSON object")
    return document


def _relative(root: Path, path: Path) -> str:
    try:
        return path.relative_to(root).as_posix()
    except ValueError:
        return path.as_posix()


def _path(root: Path, value: str) -> Path:
    candidate = Path(value)
    if candidate.is_absolute():
        raise ValueError(f"path must be repository-relative: {value}")
    return root / candidate


def _resource_reference(path: Path, root: Path) -> str:
    relative = _relative(root, path)
    marker = "assets/"
    if marker not in relative:
        raise ValueError(f"not an asset path: {relative}")
    asset_relative = relative.split(marker, 1)[1]
    namespace, separator, rest = asset_relative.partition("/")
    if not separator or not rest.startswith("textures/") or path.suffix != ".png":
        raise ValueError(f"not a texture path: {relative}")
    return f"{namespace}:{rest.removeprefix('textures/')[:-4]}"


def _texture_path(root: Path, reference: str) -> Path:
    namespace, separator, value = reference.partition(":")
    if not separator or not namespace or not value:
        raise ValueError(f"texture reference is not namespaced: {reference}")
    paths = (
        root / "src/main/resources/assets" / namespace / "textures" / f"{value}.png",
        root / "src/generated/resources/assets" / namespace / "textures" / f"{value}.png",
    )
    for path in paths:
        if path.is_file():
            return path
    raise FileNotFoundError(f"missing texture resource: {reference}")


def _model_path(root: Path, reference: str) -> Path | None:
    namespace, separator, value = reference.partition(":")
    if not separator:
        namespace = "minecraft" if reference.startswith(("block/", "item/")) else "cruciblecraft"
        value = reference
    if not namespace or not value:
        raise ValueError(f"invalid model reference: {reference}")
    paths = (
        root / "src/main/resources/assets" / namespace / "models" / f"{value}.json",
        root / "src/generated/resources/assets" / namespace / "models" / f"{value}.json",
    )
    for path in paths:
        if path.is_file():
            return path
    if namespace == "minecraft":
        return None
    raise FileNotFoundError(f"missing model resource: {reference}")


def _model_texture_references(path: Path, root: Path) -> set[str]:
    chain: list[dict[str, Any]] = []
    seen: set[Path] = set()
    current: Path | None = path
    while current is not None:
        if current in seen:
            raise ValueError(f"cyclic model parent: {_relative(root, current)}")
        seen.add(current)
        document = _load_json(current)
        chain.append(document)
        parent = document.get("parent")
        if parent is None:
            current = None
        elif not isinstance(parent, str):
            raise ValueError(f"model parent must be a string: {_relative(root, current)}")
        else:
            current = _model_path(root, parent)

    textures: dict[str, str] = {}
    for document in reversed(chain):
        declared = document.get("textures") or {}
        if not isinstance(declared, dict):
            raise ValueError(f"model textures must be an object: {_relative(root, path)}")
        for name, value in declared.items():
            if not isinstance(name, str) or not isinstance(value, str):
                raise ValueError(f"model texture declaration is invalid: {_relative(root, path)}")
            textures[name] = value

    resolved: set[str] = set()
    for value in textures.values():
        traversed: set[str] = set()
        while value.startswith("#"):
            alias = value[1:]
            if alias in traversed or alias not in textures:
                raise ValueError(f"unresolvable model texture alias in {_relative(root, path)}")
            traversed.add(alias)
            value = textures[alias]
        if ":" not in value:
            raise ValueError(f"model texture is not namespaced: {value}")
        resolved.add(value)
    return resolved


def _normalize_gt6_asset_path(value: str) -> str:
    prefix = "assets/gregtech/"
    return value.removeprefix(prefix)


def _manifest_cc_path(root: Path, value: str) -> Path:
    if value.startswith("src/"):
        return _path(root, value)
    if value.startswith("assets/cruciblecraft/"):
        return root / "src/main/resources" / value
    if value.startswith("textures/"):
        return (
            root
            / "src/main/resources/assets/cruciblecraft"
            / value
        )
    raise ValueError(f"unsupported CrucibleCraft destination path: {value}")


def _declared_targets_for_asset(
    raw_record: dict[str, Any], source_path: str, policy: dict[str, Any]
) -> list[str]:
    declared = raw_record.get("targets", raw_record.get("target"))
    if isinstance(declared, str):
        return [declared]
    if isinstance(declared, list) and all(
        isinstance(target, str) for target in declared
    ):
        return declared
    return [
        str(target["id"])
        for target in policy.get("targets") or []
        if isinstance(target, dict)
        and isinstance(target.get("id"), str)
        and isinstance(target.get("source"), dict)
        and source_path.startswith(
            str(target["source"].get("source_path_prefix", ""))
        )
    ]


def _manifest_records(
    policy: dict[str, Any], root: Path, errors: list[str], inputs: set[Path]
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    records: list[dict[str, Any]] = []
    summaries: list[dict[str, Any]] = []
    for declaration in policy.get("source_manifests") or []:
        if not isinstance(declaration, dict):
            errors.append("source manifest declaration must be an object")
            continue
        value = declaration.get("path")
        if not isinstance(value, str):
            errors.append("source manifest declaration is missing path")
            continue
        path = _path(root, value)
        required = declaration.get("required") is True
        summary = {
            "path": value,
            "required": required,
            "role": declaration.get("role"),
            "present": path.is_file(),
        }
        summaries.append(summary)
        if not path.is_file():
            if required:
                errors.append(f"missing declared source manifest: {value}")
            continue
        inputs.add(path)
        try:
            document = _load_json(path)
        except (OSError, ValueError, json.JSONDecodeError) as exc:
            errors.append(str(exc))
            continue
        raw_records = document.get("assets", document.get("imports"))
        if not isinstance(raw_records, list):
            errors.append(f"{value} must declare an assets or imports array")
            continue
        for index, raw_record in enumerate(raw_records):
            label = f"{value} records[{index}]"
            if not isinstance(raw_record, dict):
                errors.append(f"{label} must be an object")
                continue
            source_value = raw_record.get(
                "source_path",
                raw_record.get("source_relative_path", raw_record.get("gt6_source")),
            )
            if not isinstance(source_value, str):
                errors.append(f"{label} must declare a GT6 source path")
                continue
            source_path = _normalize_gt6_asset_path(source_value)
            if source_path.endswith(".mcmeta"):
                continue
            targets = _declared_targets_for_asset(raw_record, source_path, policy)
            if not targets:
                errors.append(
                    f"{label} has no policy-declared target mapping: {source_path}"
                )
                continue
            cc_path_value = raw_record.get(
                "cc_path",
                raw_record.get("destination_path", raw_record.get("destination")),
            )
            cc_texture = raw_record.get("cc_texture")
            if not isinstance(cc_path_value, str) and not isinstance(cc_texture, str):
                errors.append(f"{label} must declare cc_path or cc_texture")
                continue
            try:
                if isinstance(cc_path_value, str):
                    cc_path = _manifest_cc_path(root, cc_path_value)
                    canonical_texture = _resource_reference(cc_path, root)
                else:
                    canonical_texture = str(cc_texture)
                    cc_path = _texture_path(root, canonical_texture)
            except (OSError, ValueError) as exc:
                errors.append(f"{label}: {exc}")
                continue
            if isinstance(cc_texture, str) and cc_texture != canonical_texture:
                errors.append(
                    f"{label} cc_texture does not match cc_path: {cc_texture}"
                )
                continue
            declared_hash = raw_record.get(
                "sha256", raw_record.get("source_sha256")
            )
            if not isinstance(declared_hash, str) or len(declared_hash) != 64:
                errors.append(f"{label} must declare a SHA-256")
                continue
            records.append(
                {
                    "manifest": value,
                    "targets": targets,
                    "source_path": source_path,
                    "cc_path": cc_path,
                    "cc_texture": canonical_texture,
                    "sha256": declared_hash.lower(),
                    "cc_mcmeta_path": raw_record.get("cc_mcmeta_path"),
                }
            )
    return records, summaries


def _validate_policy(
    policy: dict[str, Any], root: Path, errors: list[str], inputs: set[Path]
) -> list[dict[str, Any]]:
    if policy.get("schema_version") != 1:
        errors.append("T34 GT6 art policy schema_version must be 1")
    if policy.get("gt6_asset_root") != (
        "gt6_code/gregtech6/src/main/resources/assets/gregtech"
    ):
        errors.append("GT6 asset root drifted")
    manifests = policy.get("source_manifests")
    if not isinstance(manifests, list) or not manifests:
        errors.append("policy must declare source manifests")
    targets = policy.get("targets")
    if not isinstance(targets, list):
        errors.append("policy must declare targets")
        return []
    denominator = policy.get("denominator_targets")
    ids = [target.get("id") for target in targets if isinstance(target, dict)]
    if tuple(denominator or []) != EXPECTED_TARGETS:
        errors.append("policy target denominator drifted")
    if tuple(ids) != EXPECTED_TARGETS:
        errors.append("policy targets must exactly match the reopened denominator")
    runtime_ids = [
        target.get("runtime_id") for target in targets if isinstance(target, dict)
    ]
    if len(runtime_ids) != len(set(runtime_ids)) or any(
        not isinstance(runtime_id, str) for runtime_id in runtime_ids
    ):
        errors.append("policy runtime IDs must be unique")

    excluded = {
        row.get("id"): row
        for row in policy.get("not_registered") or []
        if isinstance(row, dict)
    }
    for identity in ("gt6:burning_box_solid", "gt6:burning_box_liquid"):
        if excluded.get(identity, {}).get("cc_runtime_status") != "not_registered":
            errors.append(f"{identity} must be recorded as not_registered")
    if excluded.get("gt6:small_gas_generator", {}).get("cc_runtime_status") != (
        "not_registered"
    ):
        errors.append("standalone small gas generator must be not_registered")

    target_by_id: dict[str, dict[str, Any]] = {}
    for target in targets:
        if not isinstance(target, dict):
            errors.append("target must be an object")
            continue
        identity = target.get("id")
        if not isinstance(identity, str):
            errors.append("target is missing id")
            continue
        target_by_id[identity] = target
        if target.get("tint_identity") not in VALID_TINT_IDENTITIES:
            errors.append(f"{identity} has an invalid tint identity")
        source = target.get("source")
        if not isinstance(source, dict):
            errors.append(f"{identity} is missing source evidence")
            continue
        prefix = source.get("source_path_prefix")
        if (
            not isinstance(prefix, str)
            or prefix.startswith("gt6_code/")
            or prefix.startswith("/")
            or not prefix.startswith("textures/")
        ):
            errors.append(f"{identity} source path must be relative to GT6 assets")
        for key in ("class_reference", "loader_reference", "loader_anchor"):
            if not isinstance(source.get(key), str | int):
                errors.append(f"{identity} source is missing {key}")
        for key in ("class_reference", "loader_reference"):
            value = source.get(key)
            if isinstance(value, str):
                path = _path(root, value)
                if not path.is_file():
                    errors.append(f"{identity} source reference is missing: {value}")
                else:
                    inputs.add(path)
                    text = path.read_text(encoding="utf-8")
                    if key == "class_reference" and path.stem not in text:
                        errors.append(f"{identity} class reference is not self-evidencing")
                    anchor = str(source.get("loader_anchor", ""))
                    if key == "loader_reference" and anchor not in text:
                        errors.append(f"{identity} loader anchor is missing: {anchor}")
        resources = target.get("resources")
        if not isinstance(resources, dict):
            errors.append(f"{identity} is missing resource contract")
            continue
        block_models = resources.get("block_models")
        if not isinstance(block_models, list) or not all(
            isinstance(model, str) for model in block_models
        ):
            errors.append(f"{identity} block_models must be an array")
        if not isinstance(resources.get("item_model"), str):
            errors.append(f"{identity} is missing item_model")
        state_contract = resources.get("state_contract")
        if not isinstance(state_contract, dict):
            errors.append(f"{identity} is missing state contract")
    _validate_identity_policy(target_by_id, errors)
    return targets


def _validate_identity_policy(
    targets: dict[str, dict[str, Any]], errors: list[str]
) -> None:
    firebox = targets.get("cruciblecraft:firebox", {}).get("source", {})
    if (
        firebox.get("gt6_runtime_id") != 1199
        or firebox.get("source_family") != "burning_brick"
    ):
        errors.append("firebox must remain GT6 1199 burning_brick")
    fuel = targets.get("cruciblecraft:fuel_engine", {}).get("source", {})
    if fuel.get("gt6_runtime_id") != 9147 or fuel.get("source_family") != "motor_liquid":
        errors.append("fuel_engine must remain GT6 9147 motor_liquid")
    gas = targets.get("cruciblecraft:burning_gas_generator", {}).get("source", {})
    if (
        gas.get("gt6_runtime_id") != 1602
        or gas.get("source_family") != "burning_gas"
        or gas.get("gt6_identity") != "Burning Box Gas"
        or gas.get("standalone_small_gas_generator") != "not_registered"
    ):
        errors.append("burning_gas_generator identity requirement drifted")
    boiler = targets.get("cruciblecraft:bronze_boiler", {}).get("source", {})
    if boiler.get("gt6_runtime_id") != 1202 or boiler.get("source_family") != "boiler_steam":
        errors.append("bronze_boiler must remain GT6 1202 boiler_steam")
    engine = targets.get("cruciblecraft:bronze_steam_engine", {}).get("source", {})
    if engine.get("gt6_runtime_id") != 1302 or engine.get("source_family") != "kinetic_steam":
        errors.append("bronze_steam_engine must remain GT6 1302 kinetic_steam")
    dynamo = targets.get("cruciblecraft:bronze_dynamo", {}).get("source", {})
    if (
        dynamo.get("gt6_runtime_id") != 10111
        or dynamo.get("source_family") != "electric_rotation"
        or dynamo.get("gt6_material") != "TinAlloy"
    ):
        errors.append("bronze_dynamo must remain GT6 10111 electric_rotation TinAlloy")
    for identity in (
        "cruciblecraft:ingot_mold",
        "cruciblecraft:plate_mold",
        "cruciblecraft:rod_mold",
        "cruciblecraft:bolt_mold",
    ):
        if targets.get(identity, {}).get("runtime_family") != "cruciblecraft:ceramic_mold":
            errors.append(f"{identity} must remain under ceramic_mold runtime family")


def _validate_records(
    records: list[dict[str, Any]],
    targets: list[dict[str, Any]],
    policy: dict[str, Any],
    root: Path,
    errors: list[str],
    inputs: set[Path],
) -> dict[str, list[dict[str, Any]]]:
    asset_root = _path(root, str(policy["gt6_asset_root"]))
    records_by_target: dict[str, list[dict[str, Any]]] = defaultdict(list)
    known_targets = {target["id"] for target in targets if isinstance(target.get("id"), str)}
    for record in records:
        source_path = record["source_path"]
        if source_path.startswith("/") or source_path.startswith("gt6_code/"):
            errors.append(f"GT6 source path must be asset-relative: {source_path}")
            continue
        source = asset_root / source_path
        cc_path = record["cc_path"]
        label = f"{record['manifest']}:{source_path}"
        if not source.is_file():
            errors.append(f"missing GT6 source texture: {source_path}")
            continue
        if not cc_path.is_file():
            errors.append(f"missing imported CC texture: {_relative(root, cc_path)}")
            continue
        inputs.update((source, cc_path))
        source_hash = _sha256(source)
        cc_hash = _sha256(cc_path)
        if source_hash != cc_hash:
            errors.append(f"texture SHA-256 mismatch: {source_path}")
            continue
        if record["sha256"] != source_hash:
            errors.append(f"manifest SHA-256 drifted: {label}")
            continue
        source_mcmeta = Path(f"{source}.mcmeta")
        if source_mcmeta.is_file():
            cc_mcmeta_value = record.get("cc_mcmeta_path")
            cc_mcmeta = (
                _path(root, cc_mcmeta_value)
                if isinstance(cc_mcmeta_value, str)
                else Path(f"{cc_path}.mcmeta")
            )
            if not cc_mcmeta.is_file():
                errors.append(
                    f"missing active-family .mcmeta: {_relative(root, cc_mcmeta)}"
                )
                continue
            inputs.update((source_mcmeta, cc_mcmeta))
            if source_mcmeta.read_bytes() != cc_mcmeta.read_bytes():
                errors.append(f".mcmeta mismatch: {source_path}")
                continue
        record["source_hash"] = source_hash
        for identity in record["targets"]:
            if identity not in known_targets:
                errors.append(f"art manifest declares unknown target: {identity}")
                continue
            records_by_target[identity].append(record)
    return records_by_target


def _validate_resources(
    target: dict[str, Any],
    target_records: list[dict[str, Any]],
    root: Path,
    errors: list[str],
    inputs: set[Path],
) -> None:
    identity = str(target["id"])
    source = target["source"]
    prefix = str(source["source_path_prefix"])
    resources = target["resources"]
    block_models = resources["block_models"]
    item_model = resources["item_model"]
    model_paths = [_path(root, value) for value in [*block_models, item_model]]
    texture_records = {
        record["cc_texture"]: record for record in target_records
    }

    if not target_records:
        errors.append(f"{identity} has no declared imported GT6 texture")
    for record in target_records:
        source_path = record["source_path"]
        if not source_path.startswith(prefix):
            errors.append(
                f"{identity} source family mismatch: {source_path} is outside {prefix}"
            )
        if any(
            forbidden in source_path
            for forbidden in target.get("forbidden_source_families") or []
        ):
            errors.append(f"{identity} binds forbidden source family: {source_path}")

    for path in model_paths:
        if not path.is_file():
            errors.append(f"missing model path: {_relative(root, path)}")
            continue
        inputs.add(path)
        try:
            references = _model_texture_references(path, root)
        except (OSError, ValueError, json.JSONDecodeError) as exc:
            errors.append(str(exc))
            continue
        for reference in references:
            if not reference.startswith("cruciblecraft:"):
                continue
            try:
                texture_path = _texture_path(root, reference)
                inputs.add(texture_path)
            except (OSError, ValueError) as exc:
                errors.append(str(exc))
                continue
            if reference not in texture_records:
                errors.append(
                    f"{identity} model texture has no declared GT6 provenance: {reference}"
                )

    blockstate_value = resources.get("blockstate")
    contract = resources["state_contract"]
    if contract.get("mode") == "item_only":
        if blockstate_value is not None or block_models:
            errors.append(f"{identity} item-only state contract drifted")
        return
    if not isinstance(blockstate_value, str):
        errors.append(f"{identity} must declare a blockstate")
        return
    blockstate = _path(root, blockstate_value)
    if not blockstate.is_file():
        errors.append(f"missing blockstate path: {_relative(root, blockstate)}")
        return
    inputs.add(blockstate)
    try:
        variants = _load_json(blockstate).get("variants")
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        errors.append(str(exc))
        return
    if not isinstance(variants, dict):
        errors.append(f"{_relative(root, blockstate)} must define variants")
        return
    required_keys = set(contract.get("required_variant_keys") or [])
    if not required_keys.issubset(variants):
        errors.append(f"{identity} blockstate is missing required variant keys")
    state_models: set[str] = set()
    for variant in variants.values():
        rows = variant if isinstance(variant, list) else [variant]
        for row in rows:
            if isinstance(row, dict) and isinstance(row.get("model"), str):
                state_models.add(row["model"])
    required_models = set(contract.get("required_models") or [])
    if not required_models.issubset(state_models):
        errors.append(f"{identity} blockstate model contract drifted")


def audit(
    policy: dict[str, Any], root: Path = ROOT
) -> tuple[list[str], dict[str, Any]]:
    errors: list[str] = []
    inputs: set[Path] = set()
    targets = _validate_policy(policy, root, errors, inputs)
    records, manifest_summaries = _manifest_records(policy, root, errors, inputs)
    records_by_target = _validate_records(
        records, targets, policy, root, errors, inputs
    )
    for target in targets:
        if isinstance(target, dict) and isinstance(target.get("id"), str):
            _validate_resources(
                target,
                records_by_target.get(target["id"], []),
                root,
                errors,
                inputs,
            )
    normalized_errors = sorted(set(errors))
    artifacts = [
        {
            "targets": record["targets"],
            "source_path": record["source_path"],
            "cc_path": _relative(root, record["cc_path"]),
            "cc_texture": record["cc_texture"],
            "sha256": record.get("source_hash", record["sha256"]),
        }
        for record in records
        if "source_hash" in record
    ]
    return normalized_errors, {
        "input_hashes": {
            _relative(root, path): _sha256(path) for path in sorted(inputs)
        },
        "source_manifests": manifest_summaries,
        "artifacts": sorted(
            artifacts,
            key=lambda artifact: (
                artifact["source_path"],
                artifact["cc_texture"],
                artifact["targets"],
            ),
        ),
    }


def validate(policy: dict[str, Any], root: Path = ROOT) -> dict[str, Any]:
    errors, evidence = audit(policy, root)
    if errors:
        raise ValueError("\n".join(errors))
    return evidence


def build(
    root: Path = ROOT, policy_path: Path | None = None
) -> dict[str, Any]:
    selected_policy = policy_path or root / "tools" / "t34_gt6_art_policy.json"
    if not selected_policy.is_file():
        raise FileNotFoundError(f"missing policy: {_relative(root, selected_policy)}")
    policy = _load_json(selected_policy)
    errors, evidence = audit(policy, root)
    evidence["input_hashes"][_relative(root, selected_policy)] = _sha256(selected_policy)
    target_rows = [
        {
            "id": target["id"],
            "runtime_id": target["runtime_id"],
            "runtime_kind": target["runtime_kind"],
            "runtime_family": target.get("runtime_family"),
            "tint_identity": target["tint_identity"],
            "source_family": target["source"]["source_family"],
            "gt6_runtime_id": target["source"].get("gt6_runtime_id"),
        }
        for target in policy.get("targets") or []
        if isinstance(target, dict)
    ]
    return {
        "schema_version": 1,
        "status": (
            "T34_GT6_ART_READY"
            if not errors
            else "T34_GT6_ART_NOT_READY"
        ),
        "policy": _relative(root, selected_policy),
        "denominator_targets": policy.get("denominator_targets"),
        "not_registered": policy.get("not_registered"),
        "targets": target_rows,
        "source_manifests": evidence["source_manifests"],
        "validated_artifacts": evidence["artifacts"],
        "validation_errors": errors,
        "input_hashes": dict(sorted(evidence["input_hashes"].items())),
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    document = build()
    errors = list(document["validation_errors"])
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(ROOT, OUTPUT)}")
    elif OUTPUT.read_text(encoding="utf-8") != common.stable_json(document):
        errors.append(f"{_relative(ROOT, OUTPUT)} is stale")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    try:
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
