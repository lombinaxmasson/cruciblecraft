#!/usr/bin/env python3
"""Emit T49 required-forms. Tiny washed is already registered; write no new prefixes."""
from __future__ import annotations

import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import build_t37_assembler_source as t37
from tools import t35_common as t35
from tools import t48_identities as identities
from tools import wave_bath_tiny_purified as common

OUTPUT = common.REQUIRED_FORMS


def _form_for(item_id: str, catalogs: t37.Catalogs) -> str | None:
    return identities.overlay_prefix_form(item_id) or catalogs.prefix_item_to_form.get(
        item_id
    )


def build() -> dict[str, Any]:
    catalogs = t37.load_catalogs()
    source = common.load_json(common.SOURCE)
    required: dict[str, set[str]] = defaultdict(set)
    missing_prefix: dict[str, int] = {}
    for relation in source.get("relations") or []:
        for operand in list(relation.get("item_inputs") or []) + list(
            relation.get("item_outputs") or []
        ):
            source_row = operand.get("source") or {}
            item = str(source_row.get("item") or "")
            if identities.classify_source_item(item) not in {"gt_prefix", "other"}:
                continue
            form = _form_for(item, catalogs)
            if not form:
                missing_prefix[item] = missing_prefix.get(item, 0) + 1
                continue
            meta = source_row.get("meta")
            if not isinstance(meta, int):
                continue
            material = catalogs.material_id_to_cc.get(meta)
            if not material:
                continue
            registered = form in catalogs.registered_forms.get(material, set())
            if not registered:
                required[material].add(form)
    frozen = {
        material: sorted(forms) for material, forms in sorted(required.items())
    }
    pair_count = sum(len(forms) for forms in frozen.values())
    if pair_count != 0 or missing_prefix:
        raise ValueError(
            "T49 required forms must be empty: tiny_washed_crushed_ore is already registered"
        )
    return {
        "counts": {
            "dust_div72_materials": 0,
            "new_prefix_forms": 0,
            "required_form_pairs": 0,
            "required_materials": 0,
            "small_dust_materials": 0,
        },
        "dust_div72_source_metas": [],
        "new_prefix_forms": [],
        "note": (
            "Scanned the T49 remainder slice. tiny_washed_crushed_ore was registered "
            "in T48. Recipe builder does not write material_registration_gate.json."
        ),
        "required_forms": {},
        "schema_version": 1,
        "source": {
            "missing_prefix_items": {},
            "path": common.relative(common.SOURCE),
            "sha256": t35.sha256_file(common.SOURCE),
        },
        "status": "T49_REQUIRED_FORMS_FROZEN",
    }


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write the T49 required-forms sidecar",
        OUTPUT,
        build=build,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
