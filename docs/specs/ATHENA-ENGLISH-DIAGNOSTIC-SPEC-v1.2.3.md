# ATHENA ENGLISH DIAGNOSTIC SPEC v1.2.3

**Status:** Draft for final Codex implementation-gate re-review (corrective revision)
**Supersedes:** ATHENA ENGLISH DIAGNOSTIC SPEC v1.2.2 (surgical determinism patch only — not a redesign)
**Programme:** Athena — Six-Week War Room
**Subject:** Cambridge IGCSE First Language English (0500)
**Scope of this document:** Specification only. No application code, migrations, or Day 1 implementation is produced here.

**Revision note:** v1.2.3 is a narrow corrective patch to v1.2.2, closing exactly two normative defects the latest Codex implementation-gate re-review raised against v1.2.2's own SUM-002 and SUM-003 language — reusing those finding IDs because the counterexamples target the same two rules v1.2.2 last touched, not new areas of the spec. **SUM-002:** v1.2.2's five-step spelling classification correctly identified `ACCEPTED`/exempt tokens and named `UNKNOWN_SPELLING_TOKEN`, but never defined a deterministic path by which a token becomes `MISSPELLED` — leaving two compliant implementations free to diverge completely on identical non-accepted tokens (Section 18). **SUM-003:** v1.2.2 required copied-run contiguity in both sequences, but its residual "overlapping candidates are merged into the maximal span" instruction was not restricted to matches sharing the same response/passage alignment, so overlapping response coordinates on crossing or unrelated passage alignments could be unioned into a span that is not itself a valid contiguous match (Section 18). MARK-002 (Unicode normalization) and all seven v1.2→v1.2.1 findings (CONF-001, ROUTE-001, MARK-001, SUM-001, CLAIM-001, REC-001, SEC-001) remain resolved and are not reopened by this revision. This revision does not redesign the architecture, implement Day 1, or produce application code or migrations. Section 47 is the resolution matrix for this gate; Section 48 is the v1.2.2→v1.2.3 change log.

**Provenance note (carried forward from v1.2, unchanged):** v1.1 was written without access to the live Athena repository and said so. v1.2 was produced *with* full read access to `/home/david/src/github.com/athena/recon-week` (the frontend, a single `index.html`) and the live backend repositories at `/home/david/src/github.com/athena/backend` (the current Spring Boot "Chief Examiner" service — Flyway migrations `V1__init.sql`, `V2__marking_missions.sql`, and the `ai.athena.examiner` domain/service/web packages) and `/home/david/src/github.com/athena/athena-backend` (an earlier snapshot of the same service). Every claim in this document about what currently exists in Athena is grounded in those files, not assumed. Appendix C (Repository Reconciliation Matrix, Section 38) records the concept-by-concept comparison. Where this spec's terminology does not yet exist in code, that is stated as a **missing concept**, not silently treated as already built. v1.2.2 introduces no new repository claims.

---

## 1. Purpose

Athena English exists to answer one question:

> What specific English-language weaknesses are preventing this student from earning marks, and what is the fastest evidence-based route to recovering those marks?

This document specifies the diagnostic and evidence architecture that makes that question answerable *auditably* — such that every claim Athena makes about a student is traceable to specific evidence, and every automated judgment (LLM-authored passage, LLM-marked response, LLM-classified error, LLM-selected next step) has a defined owner, a defined confidence representation, and a defined human checkpoint where the six-week campaign requires one.

v1.2 exists to close every place where v1.1 still allowed two competent, independently-implementing engineering teams to produce different marks, Evidence Claims, exposure states, confidence states, recovery states, root causes, or routing decisions for identical inputs. The governing test for every rule in this document is:

> **Could two competent independent engineering teams both follow this specification and still produce materially different student truth? If yes, the rule is not yet complete.**

And the final backstop, restated from the v1.2 task brief and binding on every section below:

> **Athena must never convert ambiguity in its own architecture into false certainty about a student.** Prefer the smallest deterministic, auditable mechanism that preserves evidence integrity over a sophisticated mechanism that cannot be trusted.

The immediate build target remains the Day 1 B1-aligned Reading Diagnostic. This spec defines the architecture generally enough that Days 2–7 and later Cambridge-specific diagnostics can reuse the same contract without re-litigating these questions (Section 33).

---

## 2. Non-Goals

Athena English v1.2 does **not**:

1. Certify an official CEFR level for any student. Athena produces B1-aligned / B2-aligned *diagnostic evidence*, never a `CEFR_CERTIFIED` claim (Section 29).
2. Predict a final Cambridge 0500 grade from CEFR-aligned diagnostic performance. No CEFR→grade conversion table is authorised anywhere in this system.
3. Produce a comprehensive English-proficiency diagnosis from a single sitting. One assessment produces one evidence point, not a stable trait estimate.
4. Treat AI-generated content (passages, questions, rubrics, marks, root causes, routing decisions) as ground truth without a defined review/approval path.
5. Build a general-purpose psychometrics platform. This is a six-week, two-student campaign. Human approval in the loop is a *feature*, not a gap to be automated away under time pressure.
6. Implement Day 1, write migrations, or touch application/frontend code. This document is the contract that implementation must satisfy.
7. Redesign the Mission (Mathematics recovery) workflow, the Chief Examiner handwriting-marking pipeline, or the `twin_snapshot` table's storage schema. v1.2 specifies how English diagnostics integrate with these *existing* systems without altering their current behaviour (Section 26).
8. Design or implement authentication/authorization. v1.2 specifies the security *boundary* English diagnostics require (Section 27) and states plainly, grounded in the actual repository, where that boundary does not yet exist (Section 34).

---

## 3. Terminology

Precise, non-conflatable definitions. Implementers must not substitute one term for another. New/changed terms since v1.1 are marked **[v1.2]**; new/changed terms since v1.2 are marked **[v1.2.1]**; new/changed terms since v1.2.1 are marked **[v1.2.2]**; new/changed terms since v1.2.2 are marked **[v1.2.3]**.

| Term | Definition |
|---|---|
| **B1-aligned** | An Athena-authored assessment designed against `ATHENA-B1-READING-PROFILE-v1` (Section 10) — CEFR used as a *design input*, not an authority Athena can certify against. |
| **Working instructional level** | The level Athena is *currently choosing to teach at*, based on evidence so far. Provisional, revisable, stored as the active `WORKING_INSTRUCTIONAL_LEVEL` Evidence Claim (Section 21). |
| **CEFR-certified level** | A level attested by an appropriate external, validated assessment. Athena never issues this claim itself. |
| **AUTHENTIC_CAMBRIDGE_0500** **[v1.2]** | Official Cambridge past-paper/source material (or officially licensed equivalent). The only `sourceType` eligible for Evidence Hierarchy Level 7 (Section 6, Section 30). |
| **ATHENA_CAMBRIDGE_STYLE** **[v1.2]** | Athena-authored, examination-like tasks designed to Cambridge 0500 spec but not sourced from official material. Never eligible for Level 7, regardless of resemblance to the real exam (Section 30). |
| **ATHENA_ORIGINAL** **[v1.2]** | Athena-authored diagnostic content not styled as a Cambridge examination task (this is what Day 1 uses). |
| **Performance** | The raw result a student achieved on a specific piece of evidence (e.g., 3/4 on inference questions in one sitting), expressed in **raw marks**, never percentage-only (Section 8, Section 23). |
| **Independent evidence unit** **[v1.2]** | One scored skill opportunity arising from one `SUBMITTED`, non-invalidated `AssessmentAttempt` on one passage, for one skill dimension (Section 22). Multiple questions on one skill within one passage/attempt collapse into exactly one unit. |
| **Marker confidence** **[v1.2, redefined]** | A deterministic derivation from structured marker signals (`evidenceSupport`, `rubricFit`, `alternativeMark`, `evidenceSpanValid`, `markerAgreement`) — never a raw LLM-reported decimal used operationally (Section 19). |
| **`advisoryModelConfidence`** **[v1.2]** | An optional raw numeric self-estimate a marking model may emit. Stored for audit only; never drives acceptance, routing, Evidence Confidence, or Digital Twin state (Section 19). |
| **Evidence confidence** | How strongly the *totality* of available evidence supports a broader claim about the student — a deterministic function of quantity, diversity, consistency, and marker confidence, computed **per skill dimension** (Section 22). One of exactly `UNKNOWN` / `LOW` / `MEDIUM` / `HIGH` — no other value (not `MODERATE`, not `LOW-MEDIUM`) is ever displayed or stored. |
| **Evidence Claim** | A first-class, auditable, immutable record of something Athena believes about a student, together with the evidence and confidence basis for believing it. Typed via `claimType` (Section 21). |
| **StudentAssessmentExposure** **[v1.2, new]** | The per-student record of whether, and how, a student has encountered a specific `AssessmentVersion`. Entirely separate from that version's global lifecycle (Section 14). |
| **AssessmentVersion** **[v1.2, renamed/clarified]** | The globally-versioned content of an assessment, which becomes immutable at `FROZEN`. Its lifecycle never encodes any individual student's exposure (Section 14). |
| **Transfer** | Successful application of a skill to material genuinely unseen by the student in that context, as distinct from a reattempt. Level derived deterministically from passage/genre/sitting axes (Section 6). |
| **Root cause** | A best-available explanation for a specific lost-mark event, typed by `failureType` (`CONSTRUCT` / `ACCESS` / `EXECUTION`) and `primaryCause`, always carrying a confidence basis and never asserted as certain in the absence of evidence (Section 20). |
| **REGRESSED** **[v1.2, new]** | A recovery state reached from `PROVISIONALLY_RECOVERED`, `VERIFIED_RECOVERED`, or `IMPROVING` on defined negative evidence, distinct from — and never silently rewriting — the prior recovered history (Section 24). Note the `IMPROVING → OBSERVED` transition is **not** `REGRESSED` (Section 24, **[v1.2.1]**). |
| **English Diagnostic Reviewer** **[v1.2, new role name]** | The human role that performs Section 13's mandatory human approval for English assessments. Deliberately **not** called "Chief Examiner" — see Section 26 for why that name is already taken by a different, existing Athena system. |
| **Defect claim status** **[v1.2.1]** | The `SKILL_DEFECT` Evidence Claim's own lifecycle field — `ACTIVE` / `CHALLENGED` / `SUPERSEDED` — distinct from, and never conflated with, `RecoveryCase` state (Section 21, Section 24, Section 25). |
| **CHALLENGED** **[v1.2.1]** | The defect-claim status entered when valid new evidence `CONTRADICTS` an `ACTIVE` `SKILL_DEFECT` claim without meeting the Section 21 supersession rule. The claim remains historically valid and visible; it is not deleted and does not become `SUPERSEDED` until supersession criteria are met (Section 21). |
| **MAJOR_SETBACK** **[v1.2.1]** | A flag recorded when an `IMPROVING` `RecoveryCase` reverts to `OBSERVED` (Section 24) and the triggering result is below 50%. Distinct from `MAJOR_REGRESSION`, which flags a sub-50% result triggering `REGRESSED` from `PROVISIONALLY_RECOVERED`/`VERIFIED_RECOVERED`. |
| **StudentAssessmentAssignment** **[v1.2.1]** | The server-verified record binding one student to one `AssessmentVersion` with an eligibility `status` and validity window. Both `AssessmentAttempt` creation and protected-content delivery require an `ACTIVE` record of this kind (Section 27, Section 28). |
| **`textNormalizationPolicyVersion`** **[v1.2.2]** | The versioned, shared Unicode-normalization/case-folding/whitespace pipeline (`ATHENA-TEXT-NORMALIZATION-v1`) that Section 17's objective matching and Section 18's copied-run/spelling subsystems both build on and never independently reinvent (Section 17). |
| **`spellingPolicyVersion`** **[v1.2.2]** | The versioned spelling-classification policy (`ATHENA-ENGLISH-SPELLING-v1`) — canonical en-GB lexicon plus frozen accepted-variant/proper-noun/contraction lists — governing the Section 18 spelling-and-punctuation criterion (Section 18). |
| **`spellingEngineVersion`** **[v1.2.3]** | The versioned deterministic dictionary-correction engine, algorithm, and configuration that the Step 3 `MISSPELLED` rule depends on — distinct from `spellingPolicyVersion` and `lexiconVersion`, and persisted alongside them (Section 18). |
| **`UNKNOWN_SPELLING_TOKEN`** **[v1.2.3, redefined]** | A token, exhaustively classified alongside `ACCEPTED`, `MISSPELLED`, and `IGNORED`, for which Step 2 finds no accepted source and Step 3's deterministic dictionary-correction rule cannot uniquely prove a misspelling. Never auto-counted as an error and never silently reinterpreted as `MISSPELLED`; forces `PENDING_HUMAN_MARK` only when it could still change an undetermined spelling mark (Section 18). |
| **`HUMAN_ACCEPTED` / `HUMAN_CONFIRMED_MISSPELLED`** **[v1.2.3]** | The two exhaustive, append-only human-resolution outcomes for one `UNKNOWN_SPELLING_TOKEN` occurrence. The original automatic `UNKNOWN_SPELLING_TOKEN` classification remains immutable; the resolution is a separate, persisted audit event (Section 18). |
| **Copied run** **[v1.2.3, redefined]** | Four or more consecutive normalized response tokens that exactly match four or more consecutive normalized passage tokens at one contiguous passage-token span, represented as a `CopiedSpanCandidate` in both response and passage coordinate systems — contiguity is required in *both* sequences, and maximality/merging is evaluated only along one shared `alignmentOffset`, never across crossing alignments (Section 18). |
| **`CopiedSpanCandidate` / `MAXIMAL_COPIED_SPAN`** **[v1.2.3]** | A candidate copied run represented as `{responseStart, passageStart, length}`; it is `MAXIMAL_COPIED_SPAN` only when neither left- nor right-extendable along its own `alignmentOffset = passageStart − responseStart` (Section 18). |

Marker confidence and evidence confidence are never used interchangeably in any UI, API response, or Digital Twin field. A field or display labelled "confidence" without qualifying which kind, or using any value outside the controlled vocabularies defined in this document, is a spec violation.

---

## 4. Governing Principles

Non-negotiable; every subsequent section must be checkable against these.

1. **Athena does not store what it believes about a student without also storing why it believes it.** Every skill estimate is backed by an Evidence Claim, never a bare number.
2. **No single LLM invocation authors and certifies its own output.** Generation, review, and approval are separate actors with separate responsibilities (Sections 11–13).
3. **CEFR-aligned evidence and Cambridge 0500 performance are related but never conflated**, and never bridged by an invented conversion formula.
4. **Confidence must be distinguished from performance.** A low score and a low-confidence estimate are different findings and must never share a single number.
5. **`UNKNOWN` is a valid, preferred answer** wherever evidence is insufficient — for root cause, for evidence confidence, for marking. Fabricated certainty is a defect, not a convenience.
6. **Reattempt is not transfer, and transfer is not retention.** These are three distinct evidence states and none substitutes for another (Section 6, Section 24).
7. **Historical evidence is immutable.** Once an assessment has produced student evidence, its content is frozen; corrections are versioned, not silent edits (Section 14).
8. **The student-facing system never leaks scoring machinery** — no answer keys, rubrics, marker prompts, or calibration metadata cross into a client-observable path (Section 27).
9. **Human approval is required before any assessment reaches students**, for the duration of the six-week campaign's small assessment bank (Section 13).
10. **Routing and classification decisions are deterministic given identical inputs and a fixed policy version** — an LLM is never handed an unconstrained "decide what happens next" prompt (Section 23).
11. **[v1.2] Student exposure to content and an assessment's global lifecycle are two separate concepts, tracked in two separate places, and neither is ever inferred from the other** (Section 14).
12. **[v1.2] The Digital Twin has exactly one source of truth: Evidence Claims.** Any materialised snapshot (including the existing `twin_snapshot` table) is a derived, rebuildable view, never an independent write target (Section 25).
13. **[v1.2] Every deterministic policy used to compute a claim, route, or confidence state is versioned, and historical records permanently retain the version that produced them** — a later policy revision never silently reinterprets history (Section 5).

