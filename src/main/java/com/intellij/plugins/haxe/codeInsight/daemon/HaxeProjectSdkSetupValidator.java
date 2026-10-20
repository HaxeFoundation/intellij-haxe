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
import com.intellij.plugins.haxe.util.HaxeEnvironmentVariables;
import com.intellij.plugins.haxe.v2.buildtools.projectmodel.HaxeModuleSdkApplier;
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
import static com.intellij.plugins.haxe.util.HaxeEnvironmentVariables.HAXE_STD_PATH;

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
    if (result == null) return null;

    return switch (result) {
      // v2 never demands a Project SDK - mixed-language projects use one
      // SDK per module, configured here or in the tool window's Environment
      case MODULE_SDK_NOT_DEFINED, PROJECT_SDK_NOT_DEFINED -> HaxeProjectBundle.message("module.haxe.sdk.not.configured");
      case NO_VALID_SDK_ROOTS_FOUND -> noValidRootMessage();
    };
  }

  /** With HAXE_STD_PATH set, the SDK's std folders come from the variable alone, so the message names it. */
  private static String noValidRootMessage() {
    String stdPathValue = HaxeEnvironmentVariables.value(HAXE_STD_PATH);
    return stdPathValue == null
           ? HaxeBundle.message("sdk.roots.no.valid.root")
           : HaxeBundle.message("sdk.roots.no.valid.root.std.path", stdPathValue);
  }

  /**
   * Validates what resolution ACTUALLY uses: the module's own SDK dependency
   * (the applier mirrors the v2 environment/settings choice into it). No
   * shortcut through the v2 stores here — a configured-but-unapplied SDK must
   * keep the banner visible, it means the resolve scope is really missing the
   * std roots.
   */
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
    return hasNoValidRoots(roots) ? NO_VALID_SDK_ROOTS_FOUND : null;
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

  @Override
  public @NotNull EditorNotificationPanel.ActionHandler getFixHandler(@NotNull Project project, @NotNull VirtualFile file) {
    return preparePopup(project, file).buildEditorNotificationPanelHandler();
  }

  private @NotNull SdkPopupBuilder preparePopup(@NotNull Project project, @NotNull VirtualFile file) {
    return SdkPopupFactory
      .newBuilder()
      .withProject(project)
      .withSdkTypeFilter(type -> type instanceof HaxeSdkType)
      // a detected or newly added SDK is only created by the popup; without
      // this it never reaches the SDK table and the module names a missing SDK
      .registerNewSdk()
      // never the project SDK: the choice lands on the MODULE, via the same
      // path as the tool window's Environment
      .onSdkSelected(sdk -> {
        Module module = ModuleUtilCore.findModuleForFile(file, project);
        if (module != null) {
          HaxeModuleSdkApplier.getInstance(project).chooseSdk(module.getName(), sdk.getName());
        }
      });
  }
}

enum SdkValidationResult {
  PROJECT_SDK_NOT_DEFINED,
  MODULE_SDK_NOT_DEFINED,
  NO_VALID_SDK_ROOTS_FOUND
}
