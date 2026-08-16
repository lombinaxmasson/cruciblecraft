#!/usr/bin/env python3
"""Run auditable fast, affected, closure, or source-replay Python suites."""
from __future__ import annotations

import argparse
import fnmatch
import json
import os
import subprocess
import sys
import time
import unittest
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
TEST_ROOT = TOOLS / "tests"
POLICY = TOOLS / "python_test_policy.json"
BUILDER_POLICY = TOOLS / "verification_builder_policy.json"
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))


class PolicyError(ValueError):
    """The workflow policy cannot prove a safe test selection."""


@dataclass(frozen=True)
class Selection:
    suite: str
    cases: tuple[unittest.TestCase, ...]
    changed_paths: tuple[str, ...] = ()
    escalated_to_closure: bool = False
    escalation_paths: tuple[str, ...] = ()


def load_policy(path: Path = POLICY) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    if document.get("schema_version") != 1:
        raise PolicyError("unsupported Python test policy schema")
    required = {
        "closure_only_test_patterns",
        "source_replay_test_patterns",
        "prechecked_stage_test_patterns",
        "affected_rules",
        "source_replay_commands",
    }
    missing = sorted(required - set(document))
    if missing:
        raise PolicyError(f"Python test policy omits {missing}")
    return document


def source_replay_records(policy: dict[str, Any]) -> list[dict[str, Any]]:
    builder_policy = json.loads(BUILDER_POLICY.read_text(encoding="utf-8"))
    if builder_policy.get("schema_version") != 1:
        raise PolicyError("unsupported verification builder policy schema")
    records: list[dict[str, Any]] = []
    for builder in builder_policy.get("builders") or []:
        replay = builder.get("full_replay")
        if not replay:
            continue
        records.append({
            "name": f"{builder['name']} full replay",
            "required_paths": replay.get("required_paths", []),
            "required_any_paths": replay.get("required_any_paths", []),
            "command": [
                "$PYTHON",
                builder["script"],
                *replay["args"],
            ],
        })
    records.extend(policy["source_replay_commands"])
    return records


def flatten_suite(
    suite: unittest.TestSuite | unittest.TestCase,
) -> list[unittest.TestCase]:
    if isinstance(suite, unittest.TestSuite):
        cases: list[unittest.TestCase] = []
        for child in suite:
            cases.extend(flatten_suite(child))
        return cases
    return [suite]


def discover_cases() -> list[unittest.TestCase]:
    suite = unittest.defaultTestLoader.discover(
        str(TEST_ROOT),
        pattern="test_*.py",
    )
    cases = flatten_suite(suite)
    failed_imports = [
        (case.id(), repr(getattr(case, "_exception", None)))
        for case in cases
        if case.__class__.__name__ == "_FailedTest"
    ]
    if failed_imports:
        raise PolicyError(
            f"unittest discovery contains failed imports: {failed_imports}"
        )
    ids = [case.id() for case in cases]
    duplicates = sorted({test_id for test_id in ids if ids.count(test_id) > 1})
    if duplicates:
        raise PolicyError(f"unittest discovery produced duplicate ids: {duplicates}")
    return cases


def normalized_test_id(case_or_id: unittest.TestCase | str) -> str:
    test_id = case_or_id if isinstance(case_or_id, str) else case_or_id.id()
    parts = test_id.split(".")
    for index, part in enumerate(parts):
        if part.startswith("test_"):
            return ".".join(parts[index:])
    return test_id


def test_module(case_or_id: unittest.TestCase | str) -> str:
    return normalized_test_id(case_or_id).split(".", 1)[0]


def matches_any(test_id: str, patterns: Iterable[str]) -> bool:
    normalized = normalized_test_id(test_id)
    return any(fnmatch.fnmatchcase(normalized, pattern) for pattern in patterns)


def validate_policy(
    policy: dict[str, Any],
    cases: Iterable[unittest.TestCase],
) -> None:
    closure = policy["closure_only_test_patterns"]
    replay = policy["source_replay_test_patterns"]
    overlap = sorted(
        normalized_test_id(case)
        for case in cases
        if matches_any(case.id(), closure) and matches_any(case.id(), replay)
    )
    if overlap:
        raise PolicyError(
            "tests cannot be both closure-only and source-replay: "
            + ", ".join(overlap)
        )
    for patterns in policy["prechecked_stage_test_patterns"].values():
        unsafe = sorted(
            normalized_test_id(case)
            for case in cases
            if matches_any(case.id(), patterns)
            and any(
                token in normalized_test_id(case).lower()
                for token in ("mutation", "tamper", "reject", "fail_closed")
            )
        )
        if unsafe:
            raise PolicyError(
                "prechecked stages cannot skip mutation/tamper tests: "
                + ", ".join(unsafe)
            )
    for rule in policy["affected_rules"]:
        if not rule.get("paths") or not rule.get("test_modules"):
            raise PolicyError("every affected rule needs paths and test_modules")


