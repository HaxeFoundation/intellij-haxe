/*
 * Copyright 2018-2018 Ilya Malanin
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
package com.intellij.plugins.haxe.codeInsight.daemon;

import com.intellij.codeInsight.daemon.impl.JavaProjectSdkSetupValidator;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ui.configuration.SdkPopupBuilder;
import com.intellij.openapi.roots.ui.configuration.SdkPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.HaxeLanguage;
import com.intellij.plugins.haxe.HaxeProjectBundle;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.EditorNotificationPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.intellij.plugins.haxe.codeInsight.daemon.SdkValidationResult.*;
import static com.intellij.plugins.haxe.model.HaxeStdTypesFileModel.STD_TYPES_HX;

public class HaxeProjectSdkSetupValidator extends JavaProjectSdkSetupValidator {

  @Override
  public boolean isApplicableFor(@NotNull Project project, @NotNull VirtualFile file) {
    final PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
    return psiFile != null && psiFile.getLanguage().isKindOf(HaxeLanguage.INSTANCE);
  }

  @Nullable
  @Override
  public String getErrorMessage(@NotNull Project project, @NotNull VirtualFile file) {
    SdkValidationResult result = validateSdk(project, file);
    if (result != null) {
      switch (result) {
        case MODULE_SDK_NOT_DEFINED:
          return HaxeProjectBundle.message("module.sdk.not.defined");
        case PROJECT_SDK_NOT_DEFINED:
          return HaxeProjectBundle.message("project.sdk.not.defined");
        case MULTIPLE_ROOTS_FOUND:
          return HaxeBundle.message("sdk.roots.multiple");
        case NO_VALID_SDK_ROOTS_FOUND:
          return HaxeBundle.message("sdk.roots.no.valid.root");
      }
    }

    return null;
  }

  private SdkValidationResult validateSdk(Project project, VirtualFile file) {
    final Module module = ModuleUtilCore.findModuleForFile(file, project);
    if (module != null && !module.isDisposed()) {
      final Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
      if (sdk == null) {
        if (ModuleRootManager.getInstance(module).isSdkInherited()) {
          return PROJECT_SDK_NOT_DEFINED;
        }
        else {
          return MODULE_SDK_NOT_DEFINED;
        }
      }
      else {
        return validateSdkRoots(sdk);
      }
    }
    return null;
  }

  private SdkValidationResult validateSdkRoots(Sdk sdk) {
    List<VirtualFile> roots = getDistinctRoots(sdk);

    if (hasNoValidRoots(roots)) {
      return NO_VALID_SDK_ROOTS_FOUND;
    }
    if (hasMultipleOrEmptyRoots(roots)) {
      return MULTIPLE_ROOTS_FOUND;
    }

    return null;
  }

  private List<VirtualFile> getDistinctRoots(Sdk sdk) {
    return getDistinctRootsStream(sdk).collect(Collectors.toList());
  }

  private Stream<VirtualFile> getDistinctRootsStream(Sdk sdk) {
    return Stream.concat(
      Arrays.stream(sdk.getRootProvider().getFiles(OrderRootType.CLASSES)),
      Arrays.stream(sdk.getRootProvider().getFiles(OrderRootType.SOURCES))
    ).distinct();
  }

  private boolean hasNoValidRoots(List<VirtualFile> roots) {
    return roots.stream().noneMatch(root -> root.findChild(STD_TYPES_HX) != null);
  }

  private boolean hasMultipleOrEmptyRoots(List<VirtualFile> roots) {
    return roots.size() != 1;
  }

  @Override
  public @NotNull EditorNotificationPanel.ActionHandler getFixHandler(@NotNull Project project, @NotNull VirtualFile file) {
    return preparePopup(project, file).buildEditorNotificationPanelHandler();
  }

  private @NotNull SdkPopupBuilder preparePopup(@NotNull Project project, @NotNull VirtualFile file) {
    return SdkPopupFactory
      .newBuilder()
      .withProject(project)
      .withSdkTypeFilter(type -> type instanceof HaxeSdkType)
      .updateSdkForFile(file);
  }
}

enum SdkValidationResult {
  PROJECT_SDK_NOT_DEFINED,
  MODULE_SDK_NOT_DEFINED,
  MULTIPLE_ROOTS_FOUND,
  NO_VALID_SDK_ROOTS_FOUND
}
