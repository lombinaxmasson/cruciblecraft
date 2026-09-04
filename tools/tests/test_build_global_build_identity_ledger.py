#!/usr/bin/env python3
"""Contract tests for the V1 global build-time identity ledger."""
from __future__ import annotations

import unittest

from tools.recipe_bulk import identity as identity_mod


class GlobalBuildIdentityLedgerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = identity_mod.build()

    def test_ledger_is_fail_closed_and_has_proven_records(self) -> None:
        self.assertEqual("GLOBAL_BUILD_IDENTITY_LEDGER_V1", self.document["status"])
        self.assertGreater(self.document["record_count"], 0)
        self.assertEqual(self.document["record_count"], len(self.document["records"]))
        self.assertEqual(self.document["blocker_count"], len(self.document["blockers"]))
        self.assertIn("storage/lock", self.document["identity_only_waves"])
        source_keys = [row["source_key"] for row in self.document["records"]]
        self.assertEqual(source_keys, sorted(source_keys))
        self.assertEqual(len(source_keys), len(set(source_keys)))

    def test_storage_stone_and_block_identities_are_present(self) -> None:
        keys = {row["source_key"] for row in self.document["records"]}
        self.assertTrue(any(key.startswith("storage:") for key in keys))
        self.assertTrue(any(key.startswith("stone:") for key in keys))
        self.assertTrue(any(key.startswith("block:") for key in keys))
        self.assertTrue(any(key.startswith("material_form:") for key in keys))
        self.assertTrue(any(key.startswith("fluid:") for key in keys))

    def test_unproven_facts_remain_typed_blockers(self) -> None:
        classes = {row["mapping_class"] for row in self.document["blockers"]}
        self.assertTrue(classes & {"unproven_alias", "unbound_tag", "stateful_identity", "unproven_lossy_alias"})
        any_diamond = [
            row
            for row in self.document["blockers"]
            if row["source_key"] == "roaster/compact|alias:AnyDiamond"
        ]
        self.assertEqual(1, len(any_diamond))
        self.assertIsNone(any_diamond[0]["target_identity"])
        stateful = [
            row
            for row in self.document["blockers"]
            if row["mapping_class"] == "stateful_identity"
        ]
        self.assertGreaterEqual(len(stateful), 1)

    def test_conflict_on_same_source_key_fails_closed(self) -> None:
        builder = identity_mod.LedgerBuilder()
        builder.commit(
            source_key="fluid:oxygen",
            target_identity="cruciblecraft:oxygen",
            target_kind="fluid",
            mapping_class="fluid",
            authority="roaster/compact",
            input_path="tools/roaster_operand_runtime_map.json",
            input_hash="a" * 64,
            evidence="test",
            disposition="proven",
        )
        with self.assertRaises(identity_mod.IdentityConflictError):
            builder.commit(
                source_key="fluid:oxygen",
                target_identity="minecraft:water",
                target_kind="fluid",
                mapping_class="fluid",
                authority="centrifuge/compact",
                input_path="tools/centrifuge_operand_runtime_map.json",
                input_hash="b" * 64,
                evidence="test",
                disposition="proven",
            )

    def test_same_mapping_merges_authorities(self) -> None:
        builder = identity_mod.LedgerBuilder()
        kwargs = dict(
            source_key="fluid:oxygen",
            target_identity="cruciblecraft:oxygen",
            target_kind="fluid",
            mapping_class="fluid",
            input_path="tools/roaster_operand_runtime_map.json",
            input_hash="a" * 64,
            evidence="oxygen",
            disposition="proven",
        )
        builder.commit(authority="roaster/compact", **kwargs)
        builder.commit(authority="block/object", **kwargs)
        record = builder.records["fluid:oxygen"]
        self.assertEqual(["block/object", "roaster/compact"], record["authorities"])


if __name__ == "__main__":
    unittest.main()
