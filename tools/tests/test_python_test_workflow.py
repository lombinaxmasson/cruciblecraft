from __future__ import annotations

import copy
import unittest
from pathlib import Path
from unittest import mock

from tools import run_python_tests as workflow


class PythonTestWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = workflow.load_policy()
        cls.cases = workflow.discover_cases()

    def test_closure_contains_every_discovered_test_exactly_once(self) -> None:
        selection = workflow.select_cases(
            "closure",
            self.policy,
            self.cases,
        )
        discovered = [case.id() for case in self.cases]
        selected = [case.id() for case in selection.cases]
        self.assertEqual(discovered, selected)
        self.assertEqual(len(selected), len(set(selected)))

    def test_fast_excludes_closure_and_source_replay_tests(self) -> None:
        selection = workflow.select_cases("fast", self.policy, self.cases)
        for case in selection.cases:
            with self.subTest(test=case.id()):
                self.assertFalse(workflow.matches_any(
                    case.id(),
                    self.policy["closure_only_test_patterns"],
                ))
                self.assertFalse(workflow.matches_any(
                    case.id(),
                    self.policy["source_replay_test_patterns"],
                ))
        self.assertLess(len(selection.cases), len(self.cases))

    def test_known_affected_path_selects_declared_modules(self) -> None:
        selection = workflow.select_cases(
            "affected",
            self.policy,
            self.cases,
            changed_paths=["tools/compare_gt6_recipes.py"],
        )
        modules = {workflow.test_module(case) for case in selection.cases}
        self.assertFalse(selection.escalated_to_closure)
        self.assertEqual(
            {
                "test_compare_gt6_recipes",
                "test_build_gt6_ore_chain",
                "test_full_verification_report",
            },
            modules,
        )

    def test_changed_test_file_selects_its_own_module(self) -> None:
        selection = workflow.select_cases(
            "affected",
            self.policy,
            self.cases,
            changed_paths=["tools/tests/test_python_test_workflow.py"],
        )
        self.assertEqual(
            {"test_python_test_workflow"},
            {workflow.test_module(case) for case in selection.cases},
        )

    def test_java_sources_contain_no_crlf_line_endings(self) -> None:
        root = Path(__file__).resolve().parents[2]
        java_files = sorted((root / "src").rglob("*.java"))
        self.assertTrue(java_files)
        offenders = [
            path.relative_to(root).as_posix()
            for path in java_files
            if b"\r\n" in path.read_bytes()
        ]
        self.assertEqual([], offenders)

    def test_t16_artifacts_select_full_t16_closure_modules(self) -> None:
        selection = workflow.select_cases(
            "affected",
            self.policy,
            self.cases,
            changed_paths=[
                "tools/t16_load_projection_input.json",
                "src/main/resources/data/cruciblecraft/"
                "t16_publication_baseline.json",
            ],
        )
        self.assertFalse(selection.escalated_to_closure)
        self.assertEqual(
            {
                "test_build_processing_machine_energy_audit",
                "test_build_t16_machine_acquisition",
                "test_build_t16_machine_denominator",
                "test_build_t16_readiness",
                "test_full_verification_report",
                "test_recipe_load_projection",
            },
            {workflow.test_module(case) for case in selection.cases},
        )

    def test_unknown_affected_path_escalates_to_closure(self) -> None:
        selection = workflow.select_cases(
            "affected",
            self.policy,
            self.cases,
            changed_paths=["unexpected/new_domain.json"],
        )
        self.assertTrue(selection.escalated_to_closure)
        self.assertEqual(
            {case.id() for case in self.cases},
            {case.id() for case in selection.cases},
        )
        self.assertEqual(
            ("unexpected/new_domain.json",),
            selection.escalation_paths,
        )

    def test_policy_rejects_overlapping_slow_tiers(self) -> None:
        changed = copy.deepcopy(self.policy)
        changed["source_replay_test_patterns"].append(
            "test_full_verification_report.*"
        )
        with self.assertRaisesRegex(
            workflow.PolicyError,
            "both closure-only and source-replay",
        ):
            workflow.validate_policy(changed, self.cases)

    def test_prechecked_stage_skips_only_declared_positive_currentness(self) -> None:
        class SampleTest(unittest.TestCase):
            def test_currentness_duplicate(self) -> None:
                pass

            def test_mutation_still_runs(self) -> None:
                pass

        currentness = SampleTest("test_currentness_duplicate")
        mutation = SampleTest("test_mutation_still_runs")
        policy = {
            "prechecked_stage_test_patterns": {
                "CRUCIBLECRAFT_BUILDER_STAGE_PASSED": [
                    "*.test_currentness_duplicate"
                ]
            }
        }
        method = getattr(SampleTest, "test_currentness_duplicate")
        try:
            with mock.patch.dict(
                workflow.os.environ,
                {"CRUCIBLECRAFT_BUILDER_STAGE_PASSED": "1"},
                clear=False,
            ):
                workflow.apply_prechecked_stage_skips(
                    [currentness, mutation],
                    policy,
                )
            self.assertTrue(method.__unittest_skip__)
            self.assertFalse(
                getattr(
                    SampleTest.test_mutation_still_runs,
                    "__unittest_skip__",
                    False,
                )
            )
        finally:
            for attribute in ("__unittest_skip__", "__unittest_skip_why__"):
                if hasattr(method, attribute):
                    delattr(method, attribute)

    def test_source_replay_tests_are_skipped_outside_replay_suite(self) -> None:
        class SampleTest(unittest.TestCase):
            def test_raw_replay(self) -> None:
                pass

        case = SampleTest("test_raw_replay")
        method = SampleTest.test_raw_replay
        policy = {"source_replay_test_patterns": ["*.test_raw_replay"]}
        try:
            workflow.apply_source_replay_skips([case], policy, "closure")
            self.assertTrue(method.__unittest_skip__)
            delattr(method, "__unittest_skip__")
            delattr(method, "__unittest_skip_why__")
            workflow.apply_source_replay_skips(
                [case],
                policy,
                "source-replay",
            )
            self.assertFalse(
                getattr(method, "__unittest_skip__", False)
            )
        finally:
            for attribute in ("__unittest_skip__", "__unittest_skip_why__"):
                if hasattr(method, attribute):
                    delattr(method, attribute)

    def test_source_replay_commands_are_derived_from_builder_policy(self) -> None:
        records = workflow.source_replay_records(self.policy)
        commands = [record["command"] for record in records]
        self.assertIn(
            [
                "$PYTHON",
                "tools/build_t13_recipe_map_denominator.py",
                "--check",
                "--full-replay",
            ],
            commands,
        )
        self.assertEqual(
            1,
            sum(
                command[1] == "tools/compare_gt6_recipes.py"
                for command in commands
            ),
        )


if __name__ == "__main__":
    unittest.main()
