#!/usr/bin/env python3
"""Ratchet GameTest namespaces, crashing lookups, and stand-in fixtures.

Existing violation keys may stay in tools/gametest_hygiene_baseline.json.
A key that is not in that file fails. Orphan namespaces and missing
structure templates always fail.
"""
from __future__ import annotations

import argparse
import json
import re
import shutil
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

GRIDS_PATH = ROOT / "tools" / "gametest_grids.json"
BASELINE_PATH = ROOT / "tools" / "gametest_hygiene_baseline.json"
INVENTORY_PATH = (
    ROOT / "tools" / "waves" / "prep" / "test-authoring-workflow" / "inventory.json"
)
TEST_JAVA = ROOT / "src" / "test" / "java"
MATERIALS = ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft" / "materials"
GATE = (
    ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
PREFIXES = (
    ROOT
    / "src"
    / "main"
    / "java"
    / "com"
    / "masson"
    / "cruciblecraft"
    / "api"
    / "material"
    / "MaterialPrefixes.java"
)
STRUCTURE_SOURCE = (
    ROOT / "src" / "main" / "resources" / "data" / "cruciblecraft_default_grid"
)

RATCHET_KINDS = {
    "cast_block_entity",
    "or_else_throw",
    "optional_get",
    "unknown_fixture",
    "unknown_pipe",
    "projection_count",
}
FATAL_KINDS = {
    "orphan_namespace",
    "missing_structure",
    "bootstrap_holder",
    "inventory_mismatch",
}

CAST_BLOCK_ENTITY = re.compile(
    r"\([^()\n]+\)\s*[A-Za-z_][\w.]*\.getBlockEntity\s*\("
)
OR_ELSE_THROW = re.compile(r"\.orElseThrow\s*\(")
OPTIONAL_GET = re.compile(
    r"\.(?:findFirst|findAny)\(\)\s*\.get\s*\(|"
    r"\.byKey\([^;\n]{0,240}\)\s*\.get\s*\("
)
ASSERT_LITERAL = re.compile(
    r"\bassert(?:Equals|True|That)\([^;\n]*(?<!\.)\b(\d{3,})L?\b"
)
FIXTURE = re.compile(
    r"(?:MaterialLookup\.(?:tryStack|stack)|"
    r"GameTestFixtures\.require(?:MaterialStack|Pipe)|"
    r"ModItems\.materialItem|"
    r"(?<![\w.])material)\(\s*(?:[A-Za-z_]\w*\s*,\s*)?"
    r"\"([a-z0-9_]+)\"\s*,\s*MaterialPrefixes\.([A-Z0-9_]+)",
    re.S,
)
PREFIX_FIELD = re.compile(
    r"public static final MaterialPrefix ([A-Z0-9_]+)\s*=\s*builtin\(\"([a-z0-9_]+)\"\)",
    re.S,
)
NAMESPACE_CONSTANT = re.compile(
    r"static\s+final\s+String\s+NAMESPACE\s*=\s*\"([^\"]+)\"",
    re.S,
)
GT6_CITE = re.compile(r"gt6-source\s*:", re.I)
HYGIENE_NEGATIVE = re.compile(r"hygiene-negative\s*:")

FLUID_SPEC_TO_FORM = {
    "pipeTiny": "tiny_fluid_pipe",
    "pipeSmall": "small_fluid_pipe",
    "pipeMedium": "fluid_pipe",
    "pipeLarge": "large_fluid_pipe",
    "pipeHuge": "huge_fluid_pipe",
}
ITEM_SPEC_TO_FORM = {
    "pipeMedium": "item_pipe",
    "pipeLarge": "large_item_pipe",
    "pipeHuge": "huge_item_pipe",
}


def load_json(path: Path) -> dict[str, Any]:
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path: Path, document: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def load_grids(root: Path = ROOT) -> dict[str, Any]:
    return load_json(root / "tools" / "gametest_grids.json")


def grid_namespaces(grid: dict[str, Any]) -> list[str]:
    names = grid.get("namespaces")
    if names:
        return [str(name) for name in names]
    return [str(grid["namespace"])]


def allowed_namespaces(document: dict[str, Any]) -> set[str]:
    names = {str(document["bootstrap"]["namespace"])}
    for grid in document["grids"]:
        names.update(grid_namespaces(grid))
    return names


def namespace_grid(document: dict[str, Any]) -> dict[str, str]:
    found: dict[str, str] = {}
    for grid in document["grids"]:
        for name in grid_namespaces(grid):
            found[name] = str(grid["id"])
    bootstrap = str(document["bootstrap"]["namespace"])
    found[bootstrap] = "bootstrap"
    return found


def relative(root: Path, path: Path) -> str:
    return path.relative_to(root).as_posix()


def java_files(root: Path = ROOT) -> list[Path]:
    base = root / "src" / "test" / "java"
    return sorted(base.rglob("*.java"))


def is_gametest_source(text: str) -> bool:
    return any(line.strip().startswith("@GameTest") for line in text.splitlines())


def strip_comments(text: str) -> str:
    without_block = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//.*?$", "", without_block, flags=re.M)


def holder_expression(text: str) -> str | None:
    for line in text.splitlines():
        stripped = line.strip()
        if not stripped.startswith("@GameTestHolder"):
            continue
        match = re.search(r"@GameTestHolder\(\s*([^)\n]+)\s*\)", stripped)
        if match:
            return match.group(1).strip()
    return None


def namespace_constants(files: list[tuple[Path, str]]) -> dict[str, str]:
    found: dict[str, str] = {}
    for path, text in files:
        match = NAMESPACE_CONSTANT.search(text)
        if match:
            found[path.stem] = match.group(1)
    return found


def resolve_namespace(
    expression: str | None,
    constants: dict[str, str],
    bootstrap: str,
) -> str | None:
    if expression is None:
        return None
    if expression.startswith('"') and expression.endswith('"'):
        return expression[1:-1]
    if expression == "CrucibleCraft.MODID":
        return bootstrap
    if expression.endswith(".NAMESPACE"):
        return constants.get(expression[: -len(".NAMESPACE")])
    return None


def _is_game_test_annotation(stripped: str) -> bool:
    if not stripped.startswith("@GameTest"):
        return False
    rest = stripped[len("@GameTest"):]
    return not rest or rest[0] in "( \t"


def test_method_names(text: str) -> list[str]:
    names: list[str] = []
    pending = 0
    for line in text.splitlines():
        stripped = line.strip()
        if _is_game_test_annotation(stripped):
            pending = 40
            continue
        if pending <= 0:
            continue
        pending -= 1
        match = re.search(r"\bvoid\s+(\w+)\s*\(", line)
        if match:
            names.append(match.group(1))
            pending = 0
    return names


def gametest_annotation_count(text: str) -> int:
    return sum(
        1
        for line in text.splitlines()
        if _is_game_test_annotation(line.strip())
    )


def classify_filename(name: str) -> str:
    lowered = name.lower()
    if "drill" in lowered:
        return "machines"
    if (
        lowered.startswith("large")
        or "multiblock" in lowered
        or "distillation" in lowered
        or "implosion" in lowered
        or "cokeoven" in lowered
        or "boiler" in lowered
    ):
        return "multiblock"
    if lowered.startswith("mte"):
        return "content"
    if lowered.startswith("gt6"):
        return "content"
    if any(
        token in lowered
        for token in (
            "pipe",
            "cover",
            "network",
            "logistics",
            "displaycpu",
            "dangerous",
        )
    ):
        return "logistics"
    if "processingmachine" in lowered or lowered.startswith("machineruntime"):
        return "machines"
    if any(
        token in lowered
        for token in (
            "energy",
            "turbine",
            "fission",
            "nuclear",
            "fusion",
            "batter",
            "transformer",
            "cooler",
            "flux",
            "converter",
            "electricconversion",
            "plasma",
            "coil",
            "euwire",
            "eucable",
            "eumissing",
            "heatexchanger",
            "steam",
            "quantum",
            "massfab",
            "prerequisite",
        )
    ):
        return "energy"
    if any(
        token in lowered
        for token in (
            "machine",
            "bath",
            "oven",
            "melter",
            "slicer",
            "sanding",
            "roll",
            "cluster",
            "laminator",
            "pressure",
            "hammer",
            "nanofab",
            "printer",
            "injector",
            "loom",
            "washer",
            "shredder",
            "mixer",
            "centrifuge",
            "crusher",
            "sluice",
            "squeezer",
            "autoclave",
            "fermenter",
            "coagulator",
            "electrolyzer",
            "smelter",
            "compressor",
            "drying",
            "roaster",
            "assembler",
            "canner",
            "matrix",
        )
    ):
        return "machines"
    if any(
        token in lowered
        for token in (
            "worldgen",
            "pebble",
            "tree",
            "crop",
            "dungeon",
            "surface",
            "smallore",
            "bedrock",
        )
    ) or re.search(r"(?<!red)stone", lowered) or re.search(r"(?<!bed)rock", lowered):
        return "worldgen"
    return "content"


def prefix_fields(root: Path = ROOT) -> dict[str, str]:
    text = (root / PREFIXES.relative_to(ROOT)).read_text(encoding="utf-8")
    if root != ROOT:
        text = (root / "src/main/java/com/masson/cruciblecraft/api/material/MaterialPrefixes.java").read_text(
            encoding="utf-8"
        )
    return {name: path for name, path in PREFIX_FIELD.findall(text)}


def _pipe_forms(specs: dict[str, Any], mapping: dict[str, str], kind: str) -> set[str]:
    forms = {mapping[key] for key in specs if key in mapping}
    if kind == "fluid":
        if "pipeMedium" in specs:
            forms.add("quadruple_fluid_pipe")
        if "pipeSmall" in specs:
            forms.add("nonuple_fluid_pipe")
    else:
        if "pipeMedium" in specs:
            forms.add("restrictive_item_pipe")
        if "pipeLarge" in specs:
            forms.add("large_restrictive_item_pipe")
        if "pipeHuge" in specs:
            forms.add("huge_restrictive_item_pipe")
    return forms


def pipe_pairs(root: Path = ROOT) -> set[tuple[str, str]]:
    pairs: set[tuple[str, str]] = set()
    materials = root / "src/main/resources/data/cruciblecraft/materials"
    if not materials.is_dir():
        return pairs
    for path in materials.glob("*.json"):
        document = json.loads(path.read_text(encoding="utf-8"))
        if not isinstance(document, dict):
            continue
        material_id = str(document.get("id") or path.stem)
        metadata = document.get("gt6_metadata") or {}
        if not isinstance(metadata, dict):
            metadata = {}
        properties = document.get("pipe_properties") or metadata.get("pipe_properties") or {}
        if not isinstance(properties, dict):
            continue
        fluid = properties.get("fluid_by_specification") or {}
        item = properties.get("item_by_specification") or {}
        if not isinstance(fluid, dict):
            fluid = {}
        if not isinstance(item, dict):
            item = {}
        for form in _pipe_forms(fluid, FLUID_SPEC_TO_FORM, "fluid"):
            pairs.add((material_id, form))
        for form in _pipe_forms(item, ITEM_SPEC_TO_FORM, "item"):
            pairs.add((material_id, form))
    return pairs


def form_pairs(root: Path = ROOT) -> set[tuple[str, str]]:
    pairs: set[tuple[str, str]] = set()
    materials = root / "src/main/resources/data/cruciblecraft/materials"
    if materials.is_dir():
        for path in materials.glob("*.json"):
            document = json.loads(path.read_text(encoding="utf-8"))
            if not isinstance(document, dict):
                continue
            material_id = str(document.get("id") or path.stem)
            for form in (document.get("form_items") or {}):
                pairs.add((material_id, str(form)))
            for flag in document.get("generation_flags") or []:
                text = str(flag)
                marker = "generates_"
                if marker in text:
                    pairs.add((material_id, text.split(marker, 1)[1]))
    gate_path = root / "src/main/resources/data/cruciblecraft/material_registration_gate.json"
    if gate_path.is_file():
        gate = json.loads(gate_path.read_text(encoding="utf-8"))
        for value in gate.values():
            if not isinstance(value, dict):
                continue
            for material_id, forms in value.items():
                if isinstance(forms, list) and all(isinstance(form, str) for form in forms):
                    for form in forms:
                        pairs.add((str(material_id), form))
    return pairs


def _line_number(text: str, index: int) -> int:
    return text.count("\n", 0, index) + 1


def _excerpt(text: str) -> str:
    collapsed = re.sub(r"\s+", " ", text).strip()
    return collapsed[:180]


def _violation(
    path: str,
    kind: str,
    line: int,
    excerpt: str,
) -> dict[str, Any]:
    return {
        "path": path,
        "kind": kind,
        "line": line,
        "excerpt": excerpt,
    }


def violation_key(item: dict[str, Any]) -> str:
    return f"{item['path']}|{item['kind']}|{item['excerpt']}"


def _previous_has(text: str, index: int, pattern: re.Pattern[str], window: int = 8) -> bool:
    line = _line_number(text, index)
    lines = text.splitlines()
    start = max(0, line - 1 - window)
    return any(pattern.search(lines[cursor]) for cursor in range(start, line - 1))


def collect_text_violations(
    relative_path: str,
    text: str,
    *,
    allowed: set[str],
    bootstrap: str,
    bootstrap_file: str,
    constants: dict[str, str],
    prefixes: dict[str, str],
    pipes: set[tuple[str, str]],
    forms: set[tuple[str, str]],
    pipe_forms: set[str],
) -> list[dict[str, Any]]:
    found: list[dict[str, Any]] = []
    if is_gametest_source(text):
        expression = holder_expression(text)
        namespace = resolve_namespace(expression, constants, bootstrap)
        if namespace is None:
            found.append(_violation(
                relative_path,
                "orphan_namespace",
                1,
                f"unresolved holder {expression}",
            ))
        elif namespace not in allowed:
            found.append(_violation(
                relative_path,
                "orphan_namespace",
                1,
                namespace,
            ))
        elif namespace == bootstrap and relative_path != bootstrap_file:
            found.append(_violation(
                relative_path,
                "bootstrap_holder",
                1,
                relative_path,
            ))
        if gametest_annotation_count(text) != len(test_method_names(text)):
            found.append(_violation(
                relative_path,
                "inventory_mismatch",
                1,
                relative_path,
            ))
        code = strip_comments(text)
        for match in CAST_BLOCK_ENTITY.finditer(code):
            found.append(_violation(
                relative_path,
                "cast_block_entity",
                _line_number(code, match.start()),
                _excerpt(match.group(0)),
            ))
        for match in OR_ELSE_THROW.finditer(code):
            found.append(_violation(
                relative_path,
                "or_else_throw",
                _line_number(code, match.start()),
                _excerpt(match.group(0)),
            ))
        for match in OPTIONAL_GET.finditer(code):
            found.append(_violation(
                relative_path,
                "optional_get",
                _line_number(code, match.start()),
                _excerpt(match.group(0)),
            ))
        for match in FIXTURE.finditer(text):
            if _previous_has(text, match.start(), HYGIENE_NEGATIVE):
                continue
            material_id, field = match.group(1), match.group(2)
            form = prefixes.get(field)
            excerpt = _excerpt(match.group(0))
            line = _line_number(text, match.start())
            if form is None:
                found.append(_violation(
                    relative_path, "unknown_fixture", line, excerpt
                ))
                continue
            if form in pipe_forms or "pipe" in form:
                if (material_id, form) not in pipes:
                    found.append(_violation(
                        relative_path, "unknown_pipe", line, excerpt
                    ))
            elif (material_id, form) not in forms:
                found.append(_violation(
                    relative_path, "unknown_fixture", line, excerpt
                ))
    lines = text.splitlines()
    for index, line in enumerate(lines, start=1):
        if not ASSERT_LITERAL.search(line):
            continue
        if GT6_CITE.search(line):
            continue
        previous = lines[index - 2] if index >= 2 else ""
        if GT6_CITE.search(previous):
            continue
        if line.strip().startswith("//") or line.strip().startswith("*"):
            continue
        found.append(_violation(
            relative_path,
            "projection_count",
            index,
            _excerpt(line),
        ))
    return found


def indexed_sources(
    root: Path = ROOT,
) -> list[tuple[Path, str, str | None]]:
    document = load_grids(root)
    bootstrap = str(document["bootstrap"]["namespace"])
    files = [(path, path.read_text(encoding="utf-8")) for path in java_files(root)]
    constants = namespace_constants(files)
    indexed: list[tuple[Path, str, str | None]] = []
    for path, text in files:
        if not is_gametest_source(text):
            continue
        expression = holder_expression(text)
        namespace = resolve_namespace(expression, constants, bootstrap)
        indexed.append((path, text, namespace))
    return indexed


def iter_tests(root: Path = ROOT) -> list[dict[str, Any]]:
    document = load_grids(root)
    grids = namespace_grid(document)
    rows: list[dict[str, Any]] = []
    for path, text, namespace in indexed_sources(root):
        rel = relative(root, path)
        for method in test_method_names(text):
            rows.append({
                "class": path.stem,
                "method": method,
                "file": rel,
                "namespace": namespace,
                "grid": grids.get(namespace or "", None),
            })
    return rows


def structure_violations(root: Path, namespaces: set[str]) -> list[dict[str, Any]]:
    found: list[dict[str, Any]] = []
    for namespace in sorted(namespaces):
        if namespace is None:
            continue
        path = (
            root
            / "src"
            / "main"
            / "resources"
            / "data"
            / namespace
            / "structure"
            / "empty.nbt"
        )
        if not path.is_file():
            found.append(_violation(
                path.relative_to(root).as_posix() if path.is_relative_to(root) else str(path),
                "missing_structure",
                1,
                namespace,
            ))
    return found


def collect(root: Path = ROOT) -> list[dict[str, Any]]:
    document = load_grids(root)
    allowed = allowed_namespaces(document)
    bootstrap = str(document["bootstrap"]["namespace"])
    bootstrap_file = str(document["bootstrap"]["file"])
    files = [(path, path.read_text(encoding="utf-8")) for path in java_files(root)]
    constants = namespace_constants(files)
    prefixes = prefix_fields(root)
    pipes = pipe_pairs(root)
    forms = form_pairs(root)
    pipe_forms = {form for _material, form in pipes}
    found: list[dict[str, Any]] = []
    used: set[str] = set()
    for path, text in files:
        rel = relative(root, path)
        found.extend(collect_text_violations(
            rel,
            text,
            allowed=allowed,
            bootstrap=bootstrap,
            bootstrap_file=bootstrap_file,
            constants=constants,
            prefixes=prefixes,
            pipes=pipes,
            forms=forms,
            pipe_forms=pipe_forms,
        ))
        if is_gametest_source(text):
            namespace = resolve_namespace(holder_expression(text), constants, bootstrap)
            if namespace:
                used.add(namespace)
    found.extend(structure_violations(root, used))
    return found


def load_baseline(root: Path = ROOT) -> list[dict[str, Any]]:
    path = root / "tools" / "gametest_hygiene_baseline.json"
    if not path.is_file():
        return []
    document = load_json(path)
    return list(document.get("violations") or [])


def ratchet_errors(
    current: list[dict[str, Any]],
    baseline_keys: set[str],
) -> list[str]:
    errors: list[str] = []
    for item in current:
        if item["kind"] in FATAL_KINDS:
            errors.append(
                f"{item['path']}:{item['line']}: {item['kind']}: {item['excerpt']}"
            )
            continue
        if violation_key(item) not in baseline_keys:
            errors.append(
                "new hygiene violation "
                f"{item['path']}:{item['line']}: {item['kind']}: {item['excerpt']}"
            )
    return errors


def check(root: Path = ROOT) -> list[str]:
    errors: list[str] = []
    baseline_path = root / "tools" / "gametest_hygiene_baseline.json"
    if not baseline_path.is_file():
        errors.append("missing tools/gametest_hygiene_baseline.json")
    errors.extend(ratchet_errors(
        collect(root),
        {violation_key(item) for item in load_baseline(root)},
    ))
    return errors


def write_baseline(root: Path = ROOT) -> int:
    current = [
        item for item in collect(root) if item["kind"] in RATCHET_KINDS
    ]
    current.sort(key=lambda item: (item["path"], item["kind"], item["line"], item["excerpt"]))
    write_json(
        root / "tools" / "gametest_hygiene_baseline.json",
        {"schema_version": 1, "violations": current},
    )
    return len(current)


def _grid_namespace(document: dict[str, Any], grid_id: str) -> str:
    for grid in document["grids"]:
        if grid["id"] == grid_id:
            names = grid_namespaces(grid)
            if len(names) != 1:
                raise ValueError(f"{grid_id} has no single namespace")
            return names[0]
    raise ValueError(f"unknown grid {grid_id}")


def migration_plan(root: Path = ROOT) -> list[dict[str, str]]:
    document = load_grids(root)
    allowed = allowed_namespaces(document)
    bootstrap = str(document["bootstrap"]["namespace"])
    bootstrap_file = str(document["bootstrap"]["file"])
    files = [(path, path.read_text(encoding="utf-8")) for path in java_files(root)]
    constants = namespace_constants(files)
    owners: dict[str, Path] = {}
    for path, text in files:
        if NAMESPACE_CONSTANT.search(text):
            owners[path.stem] = path
    actions: list[dict[str, str]] = []
    seen_owners: set[str] = set()
    for path, text in files:
        if not is_gametest_source(text):
            continue
        rel = relative(root, path)
        if rel == bootstrap_file:
            continue
        expression = holder_expression(text)
        namespace = resolve_namespace(expression, constants, bootstrap)
        if namespace in allowed and not (
            namespace == bootstrap and rel != bootstrap_file
        ):
            continue
        if expression is None:
            grid_id = classify_filename(path.stem)
            actions.append({
                "file": rel,
                "old": "",
                "grid": grid_id,
                "new": _grid_namespace(document, grid_id),
                "mode": "insert",
            })
            continue
        if expression.endswith(".NAMESPACE"):
            owner = expression[: -len(".NAMESPACE")]
            owner_path = owners.get(owner, path)
            if str(owner_path) in seen_owners:
                continue
            seen_owners.add(str(owner_path))
            grid_id = classify_filename(owner_path.stem)
            actions.append({
                "file": relative(root, owner_path),
                "old": namespace or "",
                "grid": grid_id,
                "new": _grid_namespace(document, grid_id),
                "mode": "constant",
            })
            continue
        grid_id = classify_filename(path.stem)
        actions.append({
            "file": rel,
            "old": namespace or expression or "",
            "grid": grid_id,
            "new": _grid_namespace(document, grid_id),
            "mode": "holder",
        })
    return actions


def _replace_constant(text: str, old: str, new: str) -> str:
    pattern = re.compile(
        r"(static\s+final\s+String\s+NAMESPACE\s*=\s*)\"" + re.escape(old) + r"\"",
        re.S,
    )
    updated, count = pattern.subn(r'\1"' + new + '"', text, count=1)
    if count != 1:
        raise ValueError(f"namespace constant {old!r} was not rewritten")
    return updated


def _insert_holder(text: str, new: str) -> str:
    annotation = f'@GameTestHolder("{new}")\n'
    marker = "@PrefixGameTestTemplate"
    if marker in text:
        return text.replace(marker, annotation + marker, 1)
    match = re.search(r"^public (?:final )?class ", text, re.M)
    if not match:
        raise ValueError("no class declaration for a missing GameTestHolder")
    return text[: match.start()] + annotation + text[match.start() :]


def _replace_holder(text: str, new: str) -> str:
    updated, count = re.subn(
        r"@GameTestHolder\(\s*CrucibleCraft\.MODID\s*\)",
        f'@GameTestHolder("{new}")',
        text,
        count=1,
    )
    if count == 1:
        return updated
    updated, count = re.subn(
        r'@GameTestHolder\(\s*"[^"]+"\s*\)',
        f'@GameTestHolder("{new}")',
        text,
        count=1,
    )
    if count != 1:
        raise ValueError("holder annotation was not rewritten")
    return updated


def _copy_structures(root: Path, namespaces: set[str]) -> None:
    for namespace in sorted(namespaces):
        for leaf in (
            Path("structure") / "empty.nbt",
            Path("gametest") / "structure" / "empty.nbt",
        ):
            source = root / "src/main/resources/data/cruciblecraft_default_grid" / leaf
            destination = root / "src/main/resources/data" / namespace / leaf
            if destination.is_file() or not source.is_file():
                continue
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, destination)


