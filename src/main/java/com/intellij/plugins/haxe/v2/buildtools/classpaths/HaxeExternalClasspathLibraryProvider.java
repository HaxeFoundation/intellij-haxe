package com.intellij.plugins.haxe.v2.buildtools.classpaths;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

/**
 * Indexes the external classpaths no module holds
 * ({@link HaxeExternalClasspathStore}): one synthetic library per folder,
 * listed under External Libraries, with nothing written to the module setup.
 */
public final class HaxeExternalClasspathLibraryProvider extends AdditionalLibraryRootsProvider {

  @Override
  public @NotNull Collection<SyntheticLibrary> getAdditionalProjectLibraries(@NotNull Project project) {
    return outsideModuleDirectories(project).stream()
      .<SyntheticLibrary>map(HaxeExternalClasspathLibrary::new)
      .toList();
  }

  @Override
  public @NotNull Collection<VirtualFile> getRootsToWatch(@NotNull Project project) {
    return outsideModuleDirectories(project);
  }

  @NotNull
  private static List<VirtualFile> outsideModuleDirectories(@NotNull Project project) {
    if (project.isDefault()) return List.of();
    return HaxeExternalClasspathStore.getInstance(project).outsideModuleDirectories();
  }
}
