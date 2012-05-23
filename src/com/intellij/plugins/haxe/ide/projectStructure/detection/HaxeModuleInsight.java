package com.intellij.plugins.haxe.ide.projectStructure.detection;

import com.intellij.ide.util.importProject.ModuleDescriptor;
import com.intellij.ide.util.importProject.ModuleInsight;
import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.plugins.haxe.HaxeFileType;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeModuleInsight extends ModuleInsight {
  public HaxeModuleInsight(@Nullable final ProgressIndicator progress,
                           Set<String> existingModuleNames,
                           Set<String> existingProjectLibraryNames) {
    super(progress, existingModuleNames, existingProjectLibraryNames);
  }

  @Override
  protected ModuleDescriptor createModuleDescriptor(File moduleContentRoot, Collection<DetectedProjectRoot> sourceRoots) {
    return new ModuleDescriptor(moduleContentRoot, HaxeModuleType.getInstance(), sourceRoots);
  }

  @Override
  public boolean isApplicableRoot(DetectedProjectRoot root) {
    return root instanceof HaxeModuleSourceRoot;
  }

  @Override
  protected boolean isSourceFile(File file) {
    return file.getName().endsWith(HaxeFileType.DEFAULT_EXTENSION);
  }

  @Override
  protected boolean isLibraryFile(String fileName) {
    return false;
  }

  @Override
  protected void scanSourceFileForImportedPackages(CharSequence chars, Consumer<String> result) {
  }

  @Override
  protected void scanLibraryForDeclaredPackages(File file, Consumer<String> result) throws IOException {
  }
}
