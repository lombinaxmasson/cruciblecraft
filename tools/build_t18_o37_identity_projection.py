#!/usr/bin/env python3
"""Replay the fixed GT6 Java tree and close O-37 without name guessing."""
from __future__ import annotations

import argparse
import hashlib
import io
import json
import re
import zipfile
from pathlib import Path
from typing import Any
from urllib.request import Request, urlopen


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
BUILDER = Path(__file__).resolve()
POLICY = TOOLS / "t18_o37_identity_policy.json"
TREE_MANIFEST = TOOLS / "t13_gt6_tree_manifest.json"
OUTPUT = TOOLS / "t18_o37_identity_projection.json"


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git_blob_sha1(data: bytes) -> str:
    return hashlib.sha1(
        f"blob {len(data)}\0".encode("ascii") + data
    ).hexdigest()


def java_entries() -> list[dict[str, Any]]:
    policy = load(POLICY)
    manifest = load(TREE_MANIFEST)
    if (
        policy.get("schema_version") != 1
        or policy.get("status") != "T18_O37_IDENTITY_POLICY"
        or manifest.get("revision") != policy["source"]["revision"]
        or manifest.get("repository") != policy["source"]["repository"]
    ):
        raise ValueError("O-37 policy or fixed tree header drifted")
    entries = manifest.get("entries") or []
    if (
        not entries
        or any(
            row.get("type") != "blob"
            or not row.get("path", "").startswith("src/main/java/")
            or not row["path"].endswith(".java")
            for row in entries
        )
    ):
        raise ValueError("O-37 search scope is not the complete Java manifest")
    return entries


def _download_sources(
    entries: list[dict[str, Any]],
    revision: str,
) -> dict[str, bytes]:
    url = (
        "https://codeload.github.com/GregTech6/gregtech6/zip/"
        + revision
    )
    request = Request(
        url,
        headers={"User-Agent": "CrucibleCraft-T18-O37-source-replay"},
    )
    with urlopen(request, timeout=120) as response:
        archive = zipfile.ZipFile(io.BytesIO(response.read()))
    names = archive.namelist()
    sources: dict[str, bytes] = {}
    for entry in entries:
        suffix = "/" + entry["path"]
        matches = [name for name in names if name.endswith(suffix)]
        if len(matches) != 1:
            raise ValueError(
                f"{entry['path']}: fixed archive match count {len(matches)}"
            )
        sources[entry["path"]] = archive.read(matches[0])
    return sources


def replay_sources(
    source_root: Path | None = None,
) -> dict[str, bytes]:
    entries = java_entries()
    if source_root is None:
        sources = _download_sources(
            entries, load(POLICY)["source"]["revision"]
        )
    else:
        sources = {}
        for entry in entries:
            path = source_root / entry["path"]
            if not path.is_file():
                raise ValueError(
                    f"fixed source root is missing {entry['path']}"
                )
            sources[entry["path"]] = path.read_bytes()
    if set(sources) != {row["path"] for row in entries}:
        raise ValueError("fixed source replay did not cover every Java blob")
    by_path = {row["path"]: row for row in entries}
    mismatches = [
        path
        for path, data in sources.items()
        if git_blob_sha1(data) != by_path[path]["sha"]
    ]
    if mismatches:
        raise ValueError(
            "fixed source blob mismatch: " + ", ".join(sorted(mismatches))
        )
    return sources


def _without_comments(source: str) -> str:
    def preserve_newlines(match: re.Match[str]) -> str:
        return "".join(
            "\n" if character == "\n" else " "
            for character in match.group(0)
        )

    return re.sub(
        r"//[^\n]*|/\*.*?\*/",
        preserve_newlines,
        source,
        flags=re.DOTALL,
    )


def _location(
    path: str,
    blob: str,
    source: str,
    offset: int,
    symbol: str,
) -> dict[str, Any]:
    line = source.count("\n", 0, offset) + 1
    lines = source.splitlines()
    snippet = lines[line - 1].strip() if line <= len(lines) else ""
    return {
        "path": path,
        "blob": blob,
        "line": line,
        "symbol": symbol,
        "snippet": snippet,
    }


def symbol_evidence(
    policy: dict[str, Any],
    sources: dict[str, bytes],
    blobs: dict[str, str],
) -> list[dict[str, Any]]:
    evidence: list[dict[str, Any]] = []
    for expected in policy["required_symbol_evidence"]:
        path = expected["path"]
        if blobs.get(path) != expected["blob"]:
            raise ValueError(f"{path}: policy blob does not match tree")
        source = sources[path].decode("utf-8")
        match = re.search(expected["pattern"], source)
        if match is None:
            raise ValueError(
                f"{path}: required symbol evidence did not replay"
            )
        evidence.append(
            _location(
                path,
                expected["blob"],
                source,
                match.start(),
                expected["symbol"],
            )
        )
    return evidence


