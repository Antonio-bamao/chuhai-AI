# M3 授权接缝 / 接口清单

> 当前版本基于 M2 全量静态字符串解密、bootstrap 目标还原、20 条语义抽检和第一轮 offline critical dynamic dump。本文只定位接缝，不修改客户端产物。

## 证据输入

- 原始 CFR 源码树：`.artifacts/decompiled/cfr-app-20260620-0215`
- 注释源码树：`.artifacts/decompiled/cfr-app-20260620-0215-annotated`
- 统一字符串映射：`.artifacts/analysis/string_map.json`，35,644 条
- bootstrap 映射：`.artifacts/analysis/bootstrap_map.json`，73,600 条
- critical 动态 dump：`.artifacts/analysis/strings-critical.jsonl`，35 条，命中 3/5 critical family
- 动态 dump 摘要：`.artifacts/analysis/strings-critical-summary.json`
- 解密资源：`.artifacts/analysis/resources-decrypted/`，7 个 HTML/JS/JSON，清单见 `manifest.json`

## 总体结论

当前可定位的免登录启动主门槛在 `StartApp$1.a(JSONObject)`：

1. 登录回调 JSON 的 `sf` 必须被拆分后包含目标软件标识。
2. `data.token` 必须存在且长度大于 10。
3. `SBFApi.h(token)` 调 `/getInfo` 后返回的 `result.code` 必须为 `200`。
4. 通过后保存 `StartApp.m = result.data`，再创建 `JProductSelectorHtml`。
5. 产品选择页另调 `SBFApi.C()` 的 `/system/function_module/listmy/41`；前端按每个模块的 `status`、`remainingDays` 决定是否允许点击“进入系统”。
6. 允许进入后，产品选择回调 `StartApp$1$3.a(JSONObject)` 创建 `JSBFMain(...)` 并进入主界面。

`expireTime` 只影响 `StartApp.f(String)` 的 token/header 缓存 TTL，不是启动门槛。`periodTime` 只触发过期/临期提示和主界面展示，静态代码中没有在提示后阻断主流程。

因此 M4 不能只伪造 `/getInfo`：还必须提供可进入的产品模块数据，或在产品选择页对应 Java 接口层返回等价本地结果。

## 接缝 S0：登录页面 JS bridge 入口

| 字段 | 内容 |
| --- | --- |
| 级别 | 前端入口，高优先级证据 |
| 明文资源 | `.artifacts/analysis/resources-decrypted/html/Login.html` |
| 关键行 | `Login.html:1172-1191` 登录表单；`1582-1591` SMS/邮箱提交；`1872-1880` 记住账号初始化 |
| Java bridge | `com.sbf.main.ext.j2026.ui.HtmlJava` |
| Java 行 | `HtmlJava.java:38-45` 暴露 `doShowClawWorkspace`、`doLoginByEmail`、`doEnter`；`JLoginHTML.java:56-73` 校验邮箱/密码并调度登录任务；`JLoginHTML$4.java:42-56` 调 `SBFApi.k(email, passwd)`，成功后回调 `JLoginHTML` 外层监听器 |
| 登录接口 | `SBFApi.java:3358-3369`，路径 `/api/v1/adesk.ai/login`，请求字段 `email`、`passwd`、`machineCode`、`appId` |
| 结论 | `Login.html:1591` 是新版邮箱登录的精确前端入口，最终仍回到 S1 的登录成功回调。 |
| 异常点 | `Login.html:1585` 调 `htmljava.doLoginBySMS(...)`，但 `HtmlJava` 类与实际 classfile 均未暴露该方法；短信 tab 不是当前可靠主路径。 |
| M4 含义 | 不建议直接改 HTML 表单。S1/S2 的 Java 状态短路更集中，也能保留窗口与回调生命周期。 |

## 接缝 S1：登录回调启动门槛

