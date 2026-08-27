# Athena Day 6 — Tranche B Implementation Report

Date: 2026-08-26

## Scope

Tranche B establishes immutable Cambridge 0500 assessment-package identity and provenance. It does not implement marking, mark-scheme parsing, Evidence Claims, Evidence Confidence, RecoveryCase, Digital Twin projection, Mission changes, or frontend functionality.

## Backend changes

Added to `/home/david/src/github.com/athena/backend`:

- `SyllabusVersion`, with controlled `CURRENT`, `SUPERSEDED`, and `UNDER_REVIEW` states and reviewer resolution audit fields.
- `CambridgePaperIdentity`, binding one AssessmentVersion to subject, qualification, session, year, paper, component, variant, authentic source type, syllabus version, and source reference.
- `AssessmentAsset`, a generic immutable asset record supporting `QUESTION_PAPER` and `MARK_SCHEME` separately (plus reserved official-resource types).
- `CambridgeProvenanceService`, providing reviewer-only registration, server-side storage and SHA-256 hashing, integrity verification, completeness checks, freeze, activation, retirement, and separate asset download.
- Minimal reviewer/admin endpoints for syllabus creation/resolution, Cambridge identity, asset registration, freeze, activation, retirement, asset integrity, and reviewer-only asset retrieval.

The existing Tranche A assignment, attempt, exposure, identity, and protected student content paths remain in place. The old generic activation route now delegates to the provenance-validated activation path. `AssessmentVersion` activation requires `FROZEN`; an authentic version cannot become frozen or active without complete provenance and verified assets.

## Migration

Added `backend/src/main/resources/db/migration/V4__day6_tranche_b_cambridge_provenance.sql`.

It is additive and creates `syllabus_version`, `cambridge_paper_identity`, and `assessment_asset`, with foreign keys, controlled-value checks, date/year checks, indexes, and uniqueness constraints. V1–V3 were not modified.

## Source types and provenance

The controlled source vocabulary includes `AUTHENTIC_CAMBRIDGE_0500`, `ATHENA_CAMBRIDGE_STYLE`, `ATHENA_CEFR_ALIGNED`, and `ATHENA_ORIGINAL`. Only `AUTHENTIC_CAMBRIDGE_0500` is accepted by the Cambridge provenance path. The database also constrains Cambridge identity rows to that source type and subject `0500` is checked against the parent Assessment.

Session values are `FEB_MAR`, `MAY_JUN`, and `OCT_NOV`. Paper number, component code, and variant are separate immutable fields. Each authentic version binds to one syllabus record and one question-paper and mark-scheme asset of each type.

## Lifecycle and integrity

- `UNDER_REVIEW` syllabi block authentic freeze/activation until reviewer resolution.
- Freeze requires complete identity, syllabus, question-paper asset, mark-scheme asset, authentic source type, and a second read/hash verification.
- Activation requires `FROZEN`, complete provenance, and verified hashes.
- Frozen assets and paper identity have no mutation path and are database `updatable=false`; replacement bytes conflict and require a new AssessmentVersion.
- Asset registration computes SHA-256 from server-received bytes and stores byte length, algorithm, hash, protected storage location, source reference, and registration actor.
- Question-paper and mark-scheme retrieval are separate reviewer-only paths. No student endpoint returns mark-scheme assets.
- Retirement preserves the version, assets, hashes, syllabus, attempts, exposures, and audit history.

## Audit events

The existing append-only audit table records syllabus creation/resolution, Cambridge provenance creation, asset registration, asset hash verification, version freeze, activation, and retirement, with authenticated actor and timestamp represented by the existing audit event fields.

## Tests

Added `CambridgeProvenanceServiceTest` with 13 tests covering complete and incomplete package lifecycle, missing assets/syllabus, under-review syllabus, corruption, illegal activation, source-type elevation, frozen replacement, server hashing/idempotent duplicates, conflicting duplicates, student mark-scheme denial, and retirement history. Existing Tranche A tests remain unchanged.

`mvn test` result:

```text
Tests run: 24, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Synthetic bytes only were used. No Cambridge paper or mark-scheme content was downloaded or committed.

## Clean PostgreSQL validation

Validation used a disposable Docker PostgreSQL 16.11 instance and the normal Spring Boot/Flyway startup path. The database was empty before startup.

- PostgreSQL version: 16.11
- Migration chain: V1, V2, V3, V4
- Flyway history: all four rows have `success = true`; failed migrations: 0
- Backend startup: PASS; Flyway applied all four migrations and Hibernate validated the resulting schema before Tomcat started.
- Schema: all three V4 tables exist with the expected constraints and indexes; the unique `(assessment_version_id, asset_type)` asset binding and unique paper identity binding were verified directly in PostgreSQL.
- V4 checksum: recorded successfully by Flyway; no migration correction was required.
- The disposable database/container was removed after validation. No shared or production database was used.

## Known limitations

- Official Cambridge assets are not populated; only synthetic fixtures exist in tests.
- Asset storage uses the existing local filesystem storage root and remains replaceable by a protected object-storage implementation later.
- A future tranche may add richer reviewer workflow and structured mark-scheme parsing; neither is represented here.
- The existing frontend has no Tranche B UI or Google bearer-token handoff.

## Explicitly unchanged

- No English marking or marking prompts.
- No MissionService, `mistake_tag`, or TwinSnapshot changes.
- No Evidence Claims, Evidence Confidence, RecoveryCase, Digital Twin projection, response ingestion, or Cambridge content.
- No deployment was performed.

Final Tranche B status: COMPLETE.
