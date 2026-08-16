#!/usr/bin/env python3
"""Build the T26 localization-accounting artifact.

T26d card / O-15 reclassification: zh_cn is accounted per player-visible
domain (material names / machine-block-item / GUI-status / guide-error),
never by an arbitrary global percentage. The v1 critical-path materials
carry real Chinese names from the bundled table; everything absent from
the table is explicitly ``post_1_0`` and no English-copy fake
translation is ever emitted.

The builder scans the generated en_us / zh_cn files and the bundled
table and derives:
- per-domain key counts and the zh gap for every domain;
- the post_1_0 material long tail (en material keys with no zh key);
- table integrity (every table material must exist in the catalog; the
  table may not contain English-copy values);
- the O-15 reclassification row (v1 critical domains, long-tail
  disposition = post_1_0, owner T26).

``status`` is "T26_LOCALIZATION_ACCOUNTED" only when every domain is
accounted and the table is consistent; otherwise the key is written as
null (fail-closed, never "").
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

DOMAIN_PREFIXES = {
    "material": "material.cruciblecraft.",
    "block": "block.cruciblecraft.",
    "fluid": "fluid_type.cruciblecraft.",
    "item": "item.cruciblecraft.",
    "screen": "screen.cruciblecraft.",
    "jade": "jade.cruciblecraft.",
    "tooltip": "tooltip.cruciblecraft.",
    "message": "message.cruciblecraft.",
    "emi": "emi.",
    "item_group": "itemGroup.cruciblecraft.",
    "container": "container.cruciblecraft.",
    "disconnect": "disconnect.cruciblecraft.",
    "config": "cruciblecraft.configuration.",
}
V1_CRITICAL_DOMAINS = (
    "material",
    "block",
    "fluid",
    "item",
    "screen",
    "jade",
    "tooltip",
    "message",
    "emi",
    "item_group",
    "container",
    "disconnect",
    "config",
)
# T26a first-play path: material names, machine/block/item names and
# GUI/status text must be complete in zh; the material long tail is
# explicitly post_1_0.
ZERO_GAP_DOMAINS = ("screen", "container", "disconnect", "item_group", "config")


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t26_localization_ledger.json"
EN_US = ROOT / "src/generated/resources/assets/cruciblecraft/lang/en_us.json"
ZH_CN = ROOT / "src/generated/resources/assets/cruciblecraft/lang/zh_cn.json"
TABLE = ROOT / "src/main/resources/data/cruciblecraft/material_zh_cn.json"
BUILDER = Path(__file__).resolve()


def _load_if_exists(path: Path, default: Any = None) -> Any:
    if path.is_file():
        return json.loads(path.read_text(encoding="utf-8"))
    return default


def _stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def _sha256(path: Path) -> str:
    if not path.is_file():
        return hashlib.sha256(b"").hexdigest()
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def build() -> dict[str, Any]:
    en = _load_if_exists(EN_US) or {}
    zh = _load_if_exists(ZH_CN) or {}
    table = _load_if_exists(TABLE) or {}
    table_materials = table.get("materials") or {}

    if not en or not zh or not table_materials:
        raise ValueError(
            "localization inputs are missing; run datagen and commit "
            "the generated lang files before accounting"
        )

    # Table integrity: every table id must exist in the en material
    # domain, and no value may be an English copy (must contain at
    # least one CJK character).
    en_material_ids = {
        key[len(DOMAIN_PREFIXES["material"]):]
        for key in en
        if key.startswith(DOMAIN_PREFIXES["material"])
    }
    stale = sorted(set(table_materials) - en_material_ids)
    if stale:
        raise ValueError(f"stale table material ids: {stale}")
    english_copies = sorted(
        material_id
        for material_id, name in table_materials.items()
        if not any("一" <= ch <= "鿿" for ch in name)
    )
    if english_copies:
        raise ValueError(
            f"table values are English copies (forbidden): "
            f"{english_copies}"
        )

    domains = {}
    for domain, prefix in DOMAIN_PREFIXES.items():
        en_keys = {key for key in en if key.startswith(prefix)}
        zh_keys = {key for key in zh if key.startswith(prefix)}
        domains[domain] = {
            "en_keys": len(en_keys),
            "zh_keys": len(zh_keys),
            "gap": len(en_keys - zh_keys),
            "zero_gap": len(en_keys - zh_keys) == 0,
        }

    post_1_0_materials = sorted(
        en_material_ids - set(table_materials)
    )
    critical_path_targets = {
        domain: "zero"
        for domain in ZERO_GAP_DOMAINS
    }
    critical_path_targets["material"] = (
        f"table-covered ({len(table_materials)}), long tail "
        f"post_1_0 ({len(post_1_0_materials)})"
    )
    o15_reclassification = {
        "item": "O-15",
        "classification": "v1_required",
        "owner": "T26",
        "v1_critical_domains": {
            domain: domains[domain]["gap"] == 0
            for domain in ZERO_GAP_DOMAINS
        },
        "material_domain": {
            "critical_path_translated": len(table_materials),
            "long_tail_disposition": "post_1_0",
            "long_tail_count": len(post_1_0_materials),
            "no_english_copy_fake_translations": True,
        },
        "residual": (
            "non-critical domains (block/fluid/item long tail composed "
            "from post_1_0 materials) fall back to en_us by policy"
        ),
    }

    accounted = (
        all(domains[domain]["zero_gap"] for domain in ZERO_GAP_DOMAINS)
        and len(table_materials) > 0
        and not stale
        and not english_copies
    )
    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "build_t26_localization",
        "status": "T26_LOCALIZATION_ACCOUNTED" if accounted else None,
        "policy": (
            "zh_cn is accounted per player-visible domain, never by an "
            "arbitrary global percentage. The v1 critical path uses "
            "real translations from the bundled table; everything else "
            "is explicitly post_1_0 and falls back to en_us. "
            "English-copy fake translations are forbidden."
        ),
        "domains": domains,
        "totals": {
            "en_keys": len(en),
            "zh_keys": len(zh),
            "table_materials": len(table_materials),
            "post_1_0_materials": len(post_1_0_materials),
        },
        "o15_reclassification": o15_reclassification,
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
                _relative(EN_US): _sha256(EN_US),
                _relative(ZH_CN): _sha256(ZH_CN),
                _relative(TABLE): _sha256(TABLE),
            }
        },
    }
    return document


def check() -> list[str]:
    """Return staleness / integrity errors (empty = clean)."""
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors
    on_disk = _load_if_exists(OUTPUT)
    expected = build()
    if _stable(on_disk) != _stable(expected):
        errors.append(f"stale generated file: {_relative(OUTPUT)}")
    return errors


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(_stable(document), encoding="utf-8", newline="\n")
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = _load_if_exists(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T26 localization failed: {error}", file=sys.stderr)
        return 1
    summary = {"schema_version": document.get("schema_version")}
    if "status" in document and document["status"]:
        summary["status"] = document["status"]
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
