# Athena Day 6 — Tranche A Implementation Report

Date: 2026-08-26

## Scope

This tranche establishes only the server identity and assessment exposure boundary:

`server principal → student → assignment → AssessmentVersion → attempt → protected delivery → permanent exposure`.

It does not implement English marking, Cambridge mark-scheme parsing, Evidence Claims, Evidence Confidence, RecoveryCase, Digital Twin projection, Mission changes, or Day 1 diagnostics.

## Code changed

In `/home/david/src/github.com/athena/backend`:

- Added Spring Security OAuth2 resource-server dependencies.
- Added `SecurityConfig` with Google issuer/signature/expiry/audience validation in normal runtime.
- Added explicit reviewer-principal configuration for the minimal `REVIEWER` role.
- Added `IdentityService`, which resolves identity from the authenticated server principal and never from request fields.
- Added `StudentIdentity`, `Assessment`, `AssessmentVersion`, `StudentAssessmentAssignment`, `AssessmentAttempt`, `StudentAssessmentExposure`, and `AssessmentAuditEvent`.
- Added repositories for the new entities.
- Added `AssessmentAccessService` for reviewer setup, assignment enforcement, attempt creation, protected delivery, abandonment, invalidation, and audit events.
- Added `AssessmentController` with separate student and reviewer/admin routes.
- Left legacy ScriptController, MissionService, Mission, QuestionMark, and TwinSnapshot semantics unchanged.

Frontend files changed: none. The new API accepts a standard `Authorization: Bearer <Google ID token>` request; a UI handoff can be added in a later tranche without weakening the boundary.

## Migration

Added:

- `backend/src/main/resources/db/migration/V3__day6_tranche_a.sql`

The migration is additive and creates the six required domain tables plus an append-only audit-event table. It includes foreign keys, indexes, status checks, validity-window checks, and a unique `(student_id, assessment_version_id)` exposure constraint.

## APIs

Reviewer/admin setup routes:

- `POST /api/admin/students`
- `POST /api/admin/assessments`
- `POST /api/admin/assessments/{assessmentId}/versions`
- `POST /api/admin/assessment-versions/{versionId}/activate`
- `POST /api/admin/assignments`
- `POST /api/admin/assessment-attempts/{attemptId}/invalidate`

Student routes:

- `GET /api/assessment-assignments`
- `POST /api/assessment-assignments/{assignmentId}/attempts`
- `GET /api/assessments/attempts/{attemptId}/content`
- `POST /api/assessment-attempts/{attemptId}/abandon`

The protected content route returns only the synthetic/admin-provided version payload after all identity, ownership, assignment, attempt, lifecycle, and time-window checks pass.

## Security model

Normal runtime uses Spring Security’s OAuth2 resource-server JWT support. Google issuer discovery, signature validation, standard token validators, expiry validation, and configured audience validation are required. Startup fails when `GOOGLE_CLIENT_ID` is absent in the normal Google mode.

The server maps the verified Google `sub` claim to a stable `StudentIdentity.principalId`. Email, display name, student ID, role, and `uploadedBy` request fields are not accepted as authentication. Reviewer identity is limited to configured principal IDs; all other verified principals receive the student role.

The new `/api/assessments/**`, `/api/assessment-assignments/**`, `/api/assessment-attempts/**`, and `/api/admin/**` routes are protected. Legacy `/api/scripts/**` and legacy mission routes remain outside the new Day 6 boundary by design and are not used for protected Day 6 content.

## Exposure semantics

`AssessmentVersion.globalStatus` is independent from `StudentAssessmentExposure.status`.

The delivery service performs authorization inside one transaction, then executes the PostgreSQL insert:

```sql
ON CONFLICT (student_id, assessment_version_id) DO NOTHING
```

The exposure row is therefore created before the controller returns the successful content response. The same transaction changes a `CREATED` attempt to `IN_PROGRESS` and sets `startedAt` once. Repeated delivery preserves the original exposure timestamps/request/attempt and writes an additional delivery audit event.

The safe residual failure mode is conservative: if the database commits and the HTTP response is lost, the student remains recorded as exposed. This cannot produce a false `UNSEEN` state.

Denied, failed, inactive, expired, wrong-owner, wrong-version, submitted, and invalidated requests return no protected payload and do not create exposure. Abandonment or invalidation does not delete exposure; a later authorized attempt for the same version sees the existing student/version exposure.

## Tests

Added:

- `AssessmentAccessServiceTest`
- `IdentityServiceTest`

The tests cover assigned access, active-assignment attempt creation, unassigned denial, wrong-student denial, repeated delivery, invalidation without restoring unseen, inactive assignment, retired version, access windows, concurrent first delivery, per-student independence, and principal-derived identity.

The concurrent test models the database unique-key insert behavior; the production guarantee is the PostgreSQL unique constraint plus `ON CONFLICT DO NOTHING` in V3.

Command:

```text
mvn test
```

Result:

```text
Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

No live Google authentication is used by tests. No frontend or production auth bypass was added.

## Known limitations

- A real production deployment must provide `GOOGLE_CLIENT_ID` and configure `ATHENA_REVIEWER_PRINCIPAL_IDS`.
- The existing frontend does not yet send the Google credential as a bearer token to these routes.
- The primary backend has no clean-database integration-test harness in this tranche; the migration is PostgreSQL-specific as required by the existing deployment.
- Legacy script endpoints remain unauthenticated and must not be used for Day 6 protected content or new assessment payloads.
- The assignment uniqueness constraint permits one assignment per student/version; assignment replacement policy is intentionally deferred.
- No submission, marking, evidence, claim, confidence, recovery, or Twin projection behavior is included.

## Explicitly unchanged

- No Java marking prompt was changed.
- No Mission lifecycle or verification semantics were changed.
- No `mistake_tag` semantics were changed.
- No `TwinSnapshot` behavior was changed.
- No Cambridge paper, passage, question, or mark scheme was added.
- No deployment was performed.

## Clean PostgreSQL validation

Validation was completed on a disposable Docker PostgreSQL instance:

- PostgreSQL version: 16.11
- Database: `athena_clean` (temporary isolated database)
- Container: `athena-day6-tranche-a-pg`
- JDBC URL shape: `jdbc:postgresql://localhost:<temporary-port>/athena_clean`
- Migration chain: V1, V2, V3
- Flyway history: all three migrations applied successfully from an empty schema; failed migrations: 0
- Backend startup: Spring Boot/Flyway applied the migrations, Hibernate validated the schema, and Tomcat started successfully
- Schema verification: all seven Tranche A tables, foreign keys, checks, indexes, and the unique `(student_id, assessment_version_id)` exposure constraint were verified directly in PostgreSQL
- Tests: `mvn test` — 11 passed, 0 failures, 0 errors

Acceptance criterion 17 is now verified. No migration SQL changes were required. The disposable container was stopped and removed after validation; no shared or production database was used.

Final Tranche A status: COMPLETE.
