#!/usr/bin/env python3
"""Decode first-pass Java string obfuscation from the CFR output.

This tool intentionally works on decompiled sources and writes analysis output.
It does not modify the original client assets.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

try:
    from tools.java_source_scan import iter_java_lines, java_unescape
    from tools.string_decoder_registry import DECODER_SPECS, decode_registered
except ModuleNotFoundError:  # Direct execution: python tools/decode_java_strings.py
    from java_source_scan import iter_java_lines, java_unescape
    from string_decoder_registry import DECODER_SPECS, decode_registered


def scan_file(source_root: Path, java_file: Path) -> list[dict]:
    rows: list[dict] = []

    for java_line in iter_java_lines(source_root, java_file):
        for decoder_name, spec in DECODER_SPECS.items():
            for match in spec.call_pattern.finditer(java_line.text):
                encrypted_literal = match.group(1)
                encrypted = java_unescape(encrypted_literal)
                try:
                    decoded = decode_registered(
                        decoder_name,
                        java_line.caller,
                        encrypted,
                    )
                    error = None
                except Exception as exc:  # pragma: no cover - defensive analysis output
                    decoded = ""
                    error = repr(exc)
                rows.append(
                    {
                        "decoder": decoder_name,
                        "path": str(java_file),
                        "line": java_line.line_no,
                        "caller": java_line.caller,
                        "encrypted_literal": encrypted_literal,
                        "decoded": decoded,
                        "error": error,
                    }
                )

    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()

    source_root = args.source_root.resolve()
    rows: list[dict] = []
    for java_file in source_root.rglob("*.java"):
        rows.extend(scan_file(source_root, java_file))

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(rows, ensure_ascii=True, indent=2), encoding="utf-8")

    decoded = sum(1 for row in rows if row["decoded"] and row["error"] is None)
    print(json.dumps({"rows": len(rows), "decoded": decoded, "out": str(args.out)}, ensure_ascii=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
