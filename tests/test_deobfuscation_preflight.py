import unittest

from tools.deobfuscation_preflight import validate_manifest


class DeobfuscationPreflightTests(unittest.TestCase):
    def test_accepts_explicit_bytecode_only_cleanup(self):
        errors = validate_manifest(
            {
                "tool": "threadtear",
                "version": "3.0.1",
                "bytecode_only": True,
                "executions": ["Remove NOPs", "Remove unreachable code"],
                "inspection_text": "",
            }
        )
        self.assertEqual(errors, [])

    def test_rejects_runtime_loading_features(self):
        forbidden = [
            "me.nov.threadtear.asm.vm.VM",
            "ClassLoader",
            "loadClass",
            "java.lang.reflect",
            "Method.invoke",
        ]
        for marker in forbidden:
            with self.subTest(marker=marker):
                errors = validate_manifest(
                    {
                        "tool": "threadtear",
                        "version": "3.0.1",
                        "bytecode_only": True,
                        "executions": ["cleanup"],
                        "inspection_text": marker,
                    }
                )
                self.assertTrue(errors)

    def test_rejects_manifest_not_marked_bytecode_only(self):
        errors = validate_manifest(
            {
                "tool": "threadtear",
                "version": "3.0.1",
                "bytecode_only": False,
                "executions": ["Remove NOPs"],
            }
        )
        self.assertTrue(errors)


if __name__ == "__main__":
    unittest.main()
