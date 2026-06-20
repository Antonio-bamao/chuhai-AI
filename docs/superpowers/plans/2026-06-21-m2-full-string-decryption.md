# M2 Full String Decryption Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Classify every encrypted-string candidate, expand reliable static decoding, export JSON/CSV, generate a separate annotated source tree, produce a safe reading-only deobfuscation result, and prepare a screenshot-driven offline dynamic-dump handoff.

**Architecture:** A read-only scanner creates a complete candidate inventory. A registry-backed decoder converts only verified algorithm families, while every remaining family receives an explicit status. Separate exporters create JSON/CSV and annotated sources. Deobfuscation operates only on copies; Threadtear executions capable of loading target classes are prohibited on the host and deferred to the offline Windows VM.

**Tech Stack:** Python 3 standard library (`unittest`, `csv`, `json`, `pathlib`, `hashlib`, `zipfile`), CFR 0.152, Threadtear 3.0.1 or a verified equivalent, Git.

---

## Safety gates

1. Never launch `HuoChaiAI.exe`, `StartApp`, or the complete application on the host.
2. Never overwrite `.artifacts/working/m1-02/App.jar`.
3. Never modify `.artifacts/decompiled/cfr-app-20260620-0215`.
4. Threadtear documents that some executions load and invoke target classes. Any execution using Threadtear's VM/reflection facilities must run only in the offline Windows VM.
5. Host-side deobfuscation is limited to bytecode-only generic cleanup verified not to load target classes.
6. Every phase ends with tests, artifact checks, context validation, and a dedicated commit.

## File map

| Path | Responsibility |
| --- | --- |
| `tools/java_source_scan.py` | Shared Java source parsing, literal unescaping, caller inference |
| `tools/string_decoder_core.py` | Integer and AES-table decoding primitives |
| `tools/string_decoder_registry.py` | Verified decoder configurations and call patterns |
| `tools/inventory_string_decoders.py` | Candidate-family inventory and static classification |
| `tools/decode_java_strings.py` | Unified decoding orchestration |
| `tools/export_string_map.py` | Stable IDs, JSON/CSV and unresolved exports |
| `tools/annotate_java_strings.py` | Separate annotated CFR source tree |
| `tools/hash_tree.py` | Deterministic source-tree integrity manifest |
| `tests/fixtures/java_sources/` | Minimal Java source fixtures |
| `tests/test_java_source_scan.py` | Scanner behavior |
| `tests/test_string_decoder_core.py` | Existing decoder regression samples |
| `tests/test_inventory_string_decoders.py` | Candidate classification |
| `tests/test_export_string_map.py` | JSON/CSV consistency |
| `tests/test_annotate_java_strings.py` | Safe annotation behavior |
| `tests/test_hash_tree.py` | Tree integrity |
| `.context/string-decoder-inventory.md` | Human-readable inventory summary |
| `.context/deobfuscation-report.md` | Reading-JAR process and comparison |
| `.context/dynamic-dump-runbook.md` | Manual offline VM procedure |

---

### Task 1: Freeze the baseline and create the test harness

**Files:**
- Create: `tools/hash_tree.py`
- Create: `tests/test_hash_tree.py`
- Create: `tests/__init__.py`
- Create: `.context/m2-full-baseline.md`
- Generate: `.artifacts/analysis/source-tree-baseline.json`

- [ ] **Step 1: Write the failing integrity test**

```python
# tests/test_hash_tree.py
import tempfile
import unittest
from pathlib import Path

from tools.hash_tree import hash_tree


class HashTreeTests(unittest.TestCase):
    def test_hash_tree_is_stable_and_path_sensitive(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / "b.txt").write_text("two", encoding="utf-8")
            (root / "a.txt").write_text("one", encoding="utf-8")
            first = hash_tree(root)
            second = hash_tree(root)
            self.assertEqual(first, second)
            self.assertEqual([row["path"] for row in first], ["a.txt", "b.txt"])
            self.assertEqual(first[0]["size"], 3)


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
python -m unittest tests.test_hash_tree -v
```

Expected: import failure for `tools.hash_tree`.

- [ ] **Step 3: Implement deterministic tree hashing**

```python
# tools/hash_tree.py
from __future__ import annotations

import hashlib
from pathlib import Path


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest().upper()


def hash_tree(root: Path) -> list[dict]:
    root = root.resolve()
    rows = []
    for path in sorted(p for p in root.rglob("*") if p.is_file()):
        rows.append(
            {
                "path": path.relative_to(root).as_posix(),
                "size": path.stat().st_size,
                "sha256": sha256_file(path),
            }
        )
    return rows
```

