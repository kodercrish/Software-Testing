package st.pipeline.agents;

import st.pipeline.dataset.HumanEvalX.Task;
import st.pipeline.llm.LlmClient;
import st.pipeline.llm.LlmClient.Call;
import st.pipeline.llm.LlmClient.Params;

import java.util.Map;

/** Agent 1: generates the unit under test (class Solution) from the HumanEval-X specification. */
public class CodeGeneratorAgent {
    public static final String NAME = "code-generator";

    private final LlmClient llm;
    private final Params params;

    public CodeGeneratorAgent(LlmClient llm, Params params) {
        this.llm = llm;
        this.params = params;
    }

    public record Output(String code, Call call) {}

    public Output generate(Task task) throws Exception {
        return ask(Prompts.render("codegen_user", Map.of("prompt", task.prompt())));
    }

    /** Asks for a corrected version after a compilation failure. */
    public Output fix(Task task, String code, String compilerErrors) throws Exception {
        String user = Prompts.render("codegen_user", Map.of("prompt", task.prompt())) + "\n\n"
                + Prompts.render("codegen_fix", Map.of("code", code, "errors", compilerErrors.strip()));
        return ask(user);
    }

    private Output ask(String user) throws Exception {
        Call call = llm.chat(NAME, Prompts.render("codegen_system", Map.of()), user, params);
        return new Output(Prompts.extractCode(call.response(), "Solution"), call);
    }
}
