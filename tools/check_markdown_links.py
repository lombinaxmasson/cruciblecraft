#!/usr/bin/env python3
"""Check Markdown links that resolve inside the repository."""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PATH_MAP = ROOT / "docs" / "history" / "path-map.json"
LINK_RE = re.compile(r"\[[^\]]*\]\(([^)]+)\)")
CURRENT_ROOTS = (
    ROOT / "README.md",
    ROOT / "CHANGELOG.md",
    ROOT / "CREDITS.md",
    ROOT / "NOTICE",
    ROOT / "LICENSE",
    ROOT / "docs",
    ROOT / "tools" / "README.md",
)


def load_path_map() -> dict[str, str]:
    if not PATH_MAP.is_file():
        return {}
    document = json.loads(PATH_MAP.read_text(encoding="utf-8"))
    return {
        str(old).replace("\\", "/"): str(new).replace("\\", "/")
        for old, new in document.get("moves", {}).items()
    }


def iter_markdown_files() -> list[Path]:
    files: list[Path] = []
    for root in CURRENT_ROOTS:
        if root.is_file() and root.suffix.lower() in {".md", ""}:
            if root.suffix.lower() == ".md":
                files.append(root)
            continue
        if root.is_dir():
            files.extend(sorted(root.rglob("*.md")))
    return [path for path in files if path.is_file()]


def is_external(target: str) -> bool:
    lowered = target.lower()
    return (
        lowered.startswith("http://")
        or lowered.startswith("https://")
        or lowered.startswith("mailto:")
        or target.startswith("#")
    )


def resolve_target(source: Path, raw_target: str, path_map: dict[str, str]) -> Path | None:
    target = raw_target.split("#", 1)[0].strip()
    if not target or is_external(target):
        return None
    target = target.replace("\\", "/")
    candidate = (source.parent / target).resolve()
    if candidate.exists():
        return candidate
    from_root = (ROOT / target).resolve()
    if from_root.exists():
        return from_root
    by_name = {Path(old).name: new for old, new in path_map.items()}
    mapped = (
        path_map.get(target)
        or path_map.get(target.lstrip("./"))
        or by_name.get(Path(target).name)
    )
    if mapped:
        mapped_path = (ROOT / mapped).resolve()
        if mapped_path.exists():
            return mapped_path
    old_root = ROOT / Path(target).name
    if old_root.exists():
        return old_root
    return candidate


def check() -> list[str]:
    path_map = load_path_map()
    errors: list[str] = []
    for source in iter_markdown_files():
        text = source.read_text(encoding="utf-8")
        relative_source = source.relative_to(ROOT).as_posix()
        for match in LINK_RE.finditer(text):
            raw = match.group(1).strip().strip("<>")
            resolved = resolve_target(source, raw, path_map)
            if resolved is None:
                continue
            if not resolved.exists():
                errors.append(
                    f"{relative_source}: missing {raw}"
                )
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.parse_args(argv)
    errors = check()
    if errors:
        print("Markdown link check failed:")
        for error in errors:
            print(f"- {error}")
        return 1
    print("Markdown link check passed")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
