"""T42-Owner owner tracks, molten-output recovery proof, and topology gates."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import t42_common as t42
from tools import t42_owner_common as common
from tools import build_t42_owner_disposition_lock as lock_builder
from tools import build_t42_owner_readiness as readiness_builder
from tools import build_t42_owner_track_overlay as overlay_builder


def _family(**overrides):
    row = {
        "family_id": "portfolio:track_a/cruciblecraft:smelter/gt.recipe.smelter#0001",
        "host": "cruciblecraft:smelter",
        "missing_fluids": [],
        "missing_forms": [],
        "primary_bucket": "needs_unique_block_or_mte",
        "relation_count": 1,
        "secondary_blockers": ["unique_objects"],
        "template_key": "gt.recipe.smelter#0001",
        "unique_kinds": ["mte"],
        "unique_objects": ["gregtech:gt.multitileentity@1000"],
        "unsupported_semantics": [],
    }
    row.update(overrides)
    return row


class T42OwnerTest(unittest.TestCase):
    def test_molten_output_is_recovery_shape_when_registered(self) -> None:
        relation = {
            "item_inputs": [{"item": "gregtech:gt.multitileentity", "meta": 1000}],
            "item_input_actions": [{"kind": "consume"}],
            "item_outputs": [],
            "fluid_inputs": [],
            "fluid_outputs": [{"fluid": "molten.iron", "amount": 144}],
            "recipe_index": 42,
            "row_sha256": "fixture",
            "source_map": "gt.recipe.smelter",
        }
        with mock.patch.object(
            t42,
            "map_item_source",
            return_value={
                "item": "gregtech:gt.multitileentity",
                "kind": "unique_object",
                "material": None,
                "meta": 1000,
                "runtime_id": None,
                "unique_kind": "mte",
            },
        ):
            evidence = overlay_builder._recovery_relation(
                relation,
                catalogs={
                    "fluid_mapping": {
                        "molten.iron": {
                            "cc_fluid_id": "cruciblecraft:molten_iron",
                            "cc_material": "iron",
                            "derivation": "molten_fluid",
                            "disposition": "mapped",
                            "material_id": 1,
                        }
                    },
                    "fluid_disposition": {"molten.iron": "mapped"},
                    "fluid_to_cc": {
                        "molten.iron": "cruciblecraft:molten_iron"
                    },
                },
                registry={"cruciblecraft:molten_iron"},
            )
        self.assertTrue(evidence["proven"])
        self.assertEqual(["iron"], evidence["material_outputs"])
        self.assertEqual(
            "cruciblecraft:molten_iron",
            evidence["fluid_output_mappings"][0]["cc_fluid_id"],
        )
        self.assertEqual([], evidence["shape_blockers"])

    def test_recovery_uses_authoritative_fluid_material_identity(self) -> None:
        relation = {
            "item_inputs": [{"item": "gregtech:gt.multitileentity", "meta": 1000}],
            "item_input_actions": [{"kind": "consume"}],
            "item_outputs": [],
            "fluid_inputs": [],
            "fluid_outputs": [{"fluid": "molten hsla", "amount": 144}],
            "recipe_index": 42,
            "row_sha256": "fixture",
            "source_map": "gt.recipe.smelter",
        }
        with mock.patch.object(
            t42,
            "map_item_source",
            return_value={
                "item": "gregtech:gt.multitileentity",
                "kind": "unique_object",
                "material": None,
                "meta": 1000,
                "runtime_id": None,
                "unique_kind": "mte",
            },
        ):
            evidence = overlay_builder._recovery_relation(
                relation,
                catalogs={
                    "fluid_mapping": {
                        "molten hsla": {
                            "cc_fluid_id": "cruciblecraft:molten_hslasteel",
                            "cc_material": "hslasteel",
                            "derivation": "molten_fluid",
                            "disposition": "mapped",
                            "material_id": 8637,
                        }
                    },
                    "fluid_disposition": {"molten hsla": "mapped"},
                    "fluid_to_cc": {
                        "molten hsla": "cruciblecraft:molten_hslasteel"
                    },
                },
                registry={"cruciblecraft:molten_hslasteel"},
            )
        self.assertTrue(evidence["proven"])
        self.assertEqual(["hslasteel"], evidence["material_outputs"])

    def test_mte_kind_or_host_alone_is_not_recycling_proof(self) -> None:
        owner, state, secondary = overlay_builder._owner_assignment(_family(), None)
        self.assertEqual("recycling/evidence_needed", owner)
        self.assertEqual("recovery_evidence_needed", state)
        self.assertEqual([], secondary)
        locked = lock_builder.lock_row(
            {
                **_family(),
                "current_owner": owner,
                "owner_state": state,
                "secondary_owner_tracks": secondary,
            },
            None,
        )
        self.assertEqual("retained_current_execution_gap", locked["disposition"])
        self.assertEqual("recycling/evidence_needed", locked["current_owner"])
        self.assertIsNone(locked["future_owner"])

    def test_kind_only_object_expression_stays_in_execution_gap(self) -> None:
        owner, state, secondary = overlay_builder._owner_assignment(
            _family(
                unique_kinds=["block"],
                unique_objects=["gregtech:gt.block.machine@1"],
            ),
            None,
        )
        self.assertEqual("object_expression/block", owner)
        self.assertEqual("object_boundary_needed", state)
        locked = lock_builder.lock_row(
            {
                **_family(
                    unique_kinds=["block"],
                    unique_objects=["gregtech:gt.block.machine@1"],
                ),
                "current_owner": owner,
                "owner_state": state,
                "secondary_owner_tracks": secondary,
            },
            None,
        )
        self.assertEqual("retained_current_execution_gap", locked["disposition"])
        self.assertIsNone(locked["future_owner"])

    def test_explicit_object_expression_evidence_can_defer_one_family(self) -> None:
        family = _family(
            unique_kinds=["block"],
            unique_objects=["gregtech:gt.block.machine@1"],
        )
        owner, state, secondary = overlay_builder._owner_assignment(family, None)
        locked = lock_builder.lock_row(
            {
                **family,
                "current_owner": owner,
                "owner_state": state,
                "secondary_owner_tracks": secondary,
            },
            None,
            {
                "future_owner": "later:object_expression/block",
                "object_boundary_root_sha256": "boundary-proof",
                "runtime_behavior_root_sha256": "runtime-proof",
                "recheck_condition": "recheck registration and behavior",
            },
        )
        self.assertEqual("phase_deferred", locked["disposition"])
        self.assertIsNone(locked["current_owner"])
        self.assertEqual("later:object_expression/block", locked["future_owner"])

    def test_residual_without_unique_kind_is_not_an_object_track(self) -> None:
        owner, state, secondary = overlay_builder._owner_assignment(
            _family(
                unique_kinds=[],
                unique_objects=[],
                secondary_blockers=["unmapped_operands"],
            ),
            None,
        )
        self.assertEqual("identity_mapping/unmapped", owner)
        self.assertEqual("identity_mapping_needed", state)
        self.assertEqual([], secondary)

    def test_generated_owner_lock_has_exclusive_owners(self) -> None:
        if not common.DISPOSITION_LOCK.is_file():
            self.skipTest("T42-Owner lock not generated")
        lock = common.load_json(common.DISPOSITION_LOCK)
        for row in lock["families"]:
            if row["disposition"] == "phase_deferred":
                self.assertIsNone(row["current_owner"])
                self.assertTrue(str(row["future_owner"]).startswith("later:"))
            else:
                self.assertTrue(common.valid_owner_track(row["current_owner"]))
                self.assertIsNone(row["future_owner"])

    def test_readiness_derives_status_and_topology_gate(self) -> None:
        if not common.READINESS.is_file():
            self.skipTest("T42-Owner readiness not generated")
        document = readiness_builder.build()
        failed = sorted(name for name, passed in document["gates"].items() if not passed)
        self.assertEqual(failed, document["failed_gates"])
        self.assertEqual(
            "T42_OWNER_READY" if not failed else "T42_OWNER_BLOCKED",
            document["status"],
        )
        self.assertTrue(document["gates"]["topology_gates_t43_and_storage"])


if __name__ == "__main__":
    unittest.main()
