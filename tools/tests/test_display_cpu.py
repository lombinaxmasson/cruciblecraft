#!/usr/bin/env python3
"""Display CPU closed card: sidecar, catalog lock, player_complete."""
from __future__ import annotations

import hashlib
import re
import unittest

from tools import capability_ledger as ledger
from tools import io_common as io
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.wave_closeout import spec_for

SLUG = "runtime/display-cpu"
CAPABILITY = "logistics/display-cpu"
ROOT = io.ROOT
WAVE = io.TOOLS / "waves" / "runtime" / "display-cpu"
COVER_DEFINITIONS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "cover_definitions.json"
)
GENERIC_SIDECAR = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "generic_network_cover_definitions.json"
)
SIDECAR = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "logistics_display_cpu_cover_definitions.json"
)
REGISTRY = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "logistics"
    / "pipe"
    / "cover"
    / "CoverBehaviorRegistry.java"
)
INHERITED = (
    io.TOOLS
    / "waves"
    / "portfolio"
    / "logistics-cover-net-r0"
    / "inherited_denominator.json"
)
BUILTIN_RE = re.compile(r'registerBuiltin\("([a-z0-9_]+)"')
PINNED_COVER_DEFINITIONS_HASH = "207b4d03a66bf088574c1e6fef01971de307255eedf68c8a661741d839e857ec"
PINNED_INHERITED_HASH = "d7d58f1c375aa59c45bd32e1451cc359b18a57b55328e38f3d425de62737f758"


class DisplayCpuCardTest(unittest.TestCase):
    def test_slug_is_zero_family_runtime_card(self) -> None:
        self.assertIn(SLUG, KNOWN_SEMANTIC_SLUGS)
        spec = spec_for(SLUG)
        self.assertIsNone(spec.unique_active_wave)
        self.assertTrue(spec.next_unassigned)
        self.assertEqual(0, spec.owns_families)
        self.assertIsNone(spec.production_lock)
        self.assertEqual(WAVE / "census_delta.json", spec.census)
        self.assertEqual(WAVE / "gametest_receipt.json", spec.receipt)

    def test_art_manifest_is_local_gt6_w(self) -> None:
        manifest = io.load_json(
            ROOT
            / "src"
            / "main"
            / "resources"
            / "assets"
            / "cruciblecraft"
            / "display_cpu_art_manifest.json"
        )
        self.assertEqual("gt6_referencable_port_code/gregtech6_w", manifest["source"])
        self.assertEqual(52, len(manifest["imports"]))
        gt6 = ROOT / "gt6_referencable_port_code" / "gregtech6_w" / "src" / "main" / "resources"
        item_icons = {
            "logistics_display_cpu_logic_cover.png": (
                "assets/gregtech/textures/items/gt.multiitem.technological/1086.png"
            ),
            "logistics_display_cpu_control_cover.png": (
                "assets/gregtech/textures/items/gt.multiitem.technological/1087.png"
            ),
            "logistics_display_cpu_storage_cover.png": (
                "assets/gregtech/textures/items/gt.multiitem.technological/1088.png"
            ),
            "logistics_display_cpu_conversion_cover.png": (
                "assets/gregtech/textures/items/gt.multiitem.technological/1089.png"
            ),
        }
        by_dest = {row["destination"]: row["gt6_source"] for row in manifest["imports"]}
        for name, source in item_icons.items():
            dest = (
                "assets/cruciblecraft/textures/item/gt6_import/" + name
            )
            self.assertEqual(source, by_dest[dest])
            icon = ROOT / "src" / "main" / "resources" / dest
            self.assertTrue(icon.is_file(), name)
            self.assertEqual((gt6 / source).read_bytes(), icon.read_bytes(), name)


class DisplayCpuCatalogLockTest(unittest.TestCase):
    def test_base_cover_file_stays_nine_rows_without_logistics_ids(self) -> None:
        catalog = io.load_json(COVER_DEFINITIONS)
        rows = catalog["definitions"]
        self.assertEqual(9, len(rows))
        for row in rows:
            self.assertNotIn("logistics_", row["id"])
        self.assertEqual(
            PINNED_COVER_DEFINITIONS_HASH,
            hashlib.sha256(COVER_DEFINITIONS.read_bytes()).hexdigest(),
        )

    def test_register_builtin_stays_eight(self) -> None:
        text = REGISTRY.read_text(encoding="utf-8")
        builtins = BUILTIN_RE.findall(text)
        self.assertEqual(8, len(builtins))
        self.assertNotIn("logistics_display_cpu", builtins)
        self.assertNotIn("logistics_display_cpu_logic", builtins)

    def test_display_sidecar_is_owned_not_generic(self) -> None:
        generic = [row["id"] for row in io.load_json(GENERIC_SIDECAR)["definitions"]]
        self.assertNotIn("cruciblecraft:logistics_display_cpu_logic", generic)
        rows = io.load_json(SIDECAR)["definitions"]
        self.assertEqual(4, len(rows))
        self.assertEqual(
            [
                "cruciblecraft:logistics_display_cpu_logic",
                "cruciblecraft:logistics_display_cpu_control",
                "cruciblecraft:logistics_display_cpu_storage",
                "cruciblecraft:logistics_display_cpu_conversion",
            ],
            [row["id"] for row in rows],
        )
        for row in rows:
            self.assertEqual("cruciblecraft:logistics_display_cpu", row["behavior"])
            self.assertEqual("both", row["medium"])
            self.assertEqual([], row["configurable"])

    def test_r0_display_cpu_out_of_scope_bytes_are_unchanged(self) -> None:
        inherited = io.load_json(INHERITED)
        self.assertIn("display_cpu_out_of_scope", inherited)
        self.assertEqual(
            PINNED_INHERITED_HASH,
            hashlib.sha256(INHERITED.read_bytes()).hexdigest(),
        )


class DisplayCpuCapabilityTest(unittest.TestCase):
    def test_display_cpu_capability_is_player_complete(self) -> None:
        documents = [
            ledger.load_capability(path) for path in ledger.capability_files()
        ]
        row = next(item for item in documents if item["slug"] == CAPABILITY)
        self.assertEqual("player_complete", row["maturity"])
        self.assertEqual("accepted", row["workflow"])
        self.assertEqual(
            "tools/capabilities/logistics/display-cpu/player_signoff.json",
            row.get("player_signoff"),
        )
        self.assertEqual(
            [
                "displayCoverIsSurvivalCraftable",
                "formedCoreWritesDisplayLoad",
                "missingCoreStaysZero",
                "playerSurfaceIsRegistered",
                "shapelessDisplayCycle",
            ],
            row["required_test_ids"],
        )
        compiled = ledger.compile_ledger()
        self.assertIn(CAPABILITY, compiled["declared_player_complete"])
        self.assertIn("logistics/logistics-core", compiled["declared_player_complete"])


if __name__ == "__main__":
    unittest.main()
