#!/usr/bin/python3
"""Extract bounded Source Pack fixtures from closed canonical sources."""
from __future__ import annotations

from pathlib import Path
from typing import Any

from tools import census_common as census
from tools.recipe_bulk.dialects import gt6
from tools.recipe_bulk.source_pack import sha256_file

FIXTURE_ROOT = census.ROOT / "src" / "test" / "resources" / "generic_recipe_import"
SMELTER_SOURCE = census.TOOLS / "waves" / "smelter" / "ordinary-closure" / "source.json"
MIXER_SOURCE = census.TOOLS / "waves" / "mixer" / "ordinary-closure" / "source.json"
SMELTER_TEMPLATE = "gt.recipe.smelter#0490"
MIXER_TEMPLATE = (
    "sha256:088c098eff19f1e3e175529457c75de92b023c7bbe59c6f3b9ab9c4eebba1fe1"
)


def extract_relations(source_path: Path, template_key: str) -> list[dict[str, Any]]:
    document = census.load_json(source_path)
    relations = [
        row
        for row in document.get("relations") or []
        if str(row.get("template_key") or "") == template_key
    ]
    relations.sort(key=lambda row: int(row.get("shadow_order") or 0))
    if not relations:
        raise ValueError(f"{census.relative(source_path)} missing {template_key}")
    return relations


def fixture_pack(
    *,
    name: str,
    import_slug: str,
    source_path: Path,
    template_key: str,
    host: str,
    target_map: str,
    source_map: str,
) -> dict[str, Any]:
    relations = extract_relations(source_path, template_key)
    family_id = str(relations[0]["family_id"])
    dump_recipes = []
    work_relations = []
    for relation in relations:
        recipe = gt6.dump_row_from_relation(relation)
        recipe["source_recipe_index"] = int(relation["source_recipe_index"])
        recipe["source_row_sha256"] = str(relation["source_row_sha256"])
        recipe["template_key"] = template_key
        dump_recipes.append(recipe)
        work_relations.append(
            {
                "shadow_order": int(relation["shadow_order"]),
                "source_recipe_index": int(relation["source_recipe_index"]),
                "source_row_sha256": str(relation["source_row_sha256"]),
            }
        )
    root = FIXTURE_ROOT / name
    pack_dir = root / "source_pack"
    dump_path = pack_dir / "dump_slice.json"
    work_path = pack_dir / "work_set.json"
    corpus_path = root / "compare_corpus.json"
    provenance_path = root / "provenance.json"
    manifest_path = root / "source_pack_manifest.json"
    spec_path = root / "recipe_import.json"
    dump_document = {
        "recipes": dump_recipes,
        "source_map": source_map,
        "source_revision": census.SOURCE_REVISION,
    }
    work_document = {
        "families": [
            {
                "family_id": family_id,
                "relations": work_relations,
                "template_key": template_key,
            }
        ],
        "host": host,
        "source_revision": census.SOURCE_REVISION,
    }
    provenance = {
        "family_id": family_id,
        "original_source_path": census.relative(source_path).replace("\\", "/"),
        "relation_identities": [
            {
                "shadow_order": int(row["shadow_order"]),
                "source_row_sha256": str(row["source_row_sha256"]),
                "stable_id": str(row["stable_id"]),
            }
            for row in relations
        ],
        "template_key": template_key,
    }
    corpus = {
        "original_source_path": census.relative(source_path).replace("\\", "/"),
        "relations": relations,
        "source_revision": census.SOURCE_REVISION,
    }
    return {
        "corpus": corpus,
        "corpus_path": corpus_path,
        "dump": dump_document,
        "dump_path": dump_path,
        "family_id": family_id,
        "host": host,
        "import_slug": import_slug,
        "manifest_path": manifest_path,
        "name": name,
        "provenance": provenance,
        "provenance_path": provenance_path,
        "root": root,
        "source_map": source_map,
        "spec_path": spec_path,
        "target_map": target_map,
        "template_key": template_key,
        "work": work_document,
        "work_path": work_path,
    }


