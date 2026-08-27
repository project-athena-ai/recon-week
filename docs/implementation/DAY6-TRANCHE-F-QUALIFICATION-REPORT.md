# DAY 6 — Tranche F qualification report

## Scope

Tranche F adds the persisted release gate for the existing A–E workflow. It does not add new English question-family marking, recovery, prescription, grade prediction, CEFR certification, Mission changes or Digital Twin source-of-truth changes.

## Qualification policy and states

Policy: `ATHENA-DAY6-QUALIFICATION-v1`.

The database records one qualification per AssessmentVersion and individual mandatory checks with PASS/FAIL/NOT_APPLICABLE, evidence and reason. States are `NOT_QUALIFIED`, `QUALIFICATION_IN_PROGRESS`, `QUALIFIED_FOR_REVIEWER_PILOT`, `QUALIFIED_FOR_STUDENT_USE`, and `SUSPENDED`.

Authentic assignments, attempt creation and protected delivery now require `QUALIFIED_FOR_STUDENT_USE` in addition to the existing ACTIVE and ownership checks. Suspension preserves attempts, exposure, assets, provenance and audit history while preventing new use.

## F.1 full golden-path qualification

`Day6GoldenPathE2ETest` now runs against a disposable clean PostgreSQL 16.11 instance and drives the production A–E services in order:

`student → qualified assessment → assignment → attempt → protected exposure → response upload → PDF rendering → mocked English transcription → mocked segmentation → reviewer freeze → frozen structured scheme → mocked English marking → human approval → evidence rebuild → Reality Check report`.

The synthetic result is Explicit Comprehension `5/6`, Inference `4/8`, overall `9/14`, lost `5`, with `INFERENCE` as the largest mark-loss skill. The test asserts exposure idempotency, exact provenance, approved-mark-only evidence, claim/projection/report rebuild idempotency, partial supported-family coverage, no grade/CEFR/recovery/hidden marking material, and cross-student attempt isolation. The reviewer-processing boundary was corrected so a reviewer may process a student-owned attempt while all evidence remains bound to the attempt’s server-resolved student.

The complete suite now passes with 48 tests, 0 failures and 0 errors. The clean E2E database applies V1–V8 from empty and Spring Boot starts successfully. The production-only Google token and OpenAI smoke tests remain intentionally unrun.

## E2E architecture verified

## F.1 final qualification closure

The full A→E golden path is now covered by one Spring Boot integration test,
`Day6GoldenPathE2ETest`, using the real production services, a disposable clean
PostgreSQL database, synthetic assets, and mocked transcription, segmentation,
and marking adapters. The test asserts each major boundary from server-bound
student identity through the final report, including qualification checks,
per-student exposure, immutable response hashing and rendering, reviewer
freezes, mandatory mark approval, evidence rebuild, and report generation.

The final synthetic report is deterministic: Explicit Comprehension `5/6`,
Inference `4/8`, overall `9/14`, lost `5`, and largest mark-loss skill
`INFERENCE`. Evidence Confidence is derived by the configured policy and is
presented separately from performance. The report excludes grade prediction,
CEFR statements, recovery states, raw mark-scheme material, accepted
alternatives, marker prompts, and reviewer-only data.

The test also proves that the original `4/8` inference approval remains
historical when a superseding `1/8` approval is created; the rebuilt current
report becomes `6/14` with `8` lost marks and does not double-count the old
approval. Rebuilding evidence and the report repeatedly is idempotent. Gerald
and Melusi-style student isolation, wrong-attempt denial, and independent
exposure state are preserved.

The qualification run records `QUALIFIED_FOR_STUDENT_USE` with all mandatory
checks passing. This is an automated qualification result only: live Google
token verification and a synthetic OpenAI smoke test in the target deployment
environment remain required before considering unrestricted student use.

F.1 final result: COMPLETE. Tranche F status: COMPLETE. Final safety verdict:
`READY_FOR_CONTROLLED_PILOT`.

The thin qualification service checks server identity/authorization boundaries, active authentic provenance, syllabus status, question-paper and mark-scheme SHA-256 integrity, question registry, frozen structured criteria, supported-family declaration, transcription/segmentation/marker/evidence policy versions, mandatory human review and declared partial coverage. The existing response, marking and evidence services remain the execution components; Tranche F does not duplicate their domain logic.

## Golden path and negative qualification

The repository contains focused qualification tests for a complete synthetic 0500-like package, corrupted question-paper rejection, and the authentic-version access gate. Existing A–E tests continue to cover protected exposure, provenance, response evidence, marking and evidence/report behaviour. The operational golden path is documented in the runbook and is API-driven; no copyrighted Cambridge asset is stored in the repository.

Security negatives remain enforced by Tranche A: unassigned students, wrong attempts and spoofed identity are denied without exposure. Tranche B keeps mark-scheme assets reviewer-only. Unsupported question families remain excluded from the existing D/E marking and report scope.

## Clean database qualification

Disposable PostgreSQL: `postgres:16-alpine`, PostgreSQL `16.11`, database `athena_f` (temporary container; no shared/production database used).

Migration chain:

`V1 → V2 → V3 → V4 → V5 → V6 → V7 → V8`

Spring Boot/Flyway startup applied all eight migrations successfully from an empty database. `flyway_schema_history` reported eight successful rows and no failed migrations. V8 created `day6_qualification` and `day6_qualification_check` with the version uniqueness, foreign keys, controlled-state checks and qualification-check uniqueness constraints.

## Test/build result

`mvn test` passed after the Tranche F.1 changes: 48 tests, 0 failures, 0 errors. This includes the prior 44 A–E tests, 3 Tranche F qualification/access-gate tests and 1 PostgreSQL-backed golden-path test. `git diff --check` is not applicable in the backend directory because that repository is supplied without a `.git` worktree; the documentation repository remains unmodified except for the requested Tranche F documents.

## Configuration and operational limitations

Production Google verification requires a configured Google audience/client ID and issuer; the test profile uses Spring Security test principals and does not exercise live Google credentials. OpenAI transcription/segmentation/marking requires `OPENAI_API_KEY` and model configuration; tests mock those calls and do not claim a live model smoke test. Protected asset storage and PostgreSQL must be configured in the deployment environment.

Consequently this is qualified for controlled reviewer/pilot operation after real environment configuration and paper onboarding, not an assertion of unrestricted student release. A real paper still requires reviewer-supplied official assets, reviewed supported-question registry and reviewed structured criteria.

## Final safety verdict

`READY_FOR_CONTROLLED_PILOT`

The local release gate and negative tests establish the intended trust boundary. Live external identity/model configuration and real-paper onboarding remain operational prerequisites before `READY_FOR_STUDENT_USE`.

Application code changed: YES. Migration created: YES (`V8__day6_tranche_f_qualification.sql`). Deployment performed: NO.
