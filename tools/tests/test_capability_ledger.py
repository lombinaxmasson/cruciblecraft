#!/usr/bin/env python3
"""Capability ledger schema, adapter, and impact graph."""
from __future__ import annotations

import json
import unittest
from pathlib import Path
from unittest import mock

from tools import capability_ledger as ledger
from tools import io_common as io

ROOT = io.ROOT
SCHEMA = io.TOOLS / "capabilities" / "schema.json"
FLUID = "logistics/fluid-network/basic-transfer"
ITEM = "logistics/item-network-core"
GENERIC = "logistics/generic-network/core"
CORE = "logistics/logistics-core"
DISPLAY = "logistics/display-cpu"
CONVERTER = "energy/converter-catalog"
BATTERIES = "energy/batteries"


class CapabilityLedgerTest(unittest.TestCase):
    def test_schema_and_slugs_are_loadable(self) -> None:
        schema = json.loads(SCHEMA.read_text(encoding="utf-8"))
        self.assertEqual(2, schema["properties"]["schema_version"]["const"])
        self.assertNotIn("evidence", schema["properties"])
        self.assertIn("required_test_ids", schema["properties"])
        documents = [
            ledger.load_capability(path) for path in ledger.capability_files()
        ]
        self.assertTrue(all("evidence" not in row for row in documents))
        slugs = [row["slug"] for row in documents]
        self.assertIn(FLUID, slugs)
        self.assertIn("logistics/cover-net-r0", slugs)
        self.assertIn(ITEM, slugs)
        self.assertIn(GENERIC, slugs)
        self.assertIn(CORE, slugs)
        self.assertIn(DISPLAY, slugs)
        self.assertIn(CONVERTER, slugs)
        self.assertIn(BATTERIES, slugs)
        self.assertIn("registry/tool-head-remainder", slugs)
        converter = next(row for row in documents if row["slug"] == CONVERTER)
        self.assertEqual("player_complete", converter["maturity"])
        self.assertEqual("accepted", converter["workflow"])
        batteries = next(row for row in documents if row["slug"] == BATTERIES)
        self.assertEqual("player_complete", batteries["maturity"])
        self.assertEqual("accepted", batteries["workflow"])
        fluid = next(row for row in documents if row["slug"] == FLUID)
        self.assertEqual("player_complete", fluid["maturity"])
        self.assertEqual("accepted", fluid["workflow"])
        cover = next(
            row for row in documents if row["slug"] == "logistics/cover-net-r0"
        )
        self.assertEqual("frozen", cover["maturity"])
        item = next(row for row in documents if row["slug"] == ITEM)
        self.assertEqual("player_complete", item["maturity"])
        self.assertEqual("accepted", item["workflow"])
        generic = next(row for row in documents if row["slug"] == GENERIC)
        self.assertEqual("player_complete", generic["maturity"])
        self.assertEqual("accepted", generic["workflow"])
        core = next(row for row in documents if row["slug"] == CORE)
        self.assertEqual("player_complete", core["maturity"])
        self.assertEqual("accepted", core["workflow"])
        display = next(row for row in documents if row["slug"] == DISPLAY)
        self.assertEqual("player_complete", display["maturity"])
        self.assertEqual("accepted", display["workflow"])
        self.assertEqual(
            [
                "coverIdentitySurvivesBlockEntityReload",
                "coversAreSurvivalCraftable",
                "differentIdentityIsInvisible",
                "disconnectedPipesAreNotOneNetwork",
                "importPullsFromStorage",
                "loadAxisCapsAreRecorded",
                "noTargetDoesNotSwallowFluids",
                "playerSurfaceIsRegistered",
                "sameIdentityConnectedExportsIntoStorage",
            ],
            fluid["required_test_ids"],
        )

    def test_compiled_ledger_has_no_legacy_progress_adapter(self) -> None:
        compiled = ledger.compile_ledger()
        self.assertNotIn("legacy_readiness", compiled)
        for row in compiled["capabilities"]:
            self.assertNotIn("legacy_readiness", row)
            self.assertNotIn("wave_slug", row)
        self.assertEqual(
            [BATTERIES, CONVERTER, DISPLAY, FLUID, GENERIC, ITEM, CORE], compiled["declared_player_complete"]
        )
        self.assertEqual(
            "declaration is not proof; player_complete requires fresh "
            "GameTestServer and runClient execution",
            compiled["progress_rule"],
        )

    def test_ledger_contains_profiles_and_impact_without_proof_fields(self) -> None:
        compiled = ledger.compile_ledger()
        self.assertEqual(
            [BATTERIES, CONVERTER, DISPLAY, FLUID, GENERIC, ITEM, CORE],
            compiled["profiles"]["player-complete"],
        )
        self.assertEqual(
            {
                BATTERIES,
                "logistics/cover-net-r0",
                ITEM,
                FLUID,
                GENERIC,
                CORE,
                DISPLAY,
                CONVERTER,
            },
            set(compiled["impact"]["logistics/cover-net-r0"]),
        )
        for capability in compiled["capabilities"]:
            self.assertNotIn("evidence", capability)
            self.assertNotIn("evidence_root", capability)
            self.assertNotIn("owned_hash_count", capability)

    def test_committed_ledger_is_deterministic(self) -> None:
        committed = (io.TOOLS / "capabilities" / "ledger.json").read_bytes()
        self.assertEqual(ledger.dumps(ledger.compile_ledger()), committed)

    def test_shared_cover_code_hits_cover_and_dependents(self) -> None:
        hit = ledger.affected_slugs(
            [
                "src/main/java/com/masson/cruciblecraft/logistics/pipe/cover/"
                "PipeCoverSet.java"
            ]
        )
        self.assertIn("logistics/cover-net-r0", hit)
        self.assertIn(ITEM, hit)
        self.assertIn(FLUID, hit)
        self.assertIn(GENERIC, hit)
        self.assertIn(CORE, hit)
        self.assertIn(DISPLAY, hit)
        self.assertIn(CONVERTER, hit)
        self.assertIn(BATTERIES, hit)
        self.assertNotIn("registry/tool-head-remainder", hit)

    def test_fluid_pipe_does_not_stale_tool_head_remainder(self) -> None:
        hit = ledger.affected_slugs(
            [
                "src/main/java/com/masson/cruciblecraft/content/blockentity/"
                "FluidPipeBlockEntity.java"
            ]
        )
        self.assertEqual([BATTERIES, CONVERTER, DISPLAY, FLUID, GENERIC, CORE], hit)

    def test_player_complete_promotions_detect_maturity_change(self) -> None:
        previous = json.dumps({"maturity": "runtime_ready"})
        with mock.patch.object(
            ledger,
            "_git_show",
            side_effect=lambda revision, relative: (
                previous
                if relative.endswith("basic-transfer/capability.json")
                else json.dumps(
                    {
                        "maturity": (
                            "player_complete"
                            if relative.endswith("item-network-core/capability.json")
                            or relative.endswith("generic-network/core/capability.json")
                            or relative.endswith("logistics-core/capability.json")
                            or relative.endswith("display-cpu/capability.json")
                            or relative.endswith("converter-catalog/capability.json")
                            or relative.endswith("batteries/capability.json")
                            else "frozen"
                        )
                    }
                )
            ),
        ):
            self.assertEqual(
                [FLUID],
                ledger.player_complete_promotions("origin/main"),
            )

    def test_player_complete_promotions_ignore_already_complete(self) -> None:
        with mock.patch.object(
            ledger,
            "_git_show",
            side_effect=lambda revision, relative: json.dumps(
                {
                    "maturity": (
                        "player_complete"
                        if relative.endswith("basic-transfer/capability.json")
                        or relative.endswith("item-network-core/capability.json")
                        or relative.endswith("generic-network/core/capability.json")
                        or relative.endswith("logistics-core/capability.json")
                        or relative.endswith("display-cpu/capability.json")
                        or relative.endswith("converter-catalog/capability.json")
                        or relative.endswith("batteries/capability.json")
                        else "frozen"
                    )
                }
            ),
        ):
            self.assertEqual([], ledger.player_complete_promotions("HEAD"))


if __name__ == "__main__":
    unittest.main()
