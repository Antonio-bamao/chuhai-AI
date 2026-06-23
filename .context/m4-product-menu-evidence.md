# M4 产品与菜单原包证据矩阵

> 目标：为 M4A 产品选择器与菜单静态恢复建立可审计输入。原包可证实的 code、资源和文案按原值恢复；无法取得的数值 ID 使用稳定、可替换的本地兼容值，并明确标注为“恢复值”。不沿用 v33 临时 AIGC 数据，也不根据截图猜原始内部值。

## 1. 证据来源

- 原始客户端包：`data/app/App.dll`
- 产品选择器：`.artifacts/analysis/resources-decrypted/html/product-selector.html`
- 产品进入回调：`.artifacts/decompiled/cfr-app-20260620-0215-annotated/com/sbf/main/StartApp$1$3.java`
- 主界面菜单分发：`.artifacts/decompiled/cfr-app-20260620-0215-annotated/com/sbf/main/JSBFMain.java`
- 全量字符串映射：`.artifacts/analysis/string_map.json`
- 本地多语言资源：`data/app/i18.cnf`
- 当前临时补丁输入：`tools/m4_auth_patch/M4AuthPatch.java`

## 2. 产品字段消费契约

### 产品选择器直接消费

`product-selector.html` 直接消费：

- `code`
- `name`
- `displayName`
- `status`
- `remainingDays`
- `expirationTime`
- `primaryColor`
- `secondaryColor`

其中 `code` 还用于未开通产品官网地址：`https://www.huochai.ai/${product.code}`。

### Java 主界面构造直接消费

`StartApp$1$3.a(JSONObject)` 直接读取：

- `code`
- `sid`
- `fid`
- `name`
- `displayName`
- `primary_color`
- `secondary_color`
- `bgcolor`
- `logoSvg`
- `themeStyle`
- `menuAreaBackground`
- `menuItemDefaultTextColor`
- `menuItemHoverTextColor`
- `menuItemHoverBackgroundColor`
- `menuItemSelectedTextColor`
- `menuItemSelectedBackgroundColor`
- `topBarBackground`
- `topBarDefaultTextColor`
- `topMenuItemHoverTextColor`
- `topMenuItemHoverBackgroundColor`
- `topMenuItemSelectedTextColor`
- `topMenuItemSelectedBackgroundColor`
- `defaultBtnFontColor`
- `defaultBtnBackgroundColor`

产品 `code` 还参与主窗口资源约定：`svg/main_logo_<code>.svg`。

### 菜单输入直接消费

原始主界面至少消费：

- `id`
- `parentId`
- `code`
- `name`
- `icon`
- `children`
- `localCode`
- `linkUrl`
- `webFlg`

`JSBFMain` 已确认会按 `code` 分流，例如 `aigc`、`aicloud`、`getcustomer`、`rpa`、`kefu`。因此菜单 `code/localCode/linkUrl` 必须来自真实菜单证据，不能用显示名称反推。

## 3. 九个产品入口

