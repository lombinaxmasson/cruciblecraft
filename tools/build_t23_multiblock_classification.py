#!/usr/bin/env python3
"""Build the T23 multiblock behavior-classification artifact.

A1 card: classifies all 30 canonical multiblock kinds (from the T13
denominator, read-only) with five fact groups each and a
v1_required / post_1_0 / out_of_scope verdict per kind.  The verdict
must cite the classification vocabulary in the policy, never
implementation difficulty (T22 lesson).

Builder validation (fail-closed):
  - the kind id set is a bijection with the T13 denominator ids
  - raw_members match the T13 denominator per kind
  - every classification class is a vocabulary key
  - every basis cites its class id
  - non-v1 rows carry owner / replacement_condition / recheck_point
  - unclassified == 0
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t23_multiblock_behavior_classification.json"
POLICY = TOOLS / "t23_multiblock_policy.json"
BUILDER = Path(__file__).resolve()
T13_KINDS = TOOLS / "t13_denominators" / "multiblock_kinds.json"

DEFERRED_FIELDS = (
    "owner",
    "replacement_condition",
    "recheck_point",
)


def _load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def _load_if_exists(path: Path, default: Any = None) -> Any:
    if path.is_file():
        return json.loads(path.read_text(encoding="utf-8"))
    return default


def _stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def _sha256(path: Path) -> str:
    if not path.is_file():
        return hashlib.sha256(b"").hexdigest()
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def validate_policy(
    policy: dict[str, Any],
    t13: dict[str, Any],
) -> list[str]:
    errors: list[str] = []
    vocabulary = policy.get("classification_vocabulary") or {}
    kinds = policy.get("kinds") or []
    t13_rows = {
        row["canonical_id"]: row for row in t13.get("canonical_kinds") or []
    }

    if not vocabulary:
        errors.append("policy classification_vocabulary is missing")
    if len(kinds) != 30:
        errors.append(f"policy must classify 30 kinds, got {len(kinds)}")
    if set(t13_rows) != {row.get("canonical_id") for row in kinds}:
        errors.append(
            "policy kind ids are not a bijection with the T13 denominator"
        )
    for row in kinds:
        kind_id = row.get("canonical_id")
        t13_row = t13_rows.get(kind_id) or {}
        if row.get("source_identity", {}).get("raw_members") != t13_row.get(
            "raw_members"
        ):
            errors.append(
                f"{kind_id}: raw_members disagree with the T13 denominator"
            )
        classification = row.get("classification") or {}
        klass = classification.get("class")
        if klass not in vocabulary:
            errors.append(f"{kind_id}: class {klass!r} is not in the vocabulary")
            continue
        basis = classification.get("basis") or ""
        if klass not in basis:
            errors.append(f"{kind_id}: basis must cite the class id {klass}")
        if klass in ("post_1_0", "out_of_scope"):
            missing = [
                field
                for field in DEFERRED_FIELDS
                if not classification.get(field)
            ]
            if missing:
                errors.append(
                    f"{kind_id}: non-v1 row is missing {sorted(missing)}"
                )
        for group in (
            "source_identity",
            "schema_expressibility",
            "controller_behavior",
            "economy",
            "classification",
        ):
            if not row.get(group):
                errors.append(f"{kind_id}: fact group {group} is missing")
    selected = policy.get("selected") or []
    v1_ids = {
        row.get("canonical_id")
        for row in kinds
        if (row.get("classification") or {}).get("class") == "v1_required"
    }
    selected_ids = {row.get("canonical_id") for row in selected}
    if not selected_ids.issubset(v1_ids):
        errors.append(
            "selected kinds must be a subset of v1_required: "
            f"{sorted(selected_ids - v1_ids)}"
        )
    return errors


def build() -> dict[str, Any]:
    policy = _load_if_exists(POLICY) or {}
    t13 = _load_if_exists(T13_KINDS) or {}

    if not policy or not t13.get("canonical_kinds"):
        # G2 skeleton placeholder: no authored policy / denominator yet.
        return {
            "schema_version": 1,
            "status_owner": "build_t23_multiblock_classification",
            "universe": {
                "canonical_kinds": -1,
                "source": (
                    "tools/t13_denominators/multiblock_kinds.json (read-only)"
                ),
            },
            "classification_vocabulary": policy.get(
                "classification_vocabulary"
            )
            or [],
            "counts": {
                "canonical": -1,
                "classified": -1,
                "v1_required": -1,
                "post_1_0": -1,
                "out_of_scope": -1,
                "unclassified": -1,
            },
            "kinds": [],
            "selected": [],
            "currentness": {
                "owned_inputs": {
                    _relative(BUILDER): _sha256(BUILDER),
                    _relative(POLICY): _sha256(POLICY),
                },
                "t13_denominator": {
                    "path": _relative(T13_KINDS),
                    "sha256": _sha256(T13_KINDS),
                    "role": "read-only cross-check",
                },
            },
        }

    errors = validate_policy(policy, t13)
    if errors:
        raise ValueError("; ".join(errors))

    kinds = policy.get("kinds") or []
    counts = {
        "canonical": len(kinds),
        "classified": len(kinds),
        "v1_required": sum(
            1
            for row in kinds
            if (row.get("classification") or {}).get("class")
            == "v1_required"
        ),
        "post_1_0": sum(
            1
            for row in kinds
            if (row.get("classification") or {}).get("class") == "post_1_0"
        ),
        "out_of_scope": sum(
            1
            for row in kinds
            if (row.get("classification") or {}).get("class")
            == "out_of_scope"
        ),
        "unclassified": sum(
            1
            for row in kinds
            if (row.get("classification") or {}).get("class")
            not in ("v1_required", "post_1_0", "out_of_scope")
        ),
    }

    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "build_t23_multiblock_classification",
        "universe": {
            "canonical_kinds": len(kinds),
            "source": "tools/t13_denominators/multiblock_kinds.json (read-only)",
        },
        "classification_vocabulary": policy.get(
            "classification_vocabulary"
        ),
        "counts": counts,
        "kinds": kinds,
        "selected": policy.get("selected") or [],
        "unselected_v1_required": policy.get(
            "unselected_v1_required"
        )
        or [],
        "schema_policy": policy.get("schema_policy") or {},
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
                _relative(POLICY): _sha256(POLICY),
            },
            "t13_denominator": {
                "path": _relative(T13_KINDS),
                "sha256": _sha256(T13_KINDS),
                "role": "read-only cross-check",
            },
        },
    }
    if counts["unclassified"] == 0:
        document["status"] = "T23_MULTIBLOCK_CLASSIFICATION_READY"
    return document


def check() -> list[str]:
    """Return staleness / integrity errors (empty = clean)."""
    errors: list[str] = []
    if not OUTPUT.is_file():
        errors.append(f"missing generated file: {_relative(OUTPUT)}")
        return errors
    on_disk = _load_if_exists(OUTPUT)
    expected = build()
    if _stable(on_disk) != _stable(expected):
        errors.append(f"stale generated file: {_relative(OUTPUT)}")
    return errors


def write() -> dict[str, Any]:
    document = build()
    OUTPUT.write_text(_stable(document), encoding="utf-8", newline="\n")
    return document


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.check:
            errors = check()
            if errors:
                raise ValueError("; ".join(errors))
            document = _load_if_exists(OUTPUT)
        else:
            document = write()
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"T23 multiblock classification failed: {error}", file=sys.stderr)
        return 1
    summary = {"schema_version": document.get("schema_version")}
    if "status" in document:
        summary["status"] = document["status"]
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
