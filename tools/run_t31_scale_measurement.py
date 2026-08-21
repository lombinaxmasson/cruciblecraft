#!/usr/bin/env python3
"""Run a dedicated T31 scale GameTest measurement.

Ordinary runGameTestServer does not register these tests. This runner
requires physical RAM >= 16 GiB and -Pt31Scale=<small|target|stress>.
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from tools import t31_common as t31  # noqa: E402

MIN_RAM = t31.MIN_PHYSICAL_RAM_BYTES


def physical_ram_bytes() -> int:
    if sys.platform == "win32":
        import ctypes

        class MEMORYSTATUSEX(ctypes.Structure):
            _fields_ = [
                ("dwLength", ctypes.c_ulong),
                ("dwMemoryLoad", ctypes.c_ulong),
                ("ullTotalPhys", ctypes.c_ulonglong),
                ("ullAvailPhys", ctypes.c_ulonglong),
                ("ullTotalPageFile", ctypes.c_ulonglong),
                ("ullAvailPageFile", ctypes.c_ulonglong),
                ("ullTotalVirtual", ctypes.c_ulonglong),
                ("ullAvailVirtual", ctypes.c_ulonglong),
                ("ullAvailExtendedVirtual", ctypes.c_ulonglong),
            ]

        status = MEMORYSTATUSEX()
        status.dwLength = ctypes.sizeof(MEMORYSTATUSEX)
        if ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(status)) == 0:
            raise OSError("GlobalMemoryStatusEx failed")
        return int(status.ullTotalPhys)
    page = os.sysconf("SC_PAGE_SIZE")
    phys = os.sysconf("SC_PHYS_PAGES")
    return int(page) * int(phys)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--scenario",
        required=True,
        choices=("small", "target", "stress"),
    )
    args = parser.parse_args(argv)
    ram = physical_ram_bytes()
    if ram < MIN_RAM:
        print(
            f"BLOCKED_ENVIRONMENT physical_ram_bytes={ram} min={MIN_RAM}",
            file=sys.stderr,
        )
        return 2
    env = os.environ.copy()
    env["PYTHONUTF8"] = "1"
    gradle = ROOT / "gradlew.bat" if sys.platform == "win32" else ROOT / "gradlew"
    command = [
        str(gradle),
        "runGameTestServer",
        f"-Pt31Scale={args.scenario}",
    ]
    print("physical_ram_bytes", ram)
    print("running", " ".join(command))
    completed = subprocess.run(command, cwd=ROOT, env=env, check=False)
    if completed.returncode != 0:
        return completed.returncode
    samples = t31.sample_paths(args.scenario)
    if len(samples) < 5:
        print(
            f"expected 5 samples, found {len(samples)} in {t31.SAMPLES / args.scenario}",
            file=sys.stderr,
        )
        return 1
    summary = {
        "scenario": args.scenario,
        "physical_ram_bytes": ram,
        "samples": [path.name for path in samples],
    }
    print(json.dumps(summary, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
