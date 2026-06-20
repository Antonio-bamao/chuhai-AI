# Threadtear 只读字节码清理报告

执行日期：2026-06-21

## 安全边界

- 本机没有启动目标客户端、没有加载目标业务类、没有登录或联网调用服务端。
- 仅在 `App.jar` 的不可变副本上运行一个经官方源码核对的 ASM 变换：
  `me.nov.threadtear.execution.cleanup.remove.RemoveUnusedVariables`。
- 未运行 Stringer、ZKM、字符串解密、资源解密或任何使用
  `me.nov.threadtear.asm.vm.VM`、ClassLoader、反射、`Method.invoke` 的执行项。
- Threadtear 官方 README 明确警告部分 execution 会加载并执行目标类；这些执行项
  继续留到断网 Windows 虚拟机，不能在本机运行。

## 官方来源与工具指纹

Threadtear 官方仓库：
`https://github.com/loerting/threadtear`

官方最新发布页确认 3.0.1 发布于 2020-10-15：
`https://github.com/loerting/threadtear/releases/tag/3.0.1`

| 文件 | 大小 | SHA-256 |
|---|---:|---|
| `threadtear-gui-3.0.1-all.jar` | 13,147,956 | `0821C75FB4640EA48D7A7BD777EA27AFC4C6E8984D0F73E1E137B39C29DADDDD` |
| `threadtear-3.0.1-source.zip` | 1,328,203 | `CC2432A3B2D82C3D20FED3BFAE2EEF72918C27BAFA90BEBC51DFD191B3DA1C70` |
| `OpenJDK8U-jdk_x64_windows_hotspot_8u492b09.zip` | 106,462,632 | `1E33881EA6BFC1C532E3EAAD1C1DE7777169C0C1333E2B880621E0E0A16073B2` |
| `cfr-0.152.jar` | 2,162,315 | `F686E8F3DED377D7BC87D216A90E9E9512DF4156E75B06C655A16648AE8765B2` |

## Execution 源码核对

官方 `RemoveUnusedVariables.java`：

- 输入是 `Map<String, Clazz>` 中的 ASM `ClassNode` / `MethodNode`；
- 收集局部变量 load/store 指令；
- 对从未被读取的 store，以 `POP` 或 `POP2` 替换；
- 不包含 VM 构造、目标类加载、反射或方法调用；
- 预检器对官方源码原文运行后通过。

执行清单：
`.artifacts/working/m2-deobfuscation/threadtear-manifest.json`

最小无 GUI 驱动：
`tools/threadtear_bytecode_only/ThreadtearBytecodeOnly.java`

## 输入与输出

| 资产 | 大小 | SHA-256 |
|---|---:|---|
| 原始 `App.jar` | 31,852,508 | `9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B` |
| 清理输入副本 `input-App.jar` | 31,852,508 | `9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B` |
| 输出 `threadtear-bytecode-only.jar` | 31,861,355 | `5A5D3776F8003DDB14E76BBB46838EBD08027375FF80E2A9746A7479099BE4AD` |

输入副本与原始 JAR 哈希完全一致。输出 JAR 可由 `zipfile` 打开，包含
4,226 个 class，且 `com/sbf/main/StartApp.class` 存在。

## CFR 对比

输出源码：
`.artifacts/decompiled/cfr-app-deobfuscated`

| 指标 | 清理前 | 清理后 |
|---|---:|---:|
| Java 文件 | 4,226 | 4,226 |
| CFR `WARNING -` 总数 | 5,757 | 5,757 |
| bootstrap 调用 | 73,600 | 73,600 |
| bootstrap 成功解析 | 73,597 | 73,597 |
| `StartApp.java` 行数 | 722 | 717 |
| `JLoginNew.java` 行数 | 280 | 280 |
| `ClawWorkspace.java` 行数 | 141 | 141 |
| `MiJava$191$1$0.java` 行数 | 334 | 332 |

bootstrap 目标、描述符、key、payload 和错误信息的多重集合在清理前后完全
一致；719 条仅因 CFR 输出顺序/位置变化而不在相同数组下标。

## 结论

本次 Threadtear 主机侧清理执行成功，但收益有限：只减少少量局部变量相关
源码行，没有减少 CFR 警告，也没有改变 bootstrap 语义覆盖。后续分析应继续
以原始 CFR 树和独立注释树为主，Threadtear 输出只作为辅助对照。

需要 VM、ClassLoader 或反射的 Threadtear 解密 execution 不在本机执行，
必须在断网 Windows 虚拟机中按动态 dump 手册逐步操作并截图确认。
