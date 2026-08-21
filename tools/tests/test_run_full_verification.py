from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tools import run_full_verification as runner
from tools import verify_full_verification_report as report
from tools import verification_session as sessions
from tools.verification_context import ValidationContext


class FullVerificationRunnerTest(unittest.TestCase):
    def test_builder_policy_is_complete_and_cache_independent(self) -> None:
        policy = runner.load_builder_policy()
        self.assertFalse(policy["ordinary_ci_requires_local_artifacts"])
        self.assertEqual(110, len(policy["builders"]))
        self.assertEqual(
            len(policy["builders"]),
            len({row["name"] for row in policy["builders"]}),
        )

    def test_builder_stage_records_ordered_per_script_timings(self) -> None:
        with mock.patch.object(
            runner,
            "run_command",
            side_effect=lambda name, command: runner.StageResult(name, 0.25),
        ):
            result = runner.run_builder_checks()
        checks = (result.details or {})["checks"]
        self.assertEqual(110, len(checks))
        self.assertEqual(
            [row["script"] for row in runner.BUILDER_POLICY_DOCUMENT["builders"]],
            [row["script"] for row in checks],
        )
        self.assertTrue(all(row["elapsed_ms"] == 250 for row in checks))

    def test_check_ready_never_invokes_external_stage_commands(self) -> None:
        with (
            mock.patch.object(
                runner.report,
                "current_snapshot",
                return_value={"snapshot_sha256": "a" * 64},
            ),
            mock.patch.object(
                runner,
                "load_report",
                return_value={"status": "READY"},
            ),
            mock.patch.object(
                runner.report,
                "validate_report_document",
                return_value=[],
            ) as validate,
            mock.patch.object(runner, "run_command") as run_command,
            mock.patch.object(runner.sessions, "acquire_session") as acquire,
        ):
            self.assertEqual(0, runner.main(["--check-ready"]))
        run_command.assert_not_called()
        acquire.assert_not_called()
        self.assertFalse(
            validate.call_args.kwargs["verify_live_test_evidence"]
        )

    def test_datagen_requires_both_tree_hashes_to_match(self) -> None:
        runner.require_matching_datagen_hashes("same", "same")
        with self.assertRaisesRegex(RuntimeError, "different hashes"):
            runner.require_matching_datagen_hashes("first", "second")

    def test_late_record_failure_leaves_ready_report_untouched(self) -> None:
        stage = runner.StageResult("stage", 1.0)
        context = ValidationContext.create({
            "tooling_snapshot": lambda: {
                "snapshot_sha256": "a" * 64,
            }
        })
        original = {"status": "READY", "sentinel": "old"}
        with (
            mock.patch.object(
                runner,
                "load_report",
                return_value=json.loads(json.dumps(original)),
            ),
            mock.patch.object(runner.report, "write_snapshot"),
            mock.patch.object(runner.report, "record_builder", return_value=[]),
            mock.patch.object(runner.report, "record_datagen", return_value=[]),
            mock.patch.object(runner.report, "record_java", return_value=[]),
            mock.patch.object(runner.report, "record_gametest", return_value=[]),
            mock.patch.object(runner.report, "record_python", return_value=[]),
            mock.patch.object(
                runner.report,
                "mark_ready",
                return_value=["late validation failed"],
            ),
            mock.patch.object(runner.report, "write_report_atomic") as publish,
        ):
            with self.assertRaisesRegex(RuntimeError, "late validation failed"):
                runner.record_ready(
                    context,
                    builder=stage,
                    datagen_1=stage,
                    datagen_2=stage,
                    datagen_hash_1="a",
                    datagen_hash_2="a",
                    java=stage,
                    gametest=stage,
                    python=stage,
                    extruder_replay="SKIP",
                    gametest_log=Path("unused.log"),
                )
        publish.assert_not_called()

    def test_atomic_report_replace_preserves_old_bytes_on_failure(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "report.json"
            old_bytes = b'{"status":"READY","sentinel":"old"}\n'
            path.write_bytes(old_bytes)
            with (
                mock.patch.object(report, "REPORT", path),
                mock.patch.object(
                    report.os,
                    "replace",
                    side_effect=OSError("simulated replace failure"),
                ),
            ):
                with self.assertRaisesRegex(OSError, "simulated"):
                    report.write_report_atomic({
                        "status": "READY",
                        "sentinel": "new",
                    })
            self.assertEqual(old_bytes, path.read_bytes())

    def test_report_tests_can_load_frozen_context(self) -> None:
        snapshot = {"files_sha256": {}, "trees": {}, "python_test_count": 1}
        snapshot["snapshot_sha256"] = report.stable_hash(snapshot)
        context = ValidationContext.create({
            "tooling_snapshot": lambda: snapshot,
        })
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "context.json"
            context.write(path)
            with (
                mock.patch.dict(
                    report.os.environ,
                    {
                        "CRUCIBLECRAFT_VALIDATION_CONTEXT": str(path),
                        "CRUCIBLECRAFT_VALIDATION_CONTEXT_SHA256": context.digest,
                    },
                    clear=False,
                ),
                mock.patch.object(
                    report,
                    "build_validation_context",
                    side_effect=AssertionError("must load frozen context"),
                ),
            ):
                loaded = report.load_or_build_validation_context()
            self.assertEqual(snapshot, loaded.value("tooling_snapshot"))

    def test_orchestrator_constructs_context_once_per_session(self) -> None:
        snapshot = {"snapshot_sha256": "a" * 64}
        context = ValidationContext.create({
            "tooling_snapshot": lambda: snapshot,
        })
        identity = {
            "tooling_snapshot_sha256": "a" * 64,
            "python_test_policy_sha256": "b" * 64,
            "builder_policy_sha256": "d" * 64,
            "toolchain_sha256": "c" * 64,
            "source_replay": False,
        }
        with tempfile.TemporaryDirectory() as temporary:
            session = sessions.VerificationSession.create(
                identity,
                sessions_root=Path(temporary) / "sessions",
            )
            with mock.patch.object(
                runner.report,
                "build_validation_context",
                return_value=context,
            ) as build:
                first = runner.frozen_context(session, snapshot)
                second = runner.frozen_context(session, snapshot)
            self.assertEqual(1, build.call_count)
            self.assertEqual(first.digest, second.digest)


if __name__ == "__main__":
    unittest.main()
