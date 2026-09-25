#!/usr/bin/env python3
"""Check the player-facing contract of the GT6 fluid attachment family.

The runtime overlay proves that identities are live. This contract is the
second gate: every catalog row must also have a searchable creative entry,
localization, recipe, blockstate, BlockItem model, source-exact art, particle,
and the family GameTests. It is deliberately read-only.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src" / "main" / "resources"
ASSETS = RESOURCES / "assets" / "cruciblecraft"
CATALOG = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "mte_inplace_catalog.json"
CONTRACT = (
    ROOT
    / "tools"
    / "waves"
    / "content"
    / "gt6-mte-fluid-attachments-runtime"
    / "player_surface_contract.json"
)
WAVE = CONTRACT.parent
OVERLAY = WAVE / "runtime_overlay.json"
D0_MATRIX = (
    ROOT
    / "tools"
    / "waves"
    / "prep"
    / "gt6-mte-fluid-attachments-acquisition"
    / "d0_obtain_matrix.json"
)
ART_MANIFEST = WAVE / "art_manifest.json"
RECIPE_ROOT = ROOT / "src" / "generated" / "resources" / "data" / "cruciblecraft" / "recipe"
ZH_LANG = ROOT / "src" / "generated" / "resources" / "assets" / "cruciblecraft" / "lang" / "zh_cn.json"
EN_LANG = ROOT / "src" / "generated" / "resources" / "assets" / "cruciblecraft" / "lang" / "en_us.json"
CREATIVE_TABS = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "registry"
    / "ModCreativeTabs.java"
)
GAME_TESTS = (
    ROOT
    / "src"
    / "test"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "gametest"
    / "MteFluidAttachmentsRuntimeGameTests.java"
)
GT6_W = ROOT / "gt6_referencable_port_code" / "gregtech6_w"

FAMILY_KINDS = frozenset({"FAUCET", "TAP", "FUNNEL", "NOZZLE", "CAP_NOZZLE"})
RESOURCE_ID = re.compile(r"^(?P<namespace>[a-z0-9_.-]+):(?P<path>.+)$")


def _load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _relative(path: Path) -> str:
    try:
        return path.relative_to(ROOT).as_posix()
    except ValueError:
        return str(path)


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _has_cjk(value: Any) -> bool:
    return any("\u3400" <= char <= "\u9fff" for char in str(value or ""))


def _catalog_rows(document: dict[str, Any]) -> list[dict[str, Any]]:
    return [
        row
        for row in document.get("identities") or []
        if row.get("family") == "fluid_attachment"
        and row.get("kind") in FAMILY_KINDS
    ]


def _model_path(model_id: str) -> Path | None:
    match = RESOURCE_ID.fullmatch(model_id)
    if not match:
        return None
    namespace = match["namespace"]
    path = match["path"]
    if namespace != "cruciblecraft":
        return None
    return ASSETS.parent / namespace / "models" / f"{path}.json"


def _texture_path(texture_id: str) -> Path | None:
    match = RESOURCE_ID.fullmatch(texture_id)
    if not match or match["namespace"] != "cruciblecraft":
        return None
    return ASSETS / "textures" / f"{match['path']}.png"


def _expected_model_id(kind: str, contract: dict[str, Any]) -> str:
    return f"cruciblecraft:block/{contract['kinds'][kind]['model']}"


def _check_catalog(
    rows: list[dict[str, Any]], contract: dict[str, Any]
) -> list[str]:
    errors: list[str] = []
    expected_total = int(contract["expected_total"])
    if len(rows) != expected_total:
        errors.append(f"catalog has {len(rows)} fluid attachments, expected {expected_total}")
    counts = Counter(str(row.get("kind")) for row in rows)
    for kind, spec in contract["kinds"].items():
        if counts[kind] != int(spec["expected"]):
            errors.append(
                f"catalog kind {kind} has {counts[kind]} rows, expected {spec['expected']}"
            )
    metas: set[int] = set()
    paths: set[str] = set()
    for row in rows:
        meta = int(row.get("meta", -1))
        path = str(row.get("registry_path") or "")
        runtime_id = str(row.get("runtime_id") or "")
        if meta in metas:
            errors.append(f"duplicate catalog meta {meta}")
        metas.add(meta)
        if path in paths:
            errors.append(f"duplicate catalog registry_path {path}")
        paths.add(path)
        expected_id = f"cruciblecraft:{path}"
        if runtime_id != expected_id:
            errors.append(f"catalog {path} runtime_id {runtime_id} != {expected_id}")
        if not str(row.get("english_name") or "").strip():
            errors.append(f"catalog {path} has no English name")
        if not str(row.get("chinese_name") or "").strip():
            errors.append(f"catalog {path} has no Chinese display-name source")
    return errors


def _check_overlay(rows: list[dict[str, Any]]) -> list[str]:
    errors: list[str] = []
    if not OVERLAY.is_file():
        return [f"missing {_relative(OVERLAY)}"]
    overlay = _load_json(OVERLAY)
    overlay_rows = overlay.get("rows") or []
    by_meta = {int(row.get("meta", -1)): row for row in overlay_rows}
    for row in rows:
        meta = int(row["meta"])
        actual = by_meta.get(meta)
        if actual is None:
            errors.append(f"overlay missing catalog meta {meta}")
            continue
        expected_path = str(row["registry_path"])
        for key, expected in (
            ("dummy_path", expected_path),
            ("live_block", f"cruciblecraft:{expected_path}"),
            ("kind", row["kind"]),
            ("family", "fluid_attachment"),
        ):
            if actual.get(key) != expected:
                errors.append(
                    f"overlay meta {meta} {key}={actual.get(key)!r}, expected {expected!r}"
                )
    if len(overlay_rows) != len(rows):
        errors.append(
            f"overlay has {len(overlay_rows)} rows, expected {len(rows)}"
        )
    return errors


def _check_resource_files(
    rows: list[dict[str, Any]], contract: dict[str, Any]
) -> list[str]:
    errors: list[str] = []
    checked_models: set[str] = set()
    for row in rows:
        path = str(row["registry_path"])
        kind = str(row["kind"])
        spec = contract["kinds"][kind]
        blockstate_path = ASSETS / "blockstates" / f"{path}.json"
        item_model_path = ASSETS / "models" / "item" / f"{path}.json"
        if not blockstate_path.is_file():
            errors.append(f"{path} missing blockstate")
            continue
        if not item_model_path.is_file():
            errors.append(f"{path} missing BlockItem model")
        try:
            blockstate = _load_json(blockstate_path)
        except (OSError, ValueError) as exc:
            errors.append(f"{path} invalid blockstate: {exc}")
            continue
        model_ids = {
            str(value.get("model"))
            for value in (blockstate.get("variants") or {}).values()
            if isinstance(value, dict) and value.get("model")
        }
        expected_model = _expected_model_id(kind, contract)
        if model_ids != {expected_model}:
            errors.append(
                f"{path} blockstate models {sorted(model_ids)} != [{expected_model}]"
            )
        model_path = _model_path(expected_model)
        if model_path is None or not model_path.is_file():
            errors.append(f"{path} missing shared block model {expected_model}")
            continue
        model_key = _relative(model_path)
        if model_key in checked_models:
            continue
        checked_models.add(model_key)
        try:
            model = _load_json(model_path)
        except (OSError, ValueError) as exc:
            errors.append(f"{model_key} invalid model: {exc}")
            continue
        textures = model.get("textures") or {}
        if textures.get("particle") != spec["particle"]:
            errors.append(
                f"{model_key} particle {textures.get('particle')!r} "
                f"!= {spec['particle']!r}"
            )
        elements = model.get("elements") or []
        bounds = spec["bounds"]
        if len(elements) < len(bounds):
            errors.append(f"{model_key} has too few geometry elements")
        else:
            for index, expected in enumerate(bounds):
                actual = list(elements[index].get("from", [])) + list(
                    elements[index].get("to", [])
                )
                if actual != expected:
                    errors.append(
                        f"{model_key} element {index} bounds {actual} != {expected}"
                    )
        expected_elements = len(bounds) * (2 if spec["requires_overlay"] else 1)
        if len(elements) != expected_elements:
            errors.append(
                f"{model_key} has {len(elements)} elements, expected {expected_elements}"
            )
        required_texture_keys = (
            {"body"}
            if kind == "FAUCET"
            else {
                f"{layer}_{face}"
                for layer in ("colored", "overlay")
                for face in ("bottom", "top", "side")
            }
        )
        missing_keys = sorted(required_texture_keys - set(textures))
        if missing_keys:
            errors.append(f"{model_key} missing texture keys {missing_keys}")
        for key, texture_id in textures.items():
            if not isinstance(texture_id, str) or texture_id.startswith("#"):
                continue
            texture_path = _texture_path(texture_id)
            if texture_path is None:
                continue
            if not texture_path.is_file():
                errors.append(
                    f"{model_key} texture {key} points to missing {_relative(texture_path)}"
                )
        if item_model_path.is_file():
            item_model = _load_json(item_model_path)
            if item_model.get("parent") != expected_model:
                errors.append(
                    f"{path} item model parent {item_model.get('parent')!r} "
                    f"!= {expected_model!r}"
                )
    return errors


def _check_recipes(rows: list[dict[str, Any]]) -> list[str]:
    errors: list[str] = []
    d0 = _load_json(D0_MATRIX)
    source_exact = {
        str(row.get("runtime_id") or "")
        for row in d0.get("hosts") or []
        if row.get("status") == "source_exact"
    }
    for row in rows:
        path = str(row["registry_path"])
        if f"cruciblecraft:{path}" not in source_exact:
            continue
        recipe_path = RECIPE_ROOT / f"{path}.json"
        if not recipe_path.is_file():
            errors.append(f"{path} missing generated recipe")
            continue
        try:
            recipe = _load_json(recipe_path)
        except (OSError, ValueError) as exc:
            errors.append(f"{_relative(recipe_path)} invalid JSON: {exc}")
            continue
        result = recipe.get("result")
        result_id = result.get("id") if isinstance(result, dict) else None
        expected_id = f"cruciblecraft:{path}"
        if result_id != expected_id:
            errors.append(
                f"{_relative(recipe_path)} result {result_id!r} != {expected_id!r}"
            )
    return errors


def _check_languages(rows: list[dict[str, Any]]) -> list[str]:
    errors: list[str] = []
    for path in (ZH_LANG, EN_LANG):
        if not path.is_file():
            errors.append(f"missing {_relative(path)}")
    if errors:
        return errors
    zh = _load_json(ZH_LANG)
    en = _load_json(EN_LANG)
    for row in rows:
        registry_path = str(row["registry_path"])
        translation_path = registry_path.replace("/", ".")
        for kind in ("block", "item"):
            key = f"{kind}.cruciblecraft.{translation_path}"
            if key not in en or not str(en[key]).strip():
                errors.append(f"en_us missing {key}")
            if key not in zh or not _has_cjk(zh[key]):
                errors.append(f"zh_cn missing Chinese value for {key}")
    return errors


def _check_art_manifest(
    contract: dict[str, Any], model_texture_ids: Iterable[str]
) -> list[str]:
    errors: list[str] = []
    if not ART_MANIFEST.is_file():
        return [f"missing {_relative(ART_MANIFEST)}"]
    manifest = _load_json(ART_MANIFEST)
    rows = manifest.get("rows") or []
    destinations: set[str] = set()
    kind_counts = Counter(str(row.get("kind")) for row in rows)
    for row in rows:
        source_root = str(row.get("source") or "")
        gt6_source = str(row.get("gt6_source") or "")
        destination = str(row.get("destination") or "")
        if source_root != "gt6_referencable_port_code/gregtech6_w":
            errors.append(f"art manifest has non-GT6 source root {source_root!r}")
        if destination in destinations:
            errors.append(f"art manifest duplicate destination {destination}")
        destinations.add(destination)
        source_path = GT6_W / gt6_source
        dest_path = RESOURCES / destination
        if not source_path.is_file():
            errors.append(f"art source missing {_relative(source_path)}")
            continue
        if not dest_path.is_file():
            errors.append(f"art destination missing {_relative(dest_path)}")
            continue
        declared_hash = str(row.get("sha256") or "")
        source_hash = _sha256(source_path)
        if declared_hash != source_hash:
            errors.append(
                f"art manifest hash {_relative(source_path)} {declared_hash} != {source_hash}"
            )
        if source_path.read_bytes() != dest_path.read_bytes():
            errors.append(f"art copy differs {_relative(source_path)} -> {_relative(dest_path)}")
    for kind in contract["kinds"]:
        if kind_counts[kind] == 0:
            errors.append(f"art manifest has no {kind} source")
    raw_kind = str(contract["raw_ceramic_art"]["kind"])
    if kind_counts[raw_kind] != int(contract["raw_ceramic_art"]["expected"]):
        errors.append(
            f"art manifest has {kind_counts[raw_kind]} {raw_kind} rows, "
            f"expected {contract['raw_ceramic_art']['expected']}"
        )
    manifest_destinations = {
        str(row.get("destination"))
        for row in rows
        if row.get("kind") in contract["kinds"]
    }
    for texture_id in model_texture_ids:
        texture_path = _texture_path(texture_id)
        if texture_path is None:
            continue
        destination = texture_path.relative_to(RESOURCES).as_posix()
        if destination not in manifest_destinations:
            errors.append(f"model texture {texture_id} is absent from art manifest")
    return errors


def _check_creative_route() -> list[str]:
    if not CREATIVE_TABS.is_file():
        return [f"missing {_relative(CREATIVE_TABS)}"]
    source = CREATIVE_TABS.read_text(encoding="utf-8")
    required_fragments = (
        "MteInPlaceCatalog.specs().forEach",
        "spec.kind().attachment()",
        "ModItems.mteInPlaceItemsById()",
    )
    return [
        f"{_relative(CREATIVE_TABS)} missing creative route fragment {fragment!r}"
        for fragment in required_fragments
        if fragment not in source
    ]


def _check_game_tests(contract: dict[str, Any]) -> list[str]:
    if not GAME_TESTS.is_file():
        return [f"missing {_relative(GAME_TESTS)}"]
    source = GAME_TESTS.read_text(encoding="utf-8")
    errors: list[str] = []
    for name in contract["required_game_tests"]:
        if not re.search(rf"\b{name}\s*\(", source):
            errors.append(f"missing GameTest {name}")
    return errors


def check() -> list[str]:
    """Return all player-surface contract violations."""
    errors: list[str] = []
    if not CONTRACT.is_file():
        return [f"missing {_relative(CONTRACT)}"]
    contract = _load_json(CONTRACT)
    if not CATALOG.is_file():
        return [f"missing {_relative(CATALOG)}"]
    rows = _catalog_rows(_load_json(CATALOG))
    errors.extend(_check_catalog(rows, contract))
    errors.extend(_check_overlay(rows))
    errors.extend(_check_resource_files(rows, contract))
    errors.extend(_check_recipes(rows))
    errors.extend(_check_languages(rows))
    texture_ids: set[str] = set()
    for kind, spec in contract["kinds"].items():
        model_path = ASSETS / "models" / "block" / f"{spec['model']}.json"
        if not model_path.is_file():
            continue
        model = _load_json(model_path)
        texture_ids.update(
            str(value)
            for value in (model.get("textures") or {}).values()
            if isinstance(value, str) and not value.startswith("#")
        )
    errors.extend(_check_art_manifest(contract, texture_ids))
    errors.extend(_check_creative_route())
    errors.extend(_check_game_tests(contract))
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="check the contract")
    parser.add_argument(
        "--json",
        action="store_true",
        dest="as_json",
        help="print a machine-readable error list",
    )
    args = parser.parse_args(argv)
    if not args.check:
        parser.error("choose --check")
    errors = check()
    if args.as_json:
        print(json.dumps({"ok": not errors, "errors": errors}, ensure_ascii=False, indent=2))
    elif errors:
        for error in errors:
            print(f"ERROR: {error}", file=sys.stderr)
    else:
        print("gt6_fluid_attachment_contract: OK (47 player-facing identities)")
    return 1 if errors else 0


if __name__ == "__main__":
    raise SystemExit(main())
