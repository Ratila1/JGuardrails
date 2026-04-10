# JGuardrails

```
     _  _____                     _          _ _
    | ||  __ \                   | |        (_) |
    | || |  \] _   _  __ _ _ __  | |     __ _ _| |___
 _  | || | __ | | | |/ _` | '__| | |    / _` | | / __|
| |_| || |_\ \| |_| | (_| | |    | |___| (_| | | \__ \
 \___/  \____/ \__,_|\__,_|_|    \_____/\__,_|_|_|___/
```

[![Java 17+](https://img.shields.io/badge/Java-17%2B-blue.svg)](https://openjdk.org/projects/jdk/17/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)
[![JitPack](https://jitpack.io/v/Ratila1/JGuardrails.svg)](https://jitpack.io/#Ratila1/JGuardrails)

**The first Java guardrails library for LLM applications.**

JGuardrails is a framework-agnostic toolkit that adds programmable safety rails to any Java LLM application. Works with Spring AI, LangChain4j, or any custom LLM client — no vendor lock-in.

> A system prompt is a request. Guardrails are enforcement.

---

## Table of Contents

- [Why JGuardrails](#why-jguardrails)
- [How It Works](#how-it-works)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Built-in Rails](#built-in-rails)
- [Fluent API Reference](#fluent-api-reference)
- [YAML Configuration](#yaml-configuration)
- [Spring AI Integration](#spring-ai-integration)
- [LangChain4j Integration](#langchain4j-integration)
- [Custom Rails](#custom-rails)
- [Audit Logging](#audit-logging)
- [Metrics](#metrics)
- [Running Examples](#running-examples)
- [Building from Source](#building-from-source)

---

## Why JGuardrails

| | System Prompt | JGuardrails |
|---|---|---|
| Enforcement | Soft — LLM can ignore it | Hard — enforced at code level |
| Jailbreak resistance | No | Yes |
| PII masking | Not possible | Built-in |
| Audit trail | None | Every block/modify is logged |
| Added latency | 0 ms | 1–5 ms (pattern mode) |
| Framework dependency | LLM-specific | Framework-agnostic |

**Common problems JGuardrails solves:**

- User tries to jailbreak the LLM → request is blocked before it ever reaches the model
- User pastes email/phone/credit card number → PII is masked before being sent to the LLM
- LLM returns a toxic response → response is blocked before reaching the user
- User asks about forbidden topics → blocked by keyword matching
- You need a full history of all blocks and modifications → audit log included out of the box

---

## How It Works

```
User Input → [InputRail 1] → [InputRail 2] → ... → Your LLM
                                                        ↓
User        ← [OutputRail 1] ← [OutputRail 2] ← ... ←
```

Each rail returns one of three decisions:

| Decision | Meaning |
|---|---|
| **PASS** | Text continues to the next rail unchanged |
| **BLOCK** | Chain stops; user receives the `blockedResponse` message |
| **MODIFY** | Text is transformed (e.g., PII masked) and forwarded to the next rail |

**Important:** the pipeline does **not** call the LLM itself. Your code calls the LLM — the pipeline only processes text before and after.

---

## Installation

### Option 1 — JitPack (recommended, no local build needed)

Add JitPack to your repositories and declare the dependency:

**Gradle (Kotlin DSL):**

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    // Core + built-in detectors (required)
    implementation("com.github.Ratila1.JGuardrails:jguardrails-core:main-SNAPSHOT")
    implementation("com.github.Ratila1.JGuardrails:jguardrails-detectors:main-SNAPSHOT")

    // Spring AI adapter (optional)
    implementation("com.github.Ratila1.JGuardrails:jguardrails-spring-ai:main-SNAPSHOT")

    // LangChain4j adapter (optional)
    implementation("com.github.Ratila1.JGuardrails:jguardrails-langchain4j:main-SNAPSHOT")

    // LLM-as-judge support (optional)
    implementation("com.github.Ratila1.JGuardrails:jguardrails-llm:main-SNAPSHOT")
}
```

**Gradle (Groovy DSL):**

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

```groovy
// build.gradle
dependencies {
    implementation 'com.github.Ratila1.JGuardrails:jguardrails-core:main-SNAPSHOT'
    implementation 'com.github.Ratila1.JGuardrails:jguardrails-detectors:main-SNAPSHOT'
}
```

**Maven:**

```xml
<!-- pom.xml -->
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Ratila1.JGuardrails</groupId>
        <artifactId>jguardrails-core</artifactId>
        <version>main-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>com.github.Ratila1.JGuardrails</groupId>
        <artifactId>jguardrails-detectors</artifactId>
        <version>main-SNAPSHOT</version>
    </dependency>
</dependencies>
```

> **Tip:** replace `main-SNAPSHOT` with a specific Git tag (e.g., `v0.1.0`) or commit hash for reproducible builds.

### Option 2 — Build from source

```bash
git clone https://github.com/Ratila1/JGuardrails.git
cd JGuardrails
./gradlew publishToMavenLocal
```

Then in your project:

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("io.jguardrails:jguardrails-core:0.1.0-SNAPSHOT")
    implementation("io.jguardrails:jguardrails-detectors:0.1.0-SNAPSHOT")
}
```

---

## Quick Start

10 lines to get started:

```java
import io.jguardrails.core.RailContext;
import io.jguardrails.detectors.input.jailbreak.JailbreakDetector;
import io.jguardrails.detectors.input.pii.*;
import io.jguardrails.detectors.output.toxicity.ToxicityChecker;
import io.jguardrails.pipeline.GuardrailPipeline;

GuardrailPipeline pipeline = GuardrailPipeline.builder()
    .addInputRail(new JailbreakDetector())
    .addInputRail(PiiMasker.builder().entities(PiiEntity.EMAIL, PiiEntity.PHONE).build())
    .addOutputRail(new ToxicityChecker())
    .blockedResponse("I'm unable to process this request.")
    .build();

// Full cycle with your LLM in one line:
String safeResponse = pipeline.execute(
    userMessage,
    RailContext.empty(),
    processedInput -> myLlmClient.chat(processedInput)  // ← your LLM here
);
```

---

## Built-in Rails

### Input Rails

#### JailbreakDetector

Detects prompt injection and jailbreak attempts locally using regex — no API calls required.

```java
JailbreakDetector detector = JailbreakDetector.builder()
    .sensitivity(JailbreakDetector.Sensitivity.HIGH) // LOW | MEDIUM | HIGH
    .build();
```

**What it blocks:**
- `"Ignore previous instructions..."` / `"Forget all prior instructions..."`
- `"You are now DAN"` / `"Act as if you are..."` / `"Pretend to be..."`
- `"Developer mode enabled"` / `"Jailbreak mode"`
- Delimiter injection: ` ```system``` `, `[SYSTEM]`, `<<<override>>>`
- Patterns in English, Russian, German, French, Spanish

```java
// Add your own patterns:
JailbreakDetector detector = JailbreakDetector.builder()
    .sensitivity(JailbreakDetector.Sensitivity.MEDIUM)
    .addCustomPattern("reveal.*system.*prompt")
    .addCustomPattern("bypass.*filter")
    .build();
```

---

#### PiiMasker

Masks personally identifiable information before it reaches the LLM.

```java
PiiMasker masker = PiiMasker.builder()
    .entities(
        PiiEntity.EMAIL,         // john@example.com      → [EMAIL REDACTED]
        PiiEntity.PHONE,         // +1 555 000 1234       → [PHONE REDACTED]
        PiiEntity.CREDIT_CARD,   // 4276 1234 5678 9012   → [CREDIT_CARD REDACTED]
        PiiEntity.SSN,           // 123-45-6789           → [SSN REDACTED]
        PiiEntity.IBAN,          // DE89370400440532013000 → [IBAN REDACTED]
        PiiEntity.IP_ADDRESS,    // 192.168.1.1           → [IP_ADDRESS REDACTED]
        PiiEntity.DATE_OF_BIRTH  // 01/01/1990            → [DATE_OF_BIRTH REDACTED]
    )
    .strategy(PiiMaskingStrategy.REDACT)        // Full replacement (default)
    // .strategy(PiiMaskingStrategy.MASK_PARTIAL) // j***@g***.com  |  +1***1234
    // .strategy(PiiMaskingStrategy.HASH)         // [EMAIL:a3f8c2d1e4b5]
    .build();
```

---

#### TopicFilter

Blocks or allows requests based on topic keyword matching.

```java
// BLOCKLIST mode — listed topics are blocked, everything else is allowed:
TopicFilter filter = TopicFilter.builder()
    .blockTopics("politics", "religion", "violence", "adult", "drugs")
    .build();

// ALLOWLIST mode — only listed topics are allowed, everything else is blocked:
TopicFilter filter = TopicFilter.builder()
    .allowTopics("banking", "payments", "account")
    .build();

// Custom topics with your own keywords:
TopicFilter filter = TopicFilter.builder()
    .mode(TopicFilter.Mode.BLOCKLIST)
    .customTopic("competitors", "CompetitorX", "RivalCorp", "OtherProduct")
    .customTopic("legal_risk",  "lawsuit", "litigation", "court", "sue")
    .build();
```

**Built-in topics:** `politics`, `religion`, `violence`, `adult`, `drugs`, `medical_advice`, `financial_advice`

---

#### InputLengthValidator

Blocks inputs that exceed configured length limits (prevents context-overflow attacks).

```java
InputLengthValidator validator = InputLengthValidator.builder()
    .maxCharacters(5000)  // 0 = disabled
    .maxWords(800)        // 0 = disabled
    .build();
```

---

### Output Rails

#### ToxicityChecker

Blocks toxic LLM responses before they reach the user.

```java
ToxicityChecker checker = ToxicityChecker.builder()
    .categories(
        ToxicityChecker.Category.PROFANITY,    // offensive language
        ToxicityChecker.Category.HATE_SPEECH,  // discrimination, hate speech
        ToxicityChecker.Category.THREATS,      // threats and incitement to violence
        ToxicityChecker.Category.SELF_HARM     // self-harm content
    )
    .addBlockedWord("my_custom_word")
    .build();
```

---

#### OutputPiiScanner

Masks PII in LLM responses (in case the model recalls personal data from training).

```java
OutputPiiScanner scanner = OutputPiiScanner.builder()
    .entities(PiiEntity.EMAIL, PiiEntity.PHONE, PiiEntity.CREDIT_CARD)
    .strategy(PiiMaskingStrategy.MASK_PARTIAL)
    .build();
```

---

#### OutputLengthValidator

Limits the length of LLM responses.

```java
OutputLengthValidator validator = OutputLengthValidator.builder()
    .maxCharacters(2000)
    .truncate(true)   // true = truncate with "...", false = block
    .build();
```

---

#### JsonSchemaValidator

Validates that the LLM returned valid JSON (useful for structured output).

```java
JsonSchemaValidator validator = JsonSchemaValidator.builder()
    .requireValidJson(true)
    .build();
```

---

## Fluent API Reference

### Building a pipeline

```java
GuardrailPipeline pipeline = GuardrailPipeline.builder()
    // Input rails — executed in priority order (lower number = earlier)
    .addInputRail(InputLengthValidator.builder().maxCharacters(5000).build())  // priority=5
    .addInputRail(JailbreakDetector.builder().build())                         // priority=10
    .addInputRail(PiiMasker.builder().entities(PiiEntity.EMAIL).build())       // priority=20
    .addInputRail(TopicFilter.builder().blockTopics("violence").build())        // priority=30

    // Output rails
    .addOutputRail(ToxicityChecker.builder().build())                          // priority=10
    .addOutputRail(OutputPiiScanner.builder().build())                         // priority=20
    .addOutputRail(OutputLengthValidator.builder().maxCharacters(2000).build())// priority=30

    // Static blocked-response message:
    .blockedResponse("I'm unable to process this request.")
    // Or dynamic, based on context:
    // .onBlocked(ctx -> "Blocked for session: " + ctx.getSessionId().orElse("unknown"))

    // Fail strategy on rail exception:
    // true  = fail-open:   skip the broken rail and continue (lenient)
    // false = fail-closed: block the request (safe default)
    .failOpen(false)

    .auditLogger(new DefaultAuditLogger())
    .metrics(new DefaultMetrics())
    .build();
```

### Option 1 — Separate input/output processing

Use this when you need full control over each step:

```java
RailContext context = RailContext.builder()
    .sessionId("session-abc123")
    .userId("user-456")
    .attribute("language", "en")
    .build();

// Step 1: process input
PipelineExecutionResult inputResult = pipeline.processInput(userMessage, context);

if (inputResult.isBlocked()) {
    return inputResult.getText(); // returns blockedResponse — do not call LLM
}

// Step 2: call your LLM (pipeline never does this itself)
String llmResponse = myLlmClient.chat(inputResult.getText()); // text may be modified

// Step 3: process output
PipelineExecutionResult outputResult = pipeline.processOutput(llmResponse, userMessage, context);

return outputResult.getText(); // safe response (or blockedResponse if blocked)
```

### Option 2 — Single call with LLM callback

```java
String response = pipeline.execute(
    userMessage,
    context,
    processedInput -> myLlmClient.chat(processedInput)
);
```

### Inspecting PipelineExecutionResult

```java
PipelineExecutionResult result = pipeline.processInput(userMessage, context);

result.isBlocked();         // whether the pipeline blocked this request
result.getText();           // final text (or blockedResponse if blocked)
result.getOriginalText();   // original text before any rails ran
result.getExecutionTime();  // Duration — total pipeline execution time
result.getRailResults();    // List<RailResult> — result from every rail

// Details of the blocking rail (if blocked):
result.getBlockingResult().ifPresent(r -> {
    System.out.println("Blocked by: " + r.railName());
    System.out.println("Reason:     " + r.reason());
    System.out.println("Confidence: " + r.confidence()); // 0.0–1.0
    System.out.println("Metadata:   " + r.metadata());
});
```

### Passing data between rails via RailContext

```java
RailContext context = RailContext.builder()
    .sessionId("ses-123")           // session ID for audit
    .userId("usr-456")              // user ID for audit
    .addHistory("previous message") // conversation history
    .attribute("region", "EU")      // arbitrary attributes
    .attribute("role", "admin")
    .build();

// Inside any rail, read and write attributes:
context.getAttribute("region", String.class); // Optional<String>
context.setAttribute("detectedLanguage", "en"); // visible to downstream rails
```

---

## YAML Configuration

Configure the entire pipeline via YAML without recompiling.

### Create `src/main/resources/guardrails.yml`

```yaml
jguardrails:

  # Behavior when a rail throws an exception
  # closed = block the request (safer, default)
  # open   = skip the broken rail and continue
  fail-strategy: closed

  # Message returned when a request is blocked
  blocked-response: "I'm unable to process this request. Please rephrase and try again."

  # Input rails — executed in priority order (lower = earlier)
  input-rails:

    - type: input-length
      enabled: true
      priority: 5
      config:
        max-characters: 8000

    - type: jailbreak-detect
      enabled: true
      priority: 10
      config:
        sensitivity: high       # low | medium | high
        mode: pattern           # pattern | llm-judge | hybrid

    - type: pii-mask
      enabled: true
      priority: 20
      config:
        entities:
          - EMAIL
          - PHONE
          - CREDIT_CARD
          - IBAN
        strategy: redact        # redact | mask-partial | hash

    - type: topic-filter
      enabled: true
      priority: 30
      config:
        mode: blocklist         # blocklist | allowlist
        topics:
          - violence
          - adult
          - drugs
        custom-topics:
          competitors:
            - "CompetitorName"
            - "RivalProduct"

  # Output rails
  output-rails:

    - type: toxicity-check
      enabled: true
      priority: 10
      config:
        categories:
          - PROFANITY
          - HATE_SPEECH
          - THREATS
          - SELF_HARM

    - type: output-pii-scan
      enabled: true
      priority: 20
      config:
        entities:
          - EMAIL
          - PHONE
        strategy: mask-partial

    - type: output-length
      enabled: true
      priority: 30
      config:
        max-characters: 3000
        truncate: true          # true = truncate with "...", false = block

  # Audit logging
  audit:
    enabled: true
    log-level: INFO
    include-original-text: false  # keep false for privacy

  # Metrics
  metrics:
    enabled: true
```

### Loading in code

```java
// From classpath (src/main/resources/)
GuardrailConfig config = YamlConfigLoader.loadFromClasspath("guardrails.yml");

// From filesystem
GuardrailConfig config = YamlConfigLoader.load(Path.of("/etc/myapp/guardrails.yml"));

// From any InputStream
GuardrailConfig config = YamlConfigLoader.loadFromStream(inputStream);
```

---

## Spring AI Integration

### Dependency

```kotlin
implementation("com.github.Ratila1.JGuardrails:jguardrails-spring-ai:main-SNAPSHOT")
```

### Option 1 — Auto-configuration (recommended)

Just add the dependency and create `guardrails.yml`. Spring Boot auto-configures everything via `GuardrailAutoConfiguration`.

```yaml
# application.yml
jguardrails:
  enabled: true
  config-path: classpath:guardrails.yml
```

`GuardrailPipeline` and `GuardrailAdvisor` are created automatically as Spring beans. No additional code needed.

### Option 2 — Manual configuration

```java
@Configuration
public class LlmConfig {

    @Bean
    public GuardrailPipeline guardrailPipeline() {
        return GuardrailPipeline.builder()
            .addInputRail(new JailbreakDetector())
            .addInputRail(PiiMasker.builder()
                .entities(PiiEntity.EMAIL, PiiEntity.PHONE)
                .build())
            .addOutputRail(new ToxicityChecker())
            .blockedResponse("I'm unable to process this request.")
            .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, GuardrailPipeline pipeline) {
        return builder
            .defaultAdvisors(new GuardrailAdvisor(pipeline))
            .build();
    }
}
```

### Using in a service

```java
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String chat(String userMessage) {
        // Guardrails are applied automatically via the Advisor
        return chatClient.prompt()
            .user(userMessage)
            .call()
            .content();
    }
}
```

---

## LangChain4j Integration

### Dependency

```kotlin
implementation("com.github.Ratila1.JGuardrails:jguardrails-langchain4j:main-SNAPSHOT")
```

### Option 1 — GuardrailChatModelFilter (transparent wrapper)

Wraps any `ChatLanguageModel`. All `generate()` calls automatically pass through the pipeline.

```java
ChatLanguageModel baseModel = OpenAiChatModel.builder()
    .apiKey(System.getenv("OPENAI_API_KEY"))
    .modelName("gpt-4o")
    .build();

