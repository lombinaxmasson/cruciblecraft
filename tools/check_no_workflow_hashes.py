#!/usr/bin/env python3
"""Report workflow-only hashes, currentness state, and seal APIs in active files.

The active file set is derived from the verification profiles, ordinary
builder policy, and Python test policy.  The default migration-report mode is
non-blocking while the repository is being cut over; ``--check`` makes every
finding a failure.
"""
from __future__ import annotations

import argparse
import fnmatch
import json
import re
import sys
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_POLICY = ROOT / "tools" / "workflow_hash_policy.json"

SKIP_DIR_NAMES = frozenset(
    {".git", ".gradle", ".idea", "__pycache__", "build", "node_modules"}
)


class PolicyError(ValueError):
    """The boundary policy or one of its reachability inputs is invalid."""


def _load_json(path: Path) -> dict[str, Any]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise PolicyError(f"cannot load {path}: {error}") from error
    if not isinstance(value, dict):
        raise PolicyError(f"{path} must contain a JSON object")
    return value


def load_policy(path: Path = DEFAULT_POLICY) -> dict[str, Any]:
    policy = _load_json(path)
    if policy.get("schema_version") != 1:
        raise PolicyError(f"{path} has unsupported schema_version")
    required = (
        "reachability_sources",
        "scannable_suffixes",
        "forbidden_patterns",
    )
    missing = [key for key in required if key not in policy]
    if missing:
        raise PolicyError(f"{path} missing {', '.join(missing)}")
    for row in policy["forbidden_patterns"]:
        try:
            re.compile(str(row["regex"]))
        except (KeyError, re.error) as error:
            raise PolicyError(f"invalid forbidden pattern {row!r}: {error}") from error
    return policy


def _normalise_pattern(value: object) -> str | None:
    if not isinstance(value, str):
        return None
    value = value.strip().replace("\\", "/")
    if not value or value.startswith("/") or re.match(r"^[A-Za-z]:/", value):
        return None
    if ".." in value.split("/"):
        return None
    return value.removeprefix("./")


def _add_patterns(target: set[str], values: Iterable[object]) -> None:
    for value in values:
        normalised = _normalise_pattern(value)
        if normalised:
            target.add(normalised)


def _test_module_path(module: object) -> str | None:
    if not isinstance(module, str) or module.startswith("$"):
        return None
    leaf = module.rsplit(".", 1)[-1]
    if not re.fullmatch(r"[A-Za-z_][A-Za-z0-9_]*", leaf):
        return None
    return f"tools/tests/{leaf}.py"


def _builder_output_path(output: object) -> str | None:
    value = _normalise_pattern(output)
    if not value:
        return None
    top_level = ("tools/", "src/", "docs/", ".github/", "archive/")
    return value if value.startswith(top_level) else f"tools/{value}"


def derive_active_patterns(
    root: Path, policy: dict[str, Any]
) -> list[str]:
    """Derive active paths from all three verification reachability policies."""
    sources = policy.get("reachability_sources")
    if not isinstance(sources, dict):
        raise PolicyError("reachability_sources must be an object")

    def source(name: str) -> dict[str, Any]:
        relative = _normalise_pattern(sources.get(name))
        if relative is None:
            raise PolicyError(f"missing reachability source {name}")
        return _load_json(root / relative)

    profiles_document = source("verification_profiles")
    builders_document = source("verification_builder_policy")
    tests_document = source("python_test_policy")

    patterns: set[str] = set()
    active_builder_names: set[str] = set()
    active_test_modules: set[str] = set()
    excluded_profiles = {
        str(name) for name in policy.get("excluded_profile_names") or []
    }

    _add_patterns(
        patterns, profiles_document.get("documentation_path_patterns") or []
    )
    profiles = profiles_document.get("profiles")
    if not isinstance(profiles, dict):
        raise PolicyError("verification_profiles.json profiles must be an object")
    for profile_name, profile in profiles.items():
        if profile_name in excluded_profiles or not isinstance(profile, dict):
            continue
        _add_patterns(patterns, profile.get("owned_paths") or [])
        active_builder_names.update(
            str(name)
            for name in profile.get("builders") or []
            if isinstance(name, str)
        )
        active_test_modules.update(
            str(name)
            for name in profile.get("python_modules") or []
            if isinstance(name, str)
        )

    pre_chain = builders_document.get("pre_chain_builders") or []
    ordinary = builders_document.get("builders") or []
    if not isinstance(pre_chain, list) or not isinstance(ordinary, list):
        raise PolicyError("verification_builder_policy builder lists are invalid")

    def add_builder_row(row: object) -> None:
        if not isinstance(row, dict):
            return
        script = _normalise_pattern(row.get("script"))
        if script:
            patterns.add(script)
        for output in row.get("outputs") or []:
            resolved = _builder_output_path(output)
            if resolved:
                patterns.add(resolved)

    for row in pre_chain:
        add_builder_row(row)
    for row in ordinary:
        if isinstance(row, dict) and str(row.get("name") or "") in active_builder_names:
            add_builder_row(row)

    _add_patterns(
        patterns, tests_document.get("documentation_path_patterns") or []
    )
    affected_rules = tests_document.get("affected_rules") or []
    if not isinstance(affected_rules, list):
        raise PolicyError("python_test_policy.json affected_rules must be a list")
    for rule in affected_rules:
        if not isinstance(rule, dict):
            continue
        _add_patterns(patterns, rule.get("paths") or [])
        active_test_modules.update(
            str(name)
            for name in rule.get("test_modules") or []
            if isinstance(name, str)
        )

    for module in active_test_modules:
        path = _test_module_path(module)
        if path:
            patterns.add(path)
    _add_patterns(patterns, policy.get("always_active_patterns") or [])
    return sorted(patterns)


