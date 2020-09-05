/*
 * Copyright 2020 Eric Bishton
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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import org.jetbrains.annotations.NotNull;

public class HaxeProjectUtil {
  private HaxeProjectUtil() {} // Static interface only.

  /**
   * Gets the most likely "current" project.  When multiple projects are available,
   * checks for the SDK type.
   *
   * This should only be used when no other information (e.g. {@link Project},
   * {@link com.intellij.openapi.module.Module},
   * {@link com.intellij.psi.PsiElement}, or {@link com.intellij.lang.ASTNode})
   * is available to give the project information.
   *
   * XXX: We *could* check the module types to see if any of them use a Haxe SDK.
   *
   * @return the first open project using a Haxe SDK; the first open project if none
   *         do; the default project if none are open.
   */
  @NotNull
  public static Project getLikelyCurrentProject() {
    Project[] projects = ProjectManagerEx.getInstance().getOpenProjects();
    for (Project project : projects) {
      ProjectRootManager mgr = ProjectRootManager.getInstance(project);
      Sdk sdk = null != mgr ? mgr.getProjectSdk() : null;
      if (sdk instanceof HaxeSdkType) {
        return project;
      }
    }
    return projects.length > 0 ? projects[0] : ProjectManagerEx.getInstanceEx().getDefaultProject();
  }
}
