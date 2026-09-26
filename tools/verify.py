#!/usr/bin/env python3
"""Run fresh, profile-driven verification and write the latest local receipt."""
from __future__ import annotations

import argparse
import fnmatch
import json
import os
import platform
import re
import shutil
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
LATEST = ROOT / "build" / "verification" / "latest.json"
TEST_RESULTS = ROOT / "build" / "test-results" / "test"

if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import capability_ledger
from tools import check_gametest_hygiene as gametest_hygiene
from tools import run_python_tests as python_tests
from tools import tree_compare

# Local Minecraft run trees are evidence, not profile-owned sources.
# fnmatch "*" matches slashes, so "run-*" covers run-game-test-filtered/**.
LOCAL_RUN_PATH_PATTERNS = (
    "run/*",
    "run-*",
)


class VerificationError(ValueError):
    """Active verification configuration is inconsistent."""


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def load_profiles() -> dict[str, Any]:
    document = load_json(PROFILES)
    if document.get("schema_version") != 2:
        raise VerificationError("unsupported verification profile schema")
    if document.get("unmatched_policy") != "report":
        raise VerificationError("unmatched paths must be reported")
    profiles = document.get("profiles")
    if not isinstance(profiles, dict) or not profiles:
        raise VerificationError("verification profiles are missing")
    for field in ("active_profiles", "release_profiles"):
        names = document.get(field)
        if not isinstance(names, list) or not names:
            raise VerificationError(f"{field} must name at least one profile")
        missing = sorted(set(names) - set(profiles))
        if missing:
            raise VerificationError(f"{field} names missing profiles: {missing}")
    return document


def load_builder_policy() -> dict[str, Any]:
    document = load_json(BUILDER_POLICY)
    if document.get("schema_version") != 2:
        raise VerificationError("unsupported active builder policy schema")
    rows = document.get("builders")
    if not isinstance(rows, list):
        raise VerificationError("active builder policy omits builders")
    names = [str(row.get("name") or "") for row in rows]
    if any(not name for name in names) or len(names) != len(set(names)):
        raise VerificationError("active builder names must be non-empty and unique")
    for row in rows:
        if not row.get("script") or not isinstance(row.get("ordinary_args"), list):
            raise VerificationError(
                f"builder {row.get('name')!r} needs script and ordinary_args"
            )
        for binding in row.get("environment_args") or []:
            if not binding.get("option") or not binding.get("variable"):
                raise VerificationError(
                    f"builder {row.get('name')!r} has an invalid environment binding"
                )
    return document


def validate_configuration(
    profiles: dict[str, Any],
    builder_policy: dict[str, Any],
) -> None:
    owned: dict[str, str] = {}
    active = set(profiles["active_profiles"])
    for profile_name in active:
        for builder in profiles["profiles"][profile_name]["builders"]:
            if builder in owned:
                raise VerificationError(
                    f"builder {builder!r} belongs to both "
                    f"{owned[builder]!r} and {profile_name!r}"
                )
            owned[builder] = profile_name
    policy_names = {row["name"] for row in builder_policy["builders"]}
    if set(owned) != policy_names:
        missing = sorted(policy_names - set(owned))
        extra = sorted(set(owned) - policy_names)
        raise VerificationError(
            f"profiles and builder policy disagree; missing={missing}, extra={extra}"
        )


def normalize_paths(paths: Iterable[str]) -> tuple[str, ...]:
    return tuple(
        sorted(
            {
                path.replace("\\", "/").removeprefix("./")
                for path in paths
                if path.strip()
            }
        )
    )


def path_matches(path: str, pattern: str) -> bool:
    return fnmatch.fnmatchcase(path, pattern)


def is_documentation_path(document: dict[str, Any], path: str) -> bool:
    patterns = document.get("documentation_path_patterns") or ["*.md", "docs/**"]
    return any(path_matches(path, pattern) for pattern in patterns)


