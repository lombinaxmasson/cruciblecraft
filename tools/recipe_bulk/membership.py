#!/usr/bin/python3
"""Java-compatible compact publication-group membership root."""
from __future__ import annotations

import hashlib
from collections.abc import Iterable


def membership_root(family_ids: Iterable[str], stable_ids: Iterable[str]) -> str:
    """SHA-256 of sorted family_id lines then sorted stable_id lines.

    Must stay byte-identical to CompactPublicationPolicy.membershipRoot().
    """
    payload = "".join(f"{value}\n" for value in sorted(family_ids))
    payload += "".join(f"{value}\n" for value in sorted(stable_ids))
    return hashlib.sha256(payload.encode("utf-8")).hexdigest()


def identity_semantic_root(rows: Iterable[dict]) -> str:
    """Fail-closed identity binding over source_key, target, and disposition."""
    lines = [
        f"{row.get('source_key')}\t{row.get('target_identity')}\t{row.get('disposition')}\n"
        for row in rows
    ]
    lines.sort()
    return hashlib.sha256("".join(lines).encode("utf-8")).hexdigest()
