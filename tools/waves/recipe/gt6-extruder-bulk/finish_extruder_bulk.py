#!/usr/bin/env python3
"""Prove extruder Rule IR and freeze the production lock."""
from __future__ import annotations

import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
TOOLS = ROOT / "tools"
sys.path.insert(0, str(ROOT))
sys.path.insert(1, str(TOOLS))

from tools import census_common as census
from tools import centrifuge_common as centrifuge
from tools.recipe_bulk.compile import _consume_identity
from tools.recipe_bulk.emit import emit_action, emit_item, emit_item_output
from tools.recipe_bulk.handlers import shape_transform
from tools.recipe_bulk.pilot import reviewed_lock
from tools.recipe_bulk.rule_ir import prove_coverage

WAVE = ROOT / "tools" / "waves" / "recipe" / "gt6-extruder-bulk"
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
PUBLICATION_GROUP = "cruciblecraft:extruder/bulk"
NOTE = (
    "live compile for recipe/gt6-extruder-bulk; translated gt.recipe.extruder rows only; "
    "missing forms and objects stay blocked; on-demand matrix publication; not player_complete"
)


def _runtime(operand: dict[str, Any]) -> str:
    return str(operand.get("runtime_id") or operand.get("value") or "")


HOST_ITEM_OUTPUTS = 2
HOST_MAX_EUT = 256
SHAPE_TAG = (
    ROOT / "src/generated/resources/data/cruciblecraft/tags/item/extruder_shapes.json"
)
HOST_SHAPES = frozenset(
    str(value) for value in json.loads(SHAPE_TAG.read_text(encoding="utf-8"))["values"]
)


def _uses_lossy_alias(relation: dict[str, Any]) -> bool:
    for operand in list(relation.get("item_inputs") or []) + list(
        relation.get("item_outputs") or []
    ):
        if not operand.get("alias"):
            continue
        source = operand.get("source") or {}
        meta = str(source.get("meta") or 0)
        if not meta.isdigit():
            continue
        key = (str(source.get("item") or ""), int(meta))
        if key in centrifuge.FIXTURE_ONLY_LOSSY_ITEM_ALIASES:
            return True
    return False


def _host_rejection(relation: dict[str, Any]) -> str | None:
    """Mirror ModProcessingMachines.validateComponentRecipe for the extruder host."""
    inputs = list(relation.get("item_inputs") or [])
    outputs = list(relation.get("item_outputs") or [])
    actions = list(relation.get("item_input_actions") or [])
    counts = list(relation.get("item_input_counts") or [])
    if relation.get("fluid_inputs") or relation.get("fluid_outputs"):
        return "extruder host rejected fluid"
    if not outputs or len(outputs) > HOST_ITEM_OUTPUTS:
        return "extruder host rejected output count"
    if len(inputs) != 2 or len(actions) != 2 or len(counts) != 2:
        return "extruder host rejected input count"
    if str(actions[0].get("kind") or "") != "CONSUME":
        return "extruder host rejected material slot"
    if str(actions[1].get("kind") or "") != "PRESERVE" or int(counts[1]) != 0:
        return "extruder host rejected mold slot"
    if _runtime(inputs[1]) not in HOST_SHAPES:
        return "extruder host rejected mold slot"
    if int(counts[0]) <= 0 or int(counts[0]) > 64:
        return "extruder host rejected input amount"
    eut = int(relation.get("eut") or 0)
    if eut <= 0 or eut > HOST_MAX_EUT:
        return "extruder host rejected energy"
    for output in outputs:
        count = int((output.get("source") or {}).get("count") or 1)
        if count <= 0 or count > 64:
            return "extruder host rejected output amount"
    return None


def _miss_reason(relation: dict[str, Any]) -> str:
    if relation.get("fluid_inputs") or relation.get("fluid_outputs"):
        return "fluid"
    inputs = list(relation.get("item_inputs") or [])
    actions = list(relation.get("item_input_actions") or [])
    outputs = list(relation.get("item_outputs") or [])
    if len(inputs) != 2 or len(actions) != 2 or not outputs:
        return "arity"
    consumed = [
        item
        for item, action in zip(inputs, actions)
        if str(action.get("kind") or "") == "CONSUME"
    ]
    preserved = [
        item
        for item, action in zip(inputs, actions)
        if str(action.get("kind") or "") == "PRESERVE"
    ]
    if len(consumed) != 1 or len(preserved) != 1:
        return "actions"
    if not consumed[0].get("material") or not consumed[0].get("form"):
        return "consume_not_form"
    shape = _runtime(preserved[0])
    if not shape.startswith(shape_transform.SHAPE_PREFIX):
        return "shape"
    material = consumed[0].get("material")
    from_form = consumed[0].get("form")
    for output in outputs:
        if output.get("material") != material or not output.get("form"):
            return "output_not_same_form"
        if output.get("form") == from_form:
            return "output_same_form"
    return "other"