- [ ] **Step 4: Verify GREEN and record the real baseline**

Run:

```powershell
python -m unittest tests.test_hash_tree -v
python -c "import json; from pathlib import Path; from tools.hash_tree import hash_tree,sha256_file; root=Path('.artifacts/decompiled/cfr-app-20260620-0215'); rows=hash_tree(root); Path('.artifacts/analysis/source-tree-baseline.json').write_text(json.dumps(rows,indent=2),encoding='utf-8'); print(len(rows)); print(sha256_file(Path('.artifacts/working/m1-02/App.jar')))"
```

Expected:

- test passes;
- tree contains 4,227 total files and 4,226 Java files;
- JAR hash remains `9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B`.

- [ ] **Step 5: Write `.context/m2-full-baseline.md` with exact counts and hashes**

Include the JAR hash, source-tree counts, existing map counts, Git commit, and the rule that these inputs are immutable.

- [ ] **Step 6: Commit**

```powershell
git add tools/hash_tree.py tests/test_hash_tree.py tests/__init__.py .context/m2-full-baseline.md
git commit -m "test: freeze m2 decryption baseline"
```

---

### Task 2: Extract shared Java source scanning

**Files:**
- Create: `tools/java_source_scan.py`
- Create: `tests/test_java_source_scan.py`
- Create: `tests/fixtures/java_sources/com/example/Sample.java`
- Modify: `tools/decode_java_strings.py`

- [ ] **Step 1: Create a fixture with constructors, overloads, and multiple calls**

```java
// tests/fixtures/java_sources/com/example/Sample.java
package com.example;

public class Sample {
    static String a = Decoder.x("\u0001");

    public Sample() {
        Decoder.x("\u0002");
    }

    public String run(String value) {
        return Decoder.x("\u0003") + Decoder.x("\u0004");
    }
}
```

- [ ] **Step 2: Write failing scanner tests**

```python
# tests/test_java_source_scan.py
import unittest
from pathlib import Path

from tools.java_source_scan import iter_java_lines, java_unescape


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


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 3: Run and verify RED**

```powershell
python -m unittest tests.test_java_source_scan -v
```

Expected: import failure for `tools.java_source_scan`.

- [ ] **Step 4: Implement the shared scanner**

Implement:

```python
@dataclass(frozen=True)
class JavaLine:
    path: Path
    line_no: int
    text: str
    class_name: str
    method_name: str
    signature: str

    @property
    def caller(self) -> str:
        return f"{self.class_name}{self.method_name}"
```

Move `java_unescape`, package parsing, method parsing, class-name inference, and brace-depth tracking from both existing scripts into this module. `iter_java_lines(source_root, java_file)` must yield one `JavaLine` per source line.

- [ ] **Step 5: Refactor existing decoders to import the shared scanner**

`decode_java_strings.py` and `decode_bootstrap_calls.py` must import the common functions without changing output semantics.

- [ ] **Step 6: Verify regression**

```powershell
python -m unittest tests.test_java_source_scan -v
python -m py_compile tools\java_source_scan.py tools\decode_java_strings.py tools\decode_bootstrap_calls.py
python tools\decode_java_strings.py --source-root .artifacts\decompiled\cfr-app-20260620-0215 --out .artifacts\analysis\string_map.refactor-check.json
python -c "import json; from pathlib import Path; a=json.loads(Path('.artifacts/analysis/string_map.json').read_text()); b=json.loads(Path('.artifacts/analysis/string_map.refactor-check.json').read_text()); assert a==b; print(len(b))"
```

Expected: 4,599 identical records.

- [ ] **Step 7: Commit**

```powershell
git add tools/java_source_scan.py tools/decode_java_strings.py tools/decode_bootstrap_calls.py tests/test_java_source_scan.py tests/fixtures/java_sources/com/example/Sample.java
git commit -m "refactor: share java source scanner"
```

---

### Task 3: Separate decoder algorithm and registry

**Files:**
- Create: `tools/string_decoder_core.py`
- Create: `tools/string_decoder_registry.py`
- Create: `tests/test_string_decoder_core.py`
- Modify: `tools/decode_java_strings.py`

- [ ] **Step 1: Write regression tests for verified samples**

Use exact existing samples:

```python
SAMPLES = [
    ("JSetupDialog$JLoginNew.N", "com.sbf.main.StartAppf",
     r"\ueca0\u309c\u751c\u33ee\u2473", "token"),
    ("JSetupDialog$JLoginNew.N", "com.sbf.main.StartAppf",
     r"\uecb1\u308b\u7507\u33e2\u246f\uded4\uc82e\u80e3\u0573\ue160", "expireTime"),
    ("JLoginHTML$h.v", "com.sbf.main.ext.j2026.ClawWorkspace<init>",
     r"\uaae8\u02d5\u4a12\ue864\u241c\uf7de\ufa70\u4318\u8f76\u51a2\u9b6d\u45cd\u9fb1", "ClawWorkspace"),
]
```

Assert `decode_registered(name, caller, java_unescape(literal)) == expected`.

- [ ] **Step 2: Verify RED**

```powershell
python -m unittest tests.test_string_decoder_core -v
```

Expected: import failure for the new modules.

- [ ] **Step 3: Move primitives and configurations**

`string_decoder_core.py` owns `i32`, shifts, Java hash, AES-table construction, and `decode_string`.

`string_decoder_registry.py` defines:

```python
@dataclass(frozen=True)
class DecoderSpec:
    name: str
    call_pattern: re.Pattern[str]
    seed_long: int
    suffix_bytes: tuple[int, ...]
    constants: tuple[int, int, int, int]


