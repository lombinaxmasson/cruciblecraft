#!/usr/bin/env python3
"""Semantic-body currentness sidecars (schema v2). Never rewrite giant T35 bodies."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path
from typing import Any

from tools import atomic_io
from tools import semantic_projection as projection
from tools import t35_common as t35

SCHEMA = t35.TOOLS / "currentness.schema.json"
CURRENTNESS_FIELD_NAMES = t35.CURRENTNESS_FIELD_NAMES
GIANT_ARTIFACTS = frozenset(
    {
        "tools/t35_census.json",
        "tools/t35_runtime_registry.json",
        "tools/t35_recipe_families.json",
        "tools/t42_remaining_catalog.json",
        "tools/t42_family_operand_snapshot.json",
        "tools/t42_blocker_overlay.json",
        "tools/t42_operand_disposition.json",
        "tools/t42_reachability_baseline.json",
        "tools/t42_owner_track_overlay.json",
        "tools/t42_owner_recovery_evidence.json",
        "tools/global_build_identity_ledger.json",
    }
)

SIDECAR_SCHEMA_VERSION = 2
SOURCE_REPLAY_STATUSES = frozenset({"unavailable", "bound", "stale"})

DUMP_INDEX = t35.ROOT / "gt6_dump/gt6_recipe_dump/index.json"

TARGETS: tuple[dict[str, Any], ...] = (
    {
        "artifact": "src/main/resources/data/cruciblecraft/material_registration_gate.json",
        "sidecar": "tools/material_registration_gate.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_GATE,
        "builder": "tools/material_form_authority.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t4_tool_readiness.json",
        "sidecar": "tools/t4_tool_readiness.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t4_tool_readiness.py",
        "dependencies": [
            "src/main/resources/data/cruciblecraft/material_registration_gate.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t5_chemical_readiness.json",
        "sidecar": "tools/t5_chemical_readiness.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t5_chemical_readiness.py",
        "dependencies": [
            "src/main/resources/data/cruciblecraft/material_registration_gate.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t5_chemical_recipe_manifest.json",
        "sidecar": "tools/t5_chemical_recipe_manifest.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t5_chemical_recipes.py",
        "dependencies": [
            "src/main/resources/data/cruciblecraft/material_registration_gate.json",
            "tools/t5_chemical_readiness.json",
            "tools/t5_distillery_projection.json",
        ],
        "recipe_roots": [
            "src/t5_chemical_generated/resources/data/cruciblecraft/recipe/t5",
        ],
    },
    {
        "artifact": "tools/t5_distillery_projection.json",
        "sidecar": "tools/t5_distillery_projection.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t5_distillery_projection.py",
        "dependencies": [
            "src/main/resources/data/cruciblecraft/material_registration_gate.json",
        ],
        "recipe_roots": [
            "src/t5_chemical_generated/resources/data/cruciblecraft/recipe/t5/distillery",
        ],
    },
    {
        "artifact": "tools/t14_extruder_readiness.json",
        "sidecar": "tools/t14_extruder_readiness.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t14_extruder_equivalence.py",
        "dependencies": [
            "src/main/resources/data/cruciblecraft/material_registration_gate.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t14_readiness.json",
        "sidecar": "tools/t14_readiness.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t14_readiness.py",
        "dependencies": [
            "tools/t14_extruder_readiness.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t21_chemical_axis.json",
        "sidecar": "tools/t21_chemical_axis.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t21_chemical_axis.py",
        "dependencies": [
            "tools/t5_chemical_readiness.json",
            "tools/t5_chemical_recipe_manifest.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t21_source_denominator.json",
        "sidecar": "tools/t21_source_denominator.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t21_source_denominator.py",
        "dependencies": [
            "tools/t5_chemical_readiness.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t21_template_denominator.json",
        "sidecar": "tools/t21_template_denominator.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t21_template_denominator.py",
        "dependencies": [
            "tools/t5_chemical_recipe_manifest.json",
            "tools/t21_source_denominator.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/component_rule_manifest.json",
        "sidecar": "tools/component_rule_manifest.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_component_rules.py",
        "dependencies": [
            "src/main/resources/data/cruciblecraft/material_registration_gate.json",
            "tools/t14_extruder_readiness.json",
        ],
        "recipe_roots": [
            "src/component_rule_generated/resources/data/cruciblecraft/recipe",
        ],
    },
    {
        "artifact": "tools/t38_player_path_recovery.json",
        "sidecar": "tools/t38_player_path_recovery.currentness.json",
        "scope": "recipes",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t38_player_path_recovery.py",
        "dependencies": [
            "src/main/resources/data/cruciblecraft/material_registration_gate.json",
        ],
        "recipe_roots": [
            "src/main/resources/data/cruciblecraft/recipe/t38_player_path_recovery",
        ],
    },
    {
        "artifact": "tools/t35_recipe_families.json",
        "sidecar": "tools/t35_recipe_families.currentness.json",
        "scope": "t35",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t35_recipe_families.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t35_runtime_registry.json",
        "sidecar": "tools/t35_runtime_registry.currentness.json",
        "scope": "t35",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t35_runtime_registry.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t35_census.json",
        "sidecar": "tools/t35_census.currentness.json",
        "scope": "t35",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t35_census.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t35_card_topology.json",
        "sidecar": "tools/t35_card_topology.currentness.json",
        "scope": "t35",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t35_card_topology.py",
        "dependencies": ["tools/t35_census.json"],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t35_readiness.json",
        "sidecar": "tools/t35_readiness.currentness.json",
        "scope": "t35",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t35_readiness.py",
        "dependencies": ["tools/t35_census.json", "tools/t35_card_topology.json"],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t39_census_delta.json",
        "sidecar": "tools/t39_census_delta.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t39_census_delta.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t39_card_topology.json",
        "sidecar": "tools/t39_card_topology.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t39_card_topology.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t39_readiness.json",
        "sidecar": "tools/t39_readiness.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t39_readiness.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t40_census_delta.json",
        "sidecar": "tools/t40_census_delta.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t40_census_delta.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t40_card_topology.json",
        "sidecar": "tools/t40_card_topology.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t40_card_topology.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t40_readiness.json",
        "sidecar": "tools/t40_readiness.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t40_readiness.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t41_census_delta.json",
        "sidecar": "tools/t41_census_delta.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t41_census_delta.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t41_card_topology.json",
        "sidecar": "tools/t41_card_topology.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t41_card_topology.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t41_readiness.json",
        "sidecar": "tools/t41_readiness.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t41_readiness.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_partition_freeze.json",
        "sidecar": "tools/t42_partition_freeze.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_partition_freeze.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_remaining_catalog.json",
        "sidecar": "tools/t42_remaining_catalog.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_partition_freeze.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_family_operand_snapshot.json",
        "sidecar": "tools/t42_family_operand_snapshot.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_family_operand_snapshot.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_runtime_expression_inventory.json",
        "sidecar": "tools/t42_runtime_expression_inventory.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_runtime_expression_inventory.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_reachability_baseline.json",
        "sidecar": "tools/t42_reachability_baseline.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_reachability_baseline.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_blocker_overlay.json",
        "sidecar": "tools/t42_blocker_overlay.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_blocker_overlay.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_disposition_lock.json",
        "sidecar": "tools/t42_disposition_lock.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_disposition_lock.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_gap_partition.json",
        "sidecar": "tools/t42_gap_partition.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_gap_partition.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_census_delta.json",
        "sidecar": "tools/t42_census_delta.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_census_delta.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_card_topology.json",
        "sidecar": "tools/t42_card_topology.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_card_topology.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_readiness.json",
        "sidecar": "tools/t42_readiness.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_readiness.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_repair_pre_freeze.json",
        "sidecar": "tools/t42_repair_pre_freeze.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_repair_pre_freeze.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_repair_readiness.json",
        "sidecar": "tools/t42_repair_readiness.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_repair_readiness.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_owner_pre_freeze.json",
        "sidecar": "tools/t42_owner_pre_freeze.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_owner_pre_freeze.py",
        "dependencies": [
            "tools/t42_repair_readiness.json",
            "tools/t42_blocker_overlay.json",
            "tools/t42_disposition_lock.json",
            "tools/t42_gap_partition.json",
            "tools/t42_card_topology.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_owner_track_overlay.json",
        "sidecar": "tools/t42_owner_track_overlay.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_owner_track_overlay.py",
        "dependencies": [
            "tools/t42_owner_pre_freeze.json",
            "tools/t42_family_operand_snapshot.json",
            "tools/t42_blocker_overlay.json",
            "tools/t42_disposition_lock.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_owner_recovery_evidence.json",
        "sidecar": "tools/t42_owner_recovery_evidence.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_owner_track_overlay.py",
        "dependencies": [
            "tools/t42_owner_pre_freeze.json",
            "tools/t42_family_operand_snapshot.json",
            "tools/t42_blocker_overlay.json",
            "tools/t42_disposition_lock.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_owner_disposition_lock.json",
        "sidecar": "tools/t42_owner_disposition_lock.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_owner_disposition_lock.py",
        "dependencies": [
            "tools/t42_owner_track_overlay.json",
            "tools/t42_owner_recovery_evidence.json",
            "tools/t42_owner_object_expression_evidence.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_owner_gap_partition.json",
        "sidecar": "tools/t42_owner_gap_partition.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_owner_gap_partition.py",
        "dependencies": [
            "tools/t42_owner_disposition_lock.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t42_owner_readiness.json",
        "sidecar": "tools/t42_owner_readiness.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t42_owner_readiness.py",
        "dependencies": [
            "tools/t42_owner_pre_freeze.json",
            "tools/t42_owner_track_overlay.json",
            "tools/t42_owner_recovery_evidence.json",
            "tools/t42_owner_disposition_lock.json",
            "tools/t42_owner_gap_partition.json",
            "tools/t42_card_topology.json",
        ],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t43_census_delta.json",
        "sidecar": "tools/t43_census_delta.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t43_census_delta.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t43_card_topology.json",
        "sidecar": "tools/t43_card_topology.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t43_card_topology.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t43_readiness.json",
        "sidecar": "tools/t43_readiness.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t43_readiness.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t36_repair_pre_freeze.json",
        "sidecar": "tools/t36_repair_pre_freeze.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t36_repair_pre_freeze.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t36_repair_second_list_inventory.json",
        "sidecar": "tools/t36_repair_second_list_inventory.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t36_repair_second_list_inventory.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t36_repair_readiness.json",
        "sidecar": "tools/t36_repair_readiness.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t36_repair_readiness.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t44_storage_work_set.json",
        "sidecar": "tools/t44_storage_work_set.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t44_storage_work_set.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t44_storage_source.json",
        "sidecar": "tools/t44_storage_source.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t44_storage_source.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t44_storage_production_lock.json",
        "sidecar": "tools/t44_storage_production_lock.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t44_storage_production_lock.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t44_storage_census_delta.json",
        "sidecar": "tools/t44_storage_census_delta.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t44_storage_census_delta.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t44_card_topology.json",
        "sidecar": "tools/t44_card_topology.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t44_card_topology.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t44_readiness.json",
        "sidecar": "tools/t44_readiness.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t44_storage_readiness.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t45_census_delta.json",
        "sidecar": "tools/t45_census_delta.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t45_census_delta.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t45_card_topology.json",
        "sidecar": "tools/t45_card_topology.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t45_card_topology.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/t45_readiness.json",
        "sidecar": "tools/t45_readiness.currentness.json",
        "scope": "card-closeout",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_t45_readiness.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/recipe_wave_production_baseline.json",
        "sidecar": "tools/recipe_wave_production_baseline.currentness.json",
        "scope": "verification",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_recipe_wave_production_baseline.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/global_build_identity_ledger.json",
        "sidecar": "tools/global_build_identity_ledger.currentness.json",
        "scope": "verification",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_global_build_identity_ledger.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/recipe_wave_shadow_parity.json",
        "sidecar": "tools/recipe_wave_shadow_parity.currentness.json",
        "scope": "verification",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_recipe_wave_shadow_parity.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/unified_import_shadow_readiness.json",
        "sidecar": "tools/unified_import_shadow_readiness.currentness.json",
        "scope": "verification",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_unified_import_shadow_readiness.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/compact_recipe_runtime_manifest.json",
        "sidecar": "tools/compact_recipe_runtime_manifest.currentness.json",
        "scope": "verification",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_compact_recipe_runtime_manifest.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/compact_recipe_manifest_cutover_readiness.json",
        "sidecar": "tools/compact_recipe_manifest_cutover_readiness.currentness.json",
        "scope": "verification",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_compact_recipe_manifest_cutover_readiness.py",
        "dependencies": [],
        "recipe_roots": [],
    },
    {
        "artifact": "tools/unified_recipe_compile_readiness.json",
        "sidecar": "tools/unified_recipe_compile_readiness.currentness.json",
        "scope": "verification",
        "kind": projection.KIND_LEDGER,
        "builder": "tools/build_recipe_compile_cutover_readiness.py",
        "dependencies": [
            "tools/unified_import_shadow_readiness.json",
            "tools/compact_recipe_manifest_cutover_readiness.json",
            "tools/global_build_identity_ledger.json",
            "tools/recipe_wave_production_baseline.json",
        ],
        "recipe_roots": [],
    },
)

_TARGETS_BY_ARTIFACT = {row["artifact"]: row for row in TARGETS}


def artifact_label(artifact: Path) -> str:
    try:
        return t35.relative(artifact)
    except ValueError:
        return artifact.as_posix()


def target_row(artifact: Path) -> dict[str, Any] | None:
    return _TARGETS_BY_ARTIFACT.get(artifact_label(artifact))


def sidecar_path(artifact: Path) -> Path:
    row = target_row(artifact)
    if row is not None:
        return t35.ROOT / row["sidecar"]
    return artifact.with_name(artifact.name.replace(".json", ".currentness.json"))


def legacy_owned_inputs(document: dict[str, Any]) -> dict[str, str]:
    currentness = document.get("currentness")
    if isinstance(currentness, dict) and isinstance(currentness.get("owned_inputs"), dict):
        return {
            str(key): str(value)
            for key, value in currentness["owned_inputs"].items()
            if isinstance(value, str)
        }
    owned = document.get("owned_inputs")
    if isinstance(owned, dict):
        return {str(key): str(value) for key, value in owned.items() if isinstance(value, str)}
    return {}


def semantic_root(document: Any) -> Any:
    return projection.ledger_projection(document)["semantic_body"]


def semantic_root_sha256(document: Any) -> str:
    return projection.sha256_canonical(projection.ledger_projection(document))


def _kind_for(artifact: Path, row: dict[str, Any] | None) -> str:
    if row is not None:
        return str(row["kind"])
    return projection.KIND_LEDGER


def _builder_path(row: dict[str, Any] | None) -> Path | None:
    if row is None or not row.get("builder"):
        return None
    path = t35.ROOT / str(row["builder"])
    return path if path.is_file() else None


def _live_dependency_roots(row: dict[str, Any] | None) -> dict[str, dict[str, str]]:
    roots: dict[str, dict[str, str]] = {}
    if row is None:
        return roots
    for relative in row.get("dependencies") or []:
        path = t35.ROOT / relative
        dep_row = _TARGETS_BY_ARTIFACT.get(relative)
        kind = str(dep_row["kind"]) if dep_row else projection.KIND_LEDGER
        live = projection.project_artifact(path, kind=kind)
        roots[relative] = {
            "kind": live["kind"],
            "semantic_projection_version": str(projection.SEMANTIC_PROJECTION_VERSION),
            "semantic_root_sha256": live["semantic_root_sha256"],
        }
    for relative in row.get("recipe_roots") or []:
        path = t35.ROOT / relative
        live = projection.project_recipe_tree(path)
        roots[relative] = {
            "kind": projection.KIND_RECIPE_TREE,
            "semantic_projection_version": str(projection.SEMANTIC_PROJECTION_VERSION),
            "semantic_root_sha256": live["semantic_root_sha256"],
        }
    return roots


def _source_replay_receipt(existing: dict[str, Any] | None) -> dict[str, str]:
    if existing:
        receipt = existing.get("source_replay_receipt")
        if isinstance(receipt, dict) and receipt.get("status") in SOURCE_REPLAY_STATUSES:
            return {
                "status": str(receipt["status"]),
                "proof_tier": str(receipt.get("proof_tier") or "compact"),
            }
    if DUMP_INDEX.is_file():
        return {"status": "unavailable", "proof_tier": "compact"}
    return {"status": "unavailable", "proof_tier": "compact"}


def preserve_source_replay_receipt(
    existing: dict[str, Any] | None,
    *,
    dump_available: bool,
) -> dict[str, str]:
    """Full-replay SKIP must not upgrade a historical bound receipt."""
    if existing:
        receipt = existing.get("source_replay_receipt")
        if isinstance(receipt, dict) and receipt.get("status") == "bound" and not dump_available:
            return {
                "status": "bound",
                "proof_tier": str(receipt.get("proof_tier") or "full_replay"),
            }
    if not dump_available:
        return {"status": "unavailable", "proof_tier": "compact"}
    if existing:
        receipt = existing.get("source_replay_receipt")
        if isinstance(receipt, dict) and receipt.get("status") == "bound":
            return {
                "status": "bound",
                "proof_tier": str(receipt.get("proof_tier") or "full_replay"),
            }
    return {"status": "unavailable", "proof_tier": "compact"}


def sidecar_document(artifact: Path, document: dict[str, Any] | None = None) -> dict[str, Any]:
    row = target_row(artifact)
    kind = _kind_for(artifact, row)
    if document is None and kind != projection.KIND_RECIPE_TREE:
        document = read_artifact(artifact)
    live = projection.project_artifact(artifact, kind=kind, document=document)
    builder = _builder_path(row)
    existing = None
    sidecar_file = sidecar_path(artifact)
    if sidecar_file.is_file():
        try:
            existing = load_sidecar(sidecar_file)
        except (OSError, ValueError, json.JSONDecodeError):
            existing = None
    return {
        "schema_version": SIDECAR_SCHEMA_VERSION,
        "status": "CURRENTNESS_SIDECAR",
        "artifact": artifact_label(artifact),
        "giant_body": artifact_label(artifact) in GIANT_ARTIFACTS,
        "semantic_projection_kind": live["kind"],
        "semantic_projection_version": projection.SEMANTIC_PROJECTION_VERSION,
        "artifact_file_sha256": projection.sha256_file(artifact),
        "semantic_root_sha256": live["semantic_root_sha256"],
        "builder_sha256": projection.sha256_file(builder) if builder else None,
        "envelope_sha256": live["envelope_sha256"],
        "dependency_semantic_roots": _live_dependency_roots(row),
        "source_replay_receipt": preserve_source_replay_receipt(
            existing,
            dump_available=DUMP_INDEX.is_file(),
        ),
        "generated_by": "python tools/currentness.py",
    }


def load_sidecar(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def read_artifact(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def write_sidecar(artifact: Path, document: dict[str, Any] | None = None) -> dict[str, Any]:
    sidecar = sidecar_document(artifact, document)
    atomic_io.write_bytes(
        sidecar_path(artifact),
        t35.stable_json(sidecar).encode("utf-8"),
    )
    return sidecar


def write_scope(scope: str) -> list[str]:
    written: list[str] = []
    for row in TARGETS:
        if scope not in {row["scope"], "all"}:
            continue
        artifact = t35.ROOT / row["artifact"]
        if not artifact.is_file() and row["kind"] != projection.KIND_RECIPE_TREE:
            continue
        if row["kind"] == projection.KIND_RECIPE_TREE and not artifact.exists():
            continue
        write_sidecar(artifact)
        written.append(row["sidecar"])
    return written


def rebind_sidecar(
    artifact: Path,
    *,
    mode: str = "envelope",
    prove_rebuild: bool = False,
) -> dict[str, Any]:
    """Refresh pairing/envelope or builder hash. Never upgrades source_replay_receipt."""
    document = read_artifact(artifact)
    sidecar_file = sidecar_path(artifact)
    if not sidecar_file.is_file():
        return write_sidecar(artifact, document)
    existing = load_sidecar(sidecar_file)
    live = sidecar_document(artifact, document)
    if existing.get("semantic_root_sha256") != live["semantic_root_sha256"]:
        raise ValueError(
            f"SEMANTIC_DRIFT: {artifact_label(artifact)} semantic root changed; "
            "refusing currentness-only rebind"
        )
    try:
        projection.require_version(existing.get("semantic_projection_version"))
    except projection.ProjectionError as error:
        raise ValueError(str(error)) from error
    refreshed = dict(existing)
    if mode == "envelope":
        refreshed["artifact_file_sha256"] = live["artifact_file_sha256"]
        refreshed["envelope_sha256"] = live["envelope_sha256"]
        refreshed["dependency_semantic_roots"] = live["dependency_semantic_roots"]
        refreshed["generated_by"] = "python tools/rebind_currentness.py --envelope"
    elif mode == "builder":
        if not prove_rebuild:
            raise ValueError(
                f"builder-only rebind refused for {artifact_label(artifact)}: "
                "prove_rebuild is required"
            )
        refreshed["builder_sha256"] = live["builder_sha256"]
        refreshed["generated_by"] = "python tools/rebind_currentness.py --builder"
    else:
        raise ValueError(f"unknown currentness rebind mode {mode!r}")
    refreshed["source_replay_receipt"] = preserve_source_replay_receipt(
        existing,
        dump_available=DUMP_INDEX.is_file(),
    )
    atomic_io.write_bytes(sidecar_file, t35.stable_json(refreshed).encode("utf-8"))
    return refreshed


def check_sidecar(artifact: Path, *, include_hash_only: bool = False) -> list[str]:
    sidecar_file = sidecar_path(artifact)
    label = artifact_label(artifact)
    if not sidecar_file.is_file():
        return [f"MISSING: {artifact_label(sidecar_file)} is stale (missing)"]
    if not artifact.is_file():
        return [f"MISSING: {label} is stale (missing)"]
    try:
        actual = load_sidecar(sidecar_file)
    except (OSError, ValueError, json.JSONDecodeError) as error:
        return [f"CORRUPT: {artifact_label(sidecar_file)} {error}"]
    try:
        projection.require_version(actual.get("semantic_projection_version"))
    except projection.ProjectionError as error:
        return [str(error)]
    errors: list[str] = []
    live_file = projection.sha256_file(artifact)
    if actual.get("artifact_file_sha256") != live_file:
        errors.append(
            f"CORRUPT: {label} body/sidecar pairing drifted "
            f"(artifact_file_sha256)"
        )
    live = sidecar_document(artifact)
    if actual.get("semantic_root_sha256") != live["semantic_root_sha256"]:
        errors.append(f"SEMANTIC_DRIFT: {label} semantic root changed")
    if actual.get("dependency_semantic_roots") != live["dependency_semantic_roots"]:
        errors.append(f"SEMANTIC_DRIFT: {label} dependency semantic roots changed")
    if include_hash_only:
        if actual.get("envelope_sha256") != live["envelope_sha256"]:
            errors.append(
                f"HASH_ONLY_DRIFT: {label} envelope changed; rebind --envelope"
            )
        row = target_row(artifact)
        builder = _builder_path(row)
        if (
            builder is not None
            and actual.get("builder_sha256") != projection.sha256_file(builder)
        ):
            errors.append(
                f"HASH_ONLY_DRIFT: {label} builder changed; prove rebuild before rebind"
            )
    receipt = actual.get("source_replay_receipt") or {}
    if receipt.get("status") not in SOURCE_REPLAY_STATUSES:
        errors.append(f"{label} source_replay_receipt status is invalid")
    return errors


def check_rebuilt(artifact: Path, rebuilt: dict[str, Any]) -> list[str]:
    """Sidecar check plus rebuilt semantic root vs committed body."""
    errors = list(check_sidecar(artifact))
    if not artifact.is_file():
        return errors
    disk = read_artifact(artifact)
    row = target_row(artifact)
    kind = _kind_for(artifact, row)
    live_disk = projection.project_artifact(artifact, kind=kind, document=disk)
    live_rebuilt = projection.project_artifact(artifact, kind=kind, document=rebuilt)
    if live_disk["semantic_root_sha256"] != live_rebuilt["semantic_root_sha256"]:
        errors.append(
            f"SEMANTIC_DRIFT: {artifact_label(artifact)} rebuilt semantic root drifted"
        )
    return errors
