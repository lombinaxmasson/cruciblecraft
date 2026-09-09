#!/usr/bin/python3
"""Fresh recipes verification: import-source, isolated compile, no receipt PASS."""
from __future__ import annotations

import argparse
import json
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import machine_delivery
from tools.recipe_bulk import source_import
from tools.recipe_bulk.pilot import compile_fixture

SMELTER = (
    ROOT
    / "src/test/resources/generic_recipe_import/smelter_exact_singleton/recipe_import.json"
)
MIXER = (
    ROOT
    / "src/test/resources/generic_recipe_import/mixer_exact_multi/recipe_import.json"
)
BUILD_GRADLE = ROOT / "build.gradle"
RECEIPT_NAMES = (
    "gametest_receipt.json",
    "readiness.json",
    "closeout_seal.json",
)


def _import_errors(path: Path) -> list[str]:
    return source_import.check_import(path)


def check() -> list[str]:
    errors: list[str] = []
    errors.extend(machine_delivery.check())
    if not SMELTER.is_file() or not MIXER.is_file():
        errors.append("generic-import fixtures are missing")
        return errors
    errors.extend(_import_errors(SMELTER))
    errors.extend(_import_errors(MIXER))
    gradle = BUILD_GRADLE.read_text(encoding="utf-8")
    if "waveRecipes" not in gradle:
        errors.append("build.gradle is missing the -PwaveRecipes GameTest gate")
    if "neoforge.enabledGameTestNamespaces" not in gradle:
        errors.append("build.gradle is missing GameTest namespace isolation")
    with tempfile.TemporaryDirectory(prefix="recipe-fresh-") as tmp:
        root = Path(tmp)
        try:
            small = compile_fixture(SMELTER, root / "small")
            large = compile_fixture(MIXER, root / "large")
        except (ValueError, KeyError, OSError) as error:
            errors.append(str(error))
            return errors
        if small["family_count"] != 1 or small["relation_count"] != 1:
            errors.append(
                "small exact pilot drifted: "
                f"{small['family_count']} families / {small['relation_count']} relations"
            )
        if large["family_count"] != 1 or large["relation_count"] < 2:
            errors.append(
                "large exact_multi pilot drifted: "
                f"{large['family_count']} families / {large['relation_count']} relations"
            )
        if small["representations"].get("exact") != 1:
            errors.append("small pilot must stay exact")
        if large["representations"].get("exact_multi") != 1:
            errors.append("large pilot must stay exact_multi")
        if small["parameterized"] or large["parameterized"]:
            errors.append("parameterized families are forbidden without runtime proof")
        if small["overflow"] or large["overflow"]:
            errors.append("isolated pilots must not silently overflow")
        for name in RECEIPT_NAMES:
            if (root / name).is_file():
                errors.append(f"fresh compile must not treat {name} as PASS")
        metrics_path = ROOT / "build" / "verification" / "recipe_fresh_metrics.json"
        metrics_path.parent.mkdir(parents=True, exist_ok=True)
        metrics_path.write_text(
            json.dumps(
                {
                    "large": large,
                    "overflow": 0,
                    "parameterized": 0,
                    "receipts_are_not_pass": True,
                    "reload_sync_retained_transient": "not_executed",
                    "small": small,
                    "status": "FRESH",
                },
                indent=2,
                sort_keys=True,
            )
            + "\n",
            encoding="utf-8",
        )
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Fresh recipes verification")
    parser.add_argument("--check", action="store_true", required=True)
    parser.parse_args(argv)
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("fresh recipes profile passed isolated small/large compile")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