def is_local_run_path(path: str) -> bool:
    return any(path_matches(path, pattern) for pattern in LOCAL_RUN_PATH_PATTERNS)


def is_historical_receipt_path(document: dict[str, Any], path: str) -> bool:
    patterns = document.get("historical_receipt_patterns") or []
    if any(path_matches(path, pattern) for pattern in patterns):
        return True
    from tools import material_form_authority as form_authority

    return form_authority.is_historical_receipt(path)


def classify_paths(
    document: dict[str, Any],
    paths: Iterable[str],
) -> dict[str, Any]:
    profiles = document["profiles"]
    active = set(document["active_profiles"])
    matched: dict[str, list[str]] = {name: [] for name in profiles if name in active}
    unmatched: list[str] = []
    documentation: list[str] = []
    for path in normalize_paths(paths):
        if is_local_run_path(path) or is_documentation_path(document, path):
            if is_documentation_path(document, path):
                documentation.append(path)
            continue
        if is_historical_receipt_path(document, path):
            continue
        hits = [
            name
            for name, profile in profiles.items()
            if name in active
            and any(path_matches(path, pattern) for pattern in profile["owned_paths"])
        ]
        if hits:
            for name in hits:
                matched[name].append(path)
        else:
            unmatched.append(path)
    return {
        "matched": {name: rows for name, rows in matched.items() if rows},
        "selected_profiles": [name for name in document["active_profiles"] if matched[name]],
        "documentation_paths": documentation,
        "unmatched_paths": unmatched,
    }


def _git_output(arguments: list[str]) -> bytes:
    completed = subprocess.run(
        ["git", *arguments],
        cwd=ROOT,
        capture_output=True,
        check=False,
    )
    return completed.stdout if completed.returncode == 0 else b""


def git_revision() -> str:
    return _git_output(["rev-parse", "HEAD"]).decode(
        "utf-8", errors="replace"
    ).strip()


def git_dirty_paths() -> tuple[str, ...]:
    payload = _git_output(
        ["status", "--porcelain=v1", "-z", "--untracked-files=all"]
    )
    paths: list[str] = []
    for record in payload.split(b"\0"):
        if not record:
            continue
        text = record.decode("utf-8", errors="replace")
        path = text[3:] if len(text) >= 3 else text
        if " -> " in path:
            path = path.split(" -> ", 1)[1]
        paths.append(path)
    return normalize_paths(paths)


def git_diff_names(*rev_args: str) -> tuple[str, ...]:
    payload = _git_output(["diff", "--name-only", "-z", *rev_args])
    return normalize_paths(
        item.decode("utf-8", errors="replace")
        for item in payload.split(b"\0")
        if item
    )


def changed_paths(explicit: list[str]) -> list[str]:
    if explicit:
        return list(normalize_paths(explicit))
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


def gradle_isolated() -> bool:
    value = os.environ.get("CRUCIBLECRAFT_GRADLE_ISOLATED", "").strip().lower()
    return value in {"1", "true", "yes"}


GAME_TEST_FAILURE = re.compile(r"(\w+)\s+failed(?:!| at\b)", re.I)
GAME_TEST_SUMMARY = re.compile(r"\]:\s+-\s+(\w+)\s*$")
SPLIT_DISPOSITION = (
    ROOT
    / "tools"
    / "waves"
    / "prep"
    / "test-authoring-workflow"
    / "disposition.json"
)


def release_game_test_grids() -> list[dict[str, Any]]:
    return [
        grid
        for grid in gametest_hygiene.load_grids(ROOT)["grids"]
        if grid.get("release", True)
    ]


def split_failure_ids() -> set[str]:
    """Non-blocking splits recorded for this card. Not a historical receipt."""
    if not SPLIT_DISPOSITION.is_file():
        return set()
    document = json.loads(SPLIT_DISPOSITION.read_text(encoding="utf-8"))
    return {
        str(entry.get("test_id") or "").lower()
        for entry in document.get("entries") or []
        if entry.get("class") == "split" and entry.get("blocks") is False
    }


