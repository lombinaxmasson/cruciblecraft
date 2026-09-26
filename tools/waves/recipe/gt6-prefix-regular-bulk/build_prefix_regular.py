#!/usr/bin/env python3
"""Import the twelve GT6 prefix-regular maps as on-demand Rule IR families."""
from __future__ import annotations

import hashlib
import json
import shutil
import sys
import zipfile
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
TOOLS = ROOT / "tools"
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
if str(TOOLS) not in sys.path:
    sys.path.insert(1, str(TOOLS))

from tools import census_common as census
from tools import centrifuge_common as centrifuge
from tools.build_assembler_source import _source_row_hash
from tools.recipe_bulk import source_import
from tools.recipe_bulk.compile import _consume_identity
from tools.recipe_bulk.dialects import gt6
from tools.recipe_bulk.emit import emit_action, emit_fluid, emit_item, emit_item_output
from tools.recipe_bulk.handlers import prefix_transform
from tools.recipe_bulk.membership import membership_root
from tools.recipe_bulk.pilot import reviewed_lock
from tools.recipe_bulk.rule_ir import prove_coverage
from tools.bath_remainder_identities import VANILLA_COLOR_META_ITEMS
from tools.waves.prep.machine_prep_common import (
    missing_runtime_item_operands,
    registered_runtime_ids,
    ungated_material_form_operands,
)

SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
WAVE = ROOT / "tools" / "waves" / "recipe" / "gt6-prefix-regular-bulk"
DUMP_ROOT = ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps"
HOLDER = 2048
SAMPLES = ROOT / "src" / "test" / "resources" / "gt6_prefix_regular_samples.json"

# Host ceilings mirror the Java validators that GTRecipeMapLoader runs at load.
MAPS: tuple[dict[str, Any], ...] = (
    {
        "dump": "gt.recipe.cutter",
        "fluid_in": 1,
        "kind": "component",
        "machine": "cutter",
        "max_eut": 256,
        "max_in": 1,
        "max_out": 3,
        "press": False,
        "slug": "cutter/prefix-regular",
        "target": "cruciblecraft:cutter",
    },
    {
        "dump": "gt.recipe.mortar",
        "fluid_in": 0,
        "kind": "mortar",
        "machine": "mortar",
        "max_eut": 1024,
        "max_in": 1,
        "max_out": 4,
        "press": False,
        "slug": "mortar/prefix-regular",
        "target": "cruciblecraft:mortar",
    },
    {
        "dump": "gt.recipe.welder",
        "fluid_in": 1,
        "kind": "welder",
        "machine": "welder",
        "max_eut": 16384,
        "max_in": 9,
        "max_out": 1,
        "press": False,
        "slug": "welder/prefix-regular",
        "target": "cruciblecraft:welder",
    },
    {
        "dump": "gt.recipe.lathe",
        "fluid_in": 0,
        "kind": "component",
        "machine": "lathe",
        "max_eut": 256,
        "max_in": 1,
        "max_out": 2,
        "press": False,
        "slug": "lathe/prefix-regular",
        "target": "cruciblecraft:lathe",
    },
    {
        "dump": "gt.recipe.rollingmill",
        "fluid_in": 0,
        "kind": "component",
        "machine": "rollingmill",
        "max_eut": 256,
        "max_in": 1,
        "max_out": 1,
        "press": False,
        "slug": "rollingmill/prefix-regular",
        "target": "cruciblecraft:rollingmill",
    },
    {
        "dump": "gt.recipe.press",
        "fluid_in": 0,
        "kind": "component",
        "machine": "press",
        "max_eut": 256,
        "max_in": 3,
        "max_out": 1,
        "press": True,
        "slug": "press/prefix-regular",
        "target": "cruciblecraft:press",
    },
    {
        "dump": "gt.recipe.crusher",
        "fluid_in": 0,
        "kind": "crusher",
        "machine": "crusher",
        "max_eut": 1024,
        "max_in": 1,
        "max_out": 12,
        "press": False,
        "slug": "crusher/prefix-regular",
        "target": "cruciblecraft:crusher",
    },
    {
        "dump": "gt.recipe.anvil.bend.big",
        "fluid_in": 0,
        "kind": "anvil",
        "machine": "anvil_bend_big",
        "max_eut": None,
        "max_in": 2,
        "max_out": 2,
        "press": False,
        "slug": "anvil/bend-big",
        "target": "cruciblecraft:anvil_bend_big",
    },
    {
        "dump": "gt.recipe.anvil.bend.small",
        "fluid_in": 0,
        "kind": "anvil",
        "machine": "anvil_bend_small",
        "max_eut": None,
        "max_in": 2,
        "max_out": 2,
        "press": False,
        "slug": "anvil/bend-small",
        "target": "cruciblecraft:anvil_bend_small",
    },
    {
        "dump": "gt.recipe.rollbender",
        "fluid_in": 0,
        "kind": "component",
        "machine": "rollbender",
        "max_eut": 256,
        "max_in": 1,
        "max_out": 1,
        "press": False,
        "slug": "rollbender/prefix-regular",
        "target": "cruciblecraft:rollbender",
    },
    {
        "dump": "gt.recipe.wiremill",
        "fluid_in": 0,
        "kind": "component",
        "machine": "wiremill",
        "max_eut": 256,
        "max_in": 2,
        "max_out": 1,
        "press": False,
        "slug": "wiremill/prefix-regular",
        "target": "cruciblecraft:wiremill",
    },
    {
        "dump": "gt.recipe.sifter",
        "fluid_in": 0,
        "kind": "sifter",
        "machine": "sifter",
        "max_eut": 1024,
        "max_in": 1,
        "max_out": 12,
        "press": False,
        "slug": "sifter/prefix-regular",
        "target": "cruciblecraft:sifter",
    },
)


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _runtime(operand: dict[str, Any]) -> str:
    return str(operand.get("runtime_id") or operand.get("value") or "")


