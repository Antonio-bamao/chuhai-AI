# SBF Runtime Trace Agent Implementation Plan

> **For the implementer:** Use `superpowers:executing-plans` to execute this plan task by task.

**Goal:** Build a Java 8-compatible, shaded Byte Buddy `-javaagent` that locally traces the four specified SBF client boundaries without modifying `data/app/App.dll`.

**Architecture:** The agent has a small `premain` entry point, a Byte Buddy installer with exact matchers and an opt-in narrow fallback policy, advice classes that only observe entry/exit, and a bounded/masked console sink.  `java.net.URI` receives its own bootstrap-visible advice path, while application classes are transformed through normal retransformation.

**Tech Stack:** Java 8, Maven, Byte Buddy, JUnit 5 (Java 8-compatible release), Maven Shade Plugin, PowerShell.

---

### Task 1: Create the isolated Maven agent project and red tests for output safety

**Files:**
- Create: `tools/sbf-runtime-trace-agent/pom.xml`
- Create: `tools/sbf-runtime-trace-agent/src/main/java/com/codex/sbftrace/TraceSink.java`
- Create: `tools/sbf-runtime-trace-agent/src/test/java/com/codex/sbftrace/TraceSinkTest.java`

**Step 1: Write the failing tests.**

Cover UTF-8 plus hexadecimal rendering for a `String`, ByteBuffer logging without changing its position/limit, the 2 KiB byte cap, and masking of `token`, `cookie`, `password`, and `authorization` before plain and hex output.

**Step 2: Run the focused test to verify it fails.**

Run: `mvn -q -Dtest=TraceSinkTest test`

Expected: compilation/test failure because the sink has not been implemented.

**Step 3: Implement the smallest safe sink.**

Use one synchronized console write per record. Render `String` and a duplicate of each `ByteBuffer` as escaped UTF-8 and lowercase hex; never mutate the original buffer. Apply masking before rendering, cap each byte sequence at 2048 bytes, label null/void and thrown values distinctly, and never open a network connection or write payloads to disk.

**Step 4: Re-run the focused test.**

Run: `mvn -q -Dtest=TraceSinkTest test`

Expected: PASS.

### Task 2: Implement advice and exact method matching

**Files:**
- Create: `tools/sbf-runtime-trace-agent/src/main/java/com/codex/sbftrace/TraceAdvice.java`
- Create: `tools/sbf-runtime-trace-agent/src/main/java/com/codex/sbftrace/TraceInstaller.java`
- Create: `tools/sbf-runtime-trace-agent/src/test/java/com/codex/sbftrace/TraceTarget.java`
- Create: `tools/sbf-runtime-trace-agent/src/test/java/com/codex/sbftrace/TraceAdviceTest.java`

**Step 1: Write a failing transformer test.**

Define a synthetic target with `send(String)`, `onMessage(ByteBuffer)`, and `connect()` methods. Transform it through the same advice builder, invoke each method, and assert the captured output identifies the class/method and contains both entry arguments and exit return/void values.

**Step 2: Run it to verify it fails.**

Run: `mvn -q -Dtest=TraceAdviceTest test`

Expected: failing/compilation error until advice and matchers exist.

**Step 3: Implement advice and exact matcher methods.**

Use Byte Buddy `Advice` entry/exit hooks with `suppress = Throwable` so tracing cannot change business behavior. Exact matchers must be limited to:

- `com.sbf.main.ext.ios.a` + `send(String)`;
- `com.sbf.main.ext.ios.a` + `onMessage(ByteBuffer)`;
- `org.java_websocket.client.WebSocketClient` + zero-argument `connect()`.

Build a test-only installer entry that accepts a target matcher so the synthetic target proves the entry/exit behavior without requiring App.dll classes at Maven compile time.

**Step 4: Re-run the focused test.**

Run: `mvn -q -Dtest=TraceAdviceTest test`

Expected: PASS.

### Task 3: Add the `premain` entry point, retransformation, and URI bootstrap tracing

**Files:**
- Create: `tools/sbf-runtime-trace-agent/src/main/java/com/codex/sbftrace/TraceAgent.java`
- Modify: `tools/sbf-runtime-trace-agent/src/main/java/com/codex/sbftrace/TraceInstaller.java`
- Modify: `tools/sbf-runtime-trace-agent/src/main/java/com/codex/sbftrace/TraceAdvice.java`
- Create: `tools/sbf-runtime-trace-agent/src/test/java/com/codex/sbftrace/UriSmokeMain.java`
- Create: `tools/sbf-runtime-trace-agent/src/test/java/com/codex/sbftrace/UriAgentSmokeTest.java`

**Step 1: Write a failing separate-process URI smoke test.**

