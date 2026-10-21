package com.intellij.plugins.haxe.config;

/**
 * The targets the nme command line tool builds for, as listed by {@code nme help}.
 */
public enum NMETarget implements FrameworkTarget {

  CPP("Desktop (C++)", HaxeTarget.CPP, "cpp"),
  CPPIA("Cppia", HaxeTarget.CPPIA, "cppia"),
  ANDROID("Android", HaxeTarget.CPP, "android"),
  ANDROIDVIEW("Android (library view)", HaxeTarget.CPP, "androidview"),
  ANDROIDSIM("Android (simulator)", HaxeTarget.CPP, "androidsim"),
  IOS("iOS", HaxeTarget.CPP, "ios"),
  IPHONE("iOS (device debugging)", HaxeTarget.CPP, "iphone"),
  IPHONESIM("iOS (simulator)", HaxeTarget.CPP, "iphonesim"),
  IOSVIEW("iOS (library view)", HaxeTarget.CPP, "iosview"),
  WATCHOS("watchOS", HaxeTarget.CPP, "watchos"),
  WATCHSIMULATOR("watchOS (simulator)", HaxeTarget.CPP, "watchsimulator"),
  FLASH("Flash", HaxeTarget.FLASH, "flash"),
  WINDOWS("Windows", HaxeTarget.CPP, "windows"),
  ARM64("Windows Arm64", HaxeTarget.CPP, "arm64"),
  WINRT("WinRT / UWP", HaxeTarget.CPP, "winrt"),
  MAC("Mac OS", HaxeTarget.CPP, "mac"),
  LINUX("Linux", HaxeTarget.CPP, "linux"),
  RPI("Raspberry Pi", HaxeTarget.CPP, "rpi"),
  RG350("RG350 console", HaxeTarget.CPP, "rg350"),
  NEKO("Neko", HaxeTarget.NEKO, "neko"),
  HTML5("HTML5 (jsprime)", HaxeTarget.JAVA_SCRIPT, "html5");

  /** The default target for new projects and unset selections. */
  public static final NMETarget DEFAULT = CPP;

  private final String[] flags;
  private final String description;
  private final HaxeTarget outputTarget;

  NMETarget(String description, HaxeTarget target, String... flags) {
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
