# M14 — Multi-device sync implementation handoff

## Baselines and scope

- Android branch: `milestone-14-multidevice-sync`; HEAD remains `014b0c14991bf85d8d4a73d60496657379a86384`.
- Backend branch: `milestone-14-multidevice-sync`; baseline commit `326285f` (`Baseline Cloudflare Worker backend`). This is the only commit created for this task.
- Active backend: `D:\du an\ebook-backend\worker`. Legacy Kotlin/Ktor source was not changed.
- M14 implementation changes remain uncommitted. Nothing was pushed. No production deployment or physical device acceptance was performed.

## Profile isolation and Room migration

Room is now **6**, with a non-destructive **5 → 6** migration. Existing entity columns are retained; protocol tables are installed by the migration and Room open callback.

Each database has an explicit `sync_control.profile` owner. `DEVICE_LOCAL` retains the original `ebook_reader.db`. Accounts use `profile_<SHA-256(ACCOUNT:userId)>.db` and a corresponding private files directory. Existing M13 data is never assigned to the next login. It stays accessible in guest mode. Logging out switches profiles without deleting account databases/files; signing into the same account reopens its data.

The navigation tree is rebuilt on profile change. Repositories, file operations, download jobs and reading-session managers capture their originating profile. A background sync waits for auth initialization. Sync and ZIP operations verify the captured session's server user ID before transmitting profile data. Old jobs cannot use the newly signed-in user's token to upload another profile's data.

Shared global reader/library display preferences remain device settings. Per-book preferences are profile-owned and syncable. Custom fonts and custom local covers stay on their device; local cover overrides are preserved when remote metadata arrives.

## Entities and local mutation capture

Sync covers categories, documents/catalog metadata, reading progress, bookmarks, highlights and their notes, favorites, pin/archive/reading status, tags, collections, their independent memberships, review items, finalized reading sessions, and per-book preferences.

SQLite triggers execute inside the same transaction as each DAO insert/update/delete. The outbox records `(kind, local_key, revision, deleted)`, coalescing changes without losing the newest revision. Bookmark soft deletion creates a tombstone. Cascading deletes also produce tombstones. The trigger avoids `INSERT OR IGNORE`: the outer DAO `REPLACE` policy could otherwise reset the outbox revision.

Distributed IDs are deterministic UUIDs from entity kind and hex-encoded legacy primary keys. This preserves existing foreign keys and gives memberships an independent stable UUID. Individual new annotations/sessions already use UUID local IDs. A separate random operation UUID identifies each frozen outbound mutation.

Protocol tables:

| Table | Purpose |
|---|---|
| `sync_control` | Profile, remote-apply suppression, received watermark and applied cursor |
| `sync_outbox` | Latest local mutation revision and delete flag |
| `sync_pending` | Immutable request body/operation ID retained across process death |
| `sync_versions` | Local key ↔ distributed UUID and server revision |
| `sync_remote_heads` | Applied remote versions, including tombstones for unseen records |
| `sync_inbox` | Durably received changes waiting for dependencies/application |
| `sync_conflicts` | Preserved local/remote versions that cannot be merged safely |
| `sync_blobs` | Private file checksum, local path and upload completion |

## Protocol and conflict behavior

Cycle: verify session → push bounded batches → upload private blobs → acknowledge only the corresponding outbox revision → pull pages → apply in Room transactions. Frozen requests survive retries; newly edited revisions cannot be removed by an older receipt. `fetch_cursor` acknowledges durable receipt into the inbox; `cursor` advances only across successfully applied records. Missing dependencies remain in the inbox and are retried. Remote apply suppresses outbox echo and uses UPDATE instead of REPLACE to preserve child rows.

