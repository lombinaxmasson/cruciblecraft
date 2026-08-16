#!/usr/bin/env python3
"""Normalize the fixed GT6 worldgen source used by T20."""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import re
import urllib.request
import zipfile
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t20_worldgen_source_policy.json"
TREE_MANIFEST = TOOLS / "t13_gt6_tree_manifest.json"
CROSS_REFERENCE = TOOLS / "gt6_oredict_cross_reference.json"
OUTPUT = TOOLS / "t20_gt6_worldgen_source.json"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def git_blob_sha1(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def source_records(policy: dict[str, Any]) -> dict[str, dict[str, Any]]:
    manifest = load(TREE_MANIFEST)
    if (
        manifest.get("revision") != policy["source"]["revision"]
        or manifest.get("tree_sha1") != policy["source"]["tree_sha1"]
    ):
        raise ValueError("T13 fixed-source manifest revision/tree drifted")
    by_path = {row["path"]: row for row in manifest["entries"]}
    records = policy["source"]["files"]
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


def validate_policy(policy: dict[str, Any]) -> None:
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T20_WORLDGEN_SOURCE_POLICY"
        or policy.get("source", {}).get("repository")
        != "GregTech6/gregtech6"
        or policy.get("target", {}).get("catalog_entries") != 129
    ):
        raise ValueError("T20 source policy header/target drifted")
    precedence = policy["classification"]["precedence"]
    if precedence != [
        "EXPLICIT_SMALL_SOURCE",
        "RANDOM_SMALL_GEM_SOURCE",
        "UNIQUE_LARGE_ROLE_SOURCE",
        "DESIGN_POLICY_NO_GT6_WORLDGEN_FACT",
    ]:
        raise ValueError("T20 source classification precedence drifted")
    source_records(policy)


def download_sources(
    policy: dict[str, Any],
) -> dict[str, bytes]:
    revision = policy["source"]["revision"]
    url = (
        "https://codeload.github.com/GregTech6/gregtech6/zip/"
        + revision
    )
    request = urllib.request.Request(
        url, headers={"User-Agent": "CrucibleCraft-T20-source-replay"}
    )
    with urllib.request.urlopen(request, timeout=120) as response:
        archive = zipfile.ZipFile(io.BytesIO(response.read()))
    result: dict[str, bytes] = {}
    for key, record in source_records(policy).items():
        suffix = "/" + record["path"]
        names = [name for name in archive.namelist() if name.endswith(suffix)]
        if len(names) != 1:
            raise ValueError(
                f"{key}: expected one archive member ending in {suffix}"
            )
        result[key] = archive.read(names[0])
    return result


def read_source_root(
    policy: dict[str, Any], source_root: Path
) -> dict[str, bytes]:
    result: dict[str, bytes] = {}
    for key, record in source_records(policy).items():
        path = source_root / record["path"]
        if not path.is_file():
            raise ValueError(f"{key}: fixed source root lacks {record['path']}")
        result[key] = path.read_bytes()
    return result


def verify_source_bytes(
    policy: dict[str, Any], sources: dict[str, bytes]
) -> dict[str, dict[str, Any]]:
    evidence: dict[str, dict[str, Any]] = {}
    for key, record in source_records(policy).items():
        data = sources.get(key)
        if data is None:
            raise ValueError(f"{key}: source bytes are missing")
        actual_blob = git_blob_sha1(data)
        if actual_blob != record["git_blob_sha1"]:
            raise ValueError(
                f"{key}: git blob mismatch "
                f"{actual_blob} != {record['git_blob_sha1']}"
            )
        text = data.decode("utf-8")
        missing = [anchor for anchor in record["anchors"] if anchor not in text]
        if missing:
            raise ValueError(f"{key}: missing source anchors {missing}")
        evidence[key] = {
            "path": record["path"],
            "git_blob_sha1": actual_blob,
            "sha256": sha256(data),
            "bytes": len(data),
            "line_count": len(text.splitlines()),
            "anchors": list(record["anchors"]),
        }
    return evidence


def split_arguments(value: str) -> list[str]:
    result: list[str] = []
    start = 0
    depth = 0
    quoted = False
    escaped = False
    for index, character in enumerate(value):
        if quoted:
            if escaped:
                escaped = False
            elif character == "\\":
                escaped = True
            elif character == '"':
                quoted = False
            continue
        if character == '"':
            quoted = True
        elif character == "(":
            depth += 1
        elif character == ")":
            depth -= 1
        elif character == "," and depth == 0:
            result.append(value[start:index].strip())
            start = index + 1
    result.append(value[start:].strip())
    return result


