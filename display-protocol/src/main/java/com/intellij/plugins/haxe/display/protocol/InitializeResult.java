package com.intellij.plugins.haxe.display.protocol;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;

/** Result of {@code initialize}: what the connected compiler supports. */
public record InitializeResult(SemVer protocolVersion, SemVer haxeVersion, List<String> methods) {

  /** A version; {@code pre} and {@code build} are null when the version has none. */
  public record SemVer(int major,
                       int minor,
                       int patch,
                       @JsonSetter(nulls = Nulls.SET) String pre,
                       @JsonSetter(nulls = Nulls.SET) String build) {
    @Override
    public String toString() {
      return major + "." + minor + "." + patch + (pre != null ? "-" + pre : "");
    }
  }

  public boolean supports(String method) {
    return methods.contains(method);
  }
}
