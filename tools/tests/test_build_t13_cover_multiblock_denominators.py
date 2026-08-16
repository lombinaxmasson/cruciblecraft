from __future__ import annotations

import copy
import hashlib
import json
import re
import unittest
from pathlib import Path

from tools import build_t13_cover_multiblock_denominators as builder


EXPECTED_PATH_DIGESTS = {
    "cover_kinds": (
        65,
        "5157144dc89bb2016214156ab09ba4a1d9bcadf7924965950575832320b62c56",
    ),
    "multiblock_kinds": (
        44,
        "72e48f6afc6c0d43aeec76a07553a64aa7a47b8cf2093b5fa525f15d85482c33",
    ),
}
INDEPENDENT_DECLARATION = re.compile(
    r"^public\s+(?:(abstract)\s+)?"
    r"(class|interface|enum)\s+([A-Za-z_$][A-Za-z0-9_$]*)\b"
)


def digest(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def path_digest(paths: set[str]) -> str:
    return hashlib.sha256(
        ("\n".join(sorted(paths)) + "\n").encode("utf-8")
    ).hexdigest()


class T13CoverMultiblockDenominatorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = builder.load(builder.POLICY)
        cls.documents = builder.build(cls.policy)

    def test_fixed_revision_tree_and_complete_path_sets_are_locked(self):
        self.assertEqual(
            "3703e40308c8c030763fd6297dea8b210d2a77b1",
            self.policy["source"]["revision"],
        )
        self.assertEqual(
            "a164302f62a326208fd2076de6fb7cdb0b4602ee",
            self.policy["source"]["tree_sha1"],
        )
        for domain_id, (count, expected_digest) in (
            EXPECTED_PATH_DIGESTS.items()
        ):
            with self.subTest(domain=domain_id):
                document = self.documents[domain_id]
                raw_paths = {
                    row["source_path"] for row in document["raw_records"]
                }
                self.assertEqual(count, len(raw_paths))
                self.assertEqual(expected_digest, path_digest(raw_paths))
                self.assertEqual(
                    sorted(raw_paths),
                    document["expected_path_set"]["paths"],
                )
                self.assertEqual(
                    expected_digest,
                    document["expected_path_set"]["sha256"],
                )
                self.assertEqual(
                    raw_paths,
                    builder.expected_paths(self.policy, domain_id),
                )

    def test_source_declarations_are_independently_parsed(self):
        for domain_id, domain in self.policy["domains"].items():
            for record in domain["source_manifest"]:
                with self.subTest(
                    domain=domain_id,
                    symbol=record["source_symbol"],
                ):
                    match = INDEPENDENT_DECLARATION.match(
                        record["declaration"]
                    )
                    self.assertIsNotNone(match)
                    assert match is not None
                    self.assertEqual(
                        record["source_symbol"], match.group(3)
                    )
                    self.assertEqual(
                        record["declaration_kind"], match.group(2)
                    )
                    independently_abstract = (
                        bool(match.group(1)) or match.group(2) == "interface"
                    )
                    self.assertEqual(
                        record["abstract"], independently_abstract
                    )
                    self.assertRegex(
                        record["git_blob_sha1"], r"^[0-9a-f]{40}$"
                    )
                    self.assertRegex(
                        record["source_sha256"], r"^[0-9a-f]{64}$"
                    )

    def test_production_parser_ignores_comments_literals_and_nested_types(self):
        source = """
            package example;
            // public class NotAClass {}
            public class Outer {
                String text = "public class AlsoNotAClass {}";
                public static class Nested {}
            }
        """
        self.assertEqual(
            [
                {
                    "source_symbol": "Outer",
                    "declaration_kind": "class",
                    "abstract": False,
                    "declaration": "public class Outer {",
                    "line": 4,
                }
            ],
            builder.parse_java_top_level_symbols(source),
        )

    def test_raw_partition_is_exact_and_bidirectional(self):
        for domain_id, document in self.documents.items():
            with self.subTest(domain=domain_id):
                raw_symbols = {
                    row["source_symbol"] for row in document["raw_records"]
                }
                canonical_members = {
                    symbol
                    for row in document["canonical_kinds"]
                    for symbol in row["raw_members"]
                }
                excluded_symbols = {
                    row["source_symbol"] for row in document["exclusions"]
                }
                self.assertFalse(canonical_members & excluded_symbols)
                self.assertEqual(
                    raw_symbols,
                    canonical_members | excluded_symbols,
                )
                self.assertEqual(
                    len(raw_symbols),
                    len(document["raw_records"]),
                )
                for raw in document["raw_records"]:
                    if raw["partition"] == "canonical":
                        self.assertIn(
                            raw["source_symbol"], canonical_members
                        )
                        self.assertTrue(raw["canonical_id"])
                    else:
                        self.assertEqual("excluded", raw["partition"])
                        self.assertIn(
                            raw["source_symbol"], excluded_symbols
                        )
                        self.assertTrue(raw["rule_id"])
                        self.assertTrue(raw["rule_reason"])

    def test_only_concrete_symbols_enter_canonical_denominators(self):
        for domain_id, document in self.documents.items():
            raw_by_symbol = {
                row["source_symbol"]: row
                for row in document["raw_records"]
            }
            for canonical in document["canonical_kinds"]:
                for symbol in canonical["raw_members"]:
                    with self.subTest(domain=domain_id, symbol=symbol):
                        raw = raw_by_symbol[symbol]
                        self.assertFalse(raw["abstract"])
                        self.assertEqual("class", raw["declaration_kind"])
            for exclusion in document["exclusions"]:
                self.assertTrue(exclusion["rule_id"])
                self.assertTrue(exclusion["rule_reason"])
                self.assertTrue(exclusion["exclusion_kind"])

    def test_declared_collapses_are_narrow_and_reproducible(self):
        cover = self.documents["cover_kinds"]
        self.assertEqual(
            {
                "logistics_fluid_transfer": {
                    "CoverLogisticsFluidExport",
                    "CoverLogisticsFluidImport",
                },
                "logistics_generic_transfer": {
                    "CoverLogisticsGenericExport",
                    "CoverLogisticsGenericImport",
                },
                "logistics_item_transfer": {
                    "CoverLogisticsItemExport",
                    "CoverLogisticsItemImport",
                },
                "redstone_conductor": {
                    "CoverRedstoneConductorIN",
                    "CoverRedstoneConductorOUT",
                },
            },
            {
                row["canonical_id"]: set(row["raw_members"])
                for row in cover["declared_transformations"]
            },
        )
        multiblock = self.documents["multiblock_kinds"]
        self.assertEqual(
            {
                "MultiTileEntityTank3x3x3Metal",
                "MultiTileEntityTank3x3x3Wood",
            },
            set(
                multiblock["declared_transformations"][0]["raw_members"]
            ),
        )
        for document in self.documents.values():
            for transformation in document["declared_transformations"]:
                self.assertTrue(transformation["id"])
                self.assertTrue(transformation["statement"])
                self.assertTrue(transformation["source_evidence"])
                self.assertLessEqual(
                    set(transformation["variant_dimensions"]),
                    {"material", "numeric", "direction"},
                )

    def test_cover_counts_and_cc_t19_mapping_are_closed(self):
        cover = self.documents["cover_kinds"]
        self.assertEqual(
            {
                "canonical": 47,
                "classified": 47,
                "classifications": {
                    "deferred_with_reason": 28,
                    "in_scope": 9,
                    "out_of_scope": 10,
                },
                "excluded": 14,
                "implementation_statuses": {
                    "deferred_with_reason": 28,
                    "implemented": 4,
                    "out_of_scope": 10,
                    "selected_t19": 5,
                },
                "raw_classes": 65,
                "raw_files": 65,
                "unclassified": 0,
            },
            cover["counts"],
        )
        rows = {
            row["canonical_id"]: row for row in cover["canonical_kinds"]
        }
        self.assertEqual("FILTER", rows["filter_fluid"]["cc_behavior"])
        self.assertEqual("FILTER", rows["filter_item"]["cc_behavior"])
        self.assertEqual("OUTPUT_PUMP", rows["pump"]["cc_behavior"])
        self.assertEqual(
            "ONE_WAY_VALVE", rows["shutter"]["cc_behavior"]
        )
        self.assertEqual(
            {
                "conveyor",
                "pressure_valve",
                "retriever_item",
                "robot_arm",
                "selector_manual",
            },
            {
                row["canonical_id"]
                for row in rows.values()
                if row["implementation_status"] == "selected_t19"
            },
        )

    def test_multiblock_counts_existing_and_roadmap_mapping_are_closed(self):
        multiblock = self.documents["multiblock_kinds"]
        self.assertEqual(
            {
                "canonical": 30,
                "classified": 30,
                "classifications": {
                    "deferred_with_reason": 20,
                    "in_scope": 5,
                    "out_of_scope": 5,
                },
                "excluded": 13,
                "implementation_statuses": {
                    "implemented": 2,
                    "post_t19": 20,
                    "selected_t23": 3,
                    "out_of_scope": 5,
                },
                "raw_classes": 44,
                "raw_files": 44,
                "unclassified": 0,
            },
            multiblock["counts"],
        )
        rows = {
            row["canonical_id"]: row
            for row in multiblock["canonical_kinds"]
        }
        self.assertEqual(
            "coke_oven", rows["coke_oven"]["cc_multiblock"]
        )
        self.assertEqual(
            "large_centrifuge", rows["centrifuge"]["cc_multiblock"]
        )
        self.assertEqual(
            {
                "distillation_tower",
                "large_boiler",
                "tank_3x3x3",
            },
            {
                row["canonical_id"]
                for row in rows.values()
                if row["implementation_status"] == "selected_t23"
            },
        )
        self.assertEqual(
            set(),
            {
                row["canonical_id"]
                for row in rows.values()
                if row["implementation_status"] == "third_stage_deferred"
            },
        )
        self.assertEqual(
            {
                "bedrock_drill",
                "fusion_reactor",
                "lightning_rod",
                "matter_fabricator",
                "von_da_graagg",
            },
            {
                row["canonical_id"]
                for row in rows.values()
                if row["implementation_status"] == "out_of_scope"
            },
        )

    def test_deferred_rows_have_complete_replacement_contracts(self):
        for domain_id, document in self.documents.items():
            for row in document["canonical_kinds"]:
                if row["disposition"] != "deferred_with_reason":
                    continue
                with self.subTest(
                    domain=domain_id,
                    canonical=row["canonical_id"],
                ):
                    deferred = row["deferred"]
                    self.assertEqual(
                        set(builder.REQUIRED_DEFERRED_FIELDS),
                        set(deferred),
                    )
                    self.assertTrue(all(deferred.values()))

    def test_schema_provenance_cardinality_and_uniform_audit(self):
        for domain_id, document in self.documents.items():
            with self.subTest(domain=domain_id):
                self.assertEqual(1, document["schema_version"])
                self.assertEqual(builder.GENERATION_COMMAND, document[
                    "generation_command"
                ])
                self.assertEqual(
                    self.policy["source"]["tree_sha1"],
                    document["source"]["tree_sha1"],
                )
                self.assertRegex(
                    document["source"]["category_manifest_sha256"],
                    r"^[0-9a-f]{64}$",
                )
                self.assertIn(
                    "git-tree-sha1:",
                    document["source"]["source_digest"],
                )
                self.assertEqual(
                    document["counts"]["raw_classes"],
                    document["field_cardinality"]["raw_source"][
                        "source_symbol"
                    ],
                )
                audit = document["uniform_audit"]
                self.assertEqual("PASS", audit["status"])
                self.assertEqual([], audit["uniform_statuses"])
                self.assertEqual([], audit["undeclared_many_to_one"])
                self.assertEqual(
                    [],
                    audit[
                        "unexplained_field_cardinality_reductions"
                    ],
                )

    def test_partition_and_undeclared_collapse_mutations_fail_closed(self):
        candidate = copy.deepcopy(self.policy)
        candidate["domains"]["cover_kinds"]["exclusion_rules"].pop()
        with self.assertRaisesRegex(ValueError, "partition is not closed"):
            builder.build(candidate)

        candidate = copy.deepcopy(self.policy)
        cover = candidate["domains"]["cover_kinds"]
        fluid = next(
            row
            for row in cover["canonical_rules"]
            if row["canonical_id"] == "filter_fluid"
        )
        fluid["source_symbols"].append("CoverFilterItem")
        cover["canonical_rules"] = [
            row
            for row in cover["canonical_rules"]
            if row["canonical_id"] != "filter_item"
        ]
        cover["classification_assignments"] = [
            row
            for row in cover["classification_assignments"]
            if row["canonical_id"] != "filter_item"
        ]
        with self.assertRaisesRegex(
            ValueError, "many-to-one canonicalization is undeclared"
        ):
            builder.build(candidate)

        candidate = copy.deepcopy(self.policy)
        collapse = next(
            row
            for row in candidate["domains"]["cover_kinds"][
                "canonical_rules"
            ]
            if row["canonical_id"] == "logistics_fluid_transfer"
        )
        collapse["collapse_rule"]["variant_dimensions"] = ["behavior"]
        with self.assertRaisesRegex(
            ValueError, "material/numeric/direction"
        ):
            builder.build(candidate)

    def test_path_and_symbol_mutations_fail_closed(self):
        candidate = copy.deepcopy(self.policy)
        candidate["domains"]["multiblock_kinds"]["source_manifest"][0][
            "source_path"
        ] = "src/main/java/gregapi/tileentity/multiblocks/Other.java"
        with self.assertRaisesRegex(
            ValueError, "source manifest does not match expected paths"
        ):
            builder.build(candidate)

        candidate = copy.deepcopy(self.policy)
        candidate["domains"]["cover_kinds"]["source_manifest"][0][
            "declaration"
        ] = "public class WrongSymbol {"
        with self.assertRaisesRegex(
            ValueError, "stored declaration does not parse"
        ):
            builder.build(candidate)

    def test_committed_tables_are_current_and_check_is_read_only(self):
        before = {
            domain_id: digest(path)
            for domain_id, path in builder.OUTPUTS.items()
        }
        self.assertEqual([], builder.check(self.documents))
        self.assertEqual(
            before,
            {
                domain_id: digest(path)
                for domain_id, path in builder.OUTPUTS.items()
            },
        )
        for domain_id, path in builder.OUTPUTS.items():
            self.assertEqual(
                self.documents[domain_id],
                json.loads(path.read_text(encoding="utf-8")),
            )


if __name__ == "__main__":
    unittest.main()
