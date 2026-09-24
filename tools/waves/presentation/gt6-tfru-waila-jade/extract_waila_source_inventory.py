#!/usr/bin/env python3
"""Extract a source-backed GT6-TFRU/kTFRU Waila inventory.

This deliberately inventories source facts instead of guessing display rows:
implemented IWailaTile methods, standard provider references, NBT writes,
body operations, color tokens, and loader registrations. The output is an
inventory for a later Jade projection, not a Jade registration manifest.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[4]
DEFAULT_OUTPUT = (
    ROOT
    / "tools"
    / "waves"
    / "presentation"
    / "gt6-tfru-waila-jade"
    / "source_inventory.json"
)

SOURCE_ROOTS = {
    "gt6_tfru": ROOT / "gt6_tfru" / "gregtech6-TFRU" / "src" / "main" / "java",
    "ktfruaddon": ROOT
    / "ktfruaddon"
    / "kTFRUAddon"
    / "src"
    / "main"
    / "java",
}

REGISTRATION_FILES = {
    "gt6_tfru": SOURCE_ROOTS["gt6_tfru"]
    / "gregtech"
    / "loaders"
    / "b"
    / "Loader_MultiTileEntities.java",
    "ktfruaddon": SOURCE_ROOTS["ktfruaddon"]
    / "cn"
    / "kuzuanpa"
    / "ktfruaddon"
    / "tile"
    / "tileEntityInit0.java",
}

STANDARD_PROVIDER_REFS = (
    "instanceInfoState",
    "instanceInfoEnergyIORange",
    "instanceInfoEnergyIORec",
    "InfoTank",
    "addTankDesc",
    "addFluidStackDesc",
    "addEnergyAmountDesc",
    "addEnergyFlowDesc",
)

COLOR_RE = re.compile(
    r"(?:LH\.)?(?:Chat\.)?(?P<color>"
    r"WHITE|CYAN|YELLOW|GREEN|BLUE|RED|ORANGE|DRED|DGRAY|"
    r"GRAY|DCYAN|BLINKING_RED|BLINKING_CYAN)"
)
CLASS_RE = re.compile(
    r"\b(?P<kind>class|interface)\s+"
    r"(?P<name>[A-Za-z_$][\w$]*)"
    r"(?:\s+extends\s+(?P<extends>[A-Za-z_$][\w$.\s,]*?))?"
    r"(?:\s+implements\s+(?P<implements>[^{]+?))?"
    r"\s*\{"
)
METHOD_RE = re.compile(
    r"\b(?P<name>getWaila(?:Stack|Head|Body|Tail|NBT|Infos)|addToolTips)"
    r"\s*\("
)
NBT_LITERAL_RE = re.compile(
    r"(?:aNBT|tag)\.set(?:Byte|Short|Integer|Long|Boolean|String)"
    r"\s*\(\s*\"(?P<key>[^\"]+)\""
)
NBT_UT_LITERAL_RE = re.compile(
    r"UT\.NBT\.set(?:Number|Boolean)\s*\(\s*"
    r"(?:aNBT|tag)\s*,\s*\"(?P<key>[^\"]+)\""
)
NBT_SYMBOL_RE = re.compile(
    r"(?:aNBT|tag)\.(?:set|put)[A-Za-z]*\s*\(\s*(?P<key>NBT_[A-Z0-9_]+)"
)
REGISTRATION_RE = re.compile(
    r"\b(?:aRegistry|kRegistry\d*)\.add\(\s*"
    r"\"(?P<display>[^\"]*)\"\s*,\s*"
    r"\"(?P<category>[^\"]*)\"\s*,\s*"
    r"(?P<id>[^,]+)\s*,\s*"
    r"(?P<block_id>[^,]+)\s*,\s*"
    r"(?P<class>[A-Za-z_$][\w$]*)\.class"
)
CLASS_ASSIGN_RE = re.compile(
    r"\b(?:aClass|tClass)\s*=\s*(?P<class>[A-Za-z_$][\w$]*)\.class"
)


def relative(path: Path) -> str:
    return path.relative_to(ROOT).as_posix()


def source_hash(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def brace_method_range(
    lines: list[str], start_index: int
) -> dict[str, int]:
    depth = lines[start_index].count("{") - lines[start_index].count("}")
    end = start_index
    if depth > 0:
        while end + 1 < len(lines) and depth > 0:
            end += 1
            depth += lines[end].count("{") - lines[end].count("}")
    return {"start": start_index + 1, "end": end + 1}


def method_inventory(lines: list[str]) -> list[dict[str, Any]]:
    methods: list[dict[str, Any]] = []
    for index, line in enumerate(lines):
        match = METHOD_RE.search(line)
        if not match:
            continue
        name = match.group("name")
        if f".{name}" in line:
            continue
        methods.append(
            {
                "name": name,
                "line": index + 1,
                "range": brace_method_range(lines, index),
            }
        )
    return methods


def method_color_tokens(
    lines: list[str], methods: list[dict[str, Any]], method_name: str
) -> list[str]:
    colors: set[str] = set()
    for method in methods:
        if method["name"] != method_name:
            continue
        start = method["range"]["start"] - 1
        end = method["range"]["end"]
        for line in lines[start:end]:
            colors.update(match.group("color") for match in COLOR_RE.finditer(line))
    return sorted(colors)


def class_inventory(path: Path, text: str) -> list[dict[str, Any]]:
    lines = text.splitlines()
    results: list[dict[str, Any]] = []
    package_match = re.search(r"^\s*package\s+([^;]+);", text, re.MULTILINE)
    package = package_match.group(1) if package_match else ""
    methods = method_inventory(lines)
    providers = sorted(
        token for token in STANDARD_PROVIDER_REFS if token in text
    )
    nbt_literals = sorted(
        {
            match.group("key")
            for match in NBT_LITERAL_RE.finditer(text)
        }
        | {
            match.group("key")
            for match in NBT_UT_LITERAL_RE.finditer(text)
        }
    )
    nbt_symbols = sorted(
        {match.group("key") for match in NBT_SYMBOL_RE.finditer(text)}
    )
    body_operations = []
    operation_patterns = (
        ("super_body", "IWailaTile.super.getWailaBody"),
        ("super_nbt", "IWailaTile.super.getWailaNBT"),
        ("body_add", "currentTip.add"),
        ("tank_row", "addTankDesc"),
        ("fluid_stack_row", "addFluidStackDesc"),
        ("energy_amount_row", "addEnergyAmountDesc"),
        ("energy_flow_row", "addEnergyFlowDesc"),
    )
    for operation, marker in operation_patterns:
        lines_found = [
            index + 1 for index, line in enumerate(lines) if marker in line
        ]
        if lines_found:
            body_operations.append(
                {"operation": operation, "lines": lines_found}
            )
    for match in CLASS_RE.finditer(text):
        interface_text = match.group("implements") or ""
        extends_text = (match.group("extends") or "").strip()
        methods_for_class = [
            method for method in methods
            if method["line"] >= text[:match.start()].count("\n") + 1
        ]
        class_name = match.group("name")
        results.append(
            {
                "class": f"{package}.{class_name}" if package else class_name,
                "simple_class": class_name,
                "kind": match.group("kind"),
                "extends": extends_text,
                "implements": [
                    part.strip()
                    for part in interface_text.split(",")
                    if part.strip()
                ],
                "direct_iwaila_tile": (
                    "IWailaTile" in interface_text
                    or "extends IWailaTile" in text[
                        max(0, match.start() - 80):match.start()
                    ]
                ),
                "waila_reference": "IWailaTile" in text,
                "methods": methods_for_class,
                "providers": providers,
                "body_operations": body_operations,
                "nbt_literal_keys": nbt_literals,
                "nbt_symbolic_keys": nbt_symbols,
                "item_tooltip_color_tokens": method_color_tokens(
                    lines, methods, "addToolTips"
                ),
                "waila_color_tokens": sorted(
                    {
                        match.group("color")
                        for match in COLOR_RE.finditer(text)
                    }
                ),
                "source_lines": len(lines),
            }
        )
    return results


def parse_registrations(tree_name: str, path: Path) -> list[dict[str, Any]]:
    if not path.is_file():
        return []
    lines = path.read_text(encoding="utf-8").splitlines()
    current_class = None
    registrations: list[dict[str, Any]] = []
    for line_number, line in enumerate(lines, 1):
        class_match = CLASS_ASSIGN_RE.search(line)
        if class_match:
            current_class = class_match.group("class")
        match = REGISTRATION_RE.search(line)
        if not match:
            continue
        raw_id = match.group("id").strip()
        source_id: int | None
        try:
            source_id = int(raw_id)
        except ValueError:
            source_id = None
        registrations.append(
            {
                "tree": tree_name,
                "source_file": relative(path),
                "line": line_number,
                "display_name": match.group("display"),
                "category": match.group("category"),
                "source_id": source_id,
                "raw_source_id": raw_id,
                "block_id": match.group("block_id").strip(),
                "class": match.group("class"),
                "class_assignment": current_class,
            }
        )
    return registrations


def extract(output: Path) -> dict[str, Any]:
    files: list[dict[str, Any]] = []
    classes: list[dict[str, Any]] = []
    for tree_name, root in SOURCE_ROOTS.items():
        if not root.is_dir():
            raise FileNotFoundError(f"missing source tree: {root}")
        for path in sorted(root.rglob("*.java")):
            text = path.read_text(encoding="utf-8", errors="replace")
            parsed_classes = class_inventory(path, text)
            if "IWailaTile" not in text and not any(
                method["name"].startswith("getWaila")
                for cls in parsed_classes
                for method in cls["methods"]
            ):
                continue
            files.append(
                {
                    "tree": tree_name,
                    "source_file": relative(path),
                    "sha256": source_hash(text),
                    "classes": [cls["simple_class"] for cls in parsed_classes],
                }
            )
            classes.extend(
                {
                    "tree": tree_name,
                    "source_file": relative(path),
                    **cls,
                }
                for cls in parsed_classes
            )
    registrations = [
        registration
        for tree_name, path in REGISTRATION_FILES.items()
        for registration in parse_registrations(tree_name, path)
    ]
    by_simple_class = {
        cls["simple_class"]: cls for cls in classes if cls["kind"] == "class"
    }
    for registration in registrations:
        source_class = by_simple_class.get(
            registration["class_assignment"] or registration["class"]
        )
        registration["waila_mode"] = (
            "source_class_contract"
            if source_class and source_class["waila_reference"]
            else "unresolved_or_external"
        )
        if source_class:
            registration["source_file"] = source_class["source_file"]
            registration["source_class"] = source_class["class"]
            registration["waila_methods"] = source_class["methods"]
            registration["providers"] = source_class["providers"]
    output.parent.mkdir(parents=True, exist_ok=True)
    return {
        "schema_version": 1,
        "generated_by": "extract_waila_source_inventory.py",
        "source_roots": {
            name: relative(path) for name, path in SOURCE_ROOTS.items()
        },
        "registration_sources": {
            name: relative(path) for name, path in REGISTRATION_FILES.items()
        },
        "standard_provider_refs": list(STANDARD_PROVIDER_REFS),
        "files": files,
        "classes": classes,
        "registrations": registrations,
        "summary": {
            "waila_source_files": len(files),
            "waila_classes": len(classes),
            "mte_registrations": len(registrations),
            "classes_with_waila_methods": sum(
                bool(cls["methods"]) for cls in classes
            ),
            "classes_with_standard_providers": sum(
                bool(cls["providers"]) for cls in classes
            ),
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--write", action="store_true")
    parser.add_argument(
        "--output",
        type=Path,
        default=DEFAULT_OUTPUT,
    )
    args = parser.parse_args()
    document = extract(args.output)
    if args.write:
        args.output.write_text(
            json.dumps(document, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
    print(json.dumps(document["summary"], ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
