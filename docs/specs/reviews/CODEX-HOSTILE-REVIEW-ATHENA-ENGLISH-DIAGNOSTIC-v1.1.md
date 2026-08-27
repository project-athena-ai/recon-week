Pasted text(20260825-164901).txt
Document
TASK

Produce:

ATHENA ENGLISH DIAGNOSTIC SPEC v1.2

This is a specification revision only.

Do not implement Day 1.

Do not create application code.

Do not create migrations.

Do not modify backend/frontend behaviour.

The goal is to convert the remaining ambiguities identified by Codex into deterministic, implementation-ready rules.

AUTHORITATIVE INPUTS

Read both of these in full before editing:

Current specification
/home/david/src/github.com/athena/recon-week/docs/specs/ATHENA-ENGLISH-DIAGNOSTIC-SPEC-v1.1.md
Codex hostile review

Use the Codex hostile review output as mandatory review input.

If it has already been saved in the repository, locate and read the full file.

If multiple hostile-review files exist, identify the one whose review target is:

ATHENA-ENGLISH-DIAGNOSTIC-SPEC-v1.1.md

and whose verdict is:

NOT implementation-ready

Do not work from a summary of the Codex review.

Read the review in full.

REPOSITORY INSPECTION IS MANDATORY

Unlike v1.1, this revision must be reconciled against the live Athena repository.

Inspect:

/home/david/src/github.com/athena/recon-week

and any directly referenced Athena backend/repository available locally.

Determine the actual current implementation or nearest equivalent of:

Student Digital Twin;
Chief Examiner;
Mark-Loss Ledger;
Mission/recovery tracking;
student identity;
assessment-related concepts;
confidence fields;
evidence fields;
English page;
Six-Week War Room;
APIs;
persistence;
authentication/authorization;
existing LLM marking structures.

Do not pretend concepts already exist when they do not.

Do not silently redefine existing Athena terms.

Where v1.2 intentionally introduces a new architecture that differs from the current implementation, state that explicitly.

REVISION STRATEGY

Do not rewrite the specification from scratch.

Preserve the strongest parts of v1.1, especially:

Evidence Claims;
evidence hierarchy;
distinction between performance and confidence;
distinction between Marker Confidence and Evidence Confidence;
UNKNOWN over fabricated certainty;
immutable historical evidence;
transfer vs reattempt vs retention;
deterministic routing;
separate Authoring Agent / Review Agent / human approval;
CEFR-aligned evidence not being CEFR certification;
CEFR-aligned evidence not being converted mechanically into Cambridge 0500 grade predictions.

The purpose of v1.2 is to eliminate the remaining places where two competent implementations could still produce materially different student truth.

RELEASE GATE

Treat the following question as the governing test:

Can two independent engineering teams implement this specification and produce materially equivalent marks, Evidence Claims, confidence states, exposure states, recovery states, and routing decisions?

If the answer is not unequivocally yes, v1.2 is not ready.

MANDATORY CODEX FINDINGS TO RESOLVE

All findings below must be addressed explicitly.

Do not merely mention them in the change log.

Convert them into normative rules.

1. SEPARATE GLOBAL ASSESSMENT LIFECYCLE FROM STUDENT EXPOSURE

v1.1 incorrectly mixes:

global assessment lifecycle; and
whether one student has already seen the material.

Create two distinct concepts.

A likely model is:

AssessmentVersion

with a global lifecycle such as:

DRAFT
→ AUTOMATED_CHECKED
→ AI_REVIEWED
→ HUMAN_APPROVED
→ FROZEN
→ ACTIVE
→ GLOBALLY_RETIRED

and a separate student-specific concept such as:

StudentAssessmentExposure

Do not blindly copy these names if the actual Athena model suggests better canonical terminology.

Define unseen precisely

Use a normative rule equivalent to:

A student ceases to have UNSEEN status at the first successful server response that delivers any protected assessment content to that student's authorised session.

Protected content includes at minimum:

passage;
question content.

Assignment, scheduling and attempt creation alone must not count as exposure if no protected content has been delivered.

Define exact behaviour for:

content delivery;
rendering;
browser crash;
network retry;
abandonment;
accidental exposure;
invalidated attempt;
resume;
restart;
repeat attempt;
teacher/admin preview;
two students sharing the same globally ACTIVE assessment.

A crash or abandonment must never restore UNSEEN.

A repeated attempt must never become unseen again.

Required state model

Specify the smallest practical student exposure/attempt model.

Potential states may include concepts like:

UNSEEN
ASSIGNED
EXPOSED
IN_PROGRESS
SUBMITTED
INVALIDATED

but choose and define them carefully.

Avoid redundant states.

2. DEFINE A COMPLETE EVIDENCE CONFIDENCE POLICY

v1.1 explicitly deferred this to implementation.

That is no longer allowed.

The normative specification must define:

Evidence Quantity;
Evidence Diversity;
Evidence Consistency;
Marker Confidence;
Overall Evidence Confidence.

All inputs and the final value must use one consistent vocabulary.

Prefer:

UNKNOWN
LOW
MEDIUM
HIGH

Do not use undefined values such as LOW-MEDIUM or MODERATE.

Independent evidence unit

Define exactly what counts as an independent evidence unit.

The Codex review proposed:

one scored skill opportunity from one assessment attempt on one passage.

Critique and refine this.

Questions within one passage must not be treated as independent passages/sittings simply to inflate confidence.

Suggested starting rules

Critically assess something approximately like:

Quantity
UNKNOWN = no evidence
LOW     = 1 independent evidence unit
MEDIUM  = 2–3 independent units
HIGH    = 4+ independent units
Diversity

At minimum, diversity must consider:

distinct passages;
genre/topic;
distinct sittings.

