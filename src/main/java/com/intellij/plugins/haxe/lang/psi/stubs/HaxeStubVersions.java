package com.intellij.plugins.haxe.lang.psi.stubs;

/**
 * Central stub version constant. Bump this whenever any stub format changes -
 * and also when PARSE OUTPUT changes for unchanged text (a grammar or error
 * recovery fix, a conditional-compilation evaluation change): stale indexed
 * stubs against a fresh parse fail the platform's stub-count consistency
 * check (UpToDateStubIndexMismatch).
 */
public final class HaxeStubVersions {
  public static final int STUB_VERSION = 141;

  private HaxeStubVersions() {}
}

