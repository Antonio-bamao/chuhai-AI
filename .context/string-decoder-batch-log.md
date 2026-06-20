# 字符串解码 Family 批次日志

## Batch 1｜2026-06-21｜确定性同构发现与高频样本验证

### 选择规则

1. 按调用数降序选择尚未注册的 family。
2. 定义必须唯一，并同时包含既有 AES-table 解码器的关键结构：
   `getStackTrace`、caller 类名/方法名、`toCharArray`、36 轮表运算、
   8 路字符异或、256 字节 S-box 和 30 项 Rcon。
3. 必须能唯一提取一个 long seed、8 个 suffix byte 和 4 个 caller 常量。
4. 提取器必须对原九个注册 family 的全部常量 9/9 精确复现。
5. 同名多定义不得猜测。

算法发现器在 348 个候选 family 中确认 347 个唯一同构定义；
唯一保留未决的是 `a$5$0.Z`，因为两个不同包中存在同名、不同定义。

### 高频验证样本

| Family | 调用数 | Caller 证据 | 明文样本 |
|---|---:|---|---|
| `MiJava$191$1$0.y` | 2,554 | `com.sbf.main.jxbrowser.a$1run` | `data` |
| `InsApiHelper$InsUserAgent.b` | 1,727 | `com.sbf.main.rpa.func.ins.a<init>` | `name` |
| `a$MiJava$60.B` | 912 | classfile synthetic method `com.sbf.main.jxbrowser.a$1a` | `access_token` |
| `f$JVKSpiderHelper2$1.T` | 622 | `com.sbf.main.rpa.func.a$1run` | `Menu0002001` |
| `a$a$11.Y` | 505 | `com.sbf.main.ext.open.zw.ads.a$2run` | `code` |
| `SafeProcessManager$SafeProcessManager.N` | 501 | `<clinit>` | `HTTPDebugger` |
| `d$StringHelper.o` | 478 | `com.sbf.util.BanWordsUtila` | `http` |
| `ADBrowser$4$0.I` | 467 | `com.sbf.main.ext.ads.ADBrowser$2run` | `browser` |
| `SecureRSAUtil$SBFApi$1.m` | 453 | `com.sbf.util.http.AESCBCHelpera` | `utf-8` |
| `f$j$1.w` | 449 | typed static method `com.sbf.main.rpa.func.aa` | `start.....` |

### 结果

- `decoded_static` family：347
- 已覆盖加密调用点：35,193
- 保守未决 family：1（`a$5$0.Z`，449 个调用点）
- 另发现两个传给解码方法的原始明文参数 `")"`、`"w"`；它们不含
  `\uXXXX`，不会误当作密文解码，后续统一导出时归入
  `decoded_existing_plaintext`。

### Caller 限制

CFR 会把 lambda 内的 synthetic 方法折叠回外层方法，源码行级 caller
不一定等于运行时 `StackTraceElement`。已验证的典型例子中，源码看似位于
`a$1.run`，实际 classfile caller 方法名为混淆后的 `a`。因此：

- 普通方法、构造器和静态初始化器可以静态确定；
- lambda/synthetic 调用必须结合 classfile 方法表或保守标记；
- 不以“能解出随机 Unicode”作为成功依据。
