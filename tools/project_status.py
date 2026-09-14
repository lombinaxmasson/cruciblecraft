#!/usr/bin/env python3
"""Rebuild the single generated project-status page from capability authority."""
from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from tools import atomic_io
from tools import capability_ledger
from tools import io_common as io

STATUS_PATH = ROOT / "docs" / "current" / "project-status.md"
POINTER_FILES = (
    ROOT / "docs" / "README.md",
    ROOT / "docs" / "current" / "roadmap.md",
    ROOT / "docs" / "current" / "known-issues.md",
    ROOT / "docs" / "current" / "unimplemented-gap.md",
    ROOT / "docs" / "history" / "INDEX.md",
)
POINTER_NEEDLE = "project-status.md"
FORBIDDEN_STATUS_RECITAL = (
    (
        ROOT / "docs" / "README.md",
        (
            r"现行 `player_complete`（",
            r"当前 unique active：",
            r"最近关闭：\[",
        ),
    ),
    (
        ROOT / "docs" / "current" / "roadmap.md",
        (
            r"现行 `player_complete`（",
            r"当前人读 unique active",
        ),
    ),
    (
        ROOT / "docs" / "current" / "known-issues.md",
        (
            r"无 unique-active。已关闭",
            r"现行 `player_complete`（",
        ),
    ),
    (
        ROOT / "docs" / "history" / "INDEX.md",
        (
            r"现行 `player_complete`（",
            r"当前 active 卡",
            r"^## 最近关闭$",
        ),
    ),
)


def _href(path: Path) -> str:
    return os.path.relpath(path, STATUS_PATH.parent).replace("\\", "/")


def _plan_md(path: Path) -> str:
    return f"[{capability_ledger.card_plan_title(path)}]({_href(path)})"