- **Progress:** bounds and timestamp validation; a newer valid timestamp can win over an older base version. A completed progression cannot regress to incomplete progress. Stale document state cannot clear completed status.
- **Annotations/notes/documents:** compare-and-set record revisions. Conflicting local bodies are kept in `sync_conflicts`, and the server also retains rejected operations.
- **Deletes:** permanent record tombstones reject stale recreation. Remote tombstones are remembered even before the original record is materialized. Unsent child data is preserved before parent cascade deletion.
- **Memberships/favorites:** independent records. Explicit re-add is allowed only after observing the exact tombstone version; stale replay remains rejected.
- **Reviews:** absolute state with version checks, monotonic review count/time, and operation dedupe. Retry never increments twice. Concurrent incompatible review states are preserved rather than summed speculatively.
- **Reading sessions:** only finalized sessions are pushed; append-only UUID dedupe. Subsequent edits are rejected, with rejected data retained. Existing stale-session recovery runs for the selected profile.

## Worker and D1

Migration: `worker/migrations/0003_sync.sql`.

- `sync_records`: account/kind/UUID primary key, revision and tombstone.
- `sync_operations`: account/operation UUID primary key; durable idempotency gate and rejected-operation retention.
- `sync_changes`: immutable, ordered feed with `(userId, seq)` index.
- SQL triggers atomically apply accepted operations and append feed records. The authenticated session is the sole source of account ownership.

| Route | Behavior |
|---|---|
| `POST /api/v1/sync/push` | Up to 50 operations, 512 KiB request, 32,768 characters per payload; operation receipts/conflicts |
| `GET /api/v1/sync/changes?cursor=…&limit=…` | Account-scoped feed, up to 100 changes, next cursor and `hasMore` |
| `PUT /api/v1/sync/blobs/{sha256}` | Checksum-verified private upload |
| `GET/HEAD /api/v1/sync/blobs/{sha256}` | Authenticated private download, range support |

Push and upload routes are rate-limited. Reusing an operation ID with different content is rejected. Client-provided owner fields are rejected; an account cannot read another account's feed or private files.

## R2, download and ZIP separation

Imported files use `users/{authenticatedUserId}/sync/{sha256}`. They are not reachable through `/assets/`. Public catalog books retain public download references instead of uploading another private copy. Metadata arrives before files; the personal library displays remote-only documents, and opening one triggers its download. Bootstrap never downloads the whole library.

Private uploads are streamed, checksum-validated and limited to 250 MiB by the route; the Cloudflare account's inbound request-size limit may impose a lower ceiling. No chunked large-file sync is implemented in M14.

ZIP backup/restore remains a separate manual disaster-recovery feature. It excludes protocol tables and other profiles' directories. Deleting a ZIP backup now deletes only its manifest/chunks, not sync blobs. Account deletion still deletes all of that account's private R2 data. ZIP restore retains the existing same-database-version restriction.

## WorkManager

- Vietnamese **Đồng bộ ngay** button and status in Settings.
- Network-constrained jobs, exponential retry starting at 30 seconds.
- 15-minute periodic work (Android decides actual execution time).
- Mutation-triggered scheduling with a two-second debounce.
- Application/auth initialization and foreground entry schedule sync.
- Account-specific unique work names plus a shared execution mutex prevent concurrent sync execution in this app process.
- Inactive account jobs do not sync. Existing account data is retained.

## Verification

Final gate results: **221 debug tests + 221 release tests**, zero failures/errors/skips; **lintDebug: 0 errors, 33 warnings**; **assembleDebug: BUILD SUCCESSFUL**. Worker: **11 test cases pass** (Node reports 12 including the shared helper file), dry-run check passes, fresh local D1 migrations pass. Both repositories pass `git diff --check`.

Commands run with JDK 23 because the default local JBR lacks the required build tooling:

```powershell
.\gradlew.bat '-Dorg.gradle.java.home=C:/Program Files/Java/jdk-23' test lintDebug assembleDebug --console=plain
```

Worker gates:

```powershell
npm test
npm run check
npx wrangler d1 migrations apply DB --local --persist-to .wrangler/m14-verified
```