GuardrailPipeline pipeline = GuardrailPipeline.builder()
    .addInputRail(new JailbreakDetector())
    .addInputRail(PiiMasker.builder().entities(PiiEntity.EMAIL).build())
    .addOutputRail(new ToxicityChecker())
    .blockedResponse("Request blocked.")
    .build();

// Wrap the model — guardrails are applied transparently
ChatLanguageModel guardedModel = new GuardrailChatModelFilter(baseModel, pipeline);

String response = guardedModel.generate("Tell me about Java 21");
```

### Option 2 — GuardrailAiServiceInterceptor (for AiServices)

```java
interface MyAssistant {
    String chat(String userMessage);
}

MyAssistant assistant = AiServices.builder(MyAssistant.class)
    .chatLanguageModel(model)
    .build();

GuardrailAiServiceInterceptor interceptor = new GuardrailAiServiceInterceptor(pipeline);

// Wrap the AiService call:
String response = interceptor.intercept(
    userInput,
    processedInput -> assistant.chat(processedInput)
);
```

---

## Custom Rails

Creating a custom rail requires implementing a single method.

### Custom InputRail

```java
public class CompanyPolicyRail implements InputRail {

    @Override
    public String name() {
        return "company-policy";
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public RailResult process(String input, RailContext context) {
        if (input.toLowerCase().contains("confidential")) {
            return RailResult.block(name(), "Input contains restricted keyword 'confidential'");
        }
        return RailResult.pass(input, name());
    }
}
```

### Custom OutputRail

```java
public class DisclaimerRail implements OutputRail {

