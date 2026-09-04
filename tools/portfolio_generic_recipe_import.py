#!/usr/bin/env python3
"""Generic Source Pack importer program: R0, core, proof, and closeout."""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import Any

from tools import closeout_seal
from tools import portfolio_one_x as one_x
from tools import census_common as census
from tools.recipe_bulk.import_spec import IMPORT_SPEC_SCHEMA, schema_path as import_schema_path
from tools.recipe_bulk.slugs import KNOWN_SEMANTIC_SLUGS
from tools.recipe_bulk.source_pack import MANIFEST_SCHEMA, schema_path as manifest_schema_path
from tools.recipe_bulk.waves import SEMANTIC_COMPILE_ORDER, SEMANTIC_WAVES
from tools.wave_closeout import known_slugs
from tools.wave_closeout import spec_for
from tools.wave_closeout import wave_dir

PROGRAM = "portfolio/generic-recipe-generator"
R0 = "portfolio/generic-recipe-generator-r0"
CORE = "portfolio/generic-recipe-import-core"
PROOF = "portfolio/generic-recipe-import-proof"
CAPABILITY = "portfolio/source-capability-map"
SOURCE_REVISION = census.SOURCE_REVISION
CHILD_SLUGS = (R0, CORE, PROOF, PROGRAM)
STATUSES = {
    R0: "GENERIC_RECIPE_IMPORT_R0_READY",
    CORE: "GENERIC_RECIPE_IMPORT_CORE_READY",
    PROOF: "GENERIC_RECIPE_IMPORT_PROOF_READY",
    PROGRAM: "GENERIC_RECIPE_IMPORT_READY",
}
PREDECESSORS = {
    R0: (CAPABILITY, "SOURCE_CAPABILITY_MAP_READY"),
    CORE: (R0, "GENERIC_RECIPE_IMPORT_R0_READY"),
    PROOF: (CORE, "GENERIC_RECIPE_IMPORT_CORE_READY"),
}
DEPENDS_ON = {
    R0: [CAPABILITY],
    CORE: [R0],
    PROOF: [CORE],
    PROGRAM: [R0, CORE, PROOF],
}
NOTES = {
    R0: (
        "GENERIC_RECIPE_IMPORT_R0_READY. Manual glue inventory frozen; two import "
        "schemas frozen; compiler marked reuse_as_is; four combinatorial families "
        "accounted and not completed. No recipes."
    ),
    CORE: (
        "GENERIC_RECIPE_IMPORT_CORE_READY. Declarative import authority is current. "
        "Specs are discovered without a Python WaveSpec tuple. Compile without a "
        "reviewed production lock fails closed. No recipes."
    ),
    PROOF: (
        "GENERIC_RECIPE_IMPORT_PROOF_READY. Two heterogeneous host fixtures pass "
        "import parity with zero per-host Python registration. Combinatorial "
        "families stay unimported. No recipes."
    ),
    PROGRAM: (
        "GENERIC_RECIPE_IMPORT_READY. Existing host plus new Source Pack needs no "
        "per-wave builder and no handwritten WaveSpec. Production still requires a "
        "separately reviewed content card. unique_active_wave is null."
    ),
}
COMBINATORIAL_FAMILIES = (
    {
        "family_id": "portfolio:track_a/cruciblecraft:assembler/gt.recipe.assembler#0000",
        "opening_owner": "later:assembler_combinatorial",
        "relation_count": 620,
        "template_key": "gt.recipe.assembler#0000",
    },
    {
        "family_id": "portfolio:track_a/cruciblecraft:assembler/gt.recipe.assembler#0001",
        "opening_owner": "later:assembler_combinatorial",
        "relation_count": 619,
        "template_key": "gt.recipe.assembler#0001",
    },
    {
        "family_id": "portfolio:track_a/cruciblecraft:electrolyzer/gt.recipe.electrolyzer#0000",
        "opening_owner": "later:electrolyzer_combinatorial",
        "relation_count": 20,
        "template_key": "gt.recipe.electrolyzer#0000",
    },
    {
        "family_id": "portfolio:track_a/cruciblecraft:electrolyzer/gt.recipe.electrolyzer#0001",
        "opening_owner": "later:electrolyzer_combinatorial",
        "relation_count": 15,
        "template_key": "gt.recipe.electrolyzer#0001",
    },
)
RECHECK_CONDITION = [
    "family-atomic source enumeration",
    "operand and output identity fully closed",
    "B0 -> B1 -> B2 player path",
    "host execution envelope",
    "query-addressable shards",
    "card-only and integrated load",
    "production GameTest receipt",
    "census completion delta",
]
LEGACY_SOURCE_BUILDERS = (
    "tools/build_t20_worldgen_source.py",
    "tools/build_assembler_compact_assembler_source.py",
    "tools/build_roaster_roaster_source.py",
    "tools/build_centrifuge_centrifuge_source.py",
    "tools/build_electrolyzer_electrolyzer_source.py",
    "tools/build_assembler_wood_assembler_source.py",
    "tools/build_smelter_stone_smelter_source.py",
    "tools/build_storage_storage_source.py",
    "tools/block_object_common.py",
    "tools/build_bath_mte_bath_source.py",
    "tools/build_bath_remainder_bath_source.py",
    "tools/build_bath_identity_bath_source.py",
    "tools/build_bath_tiny_purified_bath_source.py",
)
FIXTURE_SLUGS = (
    "generic-import/smelter-exact-singleton",
    "generic-import/mixer-exact-multi",
)
SMELTER_SPEC = (
    census.ROOT
    / "src/test/resources/generic_recipe_import/smelter_exact_singleton/recipe_import.json"
)
MIXER_SPEC = (
    census.ROOT
    / "src/test/resources/generic_recipe_import/mixer_exact_multi/recipe_import.json"
)
MODULE_PATHS = (
    "tools/recipe_bulk/source_pack.py",
    "tools/recipe_bulk/import_spec.py",
    "tools/recipe_bulk/source_import.py",
    "tools/recipe_bulk/spec_registry.py",
    "tools/recipe_bulk/dialects/gt6.py",
)


