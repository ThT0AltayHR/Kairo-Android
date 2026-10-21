# Kairo AI 0.3.0 — first manual QA pass

Status: source-only checklist. No Android build or device run has been performed in the authoring environment.

## Build and launch

- [ ] Open in Android Studio with Android SDK Platform 36 and JDK 17.
- [ ] Gradle sync completes; run `assembleDebug`, `lint`, and unit tests.
- [ ] Launch on Android 16 emulator and at least one physical device.
- [ ] Confirm status/navigation bars, keyboard resize, and dark theme render correctly.

## Gemini setup and model selection

- [ ] Save a valid Gemini key; force-stop/reopen; confirm key remains available.
- [ ] Load models; confirm error states for invalid key, no network, and empty model results.
- [ ] Search the model library by display name, model ID, and description.
- [ ] Change model during an existing conversation; confirm its selected model is retained.
- [ ] Change temperature and output-token limit; verify generated requests include the selected values and provider limit errors are shown clearly.
- [ ] Send a prompt, observe partial streaming, stop generation, then send a follow-up.

## Chat attachments

- [ ] Attach PNG, JPEG, WebP, PDF, and a UTF-8 text/JSON file.
- [ ] Confirm unsupported MIME types, >5 MB binary files, >256 KB text files, and a fourth attachment are rejected with a clear message.
- [ ] Confirm the attachment is only sent after the user taps Send.
- [ ] Confirm the composer permits removal before sending.
- [ ] Reopen an archived conversation: attachment metadata remains, payload is absent, and the file is not silently resent.

## Encrypted conversation archive

- [ ] Send multiple turns, leave the chat, force-stop, relaunch, and reopen the conversation.
- [ ] Search, rename, delete one conversation, and confirm the active chat remains coherent.
- [ ] Clear all history; confirm the archive screen and Home recent-chat list are empty.
- [ ] Inspect app-private storage in a debug build; conversation content should be encrypted, not readable JSON.
- [ ] Exercise rapid streaming and navigation to check that debounced saves do not overwrite another conversation.

## Prompt library and navigation

- [ ] Open the prompt library; switch category filters and insert a template.
- [ ] Edit the inserted prompt before sending.
- [ ] Open recent conversations from Home and the archive dialog.
- [ ] Verify all bottom navigation tabs remain reachable with large font and display scaling.

## Project, overlay, and widgets

- [ ] Select a project folder; verify hidden/build/dependency folders are skipped.
- [ ] Generate a proposal, inspect it, reject it, then separately approve a test change.
- [ ] Change the file externally and confirm stale-file protection refuses to overwrite.
- [ ] Test delete confirmation and cancel.
- [ ] Grant/revoke overlay permission; verify start/stop, rotation, background/foreground transitions, and notification controls.
- [ ] Add all 20 widget providers on supported launchers; verify labels, configuration, tap behavior, and state refresh.
- [ ] Confirm widgets do not claim continuous GIF animation.

## Accessibility and release checks

- [ ] TalkBack labels, focus order, contrast, 200% font scaling, and reduced-motion behavior.
- [ ] Verify error copy and network retry behavior.
- [ ] Review dependencies and license notices.
- [ ] Use a backend proxy before any public distribution of Gemini requests.
- [ ] Review Android foreground-service, overlay, privacy, and Play policy requirements.