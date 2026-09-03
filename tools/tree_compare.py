#!/usr/bin/env python3
"""Compare generated trees by relative path and exact file bytes."""
from __future__ import annotations

from pathlib import Path
from typing import Iterable


DEFAULT_IGNORED_PARTS = frozenset({".cache", "__pycache__"})


def file_map(
    root: Path,
    *,
    ignored_parts: Iterable[str] = DEFAULT_IGNORED_PARTS,
) -> dict[str, Path]:
    ignored = set(ignored_parts)
    if not root.is_dir():
        return {}
    return {
        path.relative_to(root).as_posix(): path
        for path in sorted(root.rglob("*"))
        if path.is_file()
        and not ignored.intersection(path.relative_to(root).parts)
    }


def same_bytes(left: Path, right: Path) -> bool:
    if left.stat().st_size != right.stat().st_size:
        return False
    with left.open("rb") as left_file, right.open("rb") as right_file:
        while True:
            left_chunk = left_file.read(128 * 1024)
            right_chunk = right_file.read(128 * 1024)
            if left_chunk != right_chunk:
                return False
            if not left_chunk:
                return True


def compare_trees(left: Path, right: Path) -> list[str]:
    left_files = file_map(left)
    right_files = file_map(right)
    errors: list[str] = []
    for relative in sorted(set(left_files) - set(right_files)):
        errors.append(f"missing from second tree: {relative}")
    for relative in sorted(set(right_files) - set(left_files)):
        errors.append(f"extra in second tree: {relative}")
    for relative in sorted(set(left_files) & set(right_files)):
        if not same_bytes(left_files[relative], right_files[relative]):
            errors.append(f"file bytes differ: {relative}")
    return errors
