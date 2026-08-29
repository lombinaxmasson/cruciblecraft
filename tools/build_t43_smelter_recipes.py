#!/usr/bin/env python3
"""Generate 407 T43 Smelter stone compact families and sidecar proofs."""
from __future__ import annotations

import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t43_common as common  # noqa: E402

SOURCE = common.SOURCE
OUTPUT_ROOT = common.GENERATED_ROOT
FIXTURE_ROOT = common.CATALOG_FIXTURE_ROOT
PLAYER_PATH = common.PLAYER_PATH
EQUIVALENCE = common.EQUIVALENCE
REQUIRED_FORMS = common.REQUIRED_FORMS


def stable(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def digest_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def family_filename(template_key: str) -> str:
    return template_key.replace(".", "_").replace("#", "_") + ".json"


def emit_action(action: dict[str, Any]) -> dict[str, Any]:
    kind = str(action["kind"]).lower()
    emitted: dict[str, Any] = {"kind": kind}
    if kind == "wear":
        emitted["damage"] = int(action.get("damage") or 0)
    return emitted


def emit_item(operand: dict[str, Any], *, consume: bool) -> dict[str, Any]:
    runtime_id = common.assert_runtime_id(operand.get("runtime_id"), consume=consume)
    return {"item": runtime_id}


def emit_item_output(operand: dict[str, Any]) -> dict[str, Any]:
    runtime_id = common.assert_runtime_id(operand.get("runtime_id"), consume=False)
    count = int((operand.get("source") or {}).get("count") or 1)
    return {"count": count, "id": runtime_id}


def emit_fluid(operand: dict[str, Any]) -> dict[str, Any]:
    runtime_id = common.assert_runtime_id(operand.get("runtime_id"), consume=False)
    amount = int((operand.get("source") or {}).get("amount") or 0)
    if amount <= 0:
        raise ValueError(f"T43 fluid amount must be positive: {operand}")
    return {"amount": amount, "id": runtime_id}


def emit_family(relation: dict[str, Any], lock_row: dict[str, Any]) -> dict[str, Any]:
    template_key = str(relation["template_key"])
    item_inputs = [emit_item(op, consume=True) for op in relation.get("item_inputs") or []]
    item_outputs = [emit_item_output(op) for op in relation.get("item_outputs") or []]
    fluid_inputs = [emit_fluid(op) for op in relation.get("fluid_inputs") or []]
    fluid_outputs = [emit_fluid(op) for op in relation.get("fluid_outputs") or []]
    kinds = set(relation.get("provenance", {}).get("kinds") or [])
    source_kind = (
        "DESIGN_POLICY"
        if "DESIGN_POLICY" in kinds
        else "SOURCE_DERIVED"
        if "SOURCE_DERIVED" in kinds
        else "SOURCE_BACKED"
    )
    stable_id = str(lock_row["stable_id"])
    return {
        "type": "cruciblecraft:compact_gt_recipe_family",
        "family_id": template_key,
        "target_map": common.TARGET_MAP,
        "source_revision": relation["source_revision"]
        if relation.get("source_revision")
        else common.SOURCE_REVISION,
        "publication_group": common.STONE_GROUP,
        "relations": [
            {
                "stable_id": stable_id,
                "item_inputs": item_inputs,
                "item_input_counts": [int(v) for v in relation["item_input_counts"]],
                "item_input_actions": [
                    emit_action(action) for action in relation["item_input_actions"]
                ],
                "item_outputs": item_outputs,
                "fluid_inputs": fluid_inputs,
                "fluid_outputs": fluid_outputs,
                "output_chances": [int(value) for value in relation.get("output_chances") or []],
                "duration": int(relation["duration"]),
                "eut": int(relation["eut"]),
                "special_value": int(relation["special_value"]),
                "can_be_buffered": bool(relation["can_be_buffered"]),
                "shadow_order": int(relation.get("shadow_order") or 0),
                "provenance": {
                    "source_kind": source_kind,
                    "selected_source_recipe": template_key,
                    "evidence_hashes": [relation["source_row_sha256"]]
                    if relation.get("source_row_sha256")
                    else list(
                        (relation.get("provenance") or {}).get("evidence_hashes") or []
                    ),
                },
            }
        ],
    }


def consume_identity(document: dict[str, Any]) -> str:
    relation = document["relations"][0]
    return json.dumps(
        {
            "item_inputs": relation["item_inputs"],
            "item_input_counts": relation["item_input_counts"],
            "item_input_actions": relation["item_input_actions"],
        },
        sort_keys=True,
    )


def player_path_document(families: list[dict[str, Any]]) -> dict[str, Any]:
    rows: list[dict[str, Any]] = []
    reachable = 0
    registered = 0
    for family in families:
        relation = family["relations"][0]
        consume_ids: list[str] = []
        preserve_ids: list[str] = []
        for ingredient, count, action in zip(
            relation["item_inputs"],
            relation["item_input_counts"],
            relation["item_input_actions"],
            strict=True,
        ):
            item_id = common.ingredient_identity(ingredient).removeprefix("item:")
            if str(action.get("kind")) == "consume" and int(count) > 0:
                consume_ids.append(item_id)
            else:
                preserve_ids.append(item_id)
        output_ids = [str(stack["id"]) for stack in relation["item_outputs"]]
        fluid_out = [str(stack["id"]) for stack in relation["fluid_outputs"]]
        fluid_in = [str(stack["id"]) for stack in relation["fluid_inputs"]]
        inputs_ok = all(
            item_id.startswith(("minecraft:", "cruciblecraft:")) for item_id in consume_ids
        )
        outputs_ok = all(
            item_id.startswith(("minecraft:", "cruciblecraft:"))
            for item_id in output_ids + fluid_out
        )
        if inputs_ok:
            reachable += 1
        if outputs_ok:
            registered += 1
        rows.append({
            "alias_fail_closed": False,
            "consume_ids": consume_ids,
            "fluid_input_ids": fluid_in,
            "fluid_output_ids": fluid_out,
            "inputs_reachable": inputs_ok,
            "output_ids": output_ids,
            "outputs_registered": outputs_ok,
            "preserve_ids": preserve_ids,
            "publication_group": family["publication_group"],
            "stable_id": relation["stable_id"],
            "template_key": family["family_id"],
        })
    return {
        "families": len(families),
        "frozen_review_unmodified": True,
        "inputs_reachable": reachable,
        "note": (
            "B0/B1 proof is tools/t43_layered_player_path.json. "
            "This sidecar records consume/preserve identities after mapping."
        ),
        "outputs_registered": registered,
        "relations": len(families),
        "rows": rows,
        "schema_version": 1,
        "status": "T43_PLAYER_PATH",
    }


def required_forms_document(source: dict[str, Any]) -> dict[str, Any]:
    return {
        "counts": {
            "dust_div72_materials": 0,
            "required_form_pairs": 0,
            "small_dust_materials": 0,
        },
        "dust_div72_source_metas": [],
        "note": (
            "T43 stone expressions are Block/Slab identities, not material forms. "
            "Recipe builder does not write material_registration_gate.json."
        ),
        "required_forms": {},
        "schema_version": 1,
        "source": {
            "path": common.relative(SOURCE),
            "sha256": t35.sha256_file(SOURCE),
        },
        "status": "T43_REQUIRED_FORMS",
    }


def write_tree(root: Path, files: dict[str, str]) -> None:
    if root.exists():
        for path in root.glob("*.json"):
            path.unlink()
    root.mkdir(parents=True, exist_ok=True)
    for name, content in files.items():
        (root / name).write_text(content, encoding="utf-8", newline="\n")


def planned_documents() -> dict[str, str]:
    source = common.load_json(SOURCE)
    lock = common.load_production_lock()
    lock_by_template = {
        str(row["template_key"]): row
        for row in (lock.get("production") or {}).get("families") or []
    }
    files: dict[str, str] = {}
    families: list[dict[str, Any]] = []
    identities: dict[str, str] = {}
    for relation in sorted(
        source.get("relations") or [], key=lambda row: str(row["template_key"])
    ):
        template_key = str(relation["template_key"])
        lock_row = lock_by_template.get(template_key)
        if lock_row is None:
            raise ValueError(f"T43 source row missing from production lock: {template_key}")
        document = emit_family(relation, lock_row)
        identity = consume_identity(document)
        previous = identities.get(identity)
        if previous is not None:
            raise ValueError(
                "Consume-side identity collision between "
                f"{previous} and {document['family_id']}"
            )
        identities[identity] = document["family_id"]
        files[family_filename(template_key)] = stable(document)
        families.append(document)
    if len(files) != common.PRODUCTION_FAMILY_COUNT:
        raise ValueError(
            f"T43 generated family count drifted: {len(files)} != "
            f"{common.PRODUCTION_FAMILY_COUNT}"
        )
    player = player_path_document(families)
    required = required_forms_document(source)
    generated_hashes = {
        name: digest_text(content) for name, content in sorted(files.items())
    }
    stable_ids = [
        json.loads(content)["relations"][0]["stable_id"]
        for name, content in sorted(files.items())
    ]
    equivalence = {
        "generated": {
            "file_count": len(files),
            "files": generated_hashes,
            "root": common.relative(OUTPUT_ROOT),
            "sha256": digest_text(stable(generated_hashes)),
        },
        "runtime_expected": {
            "logical_ids": len(stable_ids),
            "sha256": digest_text(stable(stable_ids)),
            "stable_ids": stable_ids,
        },
        "schema_version": 1,
        "source": {
            "family_count": len(families),
            "path": common.relative(SOURCE),
            "relation_count": len(families),
            "scope": "production",
            "sha256": t35.sha256_file(SOURCE),
            "source_revision": common.SOURCE_REVISION,
        },
        "status": "T43_SMELTER_EQUIVALENCE",
    }
    sidecars = {
        "equivalence": stable(equivalence),
        "player_path": stable(player),
        "required_forms": stable(required),
    }
    files["__sidecars__"] = json.dumps(sidecars)
    return files


def split_planned(files: dict[str, str]) -> tuple[dict[str, str], dict[str, str]]:
    sidecars = json.loads(files["__sidecars__"])
    recipes = {name: content for name, content in files.items() if name != "__sidecars__"}
    sidecar_files = {
        PLAYER_PATH.name: sidecars["player_path"],
        EQUIVALENCE.name: sidecars["equivalence"],
        REQUIRED_FORMS.name: sidecars["required_forms"],
    }
    return recipes, sidecar_files


def build() -> dict[str, Any]:
    planned = planned_documents()
    recipes, sidecars = split_planned(planned)
    fixture = dict(recipes)
    player = json.loads(sidecars[PLAYER_PATH.name])
    if player["inputs_reachable"] != common.PRODUCTION_FAMILY_COUNT:
        raise ValueError("T43 production consume identities are incomplete")
    if player["outputs_registered"] != common.PRODUCTION_FAMILY_COUNT:
        raise ValueError("T43 production outputs are not registered identities")
    return {
        "equivalence": json.loads(sidecars[EQUIVALENCE.name]),
        "family_count": len(recipes),
        "fixture_count": len(fixture),
        "recipes": recipes,
        "fixture": fixture,
        "sidecars": sidecars,
    }


def write() -> dict[str, Any]:
    document = build()
    write_tree(OUTPUT_ROOT, document["recipes"])
    write_tree(FIXTURE_ROOT, document["fixture"])
    for name, content in document["sidecars"].items():
        {
            PLAYER_PATH.name: PLAYER_PATH,
            EQUIVALENCE.name: EQUIVALENCE,
            REQUIRED_FORMS.name: REQUIRED_FORMS,
        }[name].write_text(content, encoding="utf-8", newline="\n")
    return document


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Generate T43 Smelter compact families", argv)
    if common.handle_rebind(args, EQUIVALENCE):
        return 0
    try:
        if args.check:
            document = build()
            errors = []
            if document["family_count"] != common.PRODUCTION_FAMILY_COUNT:
                errors.append("T43 generated family count drifted")
            if document["fixture_count"] != common.CATALOG_FAMILY_COUNT:
                errors.append("T43 catalog fixture count drifted")
            generated = {
                path.name: path.read_text(encoding="utf-8")
                for path in common.generated_family_files()
            }
            if generated != document["recipes"]:
                errors.append("T43 generated recipe tree drifted")
            fixture = {
                path.name: path.read_text(encoding="utf-8")
                for path in common.generated_family_files("catalog")
            }
            if fixture != document["fixture"]:
                errors.append("T43 catalog fixture tree drifted")
            for name, content in document["sidecars"].items():
                path = {
                    PLAYER_PATH.name: PLAYER_PATH,
                    EQUIVALENCE.name: EQUIVALENCE,
                    REQUIRED_FORMS.name: REQUIRED_FORMS,
                }[name]
                errors.extend(common.check_document(path, json.loads(content)))
            if errors:
                print("T43 generated recipes are stale:")
                for error in errors:
                    print(f"- {error}")
                return 1
            print("T43 generated recipes are current.")
            return 0
        document = write()
        print(
            f"Wrote {document['family_count']} T43 Smelter compact families "
            f"and {document['fixture_count']} catalog fixtures."
        )
        return 0
    except ValueError as failure:
        print(str(failure), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
