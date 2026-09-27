from __future__ import annotations

import importlib.util
import json
import sys
import unittest
from collections import Counter
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
ASSESSMENT = (
    ROOT
    / "tools"
    / "waves"
    / "portfolio"
    / "gt6-full-coverage-reassessment"
)
MARKDOWN = ROOT / "docs" / "current" / "gt6-full-coverage.md"


def load(name: str):
    return json.loads((ASSESSMENT / name).read_text(encoding="utf-8"))


def load_builder():
    spec = importlib.util.spec_from_file_location(
        "gt6_full_coverage_builder", ASSESSMENT / "build_reconciliation.py"
    )
    module = importlib.util.module_from_spec(spec)
    assert spec.loader is not None
    sys.modules[spec.name] = module
    spec.loader.exec_module(module)
    return module


class Gt6FullCoverageReassessmentTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.coverage = load("coverage.json")
        cls.chemistry = load("chem_thermal.json")
        cls.scope = load("scope.json")
        cls.attribution = load("source_attribution.json")
        cls.by_source = {row["source_map"]: row for row in cls.coverage["recipe_maps"]}

    def test_frozen_source_scope_is_current(self):
        self.assertEqual(
            self.scope["source"]["revision"],
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
        )
        self.assertEqual(self.coverage["scope"]["source"], self.scope["source"])
        self.assertTrue(self.coverage["source_check"]["matches"])

    def test_reconciliation_keeps_independent_units(self):
        summary = self.coverage["summary"]
        self.assertEqual(summary["recipe_map_count"], 95)
        self.assertEqual(summary["recipe_source_rows"], 720841)
        capability_files = list((ROOT / "tools" / "capabilities").rglob("capability.json"))
        self.assertEqual(summary["capability_count"], len(capability_files))
        catalog = json.loads((ROOT / "tools" / "blockers" / "catalog.json").read_text(encoding="utf-8"))
        entries = catalog.get("entries", catalog)
        self.assertEqual(summary["blocker_count"], len(entries))
        self.assertNotEqual(summary["recipe_source_rows"], summary["capability_count"])

    def test_raw_row_classes_partition_the_denominator(self):
        summary = self.coverage["summary"]
        grades = summary["recipe_evidence_grades"]
        self.assertEqual(summary["recipe_source_rows"], grades["source_rows"])
        self.assertEqual(summary["recipe_source_rows"], sum(grades["classes"].values()))
        self.assertEqual(summary["recipe_source_rows"], sum(grades["groups"].values()))
        self.assertEqual(grades, self.coverage["recipe_evidence_grades"])
        for row in self.coverage["recipe_maps"]:
            self.assertEqual(
                row["source_rows"],
                sum(row["raw_row_classes"].values()),
                row["source_map"],
            )
        semantic = self.coverage["semantic_coverage"]
        self.assertEqual(2, semantic["schema_version"])
        self.assertEqual(summary["recipe_source_rows"], semantic["total_rows"])
        self.assertEqual(9642, grades["classes"]["legacy_exclusion_pending"])
        self.assertGreaterEqual(
            grades["classes"]["source_exact"],
            summary["traced_source_rows"] - 1000,
        )

    def test_translator_is_calibrated_on_hash_proven_rows(self):
        calibration = self.coverage["semantic_coverage"]["calibration"]
        self.assertGreater(calibration["pairs"], 100000)
        self.assertEqual(
            calibration["pairs"],
            calibration["equal"] + calibration["differs"] + calibration["untranslatable"],
        )
        self.assertGreaterEqual(calibration["agreement"], 0.99)
        for sample in calibration["differs_samples"]:
            self.assertTrue({"map", "index", "sha256"} <= set(sample))

    def test_extruder_shapes_are_not_blamed_on_the_translator(self):
        extruder = self.by_source["gt.recipe.extruder"]["raw_row_classes"]
        blockers = self.coverage["semantic_coverage"]["blockers"]
        shapes = [entry for entry in blockers if entry["kind"] == "extruder_shape"]
        self.assertFalse(shapes)

    def test_progress_uses_decided_exclusions_only(self):
        grades = self.coverage["summary"]["recipe_evidence_grades"]
        progress = grades["progress"]
        self.assertEqual(
            progress["target_rows"],
            progress["source_rows"] - progress["excluded_rows"],
        )
        exclusions = load("exclusions.json")
        self.assertEqual(
            [
                "gt6-nei-display-rows",
                "external-mod-forgemicroblock-stonerod",
                "external-mod-binnie-juicecranberry",
            ],
            [rule["id"] for rule in exclusions["rules"]],
        )
        self.assertEqual(
            progress["excluded_rows"],
            sum(grades["excluded"].values()),
        )
        self.assertEqual(
            progress["proven_rows"],
            grades["classes"]["source_exact"] + grades["classes"]["translated_exact"],
        )
        self.assertGreater(progress["recipe_progress"], grades["groups"]["proven"] / progress["source_rows"])
        for row in self.coverage["recipe_maps"]:
            if row["source_rows"]:
                self.assertEqual(
                    row["progress"]["target_rows"],
                    row["source_rows"] - sum(row["raw_row_excluded"].values()),
                    row["source_map"],
                )
        machine = grades["machine_progress"]
        self.assertGreater(machine["maps"], 50)
        self.assertLessEqual(machine["maps_complete"] + machine["maps_untouched"], machine["maps"])

    def test_missing_material_is_split_from_missing_form(self):
        classes = self.coverage["summary"]["recipe_evidence_grades"]["classes"]
        self.assertGreater(classes["missing_material_form"], 0)
        self.assertIn("missing_material", classes)
        kinds = {entry["kind"] for entry in self.coverage["semantic_coverage"]["blockers"]}
        self.assertTrue({"form", "fluid"} <= kinds)

    def test_recipe_maps_are_unique_and_complete(self):
        rows = self.coverage["recipe_maps"]
        self.assertEqual(len(rows), 95)
        self.assertEqual(
            len({row["normalized_map"] for row in rows}),
            len(rows),
        )
        self.assertTrue(all(row["source_revision"] == self.scope["source"]["revision"] for row in rows))

    def test_chemistry_contains_cracking_and_thermal_rows(self):
        names = {row["source_map"] for row in self.chemistry["maps"]}
        self.assertIn("gt.recipe.catalyticcracking", names)
        self.assertIn("gt.recipe.steamcracking", names)
        self.assertIn("gt.recipe.melter", names)
        self.assertGreater(self.chemistry["source_rows"], 0)
        self.assertEqual(self.chemistry["pipeline_deferred_map_count"], 11)
        self.assertEqual(self.chemistry["pipeline_deferred_source_rows"], 15811)
        cracking = {
            row["source_map"]: row
            for row in self.chemistry["maps"]
            if row["source_map"]
            in {"gt.recipe.catalyticcracking", "gt.recipe.steamcracking"}
        }
        self.assertEqual(cracking["gt.recipe.catalyticcracking"]["capabilities"], [])
        self.assertEqual(
            ["recipe/gt6-steamcracking-bulk"],
            [row["slug"] for row in cracking["gt.recipe.steamcracking"]["capabilities"]],
        )

    def test_every_denominator_axis_is_reconciled(self):
        self.assertEqual(96, len(self.coverage["machine_kinds"]))
        self.assertEqual(30, len(self.coverage["multiblock_kinds"]))
        self.assertEqual(47, len(self.coverage["cover_kinds"]))
        self.assertGreaterEqual(self.coverage["summary"]["live_cover_definitions"], 47)
        self.assertEqual(20, len(self.coverage["energy_identities"]))
        self.assertEqual(25, len(self.coverage["itemgenerator_domains"]))
        self.assertEqual(452, len(self.coverage["prefixes"]["rows"]))
        self.assertTrue(self.coverage["mte_identity_dispositions"]["by_family"])
        for row in self.coverage["machine_kinds"]:
            self.assertIn("delivery_depth", row)
        for row in self.coverage["multiblock_kinds"]:
            self.assertIn("delivery_depth", row)

    def test_cover_denominator_and_live_status_are_separate(self):
        covers = {row["canonical_id"]: row for row in self.coverage["cover_kinds"]}
        self.assertEqual("deferred_with_reason", covers["controller_auto"]["implementation_status"])
        self.assertTrue(
            any(
                definition["id"] == "cruciblecraft:controller_auto"
                for definition in covers["controller_auto"]["live_definitions"]
            )
        )
        text = MARKDOWN.read_text(encoding="utf-8")
        self.assertIn("CC live ids", text)
        self.assertNotIn("CC 行为", text)

    def test_markdown_lists_every_axis_not_only_chemistry(self):
        text = MARKDOWN.read_text(encoding="utf-8")
        for heading in (
            "## 2. 全部 GT6 配方图（95）",
            "## 4. 机器 kind（96）",
            "## 5. 多方块控制器（30）",
            "## 6. 盖板（47）",
            "## 7. 能量身份（20）",
            "## 8. 物品/流体生成域（25）",
            "## 9. 材料前缀（452）",
            "## 10. MTE 身份处置",
            f"## 12. Capability（{self.coverage['summary']['capability_count']}）",
            "## 13. 未关闭 blocker",
            "## 16. 各轴来源与新鲜度",
        ):
            self.assertIn(heading, text)
        for grade in (
            "source_exact",
            "translated_exact",
            "translatable_missing",
            "missing_material_form",
            "display_only",
        ):
            self.assertIn(grade, text)
        self.assertIn("互斥，合计等于分母", text)
        self.assertIn("翻译链校准", text)
        for row in self.coverage["recipe_maps"]:
            name = row["source_map"] or "(unnamed)"
            self.assertIn(f"`{name}`", text)
        self.assertIn("gt6-full-coverage-workflow.md", text)
        self.assertIn("### 2.2 逐行分类", text)
        self.assertIn("配方移植进度", text)
        self.assertIn("## 17. 缺口行动清单", text)

    def test_empty_maps_are_not_reported_as_full_replay(self):
        for row in self.coverage["recipe_maps"]:
            if row["source_rows"] == 0 and row["denominator_class"] != "out_of_scope":
                self.assertEqual("empty_source", row["delivery_depth"], row["source_map"])

    def test_known_ported_maps_resolve_to_local_names(self):
        by_source = self.by_source
        self.assertEqual("anvil_bend_big", by_source["gt.recipe.anvil.bend.big"]["local_map"])
        self.assertEqual("sanding", by_source["gt.recipe.sharpener"]["local_map"])
        self.assertEqual("fuels_engine", by_source["gt.recipe.fuels.engine"]["local_map"])
        self.assertGreater(by_source["gt.recipe.cutter"]["rule_expanded_recipes"], 0)
        self.assertNotEqual("runtime_only", by_source["gt.recipe.cutter"]["delivery_depth"])

    def test_shared_wave_overflow_is_not_double_counted(self):
        by_source = self.by_source
        self.assertEqual(0, by_source["gt.recipe.hammer"]["overflow_rows"])
        self.assertEqual(3155, by_source["gt.recipe.melter"]["overflow_rows"])
        self.assertGreater(by_source["gt.recipe.shredder"]["overflow_rows"], 0)

    def test_multiblocks_and_kinds_use_class_evidence(self):
        multiblocks = {row["canonical_id"]: row for row in self.coverage["multiblock_kinds"]}
        self.assertIn(
            "energy/large-gas-turbine",
            {cap["slug"] for cap in multiblocks["large_turbine_gas"]["capabilities"]},
        )
        self.assertEqual("runtime_code_uncarded", multiblocks["large_boiler"]["delivery_depth"])
        self.assertEqual("runtime_code_uncarded", multiblocks["tank_3x3x3"]["delivery_depth"])
        self.assertEqual("runtime_accepted", multiblocks["electrolyzer"]["delivery_depth"])
        self.assertEqual("runtime_accepted", multiblocks["matter_fabricator"]["delivery_depth"])
        self.assertEqual(
            ["machines/gt6-coil-hosts"],
            [cap["slug"] for cap in multiblocks["matter_fabricator"]["capabilities"]],
        )
        self.assertEqual(
            ["machines/large-crucible"],
            [cap["slug"] for cap in multiblocks["crucible"]["capabilities"]],
        )
        kinds = {row["behavior_class"]: row for row in self.coverage["machine_kinds"] if not row.get("recipe_map") or row["recipe_map"] == "NONE"}
        self.assertEqual("runtime_accepted", kinds["MultiTileEntityReactorCore1x1"]["delivery_depth"])
        self.assertEqual("runtime_accepted", kinds["MultiTileEntityAxle"]["delivery_depth"])
        self.assertEqual("runtime_accepted", kinds["MultiTileEntityEngineRotation"]["delivery_depth"])
        self.assertIn(
            "content/gt6-mte-drive-runtime",
            {cap["slug"] for cap in kinds["MultiTileEntityEngineRotation"]["evidence"]["capabilities"]},
        )
        self.assertEqual("runtime_accepted", kinds["MultiTileEntitySolarPanelElectric"]["delivery_depth"])
        self.assertNotIn(
            "needs_manual_audit",
            {row["delivery_depth"] for row in self.coverage["machine_kinds"]},
        )

    def test_status_axes_are_explicit(self):
        for row in self.coverage["recipe_maps"]:
            self.assertIn(
                row["delivery_depth"],
                {
                    "full_replay",
                    "empty_source",
                    "bounded_subset",
                    "runtime_only",
                    "identity_only",
                    "denominator_only",
                    "legacy_exclusion_pending",
                },
            )
            self.assertIn("freshness", row)
            self.assertIn("related_blockers", row)

    # Regression coverage for the first revision of this report, which only
    # scanned src/main + src/*_generated, only read the `map` field, and
    # compared CC file counts against GT6 source rows.

    def test_datagen_root_and_gradle_excludes_are_scanned(self):
        scan = self.coverage["recipe_scan"]
        self.assertIn("src/generated/resources", scan["roots"])
        self.assertIn("src/main/resources", scan["roots"])
        self.assertIn(
            "data/cruciblecraft/recipe/player_path_recovery/roaster/**",
            scan["gradle_excludes"],
        )
        for name in ("gt.recipe.freezer", "gt.recipe.fusionreactor", "gt.recipe.polarizer"):
            row = self.by_source[name]
            self.assertGreater(row["untraced_cc_rows"], 0, name)
            self.assertEqual("bounded_subset", row["delivery_depth"], name)

    def test_matrix_families_are_counted_as_source_rows(self):
        bath = self.by_source["gt.recipe.bath"]
        self.assertGreater(bath["traced_source_rows"], 40000)
        self.assertLessEqual(bath["traced_source_rows"], bath["source_rows"])
        self.assertGreater(self.by_source["gt.recipe.mixer"]["traced_source_rows"], 5000)

    def test_traced_rows_never_exceed_source_rows(self):
        for row in self.coverage["recipe_maps"]:
            self.assertLessEqual(row["traced_source_rows"], row["source_rows"], row["source_map"])
            if row["delivery_depth"] == "full_replay":
                self.assertEqual(row["traced_source_rows"], row["source_rows"], row["source_map"])
                self.assertEqual(0, row["overflow_rows"], row["source_map"])

    def test_fuel_maps_follow_row_evidence_not_names(self):
        gas = self.by_source["gt.recipe.fuels.gas"]
        self.assertEqual({"fuels_gas_turbine"}, set(gas["cc_host_maps"]))
        self.assertEqual("full_replay", gas["delivery_depth"])
        burn = self.by_source["gt.recipe.fuels.burn"]
        self.assertEqual({"fuels_gas"}, set(burn["cc_host_maps"]))
        self.assertNotEqual("denominator_only", burn["delivery_depth"])
        self.assertNotEqual("full_replay", self.by_source["gt.recipe.fuels.turbine"]["delivery_depth"])

    def test_identical_rows_stay_on_their_declared_map(self):
        # GT6 registers 6743 identical rows in melter and smelter; the CC
        # melter family must not leak into smelter coverage.
        self.assertEqual({"melter"}, set(self.by_source["gt.recipe.melter"]["cc_host_maps"]))
        self.assertNotIn("melter", self.by_source["gt.recipe.smelter"]["cc_host_maps"])
        self.assertNotIn("compressor", self.by_source["gt.recipe.rollingmill"]["cc_host_maps"])

    def test_cc_only_maps_are_not_dropped(self):
        rollbender = self.by_source["gt.recipe.rollbender"]
        self.assertIn("bender", rollbender["owned_cc_maps"])
        manifest = json.loads((ROOT / "tools" / "component_rule_manifest.json").read_text(encoding="utf-8"))
        per_map = manifest["per_map"]
        self.assertEqual(
            per_map["bender"]["expanded_recipes"] + per_map["rollbender"]["expanded_recipes"],
            rollbender["rule_expanded_recipes"],
        )
        owned = {cc for row in self.coverage["recipe_maps"] for cc in row["owned_cc_maps"]}
        unowned = {row["cc_map"] for row in self.coverage["unowned_cc_maps"]}
        self.assertFalse(owned & unowned)

    def test_attribution_pin_matches_report(self):
        scan = self.coverage["recipe_scan"]
        self.assertEqual(self.attribution["evidence_digest"], scan["attribution_digest"])
        self.assertEqual(0, scan["unverified_hashes"])

    def test_empty_source_machine_kinds_keep_their_host(self):
        furnace = [
            row for row in self.coverage["machine_kinds"] if row.get("recipe_map") == "RM.Furnace"
        ]
        self.assertTrue(furnace)
        for row in furnace:
            self.assertIn("cruciblecraft:oven", row["cc_hosts"])
            self.assertNotEqual("empty_source", row["delivery_depth"])

    def test_live_axes_are_not_frozen_copies(self):
        energy = {row["symbol"]: row for row in self.coverage["energy_identities"]}
        self.assertEqual("CU", energy["CRYO"]["live_energy_type"])
        self.assertEqual("TIME", energy["TIME"]["live_energy_type"])
        self.assertIsNone(energy["NEUTRON"]["live_energy_type"])
        prefixes = self.coverage["prefixes"]["rows"]
        self.assertGreaterEqual(
            sum(row["cc_mapped"] for row in prefixes),
            sum(row["frozen_cc_mapped"] for row in prefixes),
        )
        mte = self.coverage["mte_identity_dispositions"]
        self.assertEqual(sum(mte["counts"].values()), sum(mte["live_counts"].values()))
        self.assertGreater(mte["live_counts"].get("inplace_runtime", 0), 0)


class Gt6FullCoverageBuilderUnitTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.builder = load_builder()

    def test_gradle_glob_matches_like_gradle(self):
        regex = self.builder._gradle_glob_regex
        self.assertTrue(regex("**/*.bbmodel").match("assets/x/y.bbmodel"))
        self.assertTrue(regex("**/*.bbmodel").match("y.bbmodel"))
        pattern = regex("data/cruciblecraft/recipe/player_path_recovery/roaster/**")
        self.assertTrue(pattern.match("data/cruciblecraft/recipe/player_path_recovery/roaster/a.json"))
        self.assertFalse(pattern.match("data/cruciblecraft/recipe/player_path_recovery/other/a.json"))

    def test_declared_source_forms(self):
        declared = self.builder.declared_source
        self.assertEqual(("gt.recipe.bath", None), declared("gt.recipe.bath#0012", None))
        self.assertEqual(
            ("gt.recipe.fuels.burn", ("gt.recipe.fuels.burn", 32)),
            declared("gt6_dump/gt6_recipe_dump/maps/gt.recipe.fuels.burn.json#recipes[32]", None),
        )
        self.assertEqual(("gt.recipe.mixer", None), declared("sha256:abc", "gt.recipe.mixer#0001"))
        self.assertEqual((None, None), declared("topology_projection:abc", "sha256:abc"))

    def test_attribution_uses_overrides_and_skips_unverified(self):
        b = self.builder
        full_a = "a" * 64
        full_b = "b" * 64
        rows = [
            b.CcRecipeRow("melter", "gt.recipe.melter", None, (full_a,), (), "SOURCE_DERIVED"),
            b.CcRecipeRow("compressor", "gt.recipe.compressor", None, (full_b,), (), "SOURCE_DERIVED"),
            b.CcRecipeRow("freezer", None, None, (), (), "none"),
        ]
        owners = {"melter": "gt.recipe.melter", "compressor": "gt.recipe.compressor"}
        attribution = {
            "full_overrides": {f"gt.recipe.compressor|{full_b}": "gt.recipe.rollingmill"},
            "unverified_full_hashes": [],
            "ambiguous_hashes": [],
        }
        traces, untraced, counts = b.attribute_rows(rows, owners, attribution)
        self.assertEqual({full_a}, traces["gt.recipe.melter"].exact)
        self.assertEqual(Counter({"compressor": 1}), traces["gt.recipe.rollingmill"].exact_hosts)
        self.assertNotIn("gt.recipe.compressor", traces)
        self.assertEqual(Counter({"none": 1}), untraced["freezer"])
        self.assertEqual(3, sum(counts.values()))
        attribution["unverified_full_hashes"] = [full_a]
        traces, untraced, _ = b.attribute_rows(rows, owners, attribution)
        self.assertNotIn("gt.recipe.melter", traces)
        self.assertEqual(1, sum(untraced["melter"].values()))

    def test_digest_changes_when_evidence_changes(self):
        b = self.builder
        row = b.CcRecipeRow("bath", "gt.recipe.bath", None, ("c" * 64,), (), "SOURCE_DERIVED")
        other = b.CcRecipeRow("bath", "gt.recipe.bath", None, ("d" * 64,), (), "SOURCE_DERIVED")
        owners = {"bath": "gt.recipe.bath"}
        self.assertNotEqual(b.evidence_digest([row], owners), b.evidence_digest([row, other], owners))
        self.assertEqual(b.evidence_digest([row, row], owners), b.evidence_digest([row], owners))


if __name__ == "__main__":
    unittest.main()
