# Chief Examiner Backend v0.1 — "Read Handwritten Evidence"

Athena Agent 001, pipeline stage 1: upload a handwritten script PDF, render its
pages, transcribe them faithfully with OpenAI vision (GPT-4o by default; set OPENAI_MODEL to upgrade), and segment the evidence
by exam question (1(a), 1(b)(ii), ...).

## Prerequisites
- Docker + Docker Compose  (or: Java 21 + Maven + a local PostgreSQL)
- An OpenAI API key: https://platform.openai.com/api-keys (needs billing enabled; GPT-4o vision)

## Run everything with Docker
```bash
export OPENAI_API_KEY=your-key-here
docker compose up --build
```
Backend: http://localhost:8080  ·  Postgres: localhost:5432 (athena/athena)

## Run backend from source (Postgres via Docker)
```bash
docker compose up -d postgres
export OPENAI_API_KEY=your-key-here
mvn spring-boot:run
```

## API
| Method | Path                        | Purpose                                   |
|--------|-----------------------------|-------------------------------------------|
| POST   | /api/scripts                | multipart upload: file + card metadata    |
| POST   | /api/scripts/{id}/read      | start reading (202; poll status)          |
| GET    | /api/scripts/{id}           | script status (UPLOADED/READING/READ/FAILED) |
| GET    | /api/scripts/{id}/transcript| question segments + per-page transcripts  |
| GET    | /api/scripts                | list all scripts                          |
| DELETE | /api/scripts/{id}           | remove a script                           |

## Smoke test without the UI
```bash
curl -F file=@script.pdf -F exam="Mathematics P2 — June 2025" \
     -F paperId=0580_s25_22 http://localhost:8080/api/scripts
curl -X POST http://localhost:8080/api/scripts/<id>/read
curl http://localhost:8080/api/scripts/<id>            # until status=READ
curl http://localhost:8080/api/scripts/<id>/transcript
```

## Spec behaviours enforced
- unreadable handwriting becomes `[illegible]` — the model is told to NEVER guess
- every page and segment carries a confidence score — uncertainty is never hidden
- re-running /read is idempotent (previous transcripts are replaced)

## Not yet in this increment (by design)
- Google ID-token verification server-side (frontend auth is UI-gating only)
- Marking against mark scheme, mistake taxonomy, Intelligence Briefing, Digital Twin update
