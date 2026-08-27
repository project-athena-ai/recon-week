# Athena Day 6 — Tranche D Implementation Report

Date: 2026-08-26

## Scope

Tranche D adds a reviewer-controlled structured mark-scheme interpretation and an English-specific marking boundary for the narrow `EXPLICIT_COMPREHENSION` and `INFERENCE` families. It does not implement Evidence Claims, Evidence Confidence aggregation, RecoveryCase, Digital Twin projection, Mission changes, root-cause classification, or the Day-6 report.

## Structured scheme

Added `MarkSchemeDefinition`, `MarkSchemeQuestion`, `MarkSchemeCriterion`, and `AcceptedAnswerRule`. A definition binds to the exact frozen mark-scheme asset ID and persisted SHA-256 hash, AssessmentVersion, source type, parser-policy version, revision, creator, and review lifecycle. Criteria bind by immutable `AssessmentQuestion` ID, not by a free-text label. Unsupported question families remain explicitly declared and are never sent to the marker.

The lifecycle is `DRAFT`/`AI_EXTRACTED` → `REVIEW_REQUIRED` → `HUMAN_APPROVED` → `FROZEN`. A frozen definition cannot be edited. Freeze re-reads and verifies the bound asset bytes against the stored SHA-256. The raw mark-scheme asset remains the provenance source; no copyrighted Cambridge content was added.

## Marker and result model

Added `English0500Marker` and `EnglishMarkingService`. The OpenAI transport is reused only as transport; mathematics `MissionService`/`QuestionMark` semantics are not reused. The prompt policy is versioned as `ATHENA-0500-MARKER-v1` / `ATHENA-0500-MARKER-PROMPT-v1` and requires criterion-level strict JSON, supplied-rubric-only marking, exact student evidence, and concise rationale.

`CriterionMarkResult` stores marks, evidence, rationale, controlled uncertainty signals, advisory model confidence, derived operational confidence, and review state. Confidence follows v1.2.3 exactly: HIGH is `COMPLETE + CLEAR + NONE + TRUE + AGREED`; LOW is weak/invalid/disagreed/unresolved; all other valid results are MEDIUM. Advisory numeric confidence is audit-only. Every model result remains `REVIEW_REQUIRED`; malformed/model-failure results become `PENDING_HUMAN_MARK` after three attempts, and unsupported questions do not invoke the marker.

`ApprovedQuestionMark` is a separate immutable reviewer-approved criterion mark with reviewer, rationale, evidence references, timestamp, and optional supersession link. Manual approval is available for model failure and unsupported/human-only questions. Question totals and lost marks are derived by summing approved criterion rows; marks are bounds-checked in Java and PostgreSQL. Review events preserve approval/amendment/rejection history.

## APIs

- `POST /api/admin/mark-scheme-definitions` — create a draft definition for an existing official mark-scheme asset.
- `POST /api/admin/mark-scheme-definitions/{id}/questions` — bind a registered AssessmentQuestion.
- `POST /api/admin/mark-scheme-questions/{id}/criteria` — add a structured criterion.
- `POST /api/admin/mark-scheme-definitions/{id}/submit`, `/approve`, `/freeze` — reviewer lifecycle actions.
- `POST /api/admin/response-segments/{id}/mark` — mark only a frozen response segment against a frozen scheme.
- `POST /api/admin/marking-events/{id}/approve` or `/reject` — reviewer decision/fallback.
- `GET /api/admin/response-segments/{id}/mark-total` — derived approved total/lost marks.

All new endpoints are under the existing reviewer-only `/api/admin/**` boundary. No student endpoint exposes raw or structured mark-scheme material.

## Migration

Added `V6__day6_tranche_d_0500_marking.sql` without modifying V1–V5. It adds question support/type fields and tables for structured schemes, accepted rules, scheme review events, marking events, criterion results, approved marks, and mark review events, including foreign keys, check constraints, and criterion/mark indexes.

## Tests and validation

Added `EnglishMarkingServiceTest` with 6 focused tests covering semantic-equivalent comprehension, mandatory human approval even for high confidence, fabricated evidence rejection, unsupported-question no-LLM routing, out-of-range rejection, and manual approval.

`mvn test` result:

```text
Tests run: 38, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Spring Boot was started against a newly created disposable Docker PostgreSQL 16.11 database. Flyway validated and applied V1 through V6 from an empty schema; `flyway_schema_history` recorded all six with `success = true`. Hibernate schema validation and backend startup passed. The schema was inspected for all V6 tables, foreign keys, checks, and approved-mark bounds constraints. No shared or production database was used, and the disposable container was removed after validation.

## Known limitations

- The first slice supports only short-answer comprehension and inference; summary, writer’s effect, language analysis, directed writing, and levels-based writing remain unsupported.
- Automatic mark-scheme extraction is not implemented; reviewer APIs create the structured interpretation from the immutable source asset.
- Evidence validation uses exact substring presence in the frozen transcript; richer coordinate navigation remains a later concern.
- Reviewer UI, Evidence Claims, Evidence Confidence aggregation, RecoveryCase, Digital Twin projection, and final reporting are deferred.
- Re-marking history is represented by immutable event/mark records and supersession fields; a dedicated re-mark orchestration endpoint is deferred.

## Explicitly unchanged

No Mission, `mistake_tag`, TwinSnapshot, Evidence Claims, Evidence Confidence aggregation, RecoveryCase, or Digital Twin behavior was changed. No frontend functionality or deployment was changed.

Final Tranche D status: COMPLETE.
