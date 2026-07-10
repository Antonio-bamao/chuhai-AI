# C-6 Commerce UI Closure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:executing-plans` to execute this plan task by task.

**Goal:** Produce a candidate-only `App.dll` in which the original global recharge entry and the original `C3460_001` advertising entry open their existing hosts with stable local empty UI. Payment, order, advertising authorization, campaign creation, delivery, and charging remain unavailable and visibly disabled. No original service request is permitted.

**Architecture:** Keep the original entry surfaces: the bottom-left recharge control in `JSBFMain` and the `com.sbf.main.theme.ad.f` advertising shell selected by `C3460_001`. Add a narrowly scoped C-6 local content contract for only those surfaces, delivered through the existing JxBrowser local-scheme bridge. Patch the two discovered entry actions to open the corresponding original host with a C-6 local URL. The local content contains no balances, orders, plans, or performance rows; it labels unavailable dependencies and disables mutation controls. Route-specific JavaScript and XHR guards enforce the same boundary if the original host attempts a read or action request.

**Tech Stack:** Java 8, ASM 9 bytecode patching, JxBrowser 7 local-scheme bridge, existing `M4AuthPatch`, `M5LocalSpiderBridge`, and Python `unittest` harness.

---

## Scope and invariants

- Candidate output only. Do not overwrite `data/app/App.dll`, alter `data/app/app.ver`, commit a candidate, or perform a live swap.
- Preserve the original global recharge and `theme.ad.f` advertising entry points. Do not create a WhatsApp recharge menu entry or a parallel advertising shell.
- Keep all real payment, order, advertising, authorization, delivery, and charging calls unavailable. Do not return synthetic balances, payment records, plans, campaign rows, credits, or metrics.
- Do not modify `.cnf`, `cloud.spider.b`, `libmytrpc`, system-clock behavior, or any collection chain.
- Use only exact C-6 local URLs and exact C-6 request paths in the bridge; no wildcard proxy, hostname fallback, or global button/XHR interception.
- Re-run the existing regression suite before reporting success.

## Task 1: Pin the two original entry actions and their browser hosts

**Files:**
- Read: `data/app/App.dll`
- Read: `.artifacts/working/m5-online-full/**`
- Read: `tools/m4_auth_patch/M4AuthPatch.java`
- Read: `tools/m4_auth_patch/M4RecoveryCatalog.java`
- Add: `.artifacts/working/c6-commerce-entry-evidence.json`

**Step 1: Inspect the global recharge listener candidates.**

Run:

```powershell
& jar tf data/app/App.dll | Select-String 'com/sbf/main/JSBFMain\$'
& javap -classpath data/app/App.dll -c -p com.sbf.main.JSBFMain | Select-String -Pattern '充值|alipay|enterpriseAuth|personal/auth|userPayofflineOrder' -Context 8,16
```

For every nested `JSBFMain$*.class` returned by the first command, inspect its listener body until the one that opens the recharge surface is found:

```powershell
& javap -classpath data/app/App.dll -c -p 'com.sbf.main.JSBFMain$NN' | Select-String -Pattern 'mouseClicked|actionPerformed|充值|alipay|enterpriseAuth|JxBrowser|JZWBrowserMaster' -Context 8,20
```

Replace `NN` with each candidate number. Record the one class, method descriptor, existing destination, and host type.

**Step 2: Inspect the advertising menu action and `theme.ad.f` mount.**

Run:

```powershell
& javap -classpath data/app/App.dll -c -p com.sbf.main.theme.ad.f | Select-String -Pattern 'pay_money|广告充值|帐号余额|overseasAds|JxBrowser|new.*com/sbf/main/jxbrowser/c' -Context 10,24
rg -n -S 'C3460_001|theme\.ad\.f|overseasAds|pay_money|广告充值' tools/m4_auth_patch .artifacts/working/m5-online-full
```

Identify the menu-switch class and method that instantiates or selects `com.sbf.main.theme.ad.f`, then identify its internal page destination(s).

**Step 3: Write the evidence artifact.**

Create `.artifacts/working/c6-commerce-entry-evidence.json` with only these fields:

