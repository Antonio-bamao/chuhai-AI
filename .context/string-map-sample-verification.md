# 字符串映射 20 条语义抽检

抽检时间：2026-06-21  
输入：`.artifacts/analysis/string_map.json`

选择方式：按稳定映射 ID，从 URL/路径、JSON/字段、UI 文案、启动与登录
四类各取 5 条；启动类额外保留 `token` 与 `expireTime` 两个 M2 核心字段。

## URL 与路径

| ID | Decoder | 来源 / Caller | 密文 | 明文 | 上下文判断 | 结论 |
|---|---|---|---|---|---|---|
| `sm-000077` | `JSetupDialog$JLoginNew.N` | `com/sbf/main/a$4.java:70` / `com.sbf.main.a$4run` | `\uc8bd\u7158\ub5f4\uf698\u8833` | `/out/` | 作为输出目录片段传入文件处理调用，语义一致。 | coherent |
| `sm-000093` | `OfficeNotice$c.A` | `com/sbf/main/a.java:138` / `com.sbf.main.ab` | `\ue4df\ub3bc\u71ad\u48b0\u6612\ueeba` | `/data/` | 用于 `DBHelper.init(...)` 的数据目录片段，语义一致。 | coherent |
| `sm-000137` | `b$d.N` | `com/sbf/main/a/a$3.java:24` / `com.sbf.main.a.a$3<init>` | `\ub847\u600e\u5f70\u9802\u7e0b\u521e\uba17\ud53f\u3a68\u29cf\ucf22\u188c\ubc06\ubbff\u7218\u3f3b\u46f2\u11fe\ud7ea\u3e4b\u74d6\u5170\ubf80\u69c8\u5a1e\u19e9\u8c08\u8f68\uaf86\u202d\u1643\u56f8\uff1c\u0259\u8e86\u8db3\u7ce0\u5991\u5191` | `https://trans.siyutui.com/bigdata/dpool` | 构造大数据导出组件时传入完整服务 URL，语义一致。 | coherent |
| `sm-000141` | `b$d.N` | `com/sbf/main/a/a.java:40` / `com.sbf.main.a.a<init>` | `\u8fc1\u2157\u8178\uc9f3\u5583\u1715\u6839\uac7b\uadf2\u7766\u4fd1` | `/banner.jpg` | 作为图片组件资源路径，语义一致。 | coherent |
| `sm-000260` | `AdsCallback$8$0.M` | `com/sbf/main/ads/SGAICloudPanel$5.java:25` / `com.sbf.main.ads.SGAICloudPanel$5actionPerformed` | `\u6a6d\u7d41\u2564\uafc8\u2155\u9959\u3242\ua7f0\uc679\u490c\ua3d4\uc4da\u4fad\ud001\uc368\u9c4e\u7efa\ub5b2\u3899\ue43f\ufd59\u2d4d\ua242\ua5ee\ueb20` | `https://zhiwen.888005.xyz` | 点击事件中作为外部地址传入调用，格式与使用位置一致。 | coherent |

## JSON、Cookie 与字段名

| ID | Decoder | 来源 / Caller | 密文 | 明文 | 上下文判断 | 结论 |
|---|---|---|---|---|---|---|
| `sm-000172` | `SGAICloudPanel$AdsCallback$6.O` | `com/sbf/main/ads/AdsCallback$6.java:53` / `com.sbf.main.ads.AdsCallback$6run` | `\u2887\u9508\uc2ee\u029f` | `name` | 直接作为 `JSONObject.optString` 键读取，语义一致。 | coherent |
| `sm-000183` | `AdsCallback$SGAICloudPanel$2.I` | `com/sbf/main/ads/AdsCallback$8.java:44` / `com.sbf.main.ads.AdsCallback$8run` | `\u454b\u697a\ub443\uffc7\u705a\u5f12` | `result` | 直接作为 `JSONObject.optString` 返回包装字段，语义一致。 | coherent |
| `sm-000241` | `AdsCallback$8$0.M` | `com/sbf/main/ads/AdsCallback.java:362` / `com.sbf.main.ads.AdsCallbacklambda$2` | `\u6be1\u7767\u0ea7\ub88b\u16c5` | `value` | 写入 Cookie JSON 时对应 `cookie.value()`，字段和值完全匹配。 | coherent |
| `sm-000242` | `AdsCallback$8$0.M` | `com/sbf/main/ads/AdsCallback.java:363` / `com.sbf.main.ads.AdsCallbacklambda$2` | `\u6bf3\u7769\u0ea6\ub89f\u16c9\ua42e` | `domain` | 写入 Cookie JSON 时对应 `cookie.domain()`，字段和值完全匹配。 | coherent |
| `sm-000245` | `AdsCallback$8$0.M` | `com/sbf/main/ads/AdsCallback.java:366` / `com.sbf.main.ads.AdsCallbacklambda$2` | `\u6be4\u7763\u0ea8\ub88b\u16d2\ua425` | `secure` | 写入 Cookie JSON 时对应 `cookie.isSecure()`，字段和值完全匹配。 | coherent |