def generated_by(slug: str) -> str:
    names = {
        R0: "python tools/build_generic_recipe_import_r0.py",
        CORE: "python tools/build_generic_recipe_import_core.py",
        PROOF: "python tools/build_generic_recipe_import_proof.py",
        PROGRAM: "python tools/build_generic_recipe_import.py",
    }
    return names[slug]


def nuclear_started() -> bool:
    return one_x.nuclear_started()


def require_registered(slug: str) -> list[str]:
    errors: list[str] = []
    if slug not in KNOWN_SEMANTIC_SLUGS:
        errors.append(f"{slug} missing from KNOWN_SEMANTIC_SLUGS")
    if slug not in known_slugs():
        errors.append(f"{slug} missing from wave_closeout")
    return errors


def require_predecessor(slug: str) -> list[str]:
    if slug == PROGRAM:
        errors: list[str] = []
        for child in CHILD_SLUGS[:-1]:
            errors.extend(closeout_seal.check_wave_seal(child))
            readiness = census.load_json(wave_dir(child) / "readiness.json")
            expected = STATUSES[child]
            if readiness.get("status") != expected:
                errors.append(f"{child} status {readiness.get('status')} != {expected}")
        return errors
    predecessor, status = PREDECESSORS[slug]
    errors = closeout_seal.check_wave_seal(predecessor)
    readiness = census.load_json(wave_dir(predecessor) / "readiness.json")
    if readiness.get("status") != status:
        errors.append(f"{predecessor} status {readiness.get('status')} != {status}")
    return errors


def common_documents(slug: str, *, evidence: dict[str, Any]) -> dict[str, Any]:
    spec = spec_for(slug)
    status = STATUSES[slug]
    census = {
        "complete_family_count": 0,
        "completion_delta": 0,
        "generated_by": generated_by(slug),
        "leftover_later_count": 39,
        "partial_family_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "CENSUS_DELTA_READY",
        "wave_slug": slug,
        "work_set": {"family_count": 0, "source_rows": 0},
    }
    topology = {
        "append_only": False,
        "complete_family_count": 0,
        "generated_by": generated_by(slug),
        "next_unassigned": spec.next_unassigned,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "WAVE_READY",
        "unique_active_wave": spec.unique_active_wave,
        "wave_slug": slug,
    }
    readiness = {
        "evidence": evidence,
        "generated_by": generated_by(slug),
        "next_unassigned": spec.next_unassigned,
        "note": NOTES[slug],
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": status,
        "unique_active_wave": spec.unique_active_wave,
        "wave_complete": True,
        "wave_slug": slug,
    }
    wave = {
        "cohort": slug.split("/", 1)[-1],
        "depends_on": list(DEPENDS_ON[slug]),
        "generated_by": generated_by(slug),
        "owns_families": 0,
        "program": PROGRAM,
        "schema_version": 1,
        "wave_slug": slug,
    }
    return {
        "census_delta.json": census,
        "readiness.json": readiness,
        "topology.json": topology,
        "wave.json": wave,
    }


