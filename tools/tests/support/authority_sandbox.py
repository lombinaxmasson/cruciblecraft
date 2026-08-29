#!/usr/bin/env python3
"""Copy committed authority files into a temp tree for mutating tests."""
from __future__ import annotations

import fnmatch
import json
import os
import shutil
import tempfile
from collections.abc import Iterator
from contextlib import contextmanager
from pathlib import Path
from unittest import mock

ROOT = Path(__file__).resolve().parents[3]
MANIFEST = ROOT / "tools" / "authority_manifest.json"

_installed = False
_original_write_text = Path.write_text
_original_write_bytes = Path.write_bytes
_original_replace = os.replace


def load_globs() -> tuple[str, ...]:
    document = json.loads(MANIFEST.read_text(encoding="utf-8"))
    return tuple(document.get("protected_path_globs") or [])


def relative_to_root(path: Path) -> str | None:
    try:
        return path.resolve().relative_to(ROOT.resolve()).as_posix()
    except (OSError, ValueError):
        return None


def is_protected(path: Path) -> bool:
    relative = relative_to_root(Path(path))
    if relative is None:
        return False
    return any(fnmatch.fnmatchcase(relative, glob) for glob in load_globs())


class AuthorityWriteError(PermissionError):
    """A test tried to mutate a committed authority artifact."""


def _reject(path: Path) -> None:
    relative = relative_to_root(Path(path)) or str(path)
    raise AuthorityWriteError(
        f"test write to committed authority path is forbidden: {relative}"
    )


def _guarded_write_text(self: Path, *args, **kwargs):
    if is_protected(self):
        _reject(self)
    return _original_write_text(self, *args, **kwargs)


def _guarded_write_bytes(self: Path, *args, **kwargs):
    if is_protected(self):
        _reject(self)
    return _original_write_bytes(self, *args, **kwargs)


def _guarded_replace(src, dst, *args, **kwargs):
    if is_protected(Path(dst)):
        _reject(Path(dst))
    return _original_replace(src, dst, *args, **kwargs)


def install_write_guard() -> None:
    """Patch Path/os writers so tests cannot mutate committed authority files."""
    global _installed
    if _installed:
        return
    Path.write_text = _guarded_write_text  # type: ignore[method-assign]
    Path.write_bytes = _guarded_write_bytes  # type: ignore[method-assign]
    os.replace = _guarded_replace  # type: ignore[assignment]
    _installed = True


@contextmanager
def sandbox_copy(original: Path) -> Iterator[Path]:
    """Yield a writable copy of *original* outside the protected tree."""
    original = Path(original)
    tmp = Path(tempfile.mkdtemp(prefix="authority_sandbox_"))
    copy = tmp / original.name
    if original.is_file():
        shutil.copy2(original, copy)
    try:
        yield copy
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


@contextmanager
def patch_builder_path(module, attribute: str) -> Iterator[Path]:
    original = getattr(module, attribute)
    with sandbox_copy(original) as copy:
        with mock.patch.object(module, attribute, copy):
            yield copy
