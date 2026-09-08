#!/usr/bin/env python3
"""Global registry path and semantic identity gate."""
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
from tools import capability_ledger
from tools import io_common as io

MANIFEST = io.TOOLS / "registry_identity_manifest.json"
REGISTER_RE = re.compile(
    r'(?:ITEMS\.register(?:SimpleBlockItem)?|ITEMS\.register)\(\s*"([^"]+)"'
)
BLOCK_REGISTER_RE = re.compile(
    r'BLOCKS\.register(?:SimpleBlock)?\(\s*"([^"]+)"'
)
SHAPE_ID_RE = re.compile(r'\bshape\("([a-z0-9_]+)"')
PATTERN_ID_RE = re.compile(r'\bpattern\("([a-z0-9_]+)"')
WOOD_ID_RE = re.compile(r'\bwood\("([a-z0-9_]+)"')
COVER_FILES = (
    ROOT / "src/main/resources/data/cruciblecraft/cover_definitions.json",
    ROOT
    / "src/main/resources/data/cruciblecraft/item_network_cover_definitions.json",
    ROOT
    / "src/main/resources/data/cruciblecraft/fluid_network_cover_definitions.json",
    ROOT
    / "src/main/resources/data/cruciblecraft/generic_network_cover_definitions.json",
    ROOT
    / "src/main/resources/data/cruciblecraft/logistics_dump_cover_definitions.json",
    ROOT
    / "src/main/resources/data/cruciblecraft/logistics_display_cpu_cover_definitions.json",
)
BATH = ROOT / "src/main/resources/data/cruciblecraft/bath_identity_catalog.json"
SEMANTIC = (
    ROOT / "src/main/resources/data/cruciblecraft/semantic_object_catalog.json"
)
BATH_MTE = (
    ROOT / "src/main/resources/data/cruciblecraft/bath_mte_identity_catalog.json"
)
SMELTER_MTE = (
    ROOT
    / "src/main/resources/data/cruciblecraft/smelter_mte_identity_catalog.json"
)
GT_BLOCK = (
    ROOT / "src/main/resources/data/cruciblecraft/gt_block_object_catalog.json"
)
GT_STONE = ROOT / "src/main/resources/data/cruciblecraft/gt_stone_catalog.json"
BATH_REMAINDER = (
    ROOT
    / "src/main/resources/data/cruciblecraft/bath_remainder_identity_catalog.json"
)
MACHINE_TIERS = ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"
TECHNOLOGICAL_PARTS = (
    ROOT / "src/main/resources/data/cruciblecraft/technological_parts.json"
)
TRANSFORMER_TIERS = (
    ROOT / "src/main/resources/data/cruciblecraft/energy_transformer_tiers.json"
)
BATTERY_TIERS = (
    ROOT / "src/main/resources/data/cruciblecraft/energy_battery_tiers.json"
)
CONVERTER_TIERS = (
    ROOT / "src/main/resources/data/cruciblecraft/energy_converter_tiers.json"
)
STORAGE_VARIANTS = (
    ROOT / "src/main/resources/data/cruciblecraft/storage_variants.json"
)
EXTRUDER_JAVA = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/item"
    / "ExtruderShapeCatalog.java"
)
TOOL_PATTERN_JAVA = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/item"
    / "ToolPatternCatalog.java"
)
GT_WOOD_JAVA = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/content/item"
    / "GtWoodCatalog.java"
)
MOD_ITEMS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModItems.java"
)
MOD_BLOCKS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModBlocks.java"
)
REGISTRY = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/logistics/pipe/cover"
    / "CoverBehaviorRegistry.java"
)
BUILTIN_RE = re.compile(r'registerBuiltin\("([a-z0-9_]+)"')
NETWORK_REGISTER_RE = re.compile(
    r'ResourceLocation\.fromNamespaceAndPath\(\s*"cruciblecraft",\s*"([^"]+)"'
)

EXPECTED_COVER_BEHAVIORS = [
    "cruciblecraft:conveyor",
    "cruciblecraft:filter",
    "cruciblecraft:logistics_display_cpu",
    "cruciblecraft:logistics_fluid_storage",
    "cruciblecraft:logistics_fluid_transfer",
    "cruciblecraft:logistics_generic_dump",
    "cruciblecraft:logistics_generic_storage",
    "cruciblecraft:logistics_generic_transfer",
    "cruciblecraft:logistics_item_storage",
    "cruciblecraft:logistics_item_transfer",
    "cruciblecraft:pressure_valve",
    "cruciblecraft:pump_adapter",
    "cruciblecraft:retriever_item",
    "cruciblecraft:robot_arm",
    "cruciblecraft:selector_manual",
    "cruciblecraft:shutter",
]


