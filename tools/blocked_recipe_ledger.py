#!/usr/bin/env python3
"""Replayable blocked-recipe ledger. Counts stay in separate denominators."""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import atomic_io
from tools import io_common as io

SOURCE_REVISION = io.SOURCE_REVISION
LEDGER = io.TOOLS / "blocked_recipe_ledger.json"
FLUIDBED_DUMP = (
    ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / "gt.recipe.fuels.fluidbed.json"
)
FLUIDBED_BLOCKED = io.TOOLS / "energy_converter_fluidbed_blocked.json"
FLUIDBED_OUT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
    / "energy"
    / "fuels_fluidbed"
)
CROSS_REF = io.TOOLS / "gt6_oredict_cross_reference.json"
MATERIALS = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
)
# The old root selections remain historical inputs. The current selection below
# is the only source used for the current blocked ledger.
BATH_CURRENT = (
    io.TOOLS
    / "waves"
    / "bath"
    / "tiny-purified"
    / "bath_candidate_selection.json"
)
PETROLEUM = io.TOOLS / "petroleum_b4_operand_proof.json"
CAPABILITIES = io.TOOLS / "capabilities"
ORDINARY_SELECTIONS = (
    io.TOOLS / "waves" / "smelter" / "ordinary-closure" / "candidate_selection.json",
    io.TOOLS / "waves" / "mixer" / "ordinary-closure" / "candidate_selection.json",
    io.TOOLS / "waves" / "autoclave" / "ordinary-closure" / "candidate_selection.json",
    io.TOOLS / "waves" / "centrifuge" / "ordinary-closure" / "candidate_selection.json",
    io.TOOLS / "waves" / "compressor" / "ordinary-closure" / "candidate_selection.json",
    io.TOOLS / "waves" / "drying" / "ordinary-closure" / "candidate_selection.json",
    io.TOOLS / "waves" / "electrolyzer" / "ordinary-closure" / "candidate_selection.json",
)
PREFIX_ITEMS = {
    "gregtech:gt.meta.dust": "dust",
    "gregtech:gt.meta.dustTiny": "tiny_dust",
    "gregtech:gt.meta.dustSmall": "small_dust",
    "gregtech:gt.meta.dustDiv72": "dust_div72",
    "gregtech:gt.meta.ingot": "ingot",
    "gregtech:gt.meta.gem": "gem",
}
DUST_FLAG_FORMS = frozenset({"dust", "tiny_dust", "small_dust"})
OWNER = "recipe/blocked-chain-ledger"
CHAIN_FLUIDBED = "fluidbed-form-output-model"
CHAIN_BATH = "bath-identity-form-object"
CHAIN_PETROLEUM = "petroleum-mixer-mapping"


def dumps(document: Any) -> bytes:
    return (
        json.dumps(document, indent=2, sort_keys=True, ensure_ascii=False) + "\n"
    ).encode("utf-8")


def load_materials() -> dict[str, dict[str, Any]]:
    catalog: dict[str, dict[str, Any]] = {}
    for path in sorted(MATERIALS.glob("*.json")):
        catalog[path.stem] = io.load_json(path)
    return catalog


def material_forms(document: dict[str, Any]) -> set[str]:
    forms = set(document.get("include_prefixes") or [])
    forms.update((document.get("form_items") or {}).keys())
    flags = set(document.get("generation_flags") or [])
    if "cruciblecraft:generates_dust" in flags:
        forms.update(DUST_FLAG_FORMS)
    return forms


def item_exists(item_id: str, materials: dict[str, dict[str, Any]]) -> bool:
    if not item_id.startswith("cruciblecraft:"):
        return False
    slug = item_id.split(":", 1)[1]
    if "/" not in slug:
        return False
    material, prefix = slug.split("/", 1)
    document = materials.get(material)
    if document is None:
        return False
    return prefix in material_forms(document)