## UI 文案

| ID | Decoder | 来源 / Caller | 密文 | 明文 | 上下文判断 | 结论 |
|---|---|---|---|---|---|---|
| `sm-000009` | `JBaseDialog$GradientToolTip$GradientToolTipUI.C` | `com/sbf/base/a.java:53` / `com.sbf.base.a<init>` | `\u0e6c\u7e1b\u08df\uaa41\u3c48\uafa6\u2dd9\u370b` | `欢迎您登录本软件` | 传给窗口 `setTitle(...)`，是完整登录欢迎标题。 | coherent |
| `sm-000020` | `JBaseDialog$GradientToolTip$GradientToolTipUI.C` | `com/sbf/base/AdvancedColorConverter.java:55` / `com.sbf.base.AdvancedColorConvertera` | `\u5c28\ub0d3\ub32e\ud091\ua941\u70a6\u13eb\u1eb0` | `无法解析颜色: ` | 与错误输出及待解析颜色值拼接，语义一致。 | coherent |
| `sm-000040` | `JBaseDialog$JBaseDialog$1.q` | `com/sbf/base/AdvancedColorConverter.java:164` / `com.sbf.base.AdvancedColorConvertera` | `\ue447\u067c\udcc3\u5478\u00ee\u8817\uc73f\ucd77\u0875\ub380\u325f\u860b\ud0c1\u7503\u9c8b\uc697` | `颜色必须是Color对象或字符串` | 作为 `IllegalArgumentException` 消息，类型校验语义完整。 | coherent |
| `sm-000134` | `b$d.N` | `com/sbf/main/a/a$1$1.java:31` / `com.sbf.main.a.a$1$1run` | `\u457f\u7ef6\u4740\u705e\u7f22\u5210\u52b5\u09ec\u8aa2\u4c13\ub47e\u643e\u1d5c` | `输入关键词异常,请重新输入` | 位于关键词输入异常处理路径，是完整用户提示。 | coherent |
| `sm-000139` | `b$d.N` | `com/sbf/main/a/a$3.java:24` / `com.sbf.main.a.a$3<init>` | `\ue108\u050a\u3c6a\uc38e\u2f82\ud994\ue56d` | `大数据导出记录` | 作为大数据导出组件标题参数，语义一致。 | coherent |

## 启动、登录与工作区

| ID | Decoder | 来源 / Caller | 密文 | 明文 | 上下文判断 | 结论 |
|---|---|---|---|---|---|---|
| `sm-003500` | `JLoginHTML$h.v` | `com/sbf/main/ext/j2026/ClawWorkspace.java:29` / `com.sbf.main.ext.j2026.ClawWorkspace<init>` | `\uaae8\u02d5\u4a12\ue864\u241c\uf7de\ufa70\u4318\u8f76\u51a2\u9b6d\u45cd\u9fb1` | `ClawWorkspace` | 传给工作区窗口 `setTitle(...)`，与类职责一致。 | coherent |
| `sm-003672` | `JLoginHTML$h.v` | `com/sbf/main/ext/j2026/JLoginHTML.java:39` / `com.sbf.main.ext.j2026.JLoginHTML<init>` | `\u62f7\u7e61\u89bf\ua291` | `登录系统` | 传给登录窗口 `setTitle(...)`，语义一致。 | coherent |
| `sm-003773` | `g$f.e` | `com/sbf/main/ext/j2026/JProductSelectorHtml.java:29` / `com.sbf.main.ext.j2026.JProductSelectorHtml<init>` | `\u5aa0\u505a\u70cb\ua814` | `功能入口` | 传给产品选择器窗口 `setTitle(...)`，与启动分支一致。 | coherent |
| `sm-028194` | `JSetupDialog$JLoginNew.N` | `com/sbf/main/StartApp.java:383` / `com.sbf.main.StartAppf` | `\ueca0\u309c\u751c\u33ee\u2473` | `token` | 作为请求 JSON 的 `put` 键写入 token，语义和数据流一致。 | coherent |
| `sm-028202` | `JSetupDialog$JLoginNew.N` | `com/sbf/main/StartApp.java:395` / `com.sbf.main.StartAppf` | `\uecb1\u308b\u7507\u33e2\u246f\uded4\uc82e\u80e3\u0573\ue160` | `expireTime` | 作为响应 JSON 的 `optLong(...,0)` 键读取缓存期限，语义和 M2 追踪一致。 | coherent |

## 机器可核验摘要

```json
{
  "selected_ids": [
    "sm-000077", "sm-000093", "sm-000137", "sm-000141", "sm-000260",
    "sm-000172", "sm-000183", "sm-000241", "sm-000242", "sm-000245",
    "sm-000009", "sm-000020", "sm-000040", "sm-000134", "sm-000139",
    "sm-003500", "sm-003672", "sm-003773", "sm-028194", "sm-028202"
  ],
  "coherent": 20,
  "rejected": 0
}
```

结论：20/20 与调用上下文一致，没有样本需要降级为 `decode_error`。
