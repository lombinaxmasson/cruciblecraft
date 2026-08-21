from __future__ import annotations

import json
import unittest

from tools import build_t30_load_projection as builder
from tools import t27_common as common


class T30LoadProjectionTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_gt_publication_delta_is_zero_and_vanilla_is_bounded(self) -> None:
        document = builder.build()
        self.assertEqual("T30_LOAD_PROJECTION", document["status"])
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, document["publication_delta"])
        self.assertEqual(18766, document["opening_publication"]["logical"])
        self.assertEqual(16541, document["opening_publication"]["eager"])
        self.assertEqual(2225, document["opening_publication"]["lazy"])
        self.assertEqual(
            0,
            document["t14_axes"]["eager_publication_rows"]["delta"],
        )
        self.assertEqual("zero", document["t14_axes"]["eager_publication_rows"]["sign"])
        self.assertLess(
            document["t14_axes"]["eager_publication_rows"]["projected"],
            21000,
        )
        datapack = document["t14_axes"]["datapack_authored_entries"]
        self.assertEqual(121 + 121 + 1, datapack["delta"])
        self.assertLess(datapack["projected"], 6600)
        self.assertEqual(121, document["registration"]["blocks"])
        self.assertEqual(121, document["registration"]["items"])
        self.assertEqual(60, document["registration"]["hopper"])
        self.assertEqual(60, document["registration"]["queue_hopper"])
        self.assertEqual(1, document["registration"]["dust_funnel"])
        self.assertEqual(2, document["registration"]["block_entity_types"])
        self.assertEqual(1, document["registration"]["menu_types"])
        self.assertEqual(0, document["resources"]["per_material_texture"])
        self.assertFalse(document["vanilla_crafting"]["gt_eager"])
        self.assertEqual(121, document["vanilla_crafting"]["total"])

    def test_pending_axes_and_test_ranges_are_not_fake_zeros(self) -> None:
        document = builder.build()
        for name in builder.PENDING_AXES:
            row = document["t14_axes"][name]
            self.assertEqual("pending", row["status"])
            self.assertEqual(common.PENDING_LOAD_VERDICT, row["verdict"])
            self.assertIsNone(row["delta"])
            self.assertIsNone(row["projected"])
        self.assertEqual("pending", document["tests"]["junit"]["status"])
        self.assertIsNone(document["tests"]["junit"]["delta"])
        self.assertEqual("pending", document["tests"]["python"]["status"])
        self.assertIsNone(document["tests"]["python"]["delta"])
        gametest = document["tests"]["gametest"]
        self.assertEqual("projected_range", gametest["status"])
        self.assertEqual(131, gametest["base"])
        self.assertEqual(30, gametest["delta_min"])
        self.assertGreaterEqual(gametest["delta_max"], gametest["delta_min"])
        self.assertIsNone(gametest["delta"])


if __name__ == "__main__":
    unittest.main()
