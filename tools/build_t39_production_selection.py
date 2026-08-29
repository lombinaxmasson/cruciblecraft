#!/usr/bin/env python3
"""Record the T39 production withdrawal and dynamic player-path candidate.

The 157/250 catalog stays as an isolated router/load fixture. Production
publication still requires an immutable production lock; this diagnostic is
the family-atomic candidate that survives alias fail-close and source-fidelity
review.
"""
from __future__ import annotations

import json
import sys
from collections import defaultdict
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t39_common as t39  # noqa: E402
from tools.build_t39_centrifuge_recipes import (  # noqa: E402
    load_source,
    planned_documents,
    split_planned,
)
from tools.build_t39_player_path_support import (  # noqa: E402
    OUTPUT as SUPPORT_OUTPUT,
    T21,
    _row_inputs_satisfied,
)

OUTPUT = t39.PRODUCTION_SELECTION


def _player_path_from_generated() -> dict[str, Any]:
    _recipes, sidecars = split_planned(planned_documents("catalog"))
    return json.loads(sidecars[t39.PLAYER_PATH.name])


def _support_identities() -> set[str]:
    identities = set(t35.load_json(T21)["closure"]["reachable_identities"])
    if not SUPPORT_OUTPUT.is_file():
        return identities
    support = t35.load_json(SUPPORT_OUTPUT)
    for route in support.get("routes") or []:
        identities.update(route.get("output_identities") or [route["output_identity"]])
    return identities


def _min_support_projection() -> dict[str, Any]:
    if not SUPPORT_OUTPUT.is_file():
        return {"status": "ABSENT"}
    support = t35.load_json(SUPPORT_OUTPUT)
    closed = (support.get("discovery") or {}).get("family_atomic_closed") or {}
    families = int(closed.get("families") or 0)
    return {
        "accepted_routes": (support.get("discovery") or {}).get("accepted_routes", 0),
        "families": families,
        "if_candidate_remaining": t39.T38_REMAINING_ORDINARY_FAMILIES - families,
        "multi_families": closed.get("multi_families", 0),
        "multi_relations": closed.get("multi_relations", 0),
        "relations": closed.get("relations", 0),
        "singleton_families": closed.get("singleton_families", 0),
        "status": "REVIEWED",
        "template_keys": list(closed.get("template_keys") or []),
    }


