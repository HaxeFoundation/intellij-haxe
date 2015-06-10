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
package com.intellij.plugins.haxe.haxelib;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import org.jetbrains.annotations.NotNull;

/**
 * Static interface for SDK utility functions.
 */
public class HaxelibSdkUtils {

  private static final Logger LOG = Logger.getInstance("#com.intellij.plugin.haxe.haxelib.HaxelibSdkUtils");

  /**
   * An SDK that can be used/returned when project lookup fails.
   */
  static public Sdk DefaultSDK;
  static {
    HaxeSdkType sdkType = HaxeSdkType.getInstance();
    DefaultSDK = new ProjectJdkImpl(sdkType.suggestSdkName(null, sdkType.suggestHomePath()),
                                    sdkType,
                                    sdkType.suggestHomePath(),
                                    sdkType.getVersionString(sdkType.suggestHomePath()));
    sdkType.setupSdkPaths(DefaultSDK);
  }
  static boolean myDefaultSdkErrorHasBeenLogged = false;

  // Static interface only.
  private HaxelibSdkUtils() {}

  /**
   * Look up the actual SDK in use by a module.  The SDK may be specified by
   * the module, or it may be inherited from the Project.  Either way, the
   * correct SDK will be returned.
   *
   * @param module - the module to check
   * @return the SDK instance, or DefaultSDK if the name of the selected SDK does not correspond
   * to any existing SDK instance.
   */
  @NotNull
  public static Sdk lookupSdk(@NotNull Module module) {
    Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
    if (null == sdk) {
      // TODO: Move error string to a resource in HaxeBundle.
      sdk = getDefaultSDK("Invalid (or no) SDK specified for module " + module.getName());
    }
    return sdk;
  }

  /**
   * Lookup the project's SDK.
   *
   * @param project to look up the SDK for.
   * @return the SDK instance, or DefaultSDK if the name of the selected SDK does not correspond
   * to any existing SDK instance.
   */
  @NotNull
  public static Sdk lookupSdk(@NotNull Project project) {
    Sdk sdk = ProjectRootManager.getInstance(project).getProjectSdk();
    if (null == sdk) {
      // TODO: Move error string to a resource in HaxeBundle.
      sdk = getDefaultSDK("Invalid (or no) SDK specified for project " + project.getName());
    }
    return sdk;
  }


  @NotNull
  public static Sdk getDefaultSDK(String errorMessage) {
    if (null != errorMessage) {
      // We can argue whether this is just a warning, because we "fix" the
      // problem.  However, an error pops out on the user log, so that they
      // can take action.
      // We only log one error, as only one gets displayed to the user via
      // the UI anyway, and it floods the logs.
      if (!myDefaultSdkErrorHasBeenLogged) {
        LOG.warn(errorMessage);
        myDefaultSdkErrorHasBeenLogged = true;
      }
    }
    return DefaultSDK;
  }

}
