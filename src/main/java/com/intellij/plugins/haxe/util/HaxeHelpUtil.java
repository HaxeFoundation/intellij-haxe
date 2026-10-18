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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;


public class HaxeHelpUtil {

  /** The compiler for the module: its SDK's haxe (in the home or its bin), else the bare name for a PATH lookup. */
  public static String getHaxePath(Module module) {
    Sdk sdk = module != null ? ModuleRootManager.getInstance(module).getSdk() : null;
    String home = sdk != null ? sdk.getHomePath() : null;
    String compiler = home != null ? HaxeSdkUtilBase.getCompilerPathByFolderPath(home) : null;
    return compiler != null ? compiler : HaxeSdkUtilBase.getExecutableName("haxe");
  }
}
