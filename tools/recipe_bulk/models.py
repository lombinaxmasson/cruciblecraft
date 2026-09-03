#!/usr/bin/python3
"""Typed models for the unified recipe-wave compile authority."""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Literal

from tools.recipe_bulk.matrix import authored_relations

Archetype = Literal[
    "no_lock_singleton",
    "no_lock_relation_set",
    "lock_relation_set",
    "lock_singleton",
]
SelectionPolicy = Literal["all", "lock_templates", "exclude_combinatorial"]
PublicationPolicy = Literal[
    "omit",
    "lock",
    "constant",
    "expanded_count",
    "relation",
]
PathLayout = Literal["flat", "host_nested", "cohort_nested"]
CompileAuthority = Literal["recipe_bulk"]
RelationSort = Literal["template_key", "source_recipe_index_then_stable_id"]
StableIdPolicy = Literal["source", "hex_suffix", "lock"]
TargetMapPolicy = Literal["spec", "relation", "lock_host"]
SourceKindPolicy = Literal[
    "operand_design_first",
    "operand_derived_or_backed",
    "relation_provenance",
]


@dataclass(frozen=True)
class WaveSpec:
    wave_id: str
    archetype: Archetype
    template_kind: str
    host: str
    target_map: str
    source_path: Path
    generated_root: Path
    equivalence_path: Path
    selection_policy: SelectionPolicy
    publication_policy: PublicationPolicy
    path_layout: PathLayout
    compile_authority: CompileAuthority
    relation_sort: RelationSort
    stable_id_policy: StableIdPolicy
    target_map_policy: TargetMapPolicy
    source_kind_policy: SourceKindPolicy
    lock_path: Path | None = None
    operand_map_path: Path | None = None
    default_publication_group: str | None = None
    singleton_publication_group: str | None = None
    multi_publication_group: str | None = None
    stable_id_prefix: str | None = None
    combinatorial_template_keys: tuple[str, ...] = ()
    expected_family_count: int | None = None
    expected_relation_count: int | None = None
    identity_only: bool = False
    wave_slug: str | None = None
    cohort: str | None = None
    representation: str | None = None
    depends_on_slugs: tuple[str, ...] = ()
    path_prefix: str | None = None
    dry_run_without_lock: bool = False


@dataclass
class ShadowFamily:
    semantic_family_id: str
    template_kind: str
    relative_path: str
    target_map: str
    source_revision: str
    document: dict[str, Any]
    source_rows: list[Any]
    resolved_operands: list[dict[str, Any]]
    cardinality_proof: dict[str, Any]
    provenance: dict[str, Any]
    publication_group: str | None = None
    lock_row: dict[str, Any] | None = None


@dataclass
class WaveIR:
    wave_id: str
    archetype: str
    template_kind: str
    source_revision: str
    source_path: str
    source_sha256: str
    target_map: str
    families: list[ShadowFamily] = field(default_factory=list)
    lock_path: str | None = None
    lock_sha256: str | None = None

    def to_document(self) -> dict[str, Any]:
        families: list[dict[str, Any]] = []
        for family in self.families:
            families.append(
                {
                    "cardinality_proof": family.cardinality_proof,
                    "family_kind": "compact_gt_recipe_family",
                    "lock_row": family.lock_row,
                    "provenance": family.provenance,
                    "publication_group": family.publication_group,
                    "relative_path": family.relative_path,
                    "relations": authored_relations(family.document),
                    "resolved_operands": family.resolved_operands,
                    "semantic_family_id": family.semantic_family_id,
                    "source_revision": family.source_revision,
                    "source_rows": family.source_rows,
                    "target_map": family.target_map,
                    "template_kind": family.template_kind,
                }
            )
        return {
            "archetype": self.archetype,
            "families": families,
            "lock_path": self.lock_path,
            "lock_sha256": self.lock_sha256,
            "schema_version": 1,
            "source_path": self.source_path,
            "source_revision": self.source_revision,
            "source_sha256": self.source_sha256,
            "target_map": self.target_map,
            "template_kind": self.template_kind,
            "wave_id": self.wave_id,
        }