DECODER_SPECS: dict[str, DecoderSpec] = {}


def decode_registered(name: str, caller: str, encrypted: str) -> str:
    spec = DECODER_SPECS[name]
    decoder = build_decoder(
        spec.seed_long,
        list(spec.suffix_bytes),
        list(spec.constants),
    )
    return decode_string(encrypted, caller, decoder)
```

Populate `DECODER_SPECS` by mechanically moving all nine entries from the existing `DECODERS` and `CALL_PATTERNS` dictionaries. Preserve every integer and regular expression byte-for-byte; the full-map equality check in Step 4 is the acceptance test for the transfer.

- [ ] **Step 4: Verify GREEN and full regression**

```powershell
python -m unittest tests.test_string_decoder_core -v
python tools\decode_java_strings.py --source-root .artifacts\decompiled\cfr-app-20260620-0215 --out .artifacts\analysis\string_map.registry-check.json
python -c "import json; from pathlib import Path; a=json.loads(Path('.artifacts/analysis/string_map.json').read_text()); b=json.loads(Path('.artifacts/analysis/string_map.registry-check.json').read_text()); assert a==b; print('4599 identical')"
```

- [ ] **Step 5: Commit**

```powershell
git add tools/string_decoder_core.py tools/string_decoder_registry.py tools/decode_java_strings.py tests/test_string_decoder_core.py
git commit -m "refactor: separate string decoder registry"
```

---

### Task 4: Build the complete candidate inventory

**Files:**
- Create: `tools/inventory_string_decoders.py`
- Create: `tests/test_inventory_string_decoders.py`
- Create: `tests/fixtures/java_sources/com/example/Decoder.java`
- Create: `.context/string-decoder-inventory.md`

- [ ] **Step 1: Add a decoder-definition fixture**

The fixture must include:

- one static `String x(String)` method with `toCharArray`, XOR, and caller-name access;
- one ordinary `void log(String)` method;
- one unresolved call whose definition is absent.

- [ ] **Step 2: Write failing inventory tests**

Assert:

```python
inventory = build_inventory(fixture_root, known_decoders={"Decoder.x"})
self.assertEqual(inventory["Decoder.x"].call_count, 4)
self.assertEqual(inventory["Decoder.x"].status, "decoded_static")
self.assertEqual(inventory["Logger.log"].status, "not_string_decoder")
self.assertEqual(inventory["Missing.z"].status, "unsupported_shape")
```

Also assert that every discovered family has exactly one allowed status.

- [ ] **Step 3: Verify RED**

```powershell
python -m unittest tests.test_inventory_string_decoders -v
```

- [ ] **Step 4: Implement inventory scanning and evidence**

The scanner must:

1. Match one-argument string-literal calls containing at least one `\uXXXX`.
2. Count by fully qualified textual call family.
3. Locate likely method definitions by owner simple name and method name.
4. Capture up to five call samples and method-definition locations.
5. Detect features:

```text
returns_string
static_method
uses_to_char_array
uses_xor_or_shift
uses_base64
uses_stack_trace
uses_class_name
uses_system_property
uses_large_switch
definition_missing
known_decoder
```

6. Assign only conservative statuses:
   - known registry member → `decoded_static`
   - definition proves non-String return → `not_string_decoder`
   - definition missing or ambiguous → `unsupported_shape`
   - runtime-context feature without registry support → `dynamic_dump_required`
   - otherwise → `unsupported_shape`

- [ ] **Step 5: Generate real inventory**

```powershell
python tools\inventory_string_decoders.py --source-root .artifacts\decompiled\cfr-app-20260620-0215 --json .artifacts\analysis\string_decoder_inventory.json --csv .artifacts\analysis\string_decoder_inventory.csv --markdown .context\string-decoder-inventory.md
```

Expected baseline: approximately 348 families and 35,642 calls. Exact values become authoritative after the tested scanner runs.

- [ ] **Step 6: Verify all families have status**

```powershell
python -c "import json; from pathlib import Path; rows=json.loads(Path('.artifacts/analysis/string_decoder_inventory.json').read_text()); allowed={'decoded_static','decoded_existing_plaintext','not_string_decoder','unsupported_shape','dynamic_dump_required','decode_error'}; assert rows and all(r['status'] in allowed for r in rows); assert len({r['family'] for r in rows})==len(rows); print(len(rows),sum(r['call_count'] for r in rows))"
```

- [ ] **Step 7: Commit**

```powershell
git add tools/inventory_string_decoders.py tests/test_inventory_string_decoders.py tests/fixtures/java_sources/com/example/Decoder.java .context/string-decoder-inventory.md
git commit -m "feat: inventory encrypted string families"
```

---

### Task 5: Expand verified static decoder families in controlled batches

**Files:**
- Modify: `tools/string_decoder_registry.py`
- Modify: `tests/test_string_decoder_core.py`
- Modify: `.context/string-decoder-inventory.md`
- Create: `.context/string-decoder-batch-log.md`

- [ ] **Step 1: Select the first batch deterministically**

From the inventory choose up to ten families satisfying:

1. status is `unsupported_shape` or `dynamic_dump_required`;
2. return type is String;
3. method body matches the verified AES-table structure;
4. highest call count first;
5. prioritize files under `com/sbf/main`, `com/sbf/util/http`, and `ext/j2026`.

Record family, call count, definition, algorithm fingerprint, and three source samples in the batch log before changing the registry.

- [ ] **Step 2: Write one failing sample test per selected family**

For each family, use one real encrypted literal and caller. The expected plaintext must be established by reproducing the method's constants and checking at least two neighboring source usages for semantic consistency.

Test shape, using each selected family's literal values from the batch log:

```python
def assert_verified_family_sample(self, family, caller, literal, expected):
    self.assertEqual(
        decode_registered(family, caller, java_unescape(literal)),
        expected,
    )
