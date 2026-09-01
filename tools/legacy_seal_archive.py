#!/usr/bin/env python3
"""Copy T38-T49 seal-bound bytes into archive/sealed without rewriting seals."""
from __future__ import annotations

import argparse
import importlib
import shutil
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import closeout_seal
from tools import legacy_seal_resolver as resolver
from tools import t35_common as t35

EXTRA_COMMON_ATTRS = (
    "IDENTITY_DELTA",
    "RUNTIME_DELTA",
    "PUBLICATION_DELTA",
)


def _copy_file(src: Path, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.is_file():
        if dest.read_bytes() == src.read_bytes():
            return
        raise ValueError(f"archive file drifted: {t35.relative(dest)}")
    shutil.copy2(src, dest)
    if dest.read_bytes() != src.read_bytes():
        raise ValueError(f"archive copy failed: {t35.relative(dest)}")


def _copy_tree(src: Path, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists():
        if closeout_seal._tree_hash(dest) == closeout_seal._tree_hash(src):
            return
        raise ValueError(f"archive tree drifted: {t35.relative(dest)}")
    shutil.copytree(src, dest, copy_function=shutil.copy2)
    if closeout_seal._tree_hash(dest) != closeout_seal._tree_hash(src):
        raise ValueError(f"archive tree copy failed: {t35.relative(dest)}")


def _extra_paths(card_id: str) -> list[Path]:
    module_name = f"tools.{card_id.lower()}_common"
    try:
        module = importlib.import_module(module_name)
    except ImportError:
        return []
    extras: list[Path] = []
    for attr in EXTRA_COMMON_ATTRS:
        value = getattr(module, attr, None)
        if isinstance(value, Path) and value.is_file():
            extras.append(value)
    return extras


def _artifact_row(
    *,
    field: str,
    live: Path,
    archive: Path,
    sha256: str | None,
    kind: str,
) -> dict[str, Any]:
    return {
        "archive": t35.relative(archive),
        "field": field,
        "kind": kind,
        "live": t35.relative(live),
        "sha256": sha256,
    }


def archive_card(card_id: str) -> dict[str, Any]:
    spec = closeout_seal.spec_for(card_id)
    live_seal = closeout_seal.seal_path(card_id)
    if not live_seal.is_file():
        raise FileNotFoundError(f"missing live seal: {t35.relative(live_seal)}")
    live_seal_bytes = live_seal.read_bytes()
    seal = closeout_seal.load_seal(card_id)
    hashes = seal.get("hashes") or {}
    dest_root = resolver.card_dir(card_id)
    dest_root.mkdir(parents=True, exist_ok=True)
    seal_copy = resolver.seal_snapshot_path(card_id)
    if seal_copy.is_file() and seal_copy.read_bytes() != live_seal_bytes:
        raise ValueError(f"refusing to rewrite seal snapshot {t35.relative(seal_copy)}")
    if not seal_copy.is_file():
        seal_copy.write_bytes(live_seal_bytes)
    if seal_copy.read_bytes() != live_seal_bytes:
        raise ValueError(f"seal snapshot is not byte-identical: {t35.relative(seal_copy)}")

    artifacts: list[dict[str, Any]] = [
        _artifact_row(
            field="closeout_seal",
            live=live_seal,
            archive=seal_copy,
            sha256=t35.sha256_file(live_seal),
            kind="file",
        )
    ]
    mismatches: list[str] = []
    for field in resolver.SPEC_PATH_FIELDS:
        live = getattr(spec, field)
        if live is None or not live.exists():
            continue
        dest = dest_root / Path(t35.relative(live))
        hash_field = resolver.HASH_FIELD_FOR_SPEC[field]
        expected = hashes.get(hash_field)
        if live.is_file():
            _copy_file(live, dest)
            actual = t35.sha256_file(dest)
            kind = "file"
        else:
            _copy_tree(live, dest)
            actual = closeout_seal._tree_hash(dest)
            kind = "tree"
        if expected and actual != expected:
            mismatches.append(
                f"{card_id} {hash_field} archive {actual} != seal {expected}"
            )
        artifacts.append(
            _artifact_row(
                field=hash_field,
                live=live,
                archive=dest,
                sha256=actual,
                kind=kind,
            )
        )
    for extra in _extra_paths(card_id):
        dest = dest_root / Path(t35.relative(extra))
        _copy_file(extra, dest)
        artifacts.append(
            _artifact_row(
                field=extra.name,
                live=extra,
                archive=dest,
                sha256=t35.sha256_file(dest),
                kind="file",
            )
        )
    if mismatches:
        raise ValueError("; ".join(mismatches))
    document = {
        "artifacts": artifacts,
        "card_id": card_id,
        "complete_key": spec.complete_key,
        "generated_by": "python tools/legacy_seal_archive.py --write",
        "next_issue_id": spec.next_issue_id,
        "schema_version": 1,
        "seal_sha256": t35.sha256_file(live_seal),
        "source_revision": seal.get("source_revision"),
        "status": "SEALED_ARCHIVE",
    }
    t35.write_stable(resolver.manifest_path(card_id), document)
    return document


def verify_card(card_id: str) -> list[str]:
    errors: list[str] = []
    if not resolver.has_archive(card_id):
        return [f"{card_id} archive is missing"]
    live_seal = closeout_seal.seal_path(card_id)
    snapshot = resolver.seal_snapshot_path(card_id)
    if live_seal.is_file() and snapshot.read_bytes() != live_seal.read_bytes():
        errors.append(f"{card_id} archive seal snapshot is not byte-identical")
    try:
        seal = closeout_seal.load_seal(card_id)
    except (OSError, ValueError) as error:
        return [f"{card_id} cannot load seal: {error}"]
    hashes = seal.get("hashes") or {}
    spec = closeout_seal.resolved_spec(card_id)
    for field, hash_field in resolver.HASH_FIELD_FOR_SPEC.items():
        expected = hashes.get(hash_field)
        if not expected:
            continue
        path = getattr(spec, field)
        if path is None:
            continue
        if not path.exists():
            errors.append(f"{card_id} archived {hash_field} missing: {t35.relative(path)}")
            continue
        actual = (
            t35.sha256_file(path) if path.is_file() else closeout_seal._tree_hash(path)
        )
        if actual != expected:
            errors.append(
                f"{card_id} archived {hash_field} {actual} != seal {expected}"
            )
    return errors


def write_all() -> dict[str, str]:
    written: dict[str, str] = {}
    for card_id in closeout_seal.closed_card_ids():
        archive_card(card_id)
        written[card_id] = t35.relative(resolver.manifest_path(card_id))
    return written


def check_all() -> list[str]:
    errors: list[str] = []
    for card_id in closeout_seal.closed_card_ids():
        errors.extend(verify_card(card_id))
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--card", action="append", dest="cards")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    cards = tuple(args.cards) if args.cards else closeout_seal.closed_card_ids()
    try:
        if args.write:
            for card_id in cards:
                archive_card(card_id)
                print(f"Wrote {t35.relative(resolver.manifest_path(card_id))}")
            return 0
        errors: list[str] = []
        for card_id in cards:
            errors.extend(verify_card(card_id))
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("legacy seal archives are current")
        return 0
    except (OSError, ValueError, KeyError) as error:
        print(f"legacy seal archive failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
