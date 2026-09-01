#!/usr/bin/env python3
"""Contract tests for T47 remainder identity, fluid, form, and operand freeze."""
from __future__ import annotations

import unittest

from tools import build_t47_bath_fluid_mapping as fluids
from tools import build_t47_bath_recycling_disposition as recycling
from tools import build_t47_identity_catalog as identity
from tools import build_t47_operand_runtime_map as operands
from tools import build_t47_required_forms as required_forms
from tools import t47_common as common
from tools import t47_identities as identities


class T47IdentityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.catalog = common.load_json(identity.OUTPUT)
        cls.fluids = fluids.build()
        cls.required_forms = required_forms.build()
        cls.recycling = common.load_json(recycling.OUTPUT)
        cls.operands = common.load_json(operands.OUTPUT)

    def test_identity_catalog_is_283_new_blocks(self) -> None:
        identities = list(self.catalog.get("identities") or [])
        self.assertEqual("T47_IDENTITY_CATALOG", self.catalog["status"])
        self.assertEqual(283, self.catalog["variant_count"])
        self.assertEqual(283, self.catalog["identity_count"])
        self.assertEqual(283, len(identities))
        self.assertEqual({"block": 283}, self.catalog["kind_counts"])
        self.assertTrue(all(row.get("kind") == "block" for row in identities))
        runtime_ids = [str(row["runtime_id"]) for row in identities]
        self.assertEqual(283, len(set(runtime_ids)))
        self.assertTrue(all(item.startswith("cruciblecraft:") for item in runtime_ids))
        self.assertGreaterEqual(self.catalog["lock_family_count"], common.MIN_PRODUCTION_FAMILIES)
        self.assertGreaterEqual(self.catalog["lock_family_count"], 300)
        self.assertEqual(identity.OUTPUT, common.IDENTITY_CATALOG)

    def test_fluid_mapping_has_three_lock_fluids(self) -> None:
        self.assertEqual("T47_BATH_FLUID_MAPPING", self.fluids["status"])
        self.assertEqual(3, self.fluids["counts"]["mapped"])
        self.assertEqual(3, len(self.fluids["mapping"]))
        self.assertEqual(
            [row["source_fluid"] for row in fluids.LOCK_FLUIDS],
            [row["source_fluid"] for row in self.fluids["mapping"]],
        )
        self.assertEqual(fluids.OUTPUT, common.FLUID_MAPPING)

    def test_required_forms_are_empty(self) -> None:
        self.assertEqual("T47_REQUIRED_FORMS", self.required_forms["status"])
        self.assertEqual({}, self.required_forms["required_forms"])
        self.assertEqual(0, self.required_forms["counts"]["required_form_pairs"])
        self.assertEqual(0, self.required_forms["counts"]["dust_div72_materials"])
        self.assertEqual(0, self.required_forms["counts"]["small_dust_materials"])
        self.assertEqual([], self.required_forms["dust_div72_source_metas"])
        self.assertEqual(required_forms.OUTPUT, common.REQUIRED_FORMS)

    def test_recycling_candidate_is_false_and_deferred_is_untouched(self) -> None:
        self.assertEqual("T47_BATH_RECYCLING_DISPOSITION", self.recycling["status"])
        self.assertIs(False, self.recycling["recycling_candidate"])
        self.assertEqual(common.DEFERRED_RECYCLING_COUNT, self.recycling["deferred_recycling_untouched"])
        self.assertEqual(1817, self.recycling["deferred_recycling_untouched"])
        self.assertEqual(self.catalog["lock_family_count"], self.recycling["family_count"])
        self.assertTrue(
            all(row.get("recycling_candidate") is False for row in self.recycling["families"])
        )
        self.assertEqual(recycling.OUTPUT, common.RECYCLING_DISPOSITION)

    def test_operand_map_covers_the_identity_lock_set(self) -> None:
        self.assertEqual("T47_OPERAND_RUNTIME_MAP", self.operands["status"])
        self.assertEqual(self.catalog["lock_family_count"], self.operands["lock_family_count"])
        self.assertGreaterEqual(self.operands["lock_family_count"], common.MIN_PRODUCTION_FAMILIES)
        self.assertEqual(operands.OUTPUT, common.OPERAND_RUNTIME_MAP)

    def test_vanilla_1_12_ids_map_to_registered_1_21(self) -> None:
        self.assertEqual(
            "minecraft:terracotta",
            identities.vanilla_meta_runtime("minecraft:hardened_clay", 0),
        )
        self.assertEqual(
            "minecraft:short_grass",
            identities.vanilla_meta_runtime("minecraft:grass", 0),
        )
        self.assertEqual(
            "minecraft:sugar_cane",
            identities.vanilla_meta_runtime("minecraft:reeds", 0),
        )
        self.assertEqual(
            "minecraft:melon_slice",
            identities.vanilla_meta_runtime("minecraft:melon", 0),
        )
        self.assertEqual(
            "minecraft:glistering_melon_slice",
            identities.vanilla_meta_runtime("minecraft:speckled_melon", 0),
        )
        retired = set(identities.VANILLA_RENAMES)
        for row in self.operands.get("operands") or []:
            runtime = ((row.get("runtime") or {}).get("id") or "")
            self.assertNotIn(runtime, retired)
