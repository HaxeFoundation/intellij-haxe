package com.intellij.plugins.haxe.profiler.bridge.js;

import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

/** Settings form of one "JavaScript Profiler" configuration: the sampling interval and the note. Layout in the bound .form. */
final class HaxeJsProfilerConfigurable implements UnnamedConfigurable {

  private final HaxeJsProfilerConfigurationState state;
  private JPanel panel;
  private JBTextField intervalField;

  HaxeJsProfilerConfigurable(@NotNull HaxeJsProfilerConfigurationState state) {
    this.state = state;
  }

  @Override
  public @Nullable JComponent createComponent() {
    return panel;
  }

  @Override
  public boolean isModified() {
    return parsedInterval() != state.getSamplingIntervalUs();
  }

  @Override
  public void apply() {
    state.setSamplingIntervalUs(parsedInterval());
  }

  @Override
  public void reset() {
    intervalField.setText(String.valueOf(state.getSamplingIntervalUs()));
  }

  /** A non-numeric entry falls back to the stored value rather than failing apply. */
  private int parsedInterval() {
    return StringUtil.parseInt(intervalField.getText().trim(), state.getSamplingIntervalUs());
  }
}