A single passage or single sitting must always remain LOW.

Consistency

Must be calculated across independent evidence units, not item-level binary variance inside one passage.

A possible starting rule:

UNKNOWN = fewer than 3 independent evidence units

LOW =
range of independent-unit skill percentages > 40 percentage points

MEDIUM =
range > 20 and <= 40 percentage points

HIGH =
range <= 20 percentage points
AND at least 4 independent evidence units

Critique these thresholds and choose deterministic v1.2 rules.

Overall Evidence Confidence

Provide an exact algorithm or lookup table.

For example, evaluate:

if no scored evidence:
    UNKNOWN

if fewer than 3 independent units:
    LOW

otherwise:
    overall =
      minimum(
        quantity,
        diversity,
        consistency,
        marker_confidence
      )

with deterministic items treated as Marker Confidence HIGH.

Do not accept this automatically.

Choose the smallest defensible deterministic policy and specify it completely.

Record:

evidenceConfidencePolicyVersion

for every derived confidence state.

Historical claims must retain the policy version originally used.

3. REDEFINE MARKER CONFIDENCE

Do not allow an LLM's arbitrary self-reported decimal such as:

0.87

to directly determine acceptance, routing, Evidence Confidence, or Digital Twin state.

Replace or constrain this mechanism.

Strongly consider structured marker signals:

evidenceSupport:
  COMPLETE
  PARTIAL
  WEAK

rubricFit:
  CLEAR
  AMBIGUOUS

alternativeMark:
  NONE
  PLAUSIBLE

evidenceSpanValid:
  TRUE
  FALSE

and derive operational Marker Confidence deterministically.

For example, evaluate a rule like:

HIGH:
  COMPLETE
  + CLEAR
  + NONE
  + TRUE
  + no unresolved marker disagreement

LOW:
  WEAK
  OR FALSE
  OR unresolved score disagreement

MEDIUM:
  every other valid combination

Critique and refine this.

If the model still returns a numeric self-confidence value, it must be stored only as:

advisoryModelConfidence

or equivalent.

It must not drive student truth unless there is a separately defined calibration procedure.

Define marker disagreement precisely, including partial-mark differences.

Define:

agreement;
minor disagreement;
material disagreement;
escalation;
human resolution.
4. ADD A VERSIONED B1 ALIGNMENT PROFILE

The term:

B1-aligned

must have a normative design anchor.

Introduce something equivalent to:

ATHENA-B1-READING-PROFILE-v1

This must not pretend to be official CEFR certification.

It must be an Athena internal assessment-design profile informed by documented CEFR sources.

Define its provenance and version.

The profile should operationalise dimensions such as:

lexical frequency/range;
grammatical complexity;
sentence structure;
discourse organisation;
topic familiarity;
specialist-knowledge dependency;
information density;
explicit-information demand;
inferential demand;
abstraction;
rhetorical complexity;
question/task complexity.

Avoid fake precision.

Use rating bands where appropriate.

Critical failure conditions

Define conditions that automatically prevent B1-aligned approval, such as:

specialist knowledge required;
excessive unexplained technical vocabulary;
ambiguity affecting answerability;
inferential demand materially above profile;
question task requiring higher-order language analysis not intended for Day 1.
Alignment decision rule

Define exactly how human approval determines:

ALIGNED
ALIGNED_WITH_WARNINGS
NOT_ALIGNED

or another minimal controlled vocabulary.

Every active assessment must store:

alignmentProfileVersion

and the final human alignment decision.

5. REWRITE DAY 1 ROUTING USING RAW MARKS

Codex correctly identified that the existing percentage bands create gaps and unreachable cases.

For Day 1, raw marks should be authoritative.

Percentages may be display-only.

Strongly consider removing the summary from the reading-level routing decision entirely.

The objective reading sections are:

A Explicit comprehension = 8
B Vocabulary             = 6
C Inference              = 8
D Main idea / purpose    = 4

Total                    = 26

The summary may remain useful preliminary writing evidence but should not destabilise reading placement.

Critically evaluate this proposed routing policy
23–26 / 26
→ eligible for B2 boundary probe

19–22 / 26
→ second independent B1 diagnostic

13–18 / 26
→ B1 instruction + targeted remediation

0–12 / 26
→ A2/B1 boundary probe

with B2 eligibility gates:

Explicit comprehension >= 7/8
Vocabulary             >= 5/6
Inference              >= 6/8

If overall is 23–26 but a gate fails:

→ second independent B1 diagnostic

unless a clearly defined remediation override applies.

Do not preserve the old 20-percentage-point override merely because it exists.

Determine whether it is still useful when routing is expressed in raw marks.

If it is retained, define it exactly.

If not necessary, remove it.

Multiple weak skills

Specify deterministic rules for:

one weakest skill;
multiple tied weak skills;
remediation order;
stable ordering.
Routing immutability

Once the student has started the next assignment:

the original routing decision must not mutate retroactively.

Later evidence may create:

SUPERSEDING_RECOMMENDATION

or equivalent for future assignments.

Define this cleanly.

6. FIX SMALL-SAMPLE SCORE SEMANTICS

Make raw marks normative.

Percentages are presentation values.

Explicitly document possible raw outcomes where important.

Do not base thresholds on impossible values such as:

Vocabulary >= 75%

when the actual marks jump from 4/6 to 5/6.

7. DEFINE SUMMARY MARKING COMPLETELY ENOUGH FOR DAY 1

Codex found that the summary marking contract is incomplete.

Define, for Day 1:

exact task purpose;
maximum marks;
content-point handling;
acceptable paraphrase;
copied wording;
word-count rule;
under-length behaviour;
over-length behaviour;
language/grammar treatment;
spelling tolerance;
whether language quality affects marks;
whether the summary mark contributes to reading routing;
how low-confidence marking is escalated.

