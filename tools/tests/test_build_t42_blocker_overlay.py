"""T42 overlay classifier fixtures: buckets, partials, size-not-defer, retags."""
from __future__ import annotations

import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools.build_t42_blocker_overlay import classify_family
from tools.build_t42_disposition_lock import lock_row
from tools import t42_common as common


def _family(**overrides):
    row = {
        "cc_host_map": "cruciblecraft:drying",
        "family_id": "portfolio:track_a/cruciblecraft:drying/gt.recipe.drying#0001",
        "membership_kind": "semantic_template",
        "relation_count": 1,
        "template_key": "gt.recipe.drying#0001",
    }
    row.update(overrides)
    return row


def _operand(**overrides):
    row = {
        "b0_reachable": True,
        "disposition": "proven_equivalent",
        "family_id": "portfolio:track_a/cruciblecraft:drying/gt.recipe.drying#0001",
        "kind": "material_form",
        "reason": "material_form",
        "runtime_id": "cruciblecraft:iron/dust",
        "side": "item_inputs",
        "source": {
            "fluid": None,
            "form": "dust",
            "item": "gregtech:gt.metaitem.01",
            "material": "iron",
            "meta": 1,
            "unique_kind": None,
        },
    }
    row.update(overrides)
    return row


class T42BlockerOverlayTest(unittest.TestCase):
    def test_ready_family_requires_proven_operands_and_b0(self) -> None:
        row = classify_family(
            family=_family(),
            relation_summaries=[{"identity": "ready-id"}],
            operand_rows=[
                _operand(),
                _operand(side="item_outputs", b0_reachable=None),
            ],
            published=set(),
        )
        self.assertEqual("current_closure_ready", row["primary_bucket"])
        self.assertNotIn("partial", row["secondary_blockers"])

    def test_b0_removal_retags_off_ready(self) -> None:
        row = classify_family(
            family=_family(),
            relation_summaries=[{"identity": "ready-id"}],
            operand_rows=[
                _operand(b0_reachable=False, reason="b0_unreachable"),
                _operand(side="item_outputs", b0_reachable=None),
            ],
            published=set(),
        )
        self.assertNotEqual("current_closure_ready", row["primary_bucket"])
        self.assertIn("b0_unreachable_inputs", row["secondary_blockers"])

    def test_missing_form_is_prefix_bucket_not_recycling(self) -> None:
        row = classify_family(
            family=_family(),
            relation_summaries=[{"identity": "form-id"}],
            operand_rows=[
                _operand(
                    kind="missing_form",
                    disposition="needs_current_expression",
                    reason="missing_material_form",
                    runtime_id=None,
                )
            ],
            published=set(),
        )
        self.assertEqual("needs_prefix_or_molten", row["primary_bucket"])
        lock = lock_row(row)
        self.assertEqual("retained_current_execution_gap", lock["disposition"])

    def test_partial_identity_is_never_already_expressed(self) -> None:
        row = classify_family(
            family=_family(relation_count=2),
            relation_summaries=[
                {"identity": "live-a"},
                {"identity": "other-b"},
            ],
            operand_rows=[_operand(), _operand(side="item_outputs", b0_reachable=None)],
            published={"live-a"},
        )
        self.assertNotEqual("already_expressed", row["primary_bucket"])
        self.assertIn("partial", row["secondary_blockers"])
        lock = lock_row(row)
        self.assertEqual("retained_current_execution_gap", lock["disposition"])

    def test_cross_host_published_identities_do_not_count(self) -> None:
        by_host = common.published_relation_identities_by_host()
        distillery = by_host.get("cruciblecraft:distillery") or set()
        drying = by_host.get("cruciblecraft:drying") or set()
        mixer = by_host.get("cruciblecraft:mixer") or set()
        self.assertTrue(distillery)
        self.assertTrue(mixer)
        self.assertFalse(distillery & drying)
        water_to_distilled = next(
            (
                identity
                for identity in distillery
                if "minecraft:water" in identity
                and "cruciblecraft:water_distilled" in identity
            ),
            None,
        )
        self.assertIsNotNone(water_to_distilled)
        self.assertNotIn(water_to_distilled, drying)

    def test_identity_collision_all_relations_already_expressed(self) -> None:
        row = classify_family(
            family=_family(),
            relation_summaries=[{"identity": "live-a"}],
            operand_rows=[_operand(), _operand(side="item_outputs", b0_reachable=None)],
            published={"live-a"},
        )
        self.assertEqual("already_expressed", row["primary_bucket"])

    def test_bath_size_alone_does_not_defer(self) -> None:
        row = classify_family(
            family=_family(
                cc_host_map="cruciblecraft:bath",
                family_id="portfolio:track_a/cruciblecraft:bath/gt.recipe.bath#0001",
                relation_count=1116,
                template_key="gt.recipe.bath#0001",
            ),
            relation_summaries=[{"identity": f"bath-{index}"} for index in range(3)],
            operand_rows=[
                _operand(),
                _operand(side="item_outputs", b0_reachable=None),
            ],
            published=set(),
        )
        self.assertEqual("current_closure_ready", row["primary_bucket"])
        lock = lock_row(row)
        self.assertEqual("retained_current_execution_gap", lock["disposition"])
        self.assertNotIn("later:", str(lock.get("future_owner") or ""))

    def test_combinatorial_template_stays_unproven(self) -> None:
        row = classify_family(
            family=_family(
                cc_host_map="cruciblecraft:assembler",
                family_id="portfolio:track_a/cruciblecraft:assembler/gt.recipe.assembler#0000",
                template_key="gt.recipe.assembler#0000",
            ),
            relation_summaries=[{"identity": "combo"}],
            operand_rows=[_operand()],
            published=set(),
        )
        self.assertEqual("combinatorial_unproven", row["primary_bucket"])
        lock = lock_row(row)
        self.assertEqual("phase_deferred", lock["disposition"])
        self.assertEqual("later:combinatorial/assembler", lock["future_owner"])
        self.assertTrue(lock["recheck_condition"])
        self.assertTrue(lock["evidence_root_sha256"])

    def test_unique_object_is_not_a_missing_form(self) -> None:
        row = classify_family(
            family=_family(
                cc_host_map="cruciblecraft:smelter",
                family_id="portfolio:track_a/cruciblecraft:smelter/gt.recipe.smelter#0001",
                template_key="gt.recipe.smelter#0001",
            ),
            relation_summaries=[{"identity": "mte"}],
            operand_rows=[
                _operand(
                    kind="unique_object",
                    disposition="phase_deferred",
                    reason="unique_object:mte",
                    runtime_id=None,
                    source={
                        "fluid": None,
                        "form": None,
                        "item": "gregtech:gt.multitileentity",
                        "material": None,
                        "meta": 1000,
                        "unique_kind": "mte",
                    },
                )
            ],
            published=set(),
        )
        self.assertEqual("needs_unique_block_or_mte", row["primary_bucket"])
        self.assertNotEqual("needs_prefix_or_molten", row["primary_bucket"])
        lock = lock_row(row)
        self.assertEqual("retained_current_execution_gap", lock["disposition"])
        self.assertNotIn("later:", str(lock.get("future_owner") or ""))

    def test_residual_unique_without_kind_is_unmapped(self) -> None:
        row = classify_family(
            family=_family(),
            relation_summaries=[{"identity": "residual"}],
            operand_rows=[
                _operand(
                    kind="cc_static",
                    disposition="needs_current_expression",
                    reason="runtime_id_not_registered",
                    runtime_id="cruciblecraft:missing",
                    b0_reachable=False,
                )
            ],
            published=set(),
        )
        self.assertEqual("needs_unique_block_or_mte", row["primary_bucket"])
        self.assertIn("unmapped_operands", row["secondary_blockers"])
        self.assertEqual([], row["unique_kinds"])

    def test_unique_object_without_kind_is_refused(self) -> None:
        with self.assertRaises(ValueError):
            classify_family(
                family=_family(),
                relation_summaries=[{"identity": "broken"}],
                operand_rows=[
                    _operand(
                        kind="unique_object",
                        disposition="phase_deferred",
                        reason="unique_object:",
                        runtime_id=None,
                        source={
                            "fluid": None,
                            "form": None,
                            "item": "gregtech:gt.meta.armor",
                            "material": "iron",
                            "meta": 260,
                            "unique_kind": None,
                        },
                    )
                ],
                published=set(),
            )

    def test_gt_prefix_kind_is_not_a_cc_form(self) -> None:
        catalogs = common.load_runtime_catalogs()
        mapped = common.map_item_source(
            {"item": "gregtech:gt.meta.armor", "meta": 260, "count": 1},
            catalogs,
        )
        self.assertEqual("unique_object", mapped["kind"])
        self.assertEqual("armor", mapped["unique_kind"])
        self.assertNotEqual("armor", mapped.get("form"))
        circuit = common.map_item_source(
            {"item": "gregapi:gt.integrated_circuit", "meta": 1, "count": 0},
            catalogs,
        )
        self.assertEqual("cc_static", circuit["kind"])
        self.assertEqual("cruciblecraft:programmed_circuit", circuit["runtime_id"])
        self.assertEqual(
            {"cruciblecraft:circuit_config": 1},
            circuit.get("components"),
        )

    def test_overlay_unique_kind_counts_are_not_all_mte(self) -> None:
        if not common.BLOCKER_OVERLAY.is_file():
            self.skipTest("t42_blocker_overlay.json not generated")
        overlay = common.load_json(common.BLOCKER_OVERLAY)
        if "unique_kind_counts" not in overlay:
            self.skipTest("overlay not repaired yet")
        counts = overlay.get("unique_kind_counts") or {}
        unique_bucket = int(
            overlay.get("unique_bucket_family_count")
            or overlay.get("bucket_counts", {}).get("needs_unique_block_or_mte")
            or 0
        )
        self.assertTrue(counts)
        self.assertLess(int(counts.get("mte") or 0), unique_bucket)
        with_kind = int(overlay.get("unique_object_kind_family_count") or 0)
        residual = int(overlay.get("unique_residual_without_kind_count") or 0)
        self.assertEqual(with_kind + residual, unique_bucket)
        self.assertLess(with_kind, unique_bucket)
        self.assertEqual(1, int(overlay.get("partial_family_count") or 0))

    def test_wave_candidates_are_not_authoritative(self) -> None:
        if not common.WAVE_CANDIDATES.is_file():
            self.skipTest("t42_wave_candidates.json not generated")
        document = common.load_json(common.WAVE_CANDIDATES)
        self.assertFalse(document["authoritative"])
        self.assertEqual(152, document["priority_intent"]["t35_host_family_total"])
        self.assertEqual("cruciblecraft:drying", document["priority_intent"]["host"])


if __name__ == "__main__":
    unittest.main()
