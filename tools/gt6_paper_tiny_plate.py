#!/usr/bin/env python3
"""Register GT6 paper tiny_plate and fold the slicer overflow row.

OP.plateTiny.forceItemGeneration(MT.Paper) is the source. Do not stand in
another plate form. Close at runtime_ready; slicer stays not player_complete.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import sys
from pathlib import Path
from typing import Any

from tools import census_common as census
from tools import io_common as io

GT6_REVISION = io.SOURCE_REVISION
SLUG = "content/gt6-paper-tiny-plate"
PLAN_STEM = "GT6纸微型板详细计划.md"
STATUS = "PAPER_TINY_PLATE_RUNTIME_READY"
WAVE = census.TOOLS / "waves" / "content" / "gt6-paper-tiny-plate"
REQUIRED_FORMS = WAVE / "required_forms.json"
AUTHORITY = census.TOOLS / "material_form_authority.json"
GATE = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft"
    / "material_registration_gate.json"
)
EMPTY_NBT = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_fluid_pipe_runtime"
    / "structure"
    / "empty.nbt"
)
PACK = (
    census.ROOT
    / "src"
    / "main"
    / "resources"
    / "data"
    / "cruciblecraft_wave_content_gt6_paper_tiny_plate"
)
GATE_SECTION = "machines_slicer_paper_tiny_plate_required_forms"
SOURCE_ID = GATE_SECTION
GAME_TESTS = "PaperTinyPlateGameTests.java"
TEST_IDS = ["paperTinyPlateItemIsRegistered"]


def _write_json(path: Path, document: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(
        json.dumps(document, indent=2, ensure_ascii=False) + "\n",
        encoding="utf-8",
    )


def _pack_namespace() -> str:
    return "cruciblecraft_wave_content_gt6_paper_tiny_plate"


def _owned_paths(*, closed: bool) -> list[str]:
    lane = "closed" if closed else "active"
    return [
        f"docs/history/card-plans/{lane}/{PLAN_STEM}",
        f"src/test/java/com/masson/cruciblecraft/gametest/{GAME_TESTS}",
        f"src/main/resources/data/{_pack_namespace()}/**",
        f"tools/capabilities/{SLUG}/**",
        "tools/waves/content/gt6-paper-tiny-plate/**",
        "tools/gt6_paper_tiny_plate.py",
        "tools/build_gt6_paper_tiny_plate.py",
        "tools/tests/test_gt6_paper_tiny_plate.py",
        "src/main/resources/data/cruciblecraft/material_registration_gate.json",
        "tools/material_form_authority.json",
    ]


def _plan_body(*, closed: bool) -> str:
    if closed:
        lane = (
            "lane                         = closed\n"
            f"capability_slug              = {SLUG}\n"
            "unique_active_wave           = null\n"
            "maturity                     = runtime_ready\n"
            "workflow                     = accepted\n"
            "depends_on                   = machines/slicer\n"
            "close_target                 = runtime_ready"
        )
        status = (
            "> 状态：已关闭 `runtime_ready` / `workflow=accepted`。\n"
            "> 本文件位于 `card-plans/closed/`。"
        )
        gate = "[x]"
    else:
        lane = (
            "lane                         = unique-active\n"
            f"capability_slug              = {SLUG}\n"
            f"unique_active_wave           = {SLUG}\n"
            "maturity                     = runtime_ready\n"
            "workflow                     = active\n"
            "depends_on                   = machines/slicer\n"
            "close_target                 = runtime_ready"
        )
        status = (
            "> 状态：unique-active，目标 `runtime_ready`。\n"
            "> 本文件位于 `card-plans/active/`。"
        )
        gate = "[ ]"
    return (
        f"# GT6 纸微型板\n\n"
        f"> 计划 slug：`{SLUG}`\n"
        f"{status}\n"
        "> 正式名称：GT6 纸微型板\n"
        "> 性质：按 GT6 `OP.plateTiny.forceItemGeneration(MT.Paper)` 注册\n"
        "> `paper:tiny_plate`，并重算切片机 overflow。不是其它板的替身。\n"
        ">\n"
        "> Java/tick 源：`gt6_code/gregtech6` @ "
        "`3703e40308c8c030763fd6297dea8b210d2a77b1`。\n\n"
        "```text\n"
        f"{lane}\n"
        "```\n\n"
        "## 门禁\n\n"
        f"- {gate} `paper:tiny_plate` 在 material registration gate 中\n"
        f"- {gate} 切片机 selected 33 / overflow 0\n"
        f"- {gate} 隔离 GameTest `{TEST_IDS[0]}`\n"
        "- [ ] 不以 programmed circuit 或其它板顶格\n"
    )


def topology(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "depends_on": ["machines/slicer"],
    }


def readiness(unique_active: bool) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "wave_slug": SLUG,
        "unique_active_wave": SLUG if unique_active else None,
        "status": STATUS,
        "source_revision": GT6_REVISION,
        "close_target": "runtime_ready",
        "form": "paper:tiny_plate",
        "gt6_evidence": "OP.plateTiny.forceItemGeneration(MT.Paper)",
    }


def _copy_empty_nbt() -> None:
    if not EMPTY_NBT.is_file():
        raise FileNotFoundError(census.relative(EMPTY_NBT))
    for relative in ("structure/empty.nbt", "gametest/structure/empty.nbt"):
        dest = PACK / relative
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(EMPTY_NBT, dest)


def _patch_authority() -> None:
    document = census.load_json(AUTHORITY)
    sources = list(document.get("sources") or [])
    existing = next((row for row in sources if row.get("id") == SOURCE_ID), None)
    source = {
        "extra_factual_forms": ["tiny_plate"],
        "field": "required_forms",
        "gate_section": GATE_SECTION,
        "id": SOURCE_ID,
        "java_runtime_visible": True,
        "owner": SLUG,
        "path": "tools/waves/content/gt6-paper-tiny-plate/required_forms.json",
        "required_factual_prereqs": {},
    }
    if existing is None:
        sources.append(source)
    else:
        sources[sources.index(existing)] = source
    document["sources"] = sources
    sections = list(document.get("java_overlay_sections") or [])
    if GATE_SECTION not in sections:
        sections.append(GATE_SECTION)
    document["java_overlay_sections"] = sections
    io.write_stable(AUTHORITY, document)


def _source_sha256() -> str:
    return hashlib.sha256(REQUIRED_FORMS.read_bytes()).hexdigest()


def _patch_gate() -> None:
    """Register paper:tiny_plate without replaying the full material-form gate."""
    from tools import currentness

    if not GATE.is_file():
        raise ValueError("missing material registration gate")
    text = GATE.read_text(encoding="utf-8")
    data = json.loads(text)
    overlay_object = (
        '  "machines_slicer_paper_tiny_plate_required_forms": {\n'
        '    "paper": [\n'
        '      "tiny_plate"\n'
        '    ]\n'
        "  },\n"
    )
    overlay = data.get(GATE_SECTION)
    if not isinstance(overlay, dict) or overlay.get("paper") != ["tiny_plate"]:
        marker = '\n  "materials": {'
        if text.count(marker) != 1:
            raise ValueError("material registration gate materials marker drifted")
        text = text.replace(marker, "\n" + overlay_object + '  "materials": {', 1)
        data = json.loads(text)
    sources = data.get("sources") or {}
    source_row = sources.get(GATE_SECTION) if isinstance(sources, dict) else None
    expected_source = {
        "classification": "semantic_ordinary_runtime_required",
        "field": "required_forms",
        "path": "tools/waves/content/gt6-paper-tiny-plate/required_forms.json",
        "sha256": _source_sha256(),
    }
    if source_row != expected_source:
        source_blob = (
            '    "machines_slicer_paper_tiny_plate_required_forms": {\n'
            '      "classification": "semantic_ordinary_runtime_required",\n'
            '      "field": "required_forms",\n'
            '      "path": "tools/waves/content/gt6-paper-tiny-plate/required_forms.json",\n'
            f'      "sha256": "{expected_source["sha256"]}"\n'
            "    },\n"
        )
        marker = '    "material_tag_policy": {'
        if GATE_SECTION in sources:
            old_sha = str((source_row or {}).get("sha256") or "")
            if not old_sha:
                raise ValueError("paper tiny_plate gate source sha256 missing")
            text = text.replace(old_sha, expected_source["sha256"], 1)
        else:
            if text.count(marker) != 1:
                raise ValueError("material registration gate sources marker drifted")
            text = text.replace(marker, source_blob + marker, 1)
        data = json.loads(text)
    sections = list(data.get("java_overlay_sections") or [])
    if GATE_SECTION not in sections:
        marker = (
            '    "technological_parts_foundation_required_forms"\n'
            "  ]"
        )
        replacement = (
            '    "technological_parts_foundation_required_forms",\n'
            '    "machines_slicer_paper_tiny_plate_required_forms"\n'
            "  ]"
        )
        if text.count(marker) != 1:
            raise ValueError("material registration gate overlay list drifted")
        text = text.replace(marker, replacement, 1)
        data = json.loads(text)
    paper_forms = list((data.get("materials") or {}).get("paper") or [])
    if "tiny_plate" not in paper_forms:
        old_forms = list(paper_forms)
        new_forms = sorted([*old_forms, "tiny_plate"])
        if not old_forms:
            raise ValueError("materials.paper is empty")
        pattern = (
            '    "paper": [\n'
            + ",\n".join(f'      "{form}"' for form in old_forms)
            + "\n    ]"
        )
        replacement = (
            '    "paper": [\n'
            + ",\n".join(f'      "{form}"' for form in new_forms)
            + "\n    ]"
        )
        if text.count(pattern) != 1:
            raise ValueError("materials.paper form list drifted")
        text = text.replace(pattern, replacement, 1)
        registered = int((data.get("counts") or {}).get("registered_forms") or 0)
        text = text.replace(
            f'"registered_forms": {registered}',
            f'"registered_forms": {registered + 1}',
            1,
        )
        data = json.loads(text)
    json.loads(text)
    if GATE.read_text(encoding="utf-8") != text:
        GATE.write_text(text, encoding="utf-8")
        currentness.write_sidecar(GATE)


def _gate_has_paper_tiny_plate() -> bool:
    if not GATE.is_file():
        return False
    gate = census.load_json(GATE)
    materials = gate.get("materials") or {}
    overlay = gate.get(GATE_SECTION) or {}
    return (
        "tiny_plate" in (materials.get("paper") or [])
        and "tiny_plate" in (overlay.get("paper") or [])
    )


def issue_active() -> dict[str, Any]:
    active_dir = census.ROOT / "docs" / "history" / "card-plans" / "active"
    for path in sorted(active_dir.glob("*.md")):
        if path.name != PLAN_STEM:
            raise ValueError(f"unique-active already occupied by {path.name}")
    cap_path = census.TOOLS / "capabilities" / SLUG / "capability.json"
    if cap_path.is_file():
        existing = census.load_json(cap_path)
        if existing.get("workflow") == "accepted":
            raise ValueError(f"{SLUG} already closed")
    document = {
        "schema_version": 2,
        "slug": SLUG,
        "title": "GT6 Paper Tiny Plate",
        "maturity": "runtime_ready",
        "workflow": "active",
        "owned_paths": _owned_paths(closed=False),
        "depends_on": ["machines/slicer"],
        "profiles": ["capability-runtime"],
        "wave_slug": SLUG,
        "required_test_ids": list(TEST_IDS),
        "identity_disposition": [
            {
                "semantic_key": "form:paper:tiny_plate",
                "disposition": "new_distinct",
                "runtime_ids": ["cruciblecraft:paper/tiny_plate"],
                "reason": (
                    "GT6 OP.plateTiny.forceItemGeneration(MT.Paper). "
                    "Not another plate form."
                ),
            }
        ],
        "note": (
            "Registers paper:tiny_plate from GT6 forceItemGeneration and "
            "recomputes machines/slicer overflow. Close at runtime_ready."
        ),
    }
    _write_json(cap_path, document)
    plan = census.ROOT / "docs" / "history" / "card-plans" / "active" / PLAN_STEM
    plan.parent.mkdir(parents=True, exist_ok=True)
    plan.write_text(_plan_body(closed=False), encoding="utf-8")
    _copy_empty_nbt()
    return {"capability": census.relative(cap_path), "plan": census.relative(plan)}


def prepare_close() -> dict[str, Any]:
    cap_path = census.TOOLS / "capabilities" / SLUG / "capability.json"
    document = census.load_json(cap_path)
    document["owned_paths"] = _owned_paths(closed=True)
    _write_json(cap_path, document)
    active = census.ROOT / "docs" / "history" / "card-plans" / "active" / PLAN_STEM
    if active.is_file():
        active.write_text(_plan_body(closed=True), encoding="utf-8")
    _write_json(WAVE / "topology.json", topology(False))
    _write_json(WAVE / "readiness.json", readiness(False))
    return {"owned_paths": document["owned_paths"]}


def write(*, unique_active: bool) -> dict[str, Any]:
    WAVE.mkdir(parents=True, exist_ok=True)
    _write_json(
        REQUIRED_FORMS,
        {
            "counts": {"required_form_pairs": 1, "required_materials": 1},
            "generated_by": SLUG,
            "note": "GT6 OP.plateTiny.forceItemGeneration(MT.Paper). Not a stand-in plate.",
            "required_forms": {"paper": ["tiny_plate"]},
            "schema_version": 1,
            "source_revision": GT6_REVISION,
        },
    )
    _patch_authority()
    _patch_gate()
    _write_json(WAVE / "topology.json", topology(unique_active))
    _write_json(WAVE / "readiness.json", readiness(unique_active))
    _write_json(
        WAVE / "production_lock.json",
        {
            "note": (
                "paper:tiny_plate is a GT6 force-generated form; "
                "slicer overflow recomputed; not player_complete"
            )
        },
    )
    _copy_empty_nbt()
    if not _gate_has_paper_tiny_plate():
        raise ValueError("gate write did not register paper:tiny_plate")
    return {
        "slug": SLUG,
        "gate": _gate_has_paper_tiny_plate(),
        "unique_active": unique_active,
    }


def check() -> list[str]:
    errors: list[str] = []
    if not REQUIRED_FORMS.is_file():
        return ["missing paper tiny_plate required_forms"]
    required = census.load_json(REQUIRED_FORMS).get("required_forms") or {}
    if required.get("paper") != ["tiny_plate"]:
        errors.append("required_forms must be paper:tiny_plate only")
    authority = census.load_json(AUTHORITY)
    if GATE_SECTION not in (authority.get("java_overlay_sections") or []):
        errors.append("authority java_overlay_sections missing paper tiny_plate")
    if not _gate_has_paper_tiny_plate():
        errors.append("material registration gate missing paper:tiny_plate")
    overflow = census.TOOLS / "waves" / "machines" / "slicer" / "overflow.json"
    if overflow.is_file():
        document = census.load_json(overflow)
        if int(document.get("blocked_rows") or 0) != 0:
            errors.append("slicer overflow must be 0 after paper tiny_plate")
        blob = str(document)
        if "paper:tiny_plate" in blob:
            errors.append("slicer overflow still names paper:tiny_plate")
    if not (PACK / "structure" / "empty.nbt").is_file():
        errors.append("missing paper tiny_plate structure/empty.nbt")
    java = (
        census.ROOT
        / "src"
        / "test"
        / "java"
        / "com"
        / "masson"
        / "cruciblecraft"
        / "gametest"
        / GAME_TESTS
    )
    if not java.is_file():
        errors.append(f"missing {GAME_TESTS}")
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    parser.add_argument("--issue", action="store_true")
    parser.add_argument("--prepare-close", action="store_true")
    parser.add_argument("--unique-active", action="store_true")
    parser.add_argument("--no-unique-active", action="store_true")
    args = parser.parse_args(argv)
    unique_active = True
    if args.no_unique_active:
        unique_active = False
    if args.unique_active:
        unique_active = True
    try:
        if args.issue:
            print(json.dumps(issue_active(), indent=2))
            return 0
        if args.prepare_close:
            print(json.dumps(prepare_close(), indent=2))
            return 0
        if args.check:
            errors = check()
            if errors:
                print("\n".join(errors), file=sys.stderr)
                return 1
            print("content/gt6-paper-tiny-plate is current")
            return 0
        if args.write:
            print(json.dumps(write(unique_active=unique_active), indent=2))
            return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as error:
        print(f"gt6 paper tiny_plate failed: {error}", file=sys.stderr)
        return 1
    parser.print_help()
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
