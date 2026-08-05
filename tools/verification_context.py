"""Process-local immutable values shared by full verification checks."""
from __future__ import annotations

import copy
import json
import time
from dataclasses import dataclass
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