The test must launch the current Java executable with the built agent JAR as `-javaagent`, run `UriSmokeMain`, create a URI, and assert that stdout includes a `java.net.URI#<init>` record. It must not use App.dll.

**Step 2: Run it to verify it fails.**

Run: `mvn -q -Dtest=UriAgentSmokeTest test`

Expected: failure until premain packaging and bootstrap visibility are correct.

**Step 3: Implement agent installation.**

Set a retransformation-capable `AgentBuilder` and do not ignore `java.net.URI`. Use a bootstrap injection strategy, with all advice/sink helpers needed by URI placed where bootstrap-loaded instrumented bytecode can resolve them. Transform only URI constructors and install an explicit listener that writes concise transform/error diagnostics to stderr. Determine the running agent JAR from `TraceAgent`’s code source; fail closed with a clear message if it is not a JAR.

**Step 4: Re-run the smoke test.**

Run: `mvn -q -Dtest=UriAgentSmokeTest test`

Expected: PASS on Java 8.

### Task 4: Add constrained opt-in fallback matching

**Files:**
- Create: `tools/sbf-runtime-trace-agent/src/main/java/com/codex/sbftrace/MatchPolicy.java`
- Modify: `tools/sbf-runtime-trace-agent/src/main/java/com/codex/sbftrace/TraceInstaller.java`
- Create: `tools/sbf-runtime-trace-agent/src/test/java/com/codex/sbftrace/MatchPolicyTest.java`

**Step 1: Write failing matcher tests.**

Assert that fallback is disabled without `fallback=true`; enabled iOS fallback still requires `nameContains("ios")` plus the exact `send(String)`/`onMessage(ByteBuffer)` shape; and WebSocket fallback combines `nameContains("WebSocket")` with `isSubTypeOf` the known WebSocket base type. Assert unrelated methods/classes are excluded.

**Step 2: Run it to verify it fails.**

Run: `mvn -q -Dtest=MatchPolicyTest test`

Expected: FAIL until the policy parser and matchers exist.

**Step 3: Implement minimal fallback policy.**

Parse agent arguments as simple comma-separated `key=value` settings. Only install fallback type matchers when `fallback=true`, emit a local diagnostic for each fallback transform, and retain exact method-signature constraints. Do not install a broad all-method or all-class hook.

**Step 4: Re-run the focused test.**

Run: `mvn -q -Dtest=MatchPolicyTest test`

Expected: PASS.

### Task 5: Configure shaded packaging and verify Java 8 build output

**Files:**
- Modify: `tools/sbf-runtime-trace-agent/pom.xml`
- Create: `tools/sbf-runtime-trace-agent/README.md`

**Step 1: Add Java 8 build configuration.**

Set source/target 1.8, the Byte Buddy and test dependencies, Surefire configuration, and Maven Shade output. Write manifest entries `Premain-Class`, `Agent-Class`, `Can-Redefine-Classes`, and `Can-Retransform-Classes` into the shaded artifact.

**Step 2: Build and inspect the artifact.**

Run: `mvn clean package`

Run: `jar tf target/sbf-runtime-trace-agent.jar | Select-String 'TraceAgent|net/bytebuddy'`

Run: `jar xf target/sbf-runtime-trace-agent.jar META-INF/MANIFEST.MF; Get-Content META-INF/MANIFEST.MF`

Expected: all tests pass; the JAR contains agent and Byte Buddy classes; the manifest has the four requested agent attributes.

**Step 3: Write the operational README.**

Document the build command, log record format and limits, the non-invasive launch form, and exactly where a future launcher build changes: `.artifacts/working/m4b-v50-local-launcher-src/HuoChaiAILocalLauncher.cs:43`, inserting the `-javaagent:` argument before `-jar App.dll`. State that this source is an inspected launcher source reference; this task does not edit it, the existing launcher binary, or App.dll. Include an explicit command for direct diagnostic launch and `fallback=true` usage.

### Task 6: Full verification and read-only client integrity check

**Files:**
- Modify: `tools/sbf-runtime-trace-agent/README.md` only if command output reveals a correction.

**Step 1: Run the full project suite and package.**

Run: `mvn clean test package`

Expected: PASS.

**Step 2: Verify package metadata and URI smoke output again.**

Run: `mvn -q -Dtest=UriAgentSmokeTest test`

Run: `Get-FileHash data/app/App.dll -Algorithm SHA256`

Expected: URI trace evidence is present; App.dll remains `63135EA6272140F9C816357F63554F72F122A8EF60AC8E8394B1E5F365185F71`.

**Step 3: Inspect the final diff.**

Run: `git status --short; git diff -- tools/sbf-runtime-trace-agent docs/superpowers/plans/2026-07-13-sbf-runtime-trace-agent.md`

Expected: changes are restricted to the independent agent project and its documentation/plan; no client, configuration, collector-chain, or launcher binary files are changed.
