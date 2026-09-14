#!/usr/bin/env python3
"""Cross-capability blocker ledger. Counts stay in separate denominators."""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from pathlib import Path
from typing import Any

from tools import atomic_io
from tools import capability_ledger
from tools import io_common as io

CATALOG = io.TOOLS / "blockers" / "catalog.json"
LEDGER = io.TOOLS / "blockers" / "ledger.json"
RECIPE_LEDGER = io.TOOLS / "blocked_recipe_ledger.json"
BATCHES = io.TOOLS / "blockers" / "batches.json"
MARKDOWN = io.ROOT / "docs" / "current" / "blocked.md"
CLOSED_PLANS = io.ROOT / "docs" / "history" / "card-plans" / "closed"
STATUSES = ("open", "partial", "resolved", "superseded", "out_of_scope")
MATURITY = ("player_complete", "runtime_ready", "none")
OPEN_STATUSES = frozenset({"open", "partial"})
PLANNING_BUCKETS = (
    "scale_not_todo",
    "schedulable",
    "audit_first",
    "not_work",
)
PLANNING_LABELS = {
    "scale_not_todo": "A. 数字是规模，不是待办",
    "schedulable": "B. 分母已冻，可当卡排",
    "audit_first": "C. 有名字，分母未冻成工作量",
    "not_work": "D. 不是活",
}
OBTAIN_MARKER = "Obtain stays explicitly_blocked"
REQUIRED = (
    "id",
    "title",
    "root_cause",
    "root_cause_class",
    "status",
    "count",
    "unit",
    "blocks_maturity",
    "discovered_by",
    "introduced_by",
    "resolved_by",
    "affected_capabilities",
    "claimed_by",
    "authority",
    "no_stand_in",
    "planning_bucket",
)
ID_PREFIX = (
    "recipe/",
    "material-form/",
    "fluid/",
    "identity/",
    "obtain/",
    "worldgen/",
    "energy/",
    "cover/",
    "peripheral/",
    "storage/",
    "tools/",
    "architecture/",
    "historical/",
)
BATCH_KINDS = frozenset(
    {
        "domain_fluid_closure",
        "machine_closure",
        "recipe_wave",
        "shared_cover_host_audit",
        "shared_identity_audit",
        "shared_obtain_audit",
    }
)
BATCH_ROLES = frozenset({"primary", "audit_only", "scale_context"})


def dumps(document: Any) -> bytes:
    return (
        json.dumps(document, indent=2, sort_keys=True, ensure_ascii=False) + "\n"
    ).encode("utf-8")


def load_catalog() -> dict[str, Any]:
    if not CATALOG.is_file():
        raise ValueError("missing tools/blockers/catalog.json")
    return io.load_json(CATALOG)


def load_batches() -> dict[str, Any]:
    if not BATCHES.is_file():
        raise ValueError("missing tools/blockers/batches.json")
    return io.load_json(BATCHES)


def recipe_ledger_errors(catalog: dict[str, Any] | None = None) -> list[str]:
    if not RECIPE_LEDGER.is_file():
        return ["missing tools/blocked_recipe_ledger.json"]
    document = io.load_json(RECIPE_LEDGER)
    entries = {
        str(row.get("id") or ""): row
        for row in list((catalog if catalog is not None else load_catalog()).get("entries") or [])
    }
    counts = document.get("counts", {}).get("blocked_families", {})
    errors: list[str] = []
    for count_key, blocker_id in {
        "bath_remainder": "recipe/bath-remainder-families",
        "bath_identity": "recipe/bath-identity-families",
    }.items():
        row = entries.get(blocker_id)
        if row is None:
            errors.append(f"{blocker_id}: missing from blocker catalog")
            continue
        count = int(counts.get(count_key, -1))
        if count < 0:
            errors.append(
                f"{blocker_id}: recipe ledger is missing blocked_families.{count_key}"
            )
        elif count == 0 and row["status"] in OPEN_STATUSES:
            errors.append(
                f"{blocker_id}: catalog is open but current recipe ledger is closed"
            )
        elif count > 0 and row["status"] not in OPEN_STATUSES:
            errors.append(
                f"{blocker_id}: catalog is closed but current recipe ledger is open"
            )
    return errors


