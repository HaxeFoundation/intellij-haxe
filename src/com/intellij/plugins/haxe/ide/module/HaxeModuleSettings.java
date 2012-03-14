package com.intellij.plugins.haxe.ide.module;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

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
  private String outputFileName = "";
  private String arguments = "";
  private boolean excludeFromCompilation = false;
  private HaxeTarget target = HaxeTarget.NEKO;
  private String flexSdkName = "";

  public HaxeModuleSettings() {
  }

  public HaxeModuleSettings(String mainClass,
                            HaxeTarget target,
                            String arguments,
                            boolean excludeFromCompilation,
                            String outputFileName,
                            String flexSdkName) {
    this.mainClass = mainClass;
    this.arguments = arguments;
    this.excludeFromCompilation = excludeFromCompilation;
    this.target = target;
    this.outputFileName = outputFileName;
    this.flexSdkName = flexSdkName;
  }

  @Override
  public HaxeModuleSettings getState() {
    return this;
  }

  @Override
  public void loadState(HaxeModuleSettings state) {
    XmlSerializerUtil.copyBean(state, this);
  }

  public void setFlexSdkName(String flexSdkName) {
    this.flexSdkName = flexSdkName;
  }

  public String getFlexSdkName() {
    return flexSdkName;
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

  public HaxeTarget getTarget() {
    return target;
  }

  public void setTarget(HaxeTarget target) {
    this.target = target;
  }

  public boolean isExcludeFromCompilation() {
    return excludeFromCompilation;
  }

  public void setExcludeFromCompilation(boolean excludeFromCompilation) {
    this.excludeFromCompilation = excludeFromCompilation;
  }

  public String getOutputFileName() {
    return outputFileName;
  }

  public void setOutputFileName(String outputFileName) {
    this.outputFileName = outputFileName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HaxeModuleSettings settings = (HaxeModuleSettings)o;

    if (excludeFromCompilation != settings.excludeFromCompilation) return false;
    if (arguments != null ? !arguments.equals(settings.arguments) : settings.arguments != null) return false;
    if (mainClass != null ? !mainClass.equals(settings.mainClass) : settings.mainClass != null) return false;
    if (outputFileName != null ? !outputFileName.equals(settings.outputFileName) : settings.outputFileName != null) return false;
    if (mainClass != null ? !mainClass.equals(settings.mainClass) : settings.mainClass != null) return false;
    if (flexSdkName != null ? !flexSdkName.equals(settings.flexSdkName) : settings.flexSdkName != null) return false;
    if (target != settings.target) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = mainClass != null ? mainClass.hashCode() : 0;
    result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
    result = 31 * result + (outputFileName != null ? outputFileName.hashCode() : 0);
    result = 31 * result + (flexSdkName != null ? flexSdkName.hashCode() : 0);
    result = 31 * result + (excludeFromCompilation ? 1 : 0);
    result = 31 * result + (target != null ? target.hashCode() : 0);
    return result;
  }
}
