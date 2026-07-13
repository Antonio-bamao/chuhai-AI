# D-1 X Local Pages Implementation Plan

> **For agentic workers:** Execute inline in this session. The task has a single bounded candidate overlay; no live swap, merge, or PR is authorized.

**Goal:** Produce a candidate-only DLL in which every X submenu opens a distinct local, offline page with a disabled real-operation control and no fabricated records.

**Architecture:** The recovered X menu catalog assigns each C4133 submenu an explicit `/pc/local/x/<leaf>` route and a child `JSinglepage:/...` leaf. The existing JSinglepage normalizer preserves explicit paths; the existing local Web resource bridge recognizes the nine paths and returns separate static HTML surfaces. A D-1 overlay updates only the serialized PC menu class and the existing local bridge class in the C-67 candidate.

**Tech Stack:** Java 8-compatible source, ASM patcher, JxBrowser local response interception, Python `unittest` harness.

---

### Task 1: Pin the default-dispatch root cause with catalog tests

**Files:**

- Modify: `tests/test_m4_auth_patch.py`
- Modify: `tools/m4_auth_patch/M4RecoveryCatalog.java`

- [ ] Write a failing catalog probe asserting all nine X codes have distinct `/pc/local/x/` links and each has one explicit `JSinglepage:/pc/local/x/` leaf.
- [ ] Run the probe and record that the current catalog fails because most entries are `original(...)` and resolve to `/pc/aicloud/my`.
- [ ] Implement `xLocalRoute(...)` and route-child generation for the nine known X menus only.
- [ ] Re-run the catalog probe; it must pass without changing another platform's menu contract.

### Task 2: Serve separate local X surfaces and gate operations

**Files:**

- Modify: `tests/test_m4_auth_patch.py`
- Modify: `tools/m4_auth_patch/M5LocalSpiderBridge.java`

- [ ] Write a failing bridge probe for all nine X paths. It must require a unique `data-d1-x-route`, the expected title/button, offline copy, and a disabled `data-d1-action="guarded"` button; it must reject task/number/fingerprint sample data.
- [ ] Run the probe and record failure because no D-1 local pages exist.
- [ ] Implement the static page dispatcher before the existing C-6 page dispatcher. It must only return content for the nine exact local paths.
- [ ] Re-run the bridge probe; it must pass and preserve C-6 local pages.

### Task 3: Build a minimal D-1 overlay and prove its boundary

**Files:**

- Modify: `tests/test_m4_auth_patch.py`
- Modify: `tools/m4_auth_patch/M4AuthPatch.java`

- [ ] Write a failing test for `--d1-x-overlay` using `App.c67.candidate.dll`; assert only `SBFApi.class` and `M5LocalSpiderBridge.class` change and that output bytecode contains D-1 markers.
- [ ] Run the test and record that the option is unsupported.
- [ ] Implement the dedicated overlay writer and argument branch, reusing the existing menu replacement and generated bridge compilation path.
- [ ] Re-run the test, then the full unit suite.

### Task 4: Candidate evidence and bounded runtime regression

**Files:**

- Create: `.artifacts/working/d1-x-local-pages/App.d1.x.candidate.dll`
- Create: `.artifacts/working/d1-x-local-pages/evidence.md`
- Modify: `.context/work-log.md`
- Modify: `.context/current-status.md`

- [ ] Build the candidate from `App.c67.candidate.dll`, calculate SHA-256, and compare it to live `data/app/App.dll` without writing live.
- [ ] Run an isolated candidate harness with the existing request observer. Record the per-page local route, disabled-operation marker, and observed requests; verify the three named remote IPs receive zero traffic during the window.
- [ ] Verify the live SQLite counts for 848/858 before and after, and run the existing C-6 recharge/advertising and WhatsApp customer-service regression tests.
- [ ] Record evidence, append the structured work log, refresh current status, and validate `.context`.
