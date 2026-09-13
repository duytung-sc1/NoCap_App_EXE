# M18 reliability and performance audit

Base: `931ed67`. Changes are uncommitted. Existing productionPreview configuration is preserved. Backend, billing and sync protocol are unchanged.

## Fixes

- Text/CBZ readers wait for initial position restoration before writing progress. Failed loads do not overwrite saved text progress with zero.
- Text locators preserve a pixel offset within a block, with backward-compatible default zero; sampled writes limit scroll-driven DB traffic. Pixel offsets are approximate across different font sizes/devices.
- CBZ disposal writes and EPUB/PDF final position writes use a process-lifetime scope. Reader writes are serialized within the ViewModel and reject older timestamps.
- Reader reload cancels its previous load job; unexpected load failures show a recoverable state; search iterators close in finally.
- Publication manager holds application context.
- Imports serialize duplicate detection, preserve cancellation, close resources and clean partial copy files. Catalog/download inserts share a Room transaction.
- Remote imports have a total call timeout and close responses in finally.
- Local filenames containing `#` or `?` retain their extensions. URL query/fragment stripping remains supported.
- Home list combination runs off the main thread.

## Automated evidence

`test lintDebug assembleDebug assembleDebugAndroidTest assembleProductionPreview`: PASS.
247 tests per variant (debug, release, productionPreview): 741 executions, zero failures/errors.
Lint: zero errors; 36 warnings and one hint. `git diff --check`: PASS.

Negative tests cover network/DNS/timeout failures, server-error retry, empty response, cancellation cleanup, old/malformed text locators and special filenames. Existing sync/outbox/isolation and parser regression tests pass. This does not substitute for live cloud-sync fault injection.

## S24+ physical evidence

Serial: `R5CX10EY94W`. Preview and instrumentation APKs installed with `-r`; no uninstall/data clear. Preview continues to use production.

M18ReliabilityTest: **3/3 PASS** on September 13. Isolated in-memory Room and generated temporary fixtures verify cancellation/provider-failure cleanup, malformed/empty/missing files, concurrent duplicate import, Unicode filenames, transaction rollback/retry and 3,000-document search. Test fixtures are deleted after execution; no account data or backend writes in these tests.

- 3,000-document search: 192 ms (previous run 193 ms).
- Android cold launch: before 175 ms; after 159/182/170 ms across informal runs. Not a controlled speedup claim or time-to-full-display measurement.
- Background-to-foreground: 23 ms Android wait time.
- PSS: initial baseline ~92 MiB; after launch ~101 MiB; after text/configuration smoke ~135 MiB. A prior EPUB open reached ~274 MiB. Different screen states; not a leak proof.
- HTML reader position before/after force-stop and reopen: same visible paragraph and UI bounds.
- Font scale 1.3: reader stayed usable; original setting restored.
- Local HTML open with airplane mode enabled succeeded; previous airplane setting restored afterward. This is offline local-reading smoke, not proof of disconnected Wi-Fi or live cloud retry.
- Current-process logcat scan: zero FATAL EXCEPTION, ANR, SQLiteException, SecurityException, or obvious Bearer-token matches. This is not a comprehensive privacy audit.
- Short gfxinfo sample including lifecycle/configuration work: 122 frames, 15 janky (12.3%); p50 5 ms, p90 31 ms, p95 73 ms. No steady-state scrolling claim.

## Remaining final-QA work / limits

No unresolved P0/P1 was reproduced in the executed checks; full milestone acceptance remains subject to the unexecuted cases below.

- Long-run scrolling/memory and separately measured Home/Library/reader/catalog rendering; jank is a P2 investigation item.
- Full EPUB/PDF/CBZ visual-resume matrix, annotations/bookmarks after interrupted sessions, rotation and dark-mode matrix.
- OS process kill during import (coroutine cancellation was tested), revoked content-provider permissions, and maximum-size files on constrained storage.
- Live Pro sync network loss/process restart, duplicate WorkManager execution, cloud-only download and tombstone replay. No Pro override was added and no production backend was modified.
- Existing parsers/search still load document/library data in memory. Home mapping was moved off main; no pagination redesign was attempted.
- Hard process death can leave orphan import files before DB commit. The tested DB failure is atomic and retry succeeds; automatic orphan cleanup is not implemented.

No commit, push, release key creation, or backend deployment performed.

## M51 user-reported regression follow-up (September 13)

Device `RF8NC11QWJB`, productionPreview installed with `-r`, existing data preserved.

- Fixed missing translations for Library filters/sorts, built-in category badges and observed Settings/search labels. All five language selections were exercised on M51; Library filter labels changed correctly. Catalog descriptions and text baked into cover images remain source content.
- Replaced the constrained selection IconButtons with horizontally scrollable TextButtons. Long-press selection showed all five actions with complete labels in English and Vietnamese. No separate image-document selection case was executed; it uses the same Library selection toolbar.
- Reproduced category `INSERT OR REPLACE` failure with an existing book's restrictive foreign key. Category upsert now updates in place. Download-start failures are caught and presented as retryable errors, with cancellation propagated.
- Instrumentation: **4/4 PASS** on M51, including category update with existing books; 3,000-document isolated search 235 ms.
- Actual app downloads: Alice's Adventures in Wonderland followed by Pride and Prejudice both reached Read book. Alice opened in the EPUB reader. Current-process serious-error logcat scan returned zero matches. This does not establish that every catalog book downloads successfully.
- Final automated gates: 248 tests per variant / 744 executions, zero failures/errors; lint zero errors (36 warnings, one hint); debug, androidTest and productionPreview builds PASS; diff check PASS.
