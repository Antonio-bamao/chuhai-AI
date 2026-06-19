# `expireTime` / `periodTime` 影响追踪

> M2 静态分析结论。本文区分 token 缓存 TTL 与账号授权期限；未修改客户端产物。

## 结论摘要

- token 接口返回的 `expireTime` 只参与 `StartApp.f(String)` 的本地缓存有效性判断和后台刷新，不直接控制登录窗口、产品选择器或 `ClawWorkspace` 启动分支。
- 登录响应中的 `periodTime` 才是账号授权期限。它会触发“已过期”或“15 天内到期”提示，并在 `JReadme` 中显示到期日期。
- `periodTime` 的本地判断不会中断成功启动路径。过期提示之后，代码仍继续解析用户信息、请求初始化数据并创建 `JProductSelectorHtml`。
- 文案声称“切换为普通版”，但当前静态代码未发现由 `periodTime` 直接改写角色或功能开关的分支。实际降级更可能由服务端返回的 `roles`、`overdue`、套餐/功能配置决定；这是基于现有调用链的推断，仍需隔离环境验证。

## 1. token 缓存 `expireTime`

### 写入路径

`StartApp.f(String)`：

1. 对原始 URL 做 MD5，作为 `HashMap<String, n>` 的缓存键。
2. 缓存未命中或即将过期时请求远端。
3. 从返回对象读取 `header` 和 `expireTime`。
4. 创建 `n` 缓存对象，保存 URL、请求体、`header` 和 `expireTime`。
5. 返回 `header` 给 `get_current_token` / `getLoingIsToken` 的 JS bridge 调用者。

关键位置：

- `StartApp.java:367-371`：缓存读取。
- `StartApp.java:393-400`：读取 `header/expireTime` 并写回缓存。
- `AdsCallback.java:191-192`、`MiJava.java:1553-1554`：bridge 入口。

### 有效性判断

`n.a()` 的条件等价于：

```text
currentTimeMillis >= expireTime - 60_000
```

满足时返回 `true`，表示缓存已经过期或距离过期不足 60 秒；`StartApp.f` 不再返回旧 `header`，而是重新请求。

若服务端未返回有效的正数 `expireTime`，`n.a(long)` 会保留构造时的默认值：

```text
currentTimeMillis + 3_600_000
```

因此缺失或为 `0` 的 `expireTime` 会退化为约 1 小时缓存，而不是立即失效。

### 后台刷新

- 每个缓存对象首次写入时注册定时任务。
- 首次延迟 1 小时，之后每 10 小时调用 `n.c()`。
- `n.c()` 使用保存的 URL/请求体重新请求，只在新 `header` 非空时更新 `header` 和 `expireTime`。
- 定时周期固定，不由 `expireTime` 数值决定。

### 对 UI / 启动的实际影响

- Java 层没有把 `expireTime` 传给 Swing/JxBrowser UI。
- bridge 返回的是 `header` 字符串，不是包含 `expireTime` 的 JSON。
- `expireTime` 只能间接影响 UI 后续请求使用旧 token 还是刷新后的 token。
- 未发现 `expireTime` 触发 `JLoginHTML`、`JProductSelectorHtml`、`ClawWorkspace` 显隐或跳转的分支。

## 2. 账号授权期限 `periodTime`

### 启动回调路径

`StartApp$1.a(JSONObject)` 是登录成功后的回调：

1. 校验当前软件是否在服务端返回的开通列表中。
2. 读取登录 token。
3. 读取 `time`、`periodTime`，将 `periodTime` 补成当天 `23:59:59` 后解析。
4. 根据当前/服务端时间与到期时间的差值显示提示。
5. 继续调用 `SBFApi.h(token)` 获取初始化数据。
6. `result.code == 200` 时创建并显示 `JProductSelectorHtml`，随后关闭 `JLoginHTML`。

### 本地提示分支

- 已过期：显示“您的授权有效期已过期，将切换为普通版。”
- 剩余不超过 15 天：按天、小时或分钟显示续费提醒，并将剩余时间替换进 `@ExpiredTime`。
- 两个分支均没有 `return`、`System.exit` 或窗口跳转改写；执行会继续进入产品选择器路径。

### UI 展示

- `JSBFMain` 初始化时再次读取并规范化 `periodTime`。
- `JReadme.java:92-93` 将 `JSBFMain.d`（即规范化后的 `periodTime`）显示为绿色标签。
- `overdue` 字段在 `JSBFMain` 中被读取但返回值未保存，当前未形成可见的本地判定。

## 3. 启动分支的真实门槛

当前静态证据显示，启动成功更直接依赖：

1. 登录回调中的 `sf`/软件开通列表包含当前软件。
2. token 非空且长度大于 10。
3. `SBFApi.h(token)` 返回对象存在且 `result.code == 200`。
4. 随后的产品选择器回调创建 `ClawWorkspace`。

`periodTime` 位于第 2、3 步之间，但只产生提示；token 缓存 `expireTime` 则属于进入 UI 后的 bridge 请求缓存，不是启动门槛。

## 4. 对 M3 的影响

- `expireTime` 不应单独作为“免登录/去时效”的启动 patch 点。
- `n.a()` / `n.c()` 可列为 token 缓存接缝，但优先级低于登录回调和初始化结果判定。
- M3 应重点记录：
  - `StartApp$1.a(JSONObject)` 的软件开通列表、token 和 `SBFApi.h(...).result.code` 分支。
  - `JProductSelectorHtml$a` 到 `ClawWorkspace` 的创建路径。
  - 服务端返回的 `roles`、`overdue`、套餐/功能配置如何决定普通版与已授权外表。

