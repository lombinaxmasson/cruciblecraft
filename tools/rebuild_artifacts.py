#!/usr/bin/env python3
"""Rebuild every committed builder artifact in dependency order.

`tools/verification_builder_policy.json` already lists the 66 builders in
topological order, but every entry only carries read-only `--check` argv.
This script supplies the missing half: it walks the same ordered list and
runs each builder in its *write* mode, so a change to a shared input can be
propagated down the whole currentness chain in one command instead of being
rediscovered one artifact at a time by the full verification run.

Write-mode argv is detected from each builder's own argparse flags:

  * declares ``--write``            -> run with ``--write``
  * declares only ``--check``       -> run with no flags (writes by default)
  * listed in ``SKIP``              -> never rebuilt here (see notes below)

Before the main chain, ``pre_chain_builders`` from the policy runs first:
writers whose outputs are pinned by policy builders but which must never
enter the ordinary check chain.

Usage
  python tools/rebuild_artifacts.py --dry-run      # print the plan, run nothing
  python tools/rebuild_artifacts.py                # rebuild everything, in order
  python tools/rebuild_artifacts.py --from t12     # resume at first match
  python tools/rebuild_artifacts.py --only t16,t17 # rebuild a subset, in order
  python tools/rebuild_artifacts.py --verify       # rebuild, then --check all

This is a refresh tool, not a verification tool. It deliberately does NOT
decide whether a rebuild was legitimate. Always read `git diff` afterwards:
a rebuild that changes a *historical* baseline is a bug, not a refresh.

The policy order is topological only in intent; `tools/check_builder_graph.py`
fails hard when an artifact pins a later builder's output.  The fixed-point
round loop below is a regression guard: with an acyclic graph it converges
in one round, and a re-introduced back edge fails loudly instead of silently
leaving stale artifacts.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
POLICY = ROOT / "tools" / "verification_builder_policy.json"

# Builders whose output must never be written by this script.
SKIP = {
    # status field is owned exclusively by run_full_verification --record
    "build_t21_readiness": "status is owned by run_full_verification",
    # writes the 310 MB report / 130 MB reference and needs the raw dump;
    # its write modes are --write-reference / --write-report / --write-process-
    # expectations and must be chosen deliberately, never swept into a batch
    "compare_gt6_recipes": "manual only — see --write-reference / --write-report",
    # write mode re-derives the CLOSED T14 materialization decision from
    # whatever build/t14-benchmark/raw_measurements.json happens to be on this
    # machine.  A batch refresh must never silently replace closed-phase
    # measurement evidence or re-rank the three candidates.
    "build_t14_recipe_load_benchmark": "rewrites closed T14 measurement evidence",
}

# Directories sampled after EVERY builder to catch intra-round oscillation.
# Round-boundary comparison alone is not enough: if builder i writes a file and
# builder j>i writes it back, net churn is zero and the round looks settled,
# while any builder between i and j has recorded the transient value.  That is
# exactly how t10_preflight_projection captured the CRLF material tree.
PROBE_ROOTS = ("src/main/resources/data/cruciblecraft", "tools")

MAX_ROUNDS = 5

# Explicit overrides when argparse detection is not enough.
WRITE_ARGV: dict[str, list[str]] = {
    # Builders whose argparse requires --write --full-replay together
    "build_t18_o37_identity_projection": ["--check"],  # no write flag; rebuild not supported
    "build_t21_operand_reachability": ["--json"],
    "build_t20_worldgen_source": ["--write", "--full-replay"],
    "build_t21_chemical_axis": ["--write", "--full-replay"],
    "build_t21_source_denominator": ["--write", "--full-replay"],
    "build_gt6_mixer_templates": ["--write", "--full-replay"],
    "build_t21_template_denominator": ["--write", "--full-replay"],
    "build_t21_mixer_gunpowder": ["--write", "--full-replay"],
    "build_t22_5_fluid_mapping": ["--write", "--full-replay"],
    "build_t22_5_shape_analysis": ["--write", "--full-replay"],
    "build_t22_5_row_classification": ["--write", "--full-replay"],
    "build_t22_5_fluid_gap_disposition": ["--write", "--full-replay"],
    "build_t22_5_denominator_recompute": ["--write", "--full-replay"],
}

FLAG_RE = re.compile(r'add_argument\(\s*"(--[a-z0-9-]+)"')


def pre_pass() -> list[tuple[str, list[str]]]:
    """Off-policy writers, declared by the policy itself."""
    document = json.loads(POLICY.read_text(encoding="utf-8"))
    return [
        (row["script"], [])
        for row in document.get("pre_chain_builders", [])
    ]


def detect_write_argv(script: Path) -> list[str] | None:
    """Return write-mode argv for a builder, or None if it cannot be decided."""
    try:
        source = script.read_text(encoding="utf-8", errors="ignore")
    except OSError:
        return None
    flags = set(FLAG_RE.findall(source))
    if "--write" in flags:
        return ["--write"]
    if "--check" in flags:
        return []
    return None


def load_plan() -> list[dict]:
    document = json.loads(POLICY.read_text(encoding="utf-8"))
    plan = []
    for row in document["builders"]:
        name = row["name"]
        script = ROOT / row["script"]
        entry = {
            "name": name,
            "script": row["script"],
            "check_argv": list(row["ordinary_args"]),
            "write_argv": None,
            "status": "ok",
        }
        if name in SKIP:
            entry["status"] = f"skip: {SKIP[name]}"
        elif not script.is_file():
            entry["status"] = "skip: script missing"
        elif name in WRITE_ARGV:
            entry["write_argv"] = WRITE_ARGV[name]
        else:
            argv = detect_write_argv(script)
            if argv is None:
                entry["status"] = "skip: write mode undetected — add to WRITE_ARGV"
            else:
                entry["write_argv"] = argv
        plan.append(entry)
    return plan


def tree_state(roots: tuple[str, ...] = ("tools", "src", "build")) -> dict[str, str]:
    """sha256 of every tracked file, used to detect a fixed point.

    Hashes raw bytes on purpose: several builders record byte-level digests of
    these trees, so a line-ending-only rewrite is a real difference to them
    even though every `--check` path (which uses `read_text`) is blind to it.
    """
    import hashlib
    state = {}
    for base in roots:
        root = ROOT / base
        if not root.is_dir():
            continue
        for path in root.rglob("*"):
            if path.is_file() and "__pycache__" not in path.parts:
                state[str(path.relative_to(ROOT))] = hashlib.sha256(
                    path.read_bytes()).hexdigest()
    return state


def report_oscillation(history: list[tuple[str, dict[str, str]]]) -> list[str]:
    """Files that took more than one value during a single round.

    A file whose value at the end of the round equals its value at the start,
    but which held a different value in between, means every builder that ran
    inside that window hashed a state that no longer exists.  This is reported
    even when net churn is zero.
    """
    tracked: dict[str, list[tuple[str, str]]] = {}
    for step, state in history:
        for name, digest in state.items():
            series = tracked.setdefault(name, [])
            if not series or series[-1][1] != digest:
                series.append((step, digest))
    findings = []
    for name, series in sorted(tracked.items()):
        if len(series) < 3:
            continue
        if series[0][1] == series[-1][1]:
            path = " -> ".join(step for step, _ in series[1:])
            findings.append(f"{name}: written and reverted by {path}")
    return findings


def run(entry: dict, argv: list[str]) -> tuple[bool, float, str]:
    command = [sys.executable, entry["script"], *argv]
    started = time.perf_counter()
    env = {**os.environ, "PYTHONIOENCODING": "utf-8"}
    completed = subprocess.run(
        command, cwd=ROOT, capture_output=True, text=True, check=False, env=env
    )
    elapsed = time.perf_counter() - started
    output = (completed.stdout or "") + (completed.stderr or "")
    return completed.returncode == 0, elapsed, output.strip()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--from", dest="start", metavar="SUBSTR")
    parser.add_argument("--only", metavar="SUBSTR[,SUBSTR...]")
    parser.add_argument("--verify", action="store_true",
                        help="after rebuilding, run every builder's --check argv")
    parser.add_argument("--keep-going", action="store_true",
                        help="do not stop at the first failing builder")
    args = parser.parse_args()

    plan = load_plan()

    if args.only:
        wanted = [s.strip() for s in args.only.split(",") if s.strip()]
        plan = [e for e in plan if any(w in e["name"] for w in wanted)]
    if args.start:
        for index, entry in enumerate(plan):
            if args.start in entry["name"]:
                plan = plan[index:]
                break
        else:
            print(f"no builder matches --from {args.start}", file=sys.stderr)
            return 2

    width = max((len(e["name"]) for e in plan), default=0)
    runnable = [e for e in plan if e["status"] == "ok"]
    skipped = [e for e in plan if e["status"] != "ok"]

    print(f"plan: {len(runnable)} to rebuild, {len(skipped)} skipped\n")
    for entry in plan:
        if entry["status"] == "ok":
            shown = " ".join(entry["write_argv"]) or "(no flags = write)"
            print(f"  {entry['name']:<{width}}  {shown}")
        else:
            print(f"  {entry['name']:<{width}}  -- {entry['status']}")

    if args.dry_run:
        return 0

    print("\nrebuilding...\n")
    failures: list[tuple[str, str]] = []

    if not args.only and not args.start:
        print("pre-pass: off-policy writers\n")
        for script, argv in pre_pass():
            if not (ROOT / script).is_file():
                continue
            ok, elapsed, output = run({"script": script}, argv)
            print(f"[{'OK  ' if ok else 'WARN'}] {script:<{width}} {elapsed:7.3f}s")
        print()

    # The policy order is topological only in intent: several artifacts pin the
    # hash of a file written by a LATER builder (run tools/check_builder_graph.py
    # for the current list).  A single forward pass therefore cannot converge, so
    # repeat until the tree stops changing.
    settled = False
    for round_number in range(1, MAX_ROUNDS + 1):
        before = tree_state()
        print(f"--- round {round_number} ---")
        round_failures: list[tuple[str, str]] = []
        history: list[tuple[str, dict[str, str]]] = [
            ("<round start>", tree_state(PROBE_ROOTS))
        ]
        for entry in runnable:
            ok, elapsed, output = run(entry, entry["write_argv"])
            history.append((entry["name"], tree_state(PROBE_ROOTS)))
            mark = "OK  " if ok else "FAIL"
            print(f"[{mark}] {entry['name']:<{width}} {elapsed:7.3f}s")
            if not ok:
                print("\n".join("        " + line
                                for line in output.splitlines()[-12:]))
                round_failures.append((entry["name"], output))
                if not args.keep_going:
                    print("\nstopped at first failure; rerun with --keep-going "
                          "to continue past it")
                    break
        after = tree_state()
        churn = sorted(k for k in set(before) | set(after)
                       if before.get(k) != after.get(k))
        oscillating = report_oscillation(history)
        print(f"\nround {round_number}: {len(churn)} file(s) changed, "
              f"{len(oscillating)} oscillating, "
              f"{len(round_failures)} failure(s)\n")
        if oscillating:
            print("INTRA-ROUND OSCILLATION — a builder recorded a transient state:")
            for line in oscillating[:15]:
                print(f"  - {line}")
            if len(oscillating) > 15:
                print(f"  ... +{len(oscillating) - 15} more")
            print("\nNet churn can be zero here and the round will still look "
                  "settled. Fix the writer, do not add rounds. If the two "
                  "writers disagree only on line endings, run "
                  "python tools/check_text_write_newline.py")
            return 1
        failures = round_failures
        if round_failures and not args.keep_going:
            break
        if not churn:
            settled = True
            print(f"fixed point reached after {round_number} round(s).\n")
            break
        if round_number == MAX_ROUNDS:
            print(f"NOT CONVERGED after {MAX_ROUNDS} rounds. Still changing:")
            for name in churn[:20]:
                print(f"  - {name}")
            print("\nThis is a dependency cycle, not a slow refresh. Run "
                  "python tools/check_builder_graph.py to see which artifact "
                  "pins a later builder's output.")
            return 1
    if not settled and not failures:
        return 1

    if args.verify and not failures:
        print("\nverifying with --check argv...\n")
        for entry in runnable:
            ok, elapsed, output = run(entry, entry["check_argv"])
            mark = "OK  " if ok else "FAIL"
            print(f"[{mark}] {entry['name']:<{width}} {elapsed:7.3f}s")
            if not ok:
                print("\n".join("        " + line
                                for line in output.splitlines()[-12:]))
                failures.append((entry["name"] + " (check)", output))

    print()
    if failures:
        print(f"{len(failures)} builder(s) failed:")
        for name, _ in failures:
            print(f"  - {name}")
        print("\nA builder that fails in write mode usually means its own "
              "inputs are inconsistent, not that the rebuild is wrong. "
              "Read the error before touching any expected value.")
        return 1

    print("all requested builders rebuilt.")
    print("NOW READ `git diff`. Any change to a *historical* baseline "
          "(t1x_publication_baseline.json, *_machine_readiness.json history, "
          "closed-phase counts) is a bug, not a refresh.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
