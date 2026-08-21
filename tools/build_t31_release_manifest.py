#!/usr/bin/env python3
"""Build T31 release package manifest. Hashes are derived from files."""
from __future__ import annotations

import argparse
import json
import sys
import zipfile
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t27_common as common  # noqa: E402
from tools import t31_common as t31  # noqa: E402

OUTPUT = t31.TOOLS / "t31_release_manifest.json"
BUILDER = Path(__file__).resolve()
CHANGELOG = ROOT / "CHANGELOG.md"
README = ROOT / "README.md"
GUIDE = ROOT / "docs" / "CrucibleCraft-玩家指南.md"
TOML = ROOT / "src" / "main" / "templates" / "META-INF" / "neoforge.mods.toml"
LIBS = ROOT / "build" / "libs"
DISTS = ROOT / "build" / "distributions"
REPORT_OWNED = ("currentness",)
FORBIDDEN_FRAGMENTS = (
    "4.5Fix",
    ".env",
    "secret",
    "run-t31",
    "debug.log",
    "latest.log",
    "t31_compat_samples",
    "0.1.0-beta.1",
    ".cache",
)


def _gradle_property(name: str) -> str | None:
    for line in t31.GRADLE.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if stripped.startswith(f"{name}="):
            return stripped.split("=", 1)[1].strip()
    return None


def _package(kind: str, path: Path) -> dict[str, Any]:
    if not path.is_file():
        return {
            "bytes": None,
            "kind": kind,
            "path": common.relative(path) if path.is_absolute() else str(path),
            "sha256": None,
            "status": "pending",
            "zero_filled": False,
        }
    return {
        "bytes": path.stat().st_size,
        "kind": kind,
        "path": common.relative(path),
        "sha256": common.sha256_file(path),
        "status": "measured",
        "zero_filled": False,
    }


def _inspect_jar(path: Path, version: str) -> dict[str, Any]:
    if not path.is_file():
        return {
            "embedded_version": None,
            "has_assets": False,
            "has_data": False,
            "has_hopper_resources": False,
            "has_mixins": False,
            "has_mods_toml": False,
            "has_test_classes": None,
            "forbidden_hits": [],
            "status": "pending",
            "zero_filled": False,
        }
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
        toml = ""
        if "META-INF/neoforge.mods.toml" in names:
            toml = archive.read("META-INF/neoforge.mods.toml").decode(
                "utf-8", errors="replace"
            )
    joined = "\n".join(names)
    forbidden = [
        fragment
        for fragment in FORBIDDEN_FRAGMENTS
        if fragment in joined or (fragment == "0.1.0-beta.1" and fragment in toml)
    ]
    has_test_classes = any(
        name.endswith("Test.class") and "/test/" in name.replace("\\", "/")
        for name in names
    )
    embedded = None
    for line in toml.splitlines():
        stripped = line.strip()
        if stripped.startswith("version="):
            embedded = stripped.split("=", 1)[1].strip().strip('"')
            break
    ok = (
        embedded == version
        and "assets/cruciblecraft/" in joined
        and "data/cruciblecraft/" in joined
        and "cruciblecraft.mixins.json" in names
        and "META-INF/neoforge.mods.toml" in names
        and ("hopper" in joined or "steel_dust_funnel" in joined)
        and not has_test_classes
        and not forbidden
    )
    return {
        "embedded_version": embedded,
        "has_assets": "assets/cruciblecraft/" in joined,
        "has_data": "data/cruciblecraft/" in joined,
        "has_hopper_resources": "hopper" in joined or "steel_dust_funnel" in joined,
        "has_mixins": "cruciblecraft.mixins.json" in names,
        "has_mods_toml": "META-INF/neoforge.mods.toml" in names,
        "has_test_classes": has_test_classes,
        "forbidden_hits": forbidden,
        "status": "measured" if ok else "failed",
        "zero_filled": False,
    }


