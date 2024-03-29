/*
 * Copyright 2017-2018 Eric Bishton
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
package com.intellij.plugins.haxe.buildsystem.hxml.model;

import com.intellij.openapi.diagnostic.LogLevel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.plugins.haxe.hxml.psi.HXMLLib;
import com.intellij.plugins.haxe.hxml.psi.HXMLOption;
import com.intellij.plugins.haxe.hxml.psi.HXMLProperty;
import com.intellij.plugins.haxe.hxml.psi.HXMLValue;
import com.intellij.plugins.haxe.util.UsefulPsiTreeUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import lombok.CustomLog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Created by ebishton on 9/8/2017.
 */
@CustomLog
public class HXMLProjectModel {

  static {
    log.setLevel(LogLevel.INFO);
  }
  public static final String SWF_NAME = "-swf";
  public static final String SWF_VERSION = "-swf-version";
  public static final String SWF_HEADER = "-swf-header";
  public static final String SWF_LIB = "-swf-lib";

  public static final String MACRO = "--macro";
  public static final String MAIN_CLASS = "-main";
  public static final String DEBUG = "-debug";

  public static final String LIBRARY = "-lib";
  public static final String CLASSPATH = "-cp";

  public static final String CWD = "--cwd";


  protected PsiFile psiFile;

  public HXMLProjectModel(PsiFile psiFile) {
    this.psiFile = psiFile;
  }

  public static HXMLProjectModel create(Project project, VirtualFile hxmlFile) {
    if (null == project || null == hxmlFile) {
      log.debug("Null project or hxmlFile");
      return null;
    }

    PsiFile psi = PsiManager.getInstance(project).findFile(hxmlFile);
    return new HXMLProjectModel(psi);
  }

  // public Map<String,String> getDefinitions();
  // public List<String> getMacros();
  // public List<String> getClasspath();
  // public String getMainClass();

  public List<String> getLibraries() {
      List<String> found = new ArrayList<>();;
      HXMLLib[] libs = UsefulPsiTreeUtil.getChildrenOfType(psiFile, HXMLLib.class, null);
      if (null != libs) {
        for (HXMLLib lib : libs) {
          lib.getValueList().stream()
            .map(PsiElement::getText)
            .filter(Objects::nonNull)
            .forEach(found::add);
        }
      }
    return found;
  }

  @Nullable
  public String getSwfOutputFileName() {
    return getProperty(SWF_NAME);
  }


  @Nullable
  public String getProperty(String propertyName) {
    List<String> found = getProperties(propertyName);
    if (found != null) {
      if (found.size() == 1) {
        return found.get(0);
      } else if (found.size() > 1) {
        if (log.isDebugEnabled()) {
          StringBuilder msg = new StringBuilder(256);
          msg.append("Unexpectedly found more than one setting for ");
          msg.append(propertyName);
          msg.append(":\n");
          for (String s : found) {
            msg.append("   ");
            msg.append(s);
            msg.append('\n');
          }
          log.info(msg.toString());
        }
        return found.get(0);
      }
    }
    return null;
  }


  @Nullable
  public List<String> getProperties(@NotNull String propertyName) {
    if (propertyName.isEmpty()) {
      return null;
    }

    List<String> found = null;
    HXMLProperty[] properties = UsefulPsiTreeUtil.getChildrenOfType(psiFile, HXMLProperty.class, null);
    if (null != properties) {
      for (HXMLProperty foundProperty : properties) {
        HXMLOption option = foundProperty.getOption();
        if (null != option && propertyName.equals(option.getText())) {
          HXMLValue val = foundProperty.getValue();
          if (val != null) {
            if (found == null) {
              found = new ArrayList<String>();
            }
            found.add(val.getText());
          }
        }
      }
    }
    return found;
  }

  public String getWorkingDirectory() {
    return getProperty(CWD);
  }
}
