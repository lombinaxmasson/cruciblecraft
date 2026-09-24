#!/usr/bin/env python3
"""Run only Python test modules declared by active verification policy."""
from __future__ import annotations

import argparse
import fnmatch
import json
import subprocess
import sys
import time
import unittest
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "python_test_policy.json"
PROFILES = TOOLS / "verification_profiles.json"

if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))


class PolicyError(ValueError):
    """The active test policy cannot prove a safe selection."""


@dataclass(frozen=True)
class Selection:
    suite: str
    cases: tuple[unittest.TestCase, ...]
    changed_paths: tuple[str, ...] = ()
    unmatched_paths: tuple[str, ...] = ()


def load_policy(path: Path = POLICY) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    if document.get("schema_version") != 2:
        raise PolicyError("unsupported Python test policy schema")
    required = {
        "default_suite",
        "active_test_modules",
        "affected_rules",
        "test_tiers",
    }
    missing = sorted(required - set(document))
    if missing:
        raise PolicyError(f"Python test policy omits {missing}")
    validate_policy(document)
    return document


def load_profiles(path: Path = PROFILES) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    if document.get("schema_version") != 2:
        raise PolicyError("unsupported verification profile schema")
    return document


def normalize_module_names(modules: Iterable[str]) -> tuple[str, ...]:
    names: list[str] = []
    seen: set[str] = set()
    for raw in modules:
        stem = Path(str(raw).replace("\\", "/")).stem
        if not stem or stem in seen:
            continue
        seen.add(stem)
        names.append(stem)
    return tuple(names)


def profile_test_modules(profiles: dict[str, Any] | None = None) -> tuple[str, ...]:
    document = profiles if profiles is not None else load_profiles()
    names: list[str] = []
    for profile_name in document.get("active_profiles") or []:
        profile = (document.get("profiles") or {}).get(profile_name)
        if not isinstance(profile, dict):
            raise PolicyError(f"active profile {profile_name!r} is missing")
        names.extend(profile.get("python_modules") or [])
    return normalize_module_names(names)


def active_module_names(
    policy: dict[str, Any] | None = None,
    profiles: dict[str, Any] | None = None,
) -> tuple[str, ...]:
    document = policy if policy is not None else load_policy()
    declared = normalize_module_names(document["active_test_modules"])
    profiled = set(profile_test_modules(profiles))
    missing = sorted(profiled - set(declared))
    unprofiled = sorted(set(declared) - profiled)
    if missing or unprofiled:
        raise PolicyError(
            "active test policy and profiles disagree; "
            f"missing={missing}, unprofiled={unprofiled}"
        )
    return declared


def validate_policy(policy: dict[str, Any]) -> None:
    modules = normalize_module_names(policy.get("active_test_modules") or [])
    if not modules:
        raise PolicyError("active_test_modules must not be empty")
    if len(modules) != len(policy["active_test_modules"]):
        raise PolicyError("active_test_modules must be unique module stems")
    allowed = set(modules)
    tiers = policy.get("test_tiers")
    if not isinstance(tiers, dict):
        raise PolicyError("test_tiers must be an object")
    for key in ("manual_replay", "historical"):
        if key not in tiers:
            raise PolicyError(f"test_tiers omits {key}")
        names = normalize_module_names(tiers.get(key) or [])
        if len(names) != len(tiers[key]):
            raise PolicyError(f"test_tiers.{key} must be unique module stems")
        overlap = sorted(set(names) & allowed)
        if overlap:
            raise PolicyError(f"test_tiers.{key} overlaps active modules: {overlap}")
    manual = set(normalize_module_names(tiers["manual_replay"]))
    historical = set(normalize_module_names(tiers["historical"]))
    overlap = sorted(manual & historical)
    if overlap:
        raise PolicyError(
            f"test_tiers overlap between replay and historical: {overlap}"
        )
    for rule in policy.get("affected_rules") or []:
        if not rule.get("paths") or not rule.get("test_modules"):
            raise PolicyError("every affected rule needs paths and test_modules")
        unknown = sorted(set(rule["test_modules"]) - allowed)
        if unknown:
            raise PolicyError(
                f"affected rule reaches inactive test modules: {unknown}"
            )


def flatten_suite(
    suite: unittest.TestSuite | unittest.TestCase,
) -> list[unittest.TestCase]:
    if isinstance(suite, unittest.TestSuite):
        cases: list[unittest.TestCase] = []
        for child in suite:
            cases.extend(flatten_suite(child))
        return cases
    return [suite]


