# D-2 Instagram Local Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a candidate-only Instagram rollout with nine distinct guarded local empty-state pages.

**Architecture:** `M4RecoveryCatalog` owns nine Instagram parent routes and explicit `JSinglepage:/pc/local/ins/<leaf>` child routes. `M5LocalSpiderBridge` owns static local pages. `M4AuthPatch --d2-ins-overlay` replaces only `SBFApi.class` and `M5LocalSpiderBridge.class` in a copy of the D-1 live baseline.

**Tech Stack:** Java 8, ASM patch compiler, Python `unittest`, JAR overlay, isolated Windows Java GUI.

---

### Task 1: Write failing Instagram contracts

**Files:**

- Modify: `tests/test_m4_auth_patch.py:2641-2656,4706-4847`
- Test: `tests/test_m4_auth_patch.py`

- [ ] Add `run_d2_ins_overlay(input_jar)` and a catalog probe that requires these exact pairs: `C4131_002/account-login`, `C4131_003/account-search`, `C4131_004/post-search`, `C4131_005/profile-mining`, `C4131_006/active-filter`, `C4131_007/api-broadcast`, `C4131_008/android-agent`, `C4131_009/aicloud-fingerprint`, `C4131_010/adspower-fingerprint`. For every pair assert: parent `localCode=JSinglepage`; parent `linkUrl=/pc/local/ins/<leaf>`; parent evidence starts `d2-ins-local:`; exactly one `treeEndFlg=1` child has localCode `/pc/local/ins/<leaf>`, linkUrl `JSinglepage:/pc/local/ins/<leaf>`, and child evidence `d2-ins-local-child:`; all routes unique; no `http`, `/pc/aicloud/my`, or `/es/bigData/bigDataTask` remains.

- [ ] Run `python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_d2_ins_catalog_uses_nine_distinct_explicit_local_leaves -v`; expect RED because present menus are generic or the existing blogger big-data route.

- [ ] Add a `M5LocalSpiderBridge` probe that requires `/pc/local/ins/<leaf>` bodies to include leaf-specific `data-d2-ins-route`, `D2_INS_LOCAL_PAGE`, `离线提示`, `data-d1-action="guarded" disabled`, and these exact title/button pairs: `Ins 帐号登录/登录 Ins 帐号`, `Ins 帐号搜索/提交帐号搜索`, `Ins 帖子搜索/提交帖子搜索`, `Ins 主页挖掘/采集主页数据`, `Ins 筛选活跃/开始活跃筛选`, `Ins 接口群发/开始接口群发`, `Ins 安卓智能体/启动安卓智能体`, `Ins AiCloud指纹/绑定 AiCloud 指纹`, `Ins AdsPower指纹/绑定 AdsPower 指纹`. Reject `任务批次号`, `fingerprintId`, `mock`, and `+10000000000`.

- [ ] Run `python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_d2_ins_bridge_serves_distinct_local_pages_with_guarded_operations -v`; expect RED because no Instagram local resolver exists.

### Task 2: Implement the two-class D-2 behavior

**Files:**

- Modify: `tools/m4_auth_patch/M4RecoveryCatalog.java:32-42,106-115,440-510,835-852`
- Modify: `tools/m4_auth_patch/M5LocalSpiderBridge.java:536-547,627-663`
- Modify: `tools/m4_auth_patch/M4AuthPatch.java:225-280,600-669`

- [ ] Add nine `/pc/local/ins/<leaf>` constants and change every `C4131_*` menu to an `insLocalRoute` helper. Its complete body is `return new MenuSpec(code, name, icon, "JSinglepage", route, "d2-ins-local:" + route.substring("/pc/local/ins/".length()));`.

- [ ] Add an Instagram-only child emission branch that copies the parent local route into `localCode`, writes `linkUrl="JSinglepage:" + route`, `treeEndFlg=1`, and `d2-ins-local-child:` evidence. Preserve all existing non-Instagram branches.

- [ ] Insert `localD2InsLocalPage(url)` immediately after `localD1XLocalPage(url)` in `localWebAssetBytes`. It recognizes only the nine paths; logs `D2_INS_LOCAL_PAGE`; emits static HTML `<main data-d2-ins-route="<leaf>">`, marker `D2_INS_LOCAL_PAGE`, one distinct offline sentence, and `<button data-d1-action="guarded" disabled>`; unknown routes return `null`. Do not change X or C-6 resolvers.

- [ ] Add `--d2-ins-overlay` and `writeD2InsOverlay(input, output)` copied from the D-1 archive loop. It must replace exactly `com/sbf/util/http/SBFApi.class` and `com/sbf/main/jxbrowser/M5LocalSpiderBridge.class`, otherwise fail; on success print `D2_INS_LOCAL_PAGES_OVERLAY`.

- [ ] Re-run the two Task 1 tests; expect GREEN.

### Task 3: Build and prove the narrow candidate delta

**Files:**

- Modify: `tests/test_m4_auth_patch.py:4826-4847`
- Create: `.artifacts/working/d2-ins-local-pages/App.d2.ins.candidate.dll`

- [ ] Add `test_d2_ins_overlay_changes_only_catalog_and_local_bridge_on_d1_candidate`. It must require `D2_INS_LOCAL_PAGES_OVERLAY`, identical JAR entry lists, byte identity for every entry except the two declared classes, and `javap` output containing `D2_INS_LOCAL_PAGE`.

- [ ] Run `python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_d2_ins_overlay_changes_only_catalog_and_local_bridge_on_d1_candidate -v`; observe RED before overlay support and GREEN after Task 2.

- [ ] Compile the patcher and run `--d2-ins-overlay` with `data/app/App.dll` as input and `.artifacts/working/d2-ins-local-pages/App.d2.ins.candidate.dll` as output. Confirm the live input SHA stays `BA33BD1AC222ECDEDB80A7DC4E91EB9741CD124801F6243666E56BCE9D8265C8`; record candidate SHA and the two-class manifest.

### Task 4: Isolated GUI, network, and non-regression proof

**Files:**

- Create: `.artifacts/working/d2-ins-local-pages/evidence.md`
- Create: `.artifacts/working/d2-ins-local-pages/screens/`
- Create: `.artifacts/working/d2-ins-local-pages/network-samples.csv`
- Modify: `.context/current-status.md`, `.context/work-log.md`

- [ ] Record and stop only the project live Java PID; copy the verified local no-update run; replace only the copy’s App.dll with the candidate; verify the candidate SHA and start it. Confirm the candidate Java executable path is inside the isolated copy. Never touch `H:\HuoChaiAI\app`.

- [ ] Open Instagram and capture every one of the nine leaves. Save one image each and record its local route, marker, offline text, guarded disabled button, and unique image hash.

- [ ] Sample isolated Java non-loopback TCP every 500 ms for 90 seconds across cold start and clicks. Save the rows and require zero matches for `47.97.27.111`, `163.181.39.184`, `39.101.114.44`, and `163.181.39.181`.

- [ ] Stop the candidate and restart project live Java. Read the SQLite database in read-only mode and require `COUNT=848`, `MAX_ID=858`, `sqlite_sequence=858`. Re-smoke X, C-6 commerce, and WhatsApp customer-service coverage.

- [ ] Run `python -m unittest discover -s tests -v`, then `python C:\Users\m1591\.codex\skills\project-context-os\scripts\validate_context.py --project-root H:\项目\出海-AI`, then `git diff --check`. Expect at least 74 passing tests, valid context, no new whitespace errors, preserved candidate, and no live replacement, merge, or PR.
