#!/usr/bin/env python3
"""Write the four compact dedup-rule datapack resources."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import builder_cli
from tools import census_common as census
from tools.recipe_bulk import runtime as runtime_mod


def build() -> dict[str, dict]:
    rules = runtime_mod.dedup_rules()
    runtime_mod.validate_dedup_rules(rules)
    return {rule["rule_id"]: rule for rule in rules}


def _path_for(rule_id: str) -> Path:
    name = rule_id.split(":", 1)[1]
    return runtime_mod.datapack_dedup_root() / f"{name}.json"


def write() -> dict[str, dict]:
    documents = build()
    runtime_mod.datapack_dedup_root().mkdir(parents=True, exist_ok=True)
    for rule_id, document in documents.items():
        census.write_stable(_path_for(rule_id), document)
    return documents


def check() -> list[str]:
    errors: list[str] = []
    for rule_id, document in build().items():
        errors.extend(census.check_generated_document(_path_for(rule_id), document))
    return errors


def main(argv: list[str] | None = None) -> int:
    args = builder_cli.parse_managed(
        "Write compact dedup-rule datapack resources",
        argv,
    )
    if args.rebind_currentness_only:
        print("dedup rules do not use currentness sidecars")
        return 0
    try:
        if args.write:
            write()
            print("Wrote compact dedup-rule datapack resources")
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("compact dedup-rule datapack resources are current")
        return 0
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
