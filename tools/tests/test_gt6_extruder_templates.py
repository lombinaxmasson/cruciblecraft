from __future__ import annotations

import copy
import contextlib
import io
import json
import unittest
from unittest import mock

from tools import gt6_extruder_templates as extruder


def material_recipe(
    meta: int,
    duration: int = 32,
    shape_meta: int = 10003,
    eu_per_tick: int = 16,
) -> dict:
    return {
        "euPerTick": eu_per_tick,
        "duration": duration,
        "specialValue": 0,
        "enabled": True,
        "hidden": False,
        "fake": False,
        "canBeBuffered": True,
        "needsEmptyOutput": False,
        "noNbtChecks": True,
        "inputs": [
            {
                "item": "gregtech:gt.meta.ingot",
                "meta": meta,
                "count": 1,
                "displayName": f"Material {meta} Ingot",
            },
            {
                "item": extruder.SHAPE_ITEM,
                "meta": shape_meta,
                "count": 0,
                "displayName": "Extruder Shape (Bolt)",
            },
        ],
        "outputs": [
            {
                "item": "gregtech:gt.meta.bolt",
                "meta": meta,
                "count": 8,
                "displayName": f"Material {meta} Bolt",
            }
        ],
        "fluidInputs": [],
        "fluidOutputs": [],
        "chances": [10000],
        "maxChances": [10000],
    }


def external_recipe(item_meta: int, output_meta: int, duration: int) -> dict:
    recipe = material_recipe(output_meta, duration)
    recipe["inputs"][0] = {
        "item": "gregtech:gt.multitileentity",
        "meta": item_meta,
        "count": 1,
        "displayName": f"External {item_meta}",
    }
    return recipe


class CompactExtruderEvidenceTest(unittest.TestCase):
    def test_full_replay_without_cache_is_an_explicit_ci_skip(self) -> None:
        missing = extruder.ROOT / "tools" / "test-fixture-missing-cache.json"
        stdout = io.StringIO()
        with (
            mock.patch.object(extruder, "OUT", missing),
            contextlib.redirect_stdout(stdout),
        ):
            self.assertEqual(0, extruder.main(["--verify"]))
        self.assertIn("SKIP: full extruder replay was not executed", stdout.getvalue())
        self.assertIn("compact index/report", stdout.getvalue())
        self.assertIn("gt6_extruder_templates.py", stdout.getvalue())

    def test_index_and_report_are_independently_consistent(self) -> None:
        index = json.loads(extruder.INDEX_OUT.read_text(encoding="utf-8"))
        report = json.loads(extruder.SUMMARY_OUT.read_text(encoding="utf-8"))

        self.assertEqual(5, index["schema_version"])
        self.assertEqual("gt.recipe.extruder", index["map"])
        self.assertTrue(index["replay_verified"])
        self.assertTrue(report["verification"]["replay_verified"])
        self.assertTrue(report["verification"]["partition_verified"])
        self.assertEqual(
            report["source"]["recipe_count"],
            index["source_recipe_count"],
        )
        self.assertEqual(
            report["counts"]["shape_template_count"],
            index["functional_template_count"],
        )
        self.assertEqual(
            report["counts"]["logical_recipe_count"],
            index["logical_recipe_count"],
        )
        self.assertEqual(
            sum(row["expanded_count"] for row in index["templates"])
            + sum(row["multiplicity"] for row in index["exact_remainder"]),
            index["source_recipe_count"],
        )
        self.assertEqual(
            len({row["template_id"] for row in index["templates"]}),
            len(index["templates"]),
        )
        self.assertTrue(all(
            row["replay_verified"] for row in index["templates"]
        ))
        self.assertEqual(
            "one template per concrete extruder shape",
            report["template_policy"]["template_identity"],
        )
        self.assertIn("sparse relation", report["template_policy"]["support"])


