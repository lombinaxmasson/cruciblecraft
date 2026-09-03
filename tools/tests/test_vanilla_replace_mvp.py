#!/usr/bin/env python3
"""Vanilla replace MVP lock and closeout."""
from __future__ import annotations

import unittest

from tools import t35_common as t35
from tools import vanilla_replace_mvp as mvp

SLUG = "content/vanilla-replace-mvp"
VANILLA_PATH = "src/main/java/gregtech/loaders/c/Loader_Recipes_Vanilla.java"


class VanillaReplaceMvpLockTest(unittest.TestCase):
    def test_lock_when_present(self) -> None:
        if not mvp.LOCK_PATH.is_file():
            self.skipTest("MVP lock not written yet")
        lock = t35.load_json(mvp.LOCK_PATH)
        self.assertEqual(SLUG, lock["wave_slug"])
        self.assertEqual("VANILLA_REPLACE_MVP_READY", lock["status"])
        self.assertEqual(0, int(lock["owns_families"]))
        self.assertEqual(0, int(lock["generated_recipe_count"]))
        self.assertIsNone(lock["production_lock"])
        self.assertFalse(lock["nuclear_started"])
        self.assertEqual(
            "4c459acd2c7729d4186c5ada9ccd76181745bacf",
            lock["source_blob_sha1"],
        )
        self.assertEqual(VANILLA_PATH, lock["source_file"])
        self.assertEqual([], lock["removed"])
        self.assertEqual([], lock["added"])
        substituted = lock["substituted"]
        self.assertEqual(1, len(substituted))
        paper = substituted[0]
        self.assertEqual("minecraft:paper", paper["recipe_id"])
        self.assertEqual("substitute", paper["action"])
        self.assertEqual([43, 44, 52], paper["gt6_pointer"]["lines"])
        self.assertEqual(1, int(paper["io"]["result"]["count"]))
        deferred_text = t35.stable_json(lock["deferred"])
        self.assertIn("minecraft:furnace", deferred_text)
        self.assertIn("itemGrassDry", deferred_text)
        self.assertIn("minecraft:bone_meal", deferred_text)
        equivalent = t35.stable_json(lock["no_1_21_equivalent"])
        self.assertIn("rem_smelting", equivalent)
        self.assertIn("WiMo_Thick_Bone", equivalent)

    def test_check_artifacts_pass(self) -> None:
        if not mvp.LOCK_PATH.is_file():
            self.skipTest("MVP lock not written yet")
        self.assertEqual([], mvp.check_lock())
        if not mvp.READINESS_PATH.is_file():
            self.skipTest("MVP readiness not written yet")
        self.assertEqual([], mvp.check_artifacts())


class VanillaReplaceMvpReadinessTest(unittest.TestCase):
    def test_readiness_when_present(self) -> None:
        if not mvp.READINESS_PATH.is_file():
            self.skipTest("MVP readiness not written yet")
        readiness = t35.load_json(mvp.READINESS_PATH)
        self.assertEqual("VANILLA_REPLACE_MVP_READY", readiness["status"])
        self.assertIsNone(readiness["unique_active_wave"])
        self.assertTrue(readiness["next_unassigned"])
        self.assertEqual(0, int(readiness["owns_families"]))
        self.assertEqual(1, int(readiness["substituted_count"]))


if __name__ == "__main__":
    unittest.main()
