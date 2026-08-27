# Athena Day 6 — Tranche C Implementation Report

Date: 2026-08-26

## Scope

Tranche C establishes the path from an authorised exposed attempt to an immutable response artifact, retained rendered pages, English-specific transcription, validated 0500 question segmentation, reviewer resolution, and frozen response evidence. It does not implement marking, mark-scheme parsing, Evidence Claims, Evidence Confidence, RecoveryCase, Digital Twin projection, Mission changes, or report generation.

## Reuse decisions

- Reused PDFBox rendering mechanics and the existing local filesystem storage convention.
- Adapted the OpenAI client behind `EnglishResponseTranscriber` and `EnglishResponseSegmenter` interfaces with separate English/0500 prompt policies.
- Did not reuse legacy `Script`, `PageTranscript`, or `QuestionSegment` storage for Day 6 because those records are mathematics-oriented and rerun by replacement/deletion. Legacy controllers and Mission/TwinSnapshot behavior remain unchanged.

## New domain and persistence

Added:

- `ResponseArtifact`, bound immutably to attempt, student, and AssessmentVersion, with PDF-only policy, server SHA-256, sequence, status, and immutable source location.
- `ResponsePage`, retaining each rendered page image, dimensions, and SHA-256.
- `ResponseTranscript`, retaining exact transcript text, explicit illegible/crossed-out/uncertain/alternative regions, model/prompt/request provenance, and review status.
- `AssessmentQuestion`, the frozen version-specific question registry.
- `QuestionResponseSegment`, bound to registered questions or explicit `UNASSIGNED_TEXT`, with page and transcript-span references, status, and model provenance.
- `ResponseSegmentationIssue`, for unknown/duplicate/missing labels, invalid ranges, overlaps, unassigned text, and related blocking ambiguity.
- `ResponseReviewEvent`, append-only original/revised reviewer history.
- `ResponseProcessingEvent`, append-only render/transcription/segmentation attempt provenance and failure records.

Added migration `V5__day6_tranche_c_response_evidence.sql`; V1–V4 were not modified.

## Response, rendering, and transcription policy

Student uploads require the server-resolved student identity, exact owned attempt/assignment/version bindings, active in-progress attempt state, and an existing exposure. Only non-empty PDFs are accepted. Resubmission creates a new sequence and marks the prior non-frozen artifact `SUPERSEDED`; original bytes remain untouched.

The service verifies the source SHA-256 before processing, renders PDF pages with PDFBox, stores PNG bytes permanently, and records page hashes. Transcription is English-specific and preserves wording, spelling, punctuation, paragraph structure, crossed-out text, `[ILLEGIBLE]` markers, uncertainty, and alternatives. Transcription failure records a failed processing event and creates no synthetic transcript.

Repeated transcription is idempotent for an existing non-failed page result. Model, model-version, prompt-version, and request ID are persisted. Coordinates are represented by page number and transcript source spans; the current vision client does not provide pixel-level handwriting boxes, so no boxes are fabricated.

## Segmentation and review

Segmentation receives only registered question codes and stores proposals only after validation. Unknown labels are never inserted into the question registry. Duplicate labels, invalid ranges, overlaps/out-of-order spans, missing registered labels, and unassigned text create blocking issues. Multi-page spans are supported by `pageStart`/`pageEnd`; `UNASSIGNED_TEXT` is persisted rather than discarded.

Reviewer-only APIs approve/amend transcripts, approve/reassign/ignore segments, resolve issues, and freeze the final artifact. Amendments retain original and revised values in `ResponseReviewEvent`. Freeze verifies artifact/page integrity, approved transcript states, no unresolved blocking issues, and approved/ignored segments, then marks segments `FROZEN` and the artifact `FROZEN`. No marking endpoint is invoked.

## APIs

- `POST /api/assessment-attempts/{attemptId}/responses` — authenticated student PDF upload.
- `POST /api/admin/assessment-versions/{versionId}/questions` — reviewer question registry setup.
- `POST /api/admin/response-artifacts/{artifactId}/transcribe` — reviewer processing request.
- `POST /api/admin/response-artifacts/{artifactId}/segment` — reviewer segmentation request.
- `POST /api/admin/response-artifacts/{artifactId}/freeze` — reviewer freeze.
- Reviewer transcript, segment, and issue approve/amend/reassign/ignore/resolve endpoints.

All reviewer routes reuse Tranche A server authorization. No frontend changes were made.

## Tests

Added `ResponseEvidenceServiceTest` with 8 focused tests covering authorised upload, ownership/exposure denial, PDF-only rejection, immutable resubmission, PDF page retention, transcription preservation/uncertainty, transcription failure without fake output, unknown/unassigned segmentation, and reviewer authorization. Existing Tranche A and B tests remain green.

`mvn test` result:

```text
Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Synthetic PDFs/bytes only were used. No Cambridge or student content was added.

## Clean PostgreSQL validation

Used a disposable Docker PostgreSQL 16.11 database. The initial empty-database startup applied V1–V5 successfully; a local port collision occurred only after migration and Hibernate validation had completed. A second startup against the same migrated clean database on an unused port completed successfully.

- Flyway history: V1, V2, V3, V4, V5 all `success = true`; failed migrations: 0.
- Hibernate schema validation: PASS.
- Backend startup against migrated schema: PASS.
- Verified V5 tables: `response_artifact`, `response_page`, `response_transcript`, `assessment_question`, `question_response_segment`, `response_segmentation_issue`, `response_review_event`, `response_processing_event`.
- Verified uniqueness/indexes for artifact sequence, page ordering, transcript-per-page, question-per-version, and processing lookup.
- The disposable PostgreSQL container was removed. No shared or production database was used.

## Known limitations

- No Testcontainers regression harness was added; clean bootstrap was validated directly through Spring Boot/Flyway.
- No pixel bounding boxes are available from the current vision client; page and transcript-span references are retained instead.
- Actual reviewer UI is deferred; reviewer APIs and append-only review records are present.
- The existing frontend does not yet send the Google bearer token or provide response-upload UI.
- Unanswered registered questions create blocking missing-label issues until a reviewer resolves them explicitly.

## Explicitly unchanged

- No English marking, structured mark scheme, Mission, `mistake_tag`, TwinSnapshot, Evidence Claims, Evidence Confidence, RecoveryCase, or Digital Twin behavior was changed.
- Legacy `/api/scripts` ingestion and legacy Chief Examiner pipeline remain outside the Day 6 boundary.
- No deployment was performed.

Final Tranche C status: COMPLETE.
