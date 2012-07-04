package com.intellij.plugins.haxe.config;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.NonDefaultProjectConfigurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.ui.HaxeSettingsForm;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSettingsConfigurable implements SearchableConfigurable, NonDefaultProjectConfigurable {
  private HaxeSettingsForm mySettingsPane;
  private final Project myProject;

  public HaxeSettingsConfigurable(Project project) {
    myProject = project;
  }

  public String getDisplayName() {
    return HaxeBundle.message("haxe.settings.name");
  }

  @NotNull
  public String getId() {
    return "haxe.settings";
  }

  public String getHelpTopic() {
    return null;
  }

  public JComponent createComponent() {
    if (mySettingsPane == null) {
      mySettingsPane = new HaxeSettingsForm();
    }
    reset();
    return mySettingsPane.getPanel();
  }

  public boolean isModified() {
    return mySettingsPane != null && mySettingsPane.isModified(getSettings());
  }

  public void apply() throws ConfigurationException {
    if (mySettingsPane != null) {
      mySettingsPane.applyEditorTo(getSettings());
    }
  }

  public void reset() {
    if (mySettingsPane != null) {
      mySettingsPane.resetEditorFrom(getSettings());
    }
  }

  private HaxeProjectSettings getSettings(){
    return HaxeProjectSettings.getInstance(myProject);
  }

  public void disposeUIResources() {
    mySettingsPane = null;
  }

  public Runnable enableSearch(String option) {
    return null;
  }
}