def discover_cases(
    modules: Iterable[str] | None = None,
    *,
    policy: dict[str, Any] | None = None,
) -> list[unittest.TestCase]:
    document = policy if policy is not None else load_policy()
    allowed = active_module_names(document)
    requested = normalize_module_names(allowed if modules is None else modules)
    inactive = sorted(set(requested) - set(allowed))
    if inactive:
        raise PolicyError(
            f"test modules are not active and will not be imported: {inactive}"
        )
    names = [f"tools.tests.{name}" for name in requested]
    suite = unittest.defaultTestLoader.loadTestsFromNames(names)
    cases = flatten_suite(suite)
    failed_imports = [
        (case.id(), repr(getattr(case, "_exception", None)))
        for case in cases
        if case.__class__.__name__ == "_FailedTest"
    ]
    if failed_imports:
        raise PolicyError(f"active test modules failed to import: {failed_imports}")
    excluded = document.get("excluded_test_patterns") or []
    cases = [case for case in cases if not matches_any(case, excluded)]
    ids = [case.id() for case in cases]
    duplicates = sorted({test_id for test_id in ids if ids.count(test_id) > 1})
    if duplicates:
        raise PolicyError(f"active test loading produced duplicate ids: {duplicates}")
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


def matches_any(
    case_or_id: unittest.TestCase | str,
    patterns: Iterable[str],
) -> bool:
    normalized = normalized_test_id(case_or_id)
    return any(fnmatch.fnmatchcase(normalized, pattern) for pattern in patterns)


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


def read_path_file(path: Path) -> tuple[str, ...]:
    return normalize_paths(
        line.strip()
        for line in path.read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.lstrip().startswith("#")
    )


def resolve_suite_paths(
    *,
    suite: str,
    path_args: Iterable[str] = (),
    path_file: Path | None = None,
) -> tuple[str, ...]:
    paths = list(path_args)
    if path_file is not None:
        paths.extend(read_path_file(path_file))
    normalized = normalize_paths(paths)
    if suite == "affected" and not normalized:
        return git_changed_paths()
    return normalized


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
            item.decode("utf-8", errors="replace")
            for item in completed.stdout.split(b"\0")
            if item
        )
    return normalize_paths(paths)


def is_documentation_path(policy: dict[str, Any], path: str) -> bool:
    patterns = policy.get("documentation_path_patterns") or ("*.md", "docs/**")
    return any(fnmatch.fnmatchcase(path, pattern) for pattern in patterns)


def profile_owns_path(path: str) -> bool:
    document = load_profiles()
    active = set(document.get("active_profiles") or [])
    for name, profile in (document.get("profiles") or {}).items():
        if name not in active:
            continue
        if any(
            fnmatch.fnmatchcase(path, pattern)
            for pattern in profile.get("owned_paths") or ()
        ):
            return True
    return False


def affected_modules_for_path(
    policy: dict[str, Any],
    path: str,
) -> set[str] | None:
    normalized = path.replace("\\", "/")
    if normalized.endswith(".currentness.json"):
        return set()
    from tools import material_form_authority as form_authority

    if form_authority.is_historical_receipt(normalized):
        return set()
    if normalized in {
        "tools/material_form_authority.json",
        "tools/material_form_authority.py",
    }:
        selected = set(form_authority.consumer_test_modules())
        selected.add("test_material_form_authority")
        selected.add("test_bulk_port_verification")
        return selected
    modules: set[str] = set()
    matched = False
    for rule in policy["affected_rules"]:
        if not any(
            fnmatch.fnmatchcase(path, pattern) for pattern in rule["paths"]
        ):
            continue
        matched = True
        modules.update(rule["test_modules"])
    return modules if matched else None


def affected_module_names(
    policy: dict[str, Any],
    paths: Iterable[str],
) -> tuple[tuple[str, ...], tuple[str, ...]]:
    selected: set[str] = set()
    unmatched: list[str] = []
    for path in normalize_paths(paths):
        modules = affected_modules_for_path(policy, path)
        if modules is not None:
            selected.update(modules)
        elif is_documentation_path(policy, path):
            continue
        elif not profile_owns_path(path):
            unmatched.append(path)
    ordered = tuple(
        name for name in active_module_names(policy) if name in selected
    )
    return ordered, tuple(sorted(unmatched))


