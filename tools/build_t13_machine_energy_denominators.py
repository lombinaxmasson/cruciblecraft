#!/usr/bin/env python3
"""Build the fixed-revision T13c machine and energy denominators."""

from __future__ import annotations

import argparse
import bisect
import copy
import hashlib
import json
import os
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / "tools"
POLICY = TOOLS / "t13_machine_energy_policy.json"
MACHINE_OUTPUT = TOOLS / "t13_denominators" / "machine_kinds.json"
ENERGY_OUTPUT = TOOLS / "t13_denominators" / "energy_identities.json"

LOADER_PATH = "src/main/java/gregtech/loaders/b/Loader_MultiTileEntities.java"
MT_PATH = "src/main/java/gregapi/data/MT.java"
TD_PATH = "src/main/java/gregapi/data/TD.java"
BASIC_PATH = (
    "src/main/java/gregapi/tileentity/machines/"
    "MultiTileEntityBasicMachine.java"
)
LOCAL_ENERGY_TYPE = (
    ROOT
    / "src/main/java/com/masson/cruciblecraft/api/energy/EnergyType.java"
)
PINNED_BLOBS = {
    "loader": "cf80887c5e572ee0942f087d1c75238115ca5255",
    "material_tiers": "1681c2072f28ad2d31bc285aef1c6fd2a3bcef92",
    "energy_tags": "d1eaea41fc9b818a874e5dc83082447c27ae8053",
    "basic_machine": "69278675a647648ca208883d67e3267c7f9d5632",
}
TREE_ROOTS = (
    "src/main/java/gregapi/tileentity/machines",
    "src/main/java/gregtech/tileentity/energy",
)
TREE_SINGLETONS = (
    "src/main/java/gregapi/tileentity/connectors/MultiTileEntityAxle.java",
)
MACHINE_FLAGS = (
    "NBT_CHEAP_OVERCLOCKING",
    "NBT_NO_CONSTANT_POWER",
    "NBT_PARALLEL_DURATION",
    "NBT_NEEDS_IGNITION",
    "NBT_WASTE_ENERGY",
    "NBT_LIMIT_CONSUMPTION",
    "NBT_SPECIAL_IS_START_ENERGY",
)
DEFERRED_FIELDS = (
    "reason",
    "owner",
    "replacement_condition",
    "recheck_point",
)
CARDINALITY_FIELDS = (
    "behavior_class",
    "process_map",
    "accepted_energy",
    "emitted_energy",
    "material_expression",
    "input_minimum",
    "input_nominal",
    "input_maximum",
    "parallel",
    "efficiency",
    "output",
    "overclock_policy",
)


def load(path: Path) -> Any:
    return json.loads(path.read_text(encoding="utf-8"))


def stable(value: Any) -> str:
    return json.dumps(
        value, ensure_ascii=False, indent=2, sort_keys=True
    ) + "\n"


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256(path: Path) -> str:
    return sha256_bytes(path.read_bytes())


def git_blob_sha1(data: bytes) -> str:
    normalized = data.replace(b"\r\n", b"\n")
    header = f"blob {len(normalized)}\0".encode("ascii")
    return hashlib.sha1(header + normalized).hexdigest()


def canonical_digest(value: Any) -> str:
    encoded = json.dumps(
        value, ensure_ascii=False, sort_keys=True, separators=(",", ":")
    ).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def collapse_space(value: str) -> str:
    return re.sub(r"\s+", " ", value).strip()


def strip_java_comments(text: str) -> str:
    """Remove comments while preserving offsets and line breaks."""
    result = list(text)
    state = "normal"
    index = 0
    while index < len(text):
        char = text[index]
        following = text[index + 1] if index + 1 < len(text) else ""
        if state == "normal":
            if char == '"':
                state = "string"
            elif char == "'":
                state = "char"
            elif char == "/" and following == "/":
                result[index] = result[index + 1] = " "
                state = "line_comment"
                index += 1
            elif char == "/" and following == "*":
                result[index] = result[index + 1] = " "
                state = "block_comment"
                index += 1
        elif state == "string":
            if char == "\\":
                index += 1
            elif char == '"':
                state = "normal"
        elif state == "char":
            if char == "\\":
                index += 1
            elif char == "'":
                state = "normal"
        elif state == "line_comment":
            if char == "\n":
                state = "normal"
            else:
                result[index] = " "
        elif state == "block_comment":
            if char == "*" and following == "/":
                result[index] = result[index + 1] = " "
                state = "normal"
                index += 1
            elif char != "\n":
                result[index] = " "
        index += 1
    return "".join(result)


def matching_delimiter(
    text: str, opening: int, left: str = "(", right: str = ")"
) -> int:
    depth = 0
    state = "normal"
    index = opening
    while index < len(text):
        char = text[index]
        if state == "normal":
            if char == '"':
                state = "string"
            elif char == "'":
                state = "char"
            elif char == left:
                depth += 1
            elif char == right:
                depth -= 1
                if depth == 0:
                    return index
        elif state == "string":
            if char == "\\":
                index += 1
            elif char == '"':
                state = "normal"
        elif state == "char":
            if char == "\\":
                index += 1
            elif char == "'":
                state = "normal"
        index += 1
    raise ValueError(f"unclosed delimiter at offset {opening}")


def split_top_level(value: str) -> list[str]:
    result: list[str] = []
    start = 0
    paren = bracket = brace = 0
    state = "normal"
    index = 0
    while index < len(value):
        char = value[index]
        if state == "normal":
            if char == '"':
                state = "string"
            elif char == "'":
                state = "char"
            elif char == "(":
                paren += 1
            elif char == ")":
                paren -= 1
            elif char == "[":
                bracket += 1
            elif char == "]":
                bracket -= 1
            elif char == "{":
                brace += 1
            elif char == "}":
                brace -= 1
            elif char == "," and not (paren or bracket or brace):
                result.append(value[start:index].strip())
                start = index + 1
        elif state == "string":
            if char == "\\":
                index += 1
            elif char == '"':
                state = "normal"
        elif state == "char":
            if char == "\\":
                index += 1
            elif char == "'":
                state = "normal"
        index += 1
    result.append(value[start:].strip())
    return result


def _quoted(value: str) -> str | None:
    match = re.fullmatch(r'\s*"((?:\\.|[^"])*)"\s*', value)
    return match.group(1) if match else None


def _nearest(events: list[tuple[int, str]], offset: int) -> str | None:
    positions = [event[0] for event in events]
    index = bisect.bisect_left(positions, offset) - 1
    return events[index][1] if index >= 0 else None


def _nbt_values(call: str) -> dict[str, str]:
    marker = "UT.NBT.make("
    offset = call.find(marker)
    if offset < 0:
        return {}
    opening = offset + len(marker) - 1
    closing = matching_delimiter(call, opening)
    parts = split_top_level(call[opening + 1:closing])
    result: dict[str, str] = {}
    index = 0
    while index + 1 < len(parts):
        key = parts[index].strip()
        if key.startswith("NBT_"):
            result[key] = collapse_space(parts[index + 1])
            index += 2
        else:
            index += 1
    return result


