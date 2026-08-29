#!/usr/bin/env python3
"""Regression coverage for T38 source-backed recovery routes."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

TOOLS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(TOOLS))

import build_t38_player_path_recovery as recovery


class T38PlayerPathRecoveryTests(unittest.TestCase):
    def test_routes_cover_surface_rocks_and_vanilla_blaze(self) -> None:
        document, files = recovery.build()
        routes = {route["material"]: route for route in document["routes"]}
        self.assertEqual(
            set(routes),
            {"blaze", "diamantine", "niobium", "tantalum", "vanadium"},
        )
        for material in ("niobium", "tantalum", "vanadium"):
            self.assertEqual(
                routes[material]["seed"], f"cruciblecraft:{material}/rock"
            )
        self.assertEqual(routes["blaze"]["seed"], "minecraft:blaze_rod")
        self.assertEqual(routes["diamantine"]["seed"], "minecraft:diamond")
        self.assertEqual(len(files), 9)

    def test_every_route_has_pinned_recovery_and_packing_evidence(self) -> None:
        document, _files = recovery.build()
        for route in document["routes"]:
            if route["material"] == "diamantine":
                self.assertEqual(
                    route["source"]["source_kind"],
                    "gt6_oredict_alias_projection",
                )
                self.assertEqual(route["source"]["alias"], "AnyDiamond")
                continue
            self.assertEqual(route["source"]["packing_map"], "gt.recipe.boxinator")
            self.assertIsInstance(route["source"]["recipe"], int)
            self.assertIsInstance(route["source"]["packing_recipe"], int)


if __name__ == "__main__":
    raise SystemExit(unittest.main())