| 字段 | 内容 |
| --- | --- |
| 级别 | 主接缝，高优先级 |
| 类 / 方法 | `com.sbf.main.StartApp$1.a(JSONObject): void` |
| 文件 | `.artifacts/decompiled/cfr-app-20260620-0215-annotated/com/sbf/main/StartApp$1.java` |
| 关键行 | 32-50、62-63、89-100 |
| 输入 | 登录成功回调 JSON，至少含 `data`、`sf`、`data.token`、`data.ucf` |
| 输出 / 状态 | 设置 `StartApp.l` token、`StartApp.m` 用户/权限数据、`StartApp.q` ucf、显示产品选择器 |
| 原逻辑 | `sf` 为空或不含目标软件标识时清空 token 并提示；token 缺失或长度不足时清空 token 并提示；`SBFApi.h(token).result.code == 200` 后继续。 |
| 已解字段 | `data`、`sf`、`token`、`result`、`code`、`data`、`ucf`、`msg`、`periodTime` |
| bootstrap 证据 | `StartApp$1.java:62 -> com.sbf.util.http.SBFApi.h(String): JSONObject`；`StartApp$1.java:97-100 -> set JProductSelectorHtml visible / dispose JLoginHTML` |
| 拟改动方向 | M4 若 patch，应优先在此方法内构造等价成功状态或短路失败分支，保持后续 `JProductSelectorHtml`、`JSBFMain` 初始化链路不变。 |
| 不建议 | 不改 `DTHelper` 通用网络层；不删除登录窗口；不直接跳 `ClawWorkspace`。 |
| 回滚点 | 原始 `App.dll` / `App.jar` 备份；若只改该类，回滚 `com/sbf/main/StartApp$1.class`。 |

## 接缝 S2：`SBFApi.h(token)` 初始化信息接口

| 字段 | 内容 |
| --- | --- |
| 级别 | 主接口，高优先级 |
| 类 / 方法 | `com.sbf.util.http.SBFApi.h(String): JSONObject` |
| 文件 | `.artifacts/decompiled/cfr-app-20260620-0215-annotated/com/sbf/util/http/SBFApi.java` |
| 关键行 | 351-354 |
| 输入 | token |
| 远端路径 | `/getInfo` |
| 输出 | JSONObject，调用方读取 `result.code` 和 `result.data` |
| 原逻辑 | 拼接基础域名与 `/getInfo`，带 token 请求远端，返回 JSON。 |
| 调用方 | `StartApp$1.a(JSONObject)` 第 62 行是当前启动路径唯一确认调用。 |
| 拟改动方向 | 若选择接口级短路，应只让 `h(String)` 在启动初始化路径返回本地成功 JSON；需保证 `result.data.user`、`periodTime`、`roles`、`ucf` 等字段足够 `JSBFMain` 消费。 |
| 风险 | `SBFApi` 是大范围业务 API 类，方法级短路比改通用 `DTHelper` 安全，但仍需确认无其他业务流程依赖 `h(String)` 的真实远端副作用。 |
| 回滚点 | `com/sbf/util/http/SBFApi.class`。 |

## 接缝 S3：产品选择器到主界面

| 字段 | 内容 |
| --- | --- |
| 级别 | 次主接缝，中高优先级 |
| 类 / 方法 | `com.sbf.main.StartApp$1$3` 实现 `JProductSelectorHtml$a` |
| 文件 | `.artifacts/decompiled/cfr-app-20260620-0215-annotated/com/sbf/main/StartApp$1$3.java` |
| 关键行 | 35-44、49-87、94-97 |
| 输入 | 产品选择器传入的 JSONObject 或 String |
| 输出 / 状态 | 创建 `ClawWorkspace` 或 `JSBFMain`，显示主界面，关闭 `JProductSelectorHtml` |
| 原逻辑 | `a(String)` 分支创建 `ClawWorkspace`；`a(JSONObject)` 分支读取产品主题/菜单字段，创建 `new JSBFMain(StartApp.y, StartApp.m, StartApp.l, string)`。 |
| 已解字段 | `code`、`sid`、`fid`、`name`、`displayName`、`themeStyle`、多组菜单/按钮颜色字段 |
| bootstrap 证据 | `StartApp$1$3.java:36-44 -> new/set ClawWorkspace, dispose selector`；`85-97 -> JSBFMain setBounds/setVisible, dispose selector` |
| 拟改动方向 | M4 不应首先 patch 这里；它依赖 `StartApp.m/l` 已正确初始化。可作为 S1/S2 patch 后的验证观察点。 |
| 风险 | 直接跳过产品选择可能缺失 `JSBFMain` 需要的产品配置 `g`，导致主界面菜单或主题异常。 |
| 回滚点 | `com/sbf/main/StartApp$1$3.class`。 |

## 接缝 S3A：产品模块开通 / 有效期门槛