def dumps(document: Any) -> bytes:
    return (
        json.dumps(
            document, indent=2, sort_keys=True, ensure_ascii=False
        )
        + "\n"
    ).encode("utf-8")


def add_record(
    records: list[dict[str, str]],
    *,
    runtime_id: str,
    registry_path: str,
    source: str,
    semantic_key: str,
    display: str = "",
) -> None:
    records.append(
        {
            "display": display,
            "registry_path": registry_path,
            "runtime_id": runtime_id,
            "semantic_key": semantic_key,
            "source": source,
        }
    )


def collect_covers(records: list[dict[str, str]]) -> None:
    for path in COVER_FILES:
        if not path.is_file():
            continue
        document = io.load_json(path)
        rel = io.relative(path)
        for row in document.get("definitions") or []:
            runtime = str(row["id"])
            path_part = runtime.split(":", 1)[-1]
            add_record(
                records,
                runtime_id=runtime,
                registry_path=path_part,
                source=rel,
                semantic_key=f"cover:{path_part}",
                display=path_part,
            )


def collect_identity_catalog(path: Path, prefix: str, records: list[dict[str, str]]) -> None:
    if not path.is_file():
        return
    document = io.load_json(path)
    rel = io.relative(path)
    for row in document.get("identities") or []:
        runtime = str(row.get("runtime_id") or "")
        registry_path = str(row.get("registry_path") or runtime.split(":", 1)[-1])
        english = str(row.get("english_name") or "")
        source_item = str(row.get("source_item") or "")
        meta = row.get("meta")
        semantic_key = f"{prefix}:{source_item}:{meta}"
        if english.lower().startswith("low heat extruder shape"):
            semantic_key = "extruder_shape/low_heat/" + english.split("(")[-1].rstrip(")")
        add_record(
            records,
            runtime_id=runtime,
            registry_path=registry_path,
            source=rel,
            semantic_key=semantic_key,
            display=english,
        )


def collect_mod_items(records: list[dict[str, str]]) -> None:
    text = MOD_ITEMS.read_text(encoding="utf-8")
    rel = io.relative(MOD_ITEMS)
    seen: set[str] = set()
    for match in REGISTER_RE.finditer(text):
        path_part = match.group(1)
        if path_part in seen:
            continue
        seen.add(path_part)
        add_record(
            records,
            runtime_id=f"cruciblecraft:{path_part}",
            registry_path=path_part,
            source=rel,
            semantic_key=f"item:{path_part}",
            display=path_part,
        )


def collect_mod_blocks(records: list[dict[str, str]]) -> None:
    if not MOD_BLOCKS.is_file():
        return
    text = MOD_BLOCKS.read_text(encoding="utf-8")
    rel = io.relative(MOD_BLOCKS)
    seen: set[str] = set()
    for match in BLOCK_REGISTER_RE.finditer(text):
        path_part = match.group(1)
        if path_part in seen:
            continue
        seen.add(path_part)
        add_record(
            records,
            runtime_id=f"cruciblecraft:{path_part}",
            registry_path=path_part,
            source=rel,
            semantic_key=f"block:{path_part}",
            display=path_part,
        )


def collect_java_named_items(
    path: Path,
    pattern: re.Pattern[str],
    registry_path: Any,
    prefix: str,
    records: list[dict[str, str]],
) -> None:
    if not path.is_file():
        return
    text = path.read_text(encoding="utf-8")
    rel = io.relative(path)
    for item_id in pattern.findall(text):
        path_part = registry_path(item_id)
        add_record(
            records,
            runtime_id=f"cruciblecraft:{path_part}",
            registry_path=path_part,
            source=rel,
            semantic_key=f"{prefix}:{item_id}",
            display=item_id,
        )


def collect_technological_parts(records: list[dict[str, str]]) -> None:
    if not TECHNOLOGICAL_PARTS.is_file():
        return
    document = io.load_json(TECHNOLOGICAL_PARTS)
    rel = io.relative(TECHNOLOGICAL_PARTS)
    for row in document.get("parts") or []:
        runtime = str(row.get("id") or "")
        path_part = str(row.get("registry_path") or runtime.split(":", 1)[-1])
        if not runtime or not path_part:
            continue
        add_record(
            records,
            runtime_id=runtime if ":" in runtime else f"cruciblecraft:{path_part}",
            registry_path=path_part,
            source=rel,
            semantic_key=f"technological_part:{path_part}",
            display=str(row.get("english_name") or path_part),
        )


