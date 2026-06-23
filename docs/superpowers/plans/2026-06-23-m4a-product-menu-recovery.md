# M4A 九产品与侧边栏恢复 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在冻结 v33 技术能力的前提下，用原包证据和明确恢复值替换单一 AIGC 临时产品，生成 8 个可进入系统、1 个未开通独立站系统及各系统稳定菜单拓扑。

**Architecture:** 新增无第三方依赖的 `M4RecoveryCatalog`，集中定义产品恢复 ID、产品字段、菜单 code/名称/icon/入口与 JSON 序列化；`M4AuthPatch` 只消费目录生成的两个 JSON 字符串。测试直接运行目录探针并运行补丁后的 `SBFApi.C()/k()`，同时保留 v33 的修改类集合和关键字节码断言。

**Tech Stack:** Java 8、ASM、Python `unittest`、原包 `org.json`、PowerShell/Git。

---

### Task 1: 建立九产品恢复目录契约

**Files:**
- Create: `tools/m4_auth_patch/M4RecoveryCatalog.java`
- Modify: `tests/test_m4_auth_patch.py`

- [x] **Step 1: 写失败测试**

在测试中同时编译 `M4RecoveryCatalog.java`，运行 Java 探针并断言：

```java
JSONArray products = new JSONObject(M4RecoveryCatalog.productModulesJson()).getJSONArray("data");
String[] codes = {"whatsapp","tiktok","facebook","instagram","twitter","telegram","geo","wskefu","aishope"};
if (products.length() != codes.length) throw new AssertionError("product count");
for (int i = 0; i < codes.length; i++) {
    JSONObject product = products.getJSONObject(i);
    if (product.getInt("id") != 9101 + i) throw new AssertionError("recovery id");
    if (!codes[i].equals(product.getString("code"))) throw new AssertionError("code");
    if (i < 8 && product.getInt("status") != 1) throw new AssertionError("enterable");
    if (i == 8 && (product.getInt("status") == 0 || product.getInt("status") == 1)) {
        throw new AssertionError("aishope must be unopened");
    }
}
```

- [x] **Step 2: 运行测试确认失败**

Run: `python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_recovery_catalog_has_nine_products -v`

Expected: FAIL，原因是 `M4RecoveryCatalog.java` 不存在。

- [x] **Step 3: 实现最小产品目录**

实现 `productModulesJson()`，集中定义：

```java
private static final String[] PRODUCT_CODES = {
    "whatsapp", "tiktok", "facebook", "instagram", "twitter",
    "telegram", "geo", "wskefu", "aishope"
};
private static final int PRODUCT_ID_BASE = 9101;
```

每个产品必须包含产品选择器和 `StartApp$1$3` 已确认消费的字段；前 8 个 `status=1`、`remainingDays=99999`，`aishope status=2`。`logoSvg` 使用对应原包资源语义，主题字段使用统一兼容主题。

- [x] **Step 4: 运行测试确认通过**

Run: `python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_recovery_catalog_has_nine_products -v`

Expected: PASS。

### Task 2: 建立各系统菜单恢复目录

**Files:**
- Modify: `tools/m4_auth_patch/M4RecoveryCatalog.java`
- Modify: `tests/test_m4_auth_patch.py`

- [x] **Step 1: 写失败测试**

断言菜单目录：

```java
JSONObject menus = new JSONObject(M4RecoveryCatalog.pcMenusJson());
JSONArray entries = menus.getJSONArray("scfs");
Set<Integer> productParents = new HashSet<Integer>();
for (int i = 0; i < entries.length(); i++) {
    JSONObject item = entries.getJSONObject(i);
    if (item.getInt("id") < 910100) throw new AssertionError("not a recovery id");
    if (item.getString("code").startsWith("C2850000")) throw new AssertionError("temporary AIGC");
    if (item.getString("name").contains("AIGC Video")) throw new AssertionError("temporary label");
    if (!item.getString("icon").startsWith("svg/")) throw new AssertionError("original icon family");
    productParents.add(item.getInt("productId"));
}
if (productParents.size() != 8) throw new AssertionError("menus for eight products");
```

另断言 WhatsApp 显示拓扑包含“一句话、智能体模型、AI龙虾、超级号、AI采集、AI数据、AI筛选、AI群发、API、广告、AI客服”，并断言 TikTok/Facebook/Instagram/Twitter/Telegram/GEO/WhatsApp 客服菜单数量分别与恢复证据表一致。

- [x] **Step 2: 运行测试确认失败**

Run: `python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_recovery_catalog_has_product_specific_menus -v`

Expected: FAIL，原因是仍为临时 AIGC 菜单或方法不存在。

- [x] **Step 3: 实现菜单目录**

为 8 个可进入产品定义独立 `MenuSpec[]`。优先使用 i18n 已证实 code：