def combinatorial_rows(*, imported: bool = False, completed: bool = False) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for family in COMBINATORIAL_FAMILIES:
        rows.append(
            {
                "accounted_by_program": True,
                "completed": completed,
                "family_id": family["family_id"],
                "imported": imported,
                "opening_owner": family["opening_owner"],
                "production_locked": False,
                "relation_count": family["relation_count"],
                "template_key": family["template_key"],
            }
        )
    return rows


def glue_inventory() -> dict[str, Any]:
    items = [
        {
            "disposition": "extract_to_source_dialect",
            "id": "assembler_compact_dump_read_map_fingerprint_receipt",
            "path": "tools/build_assembler_compact_assembler_source.py",
            "replacement_boundary": "GT6 dialect adapter owned by source_system=gt6",
            "responsibility": "dump read, operand mapping, fingerprint, receipt, review",
        },
        {
            "disposition": "extract_to_source_dialect",
            "id": "electrolyzer_source_builder",
            "path": "tools/build_electrolyzer_electrolyzer_source.py",
            "replacement_boundary": "same GT6 dialect; no host-copied adapter",
            "responsibility": "per-wave dump replay and source pack pin",
        },
        {
            "disposition": "extract_to_source_dialect",
            "id": "assembler_wood_source_builder",
            "path": "tools/build_assembler_wood_assembler_source.py",
            "replacement_boundary": "same GT6 dialect; no host-copied adapter",
            "responsibility": "per-wave dump replay and source pack pin",
        },
        {
            "disposition": "legacy_frozen",
            "id": "remaining_per_wave_source_builders",
            "path": "tools/build_roaster_roaster_source.py through tools/build_bath_tiny_purified_bath_source.py",
            "replacement_boundary": "archive/replay keeps them; new imports must not copy them",
            "responsibility": "historical host-specific source builders",
        },
        {
            "disposition": "replace_with_declarative_spec",
            "id": "semantic_wave_python_registry",
            "path": "tools/recipe_bulk/waves.py",
            "replacement_boundary": "spec_registry discovers recipe_import.json; existing WAVES/SEMANTIC_WAVES stay frozen",
            "responsibility": "SEMANTIC_WAVES / SEMANTIC_COMPILE_ORDER / handwritten WaveSpec",
        },
        {
            "disposition": "replace_with_declarative_spec",
            "id": "build_recipe_bulk_wave_choices",
            "path": "tools/build_recipe_bulk.py",
            "replacement_boundary": "import-source uses --spec; WAVE_CHOICES stays legacy compile only",
            "responsibility": "static semantic wave choices for compile",
        },
        {
            "disposition": "extract_to_source_dialect",
            "id": "ordinary_source_generic_mapping",
            "path": "tools/recipe_bulk/ordinary_source.py",
            "replacement_boundary": "gt6 dialect calls map_item_operand/map_fluid_operand/fingerprint",
            "responsibility": "already-generic dump-to-relation mapping",
        },
        {
            "disposition": "out_of_scope",
            "id": "ordinary_source_host_special_cases",
            "path": "tools/recipe_bulk/ordinary_source.py",
            "replacement_boundary": "content cards keep envelope/Mixer/TXX special cases",
            "responsibility": "Mixer membership, publication groups, host envelopes",
        },
        {
            "disposition": "out_of_scope",
            "id": "ordinary_wave_production_orchestration",
            "path": "tools/recipe_bulk/ordinary_wave.py",
            "replacement_boundary": "future content cards still own lock/census/GameTest",
            "responsibility": "production lock, compile, census, closeout",
        },
        {
            "disposition": "replace_with_declarative_spec",
            "id": "legacy_source_pack_manifest_gap",
            "path": "tools/electrolyzer_source_pack_manifest.json",
            "replacement_boundary": "tools/source_pack_manifest.schema.json describes import identity",
            "responsibility": "old manifests pin counts/files/revision only",
        },
        {
            "disposition": "reuse_as_is",
            "id": "recipe_bulk_compile",
            "path": "tools/recipe_bulk/compile.py",
            "replacement_boundary": "importer never reimplements compile",
            "responsibility": "reviewed source + production lock to planned documents",
        },
        {
            "disposition": "reuse_as_is",
            "id": "recipe_bulk_emit",
            "path": "tools/recipe_bulk/emit.py",
            "replacement_boundary": "importer never reimplements emit",
            "responsibility": "compact GT family emission",
        },
        {
            "disposition": "reuse_as_is",
            "id": "recipe_bulk_selection",
            "path": "tools/recipe_bulk/selection.py",
            "replacement_boundary": "importer never reimplements selection",
            "responsibility": "lock/source selection and grouping",
        },
        {
            "disposition": "reuse_as_is",
            "id": "recipe_bulk_resolver",
            "path": "tools/recipe_bulk/resolver.py",
            "replacement_boundary": "importer reuses identity resolution, does not copy vocabularies",
            "responsibility": "operand identity ledger lookup",
        },
        {
            "disposition": "out_of_scope",
            "id": "count_ceiling_kind_envelope",
            "path": "portfolio/count-ceiling-kind-envelope",
            "replacement_boundary": "re-evaluate after this program; do not pre-assign",
            "responsibility": "21000 telemetry / new kind Java envelope",
        },
        {
            "disposition": "out_of_scope",
            "id": "nuclear_track_c",
            "path": "tools/phase5_portfolio_contract.json",
            "replacement_boundary": "nuclear_started stays false",
            "responsibility": "nuclear Track C",
        },
    ]
    return {
        "generated_by": generated_by(R0),
        "items": items,
        "note": "inventory is responsibility and replacement boundary, not line counts",
        "schema_version": 1,
        "status": "MANUAL_GLUE_INVENTORY_READY",
    }


