# M5A 业务依赖分类清单

> 目标：在不扩大本地拦截、不伪造通用 `/prod-api/*` 的前提下，先把业务能力按依赖类型分层。这个文档是 M5A 第一版静态分类，后续每个菜单的真实点击、抓包和日志会在同一表上把“待验证”收敛为“可直连保留”或“需重建兼容后端”。

## 1. 当前结论

- M4B 已让授权、登录、产品、菜单和 Web 首屏门槛本地通过，但这不等于采集、群发、云手机、投屏、视频等业务动作已经恢复。
- 直接连接第三方平台或本机 native 工具的功能，优先保留原客户端逻辑，不应改成离线假数据。
- 依赖原 `app.xdxsoft.com/prod-api` 数据库、任务队列、云设备池、OSS 或支付/订单的功能，需要按接口逐项重建兼容后端。
- 当前第一版以静态证据分类，不执行真实群发、采集写入、支付、上传或云设备调度等有副作用动作。

## 2. 证据来源

- 反编译包结构：`.artifacts/decompiled/cfr-app-20260620-0215-annotated/com/sbf/main/` 下存在 `spide`、`rpa`、`kefu`、`mnq`、`cloud`、`ext`、`jxbrowser`、`video`、`im`、`mq`、`proxy`、`ads`、`ocx` 等业务包。
- native/工具资产：`data/app/adb/adb.exe`、`data/lib/ffmpeg.exe`、`data/lib/scrcpy-server.jar`、`data/tools/vecore/*.exe`、`data/tools/vecore/*.dll`、`data/lib/jxbrowser-win64-7.41.3.jar`、`data/lib/selenium-*.jar`、`data/lib/playwright-1.46.0.jar`、`data/lib/javacv/opencv/ffmpeg` 相关库。
- 已验证服务端接缝：`.context/seams.md` 记录 `/getInfo`、`/system/function_module/listmy/41`、`/api/v1/client/pc/menus`、`/prod-api/getInfo`、`/prod-api/getRouters`、`/prod-api/mnq/mnqAuthAccounts/mylist`、`/prod-api/system/dict/data/type/yes_no_1_0`。
- 既有运行证据：`C:\m2dump\m5-v33-host.log`、`C:\m2dump\m4-v40-host.log`、`C:\m2dump\m4-v40-offline-proxy.log` 已证明 Web 首屏和产品/菜单可进入；尚未证明真实业务动作。
- 字符串证据：`.context/string-map-sample-verification.md` 已确认 `https://trans.siyutui.com/bigdata/dpool`、`https://zhiwen.888005.xyz` 等外部服务入口；`.context/string-decode-report.md` 记录 `StartApp$5` 截图上传 OSS 和状态上报链路。

## 3. 依赖类型定义

| 类型 | 含义 | M5 处理方式 |
| --- | --- | --- |
| 客户端直连第三方 | 客户端通过浏览器、WebDriver、ADB、HTTP 或平台页面直接访问 WhatsApp/TikTok/Facebook/Google 等外部平台 | 保留原逻辑，做低风险只读/登录态验证 |
| 原后端代理 | 客户端请求原 `prod-api` 或 Java API，由服务端代发、签名、聚合或转发第三方 | 记录契约；失效后进入 M5B 兼容后端 |
| 原后端数据库 | 账号、任务、授权码、联系人、结果表、字典、路由等 CRUD 依赖原数据库 | 需要本地或外置兼容存储 |
| 云资源/算力 | 云手机池、云浏览器、OSS、队列、视频/AI 算力、远程任务执行 | 无法只靠客户端恢复；需逐项替换资源或重建服务 |
| native 依赖 | ADB、scrcpy、FFmpeg、VECore、OpenCV、JxBrowser、Selenium、Playwright 等本机工具/库 | 优先保留，验证本机可用性和路径 |

## 4. 模块分类矩阵

