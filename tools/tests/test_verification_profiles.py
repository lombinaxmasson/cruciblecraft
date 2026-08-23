from __future__ import annotations

import json
import unittest
from pathlib import Path

from tools import verify as verify_entry
from tools import run_python_tests as workflow


ROOT = Path(__file__).resolve().parents[2]
PROFILES = ROOT / "tools" / "verification_profiles.json"
BUILDER_POLICY = ROOT / "tools" / "verification_builder_policy.json"
SCHEMA = ROOT / "tools" / "verification_profiles.schema.json"
NARRATIVE = ROOT / "tools" / "contracts" / "narrative_archive.json"
DEBT = ROOT / "tools" / "known_issues" / "verification-debt.json"


class VerificationProfileTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.profiles = json.loads(PROFILES.read_text(encoding="utf-8"))
        cls.builder_policy = json.loads(
            BUILDER_POLICY.read_text(encoding="utf-8")
        )

    def test_tooling_paths_exclude_narrative_markdown(self) -> None:
        from tools import verify_full_verification_report as report

        paths = [
            path.relative_to(ROOT).as_posix()
            for path in report.tooling_paths()
            if path.suffix.lower() == ".md"
        ]
        self.assertEqual([], paths)
        compact = set(report.CORE_ARTIFACTS)
        self.assertIn("gt6_recipe_expectations.json", compact)
        self.assertIn("t20_readiness.json", compact)
        self.assertIn("verification_builder_policy.json", compact)

    def test_schema_and_unmatched_policy_are_fail_closed(self) -> None:
        self.assertTrue(SCHEMA.is_file())
        self.assertEqual(1, self.profiles["schema_version"])
        self.assertEqual("report", self.profiles["unmatched_policy"])
        self.assertTrue(NARRATIVE.is_file())
        self.assertTrue(DEBT.is_file())
        debt = json.loads(DEBT.read_text(encoding="utf-8"))
        self.assertEqual("OPEN", debt["status"])
        self.assertTrue(debt["issues"])
        self.assertTrue(all(
            row["status"] in {"open", "closed"} for row in debt["issues"]
        ))
        self.assertTrue(all(
            row.get("resolved_by")
            for row in debt["issues"]
            if row["status"] == "closed"
        ))

    def test_every_builder_has_exactly_one_profile(self) -> None:
        owned: dict[str, str] = {}
        for name, profile in self.profiles["profiles"].items():
            for builder in profile["builders"]:
                self.assertNotIn(
                    builder,
                    owned,
                    f"{builder} is in both {owned.get(builder)} and {name}",
                )
                owned[builder] = name
        expected = {row["name"] for row in self.builder_policy["builders"]}
        self.assertEqual(expected, set(owned))

    def test_markdown_change_selects_docs_profile_only(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            ["docs/current/roadmap.md", "README.md"],
        )
        self.assertEqual(["docs"], classified["selected_profiles"])
        self.assertEqual([], classified["unmatched_paths"])

    def test_python_test_files_belong_to_verification_profile(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            ["tools/tests/test_python_test_workflow.py"],
        )
        self.assertIn("verification", classified["selected_profiles"])
        self.assertEqual([], classified["unmatched_paths"])

    def test_unknown_code_path_is_reported_not_escalated(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            ["unexpected/new_domain.json"],
        )
        self.assertEqual([], classified["selected_profiles"])
        self.assertEqual(
            ["unexpected/new_domain.json"],
            classified["unmatched_paths"],
        )
        selection = workflow.select_cases(
            "affected",
            workflow.load_policy(),
            workflow.discover_cases(),
            changed_paths=["unexpected/new_domain.json"],
        )
        self.assertFalse(selection.escalated_to_closure)
        self.assertEqual(
            ("unexpected/new_domain.json",),
            selection.unmatched_paths,
        )
        self.assertEqual((), selection.cases)

    def test_dev_command_exists_and_archive_inspect_is_readonly(self) -> None:
        self.assertEqual(0, verify_entry.main(["archive-inspect"]))
        self.assertEqual(2, verify_entry.main([
            "dev",
            "--path",
            "unexpected/new_domain.json",
        ]))

    def test_census_profile_selects_t35_builder_and_json_paths(self) -> None:
        t35_paths = [
            "tools/build_t35_census.py",
            "tools/build_t35_readiness.py",
            "tools/t35_census_policy.json",
            "tools/t35_readiness.json",
        ]
        classified = verify_entry.classify_paths(self.profiles, t35_paths)
        self.assertEqual(["census"], classified["selected_profiles"])
        self.assertEqual([], classified["unmatched_paths"])

    def test_census_profile_selects_t35_test_modules_with_verification(
        self,
    ) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            ["tools/tests/test_build_t35_census.py"],
        )
        self.assertEqual(
            ["census", "verification"],
            classified["selected_profiles"],
        )
        self.assertEqual([], classified["unmatched_paths"])

    def test_census_owned_paths_do_not_overlap_content_profiles(self) -> None:
        census = self.profiles["profiles"]["census"]
        content_profiles = {
            "materials",
            "recipes",
            "worldgen",
            "machines",
            "logistics",
            "presentation",
            "archive",
        }
        exclusive_samples = [
            "tools/build_t35_census_inputs.py",
            "tools/t35_census.json",
            "tools/t35_card_topology.json",
            "tools/tests/test_build_t35_readiness.py",
        ]
        for path in exclusive_samples:
            census_hits = any(
                verify_entry.path_matches(path, pattern)
                for pattern in census["owned_paths"]
            )
            other_hits = sorted(
                name
                for name, profile in self.profiles["profiles"].items()
                if name not in {"census", "verification"}
                and name in content_profiles
                and any(
                    verify_entry.path_matches(path, pattern)
                    for pattern in profile["owned_paths"]
                )
            )
            self.assertTrue(census_hits, f"{path} should match census")
            self.assertEqual(
                [],
                other_hits,
                f"{path} unexpectedly matched content profiles {other_hits}",
            )

    def test_census_profiles_split_compact_and_replay_builders(self) -> None:
        census = self.profiles["profiles"]["census"]
        self.assertEqual("T35", census["owner"])
        self.assertEqual("integration", census["tier"])
        self.assertFalse(census["datagen"])
        self.assertTrue(census["gametest"])
        expected_builders = {
            "build_t35_census_inputs",
            "build_t35_runtime_registry",
            "build_t35_excluded_object_reclaim",
            "build_t35_recipe_families_compact",
            "build_t35_machine_track",
            "build_t35_recipe_families",
            "build_t35_load_baseline",
            "build_t35_census",
            "build_t35_card_topology",
            "build_t35_readiness",
        }
        self.assertEqual(expected_builders, set(census["builders"]))
        policy_builders = {
            row["name"]
            for row in self.builder_policy["builders"]
            if row["name"] in expected_builders
        }
        self.assertEqual(expected_builders, policy_builders)
        replay = self.profiles["profiles"]["census-replay"]
        self.assertEqual(
            {
                "build_t36_census_delta",
                "build_t36_readiness",
                "build_t37_recipe_load_benchmark",
                "build_t37_census_delta",
                "build_t37_card_topology",
                "build_t37_readiness",
            },
            set(replay["builders"]),
        )
        self.assertFalse(replay["gametest"])
        self.assertEqual([], replay["gradle_tasks"])
