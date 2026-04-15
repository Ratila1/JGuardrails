# JGuardrails

```
   $$$$$╲  $$$$$$╲                                      $$╲                    $$╲ $$╲           
   ╲__$$ │$$  __$$╲                                     $$ │                   ╲__│$$ │          
      $$ │$$ ╱  ╲__│$$╲   $$╲  $$$$$$╲   $$$$$$╲   $$$$$$$ │ $$$$$$╲  $$$$$$╲  $$╲ $$ │ $$$$$$$╲ 
      $$ │$$ │$$$$╲ $$ │  $$ │ ╲____$$╲ $$  __$$╲ $$  __$$ │$$  __$$╲ ╲____$$╲ $$ │$$ │$$  _____│
$$╲   $$ │$$ │╲_$$ │$$ │  $$ │ $$$$$$$ │$$ │  ╲__│$$ ╱  $$ │$$ │  ╲__│$$$$$$$ │$$ │$$ │╲$$$$$$╲  
$$ │  $$ │$$ │  $$ │$$ │  $$ │$$  __$$ │$$ │      $$ │  $$ │$$ │     $$  __$$ │$$ │$$ │ ╲____$$╲ 
╲$$$$$$  │╲$$$$$$  │╲$$$$$$  │╲$$$$$$$ │$$ │      ╲$$$$$$$ │$$ │     ╲$$$$$$$ │$$ │$$ │$$$$$$$  │
 ╲______╱  ╲______╱  ╲______╱  ╲_______│╲__│       ╲_______│╲__│      ╲_______│╲__│╲__│╲_______╱ 
                                                                                                 
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
- [Custom Patterns and Engines](#custom-patterns-and-engines)
- [Spring AI Integration](#spring-ai-integration)
- [LangChain4j Integration](#langchain4j-integration)
- [Custom Rails](#custom-rails)
- [Audit Logging](#audit-logging)
- [Metrics](#metrics)
- [Running Examples](#running-examples)
- [Building from Source](#building-from-source)
- [What's New in 1.0.0](#whats-new-in-100)

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

### Known limitations

- Detection is pattern-based (regex + Aho-Corasick), without semantic understanding — JGuardrails is a guardrail layer, not a complete security solution.
- Officially tuned and tested languages for jailbreak/toxicity (regex): EN / RU / DE / FR / ES / PL / IT. Keyword detection also covers JA / ZH / AR / HI / TR / KO.
- Obfuscated toxicity (full leet, heavy spacing, reversed text) and sophisticated social-engineering prompts can still pass.
- PII patterns are intentionally conservative and may sometimes mask technical identifiers (UUIDs, ticket numbers, etc.).
- For high-risk or regulated use cases, JGuardrails should be combined with additional LLM- or ML-based safety systems.

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

**Gradle (Kotlin DSL):**

Step 1 — add JitPack to `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Step 2 — add dependencies to `build.gradle.kts`:

```kotlin
dependencies {
    // Core + built-in detectors (required)
    implementation("com.github.Ratila1:JGuardrails:v1.0.0")

    // Spring AI adapter (optional)
    implementation("com.github.Ratila1.JGuardrails:jguardrails-spring-ai:v1.0.0")

    // LangChain4j adapter (optional)
    implementation("com.github.Ratila1.JGuardrails:jguardrails-langchain4j:v1.0.0")

    // LLM-as-judge support (optional)
    implementation("com.github.Ratila1.JGuardrails:jguardrails-llm:v1.0.0")
}
```

**Gradle (Groovy DSL):**

