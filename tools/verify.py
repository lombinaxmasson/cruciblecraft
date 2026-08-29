#!/usr/bin/env python3
"""Layered verification entry: dev, integration, release, and archive inspect."""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
PROFILES = TOOLS / "verification_profiles.json"
BUILDER_POLICY = TOOLS / "verification_builder_policy.json"
REPORT = TOOLS / "full_verification_report.json"
DEBT = TOOLS / "known_issues" / "verification-debt.json"
TEST_RESULTS = ROOT / "build" / "test-results" / "test"
CLOSEOUT_PROFILES = ("recipes", "census", "census-replay")
RECEIPT_SCRIPTS = {
    "recipes": (
        "tools/build_t38_gametest_receipt.py",
        "tools/build_t39_gametest_receipt.py",
        "tools/build_t40_gametest_receipt.py",
        "tools/build_t41_gametest_receipt.py",
        "tools/build_t43_gametest_receipt.py",
        "tools/build_t45_gametest_receipt.py",
    ),
}

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


def builder_args_for_profile(
    profile_name: str,
    row: dict[str, Any],
) -> list[str] | None:
    if not profile_name.endswith("-replay"):
        return list(row["ordinary_args"])
    full_replay = row.get("full_replay")
    if not isinstance(full_replay, dict) or not full_replay.get("args"):
        raise ValueError(
            f"replay profile {profile_name!r} requires full_replay args "
            f"for builder {row['name']!r}"
        )
    required = [
        ROOT / path
        for path in full_replay.get("required_paths") or []
    ]
    if required and any(not path.is_file() for path in required):
        return None
    return list(full_replay["args"])


def gradle_command(task: str, *, rerun: bool = True) -> list[str]:
    wrapper = "gradlew.bat" if os.name == "nt" else "./gradlew"
    command = [str(ROOT / wrapper), task, "--no-daemon"]
    if rerun:
        command.append("--rerun-tasks")
    return command


def parse_gradle_test_xml(results_dir: Path = TEST_RESULTS) -> dict[str, Any]:
    if not results_dir.is_dir():
        return {
            "xml_present": False,
            "tests": 0,
            "failures": 0,
            "errors": 0,
            "skipped": 0,
            "rerun_tasks": True,
        }
    tests = failures = errors = skipped = 0
    xml_present = False
    for path in sorted(results_dir.glob("TEST-*.xml")):
        xml_present = True
        root = ET.parse(path).getroot()
        tests += int(root.attrib.get("tests") or 0)
        failures += int(root.attrib.get("failures") or 0)
        errors += int(root.attrib.get("errors") or 0)
        skipped += int(root.attrib.get("skipped") or 0)
    return {
        "xml_present": xml_present,
        "tests": tests,
        "failures": failures,
        "errors": errors,
        "skipped": skipped,
        "rerun_tasks": True,
    }


def gradle_full_suite_errors(summary: dict[str, Any]) -> list[str]:
    errors: list[str] = []
    if not summary.get("xml_present"):
        errors.append("Gradle TEST-*.xml is missing; filtered or UP-TO-DATE run is not a full-suite PASS")
    if int(summary.get("tests") or 0) < 1:
        errors.append("Gradle test count is insufficient for a full-suite PASS")
    if int(summary.get("failures") or 0) or int(summary.get("errors") or 0):
        errors.append("Gradle test XML records failures or errors")
    if summary.get("rerun_tasks") is not True:
        errors.append("Gradle full suite must set rerun_tasks=true")
    return errors


def gametest_spec(profile: dict[str, Any]) -> dict[str, Any]:
    raw = profile.get("gametest")
    if isinstance(raw, bool):
        return {
            "receipt_check": False,
            "run_isolated": False,
            "manual_deferred": bool(raw),
        }
    if not isinstance(raw, dict):
        raise ValueError("profile gametest must be an object")
    return {
        "receipt_check": bool(raw.get("receipt_check")),
        "run_isolated": bool(raw.get("run_isolated")),
        "manual_deferred": bool(raw.get("manual_deferred")),
    }


def debt_rows(*, debt_aware: bool, profile_name: str) -> tuple[list[dict[str, Any]], list[str]]:
    if not DEBT.is_file():
        return [], []
    document = load_json(DEBT)
    open_rows = [
        row for row in document.get("issues") or [] if row.get("status") == "open"
    ]
    notes: list[str] = []
    if not debt_aware:
        return open_rows, notes
    for row in open_rows:
        blocks = row.get("blocks_profiles") or []
        scope = row.get("scope") or "external"
        if profile_name in blocks:
            continue
        if scope == "external" or profile_name not in (blocks or []):
            notes.append(str(row.get("id") or ""))
    return open_rows, notes


def write_result_json(path: Path, document: dict[str, Any]) -> None:
    from tools import atomic_io

    payload = json.dumps(document, indent=2, sort_keys=True) + "\n"
    atomic_io.write_text(path, payload)


