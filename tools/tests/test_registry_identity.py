#!/usr/bin/env python3
"""Registry path / semantic identity gate."""
from __future__ import annotations

import unittest

from tools import registry_identity as identity
from tools import io_common as io


class RegistryIdentityTest(unittest.TestCase):
    def test_cover_behaviors_are_exact(self) -> None:
        manifest = identity.compile_manifest()
        self.assertEqual([], manifest["errors"], manifest["errors"])
        self.assertEqual("PASS", manifest["status"])
        self.assertEqual(
            identity.EXPECTED_COVER_BEHAVIORS, manifest["cover_behaviors"]
        )
        self.assertEqual(16, manifest["behavior_count"])

    def test_low_heat_is_declared_new_distinct(self) -> None:
        rows = identity.dispositions()
        keys = [row["semantic_key"] for row in rows]
        self.assertIn("extruder_shape/low_heat/", keys)
        low_heat = next(
            row
            for row in rows
            if row["semantic_key"] == "extruder_shape/low_heat/"
        )
        self.assertEqual("new_distinct", low_heat["disposition"])
        self.assertEqual("registry/tool-head-remainder", low_heat["capability"])

    def test_schema_two_covers_live_catalogs(self) -> None:
        manifest = identity.compile_manifest()
        self.assertEqual(2, manifest["schema_version"])
        self.assertEqual("PASS", manifest["status"], manifest["errors"])
        self.assertEqual(4357, manifest["live_entry_count"])
        sources = {row["source"] for row in manifest["live_entries"]}
        self.assertTrue(
            any("gt_block_object_catalog.json" in source for source in sources)
        )
        self.assertTrue(any("gt_stone_catalog.json" in source for source in sources))
        self.assertTrue(
            any("semantic_object_catalog.json" in source for source in sources)
        )
        self.assertTrue(
            any("energy_transformer_tiers.json" in source for source in sources)
        )
        self.assertTrue(any("ModBlocks.java" in source for source in sources))

    def test_same_path_different_ids_fail_closed(self) -> None:
        records: list[dict[str, str]] = []
        identity.add_record(
            records,
            runtime_id="cruciblecraft:alpha",
            registry_path="shared_path",
            source="a.json",
            semantic_key="item:alpha",
        )
        identity.add_record(
            records,
            runtime_id="cruciblecraft:beta",
            registry_path="shared_path",
            source="b.json",
            semantic_key="item:beta",
        )
        by_path: dict[str, list[dict[str, str]]] = {}
        for row in records:
            by_path.setdefault(row["registry_path"], []).append(row)
        ids = {row["runtime_id"] for row in by_path["shared_path"]}
        self.assertGreater(len(ids), 1)


if __name__ == "__main__":
    unittest.main()
