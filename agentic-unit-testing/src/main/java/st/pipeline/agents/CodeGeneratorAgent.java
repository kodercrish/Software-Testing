package st.pipeline.agents;

import st.pipeline.dataset.HumanEvalX.Task;
import st.pipeline.llm.LlmClient;
import st.pipeline.llm.LlmClient.Call;
import st.pipeline.llm.LlmClient.Params;

import java.util.Map;

/** Agent 1: generates the unit under test (class Solution) from the HumanEval-X specification. */
public class CodeGeneratorAgent {
    private final LlmClient llm;
    private final Params params;

    public CodeGeneratorAgent(LlmClient llm, Params params) {
        this.llm = llm;
        this.params = params;
    }

    public record Output(String code, Call call) {}

    public Output generate(Task task) throws Exception {
        String system = Prompts.render("codegen_system", Map.of());
        String user = Prompts.render("codegen_user", Map.of("prompt", task.prompt()));
        Call call = llm.chat("code-generator", system, user, params);
        return new Output(Prompts.extractCode(call.response(), "Solution"), call);
    }
}
