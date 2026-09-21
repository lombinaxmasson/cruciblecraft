#!/usr/bin/env python3
"""Large Electric Oven 17106 landing checks. Does not claim unique-active."""
from __future__ import annotations

import unittest

from tools import gt6_large_oven as oven


class LargeOvenLandingTest(unittest.TestCase):
    def test_source_check_passes(self) -> None:
        self.assertEqual([], oven.check())
