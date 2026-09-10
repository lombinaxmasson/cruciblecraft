#!/usr/bin/env python3
"""Atomically close the unique-active capability and rebuild projections."""
from __future__ import annotations

import argparse
import json
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import atomic_io
from tools import capability_ledger
from tools import io_common as io
from tools import player_complete
from tools import project_status

PATH_MAP = ROOT / "docs" / "history" / "path-map.json"
WAVES = ROOT / "tools" / "waves"
BLOCKED_LEDGER = ROOT / "tools" / "blocked_recipe_ledger.json"


def _load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def _write_json(path: Path, document: dict[str, Any], indent: int = 2) -> None:
    atomic_io.write_text(
        path,
        json.dumps(document, indent=indent, ensure_ascii=False) + "\n",
    )


def _owned_wave_files(capability: dict[str, Any], filename: str) -> list[Path]:
    owned = list(capability["owned_paths"])
    matches: list[Path] = []
    if not WAVES.is_dir():
        return matches
    for path in WAVES.rglob(filename):
        rel = io.relative(path)
        if capability_ledger.match_owned(owned, rel):
            matches.append(path)
    return matches


def _lock_claims_player_complete(note: str) -> bool:
    if "not player_complete" in note:
        return False
    return "player_complete" in note


def _move_plan(source: Path, destination: Path) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    completed = subprocess.run(
        ["git", "mv", "--", str(source), str(destination)],
        cwd=ROOT,
        capture_output=True,
    )
    if completed.returncode == 0:
        return
    if not source.is_file():
        raise ValueError(f"missing active plan {io.relative(source)}")
    shutil.move(str(source), str(destination))


def _append_path_map(plan_name: str) -> None:
    document = _load_json(PATH_MAP)
    moves = document.setdefault("moves", {})
    closed = f"docs/history/card-plans/closed/{plan_name}"
    changed = False
    for lane in ("active", "prep"):
        source = f"docs/history/card-plans/{lane}/{plan_name}"
        if moves.get(source) != closed:
            moves[source] = closed
            changed = True
    if changed:
        _write_json(PATH_MAP, document, indent=4)


def _null_unique_active_wave(path: Path) -> None:
    document = _load_json(path)
    if "unique_active_wave" not in document:
        return
    if document["unique_active_wave"] is None:
        return
    document["unique_active_wave"] = None
    _write_json(path, document)


def _rebuild_projections() -> list[str]:
    encoded = capability_ledger.dumps(capability_ledger.compile_ledger())
    atomic_io.write_bytes(capability_ledger.LEDGER, encoded)
    project_status.write_status()
    errors: list[str] = []
    if capability_ledger.LEDGER.read_bytes() != encoded:
        errors.append("capability ledger is stale after close")
    errors.extend(project_status.check_status())
    return errors


def close_capability(
    slug: str,
    *,
    verify: bool = False,
    maturity: str = "player_complete",
) -> list[str]:
    if maturity not in ("player_complete", "runtime_ready"):
        raise ValueError(f"unsupported close maturity {maturity!r}")
    compiled = capability_ledger.compile_ledger()
    unique = compiled.get("unique_active_slug")
    if unique != slug:
        raise ValueError(
            f"{slug}: unique-active is {unique!r}; close the active capability"
        )
    path = capability_ledger.CAP_ROOT / slug / "capability.json"
    capability = capability_ledger.load_capability(path)
    errors: list[str] = []
    if maturity == "player_complete":
        errors.extend(player_complete.check_declared_test_ids(capability))
        signoff = player_complete.load_signoff(capability)
        errors.extend(player_complete.check_signoff(capability, signoff))
        items = list(signoff.get("craftable_items") or [])
        errors.extend(player_complete.check_static_player_surface(slug, items))
        errors.extend(player_complete.check_surface_catalog(slug, items))
    else:
        if "player-complete" in list(capability.get("profiles") or []):
            errors.append(
                f"{slug}: runtime_ready close cannot keep a player-complete profile"
            )
        if capability.get("player_signoff"):
            errors.append(
                f"{slug}: runtime_ready close cannot keep player_signoff"
            )
    for lock_path in _owned_wave_files(capability, "production_lock.json"):
        note = str(_load_json(lock_path).get("note") or "")
        if _lock_claims_player_complete(note):
            errors.append(
                f"{io.relative(lock_path)}: production_lock.note claims "
                "player_complete"
            )
    if errors:
        return errors
    document = _load_json(path)
    document["maturity"] = maturity
    document["workflow"] = "accepted"
    _write_json(path, document)
    for filename in ("topology.json", "readiness.json"):
        for wave_path in _owned_wave_files(capability, filename):
            _null_unique_active_wave(wave_path)
    owned = list(capability["owned_paths"])
    if capability_ledger.match_owned(owned, io.relative(BLOCKED_LEDGER)):
        _null_unique_active_wave(BLOCKED_LEDGER)
    plans = capability_ledger.load_card_plan_index()
    active_plan = plans["active"].get(slug)
    if active_plan is None:
        raise ValueError(f"{slug}: missing docs/history/card-plans/active/ plan")
    closed_plan = capability_ledger.CARD_PLANS / "closed" / active_plan.name
    _move_plan(active_plan, closed_plan)
    _append_path_map(active_plan.name)
    errors = _rebuild_projections()
    if verify:
        if maturity != "player_complete":
            errors.append(
                f"{slug}: --verify is only for player_complete close"
            )
        else:
            errors.extend(player_complete.run_fresh_capability(slug, client=True))
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--capability", required=True)
    parser.add_argument(
        "--maturity",
        default="player_complete",
        choices=("player_complete", "runtime_ready"),
        help="close maturity; runtime_ready skips player signoff and craftability",
    )
    parser.add_argument(
        "--verify",
        action="store_true",
        help="also run a fresh GameTest plus runClient after the close writes",
    )
    args = parser.parse_args(argv)
    try:
        errors = close_capability(
            args.capability,
            verify=args.verify,
            maturity=args.maturity,
        )
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 1
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print(f"closed {args.capability}")
    print(
        "Confirm production lock, identity reasons, non-scope, "
        "and player signoff before committing. Do not commit receipts."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
