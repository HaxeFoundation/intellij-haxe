package com.intellij.plugins.haxe.config;

/**
 * The targets the openfl command line tool builds for; openfl delegates to
 * lime, so these are the lime targets an openfl project typically uses.
 * See <a href="https://lime.openfl.org/docs/getting-started/targets/">the lime targets documentation</a>.
 */
public enum OpenFLTarget implements FrameworkTarget {

  HTML5("HTML5", HaxeTarget.JAVA_SCRIPT, "html5"),
  WINDOWS("Windows", HaxeTarget.CPP, "windows"),
  MAC("Mac OS", HaxeTarget.CPP, "mac"),
  LINUX("Linux", HaxeTarget.CPP, "linux"),
  HL("HashLink", HaxeTarget.HL, "hl"),
  NEKO("Neko", HaxeTarget.NEKO, "neko"),
  RPI("Raspberry Pi", HaxeTarget.CPP, "rpi"),
  FLASH("Flash", HaxeTarget.FLASH, "flash"),
  AIR("Adobe AIR", HaxeTarget.FLASH, "air"),
  ANDROID("Android", HaxeTarget.CPP, "android"),
  IOS("iOS", HaxeTarget.CPP, "ios"),
  TVOS("tvOS", HaxeTarget.CPP, "tvos"),
  WEBASSEMBLY("WebAssembly", HaxeTarget.CPP, "webassembly"),
  ELECTRON("Electron", HaxeTarget.JAVA_SCRIPT, "electron");

  /** The default target for new projects and unset selections. */
  public static final OpenFLTarget DEFAULT = HTML5;

  private final String[] flags;
  private final String description;
  private final HaxeTarget outputTarget;

  OpenFLTarget(String description, HaxeTarget target, String... flags) {
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