def run_command(
    name: str,
    command: list[str],
    *,
    timings: list[tuple[str, float]] | None = None,
) -> int:
    print(f"[{name}] {subprocess.list2cmdline(command)}", flush=True)
    started = time.perf_counter()
    try:
        completed = subprocess.run(command, cwd=ROOT, check=False)
    finally:
        wall_seconds = time.perf_counter() - started
        if timings is not None:
            timings.append((name, wall_seconds))
        print(f"[{name}] wall-time: {wall_seconds:.3f}s", flush=True)
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


def _record(failures: list[str], report_all: bool, message: str) -> int:
    print(message, file=sys.stderr, flush=True)
    failures.append(message)
    return 0 if report_all else 1


def run_profile_steps(
    profile_name: str,
    args: argparse.Namespace,
    *,
    document: dict[str, Any] | None = None,
) -> dict[str, Any]:
    document = document if document is not None else load_profiles()
    profile = document["profiles"][profile_name]
    builder_policy = load_json(BUILDER_POLICY)
    timings: list[tuple[str, float]] = []
    failures: list[str] = []
    gradle_summary: dict[str, Any] | None = None
    gametest_result: dict[str, Any] = {"status": "SKIP"}
    report_all = bool(getattr(args, "report_all", False))
    print(f"verify integration --profile {profile_name}")
    print(f"owner={profile['owner']} tier={profile['tier']}")
    try:
        for row in builder_rows_for_profile(profile_name, document, builder_policy):
            argv = builder_args_for_profile(profile_name, row)
            if argv is None:
                print(
                    f"[builder:{row['name']}] SKIP source replay "
                    "(required input missing); compact pass is not a source pass",
                    flush=True,
                )
                continue
            code = run_command(
                f"builder:{row['name']}",
                [sys.executable, row["script"], *argv],
                timings=timings,
            )
            if code != 0:
                if _record(failures, report_all, f"builder:{row['name']} failed"):
                    return _result(profile_name, failures, timings, gradle_summary, gametest_result)
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
                timings=timings,
            )
            if code != 0:
                if _record(failures, report_all, f"python:{module} failed"):
                    return _result(profile_name, failures, timings, gradle_summary, gametest_result)
        for task in profile["gradle_tasks"]:
            code = run_command(
                f"gradle:{task}",
                gradle_command(task, rerun=True),
                timings=timings,
            )
            if task == "test":
                gradle_summary = parse_gradle_test_xml()
                gradle_summary["exit_code"] = code
                for error in gradle_full_suite_errors(gradle_summary):
                    if _record(failures, report_all, error):
                        return _result(profile_name, failures, timings, gradle_summary, gametest_result)
            elif code != 0:
                if _record(failures, report_all, f"gradle:{task} failed"):
                    return _result(profile_name, failures, timings, gradle_summary, gametest_result)
        if profile["datagen"]:
            print("datagen required: run .\\gradlew.bat runData twice and compare generated trees")
        spec = gametest_spec(profile)
        if spec["receipt_check"]:
            scripts = RECEIPT_SCRIPTS.get(profile_name, ())
            if not scripts:
                # Alias profiles (card-closeout) expand elsewhere; diagnostic
                # must not report a receipt PASS without running --check.
                gametest_result = {"status": "SKIP", "receipt_check": True}
            else:
                gametest_result = {"status": "PASS", "receipt_check": True}
                for script in scripts:
                    code = run_command(
                        f"gametest-receipt:{script}",
                        [sys.executable, script, "--check"],
                        timings=timings,
                    )
                    if code != 0:
                        gametest_result["status"] = "FAIL"
                        if _record(failures, report_all, f"gametest receipt failed: {script}"):
                            return _result(profile_name, failures, timings, gradle_summary, gametest_result)
        elif spec["run_isolated"]:
            gametest_result = {"status": "FAIL", "run_isolated": True}
            if _record(failures, report_all, "isolated GameTest requested but not executed by verify.py"):
                return _result(profile_name, failures, timings, gradle_summary, gametest_result)
        elif spec["manual_deferred"]:
            gametest_result = {"status": "DEFERRED", "manual_deferred": True}
            print("GameTest status: DEFERRED (manual_deferred; not a PASS)")
        else:
            gametest_result = {"status": "SKIP"}
        open_debt, scoped = debt_rows(
            debt_aware=bool(getattr(args, "debt_aware", False)),
            profile_name=profile_name,
        )
        if scoped:
            print("debt-aware external notes: " + ", ".join(scoped))
        if open_debt and not getattr(args, "debt_aware", False):
            print("open verification debt: " + ", ".join(row.get("id") or "?" for row in open_debt))
        return _result(profile_name, failures, timings, gradle_summary, gametest_result)
    finally:
        print("integration wall-time summary:")
        for name, wall_seconds in timings:
            print(f"  {name}: {wall_seconds:.3f}s")


