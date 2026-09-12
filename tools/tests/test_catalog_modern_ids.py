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
        self.assertEqual("active", capability["workflow"])
        self.assertNotEqual("player_complete", capability["maturity"])
        compiled = ledger.compile_ledger()
        self.assertEqual(SLUG, compiled["unique_active_slug"])
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
        reserved = {
            "slicer",
            "tin/wire",
            "tin/item_pipe",
            "steel_dust_funnel",
        }
        document = census.load_json(modern.MAP_PATH)
        mapped = {row["registry_path"] for row in document["rows"]}
        self.assertTrue(reserved.isdisjoint(mapped))


if __name__ == "__main__":
    unittest.main()
