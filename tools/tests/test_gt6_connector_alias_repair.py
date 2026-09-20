#!/usr/bin/env python3
"""GT6 connector alias-repair child."""
from __future__ import annotations

import hashlib
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_connector_live_host_dummy_fold as live_host_fold
from tools import gt6_connector_alias_repair as runtime

SLUG = "content/gt6-connector-alias-repair"
ROOT = census.ROOT
WAVE = ROOT / "tools" / "waves" / "content" / "gt6-connector-alias-repair"
CAPABILITY = (
    ROOT
    / "tools"
    / "capabilities"
    / "content"
    / "gt6-connector-alias-repair"
    / "capability.json"
)
PLAN_ACTIVE = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "active"
    / "GT6连接件身份漏匹配详细计划.md"
)
PLAN_CLOSED = (
    ROOT
    / "docs"
    / "history"
    / "card-plans"
    / "closed"
    / "GT6连接件身份漏匹配详细计划.md"
)
R0 = runtime.R0
LEDGER = runtime.LEDGER
EXPECTED_LIVE = {
    26360: "hslasteel/tiny_fluid_pipe",
    26361: "hslasteel/small_fluid_pipe",
    26362: "hslasteel/fluid_pipe",
    26363: "hslasteel/large_fluid_pipe",
    26364: "hslasteel/huge_fluid_pipe",
    28250: "hslasteel/wire",
    28252: "hslasteel/triple_wire",
    28254: "hslasteel/quintuple_wire",
    28255: "hslasteel/sextuple_wire",
}


class Gt6ConnectorAliasRepairTest(unittest.TestCase):
    def test_capability_depends_on_live_connector_children(self) -> None:
        capability = census.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual(
            [
                "registry/catalog-modern-ids",
                "content/gt6-fluid-pipe-runtime",
                "content/gt6-eu-wire-cable-runtime",
                "content/gt6-connector-art",
            ],
            capability["depends_on"],
        )
        self.assertNotIn("content/gt6-pipe-cable-baseline", capability["depends_on"])
        self.assertEqual(["capability-runtime"], capability["profiles"])
        self.assertNotEqual("player_complete", capability["maturity"])
        self.assertEqual(
            [
                "hslaSteelFiveGaugePipesFoldOntoLiveHosts",
                "hslaSteelMappedWiresFoldOntoLiveHosts",
                "hslaSteelUngatedGaugesStayDummy",
            ],
            sorted(capability["required_test_ids"]),
        )
        compiled = ledger.compile_ledger()
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertTrue(PLAN_ACTIVE.is_file())
            self.assertFalse(PLAN_CLOSED.is_file())
        else:
            self.assertEqual("accepted", capability["workflow"])
            self.assertEqual("runtime_ready", capability["maturity"])
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertFalse(PLAN_ACTIVE.is_file())
            self.assertTrue(PLAN_CLOSED.is_file())

    def test_overlay_folds_only_gated_hsla_steel_hosts(self) -> None:
        overlay = census.load_json(WAVE / "alias_overlay.json")
        self.assertEqual(SLUG, overlay["capability_slug"])
        self.assertEqual(9, int(overlay["counts"]["fold_live_block"]))
        self.assertEqual(5, int(overlay["counts"]["fluid"]))
        self.assertEqual(4, int(overlay["counts"]["eu"]))
        by_meta = {int(row["meta"]): row for row in overlay["rows"]}
        self.assertEqual(set(EXPECTED_LIVE), set(by_meta))
        for meta, live in EXPECTED_LIVE.items():
            row = by_meta[meta]
            self.assertEqual("hsla_steel", row["recorded_material"])
            self.assertEqual("hslasteel", row["canonical_material"])
            self.assertEqual(f"cruciblecraft:{live}", row["live_block"])
            self.assertEqual("keep_distinct", row["baseline_disposition"])
        gap = census.load_json(WAVE / "current_gap.json")
        leftover = {int(row["meta"]) for row in gap["hsla_steel_still_dummy"]}
        self.assertIn(26365, leftover)
        self.assertIn(26366, leftover)
        self.assertIn(28256, leftover)
        loom = (
            ROOT
            / "src"
            / "recipe_generated"
            / "resources"
            / "data"
            / "cruciblecraft"
            / "recipe"
            / "loom"
            / "loom"
            / "gt_recipe_loom_0000.json"
        )
        text = loom.read_text(encoding="utf-8")
        self.assertIn("cruciblecraft:hslasteel/triple_wire", text)
        self.assertNotIn("cruciblecraft:electric_wire/3x_hsla_steel_wire", text)
        loom_source = (
            ROOT / "tools" / "waves" / "machines" / "loom" / "source.json"
        ).read_text(encoding="utf-8")
        self.assertIn("cruciblecraft:hslasteel/triple_wire", loom_source)
        self.assertNotIn(
            "cruciblecraft:electric_wire/3x_hsla_steel_wire", loom_source
        )
        self.assertEqual(
            hashlib.sha256(R0.read_bytes()).hexdigest(),
            (WAVE / "r0_disposition_sha256.txt").read_text(encoding="utf-8").strip(),
        )
        self.assertEqual(
            hashlib.sha256(LEDGER.read_bytes()).hexdigest(),
            (WAVE / "baseline_ledger_sha256.txt")
            .read_text(encoding="utf-8")
            .strip(),
        )
        ns = (
            ROOT
            / "src"
            / "main"
            / "resources"
            / "data"
            / "cruciblecraft_wave_content_gt6_connector_alias_repair"
        )
        self.assertTrue((ns / "structure" / "empty.nbt").is_file())
        self.assertTrue((ns / "gametest" / "structure" / "empty.nbt").is_file())

    def test_unique_active_docs_and_ledger_hand_off(self) -> None:
        topology = census.load_json(WAVE / "topology.json")
        readiness = census.load_json(WAVE / "readiness.json")
        compiled = ledger.compile_ledger()
        capability = census.load_json(CAPABILITY)
        self.assertEqual([], runtime.check())
        self.assertEqual([], live_host_fold.check())
        if capability["workflow"] == "active":
            self.assertEqual(SLUG, compiled["unique_active_slug"])
            self.assertEqual(SLUG, topology["unique_active_wave"])
            self.assertEqual(SLUG, readiness["unique_active_wave"])
        else:
            self.assertNotEqual(SLUG, compiled["unique_active_slug"])
            self.assertIsNone(topology["unique_active_wave"])
            self.assertIsNone(readiness["unique_active_wave"])


if __name__ == "__main__":
    unittest.main()