def _energy_symbol(value: str | None) -> str | None:
    if value is None:
        return None
    match = re.search(r"\bTD\.Energy\.([A-Z][A-Z0-9_]*)\b", value)
    return match.group(1) if match else None


def _energy_tokens(value: str) -> list[str]:
    return [] if value == "NONE" else value.split("+")


def _bool_value(value: str | None, default: bool = False) -> bool:
    if value is None:
        return default
    if value == "T":
        return True
    if value == "F":
        return False
    raise ValueError(f"unsupported source Boolean expression {value!r}")


def _numeric(value: str | None, default: str) -> str:
    return collapse_space(value) if value is not None else default


def _loop_multiplicities(cleaned: str) -> dict[int, int]:
    """Return constant enclosing-loop multiplicity by one-based source line."""
    result: dict[int, int] = {}
    stack: list[tuple[int, int]] = []
    pending: int | None = None
    depth = 0
    for line_number, line in enumerate(cleaned.splitlines(), start=1):
        loop = re.search(
            r"\bfor\s*\(\s*int\s+\w+\s*=\s*0\s*;"
            r"\s*\w+\s*<\s*(\d+)\s*;",
            line,
        )
        if loop:
            pending = int(loop.group(1))
        current = 1
        for _, amount in stack:
            current *= amount
        if pending is not None and "{" in line:
            current *= pending
        result[line_number] = current
        for char in line:
            if char == "{":
                depth += 1
                if pending is not None:
                    stack.append((depth, pending))
                    pending = None
            elif char == "}":
                while stack and stack[-1][0] == depth:
                    stack.pop()
                depth -= 1
    return result


def _row_base_key(row: dict[str, Any]) -> str:
    return (
        f"{row['category']}|{row['source_id_expression']}|"
        f"{row['behavior_class']}"
    )


def parse_loader_registrations(
    loader_text: str, loader_blob: str, revision: str
) -> list[dict[str, Any]]:
    cleaned = strip_java_comments(loader_text)
    class_events = [
        (match.start(), match.group(1))
        for match in re.finditer(
            r"\baClass\s*=\s*([A-Za-z_][A-Za-z0-9_.]*)\.class\s*;",
            cleaned,
        )
    ]
    material_events = [
        (match.start(), collapse_space(match.group(1)))
        for match in re.finditer(r"\baMat\s*=\s*([^;]+);", cleaned)
    ]
    multiplicities = _loop_multiplicities(cleaned)
    rows: list[dict[str, Any]] = []
    needle = "aRegistry.add("
    cursor = 0
    while True:
        offset = cleaned.find(needle, cursor)
        if offset < 0:
            break
        opening = offset + len(needle) - 1
        closing = matching_delimiter(cleaned, opening)
        call = cleaned[offset:closing + 1]
        args = split_top_level(cleaned[opening + 1:closing])
        if len(args) < 9:
            raise ValueError(
                f"registration at offset {offset} has only {len(args)} arguments"
            )
        class_expression = collapse_space(args[4])
        if class_expression == "aClass":
            behavior_class = _nearest(class_events, offset)
            if behavior_class is None:
                raise ValueError("aClass registration has no preceding assignment")
        else:
            match = re.search(
                r"([A-Za-z_][A-Za-z0-9_.]*)\s*\.\s*class",
                class_expression,
            )
            if match is None:
                raise ValueError(
                    f"unsupported class expression {class_expression!r}"
                )
            behavior_class = match.group(1)
        material = _nearest(material_events, offset) or "UNDECLARED"
        line = cleaned.count("\n", 0, offset) + 1
        nbt = _nbt_values(call)
        basic_behavior = behavior_class.rsplit(".", 1)[-1] in {
            "MultiTileEntityBasicMachine",
            "MultiTileEntityBasicMachineElectric",
            "MultiTileEntityBasicMachineFlux",
        }
        nominal = _numeric(
            nbt.get("NBT_INPUT"), "32" if basic_behavior else "NONE"
        )
        input_minimum = _numeric(
            nbt.get("NBT_INPUT_MIN"),
            f"({nominal})/2" if nominal != "NONE" else "NONE",
        )
        input_maximum = _numeric(
            nbt.get("NBT_INPUT_MAX"),
            f"({nominal})*2" if nominal != "NONE" else "NONE",
        )
        recipe_map = collapse_space(nbt.get("NBT_RECIPEMAP", "NONE"))
        fuel_map = collapse_space(nbt.get("NBT_FUELMAP", "NONE"))
        process_map = (
            f"recipe:{recipe_map}"
            if recipe_map != "NONE"
            else f"fuel:{fuel_map}"
            if fuel_map != "NONE"
            else "NONE"
        )
        accepted = _energy_symbol(nbt.get("NBT_ENERGY_ACCEPTED"))
        emitted = _energy_symbol(nbt.get("NBT_ENERGY_EMITTED"))
        flags = {
            flag: _bool_value(nbt.get(flag))
            for flag in MACHINE_FLAGS
        }
        overclock = (
            "CHEAP"
            if flags["NBT_CHEAP_OVERCLOCKING"]
            else "STANDARD"
        )
        category = _quoted(args[1])
        if category is None:
            raise ValueError(
                f"registration at line {line} has dynamic category {args[1]!r}"
            )
        normalized_call = collapse_space(call)
        row = {
            "source_line": line,
            "source_id_expression": collapse_space(args[2]),
            "display_expression": collapse_space(args[0]),
            "category": category,
            "behavior_class": behavior_class.rsplit(".", 1)[-1],
            "behavior_class_expression": behavior_class,
            "material_expression": material,
            "recipe_map": recipe_map,
            "fuel_map": fuel_map,
            "process_map": process_map,
            "accepted_energy": accepted or "NONE",
            "emitted_energy": emitted or "NONE",
            "input_minimum": input_minimum,
            "input_nominal": nominal,
            "input_maximum": input_maximum,
            "parallel": _numeric(
                nbt.get("NBT_PARALLEL"), "1" if basic_behavior else "NONE"
            ),
            "efficiency": _numeric(
                nbt.get("NBT_EFFICIENCY"),
                "10000" if basic_behavior else "NONE",
            ),
            "output": _numeric(nbt.get("NBT_OUTPUT"), "NONE"),
            "overclock_policy": (
                overclock if basic_behavior else "NOT_APPLICABLE"
            ),
            "policy_flags": flags,
            "tier_array": (
                re.search(
                    r"\bMT\.DATA\.(Heat_T|Kinetic_T|Electric_T|Flux_T)"
                    r"\s*\[\s*([^\]]+)\s*\]",
                    material,
                ).group(1)
                if re.search(
                    r"\bMT\.DATA\.(Heat_T|Kinetic_T|Electric_T|Flux_T)"
                    r"\s*\[\s*([^\]]+)\s*\]",
                    material,
                )
                else None
            ),
            "tier_expression": (
                re.search(
                    r"\bMT\.DATA\.(?:Heat_T|Kinetic_T|Electric_T|Flux_T)"
                    r"\s*\[\s*([^\]]+)\s*\]",
                    material,
                ).group(1)
                if re.search(
                    r"\bMT\.DATA\.(?:Heat_T|Kinetic_T|Electric_T|Flux_T)"
                    r"\s*\[\s*([^\]]+)\s*\]",
                    material,
                )
                else None
            ),
            "multiplicity": multiplicities.get(line, 1),
            "statement_sha256": sha256_bytes(
                normalized_call.encode("utf-8")
            ),
            "source_identity": {
                "source_revision": revision,
                "source_blob": loader_blob,
                "source_path": LOADER_PATH,
                "source_symbol_or_extraction_key": (
                    "Loader_MultiTileEntities#aRegistry.add"
                ),
                "normalized_row_key": "",
            },
        }
        rows.append(row)
        cursor = closing + 1

    duplicate_counts: Counter[str] = Counter()
    for row in rows:
        base = _row_base_key(row)
        duplicate_counts[base] += 1
        suffix = (
            f"#{duplicate_counts[base]}"
            if duplicate_counts[base] > 1
            else ""
        )
        key = f"loader:{base}{suffix}"
        row["source_identity"]["normalized_row_key"] = key
    return rows


