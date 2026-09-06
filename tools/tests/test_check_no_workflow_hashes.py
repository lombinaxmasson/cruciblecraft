"""Contract tests for the no-development-workflow-hash boundary."""
from __future__ import annotations

import contextlib
import copy
import io
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import check_no_workflow_hashes as scanner  # noqa: E402


class NoWorkflowHashesTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy_path = ROOT / "tools" / "workflow_hash_policy.json"
        cls.policy = scanner.load_policy(cls.policy_path)

    def test_disallowed_workflow_fields_and_apis_are_actionable(self) -> None:
        cases = [
            ("tools/build_demo.py", 'row = {"input_sha256": digest}', "sha"),
            ("tools/demo.json", '"currentness": {"owned_inputs": {}}', "currentness"),
            (
                "tools/demo.schema.json",
                '"builder_sha256": {"type": "string"}',
                "sha256",
            ),
            (
                "docs/current/verification.md",
                "Call closeout_seal.load_seal(card) before continuing.",
                "closeout_seal",
            ),
            (
                ".github/workflows/verify.yml",
                'seal_sha256: "${{ steps.seal.outputs.sha }}"',
                "seal_sha256",
            ),
        ]
        for relative, text, expected in cases:
            with self.subTest(relative=relative):
                findings = scanner.scan_text(relative, f"safe\n{text}\n", self.policy)
                self.assertTrue(findings, relative)
                row = findings[0]
                self.assertEqual(relative, row["path"])
                self.assertEqual(2, row["line"])
                self.assertGreater(row["column"], 0)
                self.assertIn(expected, row["token"].lower())

    def test_permitted_source_release_runtime_and_archive_hashes_pass(self) -> None:
        cases = [
            (
                "archive/sealed/legacy/closeout_seal.json",
                '"input_sha256": "abc"',
            ),
            (
                "docs/history/work-logs/legacy.md",
                "currentness and closeout_seal were historical workflow state",
            ),
            (
                "tools/legacy_verification_index.json",
                '"paths": ["tools/old.currentness.json", "closeout_seal.json"]',
            ),
            (
                "tools/recipe_bulk/source_pack.py",
                "return hashlib.sha256(path.read_bytes()).hexdigest()",
            ),
            (
                "tools/source_pack_manifest.schema.json",
                '"sha256": {"type": "string"}',
            ),
            (
                "src/test/resources/generic_recipe_import/example/"
                "source_pack_manifest.json",
                '"sha256": "abc"',
            ),
            (
                "tools/external_source_manifest.schema.json",
                '"source_sha256": {"type": "string"}',
            ),
            (
                "tools/t31_release_manifest.json",
                '"artifact_checksum": "abc"',
            ),
            (
                "src/main/java/com/masson/cruciblecraft/material/"
                "MaterialFingerprint.java",
                "String protocol_sha256 = cacheKey();",
            ),
            (
                "src/main/resources/data/cruciblecraft/schema/"
                "compact_publication_policy.schema.json",
                '"membership_root_sha256": {"type": "string"}',
            ),
            (
                ".github/workflows/build.yml",
                "run: python3 tools/check_no_workflow_hashes.py --check",
            ),
            (
                "src/compact_recipe_policy_generated/resources/data/"
                "cruciblecraft/recipe/publication/policy.json",
                '"content_sha256": "abc"',
            ),
            (
                "tools/block_art.py",
                'row["sha256"] = census.sha256_file(dest)',
            ),
            (
                "src/test/resources/assembler_wood_catalog_fixture/"
                "data/cruciblecraft/recipe/assembler/demo.json",
                '"evidence_hashes": ["abc"]',
            ),
        ]
        for relative, text in cases:
            with self.subTest(relative=relative):
                self.assertEqual(
                    [], scanner.scan_text(relative, text, self.policy), relative
                )

    def test_reachability_is_derived_from_all_three_policies(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)

            def write_json(relative: str, value: object) -> None:
                path = root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(json.dumps(value), encoding="utf-8")

            def write_text(relative: str) -> None:
                path = root / relative
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("currentness = True\n", encoding="utf-8")

            write_json(
                "tools/verification_profiles.json",
                {
                    "documentation_path_patterns": ["docs/current/**"],
                    "profiles": {
                        "active": {
                            "owned_paths": ["tools/profile_owned.py"],
                            "builders": ["build_demo"],
                            "python_modules": ["test_profile_reachable"],
                        },
                        "archive": {
                            "owned_paths": ["tools/archive_only.py"],
                            "builders": ["build_archive"],
                            "python_modules": [],
                        },
                    },
                },
            )
            write_json(
                "tools/verification_builder_policy.json",
                {
                    "pre_chain_builders": [
                        {
                            "name": "preflight",
                            "script": "tools/preflight.py",
                            "outputs": ["preflight.json"],
                        }
                    ],
                    "builders": [
                        {
                            "name": "build_demo",
                            "script": "tools/build_demo.py",
                            "outputs": ["demo.json"],
                        },
                        {
                            "name": "build_archive",
                            "script": "tools/build_archive.py",
                            "outputs": ["archive.json"],
                        },
                    ],
                },
            )
            write_json(
                "tools/python_test_policy.json",
                {
                    "documentation_path_patterns": ["README.md"],
                    "affected_rules": [
                        {
                            "paths": ["tools/from_python_policy.py"],
                            "test_modules": ["test_policy_reachable"],
                        }
                    ],
                },
            )
            expected = {
                "tools/build_demo.py",
                "tools/demo.json",
                "tools/from_python_policy.py",
                "tools/preflight.json",
                "tools/preflight.py",
                "tools/profile_owned.py",
                "tools/tests/test_policy_reachable.py",
                "tools/tests/test_profile_reachable.py",
            }
            for relative in expected | {
                "tools/archive.json",
                "tools/archive_only.py",
                "tools/build_archive.py",
            }:
                write_text(relative)

            policy = copy.deepcopy(self.policy)
            policy["always_active_patterns"] = []
            active = {
                path.relative_to(root).as_posix()
                for path in scanner.iter_active_files(root, policy)
            }
            self.assertEqual(expected, active)

    def test_migration_mode_reports_but_check_mode_fails(self) -> None:
        result = {
            "mode": "migration-report",
            "active_files": 1,
            "finding_count": 1,
            "findings": [
                {
                    "path": "tools/build_demo.py",
                    "line": 7,
                    "column": 10,
                    "token": "input_sha256",
                    "rule": "workflow_sha_field_or_api",
                    "text": '"input_sha256": digest',
                }
            ],
        }
        with mock.patch.object(scanner, "scan_repository", return_value=result):
            migration_output = io.StringIO()
            with contextlib.redirect_stdout(migration_output):
                migration_code = scanner.main(
                    ["--policy", str(self.policy_path), "--migration-report"]
                )
            check_output = io.StringIO()
            with contextlib.redirect_stdout(check_output):
                check_code = scanner.main(
                    ["--policy", str(self.policy_path), "--check"]
                )
        self.assertEqual(0, migration_code)
        self.assertEqual(1, check_code)
        self.assertIn("tools/build_demo.py:7:10", migration_output.getvalue())
        self.assertIn("non-blocking", migration_output.getvalue())
        self.assertIn("blocking", check_output.getvalue())

    def test_scoped_scan_only_covers_requested_active_files(self) -> None:
        result = scanner.scan_repository(
            ROOT,
            self.policy,
            paths=["tools/tree_compare.py", "README.md"],
        )
        self.assertTrue(result["scoped"])
        self.assertEqual(1, result["active_files"])
        self.assertTrue(
            all(row["path"] == "tools/tree_compare.py" for row in result["findings"])
        )

    def test_regex_patterns_are_compiled_once_per_policy(self) -> None:
        scanners = scanner.compile_scanners(self.policy)
        self.assertEqual(3, len(scanners["forbidden"]))
        again = scanner.compile_scanners(self.policy)
        self.assertEqual(
            [pattern.pattern for _rule, pattern in scanners["forbidden"]],
            [pattern.pattern for _rule, pattern in again["forbidden"]],
        )

    def test_incremental_path_scan_skips_unlisted_active_files(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            dirty = root / "tools" / "profile_owned.py"
            clean = root / "tools" / "from_python_policy.py"
            dirty.parent.mkdir(parents=True)
            dirty.write_text('row = {"input_sha256": "abc"}\n', encoding="utf-8")
            clean.write_text('row = {"input_sha256": "abc"}\n', encoding="utf-8")
            (root / "tools" / "verification_profiles.json").write_text(
                json.dumps(
                    {
                        "profiles": {
                            "active": {
                                "owned_paths": [
                                    "tools/profile_owned.py",
                                    "tools/from_python_policy.py",
                                ],
                                "builders": [],
                                "python_modules": [],
                            }
                        }
                    }
                ),
                encoding="utf-8",
            )
            (root / "tools" / "verification_builder_policy.json").write_text(
                json.dumps({"pre_chain_builders": [], "builders": []}),
                encoding="utf-8",
            )
            (root / "tools" / "python_test_policy.json").write_text(
                json.dumps({"affected_rules": []}),
                encoding="utf-8",
            )
            policy = copy.deepcopy(self.policy)
            policy["always_active_patterns"] = []
            result = scanner.scan_repository(
                root,
                policy,
                paths=["tools/profile_owned.py"],
            )
        self.assertTrue(result["scoped"])
        self.assertEqual(1, result["active_files"])
        self.assertEqual(["tools/profile_owned.py"], [row["path"] for row in result["findings"]])

    def test_compile_scanners_reuses_regex_objects(self) -> None:
        compiled = scanner.compile_scanners(self.policy)
        self.assertTrue(compiled["forbidden"])
        self.assertTrue(all(row[1].pattern for row in compiled["forbidden"]))


if __name__ == "__main__":
    unittest.main()
