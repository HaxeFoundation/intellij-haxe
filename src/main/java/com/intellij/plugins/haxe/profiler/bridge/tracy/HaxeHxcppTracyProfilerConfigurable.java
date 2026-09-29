package com.intellij.plugins.haxe.profiler.bridge.tracy;

import com.intellij.openapi.options.UnnamedConfigurable;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.tracy.wire.TracyProtocolVersion;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.Objects;

/** Settings form of one "hxcpp Tracy" configuration: compression level, the feature toggles and the protocol choice. Layout in the bound .form. */
final class HaxeHxcppTracyProfilerConfigurable implements UnnamedConfigurable {

  /** One protocol dropdown entry: a version, or null for detection. */
  private record ProtocolChoice(@Nullable TracyProtocolVersion version) {
    @NotNull
    String label() {
      if (version == null) return HaxeProfilerBundle.message("haxe.profiler.tracy.protocol.auto");
      return HaxeHxcppTracyProfilerConfigurationType.protocolLabel(version);
    }
  }

  private final HaxeHxcppTracyProfilerConfigurationState state;
  private JPanel panel;
  private JBTextField levelField;
  private JBCheckBox captureMemoryCheckbox;
  private JBCheckBox collectProcessCpuCheckbox;
  private JComboBox<ProtocolChoice> protocolCombo;

  HaxeHxcppTracyProfilerConfigurable(@NotNull HaxeHxcppTracyProfilerConfigurationState state) {
    this.state = state;
  }

  @Override
  public @Nullable JComponent createComponent() {
    DefaultComboBoxModel<ProtocolChoice> choices = new DefaultComboBoxModel<>();
    choices.addElement(new ProtocolChoice(null));
    for (TracyProtocolVersion version : TracyProtocolVersion.values()) {
      choices.addElement(new ProtocolChoice(version));
    }
    protocolCombo.setModel(choices);
    protocolCombo.setRenderer(BuilderKt.textListCellRenderer(ProtocolChoice::label));
    return panel;
  }

  @Override
  public boolean isModified() {
    return parsedLevel() != state.getCompressionLevel()
           || captureMemoryCheckbox.isSelected() != state.isCaptureMemory()
           || collectProcessCpuCheckbox.isSelected() != state.isCollectProcessCpu()
           || !Objects.equals(selectedProtocol(), state.getPinnedProtocol());
  }

  @Override
  public void apply() {
    state.setCompressionLevel(parsedLevel());
    state.setCaptureMemory(captureMemoryCheckbox.isSelected());
    state.setCollectProcessCpu(collectProcessCpuCheckbox.isSelected());
    state.setPinnedProtocol(selectedProtocol());
  }

  @Override
  public void reset() {
    levelField.setText(String.valueOf(state.getCompressionLevel()));
    captureMemoryCheckbox.setSelected(state.isCaptureMemory());
    collectProcessCpuCheckbox.setSelected(state.isCollectProcessCpu());
    protocolCombo.setSelectedItem(new ProtocolChoice(state.getPinnedProtocol()));
  }

  @Nullable
  private TracyProtocolVersion selectedProtocol() {
    return protocolCombo.getSelectedItem() instanceof ProtocolChoice choice ? choice.version() : null;
  }

  /** A non-numeric entry falls back to the stored value rather than failing apply. */
  private int parsedLevel() {
    return StringUtil.parseInt(levelField.getText().trim(), state.getCompressionLevel());
  }
}
