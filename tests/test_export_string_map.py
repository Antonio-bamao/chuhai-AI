import csv
import json
import unittest
from pathlib import Path

from tools.export_string_map import export_records


class ExportStringMapTests(unittest.TestCase):
    def test_exports_deterministic_json_csv_and_unresolved_rows(self):
        records = [
            {
                "decoder": "B.y",
                "path": r"z\B.java",
                "line": 20,
                "caller": "z.Brun",
                "encrypted_literal": r"\u0002",
                "decoded": '中文,\n"quoted"',
                "status": "decoded_static",
                "error": None,
                "evidence": {"note": "multiline"},
            },
            {
                "decoder": "A.x",
                "path": "a/A.java",
                "line": 10,
                "caller": "a.Arun",
                "encrypted_literal": r"\u0001",
                "decoded": "",
                "status": "decode_error",
                "error": "boom",
                "evidence": {"reason": "test"},
            },
        ]
        output = Path(".artifacts/test-output")
        json_path = output / "string-map.json"
        csv_path = output / "string-map.csv"
        unresolved_path = output / "unresolved.json"

        normalized = export_records(
            records,
            json_path,
            csv_path,
            unresolved_path,
        )

        self.assertEqual([row["id"] for row in normalized], ["sm-000001", "sm-000002"])
        json_rows = json.loads(json_path.read_text(encoding="utf-8"))
        with csv_path.open(encoding="utf-8-sig", newline="") as stream:
            csv_rows = list(csv.DictReader(stream))
        unresolved = json.loads(unresolved_path.read_text(encoding="utf-8"))
        self.assertEqual(len(json_rows), len(csv_rows))
        self.assertEqual(csv_rows[1]["decoded"], '中文,\n"quoted"')
        self.assertEqual([row["status"] for row in unresolved], ["decode_error"])


if __name__ == "__main__":
    unittest.main()
