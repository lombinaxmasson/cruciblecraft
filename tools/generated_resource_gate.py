#!/usr/bin/env python3
"""Live catalog generated-resource and art gate."""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import atomic_io
from tools import io_common as io
from tools import language_names as names
from tools import registry_identity
from tools import tree_compare

MANIFEST = io.TOOLS / "generated_resource_gate_manifest.json"
GENERATED = ROOT / "src/generated/resources"
MAIN = ROOT / "src/main/resources"
BLOCK_REGISTER_RE = registry_identity.BLOCK_REGISTER_RE
FLUID_BLOCKS = {"creosote", "steam"}
NO_LOOT_BLOCKS = {
    "ceramic_mold",
    "gas_cloud",
    "gt_bedrock_ore",
    "gt_broken_ore",
    "gt_fluid_spring",
    "gt_hosted_ore",
    "gt_indicator_flower",
    "gt_indicator_grass",
    "gt_small_bedrock_ore",
    "gt_small_ore",
    "gt_surface_rock",
    "subsurface_fluid_deposit",
}
NO_ITEM_BLOCKS = {
    "gas_cloud",
    "subsurface_fluid_deposit",
}
HAND_BREAKABLE_PREFIXES = (
    "glass/",
    "glow_glass/",
)
def python_owned_generated_prefixes() -> tuple[str, ...]:
    prefixes = {
        "assets/cruciblecraft/models/gt_block/",
        "assets/cruciblecraft/models/gt_stone/",
        "assets/cruciblecraft/models/item/gt_block/",
        "assets/cruciblecraft/models/item/gt_stone/",
    }
    from tools import catalog_modern_ids as modern

    if modern.MAP_PATH.is_file():
        for row in modern.load_map().get("rows") or []:
            old = str(row.get("old_registry_path") or "")
            path = str(row.get("registry_path") or "")
            if not path or not old.startswith(("gt_block/", "gt_stone/")):
                continue
            head = path.split("/", 1)[0]
            prefixes.add(f"assets/cruciblecraft/models/{head}/")
            prefixes.add(f"assets/cruciblecraft/models/item/{head}/")
    return tuple(sorted(prefixes))


PYTHON_OWNED_GENERATED_PREFIXES = python_owned_generated_prefixes()
ART_MANIFESTS = (
    ROOT / "tools/block_art_manifest.json",
    ROOT / "tools/multiitem_art_manifest.json",
    ROOT / "tools/gt6_art_gt6_art_manifest.json",
    *(
        ROOT / "src/main/resources/assets/cruciblecraft"
    ).glob("*art_manifest.json"),
)
GT6_FAMILY_RE = re.compile(
    r"^cruciblecraft:(?:block|item)/(?:gt6(?:_import|_energy_art)?/)"
)


def dumps(document: Any) -> bytes:
    return io.stable_json(document).encode("utf-8")


def load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def live_block_paths() -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    seen: set[str] = set()

    def add(registry_path: str, source: str) -> None:
        if not registry_path or registry_path in seen or registry_path in FLUID_BLOCKS:
            return
        seen.add(registry_path)
        rows.append(
            {
                "registry_path": registry_path,
                "runtime_id": f"cruciblecraft:{registry_path}",
                "source": source,
            }
        )

    for path, _prefix in (
        (registry_identity.GT_BLOCK, "gt_block"),
        (registry_identity.BATH_REMAINDER, "building_block"),
        (registry_identity.GT_BUILDING, "building_block"),
    ):
        document = io.load_json(path)
        rel = io.relative(path)
        for identity in document.get("identities") or []:
            add(str(identity.get("registry_path") or ""), rel)

    stone = io.load_json(registry_identity.GT_STONE)
    stone_rel = io.relative(registry_identity.GT_STONE)
    for identity in stone.get("identities") or []:
        for variant in identity.get("variants") or []:
            add(str(variant.get("registry_path") or ""), stone_rel)

    for path, key, id_key in (
        (registry_identity.MACHINE_TIERS, "variants", "id"),
        (registry_identity.TRANSFORMER_TIERS, "tiers", "id"),
        (registry_identity.BATTERY_TIERS, "tiers", "id"),
        (registry_identity.CONVERTER_TIERS, "tiers", "id"),
        (registry_identity.STORAGE_VARIANTS, "variants", "runtime_id"),
    ):
        document = io.load_json(path)
        rel = io.relative(path)
        for row in document.get(key) or []:
            runtime = str(row.get(id_key) or "")
            add(runtime.split(":", 1)[-1], rel)

    blocks_java = registry_identity.MOD_BLOCKS
    if blocks_java.is_file():
        rel = io.relative(blocks_java)
        for match in BLOCK_REGISTER_RE.finditer(blocks_java.read_text(encoding="utf-8")):
            add(match.group(1), rel)
    return rows


