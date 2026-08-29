"""Semantic projection ABI and sidecar v2 contracts."""
from __future__ import annotations

import copy
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tools import currentness
from tools import semantic_projection as projection
from tools import t35_common as t35


class SemanticProjectionAbiTest(unittest.TestCase):
    def test_canonical_json_is_compact_and_sorted(self) -> None:
        encoded = projection.canonical_dumps({"b": 1, "a": {"z": 2, "m": [3, 1]}})
        self.assertEqual('{"a":{"m":[3,1],"z":2},"b":1}', encoded)
        self.assertNotIn(" ", encoded)

    def test_projection_version_is_inside_hashed_payload(self) -> None:
        gate = t35.load_json(projection.GATE)
        payload = projection.gate_projection(gate)
        self.assertEqual(1, payload["semantic_projection_version"])
        self.assertIn("semantic_projection_version", payload)
        self.assertEqual(
            projection.sha256_canonical(payload),
            projection.project_gate_document(gate)["semantic_root_sha256"],
        )

    def test_envelope_only_does_not_change_gate_semantic_root(self) -> None:
        gate = t35.load_json(projection.GATE)
        mutated = copy.deepcopy(gate)
        mutated["counts"] = {"probe": 1}
        mutated["authority"] = {"path": "probe", "semantic_root_sha256": "0" * 64}
        original = projection.project_gate_document(gate)
        changed = projection.project_gate_document(mutated)
        self.assertEqual(original["semantic_root_sha256"], changed["semantic_root_sha256"])
        self.assertNotEqual(original["envelope_sha256"], changed["envelope_sha256"])
        self.assertNotEqual(
            projection.sha256_file(projection.GATE),
            __import__("hashlib").sha256(
                json.dumps(mutated, sort_keys=True).encode("utf-8")
            ).hexdigest(),
        )

    def test_true_semantic_form_removal_changes_root(self) -> None:
        gate = t35.load_json(projection.GATE)
        mutated = copy.deepcopy(gate)
        material = next(iter(mutated["materials"]))
        forms = list(mutated["materials"][material])
        self.assertTrue(forms)
        mutated["materials"][material] = forms[1:]
        self.assertNotEqual(
            projection.project_gate_document(gate)["semantic_root_sha256"],
            projection.project_gate_document(mutated)["semantic_root_sha256"],
        )

    def test_legacy_whole_file_red_semantic_green(self) -> None:
        gate = t35.load_json(projection.GATE)
        mutated = copy.deepcopy(gate)
        mutated["sources"] = [{"probe": True}]
        original_file = projection.sha256_file(projection.GATE)
        mutated_bytes = json.dumps(mutated).encode("utf-8")
        self.assertNotEqual(original_file, projection.sha256_canonical(mutated))
        self.assertEqual(
            projection.project_gate_document(gate)["semantic_root_sha256"],
            projection.project_gate_document(mutated)["semantic_root_sha256"],
        )
        self.assertNotEqual(original_file, __import__("hashlib").sha256(mutated_bytes).hexdigest())