| 用户验收名称 | 内部 code | 主窗口 logo 资源 | 产品菜单图标族 | 证据等级 | 备注 |
| --- | --- | --- | --- | --- | --- |
| WhatsApp AI龙虾系统 | `whatsapp` | `svg/main_logo_whatsapp.svg` | `svg/whatsapp_menu_icon_1..9.svg` | 已确认 | 原包同时包含 `logo/whatsapp.png`、`svg/whatsapp.svg`。 |
| TK AI龙虾系统 | `tiktok` | `svg/main_logo_tiktok.svg` | `svg/menu_tk_1..10.svg` | 已确认 | 原包及明文业务分支统一使用 `tiktok`，显示层可写 TK。 |
| FB AI龙虾系统 | `facebook` | `svg/main_logo_facebook.svg` | `svg/facebook_menu_icon_1..10.svg` | 已确认 | 原包同时包含 `logo/facebook.png`。 |
| Ins AI龙虾系统 | `instagram` | `svg/main_logo_instagram.svg` | `svg/ins_menu_icon_1..9.svg` | 已确认 | 内部 code 不是 `ins`，原包平台分支使用 `instagram`。 |
| X AI龙虾系统 | `twitter` | `svg/main_logo_twitter.svg` | `svg/twitter_menu_icon_1..9.svg` | 已确认 | 原包内部仍使用 `twitter`，显示层可写 X。 |
| TG AI龙虾系统 | `telegram` | `svg/main_logo_telegram.svg` | `svg/tg_menu_icon_1..11.svg` | 已确认 | 原包平台分支使用 `telegram`。 |
| 海外GEO AI龙虾系统 | `geo` | `svg/main_logo_geo.svg` | `svg/geo_ai_menu_icon_1..9.svg` | 已确认 | code 与主 logo、菜单 icon 前缀一致。 |
| WhatsApp AI龙虾客服 | `wskefu` | `svg/main_logo_wskefu.svg` | `svg/wskf_menu_icon_1..7.svg` | 已确认 | `JSBFMain` 明文存在 `wskefu -> kefu` 分发。 |
| 独立站 AI龙虾系统 | `aishope` 恢复采用值 | `svg/main_logo_aishope.svg` | `svg/aishope_icon_1..3.svg` | 高置信恢复 | `data/app/i18.cnf` 的 `43_head_title/subtitle` 描述 Shopify 对标独立站商城，原包存在完整 `aishope` logo/icon 资源族。没有原始响应可二次闭合，因此 M4A 将其作为明确标注的恢复采用值，保持未开通。 |

原包另有 `adfast` 产品 logo，但其明文和本地文件均指向广告客户端 `/app/AdFast`，不符合用户给出的九产品结构，当前排除为独立站入口。

## 4. 已确认的菜单证据

### 产品级菜单资源族

- WhatsApp：9 个 `whatsapp_menu_icon_*`
- TikTok：10 个 `menu_tk_*`
- Facebook：10 个 `facebook_menu_icon_*`
- Instagram：9 个 `ins_menu_icon_*`
- Twitter/X：9 个 `twitter_menu_icon_*`
- Telegram：11 个 `tg_menu_icon_*`
- GEO：9 个 `geo_ai_menu_icon_*`
- WhatsApp 客服：7 个 `wskf_menu_icon_*`
- 独立站候选：3 个 `aishope_icon_*`

这些资源证明原软件存在按产品拆分的真实菜单视觉族，但资源文件名不包含对应菜单的 `id/code/localCode/linkUrl`。

### WhatsApp 菜单文案候选族

`data/app/i18.cnf` 中存在多代 WhatsApp 菜单 code 族，例如：

- `C4749_*`：权重、耐发号、拉群、群发、采集、数据、筛选、镜像系统、客服等。
- `C3462_*`：权重、采集、数据、裂变、筛选、API 群发、AI 群发、代发、拉群、磐石、云手机等。
- `C3805_*`、`C4129_*` 等其他历史/组合版本。

用户截图中的“一句话、智能体模型、AI龙虾、超级号、AI采集、AI数据、AI筛选、AI群发、API、广告、AI客服”是 M4 结构验收证据，但仅凭文案无法在上述多代 code 族中唯一选定真实当前菜单。正式菜单必须继续从 `/api/v1/client/pc/menus` 消费点、原始响应残留或运行时定点记录中确定。

## 5. 当前明确不能使用的值

v33 中以下值是链路验证用临时输入，不是 M4 最终产品/菜单数据：

- 单产品 `id/sid/fid = 41`
- 单产品 `code = tiktok` 但显示名为 `HuoChaiAI`
- 顶层 `aigc`
- `C28500001 / AIGC Video`
- `C28500002 / Graphic Video`
- 统一 `/pc/aicloud/my`
- 合成主题色与 `themeStyle = default`

这些值在下一版正式产品选择器和菜单恢复时必须撤掉，但在证据未闭合前不修改 v33 基线。

## 6. 尚未闭合的原始字段与恢复规则

以下字段目前没有原始产品接口响应证据：

- 九产品的真实 `id/sid/fid`
- 九产品的真实 `logoSvg`
- 九产品的真实 `themeStyle` 与完整主题色
- 每个产品菜单的真实 `id/parentId/code/name/icon`
- 每个菜单的真实 `localCode/linkUrl/webFlg`
- 独立站产品 `code=aishope` 与产品编号 `43` 的直接关联

M4A 恢复规则：

