package com.intellij.plugins.haxe.v2.buildtools.settings;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Path bookkeeping shared by the stores keyed by file path: a moved or renamed file, or a folder above it, keeps its entries. */
public final class HaxeStoredPaths {

  private HaxeStoredPaths() {
  }

  /** {@code path} after {@code oldPath} moved to {@code newPath}: the new path for the file itself or one below it, else unchanged. */
  @NotNull
  public static String moved(@NotNull String path, @NotNull String oldPath, @NotNull String newPath) {
    if (path.equals(oldPath)) return newPath;
    return path.startsWith(oldPath + "/") ? newPath + path.substring(oldPath.length()) : path;
  }

  /** Re-keys a path-keyed map in place; whether any key moved. */
  public static <V> boolean moveKeys(@NotNull Map<String, V> byPath, @NotNull String oldPath, @NotNull String newPath) {
    List<String> affected = new ArrayList<>();
    for (String path : byPath.keySet()) {
      if (!moved(path, oldPath, newPath).equals(path)) affected.add(path);
    }
    for (String path : affected) {
      byPath.put(moved(path, oldPath, newPath), byPath.remove(path));
    }
    return !affected.isEmpty();
  }

  /** Re-points the path values of a map in place; whether any moved. */
  public static boolean moveValues(@NotNull Map<String, String> byKey, @NotNull String oldPath, @NotNull String newPath) {
    boolean changed = false;
    for (Map.Entry<String, String> entry : byKey.entrySet()) {
      String movedPath = moved(entry.getValue(), oldPath, newPath);
      if (!movedPath.equals(entry.getValue())) {
        entry.setValue(movedPath);
        changed = true;
      }
    }
    return changed;
  }

  /** Rewrites the paths of a list in place; whether any moved. */
  public static boolean moveAll(@NotNull List<String> paths, @NotNull String oldPath, @NotNull String newPath) {
    boolean changed = false;
    for (int i = 0; i < paths.size(); i++) {
      String movedPath = moved(paths.get(i), oldPath, newPath);
      if (!movedPath.equals(paths.get(i))) {
        paths.set(i, movedPath);
        changed = true;
      }
    }
    return changed;
  }
}
