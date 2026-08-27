# ATHENA ENGLISH DIAGNOSTIC SPEC v1.1

**Status:** Draft for Codex adversarial review
**Supersedes:** ATHENA ENGLISH DIAGNOSTIC SPEC v1
**Programme:** Athena — Six-Week War Room
**Subject:** Cambridge IGCSE First Language English (0500)
**Scope of this document:** Specification only. No application code, migrations, or Day 1 implementation is produced here.

**Provenance note:** This revision was produced from (a) ATHENA ENGLISH DIAGNOSTIC SPEC v1 in full, and (b) the v1.1 task brief containing the mandatory adversarial-review findings. No live Athena repository or codebase was available to inspect in this session. Where the brief referenced existing platform concepts (Digital Twin, Chief Examiner, Six-Week War Room, Mark-Loss Ledger), this spec treats those names as fixed points to integrate with, but does not assert knowledge of their current implementation. Any engineer integrating this spec should reconcile Section 22 (Digital Twin Integration) against the actual current data model before building, and flag any mismatch rather than silently resolving it.

---

## 1. Purpose

Athena English exists to answer one question:

> What specific English-language weaknesses are preventing this student from earning marks, and what is the fastest evidence-based route to recovering those marks?

This document specifies the diagnostic and evidence architecture that makes that question answerable *auditably* — such that every claim Athena makes about a student is traceable to specific evidence, and every automated judgment (LLM-authored passage, LLM-marked response, LLM-classified error, LLM-selected next step) has a defined owner, a defined confidence representation, and a defined human checkpoint where the six-week campaign requires one.

The immediate build target remains the Day 1 B1-aligned Reading Diagnostic, but this spec defines the architecture generally enough that Days 2–7 and later Cambridge-specific diagnostics can be built on the same contract without re-litigating these questions.

---

## 2. Non-Goals

Athena English v1.1 does **not**:

1. Certify an official CEFR level for any student. Athena produces B1-aligned / B2-aligned *diagnostic evidence*, never a `CEFR_CERTIFIED` claim.
2. Predict a final Cambridge 0500 grade from CEFR-aligned diagnostic performance. No CEFR→grade conversion table is authorised anywhere in this system.
3. Produce a comprehensive English-proficiency diagnosis from a single sitting. One assessment produces one evidence point, not a stable trait estimate.
4. Treat AI-generated content (passages, questions, rubrics, marks, root causes, routing decisions) as ground truth without a defined review/approval path.
5. Build a general-purpose psychometrics platform. This is a six-week, two-student campaign. Human approval in the loop is a *feature*, not a gap to be automated away under time pressure.
6. Implement Day 1, write migrations, or touch application code. This document is the contract that implementation must satisfy.

---

## 3. Terminology

Precise, non-conflatable definitions. Implementers must not substitute one term for another.

| Term | Definition |
|---|---|
| **B1-aligned** | An Athena-authored assessment designed against B1-oriented criteria (CEFR used as a *design framework*, not an authority Athena can certify against). |
| **Working instructional level** | The level Athena is *currently choosing to teach at*, based on evidence so far. Provisional, revisable, not a certification. |
| **CEFR-certified level** | A level attested by an appropriate external, validated assessment. Athena never issues this claim itself. |
| **Cambridge 0500 performance** | Evidence generated from authentic Cambridge-aligned examination material (official past papers, mark schemes, Cambridge-style tasks built to 0500 spec). |
| **Performance** | The raw result a student achieved on a specific piece of evidence (e.g., 3/4 on inference questions in one sitting). |
| **Marker confidence** | Confidence in one specific marking/classification *event* (e.g., how sure the AI marker is that a particular criterion score is correct). |
| **Evidence confidence** | How strongly the *totality* of available evidence supports a broader claim about the student (a function of quantity, diversity, consistency, and marker confidence — see Section 19). |
| **Evidence Claim** | A first-class, auditable record of something Athena believes about a student, together with the evidence and confidence basis for believing it (Section 18). |
| **Transfer** | Successful application of a skill to material genuinely unseen by the student in that context, as distinct from a reattempt (Section 21). |
| **Root cause** | A best-available explanation for a specific wrong answer, always carrying a confidence level and never asserted as certain in the absence of evidence (Section 17). |

Marker confidence and evidence confidence are never used interchangeably in any UI, API response, or Digital Twin field. A field or display labelled "confidence" without qualifying which kind is a spec violation.

---

## 4. Governing Principles

These principles are non-negotiable and every subsequent section must be checkable against them.

1. **Athena does not store what it believes about a student without also storing why it believes it.** (Every skill estimate is backed by an Evidence Claim, not a bare number.)
2. **No single LLM invocation authors and certifies its own output.** Generation, review, and approval are separate actors with separate responsibilities (Section 9–11).
3. **CEFR-aligned evidence and Cambridge 0500 performance are related but never conflated**, and never bridged by an invented conversion formula.
4. **Confidence must be distinguished from performance.** A low score and a low-confidence estimate are different findings and must never share a single number.
5. **`UNKNOWN` is a valid, preferred answer** wherever evidence is insufficient — for root cause, for routing, for marking. Fabricated certainty is a defect, not a convenience.
6. **Reattempt is not transfer, and transfer is not retention.** These are three distinct evidence states and none substitutes for another (Section 21).
7. **Historical evidence is immutable.** Once an assessment has produced student evidence, its content is frozen; corrections are versioned, not silent edits (Section 13).
8. **The student-facing system never leaks scoring machinery** — no answer keys, rubrics, marker prompts, or calibration metadata cross into a client-observable path (Section 23).
9. **Human approval is required before any assessment reaches students**, for the duration of the six-week campaign's small assessment bank (Section 11).
10. **Routing and classification decisions are deterministic given identical inputs and a fixed policy version** — an LLM is never handed an unconstrained "decide what happens next" prompt (Section 20).