def normalize_paths(paths: Iterable[str]) -> tuple[str, ...]:
    return tuple(sorted({
        path.replace("\\", "/").removeprefix("./")
        for path in paths
        if path.strip()
    }))


def git_changed_paths() -> tuple[str, ...]:
    commands = (
        ["git", "diff", "--name-only", "-z", "HEAD"],
        ["git", "diff", "--name-only", "-z", "--cached"],
        ["git", "ls-files", "--others", "--exclude-standard", "-z"],
    )
    paths: list[str] = []
    for command in commands:
        completed = subprocess.run(
            command,
            cwd=ROOT,
            capture_output=True,
            check=False,
        )
        if completed.returncode != 0:
            raise PolicyError(
                f"cannot derive affected paths from {' '.join(command)}: "
                f"{completed.stderr.decode('utf-8', errors='replace').strip()}"
            )
        paths.extend(
            item.decode("utf-8")
            for item in completed.stdout.split(b"\0")
            if item
        )
    return normalize_paths(paths)


def affected_modules_for_path(
    policy: dict[str, Any],
    path: str,
) -> set[str] | None:
    modules: set[str] = set()
    matched = False
    for rule in policy["affected_rules"]:
        if not any(
            fnmatch.fnmatchcase(path, pattern)
            for pattern in rule["paths"]
        ):
            continue
        matched = True
        for module in rule["test_modules"]:
            modules.add(Path(path).stem if module == "$changed_test_module" else module)
    return modules if matched else None


def select_cases(
    suite: str,
    policy: dict[str, Any],
    cases: list[unittest.TestCase],
    *,
    changed_paths: Iterable[str] = (),
) -> Selection:
    validate_policy(policy, cases)
    closure_patterns = policy["closure_only_test_patterns"]
    replay_patterns = policy["source_replay_test_patterns"]
    if suite == "closure":
        return Selection(suite, tuple(cases))
    if suite == "fast":
        selected = tuple(
            case
            for case in cases
            if not matches_any(case.id(), closure_patterns)
            and not matches_any(case.id(), replay_patterns)
        )
        return Selection(suite, selected)
    if suite == "source-replay":
        selected = tuple(
            case for case in cases if matches_any(case.id(), replay_patterns)
        )
        return Selection(suite, selected)
    if suite != "affected":
        raise PolicyError(f"unknown suite {suite}")

    normalized_paths = normalize_paths(changed_paths)
    if not normalized_paths:
        fast = select_cases("fast", policy, cases)
        return Selection(suite, fast.cases)
    selected_modules: set[str] = set()
    unknown: list[str] = []
    for path in normalized_paths:
        modules = affected_modules_for_path(policy, path)
        if modules is None:
            unknown.append(path)
        else:
            selected_modules.update(modules)
    if unknown:
        return Selection(
            suite,
            tuple(cases),
            normalized_paths,
            escalated_to_closure=True,
            escalation_paths=tuple(sorted(unknown)),
        )
    selected = tuple(
        case for case in cases if test_module(case) in selected_modules
    )
    if selected_modules and not selected:
        raise PolicyError(
            f"affected rules selected missing test modules: {sorted(selected_modules)}"
        )
    return Selection(suite, selected, normalized_paths)


def ordered_cases(
    selection: Selection,
    policy: dict[str, Any],
) -> list[unittest.TestCase]:
    closure = policy["closure_only_test_patterns"]
    replay = policy["source_replay_test_patterns"]
    return sorted(
        selection.cases,
        key=lambda case: (
            matches_any(case.id(), replay),
            matches_any(case.id(), closure),
            normalized_test_id(case),
        ),
    )


def apply_prechecked_stage_skips(
    cases: Iterable[unittest.TestCase],
    policy: dict[str, Any],
) -> None:
    """Mark only policy-listed duplicate positive currentness tests skipped."""
    for environment_name, patterns in policy[
        "prechecked_stage_test_patterns"
    ].items():
        if os.environ.get(environment_name) != "1":
            continue
        for case in cases:
            if not matches_any(case.id(), patterns):
                continue
            method = getattr(type(case), case._testMethodName)
            method.__unittest_skip__ = True
            method.__unittest_skip_why__ = (
                f"{environment_name} evidence was recorded by the orchestrator"
            )


