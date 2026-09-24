#!/usr/bin/env python3
"""GT6 MTE dummy identities become in-place live BlockItems."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_mte_inplace_runtime as runtime

ROOT = census.ROOT
ATTACHMENTS = "content/gt6-mte-fluid-attachments-runtime"


def _capability(slug: str) -> dict:
    path = ROOT / "tools" / "capabilities" / slug / "capability.json"
    if not path.is_file():
        return {}
    return census.load_json(path)


def _plan_paths(domain: str) -> tuple[object, object]:
    stem = runtime.DOMAINS[domain]["plan_stem"]
    return (
        ROOT / "docs" / "history" / "card-plans" / "active" / stem,
        ROOT / "docs" / "history" / "card-plans" / "closed" / stem,
    )


class Gt6MteInplaceRuntimeTest(unittest.TestCase):
    def test_attachments_capability_is_runtime_not_fold(self) -> None:
        capability = _capability(ATTACHMENTS)
        if not capability:
            self.skipTest("attachments capability not issued")
        self.assertEqual(ATTACHMENTS, capability["slug"])
        self.assertEqual(
            runtime.DOMAINS["attachments"]["depends_on"],
            capability["depends_on"],
        )
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            sorted(runtime.DOMAINS["attachments"]["tests"]),
            sorted(capability["required_test_ids"]),
        )
        compiled = ledger.compile_ledger()
        active, closed = _plan_paths("attachments")
        if capability["workflow"] == "active":
            self.assertEqual(ATTACHMENTS, compiled["unique_active_slug"])
            self.assertTrue(active.is_file())
            self.assertFalse(closed.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertEqual("runtime_ready", capability["maturity"])
            self.assertNotEqual(ATTACHMENTS, compiled["unique_active_slug"])
            self.assertFalse(active.is_file())
            self.assertTrue(closed.is_file())

    def test_attachments_overlay_has_forty_six_live_hosts(self) -> None:
        wave = runtime._wave("attachments")
        overlay_path = wave / "runtime_overlay.json"
        if not overlay_path.is_file():
            self.skipTest("attachments overlay not written")
        overlay = census.load_json(overlay_path)
        self.assertEqual(ATTACHMENTS, overlay["capability_slug"])
        self.assertEqual(47, int(overlay["counts"]["in_place"]))
        by_meta = {int(row["meta"]): row for row in overlay["rows"]}
        self.assertEqual("FAUCET", by_meta[1700]["kind"])
        self.assertEqual(
            "fluid_attachment/crucible_faucet_stone",
            by_meta[1700]["dummy_path"],
        )
        self.assertEqual("TAP", by_meta[32730]["kind"])
        kinds = {row["kind"] for row in overlay["rows"]}
        self.assertEqual(
            {"FAUCET", "TAP", "FUNNEL", "NOZZLE", "CAP_NOZZLE"},
            kinds,
        )

    def test_attachments_art_is_local_gt6_not_cover_alias(self) -> None:
        wave = runtime._wave("attachments")
        manifest_path = wave / "art_manifest.json"
        if not manifest_path.is_file():
            self.skipTest("attachments art manifest not written")
        manifest = census.load_json(manifest_path)
        self.assertGreaterEqual(len(manifest.get("rows") or []), 27)
        for row in manifest["rows"]:
            self.assertEqual(
                "gt6_referencable_port_code/gregtech6_w",
                row["source"],
            )
            prefix = (
                "assets/cruciblecraft/textures/item/gt6_import/"
                if row.get("kind") == "RAW_CERAMIC_ATTACHMENT"
                else "assets/cruciblecraft/textures/block/gt6_import/mte/"
            )
            self.assertTrue(str(row["destination"]).startswith(prefix))
            dest = ROOT / "src" / "main" / "resources" / row["destination"]
            self.assertTrue(dest.is_file(), dest)
            source = runtime.GT6_W / row["gt6_source"]
            self.assertTrue(source.is_file(), source)
            self.assertEqual(source.read_bytes(), dest.read_bytes())

    def test_funnel_model_matches_gt6_render_pass_bounds(self) -> None:
        model = census.load_json(
            ROOT
            / "src"
            / "main"
            / "resources"
            / "assets"
            / "cruciblecraft"
            / "models"
            / "block"
            / "mte_inplace_funnel.json"
        )
        self.assertNotEqual("minecraft:block/cube_all", model.get("parent"))
        self.assertEqual(
            [
                {"from": [5, 9, 0], "to": [11, 10, 6]},
                {"from": [6, 8, 0], "to": [10, 9, 4]},
                {"from": [7, 7, 0], "to": [9, 9, 2]},
            ],
            [
                {
                    "from": element["from"],
                    "to": element["to"],
                }
                for element in model["elements"][:3]
            ],
        )
        self.assertIn("colored_side", model["textures"])
        self.assertIn("overlay_side", model["textures"])

    def test_faucet_model_matches_gt6_render_pass_bounds(self) -> None:
        model = census.load_json(
            ROOT
            / "src/main/resources/assets/cruciblecraft/models/block/"
            "mte_inplace_faucet.json"
        )
        self.assertEqual(
            [
                {"from": [6, 1, 12], "to": [10, 2, 16]},
                {"from": [5, 2, 12], "to": [6, 6, 16]},
                {"from": [10, 2, 12], "to": [11, 6, 16]},
            ],
            [
                {
                    "from": element["from"],
                    "to": element["to"],
                }
                for element in model["elements"]
            ],
        )
        self.assertEqual(
            "#body",
            model["textures"]["particle"],
        )

    def test_r0_and_baseline_ledgers_are_untouched(self) -> None:
        wave = runtime._wave("attachments")
        r0_hash = wave / "r0_disposition_sha256.txt"
        if not r0_hash.is_file():
            self.skipTest("attachments wave hashes not written")
        self.assertEqual(
            r0_hash.read_text(encoding="utf-8").strip(),
            runtime._sha256(runtime.R0),
        )
        self.assertEqual(
            (wave / "baseline_ledger_sha256.txt").read_text(encoding="utf-8").strip(),
            runtime._sha256(runtime.LEDGER),
        )

    def test_extender_overlay_has_two_live_hosts(self) -> None:
        wave = runtime._wave("extender")
        overlay_path = wave / "runtime_overlay.json"
        if not overlay_path.is_file():
            self.skipTest("extender overlay not written")
        overlay = census.load_json(overlay_path)
        self.assertEqual("content/gt6-mte-extender-runtime", overlay["capability_slug"])
        self.assertEqual(2, int(overlay["counts"]["in_place"]))
        kinds = {row["kind"] for row in overlay["rows"]}
        self.assertEqual({"TANK_EXTENDER", "TANK_BRIDGE"}, kinds)

    def test_issued_overlays_match_domain_expected_counts(self) -> None:
        for domain, spec in runtime.DOMAINS.items():
            overlay_path = runtime._wave(domain) / "runtime_overlay.json"
            if not overlay_path.is_file():
                continue
            overlay = census.load_json(overlay_path)
            with self.subTest(domain=domain):
                self.assertEqual(spec["slug"], overlay["capability_slug"])
                self.assertEqual(
                    spec["expected"],
                    int(overlay["counts"]["in_place"]),
                )

    def test_shared_module_bound_stays_single(self) -> None:
        self.assertLessEqual(len(runtime.DOMAINS), 20)
        self.assertIn("attachments", runtime.DOMAINS)
        self.assertIn("extender", runtime.DOMAINS)
        self.assertIn("drive", runtime.DOMAINS)


if __name__ == "__main__":
    unittest.main()
