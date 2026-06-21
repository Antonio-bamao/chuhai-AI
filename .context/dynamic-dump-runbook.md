# 断网 Windows VM 动态字符串 dump 手册

本手册只处理静态阶段保留下来的调用者歧义，不用于绕过登录、授权或服务端
控制。每一步完成后必须停止，把截图发回当前 Codex 会话；没有确认不要继续。

## 当前目标

- 未决调用记录：1,224
- 动态 caller 目标：131 个 family
- 优先级：5 critical、9 high、117 normal
- 第一轮只运行 critical；第二轮是否运行 high，要根据第一轮截图和 dump 决定。
- 唯一静态双定义 family `a$5$0.Z` 不在 javaagent 目标内，继续保守保留。

离线包：
`.artifacts/dynamic-dump-package-full`

离线包 ISO：
`.artifacts/dynamic-dump-package-full-v4.iso`

```text
SHA-256 ADD3AAF110665D2855B507FCAF7D6E1C7B485CC27ED4FA79A70BA8AC423E1F90
```

javaagent 已在本机用纯合成类做过 smoke test，成功记录：

```json
{"family":"DecoderTarget.x","caller_class":"fixture.AgentSmoke","caller_method":"main","input":"cipher","output":"cipher-plain"}
```

本机从未把 agent 挂载到 `App.jar`。

## 第 1 步：创建 Windows 虚拟机

在 VirtualBox 新建：

- Windows 10/11 x64；
- 4 GB 内存即可；
- 新建独立虚拟硬盘；
- 不复用 `lab-target`、`lab-attacker`、`lab-observer` 的 Linux 系统盘。

创建后先不要启动。

**停止并截图：** VirtualBox 左侧虚拟机列表和新 Windows VM 的“常规/系统”页面。

## 第 2 步：彻底断网

在新 VM 的“设置 → 网络”中：

- 每一个网卡都设为 `未连接（Not attached）`；
- 如果有网卡 2/3/4，也全部关闭；
- 不使用 NAT、桥接、Host-only、内部网络。

**停止并截图：** 网络页必须同时显示网卡模式为 `Not attached`。

## 第 3 步：关闭宿主机交换通道

在“设置”中确认：

- 共享剪贴板：禁用；
- 拖放：禁用；
- 共享文件夹：空；
- USB 自动捕获：禁用；
- 串口：禁用；
- 不挂载宿主机目录。

**停止并截图：** “常规 → 高级”、共享文件夹、USB 三个页面。

## 第 4 步：启动并证明无网络

启动 Windows VM，打开 `cmd.exe`，依次运行：

```bat
ipconfig /all
route print
ping 1.1.1.1
```

预期：

- 没有可用默认网关；
- 没有通往外网的默认路由；
- ping 失败。

**停止并截图：** 完整命令窗口；不要输入任何真实账号、token 或密码。

## 第 5 步：创建干净快照

关闭 VM，在 VirtualBox 创建快照：

```text
windows-offline-clean
```

**停止并截图：** 快照树和 VM 已关机状态。

## 第 6 步：制作并挂载只读输入 ISO

在宿主机把 `.artifacts/dynamic-dump-package-full` 制作成 ISO，然后作为虚拟光驱
挂载。ISO 中应包含：

- `App.jar`
- `app/`
- `lib/`
- `dynamic-string-dump-agent.jar`
- `threadtear-gui-3.0.1-all.jar`（agent 的 ASM 依赖）
- `dynamic_dump_agent_targets.tsv`
- `dynamic_dump_targets.json`
- `jre/`
- `README.txt`
- `RUN-CRITICAL.cmd`
- `RUN-HIGH.cmd`

不要使用共享文件夹传入。

**停止并截图：** VirtualBox 存储页显示 ISO 挂载；Windows 文件资源管理器显示
上述文件。

## 第 7 步：再次确认断网与文件哈希

重新执行：

```bat
ipconfig /all
route print
certutil -hashfile App.jar SHA256
certutil -hashfile dynamic-string-dump-agent.jar SHA256
```

期望：

```text
App.jar
9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B

dynamic-string-dump-agent.jar
546F525CB28A84F7DCCAF8F941D1D2D87AEBBDB46A41B876420EB0FE37B14EDD
```

**停止并截图：** 网络状态和两个哈希。

## 第 8 步：第一轮只挂 critical 目标

把光盘内容复制到 VM 内部临时目录，例如 `C:\m2dump`，然后：

```bat
cd /d C:\m2dump
RUN-CRITICAL.cmd
```

预期控制台先显示：

```text
[codex-dump-agent] target classes=...
[codex-dump-agent] output=C:\dump\strings.jsonl
```

随后可能出现：

```text
[codex-dump-agent] instrumented ...
```

不要登录，不输入真实凭据。只观察程序能否打开登录页/启动页；出现异常立即停止。

**停止并截图：** 完整控制台和应用当前窗口。

## 第 9 步：检查第一轮 dump

关闭应用，在 `cmd.exe` 运行：

```bat
dir C:\dump
type C:\dump\strings.jsonl
```

如果文件很长，只显示：

```bat
powershell -NoProfile -Command "Get-Content C:\dump\strings.jsonl -First 30"
```

**停止并截图：** 文件大小、前 30 行；不要继续 high 目标。

## 第 10 步：导出 dump，不连接网络

新建一个一次性小型虚拟磁盘，挂到 Windows VM，格式化后只复制：

```text
C:\dump\strings.jsonl
```

关机后：

1. 从 Windows VM 卸载该磁盘；
2. 以只读方式挂到 `lab-observer`；
3. 在 observer 中计算 SHA-256 并复制出来；
4. 不把该磁盘再次挂回 Windows VM。

**停止并截图：** Windows 复制完成、VM 已关机、observer 只读挂载和 SHA-256。

## 第 11 步：恢复快照

恢复：

```text
windows-offline-clean
```

确认动态运行产生的系统盘状态已丢弃。

**停止并截图：** 恢复后的快照树。

## 第二轮 high 目标

只有在第一轮 dump 被确认格式正确、没有异常行为且 critical 仍不足时，才恢复
快照后重新执行：

```bat
RUN-HIGH.cmd
```

仍然按第 7～11 步逐步截图。`normal` 默认不运行。

## 立即停止条件

出现以下任一情况，立刻关闭 VM，不继续：

- VirtualBox 网络不是 `Not attached`；
- Windows 出现默认网关或可达网络；
- agent/App 哈希不一致；
- 控制台没有 agent 启动标记；
- agent 报 transform 异常；
- 应用要求真实凭据；
- 出现非预期进程、持久化、驱动安装或系统设置修改；
- dump 中出现与目标无关的敏感数据。
