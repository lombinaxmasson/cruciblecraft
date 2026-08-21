from __future__ import annotations

import json
import random
import unittest

from tools import build_t27_card_topology as builder
from tools import build_t27_portfolio as portfolio
from tools import t27_common as common


def _item(
    table: str,
    ident: str,
    owner: str,
    *,
    deps: list[dict[str, str]] | None = None,
) -> dict:
    return {
        "axes": {
            "closure": {"evidence": "incomplete", "status": "incomplete"},
            "fidelity": {"evidence": "source", "status": "source_backed"},
            "load": {
                "evidence": "unmeasured",
                "status": "pending",
                "verdict": common.PENDING_LOAD_VERDICT,
            },
        },
        "canonical_id": ident,
        "cc_implementation": "none",
        "closure": "incomplete",
        "disposition": "v1_required",
        "fidelity": "source_backed",
        "load": "pending",
        "load_verdict": common.PENDING_LOAD_VERDICT,
        "owner": owner,
        "reason": "synthetic",
        "recheck_point": "generated card",
        "replacement_condition": "close the identity",
        "source_artifact": "tools/t27_portfolio.json",
        "table": table,
        "dependencies": deps or [],
    }


class T27CardTopologyTest(unittest.TestCase):
    def test_committed_topology_is_current_and_check_is_read_only(self) -> None:
        on_disk = json.loads(builder.OUTPUT.read_text(encoding="utf-8"))
        expected = builder.build()
        self.assertEqual(expected, on_disk)
        before = builder.OUTPUT.read_bytes()
        self.assertEqual([], builder.check())
        self.assertEqual(before, builder.OUTPUT.read_bytes())

    def test_generated_cards_cover_only_unclosed_v1_work_set(self) -> None:
        document = builder.build()
        self.assertEqual("T27_CARD_TOPOLOGY", document["status"])
        self.assertFalse(document["started"])
        self.assertIsNone(document["rc_number"])
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, document["publication_delta"])
        self.assertEqual(28, document["start_number"])
        cards = document["cards"]
        self.assertEqual(len(cards), document["card_count"])
        self.assertEqual(
            [f"T{28 + index}" for index in range(len(cards))],
            [card["id"] for card in cards],
        )
        covered = {
            (item["table"], item["canonical_id"])
            for card in cards
            for item in card["identities"]
        }
        work = {
            (item["table"], item["canonical_id"])
            for item in document["excluded"]["work_set"]
        }
        self.assertEqual(work, covered)
        self.assertEqual(set(), covered)
        self.assertNotIn(("deferred_open_items", "O-36"), covered)
        self.assertNotIn(("multiblock_kinds", "crucible"), covered)
        self.assertNotIn(("deferred_open_items", "crucible"), covered)
        self.assertNotIn(("deferred_open_items", "O-41"), covered)
        self.assertNotIn("T24-F003", document["owner_resolution"])
        self.assertIn("T24-F003", document["excluded"]["rc_recheck"])
        self.assertIn("crucible", document["excluded"]["canonical_coverage"])
        closed_ids = {
            (item["table"], item["canonical_id"])
            for item in document["excluded"]["closed_v1"]
        }
        self.assertIn(("multiblock_kinds", "centrifuge"), closed_ids)
        self.assertIn(("multiblock_kinds", "crucible"), closed_ids)
        self.assertTrue(all(not card["started"] for card in cards))
        self.assertEqual(0, document["validators"]["rc_numbers"])
        self.assertEqual(0, document["validators"]["started_cards"])
        contract = common.load_json(builder.CONTRACT)
        self.assertIsNone(contract["execution_policy"]["t28_plus_card_count"])
        self.assertIsNone(contract["t28_plus_generation"]["card_count"])

    def test_owner_keys_resolve_once_and_respect_dependencies(self) -> None:
        document = builder.build()
        owners = []
        for card in document["cards"]:
            owners.extend(card["owner_keys"])
        self.assertEqual(sorted(owners), sorted(document["owner_resolution"]))
        self.assertEqual(len(owners), len(set(owners)))
        self.assertEqual([], document["cards"])
        self.assertEqual({}, document["owner_resolution"])

    def test_shuffled_work_set_keeps_stable_card_bytes(self) -> None:
        table_docs = {
            table: common.load_json(portfolio.output_path(table))
            for table in common.TABLE_SPECS
        }
        open_items = common.load_json(portfolio.open_items_path())
        work = builder.select_work_set(table_docs, open_items)
        edges = builder.work_edges(work, table_docs, open_items)
        expected = common.stable_json(builder.merge_and_number(work, edges))
        shuffled = list(work)
        random.Random(7).shuffle(shuffled)
        actual = common.stable_json(builder.merge_and_number(shuffled, list(reversed(edges))))
        self.assertEqual(expected, actual)

    def test_cycle_and_broken_reference_fail_closed(self) -> None:
        left = _item("multiblock_kinds", "alpha", "portfolio:v1/alpha")
        right = _item("multiblock_kinds", "beta", "portfolio:v1/beta")
        with self.assertRaisesRegex(ValueError, "cycle"):
            builder.merge_and_number(
                [left, right],
                [
                    ("multiblock_kinds/alpha", "multiblock_kinds/beta"),
                    ("multiblock_kinds/beta", "multiblock_kinds/alpha"),
                ],
            )
        open_items = {"records": []}
        table_docs = {
            "multiblock_kinds": {
                "records": [
                    {
                        "canonical_id": "alpha",
                        "disposition": "v1_required",
                        "axes": {"closure": {"status": "incomplete"}},
                    }
                ]
            }
        }
        dangling = _item(
            "multiblock_kinds",
            "alpha",
            "portfolio:v1/alpha",
            deps=[{"kind": "open_item_id", "id": "missing"}],
        )
        with self.assertRaisesRegex(ValueError, "missing open_item"):
            builder.work_edges([dangling], table_docs, open_items)

    def test_same_owner_and_bounds_merge_to_one_card(self) -> None:
        first = _item("prefixes", "ingotHot", "portfolio:v1/hot_ingot_cooling")
        second = _item(
            "itemgenerator_domains",
            "ITEMGENERATOR.INGOTS_HOT",
            "portfolio:v1/hot_ingot_cooling",
        )
        cards = builder.merge_and_number([second, first], [])
        self.assertEqual(["T28"], [card["id"] for card in cards])
        self.assertEqual(
            [
                ("itemgenerator_domains", "ITEMGENERATOR.INGOTS_HOT"),
                ("prefixes", "ingotHot"),
            ],
            [
                (item["table"], item["canonical_id"])
                for item in cards[0]["identities"]
            ],
        )


if __name__ == "__main__":
    unittest.main()
