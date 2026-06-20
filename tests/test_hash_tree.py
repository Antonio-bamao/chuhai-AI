import unittest
from pathlib import Path

from tools.hash_tree import hash_tree


class HashTreeTests(unittest.TestCase):
    def test_hash_tree_is_stable_and_path_sensitive(self):
        root = Path("tests/fixtures/hash_tree")
        first = hash_tree(root)
        second = hash_tree(root)
        self.assertEqual(first, second)
        self.assertEqual([row["path"] for row in first], ["a.txt", "b.txt"])
        self.assertEqual(first[0]["size"], 4)


if __name__ == "__main__":
    unittest.main()