def resolve_resource(*relative: str) -> Path | None:
    for root in (GENERATED, MAIN):
        path = root.joinpath(*relative)
        if path.is_file():
            return path
    return None


def model_rel(model_id: str) -> str | None:
    if model_id.startswith("cruciblecraft:"):
        return "assets/cruciblecraft/models/" + model_id.split(":", 1)[1] + ".json"
    if model_id.startswith("minecraft:"):
        return None
    return None


def variant_models(blockstate: dict[str, Any]) -> list[str]:
    models: list[str] = []
    variants = blockstate.get("variants") or {}
    if isinstance(variants, dict):
        for entry in variants.values():
            payloads = entry if isinstance(entry, list) else [entry]
            for payload in payloads:
                if isinstance(payload, dict) and payload.get("model"):
                    models.append(str(payload["model"]))
    multipart = blockstate.get("multipart") or []
    if isinstance(multipart, list):
        for part in multipart:
            apply = (part or {}).get("apply")
            payloads = apply if isinstance(apply, list) else [apply]
            for payload in payloads:
                if isinstance(payload, dict) and payload.get("model"):
                    models.append(str(payload["model"]))
    return models


def expand_brace_paths(pattern: str) -> list[str]:
    """Expand ``{a,b}_{c,d}`` compact destination rows used by GT6 art manifests."""
    match = re.search(r"\{([^{}]+)\}", pattern)
    if not match:
        return [pattern]
    prefix = pattern[: match.start()]
    suffix = pattern[match.end() :]
    expanded: list[str] = []
    for option in match.group(1).split(","):
        expanded.extend(expand_brace_paths(prefix + option + suffix))
    return expanded


def manifested_textures() -> tuple[set[str], tuple[str, ...]]:
    found: set[str] = set()
    for path in ART_MANIFESTS:
        if not path.is_file():
            continue
        document = load_json(path)
        for row in document.get("imports") or document.get("assets") or []:
            dest = str(
                row.get("destination")
                or row.get("destination_path")
                or ""
            )
            if dest:
                for item in expand_brace_paths(dest.replace("\\", "/")):
                    found.add(item)
        for row in document.get("identities") or []:
            for texture in [row.get("texture"), *(row.get("textures") or [])]:
                if texture:
                    found.add(str(texture))
            for source in row.get("source_paths") or []:
                found.add(str(source))
    prefixes: list[str] = []
    for item in found:
        normalized = item.replace("\\", "/")
        if normalized.endswith(".png") or normalized.endswith(".mcmeta"):
            prefixes.append(normalized.rsplit("/", 1)[0] + "/")
    return found, tuple(sorted(set(prefixes)))


def texture_png(texture: str) -> str:
    path = texture.split(":", 1)[1]
    return "assets/cruciblecraft/textures/" + path + ".png"


def needs_gt6_manifest(texture: str, prefixes: tuple[str, ...]) -> bool:
    if not texture.startswith("cruciblecraft:"):
        return False
    if GT6_FAMILY_RE.match(texture):
        return True
    png = texture_png(texture)
    return any(png.startswith(prefix) for prefix in prefixes)