---

## 5. Policy Version Registry

v1.1 deferred several policies "to implementation" (Section 30) and Codex correctly rejected that. v1.2 fixes every policy completely (Sections 10, 19, 22, 23, 24, 20, 17, 18) and registers each under its own version identifier so that policies can evolve independently without ambiguity about which one changed:

| Version field | Governs | Defined in |
|---|---|---|
| `routingPolicyVersion` | Day 1 raw-mark bands, gates, skill-floor override, tie-break ordering | Section 23 |
| `evidenceConfidencePolicyVersion` | Independent-unit definition, quantity/diversity/consistency bands, overall-confidence algorithm | Section 22 |
| `markerConfidencePolicyVersion` | Structured marker signals, HIGH/MEDIUM/LOW derivation, disagreement thresholds | Section 19 |
| `alignmentProfileVersion` | The B1 design profile itself (e.g. `ATHENA-B1-READING-PROFILE-v1`) | Section 10 |
| `evidenceHierarchyPolicyVersion` | Level derivation table, retention elapsed-time threshold, Level 7 source-type restriction | Section 6 |
| `summaryMarkingPolicyVersion` | Section E content-point model, paraphrase/copying rule, length handling, sentence-control checklist, spelling classification **[v1.2.2]** | Section 18 |
| `objectiveMarkingPolicyVersion` | Sections A–D question-type restrictions and scoring rules, under the shared text-normalization policy **[v1.2.2]** | Section 17 |
| `textNormalizationPolicyVersion` **[v1.2.2]** | The shared Unicode normalization form/version, case-folding, and whitespace pipeline underlying Sections 17 and 18 | Section 17 |
| `spellingPolicyVersion` **[v1.2.2]** | Section 18 spelling-error token classification: lexicon, accepted variants, proper nouns, contractions, unknown-token escalation | Section 18 |
| `spellingEngineVersion` **[v1.2.3]** | The deterministic dictionary-correction engine, algorithm, and configuration underlying the Section 18 Step 3 `MISSPELLED` rule | Section 18 |
| `rootCauseTaxonomyVersion` | `failureType`/`primaryCause`/`contributingCauses` enums and tie-break rule | Section 20 |

**Hard rule:** every persisted claim, mark, routing decision, and confidence state records every policy-version field it depended upon. A later bump to any one policy version never retroactively reinterprets a historical record — historical records are read back exactly as they were computed, under the version active at computation time (Governing Principle 13). All fields above start at value `v1.2.0`, are bumped to `v1.2.1` where that revision changed their governing rule (`routingPolicyVersion`, `evidenceConfidencePolicyVersion`, `summaryMarkingPolicyVersion`), and `alignmentProfileVersion` starts at `ATHENA-B1-READING-PROFILE-v1`. **[v1.2.2]** adds two new policy-version fields — `textNormalizationPolicyVersion` (starting at `ATHENA-TEXT-NORMALIZATION-v1`) and `spellingPolicyVersion` (starting at `ATHENA-ENGLISH-SPELLING-v1`, together with the frozen `lexiconVersion` and `assessmentAcceptedFormsVersion` identifiers it depends on) — and bumps `objectiveMarkingPolicyVersion` and `summaryMarkingPolicyVersion` to `v1.2.2` where this revision changes their governing rule (Sections 17 and 18). **[v1.2.3]** adds `spellingEngineVersion` (starting at a fixed identifier for the frozen deterministic dictionary-correction engine) and bumps `summaryMarkingPolicyVersion` to `v1.2.3`, since this revision again changes Section 18's governing rule (the spelling state machine and the copied-run alignment/selection algorithm); no other field changes version in this revision. `lexiconVersion`, `assessmentAcceptedFormsVersion`, and `spellingEngineVersion` must all be frozen to fixed identifiers before Day 1 becomes `ACTIVE`; no field in this registry is ever left as "current" or unversioned.

---

## 6. Evidence Hierarchy

v1.1's Level 3c/4/5 ordering overlapped ambiguously (Codex additional defect: "Level 3c, Level 4, and Level 5 overlap ambiguously... need explicit independent fields"). v1.2 replaces the single linear judgment call with three independent, recorded axes per independent evidence unit, from which the Level number is *derived*, never separately asserted:

* `samePassage` (bool) — same passage as the evidence unit being compared against.
* `sameGenreTopic` (bool) — same genre/topic tag as the comparison unit.
* `sittingOffset` — `SAME_SITTING` or `LATER_SITTING` (`LATER_SITTING` requires elapsed time ≥ the retention threshold below).

**Level derivation table** (evaluated top-to-bottom, first match wins; versioned as `evidenceHierarchyPolicyVersion`):

| Condition | Level | Meaning |
|---|---|---|
| same question re-served | **3a** | Same-question reattempt. This condition dominates source type because repetition is not independent evidence. |
| `sourceType = AUTHENTIC_CAMBRIDGE_0500`, new question/material | **7** | Authentic examination evidence. The source type permits Level 7; it does not make a repeated question independent. |
| `samePassage = true`, `LATER_SITTING` | **6** | Pure retention re-test of the same material after elapsed time — no novelty, decay check only. |
| new question, `samePassage = true`, `SAME_SITTING` | **3b** | Near-transfer. |
| `samePassage = false`, `sameGenreTopic = true`, `SAME_SITTING` | **3c** | Fresh-transfer, same sitting. |
| `samePassage = false`, `sameGenreTopic = true`, `LATER_SITTING` | **4** | Fresh-transfer confirmed after elapsed time. |
| `sameGenreTopic = false` | **5** | New genre/topic — strongest non-authentic confound reduction, regardless of sitting timing. |
| (unassessed / self-report) | **0–2** | Self-report (0), guided exercise (1), first unseen diagnostic (2) — unchanged from v1.1. |

**Retention elapsed-time threshold:** `LATER_SITTING` requires **≥ 72 hours** between the `submitted_at` of the comparison unit and the new unit. This is an explicitly labelled six-week intervention-policy threshold, not a psychometrically validated value, versioned under `evidenceHierarchyPolicyVersion`.

**Level 7 restriction (resolves Codex's "Cambridge evidence terminology" finding):** only `sourceType = AUTHENTIC_CAMBRIDGE_0500` material may occupy Level 7. `ATHENA_CAMBRIDGE_STYLE` material — however closely it resembles the real exam — is leveled by the same passage/genre/sitting table as any other Athena-authored content and is capped at Level 6. Every `Passage` and `Assessment` record carries `sourceType ∈ {ATHENA_ORIGINAL, ATHENA_CAMBRIDGE_STYLE, AUTHENTIC_CAMBRIDGE_0500}`; Day 1 content is always `ATHENA_ORIGINAL`.

A claim's evidence confidence must reflect *which levels* its supporting evidence occupies, not merely how many data points exist at Level 2 (Section 22). Twenty questions from one passage remain Level 2/3b evidence no matter how many there are.

For deterministic comparison, each unit stores canonical `passageId`, `genreTag`, `topicTag`, and `sittingId`. `samePassage` compares passage IDs; `sameGenreTopic` is true only when both the controlled genre and controlled topic tags match; `LATER_SITTING` compares submitted timestamps and the 72-hour rule. The first diagnostic unit is Level 2; transfer levels are assigned only when a comparison target is explicitly recorded.

---

## 7. Day 1 Diagnostic Objective and Blueprint

Day 1 answers one routing question:

> Can the student independently comprehend and respond to a straightforward Athena B1-aligned text sufficiently well to justify a B2 boundary probe, another independent B1 diagnostic, B1 remediation, or an A2/B1 boundary probe?

Day 1 does not certify B1, predict a Cambridge grade, establish a stable trait, or measure the full 0500 examination construct.

| Section | Skill | Maximum marks | Routing role |
|---|---|---:|---|
| A | Explicit comprehension | 8 | Included |
| B | Vocabulary in context | 6 | Included |
| C | Inference | 8 | Included |
| D | Main idea / purpose | 4 | Included |
| E | Short summary, target 80–100 words | 8 | Excluded from reading-level routing |
| **Total** | | **34** | |

The Day 1 reading denominator is permanently **26 marks (A–D)**. Section E is preliminary writing evidence only. Its mark, whether provisional or final, cannot alter the Day 1 reading route. A later summary mark creates or updates a writing claim; it does not retroactively alter a route that has already been persisted or an assignment that has started.

Target duration is 35–45 minutes, including reading time. A response may be marked pending or invalidated, but the system never substitutes zero, an average, or an imputed answer for missing evidence.

---

## 8. Passage and Assessment Requirements

Day 1 passages are `ATHENA_ORIGINAL`, approximately 500–750 words, adult-appropriate, coherent, and understandable without specialist knowledge. They must not reproduce or lightly adapt a copyrighted source. The authoring record stores the source declaration, content hash, authoring invocation, and contamination review.

Every assessment version stores:

* `sourceType`;
* `syllabusVersionId` when Cambridge alignment is claimed;
* `alignmentProfileVersion` when B1 alignment is claimed;
* passage, question, rubric, answer-key, and calibration revisions;
* review findings and their dispositions;
* an immutable content hash after freezing.

The student payload contains only the protected passage and question content required for the authorised attempt. It never contains the answer key, rubric, calibration record, hidden difficulty metadata, examiner notes, or marking prompts.

---

## 9. Assessment Authoring Pipeline

The pipeline is:

```text
AUTHORING
  → DETERMINISTIC_CHECKED
  → AI_REVIEWED
  → HUMAN_APPROVED
  → FROZEN
  → ACTIVE
```

The Authoring Agent, deterministic analysis, Review Agent, and human reviewer are separate actors. The Review Agent receives the candidate artefact and computed statistics, but not the Authoring Agent's hidden prompt or chain of thought. The author and reviewer calls must have distinct invocation IDs and must not share a model session.

Every field revision is append-only. `FieldRevision` records field path, value or value hash, provenance, actor/model, prompt version, timestamp, and reason. The current field value is the latest valid revision; a single mutable provenance tag is never used as the audit history.

The four provenance classes are `AUTHORED`, `COMPUTED`, `AI_REVIEWED`, and `HUMAN_APPROVED`. A final human value carries `HUMAN_APPROVED` and retains links to all prior AI and authored values.

---

## 10. ATHENA-B1-READING-PROFILE-v1

`ATHENA-B1-READING-PROFILE-v1` is the normative internal design anchor. It references CEFR B1 descriptors as provenance, but is not an official CEFR examination or certification instrument.

The reviewer assigns each dimension an integer rating:

```text
0 = below the intended B1 design range
1 = borderline / requires mitigation
2 = within the intended B1 design range
3 = above the intended B1 range but still usable for a boundary probe
```

The profile requires a written rationale and evidence location for every rating:

| Dimension | Target rule |
|---|---|
| Vocabulary | Predominantly frequent everyday, work, study, and public-life vocabulary; unfamiliar content words are limited and inferable from context. |
| Grammar | Mostly simple and coordinated structures with some subordinate clauses; no sustained dense embedding or specialist syntax. |
| Sentence complexity | Sentence length and clause structure vary, but meaning remains recoverable without parsing specialist prose. |
| Discourse organisation | Clear paragraphing and explicit links between ideas; the reader can follow the progression without specialist schema. |
| Topic familiarity | Adult-interest topic, requiring no specialist knowledge; cultural assumptions are disclosed or avoided. |
| Information density | Key information is distributed clearly enough for explicit retrieval; distractor detail does not dominate the construct. |
| Inferential demand | Local, text-grounded inferences are required; no answer depends on an unstated specialist assumption. |
| Abstractness | Concrete and moderately abstract ideas may occur, but the task does not depend on sustained theoretical abstraction. |
| Rhetorical complexity | Purpose, attitude, or implication is signalled through ordinary discourse choices, not advanced rhetorical analysis. |
| Question complexity | Questions measure the declared skill and use language no more demanding than the passage unless that demand is the declared construct. |

Alignment is eligible only when every dimension is rated 2 or 3, no dimension is 0, and any dimension rated 1 has a recorded mitigation and explicit human acceptance. Two reviewers must independently rate the profile. A difference greater than one point on any dimension, or any disagreement about a critical failure, blocks approval until the English Diagnostic Reviewer adjudicates and records the final rationale.

Readability and lexical statistics are supplementary evidence only and cannot set or override alignment.

---

## 11. Deterministic Calibration

The non-LLM calibration pass computes word count, sentence statistics, lexical statistics against the versioned wordlist, readability indices, paragraph structure, question-reference integrity, duplicate-answer checks, and answerability flags. It never decides B1 alignment.

Any failed hard check blocks progression. A warning is retained and must be accepted or corrected by the English Diagnostic Reviewer. A hard check cannot be downgraded to a warning without a recorded human amendment and rationale.

---

## 12. Independent AI Review

The Review Agent must review construct relevance, B1 profile ratings, vocabulary, grammar, inferential load, ambiguity, answer-key correctness, rubric completeness, accidental clues, cultural bias, contamination risk, and question-to-skill mapping.

Each finding is one of:

```text
BLOCKING   // assessment cannot be approved
WARNING    // may proceed only with explicit human acceptance
ADVISORY   // retained for audit; does not block
```

The Review Agent cannot approve an assessment or change lifecycle state.

---

## 13. Human Approval and Loopbacks

`HUMAN_APPROVED` certifies only that the English Diagnostic Reviewer has inspected the candidate content that will be frozen, calibration, AI review, provenance history, profile ratings, answer key, rubric, security classification, and all findings; that every blocking finding is resolved; and that every warning is explicitly accepted or corrected. It does not certify student ability or statistical validity.

The reviewer may accept, amend, reject, or return the candidate. A substantive amendment to passage, question, answer key, rubric, profile rating, difficulty, source classification, or scoring rule invalidates the prior AI review for that field and moves the candidate to `REVISION_REQUIRED`. Deterministic checks and independent AI review must run again before human approval. A minor administrative amendment must be separately classified as non-substantive and still receives an audit record.

Rejection moves the candidate to `REJECTED`; it cannot become active without a new revision and a new approval event.

---

## 14. Assessment Version Lifecycle and Student Exposure

Global lifecycle:

```text
DRAFT → AUTOMATED_CHECKED → AI_REVIEWED → HUMAN_APPROVED
      → FROZEN → ACTIVE → GLOBALLY_RETIRED
```

`REVISION_REQUIRED` and `REJECTED` are terminal branches from any pre-freeze review state. A substantive correction after freezing creates a new `AssessmentVersion`; the old version and all historical attempts remain unchanged.

`AssessmentVersion` has only global lifecycle state. Per-student exposure is stored in `StudentAssessmentExposure`:

```text
UNSEEN → EXPOSED → IN_PROGRESS → SUBMITTED
                         └──────→ ABANDONED
                         └──────→ INVALIDATED
```

`ASSIGNED` may be stored as scheduling metadata, but is not an exposure state. Assignment, scheduling, attempt creation, and an unsuccessful request that delivered no protected content do not end unseen status.

The first successful server response that delivers any protected passage or question content to that student's authenticated, authorised attempt changes the exposure to `EXPOSED` and records `firstExposedAt`, request ID, session ID, and content hash. Rendering is not required. A browser crash, network loss after delivery, abandonment, invalidation, or restart never restores `UNSEEN`. A resumable attempt resumes the same exposure; a repeat attempt creates another attempt under the same exposed record. Invalidating a mark invalidates evidence, not exposure.

Teacher/admin preview uses a separately authorised preview path and does not change a student's exposure. If protected content is accidentally delivered to a student identity, it counts as exposure and is audited.

The minimum attempt fields are student identity, assessment version, exposure ID, attempt status, created/exposed/submitted timestamps, resume token, invalidation reason, and submission content hash. Gerald and Melusi therefore may share one globally `ACTIVE` version while having independent exposure records.

---

## 15. Question Taxonomy

Every question has a stable ID, section, maximum marks, primary skill, answer format, expected evidence, answer key or rubric reference, profile/version provenance, and review status.

Day 1 objective questions must use one of `SINGLE_SELECT`, `MULTI_SELECT_EXACT`, or `SHORT_TEXT_CANONICAL`. Open-ended fuzzy objective marking is not authorised. Any question requiring semantic judgement belongs in the summary rubric and is not an objective question.

**[v1.2.1, MARK-001]** `NUMERIC` is not an authorised Day 1 answer format. v1.2 permitted it without a defined numeric-equivalence contract — `accepted = 1` versus `student = 1.0` was left undecided, so two compliant markers could disagree. Day 1's construct — reading comprehension, vocabulary in context, inference, main idea, and summary — has no genuine need for a numeric response item, so the smallest deterministic fix is removal rather than specification. A future day that genuinely requires numeric responses must add `NUMERIC` back only together with a complete deterministic contract before it may be authorised for that day's assessments, per Section 33's reuse-only constraint:

* trim leading/trailing whitespace; reject an empty value; reject thousands separators unless explicitly permitted by the frozen answer rule; parse using `.` as decimal separator; permit optional leading `+`/`-`; permit leading zeros; reject scientific notation unless explicitly enabled by the question; reject trailing or leading units unless the question's frozen answer rule explicitly defines a unit; compare the parsed numeric value, not the submitted string;
* each frozen numeric question declares exactly one of `EXACT_VALUE`, `ABSOLUTE_TOLERANCE` (with a stated tolerance), or `DECIMAL_PLACES` (with a stated precision) — no implicit tolerance is ever invented;
* if units are construct-relevant, `answerFormat = NUMERIC_WITH_UNIT` with an explicit frozen unit rule; if units are not construct-relevant, the UI requests numeric input only.

---

## 16. Marking States and Immutable Marks

Marks are immutable events bound to an attempt, assessment version, rubric version, marker result, and marking policy versions. A correction creates a superseding mark event; it never edits the historical event. A score is not posted as final while required human resolution is pending.

Objective scoring is server-side and deterministic. Summary scoring uses the Section 18 rubric and Section 19 uncertainty policy. A failed model call produces `PENDING_HUMAN_MARK`, never zero.

---

## 17. Objective Marking Contract

### Shared text-normalization policy [v1.2.2, MARK-002]

This resolves Codex's MARK-002 finding: v1.2.1 said objective matching should "Unicode-normalize" without specifying the normalization form, so one implementation could apply NFC and another NFKC and silently diverge on compatibility characters (e.g. full-width forms) — producing different objective marks for the same submission from two compliant implementations. v1.2.2 defines one versioned, shared base pipeline, `ATHENA-TEXT-NORMALIZATION-v1` (`textNormalizationPolicyVersion`), that this section and Section 18 both build on rather than each inventing independent Unicode handling:

```text
1. Apply Unicode Normalization Form NFKC (never NFC), under a frozen
   UnicodeVersion = 15.1.
2. Apply Unicode Default Case Folding (not a bare "lower-case" operation).
3. Trim leading/trailing Unicode whitespace.
4. Collapse internal whitespace runs to one ASCII space.
```

NFKC is used, not NFC, because it folds compatibility variants — full-width characters, presentation forms — into their canonical equivalents, which both objective short-text matching and the Section 18 copied-run/spelling constructs require (Scenario S: full-width `Ａthena`, `ATHENA`, and `Athena` normalize identically). `UnicodeVersion` is fixed at `15.1` (or another version actually available in the target runtime, frozen before Day 1 becomes `ACTIVE`) so that library or language differences across implementations cannot produce different fold results; it is never left as "current Unicode."

Objective matching applies this base pipeline, then:

```text
5. Apply the question's frozen punctuation rule (punctuation is ignored
   only when that rule declares punctuation non-construct-relevant),
   applied after steps 1–4, never before.
6. Compare case-folded (step 2) unless the frozen answer rule explicitly
   marks case as construct-relevant, in which case step 2 is skipped for
   that question and the pre-fold string is compared instead.
```

### Objective scoring rules

Blank answers score zero. A single-select response scores one mark only for the frozen correct option. A multi-select response receives credit only when the selected set exactly equals the frozen accepted set; partial credit is not used unless the question explicitly contains independently scored subparts. Short-text responses match a frozen set of accepted normalized answers. Spelling variants are accepted only when listed in the frozen answer set or when the question explicitly declares spelling non-construct-relevant. No LLM decides equivalence for objective items.

An answer format or scoring rule cannot be changed after freezing. Ambiguous or missing answer-key entries block approval.

Every objective scoring event records `textNormalizationPolicyVersion`, `UnicodeVersion`, and `objectiveMarkingPolicyVersion`. A later bump to any of these never silently re-normalizes or re-scores a historical mark; it is read back exactly as computed at marking time (Governing Principle 13).

---

## 18. Section E Summary Marking Contract

The summary is marked out of 8:

| Criterion | Marks | Rule |
|---|---:|---|
| Required content points | 4 | Four frozen propositions, each independently 0/1. A point is credited when its proposition is accurate and present, whether expressed in the student's own words or copied. |
| Paraphrase **[v1.2.3]** | 2 | 2 marks when `copiedRunCount = 0`; 1 mark when `copiedRunCount = 1`; 0 marks when `copiedRunCount ≥ 2` — see below. |
| Sentence control **[v1.2.1]** | 1 | Awarded only when all four conditions below are met; otherwise 0, unless the ambiguity rule below applies. |
| Spelling and punctuation **[v1.2.3]** | 1 | ≤2 `confirmedMisspellingCount` occurrences AND sentence boundaries are mechanically identifiable — see below. |

All Section 18 subsystems build on the shared `ATHENA-TEXT-NORMALIZATION-v1` base pipeline (Section 17: NFKC, Unicode default case folding, whitespace normalization) and layer their own additional transformations on top of it, so that copied-run tokenization, spelling lookup, and objective matching can never independently diverge on Unicode handling:

```text
Base (Section 17):        NFKC + Unicode case folding + whitespace normalization

Objective short-text:     + optional punctuation handling per frozen answer rule
Copied-run (below):       + punctuation removal + word tokenization
Spelling (below):         + word tokenization + lexicon lookup
```

### Copied-run candidates, alignment, and deterministic counting [v1.2.3, SUM-003]

This resolves Codex's re-raised SUM-003 finding against v1.2.2's own language. v1.2.2 correctly closed v1.2.1's response-only-versus-both-sequences ambiguity by requiring `response[i:i+n] == passage[j:j+n]` contiguity in both sequences. But v1.2.2 retained one further, unqualified sentence — "two candidate matching spans that overlap are merged into the maximal span" — that did not require the overlapping candidates to share the same response/passage alignment. Two candidates can overlap in response coordinates while their matched passage tokens sit on unrelated or crossing diagonals; unioning them does not, in general, describe a substring that is itself a valid contiguous match, so that merge instruction could still produce two different, individually compliant results for the same input (Codex's crossing-alignment counterexample, worked below). v1.2.3 withdraws the unqualified merge instruction and replaces it with an explicit two-coordinate candidate model, an alignment-restricted maximality rule, and one deterministic selection algorithm for resolving overlapping candidates that lie on different alignments.

