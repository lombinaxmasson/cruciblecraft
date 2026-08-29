#!/usr/bin/env python3
"""Contract tests for the T45 production lock."""
from __future__ import annotations

import unittest

from tools import build_t45_candidate_selection as candidate
from tools import build_t45_production_lock as lock
from tools import t45_common as common

EXPECTED_SELECTION = (
    "086ae3899b89efca2d049bca40196720176f399e00d2beba4c7ea5173dbdc63d"
)


class T45ProductionLockTest(unittest.TestCase):
    def test_lock_is_379_singletons_without_centrifuge(self) -> None:
        document = lock.build()
        production = document["production"]
        self.assertEqual("T45_PRODUCTION_LOCKED", document["status"])
        self.assertEqual(379, production["family_count"])
        self.assertEqual(379, production["relation_count"])
        self.assertEqual(EXPECTED_SELECTION, production["selection_sha256"])
        self.assertEqual(
            EXPECTED_SELECTION,
            document["candidate_snapshot"]["selection_sha256"],
        )
        self.assertEqual(EXPECTED_SELECTION, candidate.build()["selection_sha256"])
        self.assertEqual(379, len(set(production["stable_ids"])))
        self.assertGreaterEqual(document["support"]["route_count"], 1)
        hosts = {row["host"] for row in production["families"]}
        self.assertEqual(
            {"cruciblecraft:smelter", "cruciblecraft:drying"},
            hosts,
        )
        self.assertTrue(
            all(row["representation"] == "singleton" for row in production["families"])
        )
        templates = {row["template_key"] for row in production["families"]}
        self.assertTrue(templates.isdisjoint(common.EXCLUDED_TEMPLATE_KEYS))
        groups = {row["publication_group"] for row in production["families"]}
        self.assertEqual(set(common.PUBLICATION_GROUPS), groups)
