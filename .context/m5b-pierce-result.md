# M5B-1 穿刺实验结果

实验时间：2026-06-25
目标：本地最小 SBFApi 桩，让 `com.sbf.main.cloud.spider.a.a(Long)` 按 `taskId` 取到一条伪造任务，并端到端进入 `cloud.spider.b + JxBrowser + whatsapp_users_lists.cnf`。
边界：未修改 `cloud.spider.b`、未修改 `.cnf`、未改 `libmytrpc` 对外签名；只做 `SBFApi.c(Long)` / `SBFApi.a(Long,int,String,Long)` 方法级桩和本地观测 harness。

## 1. SBFApi 接口契约

### 1.1 取任务：`SBFApi.c(Long)`

结论：`a(Long)` 的任务来源是 `SBFApi.c(taskId)`；本轮用 classpath shadow jar 做方法级桩，没有起完整队列/后端。

| 项 | 结论 |
| --- | --- |
| 调用方 | `.artifacts/decompiled/cfr-app-deobfuscated/com/sbf/main/cloud/spider/a.java:164` 动态调用解码为 `SBFApi.c(Long)` |
| 原始实现 | `.artifacts/decompiled/cfr-app-deobfuscated/com/sbf/util/http/SBFApi.java:1760-1771` 组装请求 JSON，调用 HTTP helper，`code == 200` 且有 `data` 时返回 `data` |
| HTTP 方法 / URL | 真实 URL/path 在 `SBFApi.a()` 和 helper 的字符串解密里；本轮未把远端 URL 明文完全解出。因 base URL 不是普通配置项，本轮采用允许的退路：方法级桩 |
| 本地桩位置 | `.artifacts/working/m5b1-local-sbfapi-stub/src/com/sbf/util/http/SBFApi.java` |
| 本地桩 jar | `.artifacts/working/m5b1-local-sbfapi-stub/sbfapi-stub.jar`，放在 `App.dll` 前面 |

`a(Long)` 实际读取到的响应 key，由桩日志反查：

| 层级 | 真实 key | 证据 |
| --- | --- | --- |
| 顶层 task payload | `task` | `M5B1_STUB_ENVELOPE_HAS key=task` / `OPTJSON key=task call=1` |
| 顶层 spider config | `spider` | `M5B1_STUB_ENVELOPE_HAS key=spider` / `OPTJSON key=spider call=2` |
| config | `code` | `M5B1_STUB_CONFIG_OPTSTRING key=code call=1 valueKind=whatsapp_users_lists` |
| config | `homeUrl` | `call=2 valueKind=https://www.google.com`; `call=3` 也会再读一次 |
| config | `injectionjs` | `call=4 valueKind=len:17558` |
| config | `postApis` | `call=5 valueKind=` |
| config | `sipderJson` | `call=6 valueKind=https://www.google.com` |
| config | `hookurls` | `call=7 valueKind=[{"path":""}]` |
| config | `steps` | `call=8 valueKind=len:899` |
| task/data | `taskConfig` | `M5B1_STUB_DATA_OPTSTRING key=taskConfig call=1 valueKind={}` |
| task/data | `spiderParams` | `call=2 valueKind=len:224`，这里必须放实际参数值，`b.getSpiderParams()` 从这里读 |

本轮最小任务字段：

```json
{
  "task": {
    "taskConfig": "{}",
    "spiderParams": [
      {"code": "googSite", "value": "google.com"},
      {"code": "areaCode", "value": "+1"},
      {"code": "pltCode", "value": "facebook.com"},
      {"code": "keywords", "value": "soccer jersey"}
    ]
  },
  "spider": {
    "code": "whatsapp_users_lists",
    "homeUrl": "https://www.google.com",
    "injectionjs": "<from cnf>",
    "hookurls": [{"path": ""}],
    "steps": "<from cnf>"
  }
}
```

说明：`.cnf` 第 1 步会拼 `https://www.` + `googSite` + `/ncr`，所以 `googSite` 实际喂 `google.com`，不是完整 URL。

### 1.2 状态回写：`SBFApi.a(Long,int,String,Long)`

