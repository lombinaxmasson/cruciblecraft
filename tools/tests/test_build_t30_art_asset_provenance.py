from __future__ import annotations

import json
import unittest

from tools import build_t30_art_asset_provenance as builder
from tools import t27_common as common


class T30ArtAssetProvenanceTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_art_derived_shared_parents_project_one_hundred_twenty_one(self) -> None:
        document = builder.build()
        self.assertEqual("T30_ART_ASSET_PROVENANCE", document["status"])
        self.assertEqual("ART_DERIVED", document["classification"])
        self.assertEqual(common.SOURCE_REVISION, document["source_revision"])
        self.assertEqual(121, len(document["projected_members"]))
        self.assertEqual(121, len(set(document["projected_members"])))
        self.assertEqual(60, document["counts"]["hopper"])
        self.assertEqual(60, document["counts"]["queue_hopper"])
        self.assertEqual(1, document["counts"]["dust_funnel"])
        self.assertEqual(0, document["counts"]["per_material_pngs"])
        self.assertIn("cruciblecraft:lead_hopper", document["projected_members"])
        self.assertIn(
            "cruciblecraft:lead_queue_hopper", document["projected_members"]
        )
        self.assertIn(
            "cruciblecraft:steel_dust_funnel", document["projected_members"]
        )
        self.assertNotIn("cruciblecraft:hopper", document["projected_members"])
        self.assertNotIn(
            "cruciblecraft:queue_hopper", document["projected_members"]
        )
        families = {row["family"]: row for row in document["families"]}
        self.assertEqual(
            "cruciblecraft:block/hopper", families["hopper"]["shared_parent"]
        )
        self.assertEqual(
            "cruciblecraft:block/material/block_overlay",
            families["queue_hopper"]["overlay_texture"],
        )
        self.assertIsNone(families["hopper"]["overlay_texture"])
        self.assertEqual(0, families["dust_funnel"]["tintindex"])
