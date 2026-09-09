#!/usr/bin/env python3
"""Machine delivery sidecar stays aligned with kind/tier catalogs."""
from __future__ import annotations

import unittest

from tools import machine_delivery


class MachineDeliveryTest(unittest.TestCase):
    def test_check_is_clean(self) -> None:
        self.assertEqual([], machine_delivery.check())


if __name__ == "__main__":
    unittest.main()