def token_inventory(
    policy: dict[str, Any],
    sources: dict[str, bytes],
    blobs: dict[str, str],
) -> dict[str, Any]:
    search = policy["direct_binding_search"]
    tokens = search["fluid_tokens"] + search["material_tokens"]
    result: dict[str, Any] = {}
    for token in tokens:
        locations: list[dict[str, Any]] = []
        count = 0
        for path, data in sources.items():
            source = data.decode("utf-8")
            for match in re.finditer(re.escape(token), source):
                count += 1
                if len(locations) < 25:
                    locations.append(
                        _location(
                            path,
                            blobs[path],
                            source,
                            match.start(),
                            token,
                        )
                    )
        result[token] = {
            "occurrence_count": count,
            "recorded_locations": locations,
            "recorded_location_limit": 25,
        }
    return result


def direct_binding_candidates(
    policy: dict[str, Any],
    sources: dict[str, bytes],
    blobs: dict[str, str],
) -> list[dict[str, Any]]:
    search = policy["direct_binding_search"]
    fluid_tokens = search["fluid_tokens"]
    material_tokens = search["material_tokens"]
    candidates: list[dict[str, Any]] = []
    seen: set[tuple[str, int, str]] = set()
    for path, data in sources.items():
        source = data.decode("utf-8")
        clean = _without_comments(source)
        start = 0
        for statement in clean.split(";")[:-1]:
            end = start + len(statement) + 1
            if (
                any(token in statement for token in fluid_tokens)
                and any(token in statement for token in material_tokens)
            ):
                compact = " ".join(statement.split()) + ";"
                line = clean.count("\n", 0, start) + 1
                key = (path, line, compact)
                if key not in seen:
                    seen.add(key)
                    candidates.append({
                        "path": path,
                        "blob": blobs[path],
                        "line": line,
                        "rule": "fluid_and_material_token_same_statement",
                        "snippet": compact[:500],
                    })
            start = end
        for pattern in search["material_fluid_patterns"]:
            for match in re.finditer(pattern, clean):
                line = clean.count("\n", 0, match.start()) + 1
                snippet = clean.splitlines()[line - 1].strip()
                key = (path, line, snippet)
                if key not in seen:
                    seen.add(key)
                    candidates.append({
                        "path": path,
                        "blob": blobs[path],
                        "line": line,
                        "rule": "material_fluid_expression",
                        "snippet": snippet[:500],
                    })
    return sorted(
        candidates,
        key=lambda row: (row["path"], row["line"], row["rule"]),
    )


def build(sources: dict[str, bytes]) -> dict[str, Any]:
    policy = load(POLICY)
    entries = java_entries()
    blobs = {row["path"]: row["sha"] for row in entries}
    expected_paths = set(blobs)
    if set(sources) != expected_paths:
        raise ValueError("O-37 replay source set is incomplete")
    evidence = symbol_evidence(policy, sources, blobs)
    inventory = token_inventory(policy, sources, blobs)
    candidates = direct_binding_candidates(policy, sources, blobs)
    resolution = "SOURCE_BACKED" if candidates else "DESIGN_POLICY"
    if resolution != policy["decision"]["status"]:
        raise ValueError(
            "O-37 direct-binding result changed; update the policy "
            "instead of guessing from names"
        )
    lock = policy["runtime_identity_lock"]
    if (
        resolution != "DESIGN_POLICY"
        or policy["decision"]["closure"]
        != "O37_CLOSED_PERMANENT_DESIGN_POLICY"
        or not policy["decision"]["permanent"]
        or lock["worldgen_material"] != "cruciblecraft:crude_oil"
        or lock["distillery_source_fluid"] != "liquid_medium_oil"
        or lock["distillery_runtime_input"] != "cruciblecraft:crude_oil"
        or lock["registered_runtime_fluid"] != "cruciblecraft:crude_oil"
        or lock["material_9852_role"] != "SOURCE_MATERIAL_LAYER_ONLY"
        or lock["t9_identity_migration"] != "NONE"
        or lock["duplicate_registration_allowed"]
        or lock["fluid_id_changed"]
        or lock["publication_delta"] != 0
    ):
        raise ValueError("O-37 permanent runtime identity lock drifted")
    total_bytes = sum(len(data) for data in sources.values())
    return {
        "schema_version": 1,
        "status": "O37_CLOSED",
        "resolution": resolution,
        "closure": policy["decision"]["closure"],
        "source": {
            "repository": policy["source"]["repository"],
            "revision": policy["source"]["revision"],
            "tree_manifest": policy["source"]["tree_manifest"],
            "tree_sha1": load(TREE_MANIFEST)["tree_sha1"],
            "java_blob_count": len(entries),
            "java_blob_bytes": total_bytes,
            "search_scope": policy["source"]["search_scope"],
        },
        "symbol_evidence": evidence,
        "negative_evidence": {
            "candidate_rule": policy["direct_binding_search"][
                "candidate_rule"
            ],
            "direct_binding_candidates": candidates,
            "direct_binding_candidate_count": len(candidates),
            "token_inventory": inventory,
            "conclusion": (
                "The fixed revision registers liquid_medium_oil as Raw Oil "
                "with a null material argument, exposes it as FL.Oil_Medium, "
                "and separately declares material 9852 MT.CrudeOil. No Java "
                "statement binds those identities directly."
            ),
        },
        "runtime_identity_lock": lock,
        "bridge": {
            "source_fluid": lock["distillery_source_fluid"],
            "runtime_identity": lock["registered_runtime_fluid"],
            "status": resolution,
            "closure": policy["decision"]["closure"],
            "gt6_equivalence": policy["decision"]["gt6_equivalence"],
            "closed_item": "O-37",
            "permanent": policy["decision"]["permanent"],
            "material_9852_role": lock["material_9852_role"],
            "evidence_projection":
                "tools/t18_o37_identity_projection.json",
            "publication_delta": lock["publication_delta"],
        },
        "currentness": {
            "builder": {
                "path": "tools/build_t18_o37_identity_projection.py",
                "sha256": sha256(BUILDER),
            },
            "policy": {
                "path": "tools/t18_o37_identity_policy.json",
                "sha256": sha256(POLICY),
            },
            "tree_manifest": {
                "path": "tools/t13_gt6_tree_manifest.json",
                "sha256": sha256(TREE_MANIFEST),
            },
        },
    }