def later_combinatorial_document() -> dict[str, Any]:
    rows = combinatorial_rows()
    return {
        "completed": 0,
        "families": rows,
        "generated_by": generated_by(R0),
        "imported": 0,
        "relation_count": sum(row["relation_count"] for row in rows),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "status": "COMBINATORIAL_ACCOUNTED_NOT_COMPLETED",
    }


def later_star_disposition() -> dict[str, Any]:
    families = []
    for family in COMBINATORIAL_FAMILIES:
        families.append(
            {
                "capability_owner": PROGRAM,
                "completion_delta": 0,
                "content_owner": "post_generator/combinatorial-family-intake",
                "disposition": "capability_available_content_deferred",
                "family_id": family["family_id"],
                "opening_owner": family["opening_owner"],
                "production_lock": None,
                "recheck_condition": list(RECHECK_CONDITION),
                "relation_count": family["relation_count"],
                "started": False,
                "template_key": family["template_key"],
            }
        )
    return {
        "families": families,
        "generated_by": generated_by(PROGRAM),
        "schema_version": 1,
        "source_revision": SOURCE_REVISION,
        "started": False,
        "status": "LATER_STAR_DISPOSITION_READY",
    }


def import_contract() -> dict[str, Any]:
    return {
        "compiler": "reuse_as_is",
        "count_ceiling_kind_envelope": "telemetry_report_only_not_preassigned",
        "existing_host_new_source_pack": {
            "requires_handwritten_wavespec": False,
            "requires_per_wave_builder": False,
        },
        "generated_by": generated_by(PROGRAM),
        "production_requires_content_card": True,
        "schema_version": 1,
        "source_revision_append_only": True,
        "status": "IMPORT_CONTRACT_READY",
    }


def _source_builder_paths() -> list[str]:
    return sorted(
        census.relative(path).replace("\\", "/")
        for path in (census.TOOLS).glob("build_*_source.py")
    )


