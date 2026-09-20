#!/usr/bin/env python3
"""Leftover connector CatalogNamedItem dummies fold onto live hosts."""
from __future__ import annotations

import hashlib
import unittest

from tools import capability_ledger as ledger
from tools import census_common as census
from tools import gt6_connector_live_host_dummy_fold as runtime

SLUG = "content/gt6-connector-live-host-dummy-fold"
WAVE = census.TOOLS / "waves" / "content" / "gt6-connector-live-host-dummy-fold"
EXPECTED_GOLD_LEAD = {
    "electric_wire/3x_gold_wire",
    "electric_wire/5x_gold_wire",
    "electric_wire/6x_gold_wire",
    "electric_wire/3x_lead_wire",
    "electric_wire/5x_lead_wire",
    "electric_wire/6x_lead_wire",
}


class Gt6ConnectorLiveHostDummyFoldTest(unittest.TestCase):
    def test_live_host_duals_are_withdrawn(self) -> None:
        self.assertEqual([], runtime.check())
        overlay = census.load_json(WAVE / "fold_overlay.json")
        self.assertEqual(SLUG, overlay["capability_slug"])
        self.assertEqual(runtime.EXPECTED_FOLDS, overlay["counts"]["fold_live_block"])
        self.assertEqual(runtime.EXPECTED_KEEP, overlay["counts"]["keep_distinct"])
        folded = {str(row["dummy_path"]) for row in overlay["rows"]}
        self.assertTrue(EXPECTED_GOLD_LEAD <= folded)
        keep = {str(row["dummy_path"]) for row in overlay["keep_distinct"]}
        self.assertIn("untyped/loot_crate", keep)
        self.assertEqual(48, sum(1 for path in keep if path.startswith("panel/")))
        compiled = ledger.compile_ledger()
        self.assertNotEqual(SLUG, compiled["unique_active_slug"])
        self.assertEqual(
            hashlib.sha256(runtime.R0.read_bytes()).hexdigest(),
            (WAVE / "r0_disposition_sha256.txt").read_text(encoding="utf-8").strip(),
        )
        self.assertEqual(
            hashlib.sha256(runtime.LEDGER.read_bytes()).hexdigest(),
            (WAVE / "baseline_ledger_sha256.txt").read_text(encoding="utf-8").strip(),
        )
