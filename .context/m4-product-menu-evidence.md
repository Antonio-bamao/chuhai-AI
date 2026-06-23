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

1. 检查原客户端运行缓存、日志和 JxBrowser profile 是否留有 `/system/function_module/listmy/41` 与 `/api/v1/client/pc/menus` 响应。
2. 若无历史响应，只在宿主机对这两个精确接口的 Java 返回边界增加一次性结构日志；不修改通用网络层，不泛化拦截 `/prod-api/*`。
3. 对日志中的产品数组与菜单数组生成脱敏 JSON 证据表，再据此编写 M4 回归测试和正式本地返回值。
4. 正式实现前先冻结并校验 v33 JAR/ISO 哈希，确保 JxBrowser 渲染、真实业务 URL、Web bridge 和 AiCloud 首屏不回退。
