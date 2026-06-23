# 当前状态

- 项目背景：这是用户本人早年设计并打包的火柴AI客户端。源码、原服务端、云服务器、数据库和管理后台控制权均已遗失；没有可用旧虚拟机、历史账号或历史凭据。当前工作是在本地恢复自己的客户端，不再把取得原服务端真实 token 作为前提。
- 当前阶段：M4B 免操作启动与运行验收。v33 是冻结的稳定技术基线，不是最终 M4 交付；v40 候选在其上增加九产品、76 菜单、原始 logo/icon 和本地自动登录，不回写或覆盖 v33 产物。
- 已完成：M1-M3 完成；v33 已实现本地登录 JSON、`getInfo`、产品初始化、Web token bridge、`getRouters`、Web 首屏必要响应形状，并验证 AiCloud 在线首屏可渲染。8 个可进入系统内部 code 已确认：`whatsapp/tiktok/facebook/instagram/twitter/telegram/geo/wskefu`；独立站采用高置信 `aishope` 候选并保持未开通。
- 已完成 M4A：v40 运行时已显示 8 个可进入系统、1 个未开通独立站、原包九产品 logo；WhatsApp 主界面已显示 11 项恢复侧边栏及原包菜单 icon。`41/aigc/C28500001/C28500002` 临时目录已撤除。
- 进行中 M4B：`StartApp$3.run()` 保留登录前初始化，只在创建 `JLoginHTML` 前调用本地登录成功链；无需账号、密码或点击登录即可进入产品选择器。`StartApp$1` 对不存在的登录窗增加 null guard，避免自动登录后的 dispose NPE。
- 下一步：在可控 VM 或具备管理员权限的环境补一次物理断网/网卡禁用验收，并制作最终可双击分发包；随后进入 M5A 业务依赖分类。
- 阻塞项：当前会话无管理员权限，无法创建临时 Windows 出站防火墙规则。进程级 HTTP/HTTPS/SOCKS 黑洞代理下九产品选择器已正常显示，但该证据不等同于阻断所有 UDP 的物理断网。
- 验收目标校正：用户桌面快捷方式 `C:\Users\m1591\Desktop\火柴AI.lnk` 指向用户本人安装的原始包 `H:\HuoChaiAI\app\HuoChaiAI.exe`，不作为本项目恢复产物验收目标。该原始安装包已恢复原 `App.dll` 哈希 `72689D3C96F28A9DFBDDCFFC3F14D082A174AC0FED153144CD2AA89D27C3D494`，后续不得再用它验证 M4B。
- M5A 已启动只读验收：`.context/m5a-business-dependency-inventory.md` 已记录第一轮结果，`.context/m5a-menu-route-discovery.md` 已记录菜单路由发现、本地 spider/dataCollect 入口证据和 v41 WhatsApp `AI采集` 候选入口。AiCloud 首屏和空表证据可复用；ADB/FFmpeg 可执行；JxBrowser/Selenium/Playwright 依赖存在。关键新边界：菜单模型确实消费 `localCode/linkUrl`，原客户端支持 `ZWBrowser/JBigDataMaster/ai_mnq_manager/PhoneFission/JSinglepage` 等打开器；v40 的 76 个菜单仍统一 `JSinglepage + /pc/aicloud/my`，因此 WhatsApp/GEO 当前只能证明外壳和菜单存在；但 `data/app/res/spider/*.cnf` 与 `JSpiderCloude` 已证明 WhatsApp/TikTok/Facebook 等采集脚本和 `/pc/dataCollect/collectionTask/data_index?spiderCode=...&moduleCode=...` 入口族仍在包内。v41 仅把 WhatsApp `C4749_006 / AI采集` 改为显式标注的恢复路由候选，下一步只做页面打开和请求记录，不提交采集/群发任务。
- 当前活跃日志分片：work-log.md
- v15 VM 证据：`M4_V14_RENDER_MODE=OFF_SCREEN`，包含 SwiftShader/D3D11 软件渲染开关；`M4_V13_LOAD_FINISHED` 成功；`C:\m2dump\m4-jxb-capture.png` 能完整渲染 `HuoChaiAI Offline Mode`，右侧空白问题被软件渲染解决。
- 重要校正：v8-v15 的 `offline-home.html` 只是诊断页，用来区分菜单/加载/渲染问题；它不是最终业务目标。最终目标不是“业务离线化”，而是“授权/登录/时效/付费门槛本地通过 + 采集/群发/云手机/投屏/视频继续联网”。
- v18 改动：保留 v15/v16 的 `RenderingMode.OFF_SCREEN`、`disableGpu()`、SwiftShader/D3D11 软件渲染修复；菜单仍保持原始业务路由 `/pc/aicloud/my`；在 `com.sbf.main.jxbrowser.c$3.run()` 调用 `Navigation.loadUrl` 前，如果 URL 以 `/` 开头，则拼为 `https://` + 原程序业务域名 `com.sbf.util.http.SBFApi.c()` + 原路径，并打印 `M4_V18_NORMALIZED_URL=`。该来源来自原始 `JSBFMain` 构造 AIGC URL 的字节码证据。
- v19 改动：新增 `com.sbf.main.StartApp.f(String)` 前置桥接。若入参包含 `getLoingIsToken` 或 `get_current_token`，打印 `M4_V19_WEB_TOKEN_BRIDGE url=...` 并返回本地登录 token `offline-local-token-1234567890`。这只补 Web 登录态入口，不改真实业务 URL，不恢复 `offline-home.html`，不把采集/群发/云手机/视频等业务功能离线化。
- v18 产物：`.artifacts/working/m4-auth-jump-v18-sbfapi-url-base/App-m4-auth-patched-v18-sbfapi-url-base.jar`，大小 `31,866,319` 字节，SHA-256 `1C0DC7C4A8D79FEAE71ADA673D960921B9E032CD1AC2E703BC3FB526A7EE33A2`。
- v18 ISO：`.artifacts/working/m4-auth-jump-v18-sbfapi-url-base.iso`，大小 `31,932,416` 字节，SHA-256 `D21AA803D8294A9BBFDB57DE9DFF4E087EB6C379A614BFC37C5D989F159CD8AE`；ISO 只包含 v18 JAR 和中文 README，不包含 `offline-home.html`。
- v19 产物：`.artifacts/working/m5-web-login-bridge-v19/App-m4-auth-patched-v19-web-login-bridge.jar`，大小 `31,866,544` 字节，SHA-256 `E7826E328D591445F8F8D3C5548DFBBDABE5B6D96B0D6FB180DAAB5FDA1E00E3`。
- v19 ISO：`.artifacts/working/m5-web-login-bridge-v19.iso`，大小 `31,932,416` 字节，SHA-256 `4EBA6D2D52D2384A3803FA7F9FFC2A37F3D574CCCDDF0CC2C6577E9D43DB7C2B`；ISO 只包含 v19 JAR 和中文 README，不包含 `offline-home.html`。
- 宿主侧验证：目标测试覆盖 `StartApp.f("...getLoingIsToken")` 和 `StartApp.f("...get_current_token")`，返回本地 token；`python -m unittest discover -s tests -v` 通过 23/23；产物级 ZIP 比较显示原始 App.jar 到 v19 仅 9 个预期 class 改变：v18 的 8 个 class 加 `com/sbf/main/StartApp.class`；`javap` 确认 `StartApp.f(String)` 中存在 `getLoingIsToken`、`get_current_token`、`M4_V19_WEB_TOKEN_BRIDGE url=`、`offline-local-token-1234567890` 和 `String.contains(CharSequence)`；`git diff --check` 仅有既有 CRLF 警告。
- 宿主机运行证据：覆盖 `C:\m2dump\app\App.jar` 为 v19 后启动，点击登录和产品页“进入系统”。日志对 `AIGC Video`、`Graphic Video` 均显示 `M4_V18_NORMALIZED_URL=https://app.xdxsoft.com/pc/aicloud/my?...`、`M4_V13_NAV_FINISHED ... error=OK`，且多次出现 `M4_V19_WEB_TOKEN_BRIDGE url=https://app.xdxsoft.com/prod-api/getLoingIsToken`；未再出现 v18 的 `login?redirect=...` 终态。截图 `C:\m2dump\host-screen-v19-business.png` 显示在线业务壳层和 `AIGC Video` / `Graphic Video` Tab，说明 Web 登录页重定向已越过。
- v27 产物：`.artifacts/working/m5-js-page-bootstrap-v27/App-m5-v27-js-page-bootstrap.jar`，大小 `31,871,018` 字节，SHA-256 `35F72D6A1C5CC4B9A1C47E38AF2028DA219ED98577261C2A515A17878FB2FDA8`。
- v27 ISO：`.artifacts/working/m5-js-page-bootstrap-v27.iso`，大小 `31,936,512` 字节，SHA-256 `3B32737E36DC6C00DA9CAA040D22EEFC3A3C779BAA60690625E91525E1F178C4`；ISO 只包含 v27 JAR 和中文 README，不包含 `offline-home.html`。
- 宿主机 v27 证据：`C:\m2dump\m5-v27-host.log` 显示 `M4_V18_NORMALIZED_URL=https://app.xdxsoft.com/pc/aicloud/my?...`、`M5_V26_WEB_BOOTSTRAP_XHR url=/prod-api/getInfo`、`/prod-api/getRouters`、`/prod-api/system/dict/data/type/yes_no_1_0`、`/prod-api/mnq/mnqAuthAccounts/mylist?pageNum=1&pageSize=10`，并加载真实业务 chunk `chunk-dea9eb98.0b47177e.js`。`C:\m2dump\m4-jxb-capture.png` 已显示 AiCloud 授权码表页面和“暂无数据”，右侧不再白屏。
- v28 产物：`.artifacts/working/m5-js-page-bootstrap-v28-init-shape/App-m5-v28-init-shape.jar`，大小 `31,871,132` 字节，SHA-256 `4AB22FF9AA1063E0CA3C74AFC057D83798DE76B9BB1554D59BD056D9DB7A25B1`。
- v28 ISO：`.artifacts/working/m5-js-page-bootstrap-v28-init-shape.iso`，大小 `31,936,512` 字节，SHA-256 `61B77F2512AC7A7042F9C537C90C2F556FCF741C5B29097ECE04BFBC67912BCC`；ISO 只包含 v28 JAR 和中文 README，不包含 `offline-home.html`。
- v28 校正：v28 补 `im.udp.port` 后宿主机仍出现非致命 `JSBFMain.<init>` NPE。v29-v32 的分段诊断证明构造函数能正常返回，异常来自内部 IM 配置 catch；原字节码密文顺序确认实际读取 `im.port.udp`，不是 `im.udp.port`。临时 `JSBFMain` 诊断/托盘兜底均未进入正式版本。
- v33 产物：`.artifacts/working/m5-im-shape-v33/App-m5-v33-im-shape.jar`，大小 `31,871,130` 字节，SHA-256 `24CCC59B18DC97EF05BBD57B46844B7B56F469E48BE1A85DA3A4649DC7957DF5`。
- v33 ISO：`.artifacts/working/m5-im-shape-v33.iso`，大小 `31,936,512` 字节，SHA-256 `AE54073C1745E08164946814ABC949EB54894F67867705DD5F7143D09416C154`；ISO 只包含 v33 JAR 和 README，不包含 `offline-home.html`。
- v34 取证产物：`.artifacts/working/m4-real-product-menu-logging-v34/App-m4-v34-real-product-menu-logging.jar`，大小 `31,870,641` 字节，SHA-256 `F0E39AFEF17D800B83F9C4066DE6D565C663AE3CB553FEBAAC8885A77B478150`。
- v34 取证 ISO：`.artifacts/working/m4-real-product-menu-logging-v34.iso`，大小 `31,934,464` 字节，SHA-256 `44FBB3CA12534BD9F5C697B7D09F290908F7A00B72063EC3EFBD1466DB4B492C`；ISO 只包含 v34 JAR 和中文 README，不包含 `offline-home.html`。v34 使用 `--real-product-menu-logging` 模式，保留真实 `SBFApi.C()` 与 `SBFApi.k()` 返回路径，并在返回边界打印 `M4_EVIDENCE_PRODUCT_MODULE_REAL_JSON=` 与 `M4_EVIDENCE_PC_MENUS_REAL_JSON=`；默认补丁模式仍保持 v33 临时产品/菜单行为。宿主机实测中真实产品接口返回 `code=401` 令牌验证失败；菜单接口用反射探针触发时在返回前抛出 `JSONObject text must begin with '{'`，说明当前本地 token 下菜单原始响应不是 JSON 对象，单纯 `ARETURN` 返回边界日志不足以取得菜单 raw body。
- v35 取证产物：`.artifacts/working/m4-real-menu-raw-logging-v35/App-m4-v35-real-menu-raw-logging.jar`，大小 `31,870,701` 字节，SHA-256 `02D912F0E2F3553374BFE5BDDFEBF34FA00E4A890D52F18B4F064F5542FDBF58`。
- v35 取证 ISO：`.artifacts/working/m4-real-menu-raw-logging-v35.iso`，大小 `31,934,464` 字节，SHA-256 `8B5C4EC9E0E9CDB82170CC2AF52DD4667470F019BDAAB70A972CD83537D448C8`；ISO 只包含 v35 JAR 和中文 README，不包含 `offline-home.html`。v35 在 v34 基础上把无参 `SBFApi.k()` 菜单取证点前移到 `new JSONObject(raw)` 之前，新增 `M4_EVIDENCE_PC_MENUS_RAW_BODY=`。独立探针实测当前本地 token 下 raw body 为空串，随后原方法按预期抛出 `JSONObject text must begin with '{'`。
- v36 取证产物：`.artifacts/working/m4-real-menu-request-logging-v36/App-m4-v36-real-menu-request-logging.jar`，大小 `31,870,823` 字节，SHA-256 `9DBC454856F9A09EEC35603404298EEB42D1BAAECBDF20E491D65C2F5269E66B`。
- v36 取证 ISO：`.artifacts/working/m4-real-menu-request-logging-v36.iso`，大小 `31,934,464` 字节，SHA-256 `2EE09AD403123125677A2FC7690761B2AD9CF848C9FBE6119E4A3922A58D0F56`；ISO 只包含 v36 JAR 和中文 README，不包含 `offline-home.html`。v36 在 v35 基础上，于无参 `SBFApi.k()` 加密请求体生成后、真实 HTTP 调用前打印 `M4_EVIDENCE_PC_MENUS_REQUEST_URL/REQUEST_JSON/REQUEST_BODY/STATIC_A/STATIC_K/STATIC_L/HEADER_E`，用于确认菜单接口空体是否来自请求入参或授权态。
- 宿主机 v36 证据：`C:\m2dump\m4-v36-real-menu-request-probe.log` 显示只手动设置 `SBFApi.a` 时，`k/l` 为空、`JSBFMain.E=null`、菜单 raw 为空；`C:\m2dump\m4-v36-initialized-menu-probe.log` 显示先调用 `SBFApi.j()` 后 `a/k/l` 已由硬件指纹链生成，且手动设置 `JSBFMain.E=offline-local-token-1234567890` 后，菜单请求仍返回空 raw body。结论：菜单空体不是单纯由 `k/l` 未初始化造成，更高置信是服务器不接受本地 fake token/header/signature；正式九产品仍需真实服务器登录态或继续定位真实登录态字段来源。宿主机 `C:\m2dump\app\App.jar` 已恢复 v33 哈希 `24CCC59B18DC97EF05BBD57B46844B7B56F469E48BE1A85DA3A4649DC7957DF5`。
- 真实登录态追踪已闭环：原登录成功响应 `data.token -> StartApp.l -> JSBFMain.E`，产品和菜单接口不存在第二枚隐藏 token。当前没有旧虚拟机、历史账号或历史凭据，新注册账号被“等待审核开通”阻断，原服务端权限也已遗失；该路线正式终止，不再继续猜测或伪造远端认可 token。
- 宿主机 v33 证据：`C:\m2dump\m5-v33-host.log` 显示真实 URL `https://app.xdxsoft.com/pc/aicloud/my?...`、四个定点 Web 初始化接口、`M4_V13_LOAD_FINISHED`；`C:\m2dump\m5-v33-host.err` 不再包含 `JSBFMain.<init>` NPE；`C:\m2dump\host-screen-v33-business.png` 显示 AiCloud 授权码表页面。stderr 仍有后台图标资源 `Stream closed`，但不影响主窗口和 Web 业务页。
- 当前边界：v33 已实现“本地授权/登录/有效期/产品门槛 + 真实在线业务首屏”。当前仅对 Java 授权/产品初始化、Web 路由守卫、页面首屏空表/字典做本地补形状；后续增删改、采集、群发、云手机、投屏、视频等真实业务动作仍应继续联网验证，不应扩大成通用离线业务代理。

