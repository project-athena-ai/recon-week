# ATHENA ENGLISH DIAGNOSTIC SPEC v1.2

**Status:** Draft for Codex implementation-gate review
**Supersedes:** ATHENA ENGLISH DIAGNOSTIC SPEC v1.1
**Programme:** Athena — Six-Week War Room
**Subject:** Cambridge IGCSE First Language English (0500)
**Scope of this document:** Specification only. No application code, migrations, or Day 1 implementation is produced here.

**Provenance note (supersedes v1.1's provenance note):** v1.1 was written without access to the live Athena repository and said so. This revision was produced *with* full read access to `/home/david/src/github.com/athena/recon-week` (the frontend, a single `index.html`) and the live backend repositories at `/home/david/src/github.com/athena/backend` (the current Spring Boot "Chief Examiner" service — Flyway migrations `V1__init.sql`, `V2__marking_missions.sql`, and the `ai.athena.examiner` domain/service/web packages) and `/home/david/src/github.com/athena/athena-backend` (an earlier snapshot of the same service). Every claim in this document about what currently exists in Athena is grounded in those files, not assumed. Appendix C (Repository Reconciliation Matrix) records the concept-by-concept comparison. Where this spec's terminology does not yet exist in code, that is stated as a **missing concept**, not silently treated as already built.

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

Precise, non-conflatable definitions. Implementers must not substitute one term for another. New/changed terms since v1.1 are marked **[v1.2]**.

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
| **REGRESSED** **[v1.2, new]** | A recovery state reached from `PROVISIONALLY_RECOVERED`, `VERIFIED_RECOVERED`, or `IMPROVING` on defined negative evidence, distinct from — and never silently rewriting — the prior recovered history (Section 24). |
| **English Diagnostic Reviewer** **[v1.2, new role name]** | The human role that performs Section 13's mandatory human approval for English assessments. Deliberately **not** called "Chief Examiner" — see Section 26 for why that name is already taken by a different, existing Athena system. |

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
| `summaryMarkingPolicyVersion` | Section E content-point model, paraphrase/copying rule, length handling | Section 18 |
| `objectiveMarkingPolicyVersion` | Sections A–D question-type restrictions and scoring rules | Section 17 |
| `rootCauseTaxonomyVersion` | `failureType`/`primaryCause`/`contributingCauses` enums and tie-break rule | Section 20 |

**Hard rule:** every persisted claim, mark, routing decision, and confidence state records every policy-version field it depended upon. A later bump to any one policy version never retroactively reinterprets a historical record — historical records are read back exactly as they were computed, under the version active at computation time (Governing Principle 13). All fields above start at value `v1.2.0` except `alignmentProfileVersion`, which starts at `ATHENA-B1-READING-PROFILE-v1`.

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

Day 1 objective questions must use one of `SINGLE_SELECT`, `MULTI_SELECT_EXACT`, `SHORT_TEXT_CANONICAL`, or `NUMERIC`. Open-ended fuzzy objective marking is not authorised. Any question requiring semantic judgement belongs in the summary rubric and is not an objective question.

---

## 16. Marking States and Immutable Marks

Marks are immutable events bound to an attempt, assessment version, rubric version, marker result, and marking policy versions. A correction creates a superseding mark event; it never edits the historical event. A score is not posted as final while required human resolution is pending.

Objective scoring is server-side and deterministic. Summary scoring uses the Section 18 rubric and Section 19 uncertainty policy. A failed model call produces `PENDING_HUMAN_MARK`, never zero.

---

## 17. Objective Marking Contract

Normalization for objective matching is versioned and deterministic: Unicode-normalize, trim, collapse internal whitespace, and compare case-insensitively unless the question's frozen answer rule explicitly marks case as construct-relevant. Punctuation is ignored only when the frozen answer rule says it is non-construct-relevant.

Blank answers score zero. A single-select response scores one mark only for the frozen correct option. A multi-select response receives credit only when the selected set exactly equals the frozen accepted set; partial credit is not used unless the question explicitly contains independently scored subparts. Short-text responses match a frozen set of accepted normalized answers. Spelling variants are accepted only when listed in the frozen answer set or when the question explicitly declares spelling non-construct-relevant. No LLM decides equivalence for objective items.

An answer format or scoring rule cannot be changed after freezing. Ambiguous or missing answer-key entries block approval.

---

## 18. Section E Summary Marking Contract

The summary is marked out of 8:

| Criterion | Marks | Rule |
|---|---:|---|
| Required content points | 4 | Four frozen propositions, each independently 0/1. A point is credited when its proposition is accurate and present, whether expressed in the student's own words or copied. |
| Paraphrase | 2 | 2 marks when no copied run exists; 1 mark when exactly one copied run exists; 0 marks when two or more copied runs exist. |
| Sentence control and spelling | 2 | One mark for complete, grammatically interpretable sentences; one mark for no more than two spelling errors and punctuation that clearly separates sentences. |

For copying, tokenize lower-case Unicode words after punctuation removal. A copied run is four or more consecutive tokens appearing in the passage in the same order. Function words are included; repeated common words alone do not form a run unless the full four-token sequence matches. The maximum run count is used. This algorithm is applied to the submitted response and frozen passage.

Word count is whitespace-delimited tokens after trimming; punctuation attached to a token does not create another token, and a hyphenated form counts as one token. `IN_TARGET` is 80–100 words, `UNDER_TARGET` is below 80, and `OVER_TARGET` is above 100. No automatic marks are deducted solely for being under or over target. Counts below 60 or above 120 set `lengthWarning = true` and require human confirmation of content completeness, but the rubric marks remain criterion-based.

A response with no text scores 0 and is recorded as `SUBMITTED_NO_RESPONSE`. A marker must cite spans for every content point and language decision. If a criterion cannot be resolved from the rubric, the criterion becomes `PENDING_HUMAN_MARK`; no model retry may invent a score. Section E never participates in reading routing. When final, it may create only `PRELIMINARY_WRITING_EVIDENCE` with LOW evidence confidence for Day 1.

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

Relation precedence for one new evidence event is `SUPERSEDES`, then `CONTRADICTS`, then `WEAKENS`, then `SUPPORTS`; only the highest applicable relation is recorded. Contradictory active claims remain visible as a conflict until the supersession rule is met.

---

## 22. Evidence Confidence Policy

All values are exactly `UNKNOWN`, `LOW`, `MEDIUM`, or `HIGH`.

An independent evidence unit is one scored skill opportunity from one non-invalidated submitted attempt on one passage. All questions for that skill on that passage and attempt collapse into one unit; they cannot inflate quantity, diversity, or consistency. A single attempt may generate one unit for each separately declared skill, but those units are not independent sittings.

**Quantity:** no valid unit = UNKNOWN; 1 unit = LOW; 2–3 = MEDIUM; 4+ = HIGH.

**Diversity:** no valid unit = UNKNOWN; one passage or one sitting = LOW; at least two distinct passages spanning at least two genre/topic tags = MEDIUM; at least four distinct passages spanning at least two genre/topic tags and at least two sittings = HIGH. Question count never changes this result.

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

The skill-vector override is evaluated before the band route using exact rational comparison. For each skill, if `skillRaw / skillMax` is more than 20 percentage points below `readingRaw / 26`, route to `TARGETED_REMEDIATION` for every tied weakest skill, ordered A, B, C, D. A B2-eligible score that fails a gate but triggers no override routes to `SECOND_B1_DIAGNOSTIC`; this closes the otherwise unreachable 85%+ gate-failure case.

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
* `PROVISIONALLY_RECOVERED`: one fresh-transfer Level 3c+ unit is at least 70%.
* `VERIFIED_RECOVERED`: two successful units from distinct passages, including at least one Level 5+ unit. A later independent sitting may satisfy the second-unit requirement but is not required when the two distinct passages already satisfy this rule. Each unit must be at least 70%.
* `REGRESSED`: after provisional or verified recovery, a valid Level 3c+ or Level 7 unit is below 70%. A result below 50% is additionally `MAJOR_REGRESSION`.

Every transition creates an immutable recovery claim linked to the preceding claim. A failed later test never deletes successful recovery evidence. The existing Mission `PROPOSED → ACCEPTED → VERIFIED` lifecycle is operational workflow only; it cannot set `PROVISIONALLY_RECOVERED`, `VERIFIED_RECOVERED`, or `REGRESSED`.

---

## 25. Digital Twin Integration

Evidence Claims are the sole source of truth. The existing `twin_snapshot` table is a rebuildable materialised projection only: it stores a generated snapshot and reason, but it is not a source from which claims may be inferred and it must not be directly used to author a new student belief.

The projection is rebuilt from immutable claims, relation records, marks, exposure/attempt records, and policy versions. It includes the active claim set, current evidence confidence, evidence count/diversity, trend, open defects, recovery state, and separate CEFR-aligned, Athena Cambridge-style, and authentic Cambridge streams. Rebuilding the same claim history under the same policy versions must produce the same projection.

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

No directly discoverable repository contains a Student Digital Twin schema, English assessment model, `AssessmentAttempt`, `EvidenceClaim`, `SyllabusVersion`, or server-verified student identity. These are new concepts and are listed as prerequisites, not hidden implementation assumptions.

---

## 27. Security and Identity Boundary

Protected assessment content may be delivered only when the backend has verified:

1. an authenticated principal;
2. a server-resolved student identity or authorised reviewer identity;
3. an active attempt bound to that identity and assessment version;
4. permission to receive the requested content.

The current frontend `localStorage` values (`athena_auth`, subject, and API settings) are UI state, not authentication. The current backend README states that server-side Google ID-token verification is not implemented. Therefore server-side identity verification and attempt authorization are **Day 1 prerequisites**.

Preview, reviewer, and student endpoints must be distinct. Rubrics, answer keys, prompts, calibration data, and hidden item metadata remain server-only. Server logs, error payloads, analytics, proxies, and browser-visible responses must redact protected scoring material. Submission endpoints must be idempotent by attempt and submission token; duplicate requests cannot create duplicate marks or exposure events.

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

---

## 36. Normative Scenarios A–H

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

---

## 37. Codex Hostile Finding Resolution Matrix

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
* Creating the English assessment, exposure, attempt, claim, recovery, and projection persistence.
* Producing the B1 profile asset and reviewer interface.
* Deciding the future adapter boundary to a Mark-Loss Ledger when that platform concept is formally specified.
* Implementing the rebuildable Digital Twin projection from claims.
* Selecting an official Cambridge source and recording its syllabus/provenance data for any later Level 7 work.

These are Day 1 prerequisites or later specifications. They do not permit a developer to substitute localStorage identity, existing Mission verification, arbitrary twin JSON, or model self-confidence.

---

## 41. Final Hostile Self-Review

The v1.2 contract now gives one normative result for the required scenarios: exposure is per student, confidence uses a complete policy, routing uses reachable raw marks, summary cannot alter reading routing, recovery can regress, and evidence claims are immutable and typed. The current repository mismatches are explicit and converted into prerequisites rather than silently reconciled.

The remaining implementation work is substantial, especially server identity and the new student-bound evidence store. That is not a defect in this specification provided implementation cannot start until Section 34 is satisfied. No current Athena code is represented as already meeting the v1.2 contract.

## Recommendation

**READY FOR FINAL CODEX IMPLEMENTATION-GATE REVIEW**

The document is ready for a separate PASS/FAIL implementation-gate review. It is not authorization to implement Day 1. Day 1 implementation may begin only after the next Codex review passes and the prerequisites in Section 34 are met.
