import unittest
from pathlib import Path

from tools.java_source_scan import (
    BOOTSTRAP_METHOD_DECL,
    iter_java_lines,
    java_unescape,
)


class JavaSourceScanTests(unittest.TestCase):
    def test_unescapes_java_literals(self):
        self.assertEqual(java_unescape(r"\u4f60\u597d\n"), "你好\n")

    def test_tracks_callers_and_multiple_calls(self):
        root = Path("tests/fixtures/java_sources")
        rows = list(iter_java_lines(root, root / "com/example/Sample.java"))
        calls = [row for row in rows if 'Decoder.x("' in row.text]
        self.assertEqual(calls[0].caller, "com.example.Sample<clinit>")
        self.assertEqual(calls[1].caller, "com.example.Sample<init>")
        self.assertEqual(calls[2].caller, "com.example.Samplerun")

    def test_supports_legacy_bootstrap_method_detection(self):
        root = Path("tests/fixtures/java_sources")
        rows = list(
            iter_java_lines(
                root,
                root / "com/example/Sample.java",
                BOOTSTRAP_METHOD_DECL,
            )
        )
        calls = [row for row in rows if 'Decoder.x("' in row.text]
        self.assertEqual(calls[1].caller, "com.example.Sample<clinit>")
        self.assertEqual(calls[2].caller, "com.example.Samplerun")


if __name__ == "__main__":
    unittest.main()
