#!/usr/bin/env python3
"""Run the fail-fast T10+ closure workflow and validate or record READY."""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Sequence

try:
    from tools import verify_full_verification_report as report
except ModuleNotFoundError:
    import verify_full_verification_report as report

ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "build" / "verification"
PYTHON_RESULT = BUILD / "python-closure.json"
GAMETEST_LOG = BUILD / "gametest.log"

BUILDER_CHECKS: tuple[tuple[str, ...], ...] = (
    ("tools/import_gt6_oredict.py", "--check", "--reference-only"),
    ("tools/compare_gt6_recipes.py", "--check", "--reference-only"),
    ("tools/build_component_rules.py", "--check"),
    ("tools/build_gt6_ore_chain.py", "--check"),
    ("tools/build_gt6_ore_chain_closure.py", "--check"),
    ("tools/build_gt6_material_form_gate.py", "--check"),
    ("tools/build_t4_tool_readiness.py", "--check"),
    ("tools/build_t5_chemical_readiness.py", "--check"),
    ("tools/build_t5_chemical_recipes.py", "--check"),
    ("tools/build_t5_distillery_projection.py", "--check"),
    ("tools/build_machine_crafting_readiness.py", "--check"),
    ("tools/build_t6_electrical_readiness.py", "--check"),
    ("tools/build_t7_material_tag_readiness.py", "--check"),
    ("tools/build_t8_pipe_readiness.py", "--check"),
    ("tools/apply_t8_pipe_metadata.py", "--check"),
    ("tools/build_worldgen_catalog.py", "--check"),
    ("tools/build_gt6_veins.py", "--check"),
    ("tools/build_t10_preflight_projection.py", "--check"),
    ("tools/build_t10_container_readiness.py", "--check"),
    ("tools/apply_t10_form_flags.py", "--check"),
)


@dataclass(frozen=True)
class StageResult:
    name: str
    elapsed_seconds: float


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
    for script_and_args in BUILDER_CHECKS:
        run_command(
            f"builder:{Path(script_and_args[0]).stem}",
            (sys.executable, *script_and_args),
        )
    return StageResult(
        "builder",
        time.perf_counter() - started,
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
) -> None:
    document = load_report()
    require_no_errors(
        "builder",
        report.record_builder(
            document,
            builder.elapsed_seconds * 1000,
            extruder_replay,
            context,
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
        ),
    )
    require_no_errors(
        "java",
        report.record_java(document, java.elapsed_seconds, context),
    )
    require_no_errors(
        "gametest",
        report.record_gametest(
            document,
            GAMETEST_LOG,
            gametest.elapsed_seconds,
            context,
        ),
    )
    require_no_errors(
        "python",
        report.record_python(document, python.elapsed_seconds, context),
    )
    require_no_errors("READY", report.mark_ready(document, context))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument(
        "--record",
        action="store_true",
        help="refresh the report and record this run as READY",
    )
    mode.add_argument(
        "--check",
        action="store_true",
        help="validate the committed READY report without rewriting it",
    )
    parser.add_argument(
        "--source-replay",
        action="store_true",
        help="also execute optional raw/cache source replay",
    )
    args = parser.parse_args(argv)
    record = args.record
    BUILD.mkdir(parents=True, exist_ok=True)

    try:
        builder = run_builder_checks()
        datagen_1 = run_command("datagen:1", gradle_command("runData"))
        datagen_hash_1 = datagen_digest()
        datagen_2 = run_command("datagen:2", gradle_command("runData"))
        datagen_hash_2 = datagen_digest()
        if datagen_hash_1 != datagen_hash_2:
            raise RuntimeError("the two runData trees have different hashes")
        java = run_command("java", gradle_command("test"))
        with GAMETEST_LOG.open("w", encoding="utf-8", newline="\n") as log:
            gametest = run_command(
                "gametest",
                gradle_command("runGameTestServer"),
                stdout=log,
            )

        context = report.build_validation_context()
        if record:
            document = load_report()
            report.write_snapshot(document, False, context)

        closure_env = dict(os.environ)
        closure_env["CRUCIBLECRAFT_CURRENTNESS_PRECHECKED"] = "1"
        python = run_command(
            "python:closure",
            (
                sys.executable,
                "tools/run_python_tests.py",
                "--suite",
                "closure",
                "--result-json",
                str(PYTHON_RESULT),
            ),
            env=closure_env,
        )
        python_result = json.loads(PYTHON_RESULT.read_text(encoding="utf-8"))
        expected_tests = context.value("tooling_snapshot")["python_test_count"]
        if (
            not python_result["success"]
            or python_result["selected_tests"] != expected_tests
            or python_result["tests_run"] != expected_tests
        ):
            raise RuntimeError(
                "Python closure did not execute discovery exactly once: "
                + json.dumps(python_result, sort_keys=True)
            )

        extruder_replay = "SKIP"
        if args.source_replay:
            replay_result_path = BUILD / "python-source-replay.json"
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

        if record:
            record_ready(
                context,
                builder=builder,
                datagen_1=datagen_1,
                datagen_2=datagen_2,
                datagen_hash_1=datagen_hash_1,
                datagen_hash_2=datagen_hash_2,
                java=java,
                gametest=gametest,
                python=python,
                extruder_replay=extruder_replay,
            )
        errors = report.validate_report_document(load_report(), context)
        require_no_errors("final report", errors)
    except (OSError, RuntimeError, ValueError, json.JSONDecodeError) as exc:
        print(f"Full verification failed: {exc}", file=sys.stderr)
        return 1

    print(
        "Full verification READY: "
        f"builder={builder.elapsed_seconds:.3f}s, "
        f"datagen={datagen_1.elapsed_seconds + datagen_2.elapsed_seconds:.3f}s, "
        f"java={java.elapsed_seconds:.3f}s, "
        f"gametest={gametest.elapsed_seconds:.3f}s, "
        f"python={python.elapsed_seconds:.3f}s"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
