import unittest
from pathlib import Path

from tools.caller_resolution import resolve_registered_caller
from tools.decoder_spec_discovery import discover_decoder_specs
from tools.java_source_scan import java_unescape
from tools.string_decoder_registry import register_decoder_specs


class CallerResolutionTests(unittest.TestCase):
    def test_recovers_obfuscated_lambda_caller_from_classfile_methods(self):
        source_root = Path(".artifacts/decompiled/cfr-app-20260620-0215")
        register_decoder_specs(discover_decoder_specs(source_root))
        result = resolve_registered_caller(
            Path(".artifacts/working/m1-02/App.jar"),
            "com.sbf.main.jxbrowser.a$1",
            "run",
            "a$MiJava$60.B",
            java_unescape(
                r"\u8e8c\uea6e\u48cc\ua771\ub30b\u0c8d\u4fe5\u3481\u5b2e\u2225\u4376\u9243"
            ),
        )
        self.assertTrue(result.resolved)
        self.assertEqual(result.caller, "com.sbf.main.jxbrowser.a$1a")
        self.assertEqual(result.decoded, "access_token")


if __name__ == "__main__":
    unittest.main()
