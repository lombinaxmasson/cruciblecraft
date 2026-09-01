#!/usr/bin/env python3
"""Emit T48 required-forms and new material-prefix JSON. Does not write the gate."""
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
from tools import t48_common as common
from tools import t48_identities as identities

OUTPUT = common.REQUIRED_FORMS
PREFIX_ROOT = (
    ROOT / "src/main/resources/data/cruciblecraft/material_prefixes"
)
PREFIX_INDEX = PREFIX_ROOT / "index.json"


def _form_for(item_id: str, catalogs: t37.Catalogs) -> str | None:
    return identities.overlay_prefix_form(item_id) or catalogs.prefix_item_to_form.get(
        item_id
    )


def _scan_pairs() -> tuple[dict[str, set[str]], dict[str, Any]]:
    catalogs = t37.load_catalogs()
    relations = identities.load_t47_work_set_relations()
    required: dict[str, set[str]] = defaultdict(set)
    missing_prefix: dict[str, int] = {}
    for relation in relations:
        for operand in list(relation.get("item_inputs") or []) + list(
            relation.get("item_outputs") or []
        ):
            source_row = operand.get("source") or {}
            item = str(source_row.get("item") or "")
            if identities.classify_source_item(item) not in {"gt_prefix", "other"}:
                continue
            if identities.is_tool_head_item(item) or identities.is_multiitem_item(item):
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
    source_path = common.t47.SOURCE
    return dict(required), {
        "missing_prefix_items": dict(sorted(missing_prefix.items())),
        "path": common.relative(source_path),
        "sha256": t35.sha256_file(source_path),
    }


def build() -> dict[str, Any]:
    required, source = _scan_pairs()
    frozen = {
        material: sorted(forms) for material, forms in sorted(required.items())
    }
    pair_count = sum(len(forms) for forms in frozen.values())
    new_prefixes = sorted(
        {
            form
            for forms in frozen.values()
            for form in forms
            if form not in identities.EXISTING_PREFIX_FORMS
        }
    )
    return {
        "counts": {
            "dust_div72_materials": 0,
            "new_prefix_forms": len(new_prefixes),
            "required_form_pairs": pair_count,
            "required_materials": len(frozen),
            "small_dust_materials": 0,
        },
        "dust_div72_source_metas": [],
        "new_prefix_forms": new_prefixes,
        "note": (
            "Scanned T47 compact source filtered by the T48 work set. "
            "Recipe builder does not write material_registration_gate.json."
        ),
        "required_forms": frozen,
        "schema_version": 1,
        "source": source,
        "status": "T48_REQUIRED_FORMS_FROZEN",
    }


def _write_prefix_json(document: dict[str, Any]) -> None:
    PREFIX_ROOT.mkdir(parents=True, exist_ok=True)
    index = list(common.load_json(PREFIX_INDEX)) if PREFIX_INDEX.is_file() else []
    known = set(index)
    for form in document.get("new_prefix_forms") or []:
        filename = f"{form}.json"
        path = PREFIX_ROOT / filename
        t35.write_stable(path, identities.prefix_document(form))
        if filename not in known:
            index.append(filename)
            known.add(filename)
    t35.write_stable(PREFIX_INDEX, index)


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    _write_prefix_json(document)
    return document


def main(argv: list[str] | None = None) -> int:
    return common.run_managed(
        "Write the T48 required-forms sidecar and new prefixes",
        OUTPUT,
        build=build,
        write=write,
        argv=argv,
    )


if __name__ == "__main__":
    raise SystemExit(main())
