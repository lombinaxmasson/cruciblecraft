from __future__ import annotations

import copy
import hashlib
import json
import os
import unittest
from pathlib import Path

from tools import build_t13_machine_energy_denominators as builder


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


class T13MachineEnergyDenominatorsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = builder.load(builder.POLICY)
        cls.machine, cls.energy = builder.build()

    def test_fixed_source_and_symbol_inventory_are_pinned(self):
        self.assertEqual(
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
            self.machine["source"]["revision"],
        )
        self.assertEqual(
            {
                "loader",
                "material_tiers",
                "energy_tags",
                "basic_machine",
            },
            set(self.machine["source_files"]),
        )
        for record in self.machine["source_files"].values():
            self.assertRegex(record["git_blob_sha1"], r"^[0-9a-f]{40}$")
            self.assertRegex(record["sha256"], r"^[0-9a-f]{64}$")
        tree = self.machine["tree_symbol_inventory"]
        self.assertEqual(69, tree["counts"]["total"])
        self.assertEqual(45, tree["counts"]["registered_behavior"])
        self.assertEqual(24, tree["counts"]["excluded"])
        self.assertEqual(0, tree["counts"]["unclassified"])
        roles = {row["inventory_role"] for row in tree["rows"]}
        self.assertTrue({
            "registered_behavior",
            "abstract_exclusion",
            "interface_exclusion",
            "compat_exclusion",
        }.issubset(roles))
        self.assertIn(
            "helper or unregistered concrete symbol",
            self.policy["normalization"]["exclusions"],
        )
        self.assertTrue(all(
            row["source_identity"]["source_blob"]
            == row["git_blob_sha1"]
            and row["source_identity"]["source_symbol_or_extraction_key"]
            == row["symbol"]
            for row in tree["rows"]
        ))

    def test_raw_registration_denominator_and_canonical_counts(self):
        counts = self.machine["counts"]
        self.assertEqual(1354, counts["raw_registration_call_sites"])
        self.assertEqual(2328, counts["raw_expanded_registrations"])
        self.assertEqual(591, counts["canonical_assigned_call_sites"])
        self.assertEqual(
            627, counts["canonical_assigned_expanded_registrations"]
        )
        self.assertEqual(763, counts["excluded_call_sites"])
        self.assertEqual(1701, counts["excluded_expanded_registrations"])
        self.assertEqual(96, counts["canonical_kinds"])
        self.assertEqual(84, counts["tiered"])
        self.assertEqual(12, counts["fixed"])
        self.assertEqual(0, counts["unclassified"])
        self.assertEqual(
            {"T15", "T16", "T17", "T18", "POST_T19"},
            set(counts["owners"]),
        )

    def test_raw_to_canonical_and_exclusion_sets_are_bidirectional(self):
        snapshot_keys = {
            row["source_identity"]["normalized_row_key"]
            for row in self.policy["source_snapshot"]["raw_registrations"]
        }
        canonical_keys = {
            variant["source_identity"]["normalized_row_key"]
            for kind in self.machine["canonical_kinds"]
            for variant in kind["variants"]
        }
        exclusion_keys = {
            row["source_identity"]["normalized_row_key"]
            for row in self.machine["exclusions"]
        }
        self.assertFalse(canonical_keys & exclusion_keys)
        self.assertEqual(snapshot_keys, canonical_keys | exclusion_keys)
        self.assertEqual(
            len(self.machine["exclusions"]), len(exclusion_keys)
        )
        ledger = self.machine["raw_assignment_ledger"]
        self.assertTrue(ledger["bidirectional_complete"])
        self.assertTrue(ledger["unique_assignment"])
        self.assertEqual(
            canonical_keys, set(ledger["canonical_source_keys"])
        )
        self.assertEqual(
            exclusion_keys, set(ledger["exclusion_source_keys"])
        )

    def test_canonical_key_retains_behavior_map_energy_and_flags(self):
        keys = [row["canonical_key"] for row in self.machine["canonical_kinds"]]
        self.assertEqual(len(keys), len(set(keys)))
        for row in self.machine["canonical_kinds"]:
            self.assertIn(row["behavior_class"], row["canonical_key"])
            self.assertIn(row["process_map"], row["canonical_key"])
            self.assertIn(
                f"accepts:{row['accepted_energy']}", row["canonical_key"]
            )
            self.assertIn(
                f"emits:{row['emitted_energy']}", row["canonical_key"]
            )
            self.assertEqual(
                {
                    "source_revision",
                    "source_blob",
                    "source_path",
                    "source_symbol_or_extraction_key",
                    "normalized_row_key",
                },
                set(row["source_identity"]),
            )
            self.assertTrue(row["source_ids"])
            self.assertTrue(row["variants"])

    def test_cc_25_plus_12_is_target_mapping_only(self):
        mapping = self.machine["cc_25_plus_12_mapping"]
        self.assertIn("target-only", mapping["role"])
        self.assertEqual(25, mapping["counts"]["processing"])
        self.assertEqual(12, mapping["counts"]["non_spec"])
        self.assertEqual(0, mapping["counts"]["unclassified"])
        self.assertEqual(
            25, len({row["cc_id"] for row in mapping["processing"]})
        )
        self.assertEqual(
            12, len({row["cc_id"] for row in mapping["non_spec"]})
        )
        source_paths = {
            record["path"]
            for record in self.policy["source_snapshot"][
                "source_files"
            ].values()
        }
        self.assertFalse(any("t12_" in path.lower() for path in source_paths))

    def test_all_td_energy_declarations_and_relations_are_preserved(self):
        self.assertEqual(20, self.energy["counts"]["identities"])
        self.assertEqual(20, self.energy["counts"]["classified"])
        self.assertEqual(0, self.energy["counts"]["unclassified"])
        rows = {row["symbol"]: row for row in self.energy["rows"]}
        self.assertEqual(set(rows), set(self.energy["relations"]["ALL"]))
        self.assertEqual(
            {"KINETIC_ROTATION", "KINETIC_PUSH"},
            set(self.energy["relations"]["ALL_KINETIC"]),
        )
        self.assertEqual(
            "KINETIC_ROTATION",
            rows["KINETIC_ROTATION"]["local_energy_type"],
        )
        self.assertEqual(
            "KINETIC_PUSH",
            rows["KINETIC_PUSH"]["local_energy_type"],
        )
        self.assertEqual(
            "FLUID_STEAM_TRANSPORT", rows["STEAM"]["topology"]
        )
        self.assertGreaterEqual(
            self.energy["counts"]["referenced_by_machine_kinds"], 10
        )
        self.assertEqual(
            "EnergyType",
            self.energy["local_mapping_source"][
                "source_symbol_or_extraction_key"
            ],
        )
        self.assertIn(
            "KINETIC",
            self.energy["local_mapping_source"]["declared_symbols"],
        )
        self.assertIn(
            "RU",
            rows["KINETIC_ROTATION"]["aliases"],
        )
        self.assertIn(
            "KU",
            rows["KINETIC_PUSH"]["aliases"],
        )

    def test_legacy_kinetic_is_not_a_source_identity(self):
        boundary = self.energy["legacy_kinetic_boundary"]
        self.assertIsNone(boundary["source_identity"])
        self.assertEqual("KINETIC", boundary["local_energy_type"])
        self.assertIn("not a TD.Energy identity", boundary["rule"])
        self.assertNotIn(
            "KINETIC", {row["tag_name"] for row in self.energy["rows"]}
        )

    def test_deferred_rows_have_complete_later_owner_contracts(self):
        deferred = [
            row
            for row in self.machine["canonical_kinds"]
            + self.energy["rows"]
            if row["classification"] == "deferred_with_reason"
        ]
        self.assertTrue(deferred)
        for row in deferred:
            for field in builder.DEFERRED_FIELDS:
                self.assertIsInstance(row[field], str)
                self.assertTrue(row[field].strip())
            self.assertEqual("POST_T19", row["owner"])
        self.assertEqual(
            0,
            sum(
                row["classification"] == "unclassified"
                for row in self.machine["canonical_kinds"]
                + self.energy["rows"]
            ),
        )

    def test_independent_expected_parser_does_not_group(self):
        fixture = """
class Fixture {
  void build() {
    Class<?> aClass = MultiTileEntityBasicMachine.class;
    aRegistry.add("One", "Basic Machines", 1, 1, aClass, 0, 16, aMachine, UT.NBT.make(NBT_INPUT, 32, NBT_ENERGY_ACCEPTED, TD.Energy.RU, NBT_RECIPEMAP, RM.One));
    // aRegistry.add("Comment", "Basic Machines", 2, 1, aClass, 0, 16, aMachine, null);
    /* aRegistry.add("Block", "Basic Machines", 3, 1, aClass, 0, 16, aMachine, null); */
    aRegistry.add("Two", "Basic Machines", 4, 1, MultiTileEntityBasicMachineElectric.class, 0, 16, aMachine, UT.NBT.make(NBT_INPUT, 128, NBT_ENERGY_ACCEPTED, TD.Energy.EU, NBT_RECIPEMAP, RM.Two));
  }
}
"""
        production = builder.parse_loader_registrations(
            fixture, "0" * 40, "fixture"
        )
        expected = builder.independent_expected_loader_keys(fixture)
        self.assertEqual(
            expected,
            [
                row["source_identity"]["normalized_row_key"]
                for row in production
            ],
        )
        self.assertEqual(2, len(expected))
        self.assertFalse(
            self.machine["independent_expected"][
                "normalizer_grouping_reused"
            ]
        )

    def test_cardinality_and_source_identity_mutations_fail_closed(self):
        mutation = copy.deepcopy(self.policy)
        mutation["normalization"]["declared_collapse_fields"].remove(
            "material_expression"
        )
        with self.assertRaises(ValueError):
            builder.build(mutation)

        duplicate = copy.deepcopy(self.policy)
        duplicate["source_snapshot"]["raw_registrations"].append(
            copy.deepcopy(
                duplicate["source_snapshot"]["raw_registrations"][0]
            )
        )
        with self.assertRaises(ValueError):
            builder.build(duplicate)

        expected_drift = copy.deepcopy(self.policy)
        expected_drift["source_snapshot"]["independent_expected"][
            "raw_registration_keys"
        ].pop()
        with self.assertRaises(ValueError):
            builder.build(expected_drift)

        blob_drift = copy.deepcopy(self.policy)
        blob_drift["source_snapshot"]["raw_registrations"][0][
            "source_identity"
        ]["source_blob"] = "0" * 40
        with self.assertRaises(ValueError):
            builder.build(blob_drift)

        for audit in (
            self.machine["field_cardinality_audit"],
            self.energy["field_cardinality_audit"],
        ):
            self.assertEqual("PASS", audit["status"])
            self.assertEqual([], audit["uniform_findings"])
            self.assertEqual("UNIFORM_", audit["status_prefix"])

    def test_optional_full_source_replay_matches_snapshot(self):
        configured = os.environ.get("T13_GT6_SOURCE")
        source_root = (
            Path(configured)
            if configured
            else builder.ROOT / "build/t13c-gt6"
        )
        if not source_root.is_dir():
            self.skipTest("pinned GT6 source checkout is not available")
        builder.verify_source(source_root, self.policy)

    def test_committed_artifacts_are_current_and_check_is_immutable(self):
        before = {
            builder.MACHINE_OUTPUT: digest(builder.MACHINE_OUTPUT),
            builder.ENERGY_OUTPUT: digest(builder.ENERGY_OUTPUT),
        }
        self.assertEqual([], builder.check())
        self.assertEqual(
            before,
            {
                builder.MACHINE_OUTPUT: digest(builder.MACHINE_OUTPUT),
                builder.ENERGY_OUTPUT: digest(builder.ENERGY_OUTPUT),
            },
        )
        self.assertEqual(
            self.machine,
            json.loads(builder.MACHINE_OUTPUT.read_text(encoding="utf-8")),
        )
        self.assertEqual(
            self.energy,
            json.loads(builder.ENERGY_OUTPUT.read_text(encoding="utf-8")),
        )


if __name__ == "__main__":
    unittest.main()