```json
{
  "recharge": {
    "listenerClass": "com.sbf.main.JSBFMain$NN",
    "listenerMethod": "actionPerformed(Ljava/awt/event/ActionEvent;)V",
    "existingDestination": "/pc/alipay/enterpriseAuth",
    "host": "JxBrowser"
  },
  "advertising": {
    "menuCode": "C3460_001",
    "hostClass": "com.sbf.main.theme.ad.f",
    "mountMethod": "<method descriptor>",
    "existingDestinations": ["/views/overseasAds/dataBoard", "/views/overseasAds/adsPeople", "/views/overseasAds/addTask"]
  }
}
```

The artifact must reflect observed bytecode. It must not contain guessed listener names or routes.

**Step 4: Add a red test that asserts pinned evidence is used.**

In `tests/test_m4_auth_patch.py`, add `test_c6_entry_evidence_and_original_hosts_are_pinned`. It must load the JSON artifact, assert the observed recharge listener and advertising host are nonempty, verify `C3460_001` remains in `M4RecoveryCatalog.java`, and assert candidate patch source contains both observed class names before a candidate can be built.

**Step 5: Run the focused test and verify it fails before C-6 production code exists.**

```powershell
python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_entry_evidence_and_original_hosts_are_pinned
```

Expected: failure because no `C6_` implementation markers have been added to the patcher.

## Task 2: Define the C-6 local empty UI contract by test first

**Files:**
- Modify: `tests/test_m4_auth_patch.py`
- Modify: `tools/m4_auth_patch/M5LocalSpiderBridge.java`
- Modify: `tools/m4_auth_patch/M4AuthPatch.java`

**Step 1: Add a red contract test.**

Add `test_c6_commerce_local_contract_is_empty_and_non_mutating` to `tests/test_m4_auth_patch.py`. Compile the existing Java probe pattern against `M5LocalSpiderBridge` and request:

```text
http://m5.local/pc/c6/recharge
http://m5.local/pc/c6/advertising
http://m5.local/api/c6/recharge/status
http://m5.local/api/c6/advertising/plans
```

Assert the two HTML documents include the distinct markers `C6_RECHARGE_UI` and `C6_ADVERTISING_UI`; include `data-c6-action="disabled"`; contain no literal numeric balance, order row, plan row, campaign row, or payment QR URL; and the two JSON bodies are self-describing unavailable/empty contracts:

```json
{"code":200,"msg":"C6_RECHARGE_UNAVAILABLE","data":{"available":false,"localOnly":true}}
```

```json
{"code":200,"msg":"C6_ADVERTISING_EMPTY","data":{"available":false,"localOnly":true},"rows":[],"total":0}
```

Assert a mutation request for either local API path returns a controlled `503` response with marker `C6_COMMERCE_ACTION_BLOCKED`, never a success response.

**Step 2: Run the new test and observe red.**

```powershell
python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_commerce_local_contract_is_empty_and_non_mutating
```

Expected: C-6 local paths resolve to no body before implementation.

**Step 3: Implement only exact C-6 local content in the existing bridge.**

In `M5LocalSpiderBridge.java`, add constants for the four exact C-6 paths and extend the existing local-body resolver with strict equality checks. The two HTML documents must be simple, local static pages inside the original browser host, for example:

```html
<main data-c6-surface="recharge">
  <h1>充值</h1>
  <p>C6_RECHARGE_UI</p>
  <p>当前离线，支付与订单功能不可用。</p>
  <button data-c6-action="disabled" disabled>立即充值</button>
</main>
```

```html
<main data-c6-surface="advertising">
  <h1>广告获客</h1>
  <p>C6_ADVERTISING_UI</p>
  <p>当前离线，广告计划、授权与投放功能不可用。</p>
  <button data-c6-action="disabled" disabled>创建广告计划</button>
</main>
```

No amounts, balances, records, campaign mockups, QR code URLs, external image URLs, or payment provider URLs may appear in the HTML.

Use the bridge's existing local HTTP response facility to return the two read contracts. Add exact C-6 mutation detection that returns a `503` JSON response before any delegation. Do not add a broad `/api/` rule.

**Step 4: Add route-scoped browser guard markers.**

In the existing injected-script generator in `M4AuthPatch.java`, add a C-6 branch that only runs when `location.pathname` equals `/pc/c6/recharge` or `/pc/c6/advertising`. It must:

