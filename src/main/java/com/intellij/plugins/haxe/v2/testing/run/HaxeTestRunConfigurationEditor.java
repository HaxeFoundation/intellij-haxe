package com.intellij.plugins.haxe.v2.testing.run;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.plugins.haxe.v2.buildtools.HaxeKnownBuildFiles;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.util.PathUtil;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Editor for {@link HaxeTestRunConfiguration}: the tests build file (any known
 * build file) and the optional test filter pattern (utest's
 * {@code UTEST_PATTERN}, e.g. {@code MathTest.testAddition}).
 */
public final class HaxeTestRunConfigurationEditor extends SettingsEditor<HaxeTestRunConfiguration> {

  private final Project project;
  private final ComboBox<String> fileCombo = new ComboBox<>();
  private final JBTextField patternField = new JBTextField();

  public HaxeTestRunConfigurationEditor(@NotNull Project project) {
    this.project = project;
    fileCombo.setRenderer(BuilderKt.textListCellRenderer("", PathUtil::getFileName));
  }

  @Override
  protected void resetEditorFrom(@NotNull HaxeTestRunConfiguration configuration) {
    DefaultComboBoxModel<String> fileModel = new DefaultComboBoxModel<>();
    collectBuildFilePaths().forEach(fileModel::addElement);
    String current = configuration.getBuildFilePath();
    if (!StringUtil.isEmptyOrSpaces(current) && fileModel.getIndexOf(current) < 0) {
      fileModel.addElement(current);
    }
    fileCombo.setModel(fileModel);
    fileCombo.setSelectedItem(StringUtil.isEmptyOrSpaces(current) ? null : current);
    patternField.setText(configuration.getFilterPattern());
  }

  @Override
  protected void applyEditorTo(@NotNull HaxeTestRunConfiguration configuration) {
    configuration.setBuildFilePath((String)fileCombo.getSelectedItem());
    configuration.setFilterPattern(patternField.getText().trim());
    HaxeTestRunConfigurations.resyncCompileStepAsync(configuration);
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeBundle.message("haxe.test.editor.build.file"), fileCombo)
      .addLabeledComponent(HaxeBundle.message("haxe.test.editor.pattern"), patternField)
      .getPanel();
  }

  /** Every known build file (detected + manually added - hidden), sorted by file name. */
  @NotNull
  private Set<String> collectBuildFilePaths() {
    // the scan walks module roots - EDT has no implicit read access
    List<HaxeBuildFile> known = ReadAction.computeBlocking(() -> HaxeKnownBuildFiles.all(project));
    return known.stream()
      .map(buildFile -> buildFile.file().getPath())
      .sorted(Comparator.comparing(PathUtil::getFileName, String.CASE_INSENSITIVE_ORDER))
      .collect(Collectors.toCollection(LinkedHashSet::new));
  }
}
