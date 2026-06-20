# M2 全量字符串解密与可读源码设计

## 1. 目标

按照外部《Phase 3 详解 · 字符串解密 AI Agent 指令脚本》的完整标准补齐 M2，在不启动完整客户端、不访问远端服务器、不修改原始资产的前提下，完成：

1. 全工程字符串解密候选盘点与分类。
2. 可追溯的 `string_map.json` 和 `string_map.csv`。
3. 独立的带明文注释源码树。
4. 仅供阅读的去花 JAR、重新反编译源码树与可读性对比报告。
5. 为后续断网 Windows 虚拟机动态 dump 准备明确的待补清单和手动操作说明。

本轮继续处于 M2，不进入 M3，不制作授权补丁。

## 2. 已知基线

- 原始 Java 产物：`.artifacts/working/m1-02/App.jar`
- CFR 源码树：`.artifacts/decompiled/cfr-app-20260620-0215`
- Java 文件数：4,226
- 已覆盖静态解密器：9 类
- 已成功解码调用：4,599 条
- 已还原 bootstrap 调用：73,597 / 73,600 条
- 启发式扫描发现：
  - 348 类接收 Unicode 字面量的候选调用族
  - 约 35,642 个候选调用
  - 当前 9 类约覆盖 4,599 个调用

上述 348 类仅为候选集合，不能直接视为 348 个真实解密器。

## 3. “全量覆盖”的准确含义

本项目不以“强行把每个 Unicode 字面量都解释成明文”为完成标准，而以“每个候选调用族均有可审计结论”为标准。

每个候选族必须被归入以下一种状态：

| 状态 | 含义 |
| --- | --- |
| `decoded_static` | 已通过静态算法可靠解密 |
| `decoded_existing_plaintext` | 参数本身是普通文本或无需解密 |
| `not_string_decoder` | 经方法体和返回类型验证，不是字符串解密器 |
| `unsupported_shape` | 属于字符串池、数组拼接或当前工具尚不支持的静态形态 |
| `dynamic_dump_required` | 依赖运行时类名、调用栈、系统属性或其他上下文，需动态 dump |
| `decode_error` | 已识别为解密器，但静态解码发生可复现错误 |

任何候选族都不得无状态消失。报告必须同时给出族数、调用数和证据位置。

## 4. 总体方案

采用五段式流水线，每段完成后单独验证和提交。

### 阶段 A：候选清单与算法族识别

新增只读扫描工具，遍历 CFR 源码树，收集：

- 调用所有者、方法名和参数签名
- Unicode 字面量调用数
- 调用文件与样本行
- 方法定义位置
- 返回类型、静态/实例方法属性
- 方法体特征：AES 表、XOR、移位、Base64、调用栈、类名、系统属性、字符串池等

输出：

- `.artifacts/analysis/string_decoder_inventory.json`
- `.artifacts/analysis/string_decoder_inventory.csv`
- `.context/string-decoder-inventory.md`

自动聚类只负责减少人工工作量；最终状态必须有静态证据。

### 阶段 B：扩展静态解密器

优先处理调用量最高、与启动/登录/授权路径相关、且与现有 AES 表算法同构的候选族。

实现原则：

- 将通用算法与每个解密器的密钥/常量配置分离。
- 不以方法名猜测算法。
- 每类解密器先用真实源码样本建立测试。
- 只有样本上下文合理且测试通过后才纳入全量映射。
- 对同一密文在不同调用上下文产生不同结果的情况，映射键必须包含解密器和调用者，不能只使用密文本身。

扩展后的统一记录至少包含：

```text
decoder
path
line
caller
encrypted_literal
decoded
status
error
evidence
```

输出：

- `.artifacts/analysis/string_map.json`
- `.artifacts/analysis/string_map.csv`
- `.artifacts/analysis/unresolved_string_calls.json`

JSON 与 CSV 必须来自同一内存记录集，避免统计漂移。

### 阶段 C：带明文注释源码树

从原 CFR 树复制生成新的阅读副本：

```text
.artifacts/decompiled/cfr-app-20260620-0215-annotated/
```

规则：

- 保留原始加密调用，不直接替换表达式。
- 在调用同一行末尾或紧邻上一行添加注释。
- 对换行、多次调用和包含 `*/`、换行符、控制字符的明文进行安全转义。
- 每条注释携带稳定映射 ID，便于从源码回查 JSON。
- 未解调用使用状态注释，例如：

```java
/* STRING_MAP: sm-000123 明文: "token" */
/* STRING_MAP: sm-000124 dynamic_dump_required */
```

输出必须保持 4,226 个 `.java` 文件；原 CFR 源码树哈希和内容不得变化。

### 阶段 D：去花阅读副本

对备份副本执行 Threadtear 或经验证的等价去混淆工具，目标仅为改善阅读，不参与最终补丁。

执行顺序：

1. 记录输入 JAR SHA256。
2. 在 `.artifacts/working/m2-deobfuscation/` 创建输入副本。
3. 先做工具兼容性和 dry-run/最小 transformer 验证。
4. 每次只启用一组明确 transformer：
   - 字符串解密/常量传播
   - 控制流简化
   - 无效跳转或异常块清理
