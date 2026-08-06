from __future__ import annotations

import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tools import build_t13_recipe_map_denominator as builder


class T13RecipeMapDenominatorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = builder.load(builder.POLICY)
        cls.document = builder.load(builder.OUTPUT)
        cls.scanned = builder.scanned_from_committed(cls.document)
        cls.index = {
            "mapCount": builder.EXPECTED_MAP_COUNT,
            "recipeCount": builder.EXPECTED_RECIPE_COUNT,
            "maps": [
                {
                    "file": row["file"],
                    "nameInternal": row["name_internal"],
                    "nameLocal": row["name_local"],
                    "recipeCount": row["actual_recipe_count"],
                    "inputItems": row["input_items"],
                    "outputItems": row["output_items"],
                    "inputFluids": row["input_fluids"],
                    "outputFluids": row["output_fluids"],
                }
                for row in cls.scanned
            ],
        }
        builder.validate_index_and_expected_set(
            cls.index,
            cls.scanned,
            cls.policy,
        )

    def test_official_tree_manifest_is_complete_for_selected_java_scope(self):
        manifest = builder.load(builder.TREE_MANIFEST)
        builder.validate_tree_manifest(manifest)
        self.assertEqual(builder.REVISION, manifest["revision"])
        self.assertEqual(builder.TREE_SHA1, manifest["tree_sha1"])
        self.assertFalse(manifest["recursive_tree_truncated"])
        self.assertEqual(
            {
                "entries": 23508,
                "by_type": {"blob": 21743, "tree": 1765},
                "blob_bytes": 24978996,
            },
            manifest["integrity"]["raw_tree"],
        )
        self.assertEqual(
            {
                "entries": 1229,
                "by_type": {"blob": 1229},
                "blob_bytes": 14138745,
            },
            manifest["integrity"]["selected_tree"],
        )
        self.assertIn("api.github.com", manifest["commit_api_url"])
        self.assertIn("api.github.com", manifest["recursive_tree_api_url"])

    def test_symbol_inventory_covers_required_source_path_families(self):
        _, inventory = builder.validate_committed_source_evidence()
        self.assertEqual(1229, inventory["counts"]["source_files"])
        self.assertEqual(1546, inventory["counts"]["declarations"])
        self.assertEqual(790, inventory["counts"]["registration_symbols"])
        self.assertEqual(
            {"cover", "multiblock", "machine", "energy", "op"},
            set(inventory["category_paths"]),
        )
        self.assertTrue(all(inventory["category_paths"].values()))
        self.assertEqual(
            ["src/main/java/gregapi/data/OP.java"],
            inventory["category_paths"]["op"],
        )
        declarations = inventory["declarations"]
        self.assertTrue(all(
            {
                "package",
                "class",
                "abstract",
                "interface",
                "extends",
                "implements",
                "path",
                "blob_sha1",
            }
            <= set(row)
            for row in declarations
        ))
        registration_kinds = {
            row["kind"] for row in inventory["registration_symbols"]
        }
        self.assertEqual(
            {"ore_dict_prefix", "recipe_map", "tag_data"},
            registration_kinds,
        )
        self.assertTrue(all(
            "<" not in row["declared_type"]
            and "[" not in row["declared_type"]
            for row in inventory["registration_symbols"]
        ))

    def test_source_replay_matches_when_fixed_source_is_present(self):
        if not builder.DEFAULT_SOURCE_ROOT.is_dir():
            self.skipTest("build/t13-gt6-source is an explicit evidence cache")
        replay = builder.verify_inventory_replay(
            builder.DEFAULT_SOURCE_ROOT
        )
        self.assertEqual(1229, replay["counts"]["source_files"])

    def test_normalizer_and_independent_expected_set_are_bidirectional(self):
        source_names = [
            row["nameInternal"] for row in self.index["maps"]
        ]
        normalized = {
            builder.normalize_map_id(name) for name in source_names
        }
        expected = set(self.policy["dispositions"])
        self.assertEqual(expected, normalized)
        self.assertEqual(95, len(expected))
        self.assertIn("", source_names)
        self.assertIn("unnamed", expected)
        reverse = {
            builder.normalize_map_id(row["nameInternal"]):
            row["nameInternal"]
            for row in self.index["maps"]
        }
        self.assertEqual("", reverse["unnamed"])
        self.assertEqual(expected, set(reverse))
        self.assertTrue(
            self.policy["normalization"]["expected_set_is_independent"]
        )

    def test_index_actual_sets_counts_and_stable_digest_are_pinned(self):
        self.assertEqual(95, len(self.index["maps"]))
        self.assertEqual(95, len(self.scanned))
        self.assertEqual(
            720841,
            sum(row["actual_recipe_count"] for row in self.scanned),
        )
        self.assertEqual(
            "e8a635d62d4c11b7995d64ec3c6cfe2a7038b24338bfb00920a614f442f9af99",
            builder.map_row_digest(self.scanned),
        )
        self.assertEqual(
            {
                "method": (
                    "nearest-rank over all 95 maps, including zero/unnamed"
                ),
                "p50": 307,
                "p95": 27454,
                "max": 325595,
            },
            self.document["recipe_count_distribution"],
        )

    def test_index_and_scan_mutations_fail_closed(self):
        missing_actual = copy.deepcopy(self.scanned[:-1])
        with self.assertRaises(ValueError):
            builder.validate_index_and_expected_set(
                self.index,
                missing_actual,
                self.policy,
            )

        changed_count = copy.deepcopy(self.scanned)
        changed_count[0]["actual_recipe_count"] += 1
        with self.assertRaises(ValueError):
            builder.validate_index_and_expected_set(
                self.index,
                changed_count,
                self.policy,
            )

        changed_expected = copy.deepcopy(self.policy)
        changed_expected["dispositions"]["invented.map"] = {
            "classification": "unclassified"
        }
        with self.assertRaises(ValueError):
            builder.validate_index_and_expected_set(
                self.index,
                self.scanned,
                changed_expected,
            )

        changed_row = copy.deepcopy(self.scanned)
        changed_row[0]["dump_sha256"] = "0" * 64
        self.assertNotEqual(
            builder.map_row_digest(self.scanned),
            builder.map_row_digest(changed_row),
        )

    def test_classifications_close_all_rows_and_supersede_post_t3(self):
        self.assertEqual(
            {
                "deferred_with_reason": 64,
                "in_scope": 29,
                "out_of_scope": 2,
                "unclassified": 0,
            },
            self.document["counts"]["classifications"],
        )
        deferred = [
            row for row in self.document["rows"]
            if row["classification"] == "deferred_with_reason"
        ]
        self.assertTrue(all(
            all(row.get(field) for field in builder.DEFERRED_FIELDS)
            for row in deferred
        ))
        supersession = self.document["supersession"]
        self.assertEqual("POST_T3", supersession["historical_phase_owner"])
        self.assertTrue(supersession["history_preserved"])
        self.assertIn("POST_T19", supersession["replacement_owners"])

    def test_uniform_audit_schema_does_not_false_positive(self):
        audit = self.document["normalization_audit"]
        self.assertEqual("UNIFORM_PASS", audit["status"])
        self.assertEqual(95, audit["input_rows"])
        self.assertEqual(95, audit["output_rows"])
        self.assertEqual({}, audit["undeclared_many_to_one"])
        self.assertEqual(
            [],
            audit["unexplained_field_cardinality_reductions"],
        )
        self.assertEqual(
            0,
            audit["field_cardinality"]["nameInternal"]["reduction"],
        )

    def test_full_replay_is_explicit_in_builder_policy(self):
        policy = builder.load(
            builder.TOOLS / "verification_builder_policy.json"
        )
        record = next(
            row
            for row in policy["builders"]
            if row["script"]
            == "tools/build_t13_recipe_map_denominator.py"
        )
        self.assertEqual(
            ["--check", "--full-replay"],
            record["full_replay"]["args"],
        )
        self.assertEqual(
            ["--check", "--reference-only"],
            record["ordinary_args"],
        )

    def test_compact_check_does_not_scan_dump(self):
        with mock.patch.object(
            builder,
            "scan_recipe_maps",
            side_effect=AssertionError("compact check must not scan gt6_dump"),
        ):
            builder.validate_compact_document(self.document)

    def test_hash_fast_detects_blob_mutation_with_small_fixture(self):
        document = copy.deepcopy(self.document)
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            maps = root / "maps"
            maps.mkdir()
            for index, row in enumerate(document["rows"]):
                path = maps / Path(row["source_path"]).name
                payload = f"fixture-{index}".encode("utf-8")
                path.write_bytes(payload)
                row["source_blob"] = hashlib.sha256(payload).hexdigest()
                row["dump_size"] = len(payload)
            index_path = root / "index.json"
            index_path.write_text("{}\n", encoding="utf-8")
            scanned = builder.scanned_from_committed(document)
            document["full_replay_receipt"]["map_tree_sha256"] = (
                builder.map_tree_digest(scanned)
            )
            document["full_replay_receipt"]["dump_index_sha256"] = (
                builder.sha256(index_path)
            )
            builder.validate_dump_hashes(document, root)
            first = maps / Path(document["rows"][0]["source_path"]).name
            first.write_bytes(first.read_bytes() + b"!")
            with self.assertRaisesRegex(ValueError, "hash/size drifted"):
                builder.validate_dump_hashes(document, root)


if __name__ == "__main__":
    unittest.main()
