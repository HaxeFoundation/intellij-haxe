package com.intellij.plugins.haxe.v2.compiler.settings.ui;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevel;
import com.intellij.plugins.haxe.v2.compiler.HaxeLanguageLevelUtil;
import com.intellij.plugins.haxe.v2.compiler.settings.HaxeCompilerSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Settings page under Build, Execution, Deployment | Compiler | Haxe Compiler.
 */
public final class HaxeCompilerConfigurable implements SearchableConfigurable, Configurable.NoScroll {

  public static final String ID = "settings.haxe.compiler";

  private final Project project;
  private HaxeCompilerSettingsPanel panel;

  public HaxeCompilerConfigurable(@NotNull Project project) {
    this.project = project;
  }

  @Override
  public @NotNull String getId() {
    return ID;
  }

  @Override
  public String getDisplayName() {
    return HaxeBundle.message("haxe.compiler.configurable.name");
  }

  @Override
  public @Nullable JComponent createComponent() {
    if (panel == null) {
      panel = new HaxeCompilerSettingsPanel();
    }
    reset();
    return panel.getComponent();
  }

  @Override
  public boolean isModified() {
    if (panel == null) return false;
    HaxeCompilerSettings settings = getSettings();
    return panel.getSelectedDefaultLevel() != settings.getExplicitDefaultLanguageLevel()
           || !panel.getModuleOverrides().equals(shownOverrides(settings))
           || panel.isUseLanguageLevelForConditionals() != settings.isUseLanguageLevelForConditionals()
           || panel.isCompilerDiagnosticsEnabled() != settings.isCompilerDiagnosticsEnabled()
           || panel.isDiagnosticsErrorsEnabled() != settings.isDiagnosticsErrorsEnabled()
           || panel.isDiagnosticsUnusedImportsEnabled() != settings.isDiagnosticsUnusedImportsEnabled()
           || panel.isDiagnosticsRemovableCodeEnabled() != settings.isDiagnosticsRemovableCodeEnabled()
           || panel.isCompilerDiagnosticsOnly() != settings.isCompilerDiagnosticsOnly()
           || panel.getCompletionMode() != settings.getCompletionMode()
           || panel.isCompilerIdeFeaturesEnabled() != settings.isCompilerIdeFeaturesEnabled();
  }

  @Override
  public void apply() {
    if (panel == null) return;
    HaxeCompilerSettings settings = getSettings();
    settings.setDefaultLanguageLevel(panel.getSelectedDefaultLevel());
    settings.setContainerLanguageLevelOverrides(withUnlistedOverrides(settings, panel.getModuleOverrides()));
    settings.setUseLanguageLevelForConditionals(panel.isUseLanguageLevelForConditionals());
    settings.setCompilerDiagnosticsEnabled(panel.isCompilerDiagnosticsEnabled());
    settings.setDiagnosticsErrorsEnabled(panel.isDiagnosticsErrorsEnabled());
    settings.setDiagnosticsUnusedImportsEnabled(panel.isDiagnosticsUnusedImportsEnabled());
    settings.setDiagnosticsRemovableCodeEnabled(panel.isDiagnosticsRemovableCodeEnabled());
    settings.setCompilerDiagnosticsOnly(panel.isCompilerDiagnosticsOnly());
    settings.setCompletionMode(panel.getCompletionMode());
    settings.setCompilerIdeFeaturesEnabled(panel.isCompilerIdeFeaturesEnabled());
    HaxeLanguageLevelUtil.notifyLanguageLevelChanged(project);
  }

  @Override
  public void reset() {
    if (panel == null) return;
    HaxeCompilerSettings settings = getSettings();
    panel.reset(settings.getExplicitDefaultLanguageLevel(),
                HaxeLanguageLevelUtil.fromCompiler(project, null),
                settings.getContainerLanguageLevelOverrides(),
                getModuleNames());
    panel.setUseLanguageLevelForConditionals(settings.isUseLanguageLevelForConditionals());
    panel.setCompilerDiagnosticsEnabled(settings.isCompilerDiagnosticsEnabled());
    panel.setDiagnosticsErrorsEnabled(settings.isDiagnosticsErrorsEnabled());
    panel.setDiagnosticsUnusedImportsEnabled(settings.isDiagnosticsUnusedImportsEnabled());
    panel.setDiagnosticsRemovableCodeEnabled(settings.isDiagnosticsRemovableCodeEnabled());
    panel.setCompilerDiagnosticsOnly(settings.isCompilerDiagnosticsOnly());
    panel.setCompletionMode(settings.getCompletionMode());
    panel.setCompilerIdeFeaturesEnabled(settings.isCompilerIdeFeaturesEnabled());
  }

  @Override
  public void disposeUIResources() {
    panel = null;
  }

  @NotNull
  private HaxeCompilerSettings getSettings() {
    return HaxeCompilerSettings.getInstance(project);
  }

  @NotNull
  private List<String> getModuleNames() {
    return Arrays.stream(ModuleManager.getInstance(project).getModules())
      .map(Module::getName)
      .sorted(String.CASE_INSENSITIVE_ORDER)
      .toList();
  }

  // The override table lists MODULES only, but the map also carries
  // non-module container keys (a /project-root override set from the tool
  // window or the language-level quickfix). Those keys never appear as
  // table rows, so isModified compares against the shown subset and apply
  // carries the unlisted entries forward instead of wiping them.

  @NotNull
  private Map<String, HaxeLanguageLevel> shownOverrides(@NotNull HaxeCompilerSettings settings) {
    Map<String, HaxeLanguageLevel> shown = new HashMap<>(settings.getContainerLanguageLevelOverrides());
    shown.keySet().retainAll(getModuleNames());
    return shown;
  }

  @NotNull
  private Map<String, HaxeLanguageLevel> withUnlistedOverrides(@NotNull HaxeCompilerSettings settings,
                                                               @NotNull Map<String, HaxeLanguageLevel> tableOverrides) {
    Map<String, HaxeLanguageLevel> merged = new HashMap<>(settings.getContainerLanguageLevelOverrides());
    merged.keySet().removeAll(getModuleNames());
    merged.putAll(tableOverrides);
    return merged;
  }
}