Android tests cover migration with the real M13 entity schema, profile database isolation, transactional outbox rollback, revision-safe acknowledgement/DAO REPLACE, cursor rollback/dependency handling, durable request retry after reopening SQLite, bookmark soft-delete tombstones and independent memberships.

Worker tests cover authenticated account isolation, forged owner rejection, bounds, operation retries/reuse, cursor pagination/replay, CAS conflicts, no resurrection, explicit membership re-add, completed progress, review/session dedupe, private R2 authorization/checksum and ZIP deletion isolation. Tests use actual SQLite triggers through the D1 adapter; migrations were also applied through local Wrangler D1.

See `build/m14-gates.log`, `app/build/reports/tests/`, `app/build/reports/lint-results-debug.html`; Worker logs are under `.wrangler/m14-tests.log` and `.wrangler/m14-check.log` (ignored generated files).

## Remaining limits and QA handoff

- Physical two-device acceptance belongs to Antigravity and has not been run.
- M14 routes/migration have **not been deployed to production**. Deploy the migration and Worker to the intended QA backend before installing/testing the APK against that backend.
- The QA APK is `app/build/outputs/apk/debug/app-debug.apk`.
- Conflicting versions are retained in profile SQLite and server operation records. The app reports their count; a dedicated interactive conflict-resolution UI is not included.
- Unresolvable foreign-key/unique-name collisions stay in the durable inbox. They do not silently discard a record or falsely advance the applied cursor.
- No automatic garbage collection of tombstones, change feed or unused sync blobs yet. This avoids deleting data still needed by offline devices but requires a future retention policy.
- Custom fonts/covers and global device display preferences are not transferred. Imported document content, required reading state and annotations are covered.
- Oversized/missing local files and incompatible records remain local with a retry/error status; no silent truncation.
- Guest data is intentionally not merged into an account. Switch to guest mode to see pre-M14 local data.

Suggested physical acceptance: login A on both devices; import and annotate offline; reconnect and verify metadata before opening a file; retry with app termination/network loss; conflict two edits; delete while the other device is offline; verify completion does not regress; re-add a removed tag; switch A → guest → B → A; confirm private content and in-flight downloads never cross profiles; verify ZIP deletion leaves synchronized documents intact.

## Final Git state

Android: 18 tracked files modified; new `data/sync/` sources, `MultiDeviceSyncTest.kt`, the `room-v5.sql` fixture and this report are untracked. HEAD is unchanged at `014b0c1`. Backend: 3 tracked Worker files modified (`src/index.js`, `src/storage.js`, `test/services.test.js`); 5 new Worker files are untracked (`M14.md`, migration, sync routes, test adapter, sync tests). HEAD is `326285f`. No M14 files have been staged or committed. The legacy Ktor paths have no diff.

M14 IMPLEMENTATION READY FOR ANTIGRAVITY PHYSICAL QA

## QA core acceptance — 2026-09-09 (supersedes earlier physical-QA status)

QA only: https://nocap-ebook-api-qa.buiminhhien001.workers.dev, D1 nocap-ebook-db-qa (9d8d67d9-eab0-48b6-aae9-014d67052529), private R2 nocap-ebook-files-qa. Migrations 0001–0003 applied; deployed version 87989674-0a41-43ef-8856-dd7d6616d24c. Debug endpoint is QA; release endpoint unchanged. No production deployment or resource modification.

Physical A: R5CX10EY94W (S24+); B: RF8NC11QWJB (M51). Both updated using adb -s SERIAL install -r; no uninstall/data clear. Real Room/SyncEngine/QA integration tests passed A→B for private document, progress, bookmark, highlight/note, tag, collection, review and finalized session; B→A note edit and position block 60 passed. M51 opened cloud-only TXT through the app: content downloaded and rendered, paragraph 60 visible. Locator transfer is exact; text viewport restoration is near-exact (one paragraph offset, character offset is not restored precisely).

