from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from tools.verification_context import ValidationContext
from tools import verification_session as sessions


def identity(snapshot: str = "a" * 64) -> dict[str, object]:
    return {
        "tooling_snapshot_sha256": snapshot,
        "python_test_policy_sha256": "b" * 64,
        "builder_policy_sha256": "d" * 64,
        "toolchain_sha256": "c" * 64,
        "source_replay": False,
    }


class VerificationContextPersistenceTest(unittest.TestCase):
    def test_context_round_trip_is_digest_bound(self) -> None:
        context = ValidationContext.create({
            "tooling_snapshot": lambda: {"snapshot_sha256": "a" * 64},
            "metric": lambda: {"value": 7},
        })
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "context.json"
            context.write(path)
            loaded = ValidationContext.read(
                path,
                expected_digest=context.digest,
            )
            self.assertEqual({"value": 7}, loaded.value("metric"))

            document = json.loads(path.read_text(encoding="utf-8"))
            document["values"]["metric"]["value"] = 8
            path.write_text(json.dumps(document), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "digest"):
                ValidationContext.read(path)


class VerificationSessionTest(unittest.TestCase):
    def test_builder_policy_hash_is_part_of_session_identity(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            tests = root / "tests.json"
            builders = root / "builders.json"
            tests.write_text("{}\n", encoding="utf-8")
            builders.write_text('{"version":1}\n', encoding="utf-8")
            first = sessions.build_identity(
                {"snapshot_sha256": "a" * 64},
                policy_path=tests,
                builder_policy_path=builders,
                source_replay=False,
            )
            builders.write_text('{"version":2}\n', encoding="utf-8")
            second = sessions.build_identity(
                {"snapshot_sha256": "a" * 64},
                policy_path=tests,
                builder_policy_path=builders,
                source_replay=False,
            )
        self.assertNotEqual(
            first["builder_policy_sha256"],
            second["builder_policy_sha256"],
        )

    def test_resume_skips_only_valid_passed_evidence(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary) / "sessions"
            session = sessions.VerificationSession.create(
                identity(),
                sessions_root=root,
            )
            session.mark_running("builder")
            session.record_passed(
                "builder",
                elapsed_seconds=1.0,
                details={"checks": 3},
            )

            first, messages = session.first_incomplete(report_mode="check")

            self.assertEqual("datagen_1", first)
            self.assertEqual(1, len(messages))
            self.assertIn("skipping", messages[0])
            self.assertEqual(
                "PASSED",
                session.manifest["steps"]["builder"]["status"],
            )

    def test_tampered_evidence_is_not_reused(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary) / "sessions"
            session = sessions.VerificationSession.create(
                identity(),
                sessions_root=root,
            )
            session.mark_running("builder")
            session.record_passed("builder", elapsed_seconds=1.0)
            evidence_path = (
                session.directory
                / session.manifest["steps"]["builder"]["evidence"]
            )
            evidence_path.write_text("{}\n", encoding="utf-8")

            first, messages = session.first_incomplete(report_mode="check")

            self.assertEqual("builder", first)
            self.assertIn("cannot be reused", messages[0])
            self.assertEqual(
                "PENDING",
                session.manifest["steps"]["builder"]["status"],
            )

    def test_snapshot_drift_opens_a_new_session(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary) / "sessions"
            original = sessions.VerificationSession.create(
                identity(),
                sessions_root=root,
            )

            replacement, reason = sessions.acquire_session(
                identity("d" * 64),
                resume=True,
                sessions_root=root,
            )

            self.assertNotEqual(
                original.manifest["session_id"],
                replacement.manifest["session_id"],
            )
            self.assertIn("different snapshot", reason)

    def test_output_drift_invalidates_the_step_and_every_later_step(self) -> None:
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            output = directory / "result.json"
            output.write_text('{"result":"PASS"}\n', encoding="utf-8")
            session = sessions.VerificationSession.create(
                identity(),
                sessions_root=directory / "sessions",
            )
            session.mark_running("builder")
            session.record_passed(
                "builder",
                elapsed_seconds=1.0,
                outputs=[sessions.capture_file(output)],
            )
            output.write_text('{"result":"FAIL"}\n', encoding="utf-8")

            first, _ = session.first_incomplete(report_mode="check")

            self.assertEqual("builder", first)
            self.assertEqual(
                "PENDING",
                session.manifest["steps"]["datagen_1"]["status"],
            )


if __name__ == "__main__":
    unittest.main()
