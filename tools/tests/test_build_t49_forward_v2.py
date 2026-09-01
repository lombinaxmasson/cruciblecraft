#!/usr/bin/env python3
"""Contract tests for T49 forward-v2 identity/runtime append-only wiring."""
from __future__ import annotations

import unittest

from tools import t35_common as t35
from tools.recipe_bulk import identity as identity_v1
from tools.recipe_bulk import identity_v2
from tools.recipe_bulk import runtime_v2
from tools.recipe_bulk.waves import FORWARD_COMPILE_ORDER, recipe_wave


class T49ForwardV2Test(unittest.TestCase):
    def test_delta_order_appends_t49_after_t48(self) -> None:
        self.assertEqual(("T46", "T47", "T48", "T49"), identity_v2.DELTA_ORDER)
        self.assertEqual(("T46", "T47", "T48", "T49"), runtime_v2.DELTA_ORDER)
        self.assertIn("T49", FORWARD_COMPILE_ORDER)
        self.assertLess(
            FORWARD_COMPILE_ORDER.index("T48"),
            FORWARD_COMPILE_ORDER.index("T49"),
        )

    def test_wave_spec_uses_lock_relation_set(self) -> None:
        spec = recipe_wave("T49")
        self.assertEqual("lock_relation_set", spec.archetype)
        self.assertEqual("host_nested", spec.path_layout)
        self.assertEqual("lock", spec.stable_id_policy)
        self.assertEqual("recipe_bulk", spec.compile_authority)
        self.assertEqual("cruciblecraft:t49_bath_exact_multi", spec.default_publication_group)
        self.assertEqual(5, spec.expected_family_count)
        self.assertEqual(95, spec.expected_relation_count)
        lock_path = (
            t35.TOOLS / "waves" / "bath" / "tiny-purified" / "t49_production_lock.json"
        )
        self.assertEqual(lock_path, spec.lock_path)

    def test_v1_identity_module_does_not_own_t49(self) -> None:
        self.assertEqual("GLOBAL_BUILD_IDENTITY_LEDGER_V1", identity_v1.STATUS)
        self.assertNotIn(
            "T49",
            {wave_id for wave_id, _path in identity_v1.OPERAND_MAPS},
        )


if __name__ == "__main__":
    unittest.main()
