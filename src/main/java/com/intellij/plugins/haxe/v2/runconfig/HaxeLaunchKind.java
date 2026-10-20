package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration.Lane;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.browser.BrowserRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.flash.*;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hashlink.HashLinkRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.hxcpp.intellij.HxcppIntellijRunConfiguration;
import com.intellij.plugins.haxe.runner.debugger.interp.InterpConfigurationFactory;
import com.intellij.plugins.haxe.runner.debugger.interp.InterpRunConfiguration;
import com.intellij.plugins.haxe.runner.neko.NekoConfigurationFactory;
import com.intellij.plugins.haxe.runner.neko.NekoRunConfiguration;
import com.intellij.plugins.haxe.runner.nodejs.NodeConfigurationFactory;
import com.intellij.plugins.haxe.runner.nodejs.NodeRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The run configuration kinds a build's output launches through: HL bytecode
/// → HashLink Application, JS → Web Browser or Node.js, a swf → Flash (AIR for
/// lime's air target), C++ → HXCPP, neko → Neko, an interpreter build → Haxe
/// Interpreter.
/// A kind's parameters are the configuration fields a launch fills (paths,
/// arguments, working directory); [HaxeProgramLaunches] derives their defaults
/// from the build file and applies them. The persisted id of a kind is its `name()`.
public enum HaxeLaunchKind {
  HASHLINK(HashLinkRunConfiguration.class, HashLinkConfigurationFactory.class,
           "haxe.toolwindow.program.configuration.name.hashlink", HaxeTarget.HL, Lane.HASHLINK,
           Parameter.HL_FILE, Parameter.HL_BINARY),
  BROWSER(BrowserRunConfiguration.class, BrowserConfigurationFactory.class,
          "haxe.toolwindow.program.configuration.name.browser", HaxeTarget.JAVA_SCRIPT, Lane.JS,
          Parameter.CONTENT_ROOT),
  NODE(NodeRunConfiguration.class, NodeConfigurationFactory.class,
       "haxe.toolwindow.program.configuration.name.node", HaxeTarget.JAVA_SCRIPT, null,
       Parameter.EXECUTABLE, Parameter.PROGRAM_ARGUMENTS, Parameter.WORKING_DIRECTORY),
  FLASH(FlashRunConfiguration.class, FlashConfigurationFactory.class,
        "haxe.toolwindow.program.configuration.name.flash", HaxeTarget.FLASH, null,
        Parameter.SWF),
  AIR(AirRunConfiguration.class, AirConfigurationFactory.class,
      "haxe.toolwindow.program.configuration.name.air", HaxeTarget.FLASH, Lane.FLASH,
      Parameter.DESCRIPTOR, Parameter.CONTENT_ROOT),
  HXCPP(HxcppIntellijRunConfiguration.class, HxcppIntellijConfigurationFactory.class,
        "haxe.toolwindow.program.configuration.name.hxcpp", HaxeTarget.CPP, Lane.HXCPP,
        Parameter.EXECUTABLE),
  NEKO(NekoRunConfiguration.class, NekoConfigurationFactory.class,
       "haxe.toolwindow.program.configuration.name.neko", HaxeTarget.NEKO, null,
       Parameter.EXECUTABLE, Parameter.PROGRAM_ARGUMENTS, Parameter.WORKING_DIRECTORY),
  INTERP(InterpRunConfiguration.class, InterpConfigurationFactory.class,
         "haxe.toolwindow.program.configuration.name.interp", HaxeTarget.INTERP, null,
         Parameter.COMPILER_ARGUMENTS, Parameter.WORKING_DIRECTORY);

  /**
   * A configuration field a launch fills; {@code key} is the stored spelling,
   * the label names it in dialogs, the shape says how a dialog edits it.
   */
  public enum Parameter {
    HL_FILE("hlFile", "haxe.launch.parameter.hl.file", Shape.FILE),
    HL_BINARY("hlBinary", "haxe.launch.parameter.hl.binary", Shape.FILE),
    CONTENT_ROOT("contentRoot", "haxe.launch.parameter.content.root", Shape.DIRECTORY),
    SWF("swf", "haxe.launch.parameter.swf", Shape.FILE),
    DESCRIPTOR("descriptor", "haxe.launch.parameter.descriptor", Shape.FILE),
    EXECUTABLE("executable", "haxe.launch.parameter.executable", Shape.FILE),
    PROGRAM_ARGUMENTS("programArguments", "haxe.launch.parameter.program.arguments", Shape.TEXT),
    WORKING_DIRECTORY("workingDirectory", "haxe.launch.parameter.working.directory", Shape.DIRECTORY),
    COMPILER_ARGUMENTS("compilerArguments", "haxe.launch.parameter.compiler.arguments", Shape.TEXT);