def _closed_plan_ids() -> set[str]:
    if not CLOSED_PLANS.is_dir():
        return set()
    return {path.stem for path in CLOSED_PLANS.glob("*.md")}


def batch_errors(
    *,
    catalog: dict[str, Any] | None = None,
    batches: dict[str, Any] | None = None,
) -> list[str]:
    catalog_document = catalog if catalog is not None else load_catalog()
    batch_document = batches if batches is not None else load_batches()
    errors: list[str] = []
    if batch_document.get("schema_version") != 1:
        errors.append("blocker batches schema_version must be 1")
    if batch_document.get("ordering_is_not_additive") is not True:
        errors.append("blocker batches must declare ordering_is_not_additive")
    entries = list(catalog_document.get("entries") or [])
    by_id = {str(row.get("id") or ""): row for row in entries}
    batch_rows = list(batch_document.get("batches") or [])
    batch_ids = [str(row.get("id") or "") for row in batch_rows]
    if len(batch_ids) != len(set(batch_ids)):
        errors.append("duplicate blocker batch id")
    known_batch_ids = set(batch_ids)
    closed_ids: set[str] = set()
    for record in batch_document.get("closed_before_batch_selection") or []:
        blocker_ids = list(record.get("blocker_ids") or [])
        if not blocker_ids:
            errors.append("closed_before_batch_selection record needs blocker_ids")
        for blocker_id in blocker_ids:
            if blocker_id in closed_ids:
                errors.append(
                    f"closed_before_batch_selection duplicates {blocker_id}"
                )
            closed_ids.add(blocker_id)
            if blocker_id not in by_id:
                errors.append(
                    f"closed_before_batch_selection unknown blocker {blocker_id}"
                )
            elif by_id[blocker_id]["status"] in OPEN_STATUSES:
                errors.append(
                    f"closed_before_batch_selection still open: {blocker_id}"
                )
        if not str(record.get("resolution") or "").strip():
            errors.append("closed_before_batch_selection record needs resolution")
        authorities = list(record.get("authority") or [])
        if not authorities:
            errors.append("closed_before_batch_selection record needs authority")
        for path in authorities:
            if not isinstance(path, str) or not path.strip():
                errors.append("closed_before_batch_selection authority needs paths")
            elif Path(path).is_absolute() or path.startswith("../"):
                errors.append(
                    "closed_before_batch_selection authority must be repo-relative"
                )
            elif not (io.ROOT / path).is_file():
                errors.append(
                    f"closed_before_batch_selection authority is missing: {path}"
                )
    seen_members: dict[str, str] = {}
    for batch in batch_rows:
        batch_id = str(batch.get("id") or "")
        if not batch_id.startswith("batch/"):
            errors.append(f"{batch_id}: batch id must start with batch/")
        if not str(batch.get("title") or "").strip():
            errors.append(f"{batch_id}: batch title is required")
        if batch.get("kind") not in BATCH_KINDS:
            errors.append(f"{batch_id}: bad batch kind {batch.get('kind')!r}")
        if batch.get("does_not_merge_counts") is not True:
            errors.append(f"{batch_id}: does_not_merge_counts must be true")
        if batch.get("auto_promote_player_complete") is not False:
            errors.append(f"{batch_id}: auto_promote_player_complete must be false")
        if not str(batch.get("production_lock_policy") or "").strip():
            errors.append(f"{batch_id}: production_lock_policy is required")
        for path in batch.get("production_locks") or []:
            if not isinstance(path, str) or not path.strip():
                errors.append(f"{batch_id}: production_locks must contain paths")
            elif Path(path).is_absolute() or path.startswith("../"):
                errors.append(f"{batch_id}: production lock path must be repo-relative")
            elif not (io.ROOT / path).is_file():
                errors.append(f"{batch_id}: production lock is missing: {path}")
        members = list(batch.get("members") or [])
        if not members:
            errors.append(f"{batch_id}: members are required")
        member_ids: set[str] = set()
        for member in members:
            blocker_id = str(member.get("blocker_id") or "")
            role = member.get("role")
            if blocker_id in member_ids:
                errors.append(f"{batch_id}: duplicate member {blocker_id}")
            member_ids.add(blocker_id)
            if blocker_id not in by_id:
                errors.append(f"{batch_id}: unknown member {blocker_id}")
                continue
            if role not in BATCH_ROLES:
                errors.append(f"{batch_id} {blocker_id}: bad member role {role!r}")
            if by_id[blocker_id]["status"] not in OPEN_STATUSES:
                errors.append(
                    f"{batch_id} {blocker_id}: batch members must be open or partial"
                )
            if blocker_id in seen_members:
                errors.append(
                    f"{blocker_id}: appears in batches "
                    f"{seen_members[blocker_id]} and {batch_id}"
                )
            else:
                seen_members[blocker_id] = batch_id
            if role == "scale_context" and by_id[blocker_id].get(
                "planning_bucket"
            ) != "scale_not_todo":
                errors.append(
                    f"{batch_id} {blocker_id}: scale_context must remain "
                    "planning_bucket scale_not_todo"
                )
        excluded = list(batch.get("excluded_blocker_ids") or [])
        if len(excluded) != len(set(excluded)):
            errors.append(f"{batch_id}: duplicate excluded blocker id")
        for blocker_id in excluded:
            if blocker_id not in by_id:
                errors.append(f"{batch_id}: unknown excluded blocker {blocker_id}")
            elif blocker_id in member_ids:
                errors.append(
                    f"{batch_id}: blocker cannot be both member and excluded: {blocker_id}"
                )
        if not list(batch.get("boundary_notes") or []):
            errors.append(f"{batch_id}: boundary_notes are required")
    recommendations = list(batch_document.get("recommended_order") or [])
    recommendation_ids = [
        str(row.get("batch_id") or "") for row in recommendations
    ]
    if recommendation_ids != list(dict.fromkeys(recommendation_ids)):
        errors.append("recommended_order contains duplicate batch ids")
    if set(recommendation_ids) != known_batch_ids:
        errors.append("recommended_order must contain every blocker batch exactly once")
    for row in recommendations:
        batch_id = str(row.get("batch_id") or "")
        if not str(row.get("basis") or "").strip():
            errors.append(f"{batch_id}: recommended order basis is required")
    return errors


