"""Contract tests for T40 player-path support (empty lock)."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_player_path_support as support  # noqa: E402
from tools import t40_common as t40  # noqa: E402


class T40PlayerPathSupportTests(unittest.TestCase):
    def test_locked_production_needs_zero_support(self) -> None:
        document, files = support.build()
        self.assertEqual([], document["routes"])
        self.assertEqual(0, document["discovery"]["accepted_routes"])
        self.assertEqual([], document["discovery"]["remaining_unmapped"])
        self.assertEqual({}, files)
        self.assertEqual(13, document["discovery"]["family_atomic_closed"]["families"])
        self.assertEqual(22, document["discovery"]["family_atomic_closed"]["relations"])
        ledger = t40.support_recipe_ledger()
        self.assertEqual(0, ledger["authored"])
        self.assertEqual(0, t40.load_production_lock()["support"]["route_count"])
