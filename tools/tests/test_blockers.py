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
        self.assertEqual(43, blocked)

    def test_do_not_add_open_amounts(self) -> None:
        ledger = blockers.compile_ledger()
        self.assertTrue(ledger["do_not_add"])
        self.assertNotIn("open_counts_by_unit", ledger)
        amounts = {row["id"]: row for row in ledger["open_amounts"]}
        self.assertEqual(869, amounts["recipe/loom-overflow"]["count"])
        self.assertEqual("rows", amounts["recipe/loom-overflow"]["unit"])
        self.assertEqual("scale_not_todo", amounts["recipe/loom-overflow"]["planning_bucket"])
        self.assertNotIn("recipe/fluidbed-overflow", amounts)
        self.assertNotIn("recipe/bath-remainder-families", amounts)
        self.assertNotIn("recipe/bath-identity-families", amounts)
        self.assertNotIn("historical/petroleum-sampled-702", amounts)
        self.assertNotIn("energy/reactor-fusion", amounts)
        self.assertNotEqual(
            amounts["recipe/loom-overflow"]["count"],
            702,
        )

    def test_planning_buckets_classify_inventory(self) -> None:
        catalog = blockers.load_catalog()
        by_id = {str(row["id"]): row for row in catalog["entries"]}
        for row in catalog["entries"]:
            self.assertIn(row["planning_bucket"], blockers.PLANNING_BUCKETS)
            if row["status"] not in blockers.OPEN_STATUSES:
                self.assertEqual("not_work", row["planning_bucket"], row["id"])
        fusion = by_id["energy/reactor-fusion"]
        self.assertIsNone(fusion["count"])
        self.assertEqual("not_work", fusion["planning_bucket"])
        self.assertEqual("scale_not_todo", by_id["recipe/loom-overflow"]["planning_bucket"])
        self.assertEqual("schedulable", by_id["recipe/laminator-overflow"]["planning_bucket"])
        self.assertEqual("audit_first", by_id["tools/world-behaviors"]["planning_bucket"])
        self.assertEqual(
            "not_work",
            by_id["energy/reactor-temperature-kelvin"]["planning_bucket"],
        )
        ledger = blockers.compile_ledger()
        self.assertIsNone(by_id["energy/reactor-fusion"]["count"])
        planning = {row["id"] for row in ledger["planning"]["audit_first"]}
        self.assertNotIn("energy/reactor-fusion", planning)
        self.assertIn("tools/world-behaviors", planning)
        markdown = blockers.render_markdown(ledger)
        self.assertIn("排期分类", markdown)
        self.assertIn("A. 数字是规模，不是待办", markdown)
        self.assertIn("B. 分母已冻，可当卡排", markdown)
        self.assertIn("C. 有名字，分母未冻成工作量", markdown)
        self.assertIn("D. 不是活", markdown)

    def test_batch_index_keeps_scopes_and_boundaries_separate(self) -> None:
        self.assertEqual([], blockers.batch_errors())
        batches = blockers.load_batches()
        by_id = {row["id"]: row for row in batches["batches"]}
        self.assertNotIn("batch/recipe-bath-wave", by_id)
        catalog = {
            row["id"]: row for row in blockers.load_catalog()["entries"]
        }
        self.assertEqual("resolved", catalog["recipe/bath-remainder-families"]["status"])
        self.assertEqual("resolved", catalog["recipe/bath-identity-families"]["status"])
        self.assertEqual(
            "T49详细计划",
            catalog["recipe/bath-remainder-families"]["resolved_by"],
        )
        for batch_id in (
            "batch/machines-injector-host-closure",
            "batch/machines-nanofab-host-closure",
        ):
            scale_members = [
                row
                for row in by_id[batch_id]["members"]
                if row["role"] == "scale_context"
            ]
            self.assertEqual(1, len(scale_members))
            blocker = next(
                row
                for row in blockers.load_catalog()["entries"]
                if row["id"] == scale_members[0]["blocker_id"]
            )
            self.assertEqual("scale_not_todo", blocker["planning_bucket"])
        self.assertTrue(batches["ordering_is_not_additive"])

    def test_recipe_ledger_rejects_stale_open_bath_projection(self) -> None:
        catalog = copy.deepcopy(blockers.load_catalog())
        by_id = {row["id"]: row for row in catalog["entries"]}
        by_id["recipe/bath-remainder-families"].update(
            {
                "planning_bucket": "schedulable",
                "resolved_by": None,
                "status": "open",
            }
        )
        errors = blockers.recipe_ledger_errors(catalog)
        self.assertTrue(
            any(
                "recipe/bath-remainder-families: catalog is open but current "
                "recipe ledger is closed" in message
                for message in errors
            ),
            errors,
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
