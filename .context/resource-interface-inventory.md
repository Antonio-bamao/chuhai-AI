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

## Phase 4 下一步

1. 写一个只读资源解密工具，从 `App.jar` 中读取目标资源并经 `ch.r` 等价流程输出到 `.artifacts/analysis/resources-decrypted/`。
2. 对解密后的 HTML/JS/JSON/CNF 跑关键词：`token`、`login`、`expire`、`vip`、`套餐`、`有效期`、`授权`、`license`、`pay`、`订单`、`支付`。
3. 将命中结果精确到 `文件:行`，补回 `.context/seams.md` 的前端/资源接缝段。
4. 若解密工具无法稳定复刻，改用最小 Java harness 调用 `ch.r`，仍保持只读读取 JAR 资源、不运行 `App.jar` 主程序。
