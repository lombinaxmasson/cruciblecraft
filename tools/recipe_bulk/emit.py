#!/usr/bin/env python3
"""Emit CompactGTRecipeFamilyDefinition JSON from resolved source relations."""
from __future__ import annotations

import re
from functools import lru_cache
from pathlib import Path
from typing import Any

from tools.recipe_bulk.models import WaveSpec
from tools.recipe_bulk.matrix import authored_relations, wrap_document
from tools.recipe_bulk.public_exchange import PUBLIC_EXCHANGE_PREFIXES
from tools import io_common as files
from tools import tool_head_prefix as thp

SOURCE_REVISION_DEFAULT = "3703e40308c8c030763fd6297dea8b210d2a77b1"
GT_PREFIXES = ("gregtech:", "gregapi:", "fixed:")
PREFIX_MATERIAL_COMPONENT = "cruciblecraft:prefix_material"
# Matches Java MaterialFormHosts unique-hosted prefixes. Do not rewrite these
# slash ids onto a shared prefix Item; pipes/cables/storage still have
# per-material registry ids until a later child card.
UNIQUE_HOSTED_PREFIXES = frozenset({
    "block",
    "ore",
    "rock",
    "machine_casing",
    "machine_casing_double",
    "machine_casing_quadruple",
    "machine_casing_dense",
    "wire",
    "double_wire",
    "triple_wire",
    "quadruple_wire",
    "quintuple_wire",
    "sextuple_wire",
    "septuple_wire",
    "octuple_wire",
    "nonuple_wire",
    "decuple_wire",
    "undecuple_wire",
    "dodecuple_wire",
    "tredecuple_wire",
    "tetradecuple_wire",
    "pentadecuple_wire",
    "hexadecuple_wire",
    "cable",
    "double_cable",
    "quadruple_cable",
    "octuple_cable",
    "dodecuple_cable",
    "tiny_fluid_pipe",
    "small_fluid_pipe",
    "fluid_pipe",
    "large_fluid_pipe",
    "huge_fluid_pipe",
    "quadruple_fluid_pipe",
    "nonuple_fluid_pipe",
    "item_pipe",
    "large_item_pipe",
    "huge_item_pipe",
    "restrictive_item_pipe",
    "large_restrictive_item_pipe",
    "huge_restrictive_item_pipe",
})
_PREFIX_INDEX = (
    files.ROOT / "src/main/resources/data/cruciblecraft/material_prefixes/index.json"
)
_MATERIAL_INDEX = (
    files.ROOT / "src/main/resources/data/cruciblecraft/materials/index.json"
)
_ASPHALT_OBJECT = re.compile(
    r"^cruciblecraft:gt_object/gt_block_asphalt_(m\d+)$"
)
_CONTENT = files.ROOT / "tools" / "waves" / "content"
# Dummy CatalogNamedItem paths folded onto live BlockItems. Mill locks still
# carry the withdrawn ids; emit must publish the live host or RegistryOps
# drops item_inputs and GTRecipe dies with inputs=0, counts=1.
_FOLD_OVERLAY_PATHS = (
    _CONTENT / "gt6-mte-converter-host-fold" / "fold_overlay.json",
    _CONTENT / "gt6-mte-processing-host-fold" / "fold_overlay.json",
    _CONTENT / "gt6-mte-hopper-host-fold" / "fold_overlay.json",
    _CONTENT / "gt6-mte-reactor-rod-host-fold" / "fold_overlay.json",
    _CONTENT / "gt6-fluid-pipe-runtime" / "execution_subset.json",
    _CONTENT / "gt6-item-pipe-runtime" / "execution_subset.json",
    _CONTENT / "gt6-restrictive-item-pipe-runtime" / "restrictive_overlay.json",
    _CONTENT / "gt6-fluid-combo-pipe-runtime" / "combo_overlay.json",
    _CONTENT / "gt6-eu-wire-cable-runtime" / "execution_subset.json",
    _CONTENT / "gt6-eu-missing-wire-gauges-runtime" / "missing_gauge_overlay.json",
    _CONTENT / "gt6-connector-alias-repair" / "alias_overlay.json",
)
# Closed mte-redstone-wire withdrew these dummy CatalogNamedItem ids onto
# the live wireGt01 RedstoneWireBlockItems. Overlay rows keep dummy_path
# historical; mill locks still emit the withdrawn ids.
_WITHDRAWN_DUMMY_HOSTS = {
    "cruciblecraft:redstone_wire/red_alloy": "cruciblecraft:red_alloy/wire",
    "cruciblecraft:redstone_wire/signalum": "cruciblecraft:signalum/wire",
    "cruciblecraft:lumium/wirelamp": "cruciblecraft:lumium/wire",
}
# Bare material ids that mill still publishes for OP.gem. Live inventory is
# the unique ``cruciblecraft:{material}/gem`` item.
_BARE_GEM_MATERIALS = frozenset({"coal_coke"})


