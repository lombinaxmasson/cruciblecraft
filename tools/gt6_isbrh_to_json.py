#!/usr/bin/env python3
"""Convert GT6 RendererBlockTextured empty-state geometry into JSON models.

Evaluates getRenderPasses2 / usesRenderPass2 / setBlockBounds2 / getTexture2
for isolated blocks facing south with no molten, tank fluid, workpieces, or
NEI overlays. Faces whose texture is null are omitted.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any, Callable

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import gt6_isbrh as isbrh

GT6_JAVA = ROOT / "gt6_code" / "gregtech6" / "src" / "main" / "java"
OUT_DIR = ROOT / "textures" / "gt6模型"
REPORT_PATH = ROOT / "docs" / "history" / "art" / "GT6模型批量转换报告.md"
CC_ANVIL = ROOT / "src" / "main" / "resources" / "assets" / "cruciblecraft" / "models" / "block" / "anvil.json"

METHOD_NAMES = (
    "getRenderPasses2",
    "usesRenderPass2",
    "setBlockBounds2",
    "getTexture2",
)

CLASS_NAMES = {
    "MultiTileEntityAnvil": "砧",
    "MultiTileEntityBasin": "浇铸盆",
    "MultiTileEntityBathingPot": "浸洗盆",
    "MultiTileEntityBathingPotTable": "浸洗盆桌",
    "MultiTileEntityBathingPotTableWood": "木浸洗盆桌",
    "MultiTileEntityBookShelf": "书架",
    "MultiTileEntitySensor": "传感器",
    "MultiTileEntityAneutronicFusion": "聚变电池",
    "MultiTileEntityBatteryAdvEU128": "高级 EU 电池 (128)",
    "MultiTileEntityBatteryAdvEU2048": "高级 EU 电池 (2048)",
    "MultiTileEntityBatteryAdvEU32": "高级 EU 电池 (32)",
    "MultiTileEntityBatteryAdvEU512": "高级 EU 电池 (512)",
    "MultiTileEntityBatteryAdvEU8": "高级 EU 电池 (8)",
    "MultiTileEntityBatteryEU128": "EU 电池 (128)",
    "MultiTileEntityBatteryEU2048": "EU 电池 (2048)",
    "MultiTileEntityBatteryEU32": "EU 电池 (32)",
    "MultiTileEntityBatteryEU512": "EU 电池 (512)",
    "MultiTileEntityBatteryEU8": "EU 电池 (8)",
    "MultiTileEntityPowerCell": "能量电池",
    "MultiTileEntityBatteryLU128": "LU 电池 (128)",
    "MultiTileEntityBatteryLU2048": "LU 电池 (2048)",
    "MultiTileEntityBatteryLU32": "LU 电池 (32)",
    "MultiTileEntityBatteryLU512": "LU 电池 (512)",
    "MultiTileEntityBatteryLU8": "LU 电池 (8)",
    "MultiTileEntityBatteryLU8192": "LU 电池 (8192)",
    "MultiTileEntityZPM": "ZPM 电池",
    "MultiTileEntityEngineElectric": "电动引擎",
    "MultiTileEntityEngineFlux": "通量引擎",
    "MultiTileEntityEngineSteam": "蒸汽引擎",
    "MultiTileEntityLaserBuildcraft": "BC 激光",
    "MultiTileEntityReactorCore1x1": "反应堆堆芯 1×1",
    "MultiTileEntityReactorCore2x2": "反应堆堆芯 2×2",
    "MultiTileEntityReactorRodBase": "燃料棒底座",
    "MultiTileEntityBottleCrate": "瓶装箱",
    "MultiTileEntityEnderGarbageBin": "末影垃圾桶",
    "MultiTileEntityHopper": "漏斗",
    "MultiTileEntityMassStorageBarrel": "大容量桶",
    "MultiTileEntityMassStorageBox": "大容量箱",
    "MultiTileEntityMassStorageLogistics": "大容量储物（物流）",
    "MultiTileEntityMassStorageStandard": "大容量储物（标准）",
    "MultiTileEntityQueueHopper": "队列漏斗",
    "MultiTileEntityCertificate": "证书牌",
    "MultiTileEntityCrucible": "坩埚（多方块）",
    "MultiTileEntityLargeTurbine": "大涡轮转子",
    "MultiTileEntityBush": "灌木",
    "MultiTileEntityButtonAdvanced": "高级按钮",
    "MultiTileEntityBarometerGasCylinder": "储气瓶（气压计）",
    "MultiTileEntityCell": "流体单元",
    "MultiTileEntityCup": "杯子",
    "MultiTileEntityJug": "水壶",
    "MultiTileEntityMeasuringPot": "量杯",
    "MultiTileEntityThermos": "保温瓶",
    "MultiTileEntityCrank": "手摇曲柄",
    "MultiTileEntityCrossing": "十字轨",
    "MultiTileEntityDustFunnel": "粉末漏斗",
    "MultiTileEntityDynamite": "炸药",
    "MultiTileEntityFaucet": "浇铸口",
    "MultiTileEntityFluidCapNozzle": "封盖喷嘴",
    "MultiTileEntityFluidFunnel": "流体漏斗",
    "MultiTileEntityFluidNozzle": "流体喷嘴",
    "MultiTileEntityFluidTap": "流体龙头",
    "MultiTileEntityGrindStone": "砂轮",
    "MultiTileEntityJuicer": "榨汁机",
    "MultiTileEntityMixingBowl": "搅拌碗",
    "MultiTileEntityMixingBowlTable": "搅拌碗桌",
    "MultiTileEntityMold": "坩埚模具",
    "MultiTileEntityMoldCoinage": "压币模具",
    "MultiTileEntityMortar": "研钵",
    "MultiTileEntityPlantPot": "花盆",
    "MultiTileEntityRope": "绳子",
    "MultiTileEntitySapBag": "树脂袋",
    "MultiTileEntityScaffold": "脚手架",
    "MultiTileEntitySiftingTable": "筛台",
    "MultiTileEntitySmeltery": "小冶炼炉",
}


@dataclass
class ConvertResult:
    class_name: str
    elements: list[dict[str, Any]] = field(default_factory=list)
    error: str | None = None
    notes: list[str] = field(default_factory=list)

    @property
    def ok(self) -> bool:
        return self.error is None


def strip_comments(source: str) -> str:
    source = re.sub(r"/\*.*?\*/", "", source, flags=re.S)
    lines = []
    for line in source.splitlines():
        in_string = False
        out = []
        i = 0
        while i < len(line):
            ch = line[i]
            if ch == '"' and not in_string:
                in_string = True
                out.append(ch)
            elif ch == '"' and in_string:
                in_string = False
                out.append(ch)
            elif not in_string and ch == "/" and i + 1 < len(line) and line[i + 1] == "/":
                break
            else:
                out.append(ch)
            i += 1
        lines.append("".join(out))
    return "\n".join(lines)


def find_matching(source: str, open_index: int, open_ch: str = "(", close_ch: str = ")") -> int:
    depth = 0
    i = open_index
    in_string = False
    while i < len(source):
        ch = source[i]
        if ch == '"' and (i == 0 or source[i - 1] != "\\"):
            in_string = not in_string
        elif not in_string:
            if ch == open_ch:
                depth += 1
            elif ch == close_ch:
                depth -= 1
                if depth == 0:
                    return i
        i += 1
    raise ValueError(f"unbalanced {open_ch}{close_ch} at {open_index}")


def extract_method_body(source: str, name: str) -> str | None:
    pattern = re.compile(
        rf"(?:public|protected|private)\s+(?:static\s+)?(?:final\s+)?"
        rf"(?:boolean|int|ITexture)\s+{re.escape(name)}\s*\("
    )
    match = pattern.search(source)
    if not match:
        return None
    paren = match.end() - 1
    close_paren = find_matching(source, paren)
    rest = source[close_paren + 1 :].lstrip()
    if rest.startswith(";"):
        return None
    if not rest.startswith("{"):
        return None
    body_open = close_paren + 1 + source[close_paren + 1 :].index("{")
    body_close = find_matching(source, body_open, "{", "}")
    return source[body_open + 1 : body_close]


def parse_extends(source: str) -> str | None:
    match = re.search(r"class\s+\w+\s+extends\s+(\w+)", source)
    if not match:
        return None
    return match.group(1)


_CLASS_FILE_CACHE: dict[str, Path] | None = None


def class_file_index() -> dict[str, Path]:
    global _CLASS_FILE_CACHE
    if _CLASS_FILE_CACHE is None:
        _CLASS_FILE_CACHE = {path.stem: path for path in GT6_JAVA.rglob("*.java")}
    return _CLASS_FILE_CACHE


def load_class_source(class_name: str) -> str | None:
    path = class_file_index().get(class_name)
    if path is None:
        return None
    return strip_comments(path.read_text(encoding="utf-8", errors="replace"))


def collect_class_methods(class_name: str) -> tuple[dict[str, str], dict[str, str], list[str]]:
    methods: dict[str, str] = {}
    statics: dict[str, str] = {}
    notes: list[str] = []
    seen: set[str] = set()
    current: str | None = class_name
    while current and current not in seen:
        seen.add(current)
        source = load_class_source(current)
        if source is None:
            break
        for name in METHOD_NAMES:
            if name in methods:
                continue
            body = extract_method_body(source, name)
            if body is not None:
                methods[name] = body
        statics.update(extract_float_matrices(source))
        current = parse_extends(source)
        if current and current.startswith("TileEntityBase0") and all(
            name in methods for name in ("getRenderPasses2", "setBlockBounds2", "getTexture2")
        ):
            break
    if "usesRenderPass2" not in methods:
        methods["usesRenderPass2"] = "return T;"
    return methods, statics, notes


def extract_float_matrices(source: str) -> dict[str, str]:
    found: dict[str, str] = {}
    for match in re.finditer(
        r"(?:protected\s+|public\s+|private\s+)?(?:static\s+)?(?:final\s+)?"
        r"float\[\]\[\]\s+(\w+)\s*=\s*\{",
        source,
    ):
        name = match.group(1)
        open_index = match.end() - 1
        try:
            close_index = find_matching(source, open_index, "{", "}")
        except ValueError:
            continue
        found[name] = source[open_index : close_index + 1]
    return found


def find_ternary_cond_start(expr: str, qpos: int) -> int:
    depth = 0
    i = qpos - 1
    while i >= 0:
        ch = expr[i]
        if ch in ")]}":
            depth += 1
        elif ch in "([{":
            if depth == 0:
                return i + 1
            depth -= 1
        elif depth == 0 and ch in ",?:":
            return i + 1
        i -= 1
    return 0


def find_false_end(expr: str, colon: int) -> int:
    depth = 0
    i = colon + 1
    while i < len(expr):
        ch = expr[i]
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            if depth == 0:
                return i
            depth -= 1
        elif depth == 0 and ch in ",:":
            return i
        i += 1
    return len(expr)


def find_matching_colon(expr: str, qpos: int) -> int:
    depth = 0
    inner = 0
    i = qpos + 1
    while i < len(expr):
        ch = expr[i]
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        elif depth == 0:
            if ch == "?":
                inner += 1
            elif ch == ":":
                if inner:
                    inner -= 1
                else:
                    return i
        i += 1
    raise ValueError(f"ternary without colon: {expr!r}")


def convert_ternaries(expr: str) -> str:
    while True:
        qpos = None
        colon = None
        end = None
        i = 0
        while i < len(expr):
            if expr[i] == "?":
                try:
                    match_colon = find_matching_colon(expr, i)
                except ValueError:
                    i += 1
                    continue
                if "?" in expr[i + 1 : match_colon]:
                    i += 1
                    continue
                false_end = find_false_end(expr, match_colon)
                if "?" in expr[match_colon + 1 : false_end]:
                    i += 1
                    continue
                qpos = i
                colon = match_colon
                end = false_end
                break
            i += 1
        if qpos is None or colon is None or end is None:
            return expr
        start = find_ternary_cond_start(expr, qpos)
        cond = expr[start:qpos].strip()
        true = expr[qpos + 1 : colon].strip()
        false = expr[colon + 1 : end].strip()
        replacement = f"({true} if {cond} else {false})"
        expr = expr[:start] + replacement + expr[end:]


def convert_not_operators(expr: str) -> str:
    out = []
    i = 0
    while i < len(expr):
        ch = expr[i]
        if ch == "!" and not (i + 1 < len(expr) and expr[i + 1] == "="):
            if i > 0 and expr[i - 1] in "=<>!":
                out.append(ch)
            else:
                out.append(" not ")
        else:
            out.append(ch)
        i += 1
    return "".join(out)


JAVA_TYPE_PREFIX = re.compile(
    r"\b(?:[A-Z][A-Za-z0-9_]*(?:\.[A-Z][A-Za-z0-9_]*)*|"
    r"byte|int|long|short|float|double|boolean|char)\s*(?:\[\s*\])*\s+"
    r"(?=[A-Za-z_]\w*\s*[+\-*/%]?=)"
)
JAVA_NEW_ARRAY = re.compile(r"\bnew\s+[A-Za-z_]\w*\s*(?:\[\s*\])?\s*\{([^{}]*)\}")
JAVA_CAST = re.compile(r"\(\s*(?:byte|int|long|short|float|double|boolean|char|[A-Z][A-Za-z0-9_]*)\s*\)")
JAVA_INSTANCEOF = re.compile(r"[\w.]+(?:\([^)]*\))?\s+instanceof\s+[\w.]+")


def replace_super_calls(expr: str) -> str:
    while True:
        match = re.search(r"\bsuper\.\w+\s*\(", expr)
        if not match:
            return expr
        open_paren = match.end() - 1
        try:
            close = find_matching(expr, open_paren)
        except ValueError:
            return expr
        expr = expr[: match.start()] + "KEEP" + expr[close + 1 :]


def convert_java_expr(expr: str) -> str:
    expr = expr.strip().rstrip(";")
    expr = expr.replace("\t", " ")
    expr = JAVA_NEW_ARRAY.sub(r"[\1]", expr)
    expr = re.sub(r"\bnew\s+([A-Z][A-Za-z0-9_]*)\s*\(", r"\1(", expr)
    expr = JAVA_TYPE_PREFIX.sub("", expr)
    expr = JAVA_CAST.sub("", expr)
    expr = JAVA_INSTANCEOF.sub("False", expr)
    expr = re.sub(r"(\d+(?:\.\d+)?)[fFdDlL]\b", r"\1", expr)
    expr = re.sub(r"\b(\w+)\.length\b", r"len(\1)", expr)
    expr = expr.replace("&&", " and ")
    expr = expr.replace("||", " or ")
    expr = convert_not_operators(expr)
    expr = re.sub(r"\bnull\b", "None", expr)
    expr = convert_ternaries(expr)
    expr = replace_super_calls(expr)
    return expr.strip()


def split_top_level_statements(body: str) -> list[str]:
    statements: list[str] = []
    depth = 0
    start = 0
    i = 0
    while i < len(body):
        ch = body[i]
        if ch in "{(":
            depth += 1
        elif ch in "})":
            depth -= 1
        elif ch == ";" and depth == 0:
            stmt = body[start : i + 1].strip()
            if stmt:
                statements.append(stmt)
            start = i + 1
        elif ch == "{" and depth == 1:
            pass
        i += 1
    tail = body[start:].strip()
    if tail:
        statements.append(tail)
    return statements


def extract_condition(source: str, start: int) -> tuple[str, int]:
    while start < len(source) and source[start].isspace():
        start += 1
    if start >= len(source) or source[start] != "(":
        raise ValueError(f"expected '(' at {start}: {source[start:start+40]!r}")
    end = find_matching(source, start)
    return source[start + 1 : end], end + 1


def _is_keyword_at(source: str, start: int, word: str) -> bool:
    if not source.startswith(word, start):
        return False
    end = start + len(word)
    if end < len(source) and (source[end].isalnum() or source[end] == "_"):
        return False
    return True


def _count_ws(source: str, start: int) -> int:
    n = 0
    while start + n < len(source) and source[start + n].isspace():
        n += 1
    return n


def parse_if_span(source: str, start: int) -> tuple[str, int]:
    _cond, after = extract_condition(source, start + 2)
    _body, pos = parse_block_or_stmt(source, after)
    n = len(source)
    while True:
        while pos < n and source[pos].isspace():
            pos += 1
        if _is_keyword_at(source, pos, "else") and _is_keyword_at(
            source, pos + 4 + _count_ws(source, pos + 4), "if"
        ):
            pos = pos + 4
            while pos < n and source[pos].isspace():
                pos += 1
            _cond, after = extract_condition(source, pos + 2)
            _body, pos = parse_block_or_stmt(source, after)
            continue
        if _is_keyword_at(source, pos, "else"):
            _body, pos = parse_block_or_stmt(source, pos + 4)
        break
    return source[start:pos], pos


def parse_block_or_stmt(source: str, start: int) -> tuple[str, int]:
    n = len(source)
    while start < n and source[start].isspace():
        start += 1
    if start >= n:
        return "", start
    if source[start] == "{":
        end = find_matching(source, start, "{", "}")
        return source[start + 1 : end], end + 1
    if _is_keyword_at(source, start, "switch"):
        _cond, after = extract_condition(source, start + 6)
        while after < n and source[after].isspace():
            after += 1
        if after < n and source[after] == "{":
            end = find_matching(source, after, "{", "}")
            return source[start : end + 1], end + 1
    if _is_keyword_at(source, start, "if"):
        return parse_if_span(source, start)
    depth = 0
    i = start
    while i < n:
        ch = source[i]
        if ch in "{(":
            depth += 1
        elif ch in "})":
            depth -= 1
        elif ch == ";" and depth == 0:
            return source[start : i + 1], i + 1
        i += 1
    return source[start:], n


def parse_switch_cases(body: str) -> list[tuple[list[str], str]]:
    cases: list[tuple[list[str], str]] = []
    labels: list[str] = []
    depth = 0
    i = 0
    body_start: int | None = None
    token_re = re.compile(r"\s*(case\b|default\b)")
    while i < len(body):
        if depth == 0:
            match = token_re.match(body, i)
            if match:
                if labels and body_start is not None:
                    chunk = body[body_start:i].strip()
                    if chunk:
                        cases.append((labels, chunk))
                        labels = []
                kind = match.group(1)
                i = match.end()
                if kind == "case":
                    colon = body.find(":", i)
                    if colon < 0:
                        break
                    labels.append(body[i:colon].strip())
                    i = colon + 1
                    body_start = i
                    continue
                colon = body.find(":", i)
                if colon < 0:
                    break
                labels.append("default")
                i = colon + 1
                body_start = i
                continue
        ch = body[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
        i += 1
    if labels and body_start is not None:
        cases.append((labels, body[body_start:]))
    return cases


_switch_counter = 0


def java_block_to_python(body: str, indent: int = 1) -> str:
    global _switch_counter
    body = body.replace("\t", " ").strip()
    prefix = "    " * indent
    if not body:
        return f"{prefix}pass\n"
    lines: list[str] = []
    i = 0
    n = len(body)
    while i < n:
        while i < n and body[i].isspace():
            i += 1
        if i >= n:
            break
        if body.startswith("else if", i) or (body.startswith("else", i) and (i + 4 >= n or not (body[i + 4].isalnum() or body[i + 4] == "_"))):
            if body.startswith("else if", i):
                cond, after = extract_condition(body, i + 7)
                block, i = parse_block_or_stmt(body, after)
                lines.append(f"{prefix}elif {convert_java_expr(cond)}:\n")
                lines.append(java_block_to_python(block, indent + 1))
            else:
                i += 4
                block, i = parse_block_or_stmt(body, i)
                lines.append(f"{prefix}else:\n")
                lines.append(java_block_to_python(block, indent + 1))
            continue
        if body.startswith("if", i) and (i + 2 >= n or not (body[i + 2].isalnum() or body[i + 2] == "_")):
            cond, after = extract_condition(body, i + 2)
            block, i = parse_block_or_stmt(body, after)
            lines.append(f"{prefix}if {convert_java_expr(cond)}:\n")
            lines.append(java_block_to_python(block, indent + 1))
            continue
        if body.startswith("switch", i) and (i + 6 >= n or not (body[i + 6].isalnum() or body[i + 6] == "_")):
            cond, after = extract_condition(body, i + 6)
            while after < n and body[after].isspace():
                after += 1
            if after >= n or body[after] != "{":
                raise ValueError("switch without body")
            end = find_matching(body, after, "{", "}")
            cases = parse_switch_cases(body[after + 1 : end])
            i = end + 1
            _switch_counter += 1
            sw = f"_sw{_switch_counter}"
            lines.append(f"{prefix}{sw} = {convert_java_expr(cond)}\n")
            first = True
            default_body: str | None = None
            for labels, case_body in cases:
                value_labels = [label for label in labels if label != "default"]
                if "default" in labels and not value_labels:
                    default_body = case_body
                    continue
                tests = " or ".join(f"{sw} == {convert_java_expr(label)}" for label in value_labels)
                keyword = "if" if first else "elif"
                first = False
                lines.append(f"{prefix}{keyword} {tests}:\n")
                lines.append(java_block_to_python(case_body, indent + 1))
            if default_body is not None:
                if first:
                    lines.append(java_block_to_python(default_body, indent))
                else:
                    lines.append(f"{prefix}else:\n")
                    lines.append(java_block_to_python(default_body, indent + 1))
            continue
        if body.startswith("for", i) and (i + 3 >= n or not (body[i + 3].isalnum() or body[i + 3] == "_")):
            _cond, after = extract_condition(body, i + 3)
            _block, i = parse_block_or_stmt(body, after)
            lines.append(f"{prefix}raise RuntimeError('for-loop in render method')\n")
            continue
        if body.startswith("return", i) and (i + 6 >= n or not (body[i + 6].isalnum() or body[i + 6] == "_")):
            stmt, i = parse_block_or_stmt(body, i)
            expr = stmt[len("return") :].strip().rstrip(";")
            if expr:
                lines.append(f"{prefix}return {convert_java_expr(expr)}\n")
            else:
                lines.append(f"{prefix}return\n")
            continue
        stmt, i = parse_block_or_stmt(body, i)
        stmt = JAVA_TYPE_PREFIX.sub("", stmt.strip().rstrip(";"))
        if not stmt:
            continue
        if stmt in {"break", "continue"}:
            continue
        assign = re.match(r"^((?:byte|int|long|short|float|double|boolean)\s+)?([A-Za-z_]\w*)\s*([+\-*/%]?=)\s*(.*)$", stmt)
        if assign:
            name = assign.group(2)
            op = assign.group(3)
            value = convert_java_expr(assign.group(4))
            lines.append(f"{prefix}{name} {op} {value}\n")
            continue
        lines.append(f"{prefix}{convert_java_expr(stmt)}\n")
    if not lines:
        return f"{prefix}pass\n"
    return "".join(lines)


PYTHON_KEYWORDS = {
    "and",
    "as",
    "assert",
    "break",
    "class",
    "continue",
    "def",
    "del",
    "elif",
    "else",
    "except",
    "False",
    "finally",
    "for",
    "from",
    "global",
    "if",
    "import",
    "in",
    "is",
    "lambda",
    "None",
    "nonlocal",
    "not",
    "or",
    "pass",
    "raise",
    "return",
    "True",
    "try",
    "while",
    "with",
    "yield",
}


def seed_unknown_names(source: str, context: dict[str, Any]) -> None:
    builtins_ns = globals()["__builtins__"]
    builtin_names = set(dir(builtins_ns) if not isinstance(builtins_ns, dict) else builtins_ns)
    for ident in set(re.findall(r"\b[A-Za-z_][A-Za-z0-9_]*\b", source)):
        if ident in context or ident in PYTHON_KEYWORDS or ident in builtin_names:
            continue
        context[ident] = isbrh.KEEP


def with_global_fields(header: str, py_body: str) -> str:
    names = sorted(set(re.findall(r"\b(m[A-Z]\w*)\s*=(?!=)", py_body)))
    if names:
        return header + f"    global {', '.join(names)}\n" + py_body
    return header + py_body


def compile_java_method(name: str, java_body: str, extra_locals: dict[str, Any]) -> Callable[..., Any]:
    global _switch_counter
    _switch_counter = 0
    py_body = java_block_to_python(java_body.replace("\t", " "), indent=1)
    if name == "getRenderPasses2":
        header = "def _fn(aBlock, aShouldSideBeRendered):\n"
    elif name == "usesRenderPass2":
        header = "def _fn(aRenderPass, aShouldSideBeRendered):\n"
    elif name == "setBlockBounds2":
        header = "def _fn(aBlock, aRenderPass, aShouldSideBeRendered):\n"
    else:
        header = "def _fn(aBlock, aRenderPass, aSide, aShouldSideBeRendered):\n"
    source = with_global_fields(header, py_body)
    context = isbrh.empty_render_context()
    context.update(extra_locals)
    seed_unknown_names(source, context)
    try:
        exec(source, context, context)
    except Exception as exc:
        raise ValueError(f"{name} translation failed:\n{source}\n{exc}") from exc
    return context["_fn"]


def eval_float_matrix(java_array: str, extra_locals: dict[str, Any]) -> list[list[float]]:
    inner = java_array.strip()
    if inner[0] != "{" or inner[-1] != "}":
        raise ValueError(f"not a matrix: {java_array[:40]}")
    rows: list[list[float]] = []
    body = inner[1:-1]
    i = 0
    while i < len(body):
        while i < len(body) and body[i] in " \t\r\n,":
            i += 1
        if i >= len(body):
            break
        if body[i] != "{":
            break
        end = find_matching(body, i, "{", "}")
        row_src = body[i + 1 : end]
        values = []
        for part in split_args(row_src):
            expr = convert_java_expr(part)
            ctx = isbrh.empty_render_context()
            ctx.update(extra_locals)
            values.append(float(eval(expr, ctx, ctx)))
        rows.append(values)
        i = end + 1
    return rows


def split_args(source: str) -> list[str]:
    parts: list[str] = []
    depth = 0
    start = 0
    for i, ch in enumerate(source):
        if ch in "{([":
            depth += 1
        elif ch in "})]":
            depth -= 1
        elif ch == "," and depth == 0:
            part = source[start:i].strip()
            if part:
                parts.append(part)
            start = i + 1
    tail = source[start:].strip()
    if tail:
        parts.append(tail)
    return parts


def round_coord(value: float) -> float:
    return float(f"{value * 16.0:.4f}")


def element_from_aabb(
    aabb: tuple[float, float, float, float, float, float],
    faces: dict[str, Any],
) -> dict[str, Any]:
    x1, y1, z1, x2, y2, z2 = aabb
    return {
        "from": [round_coord(min(x1, x2)), round_coord(min(y1, y2)), round_coord(min(z1, z2))],
        "to": [round_coord(max(x1, x2)), round_coord(max(y1, y2)), round_coord(max(z1, z2))],
        "faces": faces,
    }


def convert_class(class_name: str) -> ConvertResult:
    result = ConvertResult(class_name=class_name)
    try:
        methods, statics, notes = collect_class_methods(class_name)
        result.notes.extend(notes)
        missing = [name for name in ("getRenderPasses2", "setBlockBounds2", "getTexture2") if name not in methods]
        if missing:
            result.error = f"missing methods: {', '.join(missing)}"
            return result
        extra: dict[str, Any] = {}
        for name, java_array in statics.items():
            extra[name] = eval_float_matrix(java_array, extra)
        passes_fn = compile_java_method("getRenderPasses2", methods["getRenderPasses2"], extra)
        uses_fn = compile_java_method("usesRenderPass2", methods["usesRenderPass2"], extra)
        recorder = isbrh.BoxRecorder()
        bounds_fn = make_bounds_fn(methods["setBlockBounds2"], extra, recorder)
        tex_fn = make_texture_fn(methods["getTexture2"], extra)
        sides = list(isbrh.SIDES_ITEM_RENDER)
        pass_count = int(passes_fn(None, sides))
        if pass_count < 0 or pass_count > 96:
            result.error = f"getRenderPasses2 returned {pass_count}"
            return result
        elements: list[dict[str, Any]] = []
        for render_pass in range(pass_count):
            if not uses_fn(render_pass, sides):
                continue
            recorder.box = None
            used_custom = bool(bounds_fn(None, render_pass, sides))
            if used_custom:
                if recorder.box is None:
                    continue
                aabb = recorder.box
            else:
                aabb = (0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            faces: dict[str, Any] = {}
            for side, face_name in enumerate(isbrh.FACE_NAMES):
                texture = tex_fn(None, render_pass, side, sides)
                if isbrh.texture_is_drawn(texture):
                    faces[face_name] = {"texture": "#texture"}
            if faces:
                elements.append(element_from_aabb(aabb, faces))
        result.elements = elements
        return result
    except Exception as exc:
        result.error = str(exc)
        return result


def make_bounds_fn(java_body: str, extra: dict[str, Any], recorder: isbrh.BoxRecorder) -> Callable[..., Any]:
    global _switch_counter
    _switch_counter = 0
    py_body = java_block_to_python(java_body.replace("\t", " "), indent=1)
    source = with_global_fields("def _fn(aBlock, aRenderPass, aShouldSideBeRendered):\n", py_body)
    context = isbrh.empty_render_context(recorder)
    context.update(extra)
    seed_unknown_names(source, context)
    exec(source, context, context)
    return context["_fn"]


def make_texture_fn(java_body: str, extra: dict[str, Any]) -> Callable[..., Any]:
    global _switch_counter
    _switch_counter = 0
    py_body = java_block_to_python(java_body.replace("\t", " "), indent=1)
    source = with_global_fields("def _fn(aBlock, aRenderPass, aSide, aShouldSideBeRendered):\n", py_body)
    context = isbrh.empty_render_context()
    context.update(extra)
    seed_unknown_names(source, context)
    exec(source, context, context)
    return context["_fn"]


def model_payload(class_name: str, elements: list[dict[str, Any]]) -> dict[str, Any]:
    return {
        "credit": f"GT6 {class_name} empty-state ISBRH via tools/gt6_isbrh_to_json.py",
        "parent": "block/block",
        "textures": {
            "texture": "gregtech:blocks/materialicons/CERAMIC",
            "particle": "#texture",
        },
        "elements": elements,
    }


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(payload, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
        newline="\n",
    )


def overlay_pair(element: dict[str, Any]) -> list[dict[str, Any]]:
    body_faces = {}
    overlay_faces = {}
    for name, face in element["faces"].items():
        body_faces[name] = {"texture": "#body", "tintindex": 0}
        overlay_faces[name] = {"texture": "#body_overlay", "tintindex": 0}
    fx, fy, fz = element["from"]
    tx, ty, tz = element["to"]
    body = {"from": [fx, fy, fz], "to": [tx, ty, tz], "faces": body_faces}
    overlay = {
        "from": [round(fx - 0.01, 4), round(fy - 0.01, 4), round(fz - 0.01, 4)],
        "to": [round(tx + 0.01, 4), round(ty + 0.01, 4), round(tz + 0.01, 4)],
        "faces": overlay_faces,
    }
    return [body, overlay]


def write_cc_anvil(elements: list[dict[str, Any]]) -> None:
    cc_elements: list[dict[str, Any]] = []
    for element in elements:
        cc_elements.extend(overlay_pair(element))
    payload = {
        "credit": (
            "GregTech 6 ID 32025 MultiTileEntityAnvil empty-state ISBRH geometry; "
            "workpiece voxels and NEI watermark omitted; raw ROUGH blockSolid texture "
            "set imported unchanged for CrucibleCraft material tinting."
        ),
        "parent": "minecraft:block/block",
        "textures": {
            "body": "cruciblecraft:block/t34_gt6/materialicons/rough_block_solid",
            "body_overlay": "cruciblecraft:block/t34_gt6/materialicons/rough_block_solid_overlay",
            "particle": "#body",
        },
        "elements": cc_elements,
    }
    write_json(CC_ANVIL, payload)


def target_classes() -> list[str]:
    names = sorted(path.stem for path in OUT_DIR.glob("MultiTileEntity*.json"))
    return names


def render_report(results: list[ConvertResult]) -> str:
    ok = [item for item in results if item.ok]
    failed = [item for item in results if not item.ok]
    lines = [
        "# GT6 方块模型批量转换报告",
        "",
        "来源：GregTech6 `RendererBlockTextured` 空态循环（`getRenderPasses2` / `usesRenderPass2` / `setBlockBounds2` / `getTexture2`），",
        "由 `tools/gt6_isbrh_to_json.py` 导出为 Minecraft JSON `elements`。只保留空机器身：熔融液面、槽位物品、NEI 水印、储能条都不写入。",
        "",
        "## 转换规则说明",
        "",
        "- 朝向取南（`mFacing = SIDE_Z_POS`）；`aShouldSideBeRendered` 取 `SIDES_ITEM_RENDER`（六面都画）",
        "- `getTexture2` 返回 `null` / molten / fluid / `BI.nei()` / 槽位贴图的面被丢弃；六面皆空则丢弃该盒子",
        "- 坐标为 0–16 像素；`PX_P` / `PX_N` / `PX_OFFSET` 与 GT6 `CS` 一致",
        "- 贴图占位为 `#texture`（机身）。动态内容请用 BER 或已有 filled 变体，不要把液面烤进空态 JSON",
        "",
        f"## 转换结果（{len(ok)} 个成功 / {len(results)} 个目标）",
        "",
        "| 类名（文件名） | 名称 | 盒子数 | 备注 |",
        "|---|---|---|---|",
    ]
    for item in results:
        name = CLASS_NAMES.get(item.class_name, item.class_name)
        if item.ok:
            note = "，".join(item.notes) if item.notes else ""
            lines.append(f"| {item.class_name} | {name} | {len(item.elements)} | {note} |")
        else:
            lines.append(f"| {item.class_name} | {name} | — | 失败：{item.error} |")
    lines.extend(
        [
            "",
            "## 失败项",
            "",
        ]
    )
    if failed:
        for item in failed:
            lines.append(f"- `{item.class_name}`: {item.error}")
    else:
        lines.append("无。")
    lines.extend(
        [
            "",
            "## 未转换项",
            "",
            "- `TileEntityBase04/06Covers`、`TileEntityBase10/11Connector*`：基类，管线/覆膜模型按连接状态程序化生成，无固定空态形状",
            "- 动画 TESR（蒸汽引擎活塞 TESR 注释块、Mass Storage 计数器 TESR）不在 ISBRH 循环内",
            "",
        ]
    )
    return "\n".join(lines)


def convert_named(class_name: str) -> dict[str, Any]:
    result = convert_class(class_name)
    if not result.ok:
        raise ValueError(result.error)
    return model_payload(class_name, result.elements)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--class-name", action="append", dest="class_names")
    parser.add_argument("--write-anvil-cc", action="store_true")
    parser.add_argument("--report", action="store_true")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args(argv)
    names = args.class_names or target_classes()
    results = [convert_class(name) for name in names]
    written = 0
    for item in results:
        status = "OK" if item.ok else "FAIL"
        detail = str(len(item.elements)) if item.ok else item.error
        print(f"{status:4} {item.class_name}: {detail}")
        if args.dry_run or not item.ok:
            continue
        write_json(OUT_DIR / f"{item.class_name}.json", model_payload(item.class_name, item.elements))
        written += 1
        if args.write_anvil_cc and item.class_name == "MultiTileEntityAnvil":
            write_cc_anvil(item.elements)
    if args.report and not args.dry_run:
        REPORT_PATH.parent.mkdir(parents=True, exist_ok=True)
        REPORT_PATH.write_text(render_report(results) + "\n", encoding="utf-8", newline="\n")
    failed = sum(1 for item in results if not item.ok)
    print(f"wrote {written} models, {failed} failures")
    return 1 if failed else 0


if __name__ == "__main__":
    raise SystemExit(main())
