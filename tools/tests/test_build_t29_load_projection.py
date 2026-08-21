from __future__ import annotations

import json
import unittest

from tools import build_t29_load_projection as builder
from tools import t27_common as common


class T29LoadProjectionTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_gt_publication_delta_is_zero_and_not_vanilla(self) -> None:
        document = builder.build()
        self.assertEqual("T29_LOAD_PROJECTION", document["status"])
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, document["publication_delta"])
        self.assertEqual(27, document["structure_positions"])
        self.assertEqual(18766, document["opening_publication"]["logical"])
        self.assertEqual(16541, document["opening_publication"]["eager"])
        self.assertEqual(2225, document["opening_publication"]["lazy"])
        self.assertLess(
            document["t14_axes"]["eager_publication_rows"]["projected"],
            21000,
        )
        self.assertEqual(
            0,
            document["t14_axes"]["eager_publication_rows"]["delta"],
        )
        self.assertEqual("zero", document["t14_axes"]["eager_publication_rows"]["sign"])
        vanilla = document["vanilla_crafting"]
        self.assertEqual(1, vanilla["controller"])
        self.assertFalse(vanilla["gt_eager"])
        self.assertEqual(
            "cruciblecraft:thermal_steelmaking_host",
            document["plugin"]["id"],
        )
        self.assertNotIn(
            "cruciblecraft:processing_host",
            [document["plugin"]["id"]],
        )
        self.assertTrue(document["fidelity_layering"]["not_a_processing_host"])
        self.assertEqual(
            1,
            document["t14_axes"]["datapack_authored_entries"]["delta"],
        )

    def test_pending_axes_are_blocked_not_zero(self) -> None:
        document = builder.build()
        for name in builder.PENDING_AXES:
            row = document["t14_axes"][name]
            self.assertEqual("pending", row["status"])
            self.assertEqual(common.PENDING_LOAD_VERDICT, row["verdict"])
            self.assertIsNone(row["delta"])
            self.assertIsNone(row["projected"])
        self.assertEqual("pending", document["tests"]["junit"]["status"])
        self.assertEqual(10, document["tests"]["gametest"]["delta"])
        self.assertEqual(
            "cruciblecraft:large_crucible",
            document["registration"]["controller_id"],
        )
        self.assertTrue(document["registration"]["single_block_crucible_retained"])


if __name__ == "__main__":
    unittest.main()