def _family_rows(player_path: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for row in player_path["rows"]:
        grouped[row["template_key"]].append(row)
    return grouped


def _trusted_without_support(
    by_family: dict[str, list[dict[str, Any]]],
) -> list[dict[str, Any]]:
    trusted: list[dict[str, Any]] = []
    for template_key, rows in sorted(by_family.items()):
        if rows and all(row.get("inputs_reachable") for row in rows):
            trusted.append(
                {
                    "expanded_count": len(rows),
                    "publication_group": t39.publication_group_for_expanded_count(
                        len(rows)
                    ),
                    "template_key": template_key,
                }
            )
    return trusted


def _review_closed_families(
    by_family: dict[str, list[dict[str, Any]]],
    overlay: set[str],
) -> tuple[list[dict[str, Any]], list[dict[str, Any]]]:
    source_by_template: dict[str, list[dict[str, Any]]] = defaultdict(list)
    for relation in load_source()["relations"]:
        source_by_template[relation["template_key"]].append(relation)
    accepted: list[dict[str, Any]] = []
    rejected: list[dict[str, Any]] = []
    for template_key, rows in sorted(by_family.items()):
        blockers = list(t39.family_fidelity_blockers(source_by_template[template_key]))
        if any(row.get("alias_fail_closed") for row in rows):
            blockers.append("input_alias_fail_closed")
        closed = bool(rows) and all(
            _row_inputs_satisfied(row, overlay) for row in rows
        )
        if blockers:
            rejected.append(
                {
                    "blockers": blockers,
                    "closed_with_support": closed,
                    "template_key": template_key,
                }
            )
            continue
        if closed:
            accepted.append(
                {
                    "expanded_count": len(rows),
                    "family_id": t39.canonical_family_id(template_key),
                    "publication_group": t39.publication_group_for_expanded_count(
                        len(rows)
                    ),
                    "template_key": template_key,
                }
            )
    return accepted, rejected


def build() -> dict[str, Any]:
    player_path = _player_path_from_generated()
    by_family = _family_rows(player_path)
    trusted_families = _trusted_without_support(by_family)
    trusted_singleton = [
        row for row in trusted_families if row["expanded_count"] == 1
    ]
    trusted_multi = [row for row in trusted_families if row["expanded_count"] > 1]
    candidate, rejected = _review_closed_families(by_family, _support_identities())
    singleton = [row for row in candidate if row["expanded_count"] == 1]
    multi = [row for row in candidate if row["expanded_count"] > 1]
    candidate_relations = sum(row["expanded_count"] for row in candidate)
    candidate_ids = [row["family_id"] for row in candidate]
    status = (
        "T39_PRODUCTION_CANDIDATE"
        if candidate
        else "T39_PRODUCTION_CANDIDATE_EMPTY"
    )
    return {
        "schema_version": 1,
        "status": status,
        "source_revision": t39.SOURCE_REVISION,
        "withdrawn_catalog": {
            "family_count": t39.FAMILY_COUNT,
            "reason": (
                "Bounded support cannot close the 157-family catalog after "
                "T21 compact baseline and unproven alias fail-close. Catalog "
                "stays as an isolated router/load fixture."
            ),
            "relations": t39.SOURCE_ROWS,
            "selection_sha256": t39.EXPECTED_SELECTION_SHA256,
        },
        "t21_scan": {
            "excludes_t39_generated_and_support": True,
            "includes_t37_t38_compact": True,
            "parses_compact_gt_recipe_family": True,
        },
        "player_path": {
            "alias_fail_closed_relations": player_path.get(
                "alias_fail_closed_relations", 0
            ),
            "inputs_reachable": player_path["inputs_reachable"],
            "relations": player_path["relations"],
            "relations_with_unreachable_inputs": player_path[
                "relations_with_unreachable_inputs"
            ],
        },
        "min_support_projection": _min_support_projection(),
        "family_atomic_trusted": {
            "baseline": "t21_without_t39_or_support",
            "families": len(trusted_families),
            "includes_t39_support": False,
            "multi_families": len(trusted_multi),
            "multi_relations": sum(row["expanded_count"] for row in trusted_multi),
            "relations": sum(row["expanded_count"] for row in trusted_families),
            "singleton_families": len(trusted_singleton),
            "template_keys": [row["template_key"] for row in trusted_families],
        },
        "review": {
            "rejected_families": len(rejected),
            "rejected": rejected,
        },
        "candidate": {
            "families": len(candidate),
            "family_ids": candidate_ids,
            "multi_families": len(multi),
            "multi_relations": sum(row["expanded_count"] for row in multi),
            "player_path": {
                "inputs_reachable": candidate_relations,
                "outputs_registered": candidate_relations,
                "relations": candidate_relations,
            },
            "publication_groups": sorted({
                row["publication_group"] for row in candidate
            }),
            "relations": candidate_relations,
            "selection_sha256": t39.selection_sha256(candidate_ids),
            "singleton_families": len(singleton),
            "template_keys": [row["template_key"] for row in candidate],
        },
        "gap_projection": {
            "if_candidate_remaining": (
                t39.T38_REMAINING_ORDINARY_FAMILIES - len(candidate)
            ),
            "if_trusted_subset_candidate": (
                t39.T38_REMAINING_ORDINARY_FAMILIES - len(trusted_families)
            ),
            "opening": t39.T38_REMAINING_ORDINARY_FAMILIES,
            "withdrawn_catalog_closing": t39.WITHDRAWN_CATALOG_CLOSING_REMAINING,
        },
        "note": (
            "157/250 remains the withdrawn load fixture. The reviewed "
            "family-atomic subset is only a candidate until "
            "tools/t39_production_lock.json freezes it. Gap stays 5639 until "
            "census closes locked identities."
        ),
    }


def check() -> list[str]:
    return t39.check_document(OUTPUT, build())


def write() -> dict[str, Any]:
    document = build()
    t35.write_stable(OUTPUT, document)
    return document


def main(argv: list[str] | None = None) -> int:
    args = t39.parse_write_check("Record T39 production candidate.", argv)
    if args.check:
        errors = check()
        if errors:
            print("T39 production selection is not current:")
            for error in errors:
                print(f"- {error}")
            return 1
        print("T39 production selection is current.")
        return 0
    write()
    print(f"Wrote {t39.relative(OUTPUT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