def map_prefix_item(
    stack: dict[str, Any],
    material_ids: dict[str, str],
) -> dict[str, Any] | None:
    prefix = PREFIX_ITEMS.get(str(stack.get("item") or ""))
    material = material_ids.get(str(stack.get("meta")))
    if prefix is None or not material:
        return None
    return {
        "item": f"cruciblecraft:{material}/{prefix}",
        "count": int(stack.get("count") or 1),
        "gt_item": stack.get("item"),
        "gt_meta": stack.get("meta"),
    }


def map_fluid(
    stack: dict[str, Any],
    fluids: dict[str, str],
) -> dict[str, Any] | None:
    raw = str(stack.get("fluid") or "")
    material = fluids.get(raw)
    if not material:
        return None
    fluid_id = (
        "minecraft:water"
        if material == "water"
        else f"cruciblecraft:molten_{material}"
        if raw.startswith("molten.")
        else f"cruciblecraft:{material}"
    )
    return {"amount": int(stack.get("amount") or 1), "id": fluid_id}


def classify_fluidbed_row(
    index: int,
    recipe: dict[str, Any],
    material_ids: dict[str, str],
    fluids: dict[str, str],
    materials: dict[str, dict[str, Any]],
    needs_outputs: bool,
) -> dict[str, Any]:
    source = (
        "gt6_dump/gt6_recipe_dump/maps/gt.recipe.fuels.fluidbed.json"
        f"#recipes[{index}]"
    )
    inputs = recipe.get("inputs") or []
    outputs = recipe.get("outputs") or []
    fluid_inputs = recipe.get("fluidInputs") or []
    mapped_in = map_prefix_item(inputs[0], material_ids) if inputs else None
    mapped_fluid = map_fluid(fluid_inputs[0], fluids) if fluid_inputs else None
    mapped_outs = [map_prefix_item(stack, material_ids) for stack in outputs]
    missing_identity = None
    missing_form = None
    missing_output_model = None
    blocker = None
    gt_item = inputs[0].get("item") if inputs else None
    gt_meta = inputs[0].get("meta") if inputs else None

    if not inputs or not fluid_inputs:
        blocker = "incomplete_gt6_row"
        missing_identity = source
    elif gt_item == "gregtech:gt.meta.storage.dust":
        blocker = "unmapped_storage_dust"
        missing_identity = f"gregtech:gt.meta.storage.dust#{gt_meta}"
    elif mapped_in is None:
        blocker = "unmapped_item_input"
        missing_identity = f"{gt_item}#{gt_meta}"
    elif mapped_fluid is None:
        blocker = "unmapped_fluid_input"
        missing_identity = str(fluid_inputs[0].get("fluid"))
    elif not item_exists(mapped_in["item"], materials):
        blocker = "missing_input_form"
        missing_form = mapped_in["item"]
    elif outputs and any(mapped is None for mapped in mapped_outs):
        blocker = "unmapped_item_output"
        missing_identity = "unmapped fluidbed output"
    elif outputs:
        absent = [
            mapped["item"]
            for mapped in mapped_outs
            if mapped is not None and not item_exists(mapped["item"], materials)
        ]
        if absent:
            blocker = (
                "missing_dust_div72_form"
                if any(item.endswith("/dust_div72") for item in absent)
                else "missing_output_form"
            )
            missing_form = ",".join(absent)
    elif not needs_outputs:
        blocker = "outputless_fuel_model"
        missing_output_model = "fuels_fluidbed_outputless"
    else:
        blocker = "no_outputs"

    ready = blocker is None
    return {
        "blocker_root": "none" if ready else blocker,
        "chain": CHAIN_FLUIDBED,
        "dump_index": index,
        "family_id": None,
        "freshness": "current",
        "host": "cruciblecraft:fuels_fluidbed",
        "id": f"fluidbed#{index}",
        "mapped_fluid": None if mapped_fluid is None else mapped_fluid["id"],
        "mapped_item": None if mapped_in is None else mapped_in["item"],
        "missing": {
            "form": missing_form,
            "identity": missing_identity,
            "output_model": missing_output_model,
            "shape": None,
        },
        "owner": OWNER,
        "player_path_disposition": "emitted" if ready else "blocked",
        "recipe_map": "gt.recipe.fuels.fluidbed",
        "recheck_point": fluidbed_recheck(blocker),
        "relation_count": 1,
        "replacement_condition": fluidbed_replacement(blocker, missing_identity, missing_form),
        "source": source,
        "source_revision": SOURCE_REVISION,
    }


