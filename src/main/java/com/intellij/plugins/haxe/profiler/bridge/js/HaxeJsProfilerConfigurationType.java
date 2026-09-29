package com.intellij.plugins.haxe.profiler.bridge.js;

import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerConfigurationTypeBase;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * The "JavaScript Profiler" entry of the IU Run-with-Profiler executor: a
 * browser launch with a Chromium the IDE started itself (DevTools port
 * open), V8's sampling profiler driven over CDP in one-second stop/start
 * segments streamed into the session file. The live view follows the run,
 * and a browser closed by hand keeps what was collected. Chromium-family
 * only: Firefox speaks no CDP.
 */
public class HaxeJsProfilerConfigurationType extends HaxeProfilerConfigurationTypeBase<HaxeJsProfilerConfigurationState> {

  public static final String ID = "HaxeJsProfilerConfiguration";
  private static final String SAMPLING_INTERVAL_ATTRIBUTE = "samplingIntervalUs";

  @Override
  public @NotNull String getId() {
    return ID;
  }

  @Override
  public @NotNull String getDisplayName() {
    return HaxeProfilerBundle.message("haxe.profiler.js.configuration.name");
  }

  @Override
  protected @NotNull HaxeProfilableRunConfiguration.Lane lane() {
    return HaxeProfilableRunConfiguration.Lane.JS;
  }

  @Override
  protected @NotNull String stateElementName() {
    return "haxeJsProfiler";
  }

  @Override
  public @NotNull HaxeJsProfilerConfigurationState getTemplateState() {
    return new HaxeJsProfilerConfigurationState(getDisplayName());
  }

  @Override
  public @NotNull HaxeJsProfilerConfigurationState copyState(@NotNull HaxeJsProfilerConfigurationState state) {
    HaxeJsProfilerConfigurationState copy = new HaxeJsProfilerConfigurationState(state.getDisplayName());
    copy.setSamplingIntervalUs(state.getSamplingIntervalUs());
    return copy;
  }

  @Override
  protected void readSettings(@NotNull HaxeJsProfilerConfigurationState state, @NotNull Element element) {
    String interval = element.getAttributeValue(SAMPLING_INTERVAL_ATTRIBUTE);
    state.setSamplingIntervalUs(StringUtil.parseInt(interval, state.getSamplingIntervalUs()));
  }

  @Override
  protected void writeSettings(@NotNull HaxeJsProfilerConfigurationState state, @NotNull Element element) {
    element.setAttribute(SAMPLING_INTERVAL_ATTRIBUTE, String.valueOf(state.getSamplingIntervalUs()));
  }

  @Override
  public @NotNull UnnamedConfigurable createConfigurable(@NotNull HaxeJsProfilerConfigurationState state) {
    return new HaxeJsProfilerConfigurable(state);
  }
}