## 2026-06-23 里程碑校准

### 总体进度

| 里程碑 | 状态 | 估算 | 当前结论 |
| --- | --- | ---: | --- |
| M1 资产与反编译环境 | 完成 | 100% | 原始资产、备份、源码树和启动入口已建立。 |
| M2 字符串与资源解密 | 完成 | 100% | 明文映射、bootstrap、资源解密和动态验证已完成。 |
| M3 授权接缝定位 | 完成 | 100% | 登录、有效期、产品门槛、Web token 等接缝已明确。 |
| M4 本地授权与完整主界面 | 进行中 | 约 95% | 九产品、菜单、免操作启动和主界面已通过宿主实测；尚缺物理断网与最终双击包验收。 |
| M5 真实业务联网回归 | 只读验收与路由发现 | 约 40% | 已形成 M5A 第一版依赖分类、第一轮只读证据、菜单路由发现、spider/dataCollect 入口发现，并生成 v41 WhatsApp `AI采集` 只读候选入口；尚未执行真实业务动作验收。 |
| M6 业务源码谱与模块文档 | 未正式开始 | 约 10% | 已有分析材料，尚未形成最终模块文档。 |

### v33 已完成

- 本地登录、授权、有效期和产品门槛能够通过。
- JxBrowser 在宿主机和无 GPU VM 中可用软件渲染。
- 真实业务域名、相对 URL 归一化和 Web 登录态入口已经打通。
- AiCloud 在线页面首屏能够显示。
- `JSBFMain` 初始化阶段的 IM 配置 NPE 已解决。

