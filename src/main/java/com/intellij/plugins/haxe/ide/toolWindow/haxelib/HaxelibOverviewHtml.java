package com.intellij.plugins.haxe.ide.toolWindow.haxelib;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.haxelib.HaxelibLibraryInfo;
import com.intellij.plugins.haxe.haxelib.HaxelibLocalDocs.GitCheckout;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The Overview tab's HTML: one library's metadata (description,
 * license/owner/website), the installed picture, and the release history
 * from {@code haxelib info}. Long release lists are capped — the full
 * history lives in the version tree.
 */
final class HaxelibOverviewHtml {

  private static final int RELEASES_SHOWN = 15;

  private HaxelibOverviewHtml() {
  }

  @NotNull
  static String message(@Nls @NotNull String text) {
    return "<html><body><p>" + StringUtil.escapeXmlEntities(text) + "</p></body></html>";
  }

  /** The library overview: local state always, server metadata when available. */
  @NotNull
  static String render(@NotNull String name,
                       @NotNull Set<String> installedVersions,
                       @Nullable String selectedVersion,
                       @Nullable String devPath,
                       @Nullable GitCheckout gitCheckout,
                       @Nullable HaxelibLibraryInfo info) {
    StringBuilder html = new StringBuilder("<html><body>");
    html.append("<h2>").append(StringUtil.escapeXmlEntities(name)).append("</h2>");

    if (info != null && !info.description().isEmpty()) {
      html.append("<p>").append(StringUtil.escapeXmlEntities(info.description())).append("</p>");
    }
    appendFacts(html, info);
    appendInstalled(html, installedVersions, selectedVersion, devPath, gitCheckout);
    if (info != null) {
      appendReleases(html, info.releases());
    }
    else {
      html.append("<p><i>").append(HaxeBundle.message("haxelib.explorer.no.info")).append("</i></p>");
    }

    return html.append("</body></html>").toString();
  }

  private static void appendFacts(@NotNull StringBuilder html, @Nullable HaxelibLibraryInfo info) {
    if (info == null) return;
    html.append("<table>");
    appendFact(html, HaxeBundle.message("haxelib.explorer.latest"), info.latestVersion());
    appendFact(html, HaxeBundle.message("haxelib.explorer.license"), info.license());
    appendFact(html, HaxeBundle.message("haxelib.explorer.owner"), info.owner());
    if (!info.website().isEmpty()) {
      String url = StringUtil.escapeXmlEntities(info.website());
      String websiteRow = """
        <tr><td>%s</td><td><a href="%s">%s</a></td></tr>\
        """.formatted(HaxeBundle.message("haxelib.explorer.website"), url, url);
      html.append(websiteRow);
    }
    html.append("</table>");
  }

  private static void appendFact(@NotNull StringBuilder html, @Nls String label, @NotNull String value) {
    if (value.isEmpty()) return;
    String row = """
      <tr><td>%s</td><td>%s</td></tr>\
      """.formatted(label, StringUtil.escapeXmlEntities(value));
    html.append(row);
  }

  private static void appendInstalled(@NotNull StringBuilder html,
                                      @NotNull Set<String> installedVersions,
                                      @Nullable String selectedVersion,
                                      @Nullable String devPath,
                                      @Nullable GitCheckout gitCheckout) {
    if (installedVersions.isEmpty()) return;
    html.append("<h3>").append(HaxeBundle.message("haxelib.explorer.installed.versions")).append("</h3><p>");
    boolean first = true;
    for (String version : installedVersions) {
      if (!first) html.append(", ");
      first = false;
      boolean selected = version.equals(selectedVersion);
      if (selected) html.append("<b>");
      html.append(StringUtil.escapeXmlEntities(version));
      if (selected) html.append("</b>");
    }
    html.append("</p>");
    appendPseudoVersions(html, devPath, gitCheckout);
  }

  /** What the dev/git pseudo-versions point at: the dev directory, the git branch/commit. */
  private static void appendPseudoVersions(@NotNull StringBuilder html,
                                           @Nullable String devPath,
                                           @Nullable GitCheckout gitCheckout) {
    if (devPath == null && gitCheckout == null) return;
    html.append("<table>");
    if (devPath != null) {
      appendFact(html, HaxeBundle.message("haxelib.explorer.dev.path"), devPath);
    }
    if (gitCheckout != null) {
      appendGitCheckout(html, gitCheckout);
    }
    html.append("</table>");
  }

  /** The checkout links to its forge page (commit or branch tree) when the clone's remote is browsable. */
  private static void appendGitCheckout(@NotNull StringBuilder html, @NotNull GitCheckout checkout) {
    String webUrl = checkout.webUrl();
    if (webUrl == null) {
      appendFact(html, HaxeBundle.message("haxelib.explorer.git.checkout"), checkout.display());
      return;
    }
    String checkoutRow = """
      <tr><td>%s</td><td><a href="%s">%s</a></td></tr>\
      """.formatted(HaxeBundle.message("haxelib.explorer.git.checkout"),
                    StringUtil.escapeXmlEntities(webUrl),
                    StringUtil.escapeXmlEntities(checkout.display()));
    html.append(checkoutRow);
  }

  private static void appendReleases(@NotNull StringBuilder html, @NotNull List<HaxelibLibraryInfo.Release> releases) {
    if (releases.isEmpty()) return;
    html.append("<h3>").append(HaxeBundle.message("haxelib.explorer.releases")).append("</h3><table>");
    // newest first; haxelib prints oldest first
    List<HaxelibLibraryInfo.Release> newestFirst = releases.reversed();
    int shown = Math.min(RELEASES_SHOWN, newestFirst.size());
    for (HaxelibLibraryInfo.Release release : newestFirst.subList(0, shown)) {
      String releaseRow = """
        <tr><td><b>%s</b></td><td>%s</td><td>%s</td></tr>\
        """.formatted(StringUtil.escapeXmlEntities(release.version()),
                      StringUtil.escapeXmlEntities(release.date()),
                      StringUtil.escapeXmlEntities(release.note()));
      html.append(releaseRow);
    }
    html.append("</table>");
    if (newestFirst.size() > shown) {
      html.append("<p><i>")
        .append(HaxeBundle.message("haxelib.explorer.older.releases", newestFirst.size() - shown))
        .append("</i></p>");
    }
  }
}
