#!/usr/bin/env python3
"""T13c exclusion reclaim R0 registration and frozen artifacts."""
from __future__ import annotations

import unittest

from tools import portfolio_t13c_exclusion_reclaim as reclaim
from tools import census_common as census
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "portfolio/exclusion-reclaim-r0"
PINNED = ("Panels", "Sensors", "Portals", "Batteries", "Reactors")


class T13cExclusionReclaimR0RegistrationTest(unittest.TestCase):
    def test_slug_is_terminal(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)


class T13cExclusionReclaimR0ArtifactsTest(unittest.TestCase):
    def test_artifacts_when_present(self) -> None:
        root = census.TOOLS / "waves" / "portfolio" / "t13c-exclusion-reclaim-r0"
        if not (root / "readiness.json").is_file():
            self.skipTest("R0 artifacts not written yet")
        readiness = census.load_json(root / "readiness.json")
        self.assertEqual("T13C_EXCLUSION_RECLAIM_R0_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        evidence = readiness["evidence"]
        self.assertEqual(0, int(evidence["completion_delta"]))
        self.assertEqual(0, int(evidence["owns_families"]))
        self.assertEqual(0, int(evidence["generated_recipe_count"]))
        self.assertFalse(evidence["recipe_files_generated"])
        self.assertEqual(39, int(evidence["leftover_later_count"]))
        self.assertEqual(129, int(evidence["inherited_source_sites"]))
        self.assertEqual(471, int(evidence["inherited_expanded_multiplicity"]))
        self.assertFalse(evidence["nuclear_track_c_started"])
        self.assertFalse(evidence["allows_implementation_child"])
        self.assertEqual(
            "requires_new_runtime",
            evidence["feasibility_by_category"]["Panels"],
        )
        self.assertEqual(
            "defer_to_portfolio",
            evidence["feasibility_by_category"]["Reactors"],
        )
        inherited = census.load_json(root / "inherited_denominator.json")
        self.assertEqual(
            list(PINNED),
            [row["category"] for row in inherited["category_summaries"]],
        )
        self.assertEqual(129, int(inherited["source_site_count"]))
        self.assertEqual(471, int(inherited["expanded_multiplicity"]))
        self.assertEqual(6, int(inherited["category_summaries"][0]["source_sites"]))
        self.assertEqual(
            348,
            int(inherited["category_summaries"][0]["expanded_multiplicity"]),
        )
        self.assertEqual(763, int(inherited["full_t35"]["source_sites"]))
        self.assertEqual(1701, int(inherited["full_t35"]["expanded_multiplicity"]))
        panel_rows = [
            row for row in inherited["source_sites"] if row["category"] == "Panels"
        ]
        self.assertEqual(6, len(panel_rows))
        self.assertEqual(348, sum(int(row["multiplicity"]) for row in panel_rows))
        feasibility = census.load_json(root / "feasibility.json")
        by_category = {row["category"]: row for row in feasibility["categories"]}
        self.assertEqual(list(PINNED), list(by_category))
        for name in ("Panels", "Sensors", "Portals", "Batteries"):
            self.assertEqual("requires_new_runtime", by_category[name]["verdict"])
            self.assertFalse(by_category[name]["allows_implementation_child"])
            self.assertIn(by_category[name]["verdict"], reclaim.FEASIBILITY_VALUES)
        self.assertEqual("defer_to_portfolio", by_category["Reactors"]["verdict"])
        self.assertEqual("portfolio/nuclear", by_category["Reactors"]["destination"])
        self.assertFalse(by_category["Reactors"]["allows_implementation_child"])
        topology = census.load_json(root / "topology.json")
        self.assertIsNone(topology["unique_active_wave"])
        self.assertTrue(topology["next_unassigned"])
        self.assertEqual(set(reclaim.ALLOWED_TOPOLOGY_KEYS), set(topology))
        census = census.load_json(root / "census_delta.json")
        self.assertEqual(39, int(census["leftover_later_count"]))
        wave = census.load_json(root / "wave.json")
        self.assertEqual(SLUG, wave["program"])
        self.assertEqual(["portfolio/logistics-cover-net-r0"], wave["depends_on"])
        contract = census.load_json(root / "reclaim_contract.json")
        self.assertFalse(contract["implemented"])
        self.assertIn("load", contract["questions"])
        self.assertIn("panel_semantics", contract["questions"])
        mechanism = census.load_json(root / "existing_mechanism.json")
        self.assertEqual(9, int(mechanism["definition_count"]))
        self.assertEqual(8, int(mechanism["behavior_count"]))
        self.assertEqual(6, len(mechanism["energy_converter_profiles"]))
        self.assertEqual(6, len(mechanism["capability_map_t13c"]))
        self.assertTrue(all(row["cc_mechanism"] == "none" for row in mechanism["kinds"]))
        semantics = census.load_json(root / "source_semantics.json")
        names = [row["category"] for row in semantics["categories"]]
        self.assertEqual(list(PINNED), names)
        panels = semantics["categories"][0]
        self.assertTrue(panels["contrast"]["sites_are_not_registrations"])
        self.assertEqual(6, len(panels["contrast"]["site_multiplicities"]))
        sensors = semantics["categories"][1]
        self.assertEqual(
            6,
            int(sensors["contrast"]["redstone_wires_not_in_slice"]["source_sites"]),
        )
        self.assertFalse(sensors["contrast"]["cover_cover_has_sensor"])
        portals = semantics["categories"][2]
        self.assertTrue(portals["contrast"]["portal_behavior_classes"])
        batteries = semantics["categories"][3]
        self.assertFalse(batteries["contrast"]["energy_chain_is_battery_catalog"])
        reactors = semantics["categories"][4]
        self.assertTrue(reactors["contrast"]["part_not_controller"])
        self.assertEqual("portfolio/nuclear", reactors["contrast"]["destination_portfolio"])


if __name__ == "__main__":
    unittest.main()
