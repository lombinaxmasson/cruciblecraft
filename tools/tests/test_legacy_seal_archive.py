"""Archive snapshots stay byte-identical to live T38-T49 seals."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import closeout_seal
from tools import legacy_seal_archive
from tools import legacy_seal_resolver as resolver


class LegacySealArchiveTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if not resolver.archived_card_ids():
            raise unittest.SkipTest("legacy seal archive has not been written yet")

    def test_every_closed_card_has_an_archive(self) -> None:
        missing = [
            card_id
            for card_id in closeout_seal.closed_card_ids()
            if not resolver.has_archive(card_id)
        ]
        self.assertEqual([], missing)

    def test_seal_snapshot_is_byte_identical(self) -> None:
        for card_id in closeout_seal.closed_card_ids():
            live = closeout_seal.seal_path(card_id)
            snapshot = resolver.seal_snapshot_path(card_id)
            self.assertTrue(live.is_file(), card_id)
            self.assertEqual(live.read_bytes(), snapshot.read_bytes(), card_id)

    def test_resolver_check_matches_live_check(self) -> None:
        self.assertEqual([], legacy_seal_archive.check_all())
        self.assertEqual([], closeout_seal.check_all())

    def test_resolved_spec_reads_archive_census(self) -> None:
        cards = closeout_seal.closed_card_ids()
        self.assertTrue(cards)
        spec = closeout_seal.resolved_spec(cards[-1])
        census = str(spec.census).replace("\\", "/")
        self.assertIn("/archive/sealed/", census)
        self.assertTrue(census.endswith("census_delta.json") or "census" in census)
        if spec.gametest_java is not None:
            java = str(spec.gametest_java).replace("\\", "/")
            self.assertIn("/archive/sealed/", java)
            self.assertTrue(spec.gametest_java.is_file())


if __name__ == "__main__":
    unittest.main()
