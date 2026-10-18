package com.intellij.plugins.haxe.v2.buildtools.classpaths;

import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.psi.ResolveScopeEnlarger;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.SearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Widens a Haxe file's resolve scope by its module's external classpaths ({@link HaxeExternalClasspathStore}). */
// TODO: the union over every build of the module; a per-context scope would limit a file to its own build's classpaths
public final class HaxeExternalClasspathResolveScopeEnlarger extends ResolveScopeEnlarger {

  @Override
  public @Nullable SearchScope getAdditionalResolveScope(@NotNull VirtualFile file, @NotNull Project project) {
    if (!FileTypeRegistry.getInstance().isFileOfType(file, HaxeFileType.INSTANCE)) return null;
    Module module = ProjectFileIndex.getInstance(project).getModuleForFile(file);
    if (module == null) return null;

    List<VirtualFile> directories = HaxeExternalClasspathStore.getInstance(project).directoriesOf(module.getName());
    if (directories.isEmpty()) return null;
    return GlobalSearchScopesCore.directoriesScope(project, true, directories.toArray(VirtualFile.EMPTY_ARRAY));
  }
}
