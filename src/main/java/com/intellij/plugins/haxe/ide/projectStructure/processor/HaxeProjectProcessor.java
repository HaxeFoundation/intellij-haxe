package com.intellij.plugins.haxe.ide.projectStructure.processor;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.ide.projectStructure.HaxeProjectImportBuilder;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.projectImport.ProjectOpenProcessorBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class HaxeProjectProcessor extends ProjectOpenProcessorBase<HaxeProjectImportBuilder> {

  @Override
  protected boolean doQuickImport(@NotNull VirtualFile file, @NotNull WizardContext wizardContext) {
    HaxeProjectImportBuilder builder = getBuilder();
    builder.setFileToImport(file.getPath());

    wizardContext.setProjectName(file.getParent().getName());
    return true;
  }
  @NotNull
  @Override
  protected HaxeProjectImportBuilder doGetBuilder() {
    return ProjectImportBuilder.EXTENSIONS_POINT_NAME.findExtensionOrFail(HaxeProjectImportBuilder.class);
  }
}