- log `C6_RECHARGE_UI_GATED` or `C6_ADVERTISING_UI_GATED`;
- disable controls with `data-c6-action="disabled"`;
- return controlled local responses for only the two C-6 API paths;
- log `C6_COMMERCE_ACTION_BLOCKED` and reject non-GET C-6 local API calls.

Do not change existing C-5, C-2, or M8 request gates.

**Step 5: Re-run the focused contract test.**

```powershell
python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_commerce_local_contract_is_empty_and_non_mutating
```

Expected: green.

## Task 3: Route the original global recharge control to the local C-6 page

**Files:**
- Modify: `tests/test_m4_auth_patch.py`
- Modify: `tools/m4_auth_patch/M4AuthPatch.java`
- Modify only if required by existing helper shape: `tools/m4_auth_patch/M5LocalSpiderBridge.java`

**Step 1: Add a red bytecode test.**

Add `test_c6_global_recharge_entry_preserves_original_control_and_uses_local_leaf`. The test must build a disposable candidate with the existing patcher harness and use `javap -c -p` on the exact listener class recorded in `.artifacts/working/c6-commerce-entry-evidence.json`.

Assert the patched listener contains:

```text
C6_GLOBAL_RECHARGE_ENTRY
/pc/c6/recharge
```

and does not add a `充值` item to any `M4RecoveryCatalog` sidebar list. Assert the Java source does not map any normal product menu node to `/pc/c6/recharge`.

**Step 2: Run the test and observe red.**

```powershell
python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_global_recharge_entry_preserves_original_control_and_uses_local_leaf
```

**Step 3: Add a narrow listener patch.**

In the existing ASM patch section of `M4AuthPatch.java`, add a method dedicated to the exact listener class and method descriptor recorded in the evidence artifact. It must preserve the original control and replace only its browser destination with:

```java
"/pc/c6/recharge"
```

Emit a diagnostic marker through the existing trace/log style:

```java
"C6_GLOBAL_RECHARGE_ENTRY route=/pc/c6/recharge"
```

Do not alter shared sidebar construction or any WhatsApp catalog node. If the original listener opens a `JZWBrowserMaster`, reuse that host rather than instantiating a new independent shell.

**Step 4: Re-run the focused test.**

```powershell
python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_global_recharge_entry_preserves_original_control_and_uses_local_leaf
```

Expected: green.

## Task 4: Route the original `C3460_001` advertising host to local empty content

**Files:**
- Modify: `tests/test_m4_auth_patch.py`
- Modify: `tools/m4_auth_patch/M4AuthPatch.java`
- Modify only if native-host initialization requires it: `tools/m4_auth_patch/M5LocalSpiderBridge.java`

**Step 1: Add a red native-host test.**

Add `test_c6_advertising_entry_keeps_theme_ad_host_and_uses_local_leaf`. Build a disposable candidate. Verify with `javap -c -p` on the observed menu/mount class from the evidence JSON that:

- `C3460_001` still selects or instantiates `com.sbf.main.theme.ad.f`;
- a C-6 marker `C6_ADVERTISING_ENTRY` is emitted;
- the browser destination is `/pc/c6/advertising`;
- the original static class still contains `广告充值` and `帐号余额` labels, proving the original host was retained;
- neither `/views/overseasAds/addTask` nor an external advertising/payment URL is used for C-6 local opening.

**Step 2: Run it and observe red.**

```powershell
python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_advertising_entry_keeps_theme_ad_host_and_uses_local_leaf
```

**Step 3: Add an exact advertising mount patch.**

Patch the observed `theme.ad.f` browser-mount destination, or the exact factory call that supplies it, to open:

```java
"/pc/c6/advertising"
```

The patch must leave the native `theme.ad.f` class, the `C3460_001` menu identity, and its `广告充值` entry in place. Add the marker:

```java
"C6_ADVERTISING_ENTRY route=/pc/c6/advertising"
```

For all original `theme.ad.f` tabs, map only their first local content page to this C-6 leaf; do not route any tab to original remote paths and do not invent separate menu trees.

**Step 4: Re-run the focused test.**

```powershell
python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_advertising_entry_keeps_theme_ad_host_and_uses_local_leaf
```

