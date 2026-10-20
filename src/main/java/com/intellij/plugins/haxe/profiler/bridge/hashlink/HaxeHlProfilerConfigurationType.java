package com.intellij.plugins.haxe.profiler.bridge.hashlink;

import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.HaxeProfilableRunConfiguration;
import com.intellij.plugins.haxe.profiler.bridge.HaxeProfilerConfigurationTypeBase;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * The "HashLink Profiler" entry of the IU Run-with-Profiler executor: the VM
 * samples the run itself ({@code hl --profile}). The state carries the
 * sampling rate, editable in the profiler configuration's settings.
 */
public class HaxeHlProfilerConfigurationType extends HaxeProfilerConfigurationTypeBase<HaxeHlProfilerConfigurationState> {

  public static final String ID = "HaxeHashLinkProfilerConfiguration";
  private static final String SAMPLES_ATTRIBUTE = "samplesPerSecond";

  @Override
  public @NotNull String getId() {
    return ID;
  }

  @Override
  public @NotNull String getDisplayName() {
    return HaxeProfilerBundle.message("haxe.profiler.hl.configuration.name");
  }

  @Override
  protected @NotNull HaxeProfilableRunConfiguration.Lane lane() {
    return HaxeProfilableRunConfiguration.Lane.HASHLINK;
  }

  @Override
  protected @NotNull String stateElementName() {
    return "haxeHashlinkProfiler";
  }

  @Override
  public @NotNull HaxeHlProfilerConfigurationState getTemplateState() {
    return new HaxeHlProfilerConfigurationState(getDisplayName(), HaxeHlProfilerConfigurationState.DEFAULT_SAMPLES_PER_SECOND);
  }

  @Override
  public @NotNull HaxeHlProfilerConfigurationState copyState(@NotNull HaxeHlProfilerConfigurationState state) {
    return new HaxeHlProfilerConfigurationState(state.getDisplayName(), state.getSamplesPerSecond());
  }

  @Override
  protected void readSettings(@NotNull HaxeHlProfilerConfigurationState state, @NotNull Element element) {
    state.setSamplesPerSecond(StringUtil.parseInt(element.getAttributeValue(SAMPLES_ATTRIBUTE), state.getSamplesPerSecond()));
  }

  @Override
  protected void writeSettings(@NotNull HaxeHlProfilerConfigurationState state, @NotNull Element element) {
    element.setAttribute(SAMPLES_ATTRIBUTE, String.valueOf(state.getSamplesPerSecond()));
  }

  @Override
  public @NotNull UnnamedConfigurable createConfigurable(@NotNull HaxeHlProfilerConfigurationState state) {
    return new HaxeHlProfilerConfigurable(state);
  }
}
