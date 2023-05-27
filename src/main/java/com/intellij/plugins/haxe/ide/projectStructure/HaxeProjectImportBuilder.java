package com.intellij.plugins.haxe.ide.projectStructure;

import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.ModifiableModuleModel;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.impl.ModifiableModelCommitter;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.packaging.artifacts.ModifiableArtifactModel;
import com.intellij.plugins.haxe.HaxeBundle;
import com.intellij.plugins.haxe.config.HaxeConfiguration;
import com.intellij.plugins.haxe.config.sdk.HaxeSdkType;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.module.HaxeModuleType;
import com.intellij.plugins.haxe.ide.projectStructure.detection.HaxeProjectData;
import com.intellij.projectImport.ProjectImportBuilder;
import icons.HaxeIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HaxeProjectImportBuilder extends ProjectImportBuilder<Object> {

  @Override
  public @NotNull @Nls(capitalization = Nls.Capitalization.Sentence) String getName() {
    return HaxeBundle.message("haxe.project");
  }


  @Override
  public Icon getIcon() {
    return HaxeIcons.HAXE_LOGO;
  }

  @Override
  public boolean isMarked(Object element) {
    return true;
  }

  @Override
  public void setOpenProjectSettingsAfter(boolean on) {

  }


  @Override
  public @Nullable List<Module> commit(Project project,
                                       ModifiableModuleModel model,
                                       ModulesProvider modulesProvider,
                                       ModifiableArtifactModel artifactModel) {
    final ModifiableModuleModel moduleModel = model != null ? model : ModuleManager.getInstance(project).getModifiableModel();


    final Map<Module, ModifiableRootModel> moduleToModifiableModelMap = new HashMap<>();

    final HaxeProjectData haxeProjectData = new HaxeProjectData(getFileToImport());
    final String moduleName = haxeProjectData.getName();

    final String moduleFilePath = haxeProjectData.getProjectRootPath() + "/" + moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION;
    final Module module = moduleModel.newModule(moduleFilePath, HaxeModuleType.getInstance().getId());

    if (LocalFileSystem.getInstance().findFileByPath(moduleFilePath) != null) {
      ApplicationManager.getApplication().runWriteAction(() -> ModuleBuilder.deleteModuleFile(moduleFilePath));
    }

    final ModifiableRootModel rootModel = ModuleRootManager.getInstance(module).getModifiableModel();
    final HaxeModuleSettings instance = HaxeModuleSettings.getInstance(module);

    configureProjectSettings(haxeProjectData, instance);
    configureContentRoots(haxeProjectData, rootModel);
    moduleToModifiableModelMap.put(module, rootModel);

    final Sdk mostRecentSdk = ProjectJdkTable.getInstance().findMostRecentSdkOfType(HaxeSdkType.getInstance());
    rootModel.setSdk(mostRecentSdk);

    ApplicationManager.getApplication()
      .runWriteAction(() -> ModifiableModelCommitter.multiCommit(moduleToModifiableModelMap.values(), moduleModel));


    return List.of(module);
  }

  private static void configureProjectSettings(HaxeProjectData haxeProjectData, HaxeModuleSettings instance) {
    HaxeConfiguration type = haxeProjectData.getProjectType();
    instance.setOutputFolder(haxeProjectData.getOutputFolder());
    instance.setBuildConfig(type.asBuildConfigValue());
    switch (type) {
      case OPENFL -> instance.setOpenFLPath(haxeProjectData.getProjectFilePath());
      case HXML ->  instance.setHxmlPath(haxeProjectData.getProjectFilePath());
      case NMML -> instance.setNmmlPath(haxeProjectData.getProjectFilePath());
    }
  }

  private static void configureContentRoots(HaxeProjectData haxeProjectData, ModifiableRootModel rootModel) {
    ContentEntry contentRoot = rootModel.addContentEntry(haxeProjectData.getProjectRoot());
    haxeProjectData.getSourcePaths().forEach(source -> contentRoot.addSourceFolder(source, false));
  }
}