def _inspect_zip(path: Path, version: str) -> dict[str, Any]:
    if not path.is_file():
        return {
            "contains_rc1_jar": False,
            "contains_beta_jar": None,
            "has_changelog": False,
            "has_credits": False,
            "has_docs": False,
            "has_license": False,
            "status": "pending",
            "zero_filled": False,
        }
    with zipfile.ZipFile(path) as archive:
        names = archive.namelist()
    joined = "\n".join(names)
    jar_name = f"cruciblecraft-{version}.jar"
    beta_jar = "cruciblecraft-0.1.0-beta.1.jar"
    ok = (
        any(name.endswith(jar_name) or name == jar_name for name in names)
        and beta_jar not in joined
        and any(name == "CHANGELOG.md" or name.endswith("/CHANGELOG.md") for name in names)
        and any(name == "CREDITS.md" or name.endswith("/CREDITS.md") for name in names)
        and any("docs/" in name.replace("\\", "/") for name in names)
        and any(
            name == "TEMPLATE_LICENSE.txt" or name.endswith("/TEMPLATE_LICENSE.txt")
            for name in names
        )
    )
    return {
        "contains_rc1_jar": any(
            name.endswith(jar_name) or name == jar_name for name in names
        ),
        "contains_beta_jar": beta_jar in joined,
        "has_changelog": any(
            name == "CHANGELOG.md" or name.endswith("/CHANGELOG.md") for name in names
        ),
        "has_credits": any(
            name == "CREDITS.md" or name.endswith("/CREDITS.md") for name in names
        ),
        "has_docs": any("docs/" in name.replace("\\", "/") for name in names),
        "has_license": any(
            name == "TEMPLATE_LICENSE.txt" or name.endswith("/TEMPLATE_LICENSE.txt")
            for name in names
        ),
        "status": "measured" if ok else "failed",
        "zero_filled": False,
    }


def build() -> dict[str, Any]:
    version = t31.gradle_mod_version()
    jar = LIBS / f"cruciblecraft-{version}.jar"
    archive = DISTS / f"cruciblecraft-{version}.zip"
    packages = {
        "jar": _package("jar", jar),
        "zip": _package("zip", archive),
    }
    content_checks = {
        "jar": _inspect_jar(jar, version),
        "zip": _inspect_zip(archive, version),
    }
    docs = {
        "changelog_present": CHANGELOG.is_file(),
        "readme_present": README.is_file(),
        "player_guide_present": GUIDE.is_file(),
        "mods_toml_present": TOML.is_file(),
        "version_is_rc1": version == t31.TARGET_VERSION,
        "version_still_beta": version == t31.OPENING_VERSION,
    }
    platform = {
        "java": 21,
        "minecraft": _gradle_property("minecraft_version"),
        "neoforge": _gradle_property("neo_version"),
    }
    pending = [
        name
        for name, row in packages.items()
        if row["status"] == "pending"
    ]
    if version != t31.TARGET_VERSION:
        pending.append("mod_version")
    if content_checks["jar"]["status"] != "measured":
        pending.append("jar_content")
    if content_checks["zip"]["status"] != "measured":
        pending.append("zip_content")
    owned_inputs = {
        common.relative(BUILDER): common.sha256_file(BUILDER),
        common.relative(t31.GRADLE): common.sha256_file(t31.GRADLE),
        "tools/t31_common.py": common.sha256_file(t31.TOOLS / "t31_common.py"),
    }
    status = (
        "T31_RELEASE_MANIFEST_COMPLETE"
        if not pending
        else "T31_RELEASE_MANIFEST_PENDING"
    )
    return {
        "content_checks": content_checks,
        "currentness": {"owned_inputs": owned_inputs},
        "docs": docs,
        "generated_by": "python tools/build_t31_release_manifest.py --write",
        "mod_version": version,
        "owned_inputs": owned_inputs,
        "packages": packages,
        "pending": pending,
        "platform": platform,
        "schema_version": 1,
        "status": status,
        "status_owner": "build_t31_release_manifest",
        "target_version": t31.TARGET_VERSION,
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    return document


def check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {common.relative(OUTPUT)}"]
    expected = build()
    actual = json.loads(OUTPUT.read_text(encoding="utf-8"))
    for key in REPORT_OWNED:
        expected.pop(key, None)
        actual.pop(key, None)
    if common.stable_json(expected) != common.stable_json(actual):
        return [f"{common.relative(OUTPUT)} is stale"]
    return []


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if bool(args.write) == bool(args.check):
        parser.error("choose exactly one of --write, --check")
    try:
        if args.write:
            document = write()
            print(
                f"wrote {common.relative(OUTPUT)} "
                f"status={document.get('status')}"
            )
            return 0
        errors = check()
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print(f"{common.relative(OUTPUT)} is current")
        return 0
    except (OSError, ValueError, json.JSONDecodeError, KeyError) as exc:
        print(str(exc), file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