@lru_cache(maxsize=1)
def _inventory_prefix_paths() -> frozenset[str]:
    names = files.load_json(_PREFIX_INDEX)
    return frozenset(Path(str(name)).stem for name in names)


@lru_cache(maxsize=1)
def _material_ids() -> frozenset[str]:
    names = files.load_json(_MATERIAL_INDEX)
    return frozenset(Path(str(name)).stem for name in names)


def rewrite_published_block_runtime(runtime: str) -> str:
    """Published asphalt cubes use the modern catalog id, not gt_block/asphalt_mN."""
    match = _ASPHALT_OBJECT.fullmatch(runtime)
    if not match:
        return runtime
    numbered = f"gt_block/asphalt_{match.group(1)}"
    from tools import catalog_modern_ids as modern
    row = modern.by_old_path().get(numbered)
    if row:
        return str(row["runtime_id"])
    return f"cruciblecraft:{numbered}"


def _published_runtime(value: str) -> str:
    value = str(value or "").strip()
    if not value:
        return ""
    if ":" in value:
        return value
    return f"cruciblecraft:{value}"


@lru_cache(maxsize=1)
def _folded_host_runtime_map() -> dict[str, str]:
    mapped: dict[str, str] = {}
    for path in _FOLD_OVERLAY_PATHS:
        if not path.is_file():
            continue
        document = files.load_json(path)
        for row in document.get("rows") or []:
            dummy = _published_runtime(str(row.get("dummy_path") or ""))
            live = _published_runtime(str(row.get("live_block") or ""))
            if not dummy or not live or dummy == live:
                continue
            previous = mapped.get(dummy)
            if previous and previous != live:
                raise ValueError(
                    f"folded host remap conflict {dummy}: {previous} vs {live}"
                )
            mapped[dummy] = live
    for dummy, live in _WITHDRAWN_DUMMY_HOSTS.items():
        previous = mapped.get(dummy)
        if previous and previous != live:
            raise ValueError(
                f"folded host remap conflict {dummy}: {previous} vs {live}"
            )
        mapped[dummy] = live
    return mapped


def rewrite_folded_host_runtime(runtime: str) -> str:
    """Map withdrawn dummy MTE/pipe/wire paths onto the live host id."""
    return _folded_host_runtime_map().get(runtime, runtime)


def rewrite_bare_gem_runtime(runtime: str) -> str:
    """Map leftover bare material ids onto ``{material}/gem`` for prefix emit."""
    if not runtime.startswith("cruciblecraft:"):
        return runtime
    path = runtime.split(":", 1)[1]
    if "/" in path or path not in _BARE_GEM_MATERIALS:
        return runtime
    return f"cruciblecraft:{path}/gem"


def family_filename(template_key: str) -> str:
    return (
        template_key.replace(".", "_").replace("#", "_").replace(":", "_") + ".json"
    )


