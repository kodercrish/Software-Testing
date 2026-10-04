# CSE731 Mid-term Project Report: Agentic AI Pipeline for Coverage-Directed Unit Testing

**Team:** Krish Patel (IMT2023134), Yash Gupta (IMT2023125)

**Code repository:** https://github.com/kodercrish/Software-Testing

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
| Model | `nvidia/nemotron-3-super-120b-a12b:free` (OpenRouter) | same |
| reasoning | disabled | disabled |
| temperature | 0.2 | 0.4 |
| top_p | 0.95 | 0.95 |
| max_tokens | 2048 | 4096 |
| seed | 42 | 42 |
| Other | – | criterion = BRANCH, target = 100%, maxTestIterations = 3 |

The model's built-in reasoning ("thinking") mode is switched off in every request (`"reasoning": {"enabled": false}`), so it answers directly without a hidden chain of thought, as the project rules require. The code generator uses a low temperature to get the most likely correct solution. The test generator uses a slightly higher one so that it tries more varied inputs when it must reach uncovered branches.

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
{
  "taskId": "Java/11",
  "status": "PASS",
  "testsTotal": 9,
  "testsPassed": 9,
  "criterion": "BRANCH",
  "target": 100.0,
  "statementPct": 100.0,
  "branchPct": 100.0,
  "iterations": 2
}
```

**Sample generated code (Java/11, `stringXor`):**

```java
import java.util.*;
import java.lang.*;

class Solution {
    /**
    Input are two strings a and b consisting only of 1s and 0s.
    Perform binary XOR on these inputs and return result also as a string.
    >>> stringXor("010", "110")
    "100"
     */
    public String stringXor(String a, String b) {
        StringBuilder result = new StringBuilder();
        int length = Math.max(a.length(), b.length());
        for (int i = 0; i < length; i++) {
            char bitA = i < a.length() ? a.charAt(a.length() - 1 - i) : '0';
            char bitB = i < b.length() ? b.charAt(b.length() - 1 - i) : '0';
            char xorBit = (bitA == bitB) ? '0' : '1';
            result.insert(0, xorBit);
        }
        return result.toString();
    }
}
```

**Sample generated tests (Java/11, final round):**

```java
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SolutionTest {
    @Test
    void testEqualLengthBothZeros() {
        Solution sol = new Solution();
        assertEquals("000", sol.stringXor("000", "000"));
    }

    @Test
    void testEqualLengthBothOnes() {
        Solution sol = new Solution();
        assertEquals("000", sol.stringXor("111", "111"));
    }

    @Test
    void testEqualLengthMixed() {
        Solution sol = new Solution();
        assertEquals("110", sol.stringXor("101", "011"));
    }

    @Test
    void testFirstLonger() {
        Solution sol = new Solution();
        assertEquals("1001", sol.stringXor("1100", "101"));
    }

    @Test
    void testSecondLonger() {
        Solution sol = new Solution();
        assertEquals("1001", sol.stringXor("101", "1100"));
    }

    @Test
    void testOneEmpty() {
        Solution sol = new Solution();
        assertEquals("101", sol.stringXor("", "101"));
    }

    @Test
    void testBothEmpty() {
        Solution sol = new Solution();
        assertEquals("", sol.stringXor("", ""));
    }

    @Test
    void testSingleCharDifferent() {
        Solution sol = new Solution();
        assertEquals("1", sol.stringXor("0", "1"));
    }

    @Test
    void testSingleCharSame() {
        Solution sol = new Solution();
        assertEquals("0", sol.stringXor("1", "1"));
    }
}
```

## 4. Execution results (test case generator and test case executor)

Run of 4 October 2026 on tasks Java/0 – Java/11 (criterion BRANCH, target 100%, at most 3 rounds). All files are in `runs/20261004-182338/`.

| Task | Verdict | Tests passed | Statement % | Branch % | Iterations |
|---|---|---|---|---|---|
| Java/0 | COMPILE_ERROR | 0/0 | 0.0 | 0.0 | 3 |
| Java/1 | TESTS_FAILED | 13/16 | 100.0 | 83.3 | 3 |
| Java/2 | PASS | 4/4 | 100.0 | 100.0 | 1 |
| Java/3 | PASS | 6/6 | 100.0 | 100.0 | 1 |
| Java/4 | PASS | 7/7 | 100.0 | 100.0 | 1 |
| Java/5 | PASS | 5/5 | 100.0 | 100.0 | 1 |
| Java/6 | COVERAGE_NOT_MET | 9/9 | 100.0 | 93.8 | 3 |
| Java/7 | PASS | 5/5 | 100.0 | 100.0 | 1 |
| Java/8 | PASS | 5/5 | 100.0 | 100.0 | 1 |
| Java/9 | PASS | 8/8 | 100.0 | 100.0 | 1 |
| Java/10 | TESTS_FAILED | 10/11 | 100.0 | 91.7 | 3 |
| Java/11 | PASS | 9/9 | 100.0 | 100.0 | 2 |

Coverage goal reached with all tests passing: 8/12 tasks

Console output of the test case executor per round:

```
Java/0   iter 1: 9/10 pass, 100.0%   iter 2: 9/13 pass, 100.0%   iter 3: does not compile
Java/1   iter 1: 8/8 pass, 83.3%     iter 2: 10/12 pass, 83.3%   iter 3: 13/16 pass, 83.3%
Java/6   iter 1: 7/7 pass, 93.8%     iter 2: 8/8 pass, 93.8%     iter 3: 9/9 pass, 93.8%
Java/10  iter 1: 7/7 pass, 91.7%     iter 2: 9/9 pass, 91.7%     iter 3: 10/11 pass, 91.7%
Java/11  iter 1: 7/9 pass, 100.0%    iter 2: 9/9 pass, 100.0%
(all other tasks: PASS in iteration 1)
```

Feedback sent to the test case generator after round 1 of Java/11:

```text
Coverage: statements 9/9 (100.0%), branches 8/8 (100.0%)
Tests: 9 run, 7 passed, 2 failed
  FAILED testFirstLonger(): expected: <1010> but was: <1001>
  FAILED testSecondLonger(): expected: <1010> but was: <1001>
