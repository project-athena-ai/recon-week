# Day 6 Tranche G.2 — Google Auth Wiring Report

## Scope

This report records the production Google-auth wiring correction and the
reviewer-bootstrap gate. No OpenAI pilot, Cambridge-content workflow, or
unrelated production configuration was run.

## Source and deployment state

- Frontend repository: `recon-week`, branch `main`.
- Backend remains at the authoritative Day-6 commit; no backend application
  semantics were changed.
- The frontend is a static, single-page `index.html`; no npm/Vite build exists.
- The existing Athena Web OAuth client ID is retained. The production browser
  origin is documented as `https://athena.airwide.co.uk`.

## Auth flow before this change

Google Identity Services supplied a credential to the browser callback. The
callback decoded claims for display and stored the credential plus identity
fields in durable `localStorage`. API calls used a shared `apiFetch` helper,
but requests could proceed without a credential and upload requests included a
client-supplied `uploadedBy` email.

## Auth flow after this change

- Google credentials are held in `sessionStorage`, not durable storage.
- The shared `apiFetch` path requires a credential and sets
  `Authorization: Bearer <Google ID token>` for Athena API requests.
- A 401 clears the session and returns the UI to signed-out state.
- The development/simulated sign-in path was removed.
- Decoded Google claims are display metadata only; backend Spring Security
  remains authoritative for signature, issuer, audience, expiry, subject,
  principal, and role.
- Client-supplied `uploadedBy` identity was removed from uploads.

## Production configuration

`MARKSCHEME_DIR=/opt/athena/markschemes` was added exactly once to the root
owned, mode-0600 environment file. The directory is owned by `ubuntu:ubuntu`,
mode 0750, and has no Athena-specific NGINX exposure.

## Validation

- Frontend JavaScript syntax: PASS.
- `git diff --check`: PASS.
- Secret scan of changed frontend content: PASS; no secrets were introduced.
- Frontend commit pushed to `origin/main`: `db1a210ab90d2b5629430191ae60457f8c59282d`.
- Static release deployed at `/var/www/athena.airwide.co.uk/releases/20260827T225816Z-g2-auth`.
- Pre-G.2 frontend rollback release: `/var/www/athena.airwide.co.uk/releases/20260827T225816Z-pre-g2-auth`.
- Publicly served HTML matches the deployed release and contains the GIS,
  session-token, bearer, and 401-clearing paths.
- Protected API requests without a credential and with a clearly invalid
  bearer both returned HTTP 401.
- Real browser Google login: PENDING operator/browser interaction.
- Verified Google subject and `ATHENA_REVIEWER_PRINCIPAL_IDS`: PENDING.
- Athena restart for this tranche: NOT RUN pending verified reviewer subject.

The Google ID token and any reviewer principal are intentionally absent from
this report.
