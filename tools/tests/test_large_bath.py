#!/usr/bin/env python3
"""Large Bathing Vat 17104 source checks. Does not claim unique-active."""
from __future__ import annotations

import unittest

from tools import gt6_large_bath as bath


class LargeBathSourceTest(unittest.TestCase):
    def test_source_check_passes(self) -> None:
        self.assertEqual([], bath.check())
