#!/usr/bin/env python3
"""Group unresolved string calls into prioritized offline dump targets."""

from __future__ import annotations

import argparse
import json
from collections import defaultdict
from pathlib import Path


TARGET_STATUSES = {"dynamic_dump_required", "decode_error"}
CRITICAL_MARKERS = (
    "startapp",
    "jlogin",
    "clawworkspace",
    "jproductselector",
    "auth",
    "license",
)
HIGH_MARKERS = (
    "http",
    "token",
    "payment",
    "pay",
    "order",
    "api",
    "dthelper",
    "sbfapi",
)


def _priority(family: str, records: list[dict]) -> str:
    evidence = " ".join(
        [
            family,
            *(
                f"{record.get('path', '')} {record.get('caller', '')}"
                for record in records
            ),
        ]
    ).casefold()
    if any(marker in evidence for marker in CRITICAL_MARKERS):
        return "critical"
    if any(marker in evidence for marker in HIGH_MARKERS):
        return "high"
    return "normal"


def _internal_name(definition: dict) -> str | None:
    path = str(definition.get("path", "")).replace("\\", "/")
    marker = "/com/"
    index = path.find(marker)
    if index < 0 or not path.endswith(".java"):
        return None
    return path[index + 1 : -5]


def build_targets(
    inventory_rows: list[dict],
    string_records: list[dict],
) -> list[dict]:
    inventory = {row["family"]: row for row in inventory_rows}
    grouped: dict[str, list[dict]] = defaultdict(list)
    for record in string_records:
        if record.get("status") in TARGET_STATUSES:
            grouped[record["decoder"]].append(record)

    targets: list[dict] = []
    for family, records in grouped.items():
        owner_class, method_name = family.rsplit(".", 1)
        inventory_row = inventory.get(family, {})
        definitions = inventory_row.get("definitions", [])
        statuses = {record["status"] for record in records}
        reasons = sorted(
            {
                str(record.get("evidence", {}).get("reason", "")).strip()
                for record in records
                if record.get("evidence", {}).get("reason")
            }
        )
        targets.append(
            {
                "family": family,
                "status": (
                    "decode_error"
                    if "decode_error" in statuses
                    else "dynamic_dump_required"
                ),
                "call_count": len(records),
                "owner_class": owner_class,
                "method_name": method_name,
                "descriptor_or_inferred_signature": (
                    f"static java.lang.String {method_name}(java.lang.String)"
                ),
                "owner_internal_names": sorted(
                    {
                        internal_name
                        for definition in definitions
                        if (internal_name := _internal_name(definition))
                    }
                ),
                "definition_locations": definitions,
                "sample_calls": [
                    {
                        "id": record.get("id"),
                        "path": record.get("path"),
                        "line": record.get("line"),
                        "caller": record.get("caller"),
                        "encrypted_literal": record.get("encrypted_literal"),
                    }
                    for record in sorted(records, key=lambda row: row.get("id", ""))[:5]
                ],
                "reason": "; ".join(reasons) or "static decode unresolved",
                "priority": _priority(family, records),
            }
        )

    priority_order = {"critical": 0, "high": 1, "normal": 2}
    targets.sort(
        key=lambda row: (
            priority_order[row["priority"]],
            -row["call_count"],
            row["family"],
        )
    )
    return targets


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--inventory", required=True, type=Path)
    parser.add_argument("--unresolved", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    parser.add_argument("--agent-targets", type=Path)
    args = parser.parse_args()

    inventory_rows = json.loads(args.inventory.read_text(encoding="utf-8"))
    string_records = json.loads(args.unresolved.read_text(encoding="utf-8"))
    targets = build_targets(inventory_rows, string_records)
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(
        json.dumps(targets, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    if args.agent_targets:
        args.agent_targets.parent.mkdir(parents=True, exist_ok=True)
        lines = ["owner_internal_name\tmethod_name\tfamily\tpriority"]
        for target in targets:
            for owner in target["owner_internal_names"]:
                lines.append(
                    "\t".join(
                        (
                            owner,
                            target["method_name"],
                            target["family"],
                            target["priority"],
                        )
                    )
                )
        args.agent_targets.write_text("\n".join(lines) + "\n", encoding="utf-8")
    counts = {
        priority: sum(target["priority"] == priority for target in targets)
        for priority in ("critical", "high", "normal")
    }
    print(
        json.dumps(
            {
                "targets": len(targets),
                "priorities": counts,
                "out": str(args.out),
                "agent_targets": (
                    str(args.agent_targets) if args.agent_targets else None
                ),
            },
            ensure_ascii=True,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
