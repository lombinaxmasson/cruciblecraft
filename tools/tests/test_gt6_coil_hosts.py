#!/usr/bin/env python3
"""GT6 coil/host landing checks. Does not claim unique-active."""
from __future__ import annotations

import unittest

from tools import capability_ledger as ledger
from tools import gt6_coil_hosts as coils
from tools import io_common as io

SLUG = "machines/gt6-coil-hosts"
CAPABILITY = (
    io.ROOT
    / "tools"
    / "capabilities"
    / "machines"
    / "gt6-coil-hosts"
    / "capability.json"
)


class CoilHostLandingTest(unittest.TestCase):
    def test_source_check_passes(self) -> None:
        self.assertEqual([], coils.check())

    def test_capability_is_accepted_not_unique_active(self) -> None:
        compiled = ledger.compile_ledger()
        capability = io.load_json(CAPABILITY)
        self.assertEqual(SLUG, capability["slug"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertNotEqual(SLUG, compiled["unique_active_slug"])
        self.assertIsNone(ledger.load_card_plan_index()["active"].get(SLUG))