Passed: offline edits/reconnect; durable pending request force-stop/relaunch/retry; concurrent note edit retained as conflict; completed progress non-regression; repeated review/session dedupe; account/guest database isolation and return to A; live QA API cross-account private R2 denial. UI testing was limited to library, sync status and cloud-only text reading; mutations were primarily physical instrumentation, not exhaustive manual UI workflows.

Defect found and fixed: a finalized reading session received after its book tombstone remained indefinitely in the durable inbox due to the missing parent. SyncEngine now preserves the payload as REMOTE_CHILD_AFTER_PARENT_DELETED and advances the remote head without resurrecting the book. The original failing device state was retested on both phones: no book/note resurrection, pending/inbox empty. SQLite inspection confirmed the affected session preserved exactly once in conflicts. No fixture data or inbox rows were manually removed to make this pass.

Validation: testDebugUnitTest 221/221; testReleaseUnitTest 221/221; lintDebug and assembleDebug successful. Worker npm test 12/12 and npm run check successful (dry run). Initial physical fixture locator JSON was corrected to use TextLocator.toJson; the reverse edit test pulls the latest base first. Transient ADB disconnects and DNS recovery caused preliminary harness failures; successful reruns are recorded.

Stability: sampled logcat from both devices contained no NoCap FATAL EXCEPTION, ANR, SQLiteException or SecurityException. Unrelated Android telephony/Zalo security messages were excluded. Sync workers completed successfully; no persistent retry loop observed. This is a bounded core test, not a long-duration soak.

Evidence (ignored generated output): build/m14-qa-*-*.log, build/m14-qa-final-gates.log, build/m14-qa-b.png, build/m14-qa-a-logcat.txt, build/m14-qa-b-logcat.txt; Worker .wrangler/m14-final-tests.log and m14-final-check.log. QA credentials remain only in ignored private files.

Remaining limitations: EPUB/PDF cross-device visual locator QA and exhaustive manual profile/download switching remain final QA scope; conflict data is retained but has no merge editor. No observed open P0/P1 from the exercised core cases. No commit/push; both repositories remain on milestone-14-multidevice-sync with uncommitted M14 work preserved.

M14 CORE ACCEPTANCE PASS — READY FOR ANTIGRAVITY FINAL QA

## Final commit audit

Final physical QA PASS was supplied by the project owner at commit handoff; the core QA evidence above remains the scope directly verified by Codex. This commit changes no accepted application behavior.

Maintained physical coverage: M14PhysicalSyncTest uses generated TXT content and requires explicit instrumentation arguments `phase`, `scenario` (unique lowercase letters/digits/hyphens), `email`, and `password`. Use dedicated QA accounts only. Build/install the debug and androidTest APKs with `adb -s SERIAL install -r`; invoke `com.nocap.app.test/androidx.test.runner.AndroidJUnitRunner` with class `com.nocap.app.M14PhysicalSyncTest`. Supply credentials privately at runtime; never save commands containing them in tracked files.

Sequence: seed on A, receive on B, edit-back on B, verify-back on A. Offline phases require host network control; conflict-winner runs on A while B holds offline-conflict, then verify-conflict on B. Complete/regress-completed and dedupe cover monotonic progress and retries. Delete/verify-deleted run last. Stage-pending requires host force-stop after M14_PENDING_DURABLE, followed by retry-pending after relaunch. Isolation runs with a separate QA account. Use a new scenario for each full run. These are explicitly orchestrated integration phases, not a standalone connectedAndroidTest suite.

One-off EPUB/PDF fixture phases from final QA were excluded from the maintained test: they depended on absent external Download/test.epub and test.pdf files and a hard-coded EPUB spine. Their original source is preserved locally in ignored build output. No downloaded fixtures or temporary host scripts are committed.

Commit gates: 221 debug + 221 release unit tests, lintDebug, assembleDebug and assembleDebugAndroidTest; Worker npm test and npm run check; whitespace and staged artifact/security audit. Production is not deployed by this commit. Debug intentionally uses QA (whose public catalog is currently empty); release configuration is unchanged.