```

**Discussion.**

- **8/12 tasks reached 100% branch coverage with all tests passing.** 7 of them did so in the first round, and every task reached 100% statement coverage, except Java/0, whose final round did not compile.
- **The feedback loop repaired wrong tests.** In Java/11 the first suite had full coverage, but 2 tests expected wrong XOR results for inputs of different lengths. After the executor reported the failures, the generator corrected them and the task passed in round 2.
- **Infeasible branches (Java/1, Java/6, Java/10).** The remaining uncovered branch outcomes cannot be reached by any input that satisfies the specification:
  - in Java/1 and Java/6, the false outcome of `else if (c == ')')` needs a character that is neither `(` nor `)`, but the input contains only parentheses and spaces;
  - in Java/10, the exit condition of the `for` loop is never reached, because the last character on its own is always a palindromic suffix, so the loop always returns early.

  Trying to cover these branches, the generator invented invalid inputs (e.g. unbalanced parentheses) with guessed expected values, which then failed. This shows a limit of structural coverage criteria: 100% branch coverage is not always achievable, and an LLM cannot tell an infeasible branch from a hard one.
- **Wrong oracles.** Some failing tests had expected values that contradict the specification. In Java/0 the test `testThresholdZeroWithClose` expects `true` for threshold 0, although no difference can be smaller than 0. In Java/10, `makePalindrome("ab")` is expected to be `"ababa"` instead of `"aba"`. Execution caught these, not the LLM.
- **Regression across rounds (Java/0).** Round 1 already had 100% coverage with one wrong test. In round 3 the generator produced duplicate test-method names, so the suite no longer compiled. Because the pipeline reports the last round, the final verdict is `COMPILE_ERROR`.

## 5. Contributions

The work was split into two parts: the LLM side of the pipeline (agents 1 and 2) and the execution side (agent 3, results and report).

| Member | Part | Contribution |
|---|---|---|
| Yash Gupta (IMT2023125) | Part 1: LLM agents and orchestration | OpenRouter client (`LlmClient`), Code Generator agent, Test Case Generator agent including the feedback message, prompt templates and sampling settings (`Prompts`, `config.properties`), dataset loader (`HumanEvalX`), and the pipeline loop (`Pipeline`, `Main`) |
| Krish Patel (IMT2023134) | Part 2: Test execution, results and report | Test Case Executor agent (compiling and running tests with JUnit and JaCoCo, verdict), process runner (`Proc`), JUnit/JaCoCo report parsing (`Reports`), run logging and results summary (`RunLogger`), running the experiments on the 12 tasks, and writing this report |
