from __future__ import annotations

import json
import unittest
from unittest import mock

from tools import build_t27_portfolio as builder
from tools import t27_common as common


class T27RecipeMapPortfolioTest(unittest.TestCase):
    def test_committed_recipe_maps_are_current_and_check_is_read_only(self) -> None:
        path = builder.output_path("recipe_maps")
        on_disk = json.loads(path.read_text(encoding="utf-8"))
        expected = builder.build_table("recipe_maps")
        self.assertEqual(expected, on_disk)
        before = path.read_bytes()
        self.assertEqual([], builder.check_table("recipe_maps"))
        self.assertEqual(before, path.read_bytes())

    def test_recipe_map_set_equals_t13_and_has_reviewed_dispositions(self) -> None:
        document = builder.build_table("recipe_maps")
        expected = set(common.t13_canonical_ids("recipe_maps"))
        actual = {row["canonical_id"] for row in document["records"]}
        self.assertEqual(expected, actual)
        self.assertEqual(95, document["counts"]["canonical"])
        self.assertEqual(29, document["counts"]["v1_required"])
        self.assertEqual(64, document["counts"]["post_1_0"])
        self.assertEqual(2, document["counts"]["out_of_scope"])
        self.assertEqual(0, document["counts"]["unclassified"])
        by_id = {row["canonical_id"]: row for row in document["records"]}
        for ident in expected:
            self.assertEqual([], common.validate_identity_record(by_id[ident]))
        self.assertEqual("out_of_scope", by_id["gt.recipe.anvil"]["disposition"])
        self.assertEqual("out_of_scope", by_id["gt.recipe.cruciblealloying"]["disposition"])
        self.assertEqual("v1_required", by_id["gt.recipe.anvil.bend.big"]["disposition"])
        self.assertEqual("closed", by_id["gt.recipe.anvil.bend.big"]["axes"]["closure"]["status"])
        self.assertEqual("cruciblecraft:bender", by_id["gt.recipe.anvil.bend.big"]["cc_implementation"])
        self.assertNotEqual(
            "cruciblecraft:anvil_bend_big",
            by_id["gt.recipe.anvil.bend.big"]["cc_implementation"],
        )
        self.assertEqual(
            "portfolio:track_e/deferred_recipe_maps",
            by_id["gt.recipe.boxinator"]["owner"],
        )
        self.assertEqual("post_1_0", by_id["gt.recipe.boxinator"]["disposition"])
        self.assertEqual(
            "portfolio:track_c/nuclear_fusion_plasma",
            by_id["gt.recipe.fusionreactor"]["owner"],
        )
        self.assertEqual(
            "portfolio:track_c/nuclear_fusion_plasma",
            by_id["gt.recipe.fuels.plasma"]["owner"],
        )
        self.assertEqual("none", by_id["gt.recipe.fuels.gas"]["cc_implementation"])
        self.assertEqual(
            "portfolio:v1/closed_recipe_maps",
            by_id["gt.recipe.mixer"]["owner"],
        )
        self.assertIn("not row-level", by_id["gt.recipe.mixer"]["axes"]["fidelity"]["evidence"])
        deferred = [
            row
            for row in document["records"]
            if row["t13_classification"] == "deferred_with_reason"
        ]
        self.assertEqual(64, len(deferred))
        self.assertTrue(all(row["disposition"] == "post_1_0" for row in deferred))

    def test_mutating_t13_row_hash_fails_closed(self) -> None:
        real = common.sha256_record

        def override(value):
            return "0" * 64

        with mock.patch.object(common, "sha256_record", side_effect=override):
            errors = builder.check_table("recipe_maps")
        self.assertTrue(any("stale" in error for error in errors))

    def test_all_tables_gate_passes_seven_policies_and_765_identities(self) -> None:
        self.assertEqual([], builder.check_all_tables())


