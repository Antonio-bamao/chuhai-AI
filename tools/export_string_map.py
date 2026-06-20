#!/usr/bin/env python3
"""Normalize raw string-call records and export stable JSON/CSV artifacts."""

from __future__ import annotations

import argparse
import csv
import json
from pathlib import Path


FIELDS = [
    "id",
    "decoder",
    "path",
    "line",
    "caller",
    "encrypted_literal",
    "decoded",
    "status",
    "error",
    "evidence",
]
DECODED_STATUSES = {"decoded_static", "decoded_existing_plaintext"}


def _normalized_path(value: str) -> str:
    return value.replace("\\", "/")


def normalize_records(records: list[dict]) -> list[dict]:
    ordered = sorted(
        records,
        key=lambda row: (
            _normalized_path(str(row["path"])).casefold(),
            int(row["line"]),
            str(row["decoder"]),
            str(row["encrypted_literal"]),
        ),
    )
    normalized: list[dict] = []
    for index, source in enumerate(ordered, start=1):
        row = {
            "id": f"sm-{index:06d}",
            "decoder": str(source["decoder"]),
            "path": _normalized_path(str(source["path"])),
            "line": int(source["line"]),
            "caller": str(source["caller"]),
            "encrypted_literal": str(source["encrypted_literal"]),
            "decoded": str(source.get("decoded") or ""),
            "status": str(source["status"]),
            "error": source.get("error"),
            "evidence": source.get("evidence", {}),
        }
        normalized.append(row)
    return normalized


def export_records(
    records: list[dict],
    json_path: Path,
    csv_path: Path,
    unresolved_path: Path,
) -> list[dict]:
    normalized = normalize_records(records)
    unresolved = [
        row for row in normalized if row["status"] not in DECODED_STATUSES
    ]

    for path in (json_path, csv_path, unresolved_path):
        path.parent.mkdir(parents=True, exist_ok=True)

    json_path.write_text(
        json.dumps(normalized, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    unresolved_path.write_text(
        json.dumps(unresolved, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    with csv_path.open("w", encoding="utf-8-sig", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=FIELDS)
        writer.writeheader()
        for row in normalized:
            csv_row = dict(row)
            csv_row["error"] = "" if row["error"] is None else str(row["error"])
            csv_row["evidence"] = json.dumps(
                row["evidence"],
                ensure_ascii=False,
                sort_keys=True,
            )
            writer.writerow(csv_row)

    return normalized


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--json", required=True, type=Path)
    parser.add_argument("--csv", required=True, type=Path)
    parser.add_argument("--unresolved", required=True, type=Path)
    args = parser.parse_args()

    records = json.loads(args.input.read_text(encoding="utf-8"))
    normalized = export_records(
        records,
        args.json,
        args.csv,
        args.unresolved,
    )
    unresolved_count = sum(
        row["status"] not in DECODED_STATUSES for row in normalized
    )
    print(
        json.dumps(
            {
                "rows": len(normalized),
                "unresolved": unresolved_count,
                "json": str(args.json),
                "csv": str(args.csv),
            },
            ensure_ascii=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