| 项 | 结论 |
| --- | --- |
| 调用方 | `.artifacts/decompiled/cfr-app-deobfuscated/com/sbf/main/cloud/spider/a.java:163`、成功结束处也会回写 |
| 原始实现 | `.artifacts/decompiled/cfr-app-deobfuscated/com/sbf/util/http/SBFApi.java:1741-1752` 组装状态 JSON 并调用 HTTP helper |
| 成功判定 | 调用方不读取返回值；异常被原实现吞掉。因此本地桩 no-op 并打印日志即可 |
| 本轮证据 | `M5B1_STUB_STATUS taskId=20260625011 status=1 message= count=null` |

### 1.3 base URL 与其它远端接口

| 项 | 结论 |
| --- | --- |
| base URL | `SBFApi.a()` 返回混淆常量，不是普通本地配置；未发现可直接改到 `127.0.0.1` 的配置入口 |
| 本轮方式 | 采用方法级桩，classpath shadow `com.sbf.util.http.SBFApi` |
| `cloud.spider.b` 其它 SBFApi 调用 | 本轮进入 `b` 后未观察到额外 `SBFApi` 远端接口调用；阻断发生在 JxBrowser/JS 注入执行层 |

## 2. 桩实现与执行证据

本轮实际跑进了 `a(Long)` 和 `cloud.spider.b`：

| 证据 | 含义 |
| --- | --- |
| `M5B1_BROWSER_HOST_INJECTED class=com.sbf.main.jxbrowser.k` | JxBrowser 宿主已注入 runner |
| `procspider=20260625011` | `a(Long)` 开始处理该 taskId |
| `M5B1_STUB_GET_TASK taskId=20260625011` | `a(Long)` 已通过 `SBFApi.c(Long)` 取任务 |
| `lblNewLabel===开始处理任务:whatsapp_users_lists` | 进入 `cloud.spider.b` |
| `lblNewLabel===【5】秒后执行JS任务第1步/第2步/第3步` | `.cnf` 三个 step 已被调度 |
| `M5B1_PROXY_USED socks5://***@154.6.235.211:12324` | harness 设置了 socks5 代理系统属性和 Authenticator |

但页面没有跳到 Google。观测线程反射 JxBrowser `Browser O` 后，每 5 秒读取 `url/title/body`，关键证据：

```text
M5B1_OBS tick=1 url=http://appdemo/?t=... title=appdemo
state={"href":"chrome-error://chromewebdata/","title":"appdemo","main":false,"recaptcha":false,...}
text=无法访问此网站 找不到 appdemo 的服务器 IP 地址 ... ERR_NAME_NOT_RESOLVED

M5B1_OBS tick=6 url=http://appdemo/?t=... title=appdemo
state={"href":"chrome-error://chromewebdata/","title":"appdemo","main":false,"recaptcha":false,...}
```

同时 stderr 有 JxBrowser 回调线程异常：

```text
Exception in thread "Browser Thread: ..." java.lang.BootstrapMethodError
  at com.sbf.main.jxbrowser.g.IA(Unknown Source)
  ...
ERROR Failed to process task.
```

判断：本地桩已满足 `a(Long)` 取任务契约，但 JxBrowser 初始页/注入回调层异常后，`.cnf` step 没能实际把页面导航到 Google。继续处理这个问题会进入 JxBrowser 宿主/注入执行层修复，超出本轮“只做取任务桩 + 跑一条”的边界。

## 3. 三个问题结论

| 问题 | 答案 | 证据 |
| --- | --- | --- |
| ① Google 是否打开并返回结果？ | 不能。已进入 JxBrowser 分支，但未打开 Google。 | 观测日志一直是 `url=http://appdemo/...`、`href=chrome-error://chromewebdata/`、`ERR_NAME_NOT_RESOLVED`，未出现 `google.com` 或搜索结果 DOM。 |
| ② 是否弹 reCAPTCHA？ | 没有观察到。 | 观测日志 `recaptcha:false`，页面未到 Google；日志仅出现 `.cnf` 对 `iframe[title="reCAPTCHA"]` 的检测，但未检测到验证码 iframe。 |
| ③ `spider.postData` 是否往 `spider_data` 落至少一条真实行？ | 没有。 | SQLite 检查 `.artifacts/working/m5b1-local-sbfapi-stub/run/data/app/data/data/db_spider_data_whatsapp_users_lists.data`，`spider_data count 0`。 |

