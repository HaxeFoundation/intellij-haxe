package com.intellij.plugins.haxe.v2.buildtools.settings.ui;

import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeGeneratedSourceRoots;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.Framework;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

/**
 * The Generated Sources tab of a framework's settings page: whether the
 * generated source folders of the framework's builds, for their selected
 * target, are marked as generated source roots ({@link HaxeGeneratedSourceRoots}).
 * Layout in the bound .form.
 */
final class HaxeFrameworkGeneratedSourcesForm {

  private final Framework framework;
  private JPanel panel;
  private JBCheckBox markCheckBox;
  private JBLabel hintLabel;

  HaxeFrameworkGeneratedSourcesForm(@NotNull Framework framework) {
    this.framework = framework;
    hintLabel.setText(HaxeBundle.message("haxe.frameworks.generated.hint", exampleFolder(framework)));
    hintLabel.setComponentStyle(UIUtil.ComponentStyle.SMALL);
    hintLabel.setForeground(UIUtil.getContextHelpForeground());
  }

  @NotNull
  JPanel getPanel() {
    return panel;
  }

  boolean isModified() {
    return markCheckBox.isSelected() != settings().marksGeneratedSources(framework);
  }

  void apply() {
    if (!isModified()) return;
    settings().setMarksGeneratedSources(framework, markCheckBox.isSelected());
    HaxeGeneratedSourceRoots.requestReconcileInOpenProjects();
  }

  void reset() {
    markCheckBox.setSelected(settings().marksGeneratedSources(framework));
  }

  /** A generated folder of the framework's default layout, which the hint names as an example. */
  @NotNull
  private static String exampleFolder(@NotNull Framework framework) {
    String key = framework == Framework.NME ? "haxe.frameworks.generated.example.nme" : "haxe.frameworks.generated.example.lime";
    return HaxeBundle.message(key);
  }

  @NotNull
  private static HaxeFrameworkTargetSettings settings() {
    return HaxeFrameworkTargetSettings.getInstance();
  }
}
