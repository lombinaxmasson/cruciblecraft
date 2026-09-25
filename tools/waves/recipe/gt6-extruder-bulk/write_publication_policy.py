#!/usr/bin/env python3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(ROOT))
sys.path.insert(1, str(ROOT / "tools"))
from tools import census_common as census
from tools.recipe_bulk.membership import membership_root

WAVE = ROOT / "tools/waves/recipe/gt6-extruder-bulk"
lock = census.load_json(WAVE / "production_lock.json")
families = lock["production"]["families"]
family_ids = [str(row["template_key"]) for row in families]
stable_ids = [sid for row in families for sid in row["stable_ids"]]
policy = {
    "cache_ceiling": 16,
    "eager_stable_ids": [],
    "family_count": len(family_ids),
    "membership_root_sha256": membership_root(family_ids, stable_ids),
    "policy_type": "on_demand",
    "publication_group": "cruciblecraft:extruder/bulk",
    "relation_count": len(stable_ids),
    "routing_schema_version": "compact-shard-v1",
    "target_map": "cruciblecraft:extruder",
    "type": "cruciblecraft:compact_publication_policy",
}
dest = (
    ROOT
    / "src/recipe_generated/resources/data/cruciblecraft/recipe/publication_policy/extruder_bulk.json"
)
census.write_stable(dest, policy)
print(f"families {len(family_ids)} relations {len(stable_ids)}")
