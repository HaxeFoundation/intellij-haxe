package com.intellij.plugins.haxe.display.protocol;

import java.util.List;

/** Result of {@code initialize}: what the connected compiler supports. */
public record InitializeResult(SemVer protocolVersion, SemVer haxeVersion, List<String> methods) {

  public record SemVer(int major, int minor, int patch, String pre, String build) {
    @Override
    public String toString() {
      return major + "." + minor + "." + patch + (pre != null ? "-" + pre : "");
    }
  }

  public boolean supports(String method) {
    return methods.contains(method);
  }
}
