package com.intellij.plugins.haxe.v2.runconfig;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.v2.buildtools.HaxeKnownBuildFiles;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.buildtools.HaxeContainers;
import com.intellij.plugins.haxe.v2.buildsystem.HaxeBuildFile;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.listCellRenderer.BuilderKt;
import com.intellij.util.PathUtil;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Editor for {@link HaxeActionRunConfiguration}: a module filter, the build file
 * (that module's files only), the action to run (the file's defaults plus custom
 * actions) and extra arguments.
 */
public final class HaxeActionRunConfigurationEditor extends SettingsEditor<HaxeActionRunConfiguration> {

  private final Project project;
  private final ComboBox<String> moduleCombo = new ComboBox<>();
  private final ComboBox<String> fileCombo = new ComboBox<>();
  private final ComboBox<String> actionCombo = new ComboBox<>();
  private final JBTextField argumentsField = new JBTextField();

  public HaxeActionRunConfigurationEditor(@NotNull Project project) {
    this.project = project;
    moduleCombo.setRenderer(BuilderKt.textListCellRenderer("", name -> name));
    fileCombo.setRenderer(BuilderKt.textListCellRenderer("", PathUtil::getFileName));
    actionCombo.setRenderer(BuilderKt.textListCellRenderer("", name -> name));
    actionCombo.setEditable(true);
    moduleCombo.addActionListener(e -> refillFileCombo(null));
    fileCombo.addActionListener(e -> refillActionCombo(null));
  }

  @Override
  protected void resetEditorFrom(@NotNull HaxeActionRunConfiguration configuration) {
    DefaultComboBoxModel<String> moduleModel = new DefaultComboBoxModel<>();
    Arrays.stream(ModuleManager.getInstance(project).getModules())
      .map(Module::getName)
      .sorted(String.CASE_INSENSITIVE_ORDER)
      .forEach(moduleModel::addElement);
    moduleCombo.setModel(moduleModel);

    String owningModule = owningModuleName(configuration.getBuildFilePath());
    if (owningModule != null) {
      moduleCombo.setSelectedItem(owningModule);
    }
    else if (moduleModel.getSize() > 0) {
      moduleCombo.setSelectedIndex(0);
    }

    refillFileCombo(configuration.getBuildFilePath());
    refillActionCombo(configuration.getActionName());
    argumentsField.setText(configuration.getExtraArguments());
  }

  @Override
  protected void applyEditorTo(@NotNull HaxeActionRunConfiguration configuration) {
    configuration.setBuildFilePath((String)fileCombo.getSelectedItem());
    Object action = actionCombo.getEditor().getItem();
    configuration.setActionName(action == null ? "" : action.toString().trim());
    configuration.setExtraArguments(argumentsField.getText().trim());
  }

  @Override
  protected @NotNull JComponent createEditor() {
    return FormBuilder.createFormBuilder()
      .addLabeledComponent(HaxeBundle.message("haxe.action.editor.module"), moduleCombo)
      .addLabeledComponent(HaxeBundle.message("haxe.action.editor.build.file"), fileCombo)
      .addLabeledComponent(HaxeBundle.message("haxe.action.editor.action"), actionCombo)
      .addLabeledComponent(HaxeBundle.message("haxe.action.editor.arguments"), argumentsField)
      .getPanel();
  }

  @Nullable
  private String owningModuleName(@NotNull String buildFilePath) {
    if (buildFilePath.isEmpty()) return null;
    VirtualFile file = LocalFileSystem.getInstance().findFileByPath(buildFilePath);
    if (file == null) return null;
    // module lookup hits the file index - EDT has no implicit read access
    String containerId = ReadAction.computeBlocking(() -> HaxeContainers.containerIdFor(project, file));
    return HaxeContainers.isProjectRoot(containerId) ? null : containerId;
  }

  private void refillFileCombo(@Nullable String selectedPath) {
    String previous = selectedPath != null ? selectedPath : (String)fileCombo.getSelectedItem();

    DefaultComboBoxModel<String> fileModel = new DefaultComboBoxModel<>();
    collectBuildFilePaths((String)moduleCombo.getSelectedItem()).forEach(fileModel::addElement);
    if (!StringUtil.isEmptyOrSpaces(previous) && fileModel.getIndexOf(previous) < 0) {
      fileModel.addElement(previous);
    }
    fileCombo.setModel(fileModel);
    fileCombo.setSelectedItem(StringUtil.isEmptyOrSpaces(previous) ? null : previous);
  }

  private void refillActionCombo(@Nullable String selectedAction) {
    HaxeActionComboUtil.refillActionCombo(project, actionCombo, (String)fileCombo.getSelectedItem(), selectedAction);
  }

  /** The chosen module's known build files (detected + manually added - hidden), or every known build file. */
  @NotNull
  private Set<String> collectBuildFilePaths(@Nullable String moduleName) {
    // the scan walks module roots - EDT has no implicit read access
    List<HaxeBuildFile> known = ReadAction.computeBlocking(() -> knownBuildFiles(moduleName));
    return known.stream()
      .map(buildFile -> buildFile.file().getPath())
      .sorted(Comparator.comparing(PathUtil::getFileName, String.CASE_INSENSITIVE_ORDER))
      .collect(Collectors.toCollection(LinkedHashSet::new));
  }

  @NotNull
  private List<HaxeBuildFile> knownBuildFiles(@Nullable String moduleName) {
    if (moduleName == null) return HaxeKnownBuildFiles.all(project);
    Module module = ModuleManager.getInstance(project).findModuleByName(moduleName);
    return module == null ? List.of() : HaxeKnownBuildFiles.forModule(project, module);
  }
}