class T27MultiblockEnergyDomainTest(unittest.TestCase):
    def test_multiblock_kinds_match_t23_v1_set(self) -> None:
        document = builder.build_table("multiblock_kinds")
        self.assertEqual([], builder.check_table("multiblock_kinds"))
        self.assertEqual(30, document["counts"]["canonical"])
        self.assertEqual(6, document["counts"]["v1_required"])
        self.assertEqual(19, document["counts"]["post_1_0"])
        self.assertEqual(5, document["counts"]["out_of_scope"])
        by_id = {row["canonical_id"]: row for row in document["records"]}
        self.assertEqual("v1_required", by_id["crucible"]["disposition"])
        self.assertEqual("closed", by_id["crucible"]["axes"]["closure"]["status"])
        self.assertEqual("cruciblecraft:large_crucible", by_id["crucible"]["cc_implementation"])
        self.assertEqual("portfolio:v1/crucible", by_id["crucible"]["owner"])
        self.assertEqual("closed", by_id["centrifuge"]["axes"]["closure"]["status"])
        self.assertEqual("cruciblecraft:large_centrifuge", by_id["centrifuge"]["cc_implementation"])
        self.assertEqual("out_of_scope", by_id["fusion_reactor"]["disposition"])
        self.assertEqual(
            "portfolio:track_b/post_1_0_multiblocks",
            by_id["autoclave"]["owner"],
        )

    def test_energy_identities_keep_neutron_on_track_c(self) -> None:
        document = builder.build_table("energy_identities")
        self.assertEqual([], builder.check_table("energy_identities"))
        self.assertEqual(20, document["counts"]["canonical"])
        self.assertEqual(7, document["counts"]["v1_required"])
        self.assertEqual(5, document["counts"]["post_1_0"])
        self.assertEqual(8, document["counts"]["out_of_scope"])
        by_id = {row["canonical_id"]: row for row in document["records"]}
        self.assertEqual("post_1_0", by_id["NEUTRON"]["disposition"])
        self.assertEqual(
            "portfolio:track_c/nuclear_fusion_plasma",
            by_id["NEUTRON"]["owner"],
        )
        self.assertEqual("out_of_scope", by_id["REDSTONE_FLUX"]["disposition"])
        self.assertEqual("v1_required", by_id["ELECTRICITY"]["disposition"])
        self.assertEqual("closed", by_id["ELECTRICITY"]["axes"]["closure"]["status"])
        self.assertNotEqual(by_id["NEUTRON"]["owner"], by_id["CRYO"]["owner"])

    def test_itemgenerator_domains_are_not_prefix_gaps(self) -> None:
        document = builder.build_table("itemgenerator_domains")
        self.assertEqual([], builder.check_table("itemgenerator_domains"))
        self.assertEqual(25, document["counts"]["canonical"])
        self.assertEqual(16, document["counts"]["v1_required"])
        self.assertEqual(3, document["counts"]["post_1_0"])
        self.assertEqual(6, document["counts"]["out_of_scope"])
        by_id = {row["canonical_id"]: row for row in document["records"]}
        self.assertEqual("closed", by_id["ITEMGENERATOR.INGOTS"]["axes"]["closure"]["status"])
        self.assertEqual("post_1_0", by_id["ITEMGENERATOR.LENSES"]["disposition"])
        self.assertEqual("out_of_scope", by_id["ITEMGENERATOR.ARMORS"]["disposition"])
        self.assertEqual("O-36", by_id["ITEMGENERATOR.INGOTS_HOT"]["dependencies"][0]["id"])

    def test_cover_kinds_keep_logistics_owner_off_energy_tables(self) -> None:
        document = builder.build_table("cover_kinds")
        self.assertEqual([], builder.check_table("cover_kinds"))
        self.assertEqual(47, document["counts"]["canonical"])
        self.assertEqual(9, document["counts"]["v1_required"])
        self.assertEqual(28, document["counts"]["post_1_0"])
        self.assertEqual(10, document["counts"]["out_of_scope"])
        by_id = {row["canonical_id"]: row for row in document["records"]}
        self.assertEqual("closed", by_id["pump"]["axes"]["closure"]["status"])
        self.assertEqual("portfolio:v1/closed_covers", by_id["pump"]["owner"])
        self.assertEqual("portfolio:post_1_0/covers", by_id["controller_auto"]["owner"])
        self.assertNotEqual(
            "portfolio:v1/closed_energy",
            by_id["controller_auto"]["owner"],
        )
        self.assertEqual("out_of_scope", by_id["asphalt"]["disposition"])

    def test_prefixes_keep_runtime_forms_separate_from_unmapped_tail(self) -> None:
        document = builder.build_table("prefixes")
        self.assertEqual([], builder.check_table("prefixes"))
        self.assertEqual(452, document["counts"]["canonical"])
        self.assertEqual(55, document["counts"]["v1_required"])
        self.assertEqual(271, document["counts"]["post_1_0"])
        self.assertEqual(126, document["counts"]["out_of_scope"])
        by_id = {row["canonical_id"]: row for row in document["records"]}
        self.assertEqual("v1_required", by_id["wireGt16"]["disposition"])
        self.assertEqual("cruciblecraft:hexadecuple_wire", by_id["wireGt16"]["cc_implementation"])
        self.assertEqual("cruciblecraft:block", by_id["blockGem"]["cc_implementation"])
        self.assertEqual("cruciblecraft:block", by_id["blockIngot"]["cc_implementation"])
        self.assertTrue(
            by_id["pipeMedium"]["cc_implementation"].startswith("aliases:")
        )
        self.assertIn("cruciblecraft:fluid_pipe", by_id["pipeMedium"]["cc_implementation"])
        self.assertIn("cruciblecraft:item_pipe", by_id["pipeMedium"]["cc_implementation"])
        self.assertEqual("post_1_0", by_id["armor"]["disposition"])
        self.assertEqual("none", by_id["armor"]["cc_implementation"])
        self.assertEqual("out_of_scope", by_id["cable"]["disposition"])
        self.assertEqual("O-36", by_id["ingotHot"]["dependencies"][0]["id"])
        self.assertEqual("closed", by_id["ingotHot"]["axes"]["closure"]["status"])

    def test_machine_kinds_do_not_treat_in_scope_as_v1_closure(self) -> None:
        document = builder.build_table("machine_kinds")
        self.assertEqual([], builder.check_table("machine_kinds"))
        self.assertEqual(96, document["counts"]["canonical"])
        self.assertLess(document["counts"]["v1_required"], 81)
        self.assertEqual(0, document["counts"]["out_of_scope"])
        t13_rows = {row["canonical_key"]: row for row in common.t13_rows("machine_kinds")}
        by_id = {row["canonical_id"]: row for row in document["records"]}

        def kind(*parts: str) -> dict:
            matches = [
                by_id[key]
                for key, row in t13_rows.items()
                if all(part in key for part in parts)
            ]
            self.assertEqual(1, len(matches), parts)
            return matches[0]

        lathe = kind("RM.Lathe")
        self.assertEqual("v1_required", lathe["disposition"])
        self.assertEqual("closed", lathe["axes"]["closure"]["status"])
        self.assertEqual("cruciblecraft:lathe", lathe["cc_implementation"])
        mixer_ru = kind("RM.Mixer", "accepts:RU")
        self.assertEqual("post_1_0", mixer_ru["disposition"])
        self.assertEqual("related_host:cruciblecraft:mixer", mixer_ru["cc_implementation"])
        autoclave = kind("RM.Autoclave")
        self.assertEqual("v1_required", autoclave["disposition"])
        coagulator = kind("RM.Coagulator")
        self.assertEqual("post_1_0", coagulator["disposition"])
        self.assertEqual("none", coagulator["cc_implementation"])
        welder = kind("RM.Welder")
        self.assertEqual("post_1_0", welder["disposition"])
        self.assertEqual("related_host:cruciblecraft:welder", welder["cc_implementation"])
        reactor = kind("MultiTileEntityReactorCore1x1")
        self.assertEqual(
            "portfolio:track_c/nuclear_fusion_plasma",
            reactor["owner"],
        )
        axle = kind("MultiTileEntityAxle")
        self.assertEqual("v1_required", axle["disposition"])
        self.assertEqual("cruciblecraft:rotational_axle", axle["cc_implementation"])