def _entry_errors(entry: dict[str, Any], known_slugs: set[str]) -> list[str]:
    errors: list[str] = []
    missing = [key for key in REQUIRED if key not in entry]
    if missing:
        return [f"{entry.get('id', '<unknown>')}: missing {', '.join(missing)}"]
    ident = str(entry["id"])
    if not ident or "/" not in ident:
        errors.append(f"{ident}: id must be class/name")
    if not ident.startswith(ID_PREFIX):
        errors.append(f"{ident}: id prefix is not a known class")
    if entry["status"] not in STATUSES:
        errors.append(f"{ident}: bad status {entry['status']!r}")
    if entry["blocks_maturity"] not in MATURITY:
        errors.append(f"{ident}: bad blocks_maturity {entry['blocks_maturity']!r}")
    bucket = str(entry.get("planning_bucket") or "")
    if bucket not in PLANNING_BUCKETS:
        errors.append(f"{ident}: bad planning_bucket {bucket!r}")
    if entry["status"] not in OPEN_STATUSES and bucket != "not_work":
        errors.append(f"{ident}: closed/out_of_scope rows must use planning_bucket not_work")
    count = entry["count"]
    if count is not None and (not isinstance(count, int) or count < 0):
        errors.append(f"{ident}: count must be null or a non-negative int")
    if count is not None and not str(entry["unit"] or "").strip():
        errors.append(f"{ident}: count requires a unit")
    if entry["status"] in {"resolved", "superseded"}:
        if not entry.get("resolved_by"):
            errors.append(f"{ident}: {entry['status']} requires resolved_by")
    elif entry.get("resolved_by"):
        errors.append(f"{ident}: open/partial/out_of_scope cannot set resolved_by")
    resolved = entry.get("resolved_by")
    if resolved and resolved not in known_slugs and resolved not in _closed_plan_ids():
        errors.append(f"{ident}: unknown resolved_by {resolved}")
    if not str(entry["no_stand_in"] or "").strip():
        errors.append(f"{ident}: no_stand_in is required")
    if not list(entry.get("authority") or []):
        errors.append(f"{ident}: authority is required")
    for slug in entry.get("affected_capabilities") or []:
        if slug not in known_slugs and not str(slug).startswith("prep:"):
            errors.append(f"{ident}: unknown affected capability {slug}")
    for claim in entry.get("claimed_by") or []:
        slug = str(claim.get("slug") or "")
        key = claim.get("semantic_key")
        marker = claim.get("note_marker")
        extra = set(claim) - {"slug", "semantic_key", "note_marker"}
        if extra:
            errors.append(f"{ident}: claimed_by extra keys {sorted(extra)}")
        if not slug:
            errors.append(f"{ident}: claimed_by missing slug")
        elif slug not in known_slugs:
            errors.append(f"{ident}: claimed_by unknown slug {slug}")
        if bool(key) == bool(marker):
            errors.append(
                f"{ident}: claimed_by {slug} needs exactly one of "
                "semantic_key or note_marker"
            )
    return errors