Keep it appropriate to an 8-mark, preliminary Day 1 summary.

Do not accidentally create a full Cambridge 0500 summary rubric unless intended.

8. DEFINE OBJECTIVE MARKING RULES

The specification must define enough for two teams to score the same answer equivalently.

Cover where applicable:

case sensitivity;
whitespace;
punctuation;
spelling;
equivalent wording;
synonyms;
multiple answers;
blank answers;
partial credit;
multi-select questions;
free-text short answers.

If Day 1 can avoid ambiguous machine-scored short answers by using controlled question types, say so explicitly.

Prefer simpler deterministic assessment design over complicated answer normalisation.

9. OPERATIONALISE EVIDENCE CLAIM TYPES

The current generic Evidence Claim semantics are too loose.

Define a minimal controlled claimType vocabulary.

Do not over-model.

Potential types to evaluate:

SKILL_PERFORMANCE
WEAKNESS
RECOVERY
ROOT_CAUSE
WORKING_INSTRUCTIONAL_LEVEL

Only keep types that are required.

Define what each claim means.

Define claim relations

Make these normative:

SUPPORTS
WEAKENS
CONTRADICTS
SUPERSEDES

Specify when each relation applies.

Example:

Existing claim:

WEAKNESS:
Inference
OBSERVED

New fresh-transfer result:

4/4

The spec must say exactly what happens.

Do not allow arbitrary choice between:

weakening old claim;
contradiction;
supersession;
improvement claim.

Define whether Digital Twin current state is:

derived from Evidence Claims; or
independently stored.

Prefer one source of truth.

If a derived snapshot exists, specify that it is rebuildable from claims/evidence.

10. DEFINE RECOVERY INCLUDING REGRESSION

The existing one-way states are insufficient.

Current progression:

SUSPECTED
→ OBSERVED
→ IMPROVING
→ PROVISIONALLY_RECOVERED
→ VERIFIED_RECOVERED

Add deterministic regression/reopening semantics.

Evaluate whether the correct model is:

REGRESSED

or:

REOPENED

or a reopening transition back to OBSERVED.

Do not add unnecessary states.

Required scenario

Specify exact behaviour for:

inference weakness observed;
near-transfer success;
fresh-transfer success;
new-genre success one week later;
major failure on authentic Cambridge 0500 inference task.

Define:

state after every event;
new Evidence Claims;
what historical recovery evidence remains;
what current Digital Twin state becomes.

Recovery history must never be deleted.

Also decide the scope of recovery state:

skill;
defect;
root-cause case;
Evidence Claim.

Choose one canonical scope.

11. OPERATIONALISE ROOT-CAUSE CLASSIFICATION

The current taxonomy mixes:

construct failures;
execution failures;
access/language failures.

Fix this.

Strongly consider typed fields rather than one flat enum.

For example:

failureType:
  CONSTRUCT
  ACCESS
  EXECUTION

primaryCause:
  controlled enum

contributingCauses:
  controlled enum[]

Do not copy this blindly.

Define the smallest taxonomy that improves reproducibility.

For each root-cause category, define:

meaning;
minimum evidence;
example;
counterexample.

Specify the classification unit.

Prefer:

one incorrect response / lost-mark event

or another explicit unit.

If multiple plausible causes remain and no primary is supported strongly enough:

primaryCause = UNKNOWN

with contributing candidates stored if useful.

Do not force an arbitrary winner.

12. FIX ASSESSMENT AUTHORING / REVIEW LOOPBACKS

The lifecycle cannot remain purely linear.

Define:

rejection;
return for revision;
re-analysis after changes;
re-review after substantive changes.

Example:

AI_REVIEWED
→ human changes Question 7

The system must define whether that returns to:

DRAFT

or:

AUTOMATED_CHECKED

or another appropriate state.

Classify edits

Define:

Non-substantive edit

Examples:

typo not affecting meaning;
formatting.
Substantive edit

Examples:

passage wording;
question stem;
distractor;
answer key;
rubric;
alignment field.

Substantive changes must trigger the necessary deterministic and AI-review stages again.

Define this exactly.

13. STRENGTHEN HUMAN APPROVAL SEMANTICS

Define what HUMAN_APPROVED certifies.

The human must approve at minimum:

passage;
questions;
answer key;
rubric;
alignment result;
unresolved AI-review findings.

Define AI-review finding severity, for example:

BLOCKING
WARNING
ADVISORY

If an override of a warning is allowed, record:

reviewer;
timestamp;
reason.

Blocking findings must not be overridable without revising the assessment.

Choose the smallest workable model.

14. REPLACE SINGLE PROVENANCE TAG WITH PROVENANCE HISTORY

v1.1 currently implies one tag such as:

AUTHORED
COMPUTED
AI_REVIEWED
HUMAN_APPROVED

Codex correctly notes that this loses history.

Replace it with an auditable history/event model.

A final field may have:

authored value
AI-reviewed proposal
human-approved value

without silently overwriting earlier states.

Define the conceptual provenance record.

Do not over-design storage schema, but specify history requirements.

15. STRENGTHEN ASSESSMENT ACCESS SECURITY

The current API contract is insufficient.

Define that a student may receive protected assessment content only through an authorised attempt.

Conceptually require a binding equivalent to:

student
+
assignment
+
assessmentVersion
+
active authorised attempt

Do not expose arbitrary future assessments through predictable IDs.

Specify:

authentication requirement;
authorization;
attempt ownership;
idempotent content delivery;
resume semantics;
no future-assessment enumeration;
logging/redaction requirements for protected assessment content;
error responses that do not leak answer keys/rubrics.

You do not need to design the auth implementation.

