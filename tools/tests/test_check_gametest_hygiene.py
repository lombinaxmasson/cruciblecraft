"""GameTest hygiene ratchet, domain grids, and log selection."""
from __future__ import annotations

import unittest

from tools import check_gametest_hygiene as hygiene
from tools import verify


def _kinds(text: str) -> set[str]:
    document = hygiene.load_grids()
    pipes = hygiene.pipe_pairs()
    found = hygiene.collect_text_violations(
        "src/test/java/synthetic/SyntheticGameTests.java",
        text,
        allowed=hygiene.allowed_namespaces(document),
        bootstrap=str(document["bootstrap"]["namespace"]),
        bootstrap_file=str(document["bootstrap"]["file"]),
        constants={},
        prefixes=hygiene.prefix_fields(),
        pipes=pipes,
        forms=hygiene.form_pairs(),
        pipe_forms={form for _material, form in pipes},
    )
    return {item["kind"] for item in found}


class GameTestHygieneTest(unittest.TestCase):
    def test_release_grids_are_at_most_eight(self) -> None:
        release = [
            grid
            for grid in hygiene.load_grids()["grids"]
            if grid.get("release", True)
        ]
        self.assertLessEqual(len(release), 8)
        self.assertEqual(
            {grid["id"] for grid in release},
            {
                "default",
                "machines",
                "energy",
                "logistics",
                "multiblock",
                "worldgen",
                "content",
                "measurement",
            },
        )

    def test_repository_matches_the_ratchet_baseline(self) -> None:
        self.assertEqual(hygiene.check(), [])

    def test_new_throw_cast_pipe_and_orphan_are_violations(self) -> None:
        kinds = _kinds(
            """
            @GameTestHolder("cruciblecraft_wave_left_behind")
            public final class SyntheticGameTests {
                @GameTest
                public void broken(GameTestHelper helper) {
                    var be = (ChestBlockEntity) helper.getBlockEntity(pos);
                    map.findMatch(query).orElseThrow();
                    registry.get();
                    GameTestFixtures.requirePipe(
                            "tin",
                            MaterialPrefixes.TINY_FLUID_PIPE,
                            PipeCatalog.Kind.FLUID);
                    assertEquals(244, catalog.size());
                }
            }
            """
        )
        self.assertIn("orphan_namespace", kinds)
        self.assertIn("cast_block_entity", kinds)
        self.assertIn("or_else_throw", kinds)
        self.assertIn("unknown_pipe", kinds)
        self.assertIn("projection_count", kinds)
        self.assertNotIn("optional_get", kinds)

    def test_cited_projection_and_negative_pipe_are_clean(self) -> None:
        kinds = _kinds(
            """
            @GameTestHolder("cruciblecraft_machines")
            public final class SyntheticGameTests {
                @GameTest
                public void clean(GameTestHelper helper) {
                    // gt6-source: Loader_MultiTileEntities.java:1846
                    assertEquals(244, blades.size());
                    // hygiene-negative: GT6 has no tin fluid pipe
                    GameTestFixtures.requirePipe(
                            "tin",
                            MaterialPrefixes.TINY_FLUID_PIPE,
                            PipeCatalog.Kind.FLUID);
                    GameTestFixtures.requireMaterialStack(
                            helper,
                            "iron",
                            MaterialPrefixes.INGOT,
                            1);
                    GameTestFixtures.requirePipe(
                            helper,
                            "copper",
                            MaterialPrefixes.TINY_FLUID_PIPE,
                            PipeCatalog.Kind.FLUID);
                }
            }
            """
        )
        self.assertEqual(kinds, set())

    def test_ratchet_rejects_a_violation_absent_from_the_baseline(self) -> None:
        item = {
            "path": "src/test/java/new/NewGameTests.java",
            "kind": "or_else_throw",
            "line": 4,
            "excerpt": ".orElseThrow(",
        }
        errors = hygiene.ratchet_errors([item], set())
        self.assertEqual(len(errors), 1)
        self.assertIn("new hygiene violation", errors[0])

    def test_one_gametest_file_selects_its_grid(self) -> None:
        sample = next(
            row
            for row in hygiene.iter_tests()
            if row["namespace"] == "cruciblecraft_logistics"
        )
        selected = verify.game_test_grids_for(
            [sample["file"]],
            command="integration",
        )
        self.assertEqual([grid["id"] for grid in selected], ["logistics"])

    def test_shared_main_source_selects_every_release_grid(self) -> None:
        selected = verify.game_test_grids_for(
            ["src/main/java/com/masson/cruciblecraft/CrucibleCraft.java"],
            command="integration",
        )
        self.assertEqual(len(selected), 8)

    def test_release_selects_every_release_grid(self) -> None:
        selected = verify.game_test_grids_for(
            ["src/test/java/com/masson/cruciblecraft/gametest/BathGameTests.java"],
            command="release",
        )
        self.assertEqual(len(selected), 8)

    def test_gradle_command_passes_the_grid_property(self) -> None:
        command = verify.gradle_command(
            "runGameTestServer",
            ["-PgameTestGrid=logistics"],
        )
        self.assertIn("-PgameTestGrid=logistics", command)
        self.assertNotIn("-PgameTestNamespaces=cruciblecraft_default_grid", command)

    def test_clean_log_passes_and_unnamed_failure_does_not(self) -> None:
        clean = verify.parse_game_test_log(
            "All 3 required tests passed :)\n",
            namespace="cruciblecraft_logistics",
            minimum=3,
        )
        self.assertEqual(clean["status"], "PASS")
        hidden = verify.parse_game_test_log(
            "1 tests are now running\n1 required tests failed :(\n",
            namespace="cruciblecraft_logistics",
            minimum=1,
        )
        self.assertEqual(hidden["status"], "FAIL")
        named = verify.parse_game_test_log(
            "1 tests are now running\n"
            "1 required tests failed :(\n"
            "[Server] exampleFailed failed! boom\n",
            namespace="cruciblecraft_logistics",
            minimum=1,
            allowed_failures={"examplefailed"},
        )
        self.assertEqual(named["status"], "PASS")
        self.assertEqual(named["split_failures"], ["examplefailed"])
        repeated = verify.parse_game_test_log(
            "99 tests are now running\n"
            "11 required tests failed :(\n"
            "[Server] GameTestServer]:    - sameidentityconnectedexportsintostorage\n"
            "[Server] GameTestServer]:    - sameidentityconnectedexportsintostorage\n"
            "[Server] GameTestServer]:    - importpullsfromstorage\n"
            + "".join(
                "[Server] GameTestServer]:    - importpullsfromstorage\n"
                for _ in range(9)
            ),
            namespace="cruciblecraft_logistics",
            minimum=99,
            allowed_failures={
                "sameidentityconnectedexportsintostorage",
                "importpullsfromstorage",
            },
        )
        self.assertEqual(repeated["status"], "PASS")
        self.assertEqual(repeated["failed"], 11)


if __name__ == "__main__":
    unittest.main()
