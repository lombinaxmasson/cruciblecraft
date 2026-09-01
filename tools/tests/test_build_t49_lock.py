#!/usr/bin/env python3
"""T49 work-set / candidate / production-lock size and group contracts."""
from __future__ import annotations

import unittest

from tools import semantic_ids
from tools import wave_bath_tiny_purified as tiny_purified
from tools.build_t49_work_set import build as build_work_set


class T49LockContractTest(unittest.TestCase):
    def test_work_set_is_five_exact_multi_families(self) -> None:
        document = build_work_set()
        self.assertEqual("T49_WORK_SET_FROZEN", document["status"])
        self.assertEqual(5, document["family_count"])
        self.assertEqual(95, document["relation_count"])
        self.assertEqual(list(tiny_purified.LOCK_FAMILY_IDS), document["family_ids"])
        self.assertEqual(0, document["representation"]["exact"])
        self.assertEqual(5, document["representation"]["exact_multi"])
        self.assertEqual(
            "bath_host_remainder_recycling_correction",
            document["size_exception"]["exception_kind"],
        )
        self.assertEqual(
            ["Mixer", "Smelter", "Centrifuge production"],
            document["size_exception"]["padding_hosts_forbidden"],
        )

    def test_frozen_lock_is_new_group_only(self) -> None:
        if not tiny_purified.PRODUCTION_LOCK.is_file():
            self.skipTest("T49 production lock is not frozen yet")
        lock = tiny_purified.load_production_lock()
        production = lock["production"]
        self.assertEqual(5, production["family_count"])
        self.assertEqual(95, production["relation_count"])
        remapped_groups = [
            semantic_ids.remap_publication_group(str(group))
            for group in (production.get("publication_groups") or [])
        ]
        self.assertEqual(
            [tiny_purified.PUBLICATION_GROUP_EXACT_MULTI],
            remapped_groups,
        )
        groups = {
            semantic_ids.remap_publication_group(str(row.get("publication_group") or ""))
            for row in lock.get("families") or production.get("families") or []
        }
        if not groups and remapped_groups:
            groups = set(remapped_groups)
        self.assertEqual({tiny_purified.PUBLICATION_GROUP_EXACT_MULTI}, groups)
        for forbidden in tiny_purified.FORBIDDEN_PUBLICATION_GROUPS:
            self.assertNotIn(forbidden, groups)


if __name__ == "__main__":
    unittest.main()