def catalog_errors(catalog: dict[str, Any] | None = None) -> list[str]:
    document = catalog if catalog is not None else load_catalog()
    errors: list[str] = []
    if document.get("schema_version") != 1:
        errors.append("blockers catalog schema_version must be 1")
    entries = list(document.get("entries") or [])
    ids = [str(row.get("id") or "") for row in entries]
    if len(ids) != len(set(ids)):
        errors.append("duplicate blocker id")
    known = {row["slug"] for row in _capability_documents()}
    for entry in entries:
        errors.extend(_entry_errors(entry, known))
    errors.extend(recipe_ledger_errors(document))
    return errors


def _capability_documents() -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for path in capability_ledger.capability_files():
        rows.append(capability_ledger.load_capability(path))
    return rows


def _index_claims(
    entries: list[dict[str, Any]],
) -> tuple[dict[tuple[str, str], list[str]], dict[tuple[str, str], list[str]]]:
    by_key: dict[tuple[str, str], list[str]] = {}
    by_note: dict[tuple[str, str], list[str]] = {}
    for entry in entries:
        ident = str(entry["id"])
        for claim in entry.get("claimed_by") or []:
            slug = str(claim["slug"])
            if claim.get("semantic_key"):
                by_key.setdefault((slug, str(claim["semantic_key"])), []).append(ident)
            if claim.get("note_marker"):
                by_note.setdefault((slug, str(claim["note_marker"])), []).append(ident)
    return by_key, by_note


def binding_errors(
    *,
    catalog: dict[str, Any] | None = None,
    capabilities: list[dict[str, Any]] | None = None,
    scope_slug: str | None = None,
) -> list[str]:
    document = catalog if catalog is not None else load_catalog()
    caps = capabilities if capabilities is not None else _capability_documents()
    entries = list(document.get("entries") or [])
    catalog_ids = {str(row["id"]) for row in entries}
    by_key, by_note = _index_claims(entries)
    errors: list[str] = []
    seen_keys: set[tuple[str, str]] = set()
    for capability in caps:
        slug = capability["slug"]
        note = str(capability.get("note") or "")
        for row in capability.get("identity_disposition") or []:
            if row.get("disposition") != "blocked":
                continue
            key = str(row.get("semantic_key") or "")
            seen_keys.add((slug, key))
            declared = [str(item) for item in row.get("blocker_ids") or []]
            claimed = list(by_key.get((slug, key)) or [])
            for ident in declared:
                if ident not in catalog_ids:
                    errors.append(f"{slug} {key}: unknown blocker_ids {ident}")
            if declared and claimed and sorted(declared) != sorted(claimed):
                errors.append(
                    f"{slug} {key}: blocker_ids {declared} disagree with catalog {claimed}"
                )
            if not declared and not claimed:
                errors.append(
                    f"{slug} {key}: blocked identity is not on the blocker ledger"
                )
        if OBTAIN_MARKER in note and (slug, OBTAIN_MARKER) not in by_note:
            errors.append(f"{slug}: {OBTAIN_MARKER} is not on the blocker ledger")
    for (claim_slug, marker), idents in by_note.items():
        if scope_slug is not None and claim_slug != scope_slug:
            continue
        capability = next((row for row in caps if row["slug"] == claim_slug), None)
        if capability is None:
            if scope_slug is None:
                errors.append(
                    f"{idents[0]}: note_marker slug {claim_slug} is not a capability"
                )
            continue
        if marker not in str(capability.get("note") or ""):
            errors.append(
                f"{claim_slug}: note_marker {marker!r} claimed by {idents} is missing"
            )
    for pair, idents in by_key.items():
        if len(idents) != 1:
            errors.append(f"{pair[0]} {pair[1]}: claimed by {idents}")
        if scope_slug is not None and pair[0] != scope_slug:
            continue
        if pair not in seen_keys:
            errors.append(
                f"{idents[0]}: claimed {pair[0]} {pair[1]} but that blocked row is gone"
            )
    return errors