def fluidbed_replacement(
    blocker: str | None,
    identity: str | None,
    form: str | None,
) -> str:
    if blocker is None:
        return "already_emitted"
    if blocker == "unmapped_storage_dust":
        return (
            "Register the real GT6 storage.dust identity; do not alias it to "
            f"regular dust ({identity})"
        )
    if blocker in {"missing_dust_div72_form", "missing_output_form", "missing_input_form"}:
        return f"Add the real registered form {form}; do not substitute another prefix"
    if blocker == "outputless_fuel_model":
        return (
            "Add a fuels_fluidbed outputless model for GT6 needsOutputs:false "
            "rows; do not invent item output chance"
        )
    return "Keep blocked until the missing GT6 operand exists in CC"


def fluidbed_recheck(blocker: str | None) -> str:
    if blocker is None:
        return "keep emitted JSON unless source dump changes"
    if blocker == "unmapped_storage_dust":
        return "after a CC storage.dust identity exists"
    if blocker in {"missing_dust_div72_form", "missing_output_form", "missing_input_form"}:
        return "after the named dust_div72 / ash form is registered"
    if blocker == "outputless_fuel_model":
        return "after fuels_fluidbed allows empty outputs"
    return "next touching fluidbed / fuels card"


def fluidbed_entries() -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    dump = io.load_json(FLUIDBED_DUMP)
    xref = io.load_json(CROSS_REF)
    materials = load_materials()
    material_ids = xref["material_id_to_cc"]
    fluids = xref["fluid_to_material"]
    needs_outputs = bool(dump.get("needsOutputs"))
    emitted: list[dict[str, Any]] = []
    blocked: list[dict[str, Any]] = []
    for index, recipe in enumerate(dump.get("recipes") or []):
        row = classify_fluidbed_row(
            index,
            recipe,
            material_ids,
            fluids,
            materials,
            needs_outputs,
        )
        if row["player_path_disposition"] == "emitted":
            emitted.append(row)
        else:
            blocked.append(row)
    return emitted, blocked


def bath_entries(path: Path, owner: str, reason_field: str) -> list[dict[str, Any]]:
    document = io.load_json(path)
    rows: list[dict[str, Any]] = []
    for family in document.get("families") or []:
        if family.get("candidate_outcome") != "blocked":
            continue
        axes = list(family.get("blocking_axes") or [])
        root = axes[0] if len(axes) == 1 else "multi_axis"
        if root == "multi_axis" and axes:
            root = ",".join(axes)
        rows.append(
            {
                "blocker_root": root,
                "chain": CHAIN_BATH,
                "dump_index": None,
                "family_id": family["family_id"],
                "freshness": "current",
                "host": family.get("host"),
                "id": family["family_id"],
                "mapped_fluid": None,
                "mapped_item": None,
                "missing": {
                    "form": None,
                    "identity": family.get("candidate_reason") or reason_field,
                    "output_model": None,
                    "shape": None,
                },
                "owner": owner,
                "player_path_disposition": "blocked",
                "recipe_map": "gt.recipe.bath",
                "recheck_point": "next Bath identity / form / object card",
                "relation_count": int(family.get("source_relation_count") or 0),
                "replacement_condition": (
                    "Close the named Bath axis with real identities; "
                    "do not reopen this production lock on the ledger card"
                ),
                "source": io.relative(path),
                "source_revision": SOURCE_REVISION,
            }
        )
    return rows


