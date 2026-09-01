#!/usr/bin/env python3
"""Contract tests for T48 forward-v2 identity/runtime append-only wiring."""
from __future__ import annotations

import unittest

from tools import t35_common as t35
from tools.recipe_bulk import identity as identity_v1
from tools.recipe_bulk import identity_v2
from tools.recipe_bulk import runtime_v2
from tools.recipe_bulk.waves import FORWARD_COMPILE_ORDER, recipe_wave


class T48ForwardV2Test(unittest.TestCase):
    def test_delta_order_keeps_t46_then_t47_then_t48(self) -> None:
        self.assertEqual(("T46", "T47", "T48", "T49"), identity_v2.DELTA_ORDER)
        self.assertEqual(("T46", "T47", "T48", "T49"), runtime_v2.DELTA_ORDER)
        self.assertIn("T48", FORWARD_COMPILE_ORDER)

    def test_wave_spec_uses_lock_relation_set(self) -> None:
        spec = recipe_wave("T48")
        self.assertEqual("lock_relation_set", spec.archetype)
        self.assertEqual("host_nested", spec.path_layout)
        self.assertEqual("lock", spec.stable_id_policy)
        self.assertEqual("recipe_bulk", spec.compile_authority)
        self.assertEqual("cruciblecraft:t48_bath_exact", spec.default_publication_group)
        self.assertEqual(145, spec.expected_family_count)
        self.assertEqual(34091, spec.expected_relation_count)
        lock_path = t35.TOOLS / "t48_production_lock.json"
        self.assertTrue(lock_path.is_file())
        self.assertEqual(lock_path, spec.lock_path)

    def test_v1_identity_module_does_not_own_t48(self) -> None:
        self.assertEqual("GLOBAL_BUILD_IDENTITY_LEDGER_V1", identity_v1.STATUS)
        self.assertNotIn(
            "T48",
            {wave_id for wave_id, _path in identity_v1.OPERAND_MAPS},
        )

    def test_runtime_delta_does_not_reuse_t46_or_t47_groups(self) -> None:
        delta = runtime_v2.load_delta("T48")
        groups = [str(row.get("publication_group") or "") for row in delta.get("groups") or []]
        self.assertNotIn("cruciblecraft:t46_bath_mte", groups)
        self.assertNotIn("cruciblecraft:t47_bath_exact", groups)
        self.assertNotIn("cruciblecraft:t47_bath_exact_multi", groups)
        for group in groups:
            self.assertTrue(
                group.startswith("cruciblecraft:t48_"),
                group,
            )


if __name__ == "__main__":
    unittest.main()
