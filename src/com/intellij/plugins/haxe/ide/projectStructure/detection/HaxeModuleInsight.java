/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.plugins.haxe.ide.projectStructure.detection;

import com.intellij.ide.util.importProject.ModuleDescriptor;
import com.intellij.ide.util.importProject.ModuleInsight;
import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.DetectedSourceRoot;
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
