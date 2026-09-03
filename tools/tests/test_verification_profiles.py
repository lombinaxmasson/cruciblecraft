from __future__ import annotations

import argparse
import json
import re
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tools import verify as verify_entry

ROOT = Path(__file__).resolve().parents[2]
PROFILES = ROOT / "tools" / "verification_profiles.json"
BUILDER_POLICY = ROOT / "tools" / "verification_builder_policy.json"
SCHEMA = ROOT / "tools" / "verification_profiles.schema.json"

RETIRED_NAME = re.compile(
    r"(?:^|[_-])[tT]\d|card[-_]closeout|currentness|seal|"
    r"archive[-_]?inspect|snapshot|resume"
)


def nested_keys(value: object) -> list[str]:
    if isinstance(value, dict):
        keys = list(value)
        for child in value.values():
            keys.extend(nested_keys(child))
        return keys
    if isinstance(value, list):
        keys: list[str] = []
        for child in value:
            keys.extend(nested_keys(child))
        return keys
    return []


class VerificationProfileTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.profiles = json.loads(PROFILES.read_text(encoding="utf-8"))
        cls.builder_policy = json.loads(
            BUILDER_POLICY.read_text(encoding="utf-8")
        )
        cls.schema = json.loads(SCHEMA.read_text(encoding="utf-8"))

    def test_schema_declares_active_and_release_profiles(self) -> None:
        self.assertEqual(2, self.profiles["schema_version"])
        self.assertEqual(2, self.schema["properties"]["schema_version"]["const"])
        self.assertEqual("report", self.profiles["unmatched_policy"])
        active = self.profiles["active_profiles"]
        release = self.profiles["release_profiles"]
        self.assertEqual(len(active), len(set(active)))
        self.assertTrue(set(release) <= set(active))
        self.assertEqual(set(active), set(self.profiles["profiles"]))

    def test_builder_policy_and_profiles_agree_exactly(self) -> None:
        verify_entry.validate_configuration(
            self.profiles,
            self.builder_policy,
        )
        owned = {
            builder
            for name in self.profiles["active_profiles"]
            for builder in self.profiles["profiles"][name]["builders"]
        }
        policy = {row["name"] for row in self.builder_policy["builders"]}
        self.assertEqual(policy, owned)

    def test_active_discovery_has_no_retired_names(self) -> None:
        for name in self.profiles["active_profiles"]:
            self.assertIsNone(RETIRED_NAME.search(name), name)
            profile = self.profiles["profiles"][name]
            for builder in profile["builders"]:
                self.assertIsNone(RETIRED_NAME.search(builder), builder)
            for module in profile["python_modules"]:
                self.assertIsNone(RETIRED_NAME.search(module), module)
        for row in self.builder_policy["builders"]:
            self.assertIsNone(RETIRED_NAME.search(row["name"]), row["name"])
            self.assertIsNone(RETIRED_NAME.search(row["script"]), row["script"])

    def test_only_fresh_command_surface_is_exposed(self) -> None:
        parser = verify_entry.build_parser()
        subparsers = next(
            action
            for action in parser._actions
            if isinstance(action, argparse._SubParsersAction)
        )
        self.assertEqual(
            {"dev", "integration", "release"},
            set(subparsers.choices),
        )

    def test_markdown_change_selects_docs_profile_only(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            ["docs/current/roadmap.md", "README.md"],
        )
        self.assertEqual(["docs"], classified["selected_profiles"])
        self.assertEqual([], classified["unmatched_paths"])

    def test_unknown_path_is_reported(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            ["unexpected/new_domain.json"],
        )
        self.assertEqual([], classified["selected_profiles"])
        self.assertEqual(
            ["unexpected/new_domain.json"],
            classified["unmatched_paths"],
        )

    def test_capability_paths_select_capability_profiles(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            [
                "tools/capability_ledger.py",
                "tools/player_complete.py",
            ],
        )
        self.assertEqual(
            ["capability-runtime", "player-complete"],
            classified["selected_profiles"],
        )

    def test_release_is_fresh_profile_execution(self) -> None:
        receipt = {"profiles": [], "commands": [], "results": []}
        calls: list[str] = []

        def record(
            name: str,
            _profiles: dict[str, object],
            _builders: dict[str, object],
            _receipt: dict[str, object],
        ) -> int:
            calls.append(name)
            return 0

        with mock.patch.object(
            verify_entry,
            "_configuration",
            return_value=(self.profiles, self.builder_policy),
        ), mock.patch.object(verify_entry, "run_profile", side_effect=record):
            code = verify_entry.cmd_release(argparse.Namespace(), receipt)
        self.assertEqual(0, code)
        self.assertEqual(self.profiles["release_profiles"], calls)

    def test_latest_report_has_fresh_execution_fields_only(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            latest = Path(temporary) / "latest.json"
            with mock.patch.object(verify_entry, "LATEST", latest), mock.patch.object(
                verify_entry, "git_revision", return_value="revision"
            ), mock.patch.object(
                verify_entry,
                "git_dirty_paths",
                return_value=("tools/verify.py",),
            ):
                receipt = verify_entry._receipt(
                    ["integration", "--profile", "verification"]
                )
                receipt["commands"].append(
                    {"name": "python:verification", "argv": ["python", "-m", "test"]}
                )
                receipt["results"].append(
                    {
                        "name": "python:verification",
                        "exit_code": 0,
                        "status": "PASS",
                    }
                )
                receipt["status"] = "PASS"
                verify_entry.write_latest_receipt(receipt)
            saved = json.loads(latest.read_text(encoding="utf-8"))
        self.assertEqual("revision", saved["git_revision"])
        self.assertEqual(["tools/verify.py"], saved["dirty_paths"])
        self.assertTrue(saved["commands"])
        self.assertTrue(saved["results"])
        self.assertTrue(saved["environment"])
        forbidden_fragment = "dig" + "est"
        self.assertFalse(
            any(forbidden_fragment in key.lower() for key in nested_keys(saved))
        )

    def test_player_complete_builder_runs_both_runtimes_fresh(self) -> None:
        row = next(
            row
            for row in self.builder_policy["builders"]
            if row["name"] == "build_player_complete"
        )
        self.assertEqual("tools/build_player_complete.py", row["script"])
        self.assertEqual(
            [
                "--run",
                "--capability",
                "logistics/fluid-network/basic-transfer",
            ],
            row["ordinary_args"],
        )
        self.assertEqual([], row.get("environment_args") or [])
        self.assertEqual(
            [
                verify_entry.sys.executable,
                "tools/build_player_complete.py",
                "--run",
                "--capability",
                "logistics/fluid-network/basic-transfer",
            ],
            verify_entry.builder_command(row),
        )

    def test_semantic_profile_requires_datagen_and_junit(self) -> None:
        profile = self.profiles["profiles"]["semantic-generators"]
        self.assertIs(True, profile["datagen"])
        self.assertIn("test", profile["gradle_tasks"])
        self.assertIn("player-complete", self.profiles["release_profiles"])


if __name__ == "__main__":
    unittest.main()
