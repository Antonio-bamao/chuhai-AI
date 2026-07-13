# Facebook Local Pages (D-3) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a Facebook-only candidate DLL in which every C4747 menu dispatches to a distinct guarded local page, while leaving the D-2 live DLL untouched.

**Architecture:** `M4RecoveryCatalog` emits ten explicit `/pc/local/fb/<leaf>` parent routes and their leaf children, replacing the existing `JSinglepage:/pc/aicloud/my` fallback and the one C-5 FB page-collect route. `M5LocalSpiderBridge` intercepts only those local paths and returns static offline HTML with no data or working action. `M4AuthPatch` writes an overlay that replaces only the existing menu and local-bridge classes.

**Tech Stack:** Java 8 patcher, ASM/JAR overlay, Python `unittest`, PowerShell isolation and network sampling.

---

### Task 1: Lock the D-3 route contract with failing tests

**Files:**
- Modify: `tests/test_m4_auth_patch.py`
- Test: `tests/test_m4_auth_patch.py`

- [ ] **Step 1: Add catalog expectations for the ten Facebook code/leaf pairs**

```python
expected = {
    "C4747_000": "mirror-settings", "C4747_001": "friends-collect",
    "C4747_002": "groups-collect", "C4747_003": "pages-collect",
    "C4747_004": "live-collect", "C4747_005": "ads-collect",
    "C4747_006": "ad-comment-intercept", "C4747_007": "video-intercept",
    "C4747_008": "active-user-check", "C4747_009": "inquiry-reply",
}
```

Assert each parent has `localCode == "JSinglepage"`, the exact `/pc/local/fb/<leaf>` link, `d3-fb-local:` evidence, no fallback/C-5 route, and one matching child with `JSinglepage:<route>` plus `d3-fb-local-child:` evidence. Assert the ten routes are unique.

- [ ] **Step 2: Add the local-surface contract**

```java
String body = M5LocalSpiderBridge.localWebAssetBody(
        "https://app.xdxsoft.com/pc/local/fb/" + page[0]);
```

For all ten page triples, assert `data-d3-fb-route`, `D3_FB_LOCAL_PAGE`, the page title, the action label, `离线提示`, and `data-d1-action="guarded" disabled`; reject task IDs, phone numbers, fingerprints, and `mock` data.

- [ ] **Step 3: Add the overlay-delta contract**

Invoke `M4AuthPatch --d3-fb-overlay` with the D-2 live DLL and assert the output has exactly the two changed entries `SBFApi.class` and `M5LocalSpiderBridge.class`.

- [ ] **Step 4: Run the three D-3 tests and verify red**

Run: `python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_d3_fb_catalog_uses_ten_distinct_explicit_local_leaves tests.test_m4_auth_patch.M4AuthPatchTests.test_d3_fb_bridge_serves_distinct_local_pages_with_guarded_operations tests.test_m4_auth_patch.M4AuthPatchTests.test_d3_fb_overlay_changes_only_catalog_and_local_bridge_on_d2_candidate -v`

Expected: the new behavior tests fail because Facebook still uses the fallback/C-5 route and the overlay flag does not exist.

### Task 2: Implement minimal Facebook catalog and static bridge behavior

**Files:**
- Modify: `tools/m4_auth_patch/M4RecoveryCatalog.java`
- Modify: `tools/m4_auth_patch/M5LocalSpiderBridge.java`

- [ ] **Step 1: Define the ten FB local route constants and map all C4747 entries through a `fbLocalRoute` helper**

```java
private static MenuSpec fbLocalRoute(String code, String name, String icon, String route) {
    return new MenuSpec(code, name, icon, "JSinglepage", route,
            "d3-fb-local:" + route.substring("/pc/local/fb/".length()));
}
```

Remove the C4747_003 special C-5 classification so every Facebook menu follows the local route path.

- [ ] **Step 2: Emit a `REC_D3_FB_LOCAL_<suffix>` leaf for each FB parent**

