from __future__ import annotations

import copy
import json
import re
import unittest
from collections import Counter, defaultdict

from tools import build_t13_prefix_domain_denominators as denominators


def independent_op_expectation(source: str) -> tuple[set[str], dict[str, str], dict[str, str]]:
    """Line-oriented expectation parser; intentionally does not use production parsing."""
    start = source.index("public static final OreDictPrefix")
    end = source.index("public static final OreDictPrefix[]", start)
    canonical: set[str] = set()
    aliases: dict[str, str] = {}
    variants: dict[str, str] = {}
    field_to_identity: dict[str, str] = {}
    pending_aliases: list[tuple[str, list[str]]] = []
    for line in source[start:end].splitlines():
        code = line.split("//", 1)[0].strip().rstrip(",;")
        created = re.match(
            r'([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*'
            r'(?:create|unused)\(\s*"([^"]+)"',
            code,
        )
        if created:
            field, identity = created.groups()
            canonical.add(identity)
            field_to_identity[field] = identity
            alias_call = re.search(r"\.addIdenticalNames\(([^)]*)\)", code)
            if alias_call:
                pending_aliases.append(
                    (
                        identity,
                        re.findall(r'"([^"]+)"', alias_call.group(1)),
                    )
                )
            continue
        variant = re.match(
            r"([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*"
            r"([A-Za-z_$][A-Za-z0-9_$]*)$",
            code,
        )
        if variant:
            variants[variant.group(1)] = variant.group(2)
    for identity, names in pending_aliases:
        for name in names:
            if name in aliases:
                raise AssertionError(f"duplicate independent alias: {name}")
            aliases[name] = identity
    resolved_variants = {
        field: field_to_identity[target] for field, target in variants.items()
    }
    return canonical, aliases, resolved_variants


def independent_td_expectation(source: str) -> tuple[dict[str, str], dict[str, str]]:
    start = source.index("public static class ItemGenerator")
    body = source[start:]
    declarations = {
        identity: field
        for field, identity in re.findall(
            r"([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*"
            r'TagData\.createTagData\(\s*"(ITEMGENERATOR\.[A-Z0-9_]+)"',
            body,
        )
    }
    by_field = {field: identity for identity, field in declarations.items()}
    aliases = {}
    for field, target in re.findall(
        r"\b([A-Za-z_$][A-Za-z0-9_$]*)\s*=\s*"
        r"([A-Za-z_$][A-Za-z0-9_$]*)\s*[,;]",
        body,
    ):
        if field not in by_field and target in by_field:
            aliases[field] = by_field[target]
    return declarations, aliases


class T13PrefixDomainDenominatorTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.policy = denominators.load(denominators.POLICY_PATH)
        source_paths = [
            denominators.source_cache_path(
                cls.policy["fixed_source"][source]
            )
            for source in ("op_java", "td_java")
        ]
        if not all(path.is_file() for path in source_paths):
            raise unittest.SkipTest(
                "fixed Java source replay is reserved for source-replay"
            )
        cls.op_source, cls.op_entry = denominators.load_fixed_source(
            cls.policy["fixed_source"]["op_java"],
            allow_fetch=False,
        )
        cls.td_source, cls.td_entry = denominators.load_fixed_source(
            cls.policy["fixed_source"]["td_java"],
            allow_fetch=False,
        )
        cls.prefix_artifact = denominators.load(denominators.PREFIX_OUTPUT)
        cls.domain_artifact = denominators.load(denominators.DOMAIN_OUTPUT)
        cls.normalized_prefixes = denominators.load(
            denominators.NORMALIZED_PREFIXES
        )
        cls.normalized_materials = denominators.load(
            denominators.NORMALIZED_MATERIALS
        )

    def test_committed_artifacts_are_current(self) -> None:
        self.assertEqual([], denominators.reference_only_check())

    def test_independent_op_parser_locks_raw_to_canonical_both_ways(self) -> None:
        canonical, aliases, variants = independent_op_expectation(self.op_source)
        self.assertEqual(452, len(canonical))
        self.assertEqual(17, len(aliases))
        self.assertEqual({"oreHee": "oreEndstone"}, variants)

        artifact_records = {
            row["canonical_id"]: row for row in self.prefix_artifact["records"]
        }
        self.assertEqual(canonical, set(artifact_records))
        raw_names = {
            row["source_name"]
            for row in self.normalized_prefixes["records"]
        }
        self.assertEqual(468, len(raw_names))
        independently_assigned = {}
        for raw_name in raw_names:
            if raw_name in canonical:
                independently_assigned[raw_name] = raw_name
            elif raw_name in aliases:
                independently_assigned[raw_name] = aliases[raw_name]
            elif raw_name in variants:
                independently_assigned[raw_name] = variants[raw_name]
            else:
                independently_assigned[raw_name] = f"excluded:{raw_name}"

        artifact_assignments = {
            row["raw_key"]: row["canonical_key"]
            for row in self.prefix_artifact["raw_membership"]
        }
        self.assertEqual(independently_assigned, artifact_assignments)
        reverse_expected: dict[str, set[str]] = defaultdict(set)
        for raw_name, canonical_id in independently_assigned.items():
            if not canonical_id.startswith("excluded:"):
                reverse_expected[canonical_id].add(raw_name)
        for canonical_id, record in artifact_records.items():
            self.assertEqual(
                reverse_expected.get(canonical_id, set()),
                {row["raw_key"] for row in record["raw_members"]},
            )

    def test_prefix_types_are_not_a_subtraction_claim(self) -> None:
        summary = self.prefix_artifact["summary"]
        self.assertEqual(452, summary["op_canonical_count"])
        self.assertEqual(468, summary["raw_dump_count"])
        self.assertEqual(56, summary["cc_runtime_prefix_count"])
        self.assertEqual(1, summary["op_field_variant_count"])
        self.assertEqual(0, summary["op_field_variants_present_in_dump_count"])
        self.assertEqual(17, summary["op_identical_name_alias_count"])
        self.assertEqual(1, summary["op_shadowed_alias_count"])
        self.assertEqual(
            {
                "canonical_identity": 452,
                "dump_only": 0,
                "identical_name_alias": 16,
                "source_field_variant": 0,
            },
            summary["raw_relation_counts"],
        )
        self.assertEqual(0, summary["dump_only_count"])
        self.assertEqual(
            [
                {
                    "canonical_owner": "raw",
                    "declared_alias_owner": "oreRaw",
                    "raw_key": "raw",
                    "resolution": "canonical_declaration_precedence",
                    "transformation_rule": "PFX_CANONICAL_PRECEDENCE",
                }
            ],
            self.prefix_artifact["shadowed_identical_name_aliases"],
        )
        self.assertEqual(
            {
                "deferred_with_reason": 272,
                "in_scope": 54,
                "out_of_scope": 126,
            },
            summary["classification_counts"],
        )
        self.assertIn(
            "not an implementation count",
            self.prefix_artifact["difference"]["semantics"],
        )

    def test_all_57_runtime_prefixes_have_declared_mapping(self) -> None:
        target_counts = Counter(
            row["cc_prefix"]
            for row in self.prefix_artifact["cc_mapping_relations"]
        )
        self.assertEqual(57, len(target_counts))
        self.assertEqual(2, target_counts["block"])
        self.assertEqual(
            {"block"},
            {target for target, count in target_counts.items() if count > 1},
        )
        source_counts = Counter(
            row["source_prefix"]
            for row in self.prefix_artifact["cc_mapping_relations"]
        )
        self.assertEqual(2, source_counts["pipeMedium"])
        self.assertEqual(2, source_counts["pipeLarge"])
        self.assertEqual(2, source_counts["pipeHuge"])

    def test_independent_domain_parser_and_material_scan_lock_25_of_25(self) -> None:
        declarations, aliases = independent_td_expectation(self.td_source)
        self.assertEqual(33, len(declarations))
        self.assertEqual({"GASSES": "ITEMGENERATOR.GASES"}, aliases)
        report_rows = denominators.load(denominators.TAG_DOMAIN_REPORT)["tags"][
            "ITEMGENERATOR"
        ]
        report_counts = {
            row["tag"]: row["material_count"] for row in report_rows
        }
        independently_scanned: dict[str, set[int]] = defaultdict(set)
        for row in self.normalized_materials["records"]:
            source_id = row.get("source_id")
            if not isinstance(source_id, int) or source_id < 0:
                continue
            for tag in row.get("generation_tags") or []:
                if tag.startswith("ITEMGENERATOR."):
                    independently_scanned[tag].add(source_id)
        self.assertEqual(
            report_counts,
            {tag: len(ids) for tag, ids in independently_scanned.items()},
        )
        artifact_counts = {
            row["canonical_id"]: row["source_material_count"]
            for row in self.domain_artifact["records"]
        }
        self.assertEqual(report_counts, artifact_counts)
        self.assertEqual(set(report_counts), set(declarations) - {
            row["canonical_id"]
            for row in self.domain_artifact["source_only_exclusions"]
        })

    def test_domain_classification_and_g10_boundary_are_explicit(self) -> None:
        summary = self.domain_artifact["summary"]
        self.assertEqual(25, summary["observed_domain_count"])
        self.assertEqual(16, summary["cc_current_domain_count"])
        self.assertEqual(
            {
                "deferred_with_reason": 3,
                "in_scope": 16,
                "out_of_scope": 6,
            },
            summary["classification_counts"],
        )
        self.assertEqual(0, summary["unclassified_count"])
        records = self.domain_artifact["records"]
        peripheral = [
            row for row in records if row["scope_group"] == "G10_peripheral"
        ]
        self.assertGreaterEqual(len(peripheral), 7)
        self.assertTrue(
            all(
                row["classification"]
                in {"deferred_with_reason", "out_of_scope"}
                for row in peripheral
            )
        )

    def test_vocab_deferred_fields_schema_and_uniform_audits(self) -> None:
        vocab = set(self.policy["classification_vocab"])
        owners = set(self.policy["owner_vocab"])
        all_rows = (
            self.prefix_artifact["records"]
            + self.prefix_artifact["excluded_raw_members"]
            + self.domain_artifact["records"]
            + self.domain_artifact["source_only_exclusions"]
        )
        for row in all_rows:
            self.assertIn(row["classification"], vocab)
            self.assertIn(row["owner"], owners)
            if row["classification"] == "deferred_with_reason":
                self.assertTrue(denominators.DEFERRED_FIELDS <= set(row))
        self.assertEqual("PASS", self.prefix_artifact["uniform_audit"]["status"])
        self.assertEqual("PASS", self.domain_artifact["uniform_audit"]["status"])
        self.assertEqual([], self.prefix_artifact["uniform_audit"]["blockers"])
        self.assertEqual([], self.domain_artifact["uniform_audit"]["blockers"])
        self.assertIn("field_cardinality", self.prefix_artifact)
        self.assertIn("field_cardinality", self.domain_artifact)

    def test_source_digests_blob_and_generation_command_are_locked(self) -> None:
        denominators.verify_git_blob(
            self.op_source.encode("utf-8"),
            self.policy["fixed_source"]["op_java"]["git_blob_sha1"],
            "OP.java",
        )
        denominators.verify_git_blob(
            self.td_source.encode("utf-8"),
            self.policy["fixed_source"]["td_java"]["git_blob_sha1"],
            "TD.java",
        )
        command = "python tools/build_t13_prefix_domain_denominators.py"
        self.assertEqual(command, self.prefix_artifact["generation"]["command"])
        self.assertEqual(command, self.domain_artifact["generation"]["command"])
        self.assertEqual(
            self.op_entry["sha256"],
            self.prefix_artifact["source"]["op_java"]["sha256"],
        )
        self.assertEqual(
            self.td_entry["sha256"],
            self.domain_artifact["source"]["td_java"]["sha256"],
        )

    def test_mutations_and_undeclared_collapses_fail_closed(self) -> None:
        with self.assertRaises(ValueError):
            denominators.verify_git_blob(
                self.op_source.encode("utf-8") + b" ",
                self.policy["fixed_source"]["op_java"]["git_blob_sha1"],
                "mutated OP.java",
            )

        mutated_prefixes = copy.deepcopy(self.normalized_prefixes)
        mutated_prefixes["records"].pop()
        with self.assertRaisesRegex(ValueError, "count drifted"):
            denominators.build_prefix_document(
                self.op_source,
                self.op_entry,
                self.policy,
                mutated_prefixes,
                denominators.load(denominators.PREFIX_MAPPING),
                denominators.load(denominators.L3_PREFIX_PLAN),
                denominators.runtime_prefix_documents(),
            )

        mutated_report = denominators.load(denominators.TAG_DOMAIN_REPORT)
        mutated_report["tags"]["ITEMGENERATOR"][0]["material_count"] += 1
        with self.assertRaisesRegex(ValueError, "independent material count"):
            denominators.build_domain_document(
                self.td_source,
                self.td_entry,
                self.policy,
                mutated_report,
                denominators.load(denominators.GENERATION_BITS),
                self.normalized_materials,
                denominators.runtime_prefix_documents(),
                self.prefix_artifact,
            )

        audit = denominators.audit_assignments(
            [
                {
                    "raw_key": "one",
                    "canonical_key": "combined",
                    "transformation_rule": "DECLARED",
                },
                {
                    "raw_key": "two",
                    "canonical_key": "combined",
                    "transformation_rule": "NOT_DECLARED",
                },
            ],
            {"DECLARED"},
        )
        self.assertEqual("BLOCKED", audit["status"])
        self.assertIn(
            "UNIFORM_UNDECLARED_COLLAPSE",
            {row["code"] for row in audit["blockers"]},
        )
        field_blockers = denominators.audit_field_policy(
            [{"identity": "x", "silently_lost": 1}],
            {"identity": "identity"},
        )
        self.assertIn(
            "UNIFORM_UNDECLARED_FIELD_COLLAPSE",
            {row["code"] for row in field_blockers},
        )


if __name__ == "__main__":
    unittest.main()
