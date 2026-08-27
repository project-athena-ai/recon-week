# Athena Day 6 — Tranche G Production Validation Report

Date: 2026-08-27

## Scope and result

This validation checked the available host, source configuration, current frontend, live listeners, and the A–F automated baseline. No real Google account/token, OpenAI credential, production database credential, or safe Athena deployment target was available to this shell. Consequently no live external call or deployed synthetic pilot was claimed.

Final result: `PARTIAL`

Final safety verdict: `NOT_READY`

## Environment validation

The backend source contains the expected V1–V8 migrations. `application.yml` defines Google mode, issuer, client ID, reviewer principals, OpenAI key/model/base URL, storage, mark-scheme storage, and DB settings through environment-variable substitution.

The current shell reported the following required variables as `MISSING` (values were never printed):

| Variable | Result |
|---|---|
| ATHENA_AUTH_MODE | MISSING |
| GOOGLE_ISSUER | MISSING |
| GOOGLE_CLIENT_ID | MISSING |
| ATHENA_REVIEWER_PRINCIPAL_IDS | MISSING |
| OPENAI_API_KEY | MISSING |
| STORAGE_DIR | MISSING |
| MARKSCHEME_DIR | MISSING |
| DB_URL / DB_USER / DB_PASSWORD | MISSING |

The two running Java processes are owned by `ebridge`, but their process environments were not readable from this shell. No `/etc/athena/athena.env`, Athena systemd unit, NGINX configuration, or HTTPS listener was found. Port 8082 answered with a Spring 404, not a health response. The inspected `/home/david/app.jar` is an unrelated user-management artifact, not the Athena backend artifact; no deployment was performed.

## Google authentication

Source verification: `PASS`. `SecurityConfig` uses issuer discovery and the configured Google issuer, applies default issuer/time validators, and adds an explicit audience validator for `GOOGLE_CLIENT_ID`. The converter uses verified `sub` as the principal and reviewer role is allowlisted by configured subject IDs. Request email/student fields are not used as authentication in the Day-6 services.

Frontend configuration: `PARTIAL`. The current frontend contains the configured public Google Web Client ID and receives the GIS credential. A minimal wiring change was made so existing API calls forward the retained ID token in `Authorization: Bearer`; decoded profile fields remain display-only. The deployed backend client-ID match could not be verified.

| Check | Result |
|---|---|
| Google configuration | FAIL — runtime configuration unavailable |
| Real Google authentication | FAIL — no authorized account/token and no Athena deployment |
| Invalid Google token/audience rejection | FAIL — not exercised against deployed Athena |

No token or subject was written to this report.

## OpenAI

Static source inspection: model defaults to `gpt-4o`, endpoint is `/v1/responses`, JSON-object output is requested, and the English transcription, segmentation, and marker prompt-policy versions are persisted by the pipeline. The marker validates criterion IDs, bounds, evidence spans, and derives operational confidence independently of advisory model confidence; status remains review-required.

| Check | Result |
|---|---|
| OpenAI configuration | FAIL — runtime key/configuration unavailable; timeout/retry are not explicitly configured |
| Real OpenAI connectivity | FAIL — no key available |
| Real synthetic transcription | FAIL — not run |
| Real synthetic English marking | FAIL — not run |
| Mandatory human mark approval | FAIL — deployed pathway unavailable |

No API key, prompt payload, token, or model response was written to this report.

## Database, storage, service, and proxy

| Boundary | Result | Evidence |
|---|---|---|
| Production PostgreSQL/Flyway | FAIL | Local PostgreSQL requested an unavailable password; production target was not identifiable. V1–V8 passed only in the disposable E2E test. |
| Protected storage | FAIL | Source defaults are present and local development directories are writable, but the runtime `STORAGE_DIR`/`MARKSCHEME_DIR`, service-account access, and production permissions were not verified. |
| Service user | PARTIAL | Running Java processes are owned by `ebridge`; least-privilege filesystem access was not verifiable. |
| Systemd environment | FAIL | No Athena unit or protected environment file was found. |
| HTTPS/NGINX | FAIL | No NGINX/TLS listener was found on the host; HTTPS probes to localhost failed. |

## Automated validation

`mvn test` completed successfully after the frontend-only wiring change:

- passed: 48
- failed: 0
- errors: 0

The suite includes the disposable PostgreSQL V1–V8 golden path, synthetic response processing, human approval semantics, evidence rebuild/report assertions, qualification, and negative identity/exposure tests.

Automated Reality Check assertions remain the A–F qualified result: Explicit Comprehension `5/6`, Inference `4/8`, Overall `9/14`, Lost `5`, largest mark-loss skill `INFERENCE`. Evidence Confidence is policy-derived in the test. This was not a deployed production report.

## Deployed pilot and security boundaries

The deployed synthetic Day-6 pilot was not run because the live port does not identify an Athena deployment and required credentials/configuration are unavailable. Therefore the following deployed checks are unverified: onboarding, qualification gate, assignment/attempt/exposure, real transcription, segmentation/review/freeze, marking/approval, evidence rebuild, report generation, audit records, student isolation, and all deployed negative cases A–G.

The repository integration test covers the corresponding controlled security cases, including unassigned student denial, wrong-attempt denial, student mark-scheme denial, qualification/access gating, spoofed identity non-authority, permanent exposure, and independent student exposure.

## Leakage and false-claim review

Static test assertions for the student-safe Reality Check report passed: no predicted grade, CEFR certification, recovery status, accepted-answer rules, marker prompt, or hidden marking material. No secret values were added to this report. A deployed report and deployed logs were not available for inspection.

## Changes made

Only the frontend `index.html` was changed for Tranche G: the existing Google credential is now attached as a bearer token to existing API requests. No backend semantics, migrations, A–F domain rules, or frozen specification were changed. Pre-existing frontend deletions and untracked files were preserved.

## Required next action

Provision or identify the real Athena deployment with a safe rollback path, restrictive protected environment file, configured Google reviewer account, OpenAI key/model settings, PostgreSQL credentials, storage paths, and HTTPS routing. Then run the authorized real-auth and synthetic pilot procedure and rerun the final safety gate.
