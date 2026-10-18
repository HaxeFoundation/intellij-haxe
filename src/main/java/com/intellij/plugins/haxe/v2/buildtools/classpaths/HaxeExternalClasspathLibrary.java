package com.intellij.plugins.haxe.v2.buildtools.classpaths;

import com.intellij.icons.AllIcons;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.roots.JavaSyntheticLibrary;
import com.intellij.openapi.roots.SyntheticLibrary;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.List;
import java.util.Set;

/**
 * One external classpath folder outside every module, shown under External
 * Libraries. It extends {@link JavaSyntheticLibrary}, not
 * {@link SyntheticLibrary}: the platform registers a synthetic library's
 * folders as package roots only for that type, and the Haxe package model
 * finds packages through the package index, so the package part of
 * {@code import pack.Type} would otherwise stay unresolved.
 */
final class HaxeExternalClasspathLibrary extends JavaSyntheticLibrary implements ItemPresentation {

  private final VirtualFile directory;

  HaxeExternalClasspathLibrary(@NotNull VirtualFile directory) {
    super("haxe-classpath:" + directory.getPath(), List.of(directory), List.of(), Set.of());
    this.directory = directory;
  }

  @Override
  public @NotNull String getPresentableText() {
    return HaxeBundle.message("haxe.library.external.classpath", directory.getPresentableUrl());
  }

  @Override
  public @NotNull Icon getIcon(boolean unused) {
    return AllIcons.Nodes.PpLibFolder;
  }
}
