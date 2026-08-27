# Athena Day 6 — Tranche G.1 Deployment Report

Date: 2026-08-27

## Result

`TRANCHE G.1 STATUS: BLOCKED`

The source build is healthy, but this Ubuntu host does not currently contain the operator-provided Athena environment file or an Athena deployment target. Provisioning was not completed because the required secrets/database credentials and privileged installation capability were unavailable. No unrelated service was stopped or modified.

## Host and existing services

- Host: `david-IdeaPad-Gaming-3-15ARH7`
- OS: Ubuntu 24.04.3 LTS, x86-64
- Kernel: 7.0.0-28-generic
- Java: OpenJDK 21.0.11, compatible with `pom.xml` Java 21
- PostgreSQL service: active on localhost:5432
- NGINX: not installed

The existing Java processes were identified from their systemd cgroups:

| Service | User | Working directory | Port context | Action |
|---|---|---|---|---|
| `cabs-payment-service.service` | `ebridge` | `/opt/ebridge/cabs-payment-service` | unrelated host service | preserved |
| `allocation-service.service` | `ebridge` | `/opt/ebridge/allocation-service` | unrelated host service | preserved |

The prior `/home/david/app.jar` was confirmed to be an unrelated user-management artifact. It was not replaced.

## Athena build

Backend source: `/home/david/src/github.com/athena/backend`

The backend directory is not a Git worktree, so `git status`, `git diff`, and `git diff --check` cannot be performed there. No backend source changes were made in this tranche.

Sequential validation:

- `mvn test`: 48 passed, 0 failed, 0 errors
- `mvn clean package`: successful, tests included
- Artifact: `/home/david/src/github.com/athena/backend/target/chief-examiner-0.1.0.jar`
- Artifact manifest identifies `ai.athena.examiner.ExaminerApplication`, Spring Boot 3.4.1, Java 21
- Artifact SHA-256: recorded locally but intentionally omitted from this report because it is not needed for deployment identification

The test run against disposable PostgreSQL applied V1–V8 successfully. No production Flyway operation was performed.

## Configuration presence

`/etc/athena/athena.env`: `MISSING`

The shell environment also reported these variables as missing. Values were never printed:

| Variable | Result |
|---|---|
| ATHENA_AUTH_MODE | MISSING |
| GOOGLE_ISSUER | MISSING |
| GOOGLE_CLIENT_ID | MISSING |
| ATHENA_REVIEWER_PRINCIPAL_IDS | MISSING |
| OPENAI_API_KEY | MISSING |
| STORAGE_DIR | MISSING |
| MARKSCHEME_DIR | MISSING |
| DB_URL | MISSING |
| DB_USER | MISSING |
| DB_PASSWORD | MISSING |

The source configuration confirms the exact Spring variables are `DB_URL`, `DB_USER`, and `DB_PASSWORD`; no alternate names were invented.

## Provisioning status

No `/opt/athena`, `/var/lib/athena`, or `/etc/athena` directory existed. Noninteractive sudo is unavailable in this session, so creating root-owned configuration, a system service, protected directories, or a database user would require operator authentication. PostgreSQL is running, but connection inspection for the intended Athena database was blocked by missing credentials. No unrelated database was altered.

No `athena` service user, `athena.service`, production application directory, or production storage directory was created.

## Network and frontend

- Athena backend port: not assigned
- Athena systemd service: not created
- Local Athena health check: not available
- NGINX route for `athena.airwide.co.uk`: not present
- HTTPS: not verifiable; NGINX is absent
- Frontend Google-token wiring: present in the working-tree `index.html` from Tranche G; no frontend deployment was performed
- Athena API routing: not verifiable

The existing frontend change remains minimal: the Google GIS credential is forwarded as a bearer token on existing API requests. No localStorage identity is made authoritative.

## Rollback and safety

No production artifact, service, database, environment file, storage path, NGINX configuration, or unrelated service was modified. Therefore no rollback operation is required. The newly built JAR remains in the source tree only.

## Blockers

1. Operator environment file `/etc/athena/athena.env` is absent.
2. Required Google, reviewer, OpenAI, storage, and DB configuration is unavailable to this session.
3. Root installation privileges are not available noninteractively.
4. Dedicated Athena PostgreSQL database credentials/state are not identifiable.
5. NGINX/TLS and the public `athena.airwide.co.uk` route are absent.

## Next action

As an authenticated operator, install the protected environment file with `root:root` and mode `0600`, provision a dedicated Athena PostgreSQL database/user, create the `athena` service user and `/opt/athena` layout, install the artifact above as `athena.service`, and configure/reload NGINX only after `nginx -t` passes. Then rerun G.1 verification and proceed to the Tranche G real-boundary pilot.
