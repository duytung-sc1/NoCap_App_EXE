# M16 — Product polish

Android baseline f1fcaf6; backend baseline 3a792af. Android branch milestone-16-product-polish. Backend is unchanged. Final M16 commit is authorized; no deployment or push. Real Play purchases remain deferred.

## Source audit

No new P0/P1 identified by source inspection. The user confirmed physical UX QA PASS on M51 and visual verification of the final launcher icon. Automated gates alone do not establish visual contrast, clipping, or resume accuracy on devices.

P2 findings addressed:
- Home had no explanation when the catalog was empty/unavailable. It now offers catalog navigation and explains that local documents remain available.
- Library fixed-width tabs were crowded; tabs now scroll. Filtered empty results no longer claim the entire library is empty; search/filter guidance explains the next step. Empty-library actions stack and content scrolls for large fonts.
- Read/continue, inbox action, overflow, annotation colors and font controls had small explicit bounds. Important controls now have 48dp bounds/minimum height.
- Reader color choices lacked accessibility names/selection semantics. Shared EPUB/PDF/text color picker now exposes Vietnamese labels and radio selection.
- Text reader error had no visible recovery action. Reusable WorkspaceState provides a scrollable explanation and return action. Long press exposes the highlight/note action to accessibility services.
- Memory led with statistics. Saved knowledge/search now leads; statistics remain accessible through the existing toolbar.
- Account actions crowded a row. They now stack, with explicit logout/profile data-retention wording.
- Sync messages exposed record/conflict details and exception messages. Copy now describes pending changes/retry while preserving the scheduler and data handling.
- Unconfigured Pro upgrade looked actionable. It is disabled with an explanation; restore requires a signed-in account and is disabled during loading.

## Existing flows retained

Home continue-reading, import picker/URL/duplicate/cancel flows, EPUB/PDF navigation and annotation sheets, text locators, tags/collections, Knowledge Search/review/global annotations, local/profile storage, entitlement verification and cloud operations retain their implementations. Reader engines, M14 locator serialization and database schema are unchanged. Material3 light/dark/sepia tokens are reused rather than introducing a new palette.

## Physical UX QA checklist

Launch → Library → add local and HTTPS documents → EPUB/PDF/text reading → bookmark/highlight/note → Memory/Search → return to exact location. Check light/dark/sepia, TalkBack, landscape, 100%/150%/200% font sizes, library action wrapping, annotation color selection and content below reader chrome. Check first-launch empty catalog separately from the personal library; no QA catalog data is copied.

Check Guest/A/B/A isolation, logout wording, no-network sync and recovery, expired Pro local reading, cloud-only download and missing-file recovery. Sync does not yet expose a reactive network-specific label while WorkManager waits for connectivity; the current copy offers connection/retry guidance. Real Play billing is intentionally unavailable until configured.

The checklist above is retained for future regression checks on other devices; M51 physical UX PASS is user-reported.

## Launcher icon

Original nocap_foreground.png and nocap_monochrome.png are preserved in drawable-nodpi. XML inset wrappers keep artwork inside the adaptive safe zone. Navy background is #050B20. API 26 adaptive and API 33 monochrome variants include both ic_launcher and ic_launcher_round; the manifest references these resources. minSdk remains 26. No text or generated preview images are included.

## Final automated gates

464 unit tests (232 debug + 232 release), zero failures/errors. lintDebug zero errors; assembleDebug and assembleDebugAndroidTest PASS. Full requested Gradle command completed successfully after the final source change; log is ignored at build/m16-final-gates.log. git diff --check PASS. Existing D8 Kotlin-metadata warnings remain non-fatal; no toolchain upgrade is part of this polish. Generated .kotlin session state is ignored.