## 4. 本轮总判

M5B-1 的 SBFApi 本地取任务桩已跑通到 `a(Long) -> cloud.spider.b -> .cnf step 调度`，但未跑通真实采集；当前阻断点是 JxBrowser/JS 注入执行层的 `BootstrapMethodError` 及初始 `appdemo` 错误页未被 `.cnf` 导航替换。

一句话：纯“本地 SBFApi 取任务桩”不足以端到端跑通一条真实采集；下一步若继续，需要单独处理 JxBrowser 宿主/注入回调异常，使 `.cnf` step 能真实导航到 Google。

---

# M5B-2 穿刺实验结果：完整客户端内触发 + JxBrowser 代理修正

## 1. 只读确认

### 1.1 `appdemo` 内部 host / URL scheme 注册点

| 项 | 结论 |
| --- | --- |
| 完整启动器 | `.artifacts/working/m4b-v50-local-launcher-src/HuoChaiAILocalLauncher.cs` 用 `data/jdk/bin/java.exe`、工作目录 `data/app`、`-Djava.io.tmpdir=data/app/temp -jar App.dll` 拉起完整客户端 |
| JxBrowser Engine 构建点 | `.artifacts/decompiled/cfr-app-deobfuscated/com/sbf/main/jxbrowser/g.java:290` 通过 `EngineOptions.newBuilder(...).addScheme(Scheme.HTTP, new b()).addScheme(Scheme.HTTPS, new b()).addScheme(Scheme.of(...), new b())` 注册 URL 请求拦截 |
| Engine 创建点 | `.artifacts/decompiled/cfr-app-deobfuscated/com/sbf/main/jxbrowser/g.java:341` 调用 `Engine.newInstance(options.build())` |
| URL 请求处理类 | `.artifacts/decompiled/cfr-app-deobfuscated/com/sbf/main/jxbrowser/b.java:40` 实现 `InterceptUrlRequestCallback`；`:62-101` 从 `params.urlRequest().url()` 取 URL，按客户端内部资源规则写入 `UrlRequestJob`，否则 `Action.proceed()` |
| 本轮验证 | 完整客户端内 `appdemo` 已可被处理，日志出现 `M5B2_ALL_BROWSERS tick=0 ... [http://appdemo/?t=...&lang=zh_cn | appdemo]`，不再是 M5B-1 的 `ERR_NAME_NOT_RESOLVED:appdemo` 阻断 |

结论：`appdemo` 不是靠系统 DNS/hosts 注册，而是靠完整客户端在 JxBrowser `EngineOptions.addScheme(...)` 上注册的 HTTP/HTTPS/custom scheme 拦截器 `com.sbf.main.jxbrowser.b`。脱离完整启动链的精简 harness 没拉起这套拦截，才会落到 Chromium DNS 解析并报 `ERR_NAME_NOT_RESOLVED`。

### 1.2 JxBrowser 代理正确设置方式

| 项 | 结论 |
| --- | --- |
| 正确入口 | JxBrowser/Chromium 代理要通过 `Engine.proxy()` 或 `Profile.proxy()` 设置，不是 Java `socksProxyHost` / `Authenticator` 系统属性 |
| 反编译证据 | `.artifacts/decompiled/cfr-app-deobfuscated/com/sbf/main/jxbrowser/g.java:374-381` 已有 `object2.proxy().config(CustomProxyConfig.newInstance(...))` 和 `object2.network().set(AuthenticateCallback.class, ...)` |
| 认证回调证据 | `.artifacts/decompiled/cfr-app-deobfuscated/com/sbf/main/jxbrowser/g.java:934` 附近的 `AuthenticateCallback` 处理只在 `params.isProxy()` 时走代理认证 |
| API 形状 | `Engine.proxy().config(CustomProxyConfig.newInstance("socks=socks5://154.6.235.211:12324", "<local>"))`，再用 `Engine.network().set(AuthenticateCallback.class, callback)` 注册认证回调 |
| 重要限制 | JxBrowser 底层 Chromium 对 SOCKS 用户名/密码认证支持有限；本轮可证明代理规则已设入 JxBrowser，但日志中未观察到 `AuthenticateCallback` 被触发 |