def write_inventory(root: Path = ROOT, before: dict[tuple[str, str], str] | None = None) -> None:
    rows = iter_tests(root)
    previous: dict[tuple[str, str], str] = {}
    inventory_path = root / "tools/waves/prep/test-authoring-workflow/inventory.json"
    if before is None and inventory_path.is_file():
        old = load_json(inventory_path)
        for row in old.get("tests") or []:
            previous[(str(row["class"]), str(row["method"]))] = str(
                row.get("namespace_before") or row.get("namespace") or ""
            )
    else:
        previous = before or {}
    tests = []
    for row in rows:
        key = (row["class"], row["method"])
        tests.append({
            "class": row["class"],
            "method": row["method"],
            "file": row["file"],
            "namespace_before": previous.get(key, row["namespace"]),
            "namespace": row["namespace"],
            "grid": row["grid"],
        })
    write_json(inventory_path, {
        "schema_version": 1,
        "note": (
            "namespace_before is the holder namespace before this card merged "
            "single-card namespaces into domain grids. Fresh execution is one "
            "GameTestServer run per release grid after that holder-only move."
        ),
        "test_count": len(tests),
        "tests": tests,
    })


def apply_migration(root: Path = ROOT) -> list[dict[str, str]]:
    before_rows = iter_tests(root)
    before = {
        (row["class"], row["method"]): str(row["namespace"] or "")
        for row in before_rows
    }
    actions = migration_plan(root)
    for action in actions:
        path = root / action["file"]
        text = path.read_text(encoding="utf-8")
        if action["mode"] == "constant":
            updated = _replace_constant(text, action["old"], action["new"])
        elif action["mode"] == "insert":
            updated = _insert_holder(text, action["new"])
        else:
            updated = _replace_holder(text, action["new"])
        path.write_text(updated, encoding="utf-8")
    _copy_structures(root, {action["new"] for action in actions})
    write_inventory(root, before)
    return actions


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--write-baseline", action="store_true")
    parser.add_argument("--inventory", action="store_true")
    parser.add_argument("--migrate", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)
    if args.dry_run or args.migrate:
        actions = migration_plan() if args.dry_run else apply_migration()
        counts: dict[str, int] = {}
        for action in actions:
            counts[action["grid"]] = counts.get(action["grid"], 0) + 1
            print(f"{action['file']}: {action['old']} -> {action['new']}")
        print("files", len(actions), counts)
        if args.dry_run or (args.migrate and not args.check):
            return 0
    if args.inventory:
        write_inventory()
        print(f"wrote {INVENTORY_PATH.relative_to(ROOT).as_posix()}")
    if args.write_baseline:
        count = write_baseline()
        print(f"wrote {count} baseline violations")
        return 0
    errors = check()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    if args.check or not any((args.inventory, args.migrate, args.dry_run)):
        print("gametest hygiene ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
