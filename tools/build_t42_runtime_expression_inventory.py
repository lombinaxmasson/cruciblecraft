#!/usr/bin/env python3
"""Build the T42 runtime expression inventory and vanilla allowlist."""
from __future__ import annotations

import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35
from tools import t41_common as t41
from tools import t42_common as common

OUTPUT = common.RUNTIME_INVENTORY
ALLOWLIST = common.VANILLA_ALLOWLIST
COMPONENT_RE = re.compile(
    r'COMPONENTS\.registerComponentType\(\s*"([a-z0-9_]+)"'
)


def _component_ids() -> list[str]:
    text = common.MOD_COMPONENTS_JAVA.read_text(encoding="utf-8")
    return [f"cruciblecraft:{name}" for name in COMPONENT_RE.findall(text)]


def _vanilla_allowlist(registry_ids: dict[str, set[str]]) -> dict[str, Any]:
    dump_aliases = set(common.T37_VANILLA_ALIASES.keys()) | set(t41.VANILLA_RUNTIME_ALIASES.keys())
    alias_item_ids = {
        item_id
        for item_id in (
            set(common.T37_VANILLA_ALIASES.values())
            | set(t41.VANILLA_RUNTIME_ALIASES.values())
        )
        if item_id.startswith("minecraft:")
    }
    explicit_item_ids = ["minecraft:oak_planks"]
    live_items = {
        item_id
        for item_id in alias_item_ids | set(explicit_item_ids)
        if item_id.startswith("minecraft:")
    }
    dump_alias_ids = sorted(
        item_id
        for item_id in dump_aliases
        if item_id.startswith("minecraft:") and item_id not in live_items
    )
    fluid_ids = ["minecraft:water", "minecraft:lava"]
    t35_fluids = registry_ids.get("fluids") or set()
    for fluid_id in fluid_ids:
        if fluid_id in (registry_ids.get("items") or set()):
            raise ValueError(
                f"{fluid_id} must not be treated as a T35 item-registry proof"
            )
    vanilla_items = sorted(live_items)
    return {
        "alias_item_ids": sorted(alias_item_ids),
        "dump_alias_ids": dump_alias_ids,
        "explicit_item_ids": explicit_item_ids,
        "fluid_ids": fluid_ids,
        "generated_by": "python tools/build_t42_runtime_expression_inventory.py",
        "item_ids": vanilla_items,
        "minecraft_prefix_is_not_proof": True,
        "not_a_121_registry_scrape": True,
        "note": (
            "item_ids = T37/T41 alias live IDs plus explicit oak_planks. "
            "Dump keys such as minecraft:noteblock stay in dump_alias_ids and "
            "are remapped by vanilla_alias, not by this allowlist. Remaining "
            "dump has no oak_planks row."
        ),
        "provenance": "t37_t41_aliases_plus_explicit",
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_VANILLA_ITEM_ALLOWLIST",
        "t35_fluid_registry_contains_water_lava": {
            fluid_id: fluid_id in t35_fluids for fluid_id in fluid_ids
        },
        "version": "minecraft-1.21.1",
    }


def build() -> dict[str, Any]:
    catalogs = common.load_runtime_catalogs()
    registry_ids = catalogs["registry_ids"]
    allowlist = _vanilla_allowlist(registry_ids)
    fixture = common.load_json(common.T35_RUNTIME_GATE_FIXTURE)
    fixture_items = set(fixture.get("categories", {}).get("items") or [])
    live_items = set(registry_ids.get("items") or [])
    published = common.published_relation_identities()
    components = _component_ids()
    for predicate in common.COMPONENT_PREDICATES:
        if predicate not in components:
            raise ValueError(f"missing compact component predicate {predicate}")
    return {
        "cc_static": {
            "block_count": len(registry_ids.get("blocks") or []),
            "item_count": len(live_items),
            "registry_path": "tools/t35_runtime_registry.json",
            "registry_sha256": common.sha256_file(common.T35_RUNTIME_REGISTRY),
        },
        "component_predicates": components,
        "fluids": {
            "generation_tag_molten_material_count": len(
                catalogs.get("generation_tag_molten") or []
            ),
            "mapped_count": len(catalogs["fluid_to_cc"]),
            "molten_not_cc_registration_proof": True,
            "remaining_molten_fluids_in_mapping": len(
                catalogs.get("remaining_molten_fluids") or []
            ),
            "top_level_molten_flag_count": len(catalogs.get("top_level_molten") or []),
        },
        "generated_by": "python tools/build_t42_runtime_expression_inventory.py",
        "material_forms": {
            "prefix_forms": sorted(catalogs["prefix_forms"]),
            "registered_material_count": len(catalogs["registered_forms"]),
        },
        "minecraft_prefix_is_not_proof": True,
        "programmed_circuit": {
            "mapped_from": common.CIRCUIT_ITEM,
            "mapping_is_not_expression_proof": True,
            "present_in_t35_runtime_registry": common.PROGRAMMED_CIRCUIT
            in live_items,
            "runtime_id": common.PROGRAMMED_CIRCUIT,
        },
        "published_relation_count": len(published),
        "registry_fixture_bidirectional": {
            "extra_in_live_not_in_fixture_sample": sorted(live_items - fixture_items)[:20],
            "fixture_item_count": len(fixture_items),
            "live_item_count": len(live_items),
            "missing_from_live_sample": sorted(fixture_items - live_items)[:20],
        },
        "schema_version": 1,
        "source_revision": common.SOURCE_REVISION,
        "status": "T42_RUNTIME_EXPRESSION_INVENTORY",
        "vanilla_allowlist": {
            "count": len(allowlist["item_ids"]),
            "explicit_item_ids": list(allowlist["explicit_item_ids"]),
            "not_a_121_registry_scrape": True,
            "path": "tools/t42_vanilla_item_allowlist.json",
            "provenance": allowlist["provenance"],
            "version": allowlist["version"],
        },
        "_allowlist": allowlist,
    }


def write() -> None:
    document = build()
    allowlist = document.pop("_allowlist")
    t35.write_stable(ALLOWLIST, allowlist)
    t35.write_stable(OUTPUT, document)


def check() -> list[str]:
    document = build()
    allowlist = document.pop("_allowlist")
    errors = []
    errors.extend(common.check_document(ALLOWLIST, allowlist))
    errors.extend(common.check_document(OUTPUT, document))
    return errors


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Build T42 runtime expression inventory", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        if args.write:
            write()
            print("Wrote T42 runtime expression inventory.")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("T42 runtime expression inventory is current.")
        return 0
    except (OSError, ValueError, KeyError) as error:
        print(f"T42 runtime expression inventory failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