def _path_matches(path: str, patterns: Iterable[object]) -> bool:
    normalised = path.replace("\\", "/")
    return any(
        isinstance(pattern, str)
        and fnmatch.fnmatchcase(normalised, pattern.replace("\\", "/"))
        for pattern in patterns
    )


def exemption_for_path(
    relative: str, policy: dict[str, Any]
) -> str | None:
    for row in policy.get("whole_path_exemptions") or []:
        if isinstance(row, dict) and _path_matches(
            relative, row.get("path_patterns") or []
        ):
            return str(row.get("id") or "path_exemption")
    return None


def exemption_for_token(
    relative: str,
    token: str,
    policy: dict[str, Any],
    scanners: dict[str, Any] | None = None,
) -> str | None:
    compiled = (scanners or compile_scanners(policy))["token_exemptions"]
    for exemption_id, path_patterns, matcher in compiled:
        if not _path_matches(relative, path_patterns):
            continue
        if matcher.search(token):
            return exemption_id
    return None


def compile_scanners(policy: dict[str, Any]) -> dict[str, Any]:
    forbidden: list[tuple[str, re.Pattern[str]]] = []
    for row in policy.get("forbidden_patterns") or []:
        if not isinstance(row, dict):
            continue
        forbidden.append(
            (
                str(row.get("id") or "forbidden"),
                re.compile(str(row.get("regex") or r"(?!)")),
            )
        )
    token_exemptions: list[tuple[str, list[str], re.Pattern[str]]] = []
    for row in policy.get("token_exemptions") or []:
        if not isinstance(row, dict):
            continue
        try:
            matcher = re.compile(str(row.get("token_regex") or r"(?!)"))
        except re.error as error:
            raise PolicyError(f"invalid token exemption {row!r}: {error}") from error
        token_exemptions.append(
            (
                str(row.get("id") or "token_exemption"),
                [str(pattern) for pattern in row.get("path_patterns") or []],
                matcher,
            )
        )
    return {
        "forbidden": forbidden,
        "token_exemptions": token_exemptions,
    }