---

## 5. Evidence Hierarchy

Retained and strengthened from v1. Athena's confidence in any claim should track position in this hierarchy, not merely accumulate with score.

| Level | Evidence type | Notes |
|---|---|---|
| 0 | Self-report | Weakest possible evidence; informative only as a prior. |
| 1 | Guided exercise (with support) | Demonstrates capability-with-scaffolding, not independent skill. |
| 2 | Athena diagnostic (unseen, unassisted) | First real evidence point for a skill. |
| 3a | Same-question reattempt | **Not transfer.** Confirms the student can now do *that* item, nothing more. |
| 3b | Near-transfer question (same passage/skill, new item) | Weak-to-moderate transfer evidence. |
| 3c | Fresh-transfer question (new passage, same skill) | Genuine transfer evidence. |
| 4 | New passage, unseen | Reduces passage-specific confound. |
| 5 | New genre/topic, unseen | Reduces genre/topic confound — required before evidence diversity can be rated above LOW (Section 19). |
| 6 | Later independent sitting (time elapsed) | Retention evidence, distinct from transfer. |
| 7 | Authentic Cambridge 0500 performance | Strongest and ultimate evidence; the target the whole hierarchy serves. |

A claim's evidence confidence must reflect *which levels* its supporting evidence occupies, not merely how many data points exist at Level 2. Twenty questions from one passage remain Level 2/3b evidence no matter how many there are.

---
## 6. Day 1 Diagnostic Objective

Day 1 answers exactly one question:

> Can the student independently comprehend and respond to a straightforward B1-aligned English text sufficiently well to justify moving upward, remaining at approximately that instructional level, or probing downward?

Day 1 explicitly does **not**:

* certify CEFR B1;
* predict a Cambridge grade;
* comprehensively diagnose English ability;
* measure 0500 examination technique;
* establish stable student traits from one sitting.

Day 1's output is a single Level-2/3b evidence point (Section 5) feeding a routing decision (Section 20) — nothing more is claimed of it, and nothing more should be displayed to the student or teacher as if it were more.

---

## 7. Day 1 Assessment Blueprint

v1's 50-mark, five-section blueprint was reviewed against the narrow objective in Section 6 and found to over-scope a single sitting intended to answer one routing question in 35–45 minutes. Revised blueprint:

| Section | Skill | Marks |
|---|---|---:|
| A | Explicit comprehension | 8 |
| B | Vocabulary in context | 6 |
| C | Inference | 8 |
| D | Main idea / purpose | 4 |
| E | Short summary (approx. 80–100 words) | 8 |
| **Total** | | **34** |

Target duration: **35–45 minutes**, including reading time.

Rationale for the cut from v1:
* The original 120–150 word summary and 10-mark written-response rubric produced writing-skill evidence disproportionate to a *reading* diagnostic's stated purpose, and risked being treated informally as a writing diagnosis it was never designed to be.
* Section D (main idea/purpose) is reduced but retained at 4 marks — enough to detect gross failure, not enough to claim detailed viewpoint-analysis coverage (that work is explicitly Day-3+/Cambridge-specific per v1 Section 9).
* Any writing evidence derived from Section E must be persisted and displayed as:

  > **Preliminary writing evidence — LOW confidence** (single short response, single sitting, unscored against the full 0500 composition rubric)

  Day 1 must never be allowed to silently seed the `writing.sentenceControl` / `writing.coherence` Digital Twin dimensions at anything above LOW confidence.

---

## 8. Passage Requirements

Retained from v1 with no material change:

* Original Athena-authored passage; must not reproduce or lightly adapt a copyrighted source (British Council, Cambridge, newspaper, book, etc.).
* Approximately 500–750 words, straightforward prose, clear organisation, adult-appropriate, understandable without specialist knowledge, sufficient literal and inferential content, no deliberately obscure vocabulary.
* Must read as genuinely interesting content for an adult learner, not as a children's-level exercise merely because it targets B1 skills.

Passage requirements are necessary but not sufficient — Section 9 defines how a passage becomes an approved diagnostic instrument.

---

## 9. Assessment Authoring Pipeline

This resolves **Blocker 1** (circular passage generation/calibration).

```text
Authoring Agent
      |
Candidate Assessment  (passage + questions + proposed answer key + proposed rubric)
      |
Deterministic Analysis   (Section 10)
      |
Independent AI Assessment Review  (Section 11)
      |
Human Approval  (Section 11)
      |
Freeze / Version  (Section 13)
      |
Assessment Bank
```

**Field-level provenance is mandatory.** Every field on a Candidate Assessment and its Calibration Record must be tagged with exactly one of:

| Provenance tag | Meaning |
|---|---|
| `AUTHORED` | Produced by the Authoring Agent invocation. Never self-certifying. |
| `COMPUTED` | Produced by a deterministic, non-LLM tool (Section 10). |
| `AI_REVIEWED` | Produced or amended by the separate Review Agent invocation (Section 11). |
| `HUMAN_APPROVED` | Confirmed or amended by a human reviewer (Section 11). |

**Hard rule:** the Authoring Agent's output for CEFR-alignment fields, difficulty ratings, and answer-key correctness may never carry a final provenance tag of `AUTHORED` alone. Each such field must be advanced to at least `AI_REVIEWED`, and — before an assessment can leave `DRAFT`/`AUTOMATED_CHECKED` state — to `HUMAN_APPROVED`, per the lifecycle in Section 13.

