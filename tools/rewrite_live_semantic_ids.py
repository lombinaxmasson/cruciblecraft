#!/usr/bin/env python3
"""Rewrite live compact identities onto semantic host/cohort paths.

Does not touch archive/sealed or frozen v2 files. One-shot live migration;
safe to re-run (already-remapped files stay put).
"""
from __future__ import annotations

import argparse
import json
import shutil
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import semantic_ids
from tools import census_common as census
from tools.recipe_bulk.membership import membership_root

RECIPE_GENERATED = Path("src/recipe_generated")
SUPPORT_GENERATED = Path("src/recipe_support_generated")
SKIP_PARTS = {"archive", ".git", ".gradle", "build", "__pycache__"}


def _posix(path: Path) -> str:
    return path.as_posix()


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(document, indent=2, ensure_ascii=False) + "\n"
    path.write_text(text, encoding="utf-8")


def _is_archive(path: Path) -> bool:
    return "archive" in path.resolve().parts


def dest_for_generated(path: Path) -> Path | None:
    rel = path.resolve().relative_to(census.ROOT).as_posix()
    mapped = None
    for root in semantic_ids.live_generated_roots():
        old = census.relative(root)
        prefix = old.rstrip("/") + "/"
        if rel == old or rel.startswith(prefix):
            rest = rel[len(old) :].lstrip("/")
            if "support_generated" in old:
                mapped = (SUPPORT_GENERATED / rest).as_posix()
            else:
                mapped = (RECIPE_GENERATED / rest).as_posix()
            break
    if mapped is None:
        return None
    mapped = semantic_ids.remap_recipe_path(mapped)
    parent, slash, name = mapped.rpartition("/")
    if "/publication_policy/" in mapped + "/":
        name = semantic_ids.remap_policy_filename(name)
        mapped = f"{parent}/{name}" if slash else name
    return census.ROOT / mapped


def dest_for_named_json(path: Path) -> Path:
    parent = path.parent
    name = semantic_ids.remap_dedup_filename(path.name)
    name = semantic_ids.remap_policy_filename(name)
    return parent / name


def rewrite_json_file(src: Path, dest: Path) -> None:
    document = json.loads(src.read_text(encoding="utf-8"))
    remapped = semantic_ids.remap_json_value(document, src.as_posix())
    if dest.exists() and dest.resolve() != src.resolve():
        existing = json.loads(dest.read_text(encoding="utf-8"))
        if existing != remapped:
            raise SystemExit(f"refusing to overwrite drifted dest {census.relative(dest)}")
    _write_json(dest, remapped)
    if dest.resolve() != src.resolve():
        src.unlink()


def rewrite_generated_trees() -> list[str]:
    moved: list[str] = []
    for root in semantic_ids.live_generated_roots():
        if not root.exists() or _is_archive(root):
            continue
        files = sorted(path for path in root.rglob("*") if path.is_file())
        for src in files:
            dest = dest_for_generated(src)
            if dest is None:
                continue
            if src.suffix.lower() == ".json":
                rewrite_json_file(src, dest)
            else:
                dest.parent.mkdir(parents=True, exist_ok=True)
                if dest.resolve() != src.resolve():
                    shutil.move(str(src), str(dest))
            moved.append(census.relative(dest))
        _remove_empty_dirs(root)
    return moved


def rewrite_main_recovery() -> list[str]:
    moved: list[str] = []
    recipe_root = census.ROOT / "src/main/resources/data/cruciblecraft/recipe"
    if not recipe_root.is_dir():
        return moved
    for row in semantic_ids._prefix_rows("support_path_prefixes"):
        old = str(row.get("old") or "").strip("/")
        new = str(row.get("new") or "").strip("/")
        if not old or not new:
            continue
        src_dir = recipe_root / old
        if not src_dir.is_dir():
            continue
        dest_dir = recipe_root / new
        for src in sorted(src_dir.rglob("*.json")):
            dest = dest_dir / src.relative_to(src_dir)
            rewrite_json_file(src, dest)
            moved.append(census.relative(dest))
        _remove_empty_dirs(src_dir)
    return moved


