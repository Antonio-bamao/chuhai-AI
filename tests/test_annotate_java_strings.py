import unittest
from pathlib import Path

from tools.annotate_java_strings import annotate_tree
from tools.hash_tree import hash_tree


class AnnotateJavaStringsTests(unittest.TestCase):
    def test_annotates_decoded_and_unresolved_calls_safely(self):
        source = Path("tests/fixtures/java_sources")
        output = Path(".artifacts/test-output/annotated-java-sources")
        records = [
            {
                "id": "sm-000001",
                "path": "com/example/Sample.java",
                "line": 4,
                "decoded": "token",
                "status": "decoded_static",
            },
            {
                "id": "sm-000002",
                "path": "com/example/Sample.java",
                "line": 13,
                "decoded": "a*/b\r\n\t",
                "status": "decoded_static",
            },
            {
                "id": "sm-000003",
                "path": "com/example/Sample.java",
                "line": 13,
                "decoded": "",
                "status": "dynamic_dump_required",
            },
        ]
        before = hash_tree(source)

        annotate_tree(source, records, output)
        annotate_tree(source, records, output)

        annotated = (output / "com/example/Sample.java").read_text(encoding="utf-8")
        self.assertIn('/* STRING_MAP sm-000001: "token" */', annotated)
        self.assertIn(r'/* STRING_MAP sm-000002: "a*\/b\r\n\t" */', annotated)
        self.assertIn(
            "/* STRING_MAP sm-000003: dynamic_dump_required */",
            annotated,
        )
        self.assertEqual(annotated.count("STRING_MAP sm-000002"), 1)
        self.assertEqual(before, hash_tree(source))


if __name__ == "__main__":
    unittest.main()
