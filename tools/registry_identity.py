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
from tools import t35_common as t35

MANIFEST = t35.TOOLS / "registry_identity_manifest.json"
REGISTER_RE = re.compile(
    r'(?:ITEMS\.register(?:SimpleBlockItem)?|ITEMS\.register)\(\s*"([^"]+)"'
)
COVER_FILES = (
    ROOT / "src/main/resources/data/cruciblecraft/cover_definitions.json",
    ROOT
    / "src/main/resources/data/cruciblecraft/item_network_cover_definitions.json",
    ROOT
    / "src/main/resources/data/cruciblecraft/fluid_network_cover_definitions.json",
)
BATH = ROOT / "src/main/resources/data/cruciblecraft/bath_identity_catalog.json"
SEMANTIC = (
    ROOT / "src/main/resources/data/cruciblecraft/semantic_object_catalog.json"
)
MOD_ITEMS = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/registry/ModItems.java"
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
    "cruciblecraft:logistics_fluid_storage",
    "cruciblecraft:logistics_fluid_transfer",
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
        document = t35.load_json(path)
        rel = t35.relative(path)
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
    document = t35.load_json(path)
    rel = t35.relative(path)
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
    rel = t35.relative(MOD_ITEMS)
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
    collect_mod_items(records)
    errors: list[str] = []
    by_path: dict[str, list[dict[str, str]]] = {}
    by_id: dict[str, list[dict[str, str]]] = {}
    by_semantic: dict[str, list[dict[str, str]]] = {}
    for row in records:
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
    return {
        "behavior_count": len(behaviors),
        "cover_behaviors": behaviors,
        "disposition_count": len(dispositions()),
        "errors": errors,
        "generated_by": "tools/build_registry_identity.py",
        "record_count": len(records),
        "schema_version": 1,
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
        print(f"Wrote {t35.relative(MANIFEST)}")
        return 0
    if manifest["errors"]:
        print("\n".join(manifest["errors"]), file=sys.stderr)
        return 1
    if not MANIFEST.is_file() or MANIFEST.read_bytes() != encoded:
        print("registry identity manifest is stale; run --write", file=sys.stderr)
        return 1
    print("registry identity is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