def rewrite_policy_and_dedup_trees() -> list[str]:
    moved: list[str] = []
    roots = [
        census.ROOT / "src/compact_recipe_policy_generated",
        census.ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe/dedup_rule",
        census.ROOT / "src/recipe_generated/resources/data/cruciblecraft/recipe/publication_policy",
    ]
    for root in roots:
        if not root.exists():
            continue
        for src in sorted(root.rglob("*.json")):
            dest = dest_for_named_json(src)
            rewrite_json_file(src, dest)
            moved.append(census.relative(dest))
    return moved


def rewrite_wave_routing_schema() -> list[str]:
    updated: list[str] = []
    waves = census.ROOT / "tools" / "waves"
    if not waves.is_dir():
        return updated
    old_versions = set(semantic_ids.routing_schema_old())
    new_version = semantic_ids.routing_schema_new()
    for path in sorted(waves.rglob("*.json")):
        text = path.read_text(encoding="utf-8")
        original = text
        for old in old_versions:
            text = text.replace(old, new_version)
        if text != original:
            path.write_text(text, encoding="utf-8")
            updated.append(census.relative(path))
    return updated


def recompute_policy_membership() -> list[str]:
    family_root = (
        census.ROOT
        / "src/recipe_generated/resources/data/cruciblecraft/recipe"
    )
    if not family_root.is_dir():
        return []
    by_group: dict[str, dict[str, list[str]]] = defaultdict(
        lambda: {"family_ids": [], "stable_ids": []}
    )
    skip = {"publication_policy", "dedup_rule"}
    for path in sorted(family_root.rglob("*.json")):
        if any(part in skip for part in path.parts):
            continue
        document = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(document, dict):
            continue
        group = str(document.get("publication_group") or "")
        if not group:
            inferred = semantic_ids.inferred_publication_group(path.as_posix())
            if inferred:
                document["publication_group"] = inferred
                _write_json(path, document)
                group = inferred
        family_id = str(document.get("family_id") or "")
        if not group or not family_id:
            continue
        by_group[group]["family_ids"].append(family_id)
        for relation in document.get("relations") or []:
            if isinstance(relation, dict) and relation.get("stable_id"):
                by_group[group]["stable_ids"].append(str(relation["stable_id"]))
    updated: list[str] = []
    policy_root = family_root / "publication_policy"
    if not policy_root.is_dir():
        return updated
    for path in sorted(policy_root.glob("*.json")):
        document = json.loads(path.read_text(encoding="utf-8"))
        group = str(document.get("publication_group") or "")
        bucket = by_group.get(group)
        if not bucket:
            continue
        document["membership_root_sha256"] = membership_root(
            bucket["family_ids"],
            bucket["stable_ids"],
        )
        document["family_count"] = len(bucket["family_ids"])
        document["relation_count"] = len(bucket["stable_ids"])
        _write_json(path, document)
        updated.append(census.relative(path))
    return updated


def _remove_empty_dirs(root: Path) -> None:
    if not root.exists():
        return
    for path in sorted(root.rglob("*"), reverse=True):
        if path.is_dir():
            try:
                path.rmdir()
            except OSError:
                pass
    if root.is_dir():
        try:
            root.rmdir()
        except OSError:
            pass


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.check == args.write:
        print("choose --write or --check", file=sys.stderr)
        return 2
    remaining = [
        census.relative(root)
        for root in semantic_ids.live_generated_roots()
        if root.exists()
    ]
    if args.check:
        if remaining:
            print("live generated roots still present:", file=sys.stderr)
            print("\n".join(remaining), file=sys.stderr)
            return 1
        print("live compact generated trees are on semantic paths")
        return 0
    generated = rewrite_generated_trees()
    recovery = rewrite_main_recovery()
    policies = rewrite_policy_and_dedup_trees()
    waves = rewrite_wave_routing_schema()
    membership = recompute_policy_membership()
    print(
        f"rewrote generated={len(generated)} recovery={len(recovery)} "
        f"policy={len(policies)} waves={len(waves)} membership={len(membership)}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