def petroleum_entries() -> list[dict[str, Any]]:
    proof = io.load_json(PETROLEUM)
    coverage = proof["mixer_petroleum_templates"]["source_denominator_coverage"]
    return [
        {
            "blocker_root": "historical_fluid_item_mapping",
            "chain": CHAIN_PETROLEUM,
            "dump_index": None,
            "family_id": None,
            "freshness": "historical_optional",
            "host": "cruciblecraft:mixer",
            "id": "petroleum-mixer/sampled-blocked",
            "mapped_fluid": None,
            "mapped_item": None,
            "missing": {
                "form": None,
                "identity": (
                    "2026-08-11 sampled mixer operands; subtype counts "
                    f"fluid={coverage['blocked_by_fluid_mapping']} "
                    f"item={coverage['blocked_by_item_mapping']} "
                    f"registration={coverage['blocked_by_item_registration']} "
                    f"chemical={coverage['blocked_by_chemical_fluid_state']} "
                    f"shape={coverage['blocked_by_machine_shape']} "
                    "do not add to 702"
                ),
                "output_model": None,
                "shape": None,
            },
            "owner": "ordinary_optional/petroleum-b4",
            "player_path_disposition": "ordinary_optional",
            "recipe_map": "gt.recipe.mixer",
            "recheck_point": "replay mixer 1057 against current identities",
            "relation_count": int(coverage["blocked"]),
            "replacement_condition": (
                "Do not treat the 2026-08-11 702 as a current total; "
                "replay before any required petroleum mixer wave"
            ),
            "source": io.relative(PETROLEUM),
            "source_revision": SOURCE_REVISION,
        }
    ]


def ordinary_blocked_count() -> dict[str, int]:
    counts: dict[str, int] = {}
    for path in ORDINARY_SELECTIONS:
        document = io.load_json(path)
        blocked = document.get("blocked")
        if isinstance(blocked, list):
            counts[path.parent.parent.name] = len(blocked)
        else:
            coverage = document.get("coverage") or {}
            counts[path.parent.parent.name] = int(coverage.get("blocked") or 0)
    return counts


def capability_out_of_band() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for path in sorted(CAPABILITIES.rglob("capability.json")):
        document = io.load_json(path)
        slug = document["slug"]
        for item in document.get("identity_disposition") or []:
            if item.get("disposition") != "blocked":
                continue
            key = item["semantic_key"]
            freshness = (
                "out_of_scope_external"
                if key in {"reactor:coolant_ic2", "reactor:thorium_salt"}
                else "current"
            )
            rows.append(
                {
                    "blocker_root": key,
                    "capability": slug,
                    "freshness": freshness,
                    "id": f"{slug}/{key}",
                    "owner": slug,
                    "player_path_disposition": "capability_blocked",
                    "reason": item.get("reason"),
                    "source_revision": SOURCE_REVISION,
                }
            )
    return rows