- 已确认产品 code、logo 与 icon 资源使用原包值。
- `aishope` 作为高置信恢复采用值，证据等级不得写成“原始真实值”。
- 未知 `id/sid/fid/parentId` 使用集中定义的稳定本地兼容 ID；这些字段在代码、测试和本文中统一标“恢复值”。
- 菜单显示名称优先来自 i18n 明文；截图只用于最终界面结构验收。
- `localCode/linkUrl/webFlg` 必须来自反编译分发能力与现有业务入口证据；没有足够证据时使用保守的兼容入口并标“恢复值”，不得伪称原始路由。

## 7. 远端取证结论与终止条件

1. v34 取证构建已生成并宿主机实测：`.artifacts/working/m4-real-product-menu-logging-v34/App-m4-v34-real-product-menu-logging.jar`。
2. 当前结果：`SBFApi.C()` 的真实返回边界打印 `M4_EVIDENCE_PRODUCT_MODULE_REAL_JSON={"code":401,...}`，含 `/system/function_module/listmy/41` 令牌验证失败信息；无参 `SBFApi.k()` 在反射探针触发后于返回前抛出 `JSONObject text must begin with '{'`，说明当前本地 token 下菜单 raw 响应不是 JSON 对象，`ARETURN` 取证点拿不到菜单 raw body。
3. v35 已将无参 `SBFApi.k()` 取证点前移到 `new JSONObject(raw)` 之前，打印 `M4_EVIDENCE_PC_MENUS_RAW_BODY=`。独立探针实测当前本地 token 下 raw body 为空串，随后原方法按预期抛出 `JSONObject text must begin with '{'`。
4. v36 已把无参 `SBFApi.k()` 取证点前移到请求边界：加密请求体生成后、真实 HTTP 调用前打印 URL、明文请求 JSON、加密请求体、`SBFApi.a/k/l` 和 `JSBFMain.E`。第一次探针仅手动设置 `SBFApi.a`，发现 `k/l` 为空、`JSBFMain.E=null`；第二次探针先调用 `SBFApi.j()` 生成硬件指纹态，并手动设置 `JSBFMain.E=offline-local-token-1234567890`，此时 `a/k/l/headerE` 均有值，但菜单 raw body 仍为空串。
5. 当前结论：菜单空体不是单纯的本地硬件指纹 `k/l` 未初始化问题；更高置信是服务器不接受本地 fake token/header/signature。产品接口仍为 401，因此不能从 v33/v36 临时值恢复正式九产品 JSON。
6. 真实 token 来源已闭环为登录响应 `data.token`，不存在第二枚隐藏 Java token。
7. 当前没有旧虚拟机、历史账号或历史凭据；新注册账号等待审核；原服务端、数据库和管理后台权限均已遗失。
8. 终止继续猜测或伪造远端 token。M4A 不再等待原始产品/菜单响应，改用原包证据加明确恢复值；正式实现前继续校验 v33 哈希，确保 JxBrowser 渲染、真实业务 URL、Web bridge 和 AiCloud 首屏不回退。

## 8. 真实登录态调查闭环

- `StartApp$1.a(JSONObject)` 从登录成功响应的 `data.token` 写入 `StartApp.l`，随后直接执行 `JSBFMain.E = StartApp.l`。产品接口和菜单接口使用的是同一枚真实登录 token，不存在尚未发现的第二种 Java header token。
- `StartApp$1$3.a(JSONObject)` 创建主窗口时传入 `new JSBFMain(StartApp.y, StartApp.m, StartApp.l, productCode)`，进一步确认构造参数中的 token 也是 `StartApp.l`。
- j2026 邮箱登录路径为 `JLoginHTML$4.run() -> SBFApi.k(email, password)`；成功后回调原始 `StartApp$1`，由响应 `data.token` 建立后续服务器会话。
- 登录页“记住我”并非依赖 Chromium 密码库。`HtmlJava$1.run()` 通过 `com.sbf.main.b.u()/z()/A()` 从 Java Preferences 读取保存状态、账号密码和邮箱。
- Windows 存储位置为当前用户注册表 Java Preferences 节点：
  `HKEY_CURRENT_USER\Software\JavaSoft\Prefs\aimirrorsystem\config`
