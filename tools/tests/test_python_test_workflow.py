from __future__ import annotations

import copy
import unittest

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


if __name__ == "__main__":
    unittest.main()
