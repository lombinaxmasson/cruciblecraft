#!/usr/bin/python3
"""Compile a reviewed generic-import fixture into an isolated tree."""
from __future__ import annotations

from dataclasses import replace
from pathlib import Path
from typing import Any

from tools import census_common as census
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk.matrix import authored_relation_count
from tools.recipe_bulk.models import WaveSpec
from tools.recipe_bulk.source_import import import_documents


def reviewed_lock(
    candidate: dict[str, Any],
    *,
    publication_group: str,
    cohort: str,
) -> dict[str, Any]:
    families: list[dict[str, Any]] = []
    relation_count = 0
    representations: dict[str, int] = {}
    for row in candidate.get("families") or []:
        if row.get("blocked") or row.get("unresolved"):
            raise ValueError(
                f"{candidate.get('import_slug')}: blocked/unresolved family "
                f"{row.get('family_id')} cannot become a production lock"
            )
        payload = dict(row)
        payload["publication_group"] = publication_group
        payload["cohort"] = cohort
        families.append(payload)
        relation_count += int(row.get("relation_count") or 0)
        representation = str(row.get("representation") or "exact")
        representations[representation] = representations.get(representation, 0) + 1
    return {
        "generated_by": "tools.recipe_bulk.pilot",
        "note": "fixture-reviewed lock for isolated compile; not a live RecipeMap import",
        "production": {
            "families": families,
            "family_count": len(families),
            "relation_count": relation_count,
            "representations": dict(sorted(representations.items())),
        },
        "production_authority": True,
        "schema_version": 1,
        "source_revision": candidate.get("source_revision"),
        "status": "PRODUCTION_LOCK",
    }


def compile_fixture(spec_path: Path, dest: Path) -> dict[str, Any]:
    built = import_documents(spec_path)
    candidate = built["documents"]["lock_candidate"]
    spec_doc = built["spec"]
    slug = str(spec_doc["import_slug"])
    host = str(spec_doc["host"]).split(":", 1)[-1]
    cohort = slug.split("/", 1)[-1].replace("-", "_")
    publication_group = f"{spec_doc['target_map']}/pilot/{cohort}"
    lock = reviewed_lock(
        candidate,
        publication_group=publication_group,
        cohort=cohort,
    )
    dest.mkdir(parents=True, exist_ok=True)
    source_path = dest / "source.json"
    lock_path = dest / "production_lock.json"
    generated_root = dest / "recipe_generated"
    census.write_stable(source_path, built["documents"]["source"])
    census.write_stable(lock_path, lock)
    wave = WaveSpec(
        wave_id=slug,
        archetype="lock_relation_set",
        template_kind="exact_relation_set",
        host=str(spec_doc["host"]),
        target_map=str(spec_doc["target_map"]),
        source_path=source_path,
        generated_root=generated_root,
        equivalence_path=dest / "equivalence.json",
        selection_policy="lock_templates",
        publication_policy="lock",
        path_layout="cohort_nested",
        compile_authority="recipe_bulk",
        relation_sort="source_recipe_index_then_stable_id",
        stable_id_policy="lock",
        target_map_policy="spec",
        source_kind_policy="relation_provenance",
        lock_path=lock_path,
        wave_slug=slug,
        cohort=cohort,
        representation="exact_or_exact_multi",
        path_prefix=f"pilot/{host}/{cohort}",
        dry_run_without_lock=False,
    )
    compiled = compile_mod.compile_wave(slug, spec=wave)
    compile_mod.write_tree(
        compiled["planned"],
        generated_root,
        generated_root,
        path_prefix=wave.path_prefix,
    )
    representations: dict[str, int] = {}
    authored_form: dict[str, int] = {}
    parameterized = 0
    for _path, document in compiled["planned"]:
        if document.get("parameterized"):
            parameterized += 1
        form = str(document.get("authored_form") or "relations")
        authored_form[form] = authored_form.get(form, 0) + 1
    for family in lock["production"]["families"]:
        representation = str(family["representation"])
        representations[representation] = representations.get(representation, 0) + 1
    if parameterized:
        raise ValueError(f"{slug}: parameterized families are forbidden without runtime proof")
    return {
        "authored_form": dict(sorted(authored_form.items())),
        "family_count": len(compiled["planned"]),
        "generated_root": census.relative(generated_root),
        "import_slug": slug,
        "overflow": 0,
        "parameterized": parameterized,
        "publication_groups": sorted(
            {
                str(document.get("publication_group") or "")
                for _path, document in compiled["planned"]
            }
        ),
        "relation_count": sum(
            authored_relation_count(document) for _path, document in compiled["planned"]
        ),
        "representations": dict(sorted(representations.items())),
        "shards": len(
            {
                str(document.get("publication_group") or "")
                for _path, document in compiled["planned"]
            }
        ),
        "source_rows": int(built["documents"]["source"]["relation_count"]),
        "wave_id": slug,
    }
