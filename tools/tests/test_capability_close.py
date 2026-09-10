#!/usr/bin/env python3
"""Atomic capability close refuses when unique-active is empty."""
from __future__ import annotations

import unittest

from tools import capability_ledger
from tools import close_capability
from tools import io_common as io


class CapabilityCloseTest(unittest.TestCase):
    def test_close_requires_the_unique_active_capability(self) -> None:
        compiled = capability_ledger.compile_ledger()
        self.assertIsNone(compiled["unique_active_slug"])
        with self.assertRaisesRegex(ValueError, "unique-active is None"):
            close_capability.close_capability("machines/cluster-mill")

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


if __name__ == "__main__":
    unittest.main()
