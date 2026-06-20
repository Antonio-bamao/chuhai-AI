import unittest
from pathlib import Path

from tools.decoder_spec_discovery import discover_decoder_specs
from tools.string_decoder_registry import DECODER_SPECS


SOURCE_ROOT = Path(".artifacts/decompiled/cfr-app-20260620-0215")


class DecoderSpecDiscoveryTests(unittest.TestCase):
    def test_reproduces_registered_specs_and_excludes_ambiguous_families(self):
        discovered = discover_decoder_specs(SOURCE_ROOT)

        self.assertEqual(len(discovered), 347)
        self.assertNotIn("a$5$0.Z", discovered)
        self.assertIn("q.q", discovered)
        for name, expected in DECODER_SPECS.items():
            actual = discovered[name]
            self.assertEqual(actual.seed_long, expected.seed_long)
            self.assertEqual(actual.suffix_bytes, expected.suffix_bytes)
            self.assertEqual(actual.constants, expected.constants)
            self.assertEqual(
                actual.call_pattern.pattern,
                expected.call_pattern.pattern,
            )


if __name__ == "__main__":
    unittest.main()