    private static final String DISCLAIMER =
        "\n\n*This response was generated by AI and does not constitute professional advice.*";

    @Override
    public String name() { return "disclaimer-appender"; }

    @Override
    public int priority() { return 200; } // run last

    @Override
    public RailResult process(String output, String originalInput, RailContext context) {
        return RailResult.modify(output + DISCLAIMER, name(), "Appended legal disclaimer");
    }
}
```

### Dynamic enable/disable

```java
public class ToggleableRail implements InputRail {

    private volatile boolean enabled = true;

    @Override
    public String name() { return "toggleable"; }

    @Override
    public boolean isEnabled() { return enabled; } // pipeline checks this before calling process()

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    @Override
    public RailResult process(String input, RailContext context) {
        // your logic
        return RailResult.pass(input, name());
    }
}
```

### Register in pipeline

```java
GuardrailPipeline pipeline = GuardrailPipeline.builder()
    .addInputRail(new CompanyPolicyRail())
    .addOutputRail(new DisclaimerRail())
    .build();
```

---

## Audit Logging

By default, `DefaultAuditLogger` writes to SLF4J:
- `WARN` for every block
- `INFO` for every modification

```
[GUARDRAIL AUDIT] BLOCKED by rail='jailbreak-detector' reason='Prompt injection detected' at 2024-...
[GUARDRAIL AUDIT] MODIFIED by rail='pii-masker' reason='Masked 2 PII entities' at 2024-...
```

### InMemoryAuditLogger for tests

```java
InMemoryAuditLogger auditLogger = new InMemoryAuditLogger();

GuardrailPipeline pipeline = GuardrailPipeline.builder()
    .addInputRail(new JailbreakDetector())
    .auditLogger(auditLogger)
    .build();

pipeline.processInput("bad input", context);

// Assert in tests:
assertThat(auditLogger.getEntries()).hasSize(1);
assertThat(auditLogger.getEntries().get(0).getType()).isEqualTo(AuditEntry.Type.BLOCKED);
assertThat(auditLogger.getEntries().get(0).getRailName()).isEqualTo("jailbreak-detector");

// Filter by type:
List<AuditEntry> blocks         = auditLogger.getEntries(AuditEntry.Type.BLOCKED);
List<AuditEntry> modifications  = auditLogger.getEntries(AuditEntry.Type.MODIFIED);
```

### Custom AuditLogger (e.g., write to database)

```java
public class DatabaseAuditLogger implements AuditLogger {

