#!/usr/bin/env python3
"""Run the fail-fast T11a+ closure workflow and validate or record READY."""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable, Sequence

try:
    from tools import verify_full_verification_report as report
    from tools.verification_context import ValidationContext
    from tools import verification_session as sessions
except ModuleNotFoundError:
    import verify_full_verification_report as report
    from verification_context import ValidationContext
    import verification_session as sessions

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "build" / "verification"
BUILDER_POLICY = ROOT / "tools" / "verification_builder_policy.json"


def load_builder_policy(path: Path = BUILDER_POLICY) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    if (
        document.get("schema_version") != 1
        or document.get("ordinary_ci_requires_local_artifacts") is not False
    ):
        raise ValueError("unsupported verification builder policy")
    builders = document.get("builders") or []
    names = [row.get("name") for row in builders]
    scripts = [row.get("script") for row in builders]
    if (
        len(builders) != 110
        or len(names) != len(set(names))
        or len(scripts) != len(set(scripts))
    ):
        raise ValueError("verification builder policy must contain 110 unique builders")
    for row in builders:
        if (
            not isinstance(row.get("name"), str)
            or not isinstance(row.get("script"), str)
            or not isinstance(row.get("ordinary_args"), list)
            or row.get("proof_tier") not in {"compact", "rederived"}
        ):
            raise ValueError(f"invalid verification builder policy row: {row!r}")
    return document


BUILDER_POLICY_DOCUMENT = load_builder_policy()
BUILDER_CHECKS: tuple[tuple[str, ...], ...] = tuple(
    (row["script"], *row["ordinary_args"])
    for row in BUILDER_POLICY_DOCUMENT["builders"]
)
BUILDER_PROOF_TIERS = {
    row["script"]: row["proof_tier"]
    for row in BUILDER_POLICY_DOCUMENT["builders"]
}


@dataclass(frozen=True)
class StageResult:
    name: str
    elapsed_seconds: float
    details: dict[str, Any] | None = None


def display_command(command: Sequence[str]) -> str:
    return subprocess.list2cmdline(list(command))


def run_command(
    name: str,
    command: Sequence[str],
    *,
    env: dict[str, str] | None = None,
    stdout: object | None = None,
) -> StageResult:
    print(f"[{name}] {display_command(command)}", flush=True)
    started = time.perf_counter()
    completed = subprocess.run(
        list(command),
        cwd=ROOT,
        env=env,
        stdout=stdout,
        stderr=subprocess.STDOUT if stdout is not None else None,
        check=False,
    )
    elapsed = time.perf_counter() - started
    if completed.returncode != 0:
        raise RuntimeError(
            f"{name} failed with exit code {completed.returncode}"
        )
    print(f"[{name}] PASS in {elapsed:.3f}s", flush=True)
    return StageResult(name, elapsed)


def run_builder_checks() -> StageResult:
    started = time.perf_counter()
    checks: list[dict[str, Any]] = []
    for script_and_args in BUILDER_CHECKS:
        result = run_command(
            f"builder:{Path(script_and_args[0]).stem}",
            (sys.executable, *script_and_args),
        )
        checks.append({
            "script": script_and_args[0],
            "argv": list(script_and_args[1:]),
            "proof_tier": BUILDER_PROOF_TIERS[script_and_args[0]],
            "elapsed_ms": round(result.elapsed_seconds * 1000, 3),
            "result": "PASS",
        })
    return StageResult(
        "builder",
        time.perf_counter() - started,
        {"checks": checks},
    )


def gradle_command(task: str) -> tuple[str, ...]:
    wrapper = "gradlew.bat" if os.name == "nt" else "./gradlew"
    return (str(ROOT / wrapper), task, "--no-daemon")


def datagen_digest() -> str:
    return report.current_tree_digests()["datagen_generated"]["sha256"]


def load_report() -> dict:
    return json.loads(report.REPORT.read_text(encoding="utf-8"))


def require_no_errors(stage: str, errors: list[str]) -> None:
    if errors:
        raise RuntimeError(
            f"{stage} report recording failed:\n"
            + "\n".join(f"- {error}" for error in errors)
        )