def diagnostic_report() -> dict[str, Any]:
    """Structured stale/debt report. Never upgrades required failures to PASS."""
    from tools import currentness
    from tools import run_verification_dag as dag

    open_debt, _notes = debt_rows(debt_aware=False, profile_name="card-diagnostic-T40")
    sidecars: list[dict[str, Any]] = []
    for row in currentness.TARGETS:
        artifact = ROOT / row["artifact"]
        if not artifact.is_file():
            sidecars.append(
                {
                    "artifact": row["artifact"],
                    "scope": row["scope"],
                    "compact": ["MISSING"],
                    "hash_only": [],
                }
            )
            continue
        compact = currentness.check_sidecar(artifact, include_hash_only=False)
        hash_only = [
            error
            for error in currentness.check_sidecar(artifact, include_hash_only=True)
            if error not in compact
        ]
        sidecars.append(
            {
                "artifact": row["artifact"],
                "scope": row["scope"],
                "compact": compact,
                "hash_only": hash_only,
            }
        )
    return {
        "open_debt": [str(row.get("id") or "?") for row in open_debt],
        "dag": dag.plan(),
        "sidecars": sidecars,
    }


def combine_closeout_results(subresults: list[dict[str, Any]]) -> dict[str, Any]:
    """Keep recipes Gradle/GameTest evidence; do not let census-replay overwrite it."""
    failures: list[str] = []
    timings: list[dict[str, Any]] = []
    by_name: dict[str, Any] = {}
    for row in subresults:
        failures.extend(row.get("failures") or [])
        timings.extend(row.get("timings") or [])
        by_name[str(row.get("profile"))] = {
            "status": row.get("status"),
            "failures": list(row.get("failures") or []),
            "gradle": row.get("gradle"),
            "gametest": row.get("gametest"),
        }
    recipes = by_name.get("recipes") or {}
    return {
        "schema_version": 1,
        "command": "integration",
        "profile": "card-closeout",
        "status": "FAIL" if failures else "PASS",
        "failures": failures,
        "profiles": by_name,
        "gradle": recipes.get("gradle"),
        "gametest": recipes.get("gametest") or {"status": "SKIP"},
        "timings": timings,
    }


def _result(
    profile_name: str,
    failures: list[str],
    timings: list[tuple[str, float]],
    gradle_summary: dict[str, Any] | None,
    gametest_result: dict[str, Any],
) -> dict[str, Any]:
    diagnostic = profile_name == "card-diagnostic-T40"
    if failures:
        status = "FAIL"
    elif diagnostic:
        status = "DIAGNOSTIC"
    else:
        status = "PASS"
    if profile_name != "card-diagnostic-T40" and not failures:
        print("integration profile complete. Do not treat this as a player release.")
    elif diagnostic and not failures:
        print("card-diagnostic-T40 complete. This is not a closeout PASS.")
    result = {
        "schema_version": 1,
        "command": "integration",
        "profile": profile_name,
        "status": status,
        "failures": failures,
        "gradle": gradle_summary,
        "gametest": gametest_result,
        "timings": [{"name": name, "seconds": seconds} for name, seconds in timings],
    }
    if diagnostic:
        report = diagnostic_report()
        result["diagnostic"] = report
        print(
            "diagnostic open debt: "
            + (", ".join(report["open_debt"]) or "(none)")
        )
        stale = (report.get("dag") or {}).get("stale") or []
        print(f"diagnostic DAG stale nodes: {len(stale)}")
    return result


def cmd_integration(args: argparse.Namespace) -> int:
    document = load_profiles()
    names = list(CLOSEOUT_PROFILES) if args.profile == "card-closeout" else [args.profile]
    subresults: list[dict[str, Any]] = []
    for name in names:
        if name not in document["profiles"]:
            print(
                f"unknown profile {name!r}; available: "
                + ", ".join(sorted(document["profiles"])),
                file=sys.stderr,
            )
            return 2
        last = run_profile_steps(name, args, document=document)
        subresults.append(last)
        if last["status"] == "FAIL" and not getattr(args, "report_all", False):
            break
    if args.profile == "card-closeout":
        result = combine_closeout_results(subresults)
    else:
        result = subresults[-1] if subresults else {
            "schema_version": 1,
            "command": "integration",
            "profile": args.profile,
            "status": "FAIL",
            "failures": ["no profile ran"],
        }
    if getattr(args, "json_path", None):
        write_result_json(Path(args.json_path), result)
    return 0 if result["status"] in {"PASS", "DIAGNOSTIC"} else 1


def cmd_currentness(args: argparse.Namespace) -> int:
    command = [sys.executable, "tools/rebind_currentness.py", f"--{args.mode}", "--scope", args.scope]
    code = run_command("currentness:rebind", command)
    dag_mode = "--plan" if args.mode == "plan" else "--check"
    dag_code = run_command(
        "currentness:dag",
        [sys.executable, "tools/run_verification_dag.py", dag_mode],
    )
    return code or dag_code


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
    integration.add_argument("--report-all", action="store_true")
    integration.add_argument("--json", dest="json_path", type=Path)
    integration.add_argument("--debt-aware", action="store_true")
    integration.set_defaults(func=cmd_integration)

    currentness = sub.add_parser(
        "currentness",
        help="Plan or check DAG/currentness sidecars without a full profile run",
    )
    currentness.add_argument("--mode", choices=("plan", "check", "write"), default="plan")
    currentness.add_argument("--scope", choices=("t35", "card-closeout", "recipes", "all"), default="all")
    currentness.set_defaults(func=cmd_currentness)

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