def _independent_columns(call: str) -> list[str]:
    """Independent line-oriented argument lexer; not used by normalizer."""
    columns: list[str] = []
    begin = call.index("(") + 1
    token: list[str] = []
    level = 0
    quoted: str | None = None
    escaped = False
    for char in call[begin:]:
        if quoted is not None:
            token.append(char)
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == quoted:
                quoted = None
            continue
        if char in {'"', "'"}:
            quoted = char
            token.append(char)
        elif char in "([{":
            level += 1
            token.append(char)
        elif char in ")]}":
            if char == ")" and level == 0:
                columns.append("".join(token).strip())
                break
            level -= 1
            token.append(char)
        elif char == "," and level == 0:
            columns.append("".join(token).strip())
            token = []
        else:
            token.append(char)
    return columns


def independent_expected_loader_keys(loader_text: str) -> list[str]:
    """Build the expected raw set without canonical grouping code."""
    without_blocks = re.sub(r"/\*.*?\*/", "", loader_text, flags=re.DOTALL)
    current_class: str | None = None
    bases: list[str] = []
    for raw_line in without_blocks.splitlines():
        line = raw_line.split("//", 1)[0]
        assigned = re.search(
            r"\baClass\s*=\s*([A-Za-z_][A-Za-z0-9_.]*)\.class\s*;",
            line,
        )
        if assigned:
            current_class = assigned.group(1).rsplit(".", 1)[-1]
        marker = "aRegistry.add("
        if marker not in line:
            continue
        call = line[line.index(marker):]
        columns = _independent_columns(call)
        if len(columns) < 5:
            raise ValueError("independent parser could not read registration")
        category = _quoted(columns[1])
        if category is None:
            raise ValueError("independent parser found dynamic category")
        class_value = collapse_space(columns[4])
        if class_value == "aClass":
            behavior_class = current_class
        else:
            match = re.search(
                r"([A-Za-z_][A-Za-z0-9_.]*)\s*\.\s*class",
                class_value,
            )
            behavior_class = (
                match.group(1).rsplit(".", 1)[-1] if match else None
            )
        if behavior_class is None:
            raise ValueError("independent parser could not resolve aClass")
        bases.append(
            f"{category}|{collapse_space(columns[2])}|{behavior_class}"
        )
    counts: Counter[str] = Counter()
    result: list[str] = []
    for base in bases:
        counts[base] += 1
        suffix = f"#{counts[base]}" if counts[base] > 1 else ""
        result.append(f"loader:{base}{suffix}")
    return result


def _energy_block(td_text: str) -> str:
    start = td_text.index("public static class Energy")
    opening = td_text.index("{", start)
    closing = matching_delimiter(td_text, opening, "{", "}")
    return td_text[opening + 1:closing]


def parse_energy_declarations(
    td_text: str, td_blob: str, revision: str
) -> tuple[list[dict[str, Any]], dict[str, list[str]]]:
    block = strip_java_comments(_energy_block(td_text))
    identities: list[dict[str, Any]] = []
    alias_to_symbol: dict[str, str] = {}
    declaration_pattern = re.compile(
        r"public\s+static\s+final\s+TagData\s+"
        r"([A-Z][A-Z0-9_]*)\s*=\s*"
        r"TagData\.createTagData\(\s*"
        r'"([^"]+)"\s*,\s*"([^"]+)"'
        r"(?:\s*,\s*\"([^\"]+)\")?.*?\)\s*(.*?);"
    )
    for match in declaration_pattern.finditer(block):
        symbol, tag_name, short_name, long_name, tail = match.groups()
        aliases = [
            alias
            for alias, target in re.findall(
                r",\s*([A-Z][A-Z0-9_]*)\s*=\s*"
                r"([A-Z][A-Z0-9_]*)",
                tail,
            )
            if target == symbol
        ]
        alias_to_symbol[symbol] = symbol
        for alias in aliases:
            alias_to_symbol[alias] = symbol
        line = td_text.count(
            "\n", 0, td_text.index(match.group(0), td_text.index("public static class Energy"))
        ) + 1
        identities.append({
            "symbol": symbol,
            "tag_name": tag_name,
            "short_name": short_name,
            "long_name": long_name or short_name,
            "aliases": aliases,
            "source_line": line,
            "source_identity": {
                "source_revision": revision,
                "source_blob": td_blob,
                "source_path": TD_PATH,
                "source_symbol_or_extraction_key": f"TD.Energy.{symbol}",
                "normalized_row_key": f"energy:{tag_name}",
            },
        })
    if not identities:
        raise ValueError("TD.Energy declarations were not parsed")

    relations: dict[str, list[str]] = {}
    list_pattern = re.compile(
        r"public\s+static\s+final\s+List<TagData>\s+"
        r"([A-Z][A-Z0-9_]*)\s*=\s*"
        r"new\s+ArrayListNoNulls<>\((.*?)\);",
        flags=re.DOTALL,
    )
    for match in list_pattern.finditer(block):
        relation, body = match.groups()
        members: list[str] = []
        for token in re.findall(r"\b[A-Z][A-Z0-9_]*\b", body):
            symbol = alias_to_symbol.get(token)
            if symbol and symbol not in members:
                members.append(symbol)
        relations[relation] = members
    return identities, relations


def parse_tier_arrays(
    mt_text: str, mt_blob: str, revision: str
) -> dict[str, Any]:
    arrays: dict[str, Any] = {}
    for name in ("Heat_T", "Kinetic_T", "Electric_T", "Flux_T"):
        match = re.search(
            rf"\b{name}\s*=\s*\{{([^}}]+)\}}", mt_text, flags=re.DOTALL
        )
        if match is None:
            raise ValueError(f"MT.DATA.{name} is missing")
        values = [collapse_space(value) for value in match.group(1).split(",")]
        arrays[name] = {
            "values": values,
            "source_identity": {
                "source_revision": revision,
                "source_blob": mt_blob,
                "source_path": MT_PATH,
                "source_symbol_or_extraction_key": f"MT.DATA.{name}",
                "normalized_row_key": f"tier-array:{name}",
            },
        }
    return arrays


