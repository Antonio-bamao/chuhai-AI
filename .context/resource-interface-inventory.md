# M3 Phase 4 资源解密入口盘点

> 当前文件记录 Phase 4 Step 1：定位加密资源、明文资源和加载/解密入口。下一步再实现批量解密与关键词命中清单。

## 资源来源

- JAR 来源：`.artifacts/working/m1-02/App.jar`
- 外部资源目录：`data/app/`
- 注释源码树：`.artifacts/decompiled/cfr-app-20260620-0215-annotated`
- 字符串映射：`.artifacts/analysis/string_map.json`

## JAR 内加密资源

| JAR 路径 | 大小 | SHA-256 | 文件头 | 当前判断 |
| --- | ---: | --- | --- | --- |
| `master.html` | 183300 | `be59de8a045de6fc8bb2dda026e6bf37c3bedef5b2901ff63591531d6f2a7d6c` | `d9 7b 30 c0 ...` | 加密/压缩资源 |
| `msg.html` | 146279 | `10773c24791edb46f25750bdde889d15512efeb04ae76a7acb63dc9a548b3830` | `d9 7b 30 c0 ...` | 加密/压缩资源 |
| `fm.js` | 1763 | `55f78dd6a4d56da03d6efd16207b46c76b93708ccd4348fbb40fe8740dfbbf75` | `d9 7b 30 c1 ...` | 加密/压缩资源 |
| `country_ips.json` | 1407 | `0e7abe1f450cc968444e240f5671204f345b823accc5a9236c03376a88ccf8c9` | `d9 7b 30 c1 ...` | 加密/压缩资源 |
| `html/Login.html` | 13965 | `1e7af9dfe662f7e6a8f1a3e3a7308ba240ff842dc959eb111093a4ff7fc6a258` | `d9 7b 30 c1 ...` | 加密/压缩资源 |
| `html/product-selector.html` | 7584 | `eca2dfcb2f41588638f5ebb3a6fa5f859104cfdd6bd9c19a46948ffdcbd41541` | `d9 7b 30 c1 ...` | 加密/压缩资源 |
| `html/ClawWorkspace.html` | 7135 | `da469037778e058d69b36d5e43b94190f193ebf3bbffd8b9556aa4f3d1a05682` | `d9 7b 30 c1 ...` | 加密/压缩资源 |

直接 UTF-8 读取上述资源不会命中 `token/login/expire/vip/套餐/有效期/授权/license/pay/订单/支付`，因为内容仍处于加密/压缩态。

## 外部明文资源

| 路径 | 状态 | 备注 |
| --- | --- | --- |
| `data/app/i18.cnf` | 明文 JSON | 大型国际化/菜单文本资源。 |
| `data/app/splash.html` | 明文 HTML | 启动页资源。 |
| `data/app/res/spider/*.cnf` | 明文 JSON | 21 个采集规则文件，当前不需要资源解密。 |
| `data/app/res/pagebanner/*.cnf` | 明文空文件 | 21 个 2 字节占位文件。 |

外部 `spider/*.cnf` 中只有 3 个 WhatsApp 规则命中关键词 `token`，语义来自业务采集参数/令牌字段，不是启动授权接缝。

## 加载 / 解密入口

| 资源 | 调用点 | 行号 | 说明 |
| --- | --- | --- | --- |
| `/master.html` -> `/temp/master.html` | `com.sbf.util.FileHelper.a()` | `FileHelper.java:341-345` | 从 JAR 读 `/master.html`，写出本地 `StartApp.a + /temp/master.html`。 |
| `/msg.html` -> `/temp/msg.html` | `com.sbf.util.FileHelper.a()` | `FileHelper.java:351-355` | 从 JAR 读 `/msg.html`，写出本地 `StartApp.a + /temp/msg.html`。 |
| `/country_ips.json` | `com.sbf.util.e.a()` | `e.java:27-31` | 读取资源后构造 `JSONArray`。 |
| `/fm.js` | `com.sbf.main.jxbrowser.g.a(...)` | `g.java:417-420` | 读取并缓存指纹注入脚本。 |
| `/lib/msg.html` | `com.sbf.main.jxbrowser.MiJava.getMsgUrl()` | `MiJava.java:2443-2449` | 返回本地 `StartApp.a + /lib/msg.html`，存在才返回。 |
| `/temp/master.html` | `com.sbf.main.kefu.ws.a.<init>()` | `a.java:51-53` | 客服/WS 浏览器加载本地 master 页面。 |
| `/html/Login.html` | `com.sbf.main.ext.j2026.JLoginHTML.<init>()` | `JLoginHTML.java:37-41` | 登录页面资源。 |
| `/html/product-selector.html` | `com.sbf.main.ext.j2026.JProductSelectorHtml.<init>()` | `JProductSelectorHtml.java:27-31` | 产品选择器页面资源。 |
| `/html/ClawWorkspace.html` | `com.sbf.main.ext.j2026.ClawWorkspace.<init>()` | `ClawWorkspace.java:27-31` | 本地工作台页面资源。 |

## 解密机制线索

