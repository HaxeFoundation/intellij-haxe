package com.intellij.plugins.haxe.ide.module;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
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
  public static final int USE_PROPERTIES = 0;
  public static final int USE_HXML = 1;
  public static final int USE_NMML = 2;

  private String mainClass = "";
  private String outputFileName = "";
  private String arguments = "";
  private boolean excludeFromCompilation = false;
  private HaxeTarget haxeTarget = HaxeTarget.NEKO;
  private NMETarget nmeTarget = NMETarget.FLASH;
  private String flexSdkName = "";
  private String hxmlPath = "";
  private String nmmlPath = "";
  private int buildConfig = 0;

  public HaxeModuleSettings() {
  }

  public HaxeModuleSettings(String mainClass,
                            HaxeTarget haxeTarget,
                            NMETarget nmeTarget,
                            String arguments,
                            boolean excludeFromCompilation,
                            String outputFileName,
                            String flexSdkName,
                            int buildConfig,
                            String hxmlPath) {
    this.mainClass = mainClass;
    this.arguments = arguments;
    this.excludeFromCompilation = excludeFromCompilation;
    this.haxeTarget = haxeTarget;
    this.nmeTarget = nmeTarget;
    this.outputFileName = outputFileName;
    this.flexSdkName = flexSdkName;
    this.buildConfig = buildConfig;
    this.hxmlPath = hxmlPath;
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

  public void setNmeTarget(NMETarget nmeTarget) {
    this.nmeTarget = nmeTarget;
  }

  public int getBuildConfig() {
    return buildConfig;
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

  public HaxeTarget getHaxeTarget() {
    return haxeTarget;
  }

  public NMETarget getNmeTarget() {
    return nmeTarget;
  }

  public void setHaxeTarget(HaxeTarget haxeTarget) {
    this.haxeTarget = haxeTarget;
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

  public String getHxmlPath() {
    return hxmlPath;
  }

  public String getNmmlPath() {
    return nmmlPath;
  }

  public void setHxmlPath(String hxmlPath) {
    this.hxmlPath = hxmlPath;
  }

  public boolean isUseHxmlToBuild() {
    return buildConfig == USE_HXML;
  }

  public boolean isUseNmmlToBuild() {
    return buildConfig == USE_NMML;
  }

  public boolean isUseUserPropertiesToBuild() {
    return buildConfig == USE_PROPERTIES;
  }

  public void setNmmlPath(String nmmlPath) {
    this.nmmlPath = nmmlPath;
  }

  public void setBuildConfig(int buildConfig) {
    this.buildConfig = buildConfig;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HaxeModuleSettings settings = (HaxeModuleSettings)o;

    if (excludeFromCompilation != settings.excludeFromCompilation) return false;
    if (buildConfig != settings.buildConfig) return false;
    if (arguments != null ? !arguments.equals(settings.arguments) : settings.arguments != null) return false;
    if (flexSdkName != null ? !flexSdkName.equals(settings.flexSdkName) : settings.flexSdkName != null) return false;
    if (hxmlPath != null ? !hxmlPath.equals(settings.hxmlPath) : settings.hxmlPath != null) return false;
    if (mainClass != null ? !mainClass.equals(settings.mainClass) : settings.mainClass != null) return false;
    if (outputFileName != null ? !outputFileName.equals(settings.outputFileName) : settings.outputFileName != null) return false;
    if (haxeTarget != settings.haxeTarget) return false;
    if (nmeTarget != settings.nmeTarget) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = mainClass != null ? mainClass.hashCode() : 0;
    result = 31 * result + (outputFileName != null ? outputFileName.hashCode() : 0);
    result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
    result = 31 * result + (excludeFromCompilation ? 1 : 0);
    result = 31 * result + (haxeTarget != null ? haxeTarget.hashCode() : 0);
    result = 31 * result + (nmeTarget != null ? nmeTarget.hashCode() : 0);
    result = 31 * result + (flexSdkName != null ? flexSdkName.hashCode() : 0);
    result = 31 * result + (hxmlPath != null ? hxmlPath.hashCode() : 0);
    result = 31 * result + buildConfig;
    return result;
  }
}
