from __future__ import annotations

import io
import unittest
from contextlib import redirect_stdout
from unittest import mock

from tools import verify as verify_entry


class T35ReplayProfileTest(unittest.TestCase):
    def integration_commands(
        self,
        profile_name: str,
    ) -> tuple[list[tuple[str, list[str]]], str]:
        commands: list[tuple[str, list[str]]] = []

        def record(
            name: str,
            command: list[str],
            *,
            timings: list[tuple[str, float]] | None = None,
        ) -> int:
            commands.append((name, command))
            if timings is not None:
                timings.append((name, 0.125))
            return 0

        output = io.StringIO()
        with (
            mock.patch.object(verify_entry, "run_command", side_effect=record),
            redirect_stdout(output),
        ):
            self.assertEqual(
                0,
                verify_entry.main(["integration", "--profile", profile_name]),
            )
        return commands, output.getvalue()

    def test_census_recipe_family_uses_compact_reference_entrypoint(self) -> None:
        commands, _output = self.integration_commands("census")
        recipe_commands = [
            command
            for name, command in commands
            if name == "builder:build_t35_recipe_families_compact"
        ]
        self.assertEqual(1, len(recipe_commands))
        self.assertEqual(
            "tools/build_t35_recipe_families_compact.py",
            recipe_commands[0][-2],
        )
        self.assertEqual(
            ["--check"],
            recipe_commands[0][-1:],
        )
        self.assertNotIn("--full-replay", recipe_commands[0])
        python_commands = [
            (name, command)
            for name, command in commands
            if name.startswith("python:")
        ]
        self.assertEqual(1, len(python_commands))
        self.assertEqual("python:census", python_commands[0][0])
        python_argv = python_commands[0][1]
        self.assertEqual(
            "modules",
            python_argv[python_argv.index("--suite") + 1],
        )
        self.assertNotIn("affected", python_argv)
        self.assertIn("--module", python_argv)

    def test_census_replay_runs_t36_overlay_builders(self) -> None:
        commands, output = self.integration_commands("census-replay")
        by_name = {name: command for name, command in commands}
        self.assertIn("builder:build_t36_census_delta", by_name)
        self.assertIn("builder:build_t36_readiness", by_name)
        self.assertIn("builder:build_t37_census_delta", by_name)
        self.assertIn("builder:build_t37_card_topology", by_name)
        self.assertIn("builder:build_t37_readiness", by_name)
        self.assertIn("builder:build_t47_recipe_load_benchmark", by_name)
        self.assertNotIn("builder:build_t37_recipe_load_benchmark", by_name)
        self.assertNotIn("builder:build_t46_recipe_load_benchmark", by_name)
        self.assertEqual(
            ["--check"],
            by_name["builder:build_t36_census_delta"][-1:],
        )
        self.assertEqual(
            ["--check"],
            by_name["builder:build_t36_readiness"][-1:],
        )
        self.assertEqual(
            ["--check"],
            by_name["builder:build_t37_census_delta"][-1:],
        )
        self.assertTrue(
            by_name["builder:build_t36_census_delta"][-2].endswith(
                "build_t36_census_delta.py"
            )
        )
        self.assertTrue(
            by_name["builder:build_t36_readiness"][-2].endswith(
                "build_t36_readiness.py"
            )
        )
        self.assertIn("integration wall-time summary:", output)
        self.assertIn("builder:build_t36_census_delta: 0.125s", output)
        self.assertIn("builder:build_t36_readiness: 0.125s", output)
        python_commands = [
            (name, command)
            for name, command in commands
            if name.startswith("python:")
        ]
        self.assertEqual(1, len(python_commands))
        self.assertEqual("python:census-replay", python_commands[0][0])
        python_argv = python_commands[0][1]
        self.assertEqual(
            "modules",
            python_argv[python_argv.index("--suite") + 1],
        )
        self.assertNotIn("affected", python_argv)
        self.assertIn("test_build_t36_census_delta", python_argv)
        self.assertNotIn("test_build_t46_recipe_load_benchmark", python_argv)

    def test_census_replay_full_replay_restores_historical_load(self) -> None:
        commands: list[tuple[str, list[str]]] = []

        def record(
            name: str,
            command: list[str],
            *,
            timings: list[tuple[str, float]] | None = None,
        ) -> int:
            commands.append((name, command))
            if timings is not None:
                timings.append((name, 0.125))
            return 0

        output = io.StringIO()
        with (
            mock.patch.object(verify_entry, "run_command", side_effect=record),
            redirect_stdout(output),
        ):
            self.assertEqual(
                0,
                verify_entry.main(
                    ["integration", "--profile", "census-replay", "--full-replay"]
                ),
            )
        by_name = {name: command for name, command in commands}
        self.assertIn("builder:build_t37_recipe_load_benchmark", by_name)
        self.assertIn("builder:build_t46_recipe_load_benchmark", by_name)
        self.assertIn("builder:build_t47_recipe_load_benchmark", by_name)
        python_commands = [
            command
            for name, command in commands
            if name == "python:census-replay"
        ]
        self.assertEqual(1, len(python_commands))
        self.assertEqual(
            "modules",
            python_commands[0][python_commands[0].index("--suite") + 1],
        )
        self.assertIn("test_build_t46_recipe_load_benchmark", python_commands[0])

    def test_command_prints_deterministic_wall_time(self) -> None:
        output = io.StringIO()
        completed = mock.Mock(returncode=0)
        with (
            mock.patch.object(
                verify_entry.subprocess,
                "run",
                return_value=completed,
            ),
            mock.patch.object(
                verify_entry.time,
                "perf_counter",
                side_effect=[10.0, 10.125],
            ),
            redirect_stdout(output),
        ):
            self.assertEqual(
                0,
                verify_entry.run_command("test:timed", ["python", "-V"]),
            )
        self.assertIn("[test:timed] wall-time: 0.125s", output.getvalue())


if __name__ == "__main__":
    unittest.main()