def _symbol_record(
    path: str, data: bytes, revision: str
) -> dict[str, Any]:
    text = data.decode("utf-8")
    class_match = re.search(
        r"public\s+(abstract\s+)?class\s+([A-Za-z_][A-Za-z0-9_]*)"
        r"(?:\s+extends\s+([A-Za-z_][A-Za-z0-9_.]*))?",
        text,
    )
    interface_match = re.search(
        r"public\s+interface\s+([A-Za-z_][A-Za-z0-9_]*)", text
    )
    if class_match:
        symbol = class_match.group(2)
        kind = "class"
        abstract = bool(class_match.group(1))
        parent = class_match.group(3)
    elif interface_match:
        symbol = interface_match.group(1)
        kind = "interface"
        abstract = True
        parent = None
    else:
        symbol = Path(path).stem
        kind = "source_companion"
        abstract = True
        parent = None
    blob = git_blob_sha1(data)
    return {
        "path": path,
        "git_blob_sha1": blob,
        "sha256": sha256_bytes(data),
        "symbol": symbol,
        "symbol_kind": kind,
        "abstract": abstract,
        "extends": parent,
        "energy_symbols_referenced": sorted(set(re.findall(
            r"\bTD\.Energy\.([A-Z][A-Z0-9_]*)\b", text
        ))),
        "source_identity": {
            "source_revision": revision,
            "source_blob": blob,
            "source_path": path,
            "source_symbol_or_extraction_key": symbol,
            "normalized_row_key": f"tree-symbol:{path}#{symbol}",
        },
    }


def source_tree_inventory(
    source_root: Path, registered_symbols: set[str], revision: str
) -> list[dict[str, Any]]:
    paths: set[Path] = set()
    for relative in TREE_ROOTS:
        directory = source_root / relative
        if not directory.is_dir():
            raise ValueError(f"missing fixed source directory {relative}")
        paths.update(directory.rglob("*.java"))
    for relative in TREE_SINGLETONS:
        path = source_root / relative
        if not path.is_file():
            raise ValueError(f"missing fixed source file {relative}")
        paths.add(path)
    records: list[dict[str, Any]] = []
    for path in sorted(paths):
        relative = path.relative_to(source_root).as_posix()
        record = _symbol_record(relative, path.read_bytes(), revision)
        symbol = record["symbol"]
        if "Flux" in symbol or "Buildcraft" in symbol:
            role = "compat_exclusion"
            reason = "Compatibility wrapper is not a GT6 canonical behavior."
        elif record["symbol_kind"] == "interface":
            role = "interface_exclusion"
            reason = "Interface is a contract, not a registration behavior."
        elif record["abstract"]:
            role = "abstract_exclusion"
            reason = "Abstract/base source is not a concrete registration."
        elif symbol not in registered_symbols:
            role = "helper_or_unregistered_exclusion"
            reason = "Concrete symbol has no fixed-loader registration call."
        else:
            role = "registered_behavior"
            reason = "Concrete symbol is referenced by the fixed loader."
        record["inventory_role"] = role
        record["reason"] = reason
        records.append(record)
    return records


def extract_source_snapshot(
    source_root: Path, policy: dict[str, Any]
) -> dict[str, Any]:
    source = policy["source"]
    revision = source["revision"]
    required = {
        "loader": LOADER_PATH,
        "material_tiers": MT_PATH,
        "energy_tags": TD_PATH,
        "basic_machine": BASIC_PATH,
    }
    data: dict[str, bytes] = {}
    blobs: dict[str, str] = {}
    hashes: dict[str, str] = {}
    for name, relative in required.items():
        path = source_root / relative
        if not path.is_file():
            raise ValueError(f"missing fixed source file {relative}")
        data[name] = path.read_bytes()
        blobs[name] = git_blob_sha1(data[name])
        hashes[name] = sha256_bytes(data[name])
    rows = parse_loader_registrations(
        data["loader"].decode("utf-8"), blobs["loader"], revision
    )
    independent = independent_expected_loader_keys(
        data["loader"].decode("utf-8")
    )
    production_keys = [
        row["source_identity"]["normalized_row_key"] for row in rows
    ]
    if independent != production_keys:
        raise ValueError(
            "independent loader parser disagrees with production extraction"
        )
    energy, relations = parse_energy_declarations(
        data["energy_tags"].decode("utf-8"),
        blobs["energy_tags"],
        revision,
    )
    tiers = parse_tier_arrays(
        data["material_tiers"].decode("utf-8"),
        blobs["material_tiers"],
        revision,
    )
    registered = {row["behavior_class"] for row in rows}
    tree = source_tree_inventory(source_root, registered, revision)
    return {
        "source_files": {
            name: {
                "path": relative,
                "git_blob_sha1": blobs[name],
                "sha256": hashes[name],
            }
            for name, relative in required.items()
        },
        "raw_registrations": rows,
        "independent_expected": {
            "parser": "line_oriented_java_argument_lexer",
            "normalizer_grouping_reused": False,
            "raw_registration_keys": independent,
        },
        "tier_arrays": tiers,
        "energy_declarations": energy,
        "energy_relations": relations,
        "tree_symbol_inventory": tree,
        "counts": {
            "loader_call_sites": len(rows),
            "loader_expanded_registrations": sum(
                row["multiplicity"] for row in rows
            ),
            "energy_declarations": len(energy),
            "tree_symbols": len(tree),
        },
    }


def _policy_flag_key(row: dict[str, Any]) -> str:
    flags = row["policy_flags"]
    return ",".join(
        f"{name}={str(flags[name]).lower()}" for name in MACHINE_FLAGS
    )


def canonical_machine_key(row: dict[str, Any]) -> str:
    """The declared behavior/map/energy/policy canonical identity."""
    return "|".join((
        row["behavior_class"],
        row["process_map"],
        f"accepts:{row['accepted_energy']}",
        f"emits:{row['emitted_energy']}",
        _policy_flag_key(row),
    ))


def _tree_roles(policy: dict[str, Any]) -> dict[str, str]:
    return {
        row["symbol"]: row["inventory_role"]
        for row in policy["source_snapshot"]["tree_symbol_inventory"]
    }


def raw_assignment(
    row: dict[str, Any], tree_roles: dict[str, str]
) -> tuple[str, str]:
    symbol = row["behavior_class"]
    role = tree_roles.get(symbol)
    if role == "compat_exclusion":
        return "exclusion", "compatibility_wrapper"
    if row["category"] == "Multiblock Machines":
        return "exclusion", "multiblock_owned_by_t13d"
    if row["category"] == "Reactors" and "ReactorRod" in symbol:
        return "exclusion", "reactor_part_not_machine_behavior"
    if row["category"] == "Basic Machines":
        if symbol in {
            "MultiTileEntityBasicMachine",
            "MultiTileEntityBasicMachineElectric",
            "MultiTileEntityBasicMachineFlux",
        }:
            return "canonical", "basic_machine_registration"
        return "exclusion", "non_basic_behavior_in_basic_category"
    if role == "registered_behavior":
        return "canonical", "fixed_tree_energy_or_axle_behavior"
    return "exclusion", "outside_fixed_machine_energy_behavior_tree"


