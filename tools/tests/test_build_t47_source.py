#!/usr/bin/env python3
"""Contract tests for the T47 Bath remainder source freeze."""
from __future__ import annotations

import re
import unittest

from tools import build_t47_bath_source as source
from tools import t47_common as common

_ASSIGNED_RE = re.compile(r'"assignment"\s*:\s*\{\s*"assigned"\s*:\s*(\d+)', re.S)
_STATUS_RE = re.compile(r'"status": "(T47_BATH_SOURCE_(?:FROZEN|BLOCKED))"')


def _source_assigned_without_relations() -> int:
    with common.SOURCE.open("r", encoding="utf-8") as handle:
        chunk = handle.read(2048)
    match = _ASSIGNED_RE.search(chunk)
    if match is None:
        raise AssertionError("T47 source header is missing assignment.assigned")
    return int(match.group(1))


def _source_status_without_relations() -> str:
    with common.SOURCE.open("rb") as handle:
        handle.seek(0, 2)
        size = handle.tell()
        handle.seek(max(0, size - 524288))
        tail = handle.read().decode("utf-8")
    match = _STATUS_RE.search(tail)
    if match is None:
        raise AssertionError("T47 source tail is missing status")
    return match.group(1)


class T47SourceTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.receipt = common.load_json(common.RECEIPT)
        cls.review = common.load_json(common.REVIEW)
        cls.pack = common.load_json(common.SOURCE_PACK)

    def test_status_is_frozen_without_loading_relations(self) -> None:
        self.assertEqual("T47_BATH_SOURCE_FROZEN", _source_status_without_relations())
        self.assertEqual("T47_BATH_SOURCE_RECEIPT", self.receipt["status"])
        self.assertEqual("T47_BATH_SOURCE_REVIEW", self.review["status"])
        self.assertEqual("T47_SOURCE_PACK_FROZEN", self.pack["status"])

    def test_receipt_skip_is_not_pass(self) -> None:
        self.assertIs(True, self.receipt["skip_is_not_pass"])
        self.assertIs(True, self.receipt["full_replay"])
        self.assertEqual("full_replay", self.receipt["proof_tier"])
        self.assertIs(True, self.pack["full_replay"]["skip_is_not_pass"])

    def test_assigned_and_review_relation_count_are_47894(self) -> None:
        assigned = _source_assigned_without_relations()
        self.assertEqual(common.CANDIDATE_RELATION_COUNT, assigned)
        self.assertEqual(common.CANDIDATE_RELATION_COUNT, self.review["relation_count"])
        self.assertEqual(common.CANDIDATE_RELATION_COUNT, self.pack["catalog"]["relations"])
        self.assertEqual(common.CANDIDATE_FAMILY_COUNT, self.pack["catalog"]["families"])
        self.assertEqual([], self.review["r0_reconstruction_blockers"])

    def test_pack_is_frozen_and_rebuilds_from_receipt_review(self) -> None:
        rebuilt = source.build_source_pack(
            {
                "status": "T47_BATH_SOURCE_FROZEN",
                "work_set": {
                    "family_count": self.pack["catalog"]["families"],
                    "selection_sha256": self.pack["catalog"]["selection_sha256"],
                    "source_rows": self.pack["catalog"]["relations"],
                },
            },
            self.receipt,
            self.review,
        )
        self.assertEqual("T47_SOURCE_PACK_FROZEN", rebuilt["status"])
        self.assertEqual("T47_SOURCE_PACK_FROZEN", self.pack["status"])
        self.assertEqual(self.pack["catalog"], rebuilt["catalog"])
        self.assertEqual(self.receipt["compact_sha256"], self.pack["files"]["source"]["sha256"])
        self.assertEqual(common.relative(common.RECEIPT), self.pack["files"]["receipt"]["path"])
        self.assertEqual(common.relative(common.REVIEW), self.pack["files"]["review"]["path"])
        self.assertEqual(common.HOST, self.pack["host"])
        self.assertEqual(source.OUTPUT, common.SOURCE)
        self.assertEqual(source.RECEIPT, common.RECEIPT)
        self.assertEqual(source.REVIEW, common.REVIEW)
        self.assertEqual(source.SOURCE_PACK, common.SOURCE_PACK)
