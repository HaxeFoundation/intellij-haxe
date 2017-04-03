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
package com.intellij.plugins.haxe.config;

import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashSet;
import org.jdom.Element;

import java.util.Arrays;
import java.util.Set;

/**
 * @author: Fedor.Korotkov
 */
@State(
  name = "HaxeProjectSettings",
  storages = {
    @Storage(file = StoragePathMacros.PROJECT_FILE),
    @Storage(file = StoragePathMacros.PROJECT_CONFIG_DIR + "/haxe.xml", scheme = StorageScheme.DIRECTORY_BASED)
  }
)
public class HaxeProjectSettings implements PersistentStateComponent<Element> {
  public static final String HAXE_SETTINGS = "HaxeProjectSettings";
  public static final String DEFINES = "defines";
  private String userCompilerDefinitions = "";

  public Set<String> getUserCompilerDefinitionsAsSet() {
    return new THashSet<String>(Arrays.asList(getUserCompilerDefinitions()));
  }

  public static HaxeProjectSettings getInstance(Project project) {
    return ServiceManager.getService(project, HaxeProjectSettings.class);
  }

  public String[] getUserCompilerDefinitions() {
    // TODO: Bug here, if there are definitions that contain commas (e.g. mylib_version="2,4,3")
    return userCompilerDefinitions.split(",");
  }

  public void setUserCompilerDefinitions(String[] userCompilerDefinitions) {
    this.userCompilerDefinitions = StringUtil.join(ContainerUtil.filter(userCompilerDefinitions, new Condition<String>() {
      @Override
      public boolean value(String s) {
        return s != null && !s.isEmpty();
      }
    }), ",");
  }

  @Override
  public void loadState(Element state) {
    userCompilerDefinitions = state.getAttributeValue(DEFINES, "");
  }

  @Override
  public Element getState() {
    final Element element = new Element(HAXE_SETTINGS);
    element.setAttribute(DEFINES, userCompilerDefinitions);
    return element;
  }
}