```groovy
// settings.gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

```groovy
// build.gradle
dependencies {
    implementation 'com.github.Ratila1:JGuardrails:v1.0.0'
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
        <version>v1.0.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.Ratila1.JGuardrails</groupId>
        <artifactId>jguardrails-detectors</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

> **Tip:** replace `v1.0.0` with `master-SNAPSHOT` to always get the latest build from the master branch.

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
    implementation("io.jguardrails:jguardrails-core:1.0.0")
    implementation("io.jguardrails:jguardrails-detectors:1.0.0")
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

Detects prompt injection and jailbreak attempts locally — no API calls required.

Uses a **hybrid engine**: regex for complex structural patterns, Aho-Corasick automaton for O(n) literal phrase matching. Both engines run in parallel and the result with the earlier position in text wins.

```java
JailbreakDetector detector = JailbreakDetector.builder()
    .sensitivity(JailbreakDetector.Sensitivity.HIGH) // LOW | MEDIUM | HIGH
    .build();
```

**What it blocks:**
- `"Ignore previous instructions..."` / `"Forget all prior instructions..."`
- `"You are now DAN"` / `"Act as if you are..."` / `"Pretend to be..."`
- `"Developer mode enabled"` / `"Jailbreak mode"` / `"bypass safety filter"`
- Delimiter injection: ` ```system``` `, `[SYSTEM]`, `<<<override>>>`
- Patterns in English, Russian, German, French, Spanish, Polish, Italian
- Literal phrases in Japanese (Aho-Corasick)

```java
// Add your own patterns:
JailbreakDetector detector = JailbreakDetector.builder()
    .sensitivity(JailbreakDetector.Sensitivity.MEDIUM)
    .addCustomPattern("reveal.*system.*prompt")
    .addCustomPattern("bypass.*filter")
    .build();

// Load patterns from your own YAML file (replaces defaults):
detector = JailbreakDetector.builder()
    .patternsFromFile(myFile, "my_jailbreak_section")
    .build();

// Extend defaults with extra patterns from a YAML file:
detector = JailbreakDetector.builder()
    .addPatternsFromFile(myFile, "extra_jailbreak_section")
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

Uses the same **hybrid engine** as `JailbreakDetector`: regex for structural patterns, Aho-Corasick for literal phrases. Multilingual keyword matching (ZH / JA / AR / HI / TR / KO) runs as a second phase via `KeywordMatcher`.

```java
ToxicityChecker checker = ToxicityChecker.builder()
    .categories(
        ToxicityChecker.Category.PROFANITY,         // offensive language
        ToxicityChecker.Category.HATE_SPEECH,       // discrimination, hate speech
        ToxicityChecker.Category.THREATS,           // threats and incitement to violence
        ToxicityChecker.Category.SELF_HARM,         // self-harm content
        ToxicityChecker.Category.THIRD_PERSON_ABUSE // insults / death-wishes about absent persons
    )
    .addBlockedWord("my_custom_word")
    .build();
```

**Supported languages:**
- Regex patterns: EN / RU / FR / DE / ES / PL / IT
- Keyword (Aho-Corasick): EN + JA — hate phrases, threats, aggressive dismissals
- Multilingual keyword phase: ZH / JA / AR / HI / TR / KO

**`THIRD_PERSON_ABUSE`** category covers:
- *pronoun + copula + insult*: "he is an idiot", "she is worthless"
- *dehumanising phrases*: "waste of space", "not worth anything"
- *third-person death wishes*: "she should die", "he doesn't deserve to live"

```java
// Disable multilingual detection (e.g., for performance):
ToxicityChecker checker = ToxicityChecker.builder()
    .multilingualEnabled(false)
    .build();

// Load patterns from your own YAML file:
checker = ToxicityChecker.builder()
    .patternsFromFile(myFile, "my_toxicity_section")
    .build();

// Extend defaults:
checker = ToxicityChecker.builder()
    .addPatternsFromFile(myFile, "extra_toxicity_section")
    .build();

// Replace multilingual keywords:
checker = ToxicityChecker.builder()
    .keywordsFromFile(myKeywordsFile)
    .build();

// Add keywords on top of defaults:
checker = ToxicityChecker.builder()
    .addKeywordsFromFile(myKeywordsFile)
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
          - THIRD_PERSON_ABUSE

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

## Custom Patterns and Engines

JGuardrails 1.0.0 exposes the full pattern-matching stack so you can extend or replace every part.

### Pattern types in YAML

Each entry in a pattern YAML file can declare `type: REGEX` (default) or `type: KEYWORD`:

```yaml
my_section:
  # Regex — compiled to java.util.regex.Pattern, supports \b, lookaheads, etc.
  - id: MY_REGEX_PATTERN
    flags: CI
    pattern: "ignore\\s+all\\s+instructions"

  # Keyword — matched by Aho-Corasick O(n) engine, case-insensitive.
  # Preferred for literal phrases and for CJK / Arabic / Devanagari scripts
  # where \b word boundaries are undefined.
  - id: MY_KEYWORD_PHRASE
    type: KEYWORD
    pattern: "bypass safety filter"
```

### PatternSpec and matching engines

```java
// PatternSpec carries id, category, and type (REGEX or KEYWORD):
PatternSpec spec = new PatternSpec("MY_ID", "my_category", PatternSpec.Type.KEYWORD);

// RegexPatternEngine — matches one REGEX spec against text:
RegexPatternEngine regexEngine = RegexPatternEngine.builder()
    .register("MY_ID", Pattern.compile("my regex", Pattern.CASE_INSENSITIVE))
    .build();

// KeywordAutomatonEngine — Aho-Corasick multi-phrase matching:
KeywordAutomatonEngine kwEngine = new KeywordAutomatonEngine(
    Map.of("KW_1", "bypass filter", "KW_2", "ignore instructions")
);

// CompositePatternEngine — routes each spec to the correct sub-engine by type:
CompositePatternEngine engine = new CompositePatternEngine(regexEngine, kwEngine);

// findFirst() — single call that dispatches KEYWORD specs to Aho-Corasick
// and REGEX specs to the regex engine; returns the earliest match in text:
Optional<MatchedSpec> hit = engine.findFirst(text, activeSpecs);
hit.ifPresent(ms -> {
    System.out.println("Matched id: "       + ms.spec().id());
    System.out.println("Category:   "       + ms.spec().category());
    System.out.println("Engine type: "      + ms.spec().type());
    System.out.println("Matched text: "     + ms.result().matchedText());
    System.out.println("Position: "         + ms.result().start() + "–" + ms.result().end());
});
```

### Plug in a custom engine

```java
// Implement TextPatternEngine with your own logic (ML model, bloom filter, etc.):
TextPatternEngine myEngine = new TextPatternEngine() {
    @Override
    public MatchResult find(String text, PatternSpec spec) { /* ... */ }
};

JailbreakDetector detector = JailbreakDetector.builder()
    .engine(myEngine)
    .build();
```

### Load patterns from a YAML file

```java
// Replace all default patterns with patterns from a file:
JailbreakDetector detector = JailbreakDetector.builder()
    .patternsFromFile(Path.of("my-patterns.yml"), "custom_section")
    .build();

// Add patterns on top of defaults:
detector = JailbreakDetector.builder()
    .addPatternsFromFile(Path.of("extra-patterns.yml"), "extra_section")
    .build();

// Same API for ToxicityChecker:
ToxicityChecker checker = ToxicityChecker.builder()
    .addPatternsFromFile(Path.of("extra-toxicity.yml"), "extra_threats")
    .build();

// Replace / extend multilingual keywords:
checker = ToxicityChecker.builder()
    .keywordsFromFile(Path.of("my-keywords.yml"))       // replace
    .build();
checker = ToxicityChecker.builder()
    .addKeywordsFromFile(Path.of("extra-keywords.yml")) // extend
    .build();
```

### PatternLoader utilities

```java
// Load specs (id + category + type) from a YAML section:
List<PatternSpec> specs = PatternLoader.loadSpecs("my-resource.yml", "my_section");

// Build engines directly:
RegexPatternEngine    regexEngine    = PatternLoader.buildRegexEngine("my.yml", "sec1", "sec2");
KeywordAutomatonEngine kwEngine      = PatternLoader.buildKeywordEngine("my.yml", "sec1");
CompositePatternEngine composite     = PatternLoader.buildCompositeEngine("my.yml", "sec1", "sec2");

// From filesystem path:
RegexPatternEngine fromFile = PatternLoader.buildRegexEngineFromFile(path, "section");
```

---

## Multilingual Support

| Language | Code | Jailbreak | Toxicity | Engine |
|----------|------|-----------|----------|--------|
| English | EN | ✅ regex | ✅ regex + keywords | Regex + Aho-Corasick |
| Russian | RU | ✅ regex | ✅ regex | Regex |
| French | FR | ✅ regex | ✅ regex | Regex |
| German | DE | ✅ regex | ✅ regex | Regex |
| Spanish | ES | ✅ regex | ✅ regex | Regex |
| Polish | PL | ✅ regex | ✅ regex | Regex |
| Italian | IT | ✅ regex | ✅ regex | Regex |
| Japanese | JA | ✅ keywords | ✅ keywords | Aho-Corasick |
| Chinese | ZH | ✅ keywords | ✅ keywords | KeywordMatcher |
| Arabic | AR | ✅ keywords | ✅ keywords | KeywordMatcher |
| Hindi | HI | ✅ keywords | ✅ keywords | KeywordMatcher |
| Turkish | TR | ✅ keywords | ✅ keywords | KeywordMatcher |
| Korean | KO | ✅ keywords | ✅ keywords | KeywordMatcher |

**Detection layers:**
1. **Main engine (Aho-Corasick + Regex)** — `jailbreak-patterns.yml` / `toxicity-patterns.yml` — covers EN regex, 6 European languages regex, EN+JA keyword phrases.
2. **Multilingual keyword phase** — `multilingual-jailbreak-keywords.yml` / `multilingual-toxicity-keywords.yml` — covers ZH / JA / AR / HI / TR / KO via `KeywordMatcher` (simple substring scan). JA appears in both layers for deeper coverage.

---

## Spring AI Integration

### Dependency

```kotlin
implementation("com.github.Ratila1.JGuardrails:jguardrails-spring-ai:v1.0.0")
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
implementation("com.github.Ratila1.JGuardrails:jguardrails-langchain4j:v1.0.0")
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

## What's New in 1.0.0

### Aho-Corasick keyword engine (`KeywordAutomatonEngine`)

A new `KeywordAutomatonEngine` implements multi-keyword matching via the Aho-Corasick automaton. All registered keywords are scanned in a single O(n + m) pass over the input text, where n = text length and m = total keyword length. This replaces the previous per-pattern regex loop for literal phrases and is particularly efficient when many keywords need to be checked simultaneously.

### CompositePatternEngine — hybrid regex + Aho-Corasick

`CompositePatternEngine` routes each `PatternSpec` to the correct sub-engine at `findFirst()` time based on its declared type. REGEX specs go to `RegexPatternEngine`; KEYWORD specs go to `KeywordAutomatonEngine`. Both engines run in parallel during `findFirst()` and the result with the earlier position in text wins. `JailbreakDetector` and `ToxicityChecker` both use this composite engine by default.

### `PatternSpec.Type` — REGEX vs KEYWORD

`PatternSpec` now carries a `Type` field (`REGEX` or `KEYWORD`). YAML entries default to `REGEX`; entries with `type: KEYWORD` are compiled as Aho-Corasick keywords rather than regex patterns.

### `TextPatternEngine.findFirst()` — batch detection API

`TextPatternEngine` now declares a `findFirst(String text, List<PatternSpec> specs)` default method that iterates specs and returns the first match. `KeywordAutomatonEngine` overrides it with a true single-pass Aho-Corasick scan. `CompositePatternEngine` overrides it to partition by type, run both sub-engines, and return the earliest positional match.

### YAML-based pattern configuration with type support

The bundled YAML files (`jailbreak-patterns.yml`, `toxicity-patterns.yml`) now support `type: KEYWORD` entries. Literal jailbreak phrases (`"bypass safety filter"`, `"developer mode enabled"`, etc.) and toxicity phrases (`"kill yourself"`, `"i hate you"`, etc.) are now KEYWORD entries matched by Aho-Corasick.

### Japanese (JA) added to main pattern files

Japanese jailbreak and toxicity phrases are now defined directly in `jailbreak-patterns.yml` and `toxicity-patterns.yml` as `type: KEYWORD` entries and matched by the Aho-Corasick engine — the same engine used for all literal phrase matching. Japanese also remains in the multilingual keyword files for double coverage.

### `THIRD_PERSON_ABUSE` toxicity category

A new `ToxicityChecker.Category.THIRD_PERSON_ABUSE` detects derogatory content about absent third parties: insults (`"he is an idiot"`), dehumanising phrases (`"waste of space"`), and death wishes (`"she should die"`). Subjects are restricted to human-referencing pronouns and references to avoid false positives on abstract narrative text. Patterns use `UNICODE_CHARACTER_CLASS` for correct `\b` handling across all 7 supported languages.

### `PatternLoader` — new engine-building utilities

New public methods on `PatternLoader`:

| Method | Description |
|--------|-------------|
| `buildRegexEngine(resource, sections...)` | Build `RegexPatternEngine` from classpath YAML (skips KEYWORD entries) |
| `buildKeywordEngine(resource, sections...)` | Build `KeywordAutomatonEngine` from classpath YAML (skips REGEX entries) |
| `buildCompositeEngine(resource, sections...)` | Build combined `CompositePatternEngine` from classpath YAML |
| `buildRegexEngineFromFile(path, section)` | Same, from filesystem path |
| `buildKeywordEngineFromFile(path, section)` | Same, from filesystem path |
| `buildCompositeEngineFromFile(path, section)` | Same, from filesystem path |
| `loadSpecs(resource, section)` | Load `List<PatternSpec>` with type information |
| `loadAllKeywordsFromFile(path)` | Load all keyword strings from a YAML file |

### Extended Builder APIs

`JailbreakDetector.Builder` and `ToxicityChecker.Builder` now expose:

```java
// Plug in a fully custom engine:
.engine(myTextPatternEngine)

// Replace defaults with patterns from a YAML file (regex + keyword):
.patternsFromFile(path, sectionKey)

// Extend defaults with extra patterns from a YAML file:
.addPatternsFromFile(path, sectionKey)

// ToxicityChecker only — replace / extend multilingual keywords:
.keywordsFromFile(path)
.addKeywordsFromFile(path)

// ToxicityChecker only — toggle multilingual detection:
.multilingualEnabled(boolean)
```

### Multilingual coverage expanded

| Language | Before 1.0.0 | 1.0.0 |
|----------|-------------|-------|
| EN / RU / FR / DE / ES / PL / IT | regex | regex + Aho-Corasick (literal phrases) |
| JA | multilingual keyword phase only | main engine (Aho-Corasick) + multilingual keyword phase |
| ZH / AR / HI / TR / KO | multilingual keyword phase | multilingual keyword phase (unchanged) |

---

## License

[Apache License 2.0](LICENSE)
