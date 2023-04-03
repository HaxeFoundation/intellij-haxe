/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2023 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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
package com.intellij.plugins.haxe.config.sdk.impl;

import com.intellij.plugins.haxe.config.sdk.HaxeSdkAdditionalDataBase;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeSdkAdditionalDataBaseImpl implements HaxeSdkAdditionalDataBase {
  private String homePath = "";
  private String version = "";

  private String nekoBinPath = "";

  private String haxelibPath = "";

  private boolean useCompilerCompletionFlag = false;
  private boolean removeCompletionDuplicatesFlag = true;

  public HaxeSdkAdditionalDataBaseImpl() {
  }

  public HaxeSdkAdditionalDataBaseImpl(String homePath, String version) {
    this.homePath = null == homePath ? "" : homePath;
    this.version  = null == version  ? "" : version;
  }

  public String getHomePath() {
    return homePath;
  }

  public String getVersion() {
    return version;
  }

  public String getNekoBinPath() {
    return nekoBinPath;
  }

  public void setNekoBinPath(String nekoBinPath) {
    this.nekoBinPath = null == nekoBinPath ? "" : nekoBinPath;
  }

  public String getHaxelibPath() {
    return haxelibPath;
  }

  public void setHaxelibPath(String haxelibPath) {
    this.haxelibPath = null == haxelibPath ? "" : haxelibPath;
  }

  public boolean getUseCompilerCompletionFlag() {
    return useCompilerCompletionFlag;
  }

  public void setUseCompilerCompletionFlag(boolean newState) {
    useCompilerCompletionFlag = newState;
  }

  public boolean getRemoveCompletionDuplicatesFlag() {
    return removeCompletionDuplicatesFlag;
  }

  public void setRemoveCompletionDuplicatesFlag(boolean newState) {
    removeCompletionDuplicatesFlag = newState;
  }
}
