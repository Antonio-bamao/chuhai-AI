import unittest
from pathlib import Path

from tools.classfile_methods import read_class_method_names


class ClassfileMethodsTests(unittest.TestCase):
    def test_reads_obfuscated_synthetic_method_names(self):
        methods = read_class_method_names(
            Path(".artifacts/working/m1-02/App.jar"),
            "com.sbf.main.jxbrowser.a$1",
        )
        self.assertIn("<init>", methods)
        self.assertIn("run", methods)
        self.assertIn("a", methods)
        self.assertIn("Eo", methods)


if __name__ == "__main__":
    unittest.main()