But the security boundary must be implementable.

Also reconcile this with the repository finding that server-side Google verification may not currently exist.

If the current repository security is insufficient for protected diagnostics, state that as a Day 1 implementation prerequisite rather than pretending the assessment layer alone can solve it.

16. RECONCILE THE DIGITAL TWIN WITH ACTUAL ATHENA

Codex found that the existing Digital Twin is effectively:

twin_snapshot

with arbitrary JSON and no formal Evidence Claim model.

v1.2 must not claim this architecture already exists.

Define an integration strategy.

Prefer a minimal approach:

Evidence Claims become the authoritative English diagnostic evidence;
a Digital Twin snapshot may be a derived/materialised view;
raw claims/evidence remain immutable source of truth;
snapshot can be rebuilt.

State whether twin_snapshot can temporarily carry the derived English view or whether new persistence is required.

Do not write migrations.

Do specify the intended authority boundary.

17. RECONCILE EXISTING RECOVERY/MISSION SEMANTICS

Codex found current Mission behaviour:

PROPOSED
→ ACCEPTED
→ VERIFIED

based in part on same-question reattempt behaviour.

This conflicts with the new rule:

Same-question success is not transfer and cannot verify recovery.

Do not silently reuse VERIFIED for both meanings.

Define whether:

existing Mission verification remains a workflow status; and
English VERIFIED_RECOVERED remains a separate evidence status.

If terminology would be dangerously confusing, rename one side in the spec.

State the integration rule explicitly.

18. CAMBRIDGE EVIDENCE TERMINOLOGY

Codex found that:

authentic Cambridge 0500 material; and
Cambridge-style material

are currently mixed.

Separate them.

Define:

AUTHENTIC_CAMBRIDGE_0500

for official Cambridge past-paper/source material.

Define something separate such as:

ATHENA_CAMBRIDGE_STYLE

for Athena-created examination-like tasks.

Only authentic material should occupy the highest evidence-hierarchy category if that is the intended rule.

19. RETENTION TIMING

"Later independent sitting" is too vague.

Define a minimum elapsed-time threshold for retention evidence.

For the six-week programme, choose a pragmatic initial value.

Example:

>= 72 hours

or another justified value.

Do not pretend it is psychometrically validated.

Call it a six-week intervention-policy threshold and version it if necessary.

20. SYLLABUS VERSION SELECTION

Define what happens if an assessment is authored when a syllabus is:

UNDER_REVIEW

An ACTIVE assessment must bind to one exact syllabus version.

Do not allow historical evidence to depend on a mutable "current" pointer.

21. CROSS-REFERENCE AND TERMINOLOGY REPAIR

Perform a complete audit of v1.1.

Correct:

broken section numbers;
stale references;
undefined MODERATE;
LOW-MEDIUM usage;
inconsistent confidence representation;
entity names;
state names;
incorrect "unchanged from v1" claims where the repository has no equivalent.

No normative cross-reference may remain broken.

22. DAY 1 MINIMUM IMPLEMENTATION BOUNDARY

After resolving the architecture, clearly identify what is actually required before Day 1 can be built.

Separate:

BLOCKER BEFORE DAY 1

Only things necessary for valid Day 1 evidence.

Likely examples:

student identity/auth boundary;
assessment version;
assessment assignment/exposure;
attempt;
passage/questions;
deterministic marking;
summary marker;
B1 profile;
Evidence Claim;
routing policy;
minimal Digital Twin integration;
human approval.
CAN DEFER

Examples might include:

rich reviewer UI;
generalized multi-student scheduling;
sophisticated trend visualisation;
general psychometric calibration;
large assessment-bank management.

Keep v1.2 ruthlessly focused on six-week delivery.

23. REQUIRED SCENARIOS

v1.2 must include or fully answer these normative scenarios.

Scenario A — Uneven high performer
Explicit    8/8
Vocabulary  6/6
Inference   5/8
Purpose     4/4
Summary     7/8

State exact reading route.

Scenario B — Summary marking failure

Objective sections complete and strong.

Summary marking fails.

State:

reading route;
summary state;
Digital Twin writes;
later human mark behaviour.
Scenario C — Exposure without submission

Passage delivered.

Browser crashes.

Student returns next day.

State:

exposure status;
attempt state;
resume/restart behaviour;
evidence validity.
Scenario D — Ambiguous root cause

Wrong inference answer plausibly caused by vocabulary or inference.

State exact stored classification.

Scenario E — Recovery then relapse

Sequence:

fail diagnostic
pass near transfer
pass fresh transfer
pass later new genre
fail authentic Cambridge task

State every recovery transition.

Scenario F — Human edit after AI review

Human substantively changes one question and answer key after AI review.

State exact lifecycle transition.

Scenario G — Two students

Gerald has seen an assessment.

Melusi has not.

State global version status and each student's exposure status.

Scenario H — Score granularity

Show that all routing rules operate on reachable raw marks.

24. REQUIRED ACCEPTANCE CRITERIA

Add testable criteria covering at minimum:

Student exposure is independent from global assessment lifecycle.
A successful protected-content delivery permanently ends unseen status for that student/version.
Browser crash/abandonment never restores unseen status.
Evidence Confidence is deterministic under a fixed policy version.
A single passage cannot produce MEDIUM/HIGH diversity.
A single passage cannot produce HIGH consistency.
Model self-confidence cannot directly drive Marker Confidence.
Raw marks, not impossible percentage thresholds, drive Day 1 routing.
Summary marking failure does not make reading routing nondeterministic.
Assessment assignment already started cannot be retroactively changed by later summary marking.
VERIFIED_RECOVERED can be reopened/regressed under defined evidence.
Same-question reattempt cannot verify recovery.
Every B1-aligned assessment references one exact alignment-profile version.
Human substantive edits trigger appropriate re-check/re-review.
Protected assessment content requires authorised student/attempt binding.
Evidence Claim state is derivable deterministically from its inputs/policy.
Digital Twin snapshots can be traced back to immutable Evidence Claims.
Historic claims retain original policy/model/profile versions.
Authentic Cambridge and Athena-created Cambridge-style evidence remain distinct.
No undefined confidence state such as LOW-MEDIUM or MODERATE appears.
25. FINAL SELF-ATTACK