def source_game_test_minimum(namespaces: list[str]) -> int:
    wanted = set(namespaces)
    return sum(
        1
        for row in gametest_hygiene.iter_tests(ROOT)
        if row.get("namespace") in wanted
    )


def parse_game_test_log(
    text: str,
    *,
    namespace: str,
    minimum: int,
    allowed_failures: set[str] | None = None,
) -> dict[str, Any]:
    allowed = {name.lower() for name in (allowed_failures or set())}
    crashed = (
        "---- Minecraft Crash Report ----" in text
        or "Negative index in crash" in text
    )
    discovered = None
    failed = None
    event_names: list[str] = []
    summary_events: list[str] = []
    for line in text.splitlines():
        lower = line.lower()
        running = re.search(r"(\d+) tests are now running", lower)
        if running:
            discovered = int(running.group(1))
        complete = re.search(r"(\d+) game tests complete", lower)
        if complete:
            discovered = int(complete.group(1))
        passed = re.search(r"all (\d+) required tests passed", lower)
        if passed:
            discovered = int(passed.group(1))
            failed = 0
        failed_line = re.search(r"(\d+) required tests failed", lower)
        if failed_line:
            failed = int(failed_line.group(1))
        named = GAME_TEST_FAILURE.search(line)
        if named and ("failed!" in lower or "failed at " in lower):
            token = named.group(1).split(":")[-1].split(".")[-1].lower()
            if token not in event_names:
                event_names.append(token)
        summary = GAME_TEST_SUMMARY.search(line)
        if summary:
            summary_events.append(summary.group(1).lower())
    reported = summary_events if summary_events else event_names
    failed_names = list(dict.fromkeys(reported))
    if not text:
        return {
            "status": "FAIL",
            "namespace": namespace,
            "reason": "missing current GameTestServer log",
        }
    if crashed or discovered is None or failed is None:
        return {
            "status": "FAIL",
            "namespace": namespace,
            "discovered": discovered,
            "failed": failed,
            "failed_names": failed_names,
            "crashed": crashed,
            "reason": "server crash or missing GameTest summary",
        }
    passed_count = discovered - failed
    unexecuted = max(0, minimum - discovered)
    unexpected = [name for name in failed_names if name not in allowed]
    hidden = failed > 0 and not failed_names
    # A clean run is failed == 0. A split run may have failed > 0 only when
    # every parsed failure id is an explicit non-blocking split.
    if failed == 0:
        ok = not crashed and discovered >= minimum and passed_count == discovered
    else:
        ok = (
            not crashed
            and not hidden
            and not unexpected
            and discovered >= minimum
            and len(reported) >= failed
            and set(failed_names).issubset(allowed)
        )
    split = sorted(name for name in failed_names if name in allowed)
    return {
        "status": "PASS" if ok else "FAIL",
        "namespace": namespace,
        "discovered": discovered,
        "passed": discovered - failed,
        "failed": failed,
        "failed_names": failed_names,
        "split_failures": split,
        "crashed": crashed,
        "unexecuted": unexecuted,
        "minimum": minimum,
        "reason": None
        if ok
        else "grid did not execute every required test",
    }


def _forces_every_game_test_grid(path: str) -> bool:
    normalized = path.replace("\\", "/")
    return normalized.startswith((
        "src/main/",
        "gradle/",
        "src/test/resources/",
    ))


def game_test_grids_for(
    changed: list[str] | None,
    *,
    command: str,
) -> list[dict[str, Any]]:
    grids = release_game_test_grids()
    if command == "release" or changed is None:
        return grids
    if any(_forces_every_game_test_grid(path) for path in changed):
        return grids
    by_file: dict[str, str] = {}
    for row in gametest_hygiene.iter_tests(ROOT):
        if row.get("namespace"):
            by_file[str(row["file"])] = str(row["namespace"])
    selected: list[str] = []
    saw_java = False
    for path in changed:
        normalized = path.replace("\\", "/")
        if not normalized.endswith(".java"):
            continue
        saw_java = True
        namespace = by_file.get(normalized)
        if namespace is None:
            return grids
        for grid in grids:
            if (
                namespace in gametest_hygiene.grid_namespaces(grid)
                and grid["id"] not in selected
            ):
                selected.append(grid["id"])
    if not saw_java or not selected:
        return grids
    return [grid for grid in grids if grid["id"] in selected]


