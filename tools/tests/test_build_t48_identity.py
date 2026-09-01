#!/usr/bin/env python3
"""Contract tests for T48 remainder identity, fluid, form, and operand freeze."""
from __future__ import annotations

import unittest

from tools import t48_common as common


class T48IdentityTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.catalog = common.load_json(common.IDENTITY_CATALOG)
        cls.fluids = common.load_json(common.FLUID_MAPPING)
        cls.required_forms = common.load_json(common.REQUIRED_FORMS)
        cls.operands = common.load_json(common.OPERAND_RUNTIME_MAP)

    def test_identity_catalog_is_3532_tool_head_and_multiitem(self) -> None:
        identities = list(self.catalog.get("identities") or [])
        self.assertEqual("T48_IDENTITY_CATALOG", self.catalog["status"])
        self.assertEqual(3532, self.catalog["identity_count"])
        self.assertEqual(3532, len(identities))
        kinds = self.catalog.get("kind_counts") or {}
        self.assertEqual(3461, kinds.get("tool_head"))
        self.assertEqual(71, kinds.get("multiitem"))
        runtime_ids = [str(row["runtime_id"]) for row in identities]
        self.assertEqual(3532, len(set(runtime_ids)))
        self.assertTrue(all(item.startswith("cruciblecraft:") for item in runtime_ids))
        self.assertTrue(
            all(
                item.startswith("cruciblecraft:gt_tool_head/")
                or item.startswith("cruciblecraft:gt_multiitem/")
                or item.startswith("minecraft:")
                for item in runtime_ids
            )
        )
        self.assertEqual(2, int(self.catalog.get("reused_alias_count") or 0))
        self.assertFalse(
            any(item.startswith(("gregtech:", "gregapi:")) for item in runtime_ids)
        )

    def test_required_forms_are_frozen_and_nonempty(self) -> None:
        self.assertEqual("T48_REQUIRED_FORMS_FROZEN", self.required_forms["status"])
        self.assertEqual(
            1679,
            int((self.required_forms.get("counts") or {}).get("required_form_pairs") or 0),
        )

    def test_fluid_mapping_has_no_water_placeholders(self) -> None:
        self.assertEqual("T48_BATH_FLUID_MAPPING", self.fluids["status"])
        self.assertEqual(0, int((self.fluids.get("counts") or {}).get("mapped") or 0))

    def test_operand_map_covers_locked_families(self) -> None:
        self.assertEqual(145, int(self.operands.get("lock_family_count") or 0))
        self.assertGreater(int(self.operands.get("operand_count") or 0), 0)


if __name__ == "__main__":
    unittest.main()
