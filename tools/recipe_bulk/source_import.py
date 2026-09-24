#!/usr/bin/python3
"""Declarative Source Pack → canonical source / lock_candidate importer."""
from __future__ import annotations

from collections import defaultdict
from typing import Any

from tools import census_common as census
from tools.recipe_bulk.dialects import gt6
from tools.recipe_bulk.import_spec import (
    ImportSpecError,
    dialect_name,
    load_import_spec,
)
from tools.recipe_bulk.source_pack import (
    SourcePackError,
    load_manifest,
    replay_tier,
    sha256_file,
    verify_files,
)

AUTHORITY_PATHS = {
    "identity_ledger_v3": census.TOOLS / "global_build_identity_ledger.v3.json",
    "material_form_authority": census.TOOLS / "material_form_authority.json",
}
FORBIDDEN_OUTPUT_NAMES = frozenset(
    {
        "production_lock.json",
        "publication_group_manifest.json",
        "shard_manifest.json",
    }
)


class SourceImportError(ValueError):
    """Generic source import failed closed."""


def _raise(error: Exception) -> None:
    raise SourceImportError(str(error)) from error


def load_work_set(path: Path) -> dict[str, Any]:
    document = census.load_json(path)
    families = document.get("families") or []
    if not families:
        raise SourceImportError(f"{census.relative(path)} has no families")
    seen_ids: set[str] = set()
    for family in families:
        family_id = str(family.get("family_id") or "")
        if not family_id:
            raise SourceImportError("work-set family_id is required")
        if family_id in seen_ids:
            raise SourceImportError(f"duplicate family_id {family_id}")
        seen_ids.add(family_id)
        relations = family.get("relations") or []
        if not relations:
            raise SourceImportError(f"{family_id} work-set relations are empty")
        orders = [int(row.get("shadow_order") or 0) for row in relations]
        if orders != list(range(len(orders))):
            raise SourceImportError(f"{family_id} shadow_order must be 0..n-1")
    return document


def load_dump_slice(path: Path) -> dict[str, Any]:
    document = census.load_json(path)
    recipes = document.get("recipes")
    if not isinstance(recipes, list) or not recipes:
        raise SourceImportError(f"{census.relative(path)} dump slice has no recipes")
    return document


def _existing_receipt(spec: dict[str, Any]) -> dict[str, Any] | None:
    raw = (spec.get("output_paths") or {}).get("receipt")
    if not raw:
        return None
    path = census.ROOT / str(raw)
    if not path.is_file():
        return None
    document = census.load_json(path)
    return document if isinstance(document, dict) else None


def authority_scope_for(
    spec: dict[str, Any],
    existing: dict[str, Any] | None = None,
) -> str:
    """Legacy receipts stay whole-file. Section scope is explicit."""
    from tools import material_form_authority as form_authority

    if existing is not None:
        stored = existing.get("authority_scope")
        return str(stored or form_authority.LEGACY_WHOLE_FILE)
    declared = spec.get("authority_scope")
    if declared:
        return str(declared)
    return form_authority.LEGACY_WHOLE_FILE


def _section_binding(spec: dict[str, Any]) -> dict[str, str]:
    from tools import material_form_authority as form_authority

    wanted = [str(item) for item in spec.get("authority_sections") or []]
    if not wanted:
        raise SourceImportError(
            "section authority_scope requires authority_sections"
        )
    live = form_authority.section_index()
    binding: dict[str, str] = {}
    for section_id in wanted:
        record = live.get(section_id)
        if record is None:
            raise SourceImportError(f"unknown authority section {section_id}")
        binding[section_id] = str(record["slice_sha256"])
    return binding


def _authority_hashes(
    spec: dict[str, Any],
    existing: dict[str, Any] | None = None,
) -> dict[str, Any]:
    from tools import material_form_authority as form_authority

    scope = authority_scope_for(spec, existing)
    hashes: dict[str, Any] = {}
    stored = (existing or {}).get("authority_sha256") or {}
    for row in spec.get("operand_authorities") or []:
        if row.get("kind") != "data":
            continue
        authority_id = str(row.get("id") or "")
        path = AUTHORITY_PATHS.get(authority_id)
        if path is None or not path.is_file():
            raise SourceImportError(f"missing authority {authority_id}")
        if (
            authority_id == "material_form_authority"
            and scope == form_authority.SECTION_SCOPE
        ):
            hashes[authority_id] = _section_binding(spec)
            continue
        if (
            authority_id == "material_form_authority"
            and scope == form_authority.LEGACY_WHOLE_FILE
            and isinstance(stored.get(authority_id), str)
        ):
            hashes[authority_id] = stored[authority_id]
            continue
        hashes[authority_id] = sha256_file(path)
    return hashes


