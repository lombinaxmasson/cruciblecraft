#!/usr/bin/env python3
"""Build the fixed-source T13a recipe-map denominator and source inventory."""

from __future__ import annotations

import argparse
import hashlib
import io
import json
import math
import re
import zipfile
from collections import Counter
from pathlib import Path
from typing import Any, Iterable
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t13_recipe_map_policy.json"
TREE_MANIFEST = TOOLS / "t13_gt6_tree_manifest.json"
SYMBOL_INVENTORY = TOOLS / "t13_source_symbol_inventory.json"
OUTPUT = TOOLS / "t13_denominators" / "recipe_maps.json"
LEGACY_ROADMAP = TOOLS / "gt6_map_roadmap.json"
DENOMINATOR_MANIFEST = TOOLS / "t13_denominator_manifest.json"
DUMP_ROOT = ROOT / "gt6_dump" / "gt6_recipe_dump"
DUMP_INDEX = DUMP_ROOT / "index.json"
DEFAULT_SOURCE_ROOT = ROOT / "build" / "t13-gt6-source"

REPOSITORY = "GregTech6/gregtech6"
REVISION = "3703e40308c8c030763fd6297dea8b210d2a77b1"
TREE_SHA1 = "a164302f62a326208fd2076de6fb7cdb0b4602ee"
EXPECTED_MAP_COUNT = 95
EXPECTED_RECIPE_COUNT = 720_841
SOURCE_PREFIX = "src/main/java/"

DEFERRED_FIELDS = (
    "reason",
    "owner",
    "replacement_condition",
    "recheck_point",
)
CLASSIFICATIONS = {
    "in_scope",
    "deferred_with_reason",
    "out_of_scope",
    "unclassified",
}

JAVA_DECLARATION = re.compile(
    r"(?P<mods>(?:(?:public|protected|private|abstract|static|final|"
    r"strictfp|sealed|non-sealed)\s+)*)"
    r"(?P<kind>@interface|class|interface|enum|record)\s+"
    r"(?P<name>[A-Za-z_$][\w$]*)"
    r"(?P<tail>[^{;]*)\{",
    flags=re.MULTILINE,
)
STATIC_FINAL_DECLARATION = re.compile(
    r"(?ms)^[ \t]*(?:public|protected)[ \t]+static[ \t]+final[ \t]+"
    r"(?P<type>[A-Za-z_$][\w$.\[\]<>?, ]*?)"
    r"[ \t\r\n]+(?P<body>[A-Za-z_$][\w$]*\s*=.*?);"
)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable_json(value: Any) -> str:
    return json.dumps(
        value,
        ensure_ascii=False,
        indent=2,
        sort_keys=True,
    ) + "\n"


def canonical_digest(value: Any) -> str:
    payload = json.dumps(
        value,
        ensure_ascii=False,
        sort_keys=True,
        separators=(",", ":"),
    ).encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        while chunk := handle.read(1024 * 1024):
            digest.update(chunk)
    return digest.hexdigest()


def git_blob_sha1(data: bytes) -> str:
    header = f"blob {len(data)}\0".encode("ascii")
    return hashlib.sha1(header + data).hexdigest()


