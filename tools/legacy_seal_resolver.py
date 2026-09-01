"""Resolve T38-T49 seal-bound paths through archive/sealed when present.

Live closeout seal JSON stays at tools/*_closeout_seal.json and is never
rewritten. After archive, --check reads the byte-identical snapshot tree.
"""
from __future__ import annotations

from pathlib import Path
from typing import Any

from tools import t35_common as t35

ARCHIVE_ROOT = t35.ROOT / "archive" / "sealed"
MANIFEST_NAME = "archive_manifest.json"
SEAL_COPY_NAME = "closeout_seal.json"

SPEC_PATH_FIELDS = (
    "census",
    "topology",
    "readiness",
    "receipt",
    "production_lock",
    "generated_root",
    "support_root",
    "gametest_java",
    "gametest_log",
    "publication_group_manifest",
    "shard_manifest",
    "runtime_dependency_manifest",
)

HASH_FIELD_FOR_SPEC = {
    "census": "census",
    "topology": "topology",
    "readiness": "readiness",
    "receipt": "receipt",
    "production_lock": "production_lock",
    "generated_root": "generated_recipes",
    "support_root": "locked_support",
    "gametest_java": "gametest_java",
    "gametest_log": "gametest_log",
    "publication_group_manifest": "publication_group_manifest",
    "shard_manifest": "shard_manifest",
    "runtime_dependency_manifest": "runtime_dependency_manifest",
}


def card_dir(card_id: str) -> Path:
    return ARCHIVE_ROOT / card_id


def manifest_path(card_id: str) -> Path:
    return card_dir(card_id) / MANIFEST_NAME


def seal_snapshot_path(card_id: str) -> Path:
    return card_dir(card_id) / SEAL_COPY_NAME


def has_archive(card_id: str) -> bool:
    return manifest_path(card_id).is_file() and seal_snapshot_path(card_id).is_file()


def load_manifest(card_id: str) -> dict[str, Any]:
    path = manifest_path(card_id)
    if not path.is_file():
        raise FileNotFoundError(f"missing archive manifest: {t35.relative(path)}")
    return t35.load_json(path)


def archived_card_ids() -> tuple[str, ...]:
    if not ARCHIVE_ROOT.is_dir():
        return ()
    cards = []
    for child in sorted(ARCHIVE_ROOT.iterdir()):
        if child.is_dir() and (child / MANIFEST_NAME).is_file():
            cards.append(child.name)
    return tuple(cards)


def resolve(card_id: str, live_path: Path | None) -> Path | None:
    if live_path is None:
        return None
    if not has_archive(card_id):
        return live_path
    rel = t35.relative(live_path)
    archived = card_dir(card_id) / Path(rel)
    if archived.exists():
        return archived
    return live_path


def resolve_field(card_id: str, field: str, live_path: Path | None) -> Path | None:
    if live_path is None:
        return None
    if not has_archive(card_id):
        return live_path
    hash_field = HASH_FIELD_FOR_SPEC.get(field, field)
    try:
        manifest = load_manifest(card_id)
    except FileNotFoundError:
        return resolve(card_id, live_path)
    live_rel = t35.relative(live_path)
    for row in manifest.get("artifacts") or []:
        if row.get("field") != hash_field:
            continue
        archive_rel = str(row.get("archive") or "")
        if archive_rel:
            archived = t35.ROOT / archive_rel
            if archived.exists():
                return archived
        live_row = str(row.get("live") or "")
        if live_row == live_rel:
            archived = card_dir(card_id) / Path(live_row)
            if archived.exists():
                return archived
    return resolve(card_id, live_path)


def seal_for_read(card_id: str, live_seal: Path) -> Path:
    snapshot = seal_snapshot_path(card_id)
    if snapshot.is_file():
        return snapshot
    return live_seal
