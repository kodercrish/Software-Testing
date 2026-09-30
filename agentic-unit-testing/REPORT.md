# CSE731 Mid-term Project Report: Agentic AI Pipeline for Coverage-Directed Unit Testing

**Team:** <Member 1 (Roll no.)>, <Member 2 (Roll no.)>

## 1. Pipeline, dataset and test-generator functionality

**Dataset.** HumanEval-X, Java split (164 problems). Each problem provides:
- a `prompt`: imports, `class Solution`, a Javadoc specification with examples, and the method signature;
- a `canonical_solution`;
- official hidden tests (`class Main`).

We used tasks `<0–N>`.

**Testing requirement: option (1), coverage criterion.** The user picks the criterion (`STATEMENT` or `BRANCH`) and a target percentage in `config.properties`. The test generator must produce JUnit 5 tests that reach this coverage of the generated `Solution` class, as measured by JaCoCo.
- STATEMENT is JaCoCo line coverage.
- BRANCH is JaCoCo branch coverage, i.e. decision coverage: both outcomes of every `if`, loop condition, `&&`, `||`, `?:` and `switch`.

**Agents.** The pipeline is plain Java code: HTTP calls to an OpenAI-compatible endpoint and a loop. It uses no framework, no RAG and no chain-of-thought.

1. **Code Generator (LLM).**
   - Input: the task `prompt`.
   - Output: a complete `Solution.java`.
   - If the code does not compile, the compiler errors are sent back, up to `maxCodeAttempts` times.
2. **Test Case Generator (LLM).**
   - Input: the specification and the generated code with line numbers.
   - Output: `SolutionTest.java` (JUnit 5).
   - Expected values must come from the *specification*, not from the code, so the tests can reveal bugs.
   - In feedback rounds it receives the executor's report: lines never executed, decisions with untaken outcomes, failing tests with their messages, and compile errors. It then returns an improved suite.
3. **Test Case Executor.**
   - Compiles the code and tests, then runs them in a separate JVM using the JUnit Platform console launcher with the JaCoCo agent attached.
   - Parses the JUnit and JaCoCo XML reports and produces the verdict.
   - The loop stops when every test passes and the coverage target is met, or after `maxTestIterations` rounds. The best suite from all rounds is kept.
   - It also runs (a) the official HumanEval-X tests on the generated code, which is the ground truth for code correctness, and (b) the generated tests on the canonical solution, which checks the test oracles. Neither result is shown to the LLM agents.

<Insert the pipeline diagram from README.md>

## 2. Prompts and settings

| Setting | Code Generator | Test Generator |
|---|---|---|
| Model | `<model>` via OpenRouter | same |
| temperature | 0.2 | 0.4 |
| top_p | 0.95 | 0.95 |
| max_tokens | 2048 | 4096 |
| seed | 42 | 42 |
| Other | maxCodeAttempts = 2 | criterion = BRANCH, target = 100 %, maxTestIterations = 3 |

The code generator uses a low temperature to get the most likely correct solution. The test generator uses a slightly higher temperature so that, in feedback rounds, it produces more varied inputs that can reach uncovered branches.

Placeholders `{{...}}` are filled in at run time. The exact prompts sent for every call are logged in `runs/<ts>/Java_<id>/llm-calls.jsonl`.

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

**codegen_fix.txt**

```text
Your previous code did not compile. Compiler output:

```
{{errors}}
```

Previous code:

```java
{{code}}
```

Return the corrected complete compilation unit in one Java code block.
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

Example of the feedback produced by the Test Executor (task Java/0, after a weak first suite):

```text
Coverage: statements 1/6 (16.7%), branches 0/6 (0.0%)
Lines never executed:
  line 13: for (int i = 0; i < numbers.size(); i++) {
  line 16: if (distance < threshold) return true;
  line 19: return false;
Tests: 1 run, 1 passed, 0 failed
```

## 3. Formats of code, tests and verdict

- **Code:** one Java compilation unit, `Solution.java`, containing `class Solution` with the required method. It has no package and no `main`.
- **Tests:** one JUnit 5 compilation unit, `SolutionTest.java`, with one scenario per `@Test` method, using `assertEquals`, `assertTrue` and `assertFalse`.
- **Verdict:** JSON (`verdict.json`), for example:

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

## 4. Execution results (test generator + test executor)

<paste runs/<ts>/summary.md: per-task table and aggregate metrics>

Metrics reported:
- coverage goal met (%);
- mean statement and branch coverage;
- number of tasks with verdict PASS;
- code correctness (pass@1 against the official tests);
- buggy solutions caught by the generated tests;
- test-oracle validity (generated tests passing on the canonical solution);
- mean number of feedback iterations.

<Add 3–5 lines of discussion: e.g. how coverage improved between iteration 1 and the final iteration, and which tasks failed and why.>

## 5. Contributions

| Member | Contribution |
|---|---|
| <Member 1> | <e.g. LLM client, code and test generator agents, prompt design> |
| <Member 2> | <e.g. test executor (JUnit/JaCoCo), feedback loop, experiments, report> |