def compile_ledger() -> dict[str, Any]:
    emitted, fluidbed_blocked = fluidbed_entries()
    bath_remainder = bath_entries(
        BATH_CURRENT,
        "bath/remainder",
        "identity_or_form_or_object",
    )
    bath_identity: list[dict[str, Any]] = []
    petroleum = petroleum_entries()
    ordinary = ordinary_blocked_count()
    if any(ordinary.values()):
        raise ValueError(f"ordinary-closure candidate blocked drifted: {ordinary}")
    historical = io.load_json(FLUIDBED_BLOCKED)
    historical_indexes = {
        int(str(row["source"]).rsplit("[", 1)[1].rstrip("]"))
        for row in historical["blocked"]
    }
    live_indexes = {row["dump_index"] for row in fluidbed_blocked}
    if live_indexes != historical_indexes:
        raise ValueError(
            "fluidbed blocked indexes drifted from "
            f"{sorted(historical_indexes)} to {sorted(live_indexes)}"
        )
    if len(emitted) != 6:
        raise ValueError(f"expected 6 emitted fluidbed rows, got {len(emitted)}")
    written_files = sorted(path.name for path in FLUIDBED_OUT.glob("*.json"))
    if len(written_files) != 6:
        raise ValueError(f"expected 6 fluidbed JSON files, got {written_files}")

    remainder_ids = {row["family_id"] for row in bath_remainder}
    identity_ids = {row["family_id"] for row in bath_identity}
    overlap = sorted(remainder_ids & identity_ids)
    current_entries = fluidbed_blocked + bath_remainder + bath_identity
    historical_entries = petroleum
    recipe_entries = current_entries + historical_entries
    current_roots = sorted({row["blocker_root"] for row in current_entries})
    all_roots = sorted({row["blocker_root"] for row in recipe_entries})
    fluidbed_root_counts = Counter(row["blocker_root"] for row in fluidbed_blocked)

    first_chain = {
        "chain": CHAIN_FLUIDBED,
        "decision": "explicitly_blocked",
        "emitted_rows": 6,
        "ready_rows": 0,
        "reason": (
            "No fluidbed blocked row is source-backed ready: storage.dust has "
            "no CC identity, ash/fuel dust_div72 is unregistered, and "
            "needsOutputs:false rows have no outputless fuel model. This card "
            "does not invent forms or fake output chance."
        ),
        "rows": 49,
        "stand_in": False,
    }
    chains = [
        {
            "chain": CHAIN_FLUIDBED,
            "decision": "explicitly_blocked",
            "freshness": "current",
            "next_card": False,
            "player_path": "energy/converter-catalog fluidbed fuels",
            "rank": 1,
            "single_root_closable": False,
            "unit": "rows",
        },
        {
            "chain": CHAIN_BATH,
            "decision": "resolved",
            "freshness": "current",
            "next_card": False,
            "player_path": "bath remainder / identity",
            "rank": 2,
            "single_root_closable": False,
            "unit": "families",
        },
        {
            "chain": CHAIN_PETROLEUM,
            "decision": "historical_optional",
            "freshness": "historical_optional",
            "next_card": False,
            "player_path": "ordinary_optional mixer accessories",
            "rank": 3,
            "single_root_closable": False,
            "unit": "sampled_rows",
        },
    ]
    return {
        "blocker_roots": {
            "all_recipe_entries": all_roots,
            "current_recipe_entries": current_roots,
            "fluidbed": dict(sorted(fluidbed_root_counts.items())),
        },
        "capability_slug": OWNER,
        "chains": chains,
        "counts": {
            "blocked_families": {
                "bath_identity": len(bath_identity),
                "bath_remainder": len(bath_remainder),
                "ordinary_closure": 0,
            },
            "blocked_relations_or_rows": {
                "bath_identity_relations": sum(
                    row["relation_count"] for row in bath_identity
                ),
                "bath_remainder_relations": sum(
                    row["relation_count"] for row in bath_remainder
                ),
                "fluidbed_rows": len(fluidbed_blocked),
                "petroleum_sampled_blocked_rows": 702,
            },
            "do_not_add": True,
            "note": (
                "blocked_families, blocked_relations_or_rows, and blocker_roots "
                "are separate denominators. Bath relations inflate by family "
                "and must not be added to fluidbed 49 or petroleum 702."
            ),
        },
        "entries": recipe_entries,
        "first_chain": first_chain,
        "generated_by": "python tools/blocked_recipe_ledger.py",
        "ordinary_closure_blocked": ordinary,
        "out_of_band": {
            "bath_identity_remainder_family_overlap": overlap,
            "capability_blocked": capability_out_of_band(),
            "measurement_not_recipe_blocked": [
                "remaining_recipe_gap",
                "p2_blocked_count",
                "reachability_blocked",
            ],
            "note": (
                "Capability, verification, and measurement blocked rows stay "
                "out of recipe totals."
            ),
        },
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "unique_active_wave": None,
        "written_fluidbed": {
            "count": 6,
            "files": written_files,
            "indexes": [row["dump_index"] for row in emitted],
        },
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    try:
        ledger = compile_ledger()
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 1
    encoded = dumps(ledger)
    if args.write:
        atomic_io.write_bytes(LEDGER, encoded)
        print(f"Wrote {io.relative(LEDGER)}")
        return 0
    if not LEDGER.is_file():
        print("missing tools/blocked_recipe_ledger.json", file=sys.stderr)
        return 1
    if LEDGER.read_bytes() != encoded:
        print("blocked recipe ledger is stale; run --write", file=sys.stderr)
        return 1
    print("blocked recipe ledger is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
