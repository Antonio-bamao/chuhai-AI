# 字符串解密报告

## M2 字符串解密进展

- 时间：2026-06-20 02:29
- 输入源码树：`H:\项目\出海-AI\.artifacts\decompiled\cfr-app-20260620-0215`
- 解码脚本：`H:\项目\出海-AI\tools\decode_java_strings.py`
- 输出清单：`H:\项目\出海-AI\.artifacts\analysis\string_map.json`
- 授权候选：`H:\项目\出海-AI\.artifacts\analysis\auth_string_candidates.json`
- 原始资产修改：未修改

## 已覆盖的解码器

| 解码器 | 调用点数量 | 说明 |
| --- | ---: | --- |
| `JSetupDialog$JLoginNew.N(...)` | 550 | 依赖调用者 `className + methodName` 的字符串解密函数。 |
| `JTestFrame$JLoginNew$2.k(...)` | 451 | 与 `N(...)` 结构相同，但 key/常量不同。 |
| `JLoginHTML$h.v(...)` | 93 | j2026 登录/工作区相关字符串解密函数，与 `N(...)` 结构相同但 key/常量不同。 |
| `c$c$a.f(...)` | 382 | `DTHelper` 等 HTTP 路径的字段/header 字符串。 |
| `Keepapi$AiBotHelper$1.C(...)` | 261 | `DTHelper` 辅助 HTTP 路径的 header/协议字符串。 |
| `MiJava$MiJava$181.W(...)` | 987 | `jxbrowser.n` 等缓存/授权状态路径的字段字符串。 |
| `d$JTrayDialog.n(...)` | 403 | `StartApp$5` 等启动/定时任务路径的字符串。 |
| `d$MiJava$188$1.B(...)` | 1,346 | `jxbrowser` 相关大量业务/UI 路径字符串，后续按需筛选。 |
| `AdsCallback$SGAICloudPanel$2.I(...)` | 126 | 广告浏览器 JS bridge action 与脚本片段，结构与现有 AES 表解码器相同。 |

脚本实际抓到 4,599 条调用记录；当前 4,599 条均成功解码。授权相关候选扩展到 372 条，可作为 M3 检索输入。

## 入口类明文样本

`StartApp.java` 已能看到以下明文：

| 行 | 调用点 | 明文 |
| ---: | --- | --- |
| 75 | `com.sbf.main.StartApp<clinit>` | `D:/aimirror/` |
| 77 | `com.sbf.main.StartApp<clinit>` | `aimirrorsystem` |
| 79 | `com.sbf.main.StartApp<clinit>` | `--aimirrorsystem` |
| 285 | `com.sbf.main.StartAppi` | `user` |
| 286 | `com.sbf.main.StartAppi` | `tenantCode` |
| 287 | `com.sbf.main.StartAppi` | `userId` |
| 383 | `com.sbf.main.StartAppf` | `token` |
| 388 | `com.sbf.main.StartAppf` | `result` |
| 389 | `com.sbf.main.StartAppf` | `header` |
| 390 | `com.sbf.main.StartAppf` | `data` |
| 395 | `com.sbf.main.StartAppf` | `expireTime` |

## 授权相关候选

按 `auth/license/login/token/expire/pay/order/user/tenant/role/header/result/data/password/product` 以及中文关键词筛选出 362 条候选，已写入：

```text
H:\项目\出海-AI\.artifacts\analysis\auth_string_candidates.json
```

初步观察到的高价值字段包括：

- `user`
- `tenantCode`
- `userId`
- `token`
- `result`
- `header`
- `data`
- `expireTime`
- `mnq_license_num`
- `ads_browsers_license_num`
- `RememberPassword`
- `登录系统`
- `/html/Login.html`
- `ClawWorkspace`
- `/html/ClawWorkspace.html`
- `进入`
- `购买`
- `下载`

扩展 HTTP/jxbrowser 解码后新增确认：

