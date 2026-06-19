# Bug / 工程异常记录

> 所有会影响推进、质量、节奏或判断的异常都要记录，包括代码、环境、依赖、测试、打包和设计误判。

## PowerShell New-Item LiteralPath 参数不兼容
- 现象：创建备份目录时 New-Item 报错 A parameter cannot be found that matches parameter name LiteralPath，后续 Copy-Item 因目标目录不存在失败。
- 触发条件：执行 M1-01 备份脚本时使用 New-Item -ItemType Directory -Force -LiteralPath。
- 影响：备份目录未创建，复制未发生；未修改原始资产。
- 根因：当前 PowerShell 环境的 New-Item 参数集不支持 -LiteralPath，脚本假设了较新的参数兼容性。
- 解决方案：改用 New-Item -Path 创建目录，并在复制前显式检查目标目录存在。
- 预防措施：后续目录创建优先使用兼容性更好的 -Path；对备份脚本增加 Test-Path 检查。
- 状态：resolved

## Windows 控制台无法打印部分解码 Unicode
- 现象：筛选授权候选时，直接 print 部分 decoded 字符串触发 UnicodeEncodeError: gbk codec cannot encode character。
- 触发条件：在 PowerShell 控制台打印 string_map 中包含特殊 Unicode 或未完全可读字符的 decoded 字段。
- 影响：控制台预览提前中断，但 JSON 文件输出已成功，分析数据未丢失。
- 根因：Windows 当前控制台编码为 GBK，不能表示部分 UTF-16/Unicode 字符。
- 解决方案：将完整结果写入 UTF-8 JSON；控制台预览改用 ASCII/转义输出或只打印统计。
- 预防措施：后续不要把未筛洗的 decoded 字段直接打印到控制台；报告引用 JSON 路径和统计。
- 状态：resolved

## bootstrap 候选初筛混淆 Java 重载方法
- 现象：初版 `startapp_bootstrap_candidates.json` 把 `StartApp.i(String)` 第 478-479 行的 `HashMap` 缓存操作归到 `StartApp.i()` 候选里。
- 触发条件：`tools/decode_bootstrap_calls.py` 只记录方法名，不记录参数签名；`StartApp` 同时存在 `i()` 与 `i(String)` 重载。
- 影响：若直接进入 M3，会把不相关重载方法的缓存删除行为混入定时任务候选。
- 根因：方法上下文模型粒度过粗，只用 `caller` 字段不足以区分 Java 重载。
- 解决方案：为 bootstrap 解码结果新增 `caller_signature`、`caller_method`、`caller_method_line` 字段；重生成候选时按签名与行段过滤。
- 预防措施：后续所有接缝候选引用动态调用时同时看方法签名和源码行段，不只看方法名。
- 状态：resolved

## 误把 token action contains 分支描述为 equals
- 现象：阶段文档一度写成参数等于 get_current_token 或 getLoingIsToken，进而把 StartApp.f 入参误描述为纯 action 字符串。
- 触发条件：只核对了解密后的 action 明文和 StartApp.f 调用目标，没有同时核对同一源码行的比较函数 bootstrap 目标。
- 影响：会误导后续 URL 语义判断，使看似无协议的 action 字符串与 OkHttp URL 要求产生矛盾。
- 根因：混淆代码中的比较操作也由 invokedynamic 隐藏，CFR 外观不能代替 bootstrap 描述符证据。
- 解决方案：核对 AdsCallback.java:191 与 MiJava.java:1553，确认比较目标均为 String.contains(CharSequence)；同步修正文档，并还原 RT/rdtime 拼接。
- 预防措施：今后确认混淆分支语义时同时验证三项：明文字面量、比较函数 bootstrap 目标、被调用方法描述符。
- 状态：resolved

## CFR 将 dup 栈值误还原为第二次 new n()
- 现象：n.a(String,String) 被反编译为创建两个 n 实例，看起来 URL 字段 c 被写入一个随后丢弃的对象。
- 触发条件：追踪 n.c() 刷新时发现若按 CFR 输出理解，返回缓存实例没有 URL，和实际刷新设计矛盾。
- 影响：可能错误判断定时刷新不可用，或把字段归属和补丁候选定位到错误对象。
- 根因：混淆字节码使用 new/dup/dup/astore 的栈操作，CFR 0.152 在该方法上把同一对象引用错误展示为第二次构造。
- 解决方案：直接解析 App.jar 中 n.class 的 Code 属性，确认只有一次 new，参数 c/d 写入同一返回实例。
- 预防措施：遇到反编译结果与控制流不变量矛盾时，必须回到原始字节码核对 new/dup/astore/putfield 指令。
- 状态：resolved