def texture_manifested(texture: str, manifested: set[str]) -> bool:
    if not texture.startswith("cruciblecraft:"):
        return True
    path = texture.split(":", 1)[1]
    png = texture_png(texture)
    short = "textures/" + path + ".png"
    return (
        texture in manifested
        or png in manifested
        or short in manifested
        or path in manifested
        or any(path in item or item.endswith(path + ".png") for item in manifested)
    )


def lang_key(registry_path: str) -> str:
    return names.translation_key("block", registry_path)


def generated_blockstate_paths() -> list[str]:
    root = GENERATED / "assets/cruciblecraft/blockstates"
    if not root.is_dir():
        return []
    paths: list[str] = []
    for path in sorted(root.rglob("*.json")):
        rel = path.relative_to(root).as_posix()
        if rel.endswith(".json"):
            paths.append(rel[:-5])
    return paths


def slash_key_errors(lang: dict[str, Any], locale: str) -> list[str]:
    return [
        f"{locale} slash translation key {key}"
        for key in lang
        if isinstance(key, str) and names.is_illegal_slash_key(key)
    ]


def collision_errors(table: dict[str, Any], locale: str) -> list[str]:
    return [
        f"{locale} display-name collision {row['display_name']}"
        for row in names.grouped_collisions(table, locale=locale)
    ]


def english_copy_errors(
    english: dict[str, Any], chinese: dict[str, Any]
) -> list[str]:
    errors: list[str] = []
    for key, zh in chinese.items():
        if not isinstance(zh, str):
            continue
        if names.is_template_key(key):
            continue
        if names.registry_backed_match(key) is None:
            continue
        en = english.get(key) if isinstance(english.get(key), str) else None
        if names.is_english_copy(zh, en):
            errors.append(f"zh_cn English copy {key}={zh}")
    return errors


def mineable_ids() -> set[str]:
    found: set[str] = set()
    for root in (GENERATED, MAIN):
        tags = root / "data/minecraft/tags/block/mineable"
        if not tags.is_dir():
            continue
        for path in tags.glob("*.json"):
            try:
                document = load_json(path)
            except ValueError:
                continue
            for value in document.get("values") or []:
                if isinstance(value, str):
                    found.add(value)
                elif isinstance(value, dict) and value.get("id"):
                    found.add(str(value["id"]))
    return found


def check_block(
    row: dict[str, str],
    lang: dict[str, Any],
    manifested: set[str],
    prefixes: tuple[str, ...],
    mineable: set[str],
) -> list[str]:
    errors: list[str] = []
    registry = row["registry_path"]
    runtime = row["runtime_id"]
    state_path = resolve_resource(
        "assets/cruciblecraft/blockstates", registry + ".json"
    )
    if state_path is None:
        errors.append(f"{runtime} missing blockstate")
        return errors
    try:
        blockstate = load_json(state_path)
    except ValueError as exc:
        errors.append(f"{runtime} blockstate: {exc}")
        return errors
    models = variant_models(blockstate)
    if not models:
        errors.append(f"{runtime} blockstate has no models")
    for model_id in models:
        rel = model_rel(model_id)
        if rel is None:
            continue
        model_path = resolve_resource(*rel.split("/"))
        if model_path is None:
            errors.append(f"{runtime} missing model {model_id}")
            continue
        try:
            model = load_json(model_path)
        except ValueError as exc:
            errors.append(f"{runtime} model {model_id}: {exc}")
            continue
        parent = str(model.get("parent") or "")
        if parent == model_id:
            errors.append(f"{runtime} self-parent model {model_id}")
        textures = model.get("textures") or {}
        if isinstance(textures, dict):
            for texture in textures.values():
                value = str(texture)
                if needs_gt6_manifest(value, prefixes) and not texture_manifested(
                    value, manifested
                ):
                    errors.append(f"{runtime} unmanifested live GT6 art {value}")
    if registry not in NO_ITEM_BLOCKS:
        item_model = resolve_resource(
            "assets/cruciblecraft/models/item", registry + ".json"
        )
        if item_model is None:
            errors.append(f"{runtime} missing block item model")
    if registry not in NO_LOOT_BLOCKS:
        loot = resolve_resource(
            "data/cruciblecraft/loot_table/blocks", registry + ".json"
        )
        if loot is None:
            errors.append(f"{runtime} missing loot table")
        if runtime not in mineable and not registry.startswith(
                HAND_BREAKABLE_PREFIXES):
            errors.append(f"{runtime} missing mineable tag owner")
    if lang_key(registry) not in lang:
        errors.append(f"{runtime} missing language owner {lang_key(registry)}")
    return errors


