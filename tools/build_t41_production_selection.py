#!/usr/bin/env python3
"""Record the T41 292/292 production candidate after source overlay."""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import t35_common as t35  # noqa: E402
from tools import t41_common as common  # noqa: E402

OUTPUT = common.CANDIDATE_SELECTION


def build() -> dict[str, Any]:
    work = t35.load_json(common.WORK_SET)
    source = t35.load_json(common.SOURCE)
    accepted = []
    rejected = []
    by_template = {}
    for relation in source.get("relations") or []:
        by_template.setdefault(relation["template_key"], []).append(relation)
    for row in work.get("families") or []:
        template_key = str(row["template_key"])
        family_id = str(row["family_id"])
        relations = by_template.get(template_key) or []
        if template_key in common.COMBINATORIAL_TEMPLATE_KEYS:
            rejected.append({
                "blockers": ["combinatorial_player_path_unproven"],
                "family_id": family_id,
                "template_key": template_key,
            })
            continue
        blockers = []
        if len(relations) != 1:
            blockers.append("not_singleton")
        for relation in relations:
            blockers.extend(common.family_fidelity_blockers([relation]))
            for operand, count, action in zip(
                relation.get("item_inputs") or [],
                relation.get("item_input_counts") or [],
                relation.get("item_input_actions") or [],
            ):
                kind = str((action or {}).get("kind") or "").lower()
                if kind == "consume" and int(count or 0) > 0:
                    try:
                        common.assert_runtime_id(operand.get("runtime_id"), consume=True)
                    except ValueError as exc:
                        blockers.append(str(exc))
        if blockers:
            rejected.append({
                "blockers": sorted(set(blockers)),
                "family_id": family_id,
                "template_key": template_key,
            })
            continue
        accepted.append({
            "expanded_count": 1,
            "family_id": family_id,
            "publication_group": relations[0].get("publication_group"),
            "template_key": template_key,
        })
    family_ids = [row["family_id"] for row in accepted]
    digest = common.selection_sha256(family_ids)
    if len(accepted) != common.PRODUCTION_FAMILY_COUNT:
        raise ValueError(
            "T41 candidate is not 292/292; blockers="
            + str(rejected[:10])
        )
    if digest != common.EXPECTED_PRODUCTION_SELECTION_SHA256:
        raise ValueError("T41 candidate selection_sha256 drifted")
    return {
        "accepted": accepted,
        "accepted_count": len(accepted),
        "generated_by": "python tools/build_t41_production_selection.py",
        "rejected": rejected,
        "rejected_count": len(rejected),
        "schema_version": 1,
        "selection_sha256": digest,
        "status": "T41_PRODUCTION_CANDIDATE",
    }


def main(argv: list[str] | None = None) -> int:
    args = common.parse_managed("Record the T41 production candidate", argv)
    if common.handle_rebind(args, OUTPUT):
        return 0
    try:
        document = build()
        if args.check:
            errors = common.check_document(OUTPUT, document)
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("T41 production candidate is current.")
            return 0
        t35.write_stable(OUTPUT, document)
        print(f"Wrote T41 candidate ({document['accepted_count']}/292).")
        return 0
    except (OSError, ValueError, KeyError) as error:
        print(f"T41 production candidate failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