@unittest.skipUnless(
    extruder.MAP_PATH.is_file() and extruder.MATERIALS_PATH.is_file(),
    "full GT6 extruder replay dump is not installed",
)
class ExtruderTemplateTest(unittest.TestCase):
    def test_sentinel_material_id_is_not_a_domain_identity(self) -> None:
        self.assertNotIn(-1, extruder._material_catalog())

    def test_graphite_eu_special_precedes_future_tags(self) -> None:
        materials = copy.deepcopy(extruder._material_catalog())
        graphite = next(
            mid for mid, row in materials.items()
            if row.get("nameInternal") == "Graphite"
        )
        materials[graphite]["tags"] = [
            *materials[graphite].get("tags", []),
            "PROCESSING.EXTRUDABLE_SIMPLE",
        ]
        eu = extruder._derive_material_eu(
            {"heat_mode": "normal", "template_id": "test"},
            {"input_material_ids": [graphite]},
            materials,
        )
        self.assertEqual(512, eu)

    def test_pure_material_eu_replays_from_processing_tags(self) -> None:
        materials = extruder._material_catalog()
        simple = next(
            mid for mid, row in materials.items()
            if "PROCESSING.EXTRUDABLE_SIMPLE" in (row.get("tags") or [])
        )
        standard = next(
            mid for mid, row in materials.items()
            if "PROCESSING.EXTRUDABLE" in (row.get("tags") or [])
            and "PROCESSING.EXTRUDABLE_SIMPLE" not in (row.get("tags") or [])
        )
        graphite = next(
            mid for mid, row in materials.items()
            if row.get("nameInternal") == "Graphite"
        )
        document = extruder.extract_document([
            material_recipe(simple, eu_per_tick=16),
            material_recipe(standard, eu_per_tick=96),
            material_recipe(graphite, eu_per_tick=512),
            material_recipe(simple, shape_meta=10203, eu_per_tick=16),
        ])
        self.assertTrue(document["verification"]["replay_verified"])
        self.assertEqual(4, document["counts"]["tag_derived_eu_rows"])
        self.assertEqual(0, document["counts"]["explicit_eu_rows"])
        for template in document["templates"]:
            relation = template["relations"]["support"]
            if "euPerTick" not in relation["columns"]:
                continue
            eu_index = relation["columns"].index("euPerTick")
            self.assertTrue(
                all(row[eu_index] is None for row in relation["rows"])
            )

    def test_support_rows_use_template_local_indexes(self) -> None:
        document = extruder.extract_document([
            material_recipe(meta, duration=meta + 20)
            for meta in (10, 20, 30, 40, 50)
        ])
        template = document["templates"][0]
        relation = template["relations"]["support"]
        self.assertEqual(
            "compact_template_local_index_rows",
            relation["kind"],
        )
        self.assertEqual(
            [
                "subject",
                "input_form",
                "output_form",
                "multiplicity",
                "meta_bindings",
            ],
            relation["columns"][:5],
        )
        self.assertIn("duration", relation["columns"])
        self.assertTrue(all(isinstance(row, list) for row in relation["rows"]))
        self.assertTrue(all(row[4] is None for row in relation["rows"]))
        self.assertNotIn(
            "subject_id",
            template["axes"]["subject"]["values"][0],
        )
        self.assertIsInstance(
            template["axes"]["input_form"]["values"][0],
            list,
        )

    def test_template_boundary_is_shape_not_sample_or_operation(self) -> None:
        four = extruder.extract_document(
            [material_recipe(meta) for meta in (10, 20, 30, 40)]
        )
        self.assertEqual(1, four["counts"]["shape_template_count"])
        self.assertEqual(1, four["counts"]["logical_recipe_count"])

        five = extruder.extract_document(
            [material_recipe(meta) for meta in (10, 20, 30, 40, 50)]
        )
        self.assertEqual(1, five["counts"]["shape_template_count"])
        self.assertEqual(1, five["counts"]["logical_recipe_count"])
        self.assertTrue(five["verification"]["replay_verified"])

        two_shapes = extruder.extract_document([
            material_recipe(10, shape_meta=10003),
            material_recipe(10, shape_meta=10203),
        ])
        self.assertEqual(2, two_shapes["counts"]["shape_template_count"])

    def test_external_classification_is_diagnostic_not_template_count(self) -> None:
        family = extruder.extract_document([
            external_recipe(index, 10 + index, 32 + index)
            for index in range(5)
        ])
        self.assertEqual(1, family["counts"]["shape_template_count"])
        self.assertEqual(
            1,
            family["external_axis_diagnostics"][
                "mergeable_logic_family_count"
            ],
        )

        pair = extruder.extract_document([
            external_recipe(index, 10 + index, 32 + index)
            for index in range(2)
        ])
        self.assertEqual(1, pair["counts"]["shape_template_count"])
        self.assertEqual(
            1,
            pair["external_axis_diagnostics"]["small_review_group_count"],
        )
        self.assertTrue(pair["verification"]["replay_verified"])

    def test_template_id_is_content_addressed_not_position_addressed(self) -> None:
        recipes = [
            material_recipe(meta, duration=meta + 20)
            for meta in (10, 20, 30, 40, 50)
        ]
        forward = extruder.extract_document(recipes)
        reverse = extruder.extract_document(list(reversed(recipes)))
        self.assertEqual(
            [row["template_id"] for row in forward["templates"]],
            [row["template_id"] for row in reverse["templates"]],
        )

    def test_dependency_mapping_mutation_breaks_local_replay(self) -> None:
        document = extruder.extract_document([
            material_recipe(meta, duration=meta + 20)
            for meta in (10, 20, 30, 40, 50)
        ])
        template = copy.deepcopy(document["templates"][0])
        relation = template["relations"]["support"]
        support = relation["rows"][0]
        duration_index = relation["columns"].index("duration")
        support[duration_index] += 1
        replay_counter = extruder._replay_shape_template(template)
        self.assertNotEqual(
            template["source_multiset_sha256"],
            extruder.multiset_digest(replay_counter),
        )

    @unittest.skipUnless(
        extruder.OUT.is_file(),
        "expanded extruder replay cache is not installed",
    )
    def test_pinned_extruder_artifact_replays_exactly(self) -> None:
        document = json.loads(extruder.OUT.read_text(encoding="utf-8"))
        result = extruder.verify_document(document)
        self.assertTrue(result["replay_verified"], result)
        self.assertEqual(325595, result["expected_count"])
        self.assertEqual(0, result["missing_count"])
        self.assertEqual(0, result["extra_count"])
        self.assertEqual(62, document["counts"]["shape_template_count"])
        self.assertEqual(31, document["counts"]["normal_shape_template_count"])
        self.assertEqual(31, document["counts"]["low_heat_shape_template_count"])
        self.assertEqual(63, document["counts"]["logical_recipe_count"])
        self.assertEqual(5, document["schema_version"])
        self.assertLess(extruder.OUT.stat().st_size, 30_000_000)
        self.assertEqual(283775, document["counts"]["tag_derived_eu_rows"])
        self.assertEqual(
            {"16": 120202, "96": 163571, "512": 2},
            document["counts"]["tag_derived_eu_value_counts"],
        )
        self.assertGreater(
            document["counts"]["constant_domain_group_count"],
            1000,
        )
        self.assertGreater(
            document["counts"]["constant_domain_recipe_count"],
            90000,
        )
        self.assertLess(
            document["counts"]["stored_domain_exception_count"],
            500,
        )


if __name__ == "__main__":
    unittest.main()
