#!/usr/bin/env python3
"""List compact publication-group count and stable-id changes.

Compares publication policies and the compact recipe JSON that changed
between two git revisions, or between a revision and the working tree.

    python tools/waves/tooling/gametest-derived-counts/policy_delta.py
    python tools/waves/tooling/gametest-derived-counts/policy_delta.py --base abdb2bb61
    python tools/waves/tooling/gametest-derived-counts/policy_delta.py --json
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Iterable

ROOT = Path(__file__).resolve().parents[4]
RECIPE_ROOT = (
    "src/recipe_generated/resources/data/cruciblecraft/recipe"
)
POLICY_ROOT = f"{RECIPE_ROOT}/publication_policy"
GROUP_RE = re.compile(rb'"publication_group"\s*:\s*"([^"]+)"')
STABLE_ID_RE = re.compile(rb'"stable_id"\s*:\s*"([^"]+)"')
DEFAULT_ID_LIMIT = 20


class Tree:
    """A git revision, the working tree, or a fixture directory."""

    def __init__(
        self,
        *,
        revision: str | None = None,
        directory: Path | None = None,
        worktree: bool = False,
    ) -> None:
        self.revision = revision
        self.directory = directory
        self.worktree = worktree

    def label(self) -> str:
        if self.directory is not None:
            return str(self.directory)
        if self.worktree:
            return "worktree"
        return self.revision or "HEAD"

    def read(self, relative: str) -> bytes | None:
        relative = relative.replace("\\", "/")
        if self.directory is not None:
            path = self.directory / relative
            if not path.is_file():
                return None
            return path.read_bytes()
        if self.worktree:
            path = ROOT / relative
            if not path.is_file():
                return None
            return path.read_bytes()
        shown = subprocess.run(
            ["git", "show", f"{self.revision}:{relative}"],
            cwd=ROOT,
            capture_output=True,
        )
        if shown.returncode != 0:
            return None
        return shown.stdout

    def list_files(self, prefix: str) -> list[str]:
        prefix = prefix.replace("\\", "/").rstrip("/")
        if self.directory is not None:
            root = self.directory / prefix
            if not root.is_dir():
                return []
            return sorted(
                (prefix + "/" + path.relative_to(root).as_posix())
                for path in root.rglob("*.json")
            )
        if self.worktree:
            root = ROOT / prefix
            if not root.is_dir():
                return []
            return sorted(
                path.relative_to(ROOT).as_posix()
                for path in root.rglob("*.json")
            )
        listed = subprocess.run(
            ["git", "ls-tree", "-r", "--name-only", self.revision or "HEAD", "--", prefix],
            cwd=ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
        if listed.returncode != 0:
            raise RuntimeError(listed.stderr.strip() or "git ls-tree failed")
        return sorted(
            line.replace("\\", "/")
            for line in listed.stdout.splitlines()
            if line.endswith(".json")
        )


def load_policies(tree: Tree) -> dict[str, dict[str, object]]:
    policies: dict[str, dict[str, object]] = {}
    for relative in tree.list_files(POLICY_ROOT):
        raw = tree.read(relative)
        if raw is None:
            continue
        document = json.loads(raw.decode("utf-8"))
        group = str(document["publication_group"])
        policies[group] = {
            "relation_count": int(document["relation_count"]),
            "membership_root_sha256": str(document["membership_root_sha256"]),
            "family_count": int(document["family_count"]),
            "target_map": str(document["target_map"]),
            "file": relative,
        }
    return policies


def stable_ids(payload: bytes | None) -> tuple[str | None, set[str]]:
    if not payload:
        return None, set()
    group_match = GROUP_RE.search(payload)
    group = group_match.group(1).decode("utf-8") if group_match else None
    return group, {match.group(1).decode("utf-8") for match in STABLE_ID_RE.finditer(payload)}


def changed_recipe_files(base: Tree, head: Tree) -> list[str]:
    """Recipe JSON that differs, excluding the policy directory."""
    if base.directory is not None or head.directory is not None:
        names = set(base.list_files(RECIPE_ROOT)) | set(head.list_files(RECIPE_ROOT))
        changed = []
        for relative in sorted(names):
            if relative.startswith(POLICY_ROOT + "/"):
                continue
            if base.read(relative) != head.read(relative):
                changed.append(relative)
        return changed
    if head.worktree:
        diff = subprocess.run(
            ["git", "diff", "--name-only", "--diff-filter=ACDMRTUXB", base.revision or "HEAD", "--", RECIPE_ROOT],
            cwd=ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
        status = subprocess.run(
            ["git", "status", "--porcelain", "--untracked-files=all", "--", RECIPE_ROOT],
            cwd=ROOT,
            capture_output=True,
            text=True,
            encoding="utf-8",
        )
        if diff.returncode != 0 or status.returncode != 0:
            raise RuntimeError((diff.stderr or status.stderr).strip() or "git diff failed")
        names = {line.replace("\\", "/") for line in diff.stdout.splitlines() if line}
        for line in status.stdout.splitlines():
            path = line[3:].strip().replace("\\", "/")
            if " -> " in path:
                path = path.split(" -> ", 1)[1]
            if path.startswith(RECIPE_ROOT + "/") and path.endswith(".json"):
                names.add(path)
        return sorted(
            name for name in names if not name.startswith(POLICY_ROOT + "/")
        )
    diff = subprocess.run(
        [
            "git",
            "diff",
            "--name-only",
            "--diff-filter=ACDMRTUXB",
            base.revision or "HEAD",
            head.revision or "HEAD",
            "--",
            RECIPE_ROOT,
        ],
        cwd=ROOT,
        capture_output=True,
        text=True,
        encoding="utf-8",
    )
    if diff.returncode != 0:
        raise RuntimeError(diff.stderr.strip() or "git diff failed")
    return sorted(
        line.replace("\\", "/")
        for line in diff.stdout.splitlines()
        if line.endswith(".json") and not line.replace("\\", "/").startswith(POLICY_ROOT + "/")
    )


def id_delta(base: Tree, head: Tree, files: Iterable[str]) -> dict[str, dict[str, list[str]]]:
    grouped: dict[str, dict[str, set[str]]] = {}
    for relative in files:
        old_group, old_ids = stable_ids(base.read(relative))
        new_group, new_ids = stable_ids(head.read(relative))
        for group, side, ids in (
            (old_group, "old", old_ids),
            (new_group, "new", new_ids),
        ):
            if group is None:
                continue
            bucket = grouped.setdefault(group, {"old": set(), "new": set()})
            bucket[side].update(ids)
    report: dict[str, dict[str, list[str]]] = {}
    for group, bucket in grouped.items():
        added = sorted(bucket["new"] - bucket["old"])
        removed = sorted(bucket["old"] - bucket["new"])
        if added or removed:
            report[group] = {"added": added, "removed": removed}
    return report


def compare(base: Tree, head: Tree) -> dict[str, object]:
    old_policies = load_policies(base)
    new_policies = load_policies(head)
    identities = id_delta(base, head, changed_recipe_files(base, head))
    groups = sorted(set(old_policies) | set(new_policies) | set(identities))
    rows = []
    for group in groups:
        old = old_policies.get(group)
        new = new_policies.get(group)
        old_count = None if old is None else old["relation_count"]
        new_count = None if new is None else new["relation_count"]
        old_hash = None if old is None else old["membership_root_sha256"]
        new_hash = None if new is None else new["membership_root_sha256"]
        added = identities.get(group, {}).get("added", [])
        removed = identities.get(group, {}).get("removed", [])
        if (
            old_count == new_count
            and old_hash == new_hash
            and not added
            and not removed
        ):
            continue
        rows.append(
            {
                "publication_group": group,
                "relation_count": {"old": old_count, "new": new_count},
                "membership_changed": old_hash != new_hash,
                "added_stable_ids": added,
                "removed_stable_ids": removed,
            }
        )
    added_groups = sorted(set(new_policies) - set(old_policies))
    removed_groups = sorted(set(old_policies) - set(new_policies))
    return {
        "base": base.label(),
        "head": head.label(),
        "added_groups": added_groups,
        "removed_groups": removed_groups,
        "groups": rows,
    }


def render(report: dict[str, object], limit: int) -> str:
    lines = [
        f"base {report['base']}",
        f"head {report['head']}",
        f"added_groups {len(report['added_groups'])}",
        f"removed_groups {len(report['removed_groups'])}",
    ]
    for group in report["added_groups"]:
        lines.append(f"  + {group}")
    for group in report["removed_groups"]:
        lines.append(f"  - {group}")
    for row in report["groups"]:
        counts = row["relation_count"]
        added = row["added_stable_ids"]
        removed = row["removed_stable_ids"]
        lines.append(
            f"{row['publication_group']}: relation_count {counts['old']} -> {counts['new']}"
            f" membership_changed={str(row['membership_changed']).lower()}"
            f" added={len(added)} removed={len(removed)}"
        )
        lines.extend(f"  + {stable_id}" for stable_id in added[:limit])
        if len(added) > limit:
            lines.append(f"  + ... {len(added) - limit} more")
        lines.extend(f"  - {stable_id}" for stable_id in removed[:limit])
        if len(removed) > limit:
            lines.append(f"  - ... {len(removed) - limit} more")
    return "\n".join(lines) + "\n"


def parse_tree(value: str | None, *, worktree_default: bool) -> Tree:
    if value is None:
        if worktree_default:
            return Tree(worktree=True)
        return Tree(revision="HEAD")
    if value == "worktree":
        return Tree(worktree=True)
    return Tree(revision=value)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", default="HEAD", help="git revision (default HEAD)")
    parser.add_argument(
        "--head",
        default="worktree",
        help="git revision, or worktree (default)",
    )
    parser.add_argument("--base-dir", type=Path, help="fixture directory instead of --base")
    parser.add_argument("--head-dir", type=Path, help="fixture directory instead of --head")
    parser.add_argument("--json", action="store_true")
    parser.add_argument("--id-limit", type=int, default=DEFAULT_ID_LIMIT)
    args = parser.parse_args(argv)
    base = Tree(directory=args.base_dir) if args.base_dir else parse_tree(args.base, worktree_default=False)
    head = Tree(directory=args.head_dir) if args.head_dir else parse_tree(args.head, worktree_default=True)
    report = compare(base, head)
    if args.json:
        json.dump(report, sys.stdout, indent=2, ensure_ascii=False)
        sys.stdout.write("\n")
    else:
        sys.stdout.write(render(report, args.id_limit))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
