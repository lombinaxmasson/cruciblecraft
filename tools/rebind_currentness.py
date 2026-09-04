#!/usr/bin/env python3
"""Plan, check, or write KB-level currentness sidecars without rewriting giant bodies."""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import currentness
from tools import census_common as census


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--plan", action="store_true")
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--write", action="store_true")
    parser.add_argument(
        "--scope",
        choices=("census", "card-closeout", "recipes", "all"),
        default="all",
    )
    parser.add_argument(
        "--rebind",
        action="store_true",
        help="Envelope rebind only; refuses builder hash updates",
    )
    parser.add_argument(
        "--builder",
        action="store_true",
        help="Update builder_sha256 after a proven semantic-root rebuild",
    )
    parser.add_argument(
        "--prove-rebuild",
        action="store_true",
        help="Required with --builder: caller already rebuilt and proved the root",
    )
    return parser.parse_args(argv)


def rows_for_scope(scope: str) -> list[dict[str, str]]:
    if scope == "all":
        return list(currentness.TARGETS)
    return [row for row in currentness.TARGETS if row["scope"] == scope]


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    if args.builder and not args.prove_rebuild:
        print("builder rebind requires --prove-rebuild", file=sys.stderr)
        return 2
    rows = rows_for_scope(args.scope)
    if args.plan:
        for row in rows:
            artifact = census.ROOT / row["artifact"]
            sidecar = census.ROOT / row["sidecar"]
            status = "missing-artifact"
            if artifact.is_file():
                status = "current" if sidecar.is_file() else "sidecar-missing"
            giant = " giant" if row["artifact"] in currentness.GIANT_ARTIFACTS else ""
            print(f"{row['sidecar']}: {status}{giant}")
        return 0
    if args.check:
        errors: list[str] = []
        for row in rows:
            artifact = census.ROOT / row["artifact"]
            if not artifact.is_file():
                errors.append(f"MISSING: {row['artifact']} is stale (missing)")
                continue
            errors.extend(
                currentness.check_sidecar(artifact, include_hash_only=True)
            )
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"currentness sidecars are current for scope {args.scope}")
        return 0
    for row in rows:
        artifact = census.ROOT / row["artifact"]
        if not artifact.is_file():
            print(f"skip missing {row['artifact']}", file=sys.stderr)
            continue
        sidecar = census.ROOT / row["sidecar"]
        if args.rebind and sidecar.is_file():
            currentness.rebind_sidecar(artifact, mode="envelope")
        elif args.builder and sidecar.is_file():
            currentness.rebind_sidecar(
                artifact,
                mode="builder",
                prove_rebuild=args.prove_rebuild,
            )
        else:
            currentness.write_sidecar(artifact)
        print(f"wrote {row['sidecar']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