def _machine_scope(
    row: dict[str, Any], variants: list[dict[str, Any]]
) -> dict[str, Any]:
    accepted = row["accepted_energy"]
    emitted = row["emitted_energy"]
    energies = set(_energy_tokens(accepted) + _energy_tokens(emitted))
    symbol = row["behavior_class"]
    category = row["category"]
    if "Reactor" in symbol or energies & {"NU"}:
        return {
            "classification": "deferred_with_reason",
            "owner": "POST_T19",
            "reason": "Nuclear machines are outside the third-stage industrial batch.",
            "replacement_condition": "Open a source-backed nuclear/cooling phase.",
            "recheck_point": "After T19 and the cooling fidelity decision.",
        }
    if energies & {"RF", "MJ"}:
        return {
            "classification": "out_of_scope",
            "owner": "OUT_OF_SCOPE",
            "reason": "External RF/MJ compatibility is absent from the pinned runtime.",
        }
    if category == "Basic Machines":
        if energies <= {"RU", "KU"} and energies:
            owner = "T16"
        elif energies <= {"HU", "EU"} and energies:
            owner = "T17"
        elif energies == {"TU"}:
            owner = "T15"
        else:
            return {
                "classification": "deferred_with_reason",
                "owner": "POST_T19",
                "reason": (
                    "The machine uses an energy identity outside the "
                    "T16 RU/KU and T17 HU/EU batches."
                ),
                "replacement_condition": (
                    "Provide the source identity's producer, transport and "
                    "runtime contract."
                ),
                "recheck_point": "After T19 denominator-selected batches.",
            }
        return {
            "classification": "in_scope",
            "owner": owner,
            "reason": "Fixed-loader Basic Machine canonical kind.",
        }
    if energies & {"RU", "KU", "EU", "HU"} or "Steam" in symbol:
        return {
            "classification": "in_scope",
            "owner": "T18",
            "reason": "Fixed-loader energy production, conversion or transport kind.",
        }
    return {
        "classification": "deferred_with_reason",
        "owner": "POST_T19",
        "reason": "The energy behavior is not selected by T15-T18.",
        "replacement_condition": (
            "Select a later source-backed industrial or peripheral batch."
        ),
        "recheck_point": "After T19 scope review.",
    }


def _variant_record(row: dict[str, Any]) -> dict[str, Any]:
    return {
        "source_identity": row["source_identity"],
        "source_line": row["source_line"],
        "source_id_expression": row["source_id_expression"],
        "display_expression": row["display_expression"],
        "material_expression": row["material_expression"],
        "tier_array": row["tier_array"],
        "tier_expression": row["tier_expression"],
        "multiplicity": row["multiplicity"],
        "input_window": {
            "minimum": row["input_minimum"],
            "nominal": row["input_nominal"],
            "maximum": row["input_maximum"],
        },
        "parallel": row["parallel"],
        "efficiency": row["efficiency"],
        "output": row["output"],
        "overclock_policy": row["overclock_policy"],
        "policy_flags": row["policy_flags"],
        "energy_resolution": row.get("energy_resolution", {
            "mode": "LOADER_NBT_OR_NONE",
            "resolved_fields": [],
        }),
    }


def _field_value(row: dict[str, Any], field: str) -> Any:
    if field == "accepted_energy":
        return tuple(sorted(_energy_tokens(row[field])))
    if field == "emitted_energy":
        return tuple(sorted(_energy_tokens(row[field])))
    return row[field]


def field_cardinality_audit(
    raw_rows: list[dict[str, Any]],
    canonical_rows: list[dict[str, Any]],
    declared_collapse_fields: Iterable[str],
) -> dict[str, Any]:
    declared = set(declared_collapse_fields)
    source_cardinality: dict[str, int] = {}
    normalized_variant_cardinality: dict[str, int] = {}
    canonical_cardinality: dict[str, int] = {}
    findings: list[dict[str, Any]] = []
    for field in CARDINALITY_FIELDS:
        source_values = {
            json.dumps(_field_value(row, field), sort_keys=True)
            for row in raw_rows
        }
        variant_values = {
            json.dumps(_field_value(row, field), sort_keys=True)
            for canonical in canonical_rows
            for row in canonical["_raw_variants"]
        }
        if field in {"material_expression", "input_minimum", "input_nominal",
                     "input_maximum", "parallel", "efficiency", "output"}:
            canonical_values = {
                json.dumps("DECLARED_VARIANT_AXIS")
                for _ in canonical_rows
            }
        else:
            canonical_values = {
                json.dumps(_field_value(row["_representative"], field),
                           sort_keys=True)
                for row in canonical_rows
            }
        source_cardinality[field] = len(source_values)
        normalized_variant_cardinality[field] = len(variant_values)
        canonical_cardinality[field] = len(canonical_values)
        if source_values != variant_values:
            findings.append({
                "status": f"UNIFORM_{field.upper()}",
                "reason": "normalized variants do not preserve source values",
            })
        if (
            len(source_values) > len(canonical_values)
            and field not in declared
        ):
            findings.append({
                "status": f"UNIFORM_{field.upper()}",
                "reason": "undeclared canonical field-cardinality reduction",
            })
    if findings:
        raise ValueError(f"field cardinality audit failed: {findings}")
    return {
        "status": "PASS",
        "status_prefix": "UNIFORM_",
        "declared_collapse_fields": sorted(declared),
        "source_cardinality": source_cardinality,
        "normalized_variant_cardinality": normalized_variant_cardinality,
        "canonical_cardinality": canonical_cardinality,
        "uniform_findings": [],
    }


def _resolve_cc_mapping(
    policy: dict[str, Any], kinds: list[dict[str, Any]]
) -> dict[str, Any]:
    result: dict[str, Any] = {}
    for ledger_name, mappings in policy["cc_25_plus_12_mapping"].items():
        rows: list[dict[str, Any]] = []
        for mapping in mappings:
            target = copy.deepcopy(mapping)
            selector = target.pop("selector", None)
            if selector is not None:
                matches = [
                    row for row in kinds
                    if all(row.get(field) == value
                           for field, value in selector.items())
                ]
                if len(matches) != 1:
                    raise ValueError(
                        f"CC target {mapping['cc_id']} resolved to "
                        f"{len(matches)} canonical kinds"
                    )
                target["canonical_key"] = matches[0]["canonical_key"]
                target["source_mapping_status"] = "TARGET_MATCHED"
            else:
                target["canonical_key"] = None
                target["source_mapping_status"] = mapping[
                    "source_mapping_status"
                ]
            rows.append(target)
        result[ledger_name] = rows
    return result


