"""Contract tests for the T40 load projection."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t40_load_projection as builder  # noqa: E402
from tools import t40_common as t40  # noqa: E402


class T40LoadProjectionTest(unittest.TestCase):
    def test_input_binds_production_lock_hash(self) -> None:
        document = builder.build_input()
        output = builder.build()
        self.assertEqual(t40.production_lock_sha256(), output["production_lock_sha256"])
        if t40.production_strategy()["blocked"]:
            self.assertEqual("FAMILY_LOAD_PROJECTION_INPUT_BLOCKED", document["status"])
            return
        self.assertEqual("FAMILY_LOAD_PROJECTION_INPUT", document["status"])
        family = document["families"][0]
        self.assertEqual(t40.production_family_count(), family["authored_entries"])
        self.assertEqual(t40.production_relation_count(), family["logical_rows"])