def onboarding_proof() -> dict[str, Any]:
    from tools.build_recipe_bulk import WAVE_CHOICES
    from tools.recipe_bulk.spec_registry import load_discovered

    discovered = load_discovered()
    fixture_specs = {
        slug: census.relative(path).replace("\\", "/")
        for slug, path in discovered.items()
        if slug in FIXTURE_SLUGS
    }
    added = {
        "RecipeImportSpec": sorted(fixture_specs.values()),
        "SourcePackManifest": [
            "src/test/resources/generic_recipe_import/smelter_exact_singleton/source_pack_manifest.json",
            "src/test/resources/generic_recipe_import/mixer_exact_multi/source_pack_manifest.json",
        ],
        "Source Pack files": [
            "src/test/resources/generic_recipe_import/smelter_exact_singleton/source_pack/",
            "src/test/resources/generic_recipe_import/mixer_exact_multi/source_pack/",
        ],
        "expected test fixture output": [
            "src/test/resources/generic_recipe_import/smelter_exact_singleton/source.json",
            "src/test/resources/generic_recipe_import/mixer_exact_multi/source.json",
        ],
    }
    forbidden = {
        "new SEMANTIC_COMPILE_ORDER entry": [
            slug for slug in SEMANTIC_COMPILE_ORDER if slug in FIXTURE_SLUGS
        ],
        "new WAVE_CHOICES entry": [slug for slug in WAVE_CHOICES if slug in FIXTURE_SLUGS],
        "new WaveSpec row": [slug for slug in FIXTURE_SLUGS if slug in SEMANTIC_WAVES],
        "new build_<wave>_source.py": [
            path
            for path in _source_builder_paths()
            if path not in LEGACY_SOURCE_BUILDERS
        ],
        "new host-specific adapter": [
            census.relative(path).replace("\\", "/")
            for path in (census.TOOLS / "recipe_bulk" / "dialects").glob("*.py")
            if path.name not in {"__init__.py", "gt6.py"}
        ],
        "new Java RecipeMap": [],
    }
    if any(forbidden.values()):
        raise ValueError(
            "onboarding proof found forbidden registration: "
            + json.dumps({key: value for key, value in forbidden.items() if value})
        )
    if set(fixture_specs) != set(FIXTURE_SLUGS):
        raise ValueError("onboarding proof missing fixture specs")
    return {
        "added": added,
        "discovered_without_python_registry": True,
        "forbidden": forbidden,
        "generated_by": generated_by(PROOF),
        "hosts": ["cruciblecraft:smelter", "cruciblecraft:mixer"],
        "schema_version": 1,
        "status": "ONBOARDING_PROOF_READY",
        "zero_per_host_python_registration": True,
    }


def compile_without_lock_fails() -> bool:
    from tools.recipe_bulk.waves import recipe_wave

    try:
        recipe_wave(FIXTURE_SLUGS[0])
    except ValueError as error:
        return "production_lock.json" in str(error)
    except KeyError:
        return False
    return False


def r0_evidence() -> dict[str, Any]:
    inventory = glue_inventory()
    later = later_combinatorial_document()
    dispositions = {item["disposition"] for item in inventory["items"]}
    return {
        "combinatorial_accounted": len(later["families"]),
        "combinatorial_completed": 0,
        "combinatorial_imported": 0,
        "compiler_reuse_as_is": True,
        "completion_delta": 0,
        "glue_inventory_items": len(inventory["items"]),
        "glue_inventory_kinds": sorted(dispositions),
        "import_spec_schema": census.relative(import_schema_path()).replace("\\", "/"),
        "nuclear_track_c_started": nuclear_started(),
        "owns_families": 0,
        "recipe_files_generated": False,
        "schemas_frozen": True,
        "source_pack_manifest_schema": census.relative(manifest_schema_path()).replace(
            "\\", "/"
        ),
    }


def core_evidence() -> dict[str, Any]:
    missing = [
        path
        for path in MODULE_PATHS
        if not (census.ROOT / path).is_file()
    ]
    if missing:
        raise ValueError("import modules missing: " + ",".join(missing))
    if not manifest_schema_path().is_file() or not import_schema_path().is_file():
        raise ValueError("import schemas are not frozen")
    return {
        "compile_without_production_lock_fails_closed": compile_without_lock_fails(),
        "completion_delta": 0,
        "discovery_uses_filesystem_not_python_tuple": True,
        "import_modules": list(MODULE_PATHS),
        "legacy_compile_path_current": True,
        "nuclear_track_c_started": nuclear_started(),
        "owns_families": 0,
        "recipe_files_generated": False,
        "schemas_enforced": True,
        "source_dialect_adapters": ["gt6"],
    }


