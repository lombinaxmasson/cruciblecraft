#!/usr/bin/env python3
"""Effective recycling_candidate: frozen T42 overlay plus append-only corrections."""
from __future__ import annotations

from pathlib import Path
from typing import Any, Mapping

from tools import t35_common as t35
from tools import t42_common as t42

CORRECTION_PATH = (
    t35.TOOLS / "waves" / "bath" / "tiny-purified" / "t49_recycling_candidate_correction.json"
)


def overlay_flag(family_id: str, overlay_row: Mapping[str, Any] | None) -> bool:
    if overlay_row is None:
        return False
    return bool(overlay_row.get("recycling_candidate"))


def correction_false_ids(document: Mapping[str, Any] | None = None) -> set[str]:
    payload = document
    if payload is None:
        if not CORRECTION_PATH.is_file():
            return set()
        payload = t35.load_json(CORRECTION_PATH)
    if payload.get("status") not in {
        "T49_RECYCLING_CANDIDATE_CORRECTION",
        "RECYCLING_CANDIDATE_CORRECTION",
    }:
        raise ValueError("recycling correction status is not frozen")
    ids: set[str] = set()
    for row in payload.get("families") or []:
        if row.get("recycling_candidate") is not False:
            raise ValueError(
                f"{row.get('family_id')}: correction must set recycling_candidate false"
            )
        ids.add(str(row["family_id"]))
    expected = int(payload.get("family_count") or 0)
    if expected and len(ids) != expected:
        raise ValueError("recycling correction family_count drifted")
    return ids


def effective_recycling_candidate(
    family_id: str,
    overlay_row: Mapping[str, Any] | None = None,
    *,
    correction: Mapping[str, Any] | None = None,
) -> bool:
    if overlay_row is None:
        overlay = t35.load_json(t42.BLOCKER_OVERLAY)
        overlay_row = next(
            (
                row
                for row in overlay.get("families") or []
                if str(row.get("family_id") or "") == family_id
            ),
            None,
        )
    if not overlay_flag(family_id, overlay_row):
        return False
    return family_id not in correction_false_ids(correction)


def overlay_by_id(overlay: Mapping[str, Any] | None = None) -> dict[str, dict[str, Any]]:
    payload = overlay if overlay is not None else t35.load_json(t42.BLOCKER_OVERLAY)
    return {str(row["family_id"]): row for row in payload.get("families") or []}
