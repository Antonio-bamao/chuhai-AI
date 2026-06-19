# 接缝候选草稿

> M3 预备输入。本文只记录静态分析候选，不代表已经验证为最终 patch 点；当前阶段不修改客户端产物。

## 当前证据来源

- 源码树：`H:\项目\出海-AI\.artifacts\decompiled\cfr-app-20260620-0215`
- 字符串映射：`H:\项目\出海-AI\.artifacts\analysis\string_map.json`
- 授权候选：`H:\项目\出海-AI\.artifacts\analysis\auth_string_candidates.json`
- 静态字符串解码脚本：`H:\项目\出海-AI\tools\decode_java_strings.py`
- bootstrap 调用解码脚本：`H:\项目\出海-AI\tools\decode_bootstrap_calls.py`
- bootstrap 调用映射：`H:\项目\出海-AI\.artifacts\analysis\bootstrap_map.json`
- `StartApp` 高价值动态调用候选：`H:\项目\出海-AI\.artifacts\analysis\startapp_bootstrap_candidates.json`

## 候选 1：`com.sbf.main.StartApp.f(String)`

| 字段 | 内容 |
| --- | --- |
| 文件 | `com\sbf\main\StartApp.java` |
| 行段 | 367-413 左右 |
| 当前判断 | 高优先级候选，疑似授权/用户状态请求与响应解析点。 |
| 已解明文 | `ecode`、`ename`、`loginName`、`userName`、`roles`、`timer`、`token`、`result`、`header`、`data`、`expireTime`、`get_current_token`、`getLoingIsToken` |
| 已还原动态调用 | `MD5Util2.c(String)`、`AESCBCHelper.a(String)`、`DTHelper.b(String,String): JSONObject`、`System.currentTimeMillis()`、`HashMap.get/containsKey/remove/put(...)`、`com.sbf.main.jxbrowser.n.a()/b()` |
| 原逻辑观察 | 方法先用 `MD5Util2.c` 规范化入参，再查 `HashMap` 与 `n` 缓存；未命中时组装用户/角色/token JSON，经 `AESCBCHelper.a` 和 Base64 编码后传给 `DTHelper.b(String,String)`；随后解析返回 JSON 的 `result/header/data/expireTime`，并按 `expireTime` 写回 `n` 缓存。 |
| 调用入口确认 | `AdsCallback.getAction(String)` 第 191-192 行与 `MiJava.getAction(String)` 第 1553-1554 行均在 action 等于 `get_current_token` 或 `getLoingIsToken` 时，将原始 `object` 直接传给静态 `StartApp.f(String): String`。这说明入参来源是 JS bridge action，而不是调用者预先组装好的 URL。 |
| 补充确认 | `DTHelper.b(...)` 是通用 OkHttp 包装器，返回 `result/message/code/cookies`；`com.sbf.main.jxbrowser.n` 是带 `expireTime` 的本地状态缓存，`n.c()` 会调用 `DTHelper.b(...)` 刷新 `result/header/data/expireTime`。 |
| 风险 | 不应在 `DTHelper` 通用网络层 patch，否则高概率误伤业务联网；`n` 缓存比 `DTHelper` 更接近授权状态。当前已确认 action 入口，但第 385 行最终 URL 拼接结果和 `getLoingIsToken` 的具体语义仍待确认。 |
| 下一步 | 还原 `StartApp.f(String)` 第 385 行的 URL 拼接常量与参数；同时跟踪 `n.c()` 的刷新触发点。 |
| 回滚点 | 未 patch；回滚只需删除分析产物。 |

## 候选 2：`com.sbf.main.StartApp.i()`

| 字段 | 内容 |
| --- | --- |
| 文件 | `com\sbf\main\StartApp.java` |
| 行段 | 280-299 左右 |
| 当前判断 | 中优先级候选，疑似用户状态读取与定时任务入口。 |
| 已解明文 | `user`、`tenantCode`、`userId`、`gs` |
| 已还原动态调用 | `RobotHelper.a()`、`RobotHelper.b()`、`Timer.schedule(TimerTask, 0, 10000)` |
| 原逻辑观察 | 从全局 JSON `m` 中取 `user` 对象，读取租户、用户 ID 和状态字段；若状态允许，创建 `Timer` 并每 10 秒调度 `StartApp$5`。`StartApp$5.run()` 会截图、上传 OSS 路径 `scpres/...png`，再调用 `SBFApi.a(int,int,String)` 上报。 |
| 风险 | 当前更像登录后截图/状态上报任务，不是优先授权接缝；如果误改可能影响业务风控或远端状态。 |
| 下一步 | 暂降优先级；后续只在抓包阶段确认其是否产生非必要上报。 |
| 回滚点 | 未 patch；回滚只需删除分析产物。 |

## 候选 3：`com.sbf.main.ext.j2026.JLoginHTML`

| 字段 | 内容 |
| --- | --- |
| 文件 | `com\sbf\main\ext\j2026\JLoginHTML.java` |
| 行段 | 39-40 左右 |
| 当前判断 | 登录 UI 入口候选，不是优先 patch 点。 |
| 已解明文 | `登录系统`、`/html/Login.html` |
| 原逻辑观察 | 构造 HTML 登录窗口，加载本地登录页面。 |
| 风险 | 直接删 UI 会破坏状态链路，不符合 ADR-0001。 |
| 下一步 | 只作为调用链定位入口，继续向登录成功后的状态流追踪。 |
| 回滚点 | 未 patch。 |

## 候选 4：`com.sbf.main.ext.j2026.ClawWorkspace`

| 字段 | 内容 |
| --- | --- |
| 文件 | `com\sbf\main\ext\j2026\ClawWorkspace.java` |
| 行段 | 27-35 左右 |
| 当前判断 | 鉴权通过后的工作区入口候选，不是授权判定本身。 |
| 已解明文 | `ClawWorkspace`、`/html/ClawWorkspace.html` |
| 原逻辑观察 | 构造工作区窗口，加载工作区 HTML。 |
| 风险 | 直接跳到该类可能绕过必要初始化；需确认 `JProductSelectorHtml$a` 参数来源与状态初始化。 |
| 下一步 | 反查 `new ClawWorkspace(...)` 和 `JProductSelectorHtml$a` 的创建路径。 |
| 回滚点 | 未 patch。 |

## 当前不作为 patch 点的区域

- `JLoginNew` / `JLoginHTML` 登录界面本身：保留 UI 外表，避免删除登录模块。
- `libmytrpc*.dll` / VECore / FFmpeg：native 边界，不修改。
- `StartApp.Sy(...)`、`JLoginNew.vS(...)`、`ClawWorkspace.vv(...)`：当前只作为动态调用还原目标，不直接 patch。

## M3 前置缺口

1. 需要继续还原 `JLoginNew.vS(...)`、`ClawWorkspace.vv(...)` 的目标类/方法/签名。
2. `StartApp.f(String)` 的调用者与入参来源已确认；仍需还原最终请求 URL，并追踪 `com.sbf.main.jxbrowser.n.c()` 的刷新触发点。
3. 需要确认 `expireTime` 判断是服务端返回解析、缓存写入，还是 UI 展示。
4. 需要在隔离环境中抓包验证哪些路径真的出网。
