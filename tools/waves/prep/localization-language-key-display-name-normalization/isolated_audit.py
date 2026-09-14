#!/usr/bin/env python3
"""Isolated language-key / display-name contract for the localization prep card.

Prep-owned. Does not modify landing generators, registry ids, or generated lang.
Landing must copy these functions into ModLanguageProvider, GeneratedMaterialPack,
MaterialFormItem, generated_resource_gate, player_complete, and catalog_modern_ids.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from pathlib import Path
from typing import Any

WAVE = Path(__file__).resolve().parent
ROOT = WAVE.parents[3]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import io_common as io
from tools import generated_resource_gate as gate
from tools import language_names as names

SLUG = "localization/language-key-display-name-normalization"
NAMESPACE = "cruciblecraft"

LANGUAGE_KEY_CONTRACT = WAVE / "language_key_contract.json"
ENGLISH_CONTRACT = WAVE / "english_display_name_contract.json"
ZH_CONTRACT = WAVE / "zh_cn_fallback_contract.json"
COLLISION_CONTRACT = WAVE / "display_name_collision_contract.json"
BASELINE = WAVE / "baseline" / "current_audit.json"
MANIFEST = WAVE / "source_pack_manifest.json"
GENERATED_LANG = (
    ROOT / "src" / "generated" / "resources" / "assets" / "cruciblecraft" / "lang"
)

CONTRACT_FILES = (
    LANGUAGE_KEY_CONTRACT,
    ENGLISH_CONTRACT,
    ZH_CONTRACT,
    COLLISION_CONTRACT,
)


def load_contract(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def language_key_contract() -> dict[str, Any]:
    return load_contract(LANGUAGE_KEY_CONTRACT)


def english_contract() -> dict[str, Any]:
    return load_contract(ENGLISH_CONTRACT)


def zh_contract() -> dict[str, Any]:
    return load_contract(ZH_CONTRACT)


def collision_contract() -> dict[str, Any]:
    return load_contract(COLLISION_CONTRACT)


def translation_key(
    object_type: str,
    registry_path: str,
    *,
    namespace: str = NAMESPACE,
) -> str:
    return names.translation_key(
        object_type, registry_path, namespace=namespace
    )


def is_template_key(key: str, contract: dict[str, Any] | None = None) -> bool:
    if contract is None:
        return names.is_template_key(key)
    prefixes = contract["template_key_prefixes"]
    return any(key.startswith(prefix) for prefix in prefixes)


def registry_backed_match(key: str) -> re.Match[str] | None:
    return names.registry_backed_match(key)


def is_illegal_slash_key(key: str) -> bool:
    return names.is_illegal_slash_key(key)


def format_english_id(value: str, contract: dict[str, Any] | None = None) -> str:
    del contract
    return names.format_english_id(value)


def compose_english(material_id: str, form_id: str) -> str:
    return names.compose_english(material_id, form_id)


def has_cjk(text: str) -> bool:
    return names.has_cjk(text)


def is_english_copy(zh: str, en: str | None = None) -> bool:
    return names.is_english_copy(zh, en)


def identity_group(key: str) -> tuple[str, ...]:
    return names.identity_group(key)


def grouped_collisions(
    table: dict[str, str],
    *,
    allowlist: set[str] | None = None,
) -> list[dict[str, Any]]:
    if allowlist is None:
        return names.grouped_collisions(table)
    allowed = allowlist
    collisions = names.grouped_collisions(table)
    return [row for row in collisions if row["display_name"] not in allowed]


def _sample(values: list[str], limit: int = 25) -> list[str]:
    return values[:limit]


def _trim_collision(
    row: dict[str, Any],
    *,
    identity_limit: int = 6,
    key_limit: int = 4,
) -> dict[str, Any]:
    identities = []
    for ident in row["identities"][:identity_limit]:
        keys = ident["keys"]
        identities.append(
            {
                "group": ident["group"],
                "key_count": len(keys),
                "keys": keys[:key_limit],
            }
        )
    return {
        "display_name": row["display_name"],
        "identity_count": len(row["identities"]),
        "identities": identities,
    }


def scan_lang_tables() -> dict[str, Any]:
    english_path = GENERATED_LANG / "en_us.json"
    chinese_path = GENERATED_LANG / "zh_cn.json"
    english = io.load_json(english_path) if english_path.is_file() else {}
    chinese = io.load_json(chinese_path) if chinese_path.is_file() else {}
    slash_keys = sorted(
        key for key in english if isinstance(key, str) and is_illegal_slash_key(key)
    )
    zh_slash = sorted(
        key for key in chinese if isinstance(key, str) and is_illegal_slash_key(key)
    )
    copies: list[dict[str, str]] = []
    latin_only: list[dict[str, str]] = []
    for key, zh in chinese.items():
        if not isinstance(zh, str):
            continue
        if is_template_key(key) or registry_backed_match(key) is None:
            continue
        en = english.get(key) if isinstance(english.get(key), str) else None
        if not is_english_copy(zh, en):
            continue
        row = {"key": key, "zh": zh}
        if en is not None:
            row["en"] = en
        if en is not None and zh.strip() == en.strip():
            copies.append(row)
        else:
            latin_only.append(row)
    live = gate.live_block_paths()
    live_paths = {row["registry_path"] for row in live}
    gate_gaps: list[str] = []
    for key in slash_keys:
        match = registry_backed_match(key)
        if match is None or match.group(1) != "block":
            continue
        registry_path = match.group(2)
        if registry_path not in live_paths:
            gate_gaps.append(registry_path)
    focus = {
        "aluminium_fluid_pipe_slash": "block.cruciblecraft.aluminium/fluid_pipe"
        in english,
        "aluminium_fluid_pipe_dotted": "block.cruciblecraft.aluminium.fluid_pipe"
        in english,
        "aluminium_block_slash": "block.cruciblecraft.aluminium/block" in english,
        "water_twig": english.get("item.cruciblecraft.water.plant_gt_twig"),
        "asphalt_black": english.get("block.cruciblecraft.asphalt.black"),
        "asphalt_black_slab": english.get(
            "block.cruciblecraft.asphalt.black.slab_down"
        ),
        "andesite_m8": english.get("block.cruciblecraft.andesite.reinforced_bricks"),
        "flower_black_dye_zh": chinese.get("fluid.cruciblecraft.dye_flower_black"),
    }
    en_collisions = grouped_collisions(english)
    zh_collisions = grouped_collisions(chinese)
    return {
        "schema_version": 1,
        "capability_slug": SLUG,
        "status": "LANDING_SCAN",
        "en_us_keys": len(english),
        "zh_cn_keys": len(chinese),
        "slash_keys": {
            "en_us_count": len(slash_keys),
            "zh_cn_count": len(zh_slash),
            "en_us_sample": _sample(slash_keys),
            "canonical_sample": [
                {
                    "current": key,
                    "canonical": translation_key(
                        registry_backed_match(key).group(1),
                        registry_backed_match(key).group(2),
                    ),
                }
                for key in _sample(slash_keys, 10)
                if registry_backed_match(key)
            ],
        },
        "english_copy_zh": {
            "equal_to_en_count": len(copies),
            "latin_only_count": len(latin_only),
            "equal_to_en_sample": _sample(
                [f"{row['key']}={row['zh']}" for row in copies]
            ),
            "latin_only_sample": _sample(
                [f"{row['key']}={row['zh']}" for row in latin_only]
            ),
        },
        "collisions": {
            "en_us_count": len(en_collisions),
            "zh_cn_count": len(zh_collisions),
            "en_us_sample": [_trim_collision(row) for row in en_collisions[:15]],
        },
        "gate_inventory": {
            "live_block_count": len(live),
            "slash_block_lang_missing_from_live": len(gate_gaps),
            "sample": _sample(gate_gaps),
        },
        "focus": focus,
        "allowlist": collision_contract()["allowlist"],
    }


def write_baseline(document: dict[str, Any] | None = None) -> dict[str, Any]:
    payload = document if document is not None else scan_lang_tables()
    BASELINE.parent.mkdir(parents=True, exist_ok=True)
    io.write_stable(BASELINE, payload)
    return payload


def file_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def manifest_files() -> list[dict[str, str]]:
    rows: list[dict[str, str]] = []
    for path in (
        *CONTRACT_FILES,
        WAVE / "isolated_audit.py",
        WAVE / "test_isolated_audit.py",
        WAVE / "source.json",
        WAVE / "source_review.json",
        WAVE / "landing_gate.json",
        WAVE / "work_set.json",
        WAVE / "readiness.json",
        BASELINE,
    ):
        if not path.is_file():
            continue
        rows.append(
            {
                "path": io.relative(path),
                "role": path.stem,
                "sha256": file_sha256(path),
            }
        )
    return rows


def write_manifest() -> dict[str, Any]:
    document = {
        "files": manifest_files(),
        "provenance_policy": {
            "append_only": True,
            "forbid_gt6u": True,
            "no_landing_edits": True,
        },
        "schema_version": 1,
        "source_dialect": "cc_language_contract",
        "source_pack_id": "prep/localization-language-key-display-name-normalization",
        "source_revision_cc": io.load_json(WAVE / "source.json")["source_revision_cc"],
        "source_revision_gt6": io.SOURCE_REVISION,
        "source_system": "cruciblecraft",
    }
    io.write_stable(MANIFEST, document)
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    args = parser.parse_args(argv)
    document = scan_lang_tables()
    if args.write:
        write_baseline(document)
        if (WAVE / "source.json").is_file():
            write_manifest()
    print(json.dumps(
        {
            "slash_en": document["slash_keys"]["en_us_count"],
            "english_copy_zh": document["english_copy_zh"]["equal_to_en_count"],
            "collisions_en": document["collisions"]["en_us_count"],
            "gate_gaps": document["gate_inventory"]["slash_block_lang_missing_from_live"],
            "live_blocks": document["gate_inventory"]["live_block_count"],
        },
        indent=2,
        sort_keys=True,
    ))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
