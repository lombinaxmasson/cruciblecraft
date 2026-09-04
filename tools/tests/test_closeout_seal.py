"""Wave-slug closeout --check. Numbered-card seals are no longer a live gate."""
from __future__ import annotations

import unittest

from tools import closeout_seal


class WaveCloseoutSealTest(unittest.TestCase):
    def test_default_check_is_waves_only(self) -> None:
        self.assertEqual(0, closeout_seal.main(["--check"]))

    def test_check_all_uses_wave_seals(self) -> None:
        self.assertEqual([], closeout_seal.check_all())


if __name__ == "__main__":
    unittest.main()
