#!/usr/bin/env python3
"""Contract tests for the Phase-2 runtime-manifest cutover closeout."""
from __future__ import annotations

import unittest

from tools import t35_common as t35
from tools.recipe_bulk import cutover_readiness as readiness
from tools.recipe_bulk import runtime as runtime_mod


class CompactRecipeManifestCutoverReadinessTest(unittest.TestCase):
    def test_cutover_is_ready_without_family_ownership(self) -> None:
        document = readiness.build()
        self.assertEqual(readiness.STATUS_READY, document["status"])
        self.assertEqual([], document["failed_gates"])
        self.assertEqual(0, document["owns_families"])
        self.assertTrue(document["gates"]["java_policy_methods_removed"])
        self.assertTrue(document["gates"]["java_dedup_methods_removed"])
        self.assertTrue(document["gates"]["t43_membership_unified"])
        self.assertTrue(document["gates"]["t43_membership_skip_removed"])

    def test_nine_cutover_policies_match_runtime_manifest(self) -> None:
        documents = runtime_mod.cutover_policy_documents()
        self.assertEqual(9, len(documents))
        for group_id, document in documents.items():
            spec = runtime_mod.spec_for_group(group_id)
            path = runtime_mod.datapack_policy_path(spec)
            with self.subTest(group=group_id):
                self.assertTrue(path.is_file())
                self.assertEqual([], t35.check_generated_document(path, document))

    def test_four_dedup_rules_are_current(self) -> None:
        rules = {rule["rule_id"]: rule for rule in runtime_mod.dedup_rules()}
        self.assertEqual(4, len(rules))
        runtime_mod.validate_dedup_rules(list(rules.values()))
        for rule_id, document in rules.items():
            path = runtime_mod.datapack_dedup_root() / f"{rule_id.split(':', 1)[1]}.json"
            with self.subTest(rule=rule_id):
                self.assertTrue(path.is_file())
                self.assertEqual([], t35.check_generated_document(path, document))


if __name__ == "__main__":
    unittest.main()