def _rel(path: Path) -> str:
    return census.relative(path).replace("\\", "/")


def write_pack(pack: dict[str, Any]) -> dict[str, Path]:
    pack["root"].mkdir(parents=True, exist_ok=True)
    pack["dump_path"].parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(pack["dump_path"], pack["dump"])
    census.write_stable(pack["work_path"], pack["work"])
    census.write_stable(pack["corpus_path"], pack["corpus"])
    census.write_stable(pack["provenance_path"], pack["provenance"])
    files = [
        {
            "path": _rel(pack["dump_path"]),
            "role": "dump_slice",
            "sha256": sha256_file(pack["dump_path"]),
        },
        {
            "path": _rel(pack["work_path"]),
            "role": "work_set",
            "sha256": sha256_file(pack["work_path"]),
        },
        {
            "path": _rel(pack["corpus_path"]),
            "role": "compare_corpus",
            "sha256": sha256_file(pack["corpus_path"]),
        },
    ]
    manifest = {
        "files": files,
        "full_replay": {
            "required_for_first_generation": True,
            "skip_is_not_pass": True,
        },
        "provenance_policy": {"append_only": True, "forbid_gt6u": True},
        "schema_version": 1,
        "source_dialect": "gt6",
        "source_pack_id": pack["import_slug"],
        "source_revision": census.SOURCE_REVISION,
        "source_system": "gt6",
    }
    census.write_stable(pack["manifest_path"], manifest)
    spec = {
        "compare_corpus": _rel(pack["corpus_path"]),
        "family_membership_source": {
            "kind": "work_set",
            "path": _rel(pack["work_path"]),
        },
        "host": pack["host"],
        "import_slug": pack["import_slug"],
        "operand_authorities": [
            {"id": "identity_ledger_v3", "kind": "data"},
            {"id": "material_form_authority", "kind": "data"},
            {"adapter": "gt6", "kind": "source_dialect"},
        ],
        "output_paths": {
            "lock_candidate": _rel(pack["root"] / "lock_candidate.json"),
            "receipt": _rel(pack["root"] / "source_receipt.json"),
            "review": _rel(pack["root"] / "source_review.json"),
            "source": _rel(pack["root"] / "source.json"),
        },
        "representation_policy": {"allowed": ["exact", "exact_multi"]},
        "schema_version": 1,
        "selection_rule": {"kind": "work_set_members"},
        "source_maps": [pack["source_map"]],
        "source_pack": _rel(pack["manifest_path"]),
        "stable_id_policy": {
            "algorithm": "source_system_revision_family_relation",
            "include_card_number": False,
        },
        "target_map": pack["target_map"],
    }
    census.write_stable(pack["spec_path"], spec)
    return {
        "manifest": pack["manifest_path"],
        "spec": pack["spec_path"],
    }


def smelter_pack() -> dict[str, Any]:
    return fixture_pack(
        name="smelter_exact_singleton",
        import_slug="generic-import/smelter-exact-singleton",
        source_path=SMELTER_SOURCE,
        template_key=SMELTER_TEMPLATE,
        host="cruciblecraft:smelter",
        target_map="cruciblecraft:smelter",
        source_map="gt.recipe.smelter",
    )


def mixer_pack() -> dict[str, Any]:
    return fixture_pack(
        name="mixer_exact_multi",
        import_slug="generic-import/mixer-exact-multi",
        source_path=MIXER_SOURCE,
        template_key=MIXER_TEMPLATE,
        host="cruciblecraft:mixer",
        target_map="cruciblecraft:mixer",
        source_map="gt.recipe.mixer",
    )


def write_all_fixtures() -> dict[str, str]:
    written: dict[str, str] = {}
    for builder in (smelter_pack, mixer_pack):
        pack = builder()
        paths = write_pack(pack)
        written[pack["import_slug"]] = _rel(paths["spec"])
    return written