def _io_key(relation: dict[str, Any]) -> str:
    return json.dumps(
        {
            "actions": [
                str(action.get("kind") or "")
                for action in relation.get("item_input_actions") or []
            ],
            "counts": list(relation.get("item_input_counts") or []),
            "fluids_in": [
                _runtime(item) for item in relation.get("fluid_inputs") or []
            ],
            "fluids_out": [
                _runtime(item) for item in relation.get("fluid_outputs") or []
            ],
            "inputs": [_runtime(item) for item in relation.get("item_inputs") or []],
            "outputs": [_runtime(item) for item in relation.get("item_outputs") or []],
        },
        separators=(",", ":"),
    )


def _prune_work_set(kept: set[str]) -> None:
    """Keep work_set membership equal to the published rows so import-source --check replays."""
    work_path = WAVE / "source_pack" / "work_set.json"
    work = json.loads(work_path.read_text(encoding="utf-8"))
    families = []
    seen: set[str] = set()
    for family in work["families"]:
        members = []
        for member in family["relations"]:
            digest = str(member["source_row_sha256"])
            if digest in kept and digest not in seen:
                seen.add(digest)
                members.append(dict(member, shadow_order=len(members)))
        if members:
            families.append(dict(family, relations=members))
    work["families"] = families
    work["accounting"]["published_rows"] = len(seen)
    work_path.write_text(json.dumps(work, separators=(",", ":")), encoding="utf-8")
    manifest_path = WAVE / "source_pack_manifest.json"
    manifest = census.load_json(manifest_path)
    for entry in manifest["files"]:
        if entry["role"] == "work_set":
            entry["sha256"] = census.sha256_file(work_path)
    census.write_stable(manifest_path, manifest)