```java
appendString(json, "code", "REC_D3_FB_LOCAL_" + menu.code.substring("C4747_".length()));
appendString(json, "localCode", menu.linkUrl);
appendString(json, "linkUrl", "JSinglepage:" + menu.linkUrl);
appendString(json, "evidence", "d3-fb-local-child:" + menu.evidence);
```

Call this emitter from the catalog loop only for evidence prefixed `d3-fb-local:`.

- [ ] **Step 3: Serve only the ten FB local routes as static offline HTML**

```java
String d3FbLocalPage = localD3FbLocalPage(url);
if (d3FbLocalPage != null) {
    System.out.println("D3_FB_LOCAL_PAGE url=" + String.valueOf(url));
    return d3FbLocalPage.getBytes(StandardCharsets.UTF_8);
}
```

Use distinct title/offline/action triples: 镜像系统设置/保存镜像系统设置, FB 好友采集/提交好友采集, FB 小组采集/提交小组采集, FB 主页采集/提交主页采集, FB 直播采集/提交直播采集, FB 广告采集/提交广告采集, FB 广告评论截流/开始广告评论截流, FB 视频截流/开始视频截流, FB 活跃用户检测/开始活跃用户检测, FB 询盘回复/开始询盘回复. Every action remains disabled and no path invokes collection code.

- [ ] **Step 4: Run the three D-3 tests and verify green**

Run the same command as Task 1 Step 4.

Expected: all three tests pass.

### Task 3: Add a D-3 two-class candidate overlay and build the candidate

**Files:**
- Modify: `tools/m4_auth_patch/M4AuthPatch.java`
- Test: `tests/test_m4_auth_patch.py`
- Create: `.artifacts/working/d3-fb-local-pages/App.d3.fb.candidate.dll`

- [ ] **Step 1: Parse `--d3-fb-overlay` and add a D-3 overlay writer**

```java
bytes = patchPcMenusOverlay(readAll(in),
        "D3_FB_MENU_DISPATCH localLeaves=/pc/local/fb/*", "D3 Facebook local pages");
```

Copy all other JAR entries unchanged and replace only `TARGET_CLASS` and `M5_LOCAL_SPIDER_BRIDGE_CLASS`; use an atomic move and print `D3_FB_LOCAL_PAGES_OVERLAY`.

- [ ] **Step 2: Run the D-3 overlay-delta test**

Run the third D-3 test alone and confirm it passes with exactly two changed classes.

- [ ] **Step 3: Build candidate from `data/app/App.dll` without copying it back**

Run the patcher with `--d3-fb-overlay`, writing only `.artifacts/working/d3-fb-local-pages/App.d3.fb.candidate.dll`. Record input SHA-256, output SHA-256, and JAR entry delta.

### Task 4: Candidate validation and reversible isolated evidence

**Files:**
- Create: `.artifacts/working/d3-fb-local-pages/capture-d3-fb-network.ps1`
- Create: `.artifacts/working/d3-fb-local-pages/screens/fb-*.png`
- Test: `tests/test_m4_auth_patch.py`

- [ ] **Step 1: Run the full regression suite**

Run: `python -m unittest discover -s tests -p 'test_*.py' -v`

Expected: at least 78 tests, all passing.

- [ ] **Step 2: Record candidate-only route/HTML and offline network evidence**

Sample TCP connections during candidate cold start and all FB menu clicks; the target set is `47.97.27.111`, `163.181.39.184`, `39.101.114.44`, and `163.181.39.181`. Require zero matching connections. Do not modify hosts, `.cnf`, native libraries, system time, or collection chain.

- [ ] **Step 3: Isolate the live process, launch the candidate, capture each FB page, then restore live**

Before stopping live record its process and SHA. Use the existing isolation launcher/profile, click every C4747 entry, save ten screenshots, and collect console `D3_FB_LOCAL_PAGE` route hits. After evidence, terminate only the candidate and restart the normal live launcher. Verify live SHA remains D2 and it reaches the menu without a .NET dialog.

- [ ] **Step 4: Recheck non-regression state**

Query the live WhatsApp DB for `COUNT(*)` and `MAX(id)` (must remain `848` and `858`); run existing X/Ins/C-6/WA test coverage and report only observed results. Do not swap the D-3 candidate into live or merge/PR.
