# Day 6 Reality Check runbook

This runbook describes the controlled reviewer/API workflow for a Cambridge 0500 Day-6 package. Tranche F does not acquire Cambridge material: the reviewer supplies the official files through protected admin endpoints. Automated tests use synthetic fixtures only.

## Release gate

Before assigning a real student, the version must be `ACTIVE` and have a persisted `Day6Qualification` state of `QUALIFIED_FOR_STUDENT_USE` under `ATHENA-DAY6-QUALIFICATION-v1`.

Pre-student checklist:

- [ ] AssessmentVersion is `ACTIVE`
- [ ] Day6Qualification is `QUALIFIED_FOR_STUDENT_USE`
- [ ] Question-paper SHA-256 is verified
- [ ] Mark-scheme SHA-256 is verified
- [ ] Cambridge provenance and syllabus binding are complete
- [ ] Supported question registry is frozen/available
- [ ] Structured supported criteria are frozen
- [ ] Transcription, segmentation, marker and evidence policy versions are present
- [ ] Human mark approval remains mandatory
- [ ] Coverage explicitly excludes unsupported families
- [ ] No unresolved mandatory qualification failures exist

## Reviewer onboarding sequence

All of these operations use reviewer authentication. The exact request DTOs are defined in `AssessmentController`.

1. `POST /api/admin/assessments` to create the logical assessment (`subjectCode=0500`).
2. `POST /api/admin/assessments/{assessmentId}/versions` to create a version with synthetic or reviewer-supplied protected content metadata.
3. `POST /api/admin/syllabus-versions` and bind it with `POST /api/admin/assessment-versions/{versionId}/cambridge-identity`.
4. Upload the question paper and mark scheme with `POST /api/admin/assessment-versions/{versionId}/assets/QUESTION_PAPER` and `/MARK_SCHEME`. The server stores the bytes and computes SHA-256; client hashes are not authoritative.
5. Verify each asset with `GET /api/admin/assets/{assetId}/integrity`.
6. Freeze and activate the provenance package using the existing Cambridge provenance endpoints. Incomplete or corrupt provenance is rejected.
7. Register each supported question using `POST /api/admin/assessment-versions/{versionId}/questions`. Only explicit comprehension and inference are in the first release scope; other question families must remain unsupported/not included.
8. Create the structured scheme, add question/criterion rules, submit, approve, and freeze it using the `/api/admin/mark-scheme-definitions` endpoints. The raw mark-scheme asset remains the provenance source.
9. Start and run the release gate with `POST /api/admin/assessment-versions/{versionId}/qualification/start` and `/qualification/run`.
10. Inspect `GET /api/admin/assessment-versions/{versionId}/qualification`. Do not assign students unless the state is `QUALIFIED_FOR_STUDENT_USE`.

## Student workflow

1. Reviewer creates the student and assignment through server-authorised APIs.
2. The student creates an attempt from the assignment. Student identity is resolved from the server principal; request bodies cannot choose a student.
3. The student fetches `/api/assessments/attempts/{attemptId}/content`. Exact attempt, assignment, student, version, active-state, qualification and window checks run before delivery. The first successful delivery permanently records per-student exposure.
4. The student uploads a supported response PDF to `/api/assessment-attempts/{attemptId}/responses`.
5. Reviewer calls the transcription and segmentation endpoints, resolves any transcript/segmentation issues, reviews/amends as necessary, and freezes the response artifact.
6. Reviewer marks only frozen responses against a frozen structured scheme, then approves or manually amends every criterion result.
7. Reviewer calls `/api/admin/assessment-attempts/{attemptId}/english-evidence/rebuild` and retrieves `/reality-check`.

## Troubleshooting and safety

- An unqualified, inactive or retired package must not be assigned or delivered.
- A mark-scheme asset is reviewer-only and is never returned by the student content route.
- A hash mismatch is an integrity failure. Suspend the qualification; do not replace bytes in place.
- Transcription uncertainty, unknown labels, overlap, duplicate labels and unassigned text block response freeze until explicitly resolved.
- Model failure produces pending human work, not a zero mark. Manual reviewer fallback preserves the failed event.
- Re-marking creates a superseding approved result; evidence/report rebuild uses only the current result and retains history.
- `TwinSnapshot` is not the source of truth. English projection and reports derive from approved marks, evidence units and claims.
- The legacy `/api/scripts` controllers are outside this trust boundary and must not be used to deliver Day-6 protected content.

## Audit inspection

Inspect assessment audit events for assignment, attempt, denial, protected delivery, qualification and suspension. Trace a report through its approved mark, frozen segment, transcript/page, immutable response artifact, attempt, and exact assessment-version provenance.

Live Google verification and live OpenAI calls require production configuration (`GOOGLE_CLIENT_ID`, reviewer configuration, `OPENAI_API_KEY`, protected storage and PostgreSQL). They are intentionally not replaced with client localStorage or unverified claims.
