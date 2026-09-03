#!/usr/bin/env python3
"""Build the committed GT6 recipe-operand projection and material form gate."""
from __future__ import annotations

import argparse
import hashlib
import json
import os
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
T5_CHEMICAL_POLICY = TOOLS / "t5_chemical_policy.json"
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
T8_PIPE_READINESS = TOOLS / "t8_pipe_readiness.json"
T10_PREFLIGHT = TOOLS / "t10_preflight_projection.json"
T38_SOURCE = TOOLS / "t38_roaster_source.json"
T38_REQUIRED_FORMS = TOOLS / "t38_required_forms.json"
T38_ACQUISITION = TOOLS / "t38_source_backed_acquisition.json"
T39_SOURCE = TOOLS / "t39_centrifuge_source.json"
T39_REQUIRED_FORMS = TOOLS / "t39_required_forms.json"
T40_SOURCE = TOOLS / "t40_electrolyzer_source.json"
T40_REQUIRED_FORMS = TOOLS / "t40_required_forms.json"
T47_SOURCE = TOOLS / "t47_bath_source.json"
T48_REQUIRED_FORMS = TOOLS / "t48_required_forms.json"
TOOL_HEAD_REQUIRED_FORMS = TOOLS / "tool_head_required_forms.json"
T13_RECIPE_MAPS = TOOLS / "t13_denominators" / "recipe_maps.json"
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
        T38_SOURCE,
        T38_REQUIRED_FORMS,
        T38_ACQUISITION,
        T39_SOURCE,
        T39_REQUIRED_FORMS,
        T40_SOURCE,
        T40_REQUIRED_FORMS,
        T47_SOURCE,
        T48_REQUIRED_FORMS,
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
    t38_acquisition_document = load(T38_ACQUISITION)
    t38_acquisition_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (
            t38_acquisition_document.get("required_forms") or {}
        ).items()
    }
    t5_required_forms = {
        material: set(forms)
        for material, forms in t5_policy["required_form_overrides"]["forms"].items()
    }
    t5_scoped_prefixes = {
        form for forms in t5_required_forms.values() for form in forms
    }
    for material_id, forms in t5_required_forms.items():
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
            and cc_form not in t5_required_forms.get(material_id, set())
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
    t38_acquisition_document = load(T38_ACQUISITION)
    if (
        t38_acquisition_document.get("status")
        != "T38_SOURCE_BACKED_ACQUISITION_READY"
        or t38_acquisition_document.get("source_revision")
        != "3703e40308c8c030763fd6297dea8b210d2a77b1"
    ):
        raise ValueError("T38 source-backed acquisition evidence is not ready")
    t38_acquisition_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (
            t38_acquisition_document.get("required_forms") or {}
        ).items()
    }
    source_backed_ore_materials = {
        entry["material"]
        for vein in t38_acquisition_document.get("veins") or []
        for layer in ("top", "bottom", "between", "spread")
        for entry in vein.get(layer) or []
        if isinstance(entry, dict) and isinstance(entry.get("material"), str)
    }
    if set(t38_acquisition_forms) != source_backed_ore_materials:
        raise ValueError("T38 acquisition form coverage does not match source veins")
    for material_id, forms in t38_acquisition_forms.items():
        if material_id not in factual_forms:
            raise ValueError(
                f"T38 acquisition references unknown material: {material_id}"
            )
        unsupported = forms - factual_forms[material_id] - {"ore"}
        if unsupported:
            raise ValueError(
                f"T38 acquisition form exceeds factual runtime forms: "
                f"{material_id}/{sorted(unsupported)}"
            )
        if "ore" in forms and material_id not in source_backed_ore_materials:
            raise ValueError(
                f"T38 acquisition ore lacks a pinned source vein: {material_id}"
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
                    and form not in t38_acquisition_forms.get(material_id, set())
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
            != "t3_acceptance_required_not_gt6_original_gate"
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
    t8_pipe_forms: dict[str, set[str]] = {}
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
        t8_pipe_forms[material_id] = forms
    expected_t8_pipe_forms = int(
        load(T8_PIPE_READINESS)["counts"]["combined_runtime_blocks"]
    )
    actual_t8_pipe_forms = sum(map(len, t8_pipe_forms.values()))
    if actual_t8_pipe_forms != expected_t8_pipe_forms:
        raise ValueError(
            "T8 pipe registration denominator drifted: "
            f"{actual_t8_pipe_forms} != {expected_t8_pipe_forms}"
        )
    t10_projection = load(T10_PREFLIGHT)["route_projections"]
    t10_known_forms: dict[str, set[str]] = defaultdict(set)
    for material_id in t10_projection["multi_ingot"]["materials"]:
        t10_known_forms[material_id].update({
            "double_ingot",
            "triple_ingot",
        })
    for material_id in t10_projection["hot_ingot"]["materials"]:
        t10_known_forms[material_id].add("ingot_hot")
    t10_form_flags = {
        "double_ingot": "gt6:itemgenerator/multiingots",
        "triple_ingot": "gt6:itemgenerator/multiingots",
        "ingot_hot": "gt6:itemgenerator/hotingots",
    }
    for material_id, forms in t10_known_forms.items():
        flags = set(material_docs[material_id].get("generation_flags") or [])
        missing_flags = {
            t10_form_flags[form]
            for form in forms
            if t10_form_flags[form] not in flags
        }
        if missing_flags:
            raise ValueError(
                f"T10 known-form source lacks generation flags: "
                f"{material_id}/{sorted(missing_flags)}"
            )
    actual_t10_known_forms = sum(map(len, t10_known_forms.values()))
    if actual_t10_known_forms != 967:
        raise ValueError(
            "T10 known-form registration denominator drifted: "
            f"{actual_t10_known_forms} != 967"
        )
    t38_document = load(T38_REQUIRED_FORMS)
    if t38_document.get("status") != "T38_REQUIRED_FORMS_FROZEN":
        raise ValueError("T38 required-form source is not frozen")
    t38_source = t38_document.get("source") or {}
    if (
            t38_source.get("path") != "tools/t38_roaster_source.json"
            or t38_source.get("sha256") != sha256(T38_SOURCE)
    ):
        raise ValueError("T38 required forms do not bind the frozen compact source")
    t38_required_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (t38_document.get("required_forms") or {}).items()
    }
    for material_id, forms in t38_required_forms.items():
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
                f"T38 required form exceeds factual or bounded post-import "
                f"forms: {material_id}/{sorted(forms)}"
            )
    t39_document = load(T39_REQUIRED_FORMS)
    if t39_document.get("status") != "T39_REQUIRED_FORMS_FROZEN":
        raise ValueError("T39 required-form source is not frozen")
    t39_source = t39_document.get("source") or {}
    if (
            t39_source.get("path") != "tools/t39_centrifuge_source.json"
            or t39_source.get("sha256") != sha256(T39_SOURCE)
    ):
        raise ValueError("T39 required forms do not bind the frozen compact source")
    t39_required_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (t39_document.get("required_forms") or {}).items()
    }
    for material_id, forms in t39_required_forms.items():
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
                f"T39 required form exceeds factual or bounded post-import "
                f"forms: {material_id}/{sorted(forms)}"
            )
    t40_document = load(T40_REQUIRED_FORMS)
    if t40_document.get("status") != "T40_REQUIRED_FORMS_FROZEN":
        raise ValueError("T40 required-form source is not frozen")
    t40_source = t40_document.get("source") or {}
    if (
            t40_source.get("path") != "tools/t40_electrolyzer_source.json"
            or t40_source.get("sha256") != sha256(T40_SOURCE)
    ):
        raise ValueError("T40 required forms do not bind the frozen compact source")
    t40_required_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (t40_document.get("required_forms") or {}).items()
    }
    for material_id, forms in t40_required_forms.items():
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
                f"T40 required form exceeds factual or bounded post-import "
                f"forms: {material_id}/{sorted(forms)}"
            )
    t48_document = load(T48_REQUIRED_FORMS)
    if t48_document.get("status") != "T48_REQUIRED_FORMS_FROZEN":
        raise ValueError("T48 required-form source is not frozen")
    t48_source = t48_document.get("source") or {}
    if (
            t48_source.get("path") != "tools/t47_bath_source.json"
            or t48_source.get("sha256") != sha256(T47_SOURCE)
    ):
        raise ValueError("T48 required forms do not bind the T47 compact source scan")
    t48_required_forms: dict[str, set[str]] = {
        material_id: set(forms)
        for material_id, forms in (t48_document.get("required_forms") or {}).items()
    }
    t48_authority = form_authority.source_by_id("t48_compact_required_forms")
    bounded_t48_forms = set(t48_authority.get("extra_factual_forms") or [])
    for material_id, forms in t48_required_forms.items():
        unsupported = (
            forms - factual_forms.get(material_id, set()) - bounded_t48_forms
        )
        if material_id not in factual_forms or unsupported:
            raise ValueError(
                f"T48 required form exceeds factual or bounded post-import "
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
            | t5_required_forms.get(material_id, set())
            | electrical_wire_forms.get(material_id, set())
            | t8_pipe_forms.get(material_id, set())
            | t10_known_forms.get(material_id, set())
            | t38_required_forms.get(material_id, set())
            | t38_acquisition_forms.get(material_id, set())
            | t39_required_forms.get(material_id, set())
            | t40_required_forms.get(material_id, set())
            | t48_required_forms.get(material_id, set())
            | tool_head_required_forms.get(material_id, set())
            | semantic_required_forms.get(material_id, set())
            | rock_forms.get(material_id, set())
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
            "t5_terminal_path": "tools/gt6_ore_chain.json",
            "t5_terminal_sha256": sha256(ORE_CHAIN),
            "t5_policy_path": "tools/t5_chemical_policy.json",
            "t5_policy_sha256": sha256(T5_CHEMICAL_POLICY),
            "t5_terminal_materials": len(terminal_materials),
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
            "t3_acceptance_form_corrections": {
                "path": (
                    "tools/component_rule_sources/"
                    "acceptance_form_corrections.json"
                ),
                "sha256": sha256(ACCEPTANCE_FORM_CORRECTIONS),
                "classification": (
                    "t3_acceptance_required_not_gt6_original_gate"
                ),
            },
            "t5_selected_source_route_forms": {
                "path": "tools/t5_chemical_policy.json",
                "field": "required_form_overrides.forms",
                "sha256": sha256(T5_CHEMICAL_POLICY),
                "classification": "t5_selected_source_route_required",
            },
            "t6_source_backed_wire_forms": {
                "path": "tools/gt6_electrical_source.json",
                "field": "conductors[] plus live material wireGt01 specification",
                "sha256": sha256(T6_ELECTRICAL_SOURCE),
                "classification": "t6_source_backed_runtime_required",
            },
            "t7_material_tag_policy": {
                "path": (
                    "src/main/resources/data/cruciblecraft/"
                    "material_tag_policy.json"
                ),
                "field": "tags[] classification and acceptance",
                "sha256": sha256(T7_MATERIAL_TAG_POLICY),
                "classification": "audit_only_non_form_source",
            },
            "t8_source_backed_pipe_forms": {
                "path": "tools/t8_pipe_readiness.json",
                "field": "fluid_domain/item_domain material_catalog",
                "sha256": sha256(T8_PIPE_READINESS),
                "classification": "t8_source_backed_runtime_required",
            },
            "t10_known_forms": {
                "path": "tools/t10_preflight_projection.json",
                "field": "route_projections.multi_ingot/hot_ingot.materials",
                "multi_material_set_sha256": t10_projection[
                    "multi_ingot"
                ]["material_set_sha256"],
                "hot_material_set_sha256": t10_projection[
                    "hot_ingot"
                ]["material_set_sha256"],
                "classification": "t10_source_domain_runtime_required",
            },
            "t38_compact_required_forms": {
                "path": "tools/t38_required_forms.json",
                "field": "required_forms",
                "sha256": sha256(T38_REQUIRED_FORMS),
                "source_path": "tools/t38_roaster_source.json",
                "source_sha256": sha256(T38_SOURCE),
                "classification": "t38_compact_output_runtime_required",
            },
            "t38_source_backed_acquisition_forms": {
                "path": "tools/t38_source_backed_acquisition.json",
                "field": "required_forms",
                "sha256": sha256(T38_ACQUISITION),
                "classification": "t38_source_backed_worldgen_runtime_required",
            },
            "t39_compact_required_forms": {
                "path": "tools/t39_required_forms.json",
                "field": "required_forms",
                "sha256": sha256(T39_REQUIRED_FORMS),
                "source_path": "tools/t39_centrifuge_source.json",
                "source_sha256": sha256(T39_SOURCE),
                "classification": "t39_compact_output_runtime_required",
            },
            "t40_compact_required_forms": {
                "path": "tools/t40_required_forms.json",
                "field": "required_forms",
                "sha256": sha256(T40_REQUIRED_FORMS),
                "source_path": "tools/t40_electrolyzer_source.json",
                "source_sha256": sha256(T40_SOURCE),
                "classification": "t40_compact_output_runtime_required",
            },
            "t48_compact_required_forms": {
                "path": "tools/t48_required_forms.json",
                "field": "required_forms",
                "sha256": sha256(T48_REQUIRED_FORMS),
                "source_path": "tools/t47_bath_source.json",
                "source_sha256": sha256(T47_SOURCE),
                "classification": "t48_compact_output_runtime_required",
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
            "route-scoped T5 forms plus source-backed T6 wireGt01 forms plus explicit "
            "source-backed T8 pipe forms plus T10 multi/hot known forms plus bounded "
            "T38 compact-output forms plus source-backed T38 acquisition forms plus "
            "bounded T39 compact-output forms plus bounded T40 compact-output forms plus "
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
            "t3_acceptance_forms": sum(map(len, acceptance_forms.values())),
            "t5_required_forms": sum(map(len, t5_required_forms.values())),
            "t6_electrical_wire_forms": sum(
                map(len, electrical_wire_forms.values())
            ),
            "t8_pipe_forms": actual_t8_pipe_forms,
            "t10_known_forms": actual_t10_known_forms,
            "t38_required_forms": sum(map(len, t38_required_forms.values())),
            "t38_acquisition_forms": sum(
                map(len, t38_acquisition_forms.values())
            ),
            "t39_required_forms": sum(map(len, t39_required_forms.values())),
            "t40_required_forms": sum(map(len, t40_required_forms.values())),
            "t48_required_forms": sum(map(len, t48_required_forms.values())),
            "tool_head_required_forms": sum(map(len, tool_head_required_forms.values())),
            "compatibility_forms": sum(map(len, compatibility_forms.values())),
            "registered_forms": sum(map(len, gated_forms.values())),
        },
        "compatibility_forms": compatibility_forms,
        "t3_acceptance_forms": {
            material: sorted(forms)
            for material, forms in sorted(acceptance_forms.items())
        },
        "t5_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(t5_required_forms.items())
        },
        "t6_electrical_wire_forms": {
            material: sorted(forms)
            for material, forms in sorted(electrical_wire_forms.items())
        },
        "t8_pipe_forms": {
            material: sorted(forms)
            for material, forms in sorted(t8_pipe_forms.items())
        },
        "t10_known_forms": {
            material: sorted(forms)
            for material, forms in sorted(t10_known_forms.items())
        },
        "t38_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(t38_required_forms.items())
        },
        "t38_source_backed_acquisition_forms": {
            material: sorted(forms)
            for material, forms in sorted(t38_acquisition_forms.items())
        },
        "t39_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(t39_required_forms.items())
        },
        "t40_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(t40_required_forms.items())
        },
        "t48_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(t48_required_forms.items())
        },
        "tool_head_required_forms": {
            material: sorted(forms)
            for material, forms in sorted(tool_head_required_forms.items())
        },
        **semantic_sections,
        "materials": gated_forms,
    }
    return operand_document, gate_document


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
        if OPERANDS_OUT.read_text(encoding="utf-8") != compact_json(operands):
            errors.append("operand artifact is not canonical compact JSON")
        if GATE_OUT.read_text(encoding="utf-8") != stable_json(gate):
            errors.append("material gate is not canonical stable JSON")
        inputs = operands.get("inputs") or {}
        if inputs.get("tracked") != tracked_input_hashes():
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
        if (
            (sources.get("l1b_recipe_operands") or {}).get("sha256")
            != sha256(OPERANDS_OUT)
        ):
            errors.append("material gate does not bind the operand artifact")
        expected_source_hashes = {
            "ore_chain_operands": sha256(ORE_CHAIN_OPERANDS),
            "t3_acceptance_form_corrections": sha256(
                ACCEPTANCE_FORM_CORRECTIONS
            ),
            "t5_selected_source_route_forms": sha256(T5_CHEMICAL_POLICY),
            "t6_source_backed_wire_forms": sha256(T6_ELECTRICAL_SOURCE),
            "t7_material_tag_policy": sha256(T7_MATERIAL_TAG_POLICY),
            "t8_source_backed_pipe_forms": sha256(T8_PIPE_READINESS),
            "t38_compact_required_forms": sha256(T38_REQUIRED_FORMS),
            "t38_source_backed_acquisition_forms": sha256(T38_ACQUISITION),
            "t39_compact_required_forms": sha256(T39_REQUIRED_FORMS),
            "t40_compact_required_forms": sha256(T40_REQUIRED_FORMS),
            "t48_compact_required_forms": sha256(T48_REQUIRED_FORMS),
            "tool_head_prefix_required_forms": sha256(TOOL_HEAD_REQUIRED_FORMS),
        }
        for name, expected in expected_source_hashes.items():
            if (sources.get(name) or {}).get("sha256") != expected:
                errors.append(f"material gate source hash drifted: {name}")
        t10 = load(T10_PREFLIGHT)["route_projections"]
        t10_source = sources.get("t10_known_forms") or {}
        if (
            t10_source.get("multi_material_set_sha256")
            != t10["multi_ingot"]["material_set_sha256"]
            or t10_source.get("hot_material_set_sha256")
            != t10["hot_ingot"]["material_set_sha256"]
        ):
            errors.append("material gate T10 known-form sets drifted")
        t38_source = sources.get("t38_compact_required_forms") or {}
        if (
                t38_source.get("source_path") != "tools/t38_roaster_source.json"
                or t38_source.get("source_sha256") != sha256(T38_SOURCE)
        ):
            errors.append("material gate T38 required forms drifted")
        acquisition_source = sources.get(
            "t38_source_backed_acquisition_forms"
        ) or {}
        if acquisition_source.get("field") != "required_forms":
            errors.append("material gate T38 acquisition form source drifted")
        t39_source = sources.get("t39_compact_required_forms") or {}
        if (
                t39_source.get("source_path") != "tools/t39_centrifuge_source.json"
                or t39_source.get("source_sha256") != sha256(T39_SOURCE)
        ):
            errors.append("material gate T39 required forms drifted")
        t40_source = sources.get("t40_compact_required_forms") or {}
        if (
                t40_source.get("source_path") != "tools/t40_electrolyzer_source.json"
                or t40_source.get("source_sha256") != sha256(T40_SOURCE)
        ):
            errors.append("material gate T40 required forms drifted")
        t48_source = sources.get("t48_compact_required_forms") or {}
        if (
                t48_source.get("source_path") != "tools/t47_bath_source.json"
                or t48_source.get("source_sha256") != sha256(T47_SOURCE)
        ):
            errors.append("material gate T48 required forms drifted")
        tool_head_source = sources.get("tool_head_prefix_required_forms") or {}
        if (
                tool_head_source.get("path") != "tools/tool_head_required_forms.json"
                or tool_head_source.get("sha256") != sha256(TOOL_HEAD_REQUIRED_FORMS)
        ):
            errors.append("material gate tool-head required forms drifted")

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