def check_capability_bindings(capability: dict[str, Any]) -> list[str]:
    return binding_errors(
        capabilities=[capability],
        scope_slug=str(capability.get("slug") or ""),
    )


def compile_ledger(catalog: dict[str, Any] | None = None) -> dict[str, Any]:
    document = catalog if catalog is not None else load_catalog()
    batch_document = load_batches()
    entries = list(document.get("entries") or [])
    status_counts = Counter(str(row["status"]) for row in entries)
    classes: dict[str, list[str]] = {}
    open_amounts: list[dict[str, Any]] = []
    planning: dict[str, list[dict[str, Any]]] = {name: [] for name in PLANNING_BUCKETS}
    planning_counts: dict[str, int] = {name: 0 for name in PLANNING_BUCKETS}
    for row in entries:
        if row["status"] not in OPEN_STATUSES:
            continue
        bucket = str(row["planning_bucket"])
        planning_counts[bucket] = planning_counts.get(bucket, 0) + 1
        classes.setdefault(str(row["root_cause_class"]), []).append(str(row["id"]))
        planning.setdefault(bucket, []).append(
            {
                "count": row["count"],
                "id": str(row["id"]),
                "unit": str(row["unit"] or ""),
            }
        )
        if row["count"] is None:
            continue
        open_amounts.append(
            {
                "count": int(row["count"]),
                "id": str(row["id"]),
                "planning_bucket": bucket,
                "unit": str(row["unit"]),
            }
        )
    for bucket in PLANNING_BUCKETS:
        planning[bucket] = sorted(planning[bucket], key=lambda row: row["id"])
    return {
        "batch_relations": batch_document,
        "do_not_add": True,
        "entries": entries,
        "generated_by": "python tools/build_blockers.py --write",
        "open_amounts": sorted(open_amounts, key=lambda row: row["id"]),
        "open_ids_by_root_cause_class": {
            key: sorted(value) for key, value in sorted(classes.items())
        },
        "planning": planning,
        "planning_counts": planning_counts,
        "schema_version": 1,
        "source_revision": io.SOURCE_REVISION,
        "stats": {
            "open": int(status_counts["open"]),
            "out_of_scope": int(status_counts["out_of_scope"]),
            "partial": int(status_counts["partial"]),
            "resolved": int(status_counts["resolved"]),
            "superseded": int(status_counts["superseded"]),
            "total": len(entries),
        },
    }


