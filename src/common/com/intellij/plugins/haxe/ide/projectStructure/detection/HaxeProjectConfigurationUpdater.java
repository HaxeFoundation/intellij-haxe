/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2018 AS3Boyan
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
import com.intellij.openapi.roots.impl.libraries.LibraryEx;
import com.intellij.openapi.roots.impl.libraries.LibraryImpl;
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
import com.intellij.plugins.haxe.runner.HaxeApplicationConfiguration;
import com.intellij.plugins.haxe.runner.HaxeRunConfigurationType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class HaxeProjectConfigurationUpdater implements ProjectFromSourcesBuilderImpl.ProjectConfigurationUpdater {
  private Vector<String> myLibraries;

  @Override
  public void updateProject(@NotNull Project project, @NotNull ModifiableModelsProvider modelsProvider, @NotNull ModulesProvider modulesProvider) {
    if(myLibraries != null && !myLibraries.isEmpty()) {
      List<LibraryData> libraries = collectLibraries(project);
      if(!libraries.isEmpty()) {
        setupLibraries(project, modelsProvider, modulesProvider, libraries);
      }
    }
    setupHxmlRunConfigurations(project, modelsProvider, modulesProvider);
  }

  private void setupHxmlRunConfigurations(Project project, ModifiableModelsProvider modelsProvider, ModulesProvider modulesProvider) {
    RunManager manager = RunManager.getInstance(project);
    HaxeRunConfigurationType configType = HaxeRunConfigurationType.getInstance();
    ConfigurationFactory factory = configType.getConfigurationFactories()[0];

    for(Module module:modulesProvider.getModules()) {
      HaxeModuleSettings settings = HaxeModuleSettings.getInstance(module);
      String hxmlPath = settings.getHxmlPath();
      if(hxmlPath == null || hxmlPath.isEmpty()) {
        continue;
      }

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
    List<String> cpList = HaxelibClasspathUtils.getHaxelibLibrariesClasspaths(sdk, libNames);
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
          result.add(new HaxeProjectConfigurationUpdater.LibraryData(meta.getName(), cp));
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
        LibraryImpl library = (LibraryImpl)librariesModel.createLibrary(lib.getName());
        LibraryEx.ModifiableModelEx model = library.getModifiableModel();
        model.setKind(HaxeLibraryType.HAXE_LIBRARY);
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

  public void setLibraries(Vector<String> libraries) {
    myLibraries = libraries;
  }

  private class LibraryData {
    private final String classpath;
    private final String name;

    LibraryData(String name, String classpath) {
      this.name = name;
      this.classpath = classpath;
    }

    public String getClasspath() {
      return classpath;
    }

    public String getName() {
      return name;
    }
  }
}
