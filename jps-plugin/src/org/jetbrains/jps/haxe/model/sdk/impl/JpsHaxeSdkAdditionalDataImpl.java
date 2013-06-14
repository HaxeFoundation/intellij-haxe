/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.jetbrains.jps.haxe.model.sdk.impl;

import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.haxe.model.sdk.JpsHaxeSdkAdditionalData;
import org.jetbrains.jps.model.ex.JpsElementBase;

/**
 * @author: Fedor.Korotkov
 */
public class JpsHaxeSdkAdditionalDataImpl extends JpsElementBase<JpsHaxeSdkAdditionalDataImpl> implements JpsHaxeSdkAdditionalData {
  private HaxeSdkAdditionalDataBase myAdditionalData;

  public JpsHaxeSdkAdditionalDataImpl(HaxeSdkAdditionalDataBase additionalData) {
    myAdditionalData = additionalData;
  }

  @Override
  public HaxeSdkAdditionalDataBase getSdkData() {
    return myAdditionalData;
  }

  @Override
  public String getHomePath() {
    return myAdditionalData.getHomePath();
  }

  @Override
  public String getVersion() {
    return myAdditionalData.getVersion();
  }

  @Override
  public String getNekoBinPath() {
    return myAdditionalData.getNekoBinPath();
  }

  @Override
  public void setNekoBinPath(String nekoBinPath) {
    myAdditionalData.setNekoBinPath(nekoBinPath);
  }

  @Override
  public String getHaxelibPath() {
    return myAdditionalData.getHaxelibPath();
  }

  @Override
  public void setHaxelibPath(String haxelibPath) {
    myAdditionalData.setHaxelibPath(haxelibPath);
  }

  @NotNull
  @Override
  public JpsHaxeSdkAdditionalDataImpl createCopy() {
    return new JpsHaxeSdkAdditionalDataImpl(myAdditionalData);
  }

  @Override
  public void applyChanges(@NotNull JpsHaxeSdkAdditionalDataImpl modified) {
    myAdditionalData = modified.myAdditionalData;
  }
}
