import importlib.util
import json
import sys
import unittest


TOOLS = __import__("pathlib").Path(__file__).resolve().parents[1]
SPEC = importlib.util.spec_from_file_location(
    "build_machine_crafting_readiness",
    TOOLS / "build_machine_crafting_readiness.py",
)
assert SPEC and SPEC.loader
MODULE = importlib.util.module_from_spec(SPEC)
sys.modules[SPEC.name] = MODULE
SPEC.loader.exec_module(MODULE)


class MachineCraftingReadinessTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.policy = json.loads(MODULE.POLICY.read_text(encoding="utf-8"))
        cls.document = MODULE.build()

    def test_committed_ledger_is_current_and_deterministic(self):
        encoded = MODULE.stable_json(self.document)
        self.assertEqual(
            encoded,
            MODULE.OUTPUT.read_text(encoding="utf-8"),
        )
        self.assertEqual(encoded, MODULE.stable_json(self.document))
        self.assertEqual(
            "BLOCKED_ON_EXPLICIT_PREREQUISITES",
            self.document["status"],
        )
        self.assertFalse(self.document["replacement_authorized"])
        self.assertFalse(self.document["replacement_gate"]["ready"])
        self.assertEqual(0, self.document["counts"]["unclassified"])

    def test_configured_ids_come_from_authoritative_java_sources(self):
        configured = MODULE.configured_machine_ids()
        placeholders = MODULE.placeholder_machine_ids()
        ledger_ids = [
            row["machine_id"]
            for row in self.document["configured_machines"]
        ]
        self.assertEqual(23, len(configured))
        self.assertEqual(configured, placeholders)
        self.assertEqual(configured, ledger_ids)
        self.assertEqual(set(configured), set(self.policy["configured_machines"]))
        self.assertEqual(len(configured), len(set(configured)))
        registrations = MODULE.machine_item_registrations()
        fields = dict(
            (recipe_id, item_field)
            for item_field, recipe_id in MODULE.placeholder_machines()
        )
        for row in self.document["configured_machines"]:
            with self.subTest(identity=row["machine_id"]):
                item_field = fields[row["machine_id"]]
                self.assertEqual(
                    item_field,
                    row["cc_identity"]["mod_items_field"],
                )
                self.assertEqual(
                    f"cruciblecraft:{registrations[item_field]}",
                    row["cc_identity"]["item"],
                )

    def test_bronze_resources_are_discovered_without_a_second_id_list(self):
        paths = MODULE.existing_bronze_recipe_paths()
        expected_ids = [path.stem for path in paths]
        actual_rows = self.document["existing_bronze_resources"]
        self.assertEqual(7, len(paths))
        self.assertEqual(
            expected_ids,
            [row["recipe_id"] for row in actual_rows],
        )
        self.assertEqual(
            set(expected_ids),
            set(self.policy["existing_bronze_resources"]),
        )
        for row, path in zip(actual_rows, paths, strict=True):
            with self.subTest(recipe=row["recipe_id"]):
                source = json.loads(path.read_text(encoding="utf-8"))
                self.assertEqual(
                    source["result"]["id"],
                    row["cc_identity"]["item"],
                )
                self.assertEqual(
                    source["type"],
                    row["current_recipe"]["kind"],
                )
                self.assertTrue(row["current_recipe"]["path"].endswith(".json"))

    def test_official_gt6_revision_and_source_blobs_are_pinned(self):
        source = self.document["gt6_source"]
        revision = "3703e40308c8c030763fd6297dea8b210d2a77b1"
        self.assertEqual("GregTech6/gregtech6", source["repository"])
        self.assertEqual(
            "https://github.com/GregTech6/gregtech6",
            source["repository_url"],
        )
        self.assertEqual(revision, source["revision"])
        self.assertEqual(
            f"https://github.com/GregTech6/gregtech6/commit/{revision}",
            source["commit_url"],
        )
        self.assertRegex(source["tree_sha1"], r"^[0-9a-f]{40}$")
        self.assertEqual(
            "resolved_exact_official_revision",
            source["resolution_status"],
        )
        self.assertFalse(
            source["recipe_map_dump_scope"][
                "contains_machine_block_crafting"
            ]
        )
        self.assertEqual(
            {"multitile_loader", "material_tiers"},
            set(source["source_files"]),
        )
        for record in source["source_files"].values():
            with self.subTest(path=record["path"]):
                self.assertIn(f"/{revision}/", record["raw_url"])
                self.assertRegex(
                    record["git_blob_sha1"],
                    r"^[0-9a-f]{40}$",
                )
                self.assertTrue(record["path"].endswith(".java"))
        self.assertIn(
            "build_machine_crafting_readiness.py --fetch-source",
            source["fetch_command"],
        )
        self.assertIn(
            "--check --verify-source",
            source["verify_command"],
        )
        source_hashes = self.document["source_hashes"]
        self.assertEqual(
            MODULE.sha256(TOOLS / "build_machine_crafting_readiness.py"),
            source_hashes["builder"],
        )
        for name, digest in source_hashes.items():
            if name != "existing_bronze_resources":
                with self.subTest(source_hash=name):
                    self.assertRegex(digest, r"^[0-9a-f]{64}$")

    def test_exact_coverage_and_classification_counts_are_locked(self):
        self.assertEqual(
            {
                "configured_machine_placeholders": 23,
                "existing_bronze_resources": 7,
                "total_rows": 30,
                "classifications": {
                    "directly_projectable": 0,
                    "existing_cc_recipe_retained": 7,
                    "missing_cc_component": 3,
                    "source_identity_unresolved": 2,
                    "tier_collapsed_pending_t6": 18,
                },
                "classified": 30,
                "unclassified": 0,
            },
            self.document["counts"],
        )
        self.assertEqual(
            MODULE.ALLOWED_CLASSIFICATIONS,
            set(self.policy["classifications"]),
        )

    def test_every_row_has_identity_recipe_evidence_and_prerequisites(self):
        rows = [
            *self.document["configured_machines"],
            *self.document["existing_bronze_resources"],
        ]
        revision = self.document["gt6_source"]["revision"]
        for row in rows:
            identity = row.get("machine_id", row.get("recipe_id"))
            with self.subTest(identity=identity):
                self.assertTrue(row["cc_identity"]["item"])
                self.assertTrue(row["current_recipe"]["path"])
                self.assertTrue(row["current_recipe"]["kind"])
                self.assertIn(
                    row["classification"],
                    MODULE.ALLOWED_CLASSIFICATIONS,
                )
                self.assertTrue(row["reason"].strip())
                self.assertTrue(row["source_identity"]["status"])
                self.assertTrue(row["source_evidence"])
                self.assertTrue(row["replacement_prerequisites"])
                self.assertTrue(
                    all(
                        prerequisite.strip()
                        for prerequisite in row[
                            "replacement_prerequisites"
                        ]
                    )
                )
                for evidence in row["source_evidence"]:
                    self.assertTrue(evidence["kind"])
                    if evidence["kind"].startswith("gt6_java"):
                        self.assertEqual(revision, evidence["revision"])
                        self.assertRegex(
                            evidence["git_blob_sha1"],
                            r"^[0-9a-f]{40}$",
                        )
                        self.assertTrue(evidence["lines"])
                        self.assertTrue(evidence["reason"].strip())

    def test_placeholders_remain_evidenced_and_not_projectable(self):
        template = self.document["current_placeholder_template"]
        self.assertEqual(["CCC", "CFC", "CCC"], template["pattern"])
        self.assertEqual(
            {
                "C": "minecraft:copper_ingot",
                "F": "minecraft:furnace",
            },
            template["ingredients"],
        )
        self.assertRegex(
            template["source"]["method_sha256"],
            r"^[0-9a-f]{64}$",
        )
        for row in self.document["configured_machines"]:
            with self.subTest(machine=row["machine_id"]):
                self.assertEqual(
                    "generated_copper_furnace_placeholder",
                    row["current_recipe"]["kind"],
                )
                self.assertNotEqual(
                    "directly_projectable",
                    row["classification"],
                )
                if row["classification"] != "source_identity_unresolved":
                    self.assertTrue(row["blocking_components"])

    def test_missing_component_mappings_are_explicit(self):
        mappings = self.document["component_mappings"]
        for identity in (
            "OP.casingMachine",
            "OP.casingMachineDouble",
            "OP.casingMachineQuadruple",
            "OP.casingSmall",
            "IL.MOTORS[tier]",
            "OD_CIRCUITS[tier]",
            "MT.DATA.CABLES_01[tier]",
            "IL.Ceramic_Bowl",
            "OP.toolHeadBuzzSaw",
            "DYE_OREDICTS_LENS",
        ):
            with self.subTest(identity=identity):
                self.assertIn(identity, mappings)
                self.assertTrue(mappings[identity]["status"])
                self.assertTrue(mappings[identity]["reason"].strip())

    def test_check_mode_is_immutable(self):
        watched = (MODULE.POLICY, MODULE.OUTPUT)
        before = {
            path: (path.read_bytes(), path.stat().st_mtime_ns)
            for path in watched
        }
        self.assertEqual(
            MODULE.stable_json(self.document),
            MODULE.OUTPUT.read_text(encoding="utf-8"),
        )
        after = {
            path: (path.read_bytes(), path.stat().st_mtime_ns)
            for path in watched
        }
        self.assertEqual(before, after)


if __name__ == "__main__":
    unittest.main()
