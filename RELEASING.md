# Releasing the Haxe IntelliJ Plugin (fork)

This fork ships builds to downstream IntelliJ users through a **custom plugin repository** hosted on GitHub Pages, instead of the JetBrains Marketplace. Pushing a `v*` git tag triggers `.github/workflows/release.yml`, which builds the plugin, publishes a GitHub Release with the ZIP attached, and updates `https://<owner>.github.io/<repo>/updatePlugins.xml` so consumer IntelliJ instances see the new build.

> **Note:** the existing `release.yaml` workflow (triggered by `release-X.Y.Z` tags) is the upstream marketplace flow and is independent of this. Use the `v*` flow described here for fork releases.

## One-time repository configuration

These steps cannot be done from a workflow — apply them once via the GitHub web UI:

1. **Enable GitHub Pages with "GitHub Actions" as the source.**
   Settings → Pages → Build and deployment → Source: **GitHub Actions**.
2. **Confirm Actions has write permissions.**
   Settings → Actions → General → Workflow permissions: **Read and write permissions** (or rely on the per-workflow `permissions:` block, which already grants `contents: write`, `pages: write`, `id-token: write`).
3. **(Optional) Protect the `v*` tag pattern.**
   Settings → Tags → add a protection rule for `v*` if you want releases to require code review.

## Versioning strategy

Use the suffix scheme `<upstream-version>-fork.<n>`, e.g. `1.8.1-fork.3`.

**Why:** IntelliJ compares plugin versions string-wise with version-aware logic. The fork version must compare *higher* than whatever the marketplace publishes for the same `id` (`com.intellij.plugins.haxe`), or IntelliJ will offer the marketplace build as an "update" and silently revert users to upstream.

**Rules:**

- When you sync to a new upstream version, reset the suffix: `1.8.2-fork.1`.
- For fork-only changes between syncs, increment the suffix: `1.8.1-fork.1` → `1.8.1-fork.2`.
- Never reuse a tag — if a release fails partway, bump and re-tag.

**The marketplace catch-up problem:** if upstream cuts `1.8.2` and you're still on `1.8.1-fork.5`, marketplace's `1.8.2` is now *higher*. Two mitigations:

- Sync to upstream and re-release (`1.8.2-fork.1`) promptly.
- Tell users to disable marketplace updates for this plugin once installed (Plugins → installed plugin → ⚙ → Disable update for this plugin).

## Cutting a release

1. Sync with upstream and apply fork patches if they aren't already on top:
   ```
   git fetch upstream
   git merge upstream/master
   ```
2. Bump `pluginVersion` in `gradle.properties` per the versioning strategy above.
3. Commit the bump:
   ```
   git commit -am "Release v1.8.1-fork.N"
   ```
4. Tag and push:
   ```
   git tag v1.8.1-fork.N
   git push origin develop --tags
   ```
5. Watch the **Actions** tab. On success:
   - A new GitHub Release `v1.8.1-fork.N` exists with the plugin ZIP attached.
   - `https://<owner>.github.io/<repo>/updatePlugins.xml` reflects the new version.
   - IntelliJ instances with the custom repo configured surface an update notification on the next "Check for Plugin Updates" or restart.

To re-run a failed release without re-tagging, use **Actions → Release plugin → Run workflow** (`workflow_dispatch`) and pick the same tag from the dropdown.

## Verification — first successful run

1. **Release exists:** visit `https://github.com/<owner>/<repo>/releases/tag/v<version>` and confirm the `.zip` is attached.
2. **Pages is live:** visit `https://<owner>.github.io/<repo>/updatePlugins.xml` — should return XML, not 404. (First Pages activation can take 5–10 minutes.)
3. **XML parses:**
   ```
   curl -fsSL https://<owner>.github.io/<repo>/updatePlugins.xml | xmllint --noout -
   ```
4. **Download URL resolves:**
   ```
   curl -fsSLI "$(curl -s https://<owner>.github.io/<repo>/updatePlugins.xml | sed -nE 's/.*url="([^"]+)".*/\1/p')"
   ```
   Should return `HTTP/2 200` after redirects.
5. **End-to-end IntelliJ test:** Settings → Plugins → ⚙ → Manage Plugin Repositories → **+** → add the Pages URL → restart → confirm the fork shows up in the **Marketplace** tab with the latest tag's version.

## Gotchas

- **First release is the noisy one.** GitHub Pages first activation can take 5–10 minutes. Subsequent deploys are fast.
- **Plugin id must equal `com.intellij.plugins.haxe`.** Same id is what makes the fork an *update* of the marketplace plugin rather than a coexisting one. Don't change it to disambiguate.
- **`since-build` / `until-build` are sourced from `gradle.properties`** (`pluginSinceBuild`, `pluginUntilBuild`) via `patchPluginXml`; the workflow extracts them from the patched `plugin.xml` so the `updatePlugins.xml` stays in sync. Don't drift the values manually.
- **The plugin ZIP filename includes the version.** Don't hardcode it anywhere — the workflow discovers it from `build/distributions/`.
- **`workflow_dispatch` is intentional.** It lets you re-run a release manually if the Pages deploy fails transiently.
- **Don't add `paths:` filters to the trigger.** Tag-based triggers don't respect path filters predictably.
- **Caching Gradle:** `gradle/actions/setup-gradle@v4` caches by default. If a build is flaky on stale cache, add `cache-disabled: true` temporarily.
