"""Field-lock contract for T46 source→generated equivalence."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t46_equivalence as builder  # noqa: E402
from tools import t46_common as t46  # noqa: E402


class T46EquivalenceFieldLockTest(unittest.TestCase):
    def test_locked_fields_cover_source_runtime_contract(self) -> None:
        self.assertIn("stable_id", builder.LOCKED_FIELDS)
        self.assertIn("shadow_order", builder.LOCKED_FIELDS)
        self.assertIn("item_inputs", builder.LOCKED_FIELDS)
        self.assertIn("fluid_inputs", builder.LOCKED_FIELDS)
        self.assertIn("source_mte_meta", builder.LOCKED_FIELDS)
        self.assertIn("source_fluid", builder.LOCKED_FIELDS)
        self.assertIn("shadow_order", builder.LOCKED_FIELDS)
        self.assertTrue(t46.java_locked_fields_asserted())
        self.assertTrue(t46.equivalence_fields_locked())
        self.assertTrue(t46.java_locked_fields_asserted())

    def test_duration_mutation_is_detected(self) -> None:
        view = {
            "stable_id": "cruciblecraft:t46/deadbeef",
            "shadow_order": 0,
            "duration": 16,
            "eut": 0,
            "special_value": 0,
            "can_be_buffered": True,
            "item_inputs": [{"id": "minecraft:sunflower", "count": 1, "action": "consume"}],
            "item_outputs": [],
            "fluid_inputs": [{"id": "minecraft:water", "amount": 144}],
            "fluid_outputs": [{"id": "cruciblecraft:sunflower_oil", "amount": 144}],
            "output_chances": [],
        }
        mutated = dict(view)
        mutated["duration"] = 99
        self.assertNotEqual(builder._comparable(view), builder._comparable(mutated))
        mutated_fluid = dict(view)
        mutated_fluid["fluid_inputs"] = [{"id": "minecraft:water", "amount": 1}]
        self.assertNotEqual(builder._comparable(view), builder._comparable(mutated_fluid))
        mutated_item = dict(view)
        mutated_item["item_inputs"] = [
            {"id": "minecraft:stick", "count": 1, "action": "consume"}
        ]
        self.assertNotEqual(builder._comparable(view), builder._comparable(mutated_item))

    def test_written_artifact_is_mutation_sensitive(self) -> None:
        if not t46.EQUIVALENCE.is_file():
            raise unittest.SkipTest("T46 equivalence artifact is not on disk yet")
        document = t46.load_json(t46.EQUIVALENCE)
        self.assertEqual("T46_EQUIVALENCE", document.get("status"))
        self.assertTrue(document.get("mutation_sensitive"))
        self.assertEqual(list(builder.LOCKED_FIELDS), document.get("locked_fields"))
        self.assertEqual(
            t46.production_relation_count(),
            len(document.get("relations") or []),
        )
