from __future__ import annotations

import ast
import hashlib
import inspect
import json
import unittest
from pathlib import Path

from tools import gt6_l1b_layer_select as l1b
from tools import gt6_structural_class as structural


class L1bRuleMetadataTest(unittest.TestCase):
    def test_sentinel_id_is_never_an_appearance_lookup_key(self) -> None:
        class LookupGuard(dict):
            def get(self, key, default=None):
                if key < 0:
                    raise AssertionError(f"negative lookup: {key}")
                return super().get(key, default)

        evidence = LookupGuard({
            -1: {"item": {"should-never-be-read": 441}},
            10: {"item": {"gt.recipe.test": 1}},
        })
        self.assertEqual(
            {},
            l1b.material_evidence_for_source(evidence, -1),
        )
        self.assertEqual(
            {"item": {"gt.recipe.test": 1}},
            l1b.material_evidence_for_source(evidence, 10),
        )

    def test_every_classification_rule_declares_basis(self) -> None:
        tree = ast.parse(inspect.getsource(l1b.classify))
        returned_rules = {
            node.value.elts[1].value
            for node in ast.walk(tree)
            if isinstance(node, ast.Return)
            and isinstance(node.value, ast.Tuple)
            and len(node.value.elts) == 2
            and isinstance(node.value.elts[1], ast.Constant)
            and isinstance(node.value.elts[1].value, str)
        }
        self.assertEqual(returned_rules, set(l1b.RULE_DEFINITIONS))
        for rule, definition in l1b.RULE_DEFINITIONS.items():
            with self.subTest(rule=rule):
                self.assertIn(
                    definition.get("basis"),
                    {"verified", "inferred", "external"},
                )
                self.assertTrue(definition.get("reason"))
                self.assertIn("falsifier", definition)
                self.assertIn("falsifier_test", definition)

    def test_fictional_band_starts_after_verified_periodic_table(self) -> None:
        self.assertEqual("element", structural.structural_class(1180))
        self.assertEqual("isotope", structural.structural_class(1148))
        self.assertEqual(
            "fictional_material",
            structural.structural_class(1190),
        )
        self.assertEqual(
            "fictional_material",
            structural.structural_class(1850),
        )
        self.assertTrue(structural.is_element_like(1180))
        self.assertFalse(structural.is_element_like(1190))
        self.assertFalse(structural.is_element_like(1850))  # Abyssalnite

        root = Path(__file__).resolve().parents[2]
        materials = json.loads(
            (root / "tools/gt6_oredict_materials_normalized.json").read_text(
                encoding="utf-8"
            )
        )["records"]
        periodic = {
            row["source_id"]: row["source_name"]
            for row in materials
            if structural.is_element_like(row.get("source_id"))
            and 1000 <= row["source_id"] <= 1180
        }
        self.assertEqual(
            set(range(1000, 1190, 10)),
            set(periodic),
        )
        self.assertEqual("Fermium", periodic[1000])
        self.assertEqual("Oganesson", periodic[1180])
        isotope = next(row for row in materials if row["source_id"] == 1148)
        self.assertEqual("Flerovium298", isotope["source_name"])
        self.assertEqual("isotope", structural.structural_class(1148))

    def test_protons_cross_validate_but_do_not_classify_identity(self) -> None:
        root = Path(__file__).resolve().parents[2]
        materials = json.loads(
            (
                root
                / "gt6_dump/gt6_recipe_dump/oredict/materials.json"
            ).read_text(encoding="utf-8")
        )
        by_name = {row["nameInternal"]: row for row in materials}

        force = by_name["Force"]
        self.assertGreater(force["protons"], 0)
        self.assertEqual("alloy_compound", structural.structural_class(force["id"]))

        for row in materials:
            z = structural.atomic_number(row.get("id"))
            if z is not None:
                self.assertEqual(z, row["protons"], row["nameInternal"])
                self.assertEqual(row["protons"], row["electrons"], row["nameInternal"])
                self.assertEqual(
                    row["mass"],
                    row["protons"] + row["neutrons"],
                    row["nameInternal"],
                )

    def test_pending_t0b_active_hot_is_a_conflict(self) -> None:
        record = {
            "source_name": "SiliconDioxide",
            "status": "ACTIVE",
            "t0_forms": ["dust"],
            "recipe_evidence": {"item": {"gt.recipe.mixer": 1}},
        }
        conflicts = l1b.detect_row_conflicts(
            record,
            "pending_t0b_non_native_ownership",
            set(),
            set(),
            set(),
            3056,
            {"item": {"gt.recipe.mixer": 2561}},
            record["recipe_evidence"],
            template_appearances=24,
        )
        self.assertIn("pending_t0b_with_hot_full_evidence", conflicts)
        # Foam expansion alone should not force the hot tag once templates exist.
        conflicts_cold_template = l1b.detect_row_conflicts(
            record,
            "pending_t0b_non_native_ownership",
            set(),
            set(),
            set(),
            3056,
            {"item": {"gt.recipe.mixer": 2561}},
            record["recipe_evidence"],
            template_appearances=1,
        )
        self.assertNotIn(
            "pending_t0b_with_hot_full_evidence",
            conflicts_cold_template,
        )
        self.assertIn(
            "pending_t0b_with_full_evidence",
            conflicts_cold_template,
        )

    def test_core_closure_is_annotated_by_edge_and_target_layer(self) -> None:
        core = {"Source": {"layer": "CORE"}}
        materials = {
            "Source": {
                "components": [{"material": "Shelved"}],
                "processing_targets": {"smelting": {"material": "Pending"}},
                "byproducts": [{"material": "Dropped"}],
            }
        }
        all_rows = {
            "Source": {"layer": "CORE"},
            "Shelved": {"layer": "SHELVED_FICTIONAL"},
            "Pending": {"layer": "PENDING_T0B"},
            "Dropped": {"layer": "NEVER"},
        }

        violations = l1b.closure_violations(core, materials, all_rows)

        self.assertEqual(
            [
                ("components", "SHELVED_FICTIONAL", "HARD_FAIL"),
                ("targets", "PENDING_T0B", "DEGRADED_ROUTE"),
                ("byproducts", "NEVER", "OPTIONAL_LOSS"),
            ],
            [
                (row["edge_type"], row["target_layer"], row["severity"])
                for row in violations
            ],
        )

    def test_component_closure_promotion_reaches_fixed_point(self) -> None:
        rows = {
            "A": {"layer": "CORE", "initial_layer": "CORE", "rule": "a"},
            "B": {
                "layer": "PENDING_T0B",
                "initial_layer": "PENDING_T0B",
                "rule": "b",
            },
            "C": {
                "layer": "SHELVED_FICTIONAL",
                "initial_layer": "SHELVED_FICTIONAL",
                "rule": "c",
            },
        }
        materials = {
            "A": {"aliases": [], "components": [{"material": "B"}]},
            "B": {"aliases": [], "components": [{"material": "C"}]},
            "C": {"aliases": [], "components": []},
        }

        promotions, blocked = l1b.promote_component_closure(
            rows, materials, set()
        )

        self.assertFalse(blocked)
        self.assertEqual("CORE", rows["B"]["layer"])
        self.assertEqual("CORE", rows["C"]["layer"])
        self.assertEqual(2, len(promotions))
        self.assertIn(["A", "B", "C"], rows["C"]["promoted_by_closure"]["paths"])

    def test_active_closure_rule_requires_direct_evidence(self) -> None:
        root = Path(__file__).resolve().parents[2]
        records = json.loads(
            (root / "tools/gt6_l1b_selected.json").read_text(encoding="utf-8")
        )["records"]
        hits = [
            row
            for row in records.values()
            if row["rule"] == "active_fluid_or_dependency_closure"
        ]
        self.assertTrue(hits)
        for row in hits:
            reason = row["activation_reason"] or ""
            has_fluid = bool((row["recipe_evidence"].get("fluid") or {}))
            self.assertTrue(
                has_fluid
                or "dependency" in reason
                or reason.startswith("transitive"),
                row["source_name"],
            )

    def test_no_hit_inferred_rule_lacks_a_falsifier(self) -> None:
        root = Path(__file__).resolve().parents[2]
        report = json.loads(
            (root / "tools/gt6_l1b_layer_report.json").read_text(encoding="utf-8")
        )
        self.assertEqual(
            0,
            report["inferred_falsifier_debt"]["rule_count_without_falsifier"],
        )
        self.assertEqual(
            0,
            report["inferred_falsifier_debt"]["material_count_without_falsifier"],
        )

    def test_active_fallback_rules_match_only_their_declared_domain(self) -> None:
        root = Path(__file__).resolve().parents[2]
        records = json.loads(
            (root / "tools/gt6_l1b_selected.json").read_text(
                encoding="utf-8"
            )
        )["records"].values()
        expected_element_like = {
            "element_active_unclassified": True,
            "active_without_classified_evidence": False,
        }
        hits = [
            row for row in records if row["rule"] in expected_element_like
        ]
        self.assertTrue(hits)
        for row in hits:
            self.assertEqual("ACTIVE", row["activation_status"])
            self.assertEqual(
                expected_element_like[row["rule"]], row["element_like"]
            )
            self.assertEqual({}, row["recipe_evidence"])

    def test_sentinel_edges_cannot_hide_stable_closure_gaps(self) -> None:
        root = Path(__file__).resolve().parents[2]
        report = json.loads(
            (root / "tools/gt6_l1a_closure_report.json").read_text(
                encoding="utf-8"
            )
        )
        self.assertEqual(
            {
                "stable_id_to_stable_id": 1524,
                "stable_id_to_name_only": 0,
                "name_only_to_stable_id": 9,
                "name_only_to_name_only": 0,
            },
            report["graphs"]["components"][
                "edge_source_x_target_identity"
            ],
        )
        self.assertEqual(
            {
                "stable_id_to_stable_id": 21276,
                "stable_id_to_name_only": 0,
                "name_only_to_stable_id": 420,
                "name_only_to_name_only": 4872,
            },
            report["graphs"]["targets"][
                "edge_source_x_target_identity"
            ],
        )
        self.assertEqual(
            0,
            report["graphs"]["targets"][
                "stable_source_to_name_only_target_count"
            ],
        )

    def test_all_l1b_summaries_exclude_sentinels(self) -> None:
        root = Path(__file__).resolve().parents[2]
        report = json.loads(
            (root / "tools/gt6_l1b_layer_report.json").read_text(
                encoding="utf-8"
            )
        )
        self.assertTrue(report["excludes_sentinels"])
        self.assertEqual(1773, report["universe"])
        self.assertEqual(2214, report["source_record_count"])
        self.assertEqual(
            {
                "index_map_count": 95,
                "named_map_count_scanned": 94,
                "empty_unnamed_map_count": 1,
                "nonempty_unscanned_map_count": 0,
            },
            report["recipe_map_inventory"],
        )
        self.assertEqual(1, report["layer_counts"]["NEVER"])
        self.assertNotIn(
            "no_id",
            report["histograms"]["structural_class_x_layer"],
        )
        self.assertEqual(
            {"0": 570, "1-49": 183, "50+": 1020},
            report["histograms"]["recipe_appearances_full"],
        )
        self.assertEqual(
            {"0": 602, "1-49": 243, "50+": 928},
            report["histograms"]["reviewed_appearances"],
        )
        self.assertEqual(
            {"0": 0, "1": 578, "2-4": 2, "5+": 1193},
            report["histograms"]["registered_prefix_count_fine"],
        )
        self.assertEqual(
            0,
            report["rule_invariants"][
                "registered_prefix_zero_stable_material_count"
            ],
        )
        self.assertFalse(
            report["rule_invariants"][
                "classifier_uses_registered_prefix_count"
            ]
        )
        self.assertEqual(
            "CLOSED",
            report["sentinel_compatibility_surface"][
                "silent_zone_investigation_status"
            ],
        )

    def test_reference_metadata_tracks_sentinel_compatibility_surface(self) -> None:
        root = Path(__file__).resolve().parents[2]
        l1b_report = json.loads(
            (root / "tools/gt6_l1b_layer_report.json").read_text(
                encoding="utf-8"
            )
        )
        metadata = json.loads(
            (root / "tools/gt6_reference_metadata.json").read_text(
                encoding="utf-8"
            )
        )
        materials = json.loads(
            (
                root
                / "gt6_dump/gt6_recipe_dump/oredict/materials.json"
            ).read_text(encoding="utf-8")
        )
        names = sorted(
            row["nameInternal"]
            for row in materials
            if row.get("id") == -1
        )
        names_hash = hashlib.sha256(
            json.dumps(
                names,
                ensure_ascii=False,
                separators=(",", ":"),
            ).encode("utf-8")
        ).hexdigest()
        source = l1b_report["sentinel_compatibility_surface"]
        recorded = metadata["compatibility_surface"]
        self.assertEqual(2, metadata["schema_version"])
        self.assertEqual(
            441,
            recorded["sentinel_record_count"],
        )
        self.assertEqual(
            "CLOSED",
            recorded["silent_zone_investigation_status"],
        )
        self.assertEqual(
            [],
            metadata["runtime_mod_inventory"]["run_mods_jars"],
        )
        self.assertFalse(any(
            "hbm" in entry.lower()
            for entry in metadata["runtime_mod_inventory"][
                "runtime_classpath"
            ]
        ))
        self.assertEqual(
            source["record_count"],
            recorded["name_only_material_count"],
        )
        self.assertEqual(
            source["registered_any_prefix_count"],
            recorded["name_only_registered_any_prefix_count"],
        )
        self.assertEqual(
            source["registered_t0_prefix_and_zero_appearance_count"],
            recorded["name_only_registered_t0_prefix_count"],
        )
        self.assertEqual(
            names_hash,
            recorded["name_only_material_name_set_sha256"],
        )


if __name__ == "__main__":
    unittest.main()
