# Athena Day 6 — Tranche E Implementation Report

Date: 2026-08-26

## Scope

Tranche E converts approved Tranche D criterion marks into controlled English skill evidence, immutable performance claims, deterministic confidence inputs, a claims-derived English Digital Twin projection, and a reviewer/admin Reality Check report. It does not implement RecoveryCase, recovery transitions, Day 1 diagnostics, prescriptions, adaptive teaching, Mission changes, unsupported English question families, or grade/CEFR prediction.

## Skill taxonomy and mapping

The controlled taxonomy includes `EXPLICIT_COMPREHENSION` and `INFERENCE` (with future taxonomy values represented but not measured). Only frozen approved marks whose frozen criterion type is one of those two values create evidence units. Unsupported criterion types are ignored by the evidence ingestion path rather than converted to zero or UNKNOWN-as-zero. Mapping is server-side and not client supplied.

## Evidence units and lost marks

Added `SkillEvidenceUnit`, bound to the student, attempt, AssessmentVersion, question, criterion, approved mark, source type, evidence context, marker confidence, and timestamps. Each unit stores `maxMarks`, `awardedMarks`, and deterministic `lostMarks = maxMarks - awardedMarks`, with Java and PostgreSQL bounds/check constraints. The unit also stores passage, genre/topic, sitting, and evidence-level keys so the v1.2.3 independent-unit policy is explicit. Authentic Cambridge versions use the exact AssessmentVersion source type and evidence level 7; no client-provided provenance is accepted.

Current aggregation excludes approved marks superseded by a later approved mark. Historical marks and units remain queryable, while the current projection/report uses only the current authoritative mark set. Evidence-unit creation is idempotent per approved mark.

## Evidence Claims

Added immutable `EvidenceClaim`, `EvidenceClaimEvidence`, and `EvidenceClaimRelation` records. This tranche creates only controlled `SKILL_PERFORMANCE` claims. v1.2.3 does not define a safe single-Day-6 defect inference rule, so `SKILL_DEFECT` claims are not generated. Claims always have evidence links, controlled skill/type/status, performance totals, confidence components, policy version, and exact student/attempt/version binding. Rebuilding unchanged approved evidence reuses the same deterministic claim revision; changed authoritative marks create a new revision and supersede the prior active claim without deleting it.

## Evidence Confidence

`EvidenceConfidencePolicy` implements Section 22:

- quantity: 0 = UNKNOWN, 1 = LOW, 2–3 = MEDIUM, 4+ = HIGH;
- diversity: LOW for one sitting; MEDIUM requires at least two passages, two genre/topic combinations, and two sittings; HIGH requires four passages plus those same temporal/context conditions;
- consistency: fewer than three independent units = UNKNOWN; percentage range thresholds are >40 LOW, >20 through 40 MEDIUM, and at most 20 with at least four units HIGH;
- overall: UNKNOWN for no units, LOW for one or two units, otherwise the minimum of quantity/diversity/consistency/marker confidence with the v1.2.3 evidence-hierarchy caps.

The persisted policy identifier is `ATHENA-EVIDENCE-CONFIDENCE-v1`. Operational marker confidence comes from Tranche D’s controlled result, never advisory model confidence. Inputs and output are stored on each claim. A single sitting cannot inflate confidence beyond the contract’s limits.

## Digital Twin projection

Added `EnglishTwinProjection` as a separate materialized view. Existing `TwinSnapshot` was not changed and is not authoritative. The projection is rebuilt from current approved marks, evidence units, and claims; it includes only measured supported skills, marks, loss, unit count, last measurement, and Evidence Confidence. The rebuild replaces the materialized row deterministically and does not use prior projection JSON as source of truth.

## Reality Check report

Added `RealityCheckReport` and reviewer/admin endpoints:

- `POST /api/admin/assessment-attempts/{attemptId}/english-evidence/rebuild`
- `GET /api/admin/assessment-attempts/{attemptId}/reality-check`

The report binds one student, attempt, and AssessmentVersion and contains exact Cambridge paper/session/year/paper/component/variant/syllabus/source provenance when available, overall available/awarded/lost marks, supported skill breakdown, separate Evidence Confidence, deterministic largest mark-loss skill, criterion-level approved marks/evidence references, and explicit coverage limitations. It contains no predicted grade, CEFR certification, root-cause diagnosis, recovery state, or prescription. The endpoints remain reviewer-only in this tranche.

## Migration and audit

Added `V7__day6_tranche_e_evidence_claims.sql` without modifying V1–V6. It adds evidence-unit, claim, claim-evidence, claim-relation, English projection, and report tables with foreign keys, controlled checks, uniqueness, and mark/loss bounds. Existing audit infrastructure records evidence-unit creation, claim creation, confidence derivation, projection rebuild, and report generation.

## Tests and validation

Added `EvidenceConfidencePolicyTest` with 6 tests covering quantity, one-sitting diversity, medium diversity, consistency bands, single-sitting overall confidence, and hierarchy caps. Existing Tranche A–D tests remain green.

`mvn test` result:

```text
Tests run: 44, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Spring Boot/Flyway was started against a newly created disposable Docker PostgreSQL 16.11 database. V1 through V7 applied from an empty schema, every Flyway history row reported `success = true`, Hibernate schema validation passed, and the backend started successfully. The new V7 tables were queried directly. The disposable container was removed afterward; no production/shared database was used.

`mvn package -DskipTests` and the repository documentation `git diff --check` completed successfully. The backend directory is outside a Git worktree, so backend-local `git status`/`git diff` are unavailable; no deployment was performed.

## Known limitations

- Only explicit comprehension and inference are measured.
- Only performance claims are created; defect inference, root causes, RecoveryCase, and recovery transitions are deferred.
- Evidence units generated from the current Tranche A/B model use the AssessmentVersion as the passage key and a conservative unknown genre/topic value; consequently a single Day-6 sitting remains LOW under the frozen policy.
- Claim listing/detail APIs beyond the report endpoint are deferred; the persistence model and rebuild path are present.
- Existing TwinSnapshot, Mission, mathematics marking, and frontend functionality are unchanged.
- No student-facing report route was added; the report is reviewer/admin-only.

Final Tranche E status: COMPLETE.