def literal_string(value: str, label: str) -> str:
    match = re.fullmatch(r'"([^"]+)"', value.strip())
    if match is None:
        raise ValueError(f"{label}: expected a string literal, got {value}")
    return match.group(1)


def literal_int(value: str, label: str) -> int:
    if re.fullmatch(r"-?[0-9][0-9_]*", value.strip()) is None:
        raise ValueError(f"{label}: expected an integer literal, got {value}")
    return int(value.replace("_", ""))


def declaration_rows(
    loader: str, class_name: str
) -> list[tuple[int, list[str]]]:
    pattern = re.compile(
        rf"new\s+{re.escape(class_name)}\s*\((.*?)\)\s*;", re.DOTALL
    )
    rows: list[tuple[int, list[str]]] = []
    for match in pattern.finditer(loader):
        arguments = split_arguments(match.group(1))
        if not arguments or not arguments[0].startswith('"'):
            continue
        line = loader.count("\n", 0, match.start()) + 1
        rows.append((line, arguments))
    return rows


def block_range(source: str, marker: str) -> tuple[int, int]:
    start = source.index(marker)
    opening = source.index("{", start)
    depth = 0
    quoted = False
    escaped = False
    for index in range(opening, len(source)):
        character = source[index]
        if quoted:
            if escaped:
                escaped = False
            elif character == "\\":
                escaped = True
            elif character == '"':
                quoted = False
            continue
        if character == '"':
            quoted = True
        elif character == "{":
            depth += 1
        elif character == "}":
            depth -= 1
            if depth == 0:
                return opening + 1, index
    raise ValueError(f"unterminated source block: {marker}")


def helper_materials(mt_source: str) -> dict[str, tuple[int, str]]:
    pattern = re.compile(
        r"static\s+OreDictMaterial\s+([A-Za-z_$][\w$]*)\s*"
        r"\(\s*\)\s*\{\s*return\s+[A-Za-z_$][\w$]*\s*"
        r"\(\s*([0-9][0-9_]*)\s*,\s*\"([^\"]+)\""
    )
    return {
        match.group(1): (
            int(match.group(2).replace("_", "")),
            match.group(3),
        )
        for match in pattern.finditer(mt_source)
    }


def material_section(
    mt_source: str, token: str
) -> tuple[str, str]:
    oremats_start, oremats_end = block_range(
        mt_source, "public static class OREMATS"
    )
    if token.startswith("MT.OREMATS."):
        return token.removeprefix("MT.OREMATS."), mt_source[
            oremats_start:oremats_end
        ]
    if not token.startswith("MT."):
        raise ValueError(f"unsupported material token {token}")
    # Exclude the nested OREMATS declarations when resolving MT-level names.
    return token.removeprefix("MT."), (
        mt_source[:oremats_start] + mt_source[oremats_end:]
    )


def resolve_material_token(
    mt_source: str,
    token: str,
    helpers: dict[str, tuple[int, str]],
    seen: set[str] | None = None,
) -> tuple[int, str]:
    symbol, section = material_section(mt_source, token)
    visited = set() if seen is None else set(seen)
    if token in visited:
        raise ValueError(f"material alias cycle at {token}")
    visited.add(token)

    direct = re.search(
        rf"\b{re.escape(symbol)}\s*=\s*[A-Za-z_$][\w$]*\s*"
        r"\(\s*([0-9][0-9_]*)\s*,\s*\"([^\"]+)\"",
        section,
    )
    if direct is not None:
        return int(direct.group(1).replace("_", "")), direct.group(2)

    direct_id = re.search(
        rf"\b{re.escape(symbol)}\s*=\s*[A-Za-z_$][\w$]*\s*"
        r"\(\s*([0-9][0-9_]*)\s*,",
        section,
    )
    if direct_id is not None:
        return int(direct_id.group(1).replace("_", "")), symbol

    helper = re.search(
        rf"\b{re.escape(symbol)}\s*=\s*([A-Za-z_$][\w$]*)\s*"
        r"\(\s*\)",
        section,
    )
    if helper is not None and helper.group(1) in helpers:
        return helpers[helper.group(1)]

    alias = re.search(
        rf"\b{re.escape(symbol)}\s*=\s*"
        r"(?:(MT)\.)?([A-Za-z_$][\w$]*)\s*(?=[,;])",
        section,
    )
    if alias is not None:
        prefix = "MT." if alias.group(1) else (
            "MT.OREMATS." if token.startswith("MT.OREMATS.") else "MT."
        )
        return resolve_material_token(
            mt_source, prefix + alias.group(2), helpers, visited
        )
    raise ValueError(f"cannot resolve fixed GT6 material token {token}")


