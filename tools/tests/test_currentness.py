"""Currentness sidecar tests. Giant historical bodies are not rewritten."""
from __future__ import annotations

import unittest

from tools import currentness
from tools import census_common as census


class CurrentnessTest(unittest.TestCase):
    def test_sidecar_targets_cover_census_and_card_closeout(self) -> None:
        artifacts = {row["artifact"] for row in currentness.TARGETS}
        self.assertIn("tools/census.json", artifacts)
        self.assertIn("tools/electrolyzer_readiness.json", artifacts)
        self.assertIn("tools/smelter_stone_readiness.json", artifacts)
        self.assertIn("tools/repair_readiness.json", artifacts)
        self.assertIn("tools/smelter_stone_census_delta.json", artifacts)
        self.assertIn("tools/smelter_stone_card_topology.json", artifacts)
        self.assertIn(
            "src/main/resources/data/cruciblecraft/material_registration_gate.json",
            artifacts,
        )
        self.assertTrue(currentness.SCHEMA.is_file())

    def test_write_sidecar_does_not_touch_giant_body(self) -> None:
        artifact = census.TOOLS / "census.json"
        if not artifact.is_file():
            self.skipTest("census.json missing")
        before = artifact.stat().st_mtime_ns
        sidecar = currentness.write_sidecar(artifact)
        after = artifact.stat().st_mtime_ns
        self.assertEqual(before, after)
        self.assertTrue(sidecar["giant_body"])
        self.assertTrue((census.ROOT / "tools/census.currentness.json").is_file())

    def test_hash_only_rebind_keeps_semantic_root(self) -> None:
        artifact = census.TOOLS / "census_readiness.json"
        if not artifact.is_file():
            self.skipTest("census_readiness.json missing")
        currentness.write_sidecar(artifact)
        before = currentness.load_sidecar(currentness.sidecar_path(artifact))
        rebound = currentness.rebind_sidecar(artifact)
        self.assertEqual(before["semantic_root_sha256"], rebound["semantic_root_sha256"])
        self.assertEqual(
            before["semantic_root_sha256"],
            currentness.semantic_root_sha256(census.load_json(artifact)),
        )

    def test_hash_only_drift_passes_when_sidecar_is_current(self) -> None:
        artifact = census.TOOLS / "census_readiness.json"
        if not artifact.is_file():
            self.skipTest("census_readiness.json missing")
        currentness.rebind_sidecar(artifact)
        expected = census.load_json(artifact)
        expected["generated_by"] = "hash-only-probe"
        errors = census.check_generated_document(artifact, expected)
        self.assertEqual([], errors)


if __name__ == "__main__":
    unittest.main()
