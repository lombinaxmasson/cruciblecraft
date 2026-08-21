from __future__ import annotations

import json
import unittest

from tools import build_t29_publication_delta as builder


class T29PublicationDeltaTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_gt_delta_is_zero_when_controller_is_registered(self) -> None:
        document = builder.build()
        self.assertEqual("T29_PUBLICATION_DELTA", document["status"])
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, document["projected"])
        self.assertTrue(builder.BLOCK.is_file())
        self.assertTrue(builder.STRUCTURE.is_file())
        self.assertTrue(builder.RECIPE.is_file())
        measured = document["measured"]
        self.assertEqual("measured", measured["status"])
        self.assertEqual(0, measured["logical"])
        self.assertEqual(0, measured["eager"])
        self.assertEqual(0, measured["lazy"])
        self.assertTrue(document["same_sign_as_projection"])
        self.assertFalse(document["vanilla_crafting"]["gt_eager"])


if __name__ == "__main__":
    unittest.main()
