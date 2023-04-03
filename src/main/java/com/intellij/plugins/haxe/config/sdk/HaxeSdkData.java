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
package com.intellij.plugins.haxe.config.sdk;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.plugins.haxe.config.sdk.impl.HaxeSdkAdditionalDataBaseImpl;
import com.intellij.util.xmlb.XmlSerializerUtil;

public class HaxeSdkData extends HaxeSdkAdditionalDataBaseImpl implements SdkAdditionalData, PersistentStateComponent<HaxeSdkData> {
  public HaxeSdkData() {
    super();
  }

  public HaxeSdkData(String homePath, String version) {
    super(homePath, version);
  }

  @SuppressWarnings({"CloneDoesntCallSuperClone"})
  @Override
  public Object clone() throws CloneNotSupportedException {
    return super.clone();
  }

  public HaxeSdkData getState() {
    return this;
  }

  public void loadState(HaxeSdkData state) {
    XmlSerializerUtil.copyBean(state, this);
  }
}