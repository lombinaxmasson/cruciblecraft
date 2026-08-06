from __future__ import annotations

import copy
import json
import unittest
from collections import Counter

from tools import build_t14_extruder_equivalence as builder


class T14ExtruderEquivalenceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.bundle = builder.build_bundle()
        cls.expected = json.loads(cls.bundle.expected)
        cls.compact = json.loads(cls.bundle.compact)
        cls.readiness = json.loads(cls.bundle.readiness)
        cls.replay = json.loads(cls.bundle.legacy_replay)

    def test_expected_selector_is_independent_and_bidirectional(self) -> None:
        source = builder.read_json(builder.SOURCE)
        independently_selected = builder.build_expected_independently(
            source,
            self.replay,
            builder.material_facts(),
        )
        self.assertEqual(self.expected, independently_selected)
        builder.verify_equivalence(self.expected, self.compact)
        expected_ids = {
            row["stable_id"] for row in self.expected["relations"]
        }
        compact_ids = {
            row["stable_id"]
            for row in builder.compact_relations(self.compact)
        }
        self.assertEqual(expected_ids, compact_ids)
        self.assertEqual(2782, len(expected_ids))

    def test_all_locked_fields_and_exact_relation_fingerprint_are_stable(self) -> None:
        self.assertEqual(list(builder.LOCKED_FIELDS), self.expected["locked_fields"])
        self.assertEqual(
            builder.sha256_json(self.expected["relations"]),
            self.expected["relation_fingerprint"],
        )
        rows = self.expected["relations"]
        self.assertEqual(list(range(2782)), [row["shadow_order"] for row in rows])
        self.assertEqual(
            {"dust", "none"},
            {row["fallback"] for row in rows},
        )
        self.assertTrue(any(row["plateGem"] for row in rows))
        self.assertTrue(all(
            "forging_target" in row and row["forging_target"] is not None
            for row in rows
        ))

    def test_mutations_of_duration_eut_fallback_shadow_and_plate_gem_fail(self) -> None:
        mutations = {
            "duration": lambda row: row.__setitem__(
                "duration", row["duration"] + 1
            ),
            "eut": lambda row: row.__setitem__("eut", row["eut"] + 1),
            "fallback": lambda row: row.__setitem__(
                "fallback", "dust" if row["fallback"] == "none" else "none"
            ),
            "shadow_order": lambda row: row.__setitem__(
                "shadow_order", row["shadow_order"] + 1
            ),
            "plateGem": lambda row: row.__setitem__(
                "plateGem", not row["plateGem"]
            ),
        }
        for field, mutate in mutations.items():
            with self.subTest(field=field):
                candidate = copy.deepcopy(self.compact)
                mutate(candidate["templates"][0]["relations"][0])
                with self.assertRaisesRegex(
                    builder.EquivalenceError, field
                ):
                    builder.verify_equivalence(self.expected, candidate)

    def test_missing_extra_and_duplicate_stable_ids_fail_closed(self) -> None:
        missing = copy.deepcopy(self.compact)
        missing["templates"][0]["relations"].pop()
        with self.assertRaisesRegex(builder.EquivalenceError, "missing"):
            builder.verify_equivalence(self.expected, missing)

        duplicate = copy.deepcopy(self.compact)
        duplicate["templates"][0]["relations"].append(copy.deepcopy(
            duplicate["templates"][0]["relations"][0]
        ))
        with self.assertRaisesRegex(builder.EquivalenceError, "duplicate"):
            builder.verify_equivalence(self.expected, duplicate)

    def test_20_playable_42_skipped_remainder_and_t8_are_separate(self) -> None:
        self.assertEqual(20, self.compact["authored_entry_count"])
        self.assertEqual(2782, self.compact["logical_relation_count"])
        self.assertEqual(
            2782,
            sum(
                template["relation_count"]
                for template in self.compact["templates"]
            ),
        )
        scope = self.readiness["scope"]
        self.assertEqual(20, scope["authored_entries"])
        self.assertEqual(2782, scope["logical_relations"])
        self.assertEqual(2782, scope["runtime_publication"])
        self.assertEqual(42, scope["skipped_templates"])
        self.assertEqual(1, scope["exact_remainder"])
        self.assertEqual(257, scope["t8_pipe_publication_separate_owner"])
        self.assertEqual(
            {"skipped": 42},
            dict(Counter(
                row["classification"]
                for row in self.readiness["classification"]["skipped_templates"]
            )),
        )
        self.assertEqual(
            "discrete_recipe",
            self.readiness["classification"]["exact_remainder"][0][
                "classification"
            ],
        )

    def test_legacy_replay_is_verification_only_and_complete(self) -> None:
        self.assertEqual(
            "verification_only_not_a_runtime_datapack",
            self.replay["delivery"],
        )
        self.assertEqual(2782, self.replay["captured_rule_count"])
        self.assertEqual(2782, len(self.replay["files"]))

    def test_repository_artifacts_are_byte_exact(self) -> None:
        self.assertEqual([], builder.check_bundle(self.bundle))


if __name__ == "__main__":
    unittest.main()
