# Mid-term Project: AI-Assisted Unit Testing Pipeline (CSE731)

> **Update:** The pipeline was implemented in **Java 21 on HumanEval-X (Java)**, with JUnit 5 and JaCoCo instead of Python, pytest and coverage.py. Code lives in `agentic-unit-testing/`: see `agentic-unit-testing/README.md` for how to run it and `agentic-unit-testing/REPORT.md` for the report draft.

**Goal:** Build a simple agentic AI pipeline (from scratch, no frameworks) that generates code, generates unit tests aimed at a specific testing goal, executes them, and gives a verdict.

**Timeline:** 29 Sep – 1 Oct 2026 (report on LMS, one per team) · Demos: 1, 6, 8 Oct (all members present)

---

## Constraints (must follow)

| Rule | Why |
|---|---|
| No RAG | Focus is on generation + testing, not retrieval |
| No chain-of-thought frameworks / LangGraph / LangChain / CrewAI | Learn to wire a pipeline from first principles |
| Minimum 3 agents | Separation of concerns: generate code → generate tests → execute |
| Team of 2 | Report must state each member's contribution |

---

## What to do, how, and why

### 1. Pick a dataset
- **What:** Use MBPP (Most Basic Python Problems) or HumanEval.
- **How:** Load via HuggingFace `datasets` (`mbpp` / `openai_humaneval`) or download the JSON. Pick a small subset (e.g. 10–20 problems).
- **Why:** Gives standard, small, well-defined function-level problems — perfect "units" for unit testing.

### 2. Get LLM access
- **What:** A free LLM API.
- **How:** OpenRouter free models (or KiloAI). Use the OpenAI-compatible API with plain `requests`/`openai` client. Store the key in an env variable.
- **Why:** Agents are just LLM calls with different roles; you need an API to call.

### 3. Choose ONE testing goal for the test generator
| Option | Test generator must... | Measured by |
|---|---|---|
| **(1) Coverage** *(recommended)* | Produce tests achieving a user-specified criterion (statement / branch / loop coverage) | `coverage.py` (`--branch`) |
| (2) Property-based | Produce tests checking stated properties (e.g. element found / not found / empty list) | Pass/fail per property |
| (3) Statistical | Generate multiple suites; estimate e.g. P(at least one suite correct) over *n* samples | pass@k-style metrics |

- **Why:** The project is about tests that serve a *specific* testing goal, not random tests. Option 1 maps directly to course topics (graph / structural / logic coverage).

### 4. Build Agent 1 — Code Generator
- **What:** Takes a problem description → returns a Python function.
- **How:** System prompt defining its role + strict output format (only code in a ```python block). Parse the code out of the response.
- **Why:** Produces the "unit under test".

### 5. Build Agent 2 — Test Case Generator
- **What:** Takes the code (+ problem + chosen goal) → returns unit tests (pytest).
- **How:** System prompt stating the goal, e.g. *"Write pytest tests achieving 100% branch coverage of the function below."* Enforce a fixed output format.
- **Why:** Core of the project: goal-directed test generation.

### 6. Build Agent 3 — Test Case Executor
- **What:** Runs the tests against the code and produces a verdict.
- **How:** Write code + tests to files, run `pytest` (with `coverage run --branch` for option 1) via `subprocess` with a timeout. Parse results into a structured verdict (JSON): tests passed/failed, coverage %, goal met or not. Optionally use an LLM to summarize the verdict.
- **Why:** Closes the loop: tells you whether the tests meet the goal and whether the code is correct.

### 7. (Optional) Feedback loop
- **What:** If goal not met (e.g. coverage < target), send missing lines/branches back to Agent 2 and retry (max N iterations).
- **How:** Simple `for` loop in Python — no framework.
- **Why:** Shows agentic behaviour and improves results; good for the demo.

### 8. Log everything
- **What:** Save per problem: prompts (system + user), model, temperature, top_p, max_tokens, generated code, generated tests, verdict.
- **How:** Write a JSON/JSONL file per run.
- **Why:** The report explicitly requires prompts, settings, formats, and actual outputs.

### 9. Run on the dataset subset & collect results
- **What:** Run the pipeline over the chosen problems.
- **How:** Produce a results table (problem id, tests passed, coverage %, goal met, iterations).
- **Why:** Required as "execution results of the test case generator and executor agents".

### 10. Write the report
Must include:
1. Pipeline description: dataset + test-generator functionality (with a diagram)
2. User prompts, system prompts, and settings (temperature etc.)
3. Format of generated code, tests, verdict + actual samples
4. Results (coverage/property: execution results; statistical: metrics used)
5. Each member's contribution

### 11. Prepare the demo
- **What:** Live run on 1–2 problems showing all three agents and the verdict.
- **Why:** Demos are evaluated in class; everyone must be present.

---

## Pipeline at a glance

```
Dataset problem ─► [Code Generator] ─► code ─► [Test Generator + goal] ─► tests ─► [Executor] ─► verdict
                                                        ▲                                        │
                                                        └──── missing coverage (optional) ◄──────┘
```

## Checklist
- [ ] Dataset loaded (MBPP/HumanEval subset)
- [ ] LLM API key working
- [ ] Testing goal chosen
- [ ] Agent 1: code generator
- [ ] Agent 2: test generator
- [ ] Agent 3: executor + verdict
- [ ] (Optional) feedback loop
- [ ] Logging of prompts/settings/outputs
- [ ] Results table
- [ ] Report written (5 sections)
- [ ] Demo rehearsed
