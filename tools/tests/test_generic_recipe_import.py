#!/usr/bin/env python3
"""Generic Source Pack importer: discovery, parity, and fail-closed matrix."""
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from tools import census_common as census
from tools.recipe_bulk import source_import
from tools.recipe_bulk.dialects import gt6
from tools.recipe_bulk.import_spec import ImportSpecError, load_import_spec
from tools.recipe_bulk.source_import import SourceImportError
from tools.recipe_bulk.spec_registry import load_discovered
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, SEMANTIC_WAVES, recipe_wave
from tools.build_recipe_bulk import WAVE_CHOICES


SMELTER = (
    census.ROOT
    / "src/test/resources/generic_recipe_import/smelter_exact_singleton/recipe_import.json"
)
MIXER = (
    census.ROOT
    / "src/test/resources/generic_recipe_import/mixer_exact_multi/recipe_import.json"
)


class GenericRecipeImportDiscoveryTest(unittest.TestCase):
    def test_fixtures_are_discovered_without_python_registry(self) -> None:
        if not SMELTER.is_file():
            self.skipTest("fixtures not written yet")
        discovered = load_discovered()
        self.assertIn("generic-import/smelter-exact-singleton", discovered)
        self.assertIn("generic-import/mixer-exact-multi", discovered)
        self.assertNotIn("machines/large-squeezer", discovered)
        self.assertNotIn("generic-import/smelter-exact-singleton", SEMANTIC_WAVES)
        self.assertNotIn("generic-import/mixer-exact-multi", SEMANTIC_COMPILE_ORDER)
        self.assertNotIn("generic-import/smelter-exact-singleton", WAVE_CHOICES)

    def test_compile_without_production_lock_fails_closed(self) -> None:
        if not SMELTER.is_file():
            self.skipTest("fixtures not written yet")
        with self.assertRaises(ValueError) as raised:
            recipe_wave("generic-import/smelter-exact-singleton")
        self.assertIn("production_lock.json", str(raised.exception))


class GenericRecipeImportParityTest(unittest.TestCase):
    def test_smelter_and_mixer_parity(self) -> None:
        if not SMELTER.is_file() or not MIXER.is_file():
            self.skipTest("fixtures not written yet")
        for spec_path in (SMELTER, MIXER):
            built = source_import.import_documents(spec_path)
            imported = [
                gt6.semantic_payload(row)
                for row in built["documents"]["source"]["relations"]
            ]
            sample = census.load_json(spec_path.parent / "compare_corpus.json")
            expected = [
                gt6.semantic_payload(row) for row in sample.get("relations") or []
            ]
            self.assertEqual(expected, imported)
            self.assertEqual(
                len(expected),
                built["documents"]["source"]["relation_count"],
            )
            receipt = built["documents"]["receipt"]
            self.assertEqual("gt6-canonical-v1", receipt["adapter_abi"])
            self.assertEqual(gt6.adapter_sha256(), receipt["adapter_sha256"])
            self.assertTrue(receipt["skip_is_not_pass"])
            lock = built["documents"]["lock_candidate"]
            self.assertFalse(lock["production_authority"])
            self.assertEqual("LOCK_CANDIDATE_DRAFT", lock["status"])

    def test_write_is_deterministic(self) -> None:
        if not SMELTER.is_file():
            self.skipTest("fixtures not written yet")
        first = source_import.import_documents(SMELTER)["documents"]
        second = source_import.import_documents(SMELTER)["documents"]
        self.assertEqual(first, second)

    def test_check_does_not_modify_files(self) -> None:
        if not SMELTER.is_file():
            self.skipTest("fixtures not written yet")
        source = SMELTER.parent / "source.json"
        if not source.is_file():
            self.skipTest("imported source not written yet")
        before = source.read_bytes()
        errors = source_import.check_import(SMELTER)
        self.assertEqual([], errors)
        self.assertEqual(before, source.read_bytes())


