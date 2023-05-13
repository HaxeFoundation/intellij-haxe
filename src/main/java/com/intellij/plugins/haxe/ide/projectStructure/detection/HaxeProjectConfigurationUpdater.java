/*
 * Copyright 2018 Aleksandr Kuzmenko
 * Copyright 2018 Eric Bishton
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

import com.intellij.execution.RunManager;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.ide.util.projectWizard.importSources.impl.ProjectFromSourcesBuilderImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ModifiableModelsProvider;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.config.HaxeConfiguration;
import com.intellij.plugins.haxe.config.HaxeTarget;
import com.intellij.plugins.haxe.haxelib.HaxelibClasspathUtils;
import com.intellij.plugins.haxe.haxelib.HaxelibMetadata;
import com.intellij.plugins.haxe.haxelib.HaxelibSdkUtils;
import com.intellij.plugins.haxe.ide.library.HaxeLibraryType;
import com.intellij.plugins.haxe.ide.module.HaxeModuleSettings;
import com.intellij.plugins.haxe.ide.projectStructure.HXMLData;
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class HaxeProjectConfigurationUpdater implements ProjectFromSourcesBuilderImpl.ProjectConfigurationUpdater {
  private Set<String> myLibraries;
  private String myHxml;
  private String myProjectRoot;

  public HaxeProjectConfigurationUpdater(String projectRoot) {
    this.myProjectRoot = projectRoot;
  }

  @Override
  public void updateProject(@NotNull Project project, @NotNull ModifiableModelsProvider modelsProvider, @NotNull ModulesProvider modulesProvider) {
    if(myLibraries != null && !myLibraries.isEmpty()) {
      List<LibraryData> libraries = collectLibraries(project);
      if(!libraries.isEmpty()) {
        setupLibraries(project, modelsProvider, modulesProvider, libraries);
      }
    }
    if(myHxml != null) {
      Module rootModule = getRootModule(modelsProvider, modulesProvider);
      if(rootModule != null) {
        setupCompilationSettings(rootModule, modelsProvider.getModuleModifiableModel(rootModule));
        setupHxmlRunConfigurations(rootModule);
      }
    }
  }

  @Nullable
  private Module getRootModule(ModifiableModelsProvider modelsProvider, ModulesProvider modulesProvider) {
    for(Module module:modulesProvider.getModules()) {
      ModifiableRootModel model = modelsProvider.getModuleModifiableModel(module);
      for(VirtualFile root:model.getContentRoots()) {
        if(root.getPath().equals(myProjectRoot)) {
          return module;
        }
      }
    }
    return null;
  }

  private void setupCompilationSettings(Module module, ModifiableRootModel model) {
    Project project = module.getProject();
    for(VirtualFile root:model.getContentRoots()) {
      if(hasCompilationTarget(project, root.getPath(), myHxml)) {
        HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
        settings.setBuildConfig(HaxeConfiguration.HXML.asBuildConfigValue());
        settings.setHxmlPath(getPath(root.getPath(), myHxml));
        //TODO: Proper implementation of running the output of every Haxe target.
        settings.setHaxeTarget(HaxeTarget.INTERP);
        break;
      }
    }
  }

  /**
   * Check if specified hxml file contains the compilation target settings. (e.g. `-js` flag)
   */
  static private boolean hasCompilationTarget(Project project, String root, String hxml) {
    try {
      List<HXMLData> dataList = HXMLData.load(project, root, hxml);
      for(HXMLData data:dataList) {
        if(data.hasTarget()) {
          return true;
        }
      }
    }
    catch (HXMLData.HXMLDataException e) {
      //e.printStackTrace();
    }
    return false;
  }

  private void setupHxmlRunConfigurations(Module module) {
    RunManager manager = RunManager.getInstance(module.getProject());
    HaxeRunConfigurationType configType = HaxeRunConfigurationType.getInstance();
    ConfigurationFactory factory = configType.getConfigurationFactories()[0];

    HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
    String hxmlPath = settings.getHxmlPath();
    if(hxmlPath != null && !hxmlPath.isEmpty()) {
      HaxeApplicationConfiguration config = (HaxeApplicationConfiguration)factory.createTemplateConfiguration(module.getProject());
      config.setName(module.getName() + " " + new File(hxmlPath).getName());
      config.setModule(module);
      RunnerAndConfigurationSettings runSettings = manager.createConfiguration(config, factory);
      manager.addConfiguration(runSettings, false);
      manager.setSelectedConfiguration(runSettings);
    }
  }

  private List<LibraryData> collectLibraries(Project project) {
    List<HaxeProjectConfigurationUpdater.LibraryData> result = new ArrayList<>();
    Sdk sdk = HaxelibSdkUtils.lookupSdk(project);
    String[] libNames = myLibraries.toArray(new String[0]);
    Set<String> cpList = HaxelibClasspathUtils.getHaxelibLibrariesClasspaths(sdk, libNames);
    for(String cp:cpList) {
      VirtualFile current = LocalFileSystem.getInstance().findFileByPath(cp);
      //"haxelib path" returns something like "/path/to/repo/libname/1,0,0/src"
      //we need to traverse up along this path until we reach a directory which contains haxelib.json
      while(current != null) {
        HaxelibMetadata meta = HaxelibMetadata.load(current);
        if(meta == HaxelibMetadata.EMPTY_METADATA) {
          try {
            current = current.getParent();
          } catch(Exception e) {
            current = null;
          }
        } else {
          result.add(new LibraryData(meta.getName(), cp));
          break;
        }
      }
    }
    return result;
  }

  private void setupLibraries(Project project, ModifiableModelsProvider modelsProvider, ModulesProvider modulesProvider, List<LibraryData> libraries) {
    LibraryTable.ModifiableModel librariesModel = modelsProvider.getLibraryTableModifiableModel(project);

    for(LibraryData lib:libraries) {
      VirtualFile root = LocalFileSystem.getInstance().findFileByPath(lib.getClasspath());
      if(root != null) {
        Library library = librariesModel.createLibrary(lib.getName(), HaxeLibraryType.HAXE_LIBRARY);
        Library.ModifiableModel model = library.getModifiableModel();
        model.addRoot(root, OrderRootType.CLASSES);
        model.addRoot(root, OrderRootType.SOURCES);
        model.commit();

        for(Module module:modulesProvider.getModules()) {
          ModifiableRootModel moduleModel = modelsProvider.getModuleModifiableModel(module);
          moduleModel.addLibraryEntry(library);
          moduleModel.commit();
        }
      }
    }

    librariesModel.commit();
  }

  public void setHxml(@Nullable String hxml) {
    this.myHxml = hxml;
  }

  public void setLibraries(Set<String> libraries) {
    myLibraries = libraries;
  }

  private static String getPath(String dir, String path) {
    return Paths.get(path).isAbsolute() ? path : Paths.get(dir, path).toString();
  }

  private static class LibraryData {
    private final String myClasspath;
    private final String myName;

    LibraryData(String name, String classpath) {
      this.myName = name;
      this.myClasspath = classpath;
    }

    public String getClasspath() {
      return myClasspath;
    }

    public String getName() {
      return myName;
    }
  }
}
