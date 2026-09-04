#!/usr/bin/env python3
"""Immutable closeout seals for closed recipe/repair cards.

Closed-card --check compares seal bytes/hashes and N/L/R/gap arithmetic.
It does not call player_gametest_present() or live composed v2.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import legacy_seal_resolver as seal_resolver
from tools import io_common as io

TOOLS = io.TOOLS
SCHEMA = TOOLS / "closeout_seal.schema.json"
IDENTITY_LEDGER_V2 = TOOLS / "global_build_identity_ledger.v2.json"
RUNTIME_MANIFEST_V2 = TOOLS / "compact_recipe_runtime_manifest.v2.json"
BATH_IDENTITY_LIVE_GENERATED = (
    io.ROOT
    / "src/recipe_generated/resources/data/cruciblecraft/recipe/bath/identity"
)

REQUIRED_SEAL_FIELDS = (
    "schema_version",
    "status",
    "card_id",
    "sealed_at_wave",
    "complete_family_count",
    "relation_count",
    "reclassification_delta",
    "remaining_recipe_gap",
    "production_lock_sha256",
    "gametest_status",
    "hashes",
)

REQUIRED_HASH_FIELDS = ("census", "topology", "readiness")


@dataclass(frozen=True)
class CardSpec:
    card_id: str
    census: Path
    topology: Path
    readiness: Path
    receipt: Path | None
    production_lock: Path | None
    generated_root: Path | None
    support_root: Path | None
    gametest_java: Path | None
    gametest_log: Path | None
    publication_group_manifest: Path | None
    shard_manifest: Path | None
    runtime_dependency_manifest: Path | None
    complete_key: str
    next_issue_id: str | None = None


def seal_path(card_id: str) -> Path:
    slug = card_id.lower().replace("-", "_")
    return TOOLS / f"{slug}_closeout_seal.json"


def is_sealed(card_id: str) -> bool:
    return seal_path(card_id).is_file()


def _file_hash(path: Path | None) -> str | None:
    if path is None or not path.is_file():
        return None
    return hashlib.sha256(path.read_bytes()).hexdigest()


def _tree_hash(root: Path | None) -> str | None:
    if root is None or not root.exists():
        return None
    hasher = hashlib.sha256()
    if root.is_file():
        hasher.update(root.read_bytes())
        return hasher.hexdigest()
    files = sorted(path for path in root.rglob("*") if path.is_file())
    if not files:
        return hasher.hexdigest()
    for path in files:
        hasher.update(path.relative_to(root).as_posix().encode("utf-8"))
        hasher.update(path.read_bytes())
    return hasher.hexdigest()


def _load_json(path: Path) -> dict[str, Any]:
    return io.load_json(path)


def _path_from_manifest(manifest: dict[str, Any], spec_field: str) -> Path | None:
    hash_field = seal_resolver.HASH_FIELD_FOR_SPEC.get(spec_field, spec_field)
    for row in manifest.get("artifacts") or []:
        if str(row.get("field") or "") != hash_field:
            continue
        live = str(row.get("live") or "")
        if live:
            return io.ROOT / live
    return None


def _spec_from_archive(card_id: str) -> CardSpec:
    manifest = seal_resolver.load_manifest(card_id)
    return CardSpec(
        card_id=card_id,
        census=_path_from_manifest(manifest, "census") or io.TOOLS / "missing_census.json",
        topology=_path_from_manifest(manifest, "topology") or io.TOOLS / "missing_topology.json",
        readiness=_path_from_manifest(manifest, "readiness") or io.TOOLS / "missing_readiness.json",
        receipt=_path_from_manifest(manifest, "receipt"),
        production_lock=_path_from_manifest(manifest, "production_lock"),
        generated_root=_path_from_manifest(manifest, "generated_root"),
        support_root=_path_from_manifest(manifest, "support_root"),
        gametest_java=_path_from_manifest(manifest, "gametest_java"),
        gametest_log=_path_from_manifest(manifest, "gametest_log"),
        publication_group_manifest=_path_from_manifest(
            manifest, "publication_group_manifest"
        ),
        shard_manifest=_path_from_manifest(manifest, "shard_manifest"),
        runtime_dependency_manifest=_path_from_manifest(
            manifest, "runtime_dependency_manifest"
        ),
        complete_key=str(manifest.get("complete_key") or ""),
        next_issue_id=manifest.get("next_issue_id"),
    )


def _card_specs() -> dict[str, CardSpec]:
    return {
        card_id: _spec_from_archive(card_id)
        for card_id in seal_resolver.archived_card_ids()
    }


def spec_for(card_id: str) -> CardSpec:
    specs = _card_specs()
    if card_id not in specs:
        raise ValueError(f"no closeout seal spec for {card_id}")
    return specs[card_id]


def resolved_spec(card_id: str) -> CardSpec:
    spec = spec_for(card_id)
    if not seal_resolver.has_archive(card_id):
        return spec
    resolved = {
        field: seal_resolver.resolve_field(card_id, field, getattr(spec, field))
        for field in seal_resolver.SPEC_PATH_FIELDS
    }
    return CardSpec(
        card_id=spec.card_id,
        complete_key=spec.complete_key,
        next_issue_id=spec.next_issue_id,
        **resolved,
    )


def _seal_document_path(card_id: str) -> Path:
    return seal_resolver.seal_for_read(card_id, seal_path(card_id))


def closed_card_ids() -> tuple[str, ...]:
    return tuple(_card_specs())


def ledger_from_census(census: dict[str, Any]) -> dict[str, int]:
    remaining = census.get("remaining_ordinary") or {}
    work_set = census.get("work_set") or {}
    n = census.get("complete_family_count")
    if n is None:
        n = work_set.get("family_count")
    if n is None:
        n = census.get("complete_storage_registrations")
    l = work_set.get("source_rows")
    if l is None:
        l = census.get("complete_storage_registrations")
    r = census.get("reclassification_delta")
    if r is None:
        r = remaining.get("reclassification_delta")
    gap = census.get("remaining_recipe_gap")
    if gap is None:
        gap = remaining.get("remaining_ordinary_families")
    if gap is None:
        gap = census.get("recipe_closing_execution_gap")
    return {
        "complete_family_count": int(n or 0),
        "relation_count": int(l or 0),
        "reclassification_delta": int(r or 0),
        "remaining_recipe_gap": int(gap or 0),
    }


def _validate_seal_shape(document: dict[str, Any], card_id: str) -> list[str]:
    errors: list[str] = []
    version = document.get("schema_version")
    semantic = "/" in str(card_id)
    if semantic:
        if version not in (1, 2):
            errors.append(f"{card_id} seal schema_version must be 1 or 2")
        if version == 2 and not document.get("supersedes_sha256"):
            errors.append(f"{card_id} schema v2 seal missing supersedes_sha256")
    elif version == 2:
        if not document.get("supersedes_sha256"):
            errors.append(f"{card_id} schema v2 seal missing supersedes_sha256")
        if not document.get("repair_wave"):
            errors.append(f"{card_id} schema v2 seal missing repair_wave")
    elif version != 1:
        errors.append(f"{card_id} seal schema_version must be 1")
    if document.get("status") != "SEALED":
        errors.append(f"{card_id} seal status must be SEALED")
    if document.get("card_id") != card_id:
        errors.append(f"{card_id} seal card_id drifted")
    for field in REQUIRED_SEAL_FIELDS:
        if field not in document:
            errors.append(f"{card_id} seal missing {field}")
    hashes = document.get("hashes") or {}
    if not isinstance(hashes, dict):
        errors.append(f"{card_id} seal hashes must be an object")
        return errors
    for field in REQUIRED_HASH_FIELDS:
        if not hashes.get(field):
            errors.append(f"{card_id} seal missing hashes.{field}")
    return errors


def load_seal(card_id: str) -> dict[str, Any]:
    path = _seal_document_path(card_id)
    if not path.is_file():
        raise FileNotFoundError(f"missing closeout seal: {io.relative(path)}")
    try:
        document = _load_json(path)
    except json.JSONDecodeError as error:
        raise ValueError(
            f"CORRUPT: {io.relative(path)} JSON line={error.lineno} "
            f"column={error.colno}"
        ) from error
    if not isinstance(document, dict):
        raise ValueError(f"CORRUPT: {io.relative(path)} is not an object")
    errors = _validate_seal_shape(document, card_id)
    if errors:
        raise ValueError("; ".join(errors))
    return document


def build_seal(card_id: str) -> dict[str, Any]:
    spec = spec_for(card_id)
    missing = [
        io.relative(path)
        for path in (spec.census, spec.topology, spec.readiness)
        if not path.is_file()
    ]
    if missing:
        raise ValueError(f"{card_id} seal source missing: " + ", ".join(missing))
    census = _load_json(spec.census)
    ledger = ledger_from_census(census)
    receipt_status = "NONE"
    if spec.receipt is not None and spec.receipt.is_file():
        receipt = _load_json(spec.receipt)
        receipt_status = str(receipt.get("status") or "NONE")
        if receipt_status != "PASS":
            raise ValueError(f"{card_id} GameTest receipt is not PASS; refuse seal")
    hashes = {
        "census": _file_hash(spec.census),
        "topology": _file_hash(spec.topology),
        "readiness": _file_hash(spec.readiness),
        "receipt": _file_hash(spec.receipt),
        "gametest_java": _file_hash(spec.gametest_java),
        "gametest_log": _file_hash(spec.gametest_log),
        "generated_recipes": _tree_hash(spec.generated_root),
        "locked_support": _tree_hash(spec.support_root),
        "publication_group_manifest": _file_hash(spec.publication_group_manifest),
        "shard_manifest": _file_hash(spec.shard_manifest),
        "runtime_dependency_manifest": _file_hash(spec.runtime_dependency_manifest),
        "production_lock": _file_hash(spec.production_lock),
    }
    return {
        "schema_version": 1,
        "status": "SEALED",
        "card_id": card_id,
        "sealed_at_wave": "bath/remainder-VR",
        "source_revision": io.SOURCE_REVISION,
        "generated_by": "python tools/closeout_seal.py --write",
        "complete_family_count": ledger["complete_family_count"],
        "relation_count": ledger["relation_count"],
        "reclassification_delta": ledger["reclassification_delta"],
        "remaining_recipe_gap": ledger["remaining_recipe_gap"],
        "production_lock_sha256": hashes["production_lock"],
        "gametest_status": receipt_status if receipt_status == "PASS" else "NONE",
        "receipt_sha256": hashes["receipt"],
        "composed_identity_ledger_v2_sha256": _file_hash(IDENTITY_LEDGER_V2),
        "composed_runtime_manifest_v2_sha256": _file_hash(RUNTIME_MANIFEST_V2),
        "hashes": hashes,
        "note": (
            "Close-time snapshot. Closed-card --check compares these hashes and "
            "N/L/R/gap; it must not re-pin live composed v2 or verification profiles."
        ),
    }


def pre_repair_hash_path() -> Path:
    return (
        io.TOOLS
        / "waves"
        / "ordinary-wave"
        / "closeout-integrity-repair"
        / "pre_repair_hashes.json"
    )


def recorded_pre_repair_sha(slug: str) -> str | None:
    path = pre_repair_hash_path()
    if not path.is_file():
        return None
    document = _load_json(path)
    seals = document.get("seals") if isinstance(document.get("seals"), dict) else {}
    row = seals.get(slug) if isinstance(seals, dict) else None
    if isinstance(row, dict) and row.get("sha256"):
        return str(row["sha256"])
    return None


def _file_bytes_sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def live_reseal_supersedes_archive(card_id: str) -> bool:
    """True when live is a v2 reseal of the archived v1 snapshot."""
    live = seal_path(card_id)
    archived = seal_resolver.seal_snapshot_path(card_id)
    if not live.is_file() or not archived.is_file():
        return False
    if live.read_bytes() == archived.read_bytes():
        return False
    try:
        document = _load_json(live)
    except (OSError, json.JSONDecodeError, ValueError):
        return False
    if document.get("schema_version") != 2:
        return False
    return document.get("supersedes_sha256") == _file_bytes_sha256(archived)


def _evaluate_wave_ready_or_raise(slug: str) -> dict[str, Any]:
    from tools.recipe_bulk import ordinary_wave as ordinary
    from tools.wave_closeout import spec_for as wave_spec_for

    spec = wave_spec_for(slug)
    inputs = ordinary.load_closeout_inputs(slug)
    receipt = inputs["receipt"]
    passed = bool(receipt and receipt.get("status") == "PASS")
    unique_active = spec.unique_active_wave if passed else slug
    verdict = ordinary.evaluate_wave_ready(
        slug,
        source=inputs["source"],
        lock=inputs["lock"],
        equivalence=inputs["equivalence"],
        player_path=inputs["player_path"],
        receipt=receipt,
        load=inputs["load"],
        census=inputs["census"],
        unique_active=unique_active,
    )
    if not verdict.get("ready"):
        raise ValueError(
            f"{slug} closeout seal write forbidden; wave_ready blockers: "
            + ",".join(verdict.get("blockers") or ["unknown"])
        )
    return verdict


def write_wave_seal(
    slug: str,
    *,
    supersedes_sha256: str | None = None,
    repair_wave: str | None = None,
) -> dict[str, Any]:
    from tools.wave_closeout import spec_for as wave_spec_for
    from tools.wave_closeout import seal_path as wave_seal_path

    spec = wave_spec_for(slug)
    missing = [
        io.relative(path)
        for path in (spec.census, spec.topology, spec.readiness)
        if not path.is_file()
    ]
    if missing:
        raise ValueError(f"{slug} seal source missing: " + ", ".join(missing))
    if spec.production_lock is not None:
        _evaluate_wave_ready_or_raise(slug)
    census = _load_json(spec.census)
    ledger = ledger_from_census(census)
    receipt_status = "NONE"
    if spec.receipt is not None and spec.receipt.is_file():
        receipt = _load_json(spec.receipt)
        receipt_status = str(receipt.get("status") or "NONE")
        if receipt_status != "PASS":
            raise ValueError(f"{slug} GameTest receipt is not PASS; refuse seal")
    hashes = {
        "census": _file_hash(spec.census),
        "topology": _file_hash(spec.topology),
        "readiness": _file_hash(spec.readiness),
        "receipt": _file_hash(spec.receipt),
        "gametest_java": _file_hash(spec.gametest_java),
        "gametest_log": _file_hash(spec.gametest_log),
        "generated_recipes": _tree_hash(spec.generated_root),
        "locked_support": _tree_hash(spec.support_root),
        "publication_group_manifest": _file_hash(spec.publication_group_manifest),
        "shard_manifest": _file_hash(spec.shard_manifest),
        "runtime_dependency_manifest": _file_hash(spec.runtime_dependency_manifest),
        "production_lock": _file_hash(spec.production_lock),
    }
    schema_version = 2 if supersedes_sha256 else 1
    document: dict[str, Any] = {
        "schema_version": schema_version,
        "status": "SEALED",
        "card_id": slug,
        "sealed_at_wave": slug,
        "source_revision": io.SOURCE_REVISION,
        "generated_by": "python tools/closeout_seal.py --write --wave",
        "complete_family_count": ledger["complete_family_count"],
        "relation_count": ledger["relation_count"],
        "reclassification_delta": ledger["reclassification_delta"],
        "remaining_recipe_gap": ledger["remaining_recipe_gap"],
        "production_lock_sha256": hashes["production_lock"],
        "gametest_status": receipt_status if receipt_status == "PASS" else "NONE",
        "receipt_sha256": hashes["receipt"],
        "composed_identity_ledger_v2_sha256": _file_hash(IDENTITY_LEDGER_V2),
        "composed_runtime_manifest_v2_sha256": _file_hash(RUNTIME_MANIFEST_V2),
        "hashes": hashes,
        "note": (
            "Semantic-wave close-time snapshot. Recomputes wave_ready from load "
            "verdict and measurements. Does not rewrite frozen v2 or archived "
            "card seal bytes."
        ),
    }
    if supersedes_sha256:
        document["supersedes_sha256"] = supersedes_sha256
        document["repair_wave"] = (
            repair_wave or "ordinary-wave/closeout-integrity-repair"
        )
    output = wave_seal_path(slug)
    if output.is_file():
        existing = output.read_text(encoding="utf-8")
        if existing == io.stable_json(document):
            return _load_json(output)
        existing_sha = _file_bytes_sha256(output)
        allowed = supersedes_sha256 or recorded_pre_repair_sha(slug)
        if not allowed or existing_sha != allowed:
            raise ValueError(
                f"refusing to replace {io.relative(output)}; closeout seals are write-once"
            )
    io.write_stable(output, document)
    return document


def write_seal(
    card_id: str,
    *,
    supersedes_sha256: str | None = None,
    repair_wave: str | None = None,
) -> dict[str, Any]:
    document = build_seal(card_id)
    output = seal_path(card_id)
    if supersedes_sha256:
        if not repair_wave:
            raise ValueError("--supersedes requires --repair-wave")
        spec = spec_for(card_id)
        if (
            card_id == "bath/identity"
            and (spec.generated_root is None or not spec.generated_root.exists())
            and BATH_IDENTITY_LIVE_GENERATED.exists()
        ):
            document["hashes"]["generated_recipes"] = _tree_hash(BATH_IDENTITY_LIVE_GENERATED)
        document["schema_version"] = 2
        document["supersedes_sha256"] = supersedes_sha256
        document["repair_wave"] = repair_wave
        document["generated_by"] = (
            "python tools/closeout_seal.py --write --card --supersedes"
        )
        document["note"] = (
            "Live reseal. Archive snapshot stays the superseded v1 seal. "
            "Closed-card --check still reads archive hashes."
        )
        if output.is_file():
            existing_text = output.read_text(encoding="utf-8")
            if existing_text == io.stable_json(document):
                return _load_json(output)
            existing_sha = _file_bytes_sha256(output)
            existing_doc = _load_json(output)
            if (
                existing_sha != supersedes_sha256
                and existing_doc.get("supersedes_sha256") != supersedes_sha256
            ):
                raise ValueError(
                    f"--supersedes does not match current {io.relative(output)}"
                )
        io.write_stable(output, document)
        return document
    if output.is_file():
        existing = output.read_text(encoding="utf-8")
        if existing != io.stable_json(document):
            raise ValueError(
                f"refusing to replace {io.relative(output)}; closeout seals are write-once"
            )
        return _load_json(output)
    io.write_stable(output, document)
    return document


def write_all() -> dict[str, str]:
    written: dict[str, str] = {}
    for card_id in closed_card_ids():
        write_seal(card_id)
        written[card_id] = io.relative(seal_path(card_id))
    return written


def _hash_mismatch(card_id: str, label: str, path: Path | None, expected: str | None) -> list[str]:
    if not expected:
        return []
    actual = _file_hash(path)
    if actual != expected:
        rel = io.relative(path) if path is not None else label
        return [f"{card_id} {label} hash drifted from seal: {rel}"]
    return []


def _parse_seal_file(path: Path, card_id: str) -> list[str]:
    if not path.is_file():
        return [f"missing closeout seal: {io.relative(path)}"]
    try:
        text = path.read_text(encoding="utf-8")
        document = json.loads(text)
    except json.JSONDecodeError as error:
        return [
            f"CORRUPT: {io.relative(path)} JSON line={error.lineno} "
            f"column={error.colno} size={path.stat().st_size}"
        ]
    except OSError as error:
        return [f"CORRUPT: {io.relative(path)} {error}"]
    if not isinstance(document, dict):
        return [f"CORRUPT: {io.relative(path)} is not an object"]
    return _validate_seal_shape(document, card_id)


def check_seal_document(card_id: str) -> list[str]:
    live = seal_path(card_id)
    archived = _seal_document_path(card_id)
    if live != archived:
        live_errors = _parse_seal_file(live, card_id)
        if live_errors:
            return live_errors
        errors = _parse_seal_file(archived, card_id)
        if live.is_file() and archived.is_file() and live.read_bytes() != archived.read_bytes():
            if live_reseal_supersedes_archive(card_id):
                return live_errors
            errors.append(
                f"{card_id} live seal is not byte-identical to archive snapshot"
            )
        return errors
    return _parse_seal_file(archived, card_id)


def check_census(card_id: str) -> list[str]:
    errors = check_seal_document(card_id)
    if errors:
        return errors
    spec = resolved_spec(card_id)
    seal = load_seal(card_id)
    if not spec.census.is_file():
        return [f"missing census: {io.relative(spec.census)}"]
    try:
        census = _load_json(spec.census)
    except json.JSONDecodeError as error:
        return [
            f"CORRUPT: {io.relative(spec.census)} JSON line={error.lineno} "
            f"column={error.colno}"
        ]
    errors.extend(_hash_mismatch(card_id, "census", spec.census, (seal.get("hashes") or {}).get("census")))
    ledger = ledger_from_census(census)
    for field in (
        "complete_family_count",
        "relation_count",
        "reclassification_delta",
        "remaining_recipe_gap",
    ):
        if int(ledger[field]) != int(seal.get(field) or 0):
            errors.append(
                f"{card_id} census {field} {ledger[field]} != seal {seal.get(field)}"
            )
    return errors


def check_topology(card_id: str) -> list[str]:
    errors = check_seal_document(card_id)
    if errors:
        return errors
    spec = resolved_spec(card_id)
    seal = load_seal(card_id)
    if not spec.topology.is_file():
        return [f"missing topology: {io.relative(spec.topology)}"]
    try:
        topology = _load_json(spec.topology)
    except json.JSONDecodeError as error:
        return [
            f"CORRUPT: {io.relative(spec.topology)} JSON line={error.lineno} "
            f"column={error.colno}"
        ]
    errors.extend(
        _hash_mismatch(card_id, "topology", spec.topology, (seal.get("hashes") or {}).get("topology"))
    )
    if not topology.get(spec.complete_key):
        errors.append(f"{card_id} topology {spec.complete_key} is not complete in the seal snapshot")
    if topology.get("unique_active_card") == "bath/remainder-VR":
        errors.append("unique_active_card must not be occupied by bath/remainder-VR")
    if spec.next_issue_id and topology.get("next_issue_id") != spec.next_issue_id:
        errors.append(f"{card_id} next_issue_id must stay {spec.next_issue_id}")
    if topology.get("unique_active_card") is not None and topology.get(spec.complete_key):
        errors.append(f"{card_id} complete must leave unique_active_card null")
    return errors


def check_readiness(card_id: str) -> list[str]:
    errors = check_seal_document(card_id)
    if errors:
        return errors
    spec = resolved_spec(card_id)
    seal = load_seal(card_id)
    if not spec.readiness.is_file():
        return [f"missing readiness: {io.relative(spec.readiness)}"]
    try:
        readiness = _load_json(spec.readiness)
    except json.JSONDecodeError as error:
        return [
            f"CORRUPT: {io.relative(spec.readiness)} JSON line={error.lineno} "
            f"column={error.colno}"
        ]
    errors.extend(
        _hash_mismatch(
            card_id, "readiness", spec.readiness, (seal.get("hashes") or {}).get("readiness")
        )
    )
    status = str(readiness.get("status") or "")
    if status and "READY" not in status and "SEALED" not in status:
        errors.append(f"{card_id} readiness status {status} is not READY")
    evidence = readiness.get("evidence") or {}
    if evidence.get("unique_active_card") == "bath/remainder-VR":
        errors.append("readiness unique_active_card must not be bath/remainder-VR")
    if spec.next_issue_id:
        opening = readiness.get("bath_identity_opening") or {}
        if opening.get("next_issue_id") not in (None, spec.next_issue_id) and evidence.get(
            "next_issue_id"
        ) not in (None, spec.next_issue_id):
            if evidence.get("next_issue_id") != spec.next_issue_id:
                errors.append(f"{card_id} readiness next_issue_id must stay {spec.next_issue_id}")
    return errors


def check_receipt_binding(card_id: str) -> list[str]:
    errors = check_seal_document(card_id)
    if errors:
        return errors
    spec = resolved_spec(card_id)
    seal = load_seal(card_id)
    hashes = seal.get("hashes") or {}
    if spec.receipt is None:
        return errors
    errors.extend(_hash_mismatch(card_id, "receipt", spec.receipt, hashes.get("receipt")))
    if seal.get("receipt_sha256") and hashes.get("receipt") != seal.get("receipt_sha256"):
        errors.append(f"{card_id} seal receipt_sha256 drifted from hashes.receipt")
    return errors


def check_hashed_sidecar(card_id: str, field: str) -> list[str]:
    errors = check_seal_document(card_id)
    if errors:
        return errors
    spec = resolved_spec(card_id)
    path = getattr(spec, field)
    expected = (load_seal(card_id).get("hashes") or {}).get(field)
    if path is None or not expected:
        return errors
    if not path.is_file():
        return [f"missing {field}: {io.relative(path)}"]
    try:
        _load_json(path)
    except json.JSONDecodeError as error:
        return [
            f"CORRUPT: {io.relative(path)} JSON line={error.lineno} column={error.colno}"
        ]
    errors.extend(_hash_mismatch(card_id, field, path, expected))
    return errors


def check_runtime_dependency_manifest(card_id: str) -> list[str]:
    return check_hashed_sidecar(card_id, "runtime_dependency_manifest")


def check_publication_group_manifest(card_id: str) -> list[str]:
    return check_hashed_sidecar(card_id, "publication_group_manifest")


def check_shard_manifest(card_id: str) -> list[str]:
    return check_hashed_sidecar(card_id, "shard_manifest")


def check_production_lock(card_id: str) -> list[str]:
    return check_hashed_sidecar(card_id, "production_lock")


def check_closed_card(card_id: str) -> list[str]:
    errors: list[str] = []
    errors.extend(check_census(card_id))
    errors.extend(check_topology(card_id))
    errors.extend(check_readiness(card_id))
    errors.extend(check_receipt_binding(card_id))
    errors.extend(check_runtime_dependency_manifest(card_id))
    return list(dict.fromkeys(errors))


def check_all() -> list[str]:
    """Check semantic wave seals. Numbered-card seals are no longer a live gate."""
    from tools.wave_closeout import known_slugs
    from tools.wave_closeout import seal_path as wave_seal_path

    errors: list[str] = []
    for slug in known_slugs():
        if wave_seal_path(slug).is_file():
            errors.extend(check_wave_seal(slug))
    return errors


def live_or_sealed_errors(
    card_id: str,
    kind: str,
    live: Callable[[], list[str]],
) -> list[str]:
    sealed = check_if_sealed(card_id, kind)
    if sealed is not None:
        return sealed
    return live()


def sealed_or_live_present(card_id: str, live: Callable[[], bool]) -> bool:
    if is_sealed(card_id):
        return sealed_gametest_present(card_id)
    return live()


def check_if_sealed(card_id: str, kind: str) -> list[str] | None:
    if not is_sealed(card_id):
        return None
    dispatch: dict[str, Callable[[str], list[str]]] = {
        "census": check_census,
        "topology": check_topology,
        "readiness": check_readiness,
        "receipt": check_receipt_binding,
        "runtime_dependency_manifest": check_runtime_dependency_manifest,
        "publication_group_manifest": check_publication_group_manifest,
        "shard_manifest": check_shard_manifest,
        "production_lock": check_production_lock,
        "closed": check_closed_card,
    }
    probe = dispatch.get(kind)
    if probe is None:
        raise ValueError(f"unknown seal check kind {kind!r}")
    return probe(card_id)


def sealed_complete(card_id: str) -> bool:
    if not is_sealed(card_id):
        return False
    try:
        seal = load_seal(card_id)
    except (OSError, ValueError, json.JSONDecodeError):
        return False
    return not check_census(card_id) and int(seal.get("complete_family_count") or 0) > 0


def sealed_gametest_present(card_id: str) -> bool:
    if not is_sealed(card_id):
        return False
    try:
        seal = load_seal(card_id)
    except (OSError, ValueError, json.JSONDecodeError):
        return False
    return (
        seal.get("gametest_status") == "PASS"
        and not check_receipt_binding(card_id)
    )


def check_wave_seal(slug: str) -> list[str]:
    from tools.recipe_bulk import ordinary_wave as ordinary
    from tools.wave_closeout import spec_for as wave_spec_for
    from tools.wave_closeout import seal_path as wave_seal_path

    errors: list[str] = []
    path = wave_seal_path(slug)
    if not path.is_file():
        return [f"missing closeout seal: {io.relative(path)}"]
    document = _load_json(path)
    errors.extend(_validate_seal_shape(document, slug))
    spec = wave_spec_for(slug)
    if spec.production_lock is None:
        hashes = document.get("hashes") or {}
        errors.extend(_hash_mismatch(slug, "census", spec.census, hashes.get("census")))
        errors.extend(_hash_mismatch(slug, "topology", spec.topology, hashes.get("topology")))
        errors.extend(
            _hash_mismatch(slug, "readiness", spec.readiness, hashes.get("readiness"))
        )
        if document.get("schema_version") == 2 and not document.get("supersedes_sha256"):
            errors.append(f"{slug} v2 seal missing supersedes_sha256")
        return list(dict.fromkeys(errors))
    inputs = ordinary.load_closeout_inputs(slug)
    receipt = inputs["receipt"]
    passed = bool(receipt and receipt.get("status") == "PASS")
    unique_active = spec.unique_active_wave if passed else slug
    verdict = ordinary.evaluate_wave_ready(
        slug,
        source=inputs["source"],
        lock=inputs["lock"],
        equivalence=inputs["equivalence"],
        player_path=inputs["player_path"],
        receipt=receipt,
        load=inputs["load"],
        census=inputs["census"],
        unique_active=unique_active,
    )
    live_sha = _file_bytes_sha256(path)
    pre_sha = recorded_pre_repair_sha(slug)
    if not verdict.get("ready"):
        if pre_sha and live_sha == pre_sha:
            return errors
        errors.append(
            f"{slug} seal is SEALED but wave_ready is false: "
            + ",".join(verdict.get("blockers") or ["unknown"])
        )
        return errors
    hashes = document.get("hashes") or {}
    errors.extend(_hash_mismatch(slug, "census", spec.census, hashes.get("census")))
    errors.extend(_hash_mismatch(slug, "topology", spec.topology, hashes.get("topology")))
    errors.extend(
        _hash_mismatch(slug, "readiness", spec.readiness, hashes.get("readiness"))
    )
    errors.extend(
        _hash_mismatch(
            slug,
            "production_lock",
            spec.production_lock,
            hashes.get("production_lock"),
        )
    )
    if document.get("schema_version") == 2 and not document.get("supersedes_sha256"):
        errors.append(f"{slug} v2 seal missing supersedes_sha256")
    return list(dict.fromkeys(errors))


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--card", action="append", dest="cards")
    parser.add_argument("--wave", action="append", dest="waves")
    parser.add_argument(
        "--waves-only",
        action="store_true",
        help="Check semantic wave seals only (default for --check without --card).",
    )
    parser.add_argument(
        "--supersedes",
        default=None,
        help="Existing seal file sha256 for a v2 reseal. Requires --repair-wave.",
    )
    parser.add_argument(
        "--repair-wave",
        default=None,
        help="Repair slug recorded on a v2 reseal. Do not omit when --supersedes is set.",
    )
    args = parser.parse_args(argv)
    if args.waves_only and args.write:
        parser.error("--waves-only is check-only")
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    if args.supersedes and not args.repair_wave:
        parser.error("--supersedes requires --repair-wave")
    if args.repair_wave and not args.supersedes:
        parser.error("--repair-wave requires --supersedes")
    if args.supersedes and not args.waves and not args.cards:
        parser.error("--supersedes requires --card or --wave")
    if args.write and not args.cards and not args.waves:
        parser.error("--write requires --wave or --card")
    if args.check and not args.cards and not args.waves:
        args.waves_only = True
    if args.waves_only and not args.waves:
        from tools.wave_closeout import known_slugs
        from tools.wave_closeout import seal_path as wave_seal_path

        args.waves = [
            slug
            for slug in known_slugs()
            if wave_seal_path(slug).is_file()
        ]
        if not args.waves:
            print("no semantic closeout seals on disk")
            return 0
    if args.waves:
        try:
            if args.write:
                for slug in args.waves:
                    write_wave_seal(
                        slug,
                        supersedes_sha256=args.supersedes,
                        repair_wave=args.repair_wave,
                    )
                    from tools.wave_closeout import seal_path as wave_seal_path

                    print(f"Wrote {io.relative(wave_seal_path(slug))}")
                return 0
            errors: list[str] = []
            for slug in args.waves:
                errors.extend(check_wave_seal(slug))
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("semantic closeout seals are current")
            return 0
        except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
            print(f"closeout seal failed: {error}", file=sys.stderr)
            return 1
    cards = tuple(args.cards) if args.cards else closed_card_ids()
    try:
        if args.write:
            for card_id in cards:
                write_seal(
                    card_id,
                    supersedes_sha256=args.supersedes,
                    repair_wave=args.repair_wave,
                )
                print(f"Wrote {io.relative(seal_path(card_id))}")
            return 0
        errors: list[str] = []
        for card_id in cards:
            errors.extend(check_closed_card(card_id))
        if errors:
            print("\n".join(errors), file=sys.stderr)
            return 1
        print("closeout seals are current")
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"closeout seal failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