| 字段 | 内容 |
| --- | --- |
| 级别 | 第二主门槛，高优先级 |
| 明文资源 | `.artifacts/analysis/resources-decrypted/html/product-selector.html` |
| 数据入口 | `product-selector.html:422-429` 调 `htmljava.getMySoftModules(...)`，读取返回 JSON 的 `data` 为 `productsData` |
| Java bridge | `HtmlJava.java:67-70`；`HtmlJava$2.java:29-31` |
| 远端接口 | `SBFApi.java:3373-3379`，`SBFApi.C()` 请求 `/system/function_module/listmy/41` 并返回 `result` |
| 前端门槛 | `product-selector.html:234-280`：只有 `status == 0 || status == 1` 进入已开通分支；`status == 1 && remainingDays < 0` 显示“过期”且没有进入按钮；其他已开通状态显示“禁止使用”。 |
| 未开通分支 | `product-selector.html:283-297` 显示“未开通”，按钮只打开官网。 |
| 进入回调 | `product-selector.html:365-377` 将选中的完整 product JSON 传给 `htmljava.doEnter(...)`，随后进入 S3。 |
| 关键字段 | `code`、`status`、`remainingDays`、`expirationTime`、`name`、`displayName`、主题色和菜单配置。 |
| 结论 | 这是资源解密后新增确认的真实授权/套餐接缝。仅让 S1 `/getInfo` 成功，仍可能停在“过期 / 禁止使用 / 未开通”。 |
| 拟改动方向 | M4 本地成功数据至少要让目标 product 的 `status` 与 `remainingDays` 可进入，并保留 S3 构造主界面所需的产品配置。优先考虑 `SBFApi.C()` 方法级本地返回。 |
| 回滚点 | `com/sbf/util/http/SBFApi.class`；若只改前端资源，回滚 `html/product-selector.html`，但当前不推荐先改资源。 |

## 接缝 S4：主界面权限 / 功能配置消费

| 字段 | 内容 |
| --- | --- |
| 级别 | 权限配置接缝，中优先级 |
| 类 / 方法 | `com.sbf.main.JSBFMain.<init>(...)` 及后台刷新任务 |
| 文件 | `.artifacts/decompiled/cfr-app-20260620-0215-annotated/com/sbf/main/JSBFMain.java`、`c$1.java`、`JSBFMain$37.java` |
| 关键行 | `JSBFMain.java:380-424`；`c$1.java:47-70`；`JSBFMain$37.java:45-70` |
| 输入 | `StartApp.m`、`StartApp.q/ucf`、产品选择结果、token |
| 输出 / 状态 | 用户名、租户、认证状态、`periodTime`、`overdue`、`roles`、模拟器/广告浏览器许可数量、若干功能开关 |
| 原逻辑 | 构造主界面时读取 `user`、`certified`、`EAdmin`、`periodTime`、`overdue`、`roles`；`roles` 会置位 `enterprise_user_self_open`、`tz_show_rpa_center`、`aaa_ai_video_source` 等功能标志。`ucf` 提供 `mnq_license_num`、`ads_browsers_license_num` 等数值开关。 |
| 拟改动方向 | M4 的本地成功 JSON 应填充足够宽松的 `roles`/`ucf`，而不是在主界面到处 patch 单个功能判断。 |
| 风险 | 若 S1/S2 只返回最小 `code=200`，主界面可能进入但功能数量、菜单和角色缺省，表现为“普通版”或按钮缺失。 |
| 回滚点 | 优先回滚 S1/S2；不建议先改 `JSBFMain.class`。 |

## 接缝 S5：token/header 缓存与 JS bridge

| 字段 | 内容 |
| --- | --- |
| 级别 | Token 状态接缝，中优先级 |
| 类 / 方法 | `com.sbf.main.StartApp.f(String): String`；`com.sbf.main.jxbrowser.n` |
| 文件 | `.artifacts/decompiled/cfr-app-20260620-0215-annotated/com/sbf/main/StartApp.java`、`com/sbf/main/jxbrowser/n.java` |
| 关键行 | `StartApp.java:367-413` |
| 输入 | 前端 bridge 原始 action URL，包含 `get_current_token` 或 `getLoingIsToken` |
| 输出 | 返回 `header` 字符串；缓存 `result/header/data/expireTime` |
| 原逻辑 | bridge 入口用 `String.contains(...)` 命中 action 后传给 `StartApp.f(String)`；该方法请求远端并按 `expireTime` 写入 `n` 缓存。 |
| 已解字段 | `roles`、`token`、`result`、`header`、`data`、`expireTime`、`get_current_token`、`getLoingIsToken` |
| 结论 | 不是免登录主门槛；更像登录后 Web UI 取 token/header 的状态接口。 |
| 拟改动方向 | M4 第一阶段暂不 patch。若后续业务 UI 因 token 刷新失败异常，再在此方法或 `n` 缓存层做本地稳定返回。 |
| 风险 | 改 `DTHelper.b(...)` 会误伤业务联网；改 `StartApp.f(...)` 可能影响前端业务接口授权 header。 |
| 回滚点 | `com/sbf/main/StartApp.class`、`com/sbf/main/jxbrowser/n.class`。 |

## 接缝 S6：支付 / 订单 / 认证入口

