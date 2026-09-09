# M15 — Free / Pro implementation

Base `0f204b0`, branch `milestone-15-pro-entitlements`. Room remains version 6; no local schema or data migration is needed. M1–M14 local reading features remain Free.

Google Play Billing Library: `com.android.billingclient:billing-ktx:9.1.0`, the current release verified against Google's release notes. Product ID is the optional Gradle property `PLAY_PRO_PRODUCT_ID`; it defaults to empty and the UI explains that billing is not configured. No fictional product IDs or prices are shipped.

`data/billing/Entitlement.kt` centralizes feature policy, account-scoped entitlement serialization/cache and purchase outcome classification. `EntitlementRepository` verifies the captured session identity with `/me`, fetches the server state, rejects responses for a different active account, and keeps cache entries separate by hashed account key. Cached state is display-only, valid for at most 24 hours and never beyond expiry; cloud calls require a fresh server check. Small device/server clock skew is tolerated; the backend remains authoritative.

`PlayBilling` reconnects on foreground and queries current subscriptions, submits PURCHASED tokens to the Worker, reports PENDING without granting access, preserves state on cancellation, and reconciles ITEM_ALREADY_OWNED/restore. Purchase flow sets the server-derived obfuscated account ID. The Worker acknowledges; Android never grants Pro from a Purchase or boolean alone. Raw tokens are neither logged nor persisted by Android. Reinstall/another device uses the authenticated server account and Play restore.

Settings shows Free/Pro, purchase/restore controls, loading and error states, pending purchases and paid-period/renewal information. There is no elaborate paywall. Local data remains usable if Play is unavailable or Pro expires.

M14 SyncEngine checks central cloud access before freezing/sending outbox operations. Private downloads and cloud backup access also check the same policy. PRO_REQUIRED stops that sync run without removing pending/outbox/local rows; periodic, foreground and manual sync recheck access. Network verification failures retain normal retry behavior. Expiry in the middle of a batch is also enforced by the Worker. Already-downloaded files are read without entitlement checks. Backup deletion is available to Free.

Tests cover parsing/cache restart/corruption and account isolation, Free local access, expiry/revoke/offline stale cache, purchase states, restoration and local/pending preservation. Existing M14 tests remain. Worker tests cover default route wiring with mocked Google APIs as well as entitlement policy and M1–M14 regressions. These automated tests are not a substitute for Play physical acceptance.

## External setup before Antigravity QA

See `D:\du an\ebook-backend\worker\M15.md` for service account, package/product, encryption secret and QA migration setup. Use a real Play internal test track and license testers, with product IDs matching Android and Worker. No actual product ID or service-account credential was available during implementation. No production deployment, push or commit is performed.

Final physical QA should test pending/cancel/restore, real test renewal/refund/revoke, Pro A/Free B/Guest/A switching, same account on another device, expiry offline and while the M14 outbox is pending. Verify that local reading and notes remain usable throughout. Existing QA M14 accounts default to Free after M15 migration; do not add a production bypass to make old physical scripts pass.

Known limits: no RTDN; server reconciles Google on access. Google outage restricts cloud operations but retains local data. New purchases must have the NoCap account binding; migrating previously unbound subscriptions is outside scope. Real billing/physical acceptance remains blocked until external Play setup is provided.

Final automated gates: 228 debug + 228 release unit tests PASS, lintDebug 0 errors, assembleDebug and assembleDebugAndroidTest PASS. Worker 18 runner entries PASS (including test helper), npm run check PASS. Git whitespace and new-file artifact/secret-pattern audit passed. Logs are ignored under build/m15-final-gates.log and Worker .wrangler/m15-*.log. No known open P0/P1 in the exercised implementation; real Play/physical QA is still required.

## M15 core QA

Current uncommitted M15 was tested with local doubles only; no deployment/commit/push. Two failing backend regressions were reproduced before fixes: verifying a second pending token could return an unreconciled older Pro purchase, and missing Google acknowledgement state could be accepted. The verify/restore response now reconciles all stored references, and Google responses fail closed on missing/unknown state or invalid active expiry. Encryption configuration is checked before acknowledgement.

Android restore now continues past an individual invalid/foreign token and still reconciles server state. Account changes suppress stale restore messages, and terminal purchase failures clear the captured launch account. Reconciler tests cover partial failure, empty Play query, cancellation and server restore failure.

Core gates: 232 debug + 232 release tests PASS; lintDebug 0 errors; assembleDebug and assembleDebugAndroidTest PASS. Worker 20 runner entries PASS; npm run check PASS. The actual Worker route test includes acknowledgement failure/retry, Pro expiry blocking cloud, retained sync data and safe reactivation/replay. Existing M14 regression tests remain passing. Git diff --check passes. No known open P0/P1 in exercised core cases.

Real Play product/base plan, service-account secrets, signing/internal test track and license testers remain external blockers; no real-money purchase or physical Play QA is claimed. Logs: build/m15-core-gates.log and Worker .wrangler/m15-core-*.log (ignored).
