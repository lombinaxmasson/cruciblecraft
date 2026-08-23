"""Tests for the T35R machine / EU / TU track builder."""
from __future__ import annotations

import copy
import json
import sys
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t35_machine_track as builder  # noqa: E402
from tools import t35_common as t35  # noqa: E402
from tools import t36_common as t36  # noqa: E402


class T35MachineTrackTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = t35.load_json(builder.OUTPUT)

    @staticmethod
    def catalog() -> dict:
        return copy.deepcopy(t35.load_json(t35.MACHINE_TIERS))

    @staticmethod
    def build_with_catalog(catalog: dict) -> dict:
        real_load_json = t35.load_json

        def load_json(path: Path):
            if path == t35.MACHINE_TIERS:
                return copy.deepcopy(catalog)
            return real_load_json(path)

        with mock.patch.object(t35, "load_json", side_effect=load_json):
            return builder.build()

    def test_build_is_deterministic(self) -> None:
        self.assertEqual(
            t35.stable_json(builder.build()),
            t35.stable_json(builder.build()),
        )

    def test_selected_rows_preserve_exact_catalog_source_lineage(self) -> None:
        catalog = self.catalog()
        source_rows = {
            key: value
            for key, value in catalog["source"]["variant_rows"].items()
            if key in t36.OPENING_VARIANT_IDS
        }
        source_variants = t36.opening_variants(catalog)
        actual_variants = self.document["variants"]

        self.assertEqual(33, len(source_variants))
        self.assertEqual(33, self.document["selected_matrix"]["variant_count"])
        self.assertEqual(11, self.document["selected_matrix"]["kind_count"])
        self.assertEqual(
            {row["id"] for row in source_variants},
            {row["id"] for row in actual_variants},
        )
        self.assertEqual(
            {
                row["id"]: (
                    row["sourceId"],
                    row["sourceTier"],
                    source_rows[row["id"]],
                )
                for row in source_variants
            },
            {
                row["id"]: (
                    row["source_id"],
                    row["source_tier"],
                    row["source_row"],
                )
                for row in actual_variants
            },
        )
        self.assertEqual(
            {row["kind"] for row in source_variants},
            set(self.document["selected_matrix"]["kind_ids"]),
        )

    def test_selected_matrix_is_not_cartesian_completed(self) -> None:
        variants = self.document["variants"]
        kinds = {row["kind"] for row in variants}
        materials = {row["material"] for row in variants}
        selected_pairs = {(row["kind"], row["material"]) for row in variants}

        self.assertEqual(33, len(selected_pairs))
        self.assertGreater(len(kinds) * len(materials), len(selected_pairs))
        self.assertEqual(0, self.document["validators"]["cartesian_completion"])
        self.assertFalse(
            self.document["selected_matrix"]["automatic_kind_tier_completion"]
        )

    def test_eu_rows_are_voltage_only(self) -> None:
        eu_rows = [
            row
            for row in self.document["variants"]
            if row["energy_track"] == "EU"
        ]

        self.assertEqual(3, len(eu_rows))
        self.assertEqual(
            {
                "cruciblecraft:electric_tier_1",
                "cruciblecraft:electric_tier_2",
                "cruciblecraft:electric_tier_3",
            },
            {row["eu_voltage_band"] for row in eu_rows},
        )
        for row in eu_rows:
            self.assertEqual("ELECTRIC", row["energy"])
            self.assertIsNone(row["material_tier"])
            self.assertTrue(
                row["eu_voltage_band"].startswith("cruciblecraft:electric_tier_")
            )
        self.assertEqual(
            "voltage_band_pilot",
            self.document["selected_matrix"]["energy_tracks"]["EU"]["semantics"],
        )
        self.assertEqual(0, self.document["validators"]["eu_material_tier_mix"])

    def test_tu_time_is_not_missing_material_matrix(self) -> None:
        tu_track = self.document["selected_matrix"]["energy_tracks"]["TU"]

        self.assertEqual("TIME", tu_track["energy"])
        self.assertEqual(0, tu_track["rows_in_machine_tiers"])
        self.assertEqual("in_scope_1x", tu_track["identity_scope"])
        self.assertEqual("implemented", tu_track["identity_disposition"])
        self.assertFalse(
            any(row["energy"] == "TIME" for row in self.document["variants"])
        )
        self.assertEqual(0, self.document["validators"]["tu_rows_in_catalog"])

    def test_conclusion_a_requires_no_generated_machine_cards(self) -> None:
        conclusions = self.document["conclusions"]

        self.assertEqual("A", conclusions["material_tier_matrix"])
        self.assertEqual("A", conclusions["eu_voltage"])
        self.assertEqual("A_empty_in_catalog", conclusions["tu_processing"])
        self.assertFalse(conclusions["generated_material_cards_required"])
        self.assertFalse(conclusions["generated_eu_cards_required"])
        self.assertFalse(conclusions["generated_tu_cards_required"])
        self.assertEqual(0, conclusions["selected_unimplemented_rows"])

    def test_committed_artifact_if_present_is_current_and_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("t35_machine_track.json not generated")

        before = builder.OUTPUT.read_bytes()
        self.assertEqual(self.document, json.loads(before))
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_missing_source_lineage_fails_closed_without_writing_catalog(self) -> None:
        catalog = self.catalog()
        row = next(
            item for item in catalog["variants"]
            if item["id"] in t36.OPENING_VARIANT_IDS
        )
        row.pop("sourceId")

        with self.assertRaisesRegex(ValueError, "sourceId"):
            self.build_with_catalog(catalog)

    def test_invalid_catalog_semantics_fail_closed_without_writing_catalog(self) -> None:
        with self.subTest("automatic completion"):
            catalog = self.catalog()
            catalog["namingPolicy"]["automaticKindTierCompletion"] = True
            with self.assertRaisesRegex(ValueError, "automaticKindTierCompletion"):
                self.build_with_catalog(catalog)

        with self.subTest("EU material tier"):
            catalog = self.catalog()
            row = next(
                item for item in catalog["variants"]
                if item["id"] in t36.OPENING_VARIANT_IDS
                and item["energy"] == "ELECTRIC"
            )
            row["tierBand"] = "cruciblecraft:ru_tier_1"
            with self.assertRaisesRegex(ValueError, "EU row"):
                self.build_with_catalog(catalog)

        with self.subTest("TU catalog row"):
            catalog = self.catalog()
            row = next(
                item for item in catalog["variants"]
                if item["id"] in t36.OPENING_VARIANT_IDS
            )
            row["energy"] = "TIME"
            with self.assertRaisesRegex(ValueError, "TU/TIME"):
                self.build_with_catalog(catalog)

        with self.subTest("source row mismatch"):
            catalog = self.catalog()
            opening_id = next(iter(t36.OPENING_VARIANT_IDS))
            catalog["source"]["variant_rows"].pop(opening_id)
            with self.assertRaisesRegex(ValueError, "variant ids drifted"):
                self.build_with_catalog(catalog)


if __name__ == "__main__":
    unittest.main()
