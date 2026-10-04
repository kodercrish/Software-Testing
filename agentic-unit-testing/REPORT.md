# CSE731 Mid-term Project Report: Agentic AI Pipeline for Coverage-Directed Unit Testing

**Team:** Krish Patel (IMT2023134), Yash Gupta (IMT2023125)

## 1. Pipeline, dataset and test-generator functionality

**Dataset.** HumanEval-X, Java split. We use the first 12 problems (Java/0 – Java/11). Each problem's `prompt` contains the imports, `class Solution`, a Javadoc specification with examples, and the method signature.

**Testing requirement: option (1), coverage criterion.** The user sets the criterion (`STATEMENT` or `BRANCH`) and a target percentage in `config.properties`. The test case generator must produce JUnit 5 tests that reach this coverage of the generated code, as measured by JaCoCo. We used BRANCH coverage with a 100% target: every decision (`if`, loop condition, `&&`, `||`, `?:`) must evaluate both to true and to false.

**Agents.** The pipeline is plain Java: HTTP calls to OpenRouter and a loop. It uses no framework, no RAG and no chain-of-thought.

1. **Code Generator (LLM).** Input: the problem `prompt`. Output: `Solution.java`.
2. **Test Case Generator (LLM).** Input: the specification and the generated code with line numbers. Output: `SolutionTest.java`. Expected values must come from the specification, not from the code. In later rounds it also receives the executor's feedback (uncovered lines, decisions with untaken outcomes, failing tests, compile errors) and returns an improved test class.
3. **Test Case Executor.** Compiles code and tests, runs the tests with the JUnit console launcher in a separate JVM with the JaCoCo agent attached, reads the JUnit and JaCoCo XML reports, and gives the verdict. The loop stops when all tests pass and the coverage target is met, when the generated code itself does not compile (better tests cannot fix that), or after `maxTestIterations` rounds.

```
 problem spec ─► [1 Code Generator] ─► Solution.java ─► [2 Test Generator] ─► SolutionTest.java ─► [3 Test Executor] ─► verdict
                                                              ▲                                          │
                                                              └──── uncovered lines / failures ◄─────────┘
```

## 2. Prompts and settings

| Setting | Code Generator | Test Case Generator |
|---|---|---|
| Model | `qwen/qwen3.8-27b:free` (OpenRouter) | same |
| temperature | 0.2 | 0.4 |
| top_p | 0.95 | 0.95 |
| max_tokens | 2048 | 4096 |
| seed | 42 | 42 |
| Other | – | criterion = BRANCH, target = 100%, maxTestIterations = 3 |

The code generator uses a low temperature to get the most likely correct solution. The test generator uses a slightly higher one so that it tries more varied inputs when it must reach uncovered branches.

Placeholders `{{...}}` are filled in at run time. The exact prompts of every call are logged in `runs/<ts>/Java_<id>/llm-calls.jsonl`.

**codegen_system.txt**

```text
You are a code generator agent in an automated unit-testing pipeline.
You write correct, compilable Java 21 code that implements a specification.

Rules:
- Output exactly one Java code block (```java ... ```) and nothing else.
- The code block must be a complete compilation unit: the given imports and the class `Solution`
  containing the requested method with the exact given signature. Helper methods are allowed inside `Solution`.
- Do not add a package declaration, a main method, or any other top-level class.
- Use only the Java standard library.
```

**codegen_user.txt**

```text
Implement the method described by the Javadoc below. Keep the class name, method name, parameter types and return type exactly as given.

```java
{{prompt}}
```
```

**testgen_system.txt**

```text
You are a test case generator agent in an automated unit-testing pipeline.
You write JUnit 5 unit tests for a single Java method.

Testing goal: {{criterion}}. Target: {{target}}% as measured by JaCoCo.

Rules:
- Output exactly one Java code block (```java ... ```) and nothing else.
- The code block is a complete compilation unit containing the class `SolutionTest` (no package declaration).
- Import org.junit.jupiter.api.Test and static org.junit.jupiter.api.Assertions.*, plus any java.util classes you use.
- Create the object under test with `new Solution()`.
- Choose inputs so that the testing goal is reached: every statement / every decision outcome of the
  given implementation must be exercised by at least one test.
- Derive every expected value from the SPECIFICATION (the Javadoc and its examples), not from the implementation,
  so that a test fails if the implementation is wrong.
- One scenario per @Test method, with a descriptive method name. Use assertEquals / assertTrue / assertFalse;
  for doubles use assertEquals(expected, actual, 1e-6).
- Do not use mocking libraries, parameterized tests, reflection, randomness, or file / network I/O.
```

**testgen_user.txt**

```text
Specification of the method under test:

```java
{{prompt}}
```

Implementation under test (Solution.java, with line numbers):

```
{{numberedCode}}
```

Write the JUnit 5 class `SolutionTest`.
```

**testgen_feedback.txt**

```text
The Test Executor ran your tests. Results:

{{feedback}}

Current SolutionTest.java:

```java
{{tests}}
```

Return the complete improved `SolutionTest` class in one Java code block.
- Add tests that exercise the uncovered lines / branch outcomes listed above.
- If a failing test's expected value contradicts the specification, fix that test. If it agrees with the
  specification, keep it unchanged: it may have found a bug in the implementation.
- Keep all existing passing tests.
```

## 3. Formats of code, tests and verdict

- **Code:** one Java file, `Solution.java`, containing `class Solution` with the required method.
- **Tests:** one JUnit 5 file, `SolutionTest.java`, with one scenario per `@Test` method.
- **Verdict:** `verdict.json`, for example:

```json
<paste runs/<ts>/Java_<id>/verdict.json>
```

**Sample generated code (Java/<id>):**

```java
<paste Solution.java>
```

**Sample generated tests (Java/<id>):**

```java
<paste SolutionTest.java>
```

## 4. Execution results (test case generator and test case executor)

<paste runs/<ts>/summary.md>

<Add a few lines of discussion: e.g. which tasks needed feedback rounds, and why tasks did not reach the goal.>

## 5. Contributions

The work was split into two parts: the LLM side of the pipeline (agents 1 and 2) and the execution side (agent 3, results and report).

| Member | Part | Contribution |
|---|---|---|
| Yash Gupta (IMT2023125) | Part 1: LLM agents and orchestration | OpenRouter client (`LlmClient`), Code Generator agent, Test Case Generator agent including the feedback message, prompt templates and sampling settings (`Prompts`, `config.properties`), dataset loader (`HumanEvalX`), and the pipeline loop (`Pipeline`, `Main`) |
| Krish Patel (IMT2023134) | Part 2: Test execution, results and report | Test Case Executor agent (compiling and running tests with JUnit and JaCoCo, verdict), process runner (`Proc`), JUnit/JaCoCo report parsing (`Reports`), run logging and results summary (`RunLogger`), running the experiments on the 12 tasks, and writing this report |