- `com.sbf.main.ext.j2026.ui.e.c(String)` 在 `e.java:214-226` 通过 `e.c(o.class, path)` 打开资源并按 UTF-8 合并文本。
- `com.sbf.main.ext.j2026.ui.e.c(Class, String)` 在 `e.java:395-396` 把 `Class.getResourceAsStream(...)` 返回值包装为 `new ch.r(...)`。
- `ch.r` 是资源解密/解压包装流：`r.java:27-71` 读取 16 字节头；当头部匹配内部 magic 时进入 `InflaterInputStream(..., new ua(this))` 或 `new zq(...)`。
- 文件头 `d9 7b 30 c0/c1 ...` 与 `ch.r` 的 magic 分支吻合，下一步应复用或复刻 `ch.r` 流读取逻辑，而不是按普通文本读取。

## Step 2 只读解密结果

- 工具：`tools/resource_decoder/ResourceDecoder.java`
- 测试：`tests/test_resource_decoder.py`
- 输出目录：`.artifacts/analysis/resources-decrypted/`
- 方式：用 `JarFile` 只读打开 `App.jar` 条目，再直接复用 `ch.r` 解密/解压流；不调用 `StartApp.main`，不写回 JAR。
- 安全边界：拒绝绝对路径、反斜杠、空路径段、`.` 和 `..`，并再次校验输出路径仍位于指定输出目录。

运行命令：

```powershell
& '.artifacts\tools\jdk8u492-b09\jdk8u492-b09\bin\javac.exe' `
  -cp '.artifacts\working\m1-02\App.jar' `
  -d '.artifacts\analysis\resource-decoder-classes' `
  'tools\resource_decoder\ResourceDecoder.java'

& '.artifacts\tools\jdk8u492-b09\jdk8u492-b09\bin\java.exe' `
  -cp '.artifacts\analysis\resource-decoder-classes;.artifacts\working\m1-02\App.jar' `
  ResourceDecoder `
  '.artifacts\working\m1-02\App.jar' `
  '.artifacts\analysis\resources-decrypted'
```

| 解密后路径 | 字节数 | SHA-256 | 内容验证 |
| --- | ---: | --- | --- |
| `master.html` | 183284 | `8e5633167d59e8de5b0ec94134d167b6f48d07e809e53f60870fe022a47551de` | `<html lang="en">` |
| `msg.html` | 146263 | `405110bf0062425763f7bf20954d738a54b9783e2cb13d2a936743d9832c9879` | `<html lang="en">` |
| `fm.js` | 10210 | `4e090e0b27db514f4b1a78ea0c79d0f195991b4617b9cd0fd407deaf8c414b02` | JavaScript IIFE |
| `country_ips.json` | 4406 | `3c47d6c1f842033524d14af98215ae9018f587de4e1d714a24e3cbac1bd3c6f4` | 可解析 JSON 数组，30 项 |
| `html/Login.html` | 64781 | `f475d1f2f660ef3eefba0a5b6801ec006fb7e397d75fa34c63d5bf4c5cae15fd` | `<!DOCTYPE html>`，`zh-CN` |
| `html/product-selector.html` | 30296 | `6a7288e59ceb73cfb4b03e56724d66acbdeca632971fc6fe9e0383d2d0959a5a` | `<!DOCTYPE html>`，`zh-CN` |
| `html/ClawWorkspace.html` | 35602 | `b2ba02b4bcaccf7516cd78e26f89f7024d6449661ce278ae5523fdd3d330d8e3` | `<!DOCTYPE html>`，`zh-CN` |

工具同时生成 `manifest.json`，记录源 JAR、资源路径、解密后字节数和 SHA-256，供后续关键词扫描与复核。

## Phase 4 下一步

## Step 3 明文关键词定位

- `html/Login.html` 命中 23 行，精确入口为 `1582-1591`：短信/邮箱表单提交到 `htmljava`；邮箱路径对应 `JLoginHTML.java:56-73` 和 `JLoginHTML$4.java:42-56`。
- `html/product-selector.html` 的直接“有效期”命中为 `243`、`256`、`270`；完整门槛位于 `234-299`，按 `status`、`remainingDays` 决定进入、过期、禁止使用或未开通。
- `product-selector.html:422-429` 从 `getMySoftModules` 取得产品数组；Java 对应 `HtmlJava.java:67-70`、`HtmlJava$2.java:29-31`、`SBFApi.java:3373-3379`，接口路径 `/system/function_module/listmy/41`。
- `master.html:5`、`msg.html:91` 的 `pay/vip` 仅来自 base64 图片串，排除。
- `fm.js`、`country_ips.json`、`html/ClawWorkspace.html` 无目标关键词。
- 外部 spider CNF 的 `token` 仍归类为业务采集字段，不是启动授权。

有效命中和 Java 调用链已补入 `.context/seams.md`，均精确到 `文件:行`。Phase 4 已完成。

## M4 交接

1. 先设计 `/getInfo` 与 `/system/function_module/listmy/41` 两份最小兼容本地 JSON。
2. 优先选择 `SBFApi.h(String)`、`SBFApi.C()` 方法级短路，保持登录窗口、产品选择器、`StartApp$1$3` 和 `JSBFMain` 生命周期不变。
3. 在复制 JAR 上 patch，并以离线 VM 验证“登录页 -> 产品选择器 -> 主界面”，每个 patch 步骤独立提交和可回滚。