def emit_action(action: dict[str, Any]) -> dict[str, Any]:
    kind = str(action.get("kind") or "consume").lower()
    emitted: dict[str, Any] = {"kind": kind}
    if kind == "wear":
        emitted["damage"] = int(action.get("damage") or 0)
    return emitted


def _runtime_id(operand: dict[str, Any]) -> str:
    runtime = str(operand.get("runtime_id") or "")
    if not runtime:
        raise ValueError(f"missing runtime_id: {operand}")
    source = operand.get("source") or {}
    item = str(source.get("item") or operand.get("item") or "")
    meta = source.get("meta")
    runtime = thp.rewrite_published_runtime(runtime, item, meta)
    runtime = rewrite_published_block_runtime(runtime)
    runtime = rewrite_folded_host_runtime(runtime)
    runtime = rewrite_bare_gem_runtime(runtime)
    if thp.is_forbidden_unique_tool_head(runtime):
        raise ValueError(f"refusing unique tool_head item: {runtime}")
    if runtime.startswith(GT_PREFIXES):
        raise ValueError(f"refusing to emit GT runtime id: {runtime}")
    if not runtime.startswith(("minecraft:", "cruciblecraft:")):
        raise ValueError(f"runtime id is not a published vanilla/CC identity: {runtime}")
    return runtime


def project_shared_inventory(runtime: str) -> tuple[str, dict[str, str]] | None:
    """Map long-tail ``cruciblecraft:{material}/{form}`` onto prefix Item + component.

    Logical mill identity stays ``material/form``. Public exchange prefixes and
    unique hosted forms keep their live slash ids. GT wood, furniture, food,
    vanilla items, and already-projected prefix ids stay as their live registry
    ids.
    """
    if not runtime.startswith("cruciblecraft:"):
        return None
    path = runtime.split(":", 1)[1]
    slash = path.find("/")
    if slash <= 0 or slash >= len(path) - 1:
        return None
    material = path[:slash]
    form = path[slash + 1 :]
    if not material or not form or "/" in form:
        return None
    if form in UNIQUE_HOSTED_PREFIXES or form in PUBLIC_EXCHANGE_PREFIXES:
        return None
    if form not in _inventory_prefix_paths() or material not in _material_ids():
        return None
    return f"cruciblecraft:{form}", {PREFIX_MATERIAL_COMPONENT: material}


def _item_identity(operand: dict[str, Any], runtime: str) -> tuple[str, dict[str, Any] | None]:
    existing = operand.get("_components")
    if existing:
        return runtime, dict(existing)
    projected = project_shared_inventory(runtime)
    if projected is None:
        return runtime, None
    return projected[0], dict(projected[1])


def emit_item(operand: dict[str, Any]) -> dict[str, Any]:
    tag = operand.get("tag")
    if tag and str(operand.get("mapping") or "") == "vanilla_wildcard_tag":
        return {"tag": str(tag)}
    runtime, components = _item_identity(operand, _runtime_id(operand))
    if components:
        return {
            "type": "neoforge:components",
            "items": runtime,
            "components": dict(components),
        }
    return {"item": runtime}


def emit_item_output(operand: dict[str, Any]) -> dict[str, Any]:
    runtime, components = _item_identity(operand, _runtime_id(operand))
    count = int((operand.get("source") or {}).get("count") or operand.get("count") or 1)
    emitted: dict[str, Any] = {"count": count, "id": runtime}
    if components:
        emitted["components"] = dict(components)
    return emitted


def emit_fluid(operand: dict[str, Any]) -> dict[str, Any]:
    runtime = _runtime_id(operand)
    amount = int((operand.get("source") or {}).get("amount") or operand.get("amount") or 0)
    if amount <= 0:
        raise ValueError(f"fluid amount must be positive: {operand}")
    return {"amount": amount, "id": runtime}


