from __future__ import annotations

import argparse
import json
import os
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
            {"dev", "integration", "promotion", "release"},
            set(subparsers.choices),
        )

    def test_markdown_change_selects_no_profile(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            ["docs/current/roadmap.md", "README.md"],
        )
        self.assertEqual([], classified["selected_profiles"])
        self.assertEqual([], classified["unmatched_paths"])
        self.assertEqual(
            ["README.md", "docs/current/roadmap.md"],
            classified["documentation_paths"],
        )

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

    def test_local_run_tree_is_not_unmatched(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            [
                "run-game-test-filtered/config/cruciblecraft/"
                ".generated-material-pack/server/data/c/tags/block/cables.json",
                "run/saves/world/level.dat",
            ],
        )
        self.assertEqual([], classified["selected_profiles"])
        self.assertEqual([], classified["unmatched_paths"])

    def test_local_run_tree_does_not_block_owned_paths(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            [
                "tools/verify.py",
                "run-game-test-filtered/config/cruciblecraft/"
                ".generated-material-pack/server/.cruciblecraft-manifest.json",
            ],
        )
        self.assertEqual(["verification"], classified["selected_profiles"])
        self.assertEqual([], classified["unmatched_paths"])

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
            **_kwargs: object,
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

    def test_player_complete_builder_runs_game_test_fresh(self) -> None:
        row = next(
            row
            for row in self.builder_policy["builders"]
            if row["name"] == "build_player_complete"
        )
        self.assertEqual("tools/build_player_complete.py", row["script"])
        self.assertEqual(["--run", "--all"], row["ordinary_args"])
        self.assertEqual([], row.get("environment_args") or [])
        self.assertEqual(
            [
                verify_entry.sys.executable,
                "tools/build_player_complete.py",
                "--run",
                "--all",
            ],
            verify_entry.builder_command(row),
        )
        names = [item["name"] for item in self.builder_policy["builders"]]
        self.assertNotIn("build_player_complete_item", names)
        self.assertEqual(
            ["build_player_complete"],
            self.profiles["profiles"]["player-complete"]["builders"],
        )
        self.assertIn(
            "tools/capabilities/**",
            self.profiles["profiles"]["player-complete"]["owned_paths"],
        )

    def test_semantic_profile_requires_datagen_and_junit(self) -> None:
        semantic = self.profiles["profiles"]["semantic-generators"]
        recipes = self.profiles["profiles"]["recipe-generators"]
        runtime = self.profiles["profiles"]["runtime-java"]
        self.assertIs(True, semantic["datagen"])
        self.assertNotIn("test", semantic["gradle_tasks"])
        self.assertIs(False, recipes["datagen"])
        self.assertNotIn("test", recipes["gradle_tasks"])
        self.assertIs(False, runtime["datagen"])
        self.assertIn("test", runtime["gradle_tasks"])
        self.assertIn("runtime-java", self.profiles["release_profiles"])
        self.assertIn("player-complete", self.profiles["release_profiles"])
        self.assertIn("recipe-generators", self.profiles["release_profiles"])
        fresh = self.profiles["profiles"]["recipes"]
        self.assertIn("recipes", self.profiles["active_profiles"])
        self.assertNotIn("recipes", self.profiles["release_profiles"])
        self.assertIs(False, fresh["datagen"])
        self.assertEqual([], fresh["gradle_tasks"])
        self.assertEqual(["build_recipe_fresh"], fresh["builders"])
        self.assertNotIn(
            "src/recipe_generated/**",
            fresh["owned_paths"],
        )

    def test_runtime_java_does_not_select_datagen(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            [
                "src/main/java/com/masson/cruciblecraft/content/block/HopperBlock.java",
            ],
        )
        self.assertEqual(["runtime-java"], classified["selected_profiles"])
        self.assertEqual([], classified["unmatched_paths"])

    def test_datagen_provider_selects_runtime_java_and_semantic_generators(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            [
                "src/main/java/com/masson/cruciblecraft/datagen/ModLanguageProvider.java",
            ],
        )
        self.assertEqual(
            ["runtime-java", "semantic-generators"],
            classified["selected_profiles"],
        )

    def test_generated_resource_selects_semantic_generators_only(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            ["src/generated/resources/data/cruciblecraft/recipe/machines/hopper.json"],
        )
        self.assertEqual(["semantic-generators"], classified["selected_profiles"])
        self.assertEqual([], classified["unmatched_paths"])

    def test_recipe_generated_selects_recipe_generators_only(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            [
                "src/recipe_generated/resources/data/cruciblecraft/recipe/"
                "smelter/ordinary_closure/acquisition/gt_recipe_smelter_0099.json"
            ],
        )
        self.assertEqual(["recipe-generators"], classified["selected_profiles"])
        self.assertEqual([], classified["unmatched_paths"])

    def test_recipe_bulk_tooling_selects_recipe_generators_and_recipes(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            ["tools/recipe_bulk/compile.py"],
        )
        self.assertEqual(
            ["recipe-generators", "recipes"],
            classified["selected_profiles"],
        )

    def test_if_changed_skips_when_diff_does_not_own_the_profile(self) -> None:
        receipt = {"profiles": [], "commands": [], "results": []}
        with mock.patch.object(
            verify_entry,
            "_configuration",
            return_value=(self.profiles, self.builder_policy),
        ), mock.patch.object(
            verify_entry, "has_external_diff_base", return_value=True
        ), mock.patch.object(
            verify_entry,
            "changed_paths",
            return_value=[
                "src/main/java/com/masson/cruciblecraft/content/block/HopperBlock.java",
            ],
        ), mock.patch.object(verify_entry, "run_profile") as runner:
            code = verify_entry.cmd_integration(
                argparse.Namespace(
                    profile="semantic-generators",
                    if_changed=True,
                ),
                receipt,
            )
        self.assertEqual(0, code)
        runner.assert_not_called()
        self.assertEqual("SKIP", receipt["profiles"][0]["status"])

    def test_if_changed_runs_when_an_owned_path_changed(self) -> None:
        receipt = {"profiles": [], "commands": [], "results": []}
        with mock.patch.object(
            verify_entry,
            "_configuration",
            return_value=(self.profiles, self.builder_policy),
        ), mock.patch.object(
            verify_entry, "has_external_diff_base", return_value=True
        ), mock.patch.object(
            verify_entry,
            "changed_paths",
            return_value=[
                "src/main/java/com/masson/cruciblecraft/datagen/ModLanguageProvider.java",
            ],
        ), mock.patch.object(verify_entry, "run_profile", return_value=0) as runner:
            code = verify_entry.cmd_integration(
                argparse.Namespace(
                    profile="semantic-generators",
                    if_changed=True,
                ),
                receipt,
            )
        self.assertEqual(0, code)
        runner.assert_called_once()

    def test_if_changed_runs_when_no_diff_base(self) -> None:
        receipt = {"profiles": [], "commands": [], "results": []}
        with mock.patch.object(
            verify_entry,
            "_configuration",
            return_value=(self.profiles, self.builder_policy),
        ), mock.patch.object(
            verify_entry, "has_external_diff_base", return_value=False
        ), mock.patch.object(
            verify_entry, "changed_paths"
        ) as changed, mock.patch.object(
            verify_entry, "run_profile", return_value=0
        ) as runner:
            code = verify_entry.cmd_integration(
                argparse.Namespace(
                    profile="semantic-generators",
                    if_changed=True,
                ),
                receipt,
            )
        self.assertEqual(0, code)
        changed.assert_not_called()
        runner.assert_called_once()

    def test_if_changed_skips_runtime_java_for_generated_resources(self) -> None:
        receipt = {"profiles": [], "commands": [], "results": []}
        with mock.patch.object(
            verify_entry,
            "_configuration",
            return_value=(self.profiles, self.builder_policy),
        ), mock.patch.object(
            verify_entry, "has_external_diff_base", return_value=True
        ), mock.patch.object(
            verify_entry,
            "changed_paths",
            return_value=[
                "src/generated/resources/data/cruciblecraft/recipe/machines/hopper.json",
            ],
        ), mock.patch.object(verify_entry, "run_profile") as runner:
            code = verify_entry.cmd_integration(
                argparse.Namespace(
                    profile="runtime-java",
                    if_changed=True,
                ),
                receipt,
            )
        self.assertEqual(0, code)
        runner.assert_not_called()
        self.assertEqual("SKIP", receipt["profiles"][0]["status"])

    def test_gradle_files_select_runtime_java_and_semantic_generators(self) -> None:
        classified = verify_entry.classify_paths(
            self.profiles,
            ["build.gradle", "gradle.properties"],
        )
        self.assertEqual(
            ["runtime-java", "semantic-generators"],
            classified["selected_profiles"],
        )

    def test_ci_build_splits_runtime_java_and_datagen(self) -> None:
        text = (ROOT / ".github" / "workflows" / "build.yml").read_text(
            encoding="utf-8"
        )
        self.assertIn(
            "python3 tools/verify.py integration --profile runtime-java --if-changed",
            text,
        )
        self.assertIn(
            "python3 tools/verify.py integration --profile recipe-generators --if-changed",
            text,
        )
        self.assertIn(
            "python3 tools/verify.py integration --profile recipes --if-changed",
            text,
        )
        self.assertIn(
            "python3 tools/verify.py integration --profile semantic-generators --if-changed",
            text,
        )

    def test_gradle_command_reruns_only_the_requested_task(self) -> None:
        os.environ.pop("CRUCIBLECRAFT_GRADLE_ISOLATED", None)
        command = verify_entry.gradle_command("test")
        self.assertIn("--rerun", command)
        self.assertNotIn("--rerun-tasks", command)
        self.assertNotIn("--no-daemon", command)

    def test_release_isolates_the_gradle_daemon(self) -> None:
        previous = os.environ.get("CRUCIBLECRAFT_GRADLE_ISOLATED")
        os.environ["CRUCIBLECRAFT_GRADLE_ISOLATED"] = "1"
        try:
            command = verify_entry.gradle_command("test")
        finally:
            if previous is None:
                os.environ.pop("CRUCIBLECRAFT_GRADLE_ISOLATED", None)
            else:
                os.environ["CRUCIBLECRAFT_GRADLE_ISOLATED"] = previous
        self.assertIn("--no-daemon", command)
        self.assertIn("--rerun", command)

    def test_unmatched_paths_do_not_fail_dev(self) -> None:
        receipt = {"profiles": [], "commands": [], "results": []}
        calls: list[str] = []

        def record(
            name: str,
            _profiles: dict[str, object],
            _builders: dict[str, object],
            _receipt: dict[str, object],
            **_kwargs: object,
        ) -> int:
            calls.append(name)
            return 0

        with mock.patch.object(
            verify_entry,
            "_configuration",
            return_value=(self.profiles, self.builder_policy),
        ), mock.patch.object(
            verify_entry,
            "changed_paths",
            return_value=[
                "docs/current/roadmap.md",
                ".gitignore",
                "tools/emit_fission_hot_fluids.py",
            ],
        ), mock.patch.object(verify_entry, "run_profile", side_effect=record):
            code = verify_entry.cmd_dev(argparse.Namespace(path=[]), receipt)
        self.assertEqual(0, code)
        self.assertEqual([], calls)

    def test_verification_profile_has_no_global_text_scanners(self) -> None:
        verification = self.profiles["profiles"]["verification"]
        self.assertEqual([], verification["builders"])
        policy_names = {row["name"] for row in self.builder_policy["builders"]}
        self.assertNotIn("check_no_workflow_hashes", policy_names)
        self.assertNotIn("check_zero_milestone_names", policy_names)
        self.assertNotIn("build_semantic_recipes", policy_names)
        self.assertEqual(
            ["material_form_authority", "build_component_rules"],
            self.profiles["profiles"]["recipe-generators"]["builders"],
        )

    def test_docs_change_selects_no_verification_python_modules(self) -> None:
        verification = self.profiles["profiles"]["verification"]
        modules = verify_entry.python_modules_for_changed_paths(
            verification,
            ["docs/current/roadmap.md"],
        )
        self.assertEqual([], modules)
        self.assertNotIn("docs", self.profiles["profiles"])

    def test_promotion_runs_player_complete_only_for_promoted_slugs(self) -> None:
        receipt = {"profiles": [], "commands": [], "results": []}
        calls: list[list[str]] = []

        def record(name: str, command: list[str], _receipt: dict[str, object]) -> int:
            calls.append(command)
            return 0

        with mock.patch.object(
            verify_entry.capability_ledger,
            "player_complete_promotions",
            return_value=["logistics/fluid-network/basic-transfer"],
        ), mock.patch.object(verify_entry, "run_command", side_effect=record):
            code = verify_entry.cmd_promotion(
                argparse.Namespace(base="origin/main"),
                receipt,
            )
        self.assertEqual(0, code)
        self.assertEqual(
            ["logistics/fluid-network/basic-transfer"],
            receipt["promotion"]["slugs"],
        )
        self.assertEqual(1, len(calls))
        self.assertIn("tools/build_player_complete.py", calls[0])
        self.assertIn("--run", calls[0])
        self.assertIn("--client", calls[0])

    def test_promotion_skips_when_maturity_did_not_change(self) -> None:
        receipt = {"profiles": [], "commands": [], "results": []}
        with mock.patch.object(
            verify_entry.capability_ledger,
            "player_complete_promotions",
            return_value=[],
        ), mock.patch.object(verify_entry, "run_command") as runner:
            code = verify_entry.cmd_promotion(
                argparse.Namespace(base="HEAD"),
                receipt,
            )
        self.assertEqual(0, code)
        runner.assert_not_called()


if __name__ == "__main__":
    unittest.main()