5. 每次输出独立 JAR，不覆盖上一步。
6. 对最终阅读 JAR 使用 CFR 重新反编译。

输出：

- `.artifacts/deobfuscated/app-reading-only.jar`
- `.artifacts/decompiled/cfr-app-deobfuscated/`
- `.context/deobfuscation-report.md`

验收重点不是“能重新运行”，而是：

- JAR 结构可读取
- 关键类仍存在
- CFR 可完成反编译
- 关键方法的控制流或动态调用数量有量化改善

原始 JAR 永远是后续补丁唯一基线。

### 阶段 E：动态 dump 手动交接

本阶段不在当前主机启动客户端。静态阶段结束后生成：

- `.artifacts/analysis/dynamic_dump_targets.json`
- `.context/dynamic-dump-runbook.md`

runbook 必须按单步截图确认方式编写，涵盖：

1. 创建 Windows 10 x64 VirtualBox 虚拟机。
2. 禁用虚拟网卡、共享剪贴板、拖放和共享目录。
3. 创建纯净快照。
4. 通过只读 ISO 或临时介质导入目标和 agent。
5. 再次确认系统无网络适配器和无连接。
6. 执行 javaagent/目标程序。
7. 导出 dump 文件。
8. 回滚快照。

动态 dump 只 hook `dynamic_dump_targets.json` 中明确列出的解密出口，不进行全应用无差别拦截。

## 5. 工具与文件边界

计划新增或调整：

| 文件 | 职责 |
| --- | --- |
| `tools/inventory_string_decoders.py` | 扫描、聚类、分类候选解密器 |
| `tools/string_decoder_core.py` | 通用整数/AES 表解码算法 |
| `tools/string_decoder_registry.py` | 每类解密器的密钥、常量和调用模式 |
| `tools/decode_java_strings.py` | 编排扫描和生成统一映射 |
| `tools/annotate_java_strings.py` | 生成独立注释源码树 |
| `tools/export_string_map.py` | 从统一记录生成 JSON/CSV 与摘要 |
| `tests/` | 候选扫描、解密、CSV、注释安全性的自动测试 |

现有 `.artifacts/` 继续作为大体积本地产物目录，不加入 Git。Git 仅提交工具、测试和 `.context`/规格文档。

## 6. 测试策略

所有新行为按测试驱动开发：

### 单元测试

- Java 字面量反转义。
- 调用者类名和方法名推断。
- 候选调用族统计。
- 现有 9 类解密器的固定样本。
- 新增算法族的固定样本。
- CSV 中逗号、引号、换行、Unicode 的正确转义。
- 注释中 `*/`、CR/LF、控制字符的安全处理。
- 同一行多个调用能各自获得映射 ID。

### 集成测试

- 对小型 fixture 源码树生成完整 inventory、JSON、CSV 和注释树。
- 对真实源码树运行后校验：
  - 候选族全部有状态
  - JSON/CSV 记录数一致
  - 注释 Java 文件数为 4,226
  - 原源码树哈希不变
  - 已解记录在注释树中可按映射 ID 反查

### 抽样验证

至少抽查 20 条：

- 5 条 URL/路径
- 5 条 JSON 字段/header
- 5 条中文/英文 UI 文案
- 5 条启动、登录、token、授权相关记录

每条必须回到原文件和调用上下文检查语义。

## 7. 错误处理与停止条件

- 候选方法定义无法定位：标记 `unsupported_shape`，不猜算法。
- 静态解码产生乱码：保留原值，标记 `decode_error` 或 `dynamic_dump_required`。
- 去花工具需要联网下载：先请求用户批准，下载文件记录来源、版本和哈希。
- 去花后 JAR 无法被 CFR 读取：保留失败产物和日志，退回上一 transformer，不覆盖有效输出。
- 任一工具尝试修改原始 `App.jar` 或原 CFR 树：立即停止。
- 任一步需要启动完整客户端：停止并切换到动态 dump 手动交接，不在当前主机执行。

## 8. 验收标准

静态与去花部分完成需同时满足：

1. 所有候选调用族均具有分类状态和证据。
2. `string_map.json` 与 `string_map.csv` 记录数一致。
3. `unresolved_string_calls.json` 明确列出剩余问题。
4. 注释源码树包含 4,226 个 Java 文件。
5. 已解调用旁可见安全、可检索的明文注释和映射 ID。
6. 20 条抽样验证全部有原位证据。
7. 去花阅读 JAR 可读取并能重新 CFR 反编译。
8. 产出去花前后量化对比报告。
9. 原始 `App.jar` 与原 CFR 源码树哈希保持不变。
10. 动态 dump 待办和手动 runbook 完整。

只有动态 dump 待办清单为空，或剩余项已明确不会影响授权/启动路径时，才可宣布外部 Phase 3 完整完成。

## 9. 非目标

- 不在本机联网或断网启动完整客户端。
- 不修改服务器或探测服务器日志能力。
- 不把去花 JAR用于发布、运行验证或最终补丁。
- 不在 M2 修改登录、授权、支付、时效或业务逻辑。
- 不承诺静态阶段能解出所有运行时懒加载字符串；这些必须显式进入动态 dump 清单。