def _output_paths(spec: dict[str, Any]) -> dict[str, Path]:
    paths = {
        key: census.ROOT / str(raw)
        for key, raw in spec["output_paths"].items()
    }
    for key, path in paths.items():
        if path.name in FORBIDDEN_OUTPUT_NAMES:
            raise SourceImportError(f"refusing to write {path.name}")
        if "recipe_generated" in path.as_posix():
            raise SourceImportError("import must not write src/recipe_generated")
        if key == "lock_candidate" and path.name != "lock_candidate.json":
            raise SourceImportError("lock_candidate path is required")
    return paths


def _index_dump(dump: dict[str, Any]) -> dict[int, dict[str, Any]]:
    indexed: dict[int, dict[str, Any]] = {}
    for recipe in dump.get("recipes") or []:
        if "source_recipe_index" in recipe:
            indexed[int(recipe["source_recipe_index"])] = recipe
    if indexed:
        return indexed
    return {index: recipe for index, recipe in enumerate(dump.get("recipes") or [])}


def _recipe_body(recipe: dict[str, Any]) -> dict[str, Any]:
    return {
        key: value
        for key, value in recipe.items()
        if key not in {"source_recipe_index", "source_row_sha256", "template_key"}
    }


def _relation_unmapped(relation: dict[str, Any]) -> bool:
    operands = (
        list(relation.get("item_inputs") or [])
        + list(relation.get("item_outputs") or [])
        + list(relation.get("fluid_inputs") or [])
        + list(relation.get("fluid_outputs") or [])
    )
    return any(
        not operand.get("runtime_id") or operand.get("mapping") == "blocked_unmapped"
        for operand in operands
    )


def _load_compare_corpus(spec: dict[str, Any]) -> list[dict[str, Any]]:
    raw = spec.get("compare_corpus")
    if not raw:
        return []
    path = census.ROOT / str(raw)
    document = census.load_json(path)
    if isinstance(document, dict) and isinstance(document.get("relations"), list):
        return list(document["relations"])
    if isinstance(document, list):
        return list(document)
    raise SourceImportError("compare_corpus must contain relations")


def _classify_family(
    family: dict[str, Any],
    relations: list[dict[str, Any]],
    corpus: list[dict[str, Any]],
    blocked: list[str],
) -> str:
    if blocked:
        return "blocked"
    family_id = str(family["family_id"])
    template_key = str(family["template_key"])
    imported = {
        str(row["source_row_sha256"]): gt6.semantic_payload(row) for row in relations
    }
    existing = [
        row
        for row in corpus
        if str(row.get("family_id") or "") == family_id
        or str(row.get("template_key") or "") == template_key
    ]
    if not existing:
        return "draft"
    existing_by_hash = {
        str(row.get("source_row_sha256") or ""): gt6.semantic_payload(row)
        for row in existing
    }
    extra = sorted(set(imported) - set(existing_by_hash))
    missing = sorted(set(existing_by_hash) - set(imported))
    if extra and not missing:
        return "extension_candidate"
    for digest, payload in imported.items():
        prior = existing_by_hash.get(digest)
        if prior is None:
            continue
        if prior != payload:
            return "overlay_review_required"
    if extra or missing:
        return "overlay_review_required"
    return "exact_duplicate_candidate"


def _representation(count: int) -> str:
    return "exact" if count == 1 else "exact_multi"


