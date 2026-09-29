#!/usr/bin/env python3
"""Fail when a tracked file exceeds the size limit unless it is allowlisted.

The allowlist is a ratchet: paths may be removed when a file shrinks or
leaves the tree, and may not gain paths after the list is first committed.
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[2]
ALLOWLIST_RELATIVE = "tools/repo_slimming/large_file_allowlist.json"
DEFAULT_LIMIT_BYTES = 5 * 1024 * 1024


def load_allowlist(path: Path) -> dict[str, Any]:
    document = json.loads(path.read_text(encoding="utf-8"))
    paths = document.get("paths")
    if not isinstance(paths, list) or not all(isinstance(item, str) for item in paths):
        raise ValueError(f"{path}: paths must be a list of strings")
    limit = int(document.get("limit_bytes", DEFAULT_LIMIT_BYTES))
    if limit <= 0:
        raise ValueError(f"{path}: limit_bytes must be positive")
    return {
        "limit_bytes": limit,
        "paths": sorted(set(paths)),
    }


def oversized_paths(
    sizes: dict[str, int],
    allowlist: set[str],
    *,
    limit_bytes: int,
) -> list[str]:
    return sorted(
        path
        for path, size in sizes.items()
        if size > limit_bytes and path not in allowlist
    )


def stale_allowlist_paths(
    sizes: dict[str, int],
    allowlist: set[str],
    *,
    limit_bytes: int,
) -> list[str]:
    """Allowlist entries that are gone or no longer over the limit."""
    return sorted(
        path
        for path in allowlist
        if sizes.get(path, 0) <= limit_bytes
    )


def allowlist_growth(previous: set[str] | None, current: set[str]) -> list[str]:
    if previous is None:
        return []
    return sorted(current - previous)


def tracked_file_sizes(root: Path) -> dict[str, int]:
    """Index blob sizes, i.e. the bytes Git stores.

    Working-tree sizes differ by platform when core.autocrlf rewrites line
    endings, so a Windows checkout and a Linux CI runner would disagree.
    """
    listed = subprocess.run(
        ["git", "ls-files", "-s", "-z"],
        cwd=root,
        capture_output=True,
        check=True,
    )
    blobs: dict[str, str] = {}
    for raw in listed.stdout.split(b"\0"):
        if not raw:
            continue
        meta, _, name = raw.partition(b"\t")
        mode, sha, _stage = meta.decode("ascii").split()
        if mode == "160000":
            continue
        blobs[name.decode("utf-8").replace("\\", "/")] = sha
    if not blobs:
        return {}
    unique = sorted(set(blobs.values()))
    checked = subprocess.run(
        ["git", "cat-file", "--batch-check=%(objectname) %(objectsize)"],
        cwd=root,
        input="\n".join(unique) + "\n",
        capture_output=True,
        text=True,
        check=True,
    )
    size_by_sha: dict[str, int] = {}
    for line in checked.stdout.splitlines():
        sha, size = line.split()
        size_by_sha[sha] = int(size)
    return {relative: size_by_sha[sha] for relative, sha in blobs.items()}


def _git_show(root: Path, revision: str, relative: str) -> str | None:
    completed = subprocess.run(
        ["git", "show", f"{revision}:{relative}"],
        cwd=root,
        capture_output=True,
        check=False,
    )
    if completed.returncode != 0:
        return None
    return completed.stdout.decode("utf-8")


def previous_allowlist_paths(root: Path, relative: str = ALLOWLIST_RELATIVE) -> set[str] | None:
    """Baseline for the ratchet.

    A dirty allowlist is compared with HEAD. A clean tree is compared with
    the parent commit, so CI rejects the commit that added a path. The first
    commit that introduces the file has no baseline.
    """
    head_text = _git_show(root, "HEAD", relative)
    if head_text is None:
        return None
    working = (root / relative).read_text(encoding="utf-8")
    baseline_text = head_text
    if working == head_text:
        parent_text = _git_show(root, "HEAD^", relative)
        if parent_text is None:
            return None
        baseline_text = parent_text
    document = json.loads(baseline_text)
    return set(document.get("paths") or [])


def collect_findings(root: Path | None = None) -> list[str]:
    root = ROOT if root is None else root
    allowlist_path = root / ALLOWLIST_RELATIVE
    document = load_allowlist(allowlist_path)
    limit = int(document["limit_bytes"])
    listed = set(document["paths"])
    sizes = tracked_file_sizes(root)
    findings: list[str] = []
    for path in oversized_paths(sizes, listed, limit_bytes=limit):
        findings.append(
            f"{path}: {sizes[path]} bytes exceeds {limit} and is not allowlisted"
        )
    for path in stale_allowlist_paths(sizes, listed, limit_bytes=limit):
        findings.append(
            f"{path}: allowlist entry is not a tracked file over {limit} bytes; remove it"
        )
    for path in allowlist_growth(previous_allowlist_paths(root), listed):
        findings.append(f"{path}: large-file allowlist may only shrink")
    return findings


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="scan tracked files and exit 1 when the ratchet fails",
    )
    args = parser.parse_args(argv)
    if not args.check:
        parser.error("pass --check")
    findings = collect_findings()
    if findings:
        print("\n".join(findings), file=sys.stderr)
        return 1
    print(f"large-file ratchet ok ({ALLOWLIST_RELATIVE})")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
