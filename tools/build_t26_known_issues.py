#!/usr/bin/env python3
"""Build the T26 known-issue and freeze ledger.

T26 close consumes:
  - T25 finding dispositions (bijection preserved; F003/F005 owner and
    recheck_point remapped to T27 RC)
  - playtest rows authored in t26_known_issues_policy.json (4.5 P0-P9)
  - freeze rows for O-15, anvil_bend_* and crucible, validated against
    the T26 localization ledger

Every issue must carry severity, workaround, owner and release
disposition. No playtest or inherited finding may block Beta.
``status`` is T26_KNOWN_ISSUES_COMPLETE only when the ledger is a
complete bijection with the authored inputs; otherwise the key is
null (fail-closed, never "").
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from pathlib import Path
from typing import Any

ISSUE_VOCABULARY = ("post_beta_polish", "non_blocking")
SEVERITY_VOCABULARY = ("info", "low", "medium", "high", "blocker")
REQUIRED_ISSUE_FIELDS = (
    "id",
    "title",
    "severity",
    "workaround",
    "owner",
    "disposition",
    "blocks_beta",
    "evidence_artifact",
)
PLAYTEST_IDS = tuple(f"CC-4.5-P{index}" for index in range(10))
T25_IDS = ("T24-F001", "T24-F002", "T24-F003", "T24-F004", "T24-F005")
REMAPPED_RECHECK = ("T24-F003", "T24-F005")
ANVIL_BEND_IDS = (
    "cruciblecraft:anvil_bend_big",
    "cruciblecraft:anvil_bend_small",
)
ZERO_GAP_DOMAINS = (
    "config",
    "container",
    "disconnect",
    "item_group",
    "screen",
)


def _resolve_root() -> Path:
    return Path(__file__).resolve().parents[1]


ROOT = _resolve_root()
TOOLS = ROOT / "tools"
OUTPUT = TOOLS / "t26_known_issues.json"
POLICY = TOOLS / "t26_known_issues_policy.json"
T25_DISPOSITIONS = TOOLS / "t25_findings_disposition.json"
LOCALIZATION = TOOLS / "t26_localization_ledger.json"
BUILDER = Path(__file__).resolve()


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


def _require_issue_fields(row: dict[str, Any], *, source: str) -> None:
    missing = [field for field in REQUIRED_ISSUE_FIELDS if field not in row]
    if missing:
        raise ValueError(
            f"{source} issue {row.get('id')!r} is missing fields: {missing}"
        )
    if row["disposition"] not in ISSUE_VOCABULARY:
        raise ValueError(
            f"{source} issue {row['id']!r} has illegal disposition "
            f"{row['disposition']!r}"
        )
    if row["severity"] not in SEVERITY_VOCABULARY:
        raise ValueError(
            f"{source} issue {row['id']!r} has illegal severity "
            f"{row['severity']!r}"
        )
    if not row["workaround"] or not row["owner"] or not row["title"]:
        raise ValueError(
            f"{source} issue {row['id']!r} is missing title, workaround "
            "or owner"
        )
    if row["blocks_beta"] is True:
        raise ValueError(
            f"{source} issue {row['id']!r} blocks Beta; T26 freeze "
            "forbids promoting 4.5 or T25 findings to hard blockers"
        )


def _playtest_issues(policy: dict[str, Any]) -> list[dict[str, Any]]:
    rows = list(policy.get("playtest_issues") or [])
    ids = [row.get("id") for row in rows]
    if ids != list(PLAYTEST_IDS):
        raise ValueError(
            "playtest issues must be exactly CC-4.5-P0 through P9 in order; "
            f"got {ids}"
        )
    for row in rows:
        _require_issue_fields(row, source="playtest")
        if row["owner"] != "4.5" or row["disposition"] != "post_beta_polish":
            raise ValueError(
                f"{row['id']}: playtest rows must have owner=4.5 and "
                "disposition=post_beta_polish"
            )
    return rows


def _t25_issues(
    policy: dict[str, Any],
    dispositions: dict[str, Any],
) -> list[dict[str, Any]]:
    source_rows = dispositions.get("dispositions") or []
    source_ids = [row.get("id") for row in source_rows]
    if source_ids != list(T25_IDS):
        raise ValueError(
            "T25 disposition bijection failed: expected "
            f"{list(T25_IDS)}, got {source_ids}"
        )
    remap = ((policy.get("t25_carry_forward") or {}).get("remap")) or {}
    rows: list[dict[str, Any]] = []
    for source in source_rows:
        finding_id = source["id"]
        row = {
            "id": finding_id,
            "title": source["title"],
            "severity": "info" if finding_id == "T24-F005" else "low",
            "workaround": (
                "None required for correctness. Recheck on a declared "
                ">=16 GB environment before RC."
                if finding_id in REMAPPED_RECHECK
                else "None required; verified bound, no Beta action."
            ),
            "owner": source["owner"],
            "disposition": "non_blocking",
            "blocks_beta": source.get("blocks_beta") is True,
            "evidence_artifact": source["evidence_artifact"],
            "inherited_from": "tools/t25_findings_disposition.json",
        }
        contract = dict(source.get("recheck_contract") or {})
        override = remap.get(finding_id)
        if override:
            row["owner"] = override["owner"]
            contract["recheck_point"] = override["recheck_point"]
            if source.get("recheck_contract", {}).get("replacement_condition"):
                contract["replacement_condition"] = source["recheck_contract"][
                    "replacement_condition"
                ]
        if contract:
            row["recheck_contract"] = contract
        _require_issue_fields(row, source="t25")
        rows.append(row)
    for finding_id in REMAPPED_RECHECK:
        contract = next(
            row["recheck_contract"]
            for row in rows
            if row["id"] == finding_id
        )
        if (
            contract.get("recheck_point") != "T27 RC candidate"
            or not contract.get("replacement_condition")
        ):
            raise ValueError(
                f"{finding_id} must transfer its SKIP contract to "
                "T27 RC candidate"
            )
    return rows


def _validate_o15(freeze: dict[str, Any], localization: dict[str, Any]) -> None:
    o15 = freeze.get("o15") or {}
    if o15.get("item") != "O-15" or o15.get("disposition") != "closed":
        raise ValueError("O-15 freeze must close the v1_required case")
    if localization.get("status") != "T26_LOCALIZATION_ACCOUNTED":
        raise ValueError("O-15 cannot close: localization ledger is not accounted")
    domains = localization.get("domains") or {}
    missing = [
        name
        for name in ZERO_GAP_DOMAINS
        if not (domains.get(name) or {}).get("zero_gap")
    ]
    if missing:
        raise ValueError(
            f"O-15 cannot close: v1 critical domains still have zh gap: {missing}"
        )
    material = (localization.get("o15_reclassification") or {}).get(
        "material_domain"
    ) or {}
    if (
        material.get("critical_path_translated") != 208
        or material.get("long_tail_count") != 1566
        or material.get("long_tail_disposition") != "post_1_0"
        or material.get("no_english_copy_fake_translations") is not True
    ):
        raise ValueError(
            "O-15 cannot close: material-domain accounting does not match "
            "208 translated + 1566 post_1_0 with no English-copy fakes"
        )


def _validate_freeze(freeze: dict[str, Any], localization: dict[str, Any]) -> dict[str, Any]:
    _validate_o15(freeze, localization)
    bends = freeze.get("anvil_bend") or []
    bend_ids = [row.get("id") for row in bends]
    if bend_ids != list(ANVIL_BEND_IDS):
        raise ValueError(
            f"anvil_bend freeze must be exactly {list(ANVIL_BEND_IDS)}"
        )
    for row in bends:
        if row.get("disposition") != "post_1_0":
            raise ValueError(f"{row.get('id')} must freeze as post_1_0")
        if not row.get("v1_replacement"):
            raise ValueError(f"{row.get('id')} needs a v1 replacement path")
    crucible = freeze.get("crucible") or {}
    if (
        crucible.get("canonical_id") != "crucible"
        or crucible.get("disposition") != "v1_required"
        or crucible.get("owner") != "T27"
        or crucible.get("implementation") != "none"
        or not crucible.get("naming_constraint")
    ):
        raise ValueError(
            "crucible freeze must remain v1_required with owner T27, "
            "implementation none, and a naming constraint that avoids "
            "cruciblecraft:crucible"
        )
    return freeze


def build() -> dict[str, Any]:
    policy = _load_if_exists(POLICY) or {}
    dispositions = _load_if_exists(T25_DISPOSITIONS) or {}
    localization = _load_if_exists(LOCALIZATION) or {}
    if not policy or not dispositions:
        raise ValueError("T26 known-issue policy or T25 dispositions are missing")

    playtest = _playtest_issues(policy)
    inherited = _t25_issues(policy, dispositions)
    freeze = _validate_freeze(policy.get("freeze") or {}, localization)
    issues = playtest + inherited
    ids = [row["id"] for row in issues]
    if len(ids) != len(set(ids)):
        raise ValueError(f"duplicate known-issue ids: {ids}")

    counts = {
        "total": len(issues),
        "playtest": len(playtest),
        "inherited_t25": len(inherited),
        "post_beta_polish": sum(
            1 for row in issues if row["disposition"] == "post_beta_polish"
        ),
        "non_blocking": sum(
            1 for row in issues if row["disposition"] == "non_blocking"
        ),
        "blocks_beta": sum(1 for row in issues if row["blocks_beta"] is True),
    }
    complete = (
        counts["total"] == 15
        and counts["playtest"] == 10
        and counts["inherited_t25"] == 5
        and counts["blocks_beta"] == 0
        and freeze["o15"]["disposition"] == "closed"
    )
    document: dict[str, Any] = {
        "schema_version": 1,
        "status_owner": "build_t26_known_issues",
        "status": "T26_KNOWN_ISSUES_COMPLETE" if complete else None,
        "policy": (
            "Playtest issues reference CrucibleCraft-4.5-体验精修规划.md "
            "and stay post_beta_polish with owner 4.5. T25 findings stay "
            "non_blocking; F003/F005 recheck transfers to T27 RC. O-15 "
            "closes on the localization ledger. anvil_bend_* freeze as "
            "post_1_0 reserved slots. crucible stays v1_required with "
            "owner T27."
        ),
        "issues": issues,
        "freeze": freeze,
        "counts": counts,
        "currentness": {
            "owned_inputs": {
                _relative(BUILDER): _sha256(BUILDER),
                _relative(POLICY): _sha256(POLICY),
                _relative(T25_DISPOSITIONS): _sha256(T25_DISPOSITIONS),
                _relative(LOCALIZATION): _sha256(LOCALIZATION),
            }
        },
    }
    return document


def check() -> list[str]:
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
        print(f"T26 known issues failed: {error}", file=sys.stderr)
        return 1
    summary = {"schema_version": document.get("schema_version")}
    if document.get("status"):
        summary["status"] = document["status"]
    print(json.dumps(summary, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