def hex_stable_id(source_stable_id: str, prefix: str) -> str:
    hex16 = source_stable_id.rsplit("/", 1)[-1]
    if len(hex16) != 16 or any(char not in "0123456789abcdef" for char in hex16):
        raise ValueError(f"Frozen stable_id suffix is not 16 hex chars: {source_stable_id}")
    return f"cruciblecraft:{prefix}/{hex16}"


def relation_source_kind(
    spec: WaveSpec | None,
    relation: dict[str, Any],
    operand_classes: list[str],
) -> str:
    policy = spec.source_kind_policy if spec is not None else "relation_provenance"
    if policy == "relation_provenance":
        kinds = set((relation.get("provenance") or {}).get("kinds") or [])
        if "DESIGN_POLICY" in kinds:
            return "DESIGN_POLICY"
        if "SOURCE_DERIVED" in kinds:
            return "SOURCE_DERIVED"
        return "SOURCE_BACKED"
    if policy == "operand_design_first":
        if "DESIGN_POLICY" in operand_classes:
            return "DESIGN_POLICY"
        if "SOURCE_DERIVED" in operand_classes:
            return "SOURCE_DERIVED"
        return "SOURCE_BACKED"
    if "SOURCE_DERIVED" in operand_classes:
        return "SOURCE_DERIVED"
    return "SOURCE_BACKED"


def stable_id_for(
    spec: WaveSpec | None,
    relation: dict[str, Any],
    lock_row: dict[str, Any] | None,
) -> str:
    if spec is None or spec.stable_id_policy == "lock":
        if lock_row is None:
            raise ValueError("lock stable_id policy requires a lock row")
        if lock_row.get("stable_id") and not lock_row.get("stable_ids"):
            return str(lock_row["stable_id"])
        locked = [str(value) for value in (lock_row.get("stable_ids") or [])]
        source_id = str(relation.get("stable_id") or "")
        if source_id and source_id in locked:
            return source_id
        raise ValueError("lock stable_id policy requires a lock row")
    if spec.stable_id_policy == "hex_suffix":
        if not spec.stable_id_prefix:
            raise ValueError(f"{spec.wave_id} hex_suffix stable ids require a prefix")
        return hex_stable_id(str(relation["stable_id"]), spec.stable_id_prefix)
    return str(relation["stable_id"])


def emit_resolved_relation(
    relation: dict[str, Any],
    *,
    spec: WaveSpec | None = None,
    lock_row: dict[str, Any] | None = None,
    operand_classes: list[str] | None = None,
    template_key: str | None = None,
) -> dict[str, Any]:
    if relation.get("parameterized"):
        raise ValueError("V1 compiler must not emit runtime parameterized fields")
    key = str(
        template_key
        or (lock_row or {}).get("template_key")
        or relation.get("template_key")
    )
    item_inputs = [emit_item(op) for op in relation.get("item_inputs") or []]
    item_outputs = [emit_item_output(op) for op in relation.get("item_outputs") or []]
    fluid_inputs = [emit_fluid(op) for op in relation.get("fluid_inputs") or []]
    fluid_outputs = [emit_fluid(op) for op in relation.get("fluid_outputs") or []]
    counts = [int(v) for v in relation.get("item_input_counts") or []]
    actions = [emit_action(action) for action in relation.get("item_input_actions") or []]
    if item_inputs and not (len(item_inputs) == len(counts) == len(actions)):
        raise ValueError(f"{key} input arity drifted")
    for count, action in zip(counts, actions, strict=False):
        if action["kind"] == "consume" and count <= 0:
            raise ValueError(f"{key} consume counts must be positive")
        if action["kind"] in {"preserve", "wear"} and count != 0:
            raise ValueError(f"{key} preserve/wear counts must be zero")
    chances = [int(v) for v in relation.get("output_chances") or []]
    if item_outputs and len(chances) < len(item_outputs):
        chances = chances + [10000] * (len(item_outputs) - len(chances))
    elif len(chances) > len(item_outputs):
        chances = chances[: len(item_outputs)]
    classes = operand_classes or [
        str(op.get("_source_kind") or "SOURCE_BACKED")
        for field in ("item_inputs", "item_outputs", "fluid_inputs", "fluid_outputs")
        for op in relation.get(field) or []
    ]
    evidence = (
        [relation["source_row_sha256"]]
        if relation.get("source_row_sha256")
        else list((relation.get("provenance") or {}).get("evidence_hashes") or [])
    )
    return {
        "stable_id": stable_id_for(spec, relation, lock_row),
        "item_inputs": item_inputs,
        "item_input_counts": counts,
        "item_input_actions": actions,
        "item_outputs": item_outputs,
        "fluid_inputs": fluid_inputs,
        "fluid_outputs": fluid_outputs,
        "output_chances": chances,
        "duration": int(relation["duration"]),
        "eut": int(relation.get("eut") or 0),
        "special_value": int(relation.get("special_value") or 0),
        "can_be_buffered": bool(relation.get("can_be_buffered", True)),
        "shadow_order": int(relation.get("shadow_order") or 0),
        "provenance": {
            "source_kind": relation_source_kind(spec, relation, classes),
            "selected_source_recipe": key,
            "evidence_hashes": evidence,
        },
    }


