# M17 security hardening audit

Baseline: Android `76f0840` on `milestone-16-product-polish`; Worker backend
`3a792af` on `admin-dashboard`, including the intentional uncommitted admin work.
Final audit: M17-only commits authorized; no reset, push, remote migration or code deployment.

## Findings and changes

| Priority | Finding | Resolution in working tree |
| --- | --- | --- |
| P1 | Crafted backup paths could reference another local profile; blob-encoded text could bypass string validation | Restore confines local paths to extracted files, validates cover URLs, permits binary values only for BLOB columns, and binds the database to the captured profile. Document IDs are checked before filesystem use. |
| P1 | Startup session restoration was not serialized with logout/login | The same auth mutex now covers restoring and publishing the startup session. |
| P1 | Temporary short admin password accepted on production | Production password login now rejects passwords shorter than 12 characters. Existing strong-password and Google login remain available. Remote secrets unchanged. |
| P2 | Session and entitlement snapshots were eligible for OS backup/transfer | Excluded both preference files in legacy and Android 12+ rules. Local documents and databases remain eligible; manual backup is unaffected. |
| P2 | Upload relied on declared length | Count actual streamed bytes and use Cloudflare FixedLengthStream for R2. No full-document buffering. |
| P2 | Arbitrary nonexistent auth paths allocated rate-limit rows | Route allowlist checked before the limiter. |

## Verification

- Android: 235 debug + 235 release unit tests, zero failures/skips; lint zero
  errors (36 warnings, one hint); debug and androidTest APK assembly pass.
- Worker: 34 Node test runner cases pass, including existing M14/M15/admin
  regressions and seven new adversarial cases; Wrangler dry-run passes.
- Native local workerd/R2 smoke passes: exact-length stream accepted;
  over/under-length streams rejected without objects. No remote resource used.
- Production-dependency `npm audit --omit=dev`: zero reported vulnerabilities.
- Both repositories pass `git diff --check`; nothing staged.
- No M17 migrations. Existing additive D1 migrations run in isolated in-memory
  test databases, including FK/account-deletion regressions. No production data
  or private R2 objects touched; legacy Ktor unchanged.

Negative tests cover absent/foreign/expired/revoked tokens, disabled accounts,
forged owner fields, private R2 cross-account reads, malformed/oversized JSON,
invalid IDs/keys, upload length/checksum mismatch, retries, billing without
configuration and short production admin passwords. Existing tests cover
tombstones, conflict retention, completed progress, review/session dedupe,
Google purchase ownership/acknowledgement, expiry and recovery.

Session hashes and encrypted purchase proofs remain server-owned. Android
sessions use the Keystore. Source logging does not print raw tokens/passwords;
Worker unexpected errors log the error type only. Production and QA bindings
remain separate, Release uses production and Debug uses QA. Billing stays
configuration-driven and unavailable without real setup.

## Boundaries and remaining work

- No known unresolved P0/P1 in the audited source changes. This is not a claim
  that the currently deployed production version has received these fixes.
- **Production prerequisite:** the old short admin password remains a P1 risk
  until rotated or password login disabled in favor of Google. Configure a
  strong secret or Google Web OAuth before deploying M17, otherwise password
  login with the old credential will intentionally fail. No secret was changed.
- Physical QA still needs startup/logout/account switching and restore of a
  genuine backup. Android build warnings include D8 Kotlin metadata warnings;
  builds pass, but they are not a substitute for device execution.
- Remaining P2: cumulative private sync storage quotas/retention and distributed
  abuse controls; recovery UX for authorized but incompatible sync payloads;
  offline logout cannot revoke a remote session until the server is reachable
  (local session is cleared, existing server expiry still applies).
- Real Play purchase tests, live Cloudflare policy/secret review and deployment
  smoke are deferred. No release signing or APKPure work performed.

References used for platform behavior:
- https://developer.android.com/identity/data/autobackup
- https://developers.cloudflare.com/workers/runtime-apis/streams/transformstream/

## Final cleanup and commit separation

All three temporary QA Pro secrets were deleted and authenticated physical A returned FREE. The temporary override code/config/tests were removed locally; no purchase or schema was fabricated. No code deployment was performed, so the previously deployed QA bundle still contains the disabled override until a future QA deployment.

Admin dashboard remains uncommitted. M17 commits the standalone production-password policy and its test; dashboard wiring to that policy stays with the uncommitted dashboard. Deploying the backend commit alone does not ship the dashboard.