def run_game_test_grids(
    changed: list[str] | None,
    command: str,
    receipt: dict[str, Any],
) -> tuple[int, dict[str, Any]]:
    allowed = split_failure_ids()
    results: list[dict[str, Any]] = []
    failed = False
    for grid in game_test_grids_for(changed, command=command):
        namespaces = gametest_hygiene.grid_namespaces(grid)
        code = run_command(
            f"gradle:runGameTestServer:{grid['id']}",
            gradle_command(
                "runGameTestServer",
                [f"-PgameTestGrid={grid['id']}"],
            ),
            receipt,
        )
        log_path = ROOT / f"run-game-test-{grid['id']}" / "logs" / "latest.log"
        text = (
            log_path.read_text(encoding="utf-8", errors="replace")
            if log_path.is_file()
            else ""
        )
        parsed = parse_game_test_log(
            text,
            namespace=",".join(namespaces),
            minimum=source_game_test_minimum(namespaces),
            allowed_failures=allowed,
        )
        parsed["id"] = grid["id"]
        parsed["gradle_exit_code"] = code
        results.append(parsed)
        if parsed["status"] != "PASS":
            failed = True
            print(
                f"game test grid {grid['id']} "
                + str(parsed.get("reason") or "did not pass"),
                file=sys.stderr,
                flush=True,
            )
    summary = {
        "status": "FAIL" if failed or not results else "PASS",
        "grids": results,
    }
    return (1 if summary["status"] != "PASS" else 0), summary


def gradle_command(task: str, extra: list[str] | None = None) -> list[str]:
    wrapper = "gradlew.bat" if os.name == "nt" else "./gradlew"
    command = [
        str(ROOT / wrapper),
        task,
        "--max-workers=1",
        "--rerun",
    ]
    if extra:
        command.extend(extra)
    if gradle_isolated():
        command.append("--no-daemon")
    return command


def has_external_diff_base() -> bool:
    if os.environ.get("CRUCIBLECRAFT_DIFF_BASE", "").strip():
        return True
    if os.environ.get("GITHUB_BASE_REF", "").strip():
        return True
    before = os.environ.get("GITHUB_EVENT_BEFORE", "").strip()
    return bool(before) and set(before) != {"0"}


def promotion_diff_base() -> str:
    explicit = os.environ.get("CRUCIBLECRAFT_DIFF_BASE", "").strip()
    if explicit:
        return explicit
    github_base = os.environ.get("GITHUB_BASE_REF", "").strip()
    if github_base:
        return f"origin/{github_base}"
    return "HEAD"


def python_modules_for_changed_paths(
    profile: dict[str, Any],
    changed: list[str] | None,
) -> list[str]:
    """On path-scoped runs, keep only modules that the changed paths select."""
    declared = [str(name) for name in profile.get("python_modules") or []]
    if changed is None:
        return declared
    affected, _unmatched = python_tests.affected_module_names(
        python_tests.load_policy(),
        changed,
    )
    wanted = set(affected)
    return [name for name in declared if name in wanted]


def gradle_full_suite_errors(summary: dict[str, Any]) -> list[str]:
    """Missing or failed Gradle XML is not a pass."""
    errors: list[str] = []
    if not summary.get("xml_present"):
        errors.append("gradle test xml missing")
    if int(summary.get("tests") or 0) < 1:
        errors.append("gradle test count is empty")
    if int(summary.get("failures") or 0) or int(summary.get("errors") or 0):
        errors.append("gradle tests failed")
    return errors