def apply_source_replay_skips(
    cases: Iterable[unittest.TestCase],
    policy: dict[str, Any],
    suite: str,
) -> None:
    """Keep raw/cache replays visible but skipped outside source-replay."""
    if suite == "source-replay":
        return
    patterns = policy["source_replay_test_patterns"]
    for case in cases:
        if not matches_any(case.id(), patterns):
            continue
        method = getattr(type(case), case._testMethodName)
        method.__unittest_skip__ = True
        method.__unittest_skip_why__ = (
            "raw/cache replay is reserved for the source-replay suite"
        )


def run_source_replay_commands(policy: dict[str, Any]) -> tuple[int, int]:
    passed = 0
    skipped = 0
    for record in source_replay_records(policy):
        required = [ROOT / path for path in record.get("required_paths", ())]
        required_any = [
            ROOT / path for path in record.get("required_any_paths", ())
        ]
        unavailable = any(not path.is_file() for path in required) or (
            required_any and not any(path.is_file() for path in required_any)
        )
        if unavailable:
            skipped += 1
            print(f"SKIP source replay: {record['name']} (required input missing)")
            continue
        command = [
            sys.executable if argument == "$PYTHON" else argument
            for argument in record["command"]
        ]
        print(f"RUN source replay: {record['name']}")
        completed = subprocess.run(command, cwd=ROOT, check=False)
        if completed.returncode != 0:
            return completed.returncode, skipped
        passed += 1
    print(f"Source replay commands: {passed} passed, {skipped} skipped")
    return 0, skipped


def write_result(
    path: Path,
    *,
    selection: Selection,
    result: unittest.TestResult,
    elapsed_seconds: float,
    success: bool,
    source_replay_skipped: int,
) -> None:
    skipped_tests = getattr(result, "skipped", ())
    class_level_skips = sum(
        1 for case, _reason in skipped_tests
        if not hasattr(case, "_testMethodName")
    )
    skip_reasons: list[str] = []
    for _case, reason in skipped_tests:
        if reason and str(reason).strip():
            skip_reasons.append(str(reason).strip())
    document = {
        "schema_version": 1,
        "suite": selection.suite,
        "selected_tests": len(selection.cases),
        "tests_run": result.testsRun,
        "failures": len(result.failures),
        "errors": len(result.errors),
        "skipped": len(skipped_tests),
        "class_level_skips": class_level_skips,
        "skip_reasons": skip_reasons,
        "elapsed_seconds": round(elapsed_seconds, 3),
        "success": success,
        "escalated_to_closure": selection.escalated_to_closure,
        "escalation_paths": list(selection.escalation_paths),
        "source_replay_commands_skipped": source_replay_skipped,
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--suite",
        choices=("fast", "affected", "closure", "source-replay"),
        default=None,
    )
    parser.add_argument("--path", action="append", default=[])
    parser.add_argument("--durations", type=int, default=20)
    parser.add_argument("--failfast", action="store_true")
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--result-json", type=Path)
    args = parser.parse_args(argv)
    try:
        policy = load_policy()
        suite = args.suite or policy["default_suite"]
        cases = discover_cases()
        changed_paths = args.path
        if suite == "affected" and not changed_paths:
            changed_paths = list(git_changed_paths())
        selection = select_cases(
            suite,
            policy,
            cases,
            changed_paths=changed_paths,
        )
        selected = ordered_cases(selection, policy)
        apply_prechecked_stage_skips(selected, policy)
        apply_source_replay_skips(selected, policy, suite)
    except (OSError, json.JSONDecodeError, PolicyError) as exc:
        print(f"Python test workflow policy failed: {exc}", file=sys.stderr)
        return 2

    if selection.escalated_to_closure:
        print(
            "Affected selection escalated to closure for unknown paths: "
            + ", ".join(selection.escalation_paths)
        )
    print(
        f"Python suite {suite}: selected {len(selected)} / {len(cases)} tests"
    )
    if args.list:
        print("\n".join(normalized_test_id(case) for case in selected))
        return 0
    if not selected and suite != "source-replay":
        print("Python test workflow selected no tests", file=sys.stderr)
        return 2

    started = time.perf_counter()
    runner = unittest.TextTestRunner(
        verbosity=2,
        failfast=args.failfast,
        durations=max(0, args.durations),
    )
    result = runner.run(unittest.TestSuite(selected))
    source_replay_skipped = 0
    replay_code = 0
    if result.wasSuccessful() and suite == "source-replay":
        replay_code, source_replay_skipped = run_source_replay_commands(policy)
    elapsed = time.perf_counter() - started
    success = result.wasSuccessful() and replay_code == 0
    if args.result_json:
        write_result(
            args.result_json,
            selection=selection,
            result=result,
            elapsed_seconds=elapsed,
            success=success,
            source_replay_skipped=source_replay_skipped,
        )
    return 0 if success else (replay_code or 1)


if __name__ == "__main__":
    raise SystemExit(main())
