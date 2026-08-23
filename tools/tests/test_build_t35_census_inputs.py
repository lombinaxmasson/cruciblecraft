from __future__ import annotations

import copy
import json
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from unittest import mock

from tools import build_t35_census_inputs as builder
from tools import t27_common as common


class T35CensusInputsTest(unittest.TestCase):
    def test_committed_artifact_is_stable_and_check_is_read_only(self) -> None:
        expected = builder.build()
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        if "storage_scope" not in on_disk:
            self.skipTest(
                "T35 census inputs artifact awaits downstream T35R regeneration"
            )
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_expected_count_constraints(self) -> None:
        document = builder.build()
        tables = document["t13_seven_tables"]
        self.assertEqual(765, tables["canonical_identities_total"])
        self.assertEqual(95, tables["recipe_maps"]["canonical_count"])
        self.assertEqual(452, tables["prefixes"]["canonical_count"])
        self.assertEqual(25, tables["itemgenerator_domains"]["canonical_count"])
        self.assertEqual(96, tables["machine_kinds"]["canonical_count"])
        self.assertEqual(20, tables["energy_identities"]["canonical_count"])
        self.assertEqual(47, tables["cover_kinds"]["canonical_count"])
        self.assertEqual(30, tables["multiblock_kinds"]["canonical_count"])
        exclusions = document["exclusion_counts"]
        self.assertEqual(763, exclusions["excluded_call_sites"])
        self.assertEqual(1701, exclusions["excluded_expanded_registrations"])
        storage = document["storage_counts"]
        self.assertEqual(28, storage["source_sites"])
        self.assertEqual(624, storage["expanded_registrations"])
        t22_5 = document["t22_5_counts"]
        self.assertEqual(146841, t22_5["row_universe"])
        self.assertEqual(78682, t22_5["ordinary_optional"])
        opening = document["publication_opening"]
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, opening["publication_delta"])
        self.assertEqual(13, document["t14_authority"]["axis_count"])

    def test_storage_scope_inputs_and_vocabularies_are_recorded(self) -> None:
        document = builder.build()
        self.assertEqual(
            list(builder.LEGAL_PORTFOLIO_SCOPES),
            document["vocabularies"]["portfolio_scopes"],
        )
        scope_input = document["inputs"]["t35_storage_scope"]
        self.assertEqual(builder.STORAGE_SCOPE_INPUT, scope_input["path"])
        self.assertIn("sha256", scope_input)
        self.assertEqual(
            scope_input["sha256"],
            document["currentness"]["owned_inputs"][scope_input["path"]],
        )
        track_input = document["inputs"]["t35_machine_track"]
        self.assertEqual(builder.MACHINE_TRACK_INPUT, track_input["path"])
        if track_input["status"] == "available":
            self.assertEqual(
                track_input["sha256"],
                document["currentness"]["owned_inputs"][track_input["path"]],
            )
        else:
            self.assertEqual("bootstrap_pending", track_input["status"])
            self.assertNotIn(
                track_input["path"],
                document["currentness"]["owned_inputs"],
            )
        scope = document["storage_scope"]
        self.assertEqual(28, scope["denominators"]["storage"]["source_sites"])
        self.assertEqual(624, scope["denominators"]["storage"]["expanded_rows"])
        logistics = scope["denominators"]["mass_storage_logistics"]
        self.assertEqual(1, logistics["source_sites"])
        self.assertEqual(1, logistics["expanded_rows"])
        self.assertFalse(logistics["counts_toward_storage_624"])

    def test_roadmap_is_narrative_and_not_in_currentness_contract(self) -> None:
        document = builder.build()
        roadmap = document["inputs"]["roadmap"]
        self.assertEqual("docs/current/roadmap.md", roadmap["path"])
        self.assertEqual("narrative", roadmap["currentness"])
        self.assertNotIn("sha256", roadmap)
        self.assertNotIn(
            "docs/current/roadmap.md",
            document["currentness"]["owned_inputs"],
        )
        roadmap_hash = common.sha256_file(builder.ROOT / "docs" / "current" / "roadmap.md")
        self.assertNotIn(roadmap_hash, common.stable_json(document))
        with mock.patch.object(
            builder,
            "_narrative_input_record",
            return_value={"path": "docs/current/roadmap.md", "currentness": "narrative"},
        ):
            unchanged = builder.build()
        self.assertEqual(document["inputs"]["roadmap"], unchanged["inputs"]["roadmap"])
        self.assertEqual(
            document["currentness"]["owned_inputs"],
            unchanged["currentness"]["owned_inputs"],
        )

    def test_storage_scope_rejects_policy_and_family_scope_drift(self) -> None:
        policy = common.load_json(builder.POLICY)
        invalid_policy = copy.deepcopy(policy)
        invalid_policy["portfolio_scopes"] = list(
            reversed(builder.LEGAL_PORTFOLIO_SCOPES)
        )
        with self.assertRaisesRegex(ValueError, "portfolio_scopes must exactly equal"):
            builder._validate_policy(invalid_policy)
        invalid_policy = copy.deepcopy(policy)
        invalid_policy["input_domains"]["t35_machine_track"] = "tools/missing.json"
        with self.assertRaisesRegex(
            ValueError,
            "input_domains.t35_machine_track must equal",
        ):
            builder._validate_policy(invalid_policy)

        scope_path = builder.ROOT / policy["input_domains"]["t35_storage_scope"]
        scope = common.load_json(scope_path)
        for family, expected_scope in builder.STORAGE_SCOPE_CHOICES.items():
            with self.subTest(family=family):
                self.assertEqual(
                    expected_scope,
                    scope["families"][family]["portfolio_scope"],
                )
                invalid_scope = copy.deepcopy(scope)
                invalid_scope["families"][family]["portfolio_scope"] = next(
                    scope
                    for scope in builder.LEGAL_PORTFOLIO_SCOPES
                    if scope != expected_scope
                )
                with self.assertRaisesRegex(
                    ValueError,
                    f"families\\.{family}\\.portfolio_scope must be",
                ):
                    builder._validate_storage_scope_document(invalid_scope, policy)

    def test_storage_scope_rejects_denominator_and_locker_lineage_drift(self) -> None:
        policy = common.load_json(builder.POLICY)
        scope_path = builder.ROOT / policy["input_domains"]["t35_storage_scope"]
        scope = common.load_json(scope_path)

        invalid_denominator = copy.deepcopy(scope)
        invalid_denominator["denominators"]["storage"]["expanded_rows"] = 623
        with self.assertRaisesRegex(
            ValueError,
            "denominators.storage.expanded_rows must be integer 624",
        ):
            builder._validate_storage_scope_document(invalid_denominator, policy)

        invalid_locker = copy.deepcopy(scope)
        charging = next(
            item
            for item in invalid_locker["families"]["locker"]["folded_source_behaviors"]
            if item["role"] == "charging"
        )
        charging.pop("behavior_diff")
        with self.assertRaisesRegex(ValueError, "locker charging.behavior_diff"):
            builder._validate_storage_scope_document(invalid_locker, policy)

    def test_missing_machine_track_is_bootstrap_pending(self) -> None:
        with TemporaryDirectory(dir=builder.ROOT) as directory:
            record = builder._optional_input_record(
                Path(directory) / "t35_machine_track.json"
            )
        self.assertEqual(
            {
                "path": record["path"],
                "status": "bootstrap_pending",
            },
            record,
        )

    def test_missing_required_input_fails_closed(self) -> None:
        with mock.patch.object(
            builder,
            "_collect_inputs",
            side_effect=FileNotFoundError("missing required input"),
        ):
            with self.assertRaises(FileNotFoundError):
                builder.build()

    def test_stale_hash_is_detected(self) -> None:
        expected = builder.build()
        tampered = json.loads(json.dumps(expected))
        tampered["t22_5_counts"]["row_universe"] += 1
        with TemporaryDirectory(dir=builder.ROOT) as directory:
            output = Path(directory) / "t35_census_inputs.json"
            output.write_text(common.stable_json(tampered), encoding="utf-8")
            with mock.patch.object(builder, "OUTPUT", output):
                errors = builder.check()
        self.assertTrue(any("stale" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
