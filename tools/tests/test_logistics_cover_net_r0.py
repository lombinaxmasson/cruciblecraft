#!/usr/bin/env python3
"""Logistics cover net R0 registration and frozen artifacts."""
from __future__ import annotations

import unittest

from tools import portfolio_logistics_cover_net as cover_net
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "portfolio/logistics-cover-net-r0"
PINNED = (
    "logistics_fluid_storage",
    "logistics_fluid_transfer",
    "logistics_generic_dump",
    "logistics_generic_storage",
    "logistics_generic_transfer",
    "logistics_item_storage",
    "logistics_item_transfer",
)
DISPLAY_CPU = (
    "logistics_display_cpu_control",
    "logistics_display_cpu_conversion",
    "logistics_display_cpu_logic",
    "logistics_display_cpu_storage",
)


class LogisticsCoverNetR0RegistrationTest(unittest.TestCase):
    def test_slug_is_terminal(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)


class LogisticsCoverNetR0ArtifactsTest(unittest.TestCase):
    def test_artifacts_when_present(self) -> None:
        root = census.TOOLS / "waves" / "portfolio" / "logistics-cover-net-r0"
        if not (root / "readiness.json").is_file():
            self.skipTest("R0 artifacts not written yet")
        readiness = census.load_json(root / "readiness.json")
        self.assertEqual("LOGISTICS_COVER_NET_R0_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        evidence = readiness["evidence"]
        self.assertEqual(0, int(evidence["completion_delta"]))
        self.assertEqual(0, int(evidence["owns_families"]))
        self.assertEqual(0, int(evidence["generated_recipe_count"]))
        self.assertFalse(evidence["recipe_files_generated"])
        self.assertEqual(39, int(evidence["leftover_later_count"]))
        self.assertFalse(evidence["allows_core_child"])
        self.assertEqual("requires_new_runtime", evidence["feasibility"])
        inherited = census.load_json(root / "inherited_denominator.json")
        self.assertEqual(list(PINNED), [row["canonical_id"] for row in inherited["kinds"]])
        self.assertEqual(
            list(DISPLAY_CPU),
            [row["canonical_id"] for row in inherited["display_cpu_out_of_scope"]],
        )
        for row in inherited["display_cpu_out_of_scope"]:
            self.assertEqual("out_of_scope", row["disposition"])
        self.assertEqual("multiblock_kinds", inherited["logistics_core"]["domain"])
        feasibility = census.load_json(root / "feasibility.json")
        self.assertIn(feasibility["verdict"], cover_net.FEASIBILITY_VALUES)
        self.assertEqual("requires_new_runtime", feasibility["verdict"])
        self.assertFalse(feasibility["allows_core_child"])
        topology = census.load_json(root / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertTrue(topology["next_unassigned"])
        census = census.load_json(root / "census_delta.json")
        self.assertEqual(39, int(census["leftover_later_count"]))
        wave = census.load_json(root / "wave.json")
        self.assertEqual(SLUG, wave["program"])
        self.assertEqual(["portfolio/generic-recipe-generator"], wave["depends_on"])
        contract = census.load_json(root / "network_contract.json")
        self.assertFalse(contract["implemented"])
        self.assertIn("load", contract["questions"])
        mechanism = census.load_json(root / "existing_mechanism.json")
        self.assertEqual(9, int(mechanism["definition_count"]))
        self.assertEqual(8, int(mechanism["behavior_count"]))
        self.assertTrue(all(row["cc_mechanism"] == "none" for row in mechanism["kinds"]))
        semantics = census.load_json(root / "source_semantics.json")
        roles = {row["role"] for row in semantics["kinds"]}
        self.assertEqual({"dump", "storage", "transfer"}, roles)
        for row in semantics["kinds"]:
            if row["role"] == "transfer":
                self.assertIsNotNone(row["direction_collapse"])
                self.assertIn("direction", row["direction_collapse"]["variant_dimensions"])


if __name__ == "__main__":
    unittest.main()