def _line_findings(
    relative: str,
    line: str,
    line_number: int,
    policy: dict[str, Any],
    scanners: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    findings: list[dict[str, Any]] = []
    occupied: list[tuple[int, int]] = []
    compiled = (scanners or compile_scanners(policy))["forbidden"]
    for rule, matcher in compiled:
        for match in matcher.finditer(line):
            span = match.span()
            if any(start <= span[0] and span[1] <= end for start, end in occupied):
                continue
            token = match.group(0)
            if exemption_for_token(relative, token, policy, scanners):
                continue
            occupied.append(span)
            findings.append(
                {
                    "path": relative,
                    "line": line_number,
                    "column": match.start() + 1,
                    "token": token,
                    "rule": rule,
                    "text": line.strip()[:240],
                }
            )
    return findings


def scan_text(
    relative: str,
    text: str,
    policy: dict[str, Any],
    *,
    include_path: bool = True,
    scanners: dict[str, Any] | None = None,
) -> list[dict[str, Any]]:
    """Scan one reachable text file and return actionable findings."""
    relative = relative.replace("\\", "/")
    if exemption_for_path(relative, policy):
        return []
    scanners = scanners or compile_scanners(policy)
    findings: list[dict[str, Any]] = []
    if include_path:
        findings.extend(
            _line_findings(relative, relative, 0, policy, scanners)
        )
    for line_number, line in enumerate(text.splitlines(), start=1):
        findings.extend(_line_findings(relative, line, line_number, policy, scanners))
    return findings


def _is_scannable(path: Path, relative: str, policy: dict[str, Any]) -> bool:
    suffixes = {
        str(suffix).lower() for suffix in policy.get("scannable_suffixes") or []
    }
    if path.suffix.lower() in suffixes:
        return True
    return relative.startswith(".github/workflows/")


def iter_active_files(
    root: Path, policy: dict[str, Any]
) -> list[Path]:
    files: set[Path] = set()
    for pattern in derive_active_patterns(root, policy):
        magic = any(character in pattern for character in "*?[")
        if pattern.endswith("/**") and not any(
            character in pattern[:-3] for character in "*?["
        ):
            base = root / pattern[:-3]
            matches: Iterable[Path] = (base,) if base.exists() else ()
        elif not magic:
            direct = root / pattern
            matches = (direct,) if direct.exists() else ()
        else:
            try:
                matches = root.glob(pattern)
            except (OSError, ValueError):
                continue
        for match in matches:
            candidates = match.rglob("*") if match.is_dir() else (match,)
            for path in candidates:
                if not path.is_file():
                    continue
                relative = path.relative_to(root).as_posix()
                if any(part in SKIP_DIR_NAMES for part in Path(relative).parts):
                    continue
                if exemption_for_path(relative, policy):
                    continue
                if _is_scannable(path, relative, policy):
                    files.add(path.resolve())
    return sorted(files, key=lambda path: path.as_posix())


def resolve_scan_files(
    root: Path,
    policy: dict[str, Any],
    paths: Iterable[str] | None = None,
) -> list[Path]:
    if paths is None:
        return iter_active_files(root, policy)
    patterns = derive_active_patterns(root, policy)
    files: set[Path] = set()
    root = root.resolve()
    for raw in paths:
        relative = str(raw).replace("\\", "/").removeprefix("./")
        if not relative:
            continue
        path = (root / relative).resolve()
        try:
            path.relative_to(root)
        except ValueError:
            continue
        if not path.is_file():
            continue
        if exemption_for_path(relative, policy):
            continue
        if not _path_matches(relative, patterns):
            continue
        if _is_scannable(path, relative, policy):
            files.add(path)
    return sorted(files, key=lambda path: path.as_posix())


def scan_repository(
    root: Path = ROOT,
    policy: dict[str, Any] | None = None,
    paths: Iterable[str] | None = None,
) -> dict[str, Any]:
    policy = policy or load_policy(root / "tools" / "workflow_hash_policy.json")
    scanners = compile_scanners(policy)
    files = resolve_scan_files(root, policy, paths)
    findings: list[dict[str, Any]] = []
    for path in files:
        relative = path.relative_to(root.resolve()).as_posix()
        try:
            text = path.read_text(encoding="utf-8")
        except (OSError, UnicodeDecodeError):
            continue
        findings.extend(scan_text(relative, text, policy, scanners=scanners))
    return {
        "mode": "migration-report",
        "active_files": len(files),
        "findings": findings,
        "finding_count": len(findings),
        "scoped": paths is not None,
    }


def _print_report(
    result: dict[str, Any], *, json_output: bool, limit: int, strict: bool
) -> None:
    result = dict(result)
    result["mode"] = "check" if strict else "migration-report"
    if json_output:
        print(json.dumps(result, indent=2, sort_keys=True))
        return
    findings = result["findings"]
    for row in findings[: max(limit, 0)]:
        print(
            f"{row['path']}:{row['line']}:{row['column']}: "
            f"{row['token']} [{row['rule']}] {row['text']}"
        )
    hidden = len(findings) - min(len(findings), max(limit, 0))
    if hidden:
        print(f"... {hidden} additional finding(s) omitted")
    mode = "CHECK" if strict else "MIGRATION"
    disposition = "blocking" if strict else "non-blocking"
    print(
        f"{mode}: {result['finding_count']} workflow-hash finding(s) in "
        f"{result['active_files']} active file(s); {disposition}"
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--policy", type=Path)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument(
        "--check",
        action="store_true",
        help="fail when any active workflow-only hash/currentness/seal remains",
    )
    modes.add_argument(
        "--migration-report",
        action="store_true",
        help="report findings without failing (the temporary default)",
    )
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--limit", type=int, default=100)
    parser.add_argument(
        "--path",
        action="append",
        default=[],
        help="scan only these reachable files; omit for a full active-set scan",
    )
    args = parser.parse_args(argv)

    root = args.root.resolve()
    policy_path = args.policy or root / "tools" / "workflow_hash_policy.json"
    if not policy_path.is_absolute():
        policy_path = root / policy_path
    try:
        policy = load_policy(policy_path)
        scoped_paths = [str(path) for path in args.path] or None
        result = scan_repository(root, policy, paths=scoped_paths)
        _print_report(
            result,
            json_output=args.json,
            limit=args.limit,
            strict=args.check,
        )
    except PolicyError as error:
        print(f"POLICY_ERROR: {error}", file=sys.stderr)
        return 2
    return 1 if args.check and result["finding_count"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
