#!/usr/bin/env python3
"""Shared prep-only Source Pack + isolated compile helpers.

Prep artifacts stay under ``tools/waves/prep/<slug>/``.  They must not register
a RecipeMap or write ``src/recipe_generated``.
"""
from __future__ import annotations

import hashlib
import json
import shutil
import stat
import sys
from pathlib import Path
from typing import Any

from tools import census_common as census

ROOT = census.ROOT
TOOLS = ROOT / "tools"
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
if str(TOOLS) not in sys.path:
    sys.path.insert(1, str(TOOLS))

from tools.gt6_recipe_templates import _stable_json
from tools.recipe_bulk import source_import
from tools.recipe_bulk.dialects import gt6
from tools.recipe_bulk.pilot import compile_fixture, reviewed_lock
SOURCE_REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
GT6_ART = ROOT / "gt6_referencable_port_code" / "gregtech6_w"
GT6_BLOCK_ROOT = (
    GT6_ART
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "blocks"
    / "machines"
    / "basicmachines"
)
GT6_GUI_ROOT = (
    GT6_ART
    / "src"
    / "main"
    / "resources"
    / "assets"
    / "gregtech"
    / "textures"
    / "gui"
    / "machines"
)
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft"
LIVE_GENERATED = (
    ROOT
    / "src"
    / "recipe_generated"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "recipe"
)


def wave_dir(slug: str) -> Path:
    return ROOT / "tools" / "waves" / "prep" / slug


def dump_path(source_map: str) -> Path:
    return ROOT / "gt6_dump" / "gt6_recipe_dump" / "maps" / f"{source_map}.json"


def _write(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(path, document)


def row_hash(recipe: dict[str, Any]) -> str:
    return hashlib.sha256(_stable_json(recipe).encode("utf-8")).hexdigest()


def source_row(recipe: dict[str, Any], index: int, digest: str, template_key: str) -> dict[str, Any]:
    row = dict(recipe)
    row["source_recipe_index"] = index
    row["source_row_sha256"] = digest
    row["template_key"] = template_key
    return row


_REGISTERED_RUNTIME_IDS: set[str] | None = None


MATERIAL_GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
MATERIAL_DEFINITION_ROOTS = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "materials",
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "hydrocarbon_materials",
)
RUNTIME_ID_OVERLAY = (
    ROOT
    / "tools"
    / "waves"
    / "machines"
    / "sanding"
    / "mte_runtime_overlay.json"
)


def _registered_material_item_ids() -> set[str]:
    """Project Java's material-form registration into the prep audit.

    ``runtime_registry_gate.json`` is a static census and does not enumerate
    every ``DeferredRegister`` item created from the material registration
    gate.  The Java path registers every gated material form, using
    ``form_items`` overrides where present and the canonical material path
    otherwise.
    """
    gate = census.load_json(MATERIAL_GATE)
    form_items: dict[tuple[str, str], str] = {}
    for root in MATERIAL_DEFINITION_ROOTS:
        if not root.is_dir():
            continue
        for path in sorted(root.glob("*.json")):
            if path.name == "index.json":
                continue
            definition = census.load_json(path)
            material_id = str(definition.get("id") or path.stem)
            for form, item_id in (definition.get("form_items") or {}).items():
                form_items[(material_id, str(form))] = str(item_id)

    registered: set[str] = set()
    for material_id, forms in (gate.get("materials") or {}).items():
        material = str(material_id)
        for form in forms or []:
            prefix = str(form)
            if prefix == "ore":
                registered.add(f"cruciblecraft:{material}_ore")
                continue
            registered.add(
                form_items.get(
                    (material, prefix),
                    f"cruciblecraft:{material}/{prefix}",
                )
            )
    return registered