After writing v1.2, conduct another hostile pass against your own revision.

For every major system decision ask:

Can two reasonable implementations still produce materially different results?

Try specifically to break:

exposure;
routing;
confidence;
claim transitions;
recovery regression;
root-cause assignment;
human review loopbacks;
summary marking;
objective marking;
Digital Twin writes.

If you discover an ambiguity, fix it before finishing.

Do not merely list it as an open question if it affects Day 1 truth.

26. REQUIRED DELIVERABLES

Produce:

1. Complete standalone specification
ATHENA ENGLISH DIAGNOSTIC SPEC v1.2

It must supersede v1.1.

2. v1.1 → v1.2 Change Log

Map every substantive change.

3. Codex Finding Resolution Matrix

For every Codex hostile-review finding:

Finding
Severity
v1.1 defect
v1.2 resolution
Section
Status

Status must be:

RESOLVED
DEFERRED_WITHOUT_DAY1_IMPACT
UNRESOLVED_BLOCKER
4. Repository Reconciliation Matrix

Show how the specification now relates to actual Athena implementation.

5. Day 1 Implementation Prerequisites

A concise ordered list.

6. Remaining Deferred Work

Only work that does not affect Day 1 evidence integrity.

7. Final recommendation

Exactly one:

READY FOR FINAL CODEX IMPLEMENTATION-GATE REVIEW

or

NOT READY FOR IMPLEMENTATION

Do not recommend actual Day 1 coding yet.

The next step after v1.2 should be a short Codex PASS/FAIL implementation-gate review, not immediate coding.

FINAL GOVERNING PRINCIPLE

Use this as the final test for every design decision:

Athena must never convert ambiguity in its own architecture into false certainty about a student.

And for this six-week programme:

Prefer the smallest deterministic, auditable mechanism that preserves evidence integrity over a sophisticated mechanism that cannot be trusted.

Today 5:39 AM
Pasted text(20260826-033947).txt
Document

This Codex review confirms the right next step is v1.2 before any Day 1 implementation. Codex’s conclusion is unambiguous: the current spec still allows different compliant implementations to produce different marks, confidence, routing, recovery and Digital Twin state.

The strongest blockers are exactly the ones the v1.2 prompt should force Claude to resolve: separate global assessment lifecycle from per-student exposure; define unseen status precisely; make Evidence Confidence fully deterministic; define consistency across independent evidence units; replace LLM self-reported numeric confidence; create a versioned ATHENA-B1-READING-PROFILE-v1; rewrite routing around raw marks; formalize Evidence Claim transitions; add regression/reopening; and operationalize root-cause classification.

I especially agree with Codex’s proposed Day 1 routing simplification: use Sections A–D only, 26 marks total, with raw-mark thresholds, and keep the summary as preliminary writing evidence rather than letting uncertain summary marking distort reading placement.

The remaining Evidence Claim/recovery/root-cause issues are also real architecture questions, not wording polish. Codex shows that the current spec still permits multiple interpretations of whether new evidence weakens, contradicts or supersedes a claim, and it has no deterministic path for regression after a previously “verified” recovery.

One practical step before running Claude: save this hostile review into the repo, for example:

/home/david/src/github.com/athena/recon-week/docs/specs/reviews/CODEX-HOSTILE-REVIEW-ATHENA-ENGLISH-DIAGNOSTIC-v1.1.md

Then point Claude’s v1.2 task at both the authoritative v1.1 spec and that exact review file. That removes any ambiguity about which review Claude is expected to resolve.

The target after Claude finishes should be:

v1.2 → short Codex PASS/FAIL implementation gate → only then Day 1 build.

should i have over to Claude or claude code

Use Claude Code, not ordinary Claude chat, for the v1.2 handoff.

The reason is practical: v1.2 now depends on the actual Athena repository state, not just the text of the specification. Codex found real mismatches between the spec and implementation—for example, the current Digital Twin, recovery semantics, auth boundary, and missing English assessment models. Claude Code can inspect /home/david/src/github.com/athena/recon-week, read the live files, compare them with the spec, and produce a repository-aware v1.2.

Ordinary Claude would be fine if you only wanted a document rewrite from pasted text. But here the task explicitly requires repository reconciliation. So the handoff I recommend is:

Claude Code writes v1.2. Codex performs the final hostile PASS/FAIL gate.

Also give Claude Code the exact two files:

/home/david/src/github.com/athena/recon-week/docs/specs/ATHENA-ENGLISH-DIAGNOSTIC-SPEC-v1.1.md

and, after you save it:

/home/david/src/github.com/athena/recon-week/docs/specs/reviews/CODEX-HOSTILE-REVIEW-ATHENA-ENGLISH-DIAGNOSTIC-v1.1.md

That way Claude Code can reconcile the spec directly against the live Athena architecture rather than treating the review as abstract prose.

Hostile specification review

Verdict: NOT implementation-ready.

Two competent teams can currently produce materially different marks, evidence confidence, routing, recovery state, and Digital Twin state while both claiming compliance.

No implementation files were changed.

Normative target reviewed in full: docs/specs/ATHENA-ENGLISH-DIAGNOSTIC-SPEC-v1.1.md.

Repository reconciliation

