from __future__ import annotations

import unittest

from tools import build_t30_publication_delta as builder


class T30PublicationDeltaTest(unittest.TestCase):
    def test_committed_artifact_is_current_and_check_is_read_only(self) -> None:
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_gt_delta_is_zero_when_hopper_family_is_registered(self) -> None:
        document = builder.build()
        self.assertEqual("T30_PUBLICATION_DELTA", document["status"])
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, document["projected"])
        self.assertTrue(builder.HOPPER_BLOCK.is_file())
        self.assertTrue(builder.FUNNEL_BLOCK.is_file())
        self.assertEqual(121, document["vanilla_recipe_count"])
        measured = document["measured"]
        self.assertEqual("measured", measured["status"])
        self.assertEqual(0, measured["logical"])
        self.assertEqual(0, measured["eager"])
        self.assertEqual(0, measured["lazy"])
        self.assertTrue(document["same_sign_as_projection"])
        self.assertFalse(document["vanilla_crafting"]["gt_eager"])
        self.assertEqual(121, document["vanilla_crafting"]["total"])


if __name__ == "__main__":
    unittest.main()