def parse_gradle_test_xml(results_dir: Path = TEST_RESULTS) -> dict[str, Any]:
    tests = failures = errors = skipped = 0
    files = sorted(results_dir.glob("TEST-*.xml")) if results_dir.is_dir() else []
    for path in files:
        root = ET.parse(path).getroot()
        tests += int(root.attrib.get("tests") or 0)
        failures += int(root.attrib.get("failures") or 0)
        errors += int(root.attrib.get("errors") or 0)
        skipped += int(root.attrib.get("skipped") or 0)
    return {
        "xml_present": bool(files),
        "tests": tests,
        "failures": failures,
        "errors": errors,
        "skipped": skipped,
    }


def run_datagen(
    profile_name: str,
    receipt: dict[str, Any],
) -> tuple[int, dict[str, Any]]:
    root = ROOT / "build" / "verification" / "datagen" / profile_name
    first = root / "first"
    second = root / "second"
    for directory in (first, second):
        shutil.rmtree(directory, ignore_errors=True)
    for label, directory in (("first", first), ("second", second)):
        command = gradle_command("runData")
        command.append(f"-PdatagenOutput={directory}")
        code = run_command(f"datagen:{label}", command, receipt)
        if code:
            return code, {"status": "FAIL", "failed_run": label}
    deterministic_errors = tree_compare.compare_trees(first, second)
    from tools.generated_resource_gate import PYTHON_OWNED_GENERATED_PREFIXES

    committed_errors = tree_compare.compare_trees(
        ROOT / "src" / "generated" / "resources",
        first,
        ignored_prefixes=PYTHON_OWNED_GENERATED_PREFIXES,
    )
    result = {
        "status": (
            "PASS"
            if not deterministic_errors and not committed_errors
            else "FAIL"
        ),
        "generated_files": len(tree_compare.file_map(first)),
        "deterministic_errors": deterministic_errors[:50],
        "committed_errors": committed_errors[:50],
    }
    if result["status"] == "FAIL":
        for error in deterministic_errors:
            print(f"datagen repeat mismatch: {error}", file=sys.stderr)
        for error in committed_errors:
            print(f"datagen committed mismatch: {error}", file=sys.stderr)
        return 1, result
    return 0, result


def _receipt(argv: list[str]) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "status": "RUNNING",
        "git_revision": git_revision(),
        "dirty_paths": list(git_dirty_paths()),
        "command": [sys.executable, "tools/verify.py", *argv],
        "commands": [],
        "results": [],
        "profiles": [],
        "environment": {
            "ci": os.environ.get("CI", ""),
            "cwd": str(ROOT),
            "operating_system": platform.platform(),
            "python_executable": sys.executable,
            "python_version": platform.python_version(),
        },
    }


