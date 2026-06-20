#!/usr/bin/env python3
"""Create a separate CFR tree annotated with stable string-map comments."""

from __future__ import annotations

import argparse
import json
import shutil
from collections import defaultdict
from pathlib import Path


DECODED_STATUSES = {"decoded_static", "decoded_existing_plaintext"}


def _comment(record: dict) -> str:
    if record["status"] in DECODED_STATUSES:
        rendered = json.dumps(record.get("decoded", ""), ensure_ascii=False)
        rendered = rendered.replace("*/", r"*\/")
        detail = rendered
    else:
        detail = record["status"]
    return f"/* STRING_MAP {record['id']}: {detail} */"


def annotate_tree(
    source_root: Path,
    records: list[dict],
    output_root: Path,
) -> None:
    source_root = source_root.resolve()
    output_root = output_root.resolve()
    if output_root == source_root or output_root.is_relative_to(source_root):
        raise ValueError("annotation output must be outside the source tree")

    by_location: dict[tuple[str, int], list[dict]] = defaultdict(list)
    for record in records:
        normalized = str(record["path"]).replace("\\", "/")
        by_location[(normalized, int(record["line"]))].append(record)
    for location_records in by_location.values():
        location_records.sort(key=lambda row: row["id"])

    for source_path in source_root.rglob("*"):
        relative = source_path.relative_to(source_root)
        output_path = output_root / relative
        if source_path.is_dir():
            output_path.mkdir(parents=True, exist_ok=True)
            continue

        output_path.parent.mkdir(parents=True, exist_ok=True)
        if source_path.suffix.lower() != ".java":
            shutil.copy2(source_path, output_path)
            continue

        text = source_path.read_text(encoding="utf-8", errors="replace")
        lines = text.splitlines()
        relative_key = relative.as_posix()
        annotated_lines: list[str] = []
        for line_no, line in enumerate(lines, start=1):
            comments = by_location.get((relative_key, line_no), [])
            if comments:
                suffix = " ".join(_comment(record) for record in comments)
                line = f"{line} {suffix}"
            annotated_lines.append(line)
        trailing_newline = "\n" if text.endswith(("\n", "\r")) else ""
        output_path.write_text(
            "\n".join(annotated_lines) + trailing_newline,
            encoding="utf-8",
        )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", required=True, type=Path)
    parser.add_argument("--map", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args()

    records = json.loads(args.map.read_text(encoding="utf-8"))
    annotate_tree(args.source_root, records, args.out)
    print(
        json.dumps(
            {
                "records": len(records),
                "source": str(args.source_root),
                "out": str(args.out),
            },
            ensure_ascii=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
