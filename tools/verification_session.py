"""Snapshot-bound, resumable evidence for the full verification workflow."""
from __future__ import annotations

import hashlib
import json
import os
import sys
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Iterable, Mapping

ROOT = Path(__file__).resolve().parents[1]
SESSIONS = ROOT / "build" / "verification" / "sessions"
STEPS = (
    "builder",
    "datagen_1",
    "datagen_2",
    "java",
    "gametest",
    "python",
    "report",
)
TOOLCHAIN_PATHS = (
    "build.gradle",
    "settings.gradle",
    "gradle.properties",
    "gradlew",
    "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
)


def canonical_bytes(value: Any) -> bytes:
    return json.dumps(
        value,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    ).encode("utf-8")


def stable_hash(value: Any) -> str:
    return hashlib.sha256(canonical_bytes(value)).hexdigest()


def file_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def atomic_write_json(path: Path, document: Mapping[str, Any]) -> None:
    from tools import atomic_io

    payload = json.dumps(
        document,
        indent=2,
        ensure_ascii=False,
        sort_keys=True,
    ) + "\n"
    atomic_io.write_text(path, payload)


def relative_path(path: Path) -> str:
    resolved = path.resolve()
    try:
        return resolved.relative_to(ROOT).as_posix()
    except ValueError:
        return f"external:{resolved.as_posix()}"


def _tree_rows(
    root: Path,
    *,
    pattern: str,
    ignored_parts: Iterable[str],
) -> list[tuple[str, str]]:
    ignored = set(ignored_parts)
    return [
        (path.relative_to(root).as_posix(), file_sha256(path))
        for path in sorted(root.glob(pattern))
        if path.is_file() and not ignored.intersection(path.relative_to(root).parts)
    ]


def capture_file(path: Path) -> dict[str, Any]:
    if not path.is_file():
        raise ValueError(f"evidence file is missing: {path}")
    return {
        "kind": "file",
        "path": relative_path(path),
        "sha256": file_sha256(path),
    }


def capture_tree(
    root: Path,
    *,
    pattern: str = "**/*",
    ignored_parts: Iterable[str] = (),
) -> dict[str, Any]:
    rows = _tree_rows(
        root,
        pattern=pattern,
        ignored_parts=ignored_parts,
    )
    if not rows:
        raise ValueError(f"evidence tree is empty: {root} ({pattern})")
    return {
        "kind": "tree",
        "path": relative_path(root),
        "pattern": pattern,
        "ignored_parts": sorted(set(ignored_parts)),
        "files": len(rows),
        "sha256": stable_hash(rows),
    }


def resolve_evidence_path(value: str) -> Path:
    if value.startswith("external:"):
        return Path(value.removeprefix("external:"))
    return ROOT / value


def validate_output(output: Mapping[str, Any]) -> str | None:
    kind = output.get("kind")
    path = resolve_evidence_path(str(output.get("path") or ""))
    try:
        if kind == "file":
            current = capture_file(path)
        elif kind == "tree":
            current = capture_tree(
                path,
                pattern=str(output.get("pattern") or "**/*"),
                ignored_parts=output.get("ignored_parts") or (),
            )
        else:
            return f"unknown output evidence kind {kind!r}"
    except (OSError, ValueError) as exc:
        return str(exc)
    if current != dict(output):
        return f"output evidence drifted: {output.get('path')}"
    return None


def toolchain_document(root: Path = ROOT) -> dict[str, Any]:
    files = {
        name: file_sha256(root / name)
        for name in TOOLCHAIN_PATHS
        if (root / name).is_file()
    }
    return {
        "python": {
            "implementation": sys.implementation.name,
            "version": list(sys.version_info[:3]),
            "platform": sys.platform,
        },
        "files_sha256": dict(sorted(files.items())),
    }


def build_identity(
    tooling_snapshot: Mapping[str, Any],
    *,
    policy_path: Path,
    builder_policy_path: Path,
    source_replay: bool,
) -> dict[str, Any]:
    toolchain = toolchain_document()
    return {
        "tooling_snapshot_sha256": tooling_snapshot["snapshot_sha256"],
        "python_test_policy_sha256": file_sha256(policy_path),
        "builder_policy_sha256": file_sha256(builder_policy_path),
        "toolchain_sha256": stable_hash(toolchain),
        "source_replay": source_replay,
    }