Spec concept Actual Athena concept Classification
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━ ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Student Digital Twin twin_snapshot containing arbitrary JSON, with no student_id, skill schema, Architectural contradiction
evidence claims, or confidence model
─────────────────────────────────── ───────────────────────────────────────────────────────────────────────────────── ───────────────────────────────────────────────────────
Chief Examiner Backend reads handwritten scripts, segments answers, marks mathematics scripts, Partial match; terminology conflict
generates one mission
─────────────────────────────────── ───────────────────────────────────────────────────────────────────────────────── ───────────────────────────────────────────────────────
Mark-Loss Ledger question_mark and mission tables; no Mark-Loss Ledger entity or stable ledger Partial match
semantics
─────────────────────────────────── ───────────────────────────────────────────────────────────────────────────────── ───────────────────────────────────────────────────────
Recovery tracking Mission PROPOSED → ACCEPTED → VERIFIED, based on same-question reattempt Duplicate concept with materially different semantics
─────────────────────────────────── ───────────────────────────────────────────────────────────────────────────────── ───────────────────────────────────────────────────────
Assessment / attempts / responses No English assessment, diagnostic attempt, or student response model exists Missing implementation concept
─────────────────────────────────── ───────────────────────────────────────────────────────────────────────────────── ───────────────────────────────────────────────────────
Evidence confidence Page/segment numeric model self-estimates confidence from the LLM Terminology conflict; architectural contradiction
─────────────────────────────────── ───────────────────────────────────────────────────────────────────────────────── ───────────────────────────────────────────────────────
Student/user/role model Frontend stores Google auth state in localStorage; backend README explicitly Missing security boundary
says server-side Google verification is not implemented
─────────────────────────────────── ───────────────────────────────────────────────────────────────────────────────── ───────────────────────────────────────────────────────
Six-Week War Room Static frontend page and localStorage sprint board Partial match
─────────────────────────────────── ───────────────────────────────────────────────────────────────────────────────── ───────────────────────────────────────────────────────
English page Static British Council resource links and local content; no diagnostic Partial match
functionality
─────────────────────────────────── ───────────────────────────────────────────────────────────────────────────────── ───────────────────────────────────────────────────────
Syllabus/version model No SyllabusVersion entity found Missing implementation concept
─────────────────────────────────── ───────────────────────────────────────────────────────────────────────────────── ───────────────────────────────────────────────────────
Assessment API Existing APIs are /api/scripts, transcript, marking, and mission endpoints Missing English assessment API

The v1.1 provenance note says the live repository was unavailable, but it was discoverable. That is itself a specification defect: the integration contract was written against
assumed concepts rather than the actual platform.

Release blockers
1. Assessment lifecycle still conflates global lifecycle and student exposure

Section 12 defines one lifecycle with:

ACTIVE → RETIRED (per-student)
ACTIVE → RETIRED (global)

An AssessmentVersion cannot have a per-student state without either:

duplicating the assessment version per student; or
storing the state somewhere else.

The Gerald/Melusi case therefore has two compliant interpretations.

Interpretation A

Gerald submits the assessment, so the AssessmentVersion becomes RETIRED. Melusi receives a new version or cannot receive the scheduled assessment.

Interpretation B

The assessment remains globally ACTIVE; a separate per-student record prevents reuse for Gerald but permits Melusi to sit it.

Both comply with the current wording. The consequences affect unseen status, scheduling, exposure, and evidence hierarchy.

The wording is also internally inconsistent:

Section 12 says retirement occurs on submission.
Section 23 says retirement occurs once attempted.
The hierarchy requires “unseen” status.
No rule defines whether API fetch, rendering, passage delivery, or first answer establishes exposure.

Required model:

AssessmentVersion
lifecycle: DRAFT → AUTOMATED_CHECKED → AI_REVIEWED
→ HUMAN_APPROVED → FROZEN → ACTIVE → GLOBALLY_RETIRED

StudentAssessmentExposure
student_id
assessment_version_id
first_exposed_at
exposure_event
status

Recommended exposure rule:

A student ceases to have unseen status at the first successful server response that delivers any assessment content to that student, including the passage or a question.
Assignment, scheduling, or attempt creation alone does not expose content. A browser crash after delivery, abandonment, invalidation, or absence of submission does not
reverse exposure. A teacher preview or demonstration is not student exposure unless delivered through the student identity.

Repeat attempts must always reference the existing exposure and can never regain unseen status.

2. Evidence Confidence is explicitly deferred despite affecting student truth

Section 18 says the final lookup table is to be “finalised during implementation” while Section 29 claims deterministic behaviour.

Two compliant policies:

Policy A

Overall = minimum(quantity, diversity, consistency, marker_confidence)

A single-passage diagnostic produces LOW.

Policy B

Overall = MEDIUM when quantity and consistency are MEDIUM,
even if diversity is LOW

The worked example itself reports LOW-MEDIUM, which is not one of the defined values.

Both policies satisfy the current wording and produce different Digital Twin states. This is a blocker, not implementation detail.

A minimal deterministic policy should be normative:

An independent evidence unit is one scored skill opportunity from one assessment attempt on one passage. Questions from the same passage are not independent attempts.
Quantity: LOW = 1, MEDIUM = 2–3, HIGH = 4+ independent units.
Diversity:
LOW: one passage or one sitting;
MEDIUM: at least two passages across at least two genres/topics;
HIGH: at least four passages, at least two genres/topics, and at least two sittings.
Consistency:
UNKNOWN: fewer than three independent units;
LOW: range of unit percentages greater than 40 percentage points;
MEDIUM: range greater than 20 and at most 40 points;
HIGH: range at most 20 points and at least four units.
Deterministic items have marker confidence HIGH.
If there is no scored evidence, overall confidence is UNKNOWN.
If there is one or two independent units, overall confidence is LOW.
Otherwise:

