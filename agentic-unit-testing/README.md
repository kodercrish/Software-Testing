# Agentic AI Unit-Testing Pipeline (CSE731 Mid-term)

A three-agent pipeline, written in plain Java 21, that works on **HumanEval-X (Java)** problems. It:
1. generates the unit under test,
2. generates JUnit 5 tests that must reach a **user-specified coverage criterion** (statement or branch), and
3. executes those tests and gives a verdict.

This is **option (1): coverage-directed test generation**. The pipeline uses no agent framework, no RAG and no chain-of-thought prompting. It is plain HTTP calls plus a Java loop.

```
 HumanEval-X task (Javadoc spec + signature)
        │
        ▼
 ┌─────────────────────┐  compile errors   ┌─────────────────────────────┐
 │ 1 Code Generator    │◄──────────────────│ javac (via Test Executor)   │
 │   (LLM, T=0.2)      │──── Solution ────►│                             │
 └─────────────────────┘                   └─────────────────────────────┘
        │ Solution.java (+ line numbers)
        ▼
 ┌─────────────────────┐  uncovered lines, partially covered decisions,
 │ 2 Test Generator    │◄─ failing tests, compile errors ──────────────┐
 │   (LLM, T=0.4)      │                                               │
 └─────────────────────┘                                               │
        │ SolutionTest.java (JUnit 5)                                  │
        ▼                                                              │
 ┌──────────────────────────────────────────────────────────────┐     │
 │ 3 Test Executor  (separate JVM: javac → JUnit console         │─────┘
 │   launcher + JaCoCo agent → JaCoCo XML report)                │  goal not met
 │   → verdict.json                                             │  (≤ maxTestIterations)
 └──────────────────────────────────────────────────────────────┘
```

The Test Executor also runs two extra checks. Neither result is shown to any agent; both appear only in the verdict and summary:
- **Official HumanEval-X tests on the generated code.** This is the ground truth: is the generated code actually correct?
- **Generated tests on the canonical (reference) solution.** This checks the tests' oracles: are the expected values right?

## Setup

Requirements: JDK 21 and Maven. The first build downloads JUnit, JaCoCo and Gson.

```bash
export OPENROUTER_API_KEY="sk-or-v1-..."     # https://openrouter.ai/keys
mvn package                                  # builds target/pipeline.jar, copies tools/, runs self-tests
java -jar target/pipeline.jar                # runs the tasks listed in config.properties
```

You can override any key in `config.properties` on the command line:

```bash
java -jar target/pipeline.jar tasks=0-4 criterion=STATEMENT
java -jar target/pipeline.jar tasks=0,12,35 model=qwen/qwen3.8-27b:free maxTestIterations=4
```

To use another OpenAI-compatible provider, e.g. Groq:

```bash
export GROQ_API_KEY=...
java -jar target/pipeline.jar api.baseUrl=https://api.groq.com/openai/v1 api.keyEnv=GROQ_API_KEY model=<model-id>
```

## Project layout

| Path | Purpose |
|---|---|
| `config.properties` | Model, temperatures and other sampling settings, criterion, target, tasks |
| `src/main/resources/prompts/*.txt` | System and user prompt templates for each agent |
| `src/main/java/st/pipeline/agents/CodeGeneratorAgent.java` | Agent 1 |
| `src/main/java/st/pipeline/agents/TestGeneratorAgent.java` | Agent 2, including the feedback-message builder |
| `src/main/java/st/pipeline/agents/TestExecutorAgent.java` | Agent 3: compile, run, measure coverage, give verdict |
| `src/main/java/st/pipeline/Pipeline.java` | Orchestration loop |
| `src/main/java/st/pipeline/llm/LlmClient.java` | OpenAI-compatible HTTP client (retries on 429 errors) |
| `src/main/java/st/pipeline/exec/` | Process runner and JUnit/JaCoCo XML parsing |
| `src/main/java/st/pipeline/report/RunLogger.java` | Run logs and summary |
| `data/humaneval_java.jsonl` | HumanEval-X Java split (164 tasks) |
| `src/test/java/.../ExecutorTest.java` | Self-tests for the executor (no LLM needed) |

## Output: `runs/<timestamp>/`

```
settings.json                 all settings used for the run
summary.md / summary.csv      per-task table + aggregate metrics
Java_<id>/
  llm-calls.jsonl             every LLM call: agent, model, temperature, top_p, max_tokens, seed,
                              system prompt, user prompt, raw response, latency
  Solution.java               final generated code
  SolutionTest.java           final (best) generated test suite
  verdict.json                verdict of the Test Executor
  iter<k>/                    each test iteration: src/, junit-output.log, coverage.xml, coverage-html/
  official-tests/             HumanEval-X ground-truth run
  reference/                  generated tests run against the canonical solution
```

### Verdict status values

| Status | Meaning |
|---|---|
| `PASS` | Tests compile, all pass, and the coverage goal is met |
| `COVERAGE_GOAL_NOT_MET` | All tests pass, but coverage is below the target after the last iteration |
| `TESTS_FAILED` | Some generated test fails although the code is correct, so the test's expected value is wrong |
| `TESTS_FAILED_BUG_DETECTED` | Generated tests fail and the code really is wrong: the tests found a bug |
| `TESTS_DO_NOT_COMPILE` / `CODE_DOES_NOT_COMPILE` | Compilation failed after all retries |

## Free-tier notes

- OpenRouter's `:free` models allow about 20 requests/min and **50 requests/day**. Buying $10 of credits raises this to 1000/day.
- Each task uses 2–5 LLM calls, so about 10–15 tasks fit in the daily free quota.
- `requestDelayMs` spaces out calls, and 429 errors are retried automatically.
- If a free model is removed, pick another from https://openrouter.ai/models?q=free and set `model=...`.
