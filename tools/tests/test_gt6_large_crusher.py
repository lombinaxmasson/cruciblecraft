#!/usr/bin/env python3
"""GT6 Large Crusher 17108 source checks."""
from __future__ import annotations

import unittest

from tools import gt6_large_crusher as crusher


class LargeCrusherSourceTest(unittest.TestCase):
    def test_source_check_passes(self) -> None:
        self.assertEqual([], crusher.check())


if __name__ == "__main__":
    unittest.main()