_VANILLA_ITEM_MODELS: set[str] | None = None


def _vanilla_item_models() -> set[str] | None:
    """1.21.1 item ids from the local client-extra jar, when the artifact exists."""
    global _VANILLA_ITEM_MODELS
    if _VANILLA_ITEM_MODELS is not None:
        return _VANILLA_ITEM_MODELS or None
    artifacts = ROOT / "build" / "moddev" / "artifacts"
    jars = sorted(artifacts.glob("neoforge-*-client-extra-*.jar"))
    if not jars:
        _VANILLA_ITEM_MODELS = set()
        return None
    prefix = "assets/minecraft/models/item/"
    names: set[str] = set()
    with zipfile.ZipFile(jars[-1]) as archive:
        for name in archive.namelist():
            if name.startswith(prefix) and name.endswith(".json"):
                names.add("minecraft:" + name[len(prefix) : -5])
    _VANILLA_ITEM_MODELS = names
    return names or None


def _unknown_vanilla_item(relation: dict[str, Any]) -> str:
    models = _vanilla_item_models()
    if not models:
        return ""
    for key in ("item_inputs", "item_outputs"):
        for operand in relation.get(key) or []:
            runtime = _runtime(operand)
            if runtime.startswith("minecraft:") and runtime not in models:
                return runtime
    return ""


def _fluid_amount(operand: dict[str, Any]) -> int:
    source = operand.get("source") or {}
    if "amount" in source:
        return int(source["amount"])
    if "amount" in operand:
        return int(operand["amount"])
    return 0


def _output_count(operand: dict[str, Any]) -> int:
    source = operand.get("source") or {}
    if "count" in source:
        return int(source["count"])
    if "count" in operand:
        return int(operand["count"])
    return 1


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


def _normalized_chances(relation: dict[str, Any]) -> list[int]:
    """Match emit.py: pad short chance lists and drop empty-slot padding."""
    outputs = list(relation.get("item_outputs") or [])
    chances = [int(value) for value in (relation.get("output_chances") or [])]
    if not outputs:
        return chances
    if len(chances) < len(outputs):
        return chances + [10000] * (len(outputs) - len(chances))
    if len(chances) > len(outputs):
        return chances[: len(outputs)]
    return chances


