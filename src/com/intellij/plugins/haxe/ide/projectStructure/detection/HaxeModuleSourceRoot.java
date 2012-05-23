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