- `DTHelper` 返回包装字段：`result`、`message`、`code`、`cookies`、`name`、`value`、`domain`、`expiresAt`、`persistent`、`secure`
- `DTHelper` header/请求字段：`Authorization`、`User-Agent`、`cookie`、`headers`、`application/json`
- `jxbrowser.n` 授权缓存字段：`result`、`header`、`data`、`expireTime`
- `StartApp$5` 上报路径片段：`scpres/`、`.png`
- `AdsCallback.getAction` 与 `MiJava.getAction` 的同构 action：`get_current_token`、`getLoingIsToken`

## 重要限制

- 当前脚本覆盖 `N(...)`、`k(...)` 与 `JLoginHTML$h.v(...)` 这一类静态字符串解密。
- `JLoginNew.vS(...)`、`ClawWorkspace.vv(...)` 等是 invokedynamic/bootstrap 形态，不是同一类“密文直接变明文”调用，后续需单独处理。
- 部分记录因调用点推断不准或本身不是文本，会出现低可读性结果；M3 使用时应优先引用高可读性字段和入口类上下文。

## M2 bootstrap 动态调用解码

- 时间：2026-06-20 02:47
- 解码脚本：`H:\项目\出海-AI\tools\decode_bootstrap_calls.py`
- 全量输出：`H:\项目\出海-AI\.artifacts\analysis\bootstrap_map.json`
- `StartApp` 候选输出：`H:\项目\出海-AI\.artifacts\analysis\startapp_bootstrap_candidates.json`
- 输出统计：73,600 条 bootstrap 调用记录；73,597 条成功解码；3 条保留错误原因。
- 高价值 `StartApp` 候选：19 条，覆盖 `StartApp.f(String)`、`StartApp.i()`、`StartApp.k(String)`。
- `JLoginNew.vS(...)`：116 条，全部成功解析，主要目标为 Swing/AWT UI 方法、语言/字体辅助、窗口显示与启动线程。
- `ClawWorkspace.vv(...)`：4 条，全部成功解析，目标为 `EventQueue.invokeLater`、尺寸缩放辅助与 `Exception.printStackTrace`。
- 全量 3 条解码错误位于 `JPLTStatusBrowser` 与 `Weta365Helper` 的空 key 调用，不在当前授权/登录/工作区关键路径。

`StartApp.f(String)` 中已还原的关键调用目标：

| 行 | 目标 |
| ---: | --- |
| 369 | `com.sbf.util.MD5Util2.c(String): String` |
| 370-371 | `HashMap.get(...)`、`com.sbf.main.jxbrowser.n.a()`、`n.b()`，疑似本地缓存读取与有效性判断 |
| 379-385 | `System.currentTimeMillis()`、`AESCBCHelper.a(String): String`，参与请求体编码和时间参数组装 |
| 386 | `com.sbf.util.http.DTHelper.b(String, String): JSONObject` |
| 396-399 | `HashMap.containsKey/remove/put(...)` 与 `n.a(...)` 链式写入，疑似响应缓存 |

`StartApp.i()` 中已还原 `RobotHelper.a()/b()` 和 `Timer.schedule(TimerTask, 0, 10000)`；`StartApp.k(String)` 还原为 `HashMap.get(...)` 包装器。

## M2 HTTP/缓存边界观察

- `DTHelper.b(String,String)` 是 `DTHelper.a(..., false)` 的薄包装；底层使用 OkHttp 构造 GET/POST/PUT/DELETE 请求、执行 `Call.execute()`，并统一包装为 `JSONObject`。
- `DTHelper` 统一返回结构包含 `result/message/code/cookies`，因此它更像通用网络边界，不宜作为授权专属 patch 点。
- `com.sbf.main.jxbrowser.n` 持有 `a:String`、`b:long expireTime`、`c/d:String`，`a()` 用 `System.currentTimeMillis()` 判断缓存是否接近过期，`c()` 会再次调用 `DTHelper.b(...)` 刷新 `result/header/data/expireTime`。
- `StartApp$5.run()` 调用 `RobotHelper.a(false,null)` 截图，拼出 `scpres/...png`，上传到 `ALLOSSHelper.a(...)`，再调用 `SBFApi.a(int,int,String)` 上报；该 10 秒任务更像截图/状态上报，不是当前优先授权接缝。

