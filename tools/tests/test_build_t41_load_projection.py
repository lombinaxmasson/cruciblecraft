"""Contract tests for the T41 load projection."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t41_load_projection as builder  # noqa: E402
from tools import t41_common as t41  # noqa: E402


class T41LoadProjectionTest(unittest.TestCase):
    def test_input_binds_production_lock_hash(self) -> None:
        document = builder.build_input()
        output = builder.build()
        self.assertEqual(t41.production_lock_sha256(), output["production_lock_sha256"])
        if t41.production_strategy()["blocked"]:
            self.assertEqual("FAMILY_LOAD_PROJECTION_INPUT_BLOCKED", document["status"])
            return
        self.assertEqual("FAMILY_LOAD_PROJECTION_INPUT", document["status"])
        family = document["families"][0]
        self.assertEqual(t41.production_family_count(), family["authored_entries"])
        self.assertEqual(t41.production_relation_count(), family["logical_rows"])
        measured = family["measurement_basis"]["measured_logical_rows"]
        counts = t41.production_group_counts()
        self.assertEqual(
            sorted({
                counts[t41.PLANKS_GROUP]["relations"],
                counts[t41.FIREPROOF_GROUP]["relations"],
                counts[t41.PLANKS2_GROUP]["relations"],
                t41.production_relation_count(),
            }),
            measured,
        )
