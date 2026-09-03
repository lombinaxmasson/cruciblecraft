#!/usr/bin/env python3
"""Fail if active roots still use milestone-style TXX names.

Exempt only archive/sealed/** and docs/history/** (except the active card-plan
tree). Temporary allowlist entries must carry owner, reason, and expiry.
Program closeout requires an empty allowlist.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]

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
    "docs/history/card-plans/active",
    "docs/decisions",
    "src/t14Benchmark",
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
    r"gt6[A-Za-z0-9_]*|Circuit T\d+",
    re.IGNORECASE,
)

# Program closeout requires this list to stay empty.
ALLOWLIST: list[dict[str, str]] = []


def relative(path: Path) -> str:
    return path.resolve().relative_to(ROOT).as_posix()


def is_exempt(rel: str) -> bool:
    if rel.startswith("archive/sealed/"):
        return True
    if rel.startswith("docs/history/card-plans/active/"):
        return False
    return rel.startswith("docs/history/")


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
            if is_allowlisted(rel, line, token):
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


def iter_scan_files(*, quick: bool = False) -> list[Path]:
    if quick:
        files: list[Path] = []
        for prefix in QUICK_PREFIXES:
            root = ROOT / prefix.rstrip("/")
            if root.is_file():
                files.append(root)
                continue
            if not root.is_dir():
                continue
            for path in root.rglob("*"):
                if path.is_file() and path.suffix.lower() in TEXT_SUFFIXES:
                    files.append(path)
        for name in QUICK_FILES:
            path = ROOT / name
            if path.is_file():
                files.append(path)
        unique: list[Path] = []
        seen: set[Path] = set()
        for path in files:
            resolved = path.resolve()
            if resolved in seen:
                continue
            seen.add(resolved)
            unique.append(resolved)
        return unique
    files = []
    for raw in SCAN_ROOTS:
        root = ROOT / raw
        if root.is_file():
            files.append(root)
            continue
        if not root.is_dir():
            continue
        for path in root.rglob("*"):
            if not path.is_file():
                continue
            if any(part in SKIP_DIR_NAMES for part in path.parts):
                continue
            if path.suffix.lower() not in TEXT_SUFFIXES:
                continue
            files.append(path)
    for name in SCAN_FILES:
        path = ROOT / name
        if path.is_file():
            files.append(path)
    for path in ROOT.glob("settings.gradle*"):
        if path.is_file():
            files.append(path)
    unique: list[Path] = []
    seen: set[Path] = set()
    for path in files:
        resolved = path.resolve()
        if resolved in seen:
            continue
        seen.add(resolved)
        unique.append(resolved)
    return unique


QUICK_PREFIXES = (
    "src/main/java/com/masson/cruciblecraft/logistics/",
    "src/main/java/com/masson/cruciblecraft/verification/",
    "tools/capabilities/",
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
    for path in iter_scan_files(quick=quick):
        rel = relative(path)
        if is_exempt(rel):
            continue
        if quick and not is_quick_target(rel):
            continue
        path_tokens = line_findings(rel.replace("\\", "/"))
        rows = [
            {
                "file": rel,
                "line": 0,
                "match": token,
                "text": rel,
            }
            for token in path_tokens
        ]
        try:
            text = path.read_text(encoding="utf-8")
            rows.extend(scan_text(rel, text))
        except (OSError, UnicodeDecodeError):
            pass
        if not rows:
            continue
        count += len(rows)
        prefix = "/".join(rel.split("/")[:2]) if rel.startswith("src/") else rel.split("/")[0]
        by_prefix[prefix] = by_prefix.get(prefix, 0) + 1
        if not summary:
            findings.extend(rows)
    return count, findings, by_prefix


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--limit", type=int, default=50)
    parser.add_argument("--summary", action="store_true")
    parser.add_argument(
        "--quick",
        action="store_true",
        help="Scan live runtime Java and capability tools only.",
    )
    args = parser.parse_args(argv)
    errors = allowlist_errors()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
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
