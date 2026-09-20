#!/usr/bin/env python3
"""Import resolvable gt.recipe.squeezer dump rows on this unique-active card.

Select only rows whose every operand already has a current expression.
Ignore plant_gt_* crop rows on this card: GT6 survival obtain was IC2
crossbreeding, not mineral bush worldgen. A later crop-breeding card may
reclaim GT_BaseCrop drop rows only, not the prefix long-tail.
Keep the four Java latex rows as exact remainder. Not player_complete.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[4]
TOOLS = ROOT / "tools"
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))
if str(TOOLS) not in sys.path:
    sys.path.insert(1, str(TOOLS))

from tools import census_common as census
from tools.recipe_bulk import compile as compile_mod
from tools.recipe_bulk import source_import
from tools.recipe_bulk.dialects import gt6
from tools.recipe_bulk.matrix import authored_relations
from tools.recipe_bulk.membership import membership_root
from tools.recipe_bulk.transport import reassemble_documents
from tools.recipe_bulk.waves import recipe_wave

_COMMON_PATH = ROOT / "tools" / "waves" / "prep" / "machine_prep_common.py"
_COMMON_SPEC = importlib.util.spec_from_file_location(
    "machine_prep_common_squeezer", _COMMON_PATH
)
assert _COMMON_SPEC is not None and _COMMON_SPEC.loader is not None
common = importlib.util.module_from_spec(_COMMON_SPEC)
_COMMON_SPEC.loader.exec_module(common)

SOURCE_REVISION = common.SOURCE_REVISION
SOURCE_MAP = "gt.recipe.squeezer"
TARGET_MAP = "cruciblecraft:squeezer"
HOST = TARGET_MAP
IMPORT_SLUG = "machines/hammer-squeezer-laser"
SOURCE_PACK_ID = IMPORT_SLUG
FAMILY_ID = (
    "portfolio:track_a/cruciblecraft:squeezer/"
    "gt.recipe.squeezer#0000"
)
TEMPLATE_KEY = "gt.recipe.squeezer#0000"
SOURCE_ROWS = 5322
SELECTED_ROWS = 15
OVERFLOW_ROWS = 5307
IGNORED_PLANT_GT_ROWS = 5215
ACTIONABLE_OVERFLOW_ROWS = 92
JAVA_LATEX_ROWS = 4
PLANT_GT_ITEM_MARK = "gt.meta.plantgt"
IGNORE_PLANT_GT_REASON = (
    "ignore: plant_gt_* prefix item has no current obtain; GT6 survival was "
    "IC2 crop crossbreeding, not mineral bush worldgen. This squeezer card "
    "does not import these rows. A later crop-breeding card may reclaim only "
    "GT_BaseCrop drop rows, not the prefix long-tail."
)
IGNORE_POLICY = {
    "bucket": "ignore_unobtainable_plant_gt",
    "future_reclaim": "crop_breeding_obtain_for_gt_base_crop_drops_only",
    "this_card": "do_not_import",
}
LIVE_NEEDLE = "squeezer"
CAPABILITY_PATH = (
    ROOT / "tools" / "capabilities" / "machines" / "hammer-squeezer-laser"
    / "capability.json"
)
WAVE = ROOT / "tools" / "waves" / "machines" / "hammer-squeezer-laser"
LIVE_GENERATED = (
    ROOT / "src" / "recipe_generated" / "resources" / "data"
    / "cruciblecraft" / "recipe"
)
POLICY_PATH = LIVE_GENERATED / "publication_policy" / "squeezer.json"
PUBLICATION_GROUP = f"{TARGET_MAP}/pilot/hammer_squeezer_laser"
LATER_WAVE = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft"
    / "later_wave_publication_baseline.json"
)
JAVA_LATEX = frozenset(
    {
        "cruciblecraft:tree/rubber_resin",
        "cruciblecraft:tree/rubber_leaves",
        "cruciblecraft:tree/rubber_sapling",
        "cruciblecraft:wood_rubber/dust",
    }
)
LOCK_NOTE = (
    "live compile for machines/hammer-squeezer-laser squeezer dump; "
    f"{SELECTED_ROWS} selected exact non-plant rows; {OVERFLOW_ROWS} overflow "
    f"of which {IGNORED_PLANT_GT_ROWS} ignored plant_gt_* until crop-breeding "
    f"obtain and {ACTIONABLE_OVERFLOW_ROWS} actionable; four Java latex rows "
    "remain handwritten remainder; not player_complete"
)


def _write(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    census.write_stable(path, document)


def live_family_files() -> list[Path]:
    root = LIVE_GENERATED / LIVE_NEEDLE
    if not root.is_dir():
        return []
    return [
        path
        for path in root.rglob("*.json")
        if path.is_file() and "publication_policy" not in path.as_posix()
    ]


def unique_active_wave() -> str | None:
    if not CAPABILITY_PATH.is_file():
        return IMPORT_SLUG
    document = census.load_json(CAPABILITY_PATH)
    if document.get("workflow") == "active":
        return IMPORT_SLUG
    return None


def _reason_bucket(text: str) -> str:
    lowered = text.lower()
    if lowered.startswith("ignore:") or "unobtainable plant_gt_" in lowered:
        return "ignore_unobtainable_plant_gt"
    if "already authored from java latex" in lowered:
        return "java_latex_remainder"
    if "unregistered material form" in lowered or "unregistered runtime item" in lowered:
        return "unregistered_form"
    if "unmapped fluid" in lowered:
        return "unmapped_fluid"
    if "no modern catalog id" in lowered:
        return "unmapped_catalog"
    if "unmapped" in lowered:
        return "unmapped_operand"
    if "exceeds" in lowered:
        return "tank_limit"
    if "shadowed" in lowered:
        return "shadow"
    if "consume-identity" in lowered:
        return "consume_collision"
    return "other"


def _stack_is_plant_gt(stack: dict[str, Any] | None) -> bool:
    item = str((stack or {}).get("item") or (stack or {}).get("id") or "").lower()
    return PLANT_GT_ITEM_MARK in item


def _dump_has_plant_gt(recipe: dict[str, Any]) -> bool:
    stacks = list(recipe.get("inputs") or []) + list(recipe.get("outputs") or [])
    return any(_stack_is_plant_gt(stack) for stack in stacks)


def _mark_ignored_plant_gt(entry: dict[str, Any]) -> dict[str, Any]:
    reasons = [
        reason
        for reason in (entry.get("reasons") or [])
        if reason != IGNORE_PLANT_GT_REASON
    ]
    reasons.insert(0, IGNORE_PLANT_GT_REASON)
    entry["reasons"] = reasons
    entry["status"] = "ignored"
    entry["disposition"] = "ignore"
    return entry


def _ignored_count(overflow: list[dict[str, Any]]) -> int:
    return sum(1 for row in overflow if row.get("disposition") == "ignore")


def _apply_overflow_accounting(
    work: dict[str, Any], overflow: list[dict[str, Any]]
) -> None:
    ignored = _ignored_count(overflow)
    accounting = work.setdefault("accounting", {})
    accounting["overflow_rows"] = len(overflow)
    accounting["blocked_rows"] = len(overflow)
    accounting["ignored_rows"] = ignored
    accounting["actionable_overflow_rows"] = len(overflow) - ignored


def _overflow_buckets(overflow: list[dict[str, Any]]) -> dict[str, int]:
    counts: Counter[str] = Counter()
    for row in overflow:
        reasons = row.get("reasons") or ["unknown"]
        counts[_reason_bucket(str(reasons[0]))] += 1
    return dict(counts.most_common())


def _audit_squeezer_rows(
    dump: dict[str, Any],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    maps = gt6._maps()
    selected: list[dict[str, Any]] = []
    overflow: list[dict[str, Any]] = []
    for index, recipe in enumerate(dump.get("recipes") or []):
        digest = common.row_hash(recipe)
        inputs = recipe.get("inputs") or [{}]
        outputs = recipe.get("outputs") or [{}]
        display_in = (inputs[0] if inputs else {}).get("displayName")
        display_out = (outputs[0] if outputs else {}).get("displayName")
        entry = {
            "source_recipe_index": index,
            "source_row_sha256": digest,
            "input": display_in,
            "output": display_out,
        }
        if _dump_has_plant_gt(recipe):
            overflow.append(_mark_ignored_plant_gt(entry))
            continue
        try:
            relation, errors = gt6.compile_row(
                recipe,
                host=HOST,
                target_map=TARGET_MAP,
                source_map=SOURCE_MAP,
                family_id=FAMILY_ID,
                template_key=TEMPLATE_KEY,
                recipe_index=index,
                shadow_order=0,
                source_revision=SOURCE_REVISION,
                source_row_sha256=digest,
                maps=maps,
            )
        except Exception as error:
            entry["status"] = "blocked"
            entry["reasons"] = [str(error)]
            overflow.append(entry)
            continue
        blockers = list(dict.fromkeys(str(error) for error in errors))
        if source_import._relation_unmapped(relation):
            blockers.append("unmapped operand")
        blockers.extend(common.missing_runtime_item_operands(relation))
        for fluid in relation.get("fluid_outputs") or []:
            amount = int(
                fluid.get("amount")
                or (fluid.get("source") or {}).get("amount")
                or 0
            )
            if amount > 8_000:
                blockers.append("squeezer fluid output exceeds 8000 mB tank")
        runtime_in = [
            str(operand.get("runtime_id") or "")
            for operand in relation.get("item_inputs") or []
        ]
        if any(item in JAVA_LATEX for item in runtime_in):
            blockers.append("already authored from Java latex remainder")
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


def _shadow_signature(relation: dict[str, Any]) -> tuple[Any, ...]:
    actions = list(relation.get("item_input_actions") or [])
    inputs = []
    for index, operand in enumerate(relation.get("item_inputs") or []):
        action = actions[index] if index < len(actions) else {}
        inputs.append(
            (
                action.get("kind"),
                action.get("damage", 0),
                operand.get("runtime_id"),
                operand.get("tag"),
            )
        )
    fluids = tuple(
        (
            operand.get("runtime_id"),
            operand.get("tag"),
        )
        for operand in relation.get("fluid_inputs") or []
    )
    return tuple(inputs), fluids


def _drop_shadow_collisions() -> int:
    source = census.load_json(WAVE / "source.json")
    work = census.load_json(WAVE / "source_pack" / "work_set.json")
    overflow_document = census.load_json(WAVE / "overflow.json")
    seen: dict[tuple[Any, ...], str] = {}
    keep_hashes: set[str] = set()
    dropped: list[dict[str, Any]] = []
    for relation in source.get("relations") or []:
        signature = _shadow_signature(relation)
        digest = str(relation.get("source_row_sha256") or "")
        if signature in seen:
            dropped.append(
                {
                    "input": (
                        (relation.get("item_inputs") or [{}])[0]
                        .get("source", {})
                        .get("displayName")
                    ),
                    "output": (
                        (relation.get("item_outputs") or [{}])[0]
                        .get("source", {})
                        .get("displayName")
                    ),
                    "reasons": [
                        "shadowed input signature by " + seen[signature]
                    ],
                    "source_recipe_index": relation.get("source_recipe_index"),
                    "source_row_sha256": digest,
                    "status": "blocked",
                }
            )
            continue
        seen[signature] = digest
        keep_hashes.add(digest)
    if not dropped:
        return 0
    family = (work.get("families") or [{}])[0]
    kept = [
        row
        for row in family.get("relations") or []
        if row.get("source_row_sha256") in keep_hashes
    ]
    for index, row in enumerate(kept):
        row["shadow_order"] = index
    family["relations"] = kept
    overflow_rows = list(overflow_document.get("overflow") or []) + dropped
    overflow_document["overflow"] = overflow_rows
    ignored = _ignored_count(overflow_rows)
    overflow_document["blocked_rows"] = len(overflow_rows)
    overflow_document["ignored_rows"] = ignored
    overflow_document["actionable_overflow_rows"] = len(overflow_rows) - ignored
    accounting = work.setdefault("accounting", {})
    accounting["selected_rows"] = len(kept)
    _apply_overflow_accounting(work, overflow_rows)
    work["overflow"] = overflow_rows
    _write(WAVE / "source_pack" / "work_set.json", work)
    _write(WAVE / "overflow.json", overflow_document)
    _write(
        WAVE / "source_pack_manifest.json",
        common.build_manifest(WAVE, SOURCE_PACK_ID),
    )
    return len(dropped)


def _write_overflow_summary(overflow: list[dict[str, Any]], selected_rows: int) -> None:
    ignored = _ignored_count(overflow)
    _write(
        WAVE / "squeezer_overflow.json",
        {
            "actionable_overflow_rows": len(overflow) - ignored,
            "buckets": _overflow_buckets(overflow),
            "ignored_rows": ignored,
            "ignore_policy": dict(IGNORE_POLICY),
            "note": (
                "plant_gt_* crop rows are ignored on this squeezer card. GT6 "
                "survival obtain was IC2 crop crossbreeding, not mineral bush "
                "worldgen. Do not treat ignored_rows as a form-open or dump "
                "queue. A later crop-breeding card may reclaim only named "
                "GT_BaseCrop drop rows. Remaining overflow is food catalog / "
                "unmapped ice-juice-potion fluids / latex remainder. Four "
                "Java latex rows stay handwritten. 1.7.10 fish damage-split "
                "is live as cod/salmon/tropical_fish."
            ),
            "overflow_rows": len(overflow),
            "selected_rows": selected_rows,
            "source_map": SOURCE_MAP,
            "source_revision": SOURCE_REVISION,
            "source_rows": SOURCE_ROWS,
        },
    )


def write_source_pack() -> dict[str, int]:
    raw = common.dump_path(SOURCE_MAP)
    dump = json.loads(raw.read_text(encoding="utf-8"))
    recipes = list(dump.get("recipes") or [])
    if len(recipes) != SOURCE_ROWS:
        raise ValueError(
            f"{SOURCE_MAP} dump must contain {SOURCE_ROWS} rows, got {len(recipes)}"
        )
    selected, overflow = _audit_squeezer_rows(dump)
    work = common.build_work_set(
        selected,
        overflow,
        len(recipes),
        family_id=FAMILY_ID,
        template_key=TEMPLATE_KEY,
        host=HOST,
    )
    _apply_overflow_accounting(work, overflow)
    ignored = _ignored_count(overflow)
    _write(
        WAVE / "source_pack" / "dump_slice.json",
        {
            "recipes": [
                common.source_row(recipe, index, common.row_hash(recipe), TEMPLATE_KEY)
                for index, recipe in enumerate(recipes)
            ],
            "source_map": SOURCE_MAP,
            "source_revision": SOURCE_REVISION,
        },
    )
    _write(WAVE / "source_pack" / "work_set.json", work)
    _write(
        WAVE / "overflow.json",
        {
            "actionable_overflow_rows": len(overflow) - ignored,
            "blocked_rows": len(overflow),
            "ignored_rows": ignored,
            "ignore_policy": dict(IGNORE_POLICY),
            "overflow": overflow,
            "schema_version": 1,
            "source_map": SOURCE_MAP,
            "source_revision": SOURCE_REVISION,
            "status": "EXPLICITLY_BLOCKED_OVERFLOW",
        },
    )
    _write(
        WAVE / "source_pack_manifest.json",
        common.build_manifest(WAVE, SOURCE_PACK_ID),
    )
    _write(
        WAVE / "recipe_import.json",
        common.build_import_spec(
            WAVE,
            host=HOST,
            import_slug=IMPORT_SLUG,
            source_map=SOURCE_MAP,
            target_map=TARGET_MAP,
        ),
    )
    if not selected:
        raise ValueError("squeezer selected work set must not be empty")
    source_import.write_import(WAVE / "recipe_import.json")
    shadow_dropped = _drop_shadow_collisions()
    if shadow_dropped:
        source_import.write_import(WAVE / "recipe_import.json")
    dropped = common._drop_consume_collisions(
        WAVE,
        host=HOST,
        family_id=FAMILY_ID,
        template_key=TEMPLATE_KEY,
        import_slug=IMPORT_SLUG,
    )
    if dropped:
        source_import.write_import(WAVE / "recipe_import.json")
    work = census.load_json(WAVE / "source_pack" / "work_set.json")
    overflow_document = census.load_json(WAVE / "overflow.json")
    overflow_rows = list(overflow_document.get("overflow") or [])
    ignored = _ignored_count(overflow_rows)
    overflow_document["blocked_rows"] = len(overflow_rows)
    overflow_document["ignored_rows"] = ignored
    overflow_document["actionable_overflow_rows"] = len(overflow_rows) - ignored
    overflow_document["ignore_policy"] = dict(IGNORE_POLICY)
    overflow_document.pop("never_import", None)
    _apply_overflow_accounting(work, overflow_rows)
    _write(WAVE / "source_pack" / "work_set.json", work)
    _write(WAVE / "overflow.json", overflow_document)
    _write(
        WAVE / "source_pack_manifest.json",
        common.build_manifest(WAVE, SOURCE_PACK_ID),
    )
    accounting = work.get("accounting") or {}
    _write_overflow_summary(
        overflow_rows,
        int(accounting.get("selected_rows") or 0),
    )
    return {
        "source_rows": len(recipes),
        "selected_rows": int(accounting.get("selected_rows") or 0),
        "overflow_rows": int(accounting.get("overflow_rows") or 0),
    }


def write_wave_sidecars() -> None:
    wave = unique_active_wave()
    _write(
        WAVE / "topology.json",
        {
            "append_only": False,
            "complete_family_count": 1,
            "generated_by": "machines/hammer-squeezer-laser squeezer dump",
            "ignored_rows": IGNORED_PLANT_GT_ROWS,
            "next_unassigned": True,
            "remaining_recipe_gap": ACTIONABLE_OVERFLOW_ROWS,
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "WAVE_READY",
            "unique_active_wave": wave,
            "wave_slug": IMPORT_SLUG,
        },
    )
    _write(
        WAVE / "readiness.json",
        {
            "evidence": {
                "dump_rows": SOURCE_ROWS,
                "hosts": 4,
                "ignored_plant_gt_rows": IGNORED_PLANT_GT_ROWS,
                "java_latex_rows": JAVA_LATEX_ROWS,
                "owns_families": 1,
                "selected_rows": SELECTED_ROWS,
                "status": "runtime_ready",
            },
            "generated_by": "machines/hammer-squeezer-laser squeezer dump",
            "generated_recipe_count": SELECTED_ROWS,
            "next_unassigned": True,
            "owns_families": 1,
            "production_lock": census.relative(WAVE / "production_lock.json"),
            "schema_version": 1,
            "source_revision": SOURCE_REVISION,
            "status": "SQUEEZER_DUMP_RUNTIME_READY",
            "unique_active_wave": wave,
            "wave_slug": IMPORT_SLUG,
        },
    )


def expected_publication_policy() -> dict[str, Any]:
    families = live_family_files()
    if not families:
        raise ValueError("need live compact families to bind publication policy")
    documents = [census.load_json(path) for path in sorted(families)]
    assembled = reassemble_documents(documents)
    relations = authored_relations(assembled)
    family_id = str(assembled.get("family_id") or "")
    stable_ids = [str(row.get("stable_id") or "") for row in relations]
    if family_id != TEMPLATE_KEY or not all(stable_ids) or len(relations) != SELECTED_ROWS:
        raise ValueError("live squeezer family cannot bind publication policy")
    return {
        "cache_ceiling": 0,
        "eager_stable_ids": [],
        "family_count": 1,
        "membership_root_sha256": membership_root([family_id], stable_ids),
        "policy_type": "immediate",
        "publication_group": PUBLICATION_GROUP,
        "relation_count": SELECTED_ROWS,
        "routing_schema_version": "compact-shard-v1",
        "target_map": TARGET_MAP,
        "type": "cruciblecraft:compact_publication_policy",
    }


def write_publication_policy() -> None:
    _write(POLICY_PATH, expected_publication_policy())


def live_compile() -> dict[str, Any]:
    spec = recipe_wave(IMPORT_SLUG)
    built = compile_mod.compile_wave(IMPORT_SLUG)
    compile_mod.write_tree(
        built["planned"],
        spec.generated_root,
        spec.generated_root,
        path_prefix=spec.path_prefix,
        tree_prefixes=spec.tree_prefixes,
    )
    return built["report"]


def update_later_wave(selected_rows: int) -> None:
    document = census.load_json(LATER_WAVE)
    ledger = document.setdefault("delta_ledger_policy", {})
    deltas = list(ledger.get("registered_deltas") or [])
    existing = next(
        (row for row in deltas if row.get("phase") == "squeezer_dump"),
        None,
    )
    previous = int((existing or {}).get("logical") or 0)
    other = [row for row in deltas if row.get("phase") != "squeezer_dump"]
    other.append(
        {
            "eager": selected_rows,
            "evidence": (
                "machines/hammer-squeezer-laser: dump gt.recipe.squeezer "
                "selected exact non-plant rows; plant_gt_* ignored until "
                "crop-breeding obtain"
            ),
            "lazy": 0,
            "logical": selected_rows,
            "phase": "squeezer_dump",
        }
    )
    ledger["registered_deltas"] = other
    eager = int(ledger.get("base_eager") or 0) + sum(
        int(row.get("eager") or 0) for row in other
    )
    ledger["current_eager"] = eager
    ledger["current_logical"] = eager + int(ledger.get("current_lazy") or 0)
    delta = document.setdefault("later_wave_publication_delta", {})
    header = int(delta.get("logical_rows_added") or 0) - previous + selected_rows
    delta["logical_rows_added"] = header
    delta["eager_rows_added"] = header
    delta["gt_recipe_rows_added"] = header
    evidence = str(delta.get("evidence") or "")
    target = (
        "squeezer dump selected exact non-plant rows; plant_gt_* ignored "
        "until crop-breeding obtain"
    )
    for previous in (
        "squeezer dump selected exact rows, plant_gt_* long-tail overflow",
        "squeezer dump selected exact non-plant rows; plant_gt_* ignored "
        "never-import",
    ):
        if previous in evidence:
            evidence = evidence.replace(previous, target)
    if target not in evidence:
        evidence = evidence.rstrip() + "; " + target
    delta["evidence"] = evidence
    _write(LATER_WAVE, document)


def write() -> dict[str, Any]:
    counts = write_source_pack()
    if counts["selected_rows"] != SELECTED_ROWS:
        raise ValueError(
            f"selected rows {counts['selected_rows']} != {SELECTED_ROWS}"
        )
    if counts["overflow_rows"] != OVERFLOW_ROWS:
        raise ValueError(
            f"overflow rows {counts['overflow_rows']} != {OVERFLOW_ROWS}"
        )
    common.freeze_lock(WAVE, IMPORT_SLUG, LOCK_NOTE)
    write_wave_sidecars()
    isolated = common.isolated_compile(WAVE, LIVE_NEEDLE)
    live = live_compile()
    write_publication_policy()
    update_later_wave(SELECTED_ROWS)
    return {
        "isolated": isolated,
        "live": live,
        "source": counts,
    }


def check() -> list[str]:
    errors: list[str] = []
    required = (
        WAVE / "source_pack" / "dump_slice.json",
        WAVE / "source_pack" / "work_set.json",
        WAVE / "source_pack_manifest.json",
        WAVE / "recipe_import.json",
        WAVE / "overflow.json",
        WAVE / "squeezer_overflow.json",
        WAVE / "lock_candidate.json",
        WAVE / "production_lock.json",
        WAVE / "topology.json",
        WAVE / "readiness.json",
    )
    for path in required:
        if not path.is_file():
            errors.append(f"missing {census.relative(path)}")
    if errors:
        return errors

    try:
        manifest = source_import.load_manifest(WAVE / "source_pack_manifest.json")
        source_import.verify_files(manifest, require_present=True)
    except Exception as error:
        errors.append(f"source pack: {error}")

    work = census.load_json(WAVE / "source_pack" / "work_set.json")
    overflow = census.load_json(WAVE / "overflow.json")
    summary = census.load_json(WAVE / "squeezer_overflow.json")
    accounting = work.get("accounting") or {}
    selected = (work.get("families") or [{}])[0].get("relations") or []
    if accounting.get("source_rows") != SOURCE_ROWS:
        errors.append(f"work_set accounting source_rows must be {SOURCE_ROWS}")
    if accounting.get("selected_rows") != SELECTED_ROWS:
        errors.append(f"work_set accounting selected_rows must be {SELECTED_ROWS}")
    if accounting.get("selected_rows") != len(selected):
        errors.append("work_set accounting selected_rows disagrees with family relations")
    if accounting.get("overflow_rows") != OVERFLOW_ROWS:
        errors.append(f"work_set accounting overflow_rows must be {OVERFLOW_ROWS}")
    if accounting.get("overflow_rows") != len(overflow.get("overflow") or []):
        errors.append("work_set accounting overflow_rows disagrees with overflow")
    if accounting.get("ignored_rows") != IGNORED_PLANT_GT_ROWS:
        errors.append(
            f"work_set accounting ignored_rows must be {IGNORED_PLANT_GT_ROWS}"
        )
    if accounting.get("actionable_overflow_rows") != ACTIONABLE_OVERFLOW_ROWS:
        errors.append(
            "work_set accounting actionable_overflow_rows must be "
            f"{ACTIONABLE_OVERFLOW_ROWS}"
        )
    if summary.get("selected_rows") != SELECTED_ROWS:
        errors.append("squeezer_overflow selected_rows drifted")
    if summary.get("overflow_rows") != OVERFLOW_ROWS:
        errors.append("squeezer_overflow overflow_rows drifted")
    if summary.get("ignored_rows") != IGNORED_PLANT_GT_ROWS:
        errors.append("squeezer_overflow ignored_rows drifted")
    if summary.get("actionable_overflow_rows") != ACTIONABLE_OVERFLOW_ROWS:
        errors.append("squeezer_overflow actionable_overflow_rows drifted")
    if overflow.get("ignored_rows") != IGNORED_PLANT_GT_ROWS:
        errors.append("overflow.json ignored_rows drifted")
    if overflow.get("actionable_overflow_rows") != ACTIONABLE_OVERFLOW_ROWS:
        errors.append("overflow.json actionable_overflow_rows drifted")
    blob = str(overflow)
    if "programmed_circuit" in blob:
        errors.append("overflow must not invent programmed_circuit stand-ins")
    buckets = summary.get("buckets") or {}
    if int(buckets.get("ignore_unobtainable_plant_gt") or 0) != IGNORED_PLANT_GT_ROWS:
        errors.append("overflow must keep ignored unobtainable plant_gt_* rows")
    if int(buckets.get("shadow") or 0) != 0:
        errors.append(
            "1.7.10 fish damage-split must not remain a shadowed input signature"
        )
    policy = summary.get("ignore_policy") or {}
    if policy.get("bucket") != IGNORE_POLICY["bucket"]:
        errors.append("squeezer_overflow ignore_policy bucket drifted")
    if policy.get("future_reclaim") != IGNORE_POLICY["future_reclaim"]:
        errors.append("squeezer_overflow must keep crop-breeding reclaim path")
    if policy.get("this_card") != IGNORE_POLICY["this_card"]:
        errors.append("squeezer_overflow this_card policy drifted")
    if "never_import" in summary or "never_import" in overflow:
        errors.append("plant_gt_* overflow must not be frozen as never_import")
    ignored_rows = [
        row
        for row in (overflow.get("overflow") or [])
        if row.get("disposition") == "ignore"
    ]
    if len(ignored_rows) != IGNORED_PLANT_GT_ROWS:
        errors.append("overflow ignored disposition count drifted")
    if any(row.get("status") != "ignored" for row in ignored_rows):
        errors.append("ignored plant_gt_* rows must use status ignored")
    latex = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "recipe" / "machine" / "squeezer"
    for name in (
        "rubber_resin.json",
        "rubber_leaves.json",
        "rubber_sapling.json",
        "wood_rubber_dust.json",
    ):
        if not (latex / name).is_file():
            errors.append(f"missing Java latex remainder {name}")

    try:
        errors.extend(source_import.check_import(WAVE / "recipe_import.json"))
    except Exception as error:
        errors.append(f"import: {error}")

    expected_lock = common.pilot_lock(WAVE, IMPORT_SLUG, LOCK_NOTE)
    actual_lock = census.load_json(WAVE / "production_lock.json")
    if actual_lock != expected_lock:
        errors.append("production_lock drifted from reviewed lock_candidate")
    if actual_lock.get("production_authority") is not True:
        errors.append("production_lock must keep production_authority")
    if "not player_complete" not in str(actual_lock.get("note") or ""):
        errors.append("production_lock must not claim player_complete")
    if actual_lock.get("production", {}).get("relation_count") != SELECTED_ROWS:
        errors.append(f"production_lock relation_count must be {SELECTED_ROWS}")

    topology = census.load_json(WAVE / "topology.json")
    readiness = census.load_json(WAVE / "readiness.json")
    expected_wave = unique_active_wave()
    if topology.get("unique_active_wave") != expected_wave:
        errors.append("topology unique_active_wave drifted from capability workflow")
    if topology.get("remaining_recipe_gap") != ACTIONABLE_OVERFLOW_ROWS:
        errors.append("topology remaining_recipe_gap must exclude ignored plant_gt_*")
    if topology.get("ignored_rows") != IGNORED_PLANT_GT_ROWS:
        errors.append("topology ignored_rows drifted")
    if readiness.get("unique_active_wave") != expected_wave:
        errors.append("readiness unique_active_wave drifted from capability workflow")

    later = census.load_json(LATER_WAVE)
    phases = {
        row.get("phase"): row
        for row in (later.get("delta_ledger_policy") or {}).get("registered_deltas") or []
    }
    dump_delta = phases.get("squeezer_dump") or {}
    if int(dump_delta.get("logical") or 0) != SELECTED_ROWS:
        errors.append("later_wave squeezer_dump delta drifted")

    if errors:
        return errors
    try:
        common.isolated_compile(WAVE, LIVE_NEEDLE)
    except Exception as error:
        errors.append(f"isolated compile: {error}")
    try:
        spec = recipe_wave(IMPORT_SLUG)
        if spec.path_prefix != "squeezer":
            errors.append(f"derived path_prefix {spec.path_prefix!r} != squeezer")
        built = compile_mod.compile_wave(IMPORT_SLUG)
        relation_count = int(built["report"].get("relation_count") or 0)
        if relation_count != SELECTED_ROWS:
            errors.append(
                f"live compile relation_count {relation_count} != {SELECTED_ROWS}"
            )
        generated = live_family_files()
        if not generated:
            errors.append("live squeezer tree is missing compact family")
        assembled = reassemble_documents(
            [census.load_json(path) for path in generated]
        )
        relations = authored_relations(assembled)
        if len(relations) != SELECTED_ROWS:
            errors.append("live squeezer authored relations drifted")
        compact_items = {
            str(((row.get("item_inputs") or [{}])[0] or {}).get("item") or "")
            for row in relations
        }
        if "minecraft:fish" in compact_items:
            errors.append("compact family still uses unsplit minecraft:fish")
        if "minecraft:salmon" not in compact_items:
            errors.append("compact family missing minecraft:salmon squeezer row")
        if "minecraft:tropical_fish" not in compact_items:
            errors.append(
                "compact family missing minecraft:tropical_fish squeezer row"
            )
        try:
            expected = expected_publication_policy()
        except Exception as error:
            errors.append(f"publication policy: {error}")
        else:
            if not POLICY_PATH.is_file():
                errors.append("missing compact publication policy squeezer.json")
            elif census.load_json(POLICY_PATH) != expected:
                errors.append("publication policy drifted from live compact family")
    except Exception as error:
        errors.append(f"live compile: {error}")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        description="Squeezer dump import on hammer-squeezer-laser"
    )
    modes = parser.add_mutually_exclusive_group(required=True)
    modes.add_argument("--write", action="store_true")
    modes.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write:
        report = write()
        print(
            "Wrote squeezer dump: "
            f"selected={report['source']['selected_rows']} "
            f"overflow={report['source']['overflow_rows']} "
            f"live={report['live'].get('relation_count')}"
        )
        return 0
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("squeezer dump is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