Tokenization is unchanged from v1.2.2: the shared `ATHENA-TEXT-NORMALIZATION-v1` base pipeline (NFKC, Unicode default case folding, whitespace collapse — Section 17), then, in this fixed order, punctuation removal, then word tokenization. An apostrophe within a contraction or possessive is retained as part of the token, not treated as removable punctuation; a hyphenated form tokenizes as one token, consistent with the word-count rule below. Function words are included; repeated common words alone do not form a run unless the full four-token sequence matches contiguously in both sequences. This algorithm is applied to the submitted response and the frozen passage.

**Candidate span.** A copied-run candidate is exactly:

```text
CopiedSpanCandidate {
    responseStart
    passageStart
    length
}
```

where `length >= 4` and `responseTokens[responseStart : responseStart+length] == passageTokens[passageStart : passageStart+length]`, both ranges contiguous with no gaps in either sequence. A sequence that is consecutive in the response but reconstructed from non-contiguous passage tokens is **not** a candidate (Scenario Q). A sequence contiguous in both is a candidate (Scenario R).

**Alignment offset.** Every candidate carries `alignmentOffset = passageStart - responseStart`. Two candidates lie on the same alignment only when their `alignmentOffset` values are equal.

**Maximality is evaluated only along one alignment.** A candidate is **left-extendable** when `responseStart > 0 AND passageStart > 0 AND responseTokens[responseStart-1] == passageTokens[passageStart-1]`. A candidate is **right-extendable** when `responseStart+length < responseTokenCount AND passageStart+length < passageTokenCount AND responseTokens[responseStart+length] == passageTokens[passageStart+length]`. A candidate is a `MAXIMAL_COPIED_SPAN` only when it is neither left- nor right-extendable **along its own `alignmentOffset`**. This eliminates internal 4/5/6-token subspans of one longer aligned run (Scenario R) without ever reaching across to a different alignment to extend a span.

**Crossing alignments are never merged.** v1.2.2's unqualified "overlapping candidates are merged into the maximal span" is withdrawn in that form. Candidates may be merged or treated as one extended run only when they share the same `alignmentOffset` and form one contiguous matching span; candidates with different `alignmentOffset` values are distinct candidate alignments even where their response-token ranges overlap, and are never unioned into a span that is not itself directly verified as `responseTokens[i:i+n] == passageTokens[j:j+n]` for some single `n`.

**Deterministic selection among overlapping alignments.** Generate every `MAXIMAL_COPIED_SPAN` of length ≥4, each recording `responseStart`, `responseEndExclusive`, `passageStart`, `passageEndExclusive`, `length`, and `alignmentOffset`. Sort candidates by (1) `responseStart` ascending, (2) `length` descending, (3) `passageStart` ascending. Select greedily in that order: a candidate is selected only if its response-token interval does not overlap the response-token interval of any already-selected span, where overlap means exactly `candidate.responseStart < selected.responseEndExclusive AND selected.responseStart < candidate.responseEndExclusive` — adjacent spans (e.g. `[0,4)` and `[4,8)`) are not overlapping and may both be selected (Scenario Y), while `[0,5)` and `[4,8)` do overlap and cannot both be selected. Passage-coordinate overlap alone never disqualifies a candidate — only response-coordinate overlap does. Where two candidates share the same `responseStart`, the longer span wins; if lengths also tie, the lower `passageStart` wins (Scenario X).

**Result.** `copiedRunSpans[]` contains exactly the selected spans; `copiedRunCount` is the number of selected spans; `maxCopiedRunLength` is the greatest selected length. All generated candidates may additionally be retained for audit, but only selected spans affect the paraphrase mark. A 7-token contiguous match is one copied run of length 7; its internal 4-, 5-, and 6-token subspans on the same alignment are not separately counted.