Expected: green.

## Task 5: Build the candidate and enforce no fake financial/advertising data

**Files:**
- Modify: `tests/test_m4_auth_patch.py`
- Read: candidate output emitted by the existing test build harness

**Step 1: Add a red candidate policy test.**

Add `test_c6_candidate_has_no_fake_commerce_data_or_external_commerce_fallback`. It must extract/inspect candidate class strings and generated local content and assert:

- markers `C6_RECHARGE_UI`, `C6_ADVERTISING_UI`, `C6_COMMERCE_ACTION_BLOCKED`, `C6_GLOBAL_RECHARGE_ENTRY`, and `C6_ADVERTISING_ENTRY` exist;
- no `LOCAL-OFFLINE` value is returned by a C-6 balance contract;
- no payment provider URL, `/pc/alipay/`, `/pc/userPayofflineOrder/`, `overseasAds/addTask`, or `xpay/order` appears in C-6 local paths/content;
- C-6 API mutation calls resolve to controlled non-2xx local responses;
- `M4RecoveryCatalog.java` still has no newly created recharge sidebar node.

**Step 2: Run the test and observe red until policy implementation is complete.**

```powershell
python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_candidate_has_no_fake_commerce_data_or_external_commerce_fallback
```

**Step 3: Build a disposable candidate via the existing test harness.**

Use the same `run_patcher` / copied-JAR setup already used by the C-5 and super-environment tests. Keep the output under `.artifacts/working/`; do not target `data/app/App.dll`.

**Step 4: Make the policy test green.**

```powershell
python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_candidate_has_no_fake_commerce_data_or_external_commerce_fallback
```

## Task 6: Candidate runtime verification and regression

**Files:**
- Read: candidate `App.dll` and harness logs/screenshots under `.artifacts/working/`
- Do not modify: `data/app/App.dll`

**Step 1: Run all C-6 focused tests.**

```powershell
python -m unittest \
  tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_entry_evidence_and_original_hosts_are_pinned \
  tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_commerce_local_contract_is_empty_and_non_mutating \
  tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_global_recharge_entry_preserves_original_control_and_uses_local_leaf \
  tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_advertising_entry_keeps_theme_ad_host_and_uses_local_leaf \
  tests.test_m4_auth_patch.M4AuthPatchTests.test_c6_candidate_has_no_fake_commerce_data_or_external_commerce_fallback
```

In PowerShell, run this as a single line if line continuation is inconvenient.

**Step 2: Run an isolated candidate harness.**

Start only the candidate package with the existing local harness. Capture one screenshot and a route/console trace for each original entry:

- global bottom-left `充值` opens `/pc/c6/recharge`, paints `C6_RECHARGE_UI`, and shows its disabled control;
- `C3460_001` opens the retained `theme.ad.f` host and `/pc/c6/advertising`, paints `C6_ADVERTISING_UI`, and shows disabled advertising controls;
- both traces contain the appropriate C-6 entry and gate marker.

**Step 3: Verify zero original-service fallback.**

Search the candidate harness request log for these strings:

```text
app.xdxsoft.com
diangxiaomi.com
wandange.com
alipay
overseasAds/addTask
```

The C-6 interaction window must contain none. Record the negative result in the candidate evidence notes.

**Step 4: Run the full regression suite.**

```powershell
python -m unittest tests.test_m4_auth_patch
```

Expected: all tests green, including WhatsApp, Facebook, Instagram, X, Super Environment, and C-5 platform recovery tests.

**Step 5: Report candidate evidence only.**

Report candidate SHA-256, the two screenshots, entry/route markers, request-log negative check, and regression result. State explicitly that no live swap was performed and `data/app/App.dll` was untouched.

## Final review checklist

- [ ] No side-menu recharge node added.
- [ ] `C3460_001` still uses `com.sbf.main.theme.ad.f`.
- [ ] Original hosts open exact local C-6 pages.
- [ ] Empty UI has no balance, order, plan, campaign, or metric fixtures.
- [ ] Mutation actions are disabled and blocked locally.
- [ ] No original/third-party commerce request observed.
- [ ] Existing suite passes.
- [ ] No live file or prohibited subsystem modified.
