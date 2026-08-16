#!/usr/bin/env python3
"""Run rebuild, fix known stale builders, then run --record."""
import subprocess, sys, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

def run(cmd):
    print(f"[run] {cmd}")
    r = subprocess.run(cmd, shell=True, cwd=ROOT)
    if r.returncode != 0:
        print(f"FAILED: {cmd}")
    return r.returncode

# Step 1: rebuild all
print("=== Step 1: rebuild ===")
run("python tools/rebuild_artifacts.py --keep-going")

# Step 2: fix known topological-order issues
print("=== Step 2: fix stale builders ===")
run("python tools/build_t10_preflight_projection.py")
# t15 needs --write
run("python tools/build_t15_readiness.py --write")

# Step 3: record
print("=== Step 3: record ===")
sys.exit(run("python tools/run_full_verification.py --record --new-session"))