    /** What a parameter's value is: a file the build writes (it may not exist yet), a directory, or plain text. */
    public enum Shape {
      FILE, DIRECTORY, TEXT
    }

    private final String key;
    private final String labelKey;
    private final Shape shape;

    Parameter(@NotNull String key, @NotNull String labelKey, @NotNull Shape shape) {
      this.key = key;
      this.labelKey = labelKey;
      this.shape = shape;
    }

    @NotNull
    public Shape shape() {
      return shape;
    }

    @NotNull
    public String label() {
      return HaxeBundle.message(labelKey);
    }

    @Nullable
    public static Parameter byKey(@Nullable String key) {
      return Arrays.stream(values())
        .filter(parameter -> parameter.key.equals(key))
        .findFirst()
        .orElse(null);
    }

    /** Stored key/value pairs as parameters; a key no parameter has (a newer plugin's) is dropped. */
    @NotNull
    public static Map<Parameter, String> fromKeys(@NotNull Map<String, String> stored) {
      Map<Parameter, String> parameters = new EnumMap<>(Parameter.class);
      stored.forEach((key, value) -> {
        Parameter parameter = byKey(key);
        if (parameter != null) parameters.put(parameter, value);
      });
      return parameters;
    }

    /** Parameters as the key/value pairs the store keeps, in parameter order. */
    @NotNull
    public static Map<String, String> toKeys(@NotNull Map<Parameter, String> parameters) {
      Map<String, String> stored = new LinkedHashMap<>();
      parameters.forEach((parameter, value) -> stored.put(parameter.key, value));
      return stored;
    }
  }

  private final Class<? extends RunConfiguration> configurationClass;
  private final Class<? extends ConfigurationFactory> factoryClass;
  private final String nameKey;
  private final HaxeTarget target;
  private final Lane profilingLane;
  private final List<Parameter> parameters;

  HaxeLaunchKind(@NotNull Class<? extends RunConfiguration> configurationClass,
                 @NotNull Class<? extends ConfigurationFactory> factoryClass,
                 @NotNull String nameKey,
                 @NotNull HaxeTarget target,
                 @Nullable Lane profilingLane,
                 @NotNull Parameter... parameters) {
    this.configurationClass = configurationClass;
    this.factoryClass = factoryClass;
    this.nameKey = nameKey;
    this.target = target;
    this.profilingLane = profilingLane;
    this.parameters = List.of(parameters);
  }

  @NotNull
  public Class<? extends RunConfiguration> configurationClass() {
    return configurationClass;
  }

  @NotNull
  public ConfigurationFactory factory() {
    return HaxeRunConfigurationType.getInstance().getFactory(factoryClass);
  }

  /** The factory's display name ("HashLink Application", ...). */
  @NotNull
  public String displayName() {
    return factory().getName();
  }

  /** Names a created configuration after its build file: "{0} (HashLink)". */
  @NotNull
  public String configurationName(@NotNull String buildFileName) {
    return HaxeBundle.message(nameKey, buildFileName);
  }

  /** The compilation target whose output the kind launches; decides the debugger and the compile additions. */
  @NotNull
  public HaxeTarget target() {
    return target;
  }

  /**
   * The profiler lane the kind profiles under, null without one. Flash
   * profiles only as AIR: adl's debugger runtime carries the telemetry
   * sampler; the standalone player launch does not.
   */
  @Nullable
  public Lane profilingLane() {
    return profilingLane;
  }

  @NotNull
  public List<Parameter> parameters() {
    return parameters;
  }

  /**
   * Whether the kind's launch compiles the build file first and then runs its
   * output. The interpreter compiles AS it runs: a build step before it would
   * run the program once before the launch does.
   */
  public boolean compilesBeforeLaunch() {
    return this != INTERP;
  }

  /** The kinds a custom action may declare: every kind with a build step able to carry the action's command. */
  @NotNull
  public static List<HaxeLaunchKind> customizable() {
    return Arrays.stream(values())
      .filter(HaxeLaunchKind::compilesBeforeLaunch)
      .toList();
  }

  /** The kind stored under {@code id}, or null for an unknown one. */
  @Nullable
  public static HaxeLaunchKind byId(@Nullable String id) {
    return Arrays.stream(values())
      .filter(kind -> kind.name().equals(id))
      .findFirst()
      .orElse(null);
  }
}
