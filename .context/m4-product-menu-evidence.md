# M4 产品与菜单原包证据矩阵

> 目标：为 M4 产品选择器与真实菜单恢复建立可审计输入。本文只记录原包资源、明文映射、接口消费点和字节码能够支持的结论；没有证据的字段保持未确认，不沿用 v33 临时 AIGC 数据，也不根据截图猜内部值。

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
| 独立站 AI龙虾系统 | `aishope` 候选 | `svg/main_logo_aishope.svg` | `svg/aishope_icon_1..3.svg` | 高置信、待二次确认 | `data/app/i18.cnf` 的 `43_head_title/subtitle` 明确描述 Shopify 对标的独立站商城；原包同时存在完整 `aishope` logo/icon 资源族。尚缺产品接口响应或明确字节码把 `43`、独立站文案与 `aishope` code 直接关联，因此暂不写入正式补丁。 |

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

## 6. 尚未闭合的字段

以下字段目前没有原始产品接口响应证据：

- 九产品的真实 `id/sid/fid`
- 九产品的真实 `logoSvg`
- 九产品的真实 `themeStyle` 与完整主题色
- 每个产品菜单的真实 `id/parentId/code/name/icon`
- 每个菜单的真实 `localCode/linkUrl/webFlg`
- 独立站产品 `code=aishope` 与产品编号 `43` 的直接关联

## 7. 下一取证动作

1. v34 取证构建已生成并宿主机实测：`.artifacts/working/m4-real-product-menu-logging-v34/App-m4-v34-real-product-menu-logging.jar`。
2. 当前结果：`SBFApi.C()` 的真实返回边界打印 `M4_EVIDENCE_PRODUCT_MODULE_REAL_JSON={"code":401,...}`，含 `/system/function_module/listmy/41` 令牌验证失败信息；无参 `SBFApi.k()` 在反射探针触发后于返回前抛出 `JSONObject text must begin with '{'`，说明当前本地 token 下菜单 raw 响应不是 JSON 对象，`ARETURN` 取证点拿不到菜单 raw body。
3. v35 已将无参 `SBFApi.k()` 取证点前移到 `new JSONObject(raw)` 之前，打印 `M4_EVIDENCE_PC_MENUS_RAW_BODY=`。独立探针实测当前本地 token 下 raw body 为空串，随后原方法按预期抛出 `JSONObject text must begin with '{'`。
4. v36 已把无参 `SBFApi.k()` 取证点前移到请求边界：加密请求体生成后、真实 HTTP 调用前打印 URL、明文请求 JSON、加密请求体、`SBFApi.a/k/l` 和 `JSBFMain.E`。第一次探针仅手动设置 `SBFApi.a`，发现 `k/l` 为空、`JSBFMain.E=null`；第二次探针先调用 `SBFApi.j()` 生成硬件指纹态，并手动设置 `JSBFMain.E=offline-local-token-1234567890`，此时 `a/k/l/headerE` 均有值，但菜单 raw body 仍为空串。
5. 当前结论：菜单空体不是单纯的本地硬件指纹 `k/l` 未初始化问题；更高置信是服务器不接受本地 fake token/header/signature。产品接口仍为 401，因此不能从 v33/v36 临时值恢复正式九产品 JSON。
6. 下一步二选一：拿到可用真实服务器登录态后复跑 v36；或继续定位真实登录态/header 来源字段，特别是 `JSBFMain` 构造参数写入 `JSBFMain.E` 的上游和登录返回 `result/data` 的字段映射。
7. 对成功日志中的产品数组与菜单数组生成脱敏 JSON 证据表，再据此编写 M4 回归测试和正式本地返回值。
8. 正式实现前继续校验 v33/v36 JAR/ISO 哈希，确保 JxBrowser 渲染、真实业务 URL、Web bridge 和 AiCloud 首屏不回退。

## 8. 真实登录态与旧环境恢复路径

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
- 当前宿主机节点只有本轮测试留下的 `email=local@test.com`、`RememberPassword=0` 和空 `up`，没有历史凭据。下一步应进入保留原用户配置的旧虚拟机，先只读导出上述注册表节点；若 `RememberPassword=1` 且 `up` 非空，再由原客户端解密/自动填充并尝试正常登录，成功后复跑 v36。