def _chance_problem(relation: dict[str, Any], *, allow_zero: bool) -> str | None:
    floor = 0 if allow_zero else 1
    if any(chance < floor or chance > 10000 for chance in _normalized_chances(relation)):
        return "chance"
    return None


def _amount_problem(relation: dict[str, Any], spec: dict[str, Any]) -> str | None:
    actions = list(relation.get("item_input_actions") or [])
    counts = [int(value) for value in (relation.get("item_input_counts") or [])]
    if len(actions) != len(counts):
        return "input amount"
    for action, count in zip(actions, counts, strict=False):
        kind = str(action.get("kind") or "").upper()
        if kind == "WEAR":
            return "wear"
        if kind == "PRESERVE":
            # Press molds and welder selector tags stay in the slot.
            if count != 0 or not (spec["press"] or spec["kind"] == "welder"):
                return "preserve"
            continue
        if kind != "CONSUME" or count <= 0 or count > 64:
            return "input amount"
    for output in relation.get("item_outputs") or []:
        count = _output_count(output)
        if count <= 0 or count > 64:
            return "output amount"
    for fluid in list(relation.get("fluid_inputs") or []) + list(
        relation.get("fluid_outputs") or []
    ):
        amount = _fluid_amount(fluid)
        fluid_cap = 4096 if spec.get("machine") == "cutter" else 4000
        if amount <= 0 or amount > fluid_cap:
            return "fluid amount"
    return None


def _host_rejection(relation: dict[str, Any], spec: dict[str, Any]) -> str | None:
    """Name the Java validator clause that would reject this row at load."""
    label = spec["target"].split(":", 1)[-1]
    inputs = list(relation.get("item_inputs") or [])
    outputs = list(relation.get("item_outputs") or [])
    fluid_in = list(relation.get("fluid_inputs") or [])
    fluid_out = list(relation.get("fluid_outputs") or [])
    kind = spec["kind"]
    if fluid_out:
        return f"{label} host rejected fluid output"
    if len(fluid_in) > int(spec["fluid_in"]):
        return f"{label} host rejected fluid input"
    if kind in {"crusher", "sifter"}:
        if len(inputs) != 1:
            return f"{label} host rejected item inputs"
        if not outputs or len(outputs) > int(spec["max_out"]):
            return f"{label} host rejected item outputs"
    elif kind == "anvil":
        if len(inputs) > int(spec["max_in"]):
            return f"{label} host rejected item inputs"
        if not (1 <= len(outputs) <= 2):
            return f"{label} host rejected item outputs"
        chances = [int(value) for value in (relation.get("output_chances") or [])]
        primary = chances[0] if chances else 10000
        if primary != 10000:
            return f"{label} host rejected primary chance"
    else:
        if len(inputs) > int(spec["max_in"]):
            return f"{label} host rejected item inputs"
        if not outputs or len(outputs) > int(spec["max_out"]):
            return f"{label} host rejected item outputs"
    eut = int(relation.get("eut") or 0)
    if eut <= 0:
        return f"{label} host rejected energy"
    maximum = spec["max_eut"]
    if maximum is not None and eut > int(maximum):
        return f"{label} host rejected energy"
    allow_zero = kind in {"mortar", "welder", "anvil"}
    chance = _chance_problem(relation, allow_zero=allow_zero)
    if chance:
        return f"{label} host rejected {chance}"
    amount = _amount_problem(relation, spec)
    if amount:
        return f"{label} host rejected {amount}"
    unsupported = list(relation.get("unsupported_semantics") or [])
    if unsupported:
        return f"{label} host rejected {unsupported[0]}"
    return None


def _shape(relation: dict[str, Any]) -> tuple[Any, ...]:
    return (
        int(relation.get("duration") or 0),
        int(relation.get("eut") or 0),
        int(relation.get("special_value") or 0),
        tuple(int(value) for value in relation.get("item_input_counts") or []),
        json.dumps(relation.get("item_input_actions") or [], sort_keys=True),
        tuple(int(value) for value in relation.get("output_chances") or []),
        len(relation.get("item_inputs") or []),
        len(relation.get("item_outputs") or []),
        len(relation.get("fluid_inputs") or []),
        len(relation.get("fluid_outputs") or []),
        bool(relation.get("can_be_buffered", True)),
    )


