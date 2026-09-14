#!/usr/bin/env python3
"""Unified blocker ledger: catalog binds every blocked identity row."""
from __future__ import annotations

import copy
import unittest

from tools import blockers
from tools import capability_ledger
from tools import io_common as io


class BlockerLedgerTest(unittest.TestCase):
    def test_check_passes(self) -> None:
        self.assertEqual(0, blockers.main(["--check"]))

    def test_all_blocked_identity_rows_are_bound(self) -> None:
        errors = blockers.binding_errors()
        self.assertEqual([], errors)
        blocked = 0
        for path in capability_ledger.capability_files():
            capability = capability_ledger.load_capability(path)
            for row in capability.get("identity_disposition") or []:
                if row.get("disposition") == "blocked":
                    blocked += 1
        self.assertEqual(41, blocked)

    def test_do_not_add_open_amounts(self) -> None:
        ledger = blockers.compile_ledger()
        self.assertTrue(ledger["do_not_add"])
        self.assertNotIn("open_counts_by_unit", ledger)
        amounts = {row["id"]: row for row in ledger["open_amounts"]}
        self.assertEqual(869, amounts["recipe/loom-overflow"]["count"])
        self.assertEqual("rows", amounts["recipe/loom-overflow"]["unit"])
        self.assertEqual(49, amounts["recipe/fluidbed-overflow"]["count"])
        self.assertEqual(150, amounts["recipe/bath-remainder-families"]["count"])
        self.assertEqual("families", amounts["recipe/bath-remainder-families"]["unit"])
        self.assertNotIn("historical/petroleum-sampled-702", amounts)
        self.assertNotEqual(
            amounts["recipe/loom-overflow"]["count"]
            + amounts["recipe/fluidbed-overflow"]["count"],
            702,
        )

    def test_unbound_blocked_capability_fails_close_gate(self) -> None:
        capability = {
            "slug": "tests/fake-unbound",
            "identity_disposition": [
                {
                    "semantic_key": "recipe:fake:overflow",
                    "disposition": "blocked",
                    "runtime_ids": [],
                }
            ],
            "note": "",
        }
        errors = blockers.check_capability_bindings(capability)
        self.assertTrue(
            any("not on the blocker ledger" in message for message in errors),
            errors,
        )

    def test_bound_capability_passes_close_gate(self) -> None:
        path = (
            io.TOOLS
            / "capabilities"
            / "machines"
            / "loom"
            / "capability.json"
        )
        capability = capability_ledger.load_capability(path)
        self.assertEqual([], blockers.check_capability_bindings(capability))

    def test_obtain_marker_without_claim_fails(self) -> None:
        capability = {
            "slug": "tests/fake-obtain",
            "identity_disposition": [],
            "note": "Close at runtime_ready. Obtain stays explicitly_blocked.",
        }
        errors = blockers.check_capability_bindings(capability)
        self.assertTrue(
            any("Obtain stays explicitly_blocked" in message for message in errors),
            errors,
        )

    def test_catalog_copy_still_binds_real_loom_row(self) -> None:
        catalog = copy.deepcopy(blockers.load_catalog())
        catalog["entries"] = [
            row
            for row in catalog["entries"]
            if row["id"] != "recipe/loom-overflow"
        ]
        errors = blockers.binding_errors(
            catalog=catalog,
            capabilities=[
                capability_ledger.load_capability(
                    io.TOOLS / "capabilities" / "machines" / "loom" / "capability.json"
                )
            ],
            scope_slug="machines/loom",
        )
        self.assertTrue(
            any("recipe:loom:overflow" in message for message in errors),
            errors,
        )


if __name__ == "__main__":
    unittest.main()
