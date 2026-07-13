# D-5 GEO Local Pages Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Produce a candidate-only GEO overlay with nine explicit guarded local pages.

**Architecture:** `M4RecoveryCatalog` will turn the nine existing GEO menu records into explicit `JSinglepage` leaves. `M5LocalSpiderBridge` will intercept only those local paths and return static, semantic empty-state HTML. The overlay builder will keep the two-class boundary used by TG.

**Tech Stack:** Java source generators, Python `unittest`, JAR/ZIP entry comparison, PowerShell TCP sampling.

---

### Task 1: GEO contracts (red)

**Files:**
- Modify: `tests/test_m4_auth_patch.py`

- [ ] **Step 1: Add failing catalog contract**

```python
def test_d5_geo_catalog_uses_nine_distinct_explicit_local_leaves(self):
    # Assert C4134_002/003/006 and C4137_001..006 map one-to-one to /pc/local/geo/*.
```

- [ ] **Step 2: Add failing bridge contract**

```python
def test_d5_geo_bridge_serves_distinct_local_pages_with_guarded_operations(self):
    # Assert marker, semantic page title, and disabled guarded action for every GEO leaf.
```

- [ ] **Step 3: Add failing overlay-boundary contract**

```python
def test_d5_geo_overlay_changes_only_catalog_and_local_bridge_on_tg_live_baseline(self):
    # Build from live App.dll and require exactly SBFApi.class plus M5LocalSpiderBridge.class to differ.
```

- [ ] **Step 4: Run the three tests**

Run: `python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_d5_geo_catalog_uses_nine_distinct_explicit_local_leaves tests.test_m4_auth_patch.M4AuthPatchTests.test_d5_geo_bridge_serves_distinct_local_pages_with_guarded_operations tests.test_m4_auth_patch.M4AuthPatchTests.test_d5_geo_overlay_changes_only_catalog_and_local_bridge_on_tg_live_baseline -v`

Expected: FAIL because the D5 GEO contract and overlay mode do not exist yet.

### Task 2: Minimal two-class GEO overlay (green)

**Files:**
- Modify: `tools/m4_auth_patch/M4RecoveryCatalog.java`
- Modify: `tools/m4_auth_patch/M5LocalSpiderBridge.java`
- Modify: `tools/m4_auth_patch/M4AuthPatch.java`

- [ ] **Step 1: Add `geoLocalRoute` and nine GEO leaf constants**

Map only the nine listed menu codes to their explicit local paths and evidence prefix `d5-geo-local:`.

- [ ] **Step 2: Add `localD5GeoLocalPage`**

Return HTML only for `/pc/local/geo/*`, with `D5_GEO_LOCAL_PAGE`, distinct title/prompt, and `<button data-d1-action="guarded" disabled>`.

- [ ] **Step 3: Add `--d5-geo-overlay`**

Reuse the overlay copy loop while excluding only `com/sbf/util/http/SBFApi.class` and `com/sbf/main/jxbrowser/M5LocalSpiderBridge.class`.

- [ ] **Step 4: Re-run the three targeted tests**

Run the Task 1 command. Expected: PASS.

### Task 3: Candidate evidence

**Files:**
- Create: `.artifacts/working/d5-geo-local-pages/App.d5.geo.candidate.dll`
- Create: `.artifacts/working/d5-geo-local-pages/evidence/d5-geo-cold-start-network.csv`

- [ ] **Step 1: Build against the locked TG live App.dll**

Run the patcher with `--d5-geo-overlay`, record input/output SHA-256, and compare ZIP entries. Expected: exactly two changed classes.

- [ ] **Step 2: Run complete regression**

Run: `python -m unittest discover -s tests -p 'test_*.py' -v`

Expected: no failures and at least 84 tests.

- [ ] **Step 3: Isolated cold-start check**

Use the asInvoker runner with the candidate, collect 180 TCP samples at 500 ms, require no non-loopback rows and zero watched targets. Re-query the live DB `spider_data` contract as `848/858/858`.

- [ ] **Step 4: Report gates**

State candidate result and request user authorization before changing `data/app/App.dll`; do not swap or launch project live under this task.
