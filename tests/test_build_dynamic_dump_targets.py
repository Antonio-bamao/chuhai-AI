import unittest

from tools.build_dynamic_dump_targets import build_targets


class BuildDynamicDumpTargetsTests(unittest.TestCase):
    def test_selects_only_dynamic_or_error_and_groups_by_family(self):
        inventory = [
            {
                "family": "StartDecoder.x",
                "definitions": [{"path": "StartDecoder.java", "line": 10}],
            },
            {
                "family": "Other.y",
                "definitions": [{"path": "Other.java", "line": 20}],
            },
        ]
        records = [
            {
                "id": "sm-1",
                "decoder": "StartDecoder.x",
                "status": "dynamic_dump_required",
                "path": "com/sbf/main/StartApp.java",
                "line": 100,
                "caller": "com.sbf.main.StartAppf",
                "encrypted_literal": r"\u0001",
                "evidence": {"reason": "caller ambiguous"},
            },
            {
                "id": "sm-2",
                "decoder": "StartDecoder.x",
                "status": "decode_error",
                "path": "com/sbf/main/StartApp.java",
                "line": 101,
                "caller": "com.sbf.main.StartAppf",
                "encrypted_literal": r"\u0002",
                "evidence": {"reason": "decode failed"},
            },
            {
                "id": "sm-3",
                "decoder": "Other.y",
                "status": "decoded_static",
                "path": "Other.java",
                "line": 30,
                "caller": "Other.run",
                "encrypted_literal": r"\u0003",
                "evidence": {},
            },
        ]

        targets = build_targets(inventory, records)

        self.assertEqual(len(targets), 1)
        target = targets[0]
        self.assertEqual(target["family"], "StartDecoder.x")
        self.assertEqual(target["call_count"], 2)
        self.assertEqual(target["priority"], "critical")
        self.assertEqual(target["owner_class"], "StartDecoder")
        self.assertEqual(target["method_name"], "x")
        self.assertEqual(len(target["sample_calls"]), 2)


if __name__ == "__main__":
    unittest.main()