def record_ready(
    context: report.ValidationContext,
    *,
    builder: StageResult,
    datagen_1: StageResult,
    datagen_2: StageResult,
    datagen_hash_1: str,
    datagen_hash_2: str,
    java: StageResult,
    gametest: StageResult,
    python: StageResult,
    extruder_replay: str,
    gametest_log: Path,
) -> None:
    document = load_report()
    report.write_snapshot(document, False, context, persist=False)
    require_no_errors(
        "builder",
        report.record_builder(
            document,
            builder.elapsed_seconds * 1000,
            extruder_replay,
            context,
            per_builder=(builder.details or {}).get("checks", []),
            persist=False,
        ),
    )
    require_no_errors(
        "datagen",
        report.record_datagen(
            document,
            datagen_hash_1,
            datagen_hash_2,
            datagen_1.elapsed_seconds,
            datagen_2.elapsed_seconds,
            context,
            persist=False,
        ),
    )
    require_no_errors(
        "java",
        report.record_java(
            document,
            java.elapsed_seconds,
            context,
            persist=False,
        ),
    )
    require_no_errors(
        "gametest",
        report.record_gametest(
            document,
            gametest_log,
            gametest.elapsed_seconds,
            context,
            persist=False,
        ),
    )
    require_no_errors(
        "python",
        report.record_python(
            document,
            python.elapsed_seconds,
            context,
            persist=False,
        ),
    )
    require_no_errors(
        "READY",
        report.mark_ready(document, context, persist=False),
    )
    require_no_errors(
        "READY candidate",
        report.validate_report_document(document, context),
    )
    report.write_report_atomic(document)


def require_matching_datagen_hashes(first: str, second: str) -> None:
    if first != second:
        raise RuntimeError("the two runData trees have different hashes")


def frozen_context(
    session: sessions.VerificationSession,
    snapshot: dict[str, Any],
) -> ValidationContext:
    if session.manifest.get("context") is None:
        context = report.build_validation_context(snapshot)
        context.write(session.context_path)
        session.attach_context(context_digest=context.digest)
        return context
    record = session.validate_context_file()
    context = ValidationContext.read(
        session.context_path,
        expected_digest=record["context_sha256"],
    )
    if context.value("tooling_snapshot") != snapshot:
        raise RuntimeError("frozen validation context belongs to another snapshot")
    if (
        context.has("java_test_metrics")
        and context.value("java_test_metrics")
        != report.current_java_test_metrics()
    ):
        raise RuntimeError(
            "Java XML no longer matches the frozen context; start a new session"
        )
    return context


def evidence_result(
    session: sessions.VerificationSession,
    step: str,
) -> StageResult:
    evidence = session.evidence(step)
    return StageResult(
        step,
        float(evidence["elapsed_seconds"]),
        dict(evidence.get("details") or {}),
    )


def assert_session_identity(
    identity: dict[str, Any],
    *,
    source_replay: bool,
) -> dict[str, Any]:
    snapshot = report.current_snapshot()
    current = sessions.build_identity(
        snapshot,
        policy_path=ROOT / "tools" / "python_test_policy.json",
        builder_policy_path=BUILDER_POLICY,
        source_replay=source_replay,
    )
    if current != identity:
        raise RuntimeError(
            "tooling snapshot/policy/toolchain changed during verification; "
            "start a new session"
        )
    return snapshot


def run_session_step(
    session: sessions.VerificationSession,
    identity: dict[str, Any],
    step: str,
    action: Callable[
        [],
        tuple[StageResult, dict[str, Any], list[dict[str, Any]]],
    ],
    *,
    source_replay: bool,
) -> StageResult:
    assert_session_identity(identity, source_replay=source_replay)
    session.mark_running(step)
    try:
        result, details, outputs = action()
        assert_session_identity(identity, source_replay=source_replay)
        session.record_passed(
            step,
            elapsed_seconds=result.elapsed_seconds,
            details=details,
            outputs=outputs,
        )
        return result
    except (
        OSError,
        RuntimeError,
        ValueError,
        json.JSONDecodeError,
    ) as exc:
        session.mark_failed(step, str(exc))
        raise


