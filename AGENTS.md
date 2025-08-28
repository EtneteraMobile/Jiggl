# Repository Guidelines

## Project Structure & Module Organization
- Source code: `src/main/kotlin/` (modules: `api/`, `popup/`, `options/`, `background/`, `utils/`).
- Web assets: `src/main/resources/` (`html/`, `css/`, `js/`, images, manifests for Chrome/Firefox).
- Tests: `src/test/kotlin/` (Kotlin/JS tests mirroring main package layout).
- Build outputs: `build/extension/` (unpacked extension), `build/distributions/` (zip bundles).

## Build, Test, and Development Commands
- Build (Chrome): `./gradlew build -Pbrowser=chrome`
- Build (Firefox): `./gradlew build -Pbrowser=firefox`
- Bundle zip (Chrome): `./gradlew bundle -Pbrowser=chrome`
- Bundle zip (Firefox): `./gradlew bundle -Pbrowser=firefox`
- Run tests: `./gradlew test` (includes browser-target tests)

Load the unpacked extension from `build/extension/` (Chrome: chrome://extensions, Firefox: about:debugging > This Firefox > Load Temporary Add-on).

## Coding Style & Naming Conventions
- Language: Kotlin/JS. Use 4‑space indentation, UTF‑8, Unix line endings.
- Packages: lowercase dot-separated (e.g., `api.models`).
- Classes/objects: UpperCamelCase; functions/vars: lowerCamelCase; constants: UPPER_SNAKE_CASE.
- Prefer small, focused files; keep changes minimal and localized.
- Serialization: kotlinx.serialization with explicit serializers where needed.

## Testing Guidelines
- Framework: Kotlin test for JS (`kotlin-test-js`).
- Place tests under `src/test/kotlin/` mirroring package paths, e.g., `utils.extensions/DateExtensionsTest.kt`.
- Naming: `ThingBeingTestedTest.kt`; test functions describe behavior.
- Run with `./gradlew test`. Add tests for logic in `utils/` and `popup.models/`.

## Commit & Pull Request Guidelines
- Commits: clear, imperative subject (max ~72 chars). Group related changes.
- PRs must include: concise description, rationale, screenshots/GIFs for UI changes, and links to issues.
- Update `CHANGELOG.md` for user-visible changes. Keep manifests and permissions in sync across Chrome/Firefox.

## Security & Configuration Tips
- Do not commit secrets. Toggl API token is read from extension storage.
- Host permissions live in `src/main/resources/manifest-*.json`. Add only what’s required.
- Firefox Jira POSTs use a content script for same-origin requests; ensure a Jira tab is open when testing.

## Architecture Overview
- HTTP: Ktor client (GET) and fetch/content-script (Jira POST). Models in `api/models`.
- UI: Kotlin/JS builds `popup.html` and `options.html`; table-based, keyboard-friendly interactions.