### M4 尚未完成

- 需要在可控 VM 或管理员环境补一次真正禁用网络后的启动、九产品选择器和主界面验收；当前仅完成进程级 HTTP/HTTPS/SOCKS 黑洞代理验收。
- 需要制作并验证最终双击分发包；当前宿主验证使用 `java -cp ... com.sbf.main.StartApp` 启动，已经证明无需登录操作，但还不是最终安装/快捷方式交付。项目内 `data\app\HuoChaiAI.exe` 在本次会话启动时被系统返回“操作已被用户取消”，未形成双击通过证据。
- 76 个菜单当前统一使用恢复入口 `/pc/aicloud/my`；这是 M4 静态外壳入口，不代表各菜单业务已经恢复。M5A 第一轮已确认该统一入口会阻止 WhatsApp/GEO 真实业务页面验收；后续需基于 `data/app/res/spider/*.cnf`、`JSpiderCloude` 和分发器证据先恢复或定位真实菜单 `localCode/linkUrl`，无法证明的字段必须标为“恢复值”。

### 用户提供的产品结构证据

- 可进入系统：WhatsApp AI龙虾系统、TK AI龙虾系统、FB AI龙虾系统、Ins AI龙虾系统、X AI龙虾系统、TG AI龙虾系统、海外GEO AI龙虾系统、WhatsApp AI龙虾客服。
- 独立站 AI龙虾系统保持未开通和“进入官网了解”，当前阶段不实现其业务入口。
- WhatsApp AI龙虾系统截图显示的侧边栏包括：一句话、智能体模型、AI龙虾、超级号、AI采集、AI数据、AI筛选、AI群发、API、广告、AI客服。
- 这些截图是 M4 最终界面结构的验收证据，但不会打断当前里程碑顺序去提前开发各系统业务。

### 后续固定顺序

1. 冻结 v33 技术基线和哈希，不修改其现有产物。
2. M4A：用原包证据和明确恢复值实现 8 个可进入产品、1 个未开通独立站产品及各系统菜单拓扑。
3. M4B：正式化兼容鉴权、产品和菜单服务，实现双击免操作启动、断网进入、九产品卡与主界面验收。
4. M5A：逐项分类业务依赖为客户端直连第三方、原后端代理、原后端数据库、云资源/算力和 native 依赖，并验证仍可直连的业务。
5. M5B：对证明确实依赖遗失服务端的接口逐项重建兼容后端。
6. M6：整理系统、模块、Java 入口、Web 路由、JS bridge、业务 API、native 依赖和兼容后端文档。