```

- [ ] **Step 3: Run the batch tests and verify RED**

```powershell
python -m unittest tests.test_string_decoder_core -v
```

Expected: unknown registry entry for every newly selected family.

- [ ] **Step 4: Add only verified configurations**

Add each family to `DECODER_SPECS`. If the method body is not structurally identical to supported core algorithms, do not force it into the registry; update its inventory status to `dynamic_dump_required` with evidence.

- [ ] **Step 5: Verify GREEN and regenerate inventory**

```powershell
python -m unittest tests.test_string_decoder_core -v
python tools\inventory_string_decoders.py --source-root .artifacts\decompiled\cfr-app-20260620-0215 --json .artifacts\analysis\string_decoder_inventory.json --csv .artifacts\analysis\string_decoder_inventory.csv --markdown .context\string-decoder-inventory.md
```

- [ ] **Step 6: Repeat batches until no statically verifiable family remains**

Stop only when every remaining unresolved family has evidence for `not_string_decoder`, `unsupported_shape`, or `dynamic_dump_required`. Each batch receives a dated section in `.context/string-decoder-batch-log.md`.

- [ ] **Step 7: Commit each batch separately**

```powershell
git add tools/string_decoder_registry.py tests/test_string_decoder_core.py .context/string-decoder-inventory.md .context/string-decoder-batch-log.md
$batch=(Select-String -Path '.context\string-decoder-batch-log.md' -Pattern '^## Batch ').Count
git commit -m "feat: decode string family batch $batch"
```

---

### Task 6: Produce unified JSON, CSV, and unresolved outputs

**Files:**
- Create: `tools/export_string_map.py`
- Create: `tests/test_export_string_map.py`
- Modify: `tools/decode_java_strings.py`

- [ ] **Step 1: Write failing export tests**

Test records containing commas, quotes, newlines, Chinese text, empty decoded values, and errors. Assert:

- IDs are deterministic `sm-000001`, `sm-000002`;
- JSON and CSV have the same row count;
- CSV round-trips multiline strings;
- unresolved output contains only non-decoded statuses.

- [ ] **Step 2: Verify RED**

```powershell
python -m unittest tests.test_export_string_map -v
```

- [ ] **Step 3: Implement normalization and export**

Define:

```python
FIELDS = [
    "id", "decoder", "path", "line", "caller", "encrypted_literal",
    "decoded", "status", "error", "evidence",
]
```

Sort by normalized path, line, decoder, encrypted literal before assigning IDs. Export JSON with UTF-8 and `ensure_ascii=False`; export CSV with `newline=""` and `csv.DictWriter`.

- [ ] **Step 4: Integrate all candidate calls**

`decode_java_strings.py` must emit:

- decoded call records for registry families;
- unresolved call records for inventory families not decoded;
- explicit status and evidence on every record.

- [ ] **Step 5: Generate final static outputs**

```powershell
python tools\decode_java_strings.py --source-root .artifacts\decompiled\cfr-app-20260620-0215 --inventory .artifacts\analysis\string_decoder_inventory.json --out .artifacts\analysis\string_map.raw.json
python tools\export_string_map.py --input .artifacts\analysis\string_map.raw.json --json .artifacts\analysis\string_map.json --csv .artifacts\analysis\string_map.csv --unresolved .artifacts\analysis\unresolved_string_calls.json
```

- [ ] **Step 6: Verify consistency**

```powershell
python -c "import csv,json; from pathlib import Path; j=json.loads(Path('.artifacts/analysis/string_map.json').read_text(encoding='utf-8')); c=list(csv.DictReader(Path('.artifacts/analysis/string_map.csv').open(encoding='utf-8',newline=''))); u=json.loads(Path('.artifacts/analysis/unresolved_string_calls.json').read_text(encoding='utf-8')); assert len(j)==len(c); assert all(r['status'] not in {'decoded_static','decoded_existing_plaintext'} for r in u); print(len(j),len(u))"
```

- [ ] **Step 7: Commit**

```powershell
git add tools/export_string_map.py tools/decode_java_strings.py tests/test_export_string_map.py
git commit -m "feat: export complete string maps"
```

---

### Task 7: Generate the separate annotated source tree

**Files:**
- Create: `tools/annotate_java_strings.py`
- Create: `tests/test_annotate_java_strings.py`

- [ ] **Step 1: Write failing annotation tests**

Cover:

- one decoded call;
- two calls on one line;
- plaintext containing `*/`;
- plaintext containing CR/LF and tabs;
- unresolved status;
- idempotent regeneration into an empty output directory.

Expected comments:

```java
/* STRING_MAP sm-000001: "token" */
/* STRING_MAP sm-000002: dynamic_dump_required */
```

- [ ] **Step 2: Verify RED**

```powershell
python -m unittest tests.test_annotate_java_strings -v
```

- [ ] **Step 3: Implement safe annotation**

Rules:

1. Copy all files and directories from source to destination.
2. Match records by normalized relative path and line.
3. Escape backslash, CR, LF, tab, `*/`, and non-printable controls.
4. Append one comment per mapping record in stable ID order.
5. Never mutate the source tree.
6. Refuse a destination inside the source directory.

- [ ] **Step 4: Generate the real annotated tree**

```powershell
python tools\annotate_java_strings.py --source-root .artifacts\decompiled\cfr-app-20260620-0215 --map .artifacts\analysis\string_map.json --out .artifacts\decompiled\cfr-app-20260620-0215-annotated
```

- [ ] **Step 5: Verify counts, comments, and source integrity**

```powershell
python -c "from pathlib import Path; src=Path('.artifacts/decompiled/cfr-app-20260620-0215'); out=Path('.artifacts/decompiled/cfr-app-20260620-0215-annotated'); assert len(list(src.rglob('*.java')))==4226; assert len(list(out.rglob('*.java')))==4226; comments=sum(p.read_text(encoding='utf-8',errors='replace').count('STRING_MAP sm-') for p in out.rglob('*.java')); print(comments)"
python -c "from pathlib import Path; from tools.hash_tree import hash_tree; import json; before=json.loads(Path('.artifacts/analysis/source-tree-baseline.json').read_text()) if Path('.artifacts/analysis/source-tree-baseline.json').exists() else hash_tree(Path('.artifacts/decompiled/cfr-app-20260620-0215')); after=hash_tree(Path('.artifacts/decompiled/cfr-app-20260620-0215')); assert before==after; print('source unchanged')"
```

- [ ] **Step 6: Commit**

```powershell
git add tools/annotate_java_strings.py tests/test_annotate_java_strings.py
git commit -m "feat: generate annotated java sources"
```

---

### Task 8: Perform and document the 20-record semantic sample

**Files:**
- Create: `.context/string-map-sample-verification.md`
- Modify: `.context/string-decode-report.md`
- Modify: `.context/verify.log`

- [ ] **Step 1: Select deterministic samples**

Use the lowest stable mapping IDs satisfying:

- five decoded strings containing `http://`, `https://`, `/api/`, or URL path fragments;
- five JSON/header fields;
- five Chinese or English UI messages;
- five records under `StartApp`, `JLogin*`, `JProductSelector*`, or `ClawWorkspace`.

