#!/usr/bin/env python3
"""Write assembler/compact–assembler/wood compact publication-policy datapack resources from the runtime manifest."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import builder_cli
from tools import census_common as census
from tools.recipe_bulk import runtime as runtime_mod

SCHEMA = (
    ROOT
    / "src/main/resources/data/cruciblecraft/schema/compact_publication_policy.schema.json"
)


def _validate(document: dict) -> None:
    required = set((census.load_json(SCHEMA).get("required") or []))
    missing = sorted(required - set(document))
    if missing:
        raise ValueError(f"publication policy missing required fields: {missing}")


def build() -> dict[str, dict]:
    documents = runtime_mod.cutover_policy_documents()
    for document in documents.values():
        _validate(document)
    return documents


def write() -> dict[str, dict]:
    documents = build()
    for group_id, document in documents.items():
        spec = runtime_mod.spec_for_group(group_id)
        path = runtime_mod.datapack_policy_path(spec)
        path.parent.mkdir(parents=True, exist_ok=True)
        census.write_stable(path, document)
    return documents


def check() -> list[str]:
    errors: list[str] = []
    documents = build()
    for group_id, document in documents.items():
        spec = runtime_mod.spec_for_group(group_id)
        path = runtime_mod.datapack_policy_path(spec)
        errors.extend(census.check_generated_document(path, document))
    return errors


def main(argv: list[str] | None = None) -> int:
    args = builder_cli.parse_managed(
        "Write assembler/compact–assembler/wood datapack publication policies",
        argv,
    )
    if args.rebind_currentness_only:
        print("publication policies do not use currentness sidecars")
        return 0
    try:
        if args.write:
            write()
            print("Wrote assembler/compact–assembler/wood datapack publication policies")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("assembler/compact–assembler/wood datapack publication policies are current")
        return 0
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
