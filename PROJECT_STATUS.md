# Project status

## Prototype implemented

- Gemini key storage, dynamic model list, searchable model picker, streaming chat with cancellation, and selected chat attachments (images, PDFs, text/JSON) with size/type limits.
- Android Keystore-encrypted conversation history with archive search, rename, delete, recent-chat resume, and clear-history control; attachment payloads are deliberately not persisted.
- Categorized 12-template prompt library, home-screen recent conversations, and temperature/output-token controls.
- Before/after source-file comparison panel before the existing explicit apply confirmation.
- User-selected project folder, text-file list, Gemini replacement proposal, review-before-write, explicit delete confirmation, stale-file guard.
- Optional overlay pill, size/position sliders, mascot animations, 20 configurable widget themes.

## Product work still needed before a public release

- Decide the app's permanent name, visual direction, supported Gemini model features, and privacy policy.
- Secure production architecture: backend proxy, per-user authentication, quotas, billing controls, abuse limits, and key rotation.
- Add project-wide context and multi-file patch generation with a reviewable diff; this prototype edits one selected text file at a time.
- Add automated unit/instrumented tests, Android 16 device testing, accessibility checks, localization, crash handling, and release signing.
- Confirm foreground-service and overlay policy eligibility for the intended distribution channel.
- Consider a backend/notification bridge for longer background tasks; this prototype streams while the app is active.
- Expand the encrypted archive with a versioned database migration path and retention controls; add attachment previews and diff presentation before writes.
- Conversation history and prompt-library foundations landed in 0.3.0; next: real diff review, image/PDF previews, retry/regenerate, and automated tests.