- [ ] **Step 2: Record evidence**

For each sample include:

```text
mapping ID
decoder
source file and line
caller
encrypted literal
decoded value
one-sentence context assessment
verdict: coherent / rejected
```

Rejected records must be changed to `decode_error` and removed from decoded acceptance counts.

- [ ] **Step 3: Verify all 20 are coherent**

Use a script assertion over a small JSON block embedded or referenced by the report; expected coherent count is 20 and rejected count is 0 after corrections.

- [ ] **Step 4: Commit**

```powershell
git add .context/string-map-sample-verification.md .context/string-decode-report.md .context/verify.log
git commit -m "docs: verify full string map samples"
```

---

### Task 9: Threadtear preflight and safe host-side deobfuscation

**Files:**
- Create: `tools/deobfuscation_preflight.py`
- Create: `tests/test_deobfuscation_preflight.py`
- Create: `.context/deobfuscation-report.md`

- [ ] **Step 1: Write failing preflight tests**

The preflight must reject an execution manifest containing any of:

```text
me.nov.threadtear.asm.vm.VM
ClassLoader
loadClass
java.lang.reflect
Method.invoke
```

It must accept a manifest explicitly marked `bytecode_only: true` containing only cleanup execution names.

- [ ] **Step 2: Verify RED**

```powershell
python -m unittest tests.test_deobfuscation_preflight -v
```

