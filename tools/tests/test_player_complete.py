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
from tools import player_complete
from tools import io_common as files

SLUG = "logistics/fluid-network/basic-transfer"
ITEM_SLUG = "logistics/item-network-core"
GENERIC_SLUG = "logistics/generic-network/core"
CORE_SLUG = "logistics/logistics-core"
DISPLAY_SLUG = "logistics/display-cpu"
CONVERTER_SLUG = "energy/converter-catalog"
BATTERIES_SLUG = "energy/batteries"
TRANSFORMERS_SLUG = "energy/transformers"
NUCLEAR_SLUG = "energy/nuclear-fission-survival"
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
GENERIC_TEST_IDS = [
    "coverIdentitySurvivesBlockEntityReload",
    "coversAreSurvivalCraftable",
    "differentIdentityIsInvisible",
    "disconnectedPipesAreNotOneNetwork",
    "importPullsFromStorage",
    "loadAxisCapsAreRecorded",
    "noTargetDoesNotSwallowItems",
    "playerSurfaceIsRegistered",
    "sameIdentityConnectedExportsFluidsIntoStorage",
    "sameIdentityConnectedExportsIntoStorage",
]
ITEM_TEST_IDS = [
    "chunkUnloadHidesEndpointThenRejoins",
    "coverIdentitySurvivesBlockEntityReload",
    "coversAreSurvivalCraftable",
    "definitionsSurviveDatapackReload",
    "differentIdentityIsInvisible",
    "disconnectedPipesAreNotOneNetwork",
    "importPullsFromStorage",
    "loadAxisCapsAreRecorded",
    "noTargetDoesNotSwallowItems",
    "playerSurfaceIsRegistered",
    "removingStorageStopsTransfer",
    "sameIdentityConnectedExportsIntoStorage",
]
CORE_TEST_IDS = [
    "dumpCoverIsSurvivalCraftable",
    "formedCoreDumpsLeftoverItems",
    "missingCpuTypeDoesNotForm",
    "missingDumpChestDoesNotVoid",
    "playerSurfaceIsRegistered",
]
DISPLAY_TEST_IDS = [
    "displayCoverIsSurvivalCraftable",
    "formedCoreWritesDisplayLoad",
    "missingCoreStaysZero",
    "playerSurfaceIsRegistered",
    "shapelessDisplayCycle",
]
CONVERTER_TEST_IDS = [
    "gasBurningBoxFeedsAdjacentHu",
    "playerSurfaceIsRegistered",
    "representativeRecipesAreSurvivalCraftable",
    "solidBurningBoxBurnsCoal",
]
BATTERIES_TEST_IDS = [
    "chargeStoresEu",
    "dischargeExtractsEu",
    "luRejectsEu",
    "playerSurfaceIsRegistered",
    "reloadPreservesCharge",
    "representativeRecipesAreSurvivalCraftable",
]
TRANSFORMERS_TEST_IDS = [
    "playerSurfaceIsRegistered",
    "rejectsNonEu",
    "reloadPreservesFacingAndReverse",
    "representativeRecipesAreSurvivalCraftable",
    "reverseStepUpConvertsLvToHv",
    "stepDownConvertsHvToLv",
    "onlyMonkeyWrenchReversesTransformer",
]
NUCLEAR_TEST_IDS = [
    "breederProductTransformUsesExactTarget",
    "cannerFillsUranium238FuelRod",
    "cannerUnloadsTritiumAndKeepsProgressAcrossReload",
    "centrifugeRecoversDepletedUranium238",
    "distilledWaterProducesSteam",
    "extruderMakesEmptyZirconiumRod",
    "fortyEightNuclearSourceRecipesArePresent",
    "fortySixRodsAreRegistered",
    "insertingFuelRodStopsReactor",
    "lowDurabilityFuelBecomesDepleted",
    "lvCannerCraftsFromExactParts",
    "playerSurfaceIsRegistered",
    "reflectorNeighborReturnsNeutrons",
    "twoByTwoCoreCraftsFromExactParts",
    "unstoppedCoreEmitsNeutronsAtTick19",
    "uranium238PlayerPathMakesSteamForExistingEngine",
]