def _input_signature(relation: dict[str, Any]) -> str:
    actions = relation.get("item_input_actions") or []
    counts = relation.get("item_input_counts") or []
    items: list[str] = []
    for index, operand in enumerate(relation.get("item_inputs") or []):
        action = emit_action(actions[index]) if index < len(actions) else {}
        count = counts[index] if index < len(counts) else 0
        items.append(
            json.dumps([count, action, emit_item(operand)], sort_keys=True)
        )
    fluids = [
        json.dumps(emit_fluid(operand), sort_keys=True)
        for operand in relation.get("fluid_inputs") or []
    ]
    return "|".join(sorted(items)) + "||" + "|".join(sorted(fluids))


def _authored_input_signatures(target_map: str) -> dict[str, str]:
    """Input signatures already owned by hand-authored recipes on this map."""
    root = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "recipe"
    found: dict[str, str] = {}
    if not root.is_dir():
        return found
    for path in sorted(root.rglob("*.json")):
        document = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(document, dict) or document.get("map") != target_map:
            continue
        try:
            found[_input_signature(_relation_from_datapack(document))] = _rel(path)
        except ValueError:
            continue
    return found


def _relation_from_datapack(document: dict[str, Any]) -> dict[str, Any]:
    operands = []
    for entry in document.get("item_inputs") or []:
        if not isinstance(entry, dict):
            raise ValueError("item input")
        if entry.get("type") == "neoforge:components":
            operands.append(
                {
                    "runtime_id": entry.get("items"),
                    "_components": entry.get("components") or {},
                }
            )
            continue
        if "item" in entry:
            operands.append({"runtime_id": entry["item"]})
            continue
        if "tag" in entry:
            operands.append(
                {
                    "tag": entry["tag"],
                    "mapping": "vanilla_wildcard_tag",
                    "runtime_id": entry["tag"],
                }
            )
            continue
        raise ValueError("item input")
    fluids = []
    for entry in document.get("fluid_inputs") or []:
        if not isinstance(entry, dict):
            raise ValueError("fluid input")
        fluids.append(
            {
                "runtime_id": entry.get("id") or entry.get("fluid"),
                "amount": entry.get("amount"),
            }
        )
    count = len(operands)
    return {
        "item_inputs": operands,
        "item_input_counts": document.get("item_input_counts") or [1] * count,
        "item_input_actions": document.get("item_input_actions")
        or [{"kind": "consume"}] * count,
        "fluid_inputs": fluids,
    }


def _color_wildcard_item(relation: dict[str, Any]) -> str:
    for key in ("item_inputs", "item_outputs"):
        for operand in relation.get(key) or []:
            source = operand.get("source") or {}
            item = str(source.get("item") or "")
            if source.get("meta") == "*" and item in VANILLA_COLOR_META_ITEMS:
                return item
    return ""


def _identity(relation: dict[str, Any], slug: str) -> str:
    emitted = {
        "duration": int(relation.get("duration") or 0),
        "eut": int(relation.get("eut") or 0),
        "fluid_inputs": [emit_fluid(op) for op in relation.get("fluid_inputs") or []],
        "fluid_outputs": [emit_fluid(op) for op in relation.get("fluid_outputs") or []],
        "item_input_actions": [
            emit_action(action) for action in relation.get("item_input_actions") or []
        ],
        "item_input_counts": list(relation.get("item_input_counts") or []),
        "item_inputs": [emit_item(op) for op in relation.get("item_inputs") or []],
        "item_outputs": [
            emit_item_output(op) for op in relation.get("item_outputs") or []
        ],
        "output_chances": _normalized_chances(relation),
        "special_value": int(relation.get("special_value") or 0),
    }
    return _consume_identity(emitted, wave_id=slug)


def _block(blocked: list[dict[str, str]], index: int, digest: str, reason: str) -> None:
    blocked.append(
        {
            "reason": reason,
            "source_recipe_index": str(index),
            "source_row_sha256": digest,
        }
    )