| 字段 | 内容 |
| --- | --- |
| 级别 | 支付回传边界，M5 前复核 |
| 类 / 方法 | `com.sbf.main.JSBFMain.a(com.sbf.main.f)` 及 jxbrowser 支付监听类 |
| 文件 | `.artifacts/decompiled/cfr-app-20260620-0215-annotated/com/sbf/main/JSBFMain.java`、`com/sbf/main/jxbrowser/b/a$*.java` |
| 关键行 | `JSBFMain.java:1014`、`1022`、`1164`、`1182`；`jxbrowser/b/a$1.java:111-112`、`a$2.java:25-32`、`a$3.java:30-50` |
| 已解路径 | `/pc/alipay/enterpriseAuth`、`/pc/alipay/personal/auth`、`/pc/userPayofflineOrder/my`、`https://ws.wandange.com/xpay/order/offlinepay/transform`、支付宝 `queryQRStatus` |
| 原逻辑 | 主界面动作中存在认证、离线支付订单与支付宝状态查询入口。 |
| 拟改动方向 | 不属于断网启动主门槛；M5 抓包时分类确认哪些仍会出网，必要时只短路授权/支付状态回传，不影响业务请求。 |
| 风险 | 过早屏蔽支付域或统一网络层会破坏业务联网，并可能产生异常重试。 |
| 回滚点 | 暂无 patch；记录为后续抓包分类目标。 |

## 动态 dump 缺口

- 已命中 critical family：`JSetupDialog$JLoginNew.N`、`JLoginHTML$h.v`、`JTestFrame$JLoginNew$2.k`。
- 未命中 critical family：`JProductSelectorHtml$d.L`、`g$JMainMaster$4.r`。
- 当前判断：未命中两族对应产品选择器 / 主界面之后的运行阶段。由于 S1/S2/S3 已有静态与 bootstrap 证据，M3 接缝清单可以先推进；M4 前若需要更高把握，可在离线 VM 中触发到产品选择器/主界面后只跑 high/critical 扩展 dump。

## Phase 4 明文关键词扫描结论

扫描词：`token`、`login`、`expire`、`vip`、`套餐`、`有效期`、`授权`、`license`、`pay`、`订单`、`支付`。

| 资源 | 命中行数 | 分类 |
| --- | ---: | --- |
| `html/Login.html` | 23 | 登录表单、邮箱登录 bridge、记住账号；隐私文本中的“支付处理”不是授权接缝。 |
| `html/product-selector.html` | 3 个直接“有效期”命中；相关门槛位于 `234-299` | 有效授权/套餐门槛，形成 S3A。 |
| `master.html` | 1 | `pay/vip` 仅出现在 base64 图片串，假阳性。 |
| `msg.html` | 1 | `pay/vip` 仅出现在 base64 图片串，假阳性。 |
| `fm.js`、`country_ips.json`、`html/ClawWorkspace.html` | 0 | 无目标关键词。 |

外部 `data/app/res/spider/*.cnf` 的 `token` 命中属于 WhatsApp 等业务采集参数，不参与启动授权。Phase 4 没有发现本地 `license` 文件或独立离线授权校验。

## M4 最小 patch 候选排序

1. `StartApp$1.a(JSONObject)` 内构造或保持成功状态，优先保证 `StartApp.l/m/q` 完整。
2. `SBFApi.h(String)` 方法级本地返回 `/getInfo` 等价成功 JSON，供 S1 原逻辑自然通过。
3. `SBFApi.C()` 方法级本地返回 `/system/function_module/listmy/41` 等价产品数组，确保目标模块 `status`、`remainingDays` 可进入，并补齐产品主题/菜单字段。
4. 如产品选择回调仍缺字段，再补 `StartApp$1$3.a(JSONObject)` 的产品配置输入；先不直接跳 `ClawWorkspace`。
5. `StartApp.f(String)` / `jxbrowser.n` 只作为 token/header 后续修补点。

## 待验证问题

1. 授权服务实际返回体字段全集：尤其 `result.data.user`、`roles`、`ucf`、`periodTime` 的真实组合。
2. `sf` 中目标软件标识的准确字符串：当前静态只确认使用 `StringHelper.i(sf)` 拆分后逐项调用 `StartApp.b(String)` 判断。
3. `JProductSelectorHtml$d.L` 和 `g$JMainMaster$4.r` 在产品选择/主界面后的实际明文输出。
4. 支付/订单路径在免登录启动后是否自动触发，还是仅由用户点击触发。
5. 业务联网是否依赖 S5 的 token/header 刷新结果。
6. `/system/function_module/listmy/41` 的真实 product JSON 字段全集；M4 需以 S3/S3A 已读字段构造最小兼容样本。
