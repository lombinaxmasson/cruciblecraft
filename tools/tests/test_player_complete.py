#!/usr/bin/env python3
"""Player-complete gate for the fluid-network pilot."""
from __future__ import annotations

import contextlib
import io
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tools import capability_ledger
from tools import playtest
from tools import player_complete
from tools import io_common as files

SLUG = "logistics/fluid-network/basic-transfer"
BATTERIES_SLUG = "energy/batteries"
OBSERVATION_SLUG = "energy/nuclear-fission-observation-safety"
CLUSTER_MILL_SLUG = "machines/cluster-mill"
FLUID_TEST_IDS = [
    "coverIdentitySurvivesBlockEntityReload",
    "coversAreSurvivalCraftable",
    "differentIdentityIsInvisible",
    "disconnectedPipesAreNotOneNetwork",
    "importPullsFromStorage",
    "loadAxisCapsAreRecorded",
    "noTargetDoesNotSwallowFluids",
    "playerSurfaceIsRegistered",
    "sameIdentityConnectedExportsIntoStorage",
]
class PlayerCompleteTest(unittest.TestCase):
    def setUp(self) -> None:
        path = capability_ledger.CAP_ROOT / SLUG / "capability.json"
        self.capability = capability_ledger.load_capability(path)
        signoff_path = capability_ledger.CAP_ROOT / SLUG / "player_signoff.json"
        self.item_ids = list(files.load_json(signoff_path)["craftable_items"])

    def assert_declared_game_tests(self, slug: str) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / slug / "capability.json"
        )
        self.assertEqual("runtime_ready", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(
            [],
            player_complete.check_declared_test_ids(capability),
        )

    def test_zh_cover_english_is_rejected(self) -> None:
        errors = player_complete.check_static_player_surface(
            "demo/slug",
            ["logistics_fluid_storage_cover"],
        )
        self.assertEqual([], errors)

    def test_machine_recipe_output_counts_as_obtain(self) -> None:
        filled_geiger = "tool/geiger_counter_measures_neutron_count"
        self.assertIn(filled_geiger, player_complete.recipe_output_ids())
        self.assertEqual(
            [],
            player_complete.check_static_player_surface(
                OBSERVATION_SLUG,
                [filled_geiger],
            ),
        )

    def test_form_items_obtain_does_not_require_shaped_recipe(self) -> None:
        self.assertIn(
            "red_energium_crystal_ulv",
            player_complete.form_item_ids(),
        )
        errors = player_complete.check_static_player_surface(
            BATTERIES_SLUG,
            ["red_energium_crystal_ulv", "cyan_energium_crystal_iv"],
        )
        self.assertEqual([], errors)
        self.assertTrue(
            any(
                "missing recipe" in row
                for row in player_complete.check_static_player_surface(
                    "demo/slug",
                    ["not_a_registered_item"],
                )
            )
        )

    def test_player_complete_maturity_is_abolished(self) -> None:
        self.assertEqual([], player_complete.player_complete_slugs())
        for slug in (CLUSTER_MILL_SLUG, SLUG, BATTERIES_SLUG):
            capability = capability_ledger.load_capability(
                capability_ledger.CAP_ROOT / slug / "capability.json"
            )
            self.assert_declared_game_tests(slug)
            self.assertEqual("unreviewed", capability.get("survival_access"))
            self.assertIsNone(capability.get("player_signoff"))
            self.assertNotIn("player-complete", capability.get("profiles") or [])

    def gametest_receipt(self) -> dict[str, object]:
        wave = self.capability["wave_slug"]
        return {
            "schema_version": 1,
            "status": "PASS",
            "wave_slug": wave,
            "namespace": (
                "cruciblecraft_wave_"
                + str(wave).replace("/", "_").replace("-", "_")
            ),
            "failed": 0,
            "passed": len(FLUID_TEST_IDS),
            "required_tests": len(FLUID_TEST_IDS),
            "test_ids": list(FLUID_TEST_IDS),
            "skip_is_not_pass": True,
        }

    def client_receipt(self) -> dict[str, object]:
        registry_ids = [
            f"cruciblecraft:{item}" for item in self.item_ids
        ]
        return {
            "schema_version": 1,
            "capability": SLUG,
            "runtime": "client",
            "emi_plugin_class": player_complete.EMI_PLUGIN_CLASS,
            "modid": "cruciblecraft",
            "required_registry_ids": registry_ids,
            "registry_ids": registry_ids,
            "creative_tab": True,
            "emi_registration_plan": True,
            "status": "PASS",
        }

    def write_receipt(
        self,
        directory: str,
        name: str,
        document: dict[str, object],
    ) -> Path:
        path = Path(directory) / name
        path.write_text(
            json.dumps(document, indent=2, sort_keys=True) + "\n",
            encoding="utf-8",
        )
        return path

    def test_fresh_receipts_verify_pilot(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            gametest = self.write_receipt(
                directory,
                "gametest.json",
                self.gametest_receipt(),
            )
            client = self.write_receipt(
                directory,
                "client.json",
                self.client_receipt(),
            )
            errors = player_complete.check_capability(
                SLUG,
                gametest_receipt=gametest,
                client_receipt=client,
            )
        self.assertEqual([], errors, errors)

    def test_cli_accepts_explicit_fresh_receipts(self) -> None:
        stdout = io.StringIO()
        with tempfile.TemporaryDirectory() as directory:
            gametest = self.write_receipt(
                directory,
                "gametest.json",
                self.gametest_receipt(),
            )
            client = self.write_receipt(
                directory,
                "client.json",
                self.client_receipt(),
            )
            with contextlib.redirect_stdout(stdout):
                result = player_complete.main(
                    [
                        "--check",
                        "--capability",
                        SLUG,
                        "--gametest-receipt",
                        str(gametest),
                        "--client-receipt",
                        str(client),
                    ]
                )
        self.assertEqual(0, result)
        self.assertIn("verified by fresh execution", stdout.getvalue())

    def test_default_check_without_fresh_receipts_fails(self) -> None:
        stderr = io.StringIO()
        with contextlib.redirect_stderr(stderr):
            result = player_complete.main(
                ["--check", "--capability", SLUG]
            )
        self.assertEqual(1, result)
        self.assertIn("--gametest-receipt PATH", stderr.getvalue())

    def test_capability_tree_has_no_machine_receipt(self) -> None:
        receipt = capability_ledger.CAP_ROOT / SLUG / "client_smoke_receipt.json"
        self.assertFalse(receipt.exists())

    def test_committed_receipt_cannot_be_used_as_fresh_proof(self) -> None:
        committed = (
            files.TOOLS
            / "waves"
            / "runtime"
            / "fluid-network-basic-transfer"
            / "gametest_receipt.json"
        )
        errors = player_complete.check_gametest_receipt(
            self.capability,
            committed,
        )
        self.assertTrue(any("must be ephemeral" in row for row in errors))

    def test_gametest_counts_must_match_test_ids(self) -> None:
        receipt = self.gametest_receipt()
        receipt["passed"] = len(FLUID_TEST_IDS) + 1
        with tempfile.TemporaryDirectory() as directory:
            path = self.write_receipt(directory, "gametest.json", receipt)
            errors = player_complete.check_gametest_receipt(
                self.capability,
                path,
            )
        self.assertTrue(
            any(f"test_ids={len(FLUID_TEST_IDS)} passed=" in row for row in errors)
        )

    def test_renamed_or_dropped_required_test_fails(self) -> None:
        receipt = self.gametest_receipt()
        receipt["test_ids"] = ["renamedTest"] + FLUID_TEST_IDS[1:]
        with tempfile.TemporaryDirectory() as directory:
            path = self.write_receipt(directory, "gametest.json", receipt)
            errors = player_complete.check_gametest_receipt(
                self.capability,
                path,
            )
        self.assertTrue(any("!= required_test_ids" in row for row in errors))

    def test_declared_test_ids_match_java_methods(self) -> None:
        self.assertEqual([], player_complete.check_declared_test_ids(self.capability))
        self.assertEqual(
            FLUID_TEST_IDS,
            player_complete.discover_gametest_method_ids(self.capability),
        )

    def test_renamed_java_method_breaks_declared_contract(self) -> None:
        capability = dict(self.capability)
        capability["required_test_ids"] = ["renamedTest"] + FLUID_TEST_IDS[1:]
        errors = player_complete.check_declared_test_ids(capability)
        self.assertTrue(any("!= GameTest methods" in row for row in errors))

    def test_gametest_server_receipt_is_not_client_evidence(self) -> None:
        receipt = self.client_receipt()
        receipt["runtime"] = "gameTestServer"
        with tempfile.TemporaryDirectory() as directory:
            path = self.write_receipt(directory, "client.json", receipt)
            errors = player_complete.check_client_receipt(
                self.capability,
                path,
                self.item_ids,
            )
        self.assertTrue(any("is not runClient" in row for row in errors))

    def test_all_is_a_noop_after_obtain_reset(self) -> None:
        self.assertEqual([], player_complete.player_complete_slugs())
        self.assertEqual([], player_complete.resolve_slugs(None, True))
        stdout = io.StringIO()
        with contextlib.redirect_stdout(stdout):
            result = player_complete.main(["--check", "--all"])
        self.assertEqual(0, result)
        self.assertIn("empty", stdout.getvalue())

    def test_run_mode_uses_game_test_only_by_default(self) -> None:
        calls: list[list[str]] = []

        def property_value(command: list[str], name: str) -> str:
            prefix = f"-P{name}="
            return next(value.removeprefix(prefix) for value in command if value.startswith(prefix))

        def fake_run(command: list[str], _log: Path) -> tuple[int, str]:
            calls.append(command)
            receipt = Path(property_value(command, "smokeReceipt"))
            nonce = property_value(command, "smokeNonce")
            if "runGameTestServer" in command:
                document = self.client_receipt()
                document["runtime"] = "gameTestServer"
                document["run_nonce"] = nonce
                self.write_receipt(
                    str(receipt.parent),
                    receipt.name,
                    document,
                )
                return 0, "All 9 required tests passed :)\n"
            document = self.client_receipt()
            document["run_nonce"] = nonce
            self.write_receipt(
                str(receipt.parent),
                receipt.name,
                document,
            )
            return 0, ""

        with tempfile.TemporaryDirectory() as directory, mock.patch.object(
            player_complete,
            "LOCAL_RECEIPTS",
            Path(directory),
        ), mock.patch.object(
            player_complete,
            "run_logged",
            side_effect=fake_run,
        ):
            errors = player_complete.run_fresh_capability(SLUG)
            report = next(Path(directory).rglob("latest.json"))
            saved = json.loads(report.read_text(encoding="utf-8"))
        self.assertEqual([], errors, errors)
        self.assertEqual(1, len(calls))
        self.assertEqual("PASS", saved["status"])
        self.assertTrue(saved["client"]["skipped"])
        self.assertIn("runGameTestServer", calls[0])
        self.assertTrue(all("--rerun" in command for command in calls))
        self.assertTrue(all("--rerun-tasks" not in command for command in calls))
        self.assertTrue(all("--no-daemon" not in command for command in calls))
        self.assertEqual(
            {SLUG},
            {property_value(command, "playerCapability") for command in calls},
        )

    def test_run_mode_client_uses_one_nonce_for_both_runtime_processes(self) -> None:
        calls: list[list[str]] = []

        def property_value(command: list[str], name: str) -> str:
            prefix = f"-P{name}="
            return next(value.removeprefix(prefix) for value in command if value.startswith(prefix))

        def fake_run(command: list[str], _log: Path) -> tuple[int, str]:
            calls.append(command)
            receipt = Path(property_value(command, "smokeReceipt"))
            nonce = property_value(command, "smokeNonce")
            if "runGameTestServer" in command:
                document = self.client_receipt()
                document["runtime"] = "gameTestServer"
                document["run_nonce"] = nonce
                self.write_receipt(
                    str(receipt.parent),
                    receipt.name,
                    document,
                )
                return 0, "All 9 required tests passed :)\n"
            document = self.client_receipt()
            document["run_nonce"] = nonce
            self.write_receipt(
                str(receipt.parent),
                receipt.name,
                document,
            )
            return 0, ""

        with tempfile.TemporaryDirectory() as directory, mock.patch.object(
            player_complete,
            "LOCAL_RECEIPTS",
            Path(directory),
        ), mock.patch.object(
            player_complete,
            "run_logged",
            side_effect=fake_run,
        ):
            errors = player_complete.run_fresh_capability(SLUG, client=True)
            report = next(Path(directory).rglob("latest.json"))
            saved = json.loads(report.read_text(encoding="utf-8"))
        self.assertEqual([], errors, errors)
        self.assertEqual(2, len(calls))
        self.assertEqual("PASS", saved["status"])
        self.assertNotIn("skipped", saved["client"])
        nonces = {
            property_value(command, "smokeNonce")
            for command in calls
        }
        self.assertEqual(1, len(nonces))
        self.assertEqual(
            {SLUG},
            {property_value(command, "playerCapability") for command in calls},
        )

    def test_catalog_item_entity_worldgen_is_forbidden_obtain(self) -> None:
        root = files.ROOT
        rule = (
            root
            / ".cursor"
            / "rules"
            / "gt6-no-item-entity-worldgen-acquisition.mdc"
        ).read_text(encoding="utf-8")
        self.assertIn("alwaysApply: true", rule)
        self.assertIn("ItemEntity", rule)
        self.assertIn("WorldgenRocks", rule)
        self.assertIn("empty-input overworld item scatter", rule)
        self.assertIn("用主世界掉落物顶 player_complete", rule)
        workflow = (
            root / "docs" / "current" / "capability-delivery-workflow.md"
        ).read_text(encoding="utf-8")
        self.assertIn("用主世界掉落物顶 player_complete", workflow)
        self.assertIn("gt6-no-item-entity-worldgen.md", workflow)
        contract = (
            root / "docs" / "current" / "gt6-no-item-entity-worldgen.md"
        ).read_text(encoding="utf-8")
        self.assertIn("严禁", contract)
        self.assertIn("ItemEntity", contract)
        worldgen = (
            root
            / "src"
            / "main"
            / "java"
            / "com"
            / "masson"
            / "cruciblecraft"
            / "worldgen"
        )
        banned = {
            "GtItemScatterFeature.java",
            "GtBlockObjectScatterFeature.java",
            "GtStoneScatterFeature.java",
            "ItemScatterConfiguration.java",
        }
        present = {path.name for path in worldgen.glob("*.java")}
        self.assertEqual(set(), present & banned)
        item_entity_files = [
            path.name
            for path in worldgen.glob("*.java")
            if "new ItemEntity" in path.read_text(encoding="utf-8")
        ]
        self.assertEqual([], item_entity_files)
        features = (
            root
            / "src"
            / "main"
            / "java"
            / "com"
            / "masson"
            / "cruciblecraft"
            / "registry"
            / "ModFeatures.java"
        ).read_text(encoding="utf-8")
        self.assertEqual(0, features.count("GtItemScatterFeature::new"))
        self.assertEqual(0, features.count("GtStoneScatterFeature::new"))
        self.assertEqual(0, features.count("GtBlockObjectScatterFeature::new"))
        self.assertIn("SURFACE_ROCK_SCATTER", features)
        self.assertNotIn("do not register another", features)


class PlaytestCycleTest(unittest.TestCase):
    def test_committed_cycle_is_pending_obtain_reset(self) -> None:
        cycle = playtest.load_cycle()
        self.assertEqual("2026-09-15-obtain-reset", cycle["id"])
        self.assertEqual("pending", cycle["status"])
        self.assertIn(cycle.get("accepted"), (None, {}))

    def test_record_accept_requires_human_flag(self) -> None:
        stderr = io.StringIO()
        with contextlib.redirect_stderr(stderr):
            code = playtest.main(
                ["record-accept", "--id", "x", "--signer", "agent"]
            )
        self.assertEqual(1, code)
        self.assertIn("--i-playtested", stderr.getvalue())

    def test_minor_does_not_invalidate_accepted_cycle(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "current_cycle.json"
            path.write_text(
                json.dumps(
                    {
                        "schema_version": 1,
                        "id": "cycle-1",
                        "status": "accepted",
                        "opened_at": "2026-09-01",
                        "reason": "batch",
                        "scope": ["gui"],
                        "accepted": {
                            "at": "2026-09-01",
                            "notes": "",
                            "signer": "human",
                            "source": "human_report",
                        },
                    },
                    indent=2,
                )
                + "\n",
                encoding="utf-8",
            )
            with mock.patch.object(playtest, "CYCLE_PATH", path):
                minor = playtest.record_change("minor", "typo", "docs/foo.md")
                self.assertEqual("accepted", minor["status"])
                self.assertEqual("cycle-1", minor["id"])
                major = playtest.record_change("major", "worldgen", "worldgen")
                self.assertEqual("pending", major["status"])
                self.assertNotEqual("cycle-1", major["id"])

    def test_accept_requires_matching_id_and_human_source(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "current_cycle.json"
            path.write_text(
                json.dumps(
                    {
                        "schema_version": 1,
                        "id": "cycle-1",
                        "status": "pending",
                        "opened_at": "2026-09-01",
                        "reason": "batch",
                        "scope": ["gui"],
                        "accepted": None,
                    },
                    indent=2,
                )
                + "\n",
                encoding="utf-8",
            )
            with mock.patch.object(playtest, "CYCLE_PATH", path):
                with self.assertRaisesRegex(ValueError, "does not match"):
                    playtest.record_accept(cycle_id="wrong", signer="human")
                accepted = playtest.record_accept(
                    cycle_id="cycle-1",
                    signer="human",
                )
                self.assertEqual("accepted", accepted["status"])
                self.assertEqual("human_report", accepted["accepted"]["source"])

    def test_survival_access_does_not_block_runtime_close(self) -> None:
        for value in playtest.SURVIVAL_ACCESS:
            self.assertFalse(playtest.survival_access_blocks_runtime_close(value))


if __name__ == "__main__":
    unittest.main()