def render_markdown(ledger: dict[str, Any] | None = None) -> str:
    document = ledger if ledger is not None else compile_ledger()
    stats = document["stats"]
    lines = [
        "# Blocker 总账",
        "",
        "> 本页由 `python tools/build_blockers.py --write` 从",
        "> `tools/blockers/catalog.json` 与 `tools/blockers/batches.json` 生成，不要手改。",
        "> 条目权威是 catalog，批处理关系权威是 batches；本页和 `tools/blockers/ledger.json` 都是投影。",
        "> 不同条目、不同 `unit` **不得相加**。发现旧缺口不是任务制造了缺口。",
        "> `count` 不是剩余工作量。排期看 `planning_bucket`，不要按数字选最大的卡。",
        "> 选批只读 current recipe ledger / catalog / batches；一致性失败时先停排期，历史 candidate selection 不能重新打开已关闭条目。",
        "",
        "## 统计",
        "",
        f"- 条目 {stats['total']}：open {stats['open']}，partial {stats['partial']}，"
        f"resolved {stats['resolved']}，superseded {stats['superseded']}，"
        f"out_of_scope {stats['out_of_scope']}",
    ]
    planning_counts = document.get("planning_counts") or {}
    lines.append(
        "- 未关闭排期桶："
        + "，".join(
            f"{PLANNING_LABELS[name].split('. ', 1)[1]} {planning_counts.get(name, 0)}"
            for name in PLANNING_BUCKETS
        )
    )
    lines.extend(
        [
            "",
            "## 排期分类（未关闭）",
            "",
            "A 的整数是 dump/shadow/未核实规模。B 才是可以抽 unique-active 的冻结核。",
            "C 先审计分母。D 不是任务。同类条目仍不得相加。",
            "",
        ]
    )
    planning = document.get("planning") or {}
    for name in PLANNING_BUCKETS:
        rows = list(planning.get(name) or [])
        lines.append(f"### {PLANNING_LABELS[name]}（{len(rows)}）")
        lines.append("")
        if not rows:
            lines.append("无。")
            lines.append("")
            continue
        for row in rows:
            count = row["count"]
            amount = "n/a" if count is None else f"{count} {row['unit']}"
            lines.append(f"- `{row['id']}`：{amount}")
        lines.append("")
    batch_document = document.get("batch_relations") or {}
    batches_by_id = {
        str(row["id"]): row for row in batch_document.get("batches") or []
    }
    closed_records = batch_document.get("closed_before_batch_selection") or []
    lines.extend(
        [
            "## 批处理前已关闭的条目",
            "",
            "这些条目保留在 catalog 作为历史结算，但不进入未关闭批次；"
            "candidate selection 的旧 blocked 数不能覆盖后继卡的 current closeout。",
            "",
        ]
    )
    for record in closed_records:
        members = ", ".join(
            f"`{blocker_id}`" for blocker_id in record["blocker_ids"]
        )
        lines.append(f"- 成员：{members}")
        lines.append(f"  - 收口：{record['resolution']}")
        lines.append(
            "  - 当前权威："
            + ", ".join(f"`{path}`" for path in record["authority"])
        )
    lines.append("")
    lines.extend(
        [
            "## 批处理关系（不是分母）",
            "",
            "以下只登记共享审计、Source Pack 或验收流程；不同 `unit`、不同 production lock "
            "仍分别核算，批次不会自动晋级 `player_complete`。",
            "",
        ]
    )
    for recommendation in batch_document.get("recommended_order") or []:
        batch_id = str(recommendation["batch_id"])
        batch = batches_by_id[batch_id]
        lines.extend(
            [
                f"### `{batch_id}`：{batch['title']}",
                "",
                f"- 建议排序依据：{recommendation['basis']}",
                f"- 类型：`{batch['kind']}`；成员角色按各 blocker 保留",
                f"- production lock：`{batch['production_lock_policy']}`",
                "- 成员："
            ]
        )
        for member in batch.get("members") or []:
            lines.append(
                f"  - `{member['blocker_id']}`（`{member['role']}`）"
            )
        locks = batch.get("production_locks") or []
        if locks:
            lines.append("- 已有 lock：")
            lines.extend(f"  - `{path}`" for path in locks)
        else:
            lines.append("- 已有 lock：无（按能力/流体身份分别验收）")
        excluded = batch.get("excluded_blocker_ids") or []
        if excluded:
            lines.append("- 明确排除：")
            lines.extend(f"  - `{blocker_id}`" for blocker_id in excluded)
        lines.append("- 边界：")
        lines.extend(f"  - {note}" for note in batch.get("boundary_notes") or [])
        lines.append("")
    lines.extend(["## 按根因（未关闭）", ""])
    classes = document.get("open_ids_by_root_cause_class") or {}
    if classes:
        for klass, idents in classes.items():
            lines.append(f"- `{klass}`（{len(idents)}）")
            for ident in idents:
                lines.append(f"  - `{ident}`")
    else:
        lines.append("无。")
    lines.extend(["", "## 条目", ""])
    status_order = {name: index for index, name in enumerate(STATUSES)}
    entries = sorted(
        document["entries"],
        key=lambda row: (status_order[row["status"]], row["id"]),
    )
    for row in entries:
        count = row["count"]
        amount = "n/a" if count is None else f"{count} {row['unit']}"
        introduced = row["introduced_by"] or "否（发现既有缺口）"
        resolved = row["resolved_by"] or "—"
        affected = row["affected_capabilities"] or []
        lines.extend(
            [
                f"### `{row['id']}`",
                "",
                f"- 标题：{row['title']}",
                f"- 状态：`{row['status']}`",
                f"- 根因：`{row['root_cause_class']}` / `{row['root_cause']}`",
                f"- 数量：{amount}",
                f"- 排期：`{row['planning_bucket']}`",
                f"- 挡住：`{row['blocks_maturity']}`",
                f"- 发现卡：`{row['discovered_by']}`",
                f"- 由本卡引入：{introduced}",
                f"- 解决卡：{resolved}",
                f"- 影响：{', '.join(f'`{slug}`' for slug in affected) or '—'}",
                f"- 权威：{', '.join(f'`{path}`' for path in row['authority'])}",
                f"- 禁止 stand-in：{row['no_stand_in']}",
            ]
        )
        if row.get("note"):
            lines.append(f"- 说明：{row['note']}")
        lines.append("")
    lines.extend(
        [
            "## 怎么用",
            "",
            "关 `runtime_ready` / `player_complete` 时，能力上每一条",
            "`disposition=blocked` 或「Obtain stays explicitly_blocked」必须绑定本账的 `id`。",
            "新缺口写进 `tools/blockers/catalog.json` 再 `--write`，并填 `planning_bucket`。",
            "修根因时按 `root_cause_class` 集中收口。从 B 抽卡；A 按根因切片，不要按行数选最大。",
            "",
        ]
    )
    return "\n".join(lines)