def material_record(
    token: str,
    mt_source: str,
    helpers: dict[str, tuple[int, str]],
    material_id_to_cc: dict[str, str],
) -> dict[str, Any]:
    source_id, source_name = resolve_material_token(
        mt_source, token, helpers
    )
    return {
        "token": token,
        "source_id": source_id,
        "source_name": source_name,
        "cc_material": material_id_to_cc.get(str(source_id)),
    }


def normalize_large_rows(
    loader: str,
    mt_source: str,
    material_id_to_cc: dict[str, str],
) -> list[dict[str, Any]]:
    helpers = helper_materials(mt_source)
    rows: list[dict[str, Any]] = []
    for line, arguments in declaration_rows(loader, "WorldgenOresLarge"):
        if re.fullmatch(r'"[^"]+"', arguments[0].strip()) is None:
            continue
        source_id = literal_string(arguments[0], f"large line {line}")
        if len(arguments) < 13:
            raise ValueError(f"{source_id}: incomplete large-vein declaration")
        layers = {
            role: material_record(
                arguments[index], mt_source, helpers, material_id_to_cc
            )
            for role, index in (
                ("top", 8),
                ("bottom", 9),
                ("between", 10),
                ("spread", 11),
            )
        }
        rows.append({
            "source_fact_id": source_id,
            "source_line": line,
            "default_expression": arguments[1],
            "indicator_rocks_expression": arguments[2],
            "min_y": literal_int(arguments[3], source_id),
            "max_y": literal_int(arguments[4], source_id),
            "weight": literal_int(arguments[5], source_id),
            "density": literal_int(arguments[6], source_id),
            "size": literal_int(arguments[7], source_id),
            "layers": layers,
            "dimension_lists": arguments[12:],
        })
    if len(rows) != 40:
        raise ValueError(f"expected 40 fixed large veins, got {len(rows)}")
    return rows


def normalize_small_rows(
    loader: str,
    mt_source: str,
    material_id_to_cc: dict[str, str],
) -> list[dict[str, Any]]:
    helpers = helper_materials(mt_source)
    rows: list[dict[str, Any]] = []
    for line, arguments in declaration_rows(loader, "WorldgenOresSmall"):
        if re.fullmatch(r'"[^"]+"', arguments[0].strip()) is None:
            continue
        source_id = literal_string(arguments[0], f"small line {line}")
        if len(arguments) < 7:
            raise ValueError(f"{source_id}: incomplete small-ore declaration")
        rows.append({
            "source_fact_id": source_id,
            "source_line": line,
            "default_expression": arguments[1],
            "min_y": literal_int(arguments[2], source_id),
            "max_y": literal_int(arguments[3], source_id),
            "amount": literal_int(arguments[4], source_id),
            "material": material_record(
                arguments[5], mt_source, helpers, material_id_to_cc
            ),
            "dimension_lists": arguments[6:],
        })
    if len(rows) != 75:
        raise ValueError(f"expected 75 fixed explicit small ores, got {len(rows)}")
    return rows