**Worked counterexample (Codex's crossing-alignment example, Scenario W).** `Response = a b a b c d`; `Passage = a b c d a b a b`. Enumerating every length-≥4 maximal candidate yields exactly two: `(responseStart=0, passageStart=4, length=4)`, `alignmentOffset = 4`; and `(responseStart=2, passageStart=0, length=4)`, `alignmentOffset = -2`. Sorted by `responseStart` ascending, `(0,4,4)` is considered first and selected (response interval `[0,4)`). `(2,0,4)` has response interval `[2,6)`, which overlaps the already-selected `[0,4)` (`2<4` and `0<6`), so it is rejected — its passage overlap with the first candidate is irrelevant; the disqualification is purely on response coordinates. Final result: `copiedRunSpans = [(responseStart=0, passageStart=4, length=4)]`, `copiedRunCount = 1`, `maxCopiedRunLength = 4`, paraphrase mark = **1**. This is the unique compliant outcome under the algorithm above.

**Stored result (minimum, unchanged from v1.2.2):** `copiedRunCount`, `maxCopiedRunLength`, and `copiedRunSpans[]`, where each span records response token offsets, passage token offsets, and the matched token sequence (or its hash) — sufficient to audit the decision without re-running the algorithm.

Word count is whitespace-delimited tokens after trimming; punctuation attached to a token does not create another token, and a hyphenated form counts as one token. `IN_TARGET` is 80–100 words, `UNDER_TARGET` is below 80, and `OVER_TARGET` is above 100. No automatic marks are deducted solely for being under or over target. Counts below 60 or above 120 set `lengthWarning = true` and require human confirmation of content completeness, but the rubric marks remain criterion-based.

### Sentence-control mark [v1.2.1]

This resolves Codex's SUM-001 finding: v1.2 used subjective language ("complete," "grammatically interpretable," "clearly separates sentences") that let two markers award different marks for the same response. The 1 sentence-control mark is awarded only when all of the following are true:

1. the response contains at least two sentence units;
2. every sentence unit contains an identifiable subject/predicate, or is an accepted imperative construction;
3. no more than two sentence-level grammar faults (defined below) are recorded across the response;
4. punctuation allows sentence boundaries to be identified without marker inference.

A **sentence-level grammar fault**, narrowly, is one of:

* a missing finite verb;
* an unrecoverable subject/verb agreement error;
* a fused sentence/run-on that obscures the sentence boundary;
* a fragment that is not functioning as a deliberate, valid construction;
* a tense/form error that makes the proposition grammatically incomplete.

Minor grammatical awkwardness that does not fall into one of these five categories does not count as a fault and cannot reduce the mark.

**Ambiguity rule:** if a marker cannot deterministically decide whether a candidate fault meets one of the five enumerated categories, or cannot deterministically decide whether condition 4 is met, the sentence-control criterion is recorded as `PENDING_HUMAN_MARK`. The marking model must not choose arbitrarily between awarding and withholding the mark.

### Spelling and punctuation mark [v1.2.3, SUM-002]

This resolves Codex's re-raised SUM-002 finding against v1.2.2's own language. v1.2.2's five-step classification order correctly narrowed every token to either an accepted/exempt outcome or `UNKNOWN_SPELLING_TOKEN`, but step 5's "otherwise → `UNKNOWN_SPELLING_TOKEN`" never defined how a token could instead become `MISSPELLED` — while the mark rule separately assumed `MISSPELLED` occurrences existed to count. Two compliant implementations could both claim compliance while producing opposite outcomes for the same three non-accepted, non-exempt tokens: one escalating all three to `PENDING_HUMAN_MARK`, the other silently treating all three as `MISSPELLED` and scoring `spellingErrors = 3`. v1.2.3 closes this by making classification exhaustive under four states and defining the missing deterministic `MISSPELLED` path.

**Spelling policy version.** Every mark using this criterion records:

```text
spellingPolicyVersion = ATHENA-ENGLISH-SPELLING-v1
lexiconVersion
assessmentAcceptedFormsVersion
spellingEngineVersion
```

`ATHENA-ENGLISH-SPELLING-v1` fixes:

```text
language = en-GB
lexiconVersion = <fixed version identifier, frozen before Day 1 ACTIVE>
```

Day 1 uses British English (`en-GB`) as the canonical spelling baseline. This specification does not need to embed the lexicon itself, but the exact frozen asset and version an implementation loads must be fixed and recorded before the assessment leaves `HUMAN_APPROVED` and before Day 1 becomes `ACTIVE`; a spelling mark is never computed against an unversioned or "current" dictionary or spelling engine.

**Exhaustive classification.** Every token occurrence resolves to exactly one of `ACCEPTED`, `MISSPELLED`, `UNKNOWN_SPELLING_TOKEN`, or `IGNORED`. No token is left unclassified and no token holds two of these states at once. Classification uses the following fixed order; the first matching rule wins.

1. **`IGNORED`.** A token with no lexical word material under the frozen tokenizer — a punctuation-only token, or a formatting artefact the tokenizer excludes — is `IGNORED`. `IGNORED` never affects the spelling-error count.
2. **`ACCEPTED`.** A normalized token present in any frozen accepted source applicable to the assessment is `ACCEPTED`:
   * the canonical spelling lexicon;
   * the frozen accepted-variant list — e.g. `color`/`colour`, `organize`/`organise`, `center`/`centre` (Scenario P), never a marker judgement call about what "looks like" a valid British/American pair;
   * the assessment-specific proper-noun list, including a proper noun's occurrence at the same normalized spelling in the assessment passage;
   * the frozen assessment-specific accepted-form list, covering accepted contractions (e.g. `don't`, `can't`, `it's`, `they're`) and any other assessment-specific exemption explicitly approved in the frozen summary-marking asset.
   Every lookup source and its version is frozen before the assessment becomes `ACTIVE`. No LLM or marker may create a new exemption during automatic marking.
3. **`MISSPELLED` — deterministic path (new in v1.2.3).** A token not `ACCEPTED` under step 2 is classified `MISSPELLED` only when the frozen spelling engine returns **exactly one** recognised dictionary correction candidate at edit distance 1 from the submitted token (e.g. `becaus` → exactly one distance-1 candidate `because` → `MISSPELLED`). If the engine returns zero candidates or more than one candidate, this step does not match and classification falls through to step 4 — the engine never guesses among several plausible corrections and never asserts a misspelling it cannot uniquely resolve. The spelling engine, its algorithm, its lexicon, and its configuration are versioned under `spellingEngineVersion`. Model judgement never substitutes for this rule.
4. **`UNKNOWN_SPELLING_TOKEN`.** A token that is not `ACCEPTED` and for which step 3 cannot deterministically prove a misspelling is `UNKNOWN_SPELLING_TOKEN` — an unfamiliar surname, niche terminology, a valid word absent from the frozen lexicon, a token with several plausible distance-1 corrections, or a malformed token with no unique correction. It means Athena cannot deterministically decide whether the token is valid or misspelled — it does **not** mean misspelled, is never automatically treated as an error, and is never silently reinterpreted as `MISSPELLED` by any implementation.

**Decision table:**

| Automatic state | Counts as spelling error? | Can auto-score? |
|---|---:|---|
| `IGNORED` | No | Yes |
| `ACCEPTED` | No | Yes |
| `MISSPELLED` | Yes | Yes |
| `UNKNOWN_SPELLING_TOKEN` | Not yet known | Only if the final mark is already mathematically determined |
| `HUMAN_ACCEPTED` | No | Human-resolved |
| `HUMAN_CONFIRMED_MISSPELLED` | Yes | Human-resolved |

**Human resolution of `UNKNOWN_SPELLING_TOKEN`.** When an `UNKNOWN_SPELLING_TOKEN` occurrence requires resolution, a human reviewer classifies that occurrence as exactly one of `HUMAN_ACCEPTED` or `HUMAN_CONFIRMED_MISSPELLED`. The system persists `tokenOccurrenceId`, `submittedToken`, `normalizedToken`, `initialClassification = UNKNOWN_SPELLING_TOKEN`, `humanResolution`, `reviewerId`, `resolvedAt`, `reason`, `spellingPolicyVersion`, `lexiconVersion`, and `spellingEngineVersion`. The original automatic classification remains immutable (Governing Principle 7); the human resolution is a separate, appended audit event, never an edit to the automatic record.

**When unknown tokens require human marking.** Not every unknown token forces escalation — only unknowns that could still change an undetermined mark. Let `confirmedMisspellingCount` be the count of `MISSPELLED` plus `HUMAN_CONFIRMED_MISSPELLED` occurrences, and `unknownCount` be the count of unresolved `UNKNOWN_SPELLING_TOKEN` occurrences.

```text
Case A: confirmedMisspellingCount >= 3
        → spelling mark = 0. No unknown token can change this result;
          unresolved unknowns are retained for audit but do not force
          PENDING_HUMAN_MARK (Scenario V).

Case B: confirmedMisspellingCount <= 2 AND unknownCount = 0
        → spelling mark = 1 (deterministic).

Case C: confirmedMisspellingCount <= 2 AND unknownCount > 0
        → spelling criterion = PENDING_HUMAN_MARK, because treating every
          unknown as accepted could yield mark = 1 while treating enough
          unknowns as misspelled could yield mark = 0 (Scenario U). A human
          resolves only as many unknown occurrences as are required to make
          the mark mathematically determined; once determined, remaining
          unresolved unknowns may stay UNKNOWN_SPELLING_TOKEN without
          blocking the criterion.
```

This replaces v1.2.2's single "any unresolved `UNKNOWN_SPELLING_TOKEN` affecting the threshold → `PENDING_HUMAN_MARK`" sentence with the exact case analysis above, so escalation happens exactly when needed and never when the mark is already mathematically fixed.

**Error counting.** Only tokens deterministically classified as `MISSPELLED`, or human-resolved as `HUMAN_CONFIRMED_MISSPELLED`, count toward `confirmedMisspellingCount`. Each occurrence counts separately — repeated occurrences of the same surface form are not collapsed. Three occurrences of one misspelled token, each deterministically or human-confirmed misspelled, produce `confirmedMisspellingCount = 3` (Scenario T), unless the frozen lexicon/list resolves the token as `ACCEPTED`.

No implementation may reinterpret `UNKNOWN_SPELLING_TOKEN` as `MISSPELLED` without a persisted human-resolution event, and no implementation may leave a non-accepted, non-exempt token unclassified.

This mark shares the sentence-boundary gate (condition 4 of the sentence-control checklist above) with the sentence-control criterion exactly as in v1.2.1: the two criteria are awarded independently, but the punctuation-identifiability judgement is a single binary gate, evaluated once and read by both — a punctuation deficiency that fails condition 4 affects both marks through that one shared evaluation, never through two independently-judged punctuation assessments. A spelling error is never itself recorded as a sentence-level grammar fault unless it independently causes one of the five enumerated fault categories (e.g. a spelling error that deletes a finite verb).

A response with no text scores 0 and is recorded as `SUBMITTED_NO_RESPONSE`. A marker must cite spans for every content point and language decision. If a criterion cannot be resolved from the rubric, the criterion becomes `PENDING_HUMAN_MARK`; no model retry may invent a score. Section E never participates in reading routing. When final, it may create only `PRELIMINARY_WRITING_EVIDENCE` with LOW evidence confidence for Day 1.

Every Section E mark records `textNormalizationPolicyVersion`, `spellingPolicyVersion`, `lexiconVersion`, `assessmentAcceptedFormsVersion`, `spellingEngineVersion`, and `summaryMarkingPolicyVersion`. Historical marks retain the versions used at marking time; a later policy or version bump never silently reinterprets a historical mark (Governing Principle 13).

---

## 19. LLM Marker Uncertainty

The structured marker result contains criterion, awarded marks, maximum marks, cited student spans, rationale, `evidenceSupport`, `rubricFit`, `alternativeMark`, `evidenceSpanValid`, `markerAgreement`, model/prompt/rubric versions, and review state. The controlled signal values are `evidenceSupport = COMPLETE | PARTIAL | WEAK`, `rubricFit = CLEAR | AMBIGUOUS`, `alternativeMark = NONE | PLAUSIBLE`, `evidenceSpanValid = TRUE | FALSE`, and `markerAgreement = AGREED | DISAGREED`. The optional numeric `advisoryModelConfidence` is audit-only.

Derived marker confidence is exactly:

```text
HIGH   = COMPLETE + CLEAR + NONE + TRUE + AGREED
LOW    = WEAK, FALSE, DISAGREED, or unresolved ambiguity
MEDIUM = every other valid result
UNKNOWN = no valid marker result exists
```

If two markers award different marks for any criterion, `markerAgreement = DISAGREED` and human resolution is required. If scores agree but evidence or rubric signals disagree, the result is at most MEDIUM until human review. Unsupported spans, malformed output, timeout, or invocation failure produce `PENDING_HUMAN_MARK`. A repeated unsupported-span failure escalates directly to human marking.

Historical raw results remain stored with all model and policy versions. No numeric self-estimate can make a mark accepted or raise Evidence Confidence.

---

## 20. Root-Cause Classification

Root causes use two typed fields rather than one mixed vocabulary:

```text
failureType: CONSTRUCT | ACCESS | EXECUTION
primaryCause: controlled enum | UNKNOWN
contributingCauses: controlled enum[]
```

Allowed causes are:

* `CONSTRUCT`: `EXPLICIT_COMPREHENSION`, `MAIN_IDEA_PURPOSE`, `INFERENCE`, `SUMMARY_SELECTION`;
* `ACCESS`: `VOCABULARY_ACCESS`, `MISREAD`, `LANGUAGE_INTERPRETATION`;
* `EXECUTION`: `CARELESS_EXECUTION`, `TIME_PRESSURE`, `RESPONSE_FORMAT`, `BLANK`.

`VOCABULARY_ACCESS` is a cause; `Vocabulary in Context` is a skill. They must not be substituted for one another.

The classifier applies these rules in order:

1. A blank response is `EXECUTION / BLANK`.
2. A correct answer changed to an incorrect answer with recorded time pressure is `EXECUTION / TIME_PRESSURE`.
3. A response or answer-change record explicitly showing an unknown key word, followed by success after that word is explained, is `ACCESS / VOCABULARY_ACCESS`.
4. A wrong passage location or unsupported textual detail with no vocabulary signal is `ACCESS / MISREAD`.
5. Text that identifies relevant details but fails the required relationship is `CONSTRUCT / INFERENCE`.
6. A response that identifies the source material but selects irrelevant information for a summary is `CONSTRUCT / SUMMARY_SELECTION`.
7. If two candidate causes remain supported after these rules and neither has a uniquely stronger direct signal, `primaryCause = UNKNOWN` and both are contributing causes in enum order.

A cause assignment must cite the direct signal that triggered the rule. Absence of evidence cannot establish a cause. The same key-word example is therefore `VOCABULARY_ACCESS` primary only when the key-word signal is recorded; otherwise it is `UNKNOWN` with `VOCABULARY_ACCESS` and `INFERENCE` as contributing hypotheses.

---

## 21. Evidence Claims

`claimType` is one of:

```text
SKILL_PERFORMANCE
SKILL_DEFECT
ROOT_CAUSE
RECOVERY
WORKING_INSTRUCTIONAL_LEVEL
ROUTING_RECOMMENDATION
PRELIMINARY_WRITING_EVIDENCE
```

Every claim has student ID, claim type, skill/defect key, structured predicate, evidence references, raw performance, hierarchy levels, all policy versions used, confidence inputs, overall confidence, status, creation/update timestamps, and append-only relation records. Free text may explain a claim but cannot replace its controlled predicate or type.

The Digital Twin cannot independently overwrite or invent a claim. It is derived from the active claim set (Section 25).

Relations are deterministic:

* `SUPPORTS`: new valid evidence satisfies the existing claim predicate at the same or higher evidence level.
* `WEAKENS`: new evidence opposes the predicate but is below the claim's evidence level or consists of only one insufficient independent unit.
* `CONTRADICTS`: new evidence opposes the predicate, is valid, and is at the same or higher evidence level.
* `SUPERSEDES`: a new claim is built from at least two independent units, includes Level 5+ or Level 7 evidence, has confidence no lower than the prior active claim, and has no unresolved same-level contradiction. The old claim remains immutable but is no longer active.

Relation precedence for one new evidence event is `SUPERSEDES`, then `CONTRADICTS`, then `WEAKENS`, then `SUPPORTS`; only the highest applicable relation is recorded. A `CONTRADICTS` relation against an `ACTIVE` claim does not delete or silently deactivate that claim; it drives the claim-status transition defined below (CLAIM-001).

### SKILL_DEFECT claim status [v1.2.1]

This resolves Codex's CLAIM-001 finding: v1.2 allowed a successful fresh-transfer result to `CONTRADICT` an `ACTIVE` `SKILL_DEFECT` claim without defining what happens to that claim before full supersession — one compliant implementation could keep the defect active, another could deactivate it. A `SKILL_DEFECT` claim carries its own `status` field, independent of and never conflated with `RecoveryCase` state (Section 24):

```text
ACTIVE     → CHALLENGED
ACTIVE     → SUPERSEDED
CHALLENGED → SUPERSEDED
CHALLENGED → ACTIVE   // only if the challenging evidence is itself later
                       // contradicted or invalidated
```

**Rule:** when new valid evidence relation-types as `CONTRADICTS` against an `ACTIVE` `SKILL_DEFECT` claim, and the `SUPERSEDES` criteria above are not met, the claim status moves `ACTIVE → CHALLENGED`. The claim remains historically valid, remains visible in claim history, and is never deleted. It does not become `SUPERSEDED` until the `SUPERSEDES` criteria (built from at least two independent units, including Level 5+ or Level 7 evidence, confidence no lower than the prior active claim, no unresolved same-level contradiction) are satisfied.

This is a claim-status transition only. It does not, by itself, change any `RecoveryCase` state — `RecoveryCase` transitions are governed exclusively by Section 24's own evidence rules. Section 25 defines how claim status and `RecoveryCase` state are jointly projected onto the Digital Twin (Scenario L).

---

## 22. Evidence Confidence Policy

All values are exactly `UNKNOWN`, `LOW`, `MEDIUM`, or `HIGH`.

An independent evidence unit is one scored skill opportunity from one non-invalidated submitted attempt on one passage. All questions for that skill on that passage and attempt collapse into one unit; they cannot inflate quantity, diversity, or consistency. A single attempt may generate one unit for each separately declared skill, but those units are not independent sittings.

**Quantity:** no valid unit = UNKNOWN; 1 unit = LOW; 2–3 = MEDIUM; 4+ = HIGH.

**Diversity [v1.2.1]:** this resolves Codex's CONF-001 finding. v1.2 defined MEDIUM as "at least two distinct passages spanning at least two genre/topic tags" without stating whether multiple sittings were required, while LOW was defined as "one passage or one sitting" — so two distinct passages spanning two genre/topic tags produced within a single sitting could be scored MEDIUM by one compliant implementation and LOW by another. v1.2.1 resolves this by requiring temporal diversity at MEDIUM and above:

```text
UNKNOWN: no valid evidence units

LOW:     evidence is confined to one distinct passage, OR
         evidence is confined to one sitting (regardless of passage count)

MEDIUM:  requires ALL of —
           at least two distinct passages;
           at least two controlled genre/topic combinations;
           at least two distinct sittings

HIGH:    requires ALL of —
           at least four distinct passages;
           at least two controlled genre/topic combinations;
           at least two distinct sittings
```

Question count never changes this result. Two distinct passages spanning two genre/topic tags produced within a single sitting therefore remain LOW: exposure to multiple texts in one sitting does not establish temporal diversity (Scenario I).

**Internal consistency check:** MEDIUM's conditions are a strict subset of HIGH's (both now require ≥2 sittings; HIGH additionally requires 4 rather than 2 passages), so the bands are monotonic and no input can satisfy HIGH without also satisfying MEDIUM. This also aligns diversity with the evidence-hierarchy sitting cap already present below (no unit below Level 3c ⇒ confidence capped at LOW), since a same-sitting fresh-transfer unit is Level 3c, not Level 4 — no contradiction was found between the diversity band and the hierarchy cap.

**Consistency:** fewer than three independent units = UNKNOWN; otherwise calculate each unit's percentage from raw marks and use the range (maximum minus minimum): greater than 40 percentage points = LOW; greater than 20 and at most 40 = MEDIUM; at most 20 with at least four units = HIGH. Three units with a range at most 20 are MEDIUM because HIGH requires four units.

**Marker confidence:** objective marks are HIGH; valid open-response results use Section 19's derived state; no valid marker result is UNKNOWN.

**Overall:**

```text
no valid unit                         → UNKNOWN
one or two valid units                → LOW
otherwise                             → minimum(quantity, diversity,
                                              consistency, marker_confidence)
```

The minimum uses the four controlled states ordered LOW < MEDIUM < HIGH. A consistency value of UNKNOWN therefore keeps an early claim LOW through the explicit one/two-unit rule; it never becomes HIGH by omission. After that calculation, apply the evidence-hierarchy cap: if every supporting unit is Level 2, 3a, or 3b, overall confidence cannot exceed LOW; if no unit is Level 3c or higher, it cannot exceed LOW; and HIGH additionally requires at least one Level 5 or Level 7 unit. The calculation and all input values are stored under `evidenceConfidencePolicyVersion`.

---

## 23. Day 1 Raw-Mark Routing

Routing uses only validated final or objectively scored A–D marks. Section E is never an input. Let:

```text
readingRaw = A + B + C + D       // integer 0–26
overall = readingRaw / 26         // display percentage only
```

Bands are raw and exhaustive:

```text
23–26 → B2 boundary probe eligibility
19–22 → second independent B1 diagnostic
13–18 → B1 remediation
0–12  → A2/B1 boundary probe
```

The B2 eligibility gates are all required:

```text
A ≥ 7/8
B ≥ 5/6
C ≥ 6/8
```

The skill-vector override is evaluated before the band route using exact rational comparison. **[v1.2.1, ROUTE-001]** This resolves Codex's ROUTE-001 finding: v1.2's "route to every tied weakest skill" wording could undercount qualifying skills that were not numerically tied to one another — e.g. A = 3/8 and D = 2/4 can both independently clear the 20-point floor without being tied to each other or to any other skill.

```text
qualifyingWeakSkills =
    every A–D skill whose exact raw proportion (skillRaw / skillMax)
    is more than 20 percentage points below readingRaw / 26
```

If `qualifyingWeakSkills` is non-empty, route to `TARGETED_REMEDIATION` and include **every** qualifying skill — not only the single lowest one, and regardless of whether qualifying skills are numerically tied with each other. Display ordering is stable as A, B, C, D; ordering affects display only and never affects which skills are included, so ties among qualifying skills are irrelevant to inclusion (Scenario J). A B2-eligible score that fails a gate but triggers no override routes to `SECOND_B1_DIAGNOSTIC`; this closes the otherwise unreachable 85%+ gate-failure case.

Missing or pending A–D evidence produces `INSUFFICIENT_DATA` and no score. A pending Section E mark does not. A route is immutable once the next assignment enters `STARTED`; later information can create a new superseding `ROUTING_RECOMMENDATION` for a future assignment but cannot alter the started assignment.

Routing persists raw inputs, derived percentages, policy version, route, override skills, missing-data flags, and assignment status.

---

## 24. Transfer, Recovery, and Regression

Recovery is tracked by a `RecoveryCase` for one student, skill, and defect claim. The state is derived from claims and evidence; it is not independently edited in a Digital Twin snapshot.

```text
SUSPECTED → OBSERVED → IMPROVING → PROVISIONALLY_RECOVERED
                                      → VERIFIED_RECOVERED

IMPROVING → OBSERVED
PROVISIONALLY_RECOVERED → REGRESSED
VERIFIED_RECOVERED → REGRESSED
REGRESSED → IMPROVING only after a new intervention and qualifying near-transfer
```

Rules:

* `OBSERVED`: a valid Level-2 diagnostic skill result is below 70%.
* `IMPROVING`: intervention is recorded and a new-question same-passage near-transfer is at least 70%; same-question reattempt alone cannot reach this state.
* `IMPROVING → OBSERVED` **[v1.2.1, REC-001]**: while a `RecoveryCase` is `IMPROVING`, any valid fresh-transfer Level 3c+ skill unit scoring below 70% reverts the case to `OBSERVED`. This resolves Codex's REC-001 finding — v1.2 listed this transition in the state diagram above without ever defining its trigger. Near-transfer failure alone does not trigger this; same-question reattempt failure alone does not trigger this — only a qualifying Level 3c+ (or higher) fresh-transfer result below 70% does:

  ```text
  if currentState = IMPROVING
  AND newEvidence.level >= 3c
  AND skillPerformance < 70%
  then newState = OBSERVED
  ```

  If the triggering result is below 50%, additionally record `MAJOR_SETBACK` on the reverted claim (distinct from `MAJOR_REGRESSION` below, which applies only to the separate `REGRESSED` transition). This transition is never called `REGRESSED`: the student had not yet reached provisional or verified recovery, so there is no recovered state to regress from. All prior `IMPROVING`-state improvement evidence remains stored and immutable (Scenario M).
* `PROVISIONALLY_RECOVERED`: one fresh-transfer Level 3c+ unit is at least 70%.
* `VERIFIED_RECOVERED`: two successful units from distinct passages, including at least one Level 5+ unit. A later independent sitting may satisfy the second-unit requirement but is not required when the two distinct passages already satisfy this rule. Each unit must be at least 70%.
* `REGRESSED`: after provisional or verified recovery, a valid Level 3c+ or Level 7 unit is below 70%. A result below 50% is additionally `MAJOR_REGRESSION`.

Every transition creates an immutable recovery claim linked to the preceding claim. A failed later test never deletes successful recovery evidence. The existing Mission `PROPOSED → ACCEPTED → VERIFIED` lifecycle is operational workflow only; it cannot set `PROVISIONALLY_RECOVERED`, `VERIFIED_RECOVERED`, or `REGRESSED`.

---

## 25. Digital Twin Integration

Evidence Claims are the sole source of truth. The existing `twin_snapshot` table is a rebuildable materialised projection only: it stores a generated snapshot and reason, but it is not a source from which claims may be inferred and it must not be directly used to author a new student belief.

The projection is rebuilt from immutable claims, relation records, marks, exposure/attempt records, and policy versions. It includes the active claim set, current evidence confidence, evidence count/diversity, trend, open defects, recovery state, and separate CEFR-aligned, Athena Cambridge-style, and authentic Cambridge streams. Rebuilding the same claim history under the same policy versions must produce the same projection.

### Claim-status projection precedence [v1.2.1]

This resolves the projection half of Codex's CLAIM-001 finding. For `SKILL_DEFECT` claims specifically, the Digital Twin projection applies:

```text
SUPERSEDED  → not active; excluded from current open-defect display
CHALLENGED  → shown as unresolved/challenged evidence, but must not,
              by itself, force the skill's current RecoveryCase state
              to OBSERVED
ACTIVE      → shown as current defect evidence
```

Where a `RecoveryCase` state and a `SKILL_DEFECT` claim status differ — e.g. `RecoveryCase = PROVISIONALLY_RECOVERED` while the originating defect claim is `CHALLENGED` rather than `SUPERSEDED` — the `RecoveryCase` state governs what the Digital Twin displays as the student's *current* recovery position; the claim history and its status remain visible to explain *why*. Claim status and `RecoveryCase` state are never merged into a single field (Scenario L).

The current repository has no student ID on `twin_snapshot`; this is a Day 1 prerequisite for any English Digital Twin write. Until a student-bound claim store and projection path exist, English results must not be written into the existing snapshot as if the contract were implemented.

---

## 26. Reconciliation with Existing Athena Concepts

The discoverable repository is not yet the v1.2 English assessment platform.

| Existing location/concept | Actual implementation | v1.2 integration rule |
|---|---|---|
| `/backend` Chief Examiner | Spring Boot service with `Script`, `PageTranscript`, `QuestionSegment`, `QuestionMark`, `Mission`, and `TwinSnapshot`; Flyway `V1__init.sql` and `V2__marking_missions.sql` | Reuse only where explicitly mapped. It does not satisfy English assessment, exposure, claims, or student identity requirements. |
| `Script` | Handwritten upload lifecycle, including `READ`, `MARKING`, `MARKED` in the newer backend | Not an `AssessmentAttempt`. Do not map `Script` status to English exposure or recovery. |
| `question_mark.mistake_tag` | Single mathematics-oriented tag such as `SLIP`, `METHOD`, `MISREAD` | Not the v1.2 typed root-cause assignment. English causes require the versioned taxonomy in Section 20. |
| `Mission` | `PROPOSED → ACCEPTED → VERIFIED`, estimated/recovered marks and reattempt script | Workflow only; never English skill recovery. |
| `twin_snapshot` | Arbitrary JSON payload, reason, timestamp; no student ID | Rebuildable projection only, pending a student-bound claim store. |
| recon-week frontend | Authentication UI state, settings, sprint board, English resource page, and API calls backed by `localStorage` | No browser-local identity may authorise protected assessment content. |
| English/British Council page | Static external resource links, including B1–B2 and B2/C1 resources | Learning resources are not diagnostic evidence and cannot establish B1 alignment. |
| Mark-Loss Ledger | No entity with that name found; current question marks/missions are the nearest equivalent | v1.2 must not claim the ledger already exists. A future ledger adapter requires its own specification. |
| `/athena-backend` | Earlier snapshot of the same Chief Examiner service, with fewer marking/mission features | It is historical/alternate code, not a second source of English truth. |

No directly discoverable repository contains a Student Digital Twin schema, English assessment model, `AssessmentAttempt`, `EvidenceClaim`, `SyllabusVersion`, `StudentAssessmentAssignment`, or server-verified student identity. These are new concepts and are listed as prerequisites, not hidden implementation assumptions.

---

## 27. Security and Identity Boundary

Protected assessment content may be delivered only when the backend has verified all of:

1. an authenticated principal, server-verified;
2. a server-resolved student identity (student path) or authorised reviewer identity (reviewer/preview path, below);
3. an **ACTIVE** `StudentAssessmentAssignment` / eligibility record (Section 28) binding that exact student to that exact `AssessmentVersion`;
4. an active, authorised `AssessmentAttempt` that references that exact assignment/eligibility record and belongs to that student;
5. the `AssessmentVersion` is globally `ACTIVE`;
6. the access window, if the assignment/eligibility record defines one (`validFrom`/`validUntil`), permits delivery at request time.

**[v1.2.1, SEC-001]** This resolves Codex's SEC-001 finding: v1.2's requirement of "permission to receive the requested content" did not require a normative assignment/eligibility relationship, so an authenticated student could obtain an active attempt — and therefore protected content — for an assessment they were never assigned, merely by knowing an assessment/version ID. No assignment/eligibility record → **DENY**. An authenticated student cannot self-create eligibility by knowing an assessment ID. `AssessmentAttempt` creation itself must require an active assignment/eligibility record — an attempt cannot be created, and therefore cannot later be authorised for delivery, without one (Scenario N). This binding is enforced server-side only; frontend `localStorage` cannot satisfy any part of it.

The current frontend `localStorage` values (`athena_auth`, subject, and API settings) are UI state, not authentication. The current backend README states that server-side Google ID-token verification is not implemented. Therefore server-side identity verification, assignment/eligibility persistence, and attempt authorization are all **Day 1 prerequisites** (Section 34).

Preview, reviewer, and student endpoints must be distinct. Rubrics, answer keys, prompts, calibration data, and hidden item metadata remain server-only. Server logs, error payloads, analytics, proxies, and browser-visible responses must redact protected scoring material. Submission endpoints must be idempotent by attempt and submission token; duplicate requests cannot create duplicate marks or exposure events. An unauthorised or unassigned request creates no exposure event (Scenario N) — exposure (Section 14) is recorded only on the first successful delivery of protected content, and a `DENY` under this section delivers none.

---

## 28. Minimum Conceptual Data Model

The minimum entities are:

```text
Assessment
AssessmentVersion
Passage
Question
QuestionRubric
CalibrationRecord
FieldRevision
AssessmentAttempt
StudentAssessmentAssignment
StudentAssessmentExposure
StudentResponse
Mark
MarkerResult
RootCauseAssignment
EvidenceClaim
ClaimRelation
RecoveryCase
RoutingDecision
TransferTest
RetentionTest
SyllabusVersion
StudentIdentity / AuthorisedPrincipal
```

**[v1.2.1, SEC-001]** `StudentAssessmentAssignment` is the smallest sufficient eligibility concept and carries at minimum: `studentId`, `assessmentVersionId`, `assignmentId`, `status` (e.g. `ACTIVE` / `REVOKED` / `EXPIRED`), `assignedAt`, `validFrom`, `validUntil` (nullable). `AssessmentAttempt` creation requires an `ACTIVE` record of this kind; protected-content delivery requires the same record referenced by the attempt (Section 27).

Every student-bound entity has a stable student identity. Every historical event is bound to immutable assessment content, policy versions, and timestamps. An implementation may combine storage tables, but it must preserve these concepts and relationships.

---

## 29. CEFR Interpretation Constraints

Athena may say that performance is consistent with skills targeted by an Athena B1-aligned diagnostic. It may not say that a student is officially CEFR B1, B2, or any other certified level. No `CEFR_CERTIFIED` claim type exists. The B1 profile is a design anchor, not an external certification mechanism.

---

## 30. Cambridge Evidence

`AUTHENTIC_CAMBRIDGE_0500` means official Cambridge past-paper/source material or an officially licensed equivalent whose provenance is recorded. `ATHENA_CAMBRIDGE_STYLE` means Athena-authored material designed to resemble the examination. Only the former is Level 7 and may be described as authentic Cambridge examination evidence. Neither stream is converted to a CEFR-to-grade formula.

Cambridge-specific mark-loss records may later feed a Mark-Loss Ledger adapter, but the current repository's `question_mark` and `Mission` records are not silently declared to be that ledger.

---

## 31. Syllabus and Version Provenance

Every Cambridge-aligned artefact stores syllabus identifier, cycle, source reference, effective dates, review date, and status `CURRENT`, `SUPERSEDED`, or `UNDER_REVIEW`. An assessment authored while its syllabus is `UNDER_REVIEW` cannot become active until a human reviewer records the applicable version or rejects the alignment claim.

Historical attempts and claims retain the exact syllabus version used. A later current version never relabels historical evidence.

---

## 32. Auditability

The system retains immutable lifecycle transitions, exposure events, attempt status changes, submissions, content hashes, marks, marker results, field revisions, claim relations, root-cause evidence, recovery transitions, routing inputs, and every policy version. Human actions record authenticated principal, timestamp, and reason.

Retries and model failures are linked to the original event. They do not overwrite it. A historical record can be superseded, invalidated, or contradicted, but never silently edited or deleted.

---

## 33. Six-Week Operating Model

The campaign may use a small human-reviewed bank and no general multi-tenant workflow. That scope reduction does not permit policy ambiguity. Every active assessment still requires human approval, every route uses the fixed policy, and every recovery claim uses the evidence hierarchy and regression rules above.

Days 2–7 may reuse this contract only if they use existing claim types, marking policies, exposure semantics, recovery transitions, and routing primitives. A new construct, rubric, or routing rule requires a specification revision.

---

## 34. Day 1 Implementation Prerequisites

Day 1 implementation is blocked until these are explicitly provided and tested:

1. Server-verified authenticated identity and role authorization; frontend `localStorage` is insufficient.
2. Student-bound `AssessmentAttempt` and `StudentAssessmentExposure` persistence with idempotent delivery and resume semantics.
3. Assessment, passage, question, rubric, version, freeze, and human-review persistence.
4. The versioned `ATHENA-B1-READING-PROFILE-v1` artefact and review workflow.
5. Deterministic objective scorer and the Section E marker/escalation pipeline.
6. Immutable Evidence Claim store, relation handling, RecoveryCase derivation, and rebuildable Digital Twin projection.
7. Routing engine implementing raw A–D marks and `routingPolicyVersion`.
8. Syllabus/source provenance and content-contamination review records.
9. Server-only rubric/answer-key/marker prompt boundary with contract tests.
10. A decision about the integration boundary to the existing Chief Examiner and Mission services; no existing Mission or `twin_snapshot` record may be repurposed without preserving its current semantics.
11. **[v1.2.1, SEC-001]** Student-bound `StudentAssessmentAssignment` (eligibility) persistence and enforcement, checked both at `AssessmentAttempt` creation and at protected-content delivery time; an authenticated student must never be able to self-create eligibility merely by knowing an assessment or assessment-version ID.
12. **[v1.2.2, SUM-002/MARK-002]** The frozen `ATHENA-TEXT-NORMALIZATION-v1` and `ATHENA-ENGLISH-SPELLING-v1` assets — including a fixed `UnicodeVersion`, a fixed `lexiconVersion`, and any `assessmentAcceptedFormsVersion` accepted-proper-noun/variant/contraction lists — must be selected, frozen, and loaded by the objective and summary scorers before Day 1 becomes `ACTIVE`.
13. **[v1.2.3, SUM-002]** A frozen, versioned deterministic dictionary-correction spelling engine (`spellingEngineVersion`) implementing the Section 18 Step 3 exactly-one-distance-1-candidate rule, plus persistence for the `UNKNOWN_SPELLING_TOKEN` → `HUMAN_ACCEPTED`/`HUMAN_CONFIRMED_MISSPELLED` human-resolution audit event, must exist before Day 1 becomes `ACTIVE`.

---

## 35. Testable Acceptance Criteria

1. Identical validated A–D raw marks and policy versions produce identical routes.
2. Every integer raw reading total from 0 through 26 maps to exactly one route or `INSUFFICIENT_DATA` when required inputs are missing.
3. Section E never changes a Day 1 reading route.
4. A protected passage/question response creates exposure before rendering and a crash never restores `UNSEEN`.
5. Gerald and Melusi can share one globally `ACTIVE` version while their exposure records differ.
6. A single passage/sitting cannot produce diversity above LOW regardless of question count.
7. No operational decision reads `advisoryModelConfidence`.
8. Only the four controlled evidence-confidence states can be persisted or displayed.
9. Objective marking is reproducible from the frozen answer format, normalization rules, answer set, and policy version.
10. Summary marking records four content-point outcomes, copy-run count, word count/status, two language outcomes, marker evidence, and escalation status.
11. Human substantive edits after AI review force `REVISION_REQUIRED` and repeat the required checks.
12. `HUMAN_APPROVED` cannot be reached with an unresolved blocking finding.
13. Evidence Claims and relation records are immutable; the Digital Twin projection is rebuildable from them.
14. Same-question reattempt success cannot move recovery beyond IMPROVING.
15. A later sub-threshold Level 3c+ or Level 7 result moves provisional/verified recovery to REGRESSED and preserves prior history.
16. The root-cause classifier stores UNKNOWN when its direct-signal rules do not yield a unique cause.
17. `AUTHENTIC_CAMBRIDGE_0500` and `ATHENA_CAMBRIDGE_STYLE` produce different evidence classifications.
18. A later sitting is not recognised until at least 72 hours have elapsed.
19. Existing `Mission` verification cannot set English `VERIFIED_RECOVERED`.
20. No current frontend localStorage value can authorise protected assessment content.
21. **[v1.2.1, CONF-001]** Two distinct passages encountered within a single sitting cannot produce diversity above LOW, regardless of how many genre/topic tags they span.
22. **[v1.2.1, ROUTE-001]** Every A–D skill that independently satisfies the 20-point remediation floor is included in `TARGETED_REMEDIATION`, whether or not it is numerically tied with another qualifying skill.
23. **[v1.2.1, MARK-001]** `NUMERIC` is either fully specified under a deterministic parsing/precision/unit contract, or is not an authorised Day 1 answer format; v1.2.1 removes it for Day 1, eliminating undefined numeric equivalence.
24. **[v1.2.1, SUM-001]** A sentence-control fault a marker cannot deterministically classify against the five enumerated categories, or a boundary-identifiability judgement it cannot deterministically make, escalates to `PENDING_HUMAN_MARK` rather than being scored arbitrarily.
25. **[v1.2.1, CLAIM-001]** Valid evidence that `CONTRADICTS` an `ACTIVE` `SKILL_DEFECT` claim without meeting the `SUPERSEDES` criteria moves that claim to `CHALLENGED` — never silently deactivated or deleted.
26. **[v1.2.1, CLAIM-001]** A `CHALLENGED` `SKILL_DEFECT` claim cannot, by itself, force the affected skill's `RecoveryCase` to `OBSERVED`.
27. **[v1.2.1, REC-001]** An `IMPROVING` `RecoveryCase` reverts to `OBSERVED` on any valid Level 3c+ fresh-transfer result below 70%, and this transition is never labelled `REGRESSED`.
28. **[v1.2.1, SEC-001]** Protected assessment content is never delivered without a server-verified `ACTIVE` `StudentAssessmentAssignment` binding the requesting student to the requested `AssessmentVersion`.
29. **[v1.2.1, SEC-001]** `AssessmentAttempt` creation cannot succeed, and therefore cannot manufacture eligibility, without an active assignment/eligibility record.
30. **[v1.2.1, SEC-001]** An unassigned or unauthorised assessment request is denied and creates no `StudentAssessmentExposure` event.
31. **[v1.2.2, MARK-002]** Unicode normalization for Sections 17 and 18 is exactly NFKC under the frozen `ATHENA-TEXT-NORMALIZATION-v1` policy and a fixed `UnicodeVersion`; no compliant objective or copied-run scorer may substitute NFC.
32. **[v1.2.2, MARK-002]** Two scorers applying the same frozen `textNormalizationPolicyVersion` normalize Unicode compatibility characters (e.g. full-width forms) identically.
33. **[v1.2.2, SUM-003]** A copied run requires four or more consecutive normalized tokens contiguous in both the response and the passage; a response-contiguous sequence reconstructed from non-contiguous passage tokens is never counted.
34. **[v1.2.2, SUM-003]** Overlapping subspans of one maximal copied span are counted as exactly one copied run, never as separate overlapping runs.
35. **[v1.2.2, SUM-002]** Spelling is determined against the frozen `ATHENA-ENGLISH-SPELLING-v1` en-GB lexicon plus the frozen accepted-variant, proper-noun, and contraction lists — never marker intuition.
36. **[v1.2.2, SUM-002]** An `UNKNOWN_SPELLING_TOKEN` that could affect the ≤2-error threshold forces `PENDING_HUMAN_MARK` rather than being auto-counted as an error.
37. **[v1.2.2, SUM-002]** A proper noun is exempt from spelling-error counting only through passage occurrence, a frozen `acceptedProperNouns` list, or explicit approval in the frozen summary-marking asset — never dynamic marker invention.
38. **[v1.2.2, SUM-002]** A common standard American-English variant is accepted without penalty only when present in the frozen accepted-variant list.
39. **[v1.2.2, SUM-002]** The same misspelled token occurring three times in one response counts as three spelling errors, not one.
40. **[v1.2.2]** `textNormalizationPolicyVersion`, `UnicodeVersion`, `spellingPolicyVersion`, `lexiconVersion`, `assessmentAcceptedFormsVersion`, `objectiveMarkingPolicyVersion`, and `summaryMarkingPolicyVersion` are persisted with every mark they governed.
41. **[v1.2.2]** A historical mark is never re-normalized, re-classified, or re-scored under a later Unicode, lexicon, or normalization-policy version; it is read back exactly as computed.
42. **[v1.2.2]** No previously resolved Codex finding (CONF-001, ROUTE-001, MARK-001, SUM-001, CLAIM-001, REC-001, SEC-001) is reopened by this revision.
43. **[v1.2.3, SUM-002]** Every spelling token occurrence resolves automatically to exactly one of `IGNORED`, `ACCEPTED`, `MISSPELLED`, or `UNKNOWN_SPELLING_TOKEN`; no non-accepted, non-exempt token is ever left unclassified.
44. **[v1.2.3, SUM-002]** No implementation may treat `UNKNOWN_SPELLING_TOKEN` as `MISSPELLED` without a persisted human-resolution event.
45. **[v1.2.3, SUM-002]** Deterministic `MISSPELLED` classification requires the exact versioned spelling-engine rule (exactly one recognised dictionary correction candidate at edit distance 1) — zero or multiple candidates never resolve to `MISSPELLED`.
46. **[v1.2.3, SUM-002]** Human spelling resolution is append-only and retains the original `UNKNOWN_SPELLING_TOKEN` classification unchanged.
47. **[v1.2.3, SUM-002]** Three confirmed misspellings determine spelling mark 0 even when unresolved unknown tokens also exist (Scenario V).
48. **[v1.2.3, SUM-002]** Two confirmed misspellings plus one unresolved unknown token produces `PENDING_HUMAN_MARK`, never an auto-scored mark (Scenario U).
49. **[v1.2.3, SUM-002]** Repeated occurrences of one confirmed-misspelled surface form count once per occurrence, never collapsed (Scenario T).
50. **[v1.2.3, SUM-003]** A copied-run candidate is represented as `(responseStart, passageStart, length)` and requires contiguous equality in both response and passage token sequences.
51. **[v1.2.3, SUM-003]** Maximality (`MAXIMAL_COPIED_SPAN`) is evaluated only along one shared `alignmentOffset`; a candidate is never extended across a different alignment.
52. **[v1.2.3, SUM-003]** Candidate spans on different `alignmentOffset` values are never merged, even when their response-token ranges overlap.
53. **[v1.2.3, SUM-003]** Overlapping response-coordinate candidates on different alignments are resolved by the normative sort-and-greedy-select algorithm (`responseStart` ascending, `length` descending, `passageStart` ascending; response-interval non-overlap), never by an unqualified union.
54. **[v1.2.3, SUM-003]** Same-`responseStart` ties select the longer span first, then the lower `passageStart`.
55. **[v1.2.3, SUM-003]** Adjacent response-coordinate spans (no overlap) may both be selected and both count (Scenario Y).
56. **[v1.2.3, SUM-003]** Codex's crossing-alignment counterexample (`Response = a b a b c d`, `Passage = a b c d a b a b`) produces exactly one prescribed `copiedRunCount` (Scenario W).
57. **[v1.2.3]** `spellingEngineVersion` is persisted with every Section E spelling mark and every human spelling-resolution event it governed; a later bump never re-classifies a historical token occurrence.
58. **[v1.2.3]** No previously resolved Codex finding (CONF-001, ROUTE-001, MARK-001, SUM-001, CLAIM-001, REC-001, SEC-001, MARK-002) is reopened by this revision.

---

## 36. Normative Scenarios A–Y

### Scenario A — strong except inference

```text
A=8/8, B=6/6, C=5/8, D=4/4, E=7/8
```

`readingRaw = 23/26`. The B2 gates fail because inference is 5/8. The inference percentage is 62.5%, more than 20 percentage points below the exact overall percentage 88.46%. The route is `TARGETED_REMEDIATION`, skill `INFERENCE`. Section E is ignored for routing.

### Scenario B — summary marker failure

If A–D are valid, routing uses only their raw total and gates. Section E is `PENDING_HUMAN_MARK` and contributes no route input and no final writing claim. The submitted text may be retained as raw evidence. After human marking it creates `PRELIMINARY_WRITING_EVIDENCE`, LOW confidence, and still does not revise the Day 1 reading route.

### Scenario C — crash after delivery

The attempt is `EXPOSED` at the moment protected content is delivered, even before the first answer. The exposure is permanent and the student is not unseen. The authorised resume operation returns the same attempt and does not create a new unseen exposure. A failed delivery with no protected content response would not expose the student.

### Scenario D — vocabulary versus inference

If a recorded key-word misunderstanding and successful post-explanation answer exist, store `ACCESS / VOCABULARY_ACCESS` as primary and `INFERENCE` as contributing. Without that direct signal, store `primaryCause = UNKNOWN` with both as contributing hypotheses.

### Scenario E — recovery then authentic failure

After an intervention is recorded, the near-transfer moves `OBSERVED → IMPROVING`; the fresh transfer moves to `PROVISIONALLY_RECOVERED`; the later new-genre success supplies the second qualifying success and moves to `VERIFIED_RECOVERED`; the later authentic Cambridge result below 70% moves to `REGRESSED` (and below 50% additionally records `MAJOR_REGRESSION`). All prior claims remain immutable.

### Scenario F — human content edit

A substantive human change after AI review moves the candidate to `REVISION_REQUIRED`. Deterministic checks and independent AI review run again. It cannot become active until a new `HUMAN_APPROVED` event and freeze. If the version was already frozen, a new version is created.

### Scenario G — two students, one version

```text
global AssessmentVersion: ACTIVE
Gerald: StudentAssessmentExposure = EXPOSED/SUBMITTED
Melusi: StudentAssessmentExposure = UNSEEN
```

Gerald's exposure does not retire the version globally or change Melusi's status.

### Scenario H — raw routing reachability

Totals 0, 13, 19, and 23 are reachable, so all four bands are reachable. For example, 0/26 routes A2/B1; 13/26 routes remediation; 19/26 routes a second B1 diagnostic; 23/26 is B2-eligible only if A≥7, B≥5, and C≥6. A 23–26 total failing a gate and not triggering the 20-point override routes to a second B1 diagnostic; it has no unassigned case.

### Scenario I — diversity within one sitting [v1.2.1, CONF-001]

```text
2 distinct passages
2 controlled genre/topic combinations
1 sitting
```

Under the v1.2.1 diversity rule, MEDIUM requires at least two distinct sittings, which this evidence does not have. Diversity = `LOW`. This is the deliberate resolution of the v1.2 counterexample in which two compliant implementations could disagree (one producing MEDIUM, one producing LOW) for this exact input.

### Scenario J — multiple non-tied remediation skills [v1.2.1, ROUTE-001]

```text
A = 3/8, B = 6/6, C = 8/8, D = 2/4
readingRaw = 19/26  (73.08%)
```

Per-skill proportions: A = 37.5%, B = 100%, C = 100%, D = 50%. The floor is `readingRaw/26 − 20 points ≈ 53.08%`. A (37.5%) and D (50%) both fall more than 20 points below 53.08%; B and C do not. A and D are not numerically tied with each other. `qualifyingWeakSkills = {A, D}`. Route: `TARGETED_REMEDIATION`, targets **A and D**, displayed in order A, D. (19/26 alone would route `SECOND_B1_DIAGNOSTIC`; the skill-vector override takes precedence over the band route.)

### Scenario K — numeric equivalence [v1.2.1, MARK-001]

```text
accepted numeric value = 1
student submits = 1.0
```

`NUMERIC` is not an authorised Day 1 answer format (Section 15). This question could not have passed deterministic calibration or been frozen as a Day 1 objective item, so this counterexample cannot arise in Day 1 under v1.2.1 — there is no mark to compute because there is no such question. A future day reintroducing `NUMERIC` must adopt the full parsing/precision/unit contract described in Section 15's MARK-001 note before any such question may be authorised.

### Scenario L — challenged defect claim [v1.2.1, CLAIM-001]

```text
Existing: SKILL_DEFECT / INFERENCE, status = ACTIVE, RecoveryCase = OBSERVED
New:      one valid fresh-transfer Level 3c+ unit, INFERENCE, ≥70%
```

The new evidence opposes the defect's predicate, is valid, and is at a higher evidence level than the defect's originating Level-2 evidence — relation = `CONTRADICTS`. Supersession is not met (only one new independent unit; no Level 5+/7 evidence). Defect claim status: `ACTIVE → CHALLENGED`; the original claim remains stored and visible, not deleted. Applying Section 24's `PROVISIONALLY_RECOVERED` rule unchanged (one fresh-transfer Level 3c+ unit ≥70%), `RecoveryCase → PROVISIONALLY_RECOVERED`. Digital Twin projection: `Defect status: CHALLENGED`, `RecoveryCase: PROVISIONALLY_RECOVERED`; the CHALLENGED claim is shown as unresolved/challenged history explaining why a defect was once recorded, and does not itself force any state — the current recovery position is read from `RecoveryCase`, per Section 25's projection precedence.

### Scenario M — improving setback [v1.2.1, REC-001]

```text
RecoveryCase = IMPROVING
New: valid fresh-transfer Level 3c+ unit, < 70%
```

Per Section 24's `IMPROVING → OBSERVED` trigger, `RecoveryCase → OBSERVED`. If the result is additionally below 50%, `MAJOR_SETBACK` is also recorded. This is not called `REGRESSED` — the case had not reached `PROVISIONALLY_RECOVERED`. All prior `IMPROVING`-state evidence remains stored and immutable.

### Scenario N — unassigned student [v1.2.1, SEC-001]

An authenticated, server-verified student requests a globally `ACTIVE` `AssessmentVersion` for which no `ACTIVE` `StudentAssessmentAssignment` exists for that student. Expected: `DENY`. No `AssessmentAttempt` can be created (attempt creation itself requires an active assignment/eligibility record), no protected passage or question content is delivered, and no `StudentAssessmentExposure` event is created. Knowing a valid, globally `ACTIVE` assessment-version ID is never sufficient to self-create eligibility.

### Scenario O — unknown spelling token [v1.2.2, SUM-002]

```text
Response contains two confirmed MISSPELLED tokens
plus one token classified UNKNOWN_SPELLING_TOKEN
(absent from the frozen lexicon and every frozen accepted list)
```

The two confirmed errors alone would sit within the ≤2 threshold, but the unresolved unknown token could affect that threshold if it were later confirmed misspelled. Expected: spelling criterion = `PENDING_HUMAN_MARK` until a human resolves the unknown token; it is never automatically counted as the third error.

### Scenario P — American spelling variant [v1.2.2, SUM-002]

```text
Response uses "color" where the frozen accepted-variant list
includes the color/colour pair
```

Expected: not a spelling error — the frozen list, not marker judgement, governs acceptance.

### Scenario Q — copied sequence with passage gap [v1.2.2, SUM-003]

```text
Response: the old bridge finally collapsed
Passage:  the old bridge unexpectedly and finally collapsed
```

The response's four-token sequence "old bridge finally collapsed" is contiguous in the response, but in the passage "finally" is separated from "bridge" by "unexpectedly and" — the corresponding passage tokens are not contiguous. Expected: no 4+ token copied run for that sequence.

### Scenario R — contiguous copy [v1.2.2, SUM-003]

```text
Response and passage both contain the contiguous sequence:
the old bridge finally collapsed
```

Expected: one copied run, length 5; the internal 4-token subspans within it are not separately counted as additional runs.

### Scenario S — Unicode compatibility form [v1.2.2, MARK-002]

```text
Frozen canonical answer: Athena
Student submits a full-width compatibility equivalent
that NFKC maps to the same canonical string
```

Expected: correct, provided all other frozen answer rules (e.g. case folding) are satisfied — NFKC folds the compatibility form to its canonical equivalent before comparison.

### Scenario T — three definite misspellings [v1.2.3, SUM-002]

```text
Three token occurrences each independently satisfy the Step 3
deterministic MISSPELLED rule (exactly one distance-1 dictionary
correction candidate).
```

Expected: `confirmedMisspellingCount = 3`, spelling mark = `0`. Each occurrence counts separately even where the surface form repeats (Section 18 error-counting rule).

### Scenario U — two misspellings plus one unresolved unknown [v1.2.3, SUM-002]

```text
Two token occurrences are deterministically MISSPELLED.
One further token occurrence is UNKNOWN_SPELLING_TOKEN.
```

Expected: `confirmedMisspellingCount = 2`, `unknownCount = 1`, spelling criterion = `PENDING_HUMAN_MARK` (Case C). If the human reviewer resolves the unknown token as `HUMAN_ACCEPTED`, `confirmedMisspellingCount` stays `2` and the mark becomes `1`. If the human reviewer resolves it as `HUMAN_CONFIRMED_MISSPELLED`, `confirmedMisspellingCount` becomes `3` and the mark becomes `0`. Both transitions are deterministic from the recorded resolution.

### Scenario V — three misspellings plus an unresolved unknown [v1.2.3, SUM-002]

```text
Three token occurrences are deterministically MISSPELLED (or
HUMAN_CONFIRMED_MISSPELLED).
One further token occurrence is UNKNOWN_SPELLING_TOKEN.
```

Expected: `confirmedMisspellingCount >= 3` → spelling mark = `0` (Case A). The unknown occurrence remains recorded for audit but does not force `PENDING_HUMAN_MARK` — the mark is already mathematically determined and cannot be changed by resolving it either way.

### Scenario W — ambiguous crossed copied alignments [v1.2.3, SUM-003]

```text
Response: a b a b c d
Passage:  a b c d a b a b
```

Maximal candidates: `(responseStart=0, passageStart=4, length=4)` on `alignmentOffset=4`, and `(responseStart=2, passageStart=0, length=4)` on `alignmentOffset=-2`. Sorted by `responseStart` ascending, `(0,4,4)` is selected first; `(2,0,4)`'s response interval `[2,6)` overlaps the selected `[0,4)` and is rejected regardless of its distinct passage alignment. Expected: `copiedRunSpans = [(0,4,4)]`, `copiedRunCount = 1`, paraphrase mark = `1`. This is the unique compliant outcome (full derivation in Section 18).

### Scenario X — same-response-start repeated passage alignment [v1.2.3, SUM-003]

```text
Response: ... p q r s ...
Passage contains "p q r s" twice, at two different passageStart offsets.
```

Both candidates share the same `responseStart` and `length`, with different `passageStart` values. Expected: per the tie-break rule, the candidate with the **lower `passageStart` wins**; only that one selected span affects `copiedRunCount`, which counts this repeated-passage-text case once, not twice.

### Scenario Y — adjacent copied runs [v1.2.3, SUM-003]

```text
Response: [4-token copied span][4-token copied span], immediately
adjacent, with no overlapping response tokens (e.g. spans at response
intervals [0,4) and [4,8)).
```

The two spans do not overlap in response coordinates (`4 < 4` is false), so both are selected. Expected: `copiedRunCount = 2`.

---

## 37. Codex Hostile Finding Resolution Matrix (v1.1 → v1.2)

| Finding | Status | v1.2 resolution |
|---|---|---|
| Global lifecycle conflated with student exposure | RESOLVED | Sections 14, 35, Scenario G |
| Undefined unseen event / crash / abandonment semantics | RESOLVED | Section 14, Scenario C |
| Evidence Confidence lookup deferred | RESOLVED | Section 22 |
| Independent evidence unit undefined | RESOLVED | Sections 6 and 22 |
| Consistency based on item variance | RESOLVED | Section 22 |
| Numeric LLM self-confidence | RESOLVED | Section 19 |
| B1 alignment subjective | RESOLVED | Section 10 |
| Routing unreachable cases / summary conflict | RESOLVED | Section 23, Scenario A/B/H |
| Small-sample percentages | RESOLVED | Sections 7 and 23 |
| Summary rubric undefined | RESOLVED | Section 18 |
| Objective marking undefined | RESOLVED | Section 17 |
| Evidence Claim relation semantics | RESOLVED | Section 21 |
| Recovery one-way | RESOLVED | Section 24, Scenario E |
| Root-cause overlap | RESOLVED | Section 20, Scenario D |
| Author/reviewer loopbacks | RESOLVED | Section 13, Scenario F |
| Human approval meaning | RESOLVED | Section 13 |
| Provenance overwrite risk | RESOLVED | Sections 9 and 13 |
| API authentication / authorization gap | RESOLVED | Section 27 defines the required boundary; Section 34 explicitly blocks Day 1 until server identity exists |
| Existing Digital Twin mismatch | RESOLVED | Sections 25/26 define the rebuildable projection and explicitly prohibit writes until the student-bound claim store exists |
| Existing Mission conflation | RESOLVED | Sections 24, 26, Acceptance Criterion 19 |
| Cambridge-style versus authentic evidence | RESOLVED | Sections 3, 6, 30 |
| Retention timing | RESOLVED | 72-hour rule in Section 6 |
| Syllabus version provenance | RESOLVED | Section 31 |
| Cross-reference ambiguity | RESOLVED | v1.2 sections use the numbered definitions above |

No unresolved blocker remains inside the normative v1.2 contract. The deferred items are current-platform prerequisites, not permission to implement around missing security or evidence infrastructure.

---

## 38. Repository Reconciliation Matrix

The complete repository comparison is Section 26. In summary, the current Athena code provides a Chief Examiner handwriting pipeline, mathematics question marks, mathematics missions, an arbitrary twin snapshot, a localStorage-heavy frontend, and static British Council resources. It does not provide the student-bound English diagnostic architecture specified here. This is an intentional, explicit integration gap, not a claim of existing functionality.

---

## 39. v1.1 → v1.2 Change Log

* Separated global `AssessmentVersion` lifecycle from `StudentAssessmentExposure`.
* Defined permanent exposure on protected-content delivery, including crash, abandonment, invalidation, resume, repeat, and preview behaviour.
* Replaced deferred Evidence Confidence work with complete quantity, diversity, consistency, marker, and overall policies.
* Defined independent evidence units and removed item-count inflation.
* Constrained LLM marker confidence to structured deterministic signals; numeric self-confidence is advisory only.
* Added `ATHENA-B1-READING-PROFILE-v1` with reviewer ratings, decision rules, and CEFR non-certification constraints.
* Rewrote Day 1 routing around raw A–D marks out of 26 and closed the high-score gate-failure hole.
* Added deterministic objective scoring and an 8-mark summary contract.
* Added controlled Evidence Claim types and relation precedence.
* Added recovery regression and separated RecoveryCase state from existing Mission workflow.
* Operationalised root-cause classification with typed failure classes and an UNKNOWN rule.
* Added author/reviewer loopbacks, blocking/warning/advisory findings, and append-only field provenance history.
* Reconciled the specification against the actual Chief Examiner, Mission, `question_mark`, `twin_snapshot`, frontend, English resource, and authentication implementations.
* Distinguished authentic Cambridge evidence from Athena Cambridge-style material and defined retention timing.
* Added Day 1 prerequisites, normative scenarios, acceptance criteria, and resolution matrices.

---

## 40. Deferred Work

The following are not silently resolved by implementation:

* Building the server-side authenticated student identity and role boundary.
* Creating the English assessment, exposure, attempt, assignment/eligibility, claim, recovery, and projection persistence.
* Producing the B1 profile asset and reviewer interface.
* Deciding the future adapter boundary to a Mark-Loss Ledger when that platform concept is formally specified.
* Implementing the rebuildable Digital Twin projection from claims.
* Selecting an official Cambridge source and recording its syllabus/provenance data for any later Level 7 work.
* **[v1.2.2]** Selecting and freezing the actual `ATHENA-TEXT-NORMALIZATION-v1` Unicode library/version and the `ATHENA-ENGLISH-SPELLING-v1` en-GB lexicon, accepted-variant list, and accepted-proper-noun list — this specification defines the contract those assets must satisfy, not the assets themselves.

These are Day 1 prerequisites or later specifications. They do not permit a developer to substitute localStorage identity, existing Mission verification, arbitrary twin JSON, self-created eligibility, model self-confidence, an unversioned dictionary, or an unspecified Unicode normalization form.

---

## 41. Final Hostile Self-Review (v1.2.1)

The v1.2 contract gave one normative result for the required scenarios: exposure is per student, confidence uses a complete policy, routing uses reachable raw marks, summary cannot alter reading routing, recovery can regress, and evidence claims are immutable and typed. The current repository mismatches are explicit and converted into prerequisites rather than silently reconciled.

The final Codex implementation-gate review confirmed this — Scenarios A–H all PASS and 2,835/2,835 routing combinations tested — while finding seven remaining places (CONF-001, ROUTE-001, MARK-001, SUM-001, CLAIM-001, REC-001, SEC-001) where two compliant implementations could still diverge. v1.2.1 closes each by choosing one deterministic interpretation, propagating it to terminology, algorithms, scenarios, data requirements, and acceptance criteria, and adding six new normative scenarios (I–N) covering exactly those counterexamples. No architecture outside the seven findings was reopened.

The remaining implementation work is substantial, especially server identity, assignment/eligibility enforcement, and the new student-bound evidence store. That is not a defect in this specification provided implementation cannot start until Section 34 is satisfied. No current Athena code is represented as already meeting the v1.2.1 contract.

---

## 42. Final Gate Finding Resolution Matrix (v1.2.1)

| ID | Defect | v1.2.1 rule | Status |
|---|---|---|---|
| CONF-001 | Diversity ambiguity: MEDIUM did not state whether multiple sittings were required, so two passages/two genres in one sitting could be scored MEDIUM or LOW by different compliant implementations. | Section 22 Diversity: MEDIUM and HIGH both require ≥2 distinct sittings; LOW is confined-to-one-passage OR confined-to-one-sitting. Scenario I. | RESOLVED |
| ROUTE-001 | Multi-skill override: "route to every tied weakest skill" could undercount independently-qualifying, non-tied weak skills. | Section 23: `qualifyingWeakSkills` is every skill independently more than 20 points below `readingRaw/26`; all are included in `TARGETED_REMEDIATION`; ties affect only display order. Scenario J. | RESOLVED |
| MARK-001 | `NUMERIC` answer format permitted with no defined numeric-equivalence contract (`1` vs. `1.0`). | Section 15: `NUMERIC` removed from authorised Day 1 formats — the smallest deterministic fix; reintroduction requires a full parsing/precision/unit contract. Scenario K. | RESOLVED |
| SUM-001 | Sentence-control criterion used subjective language ("complete," "grammatically interpretable," "clearly separates sentences"). | Section 18: deterministic four-condition sentence-control checklist, five narrowly-defined grammar faults, and a `PENDING_HUMAN_MARK` escalation rule for undecidable cases. | RESOLVED |
| CLAIM-001 | Undefined disposition of an `ACTIVE` `SKILL_DEFECT` claim when new evidence `CONTRADICTS` it without meeting supersession. | Section 21: `ACTIVE → CHALLENGED` transition, claim retained and visible, never deleted; Section 25 defines projection precedence against `RecoveryCase`. Scenario L. | RESOLVED |
| REC-001 | `IMPROVING → OBSERVED` transition listed in the state diagram with no defined trigger. | Section 24: any valid Level 3c+ fresh-transfer result below 70% while `IMPROVING` reverts the case to `OBSERVED`; sub-50% also records `MAJOR_SETBACK`. Scenario M. | RESOLVED |
| SEC-001 | "Permission to receive the requested content" was not bound to a normative assignment/eligibility relationship. | Sections 27/28: `StudentAssessmentAssignment` required `ACTIVE` for both attempt creation and content delivery; no assignment → `DENY`; server-side only. Scenario N. | RESOLVED |

No row is `DEFERRED`.

---

## 43. v1.2 → v1.2.1 Change Log

* Defined diversity's MEDIUM/HIGH bands to require at least two distinct sittings, closing the one-sitting/two-passage ambiguity (CONF-001, Section 22, Scenario I).
* Replaced "every tied weakest skill" with an explicit `qualifyingWeakSkills` set covering every independently-qualifying skill regardless of ties (ROUTE-001, Section 23, Scenario J).
* Removed `NUMERIC` from authorised Day 1 answer formats rather than partially specifying it, and documented the full contract required to reintroduce it later (MARK-001, Section 15, Scenario K).
* Replaced the subjective sentence-control criterion with a deterministic four-condition checklist, five narrowly-defined grammar faults, a `PENDING_HUMAN_MARK` ambiguity rule, and a non-double-counting rule against the spelling/punctuation mark (SUM-001, Section 18).
* Added `SKILL_DEFECT` claim status (`ACTIVE`/`CHALLENGED`/`SUPERSEDED`), the `CONTRADICTS → CHALLENGED` transition rule, and Digital Twin projection precedence distinguishing claim status from `RecoveryCase` state (CLAIM-001, Sections 21 and 25, Scenario L).
* Defined the `IMPROVING → OBSERVED` trigger (Level 3c+ fresh-transfer below 70%), added `MAJOR_SETBACK`, and clarified this transition is never labelled `REGRESSED` (REC-001, Section 24, Scenario M).
* Added `StudentAssessmentAssignment` as a required server-verified eligibility binding for both attempt creation and content delivery, with `DENY` on absence (SEC-001, Sections 27, 28, 34, Scenario N).
* Added ten new acceptance criteria (21–30) and six new normative scenarios (I–N), one pair per finding.
* Added this Final Gate Finding Resolution Matrix (Section 42) and this change log (Section 43).

No other section's normative content was altered. Sections 1–14, 16, 17, 19, 20, 26, 29–33, and 37–41 are carried forward from v1.2 unchanged in substance (cross-references to changed sections and terminology updated where required).

---

## 44. Final Gate Finding Resolution Matrix (v1.2.2)

| ID | Defect | v1.2.2 resolution | Status |
|---|---|---|---|
| SUM-002 | Spelling classification is not deterministic: the same token could be treated as an error by one implementation and as an accepted variant, proper noun, contraction, or unknown-but-valid word by another. | Section 18: versioned `ATHENA-ENGLISH-SPELLING-v1` policy (`spellingPolicyVersion`, `lexiconVersion`, `assessmentAcceptedFormsVersion`); fixed five-step token classification order (non-word → proper noun → contraction → British/American variant → lexicon lookup); `UNKNOWN_SPELLING_TOKEN` forces `PENDING_HUMAN_MARK` rather than auto-counting; each repeated occurrence of a misspelled token counts as one error. Scenarios O, P. | RESOLVED |
| SUM-003 | Copied-run contiguity is ambiguous: "consecutive tokens appearing in the passage in the same order" could mean consecutive in both sequences or consecutive in the response only, with passage-order gaps. | Section 18: a copied run requires `response[i:i+n] == passage[j:j+n]`, `n ≥ 4`, contiguous in both sequences — a response-contiguous, passage-gapped sequence never counts. Maximal non-contained spans of length ≥4 are counted once; overlapping subspans are not double-counted. `copiedRunCount`, `maxCopiedRunLength`, `copiedRunSpans[]` stored for audit. Scenarios Q, R. | RESOLVED |
| MARK-002 | Unicode normalization form/version is unspecified: "Unicode-normalize" did not state NFC vs. NFKC, so compatibility characters (e.g. full-width forms) could normalize differently across implementations. | Section 17: shared, versioned `ATHENA-TEXT-NORMALIZATION-v1` base pipeline (NFKC, never NFC, under a frozen `UnicodeVersion`) plus Unicode default case folding and whitespace normalization; used identically by Sections 17 and 18, each layering only its own additional transformations. Scenario S. | RESOLVED |

All three rows are `RESOLVED`. No row is `DEFERRED`; a `DEFERRED` row here would have required reporting `NOT READY — v1.3 REQUIRED` per this revision's task brief, which does not apply.

---

## 45. v1.2.1 → v1.2.2 Change Log

* Defined a shared, versioned `ATHENA-TEXT-NORMALIZATION-v1` base pipeline (NFKC, Unicode default case folding, whitespace normalization, under a frozen `UnicodeVersion`) and made Sections 17 and 18 build on it explicitly rather than each independently interpreting "Unicode-normalize" (MARK-002, Section 17, Scenario S).
* Replaced the single-paragraph copied-run rule with a contiguous-in-both-sequences definition, a maximal-non-contained-span counting rule, and an auditable `copiedRunCount`/`maxCopiedRunLength`/`copiedRunSpans[]` result (SUM-003, Section 18, Scenarios Q and R).
* Replaced the undefined spelling-error rule with a versioned `ATHENA-ENGLISH-SPELLING-v1` policy: a fixed five-step token classification order (non-word, proper noun, contraction, British/American variant, lexicon lookup), an `UNKNOWN_SPELLING_TOKEN` → `PENDING_HUMAN_MARK` escalation rule, and a once-per-occurrence error-counting rule (SUM-002, Section 18, Scenarios O and P).
* Added `textNormalizationPolicyVersion`, `spellingPolicyVersion`, `lexiconVersion`, `assessmentAcceptedFormsVersion`, and `UnicodeVersion` to the Policy Version Registry (Section 5) and to the fields persisted with objective and Section E marks (Sections 17, 18).
* Added one new Day 1 prerequisite (Section 34, item 12) requiring the text-normalization and spelling assets to be selected and frozen before Day 1 becomes `ACTIVE`, and one new Deferred Work item (Section 40) naming those assets as not yet selected.
* Added twelve new acceptance criteria (31–42) and five new normative scenarios (O–S), covering the three findings.
* Added this Final Gate Finding Resolution Matrix (Section 44) and this change log (Section 45).

No other section's normative content was altered. All seven v1.2→v1.2.1 findings (CONF-001, ROUTE-001, MARK-001, SUM-001, CLAIM-001, REC-001, SEC-001) and their governing sections (Sections 15, 21, 22, 23, 24, 25, 27, 28) are unchanged and not reopened. Sections 1–2, 4, 6–16, 19–34 (aside from the single new prerequisite item), and 37–43 are carried forward from v1.2.1 unchanged in substance (cross-references to changed sections and terminology updated where required).

---

## 46. Final Self-Test (v1.2.2)

> Can two compliant implementations classify the previous SUM-002 spelling counterexample differently?

**NO.** Section 18's fixed five-step classification order, backed by the frozen `ATHENA-ENGLISH-SPELLING-v1` lexicon and accepted-list versions, gives every token exactly one outcome — `MISSPELLED`, exempt, or `UNKNOWN_SPELLING_TOKEN` escalating to `PENDING_HUMAN_MARK` — with no step left to marker intuition.

> Can two compliant implementations disagree on whether a non-contiguous passage sequence is a copied run?

**NO.** Section 18's copied-run rule requires `response[i:i+n] == passage[j:j+n]` with no intervening passage tokens; a sequence whose passage tokens are not contiguous fails this test under any compliant implementation (Scenario Q).

> Can one compliant implementation use NFC and another NFKC?

**NO.** Section 17 fixes `ATHENA-TEXT-NORMALIZATION-v1` to NFKC under a frozen `UnicodeVersion`, shared identically by Sections 17 and 18; NFC is explicitly excluded.

All three answers are `NO`. The v1.2.2 patch was complete against MARK-002 and against the counterexamples known at that time. This section is superseded by Section 49 below, which re-runs the same self-test against the new SUM-002/SUM-003 counterexamples the latest gate raised against v1.2.2's own language.

---

## 47. Final Gate Finding Resolution Matrix (v1.2.3)

| ID | Defect | v1.2.3 resolution | Status |
|---|---|---|---|
| SUM-002 | v1.2.2's five-step classification correctly identified `ACCEPTED`/exempt tokens and named `UNKNOWN_SPELLING_TOKEN`, but never defined how a non-accepted, non-exempt token becomes `MISSPELLED` — leaving `spellingErrors` uncomputable under one honest reading and silently invented under another, so two compliant implementations could produce opposite outcomes (`PENDING_HUMAN_MARK × 3` vs. `MISSPELLED × 3, mark = 0`) for the same three tokens. | Section 18: exhaustive four-state classification (`IGNORED` / `ACCEPTED` / `MISSPELLED` / `UNKNOWN_SPELLING_TOKEN`); new deterministic Step 3 `MISSPELLED` rule (exactly one distance-1 dictionary correction candidate, versioned under `spellingEngineVersion`); explicit `HUMAN_ACCEPTED`/`HUMAN_CONFIRMED_MISSPELLED` append-only human-resolution event; Case A/B/C rule for exactly when an unresolved unknown forces `PENDING_HUMAN_MARK` versus when the mark is already mathematically determined. Scenarios T, U, V. | RESOLVED |
| SUM-003 | v1.2.2 correctly required copied-run contiguity in both sequences, but its residual "overlapping candidates are merged into the maximal span" instruction was not restricted to candidates sharing the same response/passage alignment — two candidates overlapping in response coordinates but aligned to crossing or unrelated passage coordinates could be unioned into a span that is not itself a valid contiguous match. | Section 18: explicit `CopiedSpanCandidate{responseStart, passageStart, length}` model in both coordinate systems; `alignmentOffset = passageStart - responseStart`; maximality (`MAXIMAL_COPIED_SPAN`) and merging restricted to one shared alignment; deterministic sort-and-greedy-select algorithm for resolving response-coordinate overlap across different alignments, with an explicit response-overlap test and tie-break rules. Scenarios W, X, Y. | RESOLVED |

Both rows are `RESOLVED`. No row is `DEFERRED`; a `DEFERRED` row here would have required reporting `NOT READY — v1.3 REQUIRED` per this revision's task brief, which does not apply.

---

## 48. v1.2.2 → v1.2.3 Change Log

* Replaced the unqualified "otherwise → `UNKNOWN_SPELLING_TOKEN`" spelling classification with an exhaustive four-state machine (`IGNORED` / `ACCEPTED` / `MISSPELLED` / `UNKNOWN_SPELLING_TOKEN`) and a new deterministic Step 3 rule defining exactly when a token becomes `MISSPELLED` (SUM-002, Section 18, Scenarios T, U, V).
* Added the `spellingEngineVersion` policy field, the spelling-engine determinism requirement (exactly one distance-1 dictionary correction candidate), and the `HUMAN_ACCEPTED`/`HUMAN_CONFIRMED_MISSPELLED` append-only human-resolution event with its persisted audit fields (SUM-002, Sections 3, 5, 18).
* Replaced the single "any unresolved `UNKNOWN_SPELLING_TOKEN` affecting the threshold → `PENDING_HUMAN_MARK`" sentence with the explicit Case A/B/C rule, so escalation happens only when an unresolved unknown could still change an undetermined mark (SUM-002, Section 18, Scenarios U, V).
* Withdrew v1.2.2's unqualified "overlapping candidates are merged into the maximal span" instruction and replaced it with an explicit `CopiedSpanCandidate` model in both response and passage coordinates, an `alignmentOffset`-restricted maximality/merge rule, and a deterministic sort-and-greedy-select algorithm for overlapping candidates on different alignments (SUM-003, Section 18, Scenarios W, X, Y).
* Added the worked derivation of Codex's exact crossing-alignment counterexample (`Response = a b a b c d`, `Passage = a b c d a b a b`) directly in Section 18, showing the unique `copiedRunCount = 1` result (SUM-003, Section 18, Scenario W).
* Added `spellingEngineVersion` to the Policy Version Registry (Section 5) and to the fields persisted with every Section E spelling mark and human-resolution event (Section 18).
* Added one new Day 1 prerequisite (Section 34, item 13) requiring a frozen, versioned deterministic spelling engine and human-resolution persistence before Day 1 becomes `ACTIVE`.
* Added sixteen new acceptance criteria (43–58) and six new normative scenarios (T–Y), covering both findings.
* Added this Final Gate Finding Resolution Matrix (Section 47) and this change log (Section 48).

No other section's normative content was altered. MARK-002 and its governing Section 17 are unchanged and not reopened. All seven v1.2→v1.2.1 findings (CONF-001, ROUTE-001, MARK-001, SUM-001, CLAIM-001, REC-001, SEC-001) and their governing sections are unchanged and not reopened. Sections 1–2, 4, 6–17, 19–34 (aside from the single new prerequisite item), and 37–46 are carried forward from v1.2.2 unchanged in substance (cross-references to changed sections and terminology updated where required). The sentence-control criterion, content-point criterion, word-count/length handling, and shared sentence-boundary gate within Section 18 are unchanged in substance.

---

## 49. Final Self-Test (v1.2.3)

> Can one compliant implementation classify a non-accepted token as `UNKNOWN_SPELLING_TOKEN` while another automatically calls the same token `MISSPELLED`?

**NO.** Section 18's exhaustive four-state classification gives every token exactly one automatic outcome. A token reaches `MISSPELLED` only through the Step 3 exactly-one-distance-1-candidate rule; any token that does not satisfy that exact condition falls through to `UNKNOWN_SPELLING_TOKEN`, and no implementation may promote it to `MISSPELLED` without a persisted `HUMAN_CONFIRMED_MISSPELLED` event.

> Can two compliant implementations disagree about the `copiedRunCount` for crossing passage alignments?

**NO.** Section 18's `CopiedSpanCandidate` model, alignment-restricted maximality rule, and deterministic sort-and-greedy-select algorithm give one prescribed selection for any set of candidates, regardless of how many alignments cross or overlap in response coordinates.

> Does the exact Codex counterexample now produce one prescribed outcome?

**YES.** `Response = a b a b c d`, `Passage = a b c d a b a b` produces exactly `copiedRunSpans = [(0,4,4)]`, `copiedRunCount = 1`, paraphrase mark `1` — derived in full in Section 18 and restated as Scenario W.

All three answers match the required answers. The patch is complete.

---

## Recommendation

**READY FOR FINAL CODEX v1.2.3 PASS/FAIL GATE**

v1.2.3 path:

```text
v1.2.2 preserved unchanged: yes (not overwritten; this document is a new file)
v1.2.1 preserved unchanged: yes
v1.2 preserved unchanged:   yes
v1.1 preserved unchanged:   yes
```

SUM-002: **RESOLVED** (Section 47)
SUM-003: **RESOLVED** (Section 47)

Scenarios A–S: still pass — no scenario's inputs, rules, or expected outputs from Sections 1–17, 19–34 changed; Section 18's unchanged sub-rules (content points, sentence control, word count/length, shared punctuation gate) leave Scenarios A–N and Scenario S's normalization behaviour untouched, and Scenarios O, P, Q, R are restated exactly under the new classification/candidate model with the same expected outcomes.

Scenarios T–Y: pass (Section 36).

Previous gate fixes remain resolved: CONF-001, ROUTE-001, MARK-001, SUM-001, CLAIM-001, REC-001, SEC-001, MARK-002 — none reopened (Section 48).

Application code changed: none.
Migrations created: none.
Deployment performed: none.

Final recommendation: **READY FOR FINAL CODEX v1.2.3 PASS/FAIL GATE.** This is not authorization to implement Day 1. The sequence remains:

```text
v1.2.3
   ↓
Codex final PASS/FAIL gate
   ↓
PASS
   ↓
freeze specification
   ↓
implement Section 34 prerequisites
   ↓
Day 1 implementation
```