class PlayerCompleteTest(unittest.TestCase):
    def setUp(self) -> None:
        path = capability_ledger.CAP_ROOT / SLUG / "capability.json"
        self.capability = capability_ledger.load_capability(path)
        signoff = player_complete.load_signoff(self.capability)
        self.item_ids = list(signoff["craftable_items"])

    def test_zh_cover_english_is_rejected(self) -> None:
        errors = player_complete.check_static_player_surface(
            "demo/slug",
            ["logistics_fluid_storage_cover"],
        )
        self.assertEqual([], errors)

    def test_item_capability_declares_all_game_tests(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / ITEM_SLUG / "capability.json"
        )
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(ITEM_TEST_IDS, capability["required_test_ids"])
        self.assertEqual(
            [],
            player_complete.check_declared_test_ids(capability),
        )

    def test_generic_capability_declares_all_game_tests(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / GENERIC_SLUG / "capability.json"
        )
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(GENERIC_TEST_IDS, capability["required_test_ids"])
        self.assertEqual(
            [],
            player_complete.check_declared_test_ids(capability),
        )

    def test_generic_capability_static_player_surface(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / GENERIC_SLUG / "capability.json"
        )
        signoff = player_complete.load_signoff(capability)
        self.assertEqual(
            [],
            player_complete.check_static_player_surface(
                GENERIC_SLUG,
                list(signoff["craftable_items"]),
            ),
        )

    def test_core_capability_declares_all_game_tests(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / CORE_SLUG / "capability.json"
        )
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(CORE_TEST_IDS, capability["required_test_ids"])
        self.assertEqual(
            [],
            player_complete.check_declared_test_ids(capability),
        )

    def test_core_capability_static_player_surface(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / CORE_SLUG / "capability.json"
        )
        signoff = player_complete.load_signoff(capability)
        self.assertEqual(
            [],
            player_complete.check_static_player_surface(
                CORE_SLUG,
                list(signoff["craftable_items"]),
            ),
        )

    def test_display_capability_declares_all_game_tests(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / DISPLAY_SLUG / "capability.json"
        )
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(DISPLAY_TEST_IDS, capability["required_test_ids"])
        self.assertEqual(
            [],
            player_complete.check_declared_test_ids(capability),
        )

    def test_display_capability_static_player_surface(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / DISPLAY_SLUG / "capability.json"
        )
        signoff = player_complete.load_signoff(capability)
        self.assertEqual(
            [],
            player_complete.check_static_player_surface(
                DISPLAY_SLUG,
                list(signoff["craftable_items"]),
            ),
        )

    def test_converter_capability_declares_all_game_tests(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / CONVERTER_SLUG / "capability.json"
        )
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(CONVERTER_TEST_IDS, capability["required_test_ids"])
        self.assertEqual(
            [],
            player_complete.check_declared_test_ids(capability),
        )

    def test_converter_capability_static_player_surface(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / CONVERTER_SLUG / "capability.json"
        )
        signoff = player_complete.load_signoff(capability)
        self.assertEqual(
            [],
            player_complete.check_static_player_surface(
                CONVERTER_SLUG,
                list(signoff["craftable_items"]),
            ),
        )

    def test_batteries_capability_declares_all_game_tests(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / BATTERIES_SLUG / "capability.json"
        )
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(BATTERIES_TEST_IDS, capability["required_test_ids"])
        self.assertEqual(
            [],
            player_complete.check_declared_test_ids(capability),
        )

    def test_batteries_capability_static_player_surface(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / BATTERIES_SLUG / "capability.json"
        )
        signoff = player_complete.load_signoff(capability)
        self.assertEqual(
            [],
            player_complete.check_static_player_surface(
                BATTERIES_SLUG,
                list(signoff["craftable_items"]),
            ),
        )

    def test_transformers_capability_declares_all_game_tests(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / TRANSFORMERS_SLUG / "capability.json"
        )
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(TRANSFORMERS_TEST_IDS, capability["required_test_ids"])
        self.assertEqual(
            [],
            player_complete.check_declared_test_ids(capability),
        )

    def test_transformers_capability_static_player_surface(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / TRANSFORMERS_SLUG / "capability.json"
        )
        signoff = player_complete.load_signoff(capability)
        self.assertEqual(
            [],
            player_complete.check_static_player_surface(
                TRANSFORMERS_SLUG,
                list(signoff["craftable_items"]),
            ),
        )

    def test_nuclear_capability_declares_all_game_tests(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / NUCLEAR_SLUG / "capability.json"
        )
        self.assertEqual("player_complete", capability["maturity"])
        self.assertEqual("accepted", capability["workflow"])
        self.assertEqual(NUCLEAR_TEST_IDS, capability["required_test_ids"])
        self.assertEqual(
            [],
            player_complete.check_declared_test_ids(capability),
        )

    def test_nuclear_capability_static_player_surface(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / NUCLEAR_SLUG / "capability.json"
        )
        signoff = player_complete.load_signoff(capability)
        self.assertEqual(
            [],
            player_complete.check_static_player_surface(
                NUCLEAR_SLUG,
                list(signoff["craftable_items"]),
            ),
        )

    def test_item_capability_static_player_surface(self) -> None:
        capability = capability_ledger.load_capability(
            capability_ledger.CAP_ROOT / ITEM_SLUG / "capability.json"
        )
        signoff = player_complete.load_signoff(capability)
        self.assertEqual(
            [],
            player_complete.check_static_player_surface(
                ITEM_SLUG,
                list(signoff["craftable_items"]),
            ),
        )

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

    def test_surface_catalog_matches_signoff(self) -> None:
        catalog = player_complete.load_surface_catalog()
        surfaces = catalog["surfaces"]
        for slug in (
            SLUG,
            ITEM_SLUG,
            GENERIC_SLUG,
            CORE_SLUG,
            DISPLAY_SLUG,
            CONVERTER_SLUG,
            BATTERIES_SLUG,
            TRANSFORMERS_SLUG,
            NUCLEAR_SLUG,
        ):
            capability = capability_ledger.load_capability(
                capability_ledger.CAP_ROOT / slug / "capability.json"
            )
            signoff = player_complete.load_signoff(capability)
            self.assertEqual(
                [],
                player_complete.check_surface_catalog(
                    slug,
                    list(signoff["craftable_items"]),
                ),
            )
            self.assertIn(slug, surfaces)

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


if __name__ == "__main__":
    unittest.main()
