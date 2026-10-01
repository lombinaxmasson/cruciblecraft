"""policy_delta lists publication-group count and stable-id changes."""

from __future__ import annotations

import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
_SPEC = importlib.util.spec_from_file_location(
    "policy_delta",
    ROOT / "tools/waves/tooling/gametest-derived-counts/policy_delta.py",
)
policy_delta = importlib.util.module_from_spec(_SPEC)
assert _SPEC.loader is not None
_SPEC.loader.exec_module(policy_delta)
RECIPE = "src/recipe_generated/resources/data/cruciblecraft/recipe"
POLICY = f"{RECIPE}/publication_policy"
LOOM = "cruciblecraft:loom/pilot/loom"


def _policy(group: str, count: int, digest: str) -> dict:
    return {
        "publication_group": group,
        "relation_count": count,
        "membership_root_sha256": digest,
        "family_count": 1,
        "target_map": "cruciblecraft:loom",
    }


def _family(group: str, stable_ids: list[str]) -> dict:
    return {
        "family_id": "gt.recipe.loom#0000",
        "publication_group": group,
        "relations": [{"stable_id": stable_id} for stable_id in stable_ids],
    }


def _write(root: Path, relative: str, document: dict) -> None:
    path = root / relative
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(document), encoding="utf-8")


class PolicyDeltaTest(unittest.TestCase):
    def test_fixture_lists_count_and_stable_id_delta(self) -> None:
        with tempfile.TemporaryDirectory() as base_name, tempfile.TemporaryDirectory() as head_name:
            base = Path(base_name)
            head = Path(head_name)
            old_ids = ["cruciblecraft:gt6/aaa", "cruciblecraft:gt6/bbb"]
            new_ids = ["cruciblecraft:gt6/bbb", "cruciblecraft:gt6/ccc"]
            _write(base, f"{POLICY}/loom.json", _policy(LOOM, 2, "a" * 64))
            _write(head, f"{POLICY}/loom.json", _policy(LOOM, 2, "b" * 64))
            _write(head, f"{POLICY}/extra.json", _policy("cruciblecraft:loom/extra", 1, "c" * 64))
            _write(base, f"{RECIPE}/loom/gt_recipe_loom_0000.json", _family(LOOM, old_ids))
            _write(head, f"{RECIPE}/loom/gt_recipe_loom_0000.json", _family(LOOM, new_ids))
            _write(
                head,
                f"{RECIPE}/loom/extra.json",
                _family("cruciblecraft:loom/extra", ["cruciblecraft:gt6/ddd"]),
            )
            report = policy_delta.compare(
                policy_delta.Tree(directory=base),
                policy_delta.Tree(directory=head),
            )
        self.assertEqual(["cruciblecraft:loom/extra"], report["added_groups"])
        by_group = {row["publication_group"]: row for row in report["groups"]}
        loom = by_group[LOOM]
        self.assertEqual({"old": 2, "new": 2}, loom["relation_count"])
        self.assertTrue(loom["membership_changed"])
        self.assertEqual(["cruciblecraft:gt6/ccc"], loom["added_stable_ids"])
        self.assertEqual(["cruciblecraft:gt6/aaa"], loom["removed_stable_ids"])
        extra = by_group["cruciblecraft:loom/extra"]
        self.assertEqual({"old": None, "new": 1}, extra["relation_count"])
        self.assertEqual(["cruciblecraft:gt6/ddd"], extra["added_stable_ids"])

    def test_since_default_grid_restore_forty_groups_were_added(self) -> None:
        """Ten of these are the identity_ready groups published after the thirty."""
        old = policy_delta.load_policies(policy_delta.Tree(revision="abdb2bb61"))
        new = policy_delta.load_policies(policy_delta.Tree(worktree=True))
        self.assertEqual(40, len(set(new) - set(old)))
        self.assertEqual(set(), set(old) - set(new))

    def test_loom_drift_fix_removes_one_stable_id(self) -> None:
        """dd4ea9892 took loom/pilot/loom from 477 rows to 476 by deleting one id."""
        relative = (
            "src/recipe_generated/resources/data/cruciblecraft/recipe/"
            "loom/loom/gt_recipe_loom_0000.json"
        )
        before = policy_delta.Tree(revision="dd4ea9892^")
        after = policy_delta.Tree(revision="dd4ea9892")
        old_policies = policy_delta.load_policies(before)
        new_policies = policy_delta.load_policies(after)
        self.assertEqual(477, old_policies[LOOM]["relation_count"])
        self.assertEqual(476, new_policies[LOOM]["relation_count"])
        _old_group, old_ids = policy_delta.stable_ids(before.read(relative))
        _new_group, new_ids = policy_delta.stable_ids(after.read(relative))
        self.assertEqual([], sorted(new_ids - old_ids))
        self.assertEqual(1, len(old_ids - new_ids))


if __name__ == "__main__":
    unittest.main()
