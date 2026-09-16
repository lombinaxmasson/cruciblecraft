#!/usr/bin/env python3
"""Fail if active roots still use milestone-style TXX names.

Exempt only docs/history/card-plans/closed/**. Unique-active and prep plans in
docs/history/card-plans/active/** and card-plans/prep/** must not use milestone TXX names. Temporary allowlist
entries must carry owner, reason, and expiry. Program closeout requires an
empty allowlist. `--quick` is a manual audit of live logistics/verification
Java and unique-active plans. It is not a `verify.py` gate and does not scan
capability JSON.

Full --summary streams files with os.walk and scans line-by-line. It does not
build a complete Path list or slurp giant JSON into one string.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import sys
import time
from collections.abc import Iterator
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
ROOT_S = os.fspath(ROOT)

SCAN_ROOTS = (
    "src/main",
    "src/test",
    "src/generated",
    "src/ore_chain_generated",
    "src/worldgen_generated",
    "src/worldgen_catalog_generated",
    "src/component_rule_generated",
    "src/chemical_recipe_generated",
    "src/hydrocarbon_recipe_generated",
    "src/petroleum_recipe_generated",
    "src/compact_recipe_policy_generated",
    "src/recipe_generated",
    "src/recipe_support_generated",
    "tools",
    "docs/current",
    "docs/history",
    "docs/decisions",
    "src/recipeLoadBenchmark",
    "gradle",
)

SCAN_FILES = (
    "build.gradle",
    "gradle.properties",
    "README.md",
    "README.en.md",
    "docs/README.md",
)

TEXT_SUFFIXES = {
    ".java",
    ".py",
    ".json",
    ".gradle",
    ".properties",
    ".md",
    ".txt",
    ".kts",
    ".toml",
    ".yml",
    ".yaml",
}

SKIP_DIR_NAMES = {
    ".git",
    ".gradle",
    ".idea",
    "__pycache__",
    "build",
    "node_modules",
}

MILESTONE_PATTERNS: tuple[re.Pattern[str], ...] = (
    re.compile(r"\b[Tt]\d{1,2}\b"),
    re.compile(r"\b[Tt]\d{1,2}[._-]"),
    re.compile(r"(?:^|[^A-Za-z0-9])[Tt]\d{1,2}[A-Z]"),
    re.compile(r"(?:^|[^A-Za-z])[Tt]\d{1,2}[a-z]"),
    re.compile(r"[A-Za-z][Tt]\d{1,2}[A-Za-z]"),
    re.compile(r"[/\\]t\d{1,2}[/\\_]"),
    re.compile(r"cruciblecraft_t\d{1,2}", re.IGNORECASE),
    re.compile(r"-P[tT]\d{1,2}"),
    re.compile(r"portfolio:track_a/t\d{1,2}"),
    re.compile(r"next_issue_id[^.\n]{0,80}T\d{1,2}"),
)

FALSE_POSITIVE_SPAN = re.compile(
    r"cobalt60|TankBlock|ToolMaterial|TierProfile|"
    r"gt6[A-Za-z0-9_]*|Circuit T\d+|T\d+ dynamo",
    re.IGNORECASE,
)

# Cheap reject for the common case: no T/t + digit anywhere on the line.
_PREFILTER = re.compile(r"[Tt]\d")

# Program closeout requires this list to stay empty.
ALLOWLIST: list[dict[str, str]] = []


def relative(path: Path) -> str:
    return path.resolve().relative_to(ROOT).as_posix()


def is_exempt(rel: str) -> bool:
    return rel.startswith("docs/history/card-plans/closed/")


def is_allowlisted(rel: str, line: str, match: str) -> bool:
    for row in ALLOWLIST:
        if row.get("path") and row["path"] != rel:
            continue
        if row.get("match") and row["match"] not in (match, line):
            continue
        return True
    return False


def allowlist_errors() -> list[str]:
    errors: list[str] = []
    for index, row in enumerate(ALLOWLIST):
        missing = [
            field
            for field in ("owner", "reason", "expiry")
            if not str(row.get(field) or "").strip()
        ]
        if missing:
            errors.append(
                f"ALLOWLIST[{index}] missing {', '.join(missing)}"
            )
    return errors


def _spans_false_positive(line: str, start: int, end: int) -> bool:
    for match in FALSE_POSITIVE_SPAN.finditer(line):
        if match.start() <= start and end <= match.end():
            return True
    return False


def line_findings(line: str) -> list[str]:
    if not _PREFILTER.search(line):
        return []
    found: list[str] = []
    seen: set[str] = set()
    for pattern in MILESTONE_PATTERNS:
        for match in pattern.finditer(line):
            if _spans_false_positive(line, match.start(), match.end()):
                continue
            token = match.group(0)
            if token in seen:
                continue
            seen.add(token)
            found.append(token)
    return found


def scan_text(rel: str, text: str) -> list[dict[str, Any]]:
    findings: list[dict[str, Any]] = []
    for lineno, line in enumerate(text.splitlines(), start=1):
        for token in line_findings(line):
            if ALLOWLIST and is_allowlisted(rel, line, token):
                continue
            findings.append(
                {
                    "file": rel,
                    "line": lineno,
                    "match": token,
                    "text": line.strip()[:200],
                }
            )
    return findings


def _prefix_for(rel: str) -> str:
    if rel.startswith("src/"):
        return "/".join(rel.split("/")[:2])
    return rel.split("/")[0]


def _scan_line_tokens(
    rel: str, line: str, lineno: int, *, collect: bool
) -> tuple[int, list[dict[str, Any]]]:
    tokens = line_findings(line)
    if not tokens:
        return 0, []
    rows: list[dict[str, Any]] = []
    count = 0
    stripped = line.strip()[:200] if collect else ""
    for token in tokens:
        if ALLOWLIST and is_allowlisted(rel, line, token):
            continue
        count += 1
        if collect:
            rows.append(
                {
                    "file": rel,
                    "line": lineno,
                    "match": token,
                    "text": rel if lineno == 0 else stripped,
                }
            )
    return count, rows


def _scan_file_body(
    rel: str, full: str, *, collect: bool
) -> tuple[int, list[dict[str, Any]]]:
    count = 0
    rows: list[dict[str, Any]] = []
    try:
        with open(full, encoding="utf-8", errors="strict", newline="") as handle:
            for lineno, raw in enumerate(handle, start=1):
                line = raw.rstrip("\r\n")
                added, extra = _scan_line_tokens(
                    rel, line, lineno, collect=collect
                )
                count += added
                if extra:
                    rows.extend(extra)
    except (OSError, UnicodeDecodeError):
        return 0, []
    return count, rows


def _walk_dir(root_rel: str) -> Iterator[tuple[str, str]]:
    abs_root = os.path.join(ROOT_S, *root_rel.split("/"))
    if not os.path.isdir(abs_root):
        return
    skip = SKIP_DIR_NAMES
    suffixes = TEXT_SUFFIXES
    prune_card_plans = root_rel == "docs/history" or root_rel.startswith(
        "docs/history/"
    )
    for dirpath, dirnames, filenames in os.walk(abs_root, topdown=True, followlinks=False):
        dirnames[:] = [name for name in dirnames if name not in skip]
        if prune_card_plans:
            rel_dir = os.path.relpath(dirpath, ROOT_S).replace("\\", "/")
            if rel_dir == "docs/history" and "card-plans" in dirnames:
                dirnames.remove("card-plans")
        for name in filenames:
            suffix = os.path.splitext(name)[1].lower()
            if suffix not in suffixes:
                continue
            full = os.path.join(dirpath, name)
            rel = os.path.relpath(full, ROOT_S).replace("\\", "/")
            if is_exempt(rel):
                continue
            yield rel, full


def iter_scan_entries(*, quick: bool = False) -> Iterator[tuple[str, str]]:
    seen: set[str] = set()

    def emit(rel: str, full: str) -> Iterator[tuple[str, str]]:
        posix = rel.replace("\\", "/")
        if posix in seen or is_exempt(posix):
            return
        seen.add(posix)
        yield posix, full

    if quick:
        for prefix in QUICK_PREFIXES:
            abs_path = os.path.join(ROOT_S, *prefix.rstrip("/").split("/"))
            if os.path.isfile(abs_path):
                rel = prefix.replace("\\", "/").rstrip("/")
                yield from emit(rel, abs_path)
                continue
            if not os.path.isdir(abs_path):
                continue
            for rel, full in _walk_dir(prefix.rstrip("/")):
                yield from emit(rel, full)
        for name in sorted(QUICK_FILES):
            abs_path = os.path.join(ROOT_S, *name.split("/"))
            if os.path.isfile(abs_path):
                yield from emit(name.replace("\\", "/"), abs_path)
        return

    for raw in SCAN_ROOTS:
        abs_path = os.path.join(ROOT_S, *raw.split("/"))
        if os.path.isfile(abs_path):
            yield from emit(raw.replace("\\", "/"), abs_path)
            continue
        if not os.path.isdir(abs_path):
            continue
        for rel, full in _walk_dir(raw):
            yield from emit(rel, full)
    for name in SCAN_FILES:
        abs_path = os.path.join(ROOT_S, *name.split("/"))
        if os.path.isfile(abs_path):
            yield from emit(name.replace("\\", "/"), abs_path)
    try:
        names = os.listdir(ROOT_S)
    except OSError:
        names = []
    for name in names:
        if name.startswith("settings.gradle"):
            abs_path = os.path.join(ROOT_S, name)
            if os.path.isfile(abs_path):
                yield from emit(name.replace("\\", "/"), abs_path)


def iter_scan_files(*, quick: bool = False) -> Iterator[Path]:
    for _rel, full in iter_scan_entries(quick=quick):
        yield Path(full)


QUICK_PREFIXES = (
    "src/main/java/com/masson/cruciblecraft/logistics/",
    "src/main/java/com/masson/cruciblecraft/verification/",
    "docs/history/card-plans/active/",
    "docs/history/card-plans/prep/",
)

QUICK_FILES = {
    "src/main/java/com/masson/cruciblecraft/client/ClientSmoke.java",
    "src/main/java/com/masson/cruciblecraft/gametest/FluidNetworkCoreGameTests.java",
    "src/main/java/com/masson/cruciblecraft/content/blockentity/FluidPipeBlockEntity.java",
    "src/main/java/com/masson/cruciblecraft/compat/emi/CrucibleCraftEmiPlugin.java",
}

GENERATED_MARKERS = (
    "recipe_generated",
    "recipe_support_generated",
    "ore_chain_generated",
    "chemical_recipe_generated",
    "hydrocarbon_recipe_generated",
    "petroleum_recipe_generated",
    "compact_recipe_policy_generated",
    "worldgen_generated",
    "worldgen_catalog_generated",
    "component_rule_generated",
)


def is_quick_target(rel: str) -> bool:
    posix = rel.replace("\\", "/")
    if any(marker in posix for marker in GENERATED_MARKERS):
        return False
    if posix.endswith("/ledger.json"):
        return False
    if posix in QUICK_FILES:
        return True
    return posix.startswith(QUICK_PREFIXES)


def collect_findings(
    *, summary: bool = False, quick: bool = False
) -> tuple[int, list[dict[str, Any]], dict[str, int]]:
    findings: list[dict[str, Any]] = []
    by_prefix: dict[str, int] = {}
    count = 0
    scanned = 0
    last_progress = time.monotonic()
    collect = not summary
    for rel, full in iter_scan_entries(quick=quick):
        if quick and not is_quick_target(rel):
            continue
        scanned += 1
        file_count, rows = _scan_line_tokens(rel, rel, 0, collect=collect)
        body_count, body_rows = _scan_file_body(rel, full, collect=collect)
        file_count += body_count
        if collect:
            rows.extend(body_rows)
        if file_count:
            count += file_count
            prefix = _prefix_for(rel)
            by_prefix[prefix] = by_prefix.get(prefix, 0) + 1
            if collect:
                findings.extend(rows)
        if summary:
            now = time.monotonic()
            if now - last_progress >= 2.0:
                print(
                    f"summary progress: files={scanned} hit_files={sum(by_prefix.values())} "
                    f"findings={count} last={rel}",
                    file=sys.stderr,
                    flush=True,
                )
                last_progress = now
    return count, findings, by_prefix


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--limit", type=int, default=50)
    parser.add_argument("--summary", action="store_true")
    parser.add_argument(
        "--quick",
        action="store_true",
        help="Manual audit of live logistics/verification Java and unique-active plans.",
    )
    args = parser.parse_args(argv)
    errors = allowlist_errors()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    if args.summary:
        print("summary scan started", file=sys.stderr, flush=True)
    count, findings, by_prefix = collect_findings(
        summary=args.summary, quick=args.quick
    )
    shown = [] if args.summary else findings[: max(args.limit, 0)]
    if args.json:
        print(json.dumps(
            {
                "allowlist": ALLOWLIST,
                "count": count,
                "files": sum(by_prefix.values()),
                "by_prefix": dict(sorted(by_prefix.items(), key=lambda item: (-item[1], item[0]))),
                "findings": shown,
            },
            indent=2,
        ))
    else:
        for row in shown:
            print(f'{row["file"]}:{row["line"]}  {row["match"]}  {row["text"]}')
        if by_prefix:
            print("files_by_prefix")
            for prefix, n in sorted(by_prefix.items(), key=lambda item: (-item[1], item[0])):
                print(f"  {n:5d}  {prefix}")
        print(f"{count} milestone-name finding(s) in {sum(by_prefix.values())} files; allowlist={len(ALLOWLIST)}")
    return 1 if count else 0


if __name__ == "__main__":
    raise SystemExit(main())
