# D-2 Instagram Local Pages Design

## Goal

Apply the D-1 explicit-local-leaf pattern to Instagram only: every recovered Instagram menu renders a distinct, local, empty-state page without creating tasks, data, accounts, fingerprints, or outbound requests.

## Scope and non-goals

- Scope: the nine `C4131_*` Instagram menu entries, their explicit leaf children, and static local HTML responses.
- Out of scope: `.cnf`, `cloud.spider.b`, `libmytrpc`, real collection/search/send/login/fingerprint actions, live replacement, and all non-Instagram menu behavior.
- Candidate baseline: project live `data/app/App.dll`, SHA-256 `BA33BD1AC222ECDEDB80A7DC4E91EB9741CD124801F6243666E56BCE9D8265C8`.

## Baseline evidence

`M4RecoveryCatalog.MENUS[instagram]` contains these nine parent menus:

| Code | Existing dispatch | D-2 local leaf |
| --- | --- | --- |
| `C4131_002` Ins 帐号登录 | `original(...)` → `JSinglepage` / `/pc/aicloud/my` | `/pc/local/ins/account-login` |
| `C4131_003` Ins 帐号搜索 | same generic fallback | `/pc/local/ins/account-search` |
| `C4131_004` Ins 帖子搜索 | same generic fallback | `/pc/local/ins/post-search` |
| `C4131_005` Ins 主页挖掘 | special big-data route `/es/bigData/bigDataTask?code=ins_blogger_data` | `/pc/local/ins/profile-mining` |
| `C4131_006` Ins 筛选活跃 | same generic fallback | `/pc/local/ins/active-filter` |
| `C4131_007` Ins 接口群发 | same generic fallback | `/pc/local/ins/api-broadcast` |
| `C4131_008` Ins 安卓智能体 | same generic fallback | `/pc/local/ins/android-agent` |
| `C4131_009` Ins AiCloud指纹 | same generic fallback | `/pc/local/ins/aicloud-fingerprint` |
| `C4131_010` Ins AdsPower指纹 | same generic fallback | `/pc/local/ins/adspower-fingerprint` |

The current special `C4131_005` route is included in D-2 because the requested contract applies to every Instagram submenu.

## Architecture

Only two existing classes change in the candidate overlay.

1. `M4RecoveryCatalog.java`
   - Define the nine `/pc/local/ins/<leaf>` constants.
   - Replace all nine Instagram parents with a narrow `insLocalRoute(...)` helper (`localCode=JSinglepage`, local parent URL, `d2-ins-local:<leaf>` evidence).
   - Emit one explicit leaf child per parent: `localCode=/pc/local/ins/<leaf>`, `linkUrl=JSinglepage:/pc/local/ins/<leaf>`, `treeEndFlg=1`, and `d2-ins-local-child:<leaf>` evidence. This prevents `JSinglepage` route normalization from selecting the shared fallback.

2. `M5LocalSpiderBridge.java`
   - Resolve only `/pc/local/ins/<leaf>` URLs before external/local asset fallbacks.
   - Return one semantic empty state per leaf containing its title, a distinct offline explanation, one disabled semantic button, `D2_INS_LOCAL_PAGE`, and `data-d1-action="guarded" disabled`.
   - Return `null` for all non-Instagram local paths, preserve the existing X and C-6 paths byte-for-byte, and expose no data rows or mock identifiers.

`M4AuthPatch --d2-ins-overlay` will construct a fresh candidate by compiling these sources and replacing exactly `SBFApi.class` and `M5LocalSpiderBridge.class` in the D-1 live-baseline JAR.

## Error handling and safety

- Unknown paths return `null`; they do not become generic data pages.
- Guarded buttons are inert HTML attributes, not JavaScript handlers; no click path calls collection, login, send, or fingerprint code.
- Candidate testing uses an isolated copy with a local no-update launcher. Live Java is stopped only for isolated GUI ownership, then restarted from the project runtime after evidence collection.

## Verification contract

1. TDD contracts assert all nine parent routes and explicit children, uniqueness, no `http` URLs, and no remaining generic/big-data Instagram dispatch.
2. TDD contracts load each leaf through `M5LocalSpiderBridge` and assert title, `D2_INS_LOCAL_PAGE`, correct route marker, offline message, and guarded disabled button; reject task IDs, numbers, fingerprints, and mock rows.
3. Overlay test verifies exactly the same two changed classes as D-1.
4. Isolated GUI captures nine distinct Instagram leaf screens and records route/marker/guard evidence; no live overwrite occurs.
5. Capture non-loopback TCP for cold start and leaf clicks; target matches for `47.97.27.111`, `163.181.39.184`, `39.101.114.44`, and `163.181.39.181` must be zero.
6. Re-run complete regression (at least 74 tests), confirm DB `COUNT=848 / MAX_ID=858 / sqlite_sequence=858`, and re-smoke existing X, C-6 commerce, and WhatsApp customer-service surfaces.
