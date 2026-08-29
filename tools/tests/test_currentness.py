"""Currentness sidecar tests. Giant T35 bodies are not rewritten."""
from __future__ import annotations

import unittest

from tools import currentness
from tools import t35_common as t35


class CurrentnessTest(unittest.TestCase):
    def test_sidecar_targets_cover_t35_t39_t40_closeout(self) -> None:
        artifacts = {row["artifact"] for row in currentness.TARGETS}
        self.assertIn("tools/t35_census.json", artifacts)
        self.assertIn("tools/t40_readiness.json", artifacts)
        self.assertIn("tools/t43_readiness.json", artifacts)
        self.assertIn("tools/t36_repair_readiness.json", artifacts)
        self.assertIn("tools/t43_census_delta.json", artifacts)
        self.assertIn("tools/t43_card_topology.json", artifacts)
        self.assertIn(
            "src/main/resources/data/cruciblecraft/material_registration_gate.json",
            artifacts,
        )
        self.assertTrue(currentness.SCHEMA.is_file())

    def test_write_sidecar_does_not_touch_giant_body(self) -> None:
        artifact = t35.CENSUS
        if not artifact.is_file():
            self.skipTest("t35_census.json missing")
        before = artifact.stat().st_mtime_ns
        sidecar = currentness.write_sidecar(artifact)
        after = artifact.stat().st_mtime_ns
        self.assertEqual(before, after)
        self.assertTrue(sidecar["giant_body"])
        self.assertTrue((t35.ROOT / "tools/t35_census.currentness.json").is_file())

    def test_hash_only_rebind_keeps_semantic_root(self) -> None:
        artifact = t35.READINESS
        if not artifact.is_file():
            self.skipTest("t35_readiness.json missing")
        currentness.write_sidecar(artifact)
        before = currentness.load_sidecar(currentness.sidecar_path(artifact))
        rebound = currentness.rebind_sidecar(artifact)
        self.assertEqual(before["semantic_root_sha256"], rebound["semantic_root_sha256"])
        self.assertEqual(
            before["semantic_root_sha256"],
            currentness.semantic_root_sha256(t35.load_json(artifact)),
        )

    def test_hash_only_drift_passes_when_sidecar_is_current(self) -> None:
        artifact = t35.READINESS
        if not artifact.is_file():
            self.skipTest("t35_readiness.json missing")
        currentness.rebind_sidecar(artifact)
        expected = t35.load_json(artifact)
        expected["generated_by"] = "hash-only-probe"
        errors = t35.check_generated_document(artifact, expected)
        self.assertEqual([], errors)


if __name__ == "__main__":
    unittest.main()