class VerificationSession:
    """A single workflow run whose passed steps are immutable evidence."""

    def __init__(self, directory: Path, manifest: dict[str, Any]) -> None:
        self.directory = directory
        self.manifest = manifest

    @property
    def manifest_path(self) -> Path:
        return self.directory / "manifest.json"

    @property
    def context_path(self) -> Path:
        return self.directory / "validation-context.json"

    @classmethod
    def create(
        cls,
        identity: Mapping[str, Any],
        *,
        sessions_root: Path = SESSIONS,
    ) -> "VerificationSession":
        timestamp = datetime.now(timezone.utc)
        session_id = (
            timestamp.strftime("%Y%m%dT%H%M%S.%fZ")
            + "-"
            + str(identity["tooling_snapshot_sha256"])[:12]
            + "-"
            + uuid.uuid4().hex[:8]
        )
        directory = sessions_root / session_id
        directory.mkdir(parents=True, exist_ok=False)
        manifest = {
            "schema_version": 1,
            "session_id": session_id,
            "created_at": timestamp.isoformat(),
            "updated_at": timestamp.isoformat(),
            "identity": dict(identity),
            "context": None,
            "steps": {
                name: {
                    "status": "PENDING",
                    "attempts": 0,
                    "evidence": None,
                    "evidence_sha256": None,
                    "error": None,
                }
                for name in STEPS
            },
        }
        session = cls(directory, manifest)
        session._save()
        return session

    @classmethod
    def load(cls, directory: Path) -> "VerificationSession":
        manifest = json.loads(
            (directory / "manifest.json").read_text(encoding="utf-8")
        )
        if manifest.get("schema_version") != 1:
            raise ValueError("unsupported verification session schema")
        if set(manifest.get("steps") or {}) != set(STEPS):
            raise ValueError("verification session step order is invalid")
        if manifest.get("session_id") != directory.name:
            raise ValueError("verification session id does not match its directory")
        return cls(directory, manifest)

    @classmethod
    def latest(
        cls,
        *,
        sessions_root: Path = SESSIONS,
    ) -> "VerificationSession | None":
        if not sessions_root.is_dir():
            return None
        manifests = sorted(sessions_root.glob("*/manifest.json"), reverse=True)
        return cls.load(manifests[0].parent) if manifests else None

    def _save(self) -> None:
        self.manifest["updated_at"] = datetime.now(timezone.utc).isoformat()
        atomic_write_json(self.manifest_path, self.manifest)

    def matches_identity(self, identity: Mapping[str, Any]) -> bool:
        return self.manifest.get("identity") == dict(identity)

    def attach_context(self, *, context_digest: str) -> None:
        if not self.context_path.is_file():
            raise ValueError("validation context was not serialized")
        self.manifest["context"] = {
            "path": self.context_path.name,
            "sha256": file_sha256(self.context_path),
            "context_sha256": context_digest,
        }
        self._save()

    def validate_context_file(self) -> dict[str, Any]:
        record = self.manifest.get("context")
        if not isinstance(record, dict):
            raise ValueError("verification session has no frozen context")
        path = self.directory / str(record.get("path") or "")
        if not path.is_file() or file_sha256(path) != record.get("sha256"):
            raise ValueError("frozen validation context evidence is invalid")
        return record

    def mark_running(self, step: str) -> None:
        record = self.manifest["steps"][step]
        record.update({
            "status": "RUNNING",
            "attempts": int(record.get("attempts") or 0) + 1,
            "error": None,
        })
        self._save()

    def mark_failed(self, step: str, error: str) -> None:
        record = self.manifest["steps"][step]
        record.update({
            "status": "FAILED",
            "evidence": None,
            "evidence_sha256": None,
            "error": error,
        })
        self._save()

    def record_passed(
        self,
        step: str,
        *,
        elapsed_seconds: float,
        details: Mapping[str, Any] | None = None,
        outputs: Iterable[Mapping[str, Any]] = (),
    ) -> None:
        if elapsed_seconds < 0:
            raise ValueError("step elapsed time cannot be negative")
        evidence = {
            "schema_version": 1,
            "session_id": self.manifest["session_id"],
            "identity": self.manifest["identity"],
            "step": step,
            "result": "PASS",
            "elapsed_seconds": elapsed_seconds,
            "details": dict(details or {}),
            "outputs": [dict(output) for output in outputs],
        }
        relative = Path("evidence") / f"{step}.json"
        path = self.directory / relative
        atomic_write_json(path, evidence)
        self.manifest["steps"][step].update({
            "status": "PASSED",
            "evidence": relative.as_posix(),
            "evidence_sha256": file_sha256(path),
            "error": None,
        })
        self._save()

    def evidence(self, step: str) -> dict[str, Any]:
        record = self.manifest["steps"][step]
        relative = record.get("evidence")
        if not relative:
            raise ValueError(f"{step} has no evidence")
        path = self.directory / relative
        if not path.is_file() or file_sha256(path) != record.get(
            "evidence_sha256"
        ):
            raise ValueError(f"{step} evidence digest is invalid")
        document = json.loads(path.read_text(encoding="utf-8"))
        if (
            document.get("schema_version") != 1
            or document.get("session_id") != self.manifest["session_id"]
            or document.get("identity") != self.manifest["identity"]
            or document.get("step") != step
            or document.get("result") != "PASS"
        ):
            raise ValueError(f"{step} evidence binding is invalid")
        errors = [
            error
            for output in document.get("outputs") or ()
            if (error := validate_output(output)) is not None
        ]
        if errors:
            raise ValueError("; ".join(errors))
        return document

    def invalidate_from(self, step: str, reason: str) -> None:
        start = STEPS.index(step)
        for name in STEPS[start:]:
            record = self.manifest["steps"][name]
            record.update({
                "status": "PENDING",
                "evidence": None,
                "evidence_sha256": None,
                "error": reason if name == step else None,
            })
        self._save()

    def first_incomplete(
        self,
        *,
        report_mode: str,
    ) -> tuple[str | None, list[str]]:
        messages: list[str] = []
        for step in STEPS:
            record = self.manifest["steps"][step]
            if record.get("status") != "PASSED":
                return step, messages
            try:
                evidence = self.evidence(step)
                if (
                    step == "report"
                    and evidence.get("details", {}).get("mode") != report_mode
                ):
                    raise ValueError("report mode changed")
            except (OSError, ValueError, json.JSONDecodeError) as exc:
                self.invalidate_from(step, str(exc))
                messages.append(f"{step} cannot be reused: {exc}")
                return step, messages
            messages.append(f"{step} evidence is valid; skipping")
        return None, messages


def acquire_session(
    identity: Mapping[str, Any],
    *,
    resume: bool,
    sessions_root: Path = SESSIONS,
) -> tuple[VerificationSession, str | None]:
    if resume:
        latest = VerificationSession.latest(sessions_root=sessions_root)
        if latest is not None and latest.matches_identity(identity):
            return latest, None
        if latest is not None:
            return (
                VerificationSession.create(identity, sessions_root=sessions_root),
                "latest session belongs to a different snapshot/policy/toolchain",
            )
    return VerificationSession.create(identity, sessions_root=sessions_root), None