def _validate_disposition(row: dict[str, Any]) -> None:
    classification = row["classification"]
    if classification == "unclassified":
        raise ValueError(f"unclassified denominator row {row.get('canonical_key')}")
    if classification == "deferred_with_reason":
        missing = [
            field for field in DEFERRED_FIELDS
            if not isinstance(row.get(field), str) or not row[field].strip()
        ]
        if missing:
            raise ValueError(
                f"deferred row is missing required fields: {missing}"
            )


def build_machine(policy: dict[str, Any]) -> dict[str, Any]:
    snapshot = policy["source_snapshot"]
    raw = copy.deepcopy(snapshot["raw_registrations"])
    expected = snapshot["independent_expected"]["raw_registration_keys"]
    actual = [
        row["source_identity"]["normalized_row_key"] for row in raw
    ]
    if len(actual) != len(set(actual)):
        raise ValueError("raw registration identity is not unique")
    if actual != expected:
        raise ValueError("raw registration set differs from independent expected set")
    for row in raw:
        defaults = policy["class_energy_defaults"].get(
            row["behavior_class"], {}
        )
        resolved_by: list[str] = []
        if row["accepted_energy"] == "NONE" and defaults.get("accepted"):
            row["accepted_energy"] = defaults["accepted"]
            resolved_by.append("accepted")
        if row["emitted_energy"] == "NONE" and defaults.get("emitted"):
            row["emitted_energy"] = defaults["emitted"]
            resolved_by.append("emitted")
        row["energy_resolution"] = {
            "mode": (
                "CLASS_SYMBOL_DEFAULT"
                if resolved_by
                else "LOADER_NBT_OR_NONE"
            ),
            "resolved_fields": resolved_by,
        }
    roles = _tree_roles(policy)
    grouped: dict[str, list[dict[str, Any]]] = defaultdict(list)
    excluded: list[dict[str, Any]] = []
    for row in raw:
        assignment, reason = raw_assignment(row, roles)
        if assignment == "canonical":
            grouped[canonical_machine_key(row)].append(row)
        else:
            excluded.append({
                "source_identity": row["source_identity"],
                "source_line": row["source_line"],
                "source_id_expression": row["source_id_expression"],
                "display_expression": row["display_expression"],
                "category": row["category"],
                "behavior_class": row["behavior_class"],
                "multiplicity": row["multiplicity"],
                "exclusion_reason": reason,
                "assignment": "exclusion",
            })
    kinds_internal: list[dict[str, Any]] = []
    for key, variants in sorted(grouped.items()):
        representative = variants[0]
        scope = _machine_scope(representative, variants)
        kind = {
            "canonical_key": key,
            "behavior_class": representative["behavior_class"],
            "recipe_map": representative["recipe_map"],
            "fuel_map": representative["fuel_map"],
            "process_map": representative["process_map"],
            "accepted_energy": representative["accepted_energy"],
            "emitted_energy": representative["emitted_energy"],
            "policy_flags": representative["policy_flags"],
            "overclock_policy": representative["overclock_policy"],
            "variant_classification": (
                "tiered" if sum(row["multiplicity"] for row in variants) > 1
                else "fixed"
            ),
            "source_ids": [
                row["source_id_expression"] for row in variants
            ],
            "tier_arrays": sorted({
                row["tier_array"] for row in variants
                if row["tier_array"] is not None
            }),
            "variants": [_variant_record(row) for row in variants],
            "source_identity": {
                "source_revision": policy["source"]["revision"],
                "source_blob": snapshot["source_files"]["loader"][
                    "git_blob_sha1"
                ],
                "source_path": LOADER_PATH,
                "source_symbol_or_extraction_key": (
                    representative["behavior_class"]
                ),
                "normalized_row_key": key,
            },
            **scope,
            "_representative": representative,
            "_raw_variants": variants,
        }
        _validate_disposition(kind)
        kinds_internal.append(kind)

    audit = field_cardinality_audit(
        [row for variants in grouped.values() for row in variants],
        kinds_internal,
        policy["normalization"]["declared_collapse_fields"],
    )
    public_kinds = [
        {key: value for key, value in row.items() if not key.startswith("_")}
        for row in kinds_internal
    ]
    mappings = _resolve_cc_mapping(policy, public_kinds)
    processing = mappings["processing"]
    non_spec = mappings["non_spec"]
    if len(processing) != 25 or len(non_spec) != 12:
        raise ValueError("CC target ledger must remain 25 processing + 12 non-spec")
    if len({row["cc_id"] for row in processing}) != 25:
        raise ValueError("CC processing target ids are not unique")
    if len({row["cc_id"] for row in non_spec}) != 12:
        raise ValueError("CC non-spec target ids are not unique")

    classified = Counter(row["classification"] for row in public_kinds)
    owners = Counter(row["owner"] for row in public_kinds)
    variant_counts = Counter(
        row["variant_classification"] for row in public_kinds
    )
    raw_canonical_sites = sum(len(rows) for rows in grouped.values())
    raw_canonical_expanded = sum(
        row["multiplicity"]
        for rows in grouped.values()
        for row in rows
    )
    return {
        "schema_version": 1,
        "status": "T13C_MACHINE_DENOMINATOR_READY",
        "category": "machine_kinds",
        "generated_by": "python tools/build_t13_machine_energy_denominators.py",
        "source": policy["source"],
        "source_files": snapshot["source_files"],
        "canonical_key_contract": policy["normalization"][
            "canonical_key_contract"
        ],
        "independent_expected": {
            **snapshot["independent_expected"],
            "raw_registration_count": len(expected),
            "bidirectional_set_equality": True,
        },
        "tier_arrays": snapshot["tier_arrays"],
        "tree_symbol_inventory": {
            "counts": {
                "total": len(snapshot["tree_symbol_inventory"]),
                "registered_behavior": sum(
                    row["inventory_role"] == "registered_behavior"
                    for row in snapshot["tree_symbol_inventory"]
                ),
                "excluded": sum(
                    row["inventory_role"] != "registered_behavior"
                    for row in snapshot["tree_symbol_inventory"]
                ),
                "unclassified": 0,
            },
            "rows": snapshot["tree_symbol_inventory"],
        },
        "counts": {
            "raw_registration_call_sites": len(raw),
            "raw_expanded_registrations": sum(
                row["multiplicity"] for row in raw
            ),
            "canonical_assigned_call_sites": raw_canonical_sites,
            "canonical_assigned_expanded_registrations": (
                raw_canonical_expanded
            ),
            "excluded_call_sites": len(excluded),
            "excluded_expanded_registrations": sum(
                row["multiplicity"] for row in excluded
            ),
            "canonical_kinds": len(public_kinds),
            "tiered": variant_counts["tiered"],
            "fixed": variant_counts["fixed"],
            "classifications": dict(sorted(classified.items())),
            "owners": dict(sorted(owners.items())),
            "classified": len(public_kinds),
            "unclassified": 0,
        },
        "raw_assignment_ledger": {
            "canonical_source_keys": sorted(
                row["source_identity"]["normalized_row_key"]
                for rows in grouped.values()
                for row in rows
            ),
            "exclusion_source_keys": sorted(
                row["source_identity"]["normalized_row_key"]
                for row in excluded
            ),
            "bidirectional_complete": True,
            "unique_assignment": True,
        },
        "canonical_kinds": public_kinds,
        "exclusions": excluded,
        "cc_25_plus_12_mapping": {
            "role": (
                "target-only mapping; never used to construct the source "
                "denominator"
            ),
            "processing": processing,
            "non_spec": non_spec,
            "counts": {
                "processing": len(processing),
                "non_spec": len(non_spec),
                "unclassified": 0,
            },
        },
        "field_cardinality_audit": audit,
    }


