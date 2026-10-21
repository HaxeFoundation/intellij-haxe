package com.intellij.plugins.haxe.profiler.bridge.hashlink;

import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

/** Settings form of one HashLink profiler configuration: the sampling rate. Layout in the bound .form. */
final class HaxeHlProfilerConfigurable implements UnnamedConfigurable {

  private final HaxeHlProfilerConfigurationState state;
  private JPanel panel;
  private JBTextField samplesField;

  HaxeHlProfilerConfigurable(@NotNull HaxeHlProfilerConfigurationState state) {
    this.state = state;
  }

  @Override
  public @Nullable JComponent createComponent() {
    return panel;
  }

  @Override
  public boolean isModified() {
    return parsedSamples() != state.getSamplesPerSecond();
  }

  @Override
  public void apply() {
    state.setSamplesPerSecond(parsedSamples());
  }

  @Override
  public void reset() {
    samplesField.setText(String.valueOf(state.getSamplesPerSecond()));
  }

  /** A non-numeric entry falls back to the stored value rather than failing apply. */
  private int parsedSamples() {
    return StringUtil.parseInt(samplesField.getText().trim(), state.getSamplesPerSecond());
  }
}