| 模块/能力 | 当前分类 | 证据 | 初步判断 | M5A 下一步 |
| --- | --- | --- | --- | --- |
| 登录、授权、产品门槛 | 原后端数据库 → 已本地兼容 | `/getInfo`、`/system/function_module/listmy/41`、`/api/v1/client/pc/menus` 已在 M4B 本地化 | 启动门槛可本地恢复，不代表业务数据可用 | 保持冻结，不再猜远端 token |
| Web 首屏 `/pc/aicloud/my` | 原后端数据库 + Web 前端 | `getInfo/getRouters/mylist/dict` 由定点 JS hook 补形状 | 首屏能显示空表；真实增删改查仍依赖后端 | 只记录页面请求，不泛化拦截 |
| WhatsApp 采集/筛选/群发/API/客服 | 客户端直连第三方 + 原后端任务/数据库 + IM | 菜单已恢复；包内有 `kefu`、`msg`、`im`、`mq`，也有浏览器/自动化依赖 | 采集/登录可能可直连；任务、联系人、群发状态和客服会话大概率需后端/本地库 | 先只打开页面和账号列表，不执行群发 |
| TikTok/FB/Instagram/X/TG 采集类 | 客户端直连第三方 + 浏览器自动化 + 可能后端存储 | 平台资源、菜单、`jxbrowser`、`selenium`、`playwright`、`proxy` 包存在 | 平台页面访问可保留；批量任务和结果保存需验证后端依赖 | 逐平台做只读打开和网络日志分类 |
| GEO/全球号码/海关数据/企业大数据 | 原后端代理 + 外部数据服务 | 已确认 `https://trans.siyutui.com/bigdata/dpool`；GEO 菜单含全球号码/地区/企业大数据 | 高概率依赖数据服务和服务端配额/查询代理 | 优先抓接口契约，不承诺客户端单独恢复 |
| AiCloud/AdsPower 指纹 | 第三方/本机服务 + 原后端数据库 | 菜单含 AiCloud/AdsPower 指纹；首屏已见 `/prod-api/mnq/mnqAuthAccounts/mylist` | 本机或第三方浏览器能力可保留；账号列表/授权码依赖后端存储 | 先验证本地浏览器/指纹工具是否可启动 |
| 云手机/模拟器/群控 | native + 云资源/设备池 + 原后端任务 | `mnq`、`ext/cloud`、`JCloudMobile*`、`JMNQ*`、`adb.exe`、`scrcpy-server.jar` | 本地 ADB/scrcpy 能力可保留；云设备池无法只靠客户端恢复 | 分离“本地设备连接”和“云设备调度” |
| 投屏/RTMP/视频处理 | native + 可能 OSS/上传 | `ext/rtmp`、`VideoStreamingServer`、`video`、`ffmpeg.exe`、`vecore`、OpenCV/JavaCV 资源 | 本地转码/剪辑/投屏组件可能可用；上传/云合成依赖服务 | 先跑本地工具存在性和无副作用启动 |
| OSS 截图/状态上报/版本更新 | 云资源 + 原后端上报 | `StartApp$5` 截图上传 OSS；v8 曾遇到 `gqkoss/qqkoss.oss-cn-hangzhou.aliyuncs.com` | 非核心业务，离线/断网时应 fail-open 或关闭噪音 | 保持不阻断主业务，必要时降级 |
| 支付、审核、套餐、企业认证 | 原后端数据库 + 支付服务 | `seams.md` 已记录支付宝、离线订单和 xpay 路径 | 已被产品目标排除为门槛，不作为恢复前提 | 保持绕过，不做支付恢复 |
| IM/消息/ActiveMQ/UDP | 原后端消息服务 + 本地 MQ | `data/lib/activemq-all`、`im.ip/im.port.udp`、`mq` 包；v33 修复 IM 配置形状 | 本地启动可过；真实客服/群发状态同步需消息服务 | 记录端口与 topic，再决定本地 MQ 兼容 |

## 5. 八系统第一版风险排序

| 优先级 | 系统 | 为什么先/后 |
| --- | --- | --- |
| P1 | WhatsApp | 用户验收截图最明确，菜单最多；但群发有副作用，必须只读打开、禁止真实发送。 |
| P1 | AiCloud/指纹首屏 | 已有 v33/v40 证据和具体 `/prod-api/mnq/...` 接口，可最快闭合“后端数据库 vs 本机工具”。 |
| P2 | GEO | 数据服务/后端代理风险高，越早抓契约越能判断 M5B 工作量。 |
| P2 | TikTok/Facebook/Instagram/X/TG | 多数可能走浏览器自动化；先验证能否打开平台和代理/指纹入口。 |
| P3 | 云手机/投屏/视频 | native 资产多，云资源不确定；需要更谨慎地拆本地能力和云调度。 |
| P3 | 客服/IM/群发执行 | 容易触发外发消息或任务写入，必须在只读契约清楚后再碰。 |

## 6. 下一轮验收规则

1. 只做只读/低副作用动作：打开页面、加载列表、读取空表、捕获网络请求和日志。
2. 不执行真实发送、批量采集、批量关注、付费、上传或云设备创建。
3. 每个菜单记录三件事：入口是否打开、请求去了哪里、失败是 401/403/空体/缺本机 native/缺云资源。
4. 能直连第三方的保持原客户端逻辑；只有明确依赖遗失后端的接口进入 M5B。
5. 继续避免通用 `/prod-api/*` 拦截；每个兼容响应必须由具体页面 chunk、Java 调用点或运行日志证明。