def _energy_scope(symbol: str) -> dict[str, Any]:
    if symbol in {"KINETIC_ROTATION", "KINETIC_PUSH"}:
        return {
            "classification": "in_scope",
            "owner": "T16",
            "reason": "RU/KU machine and topology batches consume this identity.",
        }
    if symbol in {"ELECTRICITY", "HEAT"}:
        return {
            "classification": "in_scope",
            "owner": "T17",
            "reason": "HU/EU machine batches consume this identity.",
        }
    if symbol == "TIME":
        return {
            "classification": "in_scope",
            "owner": "T15",
            "reason": "T12 fixed-utility residuals use TU explicitly.",
        }
    if symbol in {"STEAM", "AIR"}:
        return {
            "classification": "in_scope",
            "owner": "T18",
            "reason": "Steam/air production and conversion remain distinct.",
        }
    if symbol in {"REDSTONE_FLUX", "MINECRAFT_JOULES"}:
        return {
            "classification": "out_of_scope",
            "owner": "OUT_OF_SCOPE",
            "reason": "External RF/MJ compatibility is absent from the pinned runtime.",
        }
    if symbol.startswith("VIS_"):
        return {
            "classification": "out_of_scope",
            "owner": "OUT_OF_SCOPE",
            "reason": "Thaumcraft Vis compatibility is outside third-stage scope.",
        }
    return {
        "classification": "deferred_with_reason",
        "owner": "POST_T19",
        "reason": "No T15-T18 batch owns this GT6 energy identity.",
        "replacement_condition": (
            "Open a source-backed producer/transport/consumer phase."
        ),
        "recheck_point": "After T19 scope review.",
    }


def build_energy(
    policy: dict[str, Any], machine: dict[str, Any]
) -> dict[str, Any]:
    snapshot = policy["source_snapshot"]
    declarations = copy.deepcopy(snapshot["energy_declarations"])
    relation_membership: dict[str, list[str]] = defaultdict(list)
    for relation, members in snapshot["energy_relations"].items():
        for symbol in members:
            relation_membership[symbol].append(relation)
    accepted_refs: dict[str, set[str]] = defaultdict(set)
    emitted_refs: dict[str, set[str]] = defaultdict(set)
    aliases = {
        alias: row["symbol"]
        for row in declarations
        for alias in [row["symbol"], *row["aliases"]]
    }
    for kind in machine["canonical_kinds"]:
        for accepted in _energy_tokens(kind["accepted_energy"]):
            symbol = aliases.get(accepted)
            if symbol is None:
                raise ValueError(
                    f"machine references undeclared accepted energy "
                    f"{accepted}"
                )
            accepted_refs[symbol].add(kind["canonical_key"])
        for emitted in _energy_tokens(kind["emitted_energy"]):
            symbol = aliases.get(emitted)
            if symbol is None:
                raise ValueError(
                    f"machine references undeclared emitted energy "
                    f"{emitted}"
                )
            emitted_refs[symbol].add(kind["canonical_key"])
    local = policy["local_energy_mapping"]
    rows: list[dict[str, Any]] = []
    for declaration in declarations:
        symbol = declaration["symbol"]
        mapping = local.get(symbol, {
            "local_energy_type": None,
            "topology": "UNIMPLEMENTED",
        })
        row = {
            **declaration,
            "relations": sorted(relation_membership.get(symbol, [])),
            "local_energy_type": mapping["local_energy_type"],
            "topology": mapping["topology"],
            "machine_references": {
                "accepted_by": sorted(accepted_refs.get(symbol, set())),
                "emitted_by": sorted(emitted_refs.get(symbol, set())),
            },
            **_energy_scope(symbol),
        }
        _validate_disposition(row)
        rows.append(row)
    tag_names = [row["tag_name"] for row in rows]
    if len(tag_names) != len(set(tag_names)):
        raise ValueError("TD.Energy canonical tag names are not unique")
    all_members = set(snapshot["energy_relations"].get("ALL", []))
    symbols = {row["symbol"] for row in rows}
    if all_members != symbols:
        raise ValueError(
            f"TD.Energy.ALL mismatch missing={sorted(symbols-all_members)} "
            f"extra={sorted(all_members-symbols)}"
        )
    classifications = Counter(row["classification"] for row in rows)
    owners = Counter(row["owner"] for row in rows)
    source_relation_cardinality = {
        name: len(values)
        for name, values in sorted(snapshot["energy_relations"].items())
    }
    local_source = strip_java_comments(
        LOCAL_ENERGY_TYPE.read_text(encoding="utf-8")
    )
    local_symbols = sorted(set(re.findall(
        r"(?m)^\s*([A-Z][A-Z0-9_]*)\s*(?:,|$)", local_source
    )))
    mapped_local_symbols = {
        row["local_energy_type"]
        for row in rows
        if row["local_energy_type"] is not None
    }
    if not mapped_local_symbols <= set(local_symbols):
        raise ValueError(
            "local energy mapping references missing EnergyType symbols"
        )
    return {
        "schema_version": 1,
        "status": "T13C_ENERGY_DENOMINATOR_READY",
        "category": "energy_identities",
        "generated_by": "python tools/build_t13_machine_energy_denominators.py",
        "source": policy["source"],
        "source_file": snapshot["source_files"]["energy_tags"],
        "identity_contract": (
            "Every TD.Energy TagData.createTagData declaration is one "
            "canonical identity; aliases and local mappings never merge tags."
        ),
        "counts": {
            "identities": len(rows),
            "classified": len(rows),
            "classifications": dict(sorted(classifications.items())),
            "owners": dict(sorted(owners.items())),
            "with_local_energy_type": sum(
                row["local_energy_type"] is not None for row in rows
            ),
            "referenced_by_machine_kinds": sum(
                bool(row["machine_references"]["accepted_by"])
                or bool(row["machine_references"]["emitted_by"])
                for row in rows
            ),
            "unclassified": 0,
        },
        "legacy_kinetic_boundary": {
            "source_identity": None,
            "local_energy_type": "KINETIC",
            "status": "T12_FIXED_OR_DEFERRED_COMPATIBILITY_ONLY",
            "rule": (
                "Legacy KINETIC is not a TD.Energy identity and cannot be "
                "used as source evidence for RU or KU. New source-classified "
                "mappings use KINETIC_ROTATION or KINETIC_PUSH."
            ),
            "t12_ledger_role": "target-only compatibility boundary",
        },
        "local_mapping_source": {
            "path": LOCAL_ENERGY_TYPE.relative_to(ROOT).as_posix(),
            "sha256": sha256(LOCAL_ENERGY_TYPE),
            "source_symbol_or_extraction_key": "EnergyType",
            "declared_symbols": local_symbols,
        },
        "relations": snapshot["energy_relations"],
        "rows": rows,
        "field_cardinality_audit": {
            "status": "PASS",
            "status_prefix": "UNIFORM_",
            "declared_collapse_fields": [],
            "source_identity_cardinality": len(declarations),
            "normalized_identity_cardinality": len(rows),
            "source_relation_cardinality": source_relation_cardinality,
            "normalized_relation_cardinality": source_relation_cardinality,
            "uniform_findings": [],
        },
    }


