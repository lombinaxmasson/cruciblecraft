#!/usr/bin/env python3
"""Cross-capability blocker ledger. Counts stay in separate denominators."""
from __future__ import annotations

import argparse
import json
import sys
from collections import Counter
from typing import Any

from tools import atomic_io
from tools import capability_ledger
from tools import io_common as io

CATALOG = io.TOOLS / "blockers" / "catalog.json"
LEDGER = io.TOOLS / "blockers" / "ledger.json"
MARKDOWN = io.ROOT / "docs" / "current" / "blocked.md"
STATUSES = ("open", "partial", "resolved", "superseded", "out_of_scope")
MATURITY = ("player_complete", "runtime_ready", "none")
OPEN_STATUSES = frozenset({"open", "partial"})
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


def dumps(document: Any) -> bytes:
    return (
        json.dumps(document, indent=2, sort_keys=True, ensure_ascii=False) + "\n"
    ).encode("utf-8")


def load_catalog() -> dict[str, Any]:
    if not CATALOG.is_file():
        raise ValueError("missing tools/blockers/catalog.json")
    return io.load_json(CATALOG)


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
    if resolved and resolved not in known_slugs:
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
    entries = list(document.get("entries") or [])
    status_counts = Counter(str(row["status"]) for row in entries)
    classes: dict[str, list[str]] = {}
    open_amounts: list[dict[str, Any]] = []
    for row in entries:
        if row["status"] not in OPEN_STATUSES:
            continue
        classes.setdefault(str(row["root_cause_class"]), []).append(str(row["id"]))
        if row["count"] is None:
            continue
        open_amounts.append(
            {
                "count": int(row["count"]),
                "id": str(row["id"]),
                "unit": str(row["unit"]),
            }
        )
    return {
        "do_not_add": True,
        "entries": entries,
        "generated_by": "python tools/build_blockers.py --write",
        "open_amounts": sorted(open_amounts, key=lambda row: row["id"]),
        "open_ids_by_root_cause_class": {
            key: sorted(value) for key, value in sorted(classes.items())
        },
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
        "> `tools/blockers/catalog.json` 生成，不要手改。",
        "> 权威是 catalog；本页和 `tools/blockers/ledger.json` 都是投影。",
        "> 不同条目、不同 `unit` **不得相加**。发现旧缺口不是任务制造了缺口。",
        "",
        "## 统计",
        "",
        f"- 条目 {stats['total']}：open {stats['open']}，partial {stats['partial']}，"
        f"resolved {stats['resolved']}，superseded {stats['superseded']}，"
        f"out_of_scope {stats['out_of_scope']}",
        "- 未关闭数量按条目列出（不得相加）：",
    ]
    amounts = document.get("open_amounts") or []
    if amounts:
        for row in amounts:
            lines.append(f"  - `{row['id']}`：{row['count']} {row['unit']}")
    else:
        lines.append("  - 无")
    lines.extend(["", "## 按根因（未关闭）", ""])
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
            "新缺口写进 `tools/blockers/catalog.json` 再 `--write`。",
            "修根因时按 `root_cause_class` 集中收口，不要把 overflow 行数抄成待办。",
            "",
        ]
    )
    return "\n".join(lines)


def check() -> list[str]:
    errors = catalog_errors()
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
