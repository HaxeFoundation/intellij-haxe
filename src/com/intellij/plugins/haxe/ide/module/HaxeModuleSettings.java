/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.ide.module;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleServiceManager;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.config.NMETarget;
import com.intellij.plugins.haxe.config.OpenFLTarget;
import com.intellij.plugins.haxe.module.HaxeModuleSettingsBase;
import com.intellij.plugins.haxe.module.impl.HaxeModuleSettingsBaseImpl;
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
public class HaxeModuleSettings extends HaxeModuleSettingsBaseImpl
  implements PersistentStateComponent<HaxeModuleSettings>, HaxeModuleSettingsBase {

  private String flexSdkName = "";
  private String myHXCPPPort = "";

  public HaxeModuleSettings() {
  }

  public HaxeModuleSettings(String mainClass,
                            HaxeTarget haxeTarget,
                            NMETarget nmeTarget,
                            OpenFLTarget openFLTarget,
                            String arguments,
                            String nmeFlags,
                            boolean excludeFromCompilation,
                            String outputFileName,
                            String flexSdkName,
                            int buildConfig,
                            String hxmlPath,
                            String nmmlPath,
                            String openFLPath) {
    super(mainClass, outputFileName, arguments, nmeFlags, excludeFromCompilation, haxeTarget, nmeTarget, openFLTarget, hxmlPath, nmmlPath,
          openFLPath, buildConfig);
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

  public String getHXCPPPort() {
    return myHXCPPPort;
  }

  public void setHXCPPPort(String value) {
    myHXCPPPort = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HaxeModuleSettings settings = (HaxeModuleSettings)o;

    if (excludeFromCompilation != settings.excludeFromCompilation) return false;
    if (buildConfig != settings.buildConfig) return false;
    if (arguments != null ? !arguments.equals(settings.arguments) : settings.arguments != null) return false;
    if (nmeFlags != null ? !nmeFlags.equals(settings.nmeFlags) : settings.nmeFlags != null) return false;
    if (openFLFlags != null ? !openFLFlags.equals(settings.openFLFlags) : settings.openFLFlags != null) return false;
    if (flexSdkName != null ? !flexSdkName.equals(settings.flexSdkName) : settings.flexSdkName != null) return false;
    if (hxmlPath != null ? !hxmlPath.equals(settings.hxmlPath) : settings.hxmlPath != null) return false;
    if (mainClass != null ? !mainClass.equals(settings.mainClass) : settings.mainClass != null) return false;
    if (outputFileName != null ? !outputFileName.equals(settings.outputFileName) : settings.outputFileName != null) return false;
    if (haxeTarget != settings.haxeTarget) return false;
    if (nmeTarget != settings.nmeTarget) return false;
    if (openFLTarget != settings.openFLTarget) return false;
    if (myHXCPPPort != settings.myHXCPPPort) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = mainClass != null ? mainClass.hashCode() : 0;
    result = 31 * result + (outputFileName != null ? outputFileName.hashCode() : 0);
    result = 31 * result + (arguments != null ? arguments.hashCode() : 0);
    result = 31 * result + (nmeFlags != null ? nmeFlags.hashCode() : 0);
    result = 31 * result + (openFLFlags != null ? openFLFlags.hashCode() : 0);
    result = 31 * result + (excludeFromCompilation ? 1 : 0);
    result = 31 * result + (haxeTarget != null ? haxeTarget.hashCode() : 0);
    result = 31 * result + (nmeTarget != null ? nmeTarget.hashCode() : 0);
    result = 31 * result + (openFLTarget != null ? openFLTarget.hashCode() : 0);
    result = 31 * result + (flexSdkName != null ? flexSdkName.hashCode() : 0);
    result = 31 * result + (hxmlPath != null ? hxmlPath.hashCode() : 0);
    result = 31 * result + (myHXCPPPort != null ? myHXCPPPort.hashCode() : 0);
    result = 31 * result + buildConfig;
    return result;
  }
}
