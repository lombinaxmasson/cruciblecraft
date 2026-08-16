#!/usr/bin/env python3
"""Generate the neutral ore-fleck texture for tinted material ore blocks.

The texture is a 16x16 RGBA PNG: transparent background with a fixed-seed
pattern of light-gray speckles. Ore block models multiply this texture by each
material's color at render time (tintindex 0), so the flecks stay neutral and
the material hue is preserved at full saturation.
"""
from __future__ import annotations

import random
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

OUTPUT = (
    ROOT
    / "src/main/resources/assets/cruciblecraft/textures/block/material/ore_flecks.png"
)

SIZE = 16
# Fixed pattern seed; changing it changes the committed texture.
SEED = 20260815

# Neutral grays: mostly bright speckles with a few darker accents. Tint
# multiplies these, so near-white values keep the material hue clean.
SPECKLE_RGB = (200, 200, 200)
ACCENT_RGB = (110, 110, 110)


def _png_chunk(tag: bytes, payload: bytes) -> bytes:
    return (
        struct.pack(">I", len(payload))
        + tag
        + payload
        + struct.pack(">I", zlib.crc32(tag + payload) & 0xFFFFFFFF)
    )


def build_pixels() -> list[list[tuple[int, int, int, int]]]:
    """Paint the deterministic 16x16 RGBA fleck pattern."""
    rng = random.Random(SEED)
    pixels = [[(0, 0, 0, 0) for _ in range(SIZE)] for _ in range(SIZE)]

    def blotch(x: int, y: int, color: tuple[int, int, int]) -> None:
        radius = rng.randint(1, 2)
        for dy in range(-radius, radius + 1):
            for dx in range(-radius, radius + 1):
                if dx * dx + dy * dy > radius * radius:
                    continue
                px, py = x + dx, y + dy
                if 1 <= px < SIZE - 1 and 1 <= py < SIZE - 1:
                    alpha = (
                        255
                        if dx == 0 and dy == 0
                        else rng.randint(150, 220)
                    )
                    pixels[py][px] = (*color, alpha)

    # Speckles stay off the outer pixel ring so adjacent-block seams
    # remain clean under cullface rendering. Centers come from a
    # jittered 4x4-cell grid over the 12x12 interior (one at most per
    # 3px cell), which scatters the clusters without ever merging them
    # into belts and always terminates, unlike rejection sampling.
    cells = [(x, y) for y in range(4) for x in range(4)]
    rng.shuffle(cells)
    for index, (cell_x, cell_y) in enumerate(cells[:10]):
        blotch(
            2 + cell_x * 3 + rng.randint(0, 1),
            2 + cell_y * 3 + rng.randint(0, 1),
            ACCENT_RGB if index >= 8 else SPECKLE_RGB,
        )
    return pixels


def encode() -> bytes:
    """Serialize the fleck pattern as a PNG (RGBA, no filter)."""
    raw = b"".join(
        b"\x00" + b"".join(struct.pack("4B", *pixel) for pixel in row)
        for row in build_pixels()
    )
    return (
        b"\x89PNG\r\n\x1a\n"
        + _png_chunk(
            b"IHDR",
            struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0),
        )
        + _png_chunk(b"IDAT", zlib.compress(raw, 9))
        + _png_chunk(b"IEND", b"")
    )


def main() -> int:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_bytes(encode())
    print(f"Wrote {OUTPUT.relative_to(ROOT)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