def reference_only_check() -> list[str]:
    if not OUTPUT.is_file():
        return ["missing O-37 identity projection"]
    try:
        document = load(OUTPUT)
        errors: list[str] = []
        if OUTPUT.read_text(encoding="utf-8") != stable(document):
            errors.append("O-37 identity projection is not canonical JSON")
        currentness = document.get("currentness") or {}
        expected = {
            "builder": sha256(BUILDER),
            "policy": sha256(POLICY),
            "tree_manifest": sha256(TREE_MANIFEST),
        }
        for owner, digest in expected.items():
            if (currentness.get(owner) or {}).get("sha256") != digest:
                errors.append(f"O-37 {owner} hash drifted")
        if (
            document.get("status") != "O37_CLOSED"
            or document.get("resolution") != "DESIGN_POLICY"
            or document.get("closure")
            != "O37_CLOSED_PERMANENT_DESIGN_POLICY"
            or document.get("negative_evidence", {}).get(
                "direct_binding_candidate_count"
            ) != 0
            or document.get("bridge", {}).get("publication_delta") != 0
        ):
            errors.append("O-37 closed policy evidence drifted")
        return errors
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        return [str(error)]


def full_replay_check(source_root: Path | None = None) -> list[str]:
    document = build(replay_sources(source_root))
    encoded = stable(document)
    if not OUTPUT.is_file():
        return ["missing O-37 identity projection"]
    return (
        []
        if OUTPUT.read_text(encoding="utf-8") == encoded
        else ["stale O-37 identity projection"]
    )


def write(source_root: Path | None = None) -> dict[str, Any]:
    document = build(replay_sources(source_root))
    OUTPUT.write_text(stable(document), encoding="utf-8", newline="\n")
    return document


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--reference-only", action="store_true")
    parser.add_argument("--full-replay", action="store_true")
    parser.add_argument("--source-root", type=Path)
    args = parser.parse_args()
    if args.reference_only and args.full_replay:
        parser.error("--reference-only and --full-replay are exclusive")
    if args.source_root is not None and not args.full_replay:
        parser.error("--source-root requires --full-replay")
    try:
        if args.full_replay:
            if args.check:
                errors = full_replay_check(args.source_root)
                document = load(OUTPUT) if OUTPUT.is_file() else {}
            else:
                errors = []
                document = write(args.source_root)
        else:
            if not args.check:
                parser.error("writing requires --full-replay")
            errors = reference_only_check()
            document = load(OUTPUT) if OUTPUT.is_file() else {}
        if errors:
            raise ValueError("; ".join(errors))
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"O-37 identity replay failed: {error}")
        return 1
    print(json.dumps({
        "status": document["status"],
        "resolution": document["resolution"],
        "direct_binding_candidates": document["negative_evidence"][
            "direct_binding_candidate_count"
        ],
        "publication_delta": document["bridge"]["publication_delta"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
