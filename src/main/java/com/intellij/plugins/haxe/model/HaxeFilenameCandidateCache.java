package com.intellij.plugins.haxe.model;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memoizes FilenameIndex candidate lookups per file name, valid until the
 * next code change (cleared with the evaluator caches). Package-file
 * resolution probes the same names constantly - std primitives like Void or
 * Int probe "Void.hx" on every type creation and can never hit (they live in
 * StdTypes.hx) - so without the memo every probe pays a full index query.
 */
@Service(Service.Level.PROJECT)
public final class HaxeFilenameCandidateCache {

  private final Project project;
  private final Map<String, Collection<VirtualFile>> byName = new ConcurrentHashMap<>();

  public HaxeFilenameCandidateCache(@NotNull Project project) {
    this.project = project;
  }

  public static HaxeFilenameCandidateCache getInstance(@NotNull Project project) {
    return project.getService(HaxeFilenameCandidateCache.class);
  }

  /** All files with this name in project+library scope; empty means a definitive miss. */
  public @NotNull Collection<VirtualFile> candidatesFor(@NotNull String fileName) {
    return byName.computeIfAbsent(fileName, name ->
      FilenameIndex.getVirtualFilesByName(name, GlobalSearchScope.allScope(project)));
  }

  public void clearCaches() {
    byName.clear();
  }
}
