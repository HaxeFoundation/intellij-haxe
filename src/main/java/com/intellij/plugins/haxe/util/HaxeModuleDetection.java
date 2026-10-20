package com.intellij.plugins.haxe.util;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.buildsystem.hxml.HXMLFileType;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.v2.buildtools.settings.HaxeEnvironmentStore;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

/**
 * Whether a module is haxe-related. v2 modules are plain modules (no module
 * type), so haxe-ness comes from content and configuration: haxe or hxml
 * files, or a configured environment SDK. The legacy HAXE_MODULE type still
 * counts for old projects. Call in a read action.
 */
public final class HaxeModuleDetection {

  private HaxeModuleDetection() {
  }

  public static boolean isHaxeModule(@NotNull Module module) {
    if (module.isDisposed()) return false;
    if (ModuleType.get(module) == HaxeModuleType.getInstance()) return true;
    if (HaxeEnvironmentStore.getInstance(module.getProject()).getSdkName(module.getName()) != null) return true;
    GlobalSearchScope scope = module.getModuleContentScope();
    return FileTypeIndex.containsFileOfType(HaxeFileType.INSTANCE, scope)
           || FileTypeIndex.containsFileOfType(HXMLFileType.INSTANCE, scope);
  }
}
