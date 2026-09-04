from __future__ import annotations

import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLS_MIRROR = ROOT / "tools" / "structure_plugin_whitelist.json"
RESOURCE_MIRROR = (
    ROOT
    / "src/main/resources/data/cruciblecraft/multiblock_plugins.json"
)
ALLOWED_CONSUMERS = {
    "large_centrifuge",
    "coke_oven",
    "distillation_tower",
    "large_boiler",
    "tank_3x3x3",
    "large_crucible",
}


class T23PluginWhitelistTest(unittest.TestCase):
    def setUp(self) -> None:
        self.tools = json.loads(
            TOOLS_MIRROR.read_text(encoding="utf-8")
        )
        self.resource = json.loads(
            RESOURCE_MIRROR.read_text(encoding="utf-8")
        )

    def test_plugin_id_set_is_frozen(self) -> None:
        self.assertEqual(
            {
                "cruciblecraft:processing_host",
                "cruciblecraft:shared_port_supply",
                "cruciblecraft:heat_energy_input",
                "cruciblecraft:steam_conversion",
                "cruciblecraft:storage_host",
                "cruciblecraft:thermal_steelmaking_host",
            },
            {row["id"] for row in self.tools["plugins"]},
        )

    def test_every_plugin_has_a_consumer_within_the_allowed_set(self) -> None:
        for row in self.tools["plugins"]:
            consumers = set(row["consumers"])
            self.assertTrue(consumers, row["id"])
            self.assertTrue(
                consumers <= ALLOWED_CONSUMERS,
                (row["id"], consumers - ALLOWED_CONSUMERS),
            )

    def test_quarantine_policy_is_declared_in_both_mirrors(self) -> None:
        self.assertTrue(self.tools["quarantine_policy"])
        self.assertTrue(self.resource["quarantine_policy"])

    def test_tools_mirror_and_resource_mirror_agree_on_ids(self) -> None:
        self.assertEqual(
            {row["id"] for row in self.resource["plugins"]},
            {row["id"] for row in self.tools["plugins"]},
        )


if __name__ == "__main__":
    unittest.main()
