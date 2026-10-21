package com.intellij.plugins.haxe.profiler.bridge.flash;

import com.intellij.openapi.options.UnnamedConfigurable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

/** Settings form of one "Flash Profiler" configuration: the telemetry note — the channel has no per-profile options. Layout in the bound .form. */
final class HaxeFlashProfilerConfigurable implements UnnamedConfigurable {

  private JPanel panel;

  @Override
  public @Nullable JComponent createComponent() {
    return panel;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() {
  }
}
