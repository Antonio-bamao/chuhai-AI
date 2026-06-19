# 备份清单

> M1-01 备份点。备份目录位于 `.artifacts`，默认被 Git 忽略，避免大体积二进制进入仓库历史。

## 备份点

- 备份时间：2026-06-20 02:10
- 备份目录：`H:\项目\出海-AI\.artifacts\backups\original-20260620-021044`
- 备份方式：PowerShell `Copy-Item -Recurse -Force`
- 源范围：根目录 5 个关键文件 + `data/` 全量目录
- 备份状态：已完成

## 备份范围

| 源路径 | 备份路径 |
| --- | --- |
| `H:\项目\出海-AI\火柴AI安装程序.exe` | `H:\项目\出海-AI\.artifacts\backups\original-20260620-021044\火柴AI安装程序.exe` |
| `H:\项目\出海-AI\logo.ico` | `H:\项目\出海-AI\.artifacts\backups\original-20260620-021044\logo.ico` |
| `H:\项目\出海-AI\Microsoft.Web.WebView2.Core.dll` | `H:\项目\出海-AI\.artifacts\backups\original-20260620-021044\Microsoft.Web.WebView2.Core.dll` |
| `H:\项目\出海-AI\Microsoft.Web.WebView2.WinForms.dll` | `H:\项目\出海-AI\.artifacts\backups\original-20260620-021044\Microsoft.Web.WebView2.WinForms.dll` |
| `H:\项目\出海-AI\WebView2Loader.dll` | `H:\项目\出海-AI\.artifacts\backups\original-20260620-021044\WebView2Loader.dll` |
| `H:\项目\出海-AI\data` | `H:\项目\出海-AI\.artifacts\backups\original-20260620-021044\data` |

## 完整性校验

| 校验项 | 源 | 备份 | 结果 |
| --- | ---: | ---: | --- |
| 文件数 | 772 | 772 | 一致 |
| 总大小 | 1,706,314,191 bytes | 1,706,314,191 bytes | 一致 |

关键文件哈希抽检：

| 文件 | 源 SHA256 | 备份 SHA256 | 结果 |
| --- | --- | --- | --- |
| `data\app\App.dll` | `9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B` | `9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B` | 一致 |
| `data\app\HuoChaiAI.exe` | `2A6DC95DE97761E4C92EF830ABCA56516B65A2FF3A3372E6ACCD156524A6D115` | `2A6DC95DE97761E4C92EF830ABCA56516B65A2FF3A3372E6ACCD156524A6D115` | 一致 |

## 全量 SHA256 清单

- 路径：`H:\项目\出海-AI\.artifacts\manifests\original-20260620-021044-sha256.csv`
- 记录数：772
- 字段：`RelativePath`、`Size`、`LastWriteTime`、`SHA256`

## 回滚方式

如后续实验污染工作目录，可从该备份点按原路径复制回对应文件。执行回滚前必须先确认目标路径仍在 `H:\项目\出海-AI` 内，并记录新的 work-log。