def emit_wave_document(
    spec: WaveSpec,
    *,
    template_key: str,
    source_rows: list[dict[str, Any]],
    resolved_rows: list[dict[str, Any]],
    lock_row: dict[str, Any] | None,
    target_map: str,
    publication_group: str | None,
    operand_classes: list[list[str]],
) -> dict[str, Any]:
    relations = [
        emit_resolved_relation(
            resolved,
            spec=spec,
            lock_row=lock_row,
            operand_classes=classes,
            template_key=template_key,
        )
        for resolved, classes in zip(resolved_rows, operand_classes, strict=True)
    ]
    source_revision = str(
        source_rows[0].get("source_revision") or SOURCE_REVISION_DEFAULT
    )
    document: dict[str, Any] = {
        "type": "cruciblecraft:compact_gt_recipe_family",
        "family_id": template_key,
        "target_map": target_map,
        "source_revision": source_revision,
    }
    if publication_group:
        document["publication_group"] = publication_group
    return wrap_document(document, relations)


def emit_family(
    relation: dict[str, Any],
    lock_row: dict[str, Any],
    *,
    target_map: str,
    publication_group: str,
    source_revision: str = SOURCE_REVISION_DEFAULT,
) -> dict[str, Any]:
    emitted = emit_resolved_relation(relation, lock_row=lock_row)
    template_key = str(lock_row.get("template_key") or relation.get("template_key"))
    return {
        "type": "cruciblecraft:compact_gt_recipe_family",
        "family_id": template_key,
        "target_map": target_map,
        "source_revision": relation.get("source_revision") or source_revision,
        "publication_group": publication_group,
        "relations": [emitted],
    }


def semantic_relation_key(relation: dict[str, Any]) -> dict[str, Any]:
    return {
        "can_be_buffered": relation.get("can_be_buffered"),
        "duration": relation.get("duration"),
        "eut": relation.get("eut"),
        "fluid_inputs": relation.get("fluid_inputs"),
        "fluid_outputs": relation.get("fluid_outputs"),
        "item_input_actions": relation.get("item_input_actions"),
        "item_input_counts": relation.get("item_input_counts"),
        "item_inputs": relation.get("item_inputs"),
        "item_outputs": relation.get("item_outputs"),
        "output_chances": relation.get("output_chances"),
        "shadow_order": relation.get("shadow_order"),
        "special_value": relation.get("special_value"),
    }


def semantic_replay_key(document: dict[str, Any]) -> dict[str, Any]:
    relations = authored_relations(document)
    return {
        "family_id": document.get("family_id"),
        "publication_group": document.get("publication_group"),
        "relations": [semantic_relation_key(relation) for relation in relations],
        "target_map": document.get("target_map"),
    }
