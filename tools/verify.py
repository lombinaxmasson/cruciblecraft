#!/usr/bin/env python3
"""Layered verification entry: dev, integration, release, and archive inspect."""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
PROFILES = TOOLS / "verification_profiles.json"
BUILDER_POLICY = TOOLS / "verification_builder_policy.json"
REPORT = TOOLS / "full_verification_report.json"
DEBT = TOOLS / "known_issues" / "verification-debt.json"

if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import run_python_tests as python_tests


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def load_profiles() -> dict[str, Any]:
    document = load_json(PROFILES)
    if document.get("schema_version") != 1:
        raise ValueError("unsupported verification profile schema")
    if document.get("unmatched_policy") != "report":
        raise ValueError("unmatched paths must be reported, not escalated")
    return document


def normalize_paths(paths: Iterable[str]) -> tuple[str, ...]:
    return tuple(sorted({
        path.replace("\\", "/").removeprefix("./")
        for path in paths
        if path.strip()
    }))


def path_matches(path: str, pattern: str) -> bool:
    import fnmatch
    return fnmatch.fnmatchcase(path, pattern)


def is_documentation_path(document: dict[str, Any], path: str) -> bool:
    patterns = document.get("documentation_path_patterns") or ["*.md"]
    return any(path_matches(path, pattern) for pattern in patterns)


def classify_paths(
    document: dict[str, Any],
    paths: Iterable[str],
) -> dict[str, Any]:
    profiles = document["profiles"]
    matched: dict[str, list[str]] = {name: [] for name in profiles}
    unmatched: list[str] = []
    documentation: list[str] = []
    for path in normalize_paths(paths):
        hits = [
            name
            for name, profile in profiles.items()
            if any(path_matches(path, pattern) for pattern in profile["owned_paths"])
        ]
        if hits:
            for name in hits:
                matched[name].append(path)
            continue
        if is_documentation_path(document, path):
            documentation.append(path)
            matched["docs"].append(path)
            continue
        unmatched.append(path)
    selected = sorted(name for name, rows in matched.items() if rows)
    return {
        "matched": {name: rows for name, rows in matched.items() if rows},
        "selected_profiles": selected,
        "documentation_paths": documentation,
        "unmatched_paths": unmatched,
    }


def builder_rows_for_profile(
    profile_name: str,
    profiles: dict[str, Any],
    builder_policy: dict[str, Any],
) -> list[dict[str, Any]]:
    wanted = set(profiles["profiles"][profile_name]["builders"])
    return [
        row for row in builder_policy["builders"]
        if row["name"] in wanted
    ]


def gradle_command(task: str) -> list[str]:
    wrapper = "gradlew.bat" if os.name == "nt" else "./gradlew"
    return [str(ROOT / wrapper), task, "--no-daemon"]


def run_command(name: str, command: list[str]) -> int:
    print(f"[{name}] {subprocess.list2cmdline(command)}", flush=True)
    completed = subprocess.run(command, cwd=ROOT, check=False)
    if completed.returncode == 0:
        print(f"[{name}] PASS", flush=True)
    else:
        print(f"[{name}] FAIL exit {completed.returncode}", flush=True)
    return completed.returncode


def git_diff_names(*rev_args: str) -> tuple[str, ...]:
    completed = subprocess.run(
        ["git", "diff", "--name-only", "-z", *rev_args],
        cwd=ROOT,
        capture_output=True,
        check=False,
    )
    if completed.returncode != 0:
        return ()
    return python_tests.normalize_paths(
        item.decode("utf-8")
        for item in completed.stdout.split(b"\0")
        if item
    )


def changed_paths(explicit: list[str]) -> list[str]:
    if explicit:
        return list(explicit)
    base = os.environ.get("CRUCIBLECRAFT_DIFF_BASE", "").strip()
    if base:
        names = git_diff_names(base)
        if names:
            return list(names)
    github_base = os.environ.get("GITHUB_BASE_REF", "").strip()
    if github_base:
        names = git_diff_names(f"origin/{github_base}...HEAD")
        if names:
            return list(names)
    before = os.environ.get("GITHUB_EVENT_BEFORE", "").strip()
    if before and set(before) != {"0"}:
        names = git_diff_names(f"{before}...HEAD")
        if names:
            return list(names)
    return list(python_tests.git_changed_paths())


def print_plan(classification: dict[str, Any], *, command: str) -> None:
    print(f"verify {command}")
    selected = classification["selected_profiles"]
    print("selected profiles: " + (", ".join(selected) if selected else "(none)"))
    if classification["documentation_paths"]:
        print("documentation paths: " + ", ".join(classification["documentation_paths"]))
    if classification["unmatched_paths"]:
        print(
            "unmatched paths (declare a profile or pass --scope): "
            + ", ".join(classification["unmatched_paths"])
        )
        print(
            "available profiles: "
            + ", ".join(sorted(load_profiles()["profiles"]))
        )


