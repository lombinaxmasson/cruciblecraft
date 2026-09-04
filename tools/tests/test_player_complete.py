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
        signoff = player_complete.load_signoff(self.capability)
        self.item_ids = list(signoff["craftable_items"])

    def test_zh_cover_english_is_rejected(self) -> None:
        errors = player_complete.check_static_player_surface(
            "demo/slug",
            ["logistics_fluid_storage_cover"],
        )
        self.assertEqual([], errors)

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
        self.assertIn("--client-receipt PATH", stderr.getvalue())

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

    def test_run_mode_uses_one_nonce_for_both_runtime_processes(self) -> None:
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
        self.assertEqual(2, len(calls))
        self.assertEqual("PASS", saved["status"])
        self.assertTrue(all("--rerun" in command for command in calls))
        self.assertTrue(all("--rerun-tasks" not in command for command in calls))
        self.assertTrue(all("--no-daemon" not in command for command in calls))
        nonces = {
            property_value(command, "smokeNonce")
            for command in calls
        }
        self.assertEqual(1, len(nonces))


if __name__ == "__main__":
    unittest.main()
