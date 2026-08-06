from __future__ import annotations

import copy
import hashlib
import json
import unittest
from pathlib import Path

from tools import build_t12_machine_readiness as builder
from tools import run_full_verification
from tools import verify_full_verification_report


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class T12MachineReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )

    def test_fixed_revision_and_required_source_symbols_are_pinned(self):
        source = self.document["gt6_source"]
        self.assertEqual(
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
            source["revision"],
        )
        self.assertEqual(
            {
                "energy_tags",
                "material_tiers",
                "multitile_loader",
                "basic_machine",
                "axle",
                "gearbox",
                "large_centrifuge",
            },
            set(source["source_files"]),
        )
        for record in source["source_files"].values():
            self.assertRegex(record["git_blob_sha1"], r"^[0-9a-f]{40}$")
            self.assertIn(source["revision"], record["raw_url"])
        anchors = self.document["source_verification"]["anchors"]
        self.assertIn("energy_identity_ru_ku_eu_hu", anchors)
        self.assertIn("axle_runtime", anchors)
        self.assertIn("gearbox_runtime", anchors)
        self.assertIn("large_centrifuge_symbol_and_structure", anchors)
        self.assertTrue(all(row["lines"] for row in anchors.values()))

    def test_ledgers_cover_all_processing_and_non_spec_boundaries(self):
        processing = self.document["processing_kind_ledger"]
        self.assertEqual(25, processing["counts"]["total"])
        self.assertEqual(24, processing["counts"]["configured"])
        self.assertEqual(1, processing["counts"]["bronze_crusher"])
        self.assertEqual(25, processing["counts"]["classified"])
        self.assertEqual(0, processing["counts"]["unclassified"])
        self.assertEqual(
            {
                "deferred_with_reason": 2,
                "fixed_utility": 4,
                "source_tiered": 19,
            },
            processing["counts"]["classifications"],
        )

        non_spec = self.document["non_spec_kind_ledger"]
        self.assertEqual(12, non_spec["counts"]["total"])
        self.assertEqual(12, non_spec["counts"]["classified"])
        self.assertEqual(0, non_spec["counts"]["unclassified"])
        rows = {row["id"]: row for row in non_spec["rows"]}
        self.assertEqual("KU", rows["steam_engine"]["energy_identity"])
        self.assertEqual("RU_TO_EU", rows["dynamo"]["energy_identity"])
        self.assertEqual("RU", rows["fuel_engine"]["energy_identity"])
        self.assertEqual(
            "UNVERIFIED",
            rows["burning_gas_generator"]["energy_identity"],
        )
        self.assertTrue(all(
            row["reason"] and row["revisit_point"]
            for row in processing["rows"] + non_spec["rows"]
        ))

    def test_scaling_matrix_is_independent_and_source_numeric(self):
        matrix = self.document["scaling_expected_matrix"]
        self.assertTrue(matrix["independent_expected_selector"])
        self.assertFalse(matrix["production_selector_reused"])
        self.assertEqual(9, matrix["counts"]["rows"])
        rows = {
            (row["machine"], row["source_tier"]): row
            for row in matrix["rows"]
        }
        for machine in ("centrifuge", "sifter", "electrolyzer"):
            self.assertEqual(
                [(16, 32, 64), (64, 128, 256), (256, 512, 1024)],
                [
                    (
                        rows[(machine, tier)]["input_min"],
                        rows[(machine, tier)]["nominal_input"],
                        rows[(machine, tier)]["input_max"],
                    )
                    for tier in (1, 2, 3)
                ],
            )
            self.assertEqual(
                [0, 1, 2],
                [
                    rows[(machine, tier)]["scenarios"]["standard"][
                        "overclock_steps"
                    ]
                    for tier in (1, 2, 3)
                ],
            )
            self.assertEqual(
                [0, 0, 0],
                [
                    rows[(machine, tier)]["scenarios"]["cheap"][
                        "overclock_steps"
                    ]
                    for tier in (1, 2, 3)
                ],
            )

        self.assertEqual(
            [24, 24, 24],
            [
                rows[("centrifuge", tier)]["scenarios"]["standard"][
                    "at_nominal"
                ]["effective_duration_ticks"]
                for tier in (1, 2, 3)
            ],
        )
        self.assertEqual(
            [24, 12, 6],
            [
                rows[("centrifuge", tier)]["scenarios"]["cheap"][
                    "at_nominal"
                ]["effective_duration_ticks"]
                for tier in (1, 2, 3)
            ],
        )
        self.assertEqual(
            [512, 512, 512],
            [
                rows[("sifter", tier)]["scenarios"]["standard"][
                    "at_nominal"
                ]["effective_duration_ticks"]
                for tier in (1, 2, 3)
            ],
        )
        self.assertEqual(
            [2560, 2560, 2560],
            [
                rows[("electrolyzer", tier)]["scenarios"]["standard"][
                    "at_nominal"
                ]["effective_duration_ticks"]
                for tier in (1, 2, 3)
            ],
        )
        self.assertEqual(
            {
                "recipe_over_window",
                "packet_overvoltage",
                "underpowered",
                "output_blocked",
            },
            set(matrix["failure_matrix"]),
        )

    def test_vertical_fact_mutations_fail_closed(self):
        policy = json.loads(builder.POLICY.read_text(encoding="utf-8"))
        for mutation in (
            lambda value: value["vertical_slice"]["machines"][
                "centrifuge"
            ]["tiers"][0].__setitem__("nominal_input", 64),
            lambda value: value["vertical_slice"]["machines"][
                "sifter"
            ].__setitem__("energy_identity", "RU"),
            lambda value: value["vertical_slice"]["machines"][
                "electrolyzer"
            ]["tiers"][0].__setitem__("cable_material", "copper"),
        ):
            candidate = copy.deepcopy(policy)
            mutation(candidate)
            with self.assertRaises(ValueError):
                builder.validate_vertical_facts(candidate)

    def test_components_network_and_structures_are_fail_closed(self):
        projection = self.document[
            "component_and_variant_projection"
        ]
        self.assertEqual(9, projection["counts"]["variants"])
        self.assertEqual(0, projection["counts"]["crafts_ready"])
        self.assertEqual(9, projection["counts"]["crafts_fail_closed"])
        self.assertTrue(all(
            row["non_casing_forms_reachable"]
            and row["blocking_components"]
            and row["craft_status"].startswith("FAIL_CLOSED")
            for row in projection["variants"]
        ))

        network = self.document["network_projection"]
        self.assertEqual(
            "Bronze Diesel Engine",
            network["selected_ru_producer"]["source_identity"],
        )
        self.assertEqual(
            "UNVERIFIED_SOURCE_FILE_NOT_IN_T12A_SET",
            network["axle"]["numeric_speed_status"],
        )
        self.assertEqual(1, network["axle"]["projected_registrations"])
        self.assertEqual(1, network["gearbox"]["projected_registrations"])

        structures = self.document["structure_projection"]
        self.assertEqual(27, structures["coke_oven"]["scan_volume"])
        large = structures["large_centrifuge"]
        self.assertEqual(18, large["scan_volume"])
        self.assertEqual(
            {"item_fluid": 15, "energy_input": 2},
            large["port_counts"],
        )
        self.assertEqual(1, large["controller_count"])
        audit = large["port_count_correction_audit"]
        self.assertEqual(
            16, audit["historical_expected_item_fluid_ports"]
        )
        self.assertEqual(
            "SUPERSEDED_INCORRECT_EXPECTATION",
            audit["historical_status"],
        )
        self.assertIn("controller", audit["error"].lower())
        self.assertIn(
            "15 item/fluid + 2 energy + 1 controller",
            audit["correction"],
        )
        self.assertEqual(
            "ABSENT_IN_PINNED_TREE",
            large["requested_symbol_status"],
        )

    def test_benchmark_and_zero_publication_gate(self):
        benchmark = self.document["capacity_matcher_benchmark"]
        dense = {
            row["supplies"]
            for row in benchmark["scenarios"]
            if row["id"].startswith("dense_consuming_")
        }
        self.assertEqual({12, 16, 32, 64}, dense)
        self.assertTrue(all(
            row["within_budget"] for row in benchmark["scenarios"]
        ))
        self.assertFalse(
            benchmark["decision"]["matcher_rewritten"]
        )
        self.assertEqual(
            12,
            benchmark["decision"]["current_presence_supply_cap"],
        )

        load = self.document["load_gate"]
        self.assertEqual(
            load["current"]["published_recipes"],
            load["projected_after_tiers_only"]["published_recipes"],
        )
        self.assertEqual(
            0,
            load["projected_after_tiers_only"][
                "concrete_recipe_id_set_change"
            ],
        )
        self.assertEqual(
            0, load["budgets"]["t12_authored_material_rules"]
        )

    def test_t5_5_owner_is_superseded_without_rewriting_history(self):
        supersession = self.document["supersession"]
        self.assertEqual(
            "tools/machine_crafting_readiness.json",
            supersession["historical_artifact"],
        )
        self.assertEqual(
            "tier_collapsed_pending_t6",
            supersession["historical_owner"],
        )
        self.assertTrue(supersession["history_preserved"])
        self.assertIn(
            "RU/KINETIC_ROTATION",
            supersession["material_tag_policy"]["replacement"],
        )

    def test_committed_historical_readiness_check_is_immutable(self):
        before = digest(builder.OUTPUT)
        self.assertEqual([], builder.reference_only_check())
        self.assertEqual(before, digest(builder.OUTPUT))
        self.assertEqual(
            self.document,
            json.loads(builder.OUTPUT.read_text(encoding="utf-8")),
        )
        self.assertEqual("t12a_machine_readiness.json", builder.OUTPUT.name)
        self.assertEqual(
            "T12A_PREPROJECTION_READY",
            self.document["load_gate"]["status"],
        )

    def test_full_verification_tracks_immutable_t12a_history(self):
        self.assertIn(
            (
                "tools/build_t12_machine_readiness.py",
                "--check",
                "--reference-only",
            ),
            run_full_verification.BUILDER_CHECKS,
        )
        self.assertIn(
            "t12a_machine_readiness.json",
            verify_full_verification_report.CORE_ARTIFACTS,
        )
        self.assertIn(
            Path(__file__).resolve().parents[1]
            / "build_t12_machine_readiness.py",
            verify_full_verification_report.tooling_paths(),
        )
        self.assertEqual("T12A_READY", self.document["status"])
        self.assertFalse(self.document["full_t12_closure_claimed"])


if __name__ == "__main__":
    unittest.main()
