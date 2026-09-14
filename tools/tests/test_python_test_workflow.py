from __future__ import annotations

import copy
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tools import run_python_tests as workflow


class PythonTestWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = workflow.load_policy()
        cls.cases = workflow.discover_cases(policy=cls.policy)

    def test_active_modules_match_profiles_exactly(self) -> None:
        declared = workflow.active_module_names(self.policy)
        profiled = workflow.profile_test_modules()
        self.assertEqual(set(profiled), set(declared))
        self.assertLess(len(declared), 75)
        self.assertEqual(
            set(declared),
            {workflow.test_module(case) for case in self.cases},
        )

    def test_loading_never_uses_directory_discovery(self) -> None:
        with mock.patch.object(
            workflow.unittest.defaultTestLoader,
            "discover",
            side_effect=AssertionError("historical directory discovery is forbidden"),
        ):
            cases = workflow.discover_cases(
                ["test_registry_identity"],
                policy=self.policy,
            )
        self.assertEqual(
            {"test_registry_identity"},
            {workflow.test_module(case) for case in cases},
        )

    def test_retired_test_cases_are_not_active(self) -> None:
        lowered = [workflow.test_module(case).lower() for case in self.cases]
        for token in ("currentness", "closeout", "seal", "snapshot", "archive"):
            self.assertFalse(
                any(token in test_id for test_id in lowered),
                token,
            )

    def test_inactive_module_is_rejected_before_import(self) -> None:
        with mock.patch.object(
            workflow.unittest.defaultTestLoader,
            "loadTestsFromNames",
        ) as loader:
            with self.assertRaisesRegex(
                workflow.PolicyError,
                "will not be imported",
            ):
                workflow.discover_cases(
                    ["test_build_t" + "46_work_set"],
                    policy=self.policy,
                )
        loader.assert_not_called()

    def test_modules_suite_stays_within_requested_active_module(self) -> None:
        cases = workflow.discover_cases(
            ["test_capability_ledger"],
            policy=self.policy,
        )
        selection = workflow.select_cases(
            "modules",
            self.policy,
            cases,
            modules=["test_capability_ledger"],
        )
        self.assertTrue(selection.cases)
        self.assertEqual(
            {"test_capability_ledger"},
            {workflow.test_module(case) for case in selection.cases},
        )

    def test_known_semantic_path_selects_declared_modules(self) -> None:
        paths = ["tools/build_semantic_recipes.py"]
        names, unmatched = workflow.affected_module_names(self.policy, paths)
        self.assertEqual((), unmatched)
        self.assertEqual(
            ("test_material_form_authority", "test_build_semantic_recipes"),
            names,
        )
        cases = workflow.discover_cases(names, policy=self.policy)
        selection = workflow.select_cases(
            "affected",
            self.policy,
            cases,
            changed_paths=paths,
        )
        self.assertEqual(
            set(names),
            {workflow.test_module(case) for case in selection.cases},
        )

    def test_recipe_bulk_path_selects_fresh_modules(self) -> None:
        paths = ["tools/recipe_bulk/compile.py"]
        names, unmatched = workflow.affected_module_names(self.policy, paths)
        self.assertEqual((), unmatched)
        self.assertEqual(
            (
                "test_material_form_authority",
                "test_build_semantic_recipes",
                "test_recipe_bulk",
                "test_generic_recipe_import",
                "test_recipe_fresh",
                "test_machine_delivery",
                "test_cluster_mill_prep",
                "test_prep_machines",
            ),
            names,
        )

    def test_changed_active_test_selects_only_its_declared_group(self) -> None:
        paths = ["tools/tests/test_python_test_workflow.py"]
        names, unmatched = workflow.affected_module_names(self.policy, paths)
        self.assertEqual((), unmatched)
        self.assertEqual(
            (
                "test_verification_profiles",
                "test_python_test_workflow",
                "test_check_no_workflow_hashes",
                "test_check_zero_milestone_names",
                "test_tree_compare",
            ),
            names,
        )

    def test_historical_test_path_is_unmatched(self) -> None:
        names, unmatched = workflow.affected_module_names(
            self.policy,
            ["tools/tests/test_build_t" + "46_work_set.py"],
        )
        self.assertEqual((), names)
        self.assertEqual(
            ("tools/tests/test_build_t" + "46_work_set.py",),
            unmatched,
        )

    def test_unknown_code_path_is_unmatched(self) -> None:
        names, unmatched = workflow.affected_module_names(
            self.policy,
            ["unexpected/new_domain.json"],
        )
        self.assertEqual((), names)
        self.assertEqual(("unexpected/new_domain.json",), unmatched)

    def test_runtime_java_path_is_owned_without_python_modules(self) -> None:
        names, unmatched = workflow.affected_module_names(
            self.policy,
            [
                "src/main/java/com/masson/cruciblecraft/content/block/HopperBlock.java",
            ],
        )
        self.assertEqual((), unmatched)
        self.assertEqual((), names)

    def test_markdown_path_selects_no_modules(self) -> None:
        names, unmatched = workflow.affected_module_names(
            self.policy,
            ["docs/current/verification.md"],
        )
        self.assertEqual((), unmatched)
        self.assertEqual((), names)

    def test_path_file_and_explicit_paths_are_merged(self) -> None:
        with tempfile.NamedTemporaryFile(
            mode="w",
            encoding="utf-8",
            suffix=".txt",
            delete=False,
        ) as handle:
            handle.write("# comment\n")
            handle.write("tools/build_capability_ledger.py\n")
            path_file = Path(handle.name)
        try:
            paths = workflow.resolve_suite_paths(
                suite="affected",
                path_args=["tools/build_registry_identity.py"],
                path_file=path_file,
            )
        finally:
            path_file.unlink(missing_ok=True)
        self.assertEqual(
            (
                "tools/build_capability_ledger.py",
                "tools/build_registry_identity.py",
            ),
            paths,
        )

    def test_policy_rejects_inactive_rule_module(self) -> None:
        changed = copy.deepcopy(self.policy)
        changed["affected_rules"][0]["test_modules"].append(
            "test_full_verification_report"
        )
        with self.assertRaisesRegex(
            workflow.PolicyError,
            "inactive test modules",
        ):
            workflow.validate_policy(changed)


if __name__ == "__main__":
    unittest.main()
