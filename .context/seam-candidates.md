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
| 调用入口确认 | `AdsCallback.getAction(String)` 第 191-192 行与 `MiJava.getAction(String)` 第 1553-1554 行均使用 `String.contains(...)` 判断参数是否包含 `get_current_token` 或 `getLoingIsToken`，命中后将原始 `object` 直接传给静态 `StartApp.f(String): String`。因此入参可以是包含 action 片段的完整 URL，而不是只有 action 名。 |
| URL 拼接确认 | `StartApp.f(String)` 第 385 行在原始 URL 已含 `?` 时追加 `&RT=<currentTimeMillis>`，否则追加 `?RT=<currentTimeMillis>`；`DTHelper.a(...)` 第 314 行随后以同样规则追加 `rdtime=<currentTimeMillis>`，最终由 `Request.Builder.url(String)` 使用。 |
| 补充确认 | `DTHelper.b(...)` 是通用 OkHttp 包装器，返回 `result/message/code/cookies`；`com.sbf.main.jxbrowser.n` 是带 `expireTime` 的本地状态缓存，`n.c()` 会调用 `DTHelper.b(...)` 刷新 `result/header/data/expireTime`。 |
| 刷新触发确认 | `n.a(long expireTime)` 在首次写入缓存时创建 `Timer` 与 `n$1`，调用 `Timer.schedule(task, 3600000, 36000000)`；即 1 小时后首次执行、之后每 10 小时调用 `n.c()`。原始字节码确认 `n.a(String url,String body)` 只创建一个实例并将参数分别写入 `c/d`，CFR 显示的第二个 `new n()` 是反编译栈复制错误。 |
| UI/启动影响 | 已确认 `expireTime` 是 token `header` 缓存 TTL：`n.a()` 在到期前 60 秒判定为不可复用；缺失/0 时默认缓存约 1 小时。它不直接控制 `JLoginHTML`、`JProductSelectorHtml` 或 `ClawWorkspace` 分支，bridge 也只返回 `header`。账号授权期限使用另一字段 `periodTime`。 |
| 风险 | 不应在 `DTHelper` 通用网络层 patch，否则高概率误伤业务联网；`n` 缓存比 `DTHelper` 更接近 token 状态，但不是启动授权判定。当前仍缺实际域名/路径样本和 `getLoingIsToken` 的具体语义。 |
| 下一步 | 在隔离环境抓包时记录实际 URL；M3 将该方法列为 token 缓存接缝而非免登录主接缝。 |
| 回滚点 | 未 patch；回滚只需删除分析产物。 |

## 候选 5：`com.sbf.main.StartApp$1.a(JSONObject)`

| 字段 | 内容 |
| --- | --- |
| 文件 | `com\sbf\main\StartApp$1.java` |
| 行段 | 31-118 左右 |
| 当前判断 | 高优先级启动授权接缝候选。 |
| 已解明文 | `data`、`sf`、`token`、`time`、`periodTime`、`result`、`code`、`msg`、`@ExpiredTime` |
| 原逻辑观察 | 校验软件开通列表和 token；根据 `periodTime` 显示过期/临期提示；随后调用 `SBFApi.h(token)`，仅当 `result.code == 200` 时创建并显示 `JProductSelectorHtml`，再关闭 `JLoginHTML`。 |
| 时效结论 | `periodTime` 的过期与临期分支只显示提示，没有 `return` 或退出；本地代码仍继续走产品选择器路径。`JReadme` 还会显示规范化后的 `periodTime`。 |
| 风险 | 文案中的“切换为普通版”可能由服务端返回角色/功能配置实现，不应只删除弹窗就假定已去时效。需继续追踪 `roles`、`overdue` 和产品配置。 |
| 下一步 | M3 精确记录软件开通列表、token、`SBFApi.h(...).result.code` 三个实际启动门槛，并反查普通版功能降级来源。 |
| 回滚点 | 未 patch。 |

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

1. `JLoginNew.vS(...)` 116 条与 `ClawWorkspace.vv(...)` 4 条均已解析；主要为 UI 装配、窗口调度、线程启动和异常打印，未出现新的授权网络判定点。
2. `StartApp.f(String)` 的调用者、入参来源、时间参数拼接与 `n.c()` 定时刷新触发点已确认；仍需在隔离环境记录实际域名/路径。
3. `expireTime` 与 `periodTime` 已区分；仍需追踪普通版降级由 `roles`、`overdue` 还是套餐配置驱动。
4. 需要在隔离环境中抓包验证哪些路径真的出网。
