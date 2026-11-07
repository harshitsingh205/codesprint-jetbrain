# Publishing to JetBrains Marketplace

## Before the first upload

1. **Verify the placeholders** in `src/main/resources/META-INF/plugin.xml`:
   - `<id>` (`com.codesprint.ide`) can never change after the first release.
   - `<vendor>` name and URL. Adding a contact email is recommended: `<vendor email="you@example.com" url="...">`.
2. **Create the vendor / organization** on https://plugins.jetbrains.com (profile menu, "Upload plugin"). Use the same name as `<vendor>`.
3. **Check the plugin name.** Marketplace does not allow trademarked words such as "IntelliJ", "JetBrains" or "Android Studio" as part of the plugin name. "CodeSprint" is fine. Mentioning compatibility in the description is allowed.
4. **Prepare screenshots** (Marketplace page, not shipped in the ZIP): dashboard in dark theme, dashboard in light theme, settings page, status bar timer. 1280x800 or similar works well.
5. Review `CHANGELOG.md`: the newest `## x.y.z` section becomes the release notes automatically.

## Build and verify

```
./gradlew test
./gradlew verifyPlugin
./gradlew buildPlugin
```

`verifyPlugin` checks binary compatibility against IntelliJ IDEA 2023.3, IntelliJ IDEA 2024.3 and Android Studio (see `build.gradle.kts`). Fix any reported problems before uploading.

## First release

The first version must be uploaded by hand: **Upload plugin** on the Marketplace site, select `build/distributions/codesprint-jetbrains-1.0.0.zip`, choose the license (MIT), tags (Productivity, Education) and submit. The first review usually takes a few working days. Later releases can be automated.

## Automated releases (optional)

1. Generate a permanent token at https://plugins.jetbrains.com/author/me/tokens and add it as the repository secret `PUBLISH_TOKEN`.
2. Optional but recommended: sign the plugin. Generate a key pair (see the JetBrains "Plugin Signing" documentation) and add `CERTIFICATE_CHAIN`, `PRIVATE_KEY` and `PRIVATE_KEY_PASSWORD` as repository secrets.
3. Bump `pluginVersion` in `gradle.properties`, update `CHANGELOG.md`, then publish a GitHub release. The `Publish` workflow builds, verifies, signs and uploads the plugin.

Pre-release versions such as `1.1.0-beta.1` are published to the matching `beta` channel automatically.

## Compatibility notes

- `pluginSinceBuild = 233` (2023.3). Android Studio Iguana (2023.2) is build 232 and is not covered; raise or lower the value only after testing with `runIde` and `verifyPlugin`.
- No `untilBuild` is set. If a future IDE release breaks the plugin, publish a fixed version rather than capping the range.
- The plugin depends only on `com.intellij.modules.platform`, which is why it installs in Android Studio and all other IntelliJ-based IDEs.
