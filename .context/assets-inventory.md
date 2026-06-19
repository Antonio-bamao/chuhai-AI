# 资产清单

> M1-01 草稿。仅记录当前仓库中的原始客户端资产，不代表已反编译或已验证业务行为。

## 扫描时间

- 时间：2026-06-20 02:10
- 工作目录：`H:\项目\出海-AI`
- 操作范围：只读清点，未修改原始资产。

## 根目录资产

| 路径 | 大小 | 说明 | SHA256 |
| --- | ---: | --- | --- |
| `火柴AI安装程序.exe` | 268,288 | 安装程序，版本信息 `0.0.0.0` | `680436DD796914AF4F1313E9677569F00206C2D243032E0CFD2413FDD62DE17F` |
| `logo.ico` | 206,149 | 应用图标 | `575A0694B7FCEF0C3383022184869E77040C606979E1D433E38013B7DD1C164B` |
| `Microsoft.Web.WebView2.Core.dll` | 536,152 | WebView2 运行时托管组件，版本 `1.0.2210.55` | `5E172B4F558723B7DBB7F568F301077C84D6571436FBE5A5F45BFA621C020403` |
| `Microsoft.Web.WebView2.WinForms.dll` | 40,528 | WebView2 WinForms 组件，版本 `1.0.2210.55` | `BEF507A4CE7B6A848993BC504AF7E2273CEC22E77469787CB1D47D3F362164ED` |
| `WebView2Loader.dll` | 117,312 | WebView2 loader，版本 `1.0.2210.55` | `AD5CFE82F102739D4CC15C3EB38A411525762520C9C4229C902F67DBAB23C5FB` |

## `data/` 总览

| 项 | 数值 |
| --- | ---: |
| 目录数 | 43 |
| 文件数 | 767 |
| 总大小 | 1,705,145,762 bytes |

顶层目录：

| 路径 | 说明 |
| --- | --- |
| `data\app` | 应用启动器、主 App 包、资源、ADB、配置。 |
| `data\jdk` | 内置 JDK/JRE 运行环境。 |
| `data\lib` | Java 依赖、JxBrowser、Playwright、FFmpeg、Selenium、`libmytrpc` 等。 |
| `data\tools` | VECore/视频处理相关工具与 DLL。 |

## 关键应用资产

| 路径 | 大小 | 观察 | SHA256 |
| --- | ---: | --- | --- |
| `data\app\App.dll` | 31,852,508 | 文件头为 `PK 03 04`，实际是 ZIP/JAR 形态，是后续反编译重点。 | `9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B` |
| `data\app\HuoChaiAI.exe` | 1,300,992 | PE 启动器，版本 `1.0.0.0`，描述 `huǒ chái AI`。 | `2A6DC95DE97761E4C92EF830ABCA56516B65A2FF3A3372E6ACCD156524A6D115` |
| `data\app\app.ver` | 3 | 内容为 `102`。 | `37834F2F25762F23E1F74A531CBE445DB73D6765EBE60878A7DFBECD7D4AF6E1` |
| `data\app\i18.cnf` | 5,251,158 | 应用配置/国际化候选资源。 | 见完整机器清单 |
| `data\app\splash.html` | 12,523 | 启动页 HTML。 | 见完整机器清单 |

## 关键依赖与 native 边界

| 路径/模式 | 观察 |
| --- | --- |
| `data\lib\jxbrowser-win64-7.41.3.jar` | 107,151,702 bytes，JxBrowser Windows 运行组件。 |
| `data\lib\driver-bundle-1.46.0.jar` | 171,527,515 bytes，大型驱动包。 |
| `data\lib\libmytrpc*.dll` | 共 50 个 DLL，属于 native 签名边界，本期只作黑盒复用。 |
| `data\tools\vecore\*.dll` / `*.exe` | VECore 和视频处理相关 native 工具，本期不修改。 |
| `data\lib\ffmpeg.exe` | FFmpeg 可执行文件，本期不修改。 |

## 文件分布 Top 10

| 目录 | 文件数 |
| --- | ---: |
| `.\data\lib` | 334 |
| `.\data\jdk\bin` | 129 |
| `.\data\tools\vecore` | 54 |
| `.\data\tools\vecore\resource\shader` | 36 |
| `.\data\jdk\lib` | 30 |
| `.\data\app\res\pagebanner` | 21 |
| `.\data\app\res\spider` | 21 |
| `.\data\tools\vecore\resource\shader\toning` | 19 |
| `.\data\jdk\lib\deploy` | 17 |
| `.\data\jdk\lib\fonts` | 16 |

## 机器清单

- SHA256 全量清单：`H:\项目\出海-AI\.artifacts\manifests\original-20260620-021044-sha256.csv`
- 该清单位于 `.artifacts`，默认被 Git 忽略，不作为源码文档提交。
