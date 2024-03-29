package com.intellij.plugins.haxe.ide.projectStructure.processor;

import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.haxelib.HaxelibProjectUpdater;
import com.intellij.plugins.haxe.ide.projectStructure.HaxeProjectImportBuilder;
import com.intellij.projectImport.ProjectImportBuilder;
import com.intellij.projectImport.ProjectOpenProcessorBase;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@CustomLog
public abstract class HaxeProjectProcessor extends ProjectOpenProcessorBase<HaxeProjectImportBuilder> {

  @Override
  public boolean isStrongProjectInfoHolder() {
    return true;
  }

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

  @Override
  public boolean canOpenProject(@NotNull VirtualFile file) {
    String path = file.getCanonicalPath();
    if (path == null || path.contains(".haxelib")) {
      log.debug("Ignoring files in .haxelib repo");
      return false;
    }
    return true;
  }

  @Override
  public boolean canImportProjectAfterwards() {
    return true;
  }

  @Override
  public void importProjectAfterwards(@NotNull Project project, @NotNull VirtualFile projectFile) {

    // TODO add when android studio moves to 2023.x
    //VirtualFile projectRoot = projectFile.isDirectory() ? projectFile : projectFile.getParent();
    //boolean allowUntrusted = ExternalSystemTrustedProjectDialog.INSTANCE.confirmLinkingUntrustedProject(project, ProjectSystemId.IDE, projectRoot.toNioPath());

    //if (allowUntrusted) {
    HaxeProjectImportBuilder builder = getBuilder();
    try {
      builder.setUpdate(true);
      builder.setFileToImport(Objects.requireNonNull(projectFile.getCanonicalPath()));
      if (builder.validate(null, project)) {
        builder.commit(project, null, ModulesProvider.EMPTY_MODULES_PROVIDER);
      }
    }
    finally {
      builder.cleanup();
    }

    HaxelibProjectUpdater.ProjectTracker tracker = HaxelibProjectUpdater.INSTANCE.findProjectTracker(project);
    if (tracker != null) {
      HaxelibProjectUpdater.INSTANCE.synchronizeClasspaths(tracker);
    }
    else {
      log.error("Project tracker not found, unable to synchronize classpaths");
    }
    //}
  }
}
