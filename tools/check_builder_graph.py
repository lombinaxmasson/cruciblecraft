#!/usr/bin/env python3
"""Fail if any committed artifact pins the hash of a file written by a LATER builder.

`tools/verification_builder_policy.json` declares a *topological* order, but
nothing enforces that the recorded `input_sha256` / `inputs` / `artifact_hashes`
blocks actually respect it.  When an early builder pins a late builder's output,
one forward pass of `tools/rebuild_artifacts.py` provably cannot converge: the
early artifact goes stale again the moment the late builder rewrites its output.

Ownership is declared statically in the policy:
  - every `builders` row lists its `outputs`;
  - `pre_chain_builders` (before `builders`, negative indices) lists writers
    that exist but must never enter the ordinary check chain.

This checker reconstructs the real read/write graph from the committed
artifacts and reports four classes of defect:

  BACK_EDGE            reader index < writer index      -> rebuild never converges
  NO_WRITER            pinned file has no builder at all -> value can only be fixed by hand
  OFF_POLICY           writer exists but is not in the policy -> rebuild never runs it
  OUTPUT_DECLARATION   a builder writes files its policy row does not declare
  MIRROR_DRIFT         an artifact hash disagrees with the hand-maintained manifest

Exit code is 1 on BACK_EDGE / MIRROR_DRIFT / OUTPUT_DECLARATION; OFF_POLICY and
NO_WRITER are warnings.

Usage:
  python tools/check_builder_graph.py            # report, exit 1 on hard defects
  python tools/check_builder_graph.py --json     # machine-readable
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "verification_builder_policy.json"

# Files no script writes at all.
HAND_MAINTAINED = {"local_artifact_manifest.json"}

# (artifact, json pointer) pairs whose value MUST equal the sha256 recorded for
# `path` in the hand-maintained tools/local_artifact_manifest.json.  Nothing
# keeps these in sync, so they are checked explicitly.
MANUAL_MIRRORS = [
    ("gt6_ore_chain.json", ("inputs", "gt6_reference", "sha256"),
     "tools/gt6_recipe_normalized_reference.json"),
]

CONST_RE = re.compile(r'^([A-Z][A-Z0-9_]*)\s*=\s*((?:[^\n]|\n\s+)+)', re.M)
NAME_RE = re.compile(r'"([^"]+\.json)"')
HASH_BLOCK_KEYS = {
    "input_sha256", "input_hashes", "artifact_hashes",
    "source_hashes", "dependencies", "owned_inputs",
}


def resolve_outputs(script: Path) -> set[str]:
    """Artifacts this builder writes, resolved from `CONST.write_text(...)`."""
    src = script.read_text(encoding="utf-8", errors="ignore").replace("\r", "")
    consts: dict[str, str] = {}
    for match in CONST_RE.finditer(src):
        name = NAME_RE.search(match.group(2))
        if name:
            consts[match.group(1)] = name.group(1)
    written = set()
    for const, filename in consts.items():
        if re.search(rf"\b{const}\s*\.write_text\s*\(", src):
            written.add(filename)
        elif re.search(rf"\bcheck_or_write\(\s*{const}\b", src):
            written.add(filename)
    return written


def collect_pinned(obj, out: set[str], key: str | None = None) -> None:
    """Every .json filename this document records a hash for."""
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in HASH_BLOCK_KEYS and isinstance(v, dict):
                for p in v:
                    if str(p).endswith(".json"):
                        out.add(Path(p).name)
            if k == "path" and isinstance(v, str) and v.endswith(".json"):
                if isinstance(obj.get("sha256"), str):
                    out.add(Path(v).name)
            collect_pinned(v, out, k)
    elif isinstance(obj, list):
        for v in obj:
            collect_pinned(v, out, key)


def load_policy_rows() -> list[tuple[int, dict]]:
    """(index, row) for pre-chain builders (negative indices) then builders."""
    document = json.loads(POLICY.read_text(encoding="utf-8"))
    pre = document.get("pre_chain_builders") or []
    builders = document.get("builders") or []
    return [
        (i - len(pre), row) for i, row in enumerate(pre)
    ] + [
        (i, row) for i, row in enumerate(builders)
    ]


def _writer_map(rows: list[tuple[int, dict]]) -> dict[str, list[tuple[int, str]]]:
    writers: dict[str, list[tuple[int, str]]] = {}
    for index, row in rows:
        name = row["name"]
        for filename in row.get("outputs") or []:
            writers.setdefault(filename, []).append((index, name))
    return writers


def collect_findings() -> dict:
    """Reconstruct the read/write graph and return every defect found."""
    rows = load_policy_rows()
    findings: dict = {
        "back_edge": [],
        "no_writer": [],
        "off_policy": [],
        "output_declaration": [],
        "manual_mirror": [],
    }

    # Policy ownership is the truth; resolve_outputs only guards that the
    # declarations stay complete for regex-resolvable writers.
    writers = _writer_map(rows)
    for index, row in rows:
        name = row["name"]
        script = ROOT / row["script"]
        if script.is_file():
            missing = resolve_outputs(script) - set(row.get("outputs") or [])
            if missing:
                findings["output_declaration"].append({
                    "builder": name,
                    "script": row["script"],
                    "undeclared_outputs": sorted(missing),
                })

    # Writers that exist in tools/ but are not declared anywhere in the policy.
    off_policy_writers: dict[str, str] = {}
    policy_scripts = {ROOT / row["script"] for _, row in rows}
    policy_names = {row["name"] for _, row in rows}
    for py in sorted(TOOLS.glob("*.py")):
        if py in policy_scripts:
            continue
        for filename in resolve_outputs(py):
            off_policy_writers.setdefault(
                filename, py.relative_to(ROOT).as_posix())

    for index, row in rows:
        name = row["name"]
        for filename in sorted(row.get("outputs") or []):
            path = TOOLS / filename
            if not path.is_file():
                continue
            try:
                document = json.loads(path.read_text(encoding="utf-8"))
            except (json.JSONDecodeError, OSError):
                continue
            pinned: set[str] = set()
            collect_pinned(document, pinned)
            for target in sorted(pinned - {filename}):
                if target in HAND_MAINTAINED:
                    findings["no_writer"].append(
                        {"reader": name, "artifact": filename, "pins": target})
                    continue
                if target in off_policy_writers and target not in writers:
                    findings["off_policy"].append({
                        "reader": name, "artifact": filename, "pins": target,
                        "writer_script": off_policy_writers[target]})
                    continue
                for widx, wname in writers.get(target, []):
                    if widx > index:
                        findings["back_edge"].append({
                            "reader_index": index, "reader": name,
                            "artifact": filename, "pins": target,
                            "writer_index": widx, "writer": wname})

    for bucket in findings.values():
        seen, unique = set(), []
        for row in bucket:
            key = tuple(sorted(row.items()))
            if key not in seen:
                seen.add(key)
                unique.append(row)
        bucket[:] = unique

    manifest_path = TOOLS / "local_artifact_manifest.json"
    if manifest_path.is_file():
        manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
        recorded = {
            row.get("path"): row.get("sha256")
            for row in manifest.get("artifacts") or []
            if isinstance(row, dict)
        }
        for filename, pointer, target in MANUAL_MIRRORS:
            path = TOOLS / filename
            if not path.is_file():
                continue
            value = json.loads(path.read_text(encoding="utf-8"))
            for key in pointer:
                value = (value or {}).get(key)
            if value != recorded.get(target):
                findings["manual_mirror"].append({
                    "artifact": filename,
                    "pointer": "/".join(pointer),
                    "artifact_value": value,
                    "manifest_target": target,
                    "manifest_value": recorded.get(target),
                })

    return findings


def report(findings: dict, json_output: bool = False) -> int:
    """Print findings; return the exit code."""
    if json_output:
        print(json.dumps(findings, indent=2, sort_keys=True))
    else:
        for row in findings["back_edge"]:
            print(f"BACK_EDGE   #{row['reader_index']:<2} {row['reader']}")
            print(f"            {row['artifact']} pins {row['pins']}")
            print(f"            written by #{row['writer_index']} {row['writer']}")
        for row in findings["no_writer"]:
            print(f"NO_WRITER   {row['reader']}: {row['artifact']} pins "
                  f"{row['pins']} (no script writes it)")
        for row in findings["off_policy"]:
            print(f"OFF_POLICY  {row['reader']}: {row['artifact']} pins "
                  f"{row['pins']} <- {row['writer_script']} (not in policy)")
        for row in findings["output_declaration"]:
            print(f"OUTPUT_DECLARATION {row['builder']} ({row['script']}) "
                  f"writes undeclared outputs: {row['undeclared_outputs']}")
        for row in findings["manual_mirror"]:
            print(f"MIRROR_DRIFT {row['artifact']}:{row['pointer']}")
            print(f"            artifact = {row['artifact_value']}")
            print(f"            manifest = {row['manifest_value']}"
                  f"  ({row['manifest_target']})")
            print("            nothing writes local_artifact_manifest.json; "
                  "update it by hand from the local file")
        print()
        print(f"mirror drift: {len(findings['manual_mirror'])}  "
              f"back edges: {len(findings['back_edge'])}  "
              f"hand-maintained pins: {len(findings['no_writer'])}  "
              f"off-policy pins: {len(findings['off_policy'])}  "
              f"undeclared outputs: {len(findings['output_declaration'])}")

    return 1 if (
        findings["back_edge"]
        or findings["manual_mirror"]
        or findings["output_declaration"]
    ) else 0


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    return report(collect_findings(), args.json)


if __name__ == "__main__":
    raise SystemExit(main())
