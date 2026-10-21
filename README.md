# Kairo AI — Android source project (0.3.0 prototype)

Kairo is an original, Gemini-first Android companion prototype for Android 16. It borrows broad interaction ideas (a compact status island, visible task states, and configurable home widgets), not Coucou's name, mascot, sounds, or visual assets.

## Included in this source build

- Native Kotlin + Jetpack Compose dark UI with an embedded Noto Sans typeface and original launcher mark.
- Five original animated plush-mascot GIF reactions: idle, thinking, done, error, and question. The same reactions are used in the app and floating island.
- Gemini API key entry, encrypted local storage using Android Keystore (AES-GCM), live model discovery through the Gemini models endpoint, searchable model-library picker, and streaming chat replies with a stop control.
- Encrypted, local conversation archive with search, rename, delete, recent-chat resume, and clear-history control. Up to 24 conversations / 80 messages each are kept (archived message text is capped at 12,000 characters per message). Attachment payloads are not saved; only attachment names and sizes remain in restored chats.
- A categorized prompt library with 12 editable starters for Android UI, Compose, debugging, code review, tests, architecture, accessibility, security, performance, and product writing.
- Per-request generation controls for temperature and output-token limit; the API/model may enforce lower limits than the selected preference.
- A before/after file review panel beside the existing explicit write confirmation and stale-file check.
- Chat file attachments: PNG/JPEG/WebP images and PDFs up to 5 MB each, plus text/JSON files up to 256 KB; up to three files may be attached to one message. Attachments are read only after the user chooses them and sent to Gemini with the message.
- A project workspace using Android's Storage Access Framework. The user explicitly selects a folder; Kairo lists supported text files (up to 32 KB each; hidden/build/dependency folders are skipped), lets Gemini propose a full-file replacement, and **never writes or deletes until a confirmation**. Before writing, the app compares the current file against the version that was opened and refuses to overwrite if it changed.
- Optional adjustable floating status island (width, height, top offset), with animated expand/collapse and status mascot; asks for Android's “Display over other apps” permission and runs as a foreground service with a notification.
- **20 separate launcher widget entries**, each with its own original dark visual preset and per-instance setup. Users can add multiple styles. Widgets update on app events, not continuously.
- User-visible progress labels and errors. The app deliberately does not expose hidden model chain-of-thought.

## Important limits and safety notes

- The Gemini model picker lists generation-capable models returned for the user's API key. It cannot promise every Google model or a model that the account/API key cannot access.
- This prototype sends requests directly from the Android client using the user's own key. The key is encrypted at rest, but a key used by a shipped client app can still be extracted. Google recommends a backend proxy for production. **Do not distribute this direct-key build publicly as a production app.**
- “Offline” is a connection/status indicator only; this project does not bundle an on-device Gemini model.
- The island is an Android overlay, not a native iPhone Dynamic Island or a replacement for Android's status bar. The user must grant overlay permission. Foreground-service and Google Play policy declarations must be reviewed before distribution.
- Android launcher widgets use restricted `RemoteViews`; the 20 presets are visual styles, not 20 continuously animated GIF players. They refresh on status changes. Continuous mascot animation is in the app and floating island.
- The island shows that a project change needs review and opens the app when tapped; confirmation buttons are in the app, not inside the overlay.
- File operations are scoped to the folder the user chooses. The project workspace is intentionally text-only and size-limited. Chat attachments are a separate, explicitly selected feature. There is no shell access, IDE integration, background agent, auto-run code, or unrestricted file access in this prototype.
- The source has not been built or exercised on a physical Android 16 device in this environment. Before release, run Gradle sync, tests, lint, and device testing; verify the overlay lifecycle, Gemini key restrictions, widget behavior across launchers, and file-provider behavior.

## Build in Android Studio

1. Open the `KairoAI` folder as a project.
2. Install Android SDK Platform 36 and use JDK 17 (Android Studio's bundled JDK is suitable).
3. Let Android Studio sync the Gradle files and download the declared dependencies.
4. Run the `app` configuration on an Android 11+ device. Android 16 is the target configuration.
5. In **Ayarlar**, enter your Gemini API key and tap **Modelleri yükle**. In chat, tap the model chip to search the available models; use the attachment icon to select a supported file.
6. Start a conversation, reopen it from the chat-history button, rename/delete it, and try the prompt library from the empty-chat screen.
7. Choose a folder in **Proje** to test the file proposal workflow.
8. Enable the floating island from **Ayarlar** and grant **Display over other apps** when Android opens the permission screen. Return to the app and tap enable again.
9. Add a Kairo widget from the launcher and choose one of the 20 styles.

The sandbox used to author this ZIP has no Android SDK or Gradle installation, so this delivery is source-only; no APK is included. Use `QA_CHECKLIST.md` for the first Android Studio/device test pass.

## Gemini API behavior

The app calls `models.list`, filters for `generateContent`, and uses `streamGenerateContent` with `x-goog-api-key` in the request header. The model list is fetched dynamically and paginated; model availability depends on the user's project, region, key restrictions, and Google's current API catalog.

## Assets and notices

- Mascot GIFs are generated by `tools_generate_mascot.py` and are original Kairo artwork.
- The bundled Noto Sans font is distributed under Apache License 2.0; see `licenses/Apache-2.0-Noto-Sans.txt`.
- This project is not affiliated with Google or Coucou.

## Scaling principle

The project is expanded by user-facing features and maintainable boundaries, not by generating placeholder files. Ten thousand source files would not make this app more capable; the roadmap in `PRODUCT_ROADMAP.md` prioritizes testable modules, privacy, reliability, and a coherent interaction design.