overall = minimum(quantity, diversity, consistency, marker_confidence)

This should be stored with evidenceConfidencePolicyVersion.

3. Evidence Consistency is not defined

The current “variance in per-item correctness” rule permits materially different results.

Example A:

Passage 1: 4/4
Passage 2: 1/4
Passage 3: 4/4

Implementation A: calculate variance across all 12 item outcomes; consistency is LOW.

Implementation B: calculate passage-level percentages; consistency is LOW.

Implementation C: calculate the median passage score and treat the one poor passage as noise; consistency is MEDIUM.

All are compatible with the current text.

Example B:

One passage: 1, 0, 1, 0

This must not generate the same consistency evidence as three independent passages. The current specification does not define the unit.

The required minimum unit should be the independent assessment opportunity, not the question. A single passage must yield UNKNOWN consistency, not HIGH or MEDIUM.

4. LLM marker confidence has false probabilistic meaning

The specification permits:

marker_confidence = 0.87

but does not define whether this means:

calibrated probability that the mark is correct;
the model’s subjective certainty;
probability that the evidence span is sufficient;
confidence in the rubric interpretation.

Two identical marks can receive 0.97 and 0.61 from two model calls. A systematically overconfident model can pass every threshold.

The current repository already stores page and segment confidence as a model self-estimate:

confidence numeric(3,2) -- model self-estimate

That is not calibrated evidence.

Recommended replacement:

evidence_support: COMPLETE | PARTIAL | WEAK
rubric_fit: CLEAR | AMBIGUOUS
alternative_mark: NONE | PLAUSIBLE
evidence_span_valid: TRUE | FALSE

Deterministic derivation:

HIGH:
COMPLETE + CLEAR + NONE + TRUE
and no marker disagreement

LOW:
WEAK, FALSE, or unresolved score disagreement

MEDIUM:
every other valid result

The numeric value may be retained as an untrusted advisory field, but it must not drive acceptance, Evidence Confidence, routing, or Digital Twin state unless a separately
versioned calibration procedure exists.

Historical marks must retain the original marker result and policy version. Recalibration must not silently rewrite history.

5. “B1-aligned” remains subjective

Section 8 provides passage constraints and Section 11 provides a review checklist. Neither defines the normative artefact used to decide alignment.

Two reviewers can reasonably disagree because no scoring framework defines acceptable ranges for:

vocabulary;
lexical frequency;
grammar;
sentence complexity;
discourse organisation;
topic familiarity;
information density;
abstraction;
inference;
rhetorical complexity;
question complexity.

A readability score cannot solve this, and the specification correctly says so.

Required artefact:

ATHENA-B1-READING-PROFILE-v1

It must define observable dimensions, rating scales, critical-failure conditions, and the decision rule for alignment. It must reference CEFR provenance without claiming
official CEFR certification.

The assessment record must store the profile version and reviewer decisions, not only intendedCefrAlignment.

6. Routing has unreachable and contradictory cases

The routing rules do not define:

whether overall includes Section E;
section weighting;
denominator;
raw-score rounding;
handling of partial marks;
precedence between the gates and bands;
whether all weak skills are routed;
what happens after later summary marking.

Required divergent case:

overall = 86
explicit = 95
vocabulary = 95
inference = 69

This fails the B2 gate. It is not in the 70–84 band. It is not in the 50–69 band. It is not below 50. If the 20-point override uses the displayed values, it does not apply
because 86 − 69 = 17.

Therefore the current function has no route for this input.

Another contradiction:

overall = 88
summary = pending
objective-only overall = 92

Section 19 says missing sections produce INSUFFICIENT_DATA, but the next paragraph says provisional or pending summary marks permit objective-only routing. Both cannot be
authoritative.

Recommended Day 1 policy: exclude Section E from reading-level routing permanently. Use Sections A–D only:

overall = (A + B + C + D) / 26

Use exact raw-mark thresholds:

23–26 / 26 → eligible for B2 boundary probe, subject to gates
19–22 / 26 → second independent B1 diagnostic
13–18 / 26 → B1 remediation
0–12 / 26 → A2/B1 boundary probe

Skill gates should also use raw marks:

explicit comprehension: ≥ 7/8
vocabulary: ≥ 5/6
inference: ≥ 6/8

If overall is at least 23/26 but a gate fails:

apply the skill-vector override if the exact unrounded difference exceeds 20 percentage points;
otherwise route to the second independent B1 diagnostic.

Ties must route all tied weak skills in stable order.

Once the next assignment starts, the existing routing decision must not be mutated. Later marking can create a superseding recommendation for a future assignment, but cannot
retroactively alter the assignment already started.

7. Small-sample thresholds are not interpretable as written

The exact section mappings are:

A Explicit / 8:
0=0%, 1=12.5%, 2=25%, 3=37.5%, 4=50%,
5=62.5%, 6=75%, 7=87.5%, 8=100%

B Vocabulary / 6:
0=0%, 1=16.7%, 2=33.3%, 3=50%,
4=66.7%, 5=83.3%, 6=100%

C Inference / 8:
0=0%, 1=12.5%, 2=25%, 3=37.5%, 4=50%,
5=62.5%, 6=75%, 7=87.5%, 8=100%

D Purpose / 4:
0=0%, 1=25%, 2=50%, 3=75%, 4=100%

Consequences:

Vocabulary can never score 75%; it jumps from 66.7% to 83.3%.
Inference can never score 70%; it jumps from 62.5% to 75%.
Purpose is too small to support fine-grained percentage interpretation.
84.9% is not a possible raw score for a full 34-mark assessment.
Objective-only overall scores advance in increments of 1/26 = 3.846 percentage points.

