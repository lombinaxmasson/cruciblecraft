from __future__ import annotations

import hashlib
import json
import unittest

from tools import build_t18_o37_identity_projection as builder


class T18O37IdentityProjectionTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.document = json.loads(
            builder.OUTPUT.read_text(encoding="utf-8")
        )

    def test_fixed_revision_negative_evidence_closes_o37_permanently(self):
        document = self.document
        self.assertEqual("O37_CLOSED", document["status"])
        self.assertEqual("DESIGN_POLICY", document["resolution"])
        self.assertEqual(
            "O37_CLOSED_PERMANENT_DESIGN_POLICY",
            document["closure"],
        )
        self.assertEqual(
            0,
            document["negative_evidence"][
                "direct_binding_candidate_count"
            ],
        )
        inventory = document["negative_evidence"]["token_inventory"]
        self.assertEqual(2, inventory["liquid_medium_oil"][
            "occurrence_count"
        ])
        self.assertEqual(1, inventory["CrudeOil"]["occurrence_count"])
        self.assertEqual(0, inventory["MT.CrudeOil"]["occurrence_count"])

    def test_search_receipts_pin_paths_blobs_symbols_and_lines(self):
        evidence = {
            row["symbol"]: row for row in self.document["symbol_evidence"]
        }
        self.assertEqual(
            {
                "Loader_Fluids#run",
                "FL.Oil_Medium",
                "MT.CrudeOil",
                "Loader_Fuels#run",
            },
            set(evidence),
        )
        self.assertEqual(
            "23d79e9be0b2a3587d83c21f8dd0301b62cfb176",
            evidence["Loader_Fluids#run"]["blob"],
        )
        self.assertEqual(
            "1681c2072f28ad2d31bc285aef1c6fd2a3bcef92",
            evidence["MT.CrudeOil"]["blob"],
        )
        self.assertTrue(all(row["line"] > 0 for row in evidence.values()))

    def test_runtime_lock_keeps_one_cc_fluid_and_material_source_only(self):
        lock = self.document["runtime_identity_lock"]
        self.assertEqual(
            "cruciblecraft:crude_oil", lock["worldgen_material"]
        )
        self.assertEqual(
            "liquid_medium_oil", lock["distillery_source_fluid"]
        )
        self.assertEqual(
            "cruciblecraft:crude_oil",
            lock["distillery_runtime_input"],
        )
        self.assertEqual(
            lock["distillery_runtime_input"],
            lock["registered_runtime_fluid"],
        )
        self.assertEqual(
            "SOURCE_MATERIAL_LAYER_ONLY",
            lock["material_9852_role"],
        )
        self.assertEqual("NONE", lock["t9_identity_migration"])
        self.assertFalse(lock["duplicate_registration_allowed"])
        self.assertFalse(lock["fluid_id_changed"])
        self.assertEqual(0, lock["publication_delta"])

    def test_binding_candidate_detector_fails_closed_on_direct_statement(self):
        policy = builder.load(builder.POLICY)
        source = (
            b"class Candidate { void bind() { "
            b"register(\"liquid_medium_oil\", MT.CrudeOil); } }"
        )
        candidates = builder.direct_binding_candidates(
            policy,
            {"Candidate.java": source},
            {"Candidate.java": builder.git_blob_sha1(source)},
        )
        self.assertEqual(1, len(candidates))
        self.assertEqual(
            "fluid_and_material_token_same_statement",
            candidates[0]["rule"],
        )

    def test_committed_projection_is_current_and_check_is_read_only(self):
        before = hashlib.sha256(builder.OUTPUT.read_bytes()).hexdigest()
        self.assertEqual([], builder.reference_only_check())
        self.assertEqual(
            before,
            hashlib.sha256(builder.OUTPUT.read_bytes()).hexdigest(),
        )

    def test_full_revision_source_replay_matches_projection(self):
        self.assertEqual([], builder.full_replay_check())


if __name__ == "__main__":
    unittest.main()