- [ ] **Step 3: Implement preflight**

The script reads a JSON manifest:

```json
{
  "tool": "threadtear",
  "version": "3.0.1",
  "bytecode_only": true,
  "executions": ["Remove NOPs", "Remove unreachable code"]
}
```

It exits nonzero if `bytecode_only` is false or forbidden runtime-loading features are detected in supplied class lists/source strings.

- [ ] **Step 4: Acquire Threadtear only after approval**

Use the official repository/release:

- repository: `https://github.com/loerting/threadtear`
- latest published release shown by the official repository: `3.0.1` dated October 15, 2020.

Record URL, file name, SHA256, size, and download date. Do not run it until the preflight and input copy checks pass.

- [ ] **Step 5: Create immutable copies and run bytecode-only cleanup**

Inputs/outputs:

```text
.artifacts/working/m2-deobfuscation/input-App.jar
.artifacts/deobfuscated/threadtear-bytecode-only.jar
```

Only generic cleanup executions verified not to load classes may run on the host. Any Stringer/ZKM/string execution using Threadtear VM is deferred to Task 11.

- [ ] **Step 6: Re-decompile with CFR**

```powershell
java -jar .artifacts\tools\cfr-0.152.jar .artifacts\deobfuscated\threadtear-bytecode-only.jar --outputdir .artifacts\decompiled\cfr-app-deobfuscated
```

- [ ] **Step 7: Verify structure and compare readability**

Record:

- output JAR opens with `zipfile`;
- `com/sbf/main/StartApp.class` exists;
- CFR exit code;
- Java file count;
- bootstrap call count before/after;
- selected method line counts and CFR warnings before/after.

- [ ] **Step 8: Commit**