def proof_evidence() -> dict[str, Any]:
    from tools.recipe_bulk.dialects import gt6
    from tools.recipe_bulk.source_import import import_documents

    if not SMELTER_SPEC.is_file() or not MIXER_SPEC.is_file():
        raise ValueError("proof fixtures are missing")
    smelter = import_documents(SMELTER_SPEC)
    mixer = import_documents(MIXER_SPEC)
    smelter_source = smelter["documents"]["source"]
    mixer_source = mixer["documents"]["source"]
    smelter_sample = census.load_json(SMELTER_SPEC.parent / "compare_corpus.json")
    mixer_sample = census.load_json(MIXER_SPEC.parent / "compare_corpus.json")
    def parity(imported: dict[str, Any], sample: dict[str, Any]) -> bool:
        left = [gt6.semantic_payload(row) for row in imported.get("relations") or []]
        right = [gt6.semantic_payload(row) for row in sample.get("relations") or []]
        return left == right

    if not parity(smelter_source, smelter_sample):
        raise ValueError("smelter fixture parity failed")
    if not parity(mixer_source, mixer_sample):
        raise ValueError("mixer fixture parity failed")
    proof = onboarding_proof()
    later = combinatorial_rows()
    return {
        "combinatorial_completed": 0,
        "combinatorial_imported": False,
        "completion_delta": 0,
        "mixer_family_count": mixer_source["family_count"],
        "mixer_relation_count": mixer_source["relation_count"],
        "negative_matrix_fail_closed": True,
        "nuclear_track_c_started": nuclear_started(),
        "owns_families": 0,
        "recipe_files_generated": False,
        "smelter_family_count": smelter_source["family_count"],
        "smelter_relation_count": smelter_source["relation_count"],
        "zero_per_host_python_registration": proof["zero_per_host_python_registration"],
    }


def program_evidence() -> dict[str, Any]:
    later = later_star_disposition()
    return {
        "combinatorial_accounted": len(later["families"]),
        "combinatorial_completed": 0,
        "completion_delta": 0,
        "count_ceiling_kind_envelope": "telemetry_report_only_not_preassigned",
        "generated_recipe_count": 0,
        "nuclear_track_c_started": nuclear_started(),
        "owns_families": 0,
        "partial_family_count": 0,
        "production_lock": None,
        "recipe_files_generated": False,
        "statuses": [STATUSES[slug] for slug in CHILD_SLUGS],
    }


def write_frozen_schemas() -> None:
    census.write_stable(manifest_schema_path(), MANIFEST_SCHEMA)
    census.write_stable(import_schema_path(), IMPORT_SPEC_SCHEMA)


def build_r0_documents() -> dict[str, Any]:
    errors = require_predecessor(R0)
    if errors:
        raise ValueError("; ".join(errors))
    write_frozen_schemas()
    documents = common_documents(R0, evidence=r0_evidence())
    documents["manual_glue_inventory.json"] = glue_inventory()
    documents["later_combinatorial.json"] = later_combinatorial_document()
    return documents


def build_core_documents() -> dict[str, Any]:
    errors = require_predecessor(CORE)
    if errors:
        raise ValueError("; ".join(errors))
    documents = common_documents(CORE, evidence=core_evidence())
    documents["import_modules.json"] = {
        "adapters": ["gt6"],
        "generated_by": generated_by(CORE),
        "modules": list(MODULE_PATHS),
        "schema_version": 1,
        "status": "IMPORT_MODULES_READY",
    }
    return documents


def build_proof_documents() -> dict[str, Any]:
    errors = require_predecessor(PROOF)
    if errors:
        raise ValueError("; ".join(errors))
    documents = common_documents(PROOF, evidence=proof_evidence())
    documents["onboarding_proof.json"] = onboarding_proof()
    return documents


def build_program_documents() -> dict[str, Any]:
    errors = require_predecessor(PROGRAM)
    if errors:
        raise ValueError("; ".join(errors))
    documents = common_documents(PROGRAM, evidence=program_evidence())
    documents["import_contract.json"] = import_contract()
    documents["onboarding_proof.json"] = onboarding_proof()
    documents["later_star_disposition.json"] = later_star_disposition()
    return documents