def validate_policy(policy: dict[str, Any]) -> None:
    if policy.get("schema_version") != 1:
        raise ValueError("unsupported T13c policy schema")
    source = policy["source"]
    if source["revision"] != "3703e40308c8c030763fd6297dea8b210d2a77b1":
        raise ValueError("T13c source revision drifted")
    snapshot = policy.get("source_snapshot")
    if not isinstance(snapshot, dict) or not snapshot.get("raw_registrations"):
        raise ValueError("T13c policy has no fixed source snapshot")
    source_files = snapshot.get("source_files", {})
    if set(source_files) != set(PINNED_BLOBS):
        raise ValueError("T13c fixed source file set drifted")
    for name, expected_blob in PINNED_BLOBS.items():
        if source_files[name].get("git_blob_sha1") != expected_blob:
            raise ValueError(f"T13c {name} source blob drifted")
    required_identity = {
        "source_revision",
        "source_blob",
        "source_path",
        "source_symbol_or_extraction_key",
        "normalized_row_key",
    }
    for row in snapshot["raw_registrations"]:
        identity = row["source_identity"]
        if set(identity) != required_identity:
            raise ValueError("raw source identity fields are incomplete")
        if (
            identity["source_revision"] != source["revision"]
            or identity["source_blob"] != PINNED_BLOBS["loader"]
            or identity["source_path"] != LOADER_PATH
        ):
            raise ValueError("raw registration source identity drifted")
    for row in snapshot["energy_declarations"]:
        identity = row["source_identity"]
        if set(identity) != required_identity:
            raise ValueError("energy source identity fields are incomplete")
        if (
            identity["source_revision"] != source["revision"]
            or identity["source_blob"] != PINNED_BLOBS["energy_tags"]
            or identity["source_path"] != TD_PATH
        ):
            raise ValueError("energy declaration source identity drifted")
    for row in snapshot["tree_symbol_inventory"]:
        identity = row.get("source_identity")
        if not isinstance(identity, dict) or set(identity) != required_identity:
            raise ValueError("tree symbol source identity fields are incomplete")
        if (
            identity["source_revision"] != source["revision"]
            or identity["source_blob"] != row["git_blob_sha1"]
            or identity["source_path"] != row["path"]
            or identity["source_symbol_or_extraction_key"] != row["symbol"]
        ):
            raise ValueError("tree symbol source identity drifted")
    declared_symbols = {
        row["symbol"] for row in snapshot["energy_declarations"]
    }
    declared_aliases = declared_symbols | {
        alias
        for row in snapshot["energy_declarations"]
        for alias in row["aliases"]
    }
    if set(policy["local_energy_mapping"]) != declared_symbols:
        raise ValueError("local energy mapping does not cover every identity")
    tree_symbols = {
        row["symbol"] for row in snapshot["tree_symbol_inventory"]
    }
    for symbol, defaults in policy["class_energy_defaults"].items():
        if symbol not in tree_symbols:
            raise ValueError(f"class energy default symbol is absent: {symbol}")
        for direction in ("accepted", "emitted"):
            energy = defaults.get(direction)
            if energy is not None and not set(_energy_tokens(energy)) <= declared_aliases:
                raise ValueError(
                    f"{symbol} uses undeclared class energy {energy}"
                )


def build(policy: dict[str, Any] | None = None) -> tuple[dict[str, Any], dict[str, Any]]:
    selected = copy.deepcopy(policy) if policy is not None else load(POLICY)
    validate_policy(selected)
    machine = build_machine(selected)
    energy = build_energy(selected, machine)
    source_snapshot = selected["source_snapshot"]
    common_hashes = {
        "policy": sha256(POLICY) if policy is None else None,
        "source_snapshot_sha256": canonical_digest(source_snapshot),
    }
    machine["source_hashes"] = {
        **common_hashes,
        "builder": sha256(Path(__file__).resolve()),
    }
    energy["source_hashes"] = {
        **common_hashes,
        "builder": sha256(Path(__file__).resolve()),
        "machine_denominator_sha256": canonical_digest(machine),
    }
    return machine, energy


def write() -> tuple[dict[str, Any], dict[str, Any]]:
    machine, energy = build()
    MACHINE_OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    MACHINE_OUTPUT.write_text(stable(machine), encoding="utf-8", newline="\n")
    ENERGY_OUTPUT.write_text(stable(energy), encoding="utf-8", newline="\n")
    return machine, energy


def check() -> list[str]:
    machine, energy = build()
    errors: list[str] = []
    for path, document in (
        (MACHINE_OUTPUT, machine),
        (ENERGY_OUTPUT, energy),
    ):
        if not path.is_file():
            errors.append(f"missing generated file: {path.relative_to(ROOT)}")
        elif path.read_text(encoding="utf-8") != stable(document):
            errors.append(f"stale generated file: {path.relative_to(ROOT)}")
    return errors


def verify_source(source_root: Path, policy: dict[str, Any]) -> None:
    actual = extract_source_snapshot(source_root, policy)
    expected = policy["source_snapshot"]
    if actual != expected:
        raise ValueError(
            "fixed source replay differs from committed T13c snapshot"
        )


def refresh_policy_source(source_root: Path) -> None:
    policy = load(POLICY)
    policy["source_snapshot"] = extract_source_snapshot(source_root, policy)
    POLICY.write_text(stable(policy), encoding="utf-8", newline="\n")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--verify-source", type=Path)
    parser.add_argument("--refresh-policy-source", type=Path)
    args = parser.parse_args()
    if args.refresh_policy_source is not None:
        if args.check or args.verify_source is not None:
            parser.error("--refresh-policy-source is exclusive")
        refresh_policy_source(args.refresh_policy_source)
        print("Refreshed pinned T13c source snapshot.")
        return 0
    if args.verify_source is not None:
        verify_source(args.verify_source, load(POLICY))
    if args.check:
        errors = check()
        if errors:
            for error in errors:
                print(error)
            return 1
        print("T13c machine/energy denominators are current.")
        return 0
    machine, energy = write()
    print(json.dumps({
        "raw_registrations": machine["counts"][
            "raw_expanded_registrations"
        ],
        "canonical_kinds": machine["counts"]["canonical_kinds"],
        "tiered": machine["counts"]["tiered"],
        "fixed": machine["counts"]["fixed"],
        "excluded": machine["counts"]["excluded_expanded_registrations"],
        "energy_identities": energy["counts"]["identities"],
    }, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
