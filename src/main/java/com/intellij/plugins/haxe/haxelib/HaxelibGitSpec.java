package com.intellij.plugins.haxe.haxelib;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/// A haxelib git version spec as build files pin it
/// (`-lib name:git:https://host/repo.git#ref` in hxml, the same
/// `git:URL[#ref]` form in haxelib.json dependencies); the optional ref is a
/// branch, tag or commit hash. Haxelib records such an install simply as
/// version `git`, so the spec never appears among installed versions.
public record HaxelibGitSpec(@NotNull String url, @Nullable String ref) {

  private static final String GIT_PREFIX = "git:";

  /** Parses a pinned version string, or null when it is not a usable git spec. */
  @Nullable
  public static HaxelibGitSpec parse(@Nullable String version) {
    if (version == null || !version.startsWith(GIT_PREFIX)) return null;
    String spec = version.substring(GIT_PREFIX.length());
    // the '#' separates the optional ref - clone urls carry no fragment
    int hash = spec.indexOf('#');
    String url = hash < 0 ? spec : spec.substring(0, hash);
    if (url.isBlank()) return null;
    String ref = hash < 0 ? null : spec.substring(hash + 1);
    return new HaxelibGitSpec(url, ref == null || ref.isBlank() ? null : ref);
  }

  /**
   * A ref that is a spelled-out commit hash shortened to git's abbreviated
   * form; branch and tag names pass through untouched.
   */
  @NotNull
  public static String shortRef(@NotNull String ref) {
    // 12+ hex-only chars read as a commit hash (a full SHA-1 has 40); real
    // branch/tag names of that shape are practically nonexistent
    boolean commitHash = ref.length() >= 12 && ref.matches("[0-9a-fA-F]+");
    return commitHash ? ref.substring(0, 10) : ref;
  }

  /**
   * The https web base for a git remote — {@code .git} stripped, the scp-like
   * ssh form (user@host:path) converted — or null when the remote is not
   * browsable (a local path, or an explicit non-http protocol).
   */
  @Nullable
  public static String browsableBase(@Nullable String remoteUrl) {
    if (remoteUrl == null) return null;
    String url = remoteUrl.endsWith(".git") ? remoteUrl.substring(0, remoteUrl.length() - ".git".length())
                                            : remoteUrl;
    // the scp-like ssh remote form (user@host:path) browses as https://host/path
    int at = url.indexOf('@');
    int colon = url.indexOf(':', at + 1);
    if (at > 0 && colon > at && !url.contains("://")) {
      return "https://" + url.substring(at + 1, colon) + "/" + url.substring(colon + 1);
    }
    return url.startsWith("http://") || url.startsWith("https://") ? url : null;
  }

  /**
   * The raw {@code haxelib.json} URL for a repository on a known forge, or
   * null when the host has no known raw scheme — github serves raw files
   * from its own host, gitlab embeds {@code /-/raw/} in the project path.
   * Without a ref the repository's default branch ({@code HEAD}) serves.
   */
  @Nullable
  public static String rawHaxelibJsonUrl(@Nullable String repositoryUrl, @Nullable String ref) {
    String base = browsableBase(repositoryUrl);
    if (base == null) return null;
    String branch = ref == null || ref.isBlank() ? "HEAD" : ref;
    String githubBase = "https://github.com/";
    if (base.startsWith(githubBase)) {
      return "https://raw.githubusercontent.com/" + base.substring(githubBase.length()) + "/" + branch + "/haxelib.json";
    }
    if (base.contains("gitlab")) {
      return base + "/-/raw/" + branch + "/haxelib.json";
    }
    return null;
  }
}
