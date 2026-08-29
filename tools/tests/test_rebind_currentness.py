"""rebind_currentness CLI tests."""
from __future__ import annotations

import unittest

from tools import rebind_currentness as rebind


class RebindCurrentnessTest(unittest.TestCase):
    def test_plan_lists_t35_and_card_closeout(self) -> None:
        self.assertEqual(0, rebind.main(["--plan", "--scope", "t35"]))
        self.assertEqual(0, rebind.main(["--plan", "--scope", "card-closeout"]))
        self.assertEqual(0, rebind.main(["--plan", "--scope", "recipes"]))


if __name__ == "__main__":
    unittest.main()