class T27OpenItemLedgerTest(unittest.TestCase):
    def test_open_items_cover_freeze_set_and_fifteen_known_issues(self) -> None:
        document = builder.build_open_items()
        self.assertEqual([], builder.check_open_items())
        by_id = {row["id"]: row for row in document["records"]}
        self.assertEqual(15, document["counts"]["known_issues"])
        self.assertEqual(0, document["counts"]["orphan"])
        self.assertEqual("canonical_coverage", by_id["crucible"]["kind"])
        self.assertEqual("portfolio:v1/crucible", by_id["crucible"]["owner"])
        self.assertEqual("v1_required", by_id["O-36"]["disposition"])
        self.assertEqual("closed", by_id["O-36"]["axes"]["closure"]["status"])
        self.assertEqual("portfolio:v1/hot_ingot_cooling", by_id["O-36"]["owner"])
        self.assertNotEqual(by_id["O-36"]["owner"], by_id["crucible"]["owner"])
        self.assertEqual("post_1_0", by_id["O-41"]["disposition"])
        self.assertEqual("portfolio:track_a/unlockable_translation", by_id["O-41"]["owner"])
        self.assertEqual("post_1_0", by_id["anvil_bend_big"]["disposition"])
        self.assertEqual("portfolio:post_1_0/anvil_bend", by_id["anvil_bend_small"]["owner"])
        self.assertEqual("closed", by_id["O-15"]["disposition"])
        self.assertEqual("T26", by_id["O-15"]["owner"])
        self.assertEqual("T27 RC", by_id["T24-F003"]["owner"])
        self.assertEqual("T27 RC candidate", by_id["T24-F003"]["recheck_point"])
        self.assertEqual(
            "Re-run on a declared environment with >= 16 GB RAM and record tools/t24_scale_evidence.json measured_at_scale.target",
            by_id["T24-F003"]["replacement_condition"],
        )
        self.assertEqual(by_id["T24-F003"]["replacement_condition"], by_id["T24-F005"]["replacement_condition"])
        self.assertEqual("4.5", by_id["CC-4.5-P0"]["owner"])
        self.assertEqual("post_beta_polish", by_id["CC-4.5-P0"]["disposition"])
        self.assertEqual("BLOCKED_PENDING_MEASUREMENT", by_id["T24-F005"]["axes"]["load"]["verdict"])


