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

  public HaxeSdkAdditionalDataBaseImpl() {
  }

  public HaxeSdkAdditionalDataBaseImpl(String homePath, String version) {
    this.homePath = homePath;
    this.version = version;
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
    this.nekoBinPath = nekoBinPath;
  }

  public String getHaxelibPath() {
    return haxelibPath;
  }

  public void setHaxelibPath(String haxelibPath) {
    this.haxelibPath = haxelibPath;
  }
}
