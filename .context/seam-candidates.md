# 接缝候选草稿

> M3 预备输入。本文只记录静态分析候选，不代表已经验证为最终 patch 点；当前阶段不修改客户端产物。

## 当前证据来源

- 源码树：`H:\项目\出海-AI\.artifacts\decompiled\cfr-app-20260620-0215`
- 字符串映射：`H:\项目\出海-AI\.artifacts\analysis\string_map.json`
- 授权候选：`H:\项目\出海-AI\.artifacts\analysis\auth_string_candidates.json`
- 解码脚本：`H:\项目\出海-AI\tools\decode_java_strings.py`

## 候选 1：`com.sbf.main.StartApp.f(String)`

| 字段 | 内容 |
| --- | --- |
| 文件 | `com\sbf\main\StartApp.java` |
| 行段 | 367-413 左右 |
| 当前判断 | 高优先级候选，疑似授权/用户状态请求与响应解析点。 |
| 已解明文 | `ecode`、`ename`、`loginName`、`userName`、`roles`、`timer`、`token`、`result`、`header`、`data`、`expireTime` |
| 原逻辑观察 | 方法组装一组用户/角色/token JSON，编码后发起动态调用；随后解析返回 JSON 的 `result/header/data/expireTime`，并根据条件返回字符串或 `null`。 |
| 风险 | 该方法包含大量 `StartApp.Sy(...)` 动态调用，尚未完全还原真实被调用方法。直接 patch 风险高。 |
| 下一步 | 先解析 `StartApp.Sy(...)` bootstrap 目标，确认网络调用、缓存读写、时间判断分别落在哪些调用上。 |
| 回滚点 | 未 patch；回滚只需删除分析产物。 |

## 候选 2：`com.sbf.main.StartApp.i()`

| 字段 | 内容 |
| --- | --- |
| 文件 | `com\sbf\main\StartApp.java` |
| 行段 | 280-299 左右 |
| 当前判断 | 中优先级候选，疑似用户状态读取与定时任务入口。 |
| 已解明文 | `user`、`tenantCode`、`userId`、`gs` |
| 原逻辑观察 | 从全局 JSON `m` 中取 `user` 对象，读取租户、用户 ID 和状态字段；若状态允许，创建 `Timer` 并每 10 秒调度一次任务。 |
| 风险 | 可能是登录后心跳或状态刷新，也可能是业务侧定时任务；需结合 `StartApp$5` 和动态调用还原。 |
| 下一步 | 跟踪 `StartApp$5` 以及被调度方法，判断是否与授权时效或业务心跳相关。 |
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

1. 需要还原 `StartApp.Sy(...)` 的目标类/方法/签名，至少覆盖 `StartApp.f/i/k` 中的高价值调用。
2. 需要反查 `StartApp$5`、`StartApp$6`、`JProductSelectorHtml$a` 等内部类。
3. 需要确认 `expireTime` 判断是服务端返回解析、缓存写入，还是 UI 展示。
4. 需要在隔离环境中抓包验证哪些路径真的出网。