class T27TrackLoadTest(unittest.TestCase):
    def test_tracks_keep_unique_owners_and_do_not_start(self) -> None:
        document = builder.build_tracks()
        self.assertEqual([], builder.check_tracks())
        self.assertEqual([], builder.check_load_projections())
        self.assertFalse(document["started"])
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, document["publication_delta"])
        tracks = document["tracks"]
        self.assertEqual(["A", "B", "C", "D", "E"], sorted(tracks))
        self.assertEqual(1, tracks["A"]["primary_identity_count"])
        self.assertEqual("O-41", tracks["A"]["primary_identities"][0]["canonical_id"])
        self.assertEqual(29, len(tracks["A"]["reference_identities"]))
        self.assertEqual(19, tracks["B"]["primary_identity_count"])
        self.assertEqual(11, tracks["C"]["primary_identity_count"])
        self.assertEqual(0, tracks["D"]["primary_identity_count"])
        self.assertIsNone(tracks["D"]["gt6u_revision"])
        self.assertEqual(62, tracks["E"]["primary_identity_count"])
        fusion = {item["canonical_id"] for item in tracks["C"]["primary_identities"]}
        self.assertIn("gt.recipe.fusionreactor", fusion)
        self.assertIn("NEUTRON", fusion)
        e_maps = {item["canonical_id"] for item in tracks["E"]["primary_identities"]}
        self.assertNotIn("gt.recipe.fusionreactor", e_maps)
        self.assertNotIn("gt.recipe.fuels.plasma", e_maps)
        self.assertFalse(any(spec["started"] for spec in tracks.values()))

    def test_load_projections_cite_opening_and_keep_pending_empty(self) -> None:
        document = builder.build_tracks()
        freeze = document["t27_card"]
        self.assertEqual(21000, freeze["axes"]["eager_publication_rows"]["hard_ceiling"])
        self.assertEqual(16862, freeze["axes"]["eager_publication_rows"]["actual"])
        self.assertEqual("PASS", freeze["axes"]["eager_publication_rows"]["verdict"])
        self.assertEqual(3267, freeze["axes"]["datapack_authored_entries"]["actual"])
        self.assertEqual(
            common.PENDING_LOAD_VERDICT,
            freeze["axes"]["lazy_cache_ceiling_rows"]["verdict"],
        )
        a0 = document["initial_cards"]["A0"]
        self.assertEqual(78682, a0["candidate"]["source_rows"])
        self.assertTrue(a0["candidate"]["source_rows_are_not_publication"])
        self.assertIsNone(a0["publication_delta"]["eager"])
        self.assertEqual(
            common.PENDING_LOAD_VERDICT,
            a0["axes"]["eager_publication_rows"]["verdict"],
        )
        self.assertIsNone(a0["axes"]["eager_publication_rows"]["delta"])
        self.assertNotIn("ratio", a0)
        for card in ("A0", "B0", "C0", "D0", "E0"):
            self.assertFalse(document["initial_cards"][card]["started"])
            self.assertEqual(
                freeze["opening_sha256"],
                document["initial_cards"][card]["opening_sha256"],
            )


