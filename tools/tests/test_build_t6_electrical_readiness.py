import importlib.util
import json
import sys
import unittest
from pathlib import Path


TOOLS = Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "build_t6_electrical_readiness",
    TOOLS / "build_t6_electrical_readiness.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class T6ElectricalReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = MODULE.gt6_electrical.load(MODULE.SOURCE)
        cls.document = MODULE.build()

    def test_authoritative_revision_and_source_hashes_are_pinned(self):
        self.assertEqual(
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
            self.document["gt6_source"]["revision"],
        )
        actual = {
            path: row["sha256"]
            for path, row in self.document["gt6_source"]["files"].items()
        }
        self.assertEqual(
            MODULE.gt6_electrical.SOURCE_FILE_HASHES,
            actual,
        )
        self.assertTrue(all(len(value) == 64 for value in actual.values()))

    def test_raw_and_live_classification_closure(self):
        counts = self.document["counts"]
        self.assertEqual(
            {
                "conductor": 30,
                "insulator": 1,
                "insulator_group": 1,
                "not_applicable": 2182,
            },
            counts["raw_source_materials"]["classifications"],
        )
        self.assertEqual(
            2214, counts["raw_source_materials"]["denominator"]
        )
        self.assertEqual(0, counts["raw_source_materials"]["unclassified"])
        self.assertEqual(
            {
                "conductor": 29,
                "insulator": 1,
                "not_applicable": 1743,
            },
            counts["live_materials"]["classifications"],
        )
        self.assertEqual(1773, counts["live_materials"]["denominator"])
        self.assertEqual(0, counts["live_materials"]["unclassified"])
        self.assertEqual(
            counts["raw_source_materials"]["denominator"],
            len(self.document["raw_source_materials"]),
        )
        self.assertEqual(
            counts["live_materials"]["denominator"],
            len(self.document["live_materials"]),
        )

    def test_acceptance_conductors_have_direct_source_specifications(self):
        rows = {
            row["material"]: row
            for row in self.document["acceptance_conductors"]
        }
        self.assertEqual({"copper", "gold", "tin"}, set(rows))
        expected = {
            "copper": (256, 1, 1),
            "gold": (512, 3, 1),
            "tin": (32, 1, 1),
        }
        for material, values in expected.items():
            with self.subTest(material=material):
                specification = rows[material][
                    "required_specifications"
                ]["cableGt01"]
                self.assertEqual(values[0], specification["max_voltage"])
                self.assertEqual(values[1], specification["max_amperage"])
                self.assertEqual(values[2], specification["loss_per_meter"])
                self.assertTrue(specification["insulated"])
                self.assertFalse(specification["contact_damage"])

    def test_imported_values_are_exact_and_no_resistance_is_invented(self):
        expected = MODULE.gt6_electrical.specifications_by_source(
            self.source
        )
        architecture = self.document["import_architecture"]
        self.assertFalse(architecture["per_conductor_java_edits_required"])
        self.assertEqual(
            "gt6_metadata.electrical_by_specification",
            architecture["material_field"],
        )
        for domain in ("raw_source_materials", "live_materials"):
            for row in self.document[domain]:
                actual = row.get("electrical_by_specification") or {}
                self.assertEqual(
                    expected.get(row["source_name"], {}),
                    actual,
                    f"{domain}/{row['source_name']}",
                )
        encoded = MODULE.stable_json(self.document)
        self.assertNotIn('"resistance"', encoded)
        self.assertEqual(
            0,
            self.document["readiness"]["criteria"][
                "invented_physical_resistance_count"
            ],
        )

    def test_cable_domain_and_representation_projection_are_derived(self):
        cable = self.document["cable_domain"]
        self.assertEqual(29, cable["source_electric_wire_material_count"])
        self.assertEqual(28, cable["source_electric_cable_material_count"])
        self.assertEqual(5, cable["gauge_count"])
        self.assertEqual(118, cable["live_form_pairs"])
        self.assertEqual(115, cable["runtime_source_backed_block_count"])
        self.assertEqual(5, cable["generic_material_rule_count"])
        self.assertEqual(118, cable["live_recipe_expansion_count"])
        self.assertEqual(
            {
                "cable": 26,
                "dodecuple_cable": 23,
                "double_cable": 23,
                "octuple_cable": 23,
                "quadruple_cable": 23,
            },
            {
                form: len(materials)
                for form, materials in cable[
                    "live_materials_by_form"
                ].items()
            },
        )
        projection = cable["block_representation_projection"]
        self.assertEqual(64, projection["connection_masks_per_material_form"])
        self.assertEqual(
            7552, projection["current_live_logical_blockstates"]
        )
        self.assertEqual(
            7360,
            projection["runtime_source_backed_logical_blockstates"],
        )
        self.assertEqual(
            8960, projection["all_source_eligible_logical_blockstates"]
        )
        self.assertEqual(
            567360, projection["naive_all_live_logical_blockstates"]
        )
        self.assertEqual("SELECTED", projection["status"])
        self.assertEqual(
            "material_specific_block_per_registered_form",
            projection["selected"]["mode"],
        )
        self.assertEqual(
            ["lumium", "red_alloy", "signalum"],
            cable["non_electric_live_materials_by_form"]["cable"],
        )
        self.assertEqual(
            [],
            [
                row
                for row in cable["runtime_block_catalog"]
                if row["material"] in {"lumium", "red_alloy", "signalum"}
            ],
        )
        self.assertEqual(
            115, len(cable["runtime_block_catalog"])
        )
        for row in cable["runtime_block_catalog"]:
            with self.subTest(
                material=row["material"], form=row["form"]
            ):
                material = next(
                    value
                    for value in self.document["live_materials"]
                    if value["material"] == row["material"]
                )
                self.assertEqual(
                    material["electrical_by_specification"][
                        row["source_specification"]
                    ],
                    row["electrical"],
                )

    def test_bare_wire_and_combined_runtime_domains_are_exact(self):
        wire = self.document["wire_domain"]
        self.assertEqual("wireGt01", wire["source_specification"])
        self.assertEqual(29, wire["runtime_source_backed_block_count"])
        self.assertEqual(29, len(wire["runtime_block_catalog"]))
        self.assertEqual(
            15, len(wire["deferred_source_specifications"])
        )
        for row in wire["runtime_block_catalog"]:
            self.assertEqual("wire", row["form"])
            self.assertEqual("wireGt01", row["source_specification"])
            self.assertFalse(row["electrical"]["insulated"])
        runtime = self.document["runtime_domain"]
        self.assertEqual(115, runtime["cable_block_count"])
        self.assertEqual(29, runtime["wire_block_count"])
        self.assertEqual(144, runtime["conductor_block_count"])
        self.assertEqual(9216, runtime["logical_blockstate_count"])
        self.assertFalse(runtime["network_graph_persisted"])
        self.assertTrue(runtime["burn_counter_persisted"])
        self.assertTrue(runtime["burn_decay_phase_persisted"])

    def test_readiness_closes_runtime_implementation_review(self):
        self.assertEqual("SOURCE_FACTS_READY", self.document["source_fact_status"])
        self.assertEqual("READY", self.document["status"])
        readiness = self.document["readiness"]
        self.assertEqual([], readiness["direct_source_blockers"])
        self.assertEqual([], readiness["policy_runtime_blockers"])
        self.assertEqual([], readiness["blockers"])
        self.assertTrue(
            readiness["criteria"]["future_cc_policy_decisions_closed"]
        )
        self.assertTrue(
            readiness["criteria"]["runtime_architecture_reviewed"]
        )
        self.assertTrue(
            readiness["criteria"]["runtime_cable_implementation_reviewed"]
        )
        implementation = self.document["runtime_implementation"]
        self.assertEqual(
            "T6_RUNTIME_IMPLEMENTED_AND_REVIEWED",
            implementation["status"],
        )
        self.assertEqual(11, len(implementation["contracts"]))
        self.assertIn(
            "serial_revalidation_source_first_bounded_dissipation",
            implementation["contracts"],
        )
        self.assertIn(
            "phased_observable_sync_and_cached_shapes",
            implementation["contracts"],
        )
        self.assertEqual(
            set(
                path.relative_to(MODULE.ROOT).as_posix()
                for path in MODULE.RUNTIME_SOURCE_GUARDS
            ),
            set(implementation["source_sha256"]),
        )
        decisions = self.document["future_cc_policy_decisions"]
        self.assertEqual(
            {
                "block_representation":
                    "material_specific_block_per_registered_form",
                "bare_wire_extension":
                    "wireGt01_source_backed_blocks",
                "burn_persistence":
                    "persist_counter_and_decay_phase",
                "gauge_mapping": "exact_five_cable_gauges",
                "loss_per_segment": "source_loss_per_traversed_block",
                "network_topology":
                    "source_faithful_stateless_packet_traversal",
                "overvoltage_behavior": "source_burn_counter",
                "voltage_tiers": "exact_source_max_voltage",
            },
            {
                name: decision["selected"]["mode"]
                for name, decision in decisions.items()
            },
        )

    def test_committed_ledger_is_current_and_check_mode_is_read_only(self):
        encoded = MODULE.stable_json(self.document)
        self.assertEqual(encoded, MODULE.OUTPUT.read_text(encoding="utf-8"))
        before = MODULE.sha256(MODULE.OUTPUT)
        self.assertEqual(encoded, MODULE.stable_json(self.document))
        self.assertEqual(before, MODULE.sha256(MODULE.OUTPUT))


if __name__ == "__main__":
    unittest.main()
