import unittest
from pathlib import Path

from tools.inventory_string_decoders import ALLOWED_STATUSES, build_inventory


class InventoryStringDecodersTests(unittest.TestCase):
    def test_classifies_fixture_families_conservatively(self):
        fixture_root = Path("tests/fixtures/java_sources")
        inventory = build_inventory(
            fixture_root,
            known_decoders={"Decoder.x"},
        )

        self.assertEqual(inventory["Decoder.x"].call_count, 4)
        self.assertEqual(inventory["Decoder.x"].status, "decoded_static")
        self.assertEqual(inventory["Logger.log"].status, "not_string_decoder")
        self.assertEqual(inventory["Missing.z"].status, "unsupported_shape")
        self.assertTrue(inventory["Decoder.x"].features["uses_to_char_array"])
        self.assertTrue(inventory["Decoder.x"].features["uses_class_name"])

    def test_every_family_has_one_allowed_status(self):
        inventory = build_inventory(
            Path("tests/fixtures/java_sources"),
            known_decoders={"Decoder.x"},
        )
        self.assertTrue(inventory)
        for entry in inventory.values():
            self.assertIn(entry.status, ALLOWED_STATUSES)


if __name__ == "__main__":
    unittest.main()
