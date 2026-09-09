#!/usr/bin/python3
"""Check machine delivery sidecar against kind/tier catalogs."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import io_common as files
from tools.recipe_bulk.schema_lite import SchemaError, validate

ROOT = files.ROOT
DELIVERY = (
    ROOT / "src/main/resources/data/cruciblecraft/machine_delivery.json"
)
SCHEMA = (
    ROOT
    / "src/main/resources/data/cruciblecraft/schema/machine_delivery.schema.json"
)
KINDS = ROOT / "src/main/resources/data/cruciblecraft/machine_kinds.json"
TIERS = ROOT / "src/main/resources/data/cruciblecraft/machine_tiers.json"


def _load(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def check() -> list[str]:
    errors: list[str] = []
    delivery = _load(DELIVERY)
    schema = _load(SCHEMA)
    try:
        validate(delivery, schema)
    except SchemaError as error:
        errors.append(str(error))
        return errors
    kinds = {
        str(row["id"])
        for row in (_load(KINDS).get("kinds") or [])
    }
    variant_rows = (
        (_load(TIERS).get("source") or {}).get("variant_rows") or {}
    )
    delivery_kinds = {
        str(row["id"])
        for row in delivery.get("hosts") or []
        if row.get("kind_catalog")
    }
    extra_hosts = {
        str(row["id"])
        for row in delivery.get("hosts") or []
        if not row.get("kind_catalog")
    }
    missing_kinds = sorted(kinds - delivery_kinds)
    extra_kinds = sorted(delivery_kinds - kinds)
    if missing_kinds:
        errors.append("delivery missing kind ids: " + ",".join(missing_kinds))
    if extra_kinds:
        errors.append("delivery kind_catalog hosts not in machine_kinds: "
                      + ",".join(extra_kinds))
    if "cruciblecraft:laser_engraver" not in extra_hosts:
        errors.append("laser_engraver must remain a delivery-only host")
    seen_ids: set[str] = set()
    for row in delivery.get("hosts") or []:
        host_id = str(row["id"])
        if host_id in seen_ids:
            errors.append(f"duplicate delivery host {host_id}")
        seen_ids.add(host_id)
        if row.get("kind_catalog"):
            source = str(row.get("gt6_source") or "")
            variant = variant_rows.get(host_id)
            if host_id == "cruciblecraft:roaster":
                variant = variant or variant_rows.get("cruciblecraft:steel_roaster")
            if variant and source != variant:
                errors.append(
                    f"{host_id} gt6_source {source} != machine_tiers {variant}"
                )
        dest = str(row["art_destination"])
        profile = str(row["texture_profile"])
        expected = f"textures/block/machine/{profile}"
        if dest != expected:
            errors.append(
                f"{host_id} art_destination {dest} != {expected}"
            )
        if "lang_key" in row or "acquisition_template" in row:
            errors.append(f"{host_id} must not duplicate kind catalog fields")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Machine delivery sidecar")
    parser.add_argument("--check", action="store_true", required=True)
    parser.parse_args(argv)
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("machine delivery sidecar is consistent")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
