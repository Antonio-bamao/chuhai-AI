# 反编译报告

## M1-02：源码树导出

- 时间：2026-06-20 02:15
- 输入文件：`H:\项目\出海-AI\.artifacts\working\m1-02\App.jar`
- 输入来源：`H:\项目\出海-AI\.artifacts\backups\original-20260620-021044\data\app\App.dll`
- 输入 SHA256：`9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B`
- 输出目录：`H:\项目\出海-AI\.artifacts\decompiled\cfr-app-20260620-0215`
- 工具：CFR 0.152
- 工具来源：`https://www.benf.org/other/cfr/`
- 工具 MD5：`8A85ADA8CEC494121246805A5562B82B`
- 运行 Java：`H:\项目\出海-AI\data\jdk\bin\java.exe`，Java `1.8.0_181`

## 工具选择说明

原计划写作“JADX 导出源码树”，但只读检查确认 `data\app\App.dll` 文件头为 `PK 03 04`，内部是 Java `.class`，不是 Android dex/apk。JADX 主要面向 Android/Dalvik 输入；本轮实际采用 CFR 反编译 Java class JAR。该选择已记录为 ADR-0002。

## 命令记录

```powershell
Copy-Item -LiteralPath '.artifacts\backups\original-20260620-021044\data\app\App.dll' `
  -Destination '.artifacts\working\m1-02\App.jar' -Force

.\data\jdk\bin\java.exe -jar .artifacts\tools\cfr-0.152.jar `
  .artifacts\working\m1-02\App.jar `
  --outputdir .artifacts\decompiled\cfr-app-20260620-0215 `
  --silent true `
  --caseinsensitivefs true
```

## 输出统计

| 指标 | 数值 |
| --- | ---: |
| `.java` 文件数 | 4,226 |
| 输出总文件数 | 4,227 |
| 输出总大小 | 59,649,060 bytes |

CFR 使用 `--silent true`，控制台没有错误输出，未生成额外日志文件。

## Manifest 线索

```text
Main-Class: com.sbf.main.StartApp
Created-By: 1.8.0_201-b09 (Oracle Corporation)
```

## 初步入口候选

| 类 / 文件 | 观察 |
| --- | --- |
| `com\sbf\main\StartApp.java` | Manifest 指定 Main-Class；含 `public static void main(String[] ...)`；导入 `JLoginHTML`、`JProductSelectorHtml`、`ClawWorkspace`；出现 `JSetupDialog$JLoginNew.N(...)` 字符串解密调用。 |
| `com\sbf\main\JLoginNew.java` | 登录相关 Swing 类；导入 `com.sbf.main.login.*`；出现 `vS(...)` 字符串/调用混淆痕迹。 |
| `com\sbf\main\ext\j2026\ClawWorkspace.java` | 核心工作区候选；构造函数接收 `JProductSelectorHtml$a`；继承 `com.sbf.main.ext.j2026.ui.e`。 |
| `com\sbf\main\ext\j2026\JLoginHTML.java` | HTML 登录界面候选。 |
| `com\sbf\main\ext\j2026\JProductSelectorHtml.java` | 套餐/产品选择界面候选。 |
| `com\sbf\main\ext\j2026\JMainMaster.java` | 主界面/master 候选。 |
| `com\sbf\main\ext\j2026\JMaster2026.java` | 2026 主流程候选。 |

## 包结构观察

`com\sbf\main` 下主要子目录包括：

```text
a, ads, armbox, b, c, cloud, d, e, ext, f, g, h, i, im, j,
jxbrowser, k, kefu, keyword, login, mnq, msg, ocx, proxy, rpa,
spide, sub, svg, theme, tree, video
```

## 对下一步的影响

M1-02 的源码树已经足够支撑 M2。下一步应围绕字符串解密函数候选展开，优先分析：

- `com\sbf\main\JSetupDialog$JLoginNew.N(...)`
- `com\sbf\main\JLoginNew.vS(...)`
- `com\sbf\main\JTestFrame$JLoginNew$2.k(...)`
- `com\sbf\main\StartApp.Sy(...)`
- `com\sbf\main\ext\j2026\ClawWorkspace.vv(...)`
- `com\sbf\main\ext\j2026\JLoginHTML$h.v(...)`

M1-03 原始启动验证仍等待隔离/断网环境确认。