def select_cases(
    suite: str,
    policy: dict[str, Any],
    cases: list[unittest.TestCase],
    *,
    changed_paths: Iterable[str] = (),
    modules: Iterable[str] = (),
) -> Selection:
    if suite == "active":
        return Selection(suite, tuple(cases))
    if suite == "modules":
        requested = normalize_module_names(modules)
        if not requested:
            raise PolicyError("modules suite requires at least one --module")
        inactive = sorted(set(requested) - set(active_module_names(policy)))
        if inactive:
            raise PolicyError(f"modules suite requested inactive modules: {inactive}")
        selected = tuple(
            case for case in cases if test_module(case) in set(requested)
        )
        missing = sorted(set(requested) - {test_module(case) for case in selected})
        if missing:
            raise PolicyError(f"modules suite selected missing modules: {missing}")
        return Selection(suite, selected)
    if suite != "affected":
        raise PolicyError(f"unknown suite {suite}")
    paths = normalize_paths(changed_paths)
    if not paths:
        return Selection(suite, tuple(cases))
    names, unmatched = affected_module_names(policy, paths)
    selected = tuple(case for case in cases if test_module(case) in set(names))
    return Selection(suite, selected, paths, unmatched)


def ordered_cases(selection: Selection) -> list[unittest.TestCase]:
    return sorted(selection.cases, key=normalized_test_id)


def write_result(
    path: Path,
    *,
    selection: Selection,
    result: unittest.TestResult,
    elapsed_seconds: float,
    success: bool,
) -> None:
    document = {
        "schema_version": 2,
        "suite": selection.suite,
        "selected_tests": len(selection.cases),
        "tests_run": result.testsRun,
        "failures": len(result.failures),
        "errors": len(result.errors),
        "skipped": len(getattr(result, "skipped", ())),
        "elapsed_seconds": round(elapsed_seconds, 3),
        "success": success,
        "unmatched_paths": list(selection.unmatched_paths),
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
        choices=("active", "affected", "modules"),
        default=None,
    )
    parser.add_argument("--path", action="append", default=[])
    parser.add_argument("--path-file", type=Path)
    parser.add_argument("--module", action="append", default=[])
    parser.add_argument("--durations", type=int, default=20)
    parser.add_argument("--failfast", action="store_true")
    parser.add_argument("--list", action="store_true")
    parser.add_argument("--result-json", type=Path)
    args = parser.parse_args(argv)

    try:
        policy = load_policy()
        suite = args.suite or policy["default_suite"]
        if suite == "modules":
            cases = discover_cases(args.module, policy=policy)
            selection = select_cases(
                suite,
                policy,
                cases,
                modules=args.module,
            )
        elif suite == "affected":
            paths = resolve_suite_paths(
                suite=suite,
                path_args=args.path,
                path_file=args.path_file,
            )
            names, unmatched = affected_module_names(policy, paths)
            if unmatched:
                print(
                    "Affected selection left unmatched paths: "
                    + ", ".join(unmatched),
                    file=sys.stderr,
                )
                return 2
            cases = discover_cases(
                names if paths else None,
                policy=policy,
            )
            selection = select_cases(
                suite,
                policy,
                cases,
                changed_paths=paths,
            )
        else:
            cases = discover_cases(policy=policy)
            selection = select_cases(suite, policy, cases)
        selected = ordered_cases(selection)
    except (OSError, json.JSONDecodeError, PolicyError) as error:
        print(f"Python test workflow policy failed: {error}", file=sys.stderr)
        return 2

    print(f"Python suite {suite}: selected {len(selected)} active tests")
    if args.list:
        print("\n".join(normalized_test_id(case) for case in selected))
        return 0
    if not selected:
        print("Python test workflow selected no tests")
        return 0

    started = time.perf_counter()
    from tools.tests.support import authority_sandbox

    authority_sandbox.install_write_guard()
    runner = unittest.TextTestRunner(
        verbosity=2,
        failfast=args.failfast,
        durations=max(0, args.durations),
    )
    result = runner.run(unittest.TestSuite(selected))
    elapsed = time.perf_counter() - started
    success = result.wasSuccessful()
    if args.result_json:
        write_result(
            args.result_json,
            selection=selection,
            result=result,
            elapsed_seconds=elapsed,
            success=success,
        )
    return 0 if success else 1


if __name__ == "__main__":
    raise SystemExit(main())