The same model instance/session must not be reused across the Authoring Agent and Review Agent roles for a given candidate assessment (see Section 11's separation requirement).

---

## 10. Deterministic Calibration / Analysis

Between authoring and AI review, a non-LLM analysis pass computes objective statistics and attaches them to the Calibration Record with `COMPUTED` provenance:

* word count;
* sentence-length distribution (mean, range, standard deviation);
* lexical statistics (type-token ratio, proportion of words outside a defined B1 wordlist/frequency band);
* one or more standard readability indices (e.g., Flesch-Kincaid), recorded as **supplementary evidence only**;
* structural checks (paragraph count, presence of headings/lists if relevant);
* answer-key internal consistency checks (e.g., every question ID referenced has a corresponding rubric entry, no duplicate correct-answer letters across a distractor set where that would be a design flaw, no question answerable from its own stem without reading the passage — flagged for human attention, not auto-resolved).

**Hard rule, restated from v1 and now binding:** readability/lexical statistics may inform the Review Agent and human reviewer. They must never, by themselves, set or override the `intendedCefrAlignment` field. A passage scoring "B1" on an automated readability index is evidence *about* the passage, not a certification.

---

## 11. Independent AI Review and Human Approval

This resolves **Blocker 2** (undefined independent reviewer).

**Separation requirement:** the Assessment Authoring Agent and the Assessment Review Agent are logically separate invocations with separate prompts and separate responsibilities. The Review Agent's prompt supplies it with the candidate assessment artefact and the Section 10 computed statistics, and instructs it to *criticise*, not to *author or re-author*. The Review Agent must not be given the Authoring Agent's chain-of-thought or generation prompt.

**Review Agent checklist (minimum):**

* construct relevance (does each question actually measure its declared skill?);
* intended B1 alignment, weighed against Section 10 statistics;
* lexical difficulty;
* grammatical complexity;
* inferential load;
* ambiguity in stem or distractors;
* answerability without specialist knowledge;
* question quality (no trivia, no answerable-without-reading items);
* accidental clues (one question giving away another's answer);
* answer-key correctness;
* rubric quality and internal consistency;
* cultural/topic bias;
* contamination risk (resemblance to a known/published passage);
* whether each question measures its declared skill rather than an adjacent one.

The Review Agent's output is a structured critique plus proposed field-level amendments, each tagged `AI_REVIEWED`. It does not itself flip the assessment's lifecycle state.

**AI review does not constitute final approval.** For the duration of the six-week campaign and its small assessment bank, a human (the designated Chief Examiner / curriculum-lead role — reusing existing Athena terminology where that role already exists) must:

1. review the candidate assessment, the Section 10 statistics, and the Review Agent's critique side by side;
2. accept, amend, or reject each `AI_REVIEWED` field;
3. explicitly approve the assessment as a whole before it can transition to `HUMAN_APPROVED` (Section 13).

**Minimum review-queue requirements (specification only — no UI is built in this task):**

* list of assessments in `AUTOMATED_CHECKED` or `AI_REVIEWED` state awaiting approval;
* side-by-side view of passage, questions, answer key, rubric, Section 10 statistics, and Review Agent critique;
* per-field accept/amend controls preserving both the AI-proposed and human-final value (never overwrite silently);
* single explicit "Approve" action that requires all required fields to have reached at least `AI_REVIEWED` status;
* reviewer identity and timestamp recorded on approval.

---

## 12. Assessment Lifecycle and Versioning

```text
DRAFT
  -> AUTOMATED_CHECKED   (Section 10 analysis complete)
  -> AI_REVIEWED         (Section 11 Review Agent complete)
  -> HUMAN_APPROVED       (Section 11 human sign-off)
  -> FROZEN               (content locked, version number assigned)
  -> ACTIVE               (available to be served as "unseen")
  -> RETIRED               (exposed to a student; no longer eligible as unseen for that student, or withdrawn entirely)
```

**Allowed transitions and actors:**

| Transition | Actor |
|---|---|
| DRAFT → AUTOMATED_CHECKED | System (Section 10 pipeline) |
| AUTOMATED_CHECKED → AI_REVIEWED | System (Review Agent invocation) |
| AI_REVIEWED → HUMAN_APPROVED | Human reviewer only |
| HUMAN_APPROVED → FROZEN | System, automatic on approval, assigns immutable version ID |
| FROZEN → ACTIVE | Human or scheduled release action |
| ACTIVE → RETIRED (per-student) | System, automatic on that student's attempt submission |
| ACTIVE → RETIRED (global) | Human, for withdrawal/deprecation |

**Hard rule:** once an assessment has produced any `AssessmentAttempt` / `StudentResponse` evidence, its `FROZEN` content (passage, questions, answer key, rubric) must never be edited in place. Any correction produces a new version (`AssessmentVersion`), and historical attempts remain permanently bound to the version they were actually sat under. This makes Section 4's Principle 7 executable.

---

## 13. Question Taxonomy

Retained from v1 with tightened metadata requirements. Every question carries:

* question ID;
* maximum marks;
* primary skill (from the Digital Twin skill list, Section 22);
* secondary skill(s), where genuinely applicable — optional, not forced;
* expected evidence / passage-line reference;
* correct answer or rubric reference;
* difficulty estimate, with provenance tag (Section 9);
* Review Agent and human-reviewer sign-off status.

Question design must avoid: ambiguous distractors, trivia, specialist knowledge, obscure vocabulary unrelated to the construct, questions answerable without reading, excessive single-sentence dependence, and accidental cross-question clues (all checked explicitly in Section 11's Review Agent checklist and human pass).

---

## 14. Marking Model

Objective (deterministically scorable) questions are scored server-side by a deterministic scorer against the frozen answer key — no LLM involvement, no ambiguity.

Open responses (Section 7's summary task, and later composition tasks) use an LLM-based examiner, governed by the structured result and uncertainty model in Section 15.

---

## 15. LLM Marking — Uncertainty Model

This resolves **Blocker 4 of the brief's marking section** ("uncertainty must be retained" made operational).

**Structured marker result (minimum shape):**

```text
criterion
awarded_marks
max_marks

evidence[]
  student_span
  rationale

marker_confidence        (0.0-1.0)

uncertainty
  present                (bool)
  reason                 (free text, required if present = true)

model
model_version
prompt_version
rubric_version

requires_review          (derived, see policy below)
```

**Initial engineering confidence policy** (explicitly labelled as engineering policy, not a validated psychometric threshold, and versioned so it can be revised without silently changing historical interpretation):

```text
marker_confidence >= 0.85        -> ACCEPTED
0.65 <= marker_confidence < 0.85 -> PROVISIONAL
marker_confidence < 0.65         -> SECOND_MARKER_REQUIRED
```

**Defined failure/disagreement behaviour:**

* **Two markers disagree on score:** the criterion is flagged `SECOND_MARKER_REQUIRED` regardless of either individual confidence value; final score requires human resolution before the mark is treated as ACCEPTED.
* **Scores agree but rationales materially disagree:** flagged `requires_review = true` with `uncertainty.reason` populated; the score may still post provisionally but the Evidence Claim built from it inherits `marker_confidence: MODERATE` at best, never HIGH, until reviewed.
* **Response is ambiguous** (marker cannot locate sufficient evidence spans to support any awarded-marks value with confidence ≥ 0.65): result is `SECOND_MARKER_REQUIRED`, awarded marks withheld from automatic Digital Twin ingestion until resolved.
* **Evidence spans cannot support the awarded marks** (a deterministic post-check: cited spans absent from the actual submitted text, or insufficient in length/relevance): the result is rejected outright and re-run; a repeated failure escalates to human marking, not to a third automatic retry.
* **Model invocation fails** (timeout, error, malformed output): the attempt is queued for human marking; it is never silently scored as zero or omitted from the student's record without an explicit `PENDING_HUMAN_MARK` state visible to the student's teacher.

Raw marking evidence (prompt version, model version, full structured result) is retained indefinitely for audit, independent of whether the mark was later revised.

---

## 16. Root-Cause Taxonomy

This resolves **Blocker 4** (overlapping root-cause categories).

v1's taxonomy is retained as a vocabulary but is no longer treated as mutually exclusive. The model becomes:

```text
Observed Failure
      |
Primary Probable Cause     (exactly one, or UNKNOWN)
      +
Contributing Cause(s)      (0..n, same taxonomy allowed)
      +
Confidence                 (per assignment)
      +
Evidence                   (what in the response/context supports this)
```

**Rules:**

* `UNKNOWN` is always a valid Primary Probable Cause and must be preferred over a low-confidence guess. Assigning `UNKNOWN` never blocks assessment completion, scoring, or Digital Twin ingestion of the raw performance data — it only withholds a diagnostic interpretation.
* A Primary Probable Cause may be assigned only when there is direct evidence in the response, timing data, or answer-change history pointing to that specific cause (e.g., a correct answer changed to an incorrect one under time pressure supports `TIME_PRESSURE`; a response that restates passage text without transformation supports `SUMMARY` or `LANGUAGE_INTERPRETATION` depending on context).
* The same taxonomy label is permitted as both Primary Cause for one failure and Contributing Cause for another — there is no reserved "primary-only" or "contributing-only" subset.
* **Contradictory evidence handling:** if evidence supports two candidate primary causes roughly equally, Athena records both as Contributing Causes with comparable confidence and sets Primary Probable Cause to `UNKNOWN` rather than arbitrarily picking one.
* **Revision after transfer testing:** a root-cause claim is not static. If a student subsequently succeeds on a fresh-transfer item (Section 5, Level 3c) targeting the same skill, the original claim's status moves from `OPEN` toward `IMPROVING`/`RECOVERED` per Section 21's state machine, and the root-cause record is annotated with the superseding evidence rather than deleted (Section 18's supersession model).

---

## 17. Evidence Claims

This resolves the brief's **Evidence Claims** requirement and operationalises Governing Principle 1.

An **Evidence Claim** is the atomic, persisted unit of "what Athena believes and why." Athena never persists a bare skill percentage without an accompanying claim.

**Minimum fields:**

* claim ID;
* student ID;
* skill (from Digital Twin skill list);
* claim statement / type (e.g., "demonstrates B1-aligned inference performance");
* evidence references (assessment ID, question IDs, attempt ID);
* context (passage genre/topic, sitting date, evidence-hierarchy level per Section 5);
* performance (raw result, e.g., 3/4);
* evidence quantity (Section 19);
* evidence diversity (Section 19);
* consistency (Section 19);
* marker confidence (Section 15, where applicable);
* overall evidence confidence (Section 19, derived);
* status (`SUSPECTED` / `OBSERVED` / `IMPROVING` / `PROVISIONALLY_RECOVERED` / `VERIFIED_RECOVERED` — see Section 21);
* created / updated timestamps;
* supersession relationship (a claim may be superseded by a later claim on the same skill; the prior claim is never deleted, only marked superseded, preserving full history).

**Supersession behaviour:** new evidence never silently overwrites an existing claim's belief content. Instead it either:

* **supports** — increases evidence quantity/consistency, may raise evidence confidence;
* **weakens** — evidence points the other way but not strongly enough to reverse the claim; confidence lowered, claim retained;
* **contradicts** — evidence materially conflicts with the claim; a new claim is created noting the contradiction, and the human/teacher view surfaces the conflict rather than auto-resolving it;
* **supersedes** — sufficient new evidence (typically crossing an evidence-hierarchy level, e.g., fresh-transfer or new-genre) justifies retiring the old claim's active status in favour of a new one, with an explicit link back to what it supersedes.

---
## 18. Evidence-Confidence Model

This resolves the brief's request to replace simplistic single-number confidence.

Four inputs, each rated on a simple, transparent, deterministic scale (LOW / MEDIUM / HIGH) rather than a false-precision statistical score:

| Input | Definition | Example rule of thumb |
|---|---|---|
| **Evidence quantity** | How many independent items/attempts contribute to the claim. | <5 items = LOW, 5–14 = MEDIUM, 15+ = HIGH |
| **Evidence diversity** | Spread across passages, genres, topics, question types, sittings, time. | Single passage/single sitting = LOW regardless of quantity; 2–3 passages or genres = MEDIUM; 4+ passages spanning genres and at least 2 sittings = HIGH |
| **Evidence consistency** | Does the student repeatedly demonstrate (or fail to demonstrate) the skill, or is performance volatile across items? | Computed from variance in per-item correctness within the same skill |
| **Marker confidence** | For claims resting on open-response marking, the aggregated marker confidence per Section 15. | N/A (HIGH) for deterministically scored items |

**Overall Evidence Confidence** is derived deterministically from these four inputs via a documented, versioned lookup policy (e.g., overall confidence cannot exceed the *lowest* of the four inputs, with defined exceptions documented in the policy version). The exact lookup table is an engineering artefact to be finalised during implementation, but the rule that **quantity alone cannot produce HIGH evidence confidence while diversity is LOW** is a hard constraint of this spec (see Acceptance Criteria, Section 30).

**Worked example (replaces v1's bare "Inference: 40%"):**

```text
INFERENCE

Performance          71%
Evidence quantity    MEDIUM
Evidence diversity   LOW
Consistency          MEDIUM
Marker confidence    HIGH

Evidence confidence  LOW-MEDIUM

Basis:
- one B1-aligned passage
- four inference questions
- one sitting
```

Twenty questions from one passage in one genre remain LOW on diversity no matter how many questions there are — quantity and diversity are independent axes, and every displayed skill result must show both, not a single collapsed number.

---

## 19. Adaptive Routing Policy

This resolves **Blocker 3** (undefined adaptive routing).

All thresholds below are explicitly labelled **Athena intervention-policy thresholds** — engineering decisions about what evidence to collect next, not validated CEFR cut scores, and not a claim about the student's true ability. They are versioned (`routingPolicyVersion`) so that routing behaviour is reproducible and auditable over time.

**Day 1 routing policy v1.1:**

```text
overall >= 85%
AND explicit_comprehension >= 80%
AND vocabulary >= 75%
AND inference >= 70%
        -> Route: B2 boundary probe

overall in [70, 84]%
        -> Route: second independent B1 diagnostic (new passage/genre)

overall in [50, 69]%
        -> Route: B1 instruction + targeted remediation on weakest skill(s)

overall < 50%
        -> Route: A2/B1 boundary probe
```

**Skill-vector override:** the overall-score bands above are necessary but not sufficient. If any individual skill dimension scores more than 20 percentage points below the overall score, the routing engine overrides the overall-score route and instead routes to targeted remediation on that skill, regardless of which overall band was hit. This prevents a strong-explicit-comprehension, weak-inference profile from being routed as if uniformly strong.

**Boundary behaviour:** band boundaries are inclusive on the lower bound as written above (e.g., exactly 70% routes to "second independent B1 diagnostic," not "B2 boundary probe"). Scores are computed to one decimal place before banding to avoid rounding ambiguity at boundaries.

**Missing-data behaviour:** if any Section (A–E) was not attempted or could not be scored (e.g., `PENDING_HUMAN_MARK` per Section 15), the routing engine does **not** substitute a zero or an average. It routes to a defined `INSUFFICIENT_DATA` state requiring either (a) waiting for the pending mark, or (b) human routing decision, and this is logged distinctly from a genuine low-score route.

**Disagreement between overall score and skill profile:** handled by the skill-vector override above; there is no case where the routing engine is permitted to silently pick one signal over the other without applying the documented override rule.

**Low-confidence open-response marking:** if Section E's mark is `PROVISIONAL` or `SECOND_MARKER_REQUIRED` (Section 15), the routing engine computes the overall score using only Sections A–D for routing purposes, flags the route as `ROUTED_WITHOUT_SUMMARY_MARK`, and re-evaluates once the Section E mark is finalised. A provisional writing signal never gates a reading-diagnostic routing decision.

**Determinism and audit:** routing is a pure function of (skill scores, routing policy version) — never an unconstrained LLM call. Every routing decision is persisted with the policy version used, the input scores, and the resulting route, so that "given identical validated results and policy version X, two executions produce the same route" is testable (Section 30).

---

## 20. Transfer / Recovery Policy

Retained and made more explicit from v1.

**Distinct evidence types, never substituted for one another:**

1. Same-question reattempt — confirms the specific item only.
2. Near-transfer question — same passage/skill, new item.
3. Fresh-transfer question — new passage, same skill.
4. New passage.
5. New genre/topic.
6. Later independent sitting (time elapsed) — retention, not transfer.
7. Authentic Cambridge 0500 performance — the ultimate evidence.

**Weakness/recovery state machine:**

```text
SUSPECTED
    -> OBSERVED                (Level-2 diagnostic evidence confirms the weakness)
    -> IMPROVING                (intervention underway; near-transfer success only)
    -> PROVISIONALLY_RECOVERED  (fresh-transfer success, Level 3c, single instance)
    -> VERIFIED_RECOVERED       (fresh-transfer success repeated across a new passage/genre
                                  and/or a later independent sitting, Levels 4-6)
```

**Hard rule (restated as testable in Section 30):** a same-question successful reattempt can never, alone, move a weakness past `IMPROVING`. Only fresh-transfer or later evidence can reach `PROVISIONALLY_RECOVERED`, and only repeated fresh-transfer/retention evidence can reach `VERIFIED_RECOVERED`.

---

## 21. Digital Twin Integration

Athena English v1.1 does not redesign the Digital Twin; it specifies the contract English diagnostics must honour when writing into it.

* Every write to a Digital Twin skill dimension must originate from an Evidence Claim (Section 17), never a bare score.
* Skill dimensions retained from v1: Explicit Comprehension, Vocabulary in Context, Inference, Main Idea, Purpose/Viewpoint, Summary, Written Expression, Grammar/Sentence Control, Coherence, Reading Speed — plus later Cambridge-specific dimensions (Language Analysis, Writer's Effect, Information Selection, Synthesis, Directed Writing, Audience Awareness, Register, Composition, Examination Pacing), unchanged from v1 Section 12.
* Each dimension stores: current estimate, evidence confidence (Section 19, not a bare number), evidence count, evidence diversity summary, last measured, trend, open defects (linked root-cause records, Section 16), recovery state (Section 21).
* CEFR-aligned evidence and Cambridge 0500 performance are stored as related but distinct evidence streams feeding the same skill dimension — never merged into one number via an invented formula (Governing Principle 3). Where both exist for a dimension, the UI/API must be able to display them side by side without collapsing them.
* Any integration point where this spec's field names diverge from the actual current Digital Twin schema must be reconciled explicitly by the implementing engineer, per the provenance note in the front matter — this document does not assert it has seen that schema.

---

## 22. API / Security Requirements

Retained and clarified from v1.

**Student-facing assessment payload** (`GET /api/english/assessments/{id}`) must never include:

* answer keys;
* hidden scoring rules;
* mark schemes / rubrics;
* examiner notes;
* calibration metadata that would compromise the assessment (e.g., which items are the "hard" discriminators);
* LLM marker system prompts or rubric text.

**Submission** (`POST /api/english/assessments/{id}/attempts/{attemptId}/submit`) triggers server-side scoring only. Deterministic scoring and LLM-based marking (Section 15) both occur server-to-server; the rubric may be supplied to the marking process but must never transit through any client-observable path, including client-side logging, browser network tabs, or any proxying layer the student's device can inspect.

This is a security *expectation*, not an implementation spec — the exact backend architecture (queueing, service boundaries) is left to implementation, provided the above boundary is honoured and testable (Section 30).

---

## 23. Data Model Requirements

Minimum conceptual entities (retained/extended from v1 Section 23):

```text
Assessment
Passage
CalibrationRecord        (new — Section 9/10, field-level provenance)
Question
QuestionRubric
AssessmentVersion
AssessmentAttempt
StudentResponse
Mark
MarkerResult              (new — Section 15 structured shape)
RootCauseAssignment        (new — Section 16, primary + contributing)
SkillEvidence
EvidenceClaim              (new — Section 17)
DiagnosticFinding
Intervention
TransferTest
RetentionTest
RoutingDecision             (new — Section 19, with policy version)
SyllabusVersion              (new — Section 24)
```

A passage/assessment is reusable for controlled purposes but must be retired from "unseen diagnostic" status for a given student once they have attempted it (`RETIRED` per-student transition, Section 12).

---

## 24. CEFR Interpretation Constraints

Restated as binding constraints, not guidance:

* Athena may design assessments *against* CEFR descriptors.
* Athena must never state or imply that a student "is CEFR B1/B2/C1."
* Acceptable: "Performance is consistent with the skills targeted by this Athena B1-aligned diagnostic."
* Not acceptable, anywhere in UI, API, or generated report text: "The student is officially CEFR B1," or any phrasing a reasonable reader would interpret as a certification.
* No `CEFR_CERTIFIED` claim type exists in this system. If a future need arises for genuine certification, it requires an external validated assessment and a separate spec.

---

## 25. Cambridge 0500 Relationship

Retained from v1. Cambridge performance is the ultimate examination evidence (Evidence Hierarchy Level 7). CEFR-aligned diagnostics identify underlying weaknesses; Cambridge-style/official past-paper work shows whether those weaknesses actually cost examination marks. The two streams are related (they should move together over six weeks) but are never merged via an invented conversion formula (Governing Principle 3), and Digital Twin dimensions must be able to display both without collapsing them (Section 22).

Cambridge-specific failures feed the existing Athena Mark-Loss Ledger, unchanged in structure from v1 Section 18.

---

## 26. Syllabus / Version Provenance

This resolves the brief's syllabus-currency requirement.

Every assessment artefact that claims Cambridge 0500 alignment must record:

* syllabus identifier (e.g., "Cambridge IGCSE First Language English 0500");
* syllabus version/cycle (e.g., "2024–2026");
* source/provenance (e.g., link or reference to the official syllabus document used);
* effective dates;
* review date (a date by which the syllabus-currency assumption must be re-checked — not left open-ended);
* status (`CURRENT` / `SUPERSEDED` / `UNDER_REVIEW`).

**Hard rule:** a future syllabus change must never silently reinterpret historical evidence. Historical `AssessmentAttempt` and `EvidenceClaim` records retain the `SyllabusVersion` that was current at the time of the attempt; they are never retroactively relabelled against a newer syllabus.

---

## 27. Auditability

Consolidated audit requirements (previously scattered across v1):

* Every field with non-deterministic (LLM) origin carries provenance, model, and prompt/version metadata (Section 9, 15).
* Every routing decision is persisted with its policy version and inputs (Section 19).
* Every root-cause assignment carries its evidence and confidence, and is never overwritten — only superseded (Section 16, 17).
* Every marking event's raw structured result is retained regardless of whether the mark was later revised (Section 15).
* Every assessment-lifecycle transition records actor and timestamp (Section 12).
* Frozen assessment content is immutable; corrections are new versions (Section 12).

---

## 28. Six-Week Operating Model

For the two-student, six-week campaign:

* Human approval (Section 11) is mandatory and is treated as a feature — it is the primary safeguard against a small, fast-moving assessment bank silently drifting on unreviewed AI output.
* No infrastructure beyond what Sections 9–21 require should be built. In particular: no general reviewer-assignment workflow, no multi-tenant review queue, no statistically sophisticated confidence model — the deterministic LOW/MEDIUM/HIGH policy in Section 19 is intentionally simple and sufficient for two students.
* Day 1 remains the only assessment this spec authorises for near-term implementation; Days 2–7 reuse this same pipeline and contract without requiring a new spec, provided they do not introduce new claim types, routing logic, or marking behaviour not covered here.

---

## 29. Acceptance Criteria

Testable, not aspirational:

1. Given identical validated assessment results and a fixed `routingPolicyVersion`, two independent executions of the routing engine produce the same route.
2. A student-facing assessment API response (`GET /api/english/assessments/{id}`) contains no answer key, rubric, hidden calibration metadata, or marker system prompt — verifiable by schema/contract test.
3. An assessment in `ACTIVE` or later state cannot be modified in place once any `AssessmentAttempt` references its version; any edit attempt must instead produce a new `AssessmentVersion`.
4. No Day 1 result, UI string, or API response can produce or imply a `CEFR_CERTIFIED` claim; no such claim type exists in the data model.
5. A skill's Evidence Claim derived from a single passage and single sitting cannot report evidence diversity above LOW, regardless of question count.
6. A failed or low-confidence root-cause classification may persist as `UNKNOWN` without blocking assessment scoring, Digital Twin ingestion of raw performance, or student progression to the next assignment.
7. A same-question successful reattempt alone cannot transition a weakness's recovery status past `IMPROVING`.
8. A `PROVISIONAL` or `SECOND_MARKER_REQUIRED` open-response mark cannot be included in a routing decision without the route being explicitly flagged `ROUTED_WITHOUT_SUMMARY_MARK`.
9. The Authoring Agent invocation and the Review Agent invocation for a given candidate assessment are logically separate calls with separate prompts, verifiable via the provenance/model-version metadata attached to each field.
10. No assessment can reach `ACTIVE` state without a recorded `HUMAN_APPROVED` transition, actor, and timestamp.
11. A historical `AssessmentAttempt`'s bound `SyllabusVersion` is never altered after the fact, even if a newer syllabus version is later marked `CURRENT`.

---

## 30. Open Questions / Deferred Work

* Exact numeric lookup table mapping the four evidence-confidence inputs (Section 19) to an overall LOW/MEDIUM/HIGH value — this spec fixes the inputs and the hard constraint (quantity cannot compensate for diversity) but leaves the precise lookup table to be finalised and versioned during implementation.
* Identity of the human reviewer role for the six-week campaign (Chief Examiner, curriculum lead, or a named individual) — assumed to reuse an existing Athena role but not confirmed against a live system in this session.
* Whether Section 10's B1 wordlist/frequency band is an existing Athena asset or needs sourcing/construction.
* Review-queue UI is specified at requirement level only (Section 11); no interface design is produced here.
* The truncated ending of v1 (Section 29, point 9, "update the English...") could not be recovered from any available source in this session — no repository or version history was accessible. This content has not been invented; if the original intent is recoverable from Athena's actual repository history, it should be restored there and reconciled with this document's Section 29 (Acceptance Criteria), which supersedes v1's incomplete Section 29 in spirit but was written fresh rather than as a repair of the missing text.

---

## Appendix A: v1 → v1.1 Change Log

* Reduced Day 1 blueprint from 50 marks / five sections (including a 120–150 word, 10-mark written response) to 34 marks / five sections (80–100 word, 8-mark summary), to match the narrowed Day 1 objective (Section 6–7).
* Introduced the Assessment Authoring Pipeline with mandatory field-level provenance tagging, separating Authoring Agent, deterministic analysis, Review Agent, and human approval (Sections 9–11) — replaces v1's informal "Generate → checks → review → calibrate → approve" list with binding actor separation and provenance rules.
* Replaced v1's implicit single "CEFR alignment" judgment with a rule that CEFR-alignment and difficulty fields can never rest solely on `AUTHORED` provenance.
* Replaced v1's vague "weak / borderline / strong / exceptional" routing language with an explicit, versioned Adaptive Routing Policy including thresholds, skill-vector override, boundary behaviour, missing-data behaviour, and provisional-mark handling (Section 19).
* Replaced v1's flat, mutually-exclusive root-cause taxonomy with a Primary + Contributing Causes model carrying confidence and evidence, with explicit contradictory-evidence and post-transfer revision rules (Section 16).
* Introduced structured LLM Marker Result and a versioned, explicitly-labelled engineering confidence policy, with defined behaviour for marker disagreement, ambiguous responses, unsupported evidence spans, and invocation failure (Section 15).
* Introduced Evidence Claims as a first-class, superseding-not-overwriting data entity (Section 17), replacing v1's bare skill-percentage storage.
* Replaced v1's single confidence number with a four-input Evidence-Confidence Model (quantity, diversity, consistency, marker confidence) and a hard rule that quantity cannot substitute for diversity (Section 18).
* Added explicit Assessment Lifecycle states and transitions with immutability of frozen, evidence-bearing content (Section 12).
* Added Syllabus/Version Provenance requirements so syllabus currency is an explicit, reviewable field rather than a silent assumption (Section 26).
* Rewrote all worked examples throughout the document to display evidence confidence alongside performance, per the brief's requirement that examples model the architecture correctly.
* Added testable Acceptance Criteria (Section 29) replacing v1's narrative "Day 1 is complete when..." list.
* Documented that v1's truncated final section could not be recovered in this session (Open Questions, Section 30) rather than inventing replacement content.

---

## Appendix B: Resolved Adversarial Findings

| # | Finding | v1 status | v1.1 resolution |
|---|---|---|---|
| 1 | Circular passage generation/calibration | Blocker | Section 9 — Authoring/Review/Human pipeline with mandatory field provenance tags; CEFR-alignment fields can never rest on `AUTHORED` alone. |
| 2 | Undefined independent reviewer | Blocker | Section 11 — separate Review Agent invocation with defined checklist; human approval mandatory before `ACTIVE`; review-queue minimum requirements specified. |
| 3 | Underspecified adaptive routing | Blocker | Section 19 — explicit versioned thresholds, skill-vector override, boundary/missing-data/provisional-mark behaviour, determinism requirement. |
| 4 | Overlapping root-cause taxonomy | Blocker | Section 16 — Primary + Contributing Cause model with confidence, evidence, contradictory-evidence handling, and post-transfer revision. |
| 5 | LLM marking uncertainty not operationalised | Non-blocker | Section 15 — structured marker result, versioned confidence policy, defined disagreement/ambiguity/failure behaviour. |
| 6 | Assessment security gap in AI-marking pathway | Non-blocker | Section 22 — explicit server-to-server marking boundary; rubric never transits a client-observable path. |
| 7 | Confidence model conflated sample size and evidence diversity | Non-blocker | Section 18 — four-input model (quantity, diversity, consistency, marker confidence) with hard rule that quantity cannot substitute for diversity. |
| 8 | Syllabus-currency claim hardcoded with no review mechanism | Non-blocker | Section 26 — `SyllabusVersion` entity with review date and status; historical attempts bound permanently to their syllabus version. |
| 9 | Day 1 scope creep vs. stated narrow objective | Non-blocker | Sections 6–7 — objective restated narrowly; blueprint reduced from 50 to 34 marks; summary evidence explicitly labelled preliminary/low-confidence. |
| 10 | Document truncated mid-sentence | Non-blocker (cosmetic but load-bearing for trust) | Not silently repaired — Section 30 documents that the original ending could not be recovered in this session, and Section 29 was written fresh as testable acceptance criteria rather than presented as a restoration of the missing text. |

---

## Appendix C: Final Adversarial Self-Check

Applying the question the brief requires — *could two independent engineering teams implement this specification and arrive at materially equivalent behaviour?* — against the categories the brief specifies:

* **Undefined thresholds:** routing thresholds (Section 19) and marker-confidence bands (Section 15) are now explicit and versioned. The evidence-confidence *lookup table* (Section 18) is intentionally left as an open item (Section 30) rather than falsely presented as fully specified — flagged, not hidden.
* **Undefined owners:** human-approval role, Review Agent separation, and lifecycle-transition actors are named per transition (Section 12, 11).
* **Subjective state transitions:** lifecycle (Section 12) and recovery state machine (Section 21) transitions are now tied to specific evidence conditions, not judgment calls.
* **Ambiguous terminology:** Section 3 forces a single meaning per term; marker confidence and evidence confidence are never interchangeable.
* **Circular validation:** resolved by mandatory Authoring/Review separation and provenance tagging (Section 9, 11).
* **Hidden LLM decisions:** routing is a deterministic function of scores and policy version, not a free-text LLM instruction (Section 19, Governing Principle 10).
* **Missing failure behaviour:** invocation failure, marker disagreement, ambiguous responses, and missing-data routing are all explicitly defined (Section 15, 19).
* **Mutable historical evidence:** explicitly prohibited (Section 12, Governing Principle 7), with versioning as the only correction path.
* **Security leaks:** explicit payload exclusion list and server-to-server boundary (Section 22).
* **False CEFR claims:** no `CEFR_CERTIFIED` claim type exists; Section 24 makes the constraint binding and testable (Acceptance Criterion 4).
* **False confidence:** Section 18's four-input model with the quantity-cannot-substitute-for-diversity rule directly targets this.
* **Conflation of performance and evidence confidence:** Section 3 and every worked example (Section 18) display them as separate fields, never one number.

No further blocking issues were identified in this self-check. The remaining gaps are itemised honestly in Section 30 rather than resolved by assertion.

---

## Recommendation

**READY FOR CODEX ADVERSARIAL REVIEW**

Reasoning: all four release-blocking findings from the prior adversarial pass have been converted into specific, testable, versioned mechanisms (Sections 9, 11, 16, 19), and the previously vague marking-uncertainty, confidence, syllabus-currency, and scope-creep issues have been operationalized alongside them. The remaining open items (Section 30) are genuinely deferred implementation details — a confidence lookup table, role-naming against the live system, wordlist sourcing, and the unrecoverable v1 truncation — none of which block a further adversarial pass, and all of which are explicitly flagged rather than silently assumed away. This document does not claim to have inspected a live Athena codebase; that reconciliation step should happen before or during Codex's review, not be treated as already complete.