```powershell
git add tools/deobfuscation_preflight.py tests/test_deobfuscation_preflight.py .context/deobfuscation-report.md
git commit -m "docs: verify reading-only deobfuscation"
```

---

### Task 10: Generate dynamic-dump targets and the screenshot-driven runbook

**Files:**
- Create: `tools/build_dynamic_dump_targets.py`
- Create: `tests/test_build_dynamic_dump_targets.py`
- Create: `.context/dynamic-dump-runbook.md`

- [ ] **Step 1: Write failing target-selection tests**

Assert only records whose family status is `dynamic_dump_required` or `decode_error` are selected. Group by method family and include call count, definitions, sample callers, and desired hook signature.

- [ ] **Step 2: Verify RED**

```powershell
python -m unittest tests.test_build_dynamic_dump_targets -v
```

- [ ] **Step 3: Implement target generation**

Generate:

```text
.artifacts/analysis/dynamic_dump_targets.json
```

Each target contains:

```text
family
status
call_count
owner_class
method_name
descriptor_or_inferred_signature
definition_locations
sample_calls
reason
priority
```

Priority is `critical` for startup/login/auth paths, `high` for HTTP/token/payment, and `normal` otherwise.

- [ ] **Step 4: Write the manual runbook**

The runbook is a strict stop-and-screenshot sequence:

1. Create Windows 10 x64 VM.
2. Set network adapter to “Not attached”.
3. Disable clipboard, drag/drop, shared folders, USB auto-capture.
4. Boot and show `ipconfig /all`.
5. Show VirtualBox network settings screenshot.
6. Create clean snapshot.
7. Attach a read-only ISO containing App.jar, agent, JRE, target list.
8. Reconfirm network.
9. Run only the agent command supplied for selected targets.
10. Screenshot console and dump directory.
11. Export dump through a newly created one-time read-only medium.
12. Power off and restore snapshot.

Every step says “stop and send screenshot” before proceeding.

- [ ] **Step 5: Verify generated target JSON**

```powershell
python tools\build_dynamic_dump_targets.py --inventory .artifacts\analysis\string_decoder_inventory.json --out .artifacts\analysis\dynamic_dump_targets.json
python -c "import json; from pathlib import Path; rows=json.loads(Path('.artifacts/analysis/dynamic_dump_targets.json').read_text()); assert all(r['status'] in {'dynamic_dump_required','decode_error'} for r in rows); print(len(rows))"
```

- [ ] **Step 6: Commit**

```powershell
git add tools/build_dynamic_dump_targets.py tests/test_build_dynamic_dump_targets.py .context/dynamic-dump-runbook.md
git commit -m "docs: prepare offline dynamic string dump"
```

---

### Task 11: Final verification and M2 status decision

**Files:**
- Modify: `.context/string-decode-report.md`
- Modify: `.context/current-status.md`
- Modify: `.context/verify.log`
- Modify: `.context/work-log.md`

- [ ] **Step 1: Run the complete automated suite**

```powershell
python -m unittest discover -s tests -v
python -m compileall -q tools
```

Expected: all tests pass and every tool compiles.

- [ ] **Step 2: Run artifact assertions**

Verify:

- inventory families all have allowed statuses;
- JSON/CSV record counts match;
- unresolved records have unresolved statuses;
- annotated tree has 4,226 Java files;
- at least one `STRING_MAP sm-` comment exists for every decoded record;
- original JAR hash matches baseline;
- original source-tree hash manifest matches baseline;
- reading-only JAR opens and key classes exist;
- deobfuscated CFR tree exists;
- dynamic target list and runbook exist.

- [ ] **Step 3: Run context and Git checks**

```powershell
python C:\Users\m1591\.codex\skills\project-context-os\scripts\validate_context.py --project-root H:\项目\出海-AI
git diff --check
git status --short
```

- [ ] **Step 4: Decide status using explicit rule**

- If `dynamic_dump_targets.json` is empty: mark external Phase 3 complete.
- If it contains only non-critical/non-high targets and the authorization/startup paths are statically decoded: mark static Phase 3 complete, dynamic appendix pending.
- If any critical/high target remains: keep M2 active and start screenshot-driven VM procedure with the user.

- [ ] **Step 5: Commit final static checkpoint**

```powershell
git add .context/string-decode-report.md .context/current-status.md .context/verify.log .context/work-log.md
git commit -m "docs: close full static m2 decryption"
```
