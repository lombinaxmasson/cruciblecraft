"""Section authority identity, historical receipts, and path selection."""
from __future__ import annotations

import copy
import unittest

from tools import material_form_authority as authority
from tools import run_python_tests
from tools import verify as verify_entry
from tools.recipe_bulk import source_import


class SectionIdentityTest(unittest.TestCase):
    def test_section_slice_is_stable_and_local(self) -> None:
        document = authority.load_authority()
        first = authority.section_index(document)
        second = authority.section_index(document)
        self.assertEqual(first, second)
        bath = first["bath_compact_required_forms"]
        self.assertEqual("bath_required_forms", bath["gate_section"])
        self.assertIn("required_forms_sha256", bath)
        self.assertIn("slice_sha256", bath)
        self.assertNotEqual(
            bath["slice_sha256"],
            first["roaster_compact_required_forms"]["slice_sha256"],
        )

    def test_one_section_change_does_not_move_another_slice(self) -> None:
        document = authority.load_authority()
        original = authority.section_sha256(
            authority.source_by_id("roaster_compact_required_forms", document)
        )
        mutated = copy.deepcopy(document)
        bath = authority.source_by_id("bath_compact_required_forms", mutated)
        forms = list(bath["extra_factual_forms"])
        forms.append("probe_form")
        bath["extra_factual_forms"] = forms
        self.assertNotEqual(
            authority.section_sha256(bath),
            authority.section_sha256(
                authority.source_by_id("bath_compact_required_forms", document)
            ),
        )
        self.assertEqual(
            original,
            authority.section_sha256(
                authority.source_by_id("roaster_compact_required_forms", mutated)
            ),
        )


class ReceiptScopeTest(unittest.TestCase):
    def test_legacy_receipt_keeps_stored_whole_file_digest(self) -> None:
        spec = {
            "authority_scope": "section",
            "authority_sections": ["bath_compact_required_forms"],
            "operand_authorities": [
                {"id": "material_form_authority", "kind": "data"}
            ],
            "output_paths": {},
        }
        existing = {
            "authority_sha256": {"material_form_authority": "a" * 64},
        }
        self.assertEqual(
            authority.LEGACY_WHOLE_FILE,
            source_import.authority_scope_for(spec, existing),
        )
        hashes = source_import._authority_hashes(spec, existing)
        self.assertEqual("a" * 64, hashes["material_form_authority"])

    def test_section_receipt_records_only_consumed_slices(self) -> None:
        spec = {
            "authority_scope": authority.SECTION_SCOPE,
            "authority_sections": ["bath_compact_required_forms"],
            "operand_authorities": [
                {"id": "material_form_authority", "kind": "data"}
            ],
        }
        hashes = source_import._authority_hashes(spec, None)
        binding = hashes["material_form_authority"]
        self.assertEqual(["bath_compact_required_forms"], sorted(binding))
        self.assertEqual(
            authority.section_index()["bath_compact_required_forms"]["slice_sha256"],
            binding["bath_compact_required_forms"],
        )

    def test_closed_card_whole_file_drift_is_historical(self) -> None:
        kind = authority.classify_authority_binding(
            {"authority_sha256": {"material_form_authority": "b" * 64}},
            workflow="accepted",
        )
        self.assertEqual(authority.HISTORICAL, kind)

    def test_active_card_whole_file_drift_stays_current_dependency(self) -> None:
        kind = authority.classify_authority_binding(
            {"authority_sha256": {"material_form_authority": "c" * 64}},
            workflow="active",
        )
        self.assertEqual(authority.CURRENT_DEPENDENCY_DRIFT, kind)

    def test_unbound_receipt_with_frozen_revision_is_current(self) -> None:
        kind = authority.classify_authority_binding(
            {"source_revision": authority.io.SOURCE_REVISION},
            workflow="accepted",
        )
        self.assertEqual("current", kind)

    def test_missing_section_binding_is_a_real_regression(self) -> None:
        kind = authority.classify_authority_binding(
            {
                "authority_scope": authority.SECTION_SCOPE,
                "authority_sha256": {"material_form_authority": {}},
                "source_revision": authority.io.SOURCE_REVISION,
            },
            workflow="active",
        )
        self.assertEqual(authority.REAL_REGRESSION, kind)


class ProfileSelectionTest(unittest.TestCase):
    def test_historical_receipt_does_not_select_capability_runtime(self) -> None:
        document = verify_entry.load_profiles()
        classified = verify_entry.classify_paths(
            document,
            [
                "tools/t4_tool_readiness.currentness.json",
                "docs/current/verification.md",
            ],
        )
        self.assertNotIn("capability-runtime", classified["selected_profiles"])
        self.assertEqual([], classified["selected_profiles"])

    def test_authority_file_selects_section_consumers_not_closed_suite(self) -> None:
        policy = run_python_tests.load_policy()
        names, unmatched = run_python_tests.affected_module_names(
            policy,
            ["tools/material_form_authority.json"],
        )
        self.assertEqual((), unmatched)
        self.assertIn("test_material_form_authority", names)
        self.assertIn("test_bulk_port_verification", names)
        self.assertNotIn("test_slicer", names)
        self.assertNotIn("test_energy_batteries", names)


if __name__ == "__main__":
    unittest.main()
