package io.jguardrails.pipeline;

import io.jguardrails.audit.InMemoryAuditLogger;
import io.jguardrails.core.InputRail;
import io.jguardrails.core.OutputRail;
import io.jguardrails.core.RailAction;
import io.jguardrails.core.RailContext;
import io.jguardrails.core.RailResult;
import io.jguardrails.metrics.DefaultMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GuardrailPipeline")
class GuardrailPipelineTest {

    private RailContext context;

    @BeforeEach
    void setUp() {
        context = RailContext.builder().sessionId("test-session").build();
    }

    // -------------------------------------------------------------------------
    // Input processing
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("passes safe input through all rails")
    void passesInput_whenAllRailsPass() {
        GuardrailPipeline pipeline = GuardrailPipeline.builder()
                .addInputRail(passingInputRail("rail-a"))
                .addInputRail(passingInputRail("rail-b"))
                .build();

        PipelineExecutionResult result = pipeline.processInput("hello world", context);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getText()).isEqualTo("hello world");
        assertThat(result.getRailResults()).hasSize(2);
    }

    @Test
    @DisplayName("blocks input when first rail returns BLOCK")
    void blocksInput_whenFirstRailBlocks() {
        GuardrailPipeline pipeline = GuardrailPipeline.builder()
                .addInputRail(blockingInputRail("blocker"))
                .addInputRail(passingInputRail("never-reached"))
                .blockedResponse("Blocked!")
                .build();

        PipelineExecutionResult result = pipeline.processInput("bad input", context);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getText()).isEqualTo("Blocked!");
        assertThat(result.getBlockingResult()).isPresent();
        assertThat(result.getBlockingResult().get().railName()).isEqualTo("blocker");
    }

    @Test
    @DisplayName("modifies input when rail returns MODIFY and passes result to next rail")
    void modifiesInput_andPassesToNextRail() {
        GuardrailPipeline pipeline = GuardrailPipeline.builder()
                .addInputRail(modifyingInputRail("modifier", "MODIFIED"))
                .addInputRail(passingInputRail("checker"))
                .build();

        PipelineExecutionResult result = pipeline.processInput("original", context);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getText()).isEqualTo("MODIFIED");
        assertThat(result.getRailResults()).hasSize(2);
        assertThat(result.getRailResults().get(0).action()).isEqualTo(RailAction.MODIFY);
    }

    @Test
    @DisplayName("executes rails in priority order")
    void executesRails_inPriorityOrder() {
        StringBuilder order = new StringBuilder();

        InputRail rail10 = new InputRail() {
            public String name() { return "rail-10"; }
            public int priority() { return 10; }
            public RailResult process(String input, RailContext ctx) {
                order.append("10,");
                return RailResult.pass(input, name());
            }
        };
        InputRail rail5 = new InputRail() {
            public String name() { return "rail-5"; }
            public int priority() { return 5; }
            public RailResult process(String input, RailContext ctx) {
                order.append("5,");
                return RailResult.pass(input, name());
            }
        };

        GuardrailPipeline pipeline = GuardrailPipeline.builder()
                .addInputRail(rail10)
                .addInputRail(rail5)
                .build();

        pipeline.processInput("test", context);
        assertThat(order.toString()).isEqualTo("5,10,");
    }

    @Test
    @DisplayName("fail-open skips failing rail and continues processing")
    void failOpen_skipsFailingRailAndContinues() {
        InputRail crashingRail = new InputRail() {
            public String name() { return "crasher"; }
            public RailResult process(String input, RailContext ctx) {
                throw new RuntimeException("Simulated failure");
            }
        };

        GuardrailPipeline pipeline = GuardrailPipeline.builder()
                .addInputRail(crashingRail)
                .addInputRail(passingInputRail("next"))
                .failOpen(true)
                .build();

        PipelineExecutionResult result = pipeline.processInput("test", context);
        assertThat(result.isBlocked()).isFalse();
    }

    @Test
    @DisplayName("fail-closed blocks on rail exception")
    void failClosed_blocksOnRailException() {
        InputRail crashingRail = new InputRail() {
            public String name() { return "crasher"; }
            public RailResult process(String input, RailContext ctx) {
                throw new RuntimeException("Simulated failure");
            }
        };

        GuardrailPipeline pipeline = GuardrailPipeline.builder()
                .addInputRail(crashingRail)
                .failOpen(false)
                .blockedResponse("Error")
                .build();

        PipelineExecutionResult result = pipeline.processInput("test", context);
        assertThat(result.isBlocked()).isTrue();
    }

    // -------------------------------------------------------------------------
    // Output processing
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("passes safe output through all output rails")
    void passesOutput_whenAllOutputRailsPass() {
        GuardrailPipeline pipeline = GuardrailPipeline.builder()
                .addOutputRail(passingOutputRail("out-a"))
                .addOutputRail(passingOutputRail("out-b"))
                .build();

        PipelineExecutionResult result = pipeline.processOutput("LLM response", "user question", context);

        assertThat(result.isBlocked()).isFalse();
        assertThat(result.getText()).isEqualTo("LLM response");
    }

    @Test
    @DisplayName("blocks output when output rail returns BLOCK")
    void blocksOutput_whenOutputRailBlocks() {
        GuardrailPipeline pipeline = GuardrailPipeline.builder()
                .addOutputRail(blockingOutputRail("output-blocker"))
                .blockedResponse("Output blocked")
                .build();

        PipelineExecutionResult result = pipeline.processOutput("toxic response", "question", context);

        assertThat(result.isBlocked()).isTrue();
        assertThat(result.getText()).isEqualTo("Output blocked");
    }

    // -------------------------------------------------------------------------
    // execute() convenience method
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("execute() returns LLM response when all rails pass")
    void execute_returnsLlmResponse_whenAllPass() {
        GuardrailPipeline pipeline = GuardrailPipeline.builder().build();

        String response = pipeline.execute("What is 2+2?", context, input -> "The answer is 4.");

        assertThat(response).isEqualTo("The answer is 4.");
    }

    @Test
    @DisplayName("execute() short-circuits to blocked response before calling LLM")
    void execute_shortCircuits_whenInputBlocked() {
        boolean[] llmCalled = {false};

        GuardrailPipeline pipeline = GuardrailPipeline.builder()
                .addInputRail(blockingInputRail("blocker"))
                .blockedResponse("Input blocked")
                .build();

        String response = pipeline.execute("bad input", context, input -> {
            llmCalled[0] = true;
            return "LLM response";
        });

        assertThat(response).isEqualTo("Input blocked");
        assertThat(llmCalled[0]).isFalse();
    }

    // -------------------------------------------------------------------------
    // Audit and metrics
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("audit logger records block events")
    void auditLogger_recordsBlockEvents() {
        InMemoryAuditLogger audit = new InMemoryAuditLogger();
        GuardrailPipeline pipeline = GuardrailPipeline.builder()
                .addInputRail(blockingInputRail("b1"))
                .auditLogger(audit)
                .build();

        pipeline.processInput("bad", context);

        assertThat(audit.getEntries()).hasSize(1);
        assertThat(audit.getEntries().get(0).getRailName()).isEqualTo("b1");
    }

    @Test
    @DisplayName("metrics record block count")
    void metrics_recordBlockCount() {
        DefaultMetrics metrics = new DefaultMetrics();
        GuardrailPipeline pipeline = GuardrailPipeline.builder()
                .addInputRail(blockingInputRail("b1"))
                .metrics(metrics)
                .build();

        pipeline.processInput("bad", context);

        assertThat(metrics.getSnapshot().totalBlocked()).isEqualTo(1);
        assertThat(metrics.getSnapshot().blockedByRail()).containsKey("b1");
    }

    // -------------------------------------------------------------------------
    // Rail factories
    // -------------------------------------------------------------------------

    private InputRail passingInputRail(String name) {
        return new InputRail() {
            public String name() { return name; }
            public RailResult process(String input, RailContext ctx) {
                return RailResult.pass(input, name());
            }
        };
    }

    private InputRail blockingInputRail(String name) {
        return new InputRail() {
            public String name() { return name; }
            public RailResult process(String input, RailContext ctx) {
                return RailResult.block(name(), "Test block");
            }
        };
    }

    private InputRail modifyingInputRail(String name, String replacement) {
        return new InputRail() {
            public String name() { return name; }
            public RailResult process(String input, RailContext ctx) {
                return RailResult.modify(replacement, name(), "Test modify");
            }
        };
    }

    private OutputRail passingOutputRail(String name) {
        return new OutputRail() {
            public String name() { return name; }
            public RailResult process(String output, String originalInput, RailContext ctx) {
                return RailResult.pass(output, name());
            }
        };
    }

    private OutputRail blockingOutputRail(String name) {
        return new OutputRail() {
            public String name() { return name; }
            public RailResult process(String output, String originalInput, RailContext ctx) {
                return RailResult.block(name(), "Output blocked");
            }
        };
    }
}
