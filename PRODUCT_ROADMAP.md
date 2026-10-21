# Kairo AI — product and engineering roadmap

## Product target

A native Android AI workbench for people building apps and websites. The app should feel alive, but every animation must clarify a state or action. Gemini is the first provider; the product boundary should allow other providers later without rewriting the UI.

## Implemented in 0.3.0 prototype

- Searchable model library populated from the current Gemini API key's accessible models.
- Streaming chat with stop control.
- Explicitly selected image, PDF, and text/JSON attachments with strict size and MIME limits.
- Encrypted local conversation archive with search, rename, delete, restore, and attachment-payload exclusion.
- Categorized prompt library, recent conversations on the home screen, and configurable generation temperature/output limit.
- User-selected source folder with single-file proposal, review-before-write, explicit delete, and stale-file protection.
- Adjustable overlay and 20 home-screen widget styles.

## Feature sequence

### 1. Conversations

- Persistent conversation list with titles, search, pin/archive/delete, and model used per conversation.
- Rename/new-chat flows and safe clear-history confirmation.
- Export/import in a documented portable format.
- Local database migrations and an explicit retention setting.

### 2. Rich chat

- Image/PDF previews, attachment removal before sending, upload progress, retry, and cancellation.
- Markdown/code rendering, syntax highlighting, copy/share, and per-message regenerate/edit.
- Show model-specific input/output capabilities and whether the selected generation settings are supported before sending.
- Accessible message navigation, font scaling, and reduced-motion option.

### 3. Project workspace

- Multi-file context picker with token/cost estimate and visible included-file list.
- Reviewable unified diff, per-hunk accept/reject, backups, undo, and conflict detection.
- Git status/diff integration through a constrained library; no arbitrary shell execution.
- Project profiles for Android, web, and general coding, with user-editable instructions.

### 4. Agents and tasks

- Explicit task queue with pause/cancel/retry, bounded tool permissions, and visible event timeline.
- User approval before every file write, delete, network action, or other consequential tool.
- Background execution only after Android platform/policy review, with a persistent notification.
- No fabricated “thinking steps”; show real tool events and user-visible progress only.

### 5. Reliability and production

- Backend proxy for API credentials, authentication, quotas, abuse controls, and key rotation.
- Unit, contract, UI, and instrumented tests; Android 16 device/launcher matrix.
- Offline/error states, retry/backoff, telemetry with consent, crash reporting, and privacy controls.
- Accessibility, localization, release signing, dependency/licence audit, and Play policy review.

## Architecture direction

Keep feature code cohesive rather than splitting every class into a separate file. As the prototype grows, migrate in small compilable steps toward:

- `core:designsystem` — tokens, typography, components, motion, accessibility.
- `core:model` — provider-neutral messages, attachments, task events, permissions.
- `core:storage` — Room migrations, encrypted secrets, file metadata.
- `core:network` — provider clients, streaming, retries, backend contract.
- `feature:chat`, `feature:models`, `feature:projects`, `feature:activity`, `feature:settings`, `feature:widgets`.
- `app` — navigation, dependency wiring, Android entry points.

## Quality gates for each release

1. Every user-facing feature has a testable acceptance checklist.
2. Destructive actions have a separate review/confirmation and a recovery plan where feasible.
3. Attachments disclose what is sent, enforce type/size limits, and are never uploaded without an explicit user action.
4. Streaming, cancellation, errors, and offline behavior are tested on device.
5. The build, lint, tests, and release artifact are verified before calling a version complete.

## File-count policy

There is no target count of source files. Ten thousand placeholder or mechanically split files would make navigation, testing, review, and Android build performance worse without adding product capability. New files are created only when they express a genuine module, feature boundary, resource, or test.