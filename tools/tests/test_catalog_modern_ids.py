#!/usr/bin/env python3
"""Numbered catalog identities use modern semantic ids."""
from __future__ import annotations

import re
import unittest

from tools import capability_ledger as ledger
from tools import catalog_modern_ids as modern
from tools import census_common as census
from tools import tool_head_prefix as thp

SLUG = "registry/catalog-modern-ids"
NUMBERED = re.compile(
    r"(?:gt_mte/mte_\d+|gt_multiitem/.+_m\d+|gt_block/.+_m\d+"
    r"|gt_stone/.+_m\d+|gt_object/.+_m\d+)"
)


class CatalogModernIdsTest(unittest.TestCase):
    def test_unique_active_and_runtime_ready(self) -> None:
        capability = census.load_json(
            census.TOOLS
            / "capabilities"
            / "registry"
            / "catalog-modern-ids"
            / "capability.json"
        )
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertNotEqual("player_complete", capability["maturity"])
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
        self.assertNotIn(SLUG, compiled["declared_player_complete"])

    def test_authority_table_covers_catalogs_without_numbered_tails(self) -> None:
        self.assertEqual([], modern.check())
        document = census.load_json(modern.MAP_PATH)
        self.assertGreater(int(document["row_count"]), 0)
        self.assertEqual(len(document["rows"]), int(document["row_count"]))
        paths = [row["registry_path"] for row in document["rows"]]
        self.assertEqual(len(paths), len(set(paths)))
        for row in document["rows"]:
            self.assertFalse(NUMBERED.search(str(row["registry_path"])))
            self.assertFalse(re.search(r"_m\d+$", str(row["registry_path"])))
            self.assertTrue(modern.PATH_RE.fullmatch(str(row["registry_path"])))
            self.assertFalse(modern.is_garbage_name(str(row["english_name"])))

    def test_tool_head_remainder_is_empty(self) -> None:
        remainder = thp.load_remap().get("remainder") or []
        self.assertEqual([], remainder)

    def test_live_hosts_are_not_stolen(self) -> None:
        live = modern.live_host_paths()
        self.assertIn("adamantium/fluid_pipe", live)
        self.assertIn("tin/wire", live)
        self.assertIn("tin/item_pipe", live)
        self.assertIn("slicer", live)
        self.assertIn("steel_dust_funnel", live)
        self.assertIn("lead_boiler", live)
        self.assertIn("lead_hopper", live)
        self.assertIn("sifter", live)
        self.assertIn("neutron_reflector_rod", live)
        document = census.load_json(modern.MAP_PATH)
        mapped = {row["registry_path"] for row in document["rows"]}
        folded = modern.folded_existing_item_paths()
        self.assertEqual([], sorted((mapped & live) - folded))

    def test_osmium_display_name_is_not_germanium_alias(self) -> None:
        materials = modern._material_index()
        self.assertEqual("osmium_elemental", materials["osmium"])
        self.assertEqual("osmium_elemental", materials["os"])
        self.assertEqual("germanium", materials["germanium"])
        self.assertEqual(
            "osmium_elemental",
            modern._english_material("Osmium Item Pipe", materials),
        )
        self.assertEqual(
            "germanium",
            modern._english_material("Germanium Item Pipe", materials),
        )
        row = next(
            item
            for item in census.load_json(modern.MAP_PATH)["rows"]
            if int(item["meta"]) == 25302
            and item["source_item"] == "gregtech:gt.multitileentity"
        )
        self.assertEqual("Osmium Item Pipe", row["english_name"])
        self.assertIn("osmium", row["registry_path"])
        self.assertNotIn("germanium", row["registry_path"])

    def test_existing_item_targets_are_registered(self) -> None:
        registered = modern.registered_holdable_paths()
        catalog = census.load_json(
            census.ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "smelter_mte_identity_catalog.json"
        )
        dangling = [
            (row["meta"], row["registry_path"])
            for row in catalog["identities"]
            if row.get("registry_kind") == "existing_item"
            and row["registry_path"] not in registered
        ]
        self.assertEqual([], dangling)


if __name__ == "__main__":
    unittest.main()
