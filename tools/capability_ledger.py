#!/usr/bin/env python3
"""Semantic capability declarations, profiles, and impact graph."""
from __future__ import annotations

import argparse
import fnmatch
import json
import re
import subprocess
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
CARD_PLANS = io.ROOT / "docs" / "history" / "card-plans"
CARD_PLAN_LANES = ("active", "prep", "closed")
SLUG_RE = re.compile(
    r"^[a-z][a-z0-9]*(?:-[a-z0-9]+)*(?:/[a-z][a-z0-9]*(?:-[a-z0-9]+)*)+$"
)
CAPABILITY_SLUG_LINE = re.compile(
    r"^capability_slug\s*=\s*(\S+)\s*$",
    re.MULTILINE,
)
PLAN_SLUG_LINE = re.compile(r"计划 slug：`([^`]+)`")
MATURITY = ("frozen", "runtime_ready")
WORKFLOW = ("active", "paused", "accepted")
SURVIVAL_ACCESS = (
    "unreviewed",
    "blocked",
    "partial",
    "complete",
    "not_applicable",
)


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
    if document.get("maturity") == "player_complete":
        raise ValueError(f"{slug}: player_complete maturity is abolished")
    if document.get("maturity") not in MATURITY:
        raise ValueError(f"{slug}: invalid maturity")
    if document.get("workflow") not in WORKFLOW:
        raise ValueError(f"{slug}: invalid workflow")
    survival = document.get("survival_access")
    if survival is not None and survival not in SURVIVAL_ACCESS:
        raise ValueError(f"{slug}: invalid survival_access")
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
    return document


def parse_card_plan_slug(path: Path) -> str | None:
    """Return the capability slug declared by a card-plan markdown file."""
    text = path.read_text(encoding="utf-8")
    match = CAPABILITY_SLUG_LINE.search(text)
    if match:
        value = match.group(1).strip().strip("`")
        if value and value not in {"null", "None", "none"}:
            return value
    match = PLAN_SLUG_LINE.search(text)
    if match:
        value = match.group(1).strip()
        if value and value not in {"null", "None", "none"}:
            return value
    return None


def card_plan_title(path: Path) -> str:
    for line in path.read_text(encoding="utf-8").splitlines():
        if line.startswith("# "):
            return line[2:].strip()
    return path.stem


def card_plan_files(lane: str) -> list[Path]:
    directory = CARD_PLANS / lane
    if not directory.is_dir():
        return []
    return sorted(path for path in directory.glob("*.md") if path.is_file())


def load_card_plan_index() -> dict[str, dict[str, Path]]:
    """Map each plan lane to capability_slug → markdown path."""
    index: dict[str, dict[str, Path]] = {lane: {} for lane in CARD_PLAN_LANES}
    for lane in CARD_PLAN_LANES:
        for path in card_plan_files(lane):
            slug = parse_card_plan_slug(path)
            if slug is None:
                if lane == "active":
                    raise ValueError(
                        f"{io.relative(path)}: active plan missing capability_slug"
                    )
                continue
            existing = index[lane].get(slug)
            if existing is not None:
                raise ValueError(
                    f"duplicate capability_slug {slug} in {lane}: "
                    f"{io.relative(path)} and {io.relative(existing)}"
                )
            index[lane][slug] = path
    return index


def _validate_unique_active_plans(
    unique_active_slug: str | None,
    capabilities: list[dict[str, Any]],
    plans: dict[str, dict[str, Path]],
) -> None:
    by_slug = {row["slug"]: row for row in capabilities}
    active_plans = plans["active"]
    prep_plans = plans["prep"]
    closed_plans = plans["closed"]
    if unique_active_slug is None:
        if active_plans:
            extras = ", ".join(sorted(active_plans))
            raise ValueError(
                "card-plans/active must be empty when no capability is workflow=active; "
                f"found {extras}"
            )
    else:
        if unique_active_slug not in active_plans:
            raise ValueError(
                f"{unique_active_slug}: workflow=active requires a plan in "
                "docs/history/card-plans/active/"
            )
        if len(active_plans) != 1:
            extras = ", ".join(sorted(active_plans))
            raise ValueError(
                "card-plans/active must contain exactly one capability plan; "
                f"found {extras}"
            )
        for lane in ("prep", "closed"):
            if unique_active_slug in plans[lane]:
                raise ValueError(
                    f"{unique_active_slug}: active capability still has a {lane} plan"
                )
    for slug, path in active_plans.items():
        capability = by_slug.get(slug)
        if capability is None:
            raise ValueError(
                f"{io.relative(path)}: active plan {slug} has no capability.json"
            )
        if capability["workflow"] != "active":
            raise ValueError(
                f"{slug}: plan is in card-plans/active/ but workflow="
                f"{capability['workflow']}"
            )
    for slug, path in prep_plans.items():
        capability = by_slug.get(slug)
        if capability is not None and capability["workflow"] == "active":
            raise ValueError(
                f"{slug}: prep plan {io.relative(path)} cannot occupy unique-active"
            )
        if slug in active_plans:
            raise ValueError(f"{slug}: plan is in both prep/ and active/")
    for slug, path in closed_plans.items():
        if slug in active_plans or slug in prep_plans:
            raise ValueError(
                f"{slug}: closed plan {io.relative(path)} still has an open lane copy"
            )
        capability = by_slug.get(slug)
        if capability is not None and capability["workflow"] == "active":
            raise ValueError(
                f"{slug}: workflow=active but plan is in card-plans/closed/"
            )
    for row in capabilities:
        slug = row["slug"]
        if row["workflow"] == "active" and slug not in active_plans:
            raise ValueError(
                f"{slug}: workflow=active requires docs/history/card-plans/active/"
            )


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
            "survival_access": document.get("survival_access") or "unreviewed",
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
    active_workflows = [
        row["slug"] for row in capabilities if row["workflow"] == "active"
    ]
    if len(active_workflows) > 1:
        raise ValueError(
            "multiple workflow=active capabilities: " + ", ".join(active_workflows)
        )
    unique_active_slug = active_workflows[0] if active_workflows else None
    _validate_unique_active_plans(
        unique_active_slug,
        capabilities,
        load_card_plan_index(),
    )
    profiles: dict[str, list[str]] = {}
    for row in capabilities:
        for profile in row["profiles"]:
            profiles.setdefault(profile, []).append(row["slug"])
    compiled_profiles = {
        profile: sorted(profile_slugs)
        for profile, profile_slugs in sorted(profiles.items())
    }
    profiled_complete = compiled_profiles.get("player-complete") or []
    if profiled_complete:
        raise ValueError(
            "player-complete profile is abolished; still joined by "
            + ", ".join(profiled_complete)
        )
    return {
        "capability_count": len(capabilities),
        "capabilities": capabilities,
        "declared_player_complete": [],
        "generated_by": "tools/build_capability_ledger.py",
        "impact": dependency_impact(capabilities),
        "profiles": compiled_profiles,
        "progress_rule": (
            "runtime_ready is the close maturity; survival_access is independent "
            "and does not gate close"
        ),
        "schema_version": 2,
        "unique_active_slug": unique_active_slug,
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


def _git_show(revision: str, relative: str) -> str | None:
    completed = subprocess.run(
        ["git", "show", f"{revision}:{relative.replace('\\', '/')}"],
        cwd=ROOT,
        capture_output=True,
        check=False,
    )
    if completed.returncode != 0:
        return None
    return completed.stdout.decode("utf-8", errors="replace")


def player_complete_promotions(base_revision: str) -> list[str]:
    """Abolished. CI must not auto-run runClient for a maturity promotion."""
    del base_revision
    return []


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
