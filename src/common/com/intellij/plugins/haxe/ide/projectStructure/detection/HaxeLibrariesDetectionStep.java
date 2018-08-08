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

import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.DetectedSourceRoot;
import com.intellij.ide.util.projectWizard.importSources.ProjectFromSourcesBuilder;
import com.intellij.ide.util.projectWizard.importSources.ProjectStructureDetector;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.plugins.haxe.ide.projectStructure.HXMLData;
import com.intellij.plugins.haxe.ide.projectStructure.ui.HaxeDetectedLibraries;

import javax.swing.*;
import java.io.File;
import java.util.*;

public class HaxeLibrariesDetectionStep extends ModuleWizardStep {
  private final HaxeModuleInsight insight;
  private final ProjectFromSourcesBuilder builder;
  private final HaxeDetectedLibraries gui;
  private final Vector<String> libraries;
  private final HaxeProjectConfigurationUpdater projectUpdater;

  public HaxeLibrariesDetectionStep(ProjectFromSourcesBuilder builder, HaxeModuleInsight insight, HaxeProjectConfigurationUpdater projectUpdater) {
    super();
    this.builder = builder;
    this.insight = insight;
    this.libraries = suggestLibraries();
    this.projectUpdater = projectUpdater;
    gui = new HaxeDetectedLibraries(libraries);
  }

  @Override
  public JComponent getComponent() {
    return gui.getContentPane();
  }

  @Override
  public void updateDataModel() {
    //{ Copy-pasted from LibrariesDetectionStep.java
    HashSet<String> fileTypes = new HashSet<>();
    StringTokenizer tokenizer = new StringTokenizer(FileTypeManager.getInstance().getIgnoredFilesList(), ";", false);

    while (tokenizer.hasMoreTokens()) {
      fileTypes.add(tokenizer.nextToken());
    }
    insight.setRoots(Collections.singletonList(new File(builder.getBaseProjectPath())), getSourceRoots(), fileTypes);
    //}

    projectUpdater.setLibraries(libraries);
  }

  private List<DetectedSourceRoot> getSourceRoots() {
    ArrayList<DetectedSourceRoot> sourceRoots = new ArrayList<>();
    ProjectStructureDetector[] detectors = ProjectStructureDetector.EP_NAME.getExtensions();

    for(ProjectStructureDetector detector:detectors) {
      for(DetectedProjectRoot root:builder.getProjectRoots(detector)) {
        if (insight.isApplicableRoot(root)) {
          sourceRoots.add((DetectedSourceRoot)root);
        }
      }
    }

    return sourceRoots;
  }

  private Vector<String> suggestLibraries() {
    String projectDir = builder.getContext().getProjectFileDirectory();
    Vector<String> libraries = new Vector<>();
    List<String> hxmls = HaxeHxmlDetectionStep.findHxmlFiles(projectDir);
    for(String hxml:hxmls) {
      try {
        List<HXMLData> dataList = HXMLData.load(projectDir, hxml);
        for(HXMLData data:dataList) {
          for(String lib:data.getLibraries()) {
            if(!libraries.contains(lib)) {
              libraries.add(lib);
            }
          }
        }
      }
      catch (HXMLData.HXMLDataException e) {
        //e.printStackTrace();
      }
    }
    return libraries;
  }
}
