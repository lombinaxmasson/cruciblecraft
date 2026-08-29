#!/usr/bin/env python3
"""Shadow-compile T45→T37 and record zero-drift parity against production trees."""
from __future__ import annotations

import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import builder_cli
from tools import t35_common as t35
from tools.recipe_bulk import shadow as shadow_mod
from tools.recipe_bulk.write_guard import assert_ledger_write

OUTPUT = t35.TOOLS / "recipe_wave_shadow_parity.json"


def build(*, persist_ir: bool = False):
    return shadow_mod.compare_all(persist_ir=persist_ir)


def _write() -> list[str]:
    document = build(persist_ir=True)
    errors: list[str] = []
    if not document.get("ok"):
        errors.append("shadow parity is not zero-drift")
        for wave_id, row in (document.get("waves") or {}).items():
            if not row.get("ok"):
                errors.append(f"{wave_id}: {','.join(row.get('mismatches') or [])}")
        return errors
    assert_ledger_write(OUTPUT)
    t35.write_stable(OUTPUT, document)
    from tools import currentness

    if currentness.target_row(OUTPUT) is not None:
        currentness.write_sidecar(OUTPUT)
    return []


def _check() -> list[str]:
    document = build(persist_ir=False)
    errors: list[str] = []
    if not document.get("ok"):
        errors.append("shadow parity is not zero-drift")
        for wave_id, row in (document.get("waves") or {}).items():
            if not row.get("ok"):
                errors.append(f"{wave_id}: {','.join(row.get('mismatches') or [])}")
    errors.extend(t35.check_generated_document(OUTPUT, document))
    return errors


def main(argv: list[str] | None = None) -> int:
    args = builder_cli.parse_managed(
        "Shadow-compile T45 through T37 and prove production parity",
        argv,
    )
    if args.rebind_currentness_only:
        from tools import currentness

        currentness.rebind_sidecar(OUTPUT)
        print(f"rebound currentness sidecar for {t35.relative(OUTPUT)}")
        return 0
    if args.write:
        errors = _write()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"Wrote {t35.relative(OUTPUT)}")
        return 0
    errors = _check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"{t35.relative(OUTPUT)} is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
