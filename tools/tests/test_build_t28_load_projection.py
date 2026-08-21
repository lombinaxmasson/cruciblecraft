from __future__ import annotations

import json
import unittest

from tools import build_t28_load_projection as builder
from tools import t27_common as common


class T28LoadProjectionTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_publication_delta_is_negative_and_derived_from_expansion(self) -> None:
        document = builder.build()
        expansion = document["cooling_expansion"]["count"]
        t10 = common.load_json(builder.T10_PREFLIGHT)
        materials = t10["route_projections"]["hot_ingot"]["materials"]
        report = common.load_json(builder.REPORT)
        published = report["rules"]["expanded_recipes_per_map"]["cruciblecraft:cooling"]
        self.assertEqual(len(materials), expansion)
        if document["cooling_expansion"]["rule_present"]:
            self.assertEqual(published, expansion)
        self.assertGreater(expansion, 0)
        self.assertEqual(-expansion, document["publication_delta"]["logical"])
        self.assertEqual(-expansion, document["publication_delta"]["eager"])
        self.assertEqual(0, document["publication_delta"]["lazy"])
        logical = document["t14_axes"]["logical_publication_rows"]
        eager = document["t14_axes"]["eager_publication_rows"]
        self.assertEqual("projected", logical["status"])
        self.assertEqual("negative", logical["sign"])
        self.assertEqual(
            document["opening_publication"]["logical"] - expansion,
            logical["projected"],
        )
        self.assertLess(eager["projected"], 21000)
        self.assertEqual(
            "tools/t10_preflight_projection.json#route_projections.hot_ingot.materials",
            document["cooling_expansion"]["authority"],
        )

    def test_pending_axes_are_blocked_not_zero(self) -> None:
        document = builder.build()
        for name in builder.PENDING_AXES:
            row = document["t14_axes"][name]
            self.assertEqual("pending", row["status"])
            self.assertEqual(common.PENDING_LOAD_VERDICT, row["verdict"])
            self.assertIsNone(row["delta"])
            self.assertIsNone(row["projected"])
        self.assertFalse(document["freezer_in_card_scope"])
        self.assertEqual("delete", document["retirement"]["cooling_rule"])
        self.assertFalse(document["cooling_expansion"]["rule_present"])
        self.assertEqual("retain_empty", document["retirement"]["cooling_map"])
        self.assertEqual(
            "delete_class_and_call",
            document["retirement"]["cool_if_ready"],
        )
        self.assertEqual("pending", document["tests"]["junit"]["status"])
        self.assertEqual("pending", document["tests"]["gametest"]["status"])
