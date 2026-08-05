import copy
import contextlib
import importlib.util
import hashlib
import io
import json
import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock


TOOLS = Path(__file__).resolve().parents[1]
FULL_REFERENCE = TOOLS / "gt6_recipe_normalized_reference.json"
FULL_REPORT = TOOLS / "gt6_recipe_compare_report.json"
requires_full_compare_cache = unittest.skipUnless(
    FULL_REFERENCE.is_file() and FULL_REPORT.is_file(),
    "expanded GT6 comparison replay caches are not installed",
)
SPEC = importlib.util.spec_from_file_location(
    "compare_gt6_recipes",
    TOOLS / "compare_gt6_recipes.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class ProcessCacheTest(unittest.TestCase):
    def tearDown(self):
        MODULE.clear_process_caches()

    def test_material_catalog_cache_returns_isolated_documents(self):
        MODULE.clear_process_caches()
        first = MODULE.load_cc_materials()
        first["copper"]["id"] = "tampered"
        second = MODULE.load_cc_materials()
        self.assertEqual("copper", second["copper"]["id"])
        self.assertEqual(1, MODULE._load_cc_materials_cached.cache_info().misses)
        self.assertEqual(1, MODULE._load_cc_materials_cached.cache_info().hits)

    def test_prefix_cache_clear_observes_source_change(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "index.json").write_text(
                json.dumps(["dust.json"]),
                encoding="utf-8",
            )
            definition = {
                "id": "cruciblecraft:dust",
                "serialized_path": "dust",
                "aliases": [],
            }
            path = root / "dust.json"
            path.write_text(json.dumps(definition), encoding="utf-8")
            with mock.patch.object(MODULE, "CC_PREFIXES", root):
                MODULE.clear_process_caches()
                material = {"id": "fixture", "forms": ["dust"]}
                self.assertEqual({"dust"}, MODULE.resolve_material_forms(material))
                definition["serialized_path"] = "powder"
                definition["aliases"] = ["dust"]
                path.write_text(json.dumps(definition), encoding="utf-8")
                self.assertEqual({"dust"}, MODULE.resolve_material_forms(material))
                MODULE.clear_process_caches()
                self.assertEqual({"powder"}, MODULE.resolve_material_forms(material))

    def test_expanded_recipe_cache_returns_an_isolated_copy(self):
        MODULE.clear_process_caches()
        first = MODULE.load_and_expand_cc_recipes()
        original = first[0].duration
        first[0].duration = original + 1
        second = MODULE.load_and_expand_cc_recipes()
        self.assertEqual(original, second[0].duration)
        self.assertEqual(
            1,
            MODULE._load_and_expand_cc_recipes_cached.cache_info().misses,
        )


class NormalizationFixtureTest(unittest.TestCase):
    def test_t8_pipe_forms_require_matching_source_flags(self):
        material = {
            "id": "example",
            "generation_flags": [
                "cruciblecraft:generates_fluid_pipe",
                "cruciblecraft:generates_item_pipe",
            ],
            "gt6_metadata": {
                "pipe_properties": {
                    "fluid_by_specification": {"pipeMedium": {}},
                    "item_by_specification": {"pipeMedium": {}},
                }
            },
        }
        self.assertEqual(
            {"fluid_pipe", "item_pipe"},
            MODULE.resolve_t8_pipe_forms(material),
        )
        material["generation_flags"].remove(
            "cruciblecraft:generates_item_pipe"
        )
        with self.assertRaises(ValueError):
            MODULE.resolve_t8_pipe_forms(material)

    def test_crucible_alloying_special_value_converts_kelvin_to_celsius(self):
        self.assertEqual(
            1083,
            MODULE.gt_special_value_for_compare("gt.recipe.cruciblealloying", 1357),
        )
        self.assertEqual(
            0,
            MODULE.gt_special_value_for_compare("gt.recipe.cruciblealloying", 0),
        )
        self.assertEqual(
            1357,
            MODULE.gt_special_value_for_compare("gt.recipe.crusher", 1357),
        )

    def test_selector_specific_target_unit_expression_is_exact(self):
        material = {"tier": 0, "thermal": {"density": 1}}
        self.assertEqual(
            2,
            MODULE.evaluate_rule_expression(
                "target.units / resource.prefix.units",
                material,
                {"target.units": 288, "resource.prefix.units": 144},
            ),
        )
        with self.assertRaises(ValueError):
            MODULE.evaluate_rule_expression(
                "target.units / resource.prefix.units",
                material,
                {"target.units": 16, "resource.prefix.units": 144},
            )
        material["gt6_metadata"] = {
            "processing_targets": {"smelting": {"cc_units": 16}}
        }
        self.assertEqual(
            9,
            MODULE.evaluate_rule_expression(
                "prefix_units(ingot) / gcd(target_units(smelting), prefix_units(ingot))",
                material,
            ),
        )

    @classmethod
    def setUpClass(cls):
        cls.fixture = json.loads(
            (TOOLS / "fixtures" / "gt_normalization_cases.json").read_text(
                encoding="utf-8"
            )
        )

    def test_display_name_normalization(self):
        for case in self.fixture["display_names"]:
            with self.subTest(display=case["display"]):
                actual = MODULE.parse_display_material(
                    case["display"],
                    set(case["known"]),
                )
                expected = tuple(case["expected"]) if case["expected"] else None
                self.assertEqual(expected, actual)

    def test_item_normalization(self):
        for case in self.fixture["items"]:
            with self.subTest(raw=case["raw"]):
                meta_map = {
                    int(meta): material
                    for meta, material in case["meta_map"].items()
                }
                resource = MODULE.normalize_gt_item(
                    case["raw"],
                    set(case["materials"]),
                    meta_map,
                )
                actual = resource.key() if resource else None
                self.assertEqual(case["expected"], actual)

    def test_authoritative_meta_map_covers_known_material_ids(self):
        materials = MODULE.load_cc_materials()
        meta_map = MODULE.build_meta_map(materials)
        self.assertEqual("copper", meta_map[290])
        self.assertEqual("iron", meta_map[260])
        self.assertEqual("tin", meta_map[500])
        self.assertEqual("bronze", meta_map[8610])

    def test_unknown_numeric_metadata_stays_fixed_without_display_guess(self):
        resource = MODULE.normalize_gt_item(
            {
                "item": "gregtech:gt.meta.ingot",
                "meta": 123456,
                "count": 1,
                "displayName": "Copper Ingot",
            },
            {"copper"},
            {},
        )
        self.assertEqual(
            "item:fixed:gregtech:gt.meta.ingot@123456x1",
            resource.key(),
        )

    def test_consuming_fixed_mold_is_preserved(self):
        resource = MODULE.normalize_gt_item(
            {
                "item": "gregtech:gt.multiitem.randomtools",
                "meta": 42,
                "count": 1,
                "displayName": "Extruder Shape (Gear)",
            },
            {"iron"},
            {},
        )
        self.assertEqual(
            "item:fixed:gregtech:gt.multiitem.randomtools@42x1",
            resource.key(),
        )

    def test_shadow_analysis_counts_same_map_input_collisions(self):
        first = MODULE.NormRecipe(
            "test", "cc", "cruciblecraft:test", "copper",
            [MODULE.Resource("item", "copper:plate", 1)],
            [MODULE.Resource("item", "copper:rod", 2)], 20, 1)
        second = MODULE.NormRecipe(
            "test", "cc", "cruciblecraft:test", "copper",
            [MODULE.Resource("item", "copper:plate", 1)],
            [MODULE.Resource("item", "copper:ring", 4)], 20, 1)
        self.assertEqual(1, MODULE.shadowed_recipe_count([first, second]))
        second.map_name = "cruciblecraft:other"
        self.assertEqual(0, MODULE.shadowed_recipe_count([first, second]))

    @requires_full_compare_cache
    def test_assembler_and_press_reference_samples_keep_fixed_inputs(self):
        reference_path = TOOLS / "gt6_recipe_normalized_reference.json"
        self.assertLess(reference_path.stat().st_size, 140_000_000)
        reference = json.loads(reference_path.read_text(encoding="utf-8"))
        for family in ("component_assembler", "component_press"):
            rows = reference["families"][family]
            self.assertTrue(rows, family)
            self.assertTrue(all(row["inputs"] for row in rows), family)
        self.assertTrue(any(
            resource["id"].startswith("fixed:")
            for row in reference["families"]["component_press"]
            for resource in row["inputs"]), "component_press")
        report = json.loads(
            (TOOLS / "gt6_recipe_compare_report.json").read_text(
                encoding="utf-8"))
        component_rows = [
            row for family in report["families"]
            if family["family"] in {"component_assembler", "component_press"}
            for row in family["rows"]
        ]
        self.assertTrue(any(
            "catalyst/insulation mismatch" in (
                row["expectation"].get("reason")
                or row["expectation"].get("issue")
                or "")
            for row in component_rows))
        cable_rows = [
            row for row in component_rows
            if row["evidence"].get("insulation_evidence")
        ]
        self.assertEqual(118, len(cable_rows))
        for row in cable_rows:
            evidence = row["evidence"]["insulation_evidence"]
            self.assertEqual("rubber", evidence["material"])
            self.assertEqual(0, evidence["machine_dump_samples"])
            decision_text = (
                row["expectation"].get("reason")
                or row["expectation"].get("issue")
                or "")
            self.assertIn("OP.java:644-648", decision_text)
            self.assertIn("Loader_OreProcessing.java:183-184", decision_text)

    def test_indexed_material_forms_resolve_all_schema_layers(self):
        self.assertEqual(
            {"ingot", "plate"},
            MODULE.resolve_material_forms({
                "generation_flags": [
                    "cruciblecraft:generates_ingot",
                    "generates_plate",
                ],
                "include_prefixes": ["rod"],
                "exclude_prefixes": ["rod"],
            }),
        )
        self.assertEqual({"dust"}, MODULE.resolve_material_forms({}))

    def test_duplicate_slots_are_merged_and_sorted(self):
        resources = [
            MODULE.Resource("item", "copper:dust", 1),
            MODULE.Resource("item", "tin:dust", 1),
            MODULE.Resource("item", "copper:dust", 2),
        ]
        self.assertEqual(
            ["item:copper:dustx3", "item:tin:dustx1"],
            [resource.key() for resource in MODULE.canonical_resources(resources)],
        )

    def test_reference_hash_is_order_independent(self):
        first = {
            "family": [
                {"map": "b", "inputs": [], "outputs": []},
                {"map": "a", "inputs": [], "outputs": []},
            ]
        }
        second = {
            "family": list(reversed(first["family"])),
        }
        normalized_first = {
            family: sorted(recipes, key=MODULE.stable_json)
            for family, recipes in first.items()
        }
        normalized_second = {
            family: sorted(recipes, key=MODULE.stable_json)
            for family, recipes in second.items()
        }
        self.assertEqual(
            MODULE.stable_hash(normalized_first),
            MODULE.stable_hash(normalized_second),
        )

    def test_reference_fingerprint_covers_every_consumed_integrity_section(self):
        # Use a compact schema-complete fixture here. The committed large
        # artifact is validated by CLI/check tests; cloning it once per section
        # made this unit test spend minutes in JSON encode/decode.
        reference = {
            "source": "fixture",
            "metadata": {
                "gt6_version": "fixture",
                "config_digest": "fixture",
                "dump_tool_version": "fixture",
            },
            "gt_map_count": 1,
            "gt_recipe_count": 1,
            "families": {
                "alloy": [{"map": "fixture", "inputs": [], "outputs": []}]
            },
            "map_inventory": [{"nameInternal": "fixture", "recipeCount": 1}],
            "gt_anvil_audit": {"recipe_count": 1},
            "coverage": {"unresolved_authoritative_item_count": 0},
        }
        reference["reference_fingerprint"] = (
            MODULE.fingerprint_from_reference(reference)
        )
        expected = MODULE.fingerprint_from_reference(reference)
        self.assertEqual(reference["reference_fingerprint"], expected)
        mutations = {
            "families": lambda value: value["families"].setdefault(
                "alloy", []
            ).append({"map": "tampered", "inputs": [], "outputs": []}),
            "map_inventory": lambda value: value["map_inventory"][0].__setitem__(
                "recipeCount", value["map_inventory"][0]["recipeCount"] + 1
            ),
            "gt_anvil_audit": lambda value: value["gt_anvil_audit"].__setitem__(
                "recipe_count", value["gt_anvil_audit"]["recipe_count"] + 1
            ),
            "coverage": lambda value: value["coverage"].__setitem__(
                "unresolved_authoritative_item_count",
                value["coverage"]["unresolved_authoritative_item_count"] + 1,
            ),
            "metadata": lambda value: value["metadata"].__setitem__(
                "dump_tool_version", value["metadata"]["dump_tool_version"] + "-tampered"
            ),
            "gt_map_count": lambda value: value.__setitem__(
                "gt_map_count", value["gt_map_count"] + 1
            ),
            "gt_recipe_count": lambda value: value.__setitem__(
                "gt_recipe_count", value["gt_recipe_count"] + 1
            ),
            "source": lambda value: value.__setitem__(
                "source", value["source"] + "-tampered"
            ),
        }
        for section, mutate in mutations.items():
            with self.subTest(section=section):
                changed = copy.deepcopy(reference)
                mutate(changed)
                self.assertNotEqual(
                    expected,
                    MODULE.fingerprint_from_reference(changed),
                )
                self.assertTrue(
                    MODULE.validate_reference_integrity(changed),
                    section,
                )

    def test_material_override_validation_rejects_unknown_keys_and_targets(self):
        materials = {
            "iron": {
                "id": "iron",
                "tier": 0,
                "thermal": {"density": 7.8},
                "_resolved_forms": ["raw_ore", "crushed_ore"],
            }
        }
        base = {
            "material_overrides": {"iron": {"duration": "20"}},
            "item_inputs": [{"prefix": "cruciblecraft:raw_ore"}],
            "item_outputs": [{"prefix": "cruciblecraft:crushed_ore"}],
            "fluid_inputs": [],
            "fluid_outputs": [],
        }
        MODULE.validate_material_overrides(base, materials, "fixture")
        unknown_material = json.loads(json.dumps(base))
        unknown_material["material_overrides"] = {"missing": {"duration": "20"}}
        with self.assertRaisesRegex(ValueError, "unknown material"):
            MODULE.validate_material_overrides(
                unknown_material, materials, "fixture"
            )
        bad_syntax = json.loads(json.dumps(base))
        bad_syntax["material_overrides"] = {"Iron": {"duration": "20"}}
        with self.assertRaisesRegex(ValueError, "syntax"):
            MODULE.validate_material_overrides(bad_syntax, materials, "fixture")
        bad_target = json.loads(json.dumps(base))
        bad_target["material_overrides"]["iron"] = {
            "item_input_counts": {"1": "1"}
        }
        with self.assertRaisesRegex(ValueError, "does not target"):
            MODULE.validate_material_overrides(bad_target, materials, "fixture")
        bad_range = json.loads(json.dumps(base))
        bad_range["material_overrides"]["iron"] = {
            "output_chances": {"0": "10001"}
        }
        with self.assertRaisesRegex(ValueError, "accepted range"):
            MODULE.validate_material_overrides(bad_range, materials, "fixture")
        dead_override = json.loads(json.dumps(base))
        dead_override["material"] = "iron"
        dead_override["material_overrides"]["copper"] = {"duration": "30"}
        materials["copper"] = {
            "id": "copper",
            "tier": 0,
            "thermal": {"density": 8.9},
            "_resolved_forms": ["raw_ore", "crushed_ore"],
        }
        with self.assertRaisesRegex(ValueError, "can never be selected"):
            MODULE.validate_material_overrides(
                dead_override, materials, "fixture"
            )

    def test_material_rule_expansion_preserves_current_parity(self):
        recipes = MODULE.expand_cc_recipes(MODULE.load_cc_materials())
        lead_crusher = next(
            recipe
            for recipe in recipes
            if recipe.family == "crush_raw_to_crushed"
            and recipe.material == "lead"
        )
        self.assertEqual(
            ["item:lead:raw_orex1"],
            [resource.key() for resource in lead_crusher.inputs],
        )
        self.assertEqual(
            ["item:lead:crushed_orex2"],
            [resource.key() for resource in lead_crusher.outputs],
        )
        self.assertEqual((256, 16, 0, [10000]), (
            lead_crusher.duration,
            lead_crusher.eut,
            lead_crusher.special_value,
            lead_crusher.chances,
        ))
        iron_bolts = next(
            recipe
            for recipe in recipes
            if recipe.family == "form_rod_to_bolt"
            and recipe.material == "iron"
        )
        self.assertEqual((1, 10000, 3, [10000]), (
            iron_bolts.duration,
            iron_bolts.eut,
            iron_bolts.special_value,
            iron_bolts.chances,
        ))

    def test_output_chance_participates_in_exact_signature(self):
        common = dict(
            family="chance",
            source="cc",
            map_name="test:map",
            material="iron",
            inputs=[MODULE.Resource("item", "iron:ingot", 1)],
            outputs=[MODULE.Resource("item", "iron:dust", 1)],
            duration=1,
            eut=1,
        )
        guaranteed = MODULE.NormRecipe(**common, chances=[10000])
        half = MODULE.NormRecipe(**common, chances=[5000])
        self.assertNotEqual(guaranteed.signature(), half.signature())
        self.assertFalse(
            MODULE.diff_pair(guaranteed, half)["deltas"]["output_chances_equal"]
        )

    def test_swapped_multi_output_chances_are_not_exact(self):
        common = dict(
            family="chance",
            source="cc",
            map_name="test:map",
            material="iron",
            inputs=[MODULE.Resource("item", "iron:ingot", 1)],
            outputs=[
                MODULE.Resource("item", "iron:dust", 1),
                MODULE.Resource("item", "iron:nugget", 2),
            ],
            duration=1,
            eut=1,
        )
        first = MODULE.NormRecipe(**common, chances=[5000, 10000])
        swapped = MODULE.NormRecipe(**common, chances=[10000, 5000])
        self.assertNotEqual(first.signature(), swapped.signature())
        self.assertFalse(
            MODULE.diff_pair(first, swapped)["deltas"]["output_chances_equal"]
        )

    def test_guaranteed_duplicate_slots_merge_like_one_larger_stack(self):
        common = dict(
            family="chance",
            source="cc",
            map_name="test:map",
            material="iron",
            inputs=[MODULE.Resource("item", "iron:raw_ore", 1)],
            duration=1,
            eut=1,
        )
        duplicate_slots = MODULE.NormRecipe(
            **common,
            outputs=[
                MODULE.Resource("item", "iron:crushed_ore", 1),
                MODULE.Resource("item", "iron:crushed_ore", 1),
            ],
            chances=[10000, 10000],
        )
        larger_stack = MODULE.NormRecipe(
            **common,
            outputs=[MODULE.Resource("item", "iron:crushed_ore", 2)],
            chances=[10000],
        )
        self.assertEqual(duplicate_slots.signature(), larger_stack.signature())
        self.assertNotIn(
            "output_chances_equal",
            MODULE.diff_pair(duplicate_slots, larger_stack)["deltas"],
        )

    def test_non_guaranteed_duplicate_slots_remain_independent(self):
        common = dict(
            family="chance",
            source="cc",
            map_name="test:map",
            material="iron",
            inputs=[MODULE.Resource("item", "iron:raw_ore", 1)],
            duration=1,
            eut=1,
        )
        duplicate_rolls = MODULE.NormRecipe(
            **common,
            outputs=[
                MODULE.Resource("item", "iron:dust", 1),
                MODULE.Resource("item", "iron:dust", 1),
            ],
            chances=[5000, 5000],
        )
        one_roll = MODULE.NormRecipe(
            **common,
            outputs=[MODULE.Resource("item", "iron:dust", 2)],
            chances=[5000],
        )
        self.assertNotEqual(duplicate_rolls.signature(), one_roll.signature())
        self.assertFalse(
            MODULE.diff_pair(duplicate_rolls, one_roll)["deltas"][
                "output_chances_equal"
            ]
        )

    def test_fluid_output_slots_always_merge_as_guaranteed_counts(self):
        common = dict(
            family="chance",
            source="cc",
            map_name="test:map",
            material=None,
            inputs=[MODULE.Resource("item", "coal:gem", 1)],
            duration=1,
            eut=1,
        )
        duplicate_fluids = MODULE.NormRecipe(
            **common,
            outputs=[
                MODULE.Resource("fluid", "steam", 80),
                MODULE.Resource("fluid", "steam", 80),
            ],
        )
        one_fluid = MODULE.NormRecipe(
            **common,
            outputs=[MODULE.Resource("fluid", "steam", 160)],
        )
        self.assertEqual(duplicate_fluids.signature(), one_fluid.signature())

    def test_duplicate_output_slots_keep_deterministic_probability_semantics(self):
        common = dict(
            family="chance",
            source="cc",
            map_name="test:map",
            material="iron",
            inputs=[MODULE.Resource("item", "iron:ingot", 1)],
            duration=1,
            eut=1,
        )
        first = MODULE.NormRecipe(
            **common,
            outputs=[
                MODULE.Resource("item", "iron:dust", 1),
                MODULE.Resource("item", "iron:dust", 2),
            ],
            chances=[5000, 10000],
        )
        reordered = MODULE.NormRecipe(
            **common,
            outputs=[
                MODULE.Resource("item", "iron:dust", 2),
                MODULE.Resource("item", "iron:dust", 1),
            ],
            chances=[10000, 5000],
        )
        changed_pairing = MODULE.NormRecipe(
            **common,
            outputs=[
                MODULE.Resource("item", "iron:dust", 1),
                MODULE.Resource("item", "iron:dust", 2),
            ],
            chances=[10000, 5000],
        )
        self.assertEqual(first.signature(), reordered.signature())
        self.assertNotEqual(first.signature(), changed_pairing.signature())


class ExpectationPolicyTest(unittest.TestCase):
    def test_compact_expectations_cover_current_cc_recipes(self):
        recipes = MODULE.expand_cc_recipes(MODULE.load_cc_materials())
        document = json.loads(
            (TOOLS / "gt6_recipe_expectations.json").read_text(encoding="utf-8")
        )
        self.assertEqual(
            [],
            MODULE.validate_compact_expectations(document, len(recipes)),
        )

    @requires_full_compare_cache
    def test_committed_verdicts_cover_every_normalized_cc_recipe(self):
        materials = MODULE.load_cc_materials()
        cc_all = MODULE.expand_cc_recipes(materials)
        reference = json.loads(
            (TOOLS / "gt6_recipe_normalized_reference.json").read_text(
                encoding="utf-8"
            )
        )
        reports = [
            MODULE.compare_family(
                family,
                [recipe for recipe in cc_all if recipe.family == family],
                [
                    MODULE.recipe_from_reference(family, recipe)
                    for recipe in reference["families"][family]
                ],
            )
            for family in MODULE.FAMILIES
        ]
        expectations = json.loads(
            (TOOLS / "gt6_recipe_expectations.json").read_text(encoding="utf-8")
        )
        result = MODULE.validate_expectations(reports, expectations)
        self.assertTrue(result["valid"], result["errors"])
        committed_report = json.loads(
            (TOOLS / "gt6_recipe_compare_report.json").read_text(
                encoding="utf-8"
            )
        )
        self.assertEqual(
            committed_report["summary"]["cc_normalized_recipe_count"],
            len(cc_all),
        )
        self.assertEqual(len(cc_all), result["total_rows"])
        self.assertEqual(
            {
                "EXACT",
                "INTENTIONAL",
                "TODO_PORT",
                "OUT_OF_SCOPE",
            },
            set(result["counts"]),
        )
        self.assertEqual(len(cc_all), sum(result["counts"].values()))
        self.assertEqual(
            {
                "human_reviewed": 0,
                "automated_evidence_reviewed": len(cc_all),
                "unreviewed": 0,
            },
            result["review_counts"],
        )

    def test_unreviewed_row_fails(self):
        report = {
            "rows": [
                {
                    "row_id": "family/material/hash",
                    "match_tier": "NONE",
                    "primary_match": None,
                }
            ]
        }
        result = MODULE.validate_expectations([report], {"expectations": {}})
        self.assertFalse(result["valid"])
        self.assertEqual(1, result["unreviewed_count"])

    def test_append_missing_expectations_never_rewrites_existing(self):
        existing = {
            "schema_version": 1,
            "_notes": "kept",
            "expectations": {
                "alloy/bronze/28641ca6d2ff": {
                    "verdict": "TODO_PORT",
                    "issue": "human melting-point mismatch",
                },
                "keep/me/aaaa": {"verdict": "EXACT"},
            },
        }
        suggested = {
            "schema_version": 1,
            "expectations": {
                "alloy/bronze/28641ca6d2ff": {
                    "verdict": "INTENTIONAL",
                    "reason": "autofill would clobber",
                },
                "keep/me/aaaa": {"verdict": "TODO_PORT", "issue": "nope"},
                "new/row/bbbb": {
                    "verdict": "TODO_PORT",
                    "issue": "fresh gap",
                    "match_tier": "NONE",
                },
            },
        }
        merged, added = MODULE.append_missing_expectations(existing, suggested)
        self.assertEqual(["new/row/bbbb"], added)
        self.assertEqual(
            existing["expectations"]["alloy/bronze/28641ca6d2ff"],
            merged["expectations"]["alloy/bronze/28641ca6d2ff"],
        )
        self.assertEqual(
            {"verdict": "EXACT"},
            merged["expectations"]["keep/me/aaaa"],
        )
        self.assertEqual(
            suggested["expectations"]["new/row/bbbb"],
            merged["expectations"]["new/row/bbbb"],
        )
        self.assertEqual("kept", merged["_notes"])

    def test_placeholder_todo_issues_are_unreviewed(self):
        report = {
            "rows": [{
                "row_id": "family/material/hash",
                "match_tier": "NONE",
                "primary_match": None,
            }]
        }
        for issue in (
            "UNTRACKED: review row",
            "review",
            "TODO",
            "TBD: determine parity",
            "prefix FIXME: inspect source",
            "needs TODO/review before merge",
            "",
        ):
            with self.subTest(issue=issue):
                result = MODULE.validate_expectations(
                    [report],
                    {"expectations": {
                        "family/material/hash": {
                            "verdict": "TODO_PORT",
                            "issue": issue,
                            "match_tier": "NONE",
                        }
                    }},
                )
                self.assertFalse(result["valid"])
                self.assertEqual(1, result["unreviewed_count"])

    @requires_full_compare_cache
    def test_generic_intentional_boilerplate_is_unreviewed(self):
        report = json.loads(
            (TOOLS / "gt6_recipe_compare_report.json").read_text(encoding="utf-8"))
        row = next(
            row for family in report["families"] for row in family["rows"]
            if row.get("expectation", {}).get("verdict") == "INTENTIONAL")
        decision = {
            "verdict": "INTENTIONAL",
            "reason": "Reviewed CC/GT numerical model difference.",
            "match_tier": row["match_tier"],
            "deltas": row["primary_match"]["deltas"],
            "evidence": MODULE.row_decision_evidence(row),
        }
        result = MODULE.validate_expectations(
            [{"rows": [row]}],
            {"expectations": {row["row_id"]: decision}},
        )
        self.assertFalse(result["valid"])
        self.assertEqual(1, result["unreviewed_count"])

    @requires_full_compare_cache
    def test_automated_review_requires_current_evidence_digest(self):
        report = json.loads(
            (TOOLS / "gt6_recipe_compare_report.json").read_text(
                encoding="utf-8"
            )
        )
        row = next(
            row
            for family in report["families"]
            for row in family["rows"]
            if row["match_tier"] == "EXACT"
        )
        evidence = MODULE.row_decision_evidence(row)
        decision = {
            "verdict": "EXACT",
            "match_tier": "EXACT",
            "review_mode": "automated",
            "evidence_source": "fixture deterministic source",
            "evidence_method": "normalized_exact_signature_and_zero_delta",
            "evidence_digest": "stale",
            "evidence": evidence,
        }
        result = MODULE.validate_expectations(
            [{"rows": [row]}],
            {"expectations": {row["row_id"]: decision}},
        )
        self.assertFalse(result["valid"])
        self.assertEqual(1, result["unreviewed_count"])

    @requires_full_compare_cache
    def test_human_review_requires_durable_attribution(self):
        report = json.loads(
            (TOOLS / "gt6_recipe_compare_report.json").read_text(
                encoding="utf-8"
            )
        )
        row = next(
            row
            for family in report["families"]
            for row in family["rows"]
            if row["match_tier"] == "NONE"
        )
        decision = {
            "verdict": "TODO_PORT",
            "issue": "Specific fixture gap.",
            "match_tier": "NONE",
            "evidence": MODULE.row_decision_evidence(row),
            "review_mode": "human",
            "evidence_source": "fixture",
            "evidence_method": "inspection",
            "evidence_digest": "fixture",
        }
        result = MODULE.validate_expectations(
            [{"rows": [row]}],
            {"expectations": {row["row_id"]: decision}},
        )
        self.assertFalse(result["valid"])
        self.assertEqual(1, result["unreviewed_count"])

    @requires_full_compare_cache
    def test_committed_non_exact_rows_have_row_specific_evidence(self):
        report = json.loads(
            (TOOLS / "gt6_recipe_compare_report.json").read_text(encoding="utf-8"))
        rows = [row for family in report["families"] for row in family["rows"]]
        reviewed = [
            row for row in rows
            if row.get("expectation", {}).get("verdict")
            in {"INTENTIONAL", "TODO_PORT"}
        ]
        self.assertTrue(reviewed)
        for row in reviewed:
            decision = row["expectation"]
            self.assertEqual(MODULE.row_decision_evidence(row), decision["evidence"])
            self.assertIn(row["cc"]["map"], decision.get("reason") or decision.get("issue"))
            self.assertIn(str(row["cc"]["inputs"]), decision.get("reason") or decision.get("issue"))

        no_match = next(
            row for row in reviewed
            if row["match_tier"] == "NONE"
            and row["expectation"]["verdict"] == "TODO_PORT")
        query = no_match["expectation"]["evidence"]["no_match_query"]
        self.assertTrue(query["gt_maps"])
        self.assertTrue(query["prefixes"])
        self.assertIn("source_ids=", no_match["expectation"]["issue"])

    def test_exact_verdict_rejects_non_zero_delta(self):
        report = {
            "rows": [
                {
                    "row_id": "family/material/hash",
                    "match_tier": "EXACT",
                    "primary_match": {
                        "deltas": {
                            "duration": 1,
                            "eut": 0,
                            "special_value": 0,
                            "input_counts_equal": True,
                            "output_counts_equal": True,
                        }
                    },
                }
            ]
        }
        result = MODULE.validate_expectations(
            [report],
            {
                "expectations": {
                    "family/material/hash": {"verdict": "EXACT"}
                }
            },
        )
        self.assertFalse(result["valid"])
        self.assertTrue(
            any("non-zero deltas" in error for error in result["errors"]),
            result["errors"],
        )


class RoadmapPolicyTest(unittest.TestCase):
    def test_bounded_subset_is_an_explicit_ported_status(self):
        self.assertTrue(MODULE.valid_roadmap_status("BOUNDED_SUBSET_PORTED"))

    def test_fixture_rejects_missing_stale_and_changed_map_entries(self):
        index = {
            "maps": [
                {"nameInternal": "active", "recipeCount": 2},
                {"nameInternal": "empty", "recipeCount": 0},
            ]
        }
        valid = MODULE.roadmap_template(index)
        rows, errors = MODULE.machine_gap_summary(index, valid)
        self.assertEqual(2, len(rows))
        self.assertEqual([], errors)

        broken = json.loads(json.dumps(valid))
        del broken["maps"]["empty"]
        broken["maps"]["stale"] = {
            "status": "DEFERRED",
            "reason": "stale",
            "reference_recipe_count": 0,
        }
        broken["maps"]["active"]["reference_recipe_count"] = 3
        _, errors = MODULE.machine_gap_summary(index, broken)
        self.assertTrue(any("missing roadmap entry" in error for error in errors))
        self.assertTrue(any("stale roadmap entry" in error for error in errors))
        self.assertTrue(any("stale reference_recipe_count" in error for error in errors))

    def test_committed_roadmap_covers_all_95_reference_maps(self):
        baseline = json.loads(
            (TOOLS / "gt6_recipe_compare_baseline.json").read_text(
                encoding="utf-8"
            )
        )
        roadmap = json.loads(
            (TOOLS / "gt6_map_roadmap.json").read_text(encoding="utf-8")
        )
        fingerprint = baseline["reference_fingerprint"]
        self.assertEqual(fingerprint["gt_map_count"], len(roadmap["maps"]))
        self.assertEqual(
            fingerprint["gt_recipe_count"],
            sum(
                row["reference_recipe_count"]
                for row in roadmap["maps"].values()
            ),
        )


class ProcessExpectationTest(unittest.TestCase):
    def test_committed_process_expectations_are_complete_and_evidenced(self):
        snapshot = MODULE.energy_constants_snapshot()
        expectations = json.loads(
            (TOOLS / "gt6_process_expectations.json").read_text(encoding="utf-8")
        )
        result = MODULE.validate_process_expectations(snapshot, expectations)
        self.assertTrue(result["valid"], result["errors"])
        self.assertEqual(
            {
                "EXACT": 1,
                "INTENTIONAL": 1,
                "TODO_PORT": 5,
                "OUT_OF_SCOPE": 0,
            },
            result["counts"],
        )

    def test_process_expectations_reject_placeholders_and_stale_evidence(self):
        snapshot = MODULE.energy_constants_snapshot()
        document = json.loads(
            (TOOLS / "gt6_process_expectations.json").read_text(
                encoding="utf-8"
            )
        )
        for field, placeholder in (
            ("issue", "TBD: inspect later"),
            ("source_evidence", "FIXME: locate source"),
        ):
            with self.subTest(field=field):
                changed = json.loads(json.dumps(document))
                changed["groups"]["air"][field] = placeholder
                result = MODULE.validate_process_expectations(snapshot, changed)
                self.assertFalse(result["valid"])
        stale = json.loads(json.dumps(document))
        stale["groups"]["steam"]["evidence_digest"] = "stale"
        result = MODULE.validate_process_expectations(snapshot, stale)
        self.assertFalse(result["valid"])
        self.assertTrue(
            any("digest" in error for error in result["errors"]),
            result["errors"],
        )

    def test_process_evidence_pinner_is_deterministic(self):
        snapshot = MODULE.energy_constants_snapshot()
        document = json.loads(
            (TOOLS / "gt6_process_expectations.json").read_text(
                encoding="utf-8"
            )
        )
        first = MODULE.pin_process_expectation_evidence(snapshot, document)
        second = MODULE.pin_process_expectation_evidence(snapshot, first)
        self.assertEqual(first, second)
        self.assertTrue(all(
            decision["evidence"]["gt6_source_fingerprints"]
            for decision in first["groups"].values()
        ))


class ReadOnlyCliTest(unittest.TestCase):
    def test_full_replay_without_cache_is_an_actionable_error(self):
        missing = TOOLS / "test-fixture-missing-cache"
        stderr = io.StringIO()
        with (
            mock.patch.object(MODULE, "GT_INDEX", missing / "index.json"),
            mock.patch.object(
                MODULE,
                "REFERENCE_JSON",
                missing / "gt6_recipe_normalized_reference.json",
            ),
            mock.patch.object(
                sys,
                "argv",
                ["compare_gt6_recipes.py", "--check", "--full-replay"],
            ),
            contextlib.redirect_stderr(stderr),
        ):
            self.assertEqual(2, MODULE.main())
        self.assertIn("requires either", stderr.getvalue())
        self.assertIn("--write-reference", stderr.getvalue())

    @unittest.skipIf(
        os.environ.get("CRUCIBLECRAFT_CURRENTNESS_PRECHECKED") == "1",
        "compact compare currentness was executed by the closure orchestrator",
    )
    def test_check_modes_do_not_modify_audit_artifacts(self):
        paths = [
            TOOLS / "gt6_recipe_normalized_reference.json",
            TOOLS / "gt6_recipe_compare_report.json",
            TOOLS / "gt6_recipe_expectations.json",
            TOOLS / "gt6_recipe_expectations_suggested.json",
            TOOLS / "gt6_map_roadmap.json",
            TOOLS / "gt6_recipe_compare_baseline.json",
        ]

        def snapshot():
            return {
                str(path): (
                    hashlib.sha256(path.read_bytes()).hexdigest()
                    if path.is_file()
                    else None
                )
                for path in paths
            }

        before = snapshot()
        completed = subprocess.run(
            [
                sys.executable,
                str(TOOLS / "compare_gt6_recipes.py"),
                "--check",
                "--reference-only",
            ],
            cwd=TOOLS.parent,
            capture_output=True,
            text=True,
            timeout=300,
        )
        self.assertEqual(
            0,
            completed.returncode,
            completed.stdout + completed.stderr,
        )
        self.assertEqual(before, snapshot())


if __name__ == "__main__":
    unittest.main()
