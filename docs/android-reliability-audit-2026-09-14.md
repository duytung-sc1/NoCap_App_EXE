# Android reliability audit — 2026-09-14

Scope: download/import, DOCX/CBZ readers, cache lifecycle, account startup,
sync scheduling/outbox handling and local search. Backend unchanged.

## Confirmed issues addressed in one batch

- Empty successful HTTP bodies could be recorded as completed downloads.
- Failed HTTP responses were not closed in the download worker.
- Coroutine cancellation entered the generic download failure path.
- Missing download URLs overwrote existing download metadata before failure;
  metadata now falls back to the profile database when absent from public catalog.
- Every DOCX drawing reused the first extracted image. Embedded relationship IDs
  now select the correct local image; external relationships are not fetched.
- DOCX reopening created new timestamp-named copies of every image.
- CBZ page cache omitted file/profile identity and content revision, and a failed
  extraction could leave a partial cache entry. Cache keys now include canonical
  source path, size, timestamp, entry and reader revision; extraction uses a
  temporary sibling file before replacement.

## Evidence

`DocxImageRegressionTest` failed before the parser change and passes afterward.
It covers two distinct embedded images in reversed relationship order, an external
image reference and repeated parsing without duplicate cache files.

`ReaderDownloadRegressionTest` covers empty/truncated/unknown-length download
validation, profile/revision/page cache separation and failed extraction retaining
an existing cache file without leftover temporary files.

`M18ReliabilityTest` additionally checks that attempting to download a local
document with no URL preserves its existing file and metadata.

Automated gates: 259 tests per Debug/Release/ProductionPreview variant, no failures;
lint has 0 errors (36 warnings, 1 hint). Debug, instrumentation and Preview builds
pass. M51 physical instrumentation passes 11 tests (`M18ReliabilityTest` and
`SharedPublicationIntentTest`), including a 3,000-document search in 311 ms.
Tests use isolated fixtures; this is not a visual acceptance pass for every reader.

## Limits and follow-up concerns

- Auth, sync and search received source review, not a new end-to-end Pro/account
  acceptance suite. No billing, auth, sync schema or server behavior changed.
- Worker process death during final file replacement and overlapping cancel/retry
  still need controlled network/WorkManager fault injection. This batch does not
  claim to resolve every interruption race.
- Cache revision keys prevent reuse of stale files but do not implement a bounded
  eviction policy. Old OS-cache entries can remain until cache cleanup.
- Successful stream validation does not prove a file is a supported publication;
  readers still perform format parsing on open.
- Existing Kotlin metadata/toolchain warnings remain; no toolchain upgrade was
  mixed into the reliability changes.
