#!/usr/bin/env python3
"""Extract the T30 60-row Hopper metalset SOURCE_BACKED evidence.

Parses the fixed-revision Loader_MultiTileEntities metalset calls, resolves
MT tokens through the T20 helper, and maps ANY.W explicitly to source
tungsten. Writes the evidence ledger and the runtime hopper_variants.json
catalog. Does not register blocks.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import Counter
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import build_t20_worldgen_source as t20  # noqa: E402
from tools import t27_common as common  # noqa: E402
from tools import t35_common as t35  # noqa: E402

TOOLS = common.TOOLS
POLICY = TOOLS / "t30_hopper_source_policy.json"
TREE_MANIFEST = TOOLS / "t13_gt6_tree_manifest.json"
CROSS_REFERENCE = TOOLS / "gt6_oredict_cross_reference.json"
GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
MATERIALS_DIR = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
)
OUTPUT = TOOLS / "t30_hopper_source_evidence.json"
BUILDER = Path(__file__).resolve()

METALSET_CALL = re.compile(
    r"metalset\(\s*aRegistry,\s*aMetal,\s*aUtilMetal,\s*aMachine,\s*aWooden,\s*"
    r"(MT\.[A-Za-z_][A-Za-z0-9_]*|ANY\.[A-Za-z_][A-Za-z0-9_]*)\s*,\s*"
    r"(\d+)\s*,\s*([0-9.]+)F\s*,\s*([0-9.]+)F\s*,\s*(\d+)\s*\)"
)
ANY_DECLARATION = re.compile(
    r"\b([A-Za-z_][A-Za-z0-9_]*)\s*=\s*any\(\s*\"([^\"]+)\"",
)


def _require(path: Path) -> None:
    if not path.is_file():
        raise FileNotFoundError(common.relative(path))


def _policy() -> dict[str, Any]:
    document = common.load_json(POLICY)
    if document.get("status") != "T30_HOPPER_SOURCE_POLICY":
        raise ValueError("T30 hopper source policy header drifted")
    if document.get("source", {}).get("revision") != common.SOURCE_REVISION:
        raise ValueError("T30 hopper source policy revision drifted")
    return document


def _source_records(policy: dict[str, Any]) -> dict[str, dict[str, Any]]:
    manifest = common.load_json(TREE_MANIFEST)
    source = policy["source"]
    if (
        manifest.get("revision") != source["revision"]
        or manifest.get("tree_sha1") != source["tree_sha1"]
    ):
        raise ValueError("T13 fixed-source manifest revision/tree drifted")
    by_path = {row["path"]: row for row in manifest["entries"]}
    records = source["files"]
    for key, record in records.items():
        manifest_row = by_path.get(record["path"])
        if manifest_row is None:
            raise ValueError(f"{key}: source path is absent from T13 manifest")
        if manifest_row["sha"] != record["git_blob_sha1"]:
            raise ValueError(f"{key}: source blob differs from T13 manifest")
        anchors = record.get("anchors")
        if not isinstance(anchors, list) or not anchors:
            raise ValueError(f"{key}: source anchors must be non-empty")
    return records


def _git_bytes(data: bytes) -> bytes:
    """Normalize Windows checkouts to the Git blob payload (LF)."""
    return data.replace(b"\r\n", b"\n").replace(b"\r", b"")


def _read_sources(policy: dict[str, Any]) -> dict[str, bytes]:
    root = ROOT / policy["source"]["local_root"]
    result: dict[str, bytes] = {}
    for key, record in _source_records(policy).items():
        path = root / record["path"]
        if not path.is_file():
            raise ValueError(f"{key}: local source root lacks {record['path']}")
        result[key] = _git_bytes(path.read_bytes())
    return result


def _verify_source_bytes(
    policy: dict[str, Any],
    sources: dict[str, bytes],
) -> dict[str, dict[str, Any]]:
    evidence: dict[str, dict[str, Any]] = {}
    for key, record in _source_records(policy).items():
        data = sources.get(key)
        if data is None:
            raise ValueError(f"{key}: source bytes are missing")
        actual_blob = t20.git_blob_sha1(data)
        if actual_blob != record["git_blob_sha1"]:
            raise ValueError(
                f"{key}: git blob mismatch {actual_blob} != {record['git_blob_sha1']}"
            )
        text = data.decode("utf-8")
        missing = [anchor for anchor in record["anchors"] if anchor not in text]
        if missing:
            raise ValueError(f"{key}: missing source anchors {missing}")
        evidence[key] = {
            "anchors": list(record["anchors"]),
            "bytes": len(data),
            "git_blob_sha1": actual_blob,
            "line_count": len(text.splitlines()),
            "path": record["path"],
            "sha256": common.sha256_bytes(data),
        }
    return evidence


def _any_label(any_source: str, token: str) -> str:
    symbol = token.removeprefix("ANY.")
    labels = {
        match.group(1): match.group(2)
        for match in ANY_DECLARATION.finditer(any_source)
    }
    if symbol not in labels:
        raise ValueError(f"{token} is not declared in ANY.java")
    return labels[symbol]


def _resolve_token(
    token: str,
    *,
    mt_source: str,
    any_source: str,
    helpers: dict[str, tuple[int, str]],
    policy: dict[str, Any],
) -> dict[str, Any]:
    if token.startswith("ANY."):
        mapping = (policy.get("any_tokens") or {}).get(token)
        if not isinstance(mapping, dict):
            raise ValueError(f"{token} has no explicit representative mapping")
        label = _any_label(any_source, token)
        expected_label = mapping.get("any_label")
        if label != expected_label:
            raise ValueError(
                f"{token} ANY.java label {label!r} != policy {expected_label!r}"
            )
        representative = mapping.get("representative_token")
        if not isinstance(representative, str) or not representative.startswith(
            "MT."
        ):
            raise ValueError(f"{token} representative_token must be an MT.* token")
        source_id, source_name = t20.resolve_material_token(
            mt_source, representative, helpers
        )
        return {
            "any_label": label,
            "representative_token": representative,
            "source_id": source_id,
            "source_name": source_name,
            "token": token,
        }
    if not token.startswith("MT."):
        raise ValueError(f"unsupported metalset token {token}")
    source_id, source_name = t20.resolve_material_token(
        mt_source, token, helpers
    )
    return {
        "any_label": None,
        "representative_token": token,
        "source_id": source_id,
        "source_name": source_name,
        "token": token,
    }


def _cc_id(source_id: int, material_id_to_cc: dict[str, str]) -> str:
    mapped = material_id_to_cc.get(str(source_id))
    if not isinstance(mapped, str) or not mapped:
        raise ValueError(f"unresolved source id {source_id}")
    return f"cruciblecraft:{mapped}"


def _material_json_exists(cc_id: str) -> bool:
    path = MATERIALS_DIR / f"{cc_id.split(':', 1)[1]}.json"
    return path.is_file()


def _has_plate(cc_id: str, gate: dict[str, Any]) -> bool:
    materials = gate.get("materials") or {}
    forms = materials.get(cc_id.split(":", 1)[1])
    return isinstance(forms, list) and "plate" in forms


def _catalog_path(policy: dict[str, Any]) -> Path:
    return ROOT / policy["catalog"]["path"]


def catalog_document(evidence: dict[str, Any]) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "source_revision": evidence["source_revision"],
        "variants": [
            {"material": row["cc_material"], "slots": row["slots"]}
            for row in evidence["rows"]
        ],
    }


def _extract_rows(
    policy: dict[str, Any],
    sources: dict[str, bytes],
) -> list[dict[str, Any]]:
    loader = sources["loader"].decode("utf-8")
    mt_source = sources["materials"].decode("utf-8")
    any_source = sources["any"].decode("utf-8")
    helpers = t20.helper_materials(mt_source)
    cross = common.load_json(CROSS_REFERENCE)
    material_id_to_cc = cross["material_id_to_cc"]
    gate = common.load_json(GATE)
    expected = policy["expected"]
    hopper_base = int(expected["hopper_id_base"])
    queue_base = int(expected["queue_id_base"])
    hopper_floor = int(expected["hopper_slot_floor"])
    queue_floor = int(expected["queue_slot_floor"])
    rows: list[dict[str, Any]] = []
    for match in METALSET_CALL.finditer(loader):
        token, a_id, hardness, resistance, hopper_size = match.groups()
        line = loader[: match.start()].count("\n") + 1
        resolved = _resolve_token(
            token,
            mt_source=mt_source,
            any_source=any_source,
            helpers=helpers,
            policy=policy,
        )
        cc_id = _cc_id(resolved["source_id"], material_id_to_cc)
        slots = max(hopper_floor, int(hopper_size))
        row = {
            "aID": int(a_id),
            "cc_material": cc_id,
            "hardness": float(hardness),
            "hopper_gt6_id": hopper_base + int(a_id),
            "material_json_present": _material_json_exists(cc_id),
            "plate_present": _has_plate(cc_id, gate),
            "queue_gt6_id": queue_base + int(a_id),
            "queue_slots": max(queue_floor, slots),
            "representative_token": resolved["representative_token"],
            "resistance": float(resistance),
            "slots": slots,
            "source_id": resolved["source_id"],
            "source_line": line,
            "source_name": resolved["source_name"],
            "token": token,
        }
        if resolved["any_label"] is not None:
            row["any_label"] = resolved["any_label"]
        rows.append(row)
    return rows


def _assert_rows(policy: dict[str, Any], rows: list[dict[str, Any]]) -> None:
    expected = policy["expected"]
    if len(rows) != int(expected["row_count"]):
        raise ValueError(f"expected {expected['row_count']} metalset rows, got {len(rows)}")
    tokens = [row["token"] for row in rows]
    materials = [row["cc_material"] for row in rows]
    if len(set(tokens)) != len(tokens):
        raise ValueError("metalset tokens are not unique")
    if len(set(materials)) != len(materials):
        raise ValueError("CC materials are not unique")
    unresolved = [row["token"] for row in rows if not row["cc_material"]]
    if unresolved:
        raise ValueError(f"unresolved materials: {unresolved}")
    missing_json = [
        row["cc_material"] for row in rows if not row["material_json_present"]
    ]
    if missing_json:
        raise ValueError(f"missing material JSON: {missing_json}")
    missing_plate = [row["cc_material"] for row in rows if not row["plate_present"]]
    if missing_plate:
        raise ValueError(f"materials missing plate: {missing_plate}")
    actual_distribution = {
        str(slots): count
        for slots, count in sorted(Counter(row["slots"] for row in rows).items())
    }
    expected_distribution = {
        str(int(slots)): int(count)
        for slots, count in expected["slot_distribution"].items()
    }
    if actual_distribution != expected_distribution:
        raise ValueError(
            "slot distribution drifted "
            f"expected={expected_distribution} actual={actual_distribution}"
        )


def build() -> dict[str, Any]:
    for path in (POLICY, TREE_MANIFEST, CROSS_REFERENCE, GATE):
        _require(path)
    if not MATERIALS_DIR.is_dir():
        raise FileNotFoundError(common.relative(MATERIALS_DIR))
    policy = _policy()
    sources = _read_sources(policy)
    files = _verify_source_bytes(policy, sources)
    rows = _extract_rows(policy, sources)
    _assert_rows(policy, rows)
    catalog = catalog_document(
        {
            "rows": rows,
            "source_revision": common.SOURCE_REVISION,
        }
    )
    return {
        "catalog": {
            "path": policy["catalog"]["path"],
            "row_count": len(catalog["variants"]),
            "sha256": common.sha256_record(catalog),
        },
        "counts": {
            "cc_materials": len({row["cc_material"] for row in rows}),
            "plate_present": sum(1 for row in rows if row["plate_present"]),
            "rows": len(rows),
            "tokens": len({row["token"] for row in rows}),
            "unresolved": 0,
        },
        "currentness": {
            "owned_inputs": {
                common.relative(BUILDER): common.sha256_file(BUILDER),
                common.relative(POLICY): common.sha256_file(POLICY),
                common.relative(TREE_MANIFEST): common.sha256_file(TREE_MANIFEST),
                common.relative(CROSS_REFERENCE): common.sha256_file(
                    CROSS_REFERENCE
                ),
                common.relative(GATE): common.sha256_file(GATE),
            }
        },
        "expected_slot_distribution": {
            str(int(slots)): int(count)
            for slots, count in policy["expected"]["slot_distribution"].items()
        },
        "generated_by": "python tools/build_t30_hopper_source_evidence.py --write",
        "rows": rows,
        "schema_version": 1,
        "source_files": files,
        "source_revision": common.SOURCE_REVISION,
        "source_tree_sha1": policy["source"]["tree_sha1"],
        "status": "T30_HOPPER_SOURCE_EVIDENCE",
    }


def write() -> dict[str, Any]:
    document = build()
    common.write_stable(OUTPUT, document)
    policy = _policy()
    common.write_stable(_catalog_path(policy), catalog_document(document))
    return document


def check() -> list[str]:
    document = build()
    errors = t35.check_compact(OUTPUT, document, encode=common.stable_json)
    errors.extend(
        t35.check_compact(
            _catalog_path(_policy()),
            catalog_document(document),
            encode=common.stable_json,
        )
    )
    return errors


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
            counts = document["counts"]
            print(
                f"wrote {common.relative(OUTPUT)} "
                f"rows={counts['rows']} materials={counts['cc_materials']} "
                f"unresolved={counts['unresolved']}"
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
