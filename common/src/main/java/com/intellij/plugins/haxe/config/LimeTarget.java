package com.intellij.plugins.haxe.config;

/**
 * The targets the lime command line tool builds for, as listed by
 * {@code lime help} and <a href="https://lime.openfl.org/docs/getting-started/targets/">the lime targets documentation</a>.
 */
public enum LimeTarget implements FrameworkTarget {

  AIR("Adobe AIR", HaxeTarget.FLASH, "air"),
  ANDROID("Android", HaxeTarget.CPP, "android"),
  WEBASSEMBLY("WebAssembly", HaxeTarget.CPP, "webassembly"),
  FLASH("Flash", HaxeTarget.FLASH, "flash"),
  HTML5("HTML5", HaxeTarget.JAVA_SCRIPT, "html5"),
  IOS("iOS", HaxeTarget.CPP, "ios"),
  LINUX("Linux", HaxeTarget.CPP, "linux"),
  MAC("Mac OS", HaxeTarget.CPP, "mac"),
  TVOS("tvOS", HaxeTarget.CPP, "tvos"),
  WINDOWS("Windows", HaxeTarget.CPP, "windows"),
  ELECTRON("Electron", HaxeTarget.JAVA_SCRIPT, "electron"),
  HL("HashLink", HaxeTarget.HL, "hl"),
  NEKO("Neko", HaxeTarget.NEKO, "neko"),
  RPI("Raspberry Pi", HaxeTarget.CPP, "rpi");

  /** The default target for new projects and unset selections. */
  public static final LimeTarget DEFAULT = HTML5;

  private final String[] flags;
  private final String description;
  private final HaxeTarget outputTarget;

  LimeTarget(String description, HaxeTarget target, String... flags) {
    this.flags = flags;
    this.description = description;
    this.outputTarget = target;
  }

  @Override
  public String getTargetFlag() {
    return flags.length > 0 ? flags[0] : "";
  }

  @Override
  public String[] getFlags() {
    return flags;
  }

  @Override
  public HaxeTarget getOutputTarget() {
    return outputTarget;
  }

  @Override
  public String toString() {
    return description;
  }
}
