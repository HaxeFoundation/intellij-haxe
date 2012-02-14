package com.intellij.plugins.haxe.ide.module;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author: Fedor.Korotkov
 */
@State(
  name = "HaxeModuleSettingsStorage",
  storages = {
    @Storage(
      file = "$MODULE_FILE$"
    )
  }
)
public class HaxeModuleSettings implements PersistentStateComponent<HaxeModuleSettings> {
  private String mainClass = "";
  private String arguments = "";
  private boolean excludeFromCompilation = false;
  @Nullable
  private HaxeTarget target = null;

  public HaxeModuleSettings() {
  }

  @Override
  public HaxeModuleSettings getState() {
    return this;
  }

  @Override
  public void loadState(HaxeModuleSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public static HaxeModuleSettings getInstance(@NotNull Module module) {
    return ModuleServiceManager.getService(module, HaxeModuleSettings.class);
  }

  public String getMainClass() {
    return mainClass;
  }

  public void setMainClass(String mainClass) {
    this.mainClass = mainClass;
  }

  public String getArguments() {
    return arguments;
  }

  public void setArguments(String arguments) {
    this.arguments = arguments;
  }

  @Nullable
  public HaxeTarget getTarget() {
    return target;
  }

  public void setTarget(@Nullable HaxeTarget target) {
    this.target = target;
  }

  public boolean isExcludeFromCompilation() {
    return excludeFromCompilation;
  }

  public void setExcludeFromCompilation(boolean excludeFromCompilation) {
    this.excludeFromCompilation = excludeFromCompilation;
  }
}