本轮实际设置证据：

```text
M5B2_JXBROWSER_PROXY_SET engineId=1958518304 rules=socks=socks5://154.6.235.211:12324 exceptions=<local>
```

## 2. 桩实现与执行方式

本轮没有修改 `cloud.spider.b`、`.cnf`、`libmytrpc`，也没有重建队列后台。采用临时 Java agent 只做运行时投喂和观测：

| 项 | 方式 |
| --- | --- |
| 取任务桩 | 拦截 `SBFApi.c(Long)`，仅对 `taskId=20260625011` 返回 M5B-1 已验证的伪造任务 |
| 状态回写桩 | 拦截 `SBFApi.a(Long,int,String,Long)`，记录日志并返回成功 |
| 代理设置 | hook `com.sbf.main.jxbrowser.g` 的 `Engine.newInstance(...)` 返回点，调用 JxBrowser `engine.proxy().config(...)` |
| 数据观测 | 仅在接收端 `JSpiderCloude` 做运行时观测/落库日志，不改 spider 执行核心 |
| 启动方式 | 用 v50 完整启动器环境的 `data/app`、`data/jdk/bin/java.exe`、`-jar App.dll` 启动完整客户端 |

关键执行证据：

```text
M5B2_PATCHED method=SBFApi.c(Long)
M5B2_PATCHED method=SBFApi.a(Long,int,String,Long)
M5B2_STUB_GET_TASK taskId=20260625011
M5B2_STUB_GET_TASK_RETURNED keys=task,spider spiderCode=whatsapp_users_lists
M5B2_RECEIVER_REGISTERED masterMapSize=1
M5B2_JXBROWSER_PROXY_SET engineId=1958518304 rules=socks=socks5://154.6.235.211:12324 exceptions=<local>
```

## 3. 三个问题结论

| 问题 | 答案 | 证据 |
| --- | --- | --- |
| ① 进入完整客户端后，Google 是否真打开并返回结果？ | **Google 真打开了，但没有返回搜索结果。** | 日志出现 `M5B2_NAVIGATION_CANDIDATE url=https://www.google.com`，浏览器枚举出现 `https://www.google.com/`，随后 `.cnf` 第 1 步 `M5B2_DOHREF_CANDIDATE url=https://www.google.com/ncr`，浏览器枚举出现 `https://www.google.com/ncr`。但后续第 3 步一直等待 `div[id='main']`，从 `0/60` 到 `59/60` 都未命中，未出现搜索结果 DOM。 |
| ② 是否弹 reCAPTCHA？ | **没有观察到 reCAPTCHA。** | 观测日志中 `recaptcha:false`，没有 `M5B2_RECAPTCHA_DETECTED`，浏览器 URL/title 中也没有 captcha/recaptcha 页面证据。 |
| ③ `spider.postData` 是否往 `spider_data` 落至少一条真实行？ | **没有。** | 全程没有 `M5B2_POSTDATA_INSERT`；包目录下未生成 `db_spider_data_whatsapp_users_lists.data`；SQLite 外部检查为 `DB_EXISTS False SIZE 0`。 |

## 4. 本轮阻断点

完整客户端已消除 M5B-1 的两个运行层问题：`appdemo` 不再 `ERR_NAME_NOT_RESOLVED`，也未再出现 `BootstrapMethodError(jxbrowser.g.IA)`。JxBrowser 代理规则也已通过 JxBrowser API 写入。

新的阻断点是：`.cnf` 已把某个 JxBrowser Browser 导航到 `https://www.google.com/` 和 `https://www.google.com/ncr`，但没有进入搜索结果页；后续步骤一直等不到 `div[id='main']`，因此没有触发 `spider.postData`，也没有真实落库。

一句话结论：M5B-2 在完整客户端内证明了 `a(Long) -> cloud.spider.b -> JxBrowser -> google.com/ncr` 可以跑到真实浏览器导航层；但这条任务仍未跑通真实采集，当前缺口是 Google 搜索结果页未产生/未被 `.cnf` 识别，`postData` 未落 `[REAL]` 行。