def write_latest_receipt(document: dict[str, Any]) -> None:
    LATEST.parent.mkdir(parents=True, exist_ok=True)
    LATEST.write_text(
        json.dumps(document, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def run_command(
    name: str,
    command: list[str],
    receipt: dict[str, Any],
) -> int:
    print(f"[{name}] {subprocess.list2cmdline(command)}", flush=True)
    receipt["commands"].append({"name": name, "argv": list(command)})
    started = time.perf_counter()
    try:
        completed = subprocess.run(command, cwd=ROOT, check=False)
        code = completed.returncode
    except OSError as error:
        print(f"[{name}] failed to start: {error}", file=sys.stderr, flush=True)
        code = 127
    seconds = round(time.perf_counter() - started, 3)
    result = {
        "name": name,
        "exit_code": code,
        "seconds": seconds,
        "status": "PASS" if code == 0 else "FAIL",
    }
    receipt["results"].append(result)
    print(f"[{name}] {result['status']} ({seconds:.3f}s)", flush=True)
    return code


def builder_rows_for_profile(
    profile_name: str,
    profiles: dict[str, Any],
    builder_policy: dict[str, Any],
) -> list[dict[str, Any]]:
    wanted = set(profiles["profiles"][profile_name]["builders"])
    return [row for row in builder_policy["builders"] if row["name"] in wanted]


def builder_command(
    row: dict[str, Any],
    extra_args: list[str] | None = None,
) -> list[str]:
    command = [
        sys.executable,
        row["script"],
        *row["ordinary_args"],
        *(extra_args or []),
    ]
    for binding in row.get("environment_args") or []:
        variable = str(binding.get("variable") or "")
        option = str(binding.get("option") or "")
        value = os.environ.get(variable, "").strip()
        if value:
            command.extend([option, value])
        elif binding.get("required"):
            raise VerificationError(
                f"builder {row['name']!r} requires environment variable {variable}"
            )
    return command


def run_profile(
    profile_name: str,
    profiles: dict[str, Any],
    builder_policy: dict[str, Any],
    receipt: dict[str, Any],
    *,
    command: str = "integration",
    changed: list[str] | None = None,
) -> int:
    profile = profiles["profiles"][profile_name]
    profile_result: dict[str, Any] = {
        "name": profile_name,
        "status": "RUNNING",
    }
    receipt["profiles"].append(profile_result)
    print(f"verify profile {profile_name} (owner={profile['owner']})")
    for row in builder_rows_for_profile(profile_name, profiles, builder_policy):
        code = run_command(
            f"builder:{row['name']}",
            builder_command(row),
            receipt,
        )
        if code:
            profile_result["status"] = "FAIL"
            return code
    modules = python_modules_for_changed_paths(profile, changed)
    if modules:
        result_path = (
            ROOT
            / "build"
            / "verification"
            / "python"
            / f"{profile_name}.json"
        )
        result_path.unlink(missing_ok=True)
        python_command = [
            sys.executable,
            "tools/run_python_tests.py",
            "--suite",
            "modules",
            "--result-json",
            str(result_path),
        ]
        for module in modules:
            python_command.extend(["--module", module])
        code = run_command(f"python:{profile_name}", python_command, receipt)
        if result_path.is_file():
            profile_result["python_test"] = load_json(result_path)
        if code:
            profile_result["status"] = "FAIL"
            return code
    for task in profile["gradle_tasks"]:
        if task == "runGameTestServer":
            code, summary = run_game_test_grids(changed, command, receipt)
            profile_result["game_tests"] = summary
            if code:
                profile_result["status"] = "FAIL"
                return code
            continue
        code = run_command(f"gradle:{task}", gradle_command(task), receipt)
        if task == "test":
            summary = parse_gradle_test_xml()
            profile_result["gradle_test"] = summary
            if code == 0 and gradle_full_suite_errors(summary):
                code = 1
        if code:
            profile_result["status"] = "FAIL"
            return code
    if profile.get("datagen"):
        code, datagen_result = run_datagen(profile_name, receipt)
        profile_result["datagen"] = datagen_result
        if code:
            profile_result["status"] = "FAIL"
            return code
    profile_result["status"] = "PASS"
    return 0


def _configuration() -> tuple[dict[str, Any], dict[str, Any]]:
    profiles = load_profiles()
    builders = load_builder_policy()
    validate_configuration(profiles, builders)
    return profiles, builders


def cmd_dev(args: argparse.Namespace, receipt: dict[str, Any]) -> int:
    profiles, builders = _configuration()
    paths = changed_paths(args.path)
    classification = classify_paths(profiles, paths)
    if classification["unmatched_paths"]:
        print(
            "unmatched paths (reported, not blocking): "
            + ", ".join(classification["unmatched_paths"]),
            file=sys.stderr,
        )
    selected = classification["selected_profiles"]
    if not selected:
        print("no active profile owns the changed paths")
        return 0
    for profile_name in selected:
        code = run_profile(
            profile_name,
            profiles,
            builders,
            receipt,
            command="dev",
            changed=paths,
        )
        if code:
            return code
    return 0


def cmd_integration(args: argparse.Namespace, receipt: dict[str, Any]) -> int:
    profiles, builders = _configuration()
    if args.profile not in profiles["active_profiles"]:
        print(
            f"inactive or unknown profile {args.profile!r}; available: "
            + ", ".join(profiles["active_profiles"]),
            file=sys.stderr,
        )
        return 2
    changed = changed_paths([]) if has_external_diff_base() else None
    if args.if_changed:
        if changed is None:
            print(
                f"verify profile {args.profile}: --if-changed has no diff base; running",
                flush=True,
            )
        else:
            classification = classify_paths(profiles, changed)
            if args.profile not in classification["selected_profiles"]:
                print(
                    f"verify profile {args.profile}: skip (no owned path in the diff)",
                    flush=True,
                )
                receipt["profiles"].append(
                    {
                        "name": args.profile,
                        "status": "SKIP",
                        "reason": "no owned path in the diff",
                    }
                )
                return 0
    return run_profile(
        args.profile,
        profiles,
        builders,
        receipt,
        command="integration",
        changed=changed,
    )


def cmd_release(_args: argparse.Namespace, receipt: dict[str, Any]) -> int:
    previous = os.environ.get("CRUCIBLECRAFT_GRADLE_ISOLATED")
    os.environ["CRUCIBLECRAFT_GRADLE_ISOLATED"] = "1"
    try:
        profiles, builders = _configuration()
        for profile_name in profiles["release_profiles"]:
            code = run_profile(
                profile_name,
                profiles,
                builders,
                receipt,
                command="release",
            )
            if code:
                return code
        return 0
    finally:
        if previous is None:
            os.environ.pop("CRUCIBLECRAFT_GRADLE_ISOLATED", None)
        else:
            os.environ["CRUCIBLECRAFT_GRADLE_ISOLATED"] = previous


def cmd_promotion(args: argparse.Namespace, receipt: dict[str, Any]) -> int:
    del args
    receipt["promotion"] = {
        "base": None,
        "slugs": [],
        "note": "player_complete promotions abolished; CI does not auto-run runClient",
    }
    print("player_complete promotions abolished; CI does not auto-run runClient")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)

    dev = sub.add_parser("dev", help="Fresh verification for changed paths")
    dev.add_argument("--path", action="append", default=[])
    dev.set_defaults(func=cmd_dev)

    integration = sub.add_parser(
        "integration",
        help="Fresh verification for one active profile",
    )
    integration.add_argument("--profile", required=True)
    integration.add_argument(
        "--if-changed",
        action="store_true",
        help="Skip when a diff base exists and no owned path changed",
    )
    integration.set_defaults(func=cmd_integration)

    release = sub.add_parser(
        "release",
        help="Fresh verification for all release profiles",
    )
    release.set_defaults(func=cmd_release)

    promotion = sub.add_parser(
        "promotion",
        help="No-op: player_complete promotions are abolished",
    )
    promotion.add_argument(
        "--base",
        help="Git revision to compare capability maturity against",
    )
    promotion.set_defaults(func=cmd_promotion)
    return parser


def main(argv: list[str] | None = None) -> int:
    actual = list(sys.argv[1:] if argv is None else argv)
    parser = build_parser()
    args = parser.parse_args(actual)
    receipt = _receipt(actual)
    try:
        code = args.func(args, receipt)
    except (OSError, json.JSONDecodeError, VerificationError) as error:
        print(f"verification configuration failed: {error}", file=sys.stderr)
        receipt["results"].append(
            {
                "name": "configuration",
                "exit_code": 2,
                "status": "FAIL",
                "error": str(error),
            }
        )
        code = 2
    receipt["status"] = "PASS" if code == 0 else "FAIL"
    receipt["exit_code"] = code
    write_latest_receipt(receipt)
    return code


if __name__ == "__main__":
    raise SystemExit(main())
