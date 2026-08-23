"""Tests for the T35 generated card topology builder."""
from __future__ import annotations

import copy
import json
import random
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT))

from tools import build_t35_card_topology as builder  # noqa: E402
from tools import build_t35_census as census_builder  # noqa: E402
from tools import t27_common as common  # noqa: E402
from tools import t35_common as t35  # noqa: E402


def _item(
    canonical_id: str,
    owner: str,
    *,
    deps: list[dict[str, str]] | None = None,
    domain: str = "exclusion",
    priority: str = "P1",
) -> dict:
    return {
        "axes": {
            "closure": {"evidence": "incomplete", "status": "incomplete"},
            "fidelity": {"evidence": "source", "status": "source_backed"},
            "load": {
                "evidence": "unmeasured",
                "status": "pending",
                "verdict": t35.PENDING_LOAD_VERDICT,
            },
        },
        "canonical_id": canonical_id,
        "closure": "incomplete",
        "dependencies": deps or [{"kind": "fixed_card", "id": "T36"}],
        "disposition": "planned",
        "domain": domain,
        "fidelity": "source_backed",
        "historical_classification": {"t13": None, "t27": None},
        "load": "pending",
        "load_verdict": t35.PENDING_LOAD_VERDICT,
        "owner": owner,
        "portfolio_priority": priority,
        "portfolio_scope": "in_scope_1x",
        "reason": "synthetic storage family",
        "recheck_point": "generated T38+ storage-track card",
        "replacement_condition": "Preserve canonical family boundary.",
        "source": {
            "artifact": "tools/t35_excluded_object_reclaim.json",
            "expanded_rows": 1,
            "record_keys": [],
            "revision": t35.SOURCE_REVISION,
            "source_sites": 1,
        },
    }