def main() -> None:
    source = json.loads((WAVE / "source.json").read_text(encoding="utf-8"))
    raw = list(source["relations"])
    relations: list[dict[str, Any]] = []
    seen_digest: set[str] = set()
    dropped = 0
    for relation in raw:
        digest = str(relation.get("source_row_sha256") or "")
        if digest in seen_digest:
            dropped += 1
            continue
        seen_digest.add(digest)
        relations.append(relation)
    kept: list[dict[str, Any]] = []
    emitted_seen: dict[str, str] = {}
    emitted_dropped: list[dict[str, str]] = []
    for relation in relations:
        emitted = {
            "duration": int(relation.get("duration") or 0),
            "eut": int(relation.get("eut") or 0),
            "special_value": int(relation.get("special_value") or 0),
            "fluid_inputs": list(relation.get("fluid_inputs") or []),
            "fluid_outputs": list(relation.get("fluid_outputs") or []),
            "item_input_actions": [
                emit_action(action)
                for action in relation.get("item_input_actions") or []
            ],
            "item_input_counts": list(relation.get("item_input_counts") or []),
            "item_inputs": [emit_item(op) for op in relation.get("item_inputs") or []],
            "item_outputs": [
                emit_item_output(op) for op in relation.get("item_outputs") or []
            ],
        }
        key = _consume_identity(emitted, wave_id="recipe/gt6-extruder-bulk")
        if key in emitted_seen:
            emitted_dropped.append(
                {
                    "reason": "duplicate runtime identity",
                    "source_recipe_index": str(relation.get("source_recipe_index") or ""),
                    "source_row_sha256": str(relation.get("source_row_sha256") or ""),
                }
            )
            continue
        emitted_seen[key] = str(relation.get("stable_id") or "")
        kept.append(relation)
    relations = kept
    host_kept: list[dict[str, Any]] = []
    host_dropped: list[dict[str, str]] = []
    host_reasons: Counter[str] = Counter()
    for relation in relations:
        rejection = (
            "lossy fixture alias is not a GT6 operand"
            if _uses_lossy_alias(relation)
            else _host_rejection(relation)
        )
        if rejection is None:
            host_kept.append(relation)
            continue
        host_reasons[rejection] += 1
        host_dropped.append(
            {
                "reason": rejection,
                "source_recipe_index": str(relation.get("source_recipe_index") or ""),
                "source_row_sha256": str(relation.get("source_row_sha256") or ""),
            }
        )
    relations = host_kept
    if dropped or emitted_dropped or host_dropped:
        source["relations"] = relations
        (WAVE / "source.json").write_text(
            json.dumps(source, separators=(",", ":")),
            encoding="utf-8",
        )
    _prune_work_set({str(row["source_row_sha256"]) for row in relations})
    reasons: Counter[str] = Counter()
    remainder: list[str] = []
    shaped = 0
    io_seen: dict[str, str] = {}
    collisions = 0
    input_pairs: Counter[str] = Counter()
    for relation in relations:
        if shape_transform.matches(relation, {}):
            shaped += 1
        else:
            reasons[_miss_reason(relation)] += 1
            remainder.append(str(relation["source_row_sha256"]))
        key = _io_key(relation)
        previous = io_seen.get(key)
        current = str(relation.get("stable_id") or "")
        if previous and previous != current:
            collisions += 1
        else:
            io_seen[key] = current
        inputs = tuple(
            sorted(_runtime(item) for item in relation.get("item_inputs") or [])
        )
        input_pairs["|".join(inputs)] += 1
    document = {
        "blocked": [],
        "family_id": "gt.recipe.extruder#bulk",
        "handlers": [
            {
                "handler_id": "extruder_shape_transform",
                "kind": "shape_transform",
                "template": {},
            },
            {
                "covered_source_row_sha256": remainder,
                "handler_id": "extruder_exact_remainder",
                "kind": "exact_remainder",
            },
        ],
        "schema": "rule_ir_v1",
        "schema_version": 1,
        "source_map": "gt.recipe.extruder",
        "source_revision": SOURCE_REVISION,
        "target_map": "cruciblecraft:extruder",
    }
    proof = prove_coverage(document, relations)
    census.write_stable(WAVE / "rule_ir.json", document)
    candidate = census.load_json(WAVE / "lock_candidate.json")
    lock = reviewed_lock(
        candidate,
        publication_group=PUBLICATION_GROUP,
        cohort="bulk",
    )
    lock["note"] = NOTE
    lock["generated_by"] = "tools/waves/recipe/gt6-extruder-bulk/finish_extruder_bulk.py"
    kept_ids = {str(row.get("stable_id") or "") for row in relations}
    dropped_hashes = {
        item["source_row_sha256"] for item in emitted_dropped
    } | {item["source_row_sha256"] for item in host_dropped}
    families = []
    for row in lock["production"]["families"]:
        stable_ids = [sid for sid in row.get("stable_ids") or [] if sid in kept_ids]
        if not stable_ids:
            continue
        row = dict(row)
        row["stable_ids"] = stable_ids
        row["relation_count"] = len(stable_ids)
        row["source_row_sha256"] = [
            digest
            for digest in row.get("source_row_sha256") or []
            if digest in seen_digest and digest not in dropped_hashes
        ]
        families.append(row)
    lock["production"]["families"] = families
    lock["production"]["family_count"] = len(families)
    lock["production"]["relation_count"] = sum(int(row["relation_count"]) for row in families)
    census.write_stable(WAVE / "production_lock.json", lock)
    blocked = census.load_json(WAVE / "blocked.json")
    published_hashes = {str(row.get("source_row_sha256") or "") for row in relations}
    rows = [
        row
        for row in blocked.get("rows") or []
        if str(row.get("source_row_sha256") or "") not in published_hashes
    ]
    known = {str(row.get("source_row_sha256") or "") for row in rows}
    for item in emitted_dropped + host_dropped:
        if item["source_row_sha256"] not in known:
            rows.append(item)
            known.add(item["source_row_sha256"])
    blocked["rows"] = rows
    census.write_stable(WAVE / "blocked.json", blocked)
    ledger_reasons = Counter(str(row.get("reason") or "") for row in rows)
    host_reasons = Counter(
        {
            reason: count
            for reason, count in ledger_reasons.items()
            if reason.startswith("extruder host rejected")
            or reason == "lossy fixture alias is not a GT6 operand"
        }
    )
    census.write_stable(
        WAVE / "coverage_proof.json",
        {
            "blocked_rows": len(blocked.get("rows") or []),
            "input_pair_max": max(input_pairs.values()),
            "duplicate_runtime_rows": ledger_reasons["duplicate runtime identity"],
            "host_rejected_rows": sum(host_reasons.values()),
            "host_rejection_reasons": dict(sorted(host_reasons.items())),
            "miss_reasons": dict(sorted(reasons.items())),
            "proof": {key: value for key, value in proof.items() if key != "row_fingerprints"},
            "published_rows": len(relations),
            "schema_version": 1,
            "shape_transform_rows": shaped,
            "source_rows": int(blocked.get("source_rows") or 0),
        },
    )
    print(
        f"shaped {shaped} remainder {len(remainder)} dropped {dropped} "
        f"duplicate_runtime {len(emitted_dropped)} host_rejected {len(host_dropped)} "
        f"collisions {collisions} pair_max {max(input_pairs.values())} "
        f"reasons {dict(reasons)} host_reasons {dict(host_reasons)}"
    )


if __name__ == "__main__":
    main()
