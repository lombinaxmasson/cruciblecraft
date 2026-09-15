#!/usr/bin/env python3
"""Atomic capability close refuses a slug that is not unique-active."""
from __future__ import annotations

import unittest

from tools import blockers
from tools import capability_ledger
from tools import close_capability
from tools import io_common as io


class CapabilityCloseTest(unittest.TestCase):
    def test_close_requires_the_unique_active_capability(self) -> None:
        compiled = capability_ledger.compile_ledger()
        self.assertNotEqual("worldgen/gt-trees", compiled["unique_active_slug"])
        with self.assertRaisesRegex(ValueError, "unique-active is"):
            close_capability.close_capability("worldgen/gt-trees")

    def test_path_map_records_prep_and_active_for_roll_former(self) -> None:
        document = io.load_json(close_capability.PATH_MAP)
        moves = document["moves"]
        closed = "docs/history/card-plans/closed/辊压成型机详细计划.md"
        self.assertEqual(
            closed,
            moves["docs/history/card-plans/active/辊压成型机详细计划.md"],
        )
        self.assertEqual(
            closed,
            moves["docs/history/card-plans/prep/辊压成型机详细计划.md"],
        )

    def test_unbound_blocked_row_fails_blocker_bindings(self) -> None:
        errors = blockers.check_capability_bindings(
            {
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
        )
        self.assertTrue(
            any("not on the blocker ledger" in message for message in errors),
            errors,
        )

    def test_lock_note_may_say_not_player_complete(self) -> None:
        self.assertFalse(
            close_capability._lock_claims_player_complete(
                "live compile; not player_complete"
            )
        )
        self.assertTrue(
            close_capability._lock_claims_player_complete(
                "status is player_complete"
            )
        )

    def test_close_rejects_player_complete_maturity(self) -> None:
        with self.assertRaisesRegex(ValueError, "unsupported close maturity"):
            close_capability.close_capability(
                "logistics/fluid-network/basic-transfer",
                maturity="player_complete",
            )

    def test_close_defaults_to_runtime_ready_major(self) -> None:
        import inspect

        defaults = inspect.signature(close_capability.close_capability).parameters
        self.assertEqual("runtime_ready", defaults["maturity"].default)
        self.assertEqual("major", defaults["change_class"].default)


if __name__ == "__main__":
    unittest.main()
