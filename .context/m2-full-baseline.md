# M2 全量字符串解密基线

冻结时间：2026-06-21  
基线提交：`46e9e2d684dd81df81376cf8b184a31e5050354e`

## 输入

- 原始 JAR：`.artifacts/working/m1-02/App.jar`
- CFR 源码树：`.artifacts/decompiled/cfr-app-20260620-0215`
- 现有静态映射：`.artifacts/analysis/string_map.json`
- bootstrap 映射：`.artifacts/analysis/bootstrap_map.json`

## 指标

- 源码树文件数：4,227
- Java 文件数：4,226
- 源码树总字节数：59,649,060
- 原始 JAR SHA-256：`9084FABCE357AAD8B18D06D0FB708DE4E92E1B5D63686CEA1DED49E19F73A99B`
- 现有静态映射记录数：4,599
- bootstrap 映射记录数：73,600
- bootstrap 已解码记录数：73,597

逐文件路径、大小和 SHA-256 保存在：
`.artifacts/analysis/source-tree-baseline.json`。

## 不可变规则

1. 不覆盖原始 JAR、原始 CFR 源码树或既有映射文件。
2. 新生成结果全部写入新的 `.artifacts/analysis` 或 `.artifacts/decompiled` 子目录。
3. 本机不启动完整客户端，不进行登录、联网或服务端探测。
4. 需要加载目标类或执行目标字节码的动作，只能在断网 Windows 虚拟机中手动进行。
5. 每个阶段先验证输入基线，再生成产物，并记录无法静态解析的项目。