    private final AuditRepository repo;

    @Override
    public void log(AuditEntry entry) {
        repo.save(new AuditRecord(
            entry.getTimestamp(),
            entry.getType().name(),
            entry.getRailName(),
            entry.getReason()
        ));
    }
}
```

---

## Metrics

`DefaultMetrics` keeps in-memory counters (thread-safe, backed by `LongAdder`).

```java
DefaultMetrics metrics = new DefaultMetrics();

GuardrailPipeline pipeline = GuardrailPipeline.builder()
    .metrics(metrics)
    .build();

MetricsSnapshot snapshot = metrics.getSnapshot();

snapshot.totalBlocked();    // long — total requests blocked
snapshot.totalModified();   // long — total texts modified
snapshot.totalPassed();     // long — total requests passed
snapshot.totalErrors();     // long — rail errors
snapshot.blockedByRail();   // Map<String, Long> — per rail
snapshot.modifiedByRail();  // Map<String, Long>
```

### Custom GuardrailMetrics (Micrometer / Prometheus)

```java
public class MicrometerGuardrailMetrics implements GuardrailMetrics {

    private final MeterRegistry registry;

    @Override
    public void recordBlock(String railName) {
        registry.counter("guardrail.blocks", "rail", railName).increment();
    }

    @Override
    public void recordModification(String railName) {
        registry.counter("guardrail.modifications", "rail", railName).increment();
    }