## M2 JS bridge token action 入口

- `AdsCallback.getAction(String)` 与 `MiJava.getAction(String)` 的分支结构和 action 明文完全一致。
- 两处均用 `String.contains(...)` 判断参数是否包含 `get_current_token` 或 `getLoingIsToken`，命中时调用 `StartApp.f(String): String`。
- bootstrap 描述符均为 `(Ljava/lang/String;)Ljava/lang/String;`，且 CFR 调用点把原始 `object` 直接作为唯一参数传入。
- `StartApp.f(String)` 在原字符串后追加 `RT=<currentTimeMillis>`；`DTHelper.a(...)` 再追加 `rdtime=<currentTimeMillis>`，最终字符串直接传给 `Request.Builder.url(...)`。
- 当前可确认输入是包含 token action 的 URL/参数字符串；本地资源未检索到实际域名/路径样本。
- `jxbrowser.n` 在缓存写入时以 `Timer.schedule(task, 3600000, 36000000)` 启动刷新，即 1 小时后首次调用 `n.c()`，之后每 10 小时刷新一次。
- `n.a(String,String)` 的原始字节码为单次 `new`、`dup`、写入 `c/d` 后返回；CFR 的“双重 new”输出不应作为对象归属证据。

## M2 `expireTime` / `periodTime` 影响结论

- token 返回中的 `expireTime` 只控制 `StartApp.f(String)` 的 `header` 缓存；在到期前 60 秒失效，缺失/0 时使用约 1 小时默认 TTL。
- 固定的 1 小时首次、10 小时周期刷新不由 `expireTime` 决定。
- bridge 只把 `header` 返回给前端，未把 `expireTime` 暴露给 UI；未发现它控制登录或工作区窗口分支。
- 登录回调中的账号授权期限字段是 `periodTime`。它触发已过期/15 天内到期提示，并在 `JReadme` 中显示日期。
- `periodTime` 提示后代码仍继续请求初始化数据并创建 `JProductSelectorHtml`；真正的本地启动门槛是软件开通列表、token 和 `SBFApi.h(...).result.code == 200`。
- 详细证据见 `.context/expiretime-impact.md`。

## 下一步

1. M2 关键入口明文和 bootstrap 目标已达到可检索门槛，可转入 M3 授权接缝清单。
2. M3 继续追踪 `roles`、`overdue`、套餐/功能配置与“普通版”降级的关系。
3. 在隔离环境记录 token 请求的实际域名/路径和真实出网行为，不做 patch。

## 2026-06-21 M2 Phase 3 全量静态解密收口

- 候选清单：348 个 family、35,642 个 Unicode 密文调用点。
- 唯一同构静态 family：347 个；`a$5$0.Z` 因两个包内同名定义保留未决。
- 最终统一映射：35,644 条，其中：
  - `decoded_static`：34,418
  - `decoded_existing_plaintext`：2
  - `dynamic_dump_required`：775
  - `unsupported_shape`：449
- 原始 classfile 方法表纠正 CFR lambda/synthetic caller：2,640 条。
- 未决输出：`.artifacts/analysis/unresolved_string_calls.json`，1,224 条。
- 注释源码树：`.artifacts/decompiled/cfr-app-20260620-0215-annotated`，
  含 35,644 条稳定 `STRING_MAP` 注释；原始源码树哈希未改变。
- 20 条 URL、字段、UI、启动/登录样本全部与上下文一致，详见
  `.context/string-map-sample-verification.md`。

旧章节中的 4,599 条统计是 2026-06-20 的阶段性历史基线，不再代表当前
全量覆盖结果。