def registered_runtime_ids() -> set[str]:
    global _REGISTERED_RUNTIME_IDS
    if _REGISTERED_RUNTIME_IDS is None:
        gate = census.load_json(
            ROOT
            / "src"
            / "main"
            / "resources"
            / "census"
            / "runtime_registry_gate.json"
        )
        categories = gate.get("categories") or {}
        _REGISTERED_RUNTIME_IDS = set(categories.get("items") or []) | set(
            categories.get("blocks") or []
        )
        _REGISTERED_RUNTIME_IDS.update(_registered_material_item_ids())
        for overlay_path in (RUNTIME_ID_OVERLAY,):
            if not overlay_path.is_file():
                continue
            overlay = census.load_json(overlay_path)
            _REGISTERED_RUNTIME_IDS.update(
                str(row["runtime_id"])
                for row in overlay.get("mappings") or []
                if row.get("runtime_id")
            )
    return _REGISTERED_RUNTIME_IDS


def missing_runtime_item_operands(relation: dict[str, Any]) -> list[str]:
    registered = registered_runtime_ids()
    missing: list[str] = []
    for key in ("item_inputs", "item_outputs"):
        for operand in relation.get(key) or []:
            runtime = str(operand.get("runtime_id") or "")
            if not runtime:
                continue
            if runtime.startswith("minecraft:") or runtime.startswith("neoforge:"):
                continue
            if runtime not in registered:
                missing.append(f"unregistered runtime item {runtime}")
    return missing