Raw marks should be authoritative. Percentages should be display-only.

8. Evidence Claim semantics remain under-specified

The four relations are not operational:

SUPPORT
WEAKEN
CONTRADICT
SUPERSEDE

Example:

Existing claim: inference weakness, OBSERVED, MEDIUM
Fresh transfer: 4/4

Possible compliant outcomes:

weaken the weakness;
contradict the weakness;
create an improvement claim;
supersede the weakness;
leave the weakness open and create a conflicting claim.

The specification says “sufficient,” “materially,” and “typically” without thresholds.

claimType also has no enumeration. Simultaneous claims can produce logically inconsistent state, such as:

Inference weakness = OBSERVED
Inference recovery = VERIFIED_RECOVERED

Required rules must define:

claim type enumeration;
relation precedence;
whether claims are about performance, defect, root cause, or recovery;
when a claim becomes inactive;
how contradictory active claims are displayed;
whether Digital Twin state is derived from claims or stored independently.
9. Recovery state machine has no regression path

The current state machine only moves forward:

SUSPECTED
→ OBSERVED
→ IMPROVING
→ PROVISIONALLY_RECOVERED
→ VERIFIED_RECOVERED

The requested failure case is unresolved:

weakness observed;
near-transfer succeeds;
fresh-transfer succeeds;
later new-genre test succeeds;
authentic Cambridge inference fails badly.

Possible implementations:

remain VERIFIED_RECOVERED;
return to OBSERVED;
add REGRESSED;
lower Evidence Confidence but keep the state;
create a new defect.

All are currently defensible.

Recommended addition:

VERIFIED_RECOVERED → REGRESSED
PROVISIONALLY_RECOVERED → REGRESSED
IMPROVING → OBSERVED

A regression should create a new Evidence Claim linked to the prior recovery claim, reduce the current derived confidence according to policy, and never delete historical
recovery evidence.

The specification must also decide whether recovery state belongs to:

a skill dimension;
a root-cause defect;
an Evidence Claim;
or a separate recovery case.

Currently it is simultaneously used in all four contexts.

10. Root-cause classification is still subjective

The Primary + Contributing model improves the data shape but does not make classification reproducible.

Example:

A student misses an inference question because one key word was misunderstood.

Implementation A

Primary: VOCABULARY
Contributing: INFERENCE

Implementation B

Primary: INFERENCE
Contributing: VOCABULARY

The rule “direct evidence” does not resolve this. Nor do “roughly equally” and “comparable confidence.”

The taxonomy also mixes constructs and causes:

INFERENCE
VOCABULARY
EXPLICIT_COMPREHENSION
TIME_PRESSURE
CARELESS_EXECUTION
MISREAD

A missed inference can be caused by vocabulary, misreading, or failure to connect evidence. The spec needs a decision tree or mutually typed taxonomies, for example:

failure_type: construct failure | execution failure | access failure
primary_cause: controlled cause enum
contributing_causes: controlled cause enum[]

It must define minimum evidence and tie-breaking rules. Otherwise LLMs and human reviewers will classify identical responses differently.

Additional high-risk defects
Section 15 calls marker_confidence numeric but Section 18 calls it LOW/MEDIUM/HIGH. MODERATE also appears despite not being defined.
“Two markers disagree” does not define the disagreement tolerance for partial marks.
Summary marking has no operational rubric, content-point model, word-count rule, penalty rule, or treatment of copied passage language.
Objective marking does not define case sensitivity, spelling tolerance, equivalent wording, blank answers, multiple answers, partial credit, or multi-select behaviour.
Provenance is described as exactly one tag per field, but human amendment requires preserving the AI value, human value, actor, timestamp, and revision history. A single tag
cannot represent this history.
Human approval requires fields to reach at least AI_REVIEWED, but does not require every final field to carry HUMAN_APPROVED, contradicting the field-level requirement.
There is no rejection, return-for-revision, or invalidation transition in the assessment lifecycle.
The assessment API has no authentication, authorization, student binding, attempt creation, resume semantics, idempotency, or concurrency rules.
“No client-observable path” is not an implementable security boundary without defining trusted backend services, logging policy, proxy behaviour, and redaction requirements.
Level 3c, Level 4, and Level 5 overlap ambiguously. “New genre/topic” and “new passage” need explicit independent fields.
“Later” retention evidence has no elapsed-time threshold.
Cambridge-style material and authentic Cambridge 0500 material are mixed in the definition of Cambridge performance.
Section 25 claims an existing Mark-Loss Ledger “unchanged from v1,” but no such entity exists in the discoverable Athena repositories.
SyllabusVersion is required for historical evidence, but there is no rule for selecting the applicable version when an assessment is authored under UNDER_REVIEW.
The frontend’s current Chief Examiner loop treats reattempt success as improved readability, while the v1.1 recovery model treats reattempts as evidence about skill
recovery. These must not be silently merged.
Required disposition

Before implementation, v1.1 needs at minimum:

Separate global assessment lifecycle from per-student exposure.
Define unseen exposure events and abandoned/crashed/invalidated attempt semantics.
Add the deterministic Evidence Confidence policy.
Define independent evidence units and consistency thresholds.
Replace or constrain numeric LLM confidence.
Add a versioned Athena B1 Reading Profile.
Rewrite routing using raw marks, fixed denominators, precedence, and immutable assignment decisions.
Define Evidence Claim types and relation/state transitions.
Add recovery regression behaviour.
Operationalize root-cause classification.
Reconcile the specification with the actual Athena backend, which currently has no student model, English assessment model, Evidence Claim model, or Mark-Loss Ledger.

Until those changes are normative, the specification should remain marked:

DRAFT — BLOCKED FOR IMPLEMENTATION