def request_bytes(url: str) -> bytes:
    request = Request(
        url,
        headers={
            "Accept": "application/vnd.github+json",
            "User-Agent": "CrucibleCraft-T13a-source-inventory",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    with urlopen(request) as response:
        return response.read()


def request_json(url: str) -> dict[str, Any]:
    return json.loads(request_bytes(url))


def _entry_counts(entries: Iterable[dict[str, Any]]) -> dict[str, Any]:
    rows = list(entries)
    by_type = Counter(row["type"] for row in rows)
    blobs = [row for row in rows if row["type"] == "blob"]
    return {
        "entries": len(rows),
        "by_type": dict(sorted(by_type.items())),
        "blob_bytes": sum(row.get("size", 0) for row in blobs),
    }


def fetch_tree_manifest() -> dict[str, Any]:
    commit_url = (
        f"https://api.github.com/repos/{REPOSITORY}/git/commits/{REVISION}"
    )
    commit = request_json(commit_url)
    if commit.get("sha") != REVISION:
        raise ValueError("GitHub commit API returned a different revision")
    tree = commit.get("tree") or {}
    if tree.get("sha") != TREE_SHA1:
        raise ValueError("GitHub commit tree SHA does not match the T13 pin")

    recursive_url = (
        f"https://api.github.com/repos/{REPOSITORY}/git/trees/"
        f"{TREE_SHA1}?recursive=1"
    )
    response = request_json(recursive_url)
    if response.get("sha") != TREE_SHA1 or response.get("truncated"):
        raise ValueError("GitHub recursive tree response is wrong or truncated")
    raw_entries = response.get("tree") or []
    selected = [
        {
            "mode": row["mode"],
            "path": row["path"],
            "sha": row["sha"],
            "size": row["size"],
            "type": row["type"],
        }
        for row in raw_entries
        if row.get("type") == "blob"
        and row.get("path", "").startswith(SOURCE_PREFIX)
        and row.get("path", "").endswith(".java")
    ]
    selected.sort(key=lambda row: row["path"])
    manifest = {
        "schema_version": 1,
        "status": "FIXED_GITHUB_TREE_MANIFEST",
        "repository": REPOSITORY,
        "repository_url": f"https://github.com/{REPOSITORY}",
        "revision": REVISION,
        "commit_api_url": commit_url,
        "tree_sha1": TREE_SHA1,
        "recursive_tree_api_url": recursive_url,
        "recursive_tree_truncated": False,
        "filter": {
            "selection": "all Git blobs under src/main/java ending in .java",
            "include_prefixes": [SOURCE_PREFIX],
            "include_suffixes": [".java"],
            "excluded_types": ["tree", "commit"],
            "completeness_scope": (
                "Complete fixed-revision Java source set; category inventories "
                "are deterministic subsets of this superset."
            ),
        },
        "integrity": {
            "raw_tree": _entry_counts(raw_entries),
            "selected_tree": _entry_counts(selected),
            "excluded_entries": len(raw_entries) - len(selected),
            "recursive_tree_payload_sha256": canonical_digest(response),
            "selected_entries_sha256": canonical_digest(selected),
        },
        "entries": selected,
    }
    validate_tree_manifest(manifest)
    return manifest


def validate_tree_manifest(manifest: dict[str, Any]) -> None:
    if (
        manifest.get("repository") != REPOSITORY
        or manifest.get("revision") != REVISION
        or manifest.get("tree_sha1") != TREE_SHA1
        or manifest.get("recursive_tree_truncated") is not False
    ):
        raise ValueError("T13 source tree manifest pin drifted")
    entries = manifest.get("entries") or []
    paths = [row.get("path") for row in entries]
    if not entries or paths != sorted(paths) or len(paths) != len(set(paths)):
        raise ValueError("T13 source manifest paths are empty, unsorted or duplicated")
    for row in entries:
        if (
            row.get("type") != "blob"
            or row.get("mode") not in {"100644", "100755"}
            or not row["path"].startswith(SOURCE_PREFIX)
            or not row["path"].endswith(".java")
            or not re.fullmatch(r"[0-9a-f]{40}", row.get("sha", ""))
            or not isinstance(row.get("size"), int)
            or row["size"] < 0
        ):
            raise ValueError(f"invalid T13 source manifest entry: {row!r}")
    selected = manifest["integrity"]["selected_tree"]
    if (
        selected != _entry_counts(entries)
        or manifest["integrity"]["selected_entries_sha256"]
        != canonical_digest(entries)
    ):
        raise ValueError("T13 selected-tree integrity counts drifted")
    raw = manifest["integrity"]["raw_tree"]
    if (
        raw["entries"] != sum(raw["by_type"].values())
        or raw["entries"] - len(entries)
        != manifest["integrity"]["excluded_entries"]
    ):
        raise ValueError("T13 raw-tree completeness counts are inconsistent")
    required = {
        "src/main/java/gregapi/data/OP.java",
        "src/main/java/gregapi/data/RM.java",
        "src/main/java/gregapi/data/TD.java",
        (
            "src/main/java/gregtech/loaders/b/"
            "Loader_MultiTileEntities.java"
        ),
    }
    missing = required - set(paths)
    if missing:
        raise ValueError(f"T13 key source paths are absent: {sorted(missing)}")


def verify_source_tree(
    source_root: Path,
    manifest: dict[str, Any],
) -> dict[str, Any]:
    validate_tree_manifest(manifest)
    missing: list[str] = []
    mismatches: list[str] = []
    total_bytes = 0
    for row in manifest["entries"]:
        path = source_root / row["path"]
        if not path.is_file():
            missing.append(row["path"])
            continue
        data = path.read_bytes()
        total_bytes += len(data)
        actual_sha = git_blob_sha1(data)
        if len(data) != row["size"] or actual_sha != row["sha"]:
            mismatches.append(
                f"{row['path']}: expected {row['sha']}/{row['size']}, "
                f"got {actual_sha}/{len(data)}"
            )
    if missing or mismatches:
        details = [
            *(f"missing {path}" for path in missing[:20]),
            *mismatches[:20],
        ]
        raise ValueError(
            "T13 fixed source verification failed "
            f"({len(missing)} missing, {len(mismatches)} mismatched):\n"
            + "\n".join(details)
        )
    return {
        "files": len(manifest["entries"]),
        "bytes": total_bytes,
        "selected_entries_sha256": manifest["integrity"][
            "selected_entries_sha256"
        ],
    }


def fetch_source_tree(
    destination: Path,
    manifest: dict[str, Any],
) -> dict[str, Any]:
    validate_tree_manifest(manifest)
    archive_url = (
        f"https://codeload.github.com/{REPOSITORY}/zip/{REVISION}"
    )
    archive = request_bytes(archive_url)
    expected = {row["path"]: row for row in manifest["entries"]}
    destination.mkdir(parents=True, exist_ok=True)
    seen: set[str] = set()
    with zipfile.ZipFile(io.BytesIO(archive)) as bundle:
        for member in bundle.infolist():
            parts = Path(member.filename).parts
            if member.is_dir() or len(parts) < 2:
                continue
            relative = Path(*parts[1:]).as_posix()
            row = expected.get(relative)
            if row is None:
                continue
            data = bundle.read(member)
            actual = git_blob_sha1(data)
            if len(data) != row["size"] or actual != row["sha"]:
                raise ValueError(
                    f"archive blob mismatch for {relative}: "
                    f"{actual}/{len(data)}"
                )
            target = destination / relative
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(data)
            seen.add(relative)
    missing = set(expected) - seen
    if missing:
        raise ValueError(
            f"source archive omitted {len(missing)} selected blobs: "
            f"{sorted(missing)[:20]}"
        )
    return verify_source_tree(destination, manifest)


def strip_java_comments(text: str) -> str:
    result: list[str] = []
    index = 0
    state = "code"
    quote = ""
    while index < len(text):
        char = text[index]
        following = text[index + 1] if index + 1 < len(text) else ""
        if state == "code":
            if char == "/" and following == "/":
                result.extend((" ", " "))
                index += 2
                state = "line_comment"
                continue
            if char == "/" and following == "*":
                result.extend((" ", " "))
                index += 2
                state = "block_comment"
                continue
            if char in {'"', "'"}:
                state = "literal"
                quote = char
                result.append(" ")
            else:
                result.append(char)
        elif state == "line_comment":
            if char == "\n":
                result.append(char)
                state = "code"
            else:
                result.append(" ")
        elif state == "block_comment":
            if char == "*" and following == "/":
                result.extend((" ", " "))
                index += 2
                state = "code"
                continue
            result.append("\n" if char == "\n" else " ")
        else:
            if char == "\\" and following:
                result.extend((" ", " "))
                index += 2
                continue
            result.append("\n" if char == "\n" else " ")
            if char == quote:
                state = "code"
        index += 1
    return "".join(result)


def _split_types(value: str) -> list[str]:
    result: list[str] = []
    current: list[str] = []
    depth = 0
    for char in value:
        if char == "<":
            depth += 1
        elif char == ">":
            depth = max(0, depth - 1)
        if char == "," and depth == 0:
            item = re.sub(r"\s+", " ", "".join(current)).strip()
            if item:
                result.append(item)
            current = []
        else:
            current.append(char)
    item = re.sub(r"\s+", " ", "".join(current)).strip()
    if item:
        result.append(item)
    return result


def _category_paths(paths: Iterable[str]) -> dict[str, list[str]]:
    result = {
        "cover": [],
        "multiblock": [],
        "machine": [],
        "energy": [],
        "op": [],
    }
    for path in paths:
        lower = path.lower()
        basename = Path(path).name.lower()
        if "/cover/" in lower or "/covers/" in lower or "cover" in basename:
            result["cover"].append(path)
        if "multiblock" in lower:
            result["multiblock"].append(path)
        if (
            "/machines/" in lower
            or "machine" in basename
            or lower.startswith("src/main/java/gregtech/tileentity/")
            or path
            == (
                "src/main/java/gregtech/loaders/b/"
                "Loader_MultiTileEntities.java"
            )
        ):
            result["machine"].append(path)
        if "/energy/" in lower or "energy" in basename or path.endswith("/TD.java"):
            result["energy"].append(path)
        if path == "src/main/java/gregapi/data/OP.java":
            result["op"].append(path)
    return {name: sorted(rows) for name, rows in result.items()}


def _registration_kind(path: str, declared_type: str) -> str | None:
    short_type = re.sub(r"\s+", "", declared_type)
    ore_prefix = re.fullmatch(
        r"(?:[A-Za-z_$][\w$]*\.)*OreDictPrefix",
        short_type,
    )
    recipe_map = re.fullmatch(
        r"(?:[A-Za-z_$][\w$]*\.)*RecipeMap[A-Za-z0-9_$]*",
        short_type,
    )
    tag_data = re.fullmatch(
        r"(?:[A-Za-z_$][\w$]*\.)*TagData",
        short_type,
    )
    if path.endswith("/OP.java") and ore_prefix:
        return "ore_dict_prefix"
    if path.endswith("/RM.java") and recipe_map:
        return "recipe_map"
    if path.endswith("/TD.java") and tag_data:
        return "tag_data"
    if recipe_map:
        return "recipe_map"
    if ore_prefix:
        return "ore_dict_prefix"
    if tag_data:
        return "tag_data"
    return None


def _registration_symbols(
    source: str,
    path: str,
    package: str,
    blob_sha1: str,
) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for declaration in STATIC_FINAL_DECLARATION.finditer(source):
        declared_type = re.sub(
            r"\s+", " ", declaration.group("type")
        ).strip()
        registration_kind = _registration_kind(path, declared_type)
        if registration_kind is None:
            continue
        body = declaration.group("body")
        for symbol in re.finditer(
            r"(?m)(?:^|,)\s*([A-Za-z_$][\w$]*)\s*=(?!=)",
            body,
        ):
            absolute = declaration.start("body") + symbol.start(1)
            result.append({
                "kind": registration_kind,
                "symbol": symbol.group(1),
                "declared_type": declared_type,
                "package": package,
                "path": path,
                "blob_sha1": blob_sha1,
                "line": source.count("\n", 0, absolute) + 1,
            })
    return result


def build_symbol_inventory(
    source_root: Path,
    manifest: dict[str, Any],
) -> dict[str, Any]:
    verification = verify_source_tree(source_root, manifest)
    declarations: list[dict[str, Any]] = []
    registrations: list[dict[str, Any]] = []
    packages: set[str] = set()
    manifest_blobs = {
        row["path"]: row["sha"] for row in manifest["entries"]
    }
    for row in manifest["entries"]:
        path = row["path"]
        text = (source_root / path).read_text(encoding="utf-8")
        source = strip_java_comments(text)
        package_match = re.search(
            r"(?m)^\s*package\s+([A-Za-z_$][\w$.]*)\s*;",
            source,
        )
        package = package_match.group(1) if package_match else ""
        packages.add(package)
        for match in JAVA_DECLARATION.finditer(source):
            mods = match.group("mods").split()
            kind = match.group("kind")
            name = match.group("name")
            tail = re.sub(r"\s+", " ", match.group("tail")).strip()
            extends_match = re.search(
                r"\bextends\s+(.+?)(?=\bimplements\b|$)",
                tail,
            )
            implements_match = re.search(r"\bimplements\s+(.+)$", tail)
            extends = (
                _split_types(extends_match.group(1))
                if extends_match
                else []
            )
            implements = (
                _split_types(implements_match.group(1))
                if implements_match
                else []
            )
            declarations.append({
                "package": package,
                "class": name,
                "qualified_name": f"{package}.{name}" if package else name,
                "kind": "annotation" if kind == "@interface" else kind,
                "abstract": kind == "interface" or "abstract" in mods,
                "interface": kind in {"interface", "@interface"},
                "extends": extends,
                "implements": implements,
                "path": path,
                "blob_sha1": row["sha"],
                "line": source.count("\n", 0, match.start()) + 1,
            })
        registrations.extend(
            _registration_symbols(
                source,
                path,
                package,
                row["sha"],
            )
        )
    declarations.sort(
        key=lambda value: (
            value["path"],
            value["line"],
            value["class"],
            value["kind"],
        )
    )
    registrations.sort(
        key=lambda value: (
            value["path"],
            value["line"],
            value["symbol"],
        )
    )
    categories = _category_paths(manifest_blobs)
    for name, paths in categories.items():
        if not paths:
            raise ValueError(f"T13 source category {name!r} has no paths")
    return {
        "schema_version": 1,
        "status": "FIXED_SOURCE_SYMBOL_INVENTORY",
        "repository": REPOSITORY,
        "source_revision": REVISION,
        "source_tree_sha1": TREE_SHA1,
        "tree_manifest": TREE_MANIFEST.relative_to(ROOT).as_posix(),
        "tree_manifest_entries_sha256": manifest["integrity"][
            "selected_entries_sha256"
        ],
        "extraction": {
            "scope": "all selected src/main/java Java blobs",
            "declarations": (
                "comment-stripped named class/interface/annotation/enum/record "
                "declarations; nested declarations are retained"
            ),
            "registration_symbols": (
                "public/protected static final RecipeMap, OreDictPrefix and "
                "TagData fields only"
            ),
            "category_path_predicates": {
                "cover": "path segment cover/covers or basename contains cover",
                "multiblock": "path contains multiblock",
                "machine": (
                    "gregapi machines, the complete gregtech/tileentity "
                    "implementation tree, the MultiTile loader, or a basename "
                    "containing machine"
                ),
                "energy": (
                    "path segment energy, basename contains energy, or TD.java"
                ),
                "op": "exact path src/main/java/gregapi/data/OP.java",
            },
        },
        "counts": {
            "source_files": verification["files"],
            "source_bytes": verification["bytes"],
            "packages": len(packages),
            "declarations": len(declarations),
            "registration_symbols": len(registrations),
            "category_paths": {
                name: len(paths) for name, paths in categories.items()
            },
        },
        "category_paths": categories,
        "declarations_sha256": canonical_digest(declarations),
        "registration_symbols_sha256": canonical_digest(registrations),
        "declarations": declarations,
        "registration_symbols": registrations,
    }


def validate_committed_source_evidence() -> tuple[dict[str, Any], dict[str, Any]]:
    manifest = load(TREE_MANIFEST)
    validate_tree_manifest(manifest)
    inventory = load(SYMBOL_INVENTORY)
    if (
        inventory.get("source_revision") != REVISION
        or inventory.get("source_tree_sha1") != TREE_SHA1
        or inventory.get("tree_manifest_entries_sha256")
        != manifest["integrity"]["selected_entries_sha256"]
    ):
        raise ValueError("T13 source symbol inventory provenance drifted")
    paths = {row["path"]: row["sha"] for row in manifest["entries"]}
    categories = _category_paths(paths)
    if inventory.get("category_paths") != categories:
        raise ValueError("T13 source category path coverage drifted")
    declarations = inventory.get("declarations") or []
    registrations = inventory.get("registration_symbols") or []
    if (
        inventory.get("declarations_sha256")
        != canonical_digest(declarations)
        or inventory.get("registration_symbols_sha256")
        != canonical_digest(registrations)
    ):
        raise ValueError("T13 source symbol inventory digest drifted")
    for symbol in declarations + registrations:
        if paths.get(symbol.get("path")) != symbol.get("blob_sha1"):
            raise ValueError(
                f"T13 source symbol has unknown path/blob: {symbol!r}"
            )
    counts = inventory["counts"]
    if (
        counts["source_files"] != len(paths)
        or counts["declarations"] != len(declarations)
        or counts["registration_symbols"] != len(registrations)
        or counts["category_paths"]
        != {name: len(rows) for name, rows in categories.items()}
    ):
        raise ValueError("T13 source inventory counts drifted")
    return manifest, inventory


def normalize_map_id(source_name: str) -> str:
    if not isinstance(source_name, str):
        raise ValueError("recipe-map nameInternal must be a string")
    return "unnamed" if source_name == "" else source_name


def _json_string(header: bytes, field: str) -> str:
    match = re.search(
        rb'"' + re.escape(field.encode("ascii"))
        + rb'"\s*:\s*("(?:\\.|[^"\\])*")',
        header,
    )
    if match is None:
        raise ValueError(f"map header has no JSON string field {field}")
    return json.loads(match.group(1))


def _json_integer(header: bytes, field: str) -> int:
    matches = list(re.finditer(
        rb'"' + re.escape(field.encode("ascii")) + rb'"\s*:\s*(-?\d+)',
        header,
    ))
    if not matches:
        raise ValueError(f"map header has no integer field {field}")
    return int(matches[-1].group(1))


def _count_recipe_values(data: bytes, state: dict[str, Any]) -> None:
    for byte in data:
        if state["closed"]:
            return
        if state["in_string"]:
            if state["escape"]:
                state["escape"] = False
            elif byte == 0x5C:
                state["escape"] = True
            elif byte == 0x22:
                state["in_string"] = False
            continue
        if byte == 0x22:
            state["in_string"] = True
            if state["depth"] == 0 and state["expect_value"]:
                state["count"] += 1
                state["expect_value"] = False
        elif byte in {0x7B, 0x5B}:
            if state["depth"] == 0 and state["expect_value"]:
                state["count"] += 1
                state["expect_value"] = False
            state["depth"] += 1
        elif byte in {0x7D, 0x5D}:
            if byte == 0x5D and state["depth"] == 0:
                state["closed"] = True
                continue
            state["depth"] -= 1
            if state["depth"] < 0:
                raise ValueError("recipe array nesting became negative")
        elif byte == 0x2C and state["depth"] == 0:
            state["expect_value"] = True
        elif byte not in {0x20, 0x09, 0x0A, 0x0D}:
            if state["depth"] == 0 and state["expect_value"]:
                state["count"] += 1
                state["expect_value"] = False


def scan_map_document(path: Path) -> dict[str, Any]:
    digest = hashlib.sha256()
    prefix = bytearray()
    marker_end: int | None = None
    state = {
        "closed": False,
        "count": 0,
        "depth": 0,
        "escape": False,
        "expect_value": True,
        "in_string": False,
    }
    with path.open("rb") as handle:
        while chunk := handle.read(1024 * 1024):
            digest.update(chunk)
            if marker_end is None:
                prefix.extend(chunk)
                marker = re.search(rb'"recipes"\s*:\s*\[', prefix)
                if marker is None:
                    if len(prefix) > 16 * 1024 * 1024:
                        raise ValueError(f"{path}: recipes array marker not found")
                    continue
                marker_end = marker.end()
                _count_recipe_values(bytes(prefix[marker_end:]), state)
            else:
                _count_recipe_values(chunk, state)
    if marker_end is None or not state["closed"] or state["depth"] != 0:
        raise ValueError(f"{path}: recipes array is missing or unterminated")
    header = bytes(prefix[:marker_end])
    return {
        "name_internal": _json_string(header, "nameInternal"),
        "name_local": _json_string(header, "nameLocal"),
        "declared_recipe_count": _json_integer(header, "recipeCount"),
        "actual_recipe_count": state["count"],
        "input_items": _json_integer(header, "inputItemsCount"),
        "output_items": _json_integer(header, "outputItemsCount"),
        "input_fluids": _json_integer(header, "inputFluidCount"),
        "output_fluids": _json_integer(header, "outputFluidCount"),
        "dump_sha256": digest.hexdigest(),
        "dump_size": path.stat().st_size,
    }


def scan_recipe_maps(dump_root: Path = DUMP_ROOT) -> list[dict[str, Any]]:
    maps_root = dump_root / "maps"
    paths = sorted(maps_root.glob("*.json"), key=lambda path: path.name)
    if len(paths) != EXPECTED_MAP_COUNT:
        raise ValueError(
            f"expected {EXPECTED_MAP_COUNT} maps/*.json files, found "
            f"{len(paths)}"
        )
    rows: list[dict[str, Any]] = []
    for path in paths:
        scanned = scan_map_document(path)
        if scanned["declared_recipe_count"] != scanned["actual_recipe_count"]:
            raise ValueError(
                f"{path.name}: declared {scanned['declared_recipe_count']} "
                f"recipes, scanned {scanned['actual_recipe_count']}"
            )
        rows.append({
            "file": f"maps/{path.name}",
            **scanned,
        })
    return rows


def map_row_digest(rows: list[dict[str, Any]]) -> str:
    digest_rows = [
        {
            "actual_recipe_count": row["actual_recipe_count"],
            "declared_recipe_count": row["declared_recipe_count"],
            "dump_sha256": row["dump_sha256"],
            "dump_size": row["dump_size"],
            "file": row["file"],
            "input_fluids": row["input_fluids"],
            "input_items": row["input_items"],
            "name_internal": row["name_internal"],
            "name_local": row["name_local"],
            "output_fluids": row["output_fluids"],
            "output_items": row["output_items"],
        }
        for row in sorted(rows, key=lambda value: value["file"])
    ]
    return canonical_digest(digest_rows)


def map_tree_digest(rows: list[dict[str, Any]]) -> str:
    return canonical_digest([
        {
            "file": row["file"],
            "sha256": row["dump_sha256"],
            "size": row["dump_size"],
        }
        for row in sorted(rows, key=lambda value: value["file"])
    ])


def full_replay_receipt(
    scanned: list[dict[str, Any]],
    index_path: Path,
) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "proof_tier": "full_replay",
        "scanner_semantics": "python-byte-state-machine-v1",
        "builder_sha256": sha256(Path(__file__).resolve()),
        "policy_sha256": sha256(POLICY),
        "tree_manifest_sha256": sha256(TREE_MANIFEST),
        "source_symbol_inventory_sha256": sha256(SYMBOL_INVENTORY),
        "dump_index_sha256": sha256(index_path),
        "legacy_roadmap_sha256": sha256(LEGACY_ROADMAP),
        "map_tree_sha256": map_tree_digest(scanned),
        "map_row_sha256": map_row_digest(scanned),
        "map_count": len(scanned),
        "recipe_count": sum(
            row["actual_recipe_count"] for row in scanned
        ),
    }


def validate_index_and_expected_set(
    index: dict[str, Any],
    scanned: list[dict[str, Any]],
    policy: dict[str, Any],
) -> None:
    index_rows = index.get("maps") or []
    if (
        index.get("mapCount") != EXPECTED_MAP_COUNT
        or len(index_rows) != EXPECTED_MAP_COUNT
        or index.get("recipeCount") != EXPECTED_RECIPE_COUNT
    ):
        raise ValueError("GT6 recipe index denominator totals drifted")
    if len(scanned) != EXPECTED_MAP_COUNT:
        raise ValueError("actual maps/*.json denominator count drifted")
    by_file = {row["file"]: row for row in scanned}
    if len(by_file) != len(scanned):
        raise ValueError("actual map files are duplicated")
    index_files = [row["file"] for row in index_rows]
    if len(index_files) != len(set(index_files)):
        raise ValueError("index map files are duplicated")
    if set(index_files) != set(by_file):
        raise ValueError(
            "index/actual map file set differs: "
            f"missing={sorted(set(index_files) - set(by_file))}, "
            f"extra={sorted(set(by_file) - set(index_files))}"
        )

    source_names: list[str] = []
    for index_row in index_rows:
        actual = by_file[index_row["file"]]
        for index_field, actual_field in (
            ("nameInternal", "name_internal"),
            ("nameLocal", "name_local"),
            ("recipeCount", "actual_recipe_count"),
            ("inputItems", "input_items"),
            ("outputItems", "output_items"),
            ("inputFluids", "input_fluids"),
            ("outputFluids", "output_fluids"),
        ):
            if index_row[index_field] != actual[actual_field]:
                raise ValueError(
                    f"{index_row['file']}: index {index_field} differs "
                    "from independently scanned map"
                )
        source_names.append(index_row["nameInternal"])
    if len(source_names) != len(set(source_names)):
        raise ValueError("source nameInternal values are not unique")

    normalized = [normalize_map_id(name) for name in source_names]
    if len(normalized) != len(set(normalized)):
        raise ValueError("recipe-map normalization is many-to-one")
    expected = set(policy.get("dispositions") or {})
    if set(normalized) != expected:
        raise ValueError(
            "normalizer output and independent policy expected set differ: "
            f"missing={sorted(expected - set(normalized))}, "
            f"extra={sorted(set(normalized) - expected)}"
        )
    if "unnamed" not in expected or "" not in source_names:
        raise ValueError("empty/unnamed recipe-map identity is not covered")
    recipe_sum = sum(row["actual_recipe_count"] for row in scanned)
    if recipe_sum != EXPECTED_RECIPE_COUNT:
        raise ValueError(
            f"actual map recipe sum is {recipe_sum}, "
            f"expected {EXPECTED_RECIPE_COUNT}"
        )
    expected_digest = policy["source"]["expected_map_row_sha256"]
    actual_digest = map_row_digest(scanned)
    if actual_digest != expected_digest:
        raise ValueError(
            f"stable map-row digest drifted: {actual_digest} != "
            f"{expected_digest}"
        )


def validate_dispositions(policy: dict[str, Any]) -> Counter[str]:
    counts: Counter[str] = Counter()
    for identity, disposition in policy["dispositions"].items():
        classification = disposition.get("classification")
        if classification not in CLASSIFICATIONS:
            raise ValueError(
                f"{identity}: unknown classification {classification!r}"
            )
        counts[classification] += 1
        if classification == "unclassified":
            raise ValueError(f"{identity}: unclassified blocks T13a")
        if classification == "deferred_with_reason":
            for field in DEFERRED_FIELDS:
                if not isinstance(disposition.get(field), str) or not (
                    disposition[field].strip()
                ):
                    raise ValueError(
                        f"{identity}: deferred row lacks {field}"
                    )
    if sum(counts.values()) != EXPECTED_MAP_COUNT:
        raise ValueError("recipe-map policy does not classify exactly 95 maps")
    return counts


def _nearest_rank(values: list[int], percentile: float) -> int:
    ordered = sorted(values)
    rank = max(1, math.ceil(percentile * len(ordered)))
    return ordered[rank - 1]


def _uniform_audit(
    source_names: list[str],
    normalized_names: list[str],
    policy: dict[str, Any],
) -> dict[str, Any]:
    collisions: dict[str, list[str]] = {}
    for source, normalized in zip(source_names, normalized_names):
        collisions.setdefault(normalized, []).append(source)
    many_to_one = {
        key: values
        for key, values in collisions.items()
        if len(values) > 1
    }
    before = len(set(source_names))
    after = len(set(normalized_names))
    unexplained = []
    if after < before and not many_to_one:
        unexplained.append({
            "field": "nameInternal",
            "before": before,
            "after": after,
        })
    status = (
        "UNIFORM_FAIL"
        if many_to_one or unexplained
        else "UNIFORM_PASS"
    )
    return {
        "schema_version": 1,
        "status": status,
        "status_prefix": policy["uniform_audit_schema"]["status_prefix"],
        "input_rows": len(source_names),
        "output_rows": len(normalized_names),
        "field_cardinality": {
            "nameInternal": {
                "before": before,
                "after": after,
                "reduction": before - after,
            }
        },
        "declared_transformations": policy["normalization"][
            "declared_transformations"
        ],
        "undeclared_many_to_one": many_to_one,
        "unexplained_field_cardinality_reductions": unexplained,
        "frequency_threshold_is_diagnostic_only": True,
    }


def build(
    dump_root: Path = DUMP_ROOT,
    scanned: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    manifest, inventory = validate_committed_source_evidence()
    policy = load(POLICY)
    if (
        policy.get("source", {}).get("revision") != REVISION
        or policy["source"].get("tree_sha1") != TREE_SHA1
    ):
        raise ValueError("T13 recipe-map policy source pin drifted")
    disposition_counts = validate_dispositions(policy)
    index_path = dump_root / "index.json"
    index = load(index_path)
    scanned = scan_recipe_maps(dump_root) if scanned is None else scanned
    validate_index_and_expected_set(index, scanned, policy)

    legacy = load(LEGACY_ROADMAP)
    supersession = policy["supersession"]
    if (
        len(legacy.get("maps") or {}) != EXPECTED_MAP_COUNT
        or supersession["historical_artifact"]
        != LEGACY_ROADMAP.relative_to(ROOT).as_posix()
        or supersession["historical_phase_owner"] != "POST_T3"
        or not supersession["history_preserved"]
    ):
        raise ValueError("gt6_map_roadmap POST_T3 supersession drifted")

    scanned_by_file = {row["file"]: row for row in scanned}
    rows: list[dict[str, Any]] = []
    source_names: list[str] = []
    normalized_names: list[str] = []
    for index_position, index_row in enumerate(index["maps"]):
        source_name = index_row["nameInternal"]
        normalized = normalize_map_id(source_name)
        scanned_row = scanned_by_file[index_row["file"]]
        source_names.append(source_name)
        normalized_names.append(normalized)
        rows.append({
            "source_revision": REVISION,
            "source_blob": scanned_row["dump_sha256"],
            "source_blob_algorithm": "sha256",
            "source_path": (
                f"gt6_dump/gt6_recipe_dump/{index_row['file']}"
            ),
            "source_symbol_or_extraction_key": source_name,
            "normalized_row_key": normalized,
            "index_position": index_position,
            "name_internal": source_name,
            "name_local": scanned_row["name_local"],
            "recipe_count": scanned_row["actual_recipe_count"],
            "dump_size": scanned_row["dump_size"],
            "capacity": {
                "input_items": scanned_row["input_items"],
                "output_items": scanned_row["output_items"],
                "input_fluids": scanned_row["input_fluids"],
                "output_fluids": scanned_row["output_fluids"],
            },
            **policy["dispositions"][normalized],
        })
    recipe_counts = [row["recipe_count"] for row in rows]
    audit = _uniform_audit(source_names, normalized_names, policy)
    if audit["status"] != "UNIFORM_PASS":
        raise ValueError("T13 recipe-map normalization audit failed")
    return {
        "schema_version": 1,
        "status": "T13A_RECIPE_MAP_DENOMINATOR_READY",
        "full_t13_closure_claimed": False,
        "source": {
            "repository": REPOSITORY,
            "revision": REVISION,
            "tree_sha1": TREE_SHA1,
            "tree_manifest": TREE_MANIFEST.relative_to(ROOT).as_posix(),
            "tree_manifest_entries": len(manifest["entries"]),
            "source_symbol_inventory": (
                SYMBOL_INVENTORY.relative_to(ROOT).as_posix()
            ),
            "source_declarations": inventory["counts"]["declarations"],
            "recipe_dump_index": (
                index_path.relative_to(ROOT).as_posix()
            ),
        },
        "independent_expected_set": {
            "carrier": POLICY.relative_to(ROOT).as_posix()
            + "#dispositions",
            "derived_by_normalizer": False,
            "count": len(policy["dispositions"]),
            "sha256": canonical_digest(sorted(policy["dispositions"])),
        },
        "normalization": policy["normalization"],
        "normalization_audit": audit,
        "counts": {
            "index_maps": len(index["maps"]),
            "actual_map_files": len(scanned),
            "rows": len(rows),
            "recipes": sum(recipe_counts),
            "classified": len(rows) - disposition_counts["unclassified"],
            "unclassified": disposition_counts["unclassified"],
            "classifications": {
                name: disposition_counts[name]
                for name in sorted(CLASSIFICATIONS)
            },
        },
        "recipe_count_distribution": {
            "method": "nearest-rank over all 95 maps, including zero/unnamed",
            "p50": _nearest_rank(recipe_counts, 0.50),
            "p95": _nearest_rank(recipe_counts, 0.95),
            "max": max(recipe_counts),
        },
        "map_row_sha256": map_row_digest(scanned),
        "full_replay_receipt": full_replay_receipt(scanned, index_path),
        "supersession": {
            **supersession,
            "historical_artifact_sha256": sha256(LEGACY_ROADMAP),
        },
        "rows": rows,
        "source_hashes": {
            "builder": sha256(Path(__file__).resolve()),
            "policy": sha256(POLICY),
            "tree_manifest": sha256(TREE_MANIFEST),
            "source_symbol_inventory": sha256(SYMBOL_INVENTORY),
            "dump_index": sha256(index_path),
            "legacy_roadmap": sha256(LEGACY_ROADMAP),
            "map_tree": map_tree_digest(scanned),
        },
    }


def scanned_from_committed(document: dict[str, Any]) -> list[dict[str, Any]]:
    scanned: list[dict[str, Any]] = []
    for row in document.get("rows") or []:
        capacity = row.get("capacity") or {}
        source_path = str(row.get("source_path") or "")
        scanned.append({
            "file": f"maps/{Path(source_path).name}",
            "name_internal": row.get("name_internal"),
            "name_local": row.get("name_local"),
            "declared_recipe_count": row.get("recipe_count"),
            "actual_recipe_count": row.get("recipe_count"),
            "input_items": capacity.get("input_items"),
            "output_items": capacity.get("output_items"),
            "input_fluids": capacity.get("input_fluids"),
            "output_fluids": capacity.get("output_fluids"),
            "dump_sha256": row.get("source_blob"),
            "dump_size": row.get("dump_size"),
        })
    return scanned


def validate_compact_document(document: dict[str, Any]) -> None:
    manifest, inventory = validate_committed_source_evidence()
    policy = load(POLICY)
    disposition_counts = validate_dispositions(policy)
    legacy = load(LEGACY_ROADMAP)
    denominator_manifest = load(DENOMINATOR_MANIFEST)
    manifest_row = denominator_manifest["tables"]["recipe_maps"]

    if (
        document.get("schema_version") != 1
        or document.get("status") != "T13A_RECIPE_MAP_DENOMINATOR_READY"
        or document.get("full_t13_closure_claimed") is not False
    ):
        raise ValueError("committed T13 recipe-map artifact header drifted")
    rows = document.get("rows") or []
    if len(rows) != EXPECTED_MAP_COUNT:
        raise ValueError("committed T13 recipe-map row count drifted")
    positions = [row.get("index_position") for row in rows]
    if positions != list(range(EXPECTED_MAP_COUNT)):
        raise ValueError("committed T13 recipe-map positions drifted")
    normalized = [row.get("normalized_row_key") for row in rows]
    source_names = [row.get("name_internal") for row in rows]
    if (
        len(set(normalized)) != EXPECTED_MAP_COUNT
        or set(normalized) != set(policy["dispositions"])
        or any(
            normalize_map_id(source) != identity
            for source, identity in zip(source_names, normalized)
        )
    ):
        raise ValueError("committed T13 recipe-map identities drifted")
    expected_prefix = "gt6_dump/gt6_recipe_dump/maps/"
    for row in rows:
        identity = row["normalized_row_key"]
        if (
            row.get("source_revision") != REVISION
            or row.get("source_symbol_or_extraction_key")
            != row.get("name_internal")
            or not str(row.get("source_path") or "").startswith(expected_prefix)
            or row.get("source_blob_algorithm") != "sha256"
            or re.fullmatch(r"[0-9a-f]{64}", str(row.get("source_blob") or ""))
            is None
            or not isinstance(row.get("dump_size"), int)
            or row["dump_size"] < 0
            or not isinstance(row.get("recipe_count"), int)
            or row["recipe_count"] < 0
        ):
            raise ValueError(f"invalid committed T13 recipe-map row: {identity}")
        disposition = policy["dispositions"][identity]
        if any(row.get(key) != value for key, value in disposition.items()):
            raise ValueError(f"{identity}: committed disposition drifted")

    scanned = scanned_from_committed(document)
    actual_row_digest = map_row_digest(scanned)
    actual_tree_digest = map_tree_digest(scanned)
    expected_row_digest = policy["source"]["expected_map_row_sha256"]
    receipt = document.get("full_replay_receipt") or {}
    if (
        actual_row_digest != expected_row_digest
        or document.get("map_row_sha256") != actual_row_digest
        or receipt.get("map_row_sha256") != actual_row_digest
        or receipt.get("map_tree_sha256") != actual_tree_digest
        or receipt.get("map_count") != EXPECTED_MAP_COUNT
        or receipt.get("recipe_count") != EXPECTED_RECIPE_COUNT
        or receipt.get("proof_tier") != "full_replay"
        or receipt.get("scanner_semantics") != "python-byte-state-machine-v1"
    ):
        raise ValueError("committed T13 full-replay receipt drifted")
    external_receipt = manifest_row.get("full_replay_receipt")
    if external_receipt != receipt:
        raise ValueError("T13 manifest does not anchor the full-replay receipt")
    if manifest_row.get("artifact_sha256") != sha256(OUTPUT):
        raise ValueError("T13 manifest recipe-map artifact hash drifted")

    expected_hashes = {
        "builder": sha256(Path(__file__).resolve()),
        "policy": sha256(POLICY),
        "tree_manifest": sha256(TREE_MANIFEST),
        "source_symbol_inventory": sha256(SYMBOL_INVENTORY),
        "legacy_roadmap": sha256(LEGACY_ROADMAP),
        "map_tree": actual_tree_digest,
        "dump_index": receipt.get("dump_index_sha256"),
    }
    if document.get("source_hashes") != expected_hashes:
        raise ValueError("committed T13 input hashes drifted")
    receipt_hashes = {
        "builder_sha256": expected_hashes["builder"],
        "policy_sha256": expected_hashes["policy"],
        "tree_manifest_sha256": expected_hashes["tree_manifest"],
        "source_symbol_inventory_sha256": expected_hashes[
            "source_symbol_inventory"
        ],
        "legacy_roadmap_sha256": expected_hashes["legacy_roadmap"],
    }
    if any(receipt.get(key) != value for key, value in receipt_hashes.items()):
        raise ValueError("T13 full-replay receipt input closure drifted")

    recipe_counts = [row["recipe_count"] for row in rows]
    classifications = Counter(
        row["classification"] for row in rows
    )
    expected_counts = {
        "index_maps": EXPECTED_MAP_COUNT,
        "actual_map_files": EXPECTED_MAP_COUNT,
        "rows": EXPECTED_MAP_COUNT,
        "recipes": sum(recipe_counts),
        "classified": EXPECTED_MAP_COUNT - disposition_counts["unclassified"],
        "unclassified": disposition_counts["unclassified"],
        "classifications": {
            name: disposition_counts[name]
            for name in sorted(CLASSIFICATIONS)
        },
    }
    if (
        expected_counts["recipes"] != EXPECTED_RECIPE_COUNT
        or document.get("counts") != expected_counts
        or classifications != disposition_counts
    ):
        raise ValueError("committed T13 counts drifted")
    expected_distribution = {
        "method": "nearest-rank over all 95 maps, including zero/unnamed",
        "p50": _nearest_rank(recipe_counts, 0.50),
        "p95": _nearest_rank(recipe_counts, 0.95),
        "max": max(recipe_counts),
    }
    if document.get("recipe_count_distribution") != expected_distribution:
        raise ValueError("committed T13 recipe distribution drifted")
    audit = _uniform_audit(source_names, normalized, policy)
    if audit["status"] != "UNIFORM_PASS" or document.get(
        "normalization_audit"
    ) != audit:
        raise ValueError("committed T13 normalization audit drifted")
    if document.get("normalization") != policy["normalization"]:
        raise ValueError("committed T13 normalization policy drifted")
    if document.get("independent_expected_set") != {
        "carrier": POLICY.relative_to(ROOT).as_posix() + "#dispositions",
        "derived_by_normalizer": False,
        "count": EXPECTED_MAP_COUNT,
        "sha256": canonical_digest(sorted(policy["dispositions"])),
    }:
        raise ValueError("committed T13 independent expected set drifted")
    supersession = policy["supersession"]
    if (
        len(legacy.get("maps") or {}) != EXPECTED_MAP_COUNT
        or document.get("supersession") != {
            **supersession,
            "historical_artifact_sha256": sha256(LEGACY_ROADMAP),
        }
    ):
        raise ValueError("committed T13 supersession drifted")
    source = document.get("source") or {}
    if (
        source.get("revision") != REVISION
        or source.get("tree_sha1") != TREE_SHA1
        or source.get("tree_manifest_entries") != len(manifest["entries"])
        or source.get("source_declarations")
        != inventory["counts"]["declarations"]
    ):
        raise ValueError("committed T13 source summary drifted")


def reference_only_check() -> list[str]:
    if not OUTPUT.is_file():
        return [f"missing generated file: {OUTPUT.relative_to(ROOT).as_posix()}"]
    try:
        document = load(OUTPUT)
        if OUTPUT.read_text(encoding="utf-8") != stable_json(document):
            raise ValueError("committed T13 artifact is not canonical JSON")
        validate_compact_document(document)
    except (KeyError, OSError, TypeError, ValueError, json.JSONDecodeError) as exc:
        return [str(exc)]
    return []


def validate_dump_hashes(
    document: dict[str, Any],
    dump_root: Path,
) -> None:
    receipt = document["full_replay_receipt"]
    maps_root = dump_root / "maps"
    paths = sorted(maps_root.glob("*.json"), key=lambda path: path.name)
    if len(paths) != EXPECTED_MAP_COUNT:
        raise ValueError(
            f"expected {EXPECTED_MAP_COUNT} maps/*.json files, found "
            f"{len(paths)}"
        )
    current = [
        {
            "file": f"maps/{path.name}",
            "dump_sha256": sha256(path),
            "dump_size": path.stat().st_size,
        }
        for path in paths
    ]
    committed = {
        row["file"]: row for row in scanned_from_committed(document)
    }
    for row in current:
        expected = committed.get(row["file"])
        if expected is None or (
            row["dump_sha256"],
            row["dump_size"],
        ) != (
            expected["dump_sha256"],
            expected["dump_size"],
        ):
            raise ValueError(f"{row['file']}: dump hash/size drifted")
    current_tree = canonical_digest([
        {
            "file": row["file"],
            "sha256": row["dump_sha256"],
            "size": row["dump_size"],
        }
        for row in current
    ])
    if current_tree != receipt["map_tree_sha256"]:
        raise ValueError("T13 dump map tree digest drifted")
    index_path = dump_root / "index.json"
    if sha256(index_path) != receipt["dump_index_sha256"]:
        raise ValueError("T13 dump index digest drifted")


def hash_fast_check(dump_root: Path = DUMP_ROOT) -> list[str]:
    errors = reference_only_check()
    if errors:
        return errors
    try:
        validate_dump_hashes(load(OUTPUT), dump_root)
    except (KeyError, OSError, TypeError, ValueError) as exc:
        return [str(exc)]
    return []


def full_replay_check(dump_root: Path = DUMP_ROOT) -> list[str]:
    encoded = stable_json(build(dump_root))
    if not OUTPUT.is_file():
        return [
            f"missing generated file: {OUTPUT.relative_to(ROOT).as_posix()}"
        ]
    if OUTPUT.read_text(encoding="utf-8") != encoded:
        return [
            f"stale generated file: {OUTPUT.relative_to(ROOT).as_posix()}"
        ]
    return []


def check(dump_root: Path = DUMP_ROOT) -> list[str]:
    return full_replay_check(dump_root)


def write(dump_root: Path = DUMP_ROOT) -> dict[str, Any]:
    document = build(dump_root)
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(
        stable_json(document),
        encoding="utf-8",
        newline="\n",
    )
    return document


def verify_inventory_replay(source_root: Path) -> dict[str, Any]:
    manifest = load(TREE_MANIFEST)
    replay = build_symbol_inventory(source_root, manifest)
    committed = load(SYMBOL_INVENTORY)
    if replay != committed:
        raise ValueError(
            "fixed-source replay does not match committed "
            "t13_source_symbol_inventory.json"
        )
    return replay


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--check",
        action="store_true",
        help="fail if the committed T13a denominator is stale",
    )
    check_mode = parser.add_mutually_exclusive_group()
    check_mode.add_argument(
        "--reference-only",
        action="store_true",
        help="validate committed compact evidence without reading gt6_dump",
    )
    check_mode.add_argument(
        "--hash-fast",
        action="store_true",
        help="validate compact evidence plus exact dump file SHA-256 values",
    )
    check_mode.add_argument(
        "--full-replay",
        action="store_true",
        help="force the full independent recipe-array replay",
    )
    parser.add_argument(
        "--refresh-tree-manifest",
        action="store_true",
        help="query the official GitHub APIs and rewrite the fixed tree manifest",
    )
    parser.add_argument(
        "--fetch-source",
        nargs="?",
        type=Path,
        const=DEFAULT_SOURCE_ROOT,
        help=(
            "download the pinned source archive into PATH (default "
            "build/t13-gt6-source), verify every selected Git blob, and "
            "replay the committed symbol inventory"
        ),
    )
    parser.add_argument(
        "--verify-source",
        nargs="?",
        type=Path,
        const=DEFAULT_SOURCE_ROOT,
        help=(
            "verify every selected blob under PATH (default "
            "build/t13-gt6-source) and replay the symbol inventory"
        ),
    )
    parser.add_argument(
        "--write-source-inventory",
        nargs="?",
        type=Path,
        const=DEFAULT_SOURCE_ROOT,
        help="rewrite the normalized symbol inventory from verified source",
    )
    args = parser.parse_args()
    if (
        args.reference_only or args.hash_fast or args.full_replay
    ) and not args.check:
        parser.error(
            "--reference-only, --hash-fast and --full-replay require --check"
        )
    evidence_actions = sum((
        bool(args.refresh_tree_manifest),
        args.fetch_source is not None,
        args.write_source_inventory is not None,
    ))
    if evidence_actions > 1:
        parser.error("source evidence write actions are mutually exclusive")

    if args.refresh_tree_manifest:
        manifest = fetch_tree_manifest()
        TREE_MANIFEST.write_text(
            stable_json(manifest),
            encoding="utf-8",
            newline="\n",
        )
        counts = manifest["integrity"]
        print(
            "T13 fixed tree manifest: "
            f"{counts['selected_tree']['entries']} selected Java blobs / "
            f"{counts['raw_tree']['entries']} raw entries"
        )
        return 0

    if args.fetch_source is not None:
        manifest = load(TREE_MANIFEST)
        result = fetch_source_tree(args.fetch_source, manifest)
        if SYMBOL_INVENTORY.is_file():
            verify_inventory_replay(args.fetch_source)
        print(
            "Fetched and verified T13 fixed source: "
            f"{result['files']} blobs, {result['bytes']} bytes under "
            f"{args.fetch_source}"
        )
        return 0

    if args.write_source_inventory is not None:
        manifest = load(TREE_MANIFEST)
        inventory = build_symbol_inventory(
            args.write_source_inventory,
            manifest,
        )
        SYMBOL_INVENTORY.write_text(
            stable_json(inventory),
            encoding="utf-8",
            newline="\n",
        )
        print(
            "T13 source inventory: "
            f"{inventory['counts']['declarations']} declarations, "
            f"{inventory['counts']['registration_symbols']} registrations"
        )
        return 0

    if args.verify_source is not None:
        inventory = verify_inventory_replay(args.verify_source)
        print(
            "Verified T13 source replay: "
            f"{inventory['counts']['source_files']} blobs, "
            f"{inventory['counts']['declarations']} declarations"
        )
        if not args.check:
            return 0

    if args.check:
        if args.reference_only:
            errors = reference_only_check()
            tier = "compact reference"
        elif args.hash_fast:
            errors = hash_fast_check()
            tier = "hash-fast"
        else:
            errors = full_replay_check()
            tier = "full replay"
        if errors:
            print("T13a recipe-map denominator is stale:")
            for error in errors:
                print(f"- {error}")
            return 1
        print(
            "T13a recipe-map denominator matches committed "
            f"{tier} evidence."
        )
        return 0

    document = write()
    distribution = document["recipe_count_distribution"]
    print(
        "T13a recipe maps: "
        f"{document['counts']['rows']} maps / "
        f"{document['counts']['recipes']} recipes; "
        f"p50={distribution['p50']}, p95={distribution['p95']}, "
        f"max={distribution['max']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