def build_from_sources(
    policy: dict[str, Any], sources: dict[str, bytes]
) -> dict[str, Any]:
    evidence = verify_source_bytes(policy, sources)
    loader = sources["loader"].decode("utf-8")
    mt_source = sources["materials"].decode("utf-8")
    cross = load(CROSS_REFERENCE)
    material_id_to_cc = cross["material_id_to_cc"]
    large = normalize_large_rows(loader, mt_source, material_id_to_cc)
    small = normalize_small_rows(loader, mt_source, material_id_to_cc)
    unresolved = sorted({
        row["layers"][role]["token"]
        for row in large
        for role in ("top", "bottom", "between", "spread")
        if row["layers"][role]["cc_material"] is None
    } | {
        row["material"]["token"]
        for row in small
        if row["material"]["cc_material"] is None
    })
    return {
        "schema_version": 1,
        "status": "T20_GT6_WORLDGEN_SOURCE_READY",
        "source": {
            "repository": policy["source"]["repository"],
            "revision": policy["source"]["revision"],
            "tree_sha1": policy["source"]["tree_sha1"],
            "license": policy["source"]["license"],
            "files": evidence,
        },
        "semantics": {
            "large_selection": {
                "grid_chunks": 3,
                "selection": "one enabled large family selected by mWeight",
                "source": (
                    "GT6WorldGenerator.WorldGenContainer.run large-ore loop"
                ),
            },
            "large_layers": {
                "bottom": "tMinY-1 through tMinY+1",
                "between": "one of tMinY+2 or tMinY+3",
                "top": "tMinY+3 through tMinY+5",
                "spread": "one of tMinY-1 through tMinY+5",
            },
            "small_distribution": (
                "per chunk, max(1, amount/2 + random(1+amount)/2) attempts"
            ),
            "dynamic_random_small_gem": {
                "source_fact_id": "ore.small.<RANDOM_SMALL_GEM_ORE>",
                "source_line": 878,
                "min_y": 5,
                "max_y": 250,
                "amount": 1,
                "selector": "TD.Properties.RANDOM_SMALL_GEM_ORE",
            },
        },
        "counts": {
            "large_source_facts": len(large),
            "explicit_small_source_facts": len(small),
            "dynamic_random_small_rules": 1,
            "source_material_tokens_without_cc_identity": len(unresolved),
        },
        "large_veins": large,
        "explicit_small_ores": small,
        "unresolved_cc_material_tokens": unresolved,
    }


def validate_compact(
    policy: dict[str, Any], document: dict[str, Any]
) -> None:
    if (
        document.get("schema_version") != 1
        or document.get("status") != "T20_GT6_WORLDGEN_SOURCE_READY"
        or document.get("source", {}).get("revision")
        != policy["source"]["revision"]
        or document.get("source", {}).get("tree_sha1")
        != policy["source"]["tree_sha1"]
    ):
        raise ValueError("committed T20 source evidence header drifted")
    counts = document.get("counts") or {}
    if (
        counts.get("large_source_facts") != 40
        or counts.get("explicit_small_source_facts") != 75
        or counts.get("dynamic_random_small_rules") != 1
        or len(document.get("large_veins") or []) != 40
        or len(document.get("explicit_small_ores") or []) != 75
    ):
        raise ValueError("committed T20 source cardinalities drifted")
    expected_files = source_records(policy)
    actual_files = document["source"]["files"]
    if set(expected_files) != set(actual_files):
        raise ValueError("committed T20 source file set drifted")
    for key, record in expected_files.items():
        if actual_files[key]["git_blob_sha1"] != record["git_blob_sha1"]:
            raise ValueError(f"{key}: committed source blob drifted")


def main() -> int:
    parser = argparse.ArgumentParser()
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--check", action="store_true")
    mode.add_argument("--write", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    parser.add_argument("--reference-only", action="store_true")
    parser.add_argument("--source-root", type=Path)
    args = parser.parse_args()
    if args.full_replay and args.reference_only:
        parser.error("--full-replay and --reference-only are exclusive")
    if args.write and not args.full_replay:
        parser.error("--write requires --full-replay")
    if args.source_root is not None and not args.full_replay:
        parser.error("--source-root requires --full-replay")

    try:
        policy = load(POLICY)
        validate_policy(policy)
        if args.full_replay:
            sources = (
                read_source_root(policy, args.source_root)
                if args.source_root is not None
                else download_sources(policy)
            )
            document = build_from_sources(policy, sources)
            encoded = stable(document)
            if args.write:
                OUTPUT.write_text(encoded, encoding="utf-8", newline="\n")
                print(
                    "Wrote fixed GT6 worldgen source evidence to "
                    f"{OUTPUT.relative_to(ROOT).as_posix()}"
                )
                return 0
            if (
                not OUTPUT.is_file()
                or OUTPUT.read_text(encoding="utf-8") != encoded
            ):
                print("Committed T20 worldgen source evidence is stale.")
                return 1
            print("Fixed GT6 worldgen source replay matches committed evidence.")
            return 0

        if not args.reference_only:
            parser.error("ordinary checks require --reference-only")
        if not OUTPUT.is_file():
            raise ValueError("committed T20 source evidence is missing")
        validate_compact(policy, load(OUTPUT))
        print("Committed T20 worldgen source evidence is current.")
        return 0
    except (OSError, ValueError, json.JSONDecodeError) as error:
        print(f"T20 worldgen source build failed: {error}")
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
