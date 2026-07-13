# Telegram Local Pages (D-5) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce and validate a Telegram-only candidate DLL in which every `C4135_*` menu reaches a distinct guarded `/pc/local/tg/<leaf>` offline page, then swap it into the D-4 live baseline only after every gate is green.

**Architecture:** `M4RecoveryCatalog` replaces the ten generic `JSinglepage + /pc/aicloud/my` routes and the C-5 Telegram group route with eleven explicit local parents plus matching leaf children. `M5LocalSpiderBridge` serves only those static pages, each carrying `D5_TG_LOCAL_PAGE` and a disabled guard button. `M4AuthPatch` creates an overlay over the current live DLL by replacing exactly `SBFApi.class` and `M5LocalSpiderBridge.class`.

**Tech Stack:** Java 8/ASM JAR overlay, Python `unittest`, PowerShell runtime isolation, SQLite read-only checks.

---

### Task 1: Establish the D-5 Telegram route contract with failing tests

**Files:**
- Modify: `tests/test_m4_auth_patch.py`

- [ ] **Step 1: Add a catalog probe for all eleven Telegram leaves**

```python
expected = {
    "C4135_001": "jump-push", "C4135_002": "accounts",
    "C4135_003": "ai-collect", "C4135_004": "ai-data",
    "C4135_005": "group-collect", "C4135_006": "group-member-extract",
    "C4135_007": "ai-filter", "C4135_008": "ai-growth",
    "C4135_009": "android-agent", "C4135_010": "aicloud-fingerprint",
    "C4135_011": "adspower-fingerprint",
}
```

Assert each parent is `JSinglepage` plus exactly `/pc/local/tg/<leaf>`, has `d5-tg-local:` evidence, and has one `treeEndFlg=1` child with `localCode` equal to that route, `linkUrl` equal to `JSinglepage:<route>`, and `d5-tg-local-child:` evidence. Assert no TG entry retains `/pc/aicloud/my`, `/pc/tg/index`, `JBigDataMaster`, or an `http` URL.

- [ ] **Step 2: Add a bridge probe for the eleven static pages**

```java
String body = M5LocalSpiderBridge.localWebAssetBody(
        "https://app.xdxsoft.com/pc/local/tg/" + page[0]);
```

For every page assert `data-d5-tg-route`, `D5_TG_LOCAL_PAGE`, its title, its offline sentence, its disabled action label, and exact `data-d1-action="guarded" disabled`. Reject mock data, account identifiers, task IDs, phone numbers, fingerprints, and executable collection/dispatch calls.

- [ ] **Step 3: Add the two-class overlay delta test**

Run `M4AuthPatch --d5-tg-overlay` with the current live `data/app/App.dll`. Compare JAR entries and require exactly `com/sbf/util/http/SBFApi.class` and `com/sbf/main/jxbrowser/M5LocalSpiderBridge.class` to differ.

- [ ] **Step 4: Run the three new D-5 tests and observe RED**

Run:

```powershell
python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_d5_tg_catalog_uses_eleven_distinct_explicit_local_leaves tests.test_m4_auth_patch.M4AuthPatchTests.test_d5_tg_bridge_serves_distinct_local_pages_with_guarded_operations tests.test_m4_auth_patch.M4AuthPatchTests.test_d5_tg_overlay_changes_only_catalog_and_local_bridge_on_live_baseline -v
```

Expected: failures identify existing generic/C-5 Telegram routing and missing D-5 overlay support.

### Task 2: Implement the minimum Telegram-only catalog and bridge

**Files:**
- Modify: `tools/m4_auth_patch/M4RecoveryCatalog.java`
- Modify: `tools/m4_auth_patch/M5LocalSpiderBridge.java`

- [ ] **Step 1: Map every C4135 entry to an explicit local route**

```java
private static MenuSpec tgLocalRoute(String code, String name, String icon, String route) {
    return new MenuSpec(code, name, icon, "JSinglepage", route,
            "d5-tg-local:" + route.substring("/pc/local/tg/".length()));
}
```

Replace all eleven Telegram `original(...)`/`c5Route(...)` entries with this helper. Remove `C4135_005` from the C-5 predicate so it cannot reach the former TG group-collection route.

- [ ] **Step 2: Emit a D-5 child route only for D-5 Telegram parents**

