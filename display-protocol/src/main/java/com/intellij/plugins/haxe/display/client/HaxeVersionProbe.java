package com.intellij.plugins.haxe.display.client;

import com.intellij.plugins.haxe.display.protocol.InitializeResult.SemVer;
import com.intellij.plugins.haxe.display.transport.DisplayChannel;
import com.intellij.plugins.haxe.display.transport.DisplayRequestException;
import com.intellij.plugins.haxe.display.transport.DisplayResponse;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a compiler's version from its {@code -version} answer, the one
 * spelling haxe 3 knows, whether a server answers it over its socket or the
 * compiler on the command line. Haxe 3 and 4 answer with the bare version,
 * haxe 3 on stderr. A haxe 5 server answers nothing over its socket (its
 * command line still does), so an empty server answer tells only that the
 * server is no haxe 3 or 4.
 */
public final class HaxeVersionProbe {

  // a dotted version as haxe prints it, with an optional pre-release and build part
  // (3.4.7, 4.3.7, 5.0.0-preview.1, 4.0.0-rc.5+1a2b3c)
  private static final Pattern VERSION =
    Pattern.compile("(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)(?:-(?<pre>[0-9A-Za-z.]+))?(?:\\+(?<build>[0-9A-Za-z.]+))?");

  private HaxeVersionProbe() {
  }

  /** The version the server's compiler reports; null when its answer names none. */
  public static SemVer versionOf(DisplayChannel channel, int timeoutMs) throws DisplayRequestException {
    DisplayResponse response = channel.request(List.of("-version"), timeoutMs);
    String answer = response.payload().isBlank() ? String.join("\n", response.logs()) : response.payload();
    return parse(answer);
  }

  /** The first version in a {@code -version} answer; null when it names none. */
  public static SemVer parse(String text) {
    Matcher matcher = VERSION.matcher(text);
    if (!matcher.find()) return null;
    int major = Integer.parseInt(matcher.group("major"));
    int minor = Integer.parseInt(matcher.group("minor"));
    int patch = Integer.parseInt(matcher.group("patch"));
    return new SemVer(major, minor, patch, matcher.group("pre"), matcher.group("build"));
  }
}
