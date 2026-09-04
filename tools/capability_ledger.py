#!/usr/bin/env python3
"""Semantic capability declarations, profiles, and impact graph."""
from __future__ import annotations

import argparse
import fnmatch
import json
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import atomic_io
from tools import io_common as io

CAP_ROOT = io.TOOLS / "capabilities"
LEDGER = CAP_ROOT / "ledger.json"
VERIFICATION_PROFILES = io.TOOLS / "verification_profiles.json"
SLUG_RE = re.compile(
    r"^[a-z][a-z0-9]*(?:-[a-z0-9]+)*(?:/[a-z][a-z0-9]*(?:-[a-z0-9]+)*)+$"
)
MATURITY = ("frozen", "runtime_ready", "player_complete")
WORKFLOW = ("active", "paused", "accepted")


def dumps(document: Any) -> bytes:
    return (
        json.dumps(
            document,
            indent=2,
            sort_keys=True,
            ensure_ascii=False,
        )
        + "\n"
    ).encode("utf-8")


def capability_files() -> list[Path]:
    return sorted(
        path for path in CAP_ROOT.rglob("capability.json") if path.is_file()
    )


def load_capability(path: Path) -> dict[str, Any]:
    document = io.load_json(path)
    if document.get("schema_version") != 2:
        raise ValueError(f"{io.relative(path)}: unsupported schema")
    if "evidence" in document:
        raise ValueError(
            f"{io.relative(path)}: schema v2 forbids static evidence"
        )
    slug = str(document.get("slug") or "")
    if not SLUG_RE.match(slug):
        raise ValueError(f"{io.relative(path)}: invalid slug {slug!r}")
    expected = path.relative_to(CAP_ROOT).parent.as_posix()
    if slug != expected:
        raise ValueError(
            f"{io.relative(path)}: slug {slug!r} != directory {expected!r}"
        )
    if document.get("maturity") not in MATURITY:
        raise ValueError(f"{slug}: invalid maturity")
    if document.get("workflow") not in WORKFLOW:
        raise ValueError(f"{slug}: invalid workflow")
    owned = document.get("owned_paths")
    if (
        not isinstance(owned, list)
        or not owned
        or not all(isinstance(value, str) and value for value in owned)
    ):
        raise ValueError(f"{slug}: owned_paths required")
    profiles = document.get("profiles")
    if (
        not isinstance(profiles, list)
        or not profiles
        or not all(isinstance(value, str) and value for value in profiles)
    ):
        raise ValueError(f"{slug}: profiles required")
    if document.get("maturity") == "player_complete":
        if document.get("workflow") != "accepted":
            raise ValueError(
                f"{slug}: player_complete requires workflow=accepted"
            )
        if not document.get("player_signoff"):
            raise ValueError(f"{slug}: player_complete requires player_signoff")
    return document


def match_owned(owned_paths: list[str], rel: str) -> bool:
    posix = rel.replace("\\", "/")
    for pattern in owned_paths:
        if fnmatch.fnmatch(posix, pattern.replace("\\", "/")):
            return True
    return False


def dependency_impact(
    capabilities: list[dict[str, Any]],
) -> dict[str, list[str]]:
    """Return each declaration and all of its transitive dependents."""
    by_slug = {document["slug"]: document for document in capabilities}
    impact: dict[str, list[str]] = {}
    for source in sorted(by_slug):
        affected = {source}
        changed = True
        while changed:
            changed = False
            for slug, document in by_slug.items():
                if slug in affected:
                    continue
                if set(document.get("depends_on") or []) & affected:
                    affected.add(slug)
                    changed = True
        impact[source] = sorted(affected)
    return impact


def compile_ledger() -> dict[str, Any]:
    capabilities: list[dict[str, Any]] = []
    for path in capability_files():
        document = load_capability(path)
        record = {
            "depends_on": list(document.get("depends_on") or []),
            "maturity": document["maturity"],
            "owned_paths": list(document["owned_paths"]),
            "path": io.relative(path),
            "player_signoff": document.get("player_signoff"),
            "profiles": list(document.get("profiles") or []),
            "slug": document["slug"],
            "title": document["title"],
            "workflow": document["workflow"],
        }
        capabilities.append(record)
    slugs = [row["slug"] for row in capabilities]
    if len(slugs) != len(set(slugs)):
        raise ValueError("duplicate capability slug")
    known = set(slugs)
    profile_document = io.load_json(VERIFICATION_PROFILES)
    active_profiles = set(profile_document.get("active_profiles") or [])
    for row in capabilities:
        for dep in row["depends_on"]:
            if dep not in known:
                raise ValueError(f"{row['slug']} depends on missing {dep}")
        unknown_profiles = sorted(set(row["profiles"]) - active_profiles)
        if unknown_profiles:
            raise ValueError(
                f"{row['slug']} names inactive profiles {unknown_profiles}"
            )
    declared_player_complete = [
        row["slug"]
        for row in capabilities
        if row["maturity"] == "player_complete"
    ]
    profiles: dict[str, list[str]] = {}
    for row in capabilities:
        for profile in row["profiles"]:
            profiles.setdefault(profile, []).append(row["slug"])
    return {
        "capability_count": len(capabilities),
        "capabilities": capabilities,
        "declared_player_complete": declared_player_complete,
        "generated_by": "tools/build_capability_ledger.py",
        "impact": dependency_impact(capabilities),
        "profiles": {
            profile: sorted(profile_slugs)
            for profile, profile_slugs in sorted(profiles.items())
        },
        "progress_rule": (
            "declaration is not proof; player_complete requires fresh "
            "GameTestServer and runClient execution"
        ),
        "schema_version": 2,
    }


def affected_slugs(changed_paths: list[str]) -> list[str]:
    owned_hit: list[str] = []
    capabilities: list[dict[str, Any]] = []
    for path in capability_files():
        document = load_capability(path)
        capabilities.append(document)
        if any(
            match_owned(list(document["owned_paths"]), rel)
            for rel in changed_paths
        ):
            owned_hit.append(document["slug"])
    impact = dependency_impact(capabilities)
    return sorted(
        {
            affected
            for slug in owned_hit
            for affected in impact.get(slug, [slug])
        }
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--affected", nargs="*", default=None)
    args = parser.parse_args(argv)
    if args.affected is not None:
        slugs = affected_slugs(args.affected)
        print("\n".join(slugs) if slugs else "(none)")
        return 0
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    try:
        ledger = compile_ledger()
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 1
    encoded = dumps(ledger)
    if args.write:
        atomic_io.write_bytes(LEDGER, encoded)
        print(f"Wrote {io.relative(LEDGER)}")
        return 0
    if not LEDGER.is_file():
        print("missing tools/capabilities/ledger.json", file=sys.stderr)
        return 1
    if LEDGER.read_bytes() != encoded:
        print("capability ledger is stale; run --write", file=sys.stderr)
        return 1
    print("capability ledger is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
