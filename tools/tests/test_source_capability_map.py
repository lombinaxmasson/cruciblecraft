#!/usr/bin/env python3
"""Source-capability-map registration, leftover, and sealed artifacts."""
from __future__ import annotations

import unittest

from tools import portfolio_source_capability as cap
from tools import t35_common as t35
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for


class SourceCapabilityMapRegistrationTest(unittest.TestCase):
    def test_slugs_are_registered(self) -> None:
        expected = {
            "portfolio/source-capability-map-r0": (
                "portfolio/source-capability-inventory"
            ),
            "portfolio/source-capability-inventory": (
                "portfolio/source-capability-growth-order"
            ),
            "portfolio/source-capability-growth-order": (
                "portfolio/source-capability-map"
            ),
        }
        for slug, nxt in expected.items():
            self.assertIn(slug, KNOWN_SEMANTIC_SLUGS)
            spec = spec_for(slug)
            self.assertEqual(nxt, spec.unique_active_wave)
            self.assertFalse(spec.next_unassigned)
            self.assertEqual(0, spec.owns_families)
            self.assertIsNone(spec.production_lock)

    def test_program_is_terminal(self) -> None:
        slug = "portfolio/source-capability-map"
        self.assertIn(slug, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(slug)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)

    def test_schema_enums_are_frozen(self) -> None:
        schema = cap.schema_document()
        self.assertEqual(
            [
                "equivalent",
                "selected_subset",
                "identity_only",
                "capability_missing",
                "post_1x",
                "out_of_scope_historical",
            ],
            schema["allowed_correspondence_classes"],
        )
        self.assertEqual(
            ["cover", "loader", "oredict", "tileentity", "worldgen"],
            schema["allowed_gt6_domains"],
        )
        self.assertEqual(7, len(schema["row_fields"]))


class SourceCapabilityMapArtifactsTest(unittest.TestCase):
    def test_r0_when_present(self) -> None:
        root = t35.TOOLS / "waves" / "portfolio" / "source-capability-map-r0"
        if not (root / "readiness.json").is_file():
            self.skipTest("R0 artifacts not written yet")
        readiness = t35.load_json(root / "readiness.json")
        self.assertEqual("SOURCE_CAPABILITY_MAP_R0_READY", readiness["status"])
        self.assertEqual(
            "portfolio/source-capability-inventory",
            readiness["unique_active_wave"],
        )
        self.assertEqual(0, int(readiness["evidence"]["completion_delta"]))
        self.assertFalse(readiness["evidence"]["recipe_files_generated"])
        seed = t35.load_json(root / "inherited_seed.json")
        self.assertEqual(22, int(seed["row_count"]))
        leftover = t35.load_json(root / "leftover_later.json")
        self.assertEqual(39, int(leftover["counts"]["total"]))
        self.assertEqual([], leftover["hanging_unaccounted"])
        refs = t35.load_json(root / "count_ceiling_refs.json")
        self.assertEqual(5, len(refs["refs"]))

    def test_inventory_when_present(self) -> None:
        root = t35.TOOLS / "waves" / "portfolio" / "source-capability-inventory"
        if not (root / "readiness.json").is_file():
            self.skipTest("inventory artifacts not written yet")
        coverage = t35.load_json(root / "coverage.json")
        self.assertEqual(22, int(coverage["rows_from_seed"]))
        self.assertEqual(39, int(coverage["leftover_later_accounted"]))
        self.assertTrue(coverage["dump_present"])
        self.assertGreater(int(coverage["rows_new"]), 0)
        capability = t35.load_json(root / "capability_map.json")
        inherited = [
            row for row in capability["rows"] if row.get("inherited_from_seed")
        ]
        self.assertEqual(22, len(inherited))

    def test_growth_when_present(self) -> None:
        root = t35.TOOLS / "waves" / "portfolio" / "source-capability-growth-order"
        if not (root / "readiness.json").is_file():
            self.skipTest("growth-order artifacts not written yet")
        order = t35.load_json(root / "growth_order.json")
        self.assertFalse(order["nuclear_started"])
        self.assertTrue(order["next_major"])
        later = order["later_star_disposition"]
        self.assertEqual(
            2, len(later["later:assembler_combinatorial"]["families"])
        )
        self.assertEqual(
            2, len(later["later:electrolyzer_combinatorial"]["families"])
        )
        self.assertEqual(
            "remain_independent_not_next_major",
            later["post_1x:nuclear"]["disposition"],
        )

    def test_program_when_present(self) -> None:
        root = t35.TOOLS / "waves" / "portfolio" / "source-capability-map"
        if not (root / "readiness.json").is_file():
            self.skipTest("program artifacts not written yet")
        readiness = t35.load_json(root / "readiness.json")
        self.assertEqual("SOURCE_CAPABILITY_MAP_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        self.assertIsNone(readiness["evidence"]["production_lock_for_next_major"])
        self.assertFalse(readiness["evidence"]["nuclear_track_c_started"])
        self.assertTrue(readiness["evidence"]["capability_map_current"])
        self.assertTrue(readiness["evidence"]["growth_order_current"])


if __name__ == "__main__":
    unittest.main()
