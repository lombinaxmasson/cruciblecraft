from __future__ import annotations

import copy
import json
import re
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tools import run_python_tests as workflow

ROOT = Path(__file__).resolve().parents[2]
UNIQUE_ACTIVE_WAVE = "tools/waves/worldgen/gt-stone-layer-rocks/"
UNIQUE_ACTIVE_WAVE_LEDGERS = (
    "tools/waves/worldgen/gt-stone-layer-rocks/denominator.json",
    "tools/waves/worldgen/gt-stone-layer-rocks/topology.json",
    "tools/waves/worldgen/gt-stone-layer-rocks/readiness.json",
    "tools/waves/worldgen/gt-stone-layer-rocks/production_lock.json",
)
CLOSED_WAVE_EVIDENCE_NAMES = frozenset(
    {
        "source.json",
        "dump_slice.json",
        "identity_ledger_delta.json",
        "equivalence.json",
        "player_path.json",
        "player_path_support.json",
        "layered_player_path.json",
        "shard_manifest.json",
        "census_delta.json",
        "load_projection.json",
        "load_projection_input.json",
        "compile_report.json",
    }
)
CLOSED_ROOT_EVIDENCE_KEEP = frozenset(
    {
        "gt6_pipe_source.json",
        "hopper_hopper_source_evidence.json",
    }
)
CLOSED_ROOT_EVIDENCE_EXACT = CLOSED_WAVE_EVIDENCE_NAMES | {"census.json"}
CLOSED_ROOT_EVIDENCE_SUFFIXES = (
    "_source.json",
    "_dump_slice.json",
    "_identity_ledger_delta.json",
    "_equivalence.json",
    "_player_path.json",
    "_player_path_support.json",
    "_layered_player_path.json",
    "_shard_manifest.json",
    "_census_delta.json",
    "_load_projection.json",
    "_load_projection_input.json",
    "_compile_report.json",
    "_source_receipt.json",
    "_source_review.json",
    "_source_pack_manifest.json",
    "_publication_delta.json",
    "_operand_runtime_map.json",
)


def is_closed_root_dump_evidence(name: str) -> bool:
    if name in CLOSED_ROOT_EVIDENCE_KEEP:
        return False
    if name in CLOSED_ROOT_EVIDENCE_EXACT:
        return True
    if name.endswith(".currentness.json"):
        return True
    return any(name.endswith(suffix) for suffix in CLOSED_ROOT_EVIDENCE_SUFFIXES)


class PythonTestWorkflowTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = workflow.load_policy()
        cls.cases = workflow.discover_cases(policy=cls.policy)

    def test_active_modules_match_profiles_exactly(self) -> None:
        declared = workflow.active_module_names(self.policy)
        profiled = workflow.profile_test_modules()
        self.assertEqual(set(profiled), set(declared))
        self.assertLess(len(declared), 80)
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

    def test_every_python_test_module_is_tiered(self) -> None:
        disk = {
            path.stem
            for path in (ROOT / "tools" / "tests").glob("test_*.py")
        }
        active = set(self.policy["active_test_modules"])
        tiers = self.policy["test_tiers"]
        manual = set(tiers["manual_replay"])
        historical = set(tiers["historical"])
        self.assertEqual(set(), (manual | historical) & active)
        self.assertEqual(set(), manual & historical)
        self.assertEqual(disk, active | manual | historical)

    def test_generated_resource_roots_match_gradle_source_sets(self) -> None:
        document = json.loads(
            (ROOT / "tools" / "generated_resource_roots.json").read_text(
                encoding="utf-8"
            )
        )
        gradle = (
            ROOT / "gradle" / "scripts" / "source-sets.gradle"
        ).read_text(encoding="utf-8")
        declared = [row["path"] for row in document["roots"]]
        wired = re.findall(r"^\s*srcDir\('([^']+)'\)", gradle, re.M)
        self.assertEqual(declared, wired)
        python_declared = [
            row["path"]
            for row in document["roots"]
            if row["owner"] == "python"
        ]
        on_disk = sorted(
            f"src/{path.name}/resources"
            for path in (ROOT / "src").glob("*_generated")
            if path.is_dir()
        )
        self.assertEqual(sorted(python_declared), on_disk)

    def test_closed_wave_evidence_stays_untracked(self) -> None:
        tracked = subprocess.check_output(
            ["git", "ls-files", "-z", "tools"],
            cwd=ROOT,
            text=True,
            encoding="utf-8",
        ).split("\0")
        tracked = [path.replace("\\", "/") for path in tracked if path]
        leaked = []
        for path in tracked:
            if path.startswith(UNIQUE_ACTIVE_WAVE):
                continue
            name = path.rsplit("/", 1)[-1]
            if path.startswith("tools/") and path.endswith(".log"):
                leaked.append(path)
            elif path.startswith("tools/waves/") and name in CLOSED_WAVE_EVIDENCE_NAMES:
                leaked.append(path)
            elif (
                path.startswith("tools/")
                and path.count("/") == 1
                and is_closed_root_dump_evidence(name)
            ):
                leaked.append(path)
        self.assertEqual([], leaked)
        for ledger in UNIQUE_ACTIVE_WAVE_LEDGERS:
            self.assertIn(ledger, tracked)
        self.assertIn("tools/gt6_pipe_source.json", tracked)
        self.assertIn("tools/hopper_hopper_source_evidence.json", tracked)
        self.assertIn("tools/python_test_policy.json", tracked)

    def test_game_test_java_lives_in_the_test_source_set(self) -> None:
        main = ROOT / "src/test/java/com/masson/cruciblecraft/gametest"
        test_holder = (
            ROOT
            / "src/test/java/com/masson/cruciblecraft/gametest"
            / "ModIdNamespaceGameTests.java"
        )
        runs = (ROOT / "gradle" / "scripts" / "runs.gradle").read_text(
            encoding="utf-8"
        )
        self.assertFalse(main.exists())
        self.assertTrue(test_holder.is_file())
        self.assertTrue(
            (
                ROOT
                / "src/test/java/com/masson/cruciblecraft/scale"
                / "ScaleGameTests.java"
            ).is_file()
        )
        self.assertTrue(
            (
                ROOT
                / "src/test/java/com/masson/cruciblecraft/census"
                / "RecipeCensusGameTests.java"
            ).is_file()
        )
        self.assertIn("addModdingDependenciesTo sourceSets.test", runs)
        self.assertIn("sourceSet = sourceSets.test", runs)
        self.assertIn("sourceSet(sourceSets.test)", runs)


if __name__ == "__main__":
    unittest.main()
