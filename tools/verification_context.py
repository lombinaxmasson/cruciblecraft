"""Process-local immutable values shared by full verification checks."""
from __future__ import annotations

import copy
import hashlib
import json
import os
import time
from dataclasses import dataclass
from pathlib import Path
from threading import RLock
from types import MappingProxyType
from typing import Any, Callable, Mapping


def _encode(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    )


def _digest(value: Any) -> str:
    return hashlib.sha256(_encode(value).encode("utf-8")).hexdigest()


@dataclass(frozen=True)
class ValidationContext:
    """An immutable, defensively copied snapshot of expensive validation facts."""

    _encoded_values: Mapping[str, str]
    construction_seconds: float

    @classmethod
    def create(
        cls,
        builders: Mapping[str, Callable[[], Any]],
    ) -> "ValidationContext":
        started = time.perf_counter()
        values = {
            name: _encode(builder())
            for name, builder in builders.items()
        }
        return cls(
            MappingProxyType(values),
            time.perf_counter() - started,
        )

    @classmethod
    def from_document(
        cls,
        document: Mapping[str, Any],
        *,
        expected_digest: str | None = None,
    ) -> "ValidationContext":
        if document.get("schema_version") != 1:
            raise ValueError("unsupported validation context schema")
        values = document.get("values")
        if not isinstance(values, dict) or not values:
            raise ValueError("validation context values are missing")
        payload = {"schema_version": 1, "values": values}
        actual_digest = _digest(payload)
        recorded_digest = document.get("context_sha256")
        if recorded_digest != actual_digest:
            raise ValueError("validation context digest is invalid")
        if expected_digest is not None and actual_digest != expected_digest:
            raise ValueError("validation context does not match session evidence")
        return cls(
            MappingProxyType({
                str(name): _encode(value)
                for name, value in values.items()
            }),
            float(document.get("construction_seconds") or 0.0),
        )

    @classmethod
    def read(
        cls,
        path: Path,
        *,
        expected_digest: str | None = None,
    ) -> "ValidationContext":
        return cls.from_document(
            json.loads(path.read_text(encoding="utf-8")),
            expected_digest=expected_digest,
        )

    def value(self, name: str) -> Any:
        try:
            encoded = self._encoded_values[name]
        except KeyError as exc:
            raise KeyError(f"validation context omits {name}") from exc
        return json.loads(encoded)

    def has(self, name: str) -> bool:
        return name in self._encoded_values

    @property
    def names(self) -> tuple[str, ...]:
        return tuple(sorted(self._encoded_values))

    @property
    def digest(self) -> str:
        return _digest(self._payload())

    def _payload(self) -> dict[str, Any]:
        return {
            "schema_version": 1,
            "values": {
                name: json.loads(encoded)
                for name, encoded in sorted(self._encoded_values.items())
            },
        }

    def to_document(self) -> dict[str, Any]:
        payload = self._payload()
        return {
            **payload,
            "construction_seconds": self.construction_seconds,
            "context_sha256": _digest(payload),
        }

    def write(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary = path.with_name(f".{path.name}.{os.getpid()}.tmp")
        try:
            temporary.write_text(
                json.dumps(
                    self.to_document(),
                    indent=2,
                    ensure_ascii=False,
                    sort_keys=True,
                ) + "\n",
                encoding="utf-8",
                newline="\n",
            )
            os.replace(temporary, path)
        finally:
            temporary.unlink(missing_ok=True)


class BuilderRebuildCache:
    """Defensive process-local memoization for deterministic builder fixtures."""

    def __init__(self) -> None:
        self._values: dict[str, Any] = {}
        self._lock = RLock()

    def get_or_build(self, name: str, builder: Callable[[], Any]) -> Any:
        with self._lock:
            if name not in self._values:
                self._values[name] = copy.deepcopy(builder())
            return copy.deepcopy(self._values[name])

    def clear(self) -> None:
        with self._lock:
            self._values.clear()

    def keys(self) -> tuple[str, ...]:
        with self._lock:
            return tuple(sorted(self._values))
