package com.intellij.plugins.haxe.profiler.bridge.chart;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.plugins.haxe.HaxeProfilerBundle;
import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeChartData.CurveCategory;
import com.intellij.plugins.haxe.profiler.bridge.chart.HaxeChartLanes.Visibility;
import com.intellij.ui.components.JBCheckBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.event.ActionEvent;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * The Call Chart's Configure View dialog: one checkbox per lane. OK applies
 * and saves the choices, Cancel discards them, Default returns every box to
 * its data-driven default (a lane with data shown, one without hidden).
 * Layout in the bound .form.
 */
final class HaxeChartViewDialog extends DialogWrapper {

  private final Visibility defaults;
  private JPanel panel;
  private JBCheckBox framesBox;
  private JBCheckBox gcBox;
  private JBCheckBox memoryBox;
  private JBCheckBox gpuMemoryBox;
  private JBCheckBox cpuBox;
  private JBCheckBox gpuLoadBox;
  private JBCheckBox eventsBox;
  private JBCheckBox callsBox;

  HaxeChartViewDialog(@Nullable Project project, @NotNull Visibility current, @NotNull Visibility defaults) {
    super(project);
    this.defaults = defaults;
    setTitle(HaxeProfilerBundle.message("haxe.profiler.callchart.configure.view"));
    init();
    setVisibility(current);
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return panel;
  }

  @Override
  protected Action @NotNull [] createLeftSideActions() {
    return new Action[]{new DialogWrapperAction(HaxeProfilerBundle.message("haxe.profiler.callchart.view.default")) {
      @Override
      protected void doAction(ActionEvent event) {
        setVisibility(defaults);
      }
    }};
  }

  private void setVisibility(Visibility visibility) {
    framesBox.setSelected(visibility.frames());
    gcBox.setSelected(visibility.gc());
    eventsBox.setSelected(visibility.events());
    callsBox.setSelected(visibility.calls());
    memoryBox.setSelected(visibility.curves().getOrDefault(CurveCategory.MEMORY, false));
    gpuMemoryBox.setSelected(visibility.curves().getOrDefault(CurveCategory.GPU_MEMORY, false));
    cpuBox.setSelected(visibility.curves().getOrDefault(CurveCategory.CPU_LOAD, false));
    gpuLoadBox.setSelected(visibility.curves().getOrDefault(CurveCategory.GPU_LOAD, false));
  }

  /** The chosen visibility after OK. */
  @NotNull
  Visibility visibility() {
    Map<CurveCategory, Boolean> curves = new EnumMap<>(CurveCategory.class);
    curves.put(CurveCategory.MEMORY, memoryBox.isSelected());
    curves.put(CurveCategory.GPU_MEMORY, gpuMemoryBox.isSelected());
    curves.put(CurveCategory.CPU_LOAD, cpuBox.isSelected());
    curves.put(CurveCategory.GPU_LOAD, gpuLoadBox.isSelected());
    return new Visibility(framesBox.isSelected(), gcBox.isSelected(), eventsBox.isSelected(),
                     callsBox.isSelected(), curves);
  }
}