def _map_dir(spec: dict[str, Any]) -> Path:
    return WAVE / spec["target"].split(":", 1)[-1]


def _rel(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def select_map(spec: dict[str, Any], maps: dict[str, Any]) -> dict[str, int]:
    directory = _map_dir(spec)
    pack = directory / "source_pack"
    pack.mkdir(parents=True, exist_ok=True)
    dump_path = DUMP_ROOT / f"{spec['dump']}.json"
    slice_path = pack / "dump_slice.json"
    if not slice_path.is_file() or slice_path.stat().st_size != dump_path.stat().st_size:
        shutil.copyfile(dump_path, slice_path)
    document = json.loads(slice_path.read_text(encoding="utf-8"))
    recipes = document["recipes"]
    groups: dict[tuple[Any, ...], list[tuple[int, str]]] = defaultdict(list)
    blocked: list[dict[str, str]] = []
    seen_digest: set[str] = set()
    seen_identity: set[str] = set()
    seen_inputs: dict[str, str] = _authored_input_signatures(spec["target"])
    prefix_rows = 0
    for index, recipe in enumerate(recipes):
        digest = _source_row_hash(recipe)
        if digest in seen_digest:
            _block(blocked, index, digest, "duplicate source row")
            continue
        seen_digest.add(digest)
        try:
            relation, errors = gt6.compile_row(
                recipe,
                host=spec["target"],
                target_map=spec["target"],
                source_map=spec["dump"],
                family_id=f"{spec['dump']}#prefix",
                template_key=f"{spec['dump']}#prefix",
                recipe_index=index,
                shadow_order=0,
                source_revision=SOURCE_REVISION,
                source_row_sha256=digest,
                maps=maps,
            )
        except ValueError as failure:
            _block(blocked, index, digest, str(failure))
            continue
        if errors or source_import._relation_unmapped(relation):
            _block(
                blocked,
                index,
                digest,
                errors[0] if errors else "unmapped operand",
            )
            continue
        if _uses_lossy_alias(relation):
            _block(blocked, index, digest, "lossy fixture alias is not a GT6 operand")
            continue
        rejection = _host_rejection(relation, spec)
        if rejection:
            _block(blocked, index, digest, rejection)
            continue
        unregistered = missing_runtime_item_operands(relation)
        unregistered.extend(ungated_material_form_operands(relation))
        unknown_vanilla = _unknown_vanilla_item(relation)
        if unknown_vanilla:
            unregistered.append(f"unregistered runtime item {unknown_vanilla}")
        if unregistered:
            _block(blocked, index, digest, unregistered[0])
            continue
        try:
            identity = _identity(relation, spec["slug"])
        except ValueError as failure:
            _block(blocked, index, digest, str(failure))
            continue
        if identity in seen_identity:
            _block(blocked, index, digest, "duplicate runtime identity")
            continue
        wildcard = _color_wildcard_item(relation)
        if wildcard:
            _block(blocked, index, digest, f"unsplit vanilla color wildcard {wildcard}")
            continue
        signature = _input_signature(relation)
        if signature in seen_inputs:
            runtime = _runtime((relation.get("item_inputs") or [{}])[0])
            _block(
                blocked,
                index,
                digest,
                "shadowed input signature "
                + (runtime or signature)
                + f" by recipe {seen_inputs[signature]}",
            )
            continue
        seen_identity.add(identity)
        seen_inputs[signature] = str(index)
        if prefix_transform.matches(relation, {}):
            prefix_rows += 1
        groups[_shape(relation)].append((index, digest))
    families: list[dict[str, Any]] = []
    published = 0
    family_index = 0
    stem = spec["dump"]
    for rows in groups.values():
        for start in range(0, len(rows), HOLDER):
            chunk = rows[start : start + HOLDER]
            family_id = f"{stem}#bulk_{family_index:04d}"
            families.append(
                {
                    "family_id": family_id,
                    "relations": [
                        {
                            "shadow_order": order,
                            "source_recipe_index": row_index,
                            "source_row_sha256": row_digest,
                        }
                        for order, (row_index, row_digest) in enumerate(chunk)
                    ],
                    "template_key": family_id,
                }
            )
            published += len(chunk)
            family_index += 1
    if not families:
        raise SystemExit(f"{spec['slug']}: no publishable rows")
    work_path = pack / "work_set.json"
    work_path.write_text(
        json.dumps(
            {
                "accounting": {
                    "blocked_rows": len(blocked),
                    "prefix_transform_rows": prefix_rows,
                    "published_rows": published,
                    "selection_rule": (
                        "host-accepted translated rows only; "
                        "missing objects and host rejections stay blocked"
                    ),
                    "source_rows": len(recipes),
                },
                "families": families,
            },
            separators=(",", ":"),
        ),
        encoding="utf-8",
    )
    census.write_stable(
        directory / "blocked.json",
        {
            "rows": blocked,
            "schema_version": 1,
            "source_rows": len(recipes),
        },
    )
    manifest_path = directory / "source_pack_manifest.json"
    census.write_stable(
        manifest_path,
        {
            "files": [
                {
                    "path": _rel(slice_path),
                    "role": "dump_slice",
                    "sha256": _sha256(slice_path),
                },
                {
                    "path": _rel(work_path),
                    "role": "work_set",
                    "sha256": _sha256(work_path),
                },
            ],
            "full_replay": {
                "required_for_first_generation": True,
                "skip_is_not_pass": True,
            },
            "provenance_policy": {"append_only": True, "forbid_gt6u": True},
            "schema_version": 1,
            "source_dialect": "gt6",
            "source_pack_id": spec["slug"],
            "source_revision": SOURCE_REVISION,
            "source_system": "gt6",
        },
    )
    census.write_stable(
        directory / "recipe_import.json",
        {
            "family_membership_source": {
                "kind": "work_set",
                "path": _rel(work_path),
            },
            "host": spec["target"],
            "import_slug": spec["slug"],
            "operand_authorities": [
                {"id": "identity_ledger_v3", "kind": "data"},
                {"id": "material_form_authority", "kind": "data"},
                {"adapter": "gt6", "kind": "source_dialect"},
            ],
            "output_paths": {
                "lock_candidate": _rel(directory / "lock_candidate.json"),
                "receipt": _rel(directory / "source_receipt.json"),
                "review": _rel(directory / "source_review.json"),
                "source": _rel(directory / "source.json"),
            },
            "representation_policy": {"allowed": ["exact", "exact_multi"]},
            "schema_version": 1,
            "selection_rule": {"kind": "work_set_members"},
            "source_maps": [spec["dump"]],
            "source_pack": _rel(manifest_path),
            "stable_id_policy": {
                "algorithm": "source_system_revision_family_relation",
                "include_card_number": False,
            },
            "target_map": spec["target"],
        },
    )
    print(
        f"{spec['slug']}: source {len(recipes)} published {published} "
        f"blocked {len(blocked)} prefix {prefix_rows} families {family_index}",
        flush=True,
    )
    return {
        "blocked": len(blocked),
        "families": family_index,
        "prefix": prefix_rows,
        "published": published,
        "source": len(recipes),
    }


def finish_map(spec: dict[str, Any]) -> list[dict[str, Any]]:
    directory = _map_dir(spec)
    source_import.write_import(directory / "recipe_import.json")
    source = json.loads((directory / "source.json").read_text(encoding="utf-8"))
    relations = list(source["relations"])
    prefix_rows = [
        row for row in relations if prefix_transform.matches(row, {})
    ]
    prefix_digests = {str(row["source_row_sha256"]) for row in prefix_rows}
    remainder = [
        str(row["source_row_sha256"])
        for row in relations
        if str(row["source_row_sha256"]) not in prefix_digests
    ]
    handlers: list[dict[str, Any]] = [
        {
            "handler_id": f"{spec['slug'].replace('/', '_')}_prefix_transform",
            "kind": "prefix_transform",
            "template": {},
        }
    ]
    if remainder:
        handlers.append(
            {
                "covered_source_row_sha256": remainder,
                "handler_id": f"{spec['slug'].replace('/', '_')}_exact_remainder",
                "kind": "exact_remainder",
            }
        )
    document = {
        "blocked": [],
        "family_id": f"{spec['dump']}#prefix",
        "handlers": handlers,
        "schema": "rule_ir_v1",
        "schema_version": 1,
        "source_map": spec["dump"],
        "source_revision": SOURCE_REVISION,
        "target_map": spec["target"],
    }
    proof = prove_coverage(document, relations)
    census.write_stable(directory / "rule_ir.json", document)
    cohort = spec["slug"].split("/", 1)[-1].replace("-", "_")
    publication_group = f"{spec['target']}/{spec['slug'].split('/', 1)[-1]}"
    candidate = census.load_json(directory / "lock_candidate.json")
    lock = reviewed_lock(
        candidate,
        publication_group=publication_group,
        cohort=cohort,
    )
    lock["generated_by"] = (
        "tools/waves/recipe/gt6-prefix-regular-bulk/build_prefix_regular.py"
    )
    lock["note"] = (
        f"live compile for {spec['slug']}; host-accepted translated rows only; "
        "missing objects stay blocked; on-demand matrix publication"
    )
    census.write_stable(directory / "production_lock.json", lock)
    family_ids = [str(row["template_key"]) for row in lock["production"]["families"]]
    stable_ids = [
        sid for row in lock["production"]["families"] for sid in row["stable_ids"]
    ]
    policy_name = spec["target"].split(":", 1)[-1] + "_" + cohort
    census.write_stable(
        ROOT
        / "src/recipe_generated/resources/data/cruciblecraft/recipe/publication_policy"
        / f"{policy_name}.json",
        {
            "cache_ceiling": 16,
            "eager_stable_ids": [],
            "family_count": len(family_ids),
            "membership_root_sha256": membership_root(family_ids, stable_ids),
            "policy_type": "on_demand",
            "publication_group": publication_group,
            "relation_count": len(stable_ids),
            "routing_schema_version": "compact-shard-v1",
            "target_map": spec["target"],
            "type": "cruciblecraft:compact_publication_policy",
        },
    )
    census.write_stable(
        directory / "coverage_proof.json",
        {
            "blocked_rows": proof["blocked"],
            "handlers": proof["handlers"],
            "prefix_transform_rows": len(prefix_rows),
            "published_rows": len(relations),
            "remainder_rows": len(remainder),
            "schema_version": 1,
        },
    )
    print(
        f"{spec['slug']}: locked {len(relations)} prefix {len(prefix_rows)} "
        f"remainder {len(remainder)}",
        flush=True,
    )
    return _samples(spec, relations, publication_group)


_REGISTERED_FLUIDS: set[str] | None = None


def _registered_fluid_ids() -> set[str]:
    global _REGISTERED_FLUIDS
    if _REGISTERED_FLUIDS is None:
        gate = census.load_json(
            ROOT
            / "src"
            / "main"
            / "resources"
            / "census"
            / "runtime_registry_gate.json"
        )
        _REGISTERED_FLUIDS = set((gate.get("categories") or {}).get("fluids") or [])
    return _REGISTERED_FLUIDS


def _simple(relation: dict[str, Any]) -> bool:
    """Rows a GameTest can insert without a prefix component."""
    operands = list(relation.get("item_inputs") or []) + list(
        relation.get("item_outputs") or []
    )
    if not operands:
        return False
    registered = registered_runtime_ids()
    fluids = _registered_fluid_ids()
    for operand in operands:
        if operand.get("_components"):
            return False
        runtime = _runtime(operand)
        if runtime.startswith("minecraft:"):
            continue
        if runtime not in registered:
            return False
    for operand in relation.get("fluid_inputs") or []:
        runtime = _runtime(operand)
        if runtime.startswith("minecraft:"):
            continue
        if runtime not in fluids:
            return False
    return True


def _rank(relation: dict[str, Any]) -> tuple[Any, ...]:
    duration = int(relation.get("duration") or 0)
    material = str((relation.get("item_inputs") or [{}])[0].get("material") or "")
    return (0 if _simple(relation) else 1, 0 if duration <= 64 else 1, duration, material)


def _stack_spec(operand: dict[str, Any], count: int) -> dict[str, Any]:
    emitted = emit_item_output(operand)
    spec: dict[str, Any] = {"count": count, "id": str(emitted["id"])}
    material = str((emitted.get("components") or {}).get("cruciblecraft:prefix_material") or "")
    if material:
        spec["material"] = material
    return spec


def _sample(role: str, relation: dict[str, Any]) -> dict[str, Any]:
    counts = [int(value) for value in relation.get("item_input_counts") or []]
    chances = [int(value) for value in relation.get("output_chances") or []]
    guaranteed = []
    for index, output in enumerate(relation.get("item_outputs") or []):
        chance = chances[index] if index < len(chances) else 10000
        if chance != 10000:
            continue
        guaranteed.append(_stack_spec(output, _output_count(output)))
    return {
        "duration": int(relation.get("duration") or 0),
        "eut": int(relation.get("eut") or 0),
        "fluids": [
            {"amount": _fluid_amount(fluid), "id": _runtime(fluid)}
            for fluid in relation.get("fluid_inputs") or []
        ],
        "guaranteed_outputs": guaranteed,
        "items": [
            _stack_spec(item, max(1, counts[index] if index < len(counts) else 1))
            for index, item in enumerate(relation.get("item_inputs") or [])
        ],
        "role": role,
        "special_value": int(relation.get("special_value") or 0),
    }


def _material(relation: dict[str, Any]) -> str:
    return str((relation.get("item_inputs") or [{}])[0].get("material") or "")


def _short_pool(rows: list[dict[str, Any]], used: set[int]) -> list[dict[str, Any]]:
    available = [row for row in rows if id(row) not in used]
    simple = [row for row in available if _simple(row)]
    short = [row for row in simple if int(row.get("duration") or 0) <= 64]
    if not (short or simple):
        raise ValueError("no registered-item sample row")
    return short or simple


def _samples(
    spec: dict[str, Any],
    relations: list[dict[str, Any]],
    publication_group: str,
) -> dict[str, Any]:
    prefix = [row for row in relations if prefix_transform.matches(row, {})]
    rest = [row for row in relations if not prefix_transform.matches(row, {})]
    used: set[int] = set()
    rule = min(_short_pool(prefix or relations, used), key=_rank)
    used.add(id(rule))
    remainder = min(_short_pool(rest or relations, used), key=_rank)
    used.add(id(remainder))
    boundary = max(_short_pool(prefix or relations, used), key=_material)
    return {
        "machine": spec["machine"],
        "publication_group": publication_group,
        "samples": [
            _sample("rule", rule),
            _sample("remainder", remainder),
            _sample("boundary", boundary),
        ],
        "target_map": spec["target"],
    }


def rewrite_samples() -> None:
    sample_maps = []
    for spec in MAPS:
        directory = _map_dir(spec)
        source = json.loads((directory / "source.json").read_text(encoding="utf-8"))
        cohort = spec["slug"].split("/", 1)[-1]
        sample_maps.append(
            _samples(spec, list(source["relations"]), f"{spec['target']}/{cohort}")
        )
    payload = {"maps": sample_maps, "schema_version": 1}
    census.write_stable(SAMPLES, payload)
    census.write_stable(WAVE / "samples.json", payload)
    print(f"samples {sum(len(row['samples']) for row in sample_maps)}", flush=True)


def main() -> None:
    if "--samples-only" in sys.argv:
        rewrite_samples()
        return
    wanted = {arg for arg in sys.argv[1:] if not arg.startswith("-")}
    selected = [spec for spec in MAPS if not wanted or spec["slug"] in wanted or spec["machine"] in wanted]
    if not selected:
        raise SystemExit("no matching maps")
    maps = gt6._maps()
    sample_maps = []
    for spec in selected:
        select_map(spec, maps)
        sample_maps.append(finish_map(spec))
    if not wanted:
        census.write_stable(
            SAMPLES,
            {"maps": sample_maps, "schema_version": 1},
        )
        census.write_stable(WAVE / "samples.json", {"maps": sample_maps, "schema_version": 1})
        print(f"samples {sum(len(row['samples']) for row in sample_maps)}", flush=True)


if __name__ == "__main__":
    main()