def render_status(ledger: dict[str, Any] | None = None) -> str:
    compiled = ledger if ledger is not None else capability_ledger.compile_ledger()
    plans = capability_ledger.load_card_plan_index()
    unique = compiled.get("unique_active_slug")
    by_slug = {row["slug"]: row for row in compiled["capabilities"]}
    lines = [
        "# 项目状态",
        "",
        "> 本页由 `python tools/build_project_status.py --write` 从",
        "> `tools/capabilities/**/capability.json` 与 `docs/history/card-plans/`",
        "> 生成。不要手改。ledger.json 与本页都是可重建投影，不是权威。",
        "",
        "## Unique active",
        "",
    ]
    if unique:
        row = by_slug[unique]
        plan = plans["active"].get(unique)
        plan_bit = f"；计划 {_plan_md(plan)}" if plan is not None else ""
        lines.append(
            f"`{unique}`（{row['title']}，`workflow=active`，"
            f"`maturity={row['maturity']}`）{plan_bit}。"
        )
    else:
        lines.append("无。`capability.json` 里没有 `workflow=active`。")
    lines.extend(["", "## Prep（不占落地锁）", ""])
    prep = plans["prep"]
    if prep:
        lines.append(
            "计划文件存在就会列在这里。MTE 家族 prep 的 runtime child 已关时，"
            "不要把本表当剩余待办。读法见 [unimplemented-gap.md](unimplemented-gap.md)。"
        )
        lines.append("")
        for slug, path in sorted(prep.items()):
            lines.append(f"- `{slug}` — {_plan_md(path)}")
    else:
        lines.append("无已签发 prep 计划。")
    complete = list(compiled["declared_player_complete"])
    runtime_ready = [
        row["slug"]
        for row in compiled["capabilities"]
        if row["maturity"] == "runtime_ready" and row["workflow"] == "accepted"
    ]
    frozen = [
        row["slug"]
        for row in compiled["capabilities"]
        if row["maturity"] == "frozen"
    ]
    lines.extend(
        [
            "",
            f"## player_complete（{len(complete)}）",
            "",
            "只计 `maturity=player_complete` 且 `workflow=accepted`。",
            "声明不是证明；晋级仍要 fresh GameTest，`runClient` 只在晋级时强制。",
            "",
        ]
    )
    for slug in complete:
        row = by_slug[slug]
        plan = plans["closed"].get(slug)
        plan_bit = f" — {_plan_md(plan)}" if plan is not None else ""
        lines.append(f"- `{slug}` — {row['title']}{plan_bit}")
    lines.extend(
        [
            "",
            f"## runtime_ready（accepted，非玩家完成）（{len(runtime_ready)}）",
            "",
            "RecipeMap / 主机已挂，获得格或配方仍 blocked。不得宣称 `player_complete`。",
            "",
        ]
    )
    if runtime_ready:
        for slug in runtime_ready:
            row = by_slug[slug]
            plan = plans["closed"].get(slug)
            plan_bit = f" — {_plan_md(plan)}" if plan is not None else ""
            lines.append(f"- `{slug}` — {row['title']}{plan_bit}")
    else:
        lines.append("无。")
    lines.extend(
        [
            "",
            f"## frozen（非 runtime_ready / player_complete）（{len(frozen)}）",
            "",
            "分母已冻。`paused` 的 PUV/OMEGA 六张代码已在 `src/main`，是 CC 扩展，"
            "不是原版高压线，也不是 `player_complete`。地牢是结构载体，房间内容仍 blocked。",
            "",
        ]
    )
    if frozen:
        for slug in frozen:
            row = by_slug[slug]
            plan = (
                plans["closed"].get(slug)
                or plans["prep"].get(slug)
                or plans["active"].get(slug)
            )
            plan_bit = f" — {_plan_md(plan)}" if plan is not None else ""
            lines.append(
                f"- `{slug}` — {row['title']}（`workflow={row['workflow']}`）{plan_bit}"
            )
    else:
        lines.append("无。")
    closed_complete = [
        (slug, path)
        for slug, path in sorted(plans["closed"].items())
        if slug in set(complete)
    ]
    lines.extend(["", "## 关闭计划（有 capability 的 player_complete）", ""])
    if closed_complete:
        for slug, path in closed_complete:
            lines.append(f"- {_plan_md(path)}（`{slug}`）")
    else:
        lines.append("无。")
    lines.extend(
        [
            "",
            "权威与流程见 [capability-delivery-workflow.md](capability-delivery-workflow.md)",
            "与 [unimplemented-gap.md](unimplemented-gap.md)。",
            "跨能力 blocked 总账见 [blocked.md](blocked.md)。",
            "关闭一张卡：`python tools/close_capability.py --capability <slug>`。",
            "",
        ]
    )
    return "\n".join(lines)


def pointer_errors() -> list[str]:
    errors: list[str] = []
    for path in POINTER_FILES:
        text = path.read_text(encoding="utf-8")
        if POINTER_NEEDLE not in text:
            errors.append(f"{io.relative(path)} must link {POINTER_NEEDLE}")
    for path, patterns in FORBIDDEN_STATUS_RECITAL:
        text = path.read_text(encoding="utf-8")
        for pattern in patterns:
            if re.search(pattern, text, flags=re.MULTILINE):
                errors.append(
                    f"{io.relative(path)} re-authors projected status ({pattern})"
                )
    return errors


def write_status(text: str | None = None) -> None:
    atomic_io.write_text(STATUS_PATH, text if text is not None else render_status())


def check_status() -> list[str]:
    expected = render_status()
    errors: list[str] = []
    if not STATUS_PATH.is_file():
        errors.append(f"missing {io.relative(STATUS_PATH)}")
    else:
        actual = STATUS_PATH.read_text(encoding="utf-8").replace("\r\n", "\n")
        if actual != expected:
            errors.append(
                f"{io.relative(STATUS_PATH)} is stale; run "
                "python tools/build_project_status.py --write"
            )
    errors.extend(pointer_errors())
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args(argv)
    if args.write == args.check:
        parser.error("choose exactly one of --write or --check")
    try:
        text = render_status()
    except ValueError as error:
        print(str(error), file=sys.stderr)
        return 1
    if args.write:
        write_status(text)
        print(f"Wrote {io.relative(STATUS_PATH)}")
        return 0
    errors = check_status()
    if errors:
        print("\n".join(errors), file=sys.stderr)
        return 1
    print("project status is current")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
