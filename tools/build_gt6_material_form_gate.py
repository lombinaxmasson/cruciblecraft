#!/usr/bin/env python3
"""Build the committed GT6 recipe-operand projection and material form gate."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import sys
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import material_form_authority as form_authority

TOOLS = ROOT / "tools"
DUMP = ROOT / "gt6_dump" / "gt6_recipe_dump"
MATERIALS = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
OPERANDS_OUT = TOOLS / "gt6_l1b_selected_recipe_operands.json"
ORE_CHAIN_OPERANDS = TOOLS / "gt6_ore_chain_operands.json"
ORE_CHAIN = TOOLS / "gt6_ore_chain.json"
T5_CHEMICAL_POLICY = TOOLS / "chemical_policy.json"
T6_ELECTRICAL_SOURCE = TOOLS / "gt6_electrical_source.json"
T7_MATERIAL_TAG_POLICY = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_tag_policy.json"
)
T8_PIPE_READINESS = TOOLS / "pipe_readiness.json"
T10_PREFLIGHT = TOOLS / "known_ingot_preflight_projection.json"
ROASTER_COMPACT_SOURCE = TOOLS / "roaster_source.json"
ROASTER_COMPACT_REQUIRED_FORMS = TOOLS / "roaster_required_forms.json"
ROASTER_COMPACT_ACQUISITION = TOOLS / "worldgen_source_backed_acquisition.json"
CENTRIFUGE_COMPACT_SOURCE = TOOLS / "centrifuge_source.json"
CENTRIFUGE_COMPACT_REQUIRED_FORMS = TOOLS / "centrifuge_required_forms.json"
ELECTROLYZER_COMPACT_SOURCE = TOOLS / "electrolyzer_source.json"
ELECTROLYZER_COMPACT_REQUIRED_FORMS = TOOLS / "electrolyzer_required_forms.json"
BATH_REMAINDER_SOURCE = TOOLS / "bath_remainder_source.json"
BATH_IDENTITY_REQUIRED_FORMS = TOOLS / "bath_required_forms.json"
TOOL_HEAD_REQUIRED_FORMS = TOOLS / "tool_head_required_forms.json"
T13_RECIPE_MAPS = TOOLS / "machine_tree_denominators" / "recipe_maps.json"
OREDICT_MANIFEST = TOOLS / "gt6_oredict_import_manifest.json"
L3_MATERIALS = TOOLS / "gt6_l3_materials.py"
L3_MATERIALS_OUT = TOOLS / "gt6_l3_prefix_plan.json"
ACCEPTANCE_FORM_CORRECTIONS = (
    TOOLS / "component_rule_sources" / "acceptance_form_corrections.json"
)
GATE_OUT = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(value, ensure_ascii=False, indent=2, sort_keys=True) + "\n"


def compact_json(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def directory_sha256(root: Path) -> str:
    rows = [
        (path.relative_to(root).as_posix(), sha256(path))
        for path in sorted(root.rglob("*.json"))
        if path.is_file()
    ]
    return hashlib.sha256(
        json.dumps(rows, separators=(",", ":"), sort_keys=True).encode("utf-8")
    ).hexdigest()


def tracked_input_hashes() -> dict[str, str]:
    paths = (
        Path(__file__).resolve(),
        TOOLS / "gt6_l1b_selected.json",
        TOOLS / "gt6_oredict_cross_reference.json",
        TOOLS / "gt6_prefix_mapping.json",
        TOOLS / "gt6_material_activation_policy.json",
        ORE_CHAIN,
        ORE_CHAIN_OPERANDS,
        T5_CHEMICAL_POLICY,
        T6_ELECTRICAL_SOURCE,
        T7_MATERIAL_TAG_POLICY,
        T8_PIPE_READINESS,
        ACCEPTANCE_FORM_CORRECTIONS,
        L3_MATERIALS,
        L3_MATERIALS_OUT,
        ROASTER_COMPACT_SOURCE,
        ROASTER_COMPACT_REQUIRED_FORMS,
        ROASTER_COMPACT_ACQUISITION,
        CENTRIFUGE_COMPACT_SOURCE,
        CENTRIFUGE_COMPACT_REQUIRED_FORMS,
        ELECTROLYZER_COMPACT_SOURCE,
        ELECTROLYZER_COMPACT_REQUIRED_FORMS,
        BATH_REMAINDER_SOURCE,
        BATH_IDENTITY_REQUIRED_FORMS,
        TOOL_HEAD_REQUIRED_FORMS,
    )
    result = {
        path.relative_to(ROOT).as_posix(): sha256(path)
        for path in paths
    }
    result["src/main/resources/data/cruciblecraft/materials"] = (
        directory_sha256(MATERIALS)
    )
    return dict(sorted(result.items()))


def semantic_summary(recipe: dict[str, Any], operands: list[dict[str, Any]]) -> str:
    fixed_items = []
    for side in ("inputs", "outputs"):
        for item in recipe.get(side) or []:
            fixed_items.append({
                "side": side,
                "item": str((item or {}).get("item") or ""),
                "meta": (item or {}).get("meta"),
                "count": (item or {}).get("count"),
            })
    fluids = []
    for side in ("fluidInputs", "fluidOutputs"):
        for fluid in recipe.get(side) or []:
            fluids.append({
                "side": side,
                "fluid": str((fluid or {}).get("fluid") or ""),
                "amount": (fluid or {}).get("amount"),
            })
    canonical = {
        "eu_per_tick": recipe.get("euPerTick"),
        "duration": recipe.get("duration"),
        "special_value": recipe.get("specialValue"),
        "items": fixed_items,
        "fluids": fluids,
        "operands": operands,
    }
    encoded = json.dumps(
        canonical, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()[:20]


def material_documents() -> tuple[dict[str, dict[str, Any]], dict[str, set[str]]]:
    sys.path.insert(0, str(TOOLS))
    import gt6_l3_materials  # noqa: WPS433

    l3_document = load(gt6_l3_materials.OUT)
    documents: dict[str, dict[str, Any]] = {}
    forms: dict[str, set[str]] = {}
    for filename in load(MATERIALS / "index.json"):
        document = load(MATERIALS / filename)
        material_id = document["id"]
        documents[material_id] = document
        forms[material_id] = gt6_l3_materials.resolve_material_forms(
            document, l3_document
        )
    return documents, forms


def ore_source_required_forms() -> set[str]:
    sys.path.insert(0, str(TOOLS))
    import gt6_l3_materials  # noqa: WPS433

    return {
        "ore",
        "raw_ore",
        *gt6_l3_materials.prefix_implication_closures()["ore"],
    }


def build_documents() -> tuple[dict[str, Any], dict[str, Any]]:
    selected_path = TOOLS / "gt6_l1b_selected.json"
    cross_path = TOOLS / "gt6_oredict_cross_reference.json"
    mapping_path = TOOLS / "gt6_prefix_mapping.json"
    policy_path = TOOLS / "gt6_material_activation_policy.json"
    selected = load(selected_path)
    cross = load(cross_path)
    policy = load(policy_path)
    t5_policy = load(T5_CHEMICAL_POLICY)
    material_docs, factual_forms = material_documents()

    selected_ids = {
        int(record["source_id"])
        for record in selected["records"].values()
        if record.get("layer") == "CORE" and int(record.get("source_id", -1)) >= 0
    }
    dead_ends = set(
        t5_policy["source_dead_end_policy"]["materials"]
    )
    terminal_materials = set(
        load(ORE_CHAIN)["coverage_ledger"]["sifter_dust_without_smelter"]
    ) - dead_ends
    if len(terminal_materials) != 145:
        raise ValueError(
            "T5 material-form gate denominator drifted: "
            f"{len(terminal_materials)}"
        )
    prefix_by_item = cross["prefix_item_to_gt_prefix"]
    prefix_dictionary = sorted(set(prefix_by_item.values()))
    prefix_indexes = {
        prefix: index for index, prefix in enumerate(prefix_dictionary)
    }
    fluid_map = load(DUMP / "oredict" / "fluid_map.json")
    map_index_path = DUMP / "index.json"
    map_names = sorted(
        entry["nameInternal"]
        for entry in load(map_index_path)["maps"]
        if entry.get("nameInternal")
    )
    map_indexes = {map_name: index for index, map_name in enumerate(map_names)}

    recipes: list[dict[str, Any]] = []
    projected_pairs: set[tuple[int, str]] = set()
    map_hashes: dict[str, str] = {}
    selected_recipe_counts: Counter[str] = Counter()
    unmapped_item_counts: Counter[str] = Counter()

    for map_name in map_names:
        path = DUMP / "maps" / f"{map_name}.json"
        map_hashes[map_name] = sha256(path)
        data = load(path)
        if data.get("nameInternal") != map_name:
            raise ValueError(f"recipe map identity mismatch: {map_name}")
        duplicate_ordinals: Counter[str] = Counter()
        for recipe in data.get("recipes") or []:
            if recipe.get("enabled") is False:
                continue
            operands: set[tuple[int, str, str]] = set()
            touched_ids: set[int] = set()
            for side, gate_side in (("inputs", "input"), ("outputs", "output")):
                for item in recipe.get(side) or []:
                    item_id = str((item or {}).get("item") or "")
                    material_id = (item or {}).get("meta")
                    if not isinstance(material_id, int) or material_id < 0:
                        continue
                    if item_id.startswith("gregtech:gt.meta."):
                        touched_ids.add(material_id)
                    gt_prefix = prefix_by_item.get(item_id)
                    if gt_prefix is None:
                        if item_id.startswith("gregtech:gt.meta."):
                            unmapped_item_counts[item_id] += 1
                        continue
                    operands.add((material_id, gt_prefix, gate_side))
            for side in ("fluidInputs", "fluidOutputs"):
                for fluid in recipe.get(side) or []:
                    mapped = fluid_map.get(str((fluid or {}).get("fluid") or ""))
                    if mapped and int(mapped.get("materialId", -1)) >= 0:
                        touched_ids.add(int(mapped["materialId"]))
            if not (touched_ids & selected_ids):
                continue
            normalized_for_summary = [
                {
                    "source_id": material_id,
                    "gt_prefix": gt_prefix,
                    "side": side,
                }
                for material_id, gt_prefix, side in sorted(operands)
            ]
            summary = semantic_summary(recipe, normalized_for_summary)
            ordinal = duplicate_ordinals[summary]
            duplicate_ordinals[summary] += 1
            recipes.append([
                map_indexes[map_name],
                summary,
                ordinal,
                [
                    [
                        material_id,
                        prefix_indexes[gt_prefix],
                        0 if side == "input" else 1,
                    ]
                    for material_id, gt_prefix, side in sorted(operands)
                ],
            ])
            selected_recipe_counts[map_name] += 1
            projected_pairs.update((material_id, prefix) for material_id, prefix, _ in operands)

    cc_by_source_id = {
        int(source_id): cc_id
        for source_id, cc_id in cross["material_id_to_cc"].items()
    }
    cc_form_by_gt_prefix = cross["gt_prefix_to_cc"]
    roaster_acquisition_document = load(ROASTER_COMPACT_ACQUISITION)
    worldgen_acquisition_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (
            roaster_acquisition_document.get("required_forms") or {}
        ).items()
    }
    chemical_required_forms = {
        material: set(forms)
        for material, forms in t5_policy["required_form_overrides"]["forms"].items()
    }
    t5_scoped_prefixes = {
        form for forms in chemical_required_forms.values() for form in forms
    }
    for material_id, forms in chemical_required_forms.items():
        if material_id not in factual_forms or not forms <= factual_forms[material_id]:
            raise ValueError(
                f"T5 required form exceeds factual runtime forms: "
                f"{material_id}/{sorted(forms)}"
            )
    recipe_forms: dict[str, set[str]] = defaultdict(set)
    ignored_pairs: list[dict[str, Any]] = []
    for source_id, gt_prefix in sorted(projected_pairs):
        material_id = cc_by_source_id.get(source_id)
        cc_form = cc_form_by_gt_prefix.get(gt_prefix)
        reason = None
        if material_id is None:
            reason = "source material is not in the stable imported catalog"
        elif cc_form is None:
            reason = "GT6 prefix has no CC prefix mapping"
        elif cc_form not in factual_forms[material_id]:
            reason = "operand form is absent from the material's GT6 factual forms"
        elif (
            cc_form in t5_scoped_prefixes
            and cc_form not in chemical_required_forms.get(material_id, set())
        ):
            reason = "newly mapped prefix is outside selected T5 source routes"
        if reason is not None:
            ignored_pairs.append({
                "source_id": source_id,
                "gt_prefix": gt_prefix,
                "reason": reason,
            })
            continue
        recipe_forms[material_id].add(cc_form)

    pre_gate_forms = {
        record["cc_id"]: set(record.get("pre_gate_registered_forms") or [])
        for record in policy["records"]
        if record.get("cc_id")
    }
    roaster_acquisition_document = load(ROASTER_COMPACT_ACQUISITION)
    if (
        roaster_acquisition_document.get("status")
        != "ROASTER_COMPACT_SOURCE_BACKED_ACQUISITION_READY"
        or roaster_acquisition_document.get("source_revision")
        != "3703e40308c8c030763fd6297dea8b210d2a77b1"
    ):
        raise ValueError("roaster/compact source-backed acquisition evidence is not ready")
    worldgen_acquisition_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (
            roaster_acquisition_document.get("required_forms") or {}
        ).items()
    }
    source_backed_ore_materials = {
        entry["material"]
        for vein in roaster_acquisition_document.get("veins") or []
        for layer in ("top", "bottom", "between", "spread")
        for entry in vein.get(layer) or []
        if isinstance(entry, dict) and isinstance(entry.get("material"), str)
    }
    if set(worldgen_acquisition_forms) != source_backed_ore_materials:
        raise ValueError("roaster/compact acquisition form coverage does not match source veins")
    for material_id, forms in worldgen_acquisition_forms.items():
        if material_id not in factual_forms:
            raise ValueError(
                f"roaster/compact acquisition references unknown material: {material_id}"
            )
        unsupported = forms - factual_forms[material_id] - {"ore"}
        if unsupported:
            raise ValueError(
                f"roaster/compact acquisition form exceeds factual runtime forms: "
                f"{material_id}/{sorted(unsupported)}"
            )
        if "ore" in forms and material_id not in source_backed_ore_materials:
            raise ValueError(
                f"roaster/compact acquisition ore lacks a pinned source vein: {material_id}"
            )
    ore_chain_forms: dict[str, set[str]] = defaultdict(set)
    if ORE_CHAIN_OPERANDS.is_file():
        for row in load(ORE_CHAIN_OPERANDS)["recipes"]:
            for operand in row["operands"]:
                material_id = operand["material"]
                form = operand["form"]
                if material_id not in factual_forms:
                    raise ValueError(
                        f"ore-chain operand references unknown material: {material_id}"
                    )
                if (
                    form not in factual_forms[material_id]
                    and form not in worldgen_acquisition_forms.get(material_id, set())
                ):
                    raise ValueError(
                        "ore-chain operand exceeds factual or source-backed "
                        f"forms: {material_id}/{form}"
                    )
                ore_chain_forms[material_id].add(form)
    # Ore blocks are a registration source in their own right. Do not infer this
    # domain from crusher/raw-ore recipe operands: only factual `ore` declarations
    # may allocate the paired stone and deepslate registries.
    required_ore_source_forms = ore_source_required_forms()
    ore_source_forms: dict[str, set[str]] = {}
    for material_id, forms in factual_forms.items():
        if "ore" not in forms:
            continue
        missing = required_ore_source_forms - forms
        if missing:
            raise ValueError(
                f"factual ore source lacks required source forms: "
                f"{material_id}/{sorted(missing)}"
            )
        ore_source_forms[material_id] = set(required_ore_source_forms)
    # 4.5 card F: the import's prefix activation (source-backed, 688
    # materials per gt6_material_activation_policy.json) flags rock; the
    # gate selects the rock form wherever the material's factual forms keep
    # it (policy-driven exclude_prefixes drop the other ten).
    rock_forms: dict[str, set[str]] = {}
    for material_id, document in material_docs.items():
        flags = set(document.get("generation_flags") or [])
        if ("cruciblecraft:generates_rock" in flags
                and "rock" in factual_forms[material_id]):
            rock_forms[material_id] = {"rock"}
    acceptance_document = load(ACCEPTANCE_FORM_CORRECTIONS)
    acceptance_forms: dict[str, set[str]] = defaultdict(set)
    for correction in acceptance_document.get("corrections") or []:
        if (
            correction.get("classification")
            != "acceptance_required_not_gt6_original_gate"
        ):
            raise ValueError("acceptance form correction classification drift")
        material_id = correction["material"]
        forms = set(correction["add_forms"])
        if material_id not in factual_forms or not forms <= factual_forms[material_id]:
            raise ValueError(
                f"acceptance form correction exceeds factual runtime forms: "
                f"{material_id}/{sorted(forms)}"
            )
        acceptance_forms[material_id].update(forms)
    electrical_wire_forms: dict[str, set[str]] = {}
    for material_id, document in material_docs.items():
        electrical = (
            document.get("gt6_metadata", {})
            .get("electrical_by_specification", {})
        )
        if "wireGt01" not in electrical:
            continue
        if "wire" not in factual_forms[material_id]:
            raise ValueError(
                f"T6 wireGt01 source exceeds factual forms: {material_id}"
            )
        electrical_wire_forms[material_id] = {"wire"}
    expected_electrical_wires = len(
        load(T6_ELECTRICAL_SOURCE)["conductors"]
    ) - 1  # live catalog intentionally lacks Superconductor
    if len(electrical_wire_forms) != expected_electrical_wires:
        raise ValueError(
            "T6 electrical wire registration denominator drifted: "
            f"{len(electrical_wire_forms)} != {expected_electrical_wires}"
        )
    fluid_pipe_forms = {
        "pipeTiny": "tiny_fluid_pipe",
        "pipeSmall": "small_fluid_pipe",
        "pipeMedium": "fluid_pipe",
        "pipeLarge": "large_fluid_pipe",
        "pipeHuge": "huge_fluid_pipe",
    }
    item_pipe_forms = {
        "pipeMedium": "item_pipe",
        "pipeLarge": "large_item_pipe",
        "pipeHuge": "huge_item_pipe",
    }
    pipe_form_flags = {
        "tiny_fluid_pipe": "cruciblecraft:generates_tiny_fluid_pipe",
        "small_fluid_pipe": "cruciblecraft:generates_small_fluid_pipe",
        "fluid_pipe": "cruciblecraft:generates_fluid_pipe",
        "large_fluid_pipe": "cruciblecraft:generates_large_fluid_pipe",
        "huge_fluid_pipe": "cruciblecraft:generates_huge_fluid_pipe",
        "item_pipe": "cruciblecraft:generates_item_pipe",
        "large_item_pipe": "cruciblecraft:generates_large_item_pipe",
        "huge_item_pipe": "cruciblecraft:generates_huge_item_pipe",
    }
    pipe_forms: dict[str, set[str]] = {}
    for material_id, document in material_docs.items():
        pipe_properties = (
            document.get("gt6_metadata", {}).get("pipe_properties", {})
        )
        forms = {
            fluid_pipe_forms[specification]
            for specification in (
                pipe_properties.get("fluid_by_specification") or {}
            )
        } | {
            item_pipe_forms[specification]
            for specification in (
                pipe_properties.get("item_by_specification") or {}
            )
        }
        if not forms:
            continue
        generation_flags = set(document.get("generation_flags") or [])
        missing_flags = {
            pipe_form_flags[form]
            for form in forms
            if pipe_form_flags[form] not in generation_flags
        }
        if missing_flags:
            raise ValueError(
                f"T8 pipe source lacks generation flags: "
                f"{material_id}/{sorted(missing_flags)}"
            )
        pipe_forms[material_id] = forms
    expected_pipe_forms = int(
        load(T8_PIPE_READINESS)["counts"]["combined_runtime_blocks"]
    )
    actual_pipe_forms = sum(map(len, pipe_forms.values()))
    if actual_pipe_forms != expected_pipe_forms:
        raise ValueError(
            "T8 pipe registration denominator drifted: "
            f"{actual_pipe_forms} != {expected_pipe_forms}"
        )
    t10_projection = load(T10_PREFLIGHT)["route_projections"]
    known_ingot_forms: dict[str, set[str]] = defaultdict(set)
    for material_id in t10_projection["multi_ingot"]["materials"]:
        known_ingot_forms[material_id].update({
            "double_ingot",
            "triple_ingot",
        })
    for material_id in t10_projection["hot_ingot"]["materials"]:
        known_ingot_forms[material_id].add("ingot_hot")
    t10_form_flags = {
        "double_ingot": "gt6:itemgenerator/multiingots",
        "triple_ingot": "gt6:itemgenerator/multiingots",
        "ingot_hot": "gt6:itemgenerator/hotingots",
    }
    for material_id, forms in known_ingot_forms.items():
        flags = set(material_docs[material_id].get("generation_flags") or [])
        missing_flags = {
            t10_form_flags[form]
            for form in forms
            if t10_form_flags[form] not in flags
        }
        if missing_flags:
            raise ValueError(
                f"known-ingot source lacks generation flags: "
                f"{material_id}/{sorted(missing_flags)}"
            )
    actual_known_ingot_forms = sum(map(len, known_ingot_forms.values()))
    if actual_known_ingot_forms != 967:
        raise ValueError(
            "known-ingot registration denominator drifted: "
            f"{actual_known_ingot_forms} != 967"
        )
    roaster_document = load(ROASTER_COMPACT_REQUIRED_FORMS)
    if roaster_document.get("status") != "ROASTER_REQUIRED_FORMS_FROZEN":
        raise ValueError("roaster required-form source is not frozen")
    roaster_source = roaster_document.get("source") or {}
    if (
            roaster_source.get("path") != "tools/roaster_source.json"
            or roaster_source.get("sha256") != sha256(ROASTER_COMPACT_SOURCE)
    ):
        raise ValueError("roaster required forms do not bind the frozen compact source")
    roaster_required_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (roaster_document.get("required_forms") or {}).items()
    }
    for material_id, forms in roaster_required_forms.items():
        bounded_post_import_forms = {"dust_div72", "small_dust"}
        unsupported = (
            forms - factual_forms.get(material_id, set()) - bounded_post_import_forms
        )
        if (
            material_id not in factual_forms
            or unsupported
            or (
                bool(forms & bounded_post_import_forms)
                and "dust" not in factual_forms[material_id]
            )
        ):
            raise ValueError(
                f"roaster required form exceeds factual or bounded post-import "
                f"forms: {material_id}/{sorted(forms)}"
            )
    centrifuge_document = load(CENTRIFUGE_COMPACT_REQUIRED_FORMS)
    if centrifuge_document.get("status") != "CENTRIFUGE_REQUIRED_FORMS_FROZEN":
        raise ValueError("centrifuge required-form source is not frozen")
    centrifuge_source = centrifuge_document.get("source") or {}
    if (
            centrifuge_source.get("path") != "tools/centrifuge_source.json"
            or centrifuge_source.get("sha256") != sha256(CENTRIFUGE_COMPACT_SOURCE)
    ):
        raise ValueError("centrifuge required forms do not bind the frozen compact source")
    centrifuge_required_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (centrifuge_document.get("required_forms") or {}).items()
    }
    for material_id, forms in centrifuge_required_forms.items():
        bounded_post_import_forms = {
            "dust_div72",
            "nugget",
            "small_dust",
            "tiny_centrifuged_crushed_ore",
        }
        unsupported = (
            forms - factual_forms.get(material_id, set()) - bounded_post_import_forms
        )
        if (
            material_id not in factual_forms
            or unsupported
            or (
                bool(forms & {"dust_div72", "small_dust"})
                and "dust" not in factual_forms[material_id]
            )
        ):
            raise ValueError(
                f"centrifuge required form exceeds factual or bounded post-import "
                f"forms: {material_id}/{sorted(forms)}"
            )
    electrolyzer_document = load(ELECTROLYZER_COMPACT_REQUIRED_FORMS)
    if electrolyzer_document.get("status") != "ELECTROLYZER_REQUIRED_FORMS_FROZEN":
        raise ValueError("electrolyzer required-form source is not frozen")
    electrolyzer_source = electrolyzer_document.get("source") or {}
    if (
            electrolyzer_source.get("path") != "tools/electrolyzer_source.json"
            or electrolyzer_source.get("sha256") != sha256(ELECTROLYZER_COMPACT_SOURCE)
    ):
        raise ValueError("electrolyzer required forms do not bind the frozen compact source")
    electrolyzer_required_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (electrolyzer_document.get("required_forms") or {}).items()
    }
    for material_id, forms in electrolyzer_required_forms.items():
        bounded_post_import_forms = {"small_dust"}
        unsupported = (
            forms - factual_forms.get(material_id, set()) - bounded_post_import_forms
        )
        if (
            material_id not in factual_forms
            or unsupported
            or (
                bool(forms & bounded_post_import_forms)
                and "dust" not in factual_forms[material_id]
            )
        ):
            raise ValueError(
                f"electrolyzer required form exceeds factual or bounded post-import "
                f"forms: {material_id}/{sorted(forms)}"
            )
    bath_identity_document = load(BATH_IDENTITY_REQUIRED_FORMS)
    if bath_identity_document.get("status") != "BATH_REQUIRED_FORMS_FROZEN":
        raise ValueError("bath required-form source is not frozen")
    bath_identity_source = bath_identity_document.get("source") or {}
    if (
            bath_identity_source.get("path") != "tools/bath_remainder_source.json"
            or bath_identity_source.get("sha256") != sha256(BATH_REMAINDER_SOURCE)
    ):
        raise ValueError("bath required forms do not bind the compact source scan")
    bath_required_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (bath_identity_document.get("required_forms") or {}).items()
    }
    bath_identity_authority = form_authority.source_by_id("bath_compact_required_forms")
    bounded_bath_identity_forms = set(bath_identity_authority.get("extra_factual_forms") or [])
    for material_id, forms in bath_required_forms.items():
        unsupported = (
            forms - factual_forms.get(material_id, set()) - bounded_bath_identity_forms
        )
        if material_id not in factual_forms or unsupported:
            raise ValueError(
                f"bath required form exceeds factual or bounded post-import "
                f"forms: {material_id}/{sorted(forms)}"
            )
    tool_head_document = load(TOOL_HEAD_REQUIRED_FORMS)
    if tool_head_document.get("status") != "TOOL_HEAD_REQUIRED_FORMS":
        raise ValueError("tool-head required-form source is not frozen")
    tool_head_required_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (tool_head_document.get("required_forms") or {}).items()
    }
    tool_head_authority = form_authority.source_by_id("tool_head_prefix_required_forms")
    bounded_tool_head_forms = set(tool_head_authority.get("extra_factual_forms") or [])
    for material_id, forms in tool_head_required_forms.items():
        unsupported = (
            forms - factual_forms.get(material_id, set()) - bounded_tool_head_forms
        )
        if material_id not in factual_forms or unsupported:
            raise ValueError(
                f"tool-head required form exceeds factual or bounded post-import "
                f"forms: {material_id}/{sorted(unsupported or forms)}"
            )
    semantic_required_forms: dict[str, set[str]] = defaultdict(set)
    semantic_sections: dict[str, dict[str, list[str]]] = {}
    semantic_source_meta: dict[str, dict[str, Any]] = {}
    for source in form_authority.load_authority().get("sources") or []:
        source_id = str(source.get("id") or "")
        semantic_wave = str(source.get("path") or "").replace("\\", "/").startswith(
            "tools/waves/"
        )
        if not semantic_wave and not source_id.endswith("_ordinary_required_forms"):
            continue
        source_path = ROOT / str(source["path"])
        if not source_path.is_file():
            continue
        semantic_document = load(source_path)
        forms_map = {
            str(material_id): set(forms)
            for material_id, forms in (semantic_document.get("required_forms") or {}).items()
        }
        extra = set(source.get("extra_factual_forms") or [])
        for material_id, forms in forms_map.items():
            unsupported = forms - factual_forms.get(material_id, set()) - extra
            if material_id not in factual_forms or unsupported:
                raise ValueError(
                    f"{source_id} required form exceeds factual or bounded post-import "
                    f"forms: {material_id}/{sorted(unsupported or forms)}"
                )
            semantic_required_forms[material_id].update(forms)
        section = str(source["gate_section"])
        semantic_sections[section] = {
            material: sorted(forms) for material, forms in sorted(forms_map.items())
        }
        semantic_source_meta[section] = {
            "classification": "semantic_ordinary_runtime_required",
            "field": "required_forms",
            "path": str(source["path"]),
            "sha256": sha256(source_path),
        }
    compatibility_forms: dict[str, list[str]] = {}
    gated_forms: dict[str, list[str]] = {}
    for material_id in sorted(material_docs):
        if material_docs[material_id].get("metadata_only"):
            gated_forms[material_id] = []
            continue
        factual = factual_forms[material_id]
        recipe_selected = recipe_forms.get(material_id, set())
        ore_chain_selected = ore_chain_forms.get(material_id, set())
        ore_source_selected = ore_source_forms.get(material_id, set())
        acceptance_selected = acceptance_forms.get(material_id, set())
        selected_forms = (
            recipe_selected
            | ore_chain_selected
            | ore_source_selected
            | acceptance_selected
            | chemical_required_forms.get(material_id, set())
            | electrical_wire_forms.get(material_id, set())
            | pipe_forms.get(material_id, set())
            | known_ingot_forms.get(material_id, set())
            | roaster_required_forms.get(material_id, set())
            | worldgen_acquisition_forms.get(material_id, set())
            | centrifuge_required_forms.get(material_id, set())
            | electrolyzer_required_forms.get(material_id, set())
            | bath_required_forms.get(material_id, set())
            | tool_head_required_forms.get(material_id, set())
            | semantic_required_forms.get(material_id, set())
            | rock_forms.get(material_id, set())
            | (
                factual
                & {
                    "machine_casing",
                    "machine_casing_double",
                    "machine_casing_quadruple",
                }
            )
        )
        compatibility = ((
            pre_gate_forms.get(material_id, set()) - selected_forms
        ) & factual) - t5_scoped_prefixes
        if compatibility:
            compatibility_forms[material_id] = sorted(compatibility)
        gated_forms[material_id] = sorted(selected_forms | compatibility)

    inputs = {
        "selection": {
            "path": "tools/gt6_l1b_selected.json",
            "sha256": sha256(selected_path),
            "selected_layer": "CORE",
            "chemical_terminal_path": "tools/gt6_ore_chain.json",
            "chemical_terminal_sha256": sha256(ORE_CHAIN),
            "chemical_policy_path": "tools/chemical_policy.json",
            "chemical_policy_sha256": sha256(T5_CHEMICAL_POLICY),
            "chemical_terminal_materials": len(terminal_materials),
        },
        "prefix_mapping": {
            "path": "tools/gt6_prefix_mapping.json",
            "sha256": sha256(mapping_path),
        },
        "cross_reference": {
            "path": "tools/gt6_oredict_cross_reference.json",
            "sha256": sha256(cross_path),
        },
        "material_policy": {
            "path": "tools/gt6_material_activation_policy.json",
            "sha256": sha256(policy_path),
        },
        "recipe_map_index": {
            "path": "gt6_dump/gt6_recipe_dump/index.json",
            "sha256": sha256(map_index_path),
        },
        "recipe_maps": map_hashes,
        "fluid_map": {
            "path": "gt6_dump/gt6_recipe_dump/oredict/fluid_map.json",
            "sha256": sha256(DUMP / "oredict" / "fluid_map.json"),
        },
        "tracked": tracked_input_hashes(),
    }
    operand_document = {
        "schema_version": 1,
        "encoding": {
            "recipe": "[map_index, semantic_summary, duplicate_ordinal, operands]",
            "operand": "[source_id, gt_prefix_index, side_index]",
            "side_dictionary": ["input", "output"],
            "map_dictionary": map_names,
            "gt_prefix_dictionary": prefix_dictionary,
        },
        "selection_rule": (
            "enabled recipe touches at least one CORE material; project all mappable "
            "GT6 material-prefix item operands"
        ),
        "inputs": inputs,
        "counts": {
            "selected_materials": len(selected_ids),
            "selected_recipes": len(recipes),
            "projected_material_prefix_pairs": len(projected_pairs),
            "ignored_gate_pairs": len(ignored_pairs),
        },
        "selected_recipes_by_map": dict(sorted(selected_recipe_counts.items())),
        "unmapped_gt_meta_items": dict(sorted(unmapped_item_counts.items())),
        "ignored_gate_pairs": ignored_pairs,
        "recipes": recipes,
    }
    gate_document = {
        "schema_version": 2,
        "authority": {
            "path": "tools/material_form_authority.json",
            "semantic_root_sha256": form_authority.semantic_root_sha256(),
        },
        "java_overlay_sections": form_authority.java_overlay_sections(),
        "typed_ore_denominators": dict(
            form_authority.load_authority().get("typed_ore_denominators") or {}
        ),
        "sources": {
            "l1b_recipe_operands": {
                "path": "tools/gt6_l1b_selected_recipe_operands.json",
                "sha256": hashlib.sha256(
                    compact_json(operand_document).encode("utf-8")
                ).hexdigest(),
            },
            "ore_chain_operands": (
                {
                    "path": "tools/gt6_ore_chain_operands.json",
                    "sha256": sha256(ORE_CHAIN_OPERANDS),
                }
                if ORE_CHAIN_OPERANDS.is_file()
                else None
            ),
            "factual_ore_sources": {
                "path": "src/main/resources/data/cruciblecraft/materials/*.json",
                "field": "resolved factual prefix `ore`",
            },
            "acceptance_form_corrections": {
                "path": (
                    "tools/component_rule_sources/"
                    "acceptance_form_corrections.json"
                ),
                "sha256": sha256(ACCEPTANCE_FORM_CORRECTIONS),
                "classification": (
                    "acceptance_required_not_gt6_original_gate"
                ),
            },
            "chemical_selected_source_route_forms": {
                "path": "tools/chemical_policy.json",
                "field": "required_form_overrides.forms",
                "sha256": sha256(T5_CHEMICAL_POLICY),
                "classification": "chemical_selected_source_route_required",
            },
            "electrical_source_backed_wire_forms": {
                "path": "tools/gt6_electrical_source.json",
                "field": "conductors[] plus live material wireGt01 specification",
                "sha256": sha256(T6_ELECTRICAL_SOURCE),
                "classification": "electrical_source_backed_runtime_required",
            },
            "material_tag_policy": {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "material_tag_policy.json"
                ),
                "field": "tags[] classification and acceptance",
                "sha256": sha256(T7_MATERIAL_TAG_POLICY),
                "classification": "audit_only_non_form_source",
            },
            "pipe_source_backed_pipe_forms": {
                "path": "tools/pipe_readiness.json",
                "field": "fluid_domain/item_domain material_catalog",
                "sha256": sha256(T8_PIPE_READINESS),
                "classification": "pipe_source_backed_runtime_required",
            },
            "known_ingot_forms": {
                "path": "tools/known_ingot_preflight_projection.json",
                "field": "route_projections.multi_ingot/hot_ingot.materials",
                "multi_material_set_sha256": t10_projection[
                    "multi_ingot"
                ]["material_set_sha256"],
                "hot_material_set_sha256": t10_projection[
                    "hot_ingot"
                ]["material_set_sha256"],
                "classification": "known_ingot_source_domain_runtime_required",
            },
            "roaster_compact_required_forms": {
                "path": "tools/roaster_required_forms.json",
                "field": "required_forms",
                "sha256": sha256(ROASTER_COMPACT_REQUIRED_FORMS),
                "source_path": "tools/roaster_source.json",
                "source_sha256": sha256(ROASTER_COMPACT_SOURCE),
                "classification": "roaster_compact_output_runtime_required",
            },
            "worldgen_acquisition_forms": {
                "path": "tools/worldgen_source_backed_acquisition.json",
                "field": "required_forms",
                "sha256": sha256(ROASTER_COMPACT_ACQUISITION),
                "classification": "source_backed_worldgen_runtime_required",
            },
            "centrifuge_compact_required_forms": {
                "path": "tools/centrifuge_required_forms.json",
                "field": "required_forms",
                "sha256": sha256(CENTRIFUGE_COMPACT_REQUIRED_FORMS),
                "source_path": "tools/centrifuge_source.json",
                "source_sha256": sha256(CENTRIFUGE_COMPACT_SOURCE),
                "classification": "centrifuge_compact_output_runtime_required",
            },
            "electrolyzer_compact_required_forms": {
                "path": "tools/electrolyzer_required_forms.json",
                "field": "required_forms",
                "sha256": sha256(ELECTROLYZER_COMPACT_REQUIRED_FORMS),
                "source_path": "tools/electrolyzer_source.json",
                "source_sha256": sha256(ELECTROLYZER_COMPACT_SOURCE),
                "classification": "electrolyzer_compact_output_runtime_required",
            },
            "bath_compact_required_forms": {
                "path": "tools/bath_required_forms.json",
                "field": "required_forms",
                "sha256": sha256(BATH_IDENTITY_REQUIRED_FORMS),
                "source_path": "tools/bath_remainder_source.json",
                "source_sha256": sha256(BATH_REMAINDER_SOURCE),
                "classification": "bath_compact_output_runtime_required",
            },
            "tool_head_prefix_required_forms": {
                "path": "tools/tool_head_required_forms.json",
                "field": "required_forms",
                "sha256": sha256(TOOL_HEAD_REQUIRED_FORMS),
                "classification": "tool_head_prefix_runtime_required",
            },
            **semantic_source_meta,
        },
        "policy": (
            "recipe-projected forms plus factual ore-source closure registrations and "
            "route-scoped chemical forms plus source-backed electrical wireGt01 forms plus explicit "
            "source-backed pipe forms plus known multi/hot ingot forms plus bounded "
            "roaster compact-output forms plus source-backed worldgen acquisition forms plus "
            "bounded centrifuge compact-output forms plus bounded electrolyzer compact-output forms plus "
            "compatibility retention from the pre-gate registered catalog"
        ),
        "compatibility_baseline": {
            "path": "tools/gt6_material_activation_policy.json",
            "sha256": sha256(policy_path),
            "field": "records[].pre_gate_registered_forms",
            "recursive": False,
            "note": (
                "audit-only legacy baseline; the builder never reads the previous "
                "material_registration_gate.json"
            ),
        },
        "counts": {
            "materials": len(gated_forms),
            "metadata_only_materials": sum(
                bool(document.get("metadata_only")) for document in material_docs.values()
            ),
            "recipe_gated_forms": sum(map(len, recipe_forms.values())),
            "ore_chain_forms": sum(map(len, ore_chain_forms.values())),
            "ore_source_materials": len(ore_source_forms),
            "ore_source_forms": sum(map(len, ore_source_forms.values())),
            "ore_source_blocks": len(ore_source_forms) * 2,
            "acceptance_forms": sum(map(len, acceptance_forms.values())),
            "chemical_required_forms": sum(map(len, chemical_required_forms.values())),
            "electrical_wire_forms": sum(
                map(len, electrical_wire_forms.values())
            ),
            "pipe_forms": actual_pipe_forms,
            "known_ingot_forms": actual_known_ingot_forms,
            "roaster_required_forms": sum(map(len, roaster_required_forms.values())),
            "worldgen_acquisition_forms": sum(
                map(len, worldgen_acquisition_forms.values())
            ),
            "centrifuge_required_forms": sum(map(len, centrifuge_required_forms.values())),
            "electrolyzer_required_forms": sum(map(len, electrolyzer_required_forms.values())),
            "bath_required_forms": sum(map(len, bath_required_forms.values())),
            "tool_head_required_forms": sum(map(len, tool_head_required_forms.values())),
            "compatibility_forms": sum(map(len, compatibility_forms.values())),
            "registered_forms": sum(map(len, gated_forms.values())),
        },
        "compatibility_forms": compatibility_forms,
        "acceptance_forms": {
            material: sorted(forms)
            for material, forms in sorted(acceptance_forms.items())
        },
        "chemical_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(chemical_required_forms.items())
        },
        "electrical_wire_forms": {
            material: sorted(forms)
            for material, forms in sorted(electrical_wire_forms.items())
        },
        "pipe_forms": {
            material: sorted(forms)
            for material, forms in sorted(pipe_forms.items())
        },
        "known_ingot_forms": {
            material: sorted(forms)
            for material, forms in sorted(known_ingot_forms.items())
        },
        "roaster_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(roaster_required_forms.items())
        },
        "worldgen_acquisition_forms": {
            material: sorted(forms)
            for material, forms in sorted(worldgen_acquisition_forms.items())
        },
        "centrifuge_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(centrifuge_required_forms.items())
        },
        "electrolyzer_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(electrolyzer_required_forms.items())
        },
        "bath_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(bath_required_forms.items())
        },
        "tool_head_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(tool_head_required_forms.items())
        },
        **semantic_sections,
        "materials": gated_forms,
    }
    return operand_document, gate_document


def _object_span(text: str, open_idx: int) -> tuple[int, int]:
    depth = 0
    for index in range(open_idx, len(text)):
        char = text[index]
        if char == "{":
            depth += 1
        elif char == "}":
            depth -= 1
            if depth == 0:
                return open_idx, index + 1
    raise ValueError("unterminated JSON object")


def patch_ordinary_casing_family() -> dict[str, int]:
    """Register quadruple casings beside every gated ordinary casing.

    Full ``--write`` needs dump-backed roaster evidence. This keeps the live
    gate aligned with ``generates_machine_casing`` without replaying maps.
    """
    from tools import currentness

    text = GATE_OUT.read_text(encoding="utf-8")
    data = json.loads(text)
    added = 0
    marker = '\n  "materials": {'
    start = text.index(marker) + 1
    _, end = _object_span(text, text.index("{", start))
    section = text[start:end]
    materials = data.get("materials") or {}
    targets = [
        material
        for material, forms in materials.items()
        if "machine_casing" in forms and "machine_casing_quadruple" not in forms
    ]
    for material in targets:
        old_forms = list(materials[material])
        new_forms = sorted([*old_forms, "machine_casing_quadruple"])
        materials[material] = new_forms
        added += 1
        pattern = (
            rf'(    "{re.escape(material)}": \[\n'
            rf'      "{re.escape(old_forms[0])}",[\s\S]*?\n    \])'
        )
        replacement = (
            f'    "{material}": [\n'
            + ",\n".join(f'      "{form}"' for form in new_forms)
            + "\n    ]"
        )
        section, count = re.subn(pattern, replacement, section, count=1)
        if count != 1:
            raise ValueError(f"failed to patch materials forms for {material}")
    registered = int(data["counts"]["registered_forms"])
    if added:
        text = text[:start] + section + text[end:]
        text = text.replace(
            f'"registered_forms": {registered}',
            f'"registered_forms": {registered + added}',
            1,
        )
        json.loads(text)
        check_or_write(GATE_OUT, text, True)
        currentness.write_sidecar(GATE_OUT)
        registered += added
    return {
        "added": added,
        "targets": len(targets),
        "registered_forms": registered,
    }


def check_or_write(path: Path, content: str, write: bool) -> bool:
    if write:
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary = path.with_name(f".{path.name}.{os.getpid()}.tmp")
        try:
            temporary.write_bytes(content.encode("utf-8"))
            os.replace(temporary, path)
        finally:
            temporary.unlink(missing_ok=True)
        return True
    return path.is_file() and path.read_text(encoding="utf-8") == content


def check_committed_outputs() -> list[str]:
    errors: list[str] = []
    if not OPERANDS_OUT.is_file() or not GATE_OUT.is_file():
        return ["material form gate compact artifacts are missing"]
    try:
        operands = load(OPERANDS_OUT)
        gate = load(GATE_OUT)
        authority_owned = "authority" in gate
        if OPERANDS_OUT.read_text(encoding="utf-8") != compact_json(operands):
            errors.append("operand artifact is not canonical compact JSON")
        if (
            not authority_owned
            and GATE_OUT.read_text(encoding="utf-8") != stable_json(gate)
        ):
            errors.append("material gate is not canonical stable JSON")
        inputs = operands.get("inputs") or {}
        if (
            not authority_owned
            and inputs.get("tracked") != tracked_input_hashes()
        ):
            errors.append("tracked material gate inputs drifted")

        recipe_maps = load(T13_RECIPE_MAPS)
        receipt = recipe_maps.get("full_replay_receipt") or {}
        expected_maps = {
            row["name_internal"]: row["source_blob"]
            for row in recipe_maps.get("rows") or []
            if row.get("name_internal")
        }
        if inputs.get("recipe_maps") != dict(sorted(expected_maps.items())):
            errors.append("recipe-map hash projection differs from T13 evidence")
        if (
            (inputs.get("recipe_map_index") or {}).get("sha256")
            != receipt.get("dump_index_sha256")
        ):
            errors.append("recipe-map index hash differs from T13 receipt")
        oredict = load(OREDICT_MANIFEST)
        expected_fluid_map = oredict["provenance"]["sha256"]["fluid_map.json"]
        if (
            (inputs.get("fluid_map") or {}).get("sha256")
            != expected_fluid_map
        ):
            errors.append("fluid-map hash differs from OreDict evidence")

        sources = gate.get("sources") or {}
        expected_source_paths = {
            "chemical_selected_source_route_forms": "tools/chemical_policy.json",
            "pipe_source_backed_pipe_forms": "tools/pipe_readiness.json",
            "known_ingot_forms": "tools/known_ingot_preflight_projection.json",
            "roaster_compact_required_forms": "tools/roaster_required_forms.json",
            "worldgen_acquisition_forms": (
                "tools/worldgen_source_backed_acquisition.json"
            ),
            "centrifuge_compact_required_forms": (
                "tools/centrifuge_required_forms.json"
            ),
            "electrolyzer_compact_required_forms": (
                "tools/electrolyzer_required_forms.json"
            ),
            "bath_compact_required_forms": "tools/bath_required_forms.json",
            "tool_head_prefix_required_forms": (
                "tools/tool_head_required_forms.json"
            ),
        }
        for name, expected_path in expected_source_paths.items():
            if (sources.get(name) or {}).get("path") != expected_path:
                errors.append(f"material gate source path drifted: {name}")
        expected_source_bindings = {
            "roaster_compact_required_forms": "tools/roaster_source.json",
            "centrifuge_compact_required_forms": "tools/centrifuge_source.json",
            "electrolyzer_compact_required_forms": (
                "tools/electrolyzer_source.json"
            ),
            "bath_compact_required_forms": "tools/bath_remainder_source.json",
        }
        for name, expected_source in expected_source_bindings.items():
            if (sources.get(name) or {}).get("source_path") != expected_source:
                errors.append(f"material gate compact source drifted: {name}")
        if not authority_owned:
            if (
                (sources.get("l1b_recipe_operands") or {}).get("sha256")
                != sha256(OPERANDS_OUT)
            ):
                errors.append("material gate does not bind the operand artifact")
            expected_source_hashes = {
                "ore_chain_operands": sha256(ORE_CHAIN_OPERANDS),
                "acceptance_form_corrections": sha256(
                    ACCEPTANCE_FORM_CORRECTIONS
                ),
                "chemical_selected_source_route_forms": sha256(
                    T5_CHEMICAL_POLICY
                ),
                "electrical_source_backed_wire_forms": sha256(
                    T6_ELECTRICAL_SOURCE
                ),
                "material_tag_policy": sha256(T7_MATERIAL_TAG_POLICY),
                "pipe_source_backed_pipe_forms": sha256(T8_PIPE_READINESS),
                "roaster_compact_required_forms": sha256(ROASTER_COMPACT_REQUIRED_FORMS),
                "worldgen_acquisition_forms": sha256(ROASTER_COMPACT_ACQUISITION),
                "centrifuge_compact_required_forms": sha256(CENTRIFUGE_COMPACT_REQUIRED_FORMS),
                "electrolyzer_compact_required_forms": sha256(ELECTROLYZER_COMPACT_REQUIRED_FORMS),
                "bath_compact_required_forms": sha256(BATH_IDENTITY_REQUIRED_FORMS),
                "tool_head_prefix_required_forms": sha256(
                    TOOL_HEAD_REQUIRED_FORMS
                ),
            }
            for name, expected in expected_source_hashes.items():
                if (sources.get(name) or {}).get("sha256") != expected:
                    errors.append(f"material gate source hash drifted: {name}")
            t10 = load(T10_PREFLIGHT)["route_projections"]
            t10_source = sources.get("known_ingot_forms") or {}
            if (
                t10_source.get("multi_material_set_sha256")
                != t10["multi_ingot"]["material_set_sha256"]
                or t10_source.get("hot_material_set_sha256")
                != t10["hot_ingot"]["material_set_sha256"]
            ):
                errors.append("material gate known-ingot sets drifted")
        acquisition_source = sources.get(
            "worldgen_acquisition_forms"
        ) or {}
        if acquisition_source.get("field") != "required_forms":
            errors.append("material gate worldgen acquisition form source drifted")

        operand_counts = operands.get("counts") or {}
        if (
            operand_counts.get("selected_recipes")
            != len(operands.get("recipes") or [])
            or sum(
                (operands.get("selected_recipes_by_map") or {}).values()
            )
            != operand_counts.get("selected_recipes")
        ):
            errors.append("material operand counts are inconsistent")
        materials = gate.get("materials") or {}
        gate_counts = gate.get("counts") or {}
        if (
            gate_counts.get("materials") != len(materials)
            or gate_counts.get("registered_forms")
            != sum(len(forms) for forms in materials.values())
            or gate_counts.get("compatibility_forms")
            != sum(
                len(forms)
                for forms in (gate.get("compatibility_forms") or {}).values()
            )
        ):
            errors.append("material gate counts are inconsistent")
    except (KeyError, OSError, TypeError, ValueError, json.JSONDecodeError) as exc:
        errors.append(str(exc))
    return errors


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--review", action="store_true")
    parser.add_argument(
        "--patch-casing-family",
        action="store_true",
        help=(
            "register quadruple casings for every gated ordinary casing "
            "without a dump replay"
        ),
    )
    replay_mode = parser.add_mutually_exclusive_group()
    replay_mode.add_argument(
        "--reference-only",
        action="store_true",
        help="validate committed compact evidence without reading gt6_dump",
    )
    replay_mode.add_argument(
        "--full-replay",
        action="store_true",
        help="force the raw GT6 recipe operand replay",
    )
    args = parser.parse_args()
    if args.write and args.check:
        parser.error("--write and --check are mutually exclusive")
    if args.patch_casing_family and (args.write or args.check or args.review):
        parser.error("--patch-casing-family cannot combine with --write/--check/--review")
    if args.patch_casing_family:
        result = patch_ordinary_casing_family()
        print(json.dumps(result, sort_keys=True))
        return 0
    if (args.reference_only or args.full_replay) and not args.check:
        parser.error("--reference-only and --full-replay require --check")
    if args.check and args.reference_only:
        errors = check_committed_outputs()
        if errors:
            print(
                "Compact material form gate validation failed:\n"
                + "\n".join(f"- {error}" for error in errors),
                file=sys.stderr,
            )
            return 1
        print("Compact GT6 material form gate artifacts are internally current")
        return 0
    operands, gate = build_documents()
    if args.review:
        print(stable_json({
            "operand_counts": operands["counts"],
            "gate_counts": gate["counts"],
            "selected_recipes_by_map": operands["selected_recipes_by_map"],
            "compatibility_materials": len(gate["compatibility_forms"]),
        }), end="")
        return 0
    write = args.write
    results = [
        check_or_write(OPERANDS_OUT, compact_json(operands), write),
        check_or_write(GATE_OUT, stable_json(gate), write),
    ]
    if write:
        print(f"Wrote {OPERANDS_OUT}")
        print(f"Wrote {GATE_OUT}")
        return 0
    if all(results):
        print("GT6 material form gate artifacts are current")
        return 0
    print("GT6 material form gate artifacts are stale; run with --write", file=sys.stderr)
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
