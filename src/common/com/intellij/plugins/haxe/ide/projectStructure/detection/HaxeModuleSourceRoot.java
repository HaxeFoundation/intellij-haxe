/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
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

import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.DetectedSourceRoot;
import com.intellij.plugins.haxe.HaxeBundle;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author: Fedor.Korotkov
 */
public class HaxeModuleSourceRoot extends DetectedSourceRoot {

  protected HaxeModuleSourceRoot(File directory) {
    super(directory, null);
  }

  @NotNull
  @Override
  public String getRootTypeName() {
    return HaxeBundle.message("autodetected.source.root.type");
  }

  public boolean canContainRoot(@NotNull final DetectedProjectRoot root) {
    return !(root instanceof HaxeModuleSourceRoot);
  }

  public DetectedProjectRoot combineWith(@NotNull final DetectedProjectRoot root) {
    return root instanceof HaxeModuleSourceRoot ? this : null;
  }
}