def audit_rows(
    dump: dict[str, Any],
    *,
    host: str,
    target_map: str,
    source_map: str,
    family_id: str,
    template_key: str,
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    maps = gt6._maps()
    selected: list[dict[str, Any]] = []
    overflow: list[dict[str, Any]] = []
    for index, recipe in enumerate(dump.get("recipes") or []):
        digest = row_hash(recipe)
        inputs = recipe.get("inputs") or [{}]
        outputs = recipe.get("outputs") or [{}]
        display_in = (inputs[0] if inputs else {}).get("displayName")
        display_out = (outputs[0] if outputs else {}).get("displayName")
        try:
            relation, errors = gt6.compile_row(
                recipe,
                host=host,
                target_map=target_map,
                source_map=source_map,
                family_id=family_id,
                template_key=template_key,
                recipe_index=index,
                shadow_order=0,
                source_revision=SOURCE_REVISION,
                source_row_sha256=digest,
                maps=maps,
            )
        except Exception as error:
            overflow.append(
                {
                    "input": display_in,
                    "output": display_out,
                    "reasons": [str(error)],
                    "source_recipe_index": index,
                    "source_row_sha256": digest,
                    "status": "blocked",
                }
            )
            continue
        blockers = list(dict.fromkeys(str(error) for error in errors))
        if source_import._relation_unmapped(relation):
            blockers.append("unmapped operand")
        if host == "cruciblecraft:sanding":
            blockers.extend(missing_runtime_item_operands(relation))
        if host == "cruciblecraft:melter":
            if any(
                    int(
                        fluid.get("amount")
                        or (fluid.get("source") or {}).get("amount")
                        or 0
                    ) > 4_000
                    for fluid in relation.get("fluid_inputs") or []
            ):
                blockers.append("melter fluid input exceeds 4000 mB tank")
            if any(
                    int(
                        fluid.get("amount")
                        or (fluid.get("source") or {}).get("amount")
                        or 0
                    ) > 8_000
                    for fluid in relation.get("fluid_outputs") or []
            ):
                blockers.append("melter fluid output exceeds 8000 mB tank")
        entry = {
            "source_recipe_index": index,
            "source_row_sha256": digest,
            "input": display_in,
            "output": display_out,
        }
        if blockers:
            entry["status"] = "blocked"
            entry["reasons"] = blockers
            overflow.append(entry)
        else:
            selected.append(
                {
                    "source_recipe_index": index,
                    "source_row_sha256": digest,
                    "shadow_order": len(selected),
                }
            )
    return selected, overflow


def build_work_set(
    selected: list[dict[str, Any]],
    overflow: list[dict[str, Any]],
    source_rows: int,
    *,
    family_id: str,
    template_key: str,
    host: str,
) -> dict[str, Any]:
    return {
        "accounting": {
            "blocked_rows": len(overflow),
            "overflow_rows": len(overflow),
            "selected_rows": len(selected),
            "source_rows": source_rows,
            "selection_rule": (
                "retain only rows whose every item/fluid operand has an "
                "exact current expression"
            ),
        },
        "families": [
            {
                "family_id": family_id,
                "relations": selected,
                "template_key": template_key,
            }
        ],
        "host": host,
        "overflow": overflow,
        "source_revision": SOURCE_REVISION,
    }


def build_manifest(wave: Path, source_pack_id: str) -> dict[str, Any]:
    dump_slice = wave / "source_pack" / "dump_slice.json"
    work_set = wave / "source_pack" / "work_set.json"
    return {
        "files": [
            {
                "path": census.relative(dump_slice),
                "role": "dump_slice",
                "sha256": census.sha256_file(dump_slice),
            },
            {
                "path": census.relative(work_set),
                "role": "work_set",
                "sha256": census.sha256_file(work_set),
            },
        ],
        "full_replay": {
            "required_for_first_generation": True,
            "skip_is_not_pass": True,
        },
        "provenance_policy": {
            "append_only": True,
            "forbid_gt6u": True,
        },
        "schema_version": 1,
        "source_dialect": "gt6",
        "source_pack_id": source_pack_id,
        "source_revision": SOURCE_REVISION,
        "source_system": "gt6",
    }


def build_import_spec(
    wave: Path,
    *,
    host: str,
    import_slug: str,
    source_map: str,
    target_map: str,
) -> dict[str, Any]:
    return {
        "family_membership_source": {
            "kind": "work_set",
            "path": census.relative(wave / "source_pack" / "work_set.json"),
        },
        "host": host,
        "import_slug": import_slug,
        "operand_authorities": [
            {"id": "identity_ledger_v3", "kind": "data"},
            {"id": "material_form_authority", "kind": "data"},
            {"adapter": "gt6", "kind": "source_dialect"},
        ],
        "output_paths": {
            "lock_candidate": census.relative(wave / "lock_candidate.json"),
            "receipt": census.relative(wave / "source_receipt.json"),
            "review": census.relative(wave / "source_review.json"),
            "source": census.relative(wave / "source.json"),
        },
        "representation_policy": {"allowed": ["exact_multi"]},
        "schema_version": 1,
        "selection_rule": {"kind": "work_set_members"},
        "source_maps": [source_map],
        "source_pack": census.relative(wave / "source_pack_manifest.json"),
        "stable_id_policy": {
            "algorithm": "source_system_revision_family_relation",
            "include_card_number": False,
        },
        "target_map": target_map,
    }


def pilot_lock(wave: Path, import_slug: str, note: str) -> dict[str, Any]:
    payload = census.load_json(wave / "lock_candidate.json")
    spec_doc = census.load_json(wave / "recipe_import.json")
    cohort = import_slug.split("/", 1)[-1].replace("-", "_")
    publication_group = f"{spec_doc['target_map']}/pilot/{cohort}"
    lock = reviewed_lock(
        payload,
        publication_group=publication_group,
        cohort=cohort,
    )
    lock["note"] = note
    return lock


def freeze_lock(wave: Path, import_slug: str, note: str) -> dict[str, Any]:
    lock = pilot_lock(wave, import_slug, note)
    _write(wave / "production_lock.json", lock)
    return lock


def isolated_compile(wave: Path, live_needle: str) -> dict[str, Any]:
    import tempfile

    import_spec = wave / "recipe_import.json"
    live_before = {
        path.name
        for path in LIVE_GENERATED.rglob(f"*{live_needle}*")
        if path.is_file()
    }
    with tempfile.TemporaryDirectory(prefix=f"{live_needle}-prep-") as tmp:
        dest = Path(tmp)
        metrics = compile_fixture(import_spec, dest)
        generated = dest / "recipe_generated"
        if not generated.is_dir():
            raise ValueError("isolated compile did not write a recipe tree")
        generated_root = generated.resolve()
        live_root = LIVE_GENERATED.resolve()
        if generated_root == live_root or live_root in generated_root.parents:
            raise ValueError("isolated compile must not use src/recipe_generated")
        live_after = {
            path.name
            for path in LIVE_GENERATED.rglob(f"*{live_needle}*")
            if path.is_file()
        }
        if live_after != live_before:
            raise ValueError(f"isolated compile wrote live {live_needle} recipes")
        return metrics


def _drop_consume_collisions(
    wave: Path,
    *,
    host: str,
    family_id: str,
    template_key: str,
    import_slug: str,
) -> int:
    from tools.recipe_bulk.compile import _consume_identity
    from tools.recipe_bulk.emit import emit_resolved_relation
    from tools.recipe_bulk.resolver import resolve_relation_operands

    source = census.load_json(wave / "source.json")
    work = census.load_json(wave / "source_pack" / "work_set.json")
    overflow_doc = census.load_json(wave / "overflow.json")
    seen: dict[str, str] = {}
    keep_hashes: set[str] = set()
    dropped: list[dict[str, Any]] = []
    for relation in source.get("relations") or []:
        digest = str(relation.get("source_row_sha256") or "")
        try:
            resolved, classes = resolve_relation_operands(
                relation, wave_id=import_slug
            )
            emitted = emit_resolved_relation(
                resolved,
                lock_row={
                    "stable_id": relation.get("stable_id"),
                    "template_key": relation.get("template_key") or template_key,
                },
                operand_classes=classes,
                template_key=str(relation.get("template_key") or template_key),
            )
            consume = _consume_identity(emitted, wave_id=import_slug)
        except Exception as error:
            dropped.append(
                {
                    "input": ((relation.get("item_inputs") or [{}])[0] or {}).get("value"),
                    "output": ((relation.get("item_outputs") or [{}])[0] or {}).get("value"),
                    "reasons": [str(error)],
                    "source_recipe_index": relation.get("source_recipe_index"),
                    "source_row_sha256": digest,
                    "status": "blocked",
                }
            )
            continue
        if consume in seen:
            dropped.append(
                {
                    "input": ((relation.get("item_inputs") or [{}])[0] or {}).get("value"),
                    "output": ((relation.get("item_outputs") or [{}])[0] or {}).get("value"),
                    "reasons": [
                        f"consume-identity collision with {seen[consume]}"
                    ],
                    "source_recipe_index": relation.get("source_recipe_index"),
                    "source_row_sha256": digest,
                    "status": "blocked",
                }
            )
            continue
        seen[consume] = digest
        keep_hashes.add(digest)
    if not dropped:
        return 0
    family = (work.get("families") or [{}])[0]
    kept = [
        row
        for row in (family.get("relations") or [])
        if row.get("source_row_sha256") in keep_hashes
    ]
    for index, row in enumerate(kept):
        row["shadow_order"] = index
    family["relations"] = kept
    overflow_rows = list(overflow_doc.get("overflow") or []) + dropped
    overflow_doc["overflow"] = overflow_rows
    overflow_doc["blocked_rows"] = len(overflow_rows)
    accounting = work.setdefault("accounting", {})
    accounting["selected_rows"] = len(kept)
    accounting["overflow_rows"] = len(overflow_rows)
    accounting["blocked_rows"] = len(overflow_rows)
    work["overflow"] = overflow_rows
    _write(wave / "source_pack" / "work_set.json", work)
    _write(wave / "overflow.json", overflow_doc)
    _write(
        wave / "source_pack_manifest.json",
        build_manifest(wave, str(census.load_json(wave / "source_pack_manifest.json").get("source_pack_id"))),
    )
    return len(dropped)


def _write_empty_selected_import(
    wave: Path,
    *,
    host: str,
    import_slug: str,
    target_map: str,
    source_pack_id: str,
) -> None:
    """All dump rows overflowed; keep an explicit empty selected set."""
    _write(
        wave / "source.json",
        {
            "family_count": 0,
            "generated_by": "tools/waves/prep/machine_prep_common.py",
            "host": host,
            "import_slug": import_slug,
            "relation_count": 0,
            "relations": [],
            "schema_version": 1,
            "source_pack_id": source_pack_id,
            "source_revision": SOURCE_REVISION,
            "target_map": target_map,
        },
    )
    _write(
        wave / "lock_candidate.json",
        {
            "families": [],
            "family_count": 0,
            "generated_by": "tools/waves/prep/machine_prep_common.py",
            "import_slug": import_slug,
            "relation_count": 0,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "LOCK_CANDIDATE",
        },
    )
    _write(
        wave / "source_receipt.json",
        {
            "generated_by": "tools/waves/prep/machine_prep_common.py",
            "import_slug": import_slug,
            "relation_count": 0,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "SOURCE_IMPORT_RECEIPT",
            "note": "selected work-set is empty; every source row is explicit overflow",
        },
    )
    _write(
        wave / "source_review.json",
        {
            "blockers": ["selected work-set is empty"],
            "family_count": 0,
            "generated_by": "tools/waves/prep/machine_prep_common.py",
            "import_slug": import_slug,
            "relation_count": 0,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "SOURCE_IMPORT_REVIEW",
        },
    )


def write_source_pack(
    *,
    slug: str,
    source_map: str,
    target_map: str,
    host: str,
    source_rows: int,
    source_pack_id: str,
    family_id: str,
    template_key: str,
    import_slug: str,
) -> dict[str, int]:
    wave = wave_dir(slug)
    raw = dump_path(source_map)
    dump = json.loads(raw.read_text(encoding="utf-8"))
    recipes = list(dump.get("recipes") or [])
    if len(recipes) != source_rows:
        raise ValueError(
            f"{source_map} dump must contain {source_rows} rows, got {len(recipes)}"
        )
    selected, overflow = audit_rows(
        dump,
        host=host,
        target_map=target_map,
        source_map=source_map,
        family_id=family_id,
        template_key=template_key,
    )
    dump_slice = {
        "recipes": [
            source_row(recipe, index, row_hash(recipe), template_key)
            for index, recipe in enumerate(recipes)
        ],
        "source_map": source_map,
        "source_revision": SOURCE_REVISION,
    }
    work_set = build_work_set(
        selected,
        overflow,
        len(recipes),
        family_id=family_id,
        template_key=template_key,
        host=host,
    )
    _write(wave / "source_pack" / "dump_slice.json", dump_slice)
    _write(wave / "source_pack" / "work_set.json", work_set)
    _write(
        wave / "overflow.json",
        {
            "blocked_rows": len(overflow),
            "overflow": overflow,
            "schema_version": 1,
            "source_map": source_map,
            "source_revision": SOURCE_REVISION,
            "status": "EXPLICITLY_BLOCKED_OVERFLOW",
        },
    )
    _write(wave / "source_pack_manifest.json", build_manifest(wave, source_pack_id))
    _write(
        wave / "recipe_import.json",
        build_import_spec(
            wave,
            host=host,
            import_slug=import_slug,
            source_map=source_map,
            target_map=target_map,
        ),
    )
    if selected:
        source_import.write_import(wave / "recipe_import.json")
        dropped = _drop_consume_collisions(
            wave,
            host=host,
            family_id=family_id,
            template_key=template_key,
            import_slug=import_slug,
        )
        if dropped:
            source_import.write_import(wave / "recipe_import.json")
            selected_count = int(
                (census.load_json(wave / "source_pack" / "work_set.json")
                 .get("accounting") or {})
                .get("selected_rows") or 0
            )
            overflow_count = int(
                (census.load_json(wave / "overflow.json").get("blocked_rows") or 0)
            )
            return {
                "source_rows": len(recipes),
                "selected_rows": selected_count,
                "overflow_rows": overflow_count,
            }
    else:
        _write_empty_selected_import(
            wave,
            host=host,
            import_slug=import_slug,
            target_map=target_map,
            source_pack_id=source_pack_id,
        )
    return {
        "source_rows": len(recipes),
        "selected_rows": len(selected),
        "overflow_rows": len(overflow),
    }


def copy_basicmachine_art(
    gt6_folder: str,
    cc_folder: str,
    *,
    gui_source: str,
    gui_dest_stems: tuple[str, ...],
    manifest_name: str,
) -> dict[str, Any]:
    source_root = GT6_BLOCK_ROOT / gt6_folder
    if not source_root.is_dir():
        raise FileNotFoundError(f"missing GT6 machine art {source_root}")
    dest_root = ASSETS / "textures" / "block" / "machine" / cc_folder
    imports: list[dict[str, str]] = []
    for src in sorted(source_root.rglob("*")):
        if not src.is_file():
            continue
        rel = src.relative_to(source_root).as_posix()
        dest = dest_root / rel
        dest.parent.mkdir(parents=True, exist_ok=True)
        if dest.exists():
            dest.chmod(stat.S_IWRITE)
        shutil.copyfile(src, dest)
        if dest.read_bytes() != src.read_bytes():
            raise ValueError(f"art copy drifted: {rel}")
        imports.append(
            {
                "destination": census.relative(dest).removeprefix(
                    "src/main/resources/"
                ),
                "gt6_source": src.relative_to(GT6_ART).as_posix(),
                "source": "gt6_referencable_port_code/gregtech6_w",
            }
        )
    gui_src = GT6_GUI_ROOT / gui_source
    if not gui_src.is_file():
        raise FileNotFoundError(f"missing GT6 GUI {gui_src}")
    for stem in gui_dest_stems:
        gui_dest = ASSETS / "textures" / "gui" / "machines" / f"{stem}.png"
        gui_dest.parent.mkdir(parents=True, exist_ok=True)
        if gui_dest.exists():
            gui_dest.chmod(stat.S_IWRITE)
        shutil.copyfile(gui_src, gui_dest)
        if gui_dest.read_bytes() != gui_src.read_bytes():
            raise ValueError(f"GUI copy drifted: {stem}")
        imports.append(
            {
                "destination": census.relative(gui_dest).removeprefix(
                    "src/main/resources/"
                ),
                "gt6_source": gui_src.relative_to(GT6_ART).as_posix(),
                "source": "gt6_referencable_port_code/gregtech6_w",
            }
        )
    document = {
        "imports": imports,
        "source": "gt6_referencable_port_code/gregtech6_w",
        "source_present": True,
        "source_revision": SOURCE_REVISION,
    }
    _write(ASSETS / manifest_name, document)
    return document


def check_art_manifest(
    art_manifest: str,
    *,
    min_art_imports: int,
    forbidden_art: tuple[str, ...] = ("multiblock_casing", "heat_exchanger"),
) -> list[str]:
    errors: list[str] = []
    art_path = ASSETS / art_manifest
    if not art_path.is_file():
        return [f"missing {census.relative(art_path)}"]
    art = census.load_json(art_path)
    rows = art.get("imports") or []
    if len(rows) < min_art_imports:
        errors.append(f"{art_manifest} must copy at least {min_art_imports} files")
    if not GT6_ART.is_dir():
        errors.append("gt6_referencable_port_code/gregtech6_w is missing")
    for row in rows:
        destination = ROOT / "src" / "main" / "resources" / row["destination"]
        source = GT6_ART / row["gt6_source"]
        dest_text = str(row.get("destination") or "")
        if not destination.is_file():
            errors.append(f"missing dest art {row['destination']}")
            continue
        if not source.is_file():
            errors.append(f"missing GT6 art {row['gt6_source']}")
            continue
        if source.read_bytes() != destination.read_bytes():
            errors.append(f"art bytes drifted {row['destination']}")
        for needle in forbidden_art:
            if needle in dest_text:
                errors.append(f"aliased art path {row['destination']}")
    return errors


def check_source_pack(
    *,
    slug: str,
    source_rows: int,
    import_slug: str,
    note: str,
    live_needle: str,
    min_art_imports: int,
    art_manifest: str,
    forbidden_art: tuple[str, ...] = ("multiblock_casing", "heat_exchanger", "smelter"),
) -> list[str]:
    errors: list[str] = []
    wave = wave_dir(slug)
    required = (
        wave / "source_pack" / "dump_slice.json",
        wave / "source_pack" / "work_set.json",
        wave / "source_pack_manifest.json",
        wave / "recipe_import.json",
        wave / "overflow.json",
        wave / "lock_candidate.json",
        wave / "production_lock.json",
        wave / "d0_obtain_matrix.json",
    )
    for path in required:
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
    if errors:
        return errors

    try:
        manifest = source_import.load_manifest(wave / "source_pack_manifest.json")
        source_import.verify_files(manifest, require_present=True)
    except Exception as error:
        errors.append(f"source pack: {error}")

    work = census.load_json(wave / "source_pack" / "work_set.json")
    overflow = census.load_json(wave / "overflow.json")
    accounting = work.get("accounting") or {}
    selected = (work.get("families") or [{}])[0].get("relations") or []
    if selected:
        try:
            errors.extend(source_import.check_import(wave / "recipe_import.json"))
        except Exception as error:
            errors.append(f"import: {error}")
    elif accounting.get("selected_rows") not in (0, None):
        errors.append("empty work-set must account selected_rows as 0")
    if accounting.get("source_rows") != source_rows:
        errors.append(f"work_set accounting source_rows must be {source_rows}")
    if accounting.get("selected_rows") != len(selected):
        errors.append("work_set accounting selected_rows disagrees with family relations")
    if accounting.get("overflow_rows") != len(overflow.get("overflow") or []):
        errors.append("work_set accounting overflow_rows disagrees with overflow")
    if accounting.get("source_rows") != (
        int(accounting.get("selected_rows") or 0)
        + int(accounting.get("overflow_rows") or 0)
    ):
        errors.append("dump must split into selected + overflow")

    expected_lock = pilot_lock(wave, import_slug, note)
    actual_lock = census.load_json(wave / "production_lock.json")
    if actual_lock != expected_lock:
        errors.append("production_lock drifted from reviewed lock_candidate")
    if actual_lock.get("production_authority") is not True:
        errors.append("production_lock must keep production_authority for isolated compile")
    if "not player_complete" not in str(actual_lock.get("note") or ""):
        errors.append("production_lock must not claim player_complete")

    errors.extend(
        check_art_manifest(
            art_manifest,
            min_art_imports=min_art_imports,
            forbidden_art=forbidden_art,
        )
    )

    if errors:
        return errors
    if not selected:
        source = census.load_json(wave / "source.json")
        if source.get("relation_count") != 0:
            errors.append("empty selected set must keep source.json relation_count 0")
        return errors
    try:
        metrics = isolated_compile(wave, live_needle)
    except Exception as error:
        errors.append(f"isolated compile: {error}")
        return errors
    if metrics.get("family_count") != 1:
        errors.append("isolated compile must emit one family")
    if metrics.get("relation_count") != accounting.get("selected_rows"):
        errors.append(
            "isolated compile must emit the selected work-set relation count"
        )
    if metrics.get("representations", {}).get("exact_multi") != 1:
        errors.append("isolated compile must stay exact_multi")
    if metrics.get("overflow"):
        errors.append("isolated compile must not silently overflow")
    if metrics.get("parameterized"):
        errors.append("isolated compile must not emit parameterized families")
    return errors