def check_ready_only() -> None:
    snapshot = report.current_snapshot()
    document = load_report()
    if document.get("status") != "READY":
        raise RuntimeError("committed full verification report is not READY")
    require_no_errors(
        "committed READY",
        report.validate_report_document(
            document,
            snapshot,
            verify_live_test_evidence=False,
        ),
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument(
        "--record",
        action="store_true",
        help="atomically record a successful full session as READY",
    )
    mode.add_argument(
        "--check",
        action="store_true",
        help="run the full workflow and validate committed READY",
    )
    mode.add_argument(
        "--check-ready",
        action="store_true",
        help="validate the committed verification report without running tests",
    )
    session_mode = parser.add_mutually_exclusive_group()
    session_mode.add_argument(
        "--resume",
        action="store_true",
        help="resume the latest session when all snapshot bindings still match",
    )
    session_mode.add_argument(
        "--new-session",
        action="store_true",
        help="force a fresh session instead of reusing evidence",
    )
    parser.add_argument(
        "--source-replay",
        action="store_true",
        help="also execute optional raw/cache source replay",
    )
    args = parser.parse_args(argv)
    if args.check_ready and (
        args.resume or args.new_session or args.source_replay
    ):
        parser.error(
            "--check-ready cannot be combined with session or source replay options"
        )

    if args.check_ready:
        try:
            check_ready_only()
        except (OSError, RuntimeError, ValueError, json.JSONDecodeError) as exc:
            print(f"Verification report check failed: {exc}", file=sys.stderr)
            return 1
        print("Committed verification report matches the current snapshot")
        return 0

    BUILD.mkdir(parents=True, exist_ok=True)
    record = args.record
    report_mode = "record" if record else "check"
    session: sessions.VerificationSession | None = None
    try:
        snapshot = report.current_snapshot()
        identity = sessions.build_identity(
            snapshot,
            policy_path=ROOT / "tools" / "python_test_policy.json",
            builder_policy_path=BUILDER_POLICY,
            source_replay=args.source_replay,
        )
        session, replacement_reason = sessions.acquire_session(
            identity,
            resume=args.resume and not args.new_session,
        )
        if replacement_reason:
            print(
                f"Resume refused; opened {session.manifest['session_id']}: "
                f"{replacement_reason}"
            )
        else:
            print(f"Verification session: {session.manifest['session_id']}")

        first_step, resume_messages = session.first_incomplete(
            report_mode=report_mode
        )
        for message in resume_messages:
            print(f"[resume] {message}")

        results: dict[str, StageResult] = {}
        for step in sessions.STEPS:
            if (
                first_step is None
                or sessions.STEPS.index(step) < sessions.STEPS.index(first_step)
            ):
                results[step] = evidence_result(session, step)

        if first_step is not None and sessions.STEPS.index(
            first_step
        ) <= sessions.STEPS.index("builder"):
            results["builder"] = run_session_step(
                session,
                identity,
                "builder",
                lambda: (
                    (result := run_builder_checks()),
                    result.details or {"checks": []},
                    [],
                ),
                source_replay=args.source_replay,
            )

        if first_step is not None and sessions.STEPS.index(
            first_step
        ) <= sessions.STEPS.index("datagen_1"):
            def run_datagen_1() -> tuple[
                StageResult, dict[str, Any], list[dict[str, Any]]
            ]:
                result = run_command("datagen:1", gradle_command("runData"))
                digest = datagen_digest()
                output = sessions.capture_tree(
                    report.SHAPE_RESOURCE_ROOT,
                    ignored_parts=(".cache",),
                )
                if output["sha256"] != digest:
                    raise RuntimeError("datagen:1 evidence digest disagrees with snapshot")
                return result, {"tree_sha256": digest}, [output]

            results["datagen_1"] = run_session_step(
                session,
                identity,
                "datagen_1",
                run_datagen_1,
                source_replay=args.source_replay,
            )

        if first_step is not None and sessions.STEPS.index(
            first_step
        ) <= sessions.STEPS.index("datagen_2"):
            def run_datagen_2() -> tuple[
                StageResult, dict[str, Any], list[dict[str, Any]]
            ]:
                result = run_command("datagen:2", gradle_command("runData"))
                first_hash = session.evidence("datagen_1")["details"][
                    "tree_sha256"
                ]
                second_hash = datagen_digest()
                require_matching_datagen_hashes(first_hash, second_hash)
                output = sessions.capture_tree(
                    report.SHAPE_RESOURCE_ROOT,
                    ignored_parts=(".cache",),
                )
                if output["sha256"] != second_hash:
                    raise RuntimeError("datagen:2 evidence digest disagrees with snapshot")
                return (
                    result,
                    {
                        "run_1_tree_sha256": first_hash,
                        "run_2_tree_sha256": second_hash,
                    },
                    [output],
                )

            results["datagen_2"] = run_session_step(
                session,
                identity,
                "datagen_2",
                run_datagen_2,
                source_replay=args.source_replay,
            )

        if first_step is not None and sessions.STEPS.index(
            first_step
        ) <= sessions.STEPS.index("java"):
            results["java"] = run_session_step(
                session,
                identity,
                "java",
                lambda: (
                    (result := run_command("java", gradle_command("test"))),
                    {},
                    [
                        sessions.capture_tree(
                            ROOT / "build" / "test-results" / "test",
                            pattern="TEST-*.xml",
                        )
                    ],
                ),
                source_replay=args.source_replay,
            )

        gametest_log = session.directory / "gametest.log"
        if first_step is not None and sessions.STEPS.index(
            first_step
        ) <= sessions.STEPS.index("gametest"):
            def run_gametest() -> tuple[
                StageResult, dict[str, Any], list[dict[str, Any]]
            ]:
                with gametest_log.open(
                    "w", encoding="utf-8", newline="\n"
                ) as log:
                    result = run_command(
                        "gametest",
                        gradle_command("runGameTestServer"),
                        stdout=log,
                    )
                return (
                    result,
                    {"log": gametest_log.name},
                    [sessions.capture_file(gametest_log)],
                )

            results["gametest"] = run_session_step(
                session,
                identity,
                "gametest",
                run_gametest,
                source_replay=args.source_replay,
            )
        else:
            gametest_log = session.directory / session.evidence(
                "gametest"
            )["details"]["log"]

        if first_step is not None and sessions.STEPS.index(
            first_step
        ) <= sessions.STEPS.index("python"):
            def run_python() -> tuple[
                StageResult, dict[str, Any], list[dict[str, Any]]
            ]:
                current_snapshot = assert_session_identity(
                    identity,
                    source_replay=args.source_replay,
                )
                context = frozen_context(session, current_snapshot)
                candidate_report_path = (
                    session.directory / "candidate-report.json"
                )
                candidate_report = load_report()
                report.write_snapshot(
                    candidate_report,
                    False,
                    context,
                    persist=False,
                )
                datagen_2_details = session.evidence("datagen_2")[
                    "details"
                ]
                candidate_report.setdefault(
                    "data_generation_determinism", {}
                )
                candidate_report["data_generation_determinism"][
                    "run_1_tree_sha256"
                ] = datagen_2_details["run_1_tree_sha256"]
                candidate_report["data_generation_determinism"][
                    "run_2_tree_sha256"
                ] = datagen_2_details["run_2_tree_sha256"]
                gametest_evidence = session.evidence("gametest")
                report.record_gametest(
                    candidate_report,
                    session.directory
                    / gametest_evidence["details"]["log"],
                    gametest_evidence["elapsed_seconds"],
                    context,
                    persist=False,
                )
                sessions.atomic_write_json(
                    candidate_report_path,
                    candidate_report,
                )
                python_result_path = session.directory / "python-closure.json"
                python_log = session.directory / "python-closure.log"
                closure_env = dict(os.environ)
                closure_env.update({
                    "CRUCIBLECRAFT_CURRENTNESS_PRECHECKED": "1",
                    "CRUCIBLECRAFT_BUILDER_STAGE_PASSED": "1",
                    "CRUCIBLECRAFT_DATAGEN_STAGE_PASSED": "1",
                    "CRUCIBLECRAFT_JAVA_STAGE_PASSED": "1",
                    "CRUCIBLECRAFT_GAMETEST_STAGE_PASSED": "1",
                    "CRUCIBLECRAFT_VALIDATION_CONTEXT": str(
                        session.context_path
                    ),
                    "CRUCIBLECRAFT_VALIDATION_CONTEXT_SHA256": context.digest,
                    "CRUCIBLECRAFT_FULL_VERIFICATION_REPORT": str(
                        candidate_report_path
                    ),
                })
                with python_log.open("w", encoding="utf-8") as log_fh:
                    result = run_command(
                        "python:closure",
                        (
                            sys.executable,
                            "tools/run_python_tests.py",
                            "--suite",
                            "closure",
                            "--result-json",
                            str(python_result_path),
                        ),
                        env=closure_env,
                        stdout=log_fh,
                    )
                python_result = json.loads(
                    python_result_path.read_text(encoding="utf-8")
                )
                expected_tests = context.value("tooling_snapshot")[
                    "python_test_count"
                ]
                if (
                    not python_result["success"]
                    or python_result["selected_tests"] != expected_tests
                ):
                    raise RuntimeError(
                        "Python closure did not execute discovery exactly once: "
                        + json.dumps(python_result, sort_keys=True)
                    )
                extruder_replay = "SKIP"
                outputs = [
                    sessions.capture_file(python_result_path),
                    sessions.capture_file(python_log),
                    sessions.capture_file(candidate_report_path),
                ]
                if args.source_replay:
                    replay_result_path = (
                        session.directory / "python-source-replay.json"
                    )
                    run_command(
                        "python:source-replay",
                        (
                            sys.executable,
                            "tools/run_python_tests.py",
                            "--suite",
                            "source-replay",
                            "--result-json",
                            str(replay_result_path),
                        ),
                    )
                    replay_result = json.loads(
                        replay_result_path.read_text(encoding="utf-8")
                    )
                    extruder_replay = (
                        "PASS"
                        if replay_result["source_replay_commands_skipped"] == 0
                        else "SKIP"
                    )
                    outputs.append(sessions.capture_file(replay_result_path))
                return (
                    result,
                    {
                        "tests": expected_tests,
                        "extruder_replay": extruder_replay,
                    },
                    outputs,
                )

            results["python"] = run_session_step(
                session,
                identity,
                "python",
                run_python,
                source_replay=args.source_replay,
            )

        if first_step is not None and sessions.STEPS.index(
            first_step
        ) <= sessions.STEPS.index("report"):
            def run_report() -> tuple[
                StageResult, dict[str, Any], list[dict[str, Any]]
            ]:
                started = time.perf_counter()
                current_snapshot = assert_session_identity(
                    identity,
                    source_replay=args.source_replay,
                )
                context = frozen_context(session, current_snapshot)
                datagen_1_evidence = session.evidence("datagen_1")
                datagen_2_evidence = session.evidence("datagen_2")
                first_hash = datagen_1_evidence["details"]["tree_sha256"]
                second_hash = datagen_2_evidence["details"][
                    "run_2_tree_sha256"
                ]
                require_matching_datagen_hashes(first_hash, second_hash)
                if record:
                    record_ready(
                        context,
                        builder=results["builder"],
                        datagen_1=results["datagen_1"],
                        datagen_2=results["datagen_2"],
                        datagen_hash_1=first_hash,
                        datagen_hash_2=second_hash,
                        java=results["java"],
                        gametest=results["gametest"],
                        python=results["python"],
                        extruder_replay=session.evidence("python")[
                            "details"
                        ]["extruder_replay"],
                        gametest_log=gametest_log,
                    )
                else:
                    require_no_errors(
                        "final report",
                        report.validate_report_document(load_report(), context),
                    )
                result = StageResult(
                    "report",
                    time.perf_counter() - started,
                )
                return (
                    result,
                    {"mode": report_mode},
                    [sessions.capture_file(report.REPORT)],
                )

            results["report"] = run_session_step(
                session,
                identity,
                "report",
                run_report,
                source_replay=args.source_replay,
            )
    except (OSError, RuntimeError, ValueError, json.JSONDecodeError) as exc:
        suffix = (
            f" (session {session.manifest['session_id']})"
            if session is not None
            else ""
        )
        print(f"Full verification failed{suffix}: {exc}", file=sys.stderr)
        return 1

    print(
        "Full verification READY: "
        f"builder={results['builder'].elapsed_seconds:.3f}s, "
        f"datagen={results['datagen_1'].elapsed_seconds + results['datagen_2'].elapsed_seconds:.3f}s, "
        f"java={results['java'].elapsed_seconds:.3f}s, "
        f"gametest={results['gametest'].elapsed_seconds:.3f}s, "
        f"python={results['python'].elapsed_seconds:.3f}s"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