def compile_manifest() -> dict[str, Any]:
    lang_path = GENERATED / "assets/cruciblecraft/lang/en_us.json"
    zh_path = GENERATED / "assets/cruciblecraft/lang/zh_cn.json"
    lang = load_json(lang_path) if lang_path.is_file() else {}
    chinese = load_json(zh_path) if zh_path.is_file() else {}
    manifested, prefixes = manifested_textures()
    mineable = mineable_ids()
    blocks = live_block_paths()
    errors: list[str] = []
    for row in blocks:
        errors.extend(check_block(row, lang, manifested, prefixes, mineable))
    live_set = {row["registry_path"] for row in blocks}
    lang_blockstates = generated_blockstate_paths()
    for registry in lang_blockstates:
        if registry in live_set:
            continue
        key = lang_key(registry)
        if key not in lang:
            errors.append(f"cruciblecraft:{registry} missing language owner {key}")
    errors.extend(slash_key_errors(lang, "en_us"))
    errors.extend(slash_key_errors(chinese, "zh_cn"))
    errors.extend(collision_errors(lang, "en_us"))
    errors.extend(collision_errors(chinese, "zh_cn"))
    errors.extend(english_copy_errors(lang, chinese))
    return {
        "errors": errors,
        "generated_by": "tools/build_generated_resource_gate.py",
        "lang_blockstate_count": len(lang_blockstates),
        "live_block_count": len(blocks),
        "schema_version": 1,
        "status": "FAIL" if errors else "PASS",
    }


def compare_datagen_trees(first: Path, second: Path) -> list[str]:
    return [
        f"datagen double-run drift: {error}"
        for error in tree_compare.compare_trees(first, second)
    ]


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--compare-isolated", nargs=2, metavar=("FIRST", "SECOND"))
    args = parser.parse_args(argv)
    if args.compare_isolated:
        errors = compare_datagen_trees(
            Path(args.compare_isolated[0]), Path(args.compare_isolated[1])
        )
        if errors:
            print("\n".join(errors), file=__import__("sys").stderr)
            return 1
        print("datagen isolated trees are identical")
        return 0
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    manifest = compile_manifest()
    encoded = dumps(
        {
            "generated_by": manifest["generated_by"],
            "live_block_count": manifest["live_block_count"],
            "schema_version": manifest["schema_version"],
            "status": manifest["status"],
        }
    )
    if args.write:
        if manifest["errors"]:
            print("\n".join(manifest["errors"][:80]), file=__import__("sys").stderr)
            if len(manifest["errors"]) > 80:
                print(
                    f"... {len(manifest['errors']) - 80} more",
                    file=__import__("sys").stderr,
                )
            return 1
        atomic_io.write_bytes(MANIFEST, encoded)
        print(f"Wrote {io.relative(MANIFEST)}")
        return 0
    if manifest["errors"]:
        print("\n".join(manifest["errors"][:80]), file=__import__("sys").stderr)
        if len(manifest["errors"]) > 80:
            print(
                f"... {len(manifest['errors']) - 80} more",
                file=__import__("sys").stderr,
            )
        return 1
    if not MANIFEST.is_file() or MANIFEST.read_bytes() != encoded:
        print("generated resource gate manifest is stale; run --write")
        return 1
    print("generated resource gate is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
