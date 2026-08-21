from __future__ import annotations

import unittest

from tools import run_t31_compat_smoke as runner


def _launch(**overrides: object) -> dict:
    row = {
        "done_line": False,
        "reached_ready": False,
        "emi_loaded": False,
        "jade_loaded": False,
        "kubejs_loaded": False,
        "cruciblecraft_loaded": False,
        "cruciblecraft_published": False,
        "world_present": False,
        "skipped_block_entity": False,
        "failed_to_start": False,
    }
    row.update(overrides)
    return row


class T31CompatSmokeAxesTest(unittest.TestCase):
    def test_derived_axes_require_measured_launches(self) -> None:
        empty = runner.derived_axes(
            {
                "junit": {"passed": True},
                "last_required_gametest_pass": True,
            }
        )
        self.assertFalse(any(empty.values()))
        self.assertFalse(empty["save_reload"])

    def test_derived_axes_pass_when_matrix_is_measured(self) -> None:
        axes = runner.derived_axes(
            {
                "junit": {"passed": True},
                "last_required_gametest_pass": True,
                "dedicated_cc_only": _launch(
                    done_line=True,
                    cruciblecraft_loaded=True,
                    cruciblecraft_published=True,
                    world_present=True,
                ),
                "dedicated_cc_only_reload": _launch(done_line=True),
                "dedicated_optional": _launch(done_line=True, jade_loaded=True),
                "client_cc_only": _launch(
                    reached_ready=True,
                    cruciblecraft_loaded=True,
                ),
                "client_optional": _launch(
                    reached_ready=True,
                    cruciblecraft_loaded=True,
                    emi_loaded=True,
                    jade_loaded=True,
                ),
            }
        )
        self.assertEqual(
            {
                "save_reload": True,
                "dedicated_cc_only": True,
                "client_cc_only": True,
                "emi": True,
                "jade": True,
                "kubejs": True,
            },
            axes,
        )

    def test_cc_only_fails_if_emi_loaded(self) -> None:
        axes = runner.derived_axes(
            {
                "junit": {"passed": True},
                "last_required_gametest_pass": True,
                "dedicated_cc_only": _launch(
                    done_line=True,
                    cruciblecraft_loaded=True,
                    cruciblecraft_published=True,
                    world_present=True,
                    emi_loaded=True,
                ),
                "dedicated_cc_only_reload": _launch(done_line=True),
                "client_cc_only": _launch(
                    reached_ready=True,
                    cruciblecraft_loaded=True,
                    emi_loaded=True,
                ),
                "client_optional": _launch(emi_loaded=True, jade_loaded=True),
            }
        )
        self.assertFalse(axes["dedicated_cc_only"])
        self.assertFalse(axes["client_cc_only"])


if __name__ == "__main__":
    unittest.main()