```java
appendString(json, "code", "REC_D5_TG_LOCAL_" + menu.code.substring("C4135_".length()));
appendString(json, "localCode", menu.linkUrl);
appendString(json, "linkUrl", "JSinglepage:" + menu.linkUrl);
appendString(json, "evidence", "d5-tg-local-child:" + menu.evidence);
```

Keep all X, Ins, FB, TikTok, GEO, WhatsApp, and customer-service child emitters unchanged.

- [ ] **Step 3: Serve the exact TG local pages as static guarded HTML**

```java
String d5TgLocalPage = localD5TgLocalPage(url);
if (d5TgLocalPage != null) {
    System.out.println("D5_TG_LOCAL_PAGE url=" + String.valueOf(url));
    return d5TgLocalPage.getBytes(StandardCharsets.UTF_8);
}
```

Use the fixed title/action pairs: `TG 跳推系统/启动跳推系统`, `TG 帐号/管理 TG 帐号`, `TG AI 采集/提交 TG AI采集`, `TG AI数据/查看 TG AI数据`, `TG AI 群采集/提交 TG AI群采集`, `TG AI 群成员提取/提取 TG 群成员`, `TG AI筛选/开始 TG AI筛选`, `TG AI裂变/启动 TG AI裂变`, `TG 安卓智能体/启动 TG 安卓智能体`, `TG AiCloud指纹/绑定 TG AiCloud指纹`, and `TG AdsPower指纹/绑定 TG AdsPower指纹`. Every corresponding offline sentence must say that local-only mode does not start, submit, bind, or modify the named real operation.

- [ ] **Step 4: Run the three D-5 tests and observe GREEN**

Re-run the exact command from Task 1 Step 4. Expected: all three pass.

### Task 3: Build a candidate without touching live

**Files:**
- Modify: `tools/m4_auth_patch/M4AuthPatch.java`
- Create: `.artifacts/working/d5-tg-local-pages/App.d5.tg.candidate.dll`

- [ ] **Step 1: Add the `--d5-tg-overlay` writer**

```java
bytes = patchPcMenusOverlay(readAll(in),
        "D5_TG_MENU_DISPATCH localLeaves=/pc/local/tg/*", "D5 Telegram local pages");
```

Copy all remaining JAR entries byte-for-byte, replace only the menu and local bridge entries, use an atomic move, and print `D5_TG_LOCAL_PAGES_OVERLAY`.

- [ ] **Step 2: Build the candidate from the recorded D-4 live SHA**

```powershell
java -cp .artifacts/working/d5-tg-local-pages/classes;data/app/App.dll M4AuthPatch --d5-tg-overlay data/app/App.dll .artifacts/working/d5-tg-local-pages/App.d5.tg.candidate.dll
```

Record input/output SHA-256 and entry delta. Do not copy this output to `data/app/App.dll` at this stage.

### Task 4: Candidate gates, reversible swap, and human handoff

**Files:**
- Create: `.artifacts/working/d5-tg-local-pages/evidence.md`
- Modify: `.context/work-log.md`
- Modify: `.context/current-status.md`

- [ ] **Step 1: Run the full suite and database baseline check**

Run `python -m unittest discover -s tests -p 'test_*.py' -v`; require at least 81 tests with no failures. Query the live WhatsApp database and require `COUNT(*)=848`, `MAX(id)=858`, and `sqlite_sequence=858`.

- [ ] **Step 2: Cold-start a copy containing only the candidate and capture all eleven TG leaves**

Keep the live DLL unchanged. Start the isolated copy, click each TG sub-menu manually, save screenshots and `D5_TG_LOCAL_PAGE` logs, and sample candidate-process non-loopback TCP throughout startup and all clicks. Require empty samples and zero connections to `47.97.27.111`, `163.181.39.184`, `39.101.114.44`, and `163.181.39.181`.

- [ ] **Step 3: Only if all three gates pass, perform the four swap steps**

1. Stop only the live Java process and record live SHA/DB values.
2. Create a timestamped `data/app/backups/App.live-*.dll` and rollback anchor.
3. Atomically replace `data/app/App.dll` with the D-5 candidate.
4. Cold-start from the project launcher, verify no .NET dialog, recapture TG pages/network/DB, and immediately restore the backup if any smoke gate fails.

- [ ] **Step 4: Preserve the verification boundary and request visual acceptance**

Re-run the full test suite after the successful live smoke. Append observed evidence to the active context log and refresh current status. Hand the live TG pages to the user for visual acceptance; do not start GEO until that acceptance is received.