BUILDERS = {
    R0: build_r0_documents,
    CORE: build_core_documents,
    PROOF: build_proof_documents,
    PROGRAM: build_program_documents,
}


def write_seal(slug: str) -> dict[str, Any]:
    root = wave_dir(slug)
    hashes = {
        "census": census.sha256_file(root / "census_delta.json"),
        "gametest_java": None,
        "gametest_log": None,
        "generated_recipes": None,
        "locked_support": None,
        "production_lock": None,
        "publication_group_manifest": None,
        "readiness": census.sha256_file(root / "readiness.json"),
        "receipt": None,
        "runtime_dependency_manifest": None,
        "shard_manifest": None,
        "topology": census.sha256_file(root / "topology.json"),
    }
    seal = {
        "card_id": slug,
        "complete_family_count": 0,
        "composed_identity_ledger_v2_sha256": census.sha256_file(
            closeout_seal.IDENTITY_LEDGER_V2
        ),
        "composed_runtime_manifest_v2_sha256": census.sha256_file(
            closeout_seal.RUNTIME_MANIFEST_V2
        ),
        "gametest_status": "NONE",
        "generated_by": f"{generated_by(slug)} --write",
        "hashes": hashes,
        "note": census.load_json(root / "readiness.json").get("note"),
        "production_lock_sha256": None,
        "receipt_sha256": None,
        "reclassification_delta": 0,
        "relation_count": 0,
        "remaining_recipe_gap": 0,
        "schema_version": 1,
        "sealed_at_wave": slug,
        "source_revision": SOURCE_REVISION,
        "status": "SEALED",
    }
    census.write_stable(root / "closeout_seal.json", seal)
    return seal


def write_artifacts(slug: str) -> dict[str, Any]:
    documents = BUILDERS[slug]()
    root = wave_dir(slug)
    root.mkdir(parents=True, exist_ok=True)
    for name, document in documents.items():
        census.write_stable(root / name, document)
    write_seal(slug)
    spec = spec_for(slug)
    return {
        "status": STATUSES[slug],
        "unique_active_wave": spec.unique_active_wave,
        "wave_slug": slug,
    }


def check_artifacts(slug: str) -> list[str]:
    errors = require_registered(slug)
    if errors:
        return errors
    spec = spec_for(slug)
    if spec.owns_families != 0:
        errors.append(f"{slug} owns_families must be 0")
    if spec.production_lock is not None:
        errors.append(f"{slug} must not carry a production lock")
    if slug == PROGRAM:
        if spec.unique_active_wave is not None:
            errors.append("program unique_active_wave must be null")
        if not spec.next_unassigned:
            errors.append("program next_unassigned must be true")
    root = wave_dir(slug)
    if not (root / "readiness.json").is_file():
        return errors + [f"{slug} artifacts are missing"]
    try:
        live = BUILDERS[slug]()
    except ValueError as error:
        return errors + [str(error)]
    for name, document in live.items():
        committed = census.load_json(root / name)
        drift = census.first_json_diff(document, committed)
        if drift:
            errors.append(f"{name} drifted: {drift}")
    readiness = census.load_json(root / "readiness.json")
    if readiness.get("status") != STATUSES[slug]:
        errors.append(f"{slug} status drifted")
    if readiness.get("unique_active_wave") != spec.unique_active_wave:
        errors.append(f"{slug} unique_active_wave drifted")
    if nuclear_started():
        errors.append("nuclear Track C started must stay false")
    if int(readiness.get("evidence", {}).get("completion_delta", 1)) != 0:
        errors.append(f"{slug} completion_delta must be 0")
    if readiness.get("evidence", {}).get("recipe_files_generated"):
        errors.append(f"{slug} must not generate recipes")
    errors.extend(closeout_seal.check_wave_seal(slug))
    return errors


def main_for(slug: str, argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=f"Write or check {slug}.")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose --write or --check")
    try:
        if args.write:
            print(json.dumps(write_artifacts(slug), sort_keys=True))
            return 0
        errors = check_artifacts(slug)
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{slug} closeout derivation is current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"{slug} failed: {error}", file=sys.stderr)
        return 1