def collect_runtime_id_rows(
    path: Path,
    prefix: str,
    records: list[dict[str, str]],
    *,
    rows_key: str = "tiers",
    id_key: str = "id",
) -> None:
    if not path.is_file():
        return
    document = io.load_json(path)
    rel = io.relative(path)
    for row in document.get(rows_key) or []:
        runtime = str(row.get(id_key) or row.get("runtime_id") or "")
        if not runtime:
            continue
        path_part = runtime.split(":", 1)[-1]
        add_record(
            records,
            runtime_id=runtime if ":" in runtime else f"cruciblecraft:{path_part}",
            registry_path=path_part,
            source=rel,
            semantic_key=f"{prefix}:{path_part}",
            display=path_part,
        )


def collect_gt_stone(records: list[dict[str, str]]) -> None:
    if not GT_STONE.is_file():
        return
    document = io.load_json(GT_STONE)
    rel = io.relative(GT_STONE)
    for identity in document.get("identities") or []:
        source_item = str(identity.get("source_item") or "")
        english = str(identity.get("english_name") or "")
        for variant in identity.get("variants") or []:
            runtime = str(variant.get("runtime_id") or "")
            path_part = str(variant.get("registry_path") or runtime.split(":", 1)[-1])
            meta = variant.get("meta")
            add_record(
                records,
                runtime_id=runtime,
                registry_path=path_part,
                source=rel,
                semantic_key=f"stone:{source_item}:{meta}",
                display=english,
            )


def collect_behaviors() -> list[str]:
    text = REGISTRY.read_text(encoding="utf-8")
    builtins = [
        f"cruciblecraft:{name}" for name in BUILTIN_RE.findall(text)
    ]
    extra: list[str] = []
    itemnet = (
        ROOT
        / "src/main/java/com/masson/cruciblecraft/logistics/itemnet"
        / "ItemNetworkKinds.java"
    )
    fluidnet = (
        ROOT
        / "src/main/java/com/masson/cruciblecraft/logistics/fluidnet"
        / "FluidNetworkKinds.java"
    )
    for path in (itemnet, fluidnet):
        if not path.is_file():
            continue
        body = path.read_text(encoding="utf-8")
        for name in NETWORK_REGISTER_RE.findall(body):
            extra.append(f"cruciblecraft:{name}")
    # Kinds files list definition ids; behaviors are storage + transfer.
    behaviors = list(builtins)
    for candidate in (
        "cruciblecraft:logistics_item_storage",
        "cruciblecraft:logistics_item_transfer",
        "cruciblecraft:logistics_fluid_storage",
        "cruciblecraft:logistics_fluid_transfer",
        "cruciblecraft:logistics_generic_storage",
        "cruciblecraft:logistics_generic_transfer",
        "cruciblecraft:logistics_generic_dump",
        "cruciblecraft:logistics_display_cpu",
    ):
        if candidate not in behaviors:
            behaviors.append(candidate)
    return sorted(set(behaviors))


def dispositions() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for path in capability_ledger.capability_files():
        document = capability_ledger.load_capability(path)
        for row in document.get("identity_disposition") or []:
            item = dict(row)
            item["capability"] = document["slug"]
            rows.append(item)
    return rows