def cmd_dev(args: argparse.Namespace) -> int:
    document = load_profiles()
    paths = args.path or changed_paths([])
    classification = classify_paths(document, paths)
    print_plan(classification, command="dev")
    if classification["unmatched_paths"]:
        return 2
    if not classification["selected_profiles"]:
        print("no owned paths changed; nothing to run")
        return 0
    selected = classification["selected_profiles"]
    python_paths = [
        path
        for rows in classification["matched"].values()
        for path in rows
    ]
    docs_only = set(selected) <= {"docs"}
    if not docs_only:
        python_command = [
            sys.executable,
            "tools/run_python_tests.py",
            "--suite",
            "affected",
            "--exclude-slow",
        ]
        for path in python_paths:
            python_command.extend(["--path", path])
        code = run_command("python:affected", python_command)
        if code != 0:
            return code
        java_changed = any(
            path.startswith("src/main/java/") or path.startswith("src/test/java/")
            for path in python_paths
        )
        if java_changed:
            code = run_command("gradle:test", gradle_command("test"))
            if code != 0:
                return code
    if "docs" in selected or classification["documentation_paths"] or docs_only:
        code = run_command(
            "markdown-links",
            [sys.executable, "tools/check_markdown_links.py"],
        )
        if code != 0:
            return code
        code = run_command(
            "python:docs",
            [
                sys.executable,
                "tools/run_python_tests.py",
                "--suite",
                "affected",
                "--exclude-slow",
                "--path",
                "docs/history/INDEX.md",
            ],
        )
        if code != 0:
            return code
    print("dev complete. Upgrade to integration --profile <name> before closing a content card.")
    return 0


def cmd_integration(args: argparse.Namespace) -> int:
    document = load_profiles()
    profile_name = args.profile
    if profile_name not in document["profiles"]:
        print(
            f"unknown profile {profile_name!r}; available: "
            + ", ".join(sorted(document["profiles"])),
            file=sys.stderr,
        )
        return 2
    profile = document["profiles"][profile_name]
    print(f"verify integration --profile {profile_name}")
    print(f"owner={profile['owner']} tier={profile['tier']}")
    builder_policy = load_json(BUILDER_POLICY)
    for row in builder_rows_for_profile(profile_name, document, builder_policy):
        command = [sys.executable, row["script"], *row["ordinary_args"]]
        code = run_command(f"builder:{row['name']}", command)
        if code != 0:
            return code
    for module in profile["python_modules"]:
        code = run_command(
            f"python:{module}",
            [
                sys.executable,
                "tools/run_python_tests.py",
                "--suite",
                "affected",
                "--path",
                f"tools/tests/{module}.py",
            ],
        )
        if code != 0:
            return code
    for task in profile["gradle_tasks"]:
        code = run_command(f"gradle:{task}", gradle_command(task))
        if code != 0:
            return code
    if profile["datagen"]:
        print("datagen required: run .\\gradlew.bat runData twice and compare generated trees")
    if profile["gametest"]:
        print("GameTest required for this profile: .\\gradlew.bat runGameTestServer")
    print("integration profile complete. Do not treat this as a player release.")
    return 0


def cmd_release(args: argparse.Namespace) -> int:
    print("verify release")
    print(
        "Player-facing release verification is deferred. "
        "This command inspects the historical receipt and debt ledger; "
        "it does not rebind READY or run soak/provenance."
    )
    if DEBT.is_file():
        debt = load_json(DEBT)
        open_rows = [
            row["id"] for row in debt.get("issues", [])
            if row.get("status") == "open"
        ]
        print("open verification debt: " + (", ".join(open_rows) or "(none)"))
    if args.run_historical:
        return run_command(
            "historical-full-check",
            [sys.executable, "tools/run_full_verification.py", "--check"],
        )
    print("pass --run-historical to invoke tools/run_full_verification.py --check")
    return 0


def cmd_archive_inspect(_args: argparse.Namespace) -> int:
    print("verify archive-inspect")
    if not REPORT.is_file():
        print("missing tools/full_verification_report.json", file=sys.stderr)
        return 2
    document = load_json(REPORT)
    print(f"historical status: {document.get('status')}")
    binding = document.get("ready_binding") or {}
    print(f"tooling_snapshot_sha256: {binding.get('tooling_snapshot_sha256')}")
    t31 = document.get("t31_readiness_acceptance") or {}
    print(f"t31_readiness_acceptance.status: {t31.get('status')}")
    print("This receipt is historical. It is not a live currentness proof.")
    return 0


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    dev = sub.add_parser("dev", help="Run tests owned by changed paths")
    dev.add_argument("--path", action="append", default=[])
    dev.set_defaults(func=cmd_dev)

    integration = sub.add_parser(
        "integration",
        help="Run one explicit profile before closing a content card",
    )
    integration.add_argument("--profile", required=True)
    integration.set_defaults(func=cmd_integration)

    release = sub.add_parser(
        "release",
        help="Reserved for a future player-facing release card",
    )
    release.add_argument("--run-historical", action="store_true")
    release.set_defaults(func=cmd_release)

    inspect = sub.add_parser(
        "archive-inspect",
        help="Read the historical full verification receipt",
    )
    inspect.set_defaults(func=cmd_archive_inspect)

    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