- 关键逻辑键为 `RememberPassword`、`up`、`email`；旧版自动登录链还可能存在 `token`、`account`。
- Windows 注册表会按 Java Preferences 规则转义大写字符，因此 `RememberPassword` 在 `reg query` 输出中可能显示为 `/Remember/Password`。
- `up` 不是明文：保存方法 `com.sbf.main.b.b(email,password)` 将 `email + "__________" + password` 交给 `AESCBCHelper` 加密后写入；读取方法 `b.z()` 调用 `AESCBCHelper.b(...)` 解密并按同一分隔符拆成账号密码。
- 旧版 `token/account` 自动登录缓存还带机器码和时间检查，最长只接受约 7 天；因此历史 token 本身大概率已过期，但 `up` 中的记住密码凭据仍可用于重新请求服务器 token。
- 当前宿主机节点只有本轮测试留下的 `email=local@test.com`、`RememberPassword=0` 和空 `up`，没有历史凭据。
- 已确认不存在可用旧虚拟机、历史账号或历史凭据；因此“恢复旧凭据后复跑 v36”不再是可执行路线。
- 正式备用路线为兼容后端：M4B 先正式化客户端内嵌的登录、`getInfo`、产品、菜单、Web token 和首屏响应；M5B 再对业务所需的数据库、任务队列、云设备或算力接口逐项外置重建。

## 9. M4A 恢复值命名空间

为避免与原始真实值混淆，M4A 使用独立、稳定、可整体替换的本地兼容 ID 段：

- 产品恢复 ID：`9101..9109`，顺序对应 `whatsapp/tiktok/facebook/instagram/twitter/telegram/geo/wskefu/aishope`。
- 每个产品顶层菜单恢复 ID：`产品恢复 ID * 100 + 菜单序号`。
- 子菜单恢复 ID：`产品恢复 ID * 10000 + 父菜单序号 * 100 + 子菜单序号`。
- 所有恢复 ID 只承担本地树关系和稳定测试定位，不声明与原服务端 ID 相等。
- 若未来获得原始响应，只需替换集中目录数据，不改菜单分发代码和测试语义。

## 10. M4A v37 静态恢复实现

实现位置：

- `tools/m4_auth_patch/M4RecoveryCatalog.java`
- `tools/m4_auth_patch/M4AuthPatch.java`
- `tests/test_m4_auth_patch.py`

产品恢复值：

| 恢复 ID | code | 显示名称 | 状态 |
| ---: | --- | --- | --- |
| 9101 | `whatsapp` | WhatsApp AI龙虾系统 | 已开通 |
| 9102 | `tiktok` | TK AI龙虾系统 | 已开通 |
| 9103 | `facebook` | FB AI龙虾系统 | 已开通 |
| 9104 | `instagram` | Ins AI龙虾系统 | 已开通 |
| 9105 | `twitter` | X AI龙虾系统 | 已开通 |
| 9106 | `telegram` | TG AI龙虾系统 | 已开通 |
| 9107 | `geo` | 海外GEO AI龙虾系统 | 已开通 |
| 9108 | `wskefu` | WhatsApp AI龙虾客服 | 已开通 |
| 9109 | `aishope` | 独立站 AI龙虾系统 | 未开通 |

菜单恢复来源：

| 产品 | 菜单数 | 主要 i18n code 证据 | icon 资源族 |
| --- | ---: | --- | --- |
| WhatsApp | 11 | `C4749_*`、`C3460_001`；“一句话/智能体模型/AI龙虾/超级号”为截图验收下的恢复 code | `whatsapp_menu_icon_1..9.svg` |
| TikTok | 10 | `C3461_002..012` 选定十项 | `menu_tk_1..10.svg` |
| Facebook | 10 | `C4747_000..009` | `facebook_menu_icon_1..10.svg` |
| Instagram | 9 | `C4131_002..010` | `ins_menu_icon_1..9.svg` |
| Twitter/X | 9 | `C4133_002..009`、`C4133_017` | `twitter_menu_icon_1..9.svg` |
| Telegram | 11 | `C4135_001..011` | `tg_menu_icon_1..11.svg` |
| GEO | 9 | `C4134_002/003/006`、`C4137_001..006` | `geo_ai_menu_icon_1..9.svg` |
| WhatsApp 客服 | 7 | `C4936_000/001/002/004/005/006/007` | `wskf_menu_icon_1..7.svg` |