```java
menu("C3461_002", "TK AI采集", "svg/menu_tk_2.svg");
menu("C4747_001", "FB 好友采集", "svg/facebook_menu_icon_2.svg");
menu("C4131_003", "Ins 帐号搜索", "svg/ins_menu_icon_3.svg");
menu("C4133_003", "X 精准搜索", "svg/twitter_menu_icon_3.svg");
menu("C4135_003", "TG AI 采集", "svg/tg_menu_icon_3.svg");
menu("C4137_001", "全球号码采集", "svg/geo_ai_menu_icon_1.svg");
menu("C4936_000", "信息总览", "svg/wskf_menu_icon_1.svg");
```

截图独有且无唯一原 code 的 WhatsApp 项使用 `REC_WHATSAPP_*` code，并在证据文档标为恢复值。菜单恢复 ID 按 `.context/m4-product-menu-evidence.md` 第 9 节生成。M4A 阶段入口统一使用保守兼容 `localCode=JSinglepage`、`linkUrl=/pc/aicloud/my`、`webFlg=1`，明确作为恢复入口，真实业务分流留给 M5A。

- [x] **Step 4: 运行测试确认通过**

Run: `python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_recovery_catalog_has_product_specific_menus -v`

Expected: PASS。

### Task 3: 将恢复目录接入补丁并保护 v33 能力

**Files:**
- Modify: `tools/m4_auth_patch/M4AuthPatch.java`
- Modify: `tests/test_m4_auth_patch.py`

- [x] **Step 1: 写失败测试**

修改现有补丁探针，要求 `SBFApi.C()` 返回九产品，`SBFApi.k()` 不含 `C28500001/C28500002/AIGC Video/Graphic Video`，并保留：

```python
self.assertIn("M4_V18_NORMALIZED_URL=", browser_load_block)
self.assertIn("M4_V19_WEB_TOKEN_BRIDGE url=", web_token_bridge_block)
self.assertIn("RenderingMode.OFF_SCREEN", engine_create_block)
self.assertIn("/prod-api/getRouters", inject_js_callback_block)
```

- [x] **Step 2: 运行测试确认失败**

Run: `python -m unittest tests.test_m4_auth_patch.M4AuthPatchTests.test_patches_auth_methods_and_adds_menu_diagnostics -v`

Expected: FAIL，原因是补丁仍返回单一临时 AIGC 产品。

- [x] **Step 3: 接入目录**

把：

```java
private static final String PRODUCT_MODULE_JSON = ...;
private static final String PC_MENUS_JSON = ...;
```

替换为：

```java
private static final String PRODUCT_MODULE_JSON = M4RecoveryCatalog.productModulesJson();
private static final String PC_MENUS_JSON = M4RecoveryCatalog.pcMenusJson();
```

不修改 v33 的渲染、URL、Web bridge、Web 首屏和 IM shape 相关方法。

- [x] **Step 4: 运行目标测试和完整测试**

Run:

```powershell
python -m unittest tests.test_m4_auth_patch -v
python -m unittest discover -s tests -v
```

Expected: 全部 PASS。

### Task 4: 生成 M4A 产物并完成静态验收

**Files:**
- Modify: `.context/m4-product-menu-evidence.md`
- Modify: `.context/current-status.md`
- Append: `.context/work-log.md`

- [x] **Step 1: 生成新 JAR**

Run:

```powershell
& .artifacts\tools\jdk8u492-b09\jdk8u492-b09\bin\javac.exe -cp .artifacts\tools\threadtear-gui-3.0.1-all.jar -d .artifacts\working\m4a-product-menu-v37\classes tools\m4_auth_patch\M4RecoveryCatalog.java tools\m4_auth_patch\M4AuthPatch.java
& .artifacts\tools\jdk8u492-b09\jdk8u492-b09\bin\java.exe -cp ".artifacts\working\m4a-product-menu-v37\classes;.artifacts\tools\threadtear-gui-3.0.1-all.jar" M4AuthPatch .artifacts\working\m1-02\App.jar .artifacts\working\m4a-product-menu-v37\App-m4a-v37-product-menu.jar
```

- [x] **Step 2: 验证产物**

验证 JAR 可读、原始 entry 集合保持、修改/新增类集合与 v33 一致；运行 `javap`/Java 探针确认九产品和菜单；重新计算 v33 JAR/ISO 哈希，必须仍为：

```text
24CCC59B18DC97EF05BBD57B46844B7B56F469E48BE1A85DA3A4649DC7957DF5
AE54073C1745E08164946814ABC949EB54894F67867705DD5F7143D09416C154
```

- [x] **Step 3: 更新上下文并验证**

记录每个产品的恢复 ID、菜单 code 来源、恢复入口边界和待运行 UI 验收项。运行：

```powershell
python C:\Users\m1591\.codex\skills\project-context-os\scripts\validate_context.py --project-root H:\项目\出海-AI
git diff --check
git status --short
```

- [x] **Step 4: 提交并推送**

```powershell
git add tools/m4_auth_patch/M4RecoveryCatalog.java tools/m4_auth_patch/M4AuthPatch.java tests/test_m4_auth_patch.py docs/superpowers/plans/2026-06-23-m4a-product-menu-recovery.md .context
git commit -m "feat: restore M4A product and menu catalog"
git push origin main
```
