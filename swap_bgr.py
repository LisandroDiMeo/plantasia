#!/usr/bin/env python3
"""
Applies a BGR↔RGB channel swap to pixel data in TFT image header files.
Run once per file after exporting from rinkydinkelectronics.
Do NOT run twice on the same file — the swap is its own inverse.

Usage: python3 swap_bgr.py monstera.h cover_plantasia_mort.h drop.h
"""

import re
import sys
from pathlib import Path


def bgr_swap(pixel: int) -> int:
    r = (pixel & 0xF800) >> 11
    g =  pixel & 0x07E0
    b =  pixel & 0x001F
    return (b << 11) | g | r


def process_file(path: str) -> None:
    content = Path(path).read_text()

    count = 0

    def replace(m: re.Match) -> str:
        nonlocal count
        count += 1
        return f'0x{bgr_swap(int(m.group(), 16)):04x}'

    result = re.sub(r'0x[0-9a-fA-F]{4}', replace, content)

    Path(path).write_text(result)
    print(f'{path}: {count} pixels swapped')


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('Usage: python3 swap_bgr.py <file.h> [file2.h ...]')
        sys.exit(1)

    for path in sys.argv[1:]:
        process_file(path)
