"""Tests for the single material-form authority."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import material_form_authority as authority
from tools import verification_runtime as vr

MATERIALS = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
PREFIX_INDEX = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_prefixes"
    / "index.json"
)
AUTHORED_RECIPES = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "recipe"
)
CENSUS = ROOT / "src" / "main" / "resources" / "census" / "runtime_registry_gate.json"
GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)


def _load_json(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _prefix_names() -> set[str]:
    return {Path(name).stem for name in _load_json(PREFIX_INDEX)}


def _material_docs() -> dict[str, dict[str, Any]]:
    documents: dict[str, dict[str, Any]] = {}
    for path in MATERIALS.glob("*.json"):
        if path.name == "index.json":
            continue
        document = _load_json(path)
        material_id = str(document.get("id") or path.stem)
        documents[material_id] = document
    return documents


def _walk_item_ids(value: Any) -> list[str]:
    found: list[str] = []
    if isinstance(value, dict):
        item = value.get("item")
        if isinstance(item, str):
            found.append(item)
        identity = value.get("id")
        if isinstance(identity, str) and ":" in identity:
            found.append(identity)
        for child in value.values():
            found.extend(_walk_item_ids(child))
    elif isinstance(value, list):
        for child in value:
            found.extend(_walk_item_ids(child))
    return found


def _ungated_authored_material_forms() -> list[str]:
    prefixes = _prefix_names()
    materials = _material_docs()
    gated = {
        material_id: set(forms)
        for material_id, forms in (_load_json(GATE).get("materials") or {}).items()
    }
    missing: list[str] = []
    for path in sorted(AUTHORED_RECIPES.rglob("*.json")):
        relative = path.relative_to(ROOT).as_posix()
        for item_id in _walk_item_ids(_load_json(path)):
            if not item_id.startswith("cruciblecraft:"):
                continue
            registry_path = item_id.split(":", 1)[1]
            if registry_path.count("/") != 1:
                continue
            material_id, form = registry_path.split("/", 1)
            if material_id not in materials or form not in prefixes:
                continue
            mapped = (materials[material_id].get("form_items") or {}).get(form)
            if mapped:
                if item_id == mapped:
                    continue
                missing.append(f"{relative}: {item_id} aliases to {mapped}")
                continue
            if form not in gated.get(material_id, set()):
                missing.append(f"{relative}: {item_id}")
    return sorted(set(missing))


def _census_form_item_aliases() -> list[str]:
    materials = _material_docs()
    fixture = _load_json(CENSUS)
    aliases: list[str] = []
    for item_id in fixture.get("categories", {}).get("items") or []:
        if not str(item_id).startswith("cruciblecraft:"):
            continue
        registry_path = str(item_id).split(":", 1)[1]
        if registry_path.count("/") != 1:
            continue
        material_id, form = registry_path.split("/", 1)
        mapped = (materials.get(material_id) or {}).get("form_items", {}).get(form)
        if mapped and mapped != item_id:
            aliases.append(f"{item_id} -> {mapped}")
    return sorted(aliases)


class MaterialFormAuthorityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = authority.build()

    def test_java_overlay_sections_are_explicit_and_stable(self) -> None:
        declared = authority.java_overlay_sections(self.document)
        self.assertEqual(declared, self.document["java_overlay_sections"])
        self.assertIn("bath_required_forms", declared)
        self.assertIn("tool_head_required_forms", declared)
        self.assertEqual(137, self.document["typed_ore_denominators"]["factual_ore_materials"])
        self.assertEqual(147, self.document["typed_ore_denominators"]["registered_ore_materials"])
        self.assertEqual(10, self.document["typed_ore_denominators"]["worldgen_acquisition_ore_delta"])
        self.assertEqual(8, self.document["typed_ore_denominators"]["chemical_semantic_vein_ledger"])

    def test_gate_overlay_reads_authority_not_hardcoded_card_lists(self) -> None:
        gate = vr.gate_document()
        overlay = authority.overlay_forms_from_gate(gate, document=self.document)
        self.assertIn("ore", overlay.get("arsenopyrite", set()))
        compare = (ROOT / "tools" / "compare_gt6_recipes.py").read_text(encoding="utf-8")
        veins = (ROOT / "tools" / "build_gt6_veins.py").read_text(encoding="utf-8")
        self.assertIn("overlay_forms_from_gate", compare)
        self.assertIn("overlay_forms_from_gate", veins)
        self.assertNotIn(
            '"worldgen_acquisition_forms",\n        "roaster_required_forms"',
            compare,
        )

    def test_check_is_read_only_when_artifact_exists(self) -> None:
        if not authority.OUTPUT.is_file():
            self.skipTest("material_form_authority.json not generated")
        before = authority.OUTPUT.read_bytes()
        self.assertEqual([], authority.check())
        self.assertEqual(before, authority.OUTPUT.read_bytes())

    def test_aluminium_capcellcon_is_authority_gated(self) -> None:
        self.assertIn(
            "fission_observation_safety_required_forms",
            self.document["java_overlay_sections"],
        )
        source = authority.source_by_id(
            "fission_observation_safety_required_forms",
            document=self.document,
        )
        self.assertEqual(
            "energy/nuclear-fission-observation-safety",
            source["owner"],
        )
        gate = vr.gate_document()
        self.assertIn("capcellcon", gate["materials"]["aluminium"])
        self.assertIn(
            "fission_observation_safety_required_forms",
            gate["java_overlay_sections"],
        )
        self.assertIn(
            "capcellcon",
            gate["fission_observation_safety_required_forms"]["aluminium"],
        )

    def test_fission_survival_required_forms_are_authority_gated(self) -> None:
        self.assertIn(
            "fission_survival_required_forms",
            self.document["java_overlay_sections"],
        )
        source = authority.source_by_id(
            "fission_survival_required_forms",
            document=self.document,
        )
        self.assertEqual("energy/nuclear-fission-survival", source["owner"])
        gate = vr.gate_document()
        self.assertIn("scrap", gate["materials"]["zirconium"])
        self.assertIn("machine_casing_dense", gate["materials"]["lead"])
        self.assertIn(
            "fission_survival_required_forms",
            gate["java_overlay_sections"],
        )
        self.assertIn("scrap", gate["fission_survival_required_forms"]["zirconium"])
        self.assertIn(
            "machine_casing_dense",
            gate["fission_survival_required_forms"]["lead"],
        )

    def test_converter_catalog_fluidbed_dust_forms_are_authority_gated(self) -> None:
        self.assertIn(
            "converter_catalog_fluidbed_required_forms",
            self.document["java_overlay_sections"],
        )
        source = authority.source_by_id(
            "converter_catalog_fluidbed_required_forms",
            document=self.document,
        )
        self.assertEqual("energy/converter-catalog", source["owner"])
        gate = vr.gate_document()
        self.assertIn("storage_dust", gate["materials"]["peat"])
        self.assertIn("dust_div72", gate["materials"]["petroleum_coke"])
        self.assertIn(
            "converter_catalog_fluidbed_required_forms",
            gate["java_overlay_sections"],
        )
        self.assertIn(
            "storage_dust",
            gate["converter_catalog_fluidbed_required_forms"]["peat"],
        )
        self.assertIn(
            "dust_div72",
            gate["converter_catalog_fluidbed_required_forms"]["petroleum_coke"],
        )

    def test_census_form_open_lands_only_the_standard_gate_slice(self) -> None:
        source = authority.source_by_id(
            "census_form_open_required_forms",
            document=self.document,
        )
        self.assertEqual("registry/census-form-open", source["owner"])
        self.assertEqual([], source["extra_factual_forms"])
        self.assertIn(
            "census_form_open_required_forms",
            self.document["java_overlay_sections"],
        )
        required = _load_json(
            ROOT / "tools/waves/registry/census-form-open/required_forms.json"
        )
        pairs = {
            (material, form)
            for material, forms in required["required_forms"].items()
            for form in forms
        }
        self.assertEqual(125, len(pairs))
        self.assertEqual({"washed_crushed_ore"}, {form for _, form in pairs})
        self.assertEqual("LANDED_BOUNDED_FORM_OPEN", required["status"])
        gate = vr.gate_document()
        self.assertEqual(
            required["required_forms"],
            gate["census_form_open_required_forms"],
        )
        for material, form in pairs:
            self.assertIn(form, gate["materials"][material])
        containers = _load_json(
            ROOT
            / "tools/waves/prep/gt6-container-chem-tube-forms/required_forms.json"
        )
        container_pairs = {
            (material, form)
            for material, forms in containers["required_forms"].items()
            for form in forms
        }
        self.assertEqual(95, len(container_pairs))
        self.assertTrue(pairs.isdisjoint(container_pairs))
        census = _load_json(
            ROOT / "tools/waves/prep/material-form-demand-census/census.json"
        )
        openable = {
            (row["material"], row["form"]) for row in census["openable"]
        }
        self.assertTrue(pairs.isdisjoint(openable))
        byproduct = {(material, "tiny_dust") for material, _form in pairs}
        self.assertTrue(container_pairs <= openable)
        self.assertTrue(byproduct.isdisjoint(openable))
        self.assertEqual(container_pairs, openable)

    def test_sifter_byproduct_tiny_dust_lands_the_census_remainder(self) -> None:
        source = authority.source_by_id(
            "sifter_byproduct_tiny_dust_required_forms",
            document=self.document,
        )
        self.assertEqual("recipe/sifter-byproduct-tiny-dust", source["owner"])
        self.assertEqual([], source["extra_factual_forms"])
        self.assertIn(
            "sifter_byproduct_tiny_dust_required_forms",
            self.document["java_overlay_sections"],
        )
        required = _load_json(
            ROOT
            / "tools/waves/recipe/sifter-byproduct-tiny-dust/required_forms.json"
        )
        pairs = {
            (material, form)
            for material, forms in required["required_forms"].items()
            for form in forms
        }
        self.assertEqual(125, len(pairs))
        self.assertEqual({"tiny_dust"}, {form for _, form in pairs})
        self.assertEqual(500, len(required["unlocked_source_row_sha256"]))
        self.assertEqual("LANDED_SIFTER_BYPRODUCT_TINY_DUST", required["status"])
        washed = _load_json(
            ROOT / "tools/waves/registry/census-form-open/required_forms.json"
        )
        self.assertEqual(
            set(washed["required_forms"]),
            set(required["required_forms"]),
        )
        gate = vr.gate_document()
        self.assertEqual(
            required["required_forms"],
            gate["sifter_byproduct_tiny_dust_required_forms"],
        )
        for material, form in pairs:
            self.assertIn(form, gate["materials"][material])
        census = _load_json(
            ROOT / "tools/waves/prep/material-form-demand-census/census.json"
        )
        openable = {
            (row["material"], row["form"]) for row in census["openable"]
        }
        self.assertTrue(pairs.isdisjoint(openable))
        proof = _load_json(
            ROOT
            / "tools/waves/recipe/gt6-prefix-regular-bulk/sifter/coverage_proof.json"
        )
        blocked = _load_json(
            ROOT / "tools/waves/recipe/gt6-prefix-regular-bulk/sifter/blocked.json"
        )
        self.assertEqual([], blocked["rows"])
        self.assertEqual(blocked["source_rows"], proof["published_rows"])
        self.assertEqual(2877, proof["published_rows"])

    def test_authored_recipe_material_forms_are_gated_or_aliased(self) -> None:
        missing = _ungated_authored_material_forms()
        self.assertEqual([], missing)

    def test_census_fixture_omits_form_item_aliases(self) -> None:
        self.assertEqual([], _census_form_item_aliases())


if __name__ == "__main__":
    unittest.main()
