/*
 * Copyright 2018 Aleksandr Kuzmenko
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
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.ide.util.projectWizard.importSources.ProjectFromSourcesBuilder;
import com.intellij.plugins.haxe.ide.projectStructure.HXMLData;
import com.intellij.plugins.haxe.ide.projectStructure.ui.HXMLSelector;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class HaxeHxmlDetectionStep extends ModuleWizardStep {
  private final HXMLSelector myGui;
  private final HaxeProjectConfigurationUpdater myProjectUpdater;

  public HaxeHxmlDetectionStep(ProjectFromSourcesBuilder builder, HaxeProjectConfigurationUpdater projectUpdater) {
    this.myProjectUpdater = projectUpdater;

    WizardContext context = builder.getContext();
    Vector<String> hxmlList = new Vector<>();
    String projectDir = context.getProjectFileDirectory();
    List<String> hxmlFiles = findHxmlFiles(projectDir);
    for(String hxmlPath:hxmlFiles) {
      try {
        List<HXMLData> hxmls = HXMLData.load(context.getProject(), projectDir, hxmlPath);
        if(!hasTarget(hxmls)) {
          throw new HXMLData.HXMLDataException("No compilation target.");
        }
        hxmlList.addElement(hxmlPath);
      }
      catch (HXMLData.HXMLDataException e) {
        //e.printStackTrace();
      }
    }

    myGui = new HXMLSelector(hxmlList);
  }

  @Override
  public JComponent getComponent() {
    return myGui.getContentPane();
  }

  @Override
  public void updateDataModel() {
    myProjectUpdater.setHxml(myGui.getSelected());
  }

  static List<String> findHxmlFiles(String directory) {
    List<String> result = new ArrayList<>();
    try {
      DirectoryStream<Path> files = Files.newDirectoryStream(Paths.get(directory), "*.hxml");
      for(Path file:files) {
        result.add(file.getFileName().toString());
      }
    }
    catch (IOException e) {
      //e.printStackTrace();
    }
    return result;
  }

  static private boolean hasTarget(List<HXMLData> hxmls) {
    for(HXMLData hxml:hxmls) {
      if(hxml.hasTarget()) {
        return true;
      }
    }
    return false;
  }
}