def import_documents(spec_path: Path) -> dict[str, Any]:
    try:
        spec = load_import_spec(spec_path)
    except (ImportSpecError, SourcePackError) as error:
        _raise(error)
    try:
        manifest = load_manifest(census.ROOT / str(spec["source_pack"]))
    except SourcePackError as error:
        _raise(error)
    if dialect_name(spec) != manifest.get("source_dialect"):
        raise SourceImportError("import spec dialect must match source pack dialect")
    if dialect_name(spec) != "gt6":
        raise SourceImportError(f"unknown adapter {dialect_name(spec)}")
    outputs = _output_paths(spec)
    first_generation = not outputs["source"].is_file()
    try:
        resolved = verify_files(
            manifest,
            require_present=False,
        )
        tier = replay_tier(manifest, resolved, first_generation=first_generation)
        if tier == "full_replay":
            verify_files(manifest, require_present=True)
    except SourcePackError as error:
        _raise(error)
    dump_rel = next(
        (
            str(entry["path"])
            for entry in manifest["files"]
            if entry.get("role") == "dump_slice"
        ),
        "",
    )
    work_rel = str(spec["family_membership_source"]["path"])
    if not dump_rel:
        raise SourceImportError("source pack missing dump_slice")
    dump = load_dump_slice(census.ROOT / dump_rel)
    work = load_work_set(census.ROOT / work_rel)
    dump_index = _index_dump(dump)
    corpus = _load_compare_corpus(spec)
    maps = gt6._maps()
    relations: list[dict[str, Any]] = []
    review_blockers: list[dict[str, Any]] = []
    families_out: list[dict[str, Any]] = []
    seen_stable: set[str] = set()
    source_map = str((spec.get("source_maps") or [dump.get("source_map")])[0])
    if dump.get("source_map") and str(dump["source_map"]) not in spec["source_maps"]:
        raise SourceImportError("dump source_map is not in spec.source_maps")
    for family in work.get("families") or []:
        family_id = str(family["family_id"])
        template_key = str(family["template_key"])
        family_relations: list[dict[str, Any]] = []
        family_blockers: list[str] = []
        for member in family.get("relations") or []:
            recipe_index = int(member["source_recipe_index"])
            recipe = dump_index.get(recipe_index)
            if recipe is None:
                family_blockers.append(f"missing dump row {recipe_index}")
                continue
            body = _recipe_body(recipe)
            relation, errors = gt6.compile_row(
                body,
                host=str(spec["host"]),
                target_map=str(spec["target_map"]),
                source_map=source_map,
                family_id=family_id,
                template_key=template_key,
                recipe_index=recipe_index,
                shadow_order=int(member.get("shadow_order") or 0),
                source_revision=str(manifest["source_revision"]),
                source_row_sha256=str(member["source_row_sha256"]),
                maps=maps,
            )
            if errors:
                family_blockers.extend(errors)
            if _relation_unmapped(relation):
                family_blockers.append(f"{template_key}: unmapped operand")
            relation["stable_id"] = gt6.stable_id_for(
                source_system=str(manifest["source_system"]),
                source_revision=str(manifest["source_revision"]),
                family_id=family_id,
                relation=relation,
            )
            if relation["stable_id"] in seen_stable:
                raise SourceImportError(
                    f"duplicate stable id {relation['stable_id']}"
                )
            seen_stable.add(relation["stable_id"])
            family_relations.append(relation)
            relations.append(relation)
        classification = _classify_family(
            family, family_relations, corpus, family_blockers
        )
        allowed = set(spec["representation_policy"]["allowed"])
        representation = _representation(len(family_relations))
        if representation not in allowed:
            family_blockers.append(f"representation {representation} is not allowed")
            classification = "blocked"
        unresolved = classification in {
            "extension_candidate",
            "overlay_review_required",
            "blocked",
        }
        families_out.append(
            {
                "blocked": classification == "blocked",
                "classification": classification,
                "family_id": family_id,
                "host": spec["host"],
                "relation_count": len(family_relations),
                "representation": representation,
                "source_row_sha256": [
                    str(row["source_row_sha256"]) for row in family_relations
                ],
                "stable_ids": [str(row["stable_id"]) for row in family_relations],
                "template_key": template_key,
                "unresolved": unresolved,
            }
        )
        if family_blockers:
            review_blockers.append(
                {"family_id": family_id, "reasons": family_blockers}
            )
    mapping_counts: dict[str, int] = defaultdict(int)
    for relation in relations:
        for operand in (
            list(relation.get("item_inputs") or [])
            + list(relation.get("item_outputs") or [])
            + list(relation.get("fluid_inputs") or [])
            + list(relation.get("fluid_outputs") or [])
        ):
            mapping_counts[str(operand.get("mapping") or "unknown")] += 1
    existing_receipt = _existing_receipt(spec)
    scope = authority_scope_for(spec, existing_receipt)
    receipt = {
        "adapter_abi": gt6.ADAPTER_ABI,
        "adapter_sha256": gt6.adapter_sha256(),
        "authority_sha256": _authority_hashes(spec, existing_receipt),
        "files": {
            str(entry["path"]): str(entry["sha256"])
            for entry in manifest.get("files") or []
        },
        "full_replay": tier == "full_replay",
        "generated_by": "python tools/build_recipe_bulk.py import-source",
        "import_slug": spec["import_slug"],
        "manifest_sha256": sha256_file(census.ROOT / str(spec["source_pack"])),
        "proof_tier": tier,
        "schema_version": 1,
        "skip_is_not_pass": True,
        "source_dialect": manifest["source_dialect"],
        "source_pack_id": manifest["source_pack_id"],
        "source_revision": manifest["source_revision"],
        "source_system": manifest["source_system"],
        "spec_sha256": sha256_file(spec_path),
        "status": "SOURCE_IMPORT_RECEIPT",
    }
    if scope != "legacy_whole_file":
        receipt["authority_scope"] = scope
    review = {
        "blockers": review_blockers,
        "family_count": len(families_out),
        "generated_by": "python tools/build_recipe_bulk.py import-source",
        "import_slug": spec["import_slug"],
        "mapping_counts": dict(sorted(mapping_counts.items())),
        "relation_count": len(relations),
        "schema_version": 1,
        "source_revision": manifest["source_revision"],
        "status": "SOURCE_IMPORT_REVIEW",
    }
    lock_candidate = {
        "families": families_out,
        "generated_by": "python tools/build_recipe_bulk.py import-source",
        "import_slug": spec["import_slug"],
        "note": "draft only; not production_lock",
        "production_authority": False,
        "schema_version": 1,
        "source_revision": manifest["source_revision"],
        "status": "LOCK_CANDIDATE_DRAFT",
        "unresolved_count": sum(1 for row in families_out if row["unresolved"]),
    }
    source = {
        "family_count": len(families_out),
        "generated_by": "python tools/build_recipe_bulk.py import-source",
        "host": spec["host"],
        "import_slug": spec["import_slug"],
        "relation_count": len(relations),
        "relations": relations,
        "source_pack_id": manifest["source_pack_id"],
        "source_revision": manifest["source_revision"],
        "target_map": spec["target_map"],
    }
    return {
        "documents": {
            "lock_candidate": lock_candidate,
            "receipt": receipt,
            "review": review,
            "source": source,
        },
        "paths": outputs,
        "proof_tier": tier,
        "spec": spec,
    }