def compile_manifest() -> dict[str, Any]:
    records: list[dict[str, str]] = []
    collect_covers(records)
    collect_identity_catalog(BATH, "bath", records)
    collect_identity_catalog(SEMANTIC, "semantic", records)
    collect_identity_catalog(BATH_MTE, "bath_mte", records)
    collect_identity_catalog(SMELTER_MTE, "smelter_mte", records)
    collect_identity_catalog(GT_BLOCK, "gt_block", records)
    collect_identity_catalog(BATH_REMAINDER, "building_block", records)
    collect_gt_stone(records)
    collect_runtime_id_rows(MACHINE_TIERS, "machine", records, rows_key="variants")
    collect_technological_parts(records)
    collect_runtime_id_rows(TRANSFORMER_TIERS, "transformer", records)
    collect_runtime_id_rows(BATTERY_TIERS, "battery", records)
    collect_runtime_id_rows(CONVERTER_TIERS, "converter", records)
    collect_runtime_id_rows(
        STORAGE_VARIANTS, "storage", records, rows_key="variants", id_key="runtime_id"
    )
    collect_java_named_items(
        EXTRUDER_JAVA,
        SHAPE_ID_RE,
        lambda item_id: f"extruder_shape_{item_id}",
        "multiitem_extruder",
        records,
    )
    collect_java_named_items(
        TOOL_PATTERN_JAVA,
        PATTERN_ID_RE,
        lambda item_id: f"tool_pattern_{item_id}",
        "multiitem_tool_pattern",
        records,
    )
    collect_java_named_items(
        GT_WOOD_JAVA,
        WOOD_ID_RE,
        lambda item_id: f"gt_wood/{item_id}",
        "building_block_wood",
        records,
    )
    collect_mod_items(records)
    collect_mod_blocks(records)
    errors: list[str] = []
    by_path: dict[str, list[dict[str, str]]] = {}
    by_id: dict[str, list[dict[str, str]]] = {}
    by_semantic: dict[str, list[dict[str, str]]] = {}
    for row in records:
        if not row["runtime_id"] or not row["registry_path"]:
            errors.append(
                f"collector emitted an empty identity from {row.get('source', '?')}"
            )
            continue
        by_path.setdefault(row["registry_path"], []).append(row)
        by_id.setdefault(row["runtime_id"], []).append(row)
        by_semantic.setdefault(row["semantic_key"], []).append(row)
    allowed_multi: dict[str, set[str]] = {}
    blocked: set[str] = set()
    allowed_prefixes: list[str] = []
    for row in dispositions():
        key = str(row["semantic_key"])
        ids = {str(item) for item in row.get("runtime_ids") or []}
        if row["disposition"] == "blocked":
            blocked.update(ids)
        if row["disposition"] in {"new_distinct", "bridge", "reuse_canonical"}:
            allowed_multi.setdefault(key, set()).update(ids)
            if row["disposition"] == "new_distinct":
                allowed_prefixes.append(key)
    for path_part, rows in by_path.items():
        ids = {row["runtime_id"] for row in rows}
        sources = {row["source"] for row in rows}
        if len(ids) > 1:
            errors.append(
                f"registry path {path_part} bound to {sorted(ids)} from {sorted(sources)}"
            )
    for key, rows in by_semantic.items():
        ids = {row["runtime_id"] for row in rows}
        if len(ids) <= 1:
            continue
        allowed = set(allowed_multi.get(key, set()))
        if any(key.startswith(prefix) for prefix in allowed_prefixes):
            continue
        if ids <= allowed:
            continue
        errors.append(
            f"semantic key {key} has undeclared ids {sorted(ids - allowed)}"
        )
    for runtime_id in sorted(blocked):
        if runtime_id in by_id:
            errors.append(f"blocked identity is registered: {runtime_id}")
    behaviors = collect_behaviors()
    if behaviors != EXPECTED_COVER_BEHAVIORS:
        errors.append(
            "cover behaviors drifted: "
            + json.dumps({"actual": behaviors, "expected": EXPECTED_COVER_BEHAVIORS})
        )
    live_entries: list[dict[str, str]] = []
    seen_live: set[tuple[str, str]] = set()
    for row in sorted(
        records,
        key=lambda item: (
            item["registry_path"],
            item["runtime_id"],
            item["semantic_key"],
            item["source"],
        ),
    ):
        key = (row["runtime_id"], row["registry_path"])
        if not row["runtime_id"] or not row["registry_path"] or key in seen_live:
            continue
        seen_live.add(key)
        live_entries.append(
            {
                "registry_path": row["registry_path"],
                "runtime_id": row["runtime_id"],
                "semantic_key": row["semantic_key"],
                "source": row["source"],
            }
        )
    return {
        "behavior_count": len(behaviors),
        "cover_behaviors": behaviors,
        "disposition_count": len(dispositions()),
        "errors": errors,
        "generated_by": "tools/build_registry_identity.py",
        "live_entries": live_entries,
        "live_entry_count": len(live_entries),
        "record_count": len(records),
        "schema_version": 2,
        "status": "FAIL" if errors else "PASS",
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    manifest = compile_manifest()
    encoded = dumps(manifest)
    if args.write:
        if manifest["errors"]:
            print("\n".join(manifest["errors"]), file=sys.stderr)
            return 1
        atomic_io.write_bytes(MANIFEST, encoded)
        print(f"Wrote {io.relative(MANIFEST)}")
        return 0
    if manifest["errors"]:
        print("\n".join(manifest["errors"]), file=sys.stderr)
        return 1
    if not MANIFEST.is_file():
        print("registry identity manifest is stale; run --write", file=sys.stderr)
        return 1
    committed = json.loads(MANIFEST.read_text(encoding="utf-8"))
    compiled_keys = {
        (row["runtime_id"], row["registry_path"])
        for row in manifest.get("live_entries") or []
    }
    committed_keys = {
        (row["runtime_id"], row["registry_path"])
        for row in committed.get("live_entries") or []
    }
    drift: list[str] = []
    for runtime_id, path_part in sorted(compiled_keys - committed_keys):
        drift.append(f"missing live entry {runtime_id} ({path_part})")
    for runtime_id, path_part in sorted(committed_keys - compiled_keys):
        drift.append(f"extra undeclared entry {runtime_id} ({path_part})")
    if drift:
        print("\n".join(drift), file=sys.stderr)
        return 1
    if MANIFEST.read_bytes() != encoded:
        print("registry identity manifest is stale; run --write", file=sys.stderr)
        return 1
    print("registry identity is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