class T35CardTopologyTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        if not census_builder.OUTPUT.is_file():
            raise unittest.SkipTest("t35_census.json not generated")
        cls.document = builder.build()

    def test_build_is_deterministic(self) -> None:
        first = builder.build()
        second = builder.build()
        self.assertEqual(common.stable_json(first), common.stable_json(second))

    def test_committed_topology_is_current_and_check_is_read_only(self) -> None:
        if not builder.OUTPUT.is_file():
            self.skipTest("artifact not yet generated")
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_generated_cards_cover_storage_work_set_minus_t37_pilot(self) -> None:
        document = self.document
        self.assertEqual("T35_CARD_TOPOLOGY", document["status"])
        self.assertFalse(document["started"])
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, document["publication_delta"])
        self.assertEqual(38, document["start_number"])
        cards = document["cards"]
        self.assertEqual(len(cards), document["generated_card_count"])
        self.assertEqual(
            [f"T{38 + index}" for index in range(len(cards))],
            [card["id"] for card in cards],
        )
        covered = {item["canonical_id"] for card in cards for item in card["identities"]}
        census = common.load_json(t35.CENSUS)
        t37_ids = set((census.get("t37_pilot") or {}).get("family_ids") or [])
        expected = {
            cid
            for cid in (census.get("work_sets") or {}).get("mandatory_p0_p1") or []
            if cid not in t37_ids
        }
        self.assertEqual(expected, covered)
        self.assertEqual(50, len(document["exclusion"]["t37_pilot_families"]))
        self.assertTrue(all(not card["started"] for card in cards))
        self.assertEqual(0, document["validators"]["started_generated_cards"])
        self.assertEqual(0, document["validators"]["t37_pilot_in_generated_cards"])

    def test_fixed_nodes_preserved_and_not_renumbered(self) -> None:
        document = self.document
        fixed = {card["id"]: card for card in document["fixed_cards"]}
        self.assertEqual({"T36", "T37"}, set(fixed))
        self.assertFalse(fixed["T36"]["generated_card"])
        self.assertFalse(fixed["T37"]["generated_card"])
        self.assertEqual("cruciblecraft:assembler", fixed["T37"]["pilot"]["host_map"])
        self.assertNotIn("T36", [card["id"] for card in document["cards"]])
        self.assertNotIn("T37", [card["id"] for card in document["cards"]])

    def test_owner_keys_resolve_once_and_all_depend_on_t36_t37(self) -> None:
        document = self.document
        owners = []
        for card in document["cards"]:
            owners.extend(card["owner_keys"])
        self.assertEqual(sorted(owners), sorted(document["owner_resolution"]))
        self.assertEqual(len(owners), len(set(owners)))
        for card in document["cards"]:
            self.assertIn("T36", card["depends_on"])
            self.assertIn("T37", card["depends_on"])
            self.assertNotBlank(card["replacement_conditions"])
            self.assertNotBlank(card["recheck_points"])
            for lineage in card["source_lineage"]:
                self.assertTrue(str(lineage.get("replacement_condition") or "").strip())
                self.assertTrue(str(lineage.get("recheck_point") or "").strip())
                self.assertEqual(lineage["owner"], card["owner_keys"][0])

    def test_epoch_a_contract_is_explicit(self) -> None:
        document = self.document
        self.assertEqual(t35.TOPOLOGY_EPOCH, document["topology_epoch"])
        self.assertEqual(t35.NUMBERING_POLICY, document["numbering_policy"])
        self.assertEqual(t35.GLOBAL_EXECUTION_GATE, document["global_execution_gate"])
        self.assertEqual("append_only", document["epoch_b"]["policy"])
        self.assertFalse(document["epoch_b"]["may_renumber_epoch_a"])
        self.assertEqual(0, document["epoch_b"]["admitted_family_count"])
        self.assertEqual(0, document["epoch_b"]["appended_card_count"])
        self.assertEqual(0, document["validators"]["t37_gate_bypass"])

    def test_admitted_p2_appends_without_renumbering_epoch_a(self) -> None:
        census = copy.deepcopy(common.load_json(t35.CENSUS))
        record = census["identities"]["exclusion/fluid_container"]
        record["portfolio_scope"] = "in_scope_1x"
        record["disposition"] = "planned"
        record["p2_admission"] = t35.P2_ADMISSION
        work, _ = builder.select_work_set(census)
        builder.validate_work_coverage(work, census)
        epoch_a, epoch_b = builder.partition_work_set(work)
        self.assertEqual(
            ["exclusion/fluid_container"],
            [item["canonical_id"] for item in epoch_b],
        )
        edges, fixed_deps = builder.work_edges(work, census["identities"])
        cards = builder.merge_and_number(work, edges, fixed_deps)
        baseline_ids = [card["id"] for card in self.document["cards"]]
        self.assertEqual(baseline_ids, [card["id"] for card in cards[: len(baseline_ids)]])
        self.assertEqual("T47", cards[-1]["id"])
        self.assertEqual(
            ["exclusion/fluid_container"],
            [item["canonical_id"] for item in cards[-1]["identities"]],
        )
        self.assertIn("T37", cards[-1]["depends_on"])
        self.assertEqual(
            [],
            t35.validate_epoch_b_append_only(self.document["cards"], cards),
        )

    def test_epoch_a_coverage_still_rejects_p0_p1_drift(self) -> None:
        census = common.load_json(t35.CENSUS)
        work, _ = builder.select_work_set(census)
        drifted = [item for item in work if item["canonical_id"] != work[0]["canonical_id"]]
        with self.assertRaisesRegex(ValueError, "work set mismatch"):
            builder.validate_work_coverage(drifted, census)

    def assertNotBlank(self, values: list[str]) -> None:  # noqa: N802
        self.assertTrue(values)
        for value in values:
            self.assertTrue(str(value).strip())

    def test_shuffled_work_set_keeps_stable_card_bytes(self) -> None:
        census = common.load_json(t35.CENSUS)
        work, _ = builder.select_work_set(census)
        identities = census["identities"]
        edges, fixed_deps = builder.work_edges(work, identities)
        expected = common.stable_json(builder.merge_and_number(work, edges, fixed_deps))
        shuffled = list(work)
        random.Random(11).shuffle(shuffled)
        actual = common.stable_json(
            builder.merge_and_number(shuffled, list(reversed(edges)), fixed_deps)
        )
        self.assertEqual(expected, actual)

    def test_cycle_and_broken_reference_fail_closed(self) -> None:
        left = _item("exclusion/alpha", "portfolio:storage/alpha")
        right = _item("exclusion/beta", "portfolio:storage/beta")
        with self.assertRaisesRegex(ValueError, "cycle"):
            builder.merge_and_number(
                [left, right],
                [
                    ("exclusion/alpha", "exclusion/beta"),
                    ("exclusion/beta", "exclusion/alpha"),
                ],
                {},
            )
        identities = {
            "exclusion/alpha": {
                "disposition": "planned",
                "axes": {"closure": {"status": "incomplete"}},
            }
        }
        dangling = _item(
            "exclusion/alpha",
            "portfolio:storage/alpha",
            deps=[{"kind": "canonical_family", "id": "exclusion/missing"}],
        )
        with self.assertRaisesRegex(ValueError, "missing canonical_family"):
            builder.work_edges([dangling], identities)

    def test_same_owner_and_bounds_merge_to_one_card(self) -> None:
        first = _item("exclusion/drawer", "portfolio:storage/drawer")
        second = _item("exclusion/locker", "portfolio:storage/drawer")
        cards = builder.merge_and_number([second, first], [], {"exclusion/drawer": ["T36"]})
        self.assertEqual(["T38"], [card["id"] for card in cards])
        self.assertEqual(
            ["exclusion/drawer", "exclusion/locker"],
            sorted(item["canonical_id"] for item in cards[0]["identities"]),
        )


if __name__ == "__main__":
    unittest.main()