def check() -> list[str]:
    errors = catalog_errors()
    errors.extend(batch_errors())
    if errors:
        return errors
    compiled = compile_ledger()
    errors.extend(binding_errors(catalog={"entries": compiled["entries"]}))
    expected_ledger = dumps(compiled).decode("utf-8")
    actual_ledger = LEDGER.read_text(encoding="utf-8") if LEDGER.is_file() else ""
    if actual_ledger != expected_ledger:
        errors.append(io.stale_error(LEDGER, expected_ledger, actual_ledger))
    expected_md = render_markdown(compiled)
    actual_md = MARKDOWN.read_text(encoding="utf-8") if MARKDOWN.is_file() else ""
    if actual_md != expected_md:
        errors.append(io.stale_error(MARKDOWN, expected_md, actual_md))
    return errors


def write() -> dict[str, Any]:
    errors = catalog_errors()
    errors.extend(batch_errors())
    if errors:
        raise ValueError("; ".join(errors))
    compiled = compile_ledger()
    bind = binding_errors(catalog={"entries": compiled["entries"]})
    if bind:
        raise ValueError("; ".join(bind))
    atomic_io.write_bytes(LEDGER, dumps(compiled))
    atomic_io.write_text(MARKDOWN, render_markdown(compiled))
    return {
        "entries": compiled["stats"]["total"],
        "open": compiled["stats"]["open"],
        "partial": compiled["stats"]["partial"],
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("blocker ledger is current")
            return 0
        if args.write:
            print(json.dumps(write(), indent=2))
            return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"blockers failed: {error}", file=sys.stderr)
        return 1
    parser.print_help()
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
