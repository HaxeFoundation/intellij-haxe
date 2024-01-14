/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 * Copyright 2017-2019 Eric Bishton
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
import com.intellij.plugins.haxe.util.HaxeModificationTracker;
import com.intellij.plugins.haxe.util.HaxeTrackedModifiable;
import com.intellij.util.containers.ContainerUtil;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author: Fedor.Korotkov
 */
@State(
  name = "HaxeProjectSettings",
  storages = {
    @Storage("haxe.xml")
  }
)
public class HaxeProjectSettings implements PersistentStateComponent<Element>, HaxeTrackedModifiable {
  public static final String HAXE_SETTINGS = "HaxeProjectSettings";
  public static final String DEFINES = "defines";
  public static final String AUTO_DETECT = "auto_detect_defines";
  private String userCompilerDefinitions = "";
  private boolean autoDetectDefinitions = true;
  private HaxeModificationTracker tracker = new HaxeModificationTracker(getClass().getName());

  public Set<String> getUserCompilerDefinitionsAsSet() {
    return new HashSet<String>(Arrays.asList(getUserCompilerDefinitions()));
  }

  public static HaxeProjectSettings getInstance(Project project) {
    return project.getService(HaxeProjectSettings.class);
  }

  public String[] getUserCompilerDefinitions() {
    // TODO: Bug here, if there are definitions that contain commas (e.g. mylib_version="2,4,3")
    return userCompilerDefinitions.split(",");
  }

  @NotNull
  public Map<String, String> getUserCompilerDefinitionMap() {
    Map<String, String> defintionMap = new HashMap<>();
    String[] definitions = getUserCompilerDefinitions();
    for (String def : definitions) {
      if (def.trim().isEmpty()) continue;

      String[] split = def.split("=", 2);

      // Dashes are subtraction operators, so definitions (on the command line) that
      // contain dashes are mapped to an equivalent using underscores (when looking up definitions).
      String key = split[0];
      String value = split.length > 1 ? split[1] : null;
      defintionMap.put(key, value == null ? "" : value);


    }
    return defintionMap;
  }

  public void setUserCompilerDefinitions(String[] userCompilerDefinitions) {
    this.userCompilerDefinitions = StringUtil.join(ContainerUtil.filter(userCompilerDefinitions, new Condition<String>() {
      @Override
      public boolean value(String s) {
        return s != null && !s.isEmpty();
      }
    }), ",");
    tracker.notifyUpdated();
  }

  @Override
  public void loadState(Element state) {
    userCompilerDefinitions = state.getAttributeValue(DEFINES, "");
    String value = state.getAttributeValue(AUTO_DETECT);
    if (value == null) {
      // using default value "true" value if not found
      autoDetectDefinitions = true;
    }else {
      autoDetectDefinitions = Boolean.parseBoolean(value);
    }
    tracker.notifyUpdated();
  }

  @Override
  public Element getState() {
    final Element element = new Element(HAXE_SETTINGS);
    element.setAttribute(DEFINES, userCompilerDefinitions);
    element.setAttribute(AUTO_DETECT, String.valueOf(autoDetectDefinitions));
    return element;
  }

  @Override
  public Stamp getStamp() {
    return tracker.getStamp();
  }

  @Override
  public boolean isModifiedSince(Stamp s) {
    return tracker.isModifiedSince(s);
  }

  public boolean getAutoDetectDefinitions() {
    return autoDetectDefinitions;
  }

  public void setAutoDetectDefinitions(boolean selected) {
    autoDetectDefinitions = selected;
  }
}
