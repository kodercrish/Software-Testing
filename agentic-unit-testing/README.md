# Agentic AI Unit-Testing Pipeline (CSE731 Mid-term)

**Team:** Krish Patel (IMT2023134), Yash Gupta (IMT2023125)

A pipeline of three agents, written in plain Java, that runs on the first 12 problems of **HumanEval-X (Java)**:

1. **Code Generator (LLM):** writes the method described in the problem.
2. **Test Case Generator (LLM):** writes JUnit 5 tests that must reach a user-specified coverage criterion (statement or branch).
3. **Test Case Executor:** runs the tests, measures coverage with JaCoCo, and gives a verdict.

If the goal is not reached, the executor's report (uncovered lines and branches, failing tests) goes back to the test generator, up to `maxTestIterations` rounds. The pipeline uses no agent framework, no RAG and no chain-of-thought prompting.

```
 problem spec ─► [1 Code Generator] ─► Solution.java ─► [2 Test Generator] ─► SolutionTest.java ─► [3 Test Executor] ─► verdict
                                                              ▲                                          │
                                                              └──── uncovered lines / failures ◄─────────┘
```

## Run

Requirements: JDK 21 and Maven.

```bash
export OPENROUTER_API_KEY="sk-or-v1-..."   # from https://openrouter.ai/keys
mvn package                                # builds target/pipeline.jar and downloads JUnit/JaCoCo into tools/
java -jar target/pipeline.jar
```

The model, temperatures, coverage criterion and target are set in `config.properties`.

## Files

| Path | Purpose |
|---|---|
| `config.properties` | Model, per-agent sampling settings, coverage criterion and target |
| `src/main/resources/prompts/` | System and user prompts of the agents |
| `st/pipeline/Main.java` | Entry point: loads settings, runs the first 12 tasks |
| `st/pipeline/Pipeline.java` | Connects the three agents and runs the feedback loop |
| `st/pipeline/agents/` | The three agents, plus `Prompts` (templates and code extraction) |
| `st/pipeline/llm/LlmClient.java` | HTTP client for the OpenRouter chat API |
| `st/pipeline/exec/` | Runs javac/java with a timeout; parses the JUnit and JaCoCo XML reports |
| `st/pipeline/report/RunLogger.java` | Writes the outputs below |

## Output: `runs/<timestamp>/`

```
summary.md                results table for all tasks
Java_<id>/
  llm-calls.jsonl         every LLM call: agent, model, temperature, top_p, max_tokens, seed, prompts, response
  Solution.java           generated code
  SolutionTest.java       generated tests (final round)
  verdict.json            verdict of the Test Executor
  iter<k>/                each round: sources, JUnit output, coverage.xml
```

Verdict values: `PASS` (all tests pass and coverage goal met), `COVERAGE_NOT_MET`, `TESTS_FAILED`, `COMPILE_ERROR`.
