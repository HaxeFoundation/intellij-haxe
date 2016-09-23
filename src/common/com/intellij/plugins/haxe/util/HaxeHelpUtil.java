/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
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
package com.intellij.plugins.haxe.util;

import com.intellij.execution.Platform;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkAdditionalData;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkData;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;

import java.io.File;

/**
 * Created by as3boyan on 15.11.14.
 */
public class HaxeHelpUtil {
  public static String getHaxePath(Module myModule) {
    String haxePath = "haxe";
    if (myModule != null) {
      Sdk sdk = ModuleRootManager.getInstance(myModule).getSdk();
      if (sdk != null && sdk.getSdkType().equals(HaxeSdkType.getInstance())) {
        String path = FileUtil.toSystemIndependentName(sdk.getHomePath()) + "/haxe";

        if (Platform.current().equals(Platform.WINDOWS)) {
          path += ".exe";
        }

        File file = new File(path);
        if (!path.isEmpty() && file.exists() && file.isFile()) {
          haxePath = path;
        }
      }
    }

    return haxePath;
  }
}
