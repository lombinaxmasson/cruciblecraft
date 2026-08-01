"""Shared names and prefix strategies for the GT6-to-CC import pipeline."""

from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any


MAPPING_PATH = Path(__file__).with_name("gt6_prefix_mapping.json")
_TAG_PATTERN = re.compile(r"[A-Z0-9_]+(?:\.[A-Z0-9_]+)+")
_STRATEGIES = {
    "rule",
    "explicit_enumeration",
    "compatibility_absorption",
    "derived_intersection",
}


def generation_tag_to_flag(tag: str) -> str:
    """Transcribe a GT6 tag to a lowercase namespaced generation flag."""
    if not isinstance(tag, str) or _TAG_PATTERN.fullmatch(tag) is None:
        raise ValueError(f"invalid GT6 generation tag: {tag!r}")
    return "gt6:" + tag.lower().replace(".", "/")


def load_prefix_mappings(path: Path = MAPPING_PATH) -> tuple[dict[str, Any], ...]:
    document = json.loads(path.read_text(encoding="utf-8"))
    if document.get("schema_version") != 1:
        raise ValueError("unsupported GT6 prefix mapping schema")
    rows = document.get("mappings")
    if not isinstance(rows, list):
        raise ValueError("GT6 prefix mappings must be a list")

    result: list[dict[str, Any]] = []
    seen: set[str] = set()
    for row in rows:
        if not isinstance(row, dict):
            raise ValueError("GT6 prefix mapping rows must be objects")
        gt_prefix = row.get("gt_prefix")
        cc_prefix = row.get("cc_prefix")
        strategy = row.get("strategy")
        domain_prefixes = row.get("domain_prefixes")
        if not isinstance(gt_prefix, str) or not gt_prefix:
            raise ValueError(f"invalid GT6 prefix mapping key: {gt_prefix!r}")
        if gt_prefix in seen:
            raise ValueError(f"duplicate GT6 prefix mapping: {gt_prefix}")
        if cc_prefix is not None and (
            not isinstance(cc_prefix, str)
            or re.fullmatch(r"[a-z0-9][a-z0-9_./-]*", cc_prefix) is None
        ):
            raise ValueError(f"invalid CC prefix mapping for {gt_prefix}: {cc_prefix!r}")
        if strategy not in _STRATEGIES:
            raise ValueError(f"invalid strategy for {gt_prefix}: {strategy!r}")
        if strategy == "compatibility_absorption" and cc_prefix is not None:
            raise ValueError(
                f"compatibility prefix {gt_prefix} must not generate a CC prefix"
            )
        if strategy == "derived_intersection":
            if (
                not isinstance(domain_prefixes, list)
                or len(domain_prefixes) < 2
                or any(
                    not isinstance(prefix, str) or not prefix
                    for prefix in domain_prefixes
                )
            ):
                raise ValueError(
                    f"derived prefix {gt_prefix} requires domain_prefixes"
                )
        elif domain_prefixes is not None:
            raise ValueError(
                f"non-derived prefix {gt_prefix} cannot define domain_prefixes"
            )
        seen.add(gt_prefix)
        result.append(
            {
                "gt_prefix": gt_prefix,
                "cc_prefix": cc_prefix,
                "strategy": strategy,
                **(
                    {"domain_prefixes": tuple(domain_prefixes)}
                    if domain_prefixes is not None
                    else {}
                ),
            }
        )
    return tuple(result)


PREFIX_MAPPINGS = load_prefix_mappings()
GT6_PREFIX_TO_CC = {
    row["gt_prefix"]: row["cc_prefix"]
    for row in PREFIX_MAPPINGS
    if row["cc_prefix"] is not None
}
PREFIX_STRATEGIES = {
    row["gt_prefix"]: row["strategy"]
    for row in PREFIX_MAPPINGS
}
COMPATIBILITY_ABSORPTION_PREFIXES = frozenset(
    prefix
    for prefix, strategy in PREFIX_STRATEGIES.items()
    if strategy == "compatibility_absorption"
)
EXPLICIT_ENUMERATION_PREFIXES = frozenset(
    prefix
    for prefix, strategy in PREFIX_STRATEGIES.items()
    if strategy == "explicit_enumeration"
)