    @Override
    public void recordPass(String railName) {
        registry.counter("guardrail.passes", "phase", railName).increment();
    }

    @Override
    public void recordError(String railName) {
        registry.counter("guardrail.errors", "rail", railName).increment();
    }
}
```

---

## Running Examples

All examples are in the `jguardrails-examples` module and require **no LLM API key**.

```bash
# Basic example: jailbreak detection, PII masking, toxicity check
./gradlew :jguardrails-examples:run -PmainClass=io.jguardrails.examples.BasicExample

# YAML configuration example with audit log output
./gradlew :jguardrails-examples:run -PmainClass=io.jguardrails.examples.YamlConfigExample

# Custom rails: language restriction + disclaimer appender
./gradlew :jguardrails-examples:run -PmainClass=io.jguardrails.examples.CustomRailExample
```

---

## Building from Source

```bash
git clone https://github.com/Ratila1/JGuardrails.git
cd JGuardrails

# Build all modules
./gradlew build

# Run all tests
./gradlew test

# Run a specific test class
./gradlew :jguardrails-detectors:test --tests "*.JailbreakDetectorTest"

# Publish to local Maven (~/.m2)
./gradlew publishToMavenLocal
```

### Module overview

```
JGuardrails/
├── jguardrails-core/          # Interfaces, pipeline, config, audit, metrics
├── jguardrails-detectors/     # Built-in rails (jailbreak, PII, toxicity, ...)
├── jguardrails-llm/           # LLM clients for LLM-as-judge
├── jguardrails-spring-ai/     # Spring AI Advisor + AutoConfiguration
├── jguardrails-langchain4j/   # LangChain4j ChatLanguageModel wrapper
└── jguardrails-examples/      # Runnable examples with no external dependencies
```

---

## License

[Apache License 2.0](LICENSE)
