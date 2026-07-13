# SBF Runtime Trace Agent Design

## Goal

Provide a standalone Java 8 `-javaagent` for the SBF desktop client. The agent observes selected iOS/WebSocket boundaries without modifying `data/app/App.dll` and writes local diagnostic records to standard output.

## Scope

The agent instruments these targets at runtime:

1. `com.sbf.main.ext.ios.a#send(String)`.
2. `com.sbf.main.ext.ios.a#onMessage(java.nio.ByteBuffer)`.
3. Every `java.net.URI` constructor.
4. `org.java_websocket.client.WebSocketClient#connect`.

It records the timestamp, class and method, arguments, returned value, and thrown exception. `String` and `ByteBuffer` values are rendered as UTF-8 plus hexadecimal bytes.

The agent does not send data, open sockets, alter the client application, or modify App.dll.

## Architecture

- `TraceAgent`: `premain(String, Instrumentation)` entry point.
- `TraceInstaller`: installs Byte Buddy `Advice` transformers with retransformation enabled.
- `TraceAdvice`: entry/exit advice for the application and WebSocket methods; constructor exit advice for URI.
- `TraceSink`: thread-safe local console renderer. It bounds output to 2 KiB per byte sequence and masks credential-like query/header keys (`token`, `cookie`, `password`, `authorization`).
- `MatchPolicy`: exact names are installed first. Opt-in fallback matching is activated by an agent argument such as `fallback=true`; it uses `nameContains` plus a type constraint and reports every fallback class it matches.

`java.net.URI` is a bootstrap-loaded class, so the installer uses Byte Buddy bootstrap injection and a redefinition/retransformation-capable strategy. Helper classes referenced by URI advice must be visible to the bootstrap loader.

## Matching policy

Exact rules:

- `named("com.sbf.main.ext.ios.a")` plus `named("send").and(takesArguments(String.class))`.
- `named("com.sbf.main.ext.ios.a")` plus `named("onMessage").and(takesArguments(ByteBuffer.class))`.
- `named("java.net.URI")` plus `isConstructor()`.
- `named("org.java_websocket.client.WebSocketClient")` plus `named("connect").and(takesArguments(0))`.

Fallback is narrow and disabled by default. The iOS fallback requires a class name containing `ios` and a method named `send` or `onMessage` with the expected argument type. The WebSocket fallback requires a class name containing `WebSocket` and `isSubTypeOf(WebSocketClient.class)` when the dependency is resolvable. It never applies a broad all-method hook.

## Packaging and startup

Maven Shade creates a self-contained agent JAR with manifest entries:

- `Premain-Class: ...TraceAgent`
- `Agent-Class: ...TraceAgent`
- `Can-Redefine-Classes: true`
- `Can-Retransform-Classes: true`

The project's Java 8 command currently launches `App.dll` from `data/app`. The agent is attached by inserting an absolute argument such as `-javaagent:H:\\project\\sbf-runtime-trace-agent\\target\\sbf-runtime-trace-agent.jar` before `-jar App.dll` in the local launcher command. This changes startup arguments only; it does not rewrite the JAR.

## Validation

- Unit tests cover value rendering, masking, truncation, and ByteBuffer position preservation.
- A synthetic test target verifies entry/exit logs for String and ByteBuffer methods.
- A smoke test runs a separate Java process with `-javaagent` and confirms URI construction is logged.
- `mvn test` and `mvn package` must pass using a Java 8-compatible Maven/bytecode target.

## Non-goals

- No bypassing of authentication, CAPTCHA, certificate, anti-bot, or fingerprint controls.
- No credential export, remote telemetry, data persistence, or automatic client launch.
- No change to App.dll, `.cnf`, client code, or collection chain.