def write_import(spec_path: Path) -> dict[str, Any]:
    built = import_documents(spec_path)
    for key, document in built["documents"].items():
        path = built["paths"][
            {
                "source": "source",
                "receipt": "receipt",
                "review": "review",
                "lock_candidate": "lock_candidate",
            }[key]
        ]
        census.write_stable(path, document)
    return {
        "import_slug": built["spec"]["import_slug"],
        "proof_tier": built["proof_tier"],
        "wrote": sorted(census.relative(path) for path in built["paths"].values()),
    }


def check_import(spec_path: Path) -> list[str]:
    built = import_documents(spec_path)
    errors: list[str] = []
    mapping = {
        "source": "source",
        "receipt": "receipt",
        "review": "review",
        "lock_candidate": "lock_candidate",
    }
    for key, document in built["documents"].items():
        path = built["paths"][mapping[key]]
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
            continue
        drift = census.first_json_diff(document, census.load_json(path))
        if drift:
            errors.append(f"{path.name} drifted: {drift}")
    return errors


def currentness_binds(
    spec_path: Path,
    *,
    mutate: dict[str, str] | None = None,
) -> bool:
    built = import_documents(spec_path)
    receipt = built["documents"]["receipt"]
    expected = {
        "adapter_sha256": gt6.adapter_sha256(),
        "manifest_sha256": sha256_file(
            census.ROOT / str(built["spec"]["source_pack"])
        ),
        "spec_sha256": sha256_file(spec_path),
        **{
            f"authority:{key}": value
            for key, value in receipt["authority_sha256"].items()
        },
    }
    if not mutate:
        return True
    for key, value in mutate.items():
        if expected.get(key) == value:
            return False
        if key.startswith("authority:"):
            auth = key.split(":", 1)[1]
            if receipt["authority_sha256"].get(auth) == value:
                return False
    return True
