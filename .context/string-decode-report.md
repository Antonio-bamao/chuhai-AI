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
| `JSetupDialog$JLoginNew.N(...)` | 442 | 依赖调用者 `className + methodName` 的字符串解密函数。 |
| `JTestFrame$JLoginNew$2.k(...)` | 293 | 与 `N(...)` 结构相同，但 key/常量不同。 |
| `JLoginHTML$h.v(...)` | 93 | j2026 登录/工作区相关字符串解密函数，与 `N(...)` 结构相同但 key/常量不同。 |

脚本实际抓到 1,094 条调用记录；其中包含同一行多个调用、以及后续匹配到的同类调用。当前可读性评分 `>= 0.8` 的记录有 1,034 条，第一版可作为 M3 检索输入。

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

按 `auth/license/login/token/expire/pay/order/user/tenant/role/header/result/data/password/product` 以及中文关键词筛选出 225 条候选，已写入：

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

## 重要限制

- 当前脚本覆盖 `N(...)`、`k(...)` 与 `JLoginHTML$h.v(...)` 这一类静态字符串解密。
- `StartApp.Sy(...)`、`JLoginNew.vS(...)`、`ClawWorkspace.vv(...)` 等是 invokedynamic/bootstrap 形态，不是同一类“密文直接变明文”调用，后续需单独处理。
- 部分记录因调用点推断不准或本身不是文本，会出现低可读性结果；M3 使用时应优先引用高可读性字段和入口类上下文。

## 下一步

继续 M2 第二阶段：

1. 解析 `ClawWorkspace.vv(...)`、`JLoginNew.vS(...)`、`StartApp.Sy(...)` 等 invokedynamic/bootstrap 形态。
2. 基于 `auth_string_candidates.json` 反查调用链，聚焦 `StartApp.f/i`、`JLoginNew`、`ext.j2026` 下登录和套餐选择路径。
3. 产出更接近 M3 的 `seam-candidates.md` 草稿，但不做 patch。