当前入口边界：

- M4A 的 76 个菜单项统一使用恢复入口 `localCode=JSinglepage`、`linkUrl=/pc/aicloud/my`、`webFlg=1`。
- 这是为恢复真实产品/菜单外壳并保护 v33 在线首屏链路的保守兼容值，不声明为各菜单原始真实路由。
- 各菜单实际业务分流、客户端直连和后端依赖在 M5A 分类后再逐项闭合。

静态验证：

- `python -m unittest discover -s tests -v`：26/26 通过。
- Java 8 `_JAVA_OPTIONS=-Xverify:all` 目标补丁探针通过。
- v37 JAR：`.artifacts/working/m4a-product-menu-v37/App-m4a-v37-product-menu.jar`
- v37 JAR SHA-256：`406B4E73990B2C03C3483B81368B2EB053F67C81EB3C25EA962573329F7E018C`
- 原始 JAR 到 v37 的修改/新增类集合与 v33 能力集合一致：修改 10 个既有类，新增 3 个 M5 观察/注入类。
- v33 JAR/ISO 哈希复核保持不变。

## 11. M4B v40 免操作启动与运行时验收

实现变化：

- `StartApp$3.run()` 继续执行原登录前初始化；到创建 `JLoginHTML` 的边界时，改为构造本地登录 JSON并调用原始 `StartApp$1.a(JSONObject)` 成功链，然后直接返回。
- `StartApp$1.a(JSONObject)` 仍负责写入 `StartApp.l`、`JSBFMain.E`、创建产品选择器和后续主界面；仅对自动登录场景下为空的 `StartApp.t` 增加 dispose null guard。
- 补丁生成时从原始 JAR 解密 `svg/main_logo_<code>.svg`，把九个原始产品 logo 以内联 SVG 写入产品 JSON。
- 菜单 icon 字段按原客户端 `IconUtil` 消费契约恢复为不含 `svg/` 前缀和 `.svg` 后缀的资源名，避免运行时形成 `/svg/svg/<name>.svg.svg`。

测试与产物：

- `python -m unittest discover -s tests -v`：26/26 通过。
- Java 8 以 `-Xverify:all` 启动 v40 成功。
- v40 JAR：`.artifacts/working/m4b-auto-login-v40/App-m4b-v40-auto-login.jar`
- v40 JAR 大小：`31,880,827` 字节。
- v40 JAR SHA-256：`4D3EA48E3D103D183D92619B96E2F3F2593FE92B4F274FD66E9649DBDA5D3046`
- 相对原始 JAR，新增 3 个 M5 观察/注入类；修改类集合在 v37 基础上增加 `StartApp$1.class` 和 `StartApp$3.class`，用于登录窗 null guard 与自动登录。

宿主运行证据：

- `C:\m2dump\m4-v40-host.log` 出现 `M4B_AUTO_LOGIN`、`/html/product-selector.html`、`M4B_SKIP_LOGIN_DISPOSE`；无需填写账号或点击登录。
- `C:\m2dump\host-screen-v39-auto-login.png` / `host-screen-v40-offline-proxy-selector.png` 显示九产品卡、原始 logo、前八“进入系统”和 aishope“进入官网了解”。
- `C:\m2dump\host-screen-v40-whatsapp-main.png` 显示 WhatsApp 主界面、11 项菜单文字和原包 icon。
- v39 首次运行发现菜单 icon 被拼为 `/svg/svg/<name>.svg.svg`；v40 已按真实消费契约修复，stderr 不再出现该批 icon `Stream closed`。
- Windows 防火墙临时出站规则因当前会话无管理员权限被拒绝，未留下规则。随后使用 JVM 级 HTTP/HTTPS/SOCKS 黑洞代理启动，九产品选择器仍正常显示；该证据证明 M4 授权、登录和产品目录不依赖远端 HTTP，但不等同于物理断网阻断全部 UDP。
- 测试后已停止 v40 进程，并把 `C:\m2dump\app\App.jar` 恢复为 v33 SHA-256 `24CCC59B18DC97EF05BBD57B46844B7B56F469E48BE1A85DA3A4649DC7957DF5`。