class T27AggregateTest(unittest.TestCase):
    def test_committed_aggregate_is_derived_and_check_is_read_only(self) -> None:
        path = builder.AGGREGATE
        on_disk = json.loads(path.read_text(encoding="utf-8"))
        expected = builder.build_aggregate()
        self.assertEqual(expected, on_disk)
        before = path.read_bytes()
        self.assertEqual([], builder.check_aggregate())
        self.assertEqual(before, path.read_bytes())

    def test_aggregate_canonical_set_equals_seven_tables_with_no_extra_records(self) -> None:
        document = builder.build_aggregate()
        expected = {
            (table, ident)
            for table in common.TABLE_SPECS
            for ident in common.t13_canonical_ids(table)
        }
        actual = {
            (item["table"], item["canonical_id"]) for item in document["identities"]
        }
        self.assertEqual(expected, actual)
        self.assertEqual(765, document["counts"]["canonical"])
        self.assertEqual(0, document["counts"]["unclassified"])
        self.assertEqual({"eager": 0, "lazy": 0, "logical": 0}, document["publication_delta"])
        self.assertIsNone(document["t28_plus_card_count"])
        self.assertIsNone(document["next_t"])
        open_ids = {item["canonical_id"] for item in document["open_items"]}
        identity_ids = {item["canonical_id"] for item in document["identities"]}
        self.assertTrue(open_ids.isdisjoint(identity_ids - {"crucible"}))
        self.assertNotIn("deferred_open_items", {item["table"] for item in document["identities"]})
        sources = {item["source_artifact"] for item in document["identities"]}
        self.assertEqual(
            {common.relative(builder.output_path(table)) for table in common.TABLE_SPECS},
            sources,
        )
        crafting = [
            item
            for item in document["identities"]
            if item["canonical_id"] == "crafting"
        ]
        self.assertEqual(
            {"cover_kinds", "prefixes"},
            {item["table"] for item in crafting},
        )

    def test_v1_work_set_is_unclosed_required_without_coverage_duplicate(self) -> None:
        document = builder.build_aggregate()
        work = {(item["table"], item["canonical_id"]) for item in document["v1_work_set"]}
        self.assertEqual(set(), work)
        self.assertEqual(0, document["counts"]["v1_work_set"])
        coverage = [
            item
            for item in document["open_items"]
            if item["kind"] == "canonical_coverage"
        ]
        self.assertEqual(["crucible"], [item["canonical_id"] for item in coverage])
        self.assertNotIn(
            ("deferred_open_items", "crucible"),
            work,
        )
        by_id = {(item["table"], item["canonical_id"]): item for item in document["identities"]}
        self.assertEqual("closed", by_id[("multiblock_kinds", "crucible")]["closure"])
        self.assertEqual("cruciblecraft:large_crucible", by_id[("multiblock_kinds", "crucible")]["cc_implementation"])

    def test_dependency_graph_resolves_and_is_acyclic(self) -> None:
        document = builder.build_aggregate()
        graph = document["dependency_graph"]
        nodes = {f"{item['table']}/{item['canonical_id']}" for item in document["identities"]}
        nodes.update(
            f"deferred_open_items/{item['canonical_id']}" for item in document["open_items"]
        )
        self.assertEqual(len(nodes), graph["node_count"])
        edge_pairs = {(edge["from"], edge["to"]) for edge in graph["edges"]}
        self.assertIn(
            ("multiblock_kinds/crucible", "deferred_open_items/O-36"),
            edge_pairs,
        )
        self.assertIn(
            ("deferred_open_items/crucible", "multiblock_kinds/crucible"),
            edge_pairs,
        )
        self.assertIn(
            ("multiblock_kinds/cryo_distillation_tower", "energy_identities/CRYO"),
            edge_pairs,
        )
        for edge in graph["edges"]:
            self.assertIn(edge["from"], nodes)
            self.assertIn(edge["to"], nodes)
        layered = [node for layer in graph["layers"] for node in layer]
        self.assertEqual(sorted(nodes), sorted(layered))
        o36_layer = next(
            index
            for index, layer in enumerate(graph["layers"])
            if "deferred_open_items/O-36" in layer
        )
        crucible_layer = next(
            index
            for index, layer in enumerate(graph["layers"])
            if "multiblock_kinds/crucible" in layer
        )
        self.assertLess(o36_layer, crucible_layer)
        self.assertEqual(0, document["validators"]["broken_dependencies"])
        self.assertEqual(0, document["validators"]["cycles"])
        self.assertEqual(0, document["validators"]["open_item_orphans"])
        self.assertEqual(0, document["validators"]["owner_violations"])
        self.assertFalse(any(spec["started"] for spec in document["tracks"].values()))
        self.assertEqual(
            19087,
            document["opening_publication"]["logical"],
        )
        self.assertEqual(16862, document["opening_publication"]["eager"])
        self.assertEqual(2225, document["opening_publication"]["lazy"])


if __name__ == "__main__":
    unittest.main()