class CurrentnessSidecarV2Test(unittest.TestCase):
    def test_builder_only_refuses_blind_rebind(self) -> None:
        directory = Path(tempfile.mkdtemp())
        artifact = directory / "card.json"
        artifact.write_text('{"body":1,"generated_by":"old"}\n', encoding="utf-8")
        currentness.write_sidecar(artifact)
        with self.assertRaises(ValueError) as ctx:
            currentness.rebind_sidecar(artifact, mode="builder", prove_rebuild=False)
        self.assertIn("prove_rebuild", str(ctx.exception))
        rebound = currentness.rebind_sidecar(
            artifact,
            mode="builder",
            prove_rebuild=True,
        )
        self.assertEqual(
            rebound["semantic_root_sha256"],
            currentness.load_sidecar(currentness.sidecar_path(artifact))[
                "semantic_root_sha256"
            ],
        )

    def test_corrupt_pairing_fails_even_if_semantic_root_matches(self) -> None:
        directory = Path(tempfile.mkdtemp())
        artifact = directory / "card.json"
        artifact.write_text('{"body":1}\n', encoding="utf-8")
        currentness.write_sidecar(artifact)
        sidecar = currentness.load_sidecar(currentness.sidecar_path(artifact))
        artifact.write_text('{"body":1 }\n', encoding="utf-8")
        errors = currentness.check_sidecar(artifact)
        self.assertTrue(any("pairing" in error for error in errors))
        self.assertEqual(
            sidecar["semantic_root_sha256"],
            currentness.semantic_root_sha256({"body": 1}),
        )

    def test_full_replay_unavailable_does_not_upgrade_bound_receipt(self) -> None:
        existing = {
            "source_replay_receipt": {
                "status": "bound",
                "proof_tier": "full_replay",
            }
        }
        preserved = currentness.preserve_source_replay_receipt(
            existing,
            dump_available=False,
        )
        self.assertEqual("bound", preserved["status"])
        self.assertEqual("full_replay", preserved["proof_tier"])
        with mock.patch.object(currentness, "DUMP_INDEX", Path(tempfile.mkdtemp()) / "missing.json"):
            directory = Path(tempfile.mkdtemp())
            artifact = directory / "card.json"
            artifact.write_text('{"body":1}\n', encoding="utf-8")
            sidecar_path = currentness.sidecar_path(artifact)
            sidecar_path.write_text(
                t35.stable_json(
                    {
                        "schema_version": 2,
                        "status": "CURRENTNESS_SIDECAR",
                        "artifact": artifact.as_posix(),
                        "semantic_projection_kind": "json_ledger",
                        "semantic_projection_version": 1,
                        "artifact_file_sha256": projection.sha256_file(artifact),
                        "semantic_root_sha256": currentness.semantic_root_sha256(
                            {"body": 1}
                        ),
                        "builder_sha256": None,
                        "envelope_sha256": projection.project_ledger_document(
                            {"body": 1}
                        )["envelope_sha256"],
                        "dependency_semantic_roots": {},
                        "source_replay_receipt": {
                            "status": "bound",
                            "proof_tier": "full_replay",
                        },
                    }
                ),
                encoding="utf-8",
            )
            written = currentness.write_sidecar(artifact)
            self.assertEqual("bound", written["source_replay_receipt"]["status"])
            self.assertEqual("full_replay", written["source_replay_receipt"]["proof_tier"])

    def test_downstream_depends_on_gate_semantic_root_not_file_sha(self) -> None:
        t5 = t35.ROOT / "tools/t5_chemical_readiness.json"
        if not t5.is_file():
            self.skipTest("t5_chemical_readiness.json missing")
        sidecar = currentness.sidecar_path(t5)
        if not sidecar.is_file():
            self.skipTest("t5 sidecar missing")
        document = currentness.load_sidecar(sidecar)
        gate_dep = document["dependency_semantic_roots"][
            "src/main/resources/data/cruciblecraft/material_registration_gate.json"
        ]
        live = projection.project_gate_document(t35.load_json(projection.GATE))
        self.assertEqual(live["semantic_root_sha256"], gate_dep["semantic_root_sha256"])
        self.assertNotEqual(gate_dep["semantic_root_sha256"], projection.sha256_file(projection.GATE))
        self.assertEqual([], currentness.check_sidecar(t5))


class RecipeTreeProjectionTest(unittest.TestCase):
    def test_recipe_indent_is_not_semantic(self) -> None:
        directory = Path(tempfile.mkdtemp())
        recipe = directory / "a.json"
        body = {"type": "minecraft:crafting_shapeless", "id": "cruciblecraft:a"}
        recipe.write_text(json.dumps(body, indent=2) + "\n", encoding="utf-8")
        pretty = projection.project_recipe_tree(directory, repo=directory)
        recipe.write_text(json.dumps(body, separators=(",", ":")), encoding="utf-8")
        compact = projection.project_recipe_tree(directory, repo=directory)
        self.assertEqual(pretty["semantic_root_sha256"], compact["semantic_root_sha256"])


if __name__ == "__main__":
    unittest.main()
