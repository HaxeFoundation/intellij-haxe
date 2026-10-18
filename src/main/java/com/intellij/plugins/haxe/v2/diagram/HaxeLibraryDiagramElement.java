package com.intellij.plugins.haxe.v2.diagram;

import com.intellij.openapi.actionSystem.DataKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What a node of the library diagram stands for: the build file the diagram
 * is rooted at, by path, or one library, by name. The qualified name is how
 * the Diagrams plugin remembers an open diagram across restarts.
 */
public record HaxeLibraryDiagramElement(@NotNull Kind kind, @NotNull String key) {

  public enum Kind { BUILD_FILE, LIBRARY }

  /** A compiler define shown inside a node: the build file's own, or the one a library contributes. */
  public record Define(@NotNull String name, @Nullable String value) {
  }

  /** The data-context key the opener passes the root element under; the element manager reads it back. */
  public static final DataKey<HaxeLibraryDiagramElement> DATA_KEY = DataKey.create("haxe.library.diagram.element");

  private static final String BUILD_FILE_PREFIX = "build:";
  private static final String LIBRARY_PREFIX = "lib:";

  @NotNull
  public static HaxeLibraryDiagramElement buildFile(@NotNull String path) {
    return new HaxeLibraryDiagramElement(Kind.BUILD_FILE, path);
  }

  @NotNull
  public static HaxeLibraryDiagramElement library(@NotNull String name) {
    return new HaxeLibraryDiagramElement(Kind.LIBRARY, name);
  }

  public boolean isBuildFile() {
    return kind == Kind.BUILD_FILE;
  }

  /** {@code build:<path>} or {@code lib:<name>}. */
  @NotNull
  public String qualifiedName() {
    return (isBuildFile() ? BUILD_FILE_PREFIX : LIBRARY_PREFIX) + key;
  }

  @Nullable
  public static HaxeLibraryDiagramElement fromQualifiedName(@NotNull String qualifiedName) {
    if (qualifiedName.startsWith(BUILD_FILE_PREFIX)) return buildFile(qualifiedName.substring(BUILD_FILE_PREFIX.length()));
    if (qualifiedName.startsWith(LIBRARY_PREFIX)) return library(qualifiedName.substring(LIBRARY_PREFIX.length()));
    return null;
  }
}