class GenericRecipeImportNegativeTest(unittest.TestCase):
    def test_unknown_field_fails_closed(self) -> None:
        if not SMELTER.is_file():
            self.skipTest("fixtures not written yet")
        document = census.load_json(SMELTER)
        document["python_delegate"] = "tools/build_t40_electrolyzer_source.py"
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "recipe_import.json"
            path.write_text(json.dumps(document), encoding="utf-8")
            with self.assertRaises(ImportSpecError):
                load_import_spec(path)

    def test_wrong_target_map_fails_closed(self) -> None:
        if not SMELTER.is_file():
            self.skipTest("fixtures not written yet")
        document = census.load_json(SMELTER)
        document["target_map"] = "cruciblecraft:not_a_map"
        document["host"] = "cruciblecraft:not_a_map"
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "recipe_import.json"
            path.write_text(json.dumps(document), encoding="utf-8")
            with self.assertRaises(ImportSpecError):
                load_import_spec(path)

    def test_path_escape_fails_closed(self) -> None:
        if not SMELTER.is_file():
            self.skipTest("fixtures not written yet")
        document = census.load_json(SMELTER)
        document["output_paths"]["source"] = "../secret/source.json"
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "recipe_import.json"
            path.write_text(json.dumps(document), encoding="utf-8")
            with self.assertRaises((ImportSpecError, SourceImportError)):
                load_import_spec(path)

    def test_unmapped_operand_is_blocked(self) -> None:
        if not SMELTER.is_file():
            self.skipTest("fixtures not written yet")
        dump_path = SMELTER.parent / "source_pack" / "dump_slice.json"
        dump = census.load_json(dump_path)
        dump["recipes"][0]["inputs"] = [
            {
                "count": 1,
                "displayName": "Unknown",
                "item": "unmappedmod:unknown_item",
                "meta": 0,
            }
        ]
        root = (
            census.ROOT
            / "src/test/resources/generic_recipe_import/_negative_unmapped"
        )
        try:
            self._write_mutated_pack(root, dump)
            spec = root / "recipe_import.json"
            built = source_import.import_documents(spec)
            family = built["documents"]["lock_candidate"]["families"][0]
            self.assertEqual("blocked", family["classification"])
            self.assertTrue(family["blocked"])
        finally:
            if root.exists():
                for path in sorted(root.rglob("*"), reverse=True):
                    if path.is_file():
                        path.unlink()
                    elif path.is_dir():
                        path.rmdir()
                root.rmdir()

    def _write_mutated_pack(self, root: Path, dump: dict) -> None:
        from tools.recipe_bulk.source_pack import sha256_file

        pack = root / "source_pack"
        pack.mkdir(parents=True)
        dump_path = pack / "dump_slice.json"
        work_src = SMELTER.parent / "source_pack" / "work_set.json"
        work_path = pack / "work_set.json"
        census.write_stable(dump_path, dump)
        census.write_stable(work_path, census.load_json(work_src))
        corpus_src = SMELTER.parent / "compare_corpus.json"
        corpus_path = root / "compare_corpus.json"
        census.write_stable(corpus_path, census.load_json(corpus_src))
        manifest = {
            "files": [
                {
                    "path": census.relative(dump_path).replace("\\", "/"),
                    "role": "dump_slice",
                    "sha256": sha256_file(dump_path),
                },
                {
                    "path": census.relative(work_path).replace("\\", "/"),
                    "role": "work_set",
                    "sha256": sha256_file(work_path),
                },
            ],
            "full_replay": {
                "required_for_first_generation": True,
                "skip_is_not_pass": True,
            },
            "provenance_policy": {"append_only": True, "forbid_gt6u": True},
            "schema_version": 1,
            "source_dialect": "gt6",
            "source_pack_id": "generic-import/smelter-exact-singleton",
            "source_revision": census.SOURCE_REVISION,
            "source_system": "gt6",
        }
        manifest_path = root / "source_pack_manifest.json"
        census.write_stable(manifest_path, manifest)
        spec = census.load_json(SMELTER)
        spec["import_slug"] = "generic-import/negative-unmapped"
        spec["source_pack"] = census.relative(manifest_path).replace("\\", "/")
        spec["family_membership_source"]["path"] = census.relative(work_path).replace(
            "\\", "/"
        )
        spec["output_paths"] = {
            "lock_candidate": census.relative(root / "lock_candidate.json").replace(
                "\\", "/"
            ),
            "receipt": census.relative(root / "source_receipt.json").replace("\\", "/"),
            "review": census.relative(root / "source_review.json").replace("\\", "/"),
            "source": census.relative(root / "source.json").replace("\\", "/"),
        }
        census.write_stable(root / "recipe_import.json", spec)


class GenericRecipeImportCurrentnessTest(unittest.TestCase):
    def test_spec_hash_is_bound_in_receipt(self) -> None:
        if not SMELTER.is_file():
            self.skipTest("fixtures not written yet")
        from tools.recipe_bulk.source_pack import sha256_file

        built = source_import.import_documents(SMELTER)
        receipt = built["documents"]["receipt"]
        self.assertEqual(sha256_file(SMELTER), receipt["spec_sha256"])
        self.assertEqual(gt6.adapter_sha256(), receipt["adapter_sha256"])
        self.assertNotEqual(receipt["spec_sha256"], "0" * 64)
