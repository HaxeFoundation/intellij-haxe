package com.intellij.plugins.haxe.v2.buildtools.settings.ui;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeFrameworkTargetSettings.Framework;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;

/** One framework's page (Settings | Haxe | Frameworks | Lime/OpenFL/NME); the page is {@link HaxeFrameworkSettingsForm}. */
public abstract class HaxeFrameworkConfigurable implements SearchableConfigurable {

  private final Framework framework;
  private final String id;
  private final String displayName;
  private HaxeFrameworkSettingsForm form;

  protected HaxeFrameworkConfigurable(@NotNull Framework framework,
                                      @NotNull String id,
                                      @NotNull String displayName) {
    this.framework = framework;
    this.id = id;
    this.displayName = displayName;
  }

  @Override
  public @NotNull String getId() {
    return id;
  }

  @Override
  public String getDisplayName() {
    return displayName;
  }

  @Override
  public @Nullable JComponent createComponent() {
    form = new HaxeFrameworkSettingsForm(framework);
    form.reset();
    return form.getPanel();
  }

  @Override
  public boolean isModified() {
    return form.isModified();
  }

  @Override
  public void apply() {
    form.apply();
  }

  @Override
  public void reset() {
    form.reset();
  }

  @Override
  public void disposeUIResources() {
    form = null;
  }
}
